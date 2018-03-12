package imgui

import imgui.ImGui.getNavInputAmount
import imgui.ImGui.io
import imgui.ImGui.isKeyPressed
import imgui.internal.InputReadMode

/** Flags for ImGui::Begin()    */
enum class WindowFlags(val i: Int) {

    Null(0),
    /** Disable title-bar   */
    NoTitleBar(1 shl 0),
    /** Disable user resizing with the lower-right grip */
    NoResize(1 shl 1),
    /** Disable user moving the window  */
    NoMove(1 shl 2),
    /** Disable scrollbars (window can still scroll with mouse or programatically)  */
    NoScrollbar(1 shl 3),
    /** Disable user vertically scrolling with mouse wheel. On child window, mouse wheel will be forwarded to the parent
     *  unless noScrollbar is also set.  */
    NoScrollWithMouse(1 shl 4),
    /** Disable user collapsing window by double-clicking on it */
    NoCollapse(1 shl 5),
    /** Resize every window to its content every frame  */
    AlwaysAutoResize(1 shl 6),
    @Deprecated("OBSOLETE! Use e.g. style.FrameBorderSize=1.0f to enable borders")
    /** Show borders around windows and items   */
    ShowBorders(1 shl 7),
    /** Never load/save settings in .ini file   */
    NoSavedSettings(1 shl 8),
    /** Disable catching mouse or keyboard inputs   */
    NoInputs(1 shl 9),
    /** Has a menu-bar  */
    MenuBar(1 shl 10),
    /** Allow horizontal scrollbar to appear (off by default). You may use SetNextWindowContentSize(ImVec2(width),0.0f));
     *  prior to calling Begin() to specify width. Read code in imgui_demo in the "Horizontal Scrolling" section.    */
    HorizontalScrollbar(1 shl 11),
    /** Disable taking focus when transitioning from hidden to visible state    */
    NoFocusOnAppearing(1 shl 12),
    /** Disable bringing window to front when taking focus (e.g. clicking on it or programatically giving it focus) */
    NoBringToFrontOnFocus(1 shl 13),
    /** Always show vertical scrollbar (even if ContentSize.y < Size.y) */
    AlwaysVerticalScrollbar(1 shl 14),
    /** Always show horizontal scrollbar (even if ContentSize.x < Size.x)   */
    AlwaysHorizontalScrollbar(1 shl 15),
    /** Ensure child windows without border uses style.WindowPadding (ignored by default for non-bordered child windows),
     *  because more convenient)  */
    AlwaysUseWindowPadding(1 shl 16),
    /** (WIP) Enable resize from any corners and borders. Your back-end needs to honor the different values of io.mouseCursor set by imgui. */
    ResizeFromAnySide(1 shl 17),
    /** No gamepad/keyboard navigation within the window    */
    NoNavInputs(1 shl 18),
    /** No focusing toward this window with gamepad/keyboard navigation (e.g. skipped by CTRL+TAB)  */
    NoNavFocus(1 shl 19),

    NoNav(NoNavInputs or NoNavFocus),

    // [Internal]

    /** (WIP) Allow gamepad/keyboard navigation to cross over parent border to this child (only use on child that have no scrolling!)   */
    NavFlattened(1 shl 23),
    /** Don't use! For internal use by BeginChild() */
    ChildWindow(1 shl 24),
    /** Don't use! For internal use by BeginTooltip()   */
    Tooltip(1 shl 25),
    /** Don't use! For internal use by BeginPopup() */
    Popup(1 shl 26),
    /** Don't use! For internal use by BeginPopupModal()    */
    Modal(1 shl 27),
    /** Don't use! For internal use by BeginMenu()  */
    ChildMenu(1 shl 28);

    infix fun or(b: WindowFlags) = i or b.i
    infix fun or(b: Int) = i or b
}

infix fun Int.or(b: WindowFlags) = this or b.i
infix fun Int.has(b: WindowFlags) = (this and b.i) != 0
infix fun Int.hasnt(b: WindowFlags) = (this and b.i) == 0
infix fun Int.wo(b: WindowFlags) = this and b.i.inv()

/** Flags for ImGui::InputText()    */
enum class InputTextFlags(val i: Int) {

