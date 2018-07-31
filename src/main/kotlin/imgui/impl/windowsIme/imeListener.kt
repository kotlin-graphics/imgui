package imgui.impl.windowsIme

import glm_.bool
import glm_.i
import glm_.toHexString
import imgui.DEBUG
import imgui.g
import org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.Platform
import org.lwjgl.system.windows.RECT
import org.lwjgl.system.windows.User32.*
import org.lwjgl.system.windows.WindowProc
import uno.glfw.GlfwWindow
import vkk.adr

object imeListener : WindowProc() {

    var hwnd: HWND = NULL
    var glfwProc = NULL

    var candidateWindow = 0
    var latestChange = 0

    lateinit var window: GlfwWindow

    fun install(window: GlfwWindow) {
        this.window = window
        if (Platform.get() == Platform.WINDOWS) {
            hwnd = glfwGetWin32Window(window.handle)
            glfwProc = GetWindowLongPtr(hwnd, GWL_WNDPROC)
            SetWindowLongPtr(hwnd, GWL_WNDPROC, adr)
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
            if (DEBUG) println("Ime startComposition")
//            g.imeInProgress = true
            NULL
        }
        WM_IME_ENDCOMPOSITION -> {
            if (DEBUG) println("Ime endComposition")
//            g.imeInProgress = false
//            g.imeLastKey = latestChange
            NULL
        }
        WM_IME_COMPOSITION, WM_IME_KEYLAST -> {
            val how = "Retrieve or update " + when (w.i) {
                GCS_COMPATTR -> "the attribute of the composition string."
                GCS_COMPCLAUSE -> "clause information of the composition string."
                GCS_COMPREADATTR -> "the attributes of the reading string of the current composition."
                GCS_COMPREADCLAUSE -> "the clause information of the reading string of the composition string."
                GCS_COMPREADSTR -> "the reading string of the current composition."
                GCS_COMPSTR -> "the current composition string."
                GCS_CURSORPOS -> "the cursor position in composition string."
                GCS_DELTASTART -> "the starting position of any changes in composition string."
                GCS_RESULTCLAUSE -> "clause information of the result string."
                GCS_RESULTREADCLAUSE -> "clause information of the reading string."
                GCS_RESULTREADSTR -> "the reading string."
                GCS_RESULTSTR -> "the string of the composition result."
                else -> "new character> ${w.i}".also { latestChange = w.i }
            }
            if (DEBUG) println("Ime composition/keyLast = latestChange: 0x${w.toHexString}, how: $how")
            window.charCallback!!(w.i)
            NULL
        }
        WM_IME_SETCONTEXT -> {
            val option = when (w.i) {
                0 -> "null"
                ISC_SHOWUIALLCANDIDATEWINDOW -> "Show the composition window by user interface window."
                ISC_SHOWUICANDIDATEWINDOW -> "Show the candidate window of index 0 by user interface window."
                ISC_SHOWUICANDIDATEWINDOW_l1 -> "Show the candidate window of index 1 by user interface window."
                ISC_SHOWUICANDIDATEWINDOW_l2 -> "Show the candidate window of index 2 by user interface window."
                ISC_SHOWUICANDIDATEWINDOW_l3 -> "Show the candidate window of index 3 by user interface window."
                else -> throw Error("${w.i}")
            }
            if (DEBUG) println("Ime setContex: active: ${w.bool}, option: $option")
            NULL
        }
        WM_IME_NOTIFY -> {
            if (DEBUG) println("Ime notify: " +
                    when (w.i) {
                        WM_IME_STARTCOMPOSITION -> "IME startComposition"
                        WM_IME_ENDCOMPOSITION -> "IME endComposition"
                        IMN_CHANGECANDIDATE -> "IME is about to change the content of the candidate window $l"
                        IMN_CLOSECANDIDATE -> {
                            g.imeInProgress = false
                            "IME is about to close the candidates window $l"
                        }
                        IMN_CLOSESTATUSWINDOW -> "IME is about to close the status window"
                        IMN_GUIDELINE -> "IME is about to show an error message or other information"
                        IMN_OPENCANDIDATE -> {
                            g.imeInProgress = true
                            "IME is about to open the candidate window $l"
                        }
                        IMN_OPENSTATUSWINDOW -> "IME is about to create the status window"
                        IMN_PRIVATE -> "IME has updated its reading string as a result of the user typing or removing " +
                                "characters. The application should retrieve the reading string and save it for rendering."
                        IMN_SETCANDIDATEPOS -> "candidate processing has finished and the IME is about to move the candidate window $l"
                        IMN_SETCOMPOSITIONFONT -> "the font of the input context is updated"
                        IMN_SETCOMPOSITIONWINDOW -> "the style or position of the composition window is updated"
                        IMN_SETCONVERSIONMODE -> "the conversion mode of the input context is updated"
                        IMN_SETOPENSTATUS -> "the open status of the input context is updated"
                        IMN_SETSENTENCEMODE -> "the sentence mode of the input context is updated"
                        IMN_SETSTATUSWINDOWPOS -> "the status window position in the input context is updated"
                        else -> throw Error("${w.i}")
                    })
            NULL
        }
        WM_IME_CONTROL -> {
            if (DEBUG) println("Ime control: Instructs the IME window to " +
                    when (w.i) {
                        IMC_CLOSESTATUSWINDOW -> "hide the status window."
                        IMC_GETCANDIDATEPOS -> "Instructs an IME window to get the position of the candidate window."
                        IMC_GETCOMPOSITIONFONT -> "Instructs an IME window to retrieve the logical font used for displaying intermediate characters in the composition window."
                        IMC_GETCOMPOSITIONWINDOW -> "Instructs an IME window to get the position of the composition window."
                        IMC_GETSTATUSWINDOWPOS -> "Instructs an IME window to get the position of the status window."
                        IMC_OPENSTATUSWINDOW -> "show the status window."
                        IMC_SETCANDIDATEPOS -> "Instructs an IME window to set the position of the candidates window."
                        IMC_SETCOMPOSITIONFONT -> "Instructs an IME window to specify the logical font to use for displaying intermediate characters in the composition window."
                        IMC_SETCOMPOSITIONWINDOW -> "Instructs an IME window to set the style of the composition window."
                        IMC_SETSTATUSWINDOWPOS -> "Instructs an IME window to set the position of the status window."
                        else -> throw Error("${w.i}")
                    })
            NULL
        }
        WM_IME_COMPOSITIONFULL -> {
            if (DEBUG) println("Ime compositionFull w: $w l: $l")
            NULL
        }
        WM_IME_SELECT -> {
            if (DEBUG) println("Ime select w: $w l: $l")
            NULL
        }
        WM_IME_CHAR -> {
            if (DEBUG) println("Ime char w: $w l: $l")
            NULL
        }
        WM_IME_REQUEST -> {
            var res = NULL
            if (DEBUG) println("Ime request " +
                    when (w.i) {
                        IMR_COMPOSITIONWINDOW -> "IME needs information about the composition window."
                        IMR_CANDIDATEWINDOW -> {
//                            val cf = CANDIDATEFORM(l).apply {
//                                ptCurrentPos.x = g.platformImePos.x
//                            }
                            res = 1
                            "IME needs information about the candidate window."
                        }
                        IMR_COMPOSITIONFONT -> "IME needs information about the font used by the composition window."
                        IMR_RECONVERTSTRING -> "IME needs a string for reconversion."
                        IMR_CONFIRMRECONVERTSTRING -> "IME needs to change the RECONVERTSTRING structure."
                        IMR_QUERYCHARPOSITION -> "IME needs information about the coordinates of a character in the composition string."
                        IMR_DOCUMENTFEED -> "IME needs the converted string from the application."
                        else -> throw Error("${w.i}")
                    })
            res
        }
        WM_IME_KEYDOWN -> {
            if (DEBUG) println("Ime keyDown w: $w l: $l")
            NULL
        }
        WM_IME_KEYUP -> {
            if (DEBUG) println("Ime keyUp w: $w l: $l")
            NULL
        }
        else -> nCallWindowProc(glfwProc, hwnd, msg, w, l)
    }

