package imgui.stb

import glm_.*
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import kool.adr
import kool.stak
import org.lwjgl.stb.*
import org.lwjgl.system.CustomBuffer
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.system.Pointer.POINTER_SIZE
import java.nio.ByteBuffer
import java.nio.IntBuffer


var STBRPRect.wasPacked: Boolean
    get() = STBRPRect.nwas_packed(adr).bool
    set(value) = STBRPRect.nwas_packed(adr, value.i)

var STBRPRect.x: Int
    get() = STBRPRect.nx(adr).i
    set(value) = STBRPRect.nx(adr, value.s)

var STBRPRect.y: Int
    get() = STBRPRect.ny(adr).i
    set(value) = STBRPRect.ny(adr, value.s)

var STBRPRect.w: Int
    get() = STBRPRect.nw(adr).i
    set(value) = STBRPRect.nw(adr, value.s)

var STBRPRect.h: Int
    get() = STBRPRect.nh(adr).i
    set(value) = STBRPRect.nh(adr, value.s)


var STBRPRect.Buffer.x: Int
    get() = STBRPRect.nx(adr).i
    set(value) = STBRPRect.nx(adr, value.s)

var STBRPRect.Buffer.y: Int
    get() = STBRPRect.ny(adr).i
    set(value) = STBRPRect.ny(adr, value.s)

var STBRPRect.Buffer.w: Int
    get() = STBRPRect.nw(adr).i
    set(value) = STBRPRect.nw(adr, value.s)
var STBRPRect.Buffer.h: Int
    get() = STBRPRect.nh(adr).i
    set(value) = STBRPRect.nh(adr, value.s)


var STBTTPackRange.fontSize: Float
    get() = STBTTPackRange.nfont_size(adr)
    set(value) = STBTTPackRange.nfont_size(adr, value)

var STBTTPackRange.firstUnicodeCodepointInRange: Int
    get() = STBTTPackRange.nfirst_unicode_codepoint_in_range(adr)
    set(value) = STBTTPackRange.nfirst_unicode_codepoint_in_range(adr, value)

var STBTTPackRange.arrayOfUnicodeCodepoints: IntBuffer?
    get() = STBTTPackRange.narray_of_unicode_codepoints(adr)
    set(value) = STBTTPackRange.narray_of_unicode_codepoints(adr, value)


var STBTTPackRange.numChars: Int
    get() = STBTTPackRange.nnum_chars(adr)
    set(value) = STBTTPackRange.nnum_chars(adr, value)

var STBTTPackRange.chardataForRange: STBTTPackedchar.Buffer
    get() = STBTTPackRange.nchardata_for_range(adr)
    set(value) = STBTTPackRange.nchardata_for_range(adr, value)


var STBTTPackRange.oversample: Vec2i
    get() = Vec2i(STBTTPackRange.nh_oversample(adr), STBTTPackRange.nv_oversample(adr))
    set(value) {
        STBTTPackRange.nh_oversample(adr, value.x.b)
        STBTTPackRange.nv_oversample(adr, value.y.b)
    }

var STBTTPackedchar.x0: Int
    get() = STBTTPackedchar.nx0(adr).i
    set(value) = STBTTPackedchar.nx0(adr, value.s)

var STBTTPackedchar.y0: Int
    get() = STBTTPackedchar.ny0(adr).i
    set(value) = STBTTPackedchar.ny0(adr, value.s)

var STBTTPackedchar.x1: Int
    get() = STBTTPackedchar.nx1(adr).i
    set(value) = STBTTPackedchar.nx1(adr, value.s)

var STBTTPackedchar.y1: Int
    get() = STBTTPackedchar.ny1(adr).i
    set(value) = STBTTPackedchar.ny1(adr, value.s)

var STBTTPackedchar.xOff: Float
    get() = STBTTPackedchar.nxoff(adr)
    set(value) = STBTTPackedchar.nxoff(adr, value)

