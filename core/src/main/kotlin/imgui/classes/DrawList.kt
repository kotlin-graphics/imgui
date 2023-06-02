package imgui.classes

import glm_.hasnt
import glm_.*
import glm_.func.common.abs
import glm_.func.common.max
import glm_.func.cos
import glm_.func.sin
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.drawData
import imgui.ImGui.shadeVertsLinearUV
import imgui.ImGui.style
import imgui.api.g
import imgui.font.Font
import imgui.font.FontAtlas
import imgui.internal.*
import imgui.internal.classes.Rect
import imgui.internal.sections.*
import kool.*
import org.lwjgl.system.MemoryUtil
import uno.kotlin.plusAssign
import java.nio.ByteBuffer
import java.util.Stack
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

/** A single draw command list (generally one per window, conceptually you may see this as a dynamic "mesh" builder)
 *
 *  Draw command list
 *  This is the low-level list of polygons that ImGui:: functions are filling. At the end of the frame,
 *  all command lists are passed to your ImGuiIO::RenderDrawListFn function for rendering.
 *  Each dear imgui window contains its own ImDrawList. You can use ImGui::GetWindowDrawList() to
 *  access the current window draw list and draw custom primitives.
 *  You can interleave normal ImGui:: calls and adding primitives to the current draw list.
 *  In single viewport mode, top-left is == GetMainViewport()->Pos (generally 0,0), bottom-right is == GetMainViewport()->Pos+Size (generally io.DisplaySize).
 *  You are totally free to apply whatever transformation matrix to want to the data (depending on the use of the transformation you may want to apply it to ClipRect as well!)
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
    val cmdBuffer = Stack<DrawCmd>()

    /** Index buffer. Each command consume ImDrawCmd::ElemCount of those    */
    var idxBuffer = IntBuffer(0)

    /** Vertex buffer.  */
    @get:JvmName("getVtxBuffer") // for java users
    var vtxBuffer = DrawVert_Buffer(0)

    /** Flags, you may poke into these to adjust anti-aliasing settings per-primitive. */
    var flags: DrawListFlags = none


    // -----------------------------------------------------------------------------------------------------------------
    // [Internal, used while building lists]
    // -----------------------------------------------------------------------------------------------------------------

    /** [Internal] Generally == VtxBuffer.Size unless we are past 64K vertices, in which case this gets reset to 0. */
    var _vtxCurrentIdx = 0

    /** Pointer to shared draw data (you can use ImGui::drawListSharedData to get the one from current ImGui context) */
    var _data: DrawListSharedData = sharedData ?: DrawListSharedData()

    /** Pointer to owner window's name for debugging    */
    var _ownerName = ""

    /** [Internal] point within VtxBuffer.Data after each add command (to avoid using the ImVector<> operators too much)    */
    var _vtxWritePtr = 0

    /** [Internal] point within IdxBuffer.Data after each add command (to avoid using the ImVector<> operators too much)    */
    var _idxWritePtr = 0

    val _clipRectStack = Stack<Vec4>()

    /** [Internal]  */
    val _textureIdStack = Stack<TextureID>()

    /** [Internal] current path building    */
    val _path = ArrayList<Vec2>()

    /** [Internal] template of active commands. Fields should match those of CmdBuffer.back(). */
    var _cmdHeader = DrawCmdHeader()

    /** [Internal] for channels api (note: prefer using your own persistent instance of ImDrawListSplitter!) */
    val _splitter = DrawListSplitter()

    /** [Internal] anti-alias fringe is scaled by this value, this helps to keep things sharp while zooming at vertex buffer content */
    var _fringeScale = 1f

    /** Render-level scissoring. This is passed down to your render function but not used for CPU-side coarse clipping.
     *  Prefer using higher-level ImGui::PushClipRect() to affect logic (hit-testing and widget culling)    */
    fun pushClipRect(rect: Rect, intersectWithCurrentClipRect: Boolean = false) =
            pushClipRect(rect.min, rect.max, intersectWithCurrentClipRect)

    fun pushClipRect(crMin: Vec2, crMax: Vec2, intersectWithCurrentClipRect: Boolean = false) {

        val cr = Vec4(crMin, crMax)
        if (intersectWithCurrentClipRect) {
            val current = _cmdHeader.clipRect // [JVM] careful, no copy
            if (cr.x < current.x) cr.x = current.x
            if (cr.y < current.y) cr.y = current.y
            if (cr.z > current.z) cr.z = current.z
            if (cr.w > current.w) cr.w = current.w
        }
        cr.z = cr.x max cr.z
        cr.w = cr.y max cr.w

        _clipRectStack += cr
        _cmdHeader.clipRect put cr
        _onChangedClipRect()
    }

    fun pushClipRectFullScreen() = pushClipRect(Vec2(_data.clipRectFullscreen), Vec2(_data.clipRectFullscreen.z, _data.clipRectFullscreen.w))

    /** [JVM] */
    inline fun withClipRect(rect: Rect, intersectWithCurrentClipRect: Boolean = false, block: DrawList.() -> Unit) =
            withClipRect(rect.min, rect.max, intersectWithCurrentClipRect, block)

    /** [JVM] */
    inline fun withClipRect(crMin: Vec2, crMax: Vec2, intersectWithCurrentClipRect: Boolean = false, block: DrawList.() -> Unit) {
        pushClipRect(crMin, crMax, intersectWithCurrentClipRect)
        this.block()
        popClipRect()
    }

    fun popClipRect() {
        _clipRectStack.pop()
        _cmdHeader.clipRect put (_clipRectStack.lastOrNull() ?: _data.clipRectFullscreen)
        _onChangedClipRect()
    }

    infix fun pushTextureID(textureId: TextureID) {
        _textureIdStack += textureId
        _cmdHeader.textureId = textureId
        _onChangedTextureID()
    }

    fun popTextureID() {
        _textureIdStack.pop()
        _cmdHeader.textureId = _textureIdStack.lastOrNull() ?: 0
        _onChangedTextureID()
    }


    // -----------------------------------------------------------------------------------------------------------------
    // Primitives
    // - Filled shapes must always use clockwise winding order. The anti-aliasing fringe depends on it. Counter-clockwise shapes will have "inward" anti-aliasing.
    // - For rectangular primitives, "p_min" and "p_max" represent the upper-left and lower-right corners.
    // - For circle primitives, use "num_segments == 0" to automatically calculate tessellation (preferred).
    //   In older versions (until Dear ImGui 1.77) the AddCircle functions defaulted to num_segments == 12.
    //   In future versions we will use textures to provide cheaper and higher-quality circles.
    //   Use AddNgon() and AddNgonFilled() functions if you need to guarantee a specific number of sides.
    // -----------------------------------------------------------------------------------------------------------------

    /** JVM it's safe to pass directly Vec2 istances, they wont be modified */
    fun addLine(p1: Vec2, p2: Vec2, col: Int, thickness: Float = 1f) {
        if (col hasnt COL32_A_MASK) return
        pathLineTo(p1 + Vec2(0.5f))
        pathLineTo(p2 + Vec2(0.5f))
        pathStroke(col, thickness = thickness)
    }

    /** Note we don't render 1 pixels sized rectangles properly.
     * @param pMin: upper-left
     * @param pMax: lower-right
     * (== upper-left + size)   */
    fun addRect(pMin: Vec2, pMax: Vec2, col: Int, rounding: Float = 0f, flags: DrawFlags = none, thickness: Float = 1f) {
        if (col hasnt COL32_A_MASK) return
        if (this.flags has DrawListFlag.AntiAliasedLines)
            pathRect(pMin + 0.5f, pMax - 0.5f, rounding, flags)
        else    // Better looking lower-right corner and rounded non-AA shapes.
            pathRect(pMin + 0.5f, pMax - 0.49f, rounding, flags)
        pathStroke(col, DrawFlag.Closed, thickness)
    }

    /** @param pMin: upper-left
     *  @param pMax: lower-right
     *  (== upper-left + size) */
    fun addRectFilled(pMin: Vec2, pMax: Vec2, col: Int, rounding: Float = 0f, flags: DrawFlags = none) {
        if (col hasnt COL32_A_MASK) return
        if (rounding < 0.5f || (flags and DrawFlag.RoundCornersMask) == DrawFlag.RoundCornersNone) {
            primReserve(6, 4)
            primRect(Vec2(pMin), pMax, col) // [JVM] `pMin` safety first
        } else {
            pathRect(pMin, pMax, rounding, flags)
            pathFillConvex(col)
        }
    }

    /**
     * @param pMin = upper-left
     * @param pMax = lower-right
     *
     * TODO pMin and pMax not safe
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
        pathStroke(col, DrawFlag.Closed, thickness)
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
        pathStroke(col, DrawFlag.Closed, thickness)
    }

    fun addTriangleFilled(p1: Vec2, p2: Vec2, p3: Vec2, col: Int) {

        if (col hasnt COL32_A_MASK) return

        pathLineTo(p1)
        pathLineTo(p2)
        pathLineTo(p3)
        pathFillConvex(col)
    }

    fun addCircle(center: Vec2, radius: Float, col: Int, numSegments_: Int = 0, thickness: Float = 1f) {

        var numSegments = numSegments_
        if (col hasnt COL32_A_MASK || radius < 0.5f)
            return

        if (numSegments <= 0) {
            // Use arc with automatic segment count
            _pathArcToFastEx(center, radius - 0.5f, 0, DRAWLIST_ARCFAST_SAMPLE_MAX, 0)
            _path.removeLast()
        } else {
            // Explicit segment count (still clamp to avoid drawing insanely tessellated shapes)
            numSegments = clamp(numSegments, 3, DRAWLIST_CIRCLE_AUTO_SEGMENT_MAX)
            // Because we are filling a closed shape we remove 1 from the count of segments/points
            val aMax = (glm.πf * 2f) * (numSegments - 1f) / numSegments
            pathArcTo(center, radius - 0.5f, 0f, aMax, numSegments - 1)
        }

        pathStroke(col, DrawFlag.Closed, thickness)
    }

    fun addCircleFilled(center: Vec2, radius: Float, col: Int, numSegments_: Int = 0) {

        var numSegments = numSegments_
        if (col hasnt COL32_A_MASK || radius < 0.5f)
            return

        if (numSegments <= 0) {
            // Use arc with automatic segment count
            _pathArcToFastEx(center, radius, 0, DRAWLIST_ARCFAST_SAMPLE_MAX, 0)
            _path.removeLast()
        } else {
            // Explicit segment count (still clamp to avoid drawing insanely tessellated shapes)
            numSegments = clamp(numSegments, 3, DRAWLIST_CIRCLE_AUTO_SEGMENT_MAX)
            // Because we are filling a closed shape we remove 1 from the count of segments/points
            val aMax = (glm.πf * 2f) * (numSegments - 1f) / numSegments
            pathArcTo(center, radius, 0f, aMax, numSegments - 1)
        }

        pathFillConvex(col)
    }

    /** Guaranteed to honor 'num_segments' */
    fun addNgon(center: Vec2, radius: Float, col: Int, numSegments: Int, thickness: Float) {
        if (col hasnt COL32_A_MASK || numSegments <= 2)
            return

        // Because we are filling a closed shape we remove 1 from the count of segments/points
        val aMax = (glm.πf * 2f) * (numSegments.f - 1f) / numSegments.f
        pathArcTo(center, radius - 0.5f, 0f, aMax, numSegments - 1)
        pathStroke(col, DrawFlag.Closed, thickness)
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

        assert(font.containerAtlas.texID == _cmdHeader.textureId) { "Use high-level ImGui::pushFont() or low-level DrawList::pushTextureId() to change font_" }

        val clipRect = Vec4(_cmdHeader.clipRect)
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
    fun addPolyline(points: List<Vec2>, col: Int, flags: DrawFlags, thickness_: Float) {

        var thickness = thickness_

        val pointsCount = points.size
        if (pointsCount < 2 || col hasnt COL32_A_MASK)
            return

        val closed = flags has DrawFlag.Closed
        val opaqueUv = Vec2(_data.texUvWhitePixel)
        val count = if (closed) pointsCount else points.lastIndex // The number of line segments we need to draw
        val thickLine = thickness > _fringeScale

        if (this.flags has DrawListFlag.AntiAliasedLines) {
            // Anti-aliased stroke
            val AA_SIZE = _fringeScale
            val colTrans = col wo COL32_A_MASK

            // Thicknesses <1.0 should behave like thickness 1.0
            thickness = thickness max 1f
            val integerThickness = thickness.i
            val fractionalThickness = thickness - integerThickness

            // Do we want to draw this line using a texture?
            // - For now, only draw integer-width lines using textures to avoid issues with the way scaling occurs,
            //      could be improved.
            // - If AA_SIZE is not 1.0f we cannot use the texture path.
            val useTexture = this.flags has DrawListFlag.AntiAliasedLinesUseTex && integerThickness < DRAWLIST_TEX_LINES_WIDTH_MAX && fractionalThickness <= 0.00001f && AA_SIZE == 1f

            ASSERT_PARANOID(!useTexture || _data.font!!.containerAtlas.flags hasnt FontAtlas.Flag.NoBakedLines) { "We should never hit this, because NewFrame() doesn't set ImDrawListFlags_AntiAliasedLinesUseTex unless ImFontAtlasFlags_NoBakedLines is off" }

            val idxCount = if (useTexture) count * 6 else (count * if (thickLine) 18 else 12)
            val vtxCount = if (useTexture) pointsCount * 2 else (pointsCount * if (thickLine) 4 else 3)
            primReserve(idxCount, vtxCount)
            vtxBuffer.pos = _vtxWritePtr
            idxBuffer.pos = _idxWritePtr

            // Temporary buffer
            // The first <points_count> items are normals at each line point, then after that there are either 2 or 4
            // temp points for each line point
            _data.tempBuffer.reserveDiscard(pointsCount * if (useTexture || !thickLine) 3 else 5)
            val temp = _data.tempBuffer
            val tempPointsIdx = pointsCount

            // Calculate normals (tangents) for each line segment
            for (i1 in 0 until count) {
                val i2 = if (i1 + 1 == pointsCount) 0 else i1 + 1
                var dx = points[i2].x - points[i1].x
                var dy = points[i2].y - points[i1].y
                NORMALIZE2F_OVER_ZERO(dx, dy) { x, y -> dx = x; dy = y }
                temp[i1].x = dy
                temp[i1].y = -dx
            }
            if (!closed)
                temp[pointsCount - 1] = temp[pointsCount - 2]

            // If we are drawing a one-pixel-wide line without a texture, or a textured line of any width,
            // we only need 2 or 3 vertices per point
            if (useTexture || !thickLine) {

                // [PATH 1] Texture-based lines (thick or non-thick)
                // [PATH 2] Non texture-based lines (non-thick)

                // The width of the geometry we need to draw - this is essentially <thickness> pixels for
                // the line itself, plus "one pixel" for AA.
                // - In the texture-based path, we don't use AA_SIZE here because the +1 is tied
                //   to the generated texture (see ImFontAtlasBuildRenderLinesTexData() function),
                //   and so alternate values won't work without changes to that code.
                // - In the non texture-based paths, we would allow AA_SIZE to potentially be != 1.0f with a patch
                //   (e.g. fringe_scale patch to allow scaling geometry while preserving one-screen-pixel AA fringe).
                val halfDrawSize = if (useTexture) thickness * 0.5f + 1 else AA_SIZE

                // If line is not closed, the first and last points need to be generated differently as there are no normals to blend
                if (!closed) {
                    temp[tempPointsIdx + 0] = points[0] + temp[0] * halfDrawSize
                    temp[tempPointsIdx + 1] = points[0] - temp[0] * halfDrawSize
                    temp[tempPointsIdx + (pointsCount - 1) * 2 + 0] = points[pointsCount - 1] + temp[pointsCount - 1] * halfDrawSize
                    temp[tempPointsIdx + (pointsCount - 1) * 2 + 1] = points[pointsCount - 1] - temp[pointsCount - 1] * halfDrawSize
                }

                // Generate the indices to form a number of triangles for each line segment, and the vertices for the
                // line edges
                // This takes points n and n+1 and writes into n+1, with the first point in a closed line being
                // generated from the final one (as n+1 wraps)
                // FIXME-OPT: Merge the different loops, possibly remove the temporary buffer.
                var idx1 = _vtxCurrentIdx // Vertex index for start of line segment
                for (i1 in 0 until count) { // i1 is the first point of the line segment
                    val i2 = if (i1 + 1 == pointsCount) 0 else i1 + 1 // i2 is the second point of the line segment
                    val idx2 = if (i1 + 1 == pointsCount) _vtxCurrentIdx else (idx1 + if (useTexture) 2 else 3) // Vertex index for end of segment

                    // Average normals
                    var dmX = (temp[i1].x + temp[i2].x) * 0.5f
                    var dmY = (temp[i1].y + temp[i2].y) * 0.5f
                    FIXNORMAL2F(dmX, dmY) { x, y -> dmX = x; dmY = y }
                    dmX *= halfDrawSize // dm_x, dm_y are offset to the outer edge of the AA area
                    dmY *= halfDrawSize

                    // Add temporary vertices for the outer edges
                    val outVtxIdx = tempPointsIdx + i2 * 2
                    temp[outVtxIdx + 0].x = points[i2].x + dmX
                    temp[outVtxIdx + 0].y = points[i2].y + dmY
                    temp[outVtxIdx + 1].x = points[i2].x - dmX
                    temp[outVtxIdx + 1].y = points[i2].y - dmY

                    if (useTexture) {
                        // Add indices for two triangles
                        idxBuffer += idx2 + 0; idxBuffer += idx1 + 0; idxBuffer += idx1 + 1 // Right tri
                        idxBuffer += idx2 + 1; idxBuffer += idx1 + 1; idxBuffer += idx2 + 0 // Left tri
                        _idxWritePtr += 6
                    } else {
                        // Add indices for four triangles
                        idxBuffer += idx2 + 0; idxBuffer += idx1 + 0; idxBuffer += idx1 + 2 // Right tri 1
                        idxBuffer += idx1 + 2; idxBuffer += idx2 + 2; idxBuffer += idx2 + 0 // Right tri 1
                        idxBuffer += idx2 + 1; idxBuffer += idx1 + 1; idxBuffer += idx1 + 0 // Left tri 1
                        idxBuffer += idx1 + 0; idxBuffer += idx2 + 0; idxBuffer += idx2 + 1 // Left tri 1
                        _idxWritePtr += 12
                    }
                    idx1 = idx2
                }

                // Add vertices for each point on the line
                if (useTexture) {
                    // If we're using textures we only need to emit the left/right edge vertices
                    val texUVs = _data.texUvLines[integerThickness]
                    /*if (fractionalThickness != 0f) { // Currently always zero when use_texture==false!
                        val texUVs1 = _data.texUvLines[integerThickness + 1]
                        texUVs.x = texUVs.x + (texUVs1.x - texUVs.x) * fractionalThickness // inlined ImLerp()
                        texUVs.y = texUVs.y + (texUVs1.y - texUVs.y) * fractionalThickness
                        texUVs.z = texUVs.z + (texUVs1.z - texUVs.z) * fractionalThickness
                        texUVs.w = texUVs.w + (texUVs1.w - texUVs.w) * fractionalThickness
                    }*/
                    val texUV0 = Vec2(texUVs.x, texUVs.y)
                    val texUV1 = Vec2(texUVs.z, texUVs.w)

                    for (i in 0 until pointsCount) {
                        vtxBuffer += temp[tempPointsIdx + i * 2 + 0]; vtxBuffer += texUV0; vtxBuffer += col // Left-side outer edge
                        vtxBuffer += temp[tempPointsIdx + i * 2 + 1]; vtxBuffer += texUV1; vtxBuffer += col // Right-side outer edge
                        _vtxWritePtr += 2
                    }
                } else
                // If we're not using a texture, we need the center vertex as well
                    for (i in 0 until pointsCount) {
                        vtxBuffer += points[i]; vtxBuffer += opaqueUv; vtxBuffer += col // Center of line
                        vtxBuffer += temp[tempPointsIdx + i * 2 + 0]; vtxBuffer += opaqueUv; vtxBuffer += colTrans // Left-side outer edge
                        vtxBuffer += temp[tempPointsIdx + i * 2 + 1]; vtxBuffer += opaqueUv; vtxBuffer += colTrans // Right-side outer edge
                        _vtxWritePtr += 3
                    }
            } else {
                // [PATH 2] Non texture-based lines (thick): we need to draw the solid line core and thus require
                // four vertices per point
                val halfInnerThickness = (thickness - AA_SIZE) * 0.5f

                // If line is not closed, the first and last points need to be generated differently as there are
                // no normals to blend
                if (!closed) {
                    val pointsLast = points.lastIndex
                    temp[tempPointsIdx + 0] = points[0] + temp[0] * (halfInnerThickness + AA_SIZE)
                    temp[tempPointsIdx + 1] = points[0] + temp[0] * (halfInnerThickness)
                    temp[tempPointsIdx + 2] = points[0] - temp[0] * (halfInnerThickness)
                    temp[tempPointsIdx + 3] = points[0] - temp[0] * (halfInnerThickness + AA_SIZE)
                    temp[tempPointsIdx + pointsLast * 4 + 0] = points[pointsLast] + temp[pointsLast] * (halfInnerThickness + AA_SIZE)
                    temp[tempPointsIdx + pointsLast * 4 + 1] = points[pointsLast] + temp[pointsLast] * halfInnerThickness
                    temp[tempPointsIdx + pointsLast * 4 + 2] = points[pointsLast] - temp[pointsLast] * halfInnerThickness
                    temp[tempPointsIdx + pointsLast * 4 + 3] = points[pointsLast] - temp[pointsLast] * (halfInnerThickness + AA_SIZE)
                }

                // Generate the indices to form a number of triangles for each line segment, and the vertices for
                // the line edges
                // This takes points n and n+1 and writes into n+1, with the first point in a closed line
                // being generated from the final one (as n+1 wraps)
                // FIXME-OPT: Merge the different loops, possibly remove the temporary buffer.
                var idx1 = _vtxCurrentIdx // Vertex index for start of line segment
                for (i1 in 0 until count) { // i1 is the first point of the line segment
                    val i2 = if ((i1 + 1) == pointsCount) 0 else (i1 + 1) // i2 is the second point of the line segment
                    val idx2 = if ((i1 + 1) == pointsCount) _vtxCurrentIdx else (idx1 + 4) // Vertex index for end of segment

                    // Average normals
                    var dmX = (temp[i1].x + temp[i2].x) * 0.5f
                    var dmY = (temp[i1].y + temp[i2].y) * 0.5f
                    FIXNORMAL2F(dmX, dmY) { x, y -> dmX = x; dmY = y }
                    val dmOutX = dmX * (halfInnerThickness + AA_SIZE)
                    val dmOutY = dmY * (halfInnerThickness + AA_SIZE)
                    val dmInX = dmX * halfInnerThickness
                    val dmInY = dmY * halfInnerThickness

                    // Add temporary vertices
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

                // Add vertices
                for (i in 0 until pointsCount) {
                    vtxBuffer += temp[tempPointsIdx + i * 4 + 0]; vtxBuffer += opaqueUv; vtxBuffer += colTrans
                    vtxBuffer += temp[tempPointsIdx + i * 4 + 1]; vtxBuffer += opaqueUv; vtxBuffer += col
                    vtxBuffer += temp[tempPointsIdx + i * 4 + 2]; vtxBuffer += opaqueUv; vtxBuffer += col
                    vtxBuffer += temp[tempPointsIdx + i * 4 + 3]; vtxBuffer += opaqueUv; vtxBuffer += colTrans
                    _vtxWritePtr += 4
                }
            }
            _vtxCurrentIdx += vtxCount
        } else {
            // [PATH 4] Non texture-based, Non anti-aliased lines
            val idxCount = count * 6
            val vtxCount = count * 4      // FIXME-OPT: Not sharing edges
            primReserve(idxCount, vtxCount)
            vtxBuffer.pos = _vtxWritePtr
            idxBuffer.pos = _idxWritePtr

            for (i1 in 0 until count) {
                val i2 = if ((i1 + 1) == pointsCount) 0 else i1 + 1
                val p1 = points[i1]
                val p2 = points[i2]

                var dX = p2.x - p1.x
                var dY = p2.y - p1.y
                NORMALIZE2F_OVER_ZERO(dX, dY) { x, y -> dX = x; dY = y }
                dX *= thickness * 0.5f
                dY *= thickness * 0.5f

                vtxBuffer += p1.x + dY; vtxBuffer += p1.y - dX; vtxBuffer += opaqueUv; vtxBuffer += col
                vtxBuffer += p2.x + dY; vtxBuffer += p2.y - dX; vtxBuffer += opaqueUv; vtxBuffer += col
                vtxBuffer += p2.x - dY; vtxBuffer += p2.y + dX; vtxBuffer += opaqueUv; vtxBuffer += col
                vtxBuffer += p1.x - dY; vtxBuffer += p1.y + dX; vtxBuffer += opaqueUv; vtxBuffer += col
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

    /** - We intentionally avoid using ImVec2 and its math operators here to reduce cost to a minimum for debug/non-inlined builds.
     *  - Filled shapes must always use clockwise winding order. The anti-aliasing fringe depends on it. Counter-clockwise shapes will have "inward" anti-aliasing. */
    fun addConvexPolyFilled(points: ArrayList<Vec2>, col: Int) {

        val pointsCount = points.size
        if (pointsCount < 3 || col hasnt COL32_A_MASK)
            return

        val uv = Vec2(_data.texUvWhitePixel)

        if (flags has DrawListFlag.AntiAliasedFill) {
            // Anti-aliased Fill
            val AA_SIZE = _fringeScale
            val colTrans = col wo COL32_A_MASK
            val idxCount = (pointsCount - 2) * 3 + pointsCount * 6
            val vtxCount = pointsCount * 2
            primReserve(idxCount, vtxCount)
            vtxBuffer.pos = _vtxWritePtr
            idxBuffer.pos = _idxWritePtr

            // Add indexes for fill
            val vtxInnerIdx = _vtxCurrentIdx
            val vtxOuterIdx = _vtxCurrentIdx + 1
            for (i in 2 until pointsCount) {
                idxBuffer += vtxInnerIdx; idxBuffer += vtxInnerIdx + ((i - 1) shl 1); idxBuffer += vtxInnerIdx + (i shl 1)
                _idxWritePtr += 3
            }

            // Compute normals
            _data.tempBuffer.reserveDiscard(pointsCount)
            val tempNormals = _data.tempBuffer
            var i0 = points.lastIndex
            var i1 = 0
            while (i1 < pointsCount) {
                val p0 = points[i0]
                val p1 = points[i1]
                var dX = p1.x - p0.x
                var dY = p1.y - p0.y
                NORMALIZE2F_OVER_ZERO(dX, dY) { x, y -> dX = x; dY = y }
                tempNormals[i0].x = dY
                tempNormals[i0].y = -dX
                i0 = i1++
            }

            i0 = points.lastIndex
            i1 = 0
            while (i1 < pointsCount) {
                // Average normals
                val n0 = tempNormals[i0]
                val n1 = tempNormals[i1]
                var dmX = (n0.x + n1.x) * 0.5f
                var dmY = (n0.y + n1.y) * 0.5f
                FIXNORMAL2F(dmX, dmY) { x, y -> dmX = x; dmY = y }
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
            val idxCount = (pointsCount - 2) * 3
            val vtxCount = pointsCount
            primReserve(idxCount, vtxCount)
            vtxBuffer.pos = _vtxWritePtr
            idxBuffer.pos = _idxWritePtr
            for (i in 0 until vtxCount) {
                vtxBuffer += points[i]; vtxBuffer += uv; vtxBuffer += col
                _vtxWritePtr++
            }
            for (i in 2 until pointsCount) {
                idxBuffer += _vtxCurrentIdx; idxBuffer += _vtxCurrentIdx + i - 1; idxBuffer[_idxWritePtr + 2] = _vtxCurrentIdx + i
                _idxWritePtr += 3
            }
            _vtxCurrentIdx += vtxCount
        }
        vtxBuffer.pos = 0
        idxBuffer.pos = 0
    }

    /** Cubic Bezier takes 4 controls points
     *
     *  Cubic Bezier (4 control points) */
    fun addBezierCubic(p1: Vec2, p2: Vec2, p3: Vec2, p4: Vec2, col: Int, thickness: Float, numSegments: Int = 0) {
        if (col hasnt COL32_A_MASK) return

        pathLineTo(p1)
        pathBezierCubicCurveTo(p2, p3, p4, numSegments)
        pathStroke(col, thickness = thickness)
    }

    /** Quad Bezier (3 control points)
     *
     *  Quadratic Bezier takes 3 controls points */
    fun addBezierQuadratic(p1: Vec2, p2: Vec2, p3: Vec2, col: Int, thickness: Float, numSegments: Int = 0) {
        if (col hasnt COL32_A_MASK)
            return

        pathLineTo(p1)
        pathBezierQuadraticCurveTo(p2, p3, numSegments)
        pathStroke(col, thickness = thickness)
    }


    // -----------------------------------------------------------------------------------------------------------------
    // Image primitives
    // - Read FAQ to understand what ImTextureID is.
    // - "p_min" and "p_max" represent the upper-left and lower-right corners of the rectangle.
    // - "uv_min" and "uv_max" represent the normalized texture coordinates to use for those corners. Using (0,0)->(1,1) texture coordinates will generally display the entire texture.
    // -----------------------------------------------------------------------------------------------------------------

    fun addImage(
            userTextureId: TextureID, pMin: Vec2, pMax: Vec2,
            uvMin: Vec2 = Vec2(0), uvMax: Vec2 = Vec2(1), col: Int = COL32_WHITE,
    ) {

        if (col hasnt COL32_A_MASK) return

        val pushTextureId = userTextureId != _cmdHeader.textureId
        if (pushTextureId) pushTextureID(userTextureId)

        primReserve(6, 4)
        primRectUV(pMin, pMax, uvMin, uvMax, col)

        if (pushTextureId) popTextureID()
    }

    fun addImageQuad(
            userTextureId: TextureID, p1: Vec2, p2: Vec2, p3: Vec2, p4: Vec2,
            uv1: Vec2 = Vec2(0), uv2: Vec2 = Vec2(1, 0),
            uv3: Vec2 = Vec2(1), uv4: Vec2 = Vec2(0, 1), col: Int = COL32_WHITE,
    ) {

        if (col hasnt COL32_A_MASK) return

        val pushTextureId = userTextureId != _cmdHeader.textureId
        if (pushTextureId)
            pushTextureID(userTextureId)

        primReserve(6, 4)
        primQuadUV(p1, p2, p3, p4, uv1, uv2, uv3, uv4, col)

        if (pushTextureId)
            popTextureID()
    }

    fun addImageRounded(userTextureId: TextureID, pMin: Vec2, pMax: Vec2, uvMin: Vec2, uvMax: Vec2, col: Int, rounding: Float, flags_: DrawFlags = none) {
        if (col hasnt COL32_A_MASK)
            return

        val flags = fixRectCornerFlags(flags_)
        if (rounding < 0.5f || (flags and DrawFlag.RoundCornersMask) == DrawFlag.RoundCornersNone) {
            addImage(userTextureId, pMin, pMax, uvMin, uvMax, col)
            return
        }

        val pushTextureId = userTextureId != _cmdHeader.textureId
        if (pushTextureId) pushTextureID(userTextureId)

        val vertStartIdx = vtxBuffer.size
        pathRect(pMin, pMax, rounding, flags)
        pathFillConvex(col)
        val vertEndIdx = vtxBuffer.size
        shadeVertsLinearUV(vertStartIdx, vertEndIdx, pMin, pMax, uvMin, uvMax, true)

        if (pushTextureId) popTextureID()
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Stateful path API, add points then finish with PathFillConvex() or PathStroke()
    // - Filled shapes must always use clockwise winding order. The anti-aliasing fringe depends on it. Counter-clockwise shapes will have "inward" anti-aliasing.
    // -----------------------------------------------------------------------------------------------------------------

    fun pathClear() = _path.clear()

    fun pathLineTo(pos: Vec2) = _path.add(pos)

    fun pathLineToMergeDuplicate(pos: Vec2) {
        if (_path.isEmpty() || _path.last() != pos) _path += pos
    }

    fun pathFillConvex(col: Int) = addConvexPolyFilled(_path, col).also { pathClear() }

    /** rounding_corners_flags: 4 bits corresponding to which corner to round   */
    fun pathStroke(col: Int, flags: DrawFlags = none, thickness: Float = 1.0f) =
            addPolyline(_path, col, flags, thickness).also { pathClear() }

    /** @param center must be a new instance */
    fun pathArcTo(center: Vec2, radius: Float, aMin: Float, aMax: Float, numSegments: Int = 0) {
        if (radius < 0.5f) {
            _path += center
            return
        }

        if (numSegments > 0) {
            _pathArcToN(center, radius, aMin, aMax, numSegments)
            return
        }

        // Automatic segment count
        if (radius <= _data.arcFastRadiusCutoff) {
            val aIsReverse = aMax < aMin

            // We are going to use precomputed values for mid samples.
            // Determine first and last sample in lookup table that belong to the arc.
            val aMinSampleF = DRAWLIST_ARCFAST_SAMPLE_MAX * aMin / (glm.πf * 2f)
            val aMaxSampleF = DRAWLIST_ARCFAST_SAMPLE_MAX * aMax / (glm.πf * 2f)

            val aMinSample = if (aIsReverse) floorSigned(aMinSampleF).i else ceil(aMinSampleF).i
            val aMaxSample = if (aIsReverse) ceil(aMaxSampleF).i else floorSigned(aMaxSampleF).i
            val aMidSamples = if (aIsReverse) max(aMinSample - aMaxSample, 0) else max(aMaxSample - aMinSample, 0)

            val aMinSegmentAngle = aMinSample * glm.πf * 2f / DRAWLIST_ARCFAST_SAMPLE_MAX
            val aMaxSegmentAngle = aMaxSample * glm.πf * 2f / DRAWLIST_ARCFAST_SAMPLE_MAX
            val aEmitStart = (aMinSegmentAngle - aMin).abs >= 1e-5f
            val aEmitEnd = (aMax - aMaxSegmentAngle).abs >= 1e-5f

            //            _path.reserve(_Path.Size + (a_mid_samples + 1 + (a_emit_start ? 1 : 0)+(a_emit_end ? 1 : 0)))
            if (aEmitStart)
                _path += Vec2(center.x + aMin.cos * radius, center.y + aMin.sin * radius)
            if (aMidSamples >= 0)
                _pathArcToFastEx(center, radius, aMinSample, aMaxSample, 0)
            if (aEmitEnd)
                _path += Vec2(center.x + aMax.cos * radius, center.y + aMax.sin * radius)
        } else {
            val arcLength = (aMax - aMin).abs
            val circleSegmentCount = _calcCircleAutoSegmentCount(radius)
            val arcSegmentCount = ceil(circleSegmentCount * arcLength / (glm.πf * 2f)).i max (2f * glm.πf / arcLength).i
            _pathArcToN(center, radius, aMin, aMax, arcSegmentCount)
        }
    }

    /** Use precomputed angles for a 12 steps circle
     *
     *  0: East, 3: South, 6: West, 9: North, 12: East
     *
     *  @param center must be a new instance */
    fun pathArcToFast(center: Vec2, radius: Float, aMinOf12: Int, aMaxOf12: Int) {
        if (radius < 0.5f) {
            _path += center
            return
        }
        _pathArcToFastEx(center, radius, aMinOf12 * DRAWLIST_ARCFAST_SAMPLE_MAX / 12, aMaxOf12 * DRAWLIST_ARCFAST_SAMPLE_MAX / 12, 0)
    }

    /** Cubic bezier
     *
     *  Cubic Bezier (4 control points) */
    fun pathBezierCubicCurveTo(p2: Vec2, p3: Vec2, p4: Vec2, numSegments: Int = 0) {

        val p1 = _path.last()
        if (numSegments == 0) {
            assert(_data.curveTessellationTol > 0f)
            pathBezierCubicCurveToCasteljau(_path, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, p4.x, p4.y, style.curveTessellationTol, 0) // Auto-tessellated
        } else {
            val tStep = 1f / numSegments
            for (iStep in 1..numSegments)
                _path += bezierCubicCalc(p1, p2, p3, p4, tStep * iStep)
        }
    }

    /** Closely mimics ImBezierCubicClosestPointCasteljau() in imgui.cpp */
    private fun pathBezierCubicCurveToCasteljau(path: ArrayList<Vec2>, x1: Float, y1: Float, x2: Float, y2: Float,
                                                x3: Float, y3: Float, x4: Float, y4: Float, tessTol: Float, level: Int) {
        val dx = x4 - x1
        val dy = y4 - y1
        var d2 = (x2 - x4) * dy - (y2 - y4) * dx
        var d3 = (x3 - x4) * dy - (y3 - y4) * dx
        d2 = if (d2 >= 0) d2 else -d2
        d3 = if (d3 >= 0) d3 else -d3
        if ((d2 + d3) * (d2 + d3) < tessTol * (dx * dx + dy * dy))
            path += Vec2(x4, y4)
        else if (level < 10) {
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
            pathBezierCubicCurveToCasteljau(path, x1, y1, x12, y12, x123, y123, x1234, y1234, tessTol, level + 1)
            pathBezierCubicCurveToCasteljau(path, x1234, y1234, x234, y234, x34, y34, x4, y4, tessTol, level + 1)
        }
    }

    /** Quadratic bezier
     *
     *  Quad Bezier (3 control points) */
    fun pathBezierQuadraticCurveTo(p2: Vec2, p3: Vec2, numSegments: Int) {
        val p1 = _path.last()
        if (numSegments == 0) {
            assert(_data.curveTessellationTol > 0f)
            pathBezierQuadraticCurveToCasteljau(_path, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, _data.curveTessellationTol, 0) // Auto-tessellated
        } else {
            val tStep = 1f / numSegments
            for (iStep in 1..numSegments)
                _path + bezierQuadraticCalc(p1, p2, p3, tStep * iStep)
        }
    }

    // Assert and return same value
    fun fixRectCornerFlags(flags_: DrawFlags): DrawFlags {
        var flags = flags_
        // If this triggers, please update your code replacing hardcoded values with new ImDrawFlags_RoundCorners* values.
        // Note that ImDrawFlags_Closed (== 0x01) is an invalid flag for AddRect(), AddRectFilled(), PathRect() etc...
        // [JVM]: Since we're using type-safe flags, this shouldn't be an issue. THe only DrawFlag that could've
        // triggered the previous type-unsafe version of this is Df.Closed, and so we handle it here.
        check(flags hasnt DrawFlag.Closed) { "Misuse of legacy hardcoded ImDrawCornerFlags values!" }

        if (flags hasnt DrawFlag.RoundCornersMask)
            flags /= DrawFlag.RoundCornersAll

        return flags
    }

    private fun pathBezierQuadraticCurveToCasteljau(path: ArrayList<Vec2>, x1: Float, y1: Float, x2: Float, y2: Float,
                                                    x3: Float, y3: Float, tessTol: Float, level: Int) {
        val dx = x3 - x1
        val dy = y3 - y1
        val det = (x2 - x3) * dy - (y2 - y3) * dx
        if (det * det * 4f < tessTol * (dx * dx + dy * dy))
            path += Vec2(x3, y3)
        else if (level < 10) {
            val x12 = (x1 + x2) * 0.5f
            val y12 = (y1 + y2) * 0.5f
            val x23 = (x2 + x3) * 0.5f
            val y23 = (y2 + y3) * 0.5f
            val x123 = (x12 + x23) * 0.5f
            val y123 = (y12 + y23) * 0.5f
            pathBezierQuadraticCurveToCasteljau(path, x1, y1, x12, y12, x123, y123, tessTol, level + 1)
            pathBezierQuadraticCurveToCasteljau(path, x123, y123, x23, y23, x3, y3, tessTol, level + 1)
        }
    }

    fun pathRect(a: Vec2, b: Vec2, rounding_: Float = 0f, flags_: DrawFlags = none) {
        val flags = fixRectCornerFlags(flags_)
        var cond = (DrawFlag.RoundCornersTop in flags) or (DrawFlag.RoundCornersBottom in flags)
        var rounding = rounding_ min ((b.x - a.x).abs * (if (cond) 0.5f else 1f) - 1f)
        cond = (DrawFlag.RoundCornersLeft in flags) or (DrawFlag.RoundCornersRight in flags)
        rounding = rounding min ((b.y - a.y).abs * (if (cond) 0.5f else 1f) - 1f)

        if (rounding < 0.5f || (flags and DrawFlag.RoundCornersMask) == DrawFlag.RoundCornersNone) {
            pathLineTo(a)
            pathLineTo(Vec2(b.x, a.y))
            pathLineTo(b)
            pathLineTo(Vec2(a.x, b.y))
        } else {
            val roundingTL = if (flags has DrawFlag.RoundCornersTopLeft) rounding else 0f
            val roundingTR = if (flags has DrawFlag.RoundCornersTopRight) rounding else 0f
            val roundingBR = if (flags has DrawFlag.RoundCornersBottomRight) rounding else 0f
            val roundingBL = if (flags has DrawFlag.RoundCornersBottomLeft) rounding else 0f
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
    fun addCallback(callback: DrawCallback, callbackData: Any? = null) {
        check(cmdBuffer.isNotEmpty())
        var currCmd = cmdBuffer.last()
        assert(currCmd.userCallback == null)
        if (currCmd.elemCount != 0) {
            addDrawCmd()
            currCmd = cmdBuffer.last()
        }

        currCmd.userCallback = callback
        currCmd.userCallbackData = callbackData
        addDrawCmd() // Force a new command after us (see comment below)
    }

    /** This is useful if you need to forcefully create a new draw call (to allow for dependent rendering / blending).
    Otherwise primitives are merged into the same draw-call as much as possible */
    fun addDrawCmd() {
        val drawCmd = DrawCmd().apply {
            clipRect put _cmdHeader.clipRect    // Same as calling ImDrawCmd_HeaderCopy()
            textureId = _cmdHeader.textureId
            vtxOffset = _cmdHeader.vtxOffset
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
    //   Prefer using your own persistent instance of ImDrawListSplitter as you can stack them.
    //   Using the ImDrawList::ChannelsXXXX you cannot stack a split over another.nels then merge at the end)
    // -----------------------------------------------------------------------------------------------------------------

    fun channelsSplit(count: Int) = _splitter.split(this, count)

    fun channelsMerge() = _splitter.merge(this)

    fun channelsSetCurrent(idx: Int) = _splitter.setCurrentChannel(this, idx)

    /** Reserve space for a number of vertices and indices.
     *  You must finish filling your reserved data before calling PrimReserve() again, as it may reallocate or
     *  submit the intermediate results. PrimUnreserve() can be used to release unused allocations.    */
    fun primReserve(idxCount: Int, vtxCount: Int) {

        // Large mesh support (when enabled)
        ASSERT_PARANOID(idxCount >= 0 && vtxCount >= 0)
        if (DrawIdx.BYTES == 2 && _vtxCurrentIdx + vtxCount >= (1 shl 16) && flags has DrawListFlag.AllowVtxOffset) {
            // FIXME: In theory we should be testing that vtx_count <64k here.
            // In practice, RenderText() relies on reserving ahead for a worst case scenario so it is currently useful
            // for us to not make that check until we rework the text functions to handle clipping and
            // large horizontal lines better.
            _cmdHeader.vtxOffset = vtxBuffer.rem
            _onChangedVtxOffset()
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

    // -----------------------------------------------------------------------------------------------------------------
    // [Internal helpers]
    // NB: all primitives needs to be reserved via PrimReserve() beforehand!
    // -----------------------------------------------------------------------------------------------------------------

    /** Initialize before use in a new frame. We always have a command ready in the buffer. */
    fun _resetForNewFrame() {

        // Verify that the ImDrawCmd fields we want to memcmp() are contiguous in memory.
        // IM_STATIC_ASSERT(IM_OFFSETOF(ImDrawCmd, ClipRect) == 0);
        // IM_STATIC_ASSERT(IM_OFFSETOF(ImDrawCmd, TextureId) == sizeof(ImVec4));
        // IM_STATIC_ASSERT(IM_OFFSETOF(ImDrawCmd, VtxOffset) == sizeof(ImVec4) + sizeof(ImTextureID));
        if (_splitter._count > 1)
            _splitter merge this

        cmdBuffer.clear()
        // we dont assign because it wont create a new instance for sure
        idxBuffer = idxBuffer.resize(0)
        vtxBuffer = vtxBuffer.resize(0)
        flags = _data.initialFlags
        _cmdHeader = DrawCmd()
        _vtxCurrentIdx = 0
        _vtxWritePtr = 0
        _idxWritePtr = 0
        _clipRectStack.clear()
        _textureIdStack.clear()
        _path.clear()
        _splitter.clear()
        cmdBuffer += DrawCmd()
    }

    /** @param destroy useful to declare if this is a memory release or not */
    fun _clearFreeMemory(destroy: Boolean = false) {
        cmdBuffer.clear()
        // we dont assign because it wont create a new instance for sure
        if (destroy) {
            vtxBuffer.data.free()
            idxBuffer.free()
        } else {
            idxBuffer = idxBuffer.resize(0)
            vtxBuffer = vtxBuffer.resize(0)
        }
        flags = none
        _vtxCurrentIdx = 0
        _vtxWritePtr = 0
        _idxWritePtr = 0
        _clipRectStack.clear()
        _textureIdStack.clear()
        _path.clear()
        _splitter.clearFreeMemory()
        // TODO check
        //        resetForNewFrame()
        //        _splitter.clearFreeMemory(destroy)
    }

    /** Pop trailing draw command (used before merging or presenting to user)
     *  Note that this leaves the ImDrawList in a state unfit for further commands,
     *  as most code assume that CmdBuffer.Size > 0 && CmdBuffer.back().UserCallback == NULL */
    fun _popUnusedDrawCmd() {
        while (cmdBuffer.isNotEmpty()) {
            val currCmd = cmdBuffer.last()
            if (currCmd.elemCount != 0 || currCmd.userCallback != null)
                return // break;
            cmdBuffer.pop()
        }
    }

    /** Try to merge two last draw commands */
    fun _tryMergeDrawCmds() {
        check(cmdBuffer.isNotEmpty())
        val currCmd = cmdBuffer.last()
        val prevCmd = cmdBuffer[cmdBuffer.lastIndex - 1]
        if (currCmd headerCompare prevCmd && prevCmd areSequentialIdxOffset currCmd && currCmd.userCallback == null && prevCmd.userCallback == null) {
            prevCmd.elemCount += currCmd.elemCount
            cmdBuffer.pop()
        }
    }

    /** Our scheme may appears a bit unusual, basically we want the most-common calls AddLine AddRect etc. to not have
    to perform any check so we always have a command ready in the stack.
    The cost of figuring out if a new command has to be added or if we can merge is paid in those Update**
    functions only. */
    fun _onChangedClipRect() {
        check(cmdBuffer.isNotEmpty())
        // If current command is used with different settings we need to add a new command
        val currCmd = cmdBuffer.last()
        if (currCmd.elemCount != 0 && currCmd.clipRect != _cmdHeader.clipRect) {
            addDrawCmd()
            return
        }
        assert(currCmd.userCallback == null)

        // Try to merge with previous command if it matches, else use current command
        cmdBuffer.getOrNull(cmdBuffer.lastIndex - 1)?.let { prevCmd ->
            if (currCmd.elemCount == 0 && cmdBuffer.size > 1 && prevCmd areSequentialIdxOffset currCmd && _cmdHeader headerCompare prevCmd && prevCmd.userCallback == null) {
                cmdBuffer.pop()
                return
            }
        }

        currCmd.clipRect put _cmdHeader.clipRect
    }

    fun _onChangedTextureID() {
        // If current command is used with different settings we need to add a new command
        check(cmdBuffer.isNotEmpty())
        val currCmd = cmdBuffer.last()
        if (currCmd.elemCount != 0 && currCmd.textureId != _cmdHeader.textureId) {
            addDrawCmd()
            return
        }
        assert(currCmd.userCallback == null)

        cmdBuffer.getOrNull(cmdBuffer.lastIndex - 1)?.let { prevCmd ->
            if (currCmd.elemCount == 0 && cmdBuffer.size > 1 && _cmdHeader headerCompare prevCmd && prevCmd areSequentialIdxOffset currCmd && prevCmd.userCallback == null) {
                cmdBuffer.pop()
                return
            }
        }

        currCmd.textureId = _cmdHeader.textureId
    }

    fun _onChangedVtxOffset() {
        // We don't need to compare curr_cmd->VtxOffset != _CmdHeader.VtxOffset because we know it'll be different at the time we call this.
        _vtxCurrentIdx = 0
        check(cmdBuffer.isNotEmpty())
        val currCmd = cmdBuffer.last()
        //        assert(currCmd.vtxOffset != _cmdHeader.vtxOffset) // See #3349
        if (currCmd.elemCount != 0) {
            addDrawCmd()
            return
        }
        assert(currCmd.userCallback == null)
        currCmd.vtxOffset = _cmdHeader.vtxOffset
    }

    fun _calcCircleAutoSegmentCount(radius: Float): Int =
            // Automatic segment count
            when (val radiusIdx = (radius + 0.999999f).i) { // ceil to never reduce accuracy
                in _data.circleSegmentCounts.indices -> _data.circleSegmentCounts[radiusIdx] // Use cached value
                else -> DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC(radius, _data.circleSegmentMaxError)
            }

    /** @center must be a new instance */
    fun _pathArcToFastEx(center: Vec2, radius: Float, aMinSample_: Int, aMaxSample_: Int, aStep_: Int) {
        if (radius < 0.5f) {
            _path += center
            return
        }
        var aMaxSample = aMaxSample_
        var aMinSample = aMinSample_

        // Calculate arc auto segment step size
        var aStep = when {
            aStep_ <= 0 -> DRAWLIST_ARCFAST_SAMPLE_MAX / _calcCircleAutoSegmentCount(radius)
            else -> aStep_
        }

        // Make sure we never do steps larger than one quarter of the circle
        aStep = clamp(aStep, 1, DRAWLIST_ARCFAST_TABLE_SIZE / 4)

        val sampleRange = (aMaxSample - aMinSample).abs
        val aNextStep = aStep
        var samples = sampleRange + 1
        var extraMaxSample = false
        if (aStep > 1) {
            samples = sampleRange / aStep + 1
            val overstep = sampleRange % aStep
            if (overstep > 0) {
                extraMaxSample = true
                samples++

                // When we have overstep to avoid awkwardly looking one long line and one tiny one at the end,
                // distribute first step range evenly between them by reducing first step size.
                if (sampleRange > 0)
                    aStep -= (aStep - overstep) / 2
            }
        }
        //        _path.resize(_Path.Size + samples);
        //        val out_ptr = _Path.Data + (_Path.Size - samples);

        var sampleIndex = aMinSample
        if (sampleIndex < 0 || sampleIndex >= DRAWLIST_ARCFAST_SAMPLE_MAX) {
            sampleIndex = sampleIndex % DRAWLIST_ARCFAST_SAMPLE_MAX
            if (sampleIndex < 0)
                sampleIndex += DRAWLIST_ARCFAST_SAMPLE_MAX
        }

        if (aMaxSample >= aMinSample) {
            var a = aMinSample
            while (a <= aMaxSample) {
                // a_step is clamped to IM_DRAWLIST_ARCFAST_SAMPLE_MAX, so we have guaranteed that it will not wrap over range twice or more
                if (sampleIndex >= DRAWLIST_ARCFAST_SAMPLE_MAX)
                    sampleIndex -= DRAWLIST_ARCFAST_SAMPLE_MAX

                val s = _data.arcFastVtx[sampleIndex]
                _path += Vec2(center.x + s.x * radius,
                        center.y + s.y * radius)

                a += aStep; sampleIndex += aStep; aStep = aNextStep
            }
        } else {
            var a = aMinSample
            while (a >= aMaxSample) {
                // a_step is clamped to IM_DRAWLIST_ARCFAST_SAMPLE_MAX, so we have guaranteed that it will not wrap over range twice or more
                if (sampleIndex < 0)
                    sampleIndex += DRAWLIST_ARCFAST_SAMPLE_MAX

                val s = _data.arcFastVtx[sampleIndex]
                _path += Vec2(center.x + s.x * radius,
                        center.y + s.y * radius)

                a -= aStep; sampleIndex -= aStep; aStep = aNextStep
            }
        }

        if (extraMaxSample) {
            var normalizedMaxSample = aMaxSample % DRAWLIST_ARCFAST_SAMPLE_MAX
            if (normalizedMaxSample < 0)
                normalizedMaxSample += DRAWLIST_ARCFAST_SAMPLE_MAX

            val s = _data.arcFastVtx[normalizedMaxSample]
            _path += Vec2(center.x + s.x * radius,
                    center.y + s.y * radius)
        }

        //        IM_ASSERT_PARANOID(_Path.Data + _Path.Size == out_ptr)
    }

    /** @param center must be new instance */
    fun _pathArcToN(center: Vec2, radius: Float, aMin: Float, aMax: Float, numSegments: Int) {
        if (radius < 0.5f) {
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

    companion object {

        // On AddPolyline() and AddConvexPolyFilled() we intentionally avoid using ImVec2 and superfluous function calls to optimize debug/non-inlined builds.
        // - Those macros expects l-values and need to be used as their own statement.
        // - Those macros are intentionally not surrounded by the 'do {} while (0)' idiom because even that translates to runtime with debug compilers.
        inline fun NORMALIZE2F_OVER_ZERO(vX: Float, vY: Float, res: (x: Float, y: Float) -> Unit) {
            var x = vX
            var y = vY
            val d2 = x * x + y * y
            if (d2 > 0f) {
                val invLen = 1 / sqrt(d2)
                x *= invLen
                y *= invLen
            }
            res(x, y)
        }

        const val FIXNORMAL2F_MAX_INVLEN2 = 100f // 500.0f (see #4053, #3366)
        inline fun FIXNORMAL2F(vX: Float, vY: Float, res: (x: Float, y: Float) -> Unit) {
            var x = vX
            var y = vY
            val d2 = x * x + y * y
            if (d2 > 0.000001f) {
                var invLen2 = 1f / d2
                if (invLen2 > FIXNORMAL2F_MAX_INVLEN2)
                    invLen2 = FIXNORMAL2F_MAX_INVLEN2
                x *= invLen2
                y *= invLen2
            }
            res(x, y)
        }
    }
}

private fun DrawVert_Buffer(size: Int = 0) = DrawVert_Buffer(ByteBuffer(size))

@JvmInline
value class DrawVert_Buffer(val data: ByteBuffer) {

    operator fun get(index: Int) = DrawVert(
            Vec2(data, index * DrawVert.SIZE),
            Vec2(data, index * DrawVert.SIZE + DrawVert.OFS_UV),
            data.getInt(index * DrawVert.SIZE + DrawVert.OFS_COL))

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
        get() = data.cap / DrawVert.SIZE

    inline var lim: Int
        get() = data.lim / DrawVert.SIZE
        set(value) {
            data.lim = value * DrawVert.SIZE
        }

    inline var pos: Int
        get() = data.pos / DrawVert.SIZE
        set(value) {
            data.pos = value * DrawVert.SIZE
        }

    inline val rem: Int
        get() = data.rem / DrawVert.SIZE

    inline val size: Int
        get() = rem

    inline val sizeByte: Int
        get() = data.rem

    inline val adr: ULong
        get() = data.adr

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
        val newData = ByteBuffer(newCapacity * DrawVert.SIZE)
        if (lim > 0)
            MemoryUtil.memCopy(data.adr.L, newData.adr.L, data.lim.L)
        data.free()
        return DrawVert_Buffer(newData)
    }
}