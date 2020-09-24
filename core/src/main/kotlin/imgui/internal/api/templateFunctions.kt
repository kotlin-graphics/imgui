@file:Suppress("UNCHECKED_CAST")

package imgui.internal.api

import glm_.*
import imgui.*
import imgui.ImGui.clearActiveID
import imgui.ImGui.getNavInputAmount2d
import imgui.ImGui.io
import imgui.ImGui.isMousePosValid
import imgui.ImGui.parseFormatFindStart2
import imgui.ImGui.parseFormatPrecision
import imgui.ImGui.style
import imgui.api.g
import imgui.internal.classes.Rect
import imgui.internal.lerp
import imgui.internal.saturate
import imgui.internal.sections.*
import unsigned.Uint
import unsigned.Ulong
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.reflect.KMutableProperty0

/** Template functions are instantiated in imgui_widgets.cpp for a finite number of types.
 *  To use them externally (for custom widget) you may need an "extern template" statement in your code in order to link to existing instances and silence Clang warnings (see #2036).
 *  e.g. " extern template IMGUI_API float RoundScalarWithFormatT<float, float>(const char* format, ImGuiDataType data_type, float v); " */
internal interface templateFunctions {

    /*
        [JVM] I first tried with generics, but it's quite hard and error prone.
        Therefore I manually implemented all the required templates.
        `DataType` is actually superfluous, but I keep it anyway to reduce distance with the native imgui
     */

    /** This is called by DragBehavior() when the widget is active (held by mouse or being manipulated with Nav controls)
     *  template<TYPE = Int, SIGNEDTYPE = Int, FLOATTYPE = Float> */
    fun dragBehaviorT(dataType: DataType, v: KMutableProperty0<Int>, vSpeed_: Float, vMin: Int, vMax: Int,
                      format: String, power: Float, flags: DragFlags): Boolean {

        val axis = if (flags has DragFlag.Vertical) Axis.Y else Axis.X
        val isDecimal = dataType == DataType.Float || dataType == DataType.Double
        val isClamped = vMin < vMax
        val isPower = power != 1f && isDecimal && isClamped && vMax - vMin < Float.MAX_VALUE
        val isLocked = vMin > vMax
        if (isLocked)
            return false

        // Default tweak speed
        var vSpeed = when {
            vSpeed_ == 0f && isClamped && vMax - vMin < Float.MAX_VALUE -> (vMax - vMin) * g.dragSpeedDefaultRatio
            else -> vSpeed_
        }

        // Inputs accumulates into g.DragCurrentAccum, which is flushed into the current value as soon as it makes a difference with our precision settings
        var adjustDelta = 0f
        if (g.activeIdSource == InputSource.Mouse && isMousePosValid() && io.mouseDragMaxDistanceSqr[0] > 1f * 1f) {
            adjustDelta = io.mouseDelta[axis]
            if (io.keyAlt)
                adjustDelta *= 1f / 100f
            if (io.keyShift)
                adjustDelta *= 10f
        } else if (g.activeIdSource == InputSource.Nav) {
            val decimalPrecision = if (isDecimal) parseFormatPrecision(format, 3) else 0
            adjustDelta = getNavInputAmount2d(NavDirSourceFlag.Keyboard or NavDirSourceFlag.PadDPad, InputReadMode.RepeatFast, 1f / 10f, 10f)[axis]
            vSpeed = vSpeed max getMinimumStepAtDecimalPrecision(decimalPrecision)
        }
        adjustDelta *= vSpeed

        // For vertical drag we currently assume that Up=higher value (like we do with vertical sliders). This may become a parameter.
        if (axis == Axis.Y)
            adjustDelta = -adjustDelta

        // Clear current value on activation
        // Avoid altering values and clamping when we are _already_ past the limits and heading in the same direction, so e.g. if range is 0..255, current value is 300 and we are pushing to the right side, keep the 300.
        val isJustActivated = g.activeIdIsJustActivated
        val isAlreadyPastLimitsAndPushingOutward = isClamped && ((v() >= vMax && adjustDelta > 0f) || (v() <= vMin && adjustDelta < 0f))
        val isDragDirectionChangeWithPower = isPower && ((adjustDelta < 0 && g.dragCurrentAccum > 0) || (adjustDelta > 0 && g.dragCurrentAccum < 0))
        if (isJustActivated || isAlreadyPastLimitsAndPushingOutward || isDragDirectionChangeWithPower) {
            g.dragCurrentAccum = 0f
            g.dragCurrentAccumDirty = false
        } else if (adjustDelta != 0.0f) {
            g.dragCurrentAccum += adjustDelta
            g.dragCurrentAccumDirty = true
        }

        if (!g.dragCurrentAccumDirty)
            return false

        var vCur = v()
        var vOldRefForAccumRemainder = 0f

        if (isPower) {
            // Offset + round to user desired precision, with a curve on the v_min..v_max range to get more precision on one side of the range
            val vOldNormCurved = ((vCur - vMin).f / (vMax - vMin).f).pow(1f / power)
            val vNewNormCurved = vOldNormCurved + g.dragCurrentAccum / (vMax - vMin)
            vCur = vMin + (saturate(vNewNormCurved).pow(power)).i * (vMax - vMin)
            vOldRefForAccumRemainder = vOldNormCurved
        } else
            vCur += g.dragCurrentAccum.i

        // Round to user desired precision based on format string
        vCur = roundScalarWithFormatT(format, dataType, vCur)

        // Preserve remainder after rounding has been applied. This also allow slow tweaking of values.
        g.dragCurrentAccumDirty = false
        g.dragCurrentAccum -= when {
            isPower -> {
                val vCurNormCurved = ((vCur - vMin).f / (vMax - vMin).f).pow(1f / power)
                vCurNormCurved - vOldRefForAccumRemainder
            }
            else -> (vCur - v()).f
        }

        // Lose zero sign for float/double
        if (vCur == -0)
            vCur = 0

        // Clamp values (+ handle overflow/wrap-around for integer types)
        if (v() != vCur && isClamped) {
            if (vCur < vMin || (vCur > v() && adjustDelta < 0f && !isDecimal))
                vCur = vMin
            if (vCur > vMax || (vCur < v() && adjustDelta > 0f && !isDecimal))
                vCur = vMax
        }

        // Apply result
        if (v() == vCur)
            return false
        v(vCur)
        return true
    }

    /** This is called by DragBehavior() when the widget is active (held by mouse or being manipulated with Nav controls)
     *  template<TYPE = Uint, SIGNEDTYPE = Int, FLOATTYPE = Float> */
    fun dragBehaviorT(dataType: DataType, v: KMutableProperty0<Uint>, vSpeed_: Float, vMin: Uint, vMax: Uint,
                      format: String, power: Float, flags: DragFlags): Boolean {

        val axis = if (flags has DragFlag.Vertical) Axis.Y else Axis.X
        val isDecimal = dataType == DataType.Float || dataType == DataType.Double
        val isClamped = vMin < vMax
        val isPower = power != 1f && isDecimal && isClamped && vMax - vMin < Float.MAX_VALUE
        val isLocked = vMin > vMax
        if (isLocked)
            return false

        // Default tweak speed
        var vSpeed = when {
            vSpeed_ == 0f && isClamped && (vMax - vMin < Float.MAX_VALUE) -> (vMax - vMin).f * g.dragSpeedDefaultRatio
            else -> vSpeed_
        }

        // Inputs accumulates into g.DragCurrentAccum, which is flushed into the current value as soon as it makes a difference with our precision settings
        var adjustDelta = 0f
        if (g.activeIdSource == InputSource.Mouse && isMousePosValid() && io.mouseDragMaxDistanceSqr[0] > 1f * 1f) {
            adjustDelta = io.mouseDelta[axis]
            if (io.keyAlt)
                adjustDelta *= 1f / 100f
            if (io.keyShift)
                adjustDelta *= 10f
        } else if (g.activeIdSource == InputSource.Nav) {
            val decimalPrecision = if (isDecimal) parseFormatPrecision(format, 3) else 0
            adjustDelta = getNavInputAmount2d(NavDirSourceFlag.Keyboard or NavDirSourceFlag.PadDPad, InputReadMode.RepeatFast, 1f / 10f, 10f)[axis]
            vSpeed = vSpeed max getMinimumStepAtDecimalPrecision(decimalPrecision)
        }
        adjustDelta *= vSpeed

        // For vertical drag we currently assume that Up=higher value (like we do with vertical sliders). This may become a parameter.
        if (axis == Axis.Y)
            adjustDelta = -adjustDelta

        // Clear current value on activation
        // Avoid altering values and clamping when we are _already_ past the limits and heading in the same direction, so e.g. if range is 0..255, current value is 300 and we are pushing to the right side, keep the 300.
        val isJustActivated = g.activeIdIsJustActivated
        val isAlreadyPastLimitsAndPushingOutward = isClamped && ((v() >= vMax && adjustDelta > 0f) || (v() <= vMin && adjustDelta < 0f))
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

        var vCur = v()
        var vOldRefForAccumRemainder = 0f

        if (isPower) {
            // Offset + round to user desired precision, with a curve on the v_min..v_max range to get more precision on one side of the range
            val vOldNormCurved = ((vCur - vMin).f / (vMax - vMin).f).pow(1f / power)
            val vNewNormCurved = vOldNormCurved + g.dragCurrentAccum / (vMax - vMin).f
            vCur = vMin + saturate(vNewNormCurved).pow(power).ui * (vMax - vMin)
            vOldRefForAccumRemainder = vOldNormCurved
        } else
            vCur += g.dragCurrentAccum

        // Round to user desired precision based on format string
        vCur = roundScalarWithFormatT(format, dataType, vCur)

        // Preserve remainder after rounding has been applied. This also allow slow tweaking of values.
        g.dragCurrentAccumDirty = false
        g.dragCurrentAccum -= when {
            isPower -> {
                val vCurNormCurved = ((vCur - vMin).f / (vMax - vMin).f).pow(1f / power)
                (vCurNormCurved - vOldRefForAccumRemainder).f
            }
            else -> (vCur.v - v().v).f
        }

        // Lose zero sign for float/double
        if (vCur.v == -0)
            vCur.v = 0

        // Clamp values (+ handle overflow/wrap-around for integer types)
        if (v() != vCur && isClamped) {
            if (vCur < vMin || (vCur > v() && adjustDelta < 0f && !isDecimal))
                vCur = vMin
            if (vCur > vMax || (vCur < v() && adjustDelta > 0f && !isDecimal))
                vCur = vMax
        }

        // Apply result
        if (v() == vCur)
            return false
        v(vCur)
        return true
    }

