package imgui

import glm_.i
import imgui.imgui.*
import imgui.imgui.widgets.*
import imgui.internal.Rect
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

/** -----------------------------------------------------------------------------
 *      Context
 *  -----------------------------------------------------------------------------
 *
 *  Current context pointer. Implicitly used by all ImGui functions. Always assumed to be != null.
 *  ::createContext() will automatically set this pointer if it is null. Change to a different context by calling
 *  ::setCurrentContext().
 *  If you use DLL hotreloading you might need to call ::setCurrentContext() after reloading code from this file.
 *  ImGui functions are not thread-safe because of this pointer. If you want thread-safety to allow N threads to access
 *  N different contexts, you can:
 *      - Change this variable to use thread local storage. You may #define GImGui in imconfig.h for that purpose.
 *          Future development aim to make this context pointer explicit to all calls.
 *          Also read https://github.com/ocornut/imgui/issues/586
 *      - Having multiple instances of the ImGui code compiled inside different namespace (easiest/safest, if you have
 *          a finite number of contexts)    */
val g get() = gImGui!!
var gImGui: Context? = null

// Helpers macros to generate 32-bits encoded colors
var USE_BGRA_PACKED_COLOR = false

val COL32_R_SHIFT by lazy { if (USE_BGRA_PACKED_COLOR) 16 else 0 }
val COL32_G_SHIFT = 8
val COL32_B_SHIFT by lazy { if (USE_BGRA_PACKED_COLOR) 0 else 16 }
val COL32_A_SHIFT = 24
val COL32_A_MASK = 0xFF000000.i

fun COL32(r: Int, g: Int, b: Int, a: Int) = (a shl COL32_A_SHIFT) or (b shl COL32_B_SHIFT) or (g shl COL32_G_SHIFT) or (r shl COL32_R_SHIFT)
val COL32_WHITE = COL32(255, 255, 255, 255) // Opaque white = 0xFFFFFFFF
val COL32_BLACK = COL32(0, 0, 0, 255)       // Opaque black
val COL32_BLACK_TRANS = COL32(0, 0, 0, 0)   // Transparent black = 0x00000000

val MOUSE_INVALID = -256000f

// Debug options
val IMGUI_DEBUG_NAV_SCORING = false
val IMGUI_DEBUG_NAV_RECTS = false

// When using CTRL+TAB (or Gamepad Square+L/R) we delay the visual a little in order to reduce visual noise doing a fast switch.

/** Time before the highlight and screen dimming starts fading in */
const val NAV_WINDOWING_HIGHLIGHT_DELAY = 0.2f
/** Time before the window list starts to appear */
const val NAV_WINDOWING_LIST_APPEAR_DELAY = 0.15f

// Window resizing from edges (when io.configWindowsResizeFromEdges = true and BackendFlag.HasMouseCursors is set in io.backendFlags by back-end)

/** Extend outside and inside windows. Affect FindHoveredWindow(). */
const val WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS = 4f
/** Reduce visual noise by only highlighting the border after a certain time. */
const val WINDOWS_RESIZE_FROM_EDGES_FEEDBACK_TIMER = 0.04f

// Test engine hooks (imgui-test)
const val IMGUI_ENABLE_TEST_ENGINE = false
var ImGuiTestEngineHook_PreNewFrame: () -> Unit = {}
var ImGuiTestEngineHook_PostNewFrame: () -> Unit = {}
var ImGuiTestEngineHook_ItemAdd: (bb: Rect, id: ID) -> Unit = { _, _ -> }
var ImGuiTestEngineHook_ItemInfo: (id: ID, label: String, flags: Int) -> Unit = { _, _, _ -> }

