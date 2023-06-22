package imgui.internal.sections

import imgui.ImGui
import imgui.api.g
import imgui.has
import imgui.internal.classes.DebugLogFlag


// Debug Printing Into TTY
fun IMGUI_DEBUG_PRINTF(fmt: String, vararg args: Any) = println(fmt.format(*args))

// Debug Logging for ShowDebugLogWindow(). This is designed for relatively rare events so please don't spam.
fun IMGUI_DEBUG_LOG(fmt: String, vararg args: Any) = ImGui.debugLog(fmt, *args)

fun IMGUI_DEBUG_LOG_ACTIVEID(fmt: String, vararg args: Any) {
    if (g.debugLogFlags has DebugLogFlag.EventActiveId)
        IMGUI_DEBUG_LOG(fmt, *args)
}

fun IMGUI_DEBUG_LOG_FOCUS(fmt: String, vararg args: Any) {
    if (g.debugLogFlags has DebugLogFlag.EventFocus)
        IMGUI_DEBUG_LOG(fmt, *args)
}

// Debug Logging for selected systems. Remove the '((void)0) //' to enable.
fun IMGUI_DEBUG_LOG_POPUP(fmt: String, vararg args: Any) {
    if (g.debugLogFlags has DebugLogFlag.EventPopup)
        IMGUI_DEBUG_LOG(fmt, *args)
}

fun IMGUI_DEBUG_LOG_NAV(fmt: String, vararg args: Any) {
    if (g.debugLogFlags has DebugLogFlag.EventNav)
        IMGUI_DEBUG_LOG(fmt, *args)
}

fun IMGUI_DEBUG_LOG_SELECTION(fmt: String, vararg args: Any) {
    if (g.debugLogFlags has DebugLogFlag.EventSelection)
        IMGUI_DEBUG_LOG(fmt, *args)
}

fun IMGUI_DEBUG_LOG_CLIPPER(fmt: String, vararg args: Any) {
    if (g.debugLogFlags has DebugLogFlag.EventClipper)
        IMGUI_DEBUG_LOG(fmt, *args)
}

fun IMGUI_DEBUG_LOG_IO(fmt: String, vararg args: Any) {
    if (g.debugLogFlags has DebugLogFlag.EventIO)
        IMGUI_DEBUG_LOG(fmt, *args)
}

fun ASSERT_PARANOID(value: Boolean) = assert(value)
fun ASSERT_PARANOID(value: Boolean, lazyMessage: () -> Any) = assert(value, lazyMessage)