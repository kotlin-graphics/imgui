package imgui.demo

import glm_.L
import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.collapsingHeader
import imgui.ImGui.columnIndex
import imgui.ImGui.columns
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.dragInt
import imgui.ImGui.fontSize
import imgui.ImGui.getColumnOffset
import imgui.ImGui.getColumnWidth
import imgui.ImGui.inputFloat
import imgui.ImGui.isItemHovered
import imgui.ImGui.nextColumn
import imgui.ImGui.popID
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushID
import imgui.ImGui.pushStyleVar
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setNextItemWidth
import imgui.ImGui.setNextWindowContentSize
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textWrapped
import imgui.ImGui.treeNode
import imgui.ImGui.treePop
import imgui.api.demoDebugInformations.Companion.helpMarker
import imgui.classes.ListClipper
import imgui.dsl.child
import imgui.dsl.collapsingHeader
import imgui.dsl.selectable
import imgui.dsl.treeNode

object ShowDemoWindowColumns {

    /* Columns */
    var selected = -1
    var disableIndent = false


    /* Borders */
    var hBorders = true
    var vBorders = true
    var columnsCount = 4

    /* Mixed Items */
    var foo = 1f
    var bar = 1f


    operator fun invoke() {

        if (!collapsingHeader("Columns"))
            return

        pushID("Columns")

        checkbox("Disable tree indentation", ::disableIndent)
        sameLine()
        helpMarker("Disable the indenting of tree nodes so demo columns can use the full window width.")
        if (disableIndent)
            pushStyleVar(StyleVar.IndentSpacing, 0f)

        // Basic columns
        treeNode("Basic") {
            text("Without border:")
            columns(3, "mycolumns3", false)  // 3-ways, no border
            separator()
            for (n in 0..13) {
                selectable("Item $n")
                //if (ImGui::Button(label, ImVec2(-FLT_MIN,0.0f))) {}
                nextColumn()
            }
            columns(1)
            separator()

            text("With border:")
            columns(4, "mycolumns") // 4-ways, with border
            separator()
            text("ID"); nextColumn()
            text("Name"); nextColumn()
            text("Path"); nextColumn()
            text("Hovered"); nextColumn()
            separator()
            val names = listOf("One", "Two", "Three")
            val paths = listOf("/path/one", "/path/two", "/path/three")
            for (i in 0..2) {
                selectable("%04d".format(style.locale, i), selected == i, SelectableFlag.SpanAllColumns.i) {
                    selected = i
                }
                nextColumn()
                text(names[i]); nextColumn()
                text(paths[i]); nextColumn()
                text("${isItemHovered().i}"); nextColumn()
            }
            columns(1)
            separator()
        }

        if (treeNode("Borders")) {
            // NB: Future columns API should allow automatic horizontal borders.
            val linesCount = 3
            setNextItemWidth(fontSize * 8)
            dragInt("##columns_count", ::columnsCount, 0.1f, 2, 10, "%d columns")
            if (columnsCount < 2)
                columnsCount = 2
            sameLine()
            checkbox("horizontal", ::hBorders)
            sameLine()
            checkbox("vertical", ::vBorders)
            columns(columnsCount, "", vBorders)
            for (i in 0 until columnsCount * linesCount) {
                if (hBorders && columnIndex == 0)
                    separator()
                text("%c%c%c", 'a' + i, 'a' + i, 'a' + i)
                text("Width %.2f", getColumnWidth())
                text("Avail %.2f", contentRegionAvail.x)
                text("Offset %.2f", getColumnOffset())
                text("Long text that is likely to clip")
                button("Button", Vec2(-Float.MIN_VALUE, 0f))
                nextColumn()
            }
            columns(1)
            if (hBorders)
                separator()
            treePop()
        }

        // Create multiple items in a same cell before switching to next column
        treeNode("Mixed items") {
            columns(3, "mixed")
            separator()

            text("Hello")
            button("Banana")
            nextColumn()

            text("ImGui")
            button("Apple")
            inputFloat("red", ::foo, 0.05f, 0f, "%.3f")
            text("An extra line here.")
            nextColumn()

            text("Sailor")
            button("Corniflower")
            inputFloat("blue", ::bar, 0.05f, 0f, "%.3f")
            nextColumn()

            collapsingHeader("Category A") { text("Blah blah blah") }; nextColumn()
            collapsingHeader("Category B") { text("Blah blah blah") }; nextColumn()
            collapsingHeader("Category C") { text("Blah blah blah") }; nextColumn()
            columns(1)
            separator()
        }

        // Word wrapping
        treeNode("Word-wrapping") {
            columns(2, "word-wrapping")
            separator()
            textWrapped("The quick brown fox jumps over the lazy dog.")
            textWrapped("Hello Left")
            nextColumn()
            textWrapped("The quick brown fox jumps over the lazy dog.")
            textWrapped("Hello Right")
            columns(1)
            separator()
        }

        treeNode("Horizontal Scrolling") {
            setNextWindowContentSize(Vec2(1500f, 0f))
            val childSize = Vec2(0f, fontSize * 20f)
            child("##ScrollingRegion", childSize, false, WindowFlag.HorizontalScrollbar.i) {
                columns(10)
                val ITEMS_COUNT = 2000
                val clipper = ListClipper()  // Also demonstrate using the clipper for large list
                clipper.begin(ITEMS_COUNT)
                while (clipper.step())
                    for (i in clipper.displayStart until clipper.displayEnd)
                        for (j in 0..9) {
                            text("Line $i Column $j...")
                            nextColumn()
                        }
                columns(1)
            }
        }

        treeNode("Tree") {
            columns(2, "tree", true)
            for (x in 0..2) {
                val open1 = treeNode(x.L, "Node$x")
                nextColumn()
                text("Node contents")
                nextColumn()
                if (open1) {
                    for (y in 0..2) {
                        val open2 = treeNode(y.L, "Node$x.$y")
                        nextColumn()
                        text("Node contents")
                        if (open2) {
                            text("Even more contents")
                            treeNode("Tree in column") {
                                text("The quick brown fox jumps over the lazy dog")
                            }
                        }
                        nextColumn()
                        if (open2)
                            treePop()
                    }
                    treePop()
                }
            }
            columns(1)
        }

        if (disableIndent)
            popStyleVar()
        popID()
    }
}