import imgui.strlen
import kool.pos
import java.nio.ByteBuffer

var IMGUI_HAS_DOCK = false
var IMGUI_HAS_TABLE = false
var IMGUI_TEST_ENGINE_DEBUG = true

fun ByteBuffer.sliceAt(offset: Int): ByteBuffer {
    val backupPos = pos
    pos = offset
    val res = slice()
    pos = backupPos
    return res
}


@SinceKotlin("1.4")
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@kotlin.jvm.JvmName("sumOfFloat")
//@kotlin.internal.InlineOnly
public inline fun <T> Iterable<T>.sumOf(selector: (T) -> Float): Float {
    var sum: Float = 0.toFloat()
    for (element in this) {
        sum += selector(element)
    }
    return sum
}