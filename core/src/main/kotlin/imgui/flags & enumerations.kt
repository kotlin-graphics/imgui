package imgui

import glm_.vec4.Vec4
import imgui.ImGui.getColorU32
import imgui.ImGui.getTypematicRepeatRate
import imgui.ImGui.io
import imgui.api.g
import imgui.classes.KeyData
import imgui.internal.sections.InputFlag
import imgui.internal.sections.InputFlags
import imgui.internal.sections.has


//-----------------------------------------------------------------------------
// [SECTION] Flags & Enumerations
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

    /** Disable user collapsing window by double-clicking on it. Also referred to as Window Menu Button (e.g. within a docking node). */
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

    /** No gamepad/keyboard navigation within the window    */
    NoNavInputs(1 shl 18),

    /** No focusing toward this window with gamepad/keyboard navigation (e.g. skipped by CTRL+TAB)  */
    NoNavFocus(1 shl 19),

    /** Display a dot next to the title. When used in a tab/docking context, tab is selected when clicking the X + closure is not assumed (will wait for user to stop submitting the tab). Otherwise closure is assumed when pressing the X, so if you keep submitting the tab may reappear at end of tab bar. */
    UnsavedDocument(1 shl 20),

    NoNav(NoNavInputs or NoNavFocus),

    NoDecoration(NoTitleBar or NoResize or NoScrollbar or NoCollapse),

    NoInputs(NoMouseInputs or NoNavInputs or NoNavFocus),

    // [Internal]

    /** [BETA] On child window: allow gamepad/keyboard navigation to cross over parent border to this child or between sibling child windows. */
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

    /** Overwrite mode */
    AlwaysOverwrite(1 shl 13),

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

    /** For internal use by InputTextMultiline() */
    _Multiline(1 shl 26),

    /** For internal use by functions using InputText() before reformatting data */
    _NoMarkEdited(1 shl 27),

    /** For internal use by TempInputText(), will skip calling ItemAdd(). Require bounding-box to strictly match. */
    _MergedItem(1 shl 28);

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
operator fun InputTextFlags.minus(flag: InputTextFlag): InputTextFlags = wo(flag)
operator fun InputTextFlags.div(flag: InputTextFlag): InputTextFlags = or(flag)


typealias TreeNodeFlags = Int

/** Flags: for TreeNode(), TreeNodeEx(), CollapsingHeader()   */
enum class TreeNodeFlag(@JvmField val i: TreeNodeFlags) {

    None(0),

    /** Draw as selected    */
    Selected(1 shl 0),

    /** Draw frame with background (e.g. for CollapsingHeader)  */
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
    _NoHoldingActiveID(1 shl 20),

    /** (WIP) Auto-select when moved into. This is not exposed in public API as to handle multi-select and modifiers we will need user to explicitly control focus scope. May be replaced with a BeginSelection() API. */
    _SelectOnNav(1 shl 21),

    /** Override button behavior to react on Click (default is Click+Release) */
    _SelectOnClick(1 shl 22),

    /** Override button behavior to react on Release (default is Click+Release) */
    _SelectOnRelease(1 shl 23),

    /** Span all avail width even if we declared less for layout purpose. FIXME: We may be able to remove this (added in 6251d379, 2bcafc86 for menus)  */
    _SpanAvailWidth(1 shl 24),

    /** Always show active when held, even is not hovered. This concept could probably be renamed/formalized somehow. */
    _DrawHoveredWhenHeld(1 shl 25),

    /** Set Nav/Focus ID on mouse hover (used by MenuItem) */
    _SetNavIdOnHover(1 shl 26),

    /** Disable padding each side with ItemSpacing * 0.5f */
    _NoPadWithHalfSpacing(1 shl 27);

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
    HeightMask_(HeightSmall or HeightRegular or HeightLarge or HeightLargest),

    // private

    /** enable BeginComboPreview() */
    _CustomPreview(1 shl 20);

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

    /** Display a dot next to the title + tab is selected when clicking the X + closure is not assumed (will wait for user to stop submitting the tab). Otherwise closure is assumed when pressing the X, so if you keep submitting the tab may reappear at end of tab bar. */
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
    NoReorder(1 shl 5),

    /**  Enforce the tab position to the left of the tab bar (after the tab list popup button) */
    Leading(1 shl 6),

    /**  Enforce the tab position to the right of the tab bar (before the scrolling buttons) */
    Trailing(1 shl 7),

    // [Internal]

    _SectionMask_(Leading or Trailing),

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


typealias TableFlags = Int

// Flags for ImGui::BeginTable()
// - Important! Sizing policies have complex and subtle side effects, much more so than you would expect.
//   Read comments/demos carefully + experiment with live demos to get acquainted with them.
// - The DEFAULT sizing policies are:
//    - Default to ImGuiTableFlags_SizingFixedFit    if ScrollX is on, or if host window has ImGuiWindowFlags_AlwaysAutoResize.
//    - Default to ImGuiTableFlags_SizingStretchSame if ScrollX is off.
// - When ScrollX is off:
//    - Table defaults to ImGuiTableFlags_SizingStretchSame -> all Columns defaults to ImGuiTableColumnFlags_WidthStretch with same weight.
//    - Columns sizing policy allowed: Stretch (default), Fixed/Auto.
//    - Fixed Columns (if any) will generally obtain their requested width (unless the table cannot fit them all).
//    - Stretch Columns will share the remaining width according to their respective weight.
//    - Mixed Fixed/Stretch columns is possible but has various side-effects on resizing behaviors.
//      The typical use of mixing sizing policies is: any number of LEADING Fixed columns, followed by one or two TRAILING Stretch columns.
//      (this is because the visible order of columns have subtle but necessary effects on how they react to manual resizing).
// - When ScrollX is on:
//    - Table defaults to ImGuiTableFlags_SizingFixedFit -> all Columns defaults to ImGuiTableColumnFlags_WidthFixed
//    - Columns sizing policy allowed: Fixed/Auto mostly.
//    - Fixed Columns can be enlarged as needed. Table will show an horizontal scrollbar if needed.
//    - When using auto-resizing (non-resizable) fixed columns, querying the content width to use item right-alignment e.g. SetNextItemWidth(-FLT_MIN) doesn't make sense, would create a feedback loop.
//    - Using Stretch columns OFTEN DOES NOT MAKE SENSE if ScrollX is on, UNLESS you have specified a value for 'inner_width' in BeginTable().
//      If you specify a value for 'inner_width' then effectively the scrolling space is known and Stretch or mixed Fixed/Stretch columns become meaningful again.
// - Read on documentation at the top of imgui_tables.cpp for details.
enum class TableFlag(@JvmField val i: TableFlags) {

