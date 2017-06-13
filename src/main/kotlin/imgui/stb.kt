package imgui

import gli.wasInit
import glm.BYTES
import glm.i
import glm.s
import glm.vec2.Vec2i
import org.lwjgl.stb.*
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.system.Pointer
import org.lwjgl.system.Pointer.POINTER_SIZE
import java.nio.ByteBuffer


val STBRPRect.wasPacked get() = was_packed() != 0

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


val STBRPRect.Buffer.size get() = capacity() / STBRPRect.SIZEOF

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


var STBTTPackContext.packInfo: STBRPContext
    get() {
        if (!wasInit { Private.rpCtx })
            Private.rpCtx = STBRPContext.create(memGetAddress(address() + Pointer.POINTER_SIZE))
        return Private.rpCtx
    }
    set(value) {
        Private.rpCtx = value
    }
var STBTTPackContext.pixels: ByteBuffer
    get() = Private.pixels
    set(value) {
        memPutAddress(address() + 2 * POINTER_SIZE + 6 * Int.BYTES, memAddress(value))
        Private.pixels = value
    }
var STBTTPackContext.height
    get() = memGetInt(address() + 2 * POINTER_SIZE + Int.BYTES)
    set(value) = memPutInt(address() + 2 * POINTER_SIZE + Int.BYTES, value)

private object Private {

    lateinit var rpCtx: STBRPContext
    lateinit var pixels: ByteBuffer
}


fun STBTruetype.stbtt_PackSetOversampling(spc: STBTTPackContext, oversample: Vec2i)
        = STBTruetype.stbtt_PackSetOversampling(spc, oversample.x, oversample.y)

fun STBTruetype.a(spc: STBTTPackContext, oversample: Int)
        = STBTruetype.stbtt_PackSetOversampling(spc, oversample, oversample)