package imgui.api

import glm_.hasnt
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.currentWindow
import imgui.ImGui.currentWindowRead
import imgui.ImGui.findWindowByName
import imgui.ImGui.focusWindow
import imgui.ImGui.isChildOf
import imgui.ImGui.setCollapsed
import imgui.ImGui.setPos
import imgui.ImGui.setSize
import imgui.classes.DrawList
import imgui.internal.classes.Window.Companion.getCombinedRootWindow
import imgui.internal.floor
import imgui.internal.sections.NextWindowDataFlag
import imgui.internal.sections.div
import imgui.FocusedFlag as Ff
import imgui.HoveredFlag as Hf


/** Windows Utilities
 *  - 'current window' = the window we are appending into while inside a Begin()/End() block.
 *  'next window' = next window we will Begin() into.   */
interface windowsUtilities {

    val isWindowAppearing: Boolean
        get() = currentWindowRead!!.appearing

    val isWindowCollapsed: Boolean
        get() = currentWindowRead!!.collapsed

    /** is current window focused? or its root/child, depending on flags. see flags for options.    */
    fun isWindowFocused(flag: Ff): Boolean = isWindowFocused(flag.i)

    /** is current window focused? or its root/child, depending on flags. see flags for options.    */
    fun isWindowFocused(flags: FocusedFlags = Ff.None.i): Boolean {

        val refWindow = g.navWindow ?: return false
        var curWindow = g.currentWindow

        if (flags has Ff.AnyWindow)
            return true

        check(curWindow != null) { "Not inside a Begin() / End()" }
        val popupHierarchy = flags hasnt Ff.NoPopupHierarchy
        if (flags has Hf.RootWindow)
            curWindow = getCombinedRootWindow(curWindow, popupHierarchy)

        return when {
            flags has Hf.ChildWindows -> refWindow.isChildOf(curWindow, popupHierarchy)
            else -> refWindow === curWindow
        }
    }

    /** iis current window hovered (and typically: not blocked by a popup/modal)? see flag for options. */
    fun isWindowHovered(flag: Hf) = isWindowHovered(flag.i)

    /** Is current window hovered (and typically: not blocked by a popup/modal)? see flags for options.
     *  NB: If you are trying to check whether your mouse should be dispatched to imgui or to your app, you should use
     *  the 'io.wantCaptureMouse' boolean for that! Please read the FAQ!    */
    fun isWindowHovered(flags: HoveredFlags = Hf.None.i): Boolean {
        assert(flags hasnt (Hf.AllowWhenOverlapped or Hf.AllowWhenDisabled)) { "Flags not supported by this function" }
        val refWindow = g.hoveredWindow ?: return false
        var curWindow = g.currentWindow

        if (flags hasnt Hf.AnyWindow) {
            check(curWindow != null) { "Not inside a Begin () / End()" }
            val popupHierarchy = flags hasnt Hf.NoPopupHierarchy
            if (flags has Hf.RootWindow)
                curWindow = getCombinedRootWindow(curWindow, popupHierarchy)

            val result = when {
                flags has Hf.ChildWindows -> refWindow.isChildOf(curWindow, popupHierarchy)
                else -> refWindow === curWindow
            }
            if (!result)
                return false
        }

        if (!refWindow.isContentHoverable(flags))
            return false
        if (flags hasnt Hf.AllowWhenBlockedByActiveItem)
            if (g.activeId != 0 && !g.activeIdAllowOverlap && g.activeId != refWindow.moveId)
                return false
        return true
    }

    /** get draw list associated to the current window, to append your own drawing primitives
     *  ~GetWindowDrawList  */
    val windowDrawList: DrawList
        get() = currentWindow.drawList

    /** get current window position in screen space (useful if you want to do your own drawing via the DrawList api)
     *  ~GetWindowPos   */
    val windowPos: Vec2
        get() = g.currentWindow!!.pos

    /** get current window size
     *  ~GetWindowSize  */
    val windowSize: Vec2
        get() = currentWindowRead!!.size

    /** ~GetWindowWidth */
    val windowWidth: Float
        get() = g.currentWindow!!.size.x

    /** ~GetWindowHeight */
    val windowHeight: Float
        get() = g.currentWindow!!.size.y


    // -----------------------------------------------------------------------------------------------------------------
    // Window manipulation
    // - Prefer using SetNextXXX functions (before Begin) rather that SetXXX functions (after Begin).
    // -----------------------------------------------------------------------------------------------------------------

    /** set next window position. call before Begin()
     *  @param pos: safe, no writes */
    fun setNextWindowPos(pos: Vec2, cond: Cond = Cond.Always, pivot: Vec2 = Vec2()) {
        //        JVM, useless
        //        assert(cond == Cond.None || cond.isPowerOfTwo) { "Make sure the user doesn't attempt to combine multiple condition flags." }
        with(g.nextWindowData) {
            flags /= NextWindowDataFlag.HasPos
            posVal put pos
            posPivotVal put pivot
            posCond = cond
        }
    }

    /** set next window size. set axis to 0.0f to force an auto-fit on this axis. call before Begin()
     *  @param size: safe, no writes */
    fun setNextWindowSize(size: Vec2, cond: Cond = Cond.Always) {
        //        JVM, useless
        //        assert(cond == Cond.None || cond.isPowerOfTwo) { "Make sure the user doesn't attempt to combine multiple condition flags." }
        with(g.nextWindowData) {
            flags /= NextWindowDataFlag.HasSize
            sizeVal put size
            sizeCond = cond
        }
    }