    // Features

    None(0),

    /** Enable resizing columns. */
    Resizable(1 shl 0),

    /** Enable reordering columns in header row (need calling TableSetupColumn() + TableHeadersRow() to display headers) */
    Reorderable(1 shl 1),

    /** Enable hiding/disabling columns in context menu. */
    Hideable(1 shl 2),

    /** Enable sorting. Call TableGetSortSpecs() to obtain sort specs. Also see ImGuiTableFlags_SortMulti and ImGuiTableFlags_SortTristate. */
    Sortable(1 shl 3),

    /** Disable persisting columns order, width and sort settings in the .ini file. */
    NoSavedSettings(1 shl 4),

    /** Right-click on columns body/contents will display table context menu. By default it is available in TableHeadersRow(). */
    ContextMenuInBody(1 shl 5),

    // Decorations

    /** Set each RowBg color with ImGuiCol_TableRowBg or ImGuiCol_TableRowBgAlt (equivalent of calling TableSetBgColor with ImGuiTableBgFlags_RowBg0 on each row manually) */
    RowBg(1 shl 6),

    /** Draw horizontal borders between rows. */
    BordersInnerH(1 shl 7),

    /** Draw horizontal borders at the top and bottom. */
    BordersOuterH(1 shl 8),

    /** Draw vertical borders between columns. */
    BordersInnerV(1 shl 9),

    /** Draw vertical borders on the left and right sides. */
    BordersOuterV(1 shl 10),

    /** Draw horizontal borders. */
    BordersH(BordersInnerH or BordersOuterH),

    /** Draw vertical borders. */
    BordersV(BordersInnerV or BordersOuterV),

    /** Draw inner borders. */
    BordersInner(BordersInnerV or BordersInnerH),

    /** Draw outer borders. */
    BordersOuter(BordersOuterV or BordersOuterH),

    /** Draw all borders. */
    Borders(BordersInner or BordersOuter),

    /** [ALPHA] Disable vertical borders in columns Body (borders will always appears in Headers). -> May move to style */
    NoBordersInBody(1 shl 11),

    /** [ALPHA] Disable vertical borders in columns Body until hovered for resize (borders will always appears in Headers). -> May move to style */
    NoBordersInBodyUntilResize(1 shl 12),

    // Sizing Policy (read above for defaults)

    /** Columns default to _WidthFixed or _WidthAuto (if resizable or not resizable), matching contents width. */
    SizingFixedFit(1 shl 13),

    /** Columns default to _WidthFixed or _WidthAuto (if resizable or not resizable), matching the maximum contents width of all columns. Implicitly enable ImGuiTableFlags_NoKeepColumnsVisible. */
    SizingFixedSame(2 shl 13),

    /** Columns default to _WidthStretch with default weights proportional to each columns contents widths. */
    SizingStretchProp(3 shl 13),

    /** Columns default to _WidthStretch with default weights all equal, unless overridden by TableSetupColumn(). */
    SizingStretchSame(4 shl 13),

    /** Make outer width auto-fit to columns, overriding outer_size.x value. Only available when ScrollX/ScrollY are disabled and Stretch columns are not used. */
    NoHostExtendX(1 shl 16),

    /** Make outer height stop exactly at outer_size.y (prevent auto-extending table past the limit). Only available when ScrollX/ScrollY are disabled. Data below the limit will be clipped and not visible. */
    NoHostExtendY(1 shl 17),

    /** Disable keeping column always minimally visible when ScrollX is off and table gets too small. Not recommended if columns are resizable. */
    NoKeepColumnsVisible(1 shl 18),

    /** Disable distributing remainder width to stretched columns (width allocation on a 100-wide table with 3 columns: Without this flag: 33,33,34. With this flag: 33,33,33). With larger number of columns, resizing will appear to be less smooth. */
    PreciseWidths(1 shl 19),

    // Clipping

    /** Disable clipping rectangle for every individual columns (reduce draw command count, items will be able to overflow into other columns). Generally incompatible with TableSetupScrollFreeze(). */
    NoClip(1 shl 20),

    // Padding

    /** Default if BordersOuterV is on. Enable outer-most padding. Generally desirable if you have headers. */
    PadOuterX(1 shl 21),

    /** Default if BordersOuterV is off. Disable outer-most padding. */
    NoPadOuterX(1 shl 22),

    /** Disable inner padding between columns (double inner padding if BordersOuterV is on, single inner padding if BordersOuterV is off). */
    NoPadInnerX(1 shl 23),

    // Scrolling

    /** Enable horizontal scrolling. Require 'outer_size' parameter of BeginTable() to specify the container size. Changes default sizing policy. Because this create a child window, ScrollY is currently generally recommended when using ScrollX. */
    ScrollX(1 shl 24),

    /** Enable vertical scrolling. Require 'outer_size' parameter of BeginTable() to specify the container size. */
    ScrollY(1 shl 25),

    // Sorting

