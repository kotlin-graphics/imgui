package imgui.api

import glm_.has
import glm_.hasnt
import glm_.max
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.beginTableEx
import imgui.ImGui.buttonBehavior
import imgui.ImGui.calcTextSize
import imgui.ImGui.endChild
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.getColorU32
import imgui.ImGui.getID
import imgui.ImGui.isItemHovered
import imgui.ImGui.isMouseDragging
import imgui.ImGui.isMouseReleased
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.popID
import imgui.ImGui.popStyleColor
import imgui.ImGui.pushID
import imgui.ImGui.pushStyleColor
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.renderText
import imgui.ImGui.renderTextEllipsis
import imgui.ImGui.setItemAllowOverlap
import imgui.ImGui.setTooltip
import imgui.ImGui.tableGetColumnNextSortDirection
import imgui.ImGui.tableGetHeaderRowHeight
import imgui.ImGui.tableGetHoveredColumn
import imgui.ImGui.tableOpenContextMenu
import imgui.ImGui.tableSetColumnSortDirection
import imgui.classes.TableSortSpecs
import imgui.internal.classes.COL32_DISABLE
import imgui.internal.classes.Rect
import imgui.internal.classes.TABLE_MAX_COLUMNS
import imgui.internal.classes.TABLE_RESIZE_SEPARATOR_HALF_THICKNESS
import imgui.internal.floor
import imgui.internal.sections.ButtonFlag
import imgui.internal.sections.NavHighlightFlag
import imgui.TableColumnFlag as Tcf
import imgui.TableFlag as Tf
import imgui.TableRowFlag as Trf

// Tables
// [BETA API] API may evolve slightly! If you use this, please update to the next version when it comes out!
// - Full-featured replacement for old Columns API.
// - See Demo->Tables for demo code.
// - See top of imgui_tables.cpp for general commentary.
// - See ImGuiTableFlags_ and ImGuiTableColumnFlags_ enums for a description of available flags.
// The typical call flow is:
// - 1. Call BeginTable()
// - 2. Optionally call TableSetupColumn() to submit column name/flags/defaults
// - 3. Optionally call TableSetupScrollFreeze() to request scroll freezing of columns/rows
// - 4. Optionally call TableHeadersRow() to submit a header row. Names will be pulled from data provided TableSetupColumn() calls)
// - 5. Populate contents
//    - In most situations you can use TableNextRow() + TableSetColumnIndex(N) to start appending into a column.
//    - If you are using tables as a sort of grid, where every columns is holding the same type of contents,
//      you may prefer using TableNextColumn() instead of TableNextRow() + TableSetColumnIndex().
//      TableNextColumn() will automatically wrap-around into the next row if needed.
//    - IMPORTANT: Comparatively to the old Columns() API, we need to call TableNextColumn() for the first column!
//    - Summary of possible call flow:
//        --------------------------------------------------------------------------------------------------------
//        TableNextRow() -> TableSetColumnIndex(0) -> Text("Hello 0") -> TableSetColumnIndex(1) -> Text("Hello 1")  // OK
//        TableNextRow() -> TableNextColumn()      -> Text("Hello 0") -> TableNextColumn()      -> Text("Hello 1")  // OK
//                          TableNextColumn()      -> Text("Hello 0") -> TableNextColumn()      -> Text("Hello 1")  // OK: TableNextColumn() automatically gets to next row!
//        TableNextRow()                           -> Text("Hello 0")                                               // Not OK! Missing TableSetColumnIndex() or TableNextColumn()! Text will not appear!
//        --------------------------------------------------------------------------------------------------------
// - 5. Call EndTable()
interface tables {

    /** Read about "TABLE SIZING" at the top of this file. */
    fun beginTable(strId: String, columnsCount: Int, flags: TableFlags = Tf.None.i,
                   outerSize: Vec2 = Vec2(-Float.MIN_VALUE, 0f), innerWidth: Float = 0f): Boolean {
        val id = getID(strId)
        return beginTableEx(strId, id, columnsCount, flags, outerSize, innerWidth)
    }

