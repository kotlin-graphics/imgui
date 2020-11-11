package imgui

import glm_.vec4.Vec4
import imgui.ImGui.getColorU32
import imgui.ImGui.getNavInputAmount
import imgui.ImGui.io
import imgui.ImGui.isKeyDown
import imgui.ImGui.isKeyPressed
import imgui.internal.sections.InputReadMode


//-----------------------------------------------------------------------------
// Flags & Enumerations
//-----------------------------------------------------------------------------


typealias WindowFlags = Int

/** Flags: for Begin(), BeginChild()    */
enum class WindowFlag(@JvmField val i: WindowFlags) {

    None(0),

    /** Disable title-bar   */
    NoTitleBar(1 shl 0),

    /** Disable user resizing with the lower-right grip */
    NoResize(1 shl 1),

    /** Disable user moving the window  */
    NoMove(1 shl 2),

    /** Disable scrollbars (window can still scroll with mouse or programmatically)  */
    NoScrollbar(1 shl 3),

    /** Disable user vertically scrolling with mouse wheel. On child window, mouse wheel will be forwarded to the parent
     *  unless noScrollbar is also set.  */
    NoScrollWithMouse(1 shl 4),

    /** Disable user collapsing window by double-clicking on it */
    NoCollapse(1 shl 5),

    /** Resize every window to its content every frame  */
    AlwaysAutoResize(1 shl 6),

    /** Disable drawing background color (WindowBg, etc.) and outside border. Similar as using SetNextWindowBgAlpha(0.0f).(1 shl 7) */
    NoBackground(1 shl 7),

    /** Never load/save settings in .ini file   */
    NoSavedSettings(1 shl 8),

    /** Disable catching mouse or keyboard inputs   */
    NoMouseInputs(1 shl 9),

    /** Has a menu-bar  */
    MenuBar(1 shl 10),

    /** Allow horizontal scrollbar to appear (off by default). You may use SetNextWindowContentSize(ImVec2(width),0.0f));
     *  prior to calling Begin() to specify width. Read code in imgui_demo in the "Horizontal Scrolling" section.    */
    HorizontalScrollbar(1 shl 11),

    /** Disable taking focus when transitioning from hidden to visible state    */
    NoFocusOnAppearing(1 shl 12),

    /** Disable bringing window to front when taking focus (e.g. clicking on it or programmatically giving it focus) */
    NoBringToFrontOnFocus(1 shl 13),

    /** Always show vertical scrollbar (even if ContentSize.y < Size.y) */
    AlwaysVerticalScrollbar(1 shl 14),

    /** Always show horizontal scrollbar (even if ContentSize.x < Size.x)   */
    AlwaysHorizontalScrollbar(1 shl 15),

    /** Ensure child windows without border uses style.WindowPadding (ignored by default for non-bordered child windows),
     *  because more convenient)  */
    AlwaysUseWindowPadding(1 shl 16),
    /** [BETA] Enable resize from any corners and borders. Your back-end needs to honor the different values of io.mouseCursor set by imgui.
     *  Set io.OptResizeWindowsFromEdges and make sure mouse cursors are supported by back-end (io.BackendFlags & ImGuiBackendFlags_HasMouseCursors) */
    // ResizeFromAnySide(1 shl 17),
    /** No gamepad/keyboard navigation within the window    */
    NoNavInputs(1 shl 18),

    /** No focusing toward this window with gamepad/keyboard navigation (e.g. skipped by CTRL+TAB)  */
    NoNavFocus(1 shl 19),

    /** Append '*' to title without affecting the ID, as a convenience to avoid using the ### operator.
     *  When used in a tab/docking context, tab is selected on closure and closure is deferred by one frame
     *  to allow code to cancel the closure (with a confirmation popup, etc.) without flicker. */
    UnsavedDocument(1 shl 20),

    NoNav(NoNavInputs or NoNavFocus),

    NoDecoration(NoTitleBar or NoResize or NoScrollbar or NoCollapse),

    NoInputs(NoMouseInputs or NoNavInputs or NoNavFocus),

    // [Internal]

    /** [BETA] Allow gamepad/keyboard navigation to cross over parent border to this child (only use on child that have no scrolling!)   */
    _NavFlattened(1 shl 23),

    /** Don't use! For internal use by BeginChild() */
    _ChildWindow(1 shl 24),

    /** Don't use! For internal use by BeginTooltip()   */
    _Tooltip(1 shl 25),

    /** Don't use! For internal use by BeginPopup() */
    _Popup(1 shl 26),

    /** Don't use! For internal use by BeginPopupModal()    */
    _Modal(1 shl 27),

    /** Don't use! For internal use by BeginMenu()  */
    _ChildMenu(1 shl 28);

    infix fun and(b: WindowFlag): WindowFlags = i and b.i
    infix fun and(b: WindowFlags): WindowFlags = i and b
    infix fun or(b: WindowFlag): WindowFlags = i or b.i
    infix fun or(b: WindowFlags): WindowFlags = i or b
    infix fun xor(b: WindowFlag): WindowFlags = i xor b.i
    infix fun xor(b: WindowFlags): WindowFlags = i xor b
    infix fun wo(b: WindowFlag): WindowFlags = and(b.i.inv())
    infix fun wo(b: WindowFlags): WindowFlags = and(b.inv())
}

infix fun WindowFlags.and(b: WindowFlag): WindowFlags = and(b.i)
infix fun WindowFlags.or(b: WindowFlag): WindowFlags = or(b.i)
infix fun WindowFlags.xor(b: WindowFlag): WindowFlags = xor(b.i)
infix fun WindowFlags.has(b: WindowFlag): Boolean = and(b.i) != 0
infix fun WindowFlags.hasnt(b: WindowFlag): Boolean = and(b.i) == 0
infix fun WindowFlags.wo(b: WindowFlag): WindowFlags = and(b.i.inv())



typealias InputTextFlags = Int

/** Flags for ImGui::InputText(), InputTextMultiline()    */
enum class InputTextFlag(@JvmField val i: InputTextFlags) { // TODO Int -> *flags the others enum

    None(0),

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

    /** Return 'true' when Enter is pressed (as opposed to every time the value was modified). Consider looking at the IsItemDeactivatedAfterEdit() function. */
    EnterReturnsTrue(1 shl 5),

    /** Callback on pressing TAB (for completion handling)    */
    CallbackCompletion(1 shl 6),

    /** Callback on pressing Up/Down arrows (for history handling)    */
    CallbackHistory(1 shl 7),

    /** Callback on each iteration. User code may query cursor position), modify text buffer.    */
    CallbackAlways(1 shl 8),

    /** Callback on character inputs to replace or discard them. Modify 'EventChar' to replace or discard,
     *  or return 1 in callback to discard.  */
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

    /** Allow 0123456789.+-* /eE (Scientific notation input) */
    CharsScientific(1 shl 17),

