@file:Suppress("UNCHECKED_CAST")

package imgui.imgui

import glm_.*
import imgui.*
import imgui.ImGui.clearActiveId
import imgui.ImGui.formatArgPattern
import imgui.ImGui.getNavInputAmount2d
import imgui.ImGui.io
import imgui.ImGui.isMousePosValid
import imgui.ImGui.parseFormatPrecision
import imgui.ImGui.style
import imgui.imgui.imgui_internal.Companion.getMinimumStepAtDecimalPrecision
import imgui.internal.*
import kool.lim
import java.util.*
import kotlin.math.max
import kotlin.reflect.KMutableProperty0
import imgui.ConfigFlag as Cf
import imgui.WindowFlag as Wf
import imgui.internal.DrawListFlag as Dlf


// Template widget behaviors
// This is called by DragBehavior() when the widget is active (held by mouse or being manipulated with Nav controls)

fun <N> dragBehaviorT(dataType: DataType, vPtr: KMutableProperty0<N>,
                      vSpeed_: Float, vMin: N, vMax: N,
                      format: String, power: Float, flags: DragFlags): Boolean where N : Number, N : Comparable<N> {

    var v by vPtr

    val axis = if (flags has DragFlag.Vertical) Axis.Y else Axis.X

    val isDecimal: Boolean
    val maxDecimal:Any = when(dataType){
        DataType.Float -> {
            isDecimal = true
            Float.MAX_VALUE
        }
        DataType.Double -> {
            isDecimal = true
            Double.MAX_VALUE
        }
        else -> {
            isDecimal = false
        }
    }

    val hasMinMax = vMin != vMax
    val range = vMax - vMin
    val isPower = power != 1f && isDecimal && hasMinMax && range < maxDecimal as N

    // Default tweak speed
    var vSpeed = vSpeed_
    if (vSpeed == 0f && hasMinMax && range < maxDecimal as N)
        vSpeed = range.f * g.dragSpeedDefaultRatio

    // Inputs accumulates into g.DragCurrentAccum, which is flushed into the current value as soon as it makes a difference with our precision settings
    var adjustDelta = 0f
    if (g.activeIdSource == InputSource.Mouse && isMousePosValid() && io.mouseDragMaxDistanceSqr[0] > 1f * 1f) {
        adjustDelta = io.mouseDelta[axis.i]
        if (io.keyAlt)
            adjustDelta *= 1f / 100f
        if (io.keyShift)
            adjustDelta *= 10f
    } else if (g.activeIdSource == InputSource.Nav) {
        val decimalPrecision = if (isDecimal) parseFormatPrecision(format, 3) else 0
        adjustDelta = getNavInputAmount2d(NavDirSourceFlag.Keyboard or NavDirSourceFlag.PadDPad, InputReadMode.RepeatFast, 1f / 10f, 10f)[axis.i]
        vSpeed = vSpeed max getMinimumStepAtDecimalPrecision(decimalPrecision)
    }
    adjustDelta *= vSpeed

    // For vertical drag we currently assume that Up=higher value (like we do with vertical sliders). This may become a parameter.
    if (axis == Axis.Y)
        adjustDelta = -adjustDelta

    /*  Clear current value on activation
        Avoid altering values and clamping when we are _already_ past the limits and heading in the same direction,
        so e.g. if range is 0..255, current value is 300 and we are pushing to the right side, keep the 300.             */
    val isJustActivated = g.activeIdIsJustActivated
    val isAlreadyPastLimitsAndPushingOutward = hasMinMax && ((v >= vMax && adjustDelta > 0f) || (v <= vMin && adjustDelta < 0f))
    val isDragDirectionChangeWithPower = isPower && ((adjustDelta < 0 && g.dragCurrentAccum > 0) || (adjustDelta > 0 && g.dragCurrentAccum < 0))
    if (isJustActivated || isAlreadyPastLimitsAndPushingOutward || isDragDirectionChangeWithPower) {
        g.dragCurrentAccum = 0f
        g.dragCurrentAccumDirty = false
    } else if (adjustDelta != 0f) {
        g.dragCurrentAccum += adjustDelta
        g.dragCurrentAccumDirty = true
    }

    if (!g.dragCurrentAccumDirty)
        return false

    var vCur = v
    var vOldRefForAccumRemainder: Number = when (dataType) {
        DataType.Long, DataType.Ulong, DataType.Double -> 0.0
        else -> 0f
    }

    if (isPower)
    // Offset + round to user desired precision, with a curve on the v_min..v_max range to get more precision on one side of the range
        when (dataType) {
            DataType.Byte, DataType.Ubyte, DataType.Short, DataType.Ushort, DataType.Int, DataType.Uint, DataType.Float -> {
                val vOldNormCurved = glm.pow((vCur - vMin).f / range.f, 1f / power)
                val vNewNormCurved = vOldNormCurved + g.dragCurrentAccum / range.f
                vCur = vMin + glm.pow(saturate(vNewNormCurved), power).`as`(v) * range
                vOldRefForAccumRemainder = vOldNormCurved
            }
            else -> {
                val vOldNormCurved = glm.pow((vCur - vMin).d / range.d, 1.0 / power)
                val vNewNormCurved = vOldNormCurved + g.dragCurrentAccum / range.d
                vCur = vMin + glm.pow(saturate(vNewNormCurved.f), power).`as`(v) * range
                vOldRefForAccumRemainder = vOldNormCurved
            }
        }
    else
        vCur += g.dragCurrentAccum

    // Round to user desired precision based on format string
    vCur = roundScalarWithFormat(format, vCur)

    // Preserve remainder after rounding has been applied. This also allow slow tweaking of values.
    g.dragCurrentAccumDirty = false
    g.dragCurrentAccum -= when {
        isPower -> when (dataType) {
            DataType.Byte, DataType.Ubyte, DataType.Short, DataType.Ushort, DataType.Int, DataType.Uint, DataType.Float -> {
                val vCurNormCurved = glm.pow((vCur - vMin).f / range.f, 1f / power)
                vCurNormCurved - vOldRefForAccumRemainder as Float
            }
            else -> {
                val vCurNormCurved = glm.pow((vCur - vMin).d / range.d, 1.0 / power)
                (vCurNormCurved - vOldRefForAccumRemainder as Double).f
            }
        }
        else -> (vCur.asSigned - v.asSigned).f
    }

    // Lose zero sign for float/double
    when (v) {
        is Float -> if (vCur == -0f) vCur = 0f as N
        is Double -> if (vCur == -0.0) vCur = 0.0 as N
    }


    // Clamp values (+ handle overflow/wrap-around for integer types)
    if (v != vCur && hasMinMax) {
        if (vCur < vMin || (vCur > v && adjustDelta < 0f && !isDecimal))
            vCur = vMin
        if (vCur > vMax || (vCur < v && adjustDelta > 0f && !isDecimal))
            vCur = vMax
    }

    // Apply result
    if (v == vCur)
        return false
    v = vCur
    return true
}

