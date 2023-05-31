package imgui.internal.api

import imgui.ID
import imgui.IMGUI_DEBUG_INI_SETTINGS
import imgui.ImGui.findWindowByName
import imgui.WindowFlag
import imgui.api.g
import imgui.div
import imgui.internal.classes.Window
import imgui.internal.hashStr
import imgui.internal.sections.WindowSettings
import imgui.statics.initOrLoadWindowSettings

// Settings - Windows
interface settingsWindows {

    fun createNewWindowSettings(name_: String): WindowSettings {
        val name = when {
            // Skip to the "###" marker if any. We don't skip past to match the behavior of GetID()
            // Preserve the full string when IMGUI_DEBUG_INI_SETTINGS is set to make .ini inspection easier.
            !IMGUI_DEBUG_INI_SETTINGS -> name_.removePrefix("###")
            else -> name_
        }
        return WindowSettings(name).also {
            g.settingsWindows += it
        }
    }

    // We don't provide a FindWindowSettingsByName() because Docking system doesn't always hold on names.
    // This is called once per window .ini entry + once per newly instanciated window.
    fun findWindowSettingsByID(id: ID): WindowSettings? = g.settingsWindows.find { it.id == id }

    // This is faster if you are holding on a Window already as we don't need to perform a search.
    fun findWindowSettingsByWindow(window: Window): WindowSettings? {
        if (window.settingsOffset != -1)
            return g.settingsWindows[window.settingsOffset]
        return findWindowSettingsByID(window.id)
    }

    // This will revert window to its initial state, including enabling the ImGuiCond_FirstUseEver/ImGuiCond_Once conditions once more.
    fun clearWindowSettings(name: String) {
        //IMGUI_DEBUG_LOG("ClearWindowSettings('%s')\n", name);
        val window = findWindowByName(name)
        if (window != null) {
            window.flags /= WindowFlag.NoSavedSettings
            initOrLoadWindowSettings(window, null)
        }
        val settings = window?.let { findWindowSettingsByWindow(window) } ?: findWindowSettingsByID(hashStr(name))
        settings?.wantDelete = true
    }
}