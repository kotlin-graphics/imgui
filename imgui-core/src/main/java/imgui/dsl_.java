package imgui;

import glm_.vec2.Vec2;
import glm_.vec4.Vec4;

/**
 * twin brother of dsl, manual overloads
 */
public class dsl_ {

    // Windows
    private static ImGui imgui = ImGui.INSTANCE;

    public static void window(String name, Runnable block) {
        window(name, null, 0, block);
    }

    public static void window(String name, MutableProperty0<Boolean> open, Runnable block) {
        window(name, open, 0, block);
    }

    public static void window(String name, MutableProperty0<Boolean> open, int windowFlags, Runnable block) {
        if (imgui.begin(name, open, windowFlags)) // ~open
            block.run();
        imgui.end();
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
        if (imgui.beginChild(strId, size, border, windowFlags)) // ~open
            block.run();
        imgui.endChild();
    }

    // Parameters stacks (shared)

    public static void withFont(Runnable block) {
        withFont(imgui.getDefaultFont(), block);
    }

    public static void withFont(Font font, Runnable block) {
        imgui.pushFont(font);
        block.run();
        imgui.popFont();
    }

    private static void _push(Col idx, Object col) {
        if (col instanceof Integer)
            imgui.pushStyleColor(idx, (int) col);
        else
            imgui.pushStyleColor(idx, (Vec4) col);
    }

    public static void withStyleColor(Col idx, Object col, Runnable block) {
        _push(idx, col);
        block.run();
        imgui.popStyleColor(1);
    }

    public static void withStyleColor(Col idx0, Object col0,
                                      Col idx1, Object col1, Runnable block) {
        _push(idx0, col0);
        _push(idx1, col1);
        block.run();
        imgui.popStyleColor(2);
    }

    public static void withStyleColor(Col idx0, Object col0,
                                      Col idx1, Object col1,
                                      Col idx2, Object col2, Runnable block) {
        _push(idx0, col0);
        _push(idx1, col1);
        _push(idx2, col2);
        block.run();
        imgui.popStyleColor(3);
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
        imgui.popStyleColor(4);
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
        imgui.popStyleColor(5);
    }

    public static void withStyleVar(StyleVar idx, Object value, Runnable block) {
        imgui.pushStyleVar(idx, value);
        block.run();
        imgui.popStyleVar(1);
    }

    // Parameters stacks (current window)

    public static void withItemWidth(int itemWidth, Runnable block) {
        withItemWidth((float) itemWidth, block);
    }

    public static void withItemWidth(float itemWidth, Runnable block) {
        imgui.pushItemWidth(itemWidth);
        block.run();
        imgui.popItemWidth();
    }

    public static void withTextWrapPos(Runnable block) {
        withTextWrapPos(0f, block);
    }

    public static void withTextWrapPos(float wrapPosX, Runnable block) {
        imgui.pushTextWrapPos(wrapPosX);
        block.run();
        imgui.popTextWrapPos();
    }

    public static void withAllowKeyboardFocus(boolean allowKeyboardFocus, Runnable block) {
        imgui.pushAllowKeyboardFocus(allowKeyboardFocus);
        block.run();
        imgui.popAllowKeyboardFocus();
    }

    public static void withButtonRepeat(boolean repeat, Runnable block) {
        imgui.pushButtonRepeat(repeat);
        block.run();
        imgui.popButtonRepeat();
    }


    // Cursor / Layout

    public static void indent(Runnable block) {
        indent(0f, block);
    }

    public static void indent(float indentW, Runnable block) {
        imgui.indent(indentW);
        block.run();
        imgui.unindent(indentW);
    }

    public static void group(Runnable block) {
        imgui.beginGroup();
        block.run();
        imgui.endGroup();
    }


    // ID stack/scopes

    public static void withId(int id, Runnable block) {
        imgui.pushId(id);
        block.run();
        imgui.popId();
    }

    public static void withId(String id, Runnable block) {
        imgui.pushId(id);
        block.run();
        imgui.popId();
    }


    // Widgets: Main

    public static void button(String label, Runnable block) {
        button(label, new Vec2(), block);
    }

