@file:OptIn(ExperimentalStdlibApi::class)

package imgui.internal.api

import glm_.*
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.beginChildEx
import imgui.ImGui.calcItemSize
import imgui.ImGui.getIDWithSeed
import imgui.ImGui.isClippedEx
import imgui.ImGui.isDoubleClicked
import imgui.ImGui.itemSize
import imgui.ImGui.loadSettings
import imgui.ImGui.logRenderedText
import imgui.ImGui.menuItem
import imgui.ImGui.msg
import imgui.ImGui.pushOverrideID
import imgui.ImGui.resetSettings
import imgui.ImGui.setNextWindowContentSize
import imgui.ImGui.setNextWindowScroll
import imgui.api.g
import imgui.api.gImGui
import imgui.classes.TableColumnSortSpecs
import imgui.internal.*
import imgui.internal.classes.*
import imgui.internal.sections.ButtonFlag
import imgui.internal.sections.ItemFlag
import imgui.internal.sections.NavLayer
import imgui.static.*
import kool.BYTES
import kotlin.math.max
import kotlin.math.min
import imgui.TableFlag as Tf
import imgui.TableRowFlag as Trf
import imgui.WindowFlag as Wf

// Tables: Internals
interface tablesInternal {

    val currentTable: Table?
        get() = g.currentTable

    fun tableFindByID(id: ID): Table? = g.tables.getByKey(id)

    fun beginTableEx(name: String, id: ID, columnsCount: Int, flags_: TableFlags = none, outerSize: Vec2 = Vec2(), innerWidth: Float = 0f): Boolean {

        var flags = flags_
        val outerWindow = ImGui.currentWindow
        if (outerWindow.skipItems) // Consistent with other tables + beneficial side effect that assert on miscalling EndTable() will be more visible.
            return false

        // Sanity checks
        assert(columnsCount in 1..TABLE_MAX_COLUMNS) { "Only 1..64 columns allowed!" }
        if (flags has Tf.ScrollX)
            assert(innerWidth >= 0f)

        // If an outer size is specified ahead we will be able to early out when not visible. Exact clipping criteria may evolve.
        val useChildWindow = flags has (Tf.ScrollX or Tf.ScrollY)
        val availSize = ImGui.contentRegionAvail
        val actualOuterSize = calcItemSize(outerSize, availSize.x max 1f, if (useChildWindow) availSize.y max 1f else 0f)
        val outerRect = Rect(outerWindow.dc.cursorPos, outerWindow.dc.cursorPos + actualOuterSize)
        if (useChildWindow && isClippedEx(outerRect, 0)) {
            itemSize(outerRect)
            return false
        }

        // Acquire storage for the table
        val table = g.tables.getOrAddByKey(id)
        val tableLastFlags = table.flags

        // Acquire temporary buffers
        val tableIdx = g.tables.getIndex(table).i
        if (++g.tablesTempDataStacked > g.tablesTempData.size)
            for (i in g.tablesTempData.size until g.tablesTempDataStacked)
                g.tablesTempData += TableTempData()
        val tempData = g.tablesTempData[g.tablesTempDataStacked - 1]; table.tempData = tempData
        tempData.tableIndex = tableIdx
        table.drawSplitter = table.tempData!!.drawSplitter
        table.drawSplitter.clear()

        // Fix flags
        table.isDefaultSizingPolicy = flags hasnt Tf._SizingMask
        flags = tableFixFlags(flags, outerWindow)

        // Initialize
        val instanceNo = if (table.lastFrameActive != g.frameCount) 0 else table.instanceCurrent + 1
        table.id = id
        table.flags = flags
        table.lastFrameActive = g.frameCount
        table.outerWindow = outerWindow
        table.innerWindow = outerWindow
        table.columnsCount = columnsCount
        table.isLayoutLocked = false
        table.innerWidth = innerWidth
        tempData.userOuterSize put outerSize

        // Instance data (for instance 0, TableID == TableInstanceID)
        table.instanceCurrent = instanceNo
        val instanceId = when {
            instanceNo > 0 -> {
                assert(table.columnsCount == columnsCount) { "BeginTable(): Cannot change columns count mid-frame while preserving same ID" }
                if (table.instanceDataExtra.size < instanceNo)
                    table.instanceDataExtra += TableInstanceData()
                getIDWithSeed(instanceNo, getIDWithSeed("##Instances", -1, id)) // Push "##Instances" followed by (int)instance_no in ID stack.
            }

            else -> id
        }
        val tableInstance = table getInstanceData table.instanceCurrent
        tableInstance.tableInstanceID = instanceId

        // When not using a child window, WorkRect.Max will grow as we append contents.
        if (useChildWindow) {
            // Ensure no vertical scrollbar appears if we only want horizontal one, to make flag consistent
            // (we have no other way to disable vertical scrollbar of a window while keeping the horizontal one showing)
            val overrideContentSize = Vec2(Float.MAX_VALUE)
            if (flags has Tf.ScrollX && flags hasnt Tf.ScrollY)
                overrideContentSize.y = Float.MIN_VALUE

            // Ensure specified width (when not specified, Stretched columns will act as if the width == OuterWidth and
            // never lead to any scrolling). We don't handle inner_width < 0.0f, we could potentially use it to right-align
            // based on the right side of the child window work rect, which would require knowing ahead if we are going to
            // have decoration taking horizontal spaces (typically a vertical scrollbar).
            if (flags has Tf.ScrollX && innerWidth > 0f)
                overrideContentSize.x = innerWidth

            if (overrideContentSize.x != Float.MAX_VALUE || overrideContentSize.y != Float.MAX_VALUE) // TODO glm -> anyNotEqual
                setNextWindowContentSize(Vec2 { if (overrideContentSize[it] != Float.MAX_VALUE) overrideContentSize[it] else 0f })

            // Reset scroll if we are reactivating it
            if (tableLastFlags hasnt (Tf.ScrollX or Tf.ScrollY))
                setNextWindowScroll(Vec2())

            // Create scrolling region (without border and zero window padding)
            val childFlags = if (flags has Tf.ScrollX) Wf.HorizontalScrollbar else none
            beginChildEx(name, instanceId, outerRect.size, false, childFlags)
            table.innerWindow = g.currentWindow
            val inner = table.innerWindow!!
            table.workRect put inner.workRect
            table.outerRect put inner.rect()
            table.innerRect put table.innerWindow!!.innerRect
            assert(inner.windowPadding.x == 0f && inner.windowPadding.y == 0f && inner.windowBorderSize == 0f) // TODO glm -> allEqual

            // When using multiple instances, ensure they have the same amount of horizontal decorations (aka vertical scrollbar) so stretched columns can be aligned)
            if (instanceNo == 0) {
                table.hasScrollbarYPrev = table.hasScrollbarYCurr
                table.hasScrollbarYCurr = false
            }
            table.hasScrollbarYCurr /= table.innerWindow!!.scrollMax.y > 0f
        } else {
            // For non-scrolling tables, WorkRect == OuterRect == InnerRect.
            // But at this point we do NOT have a correct value for .Max.y (unless a height has been explicitly passed in). It will only be updated in EndTable().
            table.workRect put outerRect; table.outerRect put outerRect; table.innerRect put outerRect
        }

        // Push a standardized ID for both child-using and not-child-using tables
        pushOverrideID(id)
        if (instanceNo > 0)
            pushOverrideID(instanceId) // FIXME: Somehow this is not resolved by stack-tool, even tho GetIDWithSeed() submitted the symbol.

        // Backup a copy of host window members we will modify
        val innerWindow = table.innerWindow!!
        table.hostIndentX = innerWindow.dc.indent
        table.hostClipRect put innerWindow.clipRect
        table.hostSkipItems = innerWindow.skipItems
        tempData.hostBackupWorkRect put innerWindow.workRect
        tempData.hostBackupParentWorkRect put innerWindow.parentWorkRect
        tempData.hostBackupColumnsOffset = outerWindow.dc.columnsOffset
        tempData.hostBackupPrevLineSize put innerWindow.dc.prevLineSize
        tempData.hostBackupCurrLineSize put innerWindow.dc.currLineSize
        tempData.hostBackupCursorMaxPos put innerWindow.dc.cursorMaxPos
        tempData.hostBackupItemWidth = outerWindow.dc.itemWidth
        tempData.hostBackupItemWidthStackSize = outerWindow.dc.itemWidthStack.size
        innerWindow.dc.prevLineSize put 0f
        innerWindow.dc.currLineSize put 0f

        // Padding and Spacing
        // - None               ........Content..... Pad .....Content........
        // - PadOuter           | Pad ..Content..... Pad .....Content.. Pad |
        // - PadInner           ........Content.. Pad | Pad ..Content........
        // - PadOuter+PadInner  | Pad ..Content.. Pad | Pad ..Content.. Pad |
        val padOuterX = when {
            flags has Tf.NoPadOuterX -> false
            else -> when {
                flags has Tf.PadOuterX -> true
                else -> flags has Tf.BordersOuterV
            }
        }
        val padInnerX = flags hasnt Tf.NoPadInnerX
        val innerSpacingForBorder = if (flags has Tf.BordersInnerV) TABLE_BORDER_SIZE else 0f
        val innerSpacingExplicit = if (padInnerX && flags hasnt Tf.BordersInnerV) g.style.cellPadding.x else 0f
        val innerPaddingExplicit = if (padInnerX && flags has Tf.BordersInnerV) g.style.cellPadding.x else 0f
        table.cellSpacingX1 = innerSpacingExplicit + innerSpacingForBorder
        table.cellSpacingX2 = innerSpacingExplicit
        table.cellPaddingX = innerPaddingExplicit
        table.cellPaddingY = g.style.cellPadding.y

        val outerPaddingForBorder = if (flags has Tf.BordersOuterV) TABLE_BORDER_SIZE else 0f
        val outerPaddingExplicit = if (padOuterX) g.style.cellPadding.x else 0f
        table.outerPaddingX = (outerPaddingForBorder + outerPaddingExplicit) - table.cellPaddingX

        table.currentColumn = -1
        table.currentRow = -1
        table.rowBgColorCounter = 0
        table.lastRowFlags = none
        table.innerClipRect put if (innerWindow === outerWindow) table.workRect else innerWindow.clipRect
        table.innerClipRect clipWith table.workRect     // We need this to honor inner_width
        table.innerClipRect clipWithFull table.hostClipRect
        table.innerClipRect.max.y = if (flags has Tf.NoHostExtendY) table.innerClipRect.max.y min innerWindow.workRect.max.y else innerWindow.clipRect.max.y

        table.rowPosY1 = table.workRect.min.y // This is needed somehow
        table.rowPosY2 = table.rowPosY1 // "
        table.rowTextBaseline = 0f // This will be cleared again by TableBeginRow()
        table.freezeRowsRequest = 0 // This will be setup by TableSetupScrollFreeze(), if any
        table.freezeRowsCount = 0 // "
        table.freezeColumnsRequest = 0
        table.freezeColumnsCount = 0
        table.isUnfrozenRows = true
        table.declColumnsCount = 0

        // Using opaque colors facilitate overlapping lines of the grid, otherwise we'd need to improve TableDrawBorders()
        table.borderColorStrong = Col.TableBorderStrong.u32
        table.borderColorLight = Col.TableBorderLight.u32

        // Make table current
        g.currentTable = table
        outerWindow.dc.navIsScrollPushableX = false // Shortcut for NavUpdateCurrentWindowIsScrollPushableX();
        outerWindow.dc.currentTableIdx = tableIdx.i
        if (innerWindow !== outerWindow) // So EndChild() within the inner window can restore the table properly.
            innerWindow.dc.currentTableIdx = tableIdx.i

        if (tableLastFlags has Tf.Reorderable && flags hasnt Tf.Reorderable)
            table.isResetDisplayOrderRequest = true

        // Mark as used to avoid GC
        if (tableIdx >= g.tablesLastTimeActive.size)
            for (i in g.tablesLastTimeActive.size..tableIdx)
                g.tablesLastTimeActive += -1f
        g.tablesLastTimeActive[tableIdx.i] = g.time.f
        tempData.lastTimeActive = g.time.f
        table.memoryCompacted = false

        // Setup memory buffer (clear data if columns count changed)
        var oldColumnsToPreserve: ArrayList<TableColumn>? = null
        //        void* old_columns_raw_data = NULL
        val oldColumnsCount = table.columns.size
        if (oldColumnsCount != 0 && oldColumnsCount != columnsCount) {
            // Attempt to preserve width on column count change (#4046) TODO
            oldColumnsToPreserve = table.columns
            //            old_columns_raw_data = table->RawData
            table.rawData = false
        }
        if (!table.rawData) {
            table beginInitMemory columnsCount
            table.isInitializing = true
            table.isSettingsRequestLoad = true
        }
        if (table.isResetAllRequest)
            table.resetSettings()
        if (table.isInitializing) {
            // Initialize
            table.settingsOffset = -1
            table.isSortSpecsDirty = true
            table.instanceInteracted = -1
            table.contextPopupColumn = -1
            table.reorderColumn = -1
            table.resizedColumn = -1
            table.lastResizedColumn = -1
            table.autoFitSingleColumn = -1
            table.hoveredColumnBody = -1
            table.hoveredColumnBorder = -1
            for (n in 0 until columnsCount) {
                var column = table.columns[n]
                if (oldColumnsToPreserve != null && n < oldColumnsCount) {
                    // FIXME: We don't attempt to preserve column order in this path.
                    //                    *column = old_columns_to_preserve[n];
                } else {
                    val widthAuto = column.widthAuto
                    //                *column = ImGuiTableColumn()
                    table.columns[n] = TableColumn()
                    column = table.columns[n]
                    column.widthAuto = widthAuto
                    column.isPreserveWidthAuto = true // Preserve WidthAuto when reinitializing a live table: not technically necessary but remove a visible flicker
                    column.isEnabled = true; column.isUserEnabled = true; column.isUserEnabledNextFrame = true
                }
                column.displayOrder = n
                table.displayOrderToIndex[n] = n
                column.isEnabled = true
                column.isUserEnabledNextFrame = true
            }
        }
        //        if (old_columns_raw_data)
        //            IM_FREE(old_columns_raw_data);

        // Load settings
        if (table.isSettingsRequestLoad)
            table.loadSettings()

        // Handle DPI/font resize
        // This is designed to facilitate DPI changes with the assumption that e.g. style.CellPadding has been scaled as well.
        // It will also react to changing fonts with mixed results. It doesn't need to be perfect but merely provide a decent transition.
        // FIXME-DPI: Provide consistent standards for reference size. Perhaps using g.CurrentDpiScale would be more self explanatory.
        // This is will lead us to non-rounded WidthRequest in columns, which should work but is a poorly tested path.
        val newRefScaleUnit = g.fontSize // g.Font->GetCharAdvance('A') ?
        if (table.refScale != 0f && table.refScale != newRefScaleUnit) {
            val scaleFactor = newRefScaleUnit / table.refScale
            //IMGUI_DEBUG_PRINT("[table] %08X RefScaleUnit %.3f -> %.3f, scaling width by %.3f\n", table->ID, table->RefScaleUnit, new_ref_scale_unit, scale_factor);
            for (n in 0 until columnsCount)
                table.columns[n].widthRequest = table.columns[n].widthRequest * scaleFactor
        }
        table.refScale = newRefScaleUnit

        // Disable output until user calls TableNextRow() or TableNextColumn() leading to the TableUpdateLayout() call..
        // This is not strictly necessary but will reduce cases were "out of table" output will be misleading to the user.
        // Because we cannot safely assert in EndTable() when no rows have been created, this seems like our best option.
        innerWindow.skipItems = true

        // Clear names
        // At this point the ->NameOffset field of each column will be invalid until TableUpdateLayout() or the first call to TableSetupColumn()
        if (table.columnsNames.isNotEmpty())
            table.columnsNames.clear()

        // Apply queued resizing/reordering/hiding requests
        table.beginApplyRequests()

        return true
    }

