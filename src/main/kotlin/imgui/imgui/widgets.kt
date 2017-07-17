package imgui.imgui

import glm_.vec2.Vec2
import imgui.ImGui.calcWrapWidthForPos
import imgui.ImGui.getCurrentWindow
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.renderTextWrapped
import imgui.internal.Rect
import imgui.Context as g


interface imgui_widgets {

    fun text(fmt: String) = textV(fmt)

    fun textV(fmt: String) {

        val window = getCurrentWindow()
        if (window.skipItems) return

        val textEnd = fmt.length
        textUnformatted(fmt, textEnd)
    }
//    IMGUI_API void          TextColored(const ImVec4& col, const char* fmt, ...) IM_PRINTFARGS(2);  // shortcut for PushStyleColor(ImGuiCol_Text, col); Text(fmt, ...); PopStyleColor();
//    IMGUI_API void          TextColoredV(const ImVec4& col, const char* fmt, va_list args);
//    IMGUI_API void          TextDisabled(const char* fmt, ...) IM_PRINTFARGS(1);                    // shortcut for PushStyleColor(ImGuiCol_Text, style.Colors[ImGuiCol_TextDisabled]); Text(fmt, ...); PopStyleColor();
//    IMGUI_API void          TextDisabledV(const char* fmt, va_list args);
//    IMGUI_API void          TextWrapped(const char* fmt, ...) IM_PRINTFARGS(1);                     // shortcut for PushTextWrapPos(0.0f); Text(fmt, ...); PopTextWrapPos();. Note that this won't work on an auto-resizing window if there's no other widgets to extend the window width, yoy may need to set a size using SetNextWindowSize().
//    IMGUI_API void          TextWrappedV(const char* fmt, va_list args);

