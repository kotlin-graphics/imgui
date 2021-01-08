package imgui.api

import glm_.vec2.Vec2
import imgui.ImGui.contentRegionMaxAbs

// Content region
// - Retrieve available space from a given point. GetContentRegionAvail() is frequently useful.
// - Those functions are bound to be redesigned (they are confusing, incomplete and the Min/Max return values are in local window coordinates which increases confusion)
interface contentRegion {

    /** == GetContentRegionMax() - GetCursorPos()
     *
     *  ~GetContentRegionAvail  */
    val contentRegionAvail: Vec2
        get() = g.currentWindow!!.run { contentRegionMaxAbs - dc.cursorPos }

    /** current content boundaries (typically window boundaries including scrolling, or current column boundaries), in
     *  windows coordinates
     *  FIXME: This is in window space (not screen space!). We should try to obsolete all those functions.
     *  ~GetContentRegionMax    */
    val contentRegionMax: Vec2
        /** ~GetContentRegionMax */
        get() {
            val window = g.currentWindow!!
            val mx = window.contentRegionRect.max - window.pos
            if (window.dc.currentColumns != null || g.currentTable != null)
                mx.x = window.workRect.max.x - window.pos.x
            return mx
        }

    /** content boundaries min (roughly (0,0)-Scroll), in window coordinates
     *  ~GetWindowContentRegionMin  */
    val windowContentRegionMin: Vec2
        get() = g.currentWindow!!.run { contentRegionRect.min - pos }

    /** content boundaries max (roughly (0,0)+Size-Scroll) where Size can be override with SetNextWindowContentSize(),
     * in window coordinates
     * ~GetWindowContentRegionMax   */
    val windowContentRegionMax: Vec2
        get() = g.currentWindow!!.run { contentRegionRect.max - pos }

    /** ~GetWindowContentRegionWidth */
    val windowContentRegionWidth: Float
        get() = g.currentWindow!!.contentRegionRect.width
}