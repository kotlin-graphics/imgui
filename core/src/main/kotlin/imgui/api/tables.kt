package imgui.api

import glm_.max
import glm_.min
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.beginCell
import imgui.ImGui.beginRow
import imgui.ImGui.beginTableEx
import imgui.ImGui.buttonBehavior
import imgui.ImGui.calcTextSize
import imgui.ImGui.drawBorders
import imgui.ImGui.endCell
import imgui.ImGui.endChild
import imgui.ImGui.endRow
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.getCellBgRect
import imgui.ImGui.getColorU32
import imgui.ImGui.getColumnName
import imgui.ImGui.getColumnWidthAuto
import imgui.ImGui.getID
import imgui.ImGui.getInstanceData
import imgui.ImGui.isItemHovered
import imgui.ImGui.isMouseDragging
import imgui.ImGui.isReleased
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.mergeDrawChannels
import imgui.ImGui.popID
import imgui.ImGui.popStyleColor
import imgui.ImGui.pushID
import imgui.ImGui.pushStyleColor
import imgui.ImGui.renderArrow
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.renderText
import imgui.ImGui.renderTextEllipsis
import imgui.ImGui.saveSettings
import imgui.ImGui.setItemAllowOverlap
import imgui.ImGui.setScrollFromPosX
import imgui.ImGui.setTooltip
import imgui.ImGui.sortSpecsBuild
import imgui.ImGui.tableGetColumnNextSortDirection
import imgui.ImGui.tableGetHeaderRowHeight
import imgui.ImGui.tableGetHoveredColumn
import imgui.ImGui.tableOpenContextMenu
import imgui.ImGui.tableSetColumnSortDirection
import imgui.ImGui.updateLayout
import imgui.TableColumnFlag
import imgui.TableFlag
import imgui.classes.TableSortSpecs
import imgui.internal.classes.*
import imgui.internal.floor
import imgui.internal.sections.ButtonFlag
import imgui.internal.sections.NavHighlightFlag
import imgui.static.TABLE_BORDER_SIZE
import imgui.static.TABLE_RESIZE_SEPARATOR_HALF_THICKNESS
import imgui.static.setupColumnFlags
import imgui.TableColumnFlag as Tcf
import imgui.TableFlag as Tf
import imgui.TableRowFlag as Trf

