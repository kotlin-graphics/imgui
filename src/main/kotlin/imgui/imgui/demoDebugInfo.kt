package imgui.imgui

import glm_.BYTES
import glm_.f
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.Context.overlayDrawList
import imgui.Context.style
import imgui.ImGui._begin
import imgui.ImGui.beginTooltip
import imgui.ImGui.bulletText
import imgui.ImGui.checkbox
import imgui.ImGui.combo
import imgui.ImGui.end
import imgui.ImGui.endTooltip
import imgui.ImGui.inputFloat
import imgui.ImGui.isItemHovered
import imgui.ImGui.menuItem
import imgui.ImGui.popTextWrapPos
import imgui.ImGui.pushTextWrapPos
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.sliderFloat
import imgui.ImGui.text
import imgui.ImGui.textColored
import imgui.ImGui.textDisabled
import imgui.ImGui.textUnformatted
import imgui.ImGui.treeNode
import imgui.ImGui.treePop
import imgui.ImGui.version
import imgui.ImGui.windowDrawList
import imgui.functionalProgramming.menu
import imgui.functionalProgramming.withChild
import imgui.imgui.demo.ExampleApp
import imgui.internal.Rect
import imgui.internal.Window
import java.util.*
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlags as Cef
import imgui.Context as g
import imgui.InputTextFlags as Itf
import imgui.SelectableFlags as Sf
import imgui.TreeNodeFlags as Tnf
import imgui.WindowFlags as Wf

/**
 *  Message to the person tempted to delete this file when integrating ImGui into their code base:
 *  Do NOT remove this file from your project! It is useful reference code that you and other users will want to refer to.
 *  Don't do it! Do NOT remove this file from your project! It is useful reference code that you and other users will want to refer to.
 *  Everything in this file will be stripped out by the linker if you don't call ImGui::ShowTestWindow().
 *  During development, you can call ImGui::ShowTestWindow() in your code to learn about various features of ImGui.
 *  Removing this file from your project is hindering your access to documentation, likely leading you to poorer usage of the library.
 *  During development, you can call ImGui::ShowTestWindow() in your code to learn about various features of ImGui. Have it wired in a debug menu!
 *  Removing this file from your project is hindering access to documentation for everyone in your team, likely leading you to poorer usage of the library.
 *
 *  Note that you can #define IMGUI_DISABLE_TEST_WINDOWS in imconfig.h for the same effect.
 *  If you want to link core ImGui in your public builds but not those test windows, #define IMGUI_DISABLE_TEST_WINDOWS in imconfig.h and those functions will be empty.
 *  For any other case, if you have ImGui available you probably want this to be available for reference and execution.
 *
 *  Thank you,
 *  -Your beloved friend, imgui_demo.cpp (that you won't delete)
 */
interface imgui_demoDebugInfo {
    /** Create demo/test window.
     *  Demonstrate most ImGui features (big function!)
     *  Call this to learn about the library! try to make it always available in your application!   */
    fun showTestWindow(open: BooleanArray) {
        showWindow = open[0]
        showTestWindow(Companion::showWindow)
        open[0] = showWindow
    }

