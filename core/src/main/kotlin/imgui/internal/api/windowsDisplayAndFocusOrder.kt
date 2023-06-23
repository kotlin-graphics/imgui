package imgui.internal.api

import imgui.*
import imgui.ImGui.findBlockingModal
import imgui.ImGui.isWithinBeginStackOf
import imgui.api.g
import imgui.api.gImGui
import imgui.internal.classes.FocusRequestFlag
import imgui.internal.classes.FocusRequestFlags
import imgui.internal.classes.Window
import imgui.internal.sections.IMGUI_DEBUG_LOG_FOCUS
import imgui.internal.sections.NavLayer
import imgui.statics.findWindowFocusIndex
import imgui.statics.navRestoreLastChildNavWindow

// Windows: Display Order and Focus Order
internal interface windowsDisplayAndFocusOrder {

    /** Moving window to front of display (which happens to be back of our sorted list)  ~ FocusWindow  */
    fun focusWindow(window_: Window? = null, flags: FocusRequestFlags = none) {

        var window = window_
        val g = gImGui

        // Modal check?
        if (flags has FocusRequestFlag.UnlessBelowModal)
            findBlockingModal(window)?.let { blockingModal ->
                IMGUI_DEBUG_LOG_FOCUS("[focus] FocusWindow(\"${window?.name ?: "<NULL>"}\", UnlessBelowModal): prevented by \"${blockingModal.name}\".\n")
                val wnd = window
                if (wnd != null && wnd === wnd.rootWindow && wnd.flags hasnt WindowFlag.NoBringToFrontOnFocus)
                    wnd bringToDisplayBehind blockingModal // Still bring to right below modal.
                return
            }

        // Find last focused child (if any) and focus it instead.
        if (flags has FocusRequestFlag.RestoreFocusedChild && window != null)
            window = navRestoreLastChildNavWindow(window)

        // Apply focus
        if (g.navWindow !== window) {
            ImGui.setNavWindow(window)
            if (window != null && g.navDisableMouseHover)
                g.navMousePosDirty = true
            g.navId = window?.navLastIds?.get(0) ?: 0 // Restore NavId
            g.navLayer = NavLayer.Main
            g.navFocusScopeId = window?.navRootFocusScopeId ?: 0
            g.navIdIsAlive = false

            // Close popups if any
            ImGui.closePopupsOverWindow(window, false)
        }

        // Move the root window to the top of the pile
        assert(window == null || window.rootWindow != null)
        val focusFrontWindow = window?.rootWindow // NB: In docking branch this is window->RootWindowDockStop
        val displayFrontWindow = window?.rootWindow

        // Steal active widgets. Some of the cases it triggers includes:
        // - Focus a window while an InputText in another window is active, if focus happens before the old InputText can run.
        // - When using Nav to activate menu items (due to timing of activating on press->new window appears->losing ActiveId)
        if (g.activeId != 0 && g.activeIdWindow?.rootWindow !== focusFrontWindow)
            if (!g.activeIdNoClearOnFocusLoss)
                ImGui.clearActiveID()

        // Passing NULL allow to disable keyboard focus
        if (window == null)
            return

        // Bring to front
        focusFrontWindow!!.bringToFocusFront()
        if ((window.flags or displayFrontWindow!!.flags) hasnt WindowFlag.NoBringToFrontOnFocus)
            displayFrontWindow.bringToDisplayFront()
    }

    // [JVM] default arguments to `null`
    fun focusTopMostWindowUnderOne(underThisWindow_: Window? = null, ignoreWindow: Window? = null, filterViewport: Viewport? = null, flags: FocusRequestFlags = none) {
        var underThisWindow = underThisWindow_
//        IM_UNUSED(filter_viewport); // Unused in master branch.
        var startIdx = g.windowsFocusOrder.lastIndex
        if (underThisWindow != null) {
            // Aim at root window behind us, if we are in a child window that's our own root (see #4640)
            var offset = -1
            while (underThisWindow!!.flags has WindowFlag._ChildWindow) {
                underThisWindow = underThisWindow.parentWindow
                offset = 0
            }
            startIdx = findWindowFocusIndex(underThisWindow) + offset
        }
        for (i in startIdx downTo 0) {
            // We may later decide to test for different NoXXXInputs based on the active navigation input (mouse vs nav) but that may feel more confusing to the user.
            val window = g.windowsFocusOrder[i]
            assert(window === window.rootWindow)
            if (window === ignoreWindow || !window.wasActive)
                continue
            if ((WindowFlag.NoMouseInputs or WindowFlag.NoNavInputs) !in window.flags) {
                focusWindow(window, flags)
                return
            }
        }
        focusWindow(flags = flags)
    }

    /** ~BringWindowToFocusFront */
    fun Window.bringToFocusFront() {
        assert(this === rootWindow)

        val curOrder = focusOrder
        assert(g.windowsFocusOrder[curOrder] === this)
        if (g.windowsFocusOrder.last() === this)
            return

        val newOrder = g.windowsFocusOrder.lastIndex
        for (n in curOrder until newOrder) {
            g.windowsFocusOrder[n] = g.windowsFocusOrder[n + 1]
            g.windowsFocusOrder[n].focusOrder--
            assert(g.windowsFocusOrder[n].focusOrder == n)
        }
        g.windowsFocusOrder[newOrder] = this
        focusOrder = newOrder
    }

    /** ~BringWindowToDisplayFront */
    fun Window.bringToDisplayFront() {
        val currentFrontWindow = g.windows.last()
        if (currentFrontWindow === this || currentFrontWindow.rootWindow === this) // Cheap early out (could be better)
            return
        for (i in g.windows.size - 2 downTo 0) // We can ignore the top-most window
            if (g.windows[i] === this) {
                g.windows.removeAt(i)
                g.windows += this
                break
            }
    }

    /** ~ BringWindowToDisplayBack */
    fun Window.bringToDisplayBack() {
        if (g.windows[0] === this) return
        for (i in 0 until g.windows.size) if (g.windows[i] === this) {
            g.windows.removeAt(i)
            g.windows.add(0, this)
        }
    }

    /** ~BringWindowToDisplayBehind */
    infix fun Window.bringToDisplayBehind(behindWindow_: Window) {
        //        IM_ASSERT(window != NULL && behind_window != NULL);
        val window = rootWindow!!
        val behindWindow = behindWindow_.rootWindow!!
        val posWnd = window.displayIndex
        val posBeh = behindWindow.displayIndex
        if (posWnd < posBeh) {
            g.windows.removeAt(posWnd)
            g.windows.add(posBeh - 1, window)
        } else {
            g.windows.remove(window)
            g.windows.add(posBeh, window)
        }
    }

    /** ~FindWindowDisplayIndex */
    val Window.displayIndex
        get() = g.windows.indexOf(this)

    fun Window.findBottomMostVisibleWindowWithinBeginStack(): Window {
        var bottomMostVisibleWindow = this
        for (i in displayIndex downTo 0) {
            val window = g.windows[i]
            if (window.flags has WindowFlag._ChildWindow)
                continue
            if (!window.isWithinBeginStackOf(this))
                break
            if (window.isActiveAndVisible && window.displayLayer <= parentWindow!!.displayLayer)
                bottomMostVisibleWindow = window
        }
        return bottomMostVisibleWindow
    }
}