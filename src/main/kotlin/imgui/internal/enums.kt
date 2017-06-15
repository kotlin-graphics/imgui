package imgui.internal

//-----------------------------------------------------------------------------
// Types
//-----------------------------------------------------------------------------

enum class ButtonFlags_ {
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

    val i = 1 shl ordinal
}

enum class SliderFlags_(val i: Int) { Vertical(1 shl 0) }

/** NB: need to be in sync with last value of SelectableFlags_  */
enum class SelectableFlagsPrivate_(val i: Int) {
    Menu(1 shl 3),
    MenuItem(1 shl 4),
    Disabled(1 shl 5),
    DrawFillAvailWidth(1 shl 6)
}

/** FIXME: this is in development, not exposed/functional as a generic feature yet. */
enum class LayoutType_ {
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
    Float2;

    val i = ordinal
}

enum class Corner(val i: Int) {
    TopLeft(1 shl 0), // 1
    TopRight(1 shl 1), // 2
    BottomRight(1 shl 2), // 4
    BottomLeft(1 shl 3), // 8
    All(0x0F)
}