    // For reference, the average total _allocation count_ for a table is:
    // + 0 (for ImGuiTable instance, we are pooling allocations in g.Tables[])
    // + 1 (for table->RawData allocated below)
    // + 1 (for table->ColumnsNames, if names are used)
    // Shared allocations for the maximum number of simultaneously nested tables (generally a very small number)
    // + 1 (for table->Splitter._Channels)
    // + 2 * active_channels_count (for ImDrawCmd and ImDrawIdx buffers inside channels)
    // Where active_channels_count is variable but often == columns_count or == columns_count + 1, see TableSetupDrawChannels() for details.
    // Unused channels don't perform their +2 allocations.
    // ~tableBeginInitMemory
    infix fun Table.beginInitMemory(columnsCount: Int) {
        // Allocate single buffer for our arrays
        repeat(columnsCount) { columns += TableColumn() }
        displayOrderToIndex = IntArray(columnsCount)
        rowCellData = Array(columnsCount) { TableCellData() }
        rawData = true
        enabledMaskByDisplayOrder = BitArray(columnsCount)
        enabledMaskByIndex = BitArray(columnsCount)
        visibleMaskByIndex = BitArray(columnsCount)
    }

    /** Apply queued resizing/reordering/hiding requests
     *  ~TableBeginApplyRequests */
    fun Table.beginApplyRequests() {
        // Handle resizing request
        // (We process this in the TableBegin() of the first instance of each table)
        // FIXME-TABLE: Contains columns if our work area doesn't allow for scrolling?
        if (instanceCurrent == 0) {
            if (resizedColumn != -1 && resizedColumnNextWidth != Float.MAX_VALUE)
                ImGui.tableSetColumnWidth(resizedColumn, resizedColumnNextWidth)
            lastResizedColumn = resizedColumn
            resizedColumnNextWidth = Float.MAX_VALUE
            resizedColumn = -1

            // Process auto-fit for single column, which is a special case for stretch columns and fixed columns with FixedSame policy.
            // FIXME-TABLE: Would be nice to redistribute available stretch space accordingly to other weights, instead of giving it all to siblings.
            if (autoFitSingleColumn != -1) {
                ImGui.tableSetColumnWidth(autoFitSingleColumn, columns[autoFitSingleColumn].widthAuto)
                autoFitSingleColumn = -1
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

                // Display order is stored in both columns->IndexDisplayOrder and table->DisplayOrder[]. Rebuild later from the former.
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
     *  - We allocate 1 or 2 background draw channels. This is because we know TablePushBackgroundChannel() is only used for
     *    horizontal spanning. If we allowed vertical spanning we'd need one background draw channel per merge group (1-4).
     *  Draw channel allocation (before merging):
     *  - NoClip                       --> 2+D+1 channels: bg0/1 + bg2 + foreground (same clip rect == always 1 draw call)
     *  - Clip                         --> 2+D+N channels
     *  - FreezeRows                   --> 2+D+N*2 (unless scrolling value is zero)
     *  - FreezeRows || FreezeColunns  --> 3+D+N*2 (unless scrolling value is zero)
     *  Where D is 1 if any column is clipped or hidden (dummy channel) otherwise 0.
     *  ~TableSetupDrawChannels */
    fun Table.setupDrawChannels() {
        val freezeRowMultiplier = if (freezeRowsCount > 0) 2 else 1
        val channelsForRow = if (flags has Tf.NoClip) 1 else columnsEnabledCount
        val channelsForBg = 1 + 1 * freezeRowMultiplier
        val channelsForDummy = (columnsEnabledCount < columnsCount || !visibleMaskByIndex.storage.contentEquals(enabledMaskByIndex.storage)).i
        val channelsTotal = channelsForBg + (channelsForRow * freezeRowMultiplier) + channelsForDummy
        drawSplitter.split(innerWindow!!.drawList, channelsTotal)
        dummyDrawChannel = if (channelsForDummy > 0) channelsTotal - 1 else -1
        bg2DrawChannelCurrent = TABLE_DRAW_CHANNEL_BG2_FROZEN
        bg2DrawChannelUnfrozen = if (freezeRowsCount > 0) 2 + channelsForRow else TABLE_DRAW_CHANNEL_BG2_FROZEN

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
        bg0ClipRectForDrawCmd put outerWindow!!.clipRect
        bg2ClipRectForDrawCmd put hostClipRect
        assert(bgClipRect.min.y <= bgClipRect.max.y)
    }

    // Layout columns for the frame. This is in essence the followup to BeginTable() and this is our largest function.
    // Runs on the first call to TableNextRow(), to give a chance for TableSetupColumn() and other TableSetupXXXXX() functions to be called first.
    // FIXME-TABLE: Our width (and therefore our WorkRect) will be minimal in the first frame for _WidthAuto columns.
    // Increase feedback side-effect with widgets relying on WorkRect.Max.x... Maybe provide a default distribution for _WidthAuto columns?
    // ~TableUpdateLayout
    fun Table.updateLayout() {

        assert(!isLayoutLocked)

        val tableSizingPolicy = flags and Tf._SizingMask
        isDefaultDisplayOrder = true
        columnsEnabledCount = 0
        enabledMaskByIndex.clearAllBits()
        enabledMaskByDisplayOrder.clearAllBits()
        leftMostEnabledColumn = -1
        minColumnWidth = 1f max (g.style.framePadding.x * 1f) // g.Style.ColumnsMinSpacing; // FIXME-TABLE

        // [Part 1] Apply/lock Enabled and Order states. Calculate auto/ideal width for columns. Count fixed/stretch columns.
        // Process columns in their visible orders as we are building the Prev/Next indices.
        var countFixed = 0                // Number of columns that have fixed sizing policies
        var countStretch = 0              // Number of columns that have stretch sizing policies
        var prevVisibleColumnIdx = -1
        var hasAutoFitRequest = false
        var hasResizable = false
        var stretchSumWidthAuto = 0f
        var fixedMaxWidthAuto = 0f
        for (orderN in 0..<columnsCount) {
            val columnN = displayOrderToIndex[orderN]
            if (columnN != orderN)
                isDefaultDisplayOrder = false
            val column = columns[columnN]

            // Clear column setup if not submitted by user. Currently we make it mandatory to call TableSetupColumn() every frame.
            // It would easily work without but we're not ready to guarantee it since e.g. names need resubmission anyway.
            // We take a slight shortcut but in theory we could be calling TableSetupColumn() here with dummy values, it should yield the same effect.
            if (declColumnsCount <= columnN) {
                setupColumnFlags(column)
                column.nameOffset = -1
                column.userID = 0
                column.initStretchWeightOrWidth = -1f
            }

            // Update Enabled state, mark settings and sort specs dirty
            if (flags hasnt Tf.Hideable || column.flags has TableColumnFlag.NoHide)
                column.isUserEnabledNextFrame = true
            if (column.isUserEnabled != column.isUserEnabledNextFrame) {
                column.isUserEnabled = column.isUserEnabledNextFrame
                isSettingsDirty = true
            }
            column.isEnabled = column.isUserEnabled && column.flags hasnt TableColumnFlag.Disabled

            if (column.sortOrder != -1 && !column.isEnabled)
                isSortSpecsDirty = true
            if (column.sortOrder > 0 && flags hasnt Tf.SortMulti)
                isSortSpecsDirty = true

            // Auto-fit unsized columns
            val startAutoFit = when {
                column.flags has TableColumnFlag.WidthFixed -> column.widthRequest < 0f
                else -> column.stretchWeight < 0f
            }
            if (startAutoFit) {
                column.autoFitQueue = (1 shl 3) - 1 // Fit for three frames
                column.cannotSkipItemsQueue = (1 shl 3) - 1 // Fit for three frames
            }

            if (!column.isEnabled) {
                column.indexWithinEnabledSet = -1
                continue
            }

            // Mark as enabled and link to previous/next enabled column
            column.prevEnabledColumn = prevVisibleColumnIdx
            column.nextEnabledColumn = -1
            if (prevVisibleColumnIdx != -1)
                columns[prevVisibleColumnIdx].nextEnabledColumn = columnN
            else
                leftMostEnabledColumn = columnN
            column.indexWithinEnabledSet = columnsEnabledCount++
            enabledMaskByIndex setBit columnN
            enabledMaskByDisplayOrder setBit column.displayOrder
            prevVisibleColumnIdx = columnN
            assert(column.indexWithinEnabledSet <= column.displayOrder)

            // Calculate ideal/auto column width (that's the width required for all contents to be visible without clipping)
            // Combine width from regular rows + width from headers unless requested not to.
            if (!column.isPreserveWidthAuto)
                column.widthAuto = getColumnWidthAuto(column)

            // Non-resizable columns keep their requested width (apply user value regardless of IsPreserveWidthAuto)
            val columnIsResizable = column.flags hasnt TableColumnFlag.NoResize
            if (columnIsResizable)
                hasResizable = true
            if (column.flags has TableColumnFlag.WidthFixed && column.initStretchWeightOrWidth > 0f && !columnIsResizable)
                column.widthAuto = column.initStretchWeightOrWidth

            if (column.autoFitQueue != 0x00)
                hasAutoFitRequest = true
            if (column.flags has TableColumnFlag.WidthStretch) {
                stretchSumWidthAuto += column.widthAuto
                countStretch++
            } else {
                fixedMaxWidthAuto = fixedMaxWidthAuto max column.widthAuto
                countFixed++
            }
        }
        if (flags has Tf.Sortable && sortSpecsCount == 0 && flags hasnt Tf.SortTristate)
            isSortSpecsDirty = true
        rightMostEnabledColumn = prevVisibleColumnIdx
        assert(leftMostEnabledColumn >= 0 && rightMostEnabledColumn >= 0)

        // [Part 2] Disable child window clipping while fitting columns. This is not strictly necessary but makes it possible
        // to avoid the column fitting having to wait until the first visible frame of the child container (may or not be a good thing).
        // FIXME-TABLE: for always auto-resizing columns may not want to do that all the time.
        if (hasAutoFitRequest && outerWindow !== innerWindow)
            innerWindow!!.skipItems = false
        if (hasAutoFitRequest)
            isSettingsDirty = true

        // [Part 3] Fix column flags and record a few extra information.
        var sumWidthRequests = 0f  // Sum of all width for fixed and auto-resize columns, excluding width contributed by Stretch columns but including spacing/padding.
        var stretchSumWeights = 0f     // Sum of all weights for weighted columns.
        leftMostStretchedColumn = -1
        rightMostStretchedColumn = -1
        for (columnN in 0 until columnsCount) {
            if (!enabledMaskByIndex.testBit(columnN))
                continue
            val column = columns[columnN]

            val columnIsResizable = column.flags hasnt TableColumnFlag.NoResize
            if (column.flags has TableColumnFlag.WidthFixed) {
                // Apply same widths policy
                var widthAuto = column.widthAuto
                if (tableSizingPolicy == Tf.SizingFixedSame && (column.autoFitQueue != 0x00 || !columnIsResizable))
                    widthAuto = fixedMaxWidthAuto

                // Apply automatic width
                // Latch initial size for fixed columns and update it constantly for auto-resizing column (unless clipped!)
                if (column.autoFitQueue != 0x00)
                    column.widthRequest = widthAuto
                else if (column.flags has TableColumnFlag.WidthFixed && !columnIsResizable && column.isRequestOutput)
                    column.widthRequest = widthAuto

                // FIXME-TABLE: Increase minimum size during init frame to avoid biasing auto-fitting widgets
                // (e.g. TextWrapped) too much. Otherwise what tends to happen is that TextWrapped would output a very
                // large height (= first frame scrollbar display very off + clipper would skip lots of items).
                // This is merely making the side-effect less extreme, but doesn't properly fixes it.
                // FIXME: Move this to ->WidthGiven to avoid temporary lossyless?
                // FIXME: This break IsPreserveWidthAuto from not flickering if the stored WidthAuto was smaller.
                if (column.autoFitQueue > 0x01 && isInitializing && !column.isPreserveWidthAuto)
                    column.widthRequest = column.widthRequest max (minColumnWidth * 4f) // FIXME-TABLE: Another constant/scale?
                sumWidthRequests += column.widthRequest
            } else {
                // Initialize stretch weight
                if (column.autoFitQueue != 0x00 || column.stretchWeight < 0f || !columnIsResizable)
                    column.stretchWeight = when {
                        column.initStretchWeightOrWidth > 0f -> column.initStretchWeightOrWidth
                        tableSizingPolicy == Tf.SizingStretchProp -> (column.widthAuto / stretchSumWidthAuto) * countStretch
                        else -> 1f
                    }

                stretchSumWeights += column.stretchWeight
                if (leftMostStretchedColumn == -1 || columns[leftMostStretchedColumn].displayOrder > column.displayOrder)
                    leftMostStretchedColumn = columnN
                if (rightMostStretchedColumn == -1 || columns[rightMostStretchedColumn].displayOrder < column.displayOrder)
                    rightMostStretchedColumn = columnN
            }
            column.isPreserveWidthAuto = false
            sumWidthRequests += cellPaddingX * 2f
        }
        columnsEnabledFixedCount = countFixed
        columnsStretchSumWeights = stretchSumWeights

        // [Part 4] Apply final widths based on requested widths
        //        val work_rect = table->WorkRect; [JVM] we use the same instance!
        val widthSpacings = outerPaddingX * 2f + (cellSpacingX1 + cellSpacingX2) * (columnsEnabledCount - 1)
        val widthRemoved = if (hasScrollbarYPrev && !innerWindow!!.scrollbar.y) g.style.scrollbarSize else 0f // To synchronize decoration width of synched tables with mismatching scrollbar state (#5920)
        val widthAvail = 1f max if (flags has Tf.ScrollX && innerWidth == 0f) innerClipRect.width else workRect.width - widthRemoved
        val widthAvailForStretchedColumns = widthAvail - widthSpacings - sumWidthRequests
        var widthRemainingForStretchedColumns = widthAvailForStretchedColumns
        columnsGivenWidth = widthSpacings + (cellPaddingX * 2) * columnsEnabledCount
        for (columnN in 0 until columnsCount) {
            if (!enabledMaskByIndex.testBit(columnN))
                continue
            val column = columns[columnN]

            // Allocate width for stretched/weighted columns (StretchWeight gets converted into WidthRequest)
            if (column.flags has TableColumnFlag.WidthStretch) {
                val weightRatio = column.stretchWeight / stretchSumWeights
                column.widthRequest = floor(max(widthAvailForStretchedColumns * weightRatio, minColumnWidth) + 0.01f)
                widthRemainingForStretchedColumns -= column.widthRequest
            }

            // [Resize Rule 1] The right-most Visible column is not resizable if there is at least one Stretch column
            // See additional comments in TableSetColumnWidth().
            if (column.nextEnabledColumn == -1 && leftMostStretchedColumn != -1)
                column.flags = column.flags or TableColumnFlag.NoDirectResize_

            // Assign final width, record width in case we will need to shrink
            column.widthGiven = floor(column.widthRequest max minColumnWidth)
            columnsGivenWidth += column.widthGiven
        }

        // [Part 5] Redistribute stretch remainder width due to rounding (remainder width is < 1.0f * number of Stretch column).
        // Using right-to-left distribution (more likely to match resizing cursor).
        if (widthRemainingForStretchedColumns >= 1f && flags hasnt Tf.PreciseWidths) {
            var orderN = columnsCount - 1
            while (stretchSumWeights > 0f && widthRemainingForStretchedColumns >= 1f && orderN >= 0) {
                if (!enabledMaskByDisplayOrder.testBit(orderN)) {
                    orderN--
                    continue
                }
                val column = columns[displayOrderToIndex[orderN]]
                if (column.flags hasnt TableColumnFlag.WidthStretch) {
                    orderN--
                    continue
                }
                column.widthRequest += 1f
                column.widthGiven += 1f
                widthRemainingForStretchedColumns -= 1f
                orderN--
            }
        }

        // Determine if table is hovered which will be used to flag columns as hovered.
        // - In principle we'd like to use the equivalent of IsItemHovered(ImGuiHoveredFlags_AllowWhenBlockedByActiveItem),
        //   but because our item is partially submitted at this point we use ItemHoverable() and a workaround (temporarily
        //   clear ActiveId, which is equivalent to the change provided by _AllowWhenBLockedByActiveItem).
        // - This allows columns to be marked as hovered when e.g. clicking a button inside the column, or using drag and drop.
        val tableInstance = getInstanceData(instanceCurrent)
        hoveredColumnBody = -1
        hoveredColumnBorder = -1
        val mouseHitRect = Rect(outerRect.min.x, outerRect.min.y, outerRect.max.x, outerRect.max.y max (outerRect.min.y + tableInstance.lastOuterHeight))
        val backupActiveId = g.activeId
        g.activeId = 0
        val isHoveringTable = ImGui.itemHoverable(mouseHitRect, 0)
        g.activeId = backupActiveId

        // [Part 6] Setup final position, offset, skip/clip states and clipping rectangles, detect hovered column
        // Process columns in their visible orders as we are comparing the visible order and adjusting host_clip_rect while looping.
        var visibleN = 0
        var offsetXFrozen = freezeColumnsCount > 0
        var offsetX = (if (freezeColumnsCount > 0) outerRect.min.x else workRect.min.x) + outerPaddingX - cellSpacingX1
        val hostClipRect = Rect(innerClipRect) // [JVM] local copy, shadow is fine
        //host_clip_rect.Max.x += table->CellPaddingX + table->CellSpacingX2;
        visibleMaskByIndex.clearAllBits()
        for (orderN in 0 until columnsCount) {
            val columnN = displayOrderToIndex[orderN]
            val column = columns[columnN]

            column.navLayerCurrent = if (freezeRowsCount > 0) NavLayer.Menu else NavLayer.Main // Use Count NOT request so Header line changes layer when frozen

            if (offsetXFrozen && freezeColumnsCount == visibleN) {
                offsetX += workRect.min.x - outerRect.min.x
                offsetXFrozen = false
            }

            // Clear status flags
            column.flags = column.flags wo TableColumnFlag.StatusMask

            if (!enabledMaskByDisplayOrder.testBit(orderN)) {
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

            // Lock start position
            column.minX = offsetX

            // Lock width based on start position and minimum/maximum width for this position
            val maxWidth = getMaxColumnWidth(columnN)
            column.widthGiven = column.widthGiven min maxWidth
            column.widthGiven = column.widthGiven max min(column.widthRequest, minColumnWidth)
            column.maxX = offsetX + column.widthGiven + cellSpacingX1 + cellSpacingX2 + cellPaddingX * 2f

            // Lock other positions
            // - ClipRect.Min.x: Because merging draw commands doesn't compare min boundaries, we make ClipRect.Min.x match left bounds to be consistent regardless of merging.
            // - ClipRect.Max.x: using WorkMaxX instead of MaxX (aka including padding) makes things more consistent when resizing down, tho slightly detrimental to visibility in very-small column.
            // - ClipRect.Max.x: using MaxX makes it easier for header to receive hover highlight with no discontinuity and display sorting arrow.
            // - FIXME-TABLE: We want equal width columns to have equal (ClipRect.Max.x - WorkMinX) width, which means ClipRect.max.x cannot stray off host_clip_rect.Max.x else right-most column may appear shorter.
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
                visibleMaskByIndex setBit columnN

            // Mark column as requesting output from user. Note that fixed + non-resizable sets are auto-fitting at all times and therefore always request output.
            column.isRequestOutput = isVisible || column.autoFitQueue != 0 || column.cannotSkipItemsQueue != 0

            // Mark column as SkipItems (ignoring all items/layout)
            column.isSkipItems = !column.isEnabled || hostSkipItems
            if (column.isSkipItems)
                assert(!isVisible)

            // Update status flags
            column.flags = column.flags or TableColumnFlag.IsEnabled
            if (isVisible)
                column.flags = column.flags or TableColumnFlag.IsVisible
            if (column.sortOrder != -1)
                column.flags = column.flags or TableColumnFlag.IsSorted
            if (hoveredColumnBody == columnN)
                column.flags = column.flags or TableColumnFlag.IsHovered

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
                hostClipRect.min.x = clamp(column.maxX + TABLE_BORDER_SIZE, hostClipRect.min.x, hostClipRect.max.x)

            offsetX += column.widthGiven + cellSpacingX1 + cellSpacingX2 + cellPaddingX * 2f
            visibleN++
        }

        // [Part 7] Detect/store when we are hovering the unused space after the right-most column (so e.g. context menus can react on it)
        // Clear Resizable flag if none of our column are actually resizable (either via an explicit _NoResize flag, either
        // because of using _WidthAuto/_WidthStretch). This will hide the resizing option from the context menu.
        val unusedX1 = workRect.min.x max columns[rightMostEnabledColumn].clipRect.max.x
        if (isHoveringTable && hoveredColumnBody == -1) {
            if (g.io.mousePos.x >= unusedX1)
                hoveredColumnBody = columnsCount
        }
        if (!hasResizable && flags has Tf.Resizable)
            flags -= Tf.Resizable

        // [Part 8] Lock actual OuterRect/WorkRect right-most position.
        // This is done late to handle the case of fixed-columns tables not claiming more widths that they need.
        // Because of this we are careful with uses of WorkRect and InnerClipRect before this point.
        if (rightMostStretchedColumn != -1)
            flags -= Tf.NoHostExtendX
        if (flags has Tf.NoHostExtendX) {
            outerRect.max.x = unusedX1
            workRect.max.x = unusedX1
            innerClipRect.max.x = innerClipRect.max.x min unusedX1
        }
        innerWindow!!.parentWorkRect put workRect
        borderX1 = innerClipRect.min.x // +((table->Flags & ImGuiTableFlags_BordersOuter) ? 0.0f : -1.0f);
        borderX2 = innerClipRect.max.x // +((table->Flags & ImGuiTableFlags_BordersOuter) ? 0.0f : +1.0f);

        // [Part 9] Allocate draw channels and setup background cliprect
        setupDrawChannels()

        // [Part 10] Hit testing on borders
        if (flags has Tf.Resizable)
            updateBorders()
        tableInstance.lastFirstRowHeight = 0f
        isLayoutLocked = true
        isUsingHeaders = false

        // [Part 11] Context menu
        if (beginContextMenuPopup()) {
            drawContextMenu()
            ImGui.endPopup()
        }

        // [Part 12] Sanitize and build sort specs before we have a chance to use them for display.
        // This path will only be exercised when sort specs are modified before header rows (e.g. init or visibility change)
        if (isSortSpecsDirty && flags has Tf.Sortable)
            sortSpecsBuild()

        // [Part 13] Setup inner window decoration size (for scrolling / nav tracking to properly take account of frozen rows/columns)
        if (freezeColumnsRequest > 0)
            innerWindow!!.decoInnerSizeX1 = columns[displayOrderToIndex[freezeColumnsRequest - 1]].maxX - outerRect.min.x
        if (freezeRowsRequest > 0)
            innerWindow!!.decoInnerSizeY1 = tableInstance.lastFrozenHeight
        tableInstance.lastFrozenHeight = 0f

        // Initial state
        innerWindow!!.let {
            if (flags has Tf.NoClip)
                drawSplitter.setCurrentChannel(it.drawList, TABLE_DRAW_CHANNEL_NOCLIP)
            else
                it.drawList.pushClipRect(it.clipRect.min, it.clipRect.max, false)
        }
    }

    // Process hit-testing on resizing borders. Actual size change will be applied in EndTable()
    // - Set table->HoveredColumnBorder with a short delay/timer to reduce visual feedback noise.
    // ~TableUpdateBorders
    fun Table.updateBorders() {

        assert(flags has Tf.Resizable)

        // At this point OuterRect height may be zero or under actual final height, so we rely on temporal coherency and
        // use the final height from last frame. Because this is only affecting _interaction_ with columns, it is not
        // really problematic (whereas the actual visual will be displayed in EndTable() and using the current frame height).
        // Actual columns highlight/render will be performed in EndTable() and not be affected.
        val tableInstance = getInstanceData(instanceCurrent)
        val hitHalfWidth = TABLE_RESIZE_SEPARATOR_HALF_THICKNESS
        val hitY1 = outerRect.min.y
        val hitY2Body = outerRect.max.y max (hitY1 + tableInstance.lastOuterHeight)
        val hitY2Head = hitY1 + tableInstance.lastFirstRowHeight

        for (orderN in 0 until columnsCount) {

            if (!enabledMaskByDisplayOrder.testBit(orderN))
                continue

            val columnN = displayOrderToIndex[orderN]
            val column = columns[columnN]
            if (column.flags has (TableColumnFlag.NoResize or TableColumnFlag.NoDirectResize_))
                continue

            // ImGuiTableFlags_NoBordersInBodyUntilResize will be honored in TableDrawBorders()
            val borderY2Hit = if (flags has Tf.NoBordersInBody) hitY2Head else hitY2Body
            if (flags has Tf.NoBordersInBody && !isUsingHeaders)
                continue

            if (!column.isVisibleX && lastResizedColumn != columnN)
                continue

            val columnId = getColumnResizeID(columnN, instanceCurrent)
            val hitRect = Rect(column.maxX - hitHalfWidth, hitY1, column.maxX + hitHalfWidth, borderY2Hit)
            ImGui.itemAdd(hitRect, columnId, null, ItemFlag.NoNav)
            //GetForegroundDrawList()->AddRect(hit_rect.Min, hit_rect.Max, IM_COL32(255, 0, 0, 100));

            var (pressed, hovered, held) = ImGui.buttonBehavior(hitRect, columnId, ButtonFlag.FlattenChildren or ButtonFlag.PressedOnClick or ButtonFlag.PressedOnDoubleClick or ButtonFlag.NoNavFocus)
            if (pressed && MouseButton.Left.isDoubleClicked) {
                setColumnWidthAutoSingle(columnN)
                ImGui.clearActiveID()
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

    /** ~static void TableUpdateColumnsWeightFromWidth(ImGuiTable* table) */
    fun Table.updateColumnsWeightFromWidth() {

        assert(leftMostStretchedColumn != -1 && rightMostStretchedColumn != -1)

        // Measure existing quantities
        var visibleWeight = 0f
        var visibleWidth = 0f
        for (columnN in 0 until columnsCount) {
            val column = columns[columnN]
            if (!column.isEnabled || column.flags hasnt TableColumnFlag.WidthStretch)
                continue
            assert(column.stretchWeight > 0f)
            visibleWeight += column.stretchWeight
            visibleWidth += column.widthRequest
        }
        assert(visibleWeight > 0f && visibleWidth > 0f)

        // Apply new weights
        for (columnN in 0 until columnsCount) {
            val column = columns[columnN]
            if (!column.isEnabled || column.flags hasnt TableColumnFlag.WidthStretch)
                continue
            column.stretchWeight = (column.widthRequest / visibleWidth) * visibleWeight
            assert(column.stretchWeight > 0f)
        }
    }

    /** FIXME-TABLE: This is a mess, need to redesign how we render borders (as some are also done in TableEndRow)
     *  ~TableDrawBorders */
    fun Table.drawBorders() {
        val innerWindow = innerWindow!!
        if (!outerWindow!!.clipRect.overlaps(outerRect))
            return

        val innerDrawlist = innerWindow.drawList
        drawSplitter.setCurrentChannel(innerDrawlist, TABLE_DRAW_CHANNEL_BG0)
        innerDrawlist.pushClipRect(bg0ClipRectForDrawCmd.min, bg0ClipRectForDrawCmd.max, false)

        // Draw inner border and resizing feedback
        val tableInstance = getInstanceData(instanceCurrent)
        val borderSize = TABLE_BORDER_SIZE
        val drawY1 = innerRect.min.y
        val drawY2Body = innerRect.max.y
        val drawY2Head = when {
            isUsingHeaders -> innerRect.max.y min ((if (freezeRowsCount >= 1) innerRect.min.y else workRect.min.y) + tableInstance.lastFirstRowHeight)
            else -> drawY1
        }
        if (flags has Tf.BordersInnerV)
            for (orderN in 0 until columnsCount) {

                if (!enabledMaskByDisplayOrder.testBit(orderN))
                    continue

                val columnN = displayOrderToIndex[orderN]
                val column = columns[columnN]
                val isHovered = hoveredColumnBorder == columnN
                val isResized = resizedColumn == columnN && instanceInteracted == instanceCurrent
                val isResizable = column.flags hasnt (TableColumnFlag.NoResize or TableColumnFlag.NoDirectResize_)
                val isFrozenSeparator = freezeColumnsCount == orderN + 1
                if (column.maxX > innerClipRect.max.x && !isResized)
                    continue

                // Decide whether right-most column is visible
                if (column.nextEnabledColumn == -1 && !isResizable)
                    if (flags and Tf._SizingMask != Tf.SizingFixedSame || flags has Tf.NoHostExtendX)
                        continue
                if (column.maxX <= column.clipRect.min.x) // FIXME-TABLE FIXME-STYLE: Assume BorderSize==1, this is problematic if we want to increase the border size..
                    continue

                // Draw in outer window so right-most column won't be clipped
                // Always draw full height border when being resized/hovered, or on the delimitation of frozen column scrolling.
                val col: Int
                val drawY2: Float
                if (isHovered || isResized || isFrozenSeparator) {
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
        // FIXME: could use AddRect or explicit VLine/HLine helper?
        if (flags has Tf.BordersOuter) {
            // Display outer border offset by 1 which is a simple way to display it without adding an extra draw call
            // (Without the offset, in outer_window it would be rendered behind cells, because child windows are above their
            // parent. In inner_window, it won't reach out over scrollbars. Another weird solution would be to display part
            // of it in inner window, and the part that's over scrollbars in the outer window..)
            // Either solution currently won't allow us to use a larger border size: the border would clipped.
            val outerBorder = Rect(outerRect) // [JVM] we need a local copy, shadowing is fine
            val outerCol = borderColorStrong
            if (Tf.BordersOuter in flags)
                innerDrawlist.addRect(outerBorder.min, outerBorder.max, outerCol, 0f, thickness = borderSize)
            else if (flags has Tf.BordersOuterV) {
                innerDrawlist.addLine(outerBorder.min, Vec2(outerBorder.min.x, outerBorder.max.y), outerCol, borderSize)
                innerDrawlist.addLine(Vec2(outerBorder.max.x, outerBorder.min.y), outerBorder.max, outerCol, borderSize)
            } else if (flags has Tf.BordersOuterH) {
                innerDrawlist.addLine(outerBorder.min, Vec2(outerBorder.max.x, outerBorder.min.y), outerCol, borderSize)
                innerDrawlist.addLine(Vec2(outerBorder.min.x, outerBorder.max.y), outerBorder.max, outerCol, borderSize)
            }
        }
        if (flags has Tf.BordersInnerH && rowPosY2 < outerRect.max.y) {
            // Draw bottom-most row border
            val borderY = rowPosY2
            if (borderY >= bgClipRect.min.y && borderY < bgClipRect.max.y)
                innerDrawlist.addLine(Vec2(borderX1, borderY), Vec2(borderX2, borderY), borderColorLight, borderSize)
        }

        innerDrawlist.popClipRect()
    }

    /** Output context menu into current window (generally a popup)
     *  FIXME-TABLE: Ideally this should be writable by the user. Full programmatic access to that data?
     *  ~TableDrawContextMenu */
    fun Table.drawContextMenu() {

        val window = g.currentWindow!!
        if (window.skipItems)
            return

        var wantSeparator = false
        val columnN = if (contextPopupColumn in 0 until columnsCount) contextPopupColumn else -1
        val column = columns.getOrNull(columnN)

        // Sizing
        if (flags has Tf.Resizable) {
            if (column != null) {
                val canResize = column.flags hasnt TableColumnFlag.NoResize && column.isEnabled
                if (menuItem(LocKey.TableSizeOne.msg, "", false, canResize)) // "###SizeOne"
                    setColumnWidthAutoSingle(columnN)
            }

            val sizeAllDesc = when {
                columnsEnabledFixedCount == columnsEnabledCount && flags and Tf._SizingMask != Tf.SizingFixedSame -> LocKey.TableSizeAllFit.msg // "###SizeAll" All fixed
                else -> LocKey.TableSizeAllDefault.msg // "###SizeAll" All stretch or mixed
            }
            if (menuItem(sizeAllDesc, ""))
                setColumnWidthAutoAll()
            wantSeparator = true
        }

        // Ordering
        if (flags has Tf.Reorderable) {
            if (menuItem(LocKey.TableResetOrder.msg, "", false, !isDefaultDisplayOrder))
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
                ImGui.separator()
            wantSeparator = true

            ImGui.pushItemFlag(ItemFlag.SelectableDontClosePopup, true)
            for (otherColumnN in 0 until columnsCount) {
                val otherColumn = columns[otherColumnN]
                if (otherColumn.flags has TableColumnFlag.Disabled)
                    continue

                var name = getColumnName(otherColumnN)
                if (name == null || name.isEmpty())
                    name = "<Unknown>"

                // Make sure we can't hide the last active column
                var menuItemActive = otherColumn.flags.hasnt(TableColumnFlag.NoHide) == true
                if (otherColumn.isUserEnabled && columnsEnabledCount <= 1)
                    menuItemActive = false
                if (ImGui.menuItem(name, "", otherColumn.isUserEnabled, menuItemActive))
                    otherColumn.isUserEnabledNextFrame = !otherColumn.isUserEnabled
            }
            ImGui.popItemFlag()
        }
    }

    /** ~TableBeginContextMenuPopup */
    fun Table.beginContextMenuPopup(): Boolean {
        if (!isContextPopupOpen || instanceCurrent != instanceInteracted)
            return false
        val contextMenuId = hashStr("##ContextMenu", 0, id)
        if (ImGui.beginPopupEx(contextMenuId, Wf.AlwaysAutoResize or Wf.NoTitleBar or Wf.NoSavedSettings))
            return true
        isContextPopupOpen = false
        return false
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
    fun Table.mergeDrawChannels() {

        val splitter = drawSplitter
        val hasFreezeV = freezeRowsCount > 0
        val hasFreezeH = freezeColumnsCount > 0
        assert(splitter._current == 0)

        // Track which groups we are going to attempt to merge, and which channels goes into each group.
        class MergeGroup(channelsMaskSize: Int) {
            val clipRect = Rect()
            var channelsCount = 0
            val channelsMask = BitArray(channelsMaskSize)
        }

        var mergeGroupMask = 0x00
        // Use a reusable temp buffer for the merge masks as they are dynamically sized.
        val maxDrawChannels = 4 + columnsCount * 2
        val remainingMask = BitArray(maxDrawChannels)
        val mergeGroups = Array(4) { MergeGroup(maxDrawChannels) }

        // 1. Scan channels and take note of those which can be merged
        for (columnN in 0 until columnsCount) {
            if (!visibleMaskByIndex.testBit(columnN))
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
                if (column.flags hasnt TableColumnFlag.NoClip) {
                    val contentMaxX = when {
                        !hasFreezeV -> column.contentMaxXUnfrozen max column.contentMaxXHeadersUsed // No row freeze
                        mergeGroupSubN == 0 -> column.contentMaxXFrozen max column.contentMaxXHeadersUsed   // Row freeze: use width before freeze
                        else -> column.contentMaxXUnfrozen                                        // Row freeze: use width after freeze
                    }
                    if (contentMaxX > column.clipRect.max.x)
                        continue
                }

                val mergeGroupN = (if (hasFreezeH && columnN < freezeColumnsCount) 0 else 1) + if (hasFreezeV && mergeGroupSubN == 0) 0 else 2
                assert(channelNo < maxDrawChannels)
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
            // We skip channel 0 (Bg0/Bg1) and 1 (Bg2 frozen) from the shuffling since they won't move - see channels allocation in TableSetupDrawChannels().
            val LEADING_DRAW_CHANNELS = 2
            //            g.drawChannelsTempMergeBuffer.resize(splitter->_Count - LEADING_DRAW_CHANNELS) // Use shared temporary storage so the allocation gets amortized
            var dstTmp = 0 //~g.drawChannelsTempMergeBuffer.Data

            remainingMask.setBitRange(LEADING_DRAW_CHANNELS, splitter._count)
            remainingMask clearBit bg2DrawChannelUnfrozen
            assert(!hasFreezeV || bg2DrawChannelUnfrozen != TABLE_DRAW_CHANNEL_BG2_FROZEN)
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
//                    GetOverlayDrawList()->AddRect(merge_group->ClipRect.Min, merge_group->ClipRect.Max, IM_COL32(255, 0, 0, 200), 0.0f, 0, 1.0f)
//                    GetOverlayDrawList()->AddLine(merge_group->ClipRect.Min, merge_clip_rect.Min, IM_COL32(255, 100, 0, 200))
//                    GetOverlayDrawList()->AddLine(merge_group->ClipRect.Max, merge_clip_rect.Max, IM_COL32(255, 100, 0, 200))
                    remainingCount -= mergeGroup.channelsCount
                    for (n in remainingMask.indices)
                        remainingMask.storage[n] = remainingMask.storage[n] wo mergeGroup.channelsMask.storage[n]
                    for (n in 0..<splitter._count) {
                        if (mergeChannelsCount == 0)
                            break
                        // Copy + overwrite new clip rect
                        if (!mergeGroup.channelsMask.testBit(n))
                            continue
                        mergeGroup.channelsMask clearBit n
                        mergeChannelsCount--

                        val channel = splitter._channels[n]
                        assert(channel._cmdBuffer.size == 1 && mergeClipRect.contains(Rect(channel._cmdBuffer[0].clipRect)))
                        channel._cmdBuffer[0].clipRect = mergeClipRect.toVec4()
//                        memcpy(dstTmp++, channel, sizeof(ImDrawChannel))
                        g.drawChannelsTempMergeBuffer += channel; dstTmp++
                    }
                }

                // Make sure Bg2DrawChannelUnfrozen appears in the middle of our groups (whereas Bg0/Bg1 and Bg2 frozen are fixed to 0 and 1)
                if (mergeGroupN == 1 && hasFreezeV) {
//                    memcpy(dstTmp++, & splitter->_Channels[table->Bg1DrawChannelUnfrozen], sizeof(ImDrawChannel))
                    g.drawChannelsTempMergeBuffer += splitter._channels[bg2DrawChannelUnfrozen]; dstTmp++
                }
            }

            // Append unmergeable channels that we didn't reorder at the end of the list
            for (n in 0..<splitter._count) {
                if (remainingCount == 0)
                    break
                if (!remainingMask.testBit(n))
                    continue
                val channel = splitter._channels[n]
                g.drawChannelsTempMergeBuffer += channel; dstTmp++
                remainingCount--
            }
            assert(dstTmp == /*~g.drawChannelsTempMergeBuffer.Data */ g.drawChannelsTempMergeBuffer.size)
            //            memcpy(splitter._channels.Data + LEADING_DRAW_CHANNELS, g.DrawChannelsTempMergeBuffer.Data, (splitter->_Count - LEADING_DRAW_CHANNELS) * sizeof(ImDrawChannel))
            for (i in 0 until splitter._count - LEADING_DRAW_CHANNELS)
                splitter._channels[LEADING_DRAW_CHANNELS + i] = g.drawChannelsTempMergeBuffer[i]
            g.drawChannelsTempMergeBuffer.clear() // [JVM]
        }
    }

    /** ~TableGetInstanceData */
    infix fun Table.getInstanceData(instanceNo: Int): TableInstanceData = if (instanceNo == 0) instanceDataFirst else instanceDataExtra[instanceNo - 1]

    /** ~TableGetInstanceID */
    infix fun Table.getInstanceID(instanceNo: Int): ID = getInstanceData(instanceNo).tableInstanceID

    /** ~TableSortSpecsSanitize */
    fun Table.sortSpecsSanitize() {

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

        // Fallback default sort order (if no column with the ImGuiTableColumnFlags_DefaultSort flag)
        if (sortOrderCount == 0 && flags hasnt Tf.SortTristate)
            for (columnN in 0 until columnsCount) {
                val column = columns[columnN]
                if (column.isEnabled && column.flags hasnt TableColumnFlag.NoSort) {
                    sortOrderCount = 1
                    column.sortOrder = 0
                    column.sortDirection = tableGetColumnAvailSortDirection(column, 0)
                    break
                }
            }

        sortSpecsCount = sortOrderCount
    }

    /** ~TableSortSpecsBuild */
    fun Table.sortSpecsBuild() {

        val dirty = isSortSpecsDirty
        if (dirty) {
            sortSpecsSanitize()
            //            sortSpecsMulti.resize(table->SortSpecsCount <= 1 ? 0 : table->SortSpecsCount);
            if (sortSpecsCount == 0)
                sortSpecsMulti.clear()
            else
                for (i in sortSpecsMulti.size until sortSpecsCount)
                    sortSpecsMulti += TableColumnSortSpecs()
            sortSpecs.specsDirty = true // Mark as dirty for user
            isSortSpecsDirty = false // Mark as not dirty for us
        }

        // Write output
        val sortSpecs = when (sortSpecsCount) {
            0 -> arrayListOf()
            1 -> arrayListOf(sortSpecsSingle)
            else -> sortSpecsMulti
        }
        if (dirty && sortSpecs.isNotEmpty())
            for (columnN in 0 until columnsCount) {
                val column = columns[columnN]
                if (column.sortOrder == -1)
                    continue
                assert(column.sortOrder < sortSpecsCount)
                val sortSpec = when (sortSpecsCount) {
                    0 -> null
                    1 -> sortSpecsSingle
                    else -> sortSpecsMulti[column.sortOrder]
                }!!
                sortSpec.columnUserID = column.userID
                sortSpec.columnIndex = columnN
                sortSpec.sortOrder = column.sortOrder
                sortSpec.sortDirection = column.sortDirection
            }

        this.sortSpecs.specs = sortSpecs
        this.sortSpecs.specsCount = sortSpecsCount
    }

    /** Calculate next sort direction that would be set after clicking the column
     *  - If the PreferSortDescending flag is set, we will default to a Descending direction on the first click.
     *  - Note that the PreferSortAscending flag is never checked, it is essentially the default and therefore a no-op. */
    //    IM_STATIC_ASSERT(ImGuiSortDirection_None == 0 && ImGuiSortDirection_Ascending == 1 && ImGuiSortDirection_Descending == 2);
    fun tableGetColumnNextSortDirection(column: TableColumn): SortDirection {
        assert(column.sortDirectionsAvailCount > 0)
        if (column.sortOrder == -1)
            return tableGetColumnAvailSortDirection(column, 0)
        for (n in 0..2)
            if (column.sortDirection == tableGetColumnAvailSortDirection(column, n))
                return tableGetColumnAvailSortDirection(column, (n + 1) % column.sortDirectionsAvailCount)
        assert(false)
        return SortDirection.None
    }

    /** Fix sort direction if currently set on a value which is unavailable (e.g. activating NoSortAscending/NoSortDescending)
     *
     *  ~tableFixColumnSortDirection */
    infix fun Table.fixColumnSortDirection(column: TableColumn) {
        if (column.sortOrder == -1 || column.sortDirectionsAvailMask has (1 shl column.sortDirection.i))
            return
        column.sortDirection = tableGetColumnAvailSortDirection(column, 0)
        isSortSpecsDirty = true
    }

    /** Note this is meant to be stored in column->WidthAuto, please generally use the WidthAuto field
     *  ~TableGetColumnWidthAuto */
    infix fun Table.getColumnWidthAuto(column: TableColumn): Float {
        val contentWidthBody = (column.contentMaxXFrozen max column.contentMaxXUnfrozen) - column.workMinX
        val contentWidthHeaders = column.contentMaxXHeadersIdeal - column.workMinX
        var widthAuto = contentWidthBody
        if (column.flags hasnt TableColumnFlag.NoHeaderWidth)
            widthAuto = widthAuto max contentWidthHeaders

        // Non-resizable fixed columns preserve their requested width
        if (column.flags has TableColumnFlag.WidthFixed && column.initStretchWeightOrWidth > 0f)
            if (flags hasnt Tf.Resizable || column.flags has TableColumnFlag.NoResize)
                widthAuto = column.initStretchWeightOrWidth

        return widthAuto max minColumnWidth
    }

    /** [Internal] Called by TableNextRow()
     *  ~TableBeginRow */
    fun Table.beginRow() {

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
        window.dc.currLineSize put 0f
        window.dc.isSameLine = false; window.dc.isSetPos = false
        window.dc.cursorMaxPos.y = nextY1

        // Making the header BG color non-transparent will allow us to overlay it multiple times when handling smooth dragging.
        if (rowFlags has Trf.Headers) {
            ImGui.tableSetBgColor(TableBgTarget.RowBg0, Col.TableHeaderBg.u32)
            if (currentRow == 0)
                isUsingHeaders = true
        }
    }

    /** [Internal] Called by TableNextRow()
     *  ~TableEndRow */
    fun Table.endRow() {
        val window = g.currentWindow!!
        assert(window === innerWindow)
        assert(isInsideRow)

        if (currentColumn != -1)
            endCell()

        // Logging
        if (g.logEnabled)
            logRenderedText(null, "|")

        // Position cursor at the bottom of our row so it can be used for e.g. clipping calculation. However it is
        // likely that the next call to TableBeginCell() will reposition the cursor to take account of vertical padding.
        window.dc.cursorPos.y = rowPosY2

        // Row background fill
        val bgY1 = rowPosY1
        val bgY2 = rowPosY2
        val unfreezeRowsActual = currentRow + 1 == freezeRowsCount
        val unfreezeRowsRequest = currentRow + 1 == freezeRowsRequest
        if (currentRow == 0)
            getInstanceData(instanceCurrent).lastFirstRowHeight = bgY2 - bgY1

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
                    window.drawList._cmdHeader.clipRect put bg0ClipRectForDrawCmd.toVec4()
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
                    // As we render the BG here we need to clip things (for layout we would not)
                    // FIXME: This cancels the OuterPadding addition done by TableGetCellBgRect(), need to keep it while rendering correctly while scrolling.
                    val cellData = rowCellData[idx++]
                    val column = columns[cellData.column]
                    val cellBgRect = getCellBgRect(cellData.column)
                    cellBgRect.clipWith(bgClipRect)
                    cellBgRect.min.x = cellBgRect.min.x max column.clipRect.min.x     // So that first column after frozen one gets clipped when scrolling
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
            for (columnN in 0 until columnsCount)
                columns[columnN].navLayerCurrent = NavLayer.Main
        if (unfreezeRowsActual) {

            assert(!isUnfrozenRows)
            val y0 = (rowPosY2 + 1) max window.innerClipRect.min.y
            isUnfrozenRows = true
            getInstanceData(instanceCurrent).lastFrozenHeight = y0 - outerRect.min.y

            // BgClipRect starts as table->InnerClipRect, reduce it now and make BgClipRectForDrawCmd == BgClipRect
            bgClipRect.min.y = y0 min window.innerClipRect.max.y
            bg2ClipRectForDrawCmd.min.y = bgClipRect.min.y
            bgClipRect.max.y = window.innerClipRect.max.y
            bg2ClipRectForDrawCmd.max.y = bgClipRect.max.y
            bg2DrawChannelCurrent = bg2DrawChannelUnfrozen
            assert(bg2ClipRectForDrawCmd.min.y <= bg2ClipRectForDrawCmd.max.y)

            val rowHeight = rowPosY2 - rowPosY1
            rowPosY2 = workRect.min.y + rowPosY2 - outerRect.min.y
            window.dc.cursorPos.y = rowPosY2
            rowPosY1 = rowPosY2 - rowHeight
            for (columnN in 0 until columnsCount) {
                val column = columns[columnN]
                column.drawChannelCurrent = column.drawChannelUnfrozen
                column.clipRect.min.y = bg2ClipRectForDrawCmd.min.y
            }

            // Update cliprect ahead of TableBeginCell() so clipper can access to new ClipRect->Min.y
            ImGui.setWindowClipRectBeforeSetChannel(window, columns[0].clipRect)
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
    infix fun Table.beginCell(columnN: Int) {

        val g = gImGui
        val column = columns[columnN]
        val window = innerWindow!!
        currentColumn = columnN

        // Start position is roughly ~~ CellRect.Min + CellPadding + Indent
        var startX = column.workMinX
        if (column.flags has TableColumnFlag.IndentEnable)
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

        window.skipItems = column.isSkipItems
        if (column.isSkipItems) {
            g.lastItemData.id = 0
            g.lastItemData.statusFlags = none
        }

        if (flags has Tf.NoClip) {
            // FIXME: if we end up drawing all borders/bg in EndTable, could remove this and just assert that channel hasn't changed.
            drawSplitter.setCurrentChannel(window.drawList, TABLE_DRAW_CHANNEL_NOCLIP)
            //IM_ASSERT(table->DrawSplitter._Current == TABLE_DRAW_CHANNEL_NOCLIP);
        } else {
            // FIXME-TABLE: Could avoid this if draw channel is dummy channel?
            ImGui.setWindowClipRectBeforeSetChannel(window, column.clipRect)
            drawSplitter.setCurrentChannel(window.drawList, column.drawChannelCurrent)
        }

        // Logging
        if (g.logEnabled && !column.isSkipItems) {
            logRenderedText(window.dc.cursorPos, "|")
            g.logLinePosY = Float.MAX_VALUE
        }
    }

    /** [Internal] Called by TableNextRow()/TableSetColumnIndex()/TableNextColumn()
     *  ~TableEndCell */
    fun Table.endCell() {

        val column = columns[currentColumn]
        val window = innerWindow!!

        if (window.dc.isSetPos)
            ImGui.errorCheckUsingSetCursorPosToExtendParentBoundaries()

        // Report maximum position so we can infer content size per column.
        val maxPosX = when {
            rowFlags has Trf.Headers -> column::contentMaxXHeadersUsed  // Useful in case user submit contents in header row that is not a TableHeader() call
            else -> if (isUnfrozenRows) column::contentMaxXUnfrozen else column::contentMaxXFrozen
        }
        maxPosX.set(maxPosX() max window.dc.cursorMaxPos.x)
        if (column.isEnabled)
            rowPosY2 = rowPosY2 max (window.dc.cursorMaxPos.y + cellPaddingY)
        column.itemWidth = window.dc.itemWidth

        // Propagate text baseline for the entire row
        // FIXME-TABLE: Here we propagate text baseline from the last line of the cell.. instead of the first one.
        rowTextBaseline = rowTextBaseline max window.dc.prevLineTextBaseOffset
    }

    /** Return the cell rectangle based on currently known height.
     *  - Important: we generally don't know our row height until the end of the row, so Max.y will be incorrect in many situations.
     *    The only case where this is correct is if we provided a min_row_height to TableNextRow() and don't go below it, or in TableEndRow() when we locked that height.
     *  - Important: if ImGuiTableFlags_PadOuterX is set but ImGuiTableFlags_PadInnerX is not set, the outer-most left and right
     *    columns report a small offset so their CellBgRect can extend up to the outer border.
     *    FIXME: But the rendering code in TableEndRow() nullifies that with clamping required for scrolling.
     *
     *  ~TableGetCellBgRect */
    infix fun Table.getCellBgRect(columnN: Int): Rect {
        val column = columns[columnN]
        var x1 = column.minX
        var x2 = column.maxX
        //        if (column.prevEnabledColumn == -1)
        //            x1 -= OuterPaddingX
        //        if (column.nextEnabledColumn == -1)
        //            x2 += OuterPaddingX
        x1 = x1 max workRect.min.x
        x2 = x2 min workRect.max.x
        return Rect(x1, rowPosY1, x2, rowPosY2)
    }

    /** ~TableGetColumnName */
    infix fun Table.getColumnName(columnN: Int): String = when {
        !isLayoutLocked && columnN >= declColumnsCount -> "" // NameOffset is invalid at this point
        else -> columnsNames.getOrElse(columns[columnN].nameOffset) { "" }
    }

    /** Return the resizing ID for the right-side of the given column.
     *  ~TableGetColumnResizeID */
    fun Table.getColumnResizeID(columnN: Int, instanceNo: Int = 0): ID {
        assert(columnN in 0 until columnsCount)
        val instanceId = getInstanceID(instanceNo)
        return instanceId + 1 + columnN // FIXME: #6140: still not ideal
    }

    /** Maximum column content width given current layout. Use column->MinX so this value on a per-column basis.
     * ~TableGetMaxColumnWidth */
    infix fun Table.getMaxColumnWidth(columnN: Int): Float {
        val column = columns[columnN]
        var maxWidth = Float.MAX_VALUE
        val minColumnDistance = minColumnWidth + cellPaddingX * 2f + cellSpacingX1 + cellSpacingX2
        if (flags has Tf.ScrollX) {
            // Frozen columns can't reach beyond visible width else scrolling will naturally break.
            // (we use DisplayOrder as within a set of multiple frozen column reordering is possible)
            if (column.displayOrder < freezeColumnsRequest) {
                maxWidth = (innerClipRect.max.x - (freezeColumnsRequest - column.displayOrder) * minColumnDistance) - column.minX
                maxWidth = maxWidth - outerPaddingX - cellPaddingX - cellSpacingX2
            }
        } else if (flags hasnt Tf.NoKeepColumnsVisible) {
            // If horizontal scrolling if disabled, we apply a final lossless shrinking of columns in order to make
            // sure they are all visible. Because of this we also know that all of the columns will always fit in
            // table->WorkRect and therefore in table->InnerRect (because ScrollX is off)
            // FIXME-TABLE: This is solved incorrectly but also quite a difficult problem to fix as we also want ClipRect width to match.
            // See "table_width_distrib" and "table_width_keep_visible" tests
            maxWidth = workRect.max.x - (columnsEnabledCount - column.indexWithinEnabledSet - 1) * minColumnDistance - column.minX
            //max_width -= table->CellSpacingX1;
            maxWidth -= cellSpacingX2
            maxWidth -= cellPaddingX * 2f
            maxWidth -= outerPaddingX
        }
        return maxWidth
    }

    /** Disable clipping then auto-fit, will take 2 frames
     *  (we don't take a shortcut for unclipped columns to reduce inconsistencies when e.g. resizing multiple columns)
     *  ~TableSetColumnWidthAutoSingle */
    infix fun Table.setColumnWidthAutoSingle(columnN: Int) {
        // Single auto width uses auto-fit
        val column = columns[columnN]
        if (!column.isEnabled)
            return
        column.cannotSkipItemsQueue = 1 shl 0
        autoFitSingleColumn = columnN
    }

    /** ~TableSetColumnWidthAutoAll */
    fun Table.setColumnWidthAutoAll() {
        for (columnN in 0 until columnsCount) {
            val column = columns[columnN]
            if (!column.isEnabled && column.flags hasnt TableColumnFlag.WidthStretch) // Cannot reset weight of hidden stretch column
                continue
            column.cannotSkipItemsQueue = 1 shl 0
            column.autoFitQueue = 1 shl 1
        }
    }

    /** Remove Table (currently only used by TestEngine)
     *  ~TableRemove */
    fun Table.remove() {
        //IMGUI_DEBUG_PRINT("TableRemove() id=0x%08X\n", table->ID);
        val tableIdx = g.tables.getIndex(this)
        //memset(table->RawData.Data, 0, table->RawData.size_in_bytes());
        //memset(table, 0, sizeof(ImGuiTable));
        g.tables.remove(id, this)
        g.tablesLastTimeActive[tableIdx.i] = -1f
    }

    /** Free up/compact internal Table buffers for when it gets unused
     *  ~TableGcCompactTransientBuffers */
    fun Table.gcCompactTransientBuffers() {
        //IMGUI_DEBUG_PRINT("TableGcCompactTransientBuffers() id=0x%08X\n", table->ID);
        assert(!memoryCompacted)
        sortSpecs.specs.clear()
        sortSpecsMulti.clear()
        isSortSpecsDirty = true // FIXME: In theory shouldn't have to leak into user performing a sort on resume.
        columnsNames.clear()
        memoryCompacted = true
        for (n in 0 until columnsCount)
            columns[n].nameOffset = -1
        g.tablesLastTimeActive[g.tables.getIndex(this).i] = -1f
    }

    /** ~TableGcCompactTransientBuffers */
    fun gcCompactTransientBuffers(tempData: TableTempData) {
        tempData.drawSplitter.clearFreeMemory()
        tempData.lastTimeActive = -1f
    }

    /** Compact and remove unused settings data (currently only used by TestEngine) */
    fun tableGcCompactSettings() {
        g.settingsTables.removeIf { it.id == 0 }
        //        var requiredMemory = 0;
        //        for (settings in g.settingsTables)
        //            if (settings.id != 0)
        //                requiredMemory += tableSettingsCalcChunkSize(settings.columnsCount);
        //        if (requiredMemory == g.settingsTables.Buf.Size)
        //            return;
        //        ImChunkStream<ImGuiTableSettings> new_chunk_stream;
        //        new_chunk_stream.Buf.reserve(requiredMemory);
        //        for (ImGuiTableSettings* settings = g.SettingsTables.begin(); settings != NULL; settings = g.SettingsTables.next_chunk(settings))
        //        if (settings->ID != 0)
        //        memcpy(new_chunk_stream.alloc_chunk(TableSettingsCalcChunkSize(settings->ColumnsCount)), settings, TableSettingsCalcChunkSize(settings->ColumnsCount));
        //        g.SettingsTables.swap(new_chunk_stream);
    }
}