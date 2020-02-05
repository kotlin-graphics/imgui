package stb_

import glm_.i
import kool.lim
import kool.pos
import java.nio.ByteBuffer

class PtrChar(val array: CharArray, var offset: Int = 0) {

    operator fun get(index: Int): Char = array[offset + index]
    operator fun set(char: Char, index: Int) {
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

class PtrByte(val array: ByteArray, var offset: Int = 0) {

    operator fun get(index: Int): Byte = array[offset + index]
    operator fun set(byte: Byte, index: Int) {
        array[offset + index] = byte
    }

    operator fun plus(offset: Int) = PtrByte(array, this.offset + offset)
}

class PtrFloat(val array: FloatArray, var offset: Int = 0) {

    operator fun get(index: Int): Float = array[offset + index]
    operator fun set(float: Float, index: Int) {
        array[offset + index] = float
    }

    operator fun plus(offset: Int) = PtrFloat(array, this.offset + offset)

    fun fill(float: Float, num: Int) {
        for(i in 0 until num)
            set(float, i)
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