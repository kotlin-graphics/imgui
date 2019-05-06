package imgui.imgui.widgets

import gli_.hasnt
import glm_.func.common.max
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec3.Vec3
import glm_.vec3.Vec3i
import glm_.vec4.Vec4
import glm_.vec4.Vec4i
import imgui.*
import imgui.ImGui.beginGroup
import imgui.ImGui.buttonEx
import imgui.ImGui.currentWindow
import imgui.ImGui.endGroup
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.frameHeight
import imgui.ImGui.inputTextEx
import imgui.ImGui.io
import imgui.ImGui.nextItemWidth
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushId
import imgui.ImGui.pushMultiItemsWidths
import imgui.ImGui.sameLine
import imgui.ImGui.style
import imgui.ImGui.textEx
import imgui.imgui.g
import imgui.imgui.withFloat
import imgui.imgui.withInt
import imgui.internal.or
import kotlin.reflect.KMutableProperty0
import imgui.InputTextFlag as Itf
import imgui.internal.ButtonFlag as Bf

/** Widgets: Input with Keyboard
 *  - If you want to use InputText() with a dynamic string type such as std::string or your own, see cpp/imgui_stdlib.h
 *  - Most of the ImGuiInputTextFlags flags are only useful for InputText() and not for InputFloatX, InputIntX, InputDouble etc. */
interface inputKeyboard {


    fun inputText(label: String, buf: CharArray, flags: InputTextFlags = 0
                  , callback: InputTextCallback? = null, userData: Any? = null): Boolean {

        // TODO, enable callback and userData, related: https://github.com/kotlin-graphics/imgui/commit/082d94e359b2c262cd67c429bfff7fe3900d74cc
        assert(flags hasnt Itf.Multiline) { "call InputTextMultiline()" }
        return inputTextEx(label, null, buf, Vec2(), flags, callback, userData)
    }

    fun inputTextWithHint(label: String, hint: String, buf: CharArray, flags: InputTextFlags = 0
            /*, ImGuiInputTextCallback callback = NULL, void* user_data = NULL*/): Boolean {
        assert(flags hasnt Itf.Multiline) { "call InputTextMultiline()" }
        return inputTextEx(label, hint, buf, Vec2(), flags/*, callback, user_data*/)
    }

    fun inputTextMultiline(label: String, buf: CharArray, size: Vec2 = Vec2(), flags: InputTextFlags = 0
            /*,ImGuiTextEditCallback callback = NULL, void* user_data = NULL*/) =
            inputTextEx(label, null, buf, size, flags or Itf.Multiline/*, callback, user_data*/)

    fun inputFloat(label: String, v: FloatArray, step: Float = 0f, stepFast: Float = 0f, format: String? = null,
                   flags: InputTextFlags = 0) = inputFloat(label, v, 0, step, stepFast, format, flags)

    fun inputFloat(label: String, v: FloatArray, ptr: Int = 0, step: Float = 0f, stepFast: Float = 0f, format: String? = null,
                   flags: InputTextFlags = 0) = withFloat(v, ptr) { inputFloat(label, it, step, stepFast, format, flags) }

    fun inputFloat(label: String, v: KMutableProperty0<Float>, step: Float = 0f, stepFast: Float = 0f, format: String? = null,
                   flags_: InputTextFlags = 0): Boolean {
        val flags = flags_ or Itf.CharsScientific
        return inputScalar(label, DataType.Float, v, step.takeIf { it > 0f }, stepFast.takeIf { it > 0f }, format, flags)
    }

    fun inputDouble(label: String, v: KMutableProperty0<Double>, step: Double = 0.0, stepFast: Double = 0.0, format: String? = null,
                    flags_: InputTextFlags = 0): Boolean {
        val flags = flags_ or Itf.CharsScientific
        /*  Ideally we'd have a minimum decimal precision of 1 to visually denote that this is a float,
            while hiding non-significant digits? %f doesn't have a minimum of 1         */
        return inputScalar(label, DataType.Double, v, step.takeIf { it > 0.0 }, stepFast.takeIf { it > 0.0 }, format, flags)
    }