    /** only call EndTable() if BeginTable() returns true! */
    fun endTable() {

        val table = g.currentTable
        check(table != null) { "Only call EndTable() if BeginTable() returns true!" }

        // This assert would be very useful to catch a common error... unfortunately it would probably trigger in some
        // cases, and for consistency user may sometimes output empty tables (and still benefit from e.g. outer border)
        //IM_ASSERT(table->IsLayoutLocked && "Table unused: never called TableNextRow(), is that the intent?");

        // If the user never got to call TableNextRow() or TableNextColumn(), we call layout ourselves to ensure all our
        // code paths are consistent (instead of just hoping that TableBegin/TableEnd will work), get borders drawn, etc.
        if (!table.isLayoutLocked)
            table.updateLayout()

        val flags = table.flags
        val innerWindow = table.innerWindow!!
        val outerWindow = table.outerWindow!!
        assert(innerWindow === g.currentWindow)
        assert(outerWindow === innerWindow || outerWindow === innerWindow.parentWindow)

        if (table.isInsideRow)
            table.endRow()

        // Context menu in columns body
        if (flags has Tf.ContextMenuInBody)
            if (table.hoveredColumnBody != -1 && !ImGui.isAnyItemHovered && ImGui.isMouseReleased(MouseButton.Right))
                tableOpenContextMenu(table.hoveredColumnBody)

        // Finalize table height
        innerWindow.dc.prevLineSize put table.hostBackupPrevLineSize
        innerWindow.dc.currLineSize put table.hostBackupCurrLineSize
        innerWindow.dc.cursorMaxPos put table.hostBackupCursorMaxPos
        if (innerWindow !== outerWindow)
        // Both OuterRect/InnerRect are valid from BeginTable
            innerWindow.dc.cursorMaxPos.y = table.rowPosY2
        else if (flags hasnt Tf.NoHostExtendY) {
            // Patch OuterRect/InnerRect height
            table.outerRect.max.y = table.outerRect.max.y max innerWindow.dc.cursorPos.y
            table.innerRect.max.y = table.outerRect.max.y
            innerWindow.dc.cursorMaxPos.y = table.rowPosY2
        }
        table.workRect.max.y = table.workRect.max.y max table.outerRect.max.y
        table.lastOuterHeight = table.outerRect.height

        if (flags hasnt Tf.NoClip)
            innerWindow.drawList.popClipRect()
        innerWindow.clipRect put innerWindow.drawList._clipRectStack.last()

        // Draw borders
        if (flags has Tf.Borders)
            table.drawBorders()

        //        #if 0
        //        // Strip out dummy channel draw calls
        //        // We have no way to prevent user submitting direct ImDrawList calls into a hidden column (but ImGui:: calls will be clipped out)
        //        // Pros: remove draw calls which will have no effect. since they'll have zero-size cliprect they may be early out anyway.
        //        // Cons: making it harder for users watching metrics/debugger to spot the wasted vertices.
        //        if (table->DummyDrawChannel != (ImGuiTableColumnIdx)-1)
        //        {
        //            ImDrawChannel* dummy_channel = &table->DrawSplitter._Channels[table->DummyDrawChannel]
        //            dummy_channel->_CmdBuffer.resize(0)
        //            dummy_channel->_IdxBuffer.resize(0)
        //        }
        //        #endif

        // Flatten channels and merge draw calls
        table.drawSplitter.setCurrentChannel(innerWindow.drawList, 0)
        if (table.flags hasnt Tf.NoClip)
            table.mergeDrawChannels()
        table.drawSplitter.merge(innerWindow.drawList)

        // Update ColumnsAutoFitWidth to get us ahead for host using our size to auto-resize without waiting for next BeginTable()
        val widthSpacings = table.outerPaddingX * 2f + (table.cellSpacingX1 + table.cellSpacingX2) * (table.columnsEnabledCount - 1)
        table.columnsAutoFitWidth = widthSpacings + (table.cellPaddingX * 2f) * table.columnsEnabledCount
        for (columnN in 0 until table.columnsCount)
            if (table.enabledMaskByIndex has (1L shl columnN))
                table.columnsAutoFitWidth += table getColumnWidthAuto table.columns[columnN]

        // Update scroll
        if (table.flags hasnt Tf.ScrollX && innerWindow !== outerWindow)
            innerWindow.scroll.x = 0f
        else if (table.lastResizedColumn != -1 && table.resizedColumn == -1 && innerWindow.scrollbar.x && table.instanceInteracted == table.instanceCurrent) {
            // When releasing a column being resized, scroll to keep the resulting column in sight
            val neighborWidthToKeepVisible = table.minColumnWidth + table.cellPaddingX * 2f
            val column = table.columns[table.lastResizedColumn]
            if (column.maxX < table.innerClipRect.min.x)
                innerWindow.setScrollFromPosX(column.maxX - innerWindow.pos.x - neighborWidthToKeepVisible, 1f)
            else if (column.maxX > table.innerClipRect.max.x)
                innerWindow.setScrollFromPosX(column.maxX - innerWindow.pos.x + neighborWidthToKeepVisible, 1f)
        }

        // Apply resizing/dragging at the end of the frame
        if (table.resizedColumn != -1 && table.instanceCurrent == table.instanceInteracted) {
            val column = table.columns[table.resizedColumn]
            val newX2 = g.io.mousePos.x - g.activeIdClickOffset.x + TABLE_RESIZE_SEPARATOR_HALF_THICKNESS
            val newWidth = floor(newX2 - column.minX - table.cellSpacingX1 - table.cellPaddingX * 2f)
            table.resizedColumnNextWidth = newWidth
        }

        // Pop from id stack
        assert(innerWindow.idStack.last() == table.id + table.instanceCurrent) { "Mismatching PushID/PopID!" }
        assert(outerWindow.dc.itemWidthStack.size >= table.hostBackupItemWidthStackSize) { "Too many PopItemWidth!" }
        popID()

        // Restore window data that we modified
        val backupOuterCursorPosX = outerWindow.dc.cursorPos.x
        val backupOuterMaxPosX = outerWindow.dc.cursorMaxPos.x
        val backupInnerMaxPosX = innerWindow.dc.cursorMaxPos.x
        innerWindow.workRect put table.hostBackupWorkRect
        innerWindow.parentWorkRect put table.hostBackupParentWorkRect
        innerWindow.skipItems = table.hostSkipItems
        outerWindow.dc.cursorPos put table.outerRect.min
        outerWindow.dc.itemWidth = table.hostBackupItemWidth
        if (table.hostBackupItemWidthStackSize != 1)
            TODO()
        //        outerWindow.dc.itemWidthStack.size = table->HostBackupItemWidthStackSize  // TODO check me
        outerWindow.dc.columnsOffset = table.hostBackupColumnsOffset

        // Layout in outer window
        // (FIXME: To allow auto-fit and allow desirable effect of SameLine() we dissociate 'used' vs 'ideal' size by overriding
        // CursorPosPrevLine and CursorMaxPos manually. That should be a more general layout feature, see same problem e.g. #3414)
        val outerWidth = if (table.isOuterRectAutoFitX) table.workRect.width else table.columnsAutoFitWidth
        val outerHeight = table.outerRect.height
        if (innerWindow != outerWindow)
            endChild()
        else {
            itemSize(Vec2(outerWidth, outerHeight))
            outerWindow.dc.cursorPosPrevLine.x = table.outerRect.max.x
        }

        // Override EndChild/ItemSize max extent with our own to enable auto-resize on the X axis when possible
        // FIXME-TABLE: This can be improved (e.g. for Fixed columns we don't want to auto AutoFitWidth? or propagate window auto-fit to table?)
        if (table.flags has Tf.ScrollX) {
            var maxPosX = backupInnerMaxPosX
            if (table.rightMostEnabledColumn != -1)
                maxPosX = maxPosX max table.columns[table.rightMostEnabledColumn].maxX
            if (table.resizedColumn != -1)
                maxPosX = maxPosX max table.resizeLockMinContentsX2
            innerWindow.dc.cursorMaxPos.x = maxPosX // For inner scrolling
            outerWindow.dc.cursorMaxPos.x = backupOuterMaxPosX max (backupOuterCursorPosX + table.columnsGivenWidth + innerWindow.scrollbarSizes.x) // For outer scrolling
        } else
            outerWindow.dc.cursorMaxPos.x = backupOuterMaxPosX max (table.workRect.min.x + outerWidth) // For auto-fit

        // Save settings
        if (table.isSettingsDirty)
            table.saveSettings()
        table.isInitializing = false

        // Clear or restore current table, if any
        assert(g.currentWindow === outerWindow && g.currentTable === table)
        g.currentTableStack.pop()
        g.currentTable = if (g.currentTableStack.isNotEmpty()) g.tables.getByIndex(g.currentTableStack.last().index) else null
        outerWindow.dc.currentTableIdx = g.currentTable?.let { g.tables.getIndex(it).i } ?: -1
    }