    fun showTestWindow(open: KMutableProperty0<Boolean>) {

        ExampleApp(open)

//
//    if (ImGui::CollapsingHeader("Filtering"))
//    {
//        static ImGuiTextFilter filter;
//        ImGui::Text("Filter usage:\n"
//                "  \"\"         display all lines\n"
//        "  \"xxx\"      display lines containing \"xxx\"\n"
//        "  \"xxx,yyy\"  display lines containing \"xxx\" or \"yyy\"\n"
//        "  \"-xxx\"     hide lines containing \"xxx\"");
//        filter.Draw();
//        const char* lines[] = { "aaa1.c", "bbb1.c", "ccc1.c", "aaa2.cpp", "bbb2.cpp", "ccc2.cpp", "abc.h", "hello, world" };
//        for (int i = 0; i < IM_ARRAYSIZE(lines); i++)
//        if (filter.PassFilter(lines[i]))
//            ImGui::BulletText("%s", lines[i]);
//    }
//        if (ImGui::CollapsingHeader("Inputs & Focus"))
//        {
//            +        ImGuiIO& io = ImGui::GetIO();
//            +        ImGui::Checkbox("io.MouseDrawCursor", &io.MouseDrawCursor);
//            +        ImGui::SameLine(); ShowHelpMarker("Request ImGui to render a mouse cursor for you in software. Note that a mouse cursor rendered via regular GPU rendering will feel more laggy than hardware cursor, but will be more in sync with your other visuals.");
//            +
//            +        ImGui::Text("WantCaptureMouse: %d", io.WantCaptureMouse);
//            +        ImGui::Text("WantCaptureKeyboard: %d", io.WantCaptureKeyboard);
//            +        ImGui::Text("WantTextInput: %d", io.WantTextInput);
//                    ImGui::Text("WantMoveMouse: %d", io.WantMoveMouse);
//            +
//            +        if (ImGui::TreeNode("Keyboard & Mouse State"))
//                +        {
//                    +            ImGui::Text("Mouse pos: (%g, %g)", io.MousePos.x, io.MousePos.y);
//                    +            ImGui::Text("Mouse down:");     for (int i = 0; i < IM_ARRAYSIZE(io.MouseDown); i++) if (io.MouseDownDuration[i] >= 0.0f)   { ImGui::SameLine(); ImGui::Text("b%d (%.02f secs)", i, io.MouseDownDuration[i]); }
//                    +            ImGui::Text("Mouse clicked:");  for (int i = 0; i < IM_ARRAYSIZE(io.MouseDown); i++) if (ImGui::IsMouseClicked(i))          { ImGui::SameLine(); ImGui::Text("b%d", i); }
//                    +            ImGui::Text("Mouse dbl-clicked:"); for (int i = 0; i < IM_ARRAYSIZE(io.MouseDown); i++) if (ImGui::IsMouseDoubleClicked(i)) { ImGui::SameLine(); ImGui::Text("b%d", i); }
//                    +            ImGui::Text("Mouse released:"); for (int i = 0; i < IM_ARRAYSIZE(io.MouseDown); i++) if (ImGui::IsMouseReleased(i))         { ImGui::SameLine(); ImGui::Text("b%d", i); }
//                    +            ImGui::Text("Mouse wheel: %.1f", io.MouseWheel);
//                    +
//                    +            ImGui::Text("Keys down:");      for (int i = 0; i < IM_ARRAYSIZE(io.KeysDown); i++) if (io.KeysDownDuration[i] >= 0.0f)     { ImGui::SameLine(); ImGui::Text("%d (%.02f secs)", i, io.KeysDownDuration[i]); }
//                    +            ImGui::Text("Keys pressed:");   for (int i = 0; i < IM_ARRAYSIZE(io.KeysDown); i++) if (ImGui::IsKeyPressed(i))             { ImGui::SameLine(); ImGui::Text("%d", i); }
//                    +            ImGui::Text("Keys release:");   for (int i = 0; i < IM_ARRAYSIZE(io.KeysDown); i++) if (ImGui::IsKeyReleased(i))            { ImGui::SameLine(); ImGui::Text("%d", i); }
//                    +            ImGui::Text("Keys mods: %s%s%s%s", io.KeyCtrl ? "CTRL " : "", io.KeyShift ? "SHIFT " : "", io.KeyAlt ? "ALT " : "", io.KeySuper ? "SUPER " : "");
//                    +
//                    +
//                    +            ImGui::Button("Hovering me sets the\nkeyboard capture flag");
//                    +            if (ImGui::IsItemHovered())
//                        +                ImGui::CaptureKeyboardFromApp(true);
//                    +            ImGui::SameLine();
//                    +            ImGui::Button("Holding me clears the\nthe keyboard capture flag");
//                    +            if (ImGui::IsItemActive())
//                        +                ImGui::CaptureKeyboardFromApp(false);
//                    +
//                    +            ImGui::TreePop();
//                    +        }
//            +
//            if (ImGui::TreeNode("Tabbing"))
//            {
//                ImGui::Text("Use TAB/SHIFT+TAB to cycle through keyboard editable fields.");
//                static char buf[32] = "dummy";
//                ImGui::InputText("1", buf, IM_ARRAYSIZE(buf));
//                ImGui::InputText("2", buf, IM_ARRAYSIZE(buf));
//                ImGui::InputText("3", buf, IM_ARRAYSIZE(buf));
//                ImGui::PushAllowKeyboardFocus(false);
//                ImGui::InputText("4 (tab skip)", buf, IM_ARRAYSIZE(buf));
//                //ImGui::SameLine(); ShowHelperMarker("Use ImGui::PushAllowKeyboardFocus(bool)\nto disable tabbing through certain widgets.");
//                ImGui::PopAllowKeyboardFocus();
//                ImGui::InputText("5", buf, IM_ARRAYSIZE(buf));
//                ImGui::TreePop();
//            }
//
//            if (ImGui::TreeNode("Focus from code"))
//            {
//                bool focus_1 = ImGui::Button("Focus on 1"); ImGui::SameLine();
//                bool focus_2 = ImGui::Button("Focus on 2"); ImGui::SameLine();
//                bool focus_3 = ImGui::Button("Focus on 3");
//                int has_focus = 0;
//                static char buf[128] = "click on a button to set focus";
//
//                if (focus_1) ImGui::SetKeyboardFocusHere();
//                ImGui::InputText("1", buf, IM_ARRAYSIZE(buf));
//                if (ImGui::IsItemActive()) has_focus = 1;
//
//                if (focus_2) ImGui::SetKeyboardFocusHere();
//                ImGui::InputText("2", buf, IM_ARRAYSIZE(buf));
//                if (ImGui::IsItemActive()) has_focus = 2;
//
//                ImGui::PushAllowKeyboardFocus(false);
//                if (focus_3) ImGui::SetKeyboardFocusHere();
//                ImGui::InputText("3 (tab skip)", buf, IM_ARRAYSIZE(buf));
//                if (ImGui::IsItemActive()) has_focus = 3;
//                ImGui::PopAllowKeyboardFocus();
//                if (has_focus)
//                    ImGui::Text("Item with focus: %d", has_focus);
//                else
//                    ImGui::Text("Item with focus: <none>");
//                ImGui::TextWrapped("Cursor & selection are preserved when refocusing last used item in code.");
//                ImGui::TreePop();
//            }
//
//        if (ImGui::TreeNode("Hovering"))
//            +        {
//                +            // Testing IsWindowHovered() function
//                +            ImGui::BulletText(
//                        +                "IsWindowHovered() = %d\n"
//                                +                "IsWindowHovered(_AllowWhenBlockedByPopup) = %d\n"
//                                +                "IsWindowHovered(_AllowWhenBlockedByActiveItem) = %d\n",
//                        +                ImGui::IsWindowHovered(),
//                        +                ImGui::IsWindowHovered(ImGuiHoveredFlags_AllowWhenBlockedByPopup),
//                        +                ImGui::IsWindowHovered(ImGuiHoveredFlags_AllowWhenBlockedByActiveItem));
//                +
//                +            // Testing IsItemHovered() function (because BulletText is an item itself and that would affect the output of IsItemHovered, we pass all lines in a single items to shorten the code)
//                +            ImGui::Button("ITEM");
//                +            ImGui::BulletText(
//                        +                "IsItemHovered() = %d\n"
//                                +                "IsItemHovered(_AllowWhenBlockedByPopup) = %d\n"
//                                +                "IsItemHovered(_AllowWhenBlockedByActiveItem) = %d\n"
//                                +                "IsItemHovered(_AllowWhenOverlapped) = %d\n"
//                                +                "IsItemhovered(_RectOnly) = %d\n",
//                        +                ImGui::IsItemHovered(),
//                        +                ImGui::IsItemHovered(ImGuiHoveredFlags_AllowWhenBlockedByPopup),
//                        +                ImGui::IsItemHovered(ImGuiHoveredFlags_AllowWhenBlockedByActiveItem),
//                        +                ImGui::IsItemHovered(ImGuiHoveredFlags_AllowWhenOverlapped),
//                        +                ImGui::IsItemHovered(ImGuiHoveredFlags_RectOnly));
//                +
//                +            ImGui::TreePop();
//                +        }
//
//            if (ImGui::TreeNode("Dragging"))
//            {
//                ImGui::TextWrapped("You can use ImGui::GetMouseDragDelta(0) to query for the dragged amount on any widget.");
//                ImGui::Button("Drag Me");
//                if (ImGui::IsItemActive())
//                {
//                    // Draw a line between the button and the mouse cursor
//                    ImDrawList* draw_list = ImGui::GetWindowDrawList();
//                    draw_list->PushClipRectFullScreen();
//        draw_list->AddLine(ImGui::CalcItemRectClosestPoint(io.MousePos, true, -2.0f), io.MousePos, ImColor(ImGui::GetStyle().Colors[ImGuiCol_Button]), 4.0f);
//        draw_list->PopClipRect();
//        ImVec2 value_raw = ImGui::GetMouseDragDelta(0, 0.0f);
//        ImVec2 value_with_lock_threshold = ImGui::GetMouseDragDelta(0);
//        ImVec2 mouse_delta = io.MouseDelta;
//        ImGui::SameLine(); ImGui::Text("Raw (%.1f, %.1f), WithLockThresold (%.1f, %.1f), MouseDelta (%.1f, %.1f)", value_raw.x, value_raw.y, value_with_lock_threshold.x, value_with_lock_threshold.y, mouse_delta.x, mouse_delta.y);
//    }
//    ImGui::TreePop();
//}
//        if (ImGui::TreeNode("Mouse cursors"))
//        {
//        ImGui::Text("Hover to see mouse cursors:");
//        +            ImGui::SameLine(); ShowHelpMarker("Your application can render a different mouse cursor based on what ImGui::GetMouseCursor() returns. If software cursor rendering (io.MouseDrawCursor) is set ImGui will draw the right cursor for you, otherwise your backend needs to handle it.");
//                     for (int i = 0; i < ImGuiMouseCursor_Count_; i++)
//        {
//            char label[32];
//            sprintf(label, "Mouse cursor %d", i);
//            ImGui::Bullet(); ImGui::Selectable(label, false);
//            if (ImGui::IsItemHovered())
//                ImGui::SetMouseCursor(i);
//        }
//        ImGui::TreePop();
//    }
//}
        end()
    }

