package plot.items

import glm_.i
import glm_.isNaN
import glm_.max
import glm_.vec2.Vec2
import imgui.classes.DrawList
import imgui.internal.classes.Rect
import kool.pos
import plot.api.Getter
import plot.internalApi.intersection
import uno.kotlin.plusAssign
import kotlin.math.max
import kotlin.math.min

//-----------------------------------------------------------------------------
// [SECTION] Renderers
//-----------------------------------------------------------------------------

abstract class RendererBase(val prims: Int, val idxConsumed: Int, val vtxConsumed: Int) {
    abstract infix fun init(drawList: DrawList)
    abstract fun render(drawList: DrawList, cullRect: Rect, prim: Int): Boolean
    val transformer = Transformer2()
}

//template <class _Getter>
class RendererLineStrip(val getter: Getter, val col: UInt, weight: Float) : RendererBase(getter.count - 1, 6, 4) {

    var halfWeight = max(1f,weight)*0.5f
    val p1 = transformer[getter[0]]
    val uv0 = Vec2()
    val uv1 = Vec2()

    override fun init(drawList: DrawList) {
        halfWeight = getLineRenderProps(drawList, halfWeight, uv0, uv1)
    }

    override fun render(drawList: DrawList, cullRect: Rect, prim: Int): Boolean {
        val p2 = transformer[getter[prim + 1]]
        if (!cullRect.overlaps(Rect(p1 min p2, p1 max p2))) {
            p1 put p2
            return false
        }
        primLine(drawList, p1, p2, halfWeight, col, uv0, uv1)
        p1 put p2
        return true
    }
}

//template <class _Getter>
class RendererLineStripSkip(val getter: Getter, val col: UInt, weight: Float) : RendererBase(getter.count - 1, 6, 4) {
    var halfWeight = max(1f, weight) * 0.5f
    val p1 = transformer[getter[0]]
    val uv0 = Vec2()
    val uv1 = Vec2()
    override fun init(drawList: DrawList) {
        halfWeight = getLineRenderProps(drawList, halfWeight, uv0, uv1)
    }

    override fun render(drawList: DrawList, cullRect: Rect, prim: Int): Boolean {
        val p2 = transformer[getter[prim + 1]]
        if (!cullRect.overlaps(Rect(p1 min p2, p1 max p2))) {
            if (!p2.x.isNaN && !p2.y.isNaN)
                p1 put p2
            return false
        }
        primLine(drawList, p1, p2, halfWeight, col, uv0, uv1)
        if (!p2.x.isNaN && !p2.y.isNaN)
            p1 put p2
        return true
    }
}

//template <class _Getter>
class RendererLineSegments1(val getter: Getter, val col: UInt, weight: Float) : RendererBase(getter.count / 2, 6, 4) {
    var halfWeight = (1f max weight) * 0.5f
    val uv0 = Vec2()
    val uv1 = Vec2()
    override fun init(drawList: DrawList) {
        halfWeight = getLineRenderProps(drawList, halfWeight, uv0, uv1)
    }

    override fun render(drawList: DrawList, cullRect: Rect, prim: Int): Boolean {
        val p1 = transformer[getter[prim * 2 + 0]]
        val p2 = transformer[getter[prim * 2 + 1]]
        if (!cullRect.overlaps(Rect(p1 min p2, p1 max p2)))
            return false
        primLine(drawList, p1, p2, halfWeight, col, uv0, uv1)
        return true
    }
}