    /** Hold shift when clicking headers to sort on multiple column. TableGetSortSpecs() may return specs where (SpecsCount > 1). */
    SortMulti(1 shl 26),

    /** Allow no sorting, disable default sorting. TableGetSortSpecs() may return specs where (SpecsCount == 0). */
    SortTristate(1 shl 27),

    /** [Internal] Combinations and masks */
    _SizingMask(SizingFixedFit or SizingFixedSame or SizingStretchProp or SizingStretchSame);

    infix fun and(b: TableFlag): TableFlags = i and b.i
    infix fun and(b: TableFlags): TableFlags = i and b
    infix fun or(b: TableFlag): TableFlags = i or b.i
    infix fun or(b: TableFlags): TableFlags = i or b
    infix fun xor(b: TableFlag): TableFlags = i xor b.i
    infix fun xor(b: TableFlags): TableFlags = i xor b
    infix fun wo(b: TableFlags): TableFlags = and(b.inv())
}

infix fun TableFlags.and(b: TableFlag): TableFlags = and(b.i)
infix fun TableFlags.or(b: TableFlag): TableFlags = or(b.i)
infix fun TableFlags.xor(b: TableFlag): TableFlags = xor(b.i)
infix fun TableFlags.has(b: TableFlag): Boolean = and(b.i) != 0
infix fun TableFlags.hasnt(b: TableFlag): Boolean = and(b.i) == 0
infix fun TableFlags.wo(b: TableFlag): TableFlags = and(b.i.inv())


typealias TableColumnFlags = Int

// Flags for ImGui::TableSetupColumn()
enum class TableColumnFlag(@JvmField val i: TableColumnFlags) {

    // Input configuration flags

    None(0),

    /** Overriding/master disable flag: hide column, won't show in context menu (unlike calling TableSetColumnEnabled() which manipulates the user accessible state) */
    Disabled(1 shl 0),

    /** Default as a hidden/disabled column. */
    DefaultHide(1 shl 1),

    /** Default as a sorting column. */
    DefaultSort(1 shl 2),

    /** Column will stretch. Preferable with horizontal scrolling disabled (default if table sizing policy is _SizingStretchSame or _SizingStretchProp). */
    WidthStretch(1 shl 3),

    /** Column will not stretch. Preferable with horizontal scrolling enabled (default if table sizing policy is _SizingFixedFit and table is resizable). */
    WidthFixed(1 shl 4),

    /** Disable manual resizing. */
    NoResize(1 shl 5),

    /** Disable manual reordering this column, this will also prevent other columns from crossing over this column. */
    NoReorder(1 shl 6),

    /** Disable ability to hide/disable this column. */
    NoHide(1 shl 7),

    /** Disable clipping for this column (all NoClip columns will render in a same draw command). */
    NoClip(1 shl 8),

    /** Disable ability to sort on this field (even if ImGuiTableFlags_Sortable is set on the table). */
    NoSort(1 shl 9),

    /** Disable ability to sort in the ascending direction. */
    NoSortAscending(1 shl 10),

    /** Disable ability to sort in the descending direction. */
    NoSortDescending(1 shl 11),

    /** TableHeadersRow() will not submit label for this column. Convenient for some small columns. Name will still appear in context menu. */
    NoHeaderLabel(1 shl 12),

    /** Disable header text width contribution to automatic column width. */
    NoHeaderWidth(1 shl 13),

    /** Make the initial sort direction Ascending when first sorting on this column (default). */
    PreferSortAscending(1 shl 14),

    /** Make the initial sort direction Descending when first sorting on this column. */
    PreferSortDescending(1 shl 15),

    /** Use current Indent value when entering cell (default for column 0). */
    IndentEnable(1 shl 16),

    /** Ignore current Indent value when entering cell (default for columns > 0). Indentation changes _within_ the cell will still be honored. */
    IndentDisable(1 shl 17),

    // Output status flags, read-only via TableGetColumnFlags()

    /** Status: is enabled == not hidden by user/api (referred to as "Hide" in _DefaultHide and _NoHide) flags. */
    IsEnabled(1 shl 24),

    /** Status: is visible == is enabled AND not clipped by scrolling. */
    IsVisible(1 shl 25),

    /** Status: is currently part of the sort specs */
    IsSorted(1 shl 26),

    /** Status: is hovered by mouse */
    IsHovered(1 shl 27),

    // [Internal] Combinations and masks

    WidthMask_(WidthStretch or WidthFixed),
    IndentMask_(IndentEnable or IndentDisable),
    StatusMask_(IsEnabled or IsVisible or IsSorted or IsHovered),

    /** [Internal] Disable user resizing this column directly (it may however we resized indirectly from its left edge) */
    NoDirectResize_(1 shl 30);

    infix fun and(b: TableColumnFlag): TableColumnFlags = i and b.i
    infix fun and(b: TableColumnFlags): TableColumnFlags = i and b
    infix fun or(b: TableColumnFlag): TableColumnFlags = i or b.i
    infix fun or(b: TableColumnFlags): TableColumnFlags = i or b
    infix fun xor(b: TableColumnFlag): TableColumnFlags = i xor b.i
    infix fun xor(b: TableColumnFlags): TableColumnFlags = i xor b
    infix fun wo(b: TableColumnFlags): TableColumnFlags = and(b.inv())
}

infix fun TableColumnFlags.and(b: TableColumnFlag): TableColumnFlags = and(b.i)
infix fun TableColumnFlags.or(b: TableColumnFlag): TableColumnFlags = or(b.i)
infix fun TableColumnFlags.xor(b: TableColumnFlag): TableColumnFlags = xor(b.i)
infix fun TableColumnFlags.has(b: TableColumnFlag): Boolean = and(b.i) != 0
infix fun TableColumnFlags.hasnt(b: TableColumnFlag): Boolean = and(b.i) == 0
infix fun TableColumnFlags.wo(b: TableColumnFlag): TableColumnFlags = and(b.i.inv())


typealias TableRowFlags = Int

// Flags for ImGui::TableNextRow()
enum class TableRowFlag(@JvmField val i: Int) {
    None(0),

