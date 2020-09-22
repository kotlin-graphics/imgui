package imgui

import glm_.*
import unsigned.Ubyte
import unsigned.Uint
import unsigned.Ulong
import unsigned.Ushort
import java.math.BigInteger
import kotlin.div
import kotlin.minus
import kotlin.plus
import kotlin.times
import imgui.internal.lerp as ilerp

@Suppress("UNCHECKED_CAST")

infix operator fun <N> N.plus(other: N): N where N : Number, N : Comparable<N> = when {
    this is Byte && other is Byte -> (this + other).b
    this is Ubyte && other is Ubyte -> (this + other).b
    this is Short && other is Short -> (this + other).s
    this is Ushort && other is Ushort -> (this + other).s
    this is Int && other is Int -> this + other
    this is Uint && other is Uint -> this + other
    this is Long && other is Long -> this + other
    this is Ulong && other is Ulong -> this + other
    this is Float && other is Float -> this + other
    this is Double && other is Double -> this + other
    this is BigInteger && other is BigInteger -> this + other
    else -> error("Invalid operand types")
} as N

infix operator fun <Type> Type.minus(other: Type): Type
        where Type : Number, Type : Comparable<Type> = when {
    this is Byte && other is Byte -> this - other
    this is Ubyte && other is Ubyte -> this - other
    this is Short && other is Short -> this - other
    this is Ushort && other is Ushort -> this - other
    this is Int && other is Int -> this - other
    this is Uint && other is Uint -> this - other
    this is Long && other is Long -> this - other
    this is Ulong && other is Ulong -> this - other
    this is Float && other is Float -> this - other
    this is Double && other is Double -> this - other
    this is BigInteger && other is BigInteger -> this - other
    else -> error("Invalid operand types")
} as Type

infix operator fun <N> N.times(other: N): N where N : Number, N : Comparable<N> = when {
    this is Byte && other is Byte -> (this * other).b
    this is Ubyte && other is Ubyte -> (this * other).b
    this is Short && other is Short -> (this * other).s
    this is Ushort && other is Ushort -> (this * other).s
    this is Int && other is Int -> this * other
    this is Uint && other is Uint -> this * other
    this is Long && other is Long -> this * other
    this is Ulong && other is Ulong -> this * other
    this is Float && other is Float -> this * other
    this is Double && other is Double -> this * other
    this is BigInteger && other is BigInteger -> this * other
    else -> error("Invalid operand types")
} as N

infix operator fun <N> N.div(other: N): N where N : Number, N : Comparable<N> = when {
    this is Byte && other is Byte -> (this / other).b
    this is Ubyte && other is Ubyte -> (this / other).b
    this is Short && other is Short -> (this / other).s
    this is Ushort && other is Ushort -> (this / other).s
    this is Int && other is Int -> this / other
    this is Uint && other is Uint -> this / other
    this is Long && other is Long -> this / other
    this is Ulong && other is Ulong -> this / other
    this is Float && other is Float -> this / other
    this is Double && other is Double -> this / other
    this is BigInteger && other is BigInteger -> this / other
    else -> error("Invalid operand types")
} as N


infix operator fun <N> N.plus(float: Float): N where N : Number, N : Comparable<N> = when (this) {
    is Byte -> (this + float).b
    is Ubyte -> (v + float).b
    is Short -> (this + float).s
    is Ushort -> (v + float).s
    is Int -> (this + float).i
    is Uint -> (v + float).i
    is Long -> (this + float).L
    is Ulong -> (v + float).L
    is Float -> this + float
    is Double -> (this + float).d
    else -> error("Invalid operand types")
} as N

infix operator fun <N> N.plus(double: Double): N where N : Number, N : Comparable<N> = when (this) {
    is Byte -> (this + double).b
    is Ubyte -> (v + double).b
    is Short -> (this + double).s
    is Ushort -> (v + double).s
    is Int -> (this + double).i
    is Uint -> (v + double).i
    is Long -> (this + double).L
    is Ulong -> (v + double).L
    is Float -> (this + double).f
    is Double -> this + double
    else -> error("Invalid operand types")
} as N

infix operator fun <N> N.plus(int: Int): N where N : Number, N : Comparable<N> = when (this) {
    is Byte -> (this + int).b
    is Ubyte -> (v + int).b
    is Short -> (this + int).s
    is Ushort -> (v + int).s
    is Int -> (this + int).i
    is Uint -> (v + int).i
    is Long -> (this + int).L
    is Ulong -> (v + int).L
    is Float -> this + int
    is Double -> (this + int).d
    else -> error("Invalid operand types")
} as N

infix operator fun <N> N.compareTo(int: Int): Int where N : Number, N : Comparable<N> = when (this) {
    is Byte -> compareTo(int)
    is Ubyte -> compareTo(int)
    is Short -> compareTo(int)
    is Ushort -> compareTo(int)
    is Int -> compareTo(int)
    is Uint -> compareTo(int)
    is Long -> compareTo(int)
    is Ulong -> compareTo(int.L)
    is Float -> compareTo(int)
    is Double -> compareTo(int)
    else -> error("Invalid operand types")
}