    /** [Public] Starts into the first cell of a new row
     *
     *  append into the first cell of a new row. */
    fun tableNextRow(rowFlags: TableRowFlags = Trf.None.i, rowMinHeight: Float = 0f) {

        val table = g.currentTable!!

        if (!table.isLayoutLocked)
            table.updateLayout()
        if (table.isInsideRow)
            table.endRow()

        table.lastRowFlags = table.rowFlags
        table.rowFlags = rowFlags
        table.rowMinHeight = rowMinHeight
        table.beginRow()

        // We honor min_row_height requested by user, but cannot guarantee per-row maximum height,
        // because that would essentially require a unique clipping rectangle per-cell.
        table.rowPosY2 += table.cellPaddingY * 2f
        table.rowPosY2 = table.rowPosY2 max (table.rowPosY1 + rowMinHeight)

        // Disable output until user calls TableNextColumn()
        table.innerWindow!!.skipItems = true
    }

    /** [Public] Append into the next column/cell
     *
     *  append into the next column (or first column of next row if currently in last column). Return true when column is visible. */
    fun tableNextColumn(): Boolean {

        val table = g.currentTable ?: return false

        if (table.isInsideRow && table.currentColumn + 1 < table.columnsCount) {
            if (table.currentColumn != -1)
                table.endCell()
            table.beginCell(table.currentColumn + 1)
        } else {
            tableNextRow()
            table.beginCell(0)
        }

        // Return whether the column is visible. User may choose to skip submitting items based on this return value,
        // however they shouldn't skip submitting for columns that may have the tallest contribution to row height.
        val columnN = table.currentColumn
        return table.requestOutputMaskByIndex has (1L shl columnN)
    }

