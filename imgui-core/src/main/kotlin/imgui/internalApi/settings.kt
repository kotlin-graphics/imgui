package imgui.internalApi

import imgui.ID
import imgui.IMGUI_DEBUG_INI_SETTINGS
import imgui.ImGui.io
import imgui.api.g
import imgui.classes.WindowSettings
import imgui.hash

/** Settings */
interface settings {

    fun markIniSettingsDirty() {
        if (g.settingsDirtyTimer <= 0f)
            g.settingsDirtyTimer = io.iniSavingRate
    }

    // MarkIniSettingsDirty(ImGuiWindow* window) -> Window class

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

    fun findWindowSettings(id: ID): WindowSettings? =
            g.settingsWindows.firstOrNull { it.id == id }

    fun findOrCreateWindowSettings(name: String): WindowSettings =
            findWindowSettings(hash(name)) ?: createNewWindowSettings(name)
}