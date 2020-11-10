package imgui.font

import gli_.has
import glm_.*
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec2.operators.div
import glm_.vec2.operators.times
import glm_.vec4.Vec4
import imgui.ImGui.style
import imgui.MouseCursor
import imgui.TextureID
import imgui.internal.*
import imgui.stb.*
import imgui.toByteArray
import kool.*
import kool.lib.isNotEmpty
import org.lwjgl.stb.*
import org.lwjgl.system.libc.LibCString.nmemset
import uno.convert.decode85
import uno.kotlin.plusAssign
import uno.stb.stb
import unsigned.toUInt
import java.nio.ByteBuffer
import kotlin.math.floor
import kotlin.math.sqrt

/** Load and rasterize multiple TTF/OTF fonts into a same texture. The font atlas will build a single texture holding:
 *      - One or more fonts.
 *      - Custom graphics data needed to render the shapes needed by Dear ImGui.
 *      - Mouse cursor shapes for software cursor rendering (unless setting 'Flags |= ImFontAtlasFlags_NoMouseCursors' in the font atlas).
 *  It is the user-code responsibility to setup/build the atlas, then upload the pixel data into a texture accessible by your graphics api.
 *      - Optionally, call any of the AddFont*** functions. If you don't call any, the default font embedded in the code will be loaded for you.
 *      - Call GetTexDataAsAlpha8() or GetTexDataAsRGBA32() to build and retrieve pixels data.
 *      - Upload the pixels data into a texture within your graphics system (see imgui_impl_xxxx.cpp examples)
 *      - Call SetTexID(my_tex_id); and pass the pointer/identifier to your texture in a format natural to your graphics API.
 *  This value will be passed back to you during rendering to identify the texture. Read FAQ entry about ImTextureID for more details.
 *  Common pitfalls:
 *      - If you pass a 'glyph_ranges' array to AddFont*** functions, you need to make sure that your array persist up until the
 *          atlas is build (when calling GetTexData*** or Build()). We only copy the pointer, not the data.
 *      - Important: By default, AddFontFromMemoryTTF() takes ownership of the data. Even though we are not writing to it,
 *          we will free the pointer on destruction.
 *  You can set font_cfg->FontDataOwnedByAtlas=false to keep ownership of your data and it won't be freed,
 *      - Even though many functions are suffixed with "TTF", OTF data is supported just as well.
 *      - This is an old API and it is currently awkward for those and and various other reasons! We will address them in the future! */
class FontAtlas {

    fun addFont(fontCfg: FontConfig): Font {

        assert(!locked) { "Cannot modify a locked FontAtlas between NewFrame() and EndFrame/Render()!" }
        assert(fontCfg.fontData.isNotEmpty())
        assert(fontCfg.sizePixels > 0f)

        // Create new font
        if (!fontCfg.mergeMode)
            fonts += Font()
        else
        // When using MergeMode make sure that a font has already been added before. You can use io.fonts.addFontDefault to add the default imgui font.
            assert(fonts.isNotEmpty()) { "Cannot use MergeMode for the first font" }
        configData.add(fontCfg)
        if (fontCfg.dstFont == null)
            fontCfg.dstFont = fonts.last()
        if (!fontCfg.fontDataOwnedByAtlas)
            fontCfg.fontDataOwnedByAtlas = true
//            memcpy(new_font_cfg.FontData, font_cfg->FontData, (size_t)new_font_cfg.FontDataSize) TODO check, same object?

        if (fontCfg.dstFont!!.ellipsisChar == '\uffff')
            fontCfg.dstFont!!.ellipsisChar = fontCfg.ellipsisChar

        // Invalidate texture
        clearTexData()
        return fontCfg.dstFont!!
    }

    /** Load embedded ProggyClean.ttf at size 13, disable oversampling  */
    fun addFontDefault(fontCfgTemplate: FontConfig? = null): Font {

        val fontCfg = fontCfgTemplate ?: FontConfig()
        if (fontCfgTemplate == null) {
            fontCfg.oversample put 1
            fontCfg.pixelSnapH = true
        }
        if (fontCfg.sizePixels <= 0f)
            fontCfg.sizePixels = 13f * 1f
        if (fontCfg.name == "")
            formatString(fontCfg.name.toByteArray(32), "ProggyClean.ttf, ${fontCfg.sizePixels.i}px")
        fontCfg.ellipsisChar = '\u0085'
        fontCfg.glyphOffset.y = 1f * floor(fontCfg.sizePixels / 13f)  // Add +1 offset per 13 units

        val ttfCompressedBase85 = proggyCleanTtfCompressedDataBase85
        val glyphRanges = fontCfg.glyphRanges.takeIf { it.isNotEmpty() } ?: glyphRanges.default
        return addFontFromMemoryCompressedBase85TTF(ttfCompressedBase85, fontCfg.sizePixels, fontCfg, glyphRanges)
    }

    fun addFontFromFileTTF(filename: String, sizePixels: Float, fontCfg: FontConfig = FontConfig(),
                           glyphRanges: Array<IntRange> = arrayOf()): Font? {

        assert(!locked) { "Cannot modify a locked FontAtlas between NewFrame() and EndFrame/Render()!" }
        val chars = fileLoadToMemory(filename) ?: run {
            System.err.println("Could not load font file.")
            return null
        }
        if (fontCfg.name.isEmpty())
        // Store a short copy of filename into into the font name for convenience
            fontCfg.name = "${filename.substringAfterLast('/')}, %.0fpx".format(style.locale, sizePixels)
        return addFontFromMemoryTTF(chars, sizePixels, fontCfg, glyphRanges)
    }

    /** Note: Transfer ownership of 'ttfData' to FontAtlas! Will be deleted after destruction of the atlas.
     *  Set font_cfg->FontDataOwnedByAtlas=false to keep ownership of your data and it won't be freed. */
    fun addFontFromMemoryTTF(fontData: CharArray, sizePixels: Float, fontCfg: FontConfig = FontConfig(),
                             glyphRanges: Array<IntRange> = arrayOf()): Font {

        assert(!locked) { "Cannot modify a locked FontAtlas between NewFrame() and EndFrame/Render()!" }
        assert(fontCfg.fontData.isEmpty())
        fontCfg.fontData = fontData
        fontCfg.fontDataBuffer = Buffer(fontData.size).apply { fontData.forEachIndexed { i, c -> this[i] = c.b } }
        fontCfg.sizePixels = sizePixels
        if (glyphRanges.isNotEmpty())
            fontCfg.glyphRanges = glyphRanges
        return addFont(fontCfg)
    }

