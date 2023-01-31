package imgui.font

import imgui.UNICODE_CODEPOINT_MAX
import imgui.internal.textCharFromUtf8
import uno.kotlin.NUL

class FontGlyphRangesBuilder {

    /** Store 1-bit per Unicode code point (0=unused, 1=used) */
    val usedChars = ArrayList<UInt>()

    init {
        clear()
    }

    fun clear() {
        val sizeInBytes = (UNICODE_CODEPOINT_MAX + 1) / 8
        usedChars.clear()
        for (i in 0 until sizeInBytes / UInt.SIZE_BYTES)
            usedChars += 0u
    }

    /** Get bit n in the array */
    fun getBit(n: Int): Boolean {
        val off = n shr 5
        val mask = 1u shl (n and 31)
        return (usedChars[off] and mask) != 0u
    }

    /** Set bit n in the array */
    fun setBit(n: Int) {
        val off = n shr 5
        val mask = 1u shl (n and 31)
        usedChars[off] = usedChars[off] or mask
    }

    /** Add character */
    fun addChar(c: Char) = setBit(c.code)

    /** Add string (each character of the UTF-8 string are added) */
    fun addText(text: ByteArray, textEnd: Int? = null) {
        var p = 0
        while (p < (textEnd ?: text.size)) {
            val (c, cLen) = textCharFromUtf8(text, p, textEnd ?: 0)
            p += cLen
            if (cLen == 0)
                break
            addChar(Char(c))
        }
    }

    /** Add ranges, e.g. builder.AddRanges(ImFontAtlas::GetGlyphRangesDefault()) to force add all of ASCII/Latin+Ext */
    fun addRanges(ranges: ArrayList<Char>) {
        for (i in ranges.indices step 2) {
            var c = ranges[i]
            while (c <= ranges[i + 1] && c.code <= UNICODE_CODEPOINT_MAX) //-V560
                addChar(c++)
        }
    }

    /** Output new ranges */
    fun buildRanges(outRanges: ArrayList<Char>) {
        val maxCodepoint = UNICODE_CODEPOINT_MAX
        var n = 0
        while (n <= maxCodepoint) {
            if (getBit(n)) {
                outRanges += Char(n)
                while (n < maxCodepoint && getBit(n + 1))
                    n++
                outRanges += Char(n)
            }
            n++
        }
        outRanges += NUL
    }
}