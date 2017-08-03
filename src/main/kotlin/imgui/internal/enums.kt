package imgui.internal

//-----------------------------------------------------------------------------
// Types
//-----------------------------------------------------------------------------

enum class ButtonFlags {

    Null,
    /** hold to repeat  */
    Repeat,
    /** (default) return pressed on click+release on same item (default if no PressedOn** flag is set)  */
    PressedOnClickRelease,
    /** return pressed on click (default requires click+release)    */
    PressedOnClick,
    /** return pressed on release (default requires click+release)  */
    PressedOnRelease,
    /** return pressed on double-click (default requires click+release) */
    PressedOnDoubleClick,
    /** allow interaction even if a child window is overlapping */
    FlattenChilds,
    /** disable automatically closing parent popup on press */
    DontClosePopups,
    /** disable interaction */
    Disabled,
    /** vertically align button to match text baseline - ButtonEx() only    */
    AlignTextBaseLine,
    /** disable interaction if a key modifier is held   */
    NoKeyModifiers,
    /** require previous frame HoveredId to either match id or be null before being usable  */
    AllowOverlapMode;

    val i = if (ordinal == 0) 0 else 1 shl (ordinal - 1)

    infix fun or(b: ButtonFlags) = i or b.i
}

infix fun Int.or(b: ButtonFlags) = this or b.i
infix fun Int.has(b: ButtonFlags) = (this and b.i) != 0
infix fun Int.hasnt(b: ButtonFlags) = (this and b.i) == 0

enum class SliderFlags(val i: Int) { Vertical(1 shl 0) }

infix fun Int.hasnt(b: SliderFlags) = (this and b.i) == 0

/** NB: need to be in sync with last value of SelectableFlags  */
@Deprecated("us")
enum class SelectableFlagsPrivate_(val i: Int) {
    Menu(1 shl 3),
    MenuItem(1 shl 4),
    Disabled(1 shl 5),
    DrawFillAvailWidth(1 shl 6)
}

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

enum class Corner(val i: Int) {
    TopLeft(1 shl 0), // 1
    TopRight(1 shl 1), // 2
    BottomRight(1 shl 2), // 4
    BottomLeft(1 shl 3), // 8
    All(0x0F);

    infix fun or(b: Corner) = i or b.i
    infix fun or(b: Int) = i or b
}