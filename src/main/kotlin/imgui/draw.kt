package imgui

import gli.hasnt
import glm_.BYTES
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import glm_.glm
import glm_.i
import glm_.vec2.Vec2i
import java.util.*
import kotlin.collections.ArrayList

/** Draw callbacks for advanced uses.
NB- You most likely do NOT need to use draw callbacks just to create your own widget or customized UI rendering
(you can poke into the draw list for that)
Draw callback may be useful, for example, to:
- change your GPU render state
- render a complex 3D scene inside a UI element (without an intermediate texture/render target), etc.
The expected behavior from your rendering function is
'if (cmd.UserCallback != NULL) cmd.UserCallback(parent_list, cmd); else RenderTriangles()'  */
typealias DrawCallback = (DrawList, DrawCmd) -> Unit

/** Typically, 1 command = 1 gpu draw call (unless command is a callback)   */
class DrawCmd {

    /** Number of indices (multiple of 3) to be rendered as triangles. Vertices are stored in the callee ImDrawList's
     *  vtx_buffer[] array, indices in idx_buffer[].    */
    var elemCount = 0
    /** Clipping rectangle (x1, y1, x2, y2) */
    var clipRect = Vec4(-8192.0f, -8192.0f, 8192.0f, 8192.0f)
    /** User-provided texture ID. Set by user in ImfontAtlas::SetTexID() for fonts or passed to Image*() functions.
    Ignore if never using images or multiple fonts atlas.   */
    var textureId: Int? = null
    /** If != NULL, call the function instead of rendering the vertices. clip_rect and texture_id will be set normally. */
    var userCallback: DrawCallback? = null
//    void*           UserCallbackData;       // The draw callback code can access this.
}

typealias DrawIdx = Int // TODO check

/** Vertex layout   */
class DrawVert {

    var pos = Vec2()
    var uv = Vec2()
    var col = 0

    companion object {
        val size = 2 * Vec2.size + Int.BYTES
    }
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

    // -----------------------------------------------------------------------------------------------------------------
    // This is what you have to render
    // -----------------------------------------------------------------------------------------------------------------

    /** Commands. Typically 1 command = 1 gpu draw call.    */
    var cmdBuffer = Stack<DrawCmd>()
    /** Index buffer. Each command consume ImDrawCmd::ElemCount of those    */
    var idxBuffer = ArrayList<DrawIdx>()
    /** Vertex buffer.  */
    var vtxBuffer = ArrayList<DrawVert>()


    // -----------------------------------------------------------------------------------------------------------------
    // [Internal, used while building lists]
    // -----------------------------------------------------------------------------------------------------------------

    /** Pointer to owner window's name for debugging    */
    var _ownerName = ""
    /** Internal == VtxBuffer.Size    */
    var _vtxCurrentIdx = 0
    /** [Internal] point within VtxBuffer.Data after each add command (to avoid using the ImVector<> operators too much)    */
    var _vtxWritePtr = -1
    /** [Internal] point within IdxBuffer.Data after each add command (to avoid using the ImVector<> operators too much)    */
    var _idxWritePtr = -1

    val _clipRectStack = Stack<Vec4>()
    /** [Internal]  */
    val _textureIdStack = Stack<Int>()
    /** [Internal] current path building    */
    val _path = mutableListOf<Vec2>()
    /** [Internal] current channel number (0)   */
    var _channelsCurrent = 0
    /** [Internal] number of active channels (1+)   */
    var _channelsCount = 0
    /** [Internal] draw channels for columns API (not resized down so _ChannelsCount may be smaller than _Channels.Size)    */
    val _channels = ArrayList<DrawChannel>()


    /** Render-level scissoring. This is passed down to your render function but not used for CPU-side coarse clipping.
     *  Prefer using higher-level ImGui::PushClipRect() to affect logic (hit-testing and widget culling)    */
    fun pushClipRect(crMin: Vec2, crMax: Vec2, intersectWithCurrentClipRect: Boolean = false) {

        val cr = Vec4(crMin, crMax)
        if (intersectWithCurrentClipRect && _clipRectStack.isNotEmpty()) {
            val current = _clipRectStack.last()
            if (cr.x < current.x) cr.x = current.x
            if (cr.y < current.y) cr.y = current.y
            if (cr.z > current.z) cr.z = current.z
            if (cr.w > current.w) cr.w = current.w
        }
        cr.z = glm.max(cr.x, cr.z)
        cr.w = glm.max(cr.y, cr.w)

        _clipRectStack.push(cr)
        updateClipRect()
    }

