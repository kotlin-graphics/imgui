@file:OptIn(ExperimentalUnsignedTypes::class)

package imgui.stb_

import glm_.f
import glm_.i
import glm_.s
import glm_.vec3.Vec3i
import glm_.vec4.Vec4i
import imgui.stb_.TrueType.getGlyfOffset
import imgui.stb_.TrueType.getGlyphInfoT2
import imgui.stb_.TrueType.short
import imgui.stb_.TrueType.ulong
import imgui.stb_.TrueType.ushort

//////////////////////////////////////////////////////////////////////////////
//
// CHARACTER TO GLYPH-INDEX CONVERSIOn


// If you're going to perform multiple operations on the same character
// and you want a speed-up, call this function with the character you're
// going to process, then use glyph-based functions instead of the
// codepoint-based functions.
// Returns 0 if the character codepoint is not defined in the font.
infix fun FontInfo.findGlyphIndex(unicodeCodepoint: Int): Int {
    return when (val format = data.ushort(indexMap + 0).i) {
        0 -> { // apple byte encoding
            val bytes = data.ushort(indexMap + 2).i
            if (unicodeCodepoint < bytes - 6)
                return data[indexMap + 6 + unicodeCodepoint].i
            0
        }

        6 -> {
            val first = data.ushort(indexMap + 6)
            val count = data.ushort(indexMap + 8)
            if (unicodeCodepoint.ui in first until first + count)
                return data.ushort(indexMap + 10 + (unicodeCodepoint - first.i) * 2).i
            0
        }

        2 -> TODO("high-byte mapping for japanese/chinese/korean")
        4 -> { // standard mapping for windows fonts: binary search collection of ranges
            val segCount = data.ushort(indexMap + 6) shr 1
            var searchRange = data.ushort(indexMap + 8) shr 1
            var entrySelector = data.ushort(indexMap + 10)
            val rangeShift = data.ushort(indexMap + 12) shr 1

            // do a binary search of the segments
            val endCount = indexMap.ui + 14u
            var search = endCount

            if (unicodeCodepoint > 0xffff)
                return 0

            // they lie from endCount .. endCount + segCount
            // but searchRange is the nearest power of two, so...
            if (unicodeCodepoint >= data.ushort(search.i + rangeShift.i * 2).i)
                search += rangeShift * 2u

            // now decrement to bias correctly to find smallest
            search -= 2u
            while (entrySelector != 0u) {
                searchRange = searchRange shr 1
                val end = data.ushort(search.i + searchRange.i * 2)
                if (unicodeCodepoint > end.i)
                    search += searchRange * 2u
                --entrySelector
            }
            search += 2u

            run {
                val item = ((search - endCount) shr 1).us

                val start = data.ushort(indexMap + 14 + segCount.i * 2 + 2 + 2 * item.i)
                val last = data.ushort(endCount.i + 2 * item.i)
                if (unicodeCodepoint < start.i || unicodeCodepoint > last.i)
                    return 0

                val offset = data.ushort(indexMap + 14 + segCount.i * 6 + 2 + 2 * item.i)
                if (offset == 0u)
                    return (unicodeCodepoint + data.ushort(indexMap + 14 + segCount.i * 4 + 2 + 2 * item.i).i).s.i

                data.ushort(offset.i + (unicodeCodepoint - start.i) * 2 + indexMap + 14 + segCount.i * 6 + 2 + 2 * item.i).i
            }
        }

        12, 13 -> {
            val nGroups = data.ulong(indexMap + 12)
            var low = 0
            var high = nGroups.i
                    // Binary search the right group.
                    while (low < high) {
                        val mid = low +((high - low) shr 1) // rounds down, so low <= mid < high
                        val startChar = data.ulong(indexMap + 16 + mid * 12)
                        val endChar = data.ulong(indexMap + 16 + mid * 12 + 4)
                        if (unicodeCodepoint.ui < startChar)
                            high = mid
                        else if (unicodeCodepoint.ui > endChar)
                            low = mid + 1
                        else {
                            val startGlyph = data.ulong(indexMap + 16 + mid * 12 + 8)
                            return startGlyph.i + when (format) {
                                12 -> unicodeCodepoint - startChar.i
                                // format == 13
                                else -> 0
                            }
                        }
                    }
            0 // not found
        }

        else -> error("")
    }
}

//////////////////////////////////////////////////////////////////////////////
//
// CHARACTER PROPERTIES
//


