package imgui.imgui

import glm_.f
import glm_.func.deg
import glm_.func.rad
import glm_.i
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec3.Vec3i
import glm_.vec4.Vec4i
import imgui.Context.style
import imgui.IO
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.currentWindow
import imgui.ImGui.focusableItemRegister
import imgui.ImGui.itemAdd
import imgui.ImGui.itemHoverable
import imgui.ImGui.itemSize
import imgui.ImGui.parseFormatPrecision
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.setActiveId
import imgui.ImGui.setFocusId
import imgui.ImGui.sliderBehavior
import imgui.ImGui.sliderFloatN
import imgui.ImGui.sliderIntN
import imgui.Ref
import imgui.internal.*
import kotlin.reflect.KMutableProperty0
import imgui.Context as g

/** Widgets: Sliders (tip: ctrl+click on a slider to input with keyboard. manually input values aren't clamped, can go
 *  off-bounds)  */
interface imgui_widgetsSliders {


    /** adjust display_format to decorate the value with a prefix or a suffix for in-slider labels or unit display.
     *  Use power!=1.0 for logarithmic sliders  */
    fun sliderFloat(label: String, v: FloatArray, vMin: Float, vMax: Float, displayFormat: String = "%.3f", power: Float = 1f) =
            sliderFloat(label, v, 0, vMin, vMax, displayFormat, power)

    fun sliderFloat(label: String, v: FloatArray, ptr: Int, vMin: Float, vMax: Float, displayFormat: String = "%.3f",
                    power: Float = 1f): Boolean {

        f0 = v[ptr]
        val res = sliderFloat(label, ::f0, vMin, vMax, displayFormat, power)
        v[ptr] = f0
        return res
    }

    fun sliderFloat(label: String, v: KMutableProperty0<Float>, vMin: Float, vMax: Float, displayFormat: String = "%.3f",
                    power: Float = 1f): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(label)
        val w = calcItemWidth()

        val labelSize = calcTextSize(label, 0, true)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w, labelSize.y + style.framePadding.y * 2f))
        val totalBb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))

        // NB- we don't call ItemSize() yet because we may turn into a text edit box below
        if (!itemAdd(totalBb, id, frameBb)) {
            itemSize(totalBb, style.framePadding.y)
            return false
        }

        val hovered = itemHoverable(frameBb, id)

        val decimalPrecision = parseFormatPrecision(displayFormat, 3)

        // Tabbing or CTRL-clicking on Slider turns it into an input box
        var startTextInput = false
        val tabFocusRequested = focusableItemRegister(window, id)
        if (tabFocusRequested || (hovered && IO.mouseClicked[0]) || g.navActivateId == id || (g.navInputId == id && g.scalarAsInputTextId != id)) {
            setActiveId(id, window)
            setFocusId(id, window)
            window.focus()
            g.activeIdAllowNavDirFlags = (1 shl Dir.Up) or (1 shl Dir.Down)
            if (tabFocusRequested || IO.keyCtrl || g.navInputId == id) {
                startTextInput = true
                g.scalarAsInputTextId = 0
            }
        }

        if (startTextInput || (g.activeId == id && g.scalarAsInputTextId == id)) TODO()
