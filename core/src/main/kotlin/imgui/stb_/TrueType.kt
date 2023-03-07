@file:OptIn(ExperimentalUnsignedTypes::class)

package imgui.stb_

import glm_.f
import glm_.i
//import glm_.*
import glm_.vec2.operators.div
import glm_.vec4.Vec4i
import kool.set
import java.nio.ByteBuffer
import kotlin.math.abs

// [DEAR IMGUI]
// This is a slightly modified version of stb_truetype.h 1.26.
// Mostly fixing for compiler and static analyzer warnings.
// Grep for [DEAR IMGUI] to find the changes.

// stb_truetype.h - v1.26 - public domain
// authored from 2009-2021 by Sean Barrett / RAD Game Tools
//
// =======================================================================
//
//    NO SECURITY GUARANTEE -- DO NOT USE THIS ON UNTRUSTED FONT FILES
//
// This library does no range checking of the offsets found in the file,
// meaning an attacker can use it to read arbitrary memory.
//
// =======================================================================
//
//   This library processes TrueType files:
//        parse files
//        extract glyph metrics
//        extract glyph shapes
//        render glyphs to one-channel bitmaps with antialiasing (box filter)
//        render glyphs to one-channel SDF bitmaps (signed-distance field/function)
//
//   Todo:
//        non-MS cmaps
//        crashproof on bad data
//        hinting? (no longer patented)
//        cleartype-style AA?
//        optimize: use simple memory allocator for intermediates
//        optimize: build edge-list directly from curves
//        optimize: rasterize directly from curves?
//
// ADDITIONAL CONTRIBUTORS
//
//   Mikko Mononen: compound shape support, more cmap formats
//   Tor Andersson: kerning, subpixel rendering
//   Dougall Johnson: OpenType / Type 2 font handling
//   Daniel Ribeiro Maciel: basic GPOS-based kerning
//
//   Misc other:
//       Ryan Gordon
//       Simon Glass
//       github:IntellectualKitty
//       Imanol Celaya
//       Daniel Ribeiro Maciel
//
//   Bug/warning reports/fixes:
//       "Zer" on mollyrocket       Fabian "ryg" Giesen   github:NiLuJe
//       Cass Everitt               Martins Mozeiko       github:aloucks
//       stoiko (Haemimont Games)   Cap Petschulat        github:oyvindjam
//       Brian Hook                 Omar Cornut           github:vassvik
//       Walter van Niftrik         Ryan Griege
//       David Gow                  Peter LaValle
//       David Given                Sergey Popov
//       Ivan-Assen Ivanov          Giumo X. Clanjor
//       Anthony Pesch              Higor Euripedes
//       Johan Duparc               Thomas Fields
//       Hou Qiming                 Derek Vinyard
//       Rob Loach                  Cort Stratton
//       Kenney Phillis Jr.         Brian Costabile
//       Ken Voskuil (kaesve)
//
// VERSION HISTORY
//
//   1.26 (2021-08-28) fix broken rasterizer
//   1.25 (2021-07-11) many fixes
//   1.24 (2020-02-05) fix warning
//   1.23 (2020-02-02) query SVG data for glyphs; query whole kerning table (but only kern not GPOS)
//   1.22 (2019-08-11) minimize missing-glyph duplication; fix kerning if both 'GPOS' and 'kern' are defined
//   1.21 (2019-02-25) fix warning
//   1.20 (2019-02-07) PackFontRange skips missing codepoints; GetScaleFontVMetrics()
//   1.19 (2018-02-11) GPOS kerning, STBTT_fmod
//   1.18 (2018-01-29) add missing function
//   1.17 (2017-07-23) make more arguments const; doc fix
//   1.16 (2017-07-12) SDF support
//   1.15 (2017-03-03) make more arguments const
//   1.14 (2017-01-16) num-fonts-in-TTC function
//   1.13 (2017-01-02) support OpenType fonts, certain Apple fonts
//   1.12 (2016-10-25) suppress warnings about casting away const with -Wcast-qual
//   1.11 (2016-04-02) fix unused-variable warning
//   1.10 (2016-04-02) user-defined fabs(); rare memory leak; remove duplicate typedef
//   1.09 (2016-01-16) warning fix; avoid crash on outofmem; use allocation userdata properly
//   1.08 (2015-09-13) document stbtt_Rasterize(); fixes for vertical & horizontal edges
//   1.07 (2015-08-01) allow PackFontRanges to accept arrays of sparse codepoints;
//                     variant PackFontRanges to pack and render in separate phases;
//                     fix stbtt_GetFontOFfsetForIndex (never worked for non-0 input?);
//                     fixed an assert() bug in the new rasterizer
//                     replace assert() with STBTT_assert() in new rasterizer
//
//   Full history can be found at the end of this file.
//
// LICENSE
//
//   See end of file for license information.
//
// USAGE
//
//   Include this file in whatever places need to refer to it. In ONE C/C++
//   file, write:
//      #define STB_TRUETYPE_IMPLEMENTATION
//   before the #include of this file. This expands out the actual
//   implementation into that C/C++ file.
//
//   To make the implementation private to the file that generates the implementation,
//      #define STBTT_STATIC
//
//   Simple 3D API (don't ship this, but it's fine for tools and quick start)
//           stbtt_BakeFontBitmap()               -- bake a font to a bitmap for use as texture
//           stbtt_GetBakedQuad()                 -- compute quad to draw for a given char
//
//   Improved 3D API (more shippable):
//           #include "stb_rect_pack.h"           -- optional, but you really want it
//           stbtt_PackBegin()
//           stbtt_PackSetOversampling()          -- for improved quality on small fonts
//           stbtt_PackFontRanges()               -- pack and renders
//           stbtt_PackEnd()
//           stbtt_GetPackedQuad()
//
//   "Load" a font file from a memory buffer (you have to keep the buffer loaded)
//           stbtt_InitFont()
//           stbtt_GetFontOffsetForIndex()        -- indexing for TTC font collections
//           stbtt_GetNumberOfFonts()             -- number of fonts for TTC font collections
//
//   Render a unicode codepoint to a bitmap
//           stbtt_GetCodepointBitmap()           -- allocates and returns a bitmap
//           stbtt_MakeCodepointBitmap()          -- renders into bitmap you provide
//           stbtt_GetCodepointBitmapBox()        -- how big the bitmap must be
//
//   Character advance/positioning
//           stbtt_GetCodepointHMetrics()
//           stbtt_GetFontVMetrics()
//           stbtt_GetFontVMetricsOS2()
//           stbtt_GetCodepointKernAdvance()
//
//   Starting with version 1.06, the rasterizer was replaced with a new,
//   faster and generally-more-precise rasterizer. The new rasterizer more
//   accurately measures pixel coverage for anti-aliasing, except in the case
//   where multiple shapes overlap, in which case it overestimates the AA pixel
//   coverage. Thus, anti-aliasing of intersecting shapes may look wrong. If
//   this turns out to be a problem, you can re-enable the old rasterizer with
//        #define STBTT_RASTERIZER_VERSION 1
//   which will incur about a 15% speed hit.
//
// ADDITIONAL DOCUMENTATION
//
//   Immediately after this block comment are a series of sample programs.
//
//   After the sample programs is the "header file" section. This section
//   includes documentation for each API function.
//
//   Some important concepts to understand to use this library:
//
//      Codepoint
//         Characters are defined by unicode codepoints, e.g. 65 is
//         uppercase A, 231 is lowercase c with a cedilla, 0x7e30 is
//         the hiragana for "ma".
//
//      Glyph
//         A visual character shape (every codepoint is rendered as
//         some glyph)
//
//      Glyph index
//         A font-specific integer ID representing a glyph
//
//      Baseline
//         Glyph shapes are defined relative to a baseline, which is the
//         bottom of uppercase characters. Characters extend both above
//         and below the baseline.
//
//      Current Point
//         As you draw text to the screen, you keep track of a "current point"
//         which is the origin of each character. The current point's vertical
//         position is the baseline. Even "baked fonts" use this model.
//
//      Vertical Font Metrics
//         The vertical qualities of the font, used to vertically position
//         and space the characters. See docs for stbtt_GetFontVMetrics.
//
//      Font Size in Pixels or Points
//         The preferred interface for specifying font sizes in stb_truetype
//         is to specify how tall the font's vertical extent should be in pixels.
//         If that sounds good enough, skip the next paragraph.
//
//         Most font APIs instead use "points", which are a common typographic
//         measurement for describing font size, defined as 72 points per inch.
//         stb_truetype provides a point API for compatibility. However, true
//         "per inch" conventions don't make much sense on computer displays
//         since different monitors have different number of pixels per
//         inch. For example, Windows traditionally uses a convention that
//         there are 96 pixels per inch, thus making 'inch' measurements have
//         nothing to do with inches, and thus effectively defining a point to
//         be 1.333 pixels. Additionally, the TrueType font data provides
//         an explicit scale factor to scale a given font's glyphs to points,
//         but the author has observed that this scale factor is often wrong
//         for non-commercial fonts, thus making fonts scaled in points
//         according to the TrueType spec incoherently sized in practice.
//
// DETAILED USAGE:
//
//  Scale:
//    Select how high you want the font to be, in points or pixels.
//    Call ScaleForPixelHeight or ScaleForMappingEmToPixels to compute
//    a scale factor SF that will be used by all other functions.
//
//  Baseline:
//    You need to select a y-coordinate that is the baseline of where
//    your text will appear. Call GetFontBoundingBox to get the baseline-relative
//    bounding box for all characters. SF*-y0 will be the distance in pixels
//    that the worst-case character could extend above the baseline, so if
//    you want the top edge of characters to appear at the top of the
//    screen where y=0, then you would set the baseline to SF*-y0.
//
//  Current point:
//    Set the current point where the first character will appear. The
//    first character could extend left of the current point; this is font
//    dependent. You can either choose a current point that is the leftmost
//    point and hope, or add some padding, or check the bounding box or
//    left-side-bearing of the first character to be displayed and set
//    the current point based on that.
//
//  Displaying a character:
//    Compute the bounding box of the character. It will contain signed values
//    relative to <current_point, baseline>. I.e. if it returns x0,y0,x1,y1,
//    then the character should be displayed in the rectangle from
//    <current_point+SF*x0, baseline+SF*y0> to <current_point+SF*x1,baseline+SF*y1).
//
//  Advancing for the next character:
//    Call GlyphHMetrics, and compute 'current_point += SF * advance'.
//
//
// ADVANCED USAGE
//
//   Quality:
//
//    - Use the functions with Subpixel at the end to allow your characters
//      to have subpixel positioning. Since the font is anti-aliased, not
//      hinted, this is very import for quality. (This is not possible with
//      baked fonts.)
//
//    - Kerning is now supported, and if you're supporting subpixel rendering
//      then kerning is worth using to give your text a polished look.
//
//   Performance:
//
//    - Convert Unicode codepoints to glyph indexes and operate on the glyphs;
//      if you don't do this, stb_truetype is forced to do the conversion on
//      every call.
//
//    - There are a lot of memory allocations. We should modify it to take
//      a temp buffer and allocate from the temp buffer (without freeing),
//      should help performance a lot.
//
// NOTES
//
//   The system uses the raw data found in the .ttf file without changing it
//   and without building auxiliary data structures. This is a bit inefficient
//   on little-endian systems (the data is big-endian), but assuming you're
//   caching the bitmaps or glyph shapes this shouldn't be a big deal.
//
//   It appears to be very hard to programmatically determine what font a
//   given file is in a general way. I provide an API for this, but I don't
//   recommend it.
//
//
// PERFORMANCE MEASUREMENTS FOR 1.06:
//
//                      32-bit     64-bit
//   Previous release:  8.83 s     7.68 s
//   Pool allocations:  7.72 s     6.34 s
//   Inline sort     :  6.54 s     5.65 s
//   New rasterizer  :  5.63 s     5.00 s

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
////
////  SAMPLE PROGRAMS
////
//
//  Incomplete text-in-3d-api example, which draws quads properly aligned to be lossless.
//  See "tests/truetype_demo_win32.c" for a complete version.
//
//#if 0
//#define STB_TRUETYPE_IMPLEMENTATION  // force following include to generate implementation
//#include "stb_truetype.h"
//
//unsigned char ttf_buffer[1<<20];
//unsigned char temp_bitmap[512*512];
//
//stbtt_bakedchar cdata[96]; // ASCII 32..126 is 95 glyphs
//GLuint ftex;
//
//void my_stbtt_initfont(void)
//{
//    fread(ttf_buffer, 1, 1<<20, fopen("c:/windows/fonts/times.ttf", "rb"));
//    stbtt_BakeFontBitmap(ttf_buffer,0, 32.0, temp_bitmap,512,512, 32,96, cdata); // no guarantee this fits!
//    // can free ttf_buffer at this point
//    glGenTextures(1, &ftex);
//    glBindTexture(GL_TEXTURE_2D, ftex);
//    glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, 512,512, 0, GL_ALPHA, GL_UNSIGNED_BYTE, temp_bitmap);
//    // can free temp_bitmap at this point
//    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
//}
//
//void my_stbtt_print(float x, float y, char *text)
//{
//    // assume orthographic projection with units = screen pixels, origin at top left
//glEnable(GL_BLEND);
//glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//    glEnable(GL_TEXTURE_2D);
//    glBindTexture(GL_TEXTURE_2D, ftex);
//    glBegin(GL_QUADS);
//    while (*text) {
//    if (*text >= 32 && *text < 128) {
//    stbtt_aligned_quad q;
//    stbtt_GetBakedQuad(cdata, 512,512, *text-32, &x,&y,&q,1);//1=opengl & d3d10+,0=d3d9
//    glTexCoord2f(q.s0,q.t0); glVertex2f(q.x0,q.y0);
//    glTexCoord2f(q.s1,q.t0); glVertex2f(q.x1,q.y0);
//    glTexCoord2f(q.s1,q.t1); glVertex2f(q.x1,q.y1);
//    glTexCoord2f(q.s0,q.t1); glVertex2f(q.x0,q.y1);
//}
//    ++text;
//}
//    glEnd();
//}
//#endif
////
////
////////////////////////////////////////////////////////////////////////////////
////
//// Complete program (this compiles): get a single bitmap, print as ASCII art
////
//#if 0
//#include <stdio.h>
//#define STB_TRUETYPE_IMPLEMENTATION  // force following include to generate implementation
//#include "stb_truetype.h"
//
//char ttf_buffer[1<<25];
//
//int main(int argc, char **argv)
//{
//    stbtt_fontinfo font;
//    unsigned char *bitmap;
//    int w,h,i,j,c = (argc > 1 ? atoi(argv[1]) : 'a'), s = (argc > 2 ? atoi(argv[2]) : 20);
//
//    fread(ttf_buffer, 1, 1<<25, fopen(argc > 3 ? argv[3] : "c:/windows/fonts/arialbd.ttf", "rb"));
//
//    stbtt_InitFont(&font, ttf_buffer, stbtt_GetFontOffsetForIndex(ttf_buffer,0));
//    bitmap = stbtt_GetCodepointBitmap(&font, 0,stbtt_ScaleForPixelHeight(&font, s), c, &w, &h, 0,0);
//
//    for (j=0; j < h; ++j) {
//    for (i=0; i < w; ++i)
//    putchar(" .:ioVM@"[bitmap[j*w+i]>>5]);
//    putchar('\n');
//}
//    return 0;
//}
//#endif
////
//// Output:
////
////     .ii.
////    @@@@@@.
////   V@Mio@@o
////   :i.  V@V
////     :oM@@M
////   :@@@MM@M
////   @@o  o@M
////  :@@.  M@M
////   @@@o@@@@
////   :M@@V:@@.
////
////////////////////////////////////////////////////////////////////////////////
////
//// Complete program: print "Hello World!" banner, with bugs
////
//#if 0
//char buffer[24<<20];
//unsigned char screen[20][79];
//
//int main(int arg, char **argv)
//{
//    stbtt_fontinfo font;
//    int i,j,ascent,baseline,ch=0;
//    float scale, xpos=2; // leave a little padding in case the character extends left
//    char *text = "Heljo World!"; // intentionally misspelled to show 'lj' brokenness
//
//    fread(buffer, 1, 1000000, fopen("c:/windows/fonts/arialbd.ttf", "rb"));
//    stbtt_InitFont(&font, buffer, 0);
//
//    scale = stbtt_ScaleForPixelHeight(&font, 15);
//    stbtt_GetFontVMetrics(&font, &ascent,0,0);
//    baseline = (int) (ascent*scale);
//
//    while (text[ch]) {
//        int advance,lsb,x0,y0,x1,y1;
//        float x_shift = xpos - (float) floor(xpos);
//        stbtt_GetCodepointHMetrics(&font, text[ch], &advance, &lsb);
//        stbtt_GetCodepointBitmapBoxSubpixel(&font, text[ch], scale,scale,x_shift,0, &x0,&y0,&x1,&y1);
//        stbtt_MakeCodepointBitmapSubpixel(&font, &screen[baseline + y0][(int) xpos + x0], x1-x0,y1-y0, 79, scale,scale,x_shift,0, text[ch]);
//        // note that this stomps the old data, so where character boxes overlap (e.g. 'lj') it's wrong
//        // because this API is really for baking character bitmaps into textures. if you want to render
//        // a sequence of characters, you really need to render each bitmap to a temp buffer, then
//        // "alpha blend" that into the working buffer
//        xpos += (advance * scale);
//        if (text[ch+1])
//            xpos += scale*stbtt_GetCodepointKernAdvance(&font, text[ch],text[ch+1]);
//        ++ch;
//    }
//
//    for (j=0; j < 20; ++j) {
//    for (i=0; i < 78; ++i)
//    putchar(" .:ioVM@"[screen[j][i]>>5]);
//    putchar('\n');
//}
//
//    return 0;
//}
//#endif


