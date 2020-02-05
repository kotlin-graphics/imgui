package imgui

import glm_.b
import glm_.i

infix fun Char.shr(i: Int) = toInt() shr i
infix fun Char.and(i: Int) = toInt() and i

fun decode85Byte(c: Char) = (c - if (c >= '\\') 36 else 35).i

fun decode85(src: String): ByteArray {
    var s = 0
    val dst = ByteArray(((src.length + 4) / 5) * 4)
    var d = 0
    while (s < src.lastIndex) {
        val tmp = decode85Byte(src[s]) + 85 * (decode85Byte(src[s + 1]) + 85 * (decode85Byte(src[s + 2]) + 85 * (decode85Byte(src[s + 3]) + 85 * decode85Byte(src[s + 4]))))
        dst[d] = tmp.b; dst[d + 1] = (tmp ushr 8).b; dst[d + 2] = (tmp ushr 16).b; dst[d + 3] = (tmp ushr 24).b   // We can't assume little-endianness.
        s += 5
        d += 4
    }
    return dst
}