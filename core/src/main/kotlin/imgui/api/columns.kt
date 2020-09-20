package imgui.api

import glm_.glm
import glm_.max
import imgui.ImGui.beginColumns
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.currentWindow
import imgui.ImGui.currentWindowRead
import imgui.ImGui.endColumns
import imgui.ImGui.getColumnOffset
import imgui.ImGui.io
import imgui.ImGui.popClipRect
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushColumnClipRect
import imgui.ImGui.pushItemWidth
import imgui.ImGui.setWindowClipRectBeforeSetChannel
import imgui.ImGui.style
import imgui.internal.*
import imgui.internal.sections.Columns
import imgui.internal.sections.ColumnsFlags
import imgui.internal.sections.has
import imgui.internal.sections.hasnt
import kotlin.math.max
import kotlin.math.min
import imgui.internal.sections.ColumnsFlag as Cf

/** Columns
 *  - You can also use SameLine(pos_x) to mimic simplified columns.
 *  - The columns API is work-in-progress and rather lacking (columns are arguably the worst part of dear imgui at the moment!)
 *  - There is a maximum of 64 columns.
 *  - Currently working on new 'Tables' api which will replace columns around Q2 2020 (see GitHub #2957). */
interface columns {

    /** [2017/12: This is currently the only public API, while we are working on making BeginColumns/EndColumns user-facing]    */
    fun columns(columnsCount: Int = 1, id: String = "", border: Boolean = true) {

        val window = currentWindow
        assert(columnsCount >= 1)

        val flags: ColumnsFlags = if (border) Cf.None.i else Cf.NoBorder.i
        //flags |= ImGuiColumnsFlags_NoPreserveWidths; // NB: Legacy behavior
        window.dc.currentColumns?.let {
            if (it.count == columnsCount && it.flags == flags)
                return

            endColumns()
        }

        if (columnsCount != 1)
            beginColumns(id, columnsCount, flags)
    }

    /** next column, defaults to current row or next row if the current row is finished */
    fun nextColumn() {

        val window = currentWindow
        if (window.skipItems || window.dc.currentColumns == null) return

        val columns = window.dc.currentColumns!!

        if (columns.count == 1) {
            window.dc.cursorPos.x = floor(window.pos.x + window.dc.indent + window.dc.columnsOffset)
            assert(columns.current == 0)
            return
        }

        // Next column
        if (++columns.current == columns.count)
            columns.current = 0

        popItemWidth()

        // Optimization: avoid PopClipRect() + SetCurrentChannel() + PushClipRect()
        // (which would needlessly attempt to update commands in the wrong channel, then pop or overwrite them),
        val column = columns.columns[columns.current]
        setWindowClipRectBeforeSetChannel(window, column.clipRect)
        columns.splitter.setCurrentChannel(window.drawList, columns.current + 1)

        val columnPadding = style.itemSpacing.x
        with(window) {
            columns.lineMaxY = max(columns.lineMaxY, dc.cursorPos.y)
            if (columns.current > 0) {
                // Columns 1+ ignore IndentX (by canceling it out)
                // FIXME-COLUMNS: Unnecessary, could be locked?
                dc.columnsOffset = getColumnOffset(columns.current) - dc.indent + columnPadding
            } else {
                // New row/line: column 0 honor IndentX.
                dc.columnsOffset = (columnPadding - window.windowPadding.x) max 0f
                columns.lineMinY = columns.lineMaxY
            }
            dc.cursorPos.x = floor(pos.x + dc.indent + dc.columnsOffset)
            dc.cursorPos.y = columns.lineMinY
            dc.currLineSize.y = 0f
            dc.currLineTextBaseOffset = 0f
        }

        // FIXME-COLUMNS: Share code with BeginColumns() - move code on columns setup.
        val offset0 = getColumnOffset(columns.current)
        val offset1 = getColumnOffset(columns.current + 1)
        val width = offset1 - offset0
        pushItemWidth(width * 0.65f)
        window.workRect.max.x = window.pos.x + offset1 - columnPadding
    }

    /** get current column index
     *  ~GetColumnIndex */
    val columnIndex: Int
        get() = currentWindowRead!!.dc.currentColumns?.current ?: 0

    /** get column width (in pixels). pass -1 to use current column   */
    fun getColumnWidth(columnIndex_: Int = -1): Float {

        val window = g.currentWindow!!
        val columns = window.dc.currentColumns ?: return contentRegionAvail.x
        val columnIndex = if (columnIndex_ < 0) columns.current else columnIndex_
        return columns.getOffsetFrom(columns.columns[columnIndex + 1].offsetNorm - columns.columns[columnIndex].offsetNorm)
    }

    /** set column width (in pixels). pass -1 to use current column */
    fun setColumnWidth(columnIndex_: Int, width: Float) {
        val window = currentWindowRead!!
        val columns = window.dc.currentColumns!!
        val columnIndex = if (columnIndex_ < 0) columns.current else columnIndex_

        setColumnOffset(columnIndex + 1, getColumnOffset(columnIndex) + width)
    }

    /** get position of column line (in pixels, from the left side of the contents region). pass -1 to use current
     *  column, otherwise 0..GetColumnsCount() inclusive. column 0 is typically 0.0f    */
    fun getColumnOffset(columnIndex_: Int = -1): Float {

        val window = currentWindowRead!!
        val columns = window.dc.currentColumns ?: return 0f

        val columnIndex = if (columnIndex_ < 0) columns.current else columnIndex_

        val t = columns.columns[columnIndex].offsetNorm
        return lerp(columns.offMinX, columns.offMaxX, t) // xOffset
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

    /** number of columns (what was passed to Columns())
     *  ~GetColumnsCount */
    val columnsCount: Int
        get() = currentWindowRead!!.dc.currentColumns?.count ?: 1


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