package imgui.imgui.demo.showExampleApp

import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.beginTabBar
import imgui.ImGui.beginTabItem
import imgui.ImGui.begin_
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
import imgui.functionalProgramming.button
import imgui.internal.DrawCornerFlag
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf

object CustomRendering {

    var sz = 36f
    var thickness = 4f
    val col = Vec4(1.0f, 1.0f, 0.4f, 1.0f)

    var addingLine = false
    val points = ArrayList<Vec2>()

    var drawBg = true
    var drawFg = true

    /** Demonstrate using the low-level ImDrawList to draw custom shapes.   */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        setNextWindowSize(Vec2(350, 560), Cond.FirstUseEver)
        if (!begin_("Example: Custom rendering", open)) {
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
                colorEdit4("Color", col)
                val p = ImGui.cursorScreenPos
                val col32 = getColorU32(col)
                var x = p.x + 4.0f
                var y = p.y + 4.0f
                val spacing = 8.0f
                for (n in 0 until 2) {
                    // First line uses a thickness of 1.0, second line uses the configurable thickness
                    val th = if (n == 0) 1.0f else thickness
                    drawList.addCircle(Vec2(x + sz * 0.5f, y + sz * 0.5f), sz * 0.5f, col32, 6, th) // Hexagon
                    x += sz + spacing
                    drawList.addCircle(Vec2(x + sz * 0.5f, y + sz * 0.5f), sz * 0.5f, col32, 20, th)          // Circle
                    x += sz + spacing
                    drawList.addRect(Vec2(x, y), Vec2(x + sz, y + sz), col32, 0.0f, DrawCornerFlag.All.i, th)
                    x += sz + spacing
                    drawList.addRect(Vec2(x, y), Vec2(x + sz, y + sz), col32, 10.0f, DrawCornerFlag.All.i, th)
                    x += sz + spacing
                    drawList.addRect(Vec2(x, y), Vec2(x + sz, y + sz), col32, 10.0f, DrawCornerFlag.TopLeft.i or DrawCornerFlag.BotRight.i, th)
                    x += sz + spacing
                    drawList.addTriangle(Vec2(x + sz * 0.5f, y), Vec2(x + sz, y + sz - 0.5f), Vec2(x, y + sz - 0.5f), col32, th)
                    x += sz + spacing
                    drawList.addLine(Vec2(x, y), Vec2(x + sz, y), col32, th)  // Horizontal line (note: drawing a filled rectangle will be faster!)
                    x += sz + spacing
                    drawList.addLine(Vec2(x, y), Vec2(x, y + sz), col32, th) // Vertical line (note: drawing a filled rectangle will be faster!)
                    x += spacing
                    drawList.addLine(Vec2(x, y), Vec2(x + sz, y + sz), col32, th) // Diagonal line
                    x += sz + spacing
                    drawList.addBezierCurve(Vec2(x, y), Vec2(x + sz * 1.3f, y + sz * 0.3f), Vec2(x + sz - sz * 1.3f, y + sz - sz * 0.3f), Vec2(x + sz, y + sz), col32, th)
                    x = p.x + 4
                    y += sz + spacing
                }
                drawList.addCircleFilled(Vec2(x + sz * 0.5f, y + sz * 0.5f), sz * 0.5f, col32, 6)
                x += sz + spacing
                drawList.addCircleFilled(Vec2(x + sz * 0.5f, y + sz * 0.5f), sz * 0.5f, col32, 32)
                x += sz + spacing
                drawList.addRectFilled(Vec2(x, y), Vec2(x + sz, y + sz), col32)
                x += sz + spacing
                drawList.addRectFilled(Vec2(x, y), Vec2(x + sz, y + sz), col32, 10.0f)
                x += sz + spacing
                drawList.addRectFilled(Vec2(x, y), Vec2(x + sz, y + sz), col32, 10.0f, DrawCornerFlag.TopLeft.i or DrawCornerFlag.BotRight.i)
                x += sz + spacing
                drawList.addTriangleFilled(Vec2(x + sz * 0.5f, y), Vec2(x + sz, y + sz - 0.5f), Vec2(x, y + sz - 0.5f), col32)
                x += sz + spacing
                drawList.addRectFilled(Vec2(x, y), Vec2(x + sz, y + thickness), col32)
                x += sz + spacing
                drawList.addRectFilled(Vec2(x, y), Vec2(x + thickness, y + sz), col32)
                x += spacing + spacing
                drawList.addRectFilled(Vec2(x, y), Vec2(x + 1, y + 1), col32)
                x += sz
                drawList.addRectFilledMultiColor(Vec2(x, y), Vec2(x + sz, y + sz), getColorU32(0, 0, 0, 255), getColorU32(255, 0, 0, 255),
                        getColorU32(255, 255, 0, 255), getColorU32(0, 255, 0, 255))
                dummy(Vec2((sz + spacing) * 8, (sz + spacing) * 3))
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
                    ImGui.backgroundDrawList.addCircle(windowCenter, windowSize.x * 0.6f, COL32(255, 0, 0, 200), 32, 10f)
                if (drawFg)
                    ImGui.foregroundDrawList.addCircle(windowCenter, windowSize.y * 0.6f, COL32(0, 255, 0, 200), 32, 10f)
                endTabItem()
            }
            endTabBar()
        }

        end()
    }
}