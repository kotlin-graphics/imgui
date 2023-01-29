package imgui.internal.api

import gli_.has
import imgui.*
import imgui.ImGui.io
import imgui.ImGui.navMoveRequestCancel
import imgui.api.g

/** Inputs
 *  FIXME: Eventually we should aim to move e.g. IsActiveIdUsingKey() into IsKeyXXX functions. */
internal interface inputs {

    fun setItemUsingMouseWheel() {
        val id = g.lastItemData.id
        if (g.hoveredId == id)
            g.hoveredIdUsingMouseWheel = true
        if (g.activeId == id)
            g.activeIdUsingMouseWheel = true
    }

    fun setActiveIdUsingNavAndKeys() {
        assert(g.activeId != 0)
        g.activeIdUsingNavDirMask = 0.inv()
        g.activeIdUsingNavInputMask = 0.inv()
        g.activeIdUsingKeyInputMask.setAllBits()
        navMoveRequestCancel()
    }

    infix fun isActiveIdUsingNavDir(dir: Dir): Boolean = g.activeIdUsingNavDirMask has (1 shl dir)

    infix fun isActiveIdUsingNavInput(input: NavInput): Boolean = g.activeIdUsingNavInputMask has (1 shl input)
    infix fun isActiveIdUsingKey(key: Key): Boolean = g.activeIdUsingKeyInputMask testBit key.i

    infix fun setActiveIdUsingKey(key: Key) = g.activeIdUsingKeyInputMask setBit key.i

    /** Return if a mouse click/drag went past the given threshold. Valid to call during the MouseReleased frame.
     *  [Internal] This doesn't test if the button is pressed */
    fun isMouseDragPastThreshold(button: MouseButton, lockThreshold_: Float): Boolean {

        assert(button.i in io.mouseDown.indices)
        if (!io.mouseDown[button.i])
            return false
        var lockThreshold = lockThreshold_
        if (lockThreshold < 0f)
            lockThreshold = io.mouseDragThreshold
        return io.mouseDragMaxDistanceSqr[button.i] >= lockThreshold * lockThreshold
    }

    // the rest of inputs functions are in the NavInput enum

    /** ~GetMergedKeyModFlags */
    val mergedKeyModFlags: KeyModFlags
        get() {
            var keyModFlags: KeyModFlags = KeyMod.None.i
            if (ImGui.io.keyCtrl) keyModFlags = keyModFlags or KeyMod.Ctrl.i
            if (ImGui.io.keyShift) keyModFlags = keyModFlags or KeyMod.Shift.i
            if (ImGui.io.keyAlt) keyModFlags = keyModFlags or KeyMod.Alt.i
            if (ImGui.io.keySuper) keyModFlags = keyModFlags or KeyMod.Super.i
            return keyModFlags
        }
}