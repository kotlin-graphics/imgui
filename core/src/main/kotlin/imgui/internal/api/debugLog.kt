package imgui.internal.api

import imgui.api.g
import imgui.internal.classes.DebugLogFlag
import imgui.internal.classes.has
import imgui.internal.sections.IMGUI_DEBUG_PRINTF

// Debug Log

//-----------------------------------------------------------------------------
// [SECTION] DEBUG LOG WINDOW
//-----------------------------------------------------------------------------

internal interface debugLog {

    fun debugLog(fmt: String, vararg args: Any) {
//        val oldSize = g.debugLogBuf.length
        g.debugLogBuf.append("[%05d] ", g.frameCount)
        g.debugLogBuf.appendLine(fmt.format(*args))
        if (g.debugLogFlags has DebugLogFlag.OutputToTTY)
            IMGUI_DEBUG_PRINTF(g.debugLogBuf.toString())
    }
}