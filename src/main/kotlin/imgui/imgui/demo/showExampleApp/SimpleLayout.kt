package imgui.imgui.demo.showExampleApp

import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.beginChild
import imgui.ImGui.beginGroup
import imgui.ImGui.begin_
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endGroup
import imgui.ImGui.frameHeightWithSpacing
import imgui.ImGui.menuItem
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.text
import imgui.ImGui.textWrapped
import imgui.functionalProgramming.button
import imgui.functionalProgramming.menu
import imgui.functionalProgramming.menuBar
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf

object SimpleLayout {

    var selectedChild = 0

    /** Demonstrate create a window with multiple child windows.    */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        setNextWindowSize(Vec2(500, 440), Cond.FirstUseEver)
        if (begin_("Example: SimpleLayout", open, Wf.MenuBar.i)) {
            menuBar {
                menu("File") {
                    if (menuItem("Close")) open.set(false)
                }
            }

            // left
            beginChild("left pane", Vec2(150, 0), true)
            repeat(100) {
                if (selectable("MyObject $it", selectedChild == it))
                    selectedChild = it
            }
            endChild()
            sameLine()

            // right
            beginGroup()
            beginChild("item view", Vec2(0, -frameHeightWithSpacing)) // Leave room for 1 line below us
            text("MyObject: ${selectedChild}")
            separator()
            textWrapped("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor " +
                    "incididunt ut labore et dolore magna aliqua. ")
            endChild()
            button("Revert") {}
            sameLine()
            button("Save") {}
            endGroup()
        }
        end()
    }
}