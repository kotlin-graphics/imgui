@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalStdlibApi::class)

package imgui.stb_

import glm_.f
import glm_.has
import glm_.i
import imgui.stb_.TrueType.getGlyfOffset
import imgui.stb_.TrueType.short
import imgui.stb_.TrueType.ushort
import kotlin.math.sqrt

//////////////////////////////////////////////////////////////////////////////
//
// GLYPH SHAPES (you probably don't need these, but they have to go before
// the bitmaps for C declaration-order reasons)
//

// (we share this with other code at RAD)
class Vertex {
    var x = 0
    var y = 0
    var cX = 0
    var cY = 0
    var cX1 = 0
    var cY1 = 0
    var type = 0.ub
    //,padding;

    fun set(type: Type, x: Int, y: Int, cX: Int, cY: Int) {
        this.type = type.ordinal.ub
        this.x = x
        this.y = y
        this.cX = cX
        this.cY = cY
    }

    enum class Type {
        none, move, line, curve, cubic;

        companion object {
            infix fun of(ubyte: UByte) = values()[ubyte.i]
        }
    }

    override fun toString() = "x=$x y=$y cx=$cX cy=$cY cx1=$cX1 cy1=$cY1 type=$type"
}

// returns non-zero if nothing is drawn for this glyph
//    STBTT_DEF int stbtt_IsGlyphEmpty(const stbtt_fontinfo *info, int glyph_index);


//    STBTT_DEF int stbtt_GetCodepointShape(const stbtt_fontinfo *info, int unicode_codepoint, stbtt_vertex **vertices);

// returns # of vertices and fills *vertices with the pointer to them
//   these are expressed in "unscaled" coordinates
//
// The shape is a series of contours. Each one starts with
// a STBTT_moveto, then consists of a series of mixed
// STBTT_lineto and STBTT_curveto segments. A lineto
// draws a line from previous endpoint to its x,y; a curveto
// draws a quadratic bezier from previous endpoint to
// its x,y, using cx,cy as the bezier control point.
infix fun FontInfo.getGlyphShape(glyphIndex: Int): Pair<Array<Vertex>, Int> = when {
    cff.isEmpty() -> getGlyphShapeTT(glyphIndex)
    else -> getGlyphShapeT2(glyphIndex)
}

