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
    NoNavFocus;

    val i = if (ordinal == 0) 0 else 1 shl (ordinal - 1)

    infix fun or(b: ButtonFlag): ButtonFlags = i or b.i
}

infix fun Int.or(b: ButtonFlag): ButtonFlags = this or b.i
infix fun Int.has(b: ButtonFlag) = (this and b.i) != 0
infix fun Int.hasnt(b: ButtonFlag) = (this and b.i) == 0

enum class SliderFlag(val i: Int) { None(0), Vertical(1 shl 0) }

infix fun Int.hasnt(b: SliderFlag) = (this and b.i) == 0

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

    val i = if(ordinal == 0) 0 else 1 shl ordinal
}

infix fun SeparatorFlag.or(b: SeparatorFlag): SeparatorFlags = i or b.i
infix fun Int.or(b: SeparatorFlag): SeparatorFlags = this or b.i
infix fun Int.has(b: SeparatorFlag) = (this and b.i) != 0
infix fun Int.hasnt(b: SeparatorFlag) = (this and b.i) == 0

/** Storage for LastItem data   */
enum class ItemStatusFlag { HoveredRect, HasDisplayRect;

    val i = 1 shl ordinal
}

infix fun Int.wo(b: ItemStatusFlag): ItemStatusFlags = and(b.i.inv())
infix fun Int.or(b: ItemStatusFlag): ItemStatusFlags = or(b.i)
infix fun Int.has(b: ItemStatusFlag) = and(b.i) != 0
infix fun Int.hasnt(b: ItemStatusFlag) = and(b.i) == 0

/** FIXME: this is in development, not exposed/functional as a generic feature yet. */
enum class LayoutType { Vertical, Horizontal;

    val i = ordinal
}

enum class Axis { None, X, Y;

    val i = ordinal - 1
}

infix fun Int.shl(b: Axis) = shl(b.i)

enum class PlotType { Lines, Histogram;

    val i = ordinal
}

enum class InputSource { None, Mouse, Nav,
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
enum class InputReadMode { Down, Pressed, Released, Repeat, RepeatSlow, RepeatFast;

    val i = ordinal
}

enum class NavHighlightFlag { None, TypeDefault, TypeThin, AlwaysDraw, NoRounding;

    val i = if(ordinal == 0) 0 else 1 shl ordinal
}

infix fun Int.has(b: NavHighlightFlag) = and(b.i) != 0
infix fun Int.hasnt(b: NavHighlightFlag) = and(b.i) == 0
infix fun NavHighlightFlag.or(b: NavHighlightFlag): NavHighlightFlags = i or b.i

enum class NavDirSourceFlag { None, Keyboard, PadDPad, PadLStick;

    val i = if(ordinal == 0) 0 else 1 shl ordinal
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

    val i = if(ordinal == 0) 0 else 1 shl ordinal

    infix fun or(b: NavMoveFlag): NavMoveFlags = i or b.i
}

infix fun Int.has(b: NavMoveFlag) = and(b.i) != 0

enum class NavForward { None, ForwardQueued, ForwardActive;

    val i = ordinal
}


// TODO check enum declarance position
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

// TODO check enum declarance position
enum class DrawListFlag { AntiAliasedLines, AntiAliasedFill;

    val i = 1 shl ordinal
}

infix fun DrawListFlag.or(b: DrawListFlag): DrawListFlags = i or b.i
infix fun Int.or(b: DrawListFlag): DrawListFlags = or(b.i)
infix fun Int.and(b: DrawListFlag): DrawListFlags = and(b.i)
infix fun Int.has(b: DrawListFlag) = (this and b.i) != 0
infix fun Int.hasnt(b: DrawListFlag) = (this and b.i) == 0