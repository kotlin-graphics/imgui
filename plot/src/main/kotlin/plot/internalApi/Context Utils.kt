@file:OptIn(ExperimentalUnsignedTypes::class)

package plot.internalApi

import imgui.COL32

//-----------------------------------------------------------------------------
// [SECTION] Context Utils
//-----------------------------------------------------------------------------

// Initializes an ImPlotContext
//IMPLOT_API void Initialize(ImPlotContext* ctx); -> Context class

// Resets an ImPlot context for the next call to BeginPlot
//IMPLOT_API void ResetCtxForNextPlot(ImPlotContext* ctx); -> Context class

// Resets an ImPlot context for the next call to BeginAlignedPlots
//IMPLOT_API void ResetCtxForNextAlignedPlots(ImPlotContext* ctx); -> Context class

// Resets an ImPlot context for the next call to BeginSubplot
//IMPLOT_API void ResetCtxForNextSubplot(ImPlotContext* ctx); -> Context class

fun PlotContext.APPEND_CMAP(name: String, array: UIntArray, qual: Boolean) = colormapData.append(name, array,  qual)
fun RGB(r: Int, g: Int, b: Int) = COL32(r, g, b, 255).toUInt()