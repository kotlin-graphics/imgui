package imgui.internal.api

import glm_.has
import glm_.f
import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.getPressedAmount
import imgui.ImGui.io
import imgui.ImGui.isDown
import imgui.ImGui.navMoveRequestCancel
import imgui.api.g
import imgui.api.gImGui
import imgui.classes.Context
import imgui.classes.KeyData
import imgui.internal.classes.*
import imgui.internal.sections.*

/** Inputs
 *  FIXME: Eventually we should aim to move e.g. IsActiveIdUsingKey() into IsKeyXXX functions. */
internal interface inputs {


    /** ~IsNamedKey */
    val Key.isNamed: Boolean
        get() = this in Key.Named

    /** ~IsNamedKeyOrModKey */
    val Key.isNamedOrMod: Boolean
        get() = isNamed || this == Key.Mod_Ctrl || this == Key.Mod_Shift || this == Key.Mod_Alt || this == Key.Mod_Super || this == Key.Mod_Shortcut

    //    inline bool             IsLegacyKey(ImGuiKey key)                                   { return key >= ImGuiKey_LegacyNativeKey_BEGIN && key < ImGuiKey_LegacyNativeKey_END; }

    /** ~IsKeyboardKey */
    val Key.isKeyboardKey: Boolean
        get() = this in Key.Keyboard

    /** ~IsMouseKey */
    val Key.isMouse: Boolean
        get() = this in Key.Mouse

    /** ~IsGamepadKey */
    val Key.isGamepad: Boolean
        get() = this in Key.Gamepad

    /** ~IsAliasKey */
    val Key.isAlias: Boolean
        get() = this in Key.Aliases

    fun convertShortcutMod(keyChord: KeyChord): KeyChord {
        check(keyChord has Key.Mod_Shortcut)
        return keyChord wo Key.Mod_Shortcut or if (g.io.configMacOSXBehaviors) Key.Mod_Super else Key.Mod_Ctrl
    }

    fun Key.convertSingleModFlagToKey(ctx: Context): Key = when (this) {
        Key.Mod_Ctrl -> Key.ReservedForModCtrl
        Key.Mod_Shift -> Key.ReservedForModShift
        Key.Mod_Alt -> Key.ReservedForModAlt
        Key.Mod_Super -> Key.ReservedForModSuper
        Key.Mod_Shortcut -> if (g.io.configMacOSXBehaviors) Key.ReservedForModSuper else Key.ReservedForModCtrl
        else -> this
    }

    /** ~GetKeyData */
    fun Key.getData(ctx: Context): KeyData {
        val g = ctx
        val k =
                // Special storage location for mods
                if (this has Key.Mod_Mask)
                    convertSingleModFlagToKey(g)
                else this
        return g.io.keysData[k.index]
    }

    val Key.data: KeyData
        // Special storage location for mods
        get() = getData(gImGui)

    // ImGuiMod_Shortcut is translated to either Ctrl or Super.
    fun getKeyChordName(keyChord_: KeyChord): String {
        val keyChord = if (keyChord_ has Key.Mod_Shortcut) convertShortcutMod(keyChord_) else keyChord_
        var out = if (keyChord has Key.Mod_Ctrl) "Ctrl+" else ""
        out += if (keyChord has Key.Mod_Shift) "Shift+" else ""
        out += if (keyChord has Key.Mod_Alt) "Alt+" else ""
        out += if (keyChord has Key.Mod_Super) (if (g.io.configMacOSXBehaviors) "Cmd+" else "Super+") else ""
        return out + (Key of (keyChord wo Key.Mod_Mask)).name
    }


    fun mouseButtonToKey(button: Int): Key {
        assert(button in 0 until MouseButton.COUNT)
        return (MouseButton of button).key
    }

