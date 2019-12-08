package imgui.api

import glm_.vec2.Vec2
import imgui.ImGui.plotEx
import imgui.PlotArrayData
import imgui.PlotArrayFunc
import imgui.internal.PlotType

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
}