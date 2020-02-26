package imgui.classes

import gli_.hasnt
import glm_.*
import glm_.func.common.max
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.drawData
import imgui.ImGui.io
import imgui.ImGui.style
import imgui.api.g
import imgui.font.Font
import imgui.internal.*
import imgui.internal.classes.DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC
import imgui.internal.classes.DRAWLIST_CIRCLE_AUTO_SEGMENT_MAX
import imgui.internal.classes.DrawListSharedData
import imgui.internal.classes.Rect
import kool.*
import org.lwjgl.system.MemoryUtil
import uno.kotlin.plusAssign
import java.nio.ByteBuffer
import java.util.Stack
import kotlin.collections.ArrayList
import kotlin.collections.lastIndex
import kotlin.math.sqrt

/** A single draw command list (generally one per window, conceptually you may see this as a dynamic "mesh" builder)
 *
 *  Draw command list
 *  This is the low-level list of polygons that ImGui:: functions are filling. At the end of the frame,
 *  all command lists are passed to your ImGuiIO::RenderDrawListFn function for rendering.
 *  Each dear imgui window contains its own ImDrawList. You can use ImGui::GetWindowDrawList() to
 *  access the current window draw list and draw custom primitives.
 *  You can interleave normal ImGui:: calls and adding primitives to the current draw list.
 *  All positions are generally in pixel coordinates (top-left at (0,0), bottom-right at io.DisplaySize),
 *  but you are totally free to apply whatever transformation matrix to want to the data
 *  (if you apply such transformation you'll want to apply it to ClipRect as well)
 *  Important: Primitives are always added to the list and not culled (culling is done at render time and
 *  at a higher-level by ImGui::functions), if you use this API a lot consider coarse culling your drawn objects.
 *
 *  If you want to create ImDrawList instances, pass them ImGui::GetDrawListSharedData() or create and use your own
 *  DrawListSharedData (so you can use ImDrawList without ImGui)    */
class DrawList(sharedData: DrawListSharedData?) {

    // -----------------------------------------------------------------------------------------------------------------
    // This is what you have to render
    // -----------------------------------------------------------------------------------------------------------------

    /** Draw commands. Typically 1 command = 1 GPU draw call, unless the command is a callback.    */
    var cmdBuffer = Stack<DrawCmd>()
    /** Index buffer. Each command consume ImDrawCmd::ElemCount of those    */
    var idxBuffer = IntBuffer(0)
    /** Vertex buffer.  */
    var vtxBuffer = DrawVert_Buffer(0)


    // -----------------------------------------------------------------------------------------------------------------
    // [Internal, used while building lists]
    // -----------------------------------------------------------------------------------------------------------------

    /** Flags, you may poke into these to adjust anti-aliasing settings per-primitive. */
    var flags: DrawListFlags = 0
    /** Pointer to shared draw data (you can use ImGui::drawListSharedData to get the one from current ImGui context) */
    var _data: DrawListSharedData = sharedData ?: DrawListSharedData()
    /** Pointer to owner window's name for debugging    */
    var _ownerName = ""
    /** [Internal] Always 0 unless 'Flags & ImDrawListFlags_AllowVtxOffset'. */
    var _vtxCurrentOffset = 0
    /** [Internal] Generally == VtxBuffer.Size unless we are past 64K vertices, in which case this gets reset to 0. */
    var _vtxCurrentIdx = 0
    /** [Internal] point within VtxBuffer.Data after each add command (to avoid using the ImVector<> operators too much)    */
    var _vtxWritePtr = 0
    /** [Internal] point within IdxBuffer.Data after each add command (to avoid using the ImVector<> operators too much)    */
    var _idxWritePtr = 0

    val _clipRectStack = Stack<Vec4>()
    /** [Internal]  */
    val _textureIdStack = Stack<TextureID>()
    /** [Internal] current path building    */
    val _path = ArrayList<Vec2>()
    /** [Internal] for channels api */
    val _splitter = DrawListSplitter()

    fun destroy() = clearFreeMemory()

    /** Render-level scissoring. This is passed down to your render function but not used for CPU-side coarse clipping.
     *  Prefer using higher-level ImGui::PushClipRect() to affect logic (hit-testing and widget culling)    */
    fun pushClipRect(rect: Rect, intersectWithCurrentClipRect: Boolean = false) = pushClipRect(rect.min, rect.max, intersectWithCurrentClipRect)

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

