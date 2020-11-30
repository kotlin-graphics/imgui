package imgui.internal.api

import glm_.L
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.bulletText
import imgui.ImGui.getForegroundDrawList
import imgui.ImGui.getStyleColorVec4
import imgui.ImGui.isItemHovered
import imgui.ImGui.popID
import imgui.ImGui.popStyleColor
import imgui.ImGui.pushID
import imgui.ImGui.pushStyleColor
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.smallButton
import imgui.ImGui.text
import imgui.ImGui.textColored
import imgui.ImGui.textDisabled
import imgui.ImGui.treeNode
import imgui.ImGui.treeNodeEx
import imgui.ImGui.treePop
import imgui.api.g
import imgui.classes.DrawList
import imgui.classes.ListClipper
import imgui.internal.DrawCmd
import imgui.internal.classes.Rect
import imgui.internal.classes.TabBar
import imgui.internal.classes.Window
import imgui.internal.floor
import imgui.internal.sections.Columns
import imgui.internal.sections.DrawListFlag
import imgui.internal.sections.WindowSettings
import imgui.internal.sections.wo
import imgui.internal.triangleArea
import kool.lib.isNotEmpty
import kool.rem
import uno.kotlin.plusAssign

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
    fun debugNodeDrawList(window: Window?, drawList: DrawList, label: String) {

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
            textDisabled("Warning: owning Window is inactive. This DrawList is not being rendered!")

        for (cmdIdx in 0 until cmdCount) {
            val cmd = drawList.cmdBuffer[cmdIdx]
            val userCallback = cmd.userCallback
            if (userCallback != null) {
                bulletText("Callback $userCallback, user_data ${cmd.userCallbackData}")
                continue
            }

            var buf = "DrawCmd:%5d tris, Tex 0x%p, ClipRect (%4.0f,%4.0f)-(%4.0f,%4.0f)".format(
                    cmd.elemCount / 3, cmd.textureId, cmd.clipRect.x, cmd.clipRect.y, cmd.clipRect.z, cmd.clipRect.w)
            val pcmdNodeOpen = treeNode(drawList.cmdBuffer.indexOf(cmd), buf)
            if (isItemHovered() && (cfg.showDrawCmdMesh || cfg.showDrawCmdBoundingBoxes) /*&& fgDrawList != null*/)
                debugNodeDrawCmdShowMeshAndBoundingBox(window, drawList, cmd, cfg.showDrawCmdMesh, cfg.showDrawCmdBoundingBoxes)
            if (!pcmdNodeOpen)
                continue

            // Calculate approximate coverage area (touched pixel count)
            // This will be in pixels squared as long there's no post-scaling happening to the renderer output.
            val idxBuffer = drawList.idxBuffer.takeIf { it.isNotEmpty() }
            val vtxBuffer = drawList.vtxBuffer
            val vtxPointer = cmd.vtxOffset
            var totalArea = 0f
            var idxN = cmd.idxOffset
            while (idxN < cmd.idxOffset + cmd.elemCount) {
                val triangle = Array(3) {
                    vtxBuffer[vtxPointer + (idxBuffer?.get(idxN) ?: idxN)].pos.also { idxN++ }
                }
                totalArea += triangleArea(triangle[0], triangle[1], triangle[2])
            }

            // Display vertex information summary. Hover to get all triangles drawn in wire-frame
            buf = "Mesh: ElemCount: ${cmd.elemCount}, VtxOffset: +${cmd.vtxOffset}, IdxOffset: +${cmd.idxOffset}, Area: ~%0.f px".format(totalArea)
            selectable(buf)
            if (isItemHovered() /*&& fgDrawList != null*/)
                debugNodeDrawCmdShowMeshAndBoundingBox(window, drawList, cmd, true, false)

            // Display individual triangles/vertices. Hover on to get the corresponding triangle highlighted.
            val clipper = ListClipper()
            clipper.begin(cmd.elemCount / 3) // Manually coarse clip our print out of individual vertices to save CPU, only items that may be visible.
            while (clipper.step()) {
                var idx_i = cmd.idxOffset + clipper.displayStart * 3
                for (prim in clipper.displayStart until clipper.displayEnd) {
                    val bufP = StringBuilder()
                    val triangle = Array(3) { Vec2() }
                    for (n in 0..2) {
                        val v = vtxBuffer[vtxPointer + (idxBuffer?.get(idx_i) ?: idx_i)]
                        triangle[n] put v.pos
                        val isFirst = if (n == 0) "Vert:" else "     "
                        bufP += "$isFirst %04d: pos (%8.2f,%8.2f), uv (%.6f,%.6f), col %08X\n"
                                .format(idx_i, v.pos.x, v.pos.y, v.uv.x, v.uv.y, v.col)
                        idx_i++
                    }
                    buf = bufP.toString()
                    selectable(buf, false)
                    if (/*fgDrawList != null &&*/ isItemHovered()) {
                        val backupFlags = fgDrawList.flags
                        fgDrawList.flags = fgDrawList.flags wo DrawListFlag.AntiAliasedLines // Disable AA on triangle outlines is more readable for very large and thin triangles.
                        fgDrawList.addPolyline(triangle.asList(), COL32(255, 255, 0, 255), true, 1f)
                        fgDrawList.flags = backupFlags
                    }
                }
            }
            treePop()
        }
        treePop()
    }

    /** [DEBUG] Display mesh/aabb of a ImDrawCmd */
    fun debugNodeDrawCmdShowMeshAndBoundingBox(window: Window?, drawList: DrawList, drawCmd: DrawCmd, showMesh: Boolean, showAabb: Boolean) {
        assert(showMesh || showAabb)
        val fgDrawList = getForegroundDrawList(window) // Render additional visuals into the top-most draw list
        val idxBuffer = drawList.idxBuffer.takeIf { it.isNotEmpty() }
        val vtxBuffer = drawList.vtxBuffer
        val vtxPointer = drawCmd.vtxOffset

        // Draw wire-frame version of all triangles
        val clipRect = Rect(drawCmd.clipRect)
        val vtxsRect = Rect(Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        val backupFlags = fgDrawList.flags
        fgDrawList.flags = fgDrawList.flags wo DrawListFlag.AntiAliasedLines // Disable AA on triangle outlines is more readable for very large and thin triangles.
        var idxN = drawCmd.idxOffset
        while (idxN < drawCmd.idxOffset + drawCmd.elemCount) {
            val triangle = Array(3) { Vec2() }
            for (n in 0..2) {
                triangle[n] put vtxBuffer[vtxPointer + (idxBuffer?.get(idxN) ?: idxN)].pos
                vtxsRect.add(triangle[n])
                idxN++
            }
            if (showMesh)
                fgDrawList.addPolyline(triangle.asList(), COL32(255, 255, 0, 255), true, 1f) // In yellow: mesh triangles
        }
        // Draw bounding boxes
        if (showAabb) {
            fgDrawList.addRect(floor(clipRect.min), floor(clipRect.max), COL32(255, 0, 255, 255)) // In pink: clipping rectangle submitted to GPU
            fgDrawList.addRect(floor(vtxsRect.min), floor(vtxsRect.max), COL32(0, 255, 255, 255)) // In cyan: bounding box of triangles
        }
        fgDrawList.flags = backupFlags
    }

    /** [DEBUG] Display contents of ImGuiStorage */
    fun <K, V> debugNodeStorage(storage: HashMap<K, V>, label: String) {
        if (!treeNode(label, "$label: ${storage.size} entries, ${storage.size} bytes"))
            return
        storage.forEach { (k, v) ->
            bulletText("Key 0x%08X Value { i: $v }".format(k)) // Important: we currently don't store a type, real value may not be integer.
        }
        treePop()
    }

    /** [DEBUG] Display contents of ImGuiTabBar */
    fun debugNodeTabBar(tabBar: TabBar, label: String) {
        // Standalone tab bars (not associated to docking/windows functionality) currently hold no discernible strings.
        val isActive = tabBar.prevFrameVisible >= ImGui.frameCount - 2
        val text = "$label 0x%08X (${tabBar.tabs.size} tabs)${if (isActive) "" else " *Inactive*"}".format(tabBar.id)
        if (!isActive)
            pushStyleColor(Col.Text, getStyleColorVec4(Col.TextDisabled))
        val open = treeNode(tabBar, text)
        if (!isActive)
            popStyleColor()
        if (isActive && isItemHovered()) {
            val drawList = ImGui.foregroundDrawList
            drawList.addRect(tabBar.barRect.min, tabBar.barRect.max, COL32(255, 255, 0, 255))
            drawList.addLine(Vec2(tabBar.scrollingRectMinX, tabBar.barRect.min.y), Vec2(tabBar.scrollingRectMinX, tabBar.barRect.max.y), COL32(0, 255, 0, 255))
            drawList.addLine(Vec2(tabBar.scrollingRectMaxX, tabBar.barRect.min.y), Vec2(tabBar.scrollingRectMaxX, tabBar.barRect.max.y), COL32(0, 255, 0, 255))
        }
        if (open) {
            for (tabN in tabBar.tabs.indices) {
                val tab = tabBar.tabs[tabN]
                pushID(tab)
                if (smallButton("<"))
                    tabBar.queueReorder(tab, -1)
                sameLine(0, 2)
                if (smallButton(">"))
                    tabBar.queueReorder(tab, +1)
                sameLine()
                val c = if (tab.id == tabBar.selectedTabId) '*' else ' '
                val name = if (tab.nameOffset != -1) tabBar.getTabName(tab) else ""
                text("%02d$c Tab 0x%08X '$name' Offset: %.1f, Width: %.1f/%.1f",
                        tabN, tab.id, tab.offset, tab.width, tab.contentWidth)
                popID()
            }
            treePop()
        }
    }

    fun debugNodeWindow(window: Window?, label: String) {
        if (window == null) {
            bulletText("$label: NULL")
            return
        }

        val isActive = window.wasActive
        val treeNodeFlags = if (window === g.navWindow) TreeNodeFlag.Selected else TreeNodeFlag.None
        if (!isActive)
            pushStyleColor(Col.Text, getStyleColorVec4(Col.TextDisabled))
        val open = treeNodeEx(label, treeNodeFlags.i, "$label '${window.name}'${if (isActive) "" else " *Inactive*"}")
        if (!isActive)
            popStyleColor()
        if (isItemHovered() && isActive)
            getForegroundDrawList(window).addRect(window.pos, window.pos + window.size, COL32(255, 255, 0, 255))
        if (!open)
            return

        if (window.memoryCompacted)
            textDisabled("Note: some memory buffers have been compacted/freed.")

        val flags = window.flags
        debugNodeDrawList(window, window.drawList, "DrawList")
        bulletText("Pos: (%.1f,%.1f), Size: (%.1f,%.1f), ContentSize (%.1f,%.1f)", window.pos.x, window.pos.y,
                window.size.x, window.size.y, window.contentSize.x, window.contentSize.y)
        val s = StringBuilder()
        if (flags has WindowFlag._ChildWindow) s += "Child "
        if (flags has WindowFlag._Tooltip) s += "Tooltip "
        if (flags has WindowFlag._Popup) s += "Popup "
        if (flags has WindowFlag._Modal) s += "Modal "
        if (flags has WindowFlag._ChildMenu) s += "ChildMenu "
        if (flags has WindowFlag.NoSavedSettings) s += "NoSavedSettings "
        if (flags has WindowFlag.NoMouseInputs) s += "NoMouseInputs"
        if (flags has WindowFlag.NoNavInputs) s += "NoNavInputs"
        if (flags has WindowFlag.AlwaysAutoResize) s += "AlwaysAutoResize"
        bulletText("Flags: 0x%08X ($s..)", flags)
        val scroll = "Scroll: (%.2f/%.2f,%.2f/%.2f)".format(window.scroll.x, window.scrollMax.x, window.scroll.y, window.scrollMax.y)
        val scrollbar = "Scrollbar:${if (window.scrollbar.x) "X" else ""}${if (window.scrollbar.y) "Y" else ""}"
        bulletText("$scroll $scrollbar")
        val active = window.active.i
        val wasActive = window.wasActive.i
        val writeAccessed = window.writeAccessed.i
        val order = if (window.active || window.wasActive) window.beginOrderWithinContext else -1
        bulletText("Active: $active/$wasActive, WriteAccessed: $writeAccessed, BeginOrderWithinContext: $order")
        val appearing = window.appearing.i
        val hidden = window.hidden.i
        val canSkip = window.hiddenFramesCanSkipItems
        val cannot = window.hiddenFramesCannotSkipItems
        val skipItems = window.skipItems.i
        bulletText("Appearing: $appearing, Hidden: $hidden (CanSkip $canSkip Cannot $cannot), SkipItems: $skipItems")
        bulletText("NavLastIds: 0x%08X,0x%08X, NavLayerActiveMask: %X", window.navLastIds[0], window.navLastIds[1], window.dc.navLayerActiveMask)
        bulletText("NavLastChildNavWindow: ${window.navLastChildNavWindow?.name ?: "NULL"}")
        val r = window.navRectRel[0]
        if (!r.isInverted)
            bulletText("NavRectRel[0]: (%.1f,%.1f)(%.1f,%.1f)", r.min.x, r.min.y, r.max.x, r.max.y)
        else
            bulletText("NavRectRel[0]: <None>")
        if (window.rootWindow !== window) debugNodeWindow(window.rootWindow, "RootWindow")
        window.parentWindow?.let { debugNodeWindow(it, "ParentWindow") }
        if (window.dc.childWindows.isNotEmpty()) debugNodeWindowsList(window.dc.childWindows, "ChildWindows")
        if (window.columnsStorage.isNotEmpty() && treeNode("Columns", "Columns sets (${window.columnsStorage.size})")) {
            for (n in window.columnsStorage.indices)
                debugNodeColumns(window.columnsStorage[n])
            treePop()
        }
        debugNodeStorage(window.stateStorage, "Storage")
        treePop()
    }

    fun debugNodeWindowSettings(settings: WindowSettings) {
        val pX = settings.pos.x
        val pY = settings.pos.y
        val sX = settings.size.x
        val sY = settings.size.y
        val collapsed = settings.collapsed.i
        text("0x%08X \"${settings.name}\" Pos ($pX,$pY) Size ($sX,$sY) Collapsed=$collapsed", settings.id)
    }

    fun debugNodeWindowsList(windows: List<Window>, label: String) {
        if (!treeNode(label, "$label (${windows.size})"))
            return
        text("(In front-to-back order:)")
        for (i in windows.size - 1 downTo 0) { // Iterate front to back
            pushID(windows[i])
            debugNodeWindow(windows[i], "Window")
            popID()
        }
        treePop()
    }
}