object TrueType {

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
//////
//////   INTEGRATION WITH YOUR CODEBASE
//////
//////   The following sections allow you to supply alternate definitions
//////   of C library functions used by stb_truetype, e.g. if you don't
//////   link with the C runtime library.
//
//#ifdef STB_TRUETYPE_IMPLEMENTATION
//// #define your own (u)stbtt_int8/16/32 before including to override this
//#ifndef stbtt_uint8
//typedef unsigned char   stbtt_uint8;
//typedef signed   char   stbtt_int8;
//typedef unsigned short  stbtt_uint16;
//typedef signed   short  stbtt_int16;
//typedef unsigned int    stbtt_uint32;
//typedef signed   int    stbtt_int32;
//#endif
//
//typedef char stbtt__check_size32[sizeof(stbtt_int32)==4 ? 1 : -1];
//typedef char stbtt__check_size16[sizeof(stbtt_int16)==2 ? 1 : -1];

    /////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    //
    //   INTERFACE
    //
    //


    // [JVM] -> Buf class

    //////////////////////////////////////////////////////////////////////////////
    //
    // TEXTURE BAKING API
    //
    // If you use this API, you only have to call two functions ever.
    //

    // [JVM] -> `texture baking api.kt`


    //////////////////////////////////////////////////////////////////////////////
    //
    // FONT LOADING
    //
    //

    // [JVM] -> `font loading.kt`

    //////////////////////////////////////////////////////////////////////////////
    //
    // CHARACTER TO GLYPH-INDEX CONVERSIOn

    // [JVM] -> character.kt

    //////////////////////////////////////////////////////////////////////////////
    //
    // CHARACTER PROPERTIES
    //

    // [JVM] -> character.kt


    //////////////////////////////////////////////////////////////////////////////
    //
    // GLYPH SHAPES (you probably don't need these, but they have to go before
    // the bitmaps for C declaration-order reasons)
    //

    // [JVM] -> `glyph shapes.kt`

    //////////////////////////////////////////////////////////////////////////////
    //
    // BITMAP RENDERING
    //

    // [JVM] -> `bitmap rendering.kt`

    //////////////////////////////////////////////////////////////////////////////
    //
    // Signed Distance Function (or Field) rendering

    // [JVM] -> `sdf rendering.kt`

    //////////////////////////////////////////////////////////////////////////////
    //
    // Finding the right font...
    //

    // [JVM] -> `finding the right font.kt`

    ///////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////
    ////
    ////   IMPLEMENTATION
    ////
    ////
    //
    //#ifdef STB_TRUETYPE_IMPLEMENTATION
    //
    //#ifndef STBTT_MAX_OVERSAMPLE
    var MAX_OVERSAMPLE = 8
//#endif

    init {
        if (MAX_OVERSAMPLE > 255)
            error("STBTT_MAX_OVERSAMPLE cannot be > 255")
    }

//typedef int stbtt__test_oversample_pow2[(STBTT_MAX_OVERSAMPLE & (STBTT_MAX_OVERSAMPLE-1)) == 0 ? 1 : -1];

    var RASTERIZER_VERSION = 2

//#ifdef _MSC_VER
//#define STBTT__NOTUSED(v)  (void)(v)
//#else
//#define STBTT__NOTUSED(v)  (void)sizeof(v)
//#endif

    //////////////////////////////////////////////////////////////////////////
    //
    // stbtt__buf helpers to parse data from file
    //

    // [JVM] -> Buf class

    ////////////////////////////////////////////////////////////////////////////
    //
    // accessors to parse data from file
    //

    // on platforms that don't allow misaligned reads, if we want to allow
    // truetype fonts that aren't padded to alignment, define ALLOW_UNALIGNED_TRUETYPE

//    #define ttBYTE(p)     (* (stbtt_uint8 *) (p))
//    #define ttCHAR(p)     (* (stbtt_int8 *) (p))
//    #define ttFixed(p)    ttLONG(p)

    internal fun UByteArray.ushort(offset: Int): UInt {
        val a = this[offset].ui shl 8
        val b = this[offset + 1].ui
        return (a + b).us.ui
    }

    internal fun UByteArray.short(offset: Int): Int = ushort(offset).toShort().toInt()

    internal fun UByteArray.ulong(offset: Int): UInt {
        val a = this[offset].ui shl 24
        val b = this[offset + 1].ui shl 16
        val c = this[offset + 2].ui shl 8
        val d = this[offset + 3].ui
        return a + b + c + d
    }

    internal fun UByteArray.long(offset: Int): Int = ulong(offset).toInt()


    fun tag4(p: UByteArray, c0: Int, c1: Int, c2: Int, c3: Int) = TrueType.tag4(p, 0, c0, c1, c2, c3)
    fun tag4(p: UByteArray, index: Int, c0: Int, c1: Int, c2: Int, c3: Int) = p[index] == c0.ub && p[index + 1] == c1.ub && p[index + 2] == c2.ub && p[index + 3] == c3.ub

    fun tag(p: UByteArray, str: String) = tag(p, 0, str)
    fun tag(p: UByteArray, index: Int, str: String) = tag4(p, index, str[0].code, str[1].code, str[2].code, str[3].code)

    fun isFont(font: UByteArray): Boolean = when { // check the version number
        tag4(font, '1'.code, 0, 0, 0) -> true // TrueType 1
        tag(font, "typ1") -> true // TrueType with type 1 font -- we don't support this!
        tag(font, "OTTO") -> true // OpenType with CFF
        tag4(font, 0, 1, 0, 0) -> true // OpenType 1.0
        tag(font, "true") -> true // Apple specification for TrueType fonts
        else -> false
    }

    // @OPTIMIZE: binary search
    fun findTable(data: UByteArray, fontStart: Int, tag: String): UInt {
        val numTables = data.ushort(fontStart + 4)
        val tableDir = fontStart + 12
        for (i in 0 until numTables.i) {
            val loc = tableDir + 16 * i
            if (tag(data, loc + 0, tag))
                return data.ulong(loc + 8)
        }
        return 0u
    }

    fun getSubrs(cff: Buf, fontDict: Buf): Buf {
        val privateLoc = IntArray(2)
        fontDict.dictGetInts(18, privateLoc)
        if (privateLoc[1] == 0 || privateLoc[0] == 0) return Buf()
        val pDict = cff.range(privateLoc[1], privateLoc[0])
        val subrsOff = pDict.dictGetInt(19)
        if (subrsOff == 0) return Buf()
        cff seek (privateLoc[1] + subrsOff)
        return cff.cffGetIndex()
    }


// since most people won't use this, find this table the first time it's needed
//    static int stbtt__get_svg(stbtt_fontinfo *info)
//    {
//        stbtt_uint32 t
//                if (info->svg < 0) {
//        t = stbtt__find_table(info->data, info->fontstart, "SVG ")
//        if (t) {
//            stbtt_uint32 offset = ttULONG (info->data+t+2)
//            info->svg = t+offset
//        } else { info ->
//            svg = 0
//        }
//    }
//        return info->svg
//    }


//STBTT_DEF int stbtt_GetCodepointShape(const stbtt_fontinfo *info, int unicode_codepoint, stbtt_vertex **vertices)
//{
//    return stbtt_GetGlyphShape(info, stbtt_FindGlyphIndex(info, unicode_codepoint), vertices);
//}

    // [JVM] stbtt_setvertex -> Vertex class

    infix fun FontInfo.getGlyfOffset(glyphIndex: Int): Int {
        val g1: Int
        val g2: Int

        assert(cff.isEmpty())

        if (glyphIndex >= numGlyphs) return -1 // glyph index out of range
        if (indexToLocFormat >= 2) return -1 // unknown index->glyph map format

        if (indexToLocFormat == 0) {
            g1 = glyf + data.ushort(loca + glyphIndex * 2).i * 2
            g2 = glyf + data.ushort(loca + glyphIndex * 2 + 2).i * 2
        } else {
            g1 = glyf + data.ulong(loca + glyphIndex * 4).i
            g2 = glyf + data.ulong(loca + glyphIndex * 4 + 4).i
        }

        return if (g1 == g2) -1 else g1 // if length is 0, return -1
    }

    fun FontInfo.getGlyphInfoT2(glyphIndex: Int, box: Vec4i): Int {
        val c = Csctx(true)
        val r = runCharString(glyphIndex, c)
        box.put(
                if (r) c.minX else 0,
                if (r) c.minY else 0,
                if (r) c.maxX else 0,
                if (r) c.maxY else 0)
        return if (r) c.numVertices else 0
    }

    fun closeShape(vertices: Array<Vertex>, numVertices_: Int, wasOff: Boolean, startOff: Boolean,
                   sx: Int, sy: Int, scx: Int, scy: Int, cx: Int, cy: Int): Int {
        var numVertices = numVertices_
        when {
            startOff -> {
                if (wasOff)
                    vertices[numVertices++].set(Vertex.Type.curve, (cx + scx) shr 1, (cy + scy) shr 1, cx, cy)
                vertices[numVertices++].set(Vertex.Type.curve, sx, sy, scx, scy)
            }

            wasOff -> vertices[numVertices++].set(Vertex.Type.curve, sx, sy, cx, cy)
            else -> vertices[numVertices++].set(Vertex.Type.line, sx, sy, 0, 0)
        }
        return numVertices
    }

    // [JVM] -> Csctx.kt
    // stbtt__csctx
    // STBTT__CSCTX_INIT
    // stbtt__track_vertex
    // stbtt__csctx_v
    // stbtt__csctx_close_shape
    // stbtt__csctx_rmove_to
    // stbtt__csctx_rline_to
    // stbtt__csctx_rccurve_to

    fun getSubr(idx: Buf, n_: Int): Buf {
        var n = n_
        val count = idx.cffIndexCount
        var bias = 107
        if (count >= 33900)
            bias = 32768
        else if (count >= 1240)
            bias = 1131
        n += bias
        if (n < 0 || n >= count)
            return Buf()
        return idx cffIndexGet n
    }

    fun cidGetGlyphSubrs(info: FontInfo, glyphIndex: Int): Buf {
        val fdSelect = info.fdSelect
        var fdSelector = -1

        fdSelect seek 0
        val fmt = fdSelect.get8().i
        if (fmt == 0) {
            // untested
            fdSelect skip glyphIndex
            fdSelector = fdSelect.get8().i
        } else if (fmt == 3) {
            val nRanges = fdSelect.get16().i
            var start = fdSelect.get16().i
            for (i in 0 until nRanges) {
                val v = fdSelect.get8().i
                val end = fdSelect.get16().i
                if (glyphIndex in start until end) {
                    fdSelector = v
                    break
                }
                start = end
            }
        }
        if (fdSelector == -1) return Buf() // [DEAR IMGUI] fixed, see #6007 and nothings/stb#1422
        return getSubrs(info.cff, info.fontDicts cffIndexGet fdSelector)
    }

