package imgui.internal.api

import imgui.ImGui
import imgui.ImGui.isWithinBeginStackOf
import imgui.WindowFlag
import imgui.api.g
import imgui.has
import imgui.hasnt
import imgui.internal.classes.Window
import imgui.internal.sections.NavLayer
import imgui.static.findWindowFocusIndex
import imgui.static.navRestoreLastChildNavWindow

// Windows: Display Order and Focus Order
internal interface windowsDisplayAndFocusOrder {

    /** Moving window to front of display (which happens to be back of our sorted list)  ~ FocusWindow  */
    fun focusWindow(window: Window? = null) {

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

    fun focusTopMostWindowUnderOne(underThisWindow_: Window? = null, ignoreWindow: Window? = null) {
        var underThisWindow = underThisWindow_
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
            if (window !== ignoreWindow && window.wasActive)
                if ((window.flags and (WindowFlag.NoMouseInputs or WindowFlag.NoNavInputs)) != (WindowFlag.NoMouseInputs or WindowFlag.NoNavInputs)) {
                    focusWindow(navRestoreLastChildNavWindow(window))
                    return
                }
        }
        focusWindow()
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
            TODO()
            //            size_t copy_bytes =(posBeh - posWnd - 1) * sizeof(ImGuiWindow *)
            //            memmove(& g . Windows . Data [posWnd], &g.Windows.Data[pos_wnd+1], copy_bytes)
            //            g.Windows[posBeh - 1] = window
        } else {
            TODO()
            //            size_t copy_bytes =(posWnd - posBeh) * sizeof(ImGuiWindow *)
            //            memmove(& g . Windows . Data [posBeh + 1], &g.Windows.Data[pos_beh], copy_bytes)
            //            g.Windows[posBeh] = window
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