    /** Callback on buffer capacity changes request (beyond 'buf_size' parameter value), allowing the string to grow.
     *  Notify when the string wants to be resized (for string types which hold a cache of their Size).
     *  You will be provided a new BufSize in the callback and NEED to honor it. (see misc/cpp/imgui_stl.h for an example of using this) */
    CallbackResize(1 shl 18),

    /** Callback on any edit (note that InputText() already returns true on edit, the callback is useful mainly to
     *  manipulate the underlying buffer while focus is active) */
    CallbackEdit(1 shl 19),

    // [Internal]

    /** For internal use by InputTextMultiline()    */
    _Multiline(1 shl 20),

    /** For internal use by functions using InputText() before reformatting data */
    _NoMarkEdited(1 shl 21);

    infix fun and(b: InputTextFlag): InputTextFlags = i and b.i
    infix fun and(b: InputTextFlags): InputTextFlags = i and b
    infix fun or(b: InputTextFlag): InputTextFlags = i or b.i
    infix fun or(b: InputTextFlags): InputTextFlags = i or b
    infix fun xor(b: InputTextFlag): InputTextFlags = i xor b.i
    infix fun xor(b: InputTextFlags): InputTextFlags = i xor b
    infix fun wo(b: InputTextFlag): InputTextFlags = and(b.i.inv())
    infix fun wo(b: InputTextFlags): InputTextFlags = and(b.inv())
}

infix fun InputTextFlags.and(b: InputTextFlag): InputTextFlags = and(b.i)
infix fun InputTextFlags.or(b: InputTextFlag): InputTextFlags = or(b.i)
infix fun InputTextFlags.xor(b: InputTextFlag): InputTextFlags = xor(b.i)
infix fun InputTextFlags.has(b: InputTextFlag): Boolean = and(b.i) != 0
infix fun InputTextFlags.hasnt(b: InputTextFlag): Boolean = and(b.i) == 0
infix fun InputTextFlags.wo(b: InputTextFlag): InputTextFlags = and(b.i.inv())


typealias TreeNodeFlags = Int

/** Flags: for TreeNode(), TreeNodeEx(), CollapsingHeader()   */
enum class TreeNodeFlag(@JvmField val i: TreeNodeFlags) {

    None(0),

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

    /** Extend hit box to the right-most edge, even if not framed. This is not the default in order to allow adding other items on the same line. In the future we may refactor the hit system to be front-to-back, allowing natural overlaps and then this can become the default. */
    SpanAvailWidth(1 shl 11),

    /** Extend hit box to the left-most and right-most edges (bypass the indented area). */
    SpanFullWidth(1 shl 12),

    /** (WIP) Nav: left direction may move to this TreeNode() from any of its child (items submitted between TreeNode and TreePop)   */
    NavLeftJumpsBackHere(1 shl 13),
    CollapsingHeader(Framed or NoTreePushOnOpen or NoAutoOpenOnLog),

    // [Internal]

    _ClipLabelForTrailingButton(1 shl 20);

    infix fun and(b: TreeNodeFlag): TreeNodeFlags = i and b.i
    infix fun and(b: TreeNodeFlags): TreeNodeFlags = i and b
    infix fun or(b: TreeNodeFlag): TreeNodeFlags = i or b.i
    infix fun or(b: TreeNodeFlags): TreeNodeFlags = i or b
    infix fun xor(b: TreeNodeFlag): TreeNodeFlags = i xor b.i
    infix fun xor(b: TreeNodeFlags): TreeNodeFlags = i xor b
    infix fun wo(b: TreeNodeFlags): TreeNodeFlags = and(b.inv())
}

infix fun TreeNodeFlags.and(b: TreeNodeFlag): TreeNodeFlags = and(b.i)
infix fun TreeNodeFlags.or(b: TreeNodeFlag): TreeNodeFlags = or(b.i)
infix fun TreeNodeFlags.xor(b: TreeNodeFlag): TreeNodeFlags = xor(b.i)
infix fun TreeNodeFlags.has(b: TreeNodeFlag): Boolean = and(b.i) != 0
infix fun TreeNodeFlags.hasnt(b: TreeNodeFlag): Boolean = and(b.i) == 0
infix fun TreeNodeFlags.wo(b: TreeNodeFlag): TreeNodeFlags = and(b.i.inv())


typealias PopupFlags = Int

/** Flags for OpenPopup*(), BeginPopupContext*(), IsPopupOpen() functions.
 *  - To be backward compatible with older API which took an 'int mouse_button = 1' argument, we need to treat
 *    small flags values as a mouse button index, so we encode the mouse button in the first few bits of the flags.
 *    It is therefore guaranteed to be legal to pass a mouse button index in ImGuiPopupFlags.
 *  - For the same reason, we exceptionally default the ImGuiPopupFlags argument of BeginPopupContextXXX functions to 1 instead of 0.
 *  - Multiple buttons currently cannot be combined/or-ed in those functions (we could allow it later). */
enum class PopupFlag(@JvmField val i: PopupFlags) {

    None(0),

    /** For BeginPopupContext*(): open on Left Mouse release. Guaranteed to always be == 0 (same as ImGuiMouseButton_Left) */
    MouseButtonLeft(0),

    /** For BeginPopupContext*(): open on Right Mouse release. Guaranteed to always be == 1 (same as ImGuiMouseButton_Right) */
    MouseButtonRight(1),

    /** For BeginPopupContext*(): open on Middle Mouse release. Guaranteed to always be == 2 (same as ImGuiMouseButton_Middle) */
    MouseButtonMiddle(2),
    MouseButtonMask_(0x1F),
    MouseButtonDefault_(1),

    /** For OpenPopup*(), BeginPopupContext*(): don't open if there's already a popup at the same level of the popup stack */
    NoOpenOverExistingPopup(1 shl 5),

    /** For BeginPopupContextWindow(): don't return true when hovering items, only when hovering empty space */
    NoOpenOverItems(1 shl 6),

    /** For IsPopupOpen(): ignore the ImGuiID parameter and test for any popup. */
    AnyPopupId(1 shl 7),

    /** For IsPopupOpen(): search/test at any level of the popup stack (default test in the current level) */
    AnyPopupLevel(1 shl 8),
    AnyPopup(AnyPopupId or AnyPopupLevel);

    infix fun and(b: PopupFlag): PopupFlags = i and b.i
    infix fun and(b: PopupFlags): PopupFlags = i and b
    infix fun or(b: PopupFlag): PopupFlags = i or b.i
    infix fun or(b: PopupFlags): PopupFlags = i or b
    infix fun xor(b: PopupFlag): PopupFlags = i xor b.i
    infix fun xor(b: PopupFlags): PopupFlags = i xor b
    infix fun wo(b: PopupFlags): PopupFlags = and(b.inv())
}

