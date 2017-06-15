package imgui.imgui

import glm_.vec2.Vec2
import imgui.*
import imgui.internal.findWindowByName
import imgui.Context as g


/*  Push a new ImGui window to add widgets to:
        - A default window called "Debug" is automatically stacked at the beginning of every frame so you can use
            widgets without explicitly calling a Begin/End pair.
        - Begin/End can be called multiple times during the frame with the same window name to append content.
        - 'size_on_first_use' for a regular window denote the initial size for first-time creation (no saved data) and
            isn't that useful. Use SetNextWindowSize() prior to calling Begin() for more flexible window manipulation.
        - The window name is used as a unique identifier to preserve window information across frames (and save
            rudimentary information to the .ini file).
            You can use the "##" or "###" markers to use the same label with different id, or same id with different
            label. See documentation at the top of this file.
        - Return false when window is collapsed, so you can early out in your code. You always need to call ImGui::End()
            even if false is returned.
        - Passing 'bool* p_open' displays a Close button on the upper-right corner of the window, the pointed value will
            be set to false when the button is pressed.
        - Passing non-zero 'size' is roughly equivalent to calling SetNextWindowSize(size, ImGuiSetCond_FirstUseEver)
            prior to calling Begin().   */
fun begin(name: String, open: Boolean? = null, flags: Int = 0) = begin(name, open, Vec2(), -1.0f, flags)

/** OBSOLETE. this is the older/longer API. the extra parameters aren't very relevant. call SetNextWindowSize() instead
if you want to set a window size. For regular windows, 'size_on_first_use' only applies to the first time EVER the
window is created and probably not what you want! might obsolete this API eventually.   */
fun begin(name: String, open: Boolean?, sizeOnFirstUse: Vec2, bgAlpha: Float = -1.0f, flags: Int = 0): Pair<Boolean, Boolean> {

    assert(name.isNotEmpty())                        // Window name required
    assert(g.initialized)                       // Forgot to call ImGui::NewFrame()
    // Called ImGui::Render() or ImGui::EndFrame() and haven't called ImGui::NewFrame() again yet
    assert(g.frameCountEnded != g.frameCount)

    var flags = flags
    if (flags has WindowFlags_.NoInputs)
        flags = flags or WindowFlags_.NoMove or WindowFlags_.NoResize

    // Find or create
    var windowIsNew = false
    var window = findWindowByName(name)
    if (window == null) {
        window = createNewWindow(name, sizeOnFirstUse, flags)
        windowIsNew = true
    }

    val currentFrame = getFrameCount()
    val firstBeginOfTheFrame = window.lastFrameActive != currentFrame
    if (firstBeginOfTheFrame)
        window.flags = flags
    else
        flags = window.flags

    // Add to stack
    val parentWindow = g.currentWindowStack.lastOrNull()
    g.currentWindowStack.add(window)
    setCurrentWindow(window)
    checkStacksSize(window, true)
    assert(parentWindow != null || flags hasnt WindowFlags_.ChildWindow)

    // Not using !WasActive because the implicit "Debug" window would always toggle off->on
    var windowWasActive = window.lastFrameActive == currentFrame - 1
    if (flags has WindowFlags_.Popup)    {
        val popupRef = g.openPopupStack[g.currentPopupStack.size]
        windowWasActive = windowWasActive && window.popupId == popupRef.popupId
        windowWasActive = windowWasActive && window == popupRef.window
        popupRef.window = window
        g.currentPopupStack.add(popupRef)
        window.popupId = popupRef.popupId
    }

    val windowAppearingAfterBeingHidden = window.hiddenFrames == 1
}
//IMGUI_API void          End();                                                                                                                      // finish appending to current window, pop it off the window stack.
//IMGUI_API bool          BeginChild(const char* str_id, const ImVec2& size = ImVec2(0,0), bool border = false, ImGuiWindowFlags extra_flags = 0);    // begin a scrolling region. size==0.0f: use remaining window size, size<0.0f: use remaining window size minus abs(size). size>0.0f: fixed size. each axis can use a different mode, e.g. ImVec2(0,400).
//IMGUI_API bool          BeginChild(ImGuiID id, const ImVec2& size = ImVec2(0,0), bool border = false, ImGuiWindowFlags extra_flags = 0);            // "
//IMGUI_API void          EndChild();
//IMGUI_API ImVec2        GetContentRegionMax();                                              // current content boundaries (typically window boundaries including scrolling, or current column boundaries), in windows coordinates
//IMGUI_API ImVec2        GetContentRegionAvail();                                            // == GetContentRegionMax() - GetCursorPos()
//IMGUI_API float         GetContentRegionAvailWidth();                                       //
//IMGUI_API ImVec2        GetWindowContentRegionMin();                                        // content boundaries min (roughly (0,0)-Scroll), in window coordinates
//IMGUI_API ImVec2        GetWindowContentRegionMax();                                        // content boundaries max (roughly (0,0)+Size-Scroll) where Size can be override with SetNextWindowContentSize(), in window coordinates
//IMGUI_API float         GetWindowContentRegionWidth();                                      //
//IMGUI_API ImDrawList*   GetWindowDrawList();                                                // get rendering command-list if you want to append your own draw primitives
//IMGUI_API ImVec2        GetWindowPos();                                                     // get current window position in screen space (useful if you want to do your own drawing via the DrawList api)
//IMGUI_API ImVec2        GetWindowSize();                                                    // get current window size
//IMGUI_API float         GetWindowWidth();
//IMGUI_API float         GetWindowHeight();
//IMGUI_API bool          IsWindowCollapsed();
//IMGUI_API void          SetWindowFontScale(float scale);                                    // per-window font scale. Adjust IO.FontGlobalScale if you want to scale all windows
//
//IMGUI_API void          SetNextWindowPos(const ImVec2& pos, ImGuiSetCond cond = 0);         // set next window position. call before Begin()
//IMGUI_API void          SetNextWindowPosCenter(ImGuiSetCond cond = 0);                      // set next window position to be centered on screen. call before Begin()

