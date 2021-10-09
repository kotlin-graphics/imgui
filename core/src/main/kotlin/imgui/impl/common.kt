package imgui.impl

import glm_.mat4x4.Mat4
import imgui.MouseButton
import imgui.MouseCursor

val mat = Mat4()

var clientApi = GlfwClientApi.Unknown
var time = 0.0
val mouseJustPressed = BooleanArray(MouseButton.COUNT)
val mouseCursors = LongArray(MouseCursor.COUNT)

enum class GlfwClientApi { Unknown, OpenGL, Vulkan, WebGPU }