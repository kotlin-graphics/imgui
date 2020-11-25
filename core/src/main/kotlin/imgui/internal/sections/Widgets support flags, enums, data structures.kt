package imgui.internal.sections

import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import imgui.internal.classes.Rect


//-----------------------------------------------------------------------------
// [SECTION] Widgets support: flags, enums, data structures
//-----------------------------------------------------------------------------

typealias ItemFlags = Int

/** Transient per-window flags, reset at the beginning of the frame. For child window, inherited from parent on first Begin().
 *  This is going to be exposed in imgui.h when stabilized enough. */
enum class ItemFlag(@JvmField val i: ItemFlags) {
    None(0),
    NoTabStop(1 shl 0),  // false

    /** Button() will return true multiple times based on io.KeyRepeatDelay and io.KeyRepeatRate settings. */
    ButtonRepeat(1 shl 1),  // false

    /** [BETA] Disable interactions but doesn't affect visuals yet. See github.com/ocornut/imgui/issues/211 */
    Disabled(1 shl 2),  // false
    NoNav(1 shl 3),  // false
    NoNavDefaultFocus(1 shl 4),  // false

    /** MenuItem/Selectable() automatically closes current Popup window */
    SelectableDontClosePopup(1 shl 5),  // false

    /** [BETA] Represent a mixed/indeterminate value, generally multi-selection where values differ. Currently only supported by Checkbox() (later should support all sorts of widgets) */
    MixedValue(1 shl 6),  // false

    /** [ALPHA] Allow hovering interactions but underlying value is not changed. */
    ReadOnly(1 shl 7),  // false

    Default_(0);

    infix fun and(b: ItemFlag): ItemFlags = i and b.i
    infix fun and(b: ItemFlags): ItemFlags = i and b
    infix fun or(b: ItemFlag): ItemFlags = i or b.i
    infix fun or(b: ItemFlags): ItemFlags = i or b
    infix fun xor(b: ItemFlag): ItemFlags = i xor b.i
    infix fun xor(b: ItemFlags): ItemFlags = i xor b
    infix fun wo(b: ItemFlags): ItemFlags = and(b.inv())
}

infix fun ItemFlags.and(b: ItemFlag): ItemFlags = and(b.i)
infix fun ItemFlags.or(b: ItemFlag): ItemFlags = or(b.i)
infix fun ItemFlags.xor(b: ItemFlag): ItemFlags = xor(b.i)
infix fun ItemFlags.has(b: ItemFlag): Boolean = and(b.i) != 0
infix fun ItemFlags.hasnt(b: ItemFlag): Boolean = and(b.i) == 0
infix fun ItemFlags.wo(b: ItemFlag): ItemFlags = and(b.i.inv())


typealias ItemStatusFlags = Int

/** Storage for LastItem data   */
enum class ItemStatusFlag(@JvmField val i: ItemStatusFlags) {
    None(0),
    HoveredRect(1 shl 0),
    HasDisplayRect(1 shl 1),

    /** Value exposed by item was edited in the current frame (should match the bool return value of most widgets) */
    Edited(1 shl 2),

    /** Set when Selectable(), TreeNode() reports toggling a selection. We can't report "Selected" because reporting
     *  the change allows us to handle clipping with less issues. */
    ToggledSelection(1 shl 3),

    /** Set when TreeNode() reports toggling their open state. */
    ToggledOpen(1 shl 4),

    /** Set if the widget/group is able to provide data for the ImGuiItemStatusFlags_Deactivated flag. */
    HasDeactivated(1 shl 5),

    /** Only valid if ImGuiItemStatusFlags_HasDeactivated is set. */
    Deactivated(1 shl 6),

    //  #ifdef IMGUI_ENABLE_TEST_ENGINE
//  [imgui-test only]
    Openable(1 shl 10),
    Opened(1 shl 11),
    Checkable(1 shl 12),
    Checked(1 shl 13);

