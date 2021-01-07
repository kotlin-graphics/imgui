package imgui.internal.classes

import glm_.*
import glm_.has
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.beginPopupEx
import imgui.ImGui.buttonBehavior
import imgui.ImGui.clearActiveID
import imgui.ImGui.endPopup
import imgui.ImGui.isMouseDoubleClicked
import imgui.ImGui.itemHoverable
import imgui.ImGui.keepAliveID
import imgui.ImGui.markIniSettingsDirty
import imgui.ImGui.menuItem
import imgui.ImGui.popItemFlag
import imgui.ImGui.pushItemFlag
import imgui.ImGui.separator
import imgui.ImGui.setWindowClipRectBeforeSetChannel
import imgui.ImGui.tableGetMinColumnWidth
import imgui.ImGui.tableSetBgColor
import imgui.ImGui.tableSetColumnWidth
import imgui.ImGui.tableSettingsFindByID
import imgui.api.g
import imgui.classes.TableColumnSortSpecs
import imgui.classes.TableSortSpecs
import imgui.internal.*
import imgui.internal.sections.ItemFlag
import imgui.internal.sections.NavLayer
import imgui.internal.sections.or
import kool.BYTES
import imgui.TableColumnFlag as Tcf
import imgui.TableFlag as Tf
import imgui.TableRowFlag as Trf
import imgui.WindowFlag as Wf
import imgui.internal.sections.ButtonFlag as Bf


/** FIXME-TABLE: transient data could be stored in a per-stacked table structure: DrawSplitter, SortSpecs, incoming RowData */
class Table {

    var id: ID = 0

    var flags = Tf.None.i

    /** Single allocation to hold Columns[], DisplayOrderToIndex[] and RowCellData[] */
//    var rawData: Any? = null

    /** Point within RawData[] */
    val columns = ArrayList<TableColumn>()

    /** Point within RawData[]. Store display order of columns (when not reordered, the values are 0...Count-1) */
    var displayOrderToIndex = intArrayOf()

    /** Point within RawData[]. Store cells background requests for current row. */
    var rowCellData = arrayOf<TableCellData>()

    /** Column DisplayOrder -> IsEnabled map */
    var enabledMaskByDisplayOrder = 0L

    /** Column Index -> IsEnabled map (== not hidden by user/api) in a format adequate for iterating column without touching cold data */
    var enabledMaskByIndex = 0L

    /** Column Index -> IsVisibleX|IsVisibleY map (== not hidden by user/api && not hidden by scrolling/cliprect) */
    var visibleMaskByIndex = 0L

    /** Column Index -> IsVisible || AutoFit (== expect user to submit items) */
    var requestOutputMaskByIndex = 0L

    /** Which data were loaded from the .ini file (e.g. when order is not altered we won't save order) */
    var settingsLoadedFlags = Tf.None.i

    /** Offset in g.SettingsTables */
    var settingsOffset = 0

    var lastFrameActive = -1

    /** Number of columns declared in BeginTable() */
    var columnsCount = 0

    var currentRow = 0

    var currentColumn = 0

    /** Count of BeginTable() calls with same ID in the same frame (generally 0). This is a little bit similar to BeginCount for a window, but multiple table with same ID look are multiple tables, they are just synched. */
    var instanceCurrent = 0

    /** Mark which instance (generally 0) of the same ID is being interacted with */
    var instanceInteracted = 0

    var rowPosY1 = 0f

    var rowPosY2 = 0f

    /** Height submitted to TableNextRow() */
    var rowMinHeight = 0f

    var rowTextBaseline = 0f

    var rowIndentOffsetX = 0f

    /** Current row flags, see ImGuiTableRowFlags_ */
    var rowFlags = Trf.None.i

    var lastRowFlags = Trf.None.i

    /** Counter for alternating background colors (can be fast-forwarded by e.g clipper), not same as CurrentRow because header rows typically don't increase this. */
    var rowBgColorCounter = 0

    val rowBgColor = IntArray(2)              // Background color override for current row.

    var borderColorStrong = 0

    var borderColorLight = 0

    var borderX1 = 0f

    var borderX2 = 0f

    var hostIndentX = 0f

    var outerPaddingX = 0f

    /** Padding from each borders */
    var cellPaddingX = 0f

    /** Padding from each borders */
    var cellPaddingY = 0f

    /** Spacing between non-bordered cells */
    var cellSpacingX1 = 0f

    /** Spacing between non-bordered cells */
    var cellSpacingX2 = 0f

    /** Outer height from last frame */
    var lastOuterHeight = 0f

    /** Height of first row from last frame */
    var lastFirstRowHeight = 0f

    /** User value passed to BeginTable(), see comments at the top of BeginTable() for details. */
    var innerWidth = 0f

    /** Sum of current column width */
    var columnsTotalWidth = 0f

    /** Sum of ideal column width in order nothing to be clipped, used for auto-fitting and content width submission in outer window */
    var columnsAutoFitWidth = 0f

    var resizedColumnNextWidth = 0f

    /** Lock minimum contents width while resizing down in order to not create feedback loops. But we allow growing the table. */
    var resizeLockMinContentsX2 = 0f

    /** Reference scale to be able to rescale columns on font/dpi changes. */
    var refScale = 0f

    /** Note: OuterRect.Max.y is often FLT_MAX until EndTable(), unless a height has been specified in BeginTable(). */
    val outerRect = Rect()

    val workRect = Rect()

    var innerClipRect = Rect()

    /** We use this to cpu-clip cell background color fill */
    val bgClipRect = Rect()

    val bgClipRectForDrawCmd = Rect()

    /** This is used to check if we can eventually merge our columns draw calls into the current draw call of the current window. */
    val hostClipRect = Rect()

    /** Backup of InnerWindow->WorkRect at the end of BeginTable() */
    val hostBackupWorkRect = Rect()

    /** Backup of InnerWindow->ParentWorkRect at the end of BeginTable() */
    val hostBackupParentWorkRect = Rect()

    /** Backup of InnerWindow->ClipRect during PushTableBackground()/PopTableBackground() */
    val hostBackupClipRect = Rect()

    /** Backup of InnerWindow->DC.PrevLineSize at the end of BeginTable() */
    val hostBackupPrevLineSize = Vec2()

    /** Backup of InnerWindow->DC.CurrLineSize at the end of BeginTable() */
    val hostBackupCurrLineSize = Vec2()

    /** Backup of InnerWindow->DC.CursorMaxPos at the end of BeginTable() */
    val hostBackupCursorMaxPos = Vec2()

    /** Backup of OuterWindow->DC.ColumnsOffset at the end of BeginTable() */
    var hostBackupColumnsOffset = 0f

    /** Backup of OuterWindow->DC.ItemWidth at the end of BeginTable() */
    var hostBackupItemWidth = 0f

    /** Backup of OuterWindow->DC.ItemWidthStack.Size at the end of BeginTable() */
    var hostBackupItemWidthStackSize = 0

    /** Parent window for the table */
    var outerWindow: Window? = null

    /** Window holding the table data (== OuterWindow or a child window) */
    var innerWindow: Window? = null

    /** Contiguous buffer holding columns names */
    val columnsNames = ArrayList<String>()

    /** We carry our own ImDrawList splitter to allow recursion (FIXME: could be stored outside, worst case we need 1 splitter per recursing table) */
    val drawSplitter = DrawListSplitter()

    val sortSpecsSingle = TableColumnSortSpecs()

    /** FIXME-OPT: Using a small-vector pattern would work be good. */
    val sortSpecsMulti = ArrayList<TableColumnSortSpecs>()

    /** Public facing sorts specs, this is what we return in TableGetSortSpecs() */
    val sortSpecs = TableSortSpecs()

    var sortSpecsCount: TableColumnIdx = 0

    /** Number of enabled columns (<= ColumnsCount) */
    var columnsEnabledCount: TableColumnIdx = 0

    /** Number of enabled columns (<= ColumnsCount) */
    var columnsEnabledFixedCount: TableColumnIdx = 0

    /** Count calls to TableSetupColumn() */
    var declColumnsCount: TableColumnIdx = 0

    /** Index of column whose visible region is being hovered. Important: == ColumnsCount when hovering empty region after the right-most column! */
    var hoveredColumnBody: TableColumnIdx = 0

    /** Index of column whose right-border is being hovered (for resizing). */
    var hoveredColumnBorder: TableColumnIdx = 0

    /** Index of single stretch column requesting auto-fit. */
    var autoFitSingleStretchColumn: TableColumnIdx = 0

    /** Index of column being resized. Reset when InstanceCurrent==0. */
    var resizedColumn: TableColumnIdx = 0

    /** Index of column being resized from previous frame. */
    var lastResizedColumn: TableColumnIdx = 0

    /** Index of column header being held. */
    var heldHeaderColumn: TableColumnIdx = 0

    /** Index of column being reordered. (not cleared) */
    var reorderColumn: TableColumnIdx = 0

    /** -1 or +1 */
    var reorderColumnDir: TableColumnIdx = 0

    /** Index of left-most stretched column. */
    var leftMostStretchedColumn: TableColumnIdx = 0

    /** Index of right-most stretched column. */
    var rightMostStretchedColumn: TableColumnIdx = 0

    /** Index of right-most non-hidden column. */
    var rightMostEnabledColumn: TableColumnIdx = 0

    /** Column right-clicked on, of -1 if opening context menu from a neutral/empty spot */
    var contextPopupColumn: TableColumnIdx = 0

    /** Requested frozen rows count */
    var freezeRowsRequest: TableColumnIdx = 0

    /** Actual frozen row count (== FreezeRowsRequest, or == 0 when no scrolling offset) */
    var freezeRowsCount: TableColumnIdx = 0

    /** Requested frozen columns count */
    var freezeColumnsRequest: TableColumnIdx = 0

    /** Actual frozen columns count (== FreezeColumnsRequest, or == 0 when no scrolling offset) */
    var freezeColumnsCount: TableColumnIdx = 0

    /** Index of current RowCellData[] entry in current row */
    var rowCellDataCurrent: TableColumnIdx = 0

    /** Redirect non-visible columns here. */
    var dummyDrawChannel: TableDrawChannelIdx = 0

    /** For Selectable() and other widgets drawing accross columns after the freezing line. Index within DrawSplitter.Channels[] */
    var bg1DrawChannelCurrent: TableDrawChannelIdx = 0

    var bg1DrawChannelUnfrozen: TableDrawChannelIdx = 0

    /** Set by TableUpdateLayout() which is called when beginning the first row. */
    var isLayoutLocked = false

    /** Set when inside TableBeginRow()/TableEndRow(). */
    var isInsideRow = false

    var isInitializing = false

    var isSortSpecsDirty = false

    /** Set when the first row had the ImGuiTableRowFlags_Headers flag. */
    var isUsingHeaders = false

    /** Set when default context menu is open (also see: ContextPopupColumn, InstanceInteracted). */
    var isContextPopupOpen = false

    var isSettingsRequestLoad = false

    /** Set when table settings have changed and needs to be reported into ImGuiTableSetttings data. */
    var isSettingsDirty = false

    /** Set when display order is unchanged from default (DisplayOrder contains 0...Count-1) */
    var isDefaultDisplayOrder = false

    var isResetAllRequest = false

    var isResetDisplayOrderRequest = false

    /** Set when we got past the frozen row. */
    var isUnfrozen = false

    /** Set when outer_size value passed to BeginTable() is (>= -1.0f && <= 0.0f) */
    var isOuterRectFitX = false

    var memoryCompacted = false

    /** Backup of InnerWindow->SkipItem at the end of BeginTable(), because we will overwrite InnerWindow->SkipItem on a per-column basis */
    var hostSkipItems = false


    // Tables: Internals

