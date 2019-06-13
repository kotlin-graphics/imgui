package imgui.imgui

import gli_.hasnt
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.currentWindowRead
import imgui.ImGui.isMouseClicked
import imgui.internal.*
import imgui.FocusedFlag as Ff
import imgui.HoveredFlag as Hf
import imgui.WindowFlag as Wf

/** Item/Widgets Utilities
 *  - Most of the functions are referring to the last/previous item we submitted.
 *  - See Demo Window under "Widgets->Querying Status" for an interactive visualization of most of those functions. */
interface imgui_itemWidgetsUtilities {

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
                    /*  Special handling for the dummy item after Begin() which represent the title bar or tab.
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

    /** Is the last item focused for keyboard/gamepad navigation?   */
    val isItemFocused: Boolean
        get() = g.navId != 0 && !g.navDisableHighlight && g.navId == g.currentWindow!!.dc.lastItemId

    /** Is the last item clicked? (e.g. button/node just clicked on) == IsMouseClicked(mouse_button) && IsItemHovered() */
    fun isItemClicked(mouseButton: Int = 0): Boolean = isMouseClicked(mouseButton) && isItemHovered(Hf.None)

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
        get() = g.currentWindow!!.dc.let { g.activeIdPreviousFrame == it.lastItemId && g.activeIdPreviousFrame != 0 && g.activeId != it.lastItemId }

    /** Was the last item just made inactive and made a value change when it was active? (e.g. Slider/Drag moved).
     *  Useful for Undo/Redo patterns with widgets that requires continuous editing.
     *  Note that you may get false positives (some widgets such as Combo()/ListBox()/Selectable() will return true even when clicking an already selected item). */
    val isItemDeactivatedAfterEdit: Boolean
        get() = isItemDeactivated && (g.activeIdPreviousFrameHasBeenEdited || (g.activeId == 0 && g.activeIdHasBeenEditedBefore))

    /** is any item hovered? */
    val isAnyItemHovered: Boolean
        get() = g.hoveredId != 0 || g.hoveredIdPreviousFrame != 0

    /** is any item active? */
    val isAnyItemActive: Boolean
        get() = g.activeId != 0

    /** is any item focused? */
    val isAnyItemFocused: Boolean
        get() = g.navId != 0 && !g.navDisableHighlight

    /** get upper-left bounding rectangle of the last item (screen space)  */
    val itemRectMin: Vec2
        get() = currentWindowRead!!.dc.lastItemRect.min

    /** get lower-right bounding rectangle of the last item (screen space)  */
    val itemRectMax: Vec2
        get() = currentWindowRead!!.dc.lastItemRect.max

    /** get size of last item  */
    val itemRectSize: Vec2
        get() = currentWindowRead!!.dc.lastItemRect.size

    /** allow last item to be overlapped by a subsequent item. sometimes useful with invisible buttons, selectables, etc.
     *  to catch unused area.   */
    fun setItemAllowOverlap() {
        if (g.hoveredId == g.currentWindow!!.dc.lastItemId)
            g.hoveredIdAllowOverlap = true
        if (g.activeId == g.currentWindow!!.dc.lastItemId)
            g.activeIdAllowOverlap = true
    }

    val Int.vec4: Vec4
        get() {
            val s = 1f / 255f
            return Vec4(
                    ((this ushr COL32_R_SHIFT) and 0xFF) * s,
                    ((this ushr COL32_G_SHIFT) and 0xFF) * s,
                    ((this ushr COL32_B_SHIFT) and 0xFF) * s,
                    ((this ushr COL32_A_SHIFT) and 0xFF) * s)
        }

    val Vec4.u32: Int
        get () {
            var out = F32_TO_INT8_SAT(x) shl COL32_R_SHIFT
            out = out or (F32_TO_INT8_SAT(y) shl COL32_G_SHIFT)
            out = out or (F32_TO_INT8_SAT(z) shl COL32_B_SHIFT)
            return out or (F32_TO_INT8_SAT(w) shl COL32_A_SHIFT)
        }

    /** Unsaturated, for display purpose    */
    fun F32_TO_INT8_UNBOUND(_val: Float) = (_val * 255f + if (_val >= 0) 0.5f else -0.5f).i

    /** Saturated, always output 0..255 */
    fun F32_TO_INT8_SAT(_val: Float) = (saturate(_val) * 255f + 0.5f).i
}