    infix fun and(b: ItemStatusFlag): ItemStatusFlags = i and b.i
    infix fun and(b: ItemStatusFlags): ItemStatusFlags = i and b
    infix fun or(b: ItemStatusFlag): ItemStatusFlags = i or b.i
    infix fun or(b: ItemStatusFlags): ItemStatusFlags = i or b
    infix fun xor(b: ItemStatusFlag): ItemStatusFlags = i xor b.i
    infix fun xor(b: ItemStatusFlags): ItemStatusFlags = i xor b
    infix fun wo(b: ItemStatusFlags): ItemStatusFlags = and(b.inv())
}

infix fun ItemStatusFlags.and(b: ItemStatusFlag): ItemStatusFlags = and(b.i)
infix fun ItemStatusFlags.or(b: ItemStatusFlag): ItemStatusFlags = or(b.i)
infix fun ItemStatusFlags.xor(b: ItemStatusFlag): ItemStatusFlags = xor(b.i)
infix fun ItemStatusFlags.has(b: ItemStatusFlag): Boolean = and(b.i) != 0
infix fun ItemStatusFlags.hasnt(b: ItemStatusFlag): Boolean = and(b.i) == 0
infix fun ItemStatusFlags.wo(b: ItemStatusFlag): ItemStatusFlags = and(b.i.inv())


enum class ButtonFlag(val i: ButtonFlags) {

    None(0),

    /** React on left mouse button (default) */
    MouseButtonLeft(1 shl 0),

    /** React on right mouse button */
    MouseButtonRight(1 shl 1),

    /** React on center mouse button */
    MouseButtonMiddle(1 shl 2),

    /** return true on click (mouse down event) */
    PressedOnClick(1 shl 4),

    /** [Default] return true on click + release on same item <-- this is what the majority of Button are using */
    PressedOnClickRelease(1 shl 5),

    /** return true on click + release even if the release event is not done while hovering the item */
    PressedOnClickReleaseAnywhere(1 shl 6),

    /** return true on release (default requires click+release) */
    PressedOnRelease(1 shl 7),

    /** return true on double-click (default requires click+release) */
    PressedOnDoubleClick(1 shl 8),

    /** return true when held into while we are drag and dropping another item (used by e.g. tree nodes, collapsing headers) */
    PressedOnDragDropHold(1 shl 9),

    /** hold to repeat  */
    Repeat(1 shl 10),

    /** allow interactions even if a child window is overlapping */
    FlattenChildren(1 shl 11),

    /** require previous frame HoveredId to either match id or be null before being usable, use along with SetItemAllowOverlap() */
    AllowItemOverlap(1 shl 12),

    /** disable automatically closing parent popup on press // [UNUSED] */
    DontClosePopups(1 shl 13),

    /** disable interactions */
    Disabled(1 shl 14),

    /** vertically align button to match text baseline - ButtonEx() only // FIXME: Should be removed and handled by SmallButton(), not possible currently because of DC.CursorPosPrevLine */
    AlignTextBaseLine(1 shl 15),

    /** disable mouse interaction if a key modifier is held */
    NoKeyModifiers(1 shl 16),

    /** don't set ActiveId while holding the mouse (ImGuiButtonFlags_PressedOnClick only) */
    NoHoldingActiveId(1 shl 17),

    /** don't override navigation focus when activated */
    NoNavFocus(1 shl 18),

    /** don't report as hovered when nav focus is on this item */
    NoHoveredOnFocus(1 shl 19),

    MouseButtonMask_(MouseButtonLeft or MouseButtonRight or MouseButtonMiddle),
    MouseButtonShift_(16),
    MouseButtonDefault_(MouseButtonLeft.i),
    PressedOnMask_(PressedOnClick or PressedOnClickRelease or PressedOnClickReleaseAnywhere or PressedOnRelease or PressedOnDoubleClick or PressedOnDragDropHold),
    PressedOnDefault_(PressedOnClickRelease.i);

