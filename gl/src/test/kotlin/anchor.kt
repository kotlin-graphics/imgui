import imgui.strlen
import kool.pos
import java.nio.ByteBuffer

val IMGUI_HAS_DOCK = false
val IMGUI_HAS_TABLE = false
val IMGUI_DEBUG_TEST_ENGINE = true

fun ByteBuffer.sliceAt(offset: Int): ByteBuffer {
    val backupPos = pos
    pos = offset
    val res = slice()
    pos = backupPos
    return res
}