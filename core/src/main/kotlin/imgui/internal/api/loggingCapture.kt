package imgui.internal.api

import glm_.vec2.Vec2
import imgui.ImGui
import imgui.ImGui.logText
import imgui.api.g
import imgui.getOrNul
import imgui.internal.eolRange
import imgui.internal.sections.LogType

/** Logging/Capture */
internal interface loggingCapture {

    /** -> BeginCapture() when we design v2 api, for now stay under the radar by using the old name. */
    /** Start logging/capturing text output */
    fun logBegin(type: LogType, autoOpenDepth: Int) {

        val window = g.currentWindow!!

        assert(!g.logEnabled && g.logFile == null && g.logBuffer.isEmpty())
        g.logEnabled = true
        g.logType = type
//        g.logNextPrefix = g.LogNextSuffix = NULL TODO
        g.logDepthRef = window.dc.treeDepth
        g.logDepthToExpand = autoOpenDepth.takeIf { it >= 0 } ?: g.logDepthToExpandDefault
        g.logLinePosY = Float.MAX_VALUE
        g.logLineFirstItem = true
    }

    /** Start logging/capturing to internal buffer */
    fun logToBuffer(autoOpenDepth: Int = -1) {
        if (g.logEnabled)
            return
        logBegin(LogType.Buffer, autoOpenDepth)
    }

    /** Internal version that takes a position to decide on newline placement and pad items according to their depth.
     *  We split text into individual lines to add current tree level padding
     *  FIXME: This code is a little complicated perhaps, considering simplifying the whole system. */
    fun logRenderedText(refPos: Vec2?, text: String, textEnd: Int = ImGui.findRenderedTextEnd(text)) { // TODO ByteArray?

        val window = g.currentWindow!!

        val prefix = g.logNextPrefix
        val suffix = g.logNextSuffix
        g.logNextPrefix = ""; g.logNextSuffix = ""

//        if (!text_end)
//            text_end = FindRenderedTextEnd(text, text_end)

        val logNewLine = refPos != null && (refPos.y > g.logLinePosY + g.style.framePadding.y + 1)
        if (refPos != null)
            g.logLinePosY = refPos.y
        if (logNewLine) {
            logText("\n")
            g.logLineFirstItem = true
        }

        if (prefix.isNotEmpty())
            logRenderedText(refPos, prefix, prefix.length) // Calculate end ourself to ensure "##" are included here.

        // Re-adjust padding if we have popped out of our starting depth
        if (g.logDepthRef > window.dc.treeDepth)
            g.logDepthRef = window.dc.treeDepth
        val treeDepth = window.dc.treeDepth - g.logDepthRef

        var textRemaining = 0//text
        while (true) {
            // Split the string. Each new line (after a '\n') is followed by indentation corresponding to the current depth of our log entry.
            // We don't add a trailing \n yet to allow a subsequent item on the same line to be captured.
            val lineStart = textRemaining
            val lineEnd = text.eolRange(lineStart, textEnd)
            val isLastLine = lineEnd == textEnd
            if (lineStart != lineEnd || !isLastLine) {
                val lineLength = lineEnd - lineStart
                val indentation = if (g.logLineFirstItem) treeDepth * 4 else 1
                logText(" ".repeat(indentation) + text.substring(lineStart, lineStart + lineLength))
                g.logLineFirstItem = false
                if (text.getOrNul(lineEnd) == '\n') {
                    logText("\n")
                    g.logLineFirstItem = true
                }
            }
            if (isLastLine)
                break
            textRemaining = lineEnd + 1
        }

        if (suffix.isNotEmpty())
            logRenderedText(refPos, suffix, suffix.length)
    }

    /** Important: doesn't copy underlying data, use carefully (prefix/suffix must be in scope at the time of the next LogRenderedText) */
    fun logSetNextTextDecoration(prefix: String, suffix: String) { // TODO check where this is called, missed some porting
        g.logNextPrefix = prefix
        g.logNextSuffix = suffix
    }
}