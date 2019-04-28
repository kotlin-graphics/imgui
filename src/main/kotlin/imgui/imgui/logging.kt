package imgui.imgui

import imgui.ImGui.button
import imgui.ImGui.clipboardText
import imgui.ImGui.logBegin
import imgui.ImGui.popAllowKeyboardFocus
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushAllowKeyboardFocus
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.ImGui.sameLine
import imgui.ImGui.sliderInt
import imgui.g
import imgui.internal.LogType
import java.io.File
import java.io.FileWriter

/** Logging/Capture
 *  - All text output from the interface can be captured into tty/file/clipboard.
 *  By default, tree nodes are automatically opened during logging.     */
interface imgui_logging {

    /** Start logging/capturing text output to TTY */
    fun logToTTY(autoOpenDepth: Int  = -1): Nothing = TODO()

    /** Start logging/capturing text output to given file */
    fun logToFile(autoOpenDepth: Int  = -1, filename: String? = null): Nothing = TODO()

    /** start logging ImGui output to OS clipboard   */
    fun logToClipboard(autoOpenDepth: Int = -1) {

        if (g.logEnabled) return

        logBegin(LogType.Clipboard, autoOpenDepth)
    }

    /** stop logging (close file, etc.) */
    fun logFinish() {

        if (!g.logEnabled) return

        logText("%s", "\n")

        when (g.logType) {
            LogType.TTY -> TODO()//fflush(g.LogFile)
            LogType.File -> TODO()//fclose(g.LogFile)
            LogType.Buffer -> Unit
            LogType.Clipboard -> {
                if (g.logBuffer.length > 1) { // TODO 1? maybe 0?
                    clipboardText = g.logBuffer.toString()
                    g.logBuffer = StringBuilder()
                }
            }
            else -> error("Invalid log type")
        }

        g.logEnabled = false
        g.logType = LogType.None
        g.logFile = null
        g.logBuffer.clear()
    }

    /** Helper to display buttons for logging to tty/file/clipboard
     *  FIXME-OBSOLETE: We should probably obsolete this and let the user have their own helper (this is one of the oldest function alive!) */
    fun logButtons() {
        pushId("LogButtons")
        val logToTty = button("Log To TTY"); sameLine()
        val logToFile = button("Log To File"); sameLine()
        val logToClipboard = button("Log To Clipboard"); sameLine()
        pushItemWidth(80f)
        pushAllowKeyboardFocus(false)
        sliderInt("Default Depth", g::logDepthToExpandDefault, 0, 9)
        popAllowKeyboardFocus()
        popItemWidth()
        popId()

        // Start logging at the end of the function so that the buttons don't appear in the log
        if (logToTty) logToTTY()
        if (logToFile) logToFile()
        if (logToClipboard) logToClipboard()
    }

    fun logToFile(maxDepth: Int, file_: File?) {
        if (g.logEnabled)
            return
        val window = g.currentWindow!!

        g.logFile = file_ ?: g.logFile ?: return

        g.logEnabled = true
        g.logDepthRef = window.dc.treeDepth
        if (maxDepth >= 0)
            g.logDepthToExpandDefault = maxDepth
    }

    /** pass text data straight to log (without being displayed)    */
    fun logText(fmt: String, vararg args: Any) {
        if (!g.logEnabled)
            return

        if (g.logFile != null) {
            val writer = FileWriter(g.logFile, true)
            writer.write(String.format(fmt, *args))
        } else {
            g.logBuffer.append(fmt.format(*args))
        }
    }
}