    fun FontInfo.runCharString(glyphIndex: Int, c: Csctx): Boolean {
        var inHeader = true
        var maskBits = 0
        var subrStackHeight = 0
        var sp = 0
        var b0: Int
        var hasSubrs = false
        var clearStack: Boolean
        val s = FloatArray(48)
        val subrStack = Array(10) { Buf() }
        var subrs = Buf(subrs)

        fun cserr(s: String): Boolean {
            System.err.println(s)
            return false
        }
        TODO()
        // this currently ignores the initial width value, which isn't needed if we have hmtx
//        var b = charStrings cffIndexGet glyphIndex
//        while (b.pos < b.lim) {
//            clearStack = true
//            b0 = b.get8()
//            when (b0) {
//                // @TODO implement hinting
//                0x13, // hintmask
//                0x14  // cntrmask
//                -> {
//                    if (inHeader)
//                        maskBits += sp / 2 // implicit "vstem"
//                    inHeader = false
//                    b skip ((maskBits + 7) / 8)
//                }
//
//                0x01, // hstem
//                0x03, // vstem
//                0x12, // hstemhm
//                0x17  // vstemhm
//                -> maskBits += sp / 2
//
//                // rmoveto
//                0x15 -> {
//                    inHeader = false
//                    if (sp < 2) return cserr("rmoveto stack")
//                    c.rMoveTo(s[sp - 2], s[sp - 1])
//                }
//
//                // vmoveto
//                0x04 -> {
//                    inHeader = false
//                    if (sp < 1) return cserr("vmoveto stack")
//                    c.rMoveTo(0f, s[sp - 1])
//                }
//
//                // hmoveto
//                0x16 -> {
//                    inHeader = false
//                    if (sp < 1) return cserr("hmoveto stack")
//                    c.rMoveTo(s[sp - 1], 0f)
//                }
//
//                // rlineto
//                0x05 -> {
//                    if (sp < 2) return cserr("rlineto stack")
//                    var i = 0
//                    while (i + 1 < sp)
//                        c.rLineTo(s[i++], s[i++])
//                }
//
//                // hlineto/vlineto and vhcurveto/hvcurveto alternate horizontal and vertical
//                // starting from a different place.
//
//                // vlineto
//                0x07 -> {
//                    if (sp < 1) return cserr("vlineto stack")
//                    c.rLineTo(0f, s[0]) // i = 0
//                    var i = 1
//                    while (true) {
//                        if (i >= sp) break
//                        c.rLineTo(s[i++], 0f)
//                        if (i >= sp) break
//                        c.rLineTo(0f, s[i++])
//                    }
//                }
//
//                // hlineto
//                0x06 -> {
//                    if (sp < 1) return cserr("hlineto stack")
//                    var i = 0
//                    while (true) {
//                        if (i >= sp) break
//                        c.rLineTo(s[i++], 0f)
//                        if (i >= sp) break
//                        c.rLineTo(0f, s[i++])
//                    }
//                }
//
//                // hvcurveto
//                0x1F -> {
//                    if (sp < 4) return cserr("hvcurveto stack")
//                    c.rcCurveTo(s[0], 0f, s[1], s[2], if (sp == 5) s[4] else 0f, s[3]) // i = 0
//                    var i = 4
//                    while (true) {
//                        if (i + 3 >= sp) break
//                        c.rcCurveTo(0f, s[i], s[i + 1], s[i + 2], s[i + 3], if (sp - i == 5) s[i + 4] else 0f)
//                        i += 4
//                        if (i + 3 >= sp) break
//                        c.rcCurveTo(s[i], 0f, s[i + 1], s[i + 2], if (sp - i == 5) s[i + 4] else 0f, s[i + 3])
//                        i += 4
//                    }
//                }
//                // vhcurveto
//                0x1E -> {
//                    if (sp < 4) return cserr("vhcurveto stack")
//                    var i = 0
//                    while (true) {
//                        if (i + 3 >= sp) break
//                        c.rcCurveTo(0f, s[i], s[i + 1], s[i + 2], s[i + 3], if (sp - i == 5) s[i + 4] else 0f)
//                        i += 4
//                        if (i + 3 >= sp) break
//                        c.rcCurveTo(s[i], 0f, s[i + 1], s[i + 2], if (sp - i == 5) s[i + 4] else 0f, s[i + 3])
//                        i += 4
//                    }
//                }
//                // rrcurveto
//                0x08 -> {
//                    if (sp < 6) return cserr("rcurveline stack")
//                    var i = 0
//                    while (i + 5 < sp)
//                        c.rcCurveTo(s[i++], s[i++], s[i++], s[i++], s[i++], s[i++])
//                }
//                // rcurveline
//                0x18 -> {
//                    if (sp < 8) return cserr("rcurveline stack")
//                    var i = 0
//                    while (i + 5 < sp - 2)
//                        c.rcCurveTo(s[i++], s[i++], s[i++], s[i++], s[i++], s[i++])
//                    if (i + 1 >= sp) return cserr("rcurveline stack")
//                    c.rLineTo(s[i], s[i + 1])
//                }
//                // rlinecurve
//                0x19
//                -> {
//                    if (sp < 8) return cserr("rlinecurve stack")
//                    var i = 0
//                    while (i + 1 < sp - 6)
//                        c.rLineTo(s[i++], s[i++])
//                    if (i + 5 >= sp) return cserr("rlinecurve stack")
//                    c.rcCurveTo(s[i], s[i + 1], s[i + 2], s[i + 3], s[i + 4], s[i + 5])
//                }
//
//                0x1A, // vvcurveto
//                0x1B  // hhcurveto
//                -> {
//                    if (sp < 4) return cserr("(vv|hh)curveto stack")
//                    var f = 0f
//                    var i = 0
//                    if (sp has 1) {
//                        f = s[i]
//                        i++
//                    }
//                    while (i + 3 < sp) {
//                        if (b0 == 0x1B)
//                            c.rcCurveTo(s[i++], f, s[i++], s[i++], s[i++], 0f)
//                        else
//                            c.rcCurveTo(f, s[i++], s[i++], s[i++], 0f, s[i++])
//                        f = 0f
//                    }
//                }
//                // callsubr
//                0x0A -> {
//                    if (!hasSubrs) {
//                        if (info.fdSelect.hasRemaining())
//                            subrs = TrueType.cidGetGlyphSubrs(info, glyphIndex)
//                        hasSubrs = true
//                    }
//                    // fallthrough
//                    if (sp < 1) return cserr("call(g|)subr stack")
//                    val v = s[--sp].toInt()
//                    if (subrStackHeight >= 10) return cserr("recursion limit")
//                    subrStack[subrStackHeight++] = b
//                    b = stbtt__get_subr(b0 == 0x0A ? subrs : info->gsubrs, v);
//                    if (b.size == 0) return STBTT__CSERR("subr not found");
//                    b = TrueType.getSubr(b, v)
//                    if (b.isEmpty()) return cserr("subr not found")
//                    b.pos = 0
//                    clearStack = false
//                }
//                // callgsubr
//                0x1D -> {
//                    if (sp < 1) return cserr("call(g|)subr stack")
//                    val v = s[--sp].i
//                    if (subrStackHeight >= 10)
//                        return cserr("recursion limit")
//                    subrStack[subrStackHeight++] = b
//                    b = if (b0 == 0x0A) subrs else info.gSubrs
//                    b = TrueType.getSubr(b, v)
//                    if (b.isEmpty()) return cserr("subr not found")
//                    b.pos = 0
//                    clearStack = false
//                }
//                // return
//                0x0B
//                -> {
//                    if (subrStackHeight <= 0) return cserr("return outside subr")
//                    b = subrStack[--subrStackHeight]
//                    clearStack = false
//                }
//                // endchar
//                0x0E
//                -> {
//                    c.closeShape()
//                    return true
//                }
//                // two-byte escape
//                0x0C
//                -> {
//                    val dx1: Float
//                    val dx2: Float
//                    val dx3: Float
//                    val dx4: Float
//                    val dx5: Float
//                    var dx6: Float
//                    val dy1: Float
//                    val dy2: Float
//                    val dy3: Float
//                    val dy4: Float
//                    val dy5: Float
//                    var dy6: Float
//                    when (b.get8()) {
//                        // @TODO These "flex" implementations ignore the flex-depth and resolution,
//                        // and always draw beziers.
//
//                        // hflex
//                        0x22 -> {
//                            if (sp < 7) return cserr("hflex stack")
//                            dx1 = s[0]
//                            dx2 = s[1]
//                            dy2 = s[2]
//                            dx3 = s[3]
//                            dx4 = s[4]
//                            dx5 = s[5]
//                            dx6 = s[6]
//                            c.rcCurveTo(dx1, 0f, dx2, dy2, dx3, 0f)
//                            c.rcCurveTo(dx4, 0f, dx5, -dy2, dx6, 0f)
//                        }
//                        // flex
//                        0x23 -> {
//                            if (sp < 13) return cserr("flex stack")
//                            dx1 = s[0]
//                            dy1 = s[1]
//                            dx2 = s[2]
//                            dy2 = s[3]
//                            dx3 = s[4]
//                            dy3 = s[5]
//                            dx4 = s[6]
//                            dy4 = s[7]
//                            dx5 = s[8]
//                            dy5 = s[9]
//                            dx6 = s[10]
//                            dy6 = s[11]
//                            //fd is s[12]
//                            c.rcCurveTo(dx1, dy1, dx2, dy2, dx3, dy3)
//                            c.rcCurveTo(dx4, dy4, dx5, dy5, dx6, dy6)
//                        }
//                        // hflex1
//                        0x24 -> {
//                            if (sp < 9) return cserr("hflex1 stack")
//                            dx1 = s[0]
//                            dy1 = s[1]
//                            dx2 = s[2]
//                            dy2 = s[3]
//                            dx3 = s[4]
//                            dx4 = s[5]
//                            dx5 = s[6]
//                            dy5 = s[7]
//                            dx6 = s[8]
//                            c.rcCurveTo(dx1, dy1, dx2, dy2, dx3, 0f)
//                            c.rcCurveTo(dx4, 0f, dx5, dy5, dx6, -(dy1 + dy2 + dy5))
//                        }
//                        // flex1
//                        0x25 -> {
//                            if (sp < 11) return cserr("flex1 stack")
//                            dx1 = s[0]
//                            dy1 = s[1]
//                            dx2 = s[2]
//                            dy2 = s[3]
//                            dx3 = s[4]
//                            dy3 = s[5]
//                            dx4 = s[6]
//                            dy4 = s[7]
//                            dx5 = s[8]
//                            dy5 = s[9]
//                            dx6 = s[10]
//                            dy6 = s[10]
//                            val dx = dx1 + dx2 + dx3 + dx4 + dx5
//                            val dy = dy1 + dy2 + dy3 + dy4 + dy5
//                            if (abs(dx) > abs(dy))
//                                dy6 = -dy
//                            else
//                                dx6 = -dx
//                            c.rcCurveTo(dx1, dy1, dx2, dy2, dx3, dy3)
//                            c.rcCurveTo(dx4, dy4, dx5, dy5, dx6, dy6)
//                        }
//
//                        else -> TODO("unimplemented")
//                    }
//                }
//
//                else -> {
//                    if (b0 != 255 && b0 != 28 && b0 < 32)
//                        return cserr("reserved operator")
//
//                    // push immediate
//                    val f = when (b0) {
//                        255 -> b.get32().f / 0x10000
//                        else -> {
//                            b skip -1
//                            b.cffInt.f
//                        }
//                    }
//                    if (sp >= 48) return cserr("push stack overflow")
//                    s[sp++] = f
//                    clearStack = false
//                }
//            }
//            if (clearStack) sp = 0
//        }
//        return cserr("no endchar")
    }

