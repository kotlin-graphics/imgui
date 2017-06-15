package imgui.internal

import imgui.WindowFlags_
import imgui.has
import jdk.internal.org.objectweb.asm.Opcodes.NULL
import imgui.Context as g

// We should always have a CurrentWindow in the stack (there is an implicit "Debug" window)
// If this ever crash because g.CurrentWindow is NULL it means that either
// - ImGui::NewFrame() has never been called, which is illegal.
// - You are calling ImGui functions after ImGui::Render() and before the next ImGui::NewFrame(), which is also illegal.
//inline    ImGuiWindow*  GetCurrentWindowRead()      { ImGuiContext& g = *GImGui; return g.CurrentWindow; }
//inline    ImGuiWindow*  GetCurrentWindow()          { ImGuiContext& g = *GImGui; g.CurrentWindow->Accessed = true; return g.CurrentWindow; }
//IMGUI_API ImGuiWindow*  GetParentWindow();

fun findWindowByName(name: String): Window? {
    // FIXME-OPT: Store sorted hashes -> pointers so we can do a bissection in a contiguous block
    val id = hash(name, 0)
    return g.windows.firstOrNull { it.id == id }
}

/** Moving window to front of display (which happens to be back of our sorted list) */
fun focusWindow(window: Window?) {

    // Always mark the window we passed as focused. This is used for keyboard interactions such as tabbing.
    g.focusedWindow = window

    // Passing NULL allow to disable keyboard focus
    if (window == null) return

    // And move its root window to the top of the pile
//    if (window.rootWindow) TODO check
    val window = window.rootWindow

    // Steal focus on active widgets
    if (window.flags has WindowFlags_.Popup) // FIXME: This statement should be unnecessary. Need further testing before removing it..
        if (g.activeId != 0 && g.activeIdWindow != null && g.activeIdWindow!!.rootWindow != window)
            clearActiveID()

    // Bring to front
    if ((window.flags has WindowFlags_.NoBringToFrontOnFocus) || g.windows.last() == window)
        return
    g.windows.remove(window)
    g.windows.add(window)
}

//IMGUI_API void          EndFrame();                 // Ends the ImGui frame. Automatically called by Render()! you most likely don't need to ever call that yourself directly. If you don't need to render you can call EndFrame() but you'll have wasted CPU already. If you don't need to render, don't create any windows instead!

fun setActiveID(id: Int, window: Window?) {
    g.activeId = id
    g.activeIdAllowOverlap = false
    g.activeIdIsJustActivated = true
    if (id != 0)
        g.activeIdIsAlive = true
    g.activeIdWindow = window
}

fun clearActiveID() = setActiveID(0, null)

//IMGUI_API void          SetHoveredID(ImGuiID id);

fun keepAliveID(id: Int) {
    if (g.activeId == id)
        g.activeIdIsAlive = true
}

