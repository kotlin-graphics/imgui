package imgui.imgui.demo.showExampleApp

import gli_.has
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.io
import imgui.ImGui.isMousePosValid
import imgui.ImGui.menuItem
import imgui.ImGui.separator
import imgui.ImGui.setNextWindowBgAlpha
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.text
import imgui.functionalProgramming.menuItem
import imgui.functionalProgramming.popupContextWindow
import imgui.functionalProgramming.withWindow
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf

object SimpleOverlay {

    var corner = 0

    /** Demonstrate creating a simple static window with no decoration + a context-menu to choose which corner
     *  of the screen to use */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        val DISTANCE = 10f

        var flags = Wf.NoDecoration or Wf.AlwaysAutoResize or Wf.NoSavedSettings or Wf.NoFocusOnAppearing or Wf.NoNav
        if (corner != -1) {
            val windowPos = Vec2{ if (corner has it + 1) io.displaySize[it] - DISTANCE else DISTANCE }
            val windowPosPivot = Vec2(if (corner has 1) 1f else 0f, if (corner has 2) 1f else 0f)
            setNextWindowPos(windowPos, Cond.Always, windowPosPivot)
            flags = flags or Wf.NoMove
        }
        setNextWindowBgAlpha(0.35f)  // Transparent background
        withWindow("Example: Simple Overlay", open, flags) {
            text("Simple overlay\nin the corner of the screen.\n(right-click to change position)")
            separator()
            text("Mouse Position: " + when {
                isMousePosValid() -> "(%.1f,%.1f)".format(io.mousePos.x, io.mousePos.y)
                else -> "<invalid>"
            })
            popupContextWindow {
                menuItem("Custom", "", corner == -1) { corner = -1 }
                menuItem("Top-left", "", corner == 0) { corner = 0 }
                menuItem("Top-right", "", corner == 1) { corner = 1 }
                menuItem("Bottom-left", "", corner == 2) { corner = 2 }
                menuItem("Bottom-right", "", corner == 3) { corner = 3 }
                if (open() && menuItem("Close")) open.set(false)
            }
        }
    }
}