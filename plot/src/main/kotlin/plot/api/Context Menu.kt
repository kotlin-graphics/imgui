package plot.api

import imgui.ImGui
import imgui.StyleVar
import imgui.api.drag
import imgui.internal.sections.ItemFlag
import imgui.none
import kotlin.reflect.KMutableProperty0

//template <typename F>
//bool DragFloat(const char*, F*, float, F, F) {
//    return false;
//}
//
//template <>
fun dragFloat(label: String, v: KMutableProperty0<Double>, vSpeed: Float, vMin: Double, vMax: Double): Boolean = ImGui.drag(label, v, vSpeed, vMin, vMax, "%.3f", none)
//
//template <>
//bool DragFloat<float>(const char* label, float* v, float v_speed, float v_min, float v_max) {
//    return ImGui::DragScalar(label, ImGuiDataType_Float, v, v_speed, &v_min, &v_max, "%.3f", 1);
//}

fun beginDisabledControls(cond: Boolean) {
    if (cond) {
        ImGui.pushItemFlag(ItemFlag.Disabled, true)
        ImGui.pushStyleVar(StyleVar.Alpha, ImGui.style.alpha * 0.25f)
    }
}

fun endDisabledControls(cond: Boolean) {
    if (cond) {
        ImGui.popItemFlag()
        ImGui.popStyleVar()
    }
}