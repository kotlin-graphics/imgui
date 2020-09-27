package imgui.api

import glm_.func.deg
import glm_.func.rad
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec3.Vec3
import glm_.vec3.Vec3i
import glm_.vec4.Vec4
import glm_.vec4.Vec4i
import imgui.*
import imgui.ImGui.sliderScalarInternal
import imgui.ImGui.sliderScalarNInternal
import imgui.ImGui.vSliderScalarInternal
import kool.getValue
import kool.setValue
import kotlin.reflect.KMutableProperty0

@Suppress("UNCHECKED_CAST")

/** Widgets: Sliders
 *  - CTRL+Click on any slider to turn them into an input box. Manually input values aren't clamped and can go off-bounds.
 *  - Adjust format string to decorate the value with a prefix, a suffix, or adapt the editing and display precision
 *      e.g. "%.3f" -> 1.234; "%5.2f secs" -> 01.23 secs; "Biscuit: %.0f" -> Biscuit: 1; etc.   */
interface widgetsSliders {


    /** Adjust format to decorate the value with a prefix or a suffix.
     *  "%.3f"         1.234
     *  "%5.2f secs"   01.23 secs
     *  "Gold: %.0f"   Gold: 1
     *  Use power != 1.0f for non-linear sliders.
     *  adjust format to decorate the value with a prefix or a suffix for in-slider labels or unit display. Use power!=1.0 for power curve sliders */
    fun sliderFloat(label: String, v: FloatArray, ptr: Int, vMin: Float, vMax: Float,
                    format: String = "%.3f", flags: DragFlags = 0): Boolean =
            withFloat(v, ptr) { sliderFloat(label, it, vMin, vMax, format, flags) }

    /** Adjust format to decorate the value with a prefix or a suffix.
     *  "%.3f"         1.234
     *  "%5.2f secs"   01.23 secs
     *  "Gold: %.0f"   Gold: 1
     *  Use power != 1.0f for non-linear sliders.
     *  adjust format to decorate the value with a prefix or a suffix for in-slider labels or unit display. Use power!=1.0 for power curve sliders */
    fun sliderFloat(label: String, v: KMutableProperty0<Float>, vMin: Float, vMax: Float,
                    format: String = "%.3f", flags: DragFlags = 0): Boolean =
            sliderScalar(label, DataType.Float, v, vMin, vMax, format, flags)

    fun sliderFloat2(label: String, v: FloatArray, vMin: Float, vMax: Float,
                     format: String = "%.3f", flags: DragFlags = 0): Boolean =
            sliderScalarN(label, DataType.Float, v, 2, vMin, vMax, format, flags)

    fun sliderVec2(label: String, v: Vec2, vMin: Float, vMax: Float,
                   format: String = "%.3f", flags: DragFlags = 0): Boolean =
            sliderScalarN(label, DataType.Float, v to _fa, 2, vMin, vMax, format, flags)
                    .also { v put _fa }

    fun sliderFloat3(label: String, v: FloatArray, vMin: Float, vMax: Float,
                     format: String = "%.3f", flags: DragFlags = 0): Boolean =
            sliderScalarN(label, DataType.Float, v, 3, vMin, vMax, format, flags)

    fun sliderVec3(label: String, v: Vec3, vMin: Float, vMax: Float,
                   format: String = "%.3f", flags: DragFlags = 0): Boolean =
            sliderScalarN(label, DataType.Float, v to _fa, 3, vMin, vMax, format, flags)
                    .also { v put _fa }

    fun sliderFloat4(label: String, v: FloatArray, vMin: Float, vMax: Float,
                     format: String = "%.3f", flags: DragFlags = 0): Boolean =
            sliderScalarN(label, DataType.Float, v, 4, vMin, vMax, format, flags)

    fun sliderVec4(label: String, v: Vec4, vMin: Float, vMax: Float,
                   format: String = "%.3f", flags: DragFlags = 0): Boolean =
            sliderScalarN(label, DataType.Float, v to _fa, 4, vMin, vMax, format, flags)
                    .also { v put _fa }

    fun sliderAngle(label: String, vRadPtr: KMutableProperty0<Float>, vDegreesMin: Float = -360f,
                    vDegreesMax: Float = 360f, format_: String = "%.0f deg", flags: DragFlags = 0): Boolean {
        val format = if (format_.isEmpty()) "%.0f deg" else format_
        var vRad by vRadPtr
        vRad = vRad.deg
        return sliderFloat(label, vRadPtr, vDegreesMin, vDegreesMax, format, flags)
                .also { vRad = vRad.rad }
    }

