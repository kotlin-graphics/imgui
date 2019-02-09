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
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.currentWindow
import imgui.ImGui.endGroup
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.focusableItemRegister
import imgui.ImGui.focusableItemUnregister
import imgui.ImGui.inputScalarAsWidgetReplacement
import imgui.ImGui.io
import imgui.ImGui.itemAdd
import imgui.ImGui.itemHoverable
import imgui.ImGui.itemSize
import imgui.ImGui.markItemEdited
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
import imgui.ImGui.textUnformatted
import imgui.imgui.widgets.drags.Companion.patchFormatStringFloatToInt
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
interface sliders {


    /** Adjust format to decorate the value with a prefix or a suffix.
     *  "%.3f"         1.234
     *  "%5.2f secs"   01.23 secs
     *  "Gold: %.0f"   Gold: 1
     *  Use power != 1.0f for non-linear sliders.
     *  adjust format to decorate the value with a prefix or a suffix for in-slider labels or unit display. Use power!=1.0 for power curve sliders */
    fun sliderFloat(label: String, v: FloatArray, vMin: Float, vMax: Float, format: String? = null, power: Float = 1f)
            : Boolean = sliderFloat(label, v, 0, vMin, vMax, format, power)

    /** Adjust format to decorate the value with a prefix or a suffix.
     *  "%.3f"         1.234
     *  "%5.2f secs"   01.23 secs
     *  "Gold: %.0f"   Gold: 1
     *  Use power != 1.0f for non-linear sliders.
     *  adjust format to decorate the value with a prefix or a suffix for in-slider labels or unit display. Use power!=1.0 for power curve sliders */
    fun sliderFloat(label: String, v: FloatArray, ptr: Int, vMin: Float, vMax: Float, format: String? = null, power: Float = 1f): Boolean =
            withFloat(v, ptr) { sliderFloat(label, it, vMin, vMax, format, power) }

    /** Adjust format to decorate the value with a prefix or a suffix.
     *  "%.3f"         1.234
     *  "%5.2f secs"   01.23 secs
     *  "Gold: %.0f"   Gold: 1
     *  Use power != 1.0f for non-linear sliders.
     *  adjust format to decorate the value with a prefix or a suffix for in-slider labels or unit display. Use power!=1.0 for power curve sliders */
    fun sliderFloat(label: String, v: KMutableProperty0<Float>, vMin: Float, vMax: Float, format: String? = null, power: Float = 1f)
            : Boolean = sliderScalar(label, DataType.Float, v, vMin, vMax, format, power)

    /** Adjust format to decorate the value with a prefix or a suffix.
     *  "%.3f"         1.234
     *  "%5.2f secs"   01.23 secs
     *  "Gold: %.0f"   Gold: 1
     *  Use power != 1.0f for non-linear sliders.
     *  adjust format to decorate the value with a prefix or a suffix for in-slider labels or unit display. Use power!=1.0 for power curve sliders */
    fun sliderScalar(label: String, dataType: DataType, v: KMutableProperty0<*>, vMin: Number, vMax: Number, format_: String? = null,
                     power: Float = 1f): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(label)
        val w = calcItemWidth()