    /** For reference, the average total _allocation count_ for a table is:
     *  + 0 (for ImGuiTable instance, we are pooling allocations in g.Tables)
     *  + 1 (for table->RawData allocated below)
     *  + 1 (for table->ColumnsNames, if names are used)
     *  + 1 (for table->Splitter._Channels)
     *  + 2 * active_channels_count (for ImDrawCmd and ImDrawIdx buffers inside channels)
     *  Where active_channels_count is variable but often == columns_count or columns_count + 1, see TableSetupDrawChannels() for details.
     *  Unused channels don't perform their +2 allocations.
     *
     *  ~tableBeginInitMemory */
    infix fun beginInitMemory(columnsCount: Int) {
        // Allocate single buffer for our arrays
//        ImSpanAllocator<3> span_allocator;
        repeat(columnsCount) {
            columns += TableColumn()
        }
        displayOrderToIndex += IntArray(columnsCount)
        rowCellData += Array(columnsCount) { TableCellData() }
//        span_allocator.ReserveBytes(0, columnsCount1 * sizeof(ImGuiTableColumn));
//        span_allocator.ReserveBytes(1, columnsCount1 * sizeof(ImGuiTableColumnIdx));
//        span_allocator.ReserveBytes(2, columnsCount1 * sizeof(ImGuiTableCellData));
//        table->RawData = IM_ALLOC(span_allocator.GetArenaSizeInBytes());
//        memset(table->RawData, 0, span_allocator.GetArenaSizeInBytes());
//        span_allocator.SetArenaBasePtr(table->RawData);
//        span_allocator.GetSpan(0, &table->Columns);
//        span_allocator.GetSpan(1, &table->DisplayOrderToIndex);
//        span_allocator.GetSpan(2, &table->RowCellData);
    }

    /** Apply queued resizing/reordering/hiding requests
     *  ~TableBeginApplyRequests */
    fun beginApplyRequests() {
        // Handle resizing request
        // (We process this at the first TableBegin of the frame)
        // FIXME-TABLE: Contains columns if our work area doesn't allow for scrolling?
        if (instanceCurrent == 0) {
            if (resizedColumn != -1 && resizedColumnNextWidth != Float.MAX_VALUE)
                tableSetColumnWidth(resizedColumn, resizedColumnNextWidth)
            lastResizedColumn = resizedColumn
            resizedColumnNextWidth = Float.MAX_VALUE
            resizedColumn = -1

            // Process auto-fit for single stretch column, which is a special case
            // FIXME-TABLE: Would be nice to redistribute available stretch space accordingly to other weights, instead of giving it all to siblings.
            if (autoFitSingleStretchColumn != -1) {
                tableSetColumnWidth(autoFitSingleStretchColumn, columns[autoFitSingleStretchColumn].widthAuto)
                autoFitSingleStretchColumn = -1
            }
        }

        // Handle reordering request
        // Note: we don't clear ReorderColumn after handling the request.
        if (instanceCurrent == 0) {
            if (heldHeaderColumn == -1 && reorderColumn != -1)
                reorderColumn = -1
            heldHeaderColumn = -1
            if (reorderColumn != -1 && reorderColumnDir != 0) {
                // We need to handle reordering across hidden columns.
                // In the configuration below, moving C to the right of E will lead to:
                //    ... C [D] E  --->  ... [D] E  C   (Column name/index)
                //    ... 2  3  4        ...  2  3  4   (Display order)
                val reorderDir = reorderColumnDir
                assert(reorderDir == -1 || reorderDir == +1)
                assert(flags has Tf.Reorderable)
                val srcColumn = columns[reorderColumn]
                val dstColumn = columns[if (reorderDir == -1) srcColumn.prevEnabledColumn else srcColumn.nextEnabledColumn]
                val srcOrder = srcColumn.displayOrder
                val dstOrder = dstColumn.displayOrder
                srcColumn.displayOrder = dstOrder
                var orderN = srcOrder + reorderDir
                while (orderN != dstOrder + reorderDir) {
                    columns[displayOrderToIndex[orderN]].displayOrder -= reorderDir
                    orderN += reorderDir
                }
                assert(dstColumn.displayOrder == dstOrder - reorderDir)

                // Display order is stored in both columns->IndexDisplayOrder and table->DisplayOrder[],
                // rebuild the later from the former.
                for (columnN in 0 until columnsCount)
                    displayOrderToIndex[columns[columnN].displayOrder] = columnN
                reorderColumnDir = 0
                isSettingsDirty = true
            }
        }

        // Handle display order reset request
        if (isResetDisplayOrderRequest) {
            for (n in 0 until columnsCount) {
                displayOrderToIndex[n] = n
                columns[n].displayOrder = n
            }
            isResetDisplayOrderRequest = false
            isSettingsDirty = true
        }
    }

    /** Allocate draw channels. Called by TableUpdateLayout()
     *  - We allocate them following storage order instead of display order so reordering columns won't needlessly
     *    increase overall dormant memory cost.
     *  - We isolate headers draw commands in their own channels instead of just altering clip rects.
     *    This is in order to facilitate merging of draw commands.
     *  - After crossing FreezeRowsCount, all columns see their current draw channel changed to a second set of channels.
     *  - We only use the dummy draw channel so we can push a null clipping rectangle into it without affecting other
     *    channels, while simplifying per-row/per-cell overhead. It will be empty and discarded when merged.
     *  - We allocate 1 or 2 background draw channels. This is because we know PushTableBackground() is only used for
     *    horizontal spanning. If we allowed vertical spanning we'd need one background draw channel per merge group (1-4).
     *  Draw channel allocation (before merging):
     *  - NoClip                       --> 2+D+1 channels: bg0 + bg1 + foreground (same clip rect == 1 draw call) (FIXME-TABLE: could merge bg1 and foreground?)
     *  - Clip                         --> 2+D+N channels
     *  - FreezeRows                   --> 2+D+N*2 (unless scrolling value is zero)
     *  - FreezeRows || FreezeColunns  --> 3+D+N*2 (unless scrolling value is zero)
     *  Where D is 1 if any column is clipped or hidden (dummy channel) otherwise 0.
     *  ~TableSetupDrawChannels */
    fun setupDrawChannels() {
        val freezeRowMultiplier = if (freezeRowsCount > 0) 2 else 1
        val channelsForRow = if (flags has Tf.NoClip) 1 else columnsEnabledCount
        val channelsForBg = 1 + 1 * freezeRowMultiplier
        val channelsForDummy = (columnsEnabledCount < columnsCount || visibleMaskByIndex != enabledMaskByIndex).i
        val channelsTotal = channelsForBg + (channelsForRow * freezeRowMultiplier) + channelsForDummy
        drawSplitter.split(innerWindow!!.drawList, channelsTotal)
        dummyDrawChannel = if (channelsForDummy > 0) channelsTotal - 1 else -1
        bg1DrawChannelCurrent = TABLE_DRAW_CHANNEL_BG1_FROZEN
        bg1DrawChannelUnfrozen = if (freezeRowsCount > 0) 2 + channelsForRow else TABLE_DRAW_CHANNEL_BG1_FROZEN

        var drawChannelCurrent = 2
        for (columnN in 0 until columnsCount) {
            val column = columns[columnN]
            if (column.isVisibleX && column.isVisibleY) {
                column.drawChannelFrozen = drawChannelCurrent
                column.drawChannelUnfrozen = drawChannelCurrent + if (freezeRowsCount > 0) channelsForRow + 1 else 0
                if (flags hasnt Tf.NoClip)
                    drawChannelCurrent++
            } else {
                column.drawChannelFrozen = dummyDrawChannel
                column.drawChannelUnfrozen = dummyDrawChannel
            }
            column.drawChannelCurrent = column.drawChannelFrozen
        }

        // Initial draw cmd starts with a BgClipRect that matches the one of its host, to facilitate merge draw commands by default.
        // All our cell highlight are manually clipped with BgClipRect. When unfreezing it will be made smaller to fit scrolling rect.
        // (This technically isn't part of setting up draw channels, but is reasonably related to be done here)
        bgClipRect put innerClipRect
        bgClipRectForDrawCmd put hostClipRect
        assert(bgClipRect.min.y <= bgClipRect.max.y)
    }

