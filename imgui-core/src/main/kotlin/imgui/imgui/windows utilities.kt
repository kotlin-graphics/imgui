package imgui.imgui

import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.currentWindow
import imgui.ImGui.currentWindowRead
import imgui.ImGui.findWindowByName
import imgui.internal.*
import imgui.FocusedFlag as Ff
import imgui.HoveredFlag as Hf
import imgui.WindowFlag as Wf
import imgui.internal.ButtonFlag as Bf
import imgui.internal.DrawCornerFlag as Dcf
import imgui.internal.DrawListFlag as Dlf
import imgui.internal.ItemFlag as If
import imgui.internal.LayoutType as Lt


/** Windows Utilities
 *  - "current window" = the window we are appending into while inside a Begin()/End() block.
 *  "next window" = next window we will Begin() into.   */
interface imgui_windowsUtilities {

    val isWindowAppearing: Boolean
        get() = currentWindowRead!!.appearing

    val isWindowCollapsed: Boolean
        get() = currentWindowRead!!.collapsed

    /** is current window focused? or its root/child, depending on flags. see flags for options.    */
    fun isWindowFocused(flag: Ff) = isWindowFocused(flag.i)

    /** is current window focused? or its root/child, depending on flags. see flags for options.    */
    fun isWindowFocused(flags: FocusedFlags = Ff.None.i): Boolean {

        if (flags has Ff.AnyWindow)
            return g.navWindow != null

        val curr = g.currentWindow!!     // Not inside a Begin()/End()
        return when (flags and (Ff.RootWindow or Ff.ChildWindows)) {
            Ff.RootWindow or Ff.ChildWindows -> g.navWindow?.let { it.rootWindow === curr.rootWindow } ?: false
            Ff.RootWindow.i -> g.navWindow === curr.rootWindow
            Ff.ChildWindows.i -> g.navWindow?.isChildOf(curr) ?: false
            else -> g.navWindow === curr
        }
    }

    /** iis current window hovered (and typically: not blocked by a popup/modal)? see flag for options. */
    fun isWindowHovered(flag: Hf) = isWindowHovered(flag.i)

    /** Is current window hovered (and typically: not blocked by a popup/modal)? see flags for options.
     *  NB: If you are trying to check whether your mouse should be dispatched to imgui or to your app, you should use
     *  the 'io.wantCaptureMouse' boolean for that! Please read the FAQ!    */
    fun isWindowHovered(flags: HoveredFlags = Hf.None.i): Boolean {
        assert(flags hasnt Hf.AllowWhenOverlapped) { "Flags not supported by this function" }
        if (flags has Hf.AnyWindow) {
            if (g.hoveredWindow == null)
                return false
        } else when (flags and (Hf.RootWindow or Hf.ChildWindows)) {
            Hf.RootWindow or Hf.ChildWindows -> if (g.hoveredRootWindow !== g.currentWindow!!.rootWindow) return false
            Hf.RootWindow.i -> if (g.hoveredWindow != g.currentWindow!!.rootWindow) return false
            Hf.ChildWindows.i -> g.hoveredWindow.let { if (it == null || !it.isChildOf(g.currentWindow)) return false }
            else -> if (g.hoveredWindow !== g.currentWindow) return false
        }

        return when {
            !g.hoveredWindow!!.isContentHoverable(flags) -> false
            flags hasnt Hf.AllowWhenBlockedByActiveItem && g.activeId != 0 && !g.activeIdAllowOverlap && g.activeId != g.hoveredWindow!!.moveId -> false
            else -> true
        }
    }

    /** get draw list associated to the current window, to append your own drawing primitives   */
    val windowDrawList: DrawList
        get() = currentWindow.drawList

    /** get current window position in screen space (useful if you want to do your own drawing via the DrawList api)    */
    val windowPos: Vec2
        get() = g.currentWindow!!.pos

    /** get current window size */
    val windowSize: Vec2
        get() = currentWindowRead!!.size

    val windowWidth: Float
        get() = g.currentWindow!!.size.x

    val windowHeight: Float
        get() = g.currentWindow!!.size.y

    // Prefer using SetNextXXX functions (before Begin) rather that SetXXX functions (after Begin).

    /** set next window position. call before Begin()   */
    fun setNextWindowPos(pos: Vec2, cond: Cond = Cond.Always, pivot: Vec2 = Vec2()) {
//        JVM, useless
//        assert(cond == Cond.None || cond.isPowerOfTwo) { "Make sure the user doesn't attempt to combine multiple condition flags." }
        with(g.nextWindowData) {
            flags = flags or NextWindowDataFlag.HasPos
            posVal put pos
            posPivotVal put pivot
            posCond = cond
        }
    }

