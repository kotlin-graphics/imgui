package imgui.demo.showExampleApp

import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.backgroundDrawList
import imgui.ImGui.begin
import imgui.ImGui.beginTabBar
import imgui.ImGui.beginTabItem
import imgui.ImGui.calcItemWidth
import imgui.ImGui.checkbox
import imgui.ImGui.colorEdit4
import imgui.ImGui.cursorScreenPos
import imgui.ImGui.dragFloat
import imgui.ImGui.dummy
import imgui.ImGui.end
import imgui.ImGui.endTabBar
import imgui.ImGui.endTabItem
import imgui.ImGui.fontSize
import imgui.ImGui.foregroundDrawList
import imgui.ImGui.frameHeight
import imgui.ImGui.getColorU32
import imgui.ImGui.getMouseDragDelta
import imgui.ImGui.invisibleButton
import imgui.ImGui.io
import imgui.ImGui.isMouseReleased
import imgui.ImGui.openPopupOnItemClick
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushItemWidth
import imgui.ImGui.sameLine
import imgui.ImGui.sliderInt
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.windowDrawList
import imgui.ImGui.windowPos
import imgui.ImGui.windowSize
import imgui.api.demoDebugInformations.Companion.helpMarker
import imgui.dsl.menuItem
import imgui.internal.sections.ButtonFlag
import imgui.internal.sections.DrawCornerFlag
import kotlin.reflect.KMutableProperty0

object CustomRendering {

    var sz = 36f
    var thickness = 3f
    var gradientSteps = 16
    var ngonSides = 6
    var circleSegmentsOverride = false
    var circleSegmentsOverrideV = 12
    val colf = Vec4(1.0f, 1.0f, 0.4f, 1.0f)

    var addingLine = false
    val points = ArrayList<Vec2>()
    val scrolling = Vec2()
    var optEnableGrid = true
    var optEnableContextMenu = true

    var drawBg = true
    var drawFg = true

    /** Demonstrate using the low-level ImDrawList to draw custom shapes.   */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        if (!begin("Example: Custom rendering", open)) {
            end()
            return
        }

        // Tip: If you do a lot of custom rendering, you probably want to use your own geometrical types and benefit of
        // overloaded operators, etc. Define IM_VEC2_CLASS_EXTRA in imconfig.h to create implicit conversions between your
        // types and ImVec2/ImVec4. Dear ImGui defines overloaded operators but they are internal to imgui.cpp and not
        // exposed outside (to avoid messing with your types) In this example we are not using the maths operators!

