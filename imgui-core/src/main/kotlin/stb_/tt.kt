package stb_

import gli_.has
import gli_.hasnt
import glm_.*
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec2.operators.div
import glm_.vec2.operators.times
import glm_.vec4.Vec4i
import imgui.NUL
import kool.BYTES
import kool.lib.fill
import kool.set
import unsigned.toUInt
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

/*
 stb_truetype.h - v1.22 - public domain
 authored from 2009-2019 by Sean Barrett / RAD Game Tools

   This library processes TrueType files:
        parse files
        extract glyph metrics
        extract glyph shapes
        render glyphs to one-channel bitmaps with antialiasing (box filter)
        render glyphs to one-channel SDF bitmaps (signed-distance field/function)

   Todo:
        non-MS cmaps
        crashproof on bad data
        hinting? (no longer patented)
        cleartype-style AA?
        optimize: use simple memory allocator for intermediates
        optimize: build edge-list directly from curves
        optimize: rasterize directly from curves?

 ADDITIONAL CONTRIBUTORS

   Mikko Mononen: compound shape support, more cmap formats
   Tor Andersson: kerning, subpixel rendering
   Dougall Johnson: OpenType / Type 2 font handling
   Daniel Ribeiro Maciel: basic GPOS-based kerning

   Misc other:
       Ryan Gordon
       Simon Glass
       github:IntellectualKitty
       Imanol Celaya
       Daniel Ribeiro Maciel

   Bug/warning reports/fixes:
       "Zer" on mollyrocket       Fabian "ryg" Giesen
       Cass Everitt               Martins Mozeiko
       stoiko (Haemimont Games)   Cap Petschulat
       Brian Hook                 Omar Cornut
       Walter van Niftrik         github:aloucks
       David Gow                  Peter LaValle
       David Given                Sergey Popov
       Ivan-Assen Ivanov          Giumo X. Clanjor
       Anthony Pesch              Higor Euripedes
       Johan Duparc               Thomas Fields
       Hou Qiming                 Derek Vinyard
       Rob Loach                  Cort Stratton
       Kenney Phillis Jr.         github:oyvindjam
       Brian Costabile            github:vassvik
       Ken Voskuil (kaesve)       Ryan Griege

 VERSION HISTORY

   1.22 (2019-08-11) minimize missing-glyph duplication; fix kerning if both 'GPOS' and 'kern' are defined
   1.21 (2019-02-25) fix warning
   1.20 (2019-02-07) PackFontRange skips missing codepoints; GetScaleFontVMetrics()
   1.19 (2018-02-11) GPOS kerning, STBTT_fmod
   1.18 (2018-01-29) add missing function
   1.17 (2017-07-23) make more arguments const; doc fix
   1.16 (2017-07-12) SDF support
   1.15 (2017-03-03) make more arguments const
   1.14 (2017-01-16) num-fonts-in-TTC function
   1.13 (2017-01-02) support OpenType fonts, certain Apple fonts
   1.12 (2016-10-25) suppress warnings about casting away const with -Wcast-qual
   1.11 (2016-04-02) fix unused-variable warning
   1.10 (2016-04-02) user-defined fabs(); rare memory leak; remove duplicate typedef
   1.09 (2016-01-16) warning fix; avoid crash on outofmem; use allocation userdata properly
   1.08 (2015-09-13) document stbtt_Rasterize(); fixes for vertical & horizontal edges
   1.07 (2015-08-01) allow PackFontRanges to accept arrays of sparse codepoints;
                     variant PackFontRanges to pack and render in separate phases;
                     fix stbtt_GetFontOFfsetForIndex (never worked for non-0 input?);
                     fixed an assert() bug in the new rasterizer
                     replace assert() with STBTT_assert() in new rasterizer

   Full history can be found at the end of this file.

 LICENSE

   See end of file for license information.

 USAGE

   Include this file in whatever places need to refer to it. In ONE C/C++
   file, write:
      #define STB_TRUETYPE_IMPLEMENTATION
   before the #include of this file. This expands out the actual
   implementation into that C/C++ file.

   To make the implementation private to the file that generates the implementation,
      #define STBTT_STATIC

   Simple 3D API (don't ship this, but it's fine for tools and quick start)
           stbtt_BakeFontBitmap()               -- bake a font to a bitmap for use as texture
           stbtt_GetBakedQuad()                 -- compute quad to draw for a given char

   Improved 3D API (more shippable):
           #include "stb_rect_pack.h"           -- optional, but you really want it
           stbtt_PackBegin()
           stbtt_PackSetOversampling()          -- for improved quality on small fonts
           stbtt_PackFontRanges()               -- pack and renders
           stbtt_PackEnd()
           stbtt_GetPackedQuad()

   "Load" a font file from a memory buffer (you have to keep the buffer loaded)
           stbtt_InitFont()
           stbtt_GetFontOffsetForIndex()        -- indexing for TTC font collections
           stbtt_GetNumberOfFonts()             -- number of fonts for TTC font collections

   Render a unicode codepoint to a bitmap
           stbtt_GetCodepointBitmap()           -- allocates and returns a bitmap
           stbtt_MakeCodepointBitmap()          -- renders into bitmap you provide
           stbtt_GetCodepointBitmapBox()        -- how big the bitmap must be

   Character advance/positioning
           stbtt_GetCodepointHMetrics()
           stbtt_GetFontVMetrics()
           stbtt_GetFontVMetricsOS2()
           stbtt_GetCodepointKernAdvance()

   Starting with version 1.06, the rasterizer was replaced with a new,
   faster and generally-more-precise rasterizer. The new rasterizer more
   accurately measures pixel coverage for anti-aliasing, except in the case
   where multiple shapes overlap, in which case it overestimates the AA pixel
   coverage. Thus, anti-aliasing of intersecting shapes may look wrong. If
   this turns out to be a problem, you can re-enable the old rasterizer with
        #define STBTT_RASTERIZER_VERSION 1
   which will incur about a 15% speed hit.

 ADDITIONAL DOCUMENTATION

   Immediately after this block comment are a series of sample programs.

   After the sample programs is the "header file" section. This section
   includes documentation for each API function.

   Some important concepts to understand to use this library:

      Codepoint
         Characters are defined by unicode codepoints, e.g. 65 is
         uppercase A, 231 is lowercase c with a cedilla, 0x7e30 is
         the hiragana for "ma".

      Glyph
         A visual character shape (every codepoint is rendered as
         some glyph)

      Glyph index
         A font-specific integer ID representing a glyph

      Baseline
         Glyph shapes are defined relative to a baseline, which is the
         bottom of uppercase characters. Characters extend both above
         and below the baseline.

      Current Point
         As you draw text to the screen, you keep track of a "current point"
         which is the origin of each character. The current point's vertical
         position is the baseline. Even "baked fonts" use this model.

      Vertical Font Metrics
         The vertical qualities of the font, used to vertically position
         and space the characters. See docs for stbtt_GetFontVMetrics.

      Font Size in Pixels or Points
         The preferred interface for specifying font sizes in stb_truetype
         is to specify how tall the font's vertical extent should be in pixels.
         If that sounds good enough, skip the next paragraph.

         Most font APIs instead use "points", which are a common typographic
         measurement for describing font size, defined as 72 points per inch.
         stb_truetype provides a point API for compatibility. However, true
         "per inch" conventions don't make much sense on computer displays
         since different monitors have different number of pixels per
         inch. For example, Windows traditionally uses a convention that
         there are 96 pixels per inch, thus making 'inch' measurements have
         nothing to do with inches, and thus effectively defining a point to
         be 1.333 pixels. Additionally, the TrueType font data provides
         an explicit scale factor to scale a given font's glyphs to points,
         but the author has observed that this scale factor is often wrong
         for non-commercial fonts, thus making fonts scaled in points
         according to the TrueType spec incoherently sized in practice.

 DETAILED USAGE:

  Scale:
    Select how high you want the font to be, in points or pixels.
    Call ScaleForPixelHeight or ScaleForMappingEmToPixels to compute
    a scale factor SF that will be used by all other functions.

  Baseline:
    You need to select a y-coordinate that is the baseline of where
    your text will appear. Call GetFontBoundingBox to get the baseline-relative
    bounding box for all characters. SF*-y0 will be the distance in pixels
    that the worst-case character could extend above the baseline, so if
    you want the top edge of characters to appear at the top of the
    screen where y=0, then you would set the baseline to SF*-y0.

  Current point:
    Set the current point where the first character will appear. The
    first character could extend left of the current point; this is font
    dependent. You can either choose a current point that is the leftmost
    point and hope, or add some padding, or check the bounding box or
    left-side-bearing of the first character to be displayed and set
    the current point based on that.

  Displaying a character:
    Compute the bounding box of the character. It will contain signed values
    relative to <current_point, baseline>. I.e. if it returns x0,y0,x1,y1,
    then the character should be displayed in the rectangle from
    <current_point+SF*x0, baseline+SF*y0> to <current_point+SF*x1,baseline+SF*y1).

  Advancing for the next character:
    Call GlyphHMetrics, and compute 'current_point += SF * advance'.


 ADVANCED USAGE

   Quality:

    - Use the functions with Subpixel at the end to allow your characters
      to have subpixel positioning. Since the font is anti-aliased, not
      hinted, this is very import for quality. (This is not possible with
      baked fonts.)

    - Kerning is now supported, and if you're supporting subpixel rendering
      then kerning is worth using to give your text a polished look.

   Performance:

    - Convert Unicode codepoints to glyph indexes and operate on the glyphs;
      if you don't do this, stb_truetype is forced to do the conversion on
      every call.

    - There are a lot of memory allocations. We should modify it to take
      a temp buffer and allocate from the temp buffer (without freeing),
      should help performance a lot.

 NOTES

   The system uses the raw data found in the .ttf file without changing it
   and without building auxiliary data structures. This is a bit inefficient
   on little-endian systems (the data is big-endian), but assuming you're
   caching the bitmaps or glyph shapes this shouldn't be a big deal.

   It appears to be very hard to programmatically determine what font a
   given file is in a general way. I provide an API for this, but I don't
   recommend it.


 PERFORMANCE MEASUREMENTS FOR 1.06:

                      32-bit     64-bit
   Previous release:  8.83 s     7.68 s
   Pool allocations:  7.72 s     6.34 s
   Inline sort     :  6.54 s     5.65 s
   New rasterizer  :  5.63 s     5.00 s
*/

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
////
////  SAMPLE PROGRAMS
////
//
//  Incomplete text-in-3d-api example, which draws quads properly aligned to be lossless
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
//    glEnable(GL_TEXTURE_2D);
//    glBindTexture(GL_TEXTURE_2D, ftex);
//    glBegin(GL_QUADS);
//    while (*text) {
//    if (*text >= 32 && *text < 128) {
//    stbtt_aligned_quad q;
//    stbtt_GetBakedQuad(cdata, 512,512, *text-32, &x,&y,&q,1);//1=opengl & d3d10+,0=d3d9
//    glTexCoord2f(q.s0,q.t1); glVertex2f(q.x0,q.y0);
//    glTexCoord2f(q.s1,q.t1); glVertex2f(q.x1,q.y0);
//    glTexCoord2f(q.s1,q.t0); glVertex2f(q.x1,q.y1);
//    glTexCoord2f(q.s0,q.t0); glVertex2f(q.x0,q.y1);
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


object tt {

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
//
//// e.g. #define your own STBTT_ifloor/STBTT_iceil() to avoid math.h
//#ifndef STBTT_ifloor
//#include <math.h>
//#define STBTT_ifloor(x)   ((int) floor(x))
//#define STBTT_iceil(x)    ((int) ceil(x))
//#endif
//
//#ifndef STBTT_sqrt
//#include <math.h>
//#define STBTT_sqrt(x)      sqrt(x)
//#define STBTT_pow(x,y)     pow(x,y)
//#endif
//
//#ifndef STBTT_fmod
//#include <math.h>
//#define STBTT_fmod(x,y)    fmod(x,y)
//#endif
//
//#ifndef STBTT_cos
//#include <math.h>
//#define STBTT_cos(x)       cos(x)
//#define STBTT_acos(x)      acos(x)
//#endif
//
//#ifndef STBTT_fabs
//#include <math.h>
//#define STBTT_fabs(x)      fabs(x)
//#endif
//
//// #define your own functions "STBTT_malloc" / "STBTT_free" to avoid malloc.h
//#ifndef STBTT_malloc
//#include <stdlib.h>
//#define STBTT_malloc(x,u)  ((void)(u),malloc(x))
//#define STBTT_free(x,u)    ((void)(u),free(x))
//#endif
//
//#ifndef STBTT_assert
//#include <assert.h>
//#define STBTT_assert(x)    assert(x)
//#endif
//
//#ifndef STBTT_strlen
//#include <string.h>
//#define STBTT_strlen(x)    strlen(x)
//#endif
//
//#ifndef STBTT_memcpy
//#include <string.h>
//#define STBTT_memcpy       memcpy
//#define STBTT_memset       memset
//#endif
//#endif

    /////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    //
    //   INTERFACE
    //
    //


    // -> Buf

    //////////////////////////////////////////////////////////////////////////////
    //
    // TEXTURE BAKING API
    //
    // If you use this API, you only have to call two functions ever.
    //

//    typedef struct
//            {
//                unsigned short x0,y0,x1,y1; // coordinates of bbox in bitmap
//                float xoff,yoff,xadvance;
//            } stbtt_bakedchar;
//
//    STBTT_DEF int stbtt_BakeFontBitmap(const unsigned char *data, int offset,  // font location (use offset=0 for plain .ttf)
//            float pixel_height,                     // height of font in pixels
//            unsigned char *pixels, int pw, int ph,  // bitmap to be filled in
//            int first_char, int num_chars,          // characters to bake
//            stbtt_bakedchar *chardata);             // you allocate this, it's num_chars long
//// if return is positive, the first unused row of the bitmap
//// if return is negative, returns the negative of the number of characters that fit
//// if return is 0, no characters fit and no rows were used
//// This uses a very crappy packing.

    class AlignedQuad {

        // top-left

        var x0 = 0f
        var y0 = 0f
        var s0 = 0f
        var t0 = 0f

        // bottom-right