    Null(0),
    /** Allow 0123456789 . + - * /      */
    CharsDecimal(1 shl 0),
    /** Allow 0123456789ABCDEFabcdef    */
    CharsHexadecimal(1 shl 1),
    /** Turn a..z into A..Z */
    CharsUppercase(1 shl 2),
    /** Filter out spaces), tabs    */
    CharsNoBlank(1 shl 3),
    /** Select entire text when first taking mouse focus    */
    AutoSelectAll(1 shl 4),
    /** Return 'true' when Enter is pressed (as opposed to when the value was modified) */
    EnterReturnsTrue(1 shl 5),
    /** Call user function on pressing TAB (for completion handling)    */
    CallbackCompletion(1 shl 6),
    /** Call user function on pressing Up/Down arrows (for history handling)    */
    CallbackHistory(1 shl 7),
    /** Call user function every time. User code may query cursor position), modify text buffer.    */
    CallbackAlways(1 shl 8),
    /** Call user function to filter character. Modify data->EventChar to replace/filter input), or return 1 to discard
     *  character.  */
    CallbackCharFilter(1 shl 9),
    /** Pressing TAB input a '\t' character into the text field */
    AllowTabInput(1 shl 10),
    /** In multi-line mode), unfocus with Enter), add new line with Ctrl+Enter (default is opposite: unfocus with
     *  Ctrl+Enter), add line with Enter).   */
    CtrlEnterForNewLine(1 shl 11),
    /** Disable following the cursor horizontally   */
    NoHorizontalScroll(1 shl 12),
    /** Insert mode */
    AlwaysInsertMode(1 shl 13),
    /** Read-only mode  */
    ReadOnly(1 shl 14),
    /** Password mode), display all characters as '*'   */
    Password(1 shl 15),
    /** Disable undo/redo. Note that input text owns the text data while active, if you want to provide your own
     *  undo/redo stack you need e.g. to call clearActiveID(). */
    NoUndoRedo(1 shl 16),

    // [Internal]

    /** For internal use by InputTextMultiline()    */
    Multiline(1 shl 20);

    infix fun or(b: InputTextFlags) = i or b.i
}

infix fun Int.or(b: InputTextFlags) = this or b.i
infix fun Int.has(b: InputTextFlags) = (this and b.i) != 0
infix fun Int.hasnt(b: InputTextFlags) = (this and b.i) == 0

/** Flags for ImGui::TreeNodeEx(), ImGui::CollapsingHeader*()   */
enum class TreeNodeFlags(val i: Int) {

    Null(0),
    /** Draw as selected    */
    Selected(1 shl 0),
    /** Full colored frame (e.g. for CollapsingHeader)  */
    Framed(1 shl 1),
    /** Hit testing to allow subsequent widgets to overlap this one */
    AllowItemOverlap(1 shl 2),
    /** Don't do a TreePush() when open (e.g. for CollapsingHeader) ( no extra indent nor pushing on ID stack   */
    NoTreePushOnOpen(1 shl 3),
    /** Don't automatically and temporarily open node when Logging is active (by default logging will automatically open
     *  tree nodes) */
    NoAutoOpenOnLog(1 shl 4),
    /** Default node to be open */
    DefaultOpen(1 shl 5),
    /** Need double-click to open node  */
    OpenOnDoubleClick(1 shl 6),
    /** Only open when clicking on the arrow part. If OpenOnDoubleClick is also set), single-click arrow or double-click
     *  all box to open.    */
    OpenOnArrow(1 shl 7),
    /** No collapsing), no arrow (use as a convenience for leaf nodes). */
    Leaf(1 shl 8),
    /** Display a bullet instead of arrow   */
    Bullet(1 shl 9),
    /** Use FramePadding (even for an unframed text node) to vertically align text baseline to regular widget height.
     *  Equivalent to calling alignTextToFramePadding().    */
    FramePadding(1 shl 10),
    //ImGuITreeNodeFlags_SpanAllAvailWidth  = 1 << 11,  // FIXME: TODO: Extend hit box horizontally even if not framed
    //ImGuiTreeNodeFlags_NoScrollOnOpen     = 1 << 12,  // FIXME: TODO: Disable automatic scroll on TreePop() if node got just open and contents is not visible
    /** (WIP) Nav: left direction may move to this TreeNode() from any of its child (items submitted between TreeNode and TreePop)   */
    NavLeftJumpsBackHere(1 shl 13),
    CollapsingHeader(Framed or NoAutoOpenOnLog);

