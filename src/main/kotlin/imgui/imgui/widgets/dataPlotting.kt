package imgui.imgui.widgets

import glm_.vec2.Vec2
import imgui.ImGui
import imgui.internal.PlotType

interface dataPlotting {

    fun plotLines(label: String, values: FloatArray, valuesOffset: Int = 0, overlayText: String = "", scaleMin: Float = Float.MAX_VALUE,
                  scaleMax: Float = Float.MAX_VALUE, graphSize: Vec2 = Vec2(), stride: Int = 1) {

        val data = imgui_widgets_main.PlotArrayData(values, stride)
        ImGui.plotEx(PlotType.Lines, label, data, valuesOffset, overlayText, scaleMin, scaleMax, graphSize)
    }

    fun plotLines(label: String, valuesGetter: (idx: Int) -> Float, valuesCount: Int, valuesOffset: Int = 0,
                  overlayText: String = "", scaleMin: Float = Float.MAX_VALUE, scaleMax: Float = Float.MAX_VALUE,
                  graphSize: Vec2 = Vec2()) {

        val data = imgui_widgets_main.PlotArrayFunc(valuesGetter, valuesCount)
        ImGui.plotEx(PlotType.Lines, label, data, valuesOffset, overlayText, scaleMin, scaleMax, graphSize)
    }

    fun plotHistogram(label: String, values: FloatArray, valuesOffset: Int = 0, overlayText: String = "",
                      scaleMin: Float = Float.MAX_VALUE, scaleMax: Float = Float.MAX_VALUE, graphSize: Vec2 = Vec2(), stride: Int = 1) {

        val data = imgui_widgets_main.PlotArrayData(values, stride)
        ImGui.plotEx(PlotType.Histogram, label, data, valuesOffset, overlayText, scaleMin, scaleMax, graphSize)
    }

    fun plotHistogram(label: String, valuesGetter: (idx: Int) -> Float, valuesCount: Int, valuesOffset: Int = 0,
                      overlayText: String = "", scaleMin: Float = Float.MAX_VALUE, scaleMax: Float = Float.MAX_VALUE,
                      graphSize: Vec2 = Vec2()) {

        val data = imgui_widgets_main.PlotArrayFunc(valuesGetter, valuesCount)
        ImGui.plotEx(PlotType.Histogram, label, data, valuesOffset, overlayText, scaleMin, scaleMax, graphSize)
    }
}