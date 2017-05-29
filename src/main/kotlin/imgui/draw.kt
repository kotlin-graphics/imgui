package imgui

import gli.hasnt
import glm.vec2.Vec2
import glm.vec4.Vec4
import glm.glm
import glm.i
import glm.s
import java.util.*

/** Large values that are easy to encode in a few bits+shift    */
val nullClipRect = Vec4(-8192.0f, -8192.0f, +8192.0f, +8192.0f)

/** Typically, 1 command = 1 gpu draw call (unless command is a callback)   */
class DrawCmd {

    /** Number of indices (multiple of 3) to be rendered as triangles. Vertices are stored in the callee ImDrawList's
     *  vtx_buffer[] array, indices in idx_buffer[].    */
    var elemCount = 0
    /** Clipping rectangle (x1, y1, x2, y2) */
    var clipRect = Vec4(-8192.0f, -8192.0f, 8192.0f, 8192.0f)
//    ImTextureID     TextureId;              // User-provided texture ID. Set by user in ImfontAtlas::SetTexID() for fonts or passed to Image*() functions. Ignore if never using images or multiple fonts atlas.
//    ImDrawCallback  UserCallback;           // If != NULL, call the function instead of rendering the vertices. clip_rect and texture_id will be set normally.
//    void*           UserCallbackData;       // The draw callback code can access this.
}

typealias DrawIdx = Short

/** Vertex layout   */
class DrawVert {

    var pos = Vec2()
    var uv = Vec2()
    var col = 0
}

/** Draw channels are used by the Columns API to "split" the render list into different channels while building, so
 *  items of each column can be batched together.
 *  You can also use them to simulate drawing layers and submit primitives in a different order than how they will be
 *  rendered.   */
class DrawChannel {

    var cmdBuffer = mutableListOf<DrawCmd>()
    var idxBuffer = mutableListOf<DrawIdx>()
}

/** Draw command list
 *  This is the low-level list of polygons that ImGui functions are filling. At the end of the frame, all command lists
 *  are passed to your ImGuiIO::RenderDrawListFn function for rendering.
 *  At the moment, each ImGui window contains its own ImDrawList but they could potentially be merged in the future.
 *  If you want to add custom rendering within a window, you can use ImGui::GetWindowDrawList() to access the current
 *  draw list and add your own primitives.
 *  You can interleave normal ImGui:: calls and adding primitives to the current draw list.
 *  All positions are in screen coordinates (0,0=top-left, 1 pixel per unit). Primitives are always added to the list
 *  and not culled (culling is done at render time and at a higher-level by ImGui:: functions). */
class DrawList {

    // This is what you have to render

    /** Commands. Typically 1 command = 1 gpu draw call.    */
    var cmdBuffer = mutableListOf<DrawCmd>()
    /** Index buffer. Each command consume ImDrawCmd::ElemCount of those    */
    var idxBuffer = mutableListOf<DrawIdx>()
    /** Vertex buffer.  */
    var vtxBuffer = mutableListOf<DrawVert>()

    // [Internal, used while building lists]
//    const char*             _OwnerName;         // Pointer to owner window's name for debugging
    /** [Internal] == VtxBuffer.Size    */
    var _VtxCurrentIdx = 0
    /** [Internal] point within VtxBuffer.Data after each add command (to avoid using the ImVector<> operators too much)    */
    var _VtxWritePtr = 0
    /** [Internal] point within IdxBuffer.Data after each add command (to avoid using the ImVector<> operators too much)    */
    var _IdxWritePtr: DrawIdx = 0
    val _ClipRectStack = Stack<Vec4>()
//    ImVector<ImTextureID>   _TextureIdStack;    // [Internal]
    /** [Internal] current path building    */
    val _Path = mutableListOf<Vec2>()
//    int                     _ChannelsCurrent;   // [Internal] current channel number (0)
//    int                     _ChannelsCount;     // [Internal] number of active channels (1+)
//    ImVector<ImDrawChannel> _Channels;          // [Internal] draw channels for columns API (not resized down so _ChannelsCount may be smaller than _Channels.Size)

