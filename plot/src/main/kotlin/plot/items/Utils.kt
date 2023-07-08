package plot.items

import glm_.i
import glm_.vec2.Vec2
import imgui.classes.DrawList
import imgui.has
import imgui.internal.sections.DrawListFlag
import kool.pos
import uno.kotlin.plusAssign

//-----------------------------------------------------------------------------
// [SECTION] Utils
//-----------------------------------------------------------------------------

// Calc maximum index size of ImDrawIdx
//template <typename T>
//struct MaxIdx { static const unsigned int Value; };
//template <> const unsigned int MaxIdx<unsigned short>::Value = 65535;
//template <> const unsigned int MaxIdx<unsigned int>::Value   = 4294967295;

/** @return [JVM] the new halfWeight */
fun getLineRenderProps(drawList: DrawList, halfWeight: Float, texUv0: Vec2, texUv1: Vec2): Float {
    val aa = drawList.flags has DrawListFlag.AntiAliasedLines && drawList.flags has DrawListFlag.AntiAliasedLinesUseTex
    return when {
        aa -> {
            val texUvs = drawList._data.texUvLines[(halfWeight * 2).i]
            texUv0.put(texUvs.x, texUvs.y)
            texUv1.put(texUvs.z, texUvs.w)
            halfWeight + 1
        }
        else -> {
            texUv1 put drawList._data.texUvWhitePixel
            texUv0 put texUv1
            halfWeight
        }
    }
}

fun primLine(drawList: DrawList, p1: Vec2, p2: Vec2, halfWeight: Float, col: UInt, texUV0: Vec2, texUV1: Vec2) {
    var dx = p2.x - p1.x
    var dy = p2.y - p1.y
//    IMPLOT_NORMALIZE2F_OVER_ZERO(dx, dy)
    run {
        val d2 = dx * dx + dy * dy
        if (d2 > 0f) {
            val invLen = d2.invSqrt
            dx *= invLen
            dy *= invLen
        }
    }
    dx *= halfWeight
    dy *= halfWeight
    drawList.vtxBuffer.pos = drawList._vtxWritePtr
    drawList.vtxBuffer.also {
        it += p1.x + dy; it += p1.y - dx; it += texUV0; it += col.toInt()
        it += p2.x + dy; it += p2.y - dx; it += texUV0; it += col.toInt()
        it += p2.x - dy; it += p2.y + dx; it += texUV1; it += col.toInt()
        it += p1.x - dy; it += p1.y + dx; it += texUV1; it += col.toInt()
    }
    drawList._vtxWritePtr += 4
    drawList.idxBuffer.pos = drawList._idxWritePtr
    drawList.idxBuffer.also {
        it += drawList._vtxCurrentIdx; it += drawList._vtxCurrentIdx + 1; it += drawList._vtxCurrentIdx + 2
        it += drawList._vtxCurrentIdx; it += drawList._vtxCurrentIdx + 2; it += drawList._vtxCurrentIdx + 3
    }
    drawList._idxWritePtr += 6
    drawList._vtxCurrentIdx += 4
}

