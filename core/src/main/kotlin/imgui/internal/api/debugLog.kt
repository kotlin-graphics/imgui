package imgui.internal.api

import imgui.api.g

// Debug Log
internal interface debugLog {

    fun debugLog(fmt: String, vararg args: Any) {
        g.debugLogBuf.append("[%05d] ", g.frameCount)
        g.debugLogBuf.appendLine(fmt.format(*args))
    }
}