    /** This is called by DragBehavior() when the widget is active (held by mouse or being manipulated with Nav controls)
     *  template<TYPE = Long, SIGNEDTYPE = Long, FLOATTYPE = Double> */
    fun dragBehaviorT(dataType: DataType, v: KMutableProperty0<Long>, vSpeed_: Float, vMin: Long, vMax: Long,
                      format: String, power: Float, flags: DragFlags): Boolean {

        val axis = if (flags has DragFlag.Vertical) Axis.Y else Axis.X
        val isDecimal = dataType == DataType.Float || dataType == DataType.Double
        val isClamped = vMin < vMax
        val isPower = power != 1f && isDecimal && isClamped && vMax - vMin < Float.MAX_VALUE
        val isLocked = vMin > vMax
        if (isLocked)
            return false

        // Default tweak speed
        var vSpeed = when {
            vSpeed_ == 0f && isClamped && vMax - vMin < Float.MAX_VALUE -> ((vMax - vMin) * g.dragSpeedDefaultRatio).f
            else -> vSpeed_
        }

        // Inputs accumulates into g.DragCurrentAccum, which is flushed into the current value as soon as it makes a difference with our precision settings
        var adjustDelta = 0f
        if (g.activeIdSource == InputSource.Mouse && isMousePosValid() && io.mouseDragMaxDistanceSqr[0] > 1f * 1f) {
            adjustDelta = io.mouseDelta[axis]
            if (io.keyAlt)
                adjustDelta *= 1f / 100f
            if (io.keyShift)
                adjustDelta *= 10f
        } else if (g.activeIdSource == InputSource.Nav) {
            val decimalPrecision = if (isDecimal) parseFormatPrecision(format, 3) else 0
            adjustDelta = getNavInputAmount2d(NavDirSourceFlag.Keyboard or NavDirSourceFlag.PadDPad, InputReadMode.RepeatFast, 1f / 10f, 10f)[axis]
            vSpeed = vSpeed max getMinimumStepAtDecimalPrecision(decimalPrecision)
        }
        adjustDelta *= vSpeed

        // For vertical drag we currently assume that Up=higher value (like we do with vertical sliders). This may become a parameter.
        if (axis == Axis.Y)
            adjustDelta = -adjustDelta

        // Clear current value on activation
        // Avoid altering values and clamping when we are _already_ past the limits and heading in the same direction, so e.g. if range is 0..255, current value is 300 and we are pushing to the right side, keep the 300.
        val isJustActivated = g.activeIdIsJustActivated
        val isAlreadyPastLimitsAndPushingOutward = isClamped && ((v() >= vMax && adjustDelta > 0f) || (v() <= vMin && adjustDelta < 0f))
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

        var vCur = v()
        var vOldRefForAccumRemainder = 0.0

        if (isPower) {
            // Offset + round to user desired precision, with a curve on the v_min..v_max range to get more precision on one side of the range
            val vOldNormCurved = ((vCur - vMin).d / (vMax - vMin).d).pow(1.0 / power)
            val vNewNormCurved = vOldNormCurved + g.dragCurrentAccum / (vMax - vMin)
            vCur = vMin + (saturate(vNewNormCurved.f).pow(power)).L * (vMax - vMin)
            vOldRefForAccumRemainder = vOldNormCurved
        } else
            vCur += g.dragCurrentAccum.L

        // Round to user desired precision based on format string
        vCur = roundScalarWithFormatT(format, dataType, vCur)

        // Preserve remainder after rounding has been applied. This also allow slow tweaking of values.
        g.dragCurrentAccumDirty = false
        g.dragCurrentAccum -= when {
            isPower -> {
                val vCurNormCurved = ((vCur - vMin).d / (vMax - vMin).d).pow(1.0 / power)
                (vCurNormCurved - vOldRefForAccumRemainder).f
            }
            else -> (vCur - v()).f
        }

        // Lose zero sign for float/double
        if (vCur == -0L)
            vCur = 0L

        // Clamp values (+ handle overflow/wrap-around for integer types)
        if (v() != vCur && isClamped) {
            if (vCur < vMin || (vCur > v() && adjustDelta < 0f && !isDecimal))
                vCur = vMin
            if (vCur > vMax || (vCur < v() && adjustDelta > 0f && !isDecimal))
                vCur = vMax
        }

        // Apply result
        if (v() == vCur)
            return false
        v(vCur)
        return true
    }

    /** This is called by DragBehavior() when the widget is active (held by mouse or being manipulated with Nav controls)
     *  template<TYPE = Ulong, typename Long, FLOATTYPE = Double> */
    fun dragBehaviorT(dataType: DataType, v: KMutableProperty0<Ulong>, vSpeed_: Float, vMin: Ulong, vMax: Ulong,
                      format: String, power: Float, flags: DragFlags): Boolean {

        val axis = if (flags has DragFlag.Vertical) Axis.Y else Axis.X
        val isDecimal = dataType == DataType.Float || dataType == DataType.Double
        val isClamped = vMin < vMax
        val isPower = power != 1f && isDecimal && isClamped && vMax - vMin < Float.MAX_VALUE
        val isLocked = vMin > vMax
        if (isLocked)
            return false

        // Default tweak speed
        var vSpeed = when {
            vSpeed_ == 0f && isClamped && vMax - vMin < Float.MAX_VALUE -> ((vMax - vMin).v * g.dragSpeedDefaultRatio).f
            else -> vSpeed_
        }

        // Inputs accumulates into g.DragCurrentAccum, which is flushed into the current value as soon as it makes a difference with our precision settings
        var adjustDelta = 0f
        if (g.activeIdSource == InputSource.Mouse && isMousePosValid() && io.mouseDragMaxDistanceSqr[0] > 1f * 1f) {
            adjustDelta = io.mouseDelta[axis]
            if (io.keyAlt)
                adjustDelta *= 1f / 100f
            if (io.keyShift)
                adjustDelta *= 10f
        } else if (g.activeIdSource == InputSource.Nav) {
            val decimalPrecision = if (isDecimal) parseFormatPrecision(format, 3) else 0
            adjustDelta = getNavInputAmount2d(NavDirSourceFlag.Keyboard or NavDirSourceFlag.PadDPad, InputReadMode.RepeatFast, 1f / 10f, 10f)[axis]
            vSpeed = vSpeed max getMinimumStepAtDecimalPrecision(decimalPrecision)
        }
        adjustDelta *= vSpeed

        // For vertical drag we currently assume that Up=higher value (like we do with vertical sliders). This may become a parameter.
        if (axis == Axis.Y)
            adjustDelta = -adjustDelta

        // Clear current value on activation
        // Avoid altering values and clamping when we are _already_ past the limits and heading in the same direction, so e.g. if range is 0..255, current value is 300 and we are pushing to the right side, keep the 300.
        val isJustActivated = g.activeIdIsJustActivated
        val isAlreadyPastLimitsAndPushingOutward = isClamped && ((v() >= vMax && adjustDelta > 0f) || (v() <= vMin && adjustDelta < 0f))
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

        var vCur = v()
        var vOldRefForAccumRemainder = 0.0

        if (isPower) {
            // Offset + round to user desired precision, with a curve on the v_min..v_max range to get more precision on one side of the range
            val vOldNormCurved = ((vCur - vMin).d / (vMax - vMin).d).pow(1.0 / power)
            val vNewNormCurved = vOldNormCurved + g.dragCurrentAccum / (vMax - vMin).v
            vCur = vMin + (saturate(vNewNormCurved.f).pow(power)).ul * (vMax - vMin)
            vOldRefForAccumRemainder = vOldNormCurved
        } else
            vCur += g.dragCurrentAccum.ul

        // Round to user desired precision based on format string
        vCur = roundScalarWithFormatT(format, dataType, vCur)

        // Preserve remainder after rounding has been applied. This also allow slow tweaking of values.
        g.dragCurrentAccumDirty = false
        g.dragCurrentAccum -= when {
            isPower -> {
                val vCurNormCurved = ((vCur - vMin).d / (vMax - vMin).d).pow(1.0 / power)
                (vCurNormCurved - vOldRefForAccumRemainder).f
            }
            else -> (vCur.v - v().v).f
        }

        // Lose zero sign for float/double
        if (vCur.v == -0L)
            vCur.v = 0L

        // Clamp values (+ handle overflow/wrap-around for integer types)
        if (v() != vCur && isClamped) {
            if (vCur < vMin || (vCur > v() && adjustDelta < 0f && !isDecimal))
                vCur = vMin
            if (vCur > vMax || (vCur < v() && adjustDelta > 0f && !isDecimal))
                vCur = vMax
        }

        // Apply result
        if (v() == vCur)
            return false
        v(vCur)
        return true
    }

    /** This is called by DragBehavior() when the widget is active (held by mouse or being manipulated with Nav controls)
     *  template<TYPE = Float, SIGNEDTYPE = Float, FLOATTYPE = Float> */
    fun dragBehaviorT(dataType: DataType, v: KMutableProperty0<Float>, vSpeed_: Float, vMin: Float, vMax: Float,
                      format: String, power: Float, flags: DragFlags): Boolean {

        val axis = if (flags has DragFlag.Vertical) Axis.Y else Axis.X
        val isDecimal = dataType == DataType.Float || dataType == DataType.Double
        val isClamped = vMin < vMax
        val isPower = power != 1f && isDecimal && isClamped && vMax - vMin < Float.MAX_VALUE
        val isLocked = (vMin > vMax)
        if (isLocked)
            return false

        // Default tweak speed
        var vSpeed = when {
            vSpeed_ == 0f && isClamped && vMax - vMin < Float.MAX_VALUE -> (vMax - vMin) * g.dragSpeedDefaultRatio
            else -> vSpeed_
        }

        // Inputs accumulates into g.DragCurrentAccum, which is flushed into the current value as soon as it makes a difference with our precision settings
        var adjustDelta = 0f
        if (g.activeIdSource == InputSource.Mouse && isMousePosValid() && io.mouseDragMaxDistanceSqr[0] > 1f * 1f) {
            adjustDelta = io.mouseDelta[axis]
            if (io.keyAlt)
                adjustDelta *= 1f / 100f
            if (io.keyShift)
                adjustDelta *= 10f
        } else if (g.activeIdSource == InputSource.Nav) {
            val decimalPrecision = if (isDecimal) parseFormatPrecision(format, 3) else 0
            adjustDelta = getNavInputAmount2d(NavDirSourceFlag.Keyboard or NavDirSourceFlag.PadDPad, InputReadMode.RepeatFast, 1f / 10f, 10f)[axis]
            vSpeed = vSpeed max getMinimumStepAtDecimalPrecision(decimalPrecision)
        }
        adjustDelta *= vSpeed

        // For vertical drag we currently assume that Up=higher value (like we do with vertical sliders). This may become a parameter.
        if (axis == Axis.Y)
            adjustDelta = -adjustDelta

        // Clear current value on activation
        // Avoid altering values and clamping when we are _already_ past the limits and heading in the same direction, so e.g. if range is 0..255, current value is 300 and we are pushing to the right side, keep the 300.
        val isJustActivated = g.activeIdIsJustActivated
        val isAlreadyPastLimitsAndPushingOutward = isClamped && ((v() >= vMax && adjustDelta > 0f) || (v() <= vMin && adjustDelta < 0f))
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

        var vCur = v()
        var vOldRefForAccumRemainder = 0f

        if (isPower) {
            // Offset + round to user desired precision, with a curve on the v_min..v_max range to get more precision on one side of the range
            val vOldNormCurved = ((vCur - vMin) / (vMax - vMin)).pow(1f / power)
            val vNewNormCurved = vOldNormCurved + g.dragCurrentAccum / (vMax - vMin)
            vCur = vMin + saturate(vNewNormCurved).pow(power) * (vMax - vMin)
            vOldRefForAccumRemainder = vOldNormCurved
        } else
            vCur += g.dragCurrentAccum

        // Round to user desired precision based on format string
        vCur = roundScalarWithFormatT(format, dataType, vCur)

        // Preserve remainder after rounding has been applied. This also allow slow tweaking of values.
        g.dragCurrentAccumDirty = false
        g.dragCurrentAccum -= when {
            isPower -> {
                val vCurNormCurved = ((vCur - vMin) / (vMax - vMin)).pow(1f / power)
                vCurNormCurved - vOldRefForAccumRemainder
            }
            else -> vCur - v()
        }

        // Lose zero sign for float/double
        if (vCur == -0f)
            vCur = 0f

        // Clamp values (+ handle overflow/wrap-around for integer types)
        if (v() != vCur && isClamped) {
            if (vCur < vMin || (vCur > v() && adjustDelta < 0f && !isDecimal))
                vCur = vMin
            if (vCur > vMax || (vCur < v() && adjustDelta > 0f && !isDecimal))
                vCur = vMax
        }

        // Apply result
        if (v() == vCur)
            return false
        v(vCur)
        return true
    }