        if (beginTabBar("##TabBar")) {

            if (beginTabItem("Primitives")) {

                pushItemWidth(-fontSize * 10)
                val drawList = windowDrawList

                // Draw gradients
                // (note that those are currently exacerbating our sRGB/Linear issues)
                // Calling ImGui::GetColorU32() multiplies the given colors by the current Style Alpha, but you may pass the IM_COL32() directly as well..
                text("Gradients")
                val gradientSize = Vec2(calcItemWidth(), frameHeight)
                run {
                    val p0 = cursorScreenPos
                    val p1 = p0 + gradientSize
                    val colA = getColorU32(COL32(0, 0, 0, 255))
                    val colB = getColorU32(COL32(255))
                    drawList.addRectFilledMultiColor(p0, p1, colA, colB, colB, colA)
                    invisibleButton("##gradient1", gradientSize)
                }
                run {
                    val p0 = cursorScreenPos
                    val p1 = p0 + gradientSize
                    val colA = getColorU32(COL32(0, 255, 0, 255))
                    val colB = getColorU32(COL32(255, 0, 0, 255))
                    drawList.addRectFilledMultiColor(p0, p1, colA, colB, colB, colA)
                    invisibleButton("##gradient2", gradientSize)
                }

                // Draw a bunch of primitives
                text("All primitives")
                dragFloat("Size", ::sz, 0.2f, 2.0f, 72.0f, "%.0f")
                dragFloat("Thickness", ::thickness, 0.05f, 1.0f, 8.0f, "%.02f")
                sliderInt("N-gon sides", ::ngonSides, 3, 12)
                checkbox("##circlesegmentoverride", ::circleSegmentsOverride)
                sameLine(0f, style.itemInnerSpacing.x)
                if (sliderInt("Circle segments", ::circleSegmentsOverrideV, 3, 40))
                    circleSegmentsOverride = true
                colorEdit4("Color", colf)

                val p = cursorScreenPos
                val col = getColorU32(colf)
                val spacing = 10f
                val cornersNone = DrawCornerFlag.None.i
                val cornersAll = DrawCornerFlag.All.i
                val cornersTlBr = DrawCornerFlag.TopLeft or DrawCornerFlag.BotRight
                val circleSegments = if (circleSegmentsOverride) circleSegmentsOverrideV else 0
                var (x, y) = p + 4f
                for (n in 0 until 2) {
                    // First line uses a thickness of 1.0f, second line uses the configurable thickness
                    val th = if (n == 0) 1.0f else thickness
                    drawList.apply {
                        addNgon(Vec2(x + sz * 0.5f, y + sz * 0.5f), sz * 0.5f, col, ngonSides, th); x += sz + spacing  // N-gon
                        addCircle(Vec2(x + sz * 0.5f, y + sz * 0.5f), sz * 0.5f, col, circleSegments, th); x += sz + spacing  // Circle
                        addRect(Vec2(x, y), Vec2(x + sz, y + sz), col, 0.0f, cornersNone, th); x += sz + spacing  // Square
                        addRect(Vec2(x, y), Vec2(x + sz, y + sz), col, 10f, cornersAll, th); x += sz + spacing  // Square with all rounded corners
                        addRect(Vec2(x, y), Vec2(x + sz, y + sz), col, 10f, cornersTlBr, th); x += sz + spacing  // Square with two rounded corners
                        addTriangle(Vec2(x + sz * 0.5f, y), Vec2(x + sz, y + sz - 0.5f), Vec2(x, y + sz - 0.5f), col, th); x += sz + spacing      // Triangle
//                        addTriangle(Vec2(x + sz * 0.2f, y), Vec2(x, y + sz - 0.5f), Vec2(x + sz * 0.4f, y + sz - 0.5f), col, th); x += sz * 0.4f + spacing // Thin triangle
                        addLine(Vec2(x, y), Vec2(x + sz, y), col, th); x += sz + spacing  // Horizontal line (note: drawing a filled rectangle will be faster!)
                        addLine(Vec2(x, y), Vec2(x, y + sz), col, th); x += spacing       // Vertical line (note: drawing a filled rectangle will be faster!)
                        addLine(Vec2(x, y), Vec2(x + sz, y + sz), col, th); x += sz + spacing  // Diagonal line
                        addBezierCurve(Vec2(x, y), Vec2(x + sz * 1.3f, y + sz * 0.3f), Vec2(x + sz - sz * 1.3f, y + sz - sz * 0.3f), Vec2(x + sz, y + sz), col, th)
                    }
                    x = p.x + 4
                    y += sz + spacing
                }
                drawList.apply {
                    addNgonFilled(Vec2(x + sz * 0.5f, y + sz * 0.5f), sz * 0.5f, col, ngonSides); x += sz + spacing  // N-gon
                    addCircleFilled(Vec2(x + sz * 0.5f, y + sz * 0.5f), sz * 0.5f, col, circleSegments); x += sz + spacing  // Circle
                    addRectFilled(Vec2(x, y), Vec2(x + sz, y + sz), col); x += sz + spacing  // Square
                    addRectFilled(Vec2(x, y), Vec2(x + sz, y + sz), col, 10f); x += sz + spacing  // Square with all rounded corners
                    addRectFilled(Vec2(x, y), Vec2(x + sz, y + sz), col, 10f, cornersTlBr); x += sz + spacing  // Square with two rounded corners
                    addTriangleFilled(Vec2(x + sz * 0.5f, y), Vec2(x + sz, y + sz - 0.5f), Vec2(x, y + sz - 0.5f), col); x += sz + spacing      // Triangle
//                    addTriangleFilled(Vec2(x + sz * 0.2f, y), Vec2(x, y + sz - 0.5f), Vec2(x + sz * 0.4f, y + sz - 0.5f), col); x += sz * 0.4f + spacing // Thin triangle
                    addRectFilled(Vec2(x, y), Vec2(x + sz, y + thickness), col); x += sz + spacing  // Horizontal line (faster than AddLine, but only handle integer thickness)
                    addRectFilled(Vec2(x, y), Vec2(x + thickness, y + sz), col); x += spacing * 2f  // Vertical line (faster than AddLine, but only handle integer thickness)
                    addRectFilled(Vec2(x, y), Vec2(x + 1, y + 1), col); x += sz            // Pixel (faster than AddLine)
                }

                dummy(Vec2((sz + spacing) * 8.8f, (sz + spacing) * 3))
                popItemWidth()
                endTabItem()
            }

            if (beginTabItem("Canvas")) {

                checkbox("Enable grid", ::optEnableGrid)
                checkbox("Enable context menu", ::optEnableContextMenu)
                text("Mouse Left: drag to add lines,\nMouse Right: drag to scroll, click for context menu.")

                // Typically you would use a BeginChild()/EndChild() pair to benefit from a clipping region + own scrolling.
                // Here we demonstrate that this can be replaced by simple offsetting + custom drawing + PushClipRect/PopClipRect() calls.
                // To use a child window instead we could use, e.g:
                //      ImGui::PushStyleVar(ImGuiStyleVar_WindowPadding, ImVec2(0, 0));      // Disable padding
                //      ImGui::PushStyleColor(ImGuiCol_ChildBg, IM_COL32(50, 50, 50, 255));  // Set a background color
                //      ImGui::BeginChild("canvas", ImVec2(0.0f, 0.0f), true, ImGuiWindowFlags_NoMove);
                //      ImGui::PopStyleColor();
                //      ImGui::PopStyleVar();
                //      [...]
                //      ImGui::EndChild();

                // Using InvisibleButton() as a convenience 1) it will advance the layout cursor and 2) allows us to use IsItemHovered()/IsItemActive()
                val canvasP0 = Vec2(ImGui.cursorScreenPos)      // ImDrawList API uses screen coordinates!
                val canvasSz = Vec2(ImGui.contentRegionAvail)   // Resize canvas to what's available
                if (canvasSz.x < 50f) canvasSz.x = 50f
                if (canvasSz.y < 50f) canvasSz.y = 50f
                val canvasP1 = canvasP0 + canvasSz

                // Draw border and background color
                val drawList = ImGui.windowDrawList
                drawList.addRectFilled(canvasP0, canvasP1, COL32(50, 50, 50, 255))
                drawList.addRect(canvasP0, canvasP1, COL32(255, 255, 255, 255))

                // This will catch our interactions
                invisibleButton("canvas", canvasSz, ButtonFlag.MouseButtonLeft or ButtonFlag.MouseButtonRight)
                val isHovered = ImGui.isItemHovered() // Hovered
                val isActive = ImGui.isItemActive   // Held
                val origin = canvasP0 + scrolling // Lock scrolled origin
                val mousePosInCanvas = io.mousePos - origin

                // Add first and second point
                if (isHovered && !addingLine && ImGui.isMouseClicked(MouseButton.Left)) {
                    points += mousePosInCanvas // TODO problems with same instance?
                    points += mousePosInCanvas
                    addingLine = true
                }
                if (addingLine) {
                    points.last() put mousePosInCanvas
                    if (!ImGui.isMouseDown(MouseButton.Left))
                        addingLine = false
                }

                // Pan (using zero mouse threshold)
                // Pan (we use a zero mouse threshold when there's no context menu)
                // You may decide to make that threshold dynamic based on whether the mouse is hovering something etc.
                val mouseThresholdForPan = if (optEnableContextMenu) -1f else 0f
                if (isActive && ImGui.isMouseDragging(MouseButton.Right, mouseThresholdForPan))
                    scrolling += io.mouseDelta

                // Context menu (under default mouse threshold)
                val dragDelta = getMouseDragDelta(MouseButton.Right)
                if (optEnableContextMenu && isMouseReleased(MouseButton.Right) && dragDelta.x == 0f && dragDelta.y == 0f) // TODO glm
                    openPopupOnItemClick("context")
                dsl.popup("context") {
                    if (addingLine) {
                        points.pop()
                        points.pop()
                    }
                    addingLine = false
                    menuItem("Remove one", "", false, points.isNotEmpty()) { points.pop(); points.pop(); }
                    menuItem("Remove all", "", false, points.isNotEmpty()) { points.clear() }
                }

                // Draw grid + all lines in the canvas
                drawList.pushClipRect(canvasP0, canvasP1, true)
                if (optEnableGrid) {
                    val GRID_STEP = 64f
                    var x = scrolling.x % GRID_STEP
                    while (x < canvasSz.x) {
                        drawList.addLine(Vec2(canvasP0.x + x, canvasP0.y), Vec2(canvasP0.x + x, canvasP1.y), COL32(200, 200, 200, 40))
                        x += GRID_STEP
                    }
                    var y = scrolling.y % GRID_STEP
                    while (y < canvasSz.y) {
                        drawList.addLine(Vec2(canvasP0.x, canvasP0.y + y), Vec2(canvasP1.x, canvasP0.y + y), COL32(200, 200, 200, 40))
                        y += GRID_STEP
                    }
                }
                for (n in points.indices step 2) {
                    val p0 = points[n]
                    val p1 = points[n + 1]
                    drawList.addLine(origin + p0, origin + p1, COL32(255, 255, 0, 255), 2f)
                }
                drawList.popClipRect()

                endTabItem()
            }

            if (beginTabItem("BG/FG draw lists")) {
                checkbox("Draw in Background draw list", ::drawBg)
                sameLine(); helpMarker("The Background draw list will be rendered below every Dear ImGui windows.")
                checkbox("Draw in Foreground draw list", ::drawFg)
                sameLine(); helpMarker("The Foreground draw list will be rendered over every Dear ImGui windows.")
                val windowCenter = Vec2(windowPos.x + windowSize.x * 0.5f, windowPos.y + windowSize.y * 0.5f)
                if (drawBg)
                    backgroundDrawList.addCircle(windowCenter, windowSize.x * 0.6f, COL32(255, 0, 0, 200), 0, 10f + 4)
                if (drawFg)
                    foregroundDrawList.addCircle(windowCenter, windowSize.y * 0.6f, COL32(0, 255, 0, 200), 0, 10f)
                endTabItem()
            }
            endTabBar()
        }

        end()
    }
}