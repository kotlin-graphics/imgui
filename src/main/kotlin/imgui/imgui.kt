package imgui

import glm_.b
import glm_.i
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import imgui.imgui.*

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

var IMGUI_HAS_NAV = false

object ImGui :

        imgui_main,
        imgui_demoDebugInformations,
        imgui_window,
        imgui_parametersStacks,
        imgui_cursorLayout,
        imgui_colums,
        imgui_idScopes,
        imgui_widgetsText,
        imgui_widgetsMain,
        imgui_widgetsComboBox,
        imgui_widgetsDrag,
        imgui_widgetsInputKeyboard,
        imgui_widgetsSliders,
        imgui_widgetsColorEditorPicker,
        imgui_widgetsTrees,
        imgui_widgetsSelectableLists,
        imgui_tooltips,
        imgui_menus,
        imgui_popups,
        imgui_logging,
        imgui_dragAndDrop,
        imgui_clipping,
        imgui_styles,
        imgui_focus,
        imgui_utilities,
        imgui_inputs,
        imgui_helpers,

        imgui_internal {

    val version = "1.53"
}

var ptrIndices = 0
val ptrId = Array(512, { java.lang.Byte(it.b) })

// TODO get rid of local top value KMutableProperty in favor of the better with*{} solution

typealias SizeConstraintCallback = (userData: Any?, pos: Vec2i, currenSize: Vec2, desiredSize: Vec2) -> Unit

// dummy main
fun main(args: Array<String>) {

}

val NUL = '\u0000'

var DEBUG = true