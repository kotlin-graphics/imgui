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
import imgui.ImGui.mainViewport
import imgui.ImGui.navMoveRequestTryWrapping
import imgui.ImGui.openPopupEx
import imgui.ImGui.setNextWindowPos
import imgui.classes.ViewportP
import imgui.internal.sections.NavMoveFlag
import imgui.internal.sections.NextWindowDataFlag
import imgui.internal.sections.hasnt
import kotlin.reflect.KMutableProperty0
import imgui.HoveredFlag as Hf
import imgui.WindowFlag as Wf

/** Popups, Modals
 *  The properties of popups windows are:
 *  - They block normal mouse hovering detection outside them. (*1)
 *  - Unless modal, they can be closed by clicking anywhere outside them, or by pressing ESCAPE.
 *    Because hovering detection is disabled outside the popup, when clicking outside the click will not be seen by underlying widgets! (*1)
 *  - Their visibility state (~bool) is held internally by Dear ImGui instead of being held by the programmer as we are used to with regular Begin() calls.
 *    User can manipulate the visibility state by calling OpenPopup(), CloseCurrentPopup() etc.
 *  - We default to use the right mouse (ImGuiMouseButton_Right=1) for the Popup Context functions.
 *  Those three properties are connected: we need to retain popup visibility state in the library because popups may be closed as any time.
 *  (*1) You can bypass that restriction and detect hovering even when normally blocked by a popup.
 *      To do this use the ImGuiHoveredFlags_AllowWhenBlockedByPopup when calling IsItemHovered() or IsWindowHovered().
 *      This is what BeginPopupContextItem() and BeginPopupContextWindow() are doing already, allowing a right-click to reopen another popups without losing the click. */
interface popupsModals {

    /** call to mark popup as open (don't call every frame!). popups are closed when user click outside, or if
     *  CloseCurrentPopup() is called within a BeginPopup()/EndPopup() block. By default, Selectable()/MenuItem() are
     *  calling CloseCurrentPopup(). Popup identifiers are relative to the current ID-stack (so OpenPopup and BeginPopup
     *  needs to be at the same level).   */
    fun openPopup(strId: String) = openPopupEx(g.currentWindow!!.getID(strId))

    /** return true if the popup is open, and you can start outputting to it. only call EndPopup() if BeginPopup() returns true!    */
    fun beginPopup(strId: String, flags_: WindowFlags = 0): Boolean {
        if (g.openPopupStack.size <= g.beginPopupStack.size) {    // Early out for performance
            g.nextWindowData.clearFlags()    // We behave like Begin() and need to consume those values
            return false
        }
        val flags = flags_ or Wf.AlwaysAutoResize or Wf.NoTitleBar or Wf.NoSavedSettings
        return beginPopupEx(g.currentWindow!!.getID(strId), flags)
    }

    /** This is a helper to handle the simplest case of associating one named popup to one given widget.
     *  You may want to handle this on user side if you have specific needs (e.g. tweaking IsItemHovered() parameters).
     *  You can pass a NULL str_id to use the identifier of the last item.
     *  helper to open and begin popup when clicked on last item. if you can pass an empty strId only if the previous
     *  item had an id. If you want to use that on a non-interactive item such as text() you need to pass in an explicit
     *  id here. read comments in .cpp! */
    fun beginPopupContextItem(strId: String = "", mouseButton: MouseButton = MouseButton.Right): Boolean {
        val window = currentWindow
        if (window.skipItems) return false
        // If user hasn't passed an id, we can use the lastItemID. Using lastItemID as a Popup id won't conflict!
        val id = if (strId.isNotEmpty()) window.getID(strId) else window.dc.lastItemId
        assert(id != 0) { "You cannot pass a NULL str_id if the last item has no identifier (e.g. a text() item)" }
        if (isMouseReleased(mouseButton) && isItemHovered(Hf.AllowWhenBlockedByPopup))
            openPopupEx(id)
        return beginPopupEx(id, Wf.AlwaysAutoResize or Wf.NoTitleBar or Wf.NoSavedSettings)
    }

    /** Helper to open and begin popup when clicked on current window.  */
    fun beginPopupContextWindow(strId: String = "", mouseButton: MouseButton = MouseButton.Right
                                , alsoOverItems: Boolean = true): Boolean {
        val id = currentWindow.getID(if (strId.isEmpty()) "window_context" else strId)
        if (isMouseReleased(mouseButton) && isWindowHovered(Hf.AllowWhenBlockedByPopup))
            if (alsoOverItems || !isAnyItemHovered)
                openPopupEx(id)
        return beginPopupEx(id, Wf.AlwaysAutoResize or Wf.NoTitleBar or Wf.NoSavedSettings)
    }

    /** helper to open and begin popup when clicked in void (where there are no imgui windows). */
    fun beginPopupContextVoid(strId: String = "", mouseButton: MouseButton = MouseButton.Right): Boolean {
        val id = currentWindow.getID(if (strId.isEmpty()) "window_context" else strId)
        if (isMouseReleased(mouseButton) && !isWindowHovered(Hf.AnyWindow))
            openPopupEx(id)
        return beginPopupEx(id, Wf.AlwaysAutoResize or Wf.NoTitleBar or Wf.NoSavedSettings)
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
        // Center modal windows by default
        // FIXME: Should test for (PosCond & window->SetWindowPosAllowFlags) with the upcoming window.
        if (g.nextWindowData.flags hasnt NextWindowDataFlag.HasPos) {
            val viewport = if(window.wasActive) window.viewport!! else mainViewport as ViewportP // FIXME-VIEWPORT: What may be our reference viewport?
            setNextWindowPos(viewport.mainRect.center, Cond.Appearing, Vec2(0.5f))
        }

        val flags = flags_ or Wf._Popup or Wf._Modal or Wf.NoCollapse or Wf.NoSavedSettings or Wf.NoDocking
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
        navMoveRequestTryWrapping(window, NavMoveFlag.LoopY.i)

        // Child-popups don't need to be laid out
        assert(!g.withinEndChild)
        if (window.flags has Wf._ChildWindow)
            g.withinEndChild = true
        end()
        g.withinEndChild = false
    }

    /** Helper to open popup when clicked on last item.  (note: actually triggers on the mouse _released_ event to be
     *  consistent with popup behaviors). return true when just opened.   */
    fun openPopupOnItemClick(strId: String = "", mouseButton: MouseButton = MouseButton.Right) =
            with(g.currentWindow!!) {
                if (isMouseReleased(mouseButton) && isItemHovered(Hf.AllowWhenBlockedByPopup)) {
                    // If user hasn't passed an ID, we can use the LastItemID. Using LastItemID as a Popup ID won't conflict!
                    val id = if (strId.isNotEmpty()) getID(strId) else dc.lastItemId
                    assert(id != 0) { "You cannot pass a NULL str_id if the last item has no identifier (e.g. a Text() item)" }
                    openPopupEx(id)
                    true
                } else false
            }

    fun isPopupOpen(strId: String) = g.openPopupStack.size > g.beginPopupStack.size &&
            g.openPopupStack[g.beginPopupStack.size].popupId == g.currentWindow!!.getID(strId)

    /** close the popup we have begin-ed into. clicking on a MenuItem or Selectable automatically close
     *  the current popup.  */
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
}