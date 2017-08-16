package imgui

//import imgui.TrueType.packFontRangesGatherRects
//import imgui.TrueType.packFontRangesRenderIntoRects
//import imgui.TrueType.packSetOversampling
import gli.wasInit
import glm_.*
import glm_.detail.Random.float
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import imgui.internal.isSpace
import imgui.internal.upperPowerOfTwo
import imgui.stb.*
import org.lwjgl.stb.*
import org.lwjgl.stb.STBRectPack.stbrp_pack_rects
import org.lwjgl.stb.STBTruetype.*
import uno.buffer.byteBufferBig
import uno.buffer.destroy
import uno.convert.decode85
import uno.stb.stb
import java.nio.ByteBuffer
import imgui.Context as g


class FontConfig {

    var fontData = charArrayOf()
    lateinit var fontDataBuffer: ByteBuffer
    /** TTF data ownership taken by the container ImFontAtlas (will delete memory itself). Set to true  */
    var fontDataOwnedByAtlas = true
    /** Index of font within TTF file   */
    var fontNo = 0
    /** Size in pixels for rasterizer   */
    var sizePixels = 0.0f
    /** Rasterize at higher quality for sub-pixel positioning. We don't use sub-pixel positions on the Y axis.  */
    var oversample = Vec2i(3, 1)
    /** Align every glyph to pixel boundary. Useful e.g. if you are merging a non-pixel aligned font with the default
     *  font. If enabled, you can set OversampleH/V to 1.   */
    var pixelSnapH = false
    /** Extra spacing (in pixels) between glyphs    */
    var glyphExtraSpacing = Vec2()
    /** Offset all glyphs from this font input  */
    var glyphOffset = Vec2()
    /** Pointer to a user-provided list of Unicode range (2 value per range, values are inclusive, zero-terminated
     *  list). THE ARRAY DATA NEEDS TO PERSIST AS LONG AS THE FONT IS ALIVE.    */
    var glyphRanges = intArrayOf()
    /** Merge into previous ImFont, so you can combine multiple inputs font into one ImFont (e.g. ASCII font + icons +
     *  Japanese glyphs). You may want to use GlyphOffset.y when merge font of different heights.   */
    var mergeMode = false

    // [Internal]
    /** Name (strictly to ease debugging)   */
    var name = ""

    lateinit var dstFont: Font
}

/** Load and rasterize multiple TTF fonts into a same texture.
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
            fonts.add(Font())
        else
            assert(fonts.isNotEmpty())  /*  When using MergeMode make sure that a font has already been added before.
                                You can use ImGui::GetIO().Fonts->AddFontDefault() to add the default imgui font.  */
        configData.add(fontCfg)
        if (!wasInit { fontCfg.dstFont })
            fontCfg.dstFont = fonts.last()
        if (!fontCfg.fontDataOwnedByAtlas)
            fontCfg.fontDataOwnedByAtlas = true