    infix fun or(treeNodeFlag: TreeNodeFlags) = i or treeNodeFlag.i
}

infix fun Int.or(b: TreeNodeFlags) = this or b.i
infix fun Int.has(b: TreeNodeFlags) = (this and b.i) != 0
infix fun Int.hasnt(b: TreeNodeFlags) = (this and b.i) == 0

/** Flags for ImGui::Selectable()   */
enum class SelectableFlags(val i: Int) {

    Null(0),
    /** Clicking this don't close parent popup window   */
    DontClosePopups(1 shl 0),
    /** Selectable frame can span all columns (text will still fit in current column)   */
    SpanAllColumns(1 shl 1),
    /** Generate press events on double clicks too  */
    AllowDoubleClick(1 shl 2),
    /* -> PressedOnClick  */
    Menu(1 shl 3),
    /* -> PressedOnRelease  */
    MenuItem(1 shl 4),
    /* private  */
    Disabled(1 shl 5),
    /* private  */
    DrawFillAvailWidth(1 shl 6);

    infix fun or(other: SelectableFlags) = i or other.i
}

infix fun Int.or(other: SelectableFlags) = this or other.i
infix fun Int.has(b: SelectableFlags) = (this and b.i) != 0
infix fun Int.hasnt(b: SelectableFlags) = (this and b.i) == 0

enum class ComboFlags(val i: Int) {
    /** Align the popup toward the left by default */
    PopupAlignLeft(1 shl 0),
    /** Max ~4 items visible */
    HeightSmall(1 shl 1),
    /** Max ~8 items visible (default) */
    HeightRegular(1 shl 2),
    /** Max ~20 items visible */
    HeightLarge(1 shl 3),
    /** As many fitting items as possible */
    HeightLargest(1 shl 4),
    /** Display on the preview box without the square arrow button  */
    NoArrowButton(1 shl 5),
    /** Display only a square arrow button  */
    NoPreview(1 shl 6),
    HeightMask_(HeightSmall or HeightRegular or HeightLarge or HeightLargest)
}

infix fun ComboFlags.or(other: ComboFlags) = i or other.i
infix fun Int.and(other: ComboFlags) = and(other.i)
infix fun Int.or(other: ComboFlags) = or(other.i)
infix fun Int.has(b: ComboFlags) = and(b.i) != 0
infix fun Int.hasnt(b: ComboFlags) = and(b.i) == 0

// Flags for ImGui::IsWindowFocused()
enum class FocusedFlags(val i: Int) {
    Null(0),
    /** isWindowFocused(): Return true if any children of the window is focused */
    ChildWindows(1 shl 0),
    /** isWindowFocused(): Test from root window (top most parent of the current hierarchy) */
    RootWindow(1 shl 1),
    /** IsWindowFocused(): Return true if any window is focused */
    AnyWindow(1 shl 2),
    RootAndChildWindows(RootWindow or ChildWindows)
}

infix fun FocusedFlags.or(other: FocusedFlags) = i or other.i
infix fun Int.and(other: FocusedFlags) = and(other.i)
infix fun Int.or(other: FocusedFlags) = or(other.i)
infix fun Int.has(b: FocusedFlags) = and(b.i) != 0
infix fun Int.hasnt(b: FocusedFlags) = and(b.i) == 0

enum class HoveredFlags(val i: Int) {
    /** Return true if directly over the item/window, not obstructed by another window, not obstructed by an active
     *  popup or modal blocking inputs under them.  */
    Default(0),
    /** isWindowHovered() only: Return true if any children of the window is hovered */
    ChildWindows(1 shl 0),
    /** isWindowHovered() only: Test from root window (top most parent of the current hierarchy) */
    RootWindow(1 shl 1),
    /** IsWindowHovered() only: Return true if any window is hovered    */
    AnyWindow(1 shl 2),
    /** Return true even if a popup window is normally blocking access to this item/window  */
    AllowWhenBlockedByPopup(1 shl 3),
    //AllowWhenBlockedByModal     (1 shl 4),   // Return true even if a modal popup window is normally blocking access to this item/window. FIXME-TODO: Unavailable yet.
    /** Return true even if an active item is blocking access to this item/window. Useful for Drag and Drop patterns.   */
    AllowWhenBlockedByActiveItem(1 shl 5),
    /** Return true even if the position is overlapped by another window,   */
    AllowWhenOverlapped(1 shl 6),
    RectOnly(AllowWhenBlockedByPopup.i or AllowWhenBlockedByActiveItem.i or AllowWhenOverlapped.i),
    RootAndChildWindows(RootWindow or ChildWindows)
}

