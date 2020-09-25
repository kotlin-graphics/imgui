package imgui.api

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
import imgui.ImGui.calcItemWidth
import imgui.ImGui.currentWindow
import imgui.ImGui.dragScalarInternal
import imgui.ImGui.dragScalarNInternal
import imgui.ImGui.endGroup
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.popID
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushID
import imgui.ImGui.pushMultiItemsWidths
import imgui.ImGui.sameLine
import imgui.ImGui.style
import imgui.ImGui.textEx
import imgui.internal.sections.DragFlag
import uno.kotlin.getValue
import kotlin.reflect.KMutableProperty0

@Suppress("UNCHECKED_CAST")

/** Widgets: Drags
 *  - CTRL+Click on any drag box to turn them into an input box. Manually input values aren't clamped and can go off-bounds.
 *  - For all the Float2/Float3/Float4/Int2/Int3/Int4 versions of every functions, note that a 'float v[X]' function argument
 *      is the same as 'float* v', the array syntax is just a way to document the number of elements that are expected to be
 *      accessible. You can pass address of your first element out of a contiguous set, e.g. &myvector.x
 *  - Adjust format string to decorate the value with a prefix, a suffix, or adapt the editing and display precision
 *      e.g. "%.3f" -> 1.234; "%5.2f secs" -> 01.23 secs; "Biscuit: %.0f" -> Biscuit: 1; etc.
 *  - Speed are per-pixel of mouse movement (v_speed=0.2f: mouse needs to move by 5 pixels to increase value by 1).
 *      For gamepad/keyboard navigation, minimum speed is Max(v_speed, minimum_step_at_given_precision).
 *  - Use v_min < v_max to clamp edits to given limits. Note that CTRL+Click manual input can override those limits. */
interface widgetsDrags {

    /** If v_min >= v_max we have no bound */
    fun dragFloat(label: String, v: KMutableProperty0<Float>, vSpeed: Float = 1f, vMin: Float = 0f,
                  vMax: Float = 0f, format: String? = "%.3f", flags: DragSliderFlags = 0): Boolean =
            dragScalar(label, DataType.Float, v, vSpeed, vMin, vMax, format, flags)

    /** If v_min >= v_max we have no bound */
    fun dragFloat(label: String, v: FloatArray, ptr: Int, vSpeed: Float = 1f, vMin: Float = 0f,
                  vMax: Float = 0f, format: String = "%.3f", flags: DragSliderFlags = 0): Boolean =
            withFloat(v, ptr) { dragScalar(label, DataType.Float, it, vSpeed, vMin, vMax, format, flags) }

    fun dragFloat2(label: String, v: FloatArray, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f,
                   format: String = "%.3f", flags: DragSliderFlags = 0): Boolean =
            dragScalarN(label, DataType.Float, v, 2, vSpeed, vMin, vMax, format, flags)

    fun dragVec2(label: String, v: Vec2, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f,
                 format: String = "%.3f", flags: DragSliderFlags = 0): Boolean =
            dragScalarN(label, DataType.Float, v to _fa, 2, vSpeed, vMin, vMax, format, flags)
                    .also { v put _fa }

    fun dragFloat3(label: String, v: FloatArray, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f,
                   format: String = "%.3f", flags: DragSliderFlags = 0): Boolean =
            dragScalarN(label, DataType.Float, v, 3, vSpeed, vMin, vMax, format, flags)

    fun dragVec3(label: String, v: Vec3, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f,
                 format: String = "%.3f", flags: DragSliderFlags = 0): Boolean =
            dragScalarN(label, DataType.Float, v to _fa, 3, vSpeed, vMin, vMax, format, flags)
                    .also { v put _fa }

    fun dragFloat4(label: String, v: FloatArray, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f,
                   format: String = "%.3f", flags: DragSliderFlags = 0): Boolean =
            dragScalarN(label, DataType.Float, v, 4, vSpeed, vMin, vMax, format, flags)

    fun dragVec4(label: String, v: Vec4, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f,
                 format: String = "%.3f", flags: DragSliderFlags = 0): Boolean =
            dragScalarN(label, DataType.Float, v to _fa, 4, vSpeed, vMin, vMax, format, flags)
                    .also { v put _fa }