infix fun PopupFlags.and(b: PopupFlag): PopupFlags = and(b.i)
infix fun PopupFlags.or(b: PopupFlag): PopupFlags = or(b.i)
infix fun PopupFlags.xor(b: PopupFlag): PopupFlags = xor(b.i)
infix fun PopupFlags.has(b: PopupFlag): Boolean = and(b.i) != 0
infix fun PopupFlags.hasnt(b: PopupFlag): Boolean = and(b.i) == 0
infix fun PopupFlags.wo(b: PopupFlag): PopupFlags = and(b.i.inv())


typealias SelectableFlags = Int

/** Flags for ImGui::Selectable()   */
enum class SelectableFlag(@JvmField val i: SelectableFlags) {

    None(0),

    /** Clicking this don't close parent popup window   */
    DontClosePopups(1 shl 0),

    /** Selectable frame can span all columns (text will still fit in current column)   */
    SpanAllColumns(1 shl 1),

    /** Generate press events on double clicks too  */
    AllowDoubleClick(1 shl 2),

    /** Cannot be selected, display grayed out text */
    Disabled(1 shl 3),

    /** (WIP) Hit testing to allow subsequent widgets to overlap this one */
    AllowItemOverlap(1 shl 4),

    // [Internal] NB: need to be in sync with last value of ImGuiSelectableFlags_

    /** private  */
    _NoHoldingActiveId(1 shl 20),

    /** private
     *  Override button behavior to react on Click (default is Click+Release) */
    _SelectOnClick(1 shl 21),

    /** private
     *  Override button behavior to react on Release (default is Click+Release) */
    _SelectOnRelease(1 shl 22),

    /** Span all avail width even if we declared less for layout purpose. FIXME: We may be able to remove this (added in 6251d379, 2bcafc86 for menus)  */
    _SpanAvailWidth(1 shl 23),

    /** Always show active when held, even is not hovered. This concept could probably be renamed/formalized somehow. */
    _DrawHoveredWhenHeld(1 shl 24),

    /** Set Nav/Focus ID on mouse hover (used by MenuItem) */
    _SetNavIdOnHover(1 shl 25),

    /** Disable padding each side with ItemSpacing * 0.5f */
    _NoPadWithHalfSpacing(1 shl 26);

    infix fun and(b: SelectableFlag): SelectableFlags = i and b.i
    infix fun and(b: SelectableFlags): SelectableFlags = i and b
    infix fun or(b: SelectableFlag): SelectableFlags = i or b.i
    infix fun or(b: SelectableFlags): SelectableFlags = i or b
    infix fun xor(b: SelectableFlag): SelectableFlags = i xor b.i
    infix fun xor(b: SelectableFlags): SelectableFlags = i xor b
    infix fun wo(b: SelectableFlags): SelectableFlags = and(b.inv())
}

infix fun SelectableFlags.and(b: SelectableFlag): SelectableFlags = and(b.i)
infix fun SelectableFlags.or(b: SelectableFlag): SelectableFlags = or(b.i)
infix fun SelectableFlags.xor(b: SelectableFlag): SelectableFlags = xor(b.i)
infix fun SelectableFlags.has(b: SelectableFlag): Boolean = and(b.i) != 0
infix fun SelectableFlags.hasnt(b: SelectableFlag): Boolean = and(b.i) == 0
infix fun SelectableFlags.wo(b: SelectableFlag): SelectableFlags = and(b.i.inv())


typealias ComboFlags = Int

/** Flags: for BeginCombo() */
enum class ComboFlag(@JvmField val i: ComboFlags) {
    None(0),

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
    HeightMask_(HeightSmall or HeightRegular or HeightLarge or HeightLargest);

    infix fun and(b: ComboFlag): ComboFlags = i and b.i
    infix fun and(b: ComboFlags): ComboFlags = i and b
    infix fun or(b: ComboFlag): ComboFlags = i or b.i
    infix fun or(b: ComboFlags): ComboFlags = i or b
    infix fun xor(b: ComboFlag): ComboFlags = i xor b.i
    infix fun xor(b: ComboFlags): ComboFlags = i xor b
    infix fun wo(b: ComboFlags): ComboFlags = and(b.inv())
}

infix fun ComboFlags.and(b: ComboFlag): ComboFlags = and(b.i)
infix fun ComboFlags.or(b: ComboFlag): ComboFlags = or(b.i)
infix fun ComboFlags.xor(b: ComboFlag): ComboFlags = xor(b.i)
infix fun ComboFlags.has(b: ComboFlag): Boolean = and(b.i) != 0
infix fun ComboFlags.hasnt(b: ComboFlag): Boolean = and(b.i) == 0
infix fun ComboFlags.wo(b: ComboFlag): ComboFlags = and(b.i.inv())


typealias TabBarFlags = Int

/** Flags for ImGui::BeginTabBar() */
enum class TabBarFlag(@JvmField val i: TabBarFlags) {
    None(0),

    /** Allow manually dragging tabs to re-order them + New tabs are appended at the end of list */
    Reorderable(1 shl 0),

    /** Automatically select new tabs when they appear */
    AutoSelectNewTabs(1 shl 1),

    /** Disable buttons to open the tab list popup */
    TabListPopupButton(1 shl 2),

    /** Disable behavior of closing tabs (that are submitted with p_open != NULL) with middle mouse button.
     *  You can still repro this behavior on user's side with if (IsItemHovered() && IsMouseClicked(2)) *p_open = false. */
    NoCloseWithMiddleMouseButton(1 shl 3),

    /** Disable scrolling buttons (apply when fitting policy is ImGuiTabBarFlags_FittingPolicyScroll) */
    NoTabListScrollingButtons(1 shl 4),

    /** Disable tooltips when hovering a tab */
    NoTooltip(1 shl 5),

    /** Resize tabs when they don't fit */
    FittingPolicyResizeDown(1 shl 6),

    /** Add scroll buttons when tabs don't fit */
    FittingPolicyScroll(1 shl 7),
    FittingPolicyMask_(FittingPolicyResizeDown or FittingPolicyScroll),
    FittingPolicyDefault_(FittingPolicyResizeDown.i),

    // Private

    /** Part of a dock node [we don't use this in the master branch but it facilitate branch syncing to keep this around] */
    _DockNode(1 shl 20),
    _IsFocused(1 shl 21),

    /** FIXME: Settings are handled by the docking system, this only request the tab bar to mark settings dirty when reordering tabs, */
    _SaveSettings(1 shl 22);

    infix fun and(b: TabBarFlag): TabBarFlags = i and b.i
    infix fun and(b: TabBarFlags): TabBarFlags = i and b
    infix fun or(b: TabBarFlag): TabBarFlags = i or b.i
    infix fun or(b: TabBarFlags): TabBarFlags = i or b
    infix fun xor(b: TabBarFlag): TabBarFlags = i xor b.i
    infix fun xor(b: TabBarFlags): TabBarFlags = i xor b
    infix fun wo(b: TabBarFlags): TabBarFlags = and(b.inv())
}