        _clipRectStack += cr
        updateClipRect()
    }

    fun pushClipRectFullScreen() = pushClipRect(Vec2(_data.clipRectFullscreen), Vec2(_data.clipRectFullscreen.z, _data.clipRectFullscreen.w))

    fun popClipRect() {
        assert(_clipRectStack.isNotEmpty())
        _clipRectStack.pop()
        updateClipRect()
    }

    fun pushTextureId(textureId: TextureID) = _textureIdStack.push(textureId).run { updateTextureID() }

    fun popTextureId() = _textureIdStack.pop().also { updateTextureID() }


    // -----------------------------------------------------------------------------------------------------------------
    // Primitives
    // - For rectangular primitives, "p_min" and "p_max" represent the upper-left and lower-right corners.
    // - For circle primitives, use "num_segments == 0" to automatically calculate tessellation (preferred).
    //   In future versions we will use textures to provide cheaper and higher-quality circles.
    //   Use AddNgon() and AddNgonFilled() functions if you need to guaranteed a specific number of sides.
    // -----------------------------------------------------------------------------------------------------------------

    /** JVM it's safe to pass directly Vec2 istances, they wont be modified */
    fun addLine(p1: Vec2, p2: Vec2, col: Int, thickness: Float = 1f) {
        if (col hasnt COL32_A_MASK) return
        pathLineTo(p1 + Vec2(0.5f))
        pathLineTo(p2 + Vec2(0.5f))
        pathStroke(col, false, thickness)
    }

    /** Note we don't render 1 pixels sized rectangles properly.
     * @param pMin: upper-left
     * @param pMax: lower-right
     * (== upper-left + size)   */
    fun addRect(pMin: Vec2, pMax: Vec2, col: Int, rounding: Float = 0f, roundingCorners: DrawCornerFlags = DrawCornerFlag.All.i, thickness: Float = 1f) {
        if (col hasnt COL32_A_MASK) return
        if (flags has DrawListFlag.AntiAliasedLines)
            pathRect(pMin + 0.5f, pMax - 0.5f, rounding, roundingCorners)
        else    // Better looking lower-right corner and rounded non-AA shapes.
            pathRect(pMin + 0.5f, pMax - 0.49f, rounding, roundingCorners)
        pathStroke(col, true, thickness)
    }

    /** @param pMin: upper-left
     *  @param pMax: lower-right
     *  (== upper-left + size) */
    fun addRectFilled(pMin: Vec2, pMax: Vec2, col: Int, rounding: Float = 0f, roundingCorners: DrawCornerFlags = DrawCornerFlag.All.i) {
        if (col hasnt COL32_A_MASK) return
        if (rounding > 0f) {
            pathRect(pMin, pMax, rounding, roundingCorners)
            pathFillConvex(col)
        } else {
            primReserve(6, 4)
            primRect(pMin, pMax, col)
        }
    }

    /**
     * @param pMin = upper-left
     * @param pMax = lower-right
     */
    fun addRectFilledMultiColor(pMin: Vec2, pMax: Vec2, colUprLeft: Int, colUprRight: Int, colBotRight: Int, colBotLeft: Int) {

        if ((colUprLeft or colUprRight or colBotRight or colBotLeft) hasnt COL32_A_MASK) return

        val uv = _data.texUvWhitePixel
        primReserve(6, 4)
        primWriteIdx(_vtxCurrentIdx); primWriteIdx(_vtxCurrentIdx + 1); primWriteIdx(_vtxCurrentIdx + 2)
        primWriteIdx(_vtxCurrentIdx); primWriteIdx(_vtxCurrentIdx + 2); primWriteIdx(_vtxCurrentIdx + 3)
        primWriteVtx(pMin, uv, colUprLeft)
        primWriteVtx(Vec2(pMax.x, pMin.y), uv, colUprRight)
        primWriteVtx(pMax, uv, colBotRight)
        primWriteVtx(Vec2(pMin.x, pMax.y), uv, colBotLeft)
    }

    fun addQuad(p1: Vec2, p2: Vec2, p3: Vec2, p4: Vec2, col: Int, thickness: Float = 1f) {

        if (col hasnt COL32_A_MASK) return

        pathLineTo(p1)
        pathLineTo(p2)
        pathLineTo(p3)
        pathLineTo(p4)
        pathStroke(col, true, thickness)
    }

    fun addQuadFilled(p1: Vec2, p2: Vec2, p3: Vec2, p4: Vec2, col: Int) {

        if (col hasnt COL32_A_MASK) return

        pathLineTo(p1)
        pathLineTo(p2)
        pathLineTo(p3)
        pathLineTo(p4)
        pathFillConvex(col)
    }

    fun addTriangle(p1: Vec2, p2: Vec2, p3: Vec2, col: Int, thickness: Float = 1f) {

        if (col hasnt COL32_A_MASK) return

        pathLineTo(p1)
        pathLineTo(p2)
        pathLineTo(p3)
        pathStroke(col, true, thickness)
    }

    fun addTriangleFilled(p1: Vec2, p2: Vec2, p3: Vec2, col: Int) {

        if (col hasnt COL32_A_MASK) return

        pathLineTo(p1)
        pathLineTo(p2)
        pathLineTo(p3)
        pathFillConvex(col)
    }

    fun addCircle(center: Vec2, radius: Float, col: Int, numSegments_: Int = 12, thickness: Float = 1f) {

        if (col hasnt COL32_A_MASK || radius <= 0f) return

        // Obtain segment count
        val numSegments = when {
            numSegments_ <= 0 -> {
                // Automatic segment count
                val radiusIdx = radius.i - 1
                _data.circleSegmentCounts.getOrElse(radiusIdx) { // Use cached value
                    DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC(radius, _data.circleSegmentMaxError)
                }
            }
            else -> // Explicit segment count (still clamp to avoid drawing insanely tessellated shapes)
                clamp(numSegments_, 3, DRAWLIST_CIRCLE_AUTO_SEGMENT_MAX)
        }

        // Because we are filling a closed shape we remove 1 from the count of segments/points
        val aMax = glm.PIf * 2f * (numSegments - 1f) / numSegments
        if (numSegments == 12)
            pathArcToFast(center, radius - 0.5f, 0, 12)
        else
            pathArcTo(center, radius - 0.5f, 0f, aMax, numSegments - 1)
        pathStroke(col, true, thickness)
    }

    fun addCircleFilled(center: Vec2, radius: Float, col: Int, numSegments_: Int = 12) {

        if (col hasnt COL32_A_MASK || radius <= 0f) return

        // Obtain segment count
        val numSegments = when {
            numSegments_ <= 0 -> {
                // Automatic segment count
                val radiusIdx = radius.i - 1
                _data.circleSegmentCounts.getOrElse(radiusIdx) { // Use cached value
                    DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC(radius, _data.circleSegmentMaxError)
                }
            }
            else -> // Explicit segment count (still clamp to avoid drawing insanely tessellated shapes)
                clamp(numSegments_, 3, DRAWLIST_CIRCLE_AUTO_SEGMENT_MAX)
        }

        // Because we are filling a closed shape we remove 1 from the count of segments/points
        val aMax = glm.PIf * 2f * (numSegments - 1f) / numSegments
        if (numSegments == 12)
            pathArcToFast(center, radius, 0, 12)
        else
            pathArcTo(center, radius, 0f, aMax, numSegments - 1)
        pathFillConvex(col)
    }

    /** Guaranteed to honor 'num_segments' */
    fun addNgon(center: Vec2, radius: Float, col: Int, numSegments: Int, thickness: Float) {
        if (col hasnt COL32_A_MASK || numSegments <= 2)
            return

        // Because we are filling a closed shape we remove 1 from the count of segments/points
        val aMax = (glm.πf * 2f) * (numSegments.f - 1f) / numSegments.f
        pathArcTo(center, radius - 0.5f, 0f, aMax, numSegments - 1)
        pathStroke(col, true, thickness)
    }

    /** Guaranteed to honor 'num_segments' */
    fun addNgonFilled(center: Vec2, radius: Float, col: Int, numSegments: Int) {
        if (col hasnt COL32_A_MASK || numSegments <= 2)
            return

        // Because we are filling a closed shape we remove 1 from the count of segments/points
        val aMax = (glm.πf * 2f) * (numSegments.f - 1f) / numSegments.f
        pathArcTo(center, radius, 0f, aMax, numSegments - 1)
        pathFillConvex(col)
    }

    fun addText(pos: Vec2, col: Int, text: String) = addText(g.font, g.fontSize, pos, col, text)

    fun addText(font: Font?, fontSize: Float, pos: Vec2, col: Int, text: String,
                wrapWidth: Float = 0f, cpuFineClipRect: Vec4? = null) {
        val bytes = text.toByteArray()
        addText(font, fontSize, pos, col, bytes, 0, bytes.size, wrapWidth, cpuFineClipRect)
    }

    fun addText(font_: Font?, fontSize_: Float, pos: Vec2, col: Int, text: ByteArray, textBegin: Int = 0,
                textEnd: Int = text.strlen(), wrapWidth: Float = 0f, cpuFineClipRect: Vec4? = null) {

        if ((col and COL32_A_MASK) == 0) return

        if (textEnd == 0)
            return

        // Pull default font/size from the shared ImDrawListSharedData instance
        val font = font_ ?: _data.font!!
        val fontSize = if (fontSize_ == 0f) _data.fontSize else fontSize_

        assert(font.containerAtlas.texID == _textureIdStack.last()) {
            "Use high-level ImGui::pushFont() or low-level DrawList::pushTextureId() to change font_"
        }

        val clipRect = Vec4(_clipRectStack.last())
        cpuFineClipRect?.let {
            clipRect.x = clipRect.x max cpuFineClipRect.x
            clipRect.y = clipRect.y max cpuFineClipRect.y
            clipRect.z = clipRect.z min cpuFineClipRect.z
            clipRect.w = clipRect.w min cpuFineClipRect.w
        }
        font.renderText(this, fontSize, Vec2(pos), col, clipRect, text, textBegin, textEnd, wrapWidth, cpuFineClipRect != null)
    }

    /** TODO: Thickness anti-aliased lines cap are missing their AA fringe.
     *  We avoid using the ImVec2 math operators here to reduce cost to a minimum for debug/non-inlined builds. */
    fun addPolyline(points: ArrayList<Vec2>, col: Int, closed: Boolean, thickness: Float) {

        if (points.size < 2) return

        val uv = Vec2(_data.texUvWhitePixel)

        var count = points.size
        if (!closed)
            count = points.lastIndex

        val thickLine = thickness > 1f

        if (flags has DrawListFlag.AntiAliasedLines) {
            // Anti-aliased stroke
            val AA_SIZE = 1f
            val colTrans = col wo COL32_A_MASK

            val idxCount = count * if (thickLine) 18 else 12
            val vtxCount = points.size * if (thickLine) 4 else 3
            primReserve(idxCount, vtxCount)
            vtxBuffer.pos = _vtxWritePtr
            idxBuffer.pos = _idxWritePtr

            // Temporary buffer
            val temp = Array(points.size * if (thickLine) 5 else 3) { Vec2() }
            val tempPointsIdx = points.size

            for (i1 in 0 until count) {
                val i2 = if (i1 + 1 == points.size) 0 else i1 + 1
                var dx = points[i2].x - points[i1].x
                var dy = points[i2].y - points[i1].y
                //IM_NORMALIZE2F_OVER_ZERO(dx, dy);
                val d2 = dx * dx + dy * dy
                if (d2 > 0f) {
                    val invLen = 1f / sqrt(d2)
                    dx *= invLen
                    dy *= invLen
                }
                temp[i1].x = dy
                temp[i1].y = -dx
            }
            if (!closed) temp[points.size - 1] = temp[points.size - 2]

            if (!thickLine) {
                if (!closed) {
                    temp[tempPointsIdx + 0] = points[0] + temp[0] * AA_SIZE
                    temp[tempPointsIdx + 1] = points[0] - temp[0] * AA_SIZE
                    temp[tempPointsIdx + (points.size - 1) * 2 + 0] = points[points.size - 1] + temp[points.size - 1] * AA_SIZE
                    temp[tempPointsIdx + (points.size - 1) * 2 + 1] = points[points.size - 1] - temp[points.size - 1] * AA_SIZE
                }

                // FIXME-OPT: Merge the different loops, possibly remove the temporary buffer.
                var idx1 = _vtxCurrentIdx
                for (i1 in 0 until count) {
                    val i2 = if ((i1 + 1) == points.size) 0 else i1 + 1
                    val idx2 = if ((i1 + 1) == points.size) _vtxCurrentIdx else idx1 + 3

                    // Average normals
                    var dmX = (temp[i1].x + temp[i2].x) * 0.5f
                    var dmY = (temp[i1].y + temp[i2].y) * 0.5f
//                    IM_FIXNORMAL2F(dm_x, dm_y)
                    run {
                        var d2 = dmX * dmX + dmY * dmY
                        if (d2 < 0.5f)
                            d2 = 0.5f
                        val invLensq = 1f / d2
                        dmX *= invLensq
                        dmY *= invLensq
                    }
                    dmX *= AA_SIZE
                    dmY *= AA_SIZE

                    // Add temporary vertexes
                    val outVtxIdx = tempPointsIdx + i2 * 2
                    temp[outVtxIdx + 0].x = points[i2].x + dmX
                    temp[outVtxIdx + 0].y = points[i2].y + dmY
                    temp[outVtxIdx + 1].x = points[i2].x - dmX
                    temp[outVtxIdx + 1].y = points[i2].y - dmY

                    // Add indexes
                    idxBuffer += idx2 + 0; idxBuffer += idx1 + 0; idxBuffer += idx1 + 2
                    idxBuffer += idx1 + 2; idxBuffer += idx2 + 2; idxBuffer += idx2 + 0
                    idxBuffer += idx2 + 1; idxBuffer += idx1 + 1; idxBuffer += idx1 + 0
                    idxBuffer += idx1 + 0; idxBuffer += idx2 + 0; idxBuffer += idx2 + 1
                    _idxWritePtr += 12

                    idx1 = idx2
                }

                // Add vertexes
                for (i in 0 until points.size) {
                    vtxBuffer += points[i]; vtxBuffer += uv; vtxBuffer += col
                    vtxBuffer += temp[tempPointsIdx + i * 2 + 0]; vtxBuffer += uv; vtxBuffer += colTrans
                    vtxBuffer += temp[tempPointsIdx + i * 2 + 1]; vtxBuffer += uv; vtxBuffer += colTrans
                    _vtxWritePtr += 3
                }
            } else {
                val halfInnerThickness = (thickness - AA_SIZE) * 0.5f
                if (!closed) {
                    temp[tempPointsIdx + 0] = points[0] + temp[0] * (halfInnerThickness + AA_SIZE)
                    temp[tempPointsIdx + 1] = points[0] + temp[0] * (halfInnerThickness)
                    temp[tempPointsIdx + 2] = points[0] - temp[0] * (halfInnerThickness)
                    temp[tempPointsIdx + 3] = points[0] - temp[0] * (halfInnerThickness + AA_SIZE)
                    temp[tempPointsIdx + (points.size - 1) * 4 + 0] = points[points.size - 1] + temp[points.size - 1] * (halfInnerThickness + AA_SIZE)
                    temp[tempPointsIdx + (points.size - 1) * 4 + 1] = points[points.size - 1] + temp[points.size - 1] * halfInnerThickness
                    temp[tempPointsIdx + (points.size - 1) * 4 + 2] = points[points.size - 1] - temp[points.size - 1] * halfInnerThickness
                    temp[tempPointsIdx + (points.size - 1) * 4 + 3] = points[points.size - 1] - temp[points.size - 1] * (halfInnerThickness + AA_SIZE)
                }

                // FIXME-OPT: Merge the different loops, possibly remove the temporary buffer.
                var idx1 = _vtxCurrentIdx
                for (i1 in 0 until count) {
                    val i2 = if ((i1 + 1) == points.size) 0 else i1 + 1
                    val idx2 = if ((i1 + 1) == points.size) _vtxCurrentIdx else idx1 + 4

                    // Average normals
                    var dmX = (temp[i1].x + temp[i2].x) * 0.5f
                    var dmY = (temp[i1].y + temp[i2].y) * 0.5f
//                    IM_FIXNORMAL2F(dm_x, dm_y)
                    run {
                        var d2 = dmX * dmX + dmY * dmY
                        if (d2 < 0.5f)
                            d2 = 0.5f
                        val invLensq = 1f / d2
                        dmX *= invLensq
                        dmY *= invLensq
                    }
                    val dmOutX = dmX * (halfInnerThickness + AA_SIZE)
                    val dmOutY = dmY * (halfInnerThickness + AA_SIZE)
                    val dmInX = dmX * halfInnerThickness
                    val dmInY = dmY * halfInnerThickness

                    // Add temporary vertexes
                    val outVtxIdx = tempPointsIdx + i2 * 4
                    temp[outVtxIdx + 0].x = points[i2].x + dmOutX
                    temp[outVtxIdx + 0].y = points[i2].y + dmOutY
                    temp[outVtxIdx + 1].x = points[i2].x + dmInX
                    temp[outVtxIdx + 1].y = points[i2].y + dmInY
                    temp[outVtxIdx + 2].x = points[i2].x - dmInX
                    temp[outVtxIdx + 2].y = points[i2].y - dmInY
                    temp[outVtxIdx + 3].x = points[i2].x - dmOutX
                    temp[outVtxIdx + 3].y = points[i2].y - dmOutY

                    // Add indexes
                    idxBuffer += idx2 + 1; idxBuffer += idx1 + 1; idxBuffer += idx1 + 2
                    idxBuffer += idx1 + 2; idxBuffer += idx2 + 2; idxBuffer += idx2 + 1
                    idxBuffer += idx2 + 1; idxBuffer += idx1 + 1; idxBuffer += idx1 + 0
                    idxBuffer += idx1 + 0; idxBuffer += idx2 + 0; idxBuffer += idx2 + 1
                    idxBuffer += idx2 + 2; idxBuffer += idx1 + 2; idxBuffer += idx1 + 3
                    idxBuffer += idx1 + 3; idxBuffer += idx2 + 3; idxBuffer += idx2 + 2
                    _idxWritePtr += 18

                    idx1 = idx2
                }

                // Add vertexes
                for (i in 0 until points.size) {
                    vtxBuffer += temp[tempPointsIdx + i * 4 + 0]; vtxBuffer += uv; vtxBuffer += colTrans
                    vtxBuffer += temp[tempPointsIdx + i * 4 + 1]; vtxBuffer += uv; vtxBuffer += col
                    vtxBuffer += temp[tempPointsIdx + i * 4 + 2]; vtxBuffer += uv; vtxBuffer += col
                    vtxBuffer += temp[tempPointsIdx + i * 4 + 3]; vtxBuffer += uv; vtxBuffer += colTrans
                    _vtxWritePtr += 4
                }
            }
            _vtxCurrentIdx += vtxCount
        } else {
            // Non Anti-aliased Stroke
            val idxCount = count * 6
            val vtxCount = count * 4      // FIXME-OPT: Not sharing edges
            primReserve(idxCount, vtxCount)
            vtxBuffer.pos = _vtxWritePtr
            idxBuffer.pos = _idxWritePtr

            for (i1 in 0 until count) {
                val i2 = if ((i1 + 1) == points.size) 0 else i1 + 1
                val p1 = points[i1]
                val p2 = points[i2]

                var dX = p2.x - p1.x
                var dY = p2.y - p1.y
//                IM_NORMALIZE2F_OVER_ZERO(dX, dY)
                val d2 = dX * dX + dY * dY
                if (d2 > 0f) {
                    val invLen = 1f / sqrt(d2)
                    dX *= invLen
                    dY *= invLen
                }
                dX *= thickness * 0.5f
                dY *= thickness * 0.5f

                vtxBuffer += p1.x + dY; vtxBuffer += p1.y - dX; vtxBuffer += uv; vtxBuffer += col
                vtxBuffer += p2.x + dY; vtxBuffer += p2.y - dX; vtxBuffer += uv; vtxBuffer += col
                vtxBuffer += p2.x - dY; vtxBuffer += p2.y + dX; vtxBuffer += uv; vtxBuffer += col
                vtxBuffer += p1.x - dY; vtxBuffer += p1.y + dX; vtxBuffer += uv; vtxBuffer += col
                _vtxWritePtr += 4

                idxBuffer += _vtxCurrentIdx; idxBuffer += _vtxCurrentIdx + 1; idxBuffer += _vtxCurrentIdx + 2
                idxBuffer += _vtxCurrentIdx; idxBuffer += _vtxCurrentIdx + 2; idxBuffer += _vtxCurrentIdx + 3
                _idxWritePtr += 6
                _vtxCurrentIdx += 4
            }
        }
        vtxBuffer.pos = 0
        idxBuffer.pos = 0
    }

    /** We intentionally avoid using ImVec2 and its math operators here to reduce cost to a minimum for debug/non-inlined builds.
     *
     *  Note: Anti-aliased filling requires points to be in clockwise order. */
    fun addConvexPolyFilled(points: ArrayList<Vec2>, col: Int) {

        if (points.size < 3)
            return

        val uv = Vec2(_data.texUvWhitePixel)

        if (flags has DrawListFlag.AntiAliasedFill) {
            // Anti-aliased Fill
            val AA_SIZE = 1f
            val colTrans = col wo COL32_A_MASK
            val idxCount = (points.size - 2) * 3 + points.size * 6
            val vtxCount = points.size * 2
            primReserve(idxCount, vtxCount)
            vtxBuffer.pos = _vtxWritePtr
            idxBuffer.pos = _idxWritePtr

            // Add indexes for fill
            val vtxInnerIdx = _vtxCurrentIdx
            val vtxOuterIdx = _vtxCurrentIdx + 1
            for (i in 2 until points.size) {
                idxBuffer += vtxInnerIdx; idxBuffer += vtxInnerIdx + ((i - 1) shl 1); idxBuffer += vtxInnerIdx + (i shl 1)
                _idxWritePtr += 3
            }

            // Compute normals
            val tempNormals = Array(points.size) { Vec2() }
            var i0 = points.lastIndex
            var i1 = 0
            while (i1 < points.size) {
                val p0 = points[i0]
                val p1 = points[i1]
                var dX = p1.x - p0.x
                var dY = p1.y - p0.y
//                IM_NORMALIZE2F_OVER_ZERO(dx, dy)
                val d2 = dX * dX + dY * dY
                if (d2 > 0f) {
                    val invLen = 1f / sqrt(d2)
                    dX *= invLen
                    dY *= invLen
                }
                tempNormals[i0].x = dY
                tempNormals[i0].y = -dX
                i0 = i1++
            }

            i0 = points.lastIndex
            i1 = 0
            while (i1 < points.size) {
                // Average normals
                val n0 = tempNormals[i0]
                val n1 = tempNormals[i1]
                var dmX = (n0.x + n1.x) * 0.5f
                var dmY = (n0.y + n1.y) * 0.5f
//                    IM_FIXNORMAL2F(dm_x, dm_y)
                run {
                    var d2 = dmX * dmX + dmY * dmY
                    if (d2 < 0.5f)
                        d2 = 0.5f
                    val invLensq = 1f / d2
                    dmX *= invLensq
                    dmY *= invLensq
                }
                dmX *= AA_SIZE * 0.5f
                dmY *= AA_SIZE * 0.5f

                // Add vertices
                vtxBuffer += points[i1].x - dmX; vtxBuffer += points[i1].y - dmY; vtxBuffer += uv; vtxBuffer += col      // Inner
                vtxBuffer += points[i1].x + dmX; vtxBuffer += points[i1].y + dmY; vtxBuffer += uv; vtxBuffer += colTrans // Outer
                _vtxWritePtr += 2

                // Add indexes for fringes
                idxBuffer += vtxInnerIdx + (i1 shl 1); idxBuffer += vtxInnerIdx + (i0 shl 1); idxBuffer += vtxOuterIdx + (i0 shl 1)
                idxBuffer += vtxOuterIdx + (i0 shl 1); idxBuffer += vtxOuterIdx + (i1 shl 1); idxBuffer += vtxInnerIdx + (i1 shl 1)
                _idxWritePtr += 6

                i0 = i1++
            }
            _vtxCurrentIdx += vtxCount
        } else {
            // Non Anti-aliased Fill
            val idxCount = (points.size - 2) * 3
            val vtxCount = points.size
            primReserve(idxCount, vtxCount)
            vtxBuffer.pos = _vtxWritePtr
            idxBuffer.pos = _idxWritePtr
            for (i in 0 until vtxCount) {
                vtxBuffer += points[i]; vtxBuffer += uv; vtxBuffer += col
                _vtxWritePtr++
            }
            for (i in 2 until points.size) {
                idxBuffer += _vtxCurrentIdx; idxBuffer += _vtxCurrentIdx + i - 1; idxBuffer[_idxWritePtr + 2] = _vtxCurrentIdx + i
                _idxWritePtr += 3
            }
            _vtxCurrentIdx += vtxCount
        }
        vtxBuffer.pos = 0
        idxBuffer.pos = 0
    }

    /** Cubic Bezier takes 4 controls points */
    fun addBezierCurve(p1: Vec2, p2: Vec2, p3: Vec2, p4: Vec2, col: Int, thickness: Float, numSegments: Int = 0) {
        if (col hasnt COL32_A_MASK) return

        pathLineTo(p1)
        pathBezierCurveTo(p2, p3, p4, numSegments)
        pathStroke(col, false, thickness)
    }


    // -----------------------------------------------------------------------------------------------------------------
    // Image primitives
    // - Read FAQ to understand what ImTextureID is.
    // - "p_min" and "p_max" represent the upper-left and lower-right corners of the rectangle.
    // - "uv_min" and "uv_max" represent the normalized texture coordinates to use for those corners. Using (0,0)->(1,1) texture coordinates will generally display the entire texture.
    // -----------------------------------------------------------------------------------------------------------------

    fun addImage(userTextureId: TextureID, pMin: Vec2, pMax: Vec2,
                 uvMin: Vec2 = Vec2(0), uvMax: Vec2 = Vec2(1), col: Int = COL32_WHITE) {

        if (col hasnt COL32_A_MASK) return

        val pushTextureId = _textureIdStack.isEmpty() || userTextureId != _textureIdStack.last()
        if (pushTextureId) pushTextureId(userTextureId)

        primReserve(6, 4)
        primRectUV(pMin, pMax, uvMin, uvMax, col)

        if (pushTextureId) popTextureId()
    }

    fun addImageQuad(userTextureId: TextureID, p1: Vec2, p2: Vec2, p3: Vec2, p4: Vec2,
                     uv1: Vec2 = Vec2(0), uv2: Vec2 = Vec2(1, 0),
                     uv3: Vec2 = Vec2(1), uv4: Vec2 = Vec2(0, 1), col: Int = COL32_WHITE) {

        if (col hasnt COL32_A_MASK) return

        val pushTextureId = _textureIdStack.isEmpty() || userTextureId != _textureIdStack.last()
        if (pushTextureId)
            pushTextureId(userTextureId)

        primReserve(6, 4)
        primQuadUV(p1, p2, p3, p4, uv1, uv2, uv3, uv4, col)

        if (pushTextureId)
            popTextureId()
    }

    fun addImageRounded(userTextureId: TextureID, pMin: Vec2, pMax: Vec2, uvMin: Vec2, uvMax: Vec2, col: Int, rounding: Float,
                        roundingCorners: DrawCornerFlags = DrawCornerFlag.All.i) {
        if (col hasnt COL32_A_MASK) return

        if (rounding <= 0f || roundingCorners hasnt DrawCornerFlag.All) {
            addImage(userTextureId, pMin, pMax, uvMin, uvMax, col)
            return
        }

        val pushTextureId = _textureIdStack.isEmpty() || userTextureId != _textureIdStack.last()
        if (pushTextureId) pushTextureId(userTextureId)

        val vertStartIdx = vtxBuffer.size
        pathRect(pMin, pMax, rounding, roundingCorners)
        pathFillConvex(col)
        val vertEndIdx = vtxBuffer.size
        shadeVertsLinearUV(vertStartIdx, vertEndIdx, pMin, pMax, uvMin, uvMax, true)

        if (pushTextureId) popTextureId()
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Stateful path API, add points then finish with PathFillConvex() or PathStroke()
    // -----------------------------------------------------------------------------------------------------------------

    fun pathClear() = _path.clear()

    fun pathLineTo(pos: Vec2) = _path.add(pos)

    fun pathLineToMergeDuplicate(pos: Vec2) {
        if (_path.isEmpty() || _path.last() != pos) _path += pos
    }

    /** Note: Anti-aliased filling requires points to be in clockwise order. */
    fun pathFillConvex(col: Int) = addConvexPolyFilled(_path, col).also { pathClear() }

    /** rounding_corners_flags: 4 bits corresponding to which corner to round   */
    fun pathStroke(col: Int, closed: Boolean, thickness: Float = 1.0f) = addPolyline(_path, col, closed, thickness).also { pathClear() }

    fun pathArcTo(center: Vec2, radius: Float, aMin: Float, aMax: Float, numSegments: Int = 10) {
        if (radius == 0f) {
            _path += center
            return
        }
        // Note that we are adding a point at both a_min and a_max.
        // If you are trying to draw a full closed circle you don't want the overlapping points!
        for (i in 0..numSegments) {
            val a = aMin + (i.f / numSegments) * (aMax - aMin)
            _path += Vec2(center.x + glm.cos(a) * radius, center.y + glm.sin(a) * radius)
        }
    }

    /** Use precomputed angles for a 12 steps circle    */
    fun pathArcToFast(center: Vec2, radius: Float, aMinOf12: Int, aMaxOf12: Int) {

        if (radius == 0f || aMinOf12 > aMaxOf12) {
            _path += center
            return
        }
        for (a in aMinOf12..aMaxOf12) {
            val c = _data.circleVtx12[a % _data.circleVtx12.size]
            _path += Vec2(center.x + c.x * radius, center.y + c.y * radius)
        }
    }

    fun pathBezierCurveTo(p2: Vec2, p3: Vec2, p4: Vec2, numSegments: Int = 0) {

        val p1 = _path.last()
        if (numSegments == 0) // Auto-tessellated
            pathBezierToCasteljau(_path, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, p4.x, p4.y, style.curveTessellationTol, 0)
        else {
            val tStep = 1f / numSegments
            for (iStep in 1..numSegments)
                _path += bezierCalc(p1, p2, p3, p4, tStep * iStep)
        }
    }

    /** Closely mimics BezierClosestPointCasteljauStep() in imgui.cpp */
    private fun pathBezierToCasteljau(path: ArrayList<Vec2>, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float, tess_tol: Float, level: Int) {
        val dx = x4 - x1
        val dy = y4 - y1
        var d2 = ((x2 - x4) * dy - (y2 - y4) * dx)
        var d3 = ((x3 - x4) * dy - (y3 - y4) * dx)
        d2 = if (d2 >= 0) d2 else -d2
        d3 = if (d3 >= 0) d3 else -d3
        if ((d2 + d3) * (d2 + d3) < tess_tol * (dx * dx + dy * dy)) {
            path.add(Vec2(x4, y4))
        } else if (level < 10) {
            val x12 = (x1 + x2) * 0.5f
            val y12 = (y1 + y2) * 0.5f
            val x23 = (x2 + x3) * 0.5f
            val y23 = (y2 + y3) * 0.5f
            val x34 = (x3 + x4) * 0.5f
            val y34 = (y3 + y4) * 0.5f
            val x123 = (x12 + x23) * 0.5f
            val y123 = (y12 + y23) * 0.5f
            val x234 = (x23 + x34) * 0.5f
            val y234 = (y23 + y34) * 0.5f
            val x1234 = (x123 + x234) * 0.5f
            val y1234 = (y123 + y234) * 0.5f
            pathBezierToCasteljau(path, x1, y1, x12, y12, x123, y123, x1234, y1234, tess_tol, level + 1)
            pathBezierToCasteljau(path, x1234, y1234, x234, y234, x34, y34, x4, y4, tess_tol, level + 1)
        }
    }

    fun pathRect(a: Vec2, b: Vec2, rounding_: Float = 0f, roundingCorners: DrawCornerFlags = DrawCornerFlag.All.i) {

        var cond = ((roundingCorners and DrawCornerFlag.Top) == DrawCornerFlag.Top.i) || ((roundingCorners and DrawCornerFlag.Bot) == DrawCornerFlag.Bot.i) // TODO consider simplyfing
        var rounding = glm.min(rounding_, glm.abs(b.x - a.x) * (if (cond) 0.5f else 1f) - 1f)
        cond = ((roundingCorners and DrawCornerFlag.Left) == DrawCornerFlag.Left.i) || ((roundingCorners and DrawCornerFlag.Right) == DrawCornerFlag.Right.i)
        rounding = glm.min(rounding, glm.abs(b.y - a.y) * (if (cond) 0.5f else 1f) - 1f)

        if (rounding <= 0f || roundingCorners == 0) {
            pathLineTo(a)
            pathLineTo(Vec2(b.x, a.y))
            pathLineTo(b)
            pathLineTo(Vec2(a.x, b.y))
        } else {
            val roundingTL = if (roundingCorners has DrawCornerFlag.TopLeft) rounding else 0f
            val roundingTR = if (roundingCorners has DrawCornerFlag.TopRight) rounding else 0f
            val roundingBR = if (roundingCorners has DrawCornerFlag.BotRight) rounding else 0f
            val roundingBL = if (roundingCorners has DrawCornerFlag.BotLeft) rounding else 0f
            pathArcToFast(Vec2(a.x + roundingTL, a.y + roundingTL), roundingTL, 6, 9)
            pathArcToFast(Vec2(b.x - roundingTR, a.y + roundingTR), roundingTR, 9, 12)
            pathArcToFast(Vec2(b.x - roundingBR, b.y - roundingBR), roundingBR, 0, 3)
            pathArcToFast(Vec2(a.x + roundingBL, b.y - roundingBL), roundingBL, 3, 6)
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Advanced
    // -----------------------------------------------------------------------------------------------------------------
    /** Your rendering function must check for 'UserCallback' in ImDrawCmd and call the function instead of rendering
    triangles.  */
    fun addCallback(callback: DrawCallback, callbackData: ByteBuffer? = null) {
        var currentCmd = cmdBuffer.peek()
        if (currentCmd == null || currentCmd.elemCount != 0 || currentCmd.userCallback != null) {
            addDrawCmd()
            currentCmd = cmdBuffer.peek()
        }

        currentCmd.userCallback = callback
        currentCmd.userCallbackData = callbackData
        addDrawCmd() // Force a new command after us (see comment below)
    }

    /** This is useful if you need to forcefully create a new draw call (to allow for dependent rendering / blending).
    Otherwise primitives are merged into the same draw-call as much as possible */
    fun addDrawCmd() {
        val drawCmd = DrawCmd().apply {
            clipRect put currentClipRect
            textureId = currentTextureId
            vtxOffset = _vtxCurrentOffset
            idxOffset = idxBuffer.rem
        }
        assert(drawCmd.clipRect.x <= drawCmd.clipRect.z && drawCmd.clipRect.y <= drawCmd.clipRect.w)
        cmdBuffer.add(drawCmd)
    }

    /** Create a clone of the CmdBuffer/IdxBuffer/VtxBuffer. */
    fun cloneOutput(): DrawData? = drawData?.clone()

    // -----------------------------------------------------------------------------------------------------------------
    // Advanced: Channels
    // - Use to split render into layers. By switching channels to can render out-of-order (e.g. submit FG primitives before BG primitives)
    // - Use to minimize draw calls (e.g. if going back-and-forth between multiple clipping rectangles, prefer to append into separate channels then merge at the end)
    // - FIXME-OBSOLETE: This API shouldn't have been in ImDrawList in the first place!
    //   Prefer using your own persistent copy of ImDrawListSplitter as you can stack them.
    //   Using the ImDrawList::ChannelsXXXX you cannot stack a split over another.nels then merge at the end)
    // -----------------------------------------------------------------------------------------------------------------

    fun channelsSplit(count: Int) = _splitter.split(this, count)

    fun channelsMerge() = _splitter.merge(this)

    fun channelsSetCurrent(idx: Int) = _splitter.setCurrentChannel(this, idx)

    // -----------------------------------------------------------------------------------------------------------------
    // Internal helpers
    // NB: all primitives needs to be reserved via PrimReserve() beforehand!
    // -----------------------------------------------------------------------------------------------------------------
    fun clear() {
        cmdBuffer.clear()
        idxBuffer.resize(0) // we dont assign because it wont create a new istance for sure
        vtxBuffer.resize(0)
        flags = _data.initialFlags
        _vtxCurrentOffset = 0
        _vtxCurrentIdx = 0
        _vtxWritePtr = 0
        _idxWritePtr = 0
        _clipRectStack.clear()
        _textureIdStack.clear()
        _path.clear()
        _splitter.clear()
    }

    fun clearFreeMemory() {
        clear()
        _splitter.clearFreeMemory()
        vtxBuffer.data.free()
        idxBuffer.free()
    }

    /** Reserve space for a number of vertices and indices.
     *  You must finish filling your reserved data before calling PrimReserve() again, as it may reallocate or
     *  submit the intermediate results. PrimUnreserve() can be used to release unused allocations.    */
    fun primReserve(idxCount: Int, vtxCount: Int) {

        // Large mesh support (when enabled)
        ASSERT_PARANOID(idxCount >= 0 && vtxCount >= 0)
        if (DrawIdx.BYTES == 2 && _vtxCurrentIdx + vtxCount >= (1 shl 16) && flags has DrawListFlag.AllowVtxOffset) {
            _vtxCurrentOffset = vtxBuffer.rem
            _vtxCurrentIdx = 0
            addDrawCmd()
        }

        cmdBuffer.last().elemCount += idxCount

        val vtxBufferOldSize = vtxBuffer.size
        vtxBuffer = vtxBuffer.resize(vtxBufferOldSize + vtxCount)
        _vtxWritePtr = vtxBufferOldSize

        val idxBufferOldSize = idxBuffer.lim
        idxBuffer = idxBuffer.resize(idxBufferOldSize + idxCount)
        _idxWritePtr = idxBufferOldSize
    }

    /** Release the a number of reserved vertices/indices from the end of the last reservation made with PrimReserve(). */
    fun primUnreserve(idxCount: Int, vtxCount: Int) {

        ASSERT_PARANOID(idxCount >= 0 && vtxCount >= 0)

        val drawCmd = cmdBuffer.last()
        drawCmd.elemCount -= idxCount
        vtxBuffer shrink (vtxBuffer.size - vtxCount)
        idxBuffer shrink (idxBuffer.rem - idxCount)
    }

    /** Fully unrolled with inline call to keep our debug builds decently fast.
    Axis aligned rectangle (composed of two triangles)  */
    fun primRect(a: Vec2, c: Vec2, col: Int) {

        vtxBuffer.pos = _vtxWritePtr
        idxBuffer.pos = _idxWritePtr

        val b = Vec2(c.x, a.y)
        val d = Vec2(a.x, c.y)
        val uv = Vec2(_data.texUvWhitePixel)
        val idx = _vtxCurrentIdx
        idxBuffer += idx; idxBuffer += idx + 1; idxBuffer += idx + 2
        idxBuffer += idx; idxBuffer += idx + 2; idxBuffer += idx + 3
        vtxBuffer += a; vtxBuffer += uv; vtxBuffer += col
        vtxBuffer += b; vtxBuffer += uv; vtxBuffer += col
        vtxBuffer += c; vtxBuffer += uv; vtxBuffer += col
        vtxBuffer += d; vtxBuffer += uv; vtxBuffer += col
        _vtxWritePtr += 4
        _vtxCurrentIdx += 4
        _idxWritePtr += 6

        vtxBuffer.pos = 0
        idxBuffer.pos = 0
    }

    fun primRectUV(a: Vec2, c: Vec2, uvA: Vec2, uvC: Vec2, col: Int) {

        vtxBuffer.pos = _vtxWritePtr
        idxBuffer.pos = _idxWritePtr

        val b = Vec2(c.x, a.y)
        val d = Vec2(a.x, c.y)
        val uvB = Vec2(uvC.x, uvA.y)
        val uvD = Vec2(uvA.x, uvC.y)
        val idx = _vtxCurrentIdx
        idxBuffer += idx; idxBuffer += idx + 1; idxBuffer += idx + 2
        idxBuffer += idx; idxBuffer += idx + 2; idxBuffer += idx + 3
        vtxBuffer += a; vtxBuffer += uvA; vtxBuffer += col
        vtxBuffer += b; vtxBuffer += uvB; vtxBuffer += col
        vtxBuffer += c; vtxBuffer += uvC; vtxBuffer += col
        vtxBuffer += d; vtxBuffer += uvD; vtxBuffer += col
        _vtxWritePtr += 4
        _vtxCurrentIdx += 4
        _idxWritePtr += 6

        vtxBuffer.pos = 0
        idxBuffer.pos = 0
    }

// On AddPolyline() and AddConvexPolyFilled() we intentionally avoid using ImVec2 and superflous function calls to optimize debug/non-inlined builds.
// Those macros expects l-values.
//    fun NORMALIZE2F_OVER_ZERO(vX: Float, vY: Float) {
//        val d2 = vX * vX + vY * vY
//        if (d2 > 0.0f) {
//            val invLen = 1f / sqrt(d2)
//            vX *= invLen
//            vY *= invLen
//        }
//    }
//
//    fun NORMALIZE2F_OVER_EPSILON_CLAMP(vX: Float, vY: Float, eps: Float, invLenMax: Float) {
//        val d2 = vX * vX + vY * vY
//        if (d2 > eps) {
//            var invLen = 1f / sqrt(d2)
//            if (invLen > invLenMax)
//                invLen = invLenMax
//            vX *= invLen
//            vY *= invLen
//        }
//    }

    fun primQuadUV(a: Vec2, b: Vec2, c: Vec2, d: Vec2, uvA: Vec2, uvB: Vec2, uvC: Vec2, uvD: Vec2, col: Int) {

        vtxBuffer.pos = _vtxWritePtr
        idxBuffer.pos = _idxWritePtr

        val idx = _vtxCurrentIdx
        idxBuffer += idx; idxBuffer += idx + 1; idxBuffer += idx + 2
        idxBuffer += idx; idxBuffer += idx + 2; idxBuffer += idx + 3
        vtxBuffer += a; vtxBuffer += uvA; vtxBuffer += col
        vtxBuffer += b; vtxBuffer += uvB; vtxBuffer += col
        vtxBuffer += c; vtxBuffer += uvC; vtxBuffer += col
        vtxBuffer += d; vtxBuffer += uvD; vtxBuffer += col
        _vtxWritePtr += 4
        _vtxCurrentIdx += 4
        _idxWritePtr += 6

        vtxBuffer.pos = 0
        idxBuffer.pos = 0
    }

    fun primWriteVtx(pos: Vec2, uv: Vec2, col: Int) {

        vtxBuffer.pos = _vtxWritePtr

        vtxBuffer += pos; vtxBuffer += uv; vtxBuffer += col
        _vtxWritePtr++
        _vtxCurrentIdx++

        vtxBuffer.pos = 0
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
    val currentClipRect: Vec4
        get() = _clipRectStack.lastOrNull() ?: _data.clipRectFullscreen
    val currentTextureId: TextureID?
        get() = _textureIdStack.lastOrNull()

    //-------------------------------------------------------------------------
    // [SECTION] FORWARD DECLARATIONS
    //-------------------------------------------------------------------------

    /** AddDrawListToDrawData */
    infix fun addTo(outList: ArrayList<DrawList>) {

        if (cmdBuffer.empty()) return

        // Remove trailing command if unused
        val lastCmd = cmdBuffer.last()
        if (lastCmd.elemCount == 0 && lastCmd.userCallback == null) {
            cmdBuffer.pop()
            if (cmdBuffer.empty()) return
        }

        /*  Draw list sanity check. Detect mismatch between PrimReserve() calls and incrementing _VtxCurrentIdx, _VtxWritePtr etc.
            May trigger for you if you are using PrimXXX functions incorrectly.   */
        assert(vtxBuffer.rem == 0 || _vtxWritePtr == vtxBuffer.rem)
        assert(idxBuffer.rem == 0 || _idxWritePtr == idxBuffer.rem)
        if (flags hasnt DrawListFlag.AllowVtxOffset)
            assert(_vtxCurrentIdx == vtxBuffer.rem)

        // JVM ImGui, this doesnt apply, we use Ints by default
        /*  Check that drawList doesn't use more vertices than indexable
            (default DrawIdx = unsigned short = 2 bytes = 64K vertices per DrawList = per window)
            If this assert triggers because you are drawing lots of stuff manually:
            - First, make sure you are coarse clipping yourself and not trying to draw many things outside visible bounds.
              Be mindful that the ImDrawList API doesn't filter vertices. Use the Metrics window to inspect draw list contents.
            - If you want large meshes with more than 64K vertices, you can either:
              (A) Handle the ImDrawCmd::VtxOffset value in your renderer back-end, and set 'io.BackendFlags |= ImGuiBackendFlags_RendererHasVtxOffset'.
                  Most example back-ends already support this from 1.71. Pre-1.71 back-ends won't.
                  Some graphics API such as GL ES 1/2 don't have a way to offset the starting vertex so it is not supported for them.
              (B) Or handle 32-bits indices in your renderer back-end, and uncomment '#define ImDrawIdx unsigned int' line in imconfig.h.
                  Most example back-ends already support this. For example, the OpenGL example code detect index size at compile-time:
                    glDrawElements(GL_TRIANGLES, (GLsizei)pcmd->ElemCount, sizeof(ImDrawIdx) == 2 ? GL_UNSIGNED_SHORT : GL_UNSIGNED_INT, idx_buffer_offset);
                  Your own engine or render API may use different parameters or function calls to specify index sizes.
                  2 and 4 bytes indices are generally supported by most graphics API.
            - If for some reason neither of those solutions works for you, a workaround is to call BeginChild()/EndChild() before reaching
              the 64K limit to split your draw commands in multiple draw lists.         */
        outList += this
        io.metricsRenderVertices += vtxBuffer.rem
        io.metricsRenderIndices += idxBuffer.rem
    }

    // Internal Api, Render helpers

    /** Render an arrow aimed to be aligned with text (p_min is a position in the same space text would be positioned). To e.g. denote expanded/collapsed state  */
    fun renderArrow(pos: Vec2, col: Int, dir: Dir, scale: Float = 1f) {

        val h = _data.fontSize * 1f
        var r = h * 0.4f * scale
        val center = pos + Vec2(h * 0.5f, h * 0.5f * scale)

        val a: Vec2
        val b: Vec2
        val c: Vec2
        when (dir) {
            Dir.Up, Dir.Down -> {
                if (dir == Dir.Up) r = -r
                a = Vec2(+0.000f, +0.75f) * r
                b = Vec2(-0.866f, -0.75f) * r
                c = Vec2(+0.866f, -0.75f) * r
            }
            Dir.Left, Dir.Right -> {
                if (dir == Dir.Left) r = -r
                a = Vec2(+0.75f, +0.000f) * r
                b = Vec2(-0.75f, +0.866f) * r
                c = Vec2(-0.75f, -0.866f) * r
            }
            else -> throw Error()
        }

        addTriangleFilled(center + a, center + b, center + c, col)
    }

    fun renderBullet(pos: Vec2, col: Int) = addCircleFilled(pos, _data.fontSize * 0.2f, col, 8)

    fun renderMouseCursor(pos: Vec2, scale: Float, mouseCursor: MouseCursor,
                          colFill: Int, colBorder: Int, colShadow: Int) {
        if (mouseCursor == MouseCursor.None)
            return

        val fontAtlas = _data.font!!.containerAtlas
        val offset = Vec2()
        val size = Vec2()
        val uv = Array(4) { Vec2() }
        if (fontAtlas.getMouseCursorTexData(mouseCursor, offset, size, uv)) {
            pos -= offset
            val texId: TextureID = fontAtlas.texID
            pushTextureId(texId)
            addImage(texId, pos + Vec2(1, 0) * scale, pos + Vec2(1, 0) * scale + size * scale, uv[2], uv[3], colShadow)
            addImage(texId, pos + Vec2(2, 0) * scale, pos + Vec2(2, 0) * scale + size * scale, uv[2], uv[3], colShadow)
            addImage(texId, pos, pos + size * scale, uv[2], uv[3], colBorder)
            addImage(texId, pos, pos + size * scale, uv[0], uv[1], colFill)
            popTextureId()
        }
    }

    /** Render an arrow. 'pos' is position of the arrow tip. halfSz.x is length from base to tip. halfSz.y is length on each side. */
    fun renderArrowPointingAt(pos: Vec2, halfSz: Vec2, direction: Dir, col: Int) =
            when (direction) {
                Dir.Left -> addTriangleFilled(Vec2(pos.x + halfSz.x, pos.y - halfSz.y), Vec2(pos.x + halfSz.x, pos.y + halfSz.y), pos, col)
                Dir.Right -> addTriangleFilled(Vec2(pos.x - halfSz.x, pos.y + halfSz.y), Vec2(pos.x - halfSz.x, pos.y - halfSz.y), pos, col)
                Dir.Up -> addTriangleFilled(Vec2(pos.x + halfSz.x, pos.y + halfSz.y), Vec2(pos.x - halfSz.x, pos.y + halfSz.y), pos, col)
                Dir.Down -> addTriangleFilled(Vec2(pos.x - halfSz.x, pos.y - halfSz.y), Vec2(pos.x + halfSz.x, pos.y - halfSz.y), pos, col)
                else -> Unit
            }

    /** FIXME: Cleanup and move code to ImDrawList. */
    fun renderRectFilledRangeH(rect: Rect, col: Int, xStartNorm_: Float, xEndNorm_: Float, rounding_: Float) {
        var xStartNorm = xStartNorm_
        var xEndNorm = xEndNorm_
        if (xEndNorm == xStartNorm) return
        if (xStartNorm > xEndNorm) {
            val tmp = xStartNorm
            xStartNorm = xEndNorm
            xEndNorm = tmp
        }
        val p0 = Vec2(lerp(rect.min.x, rect.max.x, xStartNorm), rect.min.y)
        val p1 = Vec2(lerp(rect.min.x, rect.max.x, xEndNorm), rect.max.y)
        if (rounding_ == 0f) {
            addRectFilled(p0, p1, col, 0f)
            return
        }
        val rounding = glm.clamp(glm.min((rect.max.x - rect.min.x) * 0.5f, (rect.max.y - rect.min.y) * 0.5f) - 1f, 0f, rounding_)
        val invRounding = 1f / rounding
        val arc0B = acos01(1f - (p0.x - rect.min.x) * invRounding)
        val arc0E = acos01(1f - (p1.x - rect.min.x) * invRounding)
        val halfPI = glm.HPIf // We will == compare to this because we know this is the exact value ImAcos01 can return.
        val x0 = glm.max(p0.x, rect.min.x + rounding)
        if (arc0B == arc0E) {
            pathLineTo(Vec2(x0, p1.y))
            pathLineTo(Vec2(x0, p0.y))
        } else if (arc0B == 0f && arc0E == halfPI) {
            pathArcToFast(Vec2(x0, p1.y - rounding), rounding, 3, 6) // BL
            pathArcToFast(Vec2(x0, p0.y + rounding), rounding, 6, 9) // TR
        } else {
            pathArcTo(Vec2(x0, p1.y - rounding), rounding, glm.PIf - arc0E, glm.PIf - arc0B, 3) // BL
            pathArcTo(Vec2(x0, p0.y + rounding), rounding, glm.PIf + arc0B, glm.PIf + arc0E, 3) // TR
        }
        if (p1.x > rect.min.x + rounding) {
            val arc1B = acos01(1f - (rect.max.x - p1.x) * invRounding)
            val arc1E = acos01(1f - (rect.max.x - p0.x) * invRounding)
            val x1 = glm.min(p1.x, rect.max.x - rounding)
            if (arc1B == arc1E) {
                pathLineTo(Vec2(x1, p0.y))
                pathLineTo(Vec2(x1, p1.y))
            } else if (arc1B == 0f && arc1E == halfPI) {
                pathArcToFast(Vec2(x1, p0.y + rounding), rounding, 9, 12) // TR
                pathArcToFast(Vec2(x1, p1.y - rounding), rounding, 0, 3)  // BR
            } else {
                pathArcTo(Vec2(x1, p0.y + rounding), rounding, -arc1E, -arc1B, 3) // TR
                pathArcTo(Vec2(x1, p1.y - rounding), rounding, +arc1B, +arc1E, 3) // BR
            }
        }
        pathFillConvex(col)
    }

    // Internal API, Shade functions
    //-----------------------------------------------------------------------------
    // Shade functions (write over already created vertices)
    //-----------------------------------------------------------------------------

    /** Generic linear color gradient, write to RGB fields, leave A untouched.  */
    fun shadeVertsLinearColorGradientKeepAlpha(vertStart: Int, vertEnd: Int, gradientP0: Vec2,
                                               gradientP1: Vec2, col0: Int, col1: Int) {
        val gradientExtent = gradientP1 - gradientP0
        val gradientInvLength2 = 1f / gradientExtent.lengthSqr
        for (i in vertStart until vertEnd) {
            var offset = i * DrawVert.size
            val pos = Vec2(vtxBuffer.data, offset)
            val d = pos - gradientP0 dot gradientExtent
            val t = glm.clamp(d * gradientInvLength2, 0f, 1f)
            val r = lerp((col0 ushr COL32_R_SHIFT) and 0xFF, (col1 ushr COL32_R_SHIFT) and 0xFF, t)
            val g = lerp((col0 ushr COL32_G_SHIFT) and 0xFF, (col1 ushr COL32_G_SHIFT) and 0xFF, t)
            val b = lerp((col0 ushr COL32_B_SHIFT) and 0xFF, (col1 ushr COL32_B_SHIFT) and 0xFF, t)
            offset += Vec2.size * 2
            val col = vtxBuffer.data.getInt(offset)
            val newCol = (r shl COL32_R_SHIFT) or (g shl COL32_G_SHIFT) or (b shl COL32_B_SHIFT) or (col and COL32_A_MASK)
            vtxBuffer.data.putInt(offset, newCol)
        }
    }

    /** Distribute UV over (a, b) rectangle */
    fun shadeVertsLinearUV(vertStart: Int, vertEnd: Int, a: Vec2, b: Vec2, uvA: Vec2, uvB: Vec2, clamp: Boolean) {
        val size = b - a
        val uvSize = uvB - uvA
        val scale = Vec2(
                if (size.x != 0f) uvSize.x / size.x else 0f,
                if (size.y != 0f) uvSize.y / size.y else 0f)
        if (clamp) {
            val min = uvA min uvB
            val max = uvA max uvB
            for (i in vertStart until vertEnd) {
                val vertexPos = Vec2(vtxBuffer.data, i * DrawVert.size)
                val vertexUV = glm.clamp(uvA + (vertexPos - a) * scale, min, max)
                vertexUV.to(vtxBuffer.data, i * DrawVert.size + Vec2.size)
            }
        } else
            for (i in vertStart until vertEnd) {
                val vertexPos = Vec2(vtxBuffer.data, i * DrawVert.size)
                val vertexUV = uvA + (vertexPos - a) * scale
                vertexUV.to(vtxBuffer.data, i * DrawVert.size + Vec2.size)
            }
    }

    companion object {
        private fun acos01(x: Float) = when {
            x <= 0f -> glm.PIf * 0.5f
            x >= 1f -> 0f
            else -> glm.acos(x)
            //return (-0.69813170079773212f * x * x - 0.87266462599716477f) * x + 1.5707963267948966f; // Cheap approximation, may be enough for what we do.
        }
    }
}

private fun DrawVert_Buffer(size: Int = 0) = DrawVert_Buffer(ByteBuffer(size))
inline class DrawVert_Buffer(val data: ByteBuffer) {

    operator fun get(index: Int) = DrawVert(
            Vec2(data, index * DrawVert.size),
            Vec2(data, index * DrawVert.size + DrawVert.ofsUv),
            data.getInt(index * DrawVert.size + DrawVert.ofsCol))

    operator fun plusAssign(v: Vec2) {
        data.putFloat(v.x)
        data.putFloat(v.y)
    }

    operator fun plusAssign(i: Int) {
        data.putInt(i)
    }

    operator fun plusAssign(f: Float) {
        data.putFloat(f)
    }

    inline val cap: Int
        get() = data.cap / DrawVert.size

    inline var lim: Int
        get() = data.lim / DrawVert.size
        set(value) {
            data.lim = value * DrawVert.size
        }

    inline var pos: Int
        get() = data.pos / DrawVert.size
        set(value) {
            data.pos = value * DrawVert.size
        }

    inline val rem: Int
        get() = data.rem / DrawVert.size

    inline val size: Int
        get() = rem

    fun hasRemaining(): Boolean = rem > 0

    infix fun resize(newSize: Int): DrawVert_Buffer = when {
        newSize > cap -> reserve(growCapacity(newSize))
        else -> this
    }.apply { lim = newSize }

    /** Resize a vector to a smaller size, guaranteed not to cause a reallocation */
    infix fun shrink(newSize: Int) {
        assert(newSize <= cap)
        lim = newSize
    }

    infix fun growCapacity(sz: Int): Int {
        val newCapacity = if (cap > 0) cap + cap / 2 else 8
        return if (newCapacity > sz) newCapacity else sz
    }

    infix fun reserve(newCapacity: Int): DrawVert_Buffer {
        if (newCapacity <= cap)
            return this
        val newData = ByteBuffer(newCapacity * DrawVert.size)
        if (lim > 0)
            MemoryUtil.memCopy(data.adr, newData.adr, data.lim.L)
        data.free()
        return DrawVert_Buffer(newData)
    }
}