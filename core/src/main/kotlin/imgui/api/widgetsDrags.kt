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
import imgui.ImGui.endGroup
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.format
import imgui.ImGui.popID
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushID
import imgui.ImGui.pushMultiItemsWidths
import imgui.ImGui.sameLine
import imgui.ImGui.style
import imgui.ImGui.tempInputScalar
import imgui.ImGui.textEx
import imgui.internal.classes.Rect
import imgui.static.patchFormatStringFloatToInt
import uno.kotlin.getValue
import kotlin.reflect.KMutableProperty0

@Suppress("UNCHECKED_CAST")

/** Widgets: Drag Sliders
 *  - CTRL+Click on any drag box to turn them into an input box. Manually input values aren't clamped and can go off-bounds.
 *  - For all the Float2/Float3/Float4/Int2/Int3/Int4 versions of every functions, note that a 'float v[X]' function argument
 *      is the same as 'float* v', the array syntax is just a way to document the number of elements that are expected to be
 *      accessible. You can pass address of your first element out of a contiguous set, e.g. &myvector.x
 *  - Adjust format string to decorate the value with a prefix, a suffix, or adapt the editing and display precision
 *      e.g. "%.3f" -> 1.234; "%5.2f secs" -> 01.23 secs; "Biscuit: %.0f" -> Biscuit: 1; etc.
 *  - Format string may also be set to NULL or use the default format ("%f" or "%d").
 *  - Speed are per-pixel of mouse movement (v_speed=0.2f: mouse needs to move by 5 pixels to increase value by 1).
 *      For gamepad/keyboard navigation, minimum speed is Max(v_speed, minimum_step_at_given_precision).
 *  - Use v_min < v_max to clamp edits to given limits. Note that CTRL+Click manual input can override those limits.
 *  - We use the same sets of flags for DragXXX() and SliderXXX() functions as the features are the same and it makes it
 *      easier to swap them. */
interface widgetsDrags {

    /** If v_min >= v_max we have no bound */
    fun dragFloat(label: String, v: KMutableProperty0<Float>, vSpeed: Float = 1f, vMin: Float = 0f,
                  vMax: Float = 0f, format: String? = "%.3f", flags: SliderFlags = SliderFlag.None.i): Boolean =
            dragScalar(label, DataType.Float, v, vSpeed, vMin, vMax, format, flags)

    /** If v_min >= v_max we have no bound */
    fun dragFloat(label: String, v: FloatArray, ptr: Int, vSpeed: Float = 1f, vMin: Float = 0f,
                  vMax: Float = 0f, format: String = "%.3f", flags: SliderFlags = SliderFlag.None.i): Boolean =
            withFloat(v, ptr) { dragScalar(label, DataType.Float, it, vSpeed, vMin, vMax, format, flags) }

    fun dragFloat2(label: String, v: FloatArray, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f,
                   format: String = "%.3f", flags: SliderFlags = SliderFlag.None.i): Boolean =
            dragScalarN(label, DataType.Float, v, 2, vSpeed, vMin, vMax, format, flags)

    fun dragVec2(label: String, v: Vec2, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f,
                 format: String = "%.3f", flags: SliderFlags = SliderFlag.None.i): Boolean =
            dragScalarN(label, DataType.Float, v to _fa, 2, vSpeed, vMin, vMax, format, flags)
                    .also { v put _fa }

    fun dragFloat3(label: String, v: FloatArray, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f,
                   format: String = "%.3f", flags: SliderFlags = SliderFlag.None.i): Boolean =
            dragScalarN(label, DataType.Float, v, 3, vSpeed, vMin, vMax, format, flags)

    fun dragVec3(label: String, v: Vec3, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f,
                 format: String = "%.3f", flags: SliderFlags = SliderFlag.None.i): Boolean =
            dragScalarN(label, DataType.Float, v to _fa, 3, vSpeed, vMin, vMax, format, flags)
                    .also { v put _fa }

    fun dragFloat4(label: String, v: FloatArray, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f,
                   format: String = "%.3f", flags: SliderFlags = SliderFlag.None.i): Boolean =
            dragScalarN(label, DataType.Float, v, 4, vSpeed, vMin, vMax, format, flags)

