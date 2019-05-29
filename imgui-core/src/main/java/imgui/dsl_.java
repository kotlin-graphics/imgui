package imgui;

import glm_.vec2.Vec2;
import glm_.vec4.Vec4;

/**
 * twin brother of dsl, manual overloads
 */
public class dsl_ {

    // Windows

    public static void window(String name, Runnable block) {
        window(name, null, 0, block);
    }

    public static void window(String name, MutableProperty0<Boolean> open, Runnable block) {
        window(name, open, 0, block);
    }

    public static void window(String name, MutableProperty0<Boolean> open, int windowFlags, Runnable block) {
        if (ImGui.INSTANCE.begin(name, open, windowFlags)) // ~open
            block.run();
        ImGui.INSTANCE.end();
    }

    // Child Windows

    public static void child(String strId, Runnable block) {
        child(strId, new Vec2(), false, 0, block);
    }

    public static void child(String strId, Vec2 size, Runnable block) {
        child(strId, size, false, 0, block);
    }

    public static void child(String strId, Vec2 size, boolean border, Runnable block) {
        child(strId, size, border, 0, block);
    }

    public static void child(String strId, Vec2 size, boolean border, int windowFlags, Runnable block) {
        if (ImGui.INSTANCE.beginChild(strId, size, border, windowFlags)) // ~open
            block.run();
        ImGui.INSTANCE.endChild();
    }

    // Parameters stacks (shared)

    public static void withFont(Runnable block) {
        withFont(ImGui.INSTANCE.getDefaultFont(), block);
    }

    public static void withFont(Font font, Runnable block) {
        ImGui.INSTANCE.pushFont(font);
        block.run();
        ImGui.INSTANCE.popFont();
    }

    private static void _push(Col idx, Object col) {
        if (col instanceof Integer)
            ImGui.INSTANCE.pushStyleColor(idx, (int) col);
        else
            ImGui.INSTANCE.pushStyleColor(idx, (Vec4) col);
    }

    public static void withStyleColor(Col idx, Object col, Runnable block) {
        _push(idx, col);
        block.run();
        ImGui.INSTANCE.popStyleColor(1);
    }

    public static void withStyleColor(Col idx0, Object col0,
                                      Col idx1, Object col1, Runnable block) {
        _push(idx0, col0);
        _push(idx1, col1);
        block.run();
        ImGui.INSTANCE.popStyleColor(2);
    }

    public static void withStyleColor(Col idx0, Object col0,
                                      Col idx1, Object col1,
                                      Col idx2, Object col2, Runnable block) {
        _push(idx0, col0);
        _push(idx1, col1);
        _push(idx2, col2);
        block.run();
        ImGui.INSTANCE.popStyleColor(3);
    }

    public static void withStyleColor(Col idx0, Object col0,
                                      Col idx1, Object col1,
                                      Col idx2, Object col2,
                                      Col idx3, Object col3, Runnable block) {
        _push(idx0, col0);
        _push(idx1, col1);
        _push(idx2, col2);
        _push(idx3, col3);
        block.run();
        ImGui.INSTANCE.popStyleColor(4);
    }

    public static void withStyleColor(Col idx0, Object col0,
                                      Col idx1, Object col1,
                                      Col idx2, Object col2,
                                      Col idx3, Object col3,
                                      Col idx4, Object col4, Runnable block) {
        _push(idx0, col0);
        _push(idx1, col1);
        _push(idx2, col2);
        _push(idx3, col3);
        _push(idx4, col4);
        block.run();
        ImGui.INSTANCE.popStyleColor(5);
    }

    public static void withStyleVar(StyleVar idx, Object value, Runnable block) {
        ImGui.INSTANCE.pushStyleVar(idx, value);
        block.run();
        ImGui.INSTANCE.popStyleVar(1);
    }

    // Parameters stacks (current window)

    public static void withItemWidth(int itemWidth, Runnable block) {
        withItemWidth((float) itemWidth, block);
    }

    public static void withItemWidth(float itemWidth, Runnable block) {
        ImGui.INSTANCE.pushItemWidth(itemWidth);
        block.run();
        ImGui.INSTANCE.popItemWidth();
    }

    public static void withTextWrapPos(Runnable block) {
        withTextWrapPos(0f, block);
    }