    /** Layout columns for the frame. This is in essence the followup to BeginTable().
     *  Runs on the first call to TableNextRow(), to give a chance for TableSetupColumn() to be called first.
     *  FIXME-TABLE: Our width (and therefore our WorkRect) will be minimal in the first frame for WidthAutoResize
     *  columns, increase feedback side-effect with widgets relying on WorkRect.Max.x. Maybe provide a default distribution
     *  for WidthAutoResize columns?
     *  ~TableUpdateLayout */
    fun updateLayout() {

        assert(!isLayoutLocked)

        // [Part 1] Apply/lock Enabled and Order states.
        // Process columns in their visible orders as we are building the Prev/Next indices.
        var lastVisibleColumnIdx = -1
        var wantColumnAutoFit = false
        isDefaultDisplayOrder = true
        columnsEnabledCount = 0
        enabledMaskByIndex = 0x00
        enabledMaskByDisplayOrder = 0x00
        for (orderN in 0 until columnsCount) {
            val columnN = displayOrderToIndex[orderN]
            if (columnN != orderN)
                isDefaultDisplayOrder = false
            val column = columns[columnN]

            // Clear column settings if not submitted by user.
            // Currently we make it mandatory to call TableSetupColumn() every frame.
            // It would easily work without but we're ready to guarantee it since e.g. names need resubmission anyway.
            // In theory we could be calling TableSetupColumn() here with dummy values it should yield the same effect.
            if (columnN >= declColumnsCount) {
                setupColumnFlags(column, Tcf.None.i)
                column.nameOffset = -1
                column.userID = 0
                column.initStretchWeightOrWidth = -1f
            }

            if (flags hasnt Tf.Hideable || flags has Tcf.NoHide)
                column.isEnabledNextFrame = true
            if (column.isEnabled != column.isEnabledNextFrame) {
                column.isEnabled = column.isEnabledNextFrame
                isSettingsDirty = true
                if (!column.isEnabled && column.sortOrder != -1)
                    isSortSpecsDirty = true
            }
            if (column.sortOrder > 0 && flags hasnt Tf.SortMulti)
                isSortSpecsDirty = true

            val startAutoFit = when {
                column.flags has (Tcf.WidthFixed or Tcf.WidthAutoResize) -> column.widthRequest
                else -> column.stretchWeight
            } < 0f
            if (startAutoFit) {
                column.autoFitQueue = (1 shl 3) - 1 // Fit for three frames
                column.cannotSkipItemsQueue = (1 shl 3) - 1 // Fit for three frames
            }

            if (column.autoFitQueue != 0x00)
                wantColumnAutoFit = true

            val indexMask = 1L shl columnN
            val displayOrderMask = 1L shl column.displayOrder
            if (column.isEnabled) {
                // Mark as enabled and link to previous/next enabled column
                column.prevEnabledColumn = lastVisibleColumnIdx
                column.nextEnabledColumn = -1
                if (lastVisibleColumnIdx != -1)
                    columns[lastVisibleColumnIdx].nextEnabledColumn = columnN
                column.indexWithinEnabledSet = columnsEnabledCount
                columnsEnabledCount++
                enabledMaskByIndex = enabledMaskByIndex or indexMask
                enabledMaskByDisplayOrder = enabledMaskByDisplayOrder or displayOrderMask
                lastVisibleColumnIdx = columnN
            } else column.indexWithinEnabledSet = -1
            assert(column.indexWithinEnabledSet <= column.displayOrder)
        }
        if (flags has Tf.Sortable && sortSpecsCount == 0 && flags hasnt Tf.SortTristate)
            isSortSpecsDirty = true
        rightMostEnabledColumn = lastVisibleColumnIdx
        assert(rightMostEnabledColumn >= 0)

        // [Part 2] Disable child window clipping while fitting columns. This is not strictly necessary but makes it possible
        // to avoid the column fitting to wait until the first visible frame of the child container (may or not be a good thing).
        // FIXME-TABLE: for always auto-resizing columns may not want to do that all the time.
        if (wantColumnAutoFit && outerWindow !== innerWindow)
            innerWindow!!.skipItems = false
        if (wantColumnAutoFit)
            isSettingsDirty = true

        // [Part 3] Fix column flags. Calculate ideal width for columns. Count how many fixed/stretch columns we have and sum of weights.
        val minColumnWidth = tableGetMinColumnWidth()
        val minColumnDistance = minColumnWidth + cellPaddingX * 2f + cellSpacingX1 + cellSpacingX2
        var countFixed = 0                    // Number of columns that have fixed sizing policy (not stretched sizing policy) (this is NOT the opposite of count_resizable!)
        var countResizable = 0                // Number of columns the user can resize (this is NOT the opposite of count_fixed!)
        var sumWeightsStretched = 0f     // Sum of all weights for weighted columns.
        var sumWidthFixedRequests = 0f  // Sum of all width for fixed and auto-resize columns, excluding width contributed by Stretch columns.
        var maxWidthAuto = 0f            // Largest auto-width (used for SameWidths feature)
        leftMostStretchedColumn = -1
        rightMostStretchedColumn = -1
        for (columnN in 0 until columnsCount) {
            if (enabledMaskByIndex hasnt (1L shl columnN))
                continue
            val column = columns[columnN]

            // Count resizable columns
            if (column.flags hasnt Tcf.NoResize)
                countResizable++

            // Calculate ideal/auto column width (that's the width required for all contents to be visible without clipping)
            // Combine width from regular rows + width from headers unless requested not to.
            if (!column.isPreserveWidthAuto) {
                val contentWidthBody = (column.contentMaxXFrozen max column.contentMaxXUnfrozen) - column.workMinX
                val contentWidthHeaders = column.contentMaxXHeadersIdeal - column.workMinX
                var widthAuto = contentWidthBody
                if (flags hasnt Tf.NoHeadersWidth && column.flags hasnt Tcf.NoHeaderWidth)
                    widthAuto = widthAuto max contentWidthHeaders
                widthAuto = widthAuto max minColumnWidth

                // Non-resizable columns also submit their requested width
                if (column.flags has Tcf.WidthFixed && column.initStretchWeightOrWidth > 0f)
                    if (flags hasnt Tf.Resizable || column.flags has Tcf.NoResize)
                        widthAuto = widthAuto max column.initStretchWeightOrWidth

                column.widthAuto = widthAuto
            }
            column.isPreserveWidthAuto = false

            if (column.flags has (Tcf.WidthFixed or Tcf.WidthAutoResize)) {
                // Process auto-fit for non-stretched columns
                // Latch initial size for fixed columns and update it constantly for auto-resizing column (unless clipped!)
                if (column.autoFitQueue != 0x00 || (column.flags has Tcf.WidthAutoResize && column.isVisibleX))
                    column.widthRequest = column.widthAuto

                // FIXME-TABLE: Increase minimum size during init frame to avoid biasing auto-fitting widgets
                // (e.g. TextWrapped) too much. Otherwise what tends to happen is that TextWrapped would output a very
                // large height (= first frame scrollbar display very off + clipper would skip lots of items).
                // This is merely making the side-effect less extreme, but doesn't properly fixes it.
                // FIXME: Move this to ->WidthGiven to avoid temporary lossyless?
                if (column.autoFitQueue > 0x01 && isInitializing)
                    column.widthRequest = column.widthRequest max (minColumnWidth * 4f) // FIXME-TABLE: Another constant/scale?
                countFixed += 1
                sumWidthFixedRequests += column.widthRequest
            } else {
                assert(column.flags has Tcf.WidthStretch)

                // Revert or initialize weight (when column->StretchWeight < 0.0f normally it means there has been no init value so it'll always default to 1.0f)
                if (column.autoFitQueue != 0x00 || column.stretchWeight < 0f)
                    column.stretchWeight = if (column.initStretchWeightOrWidth > 0f) column.initStretchWeightOrWidth else 1f

                sumWeightsStretched += column.stretchWeight
                if (leftMostStretchedColumn == -1 || columns[leftMostStretchedColumn].displayOrder > column.displayOrder)
                    leftMostStretchedColumn = columnN
                if (rightMostStretchedColumn == -1 || columns[rightMostStretchedColumn].displayOrder < column.displayOrder)
                    rightMostStretchedColumn = columnN
            }
            maxWidthAuto = maxWidthAuto max column.widthAuto
            sumWidthFixedRequests += cellPaddingX * 2f
        }
        columnsEnabledFixedCount = countFixed

        // [Part 4] Apply "same widths" feature.
        // - When all columns are fixed or columns are of mixed type: use the maximum auto width.
        // - When all columns are stretch: use same weight.
        val mixedSameWidths = flags has Tf.SameWidths && countFixed > 0
        if (flags has Tf.SameWidths)
            for (columnN in 0 until columnsCount) {
                if (enabledMaskByIndex hasnt (1L shl columnN))
                    continue
                val column = columns[columnN]
                if (column.flags has (Tcf.WidthFixed or Tcf.WidthAutoResize)) {
                    sumWidthFixedRequests += maxWidthAuto - column.widthRequest // Update old sum
                    column.widthRequest = maxWidthAuto
                } else {
                    sumWeightsStretched += 1f - column.stretchWeight // Update old sum
                    column.stretchWeight = 1f
                    if (mixedSameWidths)
                        column.widthRequest = maxWidthAuto
                }
            }

        // [Part 5] Apply final widths based on requested widths
//        val work_rect = table->WorkRect; [JVM] we use the same instance!
        val widthSpacings = outerPaddingX * 2f + (cellSpacingX1 + cellSpacingX2) * (columnsEnabledCount - 1)
        val widthAvail = if (flags has Tf.ScrollX && innerWidth == 0f) innerClipRect.width else workRect.width
        val widthAvailForStretchedColumns = if (mixedSameWidths) 0f else widthAvail - widthSpacings - sumWidthFixedRequests
        var widthRemainingForStretchedColumns = widthAvailForStretchedColumns
        columnsTotalWidth = widthSpacings
        columnsAutoFitWidth = widthSpacings
        for (columnN in 0 until columnsCount) {
            if (enabledMaskByIndex hasnt (1L shl columnN))
                continue
            val column = columns[columnN]

            // Allocate width for stretched/weighted columns (StretchWeight gets converted into WidthRequest)
            if ((column.flags has Tcf.WidthStretch) && !mixedSameWidths) {
                val weightRatio = column.stretchWeight / sumWeightsStretched
                column.widthRequest = floor(max(widthAvailForStretchedColumns * weightRatio, minColumnWidth) + 0.01f)
                widthRemainingForStretchedColumns -= column.widthRequest
            }

            // [Resize Rule 1] The right-most Visible column is not resizable if there is at least one Stretch column
            // See additional comments in TableSetColumnWidth().
            if (column.nextEnabledColumn == -1 && leftMostStretchedColumn != -1)
                column.flags = column.flags or Tcf.NoDirectResize_

            // Assign final width, record width in case we will need to shrink
            column.widthGiven = floor(column.widthRequest max minColumnWidth)
            columnsTotalWidth += column.widthGiven + cellPaddingX * 2f
            columnsAutoFitWidth += column.widthAuto + cellPaddingX * 2f
        }

        // [Part 6] Redistribute stretch remainder width due to rounding (remainder width is < 1.0f * number of Stretch column).
        // Using right-to-left distribution (more likely to match resizing cursor).
        if (widthRemainingForStretchedColumns >= 1f && flags hasnt Tf.PreciseWidths) {
            var orderN = columnsCount - 1
            while (sumWeightsStretched > 0f && widthRemainingForStretchedColumns >= 1f && orderN >= 0) {
                if (enabledMaskByDisplayOrder hasnt (1L shl orderN)) {
                    orderN--
                    continue
                }
                val column = columns[displayOrderToIndex[orderN]]
                if (column.flags hasnt Tcf.WidthStretch) {
                    orderN--
                    continue
                }
                column.widthRequest += 1f
                column.widthGiven += 1f
                widthRemainingForStretchedColumns -= 1f
                orderN--
            }
        }

        hoveredColumnBody = -1
        hoveredColumnBorder = -1
        val mouseHitRect = Rect(outerRect.min.x, outerRect.min.y, outerRect.max.x, outerRect.max.y max (outerRect.min.y + lastOuterHeight))
        val isHoveringTable = itemHoverable(mouseHitRect, 0)

        // [Part 7] Setup final position, offset, skip/clip states and clipping rectangles, detect hovered column
        // Process columns in their visible orders as we are comparing the visible order and adjusting host_clip_rect while looping.
        var visibleN = 0
        var offsetX = (if (freezeColumnsCount > 0) outerRect.min.x else workRect.min.x) + outerPaddingX - cellSpacingX1
        val hostClipRect = Rect(innerClipRect) // [JVM] local copy, shadow is fine
        //host_clip_rect.Max.x += table->CellPaddingX + table->CellSpacingX2;
        visibleMaskByIndex = 0x00
        requestOutputMaskByIndex = 0x00
        for (orderN in 0 until columnsCount) {
            val columnN = displayOrderToIndex[orderN]
            val column = columns[columnN]

            column.navLayerCurrent = if (freezeRowsCount > 0 || columnN < freezeColumnsCount) NavLayer.Menu else NavLayer.Main

            if (freezeColumnsCount > 0 && freezeColumnsCount == visibleN)
                offsetX += workRect.min.x - outerRect.min.x

            // Clear status flags
            column.flags = column.flags wo Tcf.StatusMask_

            if (enabledMaskByDisplayOrder hasnt (1L shl orderN)) {
                // Hidden column: clear a few fields and we are done with it for the remainder of the function.
                // We set a zero-width clip rect but set Min.y/Max.y properly to not interfere with the clipper.
                column.minX = offsetX
                column.maxX = offsetX
                column.workMinX = offsetX
                column.clipRect.min.x = offsetX
                column.clipRect.max.x = offsetX
                column.widthGiven = 0f
                column.clipRect.min.y = workRect.min.y
                column.clipRect.max.y = Float.MAX_VALUE
                column.clipRect clipWithFull hostClipRect
                column.isVisibleX = false
                column.isVisibleY = false
                column.isRequestOutput = false
                column.isSkipItems = true
                column.itemWidth = 1f
                continue
            }

            // Detect hovered column
            if (isHoveringTable && g.io.mousePos.x >= column.clipRect.min.x && g.io.mousePos.x < column.clipRect.max.x)
                hoveredColumnBody = columnN

            // Maximum width
            var maxWidth = Float.MAX_VALUE
            if (flags has Tf.ScrollX) {
                // Frozen columns can't reach beyond visible width else scrolling will naturally break.
                if (orderN < freezeColumnsRequest) {
                    maxWidth = (innerClipRect.max.x - (freezeColumnsRequest - orderN) * minColumnDistance) - offsetX
                    maxWidth -= outerPaddingX + cellPaddingX + cellSpacingX2
                }
            } else if (flags hasnt Tf.NoKeepColumnsVisible) {
                // If horizontal scrolling if disabled, we apply a final lossless shrinking of columns in order to make
                // sure they are all visible. Because of this we also know that all of the columns will always fit in
                // table->WorkRect and therefore in table->InnerRect (because ScrollX is off)
                // FIXME-TABLE: This is solved incorrectly but also quite a difficult problem to fix as we also want ClipRect width to match.
                // See "table_width_distrib" and "table_width_keep_visible" tests
                maxWidth = workRect.max.x - (columnsEnabledCount - column.indexWithinEnabledSet - 1) * minColumnDistance - offsetX
                //max_width -= table->CellSpacingX1;
                maxWidth -= cellSpacingX2 + cellPaddingX * 2f + outerPaddingX
            }
            column.widthGiven = column.widthGiven min maxWidth

            // Minimum width
            column.widthGiven = column.widthGiven max min(column.widthRequest, minColumnWidth)

            // Lock all our positions
            // - ClipRect.Min.x: Because merging draw commands doesn't compare min boundaries, we make ClipRect.Min.x match left bounds to be consistent regardless of merging.
            // - ClipRect.Max.x: using WorkMaxX instead of MaxX (aka including padding) makes things more consistent when resizing down, tho slightly detrimental to visibility in very-small column.
            // - ClipRect.Max.x: using MaxX makes it easier for header to receive hover highlight with no discontinuity and display sorting arrow.
            // - FIXME-TABLE: We want equal width columns to have equal (ClipRect.Max.x - WorkMinX) width, which means ClipRect.max.x cannot stray off host_clip_rect.Max.x else right-most column may appear shorter.
            column.minX = offsetX
            column.maxX = offsetX + column.widthGiven + cellSpacingX1 + cellSpacingX2 + cellPaddingX * 2f
            column.workMinX = column.minX + cellPaddingX + cellSpacingX1
            column.workMaxX = column.maxX - cellPaddingX - cellSpacingX2 // Expected max
            column.itemWidth = floor(column.widthGiven * 0.65f)
            column.clipRect.min.x = column.minX
            column.clipRect.min.y = workRect.min.y
            column.clipRect.max.x = column.maxX //column->WorkMaxX;
            column.clipRect.max.y = Float.MAX_VALUE
            column.clipRect clipWithFull hostClipRect

            // Mark column as Clipped (not in sight)
            // Note that scrolling tables (where inner_window != outer_window) handle Y clipped earlier in BeginTable() so IsVisibleY really only applies to non-scrolling tables.
            // FIXME-TABLE: Because InnerClipRect.Max.y is conservatively ==outer_window->ClipRect.Max.y, we never can mark columns _Above_ the scroll line as not IsVisibleY.
            // Taking advantage of LastOuterHeight would yield good results there...
            // FIXME-TABLE: Y clipping is disabled because it effectively means not submitting will reduce contents width which is fed to outer_window->DC.CursorMaxPos.x,
            // and this may be used (e.g. typically by outer_window using AlwaysAutoResize or outer_window's horizontal scrollbar, but could be something else).
            // Possible solution to preserve last known content width for clipped column. Test 'table_reported_size' fails when enabling Y clipping and window is resized small.
            column.isVisibleX = column.clipRect.max.x > column.clipRect.min.x
            column.isVisibleY = true // (column->ClipRect.Max.y > column->ClipRect.Min.y);
            val isVisible = column.isVisibleX //&& column->IsVisibleY;
            if (isVisible)
                visibleMaskByIndex = visibleMaskByIndex or (1L shl columnN)

            // Mark column as requesting output from user. Note that fixed + non-resizable sets are auto-fitting at all times and therefore always request output.
            column.isRequestOutput = isVisible || column.autoFitQueue != 0 || column.cannotSkipItemsQueue != 0
            if (column.isRequestOutput)
                requestOutputMaskByIndex = requestOutputMaskByIndex or (1L shl columnN)

            // Mark column as SkipItems (ignoring all items/layout)
            column.isSkipItems = !column.isEnabled || hostSkipItems
            if (column.isSkipItems)
                assert(!isVisible)

            // Update status flags
            column.flags = column.flags or Tcf.IsEnabled
            if (isVisible)
                column.flags = column.flags or Tcf.IsVisible
            if (column.sortOrder != -1)
                column.flags = column.flags or Tcf.IsSorted
            if (hoveredColumnBody == columnN)
                column.flags = column.flags or Tcf.IsHovered

            // Alignment
            // FIXME-TABLE: This align based on the whole column width, not per-cell, and therefore isn't useful in
            // many cases (to be able to honor this we might be able to store a log of cells width, per row, for
            // visible rows, but nav/programmatic scroll would have visible artifacts.)
            //if (column->Flags & ImGuiTableColumnFlags_AlignRight)
            //    column->WorkMinX = ImMax(column->WorkMinX, column->MaxX - column->ContentWidthRowsUnfrozen);
            //else if (column->Flags & ImGuiTableColumnFlags_AlignCenter)
            //    column->WorkMinX = ImLerp(column->WorkMinX, ImMax(column->StartX, column->MaxX - column->ContentWidthRowsUnfrozen), 0.5f);

            // Reset content width variables
            column.contentMaxXFrozen = column.workMinX
            column.contentMaxXUnfrozen = column.workMinX
            column.contentMaxXHeadersUsed = column.workMinX
            column.contentMaxXHeadersIdeal = column.workMinX

            // Don't decrement auto-fit counters until container window got a chance to submit its items
            if (!hostSkipItems) {
                column.autoFitQueue = column.autoFitQueue ushr 1
                column.cannotSkipItemsQueue = column.cannotSkipItemsQueue ushr 1
            }

            if (visibleN < freezeColumnsCount)
                hostClipRect.min.x = hostClipRect.min.x max (column.maxX + TABLE_BORDER_SIZE)

            offsetX += column.widthGiven + cellSpacingX1 + cellSpacingX2 + cellPaddingX * 2f
            visibleN++
        }

        // [Part 8] Detect/store when we are hovering the unused space after the right-most column (so e.g. context menus can react on it)
        // Clear Resizable flag if none of our column are actually resizable (either via an explicit _NoResize flag, either
        // because of using _WidthAutoResize/_WidthStretch). This will hide the resizing option from the context menu.
        val unusedX1 = workRect.min.x max columns[rightMostEnabledColumn].clipRect.max.x
        if (isHoveringTable && hoveredColumnBody == -1) {
            if (g.io.mousePos.x >= unusedX1)
                hoveredColumnBody = columnsCount
        }
        if (countResizable == 0 && flags has Tf.Resizable)
            flags = flags wo Tf.Resizable

        // [Part 9] Lock actual OuterRect/WorkRect right-most position.
        // This is done late to handle the case of fixed-columns tables not claiming more widths that they need.
        // Because of this we are careful with uses of WorkRect and InnerClipRect before this point.
        if (flags has Tf.NoHostExtendX && innerWindow === outerWindow && rightMostStretchedColumn == -1) {
            outerRect.max.x = unusedX1
            workRect.max.x = unusedX1
            innerClipRect.max.x = innerClipRect.max.x min unusedX1
            isOuterRectFitX = false
        }
        innerWindow!!.parentWorkRect put workRect
        borderX1 = innerClipRect.min.x// +((table->Flags & ImGuiTableFlags_BordersOuter) ? 0.0f : -1.0f);
        borderX2 = innerClipRect.max.x// +((table->Flags & ImGuiTableFlags_BordersOuter) ? 0.0f : +1.0f);

        // [Part 10] Allocate draw channels and setup background cliprect
        setupDrawChannels()

        // [Part 11] Hit testing on borders
        if (flags has Tf.Resizable)
            updateBorders()
        lastFirstRowHeight = 0f
        isLayoutLocked = true
        isUsingHeaders = false

        // [Part 12] Context menu
        if (isContextPopupOpen && instanceCurrent == instanceInteracted) {
            val contextMenuId = hashStr("##ContextMenu", 0, id)
            if (beginPopupEx(contextMenuId, Wf.AlwaysAutoResize or Wf.NoTitleBar or Wf.NoSavedSettings)) {
                drawContextMenu()
                endPopup()
            } else isContextPopupOpen = false
        }

        // [Part 13] Sanitize and build sort specs before we have a change to use them for display.
        // This path will only be exercised when sort specs are modified before header rows (e.g. init or visibility change)
        if (isSortSpecsDirty && flags has Tf.Sortable)
            sortSpecsBuild()

        // Initial state
        innerWindow!!.let {
            if (flags has Tf.NoClip)
                drawSplitter.setCurrentChannel(it.drawList, TABLE_DRAW_CHANNEL_NOCLIP)
            else
                it.drawList.pushClipRect(it.clipRect.min, it.clipRect.max, false)
        }
    }