    fun dragVec4(label: String, v: Vec4, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f,
                 format: String = "%.3f", flags: SliderFlags = SliderFlag.None.i): Boolean =
            dragScalarN(label, DataType.Float, v to _fa, 4, vSpeed, vMin, vMax, format, flags)
                    .also { v put _fa }

    /** NB: You likely want to specify the ImGuiSliderFlags_AlwaysClamp when using this. */
    fun dragFloatRange2(label: String, vCurrentMinPtr: KMutableProperty0<Float>, vCurrentMaxPtr: KMutableProperty0<Float>,
                        vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f, format: String = "%.3f",
                        formatMax: String = format, flags: SliderFlags = SliderFlag.None.i): Boolean {

        val vCurrentMin by vCurrentMinPtr
        val vCurrentMax by vCurrentMaxPtr
        val window = ImGui.currentWindow
        if (window.skipItems) return false

        ImGui.pushID(label)
        ImGui.beginGroup()
        ImGui.pushMultiItemsWidths(2, ImGui.calcItemWidth())

        val minMin = if (vMin >= vMax) -Float.MAX_VALUE else vMin
        val minMax = if (vMin >= vMax) vCurrentMax else vMax min vCurrentMax
        val minFlags = flags or if(minMin == minMax) SliderFlag._ReadOnly else SliderFlag.None
        var valueChanged = dragScalar("##min", DataType.Float, vCurrentMinPtr, vSpeed, minMin, minMax, format, minFlags)
        ImGui.popItemWidth()
        ImGui.sameLine(0f, ImGui.style.itemInnerSpacing.x)

        val maxMin = if (vMin >= vMax) vCurrentMin else vMin max vCurrentMin
        val maxMax = if (vMin >= vMax) Float.MAX_VALUE else vMax
        val maxFlags = flags or if(maxMin == maxMax) SliderFlag._ReadOnly else SliderFlag.None
        val fmt = if (formatMax.isNotEmpty()) formatMax else format
        valueChanged = dragScalar("##max", DataType.Float, vCurrentMaxPtr, vSpeed, maxMin, maxMax, fmt, maxFlags) || valueChanged
        ImGui.popItemWidth()
        ImGui.sameLine(0f, ImGui.style.itemInnerSpacing.x)

        ImGui.textEx(label, ImGui.findRenderedTextEnd(label))
        ImGui.endGroup()
        ImGui.popID()
        return valueChanged
    }

    /** If v_min >= v_max we have no bound
     *
     *  NB: vSpeed is float to allow adjusting the drag speed with more precision     */
    fun dragInt(label: String, v: IntArray, ptr: Int, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0,
                format: String = "%d", flags: SliderFlags = SliderFlag.None.i): Boolean =
            withInt(v, ptr) { dragInt(label, it, vSpeed, vMin, vMax, format, flags) }

    fun dragInt(label: String, v: KMutableProperty0<Int>, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0,
                format: String = "%d", flags: SliderFlags = SliderFlag.None.i): Boolean =
            dragScalar(label, DataType.Int, v, vSpeed, vMin, vMax, format, flags)

    fun dragInt2(label: String, v: IntArray, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0,
                 format: String = "%d", flags: SliderFlags = SliderFlag.None.i): Boolean =
            dragScalarN(label, DataType.Int, v, 2, vSpeed, vMin, vMax, format, flags)

    fun dragVec2i(label: String, v: Vec2i, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0,
                  format: String = "%d", flags: SliderFlags = SliderFlag.None.i): Boolean =
            dragScalarN(label, DataType.Int, v to _ia, 2, vSpeed, vMin, vMax, format, flags)
                    .also { v put _ia }

    fun dragInt3(label: String, v: IntArray, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0,
                 format: String = "%d", flags: SliderFlags = SliderFlag.None.i): Boolean =
            dragScalarN(label, DataType.Int, v, 3, vSpeed, vMin, vMax, format, flags)

    fun dragVec3i(label: String, v: Vec3i, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0,
                  format: String = "%d", flags: SliderFlags = SliderFlag.None.i): Boolean =
            dragScalarN(label, DataType.Int, v to _ia, 3, vSpeed, vMin, vMax, format, flags)
                    .also { v put _ia }