    /** [Public] Append into a specific column
     *
     *  append into the specified column. Return true when column is visible. */
    fun tableSetColumnIndex(columnN: Int): Boolean {

        val table = g.currentTable ?: return false

        if (table.currentColumn != columnN) {
            if (table.currentColumn != -1)
                table.endCell()
            assert(columnN >= 0 && table.columnsCount != 0)
            table beginCell columnN
        }

        // Return whether the column is visible. User may choose to skip submitting items based on this return value,
        // however they shouldn't skip submitting for columns that may have the tallest contribution to row height.
        return table.requestOutputMaskByIndex has (1L shl columnN)
    }

    // Tables: Headers & Columns declaration
    // - Use TableSetupColumn() to specify label, resizing policy, default width/weight, id, various other flags etc.
    //   Important: this will not display anything! The name passed to TableSetupColumn() is used by TableHeadersRow() and context-menus.
    // - Use TableHeadersRow() to create a row and automatically submit a TableHeader() for each column.
    //   Headers are required to perform: reordering, sorting, and opening the context menu (but context menu can also be available in columns body using ImGuiTableFlags_ContextMenuInBody).
    // - You may manually submit headers using TableNextRow() + TableHeader() calls, but this is only useful in some advanced cases (e.g. adding custom widgets in header row).
    // - Use TableSetupScrollFreeze() to lock columns (from the right) or rows (from the top) so they stay visible when scrolled.