infix fun TabBarFlags.and(b: TabBarFlag): TabBarFlags = and(b.i)
infix fun TabBarFlags.or(b: TabBarFlag): TabBarFlags = or(b.i)
infix fun TabBarFlags.xor(b: TabBarFlag): TabBarFlags = xor(b.i)
infix fun TabBarFlags.has(b: TabBarFlag): Boolean = and(b.i) != 0
infix fun TabBarFlags.hasnt(b: TabBarFlag): Boolean = and(b.i) == 0
infix fun TabBarFlags.wo(b: TabBarFlag): TabBarFlags = and(b.i.inv())


typealias TabItemFlags = Int

/** Flags for ImGui::BeginTabItem() */
enum class TabItemFlag(@JvmField val i: TabItemFlags) {
    None(0),

    /** Append '*' to title without affecting the ID, as a convenience to avoid using the ### operator. Also: tab is selected on closure and closure is deferred by one frame to allow code to undo it without flicker. */
    UnsavedDocument(1 shl 0),

    /** Trigger flag to programmatically make the tab selected when calling BeginTabItem() */
    SetSelected(1 shl 1),

    /** Disable behavior of closing tabs (that are submitted with p_open != NULL) with middle mouse button. You can still repro this behavior on user's side with if (IsItemHovered() && IsMouseClicked(2)) *p_open = false. */
    NoCloseWithMiddleMouseButton(1 shl 2),

    /** Don't call PushID(tab->ID)/PopID() on BeginTabItem()/EndTabItem() */
    NoPushId(1 shl 3),

    /** Disable tooltip for the given tab */
    NoTooltip(1 shl 4),

    /** Disable reordering this tab or having another tab cross over this tab */
    NoReorder                     (1 shl 5),

    /**  Enforce the tab position to the left of the tab bar (after the tab list popup button) */
    Leading (1 shl 6),

    /**  Enforce the tab position to the right of the tab bar (before the scrolling buttons) */
    Trailing (1 shl 7),

    // [Internal]

    /** Track whether p_open was set or not (we'll need this info on the next frame to recompute ContentWidth during layout) */
    _NoCloseButton(1 shl 20),

    /** Used by TabItemButton, change the tab item behavior to mimic a button */
    _Button(1 shl 21);

    infix fun and(b: TabItemFlag): TabItemFlags = i and b.i
    infix fun and(b: TabItemFlags): TabItemFlags = i and b
    infix fun or(b: TabItemFlag): TabItemFlags = i or b.i
    infix fun or(b: TabItemFlags): TabItemFlags = i or b
    infix fun xor(b: TabItemFlag): TabItemFlags = i xor b.i
    infix fun xor(b: TabItemFlags): TabItemFlags = i xor b
    infix fun wo(b: TabItemFlags): TabItemFlags = and(b.inv())
}

infix fun TabItemFlags.and(b: TabItemFlag): TabItemFlags = and(b.i)
infix fun TabItemFlags.or(b: TabItemFlag): TabItemFlags = or(b.i)
infix fun TabItemFlags.xor(b: TabItemFlag): TabItemFlags = xor(b.i)
infix fun TabItemFlags.has(b: TabItemFlag): Boolean = and(b.i) != 0
infix fun TabItemFlags.hasnt(b: TabItemFlag): Boolean = and(b.i) == 0
infix fun TabItemFlags.wo(b: TabItemFlag): TabItemFlags = and(b.i.inv())


typealias FocusedFlags = Int

/** Flags for ImGui::IsWindowFocused() */
enum class FocusedFlag(@JvmField val i: FocusedFlags) {
    None(0),

    /** isWindowFocused(): Return true if any children of the window is focused */
    ChildWindows(1 shl 0),

    /** isWindowFocused(): Test from root window (top most parent of the current hierarchy) */
    RootWindow(1 shl 1),

    /** IsWindowFocused(): Return true if any window is focused.
     *  Important: If you are trying to tell how to dispatch your low-level inputs, do NOT use this. Use 'io.WantCaptureMouse' instead! Please read the FAQ! */
    AnyWindow(1 shl 2),
    RootAndChildWindows(RootWindow or ChildWindows);

    infix fun and(b: FocusedFlag): FocusedFlags = i and b.i
    infix fun and(b: FocusedFlags): FocusedFlags = i and b
    infix fun or(b: FocusedFlag): FocusedFlags = i or b.i
    infix fun or(b: FocusedFlags): FocusedFlags = i or b
    infix fun xor(b: FocusedFlag): FocusedFlags = i xor b.i
    infix fun xor(b: FocusedFlags): FocusedFlags = i xor b
    infix fun wo(b: FocusedFlags): FocusedFlags = and(b.inv())
}

infix fun FocusedFlags.and(b: FocusedFlag): FocusedFlags = and(b.i)
infix fun FocusedFlags.or(b: FocusedFlag): FocusedFlags = or(b.i)
infix fun FocusedFlags.xor(b: FocusedFlag): FocusedFlags = xor(b.i)
infix fun FocusedFlags.has(b: FocusedFlag): Boolean = and(b.i) != 0
infix fun FocusedFlags.hasnt(b: FocusedFlag): Boolean = and(b.i) == 0
infix fun FocusedFlags.wo(b: FocusedFlag): FocusedFlags = and(b.i.inv())


typealias HoveredFlags = Int

/** Flags: for IsItemHovered(), IsWindowHovered() etc.
 *  Note: if you are trying to check whether your mouse should be dispatched to Dear ImGui or to your app, you should use 'io.WantCaptureMouse' instead! Please read the FAQ!
 *  Note: windows with the ImGuiWindowFlags_NoInputs flag are ignored by IsWindowHovered() calls.*/
enum class HoveredFlag(@JvmField val i: HoveredFlags) {
    /** Return true if directly over the item/window, not obstructed by another window, not obstructed by an active
     *  popup or modal blocking inputs under them.  */
    None(0),

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

    /** Return true even if the position is obstructed or overlapped by another window,   */
    AllowWhenOverlapped(1 shl 6),

    /** Return true even if the item is disabled */
    AllowWhenDisabled(1 shl 7),
    RectOnly(AllowWhenBlockedByPopup.i or AllowWhenBlockedByActiveItem.i or AllowWhenOverlapped.i),
    RootAndChildWindows(RootWindow or ChildWindows);

    infix fun and(b: HoveredFlag): HoveredFlags = i and b.i
    infix fun and(b: HoveredFlags): HoveredFlags = i and b
    infix fun or(b: HoveredFlag): HoveredFlags = i or b.i
    infix fun or(b: HoveredFlags): HoveredFlags = i or b
    infix fun xor(b: HoveredFlag): HoveredFlags = i xor b.i
    infix fun xor(b: HoveredFlags): HoveredFlags = i xor b
    infix fun wo(b: HoveredFlags): HoveredFlags = and(b.inv())
}

