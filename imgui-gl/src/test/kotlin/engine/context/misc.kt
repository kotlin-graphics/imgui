package engine.context

import engine.core.CHECK_RETV
import engine.core.TestRef
import engine.core.captureScreenshot
import glm_.vec2.Vec2

val TestContext.mainViewportPos
    get() =
//    #ifdef IMGUI_HAS_VIEWPORT
//    return ImGui::GetMainViewport()->Pos;
//    #else
        Vec2()
//    #endif

// [JVM]
fun TestContext.captureAddWindow(ref: String): Boolean = captureAddWindow(TestRef(path = ref))

fun TestContext.captureAddWindow(ref: TestRef): Boolean {
    val window = getWindowByRef(ref)
    if (window == null)
        CHECK_RETV(window != null, false)
    captureArgs.inCaptureWindows += window!!
    return window != null
}

// FIXME-TESTS: Could log the final filename(s) in ImGuiTest so the test browser could expose button to view/open them?
fun TestContext.captureScreenshot(): Boolean = REGISTER_DEPTH { // No extra depth to make it visible
    logInfo("CaptureScreenshot()")
    engine!! captureScreenshot captureArgs
}