    /** See "COLUMN SIZING POLICIES" comments at the top of this file  */
    fun tableSetupColumn(label: String?, flags_: TableColumnFlags = Tcf.None.i, initWidthOrWeight: Float = -1f, userId: ID = 0) {

        var flags = flags_
        val table = g.currentTable
        check(table != null) { "Need to call TableSetupColumn() after BeginTable()!" }
        assert(!table.isLayoutLocked) { "Need to call call TableSetupColumn() before first row!" }
        assert(flags hasnt Tcf.StatusMask_) { "Illegal to pass StatusMask values to TableSetupColumn()" }
        if (table.declColumnsCount >= table.columnsCount) {
            assert(table.declColumnsCount < table.columnsCount) { "Called TableSetupColumn() too many times!" }
            return
        }

        val column = table.columns[table.declColumnsCount]
        table.declColumnsCount++

        // When passing a width automatically enforce WidthFixed policy
        // (whereas TableSetupColumnFlags would default to WidthAuto if table is not Resizable)
        if (flags hasnt Tcf.WidthMask_ && initWidthOrWeight > 0f)
            (table.flags and Tf._SizingMask).let {
                if (it == Tf.SizingFixedFit.i || it == Tf.SizingFixedSame.i)
                    flags = flags or Tcf.WidthFixed
            }

        table.setupColumnFlags(column, flags)
        column.userID = userId
        flags = column.flags

        // Initialize defaults
        if (flags has Tcf.WidthStretch)
            assert(initWidthOrWeight != 0f) { "Need to provide a valid weight!" }
        column.initStretchWeightOrWidth = initWidthOrWeight
        if (table.isInitializing) {
            // Init width or weight
            if (column.widthRequest < 0f && column.stretchWeight < 0f) {
                if (flags has Tcf.WidthFixed && initWidthOrWeight > 0f)
                    column.widthRequest = initWidthOrWeight
                if (flags has Tcf.WidthStretch)
                    column.stretchWeight = initWidthOrWeight.takeIf { it > 0f } ?: -1f

                // Disable auto-fit if an explicit width/weight has been specified
                if (initWidthOrWeight > 0f)
                    column.autoFitQueue = 0x00
            }
            // Init default visibility/sort state
            if (flags has Tcf.DefaultHide && table.settingsLoadedFlags hasnt Tf.Hideable) {
                column.isEnabled = false
                column.isEnabledNextFrame = false
            }
            if (flags has Tcf.DefaultSort && table.settingsLoadedFlags hasnt Tf.Sortable) {
                column.sortOrder = 0 // Multiple columns using _DefaultSort will be reassigned unique SortOrder values when building the sort specs.
                column.sortDirection = when {
                    column.flags has Tcf.PreferSortDescending -> SortDirection.Descending
                    else -> SortDirection.Ascending
                }
            }
        }

        // Store name (append with zero-terminator in contiguous buffer)
        column.nameOffset = -1
        if (label != null && label.isNotEmpty()) {
            column.nameOffset = table.columnsNames.size
            table.columnsNames += label
        }
    }

    /** [Public]
     *  lock columns/rows so they stay visible when scrolled. */
    fun tableSetupScrollFreeze(columns: Int, rows: Int) {

        val table = g.currentTable
        check(table != null) { "Need to call TableSetupColumn() after BeginTable()!" }
        assert(!table.isLayoutLocked) { "Need to call TableSetupColumn() before first row!" }
        assert(columns in 0 until TABLE_MAX_COLUMNS)
        assert(rows in 0..127) // Arbitrary limit

        table.freezeColumnsRequest = if (table.flags has Tf.ScrollX) columns else 0
        table.freezeColumnsCount = if (table.innerWindow!!.scroll.x != 0f) table.freezeColumnsRequest else 0
        table.freezeRowsRequest = if (table.flags has Tf.ScrollY) rows else 0
        table.freezeRowsCount = if (table.innerWindow!!.scroll.y != 0f) table.freezeRowsRequest else 0
        table.isUnfrozenRows = table.freezeRowsCount == 0 // Make sure this is set before TableUpdateLayout() so ImGuiListClipper can benefit from it.b
    }

    /** [Public] This is a helper to output TableHeader() calls based on the column names declared in TableSetupColumn().
     *  The intent is that advanced users willing to create customized headers would not need to use this helper
     *  and can create their own! For example: TableHeader() may be preceeded by Checkbox() or other custom widgets.
     *  See 'Demo->Tables->Custom headers' for a demonstration of implementing a custom version of this.
     *  This code is constructed to not make much use of internal functions, as it is intended to be a template to copy.
     *  FIXME-TABLE: TableOpenContextMenu() and TableGetHeaderRowHeight() are not public.
     *
     *  submit all headers cells based on data provided to TableSetupColumn() + submit context menu */
    fun tableHeadersRow() {

        val table = g.currentTable
        check(table != null) { "Need to call TableHeadersRow() after BeginTable()!" }

        // Layout if not already done (this is automatically done by TableNextRow, we do it here solely to facilitate stepping in debugger as it is frequent to step in TableUpdateLayout)
        if (!table.isLayoutLocked)
            table.updateLayout()

        // Open row
        val rowY1 = ImGui.cursorScreenPos.y
        val rowHeight = tableGetHeaderRowHeight()
        tableNextRow(Trf.Headers.i, rowHeight)
        if (table.hostSkipItems) // Merely an optimization, you may skip in your own code.
            return

        val columnsCount = tableGetColumnCount()
        for (columnN in 0 until columnsCount) {

            if (!tableSetColumnIndex(columnN))
                continue

            // Push an id to allow unnamed labels (generally accidental, but let's behave nicely with them)
            // - in your own code you may omit the PushID/PopID all-together, provided you know they won't collide
            // - table->InstanceCurrent is only >0 when we use multiple BeginTable/EndTable calls with same identifier.
            val name = tableGetColumnName(columnN)!!
            pushID(table.instanceCurrent * table.columnsCount + columnN)
            tableHeader(name)
            popID()
        }

        // Allow opening popup from the right-most section after the last column.
        val mousePos = ImGui.mousePos
        if (isMouseReleased(1) && tableGetHoveredColumn() == columnsCount)
            if (mousePos.y >= rowY1 && mousePos.y < rowY1 + rowHeight)
                tableOpenContextMenu(-1) // Will open a non-column-specific popup.
    }

