package imgui

import glm_.f
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.ImGui.begin
import imgui.ImGui.beginChild
import imgui.ImGui.beginGroup
import imgui.ImGui.beginMainMenuBar
import imgui.ImGui.beginMenu
import imgui.ImGui.beginMenuBar
import imgui.ImGui.beginPopup
import imgui.ImGui.beginPopupContextItem
import imgui.ImGui.beginPopupContextWindow
import imgui.ImGui.beginPopupModal
import imgui.ImGui.beginTooltip
import imgui.ImGui.collapsingHeader
import imgui.ImGui.combo
import imgui.ImGui.defaultFont
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endGroup
import imgui.ImGui.endMainMenuBar
import imgui.ImGui.endMenu
import imgui.ImGui.endMenuBar
import imgui.ImGui.endPopup
import imgui.ImGui.endTooltip
import imgui.ImGui.indent
import imgui.ImGui.menuItem
import imgui.ImGui.popAllowKeyboardFocus
import imgui.ImGui.popButtonRepeat
import imgui.ImGui.popFont
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.popTextWrapPos
import imgui.ImGui.pushAllowKeyboardFocus
import imgui.ImGui.pushButtonRepeat
import imgui.ImGui.pushFont
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.pushTextWrapPos
import imgui.ImGui.selectable
import imgui.ImGui.treeNode
import imgui.ImGui.treePop
import imgui.ImGui.unindent
import kotlin.reflect.KMutableProperty0

object dsl {

    // Windows

    inline fun window(name: String, open: KMutableProperty0<Boolean>? = null, flags: WindowFlags = 0, block: () -> Unit) {
        if (begin(name, open, flags)) // ~open
            block()
        end()
    }

    // Child Windows

    inline fun child(strId: String, size: Vec2 = Vec2(), border: Boolean = false, extraFlags: WindowFlags = 0, block: () -> Unit) {
        if (beginChild(strId, size, border, extraFlags)) // ~open
            block()
        endChild()
    }

    // Parameters stacks (shared)

    inline fun withFont(font: Font = defaultFont, block: () -> Unit) {
        pushFont(font)
        block()
        popFont()
    }

    fun _push(idx: Col, col: Any) {
        if (col is Int)
            pushStyleColor(idx, col)
        else
            pushStyleColor(idx, col as Vec4)
    }

    inline fun withStyleColor(idx: Col, col: Any, block: () -> Unit) {
        _push(idx, col)
        block()
        popStyleColor()
    }

    inline fun withStyleColor(idx0: Col, col0: Any,
                              idx1: Col, col1: Any, block: () -> Unit) {
        _push(idx0, col0)
        _push(idx1, col1)
        block()
        popStyleColor(2)
    }

    inline fun withStyleColor(idx0: Col, col0: Any,
                              idx1: Col, col1: Any,
                              idx2: Col, col2: Any, block: () -> Unit) {
        _push(idx0, col0)
        _push(idx1, col1)
        _push(idx2, col2)
        block()
        popStyleColor(3)
    }

    inline fun withStyleColor(idx0: Col, col0: Any,
                              idx1: Col, col1: Any,
                              idx2: Col, col2: Any,
                              idx3: Col, col3: Any,
                              block: () -> Unit) {
        _push(idx0, col0)
        _push(idx1, col1)
        _push(idx2, col2)
        _push(idx3, col3)
        block()
        popStyleColor(4)
    }

    inline fun withStyleColor(idx0: Col, col0: Any,
                              idx1: Col, col1: Any,
                              idx2: Col, col2: Any,
                              idx3: Col, col3: Any,
                              idx4: Col, col4: Any, block: () -> Unit) {
        _push(idx0, col0)
        _push(idx1, col1)
        _push(idx2, col2)
        _push(idx3, col3)
        _push(idx4, col4)
        block()
        popStyleColor(5)
    }

    inline fun withStyleVar(idx: StyleVar, value: Any, block: () -> Unit) {
        pushStyleVar(idx, value)
        block()
        popStyleVar()
    }

    // Parameters stacks (current window)

    inline fun withItemWidth(itemWidth: Int, block: () -> Unit) = withItemWidth(itemWidth.f, block)
    inline fun withItemWidth(itemWidth: Float, block: () -> Unit) {
        pushItemWidth(itemWidth)
        block()
        popItemWidth()
    }

    inline fun withTextWrapPos(wrapPosX: Float = 0f, block: () -> Unit) {
        pushTextWrapPos(wrapPosX)
        block()
        popTextWrapPos()
    }

    inline fun withPushAllowKeyboardFocus(allowKeyboardFocus: Boolean, block: () -> Unit) {
        pushAllowKeyboardFocus(allowKeyboardFocus)
        block()
        popAllowKeyboardFocus()
    }

    inline fun withPushButtonRepeat(repeat: Boolean, block: () -> Unit) {
        pushButtonRepeat(repeat)
        block()
        popButtonRepeat()
    }


    // Cursor / Layout

    inline fun withIndent(indentW: Float = 0f, block: () -> Unit) {
        indent(indentW)
        block()
        unindent(indentW)
    }

    inline fun withGroup(block: () -> Unit) {
        beginGroup()
        block()
        endGroup()
    }