//            memcpy(new_font_cfg.FontData, font_cfg->FontData, (size_t)new_font_cfg.FontDataSize) TODO check, same object?

        // Invalidate texture
        clearTexData()
        return fontCfg.dstFont
    }

    // Load embedded ProggyClean.ttf at size 13, disable oversampling
    fun addFontDefault(): Font {
        val fontCfg = FontConfig()
        fontCfg.oversample put 1
        fontCfg.pixelSnapH = true
        return addFontDefault(fontCfg)
    }

    fun addFontDefault(fontCfg: FontConfig): Font {

        if (fontCfg.name.isEmpty()) fontCfg.name = "ProggyClean.ttf, 13px"

        val ttfCompressedBase85 = proggyCleanTtfCompressedDataBase85
        return addFontFromMemoryCompressedBase85TTF(ttfCompressedBase85, 13.0f, fontCfg, glyphRangesDefault)
    }

    //    IMGUI_API ImFont*           AddFontFromFileTTF(const char* filename, float size_pixels, const ImFontConfig* font_cfg = NULL, const ImWchar* glyph_ranges = NULL);

    /** NBM Transfer ownership of 'ttf_data' to ImFontAtlas, unless font_cfg_template->FontDataOwnedByAtlas == false.
     *  Owned TTF buffer will be deleted after Build(). */
    fun addFontFromMemoryTTF(ttfData: CharArray, sizePixels: Float, fontCfgTemplate: FontConfig? = null,
                             glyphRanges: IntArray = intArrayOf()): Font {

        val fontCfg = fontCfgTemplate ?: FontConfig()
        assert(fontCfg.fontData.isEmpty())
        fontCfg.fontData = ttfData
        fontCfg.fontDataBuffer = byteBufferBig(ttfData.size).apply { ttfData.forEachIndexed { i, c -> this[i] = c.b } }
        fontCfg.sizePixels = sizePixels
        if (glyphRanges.isNotEmpty())
            fontCfg.glyphRanges = glyphRanges
        return addFont(fontCfg)
    }

    /** 'compressed_ttf_data' still owned by caller. Compress with binary_to_compressed_c.cpp   */
    fun addFontFromMemoryCompressedTTF(compressedTtfData: CharArray, sizePixels: Float, fontCfgTemplate: FontConfig? = null,
                                       glyphRanges: IntArray): Font {

        val bufDecompressedData = stb.decompress(compressedTtfData)

        val fontCfg = fontCfgTemplate ?: FontConfig()
        assert(fontCfg.fontData.isEmpty())
        fontCfg.fontDataOwnedByAtlas = true
        return addFontFromMemoryTTF(bufDecompressedData, sizePixels, fontCfg, glyphRanges)
    }

    /** 'compressed_ttf_data_base85' still owned by caller. Compress with binary_to_compressed_c.cpp with -base85
     *  paramaeter  */
    fun addFontFromMemoryCompressedBase85TTF(compressedTtfDataBase85: String, sizePixels: Float, fontCfg: FontConfig? = null,
                                             glyphRanges: IntArray): Font {

        val compressedTtf = decode85(compressedTtfDataBase85)
        return addFontFromMemoryCompressedTTF(compressedTtf, sizePixels, fontCfg, glyphRanges)
    }

    /** Clear the CPU-side texture data. Saves RAM once the texture has been copied to graphics memory. */
    fun clearTexData() {
        texPixelsAlpha8?.destroy()
        texPixelsAlpha8 = null
        texPixelsRGBA32?.destroy()
        texPixelsRGBA32 = null
    }

    /** Clear the input TTF data (inc sizes, glyph ranges)  */
    fun clearInputData() {
        for (cfg in configData)
            if (cfg.fontData.isNotEmpty() && cfg.fontDataOwnedByAtlas)
                cfg.fontData = charArrayOf()

        // When clearing this we lose access to the font name and other information used to build the font.
        for (font in fonts)
            if (font.configData.isNotEmpty()) {
                font.configData.clear()
                font.configDataCount = 0
            }
        configData.clear()
    }

    /** Clear the ImGui-side font data (glyphs storage, UV coordinates) */
    fun clearFonts() {
        for (font in fonts) font.clear()
        fonts.clear()
    }

    /** Clear all   */
    fun clear() {
        clearInputData()
        clearTexData()
        clearFonts()
    }
//
    /* Retrieve texture data
     * User is in charge of copying the pixels into graphics memory, then call SetTextureUserID()
     * After loading the texture into your graphic system, store your texture handle in 'TexID' (ignore if you aren't
     * using multiple fonts nor images)
     * RGBA32 format is provided for convenience and high compatibility, but note that all RGB pixels are white, so 75%
     * of the memory is wasted.
     * Pitch = Width * BytesPerPixels  */
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
            val a = ByteArray(32768, { pixels[it] })
            texPixelsRGBA32 = byteBufferBig(texSize.x * texSize.y * 4)
            val dst = texPixelsRGBA32!!
            for (n in 0 until pixels.size) {
                dst[n * 4] = 255.b
                dst[n * 4 + 1] = 255.b
                dst[n * 4 + 2] = 255.b
                dst[n * 4 + 3] = pixels[n]
            }
        }

        return Triple(texPixelsRGBA32!!, texSize, 4)
    }

    /* Helpers to retrieve list of common Unicode ranges (2 value per range, values are inclusive)
     * NB: Make sure that your string are UTF-8 and NOT in your local code page. In C++11, you can create a UTF-8 string
     * literally using the u8"Hello world" syntax. See FAQ for details. */
    /** Retrieve list of range (2 int per range, values are inclusive), Basic Latin, Extended Latin  */
    val glyphRangesDefault get() = intArrayOf(0x0020, 0x00FF) // Basic Latin + Latin Supplement
