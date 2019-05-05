package imgui.internal

import imgui.DrawCornerFlags
import imgui.DrawListFlags

//-----------------------------------------------------------------------------
// Types
//-----------------------------------------------------------------------------

// Internal Drag and Drop payload types. String starting with '_' are reserved for Dear ImGui.
val PAYLOAD_TYPE_DOCKABLE = "_IMDOCK"   // ImGuiWindow* // [Internal] Docking/tabs

/** flags: for ButtonEx(), ButtonBehavior()  // enum ButtonFlag */
typealias ButtonFlags = Int

/** Flags: for DragBehavior()                // enum ImGuiDragFlags */
typealias DragFlags = Int

/** flags: for PushItemFlag()                // enum ItemFlag */
typealias ItemFlags = Int

/** flags: storage for DC.LastItemXXX        // enum ItemStatusFlag */
typealias ItemStatusFlags = Int

/** flags: for RenderNavHighlight()          // enum NavHighlightFlag */
typealias NavHighlightFlags = Int

/** flags: for GetNavInputAmount2d()         // enum NavDirSourceFlag */
typealias NavDirSourceFlags = Int

/** flags: for navigation requests           // enum ImGuiNavMoveFlags */
typealias NavMoveFlags = Int

/** flags: for Separator() - internal        // enum SeparatorFlag */
typealias SeparatorFlags = Int

/** flags: for SliderBehavior()              // enum SliderFlag */
typealias SliderFlags = Int

/** flags: for TextEx()                      // enum ImGuiTextFlags */
typealias TextFlags = Int

enum class ButtonFlag {

    None,
    /** hold to repeat  */
    Repeat,
    /** return true on click + release on same item [DEFAULT if no PressedOn* flag is set]  */
    PressedOnClickRelease,
    /** return true on click (default requires click+release)    */
    PressedOnClick,
    /** return true on release (default requires click+release)  */
    PressedOnRelease,
    /** return true on double-click (default requires click+release) */
    PressedOnDoubleClick,
    /** allow interactions even if a child window is overlapping */
    FlattenChildren,
    /** require previous frame HoveredId to either match id or be null before being usable, use along with setItemAllowOverlap() */
    AllowItemOverlap,
    /** disable automatically closing parent popup on press // [UNUSED] */
    DontClosePopups,
    /** disable interactions */
    Disabled,
    /** vertically align button to match text baseline - ButtonEx() only
     *  FIXME: Should be removed and handled by SmallButton(), not possible currently because of DC.CursorPosPrevLine */
    AlignTextBaseLine,
    /** disable interaction if a key modifier is held */
    NoKeyModifiers,
    /** don't set ActiveId while holding the mouse (ButtonFlag.PressedOnClick only) */
    NoHoldingActiveID,
    /** press when held into while we are drag and dropping another item (used by e.g. tree nodes, collapsing headers) */
    PressedOnDragDropHold,
    /** don't override navigation focus when activated; */
    NoNavFocus,
    /** don't report as hovered when navigated on */
    NoHoveredOnNav;

    val i = if (ordinal == 0) 0 else 1 shl (ordinal - 1)

    infix fun or(b: ButtonFlag): ButtonFlags = i or b.i
}

infix fun Int.or(b: ButtonFlag): ButtonFlags = this or b.i
infix fun Int.has(b: ButtonFlag) = (this and b.i) != 0
infix fun Int.hasnt(b: ButtonFlag) = (this and b.i) == 0

enum class SliderFlag(val i: Int) { None(0), Vertical(1 shl 0) }

infix fun Int.has(b: SliderFlag) = and(b.i) != 0
infix fun Int.hasnt(b: SliderFlag) = and(b.i) == 0

enum class DragFlag(val i: Int) { None(0), Vertical(1 shl 0) }

infix fun Int.has(b: DragFlag) = and(b.i) != 0
infix fun Int.hasnt(b: DragFlag) = and(b.i) == 0

enum class ColumnsFlag(val i: Int) {

    None(0),
    /** Disable column dividers */
    NoBorder(1 shl 0),
    /** Disable resizing columns when clicking on the dividers  */
    NoResize(1 shl 1),
    /** Disable column width preservation when adjusting columns    */
    NoPreserveWidths(1 shl 2),
    /** Disable forcing columns to fit within window    */
    NoForceWithinWindow(1 shl 3),
    /** (WIP) Restore pre-1.51 behavior of extending the parent window contents size but _without affecting the columns
     *  width at all_. Will eventually remove.  */
    GrowParentContentsSize(1 shl 4)
}

