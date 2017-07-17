package imgui.imgui

import glm_.f
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.COL32_A_SHIFT
import imgui.COL32_B_SHIFT
import imgui.COL32_G_SHIFT
import imgui.COL32_R_SHIFT
import imgui.ImGui.findRenderedTextEnd
import imgui.internal.saturate
import imgui.Context as g

//IMGUI_API bool          IsItemHovered();                                                    // was the last item hovered by mouse?
//IMGUI_API bool          IsItemHoveredRect();                                                // was the last item hovered by mouse? even if another item is active or window is blocked by popup while we are hovering this
//IMGUI_API bool          IsItemActive();                                                     // was the last item active? (e.g. button being held, text field being edited- items that don't interact will always return false)
//IMGUI_API bool          IsItemClicked(int mouse_button = 0);                                // was the last item clicked? (e.g. button/node just clicked on)
//IMGUI_API bool          IsItemVisible();                                                    // was the last item visible? (aka not out of sight due to clipping/scrolling.)
//IMGUI_API bool          IsAnyItemHovered();
//IMGUI_API bool          IsAnyItemActive();
//IMGUI_API ImVec2        GetItemRectMin();                                                   // get bounding rect of last item in screen space
//IMGUI_API ImVec2        GetItemRectMax();                                                   // "
//IMGUI_API ImVec2        GetItemRectSize();                                                  // "
//IMGUI_API void          SetItemAllowOverlap();                                              // allow last item to be overlapped by a subsequent item. sometimes useful with invisible buttons, selectables, etc. to catch unused area.
//IMGUI_API bool          IsWindowHovered();                                                  // is current window hovered and hoverable (not blocked by a popup) (differentiate child windows from each others)
//IMGUI_API bool          IsWindowFocused();                                                  // is current window focused
//IMGUI_API bool          IsRootWindowFocused();                                              // is current root window focused (root = top-most parent of a child, otherwise self)
//IMGUI_API bool          IsRootWindowOrAnyChildFocused();                                    // is current root window or any of its child (including current window) focused
//IMGUI_API bool          IsRootWindowOrAnyChildHovered();                                    // is current root window or any of its child (including current window) hovered and hoverable (not blocked by a popup)
//IMGUI_API bool          IsRectVisible(const ImVec2& size);                                  // test if rectangle (of given size, starting from cursor position) is visible / not clipped.
//IMGUI_API bool          IsRectVisible(const ImVec2& rect_min, const ImVec2& rect_max);      // test if rectangle (in screen space) is visible / not clipped. to perform coarse clipping on user's side.
//IMGUI_API bool          IsPosHoveringAnyWindow(const ImVec2& pos);                          // is given position hovering any active imgui window
//IMGUI_API float         GetTime();

fun getFrameCount() = g.frameCount

//IMGUI_API const char*   GetStyleColName(ImGuiCol idx);
//IMGUI_API ImVec2        CalcItemRectClosestPoint(const ImVec2& pos, bool on_edge = false, float outward = +0.0f);   // utility to find the closest point the last item bounding rectangle edge. useful to visually link items

/** Calculate text size. Text can be multi-line. Optionally ignore text after a ## marker.
 *  CalcTextSize("") should return ImVec2(0.0f, GImGui->FontSize)   */
fun calcTextSize(text: String, textEnd: Int = text.length, hideTextAfterDoubleHash: Boolean = false, wrapWidth: Float = -1f): Vec2 {

    val textDisplayEnd =
            if (hideTextAfterDoubleHash)
                findRenderedTextEnd(text, textEnd)  // Hide anything after a '##' string
            else
                textEnd

    val font = g.font
    val fontSize = g.fontSize
    if (0 == textDisplayEnd)
        return Vec2(0f, fontSize)
    val textSize = font.calcTextSizeA(fontSize, Float.MAX_VALUE, wrapWidth, text, textDisplayEnd/*, NULL*/)

    // Cancel out character spacing for the last character of a line (it is baked into glyph->XAdvance field)
    val fontScale = fontSize / font.fontSize
    val characterSpacingX = 1f * fontScale
    if (textSize.x > 0f)
        textSize.x -= characterSpacingX
    textSize.x = (textSize.x + 0.95f).i.f

    return textSize
}

//IMGUI_API void          CalcListClipping(int items_count, float items_height, int* out_items_display_start, int* out_items_display_end);    // calculate coarse clipping for large list of evenly sized items. Prefer using the ImGuiListClipper higher-level helper if you can.
//
//IMGUI_API bool          BeginChildFrame(ImGuiID id, const ImVec2& size, ImGuiWindowFlags extra_flags = 0);	// helper to create a child window / scrolling region that looks like a normal widget frame
//IMGUI_API void          EndChildFrame();
//
//IMGUI_API ImVec4        ColorConvertU32ToFloat4(ImU32 in);
fun colorConvertFloat4ToU32(color: Vec4): Int {
    var out = F32_TO_INT8_SAT(color.x) shl COL32_R_SHIFT
    out = out or (F32_TO_INT8_SAT(color.y) shl COL32_G_SHIFT)
    out = out or (F32_TO_INT8_SAT(color.z) shl COL32_B_SHIFT)
    return out or (F32_TO_INT8_SAT(color.w) shl COL32_A_SHIFT)
}

//IMGUI_API void          ColorConvertRGBtoHSV(float r, float g, float b, float& out_h, float& out_s, float& out_v);
//IMGUI_API void          ColorConvertHSVtoRGB(float h, float s, float v, float& out_r, float& out_g, float& out_b);

/** Saturated, always output 0..255 */
fun F32_TO_INT8_SAT(_val: Float) = (saturate(_val) * 255f + 0.5f).i