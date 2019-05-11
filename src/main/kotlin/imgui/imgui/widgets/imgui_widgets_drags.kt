package imgui.imgui.widgets

import glm_.func.common.max
import glm_.func.common.min
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec3.Vec3
import glm_.vec3.Vec3i
import glm_.vec4.Vec4
import glm_.vec4.Vec4i
import imgui.*
import imgui.ImGui.beginGroup
import imgui.ImGui.calcTextSize
import imgui.ImGui.currentWindow
import imgui.ImGui.dragBehavior
import imgui.ImGui.endGroup
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.focusableItemRegister
import imgui.ImGui.focusableItemUnregister
import imgui.ImGui.format
import imgui.ImGui.io
import imgui.ImGui.itemAdd
import imgui.ImGui.itemHoverable
import imgui.ImGui.itemSize
import imgui.ImGui.markItemEdited
import imgui.ImGui.nextItemWidth
import imgui.ImGui.parseFormatFindEnd
import imgui.ImGui.parseFormatFindStart
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushId
import imgui.ImGui.pushMultiItemsWidths
import imgui.ImGui.renderFrame
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.sameLine
import imgui.ImGui.setActiveId
import imgui.ImGui.setFocusId
import imgui.ImGui.style
import imgui.ImGui.tempInputTextIsActive
import imgui.ImGui.tempInputTextScalar
import imgui.ImGui.textEx
import imgui.imgui.g
import imgui.internal.DragFlag
import imgui.internal.Rect
import imgui.internal.focus
import uno.kotlin.getValue
import kotlin.reflect.KMutableProperty0

/** Widgets: Drags
 *  - CTRL+Click on any drag box to turn them into an input box. Manually input values aren't clamped and can go off-bounds.
 *  - For all the Float2/Float3/Float4/Int2/Int3/Int4 versions of every functions, note that a 'float v[X]' function argument
 *      is the same as 'float* v', the array syntax is just a way to document the number of elements that are expected to be
 *      accessible. You can pass address of your first element out of a contiguous set, e.g. &myvector.x
 *  - Adjust format string to decorate the value with a prefix, a suffix, or adapt the editing and display precision
 *      e.g. "%.3f" -> 1.234; "%5.2f secs" -> 01.23 secs; "Biscuit: %.0f" -> Biscuit: 1; etc.
 *  - Speed are per-pixel of mouse movement (v_speed=0.2f: mouse needs to move by 5 pixels to increase value by 1).
 *      For gamepad/keyboard navigation, minimum speed is Max(v_speed, minimum_step_at_given_precision). */
interface imgui_widgets_drags {

    /** If v_min >= v_max we have no bound */
    fun dragFloat(label: String, v: KMutableProperty0<Float>, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f,
                  format: String? = "%.3f", power: Float = 1f): Boolean =
            dragScalar(label, DataType.Float, v, vSpeed, vMin, vMax, format, power)

    /** If v_min >= v_max we have no bound */
    fun dragFloat(label: String, v: FloatArray, ptr: Int, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f,
                  format: String = "%.3f", power: Float = 1f): Boolean =
            withFloat(v, ptr) { dragScalar(label, DataType.Float, it, vSpeed, vMin, vMax, format, power) }

    fun dragFloat2(label: String, v: FloatArray, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f, format: String = "%.3f", power: Float = 1f) =
            dragScalarN(label, DataType.Float, v, 2, vSpeed, vMin, vMax, format, power)

    fun dragVec2(label: String, v: Vec2, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f, format: String = "%.3f", power: Float = 1f): Boolean =
            dragScalarN(label, DataType.Float, v to _f, 2, vSpeed, vMin, vMax, format, power)
                    .also { v put _f }

    fun dragFloat3(label: String, v: FloatArray, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f, format: String = "%.3f",
                   power: Float = 1f) = dragScalarN(label, DataType.Float, v, 3, vSpeed, vMin, vMax, format, power)

    fun dragVec3(label: String, v: Vec3, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f, format: String = "%.3f",
                 power: Float = 1f): Boolean =
            dragScalarN(label, DataType.Float, v to _f, 3, vSpeed, vMin, vMax, format, power)
                    .also { v put _f }

    fun dragFloat4(label: String, v: FloatArray, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f, format: String = "%.3f",
                   power: Float = 1f): Boolean = dragScalarN(label, DataType.Float, v, 4, vSpeed, vMin, vMax, format, power)

