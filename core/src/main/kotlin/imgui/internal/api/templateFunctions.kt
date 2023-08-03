@file:Suppress("NAME_SHADOWING")

package imgui.internal.api

import glm_.f
import glm_.max
import glm_.pow
import imgui.*
import imgui.ImGui.calcItemWidth
import imgui.ImGui.checkbox
import imgui.ImGui.clearActiveID
import imgui.ImGui.currentWindow
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.getNavTweakPressedAmount
import imgui.ImGui.io
import imgui.ImGui.isDown
import imgui.ImGui.isDragPastThreshold
import imgui.ImGui.isMousePosValid
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushMultiItemsWidths
import imgui.ImGui.sameLine
import imgui.ImGui.style
import imgui.ImGui.textEx
import imgui.api.g
import imgui.dsl.group
import imgui.dsl.withID
import imgui.internal.*
import imgui.internal.classes.Rect
import imgui.internal.sections.Axis
import imgui.internal.sections.InputSource
import imgui.internal.sections.ItemFlag
import imgui.internal.sections.get
import imgui.static.DRAG_MOUSE_THRESHOLD_FACTOR
import imgui.static.getMinimumStepAtDecimalPrecision
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KMutableProperty0

/** Template functions are instantiated in imgui_widgets.cpp for a finite number of types.
 *  To use them externally (for custom widget) you may need an "extern template" statement in your code in order to link to existing instances and silence Clang warnings (see #2036).
 *  e.g. " extern template IMGUI_API float RoundScalarWithFormatT<float, float>(const char* format, ImGuiDataType data_type, float v); " */
internal interface templateFunctions {

    /*
    // [JVM] Reworked to use generics and NumberOps instead of templates
     */

    /** Convert a value v in the output space of a slider into a parametric position on the slider itself
     *  (the logical opposite of scaleValueFromRatio)
     *  template<TYPE = N, SIGNEDTYPE = N, FLOATTYPE = FP> */
    fun <N, FP> NumberFpOps<N, FP>.scaleRatioFromValue(v: N, vMin: N, vMax: N, isLogarithmic: Boolean, logarithmicZeroEpsilon: Float, zeroDeadzoneHalfsize: Float): Float where N : Number, N : Comparable<N>, FP : Number, FP : Comparable<FP> {
        if (vMin == vMax) return 0f

        val vClamped = if (vMin < vMax) v.clamp(vMin, vMax) else v.clamp(vMax, vMin)
        val vClampedFp = vClamped.fp

        // Linear slider
        if (!isLogarithmic) return ((vClamped.fp - vMin.fp) / (vMax.fp - vMin.fp)).f
        val logarithmicZeroEpsilon = logarithmicZeroEpsilon.fp
        val zeroDeadzoneHalfsize = zeroDeadzoneHalfsize.fp

        // Very minor optimization: we calculate isSigned only once
        val isSigned = isSigned

        val flipped = vMax < vMin
        val vMin = if (flipped) vMax else vMin
        val vMax = if (flipped) vMin else vMax
        val vMinFp = vMin.fp
        val vMaxFp = vMax.fp

        // Fudge min/max to avoid getting close to log(0)
        val vMinFudged = when {
            // Awkward special case - we need ranges of the form (-100 .. 0) to convert to (-100 .. -epsilon), not (-100 .. epsilon)
            isSigned && vMax == zero && vMin.isNegative -> -logarithmicZeroEpsilon
            abs(vMinFp) < logarithmicZeroEpsilon -> if (isSigned && vMin.isNegative) -logarithmicZeroEpsilon else logarithmicZeroEpsilon
            else -> vMinFp
        }

        val vMaxFudged = when {
            abs(vMaxFp) < logarithmicZeroEpsilon -> if (isSigned && vMax.isNegative) -logarithmicZeroEpsilon else logarithmicZeroEpsilon
            else -> vMaxFp
        }

        // This can't happen? I'm pretty sure... because we flip the range if it's backwards, so we can't have a zero min and a negative max
        /*if (vMin == zero && vMax.isNegative)
            vMinFudged = -logarithmicZeroEpsilon
        else*/

        val result: FP = when {
            vClampedFp <= vMinFudged -> 0f.fp // Workaround for values that are in-range but below our fudge
            vClampedFp >= vMaxFudged -> 1f.fp // Workaround for values that are in-range but above our fudge
            isSigned && ((vMin.sign * vMax.sign) < 0) -> { // Range crosses zero, so split into two portions
                val zeroPointCenter = -vMinFp / (vMaxFp - vMinFp) // The zero point in parametric space.  There's an argument we should take the logarithmic nature into account when calculating this, but for now this should do (and the most common case of a symmetrical range works fine)
                val zeroPointSnapL = zeroPointCenter - zeroDeadzoneHalfsize
                val zeroPointSnapR = zeroPointCenter + zeroDeadzoneHalfsize
                when {
                    v == zero -> zeroPointCenter // Special case for exactly zero
                    v.isNegative -> (1f.fp - ln(-vClampedFp / logarithmicZeroEpsilon) / ln(-vMinFudged / logarithmicZeroEpsilon)) * zeroPointSnapL
                    else -> zeroPointSnapR + ln(vClampedFp / logarithmicZeroEpsilon) / ln(vMaxFudged / logarithmicZeroEpsilon) * (1f.fp - zeroPointSnapR)
                }
            }
            // Entirely negative slider
            isSigned && (vMin.isNegative) || (vMax.isNegative) -> 1f.fp - ln(-vClampedFp / -vMaxFudged) / ln(-vMinFudged / -vMaxFudged)
            else -> ln(vClampedFp / vMinFudged) / ln(vMaxFudged / vMinFudged)
        }
        return (if (flipped) 1f.fp - result else result).f
    }