    fun pushClipRectFullScreen() = pushClipRect(Vec2(nullClipRect.x, nullClipRect.y), Vec2(nullClipRect.z, nullClipRect.w))

    fun popClipRect() {
        assert(_clipRectStack.isNotEmpty())
        _clipRectStack.pop()
        updateClipRect()
    }

    fun pushTextureID(textureId: Int) = _textureIdStack.push(textureId).run { updateTextureID() }

    fun popTextureID() = _textureIdStack.pop().also { updateTextureID() }


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
        primWriteIdx(_vtxCurrentIdx); primWriteIdx(_vtxCurrentIdx + 1); primWriteIdx(_vtxCurrentIdx + 2)
        primWriteIdx(_vtxCurrentIdx); primWriteIdx(_vtxCurrentIdx + 2); primWriteIdx(_vtxCurrentIdx + 3)
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

//    IMGUI_API void  AddText(const ImVec2& pos, ImU32 col, const char* text_begin, const char* text_end = NULL);
//    IMGUI_API void  AddText(const ImFont* font, float font_size, const ImVec2& pos, ImU32 col, const char* text_begin, const char* text_end = NULL, float wrap_width = 0.0f, const ImVec4* cpu_fine_clip_rect = NULL);
//    IMGUI_API void  AddImage(ImTextureID user_texture_id, const ImVec2& a, const ImVec2& b, const ImVec2& uv_a = ImVec2(0,0), const ImVec2& uv_b = ImVec2(1,1), ImU32 col = 0xFFFFFFFF);
//    IMGUI_API void  AddImageQuad(ImTextureID user_texture_id, const ImVec2& a, const ImVec2& b, const ImVec2& c, const ImVec2& d, const ImVec2& uv_a = ImVec2(0,0), const ImVec2& uv_b = ImVec2(1,0), const ImVec2& uv_c = ImVec2(1,1), const ImVec2& uv_d = ImVec2(0,1), ImU32 col = 0xFFFFFFFF);
//    IMGUI_API void  AddPolyline(const ImVec2* points, const int num_points, ImU32 col, bool closed, float thickness, bool anti_aliased);
//    IMGUI_API void  AddConvexPolyFilled(const ImVec2* points, const int num_points, ImU32 col, bool anti_aliased);
//    IMGUI_API void  AddBezierCurve(const ImVec2& pos0, const ImVec2& cp0, const ImVec2& cp1, const ImVec2& pos1, ImU32 col, float thickness, int num_segments = 0);


    // -----------------------------------------------------------------------------------------------------------------
    // Stateful path API, add points then finish with PathFill() or PathStroke()
    // -----------------------------------------------------------------------------------------------------------------

    fun pathClear() = _path.clear()

    fun pathLineTo(pos: Vec2) = _path.add(pos)

    fun pathLineToMergeDuplicate(pos: Vec2) {
        if (_path.isEmpty() || _path.last() != pos) _path.add(pos)
    }

    fun pathFillConvex(col: Int) {
//        addConvexPolyFilled(_path.Data, _path.Size, col, true)
        pathClear()
    }

    /** rounding_corners_flags: 4-bits corresponding to which corner to round   */
    fun pathStroke(col: Int, closed: Boolean, thickness: Float = 1.0f) {
        TODO()
//        addPolyline(_path.Data, _path.Size, col, closed, thickness, true)
        pathClear()
    }

    fun pathArcTo(centre: Vec2, radius: Float, a_min: Float, a_max: Float, numSegments: Int = 10): Any = TODO()

    /** Use precomputed angles for a 12 steps circle    */
    fun pathArcToFast(centre: Vec2, radius: Float, a: Vec2i): Nothing = TODO()

    fun pathBezierCurveTo(p1: Vec2, p2: Vec2, p3: Vec2, numSegments: Int = 0): Nothing = TODO()

    /** rounding_corners_flags: 4-bits corresponding to which corner to round   */
    fun pathRect(rect_min: Vec2, rect_max: Vec2, rounding: Float = 0.0f, roundingCornersFlags: Int = 0xffffffff.i): Any = TODO()


