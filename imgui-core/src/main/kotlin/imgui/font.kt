package imgui


import gli_.has
import glm_.*
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec2.operators.div
import glm_.vec2.operators.times
import glm_.vec4.Vec4
import imgui.ImGui.io
import imgui.ImGui.style
import imgui.imgui.g
import imgui.internal.*
import imgui.stb.*
import kool.*
import kool.lib.isNotEmpty
import org.lwjgl.stb.*
import org.lwjgl.stb.STBRectPack.stbrp_pack_rects
import org.lwjgl.stb.STBTruetype.*
import uno.convert.decode85
import uno.stb.stb
import unsigned.toULong
import java.nio.ByteBuffer
import kotlin.math.floor
import kotlin.math.sqrt
import imgui.plusAssign
import uno.kotlin.plusAssign


//-----------------------------------------------------------------------------
// Font API (FontConfig, FontGlyph, FontAtlasFlags, FontAtlas, Font)
//-----------------------------------------------------------------------------


class FontConfig {

    /** TTF/OTF data    */
    var fontData = charArrayOf()
    /** TTF/OTF data size   */
    var fontDataSize = 0

    lateinit var fontDataBuffer: ByteBuffer
    /** TTF/OTF data ownership taken by the container ImFontAtlas (will delete memory itself).  */
    var fontDataOwnedByAtlas = true
    /** Index of font within TTF/OTF file   */
    var fontNo = 0
    /** Size in pixels for rasterizer (more or less maps to the resulting font height).  */
    var sizePixels = 0f
    /** Rasterize at higher quality for sub-pixel positioning.
     *  Read https://github.com/nothings/stb/blob/master/tests/oversample/README.md for details. */
    var oversample = Vec2i(3, 1) // FIXME: 2 may be a better x default?
    /** Align every glyph to pixel boundary. Useful e.g. if you are merging a non-pixel aligned font with the default
     *  font. If enabled, you can set OversampleH/V to 1.   */
    var pixelSnapH = false
    /** Extra spacing (in pixels) between glyphs. Only X axis is supported for now.    */
    var glyphExtraSpacing = Vec2()
    /** Offset all glyphs from this font input. */
    var glyphOffset = Vec2()
    /** Pointer to a user-provided list of Unicode range (2 value per range, values are inclusive, zero-terminated
     *  list). THE ARRAY DATA NEEDS TO PERSIST AS LONG AS THE FONT IS ALIVE.    */
    var glyphRanges = arrayOf<IntRange>()
    /** Minimum AdvanceX for glyphs, set Min to align font icons, set both Min/Max to enforce mono-space font */
    var glyphMinAdvanceX = 0f
    /** Maximum AdvanceX for glyphs */
    var glyphMaxAdvanceX = Float.MAX_VALUE
    /** Merge into previous ImFont, so you can combine multiple inputs font into one ImFont (e.g. ASCII font + icons +
     *  Japanese glyphs). You may want to use GlyphOffset.y when merge font of different heights.   */
    var mergeMode = false
    /** Settings for custom font rasterizer (e.g. ImGuiFreeType). Leave as zero if you aren't using one.    */
    var rasterizerFlags = 0x00
    /** Brighten (>1.0f) or darken (<1.0f) font output. Brightening small fonts may be a good workaround to make them
     *  more readable.  */
    var rasterizerMultiply = 1f

    // [Internal]
    /** Name (strictly to ease debugging)   */
    var name = ""

    var dstFont: Font? = null
}

/** A single font glyph (code point + coordinates within in ImFontAtlas + offset) */
class FontGlyph {
    /** 0x0000..0xFFFF  */
    var codepoint = NUL
    /** Distance to next character (= data from font + FontConfig.glyphExtraSpacing.x baked in)  */
    var advanceX = 0f
    // Glyph corners
    var x0 = 0f
    var y0 = 0f
    var x1 = 0f
    var y1 = 0f
    // Texture coordinates
    var u0 = 0f
    var v0 = 0f
    var u1 = 0f
    var v1 = 0f