infix fun HoveredFlags.or(other: HoveredFlags) = i or other.i
infix fun Int.or(other: HoveredFlags) = this or other.i
infix fun Int.has(b: HoveredFlags) = (this and b.i) != 0
infix fun Int.hasnt(b: HoveredFlags) = (this and b.i) == 0

/** Flags for beginDragDropSource(), acceptDragDropPayload() */
enum class DragDropFlags(val i: Int) {
    // BeginDragDropSource() flags
    /** By default), a successful call to beginDragDropSource opens a tooltip so you can display a preview or
     *  description of the source contents. This flag disable this behavior. */
    SourceNoPreviewTooltip(1 shl 0),
    /** By default), when dragging we clear data so that isItemHovered() will return true), to avoid subsequent user code
     *  submitting tooltips. This flag disable this behavior so you can still call IsItemHovered() on the source item. */
    SourceNoDisableHover(1 shl 1),
    /** Disable the behavior that allows to open tree nodes and collapsing header by holding over them while dragging
     *  a source item. */
    SourceNoHoldToOpenOthers(1 shl 2),
    /** Allow items such as text()), Image() that have no unique identifier to be used as drag source),
     *  by manufacturing a temporary identifier based on their window-relative position.
     *  This is extremely unusual within the dear imgui ecosystem and so we made it explicit. */
    SourceAllowNullID(1 shl 3),
    /** External source (from outside of imgui), won't attempt to read current item/window info. Will always return true.
     *  Only one Extern source can be active simultaneously.    */
    SourceExtern(1 shl 4),
    // AcceptDragDropPayload() flags
    /** AcceptDragDropPayload() will returns true even before the mouse button is released.
     *  You can then call isDelivery() to test if the payload needs to be delivered. */
    AcceptBeforeDelivery(1 shl 10),
    /** Do not draw the default highlight rectangle when hovering over target. */
    AcceptNoDrawDefaultRect(1 shl 11),
    /** For peeking ahead and inspecting the payload before delivery. */
    AcceptPeekOnly(AcceptBeforeDelivery or AcceptNoDrawDefaultRect)
}

infix fun DragDropFlags.or(other: DragDropFlags) = i or other.i
infix fun Int.or(other: DragDropFlags) = this or other.i
infix fun Int.has(b: DragDropFlags) = (this and b.i) != 0
infix fun Int.hasnt(b: DragDropFlags) = (this and b.i) == 0

// Standard Drag and Drop payload types. Types starting with '_' are defined by Dear ImGui.
/** float[3], Standard type for colors, without alpha. User code may use this type. */
val PAYLOAD_TYPE_COLOR_3F = "_COL3F"
/** float[4], Standard type for colors. User code may use this type. */
val PAYLOAD_TYPE_COLOR_4F = "_COL4F"

/** A direction */
enum class Dir { None, Left, Right, Up, Down, Count;

    val i = ordinal - 1

    companion object {
        fun of(i: Int) = values()[i]
    }
}

infix fun Int.shl(b: Dir) = shl(b.i)

/** User fill ImGuiio.KeyMap[] array with indices into the ImGuiio.KeysDown[512] array  */
enum class Key { Tab, LeftArrow, RightArrow, UpArrow, DownArrow, PageUp, PageDown, Home, End, Insert, Delete, Backspace,
    Space, Enter, Escape, A, C, V, X, Y, Z;

    companion object {
        val COUNT = values().size
    }

    val i = ordinal

    /** JVM implementation of IsKeyPressedMap   */
    fun isPressed(repeat: Boolean) = isKeyPressed(io.keyMap[i], repeat)

