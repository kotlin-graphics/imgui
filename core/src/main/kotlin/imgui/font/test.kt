package imgui.font

import imgui.stb_.TrueType.ULong
import imgui.stb_.i
import imgui.stb_.ul
import java.io.File


fun main() {
    val ini = File("imgui.ini")
    val a = ini.readBytes()
//    encode(ubyteArrayOf(0u, 1u, 2u, 3u))
}

fun base94Encode(plain: UByteArray, code: CharArray) {

    // high * 2^64 | low

    var value = plain.ULong(1)

    // 2^64 = 2087680406712262 * 94^2 + 4584

    val remainder = value % 8836u + plain[0].ul * 4584u
    value = value / 8836u + plain[0] * 2087680406712262uL

    code[10] = (33u + remainder % 94u)
    remainder /= 94
    code[9]  = 33 + remainder % 94
    value += remainder / 94

    for (i = 8; i >= 0; --i) {
        code[i] = 33 + value % 94
        value /= 94
    }

}

//fun encode(array: UByteArray): String {
//
//    var p = 1
//    val result = ArrayList<Char>()
//
//    var maxValues = 255
//    var maxRemainder = 0
//
//    while (p < array.size) {
//
//        if (maxValues + maxRemainder >= 95 * 3) {
//
//        }
//        if (maxValues + maxRemainder >= 95 * 2) {
//            val times = maxValues / 95 // 2
//            val remainder = maxValues % 95 // 65
//            val tmp = maxValues - remainder //
//        }
//        else {
////            val next = next()
//
//        }
//    }
//
//    return result.joinToString("")
//}

/*
Dec -> Base 95
0 -> ` `
95 -> ~
96 -> `! `
 */