        var x1 = 0f
        var y1 = 0f
        var s1 = 0f
        var t1 = 0f
    }

//    STBTT_DEF void stbtt_GetBakedQuad(const stbtt_bakedchar *chardata, int pw, int ph,  // same data as above
//            int char_index,             // character to display
//            float *xpos, float *ypos,   // pointers to current position in screen pixel space
//            stbtt_aligned_quad *q,      // output: quad to draw
//            int opengl_fillrule);       // true if opengl fill rule; false if DX9 or earlier
//// Call GetBakedQuad with char_index = 'character - first_char', and it
//// creates the quad you need to draw and advances the current position.
////
//// The coordinate system used assumes y increases downwards.
////
//// Characters will extend both above and below the current position;
//// see discussion of "BASELINE" above.
////
//// It's inefficient; you might want to c&p it and optimize it.
//
//    STBTT_DEF void stbtt_GetScaledFontVMetrics(const unsigned char *fontdata, int index, float size, float *ascent, float *descent, float *lineGap);
//// Query the font vertical metrics without having to create a font first.


    //////////////////////////////////////////////////////////////////////////////
    //
    // NEW TEXTURE BAKING API
    //
    // This provides options for packing multiple fonts into one atlas, not
    // perfectly but better than nothing.

    class PackedChar {

        // coordinates of bbox in bitmap

        var x0 = 0
        var y0 = 0
        var x1 = 0
        var y1 = 0


        var xOff = 0f
        var yOff = 0f
        var xAdvance = 0f
        var xOff2 = 0f
        var yOff2 = 0f
    }

//    typedef struct stbtt_pack_context stbtt_pack_context;
//    typedef struct stbtt_fontinfo stbtt_fontinfo;
//    #ifndef STB_RECT_PACK_VERSION
//            typedef struct stbrp_rect stbrp_rect;
//    #endif
//
//    STBTT_DEF int  stbtt_PackBegin(stbtt_pack_context *spc, unsigned char *pixels, int width, int height, int stride_in_bytes, int padding, void *alloc_context);
//// Initializes a packing context stored in the passed-in stbtt_pack_context.
//// Future calls using this context will pack characters into the bitmap passed
//// in here: a 1-channel bitmap that is width * height. stride_in_bytes is
//// the distance from one row to the next (or 0 to mean they are packed tightly
//// together). "padding" is the amount of padding to leave between each
//// character (normally you want '1' for bitmaps you'll use as textures with
//// bilinear filtering).
////
//// Returns 0 on failure, 1 on success.
//
//    STBTT_DEF void stbtt_PackEnd  (stbtt_pack_context *spc);
//// Cleans up the packing context and frees all memory.
//
//    #define STBTT_POINT_SIZE(x)   (-(x))
//
//    STBTT_DEF int  stbtt_PackFontRange(stbtt_pack_context *spc, const unsigned char *fontdata, int font_index, float font_size,
//            int first_unicode_char_in_range, int num_chars_in_range, stbtt_packedchar *chardata_for_range);
//// Creates character bitmaps from the font_index'th font found in fontdata (use
//// font_index=0 if you don't know what that is). It creates num_chars_in_range
//// bitmaps for characters with unicode values starting at first_unicode_char_in_range
//// and increasing. Data for how to render them is stored in chardata_for_range;
//// pass these to stbtt_GetPackedQuad to get back renderable quads.
////
//// font_size is the full height of the character from ascender to descender,
//// as computed by stbtt_ScaleForPixelHeight. To use a point size as computed
//// by stbtt_ScaleForMappingEmToPixels, wrap the point size in STBTT_POINT_SIZE()
//// and pass that result as 'font_size':
////       ...,                  20 , ... // font max minus min y is 20 pixels tall
////       ..., STBTT_POINT_SIZE(20), ... // 'M' is 20 pixels tall

    class PackRange {
        var fontSize = 0f

        /** if non-zero, then the chars are continuous, and this is the first codepoint */
        var firstUnicodeCodepointInRange = 0

        /** if non-zero, then this is an array of unicode codepoints */
        var arrayOfUnicodeCodepoints: IntArray? = null
        var numChars = 0

        /** output */
        lateinit var chardataForRange: Array<PackedChar>

        // don't set these, they're used internally

        var oversample = Vec2i()
    }

//    STBTT_DEF int  stbtt_PackFontRanges(stbtt_pack_context *spc, const unsigned char *fontdata, int font_index, stbtt_pack_range *ranges, int num_ranges);
//// Creates character bitmaps from multiple ranges of characters stored in
//// ranges. This will usually create a better-packed bitmap than multiple
//// calls to stbtt_PackFontRange. Note that you can call this multiple
//// times within a single PackBegin/PackEnd.
//
//    STBTT_DEF void stbtt_PackSetOversampling(stbtt_pack_context *spc, unsigned int h_oversample, unsigned int v_oversample);
//// Oversampling a font increases the quality by allowing higher-quality subpixel
//// positioning, and is especially valuable at smaller text sizes.
////
//// This function sets the amount of oversampling for all following calls to
//// stbtt_PackFontRange(s) or stbtt_PackFontRangesGatherRects for a given
//// pack context. The default (no oversampling) is achieved by h_oversample=1
//// and v_oversample=1. The total number of pixels required is
//// h_oversample*v_oversample larger than the default; for example, 2x2
//// oversampling requires 4x the storage of 1x1. For best results, render
//// oversampled textures with bilinear filtering. Look at the readme in
//// stb/tests/oversample for information about oversampled fonts
////
//// To use with PackFontRangesGather etc., you must set it before calls
//// call to PackFontRangesGatherRects.
//
//    STBTT_DEF void stbtt_PackSetSkipMissingCodepoints(stbtt_pack_context *spc, int skip);
//// If skip != 0, this tells stb_truetype to skip any codepoints for which
//// there is no corresponding glyph. If skip=0, which is the default, then
//// codepoints without a glyph recived the font's "missing character" glyph,
//// typically an empty box by convention.
//
//    STBTT_DEF void stbtt_GetPackedQuad(const stbtt_packedchar *chardata, int pw, int ph,  // same data as above
//            int char_index,             // character to display
//            float *xpos, float *ypos,   // pointers to current position in screen pixel space
//            stbtt_aligned_quad *q,      // output: quad to draw
//            int align_to_integer);
//
//    STBTT_DEF int  stbtt_PackFontRangesGatherRects(stbtt_pack_context *spc, const stbtt_fontinfo *info, stbtt_pack_range *ranges, int num_ranges, stbrp_rect *rects);
//    STBTT_DEF void stbtt_PackFontRangesPackRects(stbtt_pack_context *spc, stbrp_rect *rects, int num_rects);
//    STBTT_DEF int  stbtt_PackFontRangesRenderIntoRects(stbtt_pack_context *spc, const stbtt_fontinfo *info, stbtt_pack_range *ranges, int num_ranges, stbrp_rect *rects);
//// Calling these functions in sequence is roughly equivalent to calling
//// stbtt_PackFontRanges(). If you more control over the packing of multiple
//// fonts, or if you want to pack custom data into a font texture, take a look
//// at the source to of stbtt_PackFontRanges() and create a custom version
//// using these functions, e.g. call GatherRects multiple times,
//// building up a single array of rects, then call PackRects once,
//// then call RenderIntoRects repeatedly. This may result in a
//// better packing than calling PackFontRanges multiple times
//// (or it may not).

    /** this is an opaque structure that you shouldn't mess with which holds
     *  all the context needed from PackBegin to PackEnd. */
    class PackContext {
        lateinit var packInfo: rp.Context
        var width = 0
        var height = 0
        var strideInBytes = 0
        var padding = 0
        var skipMissing = false
        val oversample = Vec2i()
        lateinit var pixels: ByteBuffer
        lateinit var nodes: Array<rp.Node>
    }

    //////////////////////////////////////////////////////////////////////////////
    //
    // FONT LOADING
    //
    //

    /** This function will determine the number of fonts in a font file.  TrueType
     *  collection (.ttc) files may contain multiple fonts, while TrueType font
     *  (.ttf) files only contain one font. The number of fonts can be used for
     *  indexing with the previous function where the index is between zero and one
     *  less than the total fonts. If an error occurs, -1 is returned. */
//    STBTT_DEF int stbtt_GetNumberOfFonts(const unsigned char *data );

    /** Each .ttf/.ttc file may have more than one font. Each font has a sequential
     *  index number starting from 0. Call this function to get the font offset for
     *  a given index; it returns -1 if the index is out of range. A regular .ttf
     *  file will only define one font and it always be at offset 0, so it will
     *  return '0' for index 0, and -1 for all other indices. */
//    STBTT_DEF int stbtt_GetFontOffsetForIndex(const unsigned char *data , int index);

    /** The following structure is defined publicly so you can declare one on
     *  the stack or as a global or etc, but you should treat it as opaque. */
    class FontInfo {

        var userData: Any? = null

        /** pointer to .ttf file */
        lateinit var data: ByteBuffer

        /** offset of start of font */
        var fontStart = 0

        /** number of glyphs, needed for range checking */
        var numGlyphs = 0


        // table locations as offset from start of .ttf

        var loca = 0
        var head = 0
        var glyf = 0
        var hhea = 0
        var hmtx = 0
        var kern = 0
        var gpos = 0

        /** a cmap mapping for our chosen character encoding */
        var indexMap = 0

        /** format needed to map from glyph index to glyph */
        var indexToLocFormat = 0

        /** cff font data */
        lateinit var cff: Buf

        /** the charstring index */
        lateinit var charStrings: Buf

        /** global charstring subroutines index */
        lateinit var gSubrs: Buf

        /** private charstring subroutines index */
        lateinit var subrs: Buf

        /** array of font dicts */
        lateinit var fontDicts: Buf

        /** map from glyph to fontdict */
        lateinit var fdSelect: Buf
    }

//    STBTT_DEF int stbtt_InitFont(stbtt_fontinfo *info, const unsigned char *data, int offset);
//// Given an offset into the file that defines a font, this function builds
//// the necessary cached info for the rest of the system. You must allocate
//// the stbtt_fontinfo yourself, and stbtt_InitFont will fill it out. You don't
//// need to do anything special to free it, because the contents are pure
//// value data with no additional data structures. Returns 0 on failure.
//
//
////////////////////////////////////////////////////////////////////////////////
////
//// CHARACTER TO GLYPH-INDEX CONVERSIOn
//
//    STBTT_DEF int stbtt_FindGlyphIndex(const stbtt_fontinfo *info, int unicode_codepoint);
//// If you're going to perform multiple operations on the same character
//// and you want a speed-up, call this function with the character you're
//// going to process, then use glyph-based functions instead of the
//// codepoint-based functions.
//// Returns 0 if the character codepoint is not defined in the font.
//
//
////////////////////////////////////////////////////////////////////////////////
////
//// CHARACTER PROPERTIES
////
//
//    STBTT_DEF float stbtt_ScaleForPixelHeight(const stbtt_fontinfo *info, float pixels);
//// computes a scale factor to produce a font whose "height" is 'pixels' tall.
//// Height is measured as the distance from the highest ascender to the lowest
//// descender; in other words, it's equivalent to calling stbtt_GetFontVMetrics
//// and computing:
////       scale = pixels / (ascent - descent)
//// so if you prefer to measure height by the ascent only, use a similar calculation.
//
//    STBTT_DEF float stbtt_ScaleForMappingEmToPixels(const stbtt_fontinfo *info, float pixels);
//// computes a scale factor to produce a font whose EM size is mapped to
//// 'pixels' tall. This is probably what traditional APIs compute, but
//// I'm not positive.
//
//    STBTT_DEF void stbtt_GetFontVMetrics(const stbtt_fontinfo *info, int *ascent, int *descent, int *lineGap);
//// ascent is the coordinate above the baseline the font extends; descent
//// is the coordinate below the baseline the font extends (i.e. it is typically negative)
//// lineGap is the spacing between one row's descent and the next row's ascent...
//// so you should advance the vertical position by "*ascent - *descent + *lineGap"
////   these are expressed in unscaled coordinates, so you must multiply by
////   the scale factor for a given size
//
//    STBTT_DEF int  stbtt_GetFontVMetricsOS2(const stbtt_fontinfo *info, int *typoAscent, int *typoDescent, int *typoLineGap);
//// analogous to GetFontVMetrics, but returns the "typographic" values from the OS/2
//// table (specific to MS/Windows TTF files).
////
//// Returns 1 on success (table present), 0 on failure.
//
//    STBTT_DEF void stbtt_GetFontBoundingBox(const stbtt_fontinfo *info, int *x0, int *y0, int *x1, int *y1);
//// the bounding box around all possible characters
//
//    STBTT_DEF void stbtt_GetCodepointHMetrics(const stbtt_fontinfo *info, int codepoint, int *advanceWidth, int *leftSideBearing);
//// leftSideBearing is the offset from the current horizontal position to the left edge of the character
//// advanceWidth is the offset from the current horizontal position to the next horizontal position
////   these are expressed in unscaled coordinates
//
//    STBTT_DEF int  stbtt_GetCodepointKernAdvance(const stbtt_fontinfo *info, int ch1, int ch2);
//// an additional amount to add to the 'advance' value between ch1 and ch2
//
//    STBTT_DEF int stbtt_GetCodepointBox(const stbtt_fontinfo *info, int codepoint, int *x0, int *y0, int *x1, int *y1);
//// Gets the bounding box of the visible part of the glyph, in unscaled coordinates
//
//    STBTT_DEF void stbtt_GetGlyphHMetrics(const stbtt_fontinfo *info, int glyph_index, int *advanceWidth, int *leftSideBearing);
//    STBTT_DEF int  stbtt_GetGlyphKernAdvance(const stbtt_fontinfo *info, int glyph1, int glyph2);
//    STBTT_DEF int  stbtt_GetGlyphBox(const stbtt_fontinfo *info, int glyph_index, int *x0, int *y0, int *x1, int *y1);
//// as above, but takes one or more glyph indices for greater efficiency


    //////////////////////////////////////////////////////////////////////////////
    //
    // GLYPH SHAPES (you probably don't need these, but they have to go before
    // the bitmaps for C declaration-order reasons)
    //

    enum class V {
        move, line, curve, cubic;

        val i = ordinal + 1
    }

    // (we share this with other code at RAD)
    class Vertex {
        var x = 0
        var y = 0
        var cX = 0
        var cY = 0
        var cX1 = 0
        var cY1 = 0
        var type = 0
        //,padding;

        fun set(type: V, x: Int, y: Int, cX: Int, cY: Int) {
            this.type = type.i
            this.x = x
            this.y = y
            this.cX = cX
            this.cY = cY
        }

