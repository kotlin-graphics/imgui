package imgui.internal.api

import imgui.IMGUI_DEBUG_INI_SETTINGS
import imgui.api.g
import imgui.internal.classes.Window
import imgui.internal.hashStr
import imgui.internal.sections.WindowSettings

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

    // This is called once per window .ini entry + once per newly instanciated window.
    fun findWindowSettingsByName(name: String): WindowSettings? {
        val id = hashStr(name)
        return g.settingsWindows.find { it.id == id }
    }

    // This is faster if you are holding on a Window already as we don't need to perform a search.
    fun findWindowSettingsByWindow(window: Window): WindowSettings? {
        if (window.settingsOffset != -1)
            return g.settingsWindows[window.settingsOffset]
        return findWindowSettingsByName(window.name) // Actual search executed once, so at this point we don't mind the redundant hashing.
    }
}