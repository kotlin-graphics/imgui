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

val TestContext.mainViewportPos
    get() =
//    #ifdef IMGUI_HAS_VIEWPORT
//    return ImGui::GetMainViewport()->Pos;
//    #else
        Vec2()
//    #endif

fun TestContext.captureAddWindow(ref: TestRef): Boolean {
    val window = getWindowByRef(ref)
    if (window == null)
        CHECK_RETV(window != null, false)
    captureArgs.inCaptureWindows += window
    return window != null
}

// FIXME-TESTS: Could log the final filename(s) in ImGuiTest so the test browser could expose button to view/open them?
fun TestContext.captureScreenshot(): Boolean {
    //IMGUI_TEST_CONTEXT_REGISTER_DEPTH(this); // No extra depth to make it visible
    logInfo("CaptureScreenshot()")
    return engine.captureScreenshot(captureArgs)
}