package imgui.font


import glm_.*
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.ImGui.io
import imgui.NUL
import imgui.api.g
import imgui.classes.DrawList
import imgui.internal.DrawVert
import imgui.internal.isBlankA
import imgui.internal.isBlankW
import imgui.internal.round
import imgui.resize
import kool.*
import org.lwjgl.system.Platform
import kotlin.math.floor
import uno.kotlin.plusAssign


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
    /** Replacement character if a glyph isn't found. Only set via SetFallbackChar()    */
    var fallbackChar = '?'                      // 2     // in  // = '?'
        /** ~SetFallbackChar */
        set(value) {
            field = value
            buildLookupTable()
        }
    /** Override a codepoint used for ellipsis rendering. */
    var ellipsisChar = '\uffff'                 // out //
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

        val textEnd = if (textEnd_ == -1) text.length else textEnd_

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
            if (lineWidth + wordWidth > wrapWidth) {
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
            val x = floor(pos.x) + displayOffset.x
            val y = floor(pos.y) + displayOffset.y
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
        pos.x = floor(pos.x) + displayOffset.x
        pos.y = floor(pos.y) + displayOffset.y
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

                        drawList.apply {
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

        // Give back unused vertices (clipped ones, blanks) ~ this is essentially a PrimUnreserve() action.
        drawList.vtxBuffer.lim = vtxWrite    // Same as calling shrink()
        drawList.idxBuffer.lim = idxWrite
        drawList.cmdBuffer.last().elemCount -= (idxExpectedSize - drawList.idxBuffer.lim)
        drawList._vtxWritePtr = vtxWrite
        drawList._idxWritePtr = idxWrite
        drawList._vtxCurrentIdx = vtxCurrentIdx
    }

    fun CharArray.memchr(startIdx: Int, c: Char): Int? {
        for (index in startIdx until size)
            if (c == this[index])
                return index
        return null
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
            glyph.advanceX = round(glyph.advanceX)
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

    companion object {

        internal fun Char.remapCodepointIfProblematic(): Int {
            val i = toInt()
            return when (Platform.get()) {
                /*  https://en.wikipedia.org/wiki/Windows-1252#Character_set
                 *  manually remap the difference from  ISO-8859-1 */
                Platform.WINDOWS -> when (i) {
                    // 8_128
                    0x20AC -> 128 // €
                    0x201A -> 130 // ‚
                    0x0192 -> 131 // ƒ
                    0x201E -> 132 // „
                    0x2026 -> 133 // …
                    0x2020 -> 134 // †
                    0x2021 -> 135 // ‡
                    0x02C6 -> 136 // ˆ
                    0x2030 -> 137 // ‰
                    0x0160 -> 138 // Š
                    0x2039 -> 139 // ‹
                    0x0152 -> 140 // Œ
                    0x017D -> 142 // Ž
                    // 9_144
                    0x2018 -> 145 // ‘
                    0x2019 -> 146 // ’
                    0x201C -> 147 // “
                    0x201D -> 148 // ”
                    0x2022 -> 149 // •
                    0x2013 -> 150 // –
                    0x2014 -> 151 // —
                    0x02DC -> 152 // ˜
                    0x2122 -> 153 // ™
                    0x0161 -> 154 // š
                    0x203A -> 155 // ›
                    0x0153 -> 156 // œ
                    0x017E -> 158 // ž
                    0x0178 -> 159 // Ÿ
                    else -> i
                }
                else -> i // TODO
            }
        }
    }
}