package imgui.api

import imgui.ImGui.findSettingsHandler
import imgui.internal.sections.SettingsHandler
import java.io.File
import java.nio.file.Paths


/*  Settings/.Ini Utilities
    - The disk functions are automatically called if io.IniFilename != NULL (default is "imgui.ini").
    - Set io.IniFilename to NULL to load/save manually. Read io.WantSaveIniSettings description about handling .ini saving manually. */
interface settingsIniUtilities {

    /** call after CreateContext() and before the first call to NewFrame(). NewFrame() automatically calls LoadIniSettingsFromDisk(io.IniFilename). */
    fun loadIniSettingsFromDisk(iniFilename: String?) {
        if (iniFilename == null)
            return
        val file = File(Paths.get(iniFilename).toUri())
        if (file.exists() && file.canRead())
            loadIniSettingsFromMemory(file.readLines())
    }

    /** call after CreateContext() and before the first call to NewFrame() to provide .ini data from your own data source.
     *
     *  Zero-tolerance, no error reporting, cheap .ini parsing */
    fun loadIniSettingsFromMemory(lines: List<String>) {

        assert(g.initialized)
        //IM_ASSERT(!g.WithinFrameScope && "Cannot be called between NewFrame() and EndFrame()");
//        assert(!g.settingsLoaded && g.frameCount == 0)

        // For user convenience, we allow passing a non zero-terminated string (hence the ini_size parameter).
        // For our convenience and to make the code simpler, we'll also write zero-terminators within the buffer. So let's create a writable copy..

        // Call pre-read handlers
        // Some types will clear their data (e.g. dock information) some types will allow merge/override (window)
        for (handler in g.settingsHandlers)
            handler.readInitFn?.invoke(g, handler)

        var entryHandler: SettingsHandler? = null
        var entryData: Any? = null
        for (l in lines) {
            val line = l.trim()
            if (line.isNotEmpty() && line.isNotBlank()) {
                // Skip new lines markers, then find end of the line
                if (line[0] == '[' && line.last() == ']') {
                    // Parse "[Type][Name]". Note that 'Name' can itself contains [] characters, which is acceptable with the current format and parsing code.
                    val firstCloseBracket = line.indexOf(']')
                    if (firstCloseBracket != -1 && line[firstCloseBracket + 1] == '[') {
                        val type = line.substring(1, firstCloseBracket)
                        val name = line.substring(firstCloseBracket + 2, line.lastIndex)
                        entryHandler = findSettingsHandler(type)
                        entryData = entryHandler?.readOpenFn?.invoke(g, entryHandler, name)
//                        val typeHash = hash(type)
//                        settings = findWindowSettings(typeHash) ?: createNewWindowSettings(name)
                    }
                } else if (entryHandler != null && entryData != null)
                // Let type handler parse the line
                    entryHandler.readLineFn!!(g, entryHandler, entryData, line)
            }
        }
        g.settingsLoaded = true

        // Call post-read handlers
        for (handler in g.settingsHandlers)
            handler.applyAllFn?.invoke(g, handler)
    }

    /** this is automatically called (if io.IniFilename is not empty) a few seconds after any modification that should be reflected in the .ini file (and also by DestroyContext). */
    fun saveIniSettingsToDisk(iniFilename: String?) {

        g.settingsDirtyTimer = 0f
        if (iniFilename == null)
            return

        val file = File(Paths.get(iniFilename).toUri())
        if (file.exists() && file.canWrite())
            file.writeText(saveIniSettingsToMemory())
    }

    /** Call registered handlers (e.g. SettingsHandlerWindow_WriteAll() + custom handlers) to write their stuff into a text buffer */
    fun saveIniSettingsToMemory(): String {
        g.settingsDirtyTimer = 0f
        val buf = StringBuilder()
        for (handler in g.settingsHandlers)
            handler.writeAllFn!!(g, handler, buf)
        g.settingsIniData = buf.toString()
        return g.settingsIniData
    }
}