package imgui.imgui

import glm_.f
import glm_.func.deg
import glm_.func.rad
import glm_.i
import glm_.vec2.Vec2
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
import imgui.ImGui.sliderBehavior
import imgui.ImGui.sliderFloatN
import imgui.internal.Rect
import imgui.internal.focus
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

//        println("sliderFloat $ptr") TODO clean
        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(label)
        val w = calcItemWidth()

        val labelSize = calcTextSize(label, 0, true)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w, labelSize.y + style.framePadding.y * 2f))
        val totalBb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))

        // NB- we don't call ItemSize() yet because we may turn into a text edit box below
        if (!itemAdd(totalBb, id)) {
            itemSize(totalBb, style.framePadding.y)
            return false
        }

        val hovered = itemHoverable(frameBb, id)

        val displayFormat = if (displayFormat.isEmpty()) "%.3f" else displayFormat

        val decimalPrecision = parseFormatPrecision(displayFormat, 3)

        // Tabbing or CTRL-clicking on Slider turns it into an input box
        var startTextInput = false
        val tabFocusRequested = focusableItemRegister(window, id)
        if (tabFocusRequested || (hovered && IO.mouseClicked[0])) {
            setActiveId(id, window)
            window.focus()
            if (tabFocusRequested || IO.keyCtrl) {
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

    fun sliderFloatVec2(label: String, v: Vec2, vMin: Float, vMax: Float, displayFormat: String = "%.3f", power: Float = 1f): Boolean {
        fa0[0] = v.x
        fa0[1] = v.y
        val res = sliderFloatN(label, fa0, vMin, vMax, displayFormat, power)
        v.x = fa0[0]
        v.y = fa0[1]
        return res
    }
//    IMGUI_API bool          SliderFloat3(const char* label, float v[3], float v_min, float v_max, const char* display_format = "%.3f", float power = 1.0f);
//    IMGUI_API bool          SliderFloat4(const char* label, float v[4], float v_min, float v_max, const char* display_format = "%.3f", float power = 1.0f);

    fun sliderAngle(label: String, vRad: KMutableProperty0<Float>, vDegreesMin: Float = -360f, vDegreesMax: Float = 360f): Boolean {
        f0 = vRad().deg
        val valueChanged = sliderFloat(label, ::f0, vDegreesMin, vDegreesMax, "%.0f deg", 1f)
        vRad.set(f0.rad)
        return valueChanged
    }

    fun sliderInt(label: String, v: IntArray, vMin: Int, vMax: Int, displayFormat: String = "%.0f") =
            sliderInt(label, ::i0.apply { set(v[0]) }, vMin, vMax, displayFormat).also { v[0] = i0 }

    fun sliderInt(label: String, v: KMutableProperty0<Int>, vMin: Int, vMax: Int, displayFormat: String = "%.0f"): Boolean {
        val displayFormat = if (displayFormat.isEmpty()) "%.0f" else displayFormat
        f0 = v().f
        val valueChanged = sliderFloat(label, ::f0, vMin.f, vMax.f, displayFormat, 1f)
        v.set(f0.i)
        return valueChanged
    }
//    IMGUI_API bool          SliderInt2(const char* label, int v[2], int v_min, int v_max, const char* display_format = "%.0f");
//    IMGUI_API bool          SliderInt3(const char* label, int v[3], int v_min, int v_max, const char* display_format = "%.0f");
//    IMGUI_API bool          SliderInt4(const char* label, int v[4], int v_min, int v_max, const char* display_format = "%.0f");
//    IMGUI_API bool          VSliderFloat(const char* label, const ImVec2& size, float* v, float v_min, float v_max, const char* display_format = "%.3f", float power = 1.0f);
//    IMGUI_API bool          VSliderInt(const char* label, const ImVec2& size, int* v, int v_min, int v_max, const char* display_format = "%.0f");

    companion object {
        val fa0 = FloatArray(2)
        private var f0 = 0f
        private var i0 = 0
    }
}