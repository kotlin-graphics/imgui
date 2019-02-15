package imgui.imgui

import imgui.ImGui.button
import imgui.ImGui.popAllowKeyboardFocus
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushAllowKeyboardFocus
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.ImGui.sameLine
import imgui.ImGui.setClipboardText
import imgui.ImGui.sliderInt
import imgui.g
import java.io.File
import java.io.FileWriter

/** Logging/Capture
 *  - All text output from the interface can be captured into tty/file/clipboard.
 *  By default, tree nodes are automatically opened during logging.     */
interface imgui_logging {

//    IMGUI_API void          LogToTTY(int max_depth = -1);                                       // start logging to tty (stdout)
//    IMGUI_API void          LogToFile(int max_depth = -1, const char* filename = NULL);         // start logging to file

    /** start logging ImGui output to OS clipboard   */
    fun logToClipboard(maxDepth: Int = -1) {

        if (g.logEnabled) return

        val window = g.currentWindow!!

        assert(g.logFile != null)
        g.logFile = null
        g.logEnabled = true
        g.logStartDepth = window.dc.treeDepth
        if (maxDepth >= 0)
            g.logAutoExpandMaxDepth = maxDepth
    }


    /** stop logging (close file, etc.) */
    fun logFinish() {

        if (!g.logEnabled) return

        logText("%s", "\n")

        if (g.logFile != null) {
            g.logFile = null
        }
        if (g.logClipboard.length > 1) {
            setClipboardText(g.logClipboard.toString())
            g.logClipboard = StringBuilder()
        }
        g.logEnabled = false
    }

    /** Helper to display buttons for logging to tty/file/clipboard */
    fun logButtons() {
        pushId("LogButtons")
        val logToTty = button("Log To TTY"); sameLine()
        val logToFile = button("Log To File"); sameLine()
        val logToClipboard = button("Log To Clipboard"); sameLine()
        pushItemWidth(80f)
        pushAllowKeyboardFocus(false)
        sliderInt("Depth", g::logAutoExpandMaxDepth, 0, 9)
        popAllowKeyboardFocus()
        popItemWidth()
        popId()

        // Start logging at the end of the function so that the buttons don't appear in the log
        if (logToTty) TODO()//LogToTTY(g.LogAutoExpandMaxDepth)
        if (logToFile) logToFile(g.logAutoExpandMaxDepth, g.logFile)
        if (logToClipboard) logToClipboard(g.logAutoExpandMaxDepth)
    }

    fun logToFile(maxDepth: Int, file_: File?) {
        if(g.logEnabled)
            return
        val window = g.currentWindow!!

        g.logFile = file_ ?: g.logFile ?: return

        g.logEnabled = true
        g.logStartDepth = window.dc.treeDepth
        if (maxDepth >= 0)
            g.logAutoExpandMaxDepth = maxDepth
    }

    /** pass text data straight to log (without being displayed)    */
    fun logText(fmt: String, vararg args: Any) {
        if (!g.logEnabled)
            return

        val strArgs = Array(args.size) { args[it].toString() }

        if (g.logFile != null) {
            val writer = FileWriter(g.logFile, true)
            writer.write(String.format(fmt, *strArgs))
        } else {
            g.logClipboard.append(fmt.format(*strArgs))
        }
    }
}