    // [JVM] -> `glyph shapes.kt`
    // stbtt__GetGlyphShapeT2
    // stbtt__GetGlyphInfoT2

//static int  stbtt__GetGlyphKernInfoAdvance(const stbtt_fontinfo *info, int glyph1, int glyph2)
//{
//    stbtt_uint8 *data = info->data + info->kern;
//    stbtt_uint32 needle, straw;
//    int l, r, m;
//
//    // we only look at the first table. it must be 'horizontal' and format 0.
//    if (!info->kern)
//    return 0;
//    if (ttUSHORT(data+2) < 1) // number of tables, need at least 1
//        return 0;
//    if (ttUSHORT(data+8) != 1) // horizontal flag must be set in format
//        return 0;
//
//    l = 0;
//    r = ttUSHORT(data+10) - 1;
//    needle = glyph1 << 16 | glyph2;
//    while (l <= r) {
//        m = (l + r) >> 1;
//        straw = ttULONG(data+18+(m*6)); // note: unaligned read
//        if (needle < straw)
//            r = m - 1;
//        else if (needle > straw)
//            l = m + 1;
//        else
//            return ttSHORT(data+22+(m*6));
//    }
//    return 0;
//}
//
//static stbtt_int32  stbtt__GetCoverageIndex(stbtt_uint8 *coverageTable, int glyph)
//{
//    stbtt_uint16 coverageFormat = ttUSHORT(coverageTable);
//    switch(coverageFormat) {
//        case 1: {
//        stbtt_uint16 glyphCount = ttUSHORT(coverageTable + 2);
//
//        // Binary search.
//        stbtt_int32 l=0, r=glyphCount-1, m;
//        int straw, needle=glyph;
//        while (l <= r) {
//            stbtt_uint8 *glyphArray = coverageTable + 4;
//            stbtt_uint16 glyphID;
//            m = (l + r) >> 1;
//            glyphID = ttUSHORT(glyphArray + 2 * m);
//            straw = glyphID;
//            if (needle < straw)
//                r = m - 1;
//            else if (needle > straw)
//                l = m + 1;
//            else {
//                return m;
//            }
//        }
//    } break;
//
//        case 2: {
//        stbtt_uint16 rangeCount = ttUSHORT(coverageTable + 2);
//        stbtt_uint8 *rangeArray = coverageTable + 4;
//
//        // Binary search.
//        stbtt_int32 l=0, r=rangeCount-1, m;
//        int strawStart, strawEnd, needle=glyph;
//        while (l <= r) {
//            stbtt_uint8 *rangeRecord;
//            m = (l + r) >> 1;
//            rangeRecord = rangeArray + 6 * m;
//            strawStart = ttUSHORT(rangeRecord);
//            strawEnd = ttUSHORT(rangeRecord + 2);
//            if (needle < strawStart)
//                r = m - 1;
//            else if (needle > strawEnd)
//                l = m + 1;
//            else {
//                stbtt_uint16 startCoverageIndex = ttUSHORT(rangeRecord + 4);
//                return startCoverageIndex + glyph - strawStart;
//            }
//        }
//    } break;
//
//        default: return -1; // unsupported
//    }
//
//    return -1;
//}
//
//static stbtt_int32  stbtt__GetGlyphClass(stbtt_uint8 *classDefTable, int glyph)
//{
//    stbtt_uint16 classDefFormat = ttUSHORT(classDefTable);
//    switch(classDefFormat)
//    {
//        case 1: {
//        stbtt_uint16 startGlyphID = ttUSHORT(classDefTable + 2);
//        stbtt_uint16 glyphCount = ttUSHORT(classDefTable + 4);
//        stbtt_uint8 *classDef1ValueArray = classDefTable + 6;
//
//        if (glyph >= startGlyphID && glyph < startGlyphID + glyphCount)
//            return (stbtt_int32)ttUSHORT(classDef1ValueArray + 2 * (glyph - startGlyphID));
//
//        classDefTable = classDef1ValueArray + 2 * glyphCount;
//    } break;
//
//        case 2: {
//        stbtt_uint16 classRangeCount = ttUSHORT(classDefTable + 2);
//        stbtt_uint8 *classRangeRecords = classDefTable + 4;
//
//        // Binary search.
//        stbtt_int32 l=0, r=classRangeCount-1, m;
//        int strawStart, strawEnd, needle=glyph;
//        while (l <= r) {
//            stbtt_uint8 *classRangeRecord;
//            m = (l + r) >> 1;
//            classRangeRecord = classRangeRecords + 6 * m;
//            strawStart = ttUSHORT(classRangeRecord);
//            strawEnd = ttUSHORT(classRangeRecord + 2);
//            if (needle < strawStart)
//                r = m - 1;
//            else if (needle > strawEnd)
//                l = m + 1;
//            else
//                return (stbtt_int32)ttUSHORT(classRangeRecord + 4);
//        }
//
//        classDefTable = classRangeRecords + 6 * classRangeCount;
//    } break;
//
//        default:
//            return -1; // Unsupported definition type, return an error.
//    }
//    // "All glyphs not assigned to a class fall into class 0". (OpenType spec)
//    return 0;
//}
//
//// Define to STBTT_assert(x) if you want to break on unimplemented formats.
//#define STBTT_GPOS_TODO_assert(x)
//
//static stbtt_int32  stbtt__GetGlyphGPOSInfoAdvance(const stbtt_fontinfo *info, int glyph1, int glyph2)
//{
//    stbtt_uint16 lookupListOffset;
//    stbtt_uint8 *lookupList;
//    stbtt_uint16 lookupCount;
//    stbtt_uint8 *data;
//    stbtt_int32 i;
//
//    if (!info->gpos) return 0;
//
//    data = info->data + info->gpos;
//
//    if (ttUSHORT(data+0) != 1) return 0; // Major version 1
//    if (ttUSHORT(data+2) != 0) return 0; // Minor version 0
//
//    lookupListOffset = ttUSHORT(data+8);
//    lookupList = data + lookupListOffset;
//    lookupCount = ttUSHORT(lookupList);
//
//    for (i=0; i<lookupCount; ++i) {
//    stbtt_uint16 lookupOffset = ttUSHORT(lookupList + 2 + 2 * i);
//    stbtt_uint8 *lookupTable = lookupList + lookupOffset;
//
//    stbtt_uint16 lookupType = ttUSHORT(lookupTable);
//    stbtt_uint16 subTableCount = ttUSHORT(lookupTable + 4);
//    stbtt_uint8 *subTableOffsets = lookupTable + 6;
//    switch(lookupType) {
//        case 2: { // Pair Adjustment Positioning Subtable
//        stbtt_int32 sti;
//        for (sti=0; sti<subTableCount; sti++) {
//            stbtt_uint16 subtableOffset = ttUSHORT(subTableOffsets + 2 * sti);
//            stbtt_uint8 *table = lookupTable + subtableOffset;
//            stbtt_uint16 posFormat = ttUSHORT(table);
//            stbtt_uint16 coverageOffset = ttUSHORT(table + 2);
//            stbtt_int32 coverageIndex = stbtt__GetCoverageIndex(table + coverageOffset, glyph1);
//            if (coverageIndex == -1) continue;
//
//            switch (posFormat) {
//                case 1: {
//                stbtt_int32 l, r, m;
//                int straw, needle;
//                stbtt_uint16 valueFormat1 = ttUSHORT(table + 4);
//                stbtt_uint16 valueFormat2 = ttUSHORT(table + 6);
//                stbtt_int32 valueRecordPairSizeInBytes = 2;
//                stbtt_uint16 pairSetCount = ttUSHORT(table + 8);
//                stbtt_uint16 pairPosOffset = ttUSHORT(table + 10 + 2 * coverageIndex);
//                stbtt_uint8 *pairValueTable = table + pairPosOffset;
//                stbtt_uint16 pairValueCount = ttUSHORT(pairValueTable);
//                stbtt_uint8 *pairValueArray = pairValueTable + 2;
//                // TODO: Support more formats.
//                STBTT_GPOS_TODO_assert(valueFormat1 == 4);
//                if (valueFormat1 != 4) return 0;
//                STBTT_GPOS_TODO_assert(valueFormat2 == 0);
//                if (valueFormat2 != 0) return 0;
//
//                STBTT_assert(coverageIndex < pairSetCount);
//                STBTT__NOTUSED(pairSetCount);
//
//                needle=glyph2;
//                r=pairValueCount-1;
//                l=0;
//
//                // Binary search.
//                while (l <= r) {
//                    stbtt_uint16 secondGlyph;
//                    stbtt_uint8 *pairValue;
//                    m = (l + r) >> 1;
//                    pairValue = pairValueArray + (2 + valueRecordPairSizeInBytes) * m;
//                    secondGlyph = ttUSHORT(pairValue);
//                    straw = secondGlyph;
//                    if (needle < straw)
//                        r = m - 1;
//                    else if (needle > straw)
//                        l = m + 1;
//                    else {
//                        stbtt_int16 xAdvance = ttSHORT(pairValue + 2);
//                        return xAdvance;
//                    }
//                }
//            } break;
//
//                case 2: {
//                stbtt_uint16 valueFormat1 = ttUSHORT(table + 4);
//                stbtt_uint16 valueFormat2 = ttUSHORT(table + 6);
//
//                stbtt_uint16 classDef1Offset = ttUSHORT(table + 8);
//                stbtt_uint16 classDef2Offset = ttUSHORT(table + 10);
//                int glyph1class = stbtt__GetGlyphClass(table + classDef1Offset, glyph1);
//                int glyph2class = stbtt__GetGlyphClass(table + classDef2Offset, glyph2);
//
//                stbtt_uint16 class1Count = ttUSHORT(table + 12);
//                stbtt_uint16 class2Count = ttUSHORT(table + 14);
//                STBTT_assert(glyph1class < class1Count);
//                STBTT_assert(glyph2class < class2Count);
//
//                // TODO: Support more formats.
//                STBTT_GPOS_TODO_assert(valueFormat1 == 4);
//                if (valueFormat1 != 4) return 0;
//                STBTT_GPOS_TODO_assert(valueFormat2 == 0);
//                if (valueFormat2 != 0) return 0;
//
//                if (glyph1class >= 0 && glyph1class < class1Count && glyph2class >= 0 && glyph2class < class2Count) {
//                    stbtt_uint8 *class1Records = table + 16;
//                    stbtt_uint8 *class2Records = class1Records + 2 * (glyph1class * class2Count);
//                    stbtt_int16 xAdvance = ttSHORT(class2Records + 2 * glyph2class);
//                    return xAdvance;
//                }
//            } break;
//
//                default: {
//                // There are no other cases.
//                STBTT_assert(0);
//                break;
//            } // [DEAR IMGUI] removed ;
//            }
//        }
//        break;
//    } // [DEAR IMGUI] removed ;
//
//        default:
//        return 0; // Unsupported position format
//    }
//}
//
//    return 0;
//}
//

    //////////////////////////////////////////////////////////////////////////////
    //
    // antialiasing software rasterizer
    //

    // [JVM] -> `bitmap rendering.kt`


    //////////////////////////////////////////////////////////////////////////////
    //
    //  Rasterizer

    // [JVM] all useless here
    // stbtt__hheap_chunk
    // stbtt__hheap
    // stbtt__hheap_alloc
    // stbtt__hheap_free
    // stbtt__hheap_cleanup

    class Edge : Comparable<Edge> {
        var x0 = 0f
        var y0 = 0f
        var x1 = 0f
        var y1 = 0f
        var invert = false
        override fun toString() = "($x0, $y0) ($x1, $y1) invert=$invert"
        override fun compareTo(other: Edge): Int = y0.compareTo(other.y0)
    }


    class ActiveEdge {
        var next: ActiveEdge? = null
        var ey = 0f

        // #if STBTT_RASTERIZER_VERSION==1
        var x = 0
        var dx = 0

        //        var direction = 0
        // #elif STBTT_RASTERIZER_VERSION==2
        var fx = 0f
        var fdx = 0f
        var fdy = 0f
        var direction = 0f
        var sy = 0f
    }


    fun sizedTrapezoidArea(height: Float, topWidth: Float, bottomWidth: Float): Float {
        check(topWidth >= 0)
        check(bottomWidth >= 0)
        return (topWidth + bottomWidth) / 2f * height
    }

    fun positionTrapezoidArea(height: Float, tx0: Float, tx1: Float, bx0: Float, bx1: Float) =
            sizedTrapezoidArea(height, tx1 - tx0, bx1 - bx0)

    fun sizedTriangleArea(height: Float, width: Float) = height * width / 2

    /** note: this routine clips fills that extend off the edges... ideally this
     *  wouldn't happen, but it could happen if the truetype glyph bounding boxes
     *  are wrong, or if the user supplies a too-small bitmap */
    fun fillActiveEdges(scanline: PtrByte, len: Int, e_: ActiveEdge, maxWeight: Int) {
        // non-zero winding fill
        var x0 = 0
        var w = 0

        var e: ActiveEdge? = e_
        TODO()
//        while (e != null) {
//            if (w == 0) {
//                // if we're currently at zero, we need to record the edge start point
//                x0 = e.x; w += e.direction.i
//            } else {
//                val x1 = e.x; w += e.direction.i
//                // if we went to zero, we need to draw
//                if (w == 0) {
//                    var i = x0 shr FIXSHIFT
//                    var j = x1 shr FIXSHIFT
//
//                    if (i < len && j >= 0) {
//                        if (i == j) {
//                            // x0,x1 are the same pixel, so compute combined coverage
//                            scanline[i] = (scanline[i] + ((x1 - x0) * maxWeight shr FIXSHIFT)).b
//                        } else {
//                            if (i >= 0) // add antialiasing for x0
//                                scanline[i] = (scanline[i] + (((FIX - (x0 and FIXMASK)) * maxWeight) shr FIXSHIFT)).b
//                            else
//                                i = -1 // clip
//
//                            if (j < len) // add antialiasing for x1
//                                scanline[j] = (scanline[j] + (((x1 and FIXMASK) * maxWeight) shr FIXSHIFT)).b
//                            else
//                                j = len // clip
//
//                            while (++i < j) // fill pixels between x0 and x1
//                                scanline[i] = (scanline[i] + maxWeight).b
//                        }
//                    }
//                }
//            }
//
//            e = e.next
//        }
    }


    //#elif STBTT_RASTERIZER_VERSION == 2

    /** the edge passed in here does not cross the vertical line at x or the vertical line at x+1
     *  (i.e. it has already been clipped to those) */
    fun handleClippedEdge2(scanline: PtrFloat, x: Int, e: ActiveEdge, x0_: Float, y0_: Float, x1_: Float, y1_: Float) {

        var x0 = x0_
        var y0 = y0_
        var x1 = x1_
        var y1 = y1_

        if (y0 == y1) return
        assert(y0 < y1)
        assert(e.sy <= e.ey)
        if (y0 > e.ey) return
        if (y1 < e.sy) return
        if (y0 < e.sy) {
            x0 += (x1 - x0) * (e.sy - y0) / (y1 - y0)
            y0 = e.sy
        }
        if (y1 > e.ey) {
            x1 += (x1 - x0) * (e.ey - y1) / (y1 - y0)
            y1 = e.ey
        }

        assert(when {
                   x0 == x.f -> x1 <= x + 1
                   x0 == x + 1f -> x1 >= x
                   x0 <= x -> x1 <= x
                   x0 >= x + 1 -> x1 >= x + 1
                   else -> x1 >= x && x1 <= x + 1
               })

        if (x0 <= x && x1 <= x)
            scanline[x] += e.direction * (y1 - y0)
        else if (x0 >= x + 1 && x1 >= x + 1)
            Unit
        else {
            assert(x0 >= x && x0 <= x + 1 && x1 >= x && x1 <= x + 1)
            scanline[x] += e.direction * (y1 - y0) * (1 - ((x0 - x) + (x1 - x)) / 2) // coverage = 1 - average x position
        }
    }

