package imgui.imgui.demo.showExampleApp

import imgui.ImGui.menuItem
import imgui.ImGui.separator
import imgui.functionalProgramming.mainMenuBar
import imgui.functionalProgramming.menu
import imgui.imgui.imgui_demoDebugInformations.Companion.showExampleMenuFile
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf

/** Demonstrate creating a fullscreen menu bar and populating it.   */
object MainMenuBar {

    operator fun invoke() = mainMenuBar {
        menu("File") { showExampleMenuFile() }
        menu("Edit") {
            menuItem("Undo", "CTRL+Z")
            menuItem("Redo", "CTRL+Y", false, false) // Disabled item
            separator()
            menuItem("Cut", "CTRL+X")
            menuItem("Copy", "CTRL+C")
            menuItem("Paste", "CTRL+V")
        }
    }
}