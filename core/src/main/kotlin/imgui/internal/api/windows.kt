package imgui.internal.api

import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.markIniSettingsDirty
import imgui.api.g
import imgui.internal.classes.Rect
import imgui.internal.classes.ShrinkWidthItem
import imgui.internal.classes.Window
import imgui.internal.floor
import imgui.internal.hashStr
import imgui.WindowFlag as Wf


internal interface windows {

    // Windows

    /** We should always have a CurrentWindow in the stack (there is an implicit "Debug" window)
     *  If this ever crash because g.CurrentWindow is NULL it means that either
     *  - ImGui::NewFrame() has never been called, which is illegal.
     *  - You are calling ImGui functions after ImGui::EndFrame()/ImGui::Render() and before the next ImGui::NewFrame(), which is also illegal. */

    /** ~GetCurrentWindowRead */
    val currentWindowRead: Window?
        get() = g.currentWindow

    /** ~GetCurrentWindow */
    val currentWindow: Window
        get() = g.currentWindow!!.apply { writeAccessed = true }

    fun findWindowByID(id: ID): Window? = g.windowsById[id]

    fun findWindowByName(name: String): Window? = g.windowsById[hashStr(name)]

    /** ~UpdateWindowParentAndRootLinks */
    fun Window.updateParentAndRootLinks(flags: WindowFlags, parentWindow: Window?) {
        this.parentWindow = parentWindow
        rootWindow = this; rootWindowPopupTree = this; rootWindowForTitleBarHighlight = this; rootWindowForNav = this
        parentWindow?.let {
            if (flags has Wf._ChildWindow && flags hasnt Wf._Tooltip)
                rootWindow = it.rootWindow
            if (flags has Wf._Popup)
                rootWindowPopupTree = it.rootWindowPopupTree
            if (flags hasnt Wf._Modal && flags has (Wf._ChildWindow or Wf._Popup))
                rootWindowForTitleBarHighlight = it.rootWindowForTitleBarHighlight
        }
        while (rootWindowForNav!!.flags has Wf._NavFlattened)
            rootWindowForNav = rootWindowForNav!!.parentWindow!! // ~assert
    }

    /** ~CalcWindowNextAutoFitSize */
    fun Window.calcNextAutoFitSize(): Vec2 {
        val sizeContentsCurrent = Vec2()
        val sizeContentsIdeal = Vec2()
        calcContentSizes(sizeContentsCurrent, sizeContentsIdeal)
        val sizeAutoFit = calcAutoFitSize(sizeContentsIdeal)
        val sizeFinal = calcSizeAfterConstraint(sizeAutoFit)
        return sizeFinal
    }

    /** ~IsWindowChildOf */
    fun Window.isChildOf(potentialParent: Window?, popupHierarchy: Boolean): Boolean {
        val windowRoot = Window.getCombinedRootWindow(this, popupHierarchy)
        if (windowRoot === potentialParent)
            return true
        var window: Window? = this
        while (window != null) {
            if (window === potentialParent)
                return true
            if (window === windowRoot) // end of chain
                return false
            window = window.parentWindow
        }
        return false
    }

    /** ~IsWindowWithinBeginStackOf */
    infix fun Window.isWithinBeginStackOf(potentialParent: Window): Boolean {
        if (rootWindow === potentialParent)
            return true
        var window: Window? = this
        while (window != null) {
            if (window === potentialParent)
                return true
            window = window.parentWindowInBeginStack
        }
        return false
    }

    /** ~IsWindowAbove */
    infix fun Window.isAbove(potentialBelow: Window): Boolean {

        // It would be saner to ensure that display layer is always reflected in the g.Windows[] order, which would likely requires altering all manipulations of that array
        val displayLayerDelta = displayLayer - potentialBelow.displayLayer
        if (displayLayerDelta != 0)
            return displayLayerDelta > 0

        for (candidateWindow in g.windows.asReversed()) {
            if (candidateWindow === this)
                return true
            if (candidateWindow === potentialBelow)
                return false
        }
        return false
    }

    /** Can we focus this window with CTRL+TAB (or PadMenu + PadFocusPrev/PadFocusNext)
     *  Note that NoNavFocus makes the window not reachable with CTRL+TAB but it can still be focused with mouse or programmaticaly.
     *  If you want a window to never be focused, you may use the e.g. NoInputs flag.
     *  ~ IsWindowNavFocusable */
    val Window.isNavFocusable: Boolean
        get() = wasActive && this === rootWindow && flags hasnt Wf.NoNavFocus