    /** set next window size limits. use -1,-1 on either X/Y axis to preserve the current size.
     *  Sizes will be rounded down. Use callback to apply non-trivial programmatic constraints.   */
    fun setNextWindowSizeConstraints(sizeMin: Vec2, sizeMax: Vec2, customCallback: SizeCallback? = null, customCallbackUserData: Any? = null) {
        with(g.nextWindowData) {
            flags /= NextWindowDataFlag.HasSizeConstraint
            sizeConstraintRect.min put sizeMin
            sizeConstraintRect.max put sizeMax
            sizeCallback = customCallback
            sizeCallbackUserData = customCallbackUserData
        }
    }

    /** set next window content size (~ scrollable client area, which enforce the range of scrollbars).
     *  Not including window decorations (title bar, menu bar, etc.) nor WindowPadding. set an axis to 0.0f to leave it automatic. call before Begin()
     *
     *  Content size = inner scrollable rectangle, padded with WindowPadding.
     *  SetNextWindowContentSize(ImVec2(100,100) + ImGuiWindowFlags_AlwaysAutoResize will always allow submitting a 100x100 item.*/
    fun setNextWindowContentSize(size: Vec2) {
        with(g.nextWindowData) {
            flags /= NextWindowDataFlag.HasContentSize
            contentSizeVal put floor(size)
        }
    }

    /** Set next window collapsed state. call before Begin()    */
    fun setNextWindowCollapsed(collapsed: Boolean, cond: Cond = Cond.Always) {
        //        JVM, useless
        //        assert(cond == Cond.None || cond.isPowerOfTwo) { "Make sure the user doesn't attempt to combine multiple condition flags." }
        with(g.nextWindowData) {
            flags /= NextWindowDataFlag.HasCollapsed
            collapsedVal = collapsed
            collapsedCond = cond
        }
    }

    /** Set next window to be focused / top-most. call before Begin() */
    fun setNextWindowFocus() {
        // Using a Cond member for consistency (may transition all of them to single flag set for fast Clear() op)
        g.nextWindowData.flags /= NextWindowDataFlag.HasFocus
    }

    /** set next window scrolling value (use < 0.0f to not affect a given axis). */
    fun setNextWindowScroll(scroll: Vec2) {
        g.nextWindowData.flags /= NextWindowDataFlag.HasScroll
        g.nextWindowData.scrollVal put scroll
    }


    /** Set next window background color alpha. helper to easily override the Alpha component of ImGuiCol_WindowBg/ChildBg/PopupBg.
     *  You may also use ImGuiWindowFlags_NoBackground. */
    fun setNextWindowBgAlpha(alpha: Float) {
        g.nextWindowData.apply {
            flags /= NextWindowDataFlag.HasBgAlpha
            bgAlphaVal = alpha
        }
    }

    /** (not recommended) set current window position - call within Begin()/End(). prefer using SetNextWindowPos(),
     *  as this may incur tearing and side-effects. */
    fun setWindowPos(pos: Vec2, cond: Cond = Cond.None) = currentWindowRead!!.setPos(pos, cond)

    /** (not recommended) set current window size - call within Begin()/End(). set to ImVec2(0, 0) to force an auto-fit.
     *  prefer using SetNextWindowSize(), as this may incur tearing and minor side-effects. */
    fun setWindowSize(size: Vec2, cond: Cond = Cond.None) = g.currentWindow!!.setSize(size, cond)

    /** (not recommended) set current window collapsed state. prefer using SetNextWindowCollapsed().    */
    fun setWindowCollapsed(collapsed: Boolean, cond: Cond = Cond.None) = g.currentWindow!!.setCollapsed(collapsed, cond)

    /** (not recommended) set current window to be focused / top-most. prefer using SetNextWindowFocus(). */
    fun setWindowFocus() = focusWindow(g.currentWindow)

    /** per-window font scale. Adjust io.FontGlobalScale if you want to scale all windows
     *
     *  [OBSOLETE] set font scale. Adjust IO.FontGlobalScale if you want to scale all windows. This is an old API! For correct scaling, prefer to reload font + rebuild ImFontAtlas + call style.ScaleAllSizes(). */
    fun setWindowFontScale(scale: Float) = with(currentWindow) {
        assert(scale > 0f)
        fontWindowScale = scale
        g.fontSize = calcFontSize()
        g.drawListSharedData.fontSize = g.fontSize
    }

    /** Set named window position.  */
    fun setWindowPos(name: String, pos: Vec2, cond: Cond = Cond.None) = findWindowByName(name)?.setPos(pos, cond)

    /** Set named window size. set axis to 0.0f to force an auto-fit on this axis.  */
    fun setWindowSize(name: String, size: Vec2, cond: Cond = Cond.None) = findWindowByName(name)?.setSize(size, cond)

    /** Set named window collapsed state    */
    fun setWindowCollapsed(name: String, collapsed: Boolean, cond: Cond = Cond.None) = findWindowByName(name)?.setCollapsed(collapsed, cond)

    /** Set named window to be focused / top-most. use NULL to remove focus.  */
    fun setWindowFocus(name: String) = focusWindow(findWindowByName(name))
}