    fun dragInt4(label: String, v: IntArray, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0,
                 format: String = "%d", flags: SliderFlags = SliderFlag.None.i): Boolean =
            dragScalarN(label, DataType.Int, v, 4, vSpeed, vMin, vMax, format, flags)

    fun dragVec4i(label: String, v: Vec4i, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0,
                  format: String = "%d", flags: SliderFlags = SliderFlag.None.i): Boolean =
            dragScalarN(label, DataType.Int, v to _ia, 4, vSpeed, vMin, vMax, format, flags)
                    .also { v put _ia }

    /** NB: You likely want to specify the ImGuiSliderFlags_AlwaysClamp when using this. */
    fun dragIntRange2(label: String, vCurrentMinPtr: KMutableProperty0<Int>, vCurrentMaxPtr: KMutableProperty0<Int>,
                      vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0, format: String = "%d",
                      formatMax: String = format, flags: SliderFlags = SliderFlag.None.i): Boolean {

        val vCurrentMin by vCurrentMinPtr
        val vCurrentMax by vCurrentMaxPtr
        val window = currentWindow
        if (window.skipItems) return false

        pushID(label)
        beginGroup()
        pushMultiItemsWidths(2, calcItemWidth())

        val minMin = if (vMin >= vMax) Int.MIN_VALUE else vMin
        val minMax = if (vMin >= vMax) vCurrentMax else vMax min vCurrentMax
        val minFlags = flags or if(minMin == minMax) SliderFlag._ReadOnly else SliderFlag.None
        var valueChanged = dragInt("##min", vCurrentMinPtr, vSpeed, minMin, minMax, format, minFlags)
        popItemWidth()
        sameLine(0f, style.itemInnerSpacing.x)

        val maxMin = if (vMin >= vMax) vCurrentMin else vMin max vCurrentMin
        val maxMax = if (vMin >= vMax) Int.MAX_VALUE else vMax
        val maxFlags = flags or if(maxMin == maxMax) SliderFlag._ReadOnly else SliderFlag.None
        val fmt = if (formatMax.isNotEmpty()) formatMax else format
        valueChanged = dragInt("##max", vCurrentMaxPtr, vSpeed, maxMin, maxMax, fmt, maxFlags) || valueChanged
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
                   format: String? = null, flags: SliderFlags = SliderFlag.None.i): Boolean =
            dragScalar(label, pData, 0, vSpeed, pMin, pMax, format, flags)

    /** If vMin >= vMax we have no bound  */
    fun dragScalar(label: String, pData: FloatArray, ptr: Int = 0, vSpeed: Float, pMin: Float? = null,
                   pMax: Float? = null, format: String? = null, flags: SliderFlags = SliderFlag.None.i): Boolean =
            withFloat(pData, ptr) {
                dragScalar(label, DataType.Float, it, vSpeed, pMin, pMax, format, flags)
            }