    infix fun and(b: ButtonFlag): ButtonFlags = i and b.i
    infix fun and(b: ButtonFlags): ButtonFlags = i and b
    infix fun or(b: ButtonFlag): ButtonFlags = i or b.i
    infix fun or(b: ButtonFlags): ButtonFlags = i or b
    infix fun xor(b: ButtonFlag): ButtonFlags = i xor b.i
    infix fun xor(b: ButtonFlags): ButtonFlags = i xor b
    infix fun wo(b: ButtonFlags): ButtonFlags = and(b.inv())
}

infix fun ButtonFlags.and(b: ButtonFlag): ButtonFlags = and(b.i)
infix fun ButtonFlags.or(b: ButtonFlag): ButtonFlags = or(b.i)
infix fun ButtonFlags.xor(b: ButtonFlag): ButtonFlags = xor(b.i)
infix fun ButtonFlags.has(b: ButtonFlag): Boolean = and(b.i) != 0
infix fun ButtonFlags.hasnt(b: ButtonFlag): Boolean = and(b.i) == 0
infix fun ButtonFlags.wo(b: ButtonFlag): ButtonFlags = and(b.i.inv())

typealias ButtonFlags = Int


typealias SeparatorFlags = Int

enum class SeparatorFlag {
    None,

    /** Axis default to current layout type, so generally Horizontal unless e.g. in a menu bar  */
    Horizontal,
    Vertical,
    SpanAllColumns;

    val i: SeparatorFlags = if (ordinal == 0) 0 else 1 shl (ordinal - 1)

    infix fun and(b: SeparatorFlag): SeparatorFlags = i and b.i
    infix fun and(b: SeparatorFlags): SeparatorFlags = i and b
    infix fun or(b: SeparatorFlag): SeparatorFlags = i or b.i
    infix fun or(b: SeparatorFlags): SeparatorFlags = i or b
    infix fun xor(b: SeparatorFlag): SeparatorFlags = i xor b.i
    infix fun xor(b: SeparatorFlags): SeparatorFlags = i xor b
    infix fun wo(b: SeparatorFlags): SeparatorFlags = and(b.inv())
}

infix fun SeparatorFlags.and(b: SeparatorFlag): SeparatorFlags = and(b.i)
infix fun SeparatorFlags.or(b: SeparatorFlag): SeparatorFlags = or(b.i)
infix fun SeparatorFlags.xor(b: SeparatorFlag): SeparatorFlags = xor(b.i)
infix fun SeparatorFlags.has(b: SeparatorFlag): Boolean = and(b.i) != 0
infix fun SeparatorFlags.hasnt(b: SeparatorFlag): Boolean = and(b.i) == 0
infix fun SeparatorFlags.wo(b: SeparatorFlag): SeparatorFlags = and(b.i.inv())



typealias TextFlags = Int

enum class TextFlag {
    None, NoWidthForLargeClippedText;

    val i: TextFlags = ordinal

    infix fun and(b: TextFlag): TextFlags = i and b.i
    infix fun and(b: TextFlags): TextFlags = i and b
    infix fun or(b: TextFlag): TextFlags = i or b.i
    infix fun or(b: TextFlags): TextFlags = i or b
    infix fun xor(b: TextFlag): TextFlags = i xor b.i
    infix fun xor(b: TextFlags): TextFlags = i xor b
    infix fun wo(b: TextFlags): TextFlags = and(b.inv())
}

infix fun TextFlags.and(b: TextFlag): TextFlags = and(b.i)
infix fun TextFlags.or(b: TextFlag): TextFlags = or(b.i)
infix fun TextFlags.xor(b: TextFlag): TextFlags = xor(b.i)
infix fun TextFlags.has(b: TextFlag): Boolean = and(b.i) != 0
infix fun TextFlags.hasnt(b: TextFlag): Boolean = and(b.i) == 0
infix fun TextFlags.wo(b: TextFlag): TextFlags = and(b.i.inv())


typealias TooltipFlags = Int

enum class TooltipFlag(val i: TooltipFlags) {
    None(0),

    /** Override will clear/ignore previously submitted tooltip (defaults to append) */
    OverridePreviousTooltip(1 shl 0);

