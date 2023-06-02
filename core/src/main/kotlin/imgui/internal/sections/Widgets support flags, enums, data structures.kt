package imgui.internal.sections

import com.livefront.sealedenum.GenSealedEnum
import glm_.vec2.Vec2
import imgui.*
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.sections.ButtonFlag.*


//-----------------------------------------------------------------------------
// [SECTION] Widgets support: flags, enums, data structures
//-----------------------------------------------------------------------------

// Flags used by upcoming items
// - input: PushItemFlag() manipulates g.CurrentItemFlags, ItemAdd() calls may add extra flags.
// - output: stored in g.LastItemData.InFlags
// Current window shared by all windows.
/** Flags: for PushItemFlag(), g.LastItemData.InFlags */
typealias ItemFlags = Flag<ItemFlag>

/** Transient per-window flags, reset at the beginning of the frame. For child window, inherited from parent on first Begin().
 *  This is going to be exposed in imgui.h when stabilized enough. */
sealed class ItemFlag(override val i: Int) : FlagBase<ItemFlag>() {
    // Controlled by user
    /** Disable keyboard tabbing (FIXME: should merge with _NoNav) */
    object NoTabStop : ItemFlag(1 shl 0)  // false

    /** Button() will return true multiple times based on io.KeyRepeatDelay and io.KeyRepeatRate settings. */
    object ButtonRepeat : ItemFlag(1 shl 1)  // false

    /** Disable interactions but doesn't affect visuals. See BeginDisabled()/EndDisabled(). See github.com/ocornut/imgui/issues/211 */
    object Disabled : ItemFlag(1 shl 2)  // false

    /** Disable keyboard/gamepad directional navigation (FIXME: should merge with _NoTabStop) */
    object NoNav : ItemFlag(1 shl 3)  // false

    /** Disable item being a candidate for default focus (e.g. used by title bar items) */
    object NoNavDefaultFocus : ItemFlag(1 shl 4)  // false

    /** Disable MenuItem/Selectable() automatically closing their popup window */
    object SelectableDontClosePopup : ItemFlag(1 shl 5)  // false

    /** [BETA] Represent a mixed/indeterminate value, generally multi-selection where values differ. Currently only supported by Checkbox() (later should support all sorts of widgets) */
    object MixedValue : ItemFlag(1 shl 6)  // false

    /** [ALPHA] Allow hovering interactions but underlying value is not changed. */
    object ReadOnly : ItemFlag(1 shl 7)  // false

    /** Disable hoverable check in ItemHoverable() */
    object NoWindowHoverableCheck : ItemFlag(1 shl 8)  // false


    // Controlled by widget code

    /** [WIP] Auto-activate input mode when tab focused. Currently only used and supported by a few items before it becomes a generic feature. */
    object Inputable : ItemFlag(1 shl 10)   // false

    @GenSealedEnum
    companion object
}


/** Flags: for g.LastItemData.StatusFlags */
typealias ItemStatusFlags = Flag<ItemStatusFlag>

/** Status flags for an already submitted item
 *  - output: stored in g.LastItemData.StatusFlags   */
sealed class ItemStatusFlag(override val i: Int) : FlagBase<ItemStatusFlag>() {
    /** Mouse position is within item rectangle (does NOT mean that the window is in correct z-order and can be hovered!, this is only one part of the most-common IsItemHovered test) */
    object HoveredRect : ItemStatusFlag(1 shl 0)

    /** g.LastItemData.DisplayRect is valid */
    object HasDisplayRect : ItemStatusFlag(1 shl 1)

    /** Value exposed by item was edited in the current frame (should match the bool return value of most widgets) */
    object Edited : ItemStatusFlag(1 shl 2)

    /** Set when Selectable(), TreeNode() reports toggling a selection. We can't report "Selected", only state changes, in order to easily handle clipping with less issues. */
    object ToggledSelection : ItemStatusFlag(1 shl 3)

    /** Set when TreeNode() reports toggling their open state. */
    object ToggledOpen : ItemStatusFlag(1 shl 4)

    /** Set if the widget/group is able to provide data for the ImGuiItemStatusFlags_Deactivated flag. */
    object HasDeactivated : ItemStatusFlag(1 shl 5)

