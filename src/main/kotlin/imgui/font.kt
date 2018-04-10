package imgui


import glm_.*
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec2.operators.div
import glm_.vec4.Vec4
import imgui.ImGui.io
import imgui.ImGui.style
import imgui.internal.fileLoadToCharArray
import imgui.internal.isSpace
import imgui.internal.upperPowerOfTwo
import imgui.stb.*
import org.lwjgl.stb.*
import org.lwjgl.stb.STBRectPack.stbrp_pack_rects
import org.lwjgl.stb.STBTruetype.*
import org.lwjgl.system.MemoryUtil
import uno.buffer.bufferBig
import uno.buffer.destroy
import uno.convert.decode85
import uno.kotlin.buffers.isNotEmpty
import uno.stb.stb
import unsigned.toULong
import java.nio.ByteBuffer
import kotlin.math.floor


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
    /** Size in pixels for rasterizer.  */
    var sizePixels = 0f
    /** Rasterize at higher quality for sub-pixel positioning. We don't use sub-pixel positions on the Y axis.  */
    var oversample = Vec2i(3, 1)
    /** Align every glyph to pixel boundary. Useful e.g. if you are merging a non-pixel aligned font with the default
     *  font. If enabled, you can set OversampleH/V to 1.   */
    var pixelSnapH = false
    /** Extra spacing (in pixels) between glyphs. Only X axis is supported for now.    */
    var glyphExtraSpacing = Vec2()
    /** Offset all glyphs from this font input. */
    var glyphOffset = Vec2()
    /** Pointer to a user-provided list of Unicode range (2 value per range, values are inclusive, zero-terminated
     *  list). THE ARRAY DATA NEEDS TO PERSIST AS LONG AS THE FONT IS ALIVE.    */
    var glyphRanges = intArrayOf()
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

/** Load and rasterize multiple TTF/OTF fonts into a same texture.
 *  Sharing a texture for multiple fonts allows us to reduce the number of draw calls during rendering.
 *  We also add custom graphic data into the texture that serves for ImGui.
 *  1. (Optional) Call AddFont*** functions. If you don't call any, the default font will be loaded for you.
 *  2. Call GetTexDataAsAlpha8() or GetTexDataAsRGBA32() to build and retrieve pixels data.
 *  3. Upload the pixels data into a texture within your graphics system.
 *  4. Call SetTexID(my_tex_id); and pass the pointer/identifier to your texture. This value will be passed back to you
 *          during rendering to identify the texture.
 *  IMPORTANT: If you pass a 'glyph_ranges' array to AddFont*** functions, you need to make sure that your array persist
 *  up until the ImFont is build (when calling GetTextData*** or Build()). We only copy the pointer, not the data.
 *  We only copy the pointer, not the data. */
class FontAtlas {

    fun addFont(fontCfg: FontConfig): Font {

        assert(fontCfg.fontData.isNotEmpty())
        assert(fontCfg.sizePixels > 0.0f)

        // Create new font
        if (!fontCfg.mergeMode)
            fonts += Font()
        else
            assert(fonts.isNotEmpty())  /*  When using MergeMode make sure that a font has already been added before.
                                You can use io.fonts.addFontDefault to add the default imgui font.  */
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

        if (fontCfg.name.isEmpty()) fontCfg.name = "ProggyClean.ttf, 13px"
        if (fontCfg.sizePixels <= 0f) fontCfg.sizePixels = 13f

        val ttfCompressedBase85 = proggyCleanTtfCompressedDataBase85
        return addFontFromMemoryCompressedBase85TTF(ttfCompressedBase85, fontCfg.sizePixels, fontCfg, glyphRangesDefault)
                .apply { displayOffset.y = 1f }
    }

    fun addFontFromFileTTF(filename: String, sizePixels: Float, fontCfg: FontConfig = FontConfig(),
                           glyphRanges: IntArray = intArrayOf()): Font? {

        val chars = fileLoadToCharArray(filename) ?: return null
        if (fontCfg.name.isEmpty())
        // Store a short copy of filename into into the font name for convenience
            fontCfg.name = "${filename.substringAfterLast('/')}, %.0fpx".format(style.locale, sizePixels)
        return addFontFromMemoryTTF(chars, sizePixels, fontCfg, glyphRanges)
    }

    /** Note: Transfer ownership of 'ttfData' to FontAtlas! Will be deleted after build(). Set fontCfg.fontDataOwnedByAtlas
     *  to false to keep ownership. */
    fun addFontFromMemoryTTF(fontData: CharArray, sizePixels: Float, fontCfg: FontConfig = FontConfig(),
                             glyphRanges: IntArray = intArrayOf()): Font {

        assert(fontCfg.fontData.isEmpty())
        fontCfg.fontData = fontData
        fontCfg.fontDataBuffer = bufferBig(fontData.size).apply { fontData.forEachIndexed { i, c -> this[i] = c.b } }
        fontCfg.sizePixels = sizePixels
        if (glyphRanges.isNotEmpty())
            fontCfg.glyphRanges = glyphRanges
        return addFont(fontCfg)
    }

    /** @param compressedFontData still owned by caller. Compress with binary_to_compressed_c.cpp.   */
    fun addFontFromMemoryCompressedTTF(compressedFontData: CharArray, sizePixels: Float, fontCfg: FontConfig = FontConfig(),
                                       glyphRanges: IntArray = intArrayOf()): Font {

        val bufDecompressedData = stb.decompress(compressedFontData)

        assert(fontCfg.fontData.isEmpty())
        fontCfg.fontDataOwnedByAtlas = true
        return addFontFromMemoryTTF(bufDecompressedData, sizePixels, fontCfg, glyphRanges)
    }

    /** @param compressedFontDataBase85 still owned by caller. Compress with binary_to_compressed_c.cpp with -base85
     *  paramaeter  */
    fun addFontFromMemoryCompressedBase85TTF(compressedFontDataBase85: String, sizePixels: Float, fontCfg: FontConfig = FontConfig(),
                                             glyphRanges: IntArray = intArrayOf()): Font {

        val compressedTtf = decode85(compressedFontDataBase85)
        return addFontFromMemoryCompressedTTF(compressedTtf, sizePixels, fontCfg, glyphRanges)
    }

    /** Clear input data (all FontConfig structures including sizes, TTF data, glyph ranges, etc.) = all the data used
     *  to build the texture and fonts. */
    fun clearInputData() {
        configData.filter { it.fontData.isNotEmpty() && it.fontDataOwnedByAtlas }.forEach {
            it.fontData = charArrayOf()
        }

        // When clearing this we lose access to  the font name and other information used to build the font.
        fonts.filter { configData.contains(it.configData[0]) }.forEach {
            it.configData.clear()
            it.configDataCount = 0
        }
        configData.clear()
        customRects.clear()
        customRectIds.fill(-1)
    }

    /** Clear output texture data (CPU side). Saves RAM once the texture has been copied to graphics memory. */
    fun clearTexData() {
        texPixelsAlpha8?.destroy()
        texPixelsAlpha8 = null
        texPixelsRGBA32?.destroy()
        texPixelsRGBA32 = null
    }

    /** Clear output font data (glyphs storage, UV coordinates).    */
    fun clearFonts() {
        for (font in fonts) font.clearOutputData()
        fonts.clear()
    }

    /** Clear all input and output.   */
    fun clear() {
        clearInputData()
        clearTexData()
        clearFonts()
        stbClear()
    }

    /*  Build atlas, retrieve pixel data.
        User is in charge of copying the pixels into graphics memory (e.g. create a texture with your engine).
        Then store your texture handle with setTexID().
        RGBA32 format is provided for convenience and compatibility, but note that unless you use CustomRect to draw
        color data, the RGB pixels emitted from Fonts will all be white (~75% of waste).
        Pitch = Width * BytesPerPixels  */

