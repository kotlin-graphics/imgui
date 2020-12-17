package imgui.internal.api

import glm_.has
import glm_.max
import imgui.*
import imgui.ImGui.calcTextSize
import imgui.api.g
import imgui.internal.hashStr
import imgui.ImGui.openPopupEx
import imgui.ImGui.setWindowClipRectBeforeSetChannel
import imgui.ImGui.style
import imgui.ImGui.tableGetColumnCount
import imgui.ImGui.tableGetColumnFlags
import imgui.ImGui.tableGetColumnName
import imgui.ImGui.tableGetMinColumnWidth
import imgui.internal.classes.TableColumnIdx
import imgui.TableColumnFlag as Tcf
import imgui.TableFlag as Tf

// Tables: Candidates for public API
interface tablesCandidatesForPublicAPI {

    //-------------------------------------------------------------------------
    // [SECTION] Tables: Context Menu
    //-------------------------------------------------------------------------
    // - TableOpenContextMenu() [Internal]
    // - TableDrawContextMenu() [Internal]
    //-------------------------------------------------------------------------

    /** Use -1 to open menu not specific to a given column. */
    fun tableOpenContextMenu(columnN_: Int = -1) {
        var columnN = columnN_
        val table = g.currentTable!!
        if (columnN == -1 && table.currentColumn != -1)   // When called within a column automatically use this one (for consistency)
            columnN = table.currentColumn
        if (columnN == table.columnsCount)                // To facilitate using with TableGetHoveredColumn()
            columnN = -1
        assert(columnN >= -1 && columnN < table.columnsCount)
        if (table.flags has (Tf.Resizable or Tf.Reorderable or Tf.Hideable)) {
            table.isContextPopupOpen = true
            table.contextPopupColumn = columnN
            table.instanceInteracted = table.instanceCurrent
            val contextMenuId = hashStr("##ContextMenu", 0, table.id)
            openPopupEx(contextMenuId, PopupFlag.None.i)
        }
    }

    /** 'width' = inner column width, without padding */
    fun tableSetColumnWidth(columnN: Int, width: Float)     {
        val table = g.currentTable
        check(table != null && !table.isLayoutLocked)
        assert(columnN >= 0 && columnN < table.columnsCount)
        val column0 = table.columns[columnN]
        var column0Width = width

        // Constraints
        val minWidth = tableGetMinColumnWidth()
        val maxWidth0 = when {
            table.flags has Tf.ScrollX -> Float.MAX_VALUE
            else -> (table.workRect.max.x - column0.minX) - (table.columnsEnabledCount - (column0.indexWithinEnabledSet + 1)) * minWidth
        }
        column0Width = clamp(column0Width, minWidth, maxWidth0)

        // Compare both requested and actual given width to avoid overwriting requested width when column is stuck (minimum size, bounded)
        if (column0.widthGiven == column0Width || column0.widthRequest == column0Width)
            return

        var column1 = table.columns.getOrNull(column0.nextEnabledColumn)

        // In this surprisingly not simple because of how we support mixing Fixed and multiple Stretch columns.
        // - All fixed: easy.
        // - All stretch: easy.
        // - One or more fixed + one stretch: easy.
        // - One or more fixed + more than one stretch: A MESS

        // When forwarding resize from Wn| to Fn+1| we need to be considerate of the _NoResize flag on Fn+1.
        // FIXME-TABLE: Find a way to rewrite all of this so interactions feel more consistent for the user.
        // Scenarios:
        // - F1 F2 F3  resize from F1| or F2|   --> ok: alter ->WidthRequested of Fixed column. Subsequent columns will be offset.
        // - F1 F2 F3  resize from F3|          --> ok: alter ->WidthRequested of Fixed column. If active, ScrollX extent can be altered.
        // - F1 F2 W3  resize from F1| or F2|   --> ok: alter ->WidthRequested of Fixed column. If active, ScrollX extent can be altered, but it doesn't make much sense as the Stretch column will always be minimal size.
        // - F1 F2 W3  resize from W3|          --> ok: no-op (disabled by Resize Rule 1)
        // - W1 W2 W3  resize from W1| or W2|   --> FIXME
        // - W1 W2 W3  resize from W3|          --> ok: no-op (disabled by Resize Rule 1)
        // - W1 F2 F3  resize from F3|          --> ok: no-op (disabled by Resize Rule 1)
        // - W1 F2     resize from F2|          --> ok: no-op (disabled by Resize Rule 1)
        // - W1 W2 F3  resize from W1| or W2|   --> ok
        // - W1 F2 W3  resize from W1| or F2|   --> FIXME
        // - F1 W2 F3  resize from W2|          --> ok
        // - W1 F2 F3  resize from W1|          --> ok: equivalent to resizing |F2. F3 will not move. (forwarded by Resize Rule 2)
        // - W1 F2 F3  resize from F2|          --> FIXME should resize F2, F3 and not have effect on W1 (Stretch columns are _before_ the Fixed column).

        // Rules:
        // - [Resize Rule 1] Can't resize from right of right-most visible column if there is any Stretch column. Implemented in TableUpdateLayout().
        // - [Resize Rule 2] Resizing from right-side of a Stretch column before a fixed column forward sizing to left-side of fixed column.
        // - [Resize Rule 3] If we are are followed by a fixed column and we have a Stretch column before, we need to ensure that our left border won't move.
        table.isSettingsDirty = true
        if (column0.flags has Tcf.WidthFixed) {
            // [Resize Rule 3] If we are are followed by a fixed column and we have a Stretch column before, we need to ensure
            // that our left border won't move, which we can do by making sure column_a/column_b resizes cancels each others.
            if (column1 != null && column1.flags has Tcf.WidthFixed)
                if (table.leftMostStretchedColumn != -1 && table.columns[table.leftMostStretchedColumn].displayOrder < column0.displayOrder) {
                // (old_a + old_b == new_a + new_b) --> (new_a == old_a + old_b - new_b)
                    val column1Width = (column1.widthRequest - (column0Width - column0.widthRequest)) max minWidth
                    column0Width = column0.widthRequest + column1.widthRequest - column1Width
                    column1.widthRequest = column1Width
            }

            // Apply
            //IMGUI_DEBUG_LOG("TableSetColumnWidth(%d, %.1f->%.1f)\n", column_0_idx, column_0->WidthRequested, column_0_width);
            column0.widthRequest = column0Width
        }
        else if (column0.flags has Tcf.WidthStretch) {
            // We can also use previous column if there's no next one
            if (column1 == null)
                column1 = table.columns.getOrNull(column0.prevEnabledColumn) ?: return

            if (column1.flags has Tcf.WidthFixed) {
                // [Resize Rule 2]
                val off = column0.widthGiven - column0Width
                val column1Width = column1.widthGiven + off
                column1.widthRequest = minWidth max column1Width
            }
            else {
                // (old_a + old_b == new_a + new_b) --> (new_a == old_a + old_b - new_b)
                val column1Width = (column1.widthRequest - (column0Width - column0.widthRequest)) max minWidth
                column0Width = column0.widthRequest + column1.widthRequest - column1Width
                column1.widthRequest = column1Width
                column0.widthRequest = column0Width
                table.updateColumnsWeightFromWidth()
            }
        }
    }

