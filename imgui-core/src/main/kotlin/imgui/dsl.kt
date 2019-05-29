package imgui

import glm_.f
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import kotlin.reflect.KMutableProperty0

/** twin brother of dsl_ */
object dsl {

    // Windows

    inline fun window(name: String, open: KMutableProperty0<Boolean>? = null, flags: WindowFlags = 0, block: () -> Unit) {
        if (ImGui.begin(name, open, flags)) // ~open
            block()
        ImGui.end()
    }

    // Child Windows

    inline fun child(strId: String, size: Vec2 = Vec2(), border: Boolean = false, extraFlags: WindowFlags = 0, block: () -> Unit) {
        if (ImGui.beginChild(strId, size, border, extraFlags)) // ~open
            block()
        ImGui.endChild()
    }

    // Parameters stacks (shared)

    inline fun withFont(font: Font = ImGui.defaultFont, block: () -> Unit) {
        ImGui.pushFont(font)
        block()
        ImGui.popFont()
    }

    fun _push(idx: Col, col: Any) {
        if (col is Int)
            ImGui.pushStyleColor(idx, col)
        else
            ImGui.pushStyleColor(idx, col as Vec4)
    }

    inline fun withStyleColor(idx: Col, col: Any, block: () -> Unit) {
        _push(idx, col)
        block()
        ImGui.popStyleColor()
    }

    inline fun withStyleColor(idx0: Col, col0: Any,
                              idx1: Col, col1: Any, block: () -> Unit) {
        _push(idx0, col0)
        _push(idx1, col1)
        block()
        ImGui.popStyleColor(2)
    }

    inline fun withStyleColor(idx0: Col, col0: Any,
                              idx1: Col, col1: Any,
                              idx2: Col, col2: Any, block: () -> Unit) {
        _push(idx0, col0)
        _push(idx1, col1)
        _push(idx2, col2)
        block()
        ImGui.popStyleColor(3)
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
        ImGui.popStyleColor(4)
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
        ImGui.popStyleColor(5)
    }

    inline fun withStyleVar(idx: StyleVar, value: Any, block: () -> Unit) {
        ImGui.pushStyleVar(idx, value)
        block()
        ImGui.popStyleVar()
    }

    // Parameters stacks (current window)

    inline fun withItemWidth(itemWidth: Int, block: () -> Unit) = withItemWidth(itemWidth.f, block)
    inline fun withItemWidth(itemWidth: Float, block: () -> Unit) {
        ImGui.pushItemWidth(itemWidth)
        block()
        ImGui.popItemWidth()
    }

    inline fun withTextWrapPos(wrapPosX: Float = 0f, block: () -> Unit) {
        ImGui.pushTextWrapPos(wrapPosX)
        block()
        ImGui.popTextWrapPos()
    }

    inline fun withAllowKeyboardFocus(allowKeyboardFocus: Boolean, block: () -> Unit) {
        ImGui.pushAllowKeyboardFocus(allowKeyboardFocus)
        block()
        ImGui.popAllowKeyboardFocus()
    }

    inline fun withButtonRepeat(repeat: Boolean, block: () -> Unit) {
        ImGui.pushButtonRepeat(repeat)
        block()
        ImGui.popButtonRepeat()
    }


    // Cursor / Layout

    inline fun indent(indentW: Float = 0f, block: () -> Unit) {
        ImGui.indent(indentW)
        block()
        ImGui.unindent(indentW)
    }

    inline fun group(block: () -> Unit) {
        ImGui.beginGroup()
        block()
        ImGui.endGroup()
    }


    // ID stack/scopes

    inline fun withId(id: Int, block: () -> Unit) {
        ImGui.pushId(id)
        block()
        ImGui.popId()
    }

    inline fun withId(id: String, block: () -> Unit) {
        ImGui.pushId(id)
        block()
        ImGui.popId()
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
        if (ImGui.combo(label, currentItem, itemsSeparatedByZeros, heightInItems))
            block()
    }


    // Widgets: Trees

    inline fun treeNode(label: String, block: () -> Unit) {
        if (ImGui.treeNode(label)) {
            block()
            ImGui.treePop()
        }
    }

    inline fun treeNode(strId: String, fmt: String, block: () -> Unit) {
        if (ImGui.treeNode(strId, fmt)) {
            block()
            ImGui.treePop()
        }
    }

    inline fun treeNode(ptrId: Any, fmt: String, block: () -> Unit) {
        if (ImGui.treeNode(ptrId, fmt)) {
            block()
            ImGui.treePop()
        }
    }

