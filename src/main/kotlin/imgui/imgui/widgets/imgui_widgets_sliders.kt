package imgui.imgui.widgets

import glm_.func.deg
import glm_.func.rad
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
import imgui.ImGui.endGroup
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.focusableItemRegister
import imgui.ImGui.focusableItemUnregister
import imgui.ImGui.format
import imgui.ImGui.getColorU32
import imgui.ImGui.io
import imgui.ImGui.itemAdd
import imgui.ImGui.itemHoverable
import imgui.ImGui.itemSize
import imgui.ImGui.markItemEdited
import imgui.ImGui.nextItemWidth
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
import imgui.ImGui.sliderBehavior
import imgui.ImGui.style
import imgui.ImGui.tempInputTextIsActive
import imgui.ImGui.tempInputTextScalar
import imgui.ImGui.textEx
import imgui.imgui.Context.Companion._fa
import imgui.imgui.Context.Companion._ia
import imgui.imgui.g
import imgui.imgui.widgets.imgui_widgets_drags.Companion.patchFormatStringFloatToInt
import imgui.imgui.withFloat
import imgui.imgui.withInt
import imgui.internal.Rect
import imgui.internal.SliderFlag
import imgui.internal.focus
import kotlin.reflect.KMutableProperty0

/** Widgets: Sliders
 *  - CTRL+Click on any slider to turn them into an input box. Manually input values aren't clamped and can go off-bounds.
 *  - Adjust format string to decorate the value with a prefix, a suffix, or adapt the editing and display precision
 *      e.g. "%.3f" -> 1.234; "%5.2f secs" -> 01.23 secs; "Biscuit: %.0f" -> Biscuit: 1; etc.   */
interface imgui_widgets_sliders {


    /** Adjust format to decorate the value with a prefix or a suffix.
     *  "%.3f"         1.234
     *  "%5.2f secs"   01.23 secs
     *  "Gold: %.0f"   Gold: 1
     *  Use power != 1.0f for non-linear sliders.
     *  adjust format to decorate the value with a prefix or a suffix for in-slider labels or unit display. Use power!=1.0 for power curve sliders */
    fun sliderFloat(label: String, v: FloatArray, ptr: Int, vMin: Float, vMax: Float, format: String = "%.3f", power: Float = 1f): Boolean =
            withFloat(v, ptr) { sliderFloat(label, it, vMin, vMax, format, power) }

    /** Adjust format to decorate the value with a prefix or a suffix.
     *  "%.3f"         1.234
     *  "%5.2f secs"   01.23 secs
     *  "Gold: %.0f"   Gold: 1
     *  Use power != 1.0f for non-linear sliders.
     *  adjust format to decorate the value with a prefix or a suffix for in-slider labels or unit display. Use power!=1.0 for power curve sliders */
    fun sliderFloat(label: String, v: KMutableProperty0<Float>, vMin: Float, vMax: Float, format: String = "%.3f", power: Float = 1f): Boolean =
            sliderScalar(label, DataType.Float, v, vMin, vMax, format, power)

    fun sliderFloat2(label: String, v: FloatArray, vMin: Float, vMax: Float, format: String = "%.3f", power: Float = 1f): Boolean =
            sliderScalarN(label, DataType.Float, v, 2, vMin, vMax, format, power)

    fun sliderVec2(label: String, v: Vec2, vMin: Float, vMax: Float, format: String = "%.3f", power: Float = 1f): Boolean =
            sliderScalarN(label, DataType.Float, v to _fa, 2, vMin, vMax, format, power)
                    .also { v put _fa }

    fun sliderFloat3(label: String, v: FloatArray, vMin: Float, vMax: Float, format: String = "%.3f", power: Float = 1f) =
            sliderScalarN(label, DataType.Float, v, 3, vMin, vMax, format, power)

    fun sliderVec3(label: String, v: Vec3, vMin: Float, vMax: Float, format: String = "%.3f", power: Float = 1f): Boolean =
            sliderScalarN(label, DataType.Float, v to _fa, 3, vMin, vMax, format, power)
                    .also { v put _fa }

    fun sliderFloat4(label: String, v: FloatArray, vMin: Float, vMax: Float, format: String = "%.3f", power: Float = 1f) =
            sliderScalarN(label, DataType.Float, v, 4, vMin, vMax, format, power)

    fun sliderVec4(label: String, v: Vec4, vMin: Float, vMax: Float, format: String = "%.3f", power: Float = 1f): Boolean =
            sliderScalarN(label, DataType.Float, v to _fa, 4, vMin, vMax, format, power)
                    .also { v put _fa }