    val WM_DPICHANGED = 0x02E0
    /* Ime Composition */
    val GCS_COMPATTR = 16
    val GCS_COMPCLAUSE = 32
    val GCS_COMPREADATTR = 2
    val GCS_COMPREADCLAUSE = 4
    val GCS_COMPREADSTR = 1
    val GCS_COMPSTR = 8
    val GCS_CURSORPOS = 128
    val GCS_DELTASTART = 256
    val GCS_RESULTCLAUSE = 4096
    val GCS_RESULTREADCLAUSE = 1024
    val GCS_RESULTREADSTR = 512
    val GCS_RESULTSTR = 2048
    /* Ime Context */
    val ISC_SHOWUIALL = 3221225487
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
    /* Ime Control */
    val IMC_CLOSESTATUSWINDOW = 33
    val IMC_GETCANDIDATEPOS = 7
    val IMC_GETCOMPOSITIONFONT = 9
    val IMC_GETCOMPOSITIONWINDOW = 11
    val IMC_GETSTATUSWINDOWPOS = 15
    val IMC_OPENSTATUSWINDOW = 34
    val IMC_SETCANDIDATEPOS = 8
    val IMC_SETCOMPOSITIONFONT = 10
    val IMC_SETCOMPOSITIONWINDOW = 12
    val IMC_SETSTATUSWINDOWPOS = 16
    /* Ime Request */
    val IMR_COMPOSITIONWINDOW = 1
    val IMR_CANDIDATEWINDOW = 2
    val IMR_COMPOSITIONFONT = 3
    val IMR_RECONVERTSTRING = 4
    val IMR_CONFIRMRECONVERTSTRING = 5
    val IMR_QUERYCHARPOSITION = 6
    val IMR_DOCUMENTFEED = 7
}