//template <class _Getter1, class _Getter2>
//struct RendererLineSegments2 : RendererBase {
//    RendererLineSegments2(const _Getter1& getter1, const _Getter2& getter2, ImU32 col, float weight) :
//    RendererBase(ImMin(getter1.Count, getter1.Count), 6, 4),
//    Getter1(getter1),
//    Getter2(getter2),
//    Col(col),
//    HalfWeight(ImMax(1.0f,weight)*0.5f)
//    {}
//    void Init(ImDrawList& draw_list) const {
//        GetLineRenderProps(draw_list, HalfWeight, UV0, UV1);
//    }
//    IMPLOT_INLINE bool Render(ImDrawList& draw_list, const ImRect& cull_rect, int prim) const {
//        ImVec2 P1 = this->Transformer(Getter1(prim));
//        ImVec2 P2 = this->Transformer(Getter2(prim));
//        if (!cull_rect.Overlaps(ImRect(ImMin(P1, P2), ImMax(P1, P2))))
//            return false;
//        PrimLine(draw_list,P1,P2,HalfWeight,Col,UV0,UV1);
//        return true;
//    }
//    const _Getter1& Getter1;
//    const _Getter2& Getter2;
//    const ImU32 Col;
//    mutable float HalfWeight;
//    mutable ImVec2 UV0;
//    mutable ImVec2 UV1;
//};
//
//template <class _Getter1, class _Getter2>
//struct RendererBarsFillV : RendererBase {
//    RendererBarsFillV(const _Getter1& getter1, const _Getter2& getter2, ImU32 col, double width) :
//    RendererBase(ImMin(getter1.Count, getter1.Count), 6, 4),
//    Getter1(getter1),
//    Getter2(getter2),
//    Col(col),
//    HalfWidth(width/2)
//    {}
//    void Init(ImDrawList& draw_list) const {
//        UV = draw_list._Data->TexUvWhitePixel;
//    }
//    IMPLOT_INLINE bool Render(ImDrawList& draw_list, const ImRect& cull_rect, int prim) const {
//        ImPlotPoint p1 = Getter1(prim);
//        ImPlotPoint p2 = Getter2(prim);
//        p1.x += HalfWidth;
//        p2.x -= HalfWidth;
//        ImVec2 P1 = this->Transformer(p1);
//        ImVec2 P2 = this->Transformer(p2);
//        float width_px = ImAbs(P1.x-P2.x);
//        if (width_px < 1.0f) {
//            P1.x += P1.x > P2.x ? (1-width_px) / 2 : (width_px-1) / 2;
//            P2.x += P2.x > P1.x ? (1-width_px) / 2 : (width_px-1) / 2;
//        }
//        ImVec2 PMin = ImMin(P1, P2);
//        ImVec2 PMax = ImMax(P1, P2);
//        if (!cull_rect.Overlaps(ImRect(PMin, PMax)))
//            return false;
//        PrimRectFill(draw_list,PMin,PMax,Col,UV);
//        return true;
//    }
//    const _Getter1& Getter1;
//    const _Getter2& Getter2;
//    const ImU32 Col;
//    const double HalfWidth;
//    mutable ImVec2 UV;
//};
//
//template <class _Getter1, class _Getter2>
//struct RendererBarsFillH : RendererBase {
//    RendererBarsFillH(const _Getter1& getter1, const _Getter2& getter2, ImU32 col, double height) :
//    RendererBase(ImMin(getter1.Count, getter1.Count), 6, 4),
//    Getter1(getter1),
//    Getter2(getter2),
//    Col(col),
//    HalfHeight(height/2)
//    {}
//    void Init(ImDrawList& draw_list) const {
//        UV = draw_list._Data->TexUvWhitePixel;
//    }
//    IMPLOT_INLINE bool Render(ImDrawList& draw_list, const ImRect& cull_rect, int prim) const {
//        ImPlotPoint p1 = Getter1(prim);
//        ImPlotPoint p2 = Getter2(prim);
//        p1.y += HalfHeight;
//        p2.y -= HalfHeight;
//        ImVec2 P1 = this->Transformer(p1);
//        ImVec2 P2 = this->Transformer(p2);
//        float height_px = ImAbs(P1.y-P2.y);
//        if (height_px < 1.0f) {
//            P1.y += P1.y > P2.y ? (1-height_px) / 2 : (height_px-1) / 2;
//            P2.y += P2.y > P1.y ? (1-height_px) / 2 : (height_px-1) / 2;
//        }
//        ImVec2 PMin = ImMin(P1, P2);
//        ImVec2 PMax = ImMax(P1, P2);
//        if (!cull_rect.Overlaps(ImRect(PMin, PMax)))
//            return false;
//        PrimRectFill(draw_list,PMin,PMax,Col,UV);
//        return true;
//    }
//    const _Getter1& Getter1;
//    const _Getter2& Getter2;
//    const ImU32 Col;
//    const double HalfHeight;
//    mutable ImVec2 UV;
//};
//
//template <class _Getter1, class _Getter2>
//struct RendererBarsLineV : RendererBase {
//    RendererBarsLineV(const _Getter1& getter1, const _Getter2& getter2, ImU32 col, double width, float weight) :
//    RendererBase(ImMin(getter1.Count, getter1.Count), 24, 8),
//    Getter1(getter1),
//    Getter2(getter2),
//    Col(col),
//    HalfWidth(width/2),
//    Weight(weight)
//    {}
//    void Init(ImDrawList& draw_list) const {
//        UV = draw_list._Data->TexUvWhitePixel;
//    }
//    IMPLOT_INLINE bool Render(ImDrawList& draw_list, const ImRect& cull_rect, int prim) const {
//        ImPlotPoint p1 = Getter1(prim);
//        ImPlotPoint p2 = Getter2(prim);
//        p1.x += HalfWidth;
//        p2.x -= HalfWidth;
//        ImVec2 P1 = this->Transformer(p1);
//        ImVec2 P2 = this->Transformer(p2);
//        float width_px = ImAbs(P1.x-P2.x);
//        if (width_px < 1.0f) {
//            P1.x += P1.x > P2.x ? (1-width_px) / 2 : (width_px-1) / 2;
//            P2.x += P2.x > P1.x ? (1-width_px) / 2 : (width_px-1) / 2;
//        }
//        ImVec2 PMin = ImMin(P1, P2);
//        ImVec2 PMax = ImMax(P1, P2);
//        if (!cull_rect.Overlaps(ImRect(PMin, PMax)))
//            return false;
//        PrimRectLine(draw_list,PMin,PMax,Weight,Col,UV);
//        return true;
//    }
//    const _Getter1& Getter1;
//    const _Getter2& Getter2;
//    const ImU32 Col;
//    const double HalfWidth;
//    const float Weight;
//    mutable ImVec2 UV;
//};
//
//template <class _Getter1, class _Getter2>
//struct RendererBarsLineH : RendererBase {
//    RendererBarsLineH(const _Getter1& getter1, const _Getter2& getter2, ImU32 col, double height, float weight) :
//    RendererBase(ImMin(getter1.Count, getter1.Count), 24, 8),
//    Getter1(getter1),
//    Getter2(getter2),
//    Col(col),
//    HalfHeight(height/2),
//    Weight(weight)
//    {}
//    void Init(ImDrawList& draw_list) const {
//        UV = draw_list._Data->TexUvWhitePixel;
//    }
//    IMPLOT_INLINE bool Render(ImDrawList& draw_list, const ImRect& cull_rect, int prim) const {
//        ImPlotPoint p1 = Getter1(prim);
//        ImPlotPoint p2 = Getter2(prim);
//        p1.y += HalfHeight;
//        p2.y -= HalfHeight;
//        ImVec2 P1 = this->Transformer(p1);
//        ImVec2 P2 = this->Transformer(p2);
//        float height_px = ImAbs(P1.y-P2.y);
//        if (height_px < 1.0f) {
//            P1.y += P1.y > P2.y ? (1-height_px) / 2 : (height_px-1) / 2;
//            P2.y += P2.y > P1.y ? (1-height_px) / 2 : (height_px-1) / 2;
//        }
//        ImVec2 PMin = ImMin(P1, P2);
//        ImVec2 PMax = ImMax(P1, P2);
//        if (!cull_rect.Overlaps(ImRect(PMin, PMax)))
//            return false;
//        PrimRectLine(draw_list,PMin,PMax,Weight,Col,UV);
//        return true;
//    }
//    const _Getter1& Getter1;
//    const _Getter2& Getter2;
//    const ImU32 Col;
//    const double HalfHeight;
//    const float Weight;
//    mutable ImVec2 UV;
//};
//
//
//template <class _Getter>
//struct RendererStairsPre : RendererBase {
//    RendererStairsPre(const _Getter& getter, ImU32 col, float weight) :
//    RendererBase(getter.Count - 1, 12, 8),
//    Getter(getter),
//    Col(col),
//    HalfWeight(ImMax(1.0f,weight)*0.5f)
//    {
//        P1 = this->Transformer(Getter(0));
//    }
//    void Init(ImDrawList& draw_list) const {
//        UV = draw_list._Data->TexUvWhitePixel;
//    }
//    IMPLOT_INLINE bool Render(ImDrawList& draw_list, const ImRect& cull_rect, int prim) const {
//        ImVec2 P2 = this->Transformer(Getter(prim + 1));
//        if (!cull_rect.Overlaps(ImRect(ImMin(P1, P2), ImMax(P1, P2)))) {
//            P1 = P2;
//            return false;
//        }
//        PrimRectFill(draw_list, ImVec2(P1.x - HalfWeight, P1.y), ImVec2(P1.x + HalfWeight, P2.y), Col, UV);
//        PrimRectFill(draw_list, ImVec2(P1.x, P2.y + HalfWeight), ImVec2(P2.x, P2.y - HalfWeight), Col, UV);
//        P1 = P2;
//        return true;
//    }
//    const _Getter& Getter;
//    const ImU32 Col;
//    mutable float HalfWeight;
//    mutable ImVec2 P1;
//    mutable ImVec2 UV;
//};
//
//template <class _Getter>
//struct RendererStairsPost : RendererBase {
//    RendererStairsPost(const _Getter& getter, ImU32 col, float weight) :
//    RendererBase(getter.Count - 1, 12, 8),
//    Getter(getter),
//    Col(col),
//    HalfWeight(ImMax(1.0f,weight) * 0.5f)
//    {
//        P1 = this->Transformer(Getter(0));
//    }
//    void Init(ImDrawList& draw_list) const {
//        UV = draw_list._Data->TexUvWhitePixel;
//    }
//    IMPLOT_INLINE bool Render(ImDrawList& draw_list, const ImRect& cull_rect, int prim) const {
//        ImVec2 P2 = this->Transformer(Getter(prim + 1));
//        if (!cull_rect.Overlaps(ImRect(ImMin(P1, P2), ImMax(P1, P2)))) {
//            P1 = P2;
//            return false;
//        }
//        PrimRectFill(draw_list, ImVec2(P1.x, P1.y + HalfWeight), ImVec2(P2.x, P1.y - HalfWeight), Col, UV);
//        PrimRectFill(draw_list, ImVec2(P2.x - HalfWeight, P2.y), ImVec2(P2.x + HalfWeight, P1.y), Col, UV);
//        P1 = P2;
//        return true;
//    }
//    const _Getter& Getter;
//    const ImU32 Col;
//    mutable float HalfWeight;
//    mutable ImVec2 P1;
//    mutable ImVec2 UV;
//};
//
//template <class _Getter>
//struct RendererStairsPreShaded : RendererBase {
//    RendererStairsPreShaded(const _Getter& getter, ImU32 col) :
//    RendererBase(getter.Count - 1, 6, 4),
//    Getter(getter),
//    Col(col)
//    {
//        P1 = this->Transformer(Getter(0));
//        Y0 = this->Transformer(ImPlotPoint(0,0)).y;
//    }
//    void Init(ImDrawList& draw_list) const {
//        UV = draw_list._Data->TexUvWhitePixel;
//    }
//    IMPLOT_INLINE bool Render(ImDrawList& draw_list, const ImRect& cull_rect, int prim) const {
//        ImVec2 P2 = this->Transformer(Getter(prim + 1));
//        ImVec2 PMin(ImMin(P1.x, P2.x), ImMin(Y0, P2.y));
//        ImVec2 PMax(ImMax(P1.x, P2.x), ImMax(Y0, P2.y));
//        if (!cull_rect.Overlaps(ImRect(PMin, PMax))) {
//            P1 = P2;
//            return false;
//        }
//        PrimRectFill(draw_list, PMin, PMax, Col, UV);
//        P1 = P2;
//        return true;
//    }
//    const _Getter& Getter;
//    const ImU32 Col;
//    float Y0;
//    mutable ImVec2 P1;
//    mutable ImVec2 UV;
//};
//
//template <class _Getter>
//struct RendererStairsPostShaded : RendererBase {
//    RendererStairsPostShaded(const _Getter& getter, ImU32 col) :
//    RendererBase(getter.Count - 1, 6, 4),
//    Getter(getter),
//    Col(col)
//    {
//        P1 = this->Transformer(Getter(0));
//        Y0 = this->Transformer(ImPlotPoint(0,0)).y;
//    }
//    void Init(ImDrawList& draw_list) const {
//        UV = draw_list._Data->TexUvWhitePixel;
//    }
//    IMPLOT_INLINE bool Render(ImDrawList& draw_list, const ImRect& cull_rect, int prim) const {
//        ImVec2 P2 = this->Transformer(Getter(prim + 1));
//        ImVec2 PMin(ImMin(P1.x, P2.x), ImMin(P1.y, Y0));
//        ImVec2 PMax(ImMax(P1.x, P2.x), ImMax(P1.y, Y0));
//        if (!cull_rect.Overlaps(ImRect(PMin, PMax))) {
//            P1 = P2;
//            return false;
//        }
//        PrimRectFill(draw_list, PMin, PMax, Col, UV);
//        P1 = P2;
//        return true;
//    }
//    const _Getter& Getter;
//    const ImU32 Col;
//    float Y0;
//    mutable ImVec2 P1;
//    mutable ImVec2 UV;
//};

