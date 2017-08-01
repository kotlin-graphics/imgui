package imgui.imgui

import glm_.BYTES
import glm_.f
import glm_.glm
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.beginChild
import imgui.ImGui.beginMenu
import imgui.ImGui.bulletText
import imgui.ImGui.combo
import imgui.ImGui.endChild
import imgui.ImGui.endMenu
import imgui.ImGui.inputFloat
import imgui.ImGui.isItemHovered
import imgui.ImGui.menuItem
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.sliderFloat
import imgui.ImGui.text
import imgui.ImGui.textColored
import imgui.ImGui.treeNode
import imgui.ImGui.treePop
import imgui.ImGui.windowDrawList
import imgui.internal.Rect
import imgui.internal.Window
import java.util.*
import imgui.Context as g

interface imgui_demoDebugInfo {

    /** Create demo/test window.
     *  Demonstrate most ImGui features (big function!)
     *  Call this to learn about the library! try to make it always available in your application!   */
    fun showTestWindow(pOpen: BooleanArray) = with(ImGui) {

        if (showApp.mainMenuBar[0]) showExampleAppMainMenuBar()
        if (showApp.console[0]) showExampleAppConsole(showApp.console)
        if (showApp.log[0]) showExampleAppLog(showApp.log)
        if (showApp.layout[0]) showExampleAppLayout(showApp.layout)
        if (showApp.propertyEditor[0]) showExampleAppPropertyEditor(showApp.propertyEditor)
        if (showApp.longText[0]) showExampleAppLongText(showApp.longText)
        if (showApp.autoResize[0]) showExampleAppAutoResize(showApp.autoResize)
        if (showApp.constrainedResize[0]) showExampleAppConstrainedResize(showApp.constrainedResize)
        if (showApp.fixedOverlay[0]) showExampleAppFixedOverlay(showApp.fixedOverlay)
        if (showApp.manipulatingWindowTitle[0]) showExampleAppManipulatingWindowTitle(showApp.manipulatingWindowTitle)
        if (showApp.customRendering[0]) showExampleAppCustomRendering(showApp.customRendering)
        if (showApp.metrics[0]) ImGui.showMetricsWindow(showApp.metrics)
        if (showApp.styleEditor[0]) {
            begin("Style Editor", pOpen = showApp.styleEditor)
            showStyleEditor()
            end()
        }
        if (showApp.about[0]) {
            begin("About ImGui", showApp.about, WindowFlags.AlwaysAutoResize.i)
            text("jvm imgui, %s", version)
            separator()
            text("Original by Omar Cornut, port by Giuseppe Barbieri and all github contributors.")
            text("ImGui is licensed under the MIT License, see LICENSE for more information.")
            end()
        }

        // Demonstrate the various window flags. Typically you would just use the default.
        var windowFlags = 0
        if (noTitlebar) windowFlags = windowFlags or WindowFlags.NoTitleBar
        if (!noBorder) windowFlags = windowFlags or WindowFlags.ShowBorders
        if (noResize) windowFlags = windowFlags or WindowFlags.NoResize
        if (noMove) windowFlags = windowFlags or WindowFlags.NoMove
        if (noScrollbar) windowFlags = windowFlags or WindowFlags.NoScrollbar
        if (noCollapse) windowFlags = windowFlags or WindowFlags.NoCollapse
        if (!noMenu) windowFlags = windowFlags or WindowFlags.MenuBar
        setNextWindowSize(Vec2(550, 680), SetCond.FirstUseEver)
        if (!begin("ImGui Demo", pOpen, windowFlags)) {
            // Early out if the window is collapsed, as an optimization.
            end()
            return
        }

        //ImGui::PushItemWidth(ImGui::GetWindowWidth() * 0.65f);    // 2/3 of the space for widget and 1/3 for labels
        pushItemWidth(-140f)                                 // Right align, keep 140 pixels for labels

        text("JVM ImGui says hello.")

        // Menu
        if (beginMenuBar()) {
            if (beginMenu("Menu")) {
                showExampleMenuFile()
                endMenu()
            }
            if (beginMenu("Examples")) {
                menuItem("Main menu bar", pSelected = showApp.mainMenuBar)
                menuItem("Console", pSelected = showApp.console)
                menuItem("Log", pSelected = showApp.log)
                menuItem("Simple layout", pSelected = showApp.layout)
//                menuItem("Property editor", NULL, &show_app_property_editor)
//                menuItem("Long text display", NULL, &show_app_long_text)
//                menuItem("Auto-resizing window", NULL, &show_app_auto_resize)
//                menuItem("Constrained-resizing window", NULL, &show_app_constrained_resize)
//                menuItem("Simple overlay", NULL, &show_app_fixed_overlay)
//                menuItem("Manipulating window title", NULL, &show_app_manipulating_window_title)
//                menuItem("Custom rendering", NULL, &show_app_custom_rendering)
                endMenu()
            }
            if (beginMenu("Help")) {
                menuItem("Metrics", pSelected = showApp.metrics)
//                MenuItem("Style Editor", NULL, &show_app_style_editor)
//                MenuItem("About ImGui", NULL, &show_app_about)
                endMenu()
            }
            endMenuBar()
        }

        end()
    }

    /** create metrics window. display ImGui internals: browse window list, draw commands, individual vertices, basic
     *  internal state, etc.    */
    fun showMetricsWindow(pOpen: BooleanArray) = with(ImGui) {

        if (begin("ImGui Metrics", pOpen)) {
            text("ImGui $version")
            text("Application average %.3f ms/frame (%.1f FPS)", 1000f / IO.framerate, IO.framerate)
            text("%d vertices, %d indices (%d triangles)", IO.metricsRenderVertices, IO.metricsRenderIndices, IO.metricsRenderIndices / 3)
            text("%d allocations", IO.metricsAllocs)
            checkbox("Show clipping rectangles when hovering a ImDrawCmd", showClipRects)
            separator()

            Funcs.nodeWindows(g.windows, "Windows")
            if (treeNode("DrawList", "Active DrawLists (${g.renderDrawLists[0].size})")) {
                for (i in g.renderDrawLists[0])
                    Funcs.nodeDrawList(i, "DrawList")
                treePop()
            }
            if (treeNode("Popups", "Open Popups Stack (${g.openPopupStack.size})")) {
                for (popup in g.openPopupStack)                {
                    val window = popup.window
                    val childWindow = if(window != null && window.flags has WindowFlags.ChildWindow) " ChildWindow" else ""
                    val childMenu = if(window !=null && window.flags has WindowFlags.ChildMenu) " ChildMenu" else ""
                    bulletText("PopupID: %08x, Window: '${window?.name}'$childWindow$childMenu", popup.popupId)
                }
                treePop()
            }
            if (treeNode("Basic state")) {
                text("FocusedWindow: '${g.focusedWindow?.name}'")
                text("HoveredWindow: '${g.hoveredWindow?.name}'")
                text("HoveredRootWindow: '${g.hoveredWindow?.name}'")
                /*  Data is "in-flight" so depending on when the Metrics window is called we may see current frame
                    information or not                 */
                text("HoveredID: 0x%08X/0x%08X", g.hoveredId, g.hoveredIdPreviousFrame)
                text("ActiveID: 0x%08X/0x%08X", g.activeId, g.activeIdPreviousFrame)
                treePop()
            }
        }
        end()
    }

