package imgui.internal.sections

import imgui.DEBUG
import imgui.api.g


fun IMGUI_DEBUG_LOG(fmt: String, vararg args: Any) = println("[%05d] ".format(g.frameCount) + fmt.format(*args))

// Debug Logging for selected systems. Remove the '((void)0) //' to enable.
fun IMGUI_DEBUG_LOG_POPUP(fmt: String, vararg args: Any) {
    if (DEBUG) IMGUI_DEBUG_LOG(fmt, *args)
}
//fun IMGUI_DEBUG_LOG_POPUP(fmt: String, vararg args: String) = Unit       // Disable log
//fun IMGUI_DEBUG_LOG_NAV(fmt: String, vararg args: Any) = IMGUI_DEBUG_LOG(fmt, *args) // Enable log
fun IMGUI_DEBUG_LOG_NAV(fmt: String, vararg args: Any) = Unit // Disable log

fun ASSERT_PARANOID(value: Boolean) = assert(value)
fun ASSERT_PARANOID(value: Boolean, lazyMessage: () -> Any) = assert(value, lazyMessage)