    val isPressed get() = isPressed(true)

    /** map ImGuiKey_* values into user's key index. == io.KeyMap[key]   */
    val index get() = i
}

/** [BETA] Gamepad/Keyboard directional navigation
 *  Keyboard: Set io.configFlags |= NavFlags.EnableKeyboard to enable. ::newFrame() will automatically fill io.navInputs[]
 *  based on your io.keyDown[] + io.keyMap[] arrays.
 *  Gamepad:  Set io.configFlags |= NavFlags.EnableGamepad to enable. Fill the io.navInputs[] fields before calling
 *  ::newFrame(). Note that io.navInputs[] is cleared by ::endFrame().
 *  Read instructions in imgui.cpp for more details.    */
enum class NavInput {
    // Gamepad Mapping
    /** activate / open / toggle / tweak value       // e.g. Circle (PS4), A (Xbox), B (Switch), Space (Keyboard)   */
    Activate,
    /** cancel / close / exit                        // e.g. Cross  (PS4), B (Xbox), A (Switch), Escape (Keyboard)  */
    Cancel,
    /** text input / on-screen keyboard              // e.g. Triang.(PS4), Y (Xbox), X (Switch), Return (Keyboard)  */
    Input,
    /** tap: toggle menu / hold: focus, move, resize // e.g. Square (PS4), X (Xbox), Y (Switch), Alt (Keyboard) */
    Menu,
    /** move / tweak / resize window (w/ PadMenu)    // e.g. D-pad Left/Right/Up/Down (Gamepads), Arrow keys (Keyboard) */
    DpadLeft,
    /** move / tweak / resize window (w/ PadMenu)    // e.g. D-pad Left/Right/Up/Down (Gamepads), Arrow keys (Keyboard) */
    DpadRight,
    /** move / tweak / resize window (w/ PadMenu)    // e.g. D-pad Left/Right/Up/Down (Gamepads), Arrow keys (Keyboard) */
    DpadUp,
    /** move / tweak / resize window (w/ PadMenu)    // e.g. D-pad Left/Right/Up/Down (Gamepads), Arrow keys (Keyboard) */
    DpadDown,
    /** scroll / move window (w/ PadMenu)            // e.g. Left Analog Stick Left/Right/Up/Down   */
    LStickLeft,
    /** scroll / move window (w/ PadMenu)            // e.g. Left Analog Stick Left/Right/Up/Down   */
    LStickRight,
    /** scroll / move window (w/ PadMenu)            // e.g. Left Analog Stick Left/Right/Up/Down   */
    LStickUp,
    /** scroll / move window (w/ PadMenu)            // e.g. Left Analog Stick Left/Right/Up/Down   */
    LStickDown,
    /** next window (w/ PadMenu)                     // e.g. L1 or L2 (PS4), LB or LT (Xbox), L or ZL (Switch)  */
    FocusPrev,
    /** prev window (w/ PadMenu)                     // e.g. R1 or R2 (PS4), RB or RT (Xbox), R or ZL (Switch)  */
    FocusNext,
    /** slower tweaks                                // e.g. L1 or L2 (PS4), LB or LT (Xbox), L or ZL (Switch)  */
    TweakSlow,
    /** faster tweaks                                // e.g. R1 or R2 (PS4), RB or RT (Xbox), R or ZL (Switch)  */
    TweakFast,

    /*  [Internal] Don't use directly! This is used internally to differentiate keyboard from gamepad inputs for
        behaviors that require to differentiate them.
        Keyboard behavior that have no corresponding gamepad mapping (e.g. CTRL + TAB) may be directly reading from
        io.keyDown[] instead of io.navInputs[]. */

    /** toggle menu = io.keyAlt */
    KeyMenu,
    /** move left = Arrow keys  */
    KeyLeft,
    /** move right = Arrow keys  */
    KeyRight,
    /** move up = Arrow keys  */
    KeyUp,
    /** move down = Arrow keys  */
    KeyDown;

    val i = ordinal

    companion object {
        val COUNT = values().size
        val InternalStart = KeyMenu.i
    }

    /** Equivalent of isKeyDown() for NavInputs[]   */ // JVM TODO check for semantic Key.isPressed/Down
    fun isDown() = io.navInputs[i] > 0f

