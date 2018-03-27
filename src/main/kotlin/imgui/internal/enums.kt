package imgui.internal

//-----------------------------------------------------------------------------
// Types
//-----------------------------------------------------------------------------

// Internal Drag and Drop payload types. String starting with '_' are reserved for Dear ImGui.
val PAYLOAD_TYPE_DOCKABLE = "_IMDOCK"   // ImGuiWindow* // [Internal] Docking/tabs

enum class ButtonFlags {

    Null,
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
    /** don't set ActiveId while holding the mouse (ButtonFlags.PressedOnClick only) */
    NoHoldingActiveID,
    /** press when held into while we are drag and dropping another item (used by e.g. tree nodes, collapsing headers) */
    PressedOnDragDropHold,
    /** don't override navigation focus when activated; */
    NoNavFocus;

    val i = if (ordinal == 0) 0 else 1 shl (ordinal - 1)

    infix fun or(b: ButtonFlags) = i or b.i
}

infix fun Int.or(b: ButtonFlags) = this or b.i
infix fun Int.has(b: ButtonFlags) = (this and b.i) != 0
infix fun Int.hasnt(b: ButtonFlags) = (this and b.i) == 0

enum class SliderFlags(val i: Int) { Vertical(1 shl 0) }

infix fun Int.hasnt(b: SliderFlags) = (this and b.i) == 0

enum class ColumnsFlag(val i: Int) {

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

enum class SeparatorFlags {
    /** Axis default to current layout type, so generally Horizontal unless e.g. in a menu bar  */
    Horizontal,
    Vertical;

    val i = 1 shl ordinal
}

infix fun SeparatorFlags.or(b: SeparatorFlags) = i or b.i
infix fun Int.or(b: SeparatorFlags) = this or b.i
infix fun Int.has(b: SeparatorFlags) = (this and b.i) != 0
infix fun Int.hasnt(b: SeparatorFlags) = (this and b.i) == 0

/** Storage for LastItem data   */
enum class ItemStatusFlags { HoveredRect, HasDisplayRect;

    val i = 1 shl ordinal
}

infix fun Int.wo(b: ItemStatusFlags) = and(b.i.inv())
infix fun Int.or(b: ItemStatusFlags) = or(b.i)
infix fun Int.has(b: ItemStatusFlags) = and(b.i) != 0
infix fun Int.hasnt(b: ItemStatusFlags) = and(b.i) == 0

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

enum class DataType { Int, Float, Vec2;

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

enum class NavHighlightFlags { TypeDefault, TypeThin, AlwaysDraw, NoRounding;

    val i = 1 shl ordinal
}

infix fun Int.has(b: NavHighlightFlags) = and(b.i) != 0
infix fun Int.hasnt(b: NavHighlightFlags) = and(b.i) == 0
infix fun NavHighlightFlags.or(b: NavHighlightFlags) = i or b.i

enum class NavDirSourceFlags { Keyboard, PadDPad, PadLStick;

    val i = 1 shl ordinal
}

infix fun NavDirSourceFlags.or(b: NavDirSourceFlags) = i or b.i
infix fun Int.has(b: NavDirSourceFlags) = and(b.i) != 0

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
    /** In your function calls you may use ~0 (= all bits sets) instead of ImDrawCornerFlags_All, as a convenience  */
    All(0xF)
}

infix fun DrawCornerFlag.or(b: DrawCornerFlag) = i or b.i
infix fun Int.or(b: DrawCornerFlag) = or(b.i)
infix fun Int.and(b: DrawCornerFlag) = and(b.i)
infix fun Int.has(b: DrawCornerFlag) = (this and b.i) != 0
infix fun Int.hasnt(b: DrawCornerFlag) = (this and b.i) == 0

// TODO check enum declarance position
enum class DrawListFlag { AntiAliasedLines, AntiAliasedFill;

    val i = 1 shl ordinal
}

infix fun DrawListFlag.or(b: DrawListFlag) = i or b.i
infix fun Int.or(b: DrawListFlag) = or(b.i)
infix fun Int.and(b: DrawListFlag) = and(b.i)
infix fun Int.has(b: DrawListFlag) = (this and b.i) != 0
infix fun Int.hasnt(b: DrawListFlag) = (this and b.i) == 0