package imgui.stb_

import glm_.b
import glm_.f
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4i
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

//////////////////////////////////////////////////////////////////////////////
//
// BITMAP RENDERING
//


// frees the bitmap allocated below
//STBTT_DEF void stbtt_FreeBitmap(unsigned char *bitmap, void *userdata);

// allocates a large-enough single-channel 8bpp bitmap and renders the
// specified character/glyph at the specified scale into it, with
// antialiasing. 0 is no coverage (transparent), 255 is fully covered (opaque).
// *width & *height are filled out with the width & height of the bitmap,
// which is stored left-to-right, top-to-bottom.
//
// xoff/yoff are the offset it pixel space from the glyph origin to the top-left of the bitmap
//STBTT_DEF unsigned char *stbtt_GetCodepointBitmap(const stbtt_fontinfo *info, float scale_x, float scale_y, int codepoint, int *width, int *height, int *xoff, int *yoff);

// the same as stbtt_GetCodepoitnBitmap, but you can specify a subpixel
// shift for the character
//STBTT_DEF unsigned char *stbtt_GetCodepointBitmapSubpixel(const stbtt_fontinfo *info, float scale_x, float scale_y, float shift_x, float shift_y, int codepoint, int *width, int *height, int *xoff, int *yoff);

// the same as stbtt_GetCodepointBitmap, but you pass in storage for the bitmap
// in the form of 'output', with row spacing of 'out_stride' bytes. the bitmap
// is clipped to out_w/out_h bytes. Call stbtt_GetCodepointBitmapBox to get the
// width and height and positioning info for it first.
//STBTT_DEF void stbtt_MakeCodepointBitmap(const stbtt_fontinfo *info, unsigned char *output, int out_w, int out_h, int out_stride, float scale_x, float scale_y, int codepoint);

// same as stbtt_MakeCodepointBitmap, but you can specify a subpixel
// shift for the character
//STBTT_DEF void stbtt_MakeCodepointBitmapSubpixel(const stbtt_fontinfo *info, unsigned char *output, int out_w, int out_h, int out_stride, float scale_x, float scale_y, float shift_x, float shift_y, int codepoint);

// same as stbtt_MakeCodepointBitmapSubpixel, but prefiltering
// is performed (see stbtt_PackSetOversampling)
//STBTT_DEF void stbtt_MakeCodepointBitmapSubpixelPrefilter(const stbtt_fontinfo *info, unsigned char *output, int out_w, int out_h, int out_stride, float scale_x, float scale_y, float shift_x, float shift_y, int oversample_x, int oversample_y, float *sub_x, float *sub_y, int codepoint);

// get the bbox of the bitmap centered around the glyph origin; so the
// bitmap width is ix1-ix0, height is iy1-iy0, and location to place
// the bitmap top left is (leftSideBearing*scale,iy0).
// (Note that the bitmap uses y-increases-down, but the shape uses
// y-increases-up, so CodepointBitmapBox and CodepointBox are inverted.)
//STBTT_DEF void stbtt_GetCodepointBitmapBox(const stbtt_fontinfo *font, int codepoint, float scale_x, float scale_y, int *ix0, int *iy0, int *ix1, int *iy1);

// same as stbtt_GetCodepointBitmapBox, but you can specify a subpixel
// shift for the character
//STBTT_DEF void stbtt_GetCodepointBitmapBoxSubpixel(const stbtt_fontinfo *font, int codepoint, float scale_x, float scale_y, float shift_x, float shift_y, int *ix0, int *iy0, int *ix1, int *iy1);


// the following functions are equivalent to the above functions, but operate
// on glyph indices instead of Unicode codepoints (for efficiency)
//STBTT_DEF unsigned char *stbtt_GetGlyphBitmap(const stbtt_fontinfo *info, float scale_x, float scale_y, int glyph, int *width, int *height, int *xoff, int *yoff);
//STBTT_DEF unsigned char *stbtt_GetGlyphBitmapSubpixel(const stbtt_fontinfo *info, float scale_x, float scale_y, float shift_x, float shift_y, int glyph, int *width, int *height, int *xoff, int *yoff);
//STBTT_DEF void stbtt_MakeGlyphBitmap(const stbtt_fontinfo *info, unsigned char *output, int out_w, int out_h, int out_stride, float scale_x, float scale_y, int glyph);

