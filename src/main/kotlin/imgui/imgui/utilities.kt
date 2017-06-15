package imgui.imgui

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
//IMGUI_API ImVec2        CalcTextSize(const char* text, const char* text_end = NULL, bool hide_text_after_double_hash = false, float wrap_width = -1.0f);
//IMGUI_API void          CalcListClipping(int items_count, float items_height, int* out_items_display_start, int* out_items_display_end);    // calculate coarse clipping for large list of evenly sized items. Prefer using the ImGuiListClipper higher-level helper if you can.
//
//IMGUI_API bool          BeginChildFrame(ImGuiID id, const ImVec2& size, ImGuiWindowFlags extra_flags = 0);	// helper to create a child window / scrolling region that looks like a normal widget frame
//IMGUI_API void          EndChildFrame();
//
//IMGUI_API ImVec4        ColorConvertU32ToFloat4(ImU32 in);
//IMGUI_API ImU32         ColorConvertFloat4ToU32(const ImVec4& in);
//IMGUI_API void          ColorConvertRGBtoHSV(float r, float g, float b, float& out_h, float& out_s, float& out_v);
//IMGUI_API void          ColorConvertHSVtoRGB(float h, float s, float v, float& out_r, float& out_g, float& out_b);