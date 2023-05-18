package imgui.api

import glm_.vec2.Vec2
import imgui.internal.api.plotEx
import imgui.internal.sections.PlotType

// Widgets: Data Plotting
// - Consider using ImPlot (https://github.com/epezent/implot) which is much better!
interface widgetsDataPlotting {
    fun plotLines(label: String,
                  values: FloatArray,
                  valuesOffset: Int = 0,
                  overlayText: String = "",
                  scaleMin: Float = Float.MAX_VALUE,
                  scaleMax: Float = Float.MAX_VALUE,
                  graphSize: Vec2 = Vec2(),
                  stride: Int = 1): Int =
            plotEx(PlotType.Lines, label, values.size, valuesOffset, overlayText, scaleMin, scaleMax, graphSize) {
                values[it * stride]
            }

    fun plotHistogram(label: String,
                      values: FloatArray,
                      valuesOffset: Int = 0,
                      overlayText: String = "",
                      scaleMin: Float = Float.MAX_VALUE,
                      scaleMax: Float = Float.MAX_VALUE,
                      graphSize: Vec2 = Vec2(),
                      stride: Int = 1): Int =
            plotEx(PlotType.Histogram, label, values.size, valuesOffset, overlayText, scaleMin, scaleMax, graphSize) {
                values[it * stride]
            }
}

inline fun plotLines(label: String,
                     valuesCount: Int,
                     valuesOffset: Int = 0,
                     overlayText: String = "",
                     scaleMin: Float = Float.MAX_VALUE,
                     scaleMax: Float = Float.MAX_VALUE,
                     graphSize: Vec2 = Vec2(),
                     valuesGetter: (idx: Int) -> Float) = plotEx(PlotType.Lines, label, valuesCount, valuesOffset, overlayText, scaleMin, scaleMax, graphSize, valuesGetter)

inline fun plotHistogram(label: String,
                         valuesCount: Int,
                         valuesOffset: Int = 0,
                         overlayText: String = "",
                         scaleMin: Float = Float.MAX_VALUE,
                         scaleMax: Float = Float.MAX_VALUE,
                         graphSize: Vec2 = Vec2(),
                         valuesGetter: (idx: Int) -> Float) = plotEx(PlotType.Histogram, label, valuesCount, valuesOffset, overlayText, scaleMin, scaleMax, graphSize, valuesGetter)