    /** Identify header row (set default background color + width of its contents accounted differently for auto column width) */
    Headers(1 shl 0);

    infix fun and(b: TableRowFlag): TableRowFlags = i and b.i
    infix fun and(b: TableRowFlags): TableRowFlags = i and b
    infix fun or(b: TableRowFlag): TableRowFlags = i or b.i
    infix fun or(b: TableRowFlags): TableRowFlags = i or b
    infix fun xor(b: TableRowFlag): TableRowFlags = i xor b.i
    infix fun xor(b: TableRowFlags): TableRowFlags = i xor b
    infix fun wo(b: TableRowFlags): TableRowFlags = and(b.inv())
}

infix fun TableRowFlags.and(b: TableRowFlag): TableRowFlags = and(b.i)
infix fun TableRowFlags.or(b: TableRowFlag): TableRowFlags = or(b.i)
infix fun TableRowFlags.xor(b: TableRowFlag): TableRowFlags = xor(b.i)
infix fun TableRowFlags.has(b: TableRowFlag): Boolean = and(b.i) != 0
infix fun TableRowFlags.hasnt(b: TableRowFlag): Boolean = and(b.i) == 0
infix fun TableRowFlags.wo(b: TableRowFlag): TableRowFlags = and(b.i.inv())


typealias TableBgTargets = Int

// Enum for ImGui::TableSetBgColor()
// Background colors are rendering in 3 layers:
//  - Layer 0: draw with RowBg0 color if set, otherwise draw with ColumnBg0 if set.
//  - Layer 1: draw with RowBg1 color if set, otherwise draw with ColumnBg1 if set.
//  - Layer 2: draw with CellBg color if set.
// The purpose of the two row/columns layers is to let you decide if a background color changes should override or blend with the existing color.
// When using ImGuiTableFlags_RowBg on the table, each row has the RowBg0 color automatically set for odd/even rows.
// If you set the color of RowBg0 target, your color will override the existing RowBg0 color.
// If you set the color of RowBg1 or ColumnBg1 target, your color will blend over the RowBg0 color.
enum class TableBgTarget(@JvmField val i: TableBgTargets) {
    None(0),

    /** Set row background color 0 (generally used for background, automatically set when ImGuiTableFlags_RowBg is used) */
    RowBg0(1),

    /** Set row background color 1 (generally used for selection marking) */
    RowBg1(2),

    /** Set cell background color (top-most color) */
    CellBg(3);

    infix fun and(b: TableBgTarget): TableBgTargets = i and b.i
    infix fun and(b: TableBgTargets): TableBgTargets = i and b
    infix fun or(b: TableBgTarget): TableBgTargets = i or b.i
    infix fun or(b: TableBgTargets): TableBgTargets = i or b
    infix fun xor(b: TableBgTarget): TableBgTargets = i xor b.i
    infix fun xor(b: TableBgTargets): TableBgTargets = i xor b
    infix fun wo(b: TableBgTargets): TableBgTargets = and(b.inv())

    companion object {
        infix fun of(i: Int) = values().first { it.i == i }
    }
}

infix fun TableBgTargets.and(b: TableBgTarget): TableBgTargets = and(b.i)
infix fun TableBgTargets.or(b: TableBgTarget): TableBgTargets = or(b.i)
infix fun TableBgTargets.xor(b: TableBgTarget): TableBgTargets = xor(b.i)
infix fun TableBgTargets.has(b: TableBgTarget): Boolean = and(b.i) != 0
infix fun TableBgTargets.hasnt(b: TableBgTarget): Boolean = and(b.i) == 0
infix fun TableBgTargets.wo(b: TableBgTarget): TableBgTargets = and(b.i.inv())


typealias FocusedFlags = Int

/** Flags for ImGui::IsWindowFocused() */
enum class FocusedFlag(@JvmField val i: FocusedFlags) {
    None(0),

    /** Return true if any children of the window is focused */
    ChildWindows(1 shl 0),

    /** Test from root window (top most parent of the current hierarchy) */
    RootWindow(1 shl 1),

    /** Return true if any window is focused.
     *  Important: If you are trying to tell how to dispatch your low-level inputs, do NOT use this. Use 'io.WantCaptureMouse' instead! Please read the FAQ! */
    AnyWindow(1 shl 2),

    /** Do not consider popup hierarchy (do not treat popup emitter as parent of popup) (when used with _ChildWindows or _RootWindow) */
    NoPopupHierarchy(1 shl 3),

    //ImGuiFocusedFlags_DockHierarchy               = 1 << 4,   // Consider docking hierarchy (treat dockspace host as parent of docked window) (when used with _ChildWindows or _RootWindow)
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

    /** IsWindowHovered() only: Do not consider popup hierarchy (do not treat popup emitter as parent of popup) (when used with _ChildWindows or _RootWindow) */
    NoPopupHierarchy(1 shl 3),
    //ImGuiHoveredFlags_DockHierarchy               = 1 << 4,   // IsWindowHovered() only: Consider docking hierarchy (treat dockspace host as parent of docked window) (when used with _ChildWindows or _RootWindow)

    /** Return true even if a popup window is normally blocking access to this item/window  */
    AllowWhenBlockedByPopup(1 shl 5),
    //AllowWhenBlockedByModal     (1 shl 6),   // Return true even if a modal popup window is normally blocking access to this item/window. FIXME-TODO: Unavailable yet.

    /** Return true even if an active item is blocking access to this item/window. Useful for Drag and Drop patterns.   */
    AllowWhenBlockedByActiveItem(1 shl 7),

    /** IsItemHovered() only: Return true even if the position is obstructed or overlapped by another window,   */
    AllowWhenOverlapped(1 shl 8),