//            return inputScalarAsWidgetReplacement(frameBb, label, DataType.Float, v, id, decimalPrecision)

        // Actual slider behavior + render grab
        itemSize(totalBb, style.framePadding.y)
        val valueChanged = sliderBehavior(frameBb, id, v, vMin, vMax, power, decimalPrecision)

        // Display value using user-provided display format so user can add prefix/suffix/decorations to the value.
        val value = displayFormat.format(style.locale, v())
        renderTextClipped(frameBb.min, frameBb.max, value, value.length, null, Vec2(0.5f, 0.5f))

        if (labelSize.x > 0f)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, frameBb.min.y + style.framePadding.y), label)

        return valueChanged
    }

    fun sliderFloat2(label: String, v: FloatArray, vMin: Float, vMax: Float, displayFormat: String = "%.3f", power: Float = 1f) =
            sliderFloatN(label, v, 2, vMin, vMax, displayFormat, power)

    fun sliderVec2(label: String, v: Vec2, vMin: Float, vMax: Float, displayFormat: String = "%.3f", power: Float = 1f): Boolean {
        val floats = v to FloatArray(2)
        val res = sliderFloatN(label, floats, 2, vMin, vMax, displayFormat, power)
        v put floats
        return res
    }

    fun sliderFloat3(label: String, v: FloatArray, vMin: Float, vMax: Float, displayFormat: String = "%.3f", power: Float = 1f) =
            sliderFloatN(label, v, 3, vMin, vMax, displayFormat, power)

    fun sliderVec3(label: String, v: Vec2, vMin: Float, vMax: Float, displayFormat: String = "%.3f", power: Float = 1f): Boolean {
        val floats = v to FloatArray(3)
        val res = sliderFloatN(label, floats, 3, vMin, vMax, displayFormat, power)
        v put floats
        return res
    }

    fun sliderFloat4(label: String, v: FloatArray, vMin: Float, vMax: Float, displayFormat: String = "%.3f", power: Float = 1f) =
            sliderFloatN(label, v, 4, vMin, vMax, displayFormat, power)

    fun sliderVec4(label: String, v: Vec2, vMin: Float, vMax: Float, displayFormat: String = "%.3f", power: Float = 1f): Boolean {
        val floats = v to FloatArray(4)
        val res = sliderFloatN(label, floats, 4, vMin, vMax, displayFormat, power)
        v put floats
        return res
    }

    fun sliderAngle(label: String, vRad: KMutableProperty0<Float>, vDegreesMin: Float = -360f, vDegreesMax: Float = 360f): Boolean {
        f0 = vRad().deg
        val valueChanged = sliderFloat(label, ::f0, vDegreesMin, vDegreesMax, "%.0f deg", 1f)
        vRad.set(f0.rad)
        return valueChanged
    }

    fun sliderInt(label: String, v: IntArray, vMin: Int, vMax: Int, displayFormat: String = "%.0f") =
            sliderInt(label, ::i0.apply { set(v[0]) }, vMin, vMax, displayFormat).also { v[0] = i0 }

    fun sliderInt(label: String, v: KMutableProperty0<Int>, vMin: Int, vMax: Int, displayFormat: String = "%.0f"): Boolean {
        f0 = v().f
        val valueChanged = sliderFloat(label, ::f0, vMin.f, vMax.f, displayFormat, 1f)
        v.set(f0.i)
        return valueChanged
    }

    fun sliderInt2(label: String, v: IntArray, vMin: Int, vMax: Int, displayFormat: String = "%.0f") =
            sliderIntN(label, v, 2, vMin, vMax, displayFormat)

    fun sliderVec2i(label: String, v: Vec2i, vMin: Int, vMax: Int, displayFormat: String = "%.0f"): Boolean {
        val ints = v to IntArray(2)
        val res = sliderIntN(label, ints, 2, vMin, vMax, displayFormat)
        v put ints
        return res
    }

    fun sliderInt3(label: String, v: IntArray, vMin: Int, vMax: Int, displayFormat: String = "%.0f") =
            sliderIntN(label, v, 3, vMin, vMax, displayFormat)

    fun sliderVec3i(label: String, v: Vec3i, vMin: Int, vMax: Int, displayFormat: String = "%.0f"): Boolean {
        val ints = v to IntArray(3)
        val res = sliderIntN(label, ints, 3, vMin, vMax, displayFormat)
        v put ints
        return res
    }

    fun sliderInt4(label: String, v: IntArray, vMin: Int, vMax: Int, displayFormat: String = "%.0f") =
            sliderIntN(label, v, 4, vMin, vMax, displayFormat)

    fun sliderVec4i(label: String, v: Vec4i, vMin: Int, vMax: Int, displayFormat: String = "%.0f"): Boolean {
        val ints = v to IntArray(4)
        val res = sliderIntN(label, ints, 4, vMin, vMax, displayFormat)
        v put ints
        return res
    }

    fun vSliderFloat(label: String, size: Vec2, v: KMutableProperty0<Float>, vMin: Float, vMax: Float, displayFormat: String = "%.3f",
                     power: Float = 1f): Boolean {

        val window = currentWindow
        if (window.skipItems)        return false

        val id = window.getId(label)

        val labelSize = calcTextSize(label, 0, true)
        val frameBb = Rect (window.dc.cursorPos, window.dc.cursorPos + size)
        val bb = Rect (frameBb.min, frameBb.max + Vec2(if(labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))

        itemSize(bb, style.framePadding.y)
        if (!itemAdd(frameBb, id))            return false
        val hovered = itemHoverable(frameBb, id)

        val decimalPrecision = parseFormatPrecision(displayFormat, 3)

        if ((hovered && IO.mouseClicked[0])  || g.navActivateId == id || g.navInputId == id) {
            setActiveId(id, window)
            setFocusId(id, window)
            window.focus()
            g.activeIdAllowNavDirFlags = (1 shl Dir.Left) or (1 shl Dir.Right)
        }

        // Actual slider behavior + render grab
        val valueChanged = sliderBehavior(frameBb, id, v, vMin, vMax, power, decimalPrecision, SliderFlags.Vertical.i)

        /*  Display value using user-provided display format so user can add prefix/suffix/decorations to the value.
            For the vertical slider we allow centered text to overlap the frame padding         */
        val text =  displayFormat.format(style.locale, v())
        val posMin = Vec2(frameBb.min.x, frameBb.min.y + style.framePadding.y)
        renderTextClipped(posMin, frameBb.max, text, text.length, null, Vec2(0.5f,0f))
        if (labelSize.x > 0f)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, frameBb.min.y + style.framePadding.y), label)

        return valueChanged
    }

    fun vSliderInt(label: String, size: Vec2, v: KMutableProperty0<Int>, vMin: Int, vMax: Int, displayFormat: String = "%.0f") =
            withFloat { f ->
                f.set(v().f)
                val valueChanged = vSliderFloat (label, size, f, vMin.f, vMax.f, displayFormat, 1f)
                v.set(f().i)
                valueChanged
            }

    companion object {
        private var f0 = 0f
        private var i0 = 0
    }
}

inline fun <R>withFloat(block: (KMutableProperty0<Float>) -> R): R {
    Ref.fPtr++
    return block(Ref::float).also { Ref.fPtr-- }
}