        override fun toString() = "x=$x y=$y cx=$cX cy=$cY cx1=$cX1 cy1=$cY1 type=$type"
    }

//    STBTT_DEF int stbtt_IsGlyphEmpty(const stbtt_fontinfo *info, int glyph_index);
//// returns non-zero if nothing is drawn for this glyph
//
//    STBTT_DEF int stbtt_GetCodepointShape(const stbtt_fontinfo *info, int unicode_codepoint, stbtt_vertex **vertices);
//    STBTT_DEF int stbtt_GetGlyphShape(const stbtt_fontinfo *info, int glyph_index, stbtt_vertex **vertices);
//// returns # of vertices and fills *vertices with the pointer to them
////   these are expressed in "unscaled" coordinates
////
//// The shape is a series of contours. Each one starts with
//// a STBTT_moveto, then consists of a series of mixed
//// STBTT_lineto and STBTT_curveto segments. A lineto
//// draws a line from previous endpoint to its x,y; a curveto
//// draws a quadratic bezier from previous endpoint to
//// its x,y, using cx,cy as the bezier control point.
//
//    STBTT_DEF void stbtt_FreeShape(const stbtt_fontinfo *info, stbtt_vertex *vertices);
//// frees the data allocated above
//
////////////////////////////////////////////////////////////////////////////////
////
//// BITMAP RENDERING
////
//
//    STBTT_DEF void stbtt_FreeBitmap(unsigned char *bitmap, void *userdata);
//// frees the bitmap allocated below
//
//    STBTT_DEF unsigned char *stbtt_GetCodepointBitmap(const stbtt_fontinfo *info, float scale_x, float scale_y, int codepoint, int *width, int *height, int *xoff, int *yoff);
//// allocates a large-enough single-channel 8bpp bitmap and renders the
//// specified character/glyph at the specified scale into it, with
//// antialiasing. 0 is no coverage (transparent), 255 is fully covered (opaque).
//// *width & *height are filled out with the width & height of the bitmap,
//// which is stored left-to-right, top-to-bottom.
////
//// xoff/yoff are the offset it pixel space from the glyph origin to the top-left of the bitmap
//
//    STBTT_DEF unsigned char *stbtt_GetCodepointBitmapSubpixel(const stbtt_fontinfo *info, float scale_x, float scale_y, float shift_x, float shift_y, int codepoint, int *width, int *height, int *xoff, int *yoff);
//// the same as stbtt_GetCodepoitnBitmap, but you can specify a subpixel
//// shift for the character
//
//    STBTT_DEF void stbtt_MakeCodepointBitmap(const stbtt_fontinfo *info, unsigned char *output, int out_w, int out_h, int out_stride, float scale_x, float scale_y, int codepoint);
//// the same as stbtt_GetCodepointBitmap, but you pass in storage for the bitmap
//// in the form of 'output', with row spacing of 'out_stride' bytes. the bitmap
//// is clipped to out_w/out_h bytes. Call stbtt_GetCodepointBitmapBox to get the
//// width and height and positioning info for it first.
//
//    STBTT_DEF void stbtt_MakeCodepointBitmapSubpixel(const stbtt_fontinfo *info, unsigned char *output, int out_w, int out_h, int out_stride, float scale_x, float scale_y, float shift_x, float shift_y, int codepoint);
//// same as stbtt_MakeCodepointBitmap, but you can specify a subpixel
//// shift for the character
//
//    STBTT_DEF void stbtt_MakeCodepointBitmapSubpixelPrefilter(const stbtt_fontinfo *info, unsigned char *output, int out_w, int out_h, int out_stride, float scale_x, float scale_y, float shift_x, float shift_y, int oversample_x, int oversample_y, float *sub_x, float *sub_y, int codepoint);
//// same as stbtt_MakeCodepointBitmapSubpixel, but prefiltering
//// is performed (see stbtt_PackSetOversampling)
//
//    STBTT_DEF void stbtt_GetCodepointBitmapBox(const stbtt_fontinfo *font, int codepoint, float scale_x, float scale_y, int *ix0, int *iy0, int *ix1, int *iy1);
//// get the bbox of the bitmap centered around the glyph origin; so the
//// bitmap width is ix1-ix0, height is iy1-iy0, and location to place
//// the bitmap top left is (leftSideBearing*scale,iy0).
//// (Note that the bitmap uses y-increases-down, but the shape uses
//// y-increases-up, so CodepointBitmapBox and CodepointBox are inverted.)
//
//    STBTT_DEF void stbtt_GetCodepointBitmapBoxSubpixel(const stbtt_fontinfo *font, int codepoint, float scale_x, float scale_y, float shift_x, float shift_y, int *ix0, int *iy0, int *ix1, int *iy1);
//// same as stbtt_GetCodepointBitmapBox, but you can specify a subpixel
//// shift for the character
//
//// the following functions are equivalent to the above functions, but operate
//// on glyph indices instead of Unicode codepoints (for efficiency)
//    STBTT_DEF unsigned char *stbtt_GetGlyphBitmap(const stbtt_fontinfo *info, float scale_x, float scale_y, int glyph, int *width, int *height, int *xoff, int *yoff);
//    STBTT_DEF unsigned char *stbtt_GetGlyphBitmapSubpixel(const stbtt_fontinfo *info, float scale_x, float scale_y, float shift_x, float shift_y, int glyph, int *width, int *height, int *xoff, int *yoff);
//    STBTT_DEF void stbtt_MakeGlyphBitmap(const stbtt_fontinfo *info, unsigned char *output, int out_w, int out_h, int out_stride, float scale_x, float scale_y, int glyph);
//    STBTT_DEF void stbtt_MakeGlyphBitmapSubpixel(const stbtt_fontinfo *info, unsigned char *output, int out_w, int out_h, int out_stride, float scale_x, float scale_y, float shift_x, float shift_y, int glyph);
//    STBTT_DEF void stbtt_MakeGlyphBitmapSubpixelPrefilter(const stbtt_fontinfo *info, unsigned char *output, int out_w, int out_h, int out_stride, float scale_x, float scale_y, float shift_x, float shift_y, int oversample_x, int oversample_y, float *sub_x, float *sub_y, int glyph);
//    STBTT_DEF void stbtt_GetGlyphBitmapBox(const stbtt_fontinfo *font, int glyph, float scale_x, float scale_y, int *ix0, int *iy0, int *ix1, int *iy1);
//    STBTT_DEF void stbtt_GetGlyphBitmapBoxSubpixel(const stbtt_fontinfo *font, int glyph, float scale_x, float scale_y,float shift_x, float shift_y, int *ix0, int *iy0, int *ix1, int *iy1);
//

    /** @TODO: don't expose this structure */
    class Bitmap(val w: Int, val h: Int, val stride: Int, val pixels: ByteBuffer)

//// rasterize a shape with quadratic beziers into a bitmap
//    STBTT_DEF void stbtt_Rasterize(stbtt__bitmap *result,        // 1-channel bitmap to draw into
//            float flatness_in_pixels,     // allowable error of curve in pixels
//            stbtt_vertex *vertices,       // array of vertices defining shape
//            int num_verts,                // number of vertices in above array
//            float scale_x, float scale_y, // scale applied to input vertices
//            float shift_x, float shift_y, // translation applied to input vertices
//            int x_off, int y_off,         // another translation applied to input
//            int invert,                   // if non-zero, vertically flip shape
//            void *userdata);              // context for to STBTT_MALLOC
//
////////////////////////////////////////////////////////////////////////////////
////
//// Signed Distance Function (or Field) rendering
//
//    STBTT_DEF void stbtt_FreeSDF(unsigned char *bitmap, void *userdata);
//// frees the SDF bitmap allocated below
//
//    STBTT_DEF unsigned char * stbtt_GetGlyphSDF(const stbtt_fontinfo *info, float scale, int glyph, int padding, unsigned char onedge_value, float pixel_dist_scale, int *width, int *height, int *xoff, int *yoff);
//    STBTT_DEF unsigned char * stbtt_GetCodepointSDF(const stbtt_fontinfo *info, float scale, int codepoint, int padding, unsigned char onedge_value, float pixel_dist_scale, int *width, int *height, int *xoff, int *yoff);
//// These functions compute a discretized SDF field for a single character, suitable for storing
//// in a single-channel texture, sampling with bilinear filtering, and testing against
//// larger than some threshold to produce scalable fonts.
////        info              --  the font
////        scale             --  controls the size of the resulting SDF bitmap, same as it would be creating a regular bitmap
////        glyph/codepoint   --  the character to generate the SDF for
////        padding           --  extra "pixels" around the character which are filled with the distance to the character (not 0),
////                                 which allows effects like bit outlines
////        onedge_value      --  value 0-255 to test the SDF against to reconstruct the character (i.e. the isocontour of the character)
////        pixel_dist_scale  --  what value the SDF should increase by when moving one SDF "pixel" away from the edge (on the 0..255 scale)
////                                 if positive, > onedge_value is inside; if negative, < onedge_value is inside
////        width,height      --  output height & width of the SDF bitmap (including padding)
////        xoff,yoff         --  output origin of the character
////        return value      --  a 2D array of bytes 0..255, width*height in size
////
//// pixel_dist_scale & onedge_value are a scale & bias that allows you to make
//// optimal use of the limited 0..255 for your application, trading off precision
//// and special effects. SDF values outside the range 0..255 are clamped to 0..255.
////
//// Example:
////      scale = stbtt_ScaleForPixelHeight(22)
////      padding = 5
////      onedge_value = 180
////      pixel_dist_scale = 180/5.0 = 36.0
////
////      This will create an SDF bitmap in which the character is about 22 pixels
////      high but the whole bitmap is about 22+5+5=32 pixels high. To produce a filled
////      shape, sample the SDF at each pixel and fill the pixel if the SDF value
////      is greater than or equal to 180/255. (You'll actually want to antialias,
////      which is beyond the scope of this example.) Additionally, you can compute
////      offset outlines (e.g. to stroke the character border inside & outside,
////      or only outside). For example, to fill outside the character up to 3 SDF
////      pixels, you would compare against (180-36.0*3)/255 = 72/255. The above
////      choice of variables maps a range from 5 pixels outside the shape to
////      2 pixels inside the shape to 0..255; this is intended primarily for apply
////      outside effects only (the interior range is needed to allow proper
////      antialiasing of the font at *smaller* sizes)
////
//// The function computes the SDF analytically at each SDF pixel, not by e.g.
//// building a higher-res bitmap and approximating it. In theory the quality
//// should be as high as possible for an SDF of this size & representation, but
//// unclear if this is true in practice (perhaps building a higher-res bitmap
//// and computing from that can allow drop-out prevention).
////
//// The algorithm has not been optimized at all, so expect it to be slow
//// if computing lots of characters or very large sizes.
//
//
//
////////////////////////////////////////////////////////////////////////////////
////
//// Finding the right font...
////
//// You should really just solve this offline, keep your own tables
//// of what font is what, and don't try to get it out of the .ttf file.
//// That's because getting it out of the .ttf file is really hard, because
//// the names in the file can appear in many possible encodings, in many
//// possible languages, and e.g. if you need a case-insensitive comparison,
//// the details of that depend on the encoding & language in a complex way
//// (actually underspecified in truetype, but also gigantic).
////
//// But you can use the provided functions in two possible ways:
////     stbtt_FindMatchingFont() will use *case-sensitive* comparisons on
////             unicode-encoded names to try to find the font you want;
////             you can run this before calling stbtt_InitFont()
////
////     stbtt_GetFontNameString() lets you get any of the various strings
////             from the file yourself and do your own comparisons on them.
////             You have to have called stbtt_InitFont() first.
//
//
//    STBTT_DEF int stbtt_FindMatchingFont(const unsigned char *fontdata, const char *name, int flags);
//// returns the offset (not index) of the font that matches, or -1 if none
////   if you use STBTT_MACSTYLE_DONTCARE, use a font name like "Arial Bold".
////   if you use any other flag, use a font name like "Arial"; this checks
////     the 'macStyle' header field; i don't know if fonts set this consistently
//    #define STBTT_MACSTYLE_DONTCARE     0
//    #define STBTT_MACSTYLE_BOLD         1
//    #define STBTT_MACSTYLE_ITALIC       2
//    #define STBTT_MACSTYLE_UNDERSCORE   4
//    #define STBTT_MACSTYLE_NONE         8   // <= not same as 0, this makes us check the bitfield is 0
//
//    STBTT_DEF int stbtt_CompareUTF8toUTF16_bigendian(const char *s1, int len1, const char *s2, int len2);
//// returns 1/0 whether the first string interpreted as utf8 is identical to
//// the second string interpreted as big-endian utf16... useful for strings from next func
//
//    STBTT_DEF const char *stbtt_GetFontNameString(const stbtt_fontinfo *font, int *length, int platformID, int encodingID, int languageID, int nameID);
//// returns the string (which may be big-endian double byte, e.g. for unicode)
//// and puts the length in bytes in *length.

    // some of the values for the IDs are below; for more see the truetype spec:
    //     http://developer.apple.com/textfonts/TTRefMan/RM06/Chap6name.html
    //     http://www.microsoft.com/typography/otspec/name.htm

    enum class PlatformID {
        UNICODE, MAC, ISO, MICROSOFT;

        val i = ordinal
    }

    /** encodingID for STBTT_PLATFORM_ID_UNICODE */
    enum class UnicodeEID {
        UNICODE_1_0, UNICODE_1_1, ISO_10646, UNICODE_2_0_BMP, UNICODE_2_0_FULL;

        val i = ordinal
    }

    /** encodingID for STBTT_PLATFORM_ID_MICROSOFT */
    enum class MS_EID(val i: Int) { SYMBOL(0), UNICODE_BMP(1), SHIFTJIS(2), UNICODE_FULL(10) }

    /** encodingID for STBTT_PLATFORM_ID_MAC; same as Script Manager codes */
    enum class MAC_EID {
        ROMAN, JAPANESE, CHINESE_TRAD, KOREAN, ARABIC, HEBREW, GREEK, RUSSIAN;

        val i = ordinal
    }

    /** languageID for STBTT_PLATFORM_ID_MICROSOFT; same as LCID... */
    enum class MS_LANG(val i: Int) {
        // problematic because there are e.g. 16 english LCIDs and 16 arabic LCIDs
        ENGLISH(0x0409), ITALIAN(0x0410), CHINESE(0x0804), JAPANESE(0x0411), DUTCH(0x0413), KOREAN(0x0412),
        FRENCH(0x040c), RUSSIAN(0x0419), GERMAN(0x0407), SPANISH(0x0409), HEBREW(0x040d), SWEDISH(0x041D)
    }

    enum class MAC_LANG(val i: Int) { // languageID for STBTT_PLATFORM_ID_MAC
        ENGLISH(0), JAPANESE(11),
        ARABIC(12), KOREAN(23),
        DUTCH(4), RUSSIAN(32),
        FRENCH(1), SPANISH(6),
        GERMAN(2), SWEDISH(5),
        HEBREW(10), CHINESE_SIMPLIFIED(33),
        ITALIAN(3), CHINESE_TRAD(19)
    }

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

    // buf helpers -> Buf

