package imgui.stb

//package imgui
//
//import gli.has
//import gli.hasnt
//import glm_.*
//import glm_.vec2.Vec2
//import glm_.vec2.operators.times
//import glm_.vec2.Vec2b
//import glm_.vec2.Vec2i
//import glm_.vec4.Vec4i
//import org.lwjgl.stb.STBTruetype.stbtt_GetGlyphBitmapBoxSubpixel
//import java.nio.ByteBuffer
//
//object TrueType {
//
//    fun getFontOffsetForIndex(fontCollection: CharArray, index: Int): Int {
//        // if it's just a font, there's only one valid index
//        if (isFont(fontCollection))
//            return if (index == 0) 0 else -1
//
//        // check if it's a TTC
//        if (fontCollection.tag("ttcf"))
//        // version 1?
//            if (fontCollection.i(4) == 0x00010000 || fontCollection.i(4) == 0x00020000) {
//                val n = fontCollection.i(8)
//                if (index >= n)
//                    return -1
//                return fontCollection.i(12 + index * 4)
//            }
//        return -1
//    }
//
//    fun isFont(font: CharArray) =
//            // check the version number
//            font.tag4('1'.i, 0, 0, 0) or // TrueType 1
//                    font.tag("typ1") or // TrueType with type 1 font -- we don't support this!
//                    font.tag("OTTO") or // OpenType with CFF
//                    font.tag4(0, 1, 0, 0) or // OpenType 1.0
//                    font.tag("true")  // Apple specification for TrueType fonts
//
//    fun CharArray.tag4(c0: Int, c1: Int, c2: Int, c3: Int) = tag4(0, c0, c1, c2, c3)
//    fun CharArray.tag4(offset: Int, c0: Int, c1: Int, c2: Int, c3: Int) =
//            this[offset].i == c0 && this[offset + 1].i == c1 && this[offset + 2].i == c2 && this[offset + 3].i == c3
//
//    fun CharArray.tag(str: String) = tag(0, str)
//    fun CharArray.tag(offset: Int, str: String) = tag4(offset, str[0].i, str[1].i, str[2].i, str[3].i)
//
//    fun CharArray.s(p: Int) = this[p].i * 256 + this[p + 1]
//    fun CharArray.i(p: Int) = (this[p] shl 24) + (this[p + 1] shl 16) + (this[p + 2] shl 8) + this[p + 3]
//
//    fun initFont(info: FontInfo, data: CharArray, fontStart: Int): Boolean {
//
//        info.data = data
//        info.fontStart = fontStart
//        info.cff = Buf()
//
//        val cmap = findTable(data, fontStart, "cmap")       // required
//        info.loca = findTable(data, fontStart, "loca") // required
//        info.head = findTable(data, fontStart, "head") // required
//        info.glyf = findTable(data, fontStart, "glyf") // required
//        info.hhea = findTable(data, fontStart, "hhea") // required
//        info.hmtx = findTable(data, fontStart, "hmtx") // required
//        info.kern = findTable(data, fontStart, "kern") // not required
//
//        if (cmap == 0 || info.head == 0 || info.hhea == 0 || info.hmtx == 0) return false
//
//        if (info.glyf != 0) {
//            // required for truetype
//            if (info.loca == 0) return false
//
//        } else {
//            // initialization for CFF / Type2 fonts (OTF)
//            val cff = findTable(data, fontStart, "CFF ")
//            if (cff == 0) return false
//
//            info.fontdicts = Buf()
//            info.fdselect = Buf()
//
//            // TODO this should use size from table (not 512MB)
//            info.cff = Buf(data, cff, 512 * 1024 * 1024)
//            val b = info.cff
//
//            // read the header
//            b.skip(2)
//            b.seek(b.get8()) // hdrsize
//
//            // TODO the name INDEX could list multiple fonts, but we just use the first one.
//            b.cffGetIndex()  // name INDEX
//            val topDictIdx = b.cffGetIndex()
//            val topDict = topDictIdx.cffIndexGet(0)
//            b.cffGetIndex() // string INDEX
//            info.gsubrs = b.cffGetIndex()
//
//            val charStrings = topDict.dictGetInts(17, 1)
//            val csType = topDict.dictGetInts(0x100 or 6, 1)
//            val fdArrayOff = topDict.dictGetInts(0x100 or 36, 1)
//            val fdSelectOff = topDict.dictGetInts(0x100 or 37, 1)
//            info.subrs = b.getSubrs(topDict)
//
//            // we only support Type 2 charstrings
//            if (csType[0] != 2) return false
//            if (charStrings[0] == 0) return false
//
//            if (fdArrayOff[0] != 0) {
//                // looks like a CID font
//                if (fdSelectOff[0] == 0) return false
//                b.seek(fdArrayOff[0])
//                info.fontdicts = b.cffGetIndex()
//                info.fdselect = b.range(fdSelectOff[0], b.size - fdSelectOff[0])
//            }
//
//            b.seek(charStrings[0])
//            info.charStrings = b.cffGetIndex()
//        }
//
//        val t = findTable(data, fontStart, "maxp")
//        info.numGlyphs = if (t != 0) data.s(t + 4) else 0xffff
//
//        /*  find a cmap encoding table we understand *now* to avoid searching later. (todo: could make this installable)
//            the same regardless of glyph.   */
//        val numTables = data.s(cmap + 2)
//        info.indexMap = 0
//        for (i in 0 until numTables) {
//            val encodingRecord = cmap + 4 + 8 * i
//            // find an encoding we understand:
//            when (data.s(encodingRecord)) {
//                PlatformId.MICROSOFT.i -> {
//                    when (data.s(encodingRecord + 2)) {
//                    // MS/Unicode
//                        MsEid.UNICODE_BMP.i, MsEid.UNICODE_FULL.i -> info.indexMap = cmap + data.i(encodingRecord + 4)
//                    }
//                }
//            // Mac/iOS has these
//            // all the encodingIDs are unicode, so we don't bother to check it
//                PlatformId.UNICODE.i -> info.indexMap = cmap + data.i(encodingRecord + 4)
//            }
//        }
//        if (info.indexMap == 0) return false
//
//        info.indexToLocFormat = data.s(info.head + 50)
//        return true
//    }
//
//    // @OPTIMIZE: binary search
//    fun findTable(data: CharArray, fontStart: Int, tag: String): Int {
//        val numTables = data.s(fontStart + 4)
//        val tableDir = fontStart + 12
//        for (i in 0 until numTables) {
//            val loc = tableDir + 16 * i
//            if (data.tag(loc, tag))
//                return data.i(loc + 8)
//        }
//        return 0
//    }
//
//    /** The following structure is defined publically so you can declare one on the stack or as a global or etc, but you
//     *  should treat it as opaque.  */
//    class FontInfo {
////        void           * userdata;
//        /** pointer to .ttf file    */
//        lateinit var data: CharArray
//        /** offset of start of font */
//        var fontStart = 0
//        /** number of glyphs, needed for range checking */
//        var numGlyphs = 0
//        // table locations as offset from start of .ttf
//        var loca = 0
//        var head = 0
//        var glyf = 0
//        var hhea = 0
//        var hmtx = 0
//        var kern = 0
//        /** a cmap mapping for our chosen character encoding    */
//        var indexMap = 0
//        /** format needed to map from glyph index to glyph  */
//        var indexToLocFormat = 0
//
//        /** cff font data   */
//        lateinit var cff: Buf
//        /** the charstring index    */
//        lateinit var charStrings: Buf
//        /** global charstring subroutines index */
//        lateinit var gsubrs: Buf
//        /** private charstring subroutines index    */
//        lateinit var subrs: Buf
//        /** array of font dicts */
//        lateinit var fontdicts: Buf
//        /** map from glyph to fontdict  */
//        lateinit var fdselect: Buf
//    }
//
//    /** private structure   */
//    class Buf(
//            var data: CharArray,
//            var offset: Int,
//            var cursor: Int,
//            var size: Int
//    ) {
//        constructor() : this(charArrayOf(), 0, 0, 0)
//        constructor(data: CharArray, cursor: Int, size: Int) : this(data, 0, cursor, size)
//
//        fun get8() = if (cursor >= size) 0 else data[cursor++].i
//
//        fun peek8() = if (cursor >= size) 0 else data[offset + cursor].i
//
//        fun seek(o: Int) {
//            assert(o in 0..size)
//            cursor = if (o > size || o < 0) size else o
//        }
//
//        fun skip(o: Int) = seek(cursor + o)
//
//        fun get(n: Int): Int {
//            var v = 0
//            assert(n in 1..4)
//            repeat(n) { v = (v shl 8) or get8() }
//            return v
//        }
//
//        init {
//            assert(size < 0x40000000)
//        }
//
//        fun get16() = get(2)
//        fun get32() = get(4)
//
//        fun range(o: Int, s: Int): Buf {
//            val r = Buf()
//            if (o < 0 || s < 0 || o > size || s > size - o) return r
//            r.data = data
//            r.offset = o
//            r.size = s
//            return r
//        }
//
//        fun cffGetIndex(): Buf {
//            var count = 0
//            val start = cursor
//            var offSize = 0
//            count = get16()
//            if (count != 0) {
//                offSize = get8()
//                assert(offSize in 1..4)
//                skip(offSize * count)
//                skip(get(offSize) - 1)
//            }
//            return range(start, cursor - start)
//        }
//
//        fun cffInt(): Int {
//            val b0 = get8()
//            return when (b0) {
//                in 32..246 -> b0 - 139
//                in 247..250 -> (b0 - 247) * 256 + get8() + 108
//                in 251..254 -> -(b0 - 251) * 256 - get8() - 108
//                28 -> get16()
//                29 -> get32()
//                else -> throw Error()
//            }
//        }
//
//        fun cffSkipOperand() {
//            val b0 = peek8()
//            assert(b0 >= 28)
//            if (b0 == 30) {
//                skip(1)
//                while (cursor < size) {
//                    val v = get8()
//                    if ((v and 0xF) == 0xF || (v ushr 4) == 0xF)
//                        break
//                }
//            } else cffInt()
//        }
//
//        fun dictGet(key: Int): Buf {
//            seek(0)
//            while (cursor < size) {
//                val start = cursor
//                while (peek8() >= 28)
//                    cffSkipOperand()
//                val end = cursor
//                var op = get8()
//                if (op == 12) op = get8() or 0x100
//                if (op == key) return range(start, end - start)
//            }
//            return range(0, 0)
//        }
//
//        fun dictGetInts(key: Int, outCount: Int): ArrayList<Int> {
//            val operands = dictGet(key)
//            var i = 0
//            val out = ArrayList<Int>()
//            while (i < outCount && operands.cursor < operands.size) {
//                out.add(operands.cffInt())
//                i++
//            }
//            return out
//        }
//
//        fun cffIndexCount(): Int {
//            seek(0)
//            return get16()
//        }
//
//        fun cffIndexGet(i: Int): Buf {
//            seek(0)
//            val count = get16()
//            val offsize = get8()
//            assert(i in 0..(count - 1))
//            assert(offsize in 1..4)
//            skip(i * offsize)
//            val start = get(offsize)
//            val end = get(offsize)
//            return range(2 + (count + 1) * offsize + start, end - start)
//        }
//
//
//        fun getSubrs(fontdict: Buf): Buf {
//            val privateLoc = fontdict.dictGetInts(18, 2)
//            if (privateLoc[1] == 0 || privateLoc[0] == 0) return Buf()
//            val pDict = range(privateLoc[1], privateLoc[0])
//            val subrsOff = pDict.dictGetInts(19, 1)
//            if (subrsOff.isEmpty()) return Buf()
//            seek(privateLoc[1] + subrsOff[0])
//            return cffGetIndex()
//        }
//
//        fun getSubr(n: Int): Buf {
//            val count = cffIndexCount()
//            var bias = 107
//            if (count >= 33900)
//                bias = 32768
//            else if (count >= 1240)
//                bias = 1131
//            val _n = n + bias
//            if (_n < 0 || _n >= count)
//                return Buf()
//            return cffIndexGet(_n)
//        }
//    }
//
//    class PackRange {
//        var fontSize = 0f
//        /** if non-zero, then the chars are continuous, and this is the first codepoint */
//        var firstUnicodeCodepointInRange = 0
//        /** if non-zero, then this is an array of unicode codepoints    */
//        val arrayOfUnicodeCodepoints = ArrayList<Int>()
//        var numChars = 0
//        /** output  */
//        var charDataForRange = 0
//        lateinit var bufCharDataForRange: Array<PackedChar>
//        // don't set these, they're used internally
//        var oversample = Vec2b()
//    }
//
//    /** this is an opaque structure that you shouldn't mess with which holds all the context needed from PackBegin to
//     *  PackEnd.    */
//    class PackContext {
//        //        void *user_allocator_context;
//        lateinit var packInfo: RectPack.Context
//        var size = Vec2i()
//        var strideInBytes = 0
//        var padding = 0
//        var oversample = Vec2i()
//        lateinit var pixels: CharArray
//        lateinit var nodes: Array<RectPack.Node>
//    }
//
//    /** bitmap baking
//     *  This is SUPER-AWESOME (tm Ryan Gordon) packing using stb_rect_pack.h. If stb_rect_pack.h isn't available, it
//     *  uses the BakeFontBitmap strategy.   */
//    fun packBegin(spc: PackContext, /*unsigned char *pixels,*/size: Vec2i, strideInBytes: Int, padding: Int /*void *alloc_context*/): Boolean {
//
//        val context = RectPack.Context()
//        val numNodes = size.x - padding
//        val nodes = Array(numNodes, { RectPack.Node() })
//
////        spc.user_allocator_context = alloc_context;
//        spc.size.put(size)
////        spc.pixels = pixels;
//        spc.packInfo = context
//        spc.nodes = nodes
//        spc.padding = padding
//        spc.strideInBytes = if (strideInBytes != 0) strideInBytes else size.x
//        spc.oversample.put(1)
//
//        RectPack.initTarget(context, size - padding, nodes, numNodes)
//
////        if (pixels)
////            STBTT_memset(pixels, 0, pw*ph) // background of 0 around pixels
//
//        return true
//    }
//
//    fun packSetOversampling(spc: PackContext, oversample: Vec2i) {
//        assert(oversample.x <= MAX_OVERSAMPLE)
//        assert(oversample.y <= MAX_OVERSAMPLE)
//        spc.oversample put oversample
//    }
//
//    /** rects array must be big enough to accommodate all characters in the given ranges    */
//    fun packFontRangesGatherRects(spc: PackContext, info: FontInfo, ranges: Int, bufRanges: Array<PackRange>, numRanges: Int,
//                                  rects: Int, bufRects: Array<RectPack.Rect>): Int {
//
//        var k = 0
//        for (i in 0 until numRanges) {
//            val range = bufRanges[ranges + i]
//            val fh = range.fontSize
//            val scale = if (fh > 0) scaleForPixelHeight(info, fh) else scaleForMappingEmToPixels(info, -fh)
//            range.oversample.put(spc.oversample.x.uc.b, spc.oversample.y.uc.b) // TODO ctr from Char
//            for (j in 0 until range.numChars) {
//                val codepoint =
//                        if (range.arrayOfUnicodeCodepoints.isEmpty()) range.firstUnicodeCodepointInRange + j
//                        else range.arrayOfUnicodeCodepoints[j]
//                val glyph = findGlyphIndex(info, codepoint)
//                val ref = Vec4i()
//                getGlyphBitmapBoxSubpixel(info, glyph, scale * Vec2(spc.oversample), Vec2(), ref)
//                bufRects[rects + k].w = ref.z - ref.x + spc.padding + spc.oversample.x - 1
//                bufRects[rects + k].h = ref.w - ref.y + spc.padding + spc.oversample.y - 1
//                ++k
//            }
//        }
//
//        return k
//    }
//
//    fun scaleForPixelHeight(info: FontInfo, height: Float) = height / (info.data.s(info.hhea + 4) - info.data.s(info.hhea + 6)).f
//
//    fun scaleForMappingEmToPixels(info: FontInfo, pixels: Float) = pixels / info.data.s(info.head + 18)
//
//    fun findGlyphIndex(info: FontInfo, unicodeCodepoint: Int): Int {
//
//        val data = info.data
//        val indexMap = info.indexMap
//
//        val format = data.s(indexMap)
//
//        return when (format) {
//
//            0 -> { // apple byte encoding
//                val bytes = data.s(indexMap + 2)
//                if (unicodeCodepoint < bytes - 6)
//                    return data[indexMap + 6 + unicodeCodepoint].i
//                0
//            }
//
//            6 -> {
//                val first = data.s(indexMap + 6)
//                val count = data.s(indexMap + 8)
//                if (unicodeCodepoint >= first && unicodeCodepoint < first + count)
//                    data.s(indexMap + 10 + (unicodeCodepoint - first) * 2)
//                else 0
//            }
//
//            2 -> throw Error("TODO: high-byte mapping for japanese/chinese/korean")
//
//            4 -> { // standard mapping for windows fonts: binary search collection of ranges
//                val segCount = data.s(indexMap + 6) ushr 1
//                var searchRange = data.s(indexMap + 8) ushr 1
//                var entrySelector = data.s(indexMap + 10)
//                val rangeShift = data.s(indexMap + 12) ushr 1
//
//                // do a binary search of the segments
//                val endCount = indexMap + 14
//                var search = endCount
//
//                if (unicodeCodepoint > 0xffff) 0
//                else {
//                    // they lie from endCount .. endCount + segCount but searchRange is the nearest power of two, so...
//                    if (unicodeCodepoint >= data.s(search + rangeShift * 2))
//                        search += rangeShift * 2
//
//                    // now decrement to bias correctly to find smallest
//                    search -= 2
//                    while (entrySelector != 0) {
//                        searchRange = searchRange ushr 1
//                        val end = data.s(search + searchRange * 2)
//                        if (unicodeCodepoint > end)
//                            search += searchRange * 2
//                        --entrySelector
//                    }
//                    search += 2
//
//                    val item = (search - endCount) ushr 1
//
//                    assert(unicodeCodepoint <= data.s(endCount + 2 * item))
//                    val start = data.s(indexMap + 14 + segCount * 2 + 2 + 2 * item)
//                    if (unicodeCodepoint < start) 0
//                    else {
//                        val offset = data.s(indexMap + 14 + segCount * 6 + 2 + 2 * item)
//                        if (offset == 0)
//                            unicodeCodepoint + data.s(indexMap + 14 + segCount * 4 + 2 + 2 * item)
//                        else
//                            data.s(offset + (unicodeCodepoint - start) * 2 + indexMap + 14 + segCount * 6 + 2 + 2 * item)
//                    }
//                }
//            }
//            12, 13 -> {
//                var low = 0
//                var high = data.i(indexMap + 12)
//                // Binary search the right group.
//                var res = 0 // not found
//                while (low < high) {
//                    val mid = low + ((high - low) ushr 1) // rounds down, so low <= mid < high
//                    val startChar = data.i(indexMap + 16 + mid * 12)
//                    val endChar = data.i(indexMap + 16 + mid * 12 + 4)
//                    if (unicodeCodepoint < startChar)
//                        high = mid
//                    else if (unicodeCodepoint > endChar)
//                        low = mid + 1
//                    else {
//                        val startGlyph = data.i(indexMap + 16 + mid * 12 + 8)
//                        res =
//                                if (format == 12) startGlyph + unicodeCodepoint - startChar
//                                else startGlyph // format == 13
//                    }
//                }
//                res
//            }
//            else -> throw Error("TODO")
//        }
//    }
//
//    //////////////////////////////////////////////////////////////////////////////
//    //
//    // antialiasing software rasterizer
//    //
//
//    fun getGlyphBitmapBoxSubpixel(font: FontInfo, glyph: Int, scale: Vec2, shift: Vec2, i: Vec4i) {
//        val box = Vec4i(0)
//        if (!getGlyphBox(font, glyph, box)) {
//            // e.g. space character
//            if (i.x != 0) i.x = 0
//            if (i.y != 0) i.y = 0
//            if (i.z != 0) i.z = 0
//            if (i.w != 0) i.w = 0
//        } else {
//            // move to integral bboxes (treating pixels as little squares, what pixels get touched)?
//            if (i.x != 0) i.x = glm.floor(box.x * scale.x + shift.x).i
//            if (i.y != 0) i.y = glm.floor(-box.w * scale.y + shift.y).i
//            if (i.z != 0) i.z = glm.ceil(-box.z * scale.x + shift.x).i
//            if (i.w != 0) i.w = glm.ceil(-box.y * scale.y + shift.y).i
//        }
//    }
//
//    fun getGlyphBox(info: FontInfo, glyphIndex: Int, i: Vec4i): Boolean {
//        if (info.cff.size > 0)
//            getGlyphInfoT2(info, glyphIndex, i)
//        else {
//            val g = getGlyfOffset(info, glyphIndex)
//            if (g < 0) return false
//
//            if (i.x != 0) i.x = info.data.s(g + 2)
//            if (i.y != 0) i.y = info.data.s(g + 4)
//            if (i.z != 0) i.z = info.data.s(g + 6)
//            if (i.w != 0) i.w = info.data.s(g + 8)
//        }
//        return true
//    }
//
//    fun getGlyphInfoT2(info: FontInfo, glyphIndex: Int, i: Vec4i): Int {
//        val c = Csctx(1)
//        val r = runCharstring(info, glyphIndex, c)
//        if (i.x != 0) {
//            i.x = if (r) c.min.x else 0
//            i.y = if (r) c.min.y else 0
//            i.z = if (r) c.max.x else 0
//            i.w = if (r) c.max.y else 0
//        }
//        return if (r) c.numVertices else 0
//    }
//
//    class Csctx(val bounds: Int = 1) {
//
//        var started = false
//        val first = Vec2()
//        var x = 0f
//        var y = 0f
//        val min = Vec2i()
//        val max = Vec2i()
//
//        lateinit var vertices: ArrayList<Vertex>
//        var numVertices = 0
//
//        fun track_vertex(x: Int, y: Int) {
//            if (x > max.x || !started) max.x = x
//            if (y > max.y || !started) max.y = y
//            if (x < min.x || !started) min.x = x
//            if (y < min.y || !started) min.y = y
//            started = true
//        }
//
//        fun v(type: V, x: Int, y: Int, cx: Int, cy: Int, cx1: Int, cy1: Int) {
//            if (bounds != 0) {
//                track_vertex(x, y)
//                if (type == V.cubic) {
//                    track_vertex(cx, cy)
//                    track_vertex(cx1, cy1)
//                }
//            } else {
//                vertices[numVertices].set(type, x, y, cx, cy)
//                vertices[numVertices].cx1 = cx1
//                vertices[numVertices].cy1 = cy1
//            }
//            numVertices++
//        }
//
//        fun close_shape() {
//            if (first.x != x || first.y != y)
//                v(V.line, first.x.i, first.y.i, 0, 0, 0, 0)
//        }
//
//        fun rmove_to(dx: Float, dy: Float) {
//            close_shape()
//            x += dx
//            first.x = x
//            y += dy
//            first.y = y
//            v(V.move, x.i, y.i, 0, 0, 0, 0)
//        }
//
//        fun rline_to(dx: Float, dy: Float) {
//            x += dx
//            y += dy
//            v(V.line, x.i, y.i, 0, 0, 0, 0)
//        }
//
//        fun rccurve_to(dx1: Float, dy1: Float, dx2: Float, dy2: Float, dx3: Float, dy3: Float) {
//            val cx1 = x + dx1
//            val cy1 = y + dy1
//            val cx2 = cx1 + dx2
//            val cy2 = cy1 + dy2
//            x = cx2 + dx3
//            y = cy2 + dy3
//            v(V.cubic, x.i, y.i, cx1.i, cy1.i, cx2.i, cy2.i)
//        }
//    }
//
//    class Vertex {
//        var x = 0
//        var y = 0
//        var cx = 0
//        var cy = 0
//        var cx1 = 0
//        var cy1 = 0
//        var type = 0
//        var padding = ' '
//        fun set(type: V, x: Int, y: Int, cx: Int, cy: Int) {
//            this.type = type.i
//            this.x = x
//            this.y = y
//            this.cx = cx
//            this.cy = cy
//        }
//    }
//
//    fun runCharstring(info: FontInfo, glyphIndex: Int, c: Csctx): Boolean {
//
//        var inHeader = 1
//        var maskbits = 0
//        var subrStackHeight = 0
//        var sp = 0
//        var v = 0
//        var hasSubrs = false
//        val s = FloatArray(48)
//        val subrStack = Array(10, { Buf() })
//        var subrs = info.subrs //, b;
//        var f = 0f
//
//        fun CSERR(s: String): Boolean {
//            System.err.println(s)
//            return false
//        }
//
//        // this currently ignores the initial width value, which isn't needed if we have hmtx
//        var b = info.charStrings.cffIndexGet(glyphIndex)
//        while (b.cursor < b.size) {
//            var i = 0
//            var clear_stack = true
//            val b0 = b.get8()
//
//            when (b0) {
//            // @TODO implement hinting
//                0x13, 0x14 -> {// hintmask, cntrmask
//                    if (inHeader != 0)
//                        maskbits += sp / 2  // implicit "vstem"
//                    inHeader = 0
//                    b.skip((maskbits + 7) / 8)
//                }
//            // hstem, vstem, hstemhm, vstemhm
//                0x01, 0x03, 0x12, 0x17 -> maskbits += (sp / 2)
//
//                0x15 -> { // rmoveto
//                    inHeader = 0
//                    if (sp < 2) return CSERR("rmoveto stack")
//                    c.rmove_to(s[sp - 2], s[sp - 1])
//                }
//                0x04 -> { // vmoveto
//                    inHeader = 0
//                    if (sp < 1) return CSERR("vmoveto stack")
//                    c.rmove_to(0f, s[sp - 1])
//                }
//                0x16 -> { // hmoveto
//                    inHeader = 0
//                    if (sp < 1) return CSERR("hmoveto stack")
//                    c.rmove_to(s[sp - 1], 0f)
//                }
//
//                0x05 -> { // rlineto
//                    if (sp < 2) return CSERR("rlineto stack")
//                    while (i + 1 < sp) {
//                        c.rline_to(s[i], s[i + 1])
//                        i += 2
//                    }
//                }
//            // hlineto/vlineto and vhcurveto/hvcurveto alternate horizontal and vertical starting from a different place.
//                0x07 -> {   // vlineto
//                    if (sp < 1) return CSERR("vlineto stack")
//                    if (i < sp)
//                        c.rline_to(0f, s[i])
//                }
//                0x06 -> {   // hlineto
//                    if (sp < 1) return CSERR("hlineto stack")
//                    while (true) {
//                        if (i >= sp) break
//                        c.rline_to(s[i], 0f)
//                        i++
//                        if (i >= sp) break
//                        c.rline_to(0f, s[i])
//                        i++
//                    }
//                }
//                0x1F -> {   // hvcurveto
//                    if (sp < 4) return CSERR("hvcurveto stack")
//                    if (i + 3 < sp)
//                        c.rccurve_to(s[i], 0f, s[i + 1], s[i + 2], if (sp - i == 5) s[i + 4] else 0f, s[i + 3])
//                }
//                0x1E -> {   // vhcurveto
//                    if (sp < 4) return CSERR("vhcurveto stack")
//                    while (true) {
//                        if (i + 3 >= sp) break
//                        c.rccurve_to(0f, s[i], s[i + 1], s[i + 2], s[i + 3], if (sp - i == 5) s[i + 4] else 0f)
//                        i += 4
//                        if (i + 3 >= sp) break
//                        c.rccurve_to(s[i], 0f, s[i + 1], s[i + 2], if (sp - i == 5) s[i + 4] else 0f, s[i + 3])
//                        i += 4
//                    }
//                }
//                0x08 -> {   // rrcurveto
//                    if (sp < 6) return CSERR("rcurveline stack")
//                    while (i + 5 < sp) {
//                        c.rccurve_to(s[i], s[i + 1], s[i + 2], s[i + 3], s[i + 4], s[i + 5])
//                        i += 6
//                    }
//                }
//                0x18 -> {   // rcurveline
//                    if (sp < 8) return CSERR("rcurveline stack")
//                    while (i + 5 < sp - 2) {
//                        c.rccurve_to(s[i], s[i + 1], s[i + 2], s[i + 3], s[i + 4], s[i + 5])
//                        i += 6
//                    }
//                    if (i + 1 >= sp) return CSERR("rcurveline stack")
//                    c.rline_to(s[i], s[i + 1])
//                }
//                0x19 -> { // rlinecurve
//                    if (sp < 8) return CSERR("rlinecurve stack")
//                    while (i + 1 < sp - 6) {
//                        c.rline_to(s[i], s[i + 1])
//                        i += 2
//                    }
//                    if (i + 5 >= sp) return CSERR("rlinecurve stack")
//                    c.rccurve_to(s[i], s[i + 1], s[i + 2], s[i + 3], s[i + 4], s[i + 5])
//                }
//                0x1A, 0x1B -> {     // vvcurveto, hhcurveto
//                    if (sp < 4) return CSERR("(vv|hh)curveto stack")
//                    f = 0f
//                    if ((sp and 1) != 0) {
//                        f = s[i]
//                        i++
//                    }
//                    while (i + 3 < sp) {
//                        if (b0 == 0x1B)
//                            c.rccurve_to(s[i], f, s[i + 1], s[i + 2], s[i + 3], 0f)
//                        else
//                            c.rccurve_to(f, s[i], s[i + 1], s[i + 2], 0f, s[i + 3])
//                        f = 0f
//                        i += 4
//                    }
//                }
//                0x0A -> {   // callsubr
//                    if (!hasSubrs) {
//                        if (info.fdselect.size > 0)
//                            subrs = cidGetGlyphSubrs(info, glyphIndex)
//                        hasSubrs = true
//                    }
//                    if (sp < 1) return CSERR("call(g|)subr stack")
//                    v = s[--sp].i
//                    if (subrStackHeight >= 10) return CSERR("recursion limit")
//                    subrStack[subrStackHeight++] = b
//                    b = (if (b0 == 0x0A) subrs else info.gsubrs).getSubr(v)
//                    if (b.size == 0) return CSERR("subr not found")
//                    b.cursor = 0
//                    clear_stack = false
//                }
//                0x1D -> {   // callgsubr
//                    if (sp < 1) return CSERR("call(g|)subr stack")
//                    v = s[--sp].i
//                    if (subrStackHeight >= 10) return CSERR("recursion limit")
//                    subrStack[subrStackHeight++] = b
//                    b = (if (b0 == 0x0A) subrs else info.gsubrs).getSubr(v)
//                    if (b.size == 0) return CSERR("subr not found")
//                    b.cursor = 0
//                    clear_stack = false
//                }
//                0x0B -> {   // return
//                    if (subrStackHeight <= 0) return CSERR("return outside subr")
//                    b = subrStack[--subrStackHeight]
//                    clear_stack = false
//                }
//                0x0E -> { // endchar
//                    c.close_shape()
//                    return true
//                }
//                0x0C -> {   // two-byte escape
//                    var dx1 = 0f
//                    var dx2 = 0f
//                    var dx3 = 0f
//                    var dx4 = 0f
//                    var dx5 = 0f
//                    var dx6 = 0f
//                    var dy1 = 0f
//                    var dy2 = 0f
//                    var dy3 = 0f
//                    var dy4 = 0f
//                    var dy5 = 0f
//                    var dy6 = 0f
//                    var dx = 0f
//                    var dy = 0f
//                    val b1 = b.get8()
//                    when (b1) {
//                    // @TODO These "flex" implementations ignore the flex-depth and resolution, and always draw beziers.
//                        0x22 -> {    // hflex
//                            if (sp < 7) return CSERR("hflex stack")
//                            dx1 = s[0]
//                            dx2 = s[1]
//                            dy2 = s[2]
//                            dx3 = s[3]
//                            dx4 = s[4]
//                            dx5 = s[5]
//                            dx6 = s[6]
//                            c.rccurve_to(dx1, 0f, dx2, dy2, dx3, 0f)
//                            c.rccurve_to(dx4, 0f, dx5, -dy2, dx6, 0f)
//                        }
//                        0x23 -> { // flex
//                            if (sp < 13) return CSERR("flex stack")
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
//                            c.rccurve_to(dx1, dy1, dx2, dy2, dx3, dy3)
//                            c.rccurve_to(dx4, dy4, dx5, dy5, dx6, dy6)
//                        }
//                        0x24 -> { // hflex1
//                            if (sp < 9) return CSERR("hflex1 stack")
//                            dx1 = s[0]
//                            dy1 = s[1]
//                            dx2 = s[2]
//                            dy2 = s[3]
//                            dx3 = s[4]
//                            dx4 = s[5]
//                            dx5 = s[6]
//                            dy5 = s[7]
//                            dx6 = s[8]
//                            c.rccurve_to(dx1, dy1, dx2, dy2, dx3, 0f)
//                            c.rccurve_to(dx4, 0f, dx5, dy5, dx6, -(dy1 + dy2 + dy5))
//                        }
//                        0x25 -> { // flex1
//                            if (sp < 11) return CSERR("flex1 stack")
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
//                            dy6 = s[10]
//                            dx6 = dy6
//                            dx = dx1 + dx2 + dx3 + dx4 + dx5
//                            dy = dy1 + dy2 + dy3 + dy4 + dy5
//                            if (glm.abs(dx) > glm.abs(dy))
//                                dy6 = -dy
//                            else
//                                dx6 = -dx
//                            c.rccurve_to(dx1, dy1, dx2, dy2, dx3, dy3)
//                            c.rccurve_to(dx4, dy4, dx5, dy5, dx6, dy6)
//                        }
//                        else -> CSERR("unimplemented")
//                    }
//                }
//
//                else -> {
//                    if (b0 != 255 && b0 != 28 && (b0 < 32 || b0 > 254))
//                        return CSERR("reserved operator")
//
//                    // push immediate
//                    if (b0 == 255)
//                        f = (b.get32() / 0x10000).f
//                    else {
//                        b.skip(-1)
//                        f = b.cffInt().f
//                    }
//                    if (sp >= 48) return CSERR("push stack overflow")
//                    s[sp++] = f
//                    clear_stack = false
//                }
//            }
//            if (clear_stack) sp = 0
//        }
//        return CSERR("no endchar")
//    }
//
//    fun cidGetGlyphSubrs(info: FontInfo, glyphIndex: Int): Buf {
//        val fdselect = info.fdselect
//        var fdselector = -1
//
//        fdselect.seek(0)
//        val fmt = fdselect.get8()
//        if (fmt == 0) {
//            // untested
//            fdselect.skip(glyphIndex)
//            fdselector = fdselect.get8()
//        } else if (fmt == 3) {
//            val nranges = fdselect.get16()
//            var start = fdselect.get16()
//            for (i in 0 until nranges) {
//                val v = fdselect.get8()
//                val end = fdselect.get16()
//                if (glyphIndex in start until end) {
//                    fdselector = v
//                    break
//                }
//                start = end
//            }
//        }
////        if (fdselector == -1) stbtt__new_buf(NULL, 0) TODO useless
//        return info.cff.getSubrs(info.fontdicts.cffIndexGet(fdselector))
//    }
//
//    fun getGlyfOffset(info: FontInfo, glyphIndex: Int): Int {
//
//        assert(info.cff.size == 0)
//
//        if (glyphIndex >= info.numGlyphs) return -1 // glyph index out of range
//        if (info.indexToLocFormat >= 2) return -1   // unknown index->glyph map format
//
//        var g1 = 0
//        var g2 = 0
//        if (info.indexToLocFormat == 0) {
//            g1 = info.glyf + info.data.s(info.loca + glyphIndex * 2) * 2
//            g2 = info.glyf + info.data.s(info.loca + glyphIndex * 2 + 2) * 2
//        } else {
//            g1 = info.glyf + info.data.i(info.loca + glyphIndex * 4)
//            g2 = info.glyf + info.data.i(info.loca + glyphIndex * 4 + 4)
//        }
//
//        return if (g1 == g2) -1 else g1 // if length is 0, return -1
//    }
//
//    // rects array must be big enough to accommodate all characters in the given ranges
//    fun packFontRangesRenderIntoRects(spc: PackContext, info: FontInfo, ranges: Array<PackRange>, offsetRanges: Int, numRanges: Int,
//                                      rects: Array<RectPack.Rect>, offsetRects: Int): Boolean {
//
//        var returnValue = true
//
//        // save current values
//        val oldOver = Vec2(spc.oversample)
//
//        var k = 0
//        for (i in 0 until numRanges) {
//            val range = ranges[offsetRanges + i]
//            val fh = range.fontSize
//            val scale = if (fh > 0) scaleForPixelHeight(info, fh) else scaleForMappingEmToPixels(info, -fh)
//            spc.oversample put range.oversample
//            val recip = Vec2(1f) / spc.oversample
//            val sub = Vec2(oversampleShift(spc.oversample.x), oversampleShift(spc.oversample.y))
//            for (j in 0 until range.numChars) {
//                val r = rects[offsetRects + k]
//                if (r.wasPacked) {
//                    val bc = range.bufCharDataForRange[j]
//                    val codepoint =
//                            if (range.arrayOfUnicodeCodepoints.isEmpty()) range.firstUnicodeCodepointInRange + j
//                            else ranges[i].arrayOfUnicodeCodepoints[j]
//                    val glyph = findGlyphIndex(info, codepoint)
//                    val pad = spc.padding
//
//                    // pad on left and top
//                    r.x += pad
//                    r.y += pad
//                    r.w -= pad
//                    r.h -= pad
//                    val params = Vec2i(0) // advancedWidth, leftSideBearing
//                    getGlyphHMetrics(info, glyph, params)
//                    val advance = params.x
//                    val box = Vec4i(0)
//                    getGlyphBitmapBox(info, glyph, scale * Vec2(spc.oversample), box)
//                    makeGlyphBitmapSubpixel(info,
//                            spc->pixels+r->x+r->y*spc->stride_in_bytes,
//                    r->w-spc->h_oversample+1,
//                    r->h-spc->v_oversample+1,
//                    spc->stride_in_bytes,
//                    scale * spc->h_oversample,
//                    scale * spc->v_oversample,
//                    0, 0,
//                    glyph)
//
//                    if (spc->h_oversample > 1)
//                    stbtt__h_prefilter(spc->pixels+r->x+r->y*spc->stride_in_bytes,
//                    r->w, r->h, spc->stride_in_bytes,
//                    spc->h_oversample)
//
//                    if (spc->v_oversample > 1)
//                    stbtt__v_prefilter(spc->pixels+r->x+r->y*spc->stride_in_bytes,
//                    r->w, r->h, spc->stride_in_bytes,
//                    spc->v_oversample)
//
//                    bc->x0 = (stbtt_int16)  r->x
//                    bc->y0 = (stbtt_int16)  r->y
//                    bc->x1 = (stbtt_int16) (r->x+r->w)
//                    bc->y1 = (stbtt_int16) (r->y+r->h)
//                    bc->xadvance = scale * advance
//                    bc->xoff = (float)  x0 * recip_h+sub_x
//                    bc->yoff = (float)  y0 * recip_v+sub_y
//                    bc->xoff2 = (x0+r->w) * recip_h+sub_x
//                    bc->yoff2 = (y0+r->h) * recip_v+sub_y
//                } else {
//                    returnValue = 0 // if any fail, report failure
//                }
//
//                ++k
//            }
//        }
//
//        // restore original values
//        spc->h_oversample = old_h_over
//        spc->v_oversample = old_v_over
//
//        return returnValue
//    }
//
//    fun oversampleShift(oversample: Int): Float {
//        if (oversample == 0)
//            return 0f
//
//        /*  The prefilter is a box filter of width "oversample", which shifts phase by (oversample - 1)/2 pixels in
//            oversampled space. We want to shift in the opposite direction to counter this.  */
//        return -(oversample - 1) / (2.0f * oversample)
//    }
//
//    fun getGlyphHMetrics(info: FontInfo, glyphIndex: Int, params: Vec2i) {// advancedWidth, leftSideBearing
//        val numOfLongHorMetrics = info.data.s(info.hhea + 34)
//        if (glyphIndex < numOfLongHorMetrics) {
//            if (params.x != 0) params.x = info.data.s(info.hmtx + 4 * glyphIndex)
//            if (params.y != 0) params.y = info.data.s(info.hmtx + 4 * glyphIndex + 2)
//        } else {
//            if (params.x != 0) params.x = info.data.s(info.hmtx + 4 * (numOfLongHorMetrics - 1))
//            if (params.y != 0) params.y = info.data.s(info.hmtx + 4 * numOfLongHorMetrics + 2 * (glyphIndex - numOfLongHorMetrics))
//        }
//    }
//
//    fun getGlyphBitmapBox(font: FontInfo, glyph: Int, scale: Vec2, i: Vec4i) = getGlyphBitmapBoxSubpixel(font, glyph, scale, Vec2(), i)
//
//    fun makeGlyphBitmapSubpixel(info: FontInfo, output: CharArray, offsetOutput: Int, out: Vec2i, outStride: Int, scale: Vec2,
//                                shift: Vec2, glyph: Int) {
//        val i = Vec4i()
//        val vertices = ArrayList<Vertex>()
//        val numVerts = getGlyphShape(info, glyph, vertices)
//
//        getGlyphBitmapBoxSubpixel(info, glyph, scale, shift, i)
//        val gbm = Bitmap(out, outStride, output)
//
//        if (gbm.w != 0 && gbm.h != 0)
//            rasterize(gbm, 0.35f, vertices, numVerts, scale, shift, i.x, i.y, 1, info.userdata)
//
//        STBTT_free(vertices, info->userdata)
//    }
//
//    fun getGlyphShape(info: FontInfo, glyphIndex: Int, pVertices: ArrayList<Vertex>) =
//            if (info.cff.size == 0) getGlyphShapeTT(info, glyphIndex, pVertices)
//            else getGlyphShapeT2(info, glyphIndex, pVertices)
//
//    fun getGlyphShapeTT(info: FontInfo, glyphIndex: Int, pVertices: ArrayList<Vertex>): Int {
//
//        var data = 0
//        var vertices = arrayOf<Vertex>()
//        var numVertices = 0
//        val g = getGlyfOffset(info, glyphIndex)
//
//        if (g < 0) return 0
//
//        val numberOfContours = info.data.s(data + g)
//
//        if (numberOfContours > 0) {
//
//            var flags = 0
//
//            var j = 0
//            var wasOff = false
//            var startOff = false
//            var endPtsOfContours = (data + g + 10)
//            var ins = info.data.s(data + g + 10 + numberOfContours * 2)
//            var points = data + g + 10 + numberOfContours * 2 + 2 + ins
//
//            var n = 1 + info.data.s(endPtsOfContours + numberOfContours * 2 - 2)
//
//            var m = n + 2 * numberOfContours  // a loose bound on how many vertices we might need
//            val vertices = Array(m, { Vertex() })
//
//            var next_move = 0
//            var flagcount = 0
//
//            /*  in first pass, we load uninterpreted data into the allocated array above, shifted to the end of the
//                array so we won't overwrite it when we create our final data starting from the front    */
//
//            var off = m - n // starting offset for uninterpreted data, regardless of how m ends up being calculated
//
//            // first load flags
//
//            for (i in 0 until n) {
//                if (flagcount == 0) {
//                    flags = points++
//                    if (flags has 8)
//                        flagcount = points++
//                } else
//                    --flagcount
//                vertices[off + i].type = flags
//            }
//
//            // now load x coordinates
//            var x = 0
//            for (i in 0 until n) {
//                flags = vertices[off + i].type
//                if (flags has 2) {
//                    val dx = points++
//                    x += if (flags has 16) dx else -dx // ???
//                } else {
//                    if (flags hasnt 16) {
//                        x += info.data[points] * 256 + info.data[points + 1]
//                        points += 2
//                    }
//                }
//                vertices[off + i].x = x
//            }
//
//            // now load y coordinates
//            var y = 0
//            for (i in 0 until n) {
//                flags = vertices[off + i].type
//                if (flags has 4) {
//                    val dy = points++
//                    y += if (flags has 32) dy else -dy // ???
//                } else {
//                    if (flags hasnt 32) {
//                        y += info.data[points] * 256 + info.data[points + 1]
//                        points += 2
//                    }
//                }
//                vertices[off + i].y = y
//            }
//
//            // now convert them to our format
//            numVertices = 0
//            val s = Vec2i()
//            val c = Vec2i()
//            val sc = Vec2i()
//            var i = 0
//            while (i < n) {
//                flags = vertices[off + i].type
//                x = vertices[off + i].x
//                y = vertices[off + i].y
//
//                if (next_move == i) {
//                    if (i != 0)
//                        numVertices = closeShape(vertices, numVertices, wasOff, startOff, s, sc, c)
//
//                    // now start the new one
//                    startOff = flags hasnt 1
//                    if (startOff) {
//                        /*  if we start off with an off-curve point, then when we need to find a point on the curve
//                            where we can start, and we need to save some state for when we wraparound.  */
//                        sc.put(x, y)
//                        if (vertices[off + i + 1].type hasnt 1) {
//                            // next point is also a curve point, so interpolate an on-point curve
//                            s.x = (x + vertices[off + i + 1].x) ushr 1
//                            s.y = (y + vertices[off + i + 1].y) ushr 1
//                        } else {
//                            // otherwise just use the next point as our start point
//                            s.x = vertices[off + i + 1].x
//                            s.y = vertices[off + i + 1].y
//                            ++i // we're using point i+1 as the starting point, so skip it
//                        }
//                    } else s.put(x, y)
//                    vertices[numVertices++].set(V.move, s.x, s.y, 0, 0)
//                    wasOff = false
//                    next_move = 1 + info.data.s(endPtsOfContours + j * 2)
//                    ++j
//                } else {
//                    if (flags hasnt 1) {
//                        // if it's a curve
//                        if (wasOff) // two off-curve control points in a row means interpolate an on-curve midpoint
//                            vertices[numVertices++].set(V.curve, (c.x + x) ushr 1, (c.y + y) ushr 1, c.x, c.y)
//                        c.put(x, y)
//                        wasOff = true
//                    } else {
//                        if (wasOff)
//                            vertices[numVertices++].set(V.curve, x, y, c.x, c.y)
//                        else
//                            vertices[numVertices++].set(V.line, x, y, 0, 0)
//                        wasOff = false
//                    }
//                }
//            }
//            numVertices = closeShape(vertices, numVertices, wasOff, startOff, s, sc, c)
//        } else if (numberOfContours == -1) {
//            // Compound shapes.
//            var more = true
//            var comp = data + g + 10
//            numVertices = 0
////            vertices = 0
//            while (more) {
//                var compNumVerts = 0
//                var comp_verts = 0
//                val tmp = ArrayList<Vertex>()
//                val mtx = floatArrayOf(1f, 0f, 0f, 1f, 0f, 0f)
//
//                var flags = info.data.s(comp); comp += 2
//                var gidx = info.data.s(comp); comp += 2
//
//                if (flags has 2) {
//                    // XY values
//                    if (flags has 1) {
//                        // shorts
//                        mtx[4] = info.data.s(comp).f; comp += 2
//                        mtx[5] = info.data.s(comp).f; comp += 2
//                    } else {
//                        mtx[4] = info.data[comp].f; comp += 1
//                        mtx[5] = info.data[comp].f; comp += 1
//                    }
//                } else throw Error("TODO handle matching point")
//                if (flags has (1 shl 3)) {
//                    // WE_HAVE_A_SCALE
//                    mtx[0] = info.data.s(comp) / 16384.0f; comp += 2
//                    mtx[1] = 0f
//                    mtx[2] = 0f
//                    mtx[3] = mtx[0]
//                } else if (flags has (1 shl 6)) {
//                    // WE_HAVE_AN_X_AND_YSCALE
//                    mtx[0] = info.data.s(comp) / 16384.0f; comp += 2
//                    mtx[1] = 0f
//                    mtx[2] = 0f
//                    mtx[3] = info.data.s(comp) / 16384.0f; comp += 2
//                } else if (flags has (1 shl 7)) {
//                    // WE_HAVE_A_TWO_BY_TWO
//                    mtx[0] = info.data.s(comp) / 16384.0f; comp += 2
//                    mtx[1] = info.data.s(comp) / 16384.0f; comp += 2
//                    mtx[2] = info.data.s(comp) / 16384.0f; comp += 2
//                    mtx[3] = info.data.s(comp) / 16384.0f; comp += 2
//                }
//
//                // Find transformation scales.
//                val m = glm.sqrt(mtx[0] * mtx[0] + mtx[1] * mtx[1])
//                val n = glm.sqrt(mtx[2] * mtx[2] + mtx[3] * mtx[3])
//
//                // Get indexed glyph.
//                val compVertsList = ArrayList<Vertex>()
//                compNumVerts = getGlyphShape(info, gidx, compVertsList)
//                if (compNumVerts > 0) {
//                    // Transform vertices.
//                    for (v in compVertsList) {
//                        var x = v.x
//                        var y = v.y
//                        v.x = (m * (mtx[0] * x + mtx[2] * y + mtx[4])).i
//                        v.y = (n * (mtx[1] * x + mtx[3] * y + mtx[5])).i
//                        x = v.cx
//                        y = v.cy
//                        v.cx = (m * (mtx[0] * x + mtx[2] * y + mtx[4])).i
//                        v.cy = (n * (mtx[1] * x + mtx[3] * y + mtx[5])).i
//                    }
//                    // Append vertices.
//                    TODO("find out what is doing")
////                    if (numVertices > 0) STBTT_memcpy(tmp, vertices, num_vertices * sizeof(stbtt_vertex))
////                    STBTT_memcpy(tmp + num_vertices, comp_verts, compNumVerts * sizeof(stbtt_vertex))
////                    if (vertices) STBTT_free(vertices, info->userdata)
////                    vertices = tmp
////                    STBTT_free(comp_verts, info->userdata)
////                    num_vertices += compNumVerts
//                }
//                // More components ?
//                more = flags has (1 shl 5)
//            }
//        } else if (numberOfContours < 0) throw Error("TODO other compound variations?")
//        // else {} numberOfCounters == 0, do nothing
//
//        pVertices.addAll(vertices)
//        return numVertices
//    }
//
//    fun getGlyphShapeT2(info: FontInfo, glyphIndex: Int, pVertices: ArrayList<Vertex>): Int {
//        // runs the charstring twice, once to count and once to output (to avoid realloc)
//        val countCtx = Csctx(1)
//        val outputCtx = Csctx(0)
//        if (runCharstring(info, glyphIndex, countCtx)) {
//            outputCtx.vertices = pVertices
//            if (runCharstring(info, glyphIndex, outputCtx)) {
//                assert(outputCtx.numVertices == countCtx.numVertices)
//                return outputCtx.numVertices
//            }
//        }
//        //pVertices = NULL; TODO check
//        return 0
//    }
//
//    fun closeShape(vertices: Array<Vertex>, numVertices: Int, wasOff: Boolean, startOff: Boolean, s: Vec2i, sc: Vec2i, c: Vec2i): Int {
//        var numVertices = numVertices
//        if (startOff) {
//            if (wasOff)
//                vertices[numVertices++].set(V.curve, (c.x + sc.x) ushr 1, (c.y + sc.y) ushr 1, c.x, c.y)
//            vertices[numVertices++].set(V.curve, s.x, s.y, sc.x, sc.y)
//        } else {
//            if (wasOff)
//                vertices[numVertices++].set(V.curve, s.x, s.y, c.x, c.y)
//            else
//                vertices[numVertices++].set(V.line, s.x, s.y, 0, 0)
//        }
//        return numVertices
//    }
//
//    private fun rasterize(result: Bitmap, flatnessInPixels: Float, vertices: ArrayList<Vertex>, numVerts: Int, scale: Vec2, shift: Vec2,
//                          off: Vec2i, invert: Int/*, void *userdata*/) {
//        val _scale = if (scale.x > scale.y) scale.y else scale.x
//        val windingCount = intArrayOf(0)
//        val windingLengths = ArrayList<Int>()
//        val windings = flattenCurves(vertices, numVerts, flatnessInPixels / _scale, windingLengths, windingCount/*, userdata*/)
//        if (windings) {
//            rasterize(result, windings, windingLengths, winding_count, scale_x, scale_y, shift_x, shift_y, x_off, y_off, invert, userdata);
//            STBTT_free(windingLengths, userdata);
//            STBTT_free(windings, userdata);
//        }
//    }
//
//    // returns number of contours
//    fun flattenCurves(vertices: ArrayList<Vertex>, numVerts: Int, objspaceFlatness: Float, contourLengths: ArrayList<Int>,
//                      numContours: IntArray/*, void *userdata*/): ArrayList<Point> {
//
//        var numPoints = 0
//
//        val objspaceFlatnessSquared = objspaceFlatness * objspaceFlatness
//        var n = 0
//        var start = 0
//
//        // count how many "moves" there are to get the contour count
//        for (i in 0 until numVerts)
//            if (vertices[i].type == V.move.i)
//                ++n
//
//        numContours[0] = n
//        if (n == 0) return 0;
//
//        *contour_lengths = (int *) STBTT_malloc (sizeof(** contour_lengths) * n, userdata);
//
//        if ( * contour_lengths == 0) {
//            *num_contours = 0;
//            return 0;
//        }
//
//        // make two passes through the points so we don't need to realloc
//        for (pass= 0; pass < 2; ++pass) {
//            float x =0, y = 0;
//            if (pass == 1) {
//                points = (stbtt__point *) STBTT_malloc (num_points * sizeof(points[0]), userdata);
//                if (points == NULL) goto error;
//            }
//            num_points = 0;
//            n = -1;
//            for (i= 0; i < num_verts; ++i) {
//            switch(vertices[i].type) {
//                case STBTT_vmove :
//                // start the next contour
//                if (n >= 0)
//                    ( * contour_lengths)[n] = num_points-start;
//                ++n;
//                start = num_points;
//
//                x = vertices[i].x, y = vertices[i].y;
//                stbtt__add_point(points, num_points++, x, y);
//                break;
//                case STBTT_vline :
//                x = vertices[i].x, y = vertices[i].y;
//                stbtt__add_point(points, num_points++, x, y);
//                break;
//                case STBTT_vcurve :
//                stbtt__tesselate_curve(points, & num_points, x, y,
//                vertices[i].cx, vertices[i].cy,
//                vertices[i].x, vertices[i].y,
//                objspaceFlatnessSquared, 0);
//                x = vertices[i].x, y = vertices[i].y;
//                break;
//                case STBTT_vcubic :
//                stbtt__tesselate_cubic(points, & num_points, x, y,
//                vertices[i].cx, vertices[i].cy,
//                vertices[i].cx1, vertices[i].cy1,
//                vertices[i].x, vertices[i].y,
//                objspaceFlatnessSquared, 0);
//                x = vertices[i].x, y = vertices[i].y;
//                break;
//            }
//        }
//            ( * contour_lengths)[n] = num_points-start;
//        }
//
//        return points;
//        error:
//        STBTT_free(points, userdata);
//        STBTT_free(*contour_lengths, userdata);
//        *contour_lengths = 0;
//        *num_contours = 0;
//        return NULL;
//    }
//
//    // TODO: don't expose this structure
//    private class Bitmap(var size: Vec2i, var stride: Int, var pixels: CharArray) {
//        var w
//            get() = size.x
//            set(value) {
//                size.x = value
//            }
//        var h
//            get() = size.y
//            set(value) {
//                size.y = value
//            }
//    }
//
//    typealias Point = Vec2
//
////////////////////////////////////////////////////////////////////////////////
////
//// NEW TEXTURE BAKING API
////
//// This provides options for packing multiple fonts into one atlas, not
//// perfectly but better than nothing.
//
//    class PackedChar {
//        /** coordinates of bbox in bitmap   */
//        var _0 = Vec2i()
//        /** coordinates of bbox in bitmap   */
//        var _1 = Vec2i()
//
//        var off = Vec2()
//        var xadvance = 0f
//        var off2 = Vec2()
//    }
//
//    /** platformID  */
//    enum class PlatformId(val i: Int) {UNICODE(0), MAC(1), ISO(2), MICROSOFT(3) }
//
//    /** encodingID for STBTT_PLATFORM_ID_UNICODE    */
//    enum class UnicodeEid(val i: Int) {UNICODE_1_0(0), UNICODE_1_1(1), ISO_10646(2), UNICODE_2_0_BMP(3), UNICODE_2_0_FULL(4) }
//
//    /** encodingID for STBTT_PLATFORM_ID_MICROSOFT  */
//    enum class MsEid { SYMBOL, UNICODE_BMP, SHIFTJIS, UNICODE_FULL;
//
//        val i = ordinal
//    }
//
//    /** encodingID for STBTT_PLATFORM_ID_MAC; same as Script Manager codes  */
//    enum class MacEid(val i: Int) { EID_ROMAN(0), JAPANESE(1), CHINESE_TRAD(2), KOREAN(3), ARABIC(4), HEBREW(5), GREEK(6), RUSSIAN(7) }
//
//    /** languageID for STBTT_PLATFORM_ID_MICROSOFT; same as LCID...
//     *  problematic because there are e.g. 16 english LCIDs and 16 arabic LCIDs */
//    enum class MsLang(val i: Int) { ENGLISH(0x0409), ITALIAN(0x0410), CHINESE(0x0804), JAPANESE(0x0411), DUTCH(0x0413),
//        KOREAN(0x0412), FRENCH(0x040c), RUSSIAN(0x0419), GERMAN(0x0407), SPANISH(0x0409), HEBREW(0x040d), SWEDISH(0x041D)
//    }
//
//    /** languageID for STBTT_PLATFORM_ID_MAC    */
//    enum class MacLang(val i: Int) { ENGLISH(0), JAPANESE(11), ARABIC(12), KOREAN(23), DUTCH(4), RUSSIAN(32), FRENCH(1),
//        SPANISH(6), GERMAN(2), SWEDISH(5), HEBREW(10), CHINESE_SIMPLIFIED(33), ITALIAN(3), CHINESE_TRAD(19)
//    }
//
//    enum class V(val i: Int) { move(1), line(2), curve(3), cubic(4) }
//
//    var MAX_OVERSAMPLE = 8
//}
//
