@file:OptIn(ExperimentalStdlibApi::class)

package demo

import glm_.f
import imgui.ImGui
import plot.api.*
import kotlin.math.sin

object LinePlots {

    val xs1 = FloatArray(1001)
    val ys1 = FloatArray(1001)
    val xs2 = DoubleArray(20)
    val ys2 = DoubleArray(20)
    operator fun invoke() {
        for (i in 0..<1_001) {
            xs1[i] = i * 0.001f
            ys1[i] = 0.5f + 0.5f * sin(50 * (xs1[i] + ImGui.time.f / 10))
        }
        for (i in 0..<20) {
            xs2[i] = i * 1 / 19.0
            ys2[i] = xs2[i] * xs2[i]
        }
        if (beginPlot("Line Plots")) {
            setupAxes("x", "y")
            plotLine("f(x)", xs1, ys1)
            setNextMarkerStyle(PlotMarker.Circle)
            plotLine("g(x)", xs2, ys2, PlotLineFlag.Segments)
            endPlot()
        }
    }
}