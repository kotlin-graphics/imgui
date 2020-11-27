package imgui.internal.api

import glm_.L
import glm_.vec4.Vec4
import imgui.COL32
import imgui.ImGui
import imgui.ImGui.bulletText
import imgui.ImGui.getForegroundDrawList
import imgui.ImGui.isItemHovered
import imgui.ImGui.sameLine
import imgui.ImGui.textColored
import imgui.ImGui.treeNode
import imgui.ImGui.treePop
import imgui.api.g
import imgui.classes.DrawList
import imgui.internal.classes.Window
import imgui.internal.sections.Columns
import kool.rem

/** Debug Tools */
internal interface debugTools {

    fun debugDrawItemRect(col: Int = COL32(255, 0, 0, 255)) {
        val window = g.currentWindow!!
        getForegroundDrawList(window).addRect(window.dc.lastItemRect.min, window.dc.lastItemRect.max, col)
    }

    fun debugStartItemPicker() {
        g.debugItemPickerActive = true
    }

    /** [DEBUG] Display contents of Columns */
    fun debugNodeColumns(columns: Columns) {
        if (!treeNode(columns.id.L, "Columns Id: 0x%08X, Count: ${columns.count}, Flags: 0x%04X", columns.id, columns.flags))
            return
        bulletText("Width: %.1f (MinX: %.1f, MaxX: %.1f)", columns.offMaxX - columns.offMinX, columns.offMinX, columns.offMaxX)
        columns.columns.forEachIndexed { i, c ->
            bulletText("Column %02d: OffsetNorm %.3f (= %.1f px)", i, c.offsetNorm, columns getOffsetFrom c.offsetNorm)
        }
        treePop()
    }

