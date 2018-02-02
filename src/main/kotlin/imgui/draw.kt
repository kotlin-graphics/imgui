package imgui

import gli_.hasnt
import glm_.BYTES
import glm_.f
import glm_.func.common.max
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.ImGui.shadeVertsLinearUV
import imgui.internal.*
import java.util.*
import kotlin.collections.ArrayList
import imgui.Context as g
import imgui.internal.DrawCornerFlags as Dcf

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

    constructor()

    constructor(drawCmd: DrawCmd) {
        elemCount = drawCmd.elemCount
        clipRect put drawCmd.clipRect
        textureId = drawCmd.textureId
        userCallback = drawCmd.userCallback
    }

    /** Number of indices (multiple of 3) to be rendered as triangles. Vertices are stored in the callee ImDrawList's
     *  vtx_buffer[] array, indices in idx_buffer[].    */
    var elemCount = 0
    /** Clipping rectangle (x1, y1, x2, y2) */
    var clipRect = Vec4()
    /** User-provided texture ID. Set by user in ImfontAtlas::SetTexID() for fonts or passed to Image*() functions.
    Ignore if never using images or multiple fonts atlas.   */
    var textureId: Int? = null
    /** If != NULL, call the function instead of rendering the vertices. clip_rect and texture_id will be set normally. */
    var userCallback: DrawCallback? = null
//    void*           UserCallbackData;       // The draw callback code can access this.

    infix fun put(drawCmd: DrawCmd) {
        elemCount = drawCmd.elemCount
        clipRect put drawCmd.clipRect
        textureId = drawCmd.textureId
        userCallback = drawCmd.userCallback
    }
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

    override fun toString() = "pos: $pos, uv: $uv, col: $col"
}

/** Draw channels are used by the Columns API to "split" the render list into different channels while building, so
 *  items of each column can be batched together.
 *  You can also use them to simulate drawing layers and submit primitives in a different order than how they will be
 *  rendered.   */
class DrawChannel {

    var cmdBuffer = Stack<DrawCmd>()
    var idxBuffer = Stack<DrawIdx>()

    fun clear() {
        cmdBuffer.clear()
        idxBuffer.clear()
    }
}


/** Draw command list
 *  This is the low-level list of polygons that ImGui functions are filling. At the end of the frame, all command lists
 *  are passed to your IO.renderDrawListFn function for rendering.
 *  Each ImGui window contains its own DrawList. You can use ImGui::windowDrawList to access the current
 *  window draw list and draw custom primitives.
 *  You can interleave normal ImGui:: calls and adding primitives to the current draw list.
 *  All positions are in screen coordinates (0,0=top-left, 1 pixel per unit).
 *  Important: Primitives are always added to the list and not culled (culling is done at render time and
 *  at a higher-level by ImGui::functions), if you use this API a lot consider coarse culling your drawn objects.   */
class DrawList(sharedData: DrawListSharedData?) {

    // -----------------------------------------------------------------------------------------------------------------
    // This is what you have to render
    // -----------------------------------------------------------------------------------------------------------------

    /** Draw commands. Typically 1 command = 1 GPU draw call, unless the command is a callback.    */
    var cmdBuffer = Stack<DrawCmd>()
    /** Index buffer. Each command consume ImDrawCmd::ElemCount of those    */
    var idxBuffer = Stack<DrawIdx>()
    /** Vertex buffer.  */
    var vtxBuffer = ArrayList<DrawVert>()


    // -----------------------------------------------------------------------------------------------------------------
    // [Internal, used while building lists]
    // -----------------------------------------------------------------------------------------------------------------

    /** Flags, you may poke into these to adjust anti-aliasing settings per-primitive. */
    var flags = 0
    /** Pointer to shared draw data (you can use ImGui::drawListSharedData to get the one from current ImGui context) */
    var _data: DrawListSharedData = sharedData ?: DrawListSharedData()
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
    val _path = ArrayList<Vec2>()
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
        cr.z = cr.x max cr.z
        cr.w = cr.y max cr.w