    // ID stack/scopes

    inline fun withId(id: Int, block: () -> Unit) {
        pushId(id)
        block()
        popId()
    }

    inline fun withId(id: String, block: () -> Unit) {
        pushId(id)
        block()
        popId()
    }


    // Widgets: Main

    inline fun button(label: String, sizeArg: Vec2 = Vec2(), block: () -> Unit) {
        if (ImGui.button(label, sizeArg))
            block()
    }

    inline fun smallButton(label: String, block: () -> Unit) {
        if (ImGui.smallButton(label))
            block()
    }

    inline fun invisibleButton(strId: String, sizeArg: Vec2, block: () -> Unit) {
        if (ImGui.invisibleButton(strId, sizeArg))
            block()
    }

    inline fun arrowButton(id: String, dir: Dir, block: () -> Unit) {
        if (ImGui.arrowButton(id, dir))
            block()
    }

    inline fun imageButton(userTextureId: TextureID, size: Vec2, uv0: Vec2 = Vec2(), uv1: Vec2 = Vec2(),
                           framePadding: Int = -1, bgCol: Vec4 = Vec4(), tintCol: Vec4 = Vec4(1), block: () -> Unit) {
        if (ImGui.imageButton(userTextureId, size, uv0, uv1, framePadding, bgCol, tintCol))
            block()
    }

    inline fun checkbox(label: String, vPtr: KMutableProperty0<Boolean>, block: () -> Unit) {
        if (ImGui.checkbox(label, vPtr))
            block()
    }

    inline fun checkboxFlags(label: String, vPtr: KMutableProperty0<Int>, flagsValue: Int, block: () -> Unit) {
        if (ImGui.checkboxFlags(label, vPtr, flagsValue))
            block()
    }

    inline fun radioButton(label: String, active: Boolean, block: () -> Unit) {
        if (ImGui.radioButton(label, active))
            block()
    }

    inline fun radioButton(label: String, v: KMutableProperty0<Int>, vButton: Int, block: () -> Unit) {
        if (ImGui.radioButton(label, v, vButton))
            block()
    }


    // Widgets: Combo Box


    inline fun useCombo(label: String, previewValue: String?, flags: ComboFlags = 0, block: () -> Unit) {
        if (ImGui.beginCombo(label, previewValue, flags))
            block()
        ImGui.endCombo()
    }

    inline fun combo(label: String, currentItem: KMutableProperty0<Int>, itemsSeparatedByZeros: String, heightInItems: Int = -1,
                     block: () -> Unit) {
        if (combo(label, currentItem, itemsSeparatedByZeros, heightInItems))
            block()
    }


    // Widgets: Trees

    inline fun treeNode(label: String, block: () -> Unit) {
        if (treeNode(label)) {
            block()
            treePop()
        }
    }

    inline fun treeNode(strId: String, fmt: String, block: () -> Unit) {
        if (treeNode(strId, fmt)) {
            block()
            treePop()
        }
    }

    inline fun treeNode(ptrId: Any, fmt: String, block: () -> Unit) {
        if (treeNode(ptrId, fmt)) {
            block()
            treePop()
        }
    }

    inline fun treePushed(ptrId: Any?, block: () -> Unit) {
        ImGui.treePush(ptrId)
        ImGui.treePop()
    }

    inline fun collapsingHeader(label: String, flags: TreeNodeFlags = 0, block: () -> Unit) {
        if (collapsingHeader(label, flags)) block()
    }

    inline fun collapsingHeader(label: String, open: KMutableProperty0<Boolean>, flags: TreeNodeFlags = 0, block: () -> Unit) {
        if (collapsingHeader(label, open, flags))
            block()
    }


    // Widgets: Selectables

    inline fun selectable(label: String, selected: Boolean = false, flags: Int = 0, sizeArg: Vec2 = Vec2(), block: () -> Unit) {
        if (selectable(label, selected, flags, sizeArg)) block()
    }


    // Widgets: Menus

    inline fun mainMenuBar(block: () -> Unit) {
        if (beginMainMenuBar()) {
            block()
            endMainMenuBar()
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

    inline fun menuItem(label: String, shortcut: String = "", selected: Boolean = false, enabled: Boolean = true, block: () -> Unit) {
        if (menuItem(label, shortcut, selected, enabled)) block()
    }


    inline fun popupModal(name: String, pOpen: KMutableProperty0<Boolean>? = null, extraFlags: WindowFlags = 0, block: () -> Unit) {
        if (beginPopupModal(name, pOpen, extraFlags)) {
            block()
            endPopup()
        }
    }


    inline fun withTooltip(block: () -> Unit) {
        beginTooltip()
        block()
        endTooltip()
    }


    inline fun popupContextWindow(block: () -> Unit) {
        if (beginPopupContextWindow()) {
            block()
            endPopup()
        }
    }


    inline fun popup(strId: String, block: () -> Unit) {
        if (beginPopup(strId)) {
            block()
            endPopup()
        }
    }

    inline fun popupContextItem(strId: String = "", block: () -> Unit) {
        if (beginPopupContextItem(strId)) {
            block()
            endPopup()
        }
    }


}