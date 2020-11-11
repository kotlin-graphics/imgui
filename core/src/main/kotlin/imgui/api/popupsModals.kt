package imgui.api

import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.beginPopupEx
import imgui.ImGui.closePopupToLevel
import imgui.ImGui.currentWindow
import imgui.ImGui.end
import imgui.ImGui.io
import imgui.ImGui.isAnyItemHovered
import imgui.ImGui.isItemHovered
import imgui.ImGui.isMouseReleased
import imgui.ImGui.isPopupOpen
import imgui.ImGui.isWindowHovered
import imgui.ImGui.navMoveRequestTryWrapping
import imgui.ImGui.openPopupEx
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.topMostPopupModal
import imgui.internal.sections.IMGUI_DEBUG_LOG_POPUP
import imgui.internal.sections.NavMoveFlag
import imgui.internal.sections.NextWindowDataFlag
import imgui.internal.sections.hasnt
import kotlin.reflect.KMutableProperty0
import imgui.HoveredFlag as Hf
import imgui.WindowFlag as Wf

/** Popups, Modals */
interface popupsModals {


    //  - They block normal mouse hovering detection (and therefore most mouse interactions) behind them.
    //  - If not modal: they can be closed by clicking anywhere outside them, or by pressing ESCAPE.
    //  - Their visibility state (~bool) is held internally instead of being held by the programmer as we are used to with regular Begin*() calls.
    //  - The 3 properties above are related: we need to retain popup visibility state in the library because popups may be closed as any time.
    //  - You can bypass the hovering restriction by using ImGuiHoveredFlags_AllowWhenBlockedByPopup when calling IsItemHovered() or IsWindowHovered().
    //  - IMPORTANT: Popup identifiers are relative to the current ID stack, so OpenPopup and BeginPopup generally needs to be at the same level of the stack.
    //    This is sometimes leading to confusing mistakes. May rework this in the future.
    // Popups: begin/end functions
    //  - BeginPopup(): query popup state, if open start appending into the window. Call EndPopup() afterwards. ImGuiWindowFlags are forwarded to the window.
    //  - BeginPopupModal(): block every interactions behind the window, cannot be closed by user, add a dimming background, has a title bar.


    /** return true if the popup is open, and you can start outputting to it. */
    fun beginPopup(strId: String, flags_: WindowFlags = Wf.None.i): Boolean {
        if (g.openPopupStack.size <= g.beginPopupStack.size) {    // Early out for performance
            g.nextWindowData.clearFlags()    // We behave like Begin() and need to consume those values
            return false
        }
        val flags = flags_ or Wf.AlwaysAutoResize or Wf.NoTitleBar or Wf.NoSavedSettings
        return beginPopupEx(g.currentWindow!!.getID(strId), flags)
    }

    /** modal dialog (block interactions behind the modal window, can't close the modal window by clicking outside)
     *
     *  If 'p_open' is specified for a modal popup window, the popup will have a regular close button which will close the popup.
     *  Note that popup visibility status is owned by Dear ImGui (and manipulated with e.g. OpenPopup) so the actual value of *p_open is meaningless here.   */
    fun beginPopupModal(name: String, pOpen: BooleanArray, flags_: WindowFlags = 0): Boolean =
            withBoolean(pOpen) { beginPopupModal(name, it, flags_) }

    /** modal dialog (block interactions behind the modal window, can't close the modal window by clicking outside)
     *
     *  If 'p_open' is specified for a modal popup window, the popup will have a regular close button which will close the popup.
     *  Note that popup visibility status is owned by Dear ImGui (and manipulated with e.g. OpenPopup) so the actual value of *p_open is meaningless here.   */
    fun beginPopupModal(name: String, pOpen: KMutableProperty0<Boolean>? = null, flags_: WindowFlags = 0): Boolean {

        val window = g.currentWindow!!
        val id = window.getID(name)
        if (!isPopupOpen(id)) {
            g.nextWindowData.clearFlags() // We behave like Begin() and need to consume those values
            return false
        }

        // Center modal windows by default for increased visibility
        // (this won't really last as settings will kick in, and is mostly for backward compatibility. user may do the same themselves)
        // FIXME: Should test for (PosCond & window->SetWindowPosAllowFlags) with the upcoming window.
        if (g.nextWindowData.flags hasnt NextWindowDataFlag.HasPos)
            setNextWindowPos(Vec2(io.displaySize.x * 0.5f, io.displaySize.y * 0.5f), Cond.FirstUseEver, Vec2(0.5f))

        val flags = flags_ or Wf._Popup or Wf._Modal or Wf.NoCollapse
        val isOpen = begin(name, pOpen, flags)
        // NB: isOpen can be 'false' when the popup is completely clipped (e.g. zero size display)
        if (!isOpen || pOpen?.get() == false) {
            endPopup()
            if (isOpen)
                closePopupToLevel(g.beginPopupStack.size, true)
            return false
        }
        return isOpen
    }

