/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui.internal;

import glm.vec._2.Vec2;
import imgui.DrawData;
import imgui.DrawList;
import imgui.Font;
import imgui.IO;
import imgui.MouseCursor;
import imgui.Style;
import java.io.File;
import java.util.ArrayList;

/**
 * Main state for ImGui.
 *
 * @author GBarbieri
 */
public class Context {

    public boolean initialized = false;

    public IO io;

    public Style style;

    /**
     * (Shortcut) == FontStack.empty() ? IO.Font : FontStack.back().
     */
    Font font = null;

    /**
     * (Shortcut) == FontBaseSize * g.CurrentWindow->FontWindowScale == window->FontSize().
     */
    float fontSize = 0.0f;

    /**
     * (Shortcut) == IO.FontGlobalScale * Font->Scale * Font->FontSize. Size of characters..
     */
    float fontBaseSize = 0.0f;

    /**
     * (Shortcut) == Font->TexUvWhitePixel.
     */
    Vec2 fontTexUvWhitePixel = new Vec2(0.0f);

    float time = 0.0f;

    int frameCount = 0;

    int frameCountEnded = -1;

    int frameCountRendered = -1;

    ArrayList<Window> windows;

    ArrayList<Window> windowsSortBuffer;

    /**
     * Being drawn into.
     */
    Window currentWindow = null;

    ArrayList<Window> currentWindowStack;

    /**
     * Will catch keyboard inputs.
     */
    Window focusedWindow = null;

    /**
     * Will catch mouse inputs.
     */
    Window hoveredWindow = null;

    /**
     * Will catch mouse inputs (for focus/move only).
     */
    Window hoveredRootWindow = null;

    /**
     * Hovered widget.
     */
    int hoveredId = 0;

    boolean hoveredIdAllowOverlap = false;

    int hoveredIdPreviousFrame = 0;

    /**
     * Active widget.
     */
    int activeId = 0;

    int activeIdPreviousFrame = 0;

    boolean activeIdIsAlive = false;

    /**
     * Set at the time of activation for one frame.
     */
    boolean activeIdIsJustActivated = false;

    /**
     * Set only by active widget.
     */
    boolean activeIdAllowOverlap = false;

    /**
     * Clicked offset from upper-left corner, if applicable (currently only set by ButtonBehavior).
     */
    Vec2 activeIdClickOffset = new Vec2(-1, -1);

    Window activeIdWindow;

    /**
     * Track the child window we clicked on to move a window..
     */
    Window movedWindow = null;

    /**
     * == MovedWindow->RootWindow->MoveId.
     */
    int movedWindowMoveId = 0;

    /**
     * .ini Settings.
     */
    public ArrayList<IniData> settings;

    /**
     * Save .ini Settings on disk when time reaches zero.
     */
    float settingsDirtyTimer = 0.0f;

    /**
     * Stack for PushStyleColor()/PopStyleColor().
     */
    ArrayList<ColMod> colorModifiers;

    /**
     * Stack for PushStyleVar()/PopStyleVar().
     */
    ArrayList<StyleMod> styleModifiers;

    /**
     * Stack for PushFont()/PopFont().
     */
    ArrayList<Font> fontStack;

    /**
     * Which popups are open (persistent).
     */
    ArrayList<PopupRef> openPopupStack;

    /**
     * Which level of BeginPopup() we are in (reset every frame).
     */
    ArrayList<PopupRef> currentPopupStack;

    // Storage for SetNexWindow** and SetNextTreeNode*** functions -------------------------------------------------------
//    
    Vec2 setNextWindowPosVal = new Vec2(0.0f);

    Vec2 setNextWindowSizeValnew = new Vec2(0.0f);

    Vec2 setNextWindowContentSizeVal;

    boolean setNextWindowCollapsedVal = false;

    int setNextWindowPosCond = 0;

    int setNextWindowSizeCond = 0;

    int setNextWindowContentSizeCond = 0;

    int setNextWindowCollapsedCond = 0;

    /**
     * Valid if 'SetNextWindowSizeConstraint' is true.
     */
    Rect setNextWindowSizeConstraintRect;

//    ImGuiSizeConstraintCallback SetNextWindowSizeConstraintCallback;
//    void*                       SetNextWindowSizeConstraintCallbackUserData;
    boolean setNextWindowSizeConstraint;

    boolean setNextWindowFocus = false;

    boolean setNextTreeNodeOpenVal = false;

    int setNextTreeNodeOpenCond = 0;

    // Render ------------------------------------------------------------------------------------------------------------
    //
    /**
     * Main ImDrawData instance to pass render information to the user.
     */
    DrawData renderDrawData;

    ArrayList<DrawList> renderDrawLists = new ArrayList<DrawList>();

    float modalWindowDarkeningRatio = 0.0f;

    /**
     * Optional software render of mouse cursors, if io.MouseDrawCursor is set + a few debug overlays.
     */
    DrawList overlayDrawList = new DrawList();

    int mouseCursor = MouseCursor.Arrow;

    MouseCursorData[] mouseCursorData = new MouseCursorData[MouseCursor.Count];

    // Widget state ------------------------------------------------------------------------------------------------------
    //
    TextEditState inputTextState;

    Font inputTextPasswordFont;

    /**
     * Temporary text input when CTRL+clicking on a slider, etc.
     */
    int scalarAsInputTextId = 0;

    /**
     * Store user selection of color edit mode.
     */
    Storage colorEditModeStorage;

    /**
     * Currently dragged value, always float, not rounded by end-user precision settings.
     */
    float dragCurrentValue = 0.0f;

    Vec2 dragLastMouseDelta = new Vec2(0.0f);

    /**
     * If speed == 0.0f, uses (max-min) * DragSpeedDefaultRatio.
     */
    float dragSpeedDefaultRatio = 0.01f;

    float dragSpeedScaleSlow = 0.01f;

    float dragSpeedScaleFast = 10.0f;

    /**
     * Distance between mouse and center of grab box, normalized in parent space. Use storage?.
     */
    Vec2 scrollbarClickDeltaToGrabCenter = new Vec2(0.0f);

    char[] tooltip = new char[1024];

    /**
     * If no custom clipboard handler is defined.
     */
    String privateClipboard = null;

    /**
     * Cursor position request & last passed to the OS Input Method Editor.
     */
    Vec2 osImePosRequest = new Vec2(-1.0f), osImePosSet = new Vec2(-1.0f);

    // Logging -----------------------------------------------------------------------------------------------------------
    //
    boolean logEnabled = false;

    /**
     * If != NULL log to stdout/ file.
     */
    File logFile = null;

    /**
     * Else log to clipboard. This is pointer so our GImGui static constructor doesn't call heap allocators.
     *
     * TODO check if needed
     */
//    TextBuffer LogClipboard=null;
    int logStartDepth = 0;

    int logAutoExpandMaxDepth = 2;

    // Misc --------------------------------------------------------------------------------------------------------------
    //
    /**
     * Calculate estimate of framerate for user
     */
    float[] framerateSecPerFrame = new float[120];

    int framerateSecPerFrameIdx = 0;

    float framerateSecPerFrameAccum = 0.0f;

    /**
     * Explicit capture via CaptureInputs() sets those flags.
     */
    int captureMouseNextFrame = -1;

    int captureKeyboardNextFrame = -1;

    /**
     * Temporary text buffer.
     */
    char[] tempBuffer = new char[1024 * 3 + 1];

    public Context() {

        // Give it a name for debugging
        overlayDrawList.setOwnerName("##Overlay");
    }
}
