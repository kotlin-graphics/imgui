package imgui

import glm_.has
import glm_.L
import glm_.f
import glm_.vec2.Vec2t
import glm_.vec3.Vec3t
import glm_.vec4.Vec4t
import uno.kotlin.NUL
import kotlin.reflect.*

var IMGUI_HAS_DOCK = false
var IMGUI_DEBUG_TEST_ENGINE = true

infix fun Byte.has(i: Int): Boolean = toInt().has(i)

fun <T> ArrayList<T>.pop(): T = removeLast()

class StringPointer(val string: String) {
    var pointer = 0
    operator fun get(index: Int) = string.getOrNul(pointer + index)
    operator fun invoke(): Int = pointer
    operator fun inc(): StringPointer {
        pointer++
        return this
    }
}

fun String.getOrNul(index: Int): Char = getOrElse(index) { NUL }

//operator fun Char.compareTo(int: Int) = toInt().compareTo(int)

operator fun Boolean.div(other: Boolean) = or(other)
operator fun Boolean.rem(other: Boolean) = and(other)

infix fun ByteArray.mutablePropertyAt(index: Int) = mutableProperty({ this[index] }) { this[index] = it }
infix fun CharArray.mutablePropertyAt(index: Int) = mutableProperty({ this[index] }) { this[index] = it }
infix fun ShortArray.mutablePropertyAt(index: Int) = mutableProperty({ this[index] }) { this[index] = it }
infix fun IntArray.mutablePropertyAt(index: Int) = mutableProperty({ this[index] }) { this[index] = it }
infix fun LongArray.mutablePropertyAt(index: Int) = mutableProperty({ this[index] }) { this[index] = it }
infix fun FloatArray.mutablePropertyAt(index: Int) = mutableProperty({ this[index] }) { this[index] = it }
infix fun DoubleArray.mutablePropertyAt(index: Int) = mutableProperty({ this[index] }) { this[index] = it }
infix fun BooleanArray.mutablePropertyAt(index: Int) = mutableProperty({ this[index] }) { this[index] = it }
infix fun <T> Array<T>.mutablePropertyAt(index: Int) = mutableProperty({ this[index] }) { this[index] = it }
infix fun <T> MutableList<T>.mutablePropertyAt(index: Int) = mutableProperty({ this[index] }) { this[index] = it }
fun <K, V> MutableMap<K, V>.mutablePropertyAt(index: K, default: V) = mutableProperty({ getOrDefault(index, default) }) { this[index] = it }
infix fun <T : Number> Vec2t<T>.mutablePropertyAt(index: Int): MutableProperty<T> {
    require(index in 0 until Vec2t.length) { "Vector $this does not have index $index" }
    return mutableProperty({ this[index] }) { this[index] = it }
}

infix fun <T : Number> Vec3t<T>.mutablePropertyAt(index: Int): MutableProperty<T> {
    require(index in 0 until Vec3t.length) { "Vector $this does not have index $index" }
    return mutableProperty({ this[index] }) { this[index] = it }
}

infix fun <T : Number> Vec4t<T>.mutablePropertyAt(index: Int): MutableProperty<T> {
    require(index in 0 until Vec4t.length) { "Vector $this does not have index $index" }
    return mutableProperty({ this[index] }) { this[index] = it }
}

@JvmName("mutablePropertyAtFlagArray")
infix fun <F : Flag<F>> FlagArray<F>.mutablePropertyAt(index: Int) = mutableProperty({ this[index] }) { this[index] = it }