object ImGui :

        imgui_main,
        imgui_demoDebugInformations,
        imgui_window,
        imgui_windowScrolling,
        imgui_parametersStacks,
        imgui_cursorLayout,
        imgui_colums,
        imgui_idScopes,
        listBoxes,
        text,
        main,
        comboBox,
        dataPlotting,
        drags,
        inputKeyboard,
        lowLevelLayoutHelpers,
        sliders,
        colorEditorPicker,
        trees,
        selectableLists,
        imgui_tooltips,
        imgui_menus,
        imgui_popups,
        imgui_logging,
        imgui_dragAndDrop,
        imgui_clipping,
        imgui_styles,
        imgui_focusActivation,
        imgui_utilities,
        imgui_inputs,
        imgui_helpers,

        imgui_internal {

    // Version
    val beta = 0
    /** get the compiled version string e.g. "1.23" */
    val version = "1.67.$beta"
    /** Integer encoded as XYYZZ for use in #if preprocessor conditionals.
    Work in progress versions typically starts at XYY00 then bounced up to XYY01 when release tagging happens */
    val versionNum = 16700 + beta
}

var ptrIndices = 0
var ptrId = Array(512) { it } // it was: java.lang.Byte.valueOf(it.b)

// TODO get rid of local top value KMutableProperty in favor of the better with*{} solution


val NUL = '\u0000'

var DEBUG = true

operator fun StringBuilder.plusAssign(string: String) {
    append(string)
}

// Typedefs and Enumerations (declared as int for compatibility and to not pollute the top of this file)

/** Unique ID used by widgets (typically hashed from a stack of string) */
typealias ID = Int

/** User data to identify a texture */
typealias TextureID = Int

/** flags: for DrawList::addRect*() etc.   // enum DrawCornerFlag */
typealias DrawCornerFlags = Int

/** flags: for DrawList                    // enum DrawListFlag */
typealias DrawListFlags = Int

/** flags: for FontAtlas                   // enum FontAtlasFlags */
typealias FontAtlasFlags = Int
//typedef int ImGuiBackendFlags;      // flags: for io.BackendFlag               // enum ImGuiBackendFlags_
/** flags: for ColorEdit*(), ColorPicker*()  // enum ColorEditFlag */
typealias ColorEditFlags = Int

/** flags: for *Columns*()                   // enum ColumnsFlag */
typealias ColumnsFlags = Int

/** flags: for io.ConfigFlags                // enum ConfigFlag */
typealias ConfigFlags = Int

/** flags: for *DragDrop*()                  // enum DragDropFlag */
typealias DragDropFlags = Int

/** flags: for BeginCombo()                  // enum ComboFlag */
typealias ComboFlags = Int

/** flags: for ::isWindowFocused()             // enum FocusedFlag */
typealias FocusedFlags = Int

/** flags: for ::isItemHovered() etc.          // enum HoveredFlag */
typealias HoveredFlags = Int

/** flags: for ::inputText*()                  // enum InputTextFlag */
typealias InputTextFlags = Int

/** flags: for Selectable()                  // enum SelectableFlag */
typealias SelectableFlags = Int

/** Flags: for BeginTabBar() */
typealias TabBarFlags = Int                 // enum ImGuiTabBarFlags_

/** Flags: for BeginTabItem() */
typealias TabItemFlags = Int                // enum ImGuiTabItemFlags_

/** flags: for TreeNode*(),CollapsingHeader()// enum TreeNodeFlag */
typealias TreeNodeFlags = Int

/** flags: for Begin*()                      // enum WindowFlag */
typealias WindowFlags = Int

typealias InputTextCallback = (InputTextCallbackData) -> Int
typealias SizeCallback = (SizeCallbackData) -> Unit

// dummy main
fun main(args: Array<String>) {
}

var stop = false

inline operator fun <R> KMutableProperty0<R>.setValue(host: Any?, property: KProperty<*>, value: R) = set(value)
inline operator fun <R> KMutableProperty0<R>.getValue(host: Any?, property: KProperty<*>): R = get()

infix fun String.cmp(charArray: CharArray): Boolean {
    for (i in indices)
        if (get(i) != charArray[i])
            return false
    return true
}

infix fun CharArray.cmp(other: CharArray): Boolean {
    for (i in indices) {
        val a = get(i)
        val b = other.getOrElse(i) { return false }
        if (a == NUL)
            return b == NUL
        if (a != b)
            return false
    }
    return true
}