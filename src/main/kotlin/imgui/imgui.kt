package imgui

import glm_.i
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
        imgui_window,
        imgui_parametersStacks,
        // cursor
        imgui_colums,
        imgui_id,
        imgui_widgets,
        imgui_tooltips,
        // menus popus logging clipping
        imgui_clipping,
        imgui_log,

        imgui_internal