    /** create metrics window. display ImGui internals: browse window list, draw commands, individual vertices, basic
     *  internal state, etc.    */
    fun showMetricsWindow(open: KMutableProperty0<Boolean>) {

        if (_begin("ImGui Metrics", open)) {
            text("ImGui $version")
            text("Application average %.3f ms/frame (%.1f FPS)", 1000f / IO.framerate, IO.framerate)
            text("%d vertices, %d indices (%d triangles)", IO.metricsRenderVertices, IO.metricsRenderIndices, IO.metricsRenderIndices / 3)
            text("%d allocations", IO.metricsAllocs)
            checkbox("Show clipping rectangles when hovering an ImDrawCmd", Companion::showClipRects)
            separator()

            Funcs0.nodeWindows(g.windows, "Windows")
            if (treeNode("DrawList", "Active DrawLists (${g.renderDrawLists[0].size})")) {
                g.renderDrawLists.forEach { layer -> layer.forEach { Funcs0.nodeDrawList(it, "DrawList") } }
                for (i in g.renderDrawLists[0])
                    Funcs0.nodeDrawList(i, "DrawList")
                treePop()
            }
            if (treeNode("Popups", "Open Popups Stack (${g.openPopupStack.size})")) {
                for (popup in g.openPopupStack) {
                    val window = popup.window
                    val childWindow = if (window != null && window.flags has Wf.ChildWindow) " ChildWindow" else ""
                    val childMenu = if (window != null && window.flags has Wf.ChildMenu) " ChildMenu" else ""
                    bulletText("PopupID: %08x, Window: '${window?.name}'$childWindow$childMenu", popup.popupId)
                }
                treePop()
            }
            if (treeNode("Basic state")) {
                text("HoveredWindow: '${g.hoveredWindow?.name}'")
                text("HoveredRootWindow: '${g.hoveredWindow?.name}'")
                /*  Data is "in-flight" so depending on when the Metrics window is called we may see current frame
                    information or not                 */
                text("HoveredId: 0x%08X/0x%08X (%.2f sec)", g.hoveredId, g.hoveredIdPreviousFrame, g.hoveredIdTimer)
                text("ActiveId: 0x%08X/0x%08X (%.2f sec)", g.activeId, g.activeIdPreviousFrame, g.activeIdTimer)
                text("ActiveIdWindow: '${g.activeIdWindow?.name}'")
                text("NavWindow: '${g.navWindow?.name}'")
                treePop()
            }
        }
        end()
    }