        _clipRectStack.push(cr)
        updateClipRect()
    }

    fun pushClipRectFullScreen() = pushClipRect(Vec2(_data.clipRectFullscreen), Vec2(_data.clipRectFullscreen.z, _data.clipRectFullscreen.w))

    fun popClipRect() {
        assert(_clipRectStack.isNotEmpty())
        _clipRectStack.pop()
        updateClipRect()
    }

    fun pushTextureId(textureId: Int) = _textureIdStack.push(textureId).run { updateTextureID() }

    fun popTextureId() = _textureIdStack.pop().also { updateTextureID() }


    // -----------------------------------------------------------------------------------------------------------------
    // Primitives
    // -----------------------------------------------------------------------------------------------------------------

    fun addLine(a: Vec2, b: Vec2, col: Int, thickness: Float = 1f) {
        if (col hasnt COL32_A_MASK) return
        pathLineTo(a + Vec2(0.5f))
        pathLineTo(b + Vec2(0.5f))
        pathStroke(col, false, thickness)
    }

    /** we don't render 1 px sized rectangles properly.
     * @param a: upper-left
     * @param b: b: lower-right */
    fun addRect(a: Vec2, b: Vec2, col: Int, rounding: Float = 0f, roundingCornersFlags: Int = Dcf.All.i, thickness: Float = 1f) {
        if (col hasnt COL32_A_MASK) return
        pathRect(a + Vec2(0.5f), b - Vec2(0.5f), rounding, roundingCornersFlags)
        pathStroke(col, true, thickness)
    }

    fun addRectFilled(a: Vec2, b: Vec2, col: Int, rounding: Float = 0f, roundingCornersFlags: Int = Dcf.All.i) {
        if (col hasnt COL32_A_MASK) return
        if (rounding > 0f) {
            pathRect(a, b, rounding, roundingCornersFlags)
            pathFillConvex(col)
        } else {
            primReserve(6, 4)
            primRect(a, b, col)
        }
    }

    fun addRectFilledMultiColor(a: Vec2, c: Vec2, colUprLeft: Int, colUprRight: Int, colBotRight: Int, colBotLeft: Int) {

        if ((colUprLeft or colUprRight or colBotRight or colBotLeft) hasnt COL32_A_MASK) return

        val uv = _data.texUvWhitePixel
        primReserve(6, 4)
        primWriteIdx(_vtxCurrentIdx); primWriteIdx(_vtxCurrentIdx + 1); primWriteIdx(_vtxCurrentIdx + 2)
        primWriteIdx(_vtxCurrentIdx); primWriteIdx(_vtxCurrentIdx + 2); primWriteIdx(_vtxCurrentIdx + 3)
        primWriteVtx(a, uv, colUprLeft)
        primWriteVtx(Vec2(c.x, a.y), uv, colUprRight)
        primWriteVtx(c, uv, colBotRight)
        primWriteVtx(Vec2(a.x, c.y), uv, colBotLeft)
    }

    fun addQuad(a: Vec2, b: Vec2, c: Vec2, d: Vec2, col: Int, thickness: Float = 1f) {

        if (col hasnt COL32_A_MASK) return

        pathLineTo(a)
        pathLineTo(b)
        pathLineTo(c)
        pathLineTo(d)
        pathStroke(col, true, thickness)
    }

    fun addQuadFilled(a: Vec2, b: Vec2, c: Vec2, d: Vec2, col: Int) {

        if (col hasnt COL32_A_MASK) return

        pathLineTo(a)
        pathLineTo(b)
        pathLineTo(c)
        pathLineTo(d)
        pathFillConvex(col)
    }

    fun addTriangle(a: Vec2, b: Vec2, c: Vec2, col: Int, thickness: Float = 1f) {

        if (col hasnt COL32_A_MASK) return

        pathLineTo(a)
        pathLineTo(b)
        pathLineTo(c)
        pathStroke(col, true, thickness)
    }

    fun addTriangleFilled(a: Vec2, b: Vec2, c: Vec2, col: Int) {

        if (col hasnt COL32_A_MASK) return

        pathLineTo(a)
        pathLineTo(b)
        pathLineTo(c)
        pathFillConvex(col)
    }

    fun addCircle(centre: Vec2, radius: Float, col: Int, numSegments: Int = 12, thickness: Float = 1f) {

        if (col hasnt COL32_A_MASK) return

        val aMax = glm.PIf * 2.0f * (numSegments - 1.0f) / numSegments
        pathArcTo(centre, radius - 0.5f, 0.0f, aMax, numSegments)
        pathStroke(col, true, thickness)
    }

    fun addCircleFilled(centre: Vec2, radius: Float, col: Int, numSegments: Int = 12) {

        if (col hasnt COL32_A_MASK) return

        val aMax = glm.PIf * 2.0f * (numSegments - 1.0f) / numSegments
        pathArcTo(centre, radius, 0.0f, aMax, numSegments)
        pathFillConvex(col)
    }

    fun addText(pos: Vec2, col: Int, text: CharArray, textEnd: Int = text.size) = addText(g.font, g.fontSize, pos, col, text, textEnd)

    fun addText(font: Font?, fontSize: Float, pos: Vec2, col: Int, text: CharArray, textEnd: Int = text.size, wrapWidth: Float = 0f,
                cpuFineClipRect: Vec4? = null) {

        if ((col and COL32_A_MASK) == 0) return

        var textEnd = textEnd
        if (textEnd == 0)
            textEnd = text.strlen
        if (textEnd == 0)
            return

        // Pull default font/size from the shared ImDrawListSharedData instance
        val font = font ?: _data.font!!
        val fontSize = if (fontSize == 0f) _data.fontSize else fontSize

        assert(font.containerAtlas.texId == _textureIdStack.last())  // Use high-level ImGui::PushFont() or low-level ImDrawList::PushTextureId() to change font.

        val clipRect = Vec4(_clipRectStack.last())
        cpuFineClipRect?.let {
            clipRect.x = glm.max(clipRect.x, cpuFineClipRect.x)
            clipRect.y = glm.max(clipRect.y, cpuFineClipRect.y)
            clipRect.z = glm.min(clipRect.z, cpuFineClipRect.z)
            clipRect.w = glm.min(clipRect.w, cpuFineClipRect.w)
        }
        font.renderText(this, fontSize, pos, col, clipRect, text, textEnd, wrapWidth, cpuFineClipRect != null)
    }

    fun addImage(userTextureId: Int, a: Vec2, b: Vec2, uvA: Vec2 = Vec2(0), uvB: Vec2 = Vec2(1), col: Int = 0xFFFFFFFF.i) {

        if (col hasnt COL32_A_MASK) return

        val pushTextureId = _textureIdStack.isEmpty() || userTextureId != _textureIdStack.last()
        if (pushTextureId) pushTextureId(userTextureId)

        primReserve(6, 4)
        primRectUV(a, b, uvA, uvB, col)

        if (pushTextureId) popTextureId()
    }