    /** IsItemHovered() only: Return true even if the item is disabled */
    AllowWhenDisabled(1 shl 9),

    /** Disable using gamepad/keyboard navigation state when active, always query mouse. */
    NoNavOverride(1 shl 10),
    RectOnly(AllowWhenBlockedByPopup.i or AllowWhenBlockedByActiveItem.i or AllowWhenOverlapped.i),
    RootAndChildWindows(RootWindow or ChildWindows),

    // Hovering delays (for tooltips)
    /** Return true after io.HoverDelayNormal elapsed (~0.30 sec) */
    DelayNormal(1 shl 11),

    /** Return true after io.HoverDelayShort elapsed (~0.10 sec) */
    DelayShort(1 shl 12),

    /** Disable shared delay system where moving from one item to the next keeps the previous timer for a short time (standard for tooltips with long delays) */
    NoSharedDelay(1 shl 13);

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
    Count("Count"),

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


/** A sorting direction */
enum class SortDirection {
    None,

    /** Ascending = 0->9, A->Z etc. */
    Ascending,

    /** Descending = 9->0, Z->A etc. */
    Descending;

    @JvmField
    val i = ordinal

    companion object {
        infix fun of(i: Int) = values().first { it.i == i }
    }
}


/** A key identifier (ImGui-side enum) */
enum class Key {
    // Keyboard
    None, Tab, LeftArrow, RightArrow, UpArrow, DownArrow, PageUp, PageDown, Home, End, Insert, Delete, Backspace, Space, Enter, Escape,
    LeftCtrl, LeftShift, LeftAlt, LeftSuper,
    RightCtrl, RightShift, RightAlt, RightSuper,
    Menu,
    `0`, `1`, `2`, `3`, `4`, `5`, `6`, `7`, `8`, `9`,
    A, B, C, D, E, F, G, H, I, J,
    K, L, M, N, O, P, Q, R, S, T,
    U, V, W, X, Y, Z,
    F1, F2, F3, F4, F5, F6,
    F7, F8, F9, F10, F11, F12,

    /** ' */
    Apostrophe,

    /** , */
    Comma,

    /** - */
    Minus,

    /** . */
    Period,

    /** / */
    Slash,

    /** ; */
    Semicolon,

    /** = */
    Equal,

    /** [ */
    LeftBracket,

    /** \ (this text inhibit multiline comment caused by backslash) */
    Backslash,

    /** ] */
    RightBracket,

    /** ` */
    GraveAccent,
    CapsLock, ScrollLock, NumLock, PrintScreen, Pause,
    Keypad0, Keypad1, Keypad2, Keypad3, Keypad4,
    Keypad5, Keypad6, Keypad7, Keypad8, Keypad9,
    KeypadDecimal, KeypadDivide, KeypadMultiply, KeypadSubtract, KeypadAdd, KeypadEnter, KeypadEqual,

    // Gamepad (some of those are analog values, 0.0f to 1.0f)                         // NAVIGATION action
    GamepadStart,          // Menu (Xbox)          + (Switch)      Start/Options (PS)  // --
    GamepadBack,           // View (Xbox)          - (Switch)      Share (PS)          // --
    GamepadFaceLeft,       // X (Xbox)             Y (Switch)      Square (PS)         // -> ImGuiNavInput_Menu
    GamepadFaceRight,      // B (Xbox)             A (Switch)      Circle (PS)         // -> ImGuiNavInput_Cancel
    GamepadFaceUp,         // Y (Xbox)             X (Switch)      Triangle (PS)       // -> ImGuiNavInput_Input
    GamepadFaceDown,       // A (Xbox)             B (Switch)      Cross (PS)          // -> ImGuiNavInput_Activate
    GamepadDpadLeft,       // D-pad Left                                               // -> ImGuiNavInput_DpadLeft
    GamepadDpadRight,      // D-pad Right                                              // -> ImGuiNavInput_DpadRight
    GamepadDpadUp,         // D-pad Up                                                 // -> ImGuiNavInput_DpadUp
    GamepadDpadDown,       // D-pad Down                                               // -> ImGuiNavInput_DpadDown
    GamepadL1,             // L Bumper (Xbox)      L (Switch)      L1 (PS)             // -> ImGuiNavInput_FocusPrev + ImGuiNavInput_TweakSlow
    GamepadR1,             // R Bumper (Xbox)      R (Switch)      R1 (PS)             // -> ImGuiNavInput_FocusNext + ImGuiNavInput_TweakFast
    GamepadL2,             // L Trigger (Xbox)     ZL (Switch)     L2 (PS) [Analog]
    GamepadR2,             // R Trigger (Xbox)     ZR (Switch)     R2 (PS) [Analog]
    GamepadL3,             // L Thumbstick (Xbox)  L3 (Switch)     L3 (PS)
    GamepadR3,             // R Thumbstick (Xbox)  R3 (Switch)     R3 (PS)
    GamepadLStickLeft,     // [Analog]                                                 // -> ImGuiNavInput_LStickLeft
    GamepadLStickRight,    // [Analog]                                                 // -> ImGuiNavInput_LStickRight
    GamepadLStickUp,       // [Analog]                                                 // -> ImGuiNavInput_LStickUp
    GamepadLStickDown,     // [Analog]                                                 // -> ImGuiNavInput_LStickDown
    GamepadRStickLeft,     // [Analog]
    GamepadRStickRight,    // [Analog]
    GamepadRStickUp,       // [Analog]
    GamepadRStickDown,     // [Analog]

