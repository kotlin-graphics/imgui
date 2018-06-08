package imgui.imgui

import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec3.Vec3
import glm_.vec3.Vec3i
import glm_.vec4.Vec4
import glm_.vec4.Vec4i
import imgui.ImGui.inputFloatN
import imgui.ImGui.inputIntN
import imgui.ImGui.inputScalarEx
import imgui.ImGui.inputTextEx
import imgui.InputTextFlags
import imgui.has
import imgui.hasnt
import imgui.internal.DataType
import imgui.or
import kotlin.reflect.KMutableProperty0
import imgui.InputTextFlag as Itf

/** Widgets: Input with Keyboard    */
interface imgui_widgetsInputKeyboard {


    fun inputText(label: String, buf: CharArray, flags: InputTextFlags = 0
            /*, callback: TextEditCallback  = NULL, void* user_data = NULL*/): Boolean {

        assert(flags hasnt Itf.Multiline) { "call InputTextMultiline()" }
        return inputTextEx(label, buf, Vec2(), flags/*, callback, user_data*/)
    }

    fun inputTextMultiline(label: String, buf: CharArray, size: Vec2 = Vec2(), flags: InputTextFlags = 0
            /*,ImGuiTextEditCallback callback = NULL, void* user_data = NULL*/) =
            inputTextEx(label, buf, size, flags or Itf.Multiline/*, callback, user_data*/)

    fun inputFloat(label: String, v: FloatArray, step: Float = 0f, stepFast: Float = 0f, decimalPrecision: Int = -1,
                   extraFlags: InputTextFlags = 0) = inputFloat(label, v, 0, step, stepFast, decimalPrecision, extraFlags)

    fun inputFloat(label: String, v: FloatArray, ptr: Int = 0, step: Float = 0f, stepFast: Float = 0f, decimalPrecision: Int = -1,
                   extraFlags: InputTextFlags = 0) = withFloat { f ->
        f.set(v[ptr])
        val res = inputFloat(label, f, step, stepFast, decimalPrecision, extraFlags)
        v[ptr] = f()
        res
    }

    fun inputFloat(label: String, v: KMutableProperty0<Float>, step: Float = 0f, stepFast: Float = 0f,
                   decimalPrecision: Int = -1, extraFlags: InputTextFlags = 0): Boolean {
        val flags = extraFlags or Itf.CharsScientific
        /*  Ideally we'd have a minimum decimal precision of 1 to visually denote that this is a float,
            while hiding non-significant digits? %f doesn't have a minimum of 1         */
        val format = "%${if (decimalPrecision < 0) "" else ".$decimalPrecision"}f"
        return inputScalarEx(label, DataType.Float, v as KMutableProperty0<Number>, step.takeIf { it > 0f },
                stepFast.takeIf { it > 0f }, format, flags)
    }

    fun inputDouble(label: String, v: KMutableProperty0<Double>, step: Double = 0.0, stepFast: Double = 0.0,
                    format: String = "%.6f", extraFlags: InputTextFlags = 0): Boolean {
        val flags = extraFlags or Itf.CharsScientific
        /*  Ideally we'd have a minimum decimal precision of 1 to visually denote that this is a float,
            while hiding non-significant digits? %f doesn't have a minimum of 1         */
        return inputScalarEx(label, DataType.Double, v as KMutableProperty0<Number>, step.takeIf { it > 0.0 },
                stepFast.takeIf { it > 0.0 }, format, flags)
    }

    fun inputFloat2(label: String, v: FloatArray, decimalPrecision: Int = -1, extraFlags: InputTextFlags = 0) =
            inputFloatN(label, v, 2, decimalPrecision, extraFlags)

    fun inputVec2(label: String, v: Vec2, decimalPrecision: Int = -1, extraFlags: InputTextFlags = 0): Boolean {
        val floats = v to FloatArray(2)
        val res = inputFloatN(label, floats, 2, decimalPrecision, extraFlags)
        v put floats
        return res
    }

    fun inputFloat3(label: String, v: FloatArray, decimalPrecision: Int = -1, extraFlags: InputTextFlags = 0) =
            inputFloatN(label, v, 3, decimalPrecision, extraFlags)

    fun inputVec3(label: String, v: Vec3, decimalPrecision: Int = -1, extraFlags: InputTextFlags = 0): Boolean {
        val floats = v to FloatArray(3)
        val res = inputFloatN(label, floats, 3, decimalPrecision, extraFlags)
        v put floats
        return res
    }

    fun inputFloat4(label: String, v: FloatArray, decimalPrecision: Int = -1, extraFlags: InputTextFlags = 0) =
            inputFloatN(label, v, 4, decimalPrecision, extraFlags)

    fun inputVec4(label: String, v: Vec4, decimalPrecision: Int = -1, extraFlags: InputTextFlags = 0): Boolean {
        val floats = v to FloatArray(4)
        val res = inputFloatN(label, floats, 4, decimalPrecision, extraFlags)
        v put floats
        return res
    }

    fun inputInt(label: String, v: KMutableProperty0<Int>, step: Int = 1, stepFast: Int = 100, extraFlags: InputTextFlags = 0): Boolean {
        /*  Hexadecimal input provided as a convenience but the flag name is awkward. Typically you'd use inputText()
            to parse your own data, if you want to handle prefixes.             */
        val scalarFormat = if (extraFlags has Itf.CharsHexadecimal) "%08X" else "%d"
        return inputScalarEx(label, DataType.Int, v as KMutableProperty0<Number>, step.takeIf { it > 0f }, stepFast.takeIf { it > 0f },
                scalarFormat, extraFlags)
    }

    fun inputInt2(label: String, v: IntArray, extraFlags: InputTextFlags = 0) = inputIntN(label, v, 2, extraFlags)
    fun inputVec2i(label: String, v: Vec2i, extraFlags: InputTextFlags = 0): Boolean {
        val ints = v to IntArray(2)
        val res = inputIntN(label, ints, 2, extraFlags)
        v put ints
        return res
    }

    fun inputInt3(label: String, v: IntArray, extraFlags: InputTextFlags = 0) = inputIntN(label, v, 3, extraFlags)
    fun inputVec3i(label: String, v: Vec3i, extraFlags: InputTextFlags = 0): Boolean {
        val ints = v to IntArray(3)
        val res = inputIntN(label, ints, 3, extraFlags)
        v put ints
        return res
    }

    fun inputInt4(label: String, v: IntArray, extraFlags: InputTextFlags = 0) = inputIntN(label, v, 4, extraFlags)
    fun inputVec4i(label: String, v: Vec4i, extraFlags: InputTextFlags = 0): Boolean {
        val ints = v to IntArray(4)
        val res = inputIntN(label, ints, 4, extraFlags)
        v put ints
        return res
    }

    companion object {
        private var f0 = 0f
        private var i0 = 0
        private var L0 = 0L
    }
}