    /** @param compressedFontData still owned by caller. Compress with binary_to_compressed_c.cpp.   */
    fun addFontFromMemoryCompressedTTF(compressedFontData: CharArray, sizePixels: Float, fontCfg: FontConfig = FontConfig(),
                                       glyphRanges: Array<IntRange> = arrayOf()): Font {

        val bufDecompressedData = stb.decompress(compressedFontData)

        assert(fontCfg.fontData.isEmpty())
        fontCfg.fontDataOwnedByAtlas = true
        return addFontFromMemoryTTF(bufDecompressedData, sizePixels, fontCfg, glyphRanges)
    }

    /** @param compressedFontDataBase85 still owned by caller. Compress with binary_to_compressed_c.cpp with -base85
     *  paramaeter  */
    fun addFontFromMemoryCompressedBase85TTF(compressedFontDataBase85: String, sizePixels: Float, fontCfg: FontConfig = FontConfig(),
                                             glyphRanges: Array<IntRange> = arrayOf()): Font {

        val compressedTtf = decode85(compressedFontDataBase85)
        return addFontFromMemoryCompressedTTF(compressedTtf, sizePixels, fontCfg, glyphRanges)
    }

    /** Clear input data (all FontConfig structures including sizes, TTF data, glyph ranges, etc.) = all the data used
     *  to build the texture and fonts. */
    fun clearInputData() {
        assert(!locked) { "Cannot modify a locked FontAtlas between NewFrame() and EndFrame/Render()!" }
        configData.filter { it.fontData.isNotEmpty() && it.fontDataOwnedByAtlas }.forEach {
            it.fontData = charArrayOf()
            it.fontDataBuffer.free()
        }

        // When clearing this we lose access to  the font name and other information used to build the font.
        fonts.filter {
            if (it.configData.isNotEmpty()) configData.contains(it.configData[0]) else false
        }.forEach {
            it.configData.clear()
            it.configDataCount = 0
        }
        configData.clear()
        customRects.clear()
        packIdMouseCursors = -1
        packIdLines = -1
    }

    /** Clear output texture data (CPU side). Saves RAM once the texture has been copied to graphics memory. */
    fun clearTexData() {
        assert(!locked) { "Cannot modify a locked FontAtlas between NewFrame() and EndFrame/Render()!" }
        texPixelsAlpha8?.free()
        texPixelsAlpha8 = null
        texPixelsRGBA32?.free()
        texPixelsRGBA32 = null
    }

    /** Clear output font data (glyphs storage, UV coordinates).    */
    fun clearFonts() {
        assert(!locked) { "Cannot modify a locked FontAtlas between NewFrame() and EndFrame/Render()!" }
        for (font in fonts) font.clearOutputData()
        fonts.clear()
    }

    /** Clear all input and output. ~ destroy  */
    fun clear() {
        clearInputData()
        clearTexData()
        clearFonts()
        stbClear()
        texPixelsAlpha8?.free()
    }

    /*  Build atlas, retrieve pixel data.
        User is in charge of copying the pixels into graphics memory (e.g. create a texture with your engine).
        Then store your texture handle with setTexID().ClearInputData
        The pitch is always = Width * BytesPerPixels (1 or 4)
        Building in RGBA32 format is provided for convenience and compatibility, but note that unless
        you manually manipulate or copy color data into the texture (e.g. when using the AddCustomRect*** api),
        then the RGB pixels emitted will always be white (~75% of memory/bandwidth waste.  */

    /** Build pixels data. This is automatically for you by the GetTexData*** functions.    */
    fun build() {
        assert(!locked) { "Cannot modify a locked FontAtlas between NewFrame() and EndFrame/Render()!" }
        buildWithStbTrueType()
    }

    /** 1 byte per-pixel    */
    fun getTexDataAsAlpha8(): Triple<ByteBuffer, Vec2i, Int> {

        // Build atlas on demand
        if (texPixelsAlpha8 == null) {
            if (configData.isEmpty())
                addFontDefault()
            build()
        }
        return Triple(texPixelsAlpha8!!, texSize, 1)
    }

    /** 4 bytes-per-pixel   */
    fun getTexDataAsRGBA32(): Triple<ByteBuffer, Vec2i, Int> {
        /*  Convert to RGBA32 format on demand
            Although it is likely to be the most commonly used format, our font rendering is 1 channel / 8 bpp         */
        if (texPixelsRGBA32 == null) {
            val (pixels, _, _) = getTexDataAsAlpha8()
            if (pixels.isNotEmpty()) {
                texPixelsRGBA32 = Buffer(texSize.x * texSize.y * 4)
                val dst = texPixelsRGBA32!!
                for (n in 0 until pixels.rem) {
                    dst[n * 4] = 255.b
                    dst[n * 4 + 1] = 255.b
                    dst[n * 4 + 2] = 255.b
                    dst[n * 4 + 3] = pixels[n]
                }
            }
        }

        return Triple(texPixelsRGBA32!!, texSize, 4)
    }

    val isBuilt: Boolean
        get() = fonts.size > 0 && (texPixelsAlpha8 != null || texPixelsRGBA32 != null)


//    //-----------------------------------------------------------------------------
//    +// ImFontAtlas::GlyphRangesBuilder
//    +//-----------------------------------------------------------------------------
//    +
//    +void ImFontAtlas::GlyphRangesBuilder::AddText(const char* text, const char* text_end)
//    +{
//        while (text_end ? (text < text_end) : *text)
//        {
//                unsigned int c = 0;
//                int c_len = ImTextCharFromUtf8(&c, text, text_end);
//                text += c_len;
//                if (c_len == 0)
//                        break;
//                if (c < 0x10000)
//                        AddChar((ImWchar)c);
//            }
//        +}
//    +
//    +void ImFontAtlas::GlyphRangesBuilder::AddRanges(const ImWchar* ranges)
//    +{
//        for (; ranges[0]; ranges += 2)
//            for (ImWchar c = ranges[0]; c <= ranges[1]; c++)
//                AddChar(c);
//        +}
//    +
//    +void ImFontAtlas::GlyphRangesBuilder::BuildRanges(ImVector<ImWchar>* out_ranges)
//    +{
//          int max_codepoint = 0x10000;
//          for (int n = 0; n <= UNICODE_CODEPOINT_MAX; n++)
//            if (GetBit(n))
//                {
//                        out_ranges->push_back((ImWchar)n);
//                        while (n < max_codepoint && GetBit(n + 1))
//                                n++;
//                        out_ranges->push_back((ImWchar)n);
//                    }
//        out_ranges->push_back(0);
//        +}


