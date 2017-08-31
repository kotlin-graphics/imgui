package imgui.imgui

import glm_.f
import glm_.glm
import glm_.i
import imgui.Context.style
import imgui.ImGui.beginColumns
import imgui.ImGui.currentWindow
import imgui.ImGui.currentWindowRead
import imgui.ImGui.endColumns
import imgui.ImGui.popClipRect
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushColumnClipRect
import imgui.ImGui.pushItemWidth
import imgui.internal.*
import imgui.Context as g

/** You can also use SameLine(pos_x) for simplified columning. The columns API is still work-in-progress and rather
 *  lacking.    */
interface imgui_colums {

    /** [2017/08: This is currently the only public API, while we are working on making BeginColumns/EndColumns user-facing]    */
    fun columns(columnsCount: Int = 1, id: String? = null, border: Boolean = true) {

        val window = currentWindow
        assert(columnsCount >= 1)

        if (window.dc.columnsCount != columnsCount && window.dc.columnsCount != 1)
            endColumns()

        val flags = if (border) 0 else ColumnsFlags.NoBorder.i
        //flags |= ImGuiColumnsFlags_NoPreserveWidths; // NB: Legacy behavior
        if (columnsCount != 1)
            beginColumns(id, columnsCount, flags)
    }

    /** next column, defaults to current row or next row if the current row is finished */
    fun nextColumn() {

        val window = currentWindow
        if (window.skipItems || window.dc.columnsCount <= 1) return

        popItemWidth()
        popClipRect()

        with(window) {
            dc.columnsCellMaxY = glm.max(dc.columnsCellMaxY, dc.cursorPos.y)
            if (++dc.columnsCurrent < dc.columnsCount) {
                // Columns 1+ cancel out IndentX
                dc.columnsOffsetX = getColumnOffset(dc.columnsCurrent) - dc.indentX + style.itemSpacing.x
                drawList.channelsSetCurrent(dc.columnsCurrent)
            } else {
                dc.columnsCurrent = 0
                dc.columnsOffsetX = 0f
                dc.columnsCellMinY = dc.columnsCellMaxY
                drawList.channelsSetCurrent(0)
            }
            dc.cursorPos.x = (pos.x + dc.indentX + dc.columnsOffsetX).i.f
            dc.cursorPos.y = dc.columnsCellMinY
            dc.currentLineHeight = 0f
            dc.currentLineTextBaseOffset = 0f
        }
        pushColumnClipRect()
        pushItemWidth(getColumnWidth() * 0.65f)  // FIXME: Move on columns setup
    }

    /** get current column index    */
    val columnIndex get() = currentWindowRead!!.dc.columnsCurrent

    /** get column width (in pixels). pass -1 to use current column   */
    fun getColumnWidth(columnIndex: Int = -1): Float {

        val window = currentWindowRead!!
        var columnIndex = columnIndex
        if (columnIndex < 0)
            columnIndex = window.dc.columnsCurrent

        val offset = window.dc.columnsData[columnIndex + 1].offsetNorm - window.dc.columnsData[columnIndex].offsetNorm
        return offsetNormToPixels(window, offset)
    }

    /** set column width (in pixels). pass -1 to use current column */
    fun setColumnWidth(columnIndex: Int, width: Float) {
        val window = currentWindowRead!!
        var columnIndex = columnIndex
        if (columnIndex < 0)
            columnIndex = window.dc.columnsCurrent

        setColumnOffset(columnIndex + 1, getColumnOffset(columnIndex) + width)
    }

    /** get position of column line (in pixels, from the left side of the contents region). pass -1 to use current
     *  column, otherwise 0..GetColumnsCount() inclusive. column 0 is typically 0.0f    */
    fun getColumnOffset(columnIndex: Int = -1): Float {

        val window = currentWindowRead!!
        var columnIndex = columnIndex
        if (columnIndex < 0)
            columnIndex = window.dc.columnsCurrent

//        if (g.activeId != 0) {
//            val columnId = window.dc.columnsSetId + columnIndex
//            if (g.activeId == columnId)
//                return getDraggedColumnOffset(columnIndex)
//        }

        assert(columnIndex < window.dc.columnsData.size)
        val t = window.dc.columnsData[columnIndex].offsetNorm
        return lerp(window.dc.columnsMinX, window.dc.columnsMaxX, t) // xOffset
    }

    /** set position of column line (in pixels, from the left side of the contents region). pass -1 to use current column  */
    fun setColumnOffset(columnIndex: Int, offset: Float) {
        val window = currentWindow
        var columnIndex = columnIndex
        if (columnIndex < 0)
            columnIndex = window.dc.columnsCurrent

        assert(columnIndex < window.dc.columnsData.size)
        val preserveWidth = !(window.dc.columnsFlags has ColumnsFlags.NoPreserveWidths) && (columnIndex < window.dc.columnsCount - 1)
        val width = if (preserveWidth) getColumnWidth(columnIndex) else 0f

        var offset = offset
        if (window.dc.columnsFlags hasnt ColumnsFlags.NoForceWithinWindow)
            offset = glm.min(offset, window.dc.columnsMaxX - style.columnsMinSpacing * (window.dc.columnsCount - columnIndex))
        val offsetNorm = pixelsToOffsetNorm(window, offset)

        val columnId = window.dc.columnsSetId + columnIndex
        window.dc.stateStorage[columnId] = offsetNorm

        window.dc.columnsData[columnIndex].offsetNorm = offsetNorm

        if (preserveWidth)
            setColumnOffset(columnIndex + 1, offset + glm.max(style.columnsMinSpacing, width))
    }

    /** number of columns (what was passed to Columns())    */
    val columnsCount get() = currentWindowRead!!.dc.columnsCount

    companion object {
        fun offsetNormToPixels(window: Window, offsetNorm: Float) = offsetNorm * (window.dc.columnsMaxX - window.dc.columnsMinX)
        fun pixelsToOffsetNorm(window: Window, offset: Float) = (offset - window.dc.columnsMinX) /
                (window.dc.columnsMaxX - window.dc.columnsMinX)
    }
}