//    IMGUI_API void  AddImageQuad(ImTextureID user_texture_id, const ImVec2& a, const ImVec2& b, const ImVec2& c, const ImVec2& d, const ImVec2& uv_a = ImVec2(0,0), const ImVec2& uv_b = ImVec2(1,0), const ImVec2& uv_c = ImVec2(1,1), const ImVec2& uv_d = ImVec2(0,1), ImU32 col = 0xFFFFFFFF);

    fun addImageRounded(userTextureId: Int, a: Vec2, b: Vec2, uvA: Vec2, uvB: Vec2, col: Int, rounding: Float, roundingCorners: Int = Dcf.All.i) {
        if (col hasnt COL32_A_MASK) return

        if (rounding <= 0f || roundingCorners hasnt Dcf.All) {
            addImage(userTextureId, a, b, uvA, uvB, col)
            return
        }

        val pushTextureId = _textureIdStack.isEmpty() || userTextureId != _textureIdStack.last()
        if (pushTextureId) pushTextureId(userTextureId)

        val vertStartIdx = vtxBuffer.size
        pathRect(a, b, rounding, roundingCorners)
        pathFillConvex(col)
        val vertEndIdx = vtxBuffer.size
        shadeVertsLinearUV(vtxBuffer, vertStartIdx, vertEndIdx, a, b, uvA, uvB, true)

        if (pushTextureId) popTextureId()
    }

    // TODO: Thickness anti-aliased lines cap are missing their AA fringe.
    fun addPolyline(points: ArrayList<Vec2>, col: Int, closed: Boolean, thickness: Float) {

        if (points.size < 2) return

        val uv = Vec2(_data.texUvWhitePixel)

        var count = points.size
        if (!closed)
            count = points.lastIndex

        val thickLine = thickness > 1f
        if (flags has DrawListFlags.AntiAliasedLines) {
            // Anti-aliased stroke
            val AA_SIZE = 1f
            val colTrans = col wo COL32_A_MASK

            val idxCount = count * if (thickLine) 18 else 12
            val vtxCount = points.size * if (thickLine) 4 else 3
            primReserve(idxCount, vtxCount)

            // Temporary buffer
            val tempNormals = Array(points.size * if (thickLine) 5 else 3, { Vec2() })
            val tempPoints = points.size

            for (i1 in 0 until count) {
                val i2 = if ((i1 + 1) == points.size) 0 else i1 + 1
                val diff = points[i2] - points[i1]
                diff *= diff.invLength(1f)
                tempNormals[i1].x = diff.y
                tempNormals[i1].y = -diff.x
            }
            if (!closed) tempNormals[points.size - 1] = tempNormals[points.size - 2]

            if (!thickLine) {
                if (!closed) {
                    tempNormals[tempPoints + 0] = points[0] + tempNormals[0] * AA_SIZE
                    tempNormals[tempPoints + 1] = points[0] - tempNormals[0] * AA_SIZE
                    tempNormals[tempPoints + (points.size - 1) * 2 + 0] = points[points.size - 1] + tempNormals[points.size - 1] * AA_SIZE
                    tempNormals[tempPoints + (points.size - 1) * 2 + 1] = points[points.size - 1] - tempNormals[points.size - 1] * AA_SIZE
                }

                // FIXME-OPT: Merge the different loops, possibly remove the temporary buffer.
                var idx1 = _vtxCurrentIdx
                for (i1 in 0 until count) {
                    val i2 = if ((i1 + 1) == points.size) 0 else i1 + 1
                    val idx2 = if ((i1 + 1) == points.size) _vtxCurrentIdx else idx1 + 3

                    // Average normals
                    val dm = (tempNormals[i1] + tempNormals[i2]) * 0.5f
                    val dmr2 = dm.x * dm.x + dm.y * dm.y
                    if (dmr2 > 0.000001f) {
                        var scale = 1f / dmr2
                        if (scale > 100f) scale = 100.0f
                        dm *= scale
                    }
                    dm *= AA_SIZE
                    tempNormals[tempPoints + i2 * 2 + 0] = points[i2] + dm
                    tempNormals[tempPoints + i2 * 2 + 1] = points[i2] - dm

                    // Add indexes
                    idxBuffer[_idxWritePtr + 0] = idx2 + 0
                    idxBuffer[_idxWritePtr + 1] = idx1 + 0
                    idxBuffer[_idxWritePtr + 2] = idx1 + 2
                    idxBuffer[_idxWritePtr + 3] = idx1 + 2
                    idxBuffer[_idxWritePtr + 4] = idx2 + 2
                    idxBuffer[_idxWritePtr + 5] = idx2 + 0
                    idxBuffer[_idxWritePtr + 6] = idx2 + 1
                    idxBuffer[_idxWritePtr + 7] = idx1 + 1
                    idxBuffer[_idxWritePtr + 8] = idx1 + 0
                    idxBuffer[_idxWritePtr + 9] = idx1 + 0
                    idxBuffer[_idxWritePtr + 10] = idx2 + 0
                    idxBuffer[_idxWritePtr + 11] = idx2 + 1
                    _idxWritePtr += 12

                    idx1 = idx2
                }

                // Add vertexes
                for (i in 0 until points.size) {
                    vtxBuffer[_vtxWritePtr + 0].pos put points[i]
                    vtxBuffer[_vtxWritePtr + 0].uv put uv
                    vtxBuffer[_vtxWritePtr + 0].col = col
                    vtxBuffer[_vtxWritePtr + 1].pos put tempNormals[tempPoints + i * 2 + 0]
                    vtxBuffer[_vtxWritePtr + 1].uv put uv
                    vtxBuffer[_vtxWritePtr + 1].col = colTrans
                    vtxBuffer[_vtxWritePtr + 2].pos put tempNormals[tempPoints + i * 2 + 1]
                    vtxBuffer[_vtxWritePtr + 2].uv put uv
                    vtxBuffer[_vtxWritePtr + 2].col = colTrans
                    _vtxWritePtr += 3
                }
            } else {
                val halfInnerThickness = (thickness - AA_SIZE) * 0.5f
                if (!closed) {
                    tempNormals[tempPoints + 0] = points[0] + tempNormals[0] * (halfInnerThickness + AA_SIZE)
                    tempNormals[tempPoints + 1] = points[0] + tempNormals[0] * (halfInnerThickness)
                    tempNormals[tempPoints + 2] = points[0] - tempNormals[0] * (halfInnerThickness)
                    tempNormals[tempPoints + 3] = points[0] - tempNormals[0] * (halfInnerThickness + AA_SIZE)
                    tempNormals[tempPoints + (points.size - 1) * 4 + 0] = points[points.size - 1] + tempNormals[points.size - 1] * (halfInnerThickness + AA_SIZE)
                    tempNormals[tempPoints + (points.size - 1) * 4 + 1] = points[points.size - 1] + tempNormals[points.size - 1] * halfInnerThickness
                    tempNormals[tempPoints + (points.size - 1) * 4 + 2] = points[points.size - 1] - tempNormals[points.size - 1] * halfInnerThickness
                    tempNormals[tempPoints + (points.size - 1) * 4 + 3] = points[points.size - 1] - tempNormals[points.size - 1] * (halfInnerThickness + AA_SIZE)
                }

                // FIXME-OPT: Merge the different loops, possibly remove the temporary buffer.
                var idx1 = _vtxCurrentIdx
                for (i1 in 0 until count) {
                    val i2 = if ((i1 + 1) == points.size) 0 else i1 + 1
                    val idx2 = if ((i1 + 1) == points.size) _vtxCurrentIdx else idx1 + 4

                    // Average normals
                    val dm = (tempNormals[i1] + tempNormals[i2]) * 0.5f
                    val dmr2 = dm.x * dm.x + dm.y * dm.y
                    if (dmr2 > 0.000001f) {
                        var scale = 1f / dmr2
                        if (scale > 100f) scale = 100f
                        dm *= scale
                    }
                    val dmOut = dm * (halfInnerThickness + AA_SIZE)
                    val dmIn = dm * halfInnerThickness
                    tempNormals[tempPoints + i2 * 4 + 0] = points[i2] + dmOut
                    tempNormals[tempPoints + i2 * 4 + 1] = points[i2] + dmIn
                    tempNormals[tempPoints + i2 * 4 + 2] = points[i2] - dmIn
                    tempNormals[tempPoints + i2 * 4 + 3] = points[i2] - dmOut

                    // Add indexes
                    idxBuffer[_idxWritePtr + 0] = idx2 + 1
                    idxBuffer[_idxWritePtr + 1] = idx1 + 1
                    idxBuffer[_idxWritePtr + 2] = idx1 + 2
                    idxBuffer[_idxWritePtr + 3] = idx1 + 2
                    idxBuffer[_idxWritePtr + 4] = idx2 + 2
                    idxBuffer[_idxWritePtr + 5] = idx2 + 1
                    idxBuffer[_idxWritePtr + 6] = idx2 + 1
                    idxBuffer[_idxWritePtr + 7] = idx1 + 1
                    idxBuffer[_idxWritePtr + 8] = idx1 + 0
                    idxBuffer[_idxWritePtr + 9] = idx1 + 0
                    idxBuffer[_idxWritePtr + 10] = idx2 + 0
                    idxBuffer[_idxWritePtr + 11] = idx2 + 1
                    idxBuffer[_idxWritePtr + 12] = idx2 + 2
                    idxBuffer[_idxWritePtr + 13] = idx1 + 2
                    idxBuffer[_idxWritePtr + 14] = idx1 + 3
                    idxBuffer[_idxWritePtr + 15] = idx1 + 3
                    idxBuffer[_idxWritePtr + 16] = idx2 + 3
                    idxBuffer[_idxWritePtr + 17] = idx2 + 2
                    _idxWritePtr += 18

                    idx1 = idx2
                }

                // Add vertexes
                for (i in 0 until points.size) {
                    vtxBuffer[_vtxWritePtr + 0].pos put tempNormals[tempPoints + i * 4 + 0]
                    vtxBuffer[_vtxWritePtr + 0].uv put uv
                    vtxBuffer[_vtxWritePtr + 0].col = colTrans
                    vtxBuffer[_vtxWritePtr + 1].pos put tempNormals[tempPoints + i * 4 + 1]
                    vtxBuffer[_vtxWritePtr + 1].uv put uv
                    vtxBuffer[_vtxWritePtr + 1].col = col
                    vtxBuffer[_vtxWritePtr + 2].pos put tempNormals[tempPoints + i * 4 + 2]
                    vtxBuffer[_vtxWritePtr + 2].uv put uv
                    vtxBuffer[_vtxWritePtr + 2].col = col
                    vtxBuffer[_vtxWritePtr + 3].pos put tempNormals[tempPoints + i * 4 + 3]
                    vtxBuffer[_vtxWritePtr + 3].uv put uv
                    vtxBuffer[_vtxWritePtr + 3].col = colTrans
                    _vtxWritePtr += 4
                }
            }
            _vtxCurrentIdx += vtxCount
        } else {
            // Non Anti-aliased Stroke
            val idxCount = count * 6
            val vtxCount = count * 4      // FIXME-OPT: Not sharing edges
            primReserve(idxCount, vtxCount)

            for (i1 in 0 until count) {
                val i2 = if ((i1 + 1) == points.size) 0 else i1 + 1
                val p1 = points[i1]
                val p2 = points[i2]
                val diff = p2 - p1
                diff *= diff.invLength(1f)

                val d = diff * (thickness * 0.5f)
                vtxBuffer[_vtxWritePtr + 0].pos.x = p1.x + d.y
                vtxBuffer[_vtxWritePtr + 0].pos.y = p1.y - d.x
                vtxBuffer[_vtxWritePtr + 0].uv put uv
                vtxBuffer[_vtxWritePtr + 0].col = col
                vtxBuffer[_vtxWritePtr + 1].pos.x = p2.x + d.y
                vtxBuffer[_vtxWritePtr + 1].pos.y = p2.y - d.x
                vtxBuffer[_vtxWritePtr + 1].uv put uv
                vtxBuffer[_vtxWritePtr + 1].col = col
                vtxBuffer[_vtxWritePtr + 2].pos.x = p2.x - d.y
                vtxBuffer[_vtxWritePtr + 2].pos.y = p2.y + d.x
                vtxBuffer[_vtxWritePtr + 2].uv put uv
                vtxBuffer[_vtxWritePtr + 2].col = col
                vtxBuffer[_vtxWritePtr + 3].pos.x = p1.x - d.y
                vtxBuffer[_vtxWritePtr + 3].pos.y = p1.y + d.x
                vtxBuffer[_vtxWritePtr + 3].uv put uv
                vtxBuffer[_vtxWritePtr + 3].col = col
                _vtxWritePtr += 4

                idxBuffer[_idxWritePtr + 0] = _vtxCurrentIdx
                idxBuffer[_idxWritePtr + 1] = _vtxCurrentIdx + 1
                idxBuffer[_idxWritePtr + 2] = _vtxCurrentIdx + 2
                idxBuffer[_idxWritePtr + 3] = _vtxCurrentIdx
                idxBuffer[_idxWritePtr + 4] = _vtxCurrentIdx + 2
                idxBuffer[_idxWritePtr + 5] = _vtxCurrentIdx + 3
                _idxWritePtr += 6
                _vtxCurrentIdx += 4
            }
        }
    }

    fun addConvexPolyFilled(points: ArrayList<Vec2>, col: Int) {

        val uv = Vec2(_data.texUvWhitePixel)

        if (flags has DrawListFlags.AntiAliasedFill) {
            // Anti-aliased Fill
            val AA_SIZE = 1f
            val colTrans = col wo COL32_A_MASK
            val idxCount = (points.size - 2) * 3 + points.size * 6
            val vtxCount = points.size * 2
            primReserve(idxCount, vtxCount)

            // Add indexes for fill
            val vtxInnerIdx = _vtxCurrentIdx
            val vtxOuterIdx = _vtxCurrentIdx + 1
            for (i in 2 until points.size) {
                idxBuffer[_idxWritePtr + 0] = vtxInnerIdx
                idxBuffer[_idxWritePtr + 1] = vtxInnerIdx + ((i - 1) shl 1)
                idxBuffer[_idxWritePtr + 2] = vtxInnerIdx + (i shl 1)
                _idxWritePtr += 3
            }

            // Compute normals
            val tempNormals = Array(points.size, { Vec2() })
            var i0 = points.lastIndex
            var i1 = 0
            while (i1 < points.size) {
                val p0 = points[i0]
                val p1 = points[i1]
                val diff = p1 - p0
                diff *= diff.invLength(1f)
                tempNormals[i0].x = diff.y
                tempNormals[i0].y = -diff.x
                i0 = i1++
            }

            i0 = points.lastIndex
            i1 = 0
            while (i1 < points.size) {
                // Average normals
                val n0 = tempNormals[i0]
                val n1 = tempNormals[i1]
                val dm = (n0 + n1) * 0.5f
                val dmr2 = dm.x * dm.x + dm.y * dm.y
                if (dmr2 > 0.000001f) {
                    var scale = 1f / dmr2
                    if (scale > 100f) scale = 100f
                    dm *= scale
                }
                dm *= AA_SIZE * 0.5f

                // Add vertices
                vtxBuffer[_vtxWritePtr + 0].pos = points[i1] - dm
                vtxBuffer[_vtxWritePtr + 0].uv put uv
                vtxBuffer[_vtxWritePtr + 0].col = col        // Inner
                vtxBuffer[_vtxWritePtr + 1].pos = points[i1] + dm
                vtxBuffer[_vtxWritePtr + 1].uv put uv
                vtxBuffer[_vtxWritePtr + 1].col = colTrans  // Outer
                _vtxWritePtr += 2

                // Add indexes for fringes
                idxBuffer[_idxWritePtr + 0] = vtxInnerIdx + (i1 shl 1)
                idxBuffer[_idxWritePtr + 1] = vtxInnerIdx + (i0 shl 1)
                idxBuffer[_idxWritePtr + 2] = vtxOuterIdx + (i0 shl 1)
                idxBuffer[_idxWritePtr + 3] = vtxOuterIdx + (i0 shl 1)
                idxBuffer[_idxWritePtr + 4] = vtxOuterIdx + (i1 shl 1)
                idxBuffer[_idxWritePtr + 5] = vtxInnerIdx + (i1 shl 1)
                _idxWritePtr += 6

                i0 = i1++
            }
            _vtxCurrentIdx += vtxCount
        } else {
            // Non Anti-aliased Fill
            val idxCount = (points.size - 2) * 3
            val vtxCount = points.size
            primReserve(idxCount, vtxCount)
            for (i in 0 until vtxCount) {
                vtxBuffer[_vtxWritePtr].pos put points[i]
                vtxBuffer[_vtxWritePtr].uv put uv
                vtxBuffer[_vtxWritePtr].col = col
                _vtxWritePtr++
            }
            for (i in 2 until points.size) {
                idxBuffer[_idxWritePtr + 0] = _vtxCurrentIdx
                idxBuffer[_idxWritePtr + 1] = _vtxCurrentIdx + i - 1
                idxBuffer[_idxWritePtr + 2] = _vtxCurrentIdx + i
                _idxWritePtr += 3
            }
            _vtxCurrentIdx += vtxCount
        }
    }
