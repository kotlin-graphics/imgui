package imgui.imgui

import glm_.*
import kotlin.math.min as kmin
import kotlin.math.max as kmax
import imgui.internal.lerp as ilerp

infix operator fun <N> N.plus(other: N): N where N : Number, N : Comparable<N> = when {
    this is Byte && other is Byte -> (this + other).b
    this is Short && other is Short -> (this + other).s
    this is Int && other is Int -> this + other
    this is Long && other is Long -> this + other
    this is Float && other is Float -> this + other
    this is Double && other is Double -> this + other
    else -> error("Invalid operand types")
} as N

infix operator fun <N> N.minus(other: N): N where N : Number, N : Comparable<N> = when {
    this is Byte && other is Byte -> (this - other).b
    this is Short && other is Short -> (this - other).s
    this is Int && other is Int -> this - other
    this is Long && other is Long -> this - other
    this is Float && other is Float -> this - other
    this is Double && other is Double -> this - other
    else -> error("Invalid operand types")
} as N

infix operator fun <N> N.times(other: N): N where N : Number, N : Comparable<N> = when {
    this is Byte && other is Byte -> (this * other).b
    this is Short && other is Short -> (this * other).s
    this is Int && other is Int -> this * other
    this is Long && other is Long -> this * other
    this is Float && other is Float -> this * other
    this is Double && other is Double -> this * other
    else -> error("Invalid operand types")
} as N

infix operator fun <N> N.div(other: N): N where N : Number, N : Comparable<N> = when {
    this is Byte && other is Byte -> (this / other).b
    this is Short && other is Short -> (this / other).s
    this is Int && other is Int -> this / other
    this is Long && other is Long -> this / other
    this is Float && other is Float -> this / other
    this is Double && other is Double -> this / other
    else -> error("Invalid operand types")
} as N


infix operator fun <N> N.plus(float: Float): N where N : Number, N : Comparable<N> = when (this) {
    is Byte -> (this + float).b
    is Short -> (this + float).s
    is Int -> (this + float).i
    is Long -> (this + float).L
    is Float -> this + float
    is Double -> (this + float).d
    else -> error("Invalid operand types")
} as N

infix operator fun <N> N.plus(double: Double): N where N : Number, N : Comparable<N> = when (this) {
    is Byte -> (this + double).b
    is Short -> (this + double).s
    is Int -> (this + double).i
    is Long -> (this + double).L
    is Float -> (this + double).f
    is Double -> this + double
    else -> error("Invalid operand types")
} as N

infix operator fun <N> N.plus(int: Int): N where N : Number, N : Comparable<N> = when (this) {
    is Byte -> (this + int).b
    is Short -> (this + int).s
    is Int -> (this + int).i
    is Long -> (this + int).L
    is Float -> this + int
    is Double -> (this + int).d
    else -> error("Invalid operand types")
} as N

infix operator fun <N> N.compareTo(int: Int): Int where N : Number, N : Comparable<N> = when (this) {
    is Byte -> (this as Byte).compareTo(int)
    is Short -> (this as Short).compareTo(int)
    is Int -> (this as Int).compareTo(int)
    is Long -> (this as Long).compareTo(int)
    is Float -> (this as Float).compareTo(int)
    is Double -> (this as Double).compareTo(int)
    else -> error("Invalid operand types")
}

fun <N> Number.`as`(n: N): N where N : Number, N : Comparable<N> = when (n) {
    is Byte -> b
    is Short -> s
    is Int -> i
    is Long -> L
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
    is Short -> glm.clamp(a, min as Short, max as Short)
    is Int -> glm.clamp(a, min as Int, max as Int)
    is Long -> glm.clamp(a, min as Long, max as Long)
    is Float -> glm.clamp(a, min as Float, max as Float)
    is Double -> glm.clamp(a, min as Double, max as Double)
    else -> error("Invalid type")
} as N

fun <N> min(a: N, b: N): N where N : Number, N : Comparable<N> = when (a) {
    is Byte -> kmin(a.i, b.i).b
    is Short -> kmin(a.i, b.i).s
    is Int -> kmin(a, b as Int)
    is Long -> kmin(a, b as Long)
    is Float -> kmin(a, b as Float)
    is Double -> kmin(a, b as Double)
    else -> error("Invalid type")
} as N

fun <N> max(a: N, b: N): N where N : Number, N : Comparable<N> = when (a) {
    is Byte -> kmax(a.i, b.i).b
    is Short -> kmax(a.i, b.i).s
    is Int -> kmax(a, b as Int)
    is Long -> kmax(a, b as Long)
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