    infix fun and(b: TooltipFlag): TooltipFlags = i and b.i
    infix fun and(b: TooltipFlags): TooltipFlags = i and b
    infix fun or(b: TooltipFlag): TooltipFlags = i or b.i
    infix fun or(b: TooltipFlags): TooltipFlags = i or b
    infix fun xor(b: TooltipFlag): TooltipFlags = i xor b.i
    infix fun xor(b: TooltipFlags): TooltipFlags = i xor b
    infix fun wo(b: TooltipFlags): TooltipFlags = and(b.inv())
}

infix fun TooltipFlags.and(b: TooltipFlag): TooltipFlags = and(b.i)
infix fun TooltipFlags.or(b: TooltipFlag): TooltipFlags = or(b.i)
infix fun TooltipFlags.xor(b: TooltipFlag): TooltipFlags = xor(b.i)
infix fun TooltipFlags.has(b: TooltipFlag): Boolean = and(b.i) != 0
infix fun TooltipFlags.hasnt(b: TooltipFlag): Boolean = and(b.i) == 0
infix fun TooltipFlags.wo(b: TooltipFlag): TooltipFlags = and(b.i.inv())


/** FIXME: this is in development, not exposed/functional as a generic feature yet.
 *  Horizontal/Vertical enums are fixed to 0/1 so they may be used to index ImVec2 */
enum class LayoutType { Horizontal, Vertical }


enum class LogType { None, TTY, File, Buffer, Clipboard }


/** X/Y enums are fixed to 0/1 so they may be used to index ImVec2 */
enum class Axis {
    None, X, Y;

    infix fun xor(i: Int) = (ordinal - 1) xor i
}

operator fun Vec2i.get(axis: Axis): Int = when (axis) {
    Axis.X -> x
    Axis.Y -> y
    else -> throw Error()
}

operator fun Vec2.get(axis: Axis): Float = when (axis) {
    Axis.X -> x
    Axis.Y -> y
    else -> throw Error()
}

operator fun Vec2.set(axis: Axis, float: Float) = when (axis) {
    Axis.X -> x = float
    Axis.Y -> y = float
    else -> throw Error()
}

infix fun Int.shl(b: Axis) = shl(b.ordinal - 1)


enum class PlotType { Lines, Histogram }


enum class InputSource {
    None, Mouse, Nav,

    /** Only used occasionally for storage, not tested/handled by most code */
    NavKeyboard,

    /** Only used occasionally for storage, not tested/handled by most code */
    NavGamepad
}


// FIXME-NAV: Clarify/expose various repeat delay/rate
enum class InputReadMode { Down, Pressed, Released, Repeat, RepeatSlow, RepeatFast }


typealias NavHighlightFlags = Int

enum class NavHighlightFlag {
    None, TypeDefault, TypeThin,

    /** Draw rectangular highlight if (g.NavId == id) _even_ when using the mouse. */
    AlwaysDraw,
    NoRounding;

    val i: NavHighlightFlags = if (ordinal == 0) 0 else 1 shl ordinal

    infix fun and(b: NavHighlightFlag): NavHighlightFlags = i and b.i
    infix fun and(b: NavHighlightFlags): NavHighlightFlags = i and b
    infix fun or(b: NavHighlightFlag): NavHighlightFlags = i or b.i
    infix fun or(b: NavHighlightFlags): NavHighlightFlags = i or b
    infix fun xor(b: NavHighlightFlag): NavHighlightFlags = i xor b.i
    infix fun xor(b: NavHighlightFlags): NavHighlightFlags = i xor b
    infix fun wo(b: NavHighlightFlags): NavHighlightFlags = and(b.inv())
}

