package imgui.windowsIme

import glm_.BYTES
import glm_.L
import imgui.MINECRAFT_BEHAVIORS
import kool.Adr
import kool.BYTES
import kool.adr
import org.lwjgl.system.*
import org.lwjgl.system.MemoryUtil.memGetLong
import org.lwjgl.system.MemoryUtil.memPutLong
import org.lwjgl.system.windows.WindowsLibrary
import uno.glfw.HWND
import java.nio.ByteBuffer


object imm {

    val lib: SharedLibrary = WindowsLibrary("Imm32")

    val ImmCreateContext = lib.getFunctionAddress("ImmCreateContext")
    val ImmGetContext = lib.getFunctionAddress("ImmGetContext")
    val ImmSetCompositionWindow = lib.getFunctionAddress("ImmSetCompositionWindow")
    val ImmReleaseContext = lib.getFunctionAddress("ImmReleaseContext")

    fun getContext(hwnd: HWND): Long{
        return if(Platform.get() == Platform.WINDOWS && MINECRAFT_BEHAVIORS) {
            JNI.callP(ImmCreateContext)
        } else {
            JNI.callPP(hwnd.L, ImmGetContext)
        }
    }
    fun setCompositionWindow(himc: HIMC, compForm: COMPOSITIONFORM) = JNI.callPPI(himc.L, compForm.adr, ImmSetCompositionWindow)
    fun releaseContext(hwnd: HWND, himc: HIMC) = JNI.callPPI(hwnd.L, himc.L, ImmReleaseContext)

    // bit field for IMC_SETCOMPOSITIONWINDOW, IMC_SETCANDIDATEWINDOW
    val CFS_DEFAULT = DWORD(0x0000)
    val CFS_RECT = DWORD(0x0001)
    val CFS_POINT = DWORD(0x0002)
    val CFS_FORCE_POSITION = DWORD(0x0020)
    val CFS_CANDIDATEPOS = DWORD(0x0040)
    val CFS_EXCLUDE = DWORD(0x0080)
}


// TODO -> uno
inline class HIMC(val L: Long)

fun DWORD(i: Int) = DWORD(i.L)
inline class DWORD(val L: Long) {
    companion object {
        val BYTES get() = Long.BYTES
    }
}

/**
 * typedef struct tagCANDIDATEFORM {
 *     DWORD dwIndex;
 *     DWORD dwStyle;
 *     POINT ptCurrentPos;
 *     RECT  rcArea;
 * } CANDIDATEFORM, *PCANDIDATEFORM;
 */
class CANDIDATEFORM constructor(val adr: Long) {

    val buffer: ByteBuffer = MemoryUtil.memByteBuffer(adr, size)

    var dwIndex: DWORD
        get() = DWORD(memGetLong(adr + ofs.dwIndex))
        set(value) = memPutLong(adr + ofs.dwIndex, value.L)

    var dwStyle: DWORD
        get() = DWORD(memGetLong(adr + ofs.dwStyle))
        set(value) = memPutLong(adr + ofs.dwStyle, value.L)

    var ptCurrentPos: POINT
        get() = POINT(adr + ofs.ptCurrentPos)
        set(value) = value.to(adr + ofs.ptCurrentPos)

    var rcArea: RECT
        get() = RECT(adr + ofs.rcArea)
        set(value) = value.to(adr + ofs.rcArea)

    companion object {
        val size = 2 * Int.BYTES + POINT.size + RECT.size
    }

    object ofs {
        val dwIndex = 0
        val dwStyle = dwIndex + DWORD.BYTES
        val ptCurrentPos = dwStyle + DWORD.BYTES
        val rcArea = ptCurrentPos + POINT.size
    }
}

/**
 * typedef struct tagCOMPOSITIONFORM {
 *      DWORD dwStyle;
 *      POINT ptCurrentPos;
 *      RECT  rcArea;
 * } COMPOSITIONFORM
 */
class COMPOSITIONFORM constructor(val adr: Adr) {

    val buffer: ByteBuffer = MemoryUtil.memByteBuffer(adr, size)

    constructor() : this(MemoryUtil.nmemAlloc(size.L))
    constructor(stack: MemoryStack) : this(stack.calloc(size).adr)

    var dwStyle: DWORD
        get() = DWORD(memGetLong(adr + ofs_dwStyle))
        set(value) = memPutLong(adr + ofs_dwStyle, value.L)

    var ptCurrentPos = POINT(adr + ofs_ptCurrentPos)

    var rcArea: RECT
        get() = RECT(adr + ofs_rcArea)
        set(value) = value.to(adr + ofs_rcArea)

    fun free() = MemoryUtil.nmemFree(adr)

    companion object {
        val size = 2 * Int.BYTES + POINT.size + RECT.size

        val ofs_dwStyle = 0
        val ofs_ptCurrentPos = ofs_dwStyle + POINT.size
        val ofs_rcArea = ofs_ptCurrentPos + RECT.size
    }
}

/** typedef struct tagPOINT {
 *      LONG x;
 *      LONG y;
 *  } POINT, *PPOINT;
 */
class POINT constructor(val adr: Long) {

    val buffer: ByteBuffer = MemoryUtil.memByteBuffer(adr, size)

    var x: Long
        get() = memGetLong(adr + ofs.x)
        set(value) = memPutLong(adr + ofs.x, value)
    var y: Long
        get() = memGetLong(adr + ofs.y)
        set(value) = memPutLong(adr + ofs.y, value)

    fun to(adr: Long) {
        memPutLong(adr + ofs.x, x)
        memPutLong(adr + ofs.y, y)
    }

    companion object {
        val size = 2 * Long.BYTES
    }

    object ofs {
        val x = 0
        val y = x + Long.BYTES
    }
}

/**
 * typedef struct _RECT {
 *     LONG left;
 *     LONG top;
 *     LONG right;
 *     LONG bottom;
 * } RECT, *PRECT;
 */
class RECT(val adr: Long) {

    val buffer: ByteBuffer = MemoryUtil.memByteBuffer(adr, size)

    var left: Long
        get() = memGetLong(adr + ofs.left)
        set(value) = memPutLong(adr + ofs.left, value)
    var top: Long
        get() = memGetLong(adr + ofs.top)
        set(value) = memPutLong(adr + ofs.top, value)
    var right: Long
        get() = memGetLong(adr + ofs.right)
        set(value) = memPutLong(adr + ofs.right, value)
    var bottom: Long
        get() = memGetLong(adr + ofs.bottom)
        set(value) = memPutLong(adr + ofs.bottom, value)

    constructor() : this(MemoryUtil.nmemAlloc(size.L))

    fun to(adr: Long) {
        memPutLong(adr + ofs.left, left)
        memPutLong(adr + ofs.top, top)
        memPutLong(adr + ofs.right, right)
        memPutLong(adr + ofs.bottom, bottom)
    }

    companion object {
        val size = 4 * Long.BYTES
    }

    object ofs {
        val left = 0
        val top = left + Long.BYTES
        val right = top + Long.BYTES
        val bottom = right + Long.BYTES
    }
}