    /** Build pixels data. This is automatically for you by the GetTexData*** functions.    */
    private fun build() = buildWithStbTrueType()

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
                texPixelsRGBA32 = bufferBig(texSize.x * texSize.y * 4)
                val dst = texPixelsRGBA32!!
                for (n in 0 until pixels.size) {
                    dst[n * 4] = 255.b
                    dst[n * 4 + 1] = 255.b
                    dst[n * 4 + 2] = 255.b
                    dst[n * 4 + 3] = pixels[n]
                }
            }
        }

        return Triple(texPixelsRGBA32!!, texSize, 4)
    }

    //-------------------------------------------
    // Glyph Ranges
    //-------------------------------------------

    /* Helpers to retrieve list of common Unicode ranges (2 value per range, values are inclusive)
     * NB: Make sure that your string are UTF-8 and NOT in your local code page. In C++11, you can create a UTF-8 string
     * literally using the u8"Hello world" syntax. See FAQ for details. */
    /** Retrieve list of range (2 int per range, values are inclusive), Basic Latin, Extended Latin  */

    /** Basic Latin + Latin Supplement  */
    val glyphRangesDefault get() = intArrayOf(0x0020, 0x00FF)
    /** Default + Korean characters */
    val glyphRangesKorean
        get() = intArrayOf(
                0x0020, 0x00FF, // Basic Latin + Latin Supplement
                0x3131, 0x3163, // Korean alphabets
                0xAC00, 0xD79D) // Korean characters
    /** Default + Hiragana, Katakana, Half-Width, Selection of 1946 Ideographs  */
    val glyphRangesJapanese: IntArray
        get() = with(jap) {
            if (!jap.fullRangesUnpacked) {
                // Unpack
                var codepoint = 0x4e00
                System.arraycopy(baseRanges, 0, fullRanges, 0, baseRanges.size)
                var dst = baseRanges.size
                var n = 0
                while (n < offsetsFrom0x4E00.size) {
                    codepoint += offsetsFrom0x4E00[n] + 1
                    fullRanges[dst] = codepoint
                    fullRanges[dst + 1] = codepoint
                    n++
                    dst += 2
                }
                fullRanges[dst] = 0
                fullRangesUnpacked = true
            }
            return fullRanges
        }

    private object jap {
        /** Store the 1946 ideograms code points as successive offsets from the initial unicode codepoint 0x4E00.
         *  Each offset has an implicit +1.
         *  This encoding is designed to helps us reduce the source code size.
         *  FIXME: Source a list of the revised 2136 joyo kanji list from 2010 and rebuild this.
         *  The current list was sourced from http://theinstructionlimit.com/author/renaudbedardrenaudbedard/page/3
         *  Note that you may use FontAtlas.glyphRangesBuilder to create your own ranges, by merging existing ranges
         *  or adding new characters.   */
        val offsetsFrom0x4E00 = intArrayOf(
                -1, 0, 1, 3, 0, 0, 0, 0, 1, 0, 5, 1, 1, 0, 7, 4, 6, 10, 0, 1, 9, 9, 7, 1, 3, 19, 1, 10, 7, 1, 0, 1, 0, 5, 1, 0, 6, 4, 2, 6, 0, 0, 12, 6, 8, 0, 3, 5, 0, 1, 0, 9, 0, 0, 8, 1, 1, 3, 4, 5, 13, 0, 0, 8, 2, 17,
                4, 3, 1, 1, 9, 6, 0, 0, 0, 2, 1, 3, 2, 22, 1, 9, 11, 1, 13, 1, 3, 12, 0, 5, 9, 2, 0, 6, 12, 5, 3, 12, 4, 1, 2, 16, 1, 1, 4, 6, 5, 3, 0, 6, 13, 15, 5, 12, 8, 14, 0, 0, 6, 15, 3, 6, 0, 18, 8, 1, 6, 14, 1,
                5, 4, 12, 24, 3, 13, 12, 10, 24, 0, 0, 0, 1, 0, 1, 1, 2, 9, 10, 2, 2, 0, 0, 3, 3, 1, 0, 3, 8, 0, 3, 2, 4, 4, 1, 6, 11, 10, 14, 6, 15, 3, 4, 15, 1, 0, 0, 5, 2, 2, 0, 0, 1, 6, 5, 5, 6, 0, 3, 6, 5, 0, 0, 1, 0,
                11, 2, 2, 8, 4, 7, 0, 10, 0, 1, 2, 17, 19, 3, 0, 2, 5, 0, 6, 2, 4, 4, 6, 1, 1, 11, 2, 0, 3, 1, 2, 1, 2, 10, 7, 6, 3, 16, 0, 8, 24, 0, 0, 3, 1, 1, 3, 0, 1, 6, 0, 0, 0, 2, 0, 1, 5, 15, 0, 1, 0, 0, 2, 11, 19,
                1, 4, 19, 7, 6, 5, 1, 0, 0, 0, 0, 5, 1, 0, 1, 9, 0, 0, 5, 0, 2, 0, 1, 0, 3, 0, 11, 3, 0, 2, 0, 0, 0, 0, 0, 9, 3, 6, 4, 12, 0, 14, 0, 0, 29, 10, 8, 0, 14, 37, 13, 0, 31, 16, 19, 0, 8, 30, 1, 20, 8, 3, 48,
                21, 1, 0, 12, 0, 10, 44, 34, 42, 54, 11, 18, 82, 0, 2, 1, 2, 12, 1, 0, 6, 2, 17, 2, 12, 7, 0, 7, 17, 4, 2, 6, 24, 23, 8, 23, 39, 2, 16, 23, 1, 0, 5, 1, 2, 15, 14, 5, 6, 2, 11, 0, 8, 6, 2, 2, 2, 14,
                20, 4, 15, 3, 4, 11, 10, 10, 2, 5, 2, 1, 30, 2, 1, 0, 0, 22, 5, 5, 0, 3, 1, 5, 4, 1, 0, 0, 2, 2, 21, 1, 5, 1, 2, 16, 2, 1, 3, 4, 0, 8, 4, 0, 0, 5, 14, 11, 2, 16, 1, 13, 1, 7, 0, 22, 15, 3, 1, 22, 7, 14,
                22, 19, 11, 24, 18, 46, 10, 20, 64, 45, 3, 2, 0, 4, 5, 0, 1, 4, 25, 1, 0, 0, 2, 10, 0, 0, 0, 1, 0, 1, 2, 0, 0, 9, 1, 2, 0, 0, 0, 2, 5, 2, 1, 1, 5, 5, 8, 1, 1, 1, 5, 1, 4, 9, 1, 3, 0, 1, 0, 1, 1, 2, 0, 0,
                2, 0, 1, 8, 22, 8, 1, 0, 0, 0, 0, 4, 2, 1, 0, 9, 8, 5, 0, 9, 1, 30, 24, 2, 6, 4, 39, 0, 14, 5, 16, 6, 26, 179, 0, 2, 1, 1, 0, 0, 0, 5, 2, 9, 6, 0, 2, 5, 16, 7, 5, 1, 1, 0, 2, 4, 4, 7, 15, 13, 14, 0, 0,
                3, 0, 1, 0, 0, 0, 2, 1, 6, 4, 5, 1, 4, 9, 0, 3, 1, 8, 0, 0, 10, 5, 0, 43, 0, 2, 6, 8, 4, 0, 2, 0, 0, 9, 6, 0, 9, 3, 1, 6, 20, 14, 6, 1, 4, 0, 7, 2, 3, 0, 2, 0, 5, 0, 3, 1, 0, 3, 9, 7, 0, 3, 4, 0, 4, 9, 1, 6, 0,
                9, 0, 0, 2, 3, 10, 9, 28, 3, 6, 2, 4, 1, 2, 32, 4, 1, 18, 2, 0, 3, 1, 5, 30, 10, 0, 2, 2, 2, 0, 7, 9, 8, 11, 10, 11, 7, 2, 13, 7, 5, 10, 0, 3, 40, 2, 0, 1, 6, 12, 0, 4, 5, 1, 5, 11, 11, 21, 4, 8, 3, 7,
                8, 8, 33, 5, 23, 0, 0, 19, 8, 8, 2, 3, 0, 6, 1, 1, 1, 5, 1, 27, 4, 2, 5, 0, 3, 5, 6, 3, 1, 0, 3, 1, 12, 5, 3, 3, 2, 0, 7, 7, 2, 1, 0, 4, 0, 1, 1, 2, 0, 10, 10, 6, 2, 5, 9, 7, 5, 15, 15, 21, 6, 11, 5, 20,
                4, 3, 5, 5, 2, 5, 0, 2, 1, 0, 1, 7, 28, 0, 9, 0, 5, 12, 5, 5, 18, 30, 0, 12, 3, 3, 21, 16, 25, 32, 9, 3, 14, 11, 24, 5, 66, 9, 1, 2, 0, 5, 9, 1, 5, 1, 8, 0, 8, 3, 3, 0, 1, 15, 1, 4, 8, 1, 2, 7, 0, 7, 2,
                8, 3, 7, 5, 3, 7, 10, 2, 1, 0, 0, 2, 25, 0, 6, 4, 0, 10, 0, 4, 2, 4, 1, 12, 5, 38, 4, 0, 4, 1, 10, 5, 9, 4, 0, 14, 4, 2, 5, 18, 20, 21, 1, 3, 0, 5, 0, 7, 0, 3, 7, 1, 3, 1, 1, 8, 1, 0, 0, 0, 3, 2, 5, 2, 11,
                6, 0, 13, 1, 3, 9, 1, 12, 0, 16, 6, 2, 1, 0, 2, 1, 12, 6, 13, 11, 2, 0, 28, 1, 7, 8, 14, 13, 8, 13, 0, 2, 0, 5, 4, 8, 10, 2, 37, 42, 19, 6, 6, 7, 4, 14, 11, 18, 14, 80, 7, 6, 0, 4, 72, 12, 36, 27,
                7, 7, 0, 14, 17, 19, 164, 27, 0, 5, 10, 7, 3, 13, 6, 14, 0, 2, 2, 5, 3, 0, 6, 13, 0, 0, 10, 29, 0, 4, 0, 3, 13, 0, 3, 1, 6, 51, 1, 5, 28, 2, 0, 8, 0, 20, 2, 4, 0, 25, 2, 10, 13, 10, 0, 16, 4, 0, 1, 0,
                2, 1, 7, 0, 1, 8, 11, 0, 0, 1, 2, 7, 2, 23, 11, 6, 6, 4, 16, 2, 2, 2, 0, 22, 9, 3, 3, 5, 2, 0, 15, 16, 21, 2, 9, 20, 15, 15, 5, 3, 9, 1, 0, 0, 1, 7, 7, 5, 4, 2, 2, 2, 38, 24, 14, 0, 0, 15, 5, 6, 24, 14,
                5, 5, 11, 0, 21, 12, 0, 3, 8, 4, 11, 1, 8, 0, 11, 27, 7, 2, 4, 9, 21, 59, 0, 1, 39, 3, 60, 62, 3, 0, 12, 11, 0, 3, 30, 11, 0, 13, 88, 4, 15, 5, 28, 13, 1, 4, 48, 17, 17, 4, 28, 32, 46, 0, 16, 0,
                18, 11, 1, 8, 6, 38, 11, 2, 6, 11, 38, 2, 0, 45, 3, 11, 2, 7, 8, 4, 30, 14, 17, 2, 1, 1, 65, 18, 12, 16, 4, 2, 45, 123, 12, 56, 33, 1, 4, 3, 4, 7, 0, 0, 0, 3, 2, 0, 16, 4, 2, 4, 2, 0, 7, 4, 5, 2, 26,
                2, 25, 6, 11, 6, 1, 16, 2, 6, 17, 77, 15, 3, 35, 0, 1, 0, 5, 1, 0, 38, 16, 6, 3, 12, 3, 3, 3, 0, 9, 3, 1, 3, 5, 2, 9, 0, 18, 0, 25, 1, 3, 32, 1, 72, 46, 6, 2, 7, 1, 3, 14, 17, 0, 28, 1, 40, 13, 0, 20,
                15, 40, 6, 38, 24, 12, 43, 1, 1, 9, 0, 12, 6, 0, 6, 2, 4, 19, 3, 7, 1, 48, 0, 9, 5, 0, 5, 6, 9, 6, 10, 15, 2, 11, 19, 3, 9, 2, 0, 1, 10, 1, 27, 8, 1, 3, 6, 1, 14, 0, 26, 0, 27, 16, 3, 4, 9, 6, 2, 23,
                9, 10, 5, 25, 2, 1, 6, 1, 1, 48, 15, 9, 15, 14, 3, 4, 26, 60, 29, 13, 37, 21, 1, 6, 4, 0, 2, 11, 22, 23, 16, 16, 2, 2, 1, 3, 0, 5, 1, 6, 4, 0, 0, 4, 0, 0, 8, 3, 0, 2, 5, 0, 7, 1, 7, 3, 13, 2, 4, 10,
                3, 0, 2, 31, 0, 18, 3, 0, 12, 10, 4, 1, 0, 7, 5, 7, 0, 5, 4, 12, 2, 22, 10, 4, 2, 15, 2, 8, 9, 0, 23, 2, 197, 51, 3, 1, 1, 4, 13, 4, 3, 21, 4, 19, 3, 10, 5, 40, 0, 4, 1, 1, 10, 4, 1, 27, 34, 7, 21,
                2, 17, 2, 9, 6, 4, 2, 3, 0, 4, 2, 7, 8, 2, 5, 1, 15, 21, 3, 4, 4, 2, 2, 17, 22, 1, 5, 22, 4, 26, 7, 0, 32, 1, 11, 42, 15, 4, 1, 2, 5, 0, 19, 3, 1, 8, 6, 0, 10, 1, 9, 2, 13, 30, 8, 2, 24, 17, 19, 1, 4,
                4, 25, 13, 0, 10, 16, 11, 39, 18, 8, 5, 30, 82, 1, 6, 8, 18, 77, 11, 13, 20, 75, 11, 112, 78, 33, 3, 0, 0, 60, 17, 84, 9, 1, 1, 12, 30, 10, 49, 5, 32, 158, 178, 5, 5, 6, 3, 3, 1, 3, 1, 4, 7, 6,
                19, 31, 21, 0, 2, 9, 5, 6, 27, 4, 9, 8, 1, 76, 18, 12, 1, 4, 0, 3, 3, 6, 3, 12, 2, 8, 30, 16, 2, 25, 1, 5, 5, 4, 3, 0, 6, 10, 2, 3, 1, 0, 5, 1, 19, 3, 0, 8, 1, 5, 2, 6, 0, 0, 0, 19, 1, 2, 0, 5, 1, 2, 5,
                1, 3, 7, 0, 4, 12, 7, 3, 10, 22, 0, 9, 5, 1, 0, 2, 20, 1, 1, 3, 23, 30, 3, 9, 9, 1, 4, 191, 14, 3, 15, 6, 8, 50, 0, 1, 0, 0, 4, 0, 0, 1, 0, 2, 4, 2, 0, 2, 3, 0, 2, 0, 2, 2, 8, 7, 0, 1, 1, 1, 3, 3, 17, 11,
                91, 1, 9, 3, 2, 13, 4, 24, 15, 41, 3, 13, 3, 1, 20, 4, 125, 29, 30, 1, 0, 4, 12, 2, 21, 4, 5, 5, 19, 11, 0, 13, 11, 86, 2, 18, 0, 7, 1, 8, 8, 2, 2, 22, 1, 2, 6, 5, 2, 0, 1, 2, 8, 0, 2, 0, 5, 2, 1, 0,
                2, 10, 2, 0, 5, 9, 2, 1, 2, 0, 1, 0, 4, 0, 0, 10, 2, 5, 3, 0, 6, 1, 0, 1, 4, 4, 33, 3, 13, 17, 3, 18, 6, 4, 7, 1, 5, 78, 0, 4, 1, 13, 7, 1, 8, 1, 0, 35, 27, 15, 3, 0, 0, 0, 1, 11, 5, 41, 38, 15, 22, 6,
                14, 14, 2, 1, 11, 6, 20, 63, 5, 8, 27, 7, 11, 2, 2, 40, 58, 23, 50, 54, 56, 293, 8, 8, 1, 5, 1, 14, 0, 1, 12, 37, 89, 8, 8, 8, 2, 10, 6, 0, 0, 0, 4, 5, 2, 1, 0, 1, 1, 2, 7, 0, 3, 3, 0, 4, 6, 0, 3, 2,
                19, 3, 8, 0, 0, 0, 4, 4, 16, 0, 4, 1, 5, 1, 3, 0, 3, 4, 6, 2, 17, 10, 10, 31, 6, 4, 3, 6, 10, 126, 7, 3, 2, 2, 0, 9, 0, 0, 5, 20, 13, 0, 15, 0, 6, 0, 2, 5, 8, 64, 50, 3, 2, 12, 2, 9, 0, 0, 11, 8, 20,
                109, 2, 18, 23, 0, 0, 9, 61, 3, 0, 28, 41, 77, 27, 19, 17, 81, 5, 2, 14, 5, 83, 57, 252, 14, 154, 263, 14, 20, 8, 13, 6, 57, 39, 38)
        val baseRanges = intArrayOf(
                0x0020, 0x00FF, // Basic Latin + Latin Supplement
                0x3000, 0x30FF, // Punctuations, Hiragana, Katakana
                0x31F0, 0x31FF, // Katakana Phonetic Extensions
                0xFF00, 0xFFEF) // Half-width characters
        var fullRangesUnpacked = false
        val fullRanges = IntArray(baseRanges.size + offsetsFrom0x4E00.size * 2 + 1)
    }

    /** Default + Japanese + full set of about 21000 CJK Unified Ideographs */
    val glyphRangesChinese
        get() = intArrayOf(
                0x0020, 0x00FF, // Basic Latin + Latin Supplement
                0x3000, 0x30FF, // Punctuations, Hiragana, Katakana
                0x31F0, 0x31FF, // Katakana Phonetic Extensions
                0xFF00, 0xFFEF, // Half-width characters
                0x4e00, 0x9FAF) // CJK Ideograms
    /** Default + about 400 Cyrillic characters */
    val glyphRangesCyrillic
        get() = intArrayOf(
                0x0020, 0x00FF, // Basic Latin + Latin Supplement
                0x0400, 0x052F, // Cyrillic + Cyrillic Supplement
                0x2DE0, 0x2DFF, // Cyrillic Extended-A
                0xA640, 0xA69F) // Cyrillic Extended-B
    /** Default + Thai characters   */
    val GetGlyphRangesThai
        get() = intArrayOf(
                0x0020, 0x00FF, // Basic Latin
                0x2010, 0x205E, // Punctuations
                0x0E00, 0x0E7F) // Thai

    // Helpers to build glyph ranges from text data. Feed your application strings/characters to it then call BuildRanges().
