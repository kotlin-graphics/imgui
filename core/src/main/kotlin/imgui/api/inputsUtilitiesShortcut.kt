package imgui.api

import imgui.*
import imgui.ImGui.convertShortcutMod
import imgui.ImGui.convertSingleModFlagToKey
import imgui.ImGui.isPressed
import imgui.ImGui.setShortcutRouting

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
interface inputsUtilitiesShortcut {

    // [EXPERIMENTAL] Low-Level: Shortcut Routing
    // - Routes are resolved during NewFrame(): if keyboard modifiers are matching current ones: SetKeyOwner() is called + route is granted for the frame.
    // - Route is granted to a single owner. When multiple requests are made we have policies to select the winning route.
    // - Multiple read sites may use the same owner id and will all get the granted route.
    // - For routing: when owner_id is 0 we use the current Focus Scope ID as a default owner in order to identify our location.
    fun shortcut(keyChord_: KeyChord, ownerId: ID = 0, flags_: InputFlags = 0): Boolean {

        var flags = flags_
        // When using (owner_id == 0/Any): SetShortcutRouting() will use CurrentFocusScopeId and filter with this, so IsKeyPressed() is fine with he 0/Any.
        if (flags hasnt InputFlag._RouteMask_)
            flags /= InputFlag.RouteFocused
        if (!setShortcutRouting(keyChord_, ownerId, flags))
            return false

        val keyChord = if (keyChord_ has Key.Mod_Shortcut) convertShortcutMod(keyChord_) else keyChord_
        val mods = Key of (keyChord and Key.Mod_Mask_)
        if (g.io.keyMods != mods.i)
            return false

        // Special storage location for mods
        var key = Key of (keyChord wo Key.Mod_Mask_)
        if (key == Key.None)
            key = mods.convertSingleModFlagToKey()

        if (!key.isPressed(ownerId, flags and (InputFlag.Repeat or InputFlag.RepeatRateMask_)))
            return false
        assert(flags hasnt InputFlag._SupportedByShortcut) { "Passing flags not supported by this function !" }

        return true
    }
}