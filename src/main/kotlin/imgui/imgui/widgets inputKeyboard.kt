package imgui.imgui

import gli_.hasnt
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec3.Vec3
import glm_.vec3.Vec3i
import glm_.vec4.Vec4
import glm_.vec4.Vec4i
import imgui.*
import imgui.ImGui.beginGroup
import imgui.ImGui.buttonEx
import imgui.ImGui.calcItemWidth
import imgui.ImGui.currentWindow
import imgui.ImGui.endGroup
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.frameHeight
import imgui.ImGui.inputTextEx
import imgui.ImGui.io
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.ImGui.pushMultiItemsWidths
import imgui.ImGui.sameLine
import imgui.ImGui.style
import imgui.ImGui.textUnformatted
import kotlin.math.max
import kotlin.reflect.KMutableProperty0
import imgui.InputTextFlag as Itf
import imgui.internal.ButtonFlag as Bf

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

    fun inputFloat(label: String, v: FloatArray, step: Float = 0f, stepFast: Float = 0f, format: String? = null,
                   extraFlags: InputTextFlags = 0) = inputFloat(label, v, 0, step, stepFast, format, extraFlags)

    fun inputFloat(label: String, v: FloatArray, ptr: Int = 0, step: Float = 0f, stepFast: Float = 0f, format: String? = null,
                   extraFlags: InputTextFlags = 0) = withFloat { f ->
        f.set(v[ptr])
        val res = inputFloat(label, f, step, stepFast, format, extraFlags)
        v[ptr] = f()
        res
    }

    fun inputFloat(label: String, v: KMutableProperty0<Float>, step: Float = 0f, stepFast: Float = 0f, format: String? = null,
                   extraFlags: InputTextFlags = 0): Boolean {
        val flags = extraFlags or Itf.CharsScientific
        return inputScalar(label, DataType.Float, v, step.takeIf { it > 0f }, stepFast.takeIf { it > 0f }, format, flags)
    }

    fun inputDouble(label: String, v: KMutableProperty0<Double>, step: Double = 0.0, stepFast: Double = 0.0, format: String? = null,
                    extraFlags: InputTextFlags = 0): Boolean {
        val flags = extraFlags or Itf.CharsScientific
        /*  Ideally we'd have a minimum decimal precision of 1 to visually denote that this is a float,
            while hiding non-significant digits? %f doesn't have a minimum of 1         */
        return inputScalar(label, DataType.Double, v, step.takeIf { it > 0.0 }, stepFast.takeIf { it > 0.0 }, format, flags)
    }

    fun inputFloat2(label: String, v: FloatArray, format: String? = null, extraFlags: InputTextFlags = 0)
            : Boolean = inputFloatN(label, v, 2, null, null, format, extraFlags)

    fun inputVec2(label: String, v: Vec2, format: String? = null, extraFlags: InputTextFlags = 0): Boolean {
        val floats = v to FloatArray(2)
        val res = inputFloatN(label, floats, 2, null, null, format, extraFlags)
        v put floats
        return res
    }

    fun inputFloat3(label: String, v: FloatArray, format: String? = null, extraFlags: InputTextFlags = 0)
            : Boolean = inputFloatN(label, v, 3, null, null, format, extraFlags)

    fun inputVec3(label: String, v: Vec3, format: String? = null, extraFlags: InputTextFlags = 0): Boolean {
        val floats = v to FloatArray(3)
        val res = inputFloatN(label, floats, 3, null, null, format, extraFlags)
        v put floats
        return res
    }

    fun inputFloat4(label: String, v: FloatArray, format: String? = null, extraFlags: InputTextFlags = 0)
            : Boolean = inputFloatN(label, v, 4, null, null, format, extraFlags)

    fun inputVec4(label: String, v: Vec4, format: String? = null, extraFlags: InputTextFlags = 0): Boolean {
        val floats = v to FloatArray(4)
        val res = inputFloatN(label, floats, 4, null, null, format, extraFlags)
        v put floats
        return res
    }

    fun inputInt(label: String, v: KMutableProperty0<Int>, step: Int = 1, stepFast: Int = 100, extraFlags: InputTextFlags = 0): Boolean {
        /*  Hexadecimal input provided as a convenience but the flag name is awkward. Typically you'd use inputText()
            to parse your own data, if you want to handle prefixes.             */
        val format = if (extraFlags has Itf.CharsHexadecimal) "%08X" else "%d"
        return inputScalar(label, DataType.Int, v, step.takeIf { it > 0f }, stepFast.takeIf { it > 0f }, format, extraFlags)
    }

    fun inputInt2(label: String, v: IntArray, extraFlags: InputTextFlags = 0) =
            inputIntN(label, v, 2, null, null, "%d", extraFlags)

    fun inputVec2i(label: String, v: Vec2i, extraFlags: InputTextFlags = 0): Boolean {
        val ints = v to IntArray(2)
        val res = inputIntN(label, ints, 2, null, null, "%d", extraFlags)
        v put ints
        return res
    }

    fun inputInt3(label: String, v: IntArray, extraFlags: InputTextFlags = 0) =
            inputIntN(label, v, 3, null, null, "%d", extraFlags)

    fun inputVec3i(label: String, v: Vec3i, extraFlags: InputTextFlags = 0): Boolean {
        val ints = v to IntArray(3)
        val res = inputIntN(label, ints, 3, null, null, "%d", extraFlags)
        v put ints
        return res
    }

    fun inputInt4(label: String, v: IntArray, extraFlags: InputTextFlags = 0) =
            inputIntN(label, v, 4, null, null, "%d", extraFlags)

    fun inputVec4i(label: String, v: Vec4i, extraFlags: InputTextFlags = 0): Boolean {
        val ints = v to IntArray(4)
        val res = inputIntN(label, ints, 4, null, null, "%d", extraFlags)
        v put ints
        return res
    }

    /** NB: format here must be a simple "%xx" format string with no prefix/suffix (unlike the Drag/Slider
     *  functions "format" argument)    */
    fun inputScalar(label: String, dataType: DataType, data: IntArray, step: Int?, stepFast: Int?, format: String? = null,
                    extraFlags: InputTextFlags = 0): Boolean = withInt(data, 0) {
        inputScalar(label, dataType, it, step, stepFast, format, extraFlags)
    }

    /** NB: format here must be a simple "%xx" format string with no prefix/suffix (unlike the Drag/Slider
     *  functions "format" argument)    */
    @Suppress("UNCHECKED_CAST")
    fun inputScalar(label: String, dataType: DataType, data: KMutableProperty0<*>, step: Number?, stepFast: Number?,
                    format_: String? = null, extraFlags_: InputTextFlags = 0): Boolean {

        data as KMutableProperty0<Number>
        val window = currentWindow
        if (window.skipItems) return false

        val format = when (format_) {
            null -> when (dataType) {
                DataType.Float, DataType.Double -> "%f"
                else -> "%d"
            }
            else -> format_
        }

        val buf = data.format(dataType, format)

        var valueChanged = false
        var extraFlags = extraFlags_
        if (extraFlags hasnt (Itf.CharsHexadecimal or Itf.CharsScientific))
            extraFlags = extraFlags or Itf.CharsDecimal
        extraFlags = extraFlags or Itf.AutoSelectAll

        if (step != null) {
            val buttonSize = frameHeight

            beginGroup() // The only purpose of the group here is to allow the caller to query item data e.g. IsItemActive()
            pushId(label)
            pushItemWidth(max(1f, calcItemWidth() - (buttonSize + style.itemInnerSpacing.x) * 2))
            if (inputText("", buf, extraFlags)) // PushId(label) + "" gives us the expected ID from outside point of view
                valueChanged = dataTypeApplyOpFromText(buf, g.inputTextState.initialText, dataType, data, format)
            popItemWidth()

            // Step buttons
            sameLine(0f, style.itemInnerSpacing.x)
            if (buttonEx("-", Vec2(buttonSize), Bf.Repeat or Bf.DontClosePopups)) {
                data.set(dataTypeApplyOp(dataType, '-', data(), if (io.keyCtrl && stepFast != null) stepFast else step))
                valueChanged = true
            }
            sameLine(0f, style.itemInnerSpacing.x)
            if (buttonEx("+", Vec2(buttonSize), Bf.Repeat or Bf.DontClosePopups)) {
                data.set(dataTypeApplyOp(dataType, '+', data(), if (io.keyCtrl && stepFast != null) stepFast else step))
                valueChanged = true
            }
            sameLine(0f, style.itemInnerSpacing.x)
            textUnformatted(label, findRenderedTextEnd(label))

            popId()
            endGroup()
        } else if (inputText(label, buf, extraFlags))
            valueChanged = dataTypeApplyOpFromText(buf, g.inputTextState.initialText, dataType, data, format)

        return valueChanged
    }

    fun inputFloatN(label: String, v: FloatArray, components: Int, step: Number? = null, stepFast: Number? = null,
                    format: String? = null, extraFlags: Int): Boolean {
        val window = currentWindow
        if (window.skipItems) return false

        var valueChanged = false
        beginGroup()
        pushId(label)
        pushMultiItemsWidths(components)
        for (i in 0 until components) {
            pushId(i)
            withFloat(v, i) {
                valueChanged = inputScalar("##v", DataType.Float, it, step, stepFast, format, extraFlags) || valueChanged
            }
            sameLine(0f, style.itemInnerSpacing.x)
            popId()
            popItemWidth()
        }
        popId()

        textUnformatted(label, findRenderedTextEnd(label))
        endGroup()
        return valueChanged
    }

    fun inputIntN(label: String, v: IntArray, components: Int, step: Int? = null, stepFast: Int? = null, format: String? = null,
                  extraFlags: Int): Boolean {
        val window = currentWindow
        if (window.skipItems) return false

        var valueChanged = false
        beginGroup()
        pushId(label)
        pushMultiItemsWidths(components)
        for (i in 0 until components) {
            pushId(i)
            withInt(v, i) { valueChanged = inputScalar("##v", DataType.Int, it, step, stepFast, format, extraFlags) || valueChanged }
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
