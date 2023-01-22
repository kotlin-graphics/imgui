package imgui.demo.showExampleApp

import glm_.vec2.Vec2
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.checkboxFlags
import imgui.ImGui.io
import imgui.ImGui.mainViewport
import imgui.ImGui.sameLine
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.api.demoDebugInformations.Companion.helpMarker
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

    var useWorkArea = true
    var flags = Wf.NoDecoration or Wf.NoMove or Wf.NoResize or Wf.NoSavedSettings
    operator fun invoke(pOpen: KMutableProperty0<Boolean>?) {

        // We demonstrate using the full viewport area or the work area (without menu-bars, task-bars etc.)
        // Based on your use case you may want one of the other.
        val viewport = mainViewport
        setNextWindowPos(if(useWorkArea) viewport.workPos else viewport.pos)
        setNextWindowSize(if(useWorkArea) viewport.workSize else viewport.size)

        window("Example: Fullscreen window", pOpen, flags) {

            checkbox("Use work area instead of main area", ::useWorkArea)
            sameLine()
            helpMarker("Main Area = entire viewport,\nWork Area = entire viewport minus sections used by the main menu bars, task bars etc.\n\nEnable the main-menu bar in Examples menu to see the difference.")

            checkboxFlags("ImGuiWindowFlags_NoBackground", ::flags, Wf.NoBackground.i)
            checkboxFlags("ImGuiWindowFlags_NoDecoration", ::flags, Wf.NoDecoration.i)
            indent {
                checkboxFlags("ImGuiWindowFlags_NoTitleBar", ::flags, Wf.NoTitleBar.i)
                checkboxFlags("ImGuiWindowFlags_NoCollapse", ::flags, Wf.NoCollapse.i)
                checkboxFlags("ImGuiWindowFlags_NoScrollbar", ::flags, Wf.NoScrollbar.i)
            }

            if (pOpen != null && button("Close this window"))
                pOpen.set(false)
        }
    }
}