    /** Emit a column header (text + optional sort order)
     *  We cpu-clip text here so that all columns headers can be merged into a same draw call.
     *  Note that because of how we cpu-clip and display sorting indicators, you _cannot_ use SameLine() after a TableHeader()
     *
     *  submit one header cell manually (rarely used) */
    fun tableHeader(label: String = "") {

        val window = g.currentWindow!!
        if (window.skipItems)
            return

        val table = g.currentTable
        check(table != null) { "Need to call TableHeader() after BeginTable()!" }
        assert(table.currentColumn != -1)
        val columnN = table.currentColumn
        val column = table.columns[columnN]

        // Label
        //        if (label == NULL) -> default parameter
        //            label = "";
        val labelEnd = findRenderedTextEnd(label)
        val labelSize = calcTextSize(label.toByteArray(), 0, labelEnd, true)
        val labelPos = window.dc.cursorPos // [JVM] same instance, careful!

        // If we already got a row height, there's use that.
        // FIXME-TABLE: Padding problem if the correct outer-padding CellBgRect strays off our ClipRect
        val cellR = table getCellBgRect columnN
        val labelHeight = labelSize.y max (table.rowMinHeight - table.cellPaddingY * 2f)

        // Calculate ideal size for sort order arrow
        var wArrow = 0f
        var wSortText = 0f
        var sortOrderSuf = ""
        val ARROW_SCALE = 0.65f
        if (table.flags has Tf.Sortable && column.flags hasnt Tcf.NoSort) {
            wArrow = floor(g.fontSize * ARROW_SCALE + g.style.framePadding.x)
            if (column.sortOrder > 0) {
                sortOrderSuf = "${column.sortOrder + 1}"
                wSortText = g.style.itemInnerSpacing.x + calcTextSize(sortOrderSuf).x
            }
        }

        // We feed our unclipped width to the column without writing on CursorMaxPos, so that column is still considering for merging.
        val maxPosX = labelPos.x + labelSize.x + wSortText + wArrow
        column.contentMaxXHeadersUsed = column.contentMaxXHeadersUsed max column.workMaxX
        column.contentMaxXHeadersIdeal = column.contentMaxXHeadersIdeal max maxPosX

        // Keep header highlighted when context menu is open.
        val selected = table.isContextPopupOpen && table.contextPopupColumn == columnN && table.instanceInteracted == table.instanceCurrent
        val id = window.getID(label)
        val bb = Rect(cellR.min.x, cellR.min.y, cellR.max.x, cellR.max.y max (cellR.min.y + labelHeight + g.style.cellPadding.y * 2f))
        itemSize(Vec2(0f, labelHeight)) // Don't declare unclipped width, it'll be fed ContentMaxPosHeadersIdeal
        if (!itemAdd(bb, id))
            return

        //GetForegroundDrawList()->AddRect(cell_r.Min, cell_r.Max, IM_COL32(255, 0, 0, 255)); // [DEBUG]
        //GetForegroundDrawList()->AddRect(bb.Min, bb.Max, IM_COL32(255, 0, 0, 255)); // [DEBUG]

        // Using AllowItemOverlap mode because we cover the whole cell, and we want user to be able to submit subsequent items.
        val (pressed, hovered, held) = buttonBehavior(bb, id, ButtonFlag.AllowItemOverlap.i)
        if (g.activeId != id)
            setItemAllowOverlap()
        if (held || hovered || selected) {
            val col = if (held) Col.HeaderActive else if (hovered) Col.HeaderHovered else Col.Header
            //RenderFrame(bb.Min, bb.Max, col, false, 0.0f);
            tableSetBgColor(TableBgTarget.CellBg, col.u32, table.currentColumn)
            renderNavHighlight(bb, id, NavHighlightFlag.TypeThin or NavHighlightFlag.NoRounding)
        } else  // Submit single cell bg color in the case we didn't submit a full header row
            if (table.rowFlags hasnt Trf.Headers)
                tableSetBgColor(TableBgTarget.CellBg, Col.TableHeaderBg.u32, table.currentColumn)
        if (held)
            table.heldHeaderColumn = columnN
        window.dc.cursorPos.y -= g.style.itemSpacing.y * 0.5f

        // Drag and drop to re-order columns.
        // FIXME-TABLE: Scroll request while reordering a column and it lands out of the scrolling zone.
        if (held && table.flags has Tf.Reorderable && isMouseDragging(MouseButton.Left) && !g.dragDropActive) {
            // While moving a column it will jump on the other side of the mouse, so we also test for MouseDelta.x
            table.reorderColumn = columnN
            table.instanceInteracted = table.instanceCurrent

            // We don't reorder: through the frozen<>unfrozen line, or through a column that is marked with ImGuiTableColumnFlags_NoReorder.
            if (g.io.mouseDelta.x < 0.0f && g.io.mousePos.x < cellR.min.x)
                table.columns.getOrNull(column.prevEnabledColumn)?.let { prevColumn ->
                    if (((column.flags or prevColumn.flags) and Tcf.NoReorder) == 0)
                        if (column.indexWithinEnabledSet < table.freezeColumnsRequest == prevColumn.indexWithinEnabledSet < table.freezeColumnsRequest)
                            table.reorderColumnDir = -1
                }
            if (g.io.mouseDelta.x > 0f && g.io.mousePos.x > cellR.max.x)
                table.columns.getOrNull(column.nextEnabledColumn)?.let { nextColumn ->
                    if (((column.flags or nextColumn.flags) and Tcf.NoReorder) == 0)
                        if (column.indexWithinEnabledSet < table.freezeColumnsRequest == nextColumn.indexWithinEnabledSet < table.freezeColumnsRequest)
                            table.reorderColumnDir = +1
                }
        }

        // Sort order arrow
        val ellipsisMax = cellR.max.x - wArrow - wSortText
        if (table.flags has Tf.Sortable && column.flags hasnt Tcf.NoSort) {
            if (column.sortOrder != -1) {
                var x = cellR.min.x max (cellR.max.x - wArrow - wSortText)
                val y = labelPos.y
                if (column.sortOrder > 0) {
                    pushStyleColor(Col.Text, getColorU32(Col.Text, 0.7f))
                    renderText(Vec2(x + g.style.itemInnerSpacing.x, y), sortOrderSuf)
                    popStyleColor()
                    x += wSortText
                }
                window.drawList.renderArrow(Vec2(x, y), Col.Text.u32, if (column.sortDirection == SortDirection.Ascending) Dir.Up else Dir.Down, ARROW_SCALE)
            }

            // Handle clicking on column header to adjust Sort Order
            if (pressed && table.reorderColumn != columnN) {
                val sortDirection = tableGetColumnNextSortDirection(column)
                tableSetColumnSortDirection(columnN, sortDirection, g.io.keyShift)
            }
        }

        // Render clipped label. Clipping here ensure that in the majority of situations, all our header cells will
        // be merged into a single draw call.
        //window->DrawList->AddCircleFilled(ImVec2(ellipsis_max, label_pos.y), 40, IM_COL32_WHITE);
        val posMax = Vec2(ellipsisMax, labelPos.y + labelHeight + g.style.framePadding.y)
        renderTextEllipsis(window.drawList, labelPos, posMax, ellipsisMax, ellipsisMax, label.toByteArray(), labelEnd, labelSize)

        val textClipped = labelSize.x > (ellipsisMax - labelPos.x)
        if (textClipped && hovered && g.hoveredIdNotActiveTimer > g.tooltipSlowDelay)
            setTooltip(label.substring(0, labelEnd))

        // We don't use BeginPopupContextItem() because we want the popup to stay up even after the column is hidden
        if (isMouseReleased(1) && isItemHovered())
            tableOpenContextMenu(columnN)
    }

