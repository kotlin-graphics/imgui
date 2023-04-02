package imgui

import gli_.has
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

class Property<V>(val get: () -> V) : KProperty0<V> {
    override val annotations: List<Annotation>
        get() = TODO("Not yet implemented")
    override val getter: KProperty0.Getter<V>
        get() = TODO("Not yet implemented")
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

    override fun call(vararg args: Any?): V = TODO("Not yet implemented")

    override fun callBy(args: Map<KParameter, Any?>): V = TODO("Not yet implemented")

    override fun get(): V = get.invoke()

    override fun getDelegate(): Any = TODO("Not yet implemented")

    override fun invoke(): V = get()
}

infix fun BooleanArray.mutablePropertyAt(index: Int) = MutableProperty({ this[index] }) { this[index] = it }
infix fun <T> Array<T>.mutablePropertyAt(index: Int) = MutableProperty({ this[index] }) { this[index] = it }
infix fun <F: Flag<F>> FlagArray<F>.mutablePropertyAt(index: Int): MutableProperty<Flag<F>> = MutableProperty({ this[index] }) { this[index] = it }
inline val <V> V.asMutableProperty
    get() = MutableProperty({ this })

infix fun <V> V.mutableProperty(set: (V) -> Unit) = MutableProperty({ this }, set)
val <V> V.mutableReference: MutableReference<V>
    get() = MutableReference(this)
//fun <V> mutableProperty(get: () -> V, set: (V) -> Unit = {}) = MutableProperty(get, set)

//fun <V> mutableProperty(get: () -> V) = MutableProperty(get)
abstract class MutablePropertyBase<V> : KMutableProperty0<V> {
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
                get() = TODO("Not yet implemented")
            override val returnType: KType
                get() = TODO("Not yet implemented")
            override val typeParameters: List<KTypeParameter>
                get() = TODO("Not yet implemented")
            override val visibility: KVisibility
                get() = TODO("Not yet implemented")

            override fun call(vararg args: Any?): V = TODO("Not yet implemented")

            override fun callBy(args: Map<KParameter, Any?>): V {
                TODO("Not yet implemented")
            }

            override fun invoke(): V = get()

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
    override val setter: KMutableProperty0.Setter<V>
        get() = TODO("Not yet implemented")
    override val typeParameters: List<KTypeParameter>
        get() = TODO("Not yet implemented")
    override val visibility: KVisibility
        get() = TODO("Not yet implemented")

    override fun call(vararg args: Any?): V = TODO("Not yet implemented")
    override fun callBy(args: Map<KParameter, Any?>): V = TODO("Not yet implemented")
    override fun getDelegate(): Any = TODO("Not yet implemented")
    override fun invoke(): V = get()
}

class MutableProperty<V>(val get: () -> V, val set: (V) -> Unit = {}) : MutablePropertyBase<V>() {
    override fun get(): V = get.invoke()
    override fun set(value: V) = set.invoke(value)
}

class MutableReference<V>(var field: V) : MutablePropertyBase<V>() {
    override fun get(): V = field
    override fun set(value: V) {
        field = value
    }
}