    fun inputFloat2(label: String, v: FloatArray, format: String? = null, flags: InputTextFlags = 0): Boolean =
            inputFloatN(label, v, 2, null, null, format, flags)

    fun inputVec2(label: String, v: Vec2, format: String? = null, flags: InputTextFlags = 0): Boolean {
        val floats = v to FloatArray(2)
        val res = inputFloatN(label, floats, 2, null, null, format, flags)
        v put floats
        return res
    }

    fun inputFloat3(label: String, v: FloatArray, format: String? = null, flags: InputTextFlags = 0)
            : Boolean = inputFloatN(label, v, 3, null, null, format, flags)

    fun inputVec3(label: String, v: Vec3, format: String? = null, flags: InputTextFlags = 0): Boolean {
        val floats = v to FloatArray(3)
        val res = inputFloatN(label, floats, 3, null, null, format, flags)
        v put floats
        return res
    }

    fun inputFloat4(label: String, v: FloatArray, format: String? = null, flags: InputTextFlags = 0)
            : Boolean = inputFloatN(label, v, 4, null, null, format, flags)

    fun inputVec4(label: String, v: Vec4, format: String? = null, flags: InputTextFlags = 0): Boolean {
        val floats = v to FloatArray(4)
        val res = inputFloatN(label, floats, 4, null, null, format, flags)
        v put floats
        return res
    }

    fun inputInt(label: String, v: KMutableProperty0<Int>, step: Int = 1, stepFast: Int = 100, flags: InputTextFlags = 0): Boolean {
        /*  Hexadecimal input provided as a convenience but the flag name is awkward. Typically you'd use inputText()
            to parse your own data, if you want to handle prefixes.             */
        val format = if (flags has Itf.CharsHexadecimal) "%08X" else "%d"
        return inputScalar(label, DataType.Int, v, step.takeIf { it > 0f }, stepFast.takeIf { it > 0f }, format, flags)
    }

    fun inputInt2(label: String, v: IntArray, flags: InputTextFlags = 0): Boolean =
            inputIntN(label, v, 2, null, null, "%d", flags)

    fun inputVec2i(label: String, v: Vec2i, flags: InputTextFlags = 0): Boolean {
        val ints = v to IntArray(2)
        val res = inputIntN(label, ints, 2, null, null, "%d", flags)
        v put ints
        return res
    }

    fun inputInt3(label: String, v: IntArray, flags: InputTextFlags = 0): Boolean =
            inputIntN(label, v, 3, null, null, "%d", flags)

    fun inputVec3i(label: String, v: Vec3i, flags: InputTextFlags = 0): Boolean {
        val ints = v to IntArray(3)
        val res = inputIntN(label, ints, 3, null, null, "%d", flags)
        v put ints
        return res
    }

    fun inputInt4(label: String, v: IntArray, flags: InputTextFlags = 0): Boolean =
            inputIntN(label, v, 4, null, null, "%d", flags)

    fun inputVec4i(label: String, v: Vec4i, flags: InputTextFlags = 0): Boolean {
        val ints = v to IntArray(4)
        val res = inputIntN(label, ints, 4, null, null, "%d", flags)
        v put ints
        return res
    }

    fun <N> inputScalar(label: String, dataType: DataType,
                        data: IntArray, step: Int?, stepFast: Int?,
                        format: String? = null, flags: InputTextFlags = 0): Boolean where N : Number, N : Comparable<N> =
            withInt(data, 0) { inputScalar(label, dataType, it, step, stepFast, format, flags) }

