package imgui

import gli_.has
import uno.kotlin.NUL


infix fun Byte.has(i: Int): Boolean = toInt().has(i)

fun <T> ArrayList<T>.pop(): T = removeAt(lastIndex)

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