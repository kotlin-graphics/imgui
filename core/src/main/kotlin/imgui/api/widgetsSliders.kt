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
import imgui.ImGui.format
import imgui.ImGui.tempInputScalar
import imgui.internal.classes.Rect
import imgui.static.patchFormatStringFloatToInt
import kool.getValue
import kool.setValue
import unsigned.Ubyte
import unsigned.Uint
import unsigned.Ulong
import unsigned.Ushort
import kotlin.reflect.KMutableProperty0

@Suppress("UNCHECKED_CAST")

/** Widgets: Sliders
 *  - CTRL+Click on any slider to turn them into an input box. Manually input values aren't clamped and can go off-bounds.
 *  - Adjust format string to decorate the value with a prefix, a suffix, or adapt the editing and display precision
 *  - Format string may also be set to NULL or use the default format ("%f" or "%d").
 *      e.g. "%.3f" -> 1.234; "%5.2f secs" -> 01.23 secs; "Biscuit: %.0f" -> Biscuit: 1; etc.   */
interface widgetsSliders {


    /** Adjust format to decorate the value with a prefix or a suffix.
     *  "%.3f"         1.234
     *  "%5.2f secs"   01.23 secs
     *  "Gold: %.0f"   Gold: 1
     *  Use power != 1.0f for non-linear sliders.
     *  adjust format to decorate the value with a prefix or a suffix for in-slider labels or unit display. Use power!=1.0 for power curve sliders */
    fun sliderFloat(
        label: String, v: FloatArray, ptr: Int, vMin: Float, vMax: Float,
        format: String = "%.3f", flags: SliderFlags = SliderFlag.None.i
    ): Boolean =
        withFloat(v, ptr) { sliderFloat(label, it, vMin, vMax, format, flags) }

    /** Adjust format to decorate the value with a prefix or a suffix.
     *  "%.3f"         1.234
     *  "%5.2f secs"   01.23 secs
     *  "Gold: %.0f"   Gold: 1
     *  Use power != 1.0f for non-linear sliders.
     *  adjust format to decorate the value with a prefix or a suffix for in-slider labels or unit display. Use power!=1.0 for power curve sliders */
    fun sliderFloat(
        label: String, v: KMutableProperty0<Float>, vMin: Float, vMax: Float,
        format: String = "%.3f", flags: SliderFlags = SliderFlag.None.i
    ): Boolean =
        sliderScalar(label, DataType.Float, v, vMin, vMax, format, flags)

    fun sliderFloat2(
        label: String, v: FloatArray, vMin: Float, vMax: Float,
        format: String = "%.3f", flags: SliderFlags = SliderFlag.None.i
    ): Boolean =
        sliderScalarN(label, DataType.Float, v, 2, vMin, vMax, format, flags)

    fun sliderVec2(
        label: String, v: Vec2, vMin: Float, vMax: Float,
        format: String = "%.3f", flags: SliderFlags = SliderFlag.None.i
    ): Boolean =
        sliderScalarN(label, DataType.Float, v to _fa, 2, vMin, vMax, format, flags)
            .also { v put _fa }

    fun sliderFloat3(
        label: String, v: FloatArray, vMin: Float, vMax: Float,
        format: String = "%.3f", flags: SliderFlags = SliderFlag.None.i
    ): Boolean =
        sliderScalarN(label, DataType.Float, v, 3, vMin, vMax, format, flags)

    fun sliderVec3(
        label: String, v: Vec3, vMin: Float, vMax: Float,
        format: String = "%.3f", flags: SliderFlags = SliderFlag.None.i
    ): Boolean =
        sliderScalarN(label, DataType.Float, v to _fa, 3, vMin, vMax, format, flags)
            .also { v put _fa }

    fun sliderFloat4(
        label: String, v: FloatArray, vMin: Float, vMax: Float,
        format: String = "%.3f", flags: SliderFlags = SliderFlag.None.i
    ): Boolean =
        sliderScalarN(label, DataType.Float, v, 4, vMin, vMax, format, flags)

    fun sliderVec4(
        label: String, v: Vec4, vMin: Float, vMax: Float,
        format: String = "%.3f", flags: SliderFlags = SliderFlag.None.i
    ): Boolean =
        sliderScalarN(label, DataType.Float, v to _fa, 4, vMin, vMax, format, flags)
            .also { v put _fa }

