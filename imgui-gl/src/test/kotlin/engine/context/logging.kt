package engine.context

import engine.core.*
import engine.termColor
import glm_.func.common.max
import imgui.max

fun TestContext.logEx(level_: TestVerboseLevel, flags: TestLogFlags, fmt: String, vararg args: Any) {

    var level = level_
    assert(level > TestVerboseLevel.Silent)

    if (level == TestVerboseLevel.Debug && actionDepth > 1)
        level = TestVerboseLevel.Trace

    // Log all messages that we may want to print in future.
    if (engineIO!!.configVerboseLevelOnError < level)
        return

    val log = test!!.testLog
    val prevSize = log.buffer.length

    //const char verbose_level_char = ImGuiTestEngine_GetVerboseLevelName(level)[0];
    //if (flags & ImGuiTestLogFlags_NoHeader)
    //    log->Buffer.appendf("[%c] ", verbose_level_char);
    //else
    //    log->Buffer.appendf("[%c] [%04d] ", verbose_level_char, ctx->FrameCount);
    if (flags hasnt TestLogFlag.NoHeader)
        log.buffer.append("[%04d] ".format(frameCount))

    if (level >= TestVerboseLevel.Debug)
        log.buffer.append("-- %*s".format(0 max  (actionDepth - 1) * 2))
    log.buffer.append(fmt.format(args))
    log.buffer.append("\n")

    log.updateLineOffsets(engineIO!!, level, prevSize)
    logToTTY(level, log.buffer.substring(prevSize))
}

fun TestContext.logToTTY(level: TestVerboseLevel, message: String) {

    assert(level > TestVerboseLevel.Silent && level < TestVerboseLevel.COUNT)

    if (!engineIO!!.configLogToTTY)
        return

    val test = test!!
    val log = test.testLog

    if (test.status == TestStatus.Error) {
        // Current test failed.
        if (!log.cachedLinesPrintedToTTY) {
            // Print current message and all previous logged messages.
            log.cachedLinesPrintedToTTY = true
            for (line in log.lineInfo) {
                TODO()
//                char * line_beg = log->Buffer.Buf.Data+line_info.LineOffset
//                char * line_end = strchr(line_beg, '\n')
//                char line_end_bkp = * (line_end + 1)
//                *(line_end + 1) = 0                            // Terminate line temporarily to avoid extra copying.
//                LogToTTY(line_info.Level, line_beg)
//                *(line_end + 1) = line_end_bkp                 // Restore new line after printing.
            }
            return                                             // This process included current line as well.
        }
        // Otherwise print only current message. If we are executing here log level already is within range of
        // ConfigVerboseLevelOnError setting.
    } else if (engineIO!!.configVerboseLevel < level)
    // Skip printing messages of lower level than configured.
        return

    val color = when (level) {
        TestVerboseLevel.Warning -> termColor.brightYellow
        TestVerboseLevel.Error -> termColor.brightRed
        else -> termColor.white
    }
    println(color(message))
}

fun TestContext.logDebug(fmt: String, vararg args: Any) = logEx(TestVerboseLevel.Debug, TestLogFlag.None.i, fmt, args)  // ImGuiTestVerboseLevel_Debug or ImGuiTestVerboseLevel_Trace depending on context depth
fun TestContext.logInfo(fmt: String, vararg args: Any) = logEx(TestVerboseLevel.Info, TestLogFlag.None.i, fmt, args)  // ImGuiTestVerboseLevel_Info
fun TestContext.logWarning(fmt: String, vararg args: Any) = logEx(TestVerboseLevel.Warning, TestLogFlag.None.i, fmt, args)  // ImGuiTestVerboseLevel_Warning
fun TestContext.logError(fmt: String, vararg args: Any) = logEx(TestVerboseLevel.Error, TestLogFlag.None.i, fmt, args)  // ImGuiTestVerboseLevel_Error
fun TestContext.logDebugInfo() {
    val itemHoveredId = uiContext!!.hoveredIdPreviousFrame
    val itemActiveId = uiContext!!.activeId
    val itemHoveredInfo = if (itemHoveredId != 0) engine!!.itemLocate(itemHoveredId, "") else null
    val itemActiveInfo = if (itemActiveId != 0) engine!!.itemLocate(itemActiveId, "") else null
    val hovered = itemHoveredInfo?.debugLabel ?: ""
    val active = itemActiveInfo?.debugLabel ?: ""
    logDebug("Hovered: 0x%08X (\"$hovered\"), Active:  0x%08X(\"$active\")", itemHoveredId, itemActiveId)
}