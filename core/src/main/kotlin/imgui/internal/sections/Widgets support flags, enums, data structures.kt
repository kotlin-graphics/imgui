package imgui.internal.sections

import glm_.vec2.Vec2
import imgui.ID
import imgui.KeyModFlags
import imgui.internal.classes.Rect
import imgui.internal.classes.Window


//-----------------------------------------------------------------------------
// [SECTION] Widgets support: flags, enums, data structures
//-----------------------------------------------------------------------------

typealias ItemFlags = Int

/** Transient per-window flags, reset at the beginning of the frame. For child window, inherited from parent on first Begin().
 *  This is going to be exposed in imgui.h when stabilized enough. */
enum class ItemFlag(@JvmField val i: ItemFlags) {
    None(0),

    /** Disable keyboard tabbing (FIXME: should merge with _NoNav) */
    NoTabStop(1 shl 0),  // false

    /** Button() will return true multiple times based on io.KeyRepeatDelay and io.KeyRepeatRate settings. */
    ButtonRepeat(1 shl 1),  // false

    /** Disable interactions but doesn't affect visuals. See BeginDisabled()/EndDisabled(). See github.com/ocornut/imgui/issues/211 */
    Disabled(1 shl 2),  // false

    /** Disable keyboard/gamepad directional navigation (FIXME: should merge with _NoTabStop) */
    NoNav(1 shl 3),  // false

    /** Disable item being a candidate for default focus (e.g. used by title bar items) */
    NoNavDefaultFocus(1 shl 4),  // false

    /** Disable MenuItem/Selectable() automatically closing their popup window */
    SelectableDontClosePopup(1 shl 5),  // false

    /** [BETA] Represent a mixed/indeterminate value, generally multi-selection where values differ. Currently only supported by Checkbox() (later should support all sorts of widgets) */
    MixedValue(1 shl 6),  // false

    /** [ALPHA] Allow hovering interactions but underlying value is not changed. */
    ReadOnly(1 shl 7),  // false

    /** [WIP] Auto-activate input mode when tab focused. Currently only used and supported by a few items before it becomes a generic feature. */
    Inputable(1 shl 8);   // false

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
operator fun ItemFlags.minus(flag: ItemFlag): ItemFlags = wo(flag)
operator fun ItemFlags.div(flag: ItemFlag): ItemFlags = or(flag)


typealias ItemStatusFlags = Int

/** Storage for LastItem data   */
enum class ItemStatusFlag(@JvmField val i: ItemStatusFlags) {
    None(0),

    /** Mouse position is within item rectangle (does NOT mean that the window is in correct z-order and can be hovered!, this is only one part of the most-common IsItemHovered test) */
    HoveredRect(1 shl 0),

    /** g.LastItemData.DisplayRect is valid */
    HasDisplayRect(1 shl 1),

    /** Value exposed by item was edited in the current frame (should match the bool return value of most widgets) */
    Edited(1 shl 2),

    /** Set when Selectable(), TreeNode() reports toggling a selection. We can't report "Selected", only state changes, in order to easily handle clipping with less issues. */
    ToggledSelection(1 shl 3),

    /** Set when TreeNode() reports toggling their open state. */
    ToggledOpen(1 shl 4),

    /** Set if the widget/group is able to provide data for the ImGuiItemStatusFlags_Deactivated flag. */
    HasDeactivated(1 shl 5),

    /** Only valid if ImGuiItemStatusFlags_HasDeactivated is set. */
    Deactivated(1 shl 6),

    /** Override the HoveredWindow test to allow cross-window hover testing. */
    HoveredWindow(1 shl 7),

    /** Set when the Focusable item just got focused by Tabbing (FIXME: to be removed soon) */
    FocusedByTabbing(1 shl 8),

    //  #ifdef IMGUI_ENABLE_TEST_ENGINE
    //  [imgui-test only]
    Openable(1 shl 20),
    Opened(1 shl 21),
    Checkable(1 shl 22),
    Checked(1 shl 23);

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
operator fun ItemStatusFlags.minus(flag: ItemStatusFlag): ItemStatusFlags = wo(flag)
operator fun ItemStatusFlags.div(flag: ItemStatusFlag): ItemStatusFlags = or(flag)


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

    /** disable interactions -> use BeginDisabled() or ImGuiItemFlags_Disabled */
    //    Disabled(1 shl 14),

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
operator fun SeparatorFlags.minus(flag: SeparatorFlag): SeparatorFlags = wo(flag)
operator fun SeparatorFlags.div(flag: SeparatorFlag): SeparatorFlags = or(flag)


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

enum class PopupPositionPolicy { Default, ComboBox, Tooltip }


typealias DrawFlags = Int

/** Flags for ImDrawList functions
 *  (Legacy: bit 0 must always correspond to ImDrawFlags_Closed to be backward compatible with old API using a bool. Bits 1..3 must be unused) */
enum class DrawFlag(val i: DrawFlags) {
    None(0),

