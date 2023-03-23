package imgui.font

import com.aayushatharva.brotli4j.Brotli4jLoader
import com.aayushatharva.brotli4j.encoder.Encoder
import com.ibm.icu.text.BreakIterator
import glm_.asHexString
import glm_.i
import glm_.minus
import java.io.File
import java.math.BigInteger


fun main() {
    val ini = File("core/src/main/resources/fonts/ProggyClean.ttf")
    val array = ini.readBytes().asUByteArray()
//    encode(ubyteArrayOf(0u, 1u, 2u, 3u))

    // Load the native library
    Brotli4jLoader.ensureAvailability();

    // Compress data and get output in byte array
    val compressed = ini.readBytes()// Encoder.compress(ini.readBytes(), Encoder.Parameters().setQuality(11))//.take(4).toByteArray()

    println()

//    val n = stb.compress(CharArray(compressed.size) { compressed[it].toChar() })

    val range = (0xD7FF + 1 - 0x0000) + (0xFFFF + 1 - 0xE000) + (Char.MAX_HIGH_SURROGATE.i + 1 - Char.MIN_SURROGATE.i) * (Char.MAX_SURROGATE.i + 1 - Char.MIN_LOW_SURROGATE.i)

    for (i in 0 until range) {
        val code = encode(i)
        val plain = decode(code)
        if (i != plain)
            error("$i != $plain")
    }


    val bigInteger = BigInteger(compressed)
    var div = bigInteger
    val res = StringBuilder()
    while (div > BigInteger.ZERO) {
        val (d, r) = div.divideAndRemainder(range.toBigInteger())
        div = d
        val rem = r.toInt()
        res.insert(0, encode(rem))
    }

    var value = BigInteger.ZERO
    var i = 0
    while (i < res.length) {
        val s = when (val c = res[i++]) {
            in Char.MIN_SURROGATE..Char.MAX_SURROGATE -> c.toString() + res[i++]
            else -> c.toString()
        }
        value = value * range.toBigInteger() + decode(s).toBigInteger()
    }

    val equal = bigInteger == value
    println(equal)
}

fun encode(int: Int): String = when {
    int <= 0xD7FF -> int.toChar().toString()
    int <= 0xD7FF + (0xFFFF + 1 - 0xE000) -> (int + (0xE000 - Char.MIN_SURROGATE.i)).toChar().toString()
    int <= 0xD7FF + (0xFFFF + 1 - 0xE000) + (Char.MAX_HIGH_SURROGATE.i + 1 - Char.MIN_SURROGATE.i) * (Char.MAX_SURROGATE.i + 1 - Char.MIN_LOW_SURROGATE.i) -> {
        val i = int - (0xD7FF + (0xFFFF + 1 - 0xE000))
        val chunk = Char.MAX_SURROGATE.i + 1 - Char.MIN_LOW_SURROGATE.i
        charArrayOf(Char.MIN_SURROGATE + i / chunk, Char.MIN_LOW_SURROGATE + i % chunk).concatToString()
    }

    else -> error("int = $int")
}

fun decode(string: String): Int = when (string.length) {
    1 -> {
        val code = string[0].code
        when {
            code <= 0xD7FF -> code
            code in 0xE000..0xFFFF -> code - (0xE000 - Char.MIN_SURROGATE.i)
            else -> error("")
        }
    }

    2 -> {
        val a = string[0].code - Char.MIN_SURROGATE
        val b = string[1].code - Char.MIN_LOW_SURROGATE
        val chunk = Char.MAX_SURROGATE.i + 1 - Char.MIN_LOW_SURROGATE.i
        val code = a * chunk + b
        check(code <= (Char.MAX_HIGH_SURROGATE.i + 1 - Char.MIN_SURROGATE.i) * (Char.MAX_SURROGATE.i + 1 - Char.MIN_LOW_SURROGATE.i))
        code + 0xD7FF + (0xFFFF + 1 - 0xE000)
    }

    else -> error("")
}

fun getLength(emoji: String?): Int {
    val it: BreakIterator = BreakIterator.getCharacterInstance()
    it.setText(emoji)
    var emojiCount = 0
    while (it.next() != BreakIterator.DONE)
        emojiCount++
    return emojiCount
}

val codepoints = Char.MIN_SURROGATE.i..Char.MAX_HIGH_SURROGATE.i to Char.MIN_LOW_SURROGATE.i..Char.MAX_SURROGATE.i