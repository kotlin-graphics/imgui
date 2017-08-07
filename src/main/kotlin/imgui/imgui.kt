package imgui

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
//#define IM_COL32_WHITE       IM_COL32(255,255,255,255)  // Opaque white = 0xFFFFFFFF
//#define IM_COL32_BLACK       IM_COL32(0,0,0,255)        // Opaque black
//#define IM_COL32_BLACK_TRANS IM_COL32(0,0,0,0)          // Transparent black = 0x00000000

var _DEBUG = true


object ImGui :

        imgui_main,
        imgui_demoDebugInfo,
        imgui_window,
        imgui_parametersStacks,
        imgui_cursorLayout,
        imgui_colums,
        imgui_idScopes,
        imgui_widgets,
        imgui_widgetsDrag,
        imgui_widgetsInputKeyboard,
        imgui_widgetsSliders,
        imgui_widgetsTrees,
        imgui_widgetsSelectableLists,
        imgui_tooltips,
        imgui_menus,
        imgui_popups,
        imgui_logging,
        imgui_clipping,
        imgui_utilities,

        imgui_internal,

        imgui_functionalProgramming {

    val version = "1.51 WIP"
}

var debug = 0

var ptrIndices = 0 // TODO move


typealias SizeConstraintCallback = (userData: Any, pos: Vec2i, currenSize: Vec2, desiredSize: Vec2) -> Unit