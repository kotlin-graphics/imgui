@file:OptIn(ExperimentalStdlibApi::class)

package plot.items

import glm_.vec2.Vec2
import imgui.classes.DrawList
import imgui.internal.classes.Rect
import kool.pos
import plot.api.Getter
import plot.api.PlotMarker
import uno.kotlin.plusAssign
import kotlin.math.max

//-----------------------------------------------------------------------------
// [SECTION] Markers
//-----------------------------------------------------------------------------

//template <class _Getter>
class RendererMarkersFill(val getter: Getter, val marker: Array<Vec2>, val size: Float, val col: UInt) : RendererBase(getter.count, (marker.size - 2) * 3, marker.size) {

    val count = marker.size
    val uv = Vec2()
    override fun init(drawList: DrawList) {
        uv put drawList._data.texUvWhitePixel
    }

    override fun render(drawList: DrawList, cullRect: Rect, prim: Int): Boolean {
        val p = transformer[getter[prim]]
        if (p.x >= cullRect.min.x && p.y >= cullRect.min.y && p.x <= cullRect.max.x && p.y <= cullRect.max.y) {
            for (i in 0..<count) {
                drawList.vtxBuffer.pos = drawList._vtxWritePtr
                drawList.vtxBuffer.also {
                    it += p.x + marker[i].x * size; it += p.y + marker[i].y * size; it += uv; it += col.toInt()
                }
                drawList._vtxWritePtr++
            }
            for (i in 2..<count) {
                drawList.idxBuffer.pos = drawList._idxWritePtr
                drawList.idxBuffer.also {
                    it += drawList._vtxCurrentIdx
                    it += drawList._vtxCurrentIdx + i - 1
                    it += drawList._vtxCurrentIdx + i
                }
                drawList._idxWritePtr += 3
            }
            drawList._vtxCurrentIdx += count
            return true
        }
        return false
    }
}


//template <class _Getter>
class RendererMarkersLine(val getter: Getter, val marker: Array<Vec2>, val size: Float, weight: Float, val col: UInt) : RendererBase(getter.count, marker.size / 2 * 6, marker.size / 2 * 4) {

    val count = marker.size
    var halfWeight = max(1f, weight) * 0.5f
    val uv0 = Vec2()
    val uv1 = Vec2()
    override fun init(drawList: DrawList) {
        halfWeight = getLineRenderProps(drawList, halfWeight, uv0, uv1)
    }

    override fun render(drawList: DrawList, cullRect: Rect, prim: Int): Boolean {
        val p = transformer[getter[prim]]
        if (p.x >= cullRect.min.x && p.y >= cullRect.min.y && p.x <= cullRect.max.x && p.y <= cullRect.max.y) {
            var i = 0
            while (i < count) {
                val p1 = Vec2(p.x + marker[i].x * size, p.y + marker[i].y * size)
                val p2 = Vec2(p.x + marker[i + 1].x * size, p.y + marker[i + 1].y * size)
                primLine(drawList, p1, p2, halfWeight, col, uv0, uv1)
                i += 2
            }
            return true
        }
        return false
    }
}

val MARKER_FILL_CIRCLE = arrayOf(Vec2(1f, 0f), Vec2(0.809017f, 0.58778524f), Vec2(0.30901697f, 0.95105654f), Vec2(-0.30901703f, 0.9510565f), Vec2(-0.80901706f, 0.5877852f), Vec2(-1f, 0f), Vec2(-0.80901694f, -0.58778536f), Vec2(-0.3090171f, -0.9510565f), Vec2(0.30901712f, -0.9510565f), Vec2(0.80901694f, -0.5877853f))
val MARKER_FILL_SQUARE = arrayOf(Vec2(SQRT_1_2), Vec2(SQRT_1_2, -SQRT_1_2), Vec2(-SQRT_1_2), Vec2(-SQRT_1_2, SQRT_1_2))
val MARKER_FILL_DIAMOND = arrayOf(Vec2(1, 0), Vec2(0, -1), Vec2(-1, 0), Vec2(0, 1))
val MARKER_FILL_UP = arrayOf(Vec2(SQRT_3_2, 0.5f), Vec2(0, -1), Vec2(-SQRT_3_2, 0.5f))
val MARKER_FILL_DOWN = arrayOf(Vec2(SQRT_3_2, -0.5f), Vec2(0, 1), Vec2(-SQRT_3_2, -0.5f))
val MARKER_FILL_LEFT = arrayOf(Vec2(-1, 0), Vec2(0.5, SQRT_3_2), Vec2(0.5, -SQRT_3_2))
val MARKER_FILL_RIGHT = arrayOf(Vec2(1, 0), Vec2(-0.5, SQRT_3_2), Vec2(-0.5, -SQRT_3_2))

val MARKER_LINE_CIRCLE = arrayOf(
        Vec2(1.0f, 0.0f),
        Vec2(0.809017f, 0.58778524f),
        Vec2(0.809017f, 0.58778524f),
        Vec2(0.30901697f, 0.95105654f),
        Vec2(0.30901697f, 0.95105654f),
        Vec2(-0.30901703f, 0.9510565f),
        Vec2(-0.30901703f, 0.9510565f),
        Vec2(-0.80901706f, 0.5877852f),
        Vec2(-0.80901706f, 0.5877852f),
        Vec2(-1.0f, 0.0f),
        Vec2(-1.0f, 0.0f),
        Vec2(-0.80901694f, -0.58778536f),
        Vec2(-0.80901694f, -0.58778536f),
        Vec2(-0.3090171f, -0.9510565f),
        Vec2(-0.3090171f, -0.9510565f),
        Vec2(0.30901712f, -0.9510565f),
        Vec2(0.30901712f, -0.9510565f),
        Vec2(0.80901694f, -0.5877853f),
        Vec2(0.80901694f, -0.5877853f),
        Vec2(1.0f, 0.0f))