infix fun HoveredFlags.and(b: HoveredFlag): HoveredFlags = and(b.i)
infix fun HoveredFlags.or(b: HoveredFlag): HoveredFlags = or(b.i)
infix fun HoveredFlags.xor(b: HoveredFlag): HoveredFlags = xor(b.i)
infix fun HoveredFlags.has(b: HoveredFlag): Boolean = and(b.i) != 0
infix fun HoveredFlags.hasnt(b: HoveredFlag): Boolean = and(b.i) == 0
infix fun HoveredFlags.wo(b: HoveredFlag): HoveredFlags = and(b.i.inv())


typealias DragDropFlags = Int

/** Flags for beginDragDropSource(), acceptDragDropPayload() */
enum class DragDropFlag(@JvmField val i: DragDropFlags) {
    // BeginDragDropSource() flags
    None(0),

    /** By default), a successful call to beginDragDropSource opens a tooltip so you can display a preview or
     *  description of the source contents. This flag disable this behavior. */
    SourceNoPreviewTooltip(1 shl 0),

    /** By default, when dragging we clear data so that IsItemHovered() will return false,
     *  to avoid subsequent user code submitting tooltips.
     *  This flag disable this behavior so you can still call IsItemHovered() on the source item. */
    SourceNoDisableHover(1 shl 1),

    /** Disable the behavior that allows to open tree nodes and collapsing header by holding over them while dragging
     *  a source item. */
    SourceNoHoldToOpenOthers(1 shl 2),

    /** Allow items such as text()), Image() that have no unique identifier to be used as drag source),
     *  by manufacturing a temporary identifier based on their window-relative position.
     *  This is extremely unusual within the dear imgui ecosystem and so we made it explicit. */
    SourceAllowNullID(1 shl 3),

    /** External source (from outside of dear imgui), won't attempt to read current item/window info. Will always return true.
     *  Only one Extern source can be active simultaneously.    */
    SourceExtern(1 shl 4),

    /** Automatically expire the payload if the source cease to be submitted (otherwise payloads are persisting while being dragged) */
    SourceAutoExpirePayload(1 shl 5),
    // AcceptDragDropPayload() flags
    /** AcceptDragDropPayload() will returns true even before the mouse button is released.
     *  You can then call isDelivery() to test if the payload needs to be delivered. */
    AcceptBeforeDelivery(1 shl 10),

    /** Do not draw the default highlight rectangle when hovering over target. */
    AcceptNoDrawDefaultRect(1 shl 11),

    /** Request hiding the BeginDragDropSource tooltip from the BeginDragDropTarget site. */
    AcceptNoPreviewTooltip(1 shl 12),

    /** For peeking ahead and inspecting the payload before delivery. */
    AcceptPeekOnly(AcceptBeforeDelivery or AcceptNoDrawDefaultRect);

    infix fun and(b: DragDropFlag): DragDropFlags = i and b.i
    infix fun and(b: DragDropFlags): DragDropFlags = i and b
    infix fun or(b: DragDropFlag): DragDropFlags = i or b.i
    infix fun or(b: DragDropFlags): DragDropFlags = i or b
    infix fun xor(b: DragDropFlag): DragDropFlags = i xor b.i
    infix fun xor(b: DragDropFlags): DragDropFlags = i xor b
    infix fun wo(b: DragDropFlags): DragDropFlags = and(b.inv())
}

infix fun DragDropFlags.and(b: DragDropFlag): DragDropFlags = and(b.i)
infix fun DragDropFlags.or(b: DragDropFlag): DragDropFlags = or(b.i)
infix fun DragDropFlags.xor(b: DragDropFlag): DragDropFlags = xor(b.i)
infix fun DragDropFlags.has(b: DragDropFlag): Boolean = and(b.i) != 0
infix fun DragDropFlags.hasnt(b: DragDropFlag): Boolean = and(b.i) == 0
infix fun DragDropFlags.wo(b: DragDropFlag): DragDropFlags = and(b.i.inv())

// Standard Drag and Drop payload types. Types starting with '_' are defined by Dear ImGui.
/** float[3]: Standard type for colors, without alpha. User code may use this type. */
val PAYLOAD_TYPE_COLOR_3F = "_COL3F"

/** float[4]: Standard type for colors. User code may use this type. */
val PAYLOAD_TYPE_COLOR_4F = "_COL4F"

/** A primary data type */
enum class DataType(val name_: String) {
    Byte("S8"), Ubyte("U8"),
    Short("S16"), Ushort("U16"),
    Int("S32"), Uint("U32"),
    Long("S64"), Ulong("U64"),
    Float("float"), Double("double"),

    _String("[internal] String"), _Pointer("[internal] Pointer"), _ID("[internal] ID");

    @JvmField
    val i = ordinal
}

/** A cardinal direction */
enum class Dir {
    None, Left, Right, Up, Down;

    @JvmField
    val i = ordinal - 1


    companion object {
        infix fun of(i: Int) = values().first { it.i == i }
        val COUNT = Down.i + 1
    }
}

infix fun Int.shl(b: Dir) = shl(b.i)

/** User fill ImGuiio.KeyMap[] array with indices into the ImGuiio.KeysDown[512] array
 *
 *  A key identifier (ImGui-side enum) */
enum class Key {
    Tab, LeftArrow, RightArrow, UpArrow, DownArrow, PageUp, PageDown, Home, End, Insert, Delete, Backspace,
    Space, Enter, Escape, KeyPadEnter,

    /** for text edit CTRL+A: select all */
    A,

    /** for text edit CTRL+C: copy */
    C,

    /** for text edit CTRL+V: paste */
    V,

    /** for text edit CTRL+X: cut */
    X,

    /** for text edit CTRL+Y: redo */
    Y,

    /** for text edit CTRL+Z: undo */
    Z,
    Count;

    companion object {
        val COUNT = values().size
    }

    @JvmField
    val i = ordinal

    /** JVM implementation of IsKeyPressedMap   */
    fun isPressed(repeat: Boolean = true) = isKeyPressed(io.keyMap[i], repeat)

    val isPressed: Boolean
        get() = isPressed(true)

    val isDown: Boolean
        get() = isKeyDown(io.keyMap[i])

    /** map ImGuiKey_* values into user's key index. == io.KeyMap[key]   */
    val index get() = i
}

infix fun Long.shl(key: Key) = shl(key.i)

// for IO.keyMap

operator fun IntArray.set(index: Key, value: Int) {
    this[index.i] = value
}

operator fun IntArray.get(index: Key): Int = get(index.i)


// To test io.KeyMods (which is a combination of individual fields io.KeyCtrl, io.KeyShift, io.KeyAlt set by user/back-end)
enum class KeyMod(val i: KeyModFlags) {
    None(0),
    Ctrl(1 shl 0),
    Shift(1 shl 1),
    Alt(1 shl 2),
    Super(1 shl 3);

    infix fun or(b: KeyMod): KeyModFlags = i or b.i
}

typealias KeyModFlags = Int