infix fun NavHighlightFlags.and(b: NavHighlightFlag): NavHighlightFlags = and(b.i)
infix fun NavHighlightFlags.or(b: NavHighlightFlag): NavHighlightFlags = or(b.i)
infix fun NavHighlightFlags.xor(b: NavHighlightFlag): NavHighlightFlags = xor(b.i)
infix fun NavHighlightFlags.has(b: NavHighlightFlag): Boolean = and(b.i) != 0
infix fun NavHighlightFlags.hasnt(b: NavHighlightFlag): Boolean = and(b.i) == 0
infix fun NavHighlightFlags.wo(b: NavHighlightFlag): NavHighlightFlags = and(b.i.inv())



typealias NavDirSourceFlags = Int

enum class NavDirSourceFlag {
    None, Keyboard, PadDPad, PadLStick;

    val i: NavDirSourceFlags = if (ordinal == 0) 0 else 1 shl ordinal

    infix fun and(b: NavDirSourceFlag): NavDirSourceFlags = i and b.i
    infix fun and(b: NavDirSourceFlags): NavDirSourceFlags = i and b
    infix fun or(b: NavDirSourceFlag): NavDirSourceFlags = i or b.i
    infix fun or(b: NavDirSourceFlags): NavDirSourceFlags = i or b
    infix fun xor(b: NavDirSourceFlag): NavDirSourceFlags = i xor b.i
    infix fun xor(b: NavDirSourceFlags): NavDirSourceFlags = i xor b
    infix fun wo(b: NavDirSourceFlags): NavDirSourceFlags = and(b.inv())
}

infix fun NavDirSourceFlags.and(b: NavDirSourceFlag): NavDirSourceFlags = and(b.i)
infix fun NavDirSourceFlags.or(b: NavDirSourceFlag): NavDirSourceFlags = or(b.i)
infix fun NavDirSourceFlags.xor(b: NavDirSourceFlag): NavDirSourceFlags = xor(b.i)
infix fun NavDirSourceFlags.has(b: NavDirSourceFlag): Boolean = and(b.i) != 0
infix fun NavDirSourceFlags.hasnt(b: NavDirSourceFlag): Boolean = and(b.i) == 0
infix fun NavDirSourceFlags.wo(b: NavDirSourceFlag): NavDirSourceFlags = and(b.i.inv())


typealias NavMoveFlags = Int

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
    AlsoScoreVisibleSet,
    ScrollToEdge;

    val i: NavMoveFlags = if (ordinal == 0) 0 else 1 shl ordinal

    infix fun and(b: NavMoveFlag): NavMoveFlags = i and b.i
    infix fun and(b: NavMoveFlags): NavMoveFlags = i and b
    infix fun or(b: NavMoveFlag): NavMoveFlags = i or b.i
    infix fun or(b: NavMoveFlags): NavMoveFlags = i or b
    infix fun xor(b: NavMoveFlag): NavMoveFlags = i xor b.i
    infix fun xor(b: NavMoveFlags): NavMoveFlags = i xor b
    infix fun wo(b: NavMoveFlags): NavMoveFlags = and(b.inv())
}

infix fun NavMoveFlags.and(b: NavMoveFlag): NavMoveFlags = and(b.i)
infix fun NavMoveFlags.or(b: NavMoveFlag): NavMoveFlags = or(b.i)
infix fun NavMoveFlags.xor(b: NavMoveFlag): NavMoveFlags = xor(b.i)
infix fun NavMoveFlags.has(b: NavMoveFlag): Boolean = and(b.i) != 0
infix fun NavMoveFlags.hasnt(b: NavMoveFlag): Boolean = and(b.i) == 0
infix fun NavMoveFlags.wo(b: NavMoveFlag): NavMoveFlags = and(b.i.inv())


enum class NavForward { None, ForwardQueued, ForwardActive }

enum class NavLayer {
    /** Main scrolling layer */
    Main,

    /** Menu layer (access with Alt/ImGuiNavInput_Menu) */
    Menu;

    infix fun xor(int: Int): Int = ordinal xor int

    companion object {
        val COUNT = values().size
        infix fun of(i: Int) = values().first { it.ordinal == i }
    }
}

