@file:Suppress("UNCHECKED_CAST")
@file:JvmName("ImGuiStaticFunctions")

package imgui

import gli_.has
import glm_.*
import glm_.vec2.Vec2
import imgui.ImGui.getNavInputAmount
import imgui.api.g
import imgui.classes.Window
import imgui.internal.*
import imgui.static.findWindowFocusIndex
import uno.kotlin.getValue
import uno.kotlin.isPrintable
import uno.kotlin.setValue
import kotlin.reflect.KMutableProperty0
import imgui.InputTextFlag as Itf
import imgui.WindowFlag as Wf


//-----------------------------------------------------------------------------
// Internal API exposed in imgui_internal.h
//-----------------------------------------------------------------------------











/** Return false to discard a character.    */
fun inputTextFilterCharacter(char: KMutableProperty0<Char>, flags: InputTextFlags, callback: InputTextCallback?, userData: Any?): Boolean {

    var c by char

    // Filter non-printable (NB: isprint is unreliable! see #2467) [JVM we can rely on custom ::isPrintable]
    if (c < 0x20 && !c.isPrintable) {
        var pass = false
        pass = pass or (c == '\n' && flags has Itf._Multiline)
        pass = pass or (c == '\t' && flags has Itf.AllowTabInput)
        if (!pass) return false
    }

    // We ignore Ascii representation of delete (emitted from Backspace on OSX, see #2578, #2817)
    if (c.i == 127)
        return false

    // Filter private Unicode range. GLFW on OSX seems to send private characters for special keys like arrow keys (FIXME)
    if (c >= 0xE000 && c <= 0xF8FF) return false

    // Generic named filters
    if (flags has (Itf.CharsDecimal or Itf.CharsHexadecimal or Itf.CharsUppercase or Itf.CharsNoBlank or Itf.CharsScientific)) {

        if (flags has Itf.CharsDecimal)
            if (c !in '0'..'9' && c != '.' && c != '-' && c != '+' && c != '*' && c != '/')
                return false

        if (flags has Itf.CharsScientific)
            if (c !in '0'..'9' && c != '.' && c != '-' && c != '+' && c != '*' && c != '/' && c != 'e' && c != 'E')
                return false

        if (flags has Itf.CharsHexadecimal)
            if (c !in '0'..'9' && c !in 'a'..'f' && c !in 'A'..'F')
                return false

        if (flags has Itf.CharsUppercase && c in 'a'..'z')
            c = c + ('A' - 'a') // cant += because of https://youtrack.jetbrains.com/issue/KT-14833

        if (flags has Itf.CharsNoBlank && c.isBlankW)
            return false
    }

    // Custom callback filter
    if (flags has Itf.CallbackCharFilter) {
        callback!! //callback is non-null from all calling functions
        val itcd = InputTextCallbackData()
        itcd.eventFlag = imgui.InputTextFlag.CallbackCharFilter.i
        itcd.eventChar = c
        itcd.flags = flags
        itcd.userData = userData

        if (callback(itcd))
            return false
        if (itcd.eventChar == NUL)
            return false
    }
    return true
}

fun inputTextCalcTextLenAndLineCount(text: CharArray, outTextEnd: KMutableProperty0<Int>): Int {

    var lineCount = 0
    var s = 0
    while (text.getOrElse(s++) { NUL } != NUL) // We are only matching for \n so we can ignore UTF-8 decoding
        if (text.getOrElse(s) { NUL } == '\n')
            lineCount++
    s--
    if (text[s] != '\n' && text[s] != '\r')
        lineCount++
    outTextEnd.set(s)
    return lineCount
}

fun inputTextCalcTextSizeW(text: CharArray, textBegin: Int, textEnd: Int, remaining: KMutableProperty0<Int>? = null,
                           outOffset: Vec2? = null, stopOnNewLine: Boolean = false): Vec2 {

    val font = g.font
    val lineHeight = g.fontSize
    val scale = lineHeight / font.fontSize

    val textSize = Vec2()
    var lineWidth = 0f

    var s = textBegin
    while (s < textEnd) {
        val c = text[s++]
        if (c == '\n') {
            textSize.x = glm.max(textSize.x, lineWidth)
            textSize.y += lineHeight
            lineWidth = 0f
            if (stopOnNewLine)
                break
            continue
        }
        if (c == '\r') continue
        // renaming ::getCharAdvance continuously every build because of bug, https://youtrack.jetbrains.com/issue/KT-19612
        val charWidth = font.getCharAdvance(c) * scale
        lineWidth += charWidth
    }

    if (textSize.x < lineWidth)
        textSize.x = lineWidth

    // offset allow for the possibility of sitting after a trailing \n
    outOffset?.let {
        it.x = lineWidth
        it.y = textSize.y + lineHeight
    }

    if (lineWidth > 0 || textSize.y == 0f)  // whereas size.y will ignore the trailing \n
        textSize.y += lineHeight

    remaining?.set(s)

    return textSize
}

