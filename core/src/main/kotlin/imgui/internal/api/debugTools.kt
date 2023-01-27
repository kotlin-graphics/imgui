package imgui.internal.api

import glm_.L
import glm_.d
import glm_.i
import glm_.min
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.beginTooltip
import imgui.ImGui.bullet
import imgui.ImGui.bulletText
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endDisabled
import imgui.ImGui.endGroup
import imgui.ImGui.endTabBar
import imgui.ImGui.endTable
import imgui.ImGui.endTooltip
import imgui.ImGui.fontSize
import imgui.ImGui.foregroundDrawList
import imgui.ImGui.getColorU32
import imgui.ImGui.getForegroundDrawList
import imgui.ImGui.getStyleColorVec4
import imgui.ImGui.isItemHovered
import imgui.ImGui.isItemVisible
import imgui.ImGui.itemRectMax
import imgui.ImGui.itemRectMin
import imgui.ImGui.popFocusScope
import imgui.ImGui.popID
import imgui.ImGui.popItemFlag
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.popTextWrapPos
import imgui.ImGui.pushID
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushTextWrapPos
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.setNextItemOpen
import imgui.ImGui.smallButton
import imgui.ImGui.text
import imgui.ImGui.textColored
import imgui.ImGui.textDisabled
import imgui.ImGui.textUnformatted
import imgui.ImGui.treeNode
import imgui.ImGui.treeNodeEx
import imgui.ImGui.treePop
import imgui.api.g
import imgui.classes.DrawList
import imgui.classes.ListClipper
import imgui.demo.showExampleApp.StyleEditor
import imgui.dsl.treeNode
import imgui.dsl.withID
import imgui.font.FontAtlas
import imgui.internal.DrawCmd
import imgui.internal.classes.*
import imgui.internal.floor
import imgui.internal.sections.*
import imgui.internal.triangleArea
import kool.lib.isNotEmpty
import kool.rem
import uno.kotlin.plusAssign

typealias ErrorLogCallback = (userData: Any?, fmt: String) -> Unit

/** Debug Tools */
internal interface debugTools {

    /** Experimental recovery from incorrect usage of BeginXXX/EndXXX/PushXXX/PopXXX calls.
     *  Must be called during or before EndFrame().
     *  This is generally flawed as we are not necessarily End/Popping things in the right order.
     *  FIXME: Can't recover from inside BeginTabItem/EndTabItem yet.
     *  FIXME: Can't recover from interleaved BeginTabBar/Begin */
    fun errorCheckEndFrameRecover(logCallback: ErrorLogCallback?, userData: Any? = null) {
        // PVS-Studio V1044 is "Loop break conditions do not depend on the number of iterations"
        while (g.currentWindowStack.isNotEmpty()) {
            errorCheckEndWindowRecover(logCallback, userData)
            val window = g.currentWindow!!
            if (g.currentWindowStack.size == 1) {
                assert(window.isFallbackWindow)
                break
            }
            assert(window === g.currentWindow)
            if (window.flags has WindowFlag._ChildWindow) {
                logCallback?.invoke(userData, "Recovered from missing EndChild() for '${window.name}'")
                endChild()
            } else {
                logCallback?.invoke(userData, "Recovered from missing End() for '${window.name}'")
                end()
            }
        }
    }

