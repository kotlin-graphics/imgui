package imgui.imgui

import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.ColorEditMode
import imgui.ImGui.beginGroup
import imgui.ImGui.buttonBehavior
import imgui.ImGui.buttonEx
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcWrapWidthForPos
import imgui.ImGui.currentWindow
import imgui.ImGui.dragInt
import imgui.ImGui.endGroup
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.getColorU32
import imgui.ImGui.inputText
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.ImGui.renderFrame
import imgui.ImGui.renderTextWrapped
import imgui.ImGui.sameLine
import imgui.ImGui.setTooltip
import imgui.InputTextFlags
import imgui.Style
import imgui.internal.*
import imgui.or
import java.util.*
import imgui.Context as g


interface imgui_widgets {

    fun text(fmt: String) {

        val window = currentWindow
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

        val window = currentWindow
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
            if (!itemAdd(bb)) return

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

    /** A little colored square. Return true when clicked.
     *  FIXME: May want to display/ignore the alpha component in the color display? Yet show it in the tooltip. */
    fun colorButton(col: Vec4, smallHeight: Boolean = false, outlineBorder: Boolean = true): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId("#colorbutton")
        val squareSize = g.fontSize
        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(squareSize + Style.framePadding.y * 2,
                squareSize + (if (smallHeight) 0f else Style.framePadding.y * 2)))
        itemSize(bb, if (smallHeight) 0f else Style.framePadding.y)
        if (!itemAdd(bb, id)) return false


        val (pressed, hovered, held) = buttonBehavior(bb, id)
        renderFrame(bb.min, bb.max, getColorU32(col), outlineBorder, Style.frameRounding)

        if (hovered)
            setTooltip("Color:\n(%.2f,%.2f,%.2f,%.2f)\n#%02X%02X%02X%02X", col.x, col.y, col.z, col.w,
                    F32_TO_INT8_SAT(col.x), F32_TO_INT8_SAT(col.y), F32_TO_INT8_SAT(col.z), F32_TO_INT8_SAT(col.w))