    /** Equivalent of isKeyPressed() for NavInputs[]    */
    fun isPressed(mode: InputReadMode) = getNavInputAmount(this, mode) > 0f
}

/** Configuration flags stored in io.configFlags  */
enum class ConfigFlags(val i: Int) {
    /** Master keyboard navigation enable flag. ::newFrame() will automatically fill io.navInputs[] based on io.keyDown[].    */
    NavEnableKeyboard(1 shl 0),
    /** Master gamepad navigation enable flag. This is mostly to instruct your imgui back-end to fill io.navInputs[].   */
    NavEnableGamepad(1 shl 1),
    /** Request navigation to allow moving the mouse cursor. May be useful on TV/console systems where moving a virtual
     *  mouse is awkward. Will update io.mousePos and set io.WantMoveMouse = true. If enabled you MUST honor io.wantMoveMouse
     *  requests in your binding, otherwise ImGui will react as if the mouse is jumping around back and forth.  */
    NavMoveMouse(1 shl 2),
    /** Do not set the io.WantCaptureKeyboard flag with io.NavActive is set.    */
    NavNoCaptureKeyboard(1 shl 3),

    /*  User storage (to allow your back-end/engine to communicate to code that may be shared between multiple projects.
        Those flags are not used by core ImGui)     */

    /** Back-end is SRGB-aware. */
    IsSRGB(1 shl 20),
    /** Back-end is using a touch screen instead of a mouse.   */
    IsTouchScreen(1 shl 21);
}

infix fun Int.has(b: ConfigFlags) = and(b.i) != 0
infix fun Int.hasnt(b: ConfigFlags) = and(b.i) == 0
infix fun Int.or(b: ConfigFlags) = or(b.i)
infix fun ConfigFlags.or(b: ConfigFlags) = i or b.i

/** Enumeration for PushStyleColor() / PopStyleColor()  */
enum class Col {

    Text,
    TextDisabled,
    /** Background of normal windows    */
    WindowBg,
    /** Background of child windows */
    ChildBg,
    /*-* Background of popups, menus, tooltips windows  */
    PopupBg,
    Border,
    BorderShadow,
    /** Background of checkbox, radio button, plot, slider, text input  */
    FrameBg,
    FrameBgHovered,
    FrameBgActive,
    TitleBg,
    TitleBgActive,
    TitleBgCollapsed,
    MenuBarBg,
    ScrollbarBg,
    ScrollbarGrab,
    ScrollbarGrabHovered,
    ScrollbarGrabActive,
    CheckMark,
    SliderGrab,
    SliderGrabActive,
    Button,
    ButtonHovered,
    ButtonActive,
    Header,
    HeaderHovered,
    HeaderActive,
    Separator,
    SeparatorHovered,
    SeparatorActive,
    ResizeGrip,
    ResizeGripHovered,
    ResizeGripActive,
    CloseButton,
    CloseButtonHovered,
    CloseButtonActive,
    PlotLines,
    PlotLinesHovered,
    PlotHistogram,
    PlotHistogramHovered,
    TextSelectedBg,
    /** darken entire screen when a modal window is active   */
    ModalWindowDarkening,
    DragDropTarget,
    /** gamepad/keyboard: current highlighted item  */
    NavHighlight,
    /** gamepad/keyboard: when holding NavMenu to focus/move/resize windows */
    NavWindowingHighlight;

    val i = ordinal

    val u32 get() = ImGui.getColorU32(i, alphaMul = 1f)

    companion object {
        val COUNT = values().size
        fun of(i: Int) = values()[i]
    }
}

/** Enumeration for PushStyleVar() / PopStyleVar() to temporarily modify the ImGuiStyle structure.
 *  NB: the enum only refers to fields of ImGuiStyle which makes sense to be pushed/poped inside UI code.
 *  During initialization, feel free to just poke into ImGuiStyle directly.
 *  NB: if changing this enum, you need to update the associated internal table GStyleVarInfo[] accordingly. This is
 *  where we link enum values to members offset/type.   */
