package plot.internalApi

import glm_.glm.clamp
import imgui.DoubleOps.pow
import kotlin.math.asinh
import kotlin.math.log10
import kotlin.math.sinh

//-----------------------------------------------------------------------------
// [SECTION] Transforms
//-----------------------------------------------------------------------------

fun transformForward_Log10(v: Double, void: Any?) = log10(if(v <= 0.0) Double.MIN_VALUE else v)

fun transformInverse_Log10(v: Double, void: Any?) = 10.0.pow(v)

fun transformForward_SymLog(v: Double, void: Any?) = 2.0 * asinh(v / 2.0)

fun transformInverse_SymLog(v: Double, void: Any?) = 2.0 * sinh(v / 2.0)

fun transformForward_Logit(v: Double, void: Any?): Double {
    val vl = clamp(v, Double.MIN_VALUE, 1.0 - Double.MIN_VALUE)
    return log10(vl / (1 - vl))
}

fun transformInverse_Logit(v: Double/*, void**/) = 1.0 / (1.0 + 10.0.pow(-v))