    ////////////////////////////////////////////////////////////////////////////
////
//// accessors to parse data from file
////
//
//// on platforms that don't allow misaligned reads, if we want to allow
//// truetype fonts that aren't padded to alignment, define ALLOW_UNALIGNED_TRUETYPE
//
//#define ttBYTE(p)     (* (stbtt_uint8 *) (p))
//#define ttCHAR(p)     (* (stbtt_int8 *) (p))
//#define ttFixed(p)    ttLONG(p)
//
//static stbtt_uint16 ttUSHORT(stbtt_uint8 *p) { return p[0]*256 + p[1]; }
    fun ByteBuffer.shortUI(p: Int) = getShort(p).toUInt()
//static stbtt_int16 ttSHORT(stbtt_uint8 *p)   { return p[0]*256 + p[1]; }
//static stbtt_uint32 ttULONG(stbtt_uint8 *p)  { return (p[0]<<24) + (p[1]<<16) + (p[2]<<8) + p[3]; }
//static stbtt_int32 ttLONG(stbtt_uint8 *p)    { return (p[0]<<24) + (p[1]<<16) + (p[2]<<8) + p[3]; }

    fun tag4(p: ByteBuffer, c0: Char, c1: Char, c2: Char, c3: Char) =
            p[0] == c0.b && p[1] == c1.b && p[2] == c2.b && p[3] == c3.b

    fun tag(p: ByteBuffer, str: String) = tag4(p, str[0], str[1], str[2], str[3])

    fun isFont(font: ByteBuffer): Boolean = when { // check the version number
        tag4(font, '1', NUL, NUL, NUL) -> true // TrueType 1
        tag(font, "typ1") -> true // TrueType with type 1 font -- we don't support this!
        tag(font, "OTTO") -> true // OpenType with CFF
        tag4(font, NUL, 1.c, NUL, NUL) -> true // OpenType 1.0
        tag(font, "true") -> true // Apple specification for TrueType fonts
        else -> false
    }

    // @OPTIMIZE: binary search
    fun findTable(data: ByteBuffer, fontStart: Int, tag: String): Int {
        val numTables = data.getShort(fontStart + 4).i
        val tableDir = fontStart + 12
        for (i in 0 until numTables) {
            val loc = tableDir + 16 * i
            if (tag(data.sliceAt(loc + 0), tag))
                return data.getInt(loc + 8)
        }
        return 0
    }

    fun getFontOffsetForIndex(fontCollection: ByteBuffer, index: Int): Int {
        // if it's just a font, there's only one valid index
        if (isFont(fontCollection))
            return if (index == 0) 0 else -1

        // check if it's a TTC
        if (tag(fontCollection, "ttcf")) {
            // version 1?
            if (fontCollection.getLong(4).i == 0x00010000 || fontCollection.getLong(4).i == 0x00020000) {
                val n = fontCollection.getLong(8)
                if (index >= n)
                    return -1
                return fontCollection.getLong(12 + index * 4).i
            }
        }
        return -1
    }

    /** ~stbtt_GetNumberOfFonts_internal */
    fun getNumberOfFonts(fontCollection: ByteBuffer): Int {
        // if it's just a font, there's only one valid font
        if (isFont(fontCollection))
            return 1

        // check if it's a TTC
        if (tag(fontCollection, "ttcf"))
        // version 1?
            if (fontCollection.getInt(4) == 0x00010000 || fontCollection.getInt(4) == 0x00020000)
                return fontCollection.getInt(8)
        return 0
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

    /** ~stbtt_InitFont_internal */
    fun initFont(info: FontInfo, data: ByteBuffer, fontStart: Int): Boolean {

        info.data = data
        info.fontStart = fontStart
        info.cff = Buf()
        val cmap = findTable(data, fontStart, "cmap")       // required
        info.loca = findTable(data, fontStart, "loca") // required
        info.head = findTable(data, fontStart, "head") // required
        info.glyf = findTable(data, fontStart, "glyf") // required
        info.hhea = findTable(data, fontStart, "hhea") // required
        info.hmtx = findTable(data, fontStart, "hmtx") // required
        info.kern = findTable(data, fontStart, "kern") // not required
        info.gpos = findTable(data, fontStart, "GPOS") // not required

        if (cmap == 0 || info.head == 0 || info.hhea == 0 || info.hmtx == 0)
            return false
        if (info.glyf != 0) {
            // required for truetype
            if (info.loca == 0) return false
        } else {
            // initialization for CFF / Type2 fonts (OTF)
            TODO()
//            stbtt__buf b, topdict, topdictidx
//            stbtt_uint32 cstype = 2, charstrings = 0, fdarrayoff = 0, fdselectoff = 0
//            stbtt_uint32 cff
//
//                    cff = findTable(data, fontStart, "CFF ")
//            if (!cff) return 0
//
//            info->fontdicts = stbtt__new_buf(NULL, 0)
//            info->fdselect = stbtt__new_buf(NULL, 0)
//
//            // @TODO this should use size from table (not 512MB)
//            info->cff = stbtt__new_buf(data+cff, 512*1024*1024)
//            b = info->cff
//
//            // read the header
//            stbtt__buf_skip(& b, 2)
//            stbtt__buf_seek(& b, stbtt__buf_get8(&b)) // hdrsize
//
//            // @TODO the name INDEX could list multiple fonts,
//            // but we just use the first one.
//            stbtt__cff_get_index(& b)  // name INDEX
//            topdictidx = stbtt__cff_get_index(& b)
//            topdict = stbtt__cff_index_get(topdictidx, 0)
//            stbtt__cff_get_index(& b)  // string INDEX
//            info->gsubrs = stbtt__cff_get_index(&b)
//
//            stbtt__dict_get_ints(& topdict, 17, 1, &charstrings)
//            stbtt__dict_get_ints(& topdict, 0x100 | 6, 1, &cstype)
//            stbtt__dict_get_ints(& topdict, 0x100 | 36, 1, &fdarrayoff)
//            stbtt__dict_get_ints(& topdict, 0x100 | 37, 1, &fdselectoff)
//            info->subrs = stbtt__get_subrs(b, topdict)
//
//            // we only support Type 2 charstrings
//            if (cstype != 2) return 0
//            if (charstrings == 0) return 0
//
//            if (fdarrayoff) {
//                // looks like a CID font
//                if (!fdselectoff) return 0
//                stbtt__buf_seek(& b, fdarrayoff)
//                info->fontdicts = stbtt__cff_get_index(&b)
//                info->fdselect = stbtt__buf_range(&b, fdselectoff, b.size-fdselectoff)
//            }
//
//            stbtt__buf_seek(& b, charstrings)
//            info->charstrings = stbtt__cff_get_index(&b)
        }

        val t = findTable(data, fontStart, "maxp")
        info.numGlyphs = when (t) {
            0 -> 0xffff
            else -> data.getShort(t + 4).i
        }

        // find a cmap encoding table we understand *now* to avoid searching
        // later. (todo: could make this installable)
        // the same regardless of glyph.
        val numTables = data.getShort(cmap + 2).i
        info.indexMap = 0
        for (i in 0 until numTables) {
            val encodingRecord = cmap + 4 + 8 * i
            // find an encoding we understand:
            when (data.getShort(encodingRecord).i) {
                PlatformID.MICROSOFT.i -> when (data.getShort(encodingRecord + 2).i) {
                    MS_EID.UNICODE_BMP.i, MS_EID.UNICODE_FULL.i -> // MS/Unicode
                        info.indexMap = cmap + data.getInt(encodingRecord + 4)
                }
                PlatformID.UNICODE.i ->
                    // Mac/iOS has these
                    // all the encodingIDs are unicode, so we don't bother to check it
                    info.indexMap = cmap + data.getInt(encodingRecord + 4)
            }
        }
        if (info.indexMap == 0)
            return false

        info.indexToLocFormat = data.getShort(info.head + 50).i
        return true
    }

    fun findGlyphIndex(info: FontInfo, unicodeCodepoint: Int): Int {
        val data = info.data
        val indexMap = info.indexMap

        val format = data.shortUI(indexMap + 0)
        when (format) {
            0 -> { // apple byte encoding
                val bytes = data.shortUI(indexMap + 2)
                if (unicodeCodepoint < bytes - 6)
                    return data.get(indexMap + 6 + unicodeCodepoint).toUInt()
                return 0
            }
            6 -> {
                val first = data.shortUI(indexMap + 6)
                val count = data.shortUI(indexMap + 8)
                if (unicodeCodepoint >= first && unicodeCodepoint < first + count)
                    return data.shortUI(indexMap + 10 + (unicodeCodepoint - first) * 2)
                return 0
            }
            2 -> TODO("high-byte mapping for japanese/chinese/korean")
            4 -> { // standard mapping for windows fonts: binary search collection of ranges
                val segCount = data.shortUI(indexMap + 6) ushr 1
                var searchRange = data.shortUI(indexMap + 8) ushr 1
                var entrySelector = data.shortUI(indexMap + 10)
                val rangeShift = data.shortUI(indexMap + 12) ushr 1

                // do a binary search of the segments
                val endCount = indexMap + 14
                var search = endCount

                if (unicodeCodepoint > 0xffff)
                    return 0

                // they lie from endCount .. endCount + segCount
                // but searchRange is the nearest power of two, so...
                if (unicodeCodepoint >= data.shortUI(search + rangeShift * 2))
                    search += rangeShift * 2

                // now decrement to bias correctly to find smallest
                search -= 2
                while (entrySelector != 0) {
                    searchRange = searchRange ushr 1
                    val end = data.shortUI(search + searchRange * 2)
                    if (unicodeCodepoint > end)
                        search += searchRange * 2
                    --entrySelector
                }
                search += 2

                run {
                    val item = (search - endCount) ushr 1

                    assert(unicodeCodepoint <= data.shortUI(endCount + 2 * item))
                    val start = data.shortUI(indexMap + 14 + segCount * 2 + 2 + 2 * item)
                    if (unicodeCodepoint < start)
                        return 0

                    val offset = data.shortUI(indexMap + 14 + segCount * 6 + 2 + 2 * item)
                    if (offset == 0)
                        return unicodeCodepoint + data.shortUI(indexMap + 14 + segCount * 4 + 2 + 2 * item)

                    return data.shortUI(offset + (unicodeCodepoint - start) * 2 + indexMap + 14 + segCount * 6 + 2 + 2 * item)
                }
            }
            12, 13 -> {
//                stbtt_uint32 ngroups = ttULONG (data + indexMap + 12)
//                stbtt_int32 low, high
//                low = 0; high = (stbtt_int32) ngroups
//                        // Binary search the right group.
//                        while (low < high) {
//                            stbtt_int32 mid = low +((high - low) > > 1) // rounds down, so low <= mid < high
//                            stbtt_uint32 start_char = ttULONG (data + indexMap + 16 + mid * 12)
//                            stbtt_uint32 end_char = ttULONG (data + indexMap + 16 + mid * 12 + 4)
//                            if ((stbtt_uint32) unicode_codepoint < start_char)
//                                high = mid
//                            else if ((stbtt_uint32) unicode_codepoint > end_char)
//                                low = mid + 1
//                            else {
//                                stbtt_uint32 start_glyph = ttULONG (data + indexMap + 16 + mid * 12 + 8)
//                                if (format == 12)
//                                    return start_glyph + unicodeCodepoint - start_char
//                                else // format == 13
//                                    return start_glyph
//                            }
//                        }
                return 0 // not found
            }
        }
        TODO()
    }

//STBTT_DEF int stbtt_GetCodepointShape(const stbtt_fontinfo *info, int unicode_codepoint, stbtt_vertex **vertices)
//{
//    return stbtt_GetGlyphShape(info, stbtt_FindGlyphIndex(info, unicode_codepoint), vertices);
//}

    // setVertex -> Vertex class

    fun getGlyfOffset(info: FontInfo, glyphIndex: Int): Int {
        val g1: Int
        val g2: Int

        assert(info.cff.isEmpty())

        if (glyphIndex >= info.numGlyphs) return -1 // glyph index out of range
        if (info.indexToLocFormat >= 2) return -1 // unknown index->glyph map format

        if (info.indexToLocFormat == 0) {
            g1 = info.glyf + info.data.shortUI(info.loca + glyphIndex * 2) * 2
            g2 = info.glyf + info.data.shortUI(info.loca + glyphIndex * 2 + 2) * 2
        } else {
            g1 = info.glyf + info.data.shortUI(info.loca + glyphIndex * 4)
            g2 = info.glyf + info.data.shortUI(info.loca + glyphIndex * 4 + 4)
        }

        return if (g1 == g2) -1 else g1 // if length is 0, return -1
    }

//static int stbtt__GetGlyphInfoT2(const stbtt_fontinfo *info, int glyph_index, int *x0, int *y0, int *x1, int *y1);

    fun getGlyphBox(info: FontInfo, glyphIndex: Int, box: Vec4i): Boolean {
        if (info.cff.hasRemaining())
            getGlyphInfoT2(info, glyphIndex, box)
        else {
            val g = getGlyfOffset(info, glyphIndex)
            if (g < 0) return false
            box.put(
                    info.data.getShort(g + 2),
                    info.data.getShort(g + 4),
                    info.data.getShort(g + 6),
                    info.data.getShort(g + 8))
        }
        return true
    }

//STBTT_DEF int stbtt_GetCodepointBox(const stbtt_fontinfo *info, int codepoint, int *x0, int *y0, int *x1, int *y1)
//{
//    return stbtt_GetGlyphBox(info, stbtt_FindGlyphIndex(info,codepoint), x0,y0,x1,y1);
//}
//
//STBTT_DEF int stbtt_IsGlyphEmpty(const stbtt_fontinfo *info, int glyph_index)
//{
//    stbtt_int16 numberOfContours;
//    int g;
//    if (info->cff.size)
//    return stbtt__GetGlyphInfoT2(info, glyph_index, NULL, NULL, NULL, NULL) == 0;
//    g = stbtt__GetGlyfOffset(info, glyph_index);
//    if (g < 0) return 1;
//    numberOfContours = ttSHORT(info->data + g);
//    return numberOfContours == 0;
//}

    fun closeShape(vertices: Array<Vertex>, numVertices_: Int, wasOff: Boolean, startOff: Boolean,
                   sx: Int, sy: Int, scx: Int, scy: Int, cx: Int, cy: Int): Int {
        var numVertices = numVertices_
        when {
            startOff -> {
                if (wasOff)
                    vertices[numVertices++].set(V.curve, (cx + scx) shr 1, (cy + scy) shr 1, cx, cy)
                vertices[numVertices++].set(V.curve, sx, sy, scx, scy)
            }
            wasOff -> vertices[numVertices++].set(V.curve, sx, sy, cx, cy)
            else -> vertices[numVertices++].set(V.line, sx, sy, 0, 0)
        }
        return numVertices
    }

    fun getGlyphShapeTT(info: FontInfo, glyphIndex: Int): Array<Vertex> {
        lateinit var endPtsOfContours: ByteBuffer
        val data = info.data.duplicate()
        var vertices: Array<Vertex>? = null
        var numVertices = 0
        val g = getGlyfOffset(info, glyphIndex)

        if (g < 0) return arrayOf()

        val numberOfContours = data.getShort(g).i

        when {
            numberOfContours > 0 -> {
                var flags = 0
                var j = 0
                var wasOff = false
                var startOff = false
                endPtsOfContours = data.sliceAt(g + 10)
                val ins = data.shortUI(g + 10 + numberOfContours * 2)
                val points = data.sliceAt(g + 10 + numberOfContours * 2 + 2 + ins)

                val n = 1 + endPtsOfContours.shortUI(numberOfContours * 2 - 2)

                val m = n + 2 * numberOfContours  // a loose bound on how many vertices we might need
                vertices = Array(m) { Vertex() }

                var nextMove = 0
                var flagCount = 0

                // in first pass, we load uninterpreted data into the allocated array
                // above, shifted to the end of the array so we won't overwrite it when
                // we create our final data starting from the front

                val off = m - n // starting offset for uninterpreted data, regardless of how m ends up being calculated

                // first load flags

                for (i in 0 until n) {
                    if (flagCount == 0) {
                        flags = points.get().toUInt()
                        if (flags has 8)
                            flagCount = points.get().toUInt()
                    } else
                        --flagCount
                    vertices[off + i].type = flags
                }

                // now load x coordinates
                var x = 0
                for (i in 0 until n) {
                    flags = vertices[off + i].type
                    if (flags has 2) {
                        val dx = points.get().toUInt()
                        x += if (flags has 16) dx else -dx // ???
                    } else if (flags hasnt 16)
                        x += points.short.toUInt()
                    vertices[off + i].x = x.s.i // [JVM] Short passage to have, eg 65536 changed to 0
                }

                // now load y coordinates
                var y = 0
                for (i in 0 until n) {
                    flags = vertices[off + i].type
                    if (flags has 4) {
                        val dy = points.get().toUInt()
                        y += if (flags has 32) dy else -dy // ???
                    } else if (flags hasnt 32)
                        y += points.short.toUInt()
                    vertices[off + i].y = y.s.i // [JVM] Short passage to have, eg 65536 changed to 0
                }

                // now convert them to our format
                numVertices = 0
                var cx = 0
                var cy = 0
                var sx = 0
                var sy = 0
                var scx = 0
                var scy = 0
                var i = 0
                while (i < n) {
                    flags = vertices[off + i].type
                    x = vertices[off + i].x
                    y = vertices[off + i].y

                    if (nextMove == i) {
                        if (i != 0)
                            numVertices = closeShape(vertices, numVertices, wasOff, startOff, sx, sy, scx, scy, cx, cy)

                        // now start the new one
                        startOff = flags hasnt 1
                        if (startOff) {
                            // if we start off with an off-curve point, then when we need to find a point on the curve
                            // where we can start, and we need to save some state for when we wraparound.
                            scx = x
                            scy = y
                            if (vertices[off + i + 1].type hasnt 1) {
                                // next point is also a curve point, so interpolate an on-point curve
                                sx = (x + vertices[off + i + 1].x) shr 1
                                sy = (y + vertices[off + i + 1].y) shr 1
                            } else {
                                // otherwise just use the next point as our start point
                                sx = vertices[off + i + 1].x
                                sy = vertices[off + i + 1].y
                                ++i // we're using point i+1 as the starting point, so skip it
                            }
                        } else {
                            sx = x
                            sy = y
                        }
                        vertices[numVertices++].set(V.move, sx, sy, 0, 0)
                        wasOff = false
                        nextMove = 1 + endPtsOfContours.shortUI(j * 2)
                        ++j
                    } else {
                        if (flags hasnt 1) { // if it's a curve
                            if (wasOff) // two off-curve control points in a row means interpolate an on-curve midpoint
                                vertices[numVertices++].set(V.curve, (cx + x) shr 1, (cy + y) shr 1, cx, cy)
                            cx = x
                            cy = y
                            wasOff = true
                        } else {
                            if (wasOff)
                                vertices[numVertices++].set(V.curve, x, y, cx, cy)
                            else
                                vertices[numVertices++].set(V.line, x, y, 0, 0)
                            wasOff = false
                        }
                    }
                    i++
                }
                numVertices = closeShape(vertices, numVertices, wasOff, startOff, sx, sy, scx, scy, cx, cy)
            }
            numberOfContours == -1 -> {
                // Compound shapes.
                var more = true
                val comp = data.sliceAt(g + 10)
                numVertices = 0
                while (more) {
                    val mtx = floatArrayOf(1f, 0f, 0f, 1f, 0f, 0f)

                    val flags = comp.short.i
                    val gIdx = comp.short.i

                    if (flags has 2) { // XY values
                        if (flags has 1) { // shorts
                            mtx[4] = comp.short.f
                            mtx[5] = comp.short.f
                        } else {
                            mtx[4] = comp.get().f
                            mtx[5] = comp.get().f
                        }
                    } else TODO("handle matching point")
                    when {
                        flags has (1 shl 3) -> { // WE_HAVE_A_SCALE
                            mtx[0] = comp.short / 16384f
                            mtx[1] = 0f
                            mtx[2] = 0f
                            mtx[3] = mtx[0]
                        }
                        flags has (1 shl 6) -> { // WE_HAVE_AN_X_AND_YSCALE
                            mtx[0] = comp.short / 16384f
                            mtx[1] = 0f
                            mtx[2] = 0f
                            mtx[3] = comp.short / 16384f
                        }
                        flags has (1 shl 7) -> { // WE_HAVE_A_TWO_BY_TWO
                            mtx[0] = comp.short / 16384f
                            mtx[1] = comp.short / 16384f
                            mtx[2] = comp.short / 16384f
                            mtx[3] = comp.short / 16384f
                        }
                    }

                    // Find transformation scales.
                    val m = sqrt(mtx[0] * mtx[0] + mtx[1] * mtx[1])
                    val n = sqrt(mtx[2] * mtx[2] + mtx[3] * mtx[3])

                    // Get indexed glyph.
                    val compVerts = getGlyphShape(info, gIdx)
                    if (compVerts.isNotEmpty()) {
                        // Transform vertices.
                        for (v in compVerts) {
                            var x = v.x
                            var y = v.y
                            v.x = (m * (mtx[0] * x + mtx[2] * y + mtx[4])).i
                            v.y = (n * (mtx[1] * x + mtx[3] * y + mtx[5])).i
                            x = v.cX
                            y = v.cY
                            v.cX = (m * (mtx[0] * x + mtx[2] * y + mtx[4])).i
                            v.cY = (n * (mtx[1] * x + mtx[3] * y + mtx[5])).i
                        }
                        // Append vertices.
                        val tmp = Array(numVertices + compVerts.size) {
                            if (it < numVertices) vertices!![it]
                            else compVerts[it - numVertices]
                        }
                        vertices = tmp
                        numVertices += compVerts.size
                    }
                    // More components ?
                    more = flags has (1 shl 5)
                }
            }
            numberOfContours < 0 -> TODO("other compound variations?")
            else -> Unit // numberOfCounters == 0, do nothing
        }
        return Array(numVertices) { vertices!![it] }
    }

    // -> Csctx.kt

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
        val fmt = fdSelect.get8()
        if (fmt == 0) {
            // untested
            fdSelect skip glyphIndex
            fdSelector = fdSelect.get8()
        } else if (fmt == 3) {
            val nRanges = fdSelect.get16()
            var start = fdSelect.get16()
            for (i in 0 until nRanges) {
                val v = fdSelect.get8()
                val end = fdSelect.get16()
                if (glyphIndex in start until end) {
                    fdSelector = v
                    break
                }
                start = end
            }
        }
//        if (fdSelector == -1) stbtt__new_buf(NULL, 0) TODO useless?
        return getSubrs(info.cff, info.fontDicts cffIndexGet fdSelector)
    }