    /** Render-level scissoring. This is passed down to your render function but not used for CPU-side coarse clipping.
     *  Prefer using higher-level ImGui::PushClipRect() to affect logic (hit-testing and widget culling)    */
    fun pushClipRect(crMin: Vec2, crMax: Vec2, intersectWithCurrentClipRect: Boolean = false) {

        val cr = Vec4(crMin, crMax)
        if (intersectWithCurrentClipRect && _ClipRectStack.isNotEmpty()) {
            val current = _ClipRectStack.last()
            if (cr.x < current.x) cr.x = current.x
            if (cr.y < current.y) cr.y = current.y
            if (cr.z > current.z) cr.z = current.z
            if (cr.w > current.w) cr.w = current.w
        }
        cr.z = glm.max(cr.x, cr.z)
        cr.w = glm.max(cr.y, cr.w)

        _ClipRectStack.push(cr)
        updateClipRect()
    }

    fun pushClipRectFullScreen() = pushClipRect(Vec2(nullClipRect.x, nullClipRect.y), Vec2(nullClipRect.z, nullClipRect.w))

    fun popClipRect() {
        assert(_ClipRectStack.isNotEmpty())
        _ClipRectStack.pop()
        updateClipRect()
    }


    // -----------------------------------------------------------------------------------------------------------------
    // Primitives
    // -----------------------------------------------------------------------------------------------------------------

    fun addLine(a: Vec2, b: Vec2, col: Int, thickness: Float = 1f) {

        if (col hasnt IM_COL32_A_MASK) return
        pathLineTo(a + Vec2(0.5f))
        pathLineTo(b + Vec2(0.5f))
        pathStroke(col, false, thickness)
    }

    /** we don't render 1 px sized rectangles properly.
     * @param a: upper-left
     * @param b: b: lower-right */
    fun addRect(a: Vec2, b: Vec2, col: Int, rounding: Float = 0f, roundingCornersFlags: Int = 0xffffffff.i, thickness: Float = 1f) {
        if (col hasnt IM_COL32_A_MASK) return
        pathRect(a + Vec2(0.5f), b - Vec2(0.5f), rounding, roundingCornersFlags)
        pathStroke(col, true, thickness)
    }

    fun addRectFilled(a: Vec2, b: Vec2, col: Int, rounding: Float = 0f, roundingCornersFlags: Int = 0xffffffff.i) {
        if (col hasnt IM_COL32_A_MASK) return
        if (rounding > 0.0f) {
            pathRect(a, b, rounding, roundingCornersFlags)
            pathFillConvex(col)
        } else {
            primReserve(6, 4)
            primRect(a, b, col)
        }
    }

    fun addRectFilledMultiColor(a: Vec2, c: Vec2, colUprLeft: Int, colUprRight: Int, colBotRight: Int, colBotLeft: Int) {

        if ((colUprLeft or colUprRight or colBotRight or colBotLeft) hasnt IM_COL32_A_MASK) return

        val uv = Context.fontTexUvWhitePixel
        primReserve(6, 4)
        primWriteIdx(_VtxCurrentIdx.s); primWriteIdx((_VtxCurrentIdx + 1).s); primWriteIdx((_VtxCurrentIdx + 2).s)
        primWriteIdx(_VtxCurrentIdx.s); primWriteIdx((_VtxCurrentIdx + 2).s); primWriteIdx((_VtxCurrentIdx + 3).s)
        primWriteVtx(a, uv, colUprLeft)
        primWriteVtx(Vec2(c.x, a.y), uv, colUprRight)
        primWriteVtx(c, uv, colBotRight)
        primWriteVtx(Vec2(a.x, c.y), uv, colBotLeft)
    }

    fun addQuad(a: Vec2, b: Vec2, c: Vec2, d: Vec2, col: Int, thickness: Float = 1f) {

        if (col hasnt IM_COL32_A_MASK) return

        pathLineTo(a)
        pathLineTo(b)
        pathLineTo(c)
        pathLineTo(d)
        pathStroke(col, true, thickness)
    }