    /** Only valid if ImGuiItemStatusFlags_HasDeactivated is set. */
    object Deactivated : ItemStatusFlag(1 shl 6)

    /** Override the HoveredWindow test to allow cross-window hover testing. */
    object HoveredWindow : ItemStatusFlag(1 shl 7)

    /** Set when the Focusable item just got focused by Tabbing (FIXME: to be removed soon) */
    object FocusedByTabbing : ItemStatusFlag(1 shl 8)

    /** [WIP] Set when item is overlapping the current clipping rectangle (Used internally. Please don't use yet: API/system will change as we refactor Itemadd()). */
    object Visible : ItemStatusFlag(1 shl 9)

    // Additional status + semantic for ImGuiTestEngine
    //  #ifdef IMGUI_ENABLE_TEST_ENGINE
    //  [imgui-test only]

    /** Item is an openable (e.g. TreeNode) */
    object Openable : ItemStatusFlag(1 shl 20)

    /** Opened status */
    object Opened : ItemStatusFlag(1 shl 21)

    /** Item is a checkable (e.g. CheckBox, MenuItem) */
    object Checkable : ItemStatusFlag(1 shl 22)

    /** Checked status */
    object Checked : ItemStatusFlag(1 shl 23)

    /** Item is a text-inputable (e.g. InputText, SliderXXX, DragXXX) */
    object Inputable : ItemStatusFlag(1 shl 24)

    @GenSealedEnum
    companion object
}

typealias ButtonFlags = Flag<ButtonFlag>

sealed class ButtonFlag(override val i: Int) : FlagBase<ButtonFlag>() {
    /** React on left mouse button (default) */
    object MouseButtonLeft : ButtonFlag(1 shl 0)

    /** React on right mouse button */
    object MouseButtonRight : ButtonFlag(1 shl 1)

    /** React on center mouse button */
    object MouseButtonMiddle : ButtonFlag(1 shl 2)

    /** return true on click (mouse down event) */
    object PressedOnClick : ButtonFlag(1 shl 4)

    /** [Default] return true on click + release on same item <-- this is what the majority of Button are using */
    object PressedOnClickRelease : ButtonFlag(1 shl 5)

    /** return true on click + release even if the release event is not done while hovering the item */
    object PressedOnClickReleaseAnywhere : ButtonFlag(1 shl 6)

    /** return true on release (default requires click+release) */
    object PressedOnRelease : ButtonFlag(1 shl 7)

    /** return true on double-click (default requires click+release) */
    object PressedOnDoubleClick : ButtonFlag(1 shl 8)

    /** return true when held into while we are drag and dropping another item (used by e.g. tree nodes, collapsing headers) */
    object PressedOnDragDropHold : ButtonFlag(1 shl 9)

    /** hold to repeat  */
    object Repeat : ButtonFlag(1 shl 10)

    /** allow interactions even if a child window is overlapping */
    object FlattenChildren : ButtonFlag(1 shl 11)

    /** require previous frame HoveredId to either match id or be null before being usable, use along with SetItemAllowOverlap() */
    object AllowItemOverlap : ButtonFlag(1 shl 12)

    /** disable automatically closing parent popup on press // [UNUSED] */
    object DontClosePopups : ButtonFlag(1 shl 13)

    /** disable interactions -> use BeginDisabled() or ImGuiItemFlags_Disabled */
    //    Disabled(1 shl 14),

    /** vertically align button to match text baseline - ButtonEx() only // FIXME: Should be removed and handled by SmallButton(), not possible currently because of DC.CursorPosPrevLine */
    object AlignTextBaseLine : ButtonFlag(1 shl 15)

    /** disable mouse interaction if a key modifier is held */
    object NoKeyModifiers : ButtonFlag(1 shl 16)

    /** don't set ActiveId while holding the mouse (ImGuiButtonFlags_PressedOnClick only) */
    object NoHoldingActiveId : ButtonFlag(1 shl 17)

    /** don't override navigation focus when activated (FIXME: this is essentially used everytime an item uses ImGuiItemFlags_NoNav, but because legacy specs don't requires LastItemData to be set ButtonBehavior(), we can't poll g.LastItemData.InFlags) */
    object NoNavFocus : ButtonFlag(1 shl 18)