fun FontInfo.getGlyphShapeTT(glyphIndex: Int): Pair<Array<Vertex>, Int> {
    lateinit var vertices: Array<Vertex>
    var numVertices = 0
    val g = getGlyfOffset(glyphIndex)

    if (g < 0) return emptyArray<Vertex>() to 0

    val numberOfContours = data.short(g)

    when {
        numberOfContours > 0 -> {
            var flags = 0.ub
            var j = 0
            var wasOff = false
            var startOff = false
            val endPtsOfContours = g + 10
            val ins = data.short(g + 10 + numberOfContours * 2)
            var points = g + 10 + numberOfContours * 2 + 2 + ins

            val n = 1 + data.short(endPtsOfContours + numberOfContours * 2 - 2)

            val m = n + 2 * numberOfContours  // a loose bound on how many vertices we might need
            vertices = Array(m) { Vertex() }

            var nextMove = 0
            var flagCount = 0.ub

            // in first pass, we load uninterpreted data into the allocated array
            // above, shifted to the end of the array so we won't overwrite it when
            // we create our final data starting from the front

            val off = m - n // starting offset for uninterpreted data, regardless of how m ends up being calculated

            // first load flags

            for (i in 0 until n) {
                if (flagCount == 0.ub) {
                    flags = data[points++]
                    if (flags has 8)
                        flagCount = data[points++]
                } else
                    --flagCount
                vertices[off + i].type = flags
            }

            // now load x coordinates
            var x = 0
            for (i in 0 until n) {
                flags = vertices[off + i].type
                if (flags has 2) {
                    val dx = data[points++].i
                    x += if (flags has 16) dx else -dx // ???
                } else if (flags hasnt 16) {
                    x += data.short(points)
                    points += 2
                }
                vertices[off + i].x = x
            }

            // now load y coordinates
            var y = 0
            for (i in 0 until n) {
                flags = vertices[off + i].type
                if (flags has 4) {
                    val dy = data[points++].i
                    y += if (flags has 32) dy else -dy // ???
                } else if (flags hasnt 32) {
                    y += data.short(points)
                    points += 2
                }
                vertices[off + i].y = y
            }

            // now convert them to our format
            numVertices = 0
            var cx = 0
            var cy = 0
            var sx = 0
            var sy = 0
            var scx = 0
            var scy = 0
            var i = 0
            while (i < n) {
                flags = vertices[off + i].type
                x = vertices[off + i].x
                y = vertices[off + i].y

                if (nextMove == i) {
                    if (i != 0)
                        numVertices = TrueType.closeShape(vertices, numVertices, wasOff, startOff, sx, sy, scx, scy, cx, cy)

                    // now start the new one
                    startOff = flags hasnt 1
                    if (startOff) {
                        // if we start off with an off-curve point, then when we need to find a point on the curve
                        // where we can start, and we need to save some state for when we wraparound.
                        scx = x
                        scy = y
                        if (vertices[off + i + 1].type hasnt 1) {
                            // next point is also a curve point, so interpolate an on-point curve
                            sx = (x + vertices[off + i + 1].x) shr 1
                            sy = (y + vertices[off + i + 1].y) shr 1
                        } else {
                            // otherwise just use the next point as our start point
                            sx = vertices[off + i + 1].x
                            sy = vertices[off + i + 1].y
                            ++i // we're using point i+1 as the starting point, so skip it
                        }
                    } else {
                        sx = x
                        sy = y
                    }
                    vertices[numVertices++].set(Vertex.Type.move, sx, sy, 0, 0)
                    wasOff = false
                    nextMove = 1 + data.ushort(endPtsOfContours + j * 2).i
                    ++j
                } else {
                    if (flags hasnt 1) { // if it's a curve
                        if (wasOff) // two off-curve control points in a row means interpolate an on-curve midpoint
                            vertices[numVertices++].set(Vertex.Type.curve, (cx + x) shr 1, (cy + y) shr 1, cx, cy)
                        cx = x
                        cy = y
                        wasOff = true
                    } else {
                        if (wasOff)
                            vertices[numVertices++].set(Vertex.Type.curve, x, y, cx, cy)
                        else
                            vertices[numVertices++].set(Vertex.Type.line, x, y, 0, 0)
                        wasOff = false
                    }
                }
                i++
            }
            numVertices = TrueType.closeShape(vertices, numVertices, wasOff, startOff, sx, sy, scx, scy, cx, cy)
        }

        numberOfContours < 0 -> {
            // Compound shapes.
            var more = true
            var comp = g + 10
            numVertices = 0
            while (more) {
                val mtx = floatArrayOf(1f, 0f, 0f, 1f, 0f, 0f)

                val flags = data.short(comp); comp += 2
                val gIdx = data.short(comp); comp += 2

                if (flags has 2) { // XY values
                    if (flags has 1) { // shorts
                        mtx[4] = data.short(comp).f; comp += 2
                        mtx[5] = data.short(comp).f; comp += 2
                    } else {
                        mtx[4] = data[comp++].f
                        mtx[5] = data[comp++].f
                    }
                } else TODO("handle matching point")
                when {
                    flags has (1 shl 3) -> { // WE_HAVE_A_SCALE
                        mtx[0] = data.short(comp) / 16384f; comp += 2; mtx[3] = mtx[0]
                        mtx[1] = 0f; mtx[2] = 0f
                    }

                    flags has (1 shl 6) -> { // WE_HAVE_AN_X_AND_YSCALE
                        mtx[0] = data.short(comp) / 16384f; comp += 2
                        mtx[1] = 0f; mtx[2] = 0f
                        mtx[3] = data.short(comp) / 16384f; comp += 2
                    }

                    flags has (1 shl 7) -> { // WE_HAVE_A_TWO_BY_TWO
                        mtx[0] = data.short(comp) / 16384f; comp += 2
                        mtx[1] = data.short(comp) / 16384f; comp += 2
                        mtx[2] = data.short(comp) / 16384f; comp += 2
                        mtx[3] = data.short(comp) / 16384f; comp += 2
                    }
                }

                // Find transformation scales.
                val m = sqrt(mtx[0] * mtx[0] + mtx[1] * mtx[1])
                val n = sqrt(mtx[2] * mtx[2] + mtx[3] * mtx[3])

                // Get indexed glyph.
                val (compVerts, compNumVerts) = getGlyphShape(gIdx)
                if (compNumVerts > 0) {
                    // Transform vertices.
                    for (i in 0..<compNumVerts) {
                        val v = compVerts[i]
                        var x = v.x
                        var y = v.y
                        v.x = (m * (mtx[0] * x + mtx[2] * y + mtx[4])).i
                        v.y = (n * (mtx[1] * x + mtx[3] * y + mtx[5])).i
                        x = v.cX; y = v.cY
                        v.cX = (m * (mtx[0] * x + mtx[2] * y + mtx[4])).i
                        v.cY = (n * (mtx[1] * x + mtx[3] * y + mtx[5])).i
                    }
                    // Append vertices.
                    val tmp = Array(numVertices + compNumVerts) {
                        if (it < numVertices) vertices[it]
                        else compVerts[it - numVertices]
                    }
                    vertices = tmp
                    numVertices += compNumVerts
                }
                // More components ?
                more = flags has (1 shl 5)
            }
        }

        else -> {} // numberOfCounters == 0, do nothing
    }

    return vertices to numVertices
}


