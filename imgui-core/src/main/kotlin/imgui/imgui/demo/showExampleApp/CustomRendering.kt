package imgui.imgui.demo.showExampleApp

import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.COL32
import imgui.Cond
import imgui.DrawCornerFlags
import imgui.ImGui
import imgui.ImGui.begin
import imgui.ImGui.beginTabBar
import imgui.ImGui.beginTabItem
import imgui.ImGui.checkbox
import imgui.ImGui.colorEdit4
import imgui.ImGui.dragFloat
import imgui.ImGui.dummy
import imgui.ImGui.end
import imgui.ImGui.endTabBar
import imgui.ImGui.endTabItem
import imgui.ImGui.getColorU32
import imgui.ImGui.invisibleButton
import imgui.ImGui.sameLine
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.text
import imgui.dsl.button
import imgui.internal.DrawCornerFlag
import imgui.internal.or
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf

object CustomRendering {

    var sz = 36f
    var thickness = 3f
    val colf = Vec4(1.0f, 1.0f, 0.4f, 1.0f)

    var addingLine = false
    val points = ArrayList<Vec2>()

    var drawBg = true
    var drawFg = true

    /** Demonstrate using the low-level ImDrawList to draw custom shapes.   */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        setNextWindowSize(Vec2(350, 560), Cond.FirstUseEver)
        if (!begin("Example: Custom rendering", open)) {
            end()
            return
        }

        /*  Tip: If you do a lot of custom rendering, you probably want to use your own geometrical types and
            benefit of overloaded operators, etc.
            Define IM_VEC2_CLASS_EXTRA in imconfig.h to create implicit conversions between your types and
            ImVec2/ImVec4.
            ImGui defines overloaded operators but they are internal to imgui.cpp and not exposed outside
            (to avoid messing with your types)
            In this example we are not using the maths operators!   */
        val drawList = ImGui.windowDrawList

