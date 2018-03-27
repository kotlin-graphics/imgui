package imgui.imgui.demo

import gli_.has
import glm_.f
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.style
import imgui.ImGui.alignTextToFramePadding
import imgui.ImGui.beginChild
import imgui.ImGui.beginMenu
import imgui.ImGui.beginMenuBar
import imgui.ImGui.bullet
import imgui.ImGui.bulletText
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.columns
import imgui.ImGui.combo
import imgui.ImGui.contentRegionAvailWidth
import imgui.ImGui.cursorScreenPos
import imgui.ImGui.cursorStartPos
import imgui.ImGui.dragFloat
import imgui.ImGui.dragInt
import imgui.ImGui.dragVec2
import imgui.ImGui.dummy
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endMenu
import imgui.ImGui.endMenuBar
import imgui.ImGui.font
import imgui.ImGui.fontSize
import imgui.ImGui.frameHeightWithSpacing
import imgui.ImGui.getId
import imgui.ImGui.inputInt
import imgui.ImGui.invisibleButton
import imgui.ImGui.io
import imgui.ImGui.isItemActive
import imgui.ImGui.isItemHovered
import imgui.ImGui.isMouseDragging
import imgui.ImGui.itemRectSize
import imgui.ImGui.listBox
import imgui.ImGui.listBoxFooter
import imgui.ImGui.listBoxHeader
import imgui.ImGui.nextColumn
import imgui.ImGui.plotHistogram
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.sameLine
import imgui.ImGui.scrollMaxX
import imgui.ImGui.scrollMaxY
import imgui.ImGui.scrollX
import imgui.ImGui.scrollY
import imgui.ImGui.selectable
import imgui.ImGui.setScrollFromPosY
import imgui.ImGui.setScrollHere
import imgui.ImGui.setTooltip
import imgui.ImGui.sliderFloat
import imgui.ImGui.sliderInt
import imgui.ImGui.smallButton
import imgui.ImGui.text
import imgui.ImGui.textColored
import imgui.ImGui.textWrapped
import imgui.ImGui.treeNode
import imgui.ImGui.treePop
import imgui.ImGui.windowContentRegionWidth
import imgui.ImGui.windowDrawList
import imgui.ImGui.windowWidth
import imgui.functionalProgramming.collapsingHeader
import imgui.functionalProgramming.treeNode
import imgui.functionalProgramming.withChild
import imgui.functionalProgramming.withGroup
import imgui.functionalProgramming.withId
import imgui.functionalProgramming.withItemWidth
import imgui.functionalProgramming.withStyleVar
import imgui.imgui.imgui_demoDebugInformations.Companion.showExampleMenuFile
import imgui.imgui.imgui_demoDebugInformations.Companion.showHelpMarker
import kotlin.math.sin
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlags as Sf
import imgui.TreeNodeFlags as Tnf
import imgui.WindowFlags as Wf

object layout_ {

    /* Child regions */
    var disableMouseWheel = false
    var disableMenu = false
    var line = 50


    /* Widgets Width */
    var f = 0f


    /* Basic Horizontal Layout */
    var c1 = false
    var c2 = false
    var c3 = false
    var c4 = false
    var f0 = 1f
    var f1 = 2f
    var f2 = 3f
    var item = -1
    val selection = intArrayOf(0, 1, 2, 3)


    /* Scrolling */
    var track = true
    var trackLine = 50
    var scrollToPx = 200


    /* Horizontal Scrolling */
    var lines = 7


    /* Clipping */
    val size = Vec2(100)
    val offset = Vec2(50, 20)