    infix fun put(other: FontGlyph) {
        codepoint = other.codepoint
        advanceX = other.advanceX
        x0 = other.x0
        y0 = other.y0
        x1 = other.x1
        y1 = other.y1
        u0 = other.u0
        v0 = other.v0
        u1 = other.u1
        v1 = other.v1
    }
}

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

        // Invalidate texture
        clearTexData()
        return fontCfg.dstFont!!
    }

    /** Load embedded ProggyClean.ttf at size 13, disable oversampling  */
    fun addFontDefault(): Font {
        val fontCfg = FontConfig()
        fontCfg.oversample put 1
        fontCfg.pixelSnapH = true
        return addFontDefault(fontCfg)
    }

    fun addFontDefault(fontCfg: FontConfig): Font {

        if (fontCfg.sizePixels <= 0f) fontCfg.sizePixels = 13f
        if (fontCfg.name.isEmpty()) fontCfg.name = "ProggyClean.ttf, ${fontCfg.sizePixels.i}px"

        val ttfCompressedBase85 = proggyCleanTtfCompressedDataBase85
        val glyphRanges = fontCfg.glyphRanges.takeIf { it.isNotEmpty() } ?: glyphRanges.default
        return addFontFromMemoryCompressedBase85TTF(ttfCompressedBase85, fontCfg.sizePixels, fontCfg, glyphRanges)
                .apply { displayOffset.y = 1f }
    }

    fun addFontFromFileTTF(filename: String, sizePixels: Float, fontCfg: FontConfig = FontConfig(),
                           glyphRanges: Array<IntRange> = arrayOf()): Font? {

        assert(!locked) { "Cannot modify a locked FontAtlas between NewFrame() and EndFrame/Render()!" }
        val chars = fileLoadToCharArray(filename) ?: return null
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
        customRectIds.fill(-1)
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
    }

    /*  Build atlas, retrieve pixel data.
        User is in charge of copying the pixels into graphics memory (e.g. create a texture with your engine).
        Then store your texture handle with setTexID().ClearInputData
        The pitch is always = Width * BytesPerPixels (1 or 4)
        Building in RGBA32 format is provided for convenience and compatibility, but note that unless
        you manually manipulate or copy color data into the texture (e.g. when using the AddCustomRect*** api),
        then the RGB pixels emitted will always be white (~75% of memory/bandwidth waste.  */

    /** Build pixels data. This is automatically for you by the GetTexData*** functions.    */
    private fun build() {
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


// Helper to build glyph ranges from text/string data. Feed your application strings/characters to it then call BuildRanges().
    // This is essentially a tightly packed of vector of 64k booleans = 8KB storage.
//    struct ImFontGlyphRangesBuilder
//    {
//        ImVector<int> UsedChars;            // Store 1-bit per Unicode code point (0=unused, 1=used)
//
//    ImFontGlyphRangesBuilder()          { UsedChars.resize(0x10000 / 32); memset(UsedChars.Data, 0, 0x10000 / 32); }
//    bool           GetBit(int n) const  { int off = (n >> 5); int mask = 1 << (n & 31); return (UsedChars[off] & mask) != 0; } // Get bit n in the array
//    void           SetBit(int n)        { int off = (n >> 5); int mask = 1 << (n & 31); UsedChars[off] |= mask; }              // Set bit n in the array
//        void           AddChar(ImWchar c)   { SetBit(c); }                          // Add character
//        IMGUI_API void AddText(const char* text, const char* text_end = NULL);      // Add string (each character of the UTF-8 string are added)
//        IMGUI_API void AddRanges(const ImWchar* ranges);                            // Add ranges, e.g. builder.AddRanges(ImFontAtlas::GetGlyphRangesDefault()) to force add all of ASCII/Latin+Ext
//        IMGUI_API void BuildRanges(ImVector<ImWchar>* out_ranges);                  // Output new ranges
//    };


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
//        for (int n = 0; n < 0x10000; n++)
//            if (GetBit(n))
//                {
//                        out_ranges->push_back((ImWchar)n);
//                        while (n < 0x10000 && GetBit(n + 1))
//                                n++;
//                        out_ranges->push_back((ImWchar)n);
//                    }
//        out_ranges->push_back(0);
//        +}


    //-------------------------------------------
    // Custom Rectangles/Glyphs API
    //-------------------------------------------

    /** You can request arbitrary rectangles to be packed into the atlas, for your own purposes. After calling build(),
     *  you can query the rectangle position and render your pixels.
     *  You can also request your rectangles to be mapped as font glyph (given a font + Unicode point),
     *  so you can render e.g. custom colorful icons and use them as regular glyphs.    */
    class CustomRect {

        /** Input, User ID. Use <0x10000 to map into a font glyph, >=0x10000 for other/internal/custom texture data.   */
        var id = 0xFFFFFFFF.i
        /** Input, Desired rectangle width */
        var width = 0
        /** Input, Desired rectangle height */
        var height = 0
        /** Output, Packed width position in Atlas  */
        var x = 0xFFFF
        /** Output, Packed height position in Atlas  */
        var y = 0xFFFF
        /** Input, For custom font glyphs only (ID<0x10000): glyph xadvance */
        var glyphAdvanceX = 0f
        /** Input, For custom font glyphs only (ID<0x10000): glyph display offset   */
        var glyphOffset = Vec2()
        /** Input, For custom font glyphs only (ID<0x10000): target font    */
        var font: Font? = null

        val isPacked get() = x != 0xFFFF
    }

    /** Id needs to be >= 0x10000. Id >= 0x80000000 are reserved for ImGui and DrawList   */
    fun addCustomRectRegular(id: Int, width: Int, height: Int): Int {
        assert(id.toULong() >= 0x10000 && width in 0..0xFFFF && height in 0..0xFFFF)
        val r = CustomRect()
        r.id = id
        r.width = width
        r.height = height
        customRects.add(r)
        return customRects.lastIndex
    }

    /** Id needs to be < 0x10000 to register a rectangle to map into a specific font.   */
    fun addCustomRectFontGlyph(font: Font, id: Int, width: Int, height: Int, advanceX: Float, offset: Vec2 = Vec2()): Int {
        assert(width in 1..0xFFFF && height in 1..0xFFFF)
        val r = CustomRect()
        r.id = id
        r.width = width
        r.height = height
        r.glyphAdvanceX = advanceX
        r.glyphOffset = offset
        r.font = font
        customRects.add(r)
        return customRects.lastIndex // Return index
    }

    fun getCustomRectByIndex(index: Int) = customRects.getOrNull(index)

    // Internals

    fun calcCustomRectUV(rect: CustomRect, outUvMin: Vec2, outUvMax: Vec2) {
        assert(texSize allGreaterThan 0) { "Font atlas needs to be built before we can calculate UV coordinates" }
        assert(rect.isPacked) { "Make sure the rectangle has been packed" }
        outUvMin.put(rect.x.f * texUvScale.x, rect.y.f * texUvScale.y)
        outUvMax.put((rect.x + rect.width).f * texUvScale.x, (rect.y + rect.height).f * texUvScale.y)
    }

    fun getMouseCursorTexData(cursor: MouseCursor, outOffset: Vec2, outSize: Vec2, outUv: Array<Vec2>): Boolean {

        if (cursor == MouseCursor.None) return false

        if (flags has FontAtlasFlag.NoMouseCursors) return false

        assert(customRectIds[0] != -1)
        val r = customRects[customRectIds[0]]
        assert(r.id == DefaultTexData.id)
        val pos = DefaultTexData.cursorDatas[cursor.i][0] + Vec2(r.x, r.y)
        val size = DefaultTexData.cursorDatas[cursor.i][1]
        outSize put size
        outOffset put DefaultTexData.cursorDatas[cursor.i][2]
        // JVM border
        outUv[0] = pos * texUvScale
        outUv[1] = (pos + size) * texUvScale
        pos.x += DefaultTexData.wHalf + 1
        // JVM fill
        outUv[2] = pos * texUvScale
        outUv[3] = (pos + size) * texUvScale
        return true
    }

    //-------------------------------------------
    // Members
    //-------------------------------------------
    enum class FontAtlasFlag {
        None,
        /** Don't round the height to next power of two */
        NoPowerOfTwoHeight,
        /** Don't build software mouse cursors into the atlas   */
        NoMouseCursors;

        val i = if (ordinal == 0) 0 else 1 shl ordinal
    }

    infix fun Int.has(flag: FontAtlasFlag) = and(flag.i) != 0
    infix fun Int.hasnt(flag: FontAtlasFlag) = and(flag.i) == 0

    /** Marked as Locked by ImGui::NewFrame() so attempt to modify the atlas will assert. */
    var locked = false
    /** Build flags (see ImFontAtlasFlags_) */
    var flags = FontAtlasFlag.None.i
    /** User data to refer to the texture once it has been uploaded to user's graphic systems. It is passed back to you
    during rendering via the DrawCmd structure.   */
    var texId: TextureID = 0
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
    /** Internal data   */
    private val configData = ArrayList<FontConfig>()
    /** Identifiers of custom texture rectangle used by FontAtlas/DrawList  */
    private val customRectIds = intArrayOf(-1)

    private fun customRectCalcUV(rect: CustomRect, outUvMin: Vec2, outUvMax: Vec2) {
        assert(texSize.x > 0 && texSize.y > 0) { "Font atlas needs to be built before we can calculate UV coordinates" }
        assert(rect.isPacked) { "Make sure the rectangle has been packed " }
        outUvMin.put(rect.x / texSize.x, rect.y / texSize.y)
        outUvMax.put((rect.x + rect.width) / texSize.x, (rect.y + rect.height) / texSize.y)
    }

    /** Helper: ImBoolVector. Store 1-bit per value.
     *  Note that Resize() currently clears the whole vector. */
    class BoolVector {
        var storage = IntArray(0)
        infix fun resize(sz: Int) {
            storage = IntArray((sz + 31) shr 5)
        }

        fun clear() {
            storage = IntArray(0)
        }

        operator fun get(n: Int): Boolean {
            val off = n shr 5
            val mask = 1 shl (n and 31)
            return storage[off] has mask
        }

        operator fun set(n: Int, v: Boolean) {
            val off = (n shr 5)
            val mask = 1 shl (n and 31)
            storage[off] = when {
                v -> storage[off] or mask
                else -> storage[off] wo mask
            }
        }

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
        val glyphsSet = BoolVector()
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
        var glyphsSet = BoolVector()
    }

    // ImFontAtlas internals

    fun buildWithStbTrueType(): Boolean {

        assert(configData.isNotEmpty())

        buildRegisterDefaultCustomRects()

        // Clear atlas
        texId = 0
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
            val fontOffset = stbtt_GetFontOffsetForIndex(cfg.fontDataBuffer, cfg.fontNo)
            assert(fontOffset >= 0) { "FontData is incorrect, or FontNo cannot be found." }
            if (!stbtt_InitFont(srcTmp.fontInfo, cfg.fontDataBuffer, fontOffset))
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
            srcTmp.glyphsSet.resize(srcTmp.glyphsHighest + 1)
            if (dstTmp.glyphsSet.storage.isEmpty())
                dstTmp.glyphsSet.resize(dstTmp.glyphsHighest + 1)

            for (srcRange in srcTmp.srcRanges)
                for (codepoint in srcRange) {
                    if (dstTmp.glyphsSet[codepoint])   // Don't overwrite existing glyphs. We could make this an option for MergeMode (e.g. MergeOverwrite==true)
                        continue
                    if (!stbtt_FindGlyphIndex(srcTmp.fontInfo, codepoint).bool)    // It is actually in the font?
                        continue

                    // Add to avail set/counters
                    srcTmp.glyphsCount++
                    dstTmp.glyphsCount++
                    srcTmp.glyphsSet[codepoint] = true
                    dstTmp.glyphsSet[codepoint] = true
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
        dstTmpArray.forEach { it.glyphsSet.clear() }
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
                cfg.sizePixels > 0 -> stbtt_ScaleForPixelHeight(srcTmp.fontInfo, cfg.sizePixels)
                else -> stbtt_ScaleForMappingEmToPixels(srcTmp.fontInfo, -cfg.sizePixels)
            }
            val padding = texGlyphPadding
            for (glyphIdx in srcTmp.glyphsList.indices) {
                val glyphIndexInFont = stbtt_FindGlyphIndex(srcTmp.fontInfo, srcTmp.glyphsList[glyphIdx])
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
        stbtt_PackBegin(spc, null, texSize.x, TEX_HEIGHT_MAX, 0, texGlyphPadding)
        buildPackCustomRects(spc.packInfo)

        // 6. Pack each source font. No rendering yet, we are working with rectangles in an infinitely tall texture at this point.
        for (srcTmp in srcTmpArray) {
            if (srcTmp.glyphsCount == 0)
                continue

            stbrp_pack_rects(spc.packInfo, srcTmp.rects)

            // Extend texture height and mark missing glyphs as non-packed so we won't render them.
            // FIXME: We are not handling packing failure here (would happen if we got off TEX_HEIGHT_MAX or if a single if larger than TexWidth?)
            for (glyphIdx in 0 until srcTmp.glyphsCount)
                if (srcTmp.rects[glyphIdx].wasPacked)
                    texSize.y = texSize.y max (srcTmp.rects[glyphIdx].y + srcTmp.rects[glyphIdx].h)
        }

        // 7. Allocate texture
        texSize.y = when {
            flags has FontAtlasFlag.NoPowerOfTwoHeight -> texSize.y + 1
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
        stbtt_PackEnd(spc)
        spc.free()
        bufRects.free()

        // 9. Setup ImFont and glyphs for runtime
        val q = STBTTAlignedQuad.calloc()
        for (srcIdx in srcTmpArray.indices) {
            val srcTmp = srcTmpArray[srcIdx]
            if (srcTmp.glyphsCount == 0)
                continue

            val cfg = configData[srcIdx]
            val dstFont = cfg.dstFont!! // We can have multiple input fonts writing into a same destination font (when using MergeMode=true)

            val fontScale = stbtt_ScaleForPixelHeight(srcTmp.fontInfo, cfg.sizePixels)
            val (unscaledAscent, unscaledDescent, _) = stbtt_GetFontVMetrics(srcTmp.fontInfo)

            val ascent = floor(unscaledAscent * fontScale + if (unscaledAscent > 0f) +1 else -1)
            val descent = floor(unscaledDescent * fontScale + if (unscaledDescent > 0f) +1 else -1)
            buildSetupFont(dstFont, cfg, ascent, descent)
            val fontOff = Vec2(cfg.glyphOffset).apply { y += (dstFont.ascent + 0.5f).i.f }

            for (glyphIdx in 0 until srcTmp.glyphsCount) {
                val codepoint = srcTmp.glyphsList[glyphIdx]
                val pc = srcTmp.packedChars[glyphIdx]

                val charAdvanceXorg = pc.xAdvance
                val charAdvanceXmod = glm.clamp(charAdvanceXorg, cfg.glyphMinAdvanceX, cfg.glyphMaxAdvanceX)
                var charOffX = fontOff.x
                if (charAdvanceXorg != charAdvanceXmod)
                    charOffX += when {
                        cfg.pixelSnapH -> ((charAdvanceXmod - charAdvanceXorg) * 0.5f).i.f
                        else -> (charAdvanceXmod - charAdvanceXorg) * 0.5f
                    }

                // Register glyph
                stbtt_GetPackedQuad(srcTmp.packedChars, texSize, glyphIdx, q)
                dstFont.addGlyph(codepoint, q.x0 + charOffX, q.y0 + fontOff.y, q.x1 + charOffX, q.y1 + fontOff.y, q.s0, q.t0, q.s1, q.t1, charAdvanceXmod)
            }
        }
        bufPackedchars.free()

        // Cleanup temporary (ImVector doesn't honor destructor)
        srcTmpArray.forEach { it.free() }
        q.free()

        buildFinish()
        return true
    }

    fun buildRegisterDefaultCustomRects() {
        if (customRectIds[0] >= 0) return
        customRectIds[0] = when {
            flags hasnt FontAtlasFlag.NoMouseCursors -> addCustomRectRegular(DefaultTexData.id, DefaultTexData.wHalf * 2 + 1, DefaultTexData.h)
            else -> addCustomRectRegular(DefaultTexData.id, 2, 2)
        }
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
        stbrp_pack_rects(stbrpContext, packRects)
        for (i in userRects.indices)
            if (packRects[i].wasPacked) {
                userRects[i].x = packRects[i].x
                userRects[i].y = packRects[i].y
                assert(packRects[i].w == userRects[i].width && packRects[i].h == userRects[i].height)
                texSize.y = glm.max(texSize.y, packRects[i].y + packRects[i].h)
            }
        packRects.free()
    }

    fun buildFinish() {
        // Render into our custom data block
        buildRenderDefaultTexData()

        // Register custom rectangle glyphs
        for (r in customRects) {
            val font = r.font
            if (font == null || r.id > 0x10000) continue

            assert(font.containerAtlas === this)
            val uv0 = Vec2()
            val uv1 = Vec2()
            calcCustomRectUV(r, uv0, uv1)
            font.addGlyph(r.id, r.glyphOffset.x, r.glyphOffset.y, r.glyphOffset.x + r.width, r.glyphOffset.y + r.height,
                    uv0.x, uv0.y, uv1.x, uv1.y, r.glyphAdvanceX)
        }
        // Build all fonts lookup tables
        fonts.filter { it.dirtyLookupTables }.forEach { it.buildLookupTable() }
    }

    fun buildRenderDefaultTexData() {

        assert(customRectIds[0] >= 0 && texPixelsAlpha8 != null)
        val r = customRects[customRectIds[0]]
        assert(r.id == DefaultTexData.id && r.isPacked)

        val w = texSize.x
        if (flags hasnt FontAtlasFlag.NoMouseCursors) {
            // Render/copy pixels
            assert(r.width == DefaultTexData.wHalf * 2 + 1 && r.height == DefaultTexData.h)
            var n = 0
            for (y in 0 until DefaultTexData.h)
                for (x in 0 until DefaultTexData.wHalf) {
                    val offset0 = r.x + x + (r.y + y) * w
                    val offset1 = offset0 + DefaultTexData.wHalf + 1
                    texPixelsAlpha8!![offset0] = if (DefaultTexData.pixels[n] == '.') 0xFF.b else 0x00.b
                    texPixelsAlpha8!![offset1] = if (DefaultTexData.pixels[n] == 'X') 0xFF.b else 0x00.b
                    n++
                }
        } else {
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


    /*  A work of art lies ahead! (. = white layer, X = black layer, others are blank)
        The white texels on the top left are the ones we'll use everywhere in ImGui to render filled shapes.     */
    object DefaultTexData {
        val wHalf = 108
        val h = 27
        val id = 0x80000000.i
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

/** Font runtime data and rendering
 *  ImFontAtlas automatically loads a default embedded font for you when you call GetTexDataAsAlpha8() or
 *  GetTexDataAsRGBA32().   */
class Font {

    // @formatter:off

    // Members: Hot ~20/24 bytes (for CalcTextSize)

    /** Sparse. Glyphs->AdvanceX in a directly indexable way (cache-friendly for CalcTextSize functions which only this info,
     *  and are often bottleneck in large UI). */
    val indexAdvanceX = ArrayList<Float>()      // 12/16 // out //

    var fallbackAdvanceX = 0f                   // 4     // out // = FallbackGlyph->AdvanceX
    /** Height of characters, set during loading (don't change after loading)   */
    var fontSize = 0f                           // 4     // in  // <user set>

    // Members: Hot ~36/48 bytes (for CalcTextSize + render loop)

    /** Sparse. Index glyphs by Unicode code-point. */
    val indexLookup = ArrayList<Int>()          // 12-16 // out //
    /** All glyphs. */
    val glyphs = ArrayList<FontGlyph>()         // 12-16 // out //

    var fallbackGlyph: FontGlyph? = null        // 4-8   // out // = FindGlyph(FontFallbackChar)
    /** Offset font rendering by xx pixels  */
    var displayOffset = Vec2()                  // 8     // in  // = (0,0)

    // Members: Cold ~32/40 bytes

    /** What we has been loaded into    */
    lateinit var containerAtlas: FontAtlas      // 4-8   // out //
    /** Pointer within ContainerAtlas->ConfigData   */
    val configData = ArrayList<FontConfig>()    // 4-8   // in  //
    /** Number of ImFontConfig involved in creating this font. Bigger than 1 when merging multiple font sources into one ImFont.    */
    var configDataCount = 0                     // 2     // in  // ~ 1
    /** Replacement glyph if one isn't found. Only set via SetFallbackChar()    */
    var fallbackChar = '?'                      // 2     // in  // = '?'
        /** ~SetFallbackChar */
        set(value) {
            field = value
            buildLookupTable()
        }
    /** Base font scale, multiplied by the per-window font scale which you can adjust with SetWindowFontScale()   */
    var scale = 1f                              // 4     // in  // = 1.f
    /** Ascent: distance from top to bottom of e.g. 'A' [0..FontSize]   */
    var ascent = 0f                             // 4     // out

    var descent = 0f                            // 4     // out
    /** Total surface in pixels to get an idea of the font rasterization/texture cost (not exact, we approximate the cost of padding between glyphs)    */
    var metricsTotalSurface = 0                 // 4     // out

    var dirtyLookupTables = true                // 1     // out //

    // @formatter:on

    fun findGlyph(c: Char): FontGlyph? = findGlyph(c.remapCodepointIfProblematic())
    fun findGlyph(c: Int): FontGlyph? = indexLookup.getOrNull(c)?.let { glyphs.getOrNull(it) } ?: fallbackGlyph

    fun findGlyphNoFallback(c: Char): FontGlyph? = findGlyphNoFallback(c.i)
    fun findGlyphNoFallback(c: Int): FontGlyph? = indexLookup.getOrNull(c)?.let { glyphs.getOrNull(it) }

    fun getCharAdvance(c: Char): Float = if (c < indexAdvanceX.size) indexAdvanceX[c.i] else fallbackAdvanceX

    val isLoaded: Boolean
        get() = ::containerAtlas.isInitialized

    val debugName: String
        get() = configData.getOrNull(0)?.name ?: "<unknown>"

    /*  'maxWidth' stops rendering after a certain width (could be turned into a 2d size). FLT_MAX to disable.
        'wrapWidth' enable automatic word-wrapping across multiple lines to fit into given width. 0.0f to disable. */
    fun calcTextSizeA(size: Float, maxWidth: Float, wrapWidth: Float, text: String, textEnd_: Int = text.length,
                      remaining: IntArray? = null): Vec2 { // utf8

        val textEnd = if(textEnd_ == -1) text.length else textEnd_

        val lineHeight = size
        val scale = size / fontSize

        val textSize = Vec2(0)
        var lineWidth = 0f

        val wordWrapEnabled = wrapWidth > 0f
        var wordWrapEol = -1

        var s = 0
        while (s < textEnd) {

            if (wordWrapEnabled) {

                /*  Calculate how far we can render. Requires two passes on the string data but keeps the code simple
                    and not intrusive for what's essentially an uncommon feature.   */
                if (wordWrapEol == -1) {
                    wordWrapEol = calcWordWrapPositionA(scale, text.toCharArray(), s, textEnd, wrapWidth - lineWidth)
                    /*  Wrap_width is too small to fit anything. Force displaying 1 character to minimize the height
                        discontinuity.                     */
                    if (wordWrapEol == s)
                    // +1 may not be a character start point in UTF-8 but it's ok because we use s >= wordWrapEol below
                        wordWrapEol++
                }

                if (s >= wordWrapEol) {
                    if (textSize.x < lineWidth)
                        textSize.x = lineWidth
                    textSize.y += lineHeight
                    lineWidth = 0f
                    wordWrapEol = -1

                    // Wrapping skips upcoming blanks
                    while (s < textEnd) {
                        val c = text[s]
                        if (c.isBlankA)
                            s++
                        else if (c == '\n') {
                            s++
                            break
                        } else break
                    }
                    continue
                }
            }

            // Decode and advance source
            val prevS = s
            val c = text[s]
            /*  JVM imgui specific, not 0x80 because on jvm we have Unicode with surrogates characters (instead of utf8)
                    https://www.ibm.com/developerworks/library/j-unicode/index.html             */
            if (c < Char.MIN_SURROGATE)
                s += 1
            else {
                TODO("Probabily surrogate character")
//                s += ImTextCharFromUtf8(& c, s, text_end)
//                if (c.i == 0x0) break   // Malformed UTF-8?
            }

            if (c < 32) {
                if (c == '\n') {
                    textSize.x = glm.max(textSize.x, lineWidth)
                    textSize.y += lineHeight
                    lineWidth = 0f
                    continue
                }
                if (c == '\r') continue
            }

            val charWidth = (if (c < indexAdvanceX.size) indexAdvanceX[c.i] else fallbackAdvanceX) * scale
            if (lineWidth + charWidth >= maxWidth) {
                s = prevS
                break
            }
            lineWidth += charWidth
        }

        if (textSize.x < lineWidth)
            textSize.x = lineWidth

        if (lineWidth > 0 || textSize.y == 0.0f)
            textSize.y += lineHeight

        remaining?.set(0, s)

        return textSize
    }

    fun calcWordWrapPositionA(scale: Float, text: CharArray, ptr: Int, textEnd: Int, wrapWidth_: Float): Int {

        /*  Simple word-wrapping for English, not full-featured. Please submit failing cases!
            FIXME: Much possible improvements (don't cut things like "word !", "word!!!" but cut within "word,,,,",
            more sensible support for punctuations, support for Unicode punctuations, etc.)

            For references, possible wrap point marked with ^
            "aaa bbb, ccc,ddd. eee   fff. ggg!"
                ^    ^    ^   ^   ^__    ^    ^

            List of hardcoded separators: .,;!?'"

            Skip extra blanks after a line returns (that includes not counting them in width computation)
            e.g. "Hello    world" --> "Hello" "World"

            Cut words that cannot possibly fit within one line.
            e.g.: "The tropical fish" with ~5 characters worth of width --> "The tr" "opical" "fish"    */

        var lineWidth = 0f
        var wordWidth = 0f
        var blankWidth = 0f
        val wrapWidth = wrapWidth_ / scale   // We work with unscaled widths to avoid scaling every characters

        var wordEnd = ptr
        var prevWordEnd = -1
        var insideWord = true

        var s = ptr
        while (s < textEnd) {    // TODO remove textEnd?
            val c = text[s]
            val nextS =
                    if (c < 0x80)
                        s + 1
                    else
                        TODO() // (s + ImTextCharFromUtf8(&c, s, text_end)).c
            if (c == NUL) break

            if (c < 32) {
                if (c == '\n') {
                    lineWidth = 0f
                    wordWidth = 0f
                    blankWidth = 0f
                    insideWord = true
                    s = nextS
                    continue
                }
                if (c == '\r') {
                    s = nextS
                    continue
                }
            }
            val charWidth = indexAdvanceX.getOrElse(c.i, { fallbackAdvanceX })
            if (c.isBlankW) {
                if (insideWord) {
                    lineWidth += blankWidth
                    blankWidth = 0.0f
                    wordEnd = s
                }
                blankWidth += charWidth
                insideWord = false
            } else {
                wordWidth += charWidth
                if (insideWord)
                    wordEnd = nextS
                else {
                    prevWordEnd = wordEnd
                    lineWidth += wordWidth + blankWidth
                    wordWidth = 0f
                    blankWidth = 0f
                }
                // Allow wrapping after punctuation.
                insideWord = !(c == '.' || c == ',' || c == ';' || c == '!' || c == '?' || c == '\"')
            }

            // We ignore blank width at the end of the line (they can be skipped)
            if (lineWidth + wordWidth >= wrapWidth) {
                // Words that cannot possibly fit within an entire line will be cut anywhere.
                if (wordWidth < wrapWidth)
                    s = if (prevWordEnd != -1) prevWordEnd else wordEnd
                break
            }
            s = nextS
        }
        return s
    }

    fun renderChar(drawList: DrawList, size: Float, pos: Vec2, col: Int, c: Char) {
        if (c == ' ' || c == '\t' || c == '\n' || c == '\r') // Match behavior of RenderText(), those 4 codepoints are hard-coded.
            return
        findGlyph(c)?.let {
            val scale = if (size >= 0f) size / fontSize else 1f
            val x = pos.x.i.f + displayOffset.x
            val y = pos.y.i.f + displayOffset.y
            drawList.primReserve(6, 4)
            val a = Vec2(x + it.x0 * scale, y + it.y0 * scale)
            val c_ = Vec2(x + it.x1 * scale, y + it.y1 * scale)
            drawList.primRectUV(a, c_, Vec2(it.u0, it.v0), Vec2(it.u1, it.v1), col)
        }
    }

    //    const ImVec4& clipRect, const char* text, const char* textEnd, float wrapWidth = 0.0f, bool cpuFineClip = false) const;
    fun renderText(drawList: DrawList, size: Float, pos: Vec2, col: Int, clipRect: Vec4, text: CharArray, textEnd_: Int = -1, // TODO return it also?
                   wrapWidth: Float = 0f, cpuFineClip: Boolean = false) {

        var textEnd = textEnd_

        // Align to be pixel perfect
        pos.x = pos.x.i.f + displayOffset.x
        pos.y = pos.y.i.f + displayOffset.y
        var (x, y) = pos
        if (y > clipRect.w) return

        val scale = size / fontSize
        val lineHeight = fontSize * scale
        val wordWrapEnabled = wrapWidth > 0f
        var wordWrapEol = 0

        // Fast-forward to first visible line
        var s = 0
        if (y + lineHeight < clipRect.y && !wordWrapEnabled)
            while (y + lineHeight < clipRect.y && s < textEnd) {
                s = text.memchr(s, '\n')?.plus(1) ?: textEnd
                y += lineHeight
            }

        /*  For large text, scan for the last visible line in order to avoid over-reserving in the call to PrimReserve()
            Note that very large horizontal line will still be affected by the issue (e.g. a one megabyte string buffer without a newline will likely crash atm)         */
        if (textEnd - s > 10000 && !wordWrapEnabled) {
            var sEnd = s
            var yEnd = y
            while (yEnd < clipRect.w && s < textEnd) {
                sEnd = text.memchr(sEnd, '\n')?.plus(1) ?: textEnd
                yEnd += lineHeight
            }
            textEnd = sEnd
        }
        if (s == textEnd)
            return


        // Reserve vertices for remaining worse case (over-reserving is useful and easily amortized)
        val vtxCountMax = (textEnd - s) * 4
        val idxCountMax = (textEnd - s) * 6
        val idxExpectedSize = drawList.idxBuffer.lim + idxCountMax
        drawList.primReserve(idxCountMax, vtxCountMax)
        drawList.vtxBuffer.pos = drawList._vtxWritePtr
        drawList.idxBuffer.pos = drawList._idxWritePtr

        var vtxWrite = drawList._vtxWritePtr
        var idxWrite = drawList._idxWritePtr
        var vtxCurrentIdx = drawList._vtxCurrentIdx

        while (s < textEnd) {

            if (wordWrapEnabled) {

                /*  Calculate how far we can render. Requires two passes on the string data but keeps the code simple
                    and not intrusive for what's essentially an uncommon feature.                 */
                if (wordWrapEol == 0) {
                    wordWrapEol = calcWordWrapPositionA(scale, text, s, textEnd, wrapWidth - (x - pos.x))
                    /*  Wrap_width is too small to fit anything. Force displaying 1 character to minimize the height
                        discontinuity.                     */
                    if (wordWrapEol == s)
                    //  +1 may not be a character start point in UTF-8 but it's ok because we use s >= wordWrapEol below
                        wordWrapEol++
                }

                if (s >= wordWrapEol) {
                    x = pos.x
                    y += lineHeight
                    wordWrapEol = 0

                    // Wrapping skips upcoming blanks
                    while (s < textEnd) {
                        val c = text[s]
                        if (c.isBlankA) s++
                        else if (c == '\n') {
                            s++
                            break
                        } else break
                    }
                    continue
                }
            }
            // Decode and advance source
            if (s >= text.size)
                return
            val c = text[s]
            /*  JVM imgui specific, not 0x80 because on jvm we have Unicode with surrogates characters (instead of utf8)
                    https://www.ibm.com/developerworks/library/j-unicode/index.html             */
            if (c < Char.MIN_HIGH_SURROGATE)
                s += 1
            else {
                TODO("Probably surrogate character")
//                s += textCharFromUtf8(& c, s, text_end)
//                if (c == 0) // Malformed UTF-8?
//                    break
            }

            if (c < 32) {
                if (c == '\n') {
                    x = pos.x
                    y += lineHeight
                    if (y > clipRect.w)
                        break // break out of main loop
                    continue
                }
                if (c == '\r') continue
            }

            var charWidth = 0f
            val glyph = findGlyph(c)

            if (glyph != null) {
                charWidth = glyph.advanceX * scale

                // Arbitrarily assume that both space and tabs are empty glyphs as an optimization
                if (c != ' ' && c != '\t') {
                    /*  We don't do a second finer clipping test on the Y axis as we've already skipped anything before
                        clipRect.y and exit once we pass clipRect.w    */
                    var x1 = x + glyph.x0 * scale
                    var x2 = x + glyph.x1 * scale
                    var y1 = y + glyph.y0 * scale
                    var y2 = y + glyph.y1 * scale
                    if (x1 <= clipRect.z && x2 >= clipRect.x) {
                        // Render a character
                        var u1 = glyph.u0
                        var v1 = glyph.v0
                        var u2 = glyph.u1
                        var v2 = glyph.v1

                        /*  CPU side clipping used to fit text in their frame when the frame is too small. Only does
                            clipping for axis aligned quads.    */
                        if (cpuFineClip) {
                            if (x1 < clipRect.x) {
                                u1 += (1f - (x2 - clipRect.x) / (x2 - x1)) * (u2 - u1)
                                x1 = clipRect.x
                            }
                            if (y1 < clipRect.y) {
                                v1 += (1f - (y2 - clipRect.y) / (y2 - y1)) * (v2 - v1)
                                y1 = clipRect.y
                            }
                            if (x2 > clipRect.z) {
                                u2 = u1 + ((clipRect.z - x1) / (x2 - x1)) * (u2 - u1)
                                x2 = clipRect.z
                            }
                            if (y2 > clipRect.w) {
                                v2 = v1 + ((clipRect.w - y1) / (y2 - y1)) * (v2 - v1)
                                y2 = clipRect.w
                            }
                            if (y1 >= y2) {
                                x += charWidth
                                continue
                            }
                        }

                        /*  We are NOT calling PrimRectUV() here because non-inlined causes too much overhead in a
                            debug builds. Inlined here:   */

                        with(drawList) {
                            idxBuffer += vtxCurrentIdx; idxBuffer += vtxCurrentIdx + 1; idxBuffer += vtxCurrentIdx + 2
                            idxBuffer += vtxCurrentIdx; idxBuffer += vtxCurrentIdx + 2; idxBuffer += vtxCurrentIdx + 3
                            vtxBuffer += x1; vtxBuffer += y1; vtxBuffer += u1; vtxBuffer += v1; vtxBuffer += col
                            vtxBuffer += x2; vtxBuffer += y1; vtxBuffer += u2; vtxBuffer += v1; vtxBuffer += col
                            vtxBuffer += x2; vtxBuffer += y2; vtxBuffer += u2; vtxBuffer += v2; vtxBuffer += col
                            vtxBuffer += x1; vtxBuffer += y2; vtxBuffer += u1; vtxBuffer += v2; vtxBuffer += col
                            vtxWrite += 4
                            vtxCurrentIdx += 4
                            idxWrite += 6
                        }
                    }
                }
            }
            x += charWidth
        }
        drawList.vtxBuffer.pos = 0
        drawList.idxBuffer.pos = 0

        // Give back unused vertices
        drawList.vtxBuffer = drawList.vtxBuffer.resize(vtxWrite)
        drawList.idxBuffer = drawList.idxBuffer.resize(idxWrite)
        drawList.cmdBuffer.last().elemCount -= (idxExpectedSize - drawList.idxBuffer.lim)
        drawList._vtxWritePtr = vtxWrite
        drawList._idxWritePtr = idxWrite
        drawList._vtxCurrentIdx = drawList.vtxBuffer.lim
    }

    // [Internal] Don't use!

    val TABSIZE = 4

    fun buildLookupTable() {

        val maxCodepoint = glyphs.map { it.codepoint.i }.max()!!

        assert(glyphs.size < 0xFFFF) { "-1 is reserved" }
        indexAdvanceX.clear()
        indexLookup.clear()
        dirtyLookupTables = false
        growIndex(maxCodepoint + 1)
        glyphs.forEachIndexed { i, g ->
            indexAdvanceX[g.codepoint.i] = g.advanceX
            indexLookup[g.codepoint.i] = i
        }

        // Create a glyph to handle TAB
        // FIXME: Needs proper TAB handling but it needs to be contextualized (or we could arbitrary say that each string starts at "column 0" ?)
        if (findGlyph(' ') != null) {
            if (glyphs.last().codepoint != '\t')   // So we can call this function multiple times
                glyphs += FontGlyph()
            val tabGlyph = glyphs.last()
            tabGlyph put findGlyph(' ')!!
            tabGlyph.codepoint = '\t'
            tabGlyph.advanceX *= TABSIZE
            indexAdvanceX[tabGlyph.codepoint.i] = tabGlyph.advanceX
            indexLookup[tabGlyph.codepoint.i] = glyphs.lastIndex
        }

        fallbackGlyph = findGlyphNoFallback(fallbackChar)
        fallbackAdvanceX = fallbackGlyph?.advanceX ?: 0f
        for (i in 0 until maxCodepoint + 1)
            if (indexAdvanceX[i] < 0f)
                indexAdvanceX[i] = fallbackAdvanceX
    }

    fun clearOutputData() {
        fontSize = 0f
        fallbackAdvanceX = 0.0f
        glyphs.clear()
        indexAdvanceX.clear()
        indexLookup.clear()
        fallbackGlyph = null
        dirtyLookupTables = true
        fallbackAdvanceX = 0f
        configDataCount = 0
        configData.clear()
        containerAtlas.clearInputData()
        containerAtlas.clearTexData()
        ascent = 0f
        descent = 0f
        metricsTotalSurface = 0
    }

    private fun growIndex(newSize: Int) {
        assert(indexAdvanceX.size == indexLookup.size)
        if (newSize <= indexLookup.size)
            return
        for (i in indexLookup.size until newSize) {
            indexAdvanceX += -1f
            indexLookup += -1
        }
    }

    /** x0/y0/x1/y1 are offset from the character upper-left layout position, in pixels. Therefore x0/y0 are often fairly close to zero.
     *  Not to be mistaken with texture coordinates, which are held by u0/v0/u1/v1 in normalized format (0.0..1.0 on each texture axis). */
    fun addGlyph(codepoint: Int, x0: Float, y0: Float, x1: Float, y1: Float, u0: Float, v0: Float, u1: Float, v1: Float, advanceX: Float) {
        val glyph = FontGlyph()
        glyphs += glyph
        glyph.codepoint = codepoint.toChar()
        glyph.x0 = x0
        glyph.y0 = y0
        glyph.x1 = x1
        glyph.y1 = y1
        glyph.u0 = u0
        glyph.v0 = v0
        glyph.u1 = u1
        glyph.v1 = v1
        glyph.advanceX = advanceX + configData[0].glyphExtraSpacing.x  // Bake spacing into xAdvance

        if (configData[0].pixelSnapH)
            glyph.advanceX = (glyph.advanceX + 0.5f).i.f
        // Compute rough surface usage metrics (+1 to account for average padding, +0.99 to round)
        dirtyLookupTables = true
        metricsTotalSurface += ((glyph.u1 - glyph.u0) * containerAtlas.texSize.x + 1.99f).i *
                ((glyph.v1 - glyph.v0) * containerAtlas.texSize.y + 1.99f).i
    }

    /** Makes 'dst' character/glyph points to 'src' character/glyph. Currently needs to be called AFTER fonts have been built.  */
    fun addRemapChar(dst: Int, src: Int, overwriteDst: Boolean = true) {
        // Currently this can only be called AFTER the font has been built, aka after calling ImFontAtlas::GetTexDataAs*() function.
        assert(indexLookup.isNotEmpty())
        val indexSize = indexLookup.size

        if (dst < indexSize && indexLookup[dst] == -1 && !overwriteDst) // 'dst' already exists
            return
        if (src >= indexSize && dst >= indexSize) // both 'dst' and 'src' don't exist -> no-op
            return

        growIndex(dst + 1)
        indexLookup[dst] = indexLookup.getOrElse(src) { -1 }
        indexAdvanceX[dst] = indexAdvanceX.getOrElse(src) { 1f }
    }

    fun setCurrent() {
        assert(isLoaded) { "Font Atlas not created. Did you call io.Fonts->GetTexDataAsRGBA32 / GetTexDataAsAlpha8 ?" }
        assert(scale > 0f)
        g.font = this
        g.fontBaseSize = io.fontGlobalScale * g.font.fontSize * g.font.scale
        g.fontSize = g.currentWindow?.calcFontSize() ?: 0f

        val atlas = g.font.containerAtlas
        g.drawListSharedData.texUvWhitePixel put atlas.texUvWhitePixel
        g.drawListSharedData.font = g.font
        g.drawListSharedData.fontSize = g.fontSize
    }
}