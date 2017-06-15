package imgui

import gli.wasInit
import glm_.vec2.Vec2
import imgui.imgui.isKeyPressed
import imgui.internal.*
import imgui.Context as g

//static void             LogRenderedText(const ImVec2& ref_pos, const char* text, const char* text_end = NULL);
//
//static void             PushMultiItemsWidths(int components, float w_full = 0.0f);
//static float            GetDraggedColumnOffset(int column_index);
//
fun isKeyPressedMap(key: Key_, repeat: Boolean = true) = isKeyPressed(key, repeat)

fun getDefaultFont() = IO.fontDefault ?: IO.fonts.fonts[0]
fun setCurrentFont(font: Font) {
    assert(font.isLoaded)    // Font Atlas not created. Did you call io.Fonts->GetTexDataAsRGBA32 / GetTexDataAsAlpha8 ?
    assert(font.scale > 0.0f)
    g.font = font
    g.fontBaseSize = IO.fontGlobalScale * g.font.fontSize * g.font.scale
    g.fontSize = if (wasInit { g.currentWindow }) g.currentWindow!!.calcFontSize() else 0f
    g.fontTexUvWhitePixel = g.font.containerAtlas.texUvWhitePixel
}

//-----------------------------------------------------------------------------
// Internal API exposed in imgui_internal.h
//-----------------------------------------------------------------------------

fun setCurrentWindow(window: Window?) {
    g.currentWindow = window
    window?.let { g.fontSize = it.calcFontSize() }
}

fun setWindowScrollY(window: Window, newScrollY: Float) {
    window.dc.cursorMaxPos.y += window.scroll.y
    window.scroll.y = newScrollY
    window.dc.cursorMaxPos.y -= window.scroll.y
}
//static void             SetWindowPos(ImGuiWindow* window, const ImVec2& pos, ImGuiSetCond cond);
//static void             SetWindowSize(ImGuiWindow* window, const ImVec2& size, ImGuiSetCond cond);
//static void             SetWindowCollapsed(ImGuiWindow* window, bool collapsed, ImGuiSetCond cond);
/** Find window given position, search front-to-back
FIXME: Note that we have a lag here because WindowRectClipped is updated in Begin() so windows moved by user via
SetWindowPos() and not SetNextWindowPos() will have that rectangle lagging by a frame at the time
FindHoveredWindow() is called, aka before the next Begin(). Moving window thankfully isn't affected.    */
fun findHoveredWindow(pos: Vec2, excludingChilds: Boolean): Window? {
    for (i in g.windows.size - 1 downTo 0) {
        val window = g.windows[i]
        if (!window.active)
            continue
        if (window.flags has WindowFlags_.NoInputs)
            continue
        if (excludingChilds && window.flags has WindowFlags_.ChildWindow)
            continue

        // Using the clipped AABB so a child window will typically be clipped by its parent.
        val bb = Rect(window.windowRectClipped.min - Style.touchExtraPadding, window.windowRectClipped.max + Style.touchExtraPadding)
        if (bb contains pos)
            return window
    }
    return null
}

fun createNewWindow(name: String, size: Vec2, flags: Int): Window {

    // Create window the first time
    val window = Window(name)
    window.flags = flags

    if (flags has WindowFlags_.NoSavedSettings) {
        // User can disable loading and saving of settings. Tooltip and child windows also don't store settings.
        window.sizeFull = size
        window.size = size
    } else {
        /*  Retrieve settings from .ini file
            Use SetWindowPos() or SetNextWindowPos() with the appropriate condition flag to change the initial position
            of a window.    */
        window.posF put 60
        window.pos put window.posF

        var settings = findWindowSettings(name)
        if (settings == null)
            settings = addWindowSettings(name)
        else {
            window.setWindowPosAllowFlags = window.setWindowPosAllowFlags and SetCond_.FirstUseEver.i.inv()
            window.setWindowSizeAllowFlags = window.setWindowSizeAllowFlags and SetCond_.FirstUseEver.i.inv()
            window.setWindowCollapsedAllowFlags = window.setWindowCollapsedAllowFlags and SetCond_.FirstUseEver.i.inv()
        }

        if (settings.pos.x != Float.MAX_VALUE) {
            window.posF = settings.pos
            window.pos put window.posF
            window.collapsed = settings.collapsed
        }

        if (lengthSqr(settings.size) > 0.00001f && flags hasnt WindowFlags_.NoResize)
            size put settings.size
        window.sizeFull = size
        window.size = size
    }

    if (flags has WindowFlags_.AlwaysAutoResize) {
        window.autoFitFrames put 2
        window.autoFitOnlyGrows = false
    } else {
        if (window.size.x <= 0.0f)
            window.autoFitFrames.x = 2
        if (window.size.y <= 0.0f)
            window.autoFitFrames.y = 2
        window.autoFitOnlyGrows = window.autoFitFrames.x > 0 || window.autoFitFrames.y > 0
    }

    if (flags has WindowFlags_.NoBringToFrontOnFocus)
        g.windows.add(0, window) // Quite slow but rare and only once
    else
        g.windows.add(window)
    return window
}

//static inline bool      IsWindowContentHoverable(ImGuiWindow* window);
//static void             ClearSetNextWindowData();