/** set next window size. set axis to 0.0f to force an auto-fit on this axis. call before Begin()   */
fun setNextWindowSize(size: Vec2, cond: SetCond_? = null) {
    g.setNextWindowSizeVal = size
    g.setNextWindowSizeCond = cond ?: SetCond_.Always
}
//IMGUI_API void          SetNextWindowSizeConstraints(const ImVec2& size_min, const ImVec2& size_max, ImGuiSizeConstraintCallback custom_callback = NULL, void* custom_callback_data = NULL); // set next window size limits. use -1,-1 on either X/Y axis to preserve the current size. Use callback to apply non-trivial programmatic constraints.
//IMGUI_API void          SetNextWindowContentSize(const ImVec2& size);                       // set next window content size (enforce the range of scrollbars). set axis to 0.0f to leave it automatic. call before Begin()
//IMGUI_API void          SetNextWindowContentWidth(float width);                             // set next window content width (enforce the range of horizontal scrollbar). call before Begin()
//IMGUI_API void          SetNextWindowCollapsed(bool collapsed, ImGuiSetCond cond = 0);      // set next window collapsed state. call before Begin()
//IMGUI_API void          SetNextWindowFocus();                                               // set next window to be focused / front-most. call before Begin()
//IMGUI_API void          SetWindowPos(const ImVec2& pos, ImGuiSetCond cond = 0);             // (not recommended) set current window position - call within Begin()/End(). prefer using SetNextWindowPos(), as this may incur tearing and side-effects.
//IMGUI_API void          SetWindowSize(const ImVec2& size, ImGuiSetCond cond = 0);           // (not recommended) set current window size - call within Begin()/End(). set to ImVec2(0,0) to force an auto-fit. prefer using SetNextWindowSize(), as this may incur tearing and minor side-effects.
//IMGUI_API void          SetWindowCollapsed(bool collapsed, ImGuiSetCond cond = 0);          // (not recommended) set current window collapsed state. prefer using SetNextWindowCollapsed().
//IMGUI_API void          SetWindowFocus();                                                   // (not recommended) set current window to be focused / front-most. prefer using SetNextWindowFocus().
//IMGUI_API void          SetWindowPos(const char* name, const ImVec2& pos, ImGuiSetCond cond = 0);      // set named window position.
//IMGUI_API void          SetWindowSize(const char* name, const ImVec2& size, ImGuiSetCond cond = 0);    // set named window size. set axis to 0.0f to force an auto-fit on this axis.
//IMGUI_API void          SetWindowCollapsed(const char* name, bool collapsed, ImGuiSetCond cond = 0);   // set named window collapsed state
//IMGUI_API void          SetWindowFocus(const char* name);                                              // set named window to be focused / front-most. use NULL to remove focus.
//
//IMGUI_API float         GetScrollX();                                                       // get scrolling amount [0..GetScrollMaxX()]
//IMGUI_API float         GetScrollY();                                                       // get scrolling amount [0..GetScrollMaxY()]
//IMGUI_API float         GetScrollMaxX();                                                    // get maximum scrolling amount ~~ ContentSize.X - WindowSize.X
//IMGUI_API float         GetScrollMaxY();                                                    // get maximum scrolling amount ~~ ContentSize.Y - WindowSize.Y
//IMGUI_API void          SetScrollX(float scroll_x);                                         // set scrolling amount [0..GetScrollMaxX()]
//IMGUI_API void          SetScrollY(float scroll_y);                                         // set scrolling amount [0..GetScrollMaxY()]
//IMGUI_API void          SetScrollHere(float center_y_ratio = 0.5f);                         // adjust scrolling amount to make current cursor position visible. center_y_ratio=0.0: top, 0.5: center, 1.0: bottom.
//IMGUI_API void          SetScrollFromPosY(float pos_y, float center_y_ratio = 0.5f);        // adjust scrolling amount to make given position valid. use GetCursorPos() or GetCursorStartPos()+offset to get valid positions.
//IMGUI_API void          SetKeyboardFocusHere(int offset = 0);                               // focus keyboard on the next widget. Use positive 'offset' to access sub components of a multiple component widget. Use negative 'offset' to access previous widgets.
//IMGUI_API void          SetStateStorage(ImGuiStorage* tree);                                // replace tree state storage with our own (if you want to manipulate it yourself, typically clear subsection of it)
//IMGUI_API ImGuiStorage* GetStateStorage();