    public static void withTextWrapPos(float wrapPosX, Runnable block) {
        ImGui.INSTANCE.pushTextWrapPos(wrapPosX);
        block.run();
        ImGui.INSTANCE.popTextWrapPos();
    }

    public static void withAllowKeyboardFocus(boolean allowKeyboardFocus, Runnable block) {
        ImGui.INSTANCE.pushAllowKeyboardFocus(allowKeyboardFocus);
        block.run();
        ImGui.INSTANCE.popAllowKeyboardFocus();
    }

    public static void withButtonRepeat(boolean repeat, Runnable block) {
        ImGui.INSTANCE.pushButtonRepeat(repeat);
        block.run();
        ImGui.INSTANCE.popButtonRepeat();
    }


    // Cursor / Layout

    public static void indent(Runnable block) {
        indent(0f, block);
    }

    public static void indent(float indentW, Runnable block) {
        ImGui.INSTANCE.indent(indentW);
        block.run();
        ImGui.INSTANCE.unindent(indentW);
    }

    public static void group(Runnable block) {
        ImGui.INSTANCE.beginGroup();
        block.run();
        ImGui.INSTANCE.endGroup();
    }


    // ID stack/scopes

    public static void withId(int id, Runnable block) {
        ImGui.INSTANCE.pushId(id);
        block.run();
        ImGui.INSTANCE.popId();
    }

    public static void withId(String id, Runnable block) {
        ImGui.INSTANCE.pushId(id);
        block.run();
        ImGui.INSTANCE.popId();
    }


    // Widgets: Main

    public static void button(String label, Runnable block) {
        button(label, new Vec2(), block);
    }

    public static void button(String label, Vec2 sizeArg, Runnable block) {
        if (ImGui.INSTANCE.button(label, sizeArg))
            block.run();
    }

    public static void smallButton(String label, Runnable block) {
        if (ImGui.INSTANCE.smallButton(label))
            block.run();
    }

    public static void invisibleButton(String strId, Vec2 sizeArg, Runnable block) {
        if (ImGui.INSTANCE.invisibleButton(strId, sizeArg))
            block.run();
    }

    public static void arrowButton(String id, Dir dir, Runnable block) {
        if (ImGui.INSTANCE.arrowButton(id, dir))
            block.run();
    }

    public static void imageButton(int userTextureId, Vec2 size, Vec4 bgCol, Runnable block) {
        imageButton(userTextureId, size, new Vec2(), new Vec2(), -1, bgCol, new Vec4(1), block);
    }

    public static void imageButton(int userTextureId, Vec2 size, Vec2 uv0, Vec2 uv1, Vec4 bgCol, Runnable block) {
        imageButton(userTextureId, size, uv0, uv1, -1, bgCol, new Vec4(1), block);
    }

    public static void imageButton(int userTextureId, Vec2 size, Vec2 uv0, Vec2 uv1, int framePadding, Vec4 bgCol, Runnable block) {
        imageButton(userTextureId, size, uv0, uv1, framePadding, bgCol, new Vec4(1), block);
    }

    public static void imageButton(int userTextureId, Vec2 size, Vec2 uv0, Vec2 uv1, int framePadding, Vec4 bgCol, Vec4 tintCol, Runnable block) {
        if (ImGui.INSTANCE.imageButton(userTextureId, size, uv0, uv1, framePadding, bgCol, tintCol))
            block.run();
    }

    public static void checkbox(String label, MutableProperty0<Boolean> vPtr, Runnable block) {
        if (ImGui.INSTANCE.checkbox(label, vPtr))
            block.run();
    }

    public static void checkboxFlags(String label, MutableProperty0<Integer> vPtr, int flagsValue, Runnable block) {
        if (ImGui.INSTANCE.checkboxFlags(label, vPtr, flagsValue))
            block.run();
    }

    public static void radioButton(String label, boolean active, Runnable block) {
        if (ImGui.INSTANCE.radioButton(label, active))
            block.run();
    }

    public static void radioButton(String label, MutableProperty0<Integer> v, int vButton, Runnable block) {
        if (ImGui.INSTANCE.radioButton(label, v, vButton))
            block.run();
    }


    // Widgets: Combo Box


    public static void useCombo(String label, String previewValue, Runnable block) {
        useCombo(label, previewValue, 0, block);
    }