operator fun Array<Rect>.get(index: NavLayer): Rect = get(index.ordinal)
operator fun Array<Rect>.set(index: NavLayer, rect: Rect) = set(index.ordinal, rect)
operator fun IntArray.get(index: NavLayer): Int = get(index.ordinal)
operator fun IntArray.set(index: NavLayer, int: Int) = set(index.ordinal, int)
infix fun Int.shl(layer: NavLayer): Int = shl(layer.ordinal)


enum class PopupPositionPolicy { Default, ComboBox, Tooltip }


typealias DrawCornerFlags = Int

/** Flags: for ImDrawList::AddRect(), AddRectFilled() etc. */
enum class DrawCornerFlag(val i: DrawCornerFlags) {
    None(0),
    TopLeft(1 shl 0), // 0x1
    TopRight(1 shl 1), // 0x2
    BotLeft(1 shl 2), // 0x4
    BotRight(1 shl 3), // 0x8
    Top(TopLeft or TopRight),   // 0x3
    Bot(BotLeft or BotRight),   // 0xC
    Left(TopLeft or BotLeft),    // 0x5
    Right(TopRight or BotRight),  // 0xA

    /** In your function calls you may use ~0 (= all bits sets) instead of DrawCornerFlags.All, as a convenience  */
    All(0xF);

    infix fun and(b: DrawCornerFlag): DrawCornerFlags = i and b.i
    infix fun and(b: DrawCornerFlags): DrawCornerFlags = i and b
    infix fun or(b: DrawCornerFlag): DrawCornerFlags = i or b.i
    infix fun or(b: DrawCornerFlags): DrawCornerFlags = i or b
    infix fun xor(b: DrawCornerFlag): DrawCornerFlags = i xor b.i
    infix fun xor(b: DrawCornerFlags): DrawCornerFlags = i xor b
    infix fun wo(b: DrawCornerFlags): DrawCornerFlags = and(b.inv())
}

infix fun DrawCornerFlags.and(b: DrawCornerFlag): DrawCornerFlags = and(b.i)
infix fun DrawCornerFlags.or(b: DrawCornerFlag): DrawCornerFlags = or(b.i)
infix fun DrawCornerFlags.xor(b: DrawCornerFlag): DrawCornerFlags = xor(b.i)
infix fun DrawCornerFlags.has(b: DrawCornerFlag): Boolean = and(b.i) != 0
infix fun DrawCornerFlags.hasnt(b: DrawCornerFlag): Boolean = and(b.i) == 0
infix fun DrawCornerFlags.wo(b: DrawCornerFlag): DrawCornerFlags = and(b.i.inv())


typealias DrawListFlags = Int

/** Flags for ImDrawList. Those are set automatically by ImGui:: functions from ImGuiIO settings, and generally not
 *  manipulated directly. It is however possible to temporarily alter flags between calls to ImDrawList:: functions. */
enum class DrawListFlag(val i: DrawListFlags) {
    None(0),

    /** Enable anti-aliased lines/borders (*2 the number of triangles for 1.0f wide line or lines thin enough to be
     *  drawn using textures, otherwise *3 the number of triangles) */
    AntiAliasedLines(1 shl 0),

    /** Enable anti-aliased lines/borders using textures when possible. Require backend to render with bilinear filtering. */
    AntiAliasedLinesUseTex(1 shl 1),

    /** Enable anti-aliased edge around filled shapes (rounded rectangles, circles). */
    AntiAliasedFill(1 shl 2),

    /** Can emit 'VtxOffset > 0' to allow large meshes. Set when 'ImGuiBackendFlags_RendererHasVtxOffset' is enabled. */
    AllowVtxOffset(1 shl 3);

    infix fun and(b: DrawListFlag): DrawListFlags = i and b.i
    infix fun and(b: DrawListFlags): DrawListFlags = i and b
    infix fun or(b: DrawListFlag): DrawListFlags = i or b.i
    infix fun or(b: DrawListFlags): DrawListFlags = i or b
    infix fun xor(b: DrawListFlag): DrawListFlags = i xor b.i
    infix fun xor(b: DrawListFlags): DrawListFlags = i xor b
    infix fun wo(b: DrawListFlags): DrawListFlags = and(b.inv())
}