// computes a scale factor to produce a font whose "height" is 'pixels' tall.
// Height is measured as the distance from the highest ascender to the lowest
// descender; in other words, it's equivalent to calling stbtt_GetFontVMetrics
// and computing:
//       scale = pixels / (ascent - descent)
// so if you prefer to measure height by the ascent only, use a similar calculation.
infix fun FontInfo.scaleForPixelHeight(height: Float): Float {
    val fheight = data.short(hhea + 4) - data.short(hhea + 6)
    return height.f / fheight
}

// computes a scale factor to produce a font whose EM size is mapped to
// 'pixels' tall. This is probably what traditional APIs compute, but
// I'm not positive.
infix fun FontInfo.scaleForMappingEmToPixels(pixels: Float): Float {
    val unitsPerEm = data.ushort(head + 18).i
    return pixels.f / unitsPerEm
}

// ascent is the coordinate above the baseline the font extends; descent
// is the coordinate below the baseline the font extends (i.e. it is typically negative)
// lineGap is the spacing between one row's descent and the next row's ascent...
// so you should advance the vertical position by "*ascent - *descent + *lineGap"
//   these are expressed in unscaled coordinates, so you must multiply by
//   the scale factor for a given size
// [JVM] @returns [ascent, descent, lineGap]
val FontInfo.fontVMetrics get() = Vec3i { data.short(hhea + 4 + 2 * it) }

// analogous to GetFontVMetrics, but returns the "typographic" values from the OS/2
// table (specific to MS/Windows TTF files).
//
// Returns 1 on success (table present), 0 on failure.
//    STBTT_DEF int  stbtt_GetFontVMetricsOS2(const stbtt_fontinfo *info, int *typoAscent, int *typoDescent, int *typoLineGap);

// the bounding box around all possible characters
//    STBTT_DEF void stbtt_GetFontBoundingBox(const stbtt_fontinfo *info, int *x0, int *y0, int *x1, int *y1);

// leftSideBearing is the offset from the current horizontal position to the left edge of the character
// advanceWidth is the offset from the current horizontal position to the next horizontal position
//   these are expressed in unscaled coordinates
//    STBTT_DEF void stbtt_GetCodepointHMetrics(const stbtt_fontinfo *info, int codepoint, int *advanceWidth, int *leftSideBearing);

// an additional amount to add to the 'advance' value between ch1 and ch2
//    STBTT_DEF int  stbtt_GetCodepointKernAdvance(const stbtt_fontinfo *info, int ch1, int ch2);

// Gets the bounding box of the visible part of the glyph, in unscaled coordinates
//    STBTT_DEF int stbtt_GetCodepointBox(const stbtt_fontinfo *info, int codepoint, int *x0, int *y0, int *x1, int *y1);

// as above, but takes one or more glyph indices for greater efficiency
// [JVM] @return [advanceWidth, leftSideBearing]
infix fun FontInfo.getGlyphHMetrics(glyphIndex: Int): Pair<Int, Int> {
    val numOfLongHorMetrics = data.ushort(hhea + 34).i
    return when {
        glyphIndex < numOfLongHorMetrics -> data.short(hmtx + 4 * glyphIndex) to data.short(hmtx + 4 * glyphIndex + 2)
        else -> data.short(hmtx + 4 * (numOfLongHorMetrics - 1)) to data.short(hmtx + 4 * numOfLongHorMetrics + 2 * (glyphIndex - numOfLongHorMetrics))
    }
}
//    STBTT_DEF int  stbtt_GetGlyphKernAdvance(const stbtt_fontinfo *info, int glyph1, int glyph2);

// as above, but takes one or more glyph indices for greater efficiency
fun FontInfo.getGlyphBox(glyphIndex: Int, box: Vec4i): Boolean {
    if (cff.isNotEmpty())
        getGlyphInfoT2(glyphIndex, box)
    else {
        val g = getGlyfOffset(glyphIndex)
        if (g < 0) return false

        box.put(data.short(g + 2),
                data.short(g + 4),
                data.short(g + 6),
                data.short(g + 8))
    }
    return true
}

//    STBTT_DEF int  stbtt_GetKerningTableLength(const stbtt_fontinfo *info);

// Retrieves a complete list of all of the kerning pairs provided by the font
// stbtt_GetKerningTable never writes more than table_length entries and returns how many entries it did write.
// The table will be sorted by (a.glyph1 == b.glyph1)?(a.glyph2 < b.glyph2):(a.glyph1 < b.glyph1)
//    STBTT_DEF int  stbtt_GetKerningTable(const stbtt_fontinfo *info, stbtt_kerningentry* table, int table_length);
