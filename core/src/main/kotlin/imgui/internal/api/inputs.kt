package imgui.internal.api

import gli_.has
import glm_.f
import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.io
import imgui.ImGui.navMoveRequestCancel
import imgui.api.g
import imgui.internal.sections.*

/** Inputs
 *  FIXME: Eventually we should aim to move e.g. IsActiveIdUsingKey() into IsKeyXXX functions. */
internal interface inputs {

    fun setItemUsingMouseWheel() {
        val id = g.lastItemData.id
        if (g.hoveredId == id)
            g.hoveredIdUsingMouseWheel = true
        if (g.activeId == id) {
            g.activeIdUsingKeyInputMask setBit Key.MouseWheelX.i
            g.activeIdUsingKeyInputMask setBit Key.MouseWheelY.i
        }
    }

    // FIXME: Technically this also prevents use of Gamepad D-Pad, may not be an issue.
    fun setActiveIdUsingNavAndKeys() {
        assert(g.activeId != 0)
        g.activeIdUsingNavDirMask = 0.inv()
        g.activeIdUsingKeyInputMask.setBitRange(Key.Keyboard_BEGIN, Key.Keyboard_END)
        g.activeIdUsingKeyInputMask setBit Key.ModCtrl.i
        g.activeIdUsingKeyInputMask setBit Key.ModShift.i
        g.activeIdUsingKeyInputMask setBit Key.ModAlt.i
        g.activeIdUsingKeyInputMask setBit Key.ModSuper.i
        navMoveRequestCancel()
    }

    infix fun isActiveIdUsingNavDir(dir: Dir): Boolean = g.activeIdUsingNavDirMask has (1 shl dir)

    infix fun isActiveIdUsingKey(key: Key): Boolean = g.activeIdUsingKeyInputMask testBit key.i

    infix fun setActiveIdUsingKey(key: Key) = g.activeIdUsingKeyInputMask setBit key.i

    fun mouseButtonToKey(button: MouseButton): Key {
        val mouseLeftIndex = Key.values().indexOf(Key.MouseLeft)
        return Key.values()[mouseLeftIndex + button.i]
    }

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

    /** ~GetMergedModFlags
     *
     *  [Internal] Do not use directly (can read io.KeyMods instead) */
    val mergedModFlags: ModFlags
        get() {
            var keyMods = ModFlag.None.i
            if (ImGui.io.keyCtrl) keyMods /= ModFlag.Ctrl
            if (ImGui.io.keyShift) keyMods /= ModFlag.Shift
            if (ImGui.io.keyAlt) keyMods /= ModFlag.Alt
            if (ImGui.io.keySuper) keyMods /= ModFlag.Super
            return keyMods
        }

    /** Return 2D vector representing the combination of four cardinal direction, with analog value support (for e.g. ImGuiKey_GamepadLStick* values). */
    fun getKeyVector2d(keyLeft: Key, keyRight: Key, keyUp: Key, keyDown: Key) = Vec2(keyRight.data.analogValue - keyLeft.data.analogValue,
                                                                                     keyDown.data.analogValue - keyUp.data.analogValue)

    fun getNavTweakPressedAmount(axis: Axis): Float {

        val (repeatDelay, repeatRate) = getTypematicRepeatRate(InputFlag.RepeatRateNavTweak.i)

        val keyLess: Key
        val keyMore: Key
        if (g.navInputSource == InputSource.Gamepad) {
            keyLess = if (axis == Axis.X) Key.GamepadDpadLeft else Key.GamepadDpadUp
            keyMore = if (axis == Axis.X) Key.GamepadDpadRight else Key.GamepadDpadDown
        } else {
            keyLess = if (axis == Axis.X) Key.LeftArrow else Key.UpArrow
            keyMore = if (axis == Axis.X) Key.RightArrow else Key.DownArrow
        }
        var amount = keyMore.getPressedAmount(repeatDelay, repeatRate).f - keyLess.getPressedAmount(repeatDelay, repeatRate).f
        if (amount != 0f && keyLess.isDown && keyMore.isDown) // Cancel when opposite directions are held, regardless of repeat phase
            amount = 0f
        return amount
    }

    fun calcTypematicRepeatAmount(t0: Float, t1: Float, repeatDelay: Float, repeatRate: Float): Int = when {
        t1 == 0f -> 1
        t0 >= t1 -> 0
        repeatRate <= 0f -> (t0 < repeatDelay && t1 >= repeatDelay).i
        else -> {
            val countT0 = if (t0 < repeatDelay) -1 else ((t0 - repeatDelay) / repeatRate).i
            val countT1 = if (t1 < repeatDelay) -1 else ((t1 - repeatDelay) / repeatRate).i
            val count = countT1 - countT0
            count
        }
    }

    /** @return repeatDelay, repeatRate */
    fun getTypematicRepeatRate(flags: InputFlags): Pair<Float, Float> = when (flags and InputFlag.RepeatRateMask_) {
        InputFlag.RepeatRateNavMove.i -> g.io.keyRepeatDelay * 0.72f to g.io.keyRepeatRate * 0.80f
        InputFlag.RepeatRateNavTweak.i -> g.io.keyRepeatDelay * 0.72f to g.io.keyRepeatRate * 0.30f
        else -> g.io.keyRepeatDelay * 1.00f to g.io.keyRepeatRate * 1.00f
    }

    // IsKeyPressedEx -> Key
}