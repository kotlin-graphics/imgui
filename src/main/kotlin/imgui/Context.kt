package imgui

import glm.vec2.Vec2


object Context {

    var initialized = false
    val io = IO
    val style = Style
//    ImFont*                 Font;                               // (Shortcut) == FontStack.empty() ? IO.Font : FontStack.back()
    /** (Shortcut) == FontBaseSize * g.CurrentWindow->FontWindowScale == window->FontSize() */
    var fontSize = 0f
    /** (Shortcut) == IO.FontGlobalScale * Font->Scale * Font->FontSize. Size of characters.    */
    var fontBaseSize = 0f
    /** (Shortcut) == Font->TexUvWhitePixel */
    var fontTexUvWhitePixel = Vec2()

    var Time = 0.0f
    var FrameCount = 0
    var FrameCountEnded = -1
    var FrameCountRendered = -1

    //    ImVector<ImGuiWindow*>  Windows;
//    ImVector<ImGuiWindow*>  WindowsSortBuffer;
//    ImGuiWindow*            CurrentWindow;                      // Being drawn into
//    ImVector<ImGuiWindow*>  CurrentWindowStack;
//    ImGuiWindow*            FocusedWindow;                      // Will catch keyboard inputs
//    ImGuiWindow*            HoveredWindow;                      // Will catch mouse inputs
//    ImGuiWindow*            HoveredRootWindow;                  // Will catch mouse inputs (for focus/move only)
//    ImGuiID                 HoveredId;                          // Hovered widget
//    bool                    HoveredIdAllowOverlap;
//    ImGuiID                 HoveredIdPreviousFrame;
//    ImGuiID                 ActiveId;                           // Active widget
//    ImGuiID                 ActiveIdPreviousFrame;
//    bool                    ActiveIdIsAlive;
//    bool                    ActiveIdIsJustActivated;            // Set at the time of activation for one frame
//    bool                    ActiveIdAllowOverlap;               // Set only by active widget
//    ImVec2                  ActiveIdClickOffset;                // Clicked offset from upper-left corner, if applicable (currently only set by ButtonBehavior)
//    ImGuiWindow*            ActiveIdWindow;
//    ImGuiWindow*            MovedWindow;                        // Track the child window we clicked on to move a window.
//    ImGuiID                 MovedWindowMoveId;                  // == MovedWindow->RootWindow->MoveId
//    ImVector<ImGuiIniData>  Settings;                           // .ini Settings
//    float                   SettingsDirtyTimer;                 // Save .ini Settings on disk when time reaches zero
//    ImVector<ImGuiColMod>   ColorModifiers;                     // Stack for PushStyleColor()/PopStyleColor()
//    ImVector<ImGuiStyleMod> StyleModifiers;                     // Stack for PushStyleVar()/PopStyleVar()
//    ImVector<ImFont*>       FontStack;                          // Stack for PushFont()/PopFont()
//    ImVector<ImGuiPopupRef> OpenPopupStack;                     // Which popups are open (persistent)
//    ImVector<ImGuiPopupRef> CurrentPopupStack;                  // Which level of BeginPopup() we are in (reset every frame)
//
//    // Storage for SetNexWindow** and SetNextTreeNode*** functions
//    ImVec2                  SetNextWindowPosVal;
//    ImVec2                  SetNextWindowSizeVal;
//    ImVec2                  SetNextWindowContentSizeVal;
//    bool                    SetNextWindowCollapsedVal;
//    ImGuiSetCond            SetNextWindowPosCond;
//    ImGuiSetCond            SetNextWindowSizeCond;
//    ImGuiSetCond            SetNextWindowContentSizeCond;
//    ImGuiSetCond            SetNextWindowCollapsedCond;
//    ImRect                  SetNextWindowSizeConstraintRect;           // Valid if 'SetNextWindowSizeConstraint' is true
//    ImGuiSizeConstraintCallback SetNextWindowSizeConstraintCallback;
//    void*                       SetNextWindowSizeConstraintCallbackUserData;
//    bool                    SetNextWindowSizeConstraint;
//    bool                    SetNextWindowFocus;
//    bool                    SetNextTreeNodeOpenVal;
//    ImGuiSetCond            SetNextTreeNodeOpenCond;
//
    // Render ----------------------------------------------------------------------------------------------------------
//    ImDrawData              RenderDrawData;                     // Main ImDrawData instance to pass render information to the user
//    ImVector<ImDrawList*>   RenderDrawLists[3];
//    float                   ModalWindowDarkeningRatio;
//    ImDrawList              OverlayDrawList;                    // Optional software render of mouse cursors, if io.MouseDrawCursor is set + a few debug overlays
//    ImGuiMouseCursor        MouseCursor;
    val mouseCursorData = Array(MouseCursor_.Count.i, { MouseCursorData() })

    // Widget state ----------------------------------------------------------------------------------------------------
//    ImGuiTextEditState      InputTextState;
//    ImFont                  InputTextPasswordFont;
//    ImGuiID                 ScalarAsInputTextId;                // Temporary text input when CTRL+clicking on a slider, etc.
//    ImGuiStorage            ColorEditModeStorage;               // Store user selection of color edit mode
//    float                   DragCurrentValue;                   // Currently dragged value, always float, not rounded by end-user precision settings
//    ImVec2                  DragLastMouseDelta;
//    float                   DragSpeedDefaultRatio;              // If speed == 0.0f, uses (max-min) * DragSpeedDefaultRatio
//    float                   DragSpeedScaleSlow;
//    float                   DragSpeedScaleFast;
//    ImVec2                  ScrollbarClickDeltaToGrabCenter;    // Distance between mouse and center of grab box, normalized in parent space. Use storage?
//    char                    Tooltip[1024];
//    char*                   PrivateClipboard;                   // If no custom clipboard handler is defined
//    ImVec2                  OsImePosRequest, OsImePosSet;       // Cursor position request & last passed to the OS Input Method Editor
//
//    // Logging
//    bool                    LogEnabled;
//    FILE*                   LogFile;                            // If != NULL log to stdout/ file
//    ImGuiTextBuffer*        LogClipboard;                       // Else log to clipboard. This is pointer so our GImGui static constructor doesn't call heap allocators.
//    int                     LogStartDepth;
//    int                     LogAutoExpandMaxDepth;
//
//    // Misc
//    float                   FramerateSecPerFrame[120];          // calculate estimate of framerate for user
//    int                     FramerateSecPerFrameIdx;
//    float                   FramerateSecPerFrameAccum;
//    int                     CaptureMouseNextFrame;              // explicit capture via CaptureInputs() sets those flags
//    int                     CaptureKeyboardNextFrame;
//    char                    TempBuffer[1024*3+1];               // temporary text buffer
}