    //-------------------------------------------
    // [BETA] Custom Rectangles/Glyphs API
    //-------------------------------------------

    /** You can request arbitrary rectangles to be packed into the atlas, for your own purposes.
     *  After calling Build(), you can query the rectangle position and render your pixels.
     *  You can also request your rectangles to be mapped as font glyph (given a font + Unicode point),
     *  so you can render e.g. custom colorful icons and use them as regular glyphs.
     *  Read docs/FONTS.txt for more details about using colorful icons.
     *  Note: this API may be redesigned later in order to support multi-monitor varying DPI settings. */
    class CustomRect {

        /** Input    // Desired rectangle dimension */
        var width = 0

        /** Input    // Desired rectangle dimension */
        var height = 0

        /** Output   // Packed position in Atlas  */
        var x = 0xFFFF

        /** Output   // Packed position in Atlas  */
        var y = 0xFFFF

        /** Input, For custom font glyphs only (ID < 0x110000) */
        var glyphID = 0

        /** Input, For custom font glyphs only: glyph xadvance */
        var glyphAdvanceX = 0f

        /** Input, For custom font glyphs only: glyph display offset */
        val glyphOffset = Vec2()

        /** Input, For custom font glyphs only: target font */
        var font: Font? = null

        val isPacked: Boolean
            get() = x != 0xFFFF

        constructor()

        constructor(r: CustomRect) {
            width = r.width
            height = r.height
            x = r.x
            y = r.y
            glyphID = r.glyphID
            glyphAdvanceX = r.glyphAdvanceX
            glyphOffset put r.glyphOffset
            font = r.font
        }
    }

    fun addCustomRectRegular(width: Int, height: Int): Int {
        assert(width in 0..0xFFFF && height in 0..0xFFFF)
        val r = CustomRect()
        r.width = width
        r.height = height
        customRects += r
        return customRects.lastIndex
    }

    fun addCustomRectFontGlyph(font: Font, id: Int, width: Int, height: Int, advanceX: Float, offset: Vec2 = Vec2()): Int {
//        #ifdef IMGUI_USE_WCHAR32
//                IM_ASSERT(id <= IM_UNICODE_CODEPOINT_MAX);
//        #endif
//        IM_ASSERT(font != NULL);
        assert(width in 1..0xFFFF)
        assert(height in 1..0xFFFF)
        val r = CustomRect()
        r.width = width
        r.height = height
        r.glyphID = id
        r.glyphAdvanceX = advanceX
        r.glyphOffset put offset
        r.font = font
        customRects.add(r)
        return customRects.lastIndex // Return index
    }

    fun getCustomRectByIndex(index: Int): CustomRect {
        assert(index >= 0)
        return customRects[index]
    }

    // Internals

    fun calcCustomRectUV(rect: CustomRect, outUvMin: Vec2, outUvMax: Vec2) {
        assert(texSize allGreaterThan 0) { "Font atlas needs to be built before we can calculate UV coordinates" }
        assert(rect.isPacked) { "Make sure the rectangle has been packed" }
        outUvMin.put(rect.x.f * texUvScale.x, rect.y.f * texUvScale.y)
        outUvMax.put((rect.x + rect.width).f * texUvScale.x, (rect.y + rect.height).f * texUvScale.y)
    }

    fun getMouseCursorTexData(cursor: MouseCursor, outOffset: Vec2, outSize: Vec2, outUv: Array<Vec2>): Boolean {

        if (cursor == MouseCursor.None) return false

        if (flags has Flag.NoMouseCursors) return false

        assert(packIdMouseCursors != -1)
        val r = getCustomRectByIndex(packIdMouseCursors)
        val pos = DefaultTexData.cursorDatas[cursor.i][0] + Vec2(r.x, r.y)
        val size = DefaultTexData.cursorDatas[cursor.i][1]
        outSize put size
        outOffset put DefaultTexData.cursorDatas[cursor.i][2]
        // JVM border
        outUv[0] = pos * texUvScale
        outUv[1] = (pos + size) * texUvScale
        pos.x += DefaultTexData.w + 1
        // JVM fill
        outUv[2] = pos * texUvScale
        outUv[3] = (pos + size) * texUvScale
        return true
    }

    //-------------------------------------------
    // Members
    //-------------------------------------------

    /** Flags: for ImFontAtlas build */
    enum class Flag {
        None,

        /** Don't round the height to next power of two */
        NoPowerOfTwoHeight,

        /** Don't build software mouse cursors into the atlas (save a little texture memory) */
        NoMouseCursors,

        /** Don't build thick line textures into the atlas (save a little texture memory). The AntiAliasedLinesUseTex
         *  features uses them, otherwise they will be rendered using polygons (more expensive for CPU/GPU). */
        NoBakedLines;

        val i = if (ordinal == 0) 0 else 1 shl ordinal
    }

    infix fun Int.has(flag: Flag) = and(flag.i) != 0
    infix fun Int.hasnt(flag: Flag) = and(flag.i) == 0

    /** Marked as Locked by ImGui::NewFrame() so attempt to modify the atlas will assert. */
    var locked = false

    /** Build flags (see ImFontAtlasFlags_) */
    var flags = Flag.None.i

    /** User data to refer to the texture once it has been uploaded to user's graphic systems. It is passed back to you
    during rendering via the DrawCmd structure.   */
    var texID: TextureID = 0

    /** 1 component per pixel, each component is unsigned 8-bit. Total size = texSize.x * texSize.y  */
    var texPixelsAlpha8: ByteBuffer? = null

    /** 4 component per pixel, each component is unsigned 8-bit. Total size = texSize.x * texSize.y * 4  */
    var texPixelsRGBA32: ByteBuffer? = null

    /** Texture size calculated during Build(). */
    var texSize = Vec2i()