    fun fillActiveEdgesNew(scanline: PtrFloat, scanlineFill: PtrFloat, len: Int, e_: ActiveEdge, yTop: Float) {

        val yBottom = yTop + 1
        var e: ActiveEdge? = e_
        while (e != null) {
            // brute force every pixel

            // compute intersection points with top & bottom
            assert(e.ey >= yTop)

            if (e.fdx == 0f) {
                val x0 = e.fx
                if (x0 < len)
                    if (x0 >= 0f) {
                        handleClippedEdge2(scanline, x0.i, e, x0, yTop, x0, yBottom)
                        handleClippedEdge2(scanlineFill - 1, x0.i + 1, e, x0, yTop, x0, yBottom)
                    } else
                        handleClippedEdge2(scanlineFill - 1, 0, e, x0, yTop, x0, yBottom)
            } else {
                var x0 = e.fx
                var dx = e.fdx
                var xb = x0 + dx
                var xTop: Float
                var xBottom: Float
                var sy0: Float
                var sy1: Float
                var dy = e.fdy
                assert(e.sy <= yBottom && e.ey >= yTop)

                // compute endpoints of line segment clipped to this scanline (if the
                // line segment starts on this scanline. x0 is the intersection of the
                // line with y_top, but that may be off the line segment.
                if (e.sy > yTop) {
                    xTop = x0 + dx * (e.sy - yTop)
                    sy0 = e.sy
                } else {
                    xTop = x0
                    sy0 = yTop
                }
                if (e.ey < yBottom) {
                    xBottom = x0 + dx * (e.ey - yTop)
                    sy1 = e.ey
                } else {
                    xBottom = xb
                    sy1 = yBottom
                }
                if (xTop >= 0 && xBottom >= 0 && xTop < len && xBottom < len) {
                    // from here on, we don't have to range check x values

                    if (xTop.i == xBottom.i) {
                        // simple case, only spans one pixel
                        val x = xTop.i
                        val height = (sy1 - sy0) * e.direction
                        assert(x in 0 until len)
                        scanline[x] += positionTrapezoidArea(height, xTop, x + 1f, xBottom, x + 1f)
                        scanlineFill[x] += height // everything right of this pixel is filled
                    } else {
                        // covers 2+ pixels
                        if (xTop > xBottom) {
                            // flip scanline vertically; signed area is the same
                            sy0 = yBottom - (sy0 - yTop)
                            sy1 = yBottom - (sy1 - yTop)
                            var t = sy0
                            sy0 = sy1
                            sy1 = t
                            t = xBottom
                            xBottom = xTop
                            xTop = t
                            dx = -dx
                            dy = -dy
                            t = x0
                            x0 = xb
                            xb = t
                        }
                        check(dy >= 0)
                        check(dx >= 0)

                        val x1 = xTop.i
                        val x2 = xBottom.i
                        // compute intersection with y axis at x1+1
                        var yCrossing = yTop + dy * (x1 + 1 - x0)

                        // compute intersection with y axis at x2
                        var yFinal = yTop + dy * (x2 - x0)

                        //           x1    x_top                            x2    x_bottom
                        //     y_top  +------|-----+------------+------------+--------|---+------------+
                        //            |            |            |            |            |            |
                        //            |            |            |            |            |            |
                        //       sy0  |      Txxxxx|............|............|............|............|
                        // y_crossing |            *xxxxx.......|............|............|............|
                        //            |            |     xxxxx..|............|............|............|
                        //            |            |     /-   xx*xxxx........|............|............|
                        //            |            | dy <       |    xxxxxx..|............|............|
                        //   y_final  |            |     \-     |          xx*xxx.........|............|
                        //       sy1  |            |            |            |   xxxxxB...|............|
                        //            |            |            |            |            |            |
                        //            |            |            |            |            |            |
                        //  y_bottom  +------------+------------+------------+------------+------------+
                        //
                        // goal is to measure the area covered by '.' in each pixel

                        // if x2 is right at the right edge of x1, y_crossing can blow up, github #1057
                        // @TODO: maybe test against sy1 rather than y_bottom?
                        if (yCrossing > yBottom)
                            yCrossing = yBottom

                        val sign = e.direction

                        // area of the rectangle covered from y0..y_crossing
                        var area = sign * (yCrossing - sy0)

                        // area of the triangle (x_top,sy0), (x1+1,sy0), (x1+1,y_crossing)
                        scanline[x1] += sizedTriangleArea(area, x1 + 1 - xTop)

                        // check if final y_crossing is blown up; no test case for this
                        if (yFinal > yBottom) {
                            var denom = (x2 - (x1 + 1))
                            yFinal = yBottom
                            if (denom != 0) { // [DEAR IMGUI] Avoid div by zero (https://github.com/nothings/stb/issues/1316)
                                dy = (yFinal - yCrossing) / denom // if denom=0, y_final = y_crossing, so y_final <= y_bottom
                            }
                        }

                        // in second pixel, area covered by line segment found in first pixel
                        // is always a rectangle 1 wide * the height of that line segment; this
                        // is exactly what the variable 'area' stores. it also gets a contribution
                        // from the line segment within it. the THIRD pixel will get the first
                        // pixel's rectangle contribution, the second pixel's rectangle contribution,
                        // and its own contribution. the 'own contribution' is the same in every pixel except
                        // the leftmost and rightmost, a trapezoid that slides down in each pixel.
                        // the second pixel's contribution to the third pixel will be the
                        // rectangle 1 wide times the height change in the second pixel, which is dy.

                        val step = sign * dy * 1 // dy is dy/dx, change in y for every 1 change in x,
                        // which multiplied by 1-pixel-width is how much pixel area changes for each step in x
                        // so the area advances by 'step' every time


                        for (x in x1 + 1 until x2) {
                            scanline[x] += area + step / 2 // area of trapezoid is 1*step/2
                            area += step
                        }
                        check(abs(area) <= 1.01f) { "accumulated error from area += step unless we round step down" }
                        check(sy1 > yFinal - 0.01f)

                        // area covered in the last pixel is the rectangle from all the pixels to the left,
                        // plus the trapezoid filled by the line segment in this pixel all the way to the right edge
                        scanline[x2] += area + sign * positionTrapezoidArea(sy1 - yFinal, x2.f, x2 + 1f, xBottom, x2 + 1f)

                        // the rest of the line is filled based on the total height of the line segment in this pixel
                        scanlineFill[x2] += sign * (sy1 - sy0)
                    }
                } else {
                    // if edge goes outside of box we're drawing, we require
                    // clipping logic. since this does not match the intended use
                    // of this library, we use a different, very slow brute
                    // force implementation
                    // note though that this does happen some of the time because
                    // x_top and x_bottom can be extrapolated at the top & bottom of
                    // the shape and actually lie outside the bounding box
                    for (x in 0 until len) {
                        // cases:
                        //
                        // there can be up to two intersections with the pixel. any intersection
                        // with left or right edges can be handled by splitting into two (or three)
                        // regions. intersections with top & bottom do not necessitate case-wise logic.
                        //
                        // the old way of doing this found the intersections with the left & right edges,
                        // then used some simple logic to produce up to three segments in sorted order
                        // from top-to-bottom. however, this had a problem: if an x edge was epsilon
                        // across the x border, then the corresponding y position might not be distinct
                        // from the other y segment, and it might ignored as an empty segment. to avoid
                        // that, we need to explicitly produce segments based on x positions.

                        // rename variables to clearly-defined pairs
                        val y0 = yTop
                        val x1 = x.f
                        val x2 = x + 1f
                        val x3 = xb
                        val y3 = yBottom

                        // x = e->x + e->dx * (y-y_top)
                        // (y-y_top) = (x - e->x) / e->dx
                        // y = (x - e->x) / e->dx + y_top
                        val y1 = (x - x0) / dx + yTop
                        val y2 = (x + 1 - x0) / dx + yTop

                        when {
                            x0 < x1 && x3 > x2 -> { // three segments descending down-right
                                handleClippedEdge2(scanline, x, e, x0, y0, x1, y1)
                                handleClippedEdge2(scanline, x, e, x1, y1, x2, y2)
                                handleClippedEdge2(scanline, x, e, x2, y2, x3, y3)
                            }

                            x3 < x1 && x0 > x2 -> { // three segments descending down-left
                                handleClippedEdge2(scanline, x, e, x0, y0, x2, y2)
                                handleClippedEdge2(scanline, x, e, x2, y2, x1, y1)
                                handleClippedEdge2(scanline, x, e, x1, y1, x3, y3)
                            }

                            x0 < x1 && x3 > x1 -> { // two segments across x, down-right
                                handleClippedEdge2(scanline, x, e, x0, y0, x1, y1)
                                handleClippedEdge2(scanline, x, e, x1, y1, x3, y3)
                            }

                            x3 < x1 && x0 > x1 -> { // two segments across x, down-left
                                handleClippedEdge2(scanline, x, e, x0, y0, x1, y1)
                                handleClippedEdge2(scanline, x, e, x1, y1, x3, y3)
                            }

                            x0 < x2 && x3 > x2 -> { // two segments across x+1, down-right
                                handleClippedEdge2(scanline, x, e, x0, y0, x2, y2)
                                handleClippedEdge2(scanline, x, e, x2, y2, x3, y3)
                            }

                            x3 < x2 && x0 > x2 -> { // two segments across x+1, down-left
                                handleClippedEdge2(scanline, x, e, x0, y0, x2, y2)
                                handleClippedEdge2(scanline, x, e, x2, y2, x3, y3)
                            }
                            // one segment
                            else -> handleClippedEdge2(scanline, x, e, x0, y0, x3, y3)
                        }
                    }
                }
            }
            e = e.next
        }
    }


//#else
//#error "Unrecognized value of STBTT_RASTERIZER_VERSION"
//#endif

//typedef struct [JVM] Vec2
//{
//    float x,y;
//} stbtt__point;


//STBTT_DEF void stbtt_FreeBitmap(unsigned char *bitmap, void *userdata)
//{
//    STBTT_free(bitmap, userdata);
//}
//
//STBTT_DEF unsigned char *stbtt_GetGlyphBitmapSubpixel(const stbtt_fontinfo *info, float scale_x, float scale_y, float shift_x, float shift_y, int glyph, int *width, int *height, int *xoff, int *yoff)
//{
//    int ix0,iy0,ix1,iy1;
//    stbtt__bitmap gbm;
//    stbtt_vertex *vertices;
//    int num_verts = stbtt_GetGlyphShape(info, glyph, &vertices);
//
//    if (scale_x == 0) scale_x = scale_y;
//    if (scale_y == 0) {
//        if (scale_x == 0) {
//            STBTT_free(vertices, info->userdata);
//            return NULL;
//        }
//        scale_y = scale_x;
//    }
//
//    stbtt_GetGlyphBitmapBoxSubpixel(info, glyph, scale_x, scale_y, shift_x, shift_y, &ix0,&iy0,&ix1,&iy1);
//
//    // now we get the size
//    gbm.w = (ix1 - ix0);
//    gbm.h = (iy1 - iy0);
//    gbm.pixels = NULL; // in case we error
//
//    if (width ) *width  = gbm.w;
//    if (height) *height = gbm.h;
//    if (xoff  ) *xoff   = ix0;
//    if (yoff  ) *yoff   = iy0;
//
//    if (gbm.w && gbm.h) {
//        gbm.pixels = (unsigned char *) STBTT_malloc(gbm.w * gbm.h, info->userdata);
//        if (gbm.pixels) {
//            gbm.stride = gbm.w;
//
//            stbtt_Rasterize(&gbm, 0.35f, vertices, num_verts, scale_x, scale_y, shift_x, shift_y, ix0, iy0, 1, info->userdata);
//        }
//    }
//    STBTT_free(vertices, info->userdata);
//    return gbm.pixels;
//}
//
//STBTT_DEF unsigned char *stbtt_GetGlyphBitmap(const stbtt_fontinfo *info, float scale_x, float scale_y, int glyph, int *width, int *height, int *xoff, int *yoff)
//{
//    return stbtt_GetGlyphBitmapSubpixel(info, scale_x, scale_y, 0.0f, 0.0f, glyph, width, height, xoff, yoff);
//}


//STBTT_DEF void stbtt_MakeGlyphBitmap(const stbtt_fontinfo *info, unsigned char *output, int out_w, int out_h, int out_stride, float scale_x, float scale_y, int glyph)
//{
//    stbtt_MakeGlyphBitmapSubpixel(info, output, out_w, out_h, out_stride, scale_x, scale_y, 0.0f,0.0f, glyph);
//}
//
//STBTT_DEF unsigned char *stbtt_GetCodepointBitmapSubpixel(const stbtt_fontinfo *info, float scale_x, float scale_y, float shift_x, float shift_y, int codepoint, int *width, int *height, int *xoff, int *yoff)
//{
//    return stbtt_GetGlyphBitmapSubpixel(info, scale_x, scale_y,shift_x,shift_y, stbtt_FindGlyphIndex(info,codepoint), width,height,xoff,yoff);
//}
//
//STBTT_DEF void stbtt_MakeCodepointBitmapSubpixelPrefilter(const stbtt_fontinfo *info, unsigned char *output, int out_w, int out_h, int out_stride, float scale_x, float scale_y, float shift_x, float shift_y, int oversample_x, int oversample_y, float *sub_x, float *sub_y, int codepoint)
//{
//    stbtt_MakeGlyphBitmapSubpixelPrefilter(info, output, out_w, out_h, out_stride, scale_x, scale_y, shift_x, shift_y, oversample_x, oversample_y, sub_x, sub_y, stbtt_FindGlyphIndex(info,codepoint));
//}
//
//STBTT_DEF void stbtt_MakeCodepointBitmapSubpixel(const stbtt_fontinfo *info, unsigned char *output, int out_w, int out_h, int out_stride, float scale_x, float scale_y, float shift_x, float shift_y, int codepoint)
//{
//    stbtt_MakeGlyphBitmapSubpixel(info, output, out_w, out_h, out_stride, scale_x, scale_y, shift_x, shift_y, stbtt_FindGlyphIndex(info,codepoint));
//}
//
//STBTT_DEF unsigned char *stbtt_GetCodepointBitmap(const stbtt_fontinfo *info, float scale_x, float scale_y, int codepoint, int *width, int *height, int *xoff, int *yoff)
//{
//    return stbtt_GetCodepointBitmapSubpixel(info, scale_x, scale_y, 0.0f,0.0f, codepoint, width,height,xoff,yoff);
//}
//
//STBTT_DEF void stbtt_MakeCodepointBitmap(const stbtt_fontinfo *info, unsigned char *output, int out_w, int out_h, int out_stride, float scale_x, float scale_y, int codepoint)
//{
//    stbtt_MakeCodepointBitmapSubpixel(info, output, out_w, out_h, out_stride, scale_x, scale_y, 0.0f,0.0f, codepoint);
//}
//
////////////////////////////////////////////////////////////////////////////////
////
//// bitmap baking
////
//// This is SUPER-CRAPPY packing to keep source code small
//
//static int stbtt_BakeFontBitmap_internal(unsigned char *data, int offset,  // font location (use offset=0 for plain .ttf)
//float pixel_height,                     // height of font in pixels
//unsigned char *pixels, int pw, int ph,  // bitmap to be filled in
//int first_char, int num_chars,          // characters to bake
//stbtt_bakedchar *chardata)
//{
//    float scale;
//    int x,y,bottom_y, i;
//    stbtt_fontinfo f;
//    f.userdata = NULL;
//    if (!stbtt_InitFont(&f, data, offset))
//    return -1;
//    STBTT_memset(pixels, 0, pw*ph); // background of 0 around pixels
//    x=y=1;
//    bottom_y = 1;
//
//    scale = stbtt_ScaleForPixelHeight(&f, pixel_height);
//
//    for (i=0; i < num_chars; ++i) {
//    int advance, lsb, x0,y0,x1,y1,gw,gh;
//    int g = stbtt_FindGlyphIndex(&f, first_char + i);
//    stbtt_GetGlyphHMetrics(&f, g, &advance, &lsb);
//    stbtt_GetGlyphBitmapBox(&f, g, scale,scale, &x0,&y0,&x1,&y1);
//    gw = x1-x0;
//    gh = y1-y0;
//    if (x + gw + 1 >= pw)
//        y = bottom_y, x = 1; // advance to next row
//    if (y + gh + 1 >= ph) // check if it fits vertically AFTER potentially moving to next row
//        return -i;
//    STBTT_assert(x+gw < pw);
//    STBTT_assert(y+gh < ph);
//    stbtt_MakeGlyphBitmap(&f, pixels+x+y*pw, gw,gh,pw, scale,scale, g);
//    chardata[i].x0 = (stbtt_int16) x;
//    chardata[i].y0 = (stbtt_int16) y;
//    chardata[i].x1 = (stbtt_int16) (x + gw);
//    chardata[i].y1 = (stbtt_int16) (y + gh);
//    chardata[i].xadvance = scale * advance;
//    chardata[i].xoff     = (float) x0;
//    chardata[i].yoff     = (float) y0;
//    x = x + gw + 1;
//    if (y+gh+1 > bottom_y)
//        bottom_y = y+gh+1;
//}
//    return bottom_y;
//}
//
//STBTT_DEF void stbtt_GetBakedQuad(const stbtt_bakedchar *chardata, int pw, int ph, int char_index, float *xpos, float *ypos, stbtt_aligned_quad *q, int opengl_fillrule)
//{
//    float d3d_bias = opengl_fillrule ? 0 : -0.5f;
//    float ipw = 1.0f / pw, iph = 1.0f / ph;
//    const stbtt_bakedchar *b = chardata + char_index;
//    int round_x = STBTT_ifloor((*xpos + b->xoff) + 0.5f);
//    int round_y = STBTT_ifloor((*ypos + b->yoff) + 0.5f);
//
//    q->x0 = round_x + d3d_bias;
//    q->y0 = round_y + d3d_bias;
//    q->x1 = round_x + b->x1 - b->x0 + d3d_bias;
//    q->y1 = round_y + b->y1 - b->y0 + d3d_bias;
//
//    q->s0 = b->x0 * ipw;
//    q->t0 = b->y0 * iph;
//    q->s1 = b->x1 * ipw;
//    q->t1 = b->y1 * iph;
//
//    *xpos += b->xadvance;
//}