/** ~SetupDrawData */
infix fun DrawData.setup(drawLists: ArrayList<DrawList>) {
    valid = true
    cmdLists.clear()
    if (drawLists.isNotEmpty())
        cmdLists += drawLists
    totalIdxCount = 0
    totalVtxCount = 0
    displayPos put 0f
    displaySize put io.displaySize
    framebufferScale put io.displayFramebufferScale
    for (n in 0 until drawLists.size) {
        totalVtxCount += drawLists[n].vtxBuffer.lim
        totalIdxCount += drawLists[n].idxBuffer.lim
    }
}

// FIXME: Move some of the code into SliderBehavior(). Current responsability is larger than what the equivalent DragBehaviorT<> does, we also do some rendering, etc.

fun <N> sliderBehaviorT(bb: Rect, id: Int,
                        dataType: DataType, vPtr: KMutableProperty0<N>,
                        vMin: N, vMax: N, format: String,
                        power: Float, flags: SliderFlags = 0,
                        outGrabBb: Rect): Boolean where N : Number, N : Comparable<N> {

    var v by vPtr

    val axis = if (flags has SliderFlag.Vertical) Axis.Y else Axis.X
    val isDecimal = dataType == DataType.Float || dataType == DataType.Double
    val isPower = power != 1f && isDecimal

    val grabPadding = 2f
    val sliderSz = (bb.max[axis.i] - bb.min[axis.i]) - grabPadding * 2f
    var grabSz = style.grabMinSize
    val vRange = (if (vMin < vMax) vMax - vMin else vMin - vMax).asSigned
    if (!isDecimal && vRange >= 0)  // vRange < 0 may happen on integer overflows
        grabSz = max(sliderSz / (vRange + 1).f, style.grabMinSize)  // For integer sliders: if possible have the grab size represent 1 unit
    grabSz = grabSz min sliderSz
    val sliderUsableSz = sliderSz - grabSz
    val sliderUsablePosMin = bb.min[axis.i] + grabPadding + grabSz * 0.5f
    val sliderUsablePosMax = bb.max[axis.i] - grabPadding - grabSz * 0.5f

    // For power curve sliders that cross over sign boundary we want the curve to be symmetric around 0f
    val linearZeroPos = when (dataType) {   // 0.0->1.0f
        DataType.Long, DataType.Ulong, DataType.Double -> when {
            isPower && (vMin * vMax).f < 0f -> {
                // Different sign
                val linearDistMinTo0 = glm.pow(if (vMin >= 0) vMin.d else -vMin.d, 1.0 / power)
                val linearDistMaxTo0 = glm.pow(if (vMax >= 0) vMax.d else -vMax.d, 1.0 / power)
                (linearDistMinTo0 / (linearDistMinTo0 + linearDistMaxTo0)).f
            }
            // Same sign
            else -> if (vMin.f < 0f) 1f else 0f
        }
        else -> when {
            isPower && (vMin * vMax).f < 0f -> {
                // Different sign
                val linearDistMinTo0 = glm.pow(if (vMin >= 0) vMin.f else -vMin.f, 1f / power)
                val linearDistMaxTo0 = glm.pow(if (vMax >= 0) vMax.f else -vMax.f, 1f / power)
                linearDistMinTo0 / (linearDistMinTo0 + linearDistMaxTo0)
            }
            // Same sign
            else -> if (vMin.f < 0f) 1f else 0f
        }
    }

    // Process interacting with the slider
    var valueChanged = false
    if (g.activeId == id) {

        var setNewValue = false
        var clickedT = 0f
        if (g.activeIdSource == InputSource.Mouse) {
            if (!io.mouseDown[0])
                clearActiveId()
            else {
                val mouseAbsPos = io.mousePos[axis.i]
                clickedT = when {
                    sliderUsableSz > 0f -> glm.clamp((mouseAbsPos - sliderUsablePosMin) / sliderUsableSz, 0f, 1f)
                    else -> 0f
                }
                if (axis == Axis.Y)
                    clickedT = 1f - clickedT
                setNewValue = true
            }
        } else if (g.activeIdSource == InputSource.Nav) {
            val delta2 = getNavInputAmount2d(NavDirSourceFlag.Keyboard or NavDirSourceFlag.PadDPad, InputReadMode.RepeatFast, 0f, 0f)
            var delta = if (axis == Axis.X) delta2.x else -delta2.y
            if (g.navActivatePressedId == id && !g.activeIdIsJustActivated)
                clearActiveId()
            else if (delta != 0f) {
                clickedT = sliderCalcRatioFromValue(dataType, v, vMin, vMax, power, linearZeroPos)
                val decimalPrecision = if (isDecimal) parseFormatPrecision(format, 3) else 0
                delta = when {
                    decimalPrecision > 0 || isPower -> when { // Gamepad/keyboard tweak speeds in % of slider bounds
                        NavInput.TweakSlow.isDown() -> delta / 1_000f
                        else -> delta / 100f
                    }
                    else -> when {
                        (vRange.f >= -100f && vRange.f <= 100f) || NavInput.TweakSlow.isDown() ->
                            (if (delta < 0f) -1f else +1f) / vRange.f // Gamepad/keyboard tweak speeds in integer steps
                        else -> delta / 100f
                    }
                }
                if (NavInput.TweakFast.isDown())
                    delta *= 10f
                setNewValue = true
                // This is to avoid applying the saturation when already past the limits
                if ((clickedT >= 1f && delta > 0f) || (clickedT <= 0f && delta < 0f))
                    setNewValue = false
                else
                    clickedT = saturate(clickedT + delta)
            }
        }

        if (setNewValue) {
            var vNew = when {
                isPower -> {
                    // Account for power curve scale on both sides of the zero
                    if (clickedT < linearZeroPos) {
                        // Negative: rescale to the negative range before powering
                        var a = 1f - (clickedT / linearZeroPos)
                        a = glm.pow(a, power)
                        lerp(min(vMax, 0.`as`(v)), vMin, a)
                    } else {
                        // Positive: rescale to the positive range before powering
                        var a = when {
                            glm.abs(linearZeroPos - 1f) > 1e-6f -> (clickedT - linearZeroPos) / (1f - linearZeroPos)
                            else -> clickedT
                        }
                        a = glm.pow(a, power)
                        lerp(max(vMin, 0.`as`(v)), vMax, a)
                    }
                }
                else -> when {// Linear slider
                    isDecimal -> lerp(vMin, vMax, clickedT)
                    else -> {
                        /*  For integer values we want the clicking position to match the grab box so we round above
                            This code is carefully tuned to work with large values (e.g. high ranges of U64) while preserving this property..                             */
                        val vNewOff_f: N = when (dataType) {
                            DataType.Long, DataType.Ulong, DataType.Double -> (vMax - vMin).d * clickedT
                            else -> (vMax - vMin).f * clickedT
                        } as N
                        val vNewOffFloor: N
                        val vNewOffRound: N
                        when(dataType) {
                            DataType.Byte, DataType.Short, DataType.Int -> {
                                vNewOffFloor = vNewOff_f.i as N
                                vNewOffRound = (vNewOff_f + 0.5f).i as N
                            }
                            DataType.Long -> {
                                vNewOffFloor = vNewOff_f.L as N
                                vNewOffRound = (vNewOff_f + 0.5).L as N
                            }
                            DataType.Float -> {
                                vNewOffFloor = vNewOff_f.f as N
                                vNewOffRound = (vNewOff_f + 0.5f).f as N
                            }
                            DataType.Double -> {
                                vNewOffFloor = vNewOff_f.d as N
                                vNewOffRound = (vNewOff_f + 0.5).d as N
                            }
                            else -> error("Invalid")
                        }
                        if (!isDecimal && vNewOffFloor < vNewOffRound)
                            vMin + vNewOffRound
                        else
                            vMin + vNewOffFloor
                    }
                }
            }
            // Round past decimal precision
            vNew = roundScalarWithFormat(format, vNew)

            // Apply result
            if (v != vNew) {
                v = vNew
                valueChanged = true
            }
        }
    }

    if (sliderSz < 1f)
        outGrabBb.put(bb.min, bb.min)
    else {
        // Output grab position so it can be displayed by the caller
        var grabT = sliderCalcRatioFromValue(dataType, v, vMin, vMax, power, linearZeroPos)
        if (axis == Axis.Y)
            grabT = 1f - grabT
        val grabPos = lerp(sliderUsablePosMin, sliderUsablePosMax, grabT)
        if (axis == Axis.X)
            outGrabBb.put(grabPos - grabSz * 0.5f, bb.min.y + grabPadding, grabPos + grabSz * 0.5f, bb.max.y - grabPadding)
        else
            outGrabBb.put(bb.min.x + grabPadding, grabPos - grabSz * 0.5f, bb.max.x - grabPadding, grabPos + grabSz * 0.5f)
    }

    return valueChanged
}