    /** PathStroke(), AddPolyline(): specify that shape should be closed (Important: this is always == 1 for legacy reason) */
    Closed(1 shl 0),

    // (bits 1..3 unused to facilitate handling of legacy behavior and detection of Flags = 0x0F)

    /** AddRect(), AddRectFilled(), PathRect(): enable rounding top-left corner only (when rounding > 0.0f, we default to all corners). Was 0x01. */
    RoundCornersTopLeft(1 shl 4),

    /** AddRect(), AddRectFilled(), PathRect(): enable rounding top-right corner only (when rounding > 0.0f, we default to all corners). Was 0x02. */
    RoundCornersTopRight(1 shl 5),

    /** AddRect(), AddRectFilled(), PathRect(): enable rounding bottom-left corner only (when rounding > 0.0f, we default to all corners). Was 0x04. */
    RoundCornersBottomLeft(1 shl 6),

    /** AddRect(), AddRectFilled(), PathRect(): enable rounding bottom-right corner only (when rounding > 0.0f, we default to all corners). Wax 0x08. */
    RoundCornersBottomRight(1 shl 7),

    /** AddRect(), AddRectFilled(), PathRect(): disable rounding on all corners (when rounding > 0.0f). This is NOT zero, NOT an implicit flag! */
    RoundCornersNone(1 shl 8),
    RoundCornersTop(RoundCornersTopLeft or RoundCornersTopRight),
    RoundCornersBottom(RoundCornersBottomLeft or RoundCornersBottomRight),
    RoundCornersLeft(RoundCornersBottomLeft or RoundCornersTopLeft),
    RoundCornersRight(RoundCornersBottomRight or RoundCornersTopRight),
    RoundCornersAll(RoundCornersTopLeft or RoundCornersTopRight or RoundCornersBottomLeft or RoundCornersBottomRight),

    /** Default to ALL corners if none of the _RoundCornersXX flags are specified. */
    RoundCornersDefault_(RoundCornersAll.i),
    RoundCornersMask_(RoundCornersAll or RoundCornersNone);

    infix fun and(b: DrawFlag): DrawFlags = i and b.i
    infix fun and(b: DrawFlags): DrawFlags = i and b
    infix fun or(b: DrawFlag): DrawFlags = i or b.i
    infix fun or(b: DrawFlags): DrawFlags = i or b
    infix fun xor(b: DrawFlag): DrawFlags = i xor b.i
    infix fun xor(b: DrawFlags): DrawFlags = i xor b
    infix fun wo(b: DrawFlags): DrawFlags = and(b.inv())
}

infix fun DrawFlags.and(b: DrawFlag): DrawFlags = and(b.i)
infix fun DrawFlags.or(b: DrawFlag): DrawFlags = or(b.i)
infix fun DrawFlags.xor(b: DrawFlag): DrawFlags = xor(b.i)
infix fun DrawFlags.has(b: DrawFlag): Boolean = and(b.i) != 0
infix fun DrawFlags.hasnt(b: DrawFlag): Boolean = and(b.i) == 0
infix fun DrawFlags.wo(b: DrawFlag): DrawFlags = and(b.i.inv())


typealias DrawListFlags = Int

/** Flags: for ImDrawList instance. Those are set automatically by ImGui:: functions from ImGuiIO settings, and generally not
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
    None, HasPos, HasSize, HasContentSize, HasCollapsed, HasSizeConstraint, HasFocus, HasBgAlpha, HasScroll;

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


/** Result of a gamepad/keyboard directional navigation move query result */
class NavItemData {
    /** Init,Move    // Best candidate window (result->ItemWindow->RootWindowForNav == request->Window) */
    var window: Window? = null

    /** Init,Move    // Best candidate item ID */
    var id: ID = 0

    /** Init,Move    // Best candidate focus scope ID */
    var focusScopeId: ID = 0

    /** Init,Move    // Best candidate bounding box in window relative space */
    lateinit var rectRel: Rect

    /** ????,Move    // Best candidate item flags */
    var inFlags = ItemFlag.None.i

    /**      Move    // Best candidate box distance to current NavId */
    var distBox = Float.MAX_VALUE

    /**      Move    // Best candidate center distance to current NavId */
    var distCenter = Float.MAX_VALUE

    /**      Move    // Best candidate axial distance to current NavId */
    var distAxial = Float.MAX_VALUE

    fun clear() {
        id = 0
        window = null
        distBox = Float.MAX_VALUE
        distCenter = Float.MAX_VALUE
        distAxial = Float.MAX_VALUE
        rectRel = Rect()
    }
}