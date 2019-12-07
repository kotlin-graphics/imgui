package imgui

import glm_.BYTES
import glm_.L
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.ImGui.beginTooltip
import imgui.ImGui.endTooltip
import imgui.ImGui.fontSize
import imgui.ImGui.isItemHovered
import imgui.ImGui.parseFormatFindEnd
import imgui.ImGui.parseFormatFindStart
import imgui.ImGui.popTextWrapPos
import imgui.ImGui.pushTextWrapPos
import imgui.ImGui.textDisabled
import imgui.ImGui.textEx
import imgui.api.g
import imgui.internal.*
import kool.*
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.Platform
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.reflect.KMutableProperty0

private val DRAW_VERT_FLOAT_OFFSET = (DrawVert.size / Float.BYTES)
private val DRAW_VERT_PER_UV_OFFSET = (DrawVert.ofsUv / Float.BYTES)

fun DrawVert_Buffer(size: Int = 0) = DrawVert_Buffer(ByteBuffer(size))
inline class DrawVert_Buffer(val data: ByteBuffer) {

    operator fun get(index: Int) = DrawVert().apply {
        pos.put(data.asFloatBuffer(), index * DRAW_VERT_FLOAT_OFFSET)
        uv.put(data.asFloatBuffer(), index * DRAW_VERT_FLOAT_OFFSET + DRAW_VERT_PER_UV_OFFSET)
        col = data.getInt(index * DrawVert.size + DrawVert.ofsCol)
    }

    operator fun plusAssign(v: Vec2) {
        data.putFloat(v.x)
        data.putFloat(v.y)
    }

    operator fun plusAssign(i: Int) {
        data.putInt(i)
    }

    operator fun plusAssign(f: Float) {
        data.putFloat(f)
    }

    inline val cap: Int
        get() = data.cap / DrawVert.size

    inline var lim: Int
        get() = data.lim / DrawVert.size
        set(value) {
            data.lim = value * DrawVert.size
        }

    inline var pos: Int
        get() = data.pos / DrawVert.size
        set(value) {
            data.pos = value * DrawVert.size
        }

    inline val rem: Int
        get() = data.rem / DrawVert.size

    inline val size: Int
        get() = rem

    fun hasRemaining(): Boolean = rem > 0

    infix fun resize(newSize: Int): DrawVert_Buffer = when {
        newSize > cap -> reserve(growCapacity(newSize))
        else -> this
    }.apply { lim = newSize }

    infix fun growCapacity(sz: Int): Int {
        val newCapacity = if (cap > 0) cap + cap / 2 else 8
        return if (newCapacity > sz) newCapacity else sz
    }

    infix fun reserve(newCapacity: Int): DrawVert_Buffer {
        if (newCapacity <= cap)
            return this
        val newData = ByteBuffer(newCapacity * DrawVert.size)
        if (lim > 0)
            MemoryUtil.memCopy(data.adr, newData.adr, data.lim.L)
        data.free()
        return DrawVert_Buffer(newData)
    }
}

infix fun IntBuffer.resize(newSize: Int): IntBuffer = when {
    newSize > cap -> reserve(growCapacity(newSize))
    else -> this
}.apply { lim = newSize }

infix fun IntBuffer.growCapacity(sz: Int): Int {
    val newCapacity = if (cap > 0) cap + cap / 2 else 8
    return if (newCapacity > sz) newCapacity else sz
}

infix fun IntBuffer.reserve(newCapacity: Int): IntBuffer {
    if (newCapacity <= cap)
        return this
    val newData = IntBuffer(newCapacity)
    if (lim > 0)
        MemoryUtil.memCopy(adr, newData.adr, remSize.L)
    free()
    return newData
}

fun IntBuffer(from: IntBuffer): IntBuffer {
    val res = IntBuffer(from.cap)
    res.lim = from.lim
    for(i in 0 until from.lim)
        res[i] = from[i]
    return res
}

/** Helper to display a little (?) mark which shows a tooltip when hovered.
 *  In your own code you may want to display an actual icon if you are using a merged icon fonts (see misc/fonts/README.txt)    */
fun helpMarker(desc: String) {
    textDisabled("(?)")
    if (isItemHovered()) {
        beginTooltip()
        pushTextWrapPos(fontSize * 35f)
        textEx(desc)
        popTextWrapPos()
        endTooltip()
    }
}

// static arrays to avoid GC pressure
val _fa = FloatArray(4)
val _fa2 = FloatArray(4)
val _ia = IntArray(4)
// static referenceable
var _b = false
var _i = 0
var _L = 0L
var _d = 0.0
var _f = 0f
var _f1 = 0f
var _f2 = 0f
var _c = NUL


interface PlotArray {
    operator fun get(idx: Int): Float
    fun count(): Int
}