// TODO check if needed
//fun IntArray.format(dataType: DataType, format: String, buf: CharArray): CharArray {
//    val value: Number = when (dataType) {
//        DataType.Int -> this[0]
//        DataType.Float -> glm.intBitsToFloat(this[0])
//        else -> throw Error()
//    }
//    return Format.format(style.locale, value).toCharArray(buf)
//}

/** JVM Imgui, dataTypeFormatString replacement TODO check if needed */
//fun IntArray.format(dataType: DataType, decimalPrecision: Int, buf: CharArray) = when (dataType) {
//
//    DataType.Int -> "%${if (decimalPrecision < 0) "" else ".$decimalPrecision"}d".format(style.locale, this[0])
///*  Ideally we'd have a minimum decimal precision of 1 to visually denote that it is a float, while hiding
//    non-significant digits?         */
//    DataType.Float -> "%${if (decimalPrecision < 0) "" else ".$decimalPrecision"}f".format(style.locale, glm.intBitsToFloat(this[0]))
//    DataType.Double -> TODO()//"%${if (decimalPrecision < 0) "" else ".$decimalPrecision"}f".format(style.locale, glm.intBitsToFloat(this[0]))
//    else -> throw Error("unsupported format data type")
//}.toCharArray(buf)
//
//fun KMutableProperty0<Number>.format(buf: CharArray, dataType: DataType, decimalPrecision: Int): Int { TODO REMOVE
//    val string = when {
//        decimalPrecision < 0 -> when (dataType) {
//            DataType.Int -> "%d".format(style.locale, this() as Int)
///*  Ideally we'd have a minimum decimal precision of 1 to visually denote that it is a float, while hiding
//    non-significant digits?         */
//            DataType.Float -> "%f".format(style.locale, this() as Float)
//            DataType.Double -> "%f".format(style.locale, this() as Double)
//            else -> throw Error("unsupported format data type")
//        }
//        else -> when (dataType) {
//            DataType.Int -> "%${decimalPrecision}d".format(style.locale, this() as Int)
//            DataType.Float -> "%${decimalPrecision}f".format(style.locale, this() as Float)
//            DataType.Double -> "%${decimalPrecision}g".format(style.locale, this() as Double)
//            else -> throw Error("unsupported format data type")
//        }
//    }
//    return string.toCharArray(buf).size
//}





fun isNavInputPressedAnyOfTwo(n1: NavInput, n2: NavInput, mode: InputReadMode) = getNavInputAmount(n1, mode) + getNavInputAmount(n2, mode) > 0f

// FIXME-OPT O(N)
fun findWindowNavFocusable(iStart: Int, iStop: Int, dir: Int): Window? {
    var i = iStart
    while (i in g.windowsFocusOrder.indices && i != iStop) {
        if (g.windowsFocusOrder[i].isNavFocusable)
            return g.windowsFocusOrder[i]
        i += dir
    }
    return null
}

fun navUpdateWindowingHighlightWindow(focusChangeDir: Int) {

    val target = g.navWindowingTarget!!
    if (target.flags has Wf._Modal) return

    val iCurrent = findWindowFocusIndex(target)
    val windowTarget = findWindowNavFocusable(iCurrent + focusChangeDir, -Int.MAX_VALUE, focusChangeDir)
            ?: findWindowNavFocusable(if (focusChangeDir < 0) g.windowsFocusOrder.lastIndex else 0, iCurrent, focusChangeDir)
    // Don't reset windowing target if there's a single window in the list
    windowTarget?.let {
        g.navWindowingTarget = it
        g.navWindowingTargetAnim = it
    }
    g.navWindowingToggleLayer = false
}