fun <N> Number.`as`(n: N): N where N : Number, N : Comparable<N> = when (n) {
    is Byte -> b
    is Ubyte -> n.v
    is Short -> s
    is Ushort -> n.v
    is Int -> i
    is Uint -> n.v
    is Long -> L
    is Ulong -> n.v
    is Float -> f
    is Double -> d
    else -> error("invalid")
} as N

val <N> N.asSigned: N where N : Number, N : Comparable<N>
    get() = when (this) {
        is Byte, is Short -> i // TODO utypes
        else -> this
    } as N

fun <N> clamp(a: N, min: N, max: N): N where N : Number, N : Comparable<N> =
        if (a < min) min else if (a > max) max else a

fun <Type> min(a: Type, b: Type): Type
        where Type : Number, Type : Comparable<Type> = if (a < b) a else b

fun <Type> max(a: Type, b: Type): Type
        where Type : Number, Type : Comparable<Type> = if (a > b) a else b

fun <N> lerp(a: N, b: N, t: Float): N where N : Number, N : Comparable<N> = when (a) {
    is Byte -> ilerp(a.i, b.i, t).b
    is Short -> ilerp(a.i, b.i, t).s
    is Int -> ilerp(a, b as Int, t)
    is Long -> ilerp(a, b as Long, t)
    is Float -> ilerp(a, b as Float, t)
    is Double -> ilerp(a, b as Double, t)
    else -> error("Invalid type")
} as N

//infix operator fun <N> Float.times(n: N): N where N : Number, N : Comparable<N> = when (n) {
//    is Byte -> (b * n).b
//    is Short -> (s * n).s
//    is Int -> i * n
//    is Long -> L * n
//    is Float -> this * n
//    is Double -> d * n
//    else -> error("Invalid operand types")
//} as N
//
//infix operator fun <N> Float.div(n: N): N where N : Number, N : Comparable<N> = when (n) {
//    is Byte -> (b / n).b
//    is Short -> (s / n).s
//    is Int -> i / n
//    is Long -> L / n
//    is Float -> this / n
//    is Double -> d / n
//    else -> error("Invalid operand types")
//} as N


infix operator fun <Type> Type.compareTo(float: Float): Int where Type : Number, Type : Comparable<Type> = when (this) {
    is Byte -> compareTo(float)
    is Short -> compareTo(float)
    is Int -> compareTo(float)
    is Long -> compareTo(float)
    is Ubyte, is Ushort, is Uint, is Ulong -> f.compareTo(float) // TODO -> unsigned
    is Float -> compareTo(float)
    is Double -> compareTo(float)
    else -> error("invalid")
}

@JvmName("min_")
fun <Type, N> min(n: N, t: Type): Type
        where Type : Number, Type : Comparable<Type>,
              N : Number, N : Comparable<N> = when (t) {
    is Byte -> if (n.b < t) n.b else t
    is Ubyte -> if (n.ub < t) n.ub else t
    is Short -> if (n.s < t) n.s else t
    is Ushort -> if (n.us < t) n.us else t
    is Int -> if (n.i < t) n.i else t
    is Uint -> if (n.ui < t) n.ui else t
    is Long -> if (n.L < t) n.L else t
    is Ulong -> if (n.ul < t) n.ul else t
    is Float -> if (n.f < t) n.f else t
    is Double -> if (n.d < t) n.d else t
    else -> error("invalid")
} as Type

@JvmName("max_")
fun <Type, N> max(n: N, t: Type): Type
        where Type : Number, Type : Comparable<Type>,
              N : Number, N : Comparable<N> = when (t) {
    is Byte -> if (n.b > t) n.b else t
    is Ubyte -> if (n.ub > t) n.ub else t
    is Short -> if (n.s > t) n.s else t
    is Ushort -> if (n.us > t) n.us else t
    is Int -> if (n.i > t) n.i else t
    is Uint -> if (n.ui > t) n.ui else t
    is Long -> if (n.L > t) n.L else t
    is Ulong -> if (n.ul > t) n.ul else t
    is Float -> if (n.f > t) n.f else t
    is Double -> if (n.d > t) n.d else t
    else -> error("invalid")
} as Type

fun <Type, FloatType> Type.asFloatType(): FloatType
        where Type : Number, Type : Comparable<Type>,
              FloatType : Number, FloatType : Comparable<FloatType> = when (this) {
    is Byte, is Ubyte, is Short, is Ushort, is Int, is Uint, is Float -> f
    is Long, is Ulong, is Double -> f
    else -> error("invalid")
} as FloatType

infix operator fun <Type> Type.times(other: Float): Type
        where Type : Number, Type : Comparable<Type> = when (this) {
    is Byte -> this * other
    is Short -> this * other
    is Int -> this * other
    is Long -> this * other
    is Float -> this * other
    is Double -> this * other
    is Ubyte, is Ushort, is Uint, is Ulong -> f * other
    else -> error("Invalid operand types")
} as Type

infix operator fun <Type> Float.div(other: Type): Type
        where Type : Number, Type : Comparable<Type> = when(other) {
    is Byte -> this * other
    is Short -> this * other
    is Int -> this * other
    is Long -> this * other
    is Float -> this * other
    is Double -> this * other
    else -> error("Invalid operand types")
} as Type