    /** ote: p_data, p_min and p_max are _pointers_ to a memory address holding the data. For a Drag widget, p_min and p_max are optional.
     *  Read code of e.g. DragFloat(), DragInt() etc. or examples in 'Demo->Widgets->Data Types' to understand how to use this function directly. */
    fun <N> dragScalar(label: String, dataType: DataType, pData: KMutableProperty0<N>, vSpeed: Float,
                       pMin: N? = null, pMax: N? = null, format_: String? = null, flags: SliderFlags = SliderFlag.None.i): Boolean
            where N : Number, N : Comparable<N> {

        val window = ImGui.currentWindow
        if (window.skipItems) return false

        val id = window.getID(label)
        val w = ImGui.calcItemWidth()
        val labelSize = ImGui.calcTextSize(label, hideTextAfterDoubleHash = true)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w, labelSize.y + style.framePadding.y * 2f))
        val totalBb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))

        ImGui.itemSize(totalBb, ImGui.style.framePadding.y)
        if (!ImGui.itemAdd(totalBb, id, frameBb))
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
        val hovered = ImGui.itemHoverable(frameBb, id)
        val tempInputAllowed = flags hasnt SliderFlag.NoInput
        var tempInputIsActive = tempInputAllowed && ImGui.tempInputIsActive(id)
        if (!tempInputIsActive) {
            val focusRequested = tempInputAllowed && ImGui.focusableItemRegister(window, id)
            val clicked = hovered && ImGui.io.mouseClicked[0]
            val doubleClicked = hovered && ImGui.io.mouseDoubleClicked[0]
            if (focusRequested || clicked || doubleClicked || g.navActivateId == id || g.navInputId == id) {
                ImGui.setActiveID(id, window)
                ImGui.setFocusID(id, window)
                ImGui.focusWindow(window)
                g.activeIdUsingNavDirMask = (1 shl Dir.Left) or (1 shl Dir.Right)
                if (tempInputAllowed && (focusRequested || (clicked && ImGui.io.keyCtrl) || doubleClicked || g.navInputId == id)) {
                    tempInputIsActive = true
                    ImGui.focusableItemUnregister(window)
                }
            }
        }

        if (tempInputIsActive) {
            // Only clamp CTRL+Click input when ImGuiSliderFlags_AlwaysClamp is set
            val isClampInput = flags hasnt SliderFlag.AlwaysClamp  && (pMin == null || pMax == null || pMin < pMax)
            return tempInputScalar(frameBb, id, label, dataType, pData, format, pMin.takeIf { isClampInput },
                pMax.takeIf { isClampInput })
        }

        // Draw frame
        val frameCol = if (g.activeId == id) Col.FrameBgActive else if (g.hoveredId == id) Col.FrameBgHovered else Col.FrameBg
        ImGui.renderNavHighlight(frameBb, id)
        ImGui.renderFrame(frameBb.min, frameBb.max, frameCol.u32, true, ImGui.style.frameRounding)

        // Drag behavior
        val valueChanged = ImGui.dragBehavior(id, dataType, pData, vSpeed, pMin, pMax, format, flags)
        if (valueChanged)
            ImGui.markItemEdited(id)

        // Display value using user-provided display format so user can add prefix/suffix/decorations to the value.
        val value = pData.format(dataType, format)
        ImGui.renderTextClipped(frameBb.min, frameBb.max, value, null, Vec2(0.5f))

        if (labelSize.x > 0f)
            ImGui.renderText(Vec2(frameBb.max.x + ImGui.style.itemInnerSpacing.x, frameBb.min.y + ImGui.style.framePadding.y), label)

        Hook.itemInfo?.invoke(g, id, label, window.dc.itemFlags)
        return valueChanged
    }

    /** Note: p_data, p_min and p_max are _pointers_ to a memory address holding the data. For a Drag widget,
     *  p_min and p_max are optional.
     *  Read code of e.g. SliderFloat(), SliderInt() etc. or examples in 'Demo->Widgets->Data Types' to understand
     *  how to use this function directly. */
    fun <N> dragScalarN(label: String, dataType: DataType, v: Any, components: Int, vSpeed: Float,
                        vMin: N? = null, vMax: N? = null, format: String? = null, flags: SliderFlags = SliderFlag.None.i): Boolean
            where N : Number, N : Comparable<N> {

        val window = ImGui.currentWindow
        if (window.skipItems) return false

        var valueChanged = false
        ImGui.beginGroup()
        ImGui.pushID(label)
        ImGui.pushMultiItemsWidths(components, ImGui.calcItemWidth())
        for (i in 0 until components) {
            ImGui.pushID(i)
            if (i > 0)
                ImGui.sameLine(0f, ImGui.style.itemInnerSpacing.x)
            when (dataType) {
                DataType.Int -> withInt(v as IntArray, i) {
                    valueChanged = dragScalar("", dataType, it as KMutableProperty0<N>, vSpeed, vMin, vMax, format, flags) or valueChanged
                }
                DataType.Float -> withFloat(v as FloatArray, i) {
                    valueChanged = dragScalar("", dataType, it as KMutableProperty0<N>, vSpeed, vMin, vMax, format, flags) or valueChanged
                }
                else -> error("invalid")
            }
            ImGui.popID()
            ImGui.popItemWidth()
        }
        ImGui.popID()

        val labelEnd = ImGui.findRenderedTextEnd(label)
        if (0 != labelEnd) {
            ImGui.sameLine(0f, ImGui.style.itemInnerSpacing.x)
            ImGui.textEx(label, labelEnd)
        }

        ImGui.endGroup()
        return valueChanged
    }
}