//IMGUI_API void          ItemSize(const ImVec2& size, float text_offset_y = 0.0f);
//IMGUI_API void          ItemSize(const ImRect& bb, float text_offset_y = 0.0f);
//IMGUI_API bool          ItemAdd(const ImRect& bb, const ImGuiID* id);
//IMGUI_API bool          IsClippedEx(const ImRect& bb, const ImGuiID* id, bool clip_even_when_logged);
//IMGUI_API bool          IsHovered(const ImRect& bb, ImGuiID id, bool flatten_childs = false);
//IMGUI_API bool          FocusableItemRegister(ImGuiWindow* window, bool is_active, bool tab_stop = true);      // Return true if focus is requested
//IMGUI_API void          FocusableItemUnregister(ImGuiWindow* window);
//IMGUI_API ImVec2        CalcItemSize(ImVec2 size, float default_x, float default_y);
//IMGUI_API float         CalcWrapWidthForPos(const ImVec2& pos, float wrap_pos_x);
//
//IMGUI_API void          OpenPopupEx(const char* str_id, bool reopen_existing);
//
//// NB: All position are in absolute pixels coordinates (not window coordinates)
//// FIXME: All those functions are a mess and needs to be refactored into something decent. AVOID USING OUTSIDE OF IMGUI.CPP! NOT FOR PUBLIC CONSUMPTION.
//// We need: a sort of symbol library, preferably baked into font atlas when possible + decent text rendering helpers.
//IMGUI_API void          RenderText(ImVec2 pos, const char* text, const char* text_end = NULL, bool hide_text_after_hash = true);
//IMGUI_API void          RenderTextWrapped(ImVec2 pos, const char* text, const char* text_end, float wrap_width);
//IMGUI_API void          RenderTextClipped(const ImVec2& pos_min, const ImVec2& pos_max, const char* text, const char* text_end, const ImVec2* text_size_if_known, const ImVec2& align = ImVec2(0,0), const ImRect* clip_rect = NULL);
//IMGUI_API void          RenderFrame(ImVec2 p_min, ImVec2 p_max, ImU32 fill_col, bool border = true, float rounding = 0.0f);
//IMGUI_API void          RenderCollapseTriangle(ImVec2 pos, bool is_open, float scale = 1.0f);
//IMGUI_API void          RenderBullet(ImVec2 pos);
//IMGUI_API void          RenderCheckMark(ImVec2 pos, ImU32 col);
//IMGUI_API const char*   FindRenderedTextEnd(const char* text, const char* text_end = NULL); // Find the optional ## from which we stop displaying text.
//
//IMGUI_API bool          ButtonBehavior(const ImRect& bb, ImGuiID id, bool* out_hovered, bool* out_held, ImGuiButtonFlags flags = 0);
//IMGUI_API bool          ButtonEx(const char* label, const ImVec2& size_arg = ImVec2(0,0), ImGuiButtonFlags flags = 0);
//IMGUI_API bool          CloseButton(ImGuiID id, const ImVec2& pos, float radius);
//
//IMGUI_API bool          SliderBehavior(const ImRect& frame_bb, ImGuiID id, float* v, float v_min, float v_max, float power, int decimal_precision, ImGuiSliderFlags flags = 0);
//IMGUI_API bool          SliderFloatN(const char* label, float* v, int components, float v_min, float v_max, const char* display_format, float power);
//IMGUI_API bool          SliderIntN(const char* label, int* v, int components, int v_min, int v_max, const char* display_format);
//
//IMGUI_API bool          DragBehavior(const ImRect& frame_bb, ImGuiID id, float* v, float v_speed, float v_min, float v_max, int decimal_precision, float power);
//IMGUI_API bool          DragFloatN(const char* label, float* v, int components, float v_speed, float v_min, float v_max, const char* display_format, float power);
//IMGUI_API bool          DragIntN(const char* label, int* v, int components, float v_speed, int v_min, int v_max, const char* display_format);
//
//IMGUI_API bool          InputTextEx(const char* label, char* buf, int buf_size, const ImVec2& size_arg, ImGuiInputTextFlags flags, ImGuiTextEditCallback callback = NULL, void* user_data = NULL);
//IMGUI_API bool          InputFloatN(const char* label, float* v, int components, int decimal_precision, ImGuiInputTextFlags extra_flags);
//IMGUI_API bool          InputIntN(const char* label, int* v, int components, ImGuiInputTextFlags extra_flags);
//IMGUI_API bool          InputScalarEx(const char* label, ImGuiDataType data_type, void* data_ptr, void* step_ptr, void* step_fast_ptr, const char* scalar_format, ImGuiInputTextFlags extra_flags);
//IMGUI_API bool          InputScalarAsWidgetReplacement(const ImRect& aabb, const char* label, ImGuiDataType data_type, void* data_ptr, ImGuiID id, int decimal_precision);
//
//IMGUI_API bool          TreeNodeBehavior(ImGuiID id, ImGuiTreeNodeFlags flags, const char* label, const char* label_end = NULL);
//IMGUI_API bool          TreeNodeBehaviorIsOpen(ImGuiID id, ImGuiTreeNodeFlags flags = 0);                     // Consume previous SetNextTreeNodeOpened() data, if any. May return true when logging
//IMGUI_API void          TreePushRawID(ImGuiID id);
//
//IMGUI_API void          PlotEx(ImGuiPlotType plot_type, const char* label, float (*values_getter)(void* data, int idx), void* data, int values_count, int values_offset, const char* overlay_text, float scale_min, float scale_max, ImVec2 graph_size);
//
//IMGUI_API int           ParseFormatPrecision(const char* fmt, int default_value);
//IMGUI_API float         RoundScalar(float value, int decimal_precision);