//    IMGUI_API const ImWchar*    GetGlyphRangesKorean();     // Default + Korean characters
//    IMGUI_API const ImWchar*    GetGlyphRangesJapanese();   // Default + Hiragana, Katakana, Half-Width, Selection of 1946 Ideographs
//    IMGUI_API const ImWchar*    GetGlyphRangesChinese();    // Japanese + full set of about 21000 CJK Unified Ideographs
//    IMGUI_API const ImWchar*    GetGlyphRangesCyrillic();   // Default + about 400 Cyrillic characters
//    IMGUI_API const ImWchar*    GetGlyphRangesThai();       // Default + Thai characters
//
    // Members
    // (Access texture data via GetTexData*() calls which will setup a default font for you.)
    /** User data to refer to the texture once it has been uploaded to user's graphic systems. It is passed back to you
    during rendering via the ImDrawCmd structure.   */
    var texId = 0
    /** 1 component per pixel, each component is unsigned 8-bit. Total size = TexWidth * TexHeight  */
    var texPixelsAlpha8: ByteBuffer? = null
    /** 4 component per pixel, each component is unsigned 8-bit. Total size = TexWidth * TexHeight * 4  */
    var texPixelsRGBA32: ByteBuffer? = null
    /** Texture size calculated during Build(). */
    var texSize = Vec2i()
    /** Texture width desired by user before Build(). Must be a power-of-two. If have many glyphs your graphics API have
     *  texture size restrictions you may want to increase texture width to decrease height.    */
    var texDesiredWidth = 0
    /** Texture coordinates to a white pixel    */
    var texUvWhitePixel = Vec2()
    /** Hold all the fonts returned by AddFont*. Fonts[0] is the default font upon calling ImGui::NewFrame(), use
     *  ImGui::PushFont()/PopFont() to change the current font. */
    val fonts = ArrayList<Font>()


    // Private ---------------------------------------------------------------------------------------------------------

    private val configData = ArrayList<FontConfig>()

    /** Build pixels data. This is automatically for you by the GetTexData*** functions.    */
    private fun build(): Boolean {

        assert(configData.size > 0)

//        TexID = NULL
//        TexWidth = TexHeight = 0
//        TexUvWhitePixel = ImVec2(0, 0)
        clearTexData()

        class FontTempBuildData {

            var fontInfo = STBTTFontinfo.create()
            lateinit var rects: STBRPRect.Buffer
            lateinit var ranges: STBTTPackRange.Buffer
            var rangesCount = 0
        }

        val tmpArray = Array(configData.size, { FontTempBuildData() })

        // Initialize font information early (so we can error without any cleanup) + count glyphs
        var totalGlyphCount = 0
        var totalGlyphRangeCount = 0

        for (i in configData.indices) {

            val cfg = configData[i]
            val tmp = tmpArray[i]

            assert(wasInit { cfg.dstFont } && (!cfg.dstFont.isLoaded || cfg.dstFont.containerAtlas == this))
            val fontOffset = stbtt_GetFontOffsetForIndex(cfg.fontDataBuffer, cfg.fontNo)
            assert(fontOffset >= 0)
            if (!stbtt_InitFont(tmp.fontInfo, cfg.fontDataBuffer, fontOffset))
                return false

            // Count glyphs
            if (cfg.glyphRanges.isEmpty())
                cfg.glyphRanges = glyphRangesDefault
            for (j in cfg.glyphRanges.indices step 2) {
                totalGlyphCount += (cfg.glyphRanges[j + 1] - cfg.glyphRanges[j]) + 1
                totalGlyphRangeCount++
            }
        }

        /*  Start packing. We need a known width for the skyline algorithm. Using a cheap heuristic here to decide of
            width. User can override TexDesiredWidth if they wish.
            After packing is done, width shouldn't matter much, but some API/GPU have texture size limitations and
            increasing width can decrease height.   */
        texSize.x = when {
            texDesiredWidth > 0 -> texDesiredWidth
            totalGlyphCount > 4000 -> 4096
            totalGlyphCount > 2000 -> 2048
            totalGlyphCount > 1000 -> 1024
            else -> 512
        }
        texSize.y = 0
        val maxTexHeight = 1024 * 32
        val spc = STBTTPackContext.create()
        stbtt_PackBegin(spc, null, texSize.x, maxTexHeight, 0, 1)

        /*  Pack our extra data rectangles first, so it will be on the upper-left corner of our texture (UV will have
            small values).  */
        val extraRects = STBRPRect.calloc(1)
        renderCustomTexData(0, extraRects)
        imgui.stb.stbtt_PackSetOversampling(spc, 1)
        STBRectPack.stbrp_pack_rects(spc.packInfo, extraRects)
        for (i in 0 until extraRects.capacity())
            if (extraRects[i].wasPacked)
                texSize.y = glm.max(texSize.y, extraRects[i].y + extraRects[i].h)

        // Allocate packing character data and flag packed characters buffer as non-packed (x0=y0=x1=y1=0)
        var bufPackedcharsN = 0
        var bufRectsN = 0
        var bufRangesN = 0
        val bufPackedchars = STBTTPackedchar.calloc(totalGlyphCount)
        val bufRects = STBRPRect.calloc(totalGlyphCount)
        val bufRanges = STBTTPackRange.calloc(totalGlyphRangeCount)

        /*  First font pass: pack all glyphs (no rendering at this point, we are working with rectangles in an
            infinitely tall texture at this point)  */
        for (input in configData.indices) {

            val cfg = configData[input]
            val tmp = tmpArray[input]

            // Setup ranges
            var glyphCount = 0
            var glyphRangesCount = 0
            for (j in cfg.glyphRanges.indices step 2) {
                glyphCount += (cfg.glyphRanges[j + 1] - cfg.glyphRanges[j]) + 1
                glyphRangesCount++
            }
            tmp.ranges = STBTTPackRange.create(bufRanges.address() + bufRectsN * glyphRangesCount, glyphRangesCount)
            tmp.rangesCount = glyphRangesCount
            bufRangesN += glyphRangesCount
            for (i in 0 until glyphRangesCount) {
                val range = tmp.ranges[i]
                range.fontSize = cfg.sizePixels
                range.firstUnicodeCodepointInRange = cfg.glyphRanges[input * 2]
                range.numChars = (cfg.glyphRanges[input * 2 + 1] - cfg.glyphRanges[input * 2]) + 1
                range.chardataForRange =
                        STBTTPackedchar.create(bufPackedchars.address() + bufPackedcharsN * STBTTPackedchar.SIZEOF, range.numChars)
                bufPackedcharsN += range.numChars
            }

            // Pack
            tmp.rects = STBRPRect.create(bufRects.address() + bufRectsN * STBRPRect.SIZEOF, glyphCount)
            bufRectsN += glyphCount
            imgui.stb.stbtt_PackSetOversampling(spc, cfg.oversample)
            val n = stbtt_PackFontRangesGatherRects(spc, tmp.fontInfo, tmp.ranges, tmp.rects)
            stbrp_pack_rects(spc.packInfo, tmp.rects)
            // Extend texture height
            for (i in 0 until n)
                if (tmp.rects[i].wasPacked)
                    texSize.y = glm.max(texSize.y, tmp.rects[i].y + tmp.rects[i].h)
        }
        assert(bufRectsN == totalGlyphCount)
        assert(bufPackedcharsN == totalGlyphCount)
        assert(bufRangesN == totalGlyphRangeCount)

        // Create texture
        texSize.y = upperPowerOfTwo(texSize.y)
        texPixelsAlpha8 = byteBufferBig(texSize.x * texSize.y)
        spc.pixels = texPixelsAlpha8!!
        spc.height = texSize.y

        // Second pass: render characters
        for (input in 0 until configData.size) {
            val cfg = configData[input]
            val tmp = tmpArray[input]
            imgui.stb.stbtt_PackSetOversampling(spc, cfg.oversample)
            stbtt_PackFontRangesRenderIntoRects(spc, tmp.fontInfo, tmp.ranges, tmp.rects)
//            tmp.rects = null
        }

        // End packing
        stbtt_PackEnd(spc)
        bufRects.free()

        // Third pass: setup ImFont and glyphs for runtime
        for (input in 0 until configData.size) {

            val cfg = configData[input]
            val tmp = tmpArray[input]
            // We can have multiple input fonts writing into a same destination font (when using MergeMode=true)
            val dstFont = cfg.dstFont

            val fontScale = stbtt_ScaleForPixelHeight(tmp.fontInfo, cfg.sizePixels)
            val (unscaledAscent, unscaledDescent, unscaledLineGap) = imgui.stb.stbtt_GetFontVMetrics(tmp.fontInfo)

            val ascent = unscaledAscent * fontScale
            val descent = unscaledDescent * fontScale
            if (!cfg.mergeMode) {
                dstFont.containerAtlas = this
                dstFont.configData = configData
                dstFont.configDataIdx = input
                dstFont.configDataCount = 0
                dstFont.fontSize = cfg.sizePixels
                dstFont.ascent = ascent
                dstFont.descent = descent
                dstFont.glyphs.clear()
                dstFont.metricsTotalSurface = 0
            }
            dstFont.configDataCount++
            val off = Vec2(cfg.glyphOffset)

            // Always clear fallback so FindGlyph can return NULL. It will be set again in BuildLookupTable()
            dstFont.fallbackGlyph = null
            for (i in 0 until tmp.rangesCount) {
                val range = tmp.ranges[i]
                for (charIdx in 0 until range.numChars) {
                    val pc = range.chardataForRange[charIdx]
                    if (pc.x0 == 0 && pc.x1 == 0 && pc.y0 == 0 && pc.y1 == 0)
                        continue

                    val codepoint = range.firstUnicodeCodepointInRange + charIdx
                    if (cfg.mergeMode && dstFont.findGlyph(codepoint) != null)
                        continue

                    val (_, q) = imgui.stb.stbtt_GetPackedQuad(range.chardataForRange, texSize, charIdx, false)

                    dstFont.glyphs.add(Font.Glyph())
                    val glyph = dstFont.glyphs.last()
                    glyph.codepoint = codepoint.uc
                    glyph.x0 = q.x0 + off.x; glyph.y0 = q.y0 + off.y; glyph.x1 = q.x1 + off.x; glyph.y1 = q.y1 + off.y
                    glyph.u0 = q.s0; glyph.v0 = q.t0; glyph.u1 = q.s1; glyph.v1 = q.t1
                    glyph.y0 += (dstFont.ascent + 0.5f).i.f
                    glyph.y1 += (dstFont.ascent + 0.5f).i.f
                    glyph.xAdvance = pc.xAdvance + cfg.glyphExtraSpacing.x  // Bake spacing into XAdvance
                    if (cfg.pixelSnapH)
                        glyph.xAdvance = (glyph.xAdvance + 0.5f).i.f
                    dstFont.metricsTotalSurface += // +1 to account for average padding, +0.99 to round
                            ((glyph.u1 - glyph.u0) * texSize.x + 1.99f).i * ((glyph.v1 - glyph.v0) * texSize.y + 1.99f).i
                }
            }
            cfg.dstFont.buildLookupTable()
        }

        // Cleanup temporaries
        bufPackedchars.free()
        bufRanges.free()
        val a = ByteArray(512, { texPixelsAlpha8!![it] })
        // Render into our custom data block
        renderCustomTexData(1, extraRects)

        return true
    }

    private fun renderCustomTexData(pass: Int, rects: STBRPRect.Buffer) {
        /*  A work of art lies ahead! (. = white layer, X = black layer, others are blank)
            The white texels on the top left are the ones we'll use everywhere in ImGui to render filled shapes.    */
        val texDataSize = Vec2i(90, 27)
        val textureData = (
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


        if (pass == 0) {
            // Request rectangles
            rects[0].w = texDataSize.x * 2 + 1
            rects[0].h = texDataSize.y + 1
        } else if (pass == 1) {
            // Render/copy pixels
            val r = rects[0]
            var n = 0
            for (y in 0 until texDataSize.y)
                for (x in 0 until texDataSize.x) {
                    val offset0 = r.x + x + (r.y + y) * texSize.x
                    val offset1 = offset0 + 1 + texDataSize.x
                    texPixelsAlpha8!![offset0] = if (textureData[n] == '.') 0xFF.b else 0x00.b
                    texPixelsAlpha8!![offset1] = if (textureData[n] == 'X') 0xFF.b else 0x00.b
                    n++
                }
            val texUvScale = Vec2(1f / texSize.x, 1f / texSize.y)
            texUvWhitePixel = Vec2((r.x + 0.5f) * texUvScale.x, (r.y + 0.5f) * texUvScale.y)

            // Setup mouse cursors
            val cursorDatas = arrayOf(
                    // Pos ........ Size ......... Offset ......
                    arrayOf(Vec2(0, 3), Vec2(12, 19), Vec2(0)), // ImGuiMouseCursor_Arrow
                    arrayOf(Vec2(13, 0), Vec2(7, 16), Vec2(4, 8)), // ImGuiMouseCursor_TextInput
                    arrayOf(Vec2(31, 0), Vec2(23), Vec2(11)), // ImGuiMouseCursor_Move
                    arrayOf(Vec2(21, 0), Vec2(9, 23), Vec2(5, 11)), // ImGuiMouseCursor_ResizeNS
                    arrayOf(Vec2(55, 18), Vec2(23, 9), Vec2(11, 5)), // ImGuiMouseCursor_ResizeEW
                    arrayOf(Vec2(73, 0), Vec2(17), Vec2(9)), // ImGuiMouseCursor_ResizeNESW
                    arrayOf(Vec2(55, 0), Vec2(17), Vec2(9))) // ImGuiMouseCursor_ResizeNWSE

            for (type in 0 until MouseCursor.Count.i) {
                val cursorData = g.mouseCursorData[type]
                val pos = cursorDatas[type][0] + Vec2(r.x, r.y)
                val size = cursorDatas[type][1]
                cursorData.type = MouseCursor.of(type)
                cursorData.size = size
                cursorData.hotOffset = cursorDatas[type][2]
                cursorData.texUvMin[0] = pos * texUvScale
                cursorData.texUvMax[0] = (pos + size) * texUvScale
                pos.x += texDataSize.x + 1
                cursorData.texUvMin[1] = pos * texUvScale
                cursorData.texUvMax[1] = (pos + size) * texUvScale
            }
        }
    }
}

