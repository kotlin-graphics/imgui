package imgui.imgui

import glm_.glm
import glm_.i
import glm_.vec2.Vec2i
import imgui.ImGui.style
import imgui.createNewWindowSettings
import imgui.findWindowSettings
import imgui.has
import imgui.internal.WindowSettings
import imgui.internal.hash
import java.io.File
import java.nio.file.Paths
import imgui.WindowFlag as Wf


/*  Settings/.Ini Utilities
    - The disk functions are automatically called if io.IniFilename != NULL (default is "imgui.ini").
    - Set io.IniFilename to NULL to load/save manually. Read io.WantSaveIniSettings description about handling .ini saving manually. */
interface settingsIniUtilities {

    /** call after CreateContext() and before the first call to NewFrame(). NewFrame() automatically calls LoadIniSettingsFromDisk(io.IniFilename). */
    fun loadIniSettingsFromDisk(iniFilename: String?) {
        if (iniFilename == null) return
        var settings: WindowSettings? = null
        val a = fileLoadToLines(iniFilename)
        fileLoadToLines(iniFilename)?.filter { it.isNotEmpty() }?.forEach { s ->
            if (s[0] == '[' && s.last() == ']') {
                /*  Parse "[Type][Name]". Note that 'Name' can itself contains [] characters, which is acceptable with
                    the current format and parsing code.                 */
                val firstCloseBracket = s.indexOf(']')
                val name: String
                val type: String
                if (firstCloseBracket != s.length - 1) { // Import legacy entries that have no type
                    type = s.substring(1, firstCloseBracket)
                    name = s.substring(firstCloseBracket + 2, s.length - 1)
                } else {
                    type = "Window"
                    name = s.substring(1, firstCloseBracket)
                }
                val typeHash = hash(type)
                settings = findWindowSettings(typeHash) ?: createNewWindowSettings(name)
            } else settings?.apply {
                when {
                    s.startsWith("Pos") -> pos.put(s.substring(4).split(","))
                    s.startsWith("Size") -> size put glm.max(Vec2i(s.substring(5).split(",")), style.windowMinSize)
                    s.startsWith("Collapsed") -> collapsed = s.substring(10).toBoolean()
                }
            }
        }
        g.settingsLoaded = true
    }

    /** this is automatically called (if io.IniFilename is not empty) a few seconds after any modification that should be reflected in the .ini file (and also by DestroyContext). */
    fun saveIniSettingsToDisk(iniFilename: String?) {

        g.settingsDirtyTimer = 0f
        if (iniFilename == null) return

        // Gather data from windows that were active during this session (if a window wasn't opened in this session we preserve its settings)
        for (window in g.windows) {

            if (window.flags has Wf.NoSavedSettings) continue
            /** This will only return NULL in the rare instance where the window was first created with
             *  WindowFlag.NoSavedSettings then had the flag disabled later on.
             *  We don't bind settings in this case (bug #1000).    */
            val settings = g.settingsWindows.getOrNull(window.settingsIdx) ?: findWindowSettings(window.id)
            ?: createNewWindowSettings(window.name).also { window.settingsIdx = g.settingsWindows.indexOf(it) }
            assert(settings.id == window.id)
            settings.pos put window.pos
            settings.size put window.sizeFull
            settings.collapsed = window.collapsed
        }

        //  Write .ini file
        File(Paths.get(iniFilename).toUri()).printWriter().use {
            for (setting in g.settingsWindows) {
                if (setting.pos.x == Float.MAX_VALUE) continue
                // Skip to the "###" marker if any. We don't skip past to match the behavior of GetID()
                val name = setting.name.substringBefore("###")
                it.println("[Window][$name]")   // TODO [%s][%s]\n", handler->TypeName, name
                it.println("Pos=${setting.pos.x},${setting.pos.y}")
                it.println("Size=${setting.size.x.i},${setting.size.y.i}")
                it.println("Collapsed=${setting.collapsed.i}")
                it.println()
            }
        }
    }

    companion object {

        fun fileLoadToLines(filename: String): List<String>? {
            val file = File(Paths.get(filename).toUri())
            return file.takeIf { it.exists() && it.canRead() }?.readLines()
        }
    }
}