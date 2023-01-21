package imgui.api

import glm_.vec2.Vec2
import imgui.ImGui.plotEx
import imgui.internal.sections.PlotType

// Widgets: Data Plotting
// - Consider using ImPlot (https://github.com/epezent/implot)
interface widgetsDataPlotting {

    fun plotLines(label: String, values: FloatArray, valuesOffset: Int = 0, overlayText: String = "", scaleMin: Float = Float.MAX_VALUE,
                  scaleMax: Float = Float.MAX_VALUE, graphSize: Vec2 = Vec2(), stride: Int = 1) =
            plotEx(PlotType.Lines, label, PlotArrayData(values, stride), valuesOffset, overlayText, scaleMin, scaleMax, graphSize)

    fun plotLines(label: String, valuesGetter: (idx: Int) -> Float, valuesCount: Int, valuesOffset: Int = 0,
                  overlayText: String = "", scaleMin: Float = Float.MAX_VALUE, scaleMax: Float = Float.MAX_VALUE,
                  graphSize: Vec2 = Vec2()) =
            plotEx(PlotType.Lines, label, PlotArrayFunc(valuesGetter, valuesCount), valuesOffset, overlayText, scaleMin, scaleMax, graphSize)

    fun plotHistogram(label: String, values: FloatArray, valuesOffset: Int = 0, overlayText: String = "",
                      scaleMin: Float = Float.MAX_VALUE, scaleMax: Float = Float.MAX_VALUE, graphSize: Vec2 = Vec2(), stride: Int = 1) =
            plotEx(PlotType.Histogram, label, PlotArrayData(values, stride), valuesOffset, overlayText, scaleMin, scaleMax, graphSize)

    fun plotHistogram(label: String, valuesGetter: (idx: Int) -> Float, valuesCount: Int, valuesOffset: Int = 0,
                      overlayText: String = "", scaleMin: Float = Float.MAX_VALUE, scaleMax: Float = Float.MAX_VALUE,
                      graphSize: Vec2 = Vec2()) =
            plotEx(PlotType.Histogram, label, PlotArrayFunc(valuesGetter, valuesCount), valuesOffset, overlayText, scaleMin, scaleMax, graphSize)

    companion object {

        interface PlotArray {
            operator fun get(idx: Int): Float
            fun count(): Int
        }

        class PlotArrayData(val values: FloatArray, val stride: Int) : PlotArray {
            override operator fun get(idx: Int): Float = values[idx * stride]
            override fun count(): Int = values.size
        }

        class PlotArrayFunc(val func: (Int) -> Float, val count: Int) : PlotArray {
            override operator fun get(idx: Int): Float = func(idx)
            override fun count(): Int = count
        }
    }
}