    fun runCharString(info: FontInfo, glyphIndex: Int, c: Csctx): Boolean {
        var inHeader = true
        var maskBits = 0
        var subrStackHeight = 0
        var sp = 0
        var b0: Int
        var hasSubrs = false
        var clearStack: Boolean
        val s = FloatArray(48)
        val subrStack = Array(10) { Buf() }
        var subrs = info.subrs

        fun cserr(s: String) = false

        // this currently ignores the initial width value, which isn't needed if we have hmtx
        var b = info.charStrings cffIndexGet glyphIndex
        while (b.pos < b.lim) {
            clearStack = true
            b0 = b.get8()
            when (b0) {
                // @TODO implement hinting
                0x13, // hintmask
                0x14  // cntrmask
                -> {
                    if (inHeader)
                        maskBits += sp / 2 // implicit "vstem"
                    inHeader = false
                    b skip ((maskBits + 7) / 8)
                }

                0x01, // hstem
                0x03, // vstem
                0x12, // hstemhm
                0x17  // vstemhm
                -> maskBits += sp / 2

                // rmoveto
                0x15 -> {
                    inHeader = false
                    if (sp < 2) return cserr("rmoveto stack")
                    c.rMoveTo(s[sp - 2], s[sp - 1])
                }

                // vmoveto
                0x04 -> {
                    inHeader = false
                    if (sp < 1) return cserr("vmoveto stack")
                    c.rMoveTo(0f, s[sp - 1])
                }

                // hmoveto
                0x16 -> {
                    inHeader = false
                    if (sp < 1) return cserr("hmoveto stack")
                    c.rMoveTo(s[sp - 1], 0f)
                }

                // rlineto
                0x05 -> {
                    if (sp < 2) return cserr("rlineto stack")
                    for (i in 0 until (sp - 1) step 2)
                        c.rLineTo(s[i], s[i + 1])
                }

                // hlineto/vlineto and vhcurveto/hvcurveto alternate horizontal and vertical
                // starting from a different place.

                // vlineto
                0x07 -> {
                    if (sp < 1) return cserr("vlineto stack")
                    var i = 0
                    if (i < sp) {
                        c.rLineTo(0f, s[i])
                        i++
                        while (true) {
                            if (i >= sp) break
                            c.rLineTo(s[i], 0f)
                            i++
                            if (i >= sp) break
                            c.rLineTo(0f, s[i])
                            i++
                        }
                    }
                }

                // hlineto
                0x06
                -> {
                    if (sp < 1) return cserr("hlineto stack")
                    var i = 0
                    while (true) {
                        if (i >= sp) break
                        c.rLineTo(s[i], 0f)
                        i++
                        if (i >= sp) break
                        c.rLineTo(0f, s[i])
                        i++
                    }
                }

                // hvcurveto
                0x1F -> {
                    if (sp < 4) return cserr("hvcurveto stack")
                    var i = 0
                    if (i + 3 < sp) {
                        c.rcCurveTo(s[i], 0f, s[i + 1], s[i + 2], if (sp - i == 5) s[i + 4] else 0f, s[i + 3])
                        i += 4
                        while (true) {
                            if (i + 3 >= sp) break
                            c.rcCurveTo(0f, s[i], s[i + 1], s[i + 2], s[i + 3], if (sp - i == 5) s[i + 4] else 0f)
                            i += 4
                            if (i + 3 >= sp) break
                            c.rcCurveTo(s[i], 0f, s[i + 1], s[i + 2], if (sp - i == 5) s[i + 4] else 0f, s[i + 3])
                            i += 4
                        }
                    }
                }
                // vhcurveto
                0x1E -> {
                    if (sp < 4) return cserr("vhcurveto stack")
                    var i = 0
                    while (true) {
                        if (i + 3 >= sp) break
                        c.rcCurveTo(0f, s[i], s[i + 1], s[i + 2], s[i + 3], if (sp - i == 5) s[i + 4] else 0f)
                        i += 4
                        if (i + 3 >= sp) break
                        c.rcCurveTo(s[i], 0f, s[i + 1], s[i + 2], if (sp - i == 5) s[i + 4] else 0f, s[i + 3])
                        i += 4
                    }
                }
                // rrcurveto
                0x08 -> {
                    if (sp < 6) return cserr("rcurveline stack")
                    for (i in 0 until (sp - 5) step 6)
                        c.rcCurveTo(s[i], s[i + 1], s[i + 2], s[i + 3], s[i + 4], s[i + 5])
                }
                // rcurveline
                0x18 -> {
                    if (sp < 8) return cserr("rcurveline stack")
                    var i = 0
                    while (i + 5 < sp - 2)
                        c.rcCurveTo(s[i++], s[i++], s[i++], s[i++], s[i++], s[i++])
                    if (i + 1 >= sp) return cserr("rcurveline stack")
                    c.rLineTo(s[i], s[i + 1])
                }
                // rlinecurve
                0x19
                -> {
                    if (sp < 8) return cserr("rlinecurve stack")
                    var i = 0
                    while (i + 1 < sp - 6)
                        c.rLineTo(s[i++], s[i++])
                    if (i + 5 >= sp) return cserr("rlinecurve stack")
                    c.rcCurveTo(s[i], s[i + 1], s[i + 2], s[i + 3], s[i + 4], s[i + 5])
                }

                0x1A, // vvcurveto
                0x1B  // hhcurveto
                -> {
                    if (sp < 4) return cserr("(vv|hh)curveto stack")
                    var f = 0f
                    var i = 0
                    if (sp has 1) {
                        f = s[i]
                        i++
                    }
                    while (i + 3 < sp) {
                        if (b0 == 0x1B)
                            c.rcCurveTo(s[i], f, s[i + 1], s[i + 2], s[i + 3], 0f)
                        else
                            c.rcCurveTo(f, s[i], s[i + 1], s[i + 2], 0f, s[i + 3])
                        f = 0f
                        i += 4
                    }
                }
                // callsubr
                0x0A -> {
                    if (!hasSubrs) {
                        if (info.fdSelect.hasRemaining())
                            subrs = cidGetGlyphSubrs(info, glyphIndex)
                        hasSubrs = true
                    }
                    // fallthrough
                    if (sp < 1) return cserr("call(g|)subr stack")
                    val v = s[--sp].i
                    if (subrStackHeight >= 10)
                        return cserr("recursion limit")
                    subrStack[subrStackHeight++] = b
                    b = if (b0 == 0x0A) subrs else info.gSubrs
                    b = getSubr(b, v)
                    if (b.isEmpty()) return cserr("subr not found")
                    b.pos = 0
                    clearStack = false
                }
                // callgsubr
                0x1D -> {
                    if (sp < 1) return cserr("call(g|)subr stack")
                    val v = s[--sp].i
                    if (subrStackHeight >= 10)
                        return cserr("recursion limit")
                    subrStack[subrStackHeight++] = b
                    b = if (b0 == 0x0A) subrs else info.gSubrs
                    b = getSubr(b, v)
                    if (b.isEmpty()) return cserr("subr not found")
                    b.pos = 0
                    clearStack = false
                }
                // return
                0x0B
                -> {
                    if (subrStackHeight <= 0) return cserr("return outside subr")
                    b = subrStack[--subrStackHeight]
                    clearStack = false
                }
                // endchar
                0x0E
                -> {
                    c.closeShape()
                    return true
                }
                // two-byte escape
                0x0C
                -> {
                    val dx1: Float
                    val dx2: Float
                    val dx3: Float
                    val dx4: Float
                    val dx5: Float
                    var dx6: Float
                    val dy1: Float
                    val dy2: Float
                    val dy3: Float
                    val dy4: Float
                    val dy5: Float
                    var dy6: Float
                    when (b.get8()) {
                        // @TODO These "flex" implementations ignore the flex-depth and resolution,
                        // and always draw beziers.

                        // hflex
                        0x22 -> {
                            if (sp < 7) return cserr("hflex stack")
                            dx1 = s[0]
                            dx2 = s[1]
                            dy2 = s[2]
                            dx3 = s[3]
                            dx4 = s[4]
                            dx5 = s[5]
                            dx6 = s[6]
                            c.rcCurveTo(dx1, 0f, dx2, dy2, dx3, 0f)
                            c.rcCurveTo(dx4, 0f, dx5, -dy2, dx6, 0f)
                        }
                        // flex
                        0x23 -> {
                            if (sp < 13) return cserr("flex stack")
                            dx1 = s[0]
                            dy1 = s[1]
                            dx2 = s[2]
                            dy2 = s[3]
                            dx3 = s[4]
                            dy3 = s[5]
                            dx4 = s[6]
                            dy4 = s[7]
                            dx5 = s[8]
                            dy5 = s[9]
                            dx6 = s[10]
                            dy6 = s[11]
                            //fd is s[12]
                            c.rcCurveTo(dx1, dy1, dx2, dy2, dx3, dy3)
                            c.rcCurveTo(dx4, dy4, dx5, dy5, dx6, dy6)
                        }
                        // hflex1
                        0x24 -> {
                            if (sp < 9) return cserr("hflex1 stack")
                            dx1 = s[0]
                            dy1 = s[1]
                            dx2 = s[2]
                            dy2 = s[3]
                            dx3 = s[4]
                            dx4 = s[5]
                            dx5 = s[6]
                            dy5 = s[7]
                            dx6 = s[8]
                            c.rcCurveTo(dx1, dy1, dx2, dy2, dx3, 0f)
                            c.rcCurveTo(dx4, 0f, dx5, dy5, dx6, -(dy1 + dy2 + dy5))
                        }
                        // flex1
                        0x25 -> {
                            if (sp < 11) return cserr("flex1 stack")
                            dx1 = s[0]
                            dy1 = s[1]
                            dx2 = s[2]
                            dy2 = s[3]
                            dx3 = s[4]
                            dy3 = s[5]
                            dx4 = s[6]
                            dy4 = s[7]
                            dx5 = s[8]
                            dy5 = s[9]
                            dx6 = s[10]
                            dy6 = s[10]
                            val dx = dx1 + dx2 + dx3 + dx4 + dx5
                            val dy = dy1 + dy2 + dy3 + dy4 + dy5
                            if (abs(dx) > abs(dy))
                                dy6 = -dy
                            else
                                dx6 = -dx
                            c.rcCurveTo(dx1, dy1, dx2, dy2, dx3, dy3)
                            c.rcCurveTo(dx4, dy4, dx5, dy5, dx6, dy6)
                        }
                        else -> TODO("unimplemented")
                    }
                }

                else -> {
                    if (b0 != 255 && b0 != 28 && (b0 < 32 || b0 > 254))
                        return cserr("reserved operator")

                    // push immediate
                    val f = when (b0) {
                        255 -> b.get32().f / 0x10000
                        else -> {
                            b skip -1
                            b.cffInt.f
                        }
                    }
                    if (sp >= 48) return cserr("push stack overflow")
                    s[sp++] = f
                    clearStack = false
                }
            }
            if (clearStack) sp = 0
        }
        return cserr("no endchar")
    }

