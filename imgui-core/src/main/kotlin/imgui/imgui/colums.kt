package imgui.imgui

import glm_.f
import glm_.glm
import glm_.i
import imgui.*
import imgui.ImGui.beginColumns
import imgui.ImGui.currentWindow
import imgui.ImGui.currentWindowRead
import imgui.ImGui.endColumns
import imgui.ImGui.popClipRect
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushColumnClipRect
import imgui.ImGui.pushItemWidth
import imgui.ImGui.style
import imgui.internal.*
import kotlin.math.max
import kotlin.math.min
import imgui.internal.ColumnsFlag as Cf

/** Columns
 *  - You can also use SameLine(pos_x) to mimic simplified columns.
 *  - The columns API is work-in-progress and rather lacking (columns are arguably the worst part of dear imgui at the moment!) */
interface imgui_colums {

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

        with(window) {
            columns.lineMaxY = max(columns.lineMaxY, dc.cursorPos.y)
            if (++columns.current < columns.count) {
                // Columns 1+ cancel out IndentX
                // FIXME-COLUMNS: Unnecessary, could be locked?
                dc.columnsOffset = getColumnOffset(columns.current) - dc.indent + style.itemSpacing.x
                drawList.channelsSetCurrent(columns.current + 1)
            } else {
                // New row/line
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
        pushItemWidth(getColumnWidth() * 0.65f)  // FIXME-COLUMNS: Move on columns setup
    }

    /** get current column index    */
    val columnIndex: Int
        get() = currentWindowRead!!.dc.currentColumns?.current ?: 0

    /** get column width (in pixels). pass -1 to use current column   */
    fun getColumnWidth(columnIndex_: Int = -1): Float {

        val window = currentWindowRead!!
        val columns = window.dc.currentColumns!!
        val columnIndex = if (columnIndex_ < 0) columns.current else columnIndex_
        return offsetNormToPixels(columns, columns.columns[columnIndex + 1].offsetNorm - columns.columns[columnIndex].offsetNorm)
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
        val columns = window.dc.currentColumns!!

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
        columns.columns[columnIndex].offsetNorm = pixelsToOffsetNorm(columns, offset - columns.offMinX)

        if (preserveWidth)
            setColumnOffset(columnIndex + 1, offset + glm.max(style.columnsMinSpacing, width))
    }

    /** number of columns (what was passed to Columns())    */
    val columnsCount: Int
        get() = currentWindowRead!!.dc.currentColumns?.count ?: 1

    companion object {

        fun offsetNormToPixels(columns: Columns, offsetNorm: Float) = offsetNorm * (columns.offMaxX - columns.offMinX)

        fun pixelsToOffsetNorm(columns: Columns, offset: Float) = offset / (columns.offMaxX - columns.offMinX)

        val COLUMNS_HIT_RECT_HALF_WIDTH = 4f

        fun getColumnWidthEx(columns: Columns, columnIndex_: Int, beforeResize: Boolean = false): Float {
            val columnIndex = if (columnIndex_ < 0) columns.current else columnIndex_

            val offsetNorm =
                    if (beforeResize)
                        columns.columns[columnIndex + 1].offsetNormBeforeResize - columns.columns[columnIndex].offsetNormBeforeResize
                    else
                        columns.columns[columnIndex + 1].offsetNorm - columns.columns[columnIndex].offsetNorm
            return offsetNormToPixels(columns, offsetNorm)
        }
    }
}