fun FontInfo.makeGlyphBitmapSubpixel(output: UByteArray, ptr: Int, outW: Int, outH: Int, outStride: Int, scaleX: Float, scaleY: Float, shiftX: Float, shiftY: Float, glyph: Int) {
    val vertices = getGlyphShape(glyph)

    val (ix0, iy0, _, _) = getGlyphBitmapBoxSubpixel(glyph, scaleX, scaleY, shiftX, shiftY)
    val gbm = Bitmap(outW, outH, outStride, output, ptr)

    if (gbm.w != 0 && gbm.h != 0)
        gbm.rasterize( 0.35f, vertices, scaleX, scaleY, shiftX, shiftY, ix0, iy0, true)
}

//STBTT_DEF void stbtt_MakeGlyphBitmapSubpixelPrefilter(const stbtt_fontinfo *info, unsigned char *output, int out_w, int out_h, int out_stride, float scale_x, float scale_y, float shift_x, float shift_y, int oversample_x, int oversample_y, float *sub_x, float *sub_y, int glyph);

fun FontInfo.getGlyphBitmapBox(glyph: Int, scaleX: Float, scaleY: Float, box: Vec4i = Vec4i()): Vec4i = getGlyphBitmapBoxSubpixel(glyph, scaleX, scaleY, box = box)

fun FontInfo.getGlyphBitmapBoxSubpixel(glyph: Int, scaleX: Float, scaleY: Float, shiftX: Float = 0f, shiftY: Float = 0f, box: Vec4i = Vec4i()): Vec4i =
        if (!getGlyphBox(glyph, box))
        // e.g. space character
            box(0) as Vec4i // TODO ->glm
        else
        // move to integral bboxes (treating pixels as little squares, what pixels get touched)?
            Vec4i(floor(+box[0] * scaleX + shiftX),
                  floor(-box[3] * scaleY + shiftY),
                  ceil(+box[2] * scaleX + shiftX),
                  ceil(-box[1] * scaleY + shiftY))

/** @TODO: don't expose this structure */
class Bitmap(val w: Int, val h: Int,
             val stride: Int,
             val pixels: UByteArray, val ptr: Int)

fun Bitmap.rasterize(flatnessInPixels: Float, vertices: Array<Vertex>, scaleX: Float, scaleY: Float,
                     shiftX: Float, shiftY: Float, xOff: Int, yOff: Int, invert: Boolean) {

    val scale = if (scaleX > scaleY) scaleY else scaleX
    val windingLengths = ArrayList<Int>()
    val windings = flattenCurves(vertices, flatnessInPixels / scale, windingLengths)
    if (windings.isNotEmpty())
        rasterize(windings, windingLengths, scaleX, scaleY, shiftX, shiftY, xOff, yOff, invert)
}

/** returns the contours */
fun flattenCurves(vertices: Array<Vertex>, objspaceFlatness: Float, contourLengths: ArrayList<Int>): Array<Vec2> {
    lateinit var points: Array<Vec2>
    var numPoints = 0

    val objspaceFlatnessSquared = objspaceFlatness * objspaceFlatness
    var start = 0

    // count how many "moves" there are to get the contour count
    var n = vertices.count { it.type == Vertex.Type.move }

    if (n == 0) return emptyArray()

    repeat(n) { contourLengths += 0 }

    // make two passes through the points so we don't need to realloc
    for (pass in 0..1) {
        var x = 0f
        var y = 0f
        if (pass == 1)
            points = Array(numPoints) { Vec2() }
        numPoints = 0
        n = -1
        for (v in vertices)
            when (v.type) {
                Vertex.Type.move -> { // start the next contour
                    if (n >= 0)
                        contourLengths[n] = numPoints - start
                    ++n
                    start = numPoints

                    x = v.x.f; y = v.y.f
                    points[numPoints++].put(x, y)
                }

                Vertex.Type.line -> {
                    x = v.x.f; y = v.y.f
                    points[numPoints++].put(x, y)
                }

                Vertex.Type.curve -> {
                    numPoints = tesselateCurve(points, numPoints, x, y,
                                               v.cX.f, v.cY.f,
                                               v.x.f, v.y.f,
                                               objspaceFlatnessSquared, 0f)
                    x = v.x.f; y = v.y.f
                }

                Vertex.Type.cubic -> {
                    numPoints = tesselateCubic(points, numPoints, x, y,
                                               v.cX.f, v.cY.f,
                                               v.cX1.f, v.cY1.f,
                                               v.x.f, v.y.f,
                                               objspaceFlatnessSquared, 0)
                    x = v.x.f; y = v.y.f
                }

                Vertex.Type.none -> {}
            }
        contourLengths[n] = numPoints - start
    }

    return points
}