    object Funcs {

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
                if (cmd.userCallback != null) {
                    TODO()
//                        ImGui::BulletText("Callback %p, user_data %p", pcmd->UserCallback, pcmd->UserCallbackData)
//                        continue
                }
                val idxBuffer = drawList.idxBuffer.takeIf { it.isNotEmpty() }
                val mode = if (drawList.idxBuffer.isNotEmpty()) "indexed" else "non-indexed"
                val cmdNodeOpen = treeNode(i, "Draw %-4d $mode vtx, tex = ${cmd.textureId}, clip_rect = (%.0f,%.0f)..(%.0f,%.0f)",
                        cmd.elemCount, cmd.clipRect.x, cmd.clipRect.y, cmd.clipRect.z, cmd.clipRect.w)
                if (showClipRects[0] && isItemHovered()) {
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
                            val string = "$name %04d { pos = (%8.2f,%8.2f), uv = (%.6f,%.6f), col = %08X }\n".format(Style.locale,
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
            if (!treeNode(label, "%s (%d)", label, windows.size))
                return
            for (i in 0 until windows.size)
                nodeWindow(windows[i], "Window")
            treePop()
        }

        fun nodeWindow(window: Window, label: String) {
            val active = if(window.active or window.wasActive) "active" else "inactive"
            if (!treeNode(window, "$label '${window.name}', $active @ 0x%X", System.identityHashCode(window)))
                return
            nodeDrawList(window.drawList, "DrawList")
            bulletText("Pos: (%.1f,%.1f)", window.pos.x.f, window.pos.y.f)
            bulletText("Size: (%.1f,%.1f), SizeContents (%.1f,%.1f)", window.size.x, window.size.y, window.sizeContents.x, window.sizeContents.y)
            bulletText("Scroll: (%.2f,%.2f)", window.scroll.x, window.scroll.y)
            if (window.rootWindow !== window) nodeWindow(window.rootWindow, "RootWindow")
            if (window.dc.childWindows.isNotEmpty()) nodeWindows(window.dc.childWindows, "ChildWindows")
            bulletText("Storage: %d bytes", window.stateStorage.data.size * Int.BYTES * 2)
            treePop()
        }
    }