    /** Convert a parametric position on a slider into a value v in the output space (the logical opposite of scaleRatioFromValue)
     *  template<TYPE = N, SIGNEDTYPE = N, FLOATTYPE = FP> */
    fun <N, FP> NumberFpOps<N, FP>.scaleValueFromRatio(t: Float, vMin: N, vMax: N, isLogarithmic: Boolean, logarithmicZeroEpsilon: Float, zeroDeadzoneHalfsize: Float): N where N : Number, N : Comparable<N>, FP : Number, FP : Comparable<FP> = when {
        // We special-case the extents because otherwise our logarithmic fudging can lead to "mathematically correct"
        // but non-intuitive behaviors like a fully-left slider not actually reaching the minimum value. Also generally simpler.
        t <= 0f || vMin == vMax -> vMin
        t >= 1f -> vMax
        isLogarithmic -> {
            val isSigned = isSigned
            val logarithmicZeroEpsilon = logarithmicZeroEpsilon.fp
            val zeroDeadzoneHalfsize = zeroDeadzoneHalfsize.fp
            val vMinFp = vMin.fp
            val vMaxFp = vMax.fp
            // Fudge min/max to avoid getting silly results close to zero
            var vMinFudged = when {
                abs(vMinFp) < logarithmicZeroEpsilon -> if (isSigned && vMin.isNegative) -logarithmicZeroEpsilon else logarithmicZeroEpsilon
                else -> vMinFp
            }
            var vMaxFudged = when {
                abs(vMaxFp) < logarithmicZeroEpsilon -> if (isSigned && vMax.isNegative) -logarithmicZeroEpsilon else logarithmicZeroEpsilon
                else -> vMaxFp
            }

            val flipped = vMax < vMin // Check if range is "backwards"
            if (flipped) {
                val swap = vMinFudged
                vMinFudged = vMaxFudged
                vMaxFudged = swap
            }

            // Awkward special case - we need ranges of the form (-100 .. 0) to convert to (-100 .. -epsilon), not (-100 .. epsilon)
            if (isSigned && vMax == zero && vMin.isNegative) vMaxFudged = -logarithmicZeroEpsilon

            // t, but flipped if necessary to account for us flipping the range
            val tWithFlip = (if (flipped) 1f - t else t).fp

            when {
                isSigned && ((vMin.sign * vMax.sign) < 0) -> { // Range crosses zero, so we have to do this in two parts
                    val zeroPointCenter = -(vMin min vMax).fp / abs(vMaxFp - vMinFp) // The zero point in parametric space
                    val zeroPointSnapL = zeroPointCenter - zeroDeadzoneHalfsize
                    val zeroPointSnapR = zeroPointCenter + zeroDeadzoneHalfsize
                    when {
                        tWithFlip in zeroPointSnapL..zeroPointSnapR -> zero // Special case to make getting exactly zero possible (the epsilon prevents it otherwise)
                        tWithFlip < zeroPointCenter -> (-(logarithmicZeroEpsilon * (-vMinFudged / logarithmicZeroEpsilon).pow(1f.fp - tWithFlip / zeroPointSnapL))).n
                        else -> (logarithmicZeroEpsilon * (vMaxFudged / logarithmicZeroEpsilon).pow((tWithFlip - zeroPointSnapR) / (1f.fp - zeroPointSnapR))).n
                    }
                }
                // Entirely negative slider
                isSigned && (vMin.isNegative || vMax.isNegative) -> (-(-vMaxFudged * (-vMinFudged / -vMaxFudged).pow(1f.fp - tWithFlip))).n
                else -> (vMinFudged * (vMaxFudged / vMinFudged).pow(tWithFlip)).n
            }
        }
        // Linear slider
        // ~isFloatingPoint
        isFloatingPoint -> lerp(vMin, vMax, t.fp).n
        // - For integer values we want the clicking position to match the grab box so we round above
        //   This code is carefully tuned to work with large values (e.g. high ranges of U64) while preserving this property..
        // - Not doing a *1.0 multiply at the end of a range as it tends to be lossy. While absolute aiming at a large s64/u64
        //   range is going to be imprecise anyway, with this check we at least make the edge values matches expected limits.
        t < 1f -> {
            val vNewOffF = (vMax.fp - vMin.fp) * t.fp
            (vMin.fp + vNewOffF + if (vMin > vMax) (-0.5f).fp else 0.5f.fp).n
        }

        else -> zero
    }