// [JVM] useless, we can use replace this by a simple `put`
//    fun addPoint(points: Array<Vec2>?, n: Int, x: Float, y: Float) {
//        if (points == null) return // during first pass, it's unallocated
//        points[n].x = x
//        points[n].y = y
//    }

/** tessellate until threshold p is happy... @TODO warped to compensate for non-linear stretching
 *  [JVM]
 *  @returns numPoints */
fun tesselateCurve(points: Array<Vec2>, numPoints_: Int, x0: Float, y0: Float, x1: Float, y1: Float,
                   x2: Float, y2: Float, objspaceFlatnessSquared: Float, n: Float): Int {
    var numPoints = numPoints_
    // midpoint
    val mx = (x0 + 2 * x1 + x2) / 4
    val my = (y0 + 2 * y1 + y2) / 4
    // versus directly drawn line
    val dx = (x0 + x2) / 2 - mx
    val dy = (y0 + y2) / 2 - my
    if (n > 16) // 65536 segments on one curve better be enough!
        return numPoints
    if (dx * dx + dy * dy > objspaceFlatnessSquared) { // half-pixel error allowed... need to be smaller if AA
        numPoints = tesselateCurve(points, numPoints, x0, y0, (x0 + x1) / 2f, (y0 + y1) / 2f, mx, my, objspaceFlatnessSquared, n + 1)
        numPoints = tesselateCurve(points, numPoints, mx, my, (x1 + x2) / 2f, (y1 + y2) / 2f, x2, y2, objspaceFlatnessSquared, n + 1)
    } else
        points[numPoints++].put(x2, y2)
    return numPoints
}

/** [JVM]
 *  @return numPoints */
fun tesselateCubic(points: Array<Vec2>, numPoints_: Int, x0: Float, y0: Float, x1: Float, y1: Float,
                   x2: Float, y2: Float, x3: Float, y3: Float, objspaceFlatnessSquared: Float, n: Int): Int {
    var numPoints = numPoints_
    // @TODO this "flatness" calculation is just made-up nonsense that seems to work well enough
    val dx0 = x1 - x0
    val dy0 = y1 - y0
    val dx1 = x2 - x1
    val dy1 = y2 - y1
    val dx2 = x3 - x2
    val dy2 = y3 - y2
    val dx = x3 - x0
    val dy = y3 - y0
    val longLen = sqrt(dx0 * dx0 + dy0 * dy0) + sqrt(dx1 * dx1 + dy1 * dy1) + sqrt(dx2 * dx2 + dy2 * dy2)
    val shortLen = sqrt(dx * dx + dy * dy)
    val flatnessSquared = longLen * longLen - shortLen * shortLen

    if (n > 16) // 65536 segments on one curve better be enough!
        return numPoints

    if (flatnessSquared > objspaceFlatnessSquared) {
        val x01 = (x0 + x1) / 2
        val y01 = (y0 + y1) / 2
        val x12 = (x1 + x2) / 2
        val y12 = (y1 + y2) / 2
        val x23 = (x2 + x3) / 2
        val y23 = (y2 + y3) / 2

        val xa = (x01 + x12) / 2
        val ya = (y01 + y12) / 2
        val xb = (x12 + x23) / 2
        val yb = (y12 + y23) / 2

        val mx = (xa + xb) / 2
        val my = (ya + yb) / 2

        numPoints = tesselateCubic(points, numPoints, x0, y0, x01, y01, xa, ya, mx, my, objspaceFlatnessSquared, n + 1)
        numPoints = tesselateCubic(points, numPoints, mx, my, xb, yb, x23, y23, x3, y3, objspaceFlatnessSquared, n + 1)
    } else
        points[numPoints++].put(x3, y3)
    return numPoints
}