    //    @Suppress("UNCHECKED_CAST")
    fun <N> inputScalar(label: String, dataType: DataType,
                        dataPtr: KMutableProperty0<N>,
                        step: N? = null, stepFast: N? = null,
                        format_: String? = null, flags: InputTextFlags = 0): Boolean where N : Number, N : Comparable<N> {

        var data by dataPtr
        val window = currentWindow
        if (window.skipItems) return false

        val format = when (format_) {
            null -> when (dataType) {
                DataType.Float, DataType.Double -> "%f"
                else -> "%d"
            }
            else -> format_
        }

        val buf = dataPtr.format(dataType, format, 64)

        var valueChanged = false
        var extraFlags = flags
        if (extraFlags hasnt (Itf.CharsHexadecimal or Itf.CharsScientific))
            extraFlags = extraFlags or Itf.CharsDecimal
        extraFlags = extraFlags or Itf.AutoSelectAll

        if (step != null) {
            val buttonSize = frameHeight

            beginGroup() // The only purpose of the group here is to allow the caller to query item data e.g. IsItemActive()
            pushId(label)
            nextItemWidth = 1f max (nextItemWidth - (buttonSize + style.itemInnerSpacing.x) * 2)
            if (inputText("", buf, extraFlags)) // PushId(label) + "" gives us the expected ID from outside point of view
                valueChanged = dataTypeApplyOpFromText(buf, g.inputTextState.initialTextA, dataType, dataPtr, format)

            // Step buttons
            val backupFramePadding = Vec2(style.framePadding)
            style.framePadding.x = style.framePadding.y
            var buttonFlags = Bf.Repeat or Bf.DontClosePopups
            if (extraFlags has Itf.ReadOnly)
                buttonFlags = buttonFlags or Bf.Disabled
            sameLine(0f, style.itemInnerSpacing.x)
            if (buttonEx("-", Vec2(buttonSize), buttonFlags)) {
                data = dataTypeApplyOp(dataType, '-', data, stepFast?.takeIf { io.keyCtrl } ?: step)
                valueChanged = true
            }
            sameLine(0f, style.itemInnerSpacing.x)
            if (buttonEx("+", Vec2(buttonSize), buttonFlags)) {
                data = dataTypeApplyOp(dataType, '+', data, stepFast?.takeIf { io.keyCtrl } ?: step)
                valueChanged = true
            }
            sameLine(0f, style.itemInnerSpacing.x)
            textEx(label, findRenderedTextEnd(label))
            style.framePadding put backupFramePadding

            popId()
            endGroup()
        } else if (inputText(label, buf, extraFlags))
            valueChanged = dataTypeApplyOpFromText(buf, g.inputTextState.initialTextA, dataType, dataPtr, format)


        return valueChanged
    }

    fun <N> inputFloatN(label: String, v: FloatArray,
                        components: Int,
                        step: N? = null, stepFast: N? = null,
                        format: String? = null, flags: Int): Boolean where N : Number, N : Comparable<N> {
        val window = currentWindow
        if (window.skipItems) return false

        var valueChanged = false
        beginGroup()
        pushId(label)
        pushMultiItemsWidths(components, nextItemWidth)
        for (i in 0 until components) {
            pushId(i)
            withFloat(v, i) {
                val pN = it as KMutableProperty0<N>
                valueChanged = inputScalar("", DataType.Float, pN, step, stepFast, format, flags) || valueChanged
            }
            sameLine(0f, style.itemInnerSpacing.x)
            popId()
            popItemWidth()
        }
        popId()

        textEx(label, findRenderedTextEnd(label))
        endGroup()
        return valueChanged
    }

    fun inputIntN(label: String, v: IntArray, components: Int, step: Int? = null, stepFast: Int? = null, format: String? = null,
                  flags: Int): Boolean {
        val window = currentWindow
        if (window.skipItems) return false

        var valueChanged = false
        beginGroup()
        pushId(label)
        pushMultiItemsWidths(components, nextItemWidth)
        for (i in 0 until components) {
            pushId(i)
            withInt(v, i) { valueChanged = inputScalar("", DataType.Int, it, step, stepFast, format, flags) || valueChanged }
            sameLine(0f, style.itemInnerSpacing.x)
            popId()
            popItemWidth()
        }
        popId()

        textEx(label, findRenderedTextEnd(label))
        endGroup()
        return valueChanged
    }
}