    /** Texture width desired by user before Build(). Must be a power-of-two. If have many glyphs your graphics API have
     *  texture size restrictions you may want to increase texture width to decrease height.    */
    var texDesiredWidth = 0

    /** Padding between glyphs within texture in pixels. Defaults to 1.
     *  If your rendering method doesn't rely on bilinear filtering you may set this to 0. */
    var texGlyphPadding = 1

    /** = (1.0f/TexWidth, 1.0f/TexHeight)   */
    var texUvScale = Vec2()

    /** Texture coordinates to a white pixel    */
    var texUvWhitePixel = Vec2()

    /** Hold all the fonts returned by AddFont*. Fonts[0] is the default font upon calling ImGui::NewFrame(), use
     *  ImGui::PushFont()/PopFont() to change the current font. */
    val fonts = ArrayList<Font>()

    /** Rectangles for packing custom texture data into the atlas.  */
    private val customRects = ArrayList<CustomRect>()

    /** Configuration data */
    private val configData = ArrayList<FontConfig>()

    /** UVs for baked anti-aliased lines */
    val texUvLines = Array(DRAWLIST_TEX_LINES_WIDTH_MAX + 1) { Vec4() }


    // [Internal] Packing data

    /** Custom texture rectangle ID for white pixel and mouse cursors */
    private var packIdMouseCursors = -1

    /** Custom texture rectangle ID for baked anti-aliased lines */
    private var packIdLines = -1


    private fun customRectCalcUV(rect: CustomRect, outUvMin: Vec2, outUvMax: Vec2) {
        assert(texSize.x > 0 && texSize.y > 0) { "Font atlas needs to be built before we can calculate UV coordinates" }
        assert(rect.isPacked) { "Make sure the rectangle has been packed " }
        outUvMin.put(rect.x / texSize.x, rect.y / texSize.y)
        outUvMax.put((rect.x + rect.width) / texSize.x, (rect.y + rect.height) / texSize.y)
    }


    // Helper: ImBitArray

    /** Helper: ImBoolVector. Store 1-bit per value.
     *  Note that Resize() currently clears the whole vector. */
    class BitVector(sz: Int) { // ~create
        var storage = IntArray((sz + 31) ushr 5)

        // Helpers: Bit arrays
        infix fun IntArray.testBit(n: Int): Boolean {
            val mask = 1 shl (n and 31); return (this[n ushr 5] and mask).bool; }

        infix fun IntArray.clearBit(n: Int) {
            val mask = 1 shl (n and 31); this[n ushr 5] = this[n ushr 5] wo mask; }

        infix fun IntArray.setBit(n: Int) {
            val mask = 1 shl (n and 31); this[n ushr 5] = this[n ushr 5] or mask; }

        fun IntArray.setBitRange(n_: Int, n2: Int) {
            var n = n_
            while (n <= n2) {
                val aMod = n and 31
                val bMod = (if (n2 >= n + 31) 31 else n2 and 31) + 1
                val mask = ((1L shl bMod) - 1).toUInt() wo ((1L shl aMod) - 1).toUInt()
                this[n ushr 5] = this[n ushr 5] or mask
                n = (n + 32) wo 31
            }
        }

        fun clear() {
            storage = IntArray(0)
        }

        infix fun testBit(n: Int): Boolean {
            assert(n < storage.size shl 5); return storage testBit n; }

        infix fun setBit(n: Int) {
            assert(n < storage.size shl 5); storage setBit n; }

        infix fun clearBit(n: Int) {
            assert(n < storage.size shl 5); storage clearBit n; }

        fun unpack(): ArrayList<Int> {
            val res = arrayListOf<Int>()
            storage.forEachIndexed { index, entries32 ->
                if (entries32 != 0)
                    for (bitN in 0..31)
                        if (entries32 has (1 shl bitN))
                            res += (index shl 5) + bitN
            }
            return res
        }
    }

    /** Temporary data for one source font (multiple source fonts can be merged into one destination ImFont)
     *  (C++03 doesn't allow instancing ImVector<> with function-local types so we declare the type here.) */
    class FontBuildSrcData {

        val fontInfo = STBTTFontinfo.calloc()

        /** Hold the list of codepoints to pack (essentially points to Codepoints.Data) */
        val packRange = STBTTPackRange.calloc()

        /** Rectangle to pack. We first fill in their size and the packer will give us their position. */
        lateinit var rects: STBRPRect.Buffer

        /** Output glyphs */
        lateinit var packedChars: STBTTPackedchar.Buffer

        /** Ranges as requested by user (user is allowed to request too much, e.g. 0x0020..0xFFFF) */
        lateinit var srcRanges: Array<IntRange>

        /** Index into atlas->Fonts[] and dst_tmp_array[] */
        var dstIndex = 0

        /** Highest requested codepoint */
        var glyphsHighest = 0

        /** Glyph count (excluding missing glyphs and glyphs already set by an earlier source font) */
        var glyphsCount = 0

        /** Glyph bit map (random access, 1-bit per codepoint. This will be a maximum of 8KB) */
        lateinit var glyphsSet: BitVector

        /** Glyph codepoints list (flattened version of GlyphsMap) */
        lateinit var glyphsList: ArrayList<Int>

        fun free() {
            fontInfo.free()
            packRange.arrayOfUnicodeCodepoints?.free()
            packRange.free()
            // dummies
//            rects.free()
//            packedChars.free()
        }
    }

    /** Temporary data for one destination ImFont* (multiple source fonts can be merged into one destination ImFont) */
    class FontBuildDstData {
        /** Number of source fonts targeting this destination font. */
        var srcCount = 0
        var glyphsHighest = 0
        var glyphsCount = 0

        /** This is used to resolve collision when multiple sources are merged into a same destination font. */
        var glyphsSet: BitVector? = null
    }

    // ImFontAtlas internals

