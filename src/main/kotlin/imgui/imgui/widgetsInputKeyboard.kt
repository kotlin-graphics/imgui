package imgui.imgui

import glm_.glm
import glm_.vec2.Vec2
import imgui.ImGui.inputScalarEx
import imgui.ImGui.inputTextEx
import imgui.has
import imgui.hasnt
import imgui.internal.DataType
import imgui.or
import kotlin.reflect.KMutableProperty0
import imgui.Context as g
import imgui.InputTextFlags as Itf

/** Widgets: Input with Keyboard    */
interface imgui_widgetsInputKeyboard {


    fun inputText(label: String, buf: CharArray, flags: Int = 0
            /*, callback: TextEditCallback  = NULL, void* user_data = NULL*/): Boolean {

        assert(flags hasnt Itf.Multiline)    // call InputTextMultiline()
        return inputTextEx(label, buf, Vec2(), flags/*, callback, user_data*/)
    }

    fun inputTextMultiline(label: String, buf: CharArray, size: Vec2 = Vec2(), flags: Int = 0
            /*,ImGuiTextEditCallback callback = NULL, void* user_data = NULL*/) =
            inputTextEx(label, buf, size, flags or Itf.Multiline/*, callback, user_data*/)

    fun inputFloat(label: String, v: FloatArray, step: Float = 0f, stepFast: Float = 0f, decimalPrecision: Int = -1, extraFlags: Int = 0)
            = inputFloat(label, v, 0, step, stepFast, decimalPrecision, extraFlags)

    fun inputFloat(label: String, v: FloatArray, ptr: Int = 0, step: Float = 0f, stepFast: Float = 0f, decimalPrecision: Int = -1,
                   extraFlags: Int = 0): Boolean {
        f0 = v[ptr]
        val res = inputFloat(label, ::f0, step, stepFast, decimalPrecision, extraFlags)
        v[ptr] = f0
        return res
    }

    fun inputFloat(label: String, v: KMutableProperty0<Float>, step: Float = 0f, stepFast: Float = 0f, decimalPrecision: Int = -1,
                   extraFlags: Int = 0): Boolean {

        val pInt = intArrayOf(glm.floatBitsToInt(v()))
        val fmt = "%${if (decimalPrecision < 0) "" else ".$decimalPrecision"}f"
        val res = inputScalarEx(label, DataType.Float, pInt, step.takeIf { it > 0f }, stepFast.takeIf { it > 0f }, fmt, extraFlags)
        v.set(glm.intBitsToFloat(pInt[0]))
        return res
    }
//    IMGUI_API bool          InputFloat2(const char* label, float v[2], int decimal_precision = -1, ImGuiInputTextFlags extra_flags = 0);
//    IMGUI_API bool          InputFloat3(const char* label, float v[3], int decimal_precision = -1, ImGuiInputTextFlags extra_flags = 0);
//    IMGUI_API bool          InputFloat4(const char* label, float v[4], int decimal_precision = -1, ImGuiInputTextFlags extra_flags = 0);

    fun inputInt(label: String, v: KMutableProperty0<Int>, step: Int = 1, stepFast: Int = 100, extraFlags: Int = 0): Boolean {
        /*  Hexadecimal input provided as a convenience but the flag name is awkward. Typically you'd use inputText()
            to parse your own data, if you want to handle prefixes.             */
        val scalarFormat = if (extraFlags has Itf.CharsHexadecimal) "%08X" else "%d"
        return inputScalarEx(label, DataType.Int, v, if (step > 0f) step else null, if (stepFast > 0f) stepFast else null,
            scalarFormat, extraFlags)
    }
//    IMGUI_API bool          InputInt2(const char* label, int v[2], ImGuiInputTextFlags extra_flags = 0);
//    IMGUI_API bool          InputInt3(const char* label, int v[3], ImGuiInputTextFlags extra_flags = 0);
//    IMGUI_API bool          InputInt4(const char* label, int v[4], ImGuiInputTextFlags extra_flags = 0);

    companion object {
        private var f0 = 0f
    }
}
