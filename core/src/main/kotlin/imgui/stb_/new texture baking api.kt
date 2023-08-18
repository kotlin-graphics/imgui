@file:OptIn(ExperimentalUnsignedTypes::class)

package imgui.stb_

import glm_.vec2.Vec2
import kotlin.math.floor

//////////////////////////////////////////////////////////////////////////////
//
// NEW TEXTURE BAKING API
//
// This provides options for packing multiple fonts into one atlas, not
// perfectly but better than nothing.

class PackedChar {

    // coordinates of bbox in bitmap

    var x0 = 0 // [JVM] use `Int` instead of `UShort` for convenience
    var y0 = 0
    var x1 = 0
    var y1 = 0


    var xOff = 0f
    var yOff = 0f
    var xAdvance = 0f
    var xOff2 = 0f
    var yOff2 = 0f
}

// [JVM] PackContext constructor
// STBTT_DEF int  stbtt_PackBegin(stbtt_pack_context *spc, unsigned char *pixels, int width, int height, int stride_in_bytes, int padding, void *alloc_context);
//
// [JVM] useless here
// Cleans up the packing context and frees all memory.
// STBTT_DEF void stbtt_PackEnd  (stbtt_pack_context *spc);

// Creates character bitmaps from the font_index'th font found in fontdata (use
// font_index=0 if you don't know what that is). It creates num_chars_in_range
// bitmaps for characters with unicode values starting at first_unicode_char_in_range
// and increasing. Data for how to render them is stored in chardata_for_range;
// pass these to stbtt_GetPackedQuad to get back renderable quads.
//
// font_size is the full height of the character from ascender to descender,
// as computed by stbtt_ScaleForPixelHeight. To use a point size as computed
// by stbtt_ScaleForMappingEmToPixels, wrap the point size in STBTT_POINT_SIZE()
// and pass that result as 'font_size':
//       ...,                  20 , ... // font max minus min y is 20 pixels tall
//       ..., STBTT_POINT_SIZE(20), ... // 'M' is 20 pixels tall
fun PackContext.packFontRange(fontdata: UByteArray, fontIndex: Int, fontSize: Float,
                              firstUnicodeCodepointInRange: Int, numCharsInRange: Int/*, stbtt_packedchar *chardata_for_range*/): Int {
    val range = PackRange()
    range.firstUnicodeCodepointInRange = firstUnicodeCodepointInRange
    range.arrayOfUnicodeCodepoints = null
    range.numChars = numCharsInRange
    TODO()
//    range.chardata_for_range = chardata_for_range
//    range.fontSize = fontSize
//    return packFontRanges(fontdata, fontIndex, range, 1)
}

class PackRange {
    var fontSize = 0f
    var firstUnicodeCodepointInRange = 0 // if non-zero, then the chars are continuous, and this is the first codepoint
    var arrayOfUnicodeCodepoints: IntArray? = null // if non-zero, then this is an array of unicode codepoints
    var numChars = 0
    var chardataForRange: Array<PackedChar> = emptyArray() // output

    // don't set these, they're used internally
    var hOversample = 0u
    var vOversample = 0u
}

// Creates character bitmaps from multiple ranges of characters stored in
// ranges. This will usually create a better-packed bitmap than multiple
// calls to stbtt_PackFontRange. Note that you can call this multiple
// times within a single PackBegin/PackEnd.
//STBTT_DEF int  stbtt_PackFontRanges(stbtt_pack_context *spc, const unsigned char *fontdata, int font_index, stbtt_pack_range *ranges, int num_ranges) {
//    stbtt_fontinfo info;
//    int i, j, n, return_value; // [DEAR IMGUI] removed = 1;
//    //stbrp_context *context = (stbrp_context *) spc->pack_info;
//    stbrp_rect    *rects;
//
//    // flag all characters as NOT packed
//    for (i=0; i < num_ranges; ++i)
//    for (j=0; j < ranges[i].num_chars; ++j)
//    ranges[i].chardata_for_range[j].x0 =
//            ranges[i].chardata_for_range[j].y0 =
//            ranges[i].chardata_for_range[j].x1 =
//            ranges[i].chardata_for_range[j].y1 = 0;
//
//    n = 0;
//    for (i=0; i < num_ranges; ++i)
//    n += ranges[i].num_chars;
//
//    rects = (stbrp_rect *) STBTT_malloc(sizeof(*rects) * n, spc->user_allocator_context);
//    if (rects == NULL)
//        return 0;
//
//    info.userdata = spc->user_allocator_context;
//    stbtt_InitFont(&info, fontdata, stbtt_GetFontOffsetForIndex(fontdata,font_index));
//
//    n = stbtt_PackFontRangesGatherRects(spc, &info, ranges, num_ranges, rects);
//
//    stbtt_PackFontRangesPackRects(spc, rects, n);
//
//    return_value = stbtt_PackFontRangesRenderIntoRects(spc, &info, ranges, num_ranges, rects);
//
//    STBTT_free(rects, spc->user_allocator_context);
//    return return_value;
//}


