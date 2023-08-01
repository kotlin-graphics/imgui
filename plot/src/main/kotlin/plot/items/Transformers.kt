package plot.items

import glm_.d
import glm_.f
import glm_.vec2.Vec2
import plot.api.PlotPoint
import plot.api.PlotTransform
import plot.api.get
import plot.internalApi.PlotAxis
import plot.internalApi.PlotPlot
import plot.internalApi.gImPlot

//-----------------------------------------------------------------------------
// [SECTION] Transformers
//-----------------------------------------------------------------------------

class Transformer1(val pixMin: Double,
                   val pltMin: Double,
                   val pltMax: Double,
                   val m: Double,
                   val scaMin: Double,
                   val scaMax: Double,
                   val transformFwd: PlotTransform?,
                   val transformData: Any?) {

    operator fun get(p_: Double): Float {
        var p = p_
        if (transformFwd != null) {
            val s = transformFwd!!(p, transformData)
            val t = (s - scaMin) / (scaMax - scaMin)
            p = pltMin + (pltMax - pltMin) * t
        }
        return (pixMin + m * (p - pltMin)).f
    }
}

class Transformer2(xAxis: PlotAxis, yAxis: PlotAxis) {
    val tx = Transformer1(xAxis.pixelMin.d,
                          xAxis.range.min,
                          xAxis.range.max,
                          xAxis.scaleToPixel,
                          xAxis.scaleMin,
                          xAxis.scaleMax,
                          xAxis.transformForward,
                          xAxis.transformData)
    val ty = Transformer1(yAxis.pixelMin.d,
                          yAxis.range.min,
                          yAxis.range.max,
                          yAxis.scaleToPixel,
                          yAxis.scaleMin,
                          yAxis.scaleMax,
                          yAxis.transformForward,
                          yAxis.transformData)

    constructor(plot: PlotPlot) : this(plot.axes[plot.currentX], plot.axes[plot.currentY])

    constructor() : this(gImPlot.currentPlot!!)

    operator fun get(plt: PlotPoint) = Vec2(tx[plt.x], ty[plt.y])

//    template <typename T> IMPLOT_INLINE ImVec2 operator ()(T x, T y)
//    const {
//        ImVec2 out
//                out.x = Tx(x)
//        out.y = Ty(y)
//        return out
//    }
}