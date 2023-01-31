package imgui.api

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
import imgui.internal.classes.Rect
import imgui.internal.sections.DrawListSharedData
import imgui.WindowFlag as Wf

/** Miscellaneous Utilities */
interface miscellaneousUtilities {

    /** test if rectangle (of given size, starting from cursor position) is visible / not clipped.  */
    fun isRectVisible(size: Vec2): Boolean = with(currentWindowRead!!) { clipRect overlaps Rect(dc.cursorPos, dc.cursorPos + size) }

    /** [JVM] */
    fun isRectVisible(rect: Rect): Boolean = isRectVisible(rect.min, rect.max)

    /** test if rectangle (in screen space) is visible / not clipped. to perform coarse clipping on user's side.    */
    fun isRectVisible(rectMin: Vec2, rectMax: Vec2): Boolean = currentWindowRead!!.clipRect overlaps Rect(rectMin, rectMax)

    /** ~GetTime */
    val time: Double
        get() = g.time

    /** ~GetFrameCount */
    val frameCount: Int
        get() = g.frameCount

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