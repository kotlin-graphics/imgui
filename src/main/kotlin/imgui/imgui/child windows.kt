package imgui.imgui

import gli_.has
import glm_.glm
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.currentWindow
import imgui.ImGui.end
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.renderNavHighlight
import imgui.internal.Axis
import imgui.internal.NavHighlightFlag
import imgui.internal.Rect
import imgui.internal.shl
import imgui.FocusedFlag as Ff
import imgui.HoveredFlag as Hf
import imgui.WindowFlag as Wf
import imgui.internal.ButtonFlag as Bf
import imgui.internal.DrawCornerFlag as Dcf
import imgui.internal.DrawListFlag as Dlf
import imgui.internal.ItemFlag as If
import imgui.internal.LayoutType as Lt


interface imgui_childWindows {

    // Child Windows

    /** - Use child windows to begin into a self-contained independent scrolling/clipping regions within a host window. Child windows can embed their own child.
     *  - For each independent axis of 'size': ==0.0f: use remaining host window size / >0.0f: fixed size
     *      / <0.0f: use remaining window size minus abs(size) / Each axis can use a different mode, e.g. ImVec2(0,400).
     *  - BeginChild() returns false to indicate the window is collapsed or fully clipped, so you may early out and omit submitting anything to the window.
     *    Always call a matching EndChild() for each BeginChild() call, regardless of its return value [this is due to legacy reason and
     *    is inconsistent with most other functions such as BeginMenu/EndMenu, BeginPopup/EndPopup, etc. where the EndXXX call
     *    should only be called if the corresponding BeginXXX function returned true.]  */
    fun beginChild(strId: String, size: Vec2 = Vec2(), border: Boolean = false, flags: WindowFlags = 0): Boolean =
            beginChildEx(strId, currentWindow.getId(strId), size, border, flags)

    /** begin a scrolling region.
     *  size == 0f: use remaining window size
     *  size < 0f: use remaining window size minus abs(size)
     *  size > 0f: fixed size. each axis can use a different mode, e.g. Vec2(0, 400).   */
    fun beginChild(id: ID, sizeArg: Vec2 = Vec2(), border: Boolean = false, flags: WindowFlags = 0): Boolean {
        assert(id != 0)
        return beginChildEx("", id, sizeArg, border, flags)
    }

    /** Always call even if BeginChild() return false (which indicates a collapsed or clipping child window)    */
    fun endChild() {

        val window = currentWindow

        assert(window.flags has Wf.ChildWindow) { "Mismatched BeginChild()/EndChild() callss" }
        if (window.beginCount > 1) end()
        else {
            /*  When using auto-filling child window, we don't provide full width/height to ItemSize so that it doesn't
                feed back into automatic size-fitting.             */
            val sz = Vec2(window.size)
            // Arbitrary minimum zero-ish child size of 4.0f causes less trouble than a 0.0f
            if (window.autoFitChildAxes has (1 shl Axis.X))
                sz.x = glm.max(4f, sz.x)
            if (window.autoFitChildAxes has (1 shl Axis.Y))
                sz.y = glm.max(4f, sz.y)
            end()

            val parentWindow = currentWindow
            val bb = Rect(parentWindow.dc.cursorPos, parentWindow.dc.cursorPos + sz)
            itemSize(sz)
            if ((window.dc.navLayerActiveMask != 0 || window.dc.navHasScroll) && window.flags hasnt Wf.NavFlattened) {
                itemAdd(bb, window.childId)
                renderNavHighlight(bb, window.childId)

                // When browsing a window that has no activable items (scroll only) we keep a highlight on the child
                if (window.dc.navLayerActiveMask == 0 && window === g.navWindow)
                    renderNavHighlight(Rect(bb.min - 2, bb.max + 2), g.navId, NavHighlightFlag.TypeThin.i)
            } else // Not navigable into
                itemAdd(bb, 0)
        }
    }
}