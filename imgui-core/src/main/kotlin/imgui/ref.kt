package imgui

import kotlin.reflect.KMutableProperty0


private var iPtr = 0
private var fPtr = 0
private var bPtr = 0
private var cPtr = 0

private val size = 1024

private val ints = IntArray(size)
private val floats = FloatArray(size)
private val bools = BooleanArray(size)
private val chars = CharArray(size)

private var int: Int
    get() = ints[iPtr]
    set(value) = ints.set(iPtr, value)
private var float: Float
    get() = floats[fPtr]
    set(value) = floats.set(fPtr, value)
private var bool: Boolean
    get() = bools[bPtr]
    set(value) = bools.set(bPtr, value)
private var char: Char
    get() = chars[cPtr]
    set(value) = chars.set(cPtr, value)

fun <R> withBoolean(bools: BooleanArray, ptr: Int = 0, block: (KMutableProperty0<Boolean>) -> R): R {
    bPtr++
    val bool = ::bool
    bool.set(bools[ptr])
    return block(bool).also {
        bools[ptr] = bool()
        bPtr--
    }
}

fun <R> withFloat(floats: FloatArray, ptr: Int, block: (KMutableProperty0<Float>) -> R): R {
    fPtr++
    val f = ::float
    f.set(floats[ptr])
    return block(f).also {
        floats[ptr] = f()
        fPtr--
    }
}

fun <R> withInt(pInt: IntArray, block: (KMutableProperty0<Int>) -> R): R = withInt(pInt, 0, block)
fun <R> withInt(pInt: IntArray, ptr: Int, block: (KMutableProperty0<Int>) -> R): R {
    iPtr++
    val i = ::int
    i.set(pInt[ptr])
    return block(i).also {
        pInt[ptr] = i()
        iPtr--
    }
}

fun <R> withInt(block: (KMutableProperty0<Int>) -> R): R {
    iPtr++
    return block(::int).also { iPtr-- }
}

fun <R> withChar(block: (KMutableProperty0<Char>) -> R): R {
    cPtr++
    return block(::char).also { cPtr-- }
}

fun <R> withChar(char: Char, block: (KMutableProperty0<Char>) -> R): R {
    cPtr++
    imgui.char = char
    return block(::char).also { cPtr-- }
}

fun <R> withBool(block: (KMutableProperty0<Boolean>) -> R): R {
    bPtr++
    return block(::bool).also { bPtr-- }
}

fun <R> withBool(boolean: Boolean, block: (KMutableProperty0<Boolean>) -> R): Boolean {
    bPtr++
    bool = boolean
    block(::bool)
    return bool.also { bPtr-- }
}


// static arrays to avoid GC pressure
val _fa = FloatArray(4)
val _fa2 = FloatArray(4)
val _ia = IntArray(4)


// static referencable
var _b = false
var _i = 0
var _L = 0L
var _d = 0.0
var _f = 0f
var _f1 = 0f
var _f2 = 0f
var _c = NUL