fun Bitmap.rasterize(pts: Array<Vec2>, wCount: ArrayList<Int>, scaleX: Float, scaleY: Float, shiftX: Float, shiftY: Float, offX: Int, offY: Int, invert: Boolean) {
    val yScaleInv = if (invert) -scaleY else scaleY
    val vsubsample = when (TrueType.RASTERIZER_VERSION) {
        1 -> if (h < 8) 15 else 5
        2 -> 1
        else -> error("invalid STBTT_RASTERIZER_VERSION value (${TrueType.RASTERIZER_VERSION})")
    }
    // vsubsample should divide 255 evenly; otherwise we won't reach full opacity

    // now we have to blow out the windings into explicit edge lists
    var n = wCount.sum()

    val e = Array(n + 1) { TrueType.Edge() } // add an extra one as a sentinel
    n = 0

    var m = 0
    for (w in wCount) {
        val p = m
        m += w
        var j = w - 1
        var k = 0
        while (k < w) {
            var a = k
            var b = j
            // skip the edge if horizontal
            if (pts[p + j].y == pts[p + k].y) {
                j = k++
                continue
            }
            // add edge from j to k to the list
            e[n].invert = false
            if (if (invert) pts[p + j].y > pts[p + k].y else pts[p + j].y < pts[p + k].y) {
                e[n].invert = true
                a = j; b = k
            }
            e[n].x0 = pts[p + a].x * scaleX + shiftX
            e[n].y0 = (pts[p + a].y * yScaleInv + shiftY) * vsubsample
            e[n].x1 = pts[p + b].x * scaleX + shiftX
            e[n].y1 = (pts[p + b].y * yScaleInv + shiftY) * vsubsample
            ++n
            j = k++
        }
    }

    // now sort the edges by their highest point (should snap to integer, and then by x)
    //STBTT_sort(e, n, sizeof(e[0]), stbtt__edge_compare);
    sortEdges(e, n)

    // now, traverse the scanlines and find the intersections on each scanline, use xor winding rule
    when (TrueType.RASTERIZER_VERSION) {
        1 -> rasterizeSortedEdges(e, n, vsubsample, offX, offY)
        2 -> rasterizeSortedEdges(e, n, offX, offY)
    }
}

fun sortEdges(edges: Array<TrueType.Edge>, n: Int) {
    sortEdgesQuicksort(edges, 0, n)
    sortEdgesInsSort(edges, n)
}

fun sortEdgesQuicksort(edges: Array<TrueType.Edge>, p: Int, n_: Int) {
    var ptr = p
    var n = n_

    /* threshold for transitioning to insertion sort */
    while (n > 12) {
        lateinit var t: TrueType.Edge

        /* compute median of three */
        val m = n shr 1
        val c01 = edges[ptr + 0] < edges[ptr + m]
        val c12 = edges[ptr + m] < edges[ptr + n - 1]
        /* if 0 >= mid >= end, or 0 < mid < end, then use mid */
        if (c01 != c12) {
            /* otherwise, we'll need to swap something else to middle */
            val c = edges[ptr + 0] < edges[ptr + n - 1]
            /* 0>mid && mid<n:  0>n => n; 0<n => 0 */
            /* 0<mid && mid>n:  0>n => 0; 0<n => n */
            val z = if (c == c12) 0 else n - 1
            t = edges[ptr + z]
            edges[ptr + z] = edges[ptr + m]
            edges[ptr + m] = t
        }
        /* now p[m] is the median-of-three */
        /* swap it to the beginning so it won't move around */
        t = edges[ptr + 0]
        edges[ptr + 0] = edges[ptr + m]
        edges[ptr + m] = t

        /* partition loop */
        var i = 1
        var j = n - 1
        while (true) {
            /* handling of equality is crucial here */
            /* for sentinels & efficiency with duplicates */
            while (true)
                if (edges[ptr + i++] >= edges[ptr + 0]) break
            while (true)
                if (edges[ptr + 0] >= edges[ptr + j--]) break
            /* make sure we haven't crossed */
            if (i >= j) break
            t = edges[ptr + i]
            edges[ptr + i] = edges[ptr + j]
            edges[ptr + j] = t

            ++i
            --j
        }
        /* recurse on smaller side, iterate on larger */
        if (j < (n - i)) {
            sortEdgesQuicksort(edges, ptr, j)
            ptr += i
            n -= i
        } else {
            sortEdgesQuicksort(edges, ptr + i, n - i)
            n = j
        }
    }
}

