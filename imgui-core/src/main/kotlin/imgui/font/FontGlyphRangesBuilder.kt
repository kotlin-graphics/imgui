package imgui.font

import gli_.has
import imgui.UNICODE_CODEPOINT_MAX
import kool.BYTES

/** Helper to build glyph ranges from text/string data. Feed your application strings/characters to it then call BuildRanges().
 *  This is essentially a tightly packed of vector of 64k booleans = 8KB storage. */
class FontGlyphRangesBuilder {
    /** Store 1-bit per Unicode code point (0=unused, 1=used) */
    lateinit var usedChars: IntArray

    init {
        clear()
    }

    fun clear() {
        val sizeInBytes = (UNICODE_CODEPOINT_MAX + 1) / 8
        usedChars = IntArray(sizeInBytes / Int.BYTES)
    }

    /** Get bit n in the array */
    fun getBit(n: Int): Boolean {
        val off = n shr 5
        val mask = 1 shl (n and 31)
        return usedChars[off] has mask
    }

    /** Set bit n in the array */
    fun setBit(n: Int) {
        val off = n shr 5
        val mask = 1 shl (n and 31)
        usedChars[off] = usedChars[off] or mask
    }

    /** Add character */
    fun addChar(c: Int) = setBit(c)
    /** Add string (each character of the UTF-8 string are added) */
//    fun addText(const char* text, const char* text_end)
//    {
//        while (text_end ? (text < text_end) : *text)
//        {
//            unsigned int c = 0;
//            int c_len = ImTextCharFromUtf8(&c, text, text_end);
//            text += c_len;
//            if (c_len == 0)
//                break;
//            if (c <= IM_UNICODE_CODEPOINT_MAX)
//                AddChar((ImWchar)c);
//        }
//    }

    /** Add ranges, e.g. builder.AddRanges(ImFontAtlas::GetGlyphRangesDefault()) to force add all of ASCII/Latin+Ext */
    fun addRanges(ranges: Array<IntRange>) {
        for (range in ranges)
            for (c in range)
                addChar(c)
    }

    /** Output new ranges */
    fun buildRanges(): Array<IntRange> {
        val maxCodepoint = UNICODE_CODEPOINT_MAX
        val outRanges = ArrayList<Int>()
        var n = 0
        while (n <= maxCodepoint) {
            if (getBit(n)) {
                outRanges += n
                while (n < maxCodepoint && getBit(n + 1))
                    n++
                outRanges += n
            }
            n++
        }
        val size = outRanges.size / 2
        return Array(size) { IntRange(outRanges[it * 2], outRanges[it * 2 + 1]) }
    }
}