    public static void button(String label, Vec2 sizeArg, Runnable block) {
        if (imgui.button(label, sizeArg))
            block.run();
    }

    public static void smallButton(String label, Runnable block) {
        if (imgui.smallButton(label))
            block.run();
    }

    public static void invisibleButton(String strId, Vec2 sizeArg, Runnable block) {
        if (imgui.invisibleButton(strId, sizeArg))
            block.run();
    }

    public static void arrowButton(String id, Dir dir, Runnable block) {
        if (imgui.arrowButton(id, dir))
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
        if (imgui.imageButton(userTextureId, size, uv0, uv1, framePadding, bgCol, tintCol))
            block.run();
    }

    public static void checkbox(String label, MutableProperty0<Boolean> vPtr, Runnable block) {
        if (imgui.checkbox(label, vPtr))
            block.run();
    }

    public static void checkboxFlags(String label, MutableProperty0<Integer> vPtr, int flagsValue, Runnable block) {
        if (imgui.checkboxFlags(label, vPtr, flagsValue))
            block.run();
    }

    public static void radioButton(String label, boolean active, Runnable block) {
        if (imgui.radioButton(label, active))
            block.run();
    }

    public static void radioButton(String label, MutableProperty0<Integer> v, int vButton, Runnable block) {
        if (imgui.radioButton(label, v, vButton))
            block.run();
    }


    // Widgets: Combo Box


    public static void useCombo(String label, String previewValue, Runnable block) {
        useCombo(label, previewValue, 0, block);
    }

    public static void useCombo(String label, String previewValue, int comboFlags, Runnable block) {
        if (imgui.beginCombo(label, previewValue, comboFlags))
            block.run();
        imgui.endCombo();
    }

    public static void combo(String label, MutableProperty0<Integer> currentItem, String itemsSeparatedByZeros, Runnable block) {
        combo(label, currentItem, itemsSeparatedByZeros, -1, block);
    }

    public static void combo(String label, MutableProperty0<Integer> currentItem, String itemsSeparatedByZeros, int heightInItems, Runnable block) {
        if (imgui.combo(label, currentItem, itemsSeparatedByZeros, heightInItems))
            block.run();
    }


    // Widgets: Trees

    public static void treeNode(String label, Runnable block) {
        if (imgui.treeNode(label)) {
            block.run();
            imgui.treePop();
        }
    }

    public static void treeNode(String strId, String fmt, Runnable block) {
        if (imgui.treeNode(strId, fmt)) {
            block.run();
            imgui.treePop();
        }
    }

    public static void treeNode(Object ptrId, String fmt, Runnable block) {
        if (imgui.treeNode(ptrId, fmt)) {
            block.run();
            imgui.treePop();
        }
    }

    public static void treePushed(Object ptrId, Runnable block) {
        imgui.treePush(ptrId);
        imgui.treePop();
    }

    public static void collapsingHeader(String label, Runnable block) {
        collapsingHeader(label, 0, block);
    }

    public static void collapsingHeader(String label, int treeNodeFlags, Runnable block) {
        if (imgui.collapsingHeader(label, treeNodeFlags))
            block.run();
    }

    public static void collapsingHeader(String label, MutableProperty0<Boolean> open, Runnable block) {
        collapsingHeader(label, open, 0, block);
    }

    public static void collapsingHeader(String label, MutableProperty0<Boolean> open, int treeNodeFlags, Runnable block) {
        if (imgui.collapsingHeader(label, open, treeNodeFlags))
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
        if (imgui.selectable(label, selected, flags, sizeArg))
            block.run();
    }


    // Widgets: Menus

    public static void mainMenuBar(Runnable block) {
        if (imgui.beginMainMenuBar()) {
            block.run();
            imgui.endMainMenuBar();
        }
    }

    public static void menuBar(Runnable block) {
        if (imgui.beginMenuBar()) {
            block.run();
            imgui.endMenuBar();
        }
    }

    public static void menu(String label, Runnable block) {
        menu(label, true, block);
    }

