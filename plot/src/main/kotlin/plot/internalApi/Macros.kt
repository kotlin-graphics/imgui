package plot.internalApi

import plot.api.Axis

//-----------------------------------------------------------------------------
// [SECTION] Macros
//-----------------------------------------------------------------------------

val PLOT_NUM_X_AXES = Axis.Y1.i
val PLOT_NUM_Y_AXES = Axis.COUNT - PLOT_NUM_X_AXES

// Split ImU32 color into RGB components [0 255]
//#define IM_COL32_SPLIT_RGB(col,r,g,b) \
//ImU32 r = ((col >> IM_COL32_R_SHIFT) & 0xFF); \
//ImU32 g = ((col >> IM_COL32_G_SHIFT) & 0xFF); \
//ImU32 b = ((col >> IM_COL32_B_SHIFT) & 0xFF);