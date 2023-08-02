package imgui.internal.api

import imgui.ImGui.io
import imgui.WindowFlag
import imgui.api.g
import imgui.hasnt
import imgui.internal.classes.Window
import imgui.internal.hashStr
import imgui.internal.sections.SettingsHandler

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