package stb_

class PtrChar(val array: CharArray, var offset: Int = 0) {

    operator fun get(index: Int): Char = array[offset + index]
    operator fun set(char: Char, index: Int) {
        array[offset + index] = char
    }

    operator fun plus(offset: Int) = PtrChar(array, this.offset + offset)
}

class PtrByte(val array: ByteArray, var offset: Int = 0) {

    operator fun get(index: Int): Byte = array[offset + index]
    operator fun set(byte: Byte, index: Int) {
        array[offset + index] = byte
    }

    operator fun plus(offset: Int) = PtrByte(array, this.offset + offset)
}