    fun dragVec4(label: String, v: Vec4, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f, format: String = "%.3f",
                 power: Float = 1f): Boolean =
            dragScalarN(label, DataType.Float, v to _f, 4, vSpeed, vMin, vMax, format, power)
                    .also { v put _f }

    fun dragFloatRange2(label: String, vCurrentMinPtr: KMutableProperty0<Float>, vCurrentMaxPtr: KMutableProperty0<Float>,
                        vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f, format: String = "%.3f", formatMax: String = format,
                        power: Float = 1f): Boolean {

        val vCurrentMin by vCurrentMinPtr
        val vCurrentMax by vCurrentMaxPtr
        val window = currentWindow
        if (window.skipItems) return false

        pushId(label)
        beginGroup()
        pushMultiItemsWidths(2, nextItemWidth)

        var min = if (vMin >= vMax) -Float.MAX_VALUE else vMin
        var max = if (vMin >= vMax) vCurrentMax else vMax min vCurrentMax
        var valueChanged = dragFloat("##min", vCurrentMinPtr, vSpeed, min, max, format, power)
        popItemWidth()
        sameLine(0f, style.itemInnerSpacing.x)
        min = if (vMin >= vMax) vCurrentMin else vMin max vCurrentMin
        max = if (vMin >= vMax) Float.MAX_VALUE else vMax
        valueChanged = dragFloat("##max", vCurrentMaxPtr, vSpeed, min, max, formatMax, power) || valueChanged
        popItemWidth()
        sameLine(0f, style.itemInnerSpacing.x)

        textEx(label, findRenderedTextEnd(label))
        endGroup()
        popId()
        return valueChanged
    }

    /** If v_min >= v_max we have no bound
     *
     *  NB: vSpeed is float to allow adjusting the drag speed with more precision     */
    fun dragInt(label: String, v: IntArray, ptr: Int, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0, format: String = "%d"): Boolean =
            withInt(v, ptr) { dragInt(label, it, vSpeed, vMin, vMax, format) }

    fun dragInt(label: String, v: KMutableProperty0<Int>, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0,
                format: String = "%d"): Boolean = dragScalar(label, DataType.Int, v, vSpeed, vMin, vMax, format)

    fun dragInt2(label: String, v: IntArray, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0, format: String = "%d"): Boolean =
            dragScalarN(label, DataType.Int, v, 2, vSpeed, vMin, vMax, format)

    fun dragVec2i(label: String, v: Vec2i, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0, format: String = "%d"): Boolean =
            dragScalarN(label, DataType.Int, v to _i, 2, vSpeed, vMin, vMax, format)
                    .also { v put _i }

    fun dragInt3(label: String, v: IntArray, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0, format: String = "%d"): Boolean =
            dragScalarN(label, DataType.Int, v, 3, vSpeed, vMin, vMax, format)

    fun dragVec3i(label: String, v: Vec3i, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0, format: String = "%d"): Boolean =
            dragScalarN(label, DataType.Int, v to _i, 3, vSpeed, vMin, vMax, format)
                    .also { v put _i }

    fun dragInt4(label: String, v: IntArray, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0, format: String = "%d"): Boolean =
            dragScalarN(label, DataType.Int, v, 4, vSpeed, vMin, vMax, format)

    fun dragVec4i(label: String, v: Vec4i, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0, format: String = "%d"): Boolean =
            dragScalarN(label, DataType.Int, v to _i, 4, vSpeed, vMin, vMax, format)
                    .also { v put _i }

    fun dragIntRange2(label: String, vCurrentMinPtr: KMutableProperty0<Int>, vCurrentMaxPtr: KMutableProperty0<Int>, vSpeed: Float = 1f,
                      vMin: Int = 0, vMax: Int = 0, format: String = "%d", formatMax: String = format): Boolean {

        val vCurrentMin by vCurrentMinPtr
        val vCurrentMax by vCurrentMaxPtr
        val window = currentWindow
        if (window.skipItems) return false

        pushId(label)
        beginGroup()
        pushMultiItemsWidths(2, nextItemWidth)

        var min = if (vMin >= vMax) Int.MIN_VALUE else vMin
        var max = if (vMin >= vMax) vCurrentMax else vMax min vCurrentMax
        var valueChanged = dragInt("##min", vCurrentMinPtr, vSpeed, min, max, format)
        popItemWidth()
        sameLine(0f, style.itemInnerSpacing.x)
        min = if (vMin >= vMax) vCurrentMin else vMin max vCurrentMin
        max = if (vMin >= vMax) Int.MAX_VALUE else vMax
        valueChanged = dragInt("##max", vCurrentMaxPtr, vSpeed, min, max, formatMax) || valueChanged
        popItemWidth()
        sameLine(0f, style.itemInnerSpacing.x)

        textEx(label, findRenderedTextEnd(label))
        endGroup()
        popId()
        return valueChanged
    }