    operator fun invoke() {

        collapsingHeader("Layout") {

            treeNode("Child regions") {

                checkbox("Disable Mouse Wheel", ::disableMouseWheel)
                checkbox("Disable Menu", ::disableMenu)

                var gotoLine = button("Goto")
                sameLine()
                withItemWidth(100) {
                    gotoLine = gotoLine or inputInt("##Line", ::line, 0, 0, Itf.EnterReturnsTrue.i)
                }

                val flags = if (disableMouseWheel) Wf.NoScrollWithMouse else Wf.Null
                withChild("Child1", Vec2(windowContentRegionWidth * 0.5f, 300), false, flags or Wf.HorizontalScrollbar) {
                    for (i in 0..99) {
                        text("%04d: scrollable region", i)
                        if (gotoLine && line == i) setScrollHere()
                    }
                    if (gotoLine && line >= 100) setScrollHere()
                }

                sameLine()

                // Child 2: rounded border
                withStyleVar(StyleVar.ChildRounding, 5f) {
                    withChild("Child2", Vec2(0, 300), true, flags or if(disableMenu) Wf.Null else Wf.MenuBar) {
                        if(!disableMenu && beginMenuBar()) {
                            if (beginMenu("Menu")) {
                                showExampleMenuFile()
                                endMenu()
                            }
                            endMenuBar()
                        }
                        columns(2)
                        for (i in 0..99) {
                            if (i == 50) nextColumn()
                            val text = "%08x".format(style.locale, i * 5731)
                            button(text, Vec2(-1f, 0f))
                        }
                    }
                }
            }

            treeNode("Widgets Width") {

                text("PushItemWidth(100)")
                sameLine(); showHelpMarker("Fixed width.")
                withItemWidth(100) { dragFloat("float##1", ::f) }

                text("PushItemWidth(GetWindowWidth() * 0.5f)")
                sameLine(); showHelpMarker("Half of window width.")
                withItemWidth(windowWidth * 0.5f) { dragFloat("float##2", ::f) }

                text("PushItemWidth(GetContentRegionAvailWidth() * 0.5f)")
                sameLine(); showHelpMarker("Half of available width.\n(~ right-cursor_pos)\n(works within a column set)")
                withItemWidth(contentRegionAvailWidth * 0.5f) { dragFloat("float##3", ::f) }

                text("PushItemWidth(-100)")
                sameLine(); showHelpMarker("Align to right edge minus 100")
                withItemWidth(-100) { dragFloat("float##4", ::f) }

                text("PushItemWidth(-1)")
                sameLine(); showHelpMarker("Align to right edge")
                withItemWidth(-1) { dragFloat("float##5", ::f) }
            }

            treeNode("Basic Horizontal Layout") {

                textWrapped("(Use SameLine() to keep adding items to the right of the preceding item)")

                // Text
                text("Two items: Hello"); sameLine()
                textColored(Vec4(1, 1, 0, 1), "Sailor")

                // Adjust spacing
                text("More spacing: Hello"); sameLine(0, 20)
                textColored(Vec4(1, 1, 0, 1), "Sailor")

                // Button
                alignTextToFramePadding()
                text("Normal buttons"); sameLine()
                button("Banana"); sameLine()
                button("Apple"); sameLine()
                button("Corniflower")

                // Button
                text("Small buttons"); sameLine()
                smallButton("Like this one"); sameLine()
                text("can fit within a text block.")

                // Aligned to arbitrary position. Easy/cheap column.
                text("Aligned")
                sameLine(150); text("x=150")
                sameLine(300); text("x=300")
                text("Aligned")
                sameLine(150); smallButton("x=150")
                sameLine(300); smallButton("x=300")

                // Checkbox
                checkbox("My", ::c1); sameLine()
                checkbox("Tailor", ::c2); sameLine()
                checkbox("Is", ::c3); sameLine()
                checkbox("Rich", ::c4)

                // Various
                val items = arrayOf("AAAA", "BBBB", "CCCC", "DDDD")
                withItemWidth(80f) {
                    combo("Combo", ::item, items); sameLine()
                    sliderFloat("X", ::f0, 0f, 5f); sameLine()
                    sliderFloat("Y", ::f1, 0f, 5f); sameLine()
                    sliderFloat("Z", ::f2, 0f, 5f)
                }

                withItemWidth(80f) {
                    text("Lists:")
                    for (i in 0..3) {
                        if (i > 0) sameLine()
                        withId(i) {
                            withInt(selection, i) {
                                listBox("", it, items)
                            }
                        }
                        //if (IsItemHovered()) SetTooltip("ListBox %d hovered", i);
                    }
                }

                // Dummy
                val sz = Vec2(30)
                button("A", sz); sameLine()
                dummy(sz); sameLine()
                button("B", sz)
            }

            treeNode("Groups") {

                textWrapped("(Using BeginGroup()/EndGroup() to layout items. BeginGroup() basically locks the horizontal position. EndGroup() bundles the whole group so that you can use functions such as IsItemHovered() on it.)")
                withGroup {
                    withGroup {
                        button("AAA")
                        sameLine()
                        button("BBB")
                        sameLine()
                        withGroup {
                            button("CCC")
                            button("DDD")
                        }
                        sameLine()
                        button("EEE")
                    }
                    if (isItemHovered()) setTooltip("First group hovered")

                    // Capture the group size and create widgets using the same size
                    val size = Vec2(itemRectSize)
                    val values = floatArrayOf(0.5f, 0.2f, 0.8f, 0.6f, 0.25f)
                    plotHistogram("##values", values, 0, "", 0f, 1f, size)

                    button("ACTION", Vec2((size.x - style.itemSpacing.x) * 0.5f, size.y))
                    sameLine()
                    button("REACTION", Vec2((size.x - style.itemSpacing.x) * 0.5f, size.y))
                }
                sameLine()

                button("LEVERAGE\nBUZZWORD", size)
                sameLine()

                listBoxHeader("List", size)
                selectable("Selected", true)
                selectable("Not Selected", false)
                listBoxFooter()
            }

            treeNode("Text Baseline Alignment") {

                textWrapped("(This is testing the vertical alignment that occurs on text to keep it at the same baseline as widgets. Lines only composed of text or \"small\" widgets fit in less vertical spaces than lines with normal widgets)")

                text("One\nTwo\nThree"); sameLine()
                text("Hello\nWorld"); sameLine()
                text("Banana")

                text("Banana"); sameLine()
                text("Hello\nWorld"); sameLine()
                text("One\nTwo\nThree")

                button("HOP##1"); sameLine()
                text("Banana"); sameLine()
                text("Hello\nWorld"); sameLine()
                text("Banana")

                button("HOP##2"); sameLine()
                text("Hello\nWorld"); sameLine()
                text("Banana")

                button("TEST##1"); sameLine()
                text("TEST"); sameLine()
                smallButton("TEST##2")

                alignTextToFramePadding() // If your line starts with text, call this to align it to upcoming widgets.
                text("Text aligned to Widget"); sameLine()
                button("Widget##1"); sameLine()
                text("Widget"); sameLine()
                smallButton("Widget##2"); sameLine()
                button("Widget##3")

                // Tree
                val spacing = style.itemInnerSpacing.x
                button("Button##1")
                sameLine(0f, spacing)
                treeNode("Node##1") { for (i in 0..5) bulletText("Item $i..") } // Dummy tree data
                /*  Vertically align text node a bit lower so it'll be vertically centered with upcoming widget.
                    Otherwise you can use SmallButton (smaller fit).                 */
                alignTextToFramePadding()
                // Common mistake to avoid: if we want to SameLine after TreeNode we need to do it before we add child content.
                val nodeOpen = treeNode("Node##2")
                sameLine(0f, spacing); button("Button##2")
                if (nodeOpen) { // Dummy tree data
                    for (i in 0..5) bulletText("Item $i..")
                    treePop()
                }
                // Bullet
                button("Button##3")
                sameLine(0f, spacing)
                bulletText("Bullet text")

                alignTextToFramePadding()
                bulletText("Node")
                sameLine(0f, spacing); button("Button##4")
            }

            treeNode("Scrolling") {

                textWrapped("(Use SetScrollHere() or SetScrollFromPosY() to scroll to a given position.)")
                checkbox("Track", ::track)
                pushItemWidth(100)
                sameLine(130); track = track or dragInt("##line", ::trackLine, 0.25f, 0, 99, "Line = %.0f")
                var scrollTo = button("Scroll To Pos")
                sameLine(130); scrollTo = scrollTo or dragInt("##pos_y", ::scrollToPx, 1f, 0, 9999, "Y = %.0f px")
                popItemWidth()
                if (scrollTo) track = false

                for (i in 0..4) {
                    if (i > 0) sameLine()
                    withGroup {
                        text("%s", if (i == 0) "Top" else if (i == 1) "25%" else if (i == 2) "Center" else if (i == 3) "75%" else "Bottom")
                        beginChild(getId(i), Vec2(windowWidth * 0.17f, 200f), true)
                        if (scrollTo)
                            setScrollFromPosY(cursorStartPos.y + scrollToPx, i * 0.25f)
                        for (line in 0..99)
                            if (track && line == trackLine) {
                                textColored(Vec4.fromColor(255, 255, 0), "Line %d", line)
                                setScrollHere(i * 0.25f) // 0.0f:top, 0.5f:center, 1.0f:bottom
                            } else
                                text("Line $line")
                        val scrollY = scrollY
                        val scrollMaxY = scrollMaxY
                        endChild()
                        text("%.0f/%.0f", scrollY, scrollMaxY)
                    }
                }
            }

            treeNode("Horizontal Scrolling") {

                bullet(); textWrapped("Horizontal scrolling for a window has to be enabled explicitly via the ImGuiWindowFlags_HorizontalScrollbar flag.")
                bullet(); textWrapped("You may want to explicitly specify content width by calling SetNextWindowContentWidth() before Begin().")
                sliderInt("Lines", ::lines, 1, 15)
                pushStyleVar(StyleVar.FrameRounding, 3f)
                pushStyleVar(StyleVar.FramePadding, Vec2(2f, 1f))
                beginChild("scrolling", Vec2(0, frameHeightWithSpacing * 7 + 30), true, Wf.HorizontalScrollbar.i)
                for (line in 0 until lines) {
                    /*  Display random stuff (for the sake of this trivial demo we are using basic button+sameLine.
                        If you want to create your own time line for a real application you may be better off
                        manipulating the cursor position yourself, aka using SetCursorPos/SetCursorScreenPos to position
                        the widgets yourself. You may also want to use the lower-level ImDrawList API)  */
                    val numButtons = 10 + (line * if (line has 1) 9 else 3)
                    for (n in 0 until numButtons) {
                        if (n > 0) sameLine()
                        pushId(n + line * 1000)
                        val label = if (n % 15 == 0) "FizzBuzz" else if (n % 3 == 0) "Fizz" else if (n % 5 == 0) "Buzz" else "$n"
                        val hue = n * 0.05f
                        pushStyleColor(Col.Button, Color.hsv(hue, 0.6f, 0.6f))
                        pushStyleColor(Col.ButtonHovered, Color.hsv(hue, 0.7f, 0.7f))
                        pushStyleColor(Col.ButtonActive, Color.hsv(hue, 0.8f, 0.8f))
                        button(label, Vec2(40f + sin((line + n).f) * 20f, 0f))
                        popStyleColor(3)
                        popId()
                    }
                }
                val _scrollX = scrollX
                val scrollMaxX = scrollMaxX
                endChild()
                popStyleVar(2)
                var scrollXDelta = 0f
                smallButton("<<"); if (isItemActive) scrollXDelta = -io.deltaTime * 1000f; sameLine()
                text("Scroll from code"); sameLine()
                smallButton(">>"); if (isItemActive) scrollXDelta = io.deltaTime * 1000f; sameLine()
                text("%.0f/%.0f", _scrollX, scrollMaxX)
                if (scrollXDelta != 0f) {
                    /*  Demonstrate a trick: you can use begin() to set yourself in the context of another window (here
                        we are already out of your child window) */
                    beginChild("scrolling")
                    scrollX = scrollX + scrollXDelta    // TODO bug https://youtrack.jetbrains.com/issue/KT-21343 wait for 1.2.20 EAP
                    end()
                }
            }

            treeNode("Clipping") {
                textWrapped("On a per-widget basis we are occasionally clipping text CPU-side if it won't fit in its frame. Otherwise we are doing coarser clipping + passing a scissor rectangle to the renderer. The system is designed to try minimizing both execution and CPU/GPU rendering cost.")
                dragVec2("size", size, 0.5f, 0f, 200f, "%.0f")
                textWrapped("(Click and drag)")
                val pos = Vec2(cursorScreenPos)
                val clipRect = Vec4(pos.x, pos.y, pos.x + size.x, pos.y + size.y)
                invisibleButton("##dummy", size)
                if (isItemActive && isMouseDragging()) offset += io.mouseDelta
                windowDrawList.addRectFilled(pos, Vec2(pos.x + size.x, pos.y + size.y), COL32(90, 90, 120, 255))
                windowDrawList.addText(font, fontSize * 2f, Vec2(pos.x + offset.x, pos.y + offset.y),
                        COL32(255, 255, 255, 255), "Line 1 hello\nLine 2 clip me!".toCharArray(), 0, 0f, clipRect)
            }
        }
    }
}

private inline fun <R> withInt(ints: IntArray, ptr: Int, block: (KMutableProperty0<Int>) -> R): R {
    Ref.iPtr++
    val i = Ref::int
    i.set(ints[ptr])
    val res = block(i)
    ints[ptr] = i()
    Ref.iPtr--
    return res
}