    /** Note that the NoSortAscending/NoSortDescending flags are processed in TableSortSpecsSanitize(), and they may change/revert
     *  the value of SortDirection. We could technically also do it here but it would be unnecessary and duplicate code. */
    fun tableSetColumnSortDirection(columnN: Int, sortDirection: SortDirection, appendToSortSpecs_: Boolean) {

        var appendToSortSpecs = appendToSortSpecs_
        val table = g.currentTable!!

        if (table.flags hasnt Tf.SortMulti)
            appendToSortSpecs = false
        if (table.flags hasnt Tf.SortTristate)
            assert(sortDirection != SortDirection.None)

        var sortOrderMax: TableColumnIdx = 0
        if (appendToSortSpecs)
            for (otherColumnN in 0 until table.columnsCount)
                sortOrderMax = sortOrderMax max table.columns[otherColumnN].sortOrder

        val column = table.columns[columnN]
        column.sortDirection = sortDirection
        if (column.sortDirection == SortDirection.None)
            column.sortOrder = -1
        else if (column.sortOrder == -1 || !appendToSortSpecs)
            column.sortOrder = if(appendToSortSpecs) sortOrderMax + 1 else 0

        for (otherColumnN in 0 until table.columnsCount) {
            val otherColumn = table.columns[otherColumnN]
            if (otherColumn !== column && !appendToSortSpecs)
                otherColumn.sortOrder = -1
            table fixColumnSortDirection otherColumn
        }
        table.isSettingsDirty = true
        table.isSortSpecsDirty = true
    }

    /** May use (TableGetColumnFlags() & ImGuiTableColumnFlags_IsHovered) instead. Return hovered column. return -1 when table is not hovered. return columns_count if the unused space at the right of visible columns is hovered.
     *
     *  Return -1 when table is not hovered. return columns_count if the unused space at the right of visible columns is hovered. */
    fun tableGetHoveredColumn(): Int = g.currentTable?.hoveredColumnBody ?: -1

    fun tableGetHeaderRowHeight(): Float {
        // Caring for a minor edge case:
        // Calculate row height, for the unlikely case that some labels may be taller than others.
        // If we didn't do that, uneven header height would highlight but smaller one before the tallest wouldn't catch input for all height.
        // In your custom header row you may omit this all together and just call TableNextRow() without a height...
        var rowHeight = ImGui.textLineHeight
        val columnsCount = tableGetColumnCount()
        for (columnN in 0 until columnsCount)
        if (tableGetColumnFlags(columnN) has Tcf.IsEnabled)
            rowHeight = rowHeight max calcTextSize(tableGetColumnName(columnN)!!).y
        rowHeight += style.cellPadding.y * 2f
        return rowHeight
    }

    fun tablePushBackgroundChannel() {
        val window = g.currentWindow!!
        val table = g.currentTable!!

        // Optimization: avoid SetCurrentChannel() + PushClipRect()
        table.hostBackupClipRect put window.clipRect
        setWindowClipRectBeforeSetChannel(window, table.bgClipRectForDrawCmd)
        table.drawSplitter.setCurrentChannel(window.drawList, table.bg1DrawChannelCurrent)
    }

    fun tablePopBackgroundChannel() {
        val window = g.currentWindow!!
        val table = g.currentTable!!
        val column = table.columns[table.currentColumn]

        // Optimization: avoid PopClipRect() + SetCurrentChannel()
        setWindowClipRectBeforeSetChannel(window, table.hostBackupClipRect)
        table.drawSplitter.setCurrentChannel(window.drawList, column.drawChannelCurrent)
    }
}