// Oversampling a font increases the quality by allowing higher-quality subpixel
// positioning, and is especially valuable at smaller text sizes.
//
// This function sets the amount of oversampling for all following calls to
// stbtt_PackFontRange(s) or stbtt_PackFontRangesGatherRects for a given
// pack context. The default (no oversampling) is achieved by h_oversample=1
// and v_oversample=1. The total number of pixels required is
// h_oversample*v_oversample larger than the default; for example, 2x2
// oversampling requires 4x the storage of 1x1. For best results, render
// oversampled textures with bilinear filtering. Look at the readme in
// stb/tests/oversample for information about oversampled fonts
//
// To use with PackFontRangesGather etc., you must set it before calls
// call to PackFontRangesGatherRects.
fun PackContext.packSetOversampling(hOversample: UInt, vOversample: UInt) {
    assert(hOversample <= TrueType.MAX_OVERSAMPLE.ui)
    assert(vOversample <= TrueType.MAX_OVERSAMPLE.ui)
    if (hOversample <= TrueType.MAX_OVERSAMPLE.ui)
        this.hOversample = hOversample
    if (vOversample <= TrueType.MAX_OVERSAMPLE.ui)
        this.vOversample = vOversample
}

// [JVM] replaced with direct field access
//
// If skip != 0, this tells stb_truetype to skip any codepoints for which
// there is no corresponding glyph. If skip=0, which is the default, then
// codepoints without a glyph recived the font's "missing character" glyph,
// typically an empty box by convention.
//STBTT_DEF void stbtt_PackSetSkipMissingCodepoints(stbtt_pack_context *spc, int skip)
//{
//    spc->skip_missing = skip;
//}

fun getPackedQuad(charData: Array<PackedChar>, pw: Int, ph: Int, // same data as above
                  charIndex: Int,       // character to display
                  pos: Vec2 = Vec2(),   // pointers to current position in screen pixel space
                  q: AlignedQuad,       // output: quad to draw
                  alignToInteger: Boolean = false) {

    val ipw = 1f / pw
    val iph = 1f / ph
    val b = charData[charIndex]

    if (alignToInteger) {
        val x = floor((pos.x + b.xOff) + 0.5f)
        val y = floor((pos.y + b.yOff) + 0.5f)
        q.x0 = x
        q.y0 = y
        q.x1 = x + b.xOff2 - b.xOff
        q.y1 = y + b.yOff2 - b.yOff
    } else {
        q.x0 = pos.x + b.xOff
        q.y0 = pos.y + b.yOff
        q.x1 = pos.x + b.xOff2
        q.y1 = pos.y + b.yOff2
    }

    q.s0 = b.x0 * ipw
    q.t0 = b.y0 * iph
    q.s1 = b.x1 * ipw
    q.t1 = b.y1 * iph

    pos.x += b.xAdvance
}

//    STBTT_DEF int  stbtt_PackFontRangesGatherRects(stbtt_pack_context *spc, const stbtt_fontinfo *info, stbtt_pack_range *ranges, int num_ranges, stbrp_rect *rects);
//    STBTT_DEF void stbtt_PackFontRangesPackRects(stbtt_pack_context *spc, stbrp_rect *rects, int num_rects);