//    struct GlyphRangesBuilder
//    {
//            ImVector<unsigned char> UsedChars;  // Store 1-bit per Unicode code point (0=unused, 1=used)
//            GlyphRangesBuilder()                { UsedChars.resize(0x10000 / 8); memset(UsedChars.Data, 0, 0x10000 / 8); }
//            bool           GetBit(int n)        { return (UsedChars[n >> 3] & (1 << (n & 7))) != 0; }
//            void           SetBit(int n)        { UsedChars[n >> 3] |= 1 << (n & 7); }  // Set bit 'c' in the array
//            void           AddChar(ImWchar c)   { SetBit(c); }                          // Add character
//            IMGUI_API void AddText(const char* text, const char* text_end = NULL);      // Add string (each character of the UTF-8 string are added)
//            IMGUI_API void AddRanges(const ImWchar* ranges);                            // Add ranges, e.g. builder.AddRanges(ImFontAtlas::GetGlyphRangesDefault) to force add all of ASCII/Latin+Ext
//            IMGUI_API void BuildRanges(ImVector<ImWchar>* out_ranges);                  // Output new ranges
//        };
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
        assert(texSize greaterThan 0)   // Font atlas needs to be built before we can calculate UV coordinates
        assert(rect.isPacked)                // Make sure the rectangle has been packed
        outUvMin.put(rect.x.f * texUvScale.x, rect.y.f * texUvScale.y)
        outUvMax.put((rect.x + rect.width).f * texUvScale.x, (rect.y + rect.height).f * texUvScale.y)
    }

    fun getMouseCursorTexData(cursor: MouseCursor, outOffset: Vec2, outSize: Vec2, outUv: Array<Vec2>): Boolean {

        if (cursor == MouseCursor.None) return false

        if (flags has FontAtlasFlag.NoMouseCursors) return false

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
        /** Don't round the height to next power of two */
        NoPowerOfTwoHeight,
        /** Don't build software mouse cursors into the atlas   */
        NoMouseCursors;

        val i = 1 shl ordinal
    }

    infix fun Int.has(flag: FontAtlasFlag) = and(flag.i) != 0
    infix fun Int.hasnt(flag: FontAtlasFlag) = and(flag.i) == 0

    /** Build flags (see ImFontAtlasFlags_) */
    var flags: FontAtlasFlags = 0
    /** User data to refer to the texture once it has been uploaded to user's graphic systems. It is passed back to you
    during rendering via the DrawCmd structure.   */
    var texId: TextureID = -1
    /** 1 component per pixel, each component is unsigned 8-bit. Total size = texSize.x * texSize.y  */
    var texPixelsAlpha8: ByteBuffer? = null
    /** 4 component per pixel, each component is unsigned 8-bit. Total size = texSize.x * texSize.y * 4  */
    var texPixelsRGBA32: ByteBuffer? = null
    /** Texture size calculated during Build(). */
    var texSize = Vec2i()
    /** Texture width desired by user before Build(). Must be a power-of-two. If have many glyphs your graphics API have
     *  texture size restrictions you may want to increase texture width to decrease height.    */
    var texDesiredWidth = 0
    /** Padding between glyphs within texture in pixels. Defaults to 1. */
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
    private val customRectIds = IntArray(1, { -1 })

    private fun customRectCalcUV(rect: CustomRect, outUvMin: Vec2, outUvMax: Vec2) {
        assert(texSize.x > 0 && texSize.y > 0)   // Font atlas needs to be built before we can calculate UV coordinates
        assert(rect.isPacked)                // Make sure the rectangle has been packed
        outUvMin.put(rect.x / texSize.x, rect.y / texSize.y)
        outUvMax.put((rect.x + rect.width) / texSize.x, (rect.y + rect.height) / texSize.y)
    }

    // ImFontAtlas internals

    fun buildWithStbTrueType(): Boolean {

        assert(configData.isNotEmpty())

        buildRegisterDefaultCustomRects()

        texId = -1
        texSize put 0
        texUvScale put 0f
        texUvWhitePixel put 0f
        clearTexData()
        val inRange = IntArray(2)

        // Count glyphs/ranges
        var totalGlyphsCount = 0
        var totalRangesCount = 0
        for (cfg in configData) {
            if (cfg.glyphRanges.isEmpty()) cfg.glyphRanges = glyphRangesDefault
            var i = 0
            inRange[0] = cfg.glyphRanges[i++]
            inRange[1] = cfg.glyphRanges[i++]
            while (inRange[0] != 0 && inRange[1] != 0) {
                totalGlyphsCount += (inRange[1] - inRange[0]) + 1
                inRange[0] = cfg.glyphRanges.getOrElse(i++, { 0 })
                inRange[1] = cfg.glyphRanges.getOrElse(i++, { 0 })
                totalRangesCount++
            }
        }

        /*  We need a width for the skyline algorithm. Using a dumb heuristic here to decide of width. User can override
            texDesiredWidth and texGlyphPadding if they wish. Width doesn't really matter much, but some API/GPU have
            texture size limitations and increasing width can decrease height.  */
        texSize.x = when {
            texDesiredWidth > 0 -> texDesiredWidth
            totalGlyphsCount > 4000 -> 4096
            totalGlyphsCount > 2000 -> 2048
            totalGlyphsCount > 1000 -> 1024
            else -> 512
        }
        texSize.y = 0

        // Start packing
        val maxTexHeight = 1024 * 32
        val spc = STBTTPackContext.create()
        if (!stbtt_PackBegin(spc, null, texSize.x, maxTexHeight, 0, texGlyphPadding, MemoryUtil.NULL))
            return false

        /*  Pack our extra data rectangles first, so it will be on the upper-left corner of our texture (UV will have
            small values).  */
        buildPackCustomRects(spc.packInfo)

        // Initialize font information (so we can error without any cleanup)
        class FontTempBuildData {

            var fontInfo = STBTTFontinfo.create()
            lateinit var rects: STBRPRect.Buffer
            var rectsCount = 0
            lateinit var ranges: STBTTPackRange.Buffer
            var rangesCount = 0
        }

        val tmpArray = Array(configData.size, { FontTempBuildData() })
        configData.forEachIndexed { input, cfg ->
            val tmp = tmpArray[input]
            assert(with(cfg.dstFont!!) { !isLoaded || containerAtlas == this@FontAtlas })

            val fontOffset = stbtt_GetFontOffsetForIndex(cfg.fontDataBuffer, cfg.fontNo)
            assert(fontOffset >= 0)
            if (!stbtt_InitFont(tmp.fontInfo, cfg.fontDataBuffer, fontOffset)) {
                texSize put 0   // Reset output on failure
                return false
            }
        }

        // Allocate packing character data and flag packed characters buffer as non-packed (x0=y0=x1=y1=0)
        var (bufPackedcharsN, bufRectsN, bufRangesN) = Triple(0, 0, 0)
        val bufPackedchars = STBTTPackedchar.calloc(totalGlyphsCount)
        val bufRects = STBRPRect.calloc(totalGlyphsCount)
        val bufRanges = STBTTPackRange.calloc(totalRangesCount)

        /*  First font pass: pack all glyphs (no rendering at this point, we are working with rectangles in an
            infinitely tall texture at this point)  */
        configData.forEachIndexed { input, cfg ->

            val tmp = tmpArray[input]

            // Setup ranges
            var fontGlyphsCount = 0
            var fontRangesCount = 0
            var i = 0
            inRange[0] = cfg.glyphRanges[i++]
            inRange[1] = cfg.glyphRanges[i++]
            while (inRange[0] != 0 && inRange[1] != 0) {
                fontGlyphsCount += (inRange[1] - inRange[0]) + 1
                inRange[0] = cfg.glyphRanges.getOrElse(i++, { 0 })
                inRange[1] = cfg.glyphRanges.getOrElse(i++, { 0 })
                fontRangesCount++
            }
            var address = bufRanges.address() + bufRangesN * STBTTPackRange.SIZEOF
            tmp.ranges = STBTTPackRange.create(address, fontRangesCount)
            tmp.rangesCount = fontRangesCount
            bufRangesN += fontRangesCount
            i = 0
            while (i < fontRangesCount) {
                inRange[0] = cfg.glyphRanges[i * 2]
                inRange[1] = cfg.glyphRanges[i * 2 + 1]
                val range = tmp.ranges[i++]
                range.fontSize = cfg.sizePixels
                range.firstUnicodeCodepointInRange = inRange[0]
                range.numChars = (inRange[1] - inRange[0]) + 1
                address = bufPackedchars.address() + bufPackedcharsN * STBTTPackedchar.SIZEOF
                range.chardataForRange = STBTTPackedchar.create(address, range.numChars)
                bufPackedcharsN += range.numChars
            }

            // Pack
            tmp.rects = STBRPRect.create(bufRects.address() + bufRectsN * STBRPRect.SIZEOF, fontGlyphsCount)
            tmp.rectsCount = fontGlyphsCount
            bufRectsN += fontGlyphsCount
            stbtt_PackSetOversampling(spc, cfg.oversample)
            val n = stbtt_PackFontRangesGatherRects(spc, tmp.fontInfo, tmp.ranges, tmp.rects)
            assert(n == fontGlyphsCount)
            stbrp_pack_rects(spc.packInfo, tmp.rects)   // fuck, Omar modified his stb_rect_pack.h, we shall also have our own?

            // Extend texture height
            for (r in tmp.rects)
                if (r.wasPacked)
                    texSize.y = glm.max(texSize.y, r.y + r.h)
        }
        assert(bufRectsN == totalGlyphsCount)
        assert(bufPackedcharsN == totalGlyphsCount)
        assert(bufRangesN == totalRangesCount)

        // Create texture
        texSize.y = if (flags has FontAtlasFlag.NoPowerOfTwoHeight) texSize.y + 1 else texSize.y.upperPowerOfTwo
        texUvScale = 1f / Vec2(texSize)
        texPixelsAlpha8 = bufferBig(texSize.x * texSize.y)
        spc.pixels = texPixelsAlpha8!!
        spc.height = texSize.y

        // Second pass: render characters
        for (input in 0 until configData.size) {
            val cfg = configData[input]
            val tmp = tmpArray[input]
            stbtt_PackSetOversampling(spc, cfg.oversample)
            stbtt_PackFontRangesRenderIntoRects(spc, tmp.fontInfo, tmp.ranges, tmp.rects)
            if (cfg.rasterizerMultiply != 1f) {
                val multiplyTable = buildMultiplyCalcLookupTable(cfg.rasterizerMultiply)
                for (r in tmp.rects)
                    if (r.wasPacked)
                        buildMultiplyRectAlpha8(multiplyTable, spc.pixels, r, spc.strideInBytes)
            }
//            tmp.rects.free()
        }

        // End packing
        stbtt_PackEnd(spc)
//        bufRects.free() TODO check why crashes, container is null

        // Third pass: setup ImFont and glyphs for runtime
        for (input in 0 until configData.size) {

            val cfg = configData[input]
            val tmp = tmpArray[input]
            // We can have multiple input fonts writing into a same destination font (when using MergeMode=true)
            val dstFont = cfg.dstFont!!
            if(cfg.mergeMode)
                dstFont.buildLookupTable()

            val fontScale = stbtt_ScaleForPixelHeight(tmp.fontInfo, cfg.sizePixels)
            val (unscaledAscent, unscaledDescent, unscaledLineGap) = stbtt_GetFontVMetrics(tmp.fontInfo)

            val ascent = floor(unscaledAscent * fontScale + if(unscaledAscent > 0f) +1 else -1)
            val descent = floor(unscaledDescent * fontScale + if(unscaledDescent > 0f) +1 else -1)
            buildSetupFont(dstFont, cfg, ascent, descent)
            val off = Vec2(cfg.glyphOffset.x, cfg.glyphOffset.y + (dstFont.ascent + 0.5f).i.f)

            tmp.ranges.forEach { range ->

                for (charIdx in 0 until range.numChars) {

                    val pc = range.chardataForRange[charIdx]
                    if (pc.x0 == 0 && pc.x1 == 0 && pc.y0 == 0 && pc.y1 == 0) continue

                    val codepoint = range.firstUnicodeCodepointInRange + charIdx
                    if (cfg.mergeMode && dstFont.findGlyphNoFallback(codepoint) != null) continue

                    val q = STBTTAlignedQuad.create()
                    stbtt_GetPackedQuad(range.chardataForRange, texSize, charIdx, Vec2(), q, false)

                    dstFont.addGlyph(codepoint, q.x0 + off.x, q.y0 + off.y,
                            q.x1 + off.x, q.y1 + off.y, q.s0, q.t0, q.s1, q.t1, pc.advanceX)
                }
            }
        }

        // Cleanup temporaries
        bufPackedchars.free()
        bufRanges.free()

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
        if (!fontConfig.mergeMode) with(font) {
            containerAtlas = this@FontAtlas
            configData.add(fontConfig) // TODO replace [0] if not empty?
            configDataCount = 0
            fontSize = fontConfig.sizePixels
            this.ascent = ascent
            this.descent = descent
            glyphs.clear()
            metricsTotalSurface = 0
        }
        font.configDataCount++
    }

    fun buildPackCustomRects(packContext: STBRPContext) {

        val userRects = customRects
        // We expect at least the default custom rects to be registered, else something went wrong.
        assert(userRects.isNotEmpty())
        val packRects = STBRPRect.calloc(userRects.size)    // calloc -> all 0
        for (i in userRects.indices) {
            packRects[i].w = userRects[i].width
            packRects[i].h = userRects[i].height
        }
        stbrp_pack_rects(packContext, packRects)
        for (i in userRects.indices)
            if (packRects[i].wasPacked) {
                userRects[i].x = packRects[i].x
                userRects[i].y = packRects[i].y
                assert(packRects[i].w == userRects[i].width && packRects[i].h == userRects[i].height)
                texSize.y = glm.max(texSize.y, packRects[i].y + packRects[i].h)
            }
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

    fun buildMultiplyCalcLookupTable(inBrightenFactor: Float) = CharArray(256, {
        val value = (it * inBrightenFactor).i
        (if (value > 255) 255 else (value and 0xFF)).c
    })

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
        val wHalf = 90
        val h = 27
        val id = 0x80000000.i
        val pixels = (
                "..-         -XXXXXXX-    X    -           X           -XXXXXXX          -          XXXXXXX" +
                        "..-         -X.....X-   X.X   -          X.X          -X.....X          -          X.....X" +
                        "---         -XXX.XXX-  X...X  -         X...X         -X....X           -           X....X" +
                        "X           -  X.X  - X.....X -        X.....X        -X...X            -            X...X" +
                        "XX          -  X.X  -X.......X-       X.......X       -X..X.X           -           X.X..X" +
                        "X.X         -  X.X  -XXXX.XXXX-       XXXX.XXXX       -X.X X.X          -          X.X X.X" +
                        "X..X        -  X.X  -   X.X   -          X.X          -XX   X.X         -         X.X   XX" +
                        "X...X       -  X.X  -   X.X   -    XX    X.X    XX    -      X.X        -        X.X      " +
                        "X....X      -  X.X  -   X.X   -   X.X    X.X    X.X   -       X.X       -       X.X       " +
                        "X.....X     -  X.X  -   X.X   -  X..X    X.X    X..X  -        X.X      -      X.X        " +
                        "X......X    -  X.X  -   X.X   - X...XXXXXX.XXXXXX...X -         X.X   XX-XX   X.X         " +
                        "X.......X   -  X.X  -   X.X   -X.....................X-          X.X X.X-X.X X.X          " +
                        "X........X  -  X.X  -   X.X   - X...XXXXXX.XXXXXX...X -           X.X..X-X..X.X           " +
                        "X.........X -XXX.XXX-   X.X   -  X..X    X.X    X..X  -            X...X-X...X            " +
                        "X..........X-X.....X-   X.X   -   X.X    X.X    X.X   -           X....X-X....X           " +
                        "X......XXXXX-XXXXXXX-   X.X   -    XX    X.X    XX    -          X.....X-X.....X          " +
                        "X...X..X    ---------   X.X   -          X.X          -          XXXXXXX-XXXXXXX          " +
                        "X..X X..X   -       -XXXX.XXXX-       XXXX.XXXX       ------------------------------------" +
                        "X.X  X..X   -       -X.......X-       X.......X       -    XX           XX    -           " +
                        "XX    X..X  -       - X.....X -        X.....X        -   X.X           X.X   -           " +
                        "      X..X          -  X...X  -         X...X         -  X..X           X..X  -           " +
                        "       XX           -   X.X   -          X.X          - X...XXXXXXXXXXXXX...X -           " +
                        "------------        -    X    -           X           -X.....................X-           " +
                        "                    ----------------------------------- X...XXXXXXXXXXXXX...X -           " +
                        "                                                      -  X..X           X..X  -           " +
                        "                                                      -   X.X           X.X   -           " +
                        "                                                      -    XX           XX    -           ").toCharArray()

        val cursorDatas = arrayOf(
                // Pos ........ Size ......... Offset ......
                arrayOf(Vec2(0, 3), Vec2(12, 19), Vec2(0)),         // MouseCursor.Arrow
                arrayOf(Vec2(13, 0), Vec2(7, 16), Vec2(4, 8)),   // MouseCursor.TextInput
                arrayOf(Vec2(31, 0), Vec2(23), Vec2(11)),              // MouseCursor.Move
                arrayOf(Vec2(21, 0), Vec2(9, 23), Vec2(5, 11)),  // MouseCursor.ResizeNS
                arrayOf(Vec2(55, 18), Vec2(23, 9), Vec2(11, 5)), // MouseCursor.ResizeEW
                arrayOf(Vec2(73, 0), Vec2(17), Vec2(9)),               // MouseCursor.ResizeNESW
                arrayOf(Vec2(55, 0), Vec2(17), Vec2(9)))               // MouseCursor.ResizeNWSE
    }
}

