package imgui.imgui

import glm_.glm
import glm_.vec2.Vec2
import imgui.ImGui.inputScalarEx
import imgui.ImGui.inputTextEx
import imgui.InputTextFlags
import imgui.hasnt
import imgui.internal.DataType
import imgui.Context as g

/** Widgets: Input with Keyboard    */
interface imgui_widgetsInputKeyboard {


    fun inputText(label: String, buf: CharArray, flags: Int = 0
            /*, callback: TextEditCallback  = NULL, void* user_data = NULL*/): Boolean {

        assert(flags hasnt InputTextFlags.Multiline)    // call InputTextMultiline()
        return inputTextEx(label, buf, Vec2(), flags/*, callback, user_data*/)
    }
//    IMGUI_API bool          InputTextMultiline(const char* label, char* buf, size_t buf_size, const ImVec2& size = ImVec2(0,0), ImGuiInputTextFlags flags = 0, ImGuiTextEditCallback callback = NULL, void* user_data = NULL);

    fun inputFloat(label: String, v: FloatArray, step: Float = 0f, stepFast: Float = 0f, decimalPrecision: Int = -1, extraFlags: Int = 0) =
            inputFloat(label, v, 0, step, stepFast, decimalPrecision, extraFlags)

    fun inputFloat(label: String, v: FloatArray, ptr: Int = 0, step: Float = 0f, stepFast: Float = 0f, decimalPrecision: Int = -1,
                   extraFlags: Int = 0): Boolean {

        val pInt = intArrayOf(glm.floatBitsToInt(v[ptr]))
        val fmt = "%${if (decimalPrecision < 0) "" else ".$decimalPrecision"}f"
        val res = inputScalarEx(label, DataType.Float, pInt, step.takeIf { it > 0f }, stepFast.takeIf { it > 0f }, fmt, extraFlags)
        v[ptr] = glm.intBitsToFloat(pInt[0])
        return res
    }
//    IMGUI_API bool          InputFloat2(const char* label, float v[2], int decimal_precision = -1, ImGuiInputTextFlags extra_flags = 0);
//    IMGUI_API bool          InputFloat3(const char* label, float v[3], int decimal_precision = -1, ImGuiInputTextFlags extra_flags = 0);
//    IMGUI_API bool          InputFloat4(const char* label, float v[4], int decimal_precision = -1, ImGuiInputTextFlags extra_flags = 0);
//    IMGUI_API bool          InputInt(const char* label, int* v, int step = 1, int step_fast = 100, ImGuiInputTextFlags extra_flags = 0);
//    IMGUI_API bool          InputInt2(const char* label, int v[2], ImGuiInputTextFlags extra_flags = 0);
//    IMGUI_API bool          InputInt3(const char* label, int v[3], ImGuiInputTextFlags extra_flags = 0);
//    IMGUI_API bool          InputInt4(const char* label, int v[4], ImGuiInputTextFlags extra_flags = 0);
}