    /** This is called by DragBehavior() when the widget is active (held by mouse or being manipulated with Nav controls)
     *  template<TYPE = N, SIGNEDTYPE = N, FLOATTYPE = FP> */
    fun <N, FP> NumberFpOps<N, FP>.dragBehaviorT(v: KMutableProperty0<N>, vSpeed_: Float, vMin: N, vMax: N, format: String, flags: SliderFlags): Boolean where N : Number, N : Comparable<N>, FP : Number, FP : Comparable<FP> {

        val axis = if (flags has SliderFlag._Vertical) Axis.Y else Axis.X
        val isClamped = vMin < vMax
        val isLogarithmic = flags has SliderFlag.Logarithmic

        val vRange = vMax.fp - vMin.fp
        // Default tweak speed
        var vSpeed = when {
            vSpeed_ == 0f && isClamped && vRange < Float.MAX_VALUE.fp -> vRange * g.dragSpeedDefaultRatio.fp
            else -> vSpeed_.fp
        }

        // Inputs accumulates into g.DragCurrentAccum, which is flushed into the current value as soon as it makes a difference with our precision settings
        var adjustDelta = 0f.fp
        if (g.activeIdSource == InputSource.Mouse && isMousePosValid() && MouseButton.Left isDragPastThreshold (io.mouseDragThreshold * DRAG_MOUSE_THRESHOLD_FACTOR)) {
            adjustDelta = io.mouseDelta[axis].fp
            if (io.keyAlt) adjustDelta *= 1f.fp / 100f.fp
            if (io.keyShift) adjustDelta *= 10f.fp
        } else if (g.activeIdSource == InputSource.Keyboard || g.activeIdSource == InputSource.Gamepad) {
            val decimalPrecision = if (isFloatingPoint) parseFormatPrecision(format, 3) else 0
            val tweakSlow = (if (g.navInputSource == InputSource.Gamepad) Key._NavGamepadTweakSlow else Key._NavKeyboardTweakSlow).isDown
            val tweakFast = (if (g.navInputSource == InputSource.Gamepad) Key._NavGamepadTweakFast else Key._NavKeyboardTweakFast).isDown
            val tweakFactor = if (tweakSlow) 1f.fp else if (tweakFast) 10f.fp else 1f.fp
            adjustDelta = getNavTweakPressedAmount(axis).fp * tweakFactor
            vSpeed = vSpeed max getMinimumStepAtDecimalPrecision(decimalPrecision).fp
        }
        adjustDelta *= vSpeed

        // For vertical drag we currently assume that Up=higher value (like we do with vertical sliders). This may become a parameter.
        if (axis == Axis.Y) adjustDelta = -adjustDelta

        // For logarithmic use our range is effectively 0..1 so scale the delta into that range
        if (isLogarithmic && vRange < Float.MAX_VALUE.fp && vRange > 0.000001f.fp) // Epsilon to avoid /0
            adjustDelta /= vRange

        // Clear current value on activation
        // Avoid altering values and clamping when we are _already_ past the limits and heading in the same direction, so e.g. if range is 0..255, current value is 300 and we are pushing to the right side, keep the 300.
        val isJustActivated = g.activeIdIsJustActivated
        val isAlreadyPastLimitsAndPushingOutward = isClamped && ((v() >= vMax && adjustDelta > 0f.fp) || (v() <= vMin && adjustDelta < 0f.fp))
        if (isJustActivated || isAlreadyPastLimitsAndPushingOutward) {
            g.dragCurrentAccum = 0f
            g.dragCurrentAccumDirty = false
        } else if (adjustDelta != 0f.fp) {
            g.dragCurrentAccum += adjustDelta.f
            g.dragCurrentAccumDirty = true
        }

        if (!g.dragCurrentAccumDirty) return false

        var vCur = v()
        var vOldRefForAccumRemainder = 0f

        var logarithmicZeroEpsilon = 0f // Only valid when is_logarithmic is true
        val zeroDeadzoneHalfsize = 0f // Drag widgets have no deadzone (as it doesn't make sense)
        if (isLogarithmic) {
            // When using logarithmic sliders, we need to clamp to avoid hitting zero, but our choice of clamp value greatly affects slider precision. We attempt to use the specified precision to estimate a good lower bound.
            val decimalPrecision = if (isFloatingPoint) parseFormatPrecision(format, 3) else 1
            logarithmicZeroEpsilon = 0.1f pow decimalPrecision.f

            // Convert to parametric space, apply delta, convert back
            val vOldParametric = scaleRatioFromValue(vCur, vMin, vMax, isLogarithmic, logarithmicZeroEpsilon, zeroDeadzoneHalfsize)
            val vNewParametric = vOldParametric + g.dragCurrentAccum
            vCur = scaleValueFromRatio(vNewParametric, vMin, vMax, isLogarithmic, logarithmicZeroEpsilon, zeroDeadzoneHalfsize)
            vOldRefForAccumRemainder = vOldParametric
        } else {
            val dragAccum = g.dragCurrentAccum.fp
            if (dragAccum < 0f.fp) vCur -= (-dragAccum).n
            else vCur += dragAccum.n

        }

        // Round to user desired precision based on format string
        if (isFloatingPoint && flags hasnt SliderFlag.NoRoundToFormat) vCur = roundScalarWithFormat(format, vCur)

        // Preserve remainder after rounding has been applied. This also allow slow tweaking of values.
        g.dragCurrentAccumDirty = false
        g.dragCurrentAccum -= when {
            isLogarithmic -> {
                // Convert to parametric space, apply delta, convert back
                val vNewParametric = scaleRatioFromValue(vCur, vMin, vMax, isLogarithmic, logarithmicZeroEpsilon, zeroDeadzoneHalfsize)
                (vNewParametric - vOldRefForAccumRemainder).f
            }

            else -> (vCur.fp - v().fp).f
        }

        // Lose zero sign for float/double
        if (vCur.fp == (-0f).fp) vCur = zero

        // Clamp values (+ handle overflow/wrap-around for integer types)
        if (v() != vCur && isClamped) {
            if (vCur < vMin || (vCur > v() && adjustDelta < 0f.fp && !isFloatingPoint)) vCur = vMin
            if (vCur > vMax || (vCur < v() && adjustDelta > 0f.fp && !isFloatingPoint)) vCur = vMax
        }

        // Apply result
        if (v() == vCur) return false
        v(vCur)
        return true
    }

