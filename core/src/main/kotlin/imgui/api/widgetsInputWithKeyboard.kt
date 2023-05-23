package imgui.api

import glm_.func.common.max
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec3.Vec3
import glm_.vec3.Vec3i
import glm_.vec4.Vec4
import glm_.vec4.Vec4i
import imgui.*
import imgui.ImGui.beginDisabled
import imgui.ImGui.beginGroup
import imgui.ImGui.buttonEx
import imgui.ImGui.calcItemWidth
import imgui.ImGui.currentWindow
import imgui.ImGui.endDisabled
import imgui.ImGui.endGroup
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.frameHeight
import imgui.ImGui.input
import imgui.ImGui.inputTextEx
import imgui.ImGui.io
import imgui.ImGui.markItemEdited
import imgui.ImGui.popID
import imgui.ImGui.pushID
import imgui.ImGui.sameLine
import imgui.ImGui.setNextItemWidth
import imgui.ImGui.style
import imgui.ImGui.textEx
import imgui.internal.api.widgetN
import imgui.internal.sections.IMGUI_TEST_ENGINE_ITEM_INFO
import kool.getValue
import kool.setValue
import kotlin.reflect.KMutableProperty0
import imgui.InputTextFlag as Itf
import imgui.internal.sections.ButtonFlag as Bf

/** Widgets: Input with Keyboard
 *  - If you want to use InputText() with std::string or any custom dynamic string type, see cpp/imgui_stdlib.h and comments in imgui_demo.cpp.
 *  - Most of the ImGuiInputTextFlags flags are only useful for InputText() and not for InputFloatX, InputIntX, InputDouble etc. */
interface widgetsInputWithKeyboard {

    /** String overload */
    fun inputText(
            label: String, pString: KMutableProperty0<String>, flags: InputTextSingleFlags = none,
            callback: InputTextCallback? = null, userData: Any? = null
    ): Boolean {
        val buf = pString.get().toByteArray()
        return inputText(label, buf, flags, callback, userData).also {
            pString.set(buf.cStr)
        }
    }

    /** String overload */
    fun inputText(
            label: String, buf: String, flags: InputTextSingleFlags = none,
            callback: InputTextCallback? = null, userData: Any? = null
    ): Boolean =
        inputText(label, buf.toByteArray(), flags, callback, userData)

    fun inputText(
            label: String, buf: StringBuilder, flags: InputTextSingleFlags = none,
            callback: InputTextCallback? = null, userData: Any? = null
    ): Boolean {
        val array = buf.toString().toByteArray()
        return inputText(label, array, flags, callback, userData).also {
            buf.clear()
//            buf.append(array.)
        }
    }

    fun inputText(
            label: String, buf: ByteArray, flags: InputTextSingleFlags = none,
            callback: InputTextCallback? = null, userData: Any? = null
    ): Boolean = inputTextEx(label, null, buf, Vec2(), flags, callback, userData)

    /** String overload */
    fun inputTextMultiline(
            label: String, buf: String, size: Vec2 = Vec2(), flags: InputTextSingleFlags = none,
            callback: InputTextCallback? = null, userData: Any? = null
    ): Boolean =
        inputTextEx(label, null, buf.toByteArray(), size, flags or Itf._Multiline, callback, userData)

    fun inputTextMultiline(
            label: String, buf: ByteArray, size: Vec2 = Vec2(), flags: InputTextSingleFlags = none,
            callback: InputTextCallback? = null, userData: Any? = null
    ): Boolean =
        inputTextEx(label, null, buf, size, flags or Itf._Multiline, callback, userData)

    /** String overload */
    fun inputTextWithHint(
            label: String, hint: String, buf: String, flags: InputTextSingleFlags = none,
            callback: InputTextCallback? = null, userData: Any? = null
    ): Boolean = inputTextWithHint(label, hint, buf.toByteArray(), flags)

    /** call InputTextMultiline() or InputTextEx() manually if you need multi-line + hint. */
    fun inputTextWithHint(label: String, hint: String, buf: ByteArray, flags: InputTextSingleFlags = none, callback: InputTextCallback? = null, userData: Any? = null): Boolean {
        return inputTextEx(label, hint, buf, Vec2(), flags, callback, userData)
    }

