package imgui.imgui.widgets

import glm_.*
import glm_.vec2.Vec2
import imgui.Col
import imgui.ColumnsFlags
import imgui.ID
import imgui.ImGui.buttonBehavior
import imgui.ImGui.currentWindow
import imgui.ImGui.currentWindowRead
import imgui.ImGui.getColumnOffset
import imgui.ImGui.getOffsetFrom
import imgui.ImGui.io
import imgui.ImGui.isClippedEx
import imgui.ImGui.keepAliveID
import imgui.ImGui.popClipRect
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushClipRect
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.ImGui.style
import imgui.MouseCursor
import imgui.imgui.g
import imgui.internal.*
import kotlin.math.max
import kotlin.math.min
import imgui.internal.ColumnsFlag as Cf

//-------------------------------------------------------------------------
// [SECTION] Widgets: Columns, BeginColumns, EndColumns, etc.
// In the current version, Columns are very weak. Needs to be replaced with a more full-featured system.
//-------------------------------------------------------------------------
interface imgui_widgets_columns {

    /** get current column index
     *  ~GetColumnIndex */
    val columnIndex: Int
        get() = currentWindowRead!!.dc.currentColumns?.current ?: 0

    /** number of columns (what was passed to Columns())
     *  ~GetColumnsCount */
    val columnsCount: Int
        get() = currentWindowRead!!.dc.currentColumns?.count ?: 1

    /** ~GetColumnOffsetFromNorm    */
    infix fun Columns.getOffsetFrom(offsetNorm: Float): Float = offsetNorm * (offMaxX - offMinX)

    /** ~GetColumnNormFromOffset    */
    fun Columns.getNormFrom(offset: Float): Float = offset / (offMaxX - offMinX)

    /** get position of column line (in pixels, from the left side of the contents region). pass -1 to use current
     *  column, otherwise 0..GetColumnsCount() inclusive. column 0 is typically 0.0f    */
    fun getColumnOffset(columnIndex_: Int = -1): Float {

        val window = currentWindowRead!!
        val columns = window.dc.currentColumns!!

        val columnIndex = if (columnIndex_ < 0) columns.current else columnIndex_

        val t = columns.columns[columnIndex].offsetNorm
        return lerp(columns.offMinX, columns.offMaxX, t) // xOffset
    }

    /** get column width (in pixels). pass -1 to use current column   */
    fun getColumnWidth(columnIndex_: Int = -1): Float {

        val window = currentWindowRead!!
        val columns = window.dc.currentColumns!!
        val columnIndex = if (columnIndex_ < 0) columns.current else columnIndex_
        return columns.getOffsetFrom(columns.columns[columnIndex + 1].offsetNorm - columns.columns[columnIndex].offsetNorm)
    }

    /** set position of column line (in pixels, from the left side of the contents region). pass -1 to use current column  */
    fun setColumnOffset(columnIndex_: Int, offset_: Float) {
        val window = g.currentWindow!!
        val columns = window.dc.currentColumns!!

        val columnIndex = if (columnIndex_ < 0) columns.current else columnIndex_
        assert(columnIndex < columns.columns.size)

        val preserveWidth = columns.flags hasnt Cf.NoPreserveWidths && columnIndex < columns.count - 1
        val width = if (preserveWidth) getColumnWidthEx(columns, columnIndex, columns.isBeingResized) else 0f

        val offset = if (columns.flags has Cf.NoForceWithinWindow) offset_
        else min(offset_, columns.offMaxX - style.columnsMinSpacing * (columns.count - columnIndex))
        columns.columns[columnIndex].offsetNorm = columns.getNormFrom(offset - columns.offMinX)

        if (preserveWidth)
            setColumnOffset(columnIndex + 1, offset + glm.max(style.columnsMinSpacing, width))
    }

    /** set column width (in pixels). pass -1 to use current column */
    fun setColumnWidth(columnIndex_: Int, width: Float) {
        val window = currentWindowRead!!
        val columns = window.dc.currentColumns!!
        val columnIndex = if (columnIndex_ < 0) columns.current else columnIndex_

        setColumnOffset(columnIndex + 1, getColumnOffset(columnIndex) + width)
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
        window.drawList.channelsSetCurrent(0)
        val cmdSize = window.drawList.cmdBuffer.size
        pushClipRect(columns.hostClipRect.min, columns.hostClipRect.max, false)
        assert(cmdSize == window.drawList.cmdBuffer.size) { "Being in channel 0 this should not have created an ImDrawCmd" }
    }

    fun popColumnsBackground() {
        val window = currentWindowRead!!
        val columns = window.dc.currentColumns!!
        window.drawList.channelsSetCurrent(columns.current + 1)
        popClipRect()
    }

