package imgui

import imgui.ImGui.isKeyPressed

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
    /** Disable user vertically scrolling with mouse wheel  */
    NoScrollWithMouse(1 shl 4),
    /** Disable user collapsing window by double-clicking on it */
    NoCollapse(1 shl 5),
    /** Resize every window to its content every frame  */
    AlwaysAutoResize(1 shl 6),
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

    // [Internal]

    /** Don't use! For internal use by BeginChild() */
    ChildWindow(1 shl 22),
    /** Don't use! For internal use by ComboBox()   */
    ComboBox(1 shl 23),
    /** Don't use! For internal use by BeginTooltip()   */
    Tooltip(1 shl 24),
    /** Don't use! For internal use by BeginPopup() */
    Popup(1 shl 25),
    /** Don't use! For internal use by BeginPopupModal()    */
    Modal(1 shl 26),
    /** Don't use! For internal use by BeginMenu()  */
    ChildMenu(1 shl 27);

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
    AllowOverlapMode(1 shl 2),
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
    /* private  */
    Menu(1 shl 3),
    /* private  */
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

enum class HoveredFlags(val i: Int) {
    /** Return true if directly over the item/window, not obstructed by another window, not obstructed by an active
     *  popup or modal blocking inputs under them.  */
    Default(0),
    /** Return true even if a popup window is normally blocking access to this item/window  */
    AllowWhenBlockedByPopup(1 shl 0),
    //ImGuiHoveredFlags_AllowWhenBlockedByModal     = 1 << 1,   // Return true even if a modal popup window is normally blocking access to this item/window. FIXME-TODO: Unavailable yet.
    /** Return true even if an active item is blocking access to this item/window   */
    AllowWhenBlockedByActiveItem(1 shl 2),
    /** Return true even if the position is overlapped by another window    */
    AllowWhenOverlapped(1 shl 3),
    RectOnly(AllowWhenBlockedByPopup.i or AllowWhenBlockedByActiveItem.i or AllowWhenOverlapped.i)
}

infix fun Int.or(other: HoveredFlags) = this or other.i
infix fun Int.has(b: HoveredFlags) = (this and b.i) != 0
infix fun Int.hasnt(b: HoveredFlags) = (this and b.i) == 0

/** User fill ImGuiIO.KeyMap[] array with indices into the ImGuiIO.KeysDown[512] array  */
enum class Key {

    /** for tabbing through fields  */
    Tab,
    /** for text edit   */
    LeftArrow,
    /** for text edit   */
    RightArrow,
    /** for text edit   */
    UpArrow,
    /** for text edit   */
    DownArrow,

    PageUp,

    PageDown,
    /** for text edit   */
    Home,
    /** for text edit   */
    End,
    /** for text edit   */
    Delete,
    /** for text edit   */
    Backspace,
    /** for text edit   */
    Enter,
    /** for text edit   */
    Escape,
    /** for text edit CTRL+A: select all    */
    A,
    /** for text edit CTRL+C: copy  */
    C,
    /** for text edit CTRL+V: paste */
    V,
    /** // for text edit CTRL+X: cut    */
    X,
    /** for text edit CTRL+Y: redo  */
    Y,
    /** for text edit CTRL+Z: undo  */
    Z,
    COUNT;

    val i = ordinal

    /** JVM implementation of IsKeyPressedMap   */
    fun isPressed(repeat: Boolean) = isKeyPressed(IO.keyMap[i], repeat)

    val isPressed get() = isPressed(true)

    /** map ImGuiKey_* values into user's key index. == io.KeyMap[key]   */
    val index get() = i
}

/** Enumeration for PushStyleColor() / PopStyleColor()  */
enum class Col {

    Text,
    TextDisabled,
    /** Background of normal windows    */
    WindowBg,
    /** Background of child windows */
    ChildWindowBg,
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
    ComboBg,
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
    COUNT;

    val i = ordinal

    val u32 get() = ImGui.getColorU32(i, alphaMul = 1f)
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
    /** vec2    */
    WindowMinSize,
    /** float   */
    ChildWindowRounding,
    /** vec2    */
    FramePadding,
    /** float   */
    FrameRounding,
    /** vec2    */
    ItemSpacing,
    /** vec2    */
    ItemInnerSpacing,
    /** float   */
    IndentSpacing,
    /** float   */
    GrabMinSize,
    /** vec2  */
    ButtonTextAlign,

    COUNT;

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
enum class MouseCursor(val i: Int) {

    None(-1),
    Arrow(0),
    /** When hovering over InputText, etc.  */
    TextInput(1),
    /** Unused  */
    Move(2),
    /** Unused  */
    ResizeNS(3),
    /** When hovering over a column */
    ResizeEW(4),
    /** Unused  */
    ResizeNESW(5),
    /** When hovering over the bottom-right corner of a window  */
    ResizeNWSE(6),

    Count(7);

    companion object {
        fun of(i: Int) = values().first { it.i == i }
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

infix fun Int.or(other: Cond) = this or other.i
infix fun Int.has(b: Cond) = (this and b.i) != 0
infix fun Int.hasnt(b: Cond) = (this and b.i) == 0
infix fun Int.wo(b: Cond) = this and b.i.inv()


/** Transient per-window flags, reset at the beginning of the frame. For child window, inherited from parent
 *  on first Begin().   */
enum class ItemFlags(val i: Int) {
    /** true    */
    AllowKeyboardFocus(1 shl 0),
    /** false
     *  Button() will return true multiple times based on io.KeyRepeatDelay and io.KeyRepeatRate settings.  */
    ButtonRepeat(1 shl 1),
    /** false    // All widgets appears are disabled    */
    Disabled(1 shl 2),
    //ImGuiItemFlags_NoNav                      = 1 << 3,  // false
    //ImGuiItemFlags_AllowNavDefaultFocus       = 1 << 4,  // true
    /** false, MenuItem/Selectable() automatically closes current Popup window  */
    SelectableDontClosePopup(1 shl 5),
    Default_(AllowKeyboardFocus.i)
}

infix fun ItemFlags.or(other: ItemFlags) = i or other.i
infix fun Int.or(other: ItemFlags) = or(other.i)
infix fun Int.has(b: ItemFlags) = (this and b.i) != 0
infix fun Int.hasnt(b: ItemFlags) = (this and b.i) == 0
infix fun Int.wo(b: ItemFlags) = and(b.i.inv())