    // Tables: Miscellaneous functions
    // - Most functions taking 'int column_n' treat the default value of -1 as the same as passing the current column index
    // - Sorting: call TableGetSortSpecs() to retrieve latest sort specs for the table. Return value will be NULL if no sorting.
    //   When 'SpecsDirty == true' you should sort your data. It will be true when sorting specs have changed since last call, or the first time.
    //   Make sure to set 'SpecsDirty = false' after sorting, else you may wastefully sort your data every frame!
    //   Lifetime: don't hold on this pointer over multiple frames or past any subsequent call to BeginTable().


    /** return number of columns (value passed to BeginTable) */
    fun tableGetColumnCount(): Int = g.currentTable?.columnsCount ?: 0

    /** return current column index. */
    fun tableGetColumnIndex(): Int = g.currentTable?.currentColumn ?: 0

    /** Note: for row coloring we use ->RowBgColorCounter which is the same value without counting header rows
     *
     *  return current row index. */
    fun tableGetRowIndex(): Int = g.currentTable?.currentRow ?: 0

    /** return "" if column didn't have a name declared by TableSetupColumn(). Pass -1 to use current column. */
    fun tableGetColumnName(columnN: Int = -1): String? {
        val table = g.currentTable ?: return null
        return table.getColumnName(if (columnN < 0) table.currentColumn else columnN)
    }