    // -> rp.kt


    //////////////////////////////////////////////////////////////////////////////
    //
    // bitmap baking
    //
    // This is SUPER-AWESOME (tm Ryan Gordon) packing using stb_rect_pack.h. If
    // stb_rect_pack.h isn't available, it uses the BakeFontBitmap strategy.

    // stbtt_PackBegin
    // stbtt_PackEnd
    // stbtt_PackSetOversampling
    // stbtt_PackSetSkipMissingCodepoints -> PackContext class

    var OVER_MASK = MAX_OVERSAMPLE - 1

    fun hPrefilter(pixels: UByteArray, pointer: Int, w: Int, h: Int, strideInBytes: Int, kernelWidth: UInt) {
        var ptr = pointer
        val buffer = UByteArray(MAX_OVERSAMPLE)
        val safeW = w - kernelWidth.i
        for (j in 0 until h) {
            var i = 0
            var total = 0u
            buffer.fill(0.ub, ptr, ptr + kernelWidth.i)

            // make kernel_width a constant in common cases so compiler can optimize out the divide
            while (i <= safeW) {
                total += pixels[ptr + i] - buffer[i and OVER_MASK]
                buffer[(i + kernelWidth.i) and OVER_MASK] = pixels[ptr + i]
                pixels[ptr + i++] = (total / kernelWidth).ub
            }

            while (i < w) {
                assert(pixels[ptr + i] == 0.ub)
                total -= buffer[i and OVER_MASK]
                pixels[ptr + i++] = (total / kernelWidth).ub
            }

            ptr += strideInBytes
        }
    }

    fun vPrefilter(pixels: UByteArray, pointer: Int, w: Int, h: Int, strideInBytes: Int, kernelWidth: UInt) {
        var ptr = pointer
        val buffer = UByteArray(MAX_OVERSAMPLE)
        val safeH = h - kernelWidth.i
        for (j in 0 until w) {
            var i = 0
            var total = 0u

            // make kernel_width a constant in common cases so compiler can optimize out the divide
            while (i <= safeH) {
                total += pixels[ptr + i * strideInBytes] - buffer[i and OVER_MASK]
                buffer[(i + kernelWidth.i) and OVER_MASK] = pixels[ptr + i * strideInBytes]
                pixels[ptr + i++ * strideInBytes] = (total / kernelWidth).ub
            }

            while (i < h) {
                assert(pixels[ptr + i * strideInBytes] == 0.ub)
                total -= buffer[i and OVER_MASK]
                pixels[ptr + i++ * strideInBytes] = (total / kernelWidth).ub
            }

            ptr++
        }
    }