    public static void useCombo(String label, String previewValue, int comboFlags, Runnable block) {
        if (ImGui.INSTANCE.beginCombo(label, previewValue, comboFlags))
            block.run();
        ImGui.INSTANCE.endCombo();
    }

    public static void combo(String label, MutableProperty0<Integer> currentItem, String itemsSeparatedByZeros, Runnable block) {
        combo(label, currentItem, itemsSeparatedByZeros, -1, block);
    }

    public static void combo(String label, MutableProperty0<Integer> currentItem, String itemsSeparatedByZeros, int heightInItems, Runnable block) {
        if (ImGui.INSTANCE.combo(label, currentItem, itemsSeparatedByZeros, heightInItems))
            block.run();
    }


    // Widgets: Trees

    public static void treeNode(String label, Runnable block) {
        if (ImGui.INSTANCE.treeNode(label)) {
            block.run();
            ImGui.INSTANCE.treePop();
        }
    }

    public static void treeNode(String strId, String fmt, Runnable block) {
        if (ImGui.INSTANCE.treeNode(strId, fmt)) {
            block.run();
            ImGui.INSTANCE.treePop();
        }
    }

    public static void treeNode(Object ptrId, String fmt, Runnable block) {
        if (ImGui.INSTANCE.treeNode(ptrId, fmt)) {
            block.run();
            ImGui.INSTANCE.treePop();
        }
    }

    public static void treePushed(Object ptrId, Runnable block) {
        ImGui.INSTANCE.treePush(ptrId);
        ImGui.INSTANCE.treePop();
    }

    public static void collapsingHeader(String label, Runnable block) {
        collapsingHeader(label, 0, block);
    }

    public static void collapsingHeader(String label, int treeNodeFlags, Runnable block) {
        if (ImGui.INSTANCE.collapsingHeader(label, treeNodeFlags))
            block.run();
    }

    public static void collapsingHeader(String label, MutableProperty0<Boolean> open, Runnable block) {
        collapsingHeader(label, open, 0, block);
    }

    public static void collapsingHeader(String label, MutableProperty0<Boolean> open, int treeNodeFlags, Runnable block) {
        if (ImGui.INSTANCE.collapsingHeader(label, open, treeNodeFlags))
            block.run();
    }


    // Widgets: Selectables

    public static void selectable(String label, Runnable block) {
        selectable(label, false, 0, new Vec2(), block);
    }

    public static void selectable(String label, boolean selected, Runnable block) {
        selectable(label, selected, 0, new Vec2(), block);
    }

    public static void selectable(String label, boolean selected, int flags, Runnable block) {
        selectable(label, selected, flags, new Vec2(), block);
    }

    public static void selectable(String label, boolean selected, int flags, Vec2 sizeArg, Runnable block) {
        if (ImGui.INSTANCE.selectable(label, selected, flags, sizeArg))
            block.run();
    }


    // Widgets: Menus

    public static void mainMenuBar(Runnable block) {
        if (ImGui.INSTANCE.beginMainMenuBar()) {
            block.run();
            ImGui.INSTANCE.endMainMenuBar();
        }
    }

    public static void menuBar(Runnable block) {
        if (ImGui.INSTANCE.beginMenuBar()) {
            block.run();
            ImGui.INSTANCE.endMenuBar();
        }
    }

    public static void menu(String label, Runnable block) {
        menu(label, true, block);
    }

    public static void menu(String label, boolean enabled, Runnable block) {
        if (ImGui.INSTANCE.beginMenu(label, enabled)) {
            block.run();
            ImGui.INSTANCE.endMenu();
        }
    }

    public static void menuItem(String label, Runnable block) {
        menuItem(label, "", false, true, block);
    }

    public static void menuItem(String label, String shortcut, Runnable block) {
        menuItem(label, shortcut, false, true, block);
    }

    public static void menuItem(String label, String shortcut, boolean selected, Runnable block) {
        menuItem(label, shortcut, selected, true, block);
    }

    public static void menuItem(String label, String shortcut, boolean selected, boolean enabled, Runnable block) {
        if (ImGui.INSTANCE.menuItem(label, shortcut, selected, enabled))
            block.run();
    }


    // Tooltips

    public static void tooltip(Runnable block) {
        ImGui.INSTANCE.beginTooltip();
        block.run();
        ImGui.INSTANCE.endTooltip();
    }


    // Popups, Modals