    /** Only call EndPopup() if BeginPopupXXX() returns true!   */
    fun endPopup() {
        val window = g.currentWindow!!
        assert(window.flags has Wf._Popup) { "Mismatched BeginPopup()/EndPopup() calls" }
        assert(g.beginPopupStack.isNotEmpty())

        // Make all menus and popups wrap around for now, may need to expose that policy.
        if (g.navWindow === window)
            navMoveRequestTryWrapping(window, NavMoveFlag.LoopY.i)

        // Child-popups don't need to be laid out
        assert(!g.withinEndChild)
        if (window.flags has Wf._ChildWindow)
            g.withinEndChild = true
        end()
        g.withinEndChild = false
    }


    // Popups: open/close functions
    //  - OpenPopup(): set popup state to open. ImGuiPopupFlags are available for opening options.
    //  - If not modal: they can be closed by clicking anywhere outside them, or by pressing ESCAPE.
    //  - CloseCurrentPopup(): use inside the BeginPopup()/EndPopup() scope to close manually.
    //  - CloseCurrentPopup() is called by default by Selectable()/MenuItem() when activated (FIXME: need some options).
    //  - Use ImGuiPopupFlags_NoOpenOverExistingPopup to avoid opening a popup if there's already one at the same level. This is equivalent to e.g. testing for !IsAnyPopupOpen() prior to OpenPopup().


    /** call to mark popup as open (don't call every frame!). */
    fun openPopup(strId: String, popupFlags: PopupFlags = PopupFlag.None.i) = openPopupEx(g.currentWindow!!.getID(strId), popupFlags)

    /** helper to open popup when clicked on last item. return true when just opened.
     *  (note: actually triggers on the mouse _released_ event to be consistent with popup behaviors)
     *
     *  Helper to open a popup if mouse button is released over the item
     *  - This is essentially the same as BeginPopupContextItem() but without the trailing BeginPopup() */
    fun openPopupOnItemClick(strId: String = "", popupFlags: PopupFlags = PopupFlag.MouseButtonRight.i) =
            with(g.currentWindow!!) {
                val mouseButton = popupFlags and PopupFlag.MouseButtonMask_
                if (isMouseReleased(mouseButton) && isItemHovered(Hf.AllowWhenBlockedByPopup)) {
                    // If user hasn't passed an ID, we can use the LastItemID. Using LastItemID as a Popup ID won't conflict!
                    val id = if (strId.isNotEmpty()) getID(strId) else dc.lastItemId
                    assert(id != 0) { "You cannot pass a NULL str_id if the last item has no identifier (e.g. a Text() item)" }
                    openPopupEx(id, popupFlags)
                }
            }

    /** cmanually close the popup we have begin-ed into.  */
    fun closeCurrentPopup() {

        var popupIdx = g.beginPopupStack.lastIndex
        if (popupIdx < 0 || popupIdx >= g.openPopupStack.size || g.beginPopupStack[popupIdx].popupId != g.openPopupStack[popupIdx].popupId)
            return
        // Closing a menu closes its top-most parent popup (unless a modal)
        while (popupIdx > 0) {
            val popupWindow = g.openPopupStack[popupIdx].window
            val parentPopupWindow = g.openPopupStack[popupIdx - 1].window
            var closeParent = false
            if (popupWindow?.flags?.has(Wf._ChildMenu) == true)
                if (parentPopupWindow == null || parentPopupWindow.flags hasnt Wf._Modal)
                    closeParent = true
            if (!closeParent)
                break
            popupIdx--
        }
        IMGUI_DEBUG_LOG_POPUP("CloseCurrentPopup ${g.beginPopupStack.lastIndex} -> $popupIdx")
        closePopupToLevel(popupIdx, true)

        /*  A common pattern is to close a popup when selecting a menu item/selectable that will open another window.
            To improve this usage pattern, we avoid nav highlight for a single frame in the parent window.
            Similarly, we could avoid mouse hover highlight in this window but it is less visually problematic. */
        g.navWindow?.dc?.navHideHighlightOneFrame = true
    }

    /** Return true if the popup is open at the current BeginPopup() level of the popup stack */
    fun isPopupOpen(strId: String) = g.openPopupStack.size > g.beginPopupStack.size &&
            g.openPopupStack[g.beginPopupStack.size].popupId == g.currentWindow!!.getID(strId)