    /** [DEBUG] Display contents of ImDrawList */
    fun debugNodeDrawList(window: Window?, drawList: DrawList, label: String)  {

        val cfg = g.debugMetricsConfig
        var cmdCount = drawList.cmdBuffer.size
        val last = drawList.cmdBuffer.last()
        if (cmdCount > 0 && last.elemCount == 0 && last.userCallback == null)
            cmdCount--
        val nodeOpen = treeNode(drawList, "$label: '${drawList._ownerName}' ${drawList.vtxBuffer.size} vtx, ${drawList.idxBuffer.rem} indices, $cmdCount cmds")
        if (drawList === ImGui.windowDrawList) {
            sameLine()
            textColored(Vec4(1f, 0.4f, 0.4f, 1f), "CURRENTLY APPENDING") // Can't display stats for active draw list! (we don't have the data double-buffered)
            if (nodeOpen)
                treePop()
            return
        }

        val fgDrawList = getForegroundDrawList(window) // Render additional visuals into the top-most draw list
        if (window != null && isItemHovered())
            fgDrawList.addRect(window.pos, window.pos + window.size, COL32(255, 255, 0, 255))
        if (!nodeOpen)
            return

        if (window != null && !window.wasActive)
        TextDisabled("Warning: owning Window is inactive. This DrawList is not being rendered!")

        for (const ImDrawCmd* pcmd = draw_list->CmdBuffer.Data; pcmd < draw_list->CmdBuffer.Data + cmd_count; pcmd++)
        {
            if (pcmd->UserCallback)
            {
                BulletText("Callback %p, user_data %p", pcmd->UserCallback, pcmd->UserCallbackData)
                continue
            }

            char buf[300]
            ImFormatString(buf, IM_ARRAYSIZE(buf), "DrawCmd:%5d tris, Tex 0x%p, ClipRect (%4.0f,%4.0f)-(%4.0f,%4.0f)",
                    pcmd->ElemCount / 3, (void*)(intptr_t)pcmd->TextureId,
            pcmd->ClipRect.x, pcmd->ClipRect.y, pcmd->ClipRect.z, pcmd->ClipRect.w)
            bool pcmd_node_open = TreeNode((void*)(pcmd - draw_list->CmdBuffer.begin()), "%s", buf)
            if (IsItemHovered() && (cfg->ShowDrawCmdMesh || cfg->ShowDrawCmdBoundingBoxes) && fg_draw_list)
            DebugNodeDrawCmdShowMeshAndBoundingBox(window, draw_list, pcmd, cfg->ShowDrawCmdMesh, cfg->ShowDrawCmdBoundingBoxes)
            if (!pcmd_node_open)
                continue

            // Calculate approximate coverage area (touched pixel count)
            // This will be in pixels squared as long there's no post-scaling happening to the renderer output.
            const ImDrawIdx* idx_buffer = (draw_list->IdxBuffer.Size > 0) ? draw_list->IdxBuffer.Data : NULL
            const ImDrawVert* vtx_buffer = draw_list->VtxBuffer.Data + pcmd->VtxOffset
            float total_area = 0.0f
            for (unsigned int idx_n = pcmd->IdxOffset; idx_n < pcmd->IdxOffset + pcmd->ElemCount; )
            {
                ImVec2 triangle[3]
                for (int n = 0; n < 3; n++, idx_n++)
                triangle[n] = vtx_buffer[idx_buffer ? idx_buffer[idx_n] : idx_n].pos
                total_area += ImTriangleArea(triangle[0], triangle[1], triangle[2])
            }

            // Display vertex information summary. Hover to get all triangles drawn in wire-frame
            ImFormatString(buf, IM_ARRAYSIZE(buf), "Mesh: ElemCount: %d, VtxOffset: +%d, IdxOffset: +%d, Area: ~%0.f px", pcmd->ElemCount, pcmd->VtxOffset, pcmd->IdxOffset, total_area)
            Selectable(buf)
            if (IsItemHovered() && fgDrawList)
                DebugNodeDrawCmdShowMeshAndBoundingBox(window, draw_list, pcmd, true, false)

            // Display individual triangles/vertices. Hover on to get the corresponding triangle highlighted.
            ImGuiListClipper clipper
            clipper.Begin(pcmd->ElemCount / 3) // Manually coarse clip our print out of individual vertices to save CPU, only items that may be visible.
            while (clipper.Step())
                for (int prim = clipper.DisplayStart, idx_i = pcmd->IdxOffset + clipper.DisplayStart * 3; prim < clipper.DisplayEnd; prim++)
            {
                char* buf_p = buf, * buf_end = buf + IM_ARRAYSIZE(buf)
                ImVec2 triangle[3]
                for (int n = 0; n < 3; n++, idx_i++)
                {
                    const ImDrawVert& v = vtx_buffer[idx_buffer ? idx_buffer[idx_i] : idx_i]
                    triangle[n] = v.pos
                    buf_p += ImFormatString(buf_p, buf_end - buf_p, "%s %04d: pos (%8.2f,%8.2f), uv (%.6f,%.6f), col %08X\n",
                            (n == 0) ? "Vert:" : "     ", idx_i, v.pos.x, v.pos.y, v.uv.x, v.uv.y, v.col)
                }

                Selectable(buf, false)
                if (fgDrawList && IsItemHovered())
                {
                    ImDrawListFlags backup_flags = fg_draw_list->Flags
                    fgDrawList->Flags &= ~ImDrawListFlags_AntiAliasedLines // Disable AA on triangle outlines is more readable for very large and thin triangles.
                    fgDrawList->AddPolyline(triangle, 3, IM_COL32(255, 255, 0, 255), true, 1.0f)
                    fgDrawList->Flags = backup_flags
                }
            }
            TreePop()
        }
        TreePop()
    }

    IMGUI_API void          DebugNodeDrawCmdShowMeshAndBoundingBox(ImGuiWindow* window, const ImDrawList* draw_list, const ImDrawCmd* draw_cmd, bool show_mesh, bool show_aabb)
    IMGUI_API void          DebugNodeStorage(ImGuiStorage* storage, const char* label)
    IMGUI_API void          DebugNodeTabBar(ImGuiTabBar* tab_bar, const char* label)
    IMGUI_API void          DebugNodeWindow(ImGuiWindow* window, const char* label)
    IMGUI_API void          DebugNodeWindowSettings(ImGuiWindowSettings* settings)
    IMGUI_API void          DebugNodeWindowsList(ImVector<ImGuiWindow*>* windows, const char* label)
}