    /** Adjust flags: default width mode + stretch columns are not allowed when auto extending
     *
     *  ~static void TableSetupColumnFlags(ImGuiTable* table, ImGuiTableColumn* column, ImGuiTableColumnFlags flags_in) */
    fun setupColumnFlags(column: TableColumn, flagsIn: TableColumnFlags) {

        var flags = flagsIn

        // Sizing Policy
        if (flags hasnt Tcf.WidthMask_)
        // FIXME-TABLE: Inconsistent to promote columns to WidthAutoResize
            flags = flags or when {
                this.flags has Tf.ColumnsWidthFixed ->
                    if (this.flags has Tf.Resizable && flags hasnt Tcf.NoResize) Tcf.WidthFixed else Tcf.WidthAutoResize
                else -> Tcf.WidthStretch
            }
        assert((flags and Tcf.WidthMask_).isPowerOfTwo) { "Check that only 1 of each set is used." }
        if (flags has Tcf.WidthAutoResize)
            flags = flags or Tcf.NoResize

        // Sorting
        if (flags has Tcf.NoSortAscending && flags has Tcf.NoSortDescending)
            flags = flags or Tcf.NoSort

        // Indentation
        if (flags hasnt Tcf.IndentMask_)
            flags = flags or if (columns.indexOf(column) == 0) Tcf.IndentEnable else Tcf.IndentDisable

        // Alignment
        //if ((flags & ImGuiTableColumnFlags_AlignMask_) == 0)
        //    flags |= ImGuiTableColumnFlags_AlignCenter;
        //IM_ASSERT(ImIsPowerOfTwo(flags & ImGuiTableColumnFlags_AlignMask_)); // Check that only 1 of each set is used.

        // Preserve status flags
        column.flags = flags or (column.flags and Tcf.StatusMask_)

        // Build an ordered list of available sort directions
        column.sortDirectionsAvailCount = 0
        column.sortDirectionsAvailMask = 0
        column.sortDirectionsAvailList = 0
        if (flags has Tf.Sortable) {
            var count = 0
            var mask = 0
            var list = 0
            if (flags has Tcf.PreferSortAscending && flags hasnt Tcf.NoSortAscending) {
                mask = mask or (1 shl SortDirection.Ascending.i)
                list = list or (SortDirection.Ascending.i shl (count shl 1))
                count++
            }
            if (flags has Tcf.PreferSortDescending && flags hasnt Tcf.NoSortDescending) {
                mask = mask or (1 shl SortDirection.Descending.i)
                list = list or (SortDirection.Descending.i shl (count shl 1))
                count++
            }
            if (flags hasnt Tcf.PreferSortAscending && flags hasnt Tcf.NoSortAscending) {
                mask = mask or (1 shl SortDirection.Ascending.i)
                list = list or (SortDirection.Ascending.i shl (count shl 1))
                count++
            }
            if (flags hasnt Tcf.PreferSortDescending && flags hasnt Tcf.NoSortDescending) {
                mask = mask or (1 shl SortDirection.Descending.i)
                list = list or (SortDirection.Descending.i shl (count shl 1))
                count++
            }
            if (flags has Tf.SortTristate || count == 0) {
                mask = mask or (1 shl SortDirection.None.i)
                count++
            }
            column.sortDirectionsAvailList = list
            column.sortDirectionsAvailMask = mask
            column.sortDirectionsAvailCount = count
            fixColumnSortDirection(column)
        }
    }

    /** Process hit-testing on resizing borders. Actual size change will be applied in EndTable()
     *  - Set table->HoveredColumnBorder with a short delay/timer to reduce feedback noise
     *  - Submit ahead of table contents and header, use ImGuiButtonFlags_AllowItemOverlap to prioritize widgets
     *    overlapping the same area.
     *  ~TableUpdateBorders */
    fun updateBorders() {

        assert(flags has Tf.Resizable)

        // At this point OuterRect height may be zero or under actual final height, so we rely on temporal coherency and
        // use the final height from last frame. Because this is only affecting _interaction_ with columns, it is not
        // really problematic (whereas the actual visual will be displayed in EndTable() and using the current frame height).
        // Actual columns highlight/render will be performed in EndTable() and not be affected.
        val hitHalfWidth = TABLE_RESIZE_SEPARATOR_HALF_THICKNESS
        val hitY1 = outerRect.min.y
        val hitY2Body = outerRect.max.y max (hitY1 + lastOuterHeight)
        val hitY2Head = hitY1 + lastFirstRowHeight

        for (orderN in 0 until columnsCount) {

            if (enabledMaskByDisplayOrder hasnt (1L shl orderN))
                continue

            val columnN = displayOrderToIndex[orderN]
            val column = columns[columnN]
            if (column.flags has (Tcf.NoResize or Tcf.NoDirectResize_))
                continue

            // ImGuiTableFlags_NoBordersInBodyUntilResize will be honored in TableDrawBorders()
            val borderY2Hit = if (flags has Tf.NoBordersInBody) hitY2Head else hitY2Body
            if (flags has Tf.NoBordersInBody && !isUsingHeaders)
                continue

            val columnId = getColumnResizeID(columnN, instanceCurrent)
            val hitRect = Rect(column.maxX - hitHalfWidth, hitY1, column.maxX + hitHalfWidth, borderY2Hit)
            //GetForegroundDrawList()->AddRect(hit_rect.Min, hit_rect.Max, IM_COL32(255, 0, 0, 100));
            keepAliveID(columnId)

            var (pressed, hovered, held) = buttonBehavior(hitRect, columnId, Bf.FlattenChildren or Bf.AllowItemOverlap or Bf.PressedOnClick or Bf.PressedOnDoubleClick)
            if (pressed && isMouseDoubleClicked(MouseButton.Left)) {
                setColumnWidthAutoSingle(columnN)
                clearActiveID()
                held = false
                hovered = false
            }
            if (held) {
                if (lastResizedColumn == -1)
                    resizeLockMinContentsX2 = if (rightMostEnabledColumn != -1) columns[rightMostEnabledColumn].maxX else -Float.MAX_VALUE
                resizedColumn = columnN
                instanceInteracted = instanceCurrent
            }
            if ((hovered && g.hoveredIdTimer > TABLE_RESIZE_SEPARATOR_FEEDBACK_TIMER) || held) {
                hoveredColumnBorder = columnN
                ImGui.mouseCursor = MouseCursor.ResizeEW
            }
        }
    }