/** Font runtime data and rendering
 *  ImFontAtlas automatically loads a default embedded font for you when you call GetTexDataAsAlpha8() or
 *  GetTexDataAsRGBA32().   */
class Font {

    // Members: Hot ~62/78 bytes
    /** <user set>, Height of characters, set during loading (don't change after loading)   */
    var fontSize = 0f
    /** Base font scale, multiplied by the per-window font scale which you can adjust with SetFontScale()   */
    var scale = 1f
    /** Offset font rendering by xx pixels  */
    var displayOffset = Vec2(0f, 0f)
    /** All glyphs. */
    val glyphs = ArrayList<FontGlyph>()
    /** Sparse. Glyphs.advanceX in a directly indexable way (more cache-friendly, for CalcTextSize functions which are
    often bottleneck in large UI).  */
    val indexAdvanceX = ArrayList<Float>()
    /** Sparse. Index glyphs by Unicode code-point. */
    val indexLookup = ArrayList<Int>()
    /** == FindGlyph(FontFallbackChar)  */
    var fallbackGlyph: FontGlyph? = null
    /** == FallbackGlyph.advanceX  */
    var fallbackAdvanceX = 0f
    /** Replacement glyph if one isn't found. Only set via SetFallbackChar()    */
    var fallbackChar = '?'
//
//    // Members: Cold ~18/26 bytes
    /** Number of ImFontConfig involved in creating this font. Bigger than 1 when merging multiple font sources into one ImFont.    */
    var configDataCount = 0