    fun buildWithStbTrueType(): Boolean {

        assert(configData.isNotEmpty())

        buildInit()

        // Clear atlas
        texID = 0
        texSize put 0
        texUvScale put 0f
        texUvWhitePixel put 0f
        clearTexData()

        // Temporary storage for building
        val srcTmpArray = List(configData.size) { FontBuildSrcData() }
        val dstTmpArray = List(fonts.size) { FontBuildDstData() }

        // 1. Initialize font loading structure, check font data validity
        for (srcIdx in configData.indices) {
            val srcTmp = srcTmpArray[srcIdx]
            val cfg = configData[srcIdx]
            assert(cfg.dstFont?.isLoaded == false || cfg.dstFont?.containerAtlas === this)

            // Find index from cfg.DstFont (we allow the user to set cfg.DstFont. Also it makes casual debugging nicer than when storing indices)
            srcTmp.dstIndex = -1
            var outputIdx = 0
            while (outputIdx < fonts.size && srcTmp.dstIndex == -1) {
                if (cfg.dstFont == fonts[outputIdx])
                    srcTmp.dstIndex = outputIdx
                outputIdx++
            }
            assert(srcTmp.dstIndex != -1) { "cfg.DstFont not pointing within atlas->Fonts[] array?" }
            if (srcTmp.dstIndex == -1)
                return false

            // Initialize helper structure for font loading and verify that the TTF/OTF data is correct
            val fontOffset = STBTruetype.stbtt_GetFontOffsetForIndex(cfg.fontDataBuffer, cfg.fontNo)
            assert(fontOffset >= 0) { "FontData is incorrect, or FontNo cannot be found." }
            if (!STBTruetype.stbtt_InitFont(srcTmp.fontInfo, cfg.fontDataBuffer, fontOffset))
                return false

            // Measure highest codepoints
            val dstTmp = dstTmpArray[srcTmp.dstIndex]
            srcTmp.srcRanges = cfg.glyphRanges.takeIf { it.isNotEmpty() } ?: glyphRanges.default
            for (srcRange in srcTmp.srcRanges)
                srcTmp.glyphsHighest = srcTmp.glyphsHighest max srcRange.endInclusive
            dstTmp.srcCount++
            dstTmp.glyphsHighest = dstTmp.glyphsHighest max srcTmp.glyphsHighest
        }

        // 2. For every requested codepoint, check for their presence in the font data, and handle redundancy or overlaps between source fonts to avoid unused glyphs.
        var totalGlyphsCount = 0
        for (srcIdx in srcTmpArray.indices) {
            val srcTmp = srcTmpArray[srcIdx]
            val dstTmp = dstTmpArray[srcTmp.dstIndex]
            srcTmp.glyphsSet = BitVector(srcTmp.glyphsHighest + 1)
            if (dstTmp.glyphsSet == null)
                dstTmp.glyphsSet = BitVector(dstTmp.glyphsHighest + 1)

            for (srcRange in srcTmp.srcRanges)
                for (codepoint in srcRange) {
                    if (dstTmp.glyphsSet!! testBit codepoint)   // Don't overwrite existing glyphs. We could make this an option for MergeMode (e.g. MergeOverwrite==true)
                        continue
                    if (!STBTruetype.stbtt_FindGlyphIndex(srcTmp.fontInfo, codepoint).bool)    // It is actually in the font?
                        continue

                    // Add to avail set/counters
                    srcTmp.glyphsCount++
                    dstTmp.glyphsCount++
                    srcTmp.glyphsSet setBit codepoint
                    dstTmp.glyphsSet!! setBit codepoint
                    totalGlyphsCount++
                }
        }

        // 3. Unpack our bit map into a flat list (we now have all the Unicode points that we know are requested _and_ available _and_ not overlapping another)
        for (srcIdx in srcTmpArray.indices) {
            val srcTmp = srcTmpArray[srcIdx]
            srcTmp.glyphsList = srcTmp.glyphsSet.unpack()
            srcTmp.glyphsSet.clear()
            assert(srcTmp.glyphsList.size == srcTmp.glyphsCount)
        }
        dstTmpArray.forEach { it.glyphsSet!!.clear() }
//        dstTmpArray.clear()

        // Allocate packing character data and flag packed characters buffer as non-packed (x0=y0=x1=y1=0)
        // (We technically don't need to zero-clear buf_rects, but let's do it for the sake of sanity)
        val bufRects = STBRPRect.calloc(totalGlyphsCount)
        val bufPackedchars = STBTTPackedchar.calloc(totalGlyphsCount)

        // 4. Gather glyphs sizes so we can pack them in our virtual canvas.
        var totalSurface = 0
        var bufRectsOutN = 0
        var bufPackedcharsOutN = 0
        for (srcIdx in srcTmpArray.indices) {
            val srcTmp = srcTmpArray[srcIdx]
            if (srcTmp.glyphsCount == 0)
                continue

            srcTmp.rects = STBRPRect.create(bufRects.adr + bufRectsOutN * STBRPRect.SIZEOF, srcTmp.glyphsCount)
            srcTmp.packedChars = STBTTPackedchar.create(bufPackedchars.adr + bufPackedcharsOutN * STBTTPackedchar.SIZEOF, srcTmp.glyphsCount)
            bufRectsOutN += srcTmp.glyphsCount
            bufPackedcharsOutN += srcTmp.glyphsCount

            // Convert our ranges in the format stb_truetype wants
            val cfg = configData[srcIdx]
            srcTmp.packRange.apply {
                fontSize = cfg.sizePixels
                firstUnicodeCodepointInRange = 0
                arrayOfUnicodeCodepoints = srcTmp.glyphsList.toIntArray().toIntBuffer()
                numChars = srcTmp.glyphsList.size
                chardataForRange = srcTmp.packedChars
                oversample = cfg.oversample
            }
            // Gather the sizes of all rectangles we will need to pack (this loop is based on stbtt_PackFontRangesGatherRects)
            val scale = when {
                cfg.sizePixels > 0 -> STBTruetype.stbtt_ScaleForPixelHeight(srcTmp.fontInfo, cfg.sizePixels)
                else -> STBTruetype.stbtt_ScaleForMappingEmToPixels(srcTmp.fontInfo, -cfg.sizePixels)
            }
            val padding = texGlyphPadding
            for (glyphIdx in srcTmp.glyphsList.indices) {
                val glyphIndexInFont = STBTruetype.stbtt_FindGlyphIndex(srcTmp.fontInfo, srcTmp.glyphsList[glyphIdx])
                assert(glyphIndexInFont != 0)
                val (x0, y0, x1, y1) = stbtt_GetGlyphBitmapBoxSubpixel(srcTmp.fontInfo, glyphIndexInFont, scale * Vec2(cfg.oversample))
                srcTmp.rects[glyphIdx].apply {
                    w = x1 - x0 + padding + cfg.oversample.x - 1
                    h = y1 - y0 + padding + cfg.oversample.y - 1
                    totalSurface += w * h
                }
            }
        }

        // We need a width for the skyline algorithm, any width!
        // The exact width doesn't really matter much, but some API/GPU have texture size limitations and increasing width can decrease height.
        // User can override TexDesiredWidth and TexGlyphPadding if they wish, otherwise we use a simple heuristic to select the width based on expected surface.
        texSize.put(x = when {
            texDesiredWidth > 0 -> texDesiredWidth
            else -> {
                val surfaceSqrt = sqrt(totalSurface.f) + 1
                when {
                    surfaceSqrt >= 4096 * 0.7f -> 4096
                    else -> when {
                        surfaceSqrt >= 2048 * 0.7f -> 2048
                        else -> when {
                            surfaceSqrt >= 1024 * 0.7f -> 1024
                            else -> 512
                        }
                    }
                }
            }
        }, y = 0)

        // 5. Start packing
        // Pack our extra data rectangles first, so it will be on the upper-left corner of our texture (UV will have small values).
        val TEX_HEIGHT_MAX = 1024 * 32
        val spc = STBTTPackContext.calloc()
        STBTruetype.stbtt_PackBegin(spc, null, texSize.x, TEX_HEIGHT_MAX, 0, texGlyphPadding)
        buildPackCustomRects(spc.packInfo)

        // 6. Pack each source font. No rendering yet, we are working with rectangles in an infinitely tall texture at this point.
        for (srcTmp in srcTmpArray) {
            if (srcTmp.glyphsCount == 0)
                continue

            STBRectPack.stbrp_pack_rects(spc.packInfo, srcTmp.rects)

            // Extend texture height and mark missing glyphs as non-packed so we won't render them.
            // FIXME: We are not handling packing failure here (would happen if we got off TEX_HEIGHT_MAX or if a single if larger than TexWidth?)
            for (glyphIdx in 0 until srcTmp.glyphsCount)
                if (srcTmp.rects[glyphIdx].wasPacked)
                    texSize.y = texSize.y max (srcTmp.rects[glyphIdx].y + srcTmp.rects[glyphIdx].h)
        }

        // 7. Allocate texture
        texSize.y = when {
            flags has Flag.NoPowerOfTwoHeight -> texSize.y + 1
            else -> texSize.y.upperPowerOfTwo
        }
        texUvScale = 1f / Vec2(texSize)
        texPixelsAlpha8 = Buffer(texSize.x * texSize.y)
        spc.pixels = texPixelsAlpha8!!
        spc.height = texSize.y
        spc.width = texSize.x

        // 8. Render/rasterize font characters into the texture
        for (srcIdx in srcTmpArray.indices) {
            val cfg = configData[srcIdx]
            val srcTmp = srcTmpArray[srcIdx]
            if (srcTmp.glyphsCount == 0)
                continue

            stbtt_PackFontRangesRenderIntoRects(spc, srcTmp.fontInfo, srcTmp.packRange, srcTmp.rects)

            // Apply multiply operator
            if (cfg.rasterizerMultiply != 1f) {
                val multiplyTable = buildMultiplyCalcLookupTable(cfg.rasterizerMultiply)
                for (glyphIdx in 0 until srcTmp.glyphsCount) {
                    val r = srcTmp.rects[glyphIdx]
                    if (r.wasPacked)
                        buildMultiplyRectAlpha8(multiplyTable, texPixelsAlpha8!!, r, texSize.x)
                }
            }
//            srcTmp.rects = NULL // JVM dont free, it's a dummy container custom offset'ed
        }

        // End packing
        STBTruetype.stbtt_PackEnd(spc)
        spc.free()
        bufRects.free()

        // 9. Setup ImFont and glyphs for runtime
        val q = STBTTAlignedQuad.calloc()
        for (srcIdx in srcTmpArray.indices) {
            val srcTmp = srcTmpArray[srcIdx]
            if (srcTmp.glyphsCount == 0)
                continue

            // When merging fonts with MergeMode=true:
            // - We can have multiple input fonts writing into a same destination font.
            // - dst_font->ConfigData is != from cfg which is our source configuration.
            val cfg = configData[srcIdx]
            val dstFont = cfg.dstFont!!

            val fontScale = STBTruetype.stbtt_ScaleForPixelHeight(srcTmp.fontInfo, cfg.sizePixels)
            val (unscaledAscent, unscaledDescent, _) = stbtt_GetFontVMetrics(srcTmp.fontInfo)

            val ascent = floor(unscaledAscent * fontScale + if (unscaledAscent > 0f) +1 else -1)
            val descent = floor(unscaledDescent * fontScale + if (unscaledDescent > 0f) +1 else -1)
            buildSetupFont(dstFont, cfg, ascent, descent)
            val fontOff = Vec2(cfg.glyphOffset).apply { y += round(dstFont.ascent) }

            for (glyphIdx in 0 until srcTmp.glyphsCount) {
                // Register glyph
                val codepoint = srcTmp.glyphsList[glyphIdx]
                val pc = srcTmp.packedChars[glyphIdx]
                stbtt_GetPackedQuad(srcTmp.packedChars, texSize, glyphIdx, q)
                dstFont.addGlyph(cfg, codepoint, q.x0 + fontOff.x, q.y0 + fontOff.y,
                        q.x1 + fontOff.x, q.y1 + fontOff.y, q.s0, q.t0, q.s1, q.t1, pc.xAdvance)
            }
        }
        bufPackedchars.free()

        // Cleanup temporary (ImVector doesn't honor destructor)
        srcTmpArray.forEach { it.free() }
        q.free()

        buildFinish()
        return true
    }