    // findOrCreateColumns is in Window class

    fun getColumnsID(strId: String, columnsCount: Int): ID {

        val window = g.currentWindow!!

        // Differentiate column ID with an arbitrary prefix for cases where users name their columns set the same as another widget.
        // In addition, when an identifier isn't explicitly provided we include the number of columns in the hash to make it uniquer.
        pushId(0x11223347 + if (strId.isNotEmpty()) 0 else columnsCount)
        return window.getId(if (strId.isNotEmpty()) strId else "columns")
                .also { popId() }
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

        // Set state for first column
        val columnPadding = style.itemSpacing.x
        columns.apply {
            offMinX = window.dc.indent - columnPadding
            offMaxX = (window.workRect.max.x - window.pos.x) max (offMinX + 1f)
            hostCursorPosY = window.dc.cursorPos.y
            hostCursorMaxPosX = window.dc.cursorMaxPos.x
            hostClipRect put window.clipRect
            hostWorkRect put window.workRect
            lineMinY = window.dc.cursorPos.y
            lineMaxY = lineMinY
        }
        window.dc.columnsOffset = 0f
        window.dc.cursorPos.x = (window.pos.x + window.dc.indent + window.dc.columnsOffset).i.f

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
            val clipX1 = floor(0.5f + window.pos.x + getColumnOffset(n))
            val clipX2 = floor(0.5f + window.pos.x + getColumnOffset(n + 1) - 1f)
            column.clipRect = Rect(clipX1, -Float.MAX_VALUE, clipX2, +Float.MAX_VALUE)
            column.clipRect clipWith window.clipRect
        }

        if (columns.count > 1) {
            window.drawList.channelsSplit(1 + columns.count)
            window.drawList.channelsSetCurrent(1)
            pushColumnClipRect(0)
        }

