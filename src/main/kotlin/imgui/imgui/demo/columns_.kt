package imgui.imgui.demo

import glm_.vec2.Vec2
import imgui.ImGui.bulletText
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.columnIndex
import imgui.ImGui.columns
import imgui.ImGui.fontSize
import imgui.ImGui.getColumnOffset
import imgui.ImGui.getColumnWidth
import imgui.ImGui.inputFloat
import imgui.ImGui.isItemHovered
import imgui.ImGui.nextColumn
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setNextWindowContentSize
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textWrapped
import imgui.ImGui.treeNode
import imgui.ImGui.treePop
import imgui.ListClipper
import imgui.SelectableFlag
import imgui.WindowFlag
import imgui.functionalProgramming.collapsingHeader
import imgui.functionalProgramming.selectable
import imgui.functionalProgramming.treeNode
import imgui.functionalProgramming.withChild
import imgui.functionalProgramming.withId
import imgui.imgui.imgui_demoDebugInformations.Companion.showHelpMarker

object columns_ {

    /* Columns */
    var selected = -1


    /* Mixed Items */
    var foo = 1f
    var bar = 1f


    /* Borders */
    var hBorders = true
    var vBorders = true


    operator fun invoke() {

        collapsingHeader("Columns") {

            withId("Columns") {

                // Basic columns
                treeNode("Basic") {
                    text("Without border:")
                    columns(3, "mycolumns3", false)  // 3-ways, no border
                    separator()
                    for (n in 0..13) {
                        selectable("Item $n")
                        //if (Button(label, ImVec2(-1,0))) {}
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
                        text("${isItemHovered()}"); nextColumn()
                    }
                    columns(1)
                    separator()
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
                    inputFloat("red", ::foo, 0.05f, 0f, 3)
                    text("An extra line here.")
                    nextColumn()

                    text("Sailor")
                    button("Corniflower")
                    inputFloat("blue", ::bar, 0.05f, 0f, 3)
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

                treeNode("Borders") {
                    // NB: Future columns API should allow automatic horizontal borders.
                    checkbox("horizontal", ::hBorders)
                    sameLine()
                    checkbox("vertical", ::vBorders)
                    columns(4, "", vBorders)
                    for (i in 0 until 4 * 3) {
                        if (hBorders && columnIndex == 0) separator()
                        text("%c%c%c", 'a' + i, 'a' + i, 'a' + i)
                        text("Width %.2f\nOffset %.2f", getColumnWidth(), getColumnOffset())
                        nextColumn()
                    }
                    columns(1)
                    if (hBorders) separator()
                }

                treeNode("Horizontal Scrolling") {
                    setNextWindowContentSize(Vec2(1500f, 0f))
                    withChild("##Scrollingregion", Vec2(0, fontSize * 20), false, WindowFlag.HorizontalScrollbar.i) {
                        columns(10)
                        val ITEMS_COUNT = 2000
                        val clipper = ListClipper(ITEMS_COUNT)  // Also demonstrate using the clipper for large list
                        while (clipper.step()) {
                            val i = clipper.display.start
                            while (i < clipper.display.endInclusive)
                                for (j in 0..9) {
                                    text("Line $i Column $j...")
                                    nextColumn()
                                }
                        }
                        columns(1)
                    }
                }

                val nodeOpen = treeNode("Tree within single cell")
                sameLine(); showHelpMarker("NB: Tree node must be poped before ending the cell. There's no storage of state per-cell.")
                if (nodeOpen) {
                    columns(2, "tree items")
                    separator()
                    treeNode("Hello") { bulletText("Sailor") }; nextColumn()
                    treeNode("Bonjour") { bulletText("Marin") }; nextColumn()
                    columns(1)
                    separator()
                    treePop()
                }
            }
        }
    }
}