    /** ~ SetWindowPos */
    fun Window.setPos(pos: Vec2, cond: Cond = Cond.None) { // Test condition (NB: bit 0 is always true) and clear flags for next time
        if (cond != Cond.None && setWindowPosAllowFlags hasnt cond)
            return //        JVM, useless
        //        assert(cond == Cond.None || cond.isPowerOfTwo) { "Make sure the user doesn't attempt to combine multiple condition flags." }
        setWindowPosAllowFlags = setWindowPosAllowFlags wo (Cond.Once or Cond.FirstUseEver or Cond.Appearing)
        setWindowPosVal put Float.MAX_VALUE

        // Set
        val oldPos = Vec2(this.pos)
        this.pos put floor(pos)
        val offset = this.pos - oldPos
        if (offset.x == 0f && offset.y == 0f)
            return
        markIniSettingsDirty()
        dc.cursorPos plusAssign offset         // As we happen to move the window while it is being appended to (which is a bad idea - will smear) let's at least offset the cursor
        dc.cursorMaxPos plusAssign offset      // And more importantly we need to offset CursorMaxPos/CursorStartPos this so ContentSize calculation doesn't get affected.
        dc.idealMaxPos += offset
        dc.cursorStartPos plusAssign offset
    }

    /** ~SetWindowSize */
    fun Window.setSize(size: Vec2, cond: Cond = Cond.None) { // Test condition (NB: bit 0 is always true) and clear flags for next time
        if (cond != Cond.None && setWindowSizeAllowFlags hasnt cond)
            return

        //        JVM, useless
        //        assert(cond == Cond.None || cond.isPowerOfTwo) { "Make sure the user doesn't attempt to combine multiple condition flags." }
        setWindowSizeAllowFlags = setWindowSizeAllowFlags wo (Cond.Once or Cond.FirstUseEver or Cond.Appearing)

        // Set
        val oldSize = Vec2(sizeFull)
        autoFitFrames.x = if (size.x <= 0f) 2 else 0
        autoFitFrames.y = if (size.y <= 0f) 2 else 0
        if (size.x <= 0f)
            autoFitOnlyGrows = false
        else
            sizeFull.x = floor(size.x)
        if (size.y <= 0f)
            autoFitOnlyGrows = false
        else
            sizeFull.y = floor(size.y)
        if (oldSize.x != sizeFull.x || oldSize.y != sizeFull.y)
            markIniSettingsDirty()
    }

    /** ~SetWindowCollapsed(ImGuiWindow* window, bool collapsed, ImGuiCond cond) */
    fun Window.setCollapsed(collapsed: Boolean, cond: Cond = Cond.None) { // Test condition (NB: bit 0 is always true) and clear flags for next time
        if (cond != Cond.None && setWindowCollapsedAllowFlags hasnt cond) return
        setWindowCollapsedAllowFlags = setWindowCollapsedAllowFlags wo (Cond.Once or Cond.FirstUseEver or Cond.Appearing) // Set
        this.collapsed = collapsed
    }

    /** ~SetWindowHitTestHole */
    fun Window.setHitTestHole(pos: Vec2, size: Vec2) {
        assert(hitTestHoleSize.x == 0) { "We don't support multiple holes/hit test filters" }
        hitTestHoleSize put size
        hitTestHoleOffset put (pos - this.pos)
    }

    /** ~WindowRectAbsToRel */
    infix fun Window.rectAbsToRel(r: Rect): Rect {
        val off = dc.cursorStartPos
        return Rect(r.min.x - off.x, r.min.y - off.y, r.max.x - off.x, r.max.y - off.y)
    }

    /** ~WindowRectRelToAbs */
    infix fun Window.rectRelToAbs(r: Rect): Rect {
        val off = dc.cursorStartPos
        return Rect(r.min.x + off.x, r.min.y + off.y, r.max.x + off.x, r.max.y + off.y)
    }

    companion object {
        val shrinkWidthItemComparer: Comparator<ShrinkWidthItem> = Comparator { a, b ->
            val d = (b.width - a.width).toInt()
            if (d != 0) d else b.index - a.index
        }
    }
}