    /** FIXME-TABLE: This is a mess, need to redesign how we render borders (as some are also done in TableEndRow)
     *  ~TableDrawBorders */
    fun drawBorders() {
        val innerWindow = innerWindow!!
        val outerWindow = outerWindow!!
        drawSplitter.setCurrentChannel(innerWindow.drawList, TABLE_DRAW_CHANNEL_BG0)
        if (innerWindow.hidden || !hostClipRect.overlaps(innerClipRect))
            return
        val innerDrawlist = innerWindow.drawList
        val outerDrawlist = outerWindow.drawList

        // Draw inner border and resizing feedback
        val borderSize = TABLE_BORDER_SIZE
        val drawY1 = outerRect.min.y
        val drawY2Body = outerRect.max.y
        val drawY2Head = when {
            isUsingHeaders -> lastFirstRowHeight + if (freezeRowsCount >= 1) outerRect.min.y else workRect.min.y
            else -> drawY1
        }

        if (flags has Tf.BordersInnerV)
            for (orderN in 0 until columnsCount) {

                if (enabledMaskByDisplayOrder hasnt (1L shl orderN))
                    continue

                val columnN = displayOrderToIndex[orderN]
                val column = columns[columnN]
                val isHovered = hoveredColumnBorder == columnN
                val isResized = resizedColumn == columnN && instanceInteracted == instanceCurrent
                val isResizable = column.flags hasnt (Tcf.NoResize or Tcf.NoDirectResize_)

                if (column.maxX > innerClipRect.max.x && !isResized)// && is_hovered)
                    continue
                if (column.nextEnabledColumn == -1 && !isResizable)
                    if (flags hasnt Tf.SameWidths)
                        continue
                if (column.maxX <= column.clipRect.min.x) // FIXME-TABLE FIXME-STYLE: Assume BorderSize==1, this is problematic if we want to increase the border size..
                    continue

                // Draw in outer window so right-most column won't be clipped
                // Always draw full height border when being resized/hovered, or on the delimitation of frozen column scrolling.
                val col: Int
                val drawY2: Float
                if (isHovered || isResized || (freezeColumnsCount != -1 && freezeColumnsCount == orderN + 1)) {
                    drawY2 = drawY2Body
                    col = if (isResized) Col.SeparatorActive.u32 else if (isHovered) Col.SeparatorHovered.u32 else borderColorStrong
                } else {
                    drawY2 = if (flags has (Tf.NoBordersInBody or Tf.NoBordersInBodyUntilResize)) drawY2Head else drawY2Body
                    col = if (flags has (Tf.NoBordersInBody or Tf.NoBordersInBodyUntilResize)) borderColorStrong else borderColorLight
                }

                if (drawY2 > drawY1)
                    innerDrawlist.addLine(Vec2(column.maxX, drawY1), Vec2(column.maxX, drawY2), col, borderSize)
            }

        // Draw outer border
        // FIXME-TABLE: could use AddRect or explicit VLine/HLine helper?
        if (flags has Tf.BordersOuter) {
            // Display outer border offset by 1 which is a simple way to display it without adding an extra draw call
            // (Without the offset, in outer_window it would be rendered behind cells, because child windows are above their
            // parent. In inner_window, it won't reach out over scrollbars. Another weird solution would be to display part
            // of it in inner window, and the part that's over scrollbars in the outer window..)
            // Either solution currently won't allow us to use a larger border size: the border would clipped.
            val outerBorder = Rect(outerRect) // [JVM] we need a local copy, shadowing is fine
            val outerCol = borderColorStrong
            if (innerWindow !== outerWindow) // FIXME-TABLE
                outerBorder expand 1f
            if ((flags and Tf.BordersOuter) == Tf.BordersOuter.i)
                outerDrawlist.addRect(outerBorder.min, outerBorder.max, outerCol, 0f, 0.inv(), borderSize)
            else if (flags has Tf.BordersOuterV) {
                outerDrawlist.addLine(outerBorder.min, Vec2(outerBorder.min.x, outerBorder.max.y), outerCol, borderSize)
                outerDrawlist.addLine(Vec2(outerBorder.max.x, outerBorder.min.y), outerBorder.max, outerCol, borderSize)
            } else if (flags has Tf.BordersOuterH) {
                outerDrawlist.addLine(outerBorder.min, Vec2(outerBorder.max.x, outerBorder.min.y), outerCol, borderSize)
                outerDrawlist.addLine(Vec2(outerBorder.min.x, outerBorder.max.y), outerBorder.max, outerCol, borderSize)
            }
        }
        if (flags has Tf.BordersInnerH && rowPosY2 < outerRect.max.y) {
            // Draw bottom-most row border
            val borderY = rowPosY2
            if (borderY >= bgClipRect.min.y && borderY < bgClipRect.max.y)
                innerDrawlist.addLine(Vec2(borderX1, borderY), Vec2(borderX2, borderY), borderColorLight, borderSize)
        }
    }

    /** Output context menu into current window (generally a popup)
     *  FIXME-TABLE: Ideally this should be writable by the user. Full programmatic access to that data?
     *  ~TableDrawContextMenu */
    fun drawContextMenu() {

        val window = g.currentWindow!!
        if (window.skipItems)
            return

        var wantSeparator = false
        val columnN = if (contextPopupColumn in 0 until columnsCount) contextPopupColumn else -1
        val column = columns.getOrNull(columnN)

        // Sizing
        if (flags has Tf.Resizable) {
            if (column != null) {
                val canResize = column.flags hasnt Tcf.NoResize && column.isEnabled
                if (menuItem("Size column to fit###SizeOne", "", false, canResize))
                    setColumnWidthAutoSingle(columnN)
            }

            val sizeAllDesc = when {
                columnsEnabledFixedCount == columnsEnabledCount -> "Size all columns to fit###SizeAll"        // All fixed
                columnsEnabledFixedCount == 0 -> "Size all columns to default###SizeAll"    // All stretch
                else -> "Size all columns to fit/default###SizeAll"// Mixed
            }
            if (menuItem(sizeAllDesc, ""))
                setColumnWidthAutoAll()
            wantSeparator = true
        }

        // Ordering
        if (flags has Tf.Reorderable) {
            if (menuItem("Reset order", "", false, !isDefaultDisplayOrder))
                isResetDisplayOrderRequest = true
            wantSeparator = true
        }

        // Reset all (should work but seems unnecessary/noisy to expose?)
        //if (MenuItem("Reset all"))
        //    table->IsResetAllRequest = true;

        // Sorting
        // (modify TableOpenContextMenu() to add _Sortable flag if enabling this)
//        #if 0
//        if ((table->Flags & ImGuiTableFlags_Sortable) && column != NULL && (column->Flags & ImGuiTableColumnFlags_NoSort) == 0)
//        {
//            if (wantSeparator)
//                Separator()
//            wantSeparator = true
//
//            bool append_to_sort_specs = g.IO.KeyShift
//            if (MenuItem("Sort in Ascending Order", NULL, column->SortOrder != -1 && column->SortDirection == ImGuiSortDirection_Ascending, (column->Flags & ImGuiTableColumnFlags_NoSortAscending) == 0))
//            TableSetColumnSortDirection(table, columnN, ImGuiSortDirection_Ascending, append_to_sort_specs)
//            if (MenuItem("Sort in Descending Order", NULL, column->SortOrder != -1 && column->SortDirection == ImGuiSortDirection_Descending, (column->Flags & ImGuiTableColumnFlags_NoSortDescending) == 0))
//            TableSetColumnSortDirection(table, columnN, ImGuiSortDirection_Descending, append_to_sort_specs)
//        }
//        #endif

        // Hiding / Visibility
        if (flags has Tf.Hideable) {
            if (wantSeparator)
                separator()
            wantSeparator = true

            pushItemFlag(ItemFlag.SelectableDontClosePopup.i, true)
            for (otherColumnN in 0 until columnsCount) {
                val otherColumn = columns[otherColumnN]
                var name = getColumnName(otherColumnN)
                if (name == null || name.isEmpty())
                    name = "<Unknown>"

                // Make sure we can't hide the last active column
                var menuItemActive = flags hasnt Tcf.NoHide
                if (otherColumn.isEnabled && columnsEnabledCount <= 1)
                    menuItemActive = false
                if (menuItem(name, "", otherColumn.isEnabled, menuItemActive))
                    otherColumn.isEnabledNextFrame = !otherColumn.isEnabled
            }
            popItemFlag()
        }
    }

