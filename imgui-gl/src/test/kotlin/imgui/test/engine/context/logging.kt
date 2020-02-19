package imgui.test.engine.context

import glm_.f
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endGroup
import imgui.ImGui.endTabBar
import imgui.ImGui.findWindowByID
import imgui.ImGui.focusWindow
import imgui.ImGui.isItemActivated
import imgui.ImGui.isItemActive
import imgui.ImGui.isItemClicked
import imgui.ImGui.isItemDeactivated
import imgui.ImGui.isItemDeactivatedAfterEdit
import imgui.ImGui.isItemEdited
import imgui.ImGui.isItemFocused
import imgui.ImGui.isItemHovered
import imgui.ImGui.isItemVisible
import imgui.ImGui.popID
import imgui.ImGui.treePop
import imgui.classes.Context
import imgui.internal.*
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.test.IMGUI_HAS_TABLE
import imgui.test.engine.TestEngine
import imgui.test.engine.core.*
import io.kotlintest.shouldBe
import imgui.WindowFlag as Wf

fun TestContext.logEx(level_: TestVerboseLevel, flags: TestLogFlags, fmt: String, vararg args: Any) {

    var level = level_
    assert(level > TestVerboseLevel.Silent)

    if (level == TestVerboseLevel.Debug && actionDepth > 1)
        level = TestVerboseLevel.Trace

    // Log all messages that we may want to print in future.
    if (engineIO!!.configVerboseLevelOnError < level)
        return

    val log = test!!.testLog
    val prevSize = log.buffer.size()

    //const char verbose_level_char = ImGuiTestEngine_GetVerboseLevelName(level)[0];
    //if (flags & ImGuiTestLogFlags_NoHeader)
    //    log->Buffer.appendf("[%c] ", verbose_level_char);
    //else
    //    log->Buffer.appendf("[%c] [%04d] ", verbose_level_char, ctx->FrameCount);
    if (flags hasnt TestLogFlag.NoHeader)
        log.buffer.appendf("[%04d] ", frameCount)

    if (level >= TestVerboseLevel.Debug)
        log.buffer.appendf("-- %*s", max(0, (actionDepth - 1) * 2), "")
    log.buffer.appendfv(fmt, args)
    log.buffer.append("\n")

    log.updateLineOffsets(engineIO, level, log.buffer.begin() + prevSize)
    logToTTY(level, log.buffer.c_str() + prevSize)
}

fun TestContext.logToTTY(level: TestVerboseLevel, message: String) {

    assert(level > VerboseLevel.Silent && level < TestVerboseLevel.COUNT)

    if (!engineIO.configLogToTTY)
        return

    val test = test!!
    val log = test.testLog

    if (test.status == TestStatus.Error) {
        // Current test failed.
        if (!log.cachedLinesPrintedToTTY) {
            // Print current message and all previous logged messages.
            log.cachedLinesPrintedToTTY = true
            for (line in log.lineInfo) {
                char * line_beg = log->Buffer.Buf.Data+line_info.LineOffset
                char * line_end = strchr(line_beg, '\n')
                char line_end_bkp = * (line_end + 1)
                *(line_end + 1) = 0                            // Terminate line temporarily to avoid extra copying.
                LogToTTY(line_info.Level, line_beg)
                *(line_end + 1) = line_end_bkp                 // Restore new line after printing.
            }
            return                                             // This process included current line as well.
        }
        // Otherwise print only current message. If we are executing here log level already is within range of
        // ConfigVerboseLevelOnError setting.
    } else if (engineIO.configVerboseLevel < level)
    // Skip printing messages of lower level than configured.
        return

    val color = when (level) {
        TestVerboseLevel.Warning -> osConsoleTextColor_BrightYellow
        TestVerboseLevel.Error -> osConsoleTextColor_BrightRed
        else -> osConsoleTextColor_White
    }
    osConsoleSetTextColor(osConsoleStream_StandardOutput, color)
    println(message)
    osConsoleSetTextColor(ImOsConsoleStream_StandardOutput, ImOsConsoleTextColor_White)
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