    fun showStyleEditor() {
        TODO()
//        ImGuiStyle& style = ImGui::GetStyle();
//
//        // You can pass in a reference ImGuiStyle structure to compare to, revert to and save to (else it compares to the default style)
//        const ImGuiStyle default_style; // Default style
//        if (ImGui::Button("Revert Style"))
//            style = ref ? *ref : default_style;
//
//        if (ref)
//        {
//            ImGui::SameLine();
//            if (ImGui::Button("Save Style"))
//            *ref = style;
//        }
//
//        ImGui::PushItemWidth(ImGui::GetWindowWidth() * 0.55f);
//
//        if (ImGui::TreeNode("Rendering"))
//        {
//            ImGui::Checkbox("Anti-aliased lines", &style.AntiAliasedLines);
//            ImGui::Checkbox("Anti-aliased shapes", &style.AntiAliasedShapes);
//            ImGui::PushItemWidth(100);
//            ImGui::DragFloat("Curve Tessellation Tolerance", &style.CurveTessellationTol, 0.02f, 0.10f, FLT_MAX, NULL, 2.0f);
//            if (style.CurveTessellationTol < 0.0f) style.CurveTessellationTol = 0.10f;
//            ImGui::DragFloat("Global Alpha", &style.Alpha, 0.005f, 0.20f, 1.0f, "%.2f"); // Not exposing zero here so user doesn't "lose" the UI (zero alpha clips all widgets). But application code could have a toggle to switch between zero and non-zero.
//            ImGui::PopItemWidth();
//            ImGui::TreePop();
//        }
//
//        if (ImGui::TreeNode("Settings"))
//        {
//            ImGui::SliderFloat2("WindowPadding", (float*)&style.WindowPadding, 0.0f, 20.0f, "%.0f");
//            ImGui::SliderFloat("WindowRounding", &style.WindowRounding, 0.0f, 16.0f, "%.0f");
//            ImGui::SliderFloat("ChildWindowRounding", &style.ChildWindowRounding, 0.0f, 16.0f, "%.0f");
//            ImGui::SliderFloat2("FramePadding", (float*)&style.FramePadding, 0.0f, 20.0f, "%.0f");
//            ImGui::SliderFloat("FrameRounding", &style.FrameRounding, 0.0f, 16.0f, "%.0f");
//            ImGui::SliderFloat2("ItemSpacing", (float*)&style.ItemSpacing, 0.0f, 20.0f, "%.0f");
//            ImGui::SliderFloat2("ItemInnerSpacing", (float*)&style.ItemInnerSpacing, 0.0f, 20.0f, "%.0f");
//            ImGui::SliderFloat2("TouchExtraPadding", (float*)&style.TouchExtraPadding, 0.0f, 10.0f, "%.0f");
//            ImGui::SliderFloat("IndentSpacing", &style.IndentSpacing, 0.0f, 30.0f, "%.0f");
//            ImGui::SliderFloat("ScrollbarSize", &style.ScrollbarSize, 1.0f, 20.0f, "%.0f");
//            ImGui::SliderFloat("ScrollbarRounding", &style.ScrollbarRounding, 0.0f, 16.0f, "%.0f");
//            ImGui::SliderFloat("GrabMinSize", &style.GrabMinSize, 1.0f, 20.0f, "%.0f");
//            ImGui::SliderFloat("GrabRounding", &style.GrabRounding, 0.0f, 16.0f, "%.0f");
//            ImGui::Text("Alignment");
//            ImGui::SliderFloat2("WindowTitleAlign", (float*)&style.WindowTitleAlign, 0.0f, 1.0f, "%.2f");
//            ImGui::SliderFloat2("ButtonTextAlign", (float*)&style.ButtonTextAlign, 0.0f, 1.0f, "%.2f"); ImGui::SameLine(); ShowHelpMarker("Alignment applies when a button is larger than its text content.");
//            ImGui::TreePop();
//        }
//
//        if (ImGui::TreeNode("Colors"))
//        {
//            static int output_dest = 0;
//            static bool output_only_modified = false;
//            if (ImGui::Button("Copy Colors"))
//            {
//                if (output_dest == 0)
//                    ImGui::LogToClipboard();
//                else
//                    ImGui::LogToTTY();
//                ImGui::LogText("ImGuiStyle& style = ImGui::GetStyle();" IM_NEWLINE);
//                for (int i = 0; i < ImGuiCol_COUNT; i++)
//                {
//                    const ImVec4& col = style.Colors[i];
//                    const char* name = ImGui::GetStyleColName(i);
//                    if (!output_only_modified || memcmp(&col, (ref ? &ref->Colors[i] : &default_style.Colors[i]), sizeof(ImVec4)) != 0)
//                    ImGui::LogText("style.Colors[ImGuiCol_%s]%*s= ImVec4(%.2ff, %.2ff, %.2ff, %.2ff);" IM_NEWLINE, name, 22 - (int)strlen(name), "", col.x, col.y, col.z, col.w);
//                }
//                ImGui::LogFinish();
//            }
//            ImGui::SameLine(); ImGui::PushItemWidth(120); ImGui::Combo("##output_type", &output_dest, "To Clipboard\0To TTY\0"); ImGui::PopItemWidth();
//            ImGui::SameLine(); ImGui::Checkbox("Only Modified Fields", &output_only_modified);
//
//            static ImGuiColorEditMode edit_mode = ImGuiColorEditMode_RGB;
//            ImGui::RadioButton("RGB", &edit_mode, ImGuiColorEditMode_RGB);
//            ImGui::SameLine();
//            ImGui::RadioButton("HSV", &edit_mode, ImGuiColorEditMode_HSV);
//            ImGui::SameLine();
//            ImGui::RadioButton("HEX", &edit_mode, ImGuiColorEditMode_HEX);
//            //ImGui::Text("Tip: Click on colored square to change edit mode.");
//
//            static ImGuiTextFilter filter;
//            filter.Draw("Filter colors", 200);
//
//            ImGui::BeginChild("#colors", ImVec2(0, 300), true, ImGuiWindowFlags_AlwaysVerticalScrollbar);
//            ImGui::PushItemWidth(-160);
//            ImGui::ColorEditMode(edit_mode);
//            for (int i = 0; i < ImGuiCol_COUNT; i++)
//            {
//                const char* name = ImGui::GetStyleColName(i);
//                if (!filter.PassFilter(name))
//                    continue;
//                ImGui::PushID(i);
//                ImGui::ColorEdit4(name, (float*)&style.Colors[i], true);
//                if (memcmp(&style.Colors[i], (ref ? &ref->Colors[i] : &default_style.Colors[i]), sizeof(ImVec4)) != 0)
//                {
//                    ImGui::SameLine(); if (ImGui::Button("Revert")) style.Colors[i] = ref ? ref->Colors[i] : default_style.Colors[i];
//                    if (ref) { ImGui::SameLine(); if (ImGui::Button("Save")) ref->Colors[i] = style.Colors[i]; }
//                }
//                ImGui::PopID();
//            }
//            ImGui::PopItemWidth();
//            ImGui::EndChild();
//
//            ImGui::TreePop();
//        }
//
//        if (ImGui::TreeNode("Fonts", "Fonts (%d)", ImGui::GetIO().Fonts->Fonts.Size))
//        {
//            ImGui::SameLine(); ShowHelpMarker("Tip: Load fonts with io.Fonts->AddFontFromFileTTF()\nbefore calling io.Fonts->GetTex* functions.");
//            ImFontAtlas* atlas = ImGui::GetIO().Fonts;
//            if (ImGui::TreeNode("Atlas texture", "Atlas texture (%dx%d pixels)", atlas->TexWidth, atlas->TexHeight))
//            {
//                ImGui::Image(atlas->TexID, ImVec2((float)atlas->TexWidth, (float)atlas->TexHeight), ImVec2(0,0), ImVec2(1,1), ImColor(255,255,255,255), ImColor(255,255,255,128));
//                ImGui::TreePop();
//            }
//            ImGui::PushItemWidth(100);
//            for (int i = 0; i < atlas->Fonts.Size; i++)
//            {
//                ImFont* font = atlas->Fonts[i];
//                ImGui::BulletText("Font %d: \'%s\', %.2f px, %d glyphs", i, font->ConfigData ? font->ConfigData[0].Name : "", font->FontSize, font->Glyphs.Size);
//                ImGui::TreePush((void*)(intptr_t)i);
//                ImGui::SameLine(); if (ImGui::SmallButton("Set as default")) ImGui::GetIO().FontDefault = font;
//                ImGui::PushFont(font);
//                ImGui::Text("The quick brown fox jumps over the lazy dog");
//                ImGui::PopFont();
//                if (ImGui::TreeNode("Details"))
//                {
//                    ImGui::DragFloat("Font scale", &font->Scale, 0.005f, 0.3f, 2.0f, "%.1f");   // Scale only this font
//                    ImGui::SameLine(); ShowHelpMarker("Note than the default embedded font is NOT meant to be scaled.\n\nFont are currently rendered into bitmaps at a given size at the time of building the atlas. You may oversample them to get some flexibility with scaling. You can also render at multiple sizes and select which one to use at runtime.\n\n(Glimmer of hope: the atlas system should hopefully be rewritten in the future to make scaling more natural and automatic.)");
//                    ImGui::Text("Ascent: %f, Descent: %f, Height: %f", font->Ascent, font->Descent, font->Ascent - font->Descent);
//                    ImGui::Text("Fallback character: '%c' (%d)", font->FallbackChar, font->FallbackChar);
//                    ImGui::Text("Texture surface: %d pixels (approx) ~ %dx%d", font->MetricsTotalSurface, (int)sqrtf((float)font->MetricsTotalSurface), (int)sqrtf((float)font->MetricsTotalSurface));
//                    for (int config_i = 0; config_i < font->ConfigDataCount; config_i++)
//                    {
//                        ImFontConfig* cfg = &font->ConfigData[config_i];
//                        ImGui::BulletText("Input %d: \'%s\', Oversample: (%d,%d), PixelSnapH: %d", config_i, cfg->Name, cfg->OversampleH, cfg->OversampleV, cfg->PixelSnapH);
//                    }
//                    if (ImGui::TreeNode("Glyphs", "Glyphs (%d)", font->Glyphs.Size))
//                    {
//                        // Display all glyphs of the fonts in separate pages of 256 characters
//                        const ImFont::Glyph* glyph_fallback = font->FallbackGlyph; // Forcefully/dodgily make FindGlyph() return NULL on fallback, which isn't the default behavior.
//                        font->FallbackGlyph = NULL;
//                        for (int base = 0; base < 0x10000; base += 256)
//                        {
//                            int count = 0;
//                            for (int n = 0; n < 256; n++)
//                            count += font->FindGlyph((ImWchar)(base + n)) ? 1 : 0;
//                            if (count > 0 && ImGui::TreeNode((void*)(intptr_t)base, "U+%04X..U+%04X (%d %s)", base, base+255, count, count > 1 ? "glyphs" : "glyph"))
//                            {
//                                float cell_spacing = style.ItemSpacing.y;
//                                ImVec2 cell_size(font->FontSize * 1, font->FontSize * 1);
//                                ImVec2 base_pos = ImGui::GetCursorScreenPos();
//                                ImDrawList* draw_list = ImGui::GetWindowDrawList();
//                                for (int n = 0; n < 256; n++)
//                                {
//                                    ImVec2 cell_p1(base_pos.x + (n % 16) * (cell_size.x + cell_spacing), base_pos.y + (n / 16) * (cell_size.y + cell_spacing));
//                                    ImVec2 cell_p2(cell_p1.x + cell_size.x, cell_p1.y + cell_size.y);
//                                    const ImFont::Glyph* glyph = font->FindGlyph((ImWchar)(base+n));;
//                                    draw_list->AddRect(cell_p1, cell_p2, glyph ? IM_COL32(255,255,255,100) : IM_COL32(255,255,255,50));
//                                    font->RenderChar(draw_list, cell_size.x, cell_p1, ImGui::GetColorU32(ImGuiCol_Text), (ImWchar)(base+n)); // We use ImFont::RenderChar as a shortcut because we don't have UTF-8 conversion functions available to generate a string.
//                                    if (glyph && ImGui::IsMouseHoveringRect(cell_p1, cell_p2))
//                                    {
//                                        ImGui::BeginTooltip();
//                                        ImGui::Text("Codepoint: U+%04X", base+n);
//                                        ImGui::Separator();
//                                        ImGui::Text("XAdvance+1: %.1f", glyph->XAdvance);
//                                        ImGui::Text("Pos: (%.2f,%.2f)->(%.2f,%.2f)", glyph->X0, glyph->Y0, glyph->X1, glyph->Y1);
//                                        ImGui::Text("UV: (%.3f,%.3f)->(%.3f,%.3f)", glyph->U0, glyph->V0, glyph->U1, glyph->V1);
//                                        ImGui::EndTooltip();
//                                    }
//                                }
//                                ImGui::Dummy(ImVec2((cell_size.x + cell_spacing) * 16, (cell_size.y + cell_spacing) * 16));
//                                ImGui::TreePop();
//                            }
//                        }
//                        font->FallbackGlyph = glyph_fallback;
//                        ImGui::TreePop();
//                    }
//                    ImGui::TreePop();
//                }
//                ImGui::TreePop();
//            }
//            static float window_scale = 1.0f;
//            ImGui::DragFloat("this window scale", &window_scale, 0.005f, 0.3f, 2.0f, "%.1f");              // scale only this window
//            ImGui::DragFloat("global scale", &ImGui::GetIO().FontGlobalScale, 0.005f, 0.3f, 2.0f, "%.1f"); // scale everything
//            ImGui::PopItemWidth();
//            ImGui::SetWindowFontScale(window_scale);
//            ImGui::TreePop();
//        }
//
//        ImGui::PopItemWidth();
    }