    // Keyboard Modifiers (explicitly submitted by backend via AddKeyEvent() calls)
    // - This is mirroring the data also written to io.KeyCtrl, io.KeyShift, io.KeyAlt, io.KeySuper, in a format allowing
    //   them to be accessed via standard key API, allowing calls such as IsKeyPressed(), IsKeyReleased(), querying duration etc.
    // - Code polling every keys (e.g. an interface to detect a key press for input mapping) might want to ignore those
    //   and prefer using the real keys (e.g. ImGuiKey_LeftCtrl, ImGuiKey_RightCtrl instead of ImGuiKey_ModCtrl).
    // - In theory the value of keyboard modifiers should be roughly equivalent to a logical or of the equivalent left/right keys.
    //   In practice: it's complicated; mods are often provided from different sources. Keyboard layout, IME, sticky keys and
    //   backends tend to interfere and break that equivalence. The safer decision is to relay that ambiguity down to the end-user...
    ModCtrl, ModShift, ModAlt, ModSuper,

    // Mouse Buttons (auto-submitted from AddMouseButtonEvent() calls)
    // - This is mirroring the data also written to io.MouseDown[], io.MouseWheel, in a format allowing them to be accessed via standard key API.
    MouseLeft, MouseRight, MouseMiddle, MouseX1, MouseX2, MouseWheelX, MouseWheelY,

    // End of list
    Count;

    @JvmField
    val i = if (ordinal == 0) 0 else 512 + ordinal

    val index: Int
        get() {
            check(i in Key.BEGIN until Key.END) { "ImGuiKey and native_index was merged together and native_index is disabled by IMGUI_DISABLE_OBSOLETE_KEYIO. Please switch to ImGuiKey." }
            return i
        }

    companion object {
        val COUNT = values().size
        val BEGIN = None.i
        val END = F12.i
        val Keyboard_BEGIN = BEGIN
        val Keyboard_END = GamepadStart.i
        val Gamepad_BEGIN = GamepadStart.i
        val Gamepad_END = ModCtrl.i
        val Aliases_BEGIN = MouseLeft.i
        val Aliases_END = Count.i
        infix fun of(i: Int) = values().first { it.i == i }


        // [Internal] Named shortcuts for Navigation
        internal val _NavKeyboardTweakSlow = ModCtrl
        internal val _NavKeyboardTweakFast = ModShift
        internal val _NavGamepadTweakSlow = GamepadL1
        internal val _NavGamepadTweakFast = GamepadR1
        internal val _NavGamepadActivate = GamepadFaceDown
        internal val _NavGamepadCancel = GamepadFaceRight
        internal val _NavGamepadMenu = GamepadFaceLeft
        internal val _NavGamepadInput = GamepadFaceUp
    }

    /** ~IsGamepadKey */
    val isGamepad: Boolean
        get() = i in Gamepad_BEGIN until Gamepad_END

    /** ~IsAliasKey */
    val isAlias: Boolean
        get() = i in Aliases_BEGIN until Aliases_END

    /** ~GetKeyData */
    val data: KeyData
        get() = g.io.keysData[index]

    infix fun getChordName(mods: ModFlags): String {
        assert(mods hasnt ModFlag.All) { "Passing invalid ImGuiModFlags value!" } // A frequent mistake is to pass ImGuiKey_ModXXX instead of ImGuiModFlags_XXX
        return (if (mods has ModFlag.Ctrl) "Ctrl+" else "") +
                (if (mods has ModFlag.Shift) "Shift+" else "") +
                (if (mods has ModFlag.Alt) "Alt+" else "") +
                (if (mods has ModFlag.Super) if (g.io.configMacOSXBehaviors) "Cmd+" else "Super+" else "") +
                name
    }

    /** ~IsKeyDown
     *
     *  is key being held.
     *
     *  Note that Dear ImGui doesn't know the meaning/semantic of ImGuiKey from 0..511: they are legacy native keycodes.
     *  Consider transitioning from 'IsKeyDown(MY_ENGINE_KEY_A)' (<1.87) to IsKeyDown(A) (>= 1.87) */
    val isDown: Boolean
        get() = g.io.keysData[i].down

    /** [JVM] ~IsKeyPressed
     *
     *  uses user's key indices as stored in the keys_down[] array. if repeat=true.
     *  uses io.KeyRepeatDelay / KeyRepeatRate
     *
     *  was key pressed (went from !Down to Down)? if repeat=true, uses io.KeyRepeatDelay / KeyRepeatRate */
    infix fun isPressed(repeat: Boolean): Boolean {
        assert(i in Key.BEGIN until Key.END) { "Support for user key indices was dropped in favor of ImGuiKey. Please update backend & user code." }

        assert(i in g.io.keysData.indices)
        val t = g.io.keysData[i].downDuration
        if (t == 0f)
            return true
        if (repeat && t > io.keyRepeatDelay)
            return getPressedAmount(io.keyRepeatDelay, io.keyRepeatRate) > 0
        return false
    }

    /** ~IsKeyPressedEx
     *
     *  Important: unlike legacy IsKeyPressed(ImGuiKey, bool repeat=true) which DEFAULT to repeat, this requires EXPLICIT repeat.
     *  [Internal] 2022/07: Do not call this directly! It is a temporary entry point which we will soon replace with an overload for IsKeyPressed() when we introduce key ownership. */
    fun isPressedEx(flags: InputFlags = InputFlag.None.i): Boolean {

        val t = data.downDuration
        if (t < 0f)
            return false

        var pressed = t == 0f
        if (!pressed && flags has InputFlag.Repeat) {
            val (repeatDelay, repeatRate) = getTypematicRepeatRate(flags)
            pressed = (t > repeatDelay) && getPressedAmount(repeatDelay, repeatRate) > 0
        }

        return pressed
    }

    /** ~IsKeyPressed() */
    val isPressed: Boolean
        get() = isPressed(true)

    /** ~IsKeyReleased
     *
     *  was key released (went from Down to !Down)?    */
    val isReleased: Boolean
        get() = g.io.keysData[i].run { downDurationPrev >= 0f && !down }