    fun getGlyphShapeT2(info: FontInfo, glyphIndex: Int): Array<Vertex> {
        // runs the charstring twice, once to count and once to output (to avoid realloc)
        val countCtx = Csctx(true)
        val outputCtx = Csctx(false)
        if (runCharString(info, glyphIndex, countCtx)) {
            outputCtx.vertices = Array(countCtx.numVertices) { Vertex() }
            if (runCharString(info, glyphIndex, outputCtx)) {
                assert(outputCtx.numVertices == countCtx.numVertices)
                return outputCtx.vertices
            }
        }
        return emptyArray()
    }

    fun getGlyphInfoT2(info: FontInfo, glyphIndex: Int, box: Vec4i): Int {
        val c = Csctx(true)
        val r = runCharString(info, glyphIndex, c)
        box.put(
                if (r) c.minX else 0,
                if (r) c.minY else 0,
                if (r) c.maxX else 0,
                if (r) c.maxY else 0)
        return if (r) c.numVertices else 0
    }

    fun getGlyphShape(info: FontInfo, glyphIndex: Int): Array<Vertex> = when {
        info.cff.isEmpty() -> getGlyphShapeTT(info, glyphIndex)
        else -> getGlyphShapeT2(info, glyphIndex)
    }

    /** [JVM]
     *  @return [advanceWidth, leftSideBearing] */
    fun getGlyphHMetrics(info: FontInfo, glyphIndex: Int): Pair<Int, Int> {
        val numOfLongHorMetrics = info.data.shortUI(info.hhea + 34)
        return when {
            glyphIndex < numOfLongHorMetrics -> info.data.shortUI(info.hmtx + 4 * glyphIndex) to info.data.shortUI(info.hmtx + 4 * glyphIndex + 2)
            else -> info.data.shortUI(info.hmtx + 4 * (numOfLongHorMetrics - 1)) to info.data.shortUI(info.hmtx + 4 * numOfLongHorMetrics + 2 * (glyphIndex - numOfLongHorMetrics))
        }
    }

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
//        default: {
//        // There are no other cases.
//        STBTT_assert(0);
//    } break;
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
//        default: {
//        // There are no other cases.
//        STBTT_assert(0);
//    } break;
//    }
//
//    return -1;
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
//            };
//            }
//        }
//        break;
//    };
//
//        default:
//        // TODO: Implement other stuff.
//        break;
//    }
//}
//
//    return 0;
//}
//
//STBTT_DEF int  stbtt_GetGlyphKernAdvance(const stbtt_fontinfo *info, int g1, int g2)
//{
//    int xAdvance = 0;
//
//    if (info->gpos)
//    xAdvance += stbtt__GetGlyphGPOSInfoAdvance(info, g1, g2);
//    else if (info->kern)
//    xAdvance += stbtt__GetGlyphKernInfoAdvance(info, g1, g2);
//
//    return xAdvance;
//}
//
//STBTT_DEF int  stbtt_GetCodepointKernAdvance(const stbtt_fontinfo *info, int ch1, int ch2)
//{
//    if (!info->kern && !info->gpos) // if no kerning table, don't waste time looking up both codepoint->glyphs
//    return 0;
//    return stbtt_GetGlyphKernAdvance(info, stbtt_FindGlyphIndex(info,ch1), stbtt_FindGlyphIndex(info,ch2));
//}
//
//STBTT_DEF void stbtt_GetCodepointHMetrics(const stbtt_fontinfo *info, int codepoint, int *advanceWidth, int *leftSideBearing)
//{
//    stbtt_GetGlyphHMetrics(info, stbtt_FindGlyphIndex(info,codepoint), advanceWidth, leftSideBearing);
//}

    fun getFontVMetrics(info: FontInfo) = IntArray(3) { info.data.getShort(info.hhea + 4 + Short.BYTES * it).i }

//STBTT_DEF int  stbtt_GetFontVMetricsOS2(const stbtt_fontinfo *info, int *typoAscent, int *typoDescent, int *typoLineGap)
//{
//    int tab = stbtt__find_table(info->data, info->fontstart, "OS/2");
//    if (!tab)
//        return 0;
//    if (typoAscent ) *typoAscent  = ttSHORT(info->data+tab + 68);
//    if (typoDescent) *typoDescent = ttSHORT(info->data+tab + 70);
//    if (typoLineGap) *typoLineGap = ttSHORT(info->data+tab + 72);
//    return 1;
//}
//
//STBTT_DEF void stbtt_GetFontBoundingBox(const stbtt_fontinfo *info, int *x0, int *y0, int *x1, int *y1)
//{
//    *x0 = ttSHORT(info->data + info->head + 36);
//    *y0 = ttSHORT(info->data + info->head + 38);
//    *x1 = ttSHORT(info->data + info->head + 40);
//    *y1 = ttSHORT(info->data + info->head + 42);
//}

    fun scaleForPixelHeight(info: FontInfo, height: Float): Float {
        val fheight = info.data.getShort(info.hhea + 4) - info.data.getShort(info.hhea + 6)
        return height / fheight
    }

    fun scaleForMappingEmToPixels(info: FontInfo, pixels: Float): Float {
        val unitsPerEm = info.data.shortUI(info.head + 18)
        return pixels / unitsPerEm
    }

//STBTT_DEF void stbtt_FreeShape(const stbtt_fontinfo *info, stbtt_vertex *v)
//{
//    STBTT_free(v, info->userdata);
//}

    //////////////////////////////////////////////////////////////////////////////
    //
    // antialiasing software rasterizer
    //

    fun getGlyphBitmapBoxSubpixel(font: FontInfo, glyph: Int, scale: Vec2, shift: Vec2 = Vec2(),
                                  box: Vec4i = Vec4i()): Vec4i =
            if (!getGlyphBox(font, glyph, box))
            // e.g. space character
                box(0) as Vec4i // TODO ->glm
            else
            // move to integral bboxes (treating pixels as little squares, what pixels get touched)?
                Vec4i(
                        floor(box[0] * scale.x + shift.x),
                        floor(-box[3] * scale.y + shift.y),
                        ceil(box[2] * scale.x + shift.x),
                        ceil(-box[1] * scale.y + shift.y))

    fun getGlyphBitmapBox(font: FontInfo, glyph: Int, scale: Vec2, box: Vec4i = Vec4i()): Vec4i = getGlyphBitmapBoxSubpixel(font, glyph, scale, box = box)

//STBTT_DEF void stbtt_GetCodepointBitmapBoxSubpixel(const stbtt_fontinfo *font, int codepoint, float scale_x, float scale_y, float shift_x, float shift_y, int *ix0, int *iy0, int *ix1, int *iy1)
//{
//    stbtt_GetGlyphBitmapBoxSubpixel(font, stbtt_FindGlyphIndex(font,codepoint), scale_x, scale_y,shift_x,shift_y, ix0,iy0,ix1,iy1);
//}
//
//STBTT_DEF void stbtt_GetCodepointBitmapBox(const stbtt_fontinfo *font, int codepoint, float scale_x, float scale_y, int *ix0, int *iy0, int *ix1, int *iy1)
//{
//    stbtt_GetCodepointBitmapBoxSubpixel(font, codepoint, scale_x, scale_y,0.0f,0.0f, ix0,iy0,ix1,iy1);
//}

    //////////////////////////////////////////////////////////////////////////////
    //
    //  Rasterizer

    class HHeapChunk {
        var next: HHeapChunk? = null
    }

    class HHeap(var head: Int = -1,
                var firstFree2: ActiveEdge? = null,
                var numRemainingInHeadChunk: Int = 0) {
        lateinit var array: Array<ActiveEdge>
    }

    fun hheapAlloc(hh: HHeap): ActiveEdge =
            if (hh.firstFree2 != null) {
                val p = hh.firstFree2!!
                hh.firstFree2 = p.next
                p
            } else {
                if (hh.numRemainingInHeadChunk == 0) {
                    val count = 2000
                    val c = Array(count) { ActiveEdge() }
//                c.next = hh.head
                    hh.head = 0
                    hh.array = c
                    hh.numRemainingInHeadChunk = count
                }
                --hh.numRemainingInHeadChunk
                hh.array[hh.head + hh.numRemainingInHeadChunk]
            }

    fun hheapFree(hh: HHeap, p: ActiveEdge) {
        p.next = hh.firstFree2
        hh.firstFree2 = p
    }

//static void stbtt__hheap_cleanup(stbtt__hheap *hh, void *userdata)
//{
//    stbtt__hheap_chunk *c = hh->head;
//    while (c) {
//        stbtt__hheap_chunk *n = c->next;
//        STBTT_free(c, userdata);
//        c = n;
//    }
//}

    class Edge {
        var x0 = 0f
        var y0 = 0f
        var x1 = 0f
        var y1 = 0f
        var invert = false
        override fun toString() = "($x0, $y0) ($x1, $y1) invert=$invert"
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

    //#if STBTT_RASTERIZER_VERSION == 1
    const val FIXSHIFT = 10
    const val FIX = 1 shl FIXSHIFT
    const val FIXMASK = FIX - 1

    fun newActive1(hh: HHeap, e: Edge, offX: Int, startPoint: Float): ActiveEdge {
        val z = hheapAlloc(hh)
        val dxdy = (e.x1 - e.x0) / (e.y1 - e.y0)
//        STBTT_assert(z != NULL)
//        if (!z) return z

        // round dx down to avoid overshooting
        if (dxdy < 0)
            z.dx = -floor(FIX * -dxdy).i
        else
            z.dx = floor(FIX * dxdy).i

        z.x = floor(FIX * e.x0 + z.dx * (startPoint - e.y0)).i // use z->dx so when we offset later it's by the same amount
        z.x -= offX * FIX

        z.ey = e.y1
        z.next = null
        z.direction = (if (e.invert) 1 else -1).bitsAsFloat
        return z
    }

    //#elif STBTT_RASTERIZER_VERSION == 2
    fun newActive2(hh: HHeap, e: Edge, offX: Int, startPoint: Float): ActiveEdge {
        val z = hheapAlloc(hh)
        val dxdy = (e.x1 - e.x0) / (e.y1 - e.y0)
        //STBTT_assert(e->y0 <= start_point);
        z.fdx = dxdy
        z.fdy = if (dxdy != 0f) 1f / dxdy else 0f
        z.fx = e.x0 + dxdy * (startPoint - e.y0)
        z.fx -= offX
        z.direction = if (e.invert) 1f else -1f
        z.sy = e.y0
        z.ey = e.y1
        z.next = null
        return z
    }
//#else
//#error "Unrecognized value of STBTT_RASTERIZER_VERSION"
//#endif

//    #if STBTT_RASTERIZER_VERSION == 1

