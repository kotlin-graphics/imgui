package imgui.imgui

import glm_.vec2.Vec2
import imgui.ImGui.currentWindow

/** Clipping */
interface imgui_clipping {

    /** When using this function it is sane to ensure that float are perfectly rounded to integer values, to that e.g.
    (int)(max.x-min.x) in user's render produce correct result. */
    fun pushClipRect(clipRectMin: Vec2, clipRectMax: Vec2, intersectWithCurrentClipRect: Boolean) {
        with(currentWindow) {
            drawList.pushClipRect(clipRectMin, clipRectMax, intersectWithCurrentClipRect)
            clipRect put drawList._clipRectStack.last()
        }
    }

    fun popClipRect() {
        with(currentWindow) {
            drawList.popClipRect()
            clipRect put drawList._clipRectStack.last()
        }
    }
}