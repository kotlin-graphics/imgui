package imgui.impl.glfw

import org.lwjgl.system.APIUtil
import org.lwjgl.system.Library
import org.lwjgl.system.windows.User32

private val USER32 = Library.loadNative(User32::class.java, "org.lwjgl", "user32")

val GetWindowLong = APIUtil.apiGetFunctionAddress(USER32, "GetWindowLong")


