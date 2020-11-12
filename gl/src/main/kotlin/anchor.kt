import imgui.strlen
import kool.pos
import java.nio.ByteBuffer

var IMGUI_HAS_DOCK = false
var IMGUI_HAS_TABLE = false
var IMGUI_DEBUG_TEST_ENGINE = true

fun ByteBuffer.sliceAt(offset: Int): ByteBuffer {
    val backupPos = pos
    pos = offset
    val res = slice()
    pos = backupPos
    return res
}