    fun sliderAngle(label: String, vRadPtr: KMutableProperty0<Float>, vDegreesMin: Float = -360f,
                    vDegreesMax: Float = 360f, format_: String = "%.0f deg"): Boolean {
        val format = if (format_.isEmpty()) "%.0f deg" else format_
        var vRad by vRadPtr
        vRad = vRad.deg
        return sliderFloat(label, vRadPtr, vDegreesMin, vDegreesMax, format, 1f)
                .also { vRad = vRad.rad }
    }

    fun sliderInt(label: String, v: IntArray, ptr: Int, vMin: Int, vMax: Int, format: String = "%d"): Boolean =
            withInt(v, ptr) { sliderInt(label, it, vMin, vMax, format) }

    fun sliderInt(label: String, v: KMutableProperty0<Int>, vMin: Int, vMax: Int, format: String = "%d"): Boolean =
            sliderScalar(label, DataType.Int, v, vMin, vMax, format)

    fun sliderInt2(label: String, v: IntArray, vMin: Int, vMax: Int, format: String = "%d"): Boolean =
            sliderScalarN(label, DataType.Int, v, 2, vMin, vMax, format)

    fun sliderVec2i(label: String, v: Vec2i, vMin: Int, vMax: Int, format: String = "%d"): Boolean =
            sliderScalarN(label, DataType.Int, v to _ia, 2, vMin, vMax, format)
                    .also { v put _ia }

    fun sliderInt3(label: String, v: IntArray, vMin: Int, vMax: Int, format: String = "%d"): Boolean =
            sliderScalarN(label, DataType.Int, v, 3, vMin, vMax, format)

    fun sliderVec3i(label: String, v: Vec3i, vMin: Int, vMax: Int, format: String = "%d"): Boolean =
            sliderScalarN(label, DataType.Int, v to _ia, 3, vMin, vMax, format)
                    .also { v put _ia }

    fun sliderInt4(label: String, v: IntArray, vMin: Int, vMax: Int, format: String = "%d"): Boolean =
            sliderScalarN(label, DataType.Int, v, 4, vMin, vMax, format)

    fun sliderVec4i(label: String, v: Vec4i, vMin: Int, vMax: Int, format: String = "%d"): Boolean =
            sliderScalarN(label, DataType.Int, v to _ia, 4, vMin, vMax, format)
                    .also { v put _ia }