    infix fun oversampleShift(oversample: Int): Float = when (oversample) {
        0 -> 0f
        else ->
            // The prefilter is a box filter of width "oversample",
            // which shifts phase by (oversample - 1)/2 pixels in
            // oversampled space. We want to shift in the opposite
            // direction to counter this.
            (-(oversample - 1)).f / (2f * oversample.toFloat())
    }

//// rects array must be big enough to accommodate all characters in the given ranges
//STBTT_DEF int stbtt_PackFontRangesGatherRects(stbtt_pack_context *spc, const stbtt_fontinfo *info, stbtt_pack_range *ranges, int num_ranges, stbrp_rect *rects)
//{
//    int i,j,k;
//    int missing_glyph_added = 0;
//
//    k=0;
//    for (i=0; i < num_ranges; ++i) {
//    float fh = ranges[i].font_size;
//    float scale = fh > 0 ? stbtt_ScaleForPixelHeight(info, fh) : stbtt_ScaleForMappingEmToPixels(info, -fh);
//    ranges[i].h_oversample = (unsigned char) spc->h_oversample;
//    ranges[i].v_oversample = (unsigned char) spc->v_oversample;
//    for (j=0; j < ranges[i].num_chars; ++j) {
//    int x0,y0,x1,y1;
//    int codepoint = ranges[i].array_of_unicode_codepoints == NULL ? ranges[i].first_unicode_codepoint_in_range + j : ranges[i].array_of_unicode_codepoints[j];
//    int glyph = stbtt_FindGlyphIndex(info, codepoint);
//    if (glyph == 0 && (spc->skip_missing || missing_glyph_added)) {
//    rects[k].w = rects[k].h = 0;
//} else {
//    stbtt_GetGlyphBitmapBoxSubpixel(info,glyph,
//            scale * spc->h_oversample,
//    scale * spc->v_oversample,
//    0,0,
//    &x0,&y0,&x1,&y1);
//    rects[k].w = (stbrp_coord) (x1-x0 + spc->padding + spc->h_oversample-1);
//    rects[k].h = (stbrp_coord) (y1-y0 + spc->padding + spc->v_oversample-1);
//    if (glyph == 0)
//        missing_glyph_added = 1;
//}
//    ++k;
//}
//}
//
//    return k;
//}
//
//STBTT_DEF void stbtt_MakeGlyphBitmapSubpixelPrefilter(const stbtt_fontinfo *info, unsigned char *output, int out_w, int out_h, int out_stride, float scale_x, float scale_y, float shift_x, float shift_y, int prefilter_x, int prefilter_y, float *sub_x, float *sub_y, int glyph)
//{
//    stbtt_MakeGlyphBitmapSubpixel(info,
//            output,
//            out_w - (prefilter_x - 1),
//            out_h - (prefilter_y - 1),
//            out_stride,
//            scale_x,
//            scale_y,
//            shift_x,
//            shift_y,
//            glyph);
//
//    if (prefilter_x > 1)
//        stbtt__h_prefilter(output, out_w, out_h, out_stride, prefilter_x);
//
//    if (prefilter_y > 1)
//        stbtt__v_prefilter(output, out_w, out_h, out_stride, prefilter_y);
//
//    *sub_x = stbtt__oversample_shift(prefilter_x);
//    *sub_y = stbtt__oversample_shift(prefilter_y);
//}


//STBTT_DEF void stbtt_PackFontRangesPackRects(stbtt_pack_context *spc, stbrp_rect *rects, int num_rects)
//{
//    stbrp_pack_rects((stbrp_context *) spc->pack_info, rects, num_rects);
//}
//
//STBTT_DEF int stbtt_PackFontRanges(stbtt_pack_context *spc, const unsigned char *fontdata, int font_index, stbtt_pack_range *ranges, int num_ranges)
//{
//    stbtt_fontinfo info;
//    int i,j,n, return_value = 1; // [DEAR IMGUI] removed = 1
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
//
//STBTT_DEF int stbtt_PackFontRange(stbtt_pack_context *spc, const unsigned char *fontdata, int font_index, float font_size,
//int first_unicode_codepoint_in_range, int num_chars_in_range, stbtt_packedchar *chardata_for_range)
//{
//    stbtt_pack_range range;
//    range.first_unicode_codepoint_in_range = first_unicode_codepoint_in_range;
//    range.array_of_unicode_codepoints = NULL;
//    range.num_chars                   = num_chars_in_range;
//    range.chardata_for_range          = chardata_for_range;
//    range.font_size                   = font_size;
//    return stbtt_PackFontRanges(spc, fontdata, font_index, &range, 1);
//}
//
//STBTT_DEF void stbtt_GetScaledFontVMetrics(const unsigned char *fontdata, int index, float size, float *ascent, float *descent, float *lineGap)
//{
//    int i_ascent, i_descent, i_lineGap;
//    float scale;
//    stbtt_fontinfo info;
//    stbtt_InitFont(&info, fontdata, stbtt_GetFontOffsetForIndex(fontdata, index));
//    scale = size > 0 ? stbtt_ScaleForPixelHeight(&info, size) : stbtt_ScaleForMappingEmToPixels(&info, -size);
//    stbtt_GetFontVMetrics(&info, &i_ascent, &i_descent, &i_lineGap);
//    *ascent  = (float) i_ascent  * scale;
//    *descent = (float) i_descent * scale;
//    *lineGap = (float) i_lineGap * scale;
//}


////////////////////////////////////////////////////////////////////////////////
////
//// sdf computation
////
//
//#define STBTT_min(a,b)  ((a) < (b) ? (a) : (b))
//#define STBTT_max(a,b)  ((a) < (b) ? (b) : (a))
//
//static int stbtt__ray_intersect_bezier(float orig[2], float ray[2], float q0[2], float q1[2], float q2[2], float hits[2][2])
//{
//    float q0perp = q0[1]*ray[0] - q0[0]*ray[1];
//    float q1perp = q1[1]*ray[0] - q1[0]*ray[1];
//    float q2perp = q2[1]*ray[0] - q2[0]*ray[1];
//    float roperp = orig[1]*ray[0] - orig[0]*ray[1];
//
//    float a = q0perp - 2*q1perp + q2perp;
//    float b = q1perp - q0perp;
//    float c = q0perp - roperp;
//
//    float s0 = 0., s1 = 0.;
//    int num_s = 0;
//
//    if (a != 0.0) {
//        float discr = b*b - a*c;
//        if (discr > 0.0) {
//            float rcpna = -1 / a;
//            float d = (float) STBTT_sqrt(discr);
//            s0 = (b+d) * rcpna;
//            s1 = (b-d) * rcpna;
//            if (s0 >= 0.0 && s0 <= 1.0)
//                num_s = 1;
//            if (d > 0.0 && s1 >= 0.0 && s1 <= 1.0) {
//                if (num_s == 0) s0 = s1;
//                ++num_s;
//            }
//        }
//    } else {
//        // 2*b*s + c = 0
//        // s = -c / (2*b)
//        s0 = c / (-2 * b);
//        if (s0 >= 0.0 && s0 <= 1.0)
//            num_s = 1;
//    }
//
//    if (num_s == 0)
//        return 0;
//    else {
//        float rcp_len2 = 1 / (ray[0]*ray[0] + ray[1]*ray[1]);
//        float rayn_x = ray[0] * rcp_len2, rayn_y = ray[1] * rcp_len2;
//
//        float q0d =   q0[0]*rayn_x +   q0[1]*rayn_y;
//        float q1d =   q1[0]*rayn_x +   q1[1]*rayn_y;
//        float q2d =   q2[0]*rayn_x +   q2[1]*rayn_y;
//        float rod = orig[0]*rayn_x + orig[1]*rayn_y;
//
//        float q10d = q1d - q0d;
//        float q20d = q2d - q0d;
//        float q0rd = q0d - rod;
//
//        hits[0][0] = q0rd + s0*(2.0f - 2.0f*s0)*q10d + s0*s0*q20d;
//        hits[0][1] = a*s0+b;
//
//        if (num_s > 1) {
//            hits[1][0] = q0rd + s1*(2.0f - 2.0f*s1)*q10d + s1*s1*q20d;
//            hits[1][1] = a*s1+b;
//            return 2;
//        } else {
//            return 1;
//        }
//    }
//}
//
//static int equal(float *a, float *b)
//{
//    return (a[0] == b[0] && a[1] == b[1]);
//}
//
//static int stbtt__compute_crossings_x(float x, float y, int nverts, stbtt_vertex *verts)
//{
//    int i;
//    float orig[2], ray[2] = { 1, 0 };
//    float y_frac;
//    int winding = 0;
//
//    // make sure y never passes through a vertex of the shape
//    y_frac = (float) STBTT_fmod(y, 1.0f);
//    if (y_frac < 0.01f)
//        y += 0.01f;
//    else if (y_frac > 0.99f)
//        y -= 0.01f;
//
//    orig[0] = x;
//    orig[1] = y;
//
//    // test a ray from (-infinity,y) to (x,y)
//    for (i=0; i < nverts; ++i) {
//    if (verts[i].type == STBTT_vline) {
//        int x0 = (int) verts[i-1].x, y0 = (int) verts[i-1].y;
//        int x1 = (int) verts[i  ].x, y1 = (int) verts[i  ].y;
//        if (y > STBTT_min(y0,y1) && y < STBTT_max(y0,y1) && x > STBTT_min(x0,x1)) {
//            float x_inter = (y - y0) / (y1 - y0) * (x1-x0) + x0;
//            if (x_inter < x)
//                winding += (y0 < y1) ? 1 : -1;
//        }
//    }
//    if (verts[i].type == STBTT_vcurve) {
//        int x0 = (int) verts[i-1].x , y0 = (int) verts[i-1].y ;
//        int x1 = (int) verts[i  ].cx, y1 = (int) verts[i  ].cy;
//        int x2 = (int) verts[i  ].x , y2 = (int) verts[i  ].y ;
//        int ax = STBTT_min(x0,STBTT_min(x1,x2)), ay = STBTT_min(y0,STBTT_min(y1,y2));
//        int by = STBTT_max(y0,STBTT_max(y1,y2));
//        if (y > ay && y < by && x > ax) {
//            float q0[2],q1[2],q2[2];
//            float hits[2][2];
//            q0[0] = (float)x0;
//            q0[1] = (float)y0;
//            q1[0] = (float)x1;
//            q1[1] = (float)y1;
//            q2[0] = (float)x2;
//            q2[1] = (float)y2;
//            if (equal(q0,q1) || equal(q1,q2)) {
//                x0 = (int)verts[i-1].x;
//                y0 = (int)verts[i-1].y;
//                x1 = (int)verts[i  ].x;
//                y1 = (int)verts[i  ].y;
//                if (y > STBTT_min(y0,y1) && y < STBTT_max(y0,y1) && x > STBTT_min(x0,x1)) {
//                    float x_inter = (y - y0) / (y1 - y0) * (x1-x0) + x0;
//                    if (x_inter < x)
//                        winding += (y0 < y1) ? 1 : -1;
//                }
//            } else {
//                int num_hits = stbtt__ray_intersect_bezier(orig, ray, q0, q1, q2, hits);
//                if (num_hits >= 1)
//                    if (hits[0][0] < 0)
//                        winding += (hits[0][1] < 0 ? -1 : 1);
//                if (num_hits >= 2)
//                    if (hits[1][0] < 0)
//                        winding += (hits[1][1] < 0 ? -1 : 1);
//            }
//        }
//    }
//}
//    return winding;
//}
//
//static float stbtt__cuberoot( float x )
//{
//    if (x<0)
//        return -(float) STBTT_pow(-x,1.0f/3.0f);
//    else
//    return  (float) STBTT_pow( x,1.0f/3.0f);
//}
//
//// x^3 + a*x^2 + b*x + c = 0
//static int stbtt__solve_cubic(float a, float b, float c, float* r)
//{
//    float s = -a / 3;
//    float p = b - a*a / 3;
//    float q = a * (2*a*a - 9*b) / 27 + c;
//    float p3 = p*p*p;
//    float d = q*q + 4*p3 / 27;
//    if (d >= 0) {
//        float z = (float) STBTT_sqrt(d);
//        float u = (-q + z) / 2;
//        float v = (-q - z) / 2;
//        u = stbtt__cuberoot(u);
//        v = stbtt__cuberoot(v);
//        r[0] = s + u + v;
//        return 1;
//    } else {
//        float u = (float) STBTT_sqrt(-p/3);
//        float v = (float) STBTT_acos(-STBTT_sqrt(-27/p3) * q / 2) / 3; // p3 must be negative, since d is negative
//        float m = (float) STBTT_cos(v);
//        float n = (float) STBTT_cos(v-3.141592/2)*1.732050808f;
//        r[0] = s + u * 2 * m;
//        r[1] = s - u * (m + n);
//        r[2] = s - u * (m - n);
//
//        //STBTT_assert( STBTT_fabs(((r[0]+a)*r[0]+b)*r[0]+c) < 0.05f);  // these asserts may not be safe at all scales, though they're in bezier t parameter units so maybe?
//        //STBTT_assert( STBTT_fabs(((r[1]+a)*r[1]+b)*r[1]+c) < 0.05f);
//        //STBTT_assert( STBTT_fabs(((r[2]+a)*r[2]+b)*r[2]+c) < 0.05f);
//        return 3;
//    }
//}
//
//STBTT_DEF unsigned char * stbtt_GetGlyphSDF(const stbtt_fontinfo *info, float scale, int glyph, int padding, unsigned char onedge_value, float pixel_dist_scale, int *width, int *height, int *xoff, int *yoff)
//{
//    float scale_x = scale, scale_y = scale;
//    int ix0,iy0,ix1,iy1;
//    int w,h;
//    unsigned char *data;
//
//    if (scale == 0) return NULL;
//
//    stbtt_GetGlyphBitmapBoxSubpixel(info, glyph, scale, scale, 0.0f,0.0f, &ix0,&iy0,&ix1,&iy1);
//
//    // if empty, return NULL
//    if (ix0 == ix1 || iy0 == iy1)
//        return NULL;
//
//    ix0 -= padding;
//    iy0 -= padding;
//    ix1 += padding;
//    iy1 += padding;
//
//    w = (ix1 - ix0);
//    h = (iy1 - iy0);
//
//    if (width ) *width  = w;
//    if (height) *height = h;
//    if (xoff  ) *xoff   = ix0;
//    if (yoff  ) *yoff   = iy0;
//
//    // invert for y-downwards bitmaps
//    scale_y = -scale_y;
//
//    {
//        int x,y,i,j;
//        float *precompute;
//        stbtt_vertex *verts;
//        int num_verts = stbtt_GetGlyphShape(info, glyph, &verts);
//        data = (unsigned char *) STBTT_malloc(w * h, info->userdata);
//        precompute = (float *) STBTT_malloc(num_verts * sizeof(float), info->userdata);
//
//        for (i=0,j=num_verts-1; i < num_verts; j=i++) {
//        if (verts[i].type == STBTT_vline) {
//            float x0 = verts[i].x*scale_x, y0 = verts[i].y*scale_y;
//            float x1 = verts[j].x*scale_x, y1 = verts[j].y*scale_y;
//            float dist = (float) STBTT_sqrt((x1-x0)*(x1-x0) + (y1-y0)*(y1-y0));
//            precompute[i] = (dist == 0) ? 0.0f : 1.0f / dist;
//        } else if (verts[i].type == STBTT_vcurve) {
//            float x2 = verts[j].x *scale_x, y2 = verts[j].y *scale_y;
//            float x1 = verts[i].cx*scale_x, y1 = verts[i].cy*scale_y;
//            float x0 = verts[i].x *scale_x, y0 = verts[i].y *scale_y;
//            float bx = x0 - 2*x1 + x2, by = y0 - 2*y1 + y2;
//            float len2 = bx*bx + by*by;
//            if (len2 != 0.0f)
//                precompute[i] = 1.0f / (bx*bx + by*by);
//            else
//                precompute[i] = 0.0f;
//        } else
//            precompute[i] = 0.0f;
//    }
//
//        for (y=iy0; y < iy1; ++y) {
//        for (x=ix0; x < ix1; ++x) {
//        float val;
//        float min_dist = 999999.0f;
//        float sx = (float) x + 0.5f;
//        float sy = (float) y + 0.5f;
//        float x_gspace = (sx / scale_x);
//        float y_gspace = (sy / scale_y);
//
//        int winding = stbtt__compute_crossings_x(x_gspace, y_gspace, num_verts, verts); // @OPTIMIZE: this could just be a rasterization, but needs to be line vs. non-tesselated curves so a new path
//
//        for (i=0; i < num_verts; ++i) {
//        float x0 = verts[i].x*scale_x, y0 = verts[i].y*scale_y;
//
//        if (verts[i].type == STBTT_vline && precompute[i] != 0.0f) {
//            float x1 = verts[i-1].x*scale_x, y1 = verts[i-1].y*scale_y;
//
//    float dist,dist2 = (x0-sx)*(x0-sx) + (y0-sy)*(y0-sy);
//    if (dist2 < min_dist*min_dist)
//    min_dist = (float) STBTT_sqrt(dist2);
//
//            // coarse culling against bbox
//            //if (sx > STBTT_min(x0,x1)-min_dist && sx < STBTT_max(x0,x1)+min_dist &&
//            //    sy > STBTT_min(y0,y1)-min_dist && sy < STBTT_max(y0,y1)+min_dist)
//            dist = (float) STBTT_fabs((x1-x0)*(y0-sy) - (y1-y0)*(x0-sx)) * precompute[i];
//            STBTT_assert(i != 0);
//            if (dist < min_dist) {
//                // check position along line
//                // x' = x0 + t*(x1-x0), y' = y0 + t*(y1-y0)
//                // minimize (x'-sx)*(x'-sx)+(y'-sy)*(y'-sy)
//                float dx = x1-x0, dy = y1-y0;
//                float px = x0-sx, py = y0-sy;
//                // minimize (px+t*dx)^2 + (py+t*dy)^2 = px*px + 2*px*dx*t + t^2*dx*dx + py*py + 2*py*dy*t + t^2*dy*dy
//                // derivative: 2*px*dx + 2*py*dy + (2*dx*dx+2*dy*dy)*t, set to 0 and solve
//                float t = -(px*dx + py*dy) / (dx*dx + dy*dy);
//                if (t >= 0.0f && t <= 1.0f)
//                    min_dist = dist;
//            }
//        } else if (verts[i].type == STBTT_vcurve) {
//            float x2 = verts[i-1].x *scale_x, y2 = verts[i-1].y *scale_y;
//            float x1 = verts[i  ].cx*scale_x, y1 = verts[i  ].cy*scale_y;
//            float box_x0 = STBTT_min(STBTT_min(x0,x1),x2);
//            float box_y0 = STBTT_min(STBTT_min(y0,y1),y2);
//            float box_x1 = STBTT_max(STBTT_max(x0,x1),x2);
//            float box_y1 = STBTT_max(STBTT_max(y0,y1),y2);
//            // coarse culling against bbox to avoid computing cubic unnecessarily
//            if (sx > box_x0-min_dist && sx < box_x1+min_dist && sy > box_y0-min_dist && sy < box_y1+min_dist) {
//                int num=0;
//                float ax = x1-x0, ay = y1-y0;
//                float bx = x0 - 2*x1 + x2, by = y0 - 2*y1 + y2;
//                float mx = x0 - sx, my = y0 - sy;
//    float res[3] = {0.f,0.f,0.f};
//    float px,py,t,it,dist2;
//                float a_inv = precompute[i];
//                if (a_inv == 0.0) { // if a_inv is 0, it's 2nd degree so use quadratic formula
//                    float a = 3*(ax*bx + ay*by);
//                    float b = 2*(ax*ax + ay*ay) + (mx*bx+my*by);
//                    float c = mx*ax+my*ay;
//                    if (a == 0.0) { // if a is 0, it's linear
//                        if (b != 0.0) {
//                            res[num++] = -c/b;
//                        }
//                    } else {
//                        float discriminant = b*b - 4*a*c;
//                        if (discriminant < 0)
//                            num = 0;
//                        else {
//                            float root = (float) STBTT_sqrt(discriminant);
//                            res[0] = (-b - root)/(2*a);
//                            res[1] = (-b + root)/(2*a);
//                            num = 2; // don't bother distinguishing 1-solution case, as code below will still work
//                        }
//                    }
//                } else {
//                    float b = 3*(ax*bx + ay*by) * a_inv; // could precompute this as it doesn't depend on sample point
//                    float c = (2*(ax*ax + ay*ay) + (mx*bx+my*by)) * a_inv;
//                    float d = (mx*ax+my*ay) * a_inv;
//                    num = stbtt__solve_cubic(b, c, d, res);
//                }
//    dist2 = (x0-sx)*(x0-sx) + (y0-sy)*(y0-sy);
//    if (dist2 < min_dist*min_dist)
//    min_dist = (float) STBTT_sqrt(dist2);
//
//                if (num >= 1 && res[0] >= 0.0f && res[0] <= 1.0f) {
//                    t = res[0], it = 1.0f - t;
//                    px = it*it*x0 + 2*t*it*x1 + t*t*x2;
//                    py = it*it*y0 + 2*t*it*y1 + t*t*y2;
//                    dist2 = (px-sx)*(px-sx) + (py-sy)*(py-sy);
//                    if (dist2 < min_dist * min_dist)
//                        min_dist = (float) STBTT_sqrt(dist2);
//                }
//                if (num >= 2 && res[1] >= 0.0f && res[1] <= 1.0f) {
//                    t = res[1], it = 1.0f - t;
//                    px = it*it*x0 + 2*t*it*x1 + t*t*x2;
//                    py = it*it*y0 + 2*t*it*y1 + t*t*y2;
//                    dist2 = (px-sx)*(px-sx) + (py-sy)*(py-sy);
//                    if (dist2 < min_dist * min_dist)
//                        min_dist = (float) STBTT_sqrt(dist2);
//                }
//                if (num >= 3 && res[2] >= 0.0f && res[2] <= 1.0f) {
//                    t = res[2], it = 1.0f - t;
//                    px = it*it*x0 + 2*t*it*x1 + t*t*x2;
//                    py = it*it*y0 + 2*t*it*y1 + t*t*y2;
//                    dist2 = (px-sx)*(px-sx) + (py-sy)*(py-sy);
//                    if (dist2 < min_dist * min_dist)
//                        min_dist = (float) STBTT_sqrt(dist2);
//                }
//            }
//        }
//    }
//        if (winding == 0)
//            min_dist = -min_dist;  // if outside the shape, value is negative
//        val = onedge_value + pixel_dist_scale * min_dist;
//        if (val < 0)
//        val = 0;
//        else if (val > 255)
//        val = 255;
//        data[(y-iy0)*w+(x-ix0)] = (unsigned char) val;
//    }
//    }
//        STBTT_free(precompute, info->userdata);
//        STBTT_free(verts, info->userdata);
//    }
//    return data;
//}
//
//STBTT_DEF unsigned char * stbtt_GetCodepointSDF(const stbtt_fontinfo *info, float scale, int codepoint, int padding, unsigned char onedge_value, float pixel_dist_scale, int *width, int *height, int *xoff, int *yoff)
//{
//    return stbtt_GetGlyphSDF(info, scale, stbtt_FindGlyphIndex(info, codepoint), padding, onedge_value, pixel_dist_scale, width, height, xoff, yoff);
//}
//
//STBTT_DEF void stbtt_FreeSDF(unsigned char *bitmap, void *userdata)
//{
//    STBTT_free(bitmap, userdata);
//}
//
////////////////////////////////////////////////////////////////////////////////
////
//// font name matching -- recommended not to use this
////
//
//// check if a utf8 string contains a prefix which is the utf16 string; if so return length of matching utf8 string
//static stbtt_int32 stbtt__CompareUTF8toUTF16_bigendian_prefix(stbtt_uint8 *s1, stbtt_int32 len1, stbtt_uint8 *s2, stbtt_int32 len2)
//{
//    stbtt_int32 i=0;
//
//    // convert utf16 to utf8 and compare the results while converting
//    while (len2) {
//        stbtt_uint16 ch = s2[0]*256 + s2[1];
//        if (ch < 0x80) {
//            if (i >= len1) return -1;
//            if (s1[i++] != ch) return -1;
//        } else if (ch < 0x800) {
//            if (i+1 >= len1) return -1;
//            if (s1[i++] != 0xc0 + (ch >> 6)) return -1;
//            if (s1[i++] != 0x80 + (ch & 0x3f)) return -1;
//        } else if (ch >= 0xd800 && ch < 0xdc00) {
//            stbtt_uint32 c;
//            stbtt_uint16 ch2 = s2[2]*256 + s2[3];
//            if (i+3 >= len1) return -1;
//            c = ((ch - 0xd800) << 10) + (ch2 - 0xdc00) + 0x10000;
//            if (s1[i++] != 0xf0 + (c >> 18)) return -1;
//            if (s1[i++] != 0x80 + ((c >> 12) & 0x3f)) return -1;
//            if (s1[i++] != 0x80 + ((c >>  6) & 0x3f)) return -1;
//            if (s1[i++] != 0x80 + ((c      ) & 0x3f)) return -1;
//            s2 += 2; // plus another 2 below
//            len2 -= 2;
//        } else if (ch >= 0xdc00 && ch < 0xe000) {
//            return -1;
//        } else {
//            if (i+2 >= len1) return -1;
//            if (s1[i++] != 0xe0 + (ch >> 12)) return -1;
//            if (s1[i++] != 0x80 + ((ch >> 6) & 0x3f)) return -1;
//            if (s1[i++] != 0x80 + ((ch     ) & 0x3f)) return -1;
//        }
//        s2 += 2;
//        len2 -= 2;
//    }
//    return i;
//}
//
//static int stbtt_CompareUTF8toUTF16_bigendian_internal(char *s1, int len1, char *s2, int len2)
//{
//    return len1 == stbtt__CompareUTF8toUTF16_bigendian_prefix((stbtt_uint8*) s1, len1, (stbtt_uint8*) s2, len2);
//}
//
//// returns results in whatever encoding you request... but note that 2-byte encodings
//// will be BIG-ENDIAN... use stbtt_CompareUTF8toUTF16_bigendian() to compare
//STBTT_DEF const char *stbtt_GetFontNameString(const stbtt_fontinfo *font, int *length, int platformID, int encodingID, int languageID, int nameID)
//{
//    stbtt_int32 i,count,stringOffset;
//    stbtt_uint8 *fc = font->data;
//    stbtt_uint32 offset = font->fontstart;
//    stbtt_uint32 nm = stbtt__find_table(fc, offset, "name");
//    if (!nm) return NULL;
//
//    count = ttUSHORT(fc+nm+2);
//    stringOffset = nm + ttUSHORT(fc+nm+4);
//    for (i=0; i < count; ++i) {
//    stbtt_uint32 loc = nm + 6 + 12 * i;
//    if (platformID == ttUSHORT(fc+loc+0) && encodingID == ttUSHORT(fc+loc+2)
//            && languageID == ttUSHORT(fc+loc+4) && nameID == ttUSHORT(fc+loc+6)) {
//        *length = ttUSHORT(fc+loc+8);
//        return (const char *) (fc+stringOffset+ttUSHORT(fc+loc+10));
//    }
//}
//    return NULL;
//}
//
//static int stbtt__matchpair(stbtt_uint8 *fc, stbtt_uint32 nm, stbtt_uint8 *name, stbtt_int32 nlen, stbtt_int32 target_id, stbtt_int32 next_id)
//{
//    stbtt_int32 i;
//    stbtt_int32 count = ttUSHORT(fc+nm+2);
//    stbtt_int32 stringOffset = nm + ttUSHORT(fc+nm+4);
//
//    for (i=0; i < count; ++i) {
//    stbtt_uint32 loc = nm + 6 + 12 * i;
//    stbtt_int32 id = ttUSHORT(fc+loc+6);
//    if (id == target_id) {
//        // find the encoding
//        stbtt_int32 platform = ttUSHORT(fc+loc+0), encoding = ttUSHORT(fc+loc+2), language = ttUSHORT(fc+loc+4);
//
//        // is this a Unicode encoding?
//        if (platform == 0 || (platform == 3 && encoding == 1) || (platform == 3 && encoding == 10)) {
//            stbtt_int32 slen = ttUSHORT(fc+loc+8);
//            stbtt_int32 off = ttUSHORT(fc+loc+10);
//
//            // check if there's a prefix match
//            stbtt_int32 matchlen = stbtt__CompareUTF8toUTF16_bigendian_prefix(name, nlen, fc+stringOffset+off,slen);
//            if (matchlen >= 0) {
//                // check for target_id+1 immediately following, with same encoding & language
//                if (i+1 < count && ttUSHORT(fc+loc+12+6) == next_id && ttUSHORT(fc+loc+12) == platform && ttUSHORT(fc+loc+12+2) == encoding && ttUSHORT(fc+loc+12+4) == language) {
//                    slen = ttUSHORT(fc+loc+12+8);
//                    off = ttUSHORT(fc+loc+12+10);
//                    if (slen == 0) {
//                        if (matchlen == nlen)
//                            return 1;
//                    } else if (matchlen < nlen && name[matchlen] == ' ') {
//                        ++matchlen;
//                        if (stbtt_CompareUTF8toUTF16_bigendian_internal((char*) (name+matchlen), nlen-matchlen, (char*)(fc+stringOffset+off),slen))
//                            return 1;
//                    }
//                } else {
//                    // if nothing immediately following
//                    if (matchlen == nlen)
//                        return 1;
//                }
//            }
//        }
//
//        // @TODO handle other encodings
//    }
//}
//    return 0;
//}
//
//static int stbtt__matches(stbtt_uint8 *fc, stbtt_uint32 offset, stbtt_uint8 *name, stbtt_int32 flags)
//{
//    stbtt_int32 nlen = (stbtt_int32) STBTT_strlen((char *) name);
//    stbtt_uint32 nm,hd;
//    if (!stbtt__isfont(fc+offset)) return 0;
//
//    // check italics/bold/underline flags in macStyle...
//    if (flags) {
//        hd = stbtt__find_table(fc, offset, "head");
//        if ((ttUSHORT(fc+hd+44) & 7) != (flags & 7)) return 0;
//    }
//
//    nm = stbtt__find_table(fc, offset, "name");
//    if (!nm) return 0;
//
//    if (flags) {
//        // if we checked the macStyle flags, then just check the family and ignore the subfamily
//        if (stbtt__matchpair(fc, nm, name, nlen, 16, -1))  return 1;
//        if (stbtt__matchpair(fc, nm, name, nlen,  1, -1))  return 1;
//        if (stbtt__matchpair(fc, nm, name, nlen,  3, -1))  return 1;
//    } else {
//        if (stbtt__matchpair(fc, nm, name, nlen, 16, 17))  return 1;
//        if (stbtt__matchpair(fc, nm, name, nlen,  1,  2))  return 1;
//        if (stbtt__matchpair(fc, nm, name, nlen,  3, -1))  return 1;
//    }
//
//    return 0;
//}
//
//static int stbtt_FindMatchingFont_internal(unsigned char *font_collection, char *name_utf8, stbtt_int32 flags)
//{
//    stbtt_int32 i;
//    for (i=0;;++i) {
//    stbtt_int32 off = stbtt_GetFontOffsetForIndex(font_collection, i);
//    if (off < 0) return off;
//    if (stbtt__matches((stbtt_uint8 *) font_collection, off, (stbtt_uint8*) name_utf8, flags))
//        return off;
//}
//}
//
//#if defined(__GNUC__) || defined(__clang__)
//#pragma GCC diagnostic push
//#pragma GCC diagnostic ignored "-Wcast-qual"
//#endif
//
//STBTT_DEF int stbtt_BakeFontBitmap(const unsigned char *data, int offset,
//float pixel_height, unsigned char *pixels, int pw, int ph,
//int first_char, int num_chars, stbtt_bakedchar *chardata)
//{
//    return stbtt_BakeFontBitmap_internal((unsigned char *) data, offset, pixel_height, pixels, pw, ph, first_char, num_chars, chardata);
//}
//
//STBTT_DEF int stbtt_GetFontOffsetForIndex(const unsigned char *data, int index)
//{
//    return stbtt_GetFontOffsetForIndex_internal((unsigned char *) data, index);
//}
//
//STBTT_DEF int stbtt_GetNumberOfFonts(const unsigned char *data)
//{
//    return stbtt_GetNumberOfFonts_internal((unsigned char *) data);
//}
//
//STBTT_DEF int stbtt_InitFont(stbtt_fontinfo *info, const unsigned char *data, int offset)
//{
//    return stbtt_InitFont_internal(info, (unsigned char *) data, offset);
//}
//
//STBTT_DEF int stbtt_FindMatchingFont(const unsigned char *fontdata, const char *name, int flags)
//{
//    return stbtt_FindMatchingFont_internal((unsigned char *) fontdata, (char *) name, flags);
//}
//
//STBTT_DEF int stbtt_CompareUTF8toUTF16_bigendian(const char *s1, int len1, const char *s2, int len2)
//{
//    return stbtt_CompareUTF8toUTF16_bigendian_internal((char *) s1, len1, (char *) s2, len2);
//}
//
//#if defined(__GNUC__) || defined(__clang__)
//#pragma GCC diagnostic pop
//#endif
//
//#endif // STB_TRUETYPE_IMPLEMENTATION

}
// FULL VERSION HISTORY
//
//   1.19 (2018-02-11) OpenType GPOS kerning (horizontal only), STBTT_fmod
//   1.18 (2018-01-29) add missing function
//   1.17 (2017-07-23) make more arguments const; doc fix
//   1.16 (2017-07-12) SDF support
//   1.15 (2017-03-03) make more arguments const
//   1.14 (2017-01-16) num-fonts-in-TTC function
//   1.13 (2017-01-02) support OpenType fonts, certain Apple fonts
//   1.12 (2016-10-25) suppress warnings about casting away const with -Wcast-qual
//   1.11 (2016-04-02) fix unused-variable warning
//   1.10 (2016-04-02) allow user-defined fabs() replacement
//                     fix memory leak if fontsize=0.0
//                     fix warning from duplicate typedef
//   1.09 (2016-01-16) warning fix; avoid crash on outofmem; use alloc userdata for PackFontRanges
//   1.08 (2015-09-13) document stbtt_Rasterize(); fixes for vertical & horizontal edges
//   1.07 (2015-08-01) allow PackFontRanges to accept arrays of sparse codepoints;
//                     allow PackFontRanges to pack and render in separate phases;
//                     fix stbtt_GetFontOFfsetForIndex (never worked for non-0 input?);
//                     fixed an assert() bug in the new rasterizer
//                     replace assert() with STBTT_assert() in new rasterizer
//   1.06 (2015-07-14) performance improvements (~35% faster on x86 and x64 on test machine)
//                     also more precise AA rasterizer, except if shapes overlap
//                     remove need for STBTT_sort
//   1.05 (2015-04-15) fix misplaced definitions for STBTT_STATIC
//   1.04 (2015-04-15) typo in example
//   1.03 (2015-04-12) STBTT_STATIC, fix memory leak in new packing, various fixes
//   1.02 (2014-12-10) fix various warnings & compile issues w/ stb_rect_pack, C++
//   1.01 (2014-12-08) fix subpixel position when oversampling to exactly match
//                        non-oversampled; STBTT_POINT_SIZE for packed case only
//   1.00 (2014-12-06) add new PackBegin etc. API, w/ support for oversampling
//   0.99 (2014-09-18) fix multiple bugs with subpixel rendering (ryg)
//   0.9  (2014-08-07) support certain mac/iOS fonts without an MS platformID
//   0.8b (2014-07-07) fix a warning
//   0.8  (2014-05-25) fix a few more warnings
//   0.7  (2013-09-25) bugfix: subpixel glyph bug fixed in 0.5 had come back
//   0.6c (2012-07-24) improve documentation
//   0.6b (2012-07-20) fix a few more warnings
//   0.6  (2012-07-17) fix warnings; added stbtt_ScaleForMappingEmToPixels,
//                        stbtt_GetFontBoundingBox, stbtt_IsGlyphEmpty
//   0.5  (2011-12-09) bugfixes:
//                        subpixel glyph renderer computed wrong bounding box
//                        first vertex of shape can be off-curve (FreeSans)
//   0.4b (2011-12-03) fixed an error in the font baking example
//   0.4  (2011-12-01) kerning, subpixel rendering (tor)
//                    bugfixes for:
//                        codepoint-to-glyph conversion using table fmt=12
//                        codepoint-to-glyph conversion using table fmt=4
//                        stbtt_GetBakedQuad with non-square texture (Zer)
//                    updated Hello World! sample to use kerning and subpixel
//                    fixed some warnings
//   0.3  (2009-06-24) cmap fmt=12, compound shapes (MM)
//                    userdata, malloc-from-userdata, non-zero fill (stb)
//   0.2  (2009-03-11) Fix unsigned/signed char warnings
//   0.1  (2009-03-09) First public release
//

/*
------------------------------------------------------------------------------
This software is available under 2 licenses -- choose whichever you prefer.
------------------------------------------------------------------------------
ALTERNATIVE A - MIT License
Copyright (c) 2017 Sean Barrett
Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
------------------------------------------------------------------------------
ALTERNATIVE B - Public Domain (www.unlicense.org)
This is free and unencumbered software released into the public domain.
Anyone is free to copy, modify, publish, use, compile, sell, or distribute this
software, either in source code form or as a compiled binary, for any purpose,
commercial or non-commercial, and by any means.
In jurisdictions that recognize copyright laws, the author or authors of this
software dedicate any and all copyright interest in the software to the public
domain. We make this dedication for the benefit of the public at large and to
the detriment of our heirs and successors. We intend this dedication to be an
overt act of relinquishment in perpetuity of all present and future rights to
this software under copyright law.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
------------------------------------------------------------------------------
*/