        val offset0 = getColumnOffset(columns.current)
        val offset1 = getColumnOffset(columns.current + 1)
        val width = offset1 - offset0
        pushItemWidth(width * 0.65f)
        window.workRect.max.x = window.pos.x + offset1 - columnPadding
    }

    /** next column, defaults to current row or next row if the current row is finished */
    fun nextColumn() {

        val window = currentWindow
        if (window.skipItems || window.dc.currentColumns == null) return

        val columns = window.dc.currentColumns!!

        if (columns.count == 1) {
            window.dc.cursorPos.x = (window.pos.x + window.dc.indent + window.dc.columnsOffset).i.f
            assert(columns.current == 0)
            return
        }

        popItemWidth()
        popClipRect()

        val columnPadding = style.itemSpacing.x
        with(window) {
            columns.lineMaxY = max(columns.lineMaxY, dc.cursorPos.y)
            if (++columns.current < columns.count) {
                // Columns 1+ ignore IndentX (by canceling it out)
                // FIXME-COLUMNS: Unnecessary, could be locked?
                dc.columnsOffset = getColumnOffset(columns.current) - dc.indent + columnPadding
                drawList.channelsSetCurrent(columns.current + 1)
            } else {
                // New row/line
                // Column 0 honor IndentX
                dc.columnsOffset = 0f
                drawList.channelsSetCurrent(1)
                columns.current = 0
                columns.lineMinY = columns.lineMaxY
            }
            dc.cursorPos.x = (pos.x + dc.indent + dc.columnsOffset).i.f
            dc.cursorPos.y = columns.lineMinY
            dc.currLineSize.y = 0f
            dc.currLineTextBaseOffset = 0f
        }
        pushColumnClipRect(columns.current)     // FIXME-COLUMNS: Could it be an overwrite?

        // FIXME-COLUMNS: Share code with BeginColumns() - move code on columns setup.
        val offset0 = getColumnOffset(columns.current)
        val offset1 = getColumnOffset(columns.current + 1)
        val width = offset1 - offset0
        pushItemWidth(width * 0.65f)
        window.workRect.max.x = window.pos.x + offset1 - columnPadding
    }

    fun endColumns() {

        val window = currentWindow
        val columns = window.dc.currentColumns!! // ~IM_ASSERT(columns != NULL)

        popItemWidth()
        if (columns.count > 1) {
            popClipRect()
            window.drawList.channelsMerge()
        }

        val flags = columns.flags
        columns.lineMaxY = columns.lineMaxY max window.dc.cursorPos.y
        window.dc.cursorPos.y = columns.lineMaxY
        if (flags hasnt Cf.GrowParentContentsSize)
            window.dc.cursorMaxPos.x = columns.hostCursorMaxPosX  // Restore cursor max pos, as columns don't grow parent

        // Draw columns borders and handle resize
        // The IsBeingResized flag ensure we preserve pre-resize columns width so back-and-forth are not lossy
        var isBeingResized = false
        if (flags hasnt Cf.NoBorder && !window.skipItems) {
            // We clip Y boundaries CPU side because very long triangles are mishandled by some GPU drivers.
            val y1 = columns.hostCursorPosY max window.clipRect.min.y
            val y2 = window.dc.cursorPos.y min window.clipRect.max.y
            var draggingColumn = -1
            for (n in 1 until columns.count) {
                val column = columns.columns[n]
                val x = window.pos.x + getColumnOffset(n)
                val columnId = columns.id + n
                val columnHitHw = COLUMNS_HIT_RECT_HALF_WIDTH
                val columnHitRect = Rect(Vec2(x - columnHitHw, y1), Vec2(x + columnHitHw, y2))
                keepAliveID(columnId)
                if (isClippedEx(columnHitRect, columnId, false))
                    continue

                var hovered = false
                var held = false
                if (flags hasnt Cf.NoResize) {
                    val (_, ho, he) = buttonBehavior(columnHitRect, columnId)
                    hovered = ho
                    held = he
                    if (hovered || held)
                        g.mouseCursor = MouseCursor.ResizeEW
                    if (held && column.flags hasnt Cf.NoResize)
                        draggingColumn = n
                }

                // Draw column
                val col = if (held) Col.SeparatorActive else if (hovered) Col.SeparatorHovered else Col.Separator
                val xi = x.i.f
                window.drawList.addLine(Vec2(xi, y1 + 1f), Vec2(xi, y2), col.u32)
            }

            // Apply dragging after drawing the column lines, so our rendered lines are in sync with how items were displayed during the frame.
            if (draggingColumn != -1) {
                if (!columns.isBeingResized)
                    for (n in 0..columns.count)
                        columns.columns[n].offsetNormBeforeResize = columns.columns[n].offsetNorm
                columns.isBeingResized = true
                isBeingResized = true
                val x = getDraggedColumnOffset(columns, draggingColumn)
                setColumnOffset(draggingColumn, x)
            }
        }
        columns.isBeingResized = isBeingResized

        window.apply {
            workRect put columns.hostWorkRect
            dc.currentColumns = null
            dc.columnsOffset = 0f
            dc.cursorPos.x = (pos.x + dc.indent + dc.columnsOffset).i.f
        }
    }

    /** [2017/12: This is currently the only public API, while we are working on making BeginColumns/EndColumns user-facing]    */
    fun columns(columnsCount: Int = 1, id: String = "", border: Boolean = true) {

        val window = currentWindow
        assert(columnsCount >= 1)

        val flags: ColumnsFlags = if (border) 0 else Cf.NoBorder.i
        //flags |= ImGuiColumnsFlags_NoPreserveWidths; // NB: Legacy behavior
        window.dc.currentColumns?.let {
            if (it.count == columnsCount && it.flags == flags)
                return

            endColumns()
        }

        if (columnsCount != 1)
            beginColumns(id, columnsCount, flags)
    }

    companion object {

        val COLUMNS_HIT_RECT_HALF_WIDTH = 4f

        fun getDraggedColumnOffset(columns: Columns, columnIndex: Int): Float {
            /*  Active (dragged) column always follow mouse. The reason we need this is that dragging a column to the right edge
                of an auto-resizing window creates a feedback loop because we store normalized positions. So while dragging we
                enforce absolute positioning.   */

            val window = g.currentWindow!!
            assert(columnIndex > 0) { "We are not supposed to drag column 0." }
            assert(g.activeId == columns.id + columnIndex/* as ID */)

            var x = io.mousePos.x - g.activeIdClickOffset.x + COLUMNS_HIT_RECT_HALF_WIDTH - window.pos.x
            x = glm.max(x, getColumnOffset(columnIndex - 1) + style.columnsMinSpacing)
            if (columns.flags has Cf.NoPreserveWidths)
                x = glm.min(x, getColumnOffset(columnIndex + 1) - style.columnsMinSpacing)

            return x
        }

        fun getColumnWidthEx(columns: Columns, columnIndex_: Int, beforeResize: Boolean = false): Float {
            val columnIndex = if (columnIndex_ < 0) columns.current else columnIndex_

            val offsetNorm = when {
                beforeResize -> columns.columns[columnIndex + 1].offsetNormBeforeResize - columns.columns[columnIndex].offsetNormBeforeResize
                else -> columns.columns[columnIndex + 1].offsetNorm - columns.columns[columnIndex].offsetNorm
            }
            return columns getOffsetFrom offsetNorm
        }
    }
}