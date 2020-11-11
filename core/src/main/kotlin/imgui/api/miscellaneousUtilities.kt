package imgui.api

import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.beginChild
import imgui.ImGui.currentWindowRead
import imgui.ImGui.endChild
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.style
import imgui.classes.DrawList
import imgui.internal.classes.Rect
import imgui.internal.sections.DrawListSharedData
import imgui.WindowFlag as Wf

/** Miscellaneous Utilities */
interface miscellaneousUtilities {

    /** test if rectangle (of given size, starting from cursor position) is visible / not clipped.  */
    fun isRectVisible(size: Vec2): Boolean = with(currentWindowRead!!) { clipRect overlaps Rect(dc.cursorPos, dc.cursorPos + size) }

    /** test if rectangle (in screen space) is visible / not clipped. to perform coarse clipping on user's side.    */
    fun isRectVisible(rectMin: Vec2, rectMax: Vec2): Boolean = currentWindowRead!!.clipRect overlaps Rect(rectMin, rectMax)

    /** ~GetTime */
    val time: Double
        get() = g.time

    /** ~GetFrameCount */
    val frameCount: Int
        get() = g.frameCount

    /** this draw list will be the first rendering one. Useful to quickly draw shapes/text behind dear imgui contents.
     *  ~GetBackgroundDrawList  */
    val backgroundDrawList: DrawList
        get() = g.backgroundDrawList

    /** this draw list will be the last rendered one. Useful to quickly draw shapes/text over dear imgui contents.
     *  ~GetForegroundDrawList  */
    val foregroundDrawList: DrawList
        get() = g.foregroundDrawList

    /** you may use this when creating your own ImDrawList instances.
     *  ~GetDrawListSharedData  */
    val drawListSharedData: DrawListSharedData
        get() = g.drawListSharedData

    /** Useless on JVM with Enums */
    //IMGUI_API const char*   GetStyleColorName(ImGuiCol idx);

    var stateStorage: HashMap<ID, Boolean>?
        /** ~GetStateStorage */
        get() = g.currentWindow!!.dc.stateStorage
        /** ~SetStateStorage */
        set(value) {
            val window = g.currentWindow!!
            window.dc.stateStorage = value ?: window.stateStorage
        }

    /** calculate coarse clipping for large list of evenly sized items. Prefer using the ImGuiListClipper higher-level
     *  helper if you can.
     *  Helper to calculate coarse clipping of large list of evenly sized items.
     *  NB: Prefer using the ImGuiListClipper higher-level helper if you can! Read comments and instructions there on
     *  how those use this sort of pattern.
     *  NB: 'items_count' is only used to clamp the result, if you don't know your count you can use INT_MAX    */
    fun calcListClipping(itemsCount: Int, itemsHeight: Float): Pair<Int, Int> {
        val window = g.currentWindow!!
        return when {
            g.logEnabled -> 0 to itemsCount // If logging is active, do not perform any clipping
            window.skipItems -> 0 to 0
            else -> {
                // We create the union of the ClipRect and the NavScoringRect which at worst should be 1 page away from ClipRect
                val unclippedRect = window.clipRect
                if (g.navMoveRequest)
                    unclippedRect add g.navScoringRect
                if (g.navJustMovedToId != 0 && window.navLastIds[0] == g.navJustMovedToId)
                    unclippedRect add Rect(window.pos + window.navRectRel[0].min, window.pos + window.navRectRel[0].max)

                val pos = window.dc.cursorPos
                var start = ((unclippedRect.min.y - pos.y) / itemsHeight).i
                var end = ((unclippedRect.max.y - pos.y) / itemsHeight).i

                // When performing a navigation request, ensure we have one item extra in the direction we are moving to
                if (g.navMoveRequest && g.navMoveDir == Dir.Up)
                    start--
                if (g.navMoveRequest && g.navMoveDir == Dir.Down)
                    end++
                start = glm.clamp(start, 0, itemsCount)
                end = glm.clamp(end + 1, start, itemsCount)
                start to end
            }
        }
    }


    /** helper to create a child window / scrolling region that looks like a normal widget frame    */
    fun beginChildFrame(id: ID, size: Vec2, extraFlags: WindowFlags = 0): Boolean {
        pushStyleColor(Col.ChildBg, style.colors[Col.FrameBg])
        pushStyleVar(StyleVar.ChildRounding, style.frameRounding)
        pushStyleVar(StyleVar.ChildBorderSize, style.frameBorderSize)
        pushStyleVar(StyleVar.WindowPadding, style.framePadding)
        return beginChild(id, size, true, Wf.NoMove or Wf.AlwaysUseWindowPadding or extraFlags).also {
            popStyleVar(3)
            popStyleColor()
        }
    }

    /** Always call EndChildFrame() regardless of BeginChildFrame() return values (which indicates a collapsed/clipped window)  */
    fun endChildFrame() = endChild()
}