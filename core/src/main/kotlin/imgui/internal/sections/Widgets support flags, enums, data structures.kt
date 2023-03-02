package imgui.internal.sections

import glm_.vec2.Vec2
import imgui.Flag
import imgui.ID
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.or


//-----------------------------------------------------------------------------
// [SECTION] Widgets support: flags, enums, data structures
//-----------------------------------------------------------------------------

// Flags used by upcoming items
// - input: PushItemFlag() manipulates g.CurrentItemFlags, ItemAdd() calls may add extra flags.
// - output: stored in g.LastItemData.InFlags
// Current window shared by all windows.
/** Flags: for PushItemFlag(), g.LastItemData.InFlags */
typealias ItemFlags = Int

/** Transient per-window flags, reset at the beginning of the frame. For child window, inherited from parent on first Begin().
 *  This is going to be exposed in imgui.h when stabilized enough. */
enum class ItemFlag(override val i: ItemFlags) : Flag<ItemFlag> {


  // Controlled by user

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

    /** Disable hoverable check in ItemHoverable() */
    NoWindowHoverableCheck(1 shl 8),  // false


    // Controlled by widget code

    /** [WIP] Auto-activate input mode when tab focused. Currently only used and supported by a few items before it becomes a generic feature. */
    Inputable(1 shl 10);   // false
}


/** Flags: for g.LastItemData.StatusFlags */
typealias ItemStatusFlags = Int

/** Status flags for an already submitted item
 *  - output: stored in g.LastItemData.StatusFlags   */
enum class ItemStatusFlag(override val i: ItemStatusFlags) : Flag<ItemStatusFlag> {
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

    /** [WIP] Set when item is overlapping the current clipping rectangle (Used internally. Please don't use yet: API/system will change as we refactor Itemadd()). */
    Visible(1 shl 9),

    //  #ifdef IMGUI_ENABLE_TEST_ENGINE
    //  [imgui-test only]

    /** Item is an openable (e.g. TreeNode) */
    Openable(1 shl 20),
    Opened(1 shl 21),

  /** Item is a checkable (e.g. CheckBox, MenuItem) */
  Checkable(1 shl 22),
  Checked(1 shl 23)
}

typealias ButtonFlags = Int

enum class ButtonFlag(override val i: ButtonFlags) : Flag<ButtonFlag> {

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

    /** don't override navigation focus when activated (FIXME: this is essentially used everytime an item uses ImGuiItemFlags_NoNav, but because legacy specs don't requires LastItemData to be set ButtonBehavior(), we can't poll g.LastItemData.InFlags) */
    NoNavFocus(1 shl 18),

    /** don't report as hovered when nav focus is on this item */
    NoHoveredOnFocus(1 shl 19),

    /** don't set key/input owner on the initial click (note: mouse buttons are keys! often, the key in question will be ImGuiKey_MouseLeft!) */
    NoSetKeyOwner(1 shl 20),

    /** don't test key/input owner when polling the key (note: mouse buttons are keys! often, the key in question will be ImGuiKey_MouseLeft!) */
    NoTestKeyOwner(1 shl 21),

    MouseButtonMask_(MouseButtonLeft or MouseButtonRight or MouseButtonMiddle),
    MouseButtonShift_(16),
    MouseButtonDefault_(MouseButtonLeft.i),
  PressedOnMask_(PressedOnClick or PressedOnClickRelease or PressedOnClickReleaseAnywhere or PressedOnRelease or PressedOnDoubleClick or PressedOnDragDropHold),
  PressedOnDefault_(PressedOnClickRelease.i)
}

typealias SeparatorFlags = Int

enum class SeparatorFlag : Flag<SeparatorFlag> {
  None,

  /** Axis default to current layout type, so generally Horizontal unless e.g. in a menu bar  */
  Horizontal,
  Vertical,
  SpanAllColumns;

  override val i: SeparatorFlags = if (ordinal == 0) 0 else 1 shl (ordinal - 1)
}

typealias TextFlags = Int

enum class TextFlag : Flag<TextFlag> {
  None, NoWidthForLargeClippedText;

  override val i: TextFlags = ordinal
}

typealias TooltipFlags = Int

enum class TooltipFlag(override val i: TooltipFlags) : Flag<TooltipFlag> {
  None(0),

  /** Override will clear/ignore previously submitted tooltip (defaults to append) */
  OverridePreviousTooltip(1 shl 0)
}

/** FIXME: this is in development, not exposed/functional as a generic feature yet.
 *  Horizontal/Vertical enums are fixed to 0/1 so they may be used to index ImVec2 */
enum class LayoutType { Horizontal, Vertical }


enum class LogType { None, TTY, File, Buffer, Clipboard }


/** X/Y enums are fixed to 0/1 so they may be used to index ImVec2 */
enum class Axis {
    None, X, Y;

    val i = ordinal - 1
    infix fun xor(i: Int) = this.i xor i

    companion object {
        infix fun of(int: Int) = values().first { it.i == int }
    }
}

operator fun <T> Array<T>.get(index: Axis) = get(index.i)
operator fun <T> Array<T>.set(index: Axis, value: T) = set(index.i, value)

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
enum class DrawFlag(override val i: DrawFlags) : Flag<DrawFlag> {
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
  RoundCornersMask_(RoundCornersAll or RoundCornersNone)
}

typealias DrawListFlags = Int

/** Flags: for ImDrawList instance. Those are set automatically by ImGui:: functions from ImGuiIO settings, and generally not
 *  manipulated directly. It is however possible to temporarily alter flags between calls to ImDrawList:: functions. */
enum class DrawListFlag(override val i: DrawListFlags) : Flag<DrawListFlag> {
  None(0),

  /** Enable anti-aliased lines/borders (*2 the number of triangles for 1.0f wide line or lines thin enough to be
   *  drawn using textures, otherwise *3 the number of triangles) */
  AntiAliasedLines(1 shl 0),

  /** Enable anti-aliased lines/borders using textures when possible. Require backend to render with bilinear filtering (NOT point/nearest filtering). */
  AntiAliasedLinesUseTex(1 shl 1),

  /** Enable anti-aliased edge around filled shapes (rounded rectangles, circles). */
    AntiAliasedFill(1 shl 2),

  /** Can emit 'VtxOffset > 0' to allow large meshes. Set when 'ImGuiBackendFlags_RendererHasVtxOffset' is enabled. */
  AllowVtxOffset(1 shl 3)
}

typealias NextWindowDataFlags = Int

enum class NextWindowDataFlag : Flag<NextWindowDataFlag> {
  None, HasPos, HasSize, HasContentSize, HasCollapsed, HasSizeConstraint, HasFocus, HasBgAlpha, HasScroll;

  override val i: NextWindowDataFlags = if (ordinal == 0) 0 else 1 shl (ordinal - 1)
}

typealias NextItemDataFlags = Int

enum class NextItemDataFlag(override val i: NextItemDataFlags) : Flag<NextItemDataFlag> {
  None(0),
  HasWidth(1 shl 0),
  HasOpen(1 shl 1)
}

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