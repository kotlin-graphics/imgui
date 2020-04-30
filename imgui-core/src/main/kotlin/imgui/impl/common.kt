package imgui.impl

import glm_.mat4x4.Mat4
import imgui.MouseCursor
import uno.glfw.GlfwWindow

val mat = Mat4()

/**  Main window */
var window: GlfwWindow? = null
var clientApi = GlfwClientApi.Unknown
var time = 0.0
val mouseJustPressed = BooleanArray(5)
val mouseCursors = LongArray(MouseCursor.COUNT)
var wantUpdateMonitors = true

enum class GlfwClientApi { Unknown, OpenGL, Vulkan }