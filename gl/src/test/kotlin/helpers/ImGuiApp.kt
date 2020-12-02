package helpers

import glm_.vec2.Vec2
import glm_.vec4.Vec4
import glm_.vec4.Vec4i
import java.nio.ByteBuffer

abstract class ImGuiApp {

    var dpiAware = true
    var quit = false
    var vSync = true
    var dpiScale = 1f
    val clearColor = Vec4(0f, 0f, 0f, 1f)

    abstract fun initCreateWindow(windowTitle: String, windowSize: Vec2): Boolean
    open fun initBackends() = Unit
    abstract fun newFrame(): Boolean
    open fun render() = Unit
    open fun shutdownCloseWindow() = Unit
    open fun shutdownBackends() = Unit
    open fun destroy() = Unit
    abstract fun captureFramebuffer(rect: Vec4i, pixelsRgba: ByteBuffer, userData: Any?): Boolean
}

//typealias InitCreateWindow = (app: ImGuiApp, windowTitle: String, windowSize: Vec2) -> Boolean
//typealias InitBackends = (app: ImGuiApp) -> Unit
//typealias NewFrame = (app: ImGuiApp) -> Boolean
//typealias Render = (app: ImGuiApp) -> Unit
//typealias ShutdownCloseWindow = (app: ImGuiApp) -> Unit
//typealias ShutdownBackends = (app: ImGuiApp) -> Unit
//typealias Destroy = (app: ImGuiApp) -> Unit
//typealias CaptureFramebuffer = (app: ImGuiApp, rect: Vec4i, pixelsRgba: ByteBuffer, userData: Any?) -> Boolean