enum class StyleVar {
    /** float   */
    Alpha,
    /** vec2    */
    WindowPadding,
    /** float   */
    WindowRounding,
    /** float */
    WindowBorderSize,
    /** vec2    */
    WindowMinSize,
    /** Vec2    */
    WindowTitleAlign,
    /** float   */
    ChildRounding,
    /** float */
    ChildBorderSize,
    /** float */
    PopupRounding,
    /** float */
    PopupBorderSize,
    /** vec2    */
    FramePadding,
    /** float   */
    FrameRounding,
    /** float   */
    FrameBorderSize,
    /** vec2    */
    ItemSpacing,
    /** vec2    */
    ItemInnerSpacing,
    /** float   */
    IndentSpacing,
    /** Float   */
    ScrollbarSize,
    /** Float   */
    ScrollbarRounding,
    /** float   */
    GrabMinSize,
    /** Float   */
    GrabRounding,
    /** vec2  */
    ButtonTextAlign;

    val i = ordinal
}

/** Enumeration for ColorEdit3() / ColorEdit4() / ColorPicker3() / ColorPicker4() / ColorButton()   */
enum class ColorEditFlags(val i: Int) {

    Null(0),
    /** ColorEdit, ColorPicker, ColorButton: ignore Alpha component (read 3 components from the input pointer). */
    NoAlpha(1 shl 1),
    /** ColorEdit: disable picker when clicking on colored square.  */
    NoPicker(1 shl 2),
    /** ColorEdit: disable toggling options menu when right-clicking on inputs/small preview.   */
    NoOptions(1 shl 3),
    /** ColorEdit, ColorPicker: disable colored square preview next to the inputs. (e.g. to show only the inputs)   */
    NoSmallPreview(1 shl 4),
    /** ColorEdit, ColorPicker: disable inputs sliders/text widgets (e.g. to show only the small preview colored square).   */
    NoInputs(1 shl 5),
    /** ColorEdit, ColorPicker, ColorButton: disable tooltip when hovering the preview. */
    NoTooltip(1 shl 6),
    /** ColorEdit, ColorPicker: disable display of inline text label (the label is still forwarded to the tooltip and picker).  */
    NoLabel(1 shl 7),
    /** ColorPicker: disable bigger color preview on right side of the picker, use small colored square preview instead.    */
    NoSidePreview(1 shl 8),

    /*  User Options (right-click on widget to change some of them). You can set application defaults using
        SetColorEditOptions(). The idea is that you probably don't want to override them in most of your calls,
        let the user choose and/or call SetColorEditOptions() during startup.     */
    /** ColorEdit, ColorPicker: show vertical alpha bar/gradient in picker. */
    AlphaBar(1 shl 9),
    /** ColorEdit, ColorPicker, ColorButton: display preview as a transparent color over a checkerboard, instead of opaque. */
    AlphaPreview(1 shl 10),
    /** ColorEdit, ColorPicker, ColorButton: display half opaque / half checkerboard, instead of opaque.    */
    AlphaPreviewHalf(1 shl 11),
    /** (WIP) ColorEdit: Currently only disable 0.0f..1.0f limits in RGBA edition (note: you probably want to use
     *  ColorEditFlags.Float flag as well). */
    HDR(1 shl 12),
    /** [Inputs] ColorEdit: choose one among RGB/HSV/HEX. ColorPicker: choose any combination using RGB/HSV/HEX.    */
    RGB(1 shl 13),
    /** [Inputs] ColorEdit: choose one among RGB/HSV/HEX. ColorPicker: choose any combination using RGB/HSV/HEX.    */
    HSV(1 shl 14),
    /** [Inputs] ColorEdit: choose one among RGB/HSV/HEX. ColorPicker: choose any combination using RGB/HSV/HEX.    */
    HEX(1 shl 15),
    /** [DataType] ColorEdit, ColorPicker, ColorButton: _display_ values formatted as 0..255.   */
    Uint8(1 shl 16),
    /** [DataType] ColorEdit, ColorPicker, ColorButton: _display_ values formatted as 0.0f..1.0f floats instead of 0..255 integers.
     *  No round-trip of value via integers.    */
    Float(1 shl 17),
    /** [PickerMode] ColorPicker: bar for Hue, rectangle for Sat/Value. */
    PickerHueBar(1 shl 18),
    /** [PickerMode] ColorPicker: wheel for Hue, triangle for Sat/Value.    */
    PickerHueWheel(1 shl 19),
    // Internals/Masks
    _InputsMask(RGB or HSV or HEX),
    _DataTypeMask(Uint8 or Float),
    _PickerMask(PickerHueWheel or PickerHueBar),
    /** Change application default using SetColorEditOptions()  */
    _OptionsDefault(Uint8 or RGB or PickerHueBar)
}

