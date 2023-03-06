package imgui.stb_

import imgui.stb_.TrueType.ulong
import imgui.stb_.TrueType.ushort

//////////////////////////////////////////////////////////////////////////////
//
// FONT LOADING
//
//

// This function will determine the number of fonts in a font file.  TrueType
// collection (.ttc) files may contain multiple fonts, while TrueType font
// (.ttf) files only contain one font. The number of fonts can be used for
// indexing with the previous function where the index is between zero and one
// less than the total fonts. If an error occurs, -1 is returned.
fun getNumberOfFonts(fontCollection: UByteArray): Int {
    // if it's just a font, there's only one valid font
    if (TrueType.isFont(fontCollection))
        return 1

    // check if it's a TTC
    if (TrueType.tag(fontCollection, "ttcf"))
    // version 1?
        if (fontCollection.ulong(4) == 0x00010000u || fontCollection.ulong(4) == 0x00020000u)
            return fontCollection.ulong(8).i
    return 0
}

// Each .ttf/.ttc file may have more than one font. Each font has a sequential
// index number starting from 0. Call this function to get the font offset for
// a given index; it returns -1 if the index is out of range. A regular .ttf
// file will only define one font and it always be at offset 0, so it will
// return '0' for index 0, and -1 for all other indices.
fun getFontOffsetForIndex(fontCollection: UByteArray, index: Int): Int {
    // if it's just a font, there's only one valid index
    if (TrueType.isFont(fontCollection))
        return if (index == 0) 0 else -1

    // check if it's a TTC
    if (TrueType.tag(fontCollection, "ttcf")) {
        // version 1?
        if (fontCollection.ulong(4) == 0x00010000u || fontCollection.ulong(4) == 0x00020000u) {
            val n = fontCollection.ulong(8)
            if (index >= n.i)
                return -1
            return fontCollection.ulong(12 + index * 4).i
        }
    }
    return -1
}


// The following structure is defined publicly so you can declare one on
// the stack or as a global or etc, but you should treat it as opaque.
class FontInfo {

    var userData: Any? = null

    /** pointer to .ttf file */
    var data: UByteArray = UByteArray(0)

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
    var svg = 0

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


// Given an offset into the file that defines a font, this function builds
// the necessary cached info for the rest of the system. You must allocate
// the stbtt_fontinfo yourself, and stbtt_InitFont will fill it out. You don't
// need to do anything special to free it, because the contents are pure
// value data with no additional data structures. Returns 0 on failure.
fun FontInfo.initFont(data: ByteArray, fontStart: Int): Boolean {

    val data = data.asUByteArray()
    this.data = data
    this.fontStart = fontStart
    cff = Buf()
    val cmap = TrueType.findTable(data, fontStart, "cmap")    // required
    loca = TrueType.findTable(data, fontStart, "loca").i // required
    head = TrueType.findTable(data, fontStart, "head").i // required
    glyf = TrueType.findTable(data, fontStart, "glyf").i // required
    hhea = TrueType.findTable(data, fontStart, "hhea").i // required
    hmtx = TrueType.findTable(data, fontStart, "hmtx").i // required
    kern = TrueType.findTable(data, fontStart, "kern").i // not required
    gpos = TrueType.findTable(data, fontStart, "GPOS").i // not required

    if (cmap == 0u || head == 0 || hhea == 0 || hmtx == 0)
        return false
    if (glyf != 0) {
        // required for truetype
        if (loca == 0) return false
    } else {
        // initialization for CFF / Type2 fonts (OTF)
        TODO("initialization for CFF / Type2 fonts (OTF)")
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

    val t = TrueType.findTable(data, fontStart, "maxp")
    numGlyphs = when (t) {
        0u -> 0xffff
        else -> data.ushort(t.i + 4).i
    }

    svg = -1

    // find a cmap encoding table we understand *now* to avoid searching
    // later. (todo: could make this installable)
    // the same regardless of glyph.
    val numTables = data.ushort(cmap.i + 2).i
    indexMap = 0
    for (i in 0 until numTables) {
        val encodingRecord = cmap + 4u + 8u * i.ui
        // find an encoding we understand:
        when (data.ushort(encodingRecord.i).i) {
            TrueType.PlatformID.MICROSOFT.i -> when (data.ushort(encodingRecord.i + 2).i) {
                // MS/Unicode
                TrueType.MS_EID.UNICODE_BMP.i, TrueType.MS_EID.UNICODE_FULL.i -> indexMap = cmap.i + data.ulong(encodingRecord.i + 4).i
            }

            // Mac/iOS has these
            // all the encodingIDs are unicode, so we don't bother to check it
            TrueType.PlatformID.UNICODE.i -> indexMap = cmap.i + data.ulong(encodingRecord.i + 4).i
        }
    }
    if (indexMap == 0)
        return false

    indexToLocFormat = data.ushort(head + 50).i
    return true
}