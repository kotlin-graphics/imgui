package imgui

import glm_.BYTES
import glm_.L
import glm_.vec2.Vec2
import kool.*
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.IntBuffer

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

    fun reserve(newCapacity: Int): DrawVert_Buffer {
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

fun IntBuffer.reserve(newCapacity: Int): IntBuffer {
    if (newCapacity <= cap)
        return this
    val newData = IntBuffer(newCapacity)
    if (lim > 0)
        MemoryUtil.memCopy(adr, newData.adr, lim.L)
    free()
    return newData
}