    fun errorCheckEndWindowRecover(logCallback: ErrorLogCallback?, userData: Any? = null) {

        while (g.currentTable != null && (g.currentTable!!.outerWindow === g.currentWindow || g.currentTable!!.innerWindow === g.currentWindow)) {
            logCallback?.invoke(userData, "Recovered from missing EndTable() in '${g.currentTable!!.outerWindow!!.name}'")
            endTable()
        }
        val window = g.currentWindow!!
        while (g.currentTabBar != null) { //-V1044
            logCallback?.invoke(userData, "Recovered from missing EndTabBar() in '${window.name}'")
            endTabBar()
        }
        while (window.dc.treeDepth > 0) {
            logCallback?.invoke(userData, "Recovered from missing TreePop() in '${window.name}'")
            treePop()
        }
        while (g.groupStack.size > window.dc.stackSizesOnBegin.sizeOfGroupStack) {
            logCallback?.invoke(userData, "Recovered from missing EndGroup() in '${window.name}'")
            endGroup()
        }
        while (window.idStack.size > 1) {
            logCallback?.invoke(userData, "Recovered from missing PopID() in '${window.name}'")
            popID()
        }
        while (g.disabledStackSize > window.dc.stackSizesOnBegin.sizeOfDisabledStack) {
            logCallback?.invoke(userData, "Recovered from missing EndDisabled() in '${window.name}'")
            endDisabled()
        }
        while (g.colorStack.size > window.dc.stackSizesOnBegin.sizeOfColorStack) {
            val name = window.name
            val col = g.colorStack.last().col
            logCallback?.invoke(userData, "Recovered from missing PopStyleColor() in '$name' for ImGuiCol_$col")
            popStyleColor()
        }
        while (g.itemFlagsStack.size > window.dc.stackSizesOnBegin.sizeOfItemFlagsStack) {
            logCallback?.invoke(userData, "Recovered from missing PopItemFlag() in '${window.name}'")
            popItemFlag()
        }
        while (g.styleVarStack.size > window.dc.stackSizesOnBegin.sizeOfStyleVarStack) {
            logCallback?.invoke(userData, "Recovered from missing PopStyleVar() in '${window.name}'")
            popStyleVar()
        }
        while (g.focusScopeStack.size > window.dc.stackSizesOnBegin.sizeOfFocusScopeStack) {
            logCallback?.invoke(userData, "Recovered from missing PopFocusScope() in '${window.name}'")
            popFocusScope()
        }
    }

    fun debugDrawItemRect(col: Int = COL32(255, 0, 0, 255)) {
        val window = g.currentWindow!!
        getForegroundDrawList(window).addRect(g.lastItemData.rect.min, g.lastItemData.rect.max, col)
    }

    fun debugStartItemPicker() {
        g.debugItemPickerActive = true
    }

    fun showFontAtlas(atlas: FontAtlas) {
        for (font in atlas.fonts)
            withID(font) {
                StyleEditor.showFont(font)
            }
        treeNode("Atlas texture", "Atlas texture (${atlas.texWidth}x${atlas.texHeight} pixels)") {
            val tintCol = Vec4(1f)
            val borderCol = Vec4(1f, 1f, 1f, 0.5f)
            ImGui.image(atlas.texID, Vec2(atlas.texWidth, atlas.texHeight), Vec2(), Vec2(1), tintCol, borderCol)
        }
    }