class PlotArrayData(val values: FloatArray, val stride: Int) : PlotArray {
    override operator fun get(idx: Int) = values[idx * stride]
    override fun count() = values.size
}

class PlotArrayFunc(val func: (Int) -> Float, val count: Int) : PlotArray {
    override operator fun get(idx: Int) = func(idx)
    override fun count() = count
}

inline fun <R> withInt(ints: IntArray, ptr: Int, block: (KMutableProperty0<Int>) -> R): R {
    Ref.iPtr++
    val i = Ref::int
    i.set(ints[ptr])
    val res = block(i)
    ints[ptr] = i()
    Ref.iPtr--
    return res
}

inline fun <R> withFloat(floats: FloatArray, ptr: Int, block: (KMutableProperty0<Float>) -> R): R {
    Ref.fPtr++
    val f = Ref::float
    f.set(floats[ptr])
    val res = block(f)
    floats[ptr] = f()
    Ref.fPtr--
    return res
}

/** FIXME-LEGACY: Prior to 1.61 our DragInt() function internally used floats and because of this the compile-time default value
 *  for format was "%.0f".
 *  Even though we changed the compile-time default, we expect users to have carried %f around, which would break
 *  the display of DragInt() calls.
 *  To honor backward compatibility we are rewriting the format string, unless IMGUI_DISABLE_OBSOLETE_FUNCTIONS is enabled.
 *  What could possibly go wrong?! */
fun patchFormatStringFloatToInt(fmt: String): String {
    if (fmt == "%.0f") // Fast legacy path for "%.0f" which is expected to be the most common case.
        return "%d"
    val fmtStart = parseFormatFindStart(fmt)    // Find % (if any, and ignore %%)
    // Find end of format specifier, which itself is an exercise of confidence/recklessness (because snprintf is dependent on libc or user).
    val fmtEnd = parseFormatFindEnd(fmt, fmtStart)
    if (fmtEnd > fmtStart && fmt[fmtEnd - 1] == 'f') {
        if (fmtStart == 0 && fmtEnd == fmt.length)
            return "%d"
        return fmt.substring(0, fmtStart) + "%d" + fmt.substring(fmtEnd, fmt.length)
    }
    return fmt
}

inline fun <R> withBool(block: (KMutableProperty0<Boolean>) -> R): R {
    Ref.bPtr++
    return block(Ref::bool).also { Ref.bPtr-- }
}

val CharArray.strlen: Int
    get() {
        var i = 0
        while (i < size && this[i] != NUL) i++
        return i
    }

internal fun Char.remapCodepointIfProblematic(): Int {
    val i = toInt()
    return when (Platform.get()) {
        /*  https://en.wikipedia.org/wiki/Windows-1252#Character_set
         *  manually remap the difference from  ISO-8859-1 */
        Platform.WINDOWS -> when (i) {
            // 8_128
            0x20AC -> 128 // €
            0x201A -> 130 // ‚
            0x0192 -> 131 // ƒ
            0x201E -> 132 // „
            0x2026 -> 133 // …
            0x2020 -> 134 // †
            0x2021 -> 135 // ‡
            0x02C6 -> 136 // ˆ
            0x2030 -> 137 // ‰
            0x0160 -> 138 // Š
            0x2039 -> 139 // ‹
            0x0152 -> 140 // Œ
            0x017D -> 142 // Ž
            // 9_144
            0x2018 -> 145 // ‘
            0x2019 -> 146 // ’
            0x201C -> 147 // “
            0x201D -> 148 // ”
            0x2022 -> 149 // •
            0x2013 -> 150 // –
            0x2014 -> 151 // —
            0x02DC -> 152 // ˜
            0x2122 -> 153 // ™
            0x0161 -> 154 // š
            0x203A -> 155 // ›
            0x0153 -> 156 // œ
            0x017E -> 158 // ž
            0x0178 -> 159 // Ÿ
            else -> i
        }
        else -> i // TODO
    }
}

val Int.vec4: Vec4
    get() {
        val s = 1f / 255f
        return Vec4(
                ((this ushr COL32_R_SHIFT) and 0xFF) * s,
                ((this ushr COL32_G_SHIFT) and 0xFF) * s,
                ((this ushr COL32_B_SHIFT) and 0xFF) * s,
                ((this ushr COL32_A_SHIFT) and 0xFF) * s)
    }

val Vec4.u32: Int
    get () {
        var out = F32_TO_INT8_SAT(x) shl COL32_R_SHIFT
        out = out or (F32_TO_INT8_SAT(y) shl COL32_G_SHIFT)
        out = out or (F32_TO_INT8_SAT(z) shl COL32_B_SHIFT)
        return out or (F32_TO_INT8_SAT(w) shl COL32_A_SHIFT)
    }
