package stb_

import glm_.i
import kool.lim
import kool.pos
import kool.rem
import java.nio.ByteBuffer


// private structure
//typedef struct
//{
//    unsigned char *data;
//    int cursor;
//    int size;
//} stbtt__buf;

/////////////////////////////////////////////////////////////////////////
//
// stbtt__buf helpers to parse data from file
//

/** ~stbtt__buf_get8 */
fun ByteBuffer.get8(): Byte = when {
    rem >= 0 -> get()
    else -> 0
}

/** ~stbtt__buf_peek8 */
fun ByteBuffer.peek8(): Byte = when {
    rem >= 0 -> get(pos)
    else -> 0
}

/** ~stbtt__buf_seek */
infix fun ByteBuffer.seek(o: Int) {
    assert(o in 0..lim)
    pos = if (o > lim || o < 0) lim else o
}

/** ~stbtt__buf_skip */
infix fun ByteBuffer.skip(o: Int) = seek(pos + o)

/** ~stbtt__buf_get */
infix fun ByteBuffer.getN(n: Int): Int = when (n) {
    1 -> get().i
    2 -> short.i
    4 -> int
    else -> error("invalid")
}

/** ~stbtt__new_buf */
fun newBuf(p: ByteBuffer? = null, size: Int = 0): ByteBuffer {
    assert(size < 0x40000000)
    return when (p) {
        null -> ByteBuffer.allocate(0)
        else -> p
    }
}

fun ByteBuffer.get16(): Int = getN(2)
fun ByteBuffer.get32(): Int = getN(4)

/** ~stbtt__buf_range */

fun ByteBuffer.range(o: Int, s: Int): ByteBuffer = when {
    o < 0 || s < 0 || o > lim || s > lim - o -> newBuf()
    else -> duplicate().also {
        it.pos += o
        it.lim = s
    }
}

/** ~stbtt__cff_get_index */
fun ByteBuffer.cffGetIndex(): ByteBuffer {
    val start = pos
    val count = get16()
    if (count != 0) {
        val offsize = get8().i
        assert(offsize in 1..4)
        skip(offsize * count)
        skip(getN(offsize) - 1)
    }
    return range(start, pos - start)
}

/** ~stbtt__cff_int */
val ByteBuffer.cffInt: Int
    get() = when (val b0 = get().i) {
        in 32..246 -> b0 - 139
        in 247..250 -> (b0 - 247) * 256 + get8() + 108
        in 251..254 -> -(b0 - 251) * 256 - get8() - 108
        28 -> get16()
        29 -> get32()
        else -> error("")
    }

/** ~stbtt__cff_skip_operand */
fun ByteBuffer.cffSkipOperand() {
    val b0 = peek8().i
    assert(b0 >= 28)
    if (b0 == 30) {
        skip(1)
        while (pos < lim) {
            val v = get8().i
            if ((v and 0xF) == 0xF || (v shr 4) == 0xF)
                break
        }
    } else cffInt
}

/** ~stbtt__dict_get */
fun ByteBuffer.dictGet(key: Int): ByteBuffer {
    seek(0)
    while (pos < lim) {
        val start = pos
        while (peek8() >= 28)
            cffSkipOperand()
        val end = pos
        var op = get8().i
        if (op == 12) op = get8().i or 0x100
        if (op == key) return range(start, end - start)
    }
    return range(0, 0)
}

fun ByteBuffer.dictGetInts(key: Int, out: IntArray) {
    val operands = dictGet(key)
    var i = 0
    while (i < out.size && operands.pos < operands.lim)
        out[i++] = operands.cffInt
}

fun ByteBuffer.dictGetInt(key: Int, backup: Int = 0): Int {
    val operands = dictGet(key)
    return when {
        operands.pos < operands.lim -> operands.cffInt
        else -> backup
    }
}

val ByteBuffer.cffIndexCount: Int
    get() {
        seek(0)
        return get16()
    }

infix fun ByteBuffer.cffIndexGet(i: Int): ByteBuffer {
    seek(0)
    val count = get16()
    val offsize = get8().i
    assert(i in 0 until count)
    assert(offsize in 1..4)
    skip(i * offsize)
    val start = getN(offsize)
    val end = getN(offsize)
    return range(2 + (count + 1) * offsize + start, end - start)
}