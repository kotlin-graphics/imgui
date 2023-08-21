package imgui.demo.showExampleApp

import glm_.f
import glm_.i
import glm_.max
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.colorButton
import imgui.ImGui.combo
import imgui.ImGui.end
import imgui.ImGui.io
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushStyleVar
import imgui.ImGui.sameLine
import imgui.ImGui.setNextItemWidth
import imgui.ImGui.setNextWindowSizeConstraints
import imgui.ImGui.setWindowSize
import imgui.ImGui.text
import imgui.api.drag
import imgui.classes.SizeCallbackData
import kotlin.reflect.KMutableProperty0


//-----------------------------------------------------------------------------
// [SECTION] Example App: Constrained Resize / ShowExampleAppConstrainedResize()
//-----------------------------------------------------------------------------

// Demonstrate creating a window with custom resize constraints.
// Note that size constraints currently don't work on a docked window (when in 'docking' branch)
object ConstrainedResize {

    // Options
    var autoResize = false
    var windowPadding = true
    var type = 5 // Aspect Ratio
    var displayLines = 10
    val testDesc = listOf("Resize vertical only",
                          "Resize horizontal only",
                          "Width > 100, Height > 100",
                          "Width 400-500",
                          "Height 400-500",
                          "Custom: Always Square",
                          "Custom: Fixed Steps (100)")

    object CustomConstraints {
        // Helper functions to demonstrate programmatic constraints
        // FIXME: This doesn't take account of decoration size (e.g. title bar), library should make this easier.
        fun aspectRatio(data: SizeCallbackData) {
            val aspectRatio = data.userData as Float
            data.desiredSize.x = data.currentSize.x max data.currentSize.y
            data.desiredSize.y = (data.desiredSize.x / aspectRatio).i.f
        }

        fun square(data: SizeCallbackData) = data.desiredSize put (data.currentSize.x max data.currentSize.y)
        fun step(data: SizeCallbackData) {
            val step = data.userData as Float
            data.desiredSize.put((data.currentSize.x / step + 0.5f).i * step, (data.currentSize.y / step + 0.5f).i * step)
        }
    }

    /** Demonstrate creating a window with custom resize constraints.   */
    operator fun invoke(pOpen: KMutableProperty0<Boolean>?) {

        val testDesc = listOf("Between 100x100 and 500x500",
                              "At least 100x100",
                              "Resize vertical only",
                              "Resize horizontal only",
                              "Width Between 400 and 500",
                              "Custom: Aspect Ratio 16:9",
                              "Custom: Always Square",
                              "Custom: Fixed Steps (100)")
        // Submit constraint
        val aspectRatio = 16f / 9f
        val fixedStep = 100f
        when (type) {
            0 -> setNextWindowSizeConstraints(Vec2(100), Vec2(500))         // Between 100x100 and 500x500
            1 -> setNextWindowSizeConstraints(Vec2(100), Vec2(Float.MAX_VALUE)) // Width > 100, Height > 100
            2 -> setNextWindowSizeConstraints(Vec2(-1, 0), Vec2(-1, Float.MAX_VALUE))      // Vertical only
            3 -> setNextWindowSizeConstraints(Vec2(0, -1), Vec2(Float.MAX_VALUE, -1))      // Horizontal only
            4 -> setNextWindowSizeConstraints(Vec2(400, -1), Vec2(500, -1))          // Width Between and 400 and 500
            5 -> setNextWindowSizeConstraints(Vec2(), Vec2(Float.MAX_VALUE), CustomConstraints::aspectRatio, aspectRatio)   // Aspect ratio
            6 -> setNextWindowSizeConstraints(Vec2(), Vec2(Float.MAX_VALUE), CustomConstraints::square)  // Always Square
            7 -> setNextWindowSizeConstraints(Vec2(), Vec2(Float.MAX_VALUE), CustomConstraints::step, fixedStep) // Fixed Step
        }
        // Submit window
        if (!windowPadding)
            pushStyleVar(StyleVar.WindowPadding, Vec2())
        val windowFlags = if (autoResize) WindowFlag.AlwaysAutoResize else none
        val windowOpen = begin("Example: Constrained Resize", pOpen, windowFlags)
        if (!windowPadding)
            popStyleVar()
        if (windowOpen) {
            if (io.keyShift) {
                // Display a dummy viewport (in your real app you would likely use ImageButton() to display a texture.
                val availSize = ImGui.contentRegionAvail
                val pos = ImGui.cursorScreenPos
                colorButton("viewport", Vec4(0.5f, 0.2f, 0.5f, 1f), ColorEditFlag.NoTooltip or ColorEditFlag.NoDragDrop, availSize)
                ImGui.cursorScreenPos = Vec2(pos.x + 10, pos.y + 10)
                text("%.2f x %.2f", availSize.x, availSize.y)
            } else {
                text("(Hold SHIFT to display a dummy viewport)")
                if (button("Set 200x200")) setWindowSize(Vec2(200)); sameLine()
                if (button("Set 500x500")) setWindowSize(Vec2(500)); sameLine()
                if (button("Set 800x200")) setWindowSize(Vec2(800, 200))
                setNextItemWidth(ImGui.fontSize * 20)
                combo("Constraint", ::type, testDesc)
                setNextItemWidth(ImGui.fontSize * 20)
                drag("Lines", ::displayLines, 0.2f, 1, 100)
                checkbox("Auto-resize", ::autoResize)
                checkbox("Window padding", ::windowPadding)
                for (i in 0 until displayLines)
                    text("${"    ".repeat(i)}Hello, sailor! Making this line long enough for the example.")
            }
        }
        end()
    }
}