    /** Try to move more of the code into shared SliderBehavior()
     *  template<TYPE = Int, SIGNEDTYPE = Int, FLOATTYPE = Float> */
    fun <N, FP> NumberFpOps<N, FP>.sliderBehaviorT(bb: Rect, id: ID, v: KMutableProperty0<N>, vMin: N, vMax: N, format: String, flags: SliderFlags, outGrabBb: Rect): Boolean where N : Number, N : Comparable<N>, FP : Number, FP : Comparable<FP> {

        val axis = if (flags has SliderFlag._Vertical) Axis.Y else Axis.X
        val isLogarithmic = flags has SliderFlag.Logarithmic
        val isFloatingPoint = dataType == DataType.Float || dataType == DataType.Double
        val vRange = if (vMin < vMax) vMax - vMin else vMin - vMax

        // Calculate bounds
        val grabPadding = 2f // FIXME: Should be part of style.
        val sliderSz = (bb.max[axis] - bb.min[axis]) - grabPadding * 2f
        var grabSz = style.grabMinSize
        if (!isFloatingPoint && vRange >= zero)                                  // v_range < 0 may happen on integer overflows
            grabSz = (sliderSz.fp / (vRange + one).fp).f max style.grabMinSize // For integer sliders: if possible have the grab size represent 1 unit
        grabSz = grabSz min sliderSz
        val sliderUsableSz = sliderSz - grabSz
        val sliderUsablePosMin = bb.min[axis] + grabPadding + grabSz * 0.5f
        val sliderUsablePosMax = bb.max[axis] - grabPadding - grabSz * 0.5f

        var logarithmicZeroEpsilon = 0f // Only valid when is_logarithmic is true
        var zeroDeadzoneHalfsize = 0f // Only valid when is_logarithmic is true
        if (isLogarithmic) {
            // When using logarithmic sliders, we need to clamp to avoid hitting zero, but our choice of clamp value greatly affects slider precision. We attempt to use the specified precision to estimate a good lower bound.
            val decimalPrecision = if (isFloatingPoint) parseFormatPrecision(format, 3) else 1
            logarithmicZeroEpsilon = 0.1f pow decimalPrecision.f
            zeroDeadzoneHalfsize = style.logSliderDeadzone * 0.5f / max(sliderUsableSz, 1f)
        }

        // Process interacting with the slider
        var valueChanged = false
        if (g.activeId == id) {
            var setNewValue = false
            var clickedT = 0f
            if (g.activeIdSource == InputSource.Mouse) if (!io.mouseDown[0]) clearActiveID()
            else {
                val mouseAbsPos = io.mousePos[axis]
                if (g.activeIdIsJustActivated) {
                    var grabT: Float = scaleRatioFromValue(v(), vMin, vMax, isLogarithmic, logarithmicZeroEpsilon, zeroDeadzoneHalfsize)
                    if (axis == Axis.Y) grabT = 1f - grabT
                    val grabPos = lerp(sliderUsablePosMin, sliderUsablePosMax, grabT)
                    val clickedAroundGrab = mouseAbsPos >= grabPos - grabSz * 0.5f - 1f && mouseAbsPos <= grabPos + grabSz * 0.5f + 1f // No harm being extra generous here.
                    g.sliderGrabClickOffset = if (clickedAroundGrab && isFloatingPoint) mouseAbsPos - grabPos else 0f
                }
                if (sliderUsableSz > 0f) clickedT = saturate((mouseAbsPos - g.sliderGrabClickOffset - sliderUsablePosMin) / sliderUsableSz)
                if (axis == Axis.Y) clickedT = 1f - clickedT
                setNewValue = true
            }
            else if (g.activeIdSource == InputSource.Keyboard || g.activeIdSource == InputSource.Gamepad) {

                if (g.activeIdIsJustActivated) {
                    g.sliderCurrentAccum = 0f // Reset any stored nav delta upon activation
                    g.sliderCurrentAccumDirty = false
                }

                var inputDelta = if (axis == Axis.X) getNavTweakPressedAmount(axis) else -getNavTweakPressedAmount(axis)
                if (inputDelta != 0f) {
                    val tweakSlow = (if (g.navInputSource == InputSource.Gamepad) Key._NavGamepadTweakSlow else Key._NavKeyboardTweakSlow).isDown
                    val tweakFast = (if (g.navInputSource == InputSource.Gamepad) Key._NavGamepadTweakFast else Key._NavKeyboardTweakFast).isDown
                    val decimalPrecision = if (isFloatingPoint) parseFormatPrecision(format, 3) else 0
                    if (decimalPrecision > 0) {
                        inputDelta /= 100f    // Gamepad/keyboard tweak speeds in % of slider bounds
                        if (tweakSlow) inputDelta /= 10f
                    } else if ((vRange.fp >= (-100f).fp && vRange.fp <= 100f.fp) || tweakSlow) inputDelta = (if (inputDelta < 0f) -1f else +1f) / vRange.f // Gamepad/keyboard tweak speeds in integer steps
                    else inputDelta /= 100f
                    if (tweakFast) inputDelta *= 10f

                    g.sliderCurrentAccum += inputDelta
                    g.sliderCurrentAccumDirty = true
                }

                val delta = g.sliderCurrentAccum

                if (g.navActivatePressedId == id && !g.activeIdIsJustActivated) clearActiveID()
                else if (g.sliderCurrentAccumDirty) {
                    clickedT = scaleRatioFromValue(v(), vMin, vMax, isLogarithmic, logarithmicZeroEpsilon, zeroDeadzoneHalfsize)

                    if ((clickedT >= 1f && delta > 0f) || (clickedT <= 0f && delta < 0f)) { // This is to avoid applying the saturation when already past the limits
                        setNewValue = false
                        g.sliderCurrentAccum = 0f // If pushing up against the limits, don't continue to accumulate
                    } else {
                        setNewValue = true
                        val oldClickedT = clickedT
                        clickedT = saturate(clickedT + delta)

                        // Calculate what our "new" clicked_t will be, and thus how far we actually moved the slider, and subtract this from the accumulator
                        var vNew = scaleValueFromRatio(clickedT, vMin, vMax, isLogarithmic, logarithmicZeroEpsilon, zeroDeadzoneHalfsize)
                        if (isFloatingPoint && flags hasnt SliderFlag.NoRoundToFormat) vNew = roundScalarWithFormat(format, vNew)
                        val newClickedT = scaleRatioFromValue(vNew, vMin, vMax, isLogarithmic, logarithmicZeroEpsilon, zeroDeadzoneHalfsize)

                        g.sliderCurrentAccum -= when {
                            delta > 0 -> min(newClickedT - oldClickedT, delta)
                            else -> max(newClickedT - oldClickedT, delta)
                        }
                    }
                    g.sliderCurrentAccumDirty = false
                }
            }

            if (setNewValue) {
                var vNew = scaleValueFromRatio(clickedT, vMin, vMax, isLogarithmic, logarithmicZeroEpsilon, zeroDeadzoneHalfsize)

                // Round to user desired precision based on format string
                if (isFloatingPoint && flags hasnt SliderFlag.NoRoundToFormat) vNew = roundScalarWithFormat(format, vNew)

                // Apply result
                if (v() != vNew) {
                    v(vNew)
                    valueChanged = true
                }
            }
        }

        if (sliderSz < 1f) outGrabBb.put(bb.min, bb.min)
        else {
            // Output grab position so it can be displayed by the caller
            var grabT = scaleRatioFromValue(v(), vMin, vMax, isLogarithmic, logarithmicZeroEpsilon, zeroDeadzoneHalfsize)
            if (axis == Axis.Y) grabT = 1f - grabT
            val grabPos = lerp(sliderUsablePosMin, sliderUsablePosMax, grabT)
            if (axis == Axis.X) outGrabBb.put(grabPos - grabSz * 0.5f, bb.min.y + grabPadding, grabPos + grabSz * 0.5f, bb.max.y - grabPadding)
            else outGrabBb.put(bb.min.x + grabPadding, grabPos - grabSz * 0.5f, bb.max.x - grabPadding, grabPos + grabSz * 0.5f)
        }

        return valueChanged
    }