    /** This is called by DragBehavior() when the widget is active (held by mouse or being manipulated with Nav controls)
     *  template<TYPE = Double, SIGNEDTYPE = Double, FLOATTYPE = Double> */
    fun dragBehaviorT(dataType: DataType, v: KMutableProperty0<Double>, vSpeed_: Float, vMin: Double, vMax: Double,
                      format: String, power: Float, flags: DragFlags): Boolean {

        val axis = if (flags has DragFlag.Vertical) Axis.Y else Axis.X
        val isDecimal = dataType == DataType.Float || dataType == DataType.Double
        val isClamped = vMin < vMax
        val isPower = power != 1f && isDecimal && isClamped && vMax - vMin < Float.MAX_VALUE
        val isLocked = vMin > vMax
        if (isLocked)
            return false

        // Default tweak speed
        var vSpeed = when {
            vSpeed_ == 0f && isClamped && vMax - vMin < Float.MAX_VALUE -> ((vMax - vMin) * g.dragSpeedDefaultRatio).f
            else -> vSpeed_
        }

        // Inputs accumulates into g.DragCurrentAccum, which is flushed into the current value as soon as it makes a difference with our precision settings
        var adjustDelta = 0f
        if (g.activeIdSource == InputSource.Mouse && isMousePosValid() && io.mouseDragMaxDistanceSqr[0] > 1f * 1f) {
            adjustDelta = io.mouseDelta[axis]
            if (io.keyAlt)
                adjustDelta *= 1f / 100f
            if (io.keyShift)
                adjustDelta *= 10f
        } else if (g.activeIdSource == InputSource.Nav) {
            val decimalPrecision = if (isDecimal) parseFormatPrecision(format, 3) else 0
            adjustDelta = getNavInputAmount2d(NavDirSourceFlag.Keyboard or NavDirSourceFlag.PadDPad, InputReadMode.RepeatFast, 1f / 10f, 10f)[axis]
            vSpeed = vSpeed max getMinimumStepAtDecimalPrecision(decimalPrecision)
        }
        adjustDelta *= vSpeed

        // For vertical drag we currently assume that Up=higher value (like we do with vertical sliders). This may become a parameter.
        if (axis == Axis.Y)
            adjustDelta = -adjustDelta

        // Clear current value on activation
        // Avoid altering values and clamping when we are _already_ past the limits and heading in the same direction, so e.g. if range is 0..255, current value is 300 and we are pushing to the right side, keep the 300.
        val isJustActivated = g.activeIdIsJustActivated
        val isAlreadyPastLimitsAndPushingOutward = isClamped && ((v() >= vMax && adjustDelta > 0f) || (v() <= vMin && adjustDelta < 0f))
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

        var vCur = v()
        var vOldRefForAccumRemainder = 0.0

        if (isPower) {
            // Offset + round to user desired precision, with a curve on the v_min..v_max range to get more precision on one side of the range
            val vOldNormCurved = ((vCur - vMin) / (vMax - vMin)).pow(1.0 / power)
            val vNewNormCurved = vOldNormCurved + g.dragCurrentAccum / (vMax - vMin)
            vCur = vMin + saturate(vNewNormCurved.f).pow(power) * (vMax - vMin)
            vOldRefForAccumRemainder = vOldNormCurved
        } else
            vCur += g.dragCurrentAccum.d

        // Round to user desired precision based on format string
        vCur = roundScalarWithFormatT(format, dataType, vCur)

        // Preserve remainder after rounding has been applied. This also allow slow tweaking of values.
        g.dragCurrentAccumDirty = false
        g.dragCurrentAccum -= when {
            isPower -> {
                val vCurNormCurved = ((vCur - vMin) / (vMax - vMin)).pow(1.0 / power)
                (vCurNormCurved - vOldRefForAccumRemainder).f
            }
            else -> (vCur - v()).f
        }

        // Lose zero sign for float/double
        if (vCur == -0.0)
            vCur = 0.0

        // Clamp values (+ handle overflow/wrap-around for integer types)
        if (v() != vCur && isClamped) {
            if (vCur < vMin || (vCur > v() && adjustDelta < 0f && !isDecimal))
                vCur = vMin
            if (vCur > vMax || (vCur < v() && adjustDelta > 0f && !isDecimal))
                vCur = vMax
        }

        // Apply result
        if (v() == vCur)
            return false
        v(vCur)
        return true
    }