    /** ~getKeyPressedAmount
     *
     *  Uses provided repeat rate/delay. return a count, most often 0 or 1 but might be >1 if RepeatRate is small enough
     *  that DeltaTime > RepeatRate
     *
     *  Return value representing the number of presses in the last time period, for the given repeat rate
     *  (most often returns 0 or 1. The result is generally only >1 when RepeatRate is smaller than DeltaTime, aka large DeltaTime or fast RepeatRate) */
    fun getPressedAmount(repeatDelay: Float, repeatRate: Float): Int {
        val t = io.keysData[i].downDuration
        return ImGui.calcTypematicRepeatAmount(t - io.deltaTime, t, repeatDelay, repeatRate)
    }
}

infix fun Long.shl(key: Key) = shl(key.i)

// for IO.keyMap

operator fun IntArray.set(index: Key, value: Int) {
    this[index.i] = value
}

operator fun IntArray.get(index: Key): Int = get(index.i)


typealias ModFlags = Int

// Helper "flags" version of key-mods to store and compare multiple key-mods easily. Sometimes used for storage (e.g. io.KeyMods) but otherwise not much used in public API.
enum class ModFlag(i: ModFlags = -1) {
    None, Ctrl, Shift,

    /** Option/Menu key */
    Alt,

    /** Cmd/Super/Windows key */
    Super,
    All(0x0F);

    val i = if (i != -1) i else if (ordinal == 0) 0 else 1 shl (ordinal - 1)

    infix fun and(b: ModFlag): ModFlags = i and b.i
    infix fun and(b: ModFlags): ModFlags = i and b
    infix fun or(b: ModFlag): ModFlags = i or b.i
    infix fun or(b: ModFlags): ModFlags = i or b
    infix fun xor(b: ModFlag): ModFlags = i xor b.i
    infix fun xor(b: ModFlags): ModFlags = i xor b
    infix fun wo(b: ModFlags): ModFlags = and(b.inv())
}

infix fun ModFlags.and(b: ModFlag): ModFlags = and(b.i)
infix fun ModFlags.or(b: ModFlag): ModFlags = or(b.i)
infix fun ModFlags.xor(b: ModFlag): ModFlags = xor(b.i)
infix fun ModFlags.has(b: ModFlag): Boolean = and(b.i) != 0
infix fun ModFlags.hasnt(b: ModFlag): Boolean = and(b.i) == 0
infix fun ModFlags.wo(b: ModFlag): ModFlags = and(b.i.inv())
operator fun ModFlags.minus(flag: ModFlag): ModFlags = wo(flag)
operator fun ModFlags.div(flag: ModFlag): ModFlags = or(flag)


/** Gamepad/Keyboard navigation
 *  Since >= 1.87 backends you generally don't need to care about this enum since io.NavInputs[] is setup automatically. This might become private/internal some day.
 *  Keyboard: Set io.configFlags |= NavFlags.EnableKeyboard to enable. ::newFrame() will automatically fill io.navInputs[] based on your io.AddKeyEvent() calls.
 *  Gamepad:  Set io.configFlags |= NavFlags.EnableGamepad to enable. Fill the io.navInputs[] fields before calling NewFrame(). Note that io.navInputs[] is cleared by EndFrame().
 *  Read instructions in imgui.cpp for more details. Download PNG/PSD at http://dearimgui.org/controls_sheets.
 *
 *  An input identifier for navigation */
enum class NavInput {
    // Gamepad Mapping
    /** Activate / Open / Toggle / Tweak value       // e.g. Cross  (PS4), A (Xbox), A (Switch), Space (Keyboard) */
    Activate,

    /** Cancel / Close / Exit                        // e.g. Circle (PS4), B (Xbox), B (Switch), Escape (Keyboard) */
    Cancel,

    /** Text input / On-Screen keyboard              // e.g. Triang.(PS4), Y (Xbox), X (Switch), Return (Keyboard) */
    Input,

    /** Tap: Toggle menu / Hold: Focus, Move, Resize // e.g. Square (PS4), X (Xbox), Y (Switch), Alt (Keyboard) */
    Menu,

    /** Move / Tweak / Resize window (w/ PadMenu)    // e.g. D-pad Left/Right/Up/Down (Gamepads), Arrow keys (Keyboard) */
    DpadLeft,

    DpadRight,

    DpadUp,

    DpadDown,

    /** Scroll / Move window (w/ PadMenu)            // e.g. Left Analog Stick Left/Right/Up/Down   */
    LStickLeft,

    LStickRight,

    LStickUp,

    LStickDown,

    /** Focus Next window (w/ PadMenu)               // e.g. L1 or L2 (PS4), LB or LT (Xbox), L or ZL (Switch) */
    FocusPrev,

    /** Focus Prev window (w/ PadMenu)               // e.g. R1 or R2 (PS4), RB or RT (Xbox), R or ZL (Switch) */
    FocusNext,

    /** Slower tweaks                                // e.g. L1 or L2 (PS4), LB or LT (Xbox), L or ZL (Switch) */
    TweakSlow,

    /** Faster tweaks                                // e.g. R1 or R2 (PS4), RB or RT (Xbox), R or ZL (Switch) */
    TweakFast,

    // [Internal] Don't use directly! This is used internally to differentiate keyboard from gamepad inputs for behaviors that require to differentiate them.
    // Keyboard behavior that have no corresponding gamepad mapping (e.g. CTRL+TAB) will be directly reading from keyboard keys instead of io.NavInputs[].

    /** Move left = Arrow keys  */
    _KeyLeft,

    /** Move right = Arrow keys  */
    _KeyRight,

    /** Move up = Arrow keys  */
    _KeyUp,

    /** Move down = Arrow keys  */
    _KeyDown,
    Count;

    @JvmField
    val i = ordinal

    companion object {
        val COUNT = values().size
        val InternalStart = _KeyLeft.i
    }
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