    /** set next window size. set axis to 0.0f to force an auto-fit on this axis. call before Begin()   */
    fun setNextWindowSize(size: Vec2, cond: Cond = Cond.Always) {
//        JVM, useless
//        assert(cond == Cond.None || cond.isPowerOfTwo) { "Make sure the user doesn't attempt to combine multiple condition flags." }
        with(g.nextWindowData) {
            flags = flags or NextWindowDataFlag.HasSize
            sizeVal put size
            sizeCond = cond
        }
    }

    /** set next window size limits. use -1,-1 on either X/Y axis to preserve the current size.
     *  Sizes will be rounded down. Use callback to apply non-trivial programmatic constraints.   */
    fun setNextWindowSizeConstraints(sizeMin: Vec2, sizeMax: Vec2, customCallback: SizeCallback? = null, customCallbackUserData: Any? = null) {
        with(g.nextWindowData) {
            flags = flags or NextWindowDataFlag.HasSizeConstraint
            sizeConstraintRect.min put sizeMin
            sizeConstraintRect.max put sizeMax
            sizeCallback = customCallback
            sizeCallbackUserData = customCallbackUserData
        }
    }

    /** Set next window content size (~ enforce the range of scrollbars). not including window decorations (title bar, menu bar, etc.).
     *  set an axis to 0.0f to leave it automatic. call before Begin() */
    fun setNextWindowContentSize(size: Vec2) {
        // In Begin() we will add the size of window decorations (title bar, menu etc.) to that to form a SizeContents value.
        with(g.nextWindowData) {
            flags = flags or NextWindowDataFlag.HasContentSize
            contentSizeVal put size
        }
    }

    /** Set next window collapsed state. call before Begin()    */
    fun setNextWindowCollapsed(collapsed: Boolean, cond: Cond = Cond.Always) {
//        JVM, useless
//        assert(cond == Cond.None || cond.isPowerOfTwo) { "Make sure the user doesn't attempt to combine multiple condition flags." }
        with(g.nextWindowData) {
            flags = flags or NextWindowDataFlag.HasCollapsed
            collapsedVal = collapsed
            collapsedCond = cond
        }
    }

    /** Set next window to be focused / front-most. call before Begin() */
    fun setNextWindowFocus() {
        // Using a Cond member for consistency (may transition all of them to single flag set for fast Clear() op)
        g.nextWindowData.flags = g.nextWindowData.flags or NextWindowDataFlag.HasFocus
    }

    /** Set next window background color alpha. helper to easily modify ImGuiCol_WindowBg/ChildBg/PopupBg.
     *  You may also use ImGuiWindowFlags_NoBackground. */
    fun setNextWindowBgAlpha(alpha: Float) {
        g.nextWindowData.apply {
            flags = flags or NextWindowDataFlag.HasBgAlpha
            bgAlphaVal = alpha
        }
    }

    /** (not recommended) set current window position - call within Begin()/End(). prefer using SetNextWindowPos(),
     *  as this may incur tearing and side-effects. */
    fun setWindowPos(pos: Vec2, cond: Cond = Cond.None) = currentWindowRead!!.setPos(pos, cond)

    /** (not recommended) set current window size - call within Begin()/End(). set to ImVec2(0,0) to force an auto-fit.
     *  prefer using SetNextWindowSize(), as this may incur tearing and minor side-effects. */
    fun setWindowSize(size: Vec2, cond: Cond = Cond.None) = g.currentWindow!!.setSize(size, cond)

    /** (not recommended) set current window collapsed state. prefer using SetNextWindowCollapsed().    */
    fun setWindowCollapsed(collapsed: Boolean, cond: Cond = Cond.None) = g.currentWindow!!.setCollapsed(collapsed, cond)

    /** (not recommended) set current window to be focused / front-most. prefer using SetNextWindowFocus(). */
    fun setWindowFocus() = g.currentWindow.focus()

    /** Set named window position.  */
    fun setWindowPos(name: String, pos: Vec2, cond: Cond = Cond.None) = findWindowByName(name)?.setPos(pos, cond)

    /** Set named window size. set axis to 0.0f to force an auto-fit on this axis.  */
    fun setWindowSize(name: String, size: Vec2, cond: Cond = Cond.None) = findWindowByName(name)?.setSize(size, cond)

    /** Set named window collapsed state    */
    fun setWindowCollapsed(name: String, collapsed: Boolean, cond: Cond = Cond.None) = findWindowByName(name)?.setCollapsed(collapsed, cond)

    /** Set named window to be focused / front-most. use NULL to remove focus.  */
    fun setWindowFocus(name: String) = findWindowByName(name).focus()
}