    /** doesn't require null terminated string if 'text_end' is specified. no copy done to any bounded stack buffer,
     *  recommended for long chunks of text */
    fun textUnformatted(text: String, textEnd: Int = text.length) {

        val window = getCurrentWindow()
        if (window.skipItems) return

        var textBegin = 0

        val wrapPosX = window.dc.textWrapPos
        val wrapEnabled = wrapPosX >= 0f
        if (textEnd - textBegin > 2000 && !wrapEnabled) {
            /*  Long text!
                Perform manual coarse clipping to optimize for long multi-line text
                From this point we will only compute the width of lines that are visible. Optimization only available
                when word-wrapping is disabled.
                We also don't vertically center the text within the line full height, which is unlikely to matter
                because we are likely the biggest and only item on the line.    */
            TODO()
//            const char * line = text
//                    const float line_height = GetTextLineHeight()
//            const ImVec2 text_pos = window->DC.CursorPos+ImVec2(0.0f, window->DC.CurrentLineTextBaseOffset)
//            const ImRect clip_rect = window->ClipRect
//            ImVec2 text_size (0, 0)
//
//            if (text_pos.y <= clip_rect.Max.y) {
//                ImVec2 pos = text_pos
//
//                        // Lines to skip (can't skip when logging text)
//                        if (!g.LogEnabled) {
//                            int lines_skippable =(int)((clip_rect.Min.y - text_pos.y) / line_height)
//                            if (lines_skippable > 0) {
//                                int lines_skipped = 0
//                                while (line < text_end && lines_skipped < lines_skippable) {
//                                    const char * line_end = strchr (line, '\n')
//                                    if (!line_end)
//                                        line_end = text_end
//                                    line = line_end + 1
//                                    lines_skipped++
//                                }
//                                pos.y += lines_skipped * line_height
//                            }
//                        }
//
//                // Lines to render
//                if (line < text_end) {
//                    ImRect line_rect (pos, pos+ImVec2(FLT_MAX, line_height))
//                    while (line < text_end) {
//                        const char * line_end = strchr (line, '\n')
//                        if (IsClippedEx(line_rect, NULL, false))
//                            break
//
//                        const ImVec2 line_size = CalcTextSize(line, line_end, false)
//                        text_size.x = ImMax(text_size.x, line_size.x)
//                        RenderText(pos, line, line_end, false)
//                        if (!line_end)
//                            line_end = text_end
//                        line = line_end + 1
//                        line_rect.Min.y += line_height
//                        line_rect.Max.y += line_height
//                        pos.y += line_height
//                    }
//
//                    // Count remaining lines
//                    int lines_skipped = 0
//                    while (line < text_end) {
//                        const char * line_end = strchr (line, '\n')
//                        if (!line_end)
//                            line_end = text_end
//                        line = line_end + 1
//                        lines_skipped++
//                    }
//                    pos.y += lines_skipped * line_height
//                }
//
//                text_size.y += (pos - text_pos).y
//            }
//
//            ImRect bb (text_pos, text_pos+text_size)
//            ItemSize(bb)
//            ItemAdd(bb, NULL)
        } else {
            val wrapWidth = if (wrapEnabled) calcWrapWidthForPos(window.dc.cursorPos, wrapPosX) else 0f
            val textSize = calcTextSize(text, textEnd, false, wrapWidth)

            // Account of baseline offset
            val textPos = Vec2(window.dc.cursorPos.x, window.dc.cursorPos.y + window.dc.currentLineTextBaseOffset)
            val bb = Rect(textPos, textPos + textSize)
            itemSize(textSize)
            if (!itemAdd(bb, null)) return

            // Render (we don't hide text after ## in this end-user function)
            renderTextWrapped(bb.min, text, textEnd, wrapWidth)
        }
    }

//    IMGUI_API void          LabelText(const char* label, const char* fmt, ...) IM_PRINTFARGS(2);    // display text+label aligned the same way as value+label widgets
//    IMGUI_API void          LabelTextV(const char* label, const char* fmt, va_list args);
//    IMGUI_API void          Bullet();                                                               // draw a small circle and keep the cursor on the same line. advance cursor x position by GetTreeNodeToLabelSpacing(), same distance that TreeNode() uses
//    IMGUI_API void          BulletText(const char* fmt, ...) IM_PRINTFARGS(1);                      // shortcut for Bullet()+Text()
//    IMGUI_API void          BulletTextV(const char* fmt, va_list args);
//    IMGUI_API bool          Button(const char* label, const ImVec2& size = ImVec2(0,0));            // button
//    IMGUI_API bool          SmallButton(const char* label);                                         // button with FramePadding=(0,0)
//    IMGUI_API bool          InvisibleButton(const char* str_id, const ImVec2& size);
//    IMGUI_API void          Image(ImTextureID user_texture_id, const ImVec2& size, const ImVec2& uv0 = ImVec2(0,0), const ImVec2& uv1 = ImVec2(1,1), const ImVec4& tint_col = ImVec4(1,1,1,1), const ImVec4& border_col = ImVec4(0,0,0,0));
//    IMGUI_API bool          ImageButton(ImTextureID user_texture_id, const ImVec2& size, const ImVec2& uv0 = ImVec2(0,0),  const ImVec2& uv1 = ImVec2(1,1), int frame_padding = -1, const ImVec4& bg_col = ImVec4(0,0,0,0), const ImVec4& tint_col = ImVec4(1,1,1,1));    // <0 frame_padding uses default frame padding settings. 0 for no padding
//    IMGUI_API bool          Checkbox(const char* label, bool* v);
//    IMGUI_API bool          CheckboxFlags(const char* label, unsigned int* flags, unsigned int flags_value);
//    IMGUI_API bool          RadioButton(const char* label, bool active);
//    IMGUI_API bool          RadioButton(const char* label, int* v, int v_button);
//    IMGUI_API bool          Combo(const char* label, int* current_item, const char* const* items, int items_count, int height_in_items = -1);
//    IMGUI_API bool          Combo(const char* label, int* current_item, const char* items_separated_by_zeros, int height_in_items = -1);      // separate items with \0, end item-list with \0\0
//    IMGUI_API bool          Combo(const char* label, int* current_item, bool (*items_getter)(void* data, int idx, const char** out_text), void* data, int items_count, int height_in_items = -1);
//    IMGUI_API bool          ColorButton(const ImVec4& col, bool small_height = false, bool outline_border = true);
//    IMGUI_API bool          ColorEdit3(const char* label, float col[3]);                            // Hint: 'float col[3]' function argument is same as 'float* col'. You can pass address of first element out of a contiguous set, e.g. &myvector.x
//    IMGUI_API bool          ColorEdit4(const char* label, float col[4], bool show_alpha = true);    // "
//    IMGUI_API void          ColorEditMode(ImGuiColorEditMode mode);                                 // FIXME-OBSOLETE: This is inconsistent with most of the API and will be obsoleted/replaced.
//    IMGUI_API void          PlotLines(const char* label, const float* values, int values_count, int values_offset = 0, const char* overlay_text = NULL, float scale_min = FLT_MAX, float scale_max = FLT_MAX, ImVec2 graph_size = ImVec2(0,0), int stride = sizeof(float));
//    IMGUI_API void          PlotLines(const char* label, float (*values_getter)(void* data, int idx), void* data, int values_count, int values_offset = 0, const char* overlay_text = NULL, float scale_min = FLT_MAX, float scale_max = FLT_MAX, ImVec2 graph_size = ImVec2(0,0));
//    IMGUI_API void          PlotHistogram(const char* label, const float* values, int values_count, int values_offset = 0, const char* overlay_text = NULL, float scale_min = FLT_MAX, float scale_max = FLT_MAX, ImVec2 graph_size = ImVec2(0,0), int stride = sizeof(float));
//    IMGUI_API void          PlotHistogram(const char* label, float (*values_getter)(void* data, int idx), void* data, int values_count, int values_offset = 0, const char* overlay_text = NULL, float scale_min = FLT_MAX, float scale_max = FLT_MAX, ImVec2 graph_size = ImVec2(0,0));
//    IMGUI_API void          ProgressBar(float fraction, const ImVec2& size_arg = ImVec2(-1,0), const char* overlay = NULL);
}