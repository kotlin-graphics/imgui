package imgui

import glm.*
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
        if (fontCollection.tag("ttcf"))
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
            font.tag4('1'.i, 0, 0, 0) or // TrueType 1
                    font.tag("typ1") or // TrueType with type 1 font -- we don't support this!
                    font.tag("OTTO") or // OpenType with CFF
                    font.tag4(0, 1, 0, 0) or // OpenType 1.0
                    font.tag("true")  // Apple specification for TrueType fonts

    fun CharArray.tag4(c0: Int, c1: Int, c2: Int, c3: Int) = tag4(0, c0, c1, c2, c3)
    fun CharArray.tag4(offset: Int, c0: Int, c1: Int, c2: Int, c3: Int) =
            this[offset].i == c0 && this[offset + 1].i == c1 && this[offset + 2].i == c2 && this[offset + 3].i == c3

    fun CharArray.tag(str: String) = tag(0, str)
    fun CharArray.tag(offset: Int, str: String) = tag4(offset, str[0].i, str[1].i, str[2].i, str[3].i)

    fun CharArray.s(p: Int) = this[p].i * 256 + this[p + 1]
    fun CharArray.i(p: Int) = (this[p] shl 24) + (this[p + 1] shl 16) + (this[p + 2] shl 8) + this[p + 3]

    fun initFont(info: Fontinfo, data: CharArray, offset: Int) = initFont_internal(info, data, offset)

    fun initFont_internal(info: Fontinfo, data: CharArray, fontStart: Int): Boolean {

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

        if (cmap == 0 || info.head == 0 || info.hhea == 0 || info.hmtx == 0) return false

        if (info.glyf != 0) {
            // required for truetype
            if (info.loca == 0) return false

        } else {
            // initialization for CFF / Type2 fonts (OTF)
            val cff = findTable(data, fontStart, "CFF ")
            if (cff == 0) return false

            info.fontdicts = Buf()
            info.fdselect = Buf()

            // TODO this should use size from table (not 512MB)
            info.cff = Buf(data, cff, 512 * 1024 * 1024)
            val b = info.cff

            // read the header
            b.skip(2)
            b.seek(b.get8()) // hdrsize

            // TODO the name INDEX could list multiple fonts, but we just use the first one.
            b.cffGetIndex()  // name INDEX
            val topDictIdx = b.cffGetIndex()
            val topDict = topDictIdx.cffIndexGet(0)
            b.cffGetIndex() // string INDEX
            info.gsubrs = b.cffGetIndex()

            val charStrings = topDict.dictGetInts(17, 1)
            val csType = topDict.dictGetInts(0x100 or 6, 1)
            val fdArrayOff = topDict.dictGetInts(0x100 or 36, 1)
            val fdSelectOff = topDict.dictGetInts(0x100 or 37, 1)
            info.subrs = getSubrs(b, topDict)

            // we only support Type 2 charstrings
            if (csType[0] != 2) return false
            if (charStrings[0] == 0) return false

            if (fdArrayOff[0] != 0) {
                // looks like a CID font
                if (fdSelectOff[0] == 0) return false
                b.seek(fdArrayOff[0])
                info.fontdicts = b.cffGetIndex()
                info.fdselect = b.range(fdSelectOff[0], b.size - fdSelectOff[0])
            }

            b.seek(charStrings[0])
            info.charStrings = b.cffGetIndex()
        }

        val t = findTable(data, fontStart, "maxp")
        info.numGlyphs = if (t != 0) data.s(t + 4) else 0xffff

        /*  find a cmap encoding table we understand *now* to avoid searching later. (todo: could make this installable)
            the same regardless of glyph.   */
        val numTables = data.s(cmap + 2)
        info.indexMap = 0
        for (i in 0 until numTables) {
            val encodingRecord = cmap + 4 + 8 * i
            // find an encoding we understand:
            when (data.s(encodingRecord)) {
                PlatformId.MICROSOFT.i -> {
                    when (data.s(encodingRecord + 2)) {
                    // MS/Unicode
                        MsEid.UNICODE_BMP.i, MsEid.UNICODE_FULL.i -> info.indexMap = cmap + data.i(encodingRecord + 4)
                    }
                }
            // Mac/iOS has these
            // all the encodingIDs are unicode, so we don't bother to check it
                PlatformId.UNICODE.i -> info.indexMap = cmap + data.i(encodingRecord + 4)
            }
        }
        if (info.indexMap == 0) return false

        info.indexToLocFormat = data.s(info.head + 50)
        return true
    }

    // @OPTIMIZE: binary search
    fun findTable(data: CharArray, fontStart: Int, tag: String): Int {
        val numTables = data.s(fontStart + 4)
        val tableDir = fontStart + 12
        for (i in 0 until numTables) {
            val loc = tableDir + 16 * i
            if (data.tag(loc, tag))
                return data.i(loc + 8)
        }
        return 0
    }

    fun getSubrs(cff: Buf, fontdict: Buf): Buf {
        val privateLoc = fontdict.dictGetInts(18, 2)
        if (privateLoc[1] == 0 || privateLoc[0] == 0) return Buf()
        val pDict = cff.range(privateLoc[1], privateLoc[0])
        val subrsOff = pDict.dictGetInts(19, 1)
        if (subrsOff.isEmpty()) return Buf()
        cff.seek(privateLoc[1] + subrsOff[0])
        return cff.cffGetIndex()
    }

    /** The following structure is defined publically so you can declare one on the stack or as a global or etc, but you
     *  should treat it as opaque.  */
    class Fontinfo {
//        void           * userdata;
        /** pointer to .ttf file    */
        lateinit var data: CharArray
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
        /** a cmap mapping for our chosen character encoding    */
        var indexMap = 0
        /** format needed to map from glyph index to glyph  */
        var indexToLocFormat = 0

        /** cff font data   */
        lateinit var cff: Buf
        /** the charstring index    */
        lateinit var charStrings: Buf
        /** global charstring subroutines index */
        lateinit var gsubrs: Buf
        /** private charstring subroutines index    */
        lateinit var subrs: Buf
        /** array of font dicts */
        lateinit var fontdicts: Buf
        /** map from glyph to fontdict  */
        lateinit var fdselect: Buf
    }

    /** private structure   */
    class Buf(
            var data: CharArray,
            var offset: Int,
            var cursor: Int,
            var size: Int) {
        constructor() : this(charArrayOf(), 0, 0, 0)
        constructor(data: CharArray, cursor: Int, size: Int) : this(data, 0, cursor, size)

        fun skip(o: Int) = seek(o)

        fun seek(o: Int) {
            assert(o in 0..size)
            cursor = if (o > size || o < 0) size else o
        }

        fun get(n: Int): Int {
            var v = 0
            assert(n in 1..4)
            repeat(n) { v = (v shl 8) or get8() }
            return v
        }

        fun get8() = if (cursor >= size) 0 else data[cursor++].i
        fun get16() = get(2)
        fun get32() = get(4)

        fun cffGetIndex(): Buf {
            var count = 0
            val start = cursor
            var offSize = 0
            count = get16()
            if (count != 0) {
                offSize = get8()
                assert(offSize in 1..4)
                skip(offSize * count)
                skip(get(offSize) - 1)
            }
            return range(start, cursor - start)
        }

        fun range(o: Int, s: Int): Buf {
            val r = Buf()
            if (o < 0 || s < 0 || o > size || s > size - o) return r
            r.data = data
            r.offset = o
            r.size = s
            return r
        }

        fun cffIndexGet(i: Int): Buf {
            seek(0)
            val count = get16()
            val offsize = get8()
            assert(i in 0..(count - 1))
            assert(offsize in 1..4)
            skip(i * offsize)
            val start = get(offsize)
            val end = get(offsize)
            return range(2 + (count + 1) * offsize + start, end - start)
        }

        fun dictGetInts(key: Int, outCount: Int): ArrayList<Int> {
            val operands = dictGet(key)
            var i = 0
            val out = ArrayList<Int>()
            while (i < outCount && operands.cursor < operands.size) {
                out.add(operands.cffInt())
                i++
            }
            return out
        }

        fun dictGet(key: Int): Buf {
            seek(0)
            while (cursor < size) {
                val start = cursor
                while (peek8() >= 28)
                    cffSkipOperand()
                val end = cursor
                var op = get8()
                if (op == 12) op = get8() or 0x100
                if (op == key) return range(start, end - start)
            }
            return range(0, 0)
        }

        fun peek8() = if (cursor >= size) 0 else data[offset + cursor].i

        fun cffSkipOperand() {
            val b0 = peek8()
            assert(b0 >= 28)
            if (b0 == 30) {
                skip(1)
                while (cursor < size) {
                    val v = get8()
                    if ((v and 0xF) == 0xF || (v ushr 4) == 0xF)
                        break
                }
            } else cffInt()
        }

        fun cffInt(): Int {
            val b0 = get8()
            return when (b0) {
                in 32..246 -> b0 - 139
                in 247..250 -> (b0 - 247) * 256 + get8() + 108
                in 251..254 -> -(b0 - 251) * 256 - get8() - 108
                28 -> get16()
                29 -> get32()
                else -> throw Error()
            }
        }
    }

    class PackRange {
        var fontSize = 0f
        /** if non-zero, then the chars are continuous, and this is the first codepoint */
        var firstUnicodeCodepointInRange = 0
        /** if non-zero, then this is an array of unicode codepoints    */
        val array_of_unicode_codepoints = mutableListOf<Int>()
        var numChars = 0
        /** output  */
        val charDataForRange = mutableListOf<PackedChar>()
        // don't set these, they're used internally
        var oversample = Vec2b()
    }

    /** this is an opaque structure that you shouldn't mess with which holds all the context needed from PackBegin to
     *  PackEnd.    */
    class PackContext {
        //        void *user_allocator_context;
        lateinit var packInfo: RectPack.Context
        var size = Vec2i()
        var strideInBytes = 0
        var padding = 0
        var oversample = Vec2i()
        //        unsigned char *pixels;
        lateinit var nodes: Array<RectPack.Node>
    }

    /** bitmap baking
     *  This is SUPER-AWESOME (tm Ryan Gordon) packing using stb_rect_pack.h. If stb_rect_pack.h isn't available, it
     *  uses the BakeFontBitmap strategy.   */
    fun packBegin(spc: PackContext, /*unsigned char *pixels,*/size: Vec2i, strideInBytes: Int, padding: Int /*void *alloc_context*/): Boolean {

        val context = RectPack.Context()
        val numNodes = size.x - padding
        val nodes = Array(numNodes, { RectPack.Node() })

//        spc.user_allocator_context = alloc_context;
        spc.size.put(size)
//        spc.pixels = pixels;
        spc.packInfo = context
        spc.nodes = nodes
        spc.padding = padding
        spc.strideInBytes = if (strideInBytes != 0) strideInBytes else size.x
        spc.oversample.put(1, 1)

        RectPack.initTarget(context, size - padding, nodes, numNodes)

//        if (pixels)
//            STBTT_memset(pixels, 0, pw*ph) // background of 0 around pixels

        return true
    }

    fun packSetOversampling(spc: PackContext, oversample: Vec2i) {
        assert(oversample.x <= MAX_OVERSAMPLE)
        assert(oversample.y <= MAX_OVERSAMPLE)
        spc.oversample put oversample
    }

    //////////////////////////////////////////////////////////////////////////////
    //
    // NEW TEXTURE BAKING API
    //
    // This provides options for packing multiple fonts into one atlas, not
    // perfectly but better than nothing.

    class PackedChar {
        /** coordinates of bbox in bitmap   */
        var _0 = Vec2i()
        /** coordinates of bbox in bitmap   */
        var _1 = Vec2i()

        var off = Vec2()
        var xadvance = 0f
        var off2 = Vec2()
    }

    /** platformID  */
    enum class PlatformId {UNICODE, MAC, ISO, MICROSOFT;

        val i = ordinal
    }

    /** encodingID for STBTT_PLATFORM_ID_UNICODE    */
    enum class UnicodeEid {UNICODE_1_0, UNICODE_1_1, ISO_10646, UNICODE_2_0_BMP, UNICODE_2_0_FULL;

        val i = ordinal
    }

    /** encodingID for STBTT_PLATFORM_ID_MICROSOFT  */
    enum class MsEid { SYMBOL, UNICODE_BMP, SHIFTJIS, UNICODE_FULL;

        val i = ordinal
    }

    /** encodingID for STBTT_PLATFORM_ID_MAC; same as Script Manager codes  */
    enum class MacEid { EID_ROMAN, JAPANESE, CHINESE_TRAD, KOREAN, ARABIC, HEBREW, GREEK, RUSSIAN;

        val i = ordinal
    }

    /** languageID for STBTT_PLATFORM_ID_MICROSOFT; same as LCID...
     *  problematic because there are e.g. 16 english LCIDs and 16 arabic LCIDs */
    enum class MsLang(val i: Int) { ENGLISH(0x0409), ITALIAN(0x0410), CHINESE(0x0804), JAPANESE(0x0411), DUTCH(0x0413),
        KOREAN(0x0412), FRENCH(0x040c), RUSSIAN(0x0419), GERMAN(0x0407), SPANISH(0x0409), HEBREW(0x040d), SWEDISH(0x041D)
    }

    /** languageID for STBTT_PLATFORM_ID_MAC    */
    enum class MacLang(val i: Int) { ENGLISH(0), JAPANESE(11), ARABIC(12), KOREAN(23), DUTCH(4), RUSSIAN(32), FRENCH(1),
        SPANISH(6), GERMAN(2), SWEDISH(5), HEBREW(10), CHINESE_SIMPLIFIED(33), ITALIAN(3), CHINESE_TRAD(19)
    }

    var MAX_OVERSAMPLE = 8
}

