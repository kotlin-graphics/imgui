package imgui.internal.api

import glm_.f
import glm_.max
import glm_.min
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.buttonBehavior
import imgui.ImGui.currentWindow
import imgui.ImGui.currentWindowRead
import imgui.ImGui.getColumnOffset
import imgui.ImGui.isClippedEx
import imgui.ImGui.keepAliveID
import imgui.ImGui.popClipRect
import imgui.ImGui.popID
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushClipRect
import imgui.ImGui.pushID
import imgui.ImGui.pushItemWidth
import imgui.ImGui.setColumnOffset
import imgui.ImGui.style
import imgui.api.g
import imgui.internal.classes.Rect
import imgui.internal.*
import imgui.internal.classes.Window
import imgui.internal.sections.ColumnData
import imgui.internal.sections.ColumnsFlag
import imgui.internal.sections.ColumnsFlags
import imgui.internal.sections.hasnt
import kotlin.math.max
import kotlin.math.min

/** Internal Columns API (this is not exposed because we will encourage transitioning to the Tables API) */
internal interface internalColumnsAPI {


    /** [Internal] Small optimization to avoid calls to PopClipRect/SetCurrentChannel/PushClipRect in sequences,
     * they would meddle many times with the underlying ImDrawCmd.
     * Instead, we do a preemptive overwrite of clipping rectangle _without_ altering the command-buffer and let
     * the subsequent single call to SetCurrentChannel() does it things once. */
    fun setWindowClipRectBeforeSetChannel(window: Window, clipRect: Rect) {
        val clipRectVec4 = clipRect.toVec4()
        window.clipRect put clipRect
        window.drawList._cmdHeader.clipRect = clipRectVec4 // safe, new instance
        window.drawList._clipRectStack[window.drawList._clipRectStack.lastIndex] put clipRectVec4
    }

    /** setup number of columns. use an identifier to distinguish multiple column sets. close with EndColumns().    */
    fun beginColumns(strId: String = "", columnsCount: Int, flags: ColumnsFlags) {

        val window = currentWindow

        assert(columnsCount >= 1)
        assert(window.dc.currentColumns == null) { "Nested columns are currently not supported" }

        // Acquire storage for the columns set
        val id = getColumnsID(strId, columnsCount)
        val columns = window.findOrCreateColumns(id)
        assert(columns.id == id)
        columns.current = 0
        columns.count = columnsCount
        columns.flags = flags
        window.dc.currentColumns = columns

        val columnPadding = style.itemSpacing.x
        val halfClipExtendX = floor((window.windowPadding.x * 0.5f) max window.windowBorderSize)
        val max1 = window.workRect.max.x + columnPadding - max(columnPadding - window.windowPadding.x, 0f)
        val max2 = window.workRect.max.x + halfClipExtendX
        columns.apply {

            hostCursorPosY = window.dc.cursorPos.y
            hostCursorMaxPosX = window.dc.cursorMaxPos.x
            hostInitialClipRect put window.clipRect
            hostBackupParentWorkRect put window.parentWorkRect
            window.parentWorkRect put window.workRect

            // Set state for first column
            // We aim so that the right-most column will have the same clipping width as other after being clipped by parent ClipRect
            offMinX = window.dc.indent - columnPadding + max(columnPadding - window.windowPadding.x, 0f)
            offMaxX = max(min(max1, max2) - window.pos.x, columns.offMinX + 1f)
            lineMinY = window.dc.cursorPos.y
            lineMaxY = lineMinY
        }

        // Clear data if columns count changed
        if (columns.columns.isNotEmpty() && columns.columns.size != columnsCount + 1)
            columns.columns.clear()

        // Initialize default widths
        columns.isFirstFrame = columns.columns.isEmpty()
        if (columns.columns.isEmpty())
            for (i in 0..columnsCount)
                columns.columns += ColumnData().apply { offsetNorm = i / columnsCount.f }

        for (n in 0 until columnsCount) {
            // Compute clipping rectangle
            val column = columns.columns[n]
            val clipX1 = round(window.pos.x + getColumnOffset(n))
            val clipX2 = round(window.pos.x + getColumnOffset(n + 1) - 1f)
            column.clipRect = Rect(clipX1, -Float.MAX_VALUE, clipX2, +Float.MAX_VALUE)
            column.clipRect clipWithFull window.clipRect
        }

        if (columns.count > 1) {
            columns.splitter.split(window.drawList, 1 + columns.count)
            columns.splitter.setCurrentChannel(window.drawList, 1)
            pushColumnClipRect(0)
        }

        // We don't generally store Indent.x inside ColumnsOffset because it may be manipulated by the user.
        val offset0 = getColumnOffset(columns.current)
        val offset1 = getColumnOffset(columns.current + 1)
        val width = offset1 - offset0
        pushItemWidth(width * 0.65f)
        window.dc.columnsOffset = (columnPadding - window.windowPadding.x) max 0f
        window.dc.cursorPos.x = floor(window.pos.x + window.dc.indent + window.dc.columnsOffset)
        window.workRect.max.x = window.pos.x + offset1 - columnPadding
    }