    fun input(label: String, v: FloatArray, step: Float = 0f, stepFast: Float = 0f, format: String = "%.3f", flags: InputTextSingleFlags = none): Boolean =
            input(label, v mutablePropertyAt 0, step, stepFast, format, flags)

    fun input(label: String, v: KMutableProperty0<Float>, step: Float = 0f, stepFast: Float = 0f, format: String = "%.3f", flags_: InputTextSingleFlags = none): Boolean {
        val flags = flags_ or Itf.CharsScientific
        return input(label, v, step.takeIf { it > 0f }, stepFast.takeIf { it > 0f }, format, flags)
    }

    fun input2(label: String, v: FloatArray, format: String = "%.3f", flags: InputTextSingleFlags = none): Boolean = inputN(label, 2, null, null, format, flags, v::mutablePropertyAt)

    fun input2(label: String, v: Vec2, format: String = "%.3f", flags: InputTextSingleFlags = none): Boolean = inputN(label, 2, null, null, format, flags, v::mutablePropertyAt)

    fun input2(label: String, v: Vec3, format: String = "%.3f", flags: InputTextSingleFlags = none): Boolean = inputN(label, 2, null, null, format, flags, v::mutablePropertyAt)

    fun input2(label: String, v: Vec4, format: String = "%.3f", flags: InputTextSingleFlags = none): Boolean = inputN(label, 2, null, null, format, flags, v::mutablePropertyAt)

    fun input3(label: String, v: FloatArray, format: String = "%.3f", flags: InputTextSingleFlags = none): Boolean = inputN(label, 3, null, null, format, flags, v::mutablePropertyAt)

    fun input3(label: String, v: Vec3, format: String = "%.3f", flags: InputTextSingleFlags = none): Boolean = inputN(label, 3, null, null, format, flags, v::mutablePropertyAt)

    fun input3(label: String, v: Vec4, format: String = "%.3f", flags: InputTextSingleFlags = none): Boolean = inputN(label, 3, null, null, format, flags, v::mutablePropertyAt)

    fun input4(label: String, v: FloatArray, format: String = "%.3f", flags: InputTextSingleFlags = none): Boolean = inputN(label, 4, null, null, format, flags, v::mutablePropertyAt)

    fun input4(label: String, v: Vec4, format: String = "%.3f", flags: InputTextSingleFlags = none): Boolean = inputN(label, 4, null, null, format, flags, v::mutablePropertyAt)

    fun input(label: String, v: KMutableProperty0<Int>, step: Int = 1, stepFast: Int = 100, flags: InputTextSingleFlags = none): Boolean {/*  Hexadecimal input provided as a convenience but the flag name is awkward. Typically you'd use inputText()
            to parse your own data, if you want to handle prefixes.             */
        val format = if (flags has Itf.CharsHexadecimal) "%08X" else "%d"
        return input(label, v, step.takeIf { it > 0f }, stepFast.takeIf { it > 0f }, format, flags)
    }

    fun input(label: String, pData: IntArray, step: Int?, stepFast: Int?, format: String? = null, flags: InputTextSingleFlags = none): Boolean = input(label, pData mutablePropertyAt 0, step, stepFast, format, flags)

    fun input2(label: String, v: IntArray, flags: InputTextSingleFlags = none): Boolean = inputN(label, 2, null, null, "%d", flags, v::mutablePropertyAt)

    fun input2(label: String, v: Vec2i, flags: InputTextSingleFlags = none): Boolean = inputN(label, 2, null, null, "%d", flags, v::mutablePropertyAt)

    fun input3(label: String, v: IntArray, flags: InputTextSingleFlags = none): Boolean = inputN(label, 3, null, null, "%d", flags, v::mutablePropertyAt)

    fun input3(label: String, v: Vec3i, flags: InputTextSingleFlags = none): Boolean = inputN(label, 3, null, null, "%d", flags, v::mutablePropertyAt)

    fun input4(label: String, v: IntArray, flags: InputTextSingleFlags = none): Boolean = inputN(label, 4, null, null, "%d", flags, v::mutablePropertyAt)

    fun input4(label: String, v: Vec4i, flags: InputTextSingleFlags = none): Boolean = inputN(label, 4, null, null, "%d", flags, v::mutablePropertyAt)

