package imgui.stb

import glm_.BYTES
import glm_.i
import glm_.s
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import org.lwjgl.stb.*
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.system.Pointer.POINTER_SIZE
import java.nio.ByteBuffer


var STBRPRect.wasPacked
    get() = was_packed()
    set(value) {
        was_packed(value)
    }

var STBRPRect.x
    set(value) {
        x(value.s)
    }
    get() = x().i

var STBRPRect.y
    set(value) {
        y(value.s)
    }
    get() = y().i

var STBRPRect.w
    set(value) {
        w(value.s)
    }
    get() = w().i

var STBRPRect.h
    set(value) {
        h(value.s)
    }
    get() = h().i


var STBRPRect.Buffer.x
    set(value) {
        x(value.s)
    }
    get() = x().i

var STBRPRect.Buffer.y
    set(value) {
        y(value.s)
    }
    get() = y().i

var STBRPRect.Buffer.w
    set(value) {
        w(value.s)
    }
    get() = w().i

var STBRPRect.Buffer.h
    set(value) {
        h(value.s)
    }
    get() = h().i


var STBTTPackRange.fontSize
    set(value) {
        font_size(value)
    }
    get() = font_size()

var STBTTPackRange.firstUnicodeCodepointInRange
    set(value) {
        first_unicode_codepoint_in_range(value)
    }
    get() = first_unicode_codepoint_in_range()

var STBTTPackRange.numChars
    set(value) {
        num_chars(value)
    }
    get() = num_chars()

var STBTTPackRange.chardataForRange: STBTTPackedchar.Buffer
    set(value) {
        chardata_for_range(value)
    }
    get() = chardata_for_range()


var STBTTPackedchar.x0
    get() = x0().i
    set(value) {
        x0(value.s)
    }
var STBTTPackedchar.y0
    get() = y0().i
    set(value) {
        y0(value.s)
    }
var STBTTPackedchar.x1
    get() = x1().i
    set(value) {
        x1(value.s)
    }
var STBTTPackedchar.y1
    get() = y1().i
    set(value) {
        y1(value.s)
    }
var STBTTPackedchar.xOff
    get() = xoff()
    set(value) {
        xoff(value)
    }
var STBTTPackedchar.yOff
    get() = yoff()
    set(value) {
        yoff(value)
    }
var STBTTPackedchar.xAdvance
    get() = xadvance()
    set(value) {
        xadvance(value)
    }
var STBTTPackedchar.xOff2
    get() = xoff2()
    set(value) {
        xoff2(value)
    }
var STBTTPackedchar.yOff2
    get() = yoff2()
    set(value) {
        yoff2(value)
    }


var STBTTPackContext.packInfo: STBRPContext
    get() {
        if (Private.rpCtx == null)
            Private.rpCtx = STBRPContext.create(memGetAddress(address() + org.lwjgl.system.Pointer.POINTER_SIZE))
        return Private.rpCtx!!
    }
    set(value) {
        Private.rpCtx = value
    }
var STBTTPackContext.pixels: ByteBuffer
    get() = Private.pixels!!
    set(value) {
        memPutAddress(address() + 2 * POINTER_SIZE + 6 * Int.BYTES, memAddress(value))
        Private.pixels = value
    }
var STBTTPackContext.height
    get() = memGetInt(address() + 2 * POINTER_SIZE + Int.BYTES)
    set(value) = memPutInt(address() + 2 * POINTER_SIZE + Int.BYTES, value)
var STBTTPackContext.strideInBytes
    get() = memGetInt(address() + 2 * POINTER_SIZE + Int.BYTES * 2)
    set(value) = memPutInt(address() + 2 * POINTER_SIZE + Int.BYTES * 2, value)

private object Private {

    var rpCtx: STBRPContext? = null
    var pixels: ByteBuffer? = null
}

fun stbClear() {
    Private.rpCtx = null
    Private.pixels = null
}

fun stbtt_PackSetOversampling(spc: STBTTPackContext, oversample: Vec2i) = STBTruetype.stbtt_PackSetOversampling(spc, oversample.x, oversample.y)

fun stbtt_PackSetOversampling(spc: STBTTPackContext, oversample: Int) = STBTruetype.stbtt_PackSetOversampling(spc, oversample, oversample)

fun stbtt_GetFontVMetrics(info: STBTTFontinfo): Triple<Int, Int, Int> {
    val ascent = IntArray(1)
    val descent = IntArray(1)
    val lineGap = IntArray(1)
    STBTruetype.stbtt_GetFontVMetrics(info, ascent, descent, lineGap)
    return Triple(ascent[0], descent[0], lineGap[0])
}


fun stbtt_GetPackedQuad(chardata: STBTTPackedchar.Buffer, p: Vec2i, charIndex: Int, pos: Vec2, q: STBTTAlignedQuad, alignToInteger: Boolean) {

    val xPos = FloatArray(1)
    val yPos = FloatArray(1)

    STBTruetype.stbtt_GetPackedQuad(chardata, p.x, p.y, charIndex, xPos, yPos, q, alignToInteger)

    pos.put(xPos[0], yPos[0])
}

val STBTTAlignedQuad.x0 get() = x0()
val STBTTAlignedQuad.y0 get() = y0()
val STBTTAlignedQuad.x1 get() = x1()
val STBTTAlignedQuad.y1 get() = y1()
val STBTTAlignedQuad.s0 get() = s0()
val STBTTAlignedQuad.s1 get() = s1()
val STBTTAlignedQuad.t0 get() = t0()
val STBTTAlignedQuad.t1 get() = t1()

