        if (beginTabBar("##TabBar")) {

            // Primitives
            if (beginTabItem("Primitives")) {

                dragFloat("Size", ::sz, 0.2f, 2.0f, 72.0f, "%.0f")
                dragFloat("Thickness", ::thickness, 0.05f, 1.0f, 8.0f, "%.02f")
                colorEdit4("Color", colf)
                val p = ImGui.cursorScreenPos
                val col = getColorU32(colf)
                var x = p.x + 4f
                var y = p.y + 4f
                val spacing = 10f
                val corners_none: DrawCornerFlags = 0
                val corners_all: DrawCornerFlags = DrawCornerFlag.All.i
                val corners_tl_br: DrawCornerFlags = DrawCornerFlag.TopLeft or DrawCornerFlag.BotRight
                for (n in 0 until 2) {
                    // First line uses a thickness of 1.0f, second line uses the configurable thickness
                    val th = if (n == 0) 1.0f else thickness
                    drawList.apply {
                        addCircle(Vec2(x + sz * 0.5f, y + sz * 0.5f), sz * 0.5f, col, 6, th); x += sz + spacing  // Hexagon
                        addCircle(Vec2(x + sz * 0.5f, y + sz * 0.5f), sz * 0.5f, col, 20, th); x += sz + spacing  // Circle
                        addRect(Vec2(x, y), Vec2(x + sz, y + sz), col, 0.0f, corners_none, th); x += sz + spacing  // Square
                        addRect(Vec2(x, y), Vec2(x + sz, y + sz), col, 10f, corners_all, th); x += sz + spacing  // Square with all rounded corners
                        addRect(Vec2(x, y), Vec2(x + sz, y + sz), col, 10f, corners_tl_br, th); x += sz + spacing  // Square with two rounded corners
                        addTriangle(Vec2(x + sz * 0.5f, y), Vec2(x + sz, y + sz - 0.5f), Vec2(x, y + sz - 0.5f), col, th); x += sz + spacing      // Triangle
                        addTriangle(Vec2(x + sz * 0.2f, y), Vec2(x, y + sz - 0.5f), Vec2(x + sz * 0.4f, y + sz - 0.5f), col, th); x += sz * 0.4f + spacing // Thin triangle
                        addLine(Vec2(x, y), Vec2(x + sz, y), col, th); x += sz + spacing  // Horizontal line (note: drawing a filled rectangle will be faster!)
                        addLine(Vec2(x, y), Vec2(x, y + sz), col, th); x += spacing       // Vertical line (note: drawing a filled rectangle will be faster!)
                        addLine(Vec2(x, y), Vec2(x + sz, y + sz), col, th); x += sz + spacing  // Diagonal line
                        addBezierCurve(Vec2(x, y), Vec2(x + sz * 1.3f, y + sz * 0.3f), Vec2(x + sz - sz * 1.3f, y + sz - sz * 0.3f), Vec2(x + sz, y + sz), col, th)
                    }
                    x = p.x + 4
                    y += sz + spacing
                }
                drawList.apply {
                    addCircleFilled(Vec2(x + sz * 0.5f, y + sz * 0.5f), sz * 0.5f, col, 6); x += sz + spacing  // Hexagon
                    addCircleFilled(Vec2(x + sz * 0.5f, y + sz * 0.5f), sz * 0.5f, col, 32); x += sz + spacing  // Circle
                    addRectFilled(Vec2(x, y), Vec2(x + sz, y + sz), col); x += sz + spacing  // Square
                    addRectFilled(Vec2(x, y), Vec2(x + sz, y + sz), col, 10f); x += sz + spacing  // Square with all rounded corners
                    addRectFilled(Vec2(x, y), Vec2(x + sz, y + sz), col, 10f, corners_tl_br); x += sz + spacing  // Square with two rounded corners
                    addTriangleFilled(Vec2(x + sz * 0.5f, y), Vec2(x + sz, y + sz - 0.5f), Vec2(x, y + sz - 0.5f), col); x += sz + spacing      // Triangle
                    addTriangleFilled(Vec2(x + sz * 0.2f, y), Vec2(x, y + sz - 0.5f), Vec2(x + sz * 0.4f, y + sz - 0.5f), col); x += sz * 0.4f + spacing // Thin triangle
                    addRectFilled(Vec2(x, y), Vec2(x + sz, y + thickness), col); x += sz + spacing  // Horizontal line (faster than AddLine, but only handle integer thickness)
                    addRectFilled(Vec2(x, y), Vec2(x + thickness, y + sz), col); x += spacing * 2f  // Vertical line (faster than AddLine, but only handle integer thickness)
                    addRectFilled(Vec2(x, y), Vec2(x + 1, y + 1), col); x += sz            // Pixel (faster than AddLine)
                }
                dummy(Vec2((sz + spacing) * 9.8f, (sz + spacing) * 3))
                endTabItem()
            }

            if (beginTabItem("Canvas")) {

                button("Clear") { points.clear() }
                if (points.size >= 2) {
                    sameLine()
                    button("Undo") {
                        points.removeAt(points.lastIndex)
                        points.removeAt(points.lastIndex)
                    }
                }
                text("Left-click and drag to add lines,\nRight-click to undo")

                /*  Here we are using InvisibleButton() as a convenience to 1) advance the cursor and 2) allows us to use IsItemHovered()
                    But you can also draw directly and poll mouse/keyboard by yourself. You can manipulate the cursor using GetCursorPos() and SetCursorPos().
                    If you only use the ImDrawList API, you can notify the owner window of its extends by using SetCursorPos(max).
                 */
                val canvasPos = Vec2(ImGui.cursorScreenPos)       // ImDrawList API uses screen coordinates!
                val canvasSize = Vec2(ImGui.contentRegionAvail)   // Resize canvas to what's available
                if (canvasSize.x < 50.0f) canvasSize.x = 50.0f
                if (canvasSize.y < 50.0f) canvasSize.y = 50.0f
                drawList.addRectFilledMultiColor(canvasPos, canvasPos + canvasSize, getColorU32(50, 50, 50, 255), getColorU32(50, 50, 60, 255), getColorU32(60, 60, 70, 255), getColorU32(50, 50, 60, 255))
                drawList.addRect(canvasPos, canvasPos + canvasSize, getColorU32(255, 255, 255, 255))

                var addingPreview = false
                invisibleButton("canvas", canvasSize)
                val mousePosInCanvas = ImGui.io.mousePos - canvasPos
                if (addingLine) {
                    addingPreview = true
                    points += mousePosInCanvas
                    if (!ImGui.isMouseDown(0)) {
                        addingLine = false
                        addingPreview = false
                    }
                }
                if (ImGui.isItemHovered()) {
                    if (!addingLine and ImGui.isMouseClicked(0)) {
                        points += mousePosInCanvas
                        addingLine = true
                    }
                    if (ImGui.isMouseClicked(1) and points.isNotEmpty()) {
                        addingLine = false
                        addingPreview = false
                        points.removeAt(points.lastIndex)
                        points.removeAt(points.lastIndex)
                    }
                }
                drawList.pushClipRect(canvasPos, canvasPos + canvasSize, true)
                for (i in 0 until points.lastIndex step 2)
                    drawList.addLine(canvasPos + points[i], canvasPos + points[i + 1], getColorU32(255, 255, 0, 255), 2f)
                drawList.popClipRect()
                if (addingPreview)
                    points.removeAt(points.lastIndex)
                endTabItem()
            }

            if (beginTabItem("BG/FG draw lists")) {
                checkbox("Draw in Background draw list", ::drawBg)
                checkbox("Draw in Foreground draw list", ::drawFg)
                val windowPos = ImGui.windowPos
                val windowSize = ImGui.windowSize
                val windowCenter = Vec2(windowPos.x + windowSize.x * 0.5f, windowPos.y + windowSize.y * 0.5f)
                if (drawBg)
                    ImGui.backgroundDrawList.addCircle(windowCenter, windowSize.x * 0.6f, COL32(255, 0, 0, 200), 32, 10f + 4)
                if (drawFg)
                    ImGui.foregroundDrawList.addCircle(windowCenter, windowSize.y * 0.6f, COL32(0, 255, 0, 200), 32, 10f)
                endTabItem()
            }
            endTabBar()
        }

        end()
    }
}