    /** don't report as hovered when nav focus is on this item */
    object NoHoveredOnFocus : ButtonFlag(1 shl 19)

    /** don't set key/input owner on the initial click (note: mouse buttons are keys! often, the key in question will be ImGuiKey_MouseLeft!) */
    object NoSetKeyOwner : ButtonFlag(1 shl 20)

    /** don't test key/input owner when polling the key (note: mouse buttons are keys! often, the key in question will be ImGuiKey_MouseLeft!) */
    object NoTestKeyOwner : ButtonFlag(1 shl 21)

    @GenSealedEnum
    companion object {
        val MouseButtonMask: ButtonFlags get() = MouseButtonLeft or MouseButtonRight or MouseButtonMiddle
        val PressedOnMask: ButtonFlags
            get() =
                PressedOnClick or PressedOnClickRelease or PressedOnClickReleaseAnywhere or PressedOnRelease or PressedOnDoubleClick or PressedOnDragDropHold
        val MouseButtonDefault get() = MouseButtonLeft
        val PressedOnDefault get() = PressedOnClickRelease
    }
}

val MouseButton.buttonFlags: ButtonFlags
    get() = when (this) {
        MouseButton.Left -> MouseButtonLeft
        MouseButton.Right -> MouseButtonRight
        MouseButton.Middle -> MouseButtonMiddle
        else -> none
    }

typealias SeparatorFlags = Flag<SeparatorFlag>

sealed class SeparatorFlag : FlagBase<SeparatorFlag>() {

    /** Axis default to current layout type, so generally Horizontal unless e.g. in a menu bar  */
    object Horizontal : SeparatorFlag()
    object Vertical : SeparatorFlag()
    object SpanAllColumns : SeparatorFlag()

    override val i: Int = 1 shl ordinal

    @GenSealedEnum
    companion object
}

typealias TextFlags = Flag<TextFlag>

sealed class TextFlag : FlagBase<TextFlag>() {
    object NoWidthForLargeClippedText : TextFlag()

    override val i: Int = 1 shl ordinal

    @GenSealedEnum
    companion object
}

typealias TooltipFlags = Flag<TooltipFlag>

sealed class TooltipFlag : FlagBase<TooltipFlag>() {
    /** Override will clear/ignore previously submitted tooltip (defaults to append) */
    object OverridePreviousTooltip : TooltipFlag()

    override val i: Int = 1 shl ordinal

    @GenSealedEnum
    companion object
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
infix fun <T> Array<T>.mutablePropertyAt(index: Axis) = mutablePropertyAt(index.i)
operator fun <T> Array<T>.set(index: Axis, value: T) = set(index.i, value)

operator fun Vec2.get(axis: Axis): Float = when (axis) {
    Axis.X -> x
    Axis.Y -> y
    else -> throw Error()
}

infix fun Vec2.mutablePropertyAt(index: Axis) = mutablePropertyAt(index.i)

operator fun Vec2.set(axis: Axis, float: Float) = when (axis) {
    Axis.X -> x = float
    Axis.Y -> y = float
    else -> throw Error()
}

infix fun Int.shl(b: Axis) = shl(b.ordinal - 1)


enum class PlotType { Lines, Histogram }

enum class PopupPositionPolicy { Default, ComboBox, Tooltip }


typealias DrawFlags = Flag<DrawFlag>

/** Flags for ImDrawList functions
 *  (Legacy: bit 0 must always correspond to ImDrawFlags_Closed to be backward compatible with old API using a bool. Bits 1..3 must be unused) */
sealed class DrawFlag(override val i: Int) : FlagBase<DrawFlag>() {
    /** PathStroke(), AddPolyline(): specify that shape should be closed (Important: this is always == 1 for legacy reason) */
    object Closed : DrawFlag(1 shl 0)

    // (bits 1..3 unused to facilitate handling of legacy behavior and detection of Flags = 0x0F)

    /** AddRect(), AddRectFilled(), PathRect(): enable rounding top-left corner only (when rounding > 0.0f, we default to all corners). Was 0x01. */
    object RoundCornersTopLeft : DrawFlag(1 shl 4)