abstract class MutableProperty<V> : KMutableProperty0<V> {
    override val annotations: List<Annotation>
        get() = TODO("Not yet implemented")
    override val getter: KProperty0.Getter<V>
        get() = object : KProperty0.Getter<V> {
            override val annotations: List<Annotation>
                get() = TODO("Not yet implemented")
            override val isAbstract: Boolean
                get() = TODO("Not yet implemented")
            override val isExternal: Boolean
                get() = TODO("Not yet implemented")
            override val isFinal: Boolean
                get() = TODO("Not yet implemented")
            override val isInfix: Boolean
                get() = TODO("Not yet implemented")
            override val isInline: Boolean
                get() = TODO("Not yet implemented")
            override val isOpen: Boolean
                get() = TODO("Not yet implemented")
            override val isOperator: Boolean
                get() = TODO("Not yet implemented")
            override val isSuspend: Boolean
                get() = TODO("Not yet implemented")
            override val name: String
                get() = TODO("Not yet implemented")
            override val parameters: List<KParameter>
                get() = TODO("Not yet implemented")
            override val property: KProperty<V>
                get() = this@MutableProperty
            override val returnType: KType
                get() = TODO("Not yet implemented")
            override val typeParameters: List<KTypeParameter>
                get() = TODO("Not yet implemented")
            override val visibility: KVisibility
                get() = TODO("Not yet implemented")

            override fun call(vararg args: Any?): V = if (args.isEmpty()) get()
            else throw IllegalArgumentException("No arguments expected")

            override fun callBy(args: Map<KParameter, Any?>): V = if (args.isEmpty()) get()
            else throw IllegalArgumentException("No arguments expected")

            override fun invoke(): V = get()

        }
    override val setter: KMutableProperty0.Setter<V>
        get() = object : KMutableProperty0.Setter<V> {
            override val annotations: List<Annotation>
                get() = TODO("Not yet implemented")
            override val isAbstract: Boolean
                get() = TODO("Not yet implemented")
            override val isExternal: Boolean
                get() = TODO("Not yet implemented")
            override val isFinal: Boolean
                get() = TODO("Not yet implemented")
            override val isInfix: Boolean
                get() = TODO("Not yet implemented")
            override val isInline: Boolean
                get() = TODO("Not yet implemented")
            override val isOpen: Boolean
                get() = TODO("Not yet implemented")
            override val isOperator: Boolean
                get() = TODO("Not yet implemented")
            override val isSuspend: Boolean
                get() = TODO("Not yet implemented")
            override val name: String
                get() = TODO("Not yet implemented")
            override val parameters: List<KParameter>
                get() = TODO("Not yet implemented")
            override val property: KMutableProperty<V>
                get() = this@MutableProperty
            override val returnType: KType
                get() = TODO("Not yet implemented")
            override val typeParameters: List<KTypeParameter>
                get() = TODO("Not yet implemented")
            override val visibility: KVisibility
                get() = TODO("Not yet implemented")

            override fun call(vararg args: Any?): Unit = if (args.size == 1) set(args[0] as V)
            else throw IllegalArgumentException("One argument expected")

            override fun callBy(args: Map<KParameter, Any?>): Unit = if (args.size == 1) set(args.values.first() as V)
            else throw IllegalArgumentException("One argument expected")

            override fun invoke(p1: V): Unit = set(p1)

        }
    override val isAbstract: Boolean
        get() = TODO("Not yet implemented")
    override val isConst: Boolean
        get() = TODO("Not yet implemented")
    override val isFinal: Boolean
        get() = TODO("Not yet implemented")
    override val isLateinit: Boolean
        get() = TODO("Not yet implemented")
    override val isOpen: Boolean
        get() = TODO("Not yet implemented")
    override val isSuspend: Boolean
        get() = TODO("Not yet implemented")
    override val name: String
        get() = TODO("Not yet implemented")
    override val parameters: List<KParameter>
        get() = TODO("Not yet implemented")
    override val returnType: KType
        get() = TODO("Not yet implemented")
    override val typeParameters: List<KTypeParameter>
        get() = TODO("Not yet implemented")
    override val visibility: KVisibility
        get() = TODO("Not yet implemented")

    override fun call(vararg args: Any?): V = if (args.isEmpty()) get()
    else throw IllegalArgumentException("No arguments expected")

    override fun callBy(args: Map<KParameter, Any?>): V = if (args.isEmpty()) get()
    else throw IllegalArgumentException("No arguments expected")

    override fun getDelegate(): Any = this
    override fun invoke(): V = get()
}

inline fun <V> mutableProperty(crossinline get: () -> V, crossinline set: (V) -> Unit = {}) = object : MutableProperty<V>() {
    override fun get(): V = get.invoke()
    override fun set(value: V) = set.invoke(value)
}

class MutableReference<V>(var field: V) : MutableProperty<V>() {
    override fun get(): V = field
    override fun set(value: V) {
        field = value
    }
}

val <V> V.mutableReference: MutableReference<V>
    get() = MutableReference(this)

val KMutableProperty0<Float>.L: MutableProperty<Long>
    get() = mutableProperty({ get().L }) { set(it.f) }

val ULong.L: Long
    get() = toLong()