/** Gamepad/Keyboard navigation
 *  Keyboard: Set io.configFlags |= NavFlags.EnableKeyboard to enable. ::newFrame() will automatically fill io.navInputs[]
 *  based on your io.keysDown[] + io.keyMap[] arrays.
 *  Gamepad:  Set io.configFlags |= NavFlags.EnableGamepad to enable. Fill the io.navInputs[] fields before calling
 *  ::newFrame(). Note that io.navInputs[] is cleared by ::endFrame().
 *  Read instructions in imgui.cpp for more details.
 *
 *  An input identifier for navigation */
enum class NavInput {
    // Gamepad Mapping
    /** activate / open / toggle / tweak value       // e.g. Cross (PS4), A (Xbox), B (Switch), Space (Keyboard)   */
    Activate,

    /** cancel / close / exit                        // e.g. Circle (PS4), B (Xbox), A (Switch), Escape (Keyboard)  */
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
        Keyboard behavior that have no corresponding gamepad mapping (e.g. CTRL + TAB) will be directly reading from
        io.keysDown[] instead of io.navInputs[]. */

    /** toggle menu = io.keyAlt */
    _KeyMenu,

    /** move left = Arrow keys  */
    _KeyLeft,

    /** move right = Arrow keys  */
    _KeyRight,

    /** move up = Arrow keys  */
    _KeyUp,

    /** move down = Arrow keys  */
    _KeyDown,
    Count;

    @JvmField
    val i = ordinal

    companion object {
        val COUNT = values().size
        val InternalStart = _KeyMenu.i
    }

    /** Equivalent of isKeyDown() for NavInputs[]
     *  ~IsNavInputDown */ // JVM TODO check for semantic Key.isPressed/Down
    fun isDown(): Boolean = io.navInputs[i] > 0f

    /** Equivalent of isKeyPressed() for NavInputs[]
     *  ~IsNavInputTest  */
    fun isTest(mode: InputReadMode): Boolean = getNavInputAmount(this, mode) > 0f

    /** ~IsNavInputPressedAnyOfTwo  */
    fun isPressedAnyOfTwo(n2: NavInput, mode: InputReadMode): Boolean = (getNavInputAmount(this, mode) + getNavInputAmount(n2, mode)) > 0f
}

infix fun Int.shl(f: NavInput) = shl(f.i)

// for IO.navInputs

operator fun FloatArray.set(index: NavInput, value: Float) {
    this[index.i] = value
}

operator fun FloatArray.get(index: NavInput): Float = get(index.i)


typealias ConfigFlags = Int

/** Configuration flags stored in io.configFlags
 *
 *  Flags: for io.ConfigFlags   */
enum class ConfigFlag(@JvmField val i: ConfigFlags) {
    None(0),

    /** Master keyboard navigation enable flag. NewFrame() will automatically fill io.NavInputs[] based on io.KeysDown[]. */
    NavEnableKeyboard(1 shl 0),

    /** Master gamepad navigation enable flag. This is mostly to instruct your imgui back-end to fill io.NavInputs[].
     *  Back-end also needs to set ImGuiBackendFlags_HasGamepad. */
    NavEnableGamepad(1 shl 1),

    /** Instruct navigation to move the mouse cursor. May be useful on TV/console systems where moving a virtual mouse is awkward.
     *  Will update io.MousePos and set io.wantSetMousePos=true. If enabled you MUST honor io.wantSetMousePos requests in your binding,
     *  otherwise ImGui will react as if the mouse is jumping around back and forth. */
    NavEnableSetMousePos(1 shl 2),

    /** Instruct navigation to not set the io.WantCaptureKeyboard flag when io.NavActive is set. */
    NavNoCaptureKeyboard(1 shl 3),

    /** Instruct imgui to clear mouse position/buttons in NewFrame(). This allows ignoring the mouse information set by the back-end. */
    NoMouse(1 shl 4),

    /** Request back-end to not alter mouse cursor configuration.
     *  Use if the back-end cursor changes are interfering with yours and you don't want to use setMouseCursor() to change mouse cursor.
     *  You may want to honor requests from imgui by reading ::mouseCursor yourself instead. */
    NoMouseCursorChange(1 shl 5),

    /** JVM custom, request back-end to not read the mouse status allowing you to provide your own custom input */
    NoMouseUpdate(1 shl 12),

    /** JVM custom */
    NoKeyboardUpdate(1 shl 13),

    /*  User storage (to allow your back-end/engine to communicate to code that may be shared between multiple projects.
        Those flags are not used by core Dear ImGui)     */

    /** Application is SRGB-aware. */
    IsSRGB(1 shl 20),

    /** Application is using a touch screen instead of a mouse. */
    IsTouchScreen(1 shl 21);

    infix fun and(b: ConfigFlag): ConfigFlags = i and b.i
    infix fun and(b: ConfigFlags): ConfigFlags = i and b
    infix fun or(b: ConfigFlag): ConfigFlags = i or b.i
    infix fun or(b: ConfigFlags): ConfigFlags = i or b
    infix fun xor(b: ConfigFlag): ConfigFlags = i xor b.i
    infix fun xor(b: ConfigFlags): ConfigFlags = i xor b
    infix fun wo(b: ConfigFlags): ConfigFlags = and(b.inv())
}

infix fun ConfigFlags.and(b: ConfigFlag): ConfigFlags = and(b.i)
infix fun ConfigFlags.or(b: ConfigFlag): ConfigFlags = or(b.i)
infix fun ConfigFlags.xor(b: ConfigFlag): ConfigFlags = xor(b.i)
infix fun ConfigFlags.has(b: ConfigFlag): Boolean = and(b.i) != 0
infix fun ConfigFlags.hasnt(b: ConfigFlag): Boolean = and(b.i) == 0
infix fun ConfigFlags.wo(b: ConfigFlag): ConfigFlags = and(b.i.inv())


typealias BackendFlags = Int

/** Back-end capabilities flags stored in io.BackendFlag. Set by imgui_impl_xxx or custom back-end.
 *
 *  Flags: for io.BackendFlags  */
enum class BackendFlag(@JvmField val i: BackendFlags) {
    None(0),

    /** Back-end Platform supports gamepad and currently has one connected. */
    HasGamepad(1 shl 0),

    /** Back-end Platform supports honoring GetMouseCursor() value to change the OS cursor shape. */
    HasMouseCursors(1 shl 1),

    /** Back-end Platform supports io.WantSetMousePos requests to reposition the OS mouse position (only used if ImGuiConfigFlags_NavEnableSetMousePos is set). */
    HasSetMousePos(1 shl 2),

    /** Back-end Platform supports ImDrawCmd::VtxOffset. This enables output of large meshes (64K+ vertices) while still using 16-bit indices. */
    RendererHasVtxOffset(1 shl 3);

    infix fun and(b: BackendFlag): BackendFlags = i and b.i
    infix fun and(b: BackendFlags): BackendFlags = i and b
    infix fun or(b: BackendFlag): BackendFlags = i or b.i
    infix fun or(b: BackendFlags): BackendFlags = i or b
    infix fun xor(b: BackendFlag): BackendFlags = i xor b.i
    infix fun xor(b: BackendFlags): BackendFlags = i xor b
    infix fun wo(b: BackendFlags): BackendFlags = and(b.inv())
}