        val labelSize = calcTextSize(label, 0, true)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w, labelSize.y + style.framePadding.y * 2f))
        val totalBb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))

        itemSize(totalBb, style.framePadding.y)
        if (!itemAdd(totalBb, id, frameBb))
            return false

        // Default format string when passing NULL
        // Patch old "%.0f" format string to use "%d", read function comments for more details.
        val format = when {
            format_ == null -> when (dataType) {
                DataType.Float, DataType.Double -> "%f"
                else -> "%d"
            }
            dataType == DataType.Int && format_ != "%d" -> patchFormatStringFloatToInt(format_)
            else -> format_
        }
        // Tabbing or CTRL-clicking on Slider turns it into an input box
        var startTextInput = false
        val tabFocusRequested = focusableItemRegister(window, id)
        val hovered = itemHoverable(frameBb, id)
        if (tabFocusRequested || (hovered && io.mouseClicked[0]) || g.navActivateId == id || (g.navInputId == id && g.scalarAsInputTextId != id)) {
            setActiveId(id, window)
            setFocusId(id, window)
            window.focus()
            g.activeIdAllowNavDirFlags = (1 shl Dir.Up) or (1 shl Dir.Down)
            if (tabFocusRequested || io.keyCtrl || g.navInputId == id) {
                startTextInput = true
                g.scalarAsInputTextId = 0
            }
        }

        if (startTextInput || (g.activeId == id && g.scalarAsInputTextId == id)) {
            window.dc.cursorPos put frameBb.min
            focusableItemUnregister(window)
            return inputScalarAsWidgetReplacement(frameBb, id, label, DataType.Float, v, format)
        }

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
        val col = if (g.activeId == id) Col.SliderGrabActive else Col.SliderGrab
        window.drawList.addRectFilled(grabBb.min, grabBb.max, col.u32, style.grabRounding)

        // Display value using user-provided display format so user can add prefix/suffix/decorations to the value.
        val value = format.format(style.locale, v())
        renderTextClipped(frameBb.min, frameBb.max, value, value.length, null, Vec2(0.5f))

        if (labelSize.x > 0f)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, frameBb.min.y + style.framePadding.y), label)

        ImGuiTestEngineHook_ItemInfo(id, label, window.dc.itemFlags)
        return valueChanged
    }

    fun sliderFloat2(label: String, v: FloatArray, vMin: Float, vMax: Float, format: String? = null, power: Float = 1f) =
            sliderFloatN(label, v, 2, vMin, vMax, format, power)

    fun sliderVec2(label: String, v: Vec2, vMin: Float, vMax: Float, format: String? = null, power: Float = 1f): Boolean {
        val floats = v to FloatArray(2)
        val res = sliderFloatN(label, floats, 2, vMin, vMax, format, power)
        v put floats
        return res
    }

    fun sliderFloat3(label: String, v: FloatArray, vMin: Float, vMax: Float, format: String? = null, power: Float = 1f) =
            sliderFloatN(label, v, 3, vMin, vMax, format, power)

    fun sliderVec3(label: String, v: Vec3, vMin: Float, vMax: Float, format: String? = null, power: Float = 1f): Boolean {
        val floats = v to FloatArray(3)
        val res = sliderFloatN(label, floats, 3, vMin, vMax, format, power)
        v put floats
        return res
    }

    fun sliderFloat4(label: String, v: FloatArray, vMin: Float, vMax: Float, format: String? = null, power: Float = 1f) =
            sliderFloatN(label, v, 4, vMin, vMax, format, power)

    fun sliderVec4(label: String, v: Vec4, vMin: Float, vMax: Float, format: String? = null, power: Float = 1f): Boolean {
        val floats = v to FloatArray(4)
        val res = sliderFloatN(label, floats, 4, vMin, vMax, format, power)
        v put floats
        return res
    }

    fun sliderAngle(label: String, vRadPtr: KMutableProperty0<Float>, vDegreesMin: Float = -360f,
                    vDegreesMax: Float = 360f, format_: String = "%.0f deg"): Boolean {
        val format = if (format_.isEmpty()) "%.0f deg" else format_
        var vRad by vRadPtr
        vRad = vRad.deg
        val valueChanged = sliderFloat(label, vRadPtr, vDegreesMin, vDegreesMax, format, 1f)
        vRad = vRad.rad
        return valueChanged
    }

    fun sliderInt(label: String, v: IntArray, ptr: Int, vMin: Int, vMax: Int, format: String = "%d"): Boolean =
            withInt(v, ptr) { sliderInt(label, it, vMin, vMax, format) }

    fun sliderInt(label: String, v: KMutableProperty0<*>, vMin: Int, vMax: Int, format: String = "%d"): Boolean =
            sliderScalar(label, DataType.Int, v, vMin, vMax, format)

    fun sliderInt2(label: String, v: IntArray, vMin: Int, vMax: Int, format: String = "%d"): Boolean =
            sliderIntN(label, v, 2, vMin, vMax, format)

    fun sliderVec2i(label: String, v: Vec2i, vMin: Int, vMax: Int, format: String = "%d"): Boolean {
        val ints = v to IntArray(2)
        val res = sliderIntN(label, ints, 2, vMin, vMax, format)
        v put ints
        return res
    }

    fun sliderInt3(label: String, v: IntArray, vMin: Int, vMax: Int, format: String = "%d"): Boolean =
            sliderIntN(label, v, 3, vMin, vMax, format)

    fun sliderVec3i(label: String, v: Vec3i, vMin: Int, vMax: Int, format: String = "%d"): Boolean {
        val ints = v to IntArray(3)
        val res = sliderIntN(label, ints, 3, vMin, vMax, format)
        v put ints
        return res
    }

    fun sliderInt4(label: String, v: IntArray, vMin: Int, vMax: Int, format: String = "%d"): Boolean =
            sliderIntN(label, v, 4, vMin, vMax, format)

    fun sliderVec4i(label: String, v: Vec4i, vMin: Int, vMax: Int, format: String = "%d"): Boolean {
        val ints = v to IntArray(4)
        val res = sliderIntN(label, ints, 4, vMin, vMax, format)
        v put ints
        return res
    }

    fun vSliderFloat(label: String, size: Vec2, v: KMutableProperty0<*>, vMin: Float, vMax: Float,
                     format: String? = null, power: Float = 1f): Boolean {
        return vSliderScalar(label, size, DataType.Float, v, vMin, vMax, format, power)
    }

    fun vSliderScalar(label: String, size: Vec2, dataType: DataType, v: KMutableProperty0<*>, vMin: Number, vMax: Number,
                      format_: String? = null, power: Float = 1f): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(label)

        val labelSize = calcTextSize(label, 0, true)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        val bb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))

        itemSize(bb, style.framePadding.y)
        if (!itemAdd(frameBb, id)) return false

        // Default format string when passing NULL
        // Patch old "%.0f" format string to use "%d", read function comments for more details.
        val format = when {
            format_ == null -> when (dataType) {
                DataType.Float, DataType.Double -> "%f"
                else -> "%d"
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
        val col = if (g.activeId == id) Col.SliderGrabActive else Col.SliderGrab
        window.drawList.addRectFilled(grabBb.min, grabBb.max, col.u32, style.grabRounding)

        /*  Display value using user-provided display format so user can add prefix/suffix/decorations to the value.
            For the vertical slider we allow centered text to overlap the frame padding         */
        val value = String(v.format(dataType, format))
        val posMin = Vec2(frameBb.min.x, frameBb.min.y + style.framePadding.y)
        renderTextClipped(posMin, frameBb.max, value, value.length, null, Vec2(0.5f, 0f))
        if (labelSize.x > 0f)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, frameBb.min.y + style.framePadding.y), label)

        return valueChanged
    }

    fun vSliderInt(label: String, size: Vec2, v: KMutableProperty0<*>, vMin: Int, vMax: Int, format: String = "%d")
            : Boolean = vSliderScalar(label, size, DataType.Int, v, vMin, vMax, format)

    /** Add multiple sliders on 1 line for compact edition of multiple components   */
    fun sliderFloatN(label: String, v: FloatArray, component: Int, vMin: Float, vMax: Float, format: String?, power: Float = 1f)
            : Boolean {
        val window = currentWindow
        if (window.skipItems) return false

        var valueChanged = false
        beginGroup()
        pushId(label)
        pushMultiItemsWidths(component)
        for (i in 0 until component) {
            pushId(i)
            withFloat(v, i) { valueChanged = sliderFloat("##v", it, vMin, vMax, format, power) || valueChanged }
            sameLine(0f, style.itemInnerSpacing.x)
            popId()
            popItemWidth()
        }
        popId()

        textUnformatted(label, findRenderedTextEnd(label))
        endGroup()
        return valueChanged
    }

    fun sliderIntN(label: String, v: IntArray, components: Int, vMin: Int, vMax: Int, format: String): Boolean {
        val window = currentWindow
        if (window.skipItems) return false

        var valueChanged = false
        beginGroup()
        pushId(label)
        pushMultiItemsWidths(components)
        for (i in 0 until components) {
            pushId(i)
            withInt(v, i) { valueChanged = sliderInt("##v", it, vMin, vMax, format) || valueChanged }
            sameLine(0f, style.itemInnerSpacing.x)
            popId()
            popItemWidth()
        }
        popId()

        textUnformatted(label, findRenderedTextEnd(label))
        endGroup()
        return valueChanged
    }
}