    fun sliderAngle(
        label: String, vRadPtr: KMutableProperty0<Float>, vDegreesMin: Float = -360f, vDegreesMax: Float = 360f,
        format_: String = "%.0f deg", flags: SliderFlags = SliderFlag.None.i
    ): Boolean {
        val format = if (format_.isEmpty()) "%.0f deg" else format_
        var vRad by vRadPtr
        vRad = vRad.deg
        return sliderFloat(label, vRadPtr, vDegreesMin, vDegreesMax, format, flags)
            .also { vRad = vRad.rad }
    }

    fun sliderInt(
        label: String, v: IntArray, ptr: Int, vMin: Int, vMax: Int,
        format: String = "%d", flags: SliderFlags = SliderFlag.None.i
    ): Boolean =
        withInt(v, ptr) { sliderInt(label, it, vMin, vMax, format, flags) }

    fun sliderInt(
        label: String, v: KMutableProperty0<Int>, vMin: Int, vMax: Int,
        format: String = "%d", flags: SliderFlags = SliderFlag.None.i
    ): Boolean =
        sliderScalar(label, DataType.Int, v, vMin, vMax, format, flags)

    fun sliderInt2(
        label: String, v: IntArray, vMin: Int, vMax: Int,
        format: String = "%d", flags: SliderFlags = SliderFlag.None.i
    ): Boolean =
        sliderScalarN(label, DataType.Int, v, 2, vMin, vMax, format, flags)

    fun sliderVec2i(
        label: String, v: Vec2i, vMin: Int, vMax: Int,
        format: String = "%d", flags: SliderFlags = SliderFlag.None.i
    ): Boolean =
        sliderScalarN(label, DataType.Int, v to _ia, 2, vMin, vMax, format, flags)
            .also { v put _ia }

    fun sliderInt3(
        label: String, v: IntArray, vMin: Int, vMax: Int,
        format: String = "%d", flags: SliderFlags = SliderFlag.None.i
    ): Boolean =
        sliderScalarN(label, DataType.Int, v, 3, vMin, vMax, format, flags)

    fun sliderVec3i(
        label: String, v: Vec3i, vMin: Int, vMax: Int,
        format: String = "%d", flags: SliderFlags = SliderFlag.None.i
    ): Boolean =
        sliderScalarN(label, DataType.Int, v to _ia, 3, vMin, vMax, format, flags)
            .also { v put _ia }

    fun sliderInt4(
        label: String, v: IntArray, vMin: Int, vMax: Int,
        format: String = "%d", flags: SliderFlags = SliderFlag.None.i
    ): Boolean =
        sliderScalarN(label, DataType.Int, v, 4, vMin, vMax, format, flags)

    fun sliderVec4i(
        label: String, v: Vec4i, vMin: Int, vMax: Int,
        format: String = "%d", flags: SliderFlags = SliderFlag.None.i
    ): Boolean =
        sliderScalarN(label, DataType.Int, v to _ia, 4, vMin, vMax, format, flags)
            .also { v put _ia }