//template <class _Getter1, class _Getter2>
class RendererShaded(val getter1: Getter, val getter2: Getter, val col: UInt) : RendererBase(min(getter1.count, getter2.count) - 1, 6, 5) {

    val p11: Vec2 = transformer[getter1[0]]
    val p12: Vec2 = transformer[getter2[0]]

    val uv = Vec2()
    override infix fun init(drawList: DrawList) = uv put drawList._data.texUvWhitePixel

    override fun render(drawList: DrawList, cullRect: Rect, prim: Int): Boolean {
        val p21 = transformer[getter1[prim + 1]]
        val p22 = transformer[getter2[prim + 1]]
        val rect = Rect(((p11 min p12) min p21) min p22, ((p11 max p12) max p21) max p22)
        if (!cullRect.overlaps(rect)) {
            p11 put p21
            p12 put p22
            return false
        }
        val intersect = ((p11.y > p12.y && p22.y > p21.y) || (p12.y > p11.y && p21.y > p22.y)).i
        val intersection = intersection(p11, p21, p12, p22)
        drawList.vtxBuffer.pos = drawList._vtxWritePtr
        drawList.vtxBuffer.also {
            it += p11; it += uv; it += col.toInt()
            it += p21; it += uv; it += col.toInt()
            it += intersection; it += uv; it += col.toInt()
            it += p12; it += uv; it += col.toInt()
            it += p22; it += uv; it += col.toInt()
        }
        drawList._vtxWritePtr += 5
        drawList.idxBuffer.pos = drawList._idxWritePtr
        drawList.idxBuffer.also {
            it += drawList._vtxCurrentIdx; it += drawList._vtxCurrentIdx + 1 + intersect; it += drawList._vtxCurrentIdx + 3
            it += drawList._vtxCurrentIdx + 1; it += drawList._vtxCurrentIdx + 4; it += drawList._vtxCurrentIdx + 3 - intersect
        }
        drawList._idxWritePtr += 6
        drawList._vtxCurrentIdx += 5
        p11 put p21
        p12 put p22
        return true
    }
}