/** Font runtime data and rendering
 *  ImFontAtlas automatically loads a default embedded font for you when you call GetTexDataAsAlpha8() or
 *  GetTexDataAsRGBA32().   */
class Font {

    class Glyph {
        var codepoint = ' '
        var xAdvance = 0f
        var x0 = 0f
        var y0 = 0f
        var x1 = 0f
        var y1 = 0f
        // Texture coordinates
        var u0 = 0f
        var v0 = 0f
        var u1 = 0f
        var v1 = 0f

        infix fun put(other: Glyph) {
            codepoint = other.codepoint
            xAdvance = other.xAdvance
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

    // Members: Hot ~62/78 bytes
    /** <user set>, Height of characters, set during loading (don't change after loading)   */
    var fontSize = 0f
    /** Base font scale, multiplied by the per-window font scale which you can adjust with SetFontScale()   */
    var scale = 1f
    /** Offset font rendering by xx pixels  */
    var displayOffset = Vec2(0f, 1f)
    /** All glyphs. */
    val glyphs = ArrayList<Glyph>()
    /** Sparse. Glyphs->XAdvance in a directly indexable way (more cache-friendly, for CalcTextSize functions which are
    often bottleneck in large UI).  */
    val indexXAdvance = ArrayList<Float>()
    /** Sparse. Index glyphs by Unicode code-point. */
    val indexLookup = ArrayList<Int>()
    /** == FindGlyph(FontFallbackChar)  */
    var fallbackGlyph: Glyph? = null
    /** == FallbackGlyph->XAdvance  */
    var fallbackXAdvance = 0f
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
    /** Total surface in pixels to get an idea of the font rasterization/texture cost (not exact, we approximate the cost of padding
    between glyphs)    */
    var metricsTotalSurface = 0

    //
//    // Methods
//    IMGUI_API ImFont();
//    IMGUI_API ~ImFont();

    fun clear() {
        fontSize = 0f
        displayOffset = Vec2(0.0f, 1.0f)
        glyphs.clear()
        indexXAdvance.clear()
        indexLookup.clear()
        fallbackGlyph = null
        fallbackXAdvance = 0f
        configDataCount = 0
        configData.clear()
//        containerAtlas = NULL TODO check
        ascent = 0f
        descent = 0f
        metricsTotalSurface = 0
    }

    fun buildLookupTable() {

        val maxCodepoint = glyphs.map { it.codepoint.i }.max()!!

        assert(glyphs.size < 0xFFFF) // -1 is reserved
        indexXAdvance.clear()
        indexLookup.clear()
        growIndex(maxCodepoint + 1)
        glyphs.forEachIndexed { i, g ->
            indexXAdvance[g.codepoint.i] = g.xAdvance
            indexLookup[g.codepoint.i] = i
        }

        // Create a glyph to handle TAB
        // FIXME: Needs proper TAB handling but it needs to be contextualized (or we could arbitrary say that each string starts at "column 0" ?)
        if (findGlyph(' ') != null) {
            if (glyphs.last().codepoint != '\t')   // So we can call this function multiple times
                glyphs.add(Glyph())
            val tabGlyph = glyphs.last()
            tabGlyph put findGlyph(' ')!!
            tabGlyph.codepoint = '\t'
            tabGlyph.xAdvance *= 4
            indexXAdvance[tabGlyph.codepoint.i] = tabGlyph.xAdvance
            indexLookup[tabGlyph.codepoint.i] = glyphs.size - 1
        }

        fallbackGlyph = null
        fallbackGlyph = findGlyph(fallbackChar)
        fallbackXAdvance = fallbackGlyph?.xAdvance ?: 0f
        for (i in 0 until maxCodepoint + 1)
            if (indexXAdvance[i] < 0f)
                indexXAdvance[i] = fallbackXAdvance
    }

    fun findGlyph(c: Char) = findGlyph(c.i)
    fun findGlyph(c: Int): Glyph? {
        if (c < indexLookup.size) {
            val i = indexLookup[c]
            if (i != -1) return glyphs[i]
        }
        return fallbackGlyph
    }

    //    IMGUI_API void              SetFallbackChar(ImWchar c);

    fun getCharAdvance_A(c: Char) =
            if (c < indexXAdvance.size)
                indexXAdvance[c.i]
            else
                fallbackXAdvance

    val isLoaded get() = wasInit { containerAtlas }


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
            if (c < 0x80)
                s += 1
            else {
                TODO()
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

            val charWidth = (if (c < indexXAdvance.size) indexXAdvance[c.i] else fallbackXAdvance) * scale
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
            if (c == '\u0000') break

            if (c < 32) {
                if (c == '\n') {
                    lineWidth = 0f
                    wordWidth = 0f
                    blankWidth = 0f
                    insideWord = true
                    s = nextS.i
                    continue
                }
                if (c == '\r') {
                    s = nextS.i
                    continue
                }
            }
            val charWidth = indexXAdvance.getOrElse(c.i, { fallbackXAdvance }) * scale
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
                    wordEnd = nextS.i
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
            s = nextS.i
        }
        return s
    }