infix fun BackendFlags.and(b: BackendFlag): BackendFlags = and(b.i)
infix fun BackendFlags.or(b: BackendFlag): BackendFlags = or(b.i)
infix fun BackendFlags.xor(b: BackendFlag): BackendFlags = xor(b.i)
infix fun BackendFlags.has(b: BackendFlag): Boolean = and(b.i) != 0
infix fun BackendFlags.hasnt(b: BackendFlag): Boolean = and(b.i) == 0
infix fun BackendFlags.wo(b: BackendFlag): BackendFlags = and(b.i.inv())

/** Enumeration for PushStyleColor() / PopStyleColor()  */
/** A color identifier for styling */
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

    /** Header* colors are used for CollapsingHeader, TreeNode, Selectable, MenuItem */
    Header,
    HeaderHovered,
    HeaderActive,
    Separator,
    SeparatorHovered,
    SeparatorActive,
    ResizeGrip,
    ResizeGripHovered,
    ResizeGripActive,
    Tab,
    TabHovered,
    TabActive,
    TabUnfocused,
    TabUnfocusedActive,
    PlotLines,
    PlotLinesHovered,
    PlotHistogram,
    PlotHistogramHovered,
    TextSelectedBg,
    DragDropTarget,

    /** Gamepad/keyboard: current highlighted item  */
    NavHighlight,

    /** Highlight window when using CTRL+TAB */
    NavWindowingHighlight,

    /** Darken/colorize entire screen behind the CTRL+TAB window list, when active */
    NavWindowingDimBg,

    /** Darken/colorize entire screen behind a modal window, when one is active; */
    ModalWindowDimBg;

    @JvmField
    val i = ordinal

    val u32 get() = getColorU32(i, alphaMul = 1f)

    companion object {
        val COUNT = values().size
        fun of(i: Int) = values()[i]
    }
}

/** for style.colors    */
operator fun ArrayList<Vec4>.get(idx: Col): Vec4 = this[idx.i]

operator fun ArrayList<Vec4>.set(idx: Col, vec: Vec4) = this[idx.i] put vec

/** Enumeration for PushStyleVar() / PopStyleVar() to temporarily modify the ImGuiStyle structure.
 *  - The enum only refers to fields of ImGuiStyle which makes sense to be pushed/popped inside UI code.
 *    During initialization or between frames, feel free to just poke into ImGuiStyle directly.
 *  - Tip: Use your programming IDE navigation facilities on the names in the _second column_ below to find the actual members and their description.
 *    In Visual Studio IDE: CTRL+comma ("Edit.NavigateTo") can follow symbols in comments, whereas CTRL+F12 ("Edit.GoToImplementation") cannot.
 *    With Visual Assist installed: ALT+G ("VAssistX.GoToImplementation") can also follow symbols in comments.
 *  - When changing this enum, you need to update the associated internal table GStyleVarInfo[] accordingly. This is where we link enum values to members offset/type.
 *
 *  A variable identifier for styling   */
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

    /** float   */
    GrabRounding,

    /** float */
    TabRounding,

    /** vec2  */
    ButtonTextAlign,

    /** vec2  */
    SelectableTextAlign;

    @JvmField
    val i = ordinal
}


typealias ColorEditFlags = Int

/** Flags for ColorEdit3() / ColorEdit4() / ColorPicker3() / ColorPicker4() / ColorButton()
 *
 *  Flags: for ColorEdit4(), ColorPicker4() etc.    */
enum class ColorEditFlag(@JvmField val i: ColorEditFlags) {

    None(0),

    /** ColorEdit, ColorPicker, ColorButton: ignore Alpha component (will only read 3 components from the input pointer). */
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

    /** ColorEdit: disable drag and drop target. ColorButton: disable drag and drop source. */
    NoDragDrop(1 shl 9),

    /** ColorButton: disable border (which is enforced by default) */
    NoBorder(1 shl 10),

    // User Options (right-click on widget to change some of them).

    /** ColorEdit, ColorPicker: show vertical alpha bar/gradient in picker. */
    AlphaBar(1 shl 16),

    /** ColorEdit, ColorPicker, ColorButton: display preview as a transparent color over a checkerboard, instead of opaque. */
    AlphaPreview(1 shl 17),

    /** ColorEdit, ColorPicker, ColorButton: display half opaque / half checkerboard, instead of opaque.    */
    AlphaPreviewHalf(1 shl 18),

    /** (WIP) ColorEdit: Currently only disable 0.0f..1.0f limits in RGBA edition (note: you probably want to use
     *  ColorEditFlags.Float flag as well). */
    HDR(1 shl 19),

    /** [Display] ColorEdit: override _display_ type among RGB/HSV/Hex. ColorPicker: select any combination using one or more of RGB/HSV/Hex.    */
    DisplayRGB(1 shl 20),

    /** [Display] ColorEdit: override _display_ type among RGB/HSV/Hex. ColorPicker: select any combination using one or more of RGB/HSV/Hex.    */
    DisplayHSV(1 shl 21),

    /** [Display] ColorEdit: override _display_ type among RGB/HSV/Hex. ColorPicker: select any combination using one or more of RGB/HSV/Hex.    */
    DisplayHEX(1 shl 22),

    /** [DataType] ColorEdit, ColorPicker, ColorButton: _display_ values formatted as 0..255.   */
    Uint8(1 shl 23),

    /** [DataType] ColorEdit, ColorPicker, ColorButton: _display_ values formatted as 0.0f..1.0f floats instead of 0..255 integers.
     *  No round-trip of value via integers.    */
    Float(1 shl 24),

    /** [Picker]     // ColorPicker: bar for Hue, rectangle for Sat/Value. */
    PickerHueBar(1 shl 25),

    /** [Picker]     // ColorPicker: wheel for Hue, triangle for Sat/Value. */
    PickerHueWheel(1 shl 26),

    /** [Input]      // ColorEdit, ColorPicker: input and output data in RGB format. */
    InputRGB(1 shl 27),

    /** [Input]      // ColorEdit, ColorPicker: input and output data in HSV format. */
    InputHSV(1 shl 28),

    /** Defaults Options. You can set application defaults using SetColorEditOptions(). The intent is that you probably don't want to
     *  override them in most of your calls. Let the user choose via the option menu and/or call SetColorEditOptions() once during startup. */
    _OptionsDefault(Uint8 or DisplayRGB or InputRGB or PickerHueBar),

    // [Internal] Masks
    _DisplayMask(DisplayRGB or DisplayHSV or DisplayHEX),
    _DataTypeMask(Uint8 or Float),
    _PickerMask(PickerHueWheel or PickerHueBar),
    _InputMask(InputRGB or InputHSV);

