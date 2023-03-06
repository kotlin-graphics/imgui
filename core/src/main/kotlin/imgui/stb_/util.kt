package imgui.stb_

import glm_.b
import glm_.i
import glm_.shl
import kool.lim
import kool.pos
import java.nio.ByteBuffer

class PtrChar(val array: CharArray, var offset: Int = 0) {

    operator fun get(index: Int): Char = array[offset + index]
    operator fun set(index: Int, char: Char) {
        array[offset + index] = char
    }

    operator fun plus(offset: Int) = PtrChar(array, this.offset + offset)
}

class PString(val array: CharSequence, var offset: Int = 0) {

    operator fun get(index: Int): Char = array[offset + index]

    val isValid: Boolean
        get() = offset in array.indices

    operator fun minus(int: Int): Int = array[offset].i - int

    operator fun invoke(): Char = array[offset]

    fun next() {
        offset++
    }
}

fun PtrByte(size: Int) = PtrByte(ByteArray(size))

class PtrByte(val array: ByteArray, var offset: Int = 0) {

    operator fun get(index: Int): Byte = array[offset + index]
    operator fun set(index: Int, byte: Byte) {
        array[offset + index] = byte
    }

    operator fun plus(offset: Int) = PtrByte(array, this.offset + offset)

    fun fill(int: Int, num: Int) {
        for (i in 0 until num)
            set(i, int.b)
    }
}

fun PtrFloat(size: Int) = PtrFloat(FloatArray(size))

class PtrFloat(val array: FloatArray, var offset: Int = 0) {

    operator fun get(index: Int): Float = array[offset + index]
    operator fun set(index: Int, float: Float) {
        array[offset + index] = float
    }

    operator fun plus(offset: Int) = PtrFloat(array, this.offset + offset)
    operator fun minus(offset: Int) = PtrFloat(array, this.offset - offset)

    fun fill(float: Float, num: Int) {
        for (i in 0 until num)
            set(i, float)
    }
}

fun ByteBuffer.sliceAt(offset: Int, size: Int = lim - offset): ByteBuffer {
    val backup = pos
    pos = offset
    return slice().also {
        pos = backup
        it.lim = size
    }
}

internal val Int.ub get() = toUByte()
internal val Int.us get() = toUShort()
internal val Int.ui get() = toUInt()
internal val UByte.i get() = toInt()
internal val UByte.f get() = toFloat()
internal val UByte.us get() = toUShort()
internal val UByte.ui get() = toUInt()
internal val UByte.ul get() = toULong()
internal infix fun UByte.has(i: Int) = and(i.toUByte()).i != 0
internal infix fun UByte.hasnt(i: Int) = !has(i)
internal val UInt.f get() = toFloat()
internal val UInt.ub get() = toUByte()
internal val UInt.us get() = toUShort()
internal val UInt.ul get() = toULong()
internal val UInt.i get() = toInt()
internal val UShort.i get() = toInt()
internal val UShort.ui get() = toUInt()
internal val ULong.i get() = toInt()
internal val ULong.ui get() = toUInt()
//internal infix fun UByte.shl(i: Int) = toInt().shl(i).toUInt()
internal infix fun UShort.shr(i: Int) = toUInt().shr(i).toUShort(
