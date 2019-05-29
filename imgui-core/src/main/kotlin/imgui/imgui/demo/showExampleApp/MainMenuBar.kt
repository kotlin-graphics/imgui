package imgui.imgui.demo.showExampleApp

import imgui.ImGui.menuItem
import imgui.ImGui.separator
import imgui.dsl.mainMenuBar
import imgui.dsl.menu
import imgui.imgui.imgui_demoDebugInformations.Companion.showExampleMenuFile
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf

/** Demonstrate creating a "main" fullscreen menu bar and populating it.
 *  Note the difference between BeginMainMenuBar() and BeginMenuBar():
 *  - BeginMenuBar() = menu-bar inside current window we Begin()-ed into (the window needs the ImGuiWindowFlags_MenuBar flag)
 *  - BeginMainMenuBar() = helper to create menu-bar-sized window at the top of the main viewport + call BeginMenuBar() into it.   */
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