fun getGlyphShapeT2(glyphIndex: Int): Pair<Array<Vertex>, Int> {
    TODO()
//        // runs the charstring twice, once to count and once to output (to avoid realloc)
//        val countCtx = Csctx(true)
//        val outputCtx = Csctx(false)
//        if (runCharString(glyphIndex, countCtx)) {
//            outputCtx.vertices = Array(countCtx.numVertices) { TrueType.Vertex() }
//            if (runCharString(info, glyphIndex, outputCtx)) {
//                assert(outputCtx.numVertices == countCtx.numVertices)
//                return outputCtx.vertices
//            }
//        }
//        return emptyArray()
}

// frees the data allocated above
//    STBTT_DEF void stbtt_FreeShape(const stbtt_fontinfo *info, stbtt_vertex *vertices);

//    STBTT_DEF stbtt_uint8 *stbtt_FindSVGDoc(const stbtt_fontinfo *info, int gl)
//    {
//        int i
//                stbtt_uint8 * data = info->data
//        stbtt_uint8 * svg_doc_list = data + stbtt__get_svg((stbtt_fontinfo *) info)
//
//        int numEntries = ttUSHORT (svg_doc_list)
//        stbtt_uint8 * svg_docs = svg_doc_list + 2
//
//        for (i= 0; i < numEntries; i++) {
//        stbtt_uint8 * svg_doc = svg_docs + (12 * i)
//        if ((gl >= ttUSHORT(svg_doc)) && (gl <= ttUSHORT(svg_doc + 2)))
//            return svg_doc
//    }
//        return 0
//    }
//
//    STBTT_DEF int stbtt_GetCodepointSVG(const stbtt_fontinfo *info, int unicode_codepoint, const char **svg)
//    {
//        return stbtt_GetGlyphSVG(info, stbtt_FindGlyphIndex(info, unicode_codepoint), svg)
//    }
//
// fills svg with the character's SVG data.
// returns data size or 0 if SVG not found.
//    STBTT_DEF int stbtt_GetGlyphSVG(const stbtt_fontinfo *info, int gl, const char **svg)
//    {
//        stbtt_uint8 * data = info->data
//        stbtt_uint8 * svg_doc
//
//        if (info->svg == 0)
//        return 0
//
//        svg_doc = stbtt_FindSVGDoc(info, gl)
//        if (svg_doc != NULL) {
//            *svg = (char *) data +info->svg+ttULONG(svg_doc+4)
//            return ttULONG(svg_doc + 8)
//        } else {
//            return 0
//        }
//    }
