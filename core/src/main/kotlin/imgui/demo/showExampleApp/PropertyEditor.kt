package imgui.demo.showExampleApp

import glm_.vec2.Vec2
import imgui.Cond
import imgui.ImGui.alignTextToFramePadding
import imgui.ImGui.begin
import imgui.ImGui.drag
import imgui.ImGui.end
import imgui.ImGui.input
import imgui.ImGui.nextColumn
import imgui.ImGui.popID
import imgui.ImGui.popItemWidth
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushID
import imgui.ImGui.pushItemWidth
import imgui.ImGui.pushStyleVar
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.tableNextRow
import imgui.ImGui.tableSetColumnIndex
import imgui.ImGui.text
import imgui.ImGui.treeNode
import imgui.ImGui.treeNodeEx
import imgui.ImGui.treePop
import imgui.StyleVar
import imgui.TableFlag
import imgui.api.demoDebugInformations.Companion.helpMarker
import imgui.dsl.table
import imgui.or
import kotlin.reflect.KMutableProperty0
import imgui.TreeNodeFlag as Tnf

object PropertyEditor {

    /** Demonstrate create a simple property editor.    */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        setNextWindowSize(Vec2(430, 450), Cond.FirstUseEver)
        if (!begin("Example: Property editor", open)) {
            end()
            return
        }

        helpMarker("""
                This example shows how you may implement a property editor using two columns.
                All objects/fields data are dummies here.
                Remember that in many simple cases, you can use ImGui::SameLine(xxx) to position
                your cursor horizontally instead of using the Columns() API.""".trimIndent())

        pushStyleVar(StyleVar.FramePadding, Vec2(2))
        table("split", 2, TableFlag.BordersOuter or TableFlag.Resizable) {
            // Iterate placeholder objects (all the same data)
            for (objI in 0..3) {
                showPlaceholderObject("Object", objI)
                //ImGui::Separator();
            }
        }
        popStyleVar()
        end()
    }

    fun showPlaceholderObject(prefix: String, uid: Int) {
        //  Use object uid as identifier. Most commonly you could also use the object pointer as a base ID.
        pushID(uid)

        // Text and Tree nodes are less high than framed widgets, using AlignTextToFramePadding() we add vertical spacing to make the tree lines equal high.
        tableNextRow()
        tableSetColumnIndex(0)
        alignTextToFramePadding()
        val nodeOpen = treeNode("Object", "${prefix}_$uid")
        tableSetColumnIndex(1)
        text("my sailor is rich")

        if (nodeOpen) {
            for (i in 0..7) {
                pushID(i) // Use field index as identifier.
                if (i < 2)
                    showPlaceholderObject("Child", 424242)
                else {
                    // Here we use a TreeNode to highlight on hover (we could use e.g. Selectable as well)
                    tableNextRow()
                    tableSetColumnIndex(0)
                    alignTextToFramePadding()
                    val flags = Tnf.Leaf or Tnf.NoTreePushOnOpen or Tnf.Bullet
                    treeNodeEx("Field", flags, "Field_$i")

                    tableSetColumnIndex(1)
                    pushItemWidth(-Float.MIN_VALUE)
                    if (i >= 5)
                        input("##value", placeholderMembers, i, 1f)
                    else
                        drag("##value", placeholderMembers, i, 0.01f)
                    popItemWidth()
                    nextColumn()
                }
                popID()
            }
            treePop()
        }
        popID()
    }

    val placeholderMembers = floatArrayOf(0f, 0f, 1f, 3.1416f, 100f, 999f)
}