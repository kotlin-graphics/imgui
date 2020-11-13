package imgui.demo.showExampleApp

import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.alignTextToFramePadding
import imgui.ImGui.begin
import imgui.ImGui.columns
import imgui.ImGui.dragFloat
import imgui.ImGui.end
import imgui.ImGui.inputFloat
import imgui.ImGui.nextColumn
import imgui.ImGui.popID
import imgui.ImGui.popItemWidth
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushID
import imgui.ImGui.pushItemWidth
import imgui.ImGui.pushStyleVar
import imgui.ImGui.separator
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.text
import imgui.ImGui.treeNode
import imgui.ImGui.treeNodeEx
import imgui.ImGui.treePop
import imgui.api.demoDebugInformations.Companion.helpMarker
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

        helpMarker(
                """
                This example shows how you may implement a property editor using two columns.
                All objects/fields data are dummies here.
                Remember that in many simple cases, you can use ImGui::SameLine(xxx) to position
                your cursor horizontally instead of using the Columns() API.""".trimIndent())

        pushStyleVar(StyleVar.FramePadding, Vec2(2))
        columns(2)
        separator()

        // Iterate placeholder objects (all the same data)
        for (objI in 0..2)
            showPlaceholderObject("Object", objI)

        columns(1)
        separator()
        popStyleVar()
        end()
    }

    fun showPlaceholderObject(prefix: String, uid: Int) {
        //  Use object uid as identifier. Most commonly you could also use the object pointer as a base ID.
        pushID(uid)

        // Text and Tree nodes are less high than framed widgets, using AlignTextToFramePadding() we add vertical spacing to make the tree lines equal high.
        alignTextToFramePadding()
        val nodeOpen = treeNode("Object", "${prefix}_$uid")
        nextColumn()
        alignTextToFramePadding()
        text("my sailor is rich")
        nextColumn()
        if (nodeOpen) {
            for (i in 0..7) {
                pushID(i) // Use field index as identifier.
                if (i < 2)
                    showPlaceholderObject("Child", 424242)
                else {
                    // Here we use a TreeNode to highlight on hover (we could use e.g. Selectable as well)
                    alignTextToFramePadding()
                    val flags = Tnf.Leaf or Tnf.NoTreePushOnOpen or Tnf.Bullet
                    treeNodeEx("Field", flags, "Field_$i")
                    nextColumn()
                    pushItemWidth(-1)
                    if (i >= 5)
                        inputFloat("##value", placeholderMembers, i, 1f)
                    else
                        dragFloat("##value", placeholderMembers, i, 0.01f)
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