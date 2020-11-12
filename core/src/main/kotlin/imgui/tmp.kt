package imgui

import gli_.has


infix fun Byte.has(i: Int): Boolean = toInt().has(i)

fun <T> ArrayList<T>.pop(): T = removeAt(lastIndex)