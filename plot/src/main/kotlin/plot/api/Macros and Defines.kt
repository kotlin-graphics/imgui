package plot.api

import glm_.vec4.Vec4

//-----------------------------------------------------------------------------
// [SECTION] Macros and Defines
//-----------------------------------------------------------------------------

// Define attributes of all API symbols declarations (e.g. for DLL under Windows)
// Using ImPlot via a shared library is not recommended, because we don't guarantee
// backward nor forward ABI compatibility and also function call overhead. If you
// do use ImPlot as a DLL, be sure to call SetImGuiContext (see Miscellanous section).
//#ifndef IMPLOT_API
//#define IMPLOT_API
//#endif

// ImPlot version string.
const val PLOT_VERSION = "0.14"
// Indicates variable should deduced automatically.
const val PLOT_AUTO = -1f
// Special color used to indicate that a color should be deduced automatically.
val PLOT_AUTO_COL = Vec4(0,0,0,-1)
// Macro for templated plotting functions; keeps header clean.
//#define IMPLOT_TMP template <typename T> IMPLOT_API