    /** ~ImFontAtlasBuildInit
     *  Note: this is called / shared by both the stb_truetype and the FreeType builder */
    fun buildInit() {
        // Register texture region for mouse cursors or standard white pixels
        if (packIdMouseCursors < 0)
            packIdMouseCursors = when {
                flags hasnt Flag.NoMouseCursors -> addCustomRectRegular(DefaultTexData.w * 2 + 1, DefaultTexData.h)
                else -> addCustomRectRegular(2, 2)
            }

        // Register texture region for thick lines
        // The +2 here is to give space for the end caps, whilst height +1 is to accommodate the fact we have
        // a zero-width row
        if (packIdLines < 0)
            if (flags hasnt Flag.NoBakedLines)
                packIdLines = addCustomRectRegular(DRAWLIST_TEX_LINES_WIDTH_MAX + 2, DRAWLIST_TEX_LINES_WIDTH_MAX + 1)
    }

    fun buildSetupFont(font: Font, fontConfig: FontConfig, ascent: Float, descent: Float) {
        if (!fontConfig.mergeMode)
            font.apply {
                containerAtlas = this@FontAtlas
                configData += fontConfig
                configDataCount = 0
                fontSize = fontConfig.sizePixels
                this.ascent = ascent
                this.descent = descent
                glyphs.clear()
                metricsTotalSurface = 0
            }
        font.configDataCount++
    }

