package stb_

import glm_.b
import glm_.i
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