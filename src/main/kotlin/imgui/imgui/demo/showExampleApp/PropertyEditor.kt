package imgui.imgui.demo.showExampleApp

import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.alignTextToFramePadding
import imgui.ImGui.begin_
import imgui.ImGui.columns
import imgui.ImGui.dragFloat
import imgui.ImGui.end
import imgui.ImGui.inputFloat
import imgui.ImGui.nextColumn
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.ImGui.pushStyleVar
import imgui.ImGui.separator
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.text
import imgui.ImGui.treeNode
import imgui.ImGui.treeNodeEx
import imgui.ImGui.treePop
import imgui.imgui.imgui_demoDebugInformations.Companion.showHelpMarker
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf

object PropertyEditor {

    /** Demonstrate create a simple property editor.    */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        setNextWindowSize(Vec2(430, 450), Cond.FirstUseEver)
        if (!begin_("Example: Property editor", open)) {
            end()
            return
        }

        showHelpMarker("This example shows how you may implement a property editor using two columns.\n" +
                "All objects/fields data are dummies here.\n" +
                "Remember that in many simple cases, you can use ImGui::SameLine(xxx) to position\n" +
                "your cursor horizontally instead of using the Columns() API.")

        pushStyleVar(StyleVar.FramePadding, Vec2(2))
        columns(2)
        separator()


        // Iterate dummy objects with dummy members (all the same data)
        for (objI in 0..2)
            showDummyObject("Object", objI)

        columns(1)
        separator()
        popStyleVar()
        end()
    }

    fun showDummyObject(prefix: String, uid: Int) {
        //  Use object uid as identifier. Most commonly you could also use the object pointer as a base ID.
        pushId(uid)
        /*  Text and Tree nodes are less high than regular widgets, here we add vertical spacing to make the tree
            lines equal high.             */
        alignTextToFramePadding()
        val nodeOpen = treeNode("Object", "${prefix}_$uid")
        nextColumn()
        alignTextToFramePadding()
        text("my sailor is rich")
        nextColumn()
        if (nodeOpen) {
            for (i in 0..7) {
                pushId(i) // Use field index as identifier.
                if (i < 2)
                    showDummyObject("Child", 424242)
                else {
                    // Here we use a TreeNode to highlight on hover (we could use e.g. Selectable as well)
                    alignTextToFramePadding()
                    treeNodeEx("Field", Tnf.Leaf or Tnf.NoTreePushOnOpen or Tnf.Bullet, "Field_$i")
                    nextColumn()
                    pushItemWidth(-1)
                    if (i >= 5)
                        inputFloat("##value", dummyMembers, i, 1f)
                    else
                        dragFloat("##value", dummyMembers, i, 0.01f)
                    popItemWidth()
                    nextColumn()
                }
                popId()
            }
            treePop()
        }
        popId()
    }

    val dummyMembers = floatArrayOf(0f, 0f, 1f, 3.1416f, 100f, 999f, 0f, 0f, 0f)
}