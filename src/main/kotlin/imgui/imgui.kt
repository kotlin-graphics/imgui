package imgui

import glm_.glm
import glm_.i
import imgui.internal.clearActiveID
import imgui.internal.focusWindow
import imgui.internal.keepAliveID
import imgui.internal.lengthSqr

// Helpers macros to generate 32-bits encoded colors
var IMGUI_USE_BGRA_PACKED_COLOR = false

val IM_COL32_R_SHIFT by lazy { if (IMGUI_USE_BGRA_PACKED_COLOR) 16 else 0 }
val IM_COL32_G_SHIFT = 8
val IM_COL32_B_SHIFT by lazy { if (IMGUI_USE_BGRA_PACKED_COLOR) 0 else 16 }
val IM_COL32_A_SHIFT = 24
val IM_COL32_A_MASK = 0xFF000000.i

//fun IM_COL32(R,G,B,A)    (((ImU32)(A)<<IM_COL32_A_SHIFT) | ((ImU32)(B)<<IM_COL32_B_SHIFT) | ((ImU32)(G)<<IM_COL32_G_SHIFT) | ((ImU32)(R)<<IM_COL32_R_SHIFT))
//#define IM_COL32_WHITE       IM_COL32(255,255,255,255)  // Opaque white = 0xFFFFFFFF
//#define IM_COL32_BLACK       IM_COL32(0,0,0,255)        // Opaque black
//#define IM_COL32_BLACK_TRANS IM_COL32(0,0,0,0)          // Transparent black = 0x00000000

var _DEBUG = true


