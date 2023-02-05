package imgui

import gli_.has
import uno.kotlin.NUL

var IMGUI_HAS_DOCK = false
var IMGUI_DEBUG_TEST_ENGINE = true

infix fun Byte.has(i: Int): Boolean = toInt().has(i)

fun <T> ArrayList<T>.pop(): T = removeLast()

class StringPointer(val string: String) {
    var pointer = 0
    operator fun get(index: Int) = string.getOrNul(pointer + index)
    operator fun invoke(): Int = pointer
    operator fun inc(): StringPointer {
        pointer++
        return this
    }
}

fun String.getOrNul(index: Int): Char = getOrElse(index) { NUL }

//operator fun Char.compareTo(int: Int) = toInt().compareTo(int)

operator fun Boolean.div(other: Boolean) = or(other)