    /** Pointer within ContainerAtlas->ConfigData   */
    var configData = ArrayList<FontConfig>()

    var configDataIdx = 0
    /** What we has been loaded into    */
    lateinit var containerAtlas: FontAtlas
    /** Ascent: distance from top to bottom of e.g. 'A' [0..FontSize]   */
    var ascent = 0f

    var descent = 0f

    var dirtyLookupTables = true
    /** Total surface in pixels to get an idea of the font rasterization/texture cost (not exact, we approximate the cost of padding
    between glyphs)    */
    var metricsTotalSurface = 0

    //
//    // Methods
//    IMGUI_API ImFont();
//    IMGUI_API ~ImFont();

    fun clearOutputData() {
        fontSize = 0f
        displayOffset = Vec2(0.0f, 1.0f)
        glyphs.clear()
        indexAdvanceX.clear()
        indexLookup.clear()
        fallbackGlyph = null
        fallbackAdvanceX = 0f
        configDataCount = 0
        configData.clear()
//        containerAtlas = NULL TODO check
        ascent = 0f
        descent = 0f
        dirtyLookupTables = true
        metricsTotalSurface = 0
    }

    fun buildLookupTable() {

        val maxCodepoint = glyphs.map { it.codepoint.i }.max()!!

        assert(glyphs.size < 0xFFFF) // -1 is reserved
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
                glyphs.add(FontGlyph())
            val tabGlyph = glyphs.last()
            tabGlyph put findGlyph(' ')!!
            tabGlyph.codepoint = '\t'
            tabGlyph.advanceX *= 4
            indexAdvanceX[tabGlyph.codepoint.i] = tabGlyph.advanceX
            indexLookup[tabGlyph.codepoint.i] = glyphs.size - 1
        }

