package imgui.internal.api

import gli_.has
import glm_.f
import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.getPressedAmount
import imgui.ImGui.io
import imgui.ImGui.isDown
import imgui.ImGui.navMoveRequestCancel
import imgui.api.g
import imgui.classes.KeyData
import imgui.internal.classes.Window
import imgui.internal.isPowerOfTwo
import imgui.internal.sections.*

/** Inputs
 *  FIXME: Eventually we should aim to move e.g. IsActiveIdUsingKey() into IsKeyXXX functions. */
internal interface inputs {

    val Key.isNamed: Boolean
        get() = i in Key.BEGIN until Key.END
    val Key.isNamedOrMod: Boolean
        get() = isNamed || this == Key.Mod_Ctrl || this == Key.Mod_Shift || this == Key.Mod_Alt || this == Key.Mod_Super

    //    inline bool             IsLegacyKey(ImGuiKey key)                                   { return key >= ImGuiKey_LegacyNativeKey_BEGIN && key < ImGuiKey_LegacyNativeKey_END; }

    /** ~IsGamepadKey */
    val Key.isGamepad: Boolean
        get() = i in Key.Gamepad_BEGIN until Key.Gamepad_END

    /** ~IsAliasKey */
    val Key.isAlias: Boolean
        get() = i in Key.Aliases_BEGIN until Key.Aliases_END

    fun Key.convertSingleModFlagToKey(): Key = when (this) {
        Key.Mod_Ctrl -> Key.ReservedForModCtrl
        Key.Mod_Shift -> Key.ReservedForModShift
        Key.Mod_Alt -> Key.ReservedForModAlt
        Key.Mod_Super -> Key.ReservedForModSuper
        else -> this
    }

    /** ~GetKeyData */
    val Key.data: KeyData
        get() {
            var key = this
            // Special storage location for mods
            if (i has Key.Mod_Mask_)
                key = key.convertSingleModFlagToKey()

            return g.io.keysData[key.index]
        }

    fun getKeyChordName(keyChord: KeyChord): String {
        var out = if (keyChord has Key.Mod_Ctrl) "Ctrl+" else ""
        out += if (keyChord has Key.Mod_Shift) "Shift+" else ""
        out += if (keyChord has Key.Mod_Alt) "Alt+" else ""
        out += if (keyChord has Key.Mod_Super) (if (g.io.configMacOSXBehaviors) "Cmd+" else "Super+") else ""
        return out + (Key of (keyChord wo Key.Mod_Mask_)).name
    }