    companion object {

        /** Demonstrate creating a fullscreen menu bar and populating it.   */
        fun showExampleAppMainMenuBar() = with(ImGui) {

            if (beginMainMenuBar()) {
                if (beginMenu("File")) {
                    showExampleMenuFile()
                    endMenu()
                }
                if (beginMenu("Edit")) {
                    if (menuItem("Undo", "CTRL+Z")) {
                    }
                    if (menuItem("Redo", "CTRL+Y", false, false)) { // Disabled item
                    }
                    separator()
                    if (menuItem("Cut", "CTRL+X")) {
                    }
                    if (menuItem("Copy", "CTRL+C")) {
                    }
                    if (menuItem("Paste", "CTRL+V")) {
                    }
                    endMenu()
                }
                endMainMenuBar()
            }
        }

        fun showExampleMenuFile() {

            menuItem("(dummy menu)", "", false, false)
            if (menuItem("New")) {
            }
            if (menuItem("Open", "Ctrl+O")) {
            }
            if (beginMenu("Open Recent")) {
                menuItem("fish_hat.c")
                menuItem("fish_hat.inl")
                menuItem("fish_hat.h")
                if (beginMenu("More..")) {
                    menuItem("Hello")
                    menuItem("Sailor")
                    if (beginMenu("Recurse..")) {
                        showExampleMenuFile()
                        endMenu()
                    }
                    endMenu()
                }
                endMenu()
            }
            if (menuItem("Save", "Ctrl+S")) {
            }
            if (menuItem("Save As..")) {
            }
            separator()
            if (beginMenu("Options")) {
                menuItem("Enabled", "", enabled)
                beginChild("child", Vec2(0, 60), true)
                for (i in 0 until 10)
                    text("Scrolling Text %d", i)
                endChild()
                sliderFloat("Value", f, 0f, 1f)
                inputFloat("Input", f, 0.1f, 0f, 2)
                combo("Combo", n, "Yes\u0000No\u0000Maybe\u0000\u0000")
                endMenu()
            }
            if (beginMenu("Colors")) {
                for (col in Col.values())
                    menuItem(col.toString())
                endMenu()
            }
            if (beginMenu("Disabled", false)) // Disabled
                assert(false)
            if (menuItem("Checked", selected = true)) {
            }
            if (menuItem("Quit", "Alt+F4")) {
            }
        }

        var enabled = booleanArrayOf(true)
        var f = floatArrayOf(0.5f)
        var n = intArrayOf(0)

        /** Demonstrate creating a window which gets auto-resized according to its content. */
        fun showExampleAppAutoResize(pOpen: BooleanArray) {
            TODO()
//            if (!ImGui::Begin("Example: Auto-resizing window", p_open, ImGuiWindowFlags_AlwaysAutoResize))
//            {
//                ImGui::End();
//                return;
//            }
//
//            static int lines = 10;
//            ImGui::Text("Window will resize every-frame to the size of its content.\nNote that you probably don't want to query the window size to\noutput your content because that would create a feedback loop.");
//            ImGui::SliderInt("Number of lines", &lines, 1, 20);
//            for (int i = 0; i < lines; i++)
//            ImGui::Text("%*sThis is line %d", i*4, "", i); // Pad with space to extend size horizontally
//            ImGui::End();
        }

        /** Demonstrate creating a window with custom resize constraints.   */
        fun showExampleAppConstrainedResize(pOpen: BooleanArray) {
            TODO()
//            struct CustomConstraints // Helper functions to demonstrate programmatic constraints
//                    {
//                        static void Square(ImGuiSizeConstraintCallbackData* data) { data->DesiredSize = ImVec2(IM_MAX(data->DesiredSize.x, data->DesiredSize.y), IM_MAX(data->DesiredSize.x, data->DesiredSize.y)); }
//                        static void Step(ImGuiSizeConstraintCallbackData* data)   { float step = (float)(int)(intptr_t)data->UserData; data->DesiredSize = ImVec2((int)(data->DesiredSize.x / step + 0.5f) * step, (int)(data->DesiredSize.y / step + 0.5f) * step); }
//                    };
//
//            static int type = 0;
//            if (type == 0) ImGui::SetNextWindowSizeConstraints(ImVec2(-1, 0),    ImVec2(-1, FLT_MAX));      // Vertical only
//            if (type == 1) ImGui::SetNextWindowSizeConstraints(ImVec2(0, -1),    ImVec2(FLT_MAX, -1));      // Horizontal only
//            if (type == 2) ImGui::SetNextWindowSizeConstraints(ImVec2(100, 100), ImVec2(FLT_MAX, FLT_MAX)); // Width > 100, Height > 100
//            if (type == 3) ImGui::SetNextWindowSizeConstraints(ImVec2(300, 0),   ImVec2(400, FLT_MAX));     // Width 300-400
//            if (type == 4) ImGui::SetNextWindowSizeConstraints(ImVec2(0, 0),     ImVec2(FLT_MAX, FLT_MAX), CustomConstraints::Square);          // Always Square
//            if (type == 5) ImGui::SetNextWindowSizeConstraints(ImVec2(0, 0),     ImVec2(FLT_MAX, FLT_MAX), CustomConstraints::Step, (void*)100);// Fixed Step
//
//            if (ImGui::Begin("Example: Constrained Resize", p_open))
//            {
//                const char* desc[] =
//                        {
//                            "Resize vertical only",
//                            "Resize horizontal only",
//                            "Width > 100, Height > 100",
//                            "Width 300-400",
//                            "Custom: Always Square",
//                            "Custom: Fixed Steps (100)",
//                        };
//                ImGui::Combo("Constraint", &type, desc, IM_ARRAYSIZE(desc));
//                if (ImGui::Button("200x200")) ImGui::SetWindowSize(ImVec2(200,200)); ImGui::SameLine();
//                if (ImGui::Button("500x500")) ImGui::SetWindowSize(ImVec2(500,500)); ImGui::SameLine();
//                if (ImGui::Button("800x200")) ImGui::SetWindowSize(ImVec2(800,200));
//                for (int i = 0; i < 10; i++)
//                ImGui::Text("Hello, sailor! Making this line long enough for the example.");
//            }
//            ImGui::End();
        }

        /** Demonstrate creating a simple static window with no decoration. */
        fun showExampleAppFixedOverlay(pOpen: BooleanArray) {
            TODO()
//            ImGui::SetNextWindowPos(ImVec2(10,10));
//            if (!ImGui::Begin("Example: Fixed Overlay", p_open, ImVec2(0,0), 0.3f, ImGuiWindowFlags_NoTitleBar|ImGuiWindowFlags_NoResize|ImGuiWindowFlags_NoMove|ImGuiWindowFlags_NoSavedSettings))
//            {
//                ImGui::End();
//                return;
//            }
//            ImGui::Text("Simple overlay\non the top-left side of the screen.");
//            ImGui::Separator();
//            ImGui::Text("Mouse Position: (%.1f,%.1f)", ImGui::GetIO().MousePos.x, ImGui::GetIO().MousePos.y);
//            ImGui::End();
        }

        /** Demonstrate using "##" and "###" in identifiers to manipulate ID generation.
         *  Read section "How can I have multiple widgets with the same label? Can I have widget without a label? (Yes).
         *  A primer on the purpose of labels/IDs." about ID.   */
        fun showExampleAppManipulatingWindowTitle(p: BooleanArray) {
            TODO()
            // By default, Windows are uniquely identified by their title.
            // You can use the "##" and "###" markers to manipulate the display/ID.

            // Using "##" to display same title but have unique identifier.
//            ImGui::SetNextWindowPos(ImVec2(100,100), ImGuiSetCond_FirstUseEver);
//            ImGui::Begin("Same title as another window##1");
//            ImGui::Text("This is window 1.\nMy title is the same as window 2, but my identifier is unique.");
//            ImGui::End();
//
//            ImGui::SetNextWindowPos(ImVec2(100,200), ImGuiSetCond_FirstUseEver);
//            ImGui::Begin("Same title as another window##2");
//            ImGui::Text("This is window 2.\nMy title is the same as window 1, but my identifier is unique.");
//            ImGui::End();
//
//            // Using "###" to display a changing title but keep a static identifier "AnimatedTitle"
//            char buf[128];
//            sprintf(buf, "Animated title %c %d###AnimatedTitle", "|/-\\"[(int)(ImGui::GetTime()/0.25f)&3], rand());
//            ImGui::SetNextWindowPos(ImVec2(100,300), ImGuiSetCond_FirstUseEver);
//            ImGui::Begin(buf);
//            ImGui::Text("This window has a changing title.");
//            ImGui::End();
        }

        /** Demonstrate using the low-level ImDrawList to draw custom shapes.   */
        fun showExampleAppCustomRendering(pOpen: BooleanArray) {
//            ImGui::SetNextWindowSize(ImVec2(350,560), ImGuiSetCond_FirstUseEver);
//            if (!ImGui::Begin("Example: Custom rendering", p_open))
//            {
//                ImGui::End();
//                return;
//            }
//
//            // Tip: If you do a lot of custom rendering, you probably want to use your own geometrical types and benefit of overloaded operators, etc.
//            // Define IM_VEC2_CLASS_EXTRA in imconfig.h to create implicit conversions between your types and ImVec2/ImVec4.
//            // ImGui defines overloaded operators but they are internal to imgui.cpp and not exposed outside (to avoid messing with your types)
//            // In this example we are not using the maths operators!
//            ImDrawList* draw_list = ImGui::GetWindowDrawList();
//
//            // Primitives
//            ImGui::Text("Primitives");
//            static float sz = 36.0f;
//            static ImVec4 col = ImVec4(1.0f,1.0f,0.4f,1.0f);
//            ImGui::DragFloat("Size", &sz, 0.2f, 2.0f, 72.0f, "%.0f");
//            ImGui::ColorEdit3("Color", &col.x);
//            {
//                const ImVec2 p = ImGui::GetCursorScreenPos();
//                const ImU32 col32 = ImColor(col);
//                float x = p.x + 4.0f, y = p.y + 4.0f, spacing = 8.0f;
//                for (int n = 0; n < 2; n++)
//                {
//                    float thickness = (n == 0) ? 1.0f : 4.0f;
//                    draw_list->AddCircle(ImVec2(x+sz*0.5f, y+sz*0.5f), sz*0.5f, col32, 20, thickness); x += sz+spacing;
//                    draw_list->AddRect(ImVec2(x, y), ImVec2(x+sz, y+sz), col32, 0.0f, ~0, thickness); x += sz+spacing;
//                    draw_list->AddRect(ImVec2(x, y), ImVec2(x+sz, y+sz), col32, 10.0f, ~0, thickness); x += sz+spacing;
//                    draw_list->AddTriangle(ImVec2(x+sz*0.5f, y), ImVec2(x+sz,y+sz-0.5f), ImVec2(x,y+sz-0.5f), col32, thickness); x += sz+spacing;
//                    draw_list->AddLine(ImVec2(x, y), ImVec2(x+sz, y   ), col32, thickness); x += sz+spacing;
//                    draw_list->AddLine(ImVec2(x, y), ImVec2(x+sz, y+sz), col32, thickness); x += sz+spacing;
//                    draw_list->AddLine(ImVec2(x, y), ImVec2(x,    y+sz), col32, thickness); x += spacing;
//                    draw_list->AddBezierCurve(ImVec2(x, y), ImVec2(x+sz*1.3f,y+sz*0.3f), ImVec2(x+sz-sz*1.3f,y+sz-sz*0.3f), ImVec2(x+sz, y+sz), col32, thickness);
//                    x = p.x + 4;
//                    y += sz+spacing;
//                }
//                draw_list->AddCircleFilled(ImVec2(x+sz*0.5f, y+sz*0.5f), sz*0.5f, col32, 32); x += sz+spacing;
//                draw_list->AddRectFilled(ImVec2(x, y), ImVec2(x+sz, y+sz), col32); x += sz+spacing;
//                draw_list->AddRectFilled(ImVec2(x, y), ImVec2(x+sz, y+sz), col32, 10.0f); x += sz+spacing;
//                draw_list->AddTriangleFilled(ImVec2(x+sz*0.5f, y), ImVec2(x+sz,y+sz-0.5f), ImVec2(x,y+sz-0.5f), col32); x += sz+spacing;
//                draw_list->AddRectFilledMultiColor(ImVec2(x, y), ImVec2(x+sz, y+sz), ImColor(0,0,0), ImColor(255,0,0), ImColor(255,255,0), ImColor(0,255,0));
//                ImGui::Dummy(ImVec2((sz+spacing)*8, (sz+spacing)*3));
//            }
//            ImGui::Separator();
//            {
//                static ImVector<ImVec2> points;
//                static bool adding_line = false;
//                ImGui::Text("Canvas example");
//                if (ImGui::Button("Clear")) points.clear();
//                if (points.Size >= 2) { ImGui::SameLine(); if (ImGui::Button("Undo")) { points.pop_back(); points.pop_back(); } }
//                ImGui::Text("Left-click and drag to add lines,\nRight-click to undo");
//
//                // Here we are using InvisibleButton() as a convenience to 1) advance the cursor and 2) allows us to use IsItemHovered()
//                // However you can draw directly and poll mouse/keyboard by yourself. You can manipulate the cursor using GetCursorPos() and SetCursorPos().
//                // If you only use the ImDrawList API, you can notify the owner window of its extends by using SetCursorPos(max).
//                ImVec2 canvas_pos = ImGui::GetCursorScreenPos();            // ImDrawList API uses screen coordinates!
//                ImVec2 canvas_size = ImGui::GetContentRegionAvail();        // Resize canvas to what's available
//                if (canvas_size.x < 50.0f) canvas_size.x = 50.0f;
//                if (canvas_size.y < 50.0f) canvas_size.y = 50.0f;
//                draw_list->AddRectFilledMultiColor(canvas_pos, ImVec2(canvas_pos.x + canvas_size.x, canvas_pos.y + canvas_size.y), ImColor(50,50,50), ImColor(50,50,60), ImColor(60,60,70), ImColor(50,50,60));
//                draw_list->AddRect(canvas_pos, ImVec2(canvas_pos.x + canvas_size.x, canvas_pos.y + canvas_size.y), ImColor(255,255,255));
//
//                bool adding_preview = false;
//                ImGui::InvisibleButton("canvas", canvas_size);
//                ImVec2 mouse_pos_in_canvas = ImVec2(ImGui::GetIO().MousePos.x - canvas_pos.x, ImGui::GetIO().MousePos.y - canvas_pos.y);
//                if (adding_line)
//                {
//                    adding_preview = true;
//                    points.push_back(mouse_pos_in_canvas);
//                    if (!ImGui::GetIO().MouseDown[0])
//                        adding_line = adding_preview = false;
//                }
//                if (ImGui::IsItemHovered())
//                {
//                    if (!adding_line && ImGui::IsMouseClicked(0))
//                    {
//                        points.push_back(mouse_pos_in_canvas);
//                        adding_line = true;
//                    }
//                    if (ImGui::IsMouseClicked(1) && !points.empty())
//                    {
//                        adding_line = adding_preview = false;
//                        points.pop_back();
//                        points.pop_back();
//                    }
//                }
//                draw_list->PushClipRect(canvas_pos, ImVec2(canvas_pos.x+canvas_size.x, canvas_pos.y+canvas_size.y));      // clip lines within the canvas (if we resize it, etc.)
//                for (int i = 0; i < points.Size - 1; i += 2)
//                draw_list->AddLine(ImVec2(canvas_pos.x + points[i].x, canvas_pos.y + points[i].y), ImVec2(canvas_pos.x + points[i+1].x, canvas_pos.y + points[i+1].y), IM_COL32(255,255,0,255), 2.0f);
//                draw_list->PopClipRect();
//                if (adding_preview)
//                    points.pop_back();
//            }
//            ImGui::End();
        }

        fun showExampleAppConsole(pOpen: BooleanArray) = console.draw("Example: Console", pOpen)

        val console = ExampleAppConsole()

        /** Demonstrate creating a simple log window with basic filtering.  */
        fun showExampleAppLog(pOpen: BooleanArray) {

            // Demo fill
            val time = ImGui.time
            if (time - lastTime >= 0.3f) {
                val s = randomWords[rand % randomWords.size]
                val t = "%.1f".format(Style.locale, time)
                log.addLog("[$s] Hello, time is $t, rand() $rand\n")
                lastTime = time
            }
            log.draw("Example: Log (Filter not yet implemented)", pOpen)
        }

        val log = ExampleAppLog()
        var lastTime = -1f
        val randomWords = arrayOf("system", "info", "warning", "error", "fatal", "notice", "log")
        val random = Random()
        val rand get() = glm.abs(random.nextInt() / 100_000)

        /** Demonstrate create a window with multiple child windows.    */
        fun showExampleAppLayout(pOpen: BooleanArray) = with(ImGui) {

            setNextWindowSize(Vec2(500, 440), SetCond.FirstUseEver)
            if (begin("Example: Layout", pOpen, WindowFlags.MenuBar.i)) {
                if (beginMenuBar()) {
                    if (beginMenu("File")) {
                        if (menuItem("Close")) pOpen[0] = false
                        endMenu()
                    }
                    endMenuBar()
                }

                // left
                beginChild("left pane", Vec2(150, 0), true)
                repeat(100) {
                    if (selectable("MyObject $it", selected == it))
                        selected = it
                }
                endChild()
                sameLine()

                // right
                beginGroup()
                beginChild("item view", Vec2(0, -itemsLineHeightWithSpacing)) // Leave room for 1 line below us
                text("MyObject: %d", selected)
                separator()
                textWrapped("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor " +
                        "incididunt ut labore et dolore magna aliqua. ")
                endChild()
                beginChild("buttons")
                if (button("Revert")) {
                }
                sameLine()
                if (button("Save")) {
                }
                endChild()
                endGroup()
            }
            end()
        }

        var selected = 0

        /** Demonstrate create a simple property editor.    */
        fun showExampleAppPropertyEditor(pOpen: BooleanArray) {
            TODO()
//            ImGui::SetNextWindowSize(ImVec2(430,450), ImGuiSetCond_FirstUseEver);
//            if (!ImGui::Begin("Example: Property editor", p_open))
//            {
//                ImGui::End();
//                return;
//            }
//
//            ShowHelpMarker("This example shows how you may implement a property editor using two columns.\nAll objects/fields data are dummies here.\nRemember that in many simple cases, you can use ImGui::SameLine(xxx) to position\nyour cursor horizontally instead of using the Columns() API.");
//
//            ImGui::PushStyleVar(ImGuiStyleVar_FramePadding, ImVec2(2,2));
//            ImGui::Columns(2);
//            ImGui::Separator();
//
//            struct funcs
//                    {
//                        static void ShowDummyObject(const char* prefix, int uid)
//                        {
//                            ImGui::PushID(uid);                      // Use object uid as identifier. Most commonly you could also use the object pointer as a base ID.
//                            ImGui::AlignFirstTextHeightToWidgets();  // Text and Tree nodes are less high than regular widgets, here we add vertical spacing to make the tree lines equal high.
//                            bool node_open = ImGui::TreeNode("Object", "%s_%u", prefix, uid);
//                            ImGui::NextColumn();
//                            ImGui::AlignFirstTextHeightToWidgets();
//                            ImGui::Text("my sailor is rich");
//                            ImGui::NextColumn();
//                            if (node_open)
//                            {
//                                static float dummy_members[8] = { 0.0f,0.0f,1.0f,3.1416f,100.0f,999.0f };
//                                for (int i = 0; i < 8; i++)
//                                {
//                                    ImGui::PushID(i); // Use field index as identifier.
//                                    if (i < 2)
//                                    {
//                                        ShowDummyObject("Child", 424242);
//                                    }
//                                    else
//                                    {
//                                        ImGui::AlignFirstTextHeightToWidgets();
//                                        // Here we use a Selectable (instead of Text) to highlight on hover
//                                        //ImGui::Text("Field_%d", i);
//                                        char label[32];
//                                        sprintf(label, "Field_%d", i);
//                                        ImGui::Bullet();
//                                        ImGui::Selectable(label);
//                                        ImGui::NextColumn();
//                                        ImGui::PushItemWidth(-1);
//                                        if (i >= 5)
//                                            ImGui::InputFloat("##value", &dummy_members[i], 1.0f);
//                                        else
//                                        ImGui::DragFloat("##value", &dummy_members[i], 0.01f);
//                                        ImGui::PopItemWidth();
//                                        ImGui::NextColumn();
//                                    }
//                                    ImGui::PopID();
//                                }
//                                ImGui::TreePop();
//                            }
//                            ImGui::PopID();
//                        }
//                    };
//
//            // Iterate dummy objects with dummy members (all the same data)
//            for (int obj_i = 0; obj_i < 3; obj_i++)
//            funcs::ShowDummyObject("Object", obj_i);
//
//            ImGui::Columns(1);
//            ImGui::Separator();
//            ImGui::PopStyleVar();
//            ImGui::End();
        }

        /** Demonstrate/test rendering huge amount of text, and the incidence of clipping.  */
        fun showExampleAppLongText(pOpen: BooleanArray) {
            TODO()
//            ImGui::SetNextWindowSize(ImVec2(520,600), ImGuiSetCond_FirstUseEver);
//            if (!ImGui::Begin("Example: Long text display", p_open))
//            {
//                ImGui::End();
//                return;
//            }
//
//            static int test_type = 0;
//            static ImGuiTextBuffer log;
//            static int lines = 0;
//            ImGui::Text("Printing unusually long amount of text.");
//            ImGui::Combo("Test type", &test_type, "Single call to TextUnformatted()\0Multiple calls to Text(), clipped manually\0Multiple calls to Text(), not clipped\0");
//            ImGui::Text("Buffer contents: %d lines, %d bytes", lines, log.size());
//            if (ImGui::Button("Clear")) { log.clear(); lines = 0; }
//            ImGui::SameLine();
//            if (ImGui::Button("Add 1000 lines"))
//            {
//                for (int i = 0; i < 1000; i++)
//                log.append("%i The quick brown fox jumps over the lazy dog\n", lines+i);
//                lines += 1000;
//            }
//            ImGui::BeginChild("Log");
//            switch (test_type)
//            {
//                case 0:
//                // Single call to TextUnformatted() with a big buffer
//                ImGui::TextUnformatted(log.begin(), log.end());
//                break;
//                case 1:
//                {
//                    // Multiple calls to Text(), manually coarsely clipped - demonstrate how to use the ImGuiListClipper helper.
//                    ImGui::PushStyleVar(ImGuiStyleVar_ItemSpacing, ImVec2(0,0));
//                    ImGuiListClipper clipper(lines);
//                    while (clipper.Step())
//                        for (int i = clipper.DisplayStart; i < clipper.DisplayEnd; i++)
//                    ImGui::Text("%i The quick brown fox jumps over the lazy dog", i);
//                    ImGui::PopStyleVar();
//                    break;
//                }
//                case 2:
//                // Multiple calls to Text(), not clipped (slow)
//                ImGui::PushStyleVar(ImGuiStyleVar_ItemSpacing, ImVec2(0,0));
//                for (int i = 0; i < lines; i++)
//                ImGui::Text("%i The quick brown fox jumps over the lazy dog", i);
//                ImGui::PopStyleVar();
//                break;
//            }
//            ImGui::EndChild();
//            ImGui::End();
        }

        object showApp {
            // Examples apps
            var mainMenuBar = booleanArrayOf(false)
            var console = booleanArrayOf(false)
            var log = booleanArrayOf(false)
            var layout = booleanArrayOf(false)
            var propertyEditor = booleanArrayOf(false)
            var longText = booleanArrayOf(false)
            var autoResize = booleanArrayOf(false)
            var constrainedResize = booleanArrayOf(false)
            var fixedOverlay = booleanArrayOf(false)
            var manipulatingWindowTitle = booleanArrayOf(false)
            var customRendering = booleanArrayOf(false)
            var styleEditor = booleanArrayOf(false)

            var metrics = booleanArrayOf(false)
            var about = booleanArrayOf(false)
        }

        var noTitlebar = false
        var noBorder = true
        var noResize = false
        var noMove = false
        var noScrollbar = false
        var noCollapse = false
        var noMenu = false


        val showClipRects = booleanArrayOf(true)
    }

