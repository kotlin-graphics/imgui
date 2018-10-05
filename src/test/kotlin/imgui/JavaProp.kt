package imgui

import kotlin.reflect.*

class JavaProp<T>(val g: () -> T, val s: (T) -> T, override val isSuspend: Boolean) : KMutableProperty0<T> {

    override val isConst: Boolean = false
    override val isOpen: Boolean = false
    override val annotations: List<Annotation> = listOf()
    override val isLateinit: Boolean = false
    override val isAbstract: Boolean = false
    override val isFinal: Boolean = false
    override val name: String = ""
    override val parameters: List<KParameter> = listOf()
    override val returnType: KType = TODO()
    override val typeParameters: List<KTypeParameter> = listOf()
    override val getter: KProperty0.Getter<T> = TODO()
    override val setter: KMutableProperty0.Setter<T> = TODO()
    override val visibility: KVisibility? = null

    override fun invoke(): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun set(value: T) {
        s(value)
    }

    override fun get(): T {
        return g()
    }

    override fun call(vararg args: Any?): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun callBy(args: Map<KParameter, Any?>): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDelegate(): Any? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}