    fun dragFloatRange2(label: String, vCurrentMinPtr: KMutableProperty0<Float>, vCurrentMaxPtr: KMutableProperty0<Float>,
                        vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f, format: String = "%.3f",
                        formatMax: String = format, flags: DragSliderFlags = 0): Boolean {

        val vCurrentMin by vCurrentMinPtr
        val vCurrentMax by vCurrentMaxPtr
        val window = currentWindow
        if (window.skipItems) return false

        pushID(label)
        beginGroup()
        pushMultiItemsWidths(2, calcItemWidth())

        var min = if (vMin >= vMax) -Float.MAX_VALUE else vMin
        var max = if (vMin >= vMax) vCurrentMax else vMax min vCurrentMax
        if (min == max) {
            min = Float.MAX_VALUE; max = -Float.MAX_VALUE; } // Lock edit
        var valueChanged = dragScalar("##min", DataType.Float, vCurrentMinPtr, vSpeed, min, max, format, power)
        popItemWidth()
        sameLine(0f, style.itemInnerSpacing.x)

        min = if (vMin >= vMax) vCurrentMin else vMin max vCurrentMin
        max = if (vMin >= vMax) Float.MAX_VALUE else vMax
        if (min == max) {
            min = Float.MAX_VALUE; max = -Float.MAX_VALUE; } // Lock edit
        val f = if (formatMax.isNotEmpty()) formatMax else format
        valueChanged = dragScalar("##max", DataType.Float, vCurrentMaxPtr, vSpeed, min, max, f, power) || valueChanged
        popItemWidth()
        sameLine(0f, style.itemInnerSpacing.x)

        textEx(label, findRenderedTextEnd(label))
        endGroup()
        popID()
        return valueChanged
    }

    /** If v_min >= v_max we have no bound
     *
     *  NB: vSpeed is float to allow adjusting the drag speed with more precision     */
    fun dragInt(label: String, v: IntArray, ptr: Int, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0,
                format: String = "%d", flags: DragSliderFlags = 0): Boolean =
            withInt(v, ptr) { dragInt(label, it, vSpeed, vMin, vMax, format, flags) }

    fun dragInt(label: String, v: KMutableProperty0<Int>, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0,
                format: String = "%d", flags: DragSliderFlags = 0): Boolean =
            dragScalar(label, DataType.Int, v, vSpeed, vMin, vMax, format, flags)

    fun dragInt2(label: String, v: IntArray, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0,
                 format: String = "%d", flags: DragSliderFlags = 0): Boolean =
            dragScalarN(label, DataType.Int, v, 2, vSpeed, vMin, vMax, format, flags)

    fun dragVec2i(label: String, v: Vec2i, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0,
                  format: String = "%d", flags: DragSliderFlags = 0): Boolean =
            dragScalarN(label, DataType.Int, v to _ia, 2, vSpeed, vMin, vMax, format, flags)
                    .also { v put _ia }

    fun dragInt3(label: String, v: IntArray, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0,
                 format: String = "%d", flags: DragSliderFlags = 0): Boolean =
            dragScalarN(label, DataType.Int, v, 3, vSpeed, vMin, vMax, format, flags)

    fun dragVec3i(label: String, v: Vec3i, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0,
                  format: String = "%d", flags: DragSliderFlags = 0): Boolean =
            dragScalarN(label, DataType.Int, v to _ia, 3, vSpeed, vMin, vMax, format, flags)
                    .also { v put _ia }

    fun dragInt4(label: String, v: IntArray, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0,
                 format: String = "%d", flags: DragSliderFlags = 0): Boolean =
            dragScalarN(label, DataType.Int, v, 4, vSpeed, vMin, vMax, format, flags)

    fun dragVec4i(label: String, v: Vec4i, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0,
                  format: String = "%d", flags: DragSliderFlags = 0): Boolean =
            dragScalarN(label, DataType.Int, v to _ia, 4, vSpeed, vMin, vMax, format, flags)
                    .also { v put _ia }