    /** Adjust format to decorate the value with a prefix or a suffix.
     *  "%.3f"         1.234
     *  "%5.2f secs"   01.23 secs
     *  "Gold: %.0f"   Gold: 1
     *  Use power != 1.0f for non-linear sliders.
     *  adjust format to decorate the value with a prefix or a suffix for in-slider labels or unit display. Use power!=1.0 for power curve sliders */
    fun <N> sliderScalar(label: String, dataType: DataType, v: KMutableProperty0<N>, vMin: N, vMax: N,
                         format_: String? = null, power: Float = 1f): Boolean where N : Number, N : Comparable<N> {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(label)
        val w = nextItemWidth

        val labelSize = calcTextSize(label, 0, true)
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
        // Tabbing or CTRL-clicking on Slider turns it into an input box
        val hovered = itemHoverable(frameBb, id)
        val tempInputIsActive = tempInputTextIsActive(id)
        var tempInputStart = false
        if (!tempInputIsActive) {
            val focusRequested = focusableItemRegister(window, id)
            val clicked = hovered && io.mouseClicked[0]
            if (focusRequested || clicked || g.navActivateId == id || g.navInputId == id) {
                setActiveId(id, window)
                setFocusId(id, window)
                window.focus()
                g.activeIdAllowNavDirFlags = (1 shl Dir.Up) or (1 shl Dir.Down)
                if (focusRequested || (clicked && io.keyCtrl) || g.navInputId == id) {
                    tempInputStart = true
                    focusableItemUnregister(window)
                }
            }
        }

        if (tempInputIsActive || tempInputStart)
            return tempInputTextScalar(frameBb, id, label, DataType.Float, v, format)

        // Draw frame
        val frameCol = if (g.activeId == id) Col.FrameBgActive else if (g.hoveredId == id) Col.FrameBgHovered else Col.FrameBg
        renderNavHighlight(frameBb, id)
        renderFrame(frameBb.min, frameBb.max, frameCol.u32, true, style.frameRounding)
        // Slider behavior
        val grabBb = Rect()
        val valueChanged = sliderBehavior(frameBb, id, dataType, v, vMin, vMax, format, power, SliderFlag.None.i, grabBb)

        if (valueChanged)
            markItemEdited(id)

        // Render grab
        if (grabBb.max.x > grabBb.min.x)
            window.drawList.addRectFilled(grabBb.min, grabBb.max, getColorU32(if (g.activeId == id) Col.SliderGrabActive else Col.SliderGrab), style.grabRounding)

        // Display value using user-provided display format so user can add prefix/suffix/decorations to the value.
        val value = format.format(style.locale, v())
        renderTextClipped(frameBb.min, frameBb.max, value, value.length, null, Vec2(0.5f))

        if (labelSize.x > 0f)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, frameBb.min.y + style.framePadding.y), label)

        ImGuiTestEngineHook_ItemInfo(id, label, window.dc.itemFlags)
        return valueChanged
    }

    /** Add multiple sliders on 1 line for compact edition of multiple components */
    fun <N> sliderScalarN(label: String, dataType: DataType, v: Any, components: Int, vMin: N, vMax: N,
                          format: String? = null, power: Float = 1f): Boolean where N : Number, N : Comparable<N> {

        val window = currentWindow
        if (window.skipItems) return false

        var valueChanged = false
        beginGroup()
        pushId(label)
        pushMultiItemsWidths(components, nextItemWidth)
        for (i in 0 until components) {
            pushId(i)
            valueChanged = when (dataType) {
                DataType.Int -> withInt(v as IntArray, i) { sliderScalar("", dataType, it as KMutableProperty0<N>, vMin, vMax, format, power) }
                DataType.Float -> withFloat(v as FloatArray, i) { sliderScalar("", dataType, it as KMutableProperty0<N>, vMin, vMax, format, power) }
                else -> error("invalid")
            } || valueChanged
            sameLine(0f, style.itemInnerSpacing.x)
            popId()
            popItemWidth()
        }
        popId()

        textEx(label, findRenderedTextEnd(label))
        endGroup()
        return valueChanged
    }

    fun <N> vSliderFloat(label: String, size: Vec2, v: KMutableProperty0<N>, vMin: Float, vMax: Float,
                         format: String = "%.3f", power: Float = 1f): Boolean where N : Number, N : Comparable<N> =
            vSliderScalar(label, size, DataType.Float, v, vMin as N, vMax as N, format, power)

    fun <N> vSliderInt(label: String, size: Vec2, v: KMutableProperty0<N>, vMin: N, vMax: N,
                       format: String = "%d"): Boolean where N : Number, N : Comparable<N> =
            vSliderScalar(label, size, DataType.Int, v, vMin, vMax, format)

    fun <N> vSliderScalar(label: String, size: Vec2, dataType: DataType, v: KMutableProperty0<N>, vMin: N, vMax: N,
                          format_: String? = null, power: Float = 1f): Boolean where N : Number, N : Comparable<N> {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(label)

        val labelSize = calcTextSize(label, 0, true)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        val bb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))

        itemSize(bb, style.framePadding.y)
        if (!itemAdd(frameBb, id)) return false

        // Default format string when passing NULL
        val format = when {
            format_ == null -> when (dataType) {
                DataType.Float, DataType.Double -> "%f"
                else -> "%d" // (FIXME-LEGACY: Patch old "%.0f" format string to use "%d", read function more details.)
            }
            dataType == DataType.Int && format_ != "%d" -> patchFormatStringFloatToInt(format_)
            else -> format_
        }
        val hovered = itemHoverable(frameBb, id)
        if ((hovered && io.mouseClicked[0]) || g.navActivateId == id || g.navInputId == id) {
            setActiveId(id, window)
            setFocusId(id, window)
            window.focus()
            g.activeIdAllowNavDirFlags = (1 shl Dir.Left) or (1 shl Dir.Right)
        }

        // Draw frame
        val frameCol = if (g.activeId == id) Col.FrameBgActive else if (g.hoveredId == id) Col.FrameBgHovered else Col.FrameBg
        renderNavHighlight(frameBb, id)
        renderFrame(frameBb.min, frameBb.max, frameCol.u32, true, style.frameRounding)
        // Slider behavior
        val grabBb = Rect()
        val valueChanged = sliderBehavior(frameBb, id, dataType, v, vMin, vMax, format, power, SliderFlag.Vertical.i, grabBb)

        if (valueChanged)
            markItemEdited(id)

        // Render grab
        if (grabBb.max.y > grabBb.min.y)
            window.drawList.addRectFilled(grabBb.min, grabBb.max, getColorU32(if (g.activeId == id) Col.SliderGrabActive else Col.SliderGrab), style.grabRounding)

        /*  Display value using user-provided display format so user can add prefix/suffix/decorations to the value.
            For the vertical slider we allow centered text to overlap the frame padding         */
        val value = String(v.format(dataType, format))
        val posMin = Vec2(frameBb.min.x, frameBb.min.y + style.framePadding.y)
        renderTextClipped(posMin, frameBb.max, value, value.length, null, Vec2(0.5f, 0f))
        if (labelSize.x > 0f)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, frameBb.min.y + style.framePadding.y), label)

        return valueChanged
    }
}