    infix fun and(b: ColorEditFlag): ColorEditFlags = i and b.i
    infix fun and(b: ColorEditFlags): ColorEditFlags = i and b
    infix fun or(b: ColorEditFlag): ColorEditFlags = i or b.i
    infix fun or(b: ColorEditFlags): ColorEditFlags = i or b
    infix fun xor(b: ColorEditFlag): ColorEditFlags = i xor b.i
    infix fun xor(b: ColorEditFlags): ColorEditFlags = i xor b
    infix fun wo(b: ColorEditFlags): ColorEditFlags = and(b.inv())
}

infix fun ColorEditFlags.and(b: ColorEditFlag): ColorEditFlags = and(b.i)
infix fun ColorEditFlags.or(b: ColorEditFlag): ColorEditFlags = or(b.i)
infix fun ColorEditFlags.xor(b: ColorEditFlag): ColorEditFlags = xor(b.i)
infix fun ColorEditFlags.has(b: ColorEditFlag): Boolean = and(b.i) != 0
infix fun ColorEditFlags.hasnt(b: ColorEditFlag): Boolean = and(b.i) == 0
infix fun ColorEditFlags.wo(b: ColorEditFlag): ColorEditFlags = and(b.i.inv())


typealias SliderFlags = Int

/** Flags for DragFloat(), DragInt(), SliderFloat(), SliderInt() etc.
 *  We use the same sets of flags for DragXXX() and SliderXXX() functions as the features are the same and it makes it easier to swap them. */
enum class SliderFlag(val i: SliderFlags) {
    None(0),

    /** Clamp value to min/max bounds when input manually with CTRL+Click. By default CTRL+Click allows going out of bounds. */
    AlwaysClamp(1 shl 4),

    /** Make the widget logarithmic (linear otherwise). Consider using ImGuiDragFlags_NoRoundToFormat with this if using a format-string with small amount of digits. */
    Logarithmic(1 shl 5),

    /** Disable rounding underlying value to match precision of the display format string (e.g. %.3f values are rounded to those 3 digits) */
    NoRoundToFormat(1 shl 6),

    /** Disable CTRL+Click or Enter key allowing to input text directly into the widget */
    NoInput(1 shl 7),

    /** [Internal] We treat using those bits as being potentially a 'float power' argument from the previous API that
     *  has got miscast to this enum, and will trigger an assert if needed. */
    InvalidMask_(0x7000000F),

    /** [Private] Should this widget be orientated vertically? */
    _Vertical(1 shl 20),

    _ReadOnly(1 shl 21);

    infix fun and(b: SliderFlag): SliderFlags = i and b.i
    infix fun and(b: SliderFlags): SliderFlags = i and b
    infix fun or(b: SliderFlag): SliderFlags = i or b.i
    infix fun or(b: SliderFlags): SliderFlags = i or b
    infix fun xor(b: SliderFlag): SliderFlags = i xor b.i
    infix fun xor(b: SliderFlags): SliderFlags = i xor b
    infix fun wo(b: SliderFlags): SliderFlags = and(b.inv())
}

infix fun SliderFlags.and(b: SliderFlag): SliderFlags = and(b.i)
infix fun SliderFlags.or(b: SliderFlag): SliderFlags = or(b.i)
infix fun SliderFlags.xor(b: SliderFlag): SliderFlags = xor(b.i)
infix fun SliderFlags.has(b: SliderFlag): Boolean = and(b.i) != 0
infix fun SliderFlags.hasnt(b: SliderFlag): Boolean = and(b.i) == 0
infix fun SliderFlags.wo(b: SliderFlag): SliderFlags = and(b.i.inv())


/** Identify a mouse button.
 *  Those values are guaranteed to be stable and we frequently use 0/1 directly. Named enums provided for convenience. */
enum class MouseButton {
    None, Left, Right, Middle;

    val i = ordinal - 1 // starts at -1

    companion object {
        val COUNT = 5
        infix fun of(i: Int): MouseButton = values().find { it.i == i } ?: None
    }
}


/** Enumeration for GetMouseCursor()
 *  User code may request binding to display given cursor by calling SetMouseCursor(),
 *  which is why we have some cursors that are marked unused here
 *
 *  A mouse cursor identifier */
enum class MouseCursor {

    None,
    Arrow,

    /** When hovering over InputText, etc.  */
    TextInput,

    /** (Unused by Dear ImGui functions) */
    ResizeAll,

    /** When hovering over an horizontal border  */
    ResizeNS,

    /** When hovering over a vertical border or a column */
    ResizeEW,

    /** When hovering over the bottom-left corner of a window  */
    ResizeNESW,

    /** When hovering over the bottom-right corner of a window  */
    ResizeNWSE,

    /** (Unused by Dear ImGui functions. Use for e.g. hyperlinks) */
    Hand,

    /** When hovering something with disallowed interaction. Usually a crossed circle. */
    NotAllowed;

    @JvmField
    val i = ordinal - 1

    companion object {
        fun of(i: Int) = values().first { it.i == i }
        val COUNT = values().size - 1
    }
}

typealias CondFlags = Int

/** Enumeration for ImGui::SetWindow***(), SetNextWindow***(), SetNextItem***() functions
 *  Represent a condition.
 *  Important: Treat as a regular enum! Do NOT combine multiple values using binary operators!
 *  All the functions above treat 0 as a shortcut to Cond.Always.
 *
 *  Enum: A condition for many Set*() functions */
enum class Cond(@JvmField val i: CondFlags) {

    None(0),

    /** Set the variable    */
    Always(1 shl 0),

    /** Set the variable once per runtime session (only the first call will succeed)    */
    Once(1 shl 1),

    /** Set the variable if the object/window has no persistently saved data (no entry in .ini file)    */
    FirstUseEver(1 shl 2),

    /** Set the variable if the object/window is appearing after being hidden/inactive (or the first time) */
    Appearing(1 shl 3);

//    val isPowerOfTwo = i.isPowerOfTwo // JVM, kind of useless since it's used on cpp to avoid Cond masks

    infix fun and(b: Cond): CondFlags = i and b.i
    infix fun and(b: CondFlags): CondFlags = i and b
    infix fun or(b: Cond): CondFlags = i or b.i
    infix fun or(b: CondFlags): CondFlags = i or b
    infix fun xor(b: Cond): CondFlags = i xor b.i
    infix fun xor(b: CondFlags): CondFlags = i xor b
    infix fun wo(b: CondFlags): CondFlags = and(b.inv())
}

infix fun CondFlags.and(b: Cond): CondFlags = and(b.i)
infix fun CondFlags.or(b: Cond): CondFlags = or(b.i)
infix fun CondFlags.xor(b: Cond): CondFlags = xor(b.i)
infix fun CondFlags.has(b: Cond): Boolean = and(b.i) != 0
infix fun CondFlags.hasnt(b: Cond): Boolean = and(b.i) == 0
infix fun CondFlags.wo(b: Cond): CondFlags = and(b.i.inv())