    fun endColumns() {

        val window = currentWindow
        val columns = window.dc.currentColumns!! // ~IM_ASSERT(columns != NULL)

        popItemWidth()
        if (columns.count > 1) {
            popClipRect()
            columns.splitter merge window.drawList
        }

        val flags = columns.flags
        columns.lineMaxY = columns.lineMaxY max window.dc.cursorPos.y
        window.dc.cursorPos.y = columns.lineMaxY
        if (flags hasnt ColumnsFlag.GrowParentContentsSize)
            window.dc.cursorMaxPos.x = columns.hostCursorMaxPosX  // Restore cursor max pos, as columns don't grow parent

        // Draw columns borders and handle resize
        // The IsBeingResized flag ensure we preserve pre-resize columns width so back-and-forth are not lossy
        var isBeingResized = false
        if (flags hasnt ColumnsFlag.NoBorder && !window.skipItems) {
            // We clip Y boundaries CPU side because very long triangles are mishandled by some GPU drivers.
            val y1 = columns.hostCursorPosY max window.clipRect.min.y
            val y2 = window.dc.cursorPos.y min window.clipRect.max.y
            var draggingColumn = -1
            for (n in 1 until columns.count) {
                val column = columns.columns[n]
                val x = window.pos.x + getColumnOffset(n)
                val columnId = columns.id + n
                val columnHitHw = imgui.api.columns.COLUMNS_HIT_RECT_HALF_WIDTH
                val columnHitRect = Rect(Vec2(x - columnHitHw, y1), Vec2(x + columnHitHw, y2))
                keepAliveID(columnId)
                if (isClippedEx(columnHitRect, columnId, false))
                    continue

                var hovered = false
                var held = false
                if (flags hasnt ColumnsFlag.NoResize) {
                    val (_, ho, he) = buttonBehavior(columnHitRect, columnId)
                    hovered = ho
                    held = he
                    if (hovered || held)
                        g.mouseCursor = MouseCursor.ResizeEW
                    if (held && column.flags hasnt ColumnsFlag.NoResize)
                        draggingColumn = n
                }

                // Draw column
                val col = if (held) Col.SeparatorActive else if (hovered) Col.SeparatorHovered else Col.Separator
                val xi = floor(x)
                window.drawList.addLine(Vec2(xi, y1 + 1f), Vec2(xi, y2), col.u32)
            }

            // Apply dragging after drawing the column lines, so our rendered lines are in sync with how items were displayed during the frame.
            if (draggingColumn != -1) {
                if (!columns.isBeingResized)
                    for (n in 0..columns.count)
                        columns.columns[n].offsetNormBeforeResize = columns.columns[n].offsetNorm
                columns.isBeingResized = true
                isBeingResized = true
                val x = imgui.api.columns.getDraggedColumnOffset(columns, draggingColumn)
                setColumnOffset(draggingColumn, x)
            }
        }
        columns.isBeingResized = isBeingResized

        window.apply {
            workRect put window.parentWorkRect
            parentWorkRect put columns.hostBackupParentWorkRect
            dc.currentColumns = null
            dc.columnsOffset = 0f
            dc.cursorPos.x = floor(pos.x + dc.indent + dc.columnsOffset)
        }
    }

    fun pushColumnClipRect(columnIndex_: Int) {

        val window = currentWindowRead!!
        val columns = window.dc.currentColumns!!
        val columnIndex = if (columnIndex_ < 0) columns.current else columnIndex_

        pushClipRect(columns.columns[columnIndex].clipRect.min, columns.columns[columnIndex].clipRect.max, false)
    }

    /** Get into the columns background draw command (which is generally the same draw command as before we called BeginColumns) */
    fun pushColumnsBackground() {
        val window = currentWindowRead!!
        val columns = window.dc.currentColumns!!
        if (columns.count == 1) return

        // Optimization: avoid SetCurrentChannel() + PushClipRect()
        columns.hostBackupClipRect put window.clipRect
        setWindowClipRectBeforeSetChannel(window, columns.hostInitialClipRect)
        columns.splitter.setCurrentChannel(window.drawList, 0)
    }

    fun popColumnsBackground() {
        val window = currentWindowRead!!
        val columns = window.dc.currentColumns!!
        if (columns.count == 1) return

        // Optimization: avoid PopClipRect() + SetCurrentChannel()
        setWindowClipRectBeforeSetChannel(window, columns.hostBackupClipRect)
        columns.splitter.setCurrentChannel(window.drawList, columns.current + 1)
    }

    fun getColumnsID(strId: String, columnsCount: Int): ID {

        val window = g.currentWindow!!

        // Differentiate column ID with an arbitrary prefix for cases where users name their columns set the same as another widget.
        // In addition, when an identifier isn't explicitly provided we include the number of columns in the hash to make it uniquer.
        pushID(0x11223347 + if (strId.isNotEmpty()) 0 else columnsCount)
        return window.getID(if (strId.isNotEmpty()) strId else "columns")
                .also { popID() }
    }

    // findOrCreateColumns is in Window class

    // GetColumnOffsetFromNorm and GetColumnNormFromOffset in Columns class
}