    /** Demonstrating creating a simple console window, with scrolling, filtering, completion and history.
     *  For the console example, here we are using a more C++ like approach of declaring a class to hold the data and
     *  the functions.  */
    class ExampleAppConsole {
        //        char                  InputBuf[256];
//        ImVector<char*>       Items;
//        bool                  ScrollToBottom;
//        ImVector<char*>       History;
//        int                   HistoryPos;    // -1: new line, 0..History.Size-1 browsing history.
//        ImVector<const char*> Commands;
//
//        ExampleAppConsole()
//        {
//            ClearLog();
//            memset(InputBuf, 0, sizeof(InputBuf));
//            HistoryPos = -1;
//            Commands.push_back("HELP");
//            Commands.push_back("HISTORY");
//            Commands.push_back("CLEAR");
//            Commands.push_back("CLASSIFY");  // "classify" is here to provide an example of "C"+[tab] completing to "CL" and displaying matches.
//            AddLog("Welcome to ImGui!");
//        }
//        ~ExampleAppConsole()
//    {
//        ClearLog();
//        for (int i = 0; i < History.Size; i++)
//        free(History[i]);
//    }
//
//        // Portable helpers
//        static int   Stricmp(const char* str1, const char* str2)         { int d; while ((d = toupper(*str2) - toupper(*str1)) == 0 && *str1) { str1++; str2++; } return d; }
//        static int   Strnicmp(const char* str1, const char* str2, int n) { int d = 0; while (n > 0 && (d = toupper(*str2) - toupper(*str1)) == 0 && *str1) { str1++; str2++; n--; } return d; }
//        static char* Strdup(const char *str)                             { size_t len = strlen(str) + 1; void* buff = malloc(len); return (char*)memcpy(buff, (const void*)str, len); }
//
//        void    ClearLog()
//        {
//            for (int i = 0; i < Items.Size; i++)
//            free(Items[i]);
//            Items.clear();
//            ScrollToBottom = true;
//        }
//
//        void    AddLog(const char* fmt, ...) IM_PRINTFARGS(2)
//        {
//            char buf[1024];
//            va_list args;
//            va_start(args, fmt);
//            vsnprintf(buf, IM_ARRAYSIZE(buf), fmt, args);
//            buf[IM_ARRAYSIZE(buf)-1] = 0;
//            va_end(args);
//            Items.push_back(Strdup(buf));
//            ScrollToBottom = true;
//        }
//
        fun draw(title: String, pOpen: BooleanArray) = with(ImGui) {

            setNextWindowSize(Vec2(520, 600), SetCond.FirstUseEver)
            if (!begin(title, pOpen)) {
                end()
                return
            }

            textWrapped("This example is not yet implemented, you are welcome to contribute")
//            textWrapped("This example implements a console with basic coloring, completion and history. A more elaborate implementation may want to store entries along with extra data such as timestamp, emitter, etc.");
//            ImGui::TextWrapped("Enter 'HELP' for help, press TAB to use text completion.");
//
//            // TODO: display items starting from the bottom
//
//            if (ImGui::SmallButton("Add Dummy Text")) { AddLog("%d some text", Items.Size); AddLog("some more text"); AddLog("display very important message here!"); } ImGui::SameLine();
//            if (ImGui::SmallButton("Add Dummy Error")) AddLog("[error] something went wrong"); ImGui::SameLine();
//            if (ImGui::SmallButton("Clear")) ClearLog(); ImGui::SameLine();
//            if (ImGui::SmallButton("Scroll to bottom")) ScrollToBottom = true;
//            //static float t = 0.0f; if (ImGui::GetTime() - t > 0.02f) { t = ImGui::GetTime(); AddLog("Spam %f", t); }
//
//            ImGui::Separator();
//
//            ImGui::PushStyleVar(ImGuiStyleVar_FramePadding, ImVec2(0,0));
//            static ImGuiTextFilter filter;
//            filter.Draw("Filter (\"incl,-excl\") (\"error\")", 180);
//            ImGui::PopStyleVar();
//            ImGui::Separator();
//
//            ImGui::BeginChild("ScrollingRegion", ImVec2(0,-ImGui::GetItemsLineHeightWithSpacing()), false, ImGuiWindowFlags_HorizontalScrollbar);
//            if (ImGui::BeginPopupContextWindow())
//            {
//                if (ImGui::Selectable("Clear")) ClearLog();
//                ImGui::EndPopup();
//            }
//
//            // Display every line as a separate entry so we can change their color or add custom widgets. If you only want raw text you can use ImGui::TextUnformatted(log.begin(), log.end());
//            // NB- if you have thousands of entries this approach may be too inefficient and may require user-side clipping to only process visible items.
//            // You can seek and display only the lines that are visible using the ImGuiListClipper helper, if your elements are evenly spaced and you have cheap random access to the elements.
//            // To use the clipper we could replace the 'for (int i = 0; i < Items.Size; i++)' loop with:
//            //     ImGuiListClipper clipper(Items.Size);
//            //     while (clipper.Step())
//            //         for (int i = clipper.DisplayStart; i < clipper.DisplayEnd; i++)
//            // However take note that you can not use this code as is if a filter is active because it breaks the 'cheap random-access' property. We would need random-access on the post-filtered list.
//            // A typical application wanting coarse clipping and filtering may want to pre-compute an array of indices that passed the filtering test, recomputing this array when user changes the filter,
//            // and appending newly elements as they are inserted. This is left as a task to the user until we can manage to improve this example code!
//            // If your items are of variable size you may want to implement code similar to what ImGuiListClipper does. Or split your data into fixed height items to allow random-seeking into your list.
//            ImGui::PushStyleVar(ImGuiStyleVar_ItemSpacing, ImVec2(4,1)); // Tighten spacing
//            for (int i = 0; i < Items.Size; i++)
//            {
//                const char* item = Items[i];
//                if (!filter.PassFilter(item))
//                    continue;
//                ImVec4 col = ImVec4(1.0f,1.0f,1.0f,1.0f); // A better implementation may store a type per-item. For the sample let's just parse the text.
//                if (strstr(item, "[error]")) col = ImColor(1.0f,0.4f,0.4f,1.0f);
//                else if (strncmp(item, "# ", 2) == 0) col = ImColor(1.0f,0.78f,0.58f,1.0f);
//                ImGui::PushStyleColor(ImGuiCol_Text, col);
//                ImGui::TextUnformatted(item);
//                ImGui::PopStyleColor();
//            }
//            if (ScrollToBottom)
//                ImGui::SetScrollHere();
//            ScrollToBottom = false;
//            ImGui::PopStyleVar();
//            ImGui::EndChild();
//            ImGui::Separator();
//
//            // Command-line
//            if (ImGui::InputText("Input", InputBuf, IM_ARRAYSIZE(InputBuf), ImGuiInputTextFlags_EnterReturnsTrue|ImGuiInputTextFlags_CallbackCompletion|ImGuiInputTextFlags_CallbackHistory, &TextEditCallbackStub, (void*)this))
//            {
//                char* input_end = InputBuf+strlen(InputBuf);
//                while (input_end > InputBuf && input_end[-1] == ' ') input_end--; *input_end = 0;
//                if (InputBuf[0])
//                    ExecCommand(InputBuf);
//                strcpy(InputBuf, "");
//            }
//
//            // Demonstrate keeping auto focus on the input box
//            if (ImGui::IsItemHovered() || (ImGui::IsRootWindowOrAnyChildFocused() && !ImGui::IsAnyItemActive() && !ImGui::IsMouseClicked(0)))
//                ImGui::SetKeyboardFocusHere(-1); // Auto focus previous widget
//
//            ImGui::End();
        }
//
//        void    ExecCommand(const char* command_line)
//        {
//            AddLog("# %s\n", command_line);
//
//            // Insert into history. First find match and delete it so it can be pushed to the back. This isn't trying to be smart or optimal.
//            HistoryPos = -1;
//            for (int i = History.Size-1; i >= 0; i--)
//            if (Stricmp(History[i], command_line) == 0)
//            {
//                free(History[i]);
//                History.erase(History.begin() + i);
//                break;
//            }
//            History.push_back(Strdup(command_line));
//
//            // Process command
//            if (Stricmp(command_line, "CLEAR") == 0)
//            {
//                ClearLog();
//            }
//            else if (Stricmp(command_line, "HELP") == 0)
//            {
//                AddLog("Commands:");
//                for (int i = 0; i < Commands.Size; i++)
//                AddLog("- %s", Commands[i]);
//            }
//            else if (Stricmp(command_line, "HISTORY") == 0)
//            {
//                for (int i = History.Size >= 10 ? History.Size - 10 : 0; i < History.Size; i++)
//                AddLog("%3d: %s\n", i, History[i]);
//            }
//            else
//            {
//                AddLog("Unknown command: '%s'\n", command_line);
//            }
//        }
//
//        static int TextEditCallbackStub(ImGuiTextEditCallbackData* data) // In C++11 you are better off using lambdas for this sort of forwarding callbacks
//        {
//            ExampleAppConsole* console = (ExampleAppConsole*)data->UserData;
//            return console->TextEditCallback(data);
//        }
//
//        int     TextEditCallback(ImGuiTextEditCallbackData* data)
//        {
//            //AddLog("cursor: %d, selection: %d-%d", data->CursorPos, data->SelectionStart, data->SelectionEnd);
//            switch (data->EventFlag)
//            {
//                case ImGuiInputTextFlags_CallbackCompletion:
//                {
//                    // Example of TEXT COMPLETION
//
//                    // Locate beginning of current word
//                    const char* word_end = data->Buf + data->CursorPos;
//                    const char* word_start = word_end;
//                    while (word_start > data->Buf)
//                    {
//                        const char c = word_start[-1];
//                        if (c == ' ' || c == '\t' || c == ',' || c == ';')
//                            break;
//                        word_start--;
//                    }
//
//                    // Build a list of candidates
//                    ImVector<const char*> candidates;
//                    for (int i = 0; i < Commands.Size; i++)
//                    if (Strnicmp(Commands[i], word_start, (int)(word_end-word_start)) == 0)
//                        candidates.push_back(Commands[i]);
//
//                    if (candidates.Size == 0)
//                    {
//                        // No match
//                        AddLog("No match for \"%.*s\"!\n", (int)(word_end-word_start), word_start);
//                    }
//                    else if (candidates.Size == 1)
//                        {
//                            // Single match. Delete the beginning of the word and replace it entirely so we've got nice casing
//                            data->DeleteChars((int)(word_start-data->Buf), (int)(word_end-word_start));
//                            data->InsertChars(data->CursorPos, candidates[0]);
//                            data->InsertChars(data->CursorPos, " ");
//                        }
//                    else
//                    {
//                        // Multiple matches. Complete as much as we can, so inputing "C" will complete to "CL" and display "CLEAR" and "CLASSIFY"
//                        int match_len = (int)(word_end - word_start);
//                        for (;;)
//                        {
//                            int c = 0;
//                            bool all_candidates_matches = true;
//                            for (int i = 0; i < candidates.Size && all_candidates_matches; i++)
//                            if (i == 0)
//                                c = toupper(candidates[i][match_len]);
//                            else if (c == 0 || c != toupper(candidates[i][match_len]))
//                                all_candidates_matches = false;
//                            if (!all_candidates_matches)
//                                break;
//                            match_len++;
//                        }
//
//                        if (match_len > 0)
//                            {
//                                data->DeleteChars((int)(word_start - data->Buf), (int)(word_end-word_start));
//                                data->InsertChars(data->CursorPos, candidates[0], candidates[0] + match_len);
//                            }
//
//                        // List matches
//                        AddLog("Possible matches:\n");
//                        for (int i = 0; i < candidates.Size; i++)
//                        AddLog("- %s\n", candidates[i]);
//                    }
//
//                    break;
//                }
//                case ImGuiInputTextFlags_CallbackHistory:
//                {
//                    // Example of HISTORY
//                    const int prev_history_pos = HistoryPos;
//                    if (data->EventKey == ImGuiKey_UpArrow)
//                    {
//                        if (HistoryPos == -1)
//                            HistoryPos = History.Size - 1;
//                        else if (HistoryPos > 0)
//                            HistoryPos--;
//                    }
//                    else if (data->EventKey == ImGuiKey_DownArrow)
//                    {
//                        if (HistoryPos != -1)
//                            if (++HistoryPos >= History.Size)
//                                HistoryPos = -1;
//                    }
//
//                    // A better implementation would preserve the data on the current input line along with cursor position.
//                    if (prev_history_pos != HistoryPos)
//                        {
//                            data->CursorPos = data->SelectionStart = data->SelectionEnd = data->BufTextLen = (int)snprintf(data->Buf, (size_t)data->BufSize, "%s", (HistoryPos >= 0) ? History[HistoryPos] : "");
//                            data->BufDirty = true;
//                        }
//                }
//            }
//            return 0;
//        }
    }