    /**
     * This function reorder draw channels based on matching clip rectangle, to facilitate merging them. Called by EndTable().
     * For simplicity we call it TableMergeDrawChannels() but in fact it only reorder channels + overwrite ClipRect,
     * actual merging is done by table->DrawSplitter.Merge() which is called right after TableMergeDrawChannels().
     *
     * Columns where the contents didn't stray off their local clip rectangle can be merged. To achieve
     * this we merge their clip rect and make them contiguous in the channel list, so they can be merged
     * by the call to DrawSplitter.Merge() following to the call to this function.
     * We reorder draw commands by arranging them into a maximum of 4 distinct groups:
     *
     *   1 group:               2 groups:              2 groups:              4 groups:
     *   [ 0. ] no freeze       [ 0. ] row freeze      [ 01 ] col freeze      [ 01 ] row+col freeze
     *   [ .. ]  or no scroll   [ 2. ]  and v-scroll   [ .. ]  and h-scroll   [ 23 ]  and v+h-scroll
     *
     * Each column itself can use 1 channel (row freeze disabled) or 2 channels (row freeze enabled).
     * When the contents of a column didn't stray off its limit, we move its channels into the corresponding group
     * based on its position (within frozen rows/columns groups or not).
     * At the end of the operation our 1-4 groups will each have a ImDrawCmd using the same ClipRect.
     * This function assume that each column are pointing to a distinct draw channel,
     * otherwise merge_group->ChannelsCount will not match set bit count of merge_group->ChannelsMask.
     *
     * Column channels will not be merged into one of the 1-4 groups in the following cases:
     * - The contents stray off its clipping rectangle (we only compare the MaxX value, not the MinX value).
     *   Direct ImDrawList calls won't be taken into account by default, if you use them make sure the ImGui:: bounds
     *   matches, by e.g. calling SetCursorScreenPos().
     * - The channel uses more than one draw command itself. We drop all our attempt at merging stuff here..
     *   we could do better but it's going to be rare and probably not worth the hassle.
     * Columns for which the draw channel(s) haven't been merged with other will use their own ImDrawCmd.
     *
     * This function is particularly tricky to understand.. take a breath.
     *
     * ~TableMergeDrawChannels */
    fun mergeDrawChannels() {

        val splitter = drawSplitter
        val hasFreezeV = freezeRowsCount > 0
        val hasFreezeH = freezeColumnsCount > 0

        // Track which groups we are going to attempt to merge, and which channels goes into each group.
        class MergeGroup {
            val clipRect = Rect()
            var channelsCount = 0
            val channelsMask = BitArray(TABLE_MAX_DRAW_CHANNELS)
        }

        var mergeGroupMask = 0x00
        val mergeGroups = Array(4) { MergeGroup() }

        // 1. Scan channels and take note of those which can be merged
        for (columnN in 0 until columnsCount) {
            if (visibleMaskByIndex hasnt (1L shl columnN))
                continue
            val column = columns[columnN]

            val mergeGroupSubCount = if (hasFreezeV) 2 else 1
            for (mergeGroupSubN in 0 until mergeGroupSubCount) {
                val channelNo = if (mergeGroupSubN == 0) column.drawChannelFrozen else column.drawChannelUnfrozen

                // Don't attempt to merge if there are multiple draw calls within the column
                val srcChannel = splitter._channels[channelNo]
                if (srcChannel._cmdBuffer.size > 0 && srcChannel._cmdBuffer.last().elemCount == 0)
                    srcChannel._cmdBuffer.pop()
                if (srcChannel._cmdBuffer.size != 1)
                    continue

                // Find out the width of this merge group and check if it will fit in our column
                // (note that we assume that rendering didn't stray on the left direction. we should need a CursorMinPos to detect it)
                if (column.flags hasnt Tcf.NoClip) {
                    val contentMaxX = when {
                        !hasFreezeV -> column.contentMaxXUnfrozen max column.contentMaxXHeadersUsed // No row freeze
                        mergeGroupSubN == 0 -> column.contentMaxXFrozen max column.contentMaxXHeadersUsed   // Row freeze: use width before freeze
                        else -> column.contentMaxXUnfrozen                                        // Row freeze: use width after freeze
                    }
                    if (contentMaxX > column.clipRect.max.x)
                        continue
                }

                val mergeGroupN = (if (hasFreezeH && columnN < freezeColumnsCount) 0 else 1) + if (hasFreezeV && mergeGroupSubN == 0) 0 else 2
                assert(channelNo < TABLE_MAX_DRAW_CHANNELS)
                val mergeGroup = mergeGroups[mergeGroupN]
                if (mergeGroup.channelsCount == 0)
                    mergeGroup.clipRect.put(+Float.MAX_VALUE, +Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
                mergeGroup.channelsMask setBit channelNo
                mergeGroup.channelsCount++
                mergeGroup.clipRect add srcChannel._cmdBuffer[0].clipRect
                mergeGroupMask = mergeGroupMask or (1 shl mergeGroupN)
            }

            // Invalidate current draw channel
            // (we don't clear DrawChannelFrozen/DrawChannelUnfrozen solely to facilitate debugging/later inspection of data)
            column.drawChannelCurrent = -1
        }

        // [DEBUG] Display merge groups
//        #if 0
//        if (g.IO.KeyShift)
//            for (int merge_group_n = 0; merge_group_n < IM_ARRAYSIZE(mergeGroups); merge_group_n++)
//        {
//            MergeGroup* merge_group = &merge_groups[merge_group_n]
//            if (merge_group->ChannelsCount == 0)
//            continue
//            char buf[32]
//            ImFormatString(buf, 32, "MG%d:%d", merge_group_n, merge_group->ChannelsCount)
//            ImVec2 text_pos = merge_group->ClipRect.Min + ImVec2(4, 4)
//            ImVec2 text_size = CalcTextSize(buf, NULL)
//            GetForegroundDrawList()->AddRectFilled(text_pos, text_pos + text_size, IM_COL32(0, 0, 0, 255))
//            GetForegroundDrawList()->AddText(text_pos, IM_COL32(255, 255, 0, 255), buf, NULL)
//            GetForegroundDrawList()->AddRect(merge_group->ClipRect.Min, merge_group->ClipRect.Max, IM_COL32(255, 255, 0, 255))
//        }
//        #endif

        // 2. Rewrite channel list in our preferred order
        if (mergeGroupMask != 0) {
            // We skip channel 0 (Bg0) and 1 (Bg1 frozen) from the shuffling since they won't move - see channels allocation in TableSetupDrawChannels().
            val LEADING_DRAW_CHANNELS = 2
//            g.drawChannelsTempMergeBuffer.resize(splitter->_Count - LEADING_DRAW_CHANNELS) // Use shared temporary storage so the allocation gets amortized
            if (g.drawChannelsTempMergeBuffer.size < splitter._count - LEADING_DRAW_CHANNELS)
                for (i in g.drawChannelsTempMergeBuffer.size until (splitter._count - LEADING_DRAW_CHANNELS))
                    g.drawChannelsTempMergeBuffer += DrawChannel()
            var dstTmp = 0 //g.drawChannelsTempMergeBuffer.Data

            fun memcpy(src: DrawChannel) {
                val dst = g.drawChannelsTempMergeBuffer[dstTmp++]
                for (cmd in src._cmdBuffer)
                    dst._cmdBuffer += DrawCmd(cmd)
                dst._idxBuffer = IntBuffer(src._idxBuffer)
            }

            val remainingMask = BitArray(TABLE_MAX_DRAW_CHANNELS)                       // We need 132-bit of storage
            remainingMask.clearBits()
            remainingMask.setBitRange(LEADING_DRAW_CHANNELS, splitter._count - 1)
            remainingMask.clearBit(bg1DrawChannelUnfrozen)
            assert(!hasFreezeV || bg1DrawChannelUnfrozen != TABLE_DRAW_CHANNEL_BG1_FROZEN)
            var remainingCount = splitter._count - if (hasFreezeV) LEADING_DRAW_CHANNELS + 1 else LEADING_DRAW_CHANNELS
            //ImRect host_rect = (table->InnerWindow == table->OuterWindow) ? table->InnerClipRect : table->HostClipRect;
            val hostRect = hostClipRect // [JVM] careful, same instance!
            for (mergeGroupN in mergeGroups.indices) {
                var mergeChannelsCount = mergeGroups[mergeGroupN].channelsCount
                if (mergeChannelsCount != 0) {
                    val mergeGroup = mergeGroups[mergeGroupN]
                    val mergeClipRect = Rect(mergeGroup.clipRect)

                    // Extend outer-most clip limits to match those of host, so draw calls can be merged even if
                    // outer-most columns have some outer padding offsetting them from their parent ClipRect.
                    // The principal cases this is dealing with are:
                    // - On a same-window table (not scrolling = single group), all fitting columns ClipRect -> will extend and match host ClipRect -> will merge
                    // - Columns can use padding and have left-most ClipRect.Min.x and right-most ClipRect.Max.x != from host ClipRect -> will extend and match host ClipRect -> will merge
                    // FIXME-TABLE FIXME-WORKRECT: We are wasting a merge opportunity on tables without scrolling if column doesn't fit
                    // within host clip rect, solely because of the half-padding difference between window->WorkRect and window->InnerClipRect.
                    if ((mergeGroupN and 1) == 0 || !hasFreezeH)
                        mergeClipRect.min.x = mergeClipRect.min.x min hostRect.min.x
                    if ((mergeGroupN and 2) == 0 || !hasFreezeV)
                        mergeClipRect.min.y = mergeClipRect.min.y min hostRect.min.y
                    if (mergeGroupN has 1)
                        mergeClipRect.max.x = mergeClipRect.max.x max hostRect.max.x
                    if (mergeGroupN has 2 && flags hasnt Tf.NoHostExtendY)
                        mergeClipRect.max.y = mergeClipRect.max.y max hostRect.max.y
//                    #if 0
//                    GetOverlayDrawList()->AddRect(merge_group->ClipRect.Min, merge_group->ClipRect.Max, IM_COL32(255, 0, 0, 200), 0.0f, ~0, 1.0f)
//                    GetOverlayDrawList()->AddLine(merge_group->ClipRect.Min, merge_clip_rect.Min, IM_COL32(255, 100, 0, 200))
//                    GetOverlayDrawList()->AddLine(merge_group->ClipRect.Max, merge_clip_rect.Max, IM_COL32(255, 100, 0, 200))
//                    #endif
                    remainingCount -= mergeGroup.channelsCount
                    for (n in remainingMask.storage.indices)
                        remainingMask.storage[n] = remainingMask.storage[n] wo mergeGroup.channelsMask.storage[n]
                    var n = 0
                    while (n < splitter._count && mergeChannelsCount != 0) {
                        // Copy + overwrite new clip rect
                        if (!mergeGroup.channelsMask.testBit(n)) {
                            n++
                            continue
                        }
                        mergeGroup.channelsMask clearBit n
                        mergeChannelsCount--

                        val channel = splitter._channels[n]
                        assert(channel._cmdBuffer.size == 1 && mergeClipRect.contains(Rect(channel._cmdBuffer[0].clipRect)))
                        channel._cmdBuffer[0].clipRect = mergeClipRect.toVec4()
//                        memcpy(dstTmp++, channel, sizeof(ImDrawChannel))
                        memcpy(channel)
                        n++
                    }
                }

                // Make sure Bg1DrawChannelUnfrozen appears in the middle of our groups (whereas Bg0 and Bg1 frozen are fixed to 0 and 1)
                if (mergeGroupN == 1 && hasFreezeV)
//                    memcpy(dstTmp++, &splitter->_Channels[table->Bg1DrawChannelUnfrozen], sizeof(ImDrawChannel))
                    memcpy(splitter._channels[bg1DrawChannelUnfrozen])
            }

            // Append unmergeable channels that we didn't reorder at the end of the list
            var n = 0
            while (n < splitter._count && remainingCount != 0) {
                if (!remainingMask.testBit(n)) {
                    n++
                    continue
                }
                val channel = splitter._channels[n]
                memcpy(channel)
                remainingCount--
                n++
            }
            assert(dstTmp == /*g.drawChannelsTempMergeBuffer.Data +*/ g.drawChannelsTempMergeBuffer.size)
//            memcpy(splitter._channels.Data + LEADING_DRAW_CHANNELS, g.DrawChannelsTempMergeBuffer.Data, (splitter->_Count - LEADING_DRAW_CHANNELS) * sizeof(ImDrawChannel))
            for (chIdx in 0 until splitter._count - LEADING_DRAW_CHANNELS) {
                val dst = splitter._channels[LEADING_DRAW_CHANNELS + chIdx]
                for (channel in g.drawChannelsTempMergeBuffer) {
                    for (cmd in channel._cmdBuffer)
                        dst._cmdBuffer += DrawCmd(cmd)
                    dst._idxBuffer = IntBuffer(channel._idxBuffer)
                }
            }
        }
    }

    /** ~TableSortSpecsSanitize */
    fun sortSpecsSanitize() {

        assert(flags has Tf.Sortable)

        // Clear SortOrder from hidden column and verify that there's no gap or duplicate.
        var sortOrderCount = 0
        var sortOrderMask = 0x00L
        for (columnN in 0 until columnsCount) {
            val column = columns[columnN]
            if (column.sortOrder != -1 && !column.isEnabled)
                column.sortOrder = -1
            if (column.sortOrder == -1)
                continue
            sortOrderCount++
            sortOrderMask = sortOrderMask or (1L shl column.sortOrder)
            assert(sortOrderCount < Int.BYTES * 8)
        }

        val needFixLinearize = (1L shl sortOrderCount) != sortOrderMask + 1
        val needFixSingleSortOrder = sortOrderCount > 1 && flags hasnt Tf.SortMulti
        if (needFixLinearize || needFixSingleSortOrder) {
            var fixedMask = 0x00L
            for (sortN in 0 until sortOrderCount) {
                // Fix: Rewrite sort order fields if needed so they have no gap or duplicate.
                // (e.g. SortOrder 0 disappeared, SortOrder 1..2 exists --> rewrite then as SortOrder 0..1)
                var columnWithSmallestSortOrder = -1
                for (columnN in 0 until columnsCount)
                    if (fixedMask hasnt (1L shl columnN) && columns[columnN].sortOrder != -1)
                        if (columnWithSmallestSortOrder == -1 || columns[columnN].sortOrder < columns[columnWithSmallestSortOrder].sortOrder)
                            columnWithSmallestSortOrder = columnN
                assert(columnWithSmallestSortOrder != -1)
                fixedMask = fixedMask or (1L shl columnWithSmallestSortOrder)
                columns[columnWithSmallestSortOrder].sortOrder = sortN

                // Fix: Make sure only one column has a SortOrder if ImGuiTableFlags_MultiSortable is not set.
                if (needFixSingleSortOrder) {
                    sortOrderCount = 1
                    for (columnN in 0 until columnsCount)
                        if (columnN != columnWithSmallestSortOrder)
                            columns[columnN].sortOrder = -1
                    break
                }
            }
        }

        // Fallback default sort order (if no column had the ImGuiTableColumnFlags_DefaultSort flag)
        if (sortOrderCount == 0 && flags hasnt Tf.SortTristate)
            for (columnN in 0 until columnsCount) {
                val column = columns[columnN]
                if (column.isEnabled && flags hasnt Tcf.NoSort) {
                    sortOrderCount = 1
                    column.sortOrder = 0
                    column.sortDirection = tableGetColumnAvailSortDirection(column, 0)
                    break
                }
            }

        sortSpecsCount = sortOrderCount
    }

    /** ~TableSortSpecsBuild */
    fun sortSpecsBuild() {

        assert(isSortSpecsDirty)
        sortSpecsSanitize()

        // Write output
        if (sortSpecsCount <= 1)
            sortSpecsMulti.clear()
        else
            repeat(sortSpecsCount) {
                sortSpecsMulti += TableColumnSortSpecs()
            }
        var sortSpecs: TableColumnSortSpecs? = null
        for (columnN in 0 until columnsCount) {
            val column = columns[columnN]
            if (column.sortOrder == -1)
                continue
            assert(column.sortOrder < sortSpecsCount)
            sortSpecs = when (sortSpecsCount) {
                0 -> null
                1 -> sortSpecsSingle
                else -> sortSpecsMulti[column.sortOrder]
            }!!
            sortSpecs.columnUserID = column.userID
            sortSpecs.columnIndex = columnN
            sortSpecs.sortOrder = column.sortOrder
            sortSpecs.sortDirection = column.sortDirection
        }
        this.sortSpecs.specs = sortSpecs
        this.sortSpecs.specsCount = sortSpecsCount
        this.sortSpecs.specsDirty = true // Mark as dirty for user
        isSortSpecsDirty = false // Mark as not dirty for us
    }

    /** Fix sort direction if currently set on a value which is unavailable (e.g. activating NoSortAscending/NoSortDescending)
     *
     *  ~tableFixColumnSortDirection */
    infix fun fixColumnSortDirection(column: TableColumn) {
        if (column.sortOrder == -1 || column.sortDirectionsAvailMask has (1 shl column.sortDirection.i))
            return
        column.sortDirection = tableGetColumnAvailSortDirection(column, 0)
        isSortSpecsDirty = true
    }

    /** [Internal] Called by TableNextRow()
     *  ~TableBeginRow */
    fun beginRow() {

        val window = innerWindow!!
        assert(!isInsideRow)

        // New row
        currentRow++
        currentColumn = -1
        rowBgColor.fill(COL32_DISABLE)
        rowCellDataCurrent = -1
        isInsideRow = true

        // Begin frozen rows
        var nextY1 = rowPosY2
        if (currentRow == 0 && freezeRowsCount > 0) {
            nextY1 = outerRect.min.y
            window.dc.cursorPos.y = nextY1
        }

        rowPosY1 = nextY1
        rowPosY2 = nextY1
        rowTextBaseline = 0f
        rowIndentOffsetX = window.dc.indent - hostIndentX // Lock indent
        window.dc.prevLineTextBaseOffset = 0f
        window.dc.cursorMaxPos.y = nextY1

        // Making the header BG color non-transparent will allow us to overlay it multiple times when handling smooth dragging.
        if (rowFlags has Trf.Headers) {
            tableSetBgColor(TableBgTarget.RowBg0, Col.TableHeaderBg.u32)
            if (currentRow == 0)
                isUsingHeaders = true
        }
    }

    /** [Internal] Called by TableNextRow()
     *  ~TableEndRow */
    fun endRow() {
        val window = g.currentWindow!!
        assert(window === innerWindow)
        assert(isInsideRow)

        if (currentColumn != -1)
            endCell()

        // Position cursor at the bottom of our row so it can be used for e.g. clipping calculation. However it is
        // likely that the next call to TableBeginCell() will reposition the cursor to take account of vertical padding.
        window.dc.cursorPos.y = rowPosY2

        // Row background fill
        val bgY1 = rowPosY1
        val bgY2 = rowPosY2

        val unfreezeRowsActual = currentRow + 1 == freezeRowsCount
        val unfreezeRowsRequest = currentRow + 1 == freezeRowsRequest
        if (currentRow == 0)
            lastFirstRowHeight = bgY2 - bgY1

        val isVisible = bgY2 >= innerClipRect.min.y && bgY1 <= innerClipRect.max.y
        if (isVisible) {
            // Decide of background color for the row
            var bgCol0 = 0
            var bgCol1 = 0
            if (rowBgColor[0] != COL32_DISABLE)
                bgCol0 = rowBgColor[0]
            else if (flags has Tf.RowBg)
                bgCol0 = (if (rowBgColorCounter has 1) Col.TableRowBgAlt else Col.TableRowBg).u32
            if (rowBgColor[1] != COL32_DISABLE)
                bgCol1 = rowBgColor[1]

            // Decide of top border color
            var borderCol = 0
            val borderSize = TABLE_BORDER_SIZE
            if (currentRow > 0 || innerWindow === outerWindow)
                if (flags has Tf.BordersInnerH)
                    borderCol = if (lastRowFlags has Trf.Headers) borderColorStrong else borderColorLight

            val drawCellBgColor = rowCellDataCurrent >= 0
            val drawStrongBottomBorder = unfreezeRowsActual
            if ((bgCol0 or bgCol1 or borderCol) != 0 || drawStrongBottomBorder || drawCellBgColor) {
                // In theory we could call SetWindowClipRectBeforeSetChannel() but since we know TableEndRow() is
                // always followed by a change of clipping rectangle we perform the smallest overwrite possible here.
                if (flags hasnt Tf.NoClip)
                    window.drawList._cmdHeader.clipRect put bgClipRectForDrawCmd.toVec4()
                drawSplitter.setCurrentChannel(window.drawList, TABLE_DRAW_CHANNEL_BG0)
            }

            // Draw row background
            // We soft/cpu clip this so all backgrounds and borders can share the same clipping rectangle
            if (bgCol0 != 0 || bgCol1 != 0) {
                val rowRect = Rect(workRect.min.x, bgY1, workRect.max.x, bgY2)
                rowRect clipWith bgClipRect
                if (bgCol0 != 0 && rowRect.min.y < rowRect.max.y)
                    window.drawList.addRectFilled(rowRect.min, rowRect.max, bgCol0)
                if (bgCol1 != 0 && rowRect.min.y < rowRect.max.y)
                    window.drawList.addRectFilled(rowRect.min, rowRect.max, bgCol1)
            }

            // Draw cell background color
            if (drawCellBgColor) {
                val cellDataEnd = rowCellDataCurrent
                var idx = 0
                while (idx <= cellDataEnd) {
                    val cellData = rowCellData[idx++]
                    val column = columns[cellData.column]
                    val cellBgRect = getCellBgRect(cellData.column)
                    cellBgRect.clipWith(bgClipRect)
                    cellBgRect.min.x = cellBgRect.min.x max column.clipRect.min.x     // So that first column after frozen one gets clipped
                    cellBgRect.max.x = cellBgRect.max.x min column.maxX
                    window.drawList.addRectFilled(cellBgRect.min, cellBgRect.max, cellData.bgColor)
                }
            }

            // Draw top border
            if (borderCol != 0 && bgY1 >= bgClipRect.min.y && bgY1 < bgClipRect.max.y)
                window.drawList.addLine(Vec2(borderX1, bgY1), Vec2(borderX2, bgY1), borderCol, borderSize)

            // Draw bottom border at the row unfreezing mark (always strong)
            if (drawStrongBottomBorder && bgY2 >= bgClipRect.min.y && bgY2 < bgClipRect.max.y)
                window.drawList.addLine(Vec2(borderX1, bgY2), Vec2(borderX2, bgY2), borderColorStrong, borderSize)
        }

        // End frozen rows (when we are past the last frozen row line, teleport cursor and alter clipping rectangle)
        // We need to do that in TableEndRow() instead of TableBeginRow() so the list clipper can mark end of row and
        // get the new cursor position.
        if (unfreezeRowsRequest)
            for (columnN in 0 until columnsCount) {
                val column = columns[columnN]
                column.navLayerCurrent = if (columnN < freezeColumnsCount) NavLayer.Menu else NavLayer.Main
            }
        if (unfreezeRowsActual) {
            assert(!isUnfrozen)
            isUnfrozen = true

            // BgClipRect starts as table->InnerClipRect, reduce it now and make BgClipRectForDrawCmd == BgClipRect
            val y0 = (rowPosY2 + 1) max window.innerClipRect.min.y
            bgClipRect.min.y = y0 min window.innerClipRect.max.y
            bgClipRectForDrawCmd.min.y = bgClipRect.min.y
            bgClipRect.max.y = window.innerClipRect.max.y
            bgClipRectForDrawCmd.max.y = bgClipRect.max.y
            bg1DrawChannelCurrent = bg1DrawChannelUnfrozen
            assert(bgClipRectForDrawCmd.min.y <= bgClipRectForDrawCmd.max.y)

            val rowHeight = rowPosY2 - rowPosY1
            rowPosY2 = workRect.min.y + rowPosY2 - outerRect.min.y
            window.dc.cursorPos.y = rowPosY2
            rowPosY1 = rowPosY2 - rowHeight
            for (columnN in 0 until columnsCount) {
                val column = columns[columnN]
                column.drawChannelCurrent = column.drawChannelUnfrozen
                column.clipRect.min.y = bgClipRectForDrawCmd.min.y
            }

            // Update cliprect ahead of TableBeginCell() so clipper can access to new ClipRect->Min.y
            setWindowClipRectBeforeSetChannel(window, columns[0].clipRect)
            drawSplitter.setCurrentChannel(window.drawList, columns[0].drawChannelCurrent)
        }

        if (rowFlags hasnt Trf.Headers)
            rowBgColorCounter++
        isInsideRow = false
    }

    /** [Internal] Called by TableSetColumnIndex()/TableNextColumn()
     *  This is called very frequently, so we need to be mindful of unnecessary overhead.
     *  FIXME-TABLE FIXME-OPT: Could probably shortcut some things for non-active or clipped columns.
     *  ~TableBeginCell */
    infix fun beginCell(columnN: Int) {

        val column = columns[columnN]
        val window = innerWindow!!
        currentColumn = columnN

        // Start position is roughly ~~ CellRect.Min + CellPadding + Indent
        var startX = column.workMinX
        if (column.flags has Tcf.IndentEnable)
            startX += rowIndentOffsetX // ~~ += window.DC.Indent.x - table->HostIndentX, except we locked it for the row.

        window.dc.cursorPos.x = startX
        window.dc.cursorPos.y = rowPosY1 + cellPaddingY
        window.dc.cursorMaxPos.x = window.dc.cursorPos.x
        window.dc.columnsOffset = startX - window.pos.x - window.dc.indent // FIXME-WORKRECT
        window.dc.currLineTextBaseOffset = rowTextBaseline
        window.dc.navLayerCurrent = column.navLayerCurrent

        window.workRect.min.y = window.dc.cursorPos.y
        window.workRect.min.x = column.workMinX
        window.workRect.max.x = column.workMaxX
        window.dc.itemWidth = column.itemWidth

        // To allow ImGuiListClipper to function we propagate our row height
        if (!column.isEnabled)
            window.dc.cursorPos.y = window.dc.cursorPos.y max rowPosY2

        window.skipItems = column.isSkipItems
        if (column.isSkipItems) {
            window.dc.lastItemId = 0
            window.dc.lastItemStatusFlags = 0
        }

        if (flags has Tf.NoClip) {
            // FIXME: if we end up drawing all borders/bg in EndTable, could remove this and just assert that channel hasn't changed.
            drawSplitter.setCurrentChannel(window.drawList, TABLE_DRAW_CHANNEL_NOCLIP)
            //IM_ASSERT(table->DrawSplitter._Current == TABLE_DRAW_CHANNEL_NOCLIP);
        } else {
            // FIXME-TABLE: Could avoid this if draw channel is dummy channel?
            setWindowClipRectBeforeSetChannel(window, column.clipRect)
            drawSplitter.setCurrentChannel(window.drawList, column.drawChannelCurrent)
        }
    }

    /** [Internal] Called by TableNextRow()/TableSetColumnIndex()/TableNextColumn()
     *  ~TableEndCell */
    fun endCell() {

        val column = columns[currentColumn]
        val window = innerWindow!!

        // Report maximum position so we can infer content size per column.
        val maxPosX = when {
            rowFlags has Trf.Headers -> column::contentMaxXHeadersUsed  // Useful in case user submit contents in header row that is not a TableHeader() call
            else -> if (isUnfrozen) column::contentMaxXUnfrozen else column::contentMaxXFrozen
        }
        maxPosX.set(maxPosX() max window.dc.cursorMaxPos.x)
        rowPosY2 = rowPosY2 max (window.dc.cursorMaxPos.y + cellPaddingY)
        column.itemWidth = window.dc.itemWidth

        // Propagate text baseline for the entire row
        // FIXME-TABLE: Here we propagate text baseline from the last line of the cell.. instead of the first one.
        rowTextBaseline = rowTextBaseline max window.dc.prevLineTextBaseOffset
    }

    /** Return the cell rectangle based on currently known height.
     *  - Important: we generally don't know our row height until the end of the row, so Max.y will be incorrect in many situations.
     *    The only case where this is correct is if we provided a min_row_height to TableNextRow() and don't go below it.
     *  - Important: if ImGuiTableFlags_PadOuterX is set but ImGuiTableFlags_PadInnerX is not set, the outer-most left and right
     *    columns report a small offset so their CellBgRect can extend up to the outer border.
     *
     *  ~TableGetCellBgRect */
    infix fun getCellBgRect(columnN: Int): Rect {
        val column = columns[columnN]
        var x1 = column.minX
        var x2 = column.maxX
        if (column.prevEnabledColumn == -1)
            x1 -= cellSpacingX1
        if (column.nextEnabledColumn == -1)
            x2 += cellSpacingX2
        return Rect(x1, rowPosY1, x2, rowPosY2)
    }

    /** ~TableGetColumnName */
    infix fun getColumnName(columnN: Int): String = when {
        !isLayoutLocked && columnN >= declColumnsCount -> "" // NameOffset is invalid at this point
        else -> columnsNames.getOrElse(columns[columnN].nameOffset) { "" }
    }

    /** Return the resizing ID for the right-side of the given column.
     *  ~TableGetColumnResizeID */
    fun getColumnResizeID(columnN: Int, instanceNo: Int = 0): ID {
        assert(columnN < columnsCount)
        return id + 1 + instanceNo * columnsCount + columnN
    }

    /** Disable clipping then auto-fit, will take 2 frames
     *  (we don't take a shortcut for unclipped columns to reduce inconsistencies when e.g. resizing multiple columns)
     *  ~TableSetColumnWidthAutoSingle */
    infix fun setColumnWidthAutoSingle(columnN: Int) {
        // Single auto width uses auto-fit
        val column = columns[columnN]
        if (!column.isEnabled)
            return
        column.cannotSkipItemsQueue = 1 shl 0
        if (column.flags has Tcf.WidthStretch)
            autoFitSingleStretchColumn = columnN
        else
            column.autoFitQueue = 1 shl 1
    }

    /** ~TableSetColumnWidthAutoAll */
    fun setColumnWidthAutoAll() {
        for (columnN in 0 until columnsCount) {
            val column = columns[columnN]
            if (!column.isEnabled && column.flags hasnt Tcf.WidthStretch) // Can reset weight of hidden stretch column
                continue
            column.cannotSkipItemsQueue = 1 shl 0
            column.autoFitQueue = 1 shl 1
        }
    }

    /** Remove Table (currently only used by TestEngine)
     *  ~TableRemove */
    fun remove() {
        //IMGUI_DEBUG_LOG("TableRemove() id=0x%08X\n", table->ID);
        val tableIdx = g.tables.getIndex(this)
        //memset(table->RawData.Data, 0, table->RawData.size_in_bytes());
        //memset(table, 0, sizeof(ImGuiTable));
        g.tables.remove(id, this)
        g.tablesLastTimeActive[tableIdx.i] = -1f
    }

    /** Free up/compact internal Table buffers for when it gets unused
     *  ~TableGcCompactTransientBuffers */
    fun gcCompactTransientBuffers() {
        //IMGUI_DEBUG_LOG("TableGcCompactTransientBuffers() id=0x%08X\n", table->ID);
        assert(!memoryCompacted)
        drawSplitter.clearFreeMemory()
        sortSpecsMulti.clear()
        sortSpecs.specs = null
        isSortSpecsDirty = true
        columnsNames.clear()
        memoryCompacted = true
        for (n in 0 until columnsCount)
            columns[n].nameOffset = -1
        g.tablesLastTimeActive[g.tables.getIndex(this).i] = -1f
    }

    /** ~static void TableUpdateColumnsWeightFromWidth(ImGuiTable* table) */
    fun updateColumnsWeightFromWidth() {

        assert(leftMostStretchedColumn != -1 && rightMostStretchedColumn != -1)

        // Measure existing quantity
        var visibleWeight = 0f
        var visibleWidth = 0f
        for (columnN in 0 until columnsCount) {
            val column = columns[columnN]
            if (!column.isEnabled || column.flags hasnt Tcf.WidthStretch)
                continue
            assert(column.stretchWeight > 0f)
            visibleWeight += column.stretchWeight
            visibleWidth += column.widthRequest
        }
        assert(visibleWeight > 0f && visibleWidth > 0f)

        // Apply new weights
        for (columnN in 0 until columnsCount) {
            val column = columns[columnN]
            if (!column.isEnabled || column.flags hasnt Tcf.WidthStretch)
                continue
            column.stretchWeight = (column.widthRequest / visibleWidth) * visibleWeight
            assert(column.stretchWeight > 0f)
        }
    }

    /** ~TableLoadSettings */
    fun loadSettings() {
        isSettingsRequestLoad = false
        if (flags has Tf.NoSavedSettings)
            return

        // Bind settings
        val settings: TableSettings
        if (settingsOffset == -1) {
            settings = tableSettingsFindByID(id) ?: return
            if (settings.columnsCount != columnsCount) // Allow settings if columns count changed. We could otherwise decide to return...
                isSettingsDirty = true
            settingsOffset = g.settingsTables.indexOf(settings)
        } else
            settings = getBoundSettings()!!

        settingsLoadedFlags = settings.saveFlags
        refScale = settings.refScale

        // Serialize ImGuiTableSettings/ImGuiTableColumnSettings into ImGuiTable/ImGuiTableColumn
        var displayOrderMask = 0L
        for (dataN in 0 until settings.columnsCount) {
            val columnSettings = settings.columnSettings[dataN]
            val columnN = columnSettings.index
            if (columnN < 0 || columnN >= columnsCount)
                continue

            val column = columns[columnN]
            if (settings.saveFlags has Tf.Resizable) {
                if (columnSettings.isStretch)
                    column.stretchWeight = columnSettings.widthOrWeight
                else
                    column.widthRequest = columnSettings.widthOrWeight
                column.autoFitQueue = 0x00
            }
            column.displayOrder = when {
                settings.saveFlags has Tf.Reorderable -> columnSettings.displayOrder
                else -> columnN
            }
            displayOrderMask = displayOrderMask or (1L shl column.displayOrder)
            column.isEnabled = columnSettings.isEnabled
            column.isEnabledNextFrame = columnSettings.isEnabled
            column.sortOrder = columnSettings.sortOrder
            column.sortDirection = columnSettings.sortDirection
        }

        // Validate and fix invalid display order data
        val expectedDisplayOrderMask = when (settings.columnsCount) {
            64 -> 0.inv()
            else -> (1L shl settings.columnsCount) - 1
        }
        if (displayOrderMask != expectedDisplayOrderMask)
            for (columnN in 0 until columnsCount)
                columns[columnN].displayOrder = columnN

        // Rebuild index
        for (columnN in 0 until columnsCount)
            displayOrderToIndex[columns[columnN].displayOrder] = columnN
    }

    /** ~TableSaveSettings */
    fun saveSettings() {
        isSettingsDirty = false
        if (flags has Tf.NoSavedSettings)
            return

        // Bind or create settings data
        val settings = getBoundSettings() ?: TableSettings(id, columnsCount).also {
            settingsOffset = g.settingsTables.indexOf(it)
        }
        settings.columnsCount = columnsCount

        // Serialize ImGuiTable/ImGuiTableColumn into ImGuiTableSettings/ImGuiTableColumnSettings
        assert(settings.id == id)
        assert(settings.columnsCount == columnsCount && settings.columnsCountMax >= settings.columnsCount)

        var saveRefScale = false
        settings.saveFlags = Tf.None.i
        for (n in 0 until columnsCount) {
            val column = columns[n]
            val columnSettings = settings.columnSettings[n]
            val widthOrWeight = if (column.flags has Tcf.WidthStretch) column.stretchWeight else column.widthRequest
            columnSettings.widthOrWeight = widthOrWeight
            columnSettings.index = n
            columnSettings.displayOrder = column.displayOrder
            columnSettings.sortOrder = column.sortOrder
            columnSettings.sortDirection = column.sortDirection
            columnSettings.isEnabled = column.isEnabled
            columnSettings.isStretch = column.flags has Tcf.WidthStretch
            if (column.flags hasnt Tcf.WidthStretch)
                saveRefScale = true

            // We skip saving some data in the .ini file when they are unnecessary to restore our state.
            // Note that fixed width where initial width was derived from auto-fit will always be saved as InitStretchWeightOrWidth will be 0.0f.
            // FIXME-TABLE: We don't have logic to easily compare SortOrder to DefaultSortOrder yet so it's always saved when present.
            if (widthOrWeight != column.initStretchWeightOrWidth)
                settings.saveFlags = settings.saveFlags or Tf.Resizable
            if (column.displayOrder != n)
                settings.saveFlags = settings.saveFlags or Tf.Reorderable
            if (column.sortOrder != -1)
                settings.saveFlags = settings.saveFlags or Tf.Sortable
            if (column.isEnabled != column.flags hasnt Tcf.DefaultHide)
                settings.saveFlags = settings.saveFlags or Tf.Hideable
        }
        settings.saveFlags = settings.saveFlags and flags
        settings.refScale = if (saveRefScale) refScale else 0f

        markIniSettingsDirty()
    }

    /** Restore initial state of table (with or without saved settings)
     *  ~TableResetSettings */
    fun resetSettings() {
        isInitializing = true
        isSettingsDirty = true
        isResetAllRequest = false
        isSettingsRequestLoad = false                   // Don't reload from ini
        settingsLoadedFlags = Tf.None.i      // Mark as nothing loaded so our initialized data becomes authoritative
    }

    /** Get settings for a given table, NULL if none
     *  ~TableGetBoundSettings */
    fun getBoundSettings(): TableSettings? {
        if (settingsOffset != -1) {
            val settings = g.settingsTables[settingsOffset]
            assert(settings.id == id)
            if (settings.columnsCountMax >= columnsCount)
                return settings // OK
            settings.id = 0 // Invalidate storage, we won't fit because of a count change
        }
        return null
    }

    companion object {
        fun tableGetColumnAvailSortDirection(column: TableColumn, n: Int): SortDirection {
            assert(n < column.sortDirectionsAvailCount)
            return SortDirection of ((column.sortDirectionsAvailList ushr (n shl 1)) and 0x03)
        }
    }
}