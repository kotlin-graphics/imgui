package imgui

import glm_.*
import unsigned.Ubyte
import unsigned.Uint
import unsigned.Ulong
import unsigned.Ushort
import java.math.BigInteger
import kotlin.math.min as kmin
import kotlin.math.max as kmax
import kotlin.plus
import kotlin.minus
import kotlin.times
import kotlin.div
import imgui.internal.lerp as ilerp

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

infix operator fun <N> N.minus(other: N): N where N : Number, N : Comparable<N> = when {
    this is Byte && other is Byte -> (this - other).b
    this is Ubyte && other is Ubyte -> (this - other).b
    this is Short && other is Short -> (this - other).s
    this is Ushort && other is Ushort -> (this - other).s
    this is Int && other is Int -> this - other
    this is Uint && other is Uint -> this - other
    this is Long && other is Long -> this - other
    this is Ulong && other is Ulong -> this - other
    this is Float && other is Float -> this - other
    this is Double && other is Double -> this - other
    this is BigInteger && other is BigInteger -> this - other
    else -> error("Invalid operand types")
} as N

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
    is Byte -> (this as Byte).compareTo(int)
    is Ubyte -> compareTo(int)
    is Short -> (this as Short).compareTo(int)
    is Ushort -> compareTo(int)
    is Int -> (this as Int).compareTo(int)
    is Uint -> compareTo(int)
    is Long -> (this as Long).compareTo(int)
    is Ulong -> compareTo(int.L)
    is Float -> (this as Float).compareTo(int)
    is Double -> (this as Double).compareTo(int)
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

fun <N> clamp(a: N, min: N, max: N): N where N : Number, N : Comparable<N> = when (a) {
    is Byte -> glm.clamp(a, min as Byte, max as Byte)
    is Ubyte -> if(a < min) min else if (a > max) max else a // TODO glm -> clamp unsigned
    is Short -> glm.clamp(a, min as Short, max as Short)
    is Ushort -> if(a < min) min else if (a > max) max else a
    is Int -> glm.clamp(a, min as Int, max as Int)
    is Uint -> if(a < min) min else if (a > max) max else a
    is Long -> glm.clamp(a, min as Long, max as Long)
    is Ulong -> if(a < min) min else if (a > max) max else a
    is Float -> glm.clamp(a, min as Float, max as Float)
    is Double -> glm.clamp(a, min as Double, max as Double)
    else -> error("Invalid type")
} as N

fun <N> min(a: N, b: N): N where N : Number, N : Comparable<N> = when (a) {
    is Byte -> kmin(a.i, b.i).b
    is Ubyte -> if(a < b) a else b // TODO glm -> clamp unsigned
    is Short -> kmin(a.i, b.i).s
    is Ushort -> if(a < b) a else b
    is Int -> kmin(a, b as Int)
    is Uint -> if(a < b) a else b
    is Long -> kmin(a, b as Long)
    is Ulong -> if(a < b) a else b
    is Float -> kmin(a, b as Float)
    is Double -> kmin(a, b as Double)
    else -> error("Invalid type")
} as N

fun <N> max(a: N, b: N): N where N : Number, N : Comparable<N> = when (a) {
    is Byte -> kmax(a.i, b.i).b
    is Ubyte -> if(a > b) a else b // TODO glm -> clamp unsigned
    is Short -> kmax(a.i, b.i).s
    is Ushort -> if(a > b) a else b
    is Int -> kmax(a, b as Int)
    is Uint -> if(a > b) a else b
    is Long -> kmax(a, b as Long)
    is Ulong -> if(a > b) a else b
    is Float -> kmax(a, b as Float)
    is Double -> kmax(a, b as Double)
    else -> error("Invalid type")
} as N

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
