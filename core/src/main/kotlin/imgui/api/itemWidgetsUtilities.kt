package imgui.api

import gli_.hasnt
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.currentWindowRead
import imgui.ImGui.isMouseClicked
import imgui.internal.sections.ItemFlag
import imgui.internal.sections.ItemStatusFlag
import imgui.internal.sections.has
import imgui.internal.sections.hasnt
import imgui.HoveredFlag as Hf

/** Item/Widgets Utilities
 *  - Most of the functions are referring to the last/previous item we submitted.
 *  - See Demo Window under "Widgets->Querying Status" for an interactive visualization of most of those functions. */
interface itemWidgetsUtilities {

    /** This is roughly matching the behavior of internal-facing ItemHoverable()
     *      - we allow hovering to be true when activeId==window.moveID, so that clicking on non-interactive items
     *          such as a text() item still returns true with isItemHovered()
     *      - this should work even for non-interactive items that have no ID, so we cannot use LastItemId  */
    fun isItemHovered(flags: Hf) = isItemHovered(flags.i)

    fun isItemHovered(flags: Int = Hf.None.i): Boolean {

        if (g.navDisableMouseHover && !g.navDisableHighlight)
            return isItemFocused

        val window = g.currentWindow!!
        return when {
            // Test for bounding box overlap, as updated as ItemAdd()
            window.dc.lastItemStatusFlags hasnt ItemStatusFlag.HoveredRect -> false
            else -> {
                assert(flags hasnt (Hf.RootWindow or Hf.ChildWindows)) { "Flags not supported by this function" }
                when {
                    /*  Test if we are hovering the right window (our window could be behind another window)
                        [2017/10/16] Reverted commit 344d48be3 and testing RootWindow instead. I believe it is correct to
                        NOT test for rootWindow but this leaves us unable to use isItemHovered after endChild() itself.
                        Until a solution is found I believe reverting to the test from 2017/09/27 is safe since this was
                        the test that has been running for a long while. */
                    // g.hoveredWindow !== window -> false
                    g.hoveredRootWindow !== window.rootWindow && flags hasnt Hf.AllowWhenOverlapped -> false
                    // Test if another item is active (e.g. being dragged)
                    flags hasnt Hf.AllowWhenBlockedByActiveItem && g.activeId != 0 && g.activeId != window.dc.lastItemId &&
                            !g.activeIdAllowOverlap && g.activeId != window.moveId -> false
                    // Test if interactions on this window are blocked by an active popup or modal
                    // The ImGuiHoveredFlags_AllowWhenBlockedByPopup flag will be tested here.
                    g.navDisableMouseHover || !window.isContentHoverable(flags) -> false
                    // Test if the item is disabled
                    window.dc.itemFlags has ItemFlag.Disabled && flags hasnt Hf.AllowWhenDisabled -> false
                    /*  Special handling for calling after Begin() which represent the title bar or tab.
                        When the window is collapsed (SkipItems==true) that last item will never be overwritten
                        so we need to detect the case.  */
                    window.dc.lastItemId == window.moveId && window.writeAccessed -> false
                    else -> true
                }
            }
        }
    }

    /** Is the last item active? (e.g. button being held, text field being edited.
     *  This will continuously return true while holding mouse button on an item. Items that don't interact will always return false) */
    val isItemActive: Boolean
        get() = if (g.activeId != 0) g.activeId == g.currentWindow!!.dc.lastItemId else false

    /** Is the last item focused for keyboard/gamepad navigation?
     *
     *  == GetItemID() == GetFocusID() */
    val isItemFocused: Boolean
        get() = !(g.navId != g.currentWindow!!.dc.lastItemId || g.navId == 0)

    /** Is the last item clicked? (e.g. button/node just clicked on) == IsMouseClicked(mouse_button) && IsItemHovered() */
    fun isItemClicked(mouseButton: MouseButton = MouseButton.Left): Boolean =
            isMouseClicked(mouseButton) && isItemHovered(Hf.None)

    /** Is the last item visible? (items may be out of sight because of clipping/scrolling)    */
    val isItemVisible: Boolean
        get() = currentWindowRead!!.run { clipRect overlaps dc.lastItemRect }

    val isItemEdited: Boolean
        get () = currentWindowRead!!.run { dc.lastItemStatusFlags has ItemStatusFlag.Edited }

    /** was the last item just made active (item was previously inactive). */
    val isItemActivated: Boolean
        get() = when (g.activeId) {
            0 -> false
            else -> g.currentWindow!!.run { g.activeId == dc.lastItemId && g.activeIdPreviousFrame != dc.lastItemId }
        }

    /** Was the last item just made inactive (item was previously active).
     *  Useful for Undo/Redo patterns with widgets that requires continuous editing. */
    val isItemDeactivated: Boolean
        get() = g.currentWindow!!.run {
            when {
                dc.lastItemStatusFlags has ItemStatusFlag.HasDeactivated ->
                    dc.lastItemStatusFlags has ItemStatusFlag.Deactivated
                else -> g.activeIdPreviousFrame == dc.lastItemId && g.activeIdPreviousFrame != 0 && g.activeId != dc.lastItemId
            }
        }

    /** Was the last item just made inactive and made a value change when it was active? (e.g. Slider/Drag moved).
     *  Useful for Undo/Redo patterns with widgets that requires continuous editing.
     *  Note that you may get false positives (some widgets such as Combo()/ListBox()/Selectable() will return true even when clicking an already selected item). */
    val isItemDeactivatedAfterEdit: Boolean
        get() = isItemDeactivated && (g.activeIdPreviousFrameHasBeenEdited || (g.activeId == 0 && g.activeIdHasBeenEditedBefore))

    /** was the last item open state toggled? set by TreeNode(). */
    val isItemToggledOpen: Boolean
        get() = g.currentWindow!!.dc.lastItemStatusFlags has ItemStatusFlag.ToggledOpen

    /** is any item hovered? */
    val isAnyItemHovered: Boolean
        get() = g.hoveredId != 0 || g.hoveredIdPreviousFrame != 0

    /** is any item active? */
    val isAnyItemActive: Boolean
        get() = g.activeId != 0

    /** is any item focused? */
    val isAnyItemFocused: Boolean
        get() = g.navId != 0 && !g.navDisableHighlight

    /** get upper-left bounding rectangle of the last item (screen space)
     *  ~GetItemRectMin */
    val itemRectMin: Vec2
        get() = currentWindowRead!!.dc.lastItemRect.min

    /** get lower-right bounding rectangle of the last item (screen space)
     *  ~GetItemRectMax */
    val itemRectMax: Vec2
        get() = currentWindowRead!!.dc.lastItemRect.max

    /** get size of last item
     *  ~GetItemRectSize    */
    val itemRectSize: Vec2
        get() = currentWindowRead!!.dc.lastItemRect.size

    /** Allow last item to be overlapped by a subsequent item. Both may be activated during the same frame before the later one takes priority.
     *  FIXME: Although this is exposed, its interaction and ideal idiom with using ImGuiButtonFlags_AllowItemOverlap flag are extremely confusing, need rework. */
    fun setItemAllowOverlap() {
        val id = g.currentWindow!!.dc.lastItemId
        if (g.hoveredId == id)
            g.hoveredIdAllowOverlap = true
        if (g.activeId == id)
            g.activeIdAllowOverlap = true
    }
}