        fallbackGlyph = findGlyphNoFallback(fallbackChar)
        fallbackAdvanceX = fallbackGlyph?.advanceX ?: 0f
        for (i in 0 until maxCodepoint + 1)
            if (indexAdvanceX[i] < 0f)
                indexAdvanceX[i] = fallbackAdvanceX
    }

    fun findGlyph(c: Char) = findGlyph(c.i)
    fun findGlyph(c: Int) = indexLookup.getOrNull(c)?.let { glyphs[it] } ?: fallbackGlyph
    fun findGlyphNoFallback(c: Char) = findGlyphNoFallback(c.i)
    fun findGlyphNoFallback(c: Int) = indexLookup.getOrNull(c)?.let { glyphs[it] }

    //    IMGUI_API void              SetFallbackChar(ImWchar c);
    fun getCharAdvance(c: Char) = if (c < indexAdvanceX.size) indexAdvanceX[c.i] else fallbackAdvanceX

    val isLoaded get() = ::containerAtlas.isInitialized

    val debugName get() = configData.getOrNull(0)?.name ?: "<unknown>"

    /*  'maxWidth' stops rendering after a certain width (could be turned into a 2d size). FLT_MAX to disable.
        'wrapWidth' enable automatic word-wrapping across multiple lines to fit into given width. 0.0f to disable. */
    fun calcTextSizeA(size: Float, maxWidth: Float, wrapWidth: Float, text: String, textEnd: Int = text.length,
                      remaining: IntArray? = null): Vec2 { // utf8

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
                        if (c.isSpace)
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

    fun calcWordWrapPositionA(scale: Float, text: CharArray, ptr: Int, textEnd: Int, wrapWidth: Float): Int {

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
        val wrapWidth = wrapWidth / scale   // We work with unscaled widths to avoid scaling every characters

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
            if (c.isSpace) {
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
            val c = Vec2(x + it.x1 * scale, y + it.y1 * scale)
            drawList.primRectUV(a, c, Vec2(it.u0, it.v0), Vec2(it.u1, it.v1), col)
        }
    }

    //    const ImVec4& clipRect, const char* text, const char* textEnd, float wrapWidth = 0.0f, bool cpuFineClip = false) const;
    fun renderText(drawList: DrawList, size: Float, pos: Vec2, col: Int, clipRect: Vec4, text: CharArray, textEnd: Int = text.size,
                   wrapWidth: Float = 0f, cpuFineClip: Boolean = false) {

        // Align to be pixel perfect
        pos.x = pos.x.i.f + displayOffset.x
        pos.y = pos.y.i.f + displayOffset.y
        var (x, y) = pos
        if (y > clipRect.w) return

        val scale = size / fontSize
        val lineHeight = fontSize * scale
        val wordWrapEnabled = wrapWidth > 0f
        var wordWrapEol = 0

        // Skip non-visible lines
        var s = 0
        if (!wordWrapEnabled && y + lineHeight < clipRect.y)
            while (s < textEnd && text[s] != '\n')  // Fast-forward to next line
                s++

        // Reserve vertices for remaining worse case (over-reserving is useful and easily amortized)
        val vtxCountMax = (textEnd - s) * 4
        val idxCountMax = (textEnd - s) * 6
        val idxExpectedSize = drawList.idxBuffer.size + idxCountMax
        drawList.primReserve(idxCountMax, vtxCountMax)

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
                        if (c.isSpace) s++
                        else if (c == '\n') {
                            s++
                            break
                        } else break
                    }
                    continue
                }
            }
            // Decode and advance source
            val c = text[s]
            /*  JVM imgui specific, not 0x80 because on jvm we have Unicode with surrogates characters (instead of utf8)
                    https://www.ibm.com/developerworks/library/j-unicode/index.html             */
            if (c < Char.MIN_HIGH_SURROGATE)
                s += 1
            else {
                TODO("Probabily surrogate character")
//                s += textCharFromUtf8(& c, s, text_end)
//                if (c == 0) // Malformed UTF-8?
//                    break
            }

            if (c < 32) {
                if (c == '\n') {
                    x = pos.x
                    y += lineHeight

                    if (y > clipRect.w) break

                    if (!wordWrapEnabled && y + lineHeight < clipRect.y)
                        while (s < textEnd && text[s] != '\n')  // Fast-forward to next line
                            s++
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
                            idxBuffer[idxWrite + 0] = vtxCurrentIdx
                            idxBuffer[idxWrite + 1] = vtxCurrentIdx + 1
                            idxBuffer[idxWrite + 2] = vtxCurrentIdx + 2
                            idxBuffer[idxWrite + 3] = vtxCurrentIdx
                            idxBuffer[idxWrite + 4] = vtxCurrentIdx + 2
                            idxBuffer[idxWrite + 5] = vtxCurrentIdx + 3
                            vtxBuffer[vtxWrite + 0].pos.x = x1
                            vtxBuffer[vtxWrite + 0].pos.y = y1
                            vtxBuffer[vtxWrite + 0].col = col
                            vtxBuffer[vtxWrite + 0].uv.x = u1
                            vtxBuffer[vtxWrite + 0].uv.y = v1
                            vtxBuffer[vtxWrite + 1].pos.x = x2
                            vtxBuffer[vtxWrite + 1].pos.y = y1
                            vtxBuffer[vtxWrite + 1].col = col
                            vtxBuffer[vtxWrite + 1].uv.x = u2
                            vtxBuffer[vtxWrite + 1].uv.y = v1
                            vtxBuffer[vtxWrite + 2].pos.x = x2
                            vtxBuffer[vtxWrite + 2].pos.y = y2
                            vtxBuffer[vtxWrite + 2].col = col
                            vtxBuffer[vtxWrite + 2].uv.x = u2
                            vtxBuffer[vtxWrite + 2].uv.y = v2
                            vtxBuffer[vtxWrite + 3].pos.x = x1
                            vtxBuffer[vtxWrite + 3].pos.y = y2
                            vtxBuffer[vtxWrite + 3].col = col
                            vtxBuffer[vtxWrite + 3].uv.x = u1
                            vtxBuffer[vtxWrite + 3].uv.y = v2
                            vtxWrite += 4
                            vtxCurrentIdx += 4
                            idxWrite += 6
                        }
                    }
                }
            }
            x += charWidth
        }

        // Give back unused vertices
        for (i in vtxWrite until drawList.vtxBuffer.size)
            drawList.vtxBuffer.removeAt(drawList.vtxBuffer.lastIndex)
        for (i in idxWrite until drawList.idxBuffer.size)
            drawList.idxBuffer.removeAt(drawList.idxBuffer.lastIndex)
        drawList.cmdBuffer.last().elemCount -= (idxExpectedSize - drawList.idxBuffer.size)
        drawList._vtxWritePtr = vtxWrite
        drawList._idxWritePtr = idxWrite
        drawList._vtxCurrentIdx = drawList.vtxBuffer.size
    }

    // [Internal]

    private fun growIndex(newSize: Int) {
        assert(indexAdvanceX.size == indexLookup.size)
        if (newSize <= indexLookup.size)
            return
        for (i in indexLookup.size until newSize) {
            indexAdvanceX.add(-1f)
            indexLookup.add(-1)
        }
    }

    fun addGlyph(codepoint: Int, x0: Float, y0: Float, x1: Float, y1: Float, u0: Float, v0: Float, u1: Float, v1: Float, advanceX: Float) {
        val glyph = FontGlyph()
        glyphs.add(glyph)
        glyph.codepoint = codepoint.toChar()
        glyph.x0 = x0
        glyph.y0 = y0
        glyph.x1 = x1
        glyph.y1 = y1
        glyph.u0 = u0
        glyph.v0 = v0
        glyph.u1 = u1
        glyph.v1 = v1
        glyph.advanceX = advanceX + configData[0].glyphExtraSpacing.x  // Bake spacing into advanceX

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
        indexLookup[dst] = indexLookup.getOrElse(src, { -1 })
        indexAdvanceX[dst] = indexAdvanceX.getOrElse(src, { 1f })
    }

    fun setCurrent() {
        assert(isLoaded)    // Font Atlas not created. Did you call io.Fonts->GetTexDataAsRGBA32 / GetTexDataAsAlpha8 ?
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

private val proggyCleanTtfCompressedDataBase85 by lazy {
    "7])#######hV0qs'/###[),##/l:\$#Q6>##5[n42>c-TH`->>#/e>11NNV=Bv(*:.F?uu#(gRU.o0XGH`\$vhLG1hxt9?W`#,5LsCp#-i>.r\$<\$6pD>Lb';9Crc6tgXmKVeU2cD4Eo3R/" +
            "2*>]b(MC;\$jPfY.;h^`IWM9<Lh2TlS+f-s\$o6Q<BWH`YiU.xfLq\$N;\$0iR/GX:U(jcW2p/W*q?-qmnUCI;jHSAiFWM.R*kU@C=GH?a9wp8f\$e.-4^Qg1)Q-GL(lf(r/7GrRgwV%MS=C#" +
            "`8ND>Qo#t'X#(v#Y9w0#1D\$CIf;W'#pWUPXOuxXuU(H9M(1<q-UE31#^-V'8IRUo7Qf./L>=Ke\$\$'5F%)]0^#0X@U.a<r:QLtFsLcL6##lOj)#.Y5<-R&KgLwqJfLgN&;Q?gI^#DY2uL" +
            "i@^rMl9t=cWq6##weg>\$FBjVQTSDgEKnIS7EM9>ZY9w0#L;>>#Mx&4Mvt//L[MkA#W@lK.N'[0#7RL_&#w+F%HtG9M#XL`N&.,GM4Pg;-<nLENhvx>-VsM.M0rJfLH2eTM`*oJMHRC`N" +
            "kfimM2J,W-jXS:)r0wK#@Fge\$U>`w'N7G#\$#fB#\$E^\$#:9:hk+eOe--6x)F7*E%?76%^GMHePW-Z5l'&GiF#\$956:rS?dA#fiK:)Yr+`&#0j@'DbG&#^\$PG.Ll+DNa<XCMKEV*N)LN/N" +
            "*b=%Q6pia-Xg8I\$<MR&,VdJe\$<(7G;Ckl'&hF;;\$<_=X(b.RS%%)###MPBuuE1V:v&cX&#2m#(&cV]`k9OhLMbn%s\$G2,B\$BfD3X*sp5#l,\$R#]x_X1xKX%b5U*[r5iMfUo9U`N99hG)" +
            "tm+/Us9pG)XPu`<0s-)WTt(gCRxIg(%6sfh=ktMKn3j)<6<b5Sk_/0(^]AaN#(p/L>&VZ>1i%h1S9u5o@YaaW\$e+b<TWFn/Z:Oh(Cx2\$lNEoN^e)#CFY@@I;BOQ*sRwZtZxRcU7uW6CX" +
            "ow0i(?\$Q[cjOd[P4d)]>ROPOpxTO7Stwi1::iB1q)C_=dV26J;2,]7op\$]uQr@_V7\$q^%lQwtuHY]=DX,n3L#0PHDO4f9>dC@O>HBuKPpP*E,N+b3L#lpR/MrTEH.IAQk.a>D[.e;mc." +
            "x]Ip.PH^'/aqUO/\$1WxLoW0[iLA<QT;5HKD+@qQ'NQ(3_PLhE48R.qAPSwQ0/WK?Z,[x?-J;jQTWA0X@KJ(_Y8N-:/M74:/-ZpKrUss?d#dZq]DAbkU*JqkL+nwX@@47`5>w=4h(9.`G" +
            "CRUxHPeR`5Mjol(dUWxZa(>STrPkrJiWx`5U7F#.g*jrohGg`cg:lSTvEY/EV_7H4Q9[Z%cnv;JQYZ5q.l7Zeas:HOIZOB?G<Nald\$qs]@]L<J7bR*>gv:[7MI2k).'2(\$5FNP&EQ(,)" +
            "U]W]+fh18.vsai00);D3@4ku5P?DP8aJt+;qUM]=+b'8@;mViBKx0DE[-auGl8:PJ&Dj+M6OC]O^((##]`0i)drT;-7X`=-H3[igUnPG-NZlo.#k@h#=Ork\$m>a>\$-?Tm\$UV(?#P6YY#" +
            "'/###xe7q.73rI3*pP/\$1>s9)W,JrM7SN]'/4C#v\$U`0#V.[0>xQsH\$fEmPMgY2u7Kh(G%siIfLSoS+MK2eTM\$=5,M8p`A.;_R%#u[K#\$x4AG8.kK/HSB==-'Ie/QTtG?-.*^N-4B/ZM" +
            "_3YlQC7(p7q)&](`6_c)\$/*JL(L-^(]\$wIM`dPtOdGA,U3:w2M-0<q-]L_?^)1vw'.,MRsqVr.L;aN&#/EgJ)PBc[-f>+WomX2u7lqM2iEumMTcsF?-aT=Z-97UEnXglEn1K-bnEO`gu" +
            "Ft(c%=;Am_Qs@jLooI&NX;]0#j4#F14;gl8-GQpgwhrq8'=l_f-b49'UOqkLu7-##oDY2L(te+Mch&gLYtJ,MEtJfLh'x'M=\$CS-ZZ%P]8bZ>#S?YY#%Q&q'3^Fw&?D)UDNrocM3A76/" +
            "/oL?#h7gl85[qW/NDOk%16ij;+:1a'iNIdb-ou8.P*w,v5#EI\$TWS>Pot-R*H'-SEpA:g)f+O\$%%`kA#G=8RMmG1&O`>to8bC]T&\$,n.LoO>29sp3dt-52U%VM#q7'DHpg+#Z9%H[K<L" +
            "%a2E-grWVM3@2=-k22tL]4\$##6We'8UJCKE[d_=%wI;'6X-GsLX4j^SgJ\$##R*w,vP3wK#iiW&#*h^D&R?jp7+/u&#(AP##XU8c\$fSYW-J95_-Dp[g9wcO&#M-h1OcJlc-*vpw0xUX&#" +
            "OQFKNX@QI'IoPp7nb,QU//MQ&ZDkKP)X<WSVL(68uVl&#c'[0#(s1X&xm\$Y%B7*K:eDA323j998GXbA#pwMs-jgD\$9QISB-A_(aN4xoFM^@C58D0+Q+q3n0#3U1InDjF682-SjMXJK)(" +
            "h\$hxua_K]ul92%'BOU&#BRRh-slg8KDlr:%L71Ka:.A;%YULjDPmL<LYs8i#XwJOYaKPKc1h:'9Ke,g)b),78=I39B;xiY\$bgGw-&.Zi9InXDuYa%G*f2Bq7mn9^#p1vv%#(Wi-;/Z5h" +
            "o;#2:;%d&#x9v68C5g?ntX0X)pT`;%pB3q7mgGN)3%(P8nTd5L7GeA-GL@+%J3u2:(Yf>et`e;)f#Km8&+DC\$I46>#Kr]]u-[=99tts1.qb#q72g1WJO81q+eN'03'eM>&1XxY-caEnO" +
            "j%2n8)),?ILR5^.Ibn<-X-Mq7[a82Lq:F&#ce+S9wsCK*x`569E8ew'He]h:sI[2LM\$[guka3ZRd6:t%IG:;\$%YiJ:Nq=?eAw;/:nnDq0(CYcMpG)qLN4\$##&J<j\$UpK<Q4a1]MupW^-" +
            "sj_\$%[HK%'F####QRZJ::Y3EGl4'@%FkiAOg#p[##O`gukTfBHagL<LHw%q&OV0##F=6/:chIm0@eCP8X]:kFI%hl8hgO@RcBhS-@Qb\$%+m=hPDLg*%K8ln(wcf3/'DW-\$.lR?n[nCH-" +
            "eXOONTJlh:.RYF%3'p6sq:UIMA945&^HFS87@\$EP2iG<-lCO\$%c`uKGD3rC\$x0BL8aFn--`ke%#HMP'vh1/R&O_J9'um,.<tx[@%wsJk&bUT2`0uMv7gg#qp/ij.L56'hl;.s5CUrxjO" +
            "M7-##.l+Au'A&O:-T72L]P`&=;ctp'XScX*rU.>-XTt,%OVU4)S1+R-#dg0/Nn?Ku1^0f\$B*P:Rowwm-`0PKjYDDM'3]d39VZHEl4,.j']Pk-M.h^&:0FACm\$maq-&sgw0t7/6(^xtk%" +
            "LuH88Fj-ekm>GA#_>568x6(OFRl-IZp`&b,_P'\$M<Jnq79VsJW/mWS*PUiq76;]/NM_>hLbxfc\$mj`,O;&%W2m`Zh:/)Uetw:aJ%]K9h:TcF]u_-Sj9,VK3M.*'&0D[Ca]J9gp8,kAW]" +
            "%(?A%R\$f<->Zts'^kn=-^@c4%-pY6qI%J%1IGxfLU9CP8cbPlXv);C=b),<2mOvP8up,UVf3839acAWAW-W?#ao/^#%KYo8fRULNd2.>%m]UK:n%r\$'sw]J;5pAoO_#2mO3n,'=H5(et" +
            "Hg*`+RLgv>=4U8guD\$I%D:W>-r5V*%j*W:Kvej.Lp\$<M-SGZ':+Q_k+uvOSLiEo(<aD/K<CCc`'Lx>'?;++O'>()jLR-^u68PHm8ZFWe+ej8h:9r6L*0//c&iH&R8pRbA#Kjm%upV1g:" +
            "a_#Ur7FuA#(tRh#.Y5K+@?3<-8m0\$PEn;J:rh6?I6uG<-`wMU'ircp0LaE_OtlMb&1#6T.#FDKu#1Lw%u%+GM+X'e?YLfjM[VO0MbuFp7;>Q&#WIo)0@F%q7c#4XAXN-U&VB<HFF*qL(" +
            "\$/V,;(kXZejWO`<[5??ewY(*9=%wDc;,u<'9t3W-(H1th3+G]ucQ]kLs7df(\$/*JL]@*t7Bu_G3_7mp7<iaQjO@.kLg;x3B0lqp7Hf,^Ze7-##@/c58Mo(3;knp0%)A7?-W+eI'o8)b<" +
            "nKnw'Ho8C=Y>pqB>0ie&jhZ[?iLR@@_AvA-iQC(=ksRZRVp7`.=+NpBC%rh&3]R:8XDmE5^V8O(x<<aG/1N\$#FX\$0V5Y6x'aErI3I\$7x%E`v<-BY,)%-?Psf*l?%C3.mM(=/M0:JxG'?" +
            "7WhH%o'a<-80g0NBxoO(GH<dM]n.+%q@jH?f.UsJ2Ggs&4<-e47&Kl+f//9@`b+?.TeN_&B8Ss?v;^Trk;f#YvJkl&w\$]>-+k?'(<S:68tq*WoDfZu';mM?8X[ma8W%*`-=;D.(nc7/;" +
            ")g:T1=^J\$&BRV(-lTmNB6xqB[@0*o.erM*<SWF]u2=st-*(6v>^](H.aREZSi,#1:[IXaZFOm<-ui#qUq2\$##Ri;u75OK#(RtaW-K-F`S+cF]uN`-KMQ%rP/Xri.LRcB##=YL3BgM/3M" +
            "D?@f&1'BW-)Ju<L25gl8uhVm1hL\$##*8###'A3/LkKW+(^rWX?5W_8g)a(m&K8P>#bmmWCMkk&#TR`C,5d>g)F;t,4:@_l8G/5h4vUd%&%950:VXD'QdWoY-F\$BtUwmfe\$YqL'8(PWX(" +
            "P?^@Po3\$##`MSs?DWBZ/S>+4%>fX,VWv/w'KD`LP5IbH;rTV>n3cEK8U#bX]l-/V+^lj3;vlMb&[5YQ8#pekX9JP3XUC72L,,?+Ni&co7ApnO*5NK,((W-i:\$,kp'UDAO(G0Sq7MVjJs" +
            "bIu)'Z,*[>br5fX^:FPAWr-m2KgL<LUN098kTF&#lvo58=/vjDo;.;)Ka*hLR#/k=rKbxuV`>Q_nN6'8uTG&#1T5g)uLv:873UpTLgH+#FgpH'_o1780Ph8KmxQJ8#H72L4@768@Tm&Q" +
            "h4CB/5OvmA&,Q&QbUoi\$a_%3M01H)4x7I^&KQVgtFnV+;[Pc>[m4k//,]1?#`VY[Jr*3&&slRfLiVZJ:]?=K3Sw=[\$=uRB?3xk48@aeg<Z'<\$#4H)6,>e0jT6'N#(q%.O=?2S]u*(m<-" +
            "V8J'(1)G][68hW\$5'q[GC&5j`TE?m'esFGNRM)j,ffZ?-qx8;->g4t*:CIP/[Qap7/9'#(1sao7w-.qNUdkJ)tCF&#B^;xGvn2r9FEPFFFcL@.iFNkTve\$m%#QvQS8U@)2Z+3K:AKM5i" +
            "sZ88+dKQ)W6>J%CL<KE>`.d*(B`-n8D9oK<Up]c\$X\$(,)M8Zt7/[rdkqTgl-0cuGMv'?>-XV1q['-5k'cAZ69e;D_?\$ZPP&s^+7])\$*\$#@QYi9,5P&#9r+\$%CE=68>K8r0=dSC%%(@p7" +
            ".m7jilQ02'0-VWAg<a/''3u.=4L\$Y)6k/K:_[3=&jvL<L0C/2'v:^;-DIBW,B4E68:kZ;%?8(Q8BH=kO65BW?xSG&#@uU,DS*,?.+(o(#1vCS8#CHF>TlGW'b)Tq7VT9q^*^\$\$.:&N@@" +
            "\$&)WHtPm*5_rO0&e%K&#-30j(E4#'Zb.o/(Tpm\$>K'f@[PvFl,hfINTNU6u'0pao7%XUp9]5.>%h`8_=VYbxuel.NTSsJfLacFu3B'lQSu/m6-Oqem8T+oE--\$0a/k]uj9EwsG>%veR*" +
            "hv^BFpQj:K'#SJ,sB-'#](j.Lg92rTw-*n%@/;39rrJF,l#qV%OrtBeC6/,;qB3ebNW[?,Hqj2L.1NP&GjUR=1D8QaS3Up&@*9wP?+lo7b?@%'k4`p0Z\$22%K3+iCZj?XJN4Nm&+YF]u" +
            "@-W\$U%VEQ/,,>>#)D<h#`)h0:<Q6909ua+&VU%n2:cG3FJ-%@Bj-DgLr`Hw&HAKjKjseK</xKT*)B,N9X3]krc12t'pgTV(Lv-tL[xg_%=M_q7a^x?7Ubd>#%8cY#YZ?=,`Wdxu/ae&#" +
            "w6)R89tI#6@s'(6Bf7a&?S=^ZI_kS&ai`&=tE72L_D,;^R)7[\$s<Eh#c&)q.MXI%#v9ROa5FZO%sF7q7Nwb&#ptUJ:aqJe\$Sl68%.D###EC><?-aF&#RNQv>o8lKN%5/\$(vdfq7+ebA#" +
            "u1p]ovUKW&Y%q]'>\$1@-[xfn\$7ZTp7mM,G,Ko7a&Gu%G[RMxJs[0MM%wci.LFDK)(<c`Q8N)jEIF*+?P2a8g%)\$q]o2aH8C&<SibC/q,(e:v;-b#6[\$NtDZ84Je2KNvB#\$P5?tQ3nt(0" +
            "d=j.LQf./Ll33+(;q3L-w=8dX\$#WF&uIJ@-bfI>%:_i2B5CsR8&9Z&#=mPEnm0f`<&c)QL5uJ#%u%lJj+D-r;BoF&#4DoS97h5g)E#o:&S4weDF,9^Hoe`h*L+_a*NrLW-1pG_&2UdB8" +
            "6e%B/:=>)N4xeW.*wft-;\$'58-ESqr<b?UI(_%@[P46>#U`'6AQ]m&6/`Z>#S?YY#Vc;r7U2&326d=w&H####?TZ`*4?&.MK?LP8Vxg>\$[QXc%QJv92.(Db*B)gb*BM9dM*hJMAo*c&#" +
            "b0v=Pjer]\$gG&JXDf->'StvU7505l9\$AFvgYRI^&<^b68?j#q9QX4SM'RO#&sL1IM.rJfLUAj221]d##DW=m83u5;'bYx,*Sl0hL(W;;\$doB&O/TQ:(Z^xBdLjL<Lni;''X.`\$#8+1GD" +
            ":k\$YUWsbn8ogh6rxZ2Z9]%nd+>V#*8U_72Lh+2Q8Cj0i:6hp&\$C/:p(HK>T8Y[gHQ4`4)'\$Ab(Nof%V'8hL&#<NEdtg(n'=S1A(Q1/I&4([%dM`,Iu'1:_hL>SfD07&6D<fp8dHM7/g+" +
            "tlPN9J*rKaPct&?'uBCem^jn%9_K)<,C5K3s=5g&GmJb*[SYq7K;TRLGCsM-\$\$;S%:Y@r7AK0pprpL<Lrh,q7e/%KWK:50I^+m'vi`3?%Zp+<-d+\$L-Sv:@.o19n\$s0&39;kn;S%BSq*" +
            "\$3WoJSCLweV[aZ'MQIjO<7;X-X;&+dMLvu#^UsGEC9WEc[X(wI7#2.(F0jV*eZf<-Qv3J-c+J5AlrB#\$p(H68LvEA'q3n0#m,[`*8Ft)FcYgEud]CWfm68,(aLA\$@EFTgLXoBq/UPlp7" +
            ":d[/;r_ix=:TF`S5H-b<LI&HY(K=h#)]Lk\$K14lVfm:x\$H<3^Ql<M`\$OhapBnkup'D#L\$Pb_`N*g]2e;X/Dtg,bsj&K#2[-:iYr'_wgH)NUIR8a1n#S?Yej'h8^58UbZd+^FKD*T@;6A" +
            "7aQC[K8d-(v6GI\$x:T<&'Gp5Uf>@M.*J:;\$-rv29'M]8qMv-tLp,'886iaC=Hb*YJoKJ,(j%K=H`K.v9HggqBIiZu'QvBT.#=)0ukruV&.)3=(^1`o*Pj4<-<aN((^7('#Z0wK#5GX@7" +
            "u][`*S^43933A4rl][`*O4CgLEl]v\$1Q3AeF37dbXk,.)vj#x'd`;qgbQR%FW,2(?LO=s%Sc68%NP'##Aotl8x=BE#j1UD([3\$M(]UI2LX3RpKN@;/#f'f/&_mt&F)XdF<9t4)Qa.*kT" +
            "LwQ'(TTB9.xH'>#MJ+gLq9-##@HuZPN0]u:h7.T..G:;\$/Usj(T7`Q8tT72LnYl<-qx8;-HV7Q-&Xdx%1a,hC=0u+HlsV>nuIQL-5<N?)NBS)QN*_I,?&)2'IM%L3I)X((e/dl2&8'<M" +
            ":^#M*Q+[T.Xri.LYS3v%fF`68h;b-X[/En'CR.q7E)p'/kle2HM,u;^%OKC-N+Ll%F9CF<Nf'^#t2L,;27W:0O@6##U6W7:\$rJfLWHj\$#)woqBefIZ.PK<b*t7ed;p*_m;4ExK#h@&]>" +
            "_>@kXQtMacfD.m-VAb8;IReM3\$wf0''hra*so568'Ip&vRs849'MRYSp%:t:h5qSgwpEr\$B>Q,;s(C#\$)`svQuF\$##-D,##,g68@2[T;.XSdN9Qe)rpt._K-#5wF)sP'##p#C0c%-Gb%" +
            "hd+<-j'Ai*x&&HMkT]C'OSl##5RG[JXaHN;d'uA#x._U;.`PU@(Z3dt4r152@:v,'R.Sj'w#0<-;kPI)FfJ&#AYJ&#//)>-k=m=*XnK\$>=)72L]0I%>.G690a:\$##<,);?;72#?x9+d;" +
            "^V'9;jY@;)br#q^YQpx:X#Te\$Z^'=-=bGhLf:D6&bNwZ9-ZD#n^9HhLMr5G;']d&6'wYmTFmL<LD)F^%[tC'8;+9E#C\$g%#5Y>q9wI>P(9mI[>kC-ekLC/R&CH+s'B;K-M6\$EB%is00:" +
            "+A4[7xks.LrNk0&E)wILYF@2L'0Nb\$+pv<(2.768/FrY&h\$^3i&@+G%JT'<-,v`3;_)I9M^AE]CN?Cl2AZg+%4iTpT3<n-&%H%b<FDj2M<hH=&Eh<2Len\$b*aTX=-8QxN)k11IM1c^j%" +
            "9s<L<NFSo)B?+<-(GxsF,^-Eh@\$4dXhN\$+#rxK8'je'D7k`e;)2pYwPA'_p9&@^18ml1^[@g4t*[JOa*[=Qp7(qJ_oOL^('7fB&Hq-:sf,sNj8xq^>\$U4O]GKx'm9)b@p7YsvK3w^YR-" +
            "CdQ*:Ir<(\$u&)#(&?L9Rg3H)4fiEp^iI9O8KnTj,]H?D*r7'M;PwZ9K0E^k&-cpI;.p/6_vwoFMV<->#%Xi.LxVnrU(4&8/P+:hLSKj\$#U%]49t'I:rgMi'FL@a:0Y-uA[39',(vbma*" +
            "hU%<-SRF`Tt:542R_VV\$p@[p8DV[A,?1839FWdF<TddF<9Ah-6&9tWoDlh]&1SpGMq>Ti1O*H&#(AL8[_P%.M>v^-))qOT*F5Cq0`Ye%+\$B6i:7@0IX<N+T+0MlMBPQ*Vj>SsD<U4JHY" +
            "8kD2)2fU/M#\$e.)T4,_=8hLim[&);?UkK'-x?'(:siIfL<\$pFM`i<?%W(mGDHM%>iWP,##P`%/L<eXi:@Z9C.7o=@(pXdAO/NLQ8lPl+HPOQa8wD8=^GlPa8TKI1CjhsCTSLJM'/Wl>-" +
            "S(qw%sf/@%#B6;/U7K]uZbi^Oc^2n<bhPmUkMw>%t<)'mEVE''n`WnJra\$^TKvX5B>;_aSEK',(hwa0:i4G?.Bci.(X[?b*(\$,=-n<.Q%`(X=?+@Am*Js0&=3bh8K]mL<LoNs'6,'85`" +
            "0?t/'_U59@]ddF<#LdF<eWdF<OuN/45rY<-L@&#+fm>69=Lb,OcZV/);TTm8VI;?%OtJ<(b4mq7M6:u?KRdF<gR@2L=FNU-<b[(9c/ML3m;Z[\$oF3g)GAWqpARc=<ROu7cL5l;-[A]%/" +
            "+fsd;l#SafT/f*W]0=O'\$(Tb<[)*@e775R-:Yob%g*>l*:xP?Yb.5)%w_I?7uk5JC+FS(m#i'k.'a0i)9<7b'fs'59hq\$*5Uhv##pi^8+hIEBF`nvo`;'l0.^S1<-wUK2/Coh58KKhLj" +
            "M=SO*rfO`+qC`W-On.=AJ56>>i2@2LH6A:&5q`?9I3@@'04&p2/LVa*T-4<-i3;M9UvZd+N7>b*eIwg:CC)c<>nO&#<IGe;__.thjZl<%w(Wk2xmp4Q@I#I9,DF]u7-P=.-_:YJ]aS@V" +
            "?6*C()dOp7:WL,b&3Rg/.cmM9&r^>\$(>.Z-I&J(Q0Hd5Q%7Co-b`-c<N(6r@ip+AurK<m86QIth*#v;-OBqi+L7wDE-Ir8K['m+DDSLwK&/.?-V%U_%3:qKNu\$_b*B-kp7NaD'QdWQPK" +
            "Yq[@>P)hI;*_F]u`Rb[.j8_Q/<&>uu+VsH\$sM9TA%?)(vmJ80),P7E>)tjD%2L=-t#fK[%`v=Q8<FfNkgg^oIbah*#8/Qt\$F&:K*-(N/'+1vMB,u()-a.VUU*#[e%gAAO(S>WlA2);Sa" +
            ">gXm8YB`1d@K#n]76-a\$U,mF<fX]idqd)<3,]J7JmW4`6]uks=4-72L(jEk+:bJ0M^q-8Dm_Z?0olP1C9Sa&H[d&c\$ooQUj]Exd*3ZM@-WGW2%s',B-_M%>%Ul:#/'xoFM9QX-\$.QN'>" +
            "[%\$Z\$uF6pA6Ki2O5:8w*vP1<-1`[G,)-m#>0`P&#eb#.3i)rtB61(o'\$?X3B</R90;eZ]%Ncq;-Tl]#F>2Qft^ae_5tKL9MUe9b*sLEQ95C&`=G?@Mj=wh*'3E>=-<)Gt*Iw)'QG:`@I" +
            "wOf7&]1i'S01B+Ev/Nac#9S;=;YQpg_6U`*kVY39xK,[/6Aj7:'1Bm-_1EYfa1+o&o4hp7KN_Q(OlIo@S%;jVdn0'1<Vc52=u`3^o-n1'g4v58Hj&6_t7\$##?M)c<\$bgQ_'SY((-xkA#" +
            "Y(,p'H9rIVY-b,'%bCPF7.J<Up^,(dU1VY*5#WkTU>h19w,WQhLI)3S#f\$2(eb,jr*b;3Vw]*7NH%\$c4Vs,eD9>XW8?N]o+(*pgC%/72LV-u<Hp,3@e^9UB1J+ak9-TN/mhKPg+AJYd\$" +
            "MlvAF_jCK*.O-^(63adMT->W%iewS8W6m2rtCpo'RS1R84=@paTKt)>=%&1[)*vp'u+x,VrwN;&]kuO9JDbg=pO\$J*.jVe;u'm0dr9l,<*wMK*Oe=g8lV_KEBFkO'oU]^=[-792#ok,)" +
            "i]lR8qQ2oA8wcRCZ^7w/Njh;?.stX?Q1>S1q4Bn\$)K1<-rGdO'\$Wr.Lc.CG)\$/*JL4tNR/,SVO3,aUw'DJN:)Ss;wGn9A32ijw%FL+Z0Fn.U9;reSq)bmI32U==5ALuG&#Vf1398/pVo" +
            "1*c-(aY168o<`JsSbk-,1N;\$>0:OUas(3:8Z972LSfF8eb=c-;>SPw7.6hn3m`9^Xkn(r.qS[0;T%&Qc=+STRxX'q1BNk3&*eu2;&8q\$&x>Q#Q7^Tf+6<(d%ZVmj2bDi%.3L2n+4W'\$P" +
            "iDDG)g,r%+?,\$@?uou5tSe2aN_AQU*<h`e-GI7)?OK2A.d7_c)?wQ5AS@DL3r#7fSkgl6-++D:'A,uq7SvlB\$pcpH'q3n0#_%dY#xCpr-l<F0NR@-##FEV6NTF6##\$l84N1w?AO>'IAO" +
            "URQ##V^Fv-XFbGM7Fl(N<3DhLGF%q.1rC\$#:T__&Pi68%0xi_&[qFJ(77j_&JWoF.V735&T,[R*:xFR*K5>>#`bW-?4Ne_&6Ne_&6Ne_&n`kr-#GJcM6X;uM6X;uM(.a..^2TkL%oR(#" +
            ";u.T%fAr%4tJ8&><1=GHZ_+m9/#H1F^R#SC#*N=BA9(D?v[UiFY>>^8p,KKF.W]L29uLkLlu/+4T<XoIB&hx=T1PcDaB&;HH+-AFr?(m9HZV)FKS8JCw;SD=6[^/DZUL`EUDf]GGlG&>" +
            "w\$)F./^n3+rlo+DB;5sIYGNk+i1t-69Jg--0pao7Sm#K)pdHW&;LuDNH@H>#/X-TI(;P>#,Gc>#0Su>#4`1?#8lC?#<xU?#@.i?#D:%@#HF7@#LRI@#P_[@#Tkn@#Xw*A#]-=A#a9OA#" +
            "d<F&#*;G##.GY##2Sl##6`(\$#:l:\$#>xL\$#B.`\$#F:r\$#JF.%#NR@%#R_R%#Vke%#Zww%#_-4&#3^Rh%Sflr-k'MS.o?.5/sWel/wpEM0%3'/1)K^f1-d>G21&v(35>V`39V7A4=onx4" +
            "A1OY5EI0;6Ibgr6M\$HS7Q<)58C5w,;WoA*#[%T*#`1g*#d=#+#hI5+#lUG+#pbY+#tnl+#x\$),#&1;,#*=M,#.I`,#2Ur,#6b.-#;w[H#iQtA#m^0B#qjBB#uvTB##-hB#'9\$C#+E6C#" +
            "/QHC#3^ZC#7jmC#;v)D#?,<D#C8ND#GDaD#KPsD#O]/E#g1A5#KA*1#gC17#MGd;#8(02#L-d3#rWM4#Hga1#,<w0#T.j<#O#'2#CYN1#qa^:#_4m3#o@/=#eG8=#t8J5#`+78#4uI-#" +
            "m3B2#SB[8#Q0@8#i[*9#iOn8#1Nm;#^sN9#qh<9#:=x-#P;K2#\$%X9#bC+.#Rg;<#mN=.#MTF.#RZO.#2?)4#Y#(/#[)1/#b;L/#dAU/#0Sv;#lY\$0#n`-0#sf60#(F24#wrH0#%/e0#" +
            "TmD<#%JSMFove:CTBEXI:<eh2g)B,3h2^G3i;#d3jD>)4kMYD4lVu`4m`:&5niUA5@(A5BA1]PBB:xlBCC=2CDLXMCEUtiCf&0g2'tN?PGT4CPGT4CPGT4CPGT4CPGT4CPGT4CPGT4CP" +
            "GT4CPGT4CPGT4CPGT4CPGT4CPGT4CP-qekC`.9kEg^+F\$kwViFJTB&5KTB&5KTB&5KTB&5KTB&5KTB&5KTB&5KTB&5KTB&5KTB&5KTB&5KTB&5KTB&5KTB&5KTB&5o,^<-28ZI'O?;xp" +
            "O?;xpO?;xpO?;xpO?;xpO?;xpO?;xpO?;xpO?;xpO?;xpO?;xpO?;xpO?;xpO?;xp;7q-#lLYI:xvD=#"
}