    /** note: this routine clips fills that extend off the edges... ideally this
     *  wouldn't happen, but it could happen if the truetype glyph bounding boxes
     *  are wrong, or if the user supplies a too-small bitmap */
    fun fillActiveEdges1(scanline: PtrByte, len: Int, e_: ActiveEdge, maxWeight: Int) {
        // non-zero winding fill
        var x0 = 0
        var w = 0

        var e: ActiveEdge? = e_
        while (e != null) {
            if (w == 0) {
                // if we're currently at zero, we need to record the edge start point
                x0 = e.x; w += e.direction.asIntBits
            } else {
                val x1 = e.x; w += e.direction.asIntBits
                // if we went to zero, we need to draw
                if (w == 0) {
                    var i = x0 shr FIXSHIFT
                    var j = x1 shr FIXSHIFT

                    if (i < len && j >= 0) {
                        if (i == j) {
                            // x0,x1 are the same pixel, so compute combined coverage
                            scanline[i] = (scanline[i] + ((x1 - x0) * maxWeight shr FIXSHIFT)).b
                        } else {
                            if (i >= 0) // add antialiasing for x0
                                scanline[i] = (scanline[i] + (((FIX - (x0 and FIXMASK)) * maxWeight) shr FIXSHIFT)).b
                            else
                                i = -1 // clip

                            if (j < len) // add antialiasing for x1
                                scanline[j] = (scanline[j] + (((x1 and FIXMASK) * maxWeight) shr FIXSHIFT)).b
                            else
                                j = len // clip

                            while (++i < j) // fill pixels between x0 and x1
                                scanline[i] = (scanline[i] + maxWeight).b
                        }
                    }
                }
            }

            e = e.next
        }
    }