    /** AddRect(), AddRectFilled(), PathRect(): enable rounding top-right corner only (when rounding > 0.0f, we default to all corners). Was 0x02. */
    object RoundCornersTopRight : DrawFlag(1 shl 5)

    /** AddRect(), AddRectFilled(), PathRect(): enable rounding bottom-left corner only (when rounding > 0.0f, we default to all corners). Was 0x04. */
    object RoundCornersBottomLeft : DrawFlag(1 shl 6)

    /** AddRect(), AddRectFilled(), PathRect(): enable rounding bottom-right corner only (when rounding > 0.0f, we default to all corners). Wax 0x08. */
    object RoundCornersBottomRight : DrawFlag(1 shl 7)

    /** AddRect(), AddRectFilled(), PathRect(): disable rounding on all corners (when rounding > 0.0f). This is NOT zero, NOT an implicit flag! */
    object RoundCornersNone : DrawFlag(1 shl 8)

    @GenSealedEnum
    companion object {
        val RoundCornersAll: DrawFlags
            get() =
                RoundCornersTopLeft or RoundCornersTopRight or RoundCornersBottomLeft or RoundCornersBottomRight

        /** Default to ALL corners if none of the _RoundCornersXX flags are specified. */
        val RoundCornersDefault: DrawFlags get() = RoundCornersAll
        val RoundCornersMask: DrawFlags get() = RoundCornersAll or RoundCornersNone
        val RoundCornersTop: DrawFlags get() = RoundCornersTopLeft or RoundCornersTopRight
        val RoundCornersBottom: DrawFlags get() = RoundCornersBottomLeft or RoundCornersBottomRight
        val RoundCornersLeft: DrawFlags get() = RoundCornersBottomLeft or RoundCornersTopLeft
        val RoundCornersRight: DrawFlags get() = RoundCornersBottomRight or RoundCornersTopRight
    }
}

typealias DrawListFlags = Flag<DrawListFlag>

/** Flags: for ImDrawList instance. Those are set automatically by ImGui:: functions from ImGuiIO settings, and generally not
 *  manipulated directly. It is however possible to temporarily alter flags between calls to ImDrawList:: functions. */
sealed class DrawListFlag : FlagBase<DrawListFlag>() {

    /** Enable anti-aliased lines/borders (*2 the number of triangles for 1.0f wide line or lines thin enough to be
     *  drawn using textures, otherwise *3 the number of triangles) */
    object AntiAliasedLines : DrawListFlag()

    /** Enable anti-aliased lines/borders using textures when possible. Require backend to render with bilinear filtering (NOT point/nearest filtering). */
    object AntiAliasedLinesUseTex : DrawListFlag()

    /** Enable anti-aliased edge around filled shapes (rounded rectangles, circles). */
    object AntiAliasedFill : DrawListFlag()

    /** Can emit 'VtxOffset > 0' to allow large meshes. Set when 'ImGuiBackendFlags_RendererHasVtxOffset' is enabled. */
    object AllowVtxOffset : DrawListFlag()

    override val i: Int = 1 shl ordinal

    @GenSealedEnum
    companion object
}

typealias NextWindowDataFlags = Flag<NextWindowDataFlag>

sealed class NextWindowDataFlag : FlagBase<NextWindowDataFlag>() {

    object HasPos : NextWindowDataFlag()
    object HasSize : NextWindowDataFlag()
    object HasContentSize : NextWindowDataFlag()
    object HasCollapsed : NextWindowDataFlag()
    object HasSizeConstraint : NextWindowDataFlag()
    object HasFocus : NextWindowDataFlag()
    object HasBgAlpha : NextWindowDataFlag()
    object HasScroll : NextWindowDataFlag()

    override val i: Int = 1 shl ordinal

    @GenSealedEnum
    companion object
}

typealias NextItemDataFlags = Flag<NextItemDataFlag>

sealed class NextItemDataFlag : FlagBase<NextItemDataFlag>() {
    object HasWidth : NextItemDataFlag()
    object HasOpen : NextItemDataFlag()

    override val i: Int = 1 shl ordinal

    @GenSealedEnum
    companion object
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
    var inFlags: ItemFlags = none

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