    fun MouseButton.toKey(): Key {
        val mouseLeftIndex = Key.values().indexOf(Key.MouseLeft)
        return Key.values()[mouseLeftIndex + i]
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

    val Key.owner: ID
        get() {
            //            if (!IsNamedKeyOrModKey(key))
            //                return ImGuiKeyOwner_None;

            //            val ownerData = ownerData(key);
            val ownerId = ownerData.ownerCurr

            if (g.activeIdUsingAllKeyboardKeys && ownerId != g.activeId)
                if (i in Key.BEGIN until Key.END || this == Key.Mod_Ctrl || this == Key.Mod_Shift || this == Key.Mod_Alt || this == Key.Mod_Super)
                    return KeyOwner_None

            return ownerId
        }


    // ~SetKeyOwner
    // _LockXXX flags are useful to lock keys away from code which is not input-owner aware.
    // When using _LockXXX flags, you can use ImGuiKeyOwner_Any to lock keys from everyone.
    // - SetKeyOwner(..., None)              : clears owner
    // - SetKeyOwner(..., Any, !Lock)        : illegal (assert)
    // - SetKeyOwner(..., Any or None, Lock) : set lock
    fun Key.setOwner(ownerId: ID, flags: InputFlags = 0) {

        assert(isNamedOrMod && (ownerId != KeyOwner_Any || flags has (InputFlag.LockThisFrame or InputFlag.LockUntilRelease))) { "Can only use _Any with _LockXXX flags(to eat a key away without an ID to retrieve it)" }
        assert(flags hasnt InputFlag.SupportedBySetKeyOwner) { "Passing flags not supported by this function !" }

        val ownerData = ownerData
        ownerData.ownerCurr = ownerId; ownerData.ownerNext = ownerId

        // We cannot lock by default as it would likely break lots of legacy code.
        // In the case of using LockUntilRelease while key is not down we still lock during the frame (no key_data->Down test)
        ownerData.lockUntilRelease = flags has InputFlag.LockUntilRelease
        ownerData.lockThisFrame = flags has InputFlag.LockThisFrame || ownerData.lockUntilRelease
    }

    // This is more or less equivalent to:
    //   if (IsItemHovered() || IsItemActive())
    //       SetKeyOwner(key, GetItemID());
    // Extensive uses of that (e.g. many calls for a single item) may want to manually perform the tests once and then call SetKeyOwner() multiple times.
    // More advanced usage scenarios may want to call SetKeyOwner() manually based on different condition.
    // Worth noting is that only one item can be hovered and only one item can be active, therefore this usage pattern doesn't need to bother with routing and priority.
    fun Key.setItemKeyOwner(flags_: InputFlags = 0) { // Set key owner to last item if it is hovered or active. Equivalent to 'if (IsItemHovered() || IsItemActive()) { SetKeyOwner(key, GetItemID());'.
        var flags = flags_
        val id = g.lastItemData.id
        if (id == 0 || (g.hoveredId != id && g.activeId != id))
            return
        if (flags hasnt InputFlag.CondMask_)
            flags /= InputFlag.CondDefault_
        if ((g.hoveredId == id && flags has InputFlag.CondHovered) || (g.activeId == id && flags has InputFlag.CondActive)) {
            assert(flags hasnt InputFlag.SupportedBySetItemKeyOwner) { "Passing flags not supported by this function !" }
            setOwner(id, flags wo InputFlag.CondMask_)
        }
    }

    // TestKeyOwner(..., ID)   : (owner == None || owner == ID)
    // TestKeyOwner(..., None) : (owner == None)
    // TestKeyOwner(..., Any)  : no owner test
    // All paths are also testing for key not being locked, for the rare cases that key have been locked with using ImGuiInputFlags_LockXXX flags.
    infix fun Key.testOwner(ownerId: ID): Boolean {                       // Test that key is either not owned, either owned by 'owner_id'

        if (!isNamedOrMod)
            return true

        if (g.activeIdUsingAllKeyboardKeys && ownerId != g.activeId)
            if (i in Key.Keyboard_BEGIN until Key.Keyboard_END || this == Key.Mod_Ctrl || this == Key.Mod_Shift || this == Key.Mod_Alt || this == Key.Mod_Super)
                return false

        val ownerData = ownerData
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

    val Key.ownerData: KeyOwnerData
        get() {
            var key = this
            if (key has Key.Mod_Mask_)
                key = key.convertSingleModFlagToKey()
            return g.keysOwnerData[key.ordinal]
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
    fun Key.isPressed(ownerId: ID, flags: InputFlags = 0): Boolean { // Important: when transitioning from old to new IsKeyPressed(): old API has "bool repeat = true", so would default to repeat. New API requiress explicit ImGuiInputFlags_Repeat.
        val keyData = data
        if (!keyData.down) // In theory this should already be encoded as (DownDuration < 0.0f), but testing this facilitates eating mechanism (until we finish work on key ownership)
            return false
        val t = keyData.downDuration
        if (t < 0f)
            return false
        assert(flags hasnt InputFlag.SupportedByIsKeyPressed) { "Passing flags not supported by this function !" }

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
        return g.io.mouseDown[i] && toKey() testOwner ownerId // Should be same as IsKeyDown(MouseButtonToKey(button), owner_id), but this allows legacy code hijacking the io.Mousedown[] array.
    }

    /** ~IsMouseClicked */
    fun MouseButton.isClicked(ownerId: ID, flags: InputFlags = 0): Boolean {

        //        IM_ASSERT(button >= 0 && button < IM_ARRAYSIZE(g.IO.MouseDown));
        if (!g.io.mouseDown[i]) // In theory this should already be encoded as (DownDuration < 0.0f), but testing this facilitates eating mechanism (until we finish work on key ownership)
            return false
        val t = g.io.mouseDownDuration[i]
        if (t < 0f)
            return false
        assert(flags hasnt InputFlag.SupportedByIsKeyPressed) { "Passing flags not supported by this function !" }

        val repeat = flags has InputFlag.Repeat
        val pressed = t == 0f || (repeat && t > g.io.keyRepeatDelay && calcTypematicRepeatAmount(t - g.io.deltaTime, t, g.io.keyRepeatDelay, g.io.keyRepeatRate) > 0)
        if (!pressed)
            return false

        return toKey().testOwner(ownerId)
    }

    /** ~IsMouseReleased */
    infix fun MouseButton.isReleased(ownerId: ID): Boolean {
        //        IM_ASSERT(button >= 0 && button < IM_ARRAYSIZE(g.IO.MouseDown));
        return g.io.mouseReleased[i] && toKey() testOwner ownerId // Should be same as IsKeyReleased(MouseButtonToKey(button), owner_id)
    }

    // [EXPERIMENTAL] Shortcut Routing
    // - ImGuiKeyChord = a ImGuiKey optionally OR-red with ImGuiMod_Alt/ImGuiMod_Ctrl/ImGuiMod_Shift/ImGuiMod_Super.
    //     ImGuiKey_C                 (accepted by functions taking ImGuiKey or ImGuiKeyChord)
    //     ImGuiKey_C | ImGuiMod_Ctrl (accepted by functions taking ImGuiKeyChord)
    //   ONLY ImGuiMod_XXX values are legal to 'OR' with an ImGuiKey. You CANNOT 'OR' two ImGuiKey values.
    // - When using one of the routing flags (e.g. ImGuiInputFlags_RouteFocused): routes requested ahead of time given a chord (key + modifiers) and a routing policy.
    // - Routes are resolved during NewFrame(): if keyboard modifiers are matching current ones: SetKeyOwner() is called + route is granted for the frame.
    // - Route is granted to a single owner. When multiple requests are made we have policies to select the winning route.
    // - Multiple read sites may use the same owner id and will all get the granted route.
    // - For routing: when owner_id is 0 we use the current Focus Scope ID as a default owner in order to identify our location.

    // - Need to decide how to handle shortcut translations for Non-Mac <> Mac
    // - Ideas: https://github.com/ocornut/imgui/issues/456#issuecomment-264390864
    fun shortcut(keyChord: KeyChord, ownerId: ID = 0, flags_: InputFlags = 0): Boolean {

        var flags = flags_
        // When using (owner_id == 0/Any): SetShortcutRouting() will use CurrentFocusScopeId and filter with this, so IsKeyPressed() is fine with he 0/Any.
        if (flags hasnt InputFlag.RouteMask_)
            flags /= InputFlag.RouteFocused
        if (!setShortcutRouting(keyChord, ownerId, flags))
            return false

        var key = Key of (keyChord wo Key.Mod_Mask_)
        val mods = Key of (keyChord and Key.Mod_Mask_)
        if (g.io.keyMods != mods.i)
            return false

        // Special storage location for mods
        if (key == Key.None)
            key = mods.convertSingleModFlagToKey()

        if (!key.isPressed(ownerId, flags and (InputFlag.Repeat or InputFlag.RepeatRateMask_)))
            return false
        assert(flags hasnt InputFlag.SupportedByShortcut) { "Passing flags not supported by this function !" }

        return true
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

    // Request a desired route for an input chord (key + mods).
    // Return true if the route is available this frame.
    // - Routes and key ownership are attributed at the beginning of next frame based on best score and mod state.
    //   (Conceptually this does a "Submit for next frame" + "Test for current frame".
    //   As such, it could be called TrySetXXX or SubmitXXX, or the Submit and Test operations should be separate.)
    // - Using 'owner_id == ImGuiKeyOwner_Any/0': auto-assign an owner based on current focus scope (each window has its focus scope by default)
    // - Using 'owner_id == ImGuiKeyOwner_None': allows disabling/locking a shortcut.
    fun setShortcutRouting(keyChord: KeyChord, ownerId: ID, flags_: InputFlags): Boolean {

        var flags = flags_
        if (flags hasnt InputFlag.RouteMask_)
            flags /= InputFlag.RouteGlobalHigh // IMPORTANT: This is the default for SetShortcutRouting() but NOT Shortcut()
        else
            assert((flags and InputFlag.RouteMask_).isPowerOfTwo) { "Check that only 1 routing flag is used" }

        if (flags has InputFlag.RouteUnlessBgFocused)
            if (g.navWindow == null)
                return false
        if (flags has InputFlag.RouteAlways)
            return true

        val score = calcRoutingScore(g.currentWindow!!, ownerId, flags)
        if (score == 255)
            return false

        // Submit routing for NEXT frame (assuming score is sufficient)
        // FIXME: Could expose a way to use a "serve last" policy for same score resolution (using <= instead of <).
        val routingData = getShortcutRoutingData(keyChord)
        val routingId = getRoutingIdFromOwnerId(ownerId)
        //const bool set_route = (flags & ImGuiInputFlags_ServeLast) ? (score <= routing_data->RoutingNextScore) : (score < routing_data->RoutingNextScore);
        if (score < routingData.routingNextScore) {
            routingData.routingNext = routingId
            routingData.routingNextScore = score
        }

        // Return routing state for CURRENT frame
        return routingData.routingCurr == routingId
    }

    // Currently unused by core (but used by tests)
    // Note: this cannot be turned into GetShortcutRouting() because we do the owner_id->routing_id translation, name would be more misleading.
    fun testShortcutRouting(keyChord: KeyChord, ownerId: ID): Boolean {
        val routingId = getRoutingIdFromOwnerId(ownerId)
        val routingData = getShortcutRoutingData(keyChord)
        return routingData.routingCurr == routingId
    }

    fun getShortcutRoutingData(keyChord: KeyChord): KeyRoutingData {
        // Majority of shortcuts will be Key + any number of Mods
        // We accept _Single_ mod with ImGuiKey_None.
        //  - Shortcut(ImGuiKey_S | ImGuiMod_Ctrl);                    // Legal
        //  - Shortcut(ImGuiKey_S | ImGuiMod_Ctrl | ImGuiMod_Shift);   // Legal
        //  - Shortcut(ImGuiMod_Ctrl);                                 // Legal
        //  - Shortcut(ImGuiMod_Ctrl | ImGuiMod_Shift);                // Not legal
        val rt = g.keysRoutingTable
        var key = Key of (keyChord wo Key.Mod_Mask_)
        val mods = Key of (keyChord and Key.Mod_Mask_)
        if (key == Key.None)
            key = key.convertSingleModFlagToKey()
        //        IM_ASSERT(IsNamedKey(key))

        // Get (in the majority of case, the linked list will have one element so this should be 2 reads.
        // Subsequent elements will be contiguous in memory as list is sorted/rebuilt in NewFrame).
        var idx = rt.index[key]
        while (idx != -1) {
            val routingData = rt.entries[idx]
            if (routingData.mods == mods.i)
                return routingData
            idx = routingData.nextEntryIndex
        }

        // Add to linked-list
        val routingDataIdx = rt.entries.size
        rt.entries += KeyRoutingData()
        val routingData = rt.entries[routingDataIdx]
        routingData.mods = mods.i
        routingData.nextEntryIndex = rt.index[key] // Setup linked list
        rt.index[key] = routingDataIdx
        return routingData
    }
}