    // -----------------------------------------------------------------------------------------------------------------
    // Channels
    // - Use to simulate layers. By switching channels to can render out-of-order (e.g. submit foreground primitives before background primitives)
    // - Use to minimize draw calls (e.g. if going back-and-forth between multiple non-overlapping clipping rectangles, prefer to append into separate channels then merge at the end)
    // -----------------------------------------------------------------------------------------------------------------

    fun channelsSplit(channelsCount: Int): Nothing = TODO()
    fun channelsMerge(): Nothing = TODO()
    fun channelsSetCurrent(channelIndex: Int): Nothing = TODO()

    // -----------------------------------------------------------------------------------------------------------------
    // Advanced
    // -----------------------------------------------------------------------------------------------------------------
    /** Your rendering function must check for 'UserCallback' in ImDrawCmd and call the function instead of rendering
    triangles.  */
    fun addCallback(/*ImDrawCallback callback, void* callback_data*/): Nothing = TODO()

    /** This is useful if you need to forcefully create a new draw call (to allow for dependent rendering / blending).
    Otherwise primitives are merged into the same draw-call as much as possible */
    fun addDrawCmd() {
        val drawCmd = DrawCmd()
        drawCmd.clipRect = currentClipRect
        drawCmd.textureId = currentTextureId

        assert(drawCmd.clipRect.x <= drawCmd.clipRect.z && drawCmd.clipRect.y <= drawCmd.clipRect.w)
        cmdBuffer.add(drawCmd)
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Internal helpers
    // NB: all primitives needs to be reserved via PrimReserve() beforehand!
    // -----------------------------------------------------------------------------------------------------------------
    fun clear() {
        cmdBuffer.clear()
        idxBuffer.clear()
        vtxBuffer.clear()
        _vtxCurrentIdx = 0
        _vtxWritePtr = -1
        _idxWritePtr = -1
        _clipRectStack.clear()
        _textureIdStack.clear()
        _path.clear()
        _channelsCurrent = 0
        _channelsCount = 1
        // NB: Do not clear channels so our allocations are re-used after the first frame.
    }

    fun clearFreeMemory() {
        clear()
        _channels.forEach {
            it.cmdBuffer.clear()
            it.idxBuffer.clear()
        }
        _channels.clear()
    }

    /** // NB: this can be called with negative count for removing primitives (as long as the result does not underflow)    */
    fun primReserve(idxCount: Int, vtxCount: Int) {

        cmdBuffer.last().elemCount += idxCount

        val vtxBufferSize = vtxBuffer.size
        for (i in 0 until vtxCount)
            if (vtxCount > 0)
                vtxBuffer.add(DrawVert())
            else if (vtxCount < 0)
                vtxBuffer.remove(vtxBuffer.last())
        _vtxWritePtr = vtxBufferSize

        val idxBufferSize = idxBuffer.size
        for (i in 0 until idxCount)
            if (idxCount > 0)
                idxBuffer.add(0)
            else if (idxCount < 0)
                idxBuffer.remove(idxBuffer.last())
        _idxWritePtr = idxBufferSize
    }

    /** Fully unrolled with inline call to keep our debug builds decently fast.
    Axis aligned rectangle (composed of two triangles)  */
    fun primRect(a: Vec2, c: Vec2, col: Int) {
        val b = Vec2(c.x, a.y)
        val d = Vec2(a.x, c.y)
        val uv = Vec2(Context.fontTexUvWhitePixel)
        val idx = _vtxCurrentIdx
        idxBuffer[_idxWritePtr + 0] = idx; idxBuffer[_idxWritePtr + 1] = idx + 1; idxBuffer[_idxWritePtr + 2] = idx + 2
        idxBuffer[_idxWritePtr + 3] = idx; idxBuffer[_idxWritePtr + 4] = idx + 2; idxBuffer[_idxWritePtr + 5] = idx + 3
        vtxBuffer[_vtxWritePtr + 0].pos = a; vtxBuffer[_vtxWritePtr + 0].uv = uv; vtxBuffer[_vtxWritePtr + 0].col = col
        vtxBuffer[_vtxWritePtr + 1].pos = a; vtxBuffer[_vtxWritePtr + 1].uv = uv; vtxBuffer[_vtxWritePtr + 1].col = col
        vtxBuffer[_vtxWritePtr + 2].pos = a; vtxBuffer[_vtxWritePtr + 2].uv = uv; vtxBuffer[_vtxWritePtr + 2].col = col
        vtxBuffer[_vtxWritePtr + 3].pos = a; vtxBuffer[_vtxWritePtr + 3].uv = uv; vtxBuffer[_vtxWritePtr + 3].col = col
        _vtxWritePtr += 4
        _vtxCurrentIdx += 4
        _idxWritePtr += 6
    }

    fun primRectUV(a: Vec2, c: Vec2, uvA: Vec2, uvC: Vec2, col: Int) {
        val b = Vec2(c.x, a.y)
        val d = Vec2(a.x, c.y)
        val uvB = Vec2(uvC.x, uvA.y)
        val uvD = Vec2(uvA.x, uvC.y)
        val idx = _vtxCurrentIdx
        idxBuffer[_idxWritePtr + 0] = idx; idxBuffer[_idxWritePtr + 1] = idx + 1; idxBuffer[_idxWritePtr + 2] = idx + 2
        idxBuffer[_idxWritePtr + 3] = idx; idxBuffer[_idxWritePtr + 4] = idx + 2; idxBuffer[_idxWritePtr + 5] = idx + 3
        vtxBuffer[_vtxWritePtr + 0].pos = a; vtxBuffer[_vtxWritePtr + 0].uv = uvA; vtxBuffer[_vtxWritePtr + 0].col = col
        vtxBuffer[_vtxWritePtr + 1].pos = b; vtxBuffer[_vtxWritePtr + 1].uv = uvB; vtxBuffer[_vtxWritePtr + 1].col = col
        vtxBuffer[_vtxWritePtr + 2].pos = c; vtxBuffer[_vtxWritePtr + 2].uv = uvC; vtxBuffer[_vtxWritePtr + 2].col = col
        vtxBuffer[_vtxWritePtr + 3].pos = d; vtxBuffer[_vtxWritePtr + 3].uv = uvD; vtxBuffer[_vtxWritePtr + 3].col = col
        _vtxWritePtr += 4
        _vtxCurrentIdx += 4
        _idxWritePtr += 6
    }

    fun primQuadUV(a: Vec2, b: Vec2, c: Vec2, d: Vec2, uvA: Vec2, uvB: Vec2, uvC: Vec2, uvD: Vec2, col: Int) {
        val idx = _vtxCurrentIdx
        idxBuffer[_idxWritePtr + 0] = idx; idxBuffer[_idxWritePtr + 1] = idx + 1; idxBuffer[_idxWritePtr + 2] = idx + 2
        idxBuffer[_idxWritePtr + 3] = idx; idxBuffer[_idxWritePtr + 4] = idx + 2; idxBuffer[_idxWritePtr + 5] = idx + 3
        vtxBuffer[_vtxWritePtr + 0].pos = a; vtxBuffer[_vtxWritePtr + 0].uv = uvA; vtxBuffer[_vtxWritePtr + 0].col = col
        vtxBuffer[_vtxWritePtr + 1].pos = b; vtxBuffer[_vtxWritePtr + 1].uv = uvB; vtxBuffer[_vtxWritePtr + 1].col = col
        vtxBuffer[_vtxWritePtr + 2].pos = c; vtxBuffer[_vtxWritePtr + 2].uv = uvC; vtxBuffer[_vtxWritePtr + 2].col = col
        vtxBuffer[_vtxWritePtr + 3].pos = d; vtxBuffer[_vtxWritePtr + 3].uv = uvD; vtxBuffer[_vtxWritePtr + 3].col = col
        _vtxWritePtr += 4
        _vtxCurrentIdx += 4
        _idxWritePtr += 6
    }

    fun primWriteVtx(pos: Vec2, uv: Vec2, col: Int) {
        val vtx = vtxBuffer[_vtxWritePtr]
        vtx.pos = pos
        vtx.uv = uv
        vtx.col = col
        _vtxWritePtr++
        _vtxCurrentIdx++
    }

    fun primWriteIdx(idx: DrawIdx) {
        _idxWritePtr = idx
        _idxWritePtr++
    }

    fun primVtx(pos: Vec2, uv: Vec2, col: Int) {
        primWriteIdx(_vtxCurrentIdx)
        primWriteVtx(pos, uv, col)
    }

    /** Our scheme may appears a bit unusual, basically we want the most-common calls AddLine AddRect etc. to not have
    to perform any check so we always have a command ready in the stack.
    The cost of figuring out if a new command has to be added or if we can merge is paid in those Update**
    functions only. */
    fun updateClipRect() {
        // If current command is used with different settings we need to add a new command
        val currCmd = if (cmdBuffer.isNotEmpty()) cmdBuffer.last() else null
        if (currCmd == null || (currCmd.elemCount != 0 && currCmd.clipRect != currentClipRect) || currCmd.userCallback != null) {
            addDrawCmd()
            return
        }

        // Try to merge with previous command if it matches, else use current command
        val prevCmd = if (cmdBuffer.size > 1) cmdBuffer[cmdBuffer.lastIndex - 1] else null
        if (currCmd.elemCount == 0 && prevCmd != null && prevCmd.clipRect == currentClipRect && prevCmd.textureId == currentTextureId!!
                && prevCmd.userCallback == null)
            cmdBuffer.pop()
        else
            currCmd.clipRect = currentClipRect
    }

    fun updateTextureID() {

        // If current command is used with different settings we need to add a new command
        val currCmd = if (cmdBuffer.isNotEmpty()) cmdBuffer.last() else null
        if (currCmd == null || (currCmd.elemCount != 0 && currCmd.textureId != currentTextureId!!) || currCmd.userCallback != null) {
            addDrawCmd()
            return
        }

        // Try to merge with previous command if it matches, else use current command
        val prevCmd = if (cmdBuffer.size > 1) cmdBuffer[cmdBuffer.lastIndex - 1] else null
        if (prevCmd != null && prevCmd.textureId == currentTextureId!! && prevCmd.clipRect == currentClipRect &&
                prevCmd.userCallback == null)
            cmdBuffer.pop()
        else
            currCmd.textureId = currentTextureId!!
    }


    // Macros
    val currentClipRect get() = if (_clipRectStack.isNotEmpty()) _clipRectStack.last()!! else nullClipRect
    val currentTextureId get() = if (_textureIdStack.isNotEmpty()) _textureIdStack.last()!! else null

    companion object {
        /** Large values that are easy to encode in a few bits+shift    */
        val nullClipRect = Vec4(-8192.0f, -8192.0f, +8192.0f, +8192.0f)
    }
}


/** All draw data to render an ImGui frame  */
class DrawData {