    /** Master keyboard navigation enable flag. NewFrame() will automatically fill io.NavInputs[] based on io.AddKeyEvent() calls. */
    NavEnableKeyboard(1 shl 0),

    /** Master gamepad navigation enable flag. This is mostly to instruct your imgui backend to fill io.NavInputs[].
     *  Backend also needs to set ImGuiBackendFlags_HasGamepad. */
    NavEnableGamepad(1 shl 1),

    /** Instruct navigation to move the mouse cursor. May be useful on TV/console systems where moving a virtual mouse is awkward.
     *  Will update io.MousePos and set io.wantSetMousePos=true. If enabled you MUST honor io.wantSetMousePos requests in your backend,
     *  otherwise ImGui will react as if the mouse is jumping around back and forth. */
    NavEnableSetMousePos(1 shl 2),

    /** Instruct navigation to not set the io.WantCaptureKeyboard flag when io.NavActive is set. */
    NavNoCaptureKeyboard(1 shl 3),

    /** Instruct imgui to clear mouse position/buttons in NewFrame(). This allows ignoring the mouse information set by the backend. */
    NoMouse(1 shl 4),

    /** Request backend to not alter mouse cursor configuration.
     *  Use if the backend cursor changes are interfering with yours and you don't want to use setMouseCursor() to change mouse cursor.
     *  You may want to honor requests from imgui by reading ::mouseCursor yourself instead. */
    NoMouseCursorChange(1 shl 5),

    /** JVM custom, request backend to not read the mouse status allowing you to provide your own custom input */
    NoMouseUpdate(1 shl 12),

    /** JVM custom */
    NoKeyboardUpdate(1 shl 13),

    // User storage (to allow your backend/engine to communicate to code that may be shared between multiple projects. Those flags are NOT used by core Dear ImGui)

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

/** Backend capabilities flags stored in io.BackendFlag. Set by imgui_impl_xxx or custom backend.
 *
 *  Flags: for io.BackendFlags  */
enum class BackendFlag(@JvmField val i: BackendFlags) {
    None(0),

    /** Backend Platform supports gamepad and currently has one connected. */
    HasGamepad(1 shl 0),

    /** Backend Platform supports honoring GetMouseCursor() value to change the OS cursor shape. */
    HasMouseCursors(1 shl 1),

    /** Backend Platform supports io.WantSetMousePos requests to reposition the OS mouse position (only used if ImGuiConfigFlags_NavEnableSetMousePos is set). */
    HasSetMousePos(1 shl 2),

    /** Backend Platform supports ImDrawCmd::VtxOffset. This enables output of large meshes (64K+ vertices) while still using 16-bit indices. */
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
operator fun BackendFlags.minus(flag: BackendFlag): BackendFlags = wo(flag)
operator fun BackendFlags.div(flag: BackendFlag): BackendFlags = or(flag)

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

    /** Resize grip in lower-right and lower-left corners of windows. */
    ResizeGrip,
    ResizeGripHovered,
    ResizeGripActive,

    /** TabItem in a TabBar */
    Tab,
    TabHovered,
    TabActive,
    TabUnfocused,
    TabUnfocusedActive,
    PlotLines,
    PlotLinesHovered,
    PlotHistogram,
    PlotHistogramHovered,

    /** Table header background */
    TableHeaderBg,

    /** Table outer and header borders (prefer using Alpha=1.0 here) */
    TableBorderStrong,

    /** Table inner borders (prefer using Alpha=1.0 here) */
    TableBorderLight,

    /** Table row background (even rows) */
    TableRowBg,

    /** Table row background (odd rows) */
    TableRowBgAlt,
    TextSelectedBg,

    /** Rectangle highlighting a drop target */
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


// Enumeration for PushStyleVar() / PopStyleVar() to temporarily modify the ImGuiStyle structure.
// - The enum only refers to fields of ImGuiStyle which makes sense to be pushed/popped inside UI code.
//   During initialization or between frames, feel free to just poke into ImGuiStyle directly.
// - Tip: Use your programming IDE navigation facilities on the names in the _second column_ below to find the actual members and their description.
//   In Visual Studio IDE: CTRL+comma ("Edit.GoToAll") can follow symbols in comments, whereas CTRL+F12 ("Edit.GoToImplementation") cannot.
//   With Visual Assist installed: ALT+G ("VAssistX.GoToImplementation") can also follow symbols in comments.
// - When changing this enum, you need to update the associated internal table GStyleVarInfo[] accordingly. This is where we link enum values to members offset/type.

/** A variable identifier for styling   */
enum class StyleVar {
    /** float   */
    Alpha,

    /** WindowPadding */
    DisabledAlpha,

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

    /** vec2    */
    CellPadding,

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

    /** ColorEdit: disable picker when clicking on color square.  */
    NoPicker(1 shl 2),

    /** ColorEdit: disable toggling options menu when right-clicking on inputs/small preview.   */
    NoOptions(1 shl 3),

    /** ColorEdit, ColorPicker: disable color square preview next to the inputs. (e.g. to show only the inputs)   */
    NoSmallPreview(1 shl 4),

    /** ColorEdit, ColorPicker: disable inputs sliders/text widgets (e.g. to show only the small preview color square).   */
    NoInputs(1 shl 5),

    /** ColorEdit, ColorPicker, ColorButton: disable tooltip when hovering the preview. */
    NoTooltip(1 shl 6),

    /** ColorEdit, ColorPicker: disable display of inline text label (the label is still forwarded to the tooltip and picker).  */
    NoLabel(1 shl 7),

    /** ColorPicker: disable bigger color preview on right side of the picker, use small color square preview instead.    */
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
    DefaultOptions(Uint8 or DisplayRGB or InputRGB or PickerHueBar),

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
 *  User code may request backend to display given cursor by calling SetMouseCursor(),
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