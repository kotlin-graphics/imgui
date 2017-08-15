package imgui

import glm_.vec2.Vec2
import imgui.ImGui.begin
import imgui.ImGui.beginMenu
import imgui.ImGui.beginMenuBar
import imgui.ImGui.beginPopupModal
import imgui.ImGui.collapsingHeader
import imgui.ImGui.end
import imgui.ImGui.endMenu
import imgui.ImGui.endMenuBar
import imgui.ImGui.endPopup
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.ImGui.pushStyleVar
import imgui.ImGui.treeNode
import imgui.ImGui.treePop

object functionalProgramming {

    inline fun button(label: String, sizeArg: Vec2 = Vec2(), block: () -> Unit) {
        if (ImGui.buttonEx(label, sizeArg, 0))
            block()
    }

    inline fun withWindow(name: String, pOpen: BooleanArray? = null, flags: Int = 0, block: (Boolean) -> Unit) =
            withWindow(name, pOpen, Vec2(), -1.0f, flags, block)


    inline fun withWindow(name: String, pOpen: BooleanArray?, sizeOnFirstUse: Vec2, bgAlpha: Float = -1.0f, flags: Int = 0,
                   block: (Boolean) -> Unit) {

        block(begin(name, pOpen, sizeOnFirstUse, bgAlpha, flags))
        end()
    }

    inline fun window(name: String, pOpen: BooleanArray? = null, flags: Int = 0, block: () -> Unit) =
            window(name, pOpen, Vec2(), -1.0f, flags, block)


    inline fun window(name: String, pOpen: BooleanArray?, sizeOnFirstUse: Vec2, bgAlpha: Float = -1.0f, flags: Int = 0, block: () -> Unit) {
        if (begin(name, pOpen, sizeOnFirstUse, bgAlpha, flags)) {
            block()
            end()
        }
    }

    inline fun menuBar(block: () -> Unit) {
        if (beginMenuBar()) {
            block()
            endMenuBar()
        }
    }

    inline fun menu(label: String, enabled: Boolean = true, block: () -> Unit) {
        if (beginMenu(label, enabled)) {
            block()
            endMenu()
        }
    }

    inline fun collapsingHeader(label: String, flags: Int = 0, block: () -> Unit) {
        if (collapsingHeader(label, flags)) block()
    }

    inline fun treeNode(label: String, block: () -> Unit) {
        if (treeNode(label)) {
            block()
            treePop()
        }
    }

    inline fun popupModal(name: String, pOpen: BooleanArray? = null, extraFlags: Int = 0, block: () -> Unit) {
        if(beginPopupModal(name, pOpen, extraFlags)) {
            block()
            endPopup()
        }
    }

    inline fun withStyleVar(idx: StyleVar, value: Any, block: () -> Unit) {
        pushStyleVar(idx, value)
        block()
        popStyleVar()
    }

    inline fun withItemWidth(itemWidth: Float, block: () -> Unit) {
        pushItemWidth(itemWidth)
        block()
        popItemWidth()
    }

    inline fun withId(id: Int, block: () -> Unit) {
        pushId(id)
        block()
        popId()
    }
}