    public static void popup(String strId, Runnable block) {
        popup(strId, 0, block);
    }

    public static void popup(String strId, int windowFlags, Runnable block) {
        if (ImGui.INSTANCE.beginPopup(strId, windowFlags)) {
            block.run();
            ImGui.INSTANCE.endPopup();
        }
    }

    public static void popupContextItem(String strId, Runnable block) {
        popupContextItem(strId, 1, block);
    }

    public static void popupContextItem(String strId, int mouseButton, Runnable block) {
        if (ImGui.INSTANCE.beginPopupContextItem(strId, mouseButton)) {
            block.run();
            ImGui.INSTANCE.endPopup();
        }
    }

    public static void popupContextWindow(String strId, Runnable block) {
        popupContextWindow(strId, 1, true, block);
    }

    public static void popupContextWindow(String strId, int mouseButton, Runnable block) {
        popupContextWindow(strId, mouseButton, true, block);
    }

    public static void popupContextWindow(String strId, int mouseButton, boolean alsoOverItems, Runnable block) {
        if (ImGui.INSTANCE.beginPopupContextWindow(strId, mouseButton, alsoOverItems)) {
            block.run();
            ImGui.INSTANCE.endPopup();
        }
    }

    public static void popupContextVoid(String strId, Runnable block) {
        popupContextVoid(strId, 1, block);
    }

    public static void popupContextVoid(String strId, int mouseButton, Runnable block) {
        if (ImGui.INSTANCE.beginPopupContextVoid(strId, mouseButton)) {
            block.run();
            ImGui.INSTANCE.endPopup();
        }
    }

    public static void popupModal(String name, Runnable block) {
        popupModal(name, null, 0, block);
    }

    public static void popupModal(String name, MutableProperty0<Boolean> pOpen, Runnable block) {
        popupModal(name, pOpen, 0, block);
    }

    public static void popupModal(String name, MutableProperty0<Boolean> pOpen, int windowFlags, Runnable block) {
        if (ImGui.INSTANCE.beginPopupModal(name, pOpen, windowFlags)) {
            block.run();
            ImGui.INSTANCE.endPopup();
        }
    }


    // Tab Bars, Tabs

    public static void tabBar(String strId, Runnable block) {
        tabBar(strId, 0, block);
    }

    public static void tabBar(String strId, int tabBarFlags, Runnable block) {
        if (ImGui.INSTANCE.beginTabBar(strId, tabBarFlags))
            block.run();
        ImGui.INSTANCE.endTabBar();
    }

    public static void tabItem(String label, Runnable block) {
        tabItem(label, null, 0, block);
    }

    public static void tabItem(String label, MutableProperty0<Boolean> pOpen, Runnable block) {
        tabItem(label, pOpen, 0, block);
    }

    public static void tabItem(String label, MutableProperty0<Boolean> pOpen, int tabItemFlags, Runnable block) {
        if (ImGui.INSTANCE.beginTabItem(label, pOpen, tabItemFlags))
            block.run();
        ImGui.INSTANCE.endTabItem();
    }


    // Drag and Drop

    public static void dragDropSource(Runnable block) {
        dragDropSource(0, block);
    }

    public static void dragDropSource(int dragDropFlags, Runnable block) {
        if (ImGui.INSTANCE.beginDragDropSource(dragDropFlags)) {
            block.run();
            ImGui.INSTANCE.endDragDropSource();
        }
    }

    public static void dragDropTarget(Runnable block) {
        if (ImGui.INSTANCE.beginDragDropTarget()) {
            block.run();
            ImGui.INSTANCE.endDragDropTarget();
        }
    }


    // Clipping

    public static void withClipRect(Vec2 clipRectMin, Vec2 clipRectMax, boolean intersectWithCurrentClipRect, Runnable block) {
        ImGui.INSTANCE.pushClipRect(clipRectMin, clipRectMax, intersectWithCurrentClipRect);
        block.run();
        ImGui.INSTANCE.popClipRect();
    }


    // Miscellaneous Utilities

    public static void childFrame(int id, Vec2 size, Runnable block) {
        childFrame(id, size, 0, block);
    }

    public static void childFrame(int id, Vec2 size, int windowFlags, Runnable block) {
        ImGui.INSTANCE.beginChildFrame(id, size, windowFlags);
        block.run();
        ImGui.INSTANCE.endChildFrame();
    }
}