    fun addQuadFilled(a: Vec2, b: Vec2, c: Vec2, d: Vec2, col: Int) {

        if (col hasnt IM_COL32_A_MASK) return

        pathLineTo(a)
        pathLineTo(b)
        pathLineTo(c)
        pathLineTo(d)
        pathFillConvex(col)
    }

    fun addTriangle(a: Vec2, b: Vec2, c: Vec2, col: Int, thickness: Float = 1f) {

        if (col hasnt IM_COL32_A_MASK) return

        pathLineTo(a)
        pathLineTo(b)
        pathLineTo(c)
        pathStroke(col, true, thickness)
    }

    fun addTriangleFilled(a: Vec2, b: Vec2, c: Vec2, col: Int) {

        if (col hasnt IM_COL32_A_MASK) return

        pathLineTo(a)
        pathLineTo(b)
        pathLineTo(c)
        pathFillConvex(col)
    }

    fun addCircle(centre: Vec2, radius: Float, col: Int, numSegments: Int = 12, thickness: Float = 1f) {

        if (col hasnt IM_COL32_A_MASK) return

        val aMax = glm.PIf * 2.0f * (numSegments - 1.0f) / numSegments
        pathArcTo(centre, radius - 0.5f, 0.0f, aMax, numSegments)
        pathStroke(col, true, thickness)
    }

    fun addCircleFilled(centre: Vec2, radius: Float, col: Int, numSegments: Int = 12) {

        if (col hasnt IM_COL32_A_MASK) return

        val aMax = glm.PIf * 2.0f * (numSegments - 1.0f) / numSegments
        pathArcTo(centre, radius, 0.0f, aMax, numSegments)
        pathFillConvex(col)
    }

//    void ImDrawList::AddBezierCurve(const ImVec2& pos0, const ImVec2& cp0, const ImVec2& cp1, const ImVec2& pos1, ImU32 col, float thickness, int num_segments)
//    {
//        if ((col & IM_COL32_A_MASK) == 0)
//        return;
//
//        PathLineTo(pos0);
//        PathBezierCurveTo(cp0, cp1, pos1, num_segments);
//        PathStroke(col, false, thickness);
//    }


    fun updateClipRect(): Any = TODO()

    // Stateful path API, add points then finish with PathFill() or PathStroke()
    fun pathClear() = _Path.clear()

    fun pathLineTo(pos: Vec2) = _Path.add(pos)
    fun pathLineToMergeDuplicate(pos: Vec2) {
        if (_Path.isEmpty() || _Path.last() != pos) _Path.add(pos)
    }

    fun pathFillConvex(col: Int) {
//        addConvexPolyFilled(_Path.Data, _Path.Size, col, true)
        pathClear()
    }

    /** rounding_corners_flags: 4-bits corresponding to which corner to round   */
    fun pathStroke(col: Int, closed: Boolean, thickness: Float = 1.0f) {
//        addPolyline(_Path.Data, _Path.Size, col, closed, thickness, true)
        pathClear()
    }

    fun pathArcTo(centre: Vec2, radius: Float, a_min: Float, a_max: Float, numSegments: Int = 10): Any = TODO()

    /** rounding_corners_flags: 4-bits corresponding to which corner to round   */
    fun pathRect(rect_min: Vec2, rect_max: Vec2, rounding: Float = 0.0f, roundingCornersFlags: Int = 0xffffffff.i): Any = TODO()

    fun primReserve(idxCount: Int, vtxCount: Int): Any = TODO()

    /** Axis aligned rectangle (composed of two triangles)  */
    fun primRect(a: Vec2, b: Vec2, col: Int): Any = TODO()

    fun primWriteIdx(idx: DrawIdx) {
        _IdxWritePtr = idx
        _IdxWritePtr++
    }

    fun primWriteVtx(pos: Vec2, uv: Vec2, col: Int) {
        val vtx = vtxBuffer[_VtxWritePtr]
        vtx.pos = pos
        vtx.uv = uv
        vtx.col = col
        _VtxWritePtr++
        _VtxCurrentIdx++
    }

    fun primVtx(pos: Vec2, uv: Vec2, col: Int) {
        primWriteIdx(_VtxCurrentIdx.s); primWriteVtx(pos, uv, col); }
}