    // Popups: open+begin combined functions helpers
    //  - Helpers to do OpenPopup+BeginPopup where the Open action is triggered by e.g. hovering an item and right-clicking.
    //  - They are convenient to easily create context menus, hence the name.
    //  - IMPORTANT: Notice that BeginPopupContextXXX takes ImGuiPopupFlags just like OpenPopup() and unlike BeginPopup(). For full consistency, we may add ImGuiWindowFlags to the BeginPopupContextXXX functions in the future.
    //  - IMPORTANT: we exceptionally default their flags to 1 (== ImGuiPopupFlags_MouseButtonRight) for backward compatibility with older API taking 'int mouse_button = 1' parameter, so if you add other flags remember to re-add the ImGuiPopupFlags_MouseButtonRight.


    /** This is a helper to handle the simplest case of associating one named popup to one given widget.
     *  - You can pass a NULL str_id to use the identifier of the last item.
     *  - You may want to handle this on user side if you have specific needs (e.g. tweaking IsItemHovered() parameters).
     *  - This is essentially the same as calling OpenPopupOnItemClick() + BeginPopup() but written to avoid
     *    computing the ID twice because BeginPopupContextXXX functions may be called very frequently.
     *
     *  open+begin popup when clicked on last item. if you can pass a NULL str_id only if the previous item had an id.
     *  If you want to use that on a non-interactive item such as Text() you need to pass in an explicit ID here.
     *  read comments in .cpp! */
    fun beginPopupContextItem(strId: String = "", popupFlags: PopupFlags = PopupFlag.MouseButtonRight.i): Boolean {
        val window = currentWindow
        if (window.skipItems) return false
        // If user hasn't passed an id, we can use the lastItemID. Using lastItemID as a Popup id won't conflict!
        val id = if (strId.isNotEmpty()) window.getID(strId) else window.dc.lastItemId
        assert(id != 0) { "You cannot pass a NULL str_id if the last item has no identifier (e.g. a text() item)" }
        val mouseButton = popupFlags and PopupFlag.MouseButtonMask_
        if (isMouseReleased(mouseButton) && isItemHovered(Hf.AllowWhenBlockedByPopup))
            openPopupEx(id, popupFlags)
        return beginPopupEx(id, Wf.AlwaysAutoResize or Wf.NoTitleBar or Wf.NoSavedSettings)
    }

    /** Helper to open and begin popup when clicked on current window.
     *
     *  open+begin popup when clicked on current window.*/
    fun beginPopupContextWindow(strId: String = "", popupFlags: PopupFlags = PopupFlag.MouseButtonRight.i): Boolean {
        val window = g.currentWindow!!
        val id = window.getID(if (strId.isEmpty()) "window_context" else strId)
        val mouseButton = popupFlags and PopupFlag.MouseButtonMask_
        if (isMouseReleased(mouseButton) && isWindowHovered(Hf.AllowWhenBlockedByPopup))
            if (popupFlags hasnt PopupFlag.NoOpenOverItems || !isAnyItemHovered)
                openPopupEx(id, popupFlags)
        return beginPopupEx(id, Wf.AlwaysAutoResize or Wf.NoTitleBar or Wf.NoSavedSettings)
    }

    /** helper to open and begin popup when clicked in void (where there are no imgui windows).
     *
     *  open+begin popup when clicked in void (where there are no windows). */
    fun beginPopupContextVoid(strId: String = "", popupFlags: PopupFlags = PopupFlag.MouseButtonRight.i): Boolean {
        val window = g.currentWindow!!
        val id = window.getID(if (strId.isEmpty()) "window_context" else strId)
        val mouseButton = popupFlags and PopupFlag.MouseButtonMask_
        if (isMouseReleased(mouseButton) && !isWindowHovered(Hf.AnyWindow))
            if (topMostPopupModal == null)
                openPopupEx(id, popupFlags)
        return beginPopupEx(id, Wf.AlwaysAutoResize or Wf.NoTitleBar or Wf.NoSavedSettings)
    }


    // Popups: test function
    //  - IsPopupOpen(): return true if the popup is open at the current BeginPopup() level of the popup stack.
    //  - IsPopupOpen() with ImGuiPopupFlags_AnyPopupId: return true if any popup is open at the current BeginPopup() level of the popup stack.
    //  - IsPopupOpen() with ImGuiPopupFlags_AnyPopupId + ImGuiPopupFlags_AnyPopupLevel: return true if any popup is open.


    /** return true if the popup is open. */
    fun isPopupOpen(strId: String, popupFlags: PopupFlags = PopupFlag.None.i): Boolean {
        val id = if (popupFlags has PopupFlag.AnyPopupId) 0 else g.currentWindow!!.getID(strId)
        if (popupFlags has PopupFlag.AnyPopupLevel && id != 0)
            assert(false) { "Cannot use IsPopupOpen() with a string id and ImGuiPopupFlags_AnyPopupLevel." } // But non-string version is legal and used internally
        return isPopupOpen(id, popupFlags)
    }
}