    /** [DEBUG] Display contents of Columns */
    fun debugNodeColumns(columns: OldColumns) {
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

            var buf = "DrawCmd:%5d tris, Tex 0x%02d, ClipRect (%4.0f,%4.0f)-(%4.0f,%4.0f)".format(
                cmd.elemCount / 3, cmd.textureId, cmd.clipRect.x, cmd.clipRect.y, cmd.clipRect.z, cmd.clipRect.w)
            val pcmdNodeOpen = treeNode(drawList.cmdBuffer.indexOf(cmd), buf)
            if (isItemHovered() && (cfg.showDrawCmdMesh || cfg.showDrawCmdBoundingBoxes) /*&& fgDrawList != null*/)
                debugNodeDrawCmdShowMeshAndBoundingBox(fgDrawList, drawList, cmd, cfg.showDrawCmdMesh, cfg.showDrawCmdBoundingBoxes)
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
                debugNodeDrawCmdShowMeshAndBoundingBox(fgDrawList, drawList, cmd, true, false)

            // Display individual triangles/vertices. Hover on to get the corresponding triangle highlighted.
            val clipper = ListClipper()
            clipper.begin(cmd.elemCount / 3) // Manually coarse clip our print out of individual vertices to save CPU, only items that may be visible.
            while (clipper.step()) {
                var idx_i = cmd.idxOffset + clipper.displayStart * 3
                for (prim in clipper.display) {
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
                        fgDrawList.addPolyline(triangle.asList(), COL32(255, 255, 0, 255), DrawFlag.Closed.i, 1f)
                        fgDrawList.flags = backupFlags
                    }
                }
            }
            treePop()
        }
        treePop()
    }

    /** [DEBUG] Display mesh/aabb of a ImDrawCmd */
    fun debugNodeDrawCmdShowMeshAndBoundingBox(outDrawList: DrawList, drawList: DrawList, drawCmd: DrawCmd, showMesh: Boolean, showAabb: Boolean) {
        assert(showMesh || showAabb)

        // Draw wire-frame version of all triangles
        val clipRect = Rect(drawCmd.clipRect)
        val vtxsRect = Rect(Float.MAX_VALUE, -Float.MAX_VALUE)
        val backupFlags = outDrawList.flags
        outDrawList.flags = outDrawList.flags wo DrawListFlag.AntiAliasedLines // Disable AA on triangle outlines is more readable for very large and thin triangles.
        var idxN = drawCmd.idxOffset
        while (idxN < drawCmd.idxOffset + drawCmd.elemCount) {
            val idxBuffer = drawList.idxBuffer.takeIf { it.isNotEmpty() }
            val vtxBuffer = drawList.vtxBuffer
            val vtxPointer = drawCmd.vtxOffset

            val triangle = Array(3) { Vec2() }
            for (n in 0..2) {
                triangle[n] put vtxBuffer[vtxPointer + (idxBuffer?.get(idxN) ?: idxN)].pos
                vtxsRect.add(triangle[n])
                idxN++
            }
            if (showMesh)
                outDrawList.addPolyline(triangle.asList(), COL32(255, 255, 0, 255), DrawFlag.Closed.i, 1f) // In yellow: mesh triangles
        }
        // Draw bounding boxes
        if (showAabb) {
            outDrawList.addRect(floor(clipRect.min), floor(clipRect.max), COL32(255, 0, 255, 255)) // In pink: clipping rectangle submitted to GPU
            outDrawList.addRect(floor(vtxsRect.min), floor(vtxsRect.max), COL32(0, 255, 255, 255)) // In cyan: bounding box of triangles
        }
        outDrawList.flags = backupFlags
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
        var text = "$label 0x%08X (${tabBar.tabs.size} tabs)${if (isActive) "" else " *Inactive*"}".format(tabBar.id)
        text += "  { "
        for (tabN in 0 until (tabBar.tabs.size min 3)) {
            val tab = tabBar.tabs[tabN]
            text += (if (tabN > 0) ", " else "") + "'" + if (tab.nameOffset != -1) tabBar.getTabName(tab) else "???" + "'"
        }
        text += if (tabBar.tabs.size > 3) " ... }" else " } "
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
                val name = if (tab.nameOffset != -1) tabBar.getTabName(tab) else "???"
                text("%02d$c Tab 0x%08X '$name' Offset: %.1f, Width: %.1f/%.1f", tabN, tab.id, tab.offset, tab.width, tab.contentWidth)
                popID()
            }
            treePop()
        }
    }

    companion object {
        fun debugNodeTableGetSizingPolicyDesc(sizingPolicy: TableFlags): String =
            when (sizingPolicy and TableFlag._SizingMask) {
                TableFlag.SizingFixedFit.i -> "FixedFit"
                TableFlag.SizingFixedSame.i -> "FixedSame"
                TableFlag.SizingStretchProp.i -> "StretchProp"
                TableFlag.SizingStretchSame.i -> "StretchSame"
                else -> "N/A"
            }

        /** Avoid naming collision with imgui_demo.cpp's HelpMarker() for unity builds. */
        fun metricsHelpMarker(desc: String) {
            textDisabled("(?)")
            if (isItemHovered()) {
                beginTooltip()
                pushTextWrapPos(fontSize * 35f)
                textUnformatted(desc)
                popTextWrapPos()
                endTooltip()
            }
        }
    }

    fun debugNodeTable(table: Table) {
        val isActive = table.lastFrameActive >= ImGui.frameCount - 2 // Note that fully clipped early out scrolling tables will appear as inactive here.
        val p = "Table 0x%08X (${table.columnsCount} columns, in '${table.outerWindow!!.name}')${if (isActive) "" else " *Inactive*"}".format(table.id)
        if (!isActive) pushStyleColor(Col.Text, Col.TextDisabled.u32)
        val open = treeNode(table, p)
        if (!isActive) popStyleColor()
        if (isItemHovered())
            foregroundDrawList.addRect(table.outerRect.min, table.outerRect.max, COL32(255, 255, 0, 255))
        if (isItemVisible && table.hoveredColumnBody != -1)
            foregroundDrawList.addRect(itemRectMin, itemRectMax, COL32(255, 255, 0, 255))
        if (!open)
            return
        val clearSettings = smallButton("Clear settings")
        bulletText("OuterRect: Pos: (%.1f,%.1f) Size: (%.1f,%.1f) Sizing: '${debugNodeTableGetSizingPolicyDesc(table.flags)}'", table.outerRect.min.x, table.outerRect.min.y, table.outerRect.width, table.outerRect.height)
        bulletText("ColumnsGivenWidth: %.1f, ColumnsAutoFitWidth: %.1f, InnerWidth: %.1f${if (table.innerWidth == 0f) " (auto)" else ""}", table.columnsGivenWidth, table.columnsAutoFitWidth, table.innerWidth)
        bulletText("CellPaddingX: %.1f, CellSpacingX: %.1f/%.1f, OuterPaddingX: %.1f", table.cellPaddingX, table.cellSpacingX1, table.cellSpacingX2, table.outerPaddingX)
        bulletText("HoveredColumnBody: ${table.hoveredColumnBody}, HoveredColumnBorder: ${table.hoveredColumnBorder}")
        bulletText("ResizedColumn: ${table.resizedColumn}, ReorderColumn: ${table.reorderColumn}, HeldHeaderColumn: ${table.heldHeaderColumn}")
        //BulletText("BgDrawChannels: %d/%d", 0, table->BgDrawChannelUnfrozen);
        var sumWeights = 0f
        for (n in 0 until table.columnsCount)
            if (table.columns[n].flags has TableColumnFlag.WidthStretch)
                sumWeights += table.columns[n].stretchWeight
        for (n in 0 until table.columnsCount) {
            val column = table.columns[n]
            val name = table getColumnName n
            val buf = StringBuilder()
            column.apply {
                buf += "Column $n order $displayOrder '$name': offset %+.2f to %+.2f${if (n < table.freezeColumnsRequest) " (Frozen)" else ""}\n".format(minX - table.workRect.min.x, maxX - table.workRect.min.x)
                buf += "Enabled: ${isEnabled.i}, VisibleX/Y: ${isVisibleX.i}/${isVisibleY.i}, RequestOutput: ${isRequestOutput.i}, SkipItems: ${isSkipItems.i}, DrawChannels: $drawChannelFrozen,$drawChannelUnfrozen\n"
                buf += "WidthGiven: %.1f, Request/Auto: %.1f/%.1f, StretchWeight: %.3f (%.1f%%)\n".format(widthGiven, widthRequest, widthAuto, stretchWeight, if (column.stretchWeight > 0f) (column.stretchWeight / sumWeights) * 100f else 0f)
                buf += "MinX: %.1f, MaxX: %.1f (%+.1f), ClipRect: %.1f to %.1f (+%.1f)\n".format(minX, maxX, maxX - minX, clipRect.min.x, clipRect.max.x, clipRect.max.x - clipRect.min.x)
                buf += "ContentWidth: %.1f,%.1f, HeadersUsed/Ideal %.1f/%.1f\n".format(contentMaxXFrozen - workMinX, contentMaxXUnfrozen - workMinX, contentMaxXHeadersUsed - workMinX, contentMaxXHeadersIdeal - workMinX)
                val dir = if (sortDirection == SortDirection.Ascending) " (Asc)" else if (sortDirection == SortDirection.Descending) " (Des)" else ""
                buf += "Sort: $sortOrder$dir, UserID: 0x%08X, Flags: 0x%04X: ".format(userID, flags)
                if (flags has TableColumnFlag.WidthStretch) buf += "WidthStretch "
                if (flags has TableColumnFlag.WidthFixed) buf += "WidthFixed "
                if (flags has TableColumnFlag.NoResize) buf += "NoResize "
                buf += ".."
            }
            bullet()
            selectable(buf.toString())
            if (isItemHovered()) {
                val r = Rect(column.minX, table.outerRect.min.y, column.maxX, table.outerRect.max.y)
                foregroundDrawList.addRect(r.min, r.max, COL32(255, 255, 0, 255))
            }
        }
        table.getBoundSettings()?.let(::debugNodeTableSettings)
        if (clearSettings)
            table.isResetAllRequest = true
        treePop()
    }

    fun debugNodeTableSettings(settings: TableSettings) {
        if (!treeNode(settings.id.L, "Settings 0x%08X (${settings.columnsCount} columns)", settings.id))
            return
        bulletText("SaveFlags: 0x%08X", settings.saveFlags)
        bulletText("ColumnsCount: ${settings.columnsCount} (max ${settings.columnsCountMax})")
        for (n in 0 until settings.columnsCount) {
            val columnSettings = settings.columnSettings[n]
            val sortDir = if (columnSettings.sortOrder != -1) columnSettings.sortDirection else SortDirection.None
            val dir = if (sortDir == SortDirection.Ascending) "Asc" else if (sortDir == SortDirection.Descending) "Des" else "---"
            val stretch = if (columnSettings.isStretch) "Weight" else "Width "
            bulletText("Column $n Order ${columnSettings.displayOrder} SortOrder ${columnSettings.sortOrder} $dir Vis ${columnSettings.isEnabled.d} $stretch %7.3f UserID 0x%08X",
                       columnSettings.widthOrWeight, columnSettings.userID)
        }
        treePop()
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
        bulletText("Pos: (%.1f,%.1f), Size: (%.1f,%.1f), ContentSize (%.1f,%.1f) Ideal (%.1f,%.1f)",
                   window.pos.x, window.pos.y, window.size.x, window.size.y,
                   window.contentSize.x, window.contentSize.y, window.contentSizeIdeal.x, window.contentSizeIdeal.y)
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
        for (layer in 0 until NavLayer.COUNT) {
            val r = window.navRectRel[layer]
            if (r.min.x >= r.max.y && r.min.y >= r.max.y) {
                bulletText("NavLastIds[$layer]: 0x%08X", window.navLastIds[layer])
                continue
            }
            bulletText("NavLastIds[$layer]: 0x%08X at +(%.1f,%.1f)(%.1f,%.1f)", window.navLastIds[layer], r.min.x, r.min.y, r.max.x, r.max.y)
            if (isItemHovered())
                getForegroundDrawList(window).addRect(r.min + window.pos, r.max + window.pos, COL32(255, 255, 0, 255))
        }
        bulletText("NavLayersActiveMask: %X, NavLastChildNavWindow: %s", window.dc.navLayersActiveMask, window.navLastChildNavWindow?.name ?: "NULL")
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

    fun debugNodeViewport(viewport: ViewportP) {
        setNextItemOpen(true, Cond.Once)
        if (treeNode("viewport0", "Viewport #0")) {
            val flags = viewport.flags
            bulletText("Main Pos: (%.0f,%.0f), Size: (%.0f,%.0f)\nWorkArea Offset Left: %.0f Top: %.0f, Right: %.0f, Bottom: %.0f",
                       viewport.pos.x, viewport.pos.y, viewport.size.x, viewport.size.y,
                       viewport.workOffsetMin.x, viewport.workOffsetMin.y, viewport.workOffsetMax.x, viewport.workOffsetMax.y)
            bulletText("Flags: 0x%04X =%s%s%s", viewport.flags,
                       if (flags has ViewportFlag.IsPlatformWindow) " IsPlatformWindow" else "",
                       if (flags has ViewportFlag.IsPlatformMonitor) " IsPlatformMonitor" else "",
                       if (flags has ViewportFlag.OwnedByApp) " OwnedByApp" else "")
            for (layer in viewport.drawDataBuilder!!.layers)
                for (drawListIdx in layer.indices)
                    debugNodeDrawList(null, layer[drawListIdx], "DrawList")
            treePop()
        }
    }

    fun debugRenderViewportThumbnail(drawList: DrawList, viewport: ViewportP, bb: Rect) {
        val window = g.currentWindow!!

        val scale = bb.size / viewport.size
        val off = bb.min - viewport.pos * scale
        val alphaMul = 1f
        window.drawList.addRectFilled(bb.min, bb.max, getColorU32(Col.Border, alphaMul * 0.4f))
        for (thumbWindow in g.windows) {
            if (!thumbWindow.wasActive || (thumbWindow.flags has WindowFlag._ChildWindow))
                continue

            var thumbR = thumbWindow.rect()
            var titleR = thumbWindow.titleBarRect()
            thumbR = Rect(floor(off + thumbR.min * scale), floor(off + thumbR.max * scale))
            titleR = Rect(floor(off + titleR.min * scale), floor(off + Vec2(titleR.max.x, titleR.min.y) * scale) + Vec2(0, 5)) // Exaggerate title bar height
            thumbR clipWithFull bb
            titleR clipWithFull bb
            val windowIsFocused = g.navWindow != null && thumbWindow.rootWindowForTitleBarHighlight == g.navWindow!!.rootWindowForTitleBarHighlight
            window.drawList.apply {
                addRectFilled(thumbR.min, thumbR.max, getColorU32(Col.WindowBg, alphaMul))
                addRectFilled(titleR.min, titleR.max, getColorU32(if (windowIsFocused) Col.TitleBgActive else Col.TitleBg, alphaMul))
                addRect(thumbR.min, thumbR.max, getColorU32(Col.Border, alphaMul))
                addText(g.font, g.fontSize * 1f, titleR.min, getColorU32(Col.Text, alphaMul), thumbWindow.name/*, findRenderedTextEnd(thumbWindow.name)*/)
            }
        }
        drawList.addRect(bb.min, bb.max, getColorU32(Col.Border, alphaMul))
    }
}