//struct RectC {
//    ImPlotPoint Pos;
//    ImPlotPoint HalfSize;
//    ImU32 Color;
//};
//
//template <typename _Getter>
//struct RendererRectC : RendererBase {
//    RendererRectC(const _Getter& getter) :
//    RendererBase(getter.Count, 6, 4),
//    Getter(getter)
//    {}
//    void Init(ImDrawList& draw_list) const {
//        UV = draw_list._Data->TexUvWhitePixel;
//    }
//    IMPLOT_INLINE bool Render(ImDrawList& draw_list, const ImRect& cull_rect, int prim) const {
//        RectC rect = Getter(prim);
//        ImVec2 P1 = this->Transformer(rect.Pos.x - rect.HalfSize.x , rect.Pos.y - rect.HalfSize.y);
//        ImVec2 P2 = this->Transformer(rect.Pos.x + rect.HalfSize.x , rect.Pos.y + rect.HalfSize.y);
//        if ((rect.Color & IM_COL32_A_MASK) == 0 || !cull_rect.Overlaps(ImRect(ImMin(P1, P2), ImMax(P1, P2))))
//        return false;
//        PrimRectFill(draw_list,P1,P2,rect.Color,UV);
//        return true;
//    }
//    const _Getter& Getter;
//    mutable ImVec2 UV;
//};