    /** FIXME: Move some of the code into SliderBehavior(). Current responsibility is larger than what the equivalent DragBehaviorT<> does, we also do some rendering, etc.
     *  template<TYPE = Int, SIGNEDTYPE = Int, FLOATTYPE = Float> */
    fun sliderBehaviorT(bb: Rect, id: ID, dataType: DataType, v: KMutableProperty0<Int>, vMin: Int, vMax: Int,
                        format: String, power: Float, flags: SliderFlags, outGrabBb: Rect): Boolean {

        val axis = if (flags has SliderFlag.Vertical) Axis.Y else Axis.X
        val isDecimal = dataType == DataType.Float || dataType == DataType.Double
        val isLogarithmic = power == 0f && isDecimal
        val isPower = power != 1f && isDecimal && !isLogarithmic

        val grabPadding = 2f
        val sliderSz = (bb.max[axis] - bb.min[axis]) - grabPadding * 2f
        var grabSz = style.grabMinSize
        val vRange = if (vMin < vMax) vMax - vMin else vMin - vMax
        if (!isDecimal && vRange >= 0)                                  // v_range < 0 may happen on integer overflows
            grabSz = (sliderSz / (vRange + 1)).f max style.grabMinSize // For integer sliders: if possible have the grab size represent 1 unit
        grabSz = grabSz min sliderSz
        val sliderUsableSz = sliderSz - grabSz
        val sliderUsablePosMin = bb.min[axis] + grabPadding + grabSz * 0.5f
        val sliderUsablePosMax = bb.max[axis] - grabPadding - grabSz * 0.5f

        // For power curve sliders that cross over sign boundary we want the curve to be symmetric around 0.0f
        val linearZeroPos = when {   // 0.0->1.0f
            // Different sign
            isPower && vMin * vMax < 0f -> {
                val linearDistMinTo0 = (if (vMin >= 0) vMin.f else -vMin.f).pow(1f / power)
                val linearDistMaxTo0 = (if (vMax >= 0) vMax.f else -vMax.f).pow(1f / power)
                (linearDistMinTo0 / (linearDistMinTo0 + linearDistMaxTo0)).f
            }
            // Same sign
            else -> if (vMin < 0f) 1f else 0f
        }

        var logarithmicZeroEpsilon = 0f // Only valid when is_logarithmic is true
        if (isLogarithmic) {
            // When using logarithmic sliders, we need to clamp to avoid hitting zero, but our choice of clamp value greatly affects slider precision. We attempt to use the specified precision to estimate a good lower bound.
            val decimalPrecision = if (isDecimal) parseFormatPrecision(format, 3) else 1
            logarithmicZeroEpsilon = 0.1f pow decimalPrecision.f
        }

        // Process interacting with the slider
        var valueChanged = false
        if (g.activeId == id) {
            var setNewValue = false
            var clickedT = 0f
            if (g.activeIdSource == InputSource.Mouse)
                if (!io.mouseDown[0])
                    clearActiveID()
                else {
                    val mouseAbsPos = io.mousePos[axis]
                    clickedT = when {
                        sliderUsableSz > 0f -> clamp((mouseAbsPos - sliderUsablePosMin) / sliderUsableSz, 0f, 1f)
                        else -> 0f
                    }
                    if (axis == Axis.Y)
                        clickedT = 1f - clickedT
                    setNewValue = true
                }
            else if (g.activeIdSource == InputSource.Nav) {
                val delta2 = getNavInputAmount2d(NavDirSourceFlag.Keyboard or NavDirSourceFlag.PadDPad, InputReadMode.RepeatFast, 0f, 0f)
                var delta = if (axis == Axis.X) delta2.x else -delta2.y
                if (g.navActivatePressedId == id && !g.activeIdIsJustActivated)
                    clearActiveID()
                else if (delta != 0f) {
                    clickedT = sliderCalcRatioFromValueT(dataType, v(), vMin, vMax, power, linearZeroPos, logarithmicZeroEpsilon)
                    val decimalPrecision = if (isDecimal) parseFormatPrecision(format, 3) else 0
                    if (decimalPrecision > 0 || isPower) {
                        delta /= 100f    // Gamepad/keyboard tweak speeds in % of slider bounds
                        if (NavInput.TweakSlow.isDown())
                            delta /= 10f
                    } else if ((vRange >= -100f && vRange <= 100f) || NavInput.TweakSlow.isDown())
                        delta = (if (delta < 0f) -1f else +1f) / vRange.f // Gamepad/keyboard tweak speeds in integer steps
                    else
                        delta /= 100f
                    if (NavInput.TweakFast.isDown())
                        delta *= 10f
                    setNewValue = true
                    if ((clickedT >= 1f && delta > 0f) || (clickedT <= 0f && delta < 0f)) // This is to avoid applying the saturation when already past the limits
                        setNewValue = false
                    else
                        clickedT = saturate(clickedT + delta)
                }
            }

            if (setNewValue) {
                var vNew = when {
                    isLogarithmic -> when {
                        // We special-case the extents because otherwise our fudging can lead to "mathematically correct" but non-intuitive behaviors like a fully-left slider not actually reaching the minimum value
                        clickedT <= 0f -> vMin
                        clickedT >= 1f -> vMax
                        else -> {
                            val flipped = vMax < vMin

                            // Fudge min/max to avoid getting silly results close to zero
                            var vMinFudged = when {
                                abs(vMin.f) < logarithmicZeroEpsilon -> if(vMin < 0f) -logarithmicZeroEpsilon else logarithmicZeroEpsilon
                                    else -> vMin.f
                            }
                            var vMaxFudged = when {
                                abs(vMax.f) < logarithmicZeroEpsilon -> if(vMax < 0f) -logarithmicZeroEpsilon else logarithmicZeroEpsilon
                                    else -> vMax.f
                            }

                            // Awkward special cases - we need ranges of the form (-100 .. 0) to convert to (-100 .. -epsilon), not (-100 .. epsilon)
                            if (vMin == 0 && vMax < 0f)
                                vMinFudged = -logarithmicZeroEpsilon
                            else if (vMax == 0 && vMin < 0f)
                                vMaxFudged = -logarithmicZeroEpsilon

                            if (flipped) {
                                val t = vMinFudged
                                vMinFudged = vMaxFudged
                                vMaxFudged = t
                            }

                            val clickedTWithFlip = if(flipped) 1f - clickedT else clickedT

                            when {
                                vMin * vMax < 0f -> { // Range crosses zero, so we have to do this in two parts
                                    val zeroPoint = -(vMin min vMax).f / abs(vMax.f - vMin.f) // The zero point in parametric space
                                    when {
                                        // Special case to make getting exactly zero possible (the epsilon prevents it otherwise)
                                        clickedTWithFlip == zeroPoint -> 0
                                        clickedTWithFlip < zeroPoint -> {
                                            val pow = (-vMinFudged / logarithmicZeroEpsilon).pow(1f - (clickedTWithFlip / zeroPoint))
                                            (-(logarithmicZeroEpsilon * pow)).i
                                        }
                                        else -> {
                                            val pow = (vMaxFudged / logarithmicZeroEpsilon).pow((clickedTWithFlip - zeroPoint) / (1f - zeroPoint))
                                            (logarithmicZeroEpsilon * pow).i
                                        }
                                    }
                                }
                                // Entirely negative slider
                                vMin < 0f || vMax < 0f -> {
                                    (TYPE)-(-vMaxFudged * ImPow(-vMinFudged / -vMaxFudged, (FLOATTYPE)(1.0f - clickedTWithFlip)))
                                }
                                else
                                v_new = (TYPE)(vMinFudged * ImPow(vMaxFudged / vMinFudged, (FLOATTYPE)clicked_t_with_flip))
                            }
                        }
                    }
                if (isPower)
                // Account for power curve scale on both sides of the zero
                    if (clickedT < linearZeroPos) {
                        // Negative: rescale to the negative range before powering
                        var a = 1f - (clickedT / linearZeroPos)
                        a = a pow power
                        vNew = lerp(vMax min 0, vMin, a)
                    } else {
                        // Positive: rescale to the positive range before powering
                        var a = when {
                            abs(linearZeroPos - 1f) > 1e-6f -> (clickedT - linearZeroPos) / (1f - linearZeroPos)
                            else -> clickedT
                        }
                        a = a pow power
                        vNew = lerp(vMin max 0, vMax, a)
                    }
                // Linear slider
                else if (isDecimal)
                    vNew = lerp(vMin, vMax, clickedT)
                else {
                    // For integer values we want the clicking position to match the grab box so we round above
                    // This code is carefully tuned to work with large values (e.g. high ranges of U64) while preserving this property..
                    val vNewOffF = (vMax - vMin) * clickedT
                    val vNewOffFloor = vNewOffF.i
                    val vNewOffRound = (vNewOffF + 0.5f).i
                    vNew = vMin + if (vNewOffFloor < vNewOffRound) vNewOffRound else vNewOffFloor
                }
                }
                // Round to user desired precision based on format string
                vNew = roundScalarWithFormatT(format, dataType, vNew)

                // Apply result
                if (v() != vNew) {
                    v(vNew)
                    valueChanged = true
                }
            }
        }

        if (sliderSz < 1f)
            outGrabBb.put(bb.min, bb.min)
        else {
            // Output grab position so it can be displayed by the caller
            var grabT = sliderCalcRatioFromValueT(dataType, v(), vMin, vMax, power, linearZeroPos)
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

    /** FIXME: Move some of the code into SliderBehavior(). Current responsibility is larger than what the equivalent DragBehaviorT<> does, we also do some rendering, etc.
     *  template<TYPE = Uint, SIGNEDTYPE = Int, FLOATTYPE = Float> */
    fun sliderBehaviorT(bb: Rect, id: ID, dataType: DataType, v: KMutableProperty0<Uint>, vMin: Uint, vMax: Uint,
                        format: String, power: Float, flags: SliderFlags, outGrabBb: Rect): Boolean {

        val axis = if (flags has SliderFlag.Vertical) Axis.Y else Axis.X
        val isDecimal = dataType == DataType.Float || dataType == DataType.Double
        val isLogarithmic = power == 0f && isDecimal
        val isPower = power != 1f && isDecimal && !isLogarithmic

        val grabPadding = 2f
        val sliderSz = (bb.max[axis] - bb.min[axis]) - grabPadding * 2f
        var grabSz = style.grabMinSize
        val vRange = (if (vMin < vMax) vMax - vMin else vMin - vMax).v
        if (!isDecimal && vRange >= 0)                                             // v_range < 0 may happen on integer overflows
            grabSz = (sliderSz / (vRange + 1)).f max style.grabMinSize  // For integer sliders: if possible have the grab size represent 1 unit
        grabSz = grabSz min sliderSz
        val sliderUsableSz = sliderSz - grabSz
        val sliderUsablePosMin = bb.min[axis] + grabPadding + grabSz * 0.5f
        val sliderUsablePosMax = bb.max[axis] - grabPadding - grabSz * 0.5f

        // For power curve sliders that cross over sign boundary we want the curve to be symmetric around 0.0f
        val linearZeroPos = when {   // 0.0->1.0f
            // Different sign
            isPower && vMin * vMax < 0f -> {
                val linearDistMinTo0 = (if (vMin >= 0) vMin.f else -vMin.f).pow(1f / power)
                val linearDistMaxTo0 = (if (vMax >= 0) vMax.f else -vMax.f).pow(1f / power)
                linearDistMinTo0 / (linearDistMinTo0 + linearDistMaxTo0)
            }
            // Same sign
            else -> if (vMin < 0f) 1f else 0f
        }
        // Process interacting with the slider
        var valueChanged = false
        if (g.activeId == id) {
            var setNewValue = false
            var clickedT = 0f
            if (g.activeIdSource == InputSource.Mouse)
                if (!io.mouseDown[0])
                    clearActiveID()
                else {
                    val mouseAbsPos = io.mousePos[axis]
                    clickedT = if (sliderUsableSz > 0f) clamp((mouseAbsPos - sliderUsablePosMin) / sliderUsableSz, 0f, 1f) else 0f
                    if (axis == Axis.Y)
                        clickedT = 1f - clickedT
                    setNewValue = true
                }
            else if (g.activeIdSource == InputSource.Nav) {
                val delta2 = getNavInputAmount2d(NavDirSourceFlag.Keyboard or NavDirSourceFlag.PadDPad, InputReadMode.RepeatFast, 0f, 0f)
                var delta = if (axis == Axis.X) delta2.x else -delta2.y
                if (g.navActivatePressedId == id && !g.activeIdIsJustActivated)
                    clearActiveID()
                else if (delta != 0f) {
                    clickedT = sliderCalcRatioFromValueT(dataType, v(), vMin, vMax, power, linearZeroPos)
                    val decimalPrecision = if (isDecimal) parseFormatPrecision(format, 3) else 0
                    if (decimalPrecision > 0 || isPower) {
                        delta /= 100f    // Gamepad/keyboard tweak speeds in % of slider bounds
                        if (NavInput.TweakSlow.isDown())
                            delta /= 10f
                    } else if ((vRange >= -100f && vRange <= 100f) || NavInput.TweakSlow.isDown())
                        delta = (if (delta < 0f) -1f else +1f) / vRange.f // Gamepad/keyboard tweak speeds in integer steps
                    else
                        delta /= 100f
                    if (NavInput.TweakFast.isDown())
                        delta *= 10f
                    setNewValue = true
                    if ((clickedT >= 1f && delta > 0f) || (clickedT <= 0f && delta < 0f)) // This is to avoid applying the saturation when already past the limits
                        setNewValue = false
                    else
                        clickedT = saturate(clickedT + delta)
                }
            }

            if (setNewValue) {
                var vNew: Uint
                if (isPower)
                // Account for power curve scale on both sides of the zero
                    if (clickedT < linearZeroPos) {
                        // Negative: rescale to the negative range before powering
                        var a = 1f - (clickedT / linearZeroPos)
                        a = a pow power
                        vNew = lerp(min(vMax, Uint(0)), vMin, a)
                    } else {
                        // Positive: rescale to the positive range before powering
                        var a = when {
                            abs(linearZeroPos - 1f) > 1e-6f -> (clickedT - linearZeroPos) / (1f - linearZeroPos)
                            else -> clickedT
                        }
                        a = a pow power
                        vNew = lerp(max(vMin, Uint(0)), vMax, a)
                    }
                // Linear slider
                else if (isDecimal)
                    vNew = lerp(vMin, vMax, clickedT)
                else {
                    // For integer values we want the clicking position to match the grab box so we round above
                    // This code is carefully tuned to work with large values (e.g. high ranges of U64) while preserving this property..
                    val vNewOffF = (vMax - vMin).f * clickedT
                    val vNewOffFloor = Uint(vNewOffF)
                    val vNewOffRound = Uint(vNewOffF + 0.5f)
                    vNew = vMin + if (vNewOffFloor < vNewOffRound) vNewOffRound else vNewOffFloor
                }

                // Round to user desired precision based on format string
                vNew = roundScalarWithFormatT(format, dataType, vNew)

                // Apply result
                if (v() != vNew) {
                    v(vNew)
                    valueChanged = true
                }
            }
        }

        if (sliderSz < 1f) {
            outGrabBb.put(bb.min, bb.min)
        } else {
            // Output grab position so it can be displayed by the caller
            var grabT = sliderCalcRatioFromValueT(dataType, v(), vMin, vMax, power, linearZeroPos)
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

    /** FIXME: Move some of the code into SliderBehavior(). Current responsibility is larger than what the equivalent DragBehaviorT<> does, we also do some rendering, etc.
     *  template<TYPE = Long, SIGNEDTYPE = Long, FLOATTYPE = Double> */
    fun sliderBehaviorT(bb: Rect, id: ID, dataType: DataType, v: KMutableProperty0<Long>, vMin: Long, vMax: Long,
                        format: String, power: Float, flags: SliderFlags, outGrabBb: Rect): Boolean {

        val axis = if (flags has SliderFlag.Vertical) Axis.Y else Axis.X
        val isDecimal = dataType == DataType.Float || dataType == DataType.Double
        val isLogarithmic = power == 0f && isDecimal
        val isPower = power != 1f && isDecimal && !isLogarithmic

        val grabPadding = 2f
        val sliderSz = (bb.max[axis] - bb.min[axis]) - grabPadding * 2f
        var grabSz = style.grabMinSize
        val vRange = if (vMin < vMax) vMax - vMin else vMin - vMax
        if (!isDecimal && vRange >= 0)                                             // v_range < 0 may happen on integer overflows
            grabSz = (sliderSz / (vRange + 1)).f max style.grabMinSize  // For integer sliders: if possible have the grab size represent 1 unit
        grabSz = grabSz min sliderSz
        val sliderUsableSz = sliderSz - grabSz
        val sliderUsablePosMin = bb.min[axis] + grabPadding + grabSz * 0.5f
        val sliderUsablePosMax = bb.max[axis] - grabPadding - grabSz * 0.5f

        // For power curve sliders that cross over sign boundary we want the curve to be symmetric around 0.0f
        val linearZeroPos = when {   // 0.0->1.0f
            // Different sign
            isPower && vMin * vMax < 0f -> {
                val linearDistMinTo0 = (if (vMin >= 0) vMin.d else -vMin.d).pow(1.0 / power)
                val linearDistMaxTo0 = (if (vMax >= 0) vMax.d else -vMax.d).pow(1.0 / power)
                (linearDistMinTo0 / (linearDistMinTo0 + linearDistMaxTo0)).f
            }
            // Same sign
            else -> if (vMin < 0f) 1f else 0f
        }

        // Process interacting with the slider
        var valueChanged = false
        if (g.activeId == id) {
            var setNewValue = false
            var clickedT = 0f
            if (g.activeIdSource == InputSource.Mouse)
                if (!io.mouseDown[0])
                    clearActiveID()
                else {
                    val mouseAbsPos = io.mousePos[axis]
                    clickedT = if (sliderUsableSz > 0f) clamp((mouseAbsPos - sliderUsablePosMin) / sliderUsableSz, 0f, 1f) else 0f
                    if (axis == Axis.Y)
                        clickedT = 1f - clickedT
                    setNewValue = true
                }
            else if (g.activeIdSource == InputSource.Nav) {
                val delta2 = getNavInputAmount2d(NavDirSourceFlag.Keyboard or NavDirSourceFlag.PadDPad, InputReadMode.RepeatFast, 0f, 0f)
                var delta = if (axis == Axis.X) delta2.x else -delta2.y
                if (g.navActivatePressedId == id && !g.activeIdIsJustActivated)
                    clearActiveID()
                else if (delta != 0f) {
                    clickedT = sliderCalcRatioFromValueT(dataType, v(), vMin, vMax, power, linearZeroPos)
                    val decimalPrecision = if (isDecimal) parseFormatPrecision(format, 3) else 0
                    if (decimalPrecision > 0 || isPower) {
                        delta /= 100f    // Gamepad/keyboard tweak speeds in % of slider bounds
                        if (NavInput.TweakSlow.isDown())
                            delta /= 10f
                    } else if ((vRange >= -100f && vRange <= 100f) || NavInput.TweakSlow.isDown())
                        delta = (if (delta < 0f) -1f else +1f) / vRange.f // Gamepad/keyboard tweak speeds in integer steps
                    else
                        delta /= 100f
                    if (NavInput.TweakFast.isDown())
                        delta *= 10f
                    setNewValue = true
                    if ((clickedT >= 1f && delta > 0f) || (clickedT <= 0f && delta < 0f)) // This is to avoid applying the saturation when already past the limits
                        setNewValue = false
                    else
                        clickedT = saturate(clickedT + delta)
                }
            }

            if (setNewValue) {
                var vNew: Long
                if (isPower)
                // Account for power curve scale on both sides of the zero
                    if (clickedT < linearZeroPos) {
                        // Negative: rescale to the negative range before powering
                        var a = 1f - (clickedT / linearZeroPos)
                        a = a pow power
                        vNew = lerp(min(vMax, 0L), vMin, a)
                    } else {
                        // Positive: rescale to the positive range before powering
                        var a = when {
                            abs(linearZeroPos - 1f) > 1e-6f -> (clickedT - linearZeroPos) / (1f - linearZeroPos)
                            else -> clickedT
                        }
                        a = a pow power
                        vNew = lerp(max(vMin, 0L), vMax, a)
                    }
                // Linear slider
                else if (isDecimal)
                    vNew = lerp(vMin, vMax, clickedT)
                else {
                    // For integer values we want the clicking position to match the grab box so we round above
                    // This code is carefully tuned to work with large values (e.g. high ranges of U64) while preserving this property..
                    val vNewOffF = (vMax - vMin).d * clickedT
                    val vNewOffFloor = vNewOffF.L
                    val vNewOffRound = (vNewOffF + 0.5).L
                    vNew = vMin + if (vNewOffFloor < vNewOffRound) vNewOffRound else vNewOffFloor
                }

                // Round to user desired precision based on format string
                vNew = roundScalarWithFormatT(format, dataType, vNew)

                // Apply result
                if (v() != vNew) {
                    v(vNew)
                    valueChanged = true
                }
            }
        }

        if (sliderSz < 1f)
            outGrabBb.put(bb.min, bb.min)
        else {
            // Output grab position so it can be displayed by the caller
            var grabT = sliderCalcRatioFromValueT(dataType, v(), vMin, vMax, power, linearZeroPos)
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

    /** FIXME: Move some of the code into SliderBehavior(). Current responsibility is larger than what the equivalent DragBehaviorT<> does, we also do some rendering, etc.
     *  template<TYPE = Ulong, SIGNEDTYPE = Long, FLOATTYPE = Double> */
    fun sliderBehaviorT(bb: Rect, id: ID, dataType: DataType, v: KMutableProperty0<Ulong>, vMin: Ulong, vMax: Ulong,
                        format: String, power: Float, flags: SliderFlags, outGrabBb: Rect): Boolean {

        val axis = if (flags has SliderFlag.Vertical) Axis.Y else Axis.X
        val isDecimal = dataType == DataType.Float || dataType == DataType.Double
        val isLogarithmic = power == 0f && isDecimal
        val isPower = power != 1f && isDecimal && !isLogarithmic

        val grabPadding = 2f
        val sliderSz = (bb.max[axis] - bb.min[axis]) - grabPadding * 2f
        var grabSz = style.grabMinSize
        val vRange = (if (vMin < vMax) vMax - vMin else vMin - vMax).v
        if (!isDecimal && vRange >= 0)                                             // v_range < 0 may happen on integer overflows
            grabSz = (sliderSz / (vRange + 1)).f max style.grabMinSize  // For integer sliders: if possible have the grab size represent 1 unit
        grabSz = grabSz min sliderSz
        val sliderUsableSz = sliderSz - grabSz
        val sliderUsablePosMin = bb.min[axis] + grabPadding + grabSz * 0.5f
        val sliderUsablePosMax = bb.max[axis] - grabPadding - grabSz * 0.5f

        // For power curve sliders that cross over sign boundary we want the curve to be symmetric around 0.0f
        val linearZeroPos = when {   // 0.0->1.0f
            // Different sign
            isPower && vMin * vMax < 0f -> {
                val linearDistMinTo0 = (if (vMin >= 0) vMin.d else -vMin.d).pow(1.0 / power)
                val linearDistMaxTo0 = (if (vMax >= 0) vMax.d else -vMax.d).pow(1.0 / power)
                (linearDistMinTo0 / (linearDistMinTo0 + linearDistMaxTo0)).f
            }
            // Same sign
            else -> if (vMin < 0f) 1f else 0f
        }

        // Process interacting with the slider
        var valueChanged = false
        if (g.activeId == id) {
            var setNewValue = false
            var clickedT = 0f
            if (g.activeIdSource == InputSource.Mouse)
                if (!io.mouseDown[0])
                    clearActiveID()
                else {
                    val mouseAbsPos = io.mousePos[axis]
                    clickedT = if (sliderUsableSz > 0f) clamp((mouseAbsPos - sliderUsablePosMin) / sliderUsableSz, 0f, 1f) else 0f
                    if (axis == Axis.Y)
                        clickedT = 1f - clickedT
                    setNewValue = true
                }
            else if (g.activeIdSource == InputSource.Nav) {
                val delta2 = getNavInputAmount2d(NavDirSourceFlag.Keyboard or NavDirSourceFlag.PadDPad, InputReadMode.RepeatFast, 0f, 0f)
                var delta = if (axis == Axis.X) delta2.x else -delta2.y
                if (g.navActivatePressedId == id && !g.activeIdIsJustActivated)
                    clearActiveID()
                else if (delta != 0f) {
                    clickedT = sliderCalcRatioFromValueT(dataType, v(), vMin, vMax, power, linearZeroPos)
                    val decimalPrecision = if (isDecimal) parseFormatPrecision(format, 3) else 0
                    if (decimalPrecision > 0 || isPower) {
                        delta /= 100f    // Gamepad/keyboard tweak speeds in % of slider bounds
                        if (NavInput.TweakSlow.isDown())
                            delta /= 10f
                    } else if ((vRange >= -100f && vRange <= 100f) || NavInput.TweakSlow.isDown())
                        delta = (if (delta < 0f) -1f else +1f) / vRange.f // Gamepad/keyboard tweak speeds in integer steps
                    else
                        delta /= 100f
                    if (NavInput.TweakFast.isDown())
                        delta *= 10f
                    setNewValue = true
                    if ((clickedT >= 1f && delta > 0f) || (clickedT <= 0f && delta < 0f)) // This is to avoid applying the saturation when already past the limits
                        setNewValue = false
                    else
                        clickedT = saturate(clickedT + delta)
                }
            }

            if (setNewValue) {
                var vNew: Ulong
                if (isPower)
                // Account for power curve scale on both sides of the zero
                    if (clickedT < linearZeroPos) {
                        // Negative: rescale to the negative range before powering
                        var a = 1f - (clickedT / linearZeroPos)
                        a = a pow power
                        vNew = lerp(min(vMax, Ulong(0)), vMin, a)
                    } else {
                        // Positive: rescale to the positive range before powering
                        var a = when {
                            abs(linearZeroPos - 1f) > 1e-6f -> (clickedT - linearZeroPos) / (1f - linearZeroPos)
                            else -> clickedT
                        }
                        a = a pow power
                        vNew = lerp(max(vMin, Ulong(0)), vMax, a)
                    }
                // Linear slider
                else if (isDecimal)
                    vNew = lerp(vMin, vMax, clickedT)
                else {
                    // For integer values we want the clicking position to match the grab box so we round above
                    // This code is carefully tuned to work with large values (e.g. high ranges of U64) while preserving this property..
                    val vNewOffF = (vMax - vMin).d * clickedT
                    val vNewOffFloor = Ulong(vNewOffF)
                    val vNewOffRound = Ulong(vNewOffF + 0.5)
                    vNew = vMin + if (vNewOffFloor < vNewOffRound) vNewOffRound else vNewOffFloor
                }

                // Round to user desired precision based on format string
                vNew = roundScalarWithFormatT(format, dataType, vNew)

                // Apply result
                if (v() != vNew) {
                    v(vNew)
                    valueChanged = true
                }
            }
        }

        if (sliderSz < 1f) {
            outGrabBb.put(bb.min, bb.min)
        } else {
            // Output grab position so it can be displayed by the caller
            var grabT = sliderCalcRatioFromValueT(dataType, v(), vMin, vMax, power, linearZeroPos)
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

    /** FIXME: Move some of the code into SliderBehavior(). Current responsibility is larger than what the equivalent DragBehaviorT<> does, we also do some rendering, etc.
     *  template<TYPE = Float, SIGNEDTYPE = Float, FLOATTYPE = Float> */
    fun sliderBehaviorT(bb: Rect, id: ID, dataType: DataType, v: KMutableProperty0<Float>, vMin: Float, vMax: Float,
                        format: String, power: Float, flags: SliderFlags, outGrabBb: Rect): Boolean {

        val axis = if (flags has SliderFlag.Vertical) Axis.Y else Axis.X
        val isDecimal = dataType == DataType.Float || dataType == DataType.Double
        val isLogarithmic = power == 0f && isDecimal
        val isPower = power != 1f && isDecimal && !isLogarithmic

        val grabPadding = 2f
        val sliderSz = (bb.max[axis] - bb.min[axis]) - grabPadding * 2f
        var grabSz = style.grabMinSize
        val vRange = if (vMin < vMax) vMax - vMin else vMin - vMax
        if (!isDecimal && vRange >= 0)                                             // v_range < 0 may happen on integer overflows
            grabSz = (sliderSz / (vRange + 1)).f max style.grabMinSize  // For integer sliders: if possible have the grab size represent 1 unit
        grabSz = grabSz min sliderSz
        val sliderUsableSz = sliderSz - grabSz
        val sliderUsablePosMin = bb.min[axis] + grabPadding + grabSz * 0.5f
        val sliderUsablePosMax = bb.max[axis] - grabPadding - grabSz * 0.5f

        // For power curve sliders that cross over sign boundary we want the curve to be symmetric around 0.0f
        val linearZeroPos = when {   // 0.0->1.0f
            // Different sign
            isPower && vMin * vMax < 0f -> {
                val linearDistMinTo0 = (if (vMin >= 0) vMin.f else -vMin.f).pow(1f / power)
                val linearDistMaxTo0 = (if (vMax >= 0) vMax.f else -vMax.f).pow(1f / power)
                linearDistMinTo0 / (linearDistMinTo0 + linearDistMaxTo0)
            }
            // Same sign
            else -> if (vMin < 0f) 1f else 0f
        }

        // Process interacting with the slider
        var valueChanged = false
        if (g.activeId == id) {
            var setNewValue = false
            var clickedT = 0f
            if (g.activeIdSource == InputSource.Mouse)
                if (!io.mouseDown[0])
                    clearActiveID()
                else {
                    val mouseAbsPos = io.mousePos[axis]
                    clickedT = if (sliderUsableSz > 0f) clamp((mouseAbsPos - sliderUsablePosMin) / sliderUsableSz, 0f, 1f) else 0f
                    if (axis == Axis.Y)
                        clickedT = 1f - clickedT
                    setNewValue = true
                }
            else if (g.activeIdSource == InputSource.Nav) {
                val delta2 = getNavInputAmount2d(NavDirSourceFlag.Keyboard or NavDirSourceFlag.PadDPad, InputReadMode.RepeatFast, 0f, 0f)
                var delta = if (axis == Axis.X) delta2.x else -delta2.y
                if (g.navActivatePressedId == id && !g.activeIdIsJustActivated)
                    clearActiveID()
                else if (delta != 0f) {
                    clickedT = sliderCalcRatioFromValueT(dataType, v(), vMin, vMax, power, linearZeroPos)
                    val decimalPrecision = if (isDecimal) parseFormatPrecision(format, 3) else 0
                    if (decimalPrecision > 0 || isPower) {
                        delta /= 100f    // Gamepad/keyboard tweak speeds in % of slider bounds
                        if (NavInput.TweakSlow.isDown())
                            delta /= 10f
                    } else if ((vRange >= -100f && vRange <= 100f) || NavInput.TweakSlow.isDown())
                        delta = (if (delta < 0f) -1f else +1f) / vRange.f // Gamepad/keyboard tweak speeds in integer steps
                    else
                        delta /= 100f
                    if (NavInput.TweakFast.isDown())
                        delta *= 10f
                    setNewValue = true
                    if ((clickedT >= 1f && delta > 0f) || (clickedT <= 0f && delta < 0f)) // This is to avoid applying the saturation when already past the limits
                        setNewValue = false
                    else
                        clickedT = saturate(clickedT + delta)
                }
            }

            if (setNewValue) {
                var vNew: Float
                if (isPower)
                // Account for power curve scale on both sides of the zero
                    if (clickedT < linearZeroPos) {
                        // Negative: rescale to the negative range before powering
                        var a = 1f - (clickedT / linearZeroPos)
                        a = a pow power
                        vNew = lerp(min(vMax, 0f), vMin, a)
                    } else {
                        // Positive: rescale to the positive range before powering
                        var a = when {
                            abs(linearZeroPos - 1f) > 1e-6f -> (clickedT - linearZeroPos) / (1f - linearZeroPos)
                            else -> clickedT
                        }
                        a = a pow power
                        vNew = lerp(max(vMin, 0f), vMax, a)
                    }
                // Linear slider
                else if (isDecimal)
                    vNew = lerp(vMin, vMax, clickedT)
                else {
                    // For integer values we want the clicking position to match the grab box so we round above
                    // This code is carefully tuned to work with large values (e.g. high ranges of U64) while preserving this property..
                    val vNewOffF = (vMax - vMin) * clickedT
                    val vNewOffFloor = vNewOffF
                    val vNewOffRound = vNewOffF + 0.5f
                    vNew = vMin + if (vNewOffFloor < vNewOffRound) vNewOffRound else vNewOffFloor
                }

                // Round to user desired precision based on format string
                vNew = roundScalarWithFormatT(format, dataType, vNew)

                // Apply result
                if (v() != vNew) {
                    v(vNew)
                    valueChanged = true
                }
            }
        }

        if (sliderSz < 1f)
            outGrabBb.put(bb.min, bb.min)
        else {
            // Output grab position so it can be displayed by the caller
            var grabT = sliderCalcRatioFromValueT(dataType, v(), vMin, vMax, power, linearZeroPos)
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

    /** FIXME: Move some of the code into SliderBehavior(). Current responsibility is larger than what the equivalent DragBehaviorT<> does, we also do some rendering, etc.
     *  template<TYPE = Double, SIGNEDTYPE = Double, FLOATTYPE = Double> */
    fun sliderBehaviorT(bb: Rect, id: ID, dataType: DataType, v: KMutableProperty0<Double>, vMin: Double, vMax: Double,
                        format: String, power: Float, flags: SliderFlags, outGrabBb: Rect): Boolean {

        val axis = if (flags has SliderFlag.Vertical) Axis.Y else Axis.X
        val isDecimal = dataType == DataType.Float || dataType == DataType.Double
        val isLogarithmic = power == 0f && isDecimal
        val isPower = power != 1f && isDecimal && !isLogarithmic

        val grabPadding = 2f
        val sliderSz = (bb.max[axis] - bb.min[axis]) - grabPadding * 2f
        var grabSz = style.grabMinSize
        val vRange = if (vMin < vMax) vMax - vMin else vMin - vMax
        if (!isDecimal && vRange >= 0)                                             // v_range < 0 may happen on integer overflows
            grabSz = (sliderSz / (vRange + 1)).f max style.grabMinSize  // For integer sliders: if possible have the grab size represent 1 unit
        grabSz = grabSz min sliderSz
        val sliderUsableSz = sliderSz - grabSz
        val sliderUsablePosMin = bb.min[axis] + grabPadding + grabSz * 0.5f
        val sliderUsablePosMax = bb.max[axis] - grabPadding - grabSz * 0.5f

        // For power curve sliders that cross over sign boundary we want the curve to be symmetric around 0.0f
        val linearZeroPos = when {   // 0.0->1.0f
            // Different sign
            isPower && vMin * vMax < 0f -> {
                val linearDistMinTo0 = (if (vMin >= 0) vMin else -vMin).pow(1.0 / power)
                val linearDistMaxTo0 = (if (vMax >= 0) vMax else -vMax).pow(1.0 / power)
                (linearDistMinTo0 / (linearDistMinTo0 + linearDistMaxTo0)).f
            }
            // Same sign
            else -> if (vMin < 0f) 1f else 0f
        }

        // Process interacting with the slider
        var valueChanged = false
        if (g.activeId == id) {
            var setNewValue = false
            var clickedT = 0f
            if (g.activeIdSource == InputSource.Mouse)
                if (!io.mouseDown[0])
                    clearActiveID()
                else {
                    val mouseAbsPos = io.mousePos[axis]
                    clickedT = if (sliderUsableSz > 0f) clamp((mouseAbsPos - sliderUsablePosMin) / sliderUsableSz, 0f, 1f) else 0f
                    if (axis == Axis.Y)
                        clickedT = 1f - clickedT
                    setNewValue = true
                }
            else if (g.activeIdSource == InputSource.Nav) {
                val delta2 = getNavInputAmount2d(NavDirSourceFlag.Keyboard or NavDirSourceFlag.PadDPad, InputReadMode.RepeatFast, 0f, 0f)
                var delta = if (axis == Axis.X) delta2.x else -delta2.y
                if (g.navActivatePressedId == id && !g.activeIdIsJustActivated)
                    clearActiveID()
                else if (delta != 0f) {
                    clickedT = sliderCalcRatioFromValueT(dataType, v(), vMin, vMax, power, linearZeroPos)
                    val decimalPrecision = if (isDecimal) parseFormatPrecision(format, 3) else 0
                    if (decimalPrecision > 0 || isPower) {
                        delta /= 100f    // Gamepad/keyboard tweak speeds in % of slider bounds
                        if (NavInput.TweakSlow.isDown())
                            delta /= 10f
                    } else if ((vRange >= -100f && vRange <= 100f) || NavInput.TweakSlow.isDown())
                        delta = (if (delta < 0f) -1f else +1f) / vRange.f // Gamepad/keyboard tweak speeds in integer steps
                    else
                        delta /= 100f
                    if (NavInput.TweakFast.isDown())
                        delta *= 10f
                    setNewValue = true
                    if ((clickedT >= 1f && delta > 0f) || (clickedT <= 0f && delta < 0f)) // This is to avoid applying the saturation when already past the limits
                        setNewValue = false
                    else
                        clickedT = saturate(clickedT + delta)
                }
            }

            if (setNewValue) {
                var vNew: Double
                if (isPower)
                // Account for power curve scale on both sides of the zero
                    if (clickedT < linearZeroPos) {
                        // Negative: rescale to the negative range before powering
                        var a = 1f - (clickedT / linearZeroPos)
                        a = a pow power
                        vNew = lerp(min(vMax, 0.0), vMin, a)
                    } else {
                        // Positive: rescale to the positive range before powering
                        var a = when {
                            abs(linearZeroPos - 1f) > 1e-6f -> (clickedT - linearZeroPos) / (1f - linearZeroPos)
                            else -> clickedT
                        }
                        a = a pow power
                        vNew = lerp(max(vMin, 0.0), vMax, a)
                    }
                // Linear slider
                else if (isDecimal)
                    vNew = lerp(vMin, vMax, clickedT)
                else {
                    // For integer values we want the clicking position to match the grab box so we round above
                    // This code is carefully tuned to work with large values (e.g. high ranges of U64) while preserving this property..
                    val vNewOffF = (vMax - vMin) * clickedT
                    val vNewOffFloor = vNewOffF
                    val vNewOffRound = vNewOffF + 0.5
                    vNew = vMin + if (vNewOffFloor < vNewOffRound) vNewOffRound else vNewOffFloor
                }

                // Round to user desired precision based on format string
                vNew = roundScalarWithFormatT(format, dataType, vNew)

                // Apply result
                if (v() != vNew) {
                    v(vNew)
                    valueChanged = true
                }
            }
        }

        if (sliderSz < 1f)
            outGrabBb.put(bb.min, bb.min)
        else {
            // Output grab position so it can be displayed by the caller
            var grabT = sliderCalcRatioFromValueT(dataType, v(), vMin, vMax, power, linearZeroPos)
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

    /** Convert a value v in the output space of a slider into a parametric position on the slider itself
     *  template<TYPE = Int, FLOATTYPE = Float> */
    fun sliderCalcRatioFromValueT(dataType: DataType, v: Int, vMin_: Int, vMax_: Int, power: Float,
                                  linearZeroPos: Float, logarithmicZeroEpsilon: Float): Float {

        var vMin = vMin_
        var vMax = vMax_

        if (vMin == vMax)
            return 0f

        val isLogarithmic = power == 0f && (dataType == DataType.Float || dataType == DataType.Double)
        val isPower = power != 1f && (dataType == DataType.Float || dataType == DataType.Double) && !isLogarithmic
        val vClamped = if (vMin < vMax) clamp(v, vMin, vMax) else clamp(v, vMax, vMin)
        return when {
            isLogarithmic -> {
                val flipped = vMax < vMin

                if (flipped) { // Handle the case where the range is backwards
                    val t = vMin
                    vMin = vMax
                    vMax = t
                }

                // Fudge min/max to avoid getting close to log(0)
                var vMinFudged = when {
                    abs(vMin.f) < logarithmicZeroEpsilon -> if (vMin < 0f) -logarithmicZeroEpsilon else logarithmicZeroEpsilon
                    else -> vMin.f
                }
                var vMaxFudged = when {
                    abs(vMax.f) < logarithmicZeroEpsilon -> if (vMax < 0f) -logarithmicZeroEpsilon else logarithmicZeroEpsilon
                    else -> vMax.f
                }

                // Awkward special cases - we need ranges of the form (-100 .. 0) to convert to (-100 .. -epsilon), not (-100 .. epsilon)
                if (vMin == 0 && vMax < 0f)
                    vMinFudged = -logarithmicZeroEpsilon
                else if (vMax == 0 && vMin < 0f)
                    vMaxFudged = -logarithmicZeroEpsilon

                val result = when {
                    vClamped <= vMinFudged -> 0f // Workaround for values that are in-range but below our fudge
                    vClamped >= vMaxFudged -> 1f // Workaround for values that are in-range but above our fudge
                    (vMin * vMax) < 0f -> { // Range crosses zero, so split into two portions
                        val zeroPoint = -vMin.f / (vMax.f - vMin.f) // The zero point in parametric space.  There's an argument we should take the logarithmic nature into account when calculating this, but for now this should do (and the most common case of a symmetrical range works fine)
                        when {
                            v == 0 -> zeroPoint // Special case for exactly zero
                            v < 0f -> (1f - ln(-vClamped.f / logarithmicZeroEpsilon) / ln(-vMinFudged / logarithmicZeroEpsilon)) * zeroPoint
                            else -> zeroPoint + ln(vClamped.f / logarithmicZeroEpsilon) / ln(vMaxFudged / logarithmicZeroEpsilon) * (1f - zeroPoint)
                        }
                    }
                    // Entirely negative slider
                    (vMin < 0f) || (vMax < 0f) -> 1f - ln(-vClamped.f / -vMaxFudged) / ln(-vMinFudged / -vMaxFudged)
                    else -> ln(vClamped.f / vMinFudged) / ln(vMaxFudged / vMinFudged)
                }
                if (flipped) 1f - result else result
            }
            isPower -> when {
                vClamped < 0f -> {
                    val f = 1f - ((vClamped - vMin) / (min(0, vMax) - vMin)).f
                    1f - f.pow(1f / power) * linearZeroPos
                }
                else -> {
                    val f = ((vClamped - max(0, vMin)) / (vMax - max(0, vMin))).f
                    linearZeroPos + f.pow(1f / power) * (1f - linearZeroPos)
                }
            }
            // Linear slider
            else -> (vClamped - vMin).f / (vMax - vMin).f
        }
    }

    /** Convert a value v in the output space of a slider into a parametric position on the slider itself
     *  template<TYPE = Uint, FLOATTYPE = Float> */
    fun sliderCalcRatioFromValueT(dataType: DataType, v: Uint, vMin_: Uint, vMax_: Uint, power: Float,
                                  linearZeroPos: Float, logarithmicZeroEpsilon: Float): Float {

        var vMin = vMin_
        var vMax = vMax_

        if (vMin == vMax)
            return 0f

        val isLogarithmic = power == 0f && (dataType == DataType.Float || dataType == DataType.Double)
        val isPower = power != 1f && (dataType == DataType.Float || dataType == DataType.Double) && !isLogarithmic
        val vClamped = if (vMin < vMax) clamp(v, vMin, vMax) else clamp(v, vMax, vMin)
        return when {
            isLogarithmic -> {
                val flipped = vMax < vMin

                if (flipped) { // Handle the case where the range is backwards
                    val t = vMin
                    vMin = vMax
                    vMax = t
                }

                // Fudge min/max to avoid getting close to log(0)
                val vMinFudged = when {
                    abs(vMin.f) < logarithmicZeroEpsilon -> /*if (vMin < 0f) -logarithmicZeroEpsilon else */logarithmicZeroEpsilon
                    else -> vMin.f
                }
                val vMaxFudged = when {
                    abs(vMax.f) < logarithmicZeroEpsilon -> /*if (vMax < 0f) -logarithmicZeroEpsilon else */logarithmicZeroEpsilon
                    else -> vMax.f
                }

                // Awkward special cases - we need ranges of the form (-100 .. 0) to convert to (-100 .. -epsilon), not (-100 .. epsilon)
//                if (vMin.v == 0 && vMax < 0f)
//                    vMinFudged = -logarithmicZeroEpsilon
//                else if (vMax.v == 0 && vMin < 0f)
//                    vMaxFudged = -logarithmicZeroEpsilon

                val result = when {
                    vClamped <= vMinFudged -> 0f // Workaround for values that are in-range but below our fudge
                    vClamped >= vMaxFudged -> 1f // Workaround for values that are in-range but above our fudge
//                    (vMin * vMax) < 0f -> { // Range crosses zero, so split into two portions
//                        val zeroPoint = - vMin.f / ( vMax.f - vMin.f) // The zero point in parametric space.  There's an argument we should take the logarithmic nature into account when calculating this, but for now this should do (and the most common case of a symmetrical range works fine)
//                        when {
//                            v.v == 0 -> zeroPoint // Special case for exactly zero
//                            v < 0f -> (1f - ln(- vClamped.f / logarithmicZeroEpsilon) / ln(-vMinFudged / logarithmicZeroEpsilon)) * zeroPoint
//                            else -> zeroPoint + ln( vClamped.f / logarithmicZeroEpsilon) / ln(vMaxFudged / logarithmicZeroEpsilon) * (1f - zeroPoint)
//                        }
//                    }
                    // Entirely negative slider
//                    vMin < Uint(0) || vMax < Uint(0) -> 1f - (float)(ImLog(-(FLOATTYPE) v_clamped / - v_max_fudged) / ImLog(-vMinFudged / -vMaxFudged))
                    else -> ln(vClamped.f / vMinFudged) / ln(vMaxFudged / vMinFudged)
                }
                if (flipped) 1f - result else result
            }
            isPower -> when {
//                vClamped < 0f -> {
//                    val f = 1f - ((vClamped - vMin) / (min(Uint(0), vMax) - vMin)).f
//                    1f - f.pow(1f / power) * linearZeroPos
//                }
                else -> {
                    val f = ((vClamped - max(Uint(0), vMin)) / (vMax - max(Uint(0), vMin))).f
                    linearZeroPos + f.pow(1f / power) * (1f - linearZeroPos)
                }
            }
            // Linear slider
            else -> (vClamped - vMin).f / (vMax - vMin).f
        }
    }

    /** Convert a value v in the output space of a slider into a parametric position on the slider itself
     *  template<TYPE = Long, FLOATTYPE = Double> */
    fun sliderCalcRatioFromValueT(dataType: DataType, v: Long, vMin_: Long, vMax_: Long, power: Float,
                                  linearZeroPos: Float, logarithmicZeroEpsilon: Float): Float {

        var vMin = vMin_
        var vMax = vMax_

        if (vMin == vMax)
            return 0f

        val isLogarithmic = power == 0f && (dataType == DataType.Float || dataType == DataType.Double)
        val isPower = power != 1f && (dataType == DataType.Float || dataType == DataType.Double) && !isLogarithmic
        val vClamped = if (vMin < vMax) clamp(v, vMin, vMax) else clamp(v, vMax, vMin)
        return when {
            isLogarithmic -> {
                val flipped = vMax < vMin

                if (flipped) { // Handle the case where the range is backwards
                    val t = vMin
                    vMin = vMax
                    vMax = t
                }

                // Fudge min/max to avoid getting close to log(0)
                var vMinFudged = when {
                    abs(vMin.d) < logarithmicZeroEpsilon -> if (vMin < 0) -logarithmicZeroEpsilon else logarithmicZeroEpsilon
                    else -> vMin
                }.d
                var vMaxFudged = when {
                    abs(vMax.d) < logarithmicZeroEpsilon -> if (vMax < 0) -logarithmicZeroEpsilon else logarithmicZeroEpsilon
                    else -> vMax
                }.d

                // Awkward special cases - we need ranges of the form (-100 .. 0) to convert to (-100 .. -epsilon), not (-100 .. epsilon)
                if (vMin == 0L && vMax < 0L)
                    vMinFudged = -logarithmicZeroEpsilon.d
                else if (vMax == 0L && vMin < 0L)
                    vMaxFudged = -logarithmicZeroEpsilon.d

                val result = when {
                    vClamped <= vMinFudged -> 0f // Workaround for values that are in-range but below our fudge
                    vClamped >= vMaxFudged -> 1f // Workaround for values that are in-range but above our fudge
                    (vMin * vMax) < 0L -> { // Range crosses zero, so split into two portions
                        val zeroPoint = -vMin.f / (vMax.f - vMin.f) // The zero point in parametric space.  There's an argument we should take the logarithmic nature into account when calculating this, but for now this should do (and the most common case of a symmetrical range works fine)
                        when {
                            v == 0L -> zeroPoint // Special case for exactly zero
                            v < 0L -> (1f - (ln(-vClamped.f / logarithmicZeroEpsilon) / ln(-vMinFudged / logarithmicZeroEpsilon)).f) * zeroPoint
                            else -> zeroPoint + ((ln(vClamped.f / logarithmicZeroEpsilon) / ln(vMaxFudged / logarithmicZeroEpsilon)).f * (1f - zeroPoint))
                        }
                    }
                    // Entirely negative slider
                    (vMin < 0L || vMax < 0L) -> 1f - (ln(-vClamped.f / -vMaxFudged) / ln(-vMinFudged / -vMaxFudged)).f
                    else -> (ln(vClamped.f / vMinFudged) / ln(vMaxFudged / vMinFudged)).f
                }
                if (flipped) 1f - result else result
            }
            isPower -> when {
                vClamped < 0f -> {
                    val f = 1f - ((vClamped - vMin) / (min(0L, vMax) - vMin)).f
                    1f - f.pow(1f / power) * linearZeroPos
                }
                else -> {
                    val f = ((vClamped - max(0L, vMin)) / (vMax - max(0L, vMin))).f
                    linearZeroPos + f.pow(1f / power) * (1f - linearZeroPos)
                }
            }
            // Linear slider
            else -> ((vClamped - vMin).d / (vMax - vMin).d).f
        }
    }

    /** Convert a value v in the output space of a slider into a parametric position on the slider itself
     *  template<TYPE = Ulong, FLOATTYPE = Double> */
    fun sliderCalcRatioFromValueT(dataType: DataType, v: Ulong, vMin_: Ulong, vMax_: Ulong, power: Float,
                                  linearZeroPos: Float, logarithmicZeroEpsilon: Float): Float {

        var vMin = vMin_
        var vMax = vMax_
        if (vMin == vMax)
            return 0f

        val isLogarithmic = power == 0f && (dataType == DataType.Float || dataType == DataType.Double)
        val isPower = power != 1f && (dataType == DataType.Float || dataType == DataType.Double) && !isLogarithmic
        val vClamped = if (vMin < vMax) clamp(v, vMin, vMax) else clamp(v, vMax, vMin)
        return when {
            isLogarithmic -> {
                val flipped = vMax < vMin

                if (flipped) { // Handle the case where the range is backwards
                    val t = vMin
                    vMin = vMax
                    vMax = t
                }

                // Fudge min/max to avoid getting close to log(0)
                val vMinFudged = when {
                    abs(vMin.d) < logarithmicZeroEpsilon -> /*if(vMin < 0) -logarithmicZeroEpsilon else */logarithmicZeroEpsilon
                    else -> vMin
                }.d
                val vMaxFudged = when {
                    abs(vMax.d) < logarithmicZeroEpsilon -> /*if(vMax < 0) -logarithmicZeroEpsilon else */logarithmicZeroEpsilon
                    else -> vMax
                }.d

                // Awkward special cases - we need ranges of the form (-100 .. 0) to convert to (-100 .. -epsilon), not (-100 .. epsilon)
//                if (vMin.v == 0L && (v_max < 0.0f))
//                    vMinFudged = -logarithmic_zero_epsilon;
//                else if ((v_max == 0.0f) && (v_min < 0.0f))
//                    vMaxFudged = -logarithmic_zero_epsilon;

                val result = when {
                    vClamped <= vMinFudged.ul -> 0f // Workaround for values that are in-range but below our fudge
                    vClamped >= vMaxFudged.ul -> 1f // Workaround for values that are in-range but above our fudge
//                    else if ((v_min * v_max) < 0.0f) // Range crosses zero, so split into two portions
//                    {
//                        float zero_point =(-(float) v_min) / ((float) v_max -(float) v_min) // The zero point in parametric space.  There's an argument we should take the logarithmic nature into account when calculating this, but for now this should do (and the most common case of a symmetrical range works fine)
//                        if (v == 0.0f)
//                            result = zero_point // Special case for exactly zero
//                        else if (v < 0.0f)
//                            result = (1.0f - (float)(ImLog(-(FLOATTYPE) v_clamped / logarithmic_zero_epsilon) / ImLog(-vMinFudged / logarithmic_zero_epsilon))) * zero_point
//                        else
//                            result = zero_point + ((float)(ImLog((FLOATTYPE) v_clamped / logarithmic_zero_epsilon) / ImLog(vMaxFudged / logarithmic_zero_epsilon)) * (1.0f - zero_point))
//                    }
//                    else if ((v_min < 0.0f) || (v_max < 0.0f)) // Entirely negative slider
//                        result = 1.0f - (float)(ImLog(-(FLOATTYPE) v_clamped / - v_max_fudged) / ImLog(-vMinFudged / -vMaxFudged))
                    else -> (ln(vClamped.d / vMinFudged) / ln(vMaxFudged / vMinFudged)).f
                }
                if (flipped) 1f - result else result
            }
            isPower -> when {
                vClamped < 0f -> {
                    val f = 1f - ((vClamped - vMin) / (min(Ulong(0), vMax) - vMin)).f
                    1f - f.pow(1f / power) * linearZeroPos
                }
                else -> {
                    val f = ((vClamped - max(Ulong(0), vMin)) / (vMax - max(Ulong(0), vMin))).f
                    linearZeroPos + f.pow(1f / power) * (1f - linearZeroPos)
                }
            }
            // Linear slider
            else -> ((vClamped - vMin).d / (vMax - vMin).d).f
        }
    }

    /** Convert a value v in the output space of a slider into a parametric position on the slider itself
     *  template<TYPE = Float, FLOATTYPE = Float> */
    fun sliderCalcRatioFromValueT(dataType: DataType, v: Float, vMin_: Float, vMax_: Float, power: Float,
                                  linearZeroPos: Float, logarithmicZeroEpsilon: Float): Float {

        var vMin = vMin_
        var vMax = vMax_
        if (vMin == vMax)
            return 0f

        val isLogarithmic = power == 0f && (dataType == DataType.Float || dataType == DataType.Double)
        val isPower = power != 1f && (dataType == DataType.Float || dataType == DataType.Double) && !isLogarithmic
        val vClamped = if (vMin < vMax) clamp(v, vMin, vMax) else clamp(v, vMax, vMin)
        return when {
            isLogarithmic -> {
                val flipped = vMax < vMin

                if (flipped) { // Handle the case where the range is backwards
                    val t = vMin
                    vMin = vMax
                    vMax = t
                }

                // Fudge min/max to avoid getting close to log(0)
                var vMinFudged = when {
                    abs(vMin) < logarithmicZeroEpsilon -> if (vMin < 0f) -logarithmicZeroEpsilon else logarithmicZeroEpsilon
                    else -> vMin
                }
                var vMaxFudged = when {
                    abs(vMax) < logarithmicZeroEpsilon -> if (vMax < 0f) -logarithmicZeroEpsilon else logarithmicZeroEpsilon
                    else -> vMax
                }

                // Awkward special cases - we need ranges of the form (-100 .. 0) to convert to (-100 .. -epsilon), not (-100 .. epsilon)
                if (vMin == 0f && vMax < 0f)
                    vMinFudged = -logarithmicZeroEpsilon
                else if (vMax == 0f && vMin < 0f)
                    vMaxFudged = -logarithmicZeroEpsilon

                val result = when {
                    vClamped <= vMinFudged -> 0f // Workaround for values that are in-range but below our fudge
                    vClamped >= vMaxFudged -> 1f // Workaround for values that are in-range but above our fudge
                    (vMin * vMax) < 0f -> { // Range crosses zero, so split into two portions
                        val zeroPoint = -vMin / (vMax - vMin) // The zero point in parametric space.  There's an argument we should take the logarithmic nature into account when calculating this, but for now this should do (and the most common case of a symmetrical range works fine)
                        when {
                            v == 0f -> zeroPoint // Special case for exactly zero
                            v < 0f -> (1f - ln(-vClamped / logarithmicZeroEpsilon) / ln(-vMinFudged / logarithmicZeroEpsilon)) * zeroPoint
                            else -> zeroPoint + (ln(vClamped / logarithmicZeroEpsilon) / ln(vMaxFudged / logarithmicZeroEpsilon) * (1f - zeroPoint))
                        }
                    }
                    // Entirely negative slider
                    vMin < 0f || vMax < 0f -> 1f - ln(-vClamped / -vMaxFudged) / ln(-vMinFudged / -vMaxFudged)
                    else -> ln(vClamped / vMinFudged) / ln(vMaxFudged / vMinFudged)
                }
                if (flipped) 1f - result else result
            }
            isPower -> when {
                vClamped < 0f -> {
                    val f = 1f - (vClamped - vMin) / (min(0f, vMax) - vMin)
                    1f - f.pow(1f / power) * linearZeroPos
                }
                else -> {
                    val f = (vClamped - max(0f, vMin)) / (vMax - max(0f, vMin))
                    linearZeroPos + f.pow(1f / power) * (1f - linearZeroPos)
                }
            }
            // Linear slider
            else -> (vClamped - vMin) / (vMax - vMin)
        }
    }

    /** Convert a value v in the output space of a slider into a parametric position on the slider itself
     *  template<TYPE = Double, FLOATTYPE = Double> */
    fun sliderCalcRatioFromValueT(dataType: DataType, v: Double, vMin_: Double, vMax_: Double, power: Float,
                                  linearZeroPos: Float, logarithmicZeroEpsilon: Float): Float {

        var vMin = vMin_
        var vMax = vMax_
        if (vMin == vMax)
            return 0f

        val isLogarithmic = power == 0f && (dataType == DataType.Float || dataType == DataType.Double)
        val isPower = power != 1f && (dataType == DataType.Float || dataType == DataType.Double) && !isLogarithmic
        val vClamped = if (vMin < vMax) clamp(v, vMin, vMax) else clamp(v, vMax, vMin)
        return when {
            isLogarithmic -> {
                val flipped = vMax < vMin

                if (flipped) { // Handle the case where the range is backwards
                    val t = vMin
                    vMin = vMax
                    vMax = t
                }

                // Fudge min/max to avoid getting close to log(0)
                var vMinFudged = when {
                    abs(vMin) < logarithmicZeroEpsilon -> (if (vMin < 0f) -logarithmicZeroEpsilon else logarithmicZeroEpsilon).d
                    else -> vMin
                }
                var vMaxFudged = when {
                    abs(vMax) < logarithmicZeroEpsilon -> (if (vMax < 0f) -logarithmicZeroEpsilon else logarithmicZeroEpsilon).d
                    else -> vMax
                }

                // Awkward special cases - we need ranges of the form (-100 .. 0) to convert to (-100 .. -epsilon), not (-100 .. epsilon)
                if (vMin == 0.0 && vMax < 0f)
                    vMinFudged = -logarithmicZeroEpsilon.d
                else if (vMax == 0.0 && vMin < 0f)
                    vMaxFudged = -logarithmicZeroEpsilon.d

                val result = when {
                    vClamped <= vMinFudged -> 0f // Workaround for values that are in-range but below our fudge
                    vClamped >= vMaxFudged -> 1f // Workaround for values that are in-range but above our fudge
                    (vMin * vMax) < 0f -> { // Range crosses zero, so split into two portions
                        val zeroPoint = -vMin.f / (vMax.f - vMin.f) // The zero point in parametric space.  There's an argument we should take the logarithmic nature into account when calculating this, but for now this should do (and the most common case of a symmetrical range works fine)
                        when {
                            v == 0.0 -> zeroPoint // Special case for exactly zero
                            v < 0f -> (1f - (ln(-vClamped / logarithmicZeroEpsilon) / ln(-vMinFudged / logarithmicZeroEpsilon)).f) * zeroPoint
                            else -> zeroPoint + (ln(vClamped / logarithmicZeroEpsilon) / ln(vMaxFudged / logarithmicZeroEpsilon)).f * (1f - zeroPoint)
                        }
                    }
                    // Entirely negative slider
                    vMin < 0f || vMax < 0f -> 1f - (ln(-vClamped / -vMaxFudged) / ln(-vMinFudged / -vMaxFudged)).f
                    else -> (ln(vClamped / vMinFudged) / ln(vMaxFudged / vMinFudged)).f
                }
                if (flipped) 1f - result else result
            }
            isPower -> when {
                vClamped < 0f -> {
                    val f = 1f - ((vClamped - vMin) / (min(0.0, vMax) - vMin)).f
                    1f - f.pow(1f / power) * linearZeroPos
                }
                else -> {
                    val f = ((vClamped - max(0.0, vMin)) / (vMax - max(0.0, vMin))).f
                    linearZeroPos + f.pow(1f / power) * (1f - linearZeroPos)
                }
            }
            // Linear slider
            else -> ((vClamped - vMin).d / (vMax - vMin).d).f
        }
    }

    /** template<TYPE = Int, SIGNEDTYPE = Int> */
    fun roundScalarWithFormatT(fmt: String, dataType: DataType, v: Int): Int {
        val fmtStart = parseFormatFindStart2(fmt)
        if (fmt.getOrNul(fmtStart + 0) != '%' || fmt.getOrNul(fmtStart + 1) == '%') // Don't apply if the value is not visible in the format string
            return v
        val vStr = fmt.substring(fmtStart).format(v)
        return when (dataType) {
            DataType.Float, DataType.Double -> vStr.d.i
            else -> vStr.i
        }
    }

    /** template<TYPE = Uint, SIGNEDTYPE = Int> */
    fun roundScalarWithFormatT(fmt: String, dataType: DataType, v: Uint): Uint {
        val fmtStart = parseFormatFindStart2(fmt)
        if (fmt.getOrNul(fmtStart + 0) != '%' || fmt.getOrNul(fmtStart + 1) == '%') // Don't apply if the value is not visible in the format string
            return v
        val vStr = fmt.substring(fmtStart).format(v.v)
        return when (dataType) {
            DataType.Float, DataType.Double -> vStr.d.ui
            else -> vStr.ui
        }
    }

    /** template<TYPE = Long, SIGNEDTYPE = Long> */
    fun roundScalarWithFormatT(fmt: String, dataType: DataType, v: Long): Long {
        val fmtStart = parseFormatFindStart2(fmt)
        if (fmt.getOrNul(fmtStart + 0) != '%' || fmt.getOrNul(fmtStart + 1) == '%') // Don't apply if the value is not visible in the format string
            return v
        val vStr = fmt.substring(fmtStart).format(v)
        return when (dataType) {
            DataType.Float, DataType.Double -> vStr.d.L
            else -> vStr.L
        }
    }

    /** template<TYPE = Ulong, SIGNEDTYPE = Long> */
    fun roundScalarWithFormatT(fmt: String, dataType: DataType, v: Ulong): Ulong {
        val fmtStart = parseFormatFindStart2(fmt)
        if (fmt.getOrNul(fmtStart + 0) != '%' || fmt.getOrNul(fmtStart + 1) == '%') // Don't apply if the value is not visible in the format string
            return v
        val vStr = fmt.substring(fmtStart).format(v.v)
        return when (dataType) {
            DataType.Float, DataType.Double -> vStr.d.ul
            else -> vStr.ul
        }
    }

    /** template<TYPE = Float, SIGNEDTYPE = Float> */
    fun roundScalarWithFormatT(fmt: String, dataType: DataType, v: Float): Float {
        val fmtStart = parseFormatFindStart2(fmt)
        if (fmt.getOrNul(fmtStart + 0) != '%' || fmt.getOrNul(fmtStart + 1) == '%') // Don't apply if the value is not visible in the format string
            return v
        val vStr = fmt.substring(fmtStart).format(v)
        return when (dataType) {
            DataType.Float, DataType.Double -> vStr.d.f
            else -> vStr.f
        }
    }

    /** template<TYPE = Double, SIGNEDTYPE = Double> */
    fun roundScalarWithFormatT(fmt: String, dataType: DataType, v: Double): Double {
        val fmtStart = parseFormatFindStart2(fmt)
        if (fmt.getOrNul(fmtStart + 0) != '%' || fmt.getOrNul(fmtStart + 1) == '%') // Don't apply if the value is not visible in the format string
            return v
        val vStr = fmt.substring(fmtStart).format(v)
        return when (dataType) {
            DataType.Float, DataType.Double -> vStr.d
            else -> vStr.d
        }
    }

    companion object {

        val minSteps = floatArrayOf(1f, 0.1f, 0.01f, 0.001f, 0.0001f, 0.00001f, 0.000001f, 0.0000001f, 0.00000001f, 0.000000001f)

        fun getMinimumStepAtDecimalPrecision(decimalPrecision: Int): Float {
            return when {
                decimalPrecision < 0 -> Float.MIN_VALUE
                else -> minSteps.getOrElse(decimalPrecision) {
                    10f.pow(-decimalPrecision.f)
                }
            }
        }
    }
}