//    IMGUI_API void  AddBezierCurve(const ImVec2& pos0, const ImVec2& cp0, const ImVec2& cp1, const ImVec2& pos1, ImU32 col, float thickness, int num_segments = 0);


    // -----------------------------------------------------------------------------------------------------------------
    // Stateful path API, add points then finish with PathFill() or PathStroke()
    // -----------------------------------------------------------------------------------------------------------------

    fun pathClear() = _path.clear()

    fun pathLineTo(pos: Vec2) = _path.add(pos)

    fun pathLineToMergeDuplicate(pos: Vec2) {
        if (_path.isEmpty() || _path.last() != pos) _path.add(pos)
    }

    fun pathFillConvex(col: Int) = addConvexPolyFilled(_path, col).also { pathClear() }

    /** rounding_corners_flags: 4-bits corresponding to which corner to round   */
    fun pathStroke(col: Int, closed: Boolean, thickness: Float = 1.0f) = addPolyline(_path, col, closed, thickness).also { pathClear() }

    fun pathArcTo(centre: Vec2, radius: Float, aMin: Float, aMax: Float, numSegments: Int = 10) {
        if (radius == 0f) {
            _path.add(centre)
            return
        }
        for (i in 0..numSegments) {
            val a = aMin + (i.f / numSegments) * (aMax - aMin)
            _path.add(Vec2(centre.x + glm.cos(a) * radius, centre.y + glm.sin(a) * radius))
        }
    }

    /** Use precomputed angles for a 12 steps circle    */
    fun pathArcToFast(centre: Vec2, radius: Float, aMinOf12: Int, aMaxOf12: Int) {

        if (radius == 0f || aMinOf12 > aMaxOf12) {
            _path.add(centre)
            return
        }
        for (a in aMinOf12..aMaxOf12) {
            val c = _data.circleVtx12[a % _data.circleVtx12.size]
            _path.add(Vec2(centre.x + c.x * radius, centre.y + c.y * radius))
        }
    }