infix fun Int.has(b: ColumnsFlag) = (this and b.i) != 0
infix fun Int.hasnt(b: ColumnsFlag) = (this and b.i) == 0

enum class SeparatorFlag {
    None,
    /** Axis default to current layout type, so generally Horizontal unless e.g. in a menu bar  */
    Horizontal,
    Vertical;

    val i = if (ordinal == 0) 0 else 1 shl ordinal
}

infix fun SeparatorFlag.or(b: SeparatorFlag): SeparatorFlags = i or b.i
infix fun Int.or(b: SeparatorFlag): SeparatorFlags = this or b.i
infix fun Int.has(b: SeparatorFlag) = (this and b.i) != 0
infix fun Int.hasnt(b: SeparatorFlag) = (this and b.i) == 0

/** Transient per-window flags, reset at the beginning of the frame. For child window, inherited from parent on first Begin().
 *  This is going to be exposed in imgui.h when stabilized enough. */
enum class ItemFlag(@JvmField val i: Int) {
    // @formatter:off
    NoTabStop(1 shl 0),  // false
    /** Button() will return true multiple times based on io.KeyRepeatDelay and io.KeyRepeatRate settings. */
    ButtonRepeat(1 shl 1),  // false
    /** [BETA] Disable interactions but doesn't affect visuals yet. See github.com/ocornut/imgui/issues/211 */
    Disabled(1 shl 2),  // false
    NoNav(1 shl 3),  // false
    NoNavDefaultFocus(1 shl 4),  // false
    /** MenuItem/Selectable() automatically closes current Popup window */
    SelectableDontClosePopup(1 shl 5),  // false
    Default_(0)
    // @formatter:on
}

infix fun ItemFlag.or(other: ItemFlag) = i or other.i
infix fun Int.or(other: ItemFlag) = or(other.i)
infix fun Int.has(b: ItemFlag) = (this and b.i) != 0
infix fun Int.hasnt(b: ItemFlag) = (this and b.i) == 0
infix fun Int.wo(b: ItemFlag) = and(b.i.inv())

/** Storage for LastItem data   */
enum class ItemStatusFlag(@JvmField val i: Int) {
    None(0),
    HoveredRect(1 shl 0),
    HasDisplayRect(1 shl 1),
    /** Value exposed by item was edited in the current frame (should match the bool return value of most widgets) */
    Edited(1 shl 2),
    /** Set when Selectable(), TreeNode() reports toggling a selection. We can't report "Selected" because reporting
     *  the change allows us to handle clipping with less issues. */
    ToggledSelection(1 shl 3),

    //  #ifdef IMGUI_ENABLE_TEST_ENGINE
//  [imgui-test only]
    Openable(1 shl 10),
    Opened(1 shl 11),
    Checkable(1 shl 12),
    Checked(1 shl 13);
}

infix fun Int.wo(b: ItemStatusFlag): ItemStatusFlags = and(b.i.inv())
infix fun Int.or(b: ItemStatusFlag): ItemStatusFlags = or(b.i)
infix fun Int.has(b: ItemStatusFlag) = and(b.i) != 0
infix fun Int.hasnt(b: ItemStatusFlag) = and(b.i) == 0

enum class TextFlag(val i: Int) { None(0), NoWidthForLargeClippedText(1 shl 0) }

infix fun Int.wo(b: TextFlag): TextFlags = and(b.i.inv())
infix fun Int.or(b: TextFlag): TextFlags = or(b.i)
infix fun Int.has(b: TextFlag) = and(b.i) != 0
infix fun Int.hasnt(b: TextFlag) = and(b.i) == 0

/** FIXME: this is in development, not exposed/functional as a generic feature yet.
 *  Horizontal/Vertical enums are fixed to 0/1 so they may be used to index ImVec2 */
enum class LayoutType {
    Horizontal, Vertical;

    val i = ordinal
}

enum class LogType {
    None, TTY, File, Buffer, Clipboard;

    val i = ordinal
}

/** X/Y enums are fixed to 0/1 so they may be used to index ImVec2 */
enum class Axis {
    None, X, Y;

    val i = ordinal - 1
}

infix fun Int.shl(b: Axis) = shl(b.i)

enum class PlotType {
    Lines, Histogram;

    val i = ordinal
}

enum class InputSource {
    None, Mouse, Nav,
    /** Only used occasionally for storage, not tested/handled by most code */
    NavKeyboard,
    /** Only used occasionally for storage, not tested/handled by most code */
    NavGamepad;