    /** For all the Float2/Float3/Float4/Int2/Int3/Int4 versions of every functions, note that a 'float v[X]' function
     *  argument is the same as 'float* v', the array syntax is just a way to document the number of elements that are
     *  expected to be accessible. You can pass address of your first element out of a contiguous set, e.g. &myvector.x
     *  Adjust format string to decorate the value with a prefix, a suffix, or adapt the editing and display precision
     *  e.g. "%.3f" -> 1.234; "%5.2f secs" -> 01.23 secs; "Biscuit: %.0f" -> Biscuit: 1; etc.
     *  Speed are per-pixel of mouse movement (vSpeed = 0.2f: mouse needs to move by 5 pixels to increase value by 1).
     *  For gamepad/keyboard navigation, minimum speed is Max(vSpeed, minimumStepAtGivenPrecision). */
    fun dragScalar(label: String, v: FloatArray, vSpeed: Float, vMin: Float? = null, vMax: Float? = null, format: String? = null,
                   power: Float = 1f): Boolean = dragScalar(label, v, 0, vSpeed, vMin, vMax, format, power)

    /** If vMin >= vMax we have no bound  */
    fun dragScalar(label: String, v: FloatArray, ptr: Int = 0, vSpeed: Float, vMin: Float? = null, vMax: Float? = null,
                   format: String? = null, power: Float = 1f): Boolean =
            withFloat(v, ptr) { dragScalar(label, DataType.Float, it, vSpeed, vMin, vMax, format, power) }

