@file:OptIn(ExperimentalStdlibApi::class)

package plot.internalApi

import glm_.func.common.floor
import glm_.i
import glm_.vec2.Vec2
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

//-----------------------------------------------------------------------------
// [SECTION] Math and Misc Utils
//-----------------------------------------------------------------------------

// Rounds x to powers of 2,5 and 10 for generating axis labels (from Graphics Gems 1 Chapter 11.2)
fun niceNum(x: Double, round: Boolean): Double {
    val expv = log10(x).floor.i
    val f = x / 10.0.pow(expv)
    val nf = when {
        round -> when {
            f < 1.5 -> 1
            f < 3 -> 2
            f < 7 -> 5
            else -> 10
        }
        f <= 1 -> 1
        f <= 2 -> 2
        f <= 5 -> 5
        else -> 10
    }
    return nf * 10.0.pow(expv)
}

// Computes order of magnitude of double.
fun orderOfMagnitude(value: Double) = if (value == 0.0) 0 else (floor(log10(abs(value)))).i

// Returns the precision required for a order of magnitude.
fun orderToPrecision(order: Int) = if (order > 0) 0 else 1 - order

// Returns a floating point precision to use given a value
fun precision(value: Double) = orderToPrecision(orderOfMagnitude(value))

// Round a value to a given precision
fun roundTo(value: Double, prec: Int): Double {
    val p = 10.0.pow(prec)
    return floor(value * p + 0.5) / p
}

// Returns the intersection point of two lines A and B (assumes they are not parallel!)
fun intersection(a1: Vec2, a2: Vec2, b1: Vec2, b2: Vec2): Vec2 {
    val v1 = (a1.x * a2.y - a1.y * a2.x)
    val v2 = (b1.x * b2.y - b1.y * b2.x)
    val v3 = ((a1.x - a2.x) * (b1.y - b2.y) - (a1.y - a2.y) * (b1.x - b2.x))
    return Vec2((v1 * (b1.x - b2.x) - v2 * (a1.x - a2.x)) / v3, (v1 * (b1.y - b2.y) - v2 * (a1.y - a2.y)) / v3)
}

// Fills a buffer with n samples linear interpolated from vmin to vmax
fun DoubleArray.fillRange(vMin: Double, vMax: Double) {
    val step = (vMax - vMin) / lastIndex
    for (i in indices)
        this[i] = vMin + i * step
}

//// Calculate histogram bin counts and widths
//template <typename T>
//static inline void CalculateBins(const T* values, int count, ImPlotBin meth, const ImPlotRange& range, int& bins_out, double& width_out) {
//    switch (meth) {
//        case ImPlotBin_Sqrt:
//        bins_out  = (int)ceil(sqrt(count));
//        break;
//        case ImPlotBin_Sturges:
//        bins_out  = (int)ceil(1.0 + log2(count));
//        break;
//        case ImPlotBin_Rice:
//        bins_out  = (int)ceil(2 * cbrt(count));
//        break;
//        case ImPlotBin_Scott:
//        width_out = 3.49 * ImStdDev(values, count) / cbrt(count);
//        bins_out  = (int)round(range.Size() / width_out);
//        break;
//    }
//    width_out = range.Size() / bins_out;
//}