        return pressed
    }

    /** Hint: 'float col[3]' function argument is same as 'float* col'. You can pass address of first element out of a
     *  contiguous set, e.g. &myvector.x
     *  IMPORTANT: col must be a float array[3]     */
    fun colorEdit3(label: String, col: FloatArray): Boolean {

        val col4 = floatArrayOf(*col, 1f)
        valueChanged = colorEdit4(label, col4, false)
        col[0] = col4[0]
        col[1] = col4[1]
        col[2] = col4[2]
        return valueChanged
    }

    /** Hint: 'float col[4]' function argument is same as 'float* col'. You can pass address of first element out of a
     *  contiguous set, e.g. &myvector.x
     *  IMPORTANT: col must be a float array[4]     */
    fun colorEdit4(label: String, col: FloatArray, showAlpha: Boolean = true): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        id = window.getId(label)
        wFull = calcItemWidth()
        squareSz = (g.fontSize + Style.framePadding.y * 2f)

        val editMode =
                if (window.dc.colorEditMode == ColorEditMode.UserSelect || window.dc.colorEditMode == ColorEditMode.UserSelectShowButton)
                    ColorEditMode.of((g.colorEditModeStorage[id]?.i ?: 0) % 3)
                else window.dc.colorEditMode

        val f = col.copyOf()
        if (editMode == ColorEditMode.HSV)
            colorConvertRGBtoHSV(f, f)

        val i = IntArray(4, { F32_TO_INT8_UNBOUND(f[it]) })

        val components = if (showAlpha) 4 else 3
        var valueChanged = false

        beginGroup()
        pushId(label)

        hsv = editMode == ColorEditMode.HSV
        when (editMode) {

            ColorEditMode.RGB, ColorEditMode.HSV -> {
                // RGB/HSV 0..255 Sliders
                wItemsAll = wFull - (squareSz + Style.itemInnerSpacing.x)
                wItemOne = glm.max(1f, ((wItemsAll - Style.itemInnerSpacing.x * (components - 1)) / components.f).i.f)
                wItemLast = glm.max(1f, (wItemsAll - (wItemOne + Style.itemInnerSpacing.x) * (components - 1)).i.f)

                hidePrefix = wItemOne <= calcTextSize("M:999").x
                ids = listOf("##X", "##Y", "##Z", "##W")
                fmtTable = listOf(
                        listOf("%3.0f", "%3.0f", "%3.0f", "%3.0f"),
                        listOf("R:%3.0f", "G:%3.0f", "B:%3.0f", "A:%3.0f"),
                        listOf("H:%3.0f", "S:%3.0f", "V:%3.0f", "A:%3.0f"))
                fmt = if (hidePrefix) fmtTable[0] else if (hsv) fmtTable[2] else fmtTable[1]

                pushItemWidth(wItemOne)
                for (n in 0 until components) {
                    if (n > 0)
                        sameLine(0f, Style.itemInnerSpacing.x)
                    if (n + 1 == components)
                        pushItemWidth(wItemLast)
                    val int = intArrayOf(i[n])
                    valueChanged = valueChanged or dragInt(ids[n], int, 1f, 0, 255, fmt[n])
                    i[n] = int[0]
                }
                popItemWidth()
                popItemWidth()
            }

            ColorEditMode.HEX -> {
                // RGB Hexadecimal Input
                wSliderAll = wFull - squareSz
                val buf = CharArray(64)
                (if (showAlpha) "#%02X%02X%02X%02X".format(Style.locale, i[0], i[1], i[2], i[3])
                else "#%02X%02X%02X".format(Style.locale, i[0], i[1], i[2])).toCharArray(buf)
                pushItemWidth(wSliderAll - Style.itemInnerSpacing.x)
                if (inputText("##Text", buf, InputTextFlags.CharsHexadecimal or InputTextFlags.CharsUppercase)) {
                    valueChanged = valueChanged || true
                    var p = 0
                    while (buf[p] == '#' || buf[p].isSpace)
                        p++
                    i.fill(0)
                    String(buf, p, buf.strlen - p).scanHex(i, if (showAlpha) 4 else 3, 2)
                }
                popItemWidth()
            }
            else -> Unit
        }

        sameLine(0f, Style.itemInnerSpacing.x)

        val colDisplay = Vec4(col[0], col[1], col[2], 1.0f)
        if (colorButton(colDisplay))
            g.colorEditModeStorage[id] = ((editMode.i + 1) % 3).f // Don't set local copy of 'editMode' right away!

        // Recreate our own tooltip over's ColorButton() one because we want to display correct alpha here
        if (isItemHovered())
            setTooltip("Color:\n(%.2f,%.2f,%.2f,%.2f)\n#%02X%02X%02X%02X", col[0], col[1], col[2], col[3],
                    F32_TO_INT8_SAT(col[0]), F32_TO_INT8_SAT(col[1]), F32_TO_INT8_SAT(col[2]), F32_TO_INT8_SAT(col[3]))

        if (window.dc.colorEditMode == ColorEditMode.UserSelectShowButton) {
            sameLine(0f, Style.itemInnerSpacing.x)
            val buttonTitles = arrayOf("RGB", "HSV", "HEX")
            if (buttonEx(buttonTitles[editMode.i], Vec2(), ButtonFlags.DontClosePopups.i))
                g.colorEditModeStorage[id] = ((editMode.i + 1) % 3).f // Don't set local copy of 'editMode' right away!
        }

        val labelDisplayEnd = findRenderedTextEnd(label)
        if (labelDisplayEnd != 0) {
            sameLine(0f, if (window.dc.colorEditMode == ColorEditMode.UserSelectShowButton) -1f else Style.itemInnerSpacing.x)
            textUnformatted(label, labelDisplayEnd)
        }

        // Convert back
        for (n in 0 until 4)
            f[n] = i[n] / 255f
        if (editMode == ColorEditMode.HSV)
            colorConvertHSVtoRGB(f, f)

        if (valueChanged) {
            col[0] = f[0]
            col[1] = f[1]
            col[2] = f[2]
            if (showAlpha)
                col[3] = f[3]
        }

        popId()
        endGroup()

        return valueChanged
    }
//    IMGUI_API void          ColorEditMode(ImGuiColorEditMode mode);                                 // FIXME-OBSOLETE: This is inconsistent with most of the API and will be obsoleted/replaced.
//    IMGUI_API void          PlotLines(const char* label, const float* values, int values_count, int values_offset = 0, const char* overlay_text = NULL, float scale_min = FLT_MAX, float scale_max = FLT_MAX, ImVec2 graph_size = ImVec2(0,0), int stride = sizeof(float));
//    IMGUI_API void          PlotLines(const char* label, float (*values_getter)(void* data, int idx), void* data, int values_count, int values_offset = 0, const char* overlay_text = NULL, float scale_min = FLT_MAX, float scale_max = FLT_MAX, ImVec2 graph_size = ImVec2(0,0));
//    IMGUI_API void          PlotHistogram(const char* label, const float* values, int values_count, int values_offset = 0, const char* overlay_text = NULL, float scale_min = FLT_MAX, float scale_max = FLT_MAX, ImVec2 graph_size = ImVec2(0,0), int stride = sizeof(float));
//    IMGUI_API void          PlotHistogram(const char* label, float (*values_getter)(void* data, int idx), void* data, int values_count, int values_offset = 0, const char* overlay_text = NULL, float scale_min = FLT_MAX, float scale_max = FLT_MAX, ImVec2 graph_size = ImVec2(0,0));
//    IMGUI_API void          ProgressBar(float fraction, const ImVec2& size_arg = ImVec2(-1,0), const char* overlay = NULL);

    companion object {
        // colorEdit
        var valueChanged = false
        var id = 0
        var wFull = 0f
        var squareSz = 0f
        var hsv = false
        var wItemsAll = 0f
        var wItemOne = 0f
        var wItemLast = 0f
        var wSliderAll = 0f
        var hidePrefix = false
        var ids = listOf<String>()
        var fmtTable = listOf<List<String>>()
        var fmt = listOf<String>()
    }
}