    /** [JVM] signature different for different STBTT_RASTERIZER_VERSION, no need to mentioning version in name */
    fun rasterizeSortedEdges(result: Bitmap, edges: Array<Edge>, n: Int, vSubsample: Int, offX: Int, offY: Int) {
        val hh = HHeap()
        var active: ActiveEdge? = null
        var j = 0
        val maxWeight = 255 / vSubsample  // weight per vertical scanline

        val scanline = PtrByte(if (result.w > 512) result.w else 512)

        var y = offY * vSubsample
        var e = 0
        edges[e + n].y0 = (offY + result.h) * vSubsample.f + 1

        while (j < result.h) {
//        STBTT_memset(scanline, 0, result->w)
            for (s in 0 until vSubsample) { // vertical subsample index
                // find center of pixel for this scanline
                val scanY = y + 0.5f
                var step = active
                var prev: ActiveEdge? = null

                // update all active edges;
                // remove all active edges that terminate before the center of this scanline
                while (step != null) {
                    val z = step
                    if (z.ey <= scanY) {
                        step = z.next // delete from list
                        prev?.next = step
                        if (z === active) active = active.next
                        assert(z.direction.asIntBits != 0)
                        z.direction = 0.bitsAsFloat
                        hheapFree(hh, z)
                    } else {
                        z.x += z.dx // advance to position for current scanline
                        prev = step
                        step = step.next // advance through list
                    }
                }

                // resort the list if needed
                while (true) {
                    var changed = false
                    step = active
                    while (step?.next != null) {
                        if (step.x > step.next!!.x) {
                            val t = step
                            val q = t.next!!

                            t.next = q.next
                            q.next = t
                            step = q
                            changed = true
                        }
                        prev = step
                        step = step.next
                    }
                    if (!changed) break
                }

                // insert all edges that start before the center of this scanline -- omit ones that also end on this scanline
                while (edges[e].y0 <= scanY) {
                    if (edges[e].y1 > scanY) {
                        val z = newActive1(hh, edges[e], offX, scanY)
                        // find insertion point
                        when {
                            active == null -> active = z
                            z.x < active.x -> {
                                // insert at front
                                z.next = active
                                active = z
                            }
                            else -> {
                                // find thing to insert AFTER
                                var p: ActiveEdge = active
                                while (p.next != null && p.next!!.x < z.x)
                                    p = p.next!!
                                // at this point, p->next->x is NOT < z->x
                                z.next = p.next
                                p.next = z
                            }
                        }
                    }
                    ++e
                }

                // now process all active edges in XOR fashion
                active?.let { fillActiveEdges1(scanline, result.w, it, maxWeight) }

                ++y
            }
//            STBTT_memcpy(result->pixels+j * result->stride, scanline, result->w)
            ++j
        }
//
//        stbtt__hheap_cleanup(& hh, userdata)
//
//        if (scanline != scanlineData)
//            STBTT_free(scanline, userdata)
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
                        val height = sy1 - sy0
                        assert(x in 0 until len)
                        scanline[x] += e.direction * (1 - ((xTop - x) + (xBottom - x)) / 2) * height
                        scanlineFill[x] += e.direction * height // everything right of this pixel is filled
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

                        val x1 = xTop.i
                        val x2 = xBottom.i
                        // compute intersection with y axis at x1+1
                        var yCrossing = (x1 + 1 - x0) * dy + yTop

                        val sign = e.direction
                        // area of the rectangle covered from y0..y_crossing
                        var area = sign * (yCrossing - sy0)
                        // area of the triangle (x_top,y0), (x+1,y0), (x+1,y_crossing)
                        scanline[x1] += area * (1 - ((xTop - x1) + (x1 + 1 - x1)) / 2)

                        val step = sign * dy
                        for (x in x1 + 1 until x2) {
                            scanline[x] += area + step / 2
                            area += step
                        }
                        yCrossing += dy * (x2 - (x1 + 1))

                        assert(abs(area) <= 1.01f)

                        scanline[x2] += area + sign * (1 - ((x2 - x2) + (xBottom - x2)) / 2) * (sy1 - yCrossing)

                        scanlineFill[x2] += sign * (sy1 - sy0)
                    }
                } else {
                    // if edge goes outside of box we're drawing, we require
                    // clipping logic. since this does not match the intended use
                    // of this library, we use a different, very slow brute
                    // force implementation
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

    /** directly AA rasterize edges w/o supersampling
     *  [JVM] signature different for different STBTT_RASTERIZER_VERSION, no need to mentioning version in name */
    fun rasterizeSortedEdges(result: Bitmap, edges: Array<Edge>, n: Int, offX: Int, offY: Int) {
        val hh = HHeap()
        var active: ActiveEdge? = null

        val scanline = PtrFloat(if (result.w > 64) result.w * 2 + 1 else 129)

        val scanline2 = scanline + result.w

        var y = offY
        var e = 0
        edges[e + n].y0 = (offY + result.h).f + 1

        var j = 0
        while (j < result.h) {
            // find center of pixel for this scanline
            val scanYTop = y + 0f
            val scanYBottom = y + 1f
            var step = active
            var prev: ActiveEdge? = null

            scanline.fill(0f, result.w)
            scanline2.fill(0f, result.w + 1)

            // update all active edges;
            // remove all active edges that terminate before the top of this scanline
            while (step != null) {
                val z = step
                if (z.ey <= scanYTop) {
                    step = z.next // delete from list
                    prev?.next = step
                    if (z === active) active = active.next
                    assert(z.direction != 0f)
                    z.direction = 0f
                    hheapFree(hh, z)
                } else { // advance through list
                    prev = step
                    step = z.next
                }
            }

            // insert all edges that start before the bottom of this scanline
            while (edges[e].y0 <= scanYBottom) {
                if (edges[e].y0 != edges[e].y1) {
                    val z = newActive2(hh, edges[e], offX, scanYTop)
//                    if (z != NULL) {
                    if (j == 0 && offY != 0)
                        if (z.ey < scanYTop)
                        // this can happen due to subpixel positioning and some kind of fp rounding error i think
                            z.ey = scanYTop
                    assert(z.ey >= scanYTop) { "if we get really unlucky a tiny bit of an edge can be out of bounds" }
                    // insert at front
                    z.next = active
                    active = z
//                    }
                }
                ++e
            }

            // now process all active edges
            active?.let {
                fillActiveEdgesNew(scanline, scanline2 + 1, result.w, it, scanYTop)
            }
            run {
                var sum = 0f
                for (i in 0 until result.w) {
                    sum += scanline2[i]
                    var k = scanline[i] + sum
                    k = abs(k) * 255 + 0.5f
                    val m = k.i
                    result.pixels[j * result.stride + i] = if (m > 255) 255 else m
                }
            }
            // advance all the edges
            step = active
            while (step != null) {
                val z = step
                z.fx += z.fdx // advance to position for current scanline
                prev = step // advance through list
                step = step.next // advance through list
            }

            ++y
            ++j
        }
//        stbtt__hheap_cleanup(& hh, userdata)
//
//        if (scanline != scanlineData)
//            STBTT_free(scanline, userdata)
    }
//#else
//#error "Unrecognized value of STBTT_RASTERIZER_VERSION"
//#endif

    fun compare(a: Edge, b: Edge): Int = (a.y0 < b.y0).i

    fun sortEdgesInsSort(p: Array<Edge>, n: Int) {
        for (i in 1 until n) {
            val t = p[i]
            val a = t
            var j = i
            while (j > 0) {
                val b = p[j - 1]
                val c = compare(a, b)
                if (c == 0) break
                p[j] = p[j - 1]
                --j
            }
            if (i != j)
                p[j] = t
        }
    }

    fun sortEdgesQuicksort(a: Array<Edge>, p_: Int, n_: Int) {
        var p = p_
        var n = n_
        /* threshold for transitioning to insertion sort */
        while (n > 12) {
            lateinit var t: Edge
            /* compute median of three */
            val m = n shr 1
            val c01 = compare(a[p + 0], a[p + m])
            val c12 = compare(a[p + m], a[p + n - 1])
            /* if 0 >= mid >= end, or 0 < mid < end, then use mid */
            if (c01 != c12) {
                /* otherwise, we'll need to swap something else to middle */
                val c = compare(a[p + 0], a[p + n - 1])
                /* 0>mid && mid<n:  0>n => n; 0<n => 0 */
                /* 0<mid && mid>n:  0>n => 0; 0<n => n */
                val z = if (c == c12) 0 else n - 1
                t = a[p + z]
                a[p + z] = a[p + m]
                a[p + m] = t
            }
            /* now p[m] is the median-of-three */
            /* swap it to the beginning so it won't move around */
            t = a[p + 0]
            a[p + 0] = a[p + m]
            a[p + m] = t

            /* partition loop */
            var i = 1
            var j = n - 1
            while (true) {
                /* handling of equality is crucial here */
                /* for sentinels & efficiency with duplicates */
                while (true) if (compare(a[p + i++], a[p + 0]) == 0) break
                while (true) if (compare(a[p + 0], a[p + j--]) == 0) break
                /* make sure we haven't crossed */
                if (i >= j) break
                t = a[p + i]
                a[p + i] = a[p + j]
                a[p + j] = t

                ++i
                --j
            }
            /* recurse on smaller side, iterate on larger */
            if (j < (n - i)) {
                sortEdgesQuicksort(a, p, j)
                p += i
                n -= i
            } else {
                sortEdgesQuicksort(a, p + i, n - i)
                n = j
            }
        }
    }

    fun sortEdges(p: Array<Edge>, n: Int) {
        sortEdgesQuicksort(p, 0, n)
        sortEdgesInsSort(p, n)
    }

//typedef struct [JVM] Vec2
//{
//    float x,y;
//} stbtt__point;

    fun rasterize(result: Bitmap, pts: Array<Vec2>, wCount: ArrayList<Int>, scale: Vec2, shift: Vec2,
                  offX: Int, offY: Int, invert: Boolean) {
        val yScaleInv = if (invert) -scale.y else scale.y
        val vsubsample = when (RASTERIZER_VERSION) {
            1 -> if (result.h < 8) 15 else 5
            2 -> 1
            else -> error("Unrecognized value of STBTT_RASTERIZER_VERSION")
        }
        // vsubsample should divide 255 evenly; otherwise we won't reach full opacity

        // now we have to blow out the windings into explicit edge lists

        val e = Array(wCount.sum() + 1) { Edge() } // add an extra one as a sentinel

        var n = 0
        var m = 0
        for (w in wCount) {
            val p = m
            m += w
            var j = w - 1
            var k = 0
            while (k < w) {
                var a = k
                var b = j
                // skip the edge if horizontal
                if (pts[p + j].y == pts[p + k].y) {
                    j = k++
                    continue
                }
                // add edge from j to k to the list
                e[n].invert = false
                if (if (invert) pts[p + j].y > pts[p + k].y else pts[p + j].y < pts[p + k].y) {
                    e[n].invert = true
                    a = j; b = k
                }
                e[n].x0 = pts[p + a].x * scale.x + shift.x
                e[n].y0 = (pts[p + a].y * yScaleInv + shift.y) * vsubsample
                e[n].x1 = pts[p + b].x * scale.x + shift.x
                e[n].y1 = (pts[p + b].y * yScaleInv + shift.y) * vsubsample
                ++n
                j = k++
            }
        }

        // now sort the edges by their highest point (should snap to integer, and then by x)
        //STBTT_sort(e, n, sizeof(e[0]), stbtt__edge_compare);
        sortEdges(e, n)

        // now, traverse the scanlines and find the intersections on each scanline, use xor winding rule
        rasterizeSortedEdges(result, e, n, offX, offY)
    }

    fun addPoint(points: Array<Vec2>?, n: Int, x: Float, y: Float) {
        if (points == null) return // during first pass, it's unallocated
        points[n].x = x
        points[n].y = y
    }

    /** tessellate until threshold p is happy... @TODO warped to compensate for non-linear stretching
     *  [JVM]
     *  @returns numPoints */
    fun tesselateCurve(points: Array<Vec2>?, numPoints_: Int, x0: Float, y0: Float, x1: Float, y1: Float,
                       x2: Float, y2: Float, objspaceFlatnessSquared: Float, n: Float): Int {
        var numPoints = numPoints_
        // midpoint
        val mx = (x0 + 2 * x1 + x2) / 4
        val my = (y0 + 2 * y1 + y2) / 4
        // versus directly drawn line
        val dx = (x0 + x2) / 2 - mx
        val dy = (y0 + y2) / 2 - my
        if (n > 16) // 65536 segments on one curve better be enough!
            return numPoints
        if (dx * dx + dy * dy > objspaceFlatnessSquared) { // half-pixel error allowed... need to be smaller if AA
            numPoints = tesselateCurve(points, numPoints, x0, y0, (x0 + x1) / 2.0f, (y0 + y1) / 2.0f, mx, my, objspaceFlatnessSquared, n + 1)
            numPoints = tesselateCurve(points, numPoints, mx, my, (x1 + x2) / 2.0f, (y1 + y2) / 2.0f, x2, y2, objspaceFlatnessSquared, n + 1)
        } else addPoint(points, numPoints++, x2, y2)
        return numPoints
    }

    /** [JVM]
     *  @return numPoints */
    fun tesselateCubic(points: Array<Vec2>?, numPoints_: Int, x0: Float, y0: Float, x1: Float, y1: Float,
                       x2: Float, y2: Float, x3: Float, y3: Float, objspaceFlatnessSquared: Float, n: Int): Int {
        var numPoints = numPoints_
        // @TODO this "flatness" calculation is just made-up nonsense that seems to work well enough
        val dx0 = x1 - x0
        val dy0 = y1 - y0
        val dx1 = x2 - x1
        val dy1 = y2 - y1
        val dx2 = x3 - x2
        val dy2 = y3 - y2
        val dx = x3 - x0
        val dy = y3 - y0
        val longLen = sqrt(dx0 * dx0 + dy0 * dy0) + sqrt(dx1 * dx1 + dy1 * dy1) + sqrt(dx2 * dx2 + dy2 * dy2)
        val shortLen = sqrt(dx * dx + dy * dy)
        val flatnessSquared = longLen * longLen - shortLen * shortLen

        if (n > 16) // 65536 segments on one curve better be enough!
            return numPoints

        if (flatnessSquared > objspaceFlatnessSquared) {
            val x01 = (x0 + x1) / 2
            val y01 = (y0 + y1) / 2
            val x12 = (x1 + x2) / 2
            val y12 = (y1 + y2) / 2
            val x23 = (x2 + x3) / 2
            val y23 = (y2 + y3) / 2

            val xa = (x01 + x12) / 2
            val ya = (y01 + y12) / 2
            val xb = (x12 + x23) / 2
            val yb = (y12 + y23) / 2

            val mx = (xa + xb) / 2
            val my = (ya + yb) / 2

            numPoints = tesselateCubic(points, numPoints, x0, y0, x01, y01, xa, ya, mx, my, objspaceFlatnessSquared, n + 1)
            numPoints = tesselateCubic(points, numPoints, mx, my, xb, yb, x23, y23, x3, y3, objspaceFlatnessSquared, n + 1)
        } else addPoint(points, numPoints++, x3, y3)
        return numPoints
    }

    /** returns the contours */
    fun flattenCurves(vertices: Array<Vertex>, objspaceFlatness: Float, contourLengths: ArrayList<Int>): Array<Vec2> {
        var points: Array<Vec2>? = null
        var numPoints = 0

        val objspaceFlatnessSquared = objspaceFlatness * objspaceFlatness
//        int i,
        var start = 0

        // count how many "moves" there are to get the contour count
        var n = vertices.filter { it.type == V.move.i }.count()

        if (n == 0) return emptyArray()

        repeat(n) { contourLengths += 0 }

        // make two passes through the points so we don't need to realloc
        for (pass in 0..1) {
            var x = 0f
            var y = 0f
            if (pass == 1)
                points = Array(numPoints) { Vec2() }
            numPoints = 0
            n = -1
            for (v in vertices)
                when (v.type) {
                    V.move.i -> { // start the next contour
                        if (n >= 0)
                            contourLengths[n] = numPoints - start
                        ++n
                        start = numPoints

                        x = v.x.f; y = v.y.f
                        addPoint(points, numPoints++, x, y)
                    }
                    V.line.i -> {
                        x = v.x.f; y = v.y.f
                        addPoint(points, numPoints++, x, y)
                    }
                    V.curve.i -> {
                        numPoints = tesselateCurve(points, numPoints,
                                x, y,
                                v.cX.f, v.cY.f,
                                v.x.f, v.y.f,
                                objspaceFlatnessSquared, 0f)
                        x = v.x.f; y = v.y.f
                    }
                    V.cubic.i -> {
                        numPoints = tesselateCubic(points, numPoints, x, y,
                                v.cX.f, v.cY.f,
                                v.cX1.f, v.cY1.f,
                                v.x.f, v.y.f,
                                objspaceFlatnessSquared, 0)
                        x = v.x.f; y = v.y.f
                    }
                }
            contourLengths[n] = numPoints - start
        }

        return points!!
    }

    fun rasterize(result: Bitmap, flatnessInPixels: Float, vertices: Array<Vertex>, scale: Vec2,
                  shift: Vec2, xOff: Int, yOff: Int, invert: Boolean) {
        val s = if (scale.x > scale.y) scale.y else scale.x
        val windingLengths = ArrayList<Int>()
        val windings = flattenCurves(vertices, flatnessInPixels / s, windingLengths)
        if (windings.isNotEmpty())
            rasterize(result, windings, windingLengths, scale, shift, xOff, yOff, invert)
    }

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

    fun makeGlyphBitmapSubpixel(info: FontInfo, output: ByteBuffer, outW: Int, outH: Int, outStride: Int, scale: Vec2, shift: Vec2 = Vec2(), glyph: Int) {
        val vertices = getGlyphShape(info, glyph)

        val (ix0, iy0, _, _) = getGlyphBitmapBoxSubpixel(info, glyph, scale, shift)
        val gbm = Bitmap(outW, outH, outStride, output.duplicate())

        if (gbm.w != 0 && gbm.h != 0)
            rasterize(gbm, 0.35f, vertices, scale, shift, ix0, iy0, true)
    }

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

    fun packBegin(spc: PackContext, pixels: ByteBuffer?, pw: Int, ph: Int, strideInBytes: Int, padding: Int): Boolean {
        val context = rp.Context()
        val numNodes = pw - padding
        val nodes = Array(numNodes) { rp.Node() }

        spc.width = pw
        spc.height = ph
        pixels?.let { spc.pixels = it }
        spc.packInfo = context
        spc.nodes = nodes
        spc.padding = padding
        spc.strideInBytes = if (strideInBytes != 0) strideInBytes else pw
        spc.oversample put 1
        spc.skipMissing = false

        rp.initTarget(context, pw - padding, ph - padding, nodes)

        pixels?.fill(0, 0, pw * ph) // background of 0 around pixels

        return true
    }

//STBTT_DEF void stbtt_PackEnd  (stbtt_pack_context *spc)
//{
//    STBTT_free(spc->nodes    , spc->user_allocator_context);
//    STBTT_free(spc->pack_info, spc->user_allocator_context);
//}
//
//STBTT_DEF void stbtt_PackSetOversampling(stbtt_pack_context *spc, unsigned int h_oversample, unsigned int v_oversample)
//{
//    STBTT_assert(h_oversample <= STBTT_MAX_OVERSAMPLE);
//    STBTT_assert(v_oversample <= STBTT_MAX_OVERSAMPLE);
//    if (h_oversample <= STBTT_MAX_OVERSAMPLE)
//        spc->h_oversample = h_oversample;
//    if (v_oversample <= STBTT_MAX_OVERSAMPLE)
//        spc->v_oversample = v_oversample;
//}
//
//STBTT_DEF void stbtt_PackSetSkipMissingCodepoints(stbtt_pack_context *spc, int skip)
//{
//    spc->skip_missing = skip;
//}

    var OVER_MASK = MAX_OVERSAMPLE - 1

    fun hPrefilter(pixels: ByteBuffer, w: Int, h: Int, strideInBytes: Int, kernelWidth: Int) {
        val buffer = ByteBuffer.allocate(MAX_OVERSAMPLE)
        val safeW = w - kernelWidth
        for (j in 0 until h) {
            var p = 0
            var total = 0

            // make kernel_width a constant in common cases so compiler can optimize out the divide
            var i = 0
            while (i <= safeW) {
                total += pixels[p + i] - buffer[i and OVER_MASK]
                buffer[(i + kernelWidth) and OVER_MASK] = pixels[p + i]
                pixels[p + i++] = total / kernelWidth
            }

            while (i < w) {
                assert(pixels[p + i] == 0.b)
                total -= buffer[i and OVER_MASK]
                pixels[p + i++] = total / kernelWidth
            }

            p += strideInBytes
        }
    }

    fun vPrefilter(pixels: ByteBuffer, w: Int, h: Int, strideInBytes: Int, kernelWidth: Int) {
        val buffer = ByteBuffer.allocate(MAX_OVERSAMPLE)
        val safeH = h - kernelWidth
        for (j in 0 until w) {
            var p = 0
            var total = 0

            // make kernel_width a constant in common cases so compiler can optimize out the divide
            var i = 0
            while (i <= safeH) {
                total += pixels[p + i * strideInBytes] - buffer[i and OVER_MASK]
                buffer[(i + kernelWidth) and OVER_MASK] = pixels[p + i * strideInBytes]
                pixels[p + i++ * strideInBytes] = total / kernelWidth
            }

            while (i < h) {
                assert(pixels[p + i * strideInBytes] == 0.b)
                total -= buffer[i and OVER_MASK]
                pixels[p + i++ * strideInBytes] = total / kernelWidth
            }

            p++
        }
    }

    fun oversampleShift(oversample: Vec2i): Vec2 = Vec2 {
        if (oversample[it] == 0)
            0f

        // The prefilter is a box filter of width "oversample",
        // which shifts phase by (oversample - 1)/2 pixels in
        // oversampled space. We want to shift in the opposite
        // direction to counter this.
        else -(oversample[it] - 1) / (2f * oversample[it])
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

    /** rects array must be big enough to accommodate all characters in the given ranges */
    fun packFontRangesRenderIntoRects(spc: PackContext, info: FontInfo, ranges: Array<PackRange>, rects: Array<rp.Rect>): Boolean {
        var missingGlyph = -1
        var returnValue = true

        // save current values
        val oldOver = Vec2i(spc.oversample)

        var k = 0
        for (range in ranges) {
            val fh = range.fontSize
            val scale = if (fh > 0f) scaleForPixelHeight(info, fh) else scaleForMappingEmToPixels(info, -fh)
            spc.oversample put range.oversample
            val recip = 1f / spc.oversample
            val sub = oversampleShift(spc.oversample)
            for (j in 0 until range.numChars) {
                val r = rects[k]
                when {
                    r.wasPacked != 0 && r.w != 0 && r.h != 0 -> {
                        val bc = range.chardataForRange[j]
                        val codepoint = range.arrayOfUnicodeCodepoints?.get(j) ?: range.firstUnicodeCodepointInRange
                        val glyph = findGlyphIndex(info, codepoint)
                        val pad = spc.padding

                        // pad on left and top
                        r.x += pad
                        r.y += pad
                        r.w -= pad
                        r.h -= pad
                        val (advance, lsb) = getGlyphHMetrics(info, glyph)
                        val (x0, y0, x1, y1) = getGlyphBitmapBox(info, glyph, scale * Vec2(spc.oversample))
                        makeGlyphBitmapSubpixel(info, spc.pixels.sliceAt(r.x + r.y * spc.strideInBytes),
                                r.w - spc.oversample.x + 1, r.h - spc.oversample.y + 1,
                                spc.strideInBytes,
                                scale * Vec2(spc.oversample), // TODO -> glm
                                glyph = glyph)
                        if (spc.oversample.x > 1)
                            hPrefilter(spc.pixels.sliceAt(r.x + r.y * spc.strideInBytes),
                                    r.w, r.h, spc.strideInBytes, spc.oversample.x)

                        if (spc.oversample.y > 1)
                            vPrefilter(spc.pixels.sliceAt(r.x + r.y * spc.strideInBytes),
                                    r.w, r.h, spc.strideInBytes, spc.oversample.y)

                        bc.x0 = r.x
                        bc.y0 = r.y
                        bc.x1 = r.x + r.w
                        bc.y1 = r.y + r.h
                        bc.xAdvance = scale * advance
                        bc.xOff = x0 * recip.x + sub.x
                        bc.yOff = y0 * recip.y + sub.y
                        bc.xOff2 = (x0 + r.w) * recip.x + sub.x
                        bc.yOff2 = (y0 + r.h) * recip.y + sub.y
                    }
                    else -> returnValue = false // if any fail, report failure
                }
                ++k
            }
        }

        // restore original values
        spc.oversample put oldOver

        return returnValue
    }

//STBTT_DEF void stbtt_PackFontRangesPackRects(stbtt_pack_context *spc, stbrp_rect *rects, int num_rects)
//{
//    stbrp_pack_rects((stbrp_context *) spc->pack_info, rects, num_rects);
//}
//
//STBTT_DEF int stbtt_PackFontRanges(stbtt_pack_context *spc, const unsigned char *fontdata, int font_index, stbtt_pack_range *ranges, int num_ranges)
//{
//    stbtt_fontinfo info;
//    int i,j,n, return_value = 1;
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

    fun getPackedQuad(charData: Array<PackedChar>, p: Vec2i, charIndex: Int, pos: Vec2 = Vec2(), q: AlignedQuad, alignToInteger: Boolean = false) {
        val ip = 1f / Vec2(p)
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

        q.s0 = b.x0 * ip.x
        q.t0 = b.y0 * ip.y
        q.s1 = b.x1 * ip.x
        q.t1 = b.y1 * ip.y

        pos.x += b.xAdvance
    }

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
//    orig[0] = x;
//    orig[1] = y;
//
//    // make sure y never passes through a vertex of the shape
//    y_frac = (float) STBTT_fmod(y, 1.0f);
//    if (y_frac < 0.01f)
//        y += 0.01f;
//    else if (y_frac > 0.99f)
//        y -= 0.01f;
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
//// x^3 + c*x^2 + b*x + a = 0
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
//        // check against every point here rather than inside line/curve primitives -- @TODO: wrong if multiple 'moves' in a row produce a garbage point, and given culling, probably more efficient to do within line/curve
//        float dist2 = (x0-sx)*(x0-sx) + (y0-sy)*(y0-sy);
//        if (dist2 < min_dist*min_dist)
//            min_dist = (float) STBTT_sqrt(dist2);
//
//        if (verts[i].type == STBTT_vline) {
//            float x1 = verts[i-1].x*scale_x, y1 = verts[i-1].y*scale_y;
//
//            // coarse culling against bbox
//            //if (sx > STBTT_min(x0,x1)-min_dist && sx < STBTT_max(x0,x1)+min_dist &&
//            //    sy > STBTT_min(y0,y1)-min_dist && sy < STBTT_max(y0,y1)+min_dist)
//            float dist = (float) STBTT_fabs((x1-x0)*(y0-sy) - (y1-y0)*(x0-sx)) * precompute[i];
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
//                float res[3],px,py,t,it;
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
