package imgui.api

import imgui.*
import imgui.ImGui.calcRoutingScore
import imgui.ImGui.convertShortcutMod
import imgui.ImGui.convertSingleModFlagToKey
import imgui.ImGui.getRoutingIdFromOwnerId
import imgui.ImGui.isPressed
import imgui.internal.classes.InputFlag
import imgui.internal.classes.InputFlags
import imgui.internal.sections.KeyRoutingData

// Inputs Utilities: Shortcut testing (with Routing Resolution)
// - ImGuiKeyChord = a ImGuiKey optionally OR-red with ImGuiMod_Alt/ImGuiMod_Ctrl/ImGuiMod_Shift/ImGuiMod_Super/ImGuiMod_Shortcut.
//     ImGuiKey_C                 (accepted by functions taking ImGuiKey or ImGuiKeyChord)
//     ImGuiKey_C | ImGuiMod_Ctrl (accepted by functions taking ImGuiKeyChord)
//   ONLY ImGuiMod_XXX values are legal to 'OR' with an ImGuiKey. You CANNOT 'OR' two ImGuiKey values.
// - The general idea of routing is that multiple locations may register interest in a shortcut,
//   and only one location will be granted access to the shortcut.
// - The default routing policy (ImGuiInputFlags_RouteFocused) checks for current window being in
//   the focus stack, and route the shortcut to the deepest requesting window in the focus stack.
// - Consider Shortcut() to be a widget: the calling location matters + it has side-effects as shortcut routes are
//   registered into the system (for it to be able to pick the best one). This is why this is not called 'IsShortcutPressed()'.
// - If this is called for a specific widget, pass its ID as 'owner_id' in order for key ownership and routing priorities
//   to be honored (e.g. with default ImGuiInputFlags_RouteFocused, the highest priority is given to active item).
interface inputsUtilitiesShortcutRouting {


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
    fun shortcut(keyChord_: KeyChord, ownerId: ID = 0, flags_: InputFlags = none): Boolean {

        var flags = flags_
        // When using (owner_id == 0/Any): SetShortcutRouting() will use CurrentFocusScopeId and filter with this, so IsKeyPressed() is fine with he 0/Any.
        if (flags hasnt InputFlag.RouteMask)
            flags /= InputFlag.RouteFocused
        if (!setShortcutRouting(keyChord_, ownerId, flags))
            return false

        val keyChord = if (keyChord_ has Key.Mod_Shortcut) convertShortcutMod(keyChord_) else keyChord_
        // [JVM] don't attempt finding a `Key` with mods, it might not exist and crash, keep it as an `Int`
        val mods = keyChord and Key.Mod_Mask
        if (g.io.keyMods != mods)
            return false

        // Special storage location for mods
        var key = Key of (keyChord wo Key.Mod_Mask)
        if (key == Key.None)
            key = (Key of mods).convertSingleModFlagToKey()

        if (!key.isPressed(ownerId, flags and (InputFlag.Repeat or InputFlag.RepeatRateMask)))
            return false
        assert((flags wo InputFlag.SupportedByShortcut).isEmpty) { "Passing flags not supported by this function !" }

        return true
    }

    // Request a desired route for an input chord (key + mods).
    // Return true if the route is available this frame.
    // - Routes and key ownership are attributed at the beginning of next frame based on best score and mod state.
    //   (Conceptually this does a "Submit for next frame" + "Test for current frame".
    //   As such, it could be called TrySetXXX or SubmitXXX, or the Submit and Test operations should be separate.)
    // - Using 'owner_id == ImGuiKeyOwner_Any/0': auto-assign an owner based on current focus scope (each window has its focus scope by default)
    // - Using 'owner_id == ImGuiKeyOwner_None': allows disabling/locking a shortcut.
    fun setShortcutRouting(keyChord: KeyChord, ownerId: ID = 0, flags_: InputFlags = none): Boolean {

        var flags = flags_
        if (flags hasnt InputFlag.RouteMask)
            flags /= InputFlag.RouteGlobalHigh // IMPORTANT: This is the default for SetShortcutRouting() but NOT Shortcut()
        else
            assert((flags and InputFlag.RouteMask).isPowerOfTwo) { "Check that only 1 routing flag is used" }

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
        val routingData = getShortcutRoutingData(keyChord) // FIXME: Could avoid creating entry.
        return routingData.routingCurr == routingId
    }

    fun getShortcutRoutingData(keyChord_: KeyChord): KeyRoutingData {
        // Majority of shortcuts will be Key + any number of Mods
        // We accept _Single_ mod with ImGuiKey_None.
        //  - Shortcut(ImGuiKey_S | ImGuiMod_Ctrl);                    // Legal
        //  - Shortcut(ImGuiKey_S | ImGuiMod_Ctrl | ImGuiMod_Shift);   // Legal
        //  - Shortcut(ImGuiMod_Ctrl);                                 // Legal
        //  - Shortcut(ImGuiMod_Ctrl | ImGuiMod_Shift);                // Not legal
        val rt = g.keysRoutingTable
        val keyChord = if (keyChord_ has Key.Mod_Shortcut) convertShortcutMod(keyChord_) else keyChord_
        var key = Key of (keyChord wo Key.Mod_Mask)
        // [JVM] don't attempt finding a `Key` with mods, it might not exist and crash, keep it as an `Int`
        val mods = keyChord and Key.Mod_Mask
        if (key == Key.None)
            key = key.convertSingleModFlagToKey()
        //        IM_ASSERT(IsNamedKey(key))

        // Get (in the majority of case, the linked list will have one element so this should be 2 reads.
        // Subsequent elements will be contiguous in memory as list is sorted/rebuilt in NewFrame).
        var idx = rt.index[key]
        while (idx != -1) {
            val routingData = rt.entries[idx]
            if (routingData.mods == mods)
                return routingData
            idx = routingData.nextEntryIndex
        }

        // Add to linked-list
        val routingDataIdx = rt.entries.size
        rt.entries += KeyRoutingData()
        val routingData = rt.entries[routingDataIdx]
        routingData.mods = mods
        routingData.nextEntryIndex = rt.index[key] // Setup linked list
        rt.index[key] = routingDataIdx
        return routingData
    }
}