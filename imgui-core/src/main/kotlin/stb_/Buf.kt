package stb_

import kool.cap
import kool.lim
import kool.pos
import unsigned.toUInt
import java.nio.ByteBuffer

/** private structure */
class Buf(var data: ByteBuffer = ByteBuffer.allocate(0)) {

    init {
        assert(data.cap < 0x40000000)
    }

    fun hasRemaining() = data.hasRemaining()
    val lim get() = data.lim
    var pos
        get() = data.pos
        set(value) {
            data.pos = value
        }

    fun isEmpty() = !hasRemaining()

    //////////////////////////////////////////////////////////////////////////
    //
    // stbtt__buf helpers to parse data from file
    //

    fun get8(): Int = when {
        !data.hasRemaining() -> 0
        else -> data.get().toUInt()
    }

    fun peek8(): Int = when {
        !data.hasRemaining() -> 0
        else -> data.get(data.pos).toUInt()
    }

    infix fun seek(o: Int) {
        assert(o in 0..data.lim)
        data.pos = if (o > data.lim || o < 0) data.lim else o
    }

    infix fun skip(o: Int) = seek(data.pos + o)

    fun get(n: Int): Int = when (n) {
        1 -> data.get().toUInt()
        2 -> data.short.toUInt()
        4 -> data.int
        else -> error("Invalid")
    }

    fun get16(): Int = data.short.toUInt()
    fun get32(): Int = data.int

    fun range(o: Int, s: Int): Buf = when {
        o < 0 || s < 0 || o > data.lim || s > data.lim - o -> Buf()
        else -> Buf(data.sliceAt(o, s))
    }

    fun cffGetIndex(): Buf {
        val start = pos
        val count = get16()
        if (count != 0) {
            val offsize = get8()
            assert(offsize in 1..4)
            skip(offsize * count)
            skip(get(offsize) - 1)
        }
        return range(start, pos - start)
    }

    val cffInt: Int
        get() = when (val b0 = get8()) {
            in 32..246 -> b0 - 139
            in 247..250 -> (b0 - 247) * 256 + get8() + 108
            in 251..254 -> -(b0 - 251) * 256 - get8() - 108
            28 -> get16()
            29 -> get32()
            else -> error("")
        }

    fun cffSkipOperand() {
        val b0 = peek8()
        assert(b0 >= 28)
        if (b0 == 30) {
            skip(1)
            while (hasRemaining()) {
                val v = get8()
                if ((v and 0xF) == 0xF || (v shl 4) == 0xF)
                    break
            }
        } else cffInt
    }

    infix fun dictGet(key: Int): Buf {
        seek(0)
        while (hasRemaining()) {
            val start = pos//, end, op
            while (peek8() >= 28)
                cffSkipOperand()
            val end = pos
            var op = get8()
            if (op == 12) op = get8() or 0x100
            if (op == key) return range(start, end - start)
        }
        return range(0, 0)
    }

    fun dictGetInts(key: Int, out: IntArray) {
        val operands = dictGet(key)
        var i = 0
        while (i < out.size && operands.hasRemaining())
            out[i++] = operands.cffInt
    }

    fun dictGetInt(key: Int, default: Int = 0): Int {
        val operands = dictGet(key)
        return when {
            operands.hasRemaining() -> operands.cffInt
            else -> default
        }
    }

    val cffIndexCount: Int
        get() {
            seek(0)
            return get16()
        }

    infix fun cffIndexGet(i: Int): Buf {
        seek(0)
        val count = get16()
        val offsize = get8()
        assert(i in 0 until count)
        assert(offsize in 1..4)
        skip(i * offsize)
        val start = get(offsize)
        val end = get(offsize)
        return range(2 + (count + 1) * offsize + start, end - start)
    }
}