infix fun ColorEditFlags.and(other: ColorEditFlags) = i and other.i
infix fun ColorEditFlags.or(other: ColorEditFlags) = i or other.i
infix fun ColorEditFlags.or(other: Int) = i or other
infix fun Int.and(other: ColorEditFlags) = this and other.i
infix fun Int.or(other: ColorEditFlags) = this or other.i
infix fun Int.has(b: ColorEditFlags) = (this and b.i) != 0
infix fun Int.hasnt(b: ColorEditFlags) = (this and b.i) == 0
infix fun Int.wo(b: ColorEditFlags) = this and b.i.inv()
infix fun Int.wo(b: Int) = this and b.inv()

/** Enumeration for GetMouseCursor()    */
enum class MouseCursor {

    None,
    Arrow,
    /** When hovering over InputText, etc.  */
    TextInput,
    /** Unused  */
    ResizeAll,
    /** When hovering over an horizontal border  */
    ResizeNS,
    /** When hovering over a vertical border or a column */
    ResizeEW,
    /** When hovering over the bottom-left corner of a window  */
    ResizeNESW,
    /** When hovering over the bottom-right corner of a window  */
    ResizeNWSE;

    val i = ordinal - 1

    companion object {
        fun of(i: Int) = values().first { it.i == i }
        val COUNT = ResizeNWSE.i + 1
    }
}

/** Condition for setWindow***(), setNextWindow***(), setNextTreeNode***() functions
 *  All those functions treat 0 as a shortcut to Always.
 *  From the point of view of the user use this as an enum (don't combine multiple values into flags).    */
enum class Cond(val i: Int) {

    Null(0),
    /** Set the variable    */
    Always(1 shl 0),
    /** Set the variable once per runtime session (only the first call with succeed)    */
    Once(1 shl 1),
    /** Set the variable if the window has no saved data (if doesn't exist in the .ini file)    */
    FirstUseEver(1 shl 2),
    /** Set the variable if the window is appearing after being hidden/inactive (or the first time) */
    Appearing(1 shl 3);

    infix fun or(other: Cond) = i or other.i
}

infix fun Int.or(other: Cond) = or(other.i)
infix fun Int.has(b: Cond) = and(b.i) != 0
infix fun Cond.has(b: Cond) = i.and(b.i) != 0
infix fun Int.hasnt(b: Cond) = and(b.i) == 0
infix fun Int.wo(b: Cond) = and(b.i.inv())


/** Transient per-window flags, reset at the beginning of the frame. For child window, inherited from parent on first Begin().
 *  This is going to be exposed in imgui.h when stabilized enough.  */
enum class ItemFlags(val i: Int) {
    /** true    */
    AllowKeyboardFocus(1 shl 0),
    /** false. Button() will return true multiple times based on io.KeyRepeatDelay and io.KeyRepeatRate settings.  */
    ButtonRepeat(1 shl 1),
    /** false. FIXME-WIP: Disable interactions but doesn't affect visuals. Should be: grey out and disable interactions with widgets that affect data + view widgets (WIP)     */
    Disabled(1 shl 2),
    /** false   */
    NoNav(1 shl 3),
    /** false   */
    NoNavDefaultFocus(1 shl 4),
    /** false, MenuItem/Selectable() automatically closes current Popup window  */
    SelectableDontClosePopup(1 shl 5),

    Default_(AllowKeyboardFocus.i)
}

infix fun ItemFlags.or(other: ItemFlags) = i or other.i
infix fun Int.or(other: ItemFlags) = or(other.i)
infix fun Int.has(b: ItemFlags) = (this and b.i) != 0
infix fun Int.hasnt(b: ItemFlags) = (this and b.i) == 0
infix fun Int.wo(b: ItemFlags) = and(b.i.inv())