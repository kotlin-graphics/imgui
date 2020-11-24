package imgui.api

import glm_.vec2.Vec2
import imgui.ImGui.currentWindow

/** Clipping */
interface clipping {

    /** Push a clipping rectangle for both ImGui logic (hit-testing etc.) and low-level ImDrawList rendering.
     * - When using this function it is sane to ensure that float are perfectly rounded to integer values,
     *   so that e.g. (int)(max.x-min.x) in user's render produce correct result.
     * - If the code here changes, may need to update code of functions like NextColumn() and PushColumnClipRect():
     *   some frequently called functions which to modify both channels and clipping simultaneously tend to use the
     *   more specialized SetWindowClipRectBeforeSetChannel() to avoid extraneous updates of underlying ImDrawCmds. */
    fun pushClipRect(clipRectMin: Vec2, clipRectMax: Vec2, intersectWithCurrentClipRect: Boolean) {
        currentWindow.apply {
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