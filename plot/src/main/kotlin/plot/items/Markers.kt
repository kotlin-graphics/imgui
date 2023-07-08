@file:OptIn(ExperimentalStdlibApi::class)

package plot.items

import glm_.vec2.Vec2
import imgui.classes.DrawList
import imgui.internal.classes.Rect
import kool.pos
import plot.api.Getter
import plot.api.PlotMarker
import uno.kotlin.plusAssign

//-----------------------------------------------------------------------------
// [SECTION] Markers
//-----------------------------------------------------------------------------

//template <class _Getter>
class RendererMarkersFill(val getter: Getter, val marker: Array<Vec2>, val count: Int, val size: Float, val col: UInt) : RendererBase(getter.count, (count - 2) * 3, count) {

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

//
//template <class _Getter>
//struct RendererMarkersLine : RendererBase {
//    RendererMarkersLine(const _Getter& getter, const ImVec2* marker, int count, float size, float weight, ImU32 col) :
//    RendererBase(getter.Count, count/2*6, count/2*4),
//    Getter(getter),
//    Marker(marker),
//    Count(count),
//    HalfWeight(ImMax(1.0f,weight)*0.5f),
//    Size(size),
//    Col(col)
//    { }
//    void Init(ImDrawList& draw_list) const {
//        GetLineRenderProps(draw_list, HalfWeight, UV0, UV1);
//    }
//    IMPLOT_INLINE bool Render(ImDrawList& draw_list, const ImRect& cull_rect, int prim) const {
//        ImVec2 p = this->Transformer(Getter(prim));
//        if (p.x >= cull_rect.Min.x && p.y >= cull_rect.Min.y && p.x <= cull_rect.Max.x && p.y <= cull_rect.Max.y) {
//            for (int i = 0; i < Count; i = i + 2) {
//                ImVec2 p1(p.x + Marker[i].x * Size, p.y + Marker[i].y * Size);
//                ImVec2 p2(p.x + Marker[i+1].x * Size, p.y + Marker[i+1].y * Size);
//                PrimLine(draw_list, p1, p2, HalfWeight, Col, UV0, UV1);
//            }
//            return true;
//        }
//        return false;
//    }
//    const _Getter& Getter;
//    const ImVec2* Marker;
//    const int Count;
//    mutable float HalfWeight;
//    const float Size;
//    const ImU32 Col;
//    mutable ImVec2 UV0;
//    mutable ImVec2 UV1;
//};

val MARKER_FILL_CIRCLE = arrayOf(Vec2(1f, 0f), Vec2(0.809017f, 0.58778524f), Vec2(0.30901697f, 0.95105654f), Vec2(-0.30901703f, 0.9510565f), Vec2(-0.80901706f, 0.5877852f), Vec2(-1f, 0f), Vec2(-0.80901694f, -0.58778536f), Vec2(-0.3090171f, -0.9510565f), Vec2(0.30901712f, -0.9510565f), Vec2(0.80901694f, -0.5877853f))
val MARKER_FILL_SQUARE = arrayOf(Vec2(SQRT_1_2), Vec2(SQRT_1_2, -SQRT_1_2), Vec2(-SQRT_1_2), Vec2(-SQRT_1_2, SQRT_1_2))
val MARKER_FILL_DIAMOND = arrayOf(Vec2(1, 0), Vec2(0, -1), Vec2(-1, 0), Vec2(0, 1))
val MARKER_FILL_UP = arrayOf(Vec2(SQRT_3_2, 0.5f), Vec2(0, -1), Vec2(-SQRT_3_2, 0.5f))
val MARKER_FILL_DOWN = arrayOf(Vec2(SQRT_3_2, -0.5f), Vec2(0, 1), Vec2(-SQRT_3_2, -0.5f))
val MARKER_FILL_LEFT = arrayOf(Vec2(-1, 0), Vec2(0.5, SQRT_3_2), Vec2(0.5, -SQRT_3_2))
val MARKER_FILL_RIGHT = arrayOf(Vec2(1, 0), Vec2(-0.5, SQRT_3_2), Vec2(-0.5, -SQRT_3_2))

//static const ImVec2 MARKER_LINE_CIRCLE[20]  = {
//    ImVec2(1.0f, 0.0f),
//    ImVec2(0.809017f, 0.58778524f),
//    ImVec2(0.809017f, 0.58778524f),
//    ImVec2(0.30901697f, 0.95105654f),
//    ImVec2(0.30901697f, 0.95105654f),
//    ImVec2(-0.30901703f, 0.9510565f),
//    ImVec2(-0.30901703f, 0.9510565f),
//    ImVec2(-0.80901706f, 0.5877852f),
//    ImVec2(-0.80901706f, 0.5877852f),
//    ImVec2(-1.0f, 0.0f),
//    ImVec2(-1.0f, 0.0f),
//    ImVec2(-0.80901694f, -0.58778536f),
//    ImVec2(-0.80901694f, -0.58778536f),
//    ImVec2(-0.3090171f, -0.9510565f),
//    ImVec2(-0.3090171f, -0.9510565f),
//    ImVec2(0.30901712f, -0.9510565f),
//    ImVec2(0.30901712f, -0.9510565f),
//    ImVec2(0.80901694f, -0.5877853f),
//    ImVec2(0.80901694f, -0.5877853f),
//    ImVec2(1.0f, 0.0f)
//};
//static const ImVec2 MARKER_LINE_SQUARE[8]   = {ImVec2(SQRT_1_2,SQRT_1_2), ImVec2(SQRT_1_2,-SQRT_1_2), ImVec2(SQRT_1_2,-SQRT_1_2), ImVec2(-SQRT_1_2,-SQRT_1_2), ImVec2(-SQRT_1_2,-SQRT_1_2), ImVec2(-SQRT_1_2,SQRT_1_2), ImVec2(-SQRT_1_2,SQRT_1_2), ImVec2(SQRT_1_2,SQRT_1_2)};
//static const ImVec2 MARKER_LINE_DIAMOND[8]  = {ImVec2(1, 0), ImVec2(0, -1), ImVec2(0, -1), ImVec2(-1, 0), ImVec2(-1, 0), ImVec2(0, 1), ImVec2(0, 1), ImVec2(1, 0)};
//static const ImVec2 MARKER_LINE_UP[6]       = {ImVec2(SQRT_3_2,0.5f), ImVec2(0,-1),ImVec2(0,-1),ImVec2(-SQRT_3_2,0.5f),ImVec2(-SQRT_3_2,0.5f),ImVec2(SQRT_3_2,0.5f)};
//static const ImVec2 MARKER_LINE_DOWN[6]     = {ImVec2(SQRT_3_2,-0.5f),ImVec2(0,1),ImVec2(0,1),ImVec2(-SQRT_3_2,-0.5f), ImVec2(-SQRT_3_2,-0.5f), ImVec2(SQRT_3_2,-0.5f)};
//static const ImVec2 MARKER_LINE_LEFT[6]     = {ImVec2(-1,0), ImVec2(0.5, SQRT_3_2),  ImVec2(0.5, SQRT_3_2),  ImVec2(0.5, -SQRT_3_2) , ImVec2(0.5, -SQRT_3_2) , ImVec2(-1,0) };
//static const ImVec2 MARKER_LINE_RIGHT[6]    = {ImVec2(1,0),  ImVec2(-0.5, SQRT_3_2), ImVec2(-0.5, SQRT_3_2), ImVec2(-0.5, -SQRT_3_2), ImVec2(-0.5, -SQRT_3_2), ImVec2(1,0) };
//static const ImVec2 MARKER_LINE_ASTERISK[6] = {ImVec2(-SQRT_3_2, -0.5f), ImVec2(SQRT_3_2, 0.5f),  ImVec2(-SQRT_3_2, 0.5f), ImVec2(SQRT_3_2, -0.5f), ImVec2(0, -1), ImVec2(0, 1)};
//static const ImVec2 MARKER_LINE_PLUS[4]     = {ImVec2(-1, 0), ImVec2(1, 0), ImVec2(0, -1), ImVec2(0, 1)};
//static const ImVec2 MARKER_LINE_CROSS[4]    = {ImVec2(-SQRT_1_2,-SQRT_1_2),ImVec2(SQRT_1_2,SQRT_1_2),ImVec2(SQRT_1_2,-SQRT_1_2),ImVec2(-SQRT_1_2,SQRT_1_2)};

//template <typename _Getter>
fun renderMarkers(getter: Getter, marker: PlotMarker, size: Float, rendFill: Boolean, colFill: UInt, rendLine: Boolean, colLine: UInt, weight: Float) {
    if (rendFill)
        when (marker) {
            PlotMarker.Circle -> renderPrimitives1(RendererMarkersFill(getter, MARKER_FILL_CIRCLE, 10, size, colFill))
            PlotMarker.Square -> renderPrimitives1(RendererMarkersFill(getter, MARKER_FILL_SQUARE, 4, size, colFill))
            PlotMarker.Diamond -> renderPrimitives1(RendererMarkersFill(getter, MARKER_FILL_DIAMOND, 4, size, colFill))
            PlotMarker.Up -> renderPrimitives1(RendererMarkersFill(getter, MARKER_FILL_UP, 3, size, colFill))
            PlotMarker.Down -> renderPrimitives1(RendererMarkersFill(getter, MARKER_FILL_DOWN, 3, size, colFill))
            PlotMarker.Left -> renderPrimitives1(RendererMarkersFill(getter, MARKER_FILL_LEFT, 3, size, colFill))
            PlotMarker.Right -> renderPrimitives1(RendererMarkersFill(getter, MARKER_FILL_RIGHT, 3, size, colFill))
            else -> {}
        }
    if (rendLine)
        when(marker) {
            case ImPlotMarker_Circle : RenderPrimitives1 < RendererMarkersLine >(getter, MARKER_LINE_CIRCLE, 20, size, weight, colLine); break
            case ImPlotMarker_Square : RenderPrimitives1 < RendererMarkersLine >(getter, MARKER_LINE_SQUARE, 8, size, weight, colLine); break
            case ImPlotMarker_Diamond : RenderPrimitives1 < RendererMarkersLine >(getter, MARKER_LINE_DIAMOND, 8, size, weight, colLine); break
            case ImPlotMarker_Up : RenderPrimitives1 < RendererMarkersLine >(getter, MARKER_LINE_UP, 6, size, weight, colLine); break
            case ImPlotMarker_Down : RenderPrimitives1 < RendererMarkersLine >(getter, MARKER_LINE_DOWN, 6, size, weight, colLine); break
            case ImPlotMarker_Left : RenderPrimitives1 < RendererMarkersLine >(getter, MARKER_LINE_LEFT, 6, size, weight, colLine); break
            case ImPlotMarker_Right : RenderPrimitives1 < RendererMarkersLine >(getter, MARKER_LINE_RIGHT, 6, size, weight, colLine); break
            case ImPlotMarker_Asterisk : RenderPrimitives1 < RendererMarkersLine >(getter, MARKER_LINE_ASTERISK, 6, size, weight, colLine); break
            case ImPlotMarker_Plus : RenderPrimitives1 < RendererMarkersLine >(getter, MARKER_LINE_PLUS, 4, size, weight, colLine); break
            case ImPlotMarker_Cross : RenderPrimitives1 < RendererMarkersLine >(getter, MARKER_LINE_CROSS, 4, size, weight, colLine); break
        }
}