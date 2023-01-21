package imgui.demo.showExampleApp

import glm_.vec2.Vec2
import imgui.ImGui
import imgui.ImGui.checkboxFlags
import imgui.ImGui.indent
import imgui.ImGui.io
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.dsl
import imgui.dsl.indent
import imgui.dsl.window
import imgui.or
import kotlin.reflect.KMutableProperty0
import imgui.WindowFlag as Wf

//-----------------------------------------------------------------------------
// [SECTION] Example App: Fullscreen window / ShowExampleAppFullscreen()
//-----------------------------------------------------------------------------

// Demonstrate creating a window covering the entire screen/viewport
object Fullscreen {

    var flags = Wf.NoDecoration or Wf.NoMove or Wf.NoResize or Wf.NoSavedSettings
    operator fun invoke(pOpen: KMutableProperty0<Boolean>) {
        setNextWindowPos(Vec2())
        setNextWindowSize(Vec2(io.displaySize))
        window("Example: Fullscreen window", pOpen, flags) {
            checkboxFlags("ImGuiWindowFlags_NoBackground", ::flags, Wf.NoBackground.i)
            checkboxFlags("ImGuiWindowFlags_NoDecoration", ::flags, Wf.NoDecoration.i)
            indent {
                checkboxFlags("ImGuiWindowFlags_NoTitleBar", ::flags, Wf.NoTitleBar.i)
                checkboxFlags("ImGuiWindowFlags_NoCollapse", ::flags, Wf.NoCollapse.i)
                checkboxFlags("ImGuiWindowFlags_NoScrollbar", ::flags, Wf.NoScrollbar.i)
            }
        }
    }
}