    val i = ordinal

    companion object {
        val COUNT = values().size
    }
}

// FIXME-NAV: Clarify/expose various repeat delay/rate
enum class InputReadMode {
    Down, Pressed, Released, Repeat, RepeatSlow, RepeatFast;

    val i = ordinal
}

enum class NavHighlightFlag {
    None, TypeDefault, TypeThin,
    /** Draw rectangular highlight if (g.NavId == id) _even_ when using the mouse. */
    AlwaysDraw,
    NoRounding;

    val i = if (ordinal == 0) 0 else 1 shl ordinal
}

infix fun Int.has(b: NavHighlightFlag) = and(b.i) != 0
infix fun Int.hasnt(b: NavHighlightFlag) = and(b.i) == 0
infix fun NavHighlightFlag.or(b: NavHighlightFlag): NavHighlightFlags = i or b.i

enum class NavDirSourceFlag {
    None, Keyboard, PadDPad, PadLStick;

    val i = if (ordinal == 0) 0 else 1 shl ordinal
}

infix fun NavDirSourceFlag.or(b: NavDirSourceFlag): NavDirSourceFlags = i or b.i
infix fun Int.has(b: NavDirSourceFlag) = and(b.i) != 0

enum class NavMoveFlag {
    None,
    /** On failed request, restart from opposite side */
    LoopX,
    LoopY,
    /** On failed request, request from opposite side one line down (when NavDir==right) or one line up (when NavDir==left) */
    WrapX,
    /** This is not super useful for provided for completeness */
    WrapY,
    /** Allow scoring and considering the current NavId as a move target candidate.
     *  This is used when the move source is offset (e.g. pressing PageDown actually needs to send a Up move request,
     *  if we are pressing PageDown from the bottom-most item we need to stay in place) */
    AllowCurrentNavId,
    /** Store alternate result in NavMoveResultLocalVisibleSet that only comprise elements that are already fully visible.; */
    AlsoScoreVisibleSet;

    val i = if (ordinal == 0) 0 else 1 shl ordinal

    infix fun or(b: NavMoveFlag): NavMoveFlags = i or b.i
}

infix fun Int.has(b: NavMoveFlag) = and(b.i) != 0

enum class NavForward {
    None, ForwardQueued, ForwardActive;

    val i = ordinal
}

enum class NavLayer {
    /** Main scrolling layer */
    Main,
    /** Menu layer (access with Alt/ImGuiNavInput_Menu) */
    Menu;

    val i = ordinal

    companion object {
        val COUNT = values().size
        infix fun of(i: Int) = values().first { it.i == i }
    }
}

enum class PopupPositionPolicy { Default, ComboBox }

enum class DrawCornerFlag(val i: Int) {
    TopLeft(1 shl 0), // 0x1
    TopRight(1 shl 1), // 0x2
    BotLeft(1 shl 2), // 0x4
    BotRight(1 shl 3), // 0x8
    Top(TopLeft or TopRight),   // 0x3
    Bot(BotLeft or BotRight),   // 0xC
    Left(TopLeft or BotLeft),    // 0x5
    Right(TopRight or BotRight),  // 0xA
    /** In your function calls you may use ~0 (= all bits sets) instead of DrawCornerFlags.All, as a convenience  */
    All(0xF)
}

infix fun DrawCornerFlag.or(b: DrawCornerFlag): DrawCornerFlags = i or b.i
infix fun Int.or(b: DrawCornerFlag): DrawCornerFlags = or(b.i)
infix fun Int.and(b: DrawCornerFlag): DrawCornerFlags = and(b.i)
infix fun Int.has(b: DrawCornerFlag) = (this and b.i) != 0
infix fun Int.hasnt(b: DrawCornerFlag) = (this and b.i) == 0

enum class DrawListFlag(val i: Int) {
    None(0),
    /** Lines are anti-aliased (*2 the number of triangles for 1.0f wide line, otherwise *3 the number of triangles) */
    AntiAliasedLines(1 shl 0),
    /** Filled shapes have anti-aliased edges (*2 the number of vertices) */
    AntiAliasedFill(1 shl 1);
}

infix fun DrawListFlag.or(b: DrawListFlag): DrawListFlags = i or b.i
infix fun Int.or(b: DrawListFlag): DrawListFlags = or(b.i)
infix fun Int.and(b: DrawListFlag): DrawListFlags = and(b.i)
infix fun Int.has(b: DrawListFlag) = (this and b.i) != 0
infix fun Int.hasnt(b: DrawListFlag) = (this and b.i) == 0