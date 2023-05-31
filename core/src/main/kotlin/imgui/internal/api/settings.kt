package imgui.internal.api

import imgui.ID
import imgui.IMGUI_DEBUG_INI_SETTINGS
import imgui.ImGui.io
import imgui.WindowFlag
import imgui.api.g
import imgui.hasnt
import imgui.internal.classes.Window
import imgui.internal.hashStr
import imgui.internal.sections.SettingsHandler
import imgui.internal.sections.WindowSettings

/** Settings */
internal interface settings {

    fun markIniSettingsDirty() {
        if (g.settingsDirtyTimer <= 0f)
            g.settingsDirtyTimer = io.iniSavingRate
    }

    fun Window.markIniSettingsDirty() {
        if (flags hasnt WindowFlag.NoSavedSettings && g.settingsDirtyTimer <= 0f)
            g.settingsDirtyTimer = io.iniSavingRate
    }

    // MarkIniSettingsDirty(ImGuiWindow* window) -> Window class

    // Clear all settings (windows, tables, docking etc.)
    fun clearIniSettings() {
        g.settingsIniData = ""
        for (handler in g.settingsHandlers)
            handler.clearAllFn?.invoke(g, handler)
        g.settingsWindows.clear()
    }

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

    fun addSettingsHandler(handler: SettingsHandler) {
        assert(findSettingsHandler(handler.typeName) == null)
        g.settingsHandlers += handler
    }

    fun removeSettingsHandler(typeName: String) {
        findSettingsHandler(typeName)?.let { handler ->
            g.settingsHandlers -= handler
        }
    }

    fun findSettingsHandler(typeName: String): SettingsHandler? {
        val typeHash = hashStr(typeName)
        return g.settingsHandlers.find { it.typeHash == typeHash }
    }
}