    /** Usage:
     *      static ExampleAppLog my_log;
     *      my_log.AddLog("Hello %d world\n", 123);
     *      my_log.Draw("title");   */
    class ExampleAppLog {

        val buf = StringBuilder()
        val filter = TextFilter()// TODO
        //        ImVector<int>       LineOffsets;        // Index to lines offset
        var scrollToBottom = false

        fun addLog(fmt: String) {
            buf.append(fmt)
            scrollToBottom = true
        }

        fun clear() = buf.setLength(0)

        fun draw(title: String, pOpen: BooleanArray? = null) = with(ImGui) {

            setNextWindowSize(Vec2(500, 400), SetCond.FirstUseEver)
            begin(title, pOpen)
            if (button("Clear")) clear()
            sameLine()
            val copy = button("Copy")
            sameLine()
            filter.draw("Filter", -100f)
            separator()
            beginChild("scrolling", Vec2(0, 0), false, WindowFlags.HorizontalScrollbar.i)
            if (copy) logToClipboard()

//      TODO      if (Filter.IsActive())
//            {
//                const char* buf_begin = Buf.begin()
//                const char* line = buf_begin
//                for (int line_no = 0; line != NULL; line_no++)
//                {
//                    const char* line_end = (line_no < LineOffsets.Size) ? buf_begin + LineOffsets[line_no] : NULL
//                    if (Filter.PassFilter(line, line_end))
//                        ImGui::TextUnformatted(line, line_end)
//                    line = line_end && line_end[1] ? line_end + 1 : NULL
//                }
//            }
//            else
            textUnformatted(buf.toString())

            if (scrollToBottom)
                setScrollHere(1f)
            scrollToBottom = false
            endChild()
            end()
        }
    }
}