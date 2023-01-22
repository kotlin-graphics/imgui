package imgui.internal.sections

import glm_.max
import glm_.vec2.Vec2
import imgui.ImGui
import imgui.Viewport
import imgui.api.g
import imgui.classes.DrawList
import imgui.internal.DrawData
import imgui.internal.classes.Rect
import kool.lim


//-----------------------------------------------------------------------------
// [SECTION] Viewport support
//-----------------------------------------------------------------------------

/** ImGuiViewport Private/Internals fields (cardinal sin: we are using inheritance!)
 *  Every instance of ImGuiViewport is in fact a ImGuiViewportP. */
class ViewportP : Viewport() {

    /** Last frame number the background (0) and foreground (1) draw lists were used */
    val drawListsLastFrame = intArrayOf(-1, -1)

    /** Convenience background (0) and foreground (1) draw lists. We use them to draw software mouser cursor when io.MouseDrawCursor is set and to draw most debug overlays. */
    val drawLists = Array<DrawList?>(2) { null }

    var drawDataP: DrawData? = null

    var drawDataBuilder: DrawDataBuilder? = null

    /** Work Area: Offset from Pos to top-left corner of Work Area. Generally (0,0) or (0,+main_menu_bar_height). Work Area is Full Area but without menu-bars/status-bars (so WorkArea always fit inside Pos/Size!) */
    val workOffsetMin = Vec2()

    /** Work Area: Offset from Pos+Size to bottom-right corner of Work Area. Generally (0,0) or (0,-status_bar_height). */

    val workOffsetMax = Vec2()

    /** Work Area: Offset being built/increased during current frame */
    val currWorkOffsetMin = Vec2()

    /** Work Area: Offset being built/decreased during current frame */
    val currWorkOffsetMax = Vec2()

    val mainRect: Rect
        get() = Rect(pos.x, pos.y, pos.x + size.x, pos.y + size.y)

    val workRect: Rect
        get() = Rect(workPos.x, workPos.y, workPos.x + workSize.x, workPos.y + workSize.y)

    fun updateWorkRect() {
        workPos.put(pos.x + workOffsetMin.x, pos.y + workOffsetMin.y)
        workSize.put(0f max (size.x - workOffsetMin.x + workOffsetMax.x), 0f max (size.y - workOffsetMin.y + workOffsetMax.y))
    }

    /** ~GetViewportDrawList */
    fun getDrawList(drawlistNo: Int, drawlistName: String): DrawList {
        // Create the draw list on demand, because they are not frequently used for all viewports
        assert(drawlistNo in drawLists.indices)
        val drawList = drawLists[drawlistNo] ?: DrawList(g.drawListSharedData).apply {
            _ownerName = drawlistName
            drawLists[drawlistNo] = this
        }

        // Our ImDrawList system requires that there is always a command
        if (drawListsLastFrame[drawlistNo] != g.frameCount) {
            drawList._resetForNewFrame()
            drawList pushTextureID g.io.fonts.texID
            drawList.pushClipRect(pos, pos + size, false)
            drawListsLastFrame[drawlistNo] = g.frameCount
        }
        return drawList
    }

    /** ~SetupViewportDrawData */
    infix fun setupDrawData(drawLists: ArrayList<DrawList>) {
        drawDataP!!.apply {
            valid = true
            cmdLists.clear()
            if (drawLists.isNotEmpty())
                cmdLists += drawLists
            totalIdxCount = 0
            totalVtxCount = 0
            displayPos put pos
            displaySize put size
            framebufferScale put ImGui.io.displayFramebufferScale
            for (n in 0 until drawLists.size) {
                totalVtxCount += drawLists[n].vtxBuffer.lim
                totalIdxCount += drawLists[n].idxBuffer.lim
            }
        }
    }
}