    /** Return if a mouse click/drag went past the given threshold. Valid to call during the MouseReleased frame.
     *  [Internal] This doesn't test if the button is pressed */
    infix fun MouseButton.isDragPastThreshold(lockThreshold_: Float): Boolean {
        assert(i in io.mouseDown.indices)
        if (!io.mouseDown[i])
            return false
        var lockThreshold = lockThreshold_
        if (lockThreshold < 0f)
            lockThreshold = io.mouseDragThreshold
        return io.mouseDragMaxDistanceSqr[i] >= lockThreshold * lockThreshold
    }

    /** Return 2D vector representing the combination of four cardinal direction, with analog value support (for e.g. ImGuiKey_GamepadLStick* values). */
    fun getKeyMagnitude2d(keyLeft: Key, keyRight: Key, keyUp: Key, keyDown: Key) = Vec2(keyRight.data.analogValue - keyLeft.data.analogValue,
            keyDown.data.analogValue - keyUp.data.analogValue)

    fun getNavTweakPressedAmount(axis: Axis): Float {

        val (repeatDelay, repeatRate) = getTypematicRepeatRate(InputFlag.RepeatRateNavTweak)

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
    fun getTypematicRepeatRate(flags: InputFlags): Pair<Float, Float> {
        val flag = flags and InputFlag.RepeatRateMask
        return when {
            flag == InputFlag.RepeatRateNavMove -> g.io.keyRepeatDelay * 0.72f to g.io.keyRepeatRate * 0.80f
            flag == InputFlag.RepeatRateNavTweak -> g.io.keyRepeatDelay * 0.72f to g.io.keyRepeatRate * 0.30f
            else -> g.io.keyRepeatDelay * 1.00f to g.io.keyRepeatRate * 1.00f
        }
    }

    /** FIXME: It might be undesirable that this will likely disable KeyOwner-aware shortcuts systems. Consider a more fine-tuned version for the two users of this function. */
    fun setActiveIdUsingAllKeyboardKeys() {
        assert(g.activeId != 0)
        g.activeIdUsingNavDirMask = (1 shl Dir.COUNT) - 1
        g.activeIdUsingAllKeyboardKeys = true
        navMoveRequestCancel()
    }

    fun isActiveIdUsingNavDir(dir: Dir): Boolean = g.activeIdUsingNavDirMask has (1 shl dir)

    // [EXPERIMENTAL] Low-Level: Key/Input Ownership
    // - The idea is that instead of "eating" a given input, we can link to an owner id.
    // - Ownership is most often claimed as a result of reacting to a press/down event (but occasionally may be claimed ahead).
    // - Input queries can then read input by specifying ImGuiKeyOwner_Any (== 0), ImGuiKeyOwner_None (== -1) or a custom ID.
    // - Legacy input queries (without specifying an owner or _Any or _None) are equivalent to using ImGuiKeyOwner_Any (== 0).
    // - Input ownership is automatically released on the frame after a key is released. Therefore:
    //   - for ownership registration happening as a result of a down/press event, the SetKeyOwner() call may be done once (common case).
    //   - for ownership registration happening ahead of a down/press event, the SetKeyOwner() call needs to be made every frame (happens if e.g. claiming ownership on hover).
    // - SetItemKeyOwner() is a shortcut for common simple case. A custom widget will probably want to call SetKeyOwner() multiple times directly based on its interaction state.
    // - This is marked experimental because not all widgets are fully honoring the Set/Test idioms. We will need to move forward step by step.
    //   Please open a GitHub Issue to submit your usage scenario or if there's a use case you need solved.

    /** ~GetKeyOwner */
    val Key.owner: ID
        get() {
            if (!isNamedOrMod)
                return KeyOwner_None
            //
            //            val ownerData = ownerData(key);
            val ownerId = getOwnerData(g).ownerCurr

            if (g.activeIdUsingAllKeyboardKeys && ownerId != g.activeId && ownerId != KeyOwner_Any)
                if (this in Key.Keyboard)
                    return KeyOwner_None

            return ownerId
        }


    // ~SetKeyOwner
    // _LockXXX flags are useful to lock keys away from code which is not input-owner aware.
    // When using _LockXXX flags, you can use ImGuiKeyOwner_Any to lock keys from everyone.
    // - SetKeyOwner(..., None)              : clears owner
    // - SetKeyOwner(..., Any, !Lock)        : illegal (assert)
    // - SetKeyOwner(..., Any or None, Lock) : set lock
    fun Key.setOwner(ownerId: ID, flags: InputFlags = none) {

        assert(isNamedOrMod && (ownerId != KeyOwner_Any || flags has (InputFlag.LockThisFrame or InputFlag.LockUntilRelease))) { "Can only use _Any with _LockXXX flags(to eat a key away without an ID to retrieve it)" }
        assert((flags wo InputFlag.SupportedBySetKeyOwner).isEmpty) { "Passing flags not supported by this function !" }

        val g = gImGui
        val ownerData = getOwnerData(g)
        ownerData.ownerCurr = ownerId; ownerData.ownerNext = ownerId

        // We cannot lock by default as it would likely break lots of legacy code.
        // In the case of using LockUntilRelease while key is not down we still lock during the frame (no key_data->Down test)
        ownerData.lockUntilRelease = flags has InputFlag.LockUntilRelease
        ownerData.lockThisFrame = flags has InputFlag.LockThisFrame || ownerData.lockUntilRelease
    }

    // Rarely used helper
    fun KeyChord.setOwners(ownerId: ID, flags: InputFlags = none) {
        if (has(Key.Mod_Ctrl)) Key.Mod_Ctrl.setOwner(ownerId, flags)
        if (has(Key.Mod_Shift)) Key.Mod_Shift.setOwner(ownerId, flags)
        if (has(Key.Mod_Alt)) Key.Mod_Alt.setOwner(ownerId, flags)
        if (has(Key.Mod_Super)) Key.Mod_Super.setOwner(ownerId, flags)
        if (has(Key.Mod_Shortcut)) Key.Mod_Shortcut.setOwner(ownerId, flags)
        if (wo(Key.Mod_Mask).isNotEmpty) (Key of (this wo Key.Mod_Mask)).setOwner(ownerId, flags)
    }

    // This is more or less equivalent to:
    //   if (IsItemHovered() || IsItemActive())
    //       SetKeyOwner(key, GetItemID());
    // Extensive uses of that (e.g. many calls for a single item) may want to manually perform the tests once and then call SetKeyOwner() multiple times.
    // More advanced usage scenarios may want to call SetKeyOwner() manually based on different condition.
    // Worth noting is that only one item can be hovered and only one item can be active, therefore this usage pattern doesn't need to bother with routing and priority.
    fun Key.setItemKeyOwner(flags_: InputFlags = none) { // Set key owner to last item if it is hovered or active. Equivalent to 'if (IsItemHovered() || IsItemActive()) { SetKeyOwner(key, GetItemID());'.
        var flags = flags_
        val id = g.lastItemData.id
        if (id == 0 || (g.hoveredId != id && g.activeId != id))
            return
        if (flags hasnt InputFlag.CondMask)
            flags /= InputFlag.CondDefault
        if ((g.hoveredId == id && flags has InputFlag.CondHovered) || (g.activeId == id && flags has InputFlag.CondActive)) {
            assert((flags wo InputFlag.SupportedBySetItemKeyOwner).isEmpty) { "Passing flags not supported by this function !" }
            setOwner(id, flags wo InputFlag.CondMask)
        }
    }

    // ~TestKeyOwner
    // TestKeyOwner(..., ID)   : (owner == None || owner == ID)
    // TestKeyOwner(..., None) : (owner == None)
    // TestKeyOwner(..., Any)  : no owner test
    // All paths are also testing for key not being locked, for the rare cases that key have been locked with using ImGuiInputFlags_LockXXX flags.
    infix fun Key.testOwner(ownerId: ID): Boolean { // Test that key is either not owned, either owned by 'owner_id'

        if (!isNamedOrMod)
            return true

        if (g.activeIdUsingAllKeyboardKeys && ownerId != g.activeId && ownerId != KeyOwner_Any)
            if (this in Key.Keyboard)
                return false

        val ownerData = getOwnerData(g)
        if (ownerId == KeyOwner_Any)
            return !ownerData.lockThisFrame

        // Note: SetKeyOwner() sets OwnerCurr. It is not strictly required for most mouse routing overlap (because of ActiveId/HoveredId
        // are acting as filter before this has a chance to filter), but sane as soon as user tries to look into things.
        // Setting OwnerCurr in SetKeyOwner() is more consistent than testing OwnerNext here: would be inconsistent with getter and other functions.
        if (ownerData.ownerCurr != ownerId) {
            if (ownerData.lockThisFrame)
                return false
            if (ownerData.ownerCurr != KeyOwner_None)
                return false
        }

        return true
    }


    /** ~GetKeyOwnerData */
    fun Key.getOwnerData(ctx: Context): KeyOwnerData {
        var key = this
        if (key has Key.Mod_Mask)
            key = key.convertSingleModFlagToKey(ctx)
        return ctx.keysOwnerData[key.ordinal]
    }


    // [EXPERIMENTAL] High-Level: Input Access functions w/ support for Key/Input Ownership
    // - Important: legacy IsKeyPressed(ImGuiKey, bool repeat=true) _DEFAULTS_ to repeat, new IsKeyPressed() requires _EXPLICIT_ ImGuiInputFlags_Repeat flag.
    // - Expected to be later promoted to public API, the prototypes are designed to replace existing ones (since owner_id can default to Any == 0)
    // - Specifying a value for 'ImGuiID owner' will test that EITHER the key is NOT owned (UNLESS locked), EITHER the key is owned by 'owner'.
    //   Legacy functions use ImGuiKeyOwner_Any meaning that they typically ignore ownership, unless a call to SetKeyOwner() explicitly used ImGuiInputFlags_LockThisFrame or ImGuiInputFlags_LockUntilRelease.
    // - Binding generators may want to ignore those for now, or suffix them with Ex() until we decide if this gets moved into public API.

    /** ~IsKeyDown */
    infix fun Key.isDown(ownerId: ID): Boolean = when {
        !data.down -> false
        !testOwner(ownerId) -> false
        else -> true
    }

    // Important: unless legacy IsKeyPressed(ImGuiKey, bool repeat=true) which DEFAULT to repeat, this requires EXPLICIT repeat.
    /** ~IsKeyPressed */
    fun Key.isPressed(ownerId: ID, flags: InputFlags = none): Boolean { // Important: when transitioning from old to new IsKeyPressed(): old API has "bool repeat = true", so would default to repeat. New API requiress explicit ImGuiInputFlags_Repeat.
        val keyData = data
        if (!keyData.down) // In theory this should already be encoded as (DownDuration < 0.0f), but testing this facilitates eating mechanism (until we finish work on key ownership)
            return false
        val t = keyData.downDuration
        if (t < 0f)
            return false
        assert((flags wo InputFlag.SupportedByIsKeyPressed).isEmpty) { "Passing flags not supported by this function !" }

        var pressed = t == 0f
        if (!pressed && flags hasnt InputFlag.Repeat) {
            val (repeatDelay, repeatRate) = getTypematicRepeatRate(flags)
            pressed = t > repeatDelay && getPressedAmount(repeatDelay, repeatRate) > 0
        }
        if (!pressed)
            return false
        return testOwner(ownerId)
    }

    /** ~IsKeyReleased */
    infix fun Key.isReleased(ownerId: ID): Boolean {
        val keyData = data
        if (keyData.downDurationPrev < 0f || keyData.down)
            return false
        return testOwner(ownerId)
    }

    /** ~IsMouseDown */
    infix fun MouseButton.isDown(ownerId: ID): Boolean {
        //        assert(button >= 0 && button < IM_ARRAYSIZE(g.IO.MouseDown));
        return g.io.mouseDown[i] && key testOwner ownerId // Should be same as IsKeyDown(MouseButtonToKey(button), owner_id), but this allows legacy code hijacking the io.Mousedown[] array.
    }

    /** ~IsMouseClicked */
    fun MouseButton.isClicked(ownerId: ID, flags: InputFlags = none): Boolean {

        //        IM_ASSERT(button >= 0 && button < IM_ARRAYSIZE(g.IO.MouseDown));
        if (!g.io.mouseDown[i]) // In theory this should already be encoded as (DownDuration < 0.0f), but testing this facilitates eating mechanism (until we finish work on key ownership)
            return false
        val t = g.io.mouseDownDuration[i]
        if (t < 0f)
            return false
        assert((flags wo InputFlag.SupportedByIsKeyPressed).isEmpty) { "Passing flags not supported by this function !" }

        val repeat = flags has InputFlag.Repeat
        val pressed = t == 0f || (repeat && t > g.io.keyRepeatDelay && calcTypematicRepeatAmount(t - g.io.deltaTime, t, g.io.keyRepeatDelay, g.io.keyRepeatRate) > 0)
        if (!pressed)
            return false

        return key.testOwner(ownerId)
    }

    /** ~IsMouseReleased */
    infix fun MouseButton.isReleased(ownerId: ID): Boolean {
        //        IM_ASSERT(button >= 0 && button < IM_ARRAYSIZE(g.IO.MouseDown));
        return g.io.mouseReleased[i] && key testOwner ownerId // Should be same as IsKeyReleased(MouseButtonToKey(button), owner_id)
    }


    // static sparse


    // owner_id may be None/Any, but routing_id needs to be always be set, so we default to GetCurrentFocusScope().
    fun getRoutingIdFromOwnerId(ownerId: ID): ID = if (ownerId != KeyOwner_None && ownerId != KeyOwner_Any) ownerId else g.currentFocusScopeId

    // Current score encoding (lower is highest priority):
    //  -   0: ImGuiInputFlags_RouteGlobalHigh
    //  -   1: ImGuiInputFlags_RouteFocused (if item active)
    //  -   2: ImGuiInputFlags_RouteGlobal
    //  -  3+: ImGuiInputFlags_RouteFocused (if window in focus-stack)
    //  - 254: ImGuiInputFlags_RouteGlobalLow
    //  - 255: never route
    // 'flags' should include an explicit routing policy
    fun calcRoutingScore(location: Window, ownerId: ID, flags: InputFlags): Int {

        if (flags has InputFlag.RouteFocused) {

            var focused = g.navWindow

            // ActiveID gets top priority
            // (we don't check g.ActiveIdUsingAllKeys here. Routing is applied but if input ownership is tested later it may discard it)
            if (ownerId != 0 && g.activeId == ownerId)
                return 1

            // Score based on distance to focused window (lower is better)
            // Assuming both windows are submitting a routing request,
            // - When Window....... is focused -> Window scores 3 (best), Window/ChildB scores 255 (no match)
            // - When Window/ChildB is focused -> Window scores 4,        Window/ChildB scores 3 (best)
            // Assuming only WindowA is submitting a routing request,
            // - When Window/ChildB is focused -> Window scores 4 (best), Window/ChildB doesn't have a score.
            if (focused != null && focused.rootWindow == location.rootWindow) {
                var nextScore = 3
                while (focused != null) {
                    if (focused == location) {
                        assert(nextScore < 255)
                        return nextScore
                    }
                    focused = if (focused.rootWindow !== focused) focused.parentWindow else null // FIXME: This could be later abstracted as a focus path
                    nextScore++
                }
            }
            return 255
        }

        // ImGuiInputFlags_RouteGlobalHigh is default, so calls without flags are not conditional
        return when {
            flags has InputFlag.RouteGlobal -> 2
            flags has InputFlag.RouteGlobalLow -> 254
            else -> 0
        }
    }
}