    fun dragIntRange2(label: String, vCurrentMinPtr: KMutableProperty0<Int>, vCurrentMaxPtr: KMutableProperty0<Int>,
                      vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0, format: String = "%d",
                      formatMax: String = format, flags: DragSliderFlags = 0): Boolean {

        val vCurrentMin by vCurrentMinPtr
        val vCurrentMax by vCurrentMaxPtr
        val window = currentWindow
        if (window.skipItems) return false

        pushID(label)
        beginGroup()
        pushMultiItemsWidths(2, calcItemWidth())

        var min = if (vMin >= vMax) Int.MIN_VALUE else vMin
        var max = if (vMin >= vMax) vCurrentMax else vMax min vCurrentMax
        if (min == max) {
            min = Int.MAX_VALUE; max = Int.MIN_VALUE; } // Lock edit
        var valueChanged = dragInt("##min", vCurrentMinPtr, vSpeed, min, max, format)
        popItemWidth()
        sameLine(0f, style.itemInnerSpacing.x)

        min = if (vMin >= vMax) vCurrentMin else vMin max vCurrentMin
        max = if (vMin >= vMax) Int.MAX_VALUE else vMax
        if (min == max) {
            min = Int.MAX_VALUE; max = Int.MIN_VALUE; } // Lock edit
        val f = if (formatMax.isNotEmpty()) formatMax else format
        valueChanged = dragInt("##max", vCurrentMaxPtr, vSpeed, min, max, f) || valueChanged
        popItemWidth()
        sameLine(0f, style.itemInnerSpacing.x)

        textEx(label, findRenderedTextEnd(label))
        endGroup()
        popID()
        return valueChanged
    }

    /** For all the Float2/Float3/Float4/Int2/Int3/Int4 versions of every functions, note that a 'float v[X]' function
     *  argument is the same as 'float* v', the array syntax is just a way to document the number of elements that are
     *  expected to be accessible. You can pass address of your first element out of a contiguous set, e.g. &myvector.x
     *  Adjust format string to decorate the value with a prefix, a suffix, or adapt the editing and display precision
     *  e.g. "%.3f" -> 1.234; "%5.2f secs" -> 01.23 secs; "Biscuit: %.0f" -> Biscuit: 1; etc.
     *  Speed are per-pixel of mouse movement (vSpeed = 0.2f: mouse needs to move by 5 pixels to increase value by 1).
     *  For gamepad/keyboard navigation, minimum speed is Max(vSpeed, minimumStepAtGivenPrecision). */
    fun dragScalar(label: String, pData: FloatArray, vSpeed: Float, pMin: Float? = null, pMax: Float? = null,
                   format: String? = null, flags: DragSliderFlags = 0): Boolean =
            dragScalarInternal(label, pData, 0, vSpeed, pMin, pMax, format, 1f, flags)

    /** If vMin >= vMax we have no bound  */
    fun dragScalar(label: String, pData: FloatArray, ptr: Int = 0, vSpeed: Float, pMin: Float? = null,
                   pMax: Float? = null, format: String? = null, flags: DragSliderFlags = 0): Boolean =
            withFloat(pData, ptr) {
                dragScalarInternal(label, DataType.Float, it, vSpeed, pMin, pMax, format, 1f, flags)
            }

    /** Internal implementation - see below for entry points
     *
     *  Note: p_data, p_min and p_max are _pointers_ to a memory address holding the data.
     *  For a Drag widget, p_min and p_max are optional.
     *  Read code of e.g. SliderFloat(), SliderInt() etc. or examples in 'Demo->Widgets->Data Types' to understand
     *  how to use this function directly. */
    fun <N> dragScalar(label: String, dataType: DataType, pData: KMutableProperty0<N>, vSpeed: Float,
                       pMin: N? = null, pMax: N? = null, format: String? = null, flags: DragSliderFlags = 0): Boolean
            where N : Number, N : Comparable<N> =
            dragScalarInternal(label, dataType, pData, vSpeed, pMin, pMax, format, 1f, flags)

    /** Note: p_data, p_min and p_max are _pointers_ to a memory address holding the data. For a Drag widget,
     *  p_min and p_max are optional.
     *  Read code of e.g. SliderFloat(), SliderInt() etc. or examples in 'Demo->Widgets->Data Types' to understand
     *  how to use this function directly. */
    fun <N> dragScalarN(label: String, dataType: DataType, v: Any, components: Int, vSpeed: Float, vMin: N? = null,
                        vMax: N? = null, format: String? = null, flags: DragSliderFlags): Boolean
            where N : Number, N : Comparable<N> =
            dragScalarNInternal(label, dataType, v, components, vSpeed, vMin, vMax, format, 1f, flags)
}