    fun <N> dragScalar(label: String, dataType: DataType,
                       v: KMutableProperty0<N>, vSpeed: Float,
                       vMin: N? = null, vMax: N? = null,
                       format_: String? = null, power: Float = 1f): Boolean where N : Number, N : Comparable<N> {

        val window = currentWindow
        if (window.skipItems) return false

        if (power != 1f)
            assert(vMin != null && vMax != null) // When using a power curve the drag needs to have known bounds

        val id = window.getId(label)
        val w = nextItemWidth

        val labelSize = calcTextSize(label, -1, true)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w, labelSize.y + style.framePadding.y * 2f))
        val totalBb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))

        itemSize(totalBb, style.framePadding.y)
        if (!itemAdd(totalBb, id, frameBb))
            return false

        // Default format string when passing NULL
        val format = when {
            format_ == null -> when (dataType) {
                DataType.Float, DataType.Double -> "%f"
                else -> "%d" // (FIXME-LEGACY: Patch old "%.0f" format string to use "%d", read function more details.)
            }
            dataType == DataType.Int && format_ != "%d" -> patchFormatStringFloatToInt(format_)
            else -> format_
        }

        // Tabbing or CTRL-clicking on Drag turns it into an input box
        val hovered = itemHoverable(frameBb, id)
        val tempInputIsActive = tempInputTextIsActive(id)
        var tempInputStart = false
        if (!tempInputIsActive) {
            val focusRequested = focusableItemRegister(window, id)
            val clicked = hovered && io.mouseClicked[0]
            val doubleClicked = hovered && io.mouseDoubleClicked[0]
            if (focusRequested || clicked || doubleClicked || g.navActivateId == id || g.navInputId == id) {
                setActiveId(id, window)
                setFocusId(id, window)
                window.focus()
                g.activeIdAllowNavDirFlags = (1 shl Dir.Up) or (1 shl Dir.Down)
                if (focusRequested || (clicked && io.keyCtrl) || doubleClicked || g.navInputId == id) {
                    tempInputStart = true
                    focusableItemUnregister(window)
                }
            }
        }
        if (tempInputIsActive || tempInputStart)
            return tempInputTextScalar(frameBb, id, label, dataType, v, format)

        // Draw frame
        val frameCol = when (g.activeId) {
            id -> Col.FrameBgActive
            else -> when (g.hoveredId) {
                id -> Col.FrameBgHovered
                else -> Col.FrameBg
            }
        }
        renderNavHighlight(frameBb, id)
        renderFrame(frameBb.min, frameBb.max, frameCol.u32, true, style.frameRounding)

        // Actual drag behavior
        val valueChanged = dragBehavior(id, dataType, v, vSpeed, vMin, vMax, format, power, DragFlag.None.i)
        if (valueChanged)
            markItemEdited(id)

        // Display value using user-provided display format so user can add prefix/suffix/decorations to the value.
        val value = String(v.format(dataType, format))
        renderTextClipped(frameBb.min, frameBb.max, value, value.length, null, Vec2(0.5f))

        if (labelSize.x > 0f)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, frameBb.min.y + style.framePadding.y), label)

        ImGuiTestEngineHook_ItemInfo(id, label, window.dc.itemFlags)
        return valueChanged
    }

    fun <N> dragScalarN(label: String, dataType: DataType, v: Any, components: Int, vSpeed: Float, vMin: N? = null, vMax: N? = null,
                        format: String? = null, power: Float = 1f): Boolean where N : Number, N : Comparable<N> {

        val window = currentWindow
        if (window.skipItems) return false

        var valueChanged = false
        beginGroup()
        pushId(label)
        pushMultiItemsWidths(components, nextItemWidth)
        for (i in 0 until components) {
            pushId(i)
            when (dataType) {
                DataType.Int -> withInt(v as IntArray, i) {
                    valueChanged = dragScalar("", dataType, it as KMutableProperty0<N>, vSpeed, vMin, vMax, format, power) or valueChanged
                }
                DataType.Float -> withFloat(v as FloatArray, i) {
                    valueChanged = dragScalar("", dataType, it as KMutableProperty0<N>, vSpeed, vMin, vMax, format, power) or valueChanged
                }
                else -> error("invalid")
            }
            sameLine(0f, style.itemInnerSpacing.x)
            popId()
            popItemWidth()
        }
        popId()

        textEx(label, findRenderedTextEnd(label))
        endGroup()
        return valueChanged
    }

    companion object {

        // static arrays to avoid GC pressure
        val _f = FloatArray(4)
        val _i = IntArray(4)

        private inline fun <R> withInt(ints: IntArray, ptr: Int, block: (KMutableProperty0<Int>) -> R): R {
            Ref.iPtr++
            val i = Ref::int
            i.set(ints[ptr])
            val res = block(i)
            ints[ptr] = i()
            Ref.iPtr--
            return res
        }

        private inline fun <R> withFloat(floats: FloatArray, ptr: Int, block: (KMutableProperty0<Float>) -> R): R {
            Ref.fPtr++
            val f = Ref::float
            f.set(floats[ptr])
            val res = block(f)
            floats[ptr] = f()
            Ref.fPtr--
            return res
        }

        /** FIXME-LEGACY: Prior to 1.61 our DragInt() function internally used floats and because of this the compile-time default value
         *  for format was "%.0f".
         *  Even though we changed the compile-time default, we expect users to have carried %f around, which would break
         *  the display of DragInt() calls.
         *  To honor backward compatibility we are rewriting the format string, unless IMGUI_DISABLE_OBSOLETE_FUNCTIONS is enabled.
         *  What could possibly go wrong?! */
        fun patchFormatStringFloatToInt(fmt: String): String {
            if (fmt == "%.0f") // Fast legacy path for "%.0f" which is expected to be the most common case.
                return "%d"
            val fmtStart = parseFormatFindStart(fmt)    // Find % (if any, and ignore %%)
            // Find end of format specifier, which itself is an exercise of confidence/recklessness (because snprintf is dependent on libc or user).
            val fmtEnd = parseFormatFindEnd(fmt, fmtStart)
            if (fmtEnd > fmtStart && fmt[fmtEnd - 1] == 'f') {
                if (fmtStart == 0 && fmtEnd == fmt.length)
                    return "%d"
                return fmt.substring(0, fmtStart) + "%d" + fmt.substring(fmtEnd, fmt.length)
            }
            return fmt
        }
    }
}