    public static void menu(String label, boolean enabled, Runnable block) {
        if (imgui.beginMenu(label, enabled)) {
            block.run();
            imgui.endMenu();
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
        if (imgui.menuItem(label, shortcut, selected, enabled))
            block.run();
    }


    // Tooltips

    public static void tooltip(Runnable block) {
        imgui.beginTooltip();
        block.run();
        imgui.endTooltip();
    }


    // Popups, Modals

    public static void popup(String strId, Runnable block) {
        popup(strId, 0, block);
    }

    public static void popup(String strId, int windowFlags, Runnable block) {
        if (imgui.beginPopup(strId, windowFlags)) {
            block.run();
            imgui.endPopup();
        }
    }

    public static void popupContextItem(String strId, Runnable block) {
        popupContextItem(strId, 1, block);
    }

    public static void popupContextItem(String strId, int mouseButton, Runnable block) {
        if (imgui.beginPopupContextItem(strId, mouseButton)) {
            block.run();
            imgui.endPopup();
        }
    }

    public static void popupContextWindow(String strId, Runnable block) {
        popupContextWindow(strId, 1, true, block);
    }

    public static void popupContextWindow(String strId, int mouseButton, Runnable block) {
        popupContextWindow(strId, mouseButton, true, block);
    }

    public static void popupContextWindow(String strId, int mouseButton, boolean alsoOverItems, Runnable block) {
        if (imgui.beginPopupContextWindow(strId, mouseButton, alsoOverItems)) {
            block.run();
            imgui.endPopup();
        }
    }

    public static void popupContextVoid(String strId, Runnable block) {
        popupContextVoid(strId, 1, block);
    }

    public static void popupContextVoid(String strId, int mouseButton, Runnable block) {
        if (imgui.beginPopupContextVoid(strId, mouseButton)) {
            block.run();
            imgui.endPopup();
        }
    }

    public static void popupModal(String name, Runnable block) {
        popupModal(name, null, 0, block);
    }

    public static void popupModal(String name, MutableProperty0<Boolean> pOpen, Runnable block) {
        popupModal(name, pOpen, 0, block);
    }

    public static void popupModal(String name, MutableProperty0<Boolean> pOpen, int windowFlags, Runnable block) {
        if (imgui.beginPopupModal(name, pOpen, windowFlags)) {
            block.run();
            imgui.endPopup();
        }
    }


    // Tab Bars, Tabs

    public static void tabBar(String strId, Runnable block) {
        tabBar(strId, 0, block);
    }

    public static void tabBar(String strId, int tabBarFlags, Runnable block) {
        if (imgui.beginTabBar(strId, tabBarFlags))
            block.run();
        imgui.endTabBar();
    }

    public static void tabItem(String label, Runnable block) {
        tabItem(label, null, 0, block);
    }

    public static void tabItem(String label, MutableProperty0<Boolean> pOpen, Runnable block) {
        tabItem(label, pOpen, 0, block);
    }

    public static void tabItem(String label, MutableProperty0<Boolean> pOpen, int tabItemFlags, Runnable block) {
        if (imgui.beginTabItem(label, pOpen, tabItemFlags))
            block.run();
        imgui.endTabItem();
    }


    // Drag and Drop

    public static void dragDropSource(Runnable block) {
        dragDropSource(0, block);
    }

    public static void dragDropSource(int dragDropFlags, Runnable block) {
        if (imgui.beginDragDropSource(dragDropFlags)) {
            block.run();
            imgui.endDragDropSource();
        }
    }

    public static void dragDropTarget(Runnable block) {
        if (imgui.beginDragDropTarget()) {
            block.run();
            imgui.endDragDropTarget();
        }
    }


    // Clipping

    public static void withClipRect(Vec2 clipRectMin, Vec2 clipRectMax, boolean intersectWithCurrentClipRect, Runnable block) {
        imgui.pushClipRect(clipRectMin, clipRectMax, intersectWithCurrentClipRect);
        block.run();
        imgui.popClipRect();
    }


    // Miscellaneous Utilities

    public static void childFrame(int id, Vec2 size, Runnable block) {
        childFrame(id, size, 0, block);
    }

    public static void childFrame(int id, Vec2 size, int windowFlags, Runnable block) {
        imgui.beginChildFrame(id, size, windowFlags);
        block.run();
        imgui.endChildFrame();
    }
}