fun sortEdgesInsSort(edges: Array<TrueType.Edge>, n: Int) {
    for (i in 1 until n) {
        val t = edges[i]
        val a = t
        var j = i
        while (j > 0) {
            val b = edges[j - 1]
            val c = a < b
            if (!c) break
            edges[j] = edges[j - 1]
            --j
        }
        if (i != j)
            edges[j] = t
    }
}

fun rasterizeSortedEdges(edges: Array<TrueType.Edge>, n: Int, vSubsample: Int, offX: Int, offY: Int) {
    TODO()
//        val hh = TrueType.HHeap()
//        var active: TrueType.ActiveEdge? = null
//        var j = 0
//        val maxWeight = 255 / vSubsample  // weight per vertical scanline
//
//        val scanline = PtrByte(if (result.w > 512) result.w else 512)
//
//        var y = offY * vSubsample
//        var e = 0
//        edges[e + n].y0 = (offY + result.h) * vSubsample.f + 1
//
//        while (j < result.h) {
//            scanline.fill(0, result.w)
//            for (s in 0 until vSubsample) { // vertical subsample index
//                // find center of pixel for this scanline
//                val scanY = y + 0.5f
//                var step = active
//                var prev: TrueType.ActiveEdge? = null
//
//                // update all active edges;
//                // remove all active edges that terminate before the center of this scanline
//                while (step != null) {
//                    val z = step
//                    if (z.ey <= scanY) {
//                        step = z.next // delete from list
//                        prev?.next = step
//                        if (z === active) active = active.next
//                        assert(z.direction != 0f)
//                        z.direction = 0f
//                        TrueType.hheapFree(hh, z)
//                    } else {
//                        z.x += z.dx // advance to position for current scanline
//                        prev = step
//                        step = step.next // advance through list
//                    }
//                }
//
//                // resort the list if needed
//                while (true) {
//                    var changed = false
//                    step = active
//                    while (step?.next != null) {
//                        if (step.x > step.next!!.x) {
//                            val t = step
//                            val q = t.next!!
//
//                            t.next = q.next
//                            q.next = t
//                            step = q
//                            changed = true
//                        }
//                        prev = step
//                        step = step.next
//                    }
//                    if (!changed) break
//                }
//
//                // insert all edges that start before the center of this scanline -- omit ones that also end on this scanline
//                while (edges[e].y0 <= scanY) {
//                    if (edges[e].y1 > scanY) {
//                        val z = TrueType.newActive1(hh, edges[e], offX, scanY)
//                        // find insertion point
//                        when {
//                            active == null -> active = z
//                            z.x < active.x -> {
//                                // insert at front
//                                z.next = active
//                                active = z
//                            }
//
//                            else -> {
//                                // find thing to insert AFTER
//                                var p: TrueType.ActiveEdge = active
//                                while (p.next != null && p.next!!.x < z.x)
//                                    p = p.next!!
//                                // at this point, p->next->x is NOT < z->x
//                                z.next = p.next
//                                p.next = z
//                            }
//                        }
//                    }
//                    ++e
//                }
//
//                // now process all active edges in XOR fashion
//                active?.let {
//                    TrueType.fillActiveEdges(scanline, result.w, it, maxWeight)
//                }
//
//                ++y
//            }
//            for (i in 0 until result.w)
//                result.pixels[j * result.stride + i] = scanline[i]
//            ++j
//        }
////
////        stbtt__hheap_cleanup(& hh, userdata)
////
////        if (scanline != scanlineData)
////            STBTT_free(scanline, userdata)
}

/** directly AA rasterize edges w/o supersampling
 *  [JVM] signature different for different STBTT_RASTERIZER_VERSION, no need to mentioning version in name */
