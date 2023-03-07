package imgui.stb_

class Node(val i: Int) {
    var x = 0
    var y = 0
    var next: Ptr<Node>? = null
    override fun toString() = "i = $i, x=$x y=$y next={$next}"
}

class Ptr<N>(var value: N?) {
    operator fun invoke(): N = value!!
    fun copy() = Ptr(value)
    override fun toString(): String = value?.toString() ?: "null"
}

fun <N> ptrOf(value: N) = Ptr(value)