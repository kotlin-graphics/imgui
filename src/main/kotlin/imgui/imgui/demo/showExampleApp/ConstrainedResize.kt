package imgui.imgui.demo.showExampleApp

import glm_.f
import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.checkbox
import imgui.ImGui.combo
import imgui.ImGui.dragInt
import imgui.ImGui.sameLine
import imgui.ImGui.setNextWindowSizeConstraints
import imgui.ImGui.setWindowSize
import imgui.ImGui.text
import imgui.functionalProgramming.button
import imgui.functionalProgramming.withItemWidth
import imgui.functionalProgramming.withWindow
import kotlin.math.max
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf

object ConstrainedResize {

    var autoResize = false
    var type = 0
    var displayLines = 10

    /** Demonstrate creating a window with custom resize constraints.   */
    operator fun invoke(open: KMutableProperty0<Boolean>) {
        when (type) {
            0 -> setNextWindowSizeConstraints(Vec2(-1, 0), Vec2(-1, Float.MAX_VALUE))     // Vertical only
            1 -> setNextWindowSizeConstraints(Vec2(0, -1), Vec2(Float.MAX_VALUE, -1))     // Horizontal only
            2 -> setNextWindowSizeConstraints(Vec2(100), Vec2(Float.MAX_VALUE))                 // Width > 100, Height > 100
            3 -> setNextWindowSizeConstraints(Vec2(400, -1), Vec2(500, -1))           // Width 400-500
            4 -> setNextWindowSizeConstraints(Vec2(-1, 400), Vec2(-1, 500))           // Height 400-500
            5 -> setNextWindowSizeConstraints(Vec2(), Vec2(Float.MAX_VALUE), CustomConstraints.square)          // Always Square
            6 -> setNextWindowSizeConstraints(Vec2(), Vec2(Float.MAX_VALUE), CustomConstraints.step, 100)// Fixed Step
        }
        val flags: WindowFlags = if (autoResize) Wf.AlwaysAutoResize.i else 0
        withWindow("Example: Constrained Resize", open, flags) {
            val desc = listOf("Resize vertical only", "Resize horizontal only", "Width > 100, Height > 100",
                    "Width 400-500", "Height 400-500", "Custom: Always Square", "Custom: Fixed Steps (100)")
            button("200x200") { setWindowSize(Vec2(200)) }; sameLine()
            button("500x500") { setWindowSize(Vec2(500)) }; sameLine()
            button("800x200") { setWindowSize(Vec2(800, 200)) }
            withItemWidth(200) {
                combo("Constraint", ::type, desc)
                dragInt("Lines", ::displayLines, 0.2f, 1, 100)
            }
            checkbox("Auto-resize", ::autoResize)
            for (i in 0 until displayLines)
                text(" ".repeat(i * 4) + "Hello, sailor! Making this line long enough for the example.")
        }
    }

    /** Helper functions to demonstrate programmatic constraints    */
    object CustomConstraints {
        val square: SizeCallback = { it.desiredSize put max(it.desiredSize.x, it.desiredSize.y) }
        val step: SizeCallback = {
            val step = (it.userData as Int).f
            it.desiredSize.x = (it.desiredSize.x / step + 0.5f).i * step
            it.desiredSize.y = (it.desiredSize.y / step + 0.5f).i * step
        }

    }
}