infix fun DrawListFlags.and(b: DrawListFlag): DrawListFlags = and(b.i)
infix fun DrawListFlags.or(b: DrawListFlag): DrawListFlags = or(b.i)
infix fun DrawListFlags.xor(b: DrawListFlag): DrawListFlags = xor(b.i)
infix fun DrawListFlags.has(b: DrawListFlag): Boolean = and(b.i) != 0
infix fun DrawListFlags.hasnt(b: DrawListFlag): Boolean = and(b.i) == 0
infix fun DrawListFlags.wo(b: DrawListFlag): DrawListFlags = and(b.i.inv())



typealias NextWindowDataFlags = Int

enum class NextWindowDataFlag {
    None, HasPos, HasSize, HasContentSize, HasCollapsed, HasSizeConstraint, HasFocus, HasBgAlpha, HasScroll,
    HasViewport, HasDock, HasWindowClass;

    val i: NextWindowDataFlags = if (ordinal == 0) 0 else 1 shl (ordinal - 1)

    infix fun and(b: NextWindowDataFlag): NextWindowDataFlags = i and b.i
    infix fun and(b: NextWindowDataFlags): NextWindowDataFlags = i and b
    infix fun or(b: NextWindowDataFlag): NextWindowDataFlags = i or b.i
    infix fun or(b: NextWindowDataFlags): NextWindowDataFlags = i or b
    infix fun xor(b: NextWindowDataFlag): NextWindowDataFlags = i xor b.i
    infix fun xor(b: NextWindowDataFlags): NextWindowDataFlags = i xor b
    infix fun wo(b: NextWindowDataFlags): NextWindowDataFlags = and(b.inv())
}

infix fun NextWindowDataFlags.and(b: NextWindowDataFlag): NextWindowDataFlags = and(b.i)
infix fun NextWindowDataFlags.or(b: NextWindowDataFlag): NextWindowDataFlags = or(b.i)
infix fun NextWindowDataFlags.xor(b: NextWindowDataFlag): NextWindowDataFlags = xor(b.i)
infix fun NextWindowDataFlags.has(b: NextWindowDataFlag): Boolean = and(b.i) != 0
infix fun NextWindowDataFlags.hasnt(b: NextWindowDataFlag): Boolean = and(b.i) == 0
infix fun NextWindowDataFlags.wo(b: NextWindowDataFlag): NextWindowDataFlags = and(b.i.inv())


typealias NextItemDataFlags = Int

enum class NextItemDataFlag(val i: NextItemDataFlags) {
    None(0),
    HasWidth(1 shl 0),
    HasOpen(1 shl 1);

    infix fun and(b: NextItemDataFlag): NextItemDataFlags = i and b.i
    infix fun and(b: NextItemDataFlags): NextItemDataFlags = i and b
    infix fun or(b: NextItemDataFlag): NextItemDataFlags = i or b.i
    infix fun or(b: NextItemDataFlags): NextItemDataFlags = i or b
    infix fun xor(b: NextItemDataFlag): NextItemDataFlags = i xor b.i
    infix fun xor(b: NextItemDataFlags): NextItemDataFlags = i xor b
    infix fun wo(b: NextItemDataFlags): NextItemDataFlags = and(b.inv())
}

infix fun NextItemDataFlags.and(b: NextItemDataFlag): NextItemDataFlags = and(b.i)
infix fun NextItemDataFlags.or(b: NextItemDataFlag): NextItemDataFlags = or(b.i)
infix fun NextItemDataFlags.xor(b: NextItemDataFlag): NextItemDataFlags = xor(b.i)
infix fun NextItemDataFlags.has(b: NextItemDataFlag): Boolean = and(b.i) != 0
infix fun NextItemDataFlags.hasnt(b: NextItemDataFlag): Boolean = and(b.i) == 0
infix fun NextItemDataFlags.wo(b: NextItemDataFlag): NextItemDataFlags = and(b.i.inv())