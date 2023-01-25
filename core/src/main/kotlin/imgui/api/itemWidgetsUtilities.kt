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

// Item/Widgets Utilities and Query Functions
// - Most of the functions are referring to the previous Item that has been submitted.
// - See Demo Window under "Widgets->Querying Status" for an interactive visualization of most of those functions.
interface itemWidgetsUtilities {

    /** This is roughly matching the behavior of internal-facing ItemHoverable()
     *      - we allow hovering to be true when activeId==window.moveID, so that clicking on non-interactive items
     *          such as a text() item still returns true with isItemHovered()
     *      - this should work even for non-interactive items that have no ID, so we cannot use LastItemId  */
    fun isItemHovered(flags: Hf) = isItemHovered(flags.i)

    fun isItemHovered(flags: Int = Hf.None.i): Boolean {

        val window = g.currentWindow!!
        if (g.navDisableMouseHover && !g.navDisableHighlight) {
            if (g.lastItemData.inFlags has ItemFlag.Disabled && flags hasnt Hf.AllowWhenDisabled)
                return false
            return isItemFocused
        }

        // Test for bounding box overlap, as updated as ItemAdd()
        val statusFlags = g.lastItemData.statusFlags
        return when {
            statusFlags hasnt ItemStatusFlag.HoveredRect -> false
            else -> {
                assert(flags hasnt (Hf.RootWindow or Hf.ChildWindows)) { "Flags not supported by this function" }
                when {
                    // Test if we are hovering the right window (our window could be behind another window)
                    // [2021/03/02] Reworked / reverted the revert, finally. Note we want e.g. BeginGroup/ItemAdd/EndGroup to work as well. (#3851)
                    // [2017/10/16] Reverted commit 344d48be3 and testing RootWindow instead. I believe it is correct to NOT test for RootWindow but this leaves us unable
                    // to use IsItemHovered() after EndChild() itself. Until a solution is found I believe reverting to the test from 2017/09/27 is safe since this was
                    // the test that has been running for a long while.
                    g.hoveredWindow !== window && statusFlags hasnt ItemStatusFlag.HoveredWindow && flags hasnt Hf.AllowWhenOverlapped -> false
                    // Test if another item is active (e.g. being dragged)
                    flags hasnt Hf.AllowWhenBlockedByActiveItem && g.activeId != 0 && g.activeId != g.lastItemData.id &&
                            !g.activeIdAllowOverlap && g.activeId != window.moveId -> false
                    // Test if interactions on this window are blocked by an active popup or modal
                    // The ImGuiHoveredFlags_AllowWhenBlockedByPopup flag will be tested here.
                    g.navDisableMouseHover || !window.isContentHoverable(flags) -> false
                    // Test if the item is disabled
                    g.lastItemData.inFlags has ItemFlag.Disabled && flags hasnt Hf.AllowWhenDisabled -> false
                    /*  Special handling for calling after Begin() which represent the title bar or tab.
                        When the window is collapsed (SkipItems==true) that last item will never be overwritten
                        so we need to detect the case.  */
                    g.lastItemData.id == window.moveId && window.writeAccessed -> false
                    else -> true
                }
            }
        }
    }

    /** Is the last item active? (e.g. button being held, text field being edited.
     *  This will continuously return true while holding mouse button on an item. Items that don't interact will always return false) */
    val isItemActive: Boolean
        get() = g.activeId != 0 && g.activeId == g.lastItemData.id

    /** Is the last item focused for keyboard/gamepad navigation?
     *
     *  == GetItemID() == GetFocusID() */
    val isItemFocused: Boolean
        get() = g.navId == g.lastItemData.id && g.navId != 0

    /** is the last item hovered and mouse clicked on? (**)  == IsMouseClicked(mouse_button) && IsItemHovered()Important. (**) this it NOT equivalent to the behavior of e.g. Button(). Read comments in function definition.
     *
     *  Important: this can be useful but it is NOT equivalent to the behavior of e.g.Button()!
     *  Most widgets have specific reactions based on mouse-up/down state, mouse position etc. */
    fun isItemClicked(mouseButton: MouseButton = MouseButton.Left): Boolean =
        isMouseClicked(mouseButton) && isItemHovered(Hf.None)

    /** Is the last item visible? (items may be out of sight because of clipping/scrolling)    */
    val isItemVisible: Boolean
        get() = g.currentWindow!!.clipRect overlaps g.lastItemData.rect

    val isItemEdited: Boolean
        get() = g.lastItemData.statusFlags has ItemStatusFlag.Edited

    /** was the last item just made active (item was previously inactive). */
    val isItemActivated: Boolean
        get() = g.activeId != 0 && g.activeId == g.lastItemData.id && g.activeIdPreviousFrame != g.lastItemData.id

    /** Was the last item just made inactive (item was previously active).
     *  Useful for Undo/Redo patterns with widgets that requires continuous editing. */
    val isItemDeactivated: Boolean
        get() = when {
            g.lastItemData.statusFlags has ItemStatusFlag.HasDeactivated -> g.lastItemData.statusFlags has ItemStatusFlag.Deactivated
            else -> g.activeIdPreviousFrame == g.lastItemData.id && g.activeIdPreviousFrame != 0 && g.activeId != g.lastItemData.id
        }

    /** Was the last item just made inactive and made a value change when it was active? (e.g. Slider/Drag moved).
     *  Useful for Undo/Redo patterns with widgets that requires continuous editing.
     *  Note that you may get false positives (some widgets such as Combo()/ListBox()/Selectable() will return true even when clicking an already selected item). */
    val isItemDeactivatedAfterEdit: Boolean
        get() = isItemDeactivated && (g.activeIdPreviousFrameHasBeenEdited || (g.activeId == 0 && g.activeIdHasBeenEditedBefore))

    /** was the last item open state toggled? set by TreeNode(). */
    val isItemToggledOpen: Boolean
        get() = g.lastItemData.statusFlags has ItemStatusFlag.ToggledOpen

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
        get() = g.lastItemData.rect.min

    /** get lower-right bounding rectangle of the last item (screen space)
     *  ~GetItemRectMax */
    val itemRectMax: Vec2
        get() = g.lastItemData.rect.max

    /** get size of last item
     *  ~GetItemRectSize    */
    val itemRectSize: Vec2
        get() = g.lastItemData.rect.size

    /** Allow last item to be overlapped by a subsequent item. Both may be activated during the same frame before the later one takes priority.
     *  FIXME: Although this is exposed, its interaction and ideal idiom with using ImGuiButtonFlags_AllowItemOverlap flag are extremely confusing, need rework. */
    fun setItemAllowOverlap() {
        val id = g.lastItemData.id
        if (g.hoveredId == id)
            g.hoveredIdAllowOverlap = true
        if (g.activeId == id)
            g.activeIdAllowOverlap = true
    }
}