package imgui.internal

//-----------------------------------------------------------------------------
// Types
//-----------------------------------------------------------------------------

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
    FlattenChilds,
    /** disable automatically closing parent popup on press [UNUSED] */
    DontClosePopups,
    /** disable interactions */
    Disabled,
    /** vertically align button to match text baseline (buttonEx() only)    */
    AlignTextBaseLine,
    /** disable interaction if a key modifier is held   */
    NoKeyModifiers,
    /** require previous frame HoveredId to either match id or be null before being usable  */
    AllowOverlapMode,
    /** don't set ActiveId while holding the mouse (ImGuiButtonFlags_PressedOnClick only)   */
    NoHoldingActiveID;

    val i = if (ordinal == 0) 0 else 1 shl (ordinal - 1)

    infix fun or(b: ButtonFlags) = i or b.i
}

infix fun Int.or(b: ButtonFlags) = this or b.i
infix fun Int.has(b: ButtonFlags) = (this and b.i) != 0
infix fun Int.hasnt(b: ButtonFlags) = (this and b.i) == 0

enum class SliderFlags(val i: Int) { Vertical(1 shl 0) }

infix fun Int.hasnt(b: SliderFlags) = (this and b.i) == 0

enum class ColumnsFlags(val i: Int) {

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

infix fun Int.has(b: ColumnsFlags) = (this and b.i) != 0
infix fun Int.hasnt(b: ColumnsFlags) = (this and b.i) == 0

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

/** FIXME: this is in development, not exposed/functional as a generic feature yet. */
enum class LayoutType {
    Vertical,
    Horizontal;

    val i = ordinal
}

enum class PlotType {
    Lines,
    Histogram;

    val i = ordinal
}

enum class DataType {
    Int,
    Float,
    Vec2;

    val i = ordinal
}

enum class Dir { None, Left, Right, Up, Down;

    val i = ordinal - 1
}

enum class Corner(val i: Int) {
    Null(0),
    TopLeft(1 shl 0), // 1
    TopRight(1 shl 1), // 2
    BotRight(1 shl 2), // 4
    BotLeft(1 shl 3), // 8
    All(0x0F);

    infix fun or(b: Corner) = i or b.i
    infix fun or(b: Int) = i or b
}

infix fun Int.or(b: Corner) = this or b.i
infix fun Int.has(b: Corner) = (this and b.i) != 0