    /** Only valid after Render() is called and before the next NewFrame() is called.   */
    var valid = false
    val cmdLists = ArrayList<DrawList>()
    var cmdListsCount = 0   // TODO remove?
    /** For convenience, sum of all cmd_lists vtx_buffer.Size   */
    var totalVtxCount = 0
    /** For convenience, sum of all cmd_lists idx_buffer.Size   */
    var totalIdxCount = 0

    // Functions

    /** For backward compatibility: convert all buffers from indexed to de-indexed, in case you cannot render indexed.
     *  Note: this is slow and most likely a waste of resources. Always prefer indexed rendering!   */
    fun deIndexAllBuffers() {
        val newVtxBuffer = mutableListOf<DrawVert>()
        totalVtxCount = 0
        totalIdxCount = 0
        cmdLists.filter { it.idxBuffer.isNotEmpty() }.forEach { cmdList ->
            for (j in cmdList.idxBuffer.indices)
                newVtxBuffer[j] = cmdList.vtxBuffer[cmdList.idxBuffer[j]]
            cmdList.vtxBuffer.clear()
            cmdList.vtxBuffer.addAll(newVtxBuffer)
            cmdList.idxBuffer.clear()
            totalVtxCount += cmdList.vtxBuffer.size
        }
    }

    /** Helper to scale the ClipRect field of each ImDrawCmd. Use if your final output buffer is at a different scale
     *  than ImGui expects, or if there is a difference between your window resolution and framebuffer resolution.  */
    fun scaleClipRects(scale: Vec2) {
        cmdLists.forEach {
            it.cmdBuffer.forEach { cmd ->
                cmd.clipRect.times_(scale.x, scale.y, scale.x, scale.y)
            }
        }
    }
}