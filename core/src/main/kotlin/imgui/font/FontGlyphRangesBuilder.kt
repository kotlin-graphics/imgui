package imgui.font

import imgui.UNICODE_CODEPOINT_MAX

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

//    IMGUI_API void  AddText(const char* text, const char* text_end = NULL);     // Add string (each character of the UTF-8 string are added)
//    IMGUI_API void  AddRanges(const ImWchar* ranges);                           // Add ranges, e.g. builder.AddRanges(ImFontAtlas::GetGlyphRangesDefault()) to force add all of ASCII/Latin+Ext
//    IMGUI_API void  BuildRanges(ImVector<ImWchar>* out_ranges);                 // Output new ranges
}