    /** ~ ImFontAtlasBuildPackCustomRects */
    fun buildPackCustomRects(stbrpContext: STBRPContext) {

        val userRects = customRects
        // We expect at least the default custom rects to be registered, else something went wrong.
        assert(userRects.isNotEmpty())
        val packRects = STBRPRect.calloc(userRects.size)    // calloc -> all 0
        for (i in userRects.indices) {
            packRects[i].w = userRects[i].width
            packRects[i].h = userRects[i].height
        }
        STBRectPack.stbrp_pack_rects(stbrpContext, packRects)
        for (i in userRects.indices)
            if (packRects[i].wasPacked) {
                userRects[i].x = packRects[i].x
                userRects[i].y = packRects[i].y
                assert(packRects[i].w == userRects[i].width && packRects[i].h == userRects[i].height)
                texSize.y = glm.max(texSize.y, packRects[i].y + packRects[i].h)
            }
        packRects.free()
    }

    /** ~ImFontAtlasBuildFinish
     *
     *  This is called/shared by both the stb_truetype and the FreeType builder. */
    fun buildFinish() {
        // Render into our custom data blocks
        assert(texPixelsAlpha8 != null)
        buildRenderDefaultTexData()
        buildRenderLinesTexData(this)

        // Register custom rectangle glyphs
        for (r in customRects) {
            val font = r.font
            if (font == null || r.glyphID == 0)
                continue

            // Will ignore ImFontConfig settings: GlyphMinAdvanceX, GlyphMinAdvanceY, GlyphExtraSpacing, PixelSnapH
            assert(font.containerAtlas === this)
            val uv0 = Vec2()
            val uv1 = Vec2()
            calcCustomRectUV(r, uv0, uv1)
            font.addGlyph(null, r.glyphID, r.glyphOffset.x, r.glyphOffset.y, r.glyphOffset.x + r.width, r.glyphOffset.y + r.height,
                    uv0.x, uv0.y, uv1.x, uv1.y, r.glyphAdvanceX)
        }
        // Build all fonts lookup tables
        fonts.filter { it.dirtyLookupTables }.forEach { it.buildLookupTable() }

        // Ellipsis character is required for rendering elided text. We prefer using U+2026 (horizontal ellipsis).
        // However some old fonts may contain ellipsis at U+0085. Here we auto-detect most suitable ellipsis character.
        // FIXME: Also note that 0x2026 is currently seldom included in our font ranges. Because of this we are more likely to use three individual dots.
        fonts.filter { it.ellipsisChar == '\uffff' }.forEach { font ->
            for (ellipsisVariant in charArrayOf('\u2026', '\u0085')) {
                if (font.findGlyphNoFallback(ellipsisVariant) != null) { // Verify glyph exists
                    font.ellipsisChar = ellipsisVariant
                    break
                }
            }
        }
    }

    fun buildRender1bppRectFromString(x: Int, y: Int, w: Int, h: Int,
                                      inStr: CharArray, inMarkerChar: Char, inMarkerPixelValue: Byte) {
        assert(x >= 0 && x + w <= texSize.x)
        assert(y >= 0 && y + h <= texSize.y)
        val outPixel = texPixelsAlpha8!!
        var ptr = x + y * texSize.x
        var ptr2 = 0
        var offY = 0
        while (offY < h) {
            for (offX in 0 until w)
                outPixel[ptr + offX] = if(inStr[offX] == inMarkerChar) inMarkerPixelValue else 0x00
            offY++
            ptr += texSize.x
            ptr2 += w
        }
    }

    /** ~ImFontAtlasBuildRenderDefaultTexData */
    fun buildRenderDefaultTexData() {

        val r = getCustomRectByIndex(packIdMouseCursors)
        assert(r.isPacked)

        val w = texSize.x
        if (flags hasnt Flag.NoMouseCursors) {
            // Render/copy pixels
            assert(r.width == DefaultTexData.w * 2 + 1 && r.height == DefaultTexData.h)
            val xForWhite = r.x
            val xForBlack = r.x + DefaultTexData.w + 1
            buildRender1bppRectFromString(xForWhite, r.y, DefaultTexData.w, DefaultTexData.h, DefaultTexData.pixels, '.', 0xFF.b)
            buildRender1bppRectFromString(xForBlack, r.y, DefaultTexData.w, DefaultTexData.h, DefaultTexData.pixels, 'X', 0xFF.b)
        } else {
            // Render 4 white pixels
            assert(r.width == 2 && r.height == 2)
            val offset = r.x.i + r.y.i * w
            with(texPixelsAlpha8!!) {
                put(offset + w + 1, 0xFF.b)
                put(offset + w, 0xFF.b)
                put(offset + 1, 0xFF.b)
                put(offset, 0xFF.b)
            }
        }
        texUvWhitePixel = (Vec2(r.x, r.y) + 0.5f) * texUvScale
    }

    fun buildMultiplyCalcLookupTable(inBrightenFactor: Float) = CharArray(256) {
        val value = (it * inBrightenFactor).i
        (if (value > 255) 255 else (value and 0xFF)).c
    }

    fun buildMultiplyRectAlpha8(table: CharArray, pixels: ByteBuffer, rect: STBRPRect, stride: Int) {
        var ptr = rect.x + rect.y * stride
        var j = rect.h
        while (j > 0) {
            for (i in 0 until rect.w)
                pixels[ptr + i] = table[pixels[ptr + i].i].b
            j--
            ptr += stride
        }
    }