    /** We allow querying for an extra column in order to poll the IsHovered state of the right-most section
     *
     *  return column flags so you can query their Enabled/Visible/Sorted/Hovered status flags. Pass -1 to use current column. */
    fun tableGetColumnFlags(columnN_: Int = -1): TableColumnFlags {
        val table = g.currentTable ?: return Tcf.None.i
        val columnN = if (columnN_ < 0) table.currentColumn else columnN_
        return when (columnN) {
            table.columnsCount -> if (table.hoveredColumnBody == columnN) Tcf.IsHovered.i else Tcf.None.i
            else -> table.columns[columnN].flags
        }
    }

    /** Return NULL if no sort specs (most often when ImGuiTableFlags_Sortable is not set)
     *  You can sort your data again when 'SpecsChanged == true'. It will be true with sorting specs have changed since
     *  last call, or the first time.
     *  Lifetime: don't hold on this pointer over multiple frames or past any subsequent call to BeginTable()!
     *
     *  get latest sort specs for the table (NULL if not sorting). */
    fun tableGetSortSpecs(): TableSortSpecs? {

        val table = g.currentTable!!

        if (table.flags hasnt Tf.Sortable)
            return null

        // Require layout (in case TableHeadersRow() hasn't been called) as it may alter IsSortSpecsDirty in some paths.
        if (!table.isLayoutLocked)
            table.updateLayout()

        if (table.isSortSpecsDirty)
            table.sortSpecsBuild()

        return table.sortSpecs
    }

    /** change the color of a cell, row, or column. See ImGuiTableBgTarget_ flags for details. */
    fun tableSetBgColor(target: TableBgTarget, color_: Int, columnN_: Int = -1) {

        val table = g.currentTable!!
        assert(target != TableBgTarget.None)

        val color = if (color_ == COL32_DISABLE) 0 else color_
        var columnN = columnN_

        // We cannot draw neither the cell or row background immediately as we don't know the row height at this point in time.
        when (target) {
            TableBgTarget.CellBg -> {
                if (table.rowPosY1 > table.innerClipRect.max.y) // Discard
                    return
                if (columnN == -1)
                    columnN = table.currentColumn
                if (table.visibleMaskByIndex hasnt (1L shl columnN))
                    return
                if (table.rowCellDataCurrent < 0 || table.rowCellData[table.rowCellDataCurrent].column != columnN)
                    table.rowCellDataCurrent++
                val cellData = table.rowCellData[table.rowCellDataCurrent]
                cellData.bgColor = color
                cellData.column = columnN
            }
            TableBgTarget.RowBg0, TableBgTarget.RowBg1 -> {
                if (table.rowPosY1 > table.innerClipRect.max.y) // Discard
                    return
                assert(columnN == -1)
                val bgIdx = if (target == TableBgTarget.RowBg1) 1 else 0
                table.rowBgColor[bgIdx] = color
            }
            else -> assert(false)
        }
    }
}