val MARKER_LINE_SQUARE = arrayOf(Vec2(SQRT_1_2, SQRT_1_2), Vec2(SQRT_1_2, -SQRT_1_2), Vec2(SQRT_1_2, -SQRT_1_2), Vec2(-SQRT_1_2, -SQRT_1_2), Vec2(-SQRT_1_2, -SQRT_1_2), Vec2(-SQRT_1_2, SQRT_1_2), Vec2(-SQRT_1_2, SQRT_1_2), Vec2(SQRT_1_2, SQRT_1_2))
val MARKER_LINE_DIAMOND = arrayOf(Vec2(1, 0), Vec2(0, -1), Vec2(0, -1), Vec2(-1, 0), Vec2(-1, 0), Vec2(0, 1), Vec2(0, 1), Vec2(1, 0))
val MARKER_LINE_UP = arrayOf(Vec2(SQRT_3_2, 0.5f), Vec2(0, -1), Vec2(0, -1), Vec2(-SQRT_3_2, 0.5f), Vec2(-SQRT_3_2, 0.5f), Vec2(SQRT_3_2, 0.5f))
val MARKER_LINE_DOWN = arrayOf(Vec2(SQRT_3_2, -0.5f), Vec2(0, 1), Vec2(0, 1), Vec2(-SQRT_3_2, -0.5f), Vec2(-SQRT_3_2, -0.5f), Vec2(SQRT_3_2, -0.5f))
val MARKER_LINE_LEFT = arrayOf(Vec2(-1, 0), Vec2(0.5, SQRT_3_2), Vec2(0.5, SQRT_3_2), Vec2(0.5, -SQRT_3_2), Vec2(0.5, -SQRT_3_2), Vec2(-1, 0))
val MARKER_LINE_RIGHT = arrayOf(Vec2(1, 0), Vec2(-0.5, SQRT_3_2), Vec2(-0.5, SQRT_3_2), Vec2(-0.5, -SQRT_3_2), Vec2(-0.5, -SQRT_3_2), Vec2(1, 0))
val MARKER_LINE_ASTERISK = arrayOf(Vec2(-SQRT_3_2, -0.5f), Vec2(SQRT_3_2, 0.5f), Vec2(-SQRT_3_2, 0.5f), Vec2(SQRT_3_2, -0.5f), Vec2(0, -1), Vec2(0, 1))
val MARKER_LINE_PLUS = arrayOf(Vec2(-1, 0), Vec2(1, 0), Vec2(0, -1), Vec2(0, 1))
val MARKER_LINE_CROSS = arrayOf(Vec2(-SQRT_1_2, -SQRT_1_2), Vec2(SQRT_1_2, SQRT_1_2), Vec2(SQRT_1_2, -SQRT_1_2), Vec2(-SQRT_1_2, SQRT_1_2))

//template <typename _Getter>
fun renderMarkers(getter: Getter, marker: PlotMarker, size: Float, rendFill: Boolean, colFill: UInt, rendLine: Boolean, colLine: UInt, weight: Float) {
    if (rendFill)
        when (marker) {
            PlotMarker.Circle -> renderPrimitives1(RendererMarkersFill(getter, MARKER_FILL_CIRCLE, size, colFill))
            PlotMarker.Square -> renderPrimitives1(RendererMarkersFill(getter, MARKER_FILL_SQUARE, size, colFill))
            PlotMarker.Diamond -> renderPrimitives1(RendererMarkersFill(getter, MARKER_FILL_DIAMOND, size, colFill))
            PlotMarker.Up -> renderPrimitives1(RendererMarkersFill(getter, MARKER_FILL_UP, size, colFill))
            PlotMarker.Down -> renderPrimitives1(RendererMarkersFill(getter, MARKER_FILL_DOWN, size, colFill))
            PlotMarker.Left -> renderPrimitives1(RendererMarkersFill(getter, MARKER_FILL_LEFT, size, colFill))
            PlotMarker.Right -> renderPrimitives1(RendererMarkersFill(getter, MARKER_FILL_RIGHT, size, colFill))
            else -> {}
        }
    if (rendLine)
        when (marker) {
            PlotMarker.Circle -> renderPrimitives1(RendererMarkersLine(getter, MARKER_LINE_CIRCLE, size, weight, colLine))
            PlotMarker.Square -> renderPrimitives1(RendererMarkersLine(getter, MARKER_LINE_SQUARE, size, weight, colLine))
            PlotMarker.Diamond -> renderPrimitives1(RendererMarkersLine(getter, MARKER_LINE_DIAMOND, size, weight, colLine))
            PlotMarker.Up -> renderPrimitives1(RendererMarkersLine(getter, MARKER_LINE_UP, size, weight, colLine))
            PlotMarker.Down -> renderPrimitives1(RendererMarkersLine(getter, MARKER_LINE_DOWN, size, weight, colLine))
            PlotMarker.Left -> renderPrimitives1(RendererMarkersLine(getter, MARKER_LINE_LEFT, size, weight, colLine))
            PlotMarker.Right -> renderPrimitives1(RendererMarkersLine(getter, MARKER_LINE_RIGHT, size, weight, colLine))
            PlotMarker.Asterisk -> renderPrimitives1(RendererMarkersLine(getter, MARKER_LINE_ASTERISK, size, weight, colLine))
            PlotMarker.Plus -> renderPrimitives1(RendererMarkersLine(getter, MARKER_LINE_PLUS, size, weight, colLine))
            PlotMarker.Cross -> renderPrimitives1(RendererMarkersLine(getter, MARKER_LINE_CROSS, size, weight, colLine))
            else -> {}
        }
}