fun <N> sliderCalcRatioFromValue(dataType: DataType, v: N, vMin: N, vMax: N, power: Float, linearZeroPos: Float): Float
        where N : Number, N : Comparable<N> {

    if (vMin == vMax) return 0f

    val isPower = power != 1f && (dataType == DataType.Float || dataType == DataType.Double)
    val vClamped = if (vMin < vMax) clamp(v, vMin, vMax) else clamp(v, vMax, vMin)
    return when {
        isPower -> when {
            vClamped.f < 0f -> {
                val f = 1f - ((vClamped - vMin) / (min(0.`as`(v), vMax) - vMin)).f
                (1f - glm.pow(f, 1f / power)) * linearZeroPos
            }
            else -> {
                val f = ((vClamped - max(0.`as`(v), vMin)) / (vMax - max(0.`as`(v), vMin))).f
                linearZeroPos + glm.pow(f, 1f / power) * (1f - linearZeroPos)
            }
        }
        // Linear slider
        else -> when(dataType) {
            DataType.Long, DataType.Ulong, DataType.Double -> ((vClamped - vMin).d / (vMax - vMin).d).f
            else -> (vClamped - vMin).f / (vMax - vMin).f
        }
    }
}

/*
fun sliderCalcRatioFromValue(dataType: DataType, v: Int, vMin: Int, vMax: Int, power: Float, linearZeroPos: Float): Float {

    if (vMin == vMax) return 0f

    val isPower = power != 1f && (dataType == DataType.Float || dataType == DataType.Double)
    val vClamped = if (vMin < vMax) glm.clamp(v, vMin, vMax) else glm.clamp(v, vMax, vMin)
    return when {
        isPower -> when {
            vClamped < 0f -> {
                val f = 1f - (vClamped - vMin) / (min(0, vMax) - vMin)
                (1f - glm.pow(f, 1f / power)) * linearZeroPos
            }
            else -> {
                val f = ((vClamped - max(0, vMin)) / (vMax - max(0, vMin))).f
                linearZeroPos + glm.pow(f, 1f / power) * (1f - linearZeroPos)
            }
        }
        // Linear slider
        else -> (vClamped - vMin).f / (vMax - vMin).f
    }
}

fun sliderCalcRatioFromValue(dataType: DataType, v: Long, vMin: Long, vMax: Long, power: Float, linearZeroPos: Float): Float {

    if (vMin == vMax) return 0f

    val isPower = power != 1f && (dataType == DataType.Float || dataType == DataType.Double)
    val vClamped = if (vMin < vMax) glm.clamp(v, vMin, vMax) else glm.clamp(v, vMax, vMin)
    return when {
        isPower -> when {
            vClamped < 0f -> {
                val f = 1f - (vClamped - vMin) / (min(0, vMax) - vMin)
                (1f - glm.pow(f, 1f / power)) * linearZeroPos
            }
            else -> {
                val f = ((vClamped - max(0, vMin)) / (vMax - max(0, vMin))).f
                linearZeroPos + glm.pow(f, 1f / power) * (1f - linearZeroPos)
            }
        }
        // Linear slider
        else -> (vClamped - vMin).f / (vMax - vMin).f
    }
}

fun sliderCalcRatioFromValue(dataType: DataType, v: Float, vMin: Float, vMax: Float, power: Float, linearZeroPos: Float): Float {

    if (vMin == vMax) return 0f

    val isPower = power != 1f && (dataType == DataType.Float || dataType == DataType.Double)
    val vClamped = if (vMin < vMax) glm.clamp(v, vMin, vMax) else glm.clamp(v, vMax, vMin)
    return when {
        isPower -> when {
            vClamped < 0f -> {
                val f = 1f - (vClamped - vMin) / (min(0f, vMax) - vMin)
                (1f - glm.pow(f, 1f / power)) * linearZeroPos
            }
            else -> {
                val f = (vClamped - max(0f, vMin)) / (vMax - max(0f, vMin))
                linearZeroPos + glm.pow(f, 1f / power) * (1f - linearZeroPos)
            }
        }
        // Linear slider
        else -> (vClamped - vMin) / (vMax - vMin)
    }
}

fun sliderCalcRatioFromValue(dataType: DataType, v: Double, vMin: Double, vMax: Double, power: Float, linearZeroPos: Float): Float {

    if (vMin == vMax) return 0f

    val isPower = power != 1f && (dataType == DataType.Float || dataType == DataType.Double)
    val vClamped = if (vMin < vMax) glm.clamp(v, vMin, vMax) else glm.clamp(v, vMax, vMin)
    return when {
        isPower -> when {
            vClamped < 0f -> {
                val f = 1f - ((vClamped - vMin) / (min(0.0, vMax) - vMin)).f
                (1f - glm.pow(f, 1f / power)) * linearZeroPos
            }
            else -> {
                val f = ((vClamped - max(0.0, vMin)) / (vMax - max(0.0, vMin))).f
                linearZeroPos + glm.pow(f, 1f / power) * (1f - linearZeroPos)
            }
        }
        // Linear slider
        else -> ((vClamped - vMin) / (vMax - vMin)).f
    }
}*/

fun <N : Number> roundScalarWithFormat(format: String, value: N): N {
    if (format.isEmpty()) return value
    if (value is Int || value is Long) return value
    val matcher = formatArgPattern.matcher(format)
    var arg = ""
    var i = 0
    while (matcher.find(i)) {
        if (format[matcher.end() - 1] != '%') {
            arg = matcher.group()
            break
        }
        i = matcher.end()
    }
    if (arg.isEmpty()) // Don't apply if the value is not visible in the format string
        return value
    var formattedValue = arg.format(Locale.US, value).trim().replace(",", "")
    if (formattedValue.contains('(')) {
        formattedValue = formattedValue.replace("(", "").replace(")", "")
        formattedValue = "-$formattedValue"
    }
    return when (value) {
        is Byte -> formattedValue.parseByte as N
        is Short -> formattedValue.parseShort as N
        is Int -> formattedValue.parseInt as N
        is Long -> formattedValue.parseLong as N
        is Float -> formattedValue.parseFloat as N
        is Double -> formattedValue.parseDouble as N
        else -> throw Error("not supported")
    }
}