// Calling these functions in sequence is roughly equivalent to calling
// stbtt_PackFontRanges(). If you more control over the packing of multiple
// fonts, or if you want to pack custom data into a font texture, take a look
// at the source to of stbtt_PackFontRanges() and create a custom version
// using these functions, e.g. call GatherRects multiple times,
// building up a single array of rects, then call PackRects once,
// then call RenderIntoRects repeatedly. This may result in a
// better packing than calling PackFontRanges multiple times
// (or it may not).
// rects array must be big enough to accommodate all characters in the given ranges
fun PackContext.packFontRangesRenderIntoRects(info: FontInfo, ranges: Array<PackRange>, rects: Array<rectpack.Rect>): Boolean {

    var missingGlyph = -1
    var returnValue = true

    // save current values
    val oldHOver = hOversample
    val oldVOver = vOversample

    var k = 0
    for (range in ranges) {
        val fh = range.fontSize
        val scale = if (fh > 0f) info scaleForPixelHeight fh else info scaleForMappingEmToPixels -fh
        hOversample = range.hOversample
        vOversample = range.vOversample
        val recipH = 1f / hOversample.f
        val recipV = 1f / vOversample.f
        val subX = TrueType oversampleShift hOversample.i
        val subY = TrueType oversampleShift vOversample.i
        for (j in 0 until range.numChars) {
            val r = rects[k]
            when {
                r.wasPacked != 0 && r.w != 0 && r.h != 0 -> {
                    val bc = range.chardataForRange[j]
                    val codepoint = range.arrayOfUnicodeCodepoints?.get(j) ?: range.firstUnicodeCodepointInRange
                    val glyph = info findGlyphIndex codepoint
                    val pad = padding

                    // pad on left and top
                    r.x += pad
                    r.y += pad
                    r.w -= pad
                    r.h -= pad
                    val (advance, _) = info getGlyphHMetrics glyph
                    val (x0, y0, _, _) = info.getGlyphBitmapBox(glyph, scale * hOversample.f, scale * vOversample.f)
                    info.makeGlyphBitmapSubpixel(pixels!!, r.x + r.y * strideInBytes,
                                                 r.w - hOversample.i + 1, r.h - vOversample.i + 1,
                                                 strideInBytes,
                                                 scale * hOversample.i,
                                                 scale * vOversample.i,
                                                 0f, 0f,
                                                 glyph)
                    if (hOversample > 1u)
                        TrueType.hPrefilter(pixels!!, r.x + r.y * strideInBytes, r.w, r.h, strideInBytes, hOversample)

                    if (vOversample > 1u)
                        TrueType.vPrefilter(pixels!!, r.x + r.y * strideInBytes, r.w, r.h, strideInBytes, vOversample)

                    bc.x0 = r.x
                    bc.y0 = r.y
                    bc.x1 = r.x + r.w
                    bc.y1 = r.y + r.h
                    bc.xAdvance = scale * advance
                    bc.xOff = x0 * recipH + subX
                    bc.yOff = y0 * recipV + subY
                    bc.xOff2 = (x0 + r.w) * recipH + subX
                    bc.yOff2 = (y0 + r.h) * recipV + subY

                    if (glyph == 0)
                        missingGlyph = j
                }
                skipMissing -> returnValue = false
                r.wasPacked != 0 && r.w == 0 && r.h == 0 && missingGlyph >= 0 -> range.chardataForRange[j] = range.chardataForRange[missingGlyph]
                else -> returnValue = false // if any fail, report failure
            }
            ++k
        }
    }

    // restore original values
    hOversample = oldHOver
    vOversample = oldVOver

    return returnValue
}



// this is an opaque structure that you shouldn't mess with which holds
// all the context needed from PackBegin to PackEnd.
class PackContext
// ~stbtt_PackBegin
// Initializes a packing context stored in the passed-in stbtt_pack_context.
// Future calls using this context will pack characters into the bitmap passed
// in here: a 1-channel bitmap that is width * height. stride_in_bytes is
// the distance from one row to the next (or 0 to mean they are packed tightly
// together). "padding" is the amount of padding to leave between each
// character (normally you want '1' for bitmaps you'll use as textures with
// bilinear filtering).
//
// Returns 0 on failure, 1 on success.
constructor(var pixels: UByteArray?,
            var width: Int,
            var height: Int,
            strideInBytes: Int,
            val padding: Int) {

    val nodes = Array(width - padding) { Node(it) }
    val packInfo = Context(width - padding, height - padding, nodes)
    var skipMissing = false
    var hOversample = 1u
    var vOversample = 1u

    val strideInBytes = if (strideInBytes != 0) strideInBytes else width

    init {
        pixels?.fill(0u, toIndex = width * height) // background of 0 around pixels
    }
}