    companion object {

        fun buildRenderLinesTexData(atlas: FontAtlas) {

            if (atlas.flags has Flag.NoBakedLines.i)
                return

            // This generates a triangular shape in the texture, with the various line widths stacked on top of each other to allow interpolation between them
            val r = atlas.getCustomRectByIndex(atlas.packIdLines)
            assert(r.isPacked)
            for (n in 0 until DRAWLIST_TEX_LINES_WIDTH_MAX + 1) { // +1 because of the zero-width row { // +1 because of the zero-width row
                // Each line consists of at least two empty pixels at the ends, with a line of solid pixels in the middle
                val y = n
                val lineWidth = n
                val padLeft = (r.width - lineWidth) / 2
                val padRight = r.width - (padLeft + lineWidth)

                // Write each slice
                assert(padLeft + lineWidth + padRight == r.width && y < r.height){"Make sure we're inside the texture bounds before we start writing pixels"}
                val writePtr = atlas.texPixelsAlpha8!!.adr + r.x + (r.y + y) * atlas.texSize.x
                nmemset(writePtr, 0x00, padLeft.L)
                nmemset(writePtr + padLeft, 0xFF, lineWidth.L)
                nmemset(writePtr + padLeft + lineWidth, 0x00, padRight.L)

                // Calculate UVs for this line
                val uv0 = Vec2(r.x + padLeft - 1, r.y + y) * atlas.texUvScale
                val uv1 = Vec2(r.x + padLeft + lineWidth + 1, r.y + y + 1) * atlas.texUvScale
                val halfV = (uv0.y + uv1.y) * 0.5f // Calculate a constant V in the middle of the row to avoid sampling artifacts
                atlas.texUvLines[n].put(uv0.x, halfV, uv1.x, halfV)
            }
        }
    }

    // A work of art lies ahead! (. = white layer, X = black layer, others are blank)
    // The 2x2 white texels on the top left are the ones we'll use everywhere in Dear ImGui to render filled shapes.
    object DefaultTexData {
        val w = 108 // Actual texture will be 2 times that + 1 spacing.
        val h = 27
        val pixels = run {
            val s = StringBuilder()
            s += "..-         -XXXXXXX-    X    -           X           -XXXXXXX          -          XXXXXXX-     XX          "
            s += "..-         -X.....X-   X.X   -          X.X          -X.....X          -          X.....X-    X..X         "
            s += "---         -XXX.XXX-  X...X  -         X...X         -X....X           -           X....X-    X..X         "
            s += "X           -  X.X  - X.....X -        X.....X        -X...X            -            X...X-    X..X         "
            s += "XX          -  X.X  -X.......X-       X.......X       -X..X.X           -           X.X..X-    X..X         "
            s += "X.X         -  X.X  -XXXX.XXXX-       XXXX.XXXX       -X.X X.X          -          X.X X.X-    X..XXX       "
            s += "X..X        -  X.X  -   X.X   -          X.X          -XX   X.X         -         X.X   XX-    X..X..XXX    "
            s += "X...X       -  X.X  -   X.X   -    XX    X.X    XX    -      X.X        -        X.X      -    X..X..X..XX  "
            s += "X....X      -  X.X  -   X.X   -   X.X    X.X    X.X   -       X.X       -       X.X       -    X..X..X..X.X "
            s += "X.....X     -  X.X  -   X.X   -  X..X    X.X    X..X  -        X.X      -      X.X        -XXX X..X..X..X..X"
            s += "X......X    -  X.X  -   X.X   - X...XXXXXX.XXXXXX...X -         X.X   XX-XX   X.X         -X..XX........X..X"
            s += "X.......X   -  X.X  -   X.X   -X.....................X-          X.X X.X-X.X X.X          -X...X...........X"
            s += "X........X  -  X.X  -   X.X   - X...XXXXXX.XXXXXX...X -           X.X..X-X..X.X           - X..............X"
            s += "X.........X -XXX.XXX-   X.X   -  X..X    X.X    X..X  -            X...X-X...X            -  X.............X"
            s += "X..........X-X.....X-   X.X   -   X.X    X.X    X.X   -           X....X-X....X           -  X.............X"
            s += "X......XXXXX-XXXXXXX-   X.X   -    XX    X.X    XX    -          X.....X-X.....X          -   X............X"
            s += "X...X..X    ---------   X.X   -          X.X          -          XXXXXXX-XXXXXXX          -   X...........X "
            s += "X..X X..X   -       -XXXX.XXXX-       XXXX.XXXX       -------------------------------------    X..........X "
            s += "X.X  X..X   -       -X.......X-       X.......X       -    XX           XX    -           -    X..........X "
            s += "XX    X..X  -       - X.....X -        X.....X        -   X.X           X.X   -           -     X........X  "
            s += "      X..X          -  X...X  -         X...X         -  X..X           X..X  -           -     X........X  "
            s += "       XX           -   X.X   -          X.X          - X...XXXXXXXXXXXXX...X -           -     XXXXXXXXXX  "
            s += "------------        -    X    -           X           -X.....................X-           ------------------"
            s += "                    ----------------------------------- X...XXXXXXXXXXXXX...X -                             "
            s += "                                                      -  X..X           X..X  -                             "
            s += "                                                      -   X.X           X.X   -                             "
            s += "                                                      -    XX           XX    -                             "
            s.toString().toCharArray()
        }

        val cursorDatas = arrayOf(
                // Pos ........ Size ......... Offset ......
                arrayOf(Vec2(0, 3), Vec2(12, 19), Vec2(0)),         // MouseCursor.Arrow
                arrayOf(Vec2(13, 0), Vec2(7, 16), Vec2(1, 8)),   // MouseCursor.TextInput
                arrayOf(Vec2(31, 0), Vec2(23), Vec2(11)),              // MouseCursor.Move
                arrayOf(Vec2(21, 0), Vec2(9, 23), Vec2(4, 11)),  // MouseCursor.ResizeNS
                arrayOf(Vec2(55, 18), Vec2(23, 9), Vec2(11, 4)), // MouseCursor.ResizeEW
                arrayOf(Vec2(73, 0), Vec2(17), Vec2(8)),               // MouseCursor.ResizeNESW
                arrayOf(Vec2(55, 0), Vec2(17), Vec2(8)),               // MouseCursor.ResizeNWSE
                arrayOf(Vec2(91, 0), Vec2(17, 22), Vec2(5, 0))) // ImGuiMouseCursor_Hand
    }
}