    fun renderChar(drawList:DrawList, size:Float, pos:Vec2, col:Int, c:Char) {
        if (c == ' ' || c == '\t' || c == '\n' || c == '\r') // Match behavior of RenderText(), those 4 codepoints are hard-coded.
            return
        findGlyph(c)?.let {
            val scale = if(size >= 0f) size / fontSize else 1f
            val x = pos.x.i.f + displayOffset.x
            val y = pos.y.i.f + displayOffset.y
            val posTl = Vec2(x + it.x0 * scale, y + it.y0 * scale)
            val posBr = Vec2(x + it.x1 * scale, y + it.y1 * scale)
            drawList.primReserve(6, 4)
            drawList.primRectUV(posTl, posBr, Vec2(it.u0, it.v0), Vec2(it.u1, it.v1), col)
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
            if (c < 0x80) s += 1
            else {
                TODO()
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
                charWidth = glyph.xAdvance * scale

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

    // Private
    private fun growIndex(newSize: Int) {
        assert(indexXAdvance.size == indexLookup.size)
        val oldSize = indexLookup.size
        if (newSize <= oldSize)
            return
        for (i in oldSize until newSize) {
            indexXAdvance.add(-1.0f)
            indexLookup.add(-1)
        }
    }
//    IMGUI_API void              AddRemapChar(ImWchar dst, ImWchar src, bool overwrite_dst = true); // Makes 'dst' character/glyph points to 'src' character/glyph. Currently needs to be called AFTER fonts have been built.

    fun setCurrent() {
        assert(isLoaded)    // Font Atlas not created. Did you call io.Fonts->GetTexDataAsRGBA32 / GetTexDataAsAlpha8 ?
        assert(scale > 0.0f)
        g.font = this
        g.fontBaseSize = IO.fontGlobalScale * g.font.fontSize * g.font.scale
        g.fontSize = g.currentWindow?.calcFontSize() ?: 0f
        g.fontTexUvWhitePixel = g.font.containerAtlas.texUvWhitePixel
    }
}

val proggyCleanTtfCompressedDataBase85 by lazy {
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