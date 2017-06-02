package imgui

import glm.i
import glm.plus
import glm.s
import glm.shl
import glm.vec2.Vec2
import glm.vec2.Vec2b
import glm.vec2.Vec2i

object TrueType {

    fun getFontOffsetForIndex(data: CharArray, index: Int) = getFontOffsetForIndex_internal(data, index)

    fun getFontOffsetForIndex_internal(fontCollection: CharArray, index: Int): Int {
        // if it's just a font, there's only one valid index
        if (isFont(fontCollection))
            return if (index == 0) 0 else -1

        // check if it's a TTC
        if (tag(fontCollection, "ttcf"))
        // version 1?
            if (fontCollection.i(4) == 0x00010000 || fontCollection.i(4) == 0x00020000) {
                val n = fontCollection.i(8)
                if (index >= n)
                    return -1
                return fontCollection.i(12 + index * 4)
            }
        return -1
    }

    fun isFont(font: CharArray) =
            // check the version number
            tag4(font, '1'.i, 0, 0, 0) or // TrueType 1
                    tag(font, "typ1") or // TrueType with type 1 font -- we don't support this!
                    tag(font, "OTTO") or // OpenType with CFF
                    tag4(font, 0, 1, 0, 0) or // OpenType 1.0
                    tag(font, "true")  // Apple specification for TrueType fonts

    fun tag4(p: CharArray, c0: Int, c1: Int, c2: Int, c3: Int) = p[0].i == c0 && p[1].i == c1 && p[2].i == c2 && p[3].i == c3
    fun tag(p: CharArray, str: String) = tag4(p, str[0].i, str[1].i, str[2].i, str[3].i)

    fun CharArray.s(p: Int) = (this[p].i * 256 + this[p + 1]).s
    fun CharArray.i(p: Int) = ((this[p] shl 24) + (this[p + 1] shl 16) + (this[p + 2] shl 8) + this[p + 3])

//    fun initFont(stbtt_fontinfo *info, const unsigned char *data, int offset)
//    {
//        return stbtt_InitFont_internal(info, (unsigned char *) data, offset);
//    }

    /** The following structure is defined publically so you can declare one on the stack or as a global or etc, but you
     *  should treat it as opaque.  */
    class Fontinfo {
//        void           * userdata;
//        unsigned char  * data;              // pointer to .ttf file
//        int              fontstart;         // offset of start of font
//
//        int numGlyphs;                     // number of glyphs, needed for range checking
//
//        int loca,head,glyf,hhea,hmtx,kern; // table locations as offset from start of .ttf
//        int index_map;                     // a cmap mapping for our chosen character encoding
//        int indexToLocFormat;              // format needed to map from glyph index to glyph
//
//        stbtt__buf cff;                    // cff font data
//        stbtt__buf charstrings;            // the charstring index
//        stbtt__buf gsubrs;                 // global charstring subroutines index
//        stbtt__buf subrs;                  // private charstring subroutines index
//        stbtt__buf fontdicts;              // array of font dicts
//        stbtt__buf fdselect;               // map from glyph to fontdict
    }

    class PackRange {
        var fontSize = 0f
        /** if non-zero, then the chars are continuous, and this is the first codepoint */
        var firstUnicodeCodepointInRange = 0
        /** if non-zero, then this is an array of unicode codepoints    */
        val array_of_unicode_codepoints = mutableListOf<Int>()
        var numChars = 0
        /** output  */
        val charDataForRange= mutableListOf<PackedChar>()
        // don't set these, they're used internally
        var oversample = Vec2b()
    }

    //////////////////////////////////////////////////////////////////////////////
    //
    // NEW TEXTURE BAKING API
    //
    // This provides options for packing multiple fonts into one atlas, not
    // perfectly but better than nothing.

    class PackedChar    {
        /** coordinates of bbox in bitmap   */
        var _0 = Vec2i()
        /** coordinates of bbox in bitmap   */
        var _1 = Vec2i()

        var off = Vec2()
        var xadvance = 0f
        var off2 = Vec2()
    }
}