fun Bitmap.rasterizeSortedEdges(edges: Array<TrueType.Edge>, n: Int, /*vSubsample: Int,*/ offX: Int, offY: Int) {

    var active: TrueType.ActiveEdge? = null

    val scanline = PtrFloat(if (w > 64) w * 2 + 1 else 129)

    val scanline2 = scanline + w

    var y = offY
    edges[n].y0 = (offY + h).f + 1
    var e = 0

    var j = 0
    while (j < h) {
        // find center of pixel for this scanline
        val scanYTop = y + 0f
        val scanYBottom = y + 1f
        var step = active
        var prev: TrueType.ActiveEdge? = null

        scanline.fill(0f, w)
        scanline2.fill(0f, w + 1)

        // update all active edges;
        // remove all active edges that terminate before the top of this scanline
        while (step != null) {
            val z = step
            if (z.ey <= scanYTop) {
                step = z.next // delete from list
                prev?.next = step
                if (z === active) active = active.next
                assert(z.direction != 0f)
                z.direction = 0f
            } else { // advance through list
                prev = step
                step = z.next
            }
        }

        // insert all edges that start before the bottom of this scanline
        while (edges[e].y0 <= scanYBottom) {
            if (edges[e].y0 != edges[e].y1) {
                val z = newActive2(edges[e], offX, scanYTop)
//                    if (z != NULL) {
                if (j == 0 && offY != 0)
                    if (z.ey < scanYTop)
                    // this can happen due to subpixel positioning and some kind of fp rounding error i think
                        z.ey = scanYTop
                assert(z.ey >= scanYTop) { "if we get really unlucky a tiny bit of an edge can be out of bounds" }
                // insert at front
                z.next = active
                active = z
//                    }
            }
            ++e
        }

        // now process all active edges
        active?.let {
            TrueType.fillActiveEdgesNew(scanline, scanline2 + 1, w, it, scanYTop)
        }
        run {
            var sum = 0f
            for (i in 0 until w) {
                sum += scanline2[i]
                var k = scanline[i] + sum
                k = abs(k) * 255 + 0.5f
                val m = k.i
                pixels[j * stride + i] = (if (m > 255) 255 else m).ub
            }
        }
        // advance all the edges
        step = active
        while (step != null) {
            val z = step
            z.fx += z.fdx // advance to position for current scanline
//                prev = step // advance through list
            step = step.next // advance through list
        }

        ++y
        ++j
    }
//        stbtt__hheap_cleanup(& hh, userdata)
//
//        if (scanline != scanlineData)
//            STBTT_free(scanline, userdata)
}

//#if STBTT_RASTERIZER_VERSION == 1
const val FIXSHIFT = 10
const val FIX = 1 shl FIXSHIFT
const val FIXMASK = FIX - 1


fun newActive1(e: TrueType.Edge, offX: Int, startPoint: Float): TrueType.ActiveEdge {
    val z = TrueType.ActiveEdge()
    val dxdy = (e.x1 - e.x0) / (e.y1 - e.y0)
//        STBTT_assert(z != NULL)
//        if (!z) return z

    // round dx down to avoid overshooting
    if (dxdy < 0)
        z.dx = -floor(FIX * -dxdy).i
    else
        z.dx = floor(FIX * dxdy).i

    z.x = floor(FIX * e.x0 + z.dx * (startPoint - e.y0)).i // use z->dx so when we offset later it's by the same amount
    z.x -= offX * FIX

    z.ey = e.y1
    z.next = null
    z.direction = if (e.invert) 1f else -1f
    return z
}

//#elif STBTT_RASTERIZER_VERSION == 2
fun newActive2(e: TrueType.Edge, offX: Int, startPoint: Float): TrueType.ActiveEdge {
    val z = TrueType.ActiveEdge()
    val dxdy = (e.x1 - e.x0) / (e.y1 - e.y0)
    //STBTT_assert(e->y0 <= start_point);
    z.fdx = dxdy
    z.fdy = if (dxdy != 0f) 1f / dxdy else 0f
    z.fx = e.x0 + dxdy * (startPoint - e.y0)
    z.fx -= offX
    z.direction = if (e.invert) 1f else -1f
    z.sy = e.y0
    z.ey = e.y1
    z.next = null
    return z
}
//#else
//#error "Unrecognized value of STBTT_RASTERIZER_VERSION"
//#endif
