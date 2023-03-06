package imgui.stb_

import kool.cap
import kool.lim
import kool.pos
import unsigned.toUInt
import java.nio.ByteBuffer

/** private structure */
class Buf(var data: UByteArray = UByteArray(0), val ptr: Int = 0) {

    constructor(buf: Buf) : this(buf.data) {
        cursor = buf.cursor
    }

    init {
        assert(data.size < 0x40000000)
    }

    var cursor = 0

//    fun hasRemaining() = data.hasRemaining()

    fun isEmpty() = data.isEmpty()
    fun isNotEmpty() = data.isNotEmpty()
    val size get() = data.size

    //////////////////////////////////////////////////////////////////////////
    //
    // stbtt__buf helpers to parse data from file
    //

    fun get8(): UByte = when {
        cursor >= size -> 0u
        else -> data[cursor++]
    }

    fun peek8(): UByte = when {
        cursor >= size -> 0u
        else -> data[cursor]
    }

    infix fun seek(o: Int) {
        assert(o in data.indices)
        cursor = if (o !in data.indices) size else o
    }

    infix fun skip(o: Int) = seek(cursor + o)

    fun get(n: Int): UInt {
        var v = 0u
        assert(n in 1..4)
        for (i in 0 until n)
            v = (v shl 8) or get8().ui
        return v
    }

    // stbtt__new_buf -> Buf constructor

    fun get16(): UInt = get(2)
    fun get32(): UInt = get(4)

    fun range(o: Int, s: Int): Buf {
        TODO()
//        val r = Buf()
//        if (o < 0 || s < 0 || o > size || s > size - o) return Buf()
//        else Buf(data, o
//        r.size = s
//        return r
//        return when {
//            o < 0 || s < 0 || o > data.lim || s > data.lim - o -> Buf()
//            else -> Buf(data.sliceAt(o, s))
//        }
    }

    fun cffGetIndex(): Buf {
        TODO()
//        val start = pos
//        val count = get16()
//        if (count != 0) {
//            val offsize = get8()
//            assert(offsize in 1..4)
//            skip(offsize * count)
//            skip(get(offsize) - 1)
//        }
//        return range(start, pos - start)
    }

    val cffInt: Int
        get() = TODO()/* when (val b0 = get8()) {
            in 32..246 -> b0 - 139
            in 247..250 -> (b0 - 247) * 256 + get8() + 108
            in 251..254 -> -(b0 - 251) * 256 - get8() - 108
            28 -> get16()
            29 -> get32()
            else -> error("")
        }*/

    fun cffSkipOperand() {
        val b0 = peek8()
        TODO()
//        assert(b0 >= 28)
//        if (b0 == 30) {
//            skip(1)
//            while (hasRemaining()) {
//                val v = get8()
//                if ((v and 0xF) == 0xF || (v shl 4) == 0xF)
//                    break
//            }
//        } else cffInt
    }

    infix fun dictGet(key: Int): Buf {
        TODO()
//        seek(0)
//        while (hasRemaining()) {
//            val start = pos//, end, op
//            while (peek8() >= 28)
//                cffSkipOperand()
//            val end = pos
//            var op = get8()
//            if (op == 12) op = get8() or 0x100
//            if (op == key) return range(start, end - start)
//        }
//        return range(0, 0)
    }

    fun dictGetInts(key: Int, out: IntArray) {
        val operands = dictGet(key)
        var i = 0
        TODO()
//        while (i < out.size && operands.hasRemaining())
//            out[i++] = operands.cffInt
    }

    fun dictGetInt(key: Int, default: Int = 0): Int {
        val operands = dictGet(key)
        TODO()
//        return when {
//            operands.hasRemaining() -> operands.cffInt
//            else -> default
//        }
    }

    val cffIndexCount: Int
        get() {
            TODO()
//            seek(0)
//            return get16()
        }

    infix fun cffIndexGet(i: Int): Buf {
        seek(0)
        TODO()
//        val count = get16()
//        val offsize = get8()
//        assert(i in 0 until count)
//        assert(offsize in 1..4)
//        skip(i * offsize)
//        val start = get(offsize)
//        val end = get(offsize)
//        return range(2 + (count + 1) * offsize + start, end - start)
    }
}