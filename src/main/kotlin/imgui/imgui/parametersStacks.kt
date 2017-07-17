package imgui.imgui

import imgui.Col
import imgui.Style
import imgui.ImGui.getCurrentWindow


interface imgui_parametersStacks {

// Parameters stacks (shared)
//IMGUI_API void          PushFont(ImFont* font);                                             // use NULL as a shortcut to push default font
//IMGUI_API void          PopFont();
//IMGUI_API void          PushStyleColor(ImGuiCol idx, const ImVec4& col);
//IMGUI_API void          PopStyleColor(int count = 1);
//IMGUI_API void          PushStyleVar(ImGuiStyleVar idx, float val);
//IMGUI_API void          PushStyleVar(ImGuiStyleVar idx, const ImVec2& val);
//IMGUI_API void          PopStyleVar(int count = 1);
//IMGUI_API ImFont*       GetFont();                                                          // get current font
//IMGUI_API float         GetFontSize();                                                      // get current font size (= height in pixels) of current font with current scale applied
//IMGUI_API ImVec2        GetFontTexUvWhitePixel();                                           // get UV coordinate for a while pixel, useful to draw custom shapes via the ImDrawList API
    /** retrieve given style color with style alpha applied and optional extra alpha multiplier */
    fun getColorU32(idx: Col, alphaMul: Float = 1f) = getColorU32(idx.i, alphaMul)

    fun getColorU32(idx: Int, alphaMul: Float = 1f): Int {
        val c = Style.colors[idx]
        c.w *= Style.alpha * alphaMul
        return colorConvertFloat4ToU32(c)
    }

    //IMGUI_API ImU32         GetColorU32(const ImVec4& col);                                     // retrieve given color with style alpha applied
//
//// Parameters stacks (current window)
//IMGUI_API void          PushItemWidth(float item_width);                                    // width of items for the common item+label case, pixels. 0.0f = default to ~2/3 of windows width, >0.0f: width in pixels, <0.0f align xx pixels to the right of window (so -1.0f always align width to the right side)
    fun popItemWidth() {
        with(getCurrentWindow()) {
            dc.itemWidthStack.pop()
            dc.itemWidth = if (dc.itemWidthStack.empty()) itemWidthDefault else dc.itemWidthStack.last()
        }
    }
//IMGUI_API float         CalcItemWidth();                                                    // width of item given pushed settings and current cursor position
//IMGUI_API void          PushTextWrapPos(float wrap_pos_x = 0.0f);                           // word-wrapping for Text*() commands. < 0.0f: no wrapping; 0.0f: wrap to end of window (or column); > 0.0f: wrap at 'wrap_pos_x' position in window local space
//IMGUI_API void          PopTextWrapPos();
//IMGUI_API void          PushAllowKeyboardFocus(bool v);                                     // allow focusing using TAB/Shift-TAB, enabled by default but you can disable it for certain widgets
//IMGUI_API void          PopAllowKeyboardFocus();
//IMGUI_API void          PushButtonRepeat(bool repeat);                                      // in 'repeat' mode, Button*() functions return repeated true in a typematic manner (uses io.KeyRepeatDelay/io.KeyRepeatRate for now). Note that you can call IsItemActive() after any Button() to tell if the button is held in the current frame.
//IMGUI_API void          PopButtonRepeat();

}