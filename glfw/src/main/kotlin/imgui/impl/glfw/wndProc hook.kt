package imgui.impl.glfw

import imgui.ImGui
import imgui.MouseSource
import org.lwjgl.system.*
import org.lwjgl.system.libffi.FFICIF
import org.lwjgl.system.libffi.LibFFI
import org.lwjgl.system.windows.User32

typealias WNDPROC = Long
typealias LPARAM = Long

// GLFW doesn't allow to distinguish Mouse vs TouchScreen vs Pen.
// Add support for Win32 (based on imgui_impl_win32), because we rely on _TouchScreen info to trickle inputs differently.
val mouseSourceFromMessageExtraInfo: MouseSource
    get() {
        val extraInfo: LPARAM = User32.GetMessageExtraInfo()
        return when {
            (extraInfo and 0xFFFFFF80) == 0xFF515700 -> MouseSource.Pen
            (extraInfo and 0xFFFFFF80) == 0xFF515780 -> MouseSource.TouchScreen
            else -> MouseSource.Mouse
        }
    }

//static LRESULT CALLBACK ImGui_ImplGlfw_WndProc (HWND hWnd, UINT msg, WPARAM wParam, LPARAM lParam)
//{
//    ImGui_ImplGlfw_Data * bd = ImGui_ImplGlfw_GetBackendData()
//    switch(msg)
//    {
//        case WM_MOUSEMOVE : case WM_NCMOUSEMOVE:
//        case WM_LBUTTONDOWN : case WM_LBUTTONDBLCLK: case WM_LBUTTONUP:
//        case WM_RBUTTONDOWN : case WM_RBUTTONDBLCLK: case WM_RBUTTONUP:
//        case WM_MBUTTONDOWN : case WM_MBUTTONDBLCLK: case WM_MBUTTONUP:
//        case WM_XBUTTONDOWN : case WM_XBUTTONDBLCLK: case WM_XBUTTONUP:
//        ImGui::GetIO().AddMouseSourceEvent(GetMouseSourceFromMessageExtraInfo())
//        break
//    }
//    return ::CallWindowProc(bd->GlfwWndProc, hWnd, msg, wParam, lParam)
//}

/**
 * Instances of this interface may be passed to the [SetWindowFocusCallback][GLFW.glfwSetWindowFocusCallback] method.
 *
 * <h3>Type</h3>
 *
 * <pre>`
 * void (*[.invoke]) (
 * GLFWwindow *window,
 * int focused
 * )`</pre>
 *
 * @since version 3.0
 */
@FunctionalInterface
@NativeType("ImplGlfw_WndProc")
interface ImplGlfw_WndProcI : CallbackI {
    override fun getCallInterface(): FFICIF = CIF

    override fun callback(ret: Long, args: Long) {
        invoke(MemoryUtil.memGetAddress(MemoryUtil.memGetAddress(args)),
                MemoryUtil.memGetInt(MemoryUtil.memGetAddress(args + Pointer.POINTER_SIZE)) != 0)
    }

    /**
     * Will be called when the specified window gains or loses focus.
     *
     * @param window  the window that was focused or defocused
     * @param focused [TRUE][GLFW.GLFW_TRUE] if the window was focused, or [FALSE][GLFW.GLFW_FALSE] if it was defocused
     */
    operator fun invoke(@NativeType("GLFWwindow *") window: Long, @NativeType("int") focused: Boolean)

    companion object {
        val CIF = APIUtil.apiCreateCIF(
                LibFFI.FFI_DEFAULT_ABI,
                LibFFI.ffi_type_void,
                LibFFI.ffi_type_uint32, LibFFI.ffi_type_uint32, LibFFI.ffi_type_uint32, LibFFI.ffi_type_uint32
        )
    }
}