/** Save and compare stack sizes on Begin()/End() to detect usage errors    */
fun checkStacksSize(window: Window, write: Boolean) {
    /*  NOT checking: DC.ItemWidth, DC.AllowKeyboardFocus, DC.ButtonRepeat, DC.TextWrapPos (per window) to allow user to
        conveniently push once and not pop (they are cleared on Begin)  */
    val backup = window.dc.stackSizesBackup
    var ptr = 0

    run {
        // Too few or too many PopID()/TreePop()
        val current = window.idStack.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] == current, { "PushID/PopID or TreeNode/TreePop Mismatch!" })
        ptr++
    }
    run {
        // Too few or too many EndGroup()
        val current = window.dc.groupStack.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] == current, { "BeginGroup/EndGroup Mismatch!" })
        ptr++
    }
    run {
        // Too few or too many EndMenu()/EndPopup()
        val current = g.currentPopupStack.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] == current, { "BeginMenu/EndMenu or BeginPopup/EndPopup Mismatch" })
        ptr++
    }
    run {
        // Too few or too many PopStyleColor()
        val current = g.colorModifiers.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] == current, { "PushStyleColor/PopStyleColor Mismatch!" })
        ptr++
    }
    run {
        // Too few or too many PopStyleVar()
        val current = g.styleModifiers.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] == current, { "PushStyleVar/PopStyleVar Mismatch!" })
        ptr++
    }
    run {
        // Too few or too many PopFont()
        val current = g.fontStack.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] == current, { "PushFont/PopFont Mismatch!" })
        ptr++
    }
    assert(ptr == window.dc.stackSizesBackup.size)
}

//static void             Scrollbar(ImGuiWindow* window, bool horizontal);
//
//static void             AddDrawListToRenderList(ImVector<ImDrawList*>& out_render_list, ImDrawList* draw_list);
//static void             AddWindowToRenderList(ImVector<ImDrawList*>& out_render_list, ImGuiWindow* window);
//static void             AddWindowToSortedBuffer(ImVector<ImGuiWindow*>& out_sorted_windows, ImGuiWindow* window);
//

fun findWindowSettings(name: String): IniData? {
    val id = hash(name, 0)
    return g.settings.firstOrNull { it.id == id }
}

fun addWindowSettings(name: String): IniData {
    val ini = IniData()
    g.settings.add(ini)
    ini.name = name
    ini.id = hash(name, 0)
    ini.collapsed = false
    ini.pos = Vec2(Float.MAX_VALUE)
    ini.size = Vec2()
    return ini
}

//static void             LoadIniSettingsFromDisk(const char* ini_filename);
//static void             SaveIniSettingsToDisk(const char* ini_filename);
fun markIniSettingsDirty() {
    if (g.settingsDirtyTimer <= 0f)
        g.settingsDirtyTimer = IO.iniSavingRate
}

//
//static void             PushColumnClipRect(int column_index = -1);
//static ImRect           GetVisibleRect();
//
//static bool             BeginPopupEx(const char* str_id, ImGuiWindowFlags extra_flags);

fun closeInactivePopups() {

    if (g.openPopupStack.empty())
        return

    /*  When popups are stacked, clicking on a lower level popups puts focus back to it and close popups above it.
        Don't close our own child popup windows */
    var n = 0
    if (g.focusedWindow != null)
        while (n < g.openPopupStack.size) {
            val popup = g.openPopupStack[n]
            if (popup.window == null)
                continue
            assert(popup.window!!.flags has WindowFlags_.Popup)
            if (popup.window!!.flags has WindowFlags_.ChildWindow)
                continue

            var hasFocus = false
            var m = n
            while (m < g.openPopupStack.size && !hasFocus) {
                hasFocus = g.openPopupStack[m].window != null && g.openPopupStack[m].window!!.rootWindow == g.focusedWindow!!.rootWindow
                m++
            }
            if (!hasFocus)
                break
            n++
        }

    if (n < g.openPopupStack.size)   // This test is not required but it allows to set a useful breakpoint on the line below
        TODO() //g.OpenPopupStack.resize(n)
}

//static void             ClosePopupToLevel(int remaining);
//static void             ClosePopup(ImGuiID id);
//static bool             IsPopupOpen(ImGuiID id);
fun getFrontMostModalRootWindow(): Window? {
    for (n in g.openPopupStack.size - 1 downTo 0) {
        val frontMostPopup = g.openPopupStack[n].window
        if (frontMostPopup != null && frontMostPopup.flags has WindowFlags_.Modal)
            return frontMostPopup
    }
    return null
}
//static ImVec2           FindBestPopupWindowPos(const ImVec2& base_pos, const ImVec2& size, int* last_dir, const ImRect& rect_to_avoid);
//
//static bool             InputTextFilterCharacter(unsigned int* p_char, ImGuiInputTextFlags flags, ImGuiTextEditCallback callback, void* user_data);
//static int              InputTextCalcTextLenAndLineCount(const char* text_begin, const char** out_text_end);
//static ImVec2           InputTextCalcTextSizeW(const ImWchar* text_begin, const ImWchar* text_end, const ImWchar** remaining = NULL, ImVec2* out_offset = NULL, bool stop_on_new_line = false);
//
//static inline void      DataTypeFormatString(ImGuiDataType data_type, void* data_ptr, const char* display_format, char* buf, int buf_size);
//static inline void      DataTypeFormatString(ImGuiDataType data_type, void* data_ptr, int decimal_precision, char* buf, int buf_size);
//static void             DataTypeApplyOp(ImGuiDataType data_type, int op, void* value1, const void* value2);
//static bool             DataTypeApplyOpFromText(const char* buf, const char* initial_value_buf, ImGuiDataType data_type, void* data_ptr, const char* scalar_format);

//-----------------------------------------------------------------------------
// Platform dependent default implementations
//-----------------------------------------------------------------------------

//static const char*      GetClipboardTextFn_DefaultImpl(void* user_data);
//static void             SetClipboardTextFn_DefaultImpl(void* user_data, const char* text);
//static void             ImeSetInputScreenPosFn_DefaultImpl(int x, int y);