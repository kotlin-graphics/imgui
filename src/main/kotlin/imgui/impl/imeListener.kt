package imgui.impl

import glm_.bool
import glm_.i
import org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window
import org.lwjgl.system.Callback
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.Platform
import org.lwjgl.system.windows.RECT
import org.lwjgl.system.windows.User32.*
import org.lwjgl.system.windows.WindowProc
import imgui.Context as g

object imeListner : WindowProc() {

    var windowProc: Callback? = null
    var hwnd = 0L
    var glfwProc = 0L

    fun install(handle: Long) {
        if (Platform.get() == Platform.WINDOWS) {
            hwnd = glfwGetWin32Window(handle)
            glfwProc = GetWindowLongPtr(hwnd, GWL_WNDPROC)
            windowProc = this
            SetWindowLongPtr(hwnd, GWL_WNDPROC, windowProc!!.address())
        }
    }

    override fun invoke(hwnd: Long, msg: Int, w: Long, l: Long) = when (msg) {
        WM_DPICHANGED -> {
            with(RECT.create(l)) {
                SetWindowPos(hwnd, NULL, left(), top(), right() - left(), bottom() - top(), SWP_NOZORDER or SWP_NOACTIVATE)
            }
            NULL
        }
        WM_IME_STARTCOMPOSITION -> {
            println("Ime startComposition w: $w, l: $l")
            g.imeInProgress = true
            NULL
        }
        WM_IME_ENDCOMPOSITION -> {
            println("Ime endComposition w: $w, l: $l")
            g.imeInProgress = false
            NULL
        }
        WM_IME_COMPOSITION /*,WM_IME_KEYLAST*/ -> {
            val latestChange = w
            val how = when (l) {
                GCS_COMPATTR -> "Retrieve or update the attribute of the composition string."
                GCS_COMPCLAUSE -> "Retrieve or update clause information of the composition string."
                GCS_COMPREADATTR -> "Retrieve or update the attributes of the reading string of the current composition."
                GCS_COMPREADCLAUSE -> "Retrieve or update the clause information of the reading string of the composition string."
                GCS_COMPREADSTR -> "Retrieve or update the reading string of the current composition."
                GCS_COMPSTR -> "Retrieve or update the current composition string."
                GCS_CURSORPOS -> "Retrieve or update the cursor position in composition string."
                GCS_DELTASTART -> "Retrieve or update the starting position of any changes in composition string."
                GCS_RESULTCLAUSE -> "Retrieve or update clause information of the result string."
                GCS_RESULTREADCLAUSE -> "Retrieve or update clause information of the reading string."
                GCS_RESULTREADSTR -> "Retrieve or update the reading string."
                GCS_RESULTSTR -> "Retrieve or update the string of the composition result."
                else -> throw Error()
            }
            println("Ime composition/keyLast latestChange: $latestChange, how: $how")
            g.imeLastKey = if (g.imeInProgress) w.i else 0
            NULL
        }
        WM_IME_SETCONTEXT -> {
            val active = w.bool
            val option = when(l.i) {
                ISC_SHOWUIALLCANDIDATEWINDOW -> "Show the composition window by user interface window."
                ISC_SHOWUICANDIDATEWINDOW -> "Show the candidate window of index 0 by user interface window."
                ISC_SHOWUICANDIDATEWINDOW_l1 -> "Show the candidate window of index 1 by user interface window."
                ISC_SHOWUICANDIDATEWINDOW_l2 -> "Show the candidate window of index 2 by user interface window."
                ISC_SHOWUICANDIDATEWINDOW_l3 -> "Show the candidate window of index 3 by user interface window."
                else -> throw Error()
            }
            println("Ime setContex active: $active, option: $l")
            NULL
        }
        WM_IME_NOTIFY -> println("Ime notify w: $w l: $l").let { 0L }
        WM_IME_CONTROL -> println("Ime control w: $w l: $l").let { 0L }
        WM_IME_COMPOSITIONFULL -> println("Ime compositionFull w: $w l: $l").let { 0L }
        WM_IME_SELECT -> println("Ime select w: $w l: $l").let { 0L }
        WM_IME_CHAR -> println("Ime char w: $w l: $l").let { 0L }
        WM_IME_REQUEST -> println("Ime request w: $w l: $l").let { 0L }
        WM_IME_KEYDOWN -> println("Ime keyDown w: $w l: $l").let { 0L }
        WM_IME_KEYUP -> println("Ime keyUp w: $w l: $l").let { 0L }
        else -> nCallWindowProc(glfwProc, hwnd, msg, w, l)
    }

    val WM_DPICHANGED = 0x02E0
    /* Ime Composition */
    val GCS_COMPATTR = 16L
    val GCS_COMPCLAUSE = 32L
    val GCS_COMPREADATTR = 2L
    val GCS_COMPREADCLAUSE = 4L
    val GCS_COMPREADSTR = 1L
    val GCS_COMPSTR = 8L
    val GCS_CURSORPOS = 128L
    val GCS_DELTASTART = 256L
    val GCS_RESULTCLAUSE = 4096L
    val GCS_RESULTREADCLAUSE = 1024L
    val GCS_RESULTREADSTR = 512L
    val GCS_RESULTSTR = 2048L
    /* Ime Context */
    val ISC_SHOWUIALLCANDIDATEWINDOW = 15 // ISC_SHOWUIGUIDWINDOW - ISC_SHOWUISOFTKBD
    val ISC_SHOWUICANDIDATEWINDOW = 1
    val ISC_SHOWUICANDIDATEWINDOW_l1 = 1 shl 1
    val ISC_SHOWUICANDIDATEWINDOW_l2 = 1 shl 2
    val ISC_SHOWUICANDIDATEWINDOW_l3 = 1 shl 3
    /* Ime Notify */
    val IMN_CHANGECANDIDATE = 3
    val IMN_CLOSECANDIDATE = 4
    val IMN_CLOSESTATUSWINDOW = 1
    val IMN_GUIDELINE = 13
    val IMN_OPENCANDIDATE = 5
    val IMN_OPENSTATUSWINDOW = 2
    val IMN_PRIVATE = 14
    val IMN_SETCANDIDATEPOS = 9
    val IMN_SETCOMPOSITIONFONT = 10
    val IMN_SETCOMPOSITIONWINDOW = 11
    val IMN_SETCONVERSIONMODE = 6
    val IMN_SETOPENSTATUS = 8
    val IMN_SETSENTENCEMODE = 7
    val IMN_SETSTATUSWINDOWPOS = 12
}