//    fun pathBezierCurveTo(p1: Vec2, p2: Vec2, p3: Vec2, numSegments: Int = 0) {
//
//        val p1 = _path.last()
//        if (numSegments == 0)
//            // Auto-tessellated
//            pathBezierToCasteljau(&_Path, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, p4.x, p4.y, _Data->style.CurveTessellationTol, 0)
//        else
//        {
//            float t_step = 1.0f / (float)num_segments;
//            for (int i_step = 1; i_step <= num_segments; i_step++)
//            {
//                float t = t_step * i_step;
//                float u = 1.0f - t;
//                float w1 = u*u*u;
//                float w2 = 3*u*u*t;
//                float w3 = 3*u*t*t;
//                float w4 = t*t*t;
//                _Path.push_back(ImVec2(w1*p1.x + w2*p2.x + w3*p3.x + w4*p4.x, w1*p1.y + w2*p2.y + w3*p3.y + w4*p4.y));
//            }
//        }
//    }
//
//    private fun pathBezierToCasteljau(ImVector<ImVec2>* path, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float tess_tol, int level)
//    {
//        float dx = x4 - x1;
//        float dy = y4 - y1;
//        float d2 = ((x2 - x4) * dy - (y2 - y4) * dx);
//        float d3 = ((x3 - x4) * dy - (y3 - y4) * dx);
//        d2 = (d2 >= 0) ? d2 : -d2;
//        d3 = (d3 >= 0) ? d3 : -d3;
//        if ((d2+d3) * (d2+d3) < tess_tol * (dx*dx + dy*dy))
//            {
//                path->push_back(ImVec2(x4, y4));
//            }
//        else if (level < 10)
//        {
//            float x12 = (x1+x2)*0.5f,       y12 = (y1+y2)*0.5f;
//            float x23 = (x2+x3)*0.5f,       y23 = (y2+y3)*0.5f;
//            float x34 = (x3+x4)*0.5f,       y34 = (y3+y4)*0.5f;
//            float x123 = (x12+x23)*0.5f,    y123 = (y12+y23)*0.5f;
//            float x234 = (x23+x34)*0.5f,    y234 = (y23+y34)*0.5f;
//            float x1234 = (x123+x234)*0.5f, y1234 = (y123+y234)*0.5f;
//
//            PathBezierToCasteljau(path, x1,y1,        x12,y12,    x123,y123,  x1234,y1234, tess_tol, level+1);
//            PathBezierToCasteljau(path, x1234,y1234,  x234,y234,  x34,y34,    x4,y4,       tess_tol, level+1);
//        }
//    }

    fun pathRect(a: Vec2, b: Vec2, rounding: Float = 0f, roundingCorners: Int = Dcf.All.i) {

        var cond = ((roundingCorners and Dcf.Top) == Dcf.Top.i) || ((roundingCorners and Dcf.Bot) == Dcf.Bot.i) // TODO consider simplyfing
        var rounding = glm.min(rounding, glm.abs(b.x - a.x) * (if (cond) 0.5f else 1f) - 1f)
        cond = ((roundingCorners and Dcf.Left) == Dcf.Left.i) || ((roundingCorners and Dcf.Right) == Dcf.Right.i)
        rounding = glm.min(rounding, glm.abs(b.y - a.y) * (if (cond) 0.5f else 1f) - 1f)

        if (rounding <= 0f || roundingCorners == 0) {
            pathLineTo(a)
            pathLineTo(Vec2(b.x, a.y))
            pathLineTo(b)
            pathLineTo(Vec2(a.x, b.y))
        } else {
            val roundingTL = if (roundingCorners has Dcf.TopLeft) rounding else 0f
            val roundingTR = if (roundingCorners has Dcf.TopRight) rounding else 0f
            val roundingBR = if (roundingCorners has Dcf.BotRight) rounding else 0f
            val roundingBL = if (roundingCorners has Dcf.BotLeft) rounding else 0f
            pathArcToFast(Vec2(a.x + roundingTL, a.y + roundingTL), roundingTL, 6, 9)
            pathArcToFast(Vec2(b.x - roundingTR, a.y + roundingTR), roundingTR, 9, 12)
            pathArcToFast(Vec2(b.x - roundingBR, b.y - roundingBR), roundingBR, 0, 3)
            pathArcToFast(Vec2(a.x + roundingBL, b.y - roundingBL), roundingBL, 3, 6)
        }
    }


    // -----------------------------------------------------------------------------------------------------------------
    // Channels
    // - Use to simulate layers. By switching channels to can render out-of-order (e.g. submit foreground primitives before background primitives)
    // - Use to minimize draw calls (e.g. if going back-and-forth between multiple non-overlapping clipping rectangles, prefer to append into separate channels then merge at the end)
    // -----------------------------------------------------------------------------------------------------------------

    fun channelsSplit(channelsCount: Int) {

        assert(_channelsCurrent == 0 && _channelsCount == 1)
        val oldChannelsCount = _channels.size
        if (oldChannelsCount < channelsCount)
            for (i in oldChannelsCount until channelsCount) _channels.add(DrawChannel())   // resize(channelsCount)
        _channelsCount = channelsCount

        /*  _Channels[] (24/32 bytes each) hold storage that we'll swap with this->_CmdBuffer/_IdxBuffer
            The content of _Channels[0] at this point doesn't matter. We clear it to make state tidy in a debugger but
            we don't strictly need to.
            When we switch to the next channel, we'll copy _CmdBuffer/_IdxBuffer into _Channels[0] and then _Channels[1]
            into _CmdBuffer/_IdxBuffer  */
        _channels[0] = DrawChannel()
        for (i in 1 until channelsCount) {
            if (i < oldChannelsCount)
                _channels[i].clear()
            if (_channels[i].cmdBuffer.isEmpty()) {
                val drawCmd = DrawCmd()
                drawCmd.clipRect = Vec4(_clipRectStack.last())
                drawCmd.textureId = _textureIdStack.last()
                _channels[i].cmdBuffer.add(drawCmd)
            }
        }
    }

    fun channelsMerge() {

        /*  Note that we never use or rely on channels.Size because it is merely a buffer that we never shrink back to 0
            to keep all sub-buffers ready for use.  */
        if (_channelsCount <= 1) return

        channelsSetCurrent(0)
        if (cmdBuffer.isNotEmpty() && cmdBuffer.last().elemCount == 0)
            cmdBuffer.pop()

        var newCmdBufferCount = 0
        var newIdxBufferCount = 0
        for (i in 1 until _channelsCount) {
            val ch = _channels[i]
            if (ch.cmdBuffer.isNotEmpty() && ch.cmdBuffer.last().elemCount == 0)
                ch.cmdBuffer.pop()
            newCmdBufferCount += ch.cmdBuffer.size
            newIdxBufferCount += ch.idxBuffer.size
        }
        for (i in 0 until newCmdBufferCount) cmdBuffer.add(DrawCmd())   // resize(cmdBuffer.size + newCmdBufferCount)
        for (i in 0 until newIdxBufferCount) idxBuffer.add(0)           // resize(idxBuffer.size + newIdxBufferCount)

        var cmdWrite = cmdBuffer.size - newCmdBufferCount
        _idxWritePtr = idxBuffer.size - newIdxBufferCount

        for (i in 1 until _channelsCount) {
            val ch = _channels[i]
            for (j in ch.cmdBuffer.indices) {
                cmdBuffer[cmdWrite] = DrawCmd(ch.cmdBuffer[j])
                cmdWrite++
            }
            for (j in ch.idxBuffer.indices) {
                idxBuffer[_idxWritePtr] = ch.idxBuffer[j]
                _idxWritePtr++
            }
        }
        updateClipRect() // We call this instead of addDrawCmd(), so that empty channels won't produce an extra draw call.
        _channelsCount = 1
    }

    fun channelsSetCurrent(idx: Int) {

        assert(idx < _channelsCount)
        if (_channelsCurrent == idx) return
        _channels[_channelsCurrent].cmdBuffer = cmdBuffer
        _channels[_channelsCurrent].idxBuffer = idxBuffer
        _channelsCurrent = idx
        cmdBuffer = _channels[_channelsCurrent].cmdBuffer
        idxBuffer = _channels[_channelsCurrent].idxBuffer
        _idxWritePtr = idxBuffer.size
    }

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
        drawCmd.clipRect put currentClipRect
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
        flags = DrawListFlags.AntiAliasedLines or DrawListFlags.AntiAliasedFill
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

    /** NB: this can be called with negative count for removing primitives (as long as the result does not underflow)    */
    fun primReserve(idxCount: Int, vtxCount: Int) {

        cmdBuffer.last().elemCount += idxCount

        val vtxBufferOldSize = vtxBuffer.size
        for (v in 0 until vtxCount) // TODO check negative
            if (vtxCount > 0) vtxBuffer.add(DrawVert())
            else vtxBuffer.removeAt(vtxBuffer.lastIndex)
        _vtxWritePtr = vtxBufferOldSize

        val idxBufferOldSize = idxBuffer.size
        for (i in 0 until idxCount)
            if (idxCount > 0) idxBuffer.add(0)
            else idxBuffer.removeAt(idxBuffer.lastIndex)
        _idxWritePtr = idxBufferOldSize
    }

    /** Fully unrolled with inline call to keep our debug builds decently fast.
    Axis aligned rectangle (composed of two triangles)  */
    fun primRect(a: Vec2, c: Vec2, col: Int) {
        val b = Vec2(c.x, a.y)
        val d = Vec2(a.x, c.y)
        val uv = Vec2(_data.texUvWhitePixel)
        val idx = _vtxCurrentIdx
        idxBuffer[_idxWritePtr + 0] = idx; idxBuffer[_idxWritePtr + 1] = idx + 1; idxBuffer[_idxWritePtr + 2] = idx + 2
        idxBuffer[_idxWritePtr + 3] = idx; idxBuffer[_idxWritePtr + 4] = idx + 2; idxBuffer[_idxWritePtr + 5] = idx + 3
        vtxBuffer[_vtxWritePtr + 0].pos put a; vtxBuffer[_vtxWritePtr + 0].uv put uv; vtxBuffer[_vtxWritePtr + 0].col = col
        vtxBuffer[_vtxWritePtr + 1].pos put b; vtxBuffer[_vtxWritePtr + 1].uv put uv; vtxBuffer[_vtxWritePtr + 1].col = col
        vtxBuffer[_vtxWritePtr + 2].pos put c; vtxBuffer[_vtxWritePtr + 2].uv put uv; vtxBuffer[_vtxWritePtr + 2].col = col
        vtxBuffer[_vtxWritePtr + 3].pos put d; vtxBuffer[_vtxWritePtr + 3].uv put uv; vtxBuffer[_vtxWritePtr + 3].col = col
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
        vtxBuffer[_vtxWritePtr + 0].pos put a; vtxBuffer[_vtxWritePtr + 0].uv put uvA; vtxBuffer[_vtxWritePtr + 0].col = col
        vtxBuffer[_vtxWritePtr + 1].pos put b; vtxBuffer[_vtxWritePtr + 1].uv put uvB; vtxBuffer[_vtxWritePtr + 1].col = col
        vtxBuffer[_vtxWritePtr + 2].pos put c; vtxBuffer[_vtxWritePtr + 2].uv put uvC; vtxBuffer[_vtxWritePtr + 2].col = col
        vtxBuffer[_vtxWritePtr + 3].pos put d; vtxBuffer[_vtxWritePtr + 3].uv put uvD; vtxBuffer[_vtxWritePtr + 3].col = col
        _vtxWritePtr += 4
        _vtxCurrentIdx += 4
        _idxWritePtr += 6
    }

    fun primQuadUV(a: Vec2, b: Vec2, c: Vec2, d: Vec2, uvA: Vec2, uvB: Vec2, uvC: Vec2, uvD: Vec2, col: Int) {
        val idx = _vtxCurrentIdx
        idxBuffer[_idxWritePtr + 0] = idx; idxBuffer[_idxWritePtr + 1] = idx + 1; idxBuffer[_idxWritePtr + 2] = idx + 2
        idxBuffer[_idxWritePtr + 3] = idx; idxBuffer[_idxWritePtr + 4] = idx + 2; idxBuffer[_idxWritePtr + 5] = idx + 3
        vtxBuffer[_vtxWritePtr + 0].pos put a; vtxBuffer[_vtxWritePtr + 0].uv put uvA; vtxBuffer[_vtxWritePtr + 0].col = col
        vtxBuffer[_vtxWritePtr + 1].pos put b; vtxBuffer[_vtxWritePtr + 1].uv put uvB; vtxBuffer[_vtxWritePtr + 1].col = col
        vtxBuffer[_vtxWritePtr + 2].pos put c; vtxBuffer[_vtxWritePtr + 2].uv put uvC; vtxBuffer[_vtxWritePtr + 2].col = col
        vtxBuffer[_vtxWritePtr + 3].pos put d; vtxBuffer[_vtxWritePtr + 3].uv put uvD; vtxBuffer[_vtxWritePtr + 3].col = col
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

    fun primWriteIdx(idx: DrawIdx) = idxBuffer.set(_idxWritePtr++, idx)

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
        val currClipRect = Vec4(currentClipRect)
        val currCmd = cmdBuffer.lastOrNull()
        if (currCmd == null || (currCmd.elemCount != 0 && currCmd.clipRect != currClipRect) || currCmd.userCallback != null) {
            addDrawCmd()
            return
        }
        // Try to merge with previous command if it matches, else use current command
        val prevCmd = if (cmdBuffer.size > 1) cmdBuffer[cmdBuffer.lastIndex - 1] else null
        if (currCmd.elemCount == 0 && prevCmd != null && prevCmd.clipRect == currClipRect &&
                prevCmd.textureId == currentTextureId!! && prevCmd.userCallback == null)
            cmdBuffer.pop()
        else
            currCmd.clipRect put currClipRect
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
        if (currCmd.elemCount == 0 && prevCmd != null && prevCmd.textureId == currentTextureId!! && prevCmd.clipRect == currentClipRect &&
                prevCmd.userCallback == null)
            cmdBuffer.pop()
        else
            currCmd.textureId = currentTextureId!!
    }


    // Macros
    val currentClipRect get() = if (_clipRectStack.isNotEmpty()) _clipRectStack.last()!! else _data.clipRectFullscreen
    val currentTextureId get() = if (_textureIdStack.isNotEmpty()) _textureIdStack.last()!! else null

    infix fun addTo(renderList: ArrayList<DrawList>) {

        if (cmdBuffer.empty()) return

        // Remove trailing command if unused
        val lastCmd = cmdBuffer.last()
        if (lastCmd.elemCount == 0 && lastCmd.userCallback == null) {
            cmdBuffer.pop()
            if (cmdBuffer.empty()) return
        }

        /*  Draw list sanity check. Detect mismatch between PrimReserve() calls and incrementing _VtxCurrentIdx,
            _VtxWritePtr etc. May trigger for you if you are using PrimXXX functions incorrectly.   */
        assert(vtxBuffer.isEmpty() || _vtxWritePtr == vtxBuffer.size)
        assert(idxBuffer.isEmpty() || _idxWritePtr == idxBuffer.size)
        assert(_vtxCurrentIdx == vtxBuffer.size)

        // JVM ImGui, this doesnt apply, we use Ints by default, TODO make Int/Short option?
        /*  Check that drawList doesn't use more vertices than indexable
            (default DrawIdx = unsigned short = 2 bytes = 64K vertices per DrawList = per window)
            If this assert triggers because you are drawing lots of stuff manually:
            A) Make sure you are coarse clipping, because DrawList let all your vertices pass. You can use the Metrics
                window to inspect draw list contents.
            B) If you need/want meshes with more than 64K vertices, uncomment the '#define DrawIdx unsigned int' line in
                imconfig.h to set the index size to 4 bytes.
                You'll need to handle the 4-bytes indices to your renderer. For example, the OpenGL example code detect
                index size at compile-time by doing:
                glDrawElements(GL_TRIANGLES, cmd.elemCount, sizeof(ImDrawIdx) == 2 ? GL_UNSIGNED_SHORT : GL_UNSIGNED_INT, idxBufferOffset)
                Your own engine or render API may use different parameters or function calls to specify index sizes.
                2 and 4 bytes indices are generally supported by most API.
            C) If for some reason you cannot use 4 bytes indices or don't want to, a workaround is to call
                beginChild()/endChild() before reaching the 64K limit to split your draw commands in multiple draw lists.*/
//        assert(_vtxCurrentIdx <= (1L shl (Int.BYTES * 8))) // Too many vertices in same Im See comment above.

        renderList += this
        IO.metricsRenderVertices += vtxBuffer.size
        IO.metricsRenderIndices += idxBuffer.size
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

    /** Draw lists are owned by the ImGuiContext and only pointed to here. */
    fun clear() {
        valid = false
        cmdLists.clear()
        totalIdxCount = 0
        totalVtxCount = 0
        cmdListsCount = 0
    }

    /** For backward compatibility or convenience: convert all buffers from indexed to de-indexed, in case you cannot
     *  render indexed.
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
                cmd.clipRect.timesAssign(scale.x, scale.y, scale.x, scale.y)
            }
        }
    }
}