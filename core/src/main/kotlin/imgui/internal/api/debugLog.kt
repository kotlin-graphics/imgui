package imgui.internal.api

import imgui.api.g
import imgui.internal.classes.DebugLogFlag
import imgui.internal.sections.IMGUI_DEBUG_PRINTF

// Debug Log

//-----------------------------------------------------------------------------
// [SECTION] DEBUG LOG WINDOW
//-----------------------------------------------------------------------------

internal interface debugLog {

    fun debugLog(fmt: String, vararg args: Any) {
        val oldSize = g.debugLogBuf.length
        g.debugLogBuf.append("[%05d] ".format(g.frameCount))
        g.debugLogBuf.append(fmt.format(*args))
        if (g.debugLogFlags has DebugLogFlag.OutputToTTY)
            IMGUI_DEBUG_PRINTF(g.debugLogBuf.drop(oldSize).toString())
//        g.DebugLogIndex.append(g.DebugLogBuf.c_str(), old_size, g.DebugLogBuf.size());
    }
}