    /** Adjust format to decorate the value with a prefix or a suffix.
     *  "%.3f"         1.234
     *  "%5.2f secs"   01.23 secs
     *  "Gold: %.0f"   Gold: 1
     *  Use power != 1.0f for non-linear sliders.
     *  adjust format to decorate the value with a prefix or a suffix for in-slider labels or unit display. Use power!=1.0 for power curve sliders
     *
     *  Note: p_data, p_min and p_max are _pointers_ to a memory address holding the data. For a slider, they are all required.
     *  Read code of e.g. SliderFloat(), SliderInt() etc. or examples in 'Demo->Widgets->Data Types' to understand how to use this function directly. */
    fun <N> sliderScalar(
        label: String, dataType: DataType, pData: KMutableProperty0<N>,
        pMin: N? = null, pMax: N? = null, format_: String? = null, flags: SliderFlags = 0
    ): Boolean
            where N : Number, N : Comparable<N> {

        val window = ImGui.currentWindow
        if (window.skipItems) return false

        val id = window.getID(label)
        val w = ImGui.calcItemWidth()

        val labelSize = ImGui.calcTextSize(label, hideTextAfterDoubleHash = true)
        val frameBb =
            Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w, labelSize.y + ImGui.style.framePadding.y * 2f))
        val totalBb = Rect(
            frameBb.min,
            frameBb.max + Vec2(if (labelSize.x > 0f) ImGui.style.itemInnerSpacing.x + labelSize.x else 0f, 0f)
        )

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

        // Tabbing or CTRL-clicking on Slider turns it into an input box
        val hovered = ImGui.itemHoverable(frameBb, id)
        val tempInputAllowed = flags hasnt SliderFlag.NoInput
        var tempInputIsActive = tempInputAllowed && ImGui.tempInputIsActive(id)
        if (!tempInputIsActive) {
            val focusRequested = tempInputAllowed && ImGui.focusableItemRegister(window, id)
            val clicked = hovered && ImGui.io.mouseClicked[0]
            if (focusRequested || clicked || g.navActivateId == id || g.navInputId == id) {
                ImGui.setActiveID(id, window)
                ImGui.setFocusID(id, window)
                ImGui.focusWindow(window)
                g.activeIdUsingNavDirMask = g.activeIdUsingNavDirMask or ((1 shl Dir.Left) or (1 shl Dir.Right))
                if (tempInputAllowed && (focusRequested || (clicked && ImGui.io.keyCtrl) || g.navInputId == id)) {
                    tempInputIsActive = true
                    ImGui.focusableItemUnregister(window)
                }
            }
        }

        if (tempInputIsActive) {
            // Only clamp CTRL+Click input when ImGuiSliderFlags_AlwaysClamp is set
            val isClampInput = flags hasnt SliderFlag.AlwaysClamp
            return tempInputScalar(frameBb, id, label, dataType, pData, format, pMin.takeIf { isClampInput },
                pMax.takeIf { isClampInput })
        }

        // Draw frame
        val frameCol =
            if (g.activeId == id) Col.FrameBgActive else if (g.hoveredId == id) Col.FrameBgHovered else Col.FrameBg
        ImGui.renderNavHighlight(frameBb, id)
        ImGui.renderFrame(frameBb.min, frameBb.max, frameCol.u32, true, ImGui.style.frameRounding)

        // Slider behavior
        val grabBb = Rect()
        val valueChanged = ImGui.sliderBehavior(frameBb, id, dataType, pData, pMin!!, pMax!!, format, flags, grabBb)
        if (valueChanged)
            ImGui.markItemEdited(id)

        // Render grab
        if (grabBb.max.x > grabBb.min.x)
            window.drawList.addRectFilled(
                grabBb.min,
                grabBb.max,
                ImGui.getColorU32(if (g.activeId == id) Col.SliderGrabActive else Col.SliderGrab),
                ImGui.style.grabRounding
            )

        // Display value using user-provided display format so user can add prefix/suffix/decorations to the value.
        val value = format.format(
            ImGui.style.locale, when (val data = pData()) {
                is Ubyte -> data.v
                is Ushort -> data.v
                is Uint -> data.v
                is Ulong -> data.v
                else -> data
            }
        )
        ImGui.renderTextClipped(frameBb.min, frameBb.max, value, null, Vec2(0.5f))

        if (labelSize.x > 0f)
            ImGui.renderText(
                Vec2(
                    frameBb.max.x + ImGui.style.itemInnerSpacing.x,
                    frameBb.min.y + ImGui.style.framePadding.y
                ), label
            )

        Hook.itemInfo?.invoke(g, id, label, window.dc.itemFlags)
        return valueChanged
    }

    /** Add multiple sliders on 1 line for compact edition of multiple components */
    fun <N> sliderScalarN(
        label: String, dataType: DataType, pData: Any, components: Int,
        pMin: N? = null, pMax: N? = null, format: String? = null, flags: SliderFlags = 0
    ): Boolean
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
            valueChanged = when (dataType) {
                DataType.Int -> withInt(pData as IntArray, i) {
                    sliderScalar("", dataType, it as KMutableProperty0<N>, pMin, pMax, format, flags)
                }
                DataType.Float -> withFloat(pData as FloatArray, i) {
                    sliderScalar("", dataType, it as KMutableProperty0<N>, pMin, pMax, format, flags)
                }
                else -> error("invalid")
            } || valueChanged
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

    fun <N> vSliderFloat(
        label: String, size: Vec2, v: KMutableProperty0<N>, vMin: Float, vMax: Float,
        format: String = "%.3f", flags: SliderFlags = SliderFlag.None.i
    ): Boolean
            where N : Number, N : Comparable<N> =
        vSliderScalar(label, size, DataType.Float, v, vMin as N, vMax as N, format, flags)

    fun <N> vSliderInt(
        label: String, size: Vec2, v: KMutableProperty0<N>, vMin: N, vMax: N,
        format: String = "%d", flags: SliderFlags = SliderFlag.None.i
    ): Boolean
            where N : Number, N : Comparable<N> =
        vSliderScalar(label, size, DataType.Int, v, vMin, vMax, format, flags)

    /** Internal implementation */
    fun <N> vSliderScalar(
        label: String, size: Vec2, dataType: DataType, pData: KMutableProperty0<N>,
        pMin: N? = null, pMax: N? = null, format_: String? = null, flags: SliderFlags = 0
    ): Boolean
            where N : Number, N : Comparable<N> {

        val window = ImGui.currentWindow
        if (window.skipItems) return false

        val id = window.getID(label)

        val labelSize = ImGui.calcTextSize(label, hideTextAfterDoubleHash = true)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        val bb = Rect(
            frameBb.min,
            frameBb.max + Vec2(if (labelSize.x > 0f) ImGui.style.itemInnerSpacing.x + labelSize.x else 0f, 0f)
        )

        ImGui.itemSize(bb, ImGui.style.framePadding.y)
        if (!ImGui.itemAdd(frameBb, id)) return false

        // Default format string when passing NULL
        val format = when {
            format_ == null -> when (dataType) {
                DataType.Float, DataType.Double -> "%f"
                else -> "%d" // (FIXME-LEGACY: Patch old "%.0f" format string to use "%d", read function more details.)
            }
            dataType == DataType.Int && format_ != "%d" -> patchFormatStringFloatToInt(format_)
            else -> format_
        }
        val hovered = ImGui.itemHoverable(frameBb, id)
        if ((hovered && ImGui.io.mouseClicked[0]) || g.navActivateId == id || g.navInputId == id) {
            ImGui.setActiveID(id, window)
            ImGui.setFocusID(id, window)
            ImGui.focusWindow(window)
            g.activeIdUsingNavDirMask = g.activeIdUsingNavDirMask or ((1 shl Dir.Up) or (1 shl Dir.Down))
        }

        // Draw frame
        val frameCol =
            if (g.activeId == id) Col.FrameBgActive else if (g.hoveredId == id) Col.FrameBgHovered else Col.FrameBg
        ImGui.renderNavHighlight(frameBb, id)
        ImGui.renderFrame(frameBb.min, frameBb.max, frameCol.u32, true, ImGui.style.frameRounding)
        // Slider behavior
        val grabBb = Rect()
        val valueChanged = ImGui.sliderBehavior(
            frameBb,
            id,
            dataType,
            pData,
            pMin!!,
            pMax!!,
            format,
            flags or SliderFlag._Vertical.i,
            grabBb
        )

        if (valueChanged)
            ImGui.markItemEdited(id)

        // Render grab
        if (grabBb.max.y > grabBb.min.y)
            window.drawList.addRectFilled(
                grabBb.min,
                grabBb.max,
                ImGui.getColorU32(if (g.activeId == id) Col.SliderGrabActive else Col.SliderGrab),
                ImGui.style.grabRounding
            )

        /*  Display value using user-provided display format so user can add prefix/suffix/decorations to the value.
            For the vertical slider we allow centered text to overlap the frame padding         */
        val value = pData.format(dataType, format)
        val posMin = Vec2(frameBb.min.x, frameBb.min.y + ImGui.style.framePadding.y)
        ImGui.renderTextClipped(posMin, frameBb.max, value, null, Vec2(0.5f, 0f))
        if (labelSize.x > 0f)
            ImGui.renderText(
                Vec2(
                    frameBb.max.x + ImGui.style.itemInnerSpacing.x,
                    frameBb.min.y + ImGui.style.framePadding.y
                ), label
            )

        return valueChanged
    }
}