//IMPLOT_INLINE void PrimRectFill(ImDrawList& draw_list, const ImVec2& Pmin, const ImVec2& Pmax, ImU32 col, const ImVec2& uv) {
//    draw_list._VtxWritePtr[0].pos   = Pmin;
//    draw_list._VtxWritePtr[0].uv    = uv;
//    draw_list._VtxWritePtr[0].col   = col;
//    draw_list._VtxWritePtr[1].pos   = Pmax;
//    draw_list._VtxWritePtr[1].uv    = uv;
//    draw_list._VtxWritePtr[1].col   = col;
//    draw_list._VtxWritePtr[2].pos.x = Pmin.x;
//    draw_list._VtxWritePtr[2].pos.y = Pmax.y;
//    draw_list._VtxWritePtr[2].uv    = uv;
//    draw_list._VtxWritePtr[2].col   = col;
//    draw_list._VtxWritePtr[3].pos.x = Pmax.x;
//    draw_list._VtxWritePtr[3].pos.y = Pmin.y;
//    draw_list._VtxWritePtr[3].uv    = uv;
//    draw_list._VtxWritePtr[3].col   = col;
//    draw_list._VtxWritePtr += 4;
//    draw_list._IdxWritePtr[0] = (ImDrawIdx)(draw_list._VtxCurrentIdx);
//    draw_list._IdxWritePtr[1] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 1);
//    draw_list._IdxWritePtr[2] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 2);
//    draw_list._IdxWritePtr[3] = (ImDrawIdx)(draw_list._VtxCurrentIdx);
//    draw_list._IdxWritePtr[4] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 1);
//    draw_list._IdxWritePtr[5] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 3);
//    draw_list._IdxWritePtr += 6;
//    draw_list._VtxCurrentIdx += 4;
//}
//
//IMPLOT_INLINE void PrimRectLine(ImDrawList& draw_list, const ImVec2& Pmin, const ImVec2& Pmax, float weight, ImU32 col, const ImVec2& uv) {
//
//    draw_list._VtxWritePtr[0].pos.x = Pmin.x;
//    draw_list._VtxWritePtr[0].pos.y = Pmin.y;
//    draw_list._VtxWritePtr[0].uv    = uv;
//    draw_list._VtxWritePtr[0].col   = col;
//
//    draw_list._VtxWritePtr[1].pos.x = Pmin.x;
//    draw_list._VtxWritePtr[1].pos.y = Pmax.y;
//    draw_list._VtxWritePtr[1].uv    = uv;
//    draw_list._VtxWritePtr[1].col   = col;
//
//    draw_list._VtxWritePtr[2].pos.x = Pmax.x;
//    draw_list._VtxWritePtr[2].pos.y = Pmax.y;
//    draw_list._VtxWritePtr[2].uv    = uv;
//    draw_list._VtxWritePtr[2].col   = col;
//
//    draw_list._VtxWritePtr[3].pos.x = Pmax.x;
//    draw_list._VtxWritePtr[3].pos.y = Pmin.y;
//    draw_list._VtxWritePtr[3].uv    = uv;
//    draw_list._VtxWritePtr[3].col   = col;
//
//    draw_list._VtxWritePtr[4].pos.x = Pmin.x + weight;
//    draw_list._VtxWritePtr[4].pos.y = Pmin.y + weight;
//    draw_list._VtxWritePtr[4].uv    = uv;
//    draw_list._VtxWritePtr[4].col   = col;
//
//    draw_list._VtxWritePtr[5].pos.x = Pmin.x + weight;
//    draw_list._VtxWritePtr[5].pos.y = Pmax.y - weight;
//    draw_list._VtxWritePtr[5].uv    = uv;
//    draw_list._VtxWritePtr[5].col   = col;
//
//    draw_list._VtxWritePtr[6].pos.x = Pmax.x - weight;
//    draw_list._VtxWritePtr[6].pos.y = Pmax.y - weight;
//    draw_list._VtxWritePtr[6].uv    = uv;
//    draw_list._VtxWritePtr[6].col   = col;
//
//    draw_list._VtxWritePtr[7].pos.x = Pmax.x - weight;
//    draw_list._VtxWritePtr[7].pos.y = Pmin.y + weight;
//    draw_list._VtxWritePtr[7].uv    = uv;
//    draw_list._VtxWritePtr[7].col   = col;
//
//    draw_list._VtxWritePtr += 8;
//
//    draw_list._IdxWritePtr[0] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 0);
//    draw_list._IdxWritePtr[1] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 1);
//    draw_list._IdxWritePtr[2] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 5);
//    draw_list._IdxWritePtr += 3;
//
//    draw_list._IdxWritePtr[0] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 0);
//    draw_list._IdxWritePtr[1] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 5);
//    draw_list._IdxWritePtr[2] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 4);
//    draw_list._IdxWritePtr += 3;
//
//    draw_list._IdxWritePtr[0] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 1);
//    draw_list._IdxWritePtr[1] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 2);
//    draw_list._IdxWritePtr[2] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 6);
//    draw_list._IdxWritePtr += 3;
//
//    draw_list._IdxWritePtr[0] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 1);
//    draw_list._IdxWritePtr[1] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 6);
//    draw_list._IdxWritePtr[2] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 5);
//    draw_list._IdxWritePtr += 3;
//
//    draw_list._IdxWritePtr[0] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 2);
//    draw_list._IdxWritePtr[1] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 3);
//    draw_list._IdxWritePtr[2] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 7);
//    draw_list._IdxWritePtr += 3;
//
//    draw_list._IdxWritePtr[0] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 2);
//    draw_list._IdxWritePtr[1] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 7);
//    draw_list._IdxWritePtr[2] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 6);
//    draw_list._IdxWritePtr += 3;
//
//    draw_list._IdxWritePtr[0] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 3);
//    draw_list._IdxWritePtr[1] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 0);
//    draw_list._IdxWritePtr[2] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 4);
//    draw_list._IdxWritePtr += 3;
//
//    draw_list._IdxWritePtr[0] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 3);
//    draw_list._IdxWritePtr[1] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 4);
//    draw_list._IdxWritePtr[2] = (ImDrawIdx)(draw_list._VtxCurrentIdx + 7);
//    draw_list._IdxWritePtr += 3;
//
//    draw_list._VtxCurrentIdx += 8;
//}