    inline fun treePushed(ptrId: Any?, block: () -> Unit) {
        ImGui.treePush(ptrId)
        ImGui.treePop()
    }

    inline fun collapsingHeader(label: String, flags: TreeNodeFlags = 0, block: () -> Unit) {
        if (ImGui.collapsingHeader(label, flags))
            block()
    }

    inline fun collapsingHeader(label: String, open: KMutableProperty0<Boolean>, flags: TreeNodeFlags = 0, block: () -> Unit) {
        if (ImGui.collapsingHeader(label, open, flags))
            block()
    }


    // Widgets: Selectables

    inline fun selectable(label: String, selected: Boolean = false, flags: Int = 0, sizeArg: Vec2 = Vec2(), block: () -> Unit) {
        if (ImGui.selectable(label, selected, flags, sizeArg))
            block()
    }


    // Widgets: Menus

    inline fun mainMenuBar(block: () -> Unit) {
        if (ImGui.beginMainMenuBar()) {
            block()
            ImGui.endMainMenuBar()
        }
    }

    inline fun menuBar(block: () -> Unit) {
        if (ImGui.beginMenuBar()) {
            block()
            ImGui.endMenuBar()
        }
    }

    inline fun menu(label: String, enabled: Boolean = true, block: () -> Unit) {
        if (ImGui.beginMenu(label, enabled)) {
            block()
            ImGui.endMenu()
        }
    }

    inline fun menuItem(label: String, shortcut: String = "", selected: Boolean = false, enabled: Boolean = true, block: () -> Unit) {
        if (ImGui.menuItem(label, shortcut, selected, enabled))
            block()
    }


    // Tooltips

    inline fun tooltip(block: () -> Unit) {
        ImGui.beginTooltip()
        block()
        ImGui.endTooltip()
    }


    // Popups, Modals

    inline fun popup(strId: String, flags: WindowFlags = 0, block: () -> Unit) {
        if (ImGui.beginPopup(strId, flags)) {
            block()
            ImGui.endPopup()
        }
    }

    inline fun popupContextItem(strId: String = "", mouseButton: Int = 1, block: () -> Unit) {
        if (ImGui.beginPopupContextItem(strId, mouseButton)) {
            block()
            ImGui.endPopup()
        }
    }

    inline fun popupContextWindow(strId: String = "", mouseButton: Int = 1, alsoOverItems: Boolean = true, block: () -> Unit) {
        if (ImGui.beginPopupContextWindow(strId, mouseButton, alsoOverItems)) {
            block()
            ImGui.endPopup()
        }
    }

    inline fun popupContextVoid(strId: String = "", mouseButton: Int = 1, block: () -> Unit) {
        if (ImGui.beginPopupContextVoid(strId, mouseButton)) {
            block()
            ImGui.endPopup()
        }
    }

    inline fun popupModal(name: String, pOpen: KMutableProperty0<Boolean>? = null, extraFlags: WindowFlags = 0, block: () -> Unit) {
        if (ImGui.beginPopupModal(name, pOpen, extraFlags)) {
            block()
            ImGui.endPopup()
        }
    }


    // Tab Bars, Tabs

    inline fun tabBar(strId: String, flags: TabBarFlags = 0, block: () -> Unit) {
        if (ImGui.beginTabBar(strId, flags))
            block()
        ImGui.endTabBar()
    }

    inline fun tabItem(label: String, pOpen: KMutableProperty0<Boolean>? = null, flags: TabItemFlags = 0, block: () -> Unit) {
        if (ImGui.beginTabItem(label, pOpen, flags))
            block()
        ImGui.endTabItem()
    }


    // Drag and Drop

    inline fun dragDropSource(flags: DragDropFlags = 0, block: () -> Unit) {
        if(ImGui.beginDragDropSource(flags)) {
            block()
            ImGui.endDragDropSource()
        }
    }

    inline fun dragDropTarget(block: () -> Unit) {
        if(ImGui.beginDragDropTarget()) {
            block()
            ImGui.endDragDropTarget()
        }
    }


    // Clipping

    inline fun withClipRect(clipRectMin: Vec2, clipRectMax: Vec2, intersectWithCurrentClipRect: Boolean, block: () -> Unit) {
        ImGui.pushClipRect(clipRectMin, clipRectMax, intersectWithCurrentClipRect)
        block()
        ImGui.popClipRect()
    }


    // Miscellaneous Utilities

    inline fun childFrame(id: ID, size: Vec2, extraFlags: WindowFlags = 0, block: () -> Unit) {
        ImGui.beginChildFrame(id, size, extraFlags)
        block()
        ImGui.endChildFrame()
    }
}