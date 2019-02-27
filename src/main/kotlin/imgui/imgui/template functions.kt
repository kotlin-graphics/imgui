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
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KMutableProperty0
import imgui.ConfigFlag as Cf
import imgui.WindowFlag as Wf
import imgui.internal.DrawListFlag as Dlf


// Template widget behaviors
// This is called by DragBehavior() when the widget is active (held by mouse or being manipulated with Nav controls)

fun dragBehaviorT(dataType: DataType, vPtr: KMutableProperty0<*>, vSpeed_: Float, vMin: Int, vMax: Int, format: String,
                  power: Float, flags: DragFlags): Boolean {

    var v by vPtr as KMutableProperty0<Int>

    val axis = if (flags has DragFlag.Vertical) Axis.Y else Axis.X
    val isDecimal = false
    val hasMinMax = vMin != vMax
    val isPower = power != 1f && isDecimal && hasMinMax && (vMax - vMin < Int.MAX_VALUE)

    // Default tweak speed
    var vSpeed = vSpeed_
    if (vSpeed == 0f && hasMinMax && (vMax - vMax < Int.MAX_VALUE))
        vSpeed = (vMax - vMin) * g.dragSpeedDefaultRatio

    // Inputs accumulates into g.DragCurrentAccum, which is flushed into the current value as soon as it makes a difference with our precision settings
    var adjustDelta = 0f
    if (g.activeIdSource == InputSource.Mouse && isMousePosValid() && io.mouseDragMaxDistanceSqr[0] > 1f * 1f) {
        adjustDelta = io.mouseDelta[axis.i]
        if (io.keyAlt)
            adjustDelta *= 1f / 100f
        if (io.keyShift)
            adjustDelta *= 10f
    } else if (g.activeIdSource == InputSource.Nav) {
        val decimalPrecision = when {
            isDecimal -> parseFormatPrecision(format, 3)
            else -> 0
        }
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
    var vOldRefForAccumRemainder = 0f

    if (isPower) {
        // Offset + round to user desired precision, with a curve on the v_min..v_max range to get more precision on one side of the range
        val vOldNormCurved = glm.pow((vCur - vMin).f / (vMax - vMin).f, 1f / power)
        val vNewNormCurved = vOldNormCurved + g.dragCurrentAccum / (vMax - vMin)
        vCur = vMin + glm.pow(saturate(vNewNormCurved), power).i * (vMax - vMin)
        vOldRefForAccumRemainder = vOldNormCurved
    } else vCur += g.dragCurrentAccum.i

    // Round to user desired precision based on format string
    vCur = roundScalarWithFormat(format, vCur)

    // Preserve remainder after rounding has been applied. This also allow slow tweaking of values.
    g.dragCurrentAccumDirty = false
    g.dragCurrentAccum -= when {
        isPower -> {
            val vCurNormCurved = glm.pow((vCur - vMin).f / (vMax - vMin).f, 1f / power)
            vCurNormCurved - vOldRefForAccumRemainder
        }
        else -> (vCur - v).f
    }

    // Lose zero sign for float/double
    if (vCur == -0)
        vCur = 0

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

fun dragBehaviorT(dataType: DataType, vPtr: KMutableProperty0<*>, vSpeed_: Float, vMin: Long, vMax: Long, format: String,
                  power: Float, flags: DragFlags): Boolean {

    var v by vPtr as KMutableProperty0<Long>

    val axis = if (flags has DragFlag.Vertical) Axis.Y else Axis.X
    val isDecimal = false
    val hasMinMax = vMin != vMax
    val isPower = power != 1f && isDecimal && hasMinMax && (vMax - vMin < Long.MAX_VALUE)

    // Default tweak speed
    var vSpeed = vSpeed_
    if (vSpeed == 0f && hasMinMax && (vMax - vMax < Long.MAX_VALUE))
        vSpeed = (vMax - vMin) * g.dragSpeedDefaultRatio

    // Inputs accumulates into g.DragCurrentAccum, which is flushed into the current value as soon as it makes a difference with our precision settings
    var adjustDelta = 0f
    if (g.activeIdSource == InputSource.Mouse && isMousePosValid() && io.mouseDragMaxDistanceSqr[0] > 1f * 1f) {
        adjustDelta = io.mouseDelta[axis.i]
        if (io.keyAlt)
            adjustDelta *= 1f / 100f
        if (io.keyShift)
            adjustDelta *= 10f
    } else if (g.activeIdSource == InputSource.Nav) {
        val decimalPrecision = when {
            isDecimal -> parseFormatPrecision(format, 3)
            else -> 0
        }
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
    var vOldRefForAccumRemainder = 0.0

    if (isPower) {
        // Offset + round to user desired precision, with a curve on the v_min..v_max range to get more precision on one side of the range
        val vOldNormCurved = glm.pow((vCur - vMin).d / (vMax - vMin).d, 1.0 / power)
        val vNewNormCurved = vOldNormCurved + g.dragCurrentAccum / (vMax - vMin)
        vCur = vMin + glm.pow(saturate(vNewNormCurved.f), power).L * (vMax - vMin)
        vOldRefForAccumRemainder = vOldNormCurved
    } else vCur += g.dragCurrentAccum.L

    // Round to user desired precision based on format string
    vCur = roundScalarWithFormat(format, vCur)

    // Preserve remainder after rounding has been applied. This also allow slow tweaking of values.
    g.dragCurrentAccumDirty = false
    g.dragCurrentAccum -= when {
        isPower -> {
            val vCurNormCurved = glm.pow((vCur - vMin).f / (vMax - vMin).f, 1f / power)
            (vCurNormCurved - vOldRefForAccumRemainder).f
        }
        else -> (vCur - v).f
    }

    // Lose zero sign for float/double
    if (vCur == -0L)
        vCur = 0

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

fun dragBehaviorT(dataType: DataType, vPtr: KMutableProperty0<*>, vSpeed_: Float, vMin: Float, vMax: Float, format: String,
                  power: Float, flags: DragFlags): Boolean {

    var v by vPtr as KMutableProperty0<Float>

    val axis = if (flags has DragFlag.Vertical) Axis.Y else Axis.X
    val isDecimal = true
    val hasMinMax = vMin != vMax
    val isPower = power != 1f && isDecimal && hasMinMax && (vMax - vMin < Float.MAX_VALUE)

    // Default tweak speed
    var vSpeed = vSpeed_
    if (vSpeed == 0f && hasMinMax && (vMax - vMax < Long.MAX_VALUE))
        vSpeed = (vMax - vMin) * g.dragSpeedDefaultRatio

    // Inputs accumulates into g.DragCurrentAccum, which is flushed into the current value as soon as it makes a difference with our precision settings
    var adjustDelta = 0f
    if (g.activeIdSource == InputSource.Mouse && isMousePosValid() && io.mouseDragMaxDistanceSqr[0] > 1f * 1f) {
        adjustDelta = io.mouseDelta[axis.i]
        if (io.keyAlt)
            adjustDelta *= 1f / 100f
        if (io.keyShift)
            adjustDelta *= 10f
    } else if (g.activeIdSource == InputSource.Nav) {
        val decimalPrecision = when {
            isDecimal -> parseFormatPrecision(format, 3)
            else -> 0
        }
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
    var vOldRefForAccumRemainder = 0f

    if (isPower) {
        // Offset + round to user desired precision, with a curve on the v_min..v_max range to get more precision on one side of the range
        val vOldNormCurved = glm.pow((vCur - vMin).d / (vMax - vMin).d, 1.0 / power).f
        val vNewNormCurved = vOldNormCurved + g.dragCurrentAccum / (vMax - vMin)
        vCur = vMin + glm.pow(saturate(vNewNormCurved.f), power) * (vMax - vMin)
        vOldRefForAccumRemainder = vOldNormCurved
    } else vCur += g.dragCurrentAccum
    // Round to user desired precision based on format string
    vCur = roundScalarWithFormat(format, vCur)

    // Preserve remainder after rounding has been applied. This also allow slow tweaking of values.
    g.dragCurrentAccumDirty = false
    g.dragCurrentAccum -= when {
        isPower -> {
            val vCurNormCurved = glm.pow((vCur - vMin).f / (vMax - vMin).f, 1f / power)
            (vCurNormCurved - vOldRefForAccumRemainder).f
        }
        else -> (vCur - v).f
    }

    // Lose zero sign for float/double
    if (vCur == -0f)
        vCur = 0f

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

fun dragBehaviorT(dataType: DataType, vPtr: KMutableProperty0<*>, vSpeed_: Float, vMin: Double, vMax: Double, format: String,
                  power: Float, flags: DragFlags): Boolean {

    var v by vPtr as KMutableProperty0<Double>

    val axis = if (flags has DragFlag.Vertical) Axis.Y else Axis.X
    val isDecimal = true
    val hasMinMax = vMin != vMax
    val isPower = power != 1f && isDecimal && hasMinMax && (vMax - vMin < Double.MAX_VALUE)

    // Default tweak speed
    var vSpeed = vSpeed_
    if (vSpeed == 0f && hasMinMax && (vMax - vMax < Long.MAX_VALUE))
        vSpeed = (vMax - vMin).f * g.dragSpeedDefaultRatio

    // Inputs accumulates into g.DragCurrentAccum, which is flushed into the current value as soon as it makes a difference with our precision settings
    var adjustDelta = 0f
    if (g.activeIdSource == InputSource.Mouse && isMousePosValid() && io.mouseDragMaxDistanceSqr[0] > 1f * 1f) {
        adjustDelta = io.mouseDelta[axis.i]
        if (io.keyAlt)
            adjustDelta *= 1f / 100f
        if (io.keyShift)
            adjustDelta *= 10f
    } else if (g.activeIdSource == InputSource.Nav) {
        val decimalPrecision = when {
            isDecimal -> parseFormatPrecision(format, 3)
            else -> 0
        }
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
    var vOldRefForAccumRemainder = 0.0

    if (isPower) {
        // Offset + round to user desired precision, with a curve on the v_min..v_max range to get more precision on one side of the range
        val vOldNormCurved = glm.pow((vCur - vMin).d / (vMax - vMin).d, 1.0 / power)
        val vNewNormCurved = vOldNormCurved + g.dragCurrentAccum / (vMax - vMin)
        vCur = vMin + glm.pow(saturate(vNewNormCurved.f), power).d * (vMax - vMin)
        vOldRefForAccumRemainder = vOldNormCurved
    } else vCur += g.dragCurrentAccum.d

    // Round to user desired precision based on format string
    vCur = roundScalarWithFormat(format, vCur)

    // Preserve remainder after rounding has been applied. This also allow slow tweaking of values.
    g.dragCurrentAccumDirty = false
    g.dragCurrentAccum -= when {
        isPower -> {
            val vCurNormCurved = glm.pow((vCur - vMin).f / (vMax - vMin).f, 1f / power)
            (vCurNormCurved - vOldRefForAccumRemainder).f
        }
        else -> (vCur - v).f
    }

    // Lose zero sign for float/double
    if (vCur == -0.0)
        vCur = 0.0

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
        totalVtxCount += drawLists[n].vtxBuffer.size
        totalIdxCount += drawLists[n].idxBuffer.size
    }
}

// FIXME: Move some of the code into SliderBehavior(). Current responsability is larger than what the equivalent DragBehaviorT<> does, we also do some rendering, etc.

fun sliderBehaviorT(bb: Rect, id: Int, dataType: DataType, vPtr: KMutableProperty0<*>, vMin: Int, vMax: Int, format: String,
                    power: Float, flags: SliderFlags = 0, outGrabBb: Rect): Boolean {

    var v by vPtr as KMutableProperty0<Int>

    val axis = if (flags has SliderFlag.Vertical) Axis.Y else Axis.X
    val isDecimal = dataType == DataType.Float || dataType == DataType.Double
    val isPower = power != 0f && isDecimal

    val grabPadding = 2f
    val sliderSz = (bb.max[axis.i] - bb.min[axis.i]) - grabPadding * 2f
    var grabSz = style.grabMinSize
    val vRange = if (vMin < vMax) vMax - vMin else vMin - vMax
    if (!isDecimal && vRange >= 0)  // vRange < 0 may happen on integer overflows
        grabSz = max(sliderSz / (vRange + 1), style.grabMinSize)  // For integer sliders: if possible have the grab size represent 1 unit
    grabSz = grabSz min sliderSz
    val sliderUsableSz = sliderSz - grabSz
    val sliderUsablePosMin = bb.min[axis.i] + grabPadding + grabSz * 0.5f
    val sliderUsablePosMax = bb.max[axis.i] - grabPadding - grabSz * 0.5f

    // For power curve sliders that cross over sign boundary we want the curve to be symmetric around 0f
    val linearZeroPos = when {   // 0.0->1.0f
        vMin * vMax < 0f -> {
            // Different sign
            val linearDistMinTo0 = glm.pow(if (vMin >= 0) vMin.f else -vMin.f, 1f / power)
            val linearDistMaxTo0 = glm.pow(if (vMax >= 0) vMax.f else -vMax.f, 1f / power)
            linearDistMinTo0 / (linearDistMinTo0 + linearDistMaxTo0)
        } // Same sign
        else -> if (vMin < 0f) 1f else 0f
    }

    // Process interacting with the slider
    var valueChanged = false
    if (g.activeId == id) {

        var setNewValue = false
        var clickedT = 0f

        if (g.activeIdSource == InputSource.Mouse) {
            if (!io.mouseDown[0]) clearActiveId()
            else {
                val mouseAbsPos = io.mousePos[axis.i]
                clickedT = if (sliderUsableSz > 0f) glm.clamp((mouseAbsPos - sliderUsablePosMin) / sliderUsableSz, 0f, 1f) else 0f
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
                        (vRange >= -100f && vRange <= 100f) || NavInput.TweakSlow.isDown() ->
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
                        lerp(glm.min(vMax, 0), vMin, a)
                    } else {
                        // Positive: rescale to the positive range before powering
                        var a = when {
                            glm.abs(linearZeroPos - 1f) > 1e-6f -> (clickedT - linearZeroPos) / (1f - linearZeroPos)
                            else -> clickedT
                        }
                        a = glm.pow(a, power)
                        lerp(glm.max(vMin, 0), vMax, a)
                    }
                }
                else -> when {// Linear slider
                    isDecimal -> lerp(vMin, vMax, clickedT)
                    else -> {
                        /*  For integer values we want the clicking position to match the grab box so we round above
                            This code is carefully tuned to work with large values (e.g. high ranges of U64) while preserving this property..                             */
                        val vNewOff_f = (vMax - vMin) * clickedT
                        val vNewOffFloor = vNewOff_f.i
                        val vNewOffRound = (vNewOff_f + 0.5f).i
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

    // Output grab position so it can be displayed by the caller
    var grabT = sliderCalcRatioFromValue(dataType, v, vMin, vMax, power, linearZeroPos)
    if (axis == Axis.Y)
        grabT = 1f - grabT
    val grabPos = lerp(sliderUsablePosMin, sliderUsablePosMax, grabT)
    if (axis == Axis.X)
        outGrabBb.put(grabPos - grabSz * 0.5f, bb.min.y + grabPadding, grabPos + grabSz * 0.5f, bb.max.y - grabPadding)
    else
        outGrabBb.put(bb.min.x + grabPadding, grabPos - grabSz * 0.5f, bb.max.x - grabPadding, grabPos + grabSz * 0.5f)

    return valueChanged
}

fun sliderBehaviorT(bb: Rect, id: Int, dataType: DataType, vPtr: KMutableProperty0<*>, vMin: Long, vMax: Long, format: String,
                    power: Float, flags: SliderFlags = 0, outGrabBb: Rect): Boolean {

    var v by vPtr as KMutableProperty0<Long>

    val axis = if (flags has SliderFlag.Vertical) Axis.Y else Axis.X
    val isDecimal = dataType == DataType.Float || dataType == DataType.Double
    val isPower = power != 0f && isDecimal

    val grabPadding = 2f
    val sliderSz = (bb.max[axis.i] - bb.min[axis.i]) - grabPadding * 2f
    var grabSz = style.grabMinSize
    val vRange = if (vMin < vMax) vMax - vMin else vMin - vMax
    if (!isDecimal && vRange >= 0)  // vRange < 0 may happen on integer overflows
        grabSz = max(sliderSz / (vRange + 1).f, style.grabMinSize)  // For integer sliders: if possible have the grab size represent 1 unit
    grabSz = grabSz min sliderSz
    val sliderUsableSz = sliderSz - grabSz
    val sliderUsablePosMin = bb.min[axis.i] + grabPadding + grabSz * 0.5f
    val sliderUsablePosMax = bb.max[axis.i] - grabPadding - grabSz * 0.5f

    // For power curve sliders that cross over sign boundary we want the curve to be symmetric around 0f
    val linearZeroPos = when {   // 0.0->1.0f
        vMin * vMax < 0f -> {
            // Different sign
            val linearDistMinTo0 = glm.pow(if (vMin >= 0) vMin.f else -vMin.f, 1f / power)
            val linearDistMaxTo0 = glm.pow(if (vMax >= 0) vMax.f else -vMax.f, 1f / power)
            linearDistMinTo0 / (linearDistMinTo0 + linearDistMaxTo0)
        } // Same sign
        else -> if (vMin < 0f) 1f else 0f
    }

    // Process interacting with the slider
    var valueChanged = false
    if (g.activeId == id) {

        var setNewValue = false
        var clickedT = 0f

        if (g.activeIdSource == InputSource.Mouse) {
            if (!io.mouseDown[0]) clearActiveId()
            else {
                val mouseAbsPos = io.mousePos[axis.i]
                clickedT = if (sliderUsableSz > 0f) glm.clamp((mouseAbsPos - sliderUsablePosMin) / sliderUsableSz, 0f, 1f) else 0f
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
                        (vRange >= -100f && vRange <= 100f) || NavInput.TweakSlow.isDown() ->
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
                        lerp(glm.min(vMax, 0L), vMin, a)
                    } else {
                        // Positive: rescale to the positive range before powering
                        var a = when {
                            glm.abs(linearZeroPos - 1f) > 1e-6f -> (clickedT - linearZeroPos) / (1f - linearZeroPos)
                            else -> clickedT
                        }
                        a = glm.pow(a, power)
                        lerp(glm.max(vMin, 0L), vMax, a)
                    }
                }
                else -> when {// Linear slider
                    isDecimal -> lerp(vMin, vMax, clickedT)
                    else -> {
                        /*  For integer values we want the clicking position to match the grab box so we round above
                            This code is carefully tuned to work with large values (e.g. high ranges of U64) while preserving this property..                             */
                        val vNewOff_f = (vMax - vMin) * clickedT
                        val vNewOffFloor = vNewOff_f.L
                        val vNewOffRound = (vNewOff_f + 0.5f).L
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

    // Output grab position so it can be displayed by the caller
    var grabT = sliderCalcRatioFromValue(dataType, v, vMin, vMax, power, linearZeroPos)
    if (axis == Axis.Y)
        grabT = 1f - grabT
    val grabPos = lerp(sliderUsablePosMin, sliderUsablePosMax, grabT)
    if (axis == Axis.X)
        outGrabBb.put(grabPos - grabSz * 0.5f, bb.min.y + grabPadding, grabPos + grabSz * 0.5f, bb.max.y - grabPadding)
    else
        outGrabBb.put(bb.min.x + grabPadding, grabPos - grabSz * 0.5f, bb.max.x - grabPadding, grabPos + grabSz * 0.5f)

    return valueChanged
}

fun sliderBehaviorT(bb: Rect, id: Int, dataType: DataType, vPtr: KMutableProperty0<*>, vMin: Float, vMax: Float, format: String,
                    power: Float, flags: SliderFlags = 0, outGrabBb: Rect): Boolean {

    var v by vPtr as KMutableProperty0<Float>

    val axis = if (flags has SliderFlag.Vertical) Axis.Y else Axis.X
    val isDecimal = dataType == DataType.Float || dataType == DataType.Double
    val isPower = power != 0f && isDecimal

    val grabPadding = 2f
    val sliderSz = (bb.max[axis.i] - bb.min[axis.i]) - grabPadding * 2f
    var grabSz = style.grabMinSize
    val vRange = if (vMin < vMax) vMax - vMin else vMin - vMax
    if (!isDecimal && vRange >= 0)  // vRange < 0 may happen on integer overflows
        grabSz = max(sliderSz / (vRange + 1).f, style.grabMinSize)  // For integer sliders: if possible have the grab size represent 1 unit
    grabSz = grabSz min sliderSz
    val sliderUsableSz = sliderSz - grabSz
    val sliderUsablePosMin = bb.min[axis.i] + grabPadding + grabSz * 0.5f
    val sliderUsablePosMax = bb.max[axis.i] - grabPadding - grabSz * 0.5f

    // For power curve sliders that cross over sign boundary we want the curve to be symmetric around 0f
    val linearZeroPos = when {   // 0.0->1.0f
        vMin * vMax < 0f -> {
            // Different sign
            val linearDistMinTo0 = glm.pow(if (vMin >= 0) vMin.f else -vMin.f, 1f / power)
            val linearDistMaxTo0 = glm.pow(if (vMax >= 0) vMax.f else -vMax.f, 1f / power)
            linearDistMinTo0 / (linearDistMinTo0 + linearDistMaxTo0)
        } // Same sign
        else -> if (vMin < 0f) 1f else 0f
    }

    // Process interacting with the slider
    var valueChanged = false
    if (g.activeId == id) {

        var setNewValue = false
        var clickedT = 0f

        if (g.activeIdSource == InputSource.Mouse) {
            if (!io.mouseDown[0]) clearActiveId()
            else {
                val mouseAbsPos = io.mousePos[axis.i]
                clickedT = if (sliderUsableSz > 0f) glm.clamp((mouseAbsPos - sliderUsablePosMin) / sliderUsableSz, 0f, 1f) else 0f
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
                        (vRange >= -100f && vRange <= 100f) || NavInput.TweakSlow.isDown() ->
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
                        lerp(glm.min(vMax, 0f), vMin, a)
                    } else {
                        // Positive: rescale to the positive range before powering
                        var a = when {
                            glm.abs(linearZeroPos - 1f) > 1e-6f -> (clickedT - linearZeroPos) / (1f - linearZeroPos)
                            else -> clickedT
                        }
                        a = glm.pow(a, power)
                        lerp(glm.max(vMin, 0f), vMax, a)
                    }
                }
                else -> when {// Linear slider
                    isDecimal -> lerp(vMin, vMax, clickedT)
                    else -> {
                        /*  For integer values we want the clicking position to match the grab box so we round above
                            This code is carefully tuned to work with large values (e.g. high ranges of U64) while preserving this property..                             */
                        val vNewOff_f = (vMax - vMin) * clickedT
                        val vNewOffFloor = vNewOff_f.L
                        val vNewOffRound = (vNewOff_f + 0.5f).L
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

    // Output grab position so it can be displayed by the caller
    var grabT = sliderCalcRatioFromValue(dataType, v, vMin, vMax, power, linearZeroPos)
    if (axis == Axis.Y)
        grabT = 1f - grabT
    val grabPos = lerp(sliderUsablePosMin, sliderUsablePosMax, grabT)
    if (axis == Axis.X)
        outGrabBb.put(grabPos - grabSz * 0.5f, bb.min.y + grabPadding, grabPos + grabSz * 0.5f, bb.max.y - grabPadding)
    else
        outGrabBb.put(bb.min.x + grabPadding, grabPos - grabSz * 0.5f, bb.max.x - grabPadding, grabPos + grabSz * 0.5f)

    return valueChanged
}

fun sliderBehaviorT(bb: Rect, id: Int, dataType: DataType, v: KMutableProperty0<*>, vMin: Double, vMax: Double, format: String,
                    power: Float, flags: SliderFlags = 0, outGrabBb: Rect): Boolean {

    v as KMutableProperty0<Double>

    val axis = if (flags has SliderFlag.Vertical) Axis.Y else Axis.X
    val isDecimal = dataType == DataType.Float || dataType == DataType.Double
    val isPower = power != 0f && isDecimal

    val grabPadding = 2f
    val sliderSz = (bb.max[axis.i] - bb.min[axis.i]) - grabPadding * 2f
    var grabSz = style.grabMinSize
    val vRange = if (vMin < vMax) vMax - vMin else vMin - vMax
    if (!isDecimal && vRange >= 0)  // vRange < 0 may happen on integer overflows
        grabSz = max(sliderSz / (vRange + 1).f, style.grabMinSize)  // For integer sliders: if possible have the grab size represent 1 unit
    grabSz = grabSz min sliderSz
    val sliderUsableSz = sliderSz - grabSz
    val sliderUsablePosMin = bb.min[axis.i] + grabPadding + grabSz * 0.5f
    val sliderUsablePosMax = bb.max[axis.i] - grabPadding - grabSz * 0.5f

    // For power curve sliders that cross over sign boundary we want the curve to be symmetric around 0f
    val linearZeroPos = when {   // 0.0->1.0f
        vMin * vMax < 0f -> {
            // Different sign
            val linearDistMinTo0 = glm.pow(if (vMin >= 0) vMin.f else -vMin.f, 1f / power)
            val linearDistMaxTo0 = glm.pow(if (vMax >= 0) vMax.f else -vMax.f, 1f / power)
            linearDistMinTo0 / (linearDistMinTo0 + linearDistMaxTo0)
        } // Same sign
        else -> if (vMin < 0f) 1f else 0f
    }

    // Process interacting with the slider
    var valueChanged = false
    if (g.activeId == id) {

        var setNewValue = false
        var clickedT = 0f

        if (g.activeIdSource == InputSource.Mouse) {
            if (!io.mouseDown[0]) clearActiveId()
            else {
                val mouseAbsPos = io.mousePos[axis.i]
                clickedT = if (sliderUsableSz > 0f) glm.clamp((mouseAbsPos - sliderUsablePosMin) / sliderUsableSz, 0f, 1f) else 0f
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
                clickedT = sliderCalcRatioFromValue(dataType, v(), vMin, vMax, power, linearZeroPos)
                val decimalPrecision = if (isDecimal) parseFormatPrecision(format, 3) else 0
                delta = when {
                    decimalPrecision > 0 || isPower -> when { // Gamepad/keyboard tweak speeds in % of slider bounds
                        NavInput.TweakSlow.isDown() -> delta / 1_000f
                        else -> delta / 100f
                    }
                    else -> when {
                        (vRange >= -100f && vRange <= 100f) || NavInput.TweakSlow.isDown() ->
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
                        lerp(glm.min(vMax, 0.0), vMin, a)
                    } else {
                        // Positive: rescale to the positive range before powering
                        var a = when {
                            glm.abs(linearZeroPos - 1f) > 1e-6f -> (clickedT - linearZeroPos) / (1f - linearZeroPos)
                            else -> clickedT
                        }
                        a = glm.pow(a, power)
                        lerp(glm.max(vMin, 0.0), vMax, a)
                    }
                }
                else -> when {// Linear slider
                    isDecimal -> lerp(vMin, vMax, clickedT)
                    else -> {
                        /*  For integer values we want the clicking position to match the grab box so we round above
                            This code is carefully tuned to work with large values (e.g. high ranges of U64) while preserving this property..                             */
                        val vNewOff_f = (vMax - vMin) * clickedT
                        val vNewOffFloor = vNewOff_f.L
                        val vNewOffRound = (vNewOff_f + 0.5f).L
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
            if (v() != vNew) {
                v.set(vNew)
                valueChanged = true
            }
        }
    }

    // Output grab position so it can be displayed by the caller
    var grabT = sliderCalcRatioFromValue(dataType, v(), vMin, vMax, power, linearZeroPos)
    if (axis == Axis.Y)
        grabT = 1f - grabT
    val grabPos = lerp(sliderUsablePosMin, sliderUsablePosMax, grabT)
    if (axis == Axis.X)
        outGrabBb.put(grabPos - grabSz * 0.5f, bb.min.y + grabPadding, grabPos + grabSz * 0.5f, bb.max.y - grabPadding)
    else
        outGrabBb.put(bb.min.x + grabPadding, grabPos - grabSz * 0.5f, bb.max.x - grabPadding, grabPos + grabSz * 0.5f)

    return valueChanged
}

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
}

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
        is Float -> formattedValue.parseFloat as N
        is Double -> formattedValue.parseDouble as N
        else -> throw Error("not supported")
    }
}