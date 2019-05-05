package imgui.imgui

import glm_.f
import glm_.glm
import glm_.i
import glm_.max
import imgui.*
import imgui.ImGui.beginColumns
import imgui.ImGui.currentWindow
import imgui.ImGui.currentWindowRead
import imgui.ImGui.endColumns
import imgui.ImGui.popClipRect
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushColumnClipRect
import imgui.ImGui.pushItemWidth
import imgui.ImGui.pushOverrideID
import imgui.ImGui.style
import imgui.internal.*
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KMutableProperty0
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
                // New column (columns 1+ cancels out IndentX)
                dc.columnsOffset = getColumnOffset(columns.current) - dc.indent + style.itemSpacing.x
                drawList.channelsSetCurrent(columns.current)
            } else {
                // New row/line
                dc.columnsOffset = 0f
                drawList.channelsSetCurrent(0)
                columns.current = 0
                columns.lineMinY = columns.lineMaxY
            }
            dc.cursorPos.x = (pos.x + dc.indent + dc.columnsOffset).i.f
            dc.cursorPos.y = columns.lineMinY
            dc.currentLineSize.y = 0f
            dc.currentLineTextBaseOffset = 0f
        }
        pushColumnClipRect()
        pushItemWidth(getColumnWidth() * 0.65f)  // FIXME-COLUMNS: Move on columns setup
    }

    /** get current column index    */
    val columnIndex get() = currentWindowRead!!.dc.currentColumns?.current ?: 0

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
        return lerp(columns.minX, columns.maxX, t) // xOffset
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
        else min(offset_, columns.maxX - style.columnsMinSpacing * (columns.count - columnIndex))
        columns.columns[columnIndex].offsetNorm = pixelsToOffsetNorm(columns, offset - columns.minX)

        if (preserveWidth)
            setColumnOffset(columnIndex + 1, offset + glm.max(style.columnsMinSpacing, width))
    }

    /** number of columns (what was passed to Columns())    */
    val columnsCount get() = currentWindowRead!!.dc.currentColumns?.count ?: 1

    // Tab Bars, Tabs
    // [BETA API] API may evolve!

    /** create and append into a TabBar */
    fun beginTabBar(strId: String, flags: TabBarFlags = 0): Boolean {

        val window = g.currentWindow!!
        if (window.skipItems) return false

        val id = window.getId(strId)
        val tabBar = g.tabBars.getOrAddByKey(id)
        val tabBarBb = Rect(window.dc.cursorPos.x, window.dc.cursorPos.y, window.innerClipRect.max.x, window.dc.cursorPos.y + g.fontSize + style.framePadding.y * 2)
        tabBar.id = id
        return tabBar.beginEx(tabBarBb, flags or TabBarFlag.IsFocused)
    }

    /**  only call EndTabBar() if BeginTabBar() returns true! */
    fun endTabBar() {

        val window = g.currentWindow!!
        if (window.skipItems) return

        val tabBar = g.currentTabBar ?: error("Mismatched BeginTabBar()/EndTabBar()!") // FIXME-ERRORHANDLING
        if (tabBar.wantLayout)
            tabBar.layout()

        // Restore the last visible height if no tab is visible, this reduce vertical flicker/movement when a tabs gets removed without calling SetTabItemClosed().
        val tabBarAppearing = tabBar.prevFrameVisible + 1 < g.frameCount
        if (tabBar.visibleTabWasSubmitted || tabBar.visibleTabId == 0 || tabBarAppearing)
            tabBar.contentsHeight = (window.dc.cursorPos.y - tabBar.barRect.max.y) max 0f
        else
            window.dc.cursorPos.y = tabBar.barRect.max.y + tabBar.contentsHeight

        if (tabBar.flags hasnt TabBarFlag.DockNode)
            popId()

        g.currentTabBarStack.pop()
        g.currentTabBar = g.currentTabBarStack.lastOrNull()?.tabBar
    }

    /** create a Tab. Returns true if the Tab is selected. */
    fun beginTabItem(label: String, pOpen: BooleanArray, index: Int, flags: TabItemFlags = 0) =
            withBoolean(pOpen, index) { beginTabItem(label, it, flags) }

    /** create a Tab. Returns true if the Tab is selected. */
    fun beginTabItem(label: String, pOpen: KMutableProperty0<Boolean>? = null, flags: TabItemFlags = 0): Boolean {

        val window = g.currentWindow
        if (window.skipItems) return false

        val tabBar = g.currentTabBar
                ?: error("Needs to be called between BeginTabBar() and EndTabBar()!") // FIXME-ERRORHANDLING
        return tabBar.tabItemEx(label, pOpen, flags).also {
            if (it && flags hasnt TabItemFlag.NoPushId) {
                val tab = tabBar.tabs[tabBar.lastTabItemIdx]
                pushOverrideID(tab.id) // We already hashed 'label' so push into the ID stack directly instead of doing another hash through PushID(label)
            }
        }
    }

    /** only call EndTabItem() if BeginTabItem() returns true! */
    fun endTabItem() {

        val window = g.currentWindow
        if (window.skipItems) return

        val tabBar = g.currentTabBar
                ?: error("Needs to be called between BeginTabBar() and EndTabBar()!") // FIXME-ERRORHANDLING
        assert(tabBar.lastTabItemIdx >= 0)
        val tab = tabBar.tabs[tabBar.lastTabItemIdx]
        if (tab.flags hasnt TabItemFlag.NoPushId)
            window.idStack.pop()
    }

    /** notify TabBar or Docking system of a closed tab/window ahead (useful to reduce visual flicker on reorderable tab bars).
     *  For tab-bar: call after BeginTabBar() and before Tab submissions. Otherwise call with a window name.
     *  [Public] This is call is 100% optional but it allows to remove some one-frame glitches when a tab has been unexpectedly removed.
     *  To use it to need to call the function SetTabItemClosed() after BeginTabBar() and before any call to BeginTabItem() */
    fun setTabItemClosed(tabOrDockedWindowLabel: String) {

        val isWithinManualTabBar = g.currentTabBar?.flags?.hasnt(TabBarFlag.DockNode) == true
        if (isWithinManualTabBar) {
            val tabBar = g.currentTabBar!!
            assert(tabBar.wantLayout) { "Needs to be called AFTER BeginTabBar() and BEFORE the first call to BeginTabItem()" }
            val tabId = tabBar calcTabID tabOrDockedWindowLabel
            tabBar removeTab tabId
        }
    }

    companion object {

        fun offsetNormToPixels(columns: Columns, offsetNorm: Float) = offsetNorm * (columns.maxX - columns.minX)

        fun pixelsToOffsetNorm(columns: Columns, offset: Float) = offset / (columns.maxX - columns.minX)

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

        val TabBarRef.tabBar: TabBar?
            get() = ptr ?: g.tabBars[indexInMainPool]

        val TabBar.tabBarRef: TabBarRef?
            get() {
                if (this in g.tabBars)
                    return TabBarRef(g.tabBars.getIndex(this))
                return TabBarRef(this)
            }
    }
}