var STBTTPackedchar.yOff: Float
    get() = STBTTPackedchar.nyoff(adr)
    set(value) = STBTTPackedchar.nyoff(adr, value)

var STBTTPackedchar.xAdvance: Float
    get() = STBTTPackedchar.nxadvance(adr)
    set(value) = STBTTPackedchar.nxadvance(adr, value)

var STBTTPackedchar.xOff2: Float
    get() = STBTTPackedchar.nxoff2(adr)
    set(value) = STBTTPackedchar.nxoff2(adr, value)

var STBTTPackedchar.yOff2: Float
    get() = STBTTPackedchar.nyoff2(adr)
    set(value) = STBTTPackedchar.nyoff2(adr, value)


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
var STBTTPackContext.height: Int
    get() = STBTTPackContext.nheight(adr)
    set(value) = memPutInt(adr + STBTTPackContext.HEIGHT, value)

var STBTTPackContext.strideInBytes: Int
    get() = STBTTPackContext.nstride_in_bytes(adr)
    set(value) = memPutInt(adr + STBTTPackContext.STRIDE_IN_BYTES, value)

val STBTTAlignedQuad.x0: Float
    get() = STBTTAlignedQuad.nx0(adr)

val STBTTAlignedQuad.y0: Float
    get() = STBTTAlignedQuad.ny0(adr)
val STBTTAlignedQuad.x1: Float
    get() = STBTTAlignedQuad.nx1(adr)
val STBTTAlignedQuad.y1: Float
    get() = STBTTAlignedQuad.ny1(adr)
val STBTTAlignedQuad.s0: Float
    get() = STBTTAlignedQuad.ns0(adr)
val STBTTAlignedQuad.s1: Float
    get() = STBTTAlignedQuad.ns1(adr)
val STBTTAlignedQuad.t0: Float
    get() = STBTTAlignedQuad.nt0(adr)
val STBTTAlignedQuad.t1: Float
    get() = STBTTAlignedQuad.nt1(adr)


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

fun stbtt_GetFontVMetrics(info: STBTTFontinfo): IntArray = stak {
    val tmp = it.callocInt(3).adr
    STBTruetype.nstbtt_GetFontVMetrics(info.adr, tmp, tmp + Int.BYTES, tmp + Int.BYTES * 2)
    IntArray(3) { memGetInt(tmp + Int.BYTES * it) }
}

fun stbtt_GetPackedQuad(chardata: STBTTPackedchar.Buffer, p: Vec2i, charIndex: Int, q: STBTTAlignedQuad, alignToInteger: Boolean = false) = stak {
    val dummy = it.callocFloat(2).adr
    STBTruetype.nstbtt_GetPackedQuad(chardata.adr, p.x, p.y, charIndex, dummy, dummy + Float.BYTES, q.adr, alignToInteger.i)
}

fun stbtt_GetGlyphBitmapBoxSubpixel(font: STBTTFontinfo, glyph: Int, scale: Vec2, shift: Vec2 = Vec2()): IntArray = stak {
    val tmp = it.callocInt(4).adr
    STBTruetype.nstbtt_GetGlyphBitmapBoxSubpixel(font.adr, glyph, scale.x, scale.y, shift.x, shift.y,
            tmp, tmp + Int.BYTES, tmp + Int.BYTES * 2, tmp + Int.BYTES * 3)
    IntArray(4) { memGetInt(tmp + Int.BYTES * it) }
}

fun stbtt_PackFontRangesRenderIntoRects(spc: STBTTPackContext, info: STBTTFontinfo, range: STBTTPackRange, rects: STBRPRect.Buffer) =
        STBTruetype.nstbtt_PackFontRangesRenderIntoRects(spc.adr, info.adr, range.adr, 1, rects.adr)

var CustomBuffer<*>.pos: Int
    get() = position()
    set(value) {
        position(value)
    }
