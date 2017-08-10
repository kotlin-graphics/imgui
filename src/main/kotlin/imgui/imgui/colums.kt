package imgui.imgui

import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.buttonBehavior
import imgui.ImGui.currentWindow
import imgui.ImGui.currentWindowRead
import imgui.ImGui.isClippedEx
import imgui.ImGui.itemSize
import imgui.ImGui.keepAliveId
import imgui.ImGui.popClipRect
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.internal.ColumnData
import imgui.internal.Rect
import imgui.Context as g
import imgui.Context.style

/** You can also use SameLine(pos_x) for simplified columning. The columns API is still work-in-progress and rather
 *  lacking.    */
interface imgui_colums {

    /** setup number of columns. use an identifier to distinguish multiple column sets. close with Columns(1).  */
    fun columns(columnsCount: Int = 1, id: String = "", border: Boolean = true) {

        with(currentWindow) {

            assert(columnsCount >= 1)

            if (dc.columnsCount != 1) {
                if (dc.columnsCurrent != 0)
                    itemSize(Vec2(0))   // Advance to column 0
                popItemWidth()
                popClipRect()
                drawList.channelsMerge()

                dc.columnsCellMaxY = glm.max(dc.columnsCellMaxY, dc.cursorPos.y)
                dc.cursorPos.y = dc.columnsCellMaxY
            }

            // Draw columns borders and handle resize at the time of "closing" a columns set
            if (dc.columnsCount != columnsCount && dc.columnsCount != 1 && dc.columnsShowBorders && !skipItems) {
                val y1 = dc.columnsStartPosY
                val y2 = dc.cursorPos.y
                for (i in 1 until dc.columnsCount) {
                    var x = pos.x + getColumnOffset(i)
                    val columnId = dc.columnsSetId + i
                    val columnRect = Rect(Vec2(x - 4, y1), Vec2(x + 4, y2))
                    if (isClippedEx(columnRect, columnId, false))
                        continue

                    val (_, hovered, held) = buttonBehavior(columnRect, columnId)
                    if (hovered || held)
                        g.mouseCursor = MouseCursor.ResizeEW

                    // Draw before resize so our items positioning are in sync with the line being drawn
                    val col = ImGui.getColorU32(if (held) Col.ColumnActive else if (hovered) Col.ColumnHovered else Col.Column)
                    val xi = x.i.f
                    drawList.addLine(Vec2(xi, y1 + 1f), Vec2(xi, y2), col)

                    if (held) {
                        if (g.activeIdIsJustActivated)
                            g.activeIdClickOffset.x -= 4   // Store from center of column line (we used a 8 wide rect for columns clicking)
                        x = getDraggedColumnOffset(i)
                        setColumnOffset(i, x)
                    }
                }
            }

            // Differentiate column ID with an arbitrary prefix for cases where users name their columns set the same as another widget.
            // In addition, when an identifier isn't explicitly provided we include the number of columns in the hash to make it uniquer.
            pushId(0x11223347 + if (id.isNotEmpty()) 0 else columnsCount)
            dc.columnsSetId = getId(if (id.isNotEmpty()) id else "columns")
            popId()

            // Set state for first column
            dc.columnsCurrent = 0
            dc.columnsCount = columnsCount
            dc.columnsShowBorders = border

            val contentRegionWidth = if (sizeContentsExplicit.x != 0f) sizeContentsExplicit.x else size.x
            dc.columnsMinX = dc.indentX // Lock our horizontal range
            dc.columnsMaxX = contentRegionWidth - scroll.x - if (flags has WindowFlags.NoScrollbar) 0f else style.scrollbarSize// - window->WindowPadding().x;
            dc.columnsStartPosY = dc.cursorPos.y
            dc.columnsCellMaxY = dc.cursorPos.y
            dc.columnsCellMinY = dc.cursorPos.y
            dc.columnsOffsetX = 0f
            dc.cursorPos.x = (pos.x + dc.indentX + dc.columnsOffsetX).i.f

            if (dc.columnsCount != 1) {
                // Cache column offsets
                if (columnsCount + 1 > dc.columnsData.size)     // resize(columnsCount + 1)
                    for (i in dc.columnsData.size until columnsCount + 1) dc.columnsData.add(ColumnData())
                else
                    for (i in columnsCount + 1 until dc.columnsData.size ) dc.columnsData.removeAt(dc.columnsData.lastIndex)
                for (columnIndex in 0..columnsCount) {
                    val columnId = dc.columnsSetId + columnIndex
                    keepAliveId(columnId)
                    val defaultT = columnIndex / dc.columnsCount.f
                    // Cheaply store our floating point value inside the integer (could store a union into the map?)
                    val t = dc.stateStorage.float(columnId, defaultT)
                    dc.columnsData[columnIndex].offsetNorm = t
                }
                drawList.channelsSplit(dc.columnsCount)
                pushColumnClipRect()
                pushItemWidth(getColumnWidth() * 0.65f)
            } else dc.columnsData.clear()
        }
    }

    /** next column */
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

    /** get position of column line (in pixels, from the left side of the contents region). pass -1 to use current
     *  column, otherwise 0..GetcolumnsCount() inclusive. column 0 is usually 0.0f and not resizable unless you call
     *  this    */
    fun getColumnOffset(columnIndex: Int = -1): Float {

        val window = currentWindowRead!!
        var columnIndex = columnIndex
        if (columnIndex < 0)
            columnIndex = window.dc.columnsCurrent

        if (g.activeId != 0) {
            val columnId = window.dc.columnsSetId + columnIndex
            if (g.activeId == columnId)
                return getDraggedColumnOffset(columnIndex)
        }

        assert(columnIndex < window.dc.columnsData.size)
        val t = window.dc.columnsData[columnIndex].offsetNorm
        val xOffset = window.dc.columnsMinX + t * (window.dc.columnsMaxX - window.dc.columnsMinX)
        return xOffset.i.f
    }

    /** set position of column line (in pixels, from the left side of the contents region). pass -1 to use current
     *  column  */
    fun setColumnOffset(columnIndex: Int, offset: Float) {
        val window = currentWindow
        var columnIndex = columnIndex
        if (columnIndex < 0)
            columnIndex = window.dc.columnsCurrent

        assert(columnIndex < window.dc.columnsData.size)
        val t = (offset - window.dc.columnsMinX) / (window.dc.columnsMaxX - window.dc.columnsMinX)
        window.dc.columnsData[columnIndex].offsetNorm = t

        val columnId = window.dc.columnsSetId + columnIndex
        window.dc.stateStorage[columnId] = t
    }

    /** column width (== GetColumnOffset(GetColumnIndex()+1) - GetColumnOffset(GetColumnOffset())   */
    fun getColumnWidth(columnIndex: Int = -1): Float {

        val window = currentWindowRead!!
        var columnIndex = columnIndex
        if (columnIndex < 0)
            columnIndex = window.dc.columnsCurrent;

        return getColumnOffset(columnIndex + 1) - getColumnOffset(columnIndex)
    }

    /** number of columns (what was passed to Columns())    */
    val columnsCount get() = currentWindowRead!!.dc.columnsCount
}