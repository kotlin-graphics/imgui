package imgui.internal.api

import imgui.api.g
import imgui.internal.LogType

/** Logging/Capture */
internal interface loggingCapture {

    /** -> BeginCapture() when we design v2 api, for now stay under the radar by using the old name. */
    /** Start logging/capturing text output */
    fun logBegin(type: LogType, autoOpenDepth: Int) {

        val window = g.currentWindow!!

        assert(!g.logEnabled && g.logFile == null && g.logBuffer.isEmpty())
        g.logEnabled = true
        g.logType = type
        g.logDepthRef = window.dc.treeDepth
        g.logDepthToExpand = autoOpenDepth.takeIf { it >= 0 } ?: g.logDepthToExpandDefault
        g.logLinePosY = Float.MAX_VALUE
        g.logLineFirstItem = true
    }

    /** Start logging/capturing to internal buffer */
    fun logToBuffer(autoOpenDepth: Int = -1): Nothing = TODO()
}