    fun input(label: String, v: KMutableProperty0<Double>, step: Double = 0.0, stepFast: Double = 0.0, format: String? = "%.6f", flags_: InputTextSingleFlags = none): Boolean {
        val flags = flags_ or Itf.CharsScientific/*  Ideally we'd have a minimum decimal precision of 1 to visually denote that this is a float,
            while hiding non-significant digits? %f doesn't have a minimum of 1         */
        return input(label, v, step.takeIf { it > 0.0 }, stepFast.takeIf { it > 0.0 }, format, flags)
    }

    fun <N> NumberOps<N>.input(label: String, pData: KMutableProperty0<N>, step: N? = null, stepFast: N? = null, format_: String? = null, flags_: InputTextSingleFlags = none): Boolean where N : Number, N : Comparable<N> {
        var data by pData
        val window = currentWindow
        if (window.skipItems) return false

        val format = format_ ?: defaultFormat

        val buf = data.format(format).toByteArray(64)

        var flags = flags_
        // Testing ActiveId as a minor optimization as filtering is not needed until active
        if (g.activeId == 0 && flags hasnt (Itf.CharsDecimal or Itf.CharsHexadecimal or Itf.CharsScientific)) flags /= defaultInputCharsFilter(format)
        flags /= Itf.AutoSelectAll or Itf._NoMarkEdited // We call MarkItemEdited() ourselves by comparing the actual data rather than the string.

        var valueChanged = false
        if (step != null) {
            val buttonSize = frameHeight

            beginGroup() // The only purpose of the group here is to allow the caller to query item data e.g. IsItemActive()
            pushID(label)
            setNextItemWidth(1f max (calcItemWidth() - (buttonSize + style.itemInnerSpacing.x) * 2))
            if (inputText("", buf, flags)) // PushId(label) + "" gives us the expected ID from outside point of view
                valueChanged = pData.applyFromText(buf.cStr, format)
            IMGUI_TEST_ENGINE_ITEM_INFO(g.lastItemData.id, label, g.lastItemData.statusFlags)

            // Step buttons
            val backupFramePadding = Vec2(style.framePadding)
            style.framePadding.x = style.framePadding.y
            val buttonFlags = Bf.Repeat or Bf.DontClosePopups
            if (flags has Itf.ReadOnly)
                beginDisabled()
            sameLine(0f, style.itemInnerSpacing.x)
            if (buttonEx("-", Vec2(buttonSize), buttonFlags)) {
                data -= stepFast?.takeIf { io.keyCtrl } ?: step
                valueChanged = true
            }
            sameLine(0f, style.itemInnerSpacing.x)
            if (buttonEx("+", Vec2(buttonSize), buttonFlags)) {
                data += stepFast?.takeIf { io.keyCtrl } ?: step
                valueChanged = true
            }
            if (flags has Itf.ReadOnly)
                endDisabled()

            val labelEnd = findRenderedTextEnd(label)
            if (0 != labelEnd) {
                sameLine(0f, style.itemInnerSpacing.x)
                textEx(label, labelEnd)
            }
            style.framePadding put backupFramePadding

            popID()
            endGroup()
        } else if (inputText(label, buf, flags)) valueChanged = pData.applyFromText(buf.cStr, format)

        if (valueChanged) markItemEdited(g.lastItemData.id)

        return valueChanged
    }

}

inline fun <reified N> input(label: String, pData: KMutableProperty0<N>, step: N? = null, stepFast: N? = null, format_: String? = null, flags_: InputTextSingleFlags = none): Boolean where N : Number, N : Comparable<N> = numberOps<N>().input(label, pData, step, stepFast, format_, flags_)

inline fun <reified N> inputN(label: String, components: Int, step: N? = null, stepFast: N? = null, format: String? = null, flags: InputTextSingleFlags = none, properties: (Int) -> KMutableProperty0<N>): Boolean where N : Number, N : Comparable<N> = numberOps<N>().inputN(label, components, step, stepFast, format, flags, properties)

inline fun <N> NumberOps<N>.inputN(label: String, components: Int, step: N? = null, stepFast: N? = null, format: String? = null, flags: InputTextSingleFlags = none, properties: (Int) -> KMutableProperty0<N>): Boolean where N : Number, N : Comparable<N> = widgetN(label, components) { i ->
    input("", properties(i), step, stepFast, format, flags)
}