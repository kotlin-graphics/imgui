package imgui.impl

import glm_.BYTES
import glm_.mat4x4.Mat4
import imgui.MouseCursor
import kool.ByteBuffer
import kool.IntBuffer

val mat = Mat4()

var clientApi = GlfwClientApi.Unknown
var time = 0.0
val mouseJustPressed = BooleanArray(5)
val mouseCursors = LongArray(MouseCursor.COUNT)

enum class GlfwClientApi { Unknown, OpenGL, Vulkan }