    /** template<TYPE = N> */
    fun <N> NumberOps<N>.roundScalarWithFormat(fmt: String, v: N): N where N : Number, N : Comparable<N> {
        assert(isFloatingPoint)
        val fmtStart = parseFormatFindStart2(fmt)
        if (fmt.getOrNul(fmtStart + 0) != '%' || fmt.getOrNul(fmtStart + 1) == '%') // Don't apply if the value is not visible in the format string
            return v
        val fmtEnd = parseFormatFindEnd(fmt, fmtStart)
        val vStr = fmt.substring(fmtStart, fmtEnd).format(v).trimStart()
        return vStr.replace(',', '.').parsed
    }

    fun <F : Flag<F>> checkboxFlagsT(label: String, flagsPtr: KMutableProperty0<Flag<F>>, flagsValue: Flag<F>): Boolean {
        var flags by flagsPtr
        val allOnRef = (flagsValue in flags).mutableReference
        val allOn by allOnRef
        val anyOn = flags has flagsValue
        val pressed = when {
            !allOn && anyOn -> {
                g.nextItemData.itemFlags /= ItemFlag.MixedValue
                checkbox(label, allOnRef)
            }
            else -> checkbox(label, allOnRef)
        }
        if (pressed) flags = when {
            allOn -> flags or flagsValue
            else -> flags wo flagsValue
        }
        return pressed
    }
}


// ~SliderScalarN
inline fun widgetN(label: String, components: Int, widgets: (Int) -> Boolean): Boolean {
    if (currentWindow.skipItems)
        return false

    var valueChanged = false
    group {
        withID(label) {
            pushMultiItemsWidths(components, calcItemWidth())
            for (i in 0 until components) {
                withID(i) {
                    if (i > 0) sameLine(0f, style.itemInnerSpacing.x)
                    valueChanged /= widgets(i)
                }
                popItemWidth()
            }
        }

        val labelEnd = findRenderedTextEnd(label)
        if (0 != labelEnd) {
            sameLine(0f, style.itemInnerSpacing.x)
            textEx(label, labelEnd)
        }

    }
    return valueChanged
}