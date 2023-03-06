package imgui.stb_

import java.util.*

class Node {
    var x = 0
    var y = 0
    var next: Ptr<Node>? = null
//    override fun toString() = "x=$x y=$y next=${Arrays.toString(next)}"
}

class Ptr<N>(var value: N?) {
    operator fun invoke(): N = value!!
}

fun <N> ptrOf(value: N) = Ptr(value)