package imgui.demo

import glm_.L
import glm_.f
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import glm_.wo
import imgui.*
import imgui.ImGui.alignTextToFramePadding
import imgui.ImGui.beginTable
import imgui.ImGui.button
import imgui.ImGui.calcTextSize
import imgui.ImGui.checkbox
import imgui.ImGui.checkboxFlags
import imgui.ImGui.closeCurrentPopup
import imgui.ImGui.collapsingHeader
import imgui.ImGui.columnIndex
import imgui.ImGui.columns
import imgui.ImGui.combo
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.dragFloat
import imgui.ImGui.dragInt
import imgui.ImGui.dragVec2
import imgui.ImGui.endTable
import imgui.ImGui.fontSize
import imgui.ImGui.getColumnOffset
import imgui.ImGui.getColumnWidth
import imgui.ImGui.indent
import imgui.ImGui.inputFloat
import imgui.ImGui.inputText
import imgui.ImGui.io
import imgui.ImGui.isItemHovered
import imgui.ImGui.nextColumn
import imgui.ImGui.openPopup
import imgui.ImGui.popButtonRepeat
import imgui.ImGui.popID
import imgui.ImGui.popItemWidth
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushButtonRepeat
import imgui.ImGui.pushID
import imgui.ImGui.pushItemWidth
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.radioButton
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setNextItemOpen
import imgui.ImGui.setNextItemWidth
import imgui.ImGui.setNextWindowContentSize
import imgui.ImGui.sliderFloat
import imgui.ImGui.smallButton
import imgui.ImGui.spacing
import imgui.ImGui.style
import imgui.ImGui.tableGetColumnFlags
import imgui.ImGui.tableGetColumnIndex
import imgui.ImGui.tableGetColumnName
import imgui.ImGui.tableGetRowIndex
import imgui.ImGui.tableGetSortSpecs
import imgui.ImGui.tableHeader
import imgui.ImGui.tableHeadersRow
import imgui.ImGui.tableNextColumn
import imgui.ImGui.tableNextRow
import imgui.ImGui.tableSetBgColor
import imgui.ImGui.tableSetColumnIndex
import imgui.ImGui.tableSetupColumn
import imgui.ImGui.tableSetupScrollFreeze
import imgui.ImGui.text
import imgui.ImGui.textDisabled
import imgui.ImGui.textLineHeightWithSpacing
import imgui.ImGui.textUnformatted
import imgui.ImGui.textWrapped
import imgui.ImGui.treeNode
import imgui.ImGui.treeNodeEx
import imgui.ImGui.treePop
import imgui.ImGui.unindent
import imgui.api.demoDebugInformations.Companion.helpMarker
import imgui.classes.DrawList
import imgui.classes.ListClipper
import imgui.classes.TableSortSpecs
import imgui.dsl.child
import imgui.dsl.collapsingHeader
import imgui.dsl.popup
import imgui.dsl.popupContextItem
import imgui.dsl.selectable
import imgui.dsl.table
import imgui.dsl.treeNode
import imgui.dsl.treeNodeEx
import imgui.TableColumnFlag as Tcf
import imgui.TableFlag as Tf
import imgui.TableRowFlag as Trf
import imgui.TreeNodeFlag as Tnf

// Demonstrate old/legacy Columns API!
// [2020: Columns are under-featured and not maintained. Prefer using the more flexible and powerful BeginTable() API!]
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

        val open = treeNode("Legacy Columns API")
        sameLine()
        helpMarker("Columns() is an old API! Prefer using the more flexible and powerful BeginTable() API!")
        if (!open)
            return

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

                // Also demonstrate using clipper for large vertical lists
                val ITEMS_COUNT = 2000
                val clipper = ListClipper()
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

        treePop()
    }
}