    fun sliderInt(label: String, v: IntArray, ptr: Int, vMin: Int, vMax: Int,
                  format: String = "%d", flags: DragFlags = 0): Boolean =
            withInt(v, ptr) { sliderInt(label, it, vMin, vMax, format, flags) }

    fun sliderInt(label: String, v: KMutableProperty0<Int>, vMin: Int, vMax: Int,
                  format: String = "%d", flags: DragFlags = 0): Boolean =
            sliderScalar(label, DataType.Int, v, vMin, vMax, format, flags)

    fun sliderInt2(label: String, v: IntArray, vMin: Int, vMax: Int,
                   format: String = "%d", flags: DragFlags = 0): Boolean =
            sliderScalarN(label, DataType.Int, v, 2, vMin, vMax, format, flags)

    fun sliderVec2i(label: String, v: Vec2i, vMin: Int, vMax: Int,
                    format: String = "%d", flags: DragFlags = 0): Boolean =
            sliderScalarN(label, DataType.Int, v to _ia, 2, vMin, vMax, format, flags)
                    .also { v put _ia }

    fun sliderInt3(label: String, v: IntArray, vMin: Int, vMax: Int,
                   format: String = "%d", flags: DragFlags = 0): Boolean =
            sliderScalarN(label, DataType.Int, v, 3, vMin, vMax, format, flags)

    fun sliderVec3i(label: String, v: Vec3i, vMin: Int, vMax: Int,
                    format: String = "%d", flags: DragFlags = 0): Boolean =
            sliderScalarN(label, DataType.Int, v to _ia, 3, vMin, vMax, format, flags)
                    .also { v put _ia }

    fun sliderInt4(label: String, v: IntArray, vMin: Int, vMax: Int,
                   format: String = "%d", flags: DragFlags = 0): Boolean =
            sliderScalarN(label, DataType.Int, v, 4, vMin, vMax, format, flags)

    fun sliderVec4i(label: String, v: Vec4i, vMin: Int, vMax: Int,
                    format: String = "%d", flags: DragFlags = 0): Boolean =
            sliderScalarN(label, DataType.Int, v to _ia, 4, vMin, vMax, format, flags)
                    .also { v put _ia }

    /** Adjust format to decorate the value with a prefix or a suffix.
     *  "%.3f"         1.234
     *  "%5.2f secs"   01.23 secs
     *  "Gold: %.0f"   Gold: 1
     *  Use power != 1.0f for non-linear sliders.
     *  adjust format to decorate the value with a prefix or a suffix for in-slider labels or unit display. Use power!=1.0 for power curve sliders
     *
     *  Note: p_data, p_min and p_max are _pointers_ to a memory address holding the data. For a slider,
     *  they are all required.
     *  Read code of e.g. SliderFloat(), SliderInt() etc. or examples in 'Demo->Widgets->Data Types' to understand
     *  how to use this function directly. */
    fun <N> sliderScalar(label: String, dataType: DataType, pData: KMutableProperty0<N>, pMin: N, pMax: N,
                         format: String? = null, flags: DragFlags = 0): Boolean
            where N : Number, N : Comparable<N> =
            sliderScalarInternal(label, dataType, pData, pMin, pMax, format, 1f, flags)

    /** Add multiple sliders on 1 line for compact edition of multiple components */
    fun <N> sliderScalarN(label: String, dataType: DataType, pData: Any, components: Int, pMin: N, pMax: N,
                          format: String? = null, flags: DragFlags = 0): Boolean
            where N : Number, N : Comparable<N> =
            sliderScalarNInternal(label, dataType, pData, components, pMin, pMax, format, 1f, flags)

    fun <N> vSliderFloat(label: String, size: Vec2, v: KMutableProperty0<N>, vMin: Float, vMax: Float,
                         format: String = "%.3f", flags: DragFlags = 0): Boolean
            where N : Number, N : Comparable<N> =
            vSliderScalar(label, size, DataType.Float, v, vMin as N, vMax as N, format, flags)

    fun <N> vSliderInt(label: String, size: Vec2, v: KMutableProperty0<N>, vMin: N, vMax: N,
                       format: String = "%d", flags: DragFlags = 0): Boolean
            where N : Number, N : Comparable<N> =
            vSliderScalar(label, size, DataType.Int, v, vMin, vMax, format, flags)

    fun <N> vSliderScalar(label: String, size: Vec2, dataType: DataType, pData: KMutableProperty0<N>, pMin: N, pMax: N,
                          format_: String? = null, flags: DragFlags = 0): Boolean
            where N : Number, N : Comparable<N> =
            vSliderScalarInternal(label, size, dataType, pData, pMin, pMax, format_, 1f, flags)
}