// Tables
// - Full-featured replacement for old Columns API.
// - See Demo->Tables for demo code. See top of imgui_tables.cpp for general commentary.
// - See ImGuiTableFlags_ and ImGuiTableColumnFlags_ enums for a description of available flags.
// The typical call flow is:
// - 1. Call BeginTable(), early out if returning false.
// - 2. Optionally call TableSetupColumn() to submit column name/flags/defaults.
// - 3. Optionally call TableSetupScrollFreeze() to request scroll freezing of columns/rows.
// - 4. Optionally call TableHeadersRow() to submit a header row. Names are pulled from TableSetupColumn() data.
// - 5. Populate contents:
//    - In most situations you can use TableNextRow() + TableSetColumnIndex(N) to start appending into a column.
//    - If you are using tables as a sort of grid, where every column is holding the same type of contents,
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
    fun beginTable(
        strId: String, columns: Int, flags: TableFlags = none,
        outerSize: Vec2 = Vec2(), innerWidth: Float = 0f
    ): Boolean {
        val id = getID(strId)
        return beginTableEx(strId, id, columns, flags, outerSize, innerWidth)
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
        var tempData = table.tempData
        assert(innerWindow === g.currentWindow)
        assert(outerWindow === innerWindow || outerWindow === innerWindow.parentWindow)

        if (table.isInsideRow)
            table.endRow()

        // Context menu in columns body
        if (flags has Tf.ContextMenuInBody)
            if (table.hoveredColumnBody != -1 && !ImGui.isAnyItemHovered && MouseButton.Right.isReleased)
                tableOpenContextMenu(table.hoveredColumnBody)

        // Finalize table height
        val tableInstance = table getInstanceData table.instanceCurrent
        innerWindow.dc.prevLineSize put tempData!!.hostBackupPrevLineSize
        innerWindow.dc.currLineSize put tempData.hostBackupCurrLineSize
        innerWindow.dc.cursorMaxPos put tempData.hostBackupCursorMaxPos
        val innerContentMaxY = table.rowPosY2
        assert(table.rowPosY2 == innerWindow.dc.cursorPos.y)
        if (innerWindow !== outerWindow)
            innerWindow.dc.cursorMaxPos.y = innerContentMaxY
        else if (flags hasnt Tf.NoHostExtendY) {
            // Patch OuterRect/InnerRect height
            table.outerRect.max.y = table.outerRect.max.y max innerContentMaxY
            table.innerRect.max.y = table.outerRect.max.y
        }
        table.workRect.max.y = table.workRect.max.y max table.outerRect.max.y
        tableInstance.lastOuterHeight = table.outerRect.height

        // Setup inner scrolling range
        // FIXME: This ideally should be done earlier, in BeginTable() SetNextWindowContentSize call, just like writing to inner_window->DC.CursorMaxPos.y,
        // but since the later is likely to be impossible to do we'd rather update both axises together.
        if (table.flags has Tf.ScrollX) {
            val outerPaddingForBorder = if (table.flags has Tf.BordersOuterV) TABLE_BORDER_SIZE else 0f
            val inner = table.innerWindow!!
            var maxPosX = inner.dc.cursorMaxPos.x
            if (table.rightMostEnabledColumn != -1)
                maxPosX =
                    maxPosX max (table.columns[table.rightMostEnabledColumn].workMaxX + table.cellPaddingX + table.outerPaddingX - outerPaddingForBorder)
            if (table.resizedColumn != -1)
                maxPosX = maxPosX max table.resizeLockMinContentsX2
            inner.dc.cursorMaxPos.x = maxPosX
        }

        // Pop clipping rect
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
        val splitter = table.drawSplitter
        splitter.setCurrentChannel(innerWindow.drawList, 0)
        if (table.flags hasnt Tf.NoClip)
            table.mergeDrawChannels()
        splitter.merge(innerWindow.drawList)

        // Update ColumnsAutoFitWidth to get us ahead for host using our size to auto-resize without waiting for next BeginTable()
        var autoFitWidthForFixed = 0f
        var autoFitWidthForStretched = 0f
        var autoFitWidthForStretchedMin = 0f
        for (columnN in 0 until table.columnsCount)
            if (table.enabledMaskByIndex testBit columnN) {
                val column = table.columns[columnN]
                val columnWidthRequest =
                    if (column.flags has Tcf.WidthFixed && column.flags hasnt Tcf.NoResize) column.widthRequest else table getColumnWidthAuto column
                if (column.flags has Tcf.WidthFixed)
                    autoFitWidthForFixed += columnWidthRequest
                else
                    autoFitWidthForStretched += columnWidthRequest
                if (column.flags has Tcf.WidthStretch && column.flags hasnt Tcf.NoResize)
                    autoFitWidthForStretchedMin =
                        autoFitWidthForStretchedMin max (columnWidthRequest / (column.stretchWeight / table.columnsStretchSumWeights))
            }
        val widthSpacings =
            table.outerPaddingX * 2f + (table.cellSpacingX1 + table.cellSpacingX2) * (table.columnsEnabledCount - 1)
        table.columnsAutoFitWidth =
            widthSpacings + (table.cellPaddingX * 2f) * table.columnsEnabledCount + autoFitWidthForFixed + autoFitWidthForStretched max autoFitWidthForStretchedMin

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
        assert(innerWindow.idStack.last() == tableInstance.tableInstanceID) { "Mismatching PushID/PopID!" }
        assert(outerWindow.dc.itemWidthStack.size >= tempData.hostBackupItemWidthStackSize) { "Too many PopItemWidth!" }
        if (table.instanceCurrent > 0)
            popID()
        popID()

        // Restore window data that we modified
        val backupOuterMaxPos = Vec2(outerWindow.dc.cursorMaxPos)
        innerWindow.workRect put tempData.hostBackupWorkRect
        innerWindow.parentWorkRect put tempData.hostBackupParentWorkRect
        innerWindow.skipItems = table.hostSkipItems
        outerWindow.dc.cursorPos put table.outerRect.min
        outerWindow.dc.itemWidth = tempData.hostBackupItemWidth
        for (i in outerWindow.dc.itemWidthStack.size until tempData.hostBackupItemWidthStackSize)
            outerWindow.dc.itemWidthStack += 0f
        for (i in tempData.hostBackupItemWidthStackSize until outerWindow.dc.itemWidthStack.size)
            outerWindow.dc.itemWidthStack.pop()
        outerWindow.dc.columnsOffset = tempData.hostBackupColumnsOffset

        // Layout in outer window
        // (FIXME: To allow auto-fit and allow desirable effect of SameLine() we dissociate 'used' vs 'ideal' size by overriding
        // CursorPosPrevLine and CursorMaxPos manually. That should be a more general layout feature, see same problem e.g. #3414)
        if (innerWindow != outerWindow)
            endChild()
        else {
            itemSize(table.outerRect.size)
            itemAdd(table.outerRect, 0)
        }

        // Override declared contents width/height to enable auto-resize while not needlessly adding a scrollbar
        if (table.flags has Tf.NoHostExtendX) {
            // FIXME-TABLE: Could we remove this section?
            // ColumnsAutoFitWidth may be one frame ahead here since for Fixed+NoResize is calculated from latest contents
            assert(table.flags hasnt Tf.ScrollX)
            outerWindow.dc.cursorMaxPos.x = backupOuterMaxPos.x max (table.outerRect.min.x + table.columnsAutoFitWidth)
        } else if (tempData.userOuterSize.x <= 0f) {
            val decorationSize = if (table.flags has Tf.ScrollX) innerWindow.scrollbarSizes.x else 0f
            outerWindow.dc.idealMaxPos.x =
                outerWindow.dc.idealMaxPos.x max (table.outerRect.min.x + table.columnsAutoFitWidth + decorationSize - tempData.userOuterSize.x)
            outerWindow.dc.cursorMaxPos.x =
                backupOuterMaxPos.x max (table.outerRect.max.x min table.outerRect.min.x + table.columnsAutoFitWidth)
        } else
            outerWindow.dc.cursorMaxPos.x = backupOuterMaxPos.x max table.outerRect.max.x
        if (tempData.userOuterSize.y <= 0f) {
            val decorationSize = if (table.flags has Tf.ScrollY) innerWindow.scrollbarSizes.y else 0f
            outerWindow.dc.idealMaxPos.y =
                outerWindow.dc.idealMaxPos.y max (innerContentMaxY + decorationSize - tempData.userOuterSize.y)
            outerWindow.dc.cursorMaxPos.y = backupOuterMaxPos.y max (table.outerRect.max.y min innerContentMaxY)
        } else
        // OuterRect.Max.y may already have been pushed downward from the initial value (unless ImGuiTableFlags_NoHostExtendY is set)
            outerWindow.dc.cursorMaxPos.y = backupOuterMaxPos.y max table.outerRect.max.y

        // Override declared contents height
        if (innerWindow === outerWindow && flags hasnt Tf.NoHostExtendY)
            outerWindow.dc.cursorMaxPos.y = outerWindow.dc.cursorMaxPos.y max innerContentMaxY

        // Save settings
        if (table.isSettingsDirty)
            table.saveSettings()
        table.isInitializing = false

        // Clear or restore current table, if any
        assert(g.currentWindow === outerWindow && g.currentTable === table)
        assert(g.tablesTempDataStacked > 0)
        tempData = if (--g.tablesTempDataStacked > 0) g.tablesTempData[g.tablesTempDataStacked - 1] else null
        g.currentTable = tempData?.let { g.tables.getByIndex(it.tableIndex) }
        g.currentTable?.let {
            it.tempData = tempData
            it.drawSplitter.clearFreeMemory()
            it.drawSplitter = tempData!!.drawSplitter
        }
        outerWindow.dc.currentTableIdx = g.currentTable?.let { g.tables.getIndex(it).i } ?: -1
    }

    /** [Public] Starts into the first cell of a new row
     *
     *  append into the first cell of a new row. */
    fun tableNextRow(rowFlags: TableRowFlags = none, rowMinHeight: Float = 0f) {

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
        return table.columns[table.currentColumn].isRequestOutput
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
        return table.columns[columnN].isRequestOutput
    }

    // Tables: Headers & Columns declaration
    // - Use TableHeadersRow() to create a header row and automatically submit a TableHeader() for each column.
    //   Headers are required to perform: reordering, sorting, and opening the context menu.
    //   The context menu can also be made available in columns body using ImGuiTableFlags_ContextMenuInBody.
    // - You may manually submit headers using TableNextRow() + TableHeader() calls, but this is only useful in
    //   some advanced use cases (e.g. adding custom widgets in header row).
    // - Use TableSetupScrollFreeze() to lock columns/rows so they stay visible when scrolled.


    /** See "COLUMN SIZING POLICIES" comments at the top of this file
     *  If (init_width_or_weight <= 0.0f) it is ignored */
    fun tableSetupColumn(
        label: String?,
        flags_: TableColumnFlags = none,
        initWidthOrWeight: Float = 0f,
        userId: ID = 0
    ) {
        var flags = flags_
        val table = g.currentTable
        check(table != null) { "Need to call TableSetupColumn() after BeginTable()!" }
        assert(!table.isLayoutLocked) { "Need to call call TableSetupColumn() before first row!" }
        if (table.declColumnsCount >= table.columnsCount) {
            assert(table.declColumnsCount < table.columnsCount) { "Called TableSetupColumn() too many times!" }
            return
        }

        val column = table.columns[table.declColumnsCount]
        table.declColumnsCount++

        // Assert when passing a width or weight if policy is entirely left to default, to avoid storing width into weight and vice-versa.
        // Give a grace to users of ImGuiTableFlags_ScrollX.
        if (table.isDefaultSizingPolicy && flags hasnt Tcf.WidthMask && table.flags hasnt Tf.ScrollX)
            assert(initWidthOrWeight <= 0f) { "Can only specify width/weight if sizing policy is set explicitly in either Table or Column." }

        // When passing a width automatically enforce WidthFixed policy
        // (whereas TableSetupColumnFlags would default to WidthAuto if table is not Resizable)
        if (flags hasnt TableColumnFlag.WidthMask && initWidthOrWeight > 0f)
            if ((table.flags and TableFlag._SizingMask) == TableFlag.SizingFixedFit || (table.flags and TableFlag._SizingMask) == TableFlag.SizingFixedSame)
                flags /= TableColumnFlag.WidthFixed

        table.setupColumnFlags(column, flags)
        column.userID = userId
        flags = column.flags

        // Initialize defaults
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
                column.isUserEnabled = false
                column.isUserEnabledNextFrame = false
            }
            if (flags has Tcf.DefaultSort && table.settingsLoadedFlags hasnt Tf.Sortable) {
                column.sortOrder =
                    0 // Multiple columns using _DefaultSort will be reassigned unique SortOrder values when building the sort specs.
                column.sortDirection = when {
                    column.flags has Tcf.PreferSortDescending -> SortDirection.Descending
                    else -> SortDirection.Ascending
                }
            }
        }

        // Store name (append with zero-terminator in contiguous buffer)
        column.nameOffset = -1
        if (!label.isNullOrEmpty()) {
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

        table.freezeColumnsRequest = if (table.flags has Tf.ScrollX) (columns min table.columnsCount) else 0
        table.freezeColumnsCount = if (table.innerWindow!!.scroll.x != 0f) table.freezeColumnsRequest else 0
        table.freezeRowsRequest = if (table.flags has Tf.ScrollY) rows else 0
        table.freezeRowsCount = if (table.innerWindow!!.scroll.y != 0f) table.freezeRowsRequest else 0
        table.isUnfrozenRows =
            table.freezeRowsCount == 0 // Make sure this is set before TableUpdateLayout() so ImGuiListClipper can benefit from it.b


        // Ensure frozen columns are ordered in their section. We still allow multiple frozen columns to be reordered.
        // FIXME-TABLE: This work for preserving 2143 into 21|43. How about 4321 turning into 21|43? (preserve relative order in each section)
        for (columnN in 0 until table.freezeColumnsRequest) {
            val orderN = table.displayOrderToIndex[columnN]
            if (orderN != columnN && orderN >= table.freezeColumnsRequest)
                table.apply {
                    //                    ImSwap(table->Columns[table->DisplayOrderToIndex[order_n]].DisplayOrder, table->Columns[table->DisplayOrderToIndex[column_n]].DisplayOrder)
                    var order = this.columns[displayOrderToIndex[orderN]].displayOrder
                    this.columns[displayOrderToIndex[orderN]].displayOrder =
                        this.columns[displayOrderToIndex[columnN]].displayOrder
                    this.columns[displayOrderToIndex[columnN]].displayOrder = order
                    //                    ImSwap(table->DisplayOrderToIndex[order_n], table->DisplayOrderToIndex[column_n])
                    order = displayOrderToIndex[orderN]
                    displayOrderToIndex[orderN] = displayOrderToIndex[columnN]
                    displayOrderToIndex[columnN] = order
                }
        }
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
        tableNextRow(Trf.Headers, rowHeight)
        if (table.hostSkipItems) // Merely an optimization, you may skip in your own code.
            return

        val columnsCount = tableGetColumnCount()
        for (columnN in 0 until columnsCount) {

            if (!tableSetColumnIndex(columnN))
                continue

            // Push an id to allow unnamed labels (generally accidental, but let's behave nicely with them)
            // In your own code you may omit the PushID/PopID all-together, provided you know they won't collide.
            val name = if (tableGetColumnFlags(columnN) has Tcf.NoHeaderLabel) "" else tableGetColumnName(columnN)!!
            pushID(columnN)
            tableHeader(name)
            popID()
        }

        // Allow opening popup from the right-most section after the last column.
        val mousePos = ImGui.mousePos
        if (MouseButton.of(1).isReleased && tableGetHoveredColumn() == columnsCount)
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
        val labelPos = Vec2(window.dc.cursorPos)

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
        val selected =
            table.isContextPopupOpen && table.contextPopupColumn == columnN && table.instanceInteracted == table.instanceCurrent
        val id = window.getID(label)
        val bb = Rect(
            cellR.min.x,
            cellR.min.y,
            cellR.max.x,
            cellR.max.y max (cellR.min.y + labelHeight + g.style.cellPadding.y * 2f)
        )
        itemSize(Vec2(0f, labelHeight)) // Don't declare unclipped width, it'll be fed ContentMaxPosHeadersIdeal
        if (!itemAdd(bb, id))
            return

        //GetForegroundDrawList()->AddRect(cell_r.Min, cell_r.Max, IM_COL32(255, 0, 0, 255)); // [DEBUG]
        //GetForegroundDrawList()->AddRect(bb.Min, bb.Max, IM_COL32(255, 0, 0, 255)); // [DEBUG]

        // Using AllowItemOverlap mode because we cover the whole cell, and we want user to be able to submit subsequent items.
        val (pressed, hovered, held) = buttonBehavior(bb, id, ButtonFlag.AllowItemOverlap)
        if (g.activeId != id)
            setItemAllowOverlap()
        if (held || hovered || selected) {
            val col = if (held) Col.HeaderActive else if (hovered) Col.HeaderHovered else Col.Header
            //RenderFrame(bb.Min, bb.Max, col, false, 0.0f);
            tableSetBgColor(TableBgTarget.CellBg, col.u32, table.currentColumn)
        } else  // Submit single cell bg color in the case we didn't submit a full header row
            if (table.rowFlags hasnt Trf.Headers)
                tableSetBgColor(TableBgTarget.CellBg, Col.TableHeaderBg.u32, table.currentColumn)
        renderNavHighlight(bb, id, NavHighlightFlag.TypeThin or NavHighlightFlag.NoRounding)
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
                    if (((column.flags or prevColumn.flags) and Tcf.NoReorder).isEmpty)
                        if (column.indexWithinEnabledSet < table.freezeColumnsRequest == prevColumn.indexWithinEnabledSet < table.freezeColumnsRequest)
                            table.reorderColumnDir = -1
                }
            if (g.io.mouseDelta.x > 0f && g.io.mousePos.x > cellR.max.x)
                table.columns.getOrNull(column.nextEnabledColumn)?.let { nextColumn ->
                    if (((column.flags or nextColumn.flags) and Tcf.NoReorder).isEmpty)
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
                window.drawList.renderArrow(
                    Vec2(x, y),
                    Col.Text.u32,
                    if (column.sortDirection == SortDirection.Ascending) Dir.Up else Dir.Down,
                    ARROW_SCALE
                )
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
        renderTextEllipsis(
            window.drawList,
            labelPos,
            posMax,
            ellipsisMax,
            ellipsisMax,
            label.toByteArray(),
            labelEnd,
            labelSize
        )

        val textClipped = labelSize.x > (ellipsisMax - labelPos.x)
        if (textClipped && hovered && g.activeId == 0 && isItemHovered(HoveredFlag.DelayNormal))
            setTooltip(label.substring(0, labelEnd))

        // We don't use BeginPopupContextItem() because we want the popup to stay up even after the column is hidden
        if (MouseButton.of(1).isReleased && isItemHovered())
            tableOpenContextMenu(columnN)
    }

    // Tables: Sorting & Miscellaneous functions
    // - Sorting: call TableGetSortSpecs() to retrieve latest sort specs for the table. NULL when not sorting.
    //   When 'sort_specs->SpecsDirty == true' you should sort your data. It will be true when sorting specs have
    //   changed since last call, or the first time. Make sure to set 'SpecsDirty = false' after sorting,
    //   else you may wastefully sort your data every frame!
    // - Functions args 'int column_n' treat the default value of -1 as the same as passing the current column index.


    /** Return NULL if no sort specs (most often when ImGuiTableFlags_Sortable is not set)
     *  You can sort your data again when 'SpecsChanged == true'. It will be true with sorting specs have changed since
     *  last call, or the first time.
     *  Lifetime: don't hold on this pointer over multiple frames or past any subsequent call to BeginTable()!
     *
     *  get latest sort specs for the table (NULL if not sorting).  Lifetime: don't hold on this pointer over multiple frames or past any subsequent call to BeginTable(). */
    fun tableGetSortSpecs(): TableSortSpecs? {

        val table = g.currentTable!!

        if (table.flags hasnt Tf.Sortable)
            return null

        // Require layout (in case TableHeadersRow() hasn't been called) as it may alter IsSortSpecsDirty in some paths.
        if (!table.isLayoutLocked)
            table.updateLayout()

        table.sortSpecsBuild()

        return table.sortSpecs
    }

    // Tables: Miscellaneous functions
    // - Functions args 'int column_n' treat the default value of -1 as the same as passing the current column index.


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
        val table = g.currentTable ?: return none
        val columnN = if (columnN_ < 0) table.currentColumn else columnN_
        return when (columnN) {
            table.columnsCount -> if (table.hoveredColumnBody == columnN) Tcf.IsHovered else none
            else -> table.columns[columnN].flags
        }
    }

    /** Change user accessible enabled/disabled state of a column (often perceived as "showing/hiding" from users point of view)
     *  Note that end-user can use the context menu to change this themselves (right-click in headers, or right-click in columns body with ImGuiTableFlags_ContextMenuInBody)
     *  - Require table to have the ImGuiTableFlags_Hideable flag because we are manipulating user accessible state.
     *  - Request will be applied during next layout, which happens on the first call to TableNextRow() after BeginTable().
     *  - For the getter you can test (TableGetColumnFlags() & ImGuiTableColumnFlags_IsEnabled) != 0.
     *  - Alternative: the ImGuiTableColumnFlags_Disabled is an overriding/master disable flag which will also hide the column from context menu.
     *
     *  change user accessible enabled/disabled state of a column. Set to false to hide the column. User can use the context menu to change this themselves (right-click in headers, or right-click in columns body with ImGuiTableFlags_ContextMenuInBody) */
    fun tableSetColumnEnabled(columnN_: Int, enabled: Boolean) {
        var columnN = columnN_
        val table = g.currentTable
        assert(table != null)
        if (table == null)
            return
        assert(table.flags has Tf.Hideable) { "See comments above" }
        if (columnN < 0)
            columnN = table.currentColumn
        assert(columnN >= 0 && columnN < table.columnsCount)
        val column = table.columns[columnN]
        column.isUserEnabledNextFrame = enabled
    }

    /** change the color of a cell, row, or column. See ImGuiTableBgTarget_ flags for details. */
    fun tableSetBgColor(target: TableBgTarget, color_: Int, columnN_: Int = -1) {

        val table = g.currentTable!!

        val color = if (color_ == COL32_DISABLE) 0 else color_
        var columnN = columnN_

        // We cannot draw neither the cell or row background immediately as we don't know the row height at this point in time.
        when (target) {
            TableBgTarget.CellBg -> {
                if (table.rowPosY1 > table.innerClipRect.max.y) // Discard
                    return
                if (columnN == -1)
                    columnN = table.currentColumn
                if (!table.visibleMaskByIndex.testBit(columnN))
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