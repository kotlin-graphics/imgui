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

        // Apply constraints early
        // Compare both requested and actual given width to avoid overwriting requested width when column is stuck (minimum size, bounded)
        val minWidth = tableGetMinColumnWidth()
        val maxWidth = minWidth max (table getMaxColumnWidth columnN)
        column0Width = clamp(column0Width, minWidth, maxWidth)
        if (column0.widthGiven == column0Width || column0.widthRequest == column0Width)
            return

        //IMGUI_DEBUG_LOG("TableSetColumnWidth(%d, %.1f->%.1f)\n", column_0_idx, column_0->WidthGiven, column_0_width);
        var column1 = table.columns.getOrNull(column0.nextEnabledColumn)

        // In this surprisingly not simple because of how we support mixing Fixed and multiple Stretch columns.
        // - All fixed: easy.
        // - All stretch: easy.
        // - One or more fixed + one stretch: easy.
        // - One or more fixed + more than one stretch: tricky.
        //    // Qt when manual resize is enabled only support a single _trailing_ stretch column.

        // When forwarding resize from Wn| to Fn+1| we need to be considerate of the _NoResize flag on Fn+1.
        // FIXME-TABLE: Find a way to rewrite all of this so interactions feel more consistent for the user.
        // Scenarios:
        // - F1 F2 F3  resize from F1| or F2|   --> ok: alter ->WidthRequested of Fixed column. Subsequent columns will be offset.
        // - F1 F2 F3  resize from F3|          --> ok: alter ->WidthRequested of Fixed column. If active, ScrollX extent can be altered.
        // - F1 F2 W3  resize from F1| or F2|   --> ok: alter ->WidthRequested of Fixed column. If active, ScrollX extent can be altered, but it doesn't make much sense as the Stretch column will always be minimal size.
        // - F1 F2 W3  resize from W3|          --> ok: no-op (disabled by Resize Rule 1)
        // - W1 W2 W3  resize from W1| or W2|   --> ij
        // - W1 W2 W3  resize from W3|          --> ok: no-op (disabled by Resize Rule 1)
        // - W1 F2 F3  resize from F3|          --> ok: no-op (disabled by Resize Rule 1)
        // - W1 F2     resize from F2|          --> ok: no-op (disabled by Resize Rule 1)
        // - W1 W2 F3  resize from W1| or W2|   --> ok
        // - W1 F2 W3  resize from W1| or F2|   --> ok
        // - F1 W2 F3  resize from W2|          --> ok
        // - F1 W3 F2  resize from W3|          --> ok
        // - W1 F2 F3  resize from W1|          --> ok: equivalent to resizing |F2. F3 will not move.
        // - W1 F2 F3  resize from F2|          --> ok
        // All resizes from a Wx columns are locking other columns.

        // Possible improvements:
        // - W1 W2 W3  resize W1|               --> to not be stuck, both W2 and W3 would stretch down. Seems possible to fix. Would be most beneficial to simplify resize of all-weighted columns.
        // - W3 F1 F2  resize W3|               --> to not be stuck past F1|, both F1 and F2 would need to stretch down, which would be lossy or ambiguous. Seems hard to fix.

        // [Resize Rule 1] Can't resize from right of right-most visible column if there is any Stretch column. Implemented in TableUpdateLayout().

        // If we have all Fixed columns OR resizing a Fixed column that doesn't come after a Stretch one, we can do an offsetting resize.
        // This is the preferred resize path
        if (column0.flags has Tcf.WidthFixed)
            if (column1 == null || table.leftMostStretchedColumn == -1 || table.columns[table.leftMostStretchedColumn].displayOrder >= column0.displayOrder) {
                column0.widthRequest = column0Width
                table.isSettingsDirty = true
                return
            }

        // We can also use previous column if there's no next one (this is used when doing an auto-fit on the right-most stretch column)
        if (column1 == null)
            column1 = table.columns.getOrNull(column0.prevEnabledColumn) ?: return

        // Resizing from right-side of a Stretch column before a Fixed column forward sizing to left-side of fixed column.
        // (old_a + old_b == new_a + new_b) --> (new_a == old_a + old_b - new_b)
        val column1Width = (column1.widthRequest - (column0Width - column0.widthRequest)) max minWidth
        column0Width = column0.widthRequest + column1.widthRequest - column1Width
        assert(column0Width > 0f && column1Width > 0f)
        column0.widthRequest = column0Width
        column1.widthRequest = column1Width
        if ((column0.flags or column1.flags) has Tcf.WidthStretch)
            table.updateColumnsWeightFromWidth()
        table.isSettingsDirty = true
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

    /** Bg2 is used by Selectable (and possibly other widgets) to render to the background.
     *  Unlike our Bg0/1 channel which we uses for RowBg/CellBg/Borders and where we guarantee all shapes to be CPU-clipped, the Bg2 channel being widgets-facing will rely on regular ClipRect. */
    fun tablePushBackgroundChannel() {
        val window = g.currentWindow!!
        val table = g.currentTable!!

        // Optimization: avoid SetCurrentChannel() + PushClipRect()
        table.hostBackupInnerClipRect put window.clipRect
        setWindowClipRectBeforeSetChannel(window, table.bg2ClipRectForDrawCmd)
        table.drawSplitter.setCurrentChannel(window.drawList, table.bg2DrawChannelCurrent)
    }

    fun tablePopBackgroundChannel() {
        val window = g.currentWindow!!
        val table = g.currentTable!!
        val column = table.columns[table.currentColumn]

        // Optimization: avoid PopClipRect() + SetCurrentChannel()
        setWindowClipRectBeforeSetChannel(window, table.hostBackupInnerClipRect)
        table.drawSplitter.setCurrentChannel(window.drawList, column.drawChannelCurrent)
    }
}