    fun showUserGuide() {
        bulletText("Double-click on title bar to collapse window.")
        bulletText("Click and drag on lower right corner to resize window.")
        bulletText("Click and drag on any empty space to move window.")
        bulletText("Mouse Wheel to scroll.")
        if (IO.fontAllowUserScaling)
            bulletText("CTRL+Mouse Wheel to zoom window contents.")
        bulletText("TAB/SHIFT+TAB to cycle through keyboard editable fields.")
        bulletText("CTRL+Click on a slider or drag box to input text.")
        bulletText(
                "While editing text:\n" +
                        "- Hold SHIFT or use mouse to select text\n" +
                        "- CTRL+Left/Right to word jump\n" +
                        "- CTRL+A or double-click to select all\n" +
                        "- CTRL+X,CTRL+C,CTRL+V clipboard\n" +
                        "- CTRL+Z,CTRL+Y undo/redo\n" +
                        "- ESCAPE to revert\n" +
                        "- You can apply arithmetic operators +,*,/ on numerical values.\n" +
                        "  Use +- to subtract.\n")
    }

    companion object {

        var showWindow = false

        fun showHelpMarker(desc: String) {
            textDisabled("(?)")
            if (isItemHovered()) {
                beginTooltip()
                pushTextWrapPos(450f)
                textUnformatted(desc)
                popTextWrapPos()
                endTooltip()
            }
        }

        var enabled = true
        var float = 0.5f
        var combo = 0
        var check = true

        fun showExampleMenuFile() {
            menuItem("(dummy menu)", "", false, false)
            menuItem("New")
            menuItem("Open", "Ctrl+O")
            menu("Open Recent") {
                menuItem("fish_hat.c")
                menuItem("fish_hat.inl")
                menuItem("fish_hat.h")
                menu("More..") {
                    menuItem("Hello")
                    menuItem("Sailor")
                    menu("Recurse..") { showExampleMenuFile() }
                }
            }
            menuItem("Save", "Ctrl+S")
            menuItem("Save As..")
            separator()
            menu("Options") {
                menuItem("Enabled", "", Companion::enabled)
                withChild("child", Vec2(0, 60), true) {
                    for (i in 0 until 10) text("Scrolling Text %d", i)
                }
                sliderFloat("Value", Companion::float, 0f, 1f)
                inputFloat("Input", Companion::float, 0.1f, 0f, 2)
                combo("Combo", Companion::combo, "Yes\u0000No\u0000Maybe\u0000\u0000")
                checkbox("Check", Companion::check)
            }
            menu("Colors") { for (col in Col.values()) menuItem(col.toString()) }
            menu("Disabled", false) { assert(false) } // Disabled
            menuItem("Checked", selected = true)
            menuItem("Quit", "Alt+F4")
        }


        object Funcs0 {

            fun nodeDrawList(drawList: DrawList, label: String) {

                val nodeOpen = treeNode(drawList, "$label: '${drawList._ownerName}' ${drawList.vtxBuffer.size} vtx, " +
                        "${drawList.idxBuffer.size} indices, ${drawList.cmdBuffer.size} cmds")
                if (drawList === windowDrawList) {
                    sameLine()
                    // Can't display stats for active draw list! (we don't have the data double-buffered)
                    textColored(Vec4.fromColor(255, 100, 100), "CURRENTLY APPENDING")
                    if (nodeOpen) treePop()
                    return
                }
                if (!nodeOpen)
                    return

                val overlayDrawList = g.overlayDrawList   // Render additional visuals into the top-most draw list
                overlayDrawList.pushClipRectFullScreen()
                var elemOffset = 0
                for (i in drawList.cmdBuffer.indices) {
                    val cmd = drawList.cmdBuffer[i]
                    if (cmd.userCallback == null && cmd.elemCount == 0) continue
                    if (cmd.userCallback != null) {
                        TODO()
//                        ImGui::BulletText("Callback %p, user_data %p", pcmd->UserCallback, pcmd->UserCallbackData)
//                        continue
                    }
                    val idxBuffer = drawList.idxBuffer.takeIf { it.isNotEmpty() }
                    val mode = if (drawList.idxBuffer.isNotEmpty()) "indexed" else "non-indexed"
                    val cmdNodeOpen = treeNode(i, "Draw %-4d $mode vtx, tex = ${cmd.textureId}, clip_rect = (%.0f,%.0f)..(%.0f,%.0f)",
                            cmd.elemCount, cmd.clipRect.x, cmd.clipRect.y, cmd.clipRect.z, cmd.clipRect.w)
                    if (showClipRects && isItemHovered()) {
                        val clipRect = Rect(cmd.clipRect)
                        val vtxsRect = Rect()
                        for (e in elemOffset until elemOffset + cmd.elemCount)
                            vtxsRect.add(drawList.vtxBuffer[idxBuffer?.get(e) ?: e].pos)
                        clipRect.floor(); overlayDrawList.addRect(clipRect.min, clipRect.max, COL32(255, 255, 0, 255))
                        vtxsRect.floor(); overlayDrawList.addRect(vtxsRect.min, vtxsRect.max, COL32(255, 0, 255, 255))
                    }
                    if (!cmdNodeOpen) continue
                    // Manually coarse clip our print out of individual vertices to save CPU, only items that may be visible.
                    val clipper = ListClipper(cmd.elemCount / 3)
                    while (clipper.step()) {
                        var vtxI = elemOffset + clipper.display.start * 3
                        for (prim in clipper.display.start until clipper.display.last) {
                            val buf = CharArray(300)
                            var bufP = 0
                            val trianglesPos = arrayListOf(Vec2(), Vec2(), Vec2())
                            for (n in 0 until 3) {
                                val v = drawList.vtxBuffer[idxBuffer?.get(vtxI) ?: vtxI]
                                trianglesPos[n] = v.pos
                                val name = if (n == 0) "vtx" else "   "
                                val string = "$name %04d { pos = (%8.2f,%8.2f), uv = (%.6f,%.6f), col = %08X }\n".format(style.locale,
                                        vtxI, v.pos.x, v.pos.y, v.uv.x, v.uv.y, v.col)
                                string.toCharArray(buf, bufP)
                                bufP += string.length
                                vtxI++
                            }
                            selectable(buf.joinToString("", limit = bufP, truncated = ""), false)
                            if (isItemHovered())
                            // Add triangle without AA, more readable for large-thin triangle
                                overlayDrawList.addPolyline(trianglesPos, COL32(255, 255, 0, 255), true, 1f, false)
                        }
                    }
                    treePop()
                    elemOffset += cmd.elemCount
                }
                overlayDrawList.popClipRect()
                treePop()
            }

            fun nodeWindows(windows: ArrayList<Window>, label: String) {
                if (!treeNode(label, "$label (${windows.size})")) return
                for (i in 0 until windows.size)
                    nodeWindow(windows[i], "Window")
                treePop()
            }

            fun nodeWindow(window: Window, label: String) {
                val active = if (window.active or window.wasActive) "active" else "inactive"
                if (!treeNode(window, "$label '${window.name}', $active @ 0x%X", System.identityHashCode(window)))
                    return
                nodeDrawList(window.drawList, "DrawList")
                bulletText("Pos: (%.1f,%.1f), Size: (%.1f,%.1f), SizeContents (%.1f,%.1f)", window.pos.x.f, window.pos.y.f,
                        window.size.x, window.size.y, window.sizeContents.x, window.sizeContents.y)
                if (isItemHovered())
                    overlayDrawList.addRect(Vec2(window.pos), Vec2(window.pos + window.size), COL32(255, 255, 0, 255))
                bulletText("Scroll: (%.2f,%.2f)", window.scroll.x, window.scroll.y)
                bulletText("Active: ${window.active}, Accessed: ${window.accessed}")
                if (window.rootWindow !== window) nodeWindow(window.rootWindow, "RootWindow")
                if (window.dc.childWindows.isNotEmpty()) nodeWindows(window.dc.childWindows, "ChildWindows")
                bulletText("Storage: %d bytes", window.stateStorage.data.size * Int.BYTES * 2)
                treePop()
            }
        }


        var showClipRects = true




        val selected = BooleanArray(4 + 3 + 16 + 16, { it == 1 || it == 23 + 0 || it == 23 + 5 || it == 23 + 10 || it == 23 + 15 })


        val buf1 = CharArray(64)
        val buf2 = CharArray(64)
        val buf3 = CharArray(64)
        val buf4 = CharArray(64)
        val buf5 = CharArray(64)
        val buf6 = CharArray(64)


    }





}