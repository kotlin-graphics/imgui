package imgui.imgui

import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import imgui.Context.style
import imgui.IO
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.currentWindow
import imgui.ImGui.dragBehavior
import imgui.ImGui.focusWindow
import imgui.ImGui.focusableItemRegister
import imgui.ImGui.inputScalarAsWidgetReplacement
import imgui.ImGui.isHovered
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.parseFormatPrecision
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.setActiveId
import imgui.ImGui.setHoveredId
import imgui.internal.DataType
import imgui.internal.Rect
import imgui.Context as g

/** Widgets: Drags (tip: ctrl+click on a drag box to input with keyboard. manually input values aren't clamped, can go
 *  off-bounds)
 *  For all the Float2/Float3/Float4/Int2/Int3/Int4 versions of every functions, remember than a 'float v[3]' function
 *  argument is the same as 'float* v'. You can pass address of your first element out of a contiguous set,
 *  e.g. &myvector.x    */
interface imgui_widgetsDrag {


    fun dragFloat(label: String, v: FloatArray, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f, displayFormat: String = "%.3f",
                  power: Float = 1f) = dragFloat(label, v, 0, vSpeed, vMin, vMax, displayFormat, power)

    /** If vMin >= vMax we have no bound  */
    fun dragFloat(label: String, v: FloatArray, ptr: Int = 0, vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f,
                  displayFormat: String = "%.3f", power: Float = 1f): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(label)
        val w = calcItemWidth()

        val labelSize = calcTextSize(label, 0, true)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w, labelSize.y + style.framePadding.y * 2f))
        val innerBb = Rect(frameBb.min + style.framePadding, frameBb.max - style.framePadding)
        val totalBb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))

        // NB- we don't call ItemSize() yet because we may turn into a text edit box below
        if (!itemAdd(totalBb, id)) {
            itemSize(totalBb, style.framePadding.y)
            return false
        }

        val hovered = isHovered(frameBb, id)
        if (hovered)
            setHoveredId(id)

        val displayFormat = if (displayFormat.isEmpty()) "%.3f" else displayFormat
        val decimalPrecision = parseFormatPrecision(displayFormat, 3)

        // Tabbing or CTRL-clicking on Drag turns it into an input box
        var startTextInput = false
        val tabFocusRequested = focusableItemRegister(window, g.activeId == id)
        if (tabFocusRequested || (hovered && (IO.mouseClicked[0] || IO.mouseDoubleClicked[0]))) {
            setActiveId(id, window)
            focusWindow(window)

            if (tabFocusRequested || IO.keyCtrl || IO.mouseDoubleClicked[0]) {
                startTextInput = true
                g.scalarAsInputTextId = 0
            }
        }
        if (startTextInput || (g.activeId == id && g.scalarAsInputTextId == id)) {
            val data = intArrayOf(glm.floatBitsToInt(v[ptr]))
            val res = inputScalarAsWidgetReplacement(frameBb, label, DataType.Float, data, id, decimalPrecision)
            v[ptr] = glm.intBitsToFloat(data[0])
            return res
        }

        // Actual drag behavior
        itemSize(totalBb, style.framePadding.y)
        val valueChanged = dragBehavior(frameBb, id, v, ptr, vSpeed, vMin, vMax, decimalPrecision, power)

        // Display value using user-provided display format so user can add prefix/suffix/decorations to the value.
        val value = displayFormat.format(style.locale, v[ptr])
        renderTextClipped(frameBb.min, frameBb.max, value, value.length, null, Vec2(0.5f))

        if (labelSize.x > 0f)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, innerBb.min.y), label)

        return valueChanged
    }
//    IMGUI_API bool          DragFloat2(const char* label, float v[2], float v_speed = 1.0f, float v_min = 0.0f, float v_max = 0.0f, const char* display_format = "%.3f", float power = 1.0f);
//    IMGUI_API bool          DragFloat3(const char* label, float v[3], float v_speed = 1.0f, float v_min = 0.0f, float v_max = 0.0f, const char* display_format = "%.3f", float power = 1.0f);
//    IMGUI_API bool          DragFloat4(const char* label, float v[4], float v_speed = 1.0f, float v_min = 0.0f, float v_max = 0.0f, const char* display_format = "%.3f", float power = 1.0f);
//    IMGUI_API bool          DragFloatRange2(const char* label, float* v_current_min, float* v_current_max, float v_speed = 1.0f, float v_min = 0.0f, float v_max = 0.0f, const char* display_format = "%.3f", const char* display_format_max = NULL, float power = 1.0f);

    /** If v_min >= v_max we have no bound
     *  NB: vSpeed is float to allow adjusting the drag speed with more precision     */
    fun dragInt(label: String, v: IntArray, vSpeed: Float = 1f, vMin: Int = 0, vMax: Int = 0, displayFormat: String = "%.0f"): Boolean {

        val vF = floatArrayOf(v[0].f)
        val valueChanged = dragFloat(label, vF, vSpeed, vMin.f, vMax.f, displayFormat)
        v[0] = vF[0].i
        return valueChanged
    }
//    IMGUI_API bool          DragInt2(const char* label, int v[2], float v_speed = 1.0f, int v_min = 0, int v_max = 0, const char* display_format = "%.0f");
//    IMGUI_API bool          DragInt3(const char* label, int v[3], float v_speed = 1.0f, int v_min = 0, int v_max = 0, const char* display_format = "%.0f");
//    IMGUI_API bool          DragInt4(const char* label, int v[4], float v_speed = 1.0f, int v_min = 0, int v_max = 0, const char* display_format = "%.0f");
//    IMGUI_API bool          DragIntRange2(const char* label, int* v_current_min, int* v_current_max, float v_speed = 1.0f, int v_min = 0, int v_max = 0, const char* display_format = "%.0f", const char* display_format_max = NULL);

}