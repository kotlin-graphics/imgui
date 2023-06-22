package imgui

import com.livefront.sealedenum.GenSealedEnum
import glm_.vec4.Vec4
import imgui.DragDropFlag.*
import imgui.ImGui.getColorU32
import imgui.internal.isPowerOfTwo
import unsigned.*
import kotlin.internal.NoInfer


//-----------------------------------------------------------------------------
// [SECTION] Flags & Enumerations
//-----------------------------------------------------------------------------

@JvmInline
private value class Flags<F : Flag<F>>(override val i: Int) : Flag<F> {
    constructor() : this(0)
    constructor(flag: Flag<F>) : this(flag.i)

    @Suppress("RESERVED_MEMBER_INSIDE_VALUE_CLASS")
    override fun equals(other: Any?): Boolean = other is Flag<*> && i == other.i

    @Suppress("RESERVED_MEMBER_INSIDE_VALUE_CLASS")
    override fun hashCode(): Int = i

    infix fun and(b: Flags<F>): Flags<F> = Flags(i and b.i)
    infix fun or(b: Flags<F>): Flags<F> = Flags(i or b.i)
    infix fun xor(b: Flags<F>): Flags<F> = Flags(i xor b.i)
    infix fun wo(b: Flags<F>): Flags<F> = Flags(i and b.i.inv())
}

@JvmInline
value class FlagArray<F : Flag<F>> private constructor(private val array: IntArray) {
    constructor(flag: Flag<F>) : this(IntArray(1) {
        flag.i
    })

    constructor(flags: Array<out Flag<F>>) : this(IntArray(flags.size) {
        flags[it].i
    })

    // constructor from size
    constructor(size: Int) : this(IntArray(size))

    // Provides Java-style constructors
    companion object {
        @JvmName("of")
        @JvmStatic
        fun <F : Flag<F>> of(flag: Flag<F>): FlagArray<F> = FlagArray(flag)

        @JvmName("of")
        @JvmStatic
        fun <F : Flag<F>> of(vararg flags: Flag<F>): FlagArray<F> = FlagArray(flags)

        @JvmName("of")
        @JvmStatic
        fun <F : Flag<F>> of(size: Int): FlagArray<F> = FlagArray(size)

        @JvmName("get")
        @JvmStatic
        fun <F : Flag<F>> get(flagArray: FlagArray<F>, index: Int): Flag<F> = flagArray[index]

        @JvmName("set")
        @JvmStatic
        fun <F : Flag<F>> set(flagArray: FlagArray<F>, index: Int, value: Flag<F>) {
            flagArray[index] = value
        }

        @JvmName("iterator")
        @JvmStatic
        fun <F : Flag<F>> iterator(flagArray: FlagArray<F>): Iterator<Flag<F>> = flagArray.iterator()
    }

    operator fun get(index: Int): Flag<F> = Flags(array[index])
    operator fun set(index: Int, value: Flag<F>) {
        array[index] = value.i
    }

    val size: Int get() = array.size
    operator fun iterator(): Iterator<Flag<F>> = array.iterator().asSequence().map { Flags<F>(it) }.iterator()

    /**
     * Returns the range of valid indices for the array.
     */
    val indices: IntRange
        get() = IntRange(0, size - 1)
}

/**
 * Creates a new array of the specified [size], where each element is calculated by calling the specified
 * [init] function.
 *
 * The function [init] is called for each array element sequentially starting from the first one.
 * It should return the value for an array element given its index.
 */
inline fun <F : Flag<F>> FlagArray(size: Int, init: (Int) -> Flag<F>) = FlagArray<F>(size).apply {
    for (i in indices)
        this[i] = init(i)
}

val none: Flag<Nothing> = Flags()
fun <F : Flag<F>> emptyFlags(): Flag<F> = none
fun <F : Flag<F>> flagArrayOf(flag: Flag<F>): FlagArray<F> = FlagArray(flag)
fun <F : Flag<F>> flagArrayOf(vararg flags: Flag<F>): FlagArray<F> = FlagArray(flags)

interface Flag<out Self : Flag<Self>> {
    val i: Int

    companion object {
        @JvmStatic
        fun <F : Flag<F>> none(): Flag<F> = none
    }

    val isEmpty get() = this == none
    val isNotEmpty get() = !isEmpty
    val isPowerOfTwo get() = i.isPowerOfTwo
}

infix fun <Self : Flag<Self>> Flag<Self>.and(b: Flag<Self>): Flag<Self> = Flags(this) and Flags(b)
infix fun <Self : Flag<Self>> Flag<Self>.or(b: Flag<Self>): Flag<Self> = Flags(this) or Flags(b)
infix fun <Self : Flag<Self>> Flag<Self>.xor(b: Flag<Self>): Flag<Self> = Flags(this) xor Flags(b)
infix fun <Self : Flag<Self>> Flag<Self>.wo(b: Flag<Self>): Flag<Self> = Flags(this) wo Flags(b)
infix fun <Self : Flag<Self>> Flag<Self>.has(b: Flag<@NoInfer Self>): Boolean = and(b).isNotEmpty
infix fun <Self : Flag<Self>> Flag<Self>.hasnt(b: Flag<@NoInfer Self>): Boolean = and(b).isEmpty
operator fun <Self : Flag<Self>> Flag<Self>.minus(flag: Flag<Self>): Flag<Self> = wo(flag)
operator fun <Self : Flag<Self>> Flag<Self>.div(flag: Flag<Self>): Flag<Self> = or(flag)
operator fun <Self : Flag<Self>> Flag<Self>.contains(flag: Flag<@NoInfer Self>) = and(flag) == flag

abstract class FlagBase<out Self : Flag<Self>> : Flag<Self> {
    override fun equals(other: Any?): Boolean = this === other || (other is Flag<*> && i == other.i)
    override fun hashCode(): Int = i
}

/** Flags for ImGui::Begin()
 *  (Those are per-window flags. There are shared flags in ImGuiIO: io.ConfigWindowsResizeFromEdges and io.ConfigWindowsMoveFromTitleBarOnly) */
typealias WindowFlags = Flag<WindowFlag>

/** Flags: for Begin(), BeginChild()    */
sealed class WindowFlag(override val i: Int) : FlagBase<WindowFlag>() {
    /** Disable title-bar   */
    object NoTitleBar : WindowFlag(1 shl 0)

    /** Disable user resizing with the lower-right grip */
    object NoResize : WindowFlag(1 shl 1)

    /** Disable user moving the window  */
    object NoMove : WindowFlag(1 shl 2)

    /** Disable scrollbars (window can still scroll with mouse or programmatically)  */
    object NoScrollbar : WindowFlag(1 shl 3)

    /** Disable user vertically scrolling with mouse wheel. On child window, mouse wheel will be forwarded to the parent
     *  unless noScrollbar is also set.  */
    object NoScrollWithMouse : WindowFlag(1 shl 4)

    /** Disable user collapsing window by double-clicking on it. Also referred to as Window Menu Button (e.g. within a docking node). */
    object NoCollapse : WindowFlag(1 shl 5)

    /** Resize every window to its content every frame  */
    object AlwaysAutoResize : WindowFlag(1 shl 6)

    /** Disable drawing background color (WindowBg, etc.) and outside border. Similar as using SetNextWindowBgAlpha(0.0f).(1 shl 7) */
    object NoBackground : WindowFlag(1 shl 7)

    /** Never load/save settings in .ini file   */
    object NoSavedSettings : WindowFlag(1 shl 8)

    /** Disable catching mouse or keyboard inputs   */
    object NoMouseInputs : WindowFlag(1 shl 9)

    /** Has a menu-bar  */
    object MenuBar : WindowFlag(1 shl 10)

    /** Allow horizontal scrollbar to appear (off by default). You may use SetNextWindowContentSize(ImVec2(width),0.0f));
     *  prior to calling Begin() to specify width. Read code in imgui_demo in the "Horizontal Scrolling" section.    */
    object HorizontalScrollbar : WindowFlag(1 shl 11)

    /** Disable taking focus when transitioning from hidden to visible state    */
    object NoFocusOnAppearing : WindowFlag(1 shl 12)

    /** Disable bringing window to front when taking focus (e.g. clicking on it or programmatically giving it focus) */
    object NoBringToFrontOnFocus : WindowFlag(1 shl 13)

    /** Always show vertical scrollbar (even if ContentSize.y < Size.y) */
    object AlwaysVerticalScrollbar : WindowFlag(1 shl 14)

    /** Always show horizontal scrollbar (even if ContentSize.x < Size.x)   */
    object AlwaysHorizontalScrollbar : WindowFlag(1 shl 15)

    /** Ensure child windows without border uses style.WindowPadding (ignored by default for non-bordered child windows),
     *  because more convenient)  */
    object AlwaysUseWindowPadding : WindowFlag(1 shl 16)

    /** No gamepad/keyboard navigation within the window    */
    object NoNavInputs : WindowFlag(1 shl 18)

    /** No focusing toward this window with gamepad/keyboard navigation (e.g. skipped by CTRL+TAB)  */
    object NoNavFocus : WindowFlag(1 shl 19)

    /** Display a dot next to the title. When used in a tab/docking context, tab is selected when clicking the X + closure is not assumed (will wait for user to stop submitting the tab). Otherwise closure is assumed when pressing the X, so if you keep submitting the tab may reappear at end of tab bar. */
    object UnsavedDocument : WindowFlag(1 shl 20)

    // [Internal]

    /** [BETA] On child window: allow gamepad/keyboard navigation to cross over parent border to this child or between sibling child windows. */
    object _NavFlattened : WindowFlag(1 shl 23)

    /** Don't use! For internal use by BeginChild() */
    object _ChildWindow : WindowFlag(1 shl 24)

    /** Don't use! For internal use by BeginTooltip()   */
    object _Tooltip : WindowFlag(1 shl 25)

    /** Don't use! For internal use by BeginPopup() */
    object _Popup : WindowFlag(1 shl 26)

    /** Don't use! For internal use by BeginPopupModal()    */
    object _Modal : WindowFlag(1 shl 27)

    /** Don't use! For internal use by BeginMenu()  */
    object _ChildMenu : WindowFlag(1 shl 28)

    @GenSealedEnum
    companion object {
        val NoNav: WindowFlags get() = NoNavInputs or NoNavFocus

        val NoDecoration: WindowFlags get() = NoTitleBar or NoResize or NoScrollbar or NoCollapse

        val NoInputs: WindowFlags get() = NoMouseInputs or NoNavInputs or NoNavFocus
    }
}

/** Flags for ImGui::InputText(), InputTextMultiline()
 *  (Those are per-item flags. There are shared flags in ImGuiIO: io.ConfigInputTextCursorBlink and io.ConfigInputTextEnterKeepActive) */
typealias InputTextFlags = Flag<InputTextFlag<*>>
typealias InputTextSingleFlags = Flag<InputTextFlag.Single>

sealed class InputTextFlag<out ITF : InputTextFlag<ITF>>(override val i: Int) : FlagBase<ITF>() {
    sealed class Single(i: Int) : InputTextFlag<Single>(i)

    sealed class Multiline(i: Int) : InputTextFlag<Multiline>(i)

    /** Allow 0123456789 . + - * /      */
    object CharsDecimal : Single(1 shl 0)

    /** Allow 0123456789ABCDEFabcdef    */
    object CharsHexadecimal : Single(1 shl 1)

    /** Turn a..z into A..Z */
    object CharsUppercase : Single(1 shl 2)

    /** Filter out spaces), tabs    */
    object CharsNoBlank : Single(1 shl 3)

    /** Select entire text when first taking mouse focus    */
    object AutoSelectAll : Single(1 shl 4)

    /** Return 'true' when Enter is pressed (as opposed to every time the value was modified). Consider looking at the IsItemDeactivatedAfterEdit() function. */
    object EnterReturnsTrue : Single(1 shl 5)

    /** Callback on pressing TAB (for completion handling)    */
    object CallbackCompletion : Single(1 shl 6)

    /** Callback on pressing Up/Down arrows (for history handling)    */
    object CallbackHistory : Single(1 shl 7)

    /** Callback on each iteration. User code may query cursor position), modify text buffer.    */
    object CallbackAlways : Single(1 shl 8)

    /** Callback on character inputs to replace or discard them. Modify 'EventChar' to replace or discard,
     *  or return 1 in callback to discard.  */
    object CallbackCharFilter : Single(1 shl 9)

    /** Pressing TAB input a '\t' character into the text field */
    object AllowTabInput : Single(1 shl 10)

    /** In multi-line mode), unfocus with Enter), add new line with Ctrl+Enter (default is opposite: unfocus with
     *  Ctrl+Enter), add line with Enter).   */
    object CtrlEnterForNewLine : Single(1 shl 11)

    /** Disable following the cursor horizontally   */
    object NoHorizontalScroll : Single(1 shl 12)

    /** Overwrite mode */
    object AlwaysOverwrite : Single(1 shl 13)

    /** Read-only mode  */
    object ReadOnly : Single(1 shl 14)

    /** Password mode), display all characters as '*'   */
    object Password : Single(1 shl 15)

    /** Disable undo/redo. Note that input text owns the text data while active, if you want to provide your own
     *  undo/redo stack you need e.g. to call clearActiveID(). */
    object NoUndoRedo : Single(1 shl 16)

    /** Allow 0123456789.+-* /eE (Scientific notation input) */
    object CharsScientific : Single(1 shl 17)

    /** Callback on buffer capacity changes request (beyond 'buf_size' parameter value), allowing the string to grow.
     *  Notify when the string wants to be resized (for string types which hold a cache of their Size).
     *  You will be provided a new BufSize in the callback and NEED to honor it. (see misc/cpp/imgui_stl.h for an example of using this) */
    object CallbackResize : Single(1 shl 18) // [JVM] be sure to modify the upstream source as well on resize!

    /** Callback on any edit (note that InputText() already returns true on edit, the callback is useful mainly to
     *  manipulate the underlying buffer while focus is active) */
    object CallbackEdit : Single(1 shl 19)

    /** Escape key clears content if not empty, and deactivate otherwise (contrast to default behavior of Escape to revert) */
    object EscapeClearsAll : Single(1 shl 20)

    // [Internal]

    /** For internal use by InputTextMultiline() */
    object _Multiline : Multiline(1 shl 26)

    /** For internal use by functions using InputText() before reformatting data */
    object _NoMarkEdited : Single(1 shl 27)

    /** For internal use by TempInputText(), will skip calling ItemAdd(). Require bounding-box to strictly match. */
    object _MergedItem : Single(1 shl 28)

    @GenSealedEnum
    companion object
}


typealias TreeNodeFlags = Flag<TreeNodeFlag>

/** Flags: for TreeNode(), TreeNodeEx(), CollapsingHeader()   */
sealed class TreeNodeFlag(override val i: Int) : FlagBase<TreeNodeFlag>() {
    /** Draw as selected    */
    object Selected : TreeNodeFlag(1 shl 0)

    /** Draw frame with background (e.g. for CollapsingHeader)  */
    object Framed : TreeNodeFlag(1 shl 1)

    /** Hit testing to allow subsequent widgets to overlap this one */
    object AllowItemOverlap : TreeNodeFlag(1 shl 2)

    /** Don't do a TreePush() when open (e.g. for CollapsingHeader) ( no extra indent nor pushing on ID stack   */
    object NoTreePushOnOpen : TreeNodeFlag(1 shl 3)

    /** Don't automatically and temporarily open node when Logging is active (by default logging will automatically open
     *  tree nodes) */
    object NoAutoOpenOnLog : TreeNodeFlag(1 shl 4)

    /** Default node to be open */
    object DefaultOpen : TreeNodeFlag(1 shl 5)

    /** Need double-click to open node  */
    object OpenOnDoubleClick : TreeNodeFlag(1 shl 6)

    /** Only open when clicking on the arrow part. If OpenOnDoubleClick is also set), single-click arrow or double-click
     *  all box to open.    */
    object OpenOnArrow : TreeNodeFlag(1 shl 7)

    /** No collapsing), no arrow (use as a convenience for leaf nodes). */
    object Leaf : TreeNodeFlag(1 shl 8)

    /** Display a bullet instead of arrow   */
    object Bullet : TreeNodeFlag(1 shl 9)

    /** Use FramePadding (even for an unframed text node) to vertically align text baseline to regular widget height.
     *  Equivalent to calling alignTextToFramePadding().    */
    object FramePadding : TreeNodeFlag(1 shl 10)

    /** Extend hit box to the right-most edge, even if not framed. This is not the default in order to allow adding other items on the same line. In the future we may refactor the hit system to be front-to-back, allowing natural overlaps and then this can become the default. */
    object SpanAvailWidth : TreeNodeFlag(1 shl 11)

    /** Extend hit box to the left-most and right-most edges (bypass the indented area). */
    object SpanFullWidth : TreeNodeFlag(1 shl 12)

    /** (WIP) Nav: left direction may move to this TreeNode() from any of its child (items submitted between TreeNode and TreePop)   */
    object NavLeftJumpsBackHere : TreeNodeFlag(1 shl 13)

    // [Internal]

    object _ClipLabelForTrailingButton : TreeNodeFlag(1 shl 20)

    @GenSealedEnum
    companion object {
        val CollapsingHeader: TreeNodeFlags get() = Framed or NoTreePushOnOpen or NoAutoOpenOnLog
    }
}

typealias PopupFlags = Flag<PopupFlag>

/** Flags for OpenPopup*(), BeginPopupContext*(), IsPopupOpen() functions.
 *  - To be backward compatible with older API which took an 'int mouse_button = 1' argument, we need to treat
 *    small flags values as a mouse button index, so we encode the mouse button in the first few bits of the flags.
 *    It is therefore guaranteed to be legal to pass a mouse button index in ImGuiPopupFlags.
 *  - For the same reason, we exceptionally default the ImGuiPopupFlags argument of BeginPopupContextXXX functions to 1 instead of 0.
 *    IMPORTANT: because the default parameter is 1 (==ImGuiPopupFlags_MouseButtonRight), if you rely on the default parameter
 *    and want to use another flag, you need to pass in the ImGuiPopupFlags_MouseButtonRight flag explicitly.
 *  - Multiple buttons currently cannot be combined/or-ed in those functions (we could allow it later). */
sealed class PopupFlag(override val i: Int) : FlagBase<PopupFlag>() {
    /** For BeginPopupContext*(): open on Left Mouse release. */
    object MouseButtonLeft : PopupFlag(0)

    /** For BeginPopupContext*(): open on Right Mouse release. */
    object MouseButtonRight : PopupFlag(1)

    /** For BeginPopupContext*(): open on Middle Mouse release. */
    object MouseButtonMiddle : PopupFlag(2)

    /** For OpenPopup*(), BeginPopupContext*(): don't open if there's already a popup at the same level of the popup stack */
    object NoOpenOverExistingPopup : PopupFlag(1 shl 5)

    /** For BeginPopupContextWindow(): don't return true when hovering items, only when hovering empty space */
    object NoOpenOverItems : PopupFlag(1 shl 6)

    /** For IsPopupOpen(): ignore the ImGuiID parameter and test for any popup. */
    object AnyPopupId : PopupFlag(1 shl 7)

    /** For IsPopupOpen(): search/test at any level of the popup stack (default test in the current level) */
    object AnyPopupLevel : PopupFlag(1 shl 8)

    @GenSealedEnum
    companion object {
        val AnyPopup: PopupFlags get() = AnyPopupId or AnyPopupLevel
        val Default: PopupFlags get() = MouseButtonRight

        infix fun of(mouseButton: MouseButton): PopupFlags = when (mouseButton) {
            MouseButton.Left -> MouseButtonLeft
            MouseButton.Right -> MouseButtonRight
            MouseButton.Middle -> MouseButtonMiddle
            else -> none
        }
    }
}

val PopupFlags.mouseButton: MouseButton
    get() = when (this) {
        PopupFlag.MouseButtonLeft -> MouseButton.Left
        PopupFlag.MouseButtonRight -> MouseButton.Right
        PopupFlag.MouseButtonMiddle -> MouseButton.Middle
        else -> MouseButton.None
    }

typealias SelectableFlags = Flag<SelectableFlag>

/** Flags for ImGui::Selectable()   */
sealed class SelectableFlag(override val i: Int) : FlagBase<SelectableFlag>() {
    /** Clicking this doesn't close parent popup window   */
    object DontClosePopups : SelectableFlag(1 shl 0)

    /** Selectable frame can span all columns (text will still fit in current column)   */
    object SpanAllColumns : SelectableFlag(1 shl 1)

    /** Generate press events on double clicks too  */
    object AllowDoubleClick : SelectableFlag(1 shl 2)

    /** Cannot be selected, display grayed out text */
    object Disabled : SelectableFlag(1 shl 3)

    /** (WIP) Hit testing to allow subsequent widgets to overlap this one */
    object AllowItemOverlap : SelectableFlag(1 shl 4)

    // [Internal] NB: need to be in sync with last value of ImGuiSelectableFlags_

    /** private  */
    object _NoHoldingActiveID : SelectableFlag(1 shl 20)

    /** (WIP) Auto-select when moved into. This is not exposed in public API as to handle multi-select and modifiers we will need user to explicitly control focus scope. May be replaced with a BeginSelection() API. */
    object _SelectOnNav : SelectableFlag(1 shl 21)

    /** Override button behavior to react on Click (default is Click+Release) */
    object _SelectOnClick : SelectableFlag(1 shl 22)

    /** Override button behavior to react on Release (default is Click+Release) */
    object _SelectOnRelease : SelectableFlag(1 shl 23)

    /** Span all avail width even if we declared less for layout purpose. FIXME: We may be able to remove this (added in 6251d379, 2bcafc86 for menus)  */
    object _SpanAvailWidth : SelectableFlag(1 shl 24)

    /** Set Nav/Focus ID on mouse hover (used by MenuItem) */
    object _SetNavIdOnHover : SelectableFlag(1 shl 25)

    /** Disable padding each side with ItemSpacing * 0.5f */
    object _NoPadWithHalfSpacing : SelectableFlag(1 shl 26)

    /** Don't set key/input owner on the initial click (note: mouse buttons are keys! often, the key in question will be ImGuiKey_MouseLeft!) */
    object _NoSetKeyOwner : SelectableFlag(1 shl 27)

    @GenSealedEnum
    companion object
}

typealias ComboFlags = Flag<ComboFlag>

/** Flags: for BeginCombo() */
sealed class ComboFlag(override val i: Int) : FlagBase<ComboFlag>() {
    /** Align the popup toward the left by default */
    object PopupAlignLeft : ComboFlag(1 shl 0)

    /** Max ~4 items visible */
    object HeightSmall : ComboFlag(1 shl 1)

    /** Max ~8 items visible (default) */
    object HeightRegular : ComboFlag(1 shl 2)

    /** Max ~20 items visible */
    object HeightLarge : ComboFlag(1 shl 3)

    /** As many fitting items as possible */
    object HeightLargest : ComboFlag(1 shl 4)

    /** Display on the preview box without the square arrow button  */
    object NoArrowButton : ComboFlag(1 shl 5)

    /** Display only a square arrow button  */
    object NoPreview : ComboFlag(1 shl 6)

    // private

    /** enable BeginComboPreview() */
    object _CustomPreview : ComboFlag(1 shl 20)

    @GenSealedEnum
    companion object {
        val HeightMask: ComboFlags get() = HeightSmall or HeightRegular or HeightLarge or HeightLargest
    }
}


typealias TabBarFlags = Flag<TabBarFlag>

/** Flags for ImGui::BeginTabBar() */
sealed class TabBarFlag(override val i: Int) : FlagBase<TabBarFlag>() {
    /** Allow manually dragging tabs to re-order them + New tabs are appended at the end of list */
    object Reorderable : TabBarFlag(1 shl 0)

    /** Automatically select new tabs when they appear */
    object AutoSelectNewTabs : TabBarFlag(1 shl 1)

    /** Disable buttons to open the tab list popup */
    object TabListPopupButton : TabBarFlag(1 shl 2)

    /** Disable behavior of closing tabs (that are submitted with p_open != NULL) with middle mouse button.
     *  You can still repro this behavior on user's side with if (IsItemHovered() && IsMouseClicked(2)) *p_open = false. */
    object NoCloseWithMiddleMouseButton : TabBarFlag(1 shl 3)

    /** Disable scrolling buttons (apply when fitting policy is ImGuiTabBarFlags_FittingPolicyScroll) */
    object NoTabListScrollingButtons : TabBarFlag(1 shl 4)

    /** Disable tooltips when hovering a tab */
    object NoTooltip : TabBarFlag(1 shl 5)

    /** Resize tabs when they don't fit */
    object FittingPolicyResizeDown : TabBarFlag(1 shl 6)

    /** Add scroll buttons when tabs don't fit */
    object FittingPolicyScroll : TabBarFlag(1 shl 7)

    // Private

    /** Part of a dock node [we don't use this in the master branch but it facilitate branch syncing to keep this around] */
    object _DockNode : TabBarFlag(1 shl 20)

    object _IsFocused : TabBarFlag(1 shl 21)

    /** FIXME: Settings are handled by the docking system, this only request the tab bar to mark settings dirty when reordering tabs, */
    object _SaveSettings : TabBarFlag(1 shl 22)

    @GenSealedEnum
    companion object {
        val FittingPolicyMask: TabBarFlags get() = FittingPolicyResizeDown or FittingPolicyScroll
        val FittingPolicyDefault: TabBarFlags get() = FittingPolicyResizeDown
    }
}

// Represents flags that work for TabButton (which includes TabItem flags)
typealias TabItemFlags = Flag<TabItemFlag<*>>
// Represents flags for _only_ items
typealias TabItemOnlyFlags = Flag<TabItemFlag.ItemOnly>

/** Flags for ImGui::BeginTabItem() */
sealed class TabItemFlag<out TIF : TabItemFlag<TIF>>(override val i: Int) : FlagBase<TIF>() {
    sealed class Button(i: Int) : TabItemFlag<Button>(i)
    sealed class ItemOnly(i: Int) : TabItemFlag<ItemOnly>(i)

    /** Display a dot next to the title + tab is selected when clicking the X + closure is not assumed (will wait for user to stop submitting the tab). Otherwise closure is assumed when pressing the X, so if you keep submitting the tab may reappear at end of tab bar. */
    object UnsavedDocument : ItemOnly(1 shl 0)

    /** Trigger flag to programmatically make the tab selected when calling BeginTabItem() */
    object SetSelected : ItemOnly(1 shl 1)

    /** Disable behavior of closing tabs (that are submitted with p_open != NULL) with middle mouse button. You can still repro this behavior on user's side with if (IsItemHovered() && IsMouseClicked(2)) *p_open = false. */
    object NoCloseWithMiddleMouseButton : ItemOnly(1 shl 2)

    /** Don't call PushID(tab->ID)/PopID() on BeginTabItem()/EndTabItem() */
    object NoPushId : ItemOnly(1 shl 3)

    /** Disable tooltip for the given tab */
    object NoTooltip : ItemOnly(1 shl 4)

    /** Disable reordering this tab or having another tab cross over this tab */
    object NoReorder : ItemOnly(1 shl 5)

    /**  Enforce the tab position to the left of the tab bar (after the tab list popup button) */
    object Leading : ItemOnly(1 shl 6)

    /**  Enforce the tab position to the right of the tab bar (before the scrolling buttons) */
    object Trailing : ItemOnly(1 shl 7)

    // [Internal]
    /** Track whether p_open was set or not (we'll need this info on the next frame to recompute ContentWidth during layout) */
    object _NoCloseButton : ItemOnly(1 shl 20)

    /** Used by TabItemButton, change the tab item behavior to mimic a button */
    object _Button : Button(1 shl 21)

    @GenSealedEnum
    companion object {
        val _SectionMask: TabItemOnlyFlags get() = Leading or Trailing
    }
}

typealias TableFlags = Flag<TableFlag>

// Flags for ImGui::BeginTable()
// - Important! Sizing policies have complex and subtle side effects, much more so than you would expect.
//   Read comments/demos carefully + experiment with live demos to get acquainted with them.
// - The DEFAULT sizing policies are:
//    - Default to ImGuiTableFlags_SizingFixedFit    if ScrollX is on, or if host window has ImGuiWindowFlags_AlwaysAutoResize.
//    - Default to ImGuiTableFlags_SizingStretchSame if ScrollX is off.
// - When ScrollX is off:
//    - Table defaults to ImGuiTableFlags_SizingStretchSame -> all Columns defaults to ImGuiTableColumnFlags_WidthStretch with same weight.
//    - Columns sizing policy allowed: Stretch (default), Fixed/Auto.
//    - Fixed Columns (if any) will generally obtain their requested width (unless the table cannot fit them all).
//    - Stretch Columns will share the remaining width according to their respective weight.
//    - Mixed Fixed/Stretch columns is possible but has various side-effects on resizing behaviors.
//      The typical use of mixing sizing policies is: any number of LEADING Fixed columns, followed by one or two TRAILING Stretch columns.
//      (this is because the visible order of columns have subtle but necessary effects on how they react to manual resizing).
// - When ScrollX is on:
//    - Table defaults to ImGuiTableFlags_SizingFixedFit -> all Columns defaults to ImGuiTableColumnFlags_WidthFixed
//    - Columns sizing policy allowed: Fixed/Auto mostly.
//    - Fixed Columns can be enlarged as needed. Table will show a horizontal scrollbar if needed.
//    - When using auto-resizing (non-resizable) fixed columns, querying the content width to use item right-alignment e.g. SetNextItemWidth(-FLT_MIN) doesn't make sense, would create a feedback loop.
//    - Using Stretch columns OFTEN DOES NOT MAKE SENSE if ScrollX is on, UNLESS you have specified a value for 'inner_width' in BeginTable().
//      If you specify a value for 'inner_width' then effectively the scrolling space is known and Stretch or mixed Fixed/Stretch columns become meaningful again.
// - Read on documentation at the top of imgui_tables.cpp for details.
sealed class TableFlag(override val i: Int) : FlagBase<TableFlag>() {
    /** Enable resizing columns. */
    object Resizable : TableFlag(1 shl 0)

    /** Enable reordering columns in header row (need calling TableSetupColumn() + TableHeadersRow() to display headers) */
    object Reorderable : TableFlag(1 shl 1)

    /** Enable hiding/disabling columns in context menu. */
    object Hideable : TableFlag(1 shl 2)

    /** Enable sorting. Call TableGetSortSpecs() to obtain sort specs. Also see ImGuiTableFlags_SortMulti and ImGuiTableFlags_SortTristate. */
    object Sortable : TableFlag(1 shl 3)

    /** Disable persisting columns order, width and sort settings in the .ini file. */
    object NoSavedSettings : TableFlag(1 shl 4)

    /** Right-click on columns body/contents will display table context menu. By default it is available in TableHeadersRow(). */
    object ContextMenuInBody : TableFlag(1 shl 5)

    // Decorations

    /** Set each RowBg color with ImGuiCol_TableRowBg or ImGuiCol_TableRowBgAlt (equivalent of calling TableSetBgColor with ImGuiTableBgFlags_RowBg0 on each row manually) */
    object RowBg : TableFlag(1 shl 6)

    /** Draw horizontal borders between rows. */
    object BordersInnerH : TableFlag(1 shl 7)

    /** Draw horizontal borders at the top and bottom. */
    object BordersOuterH : TableFlag(1 shl 8)

    /** Draw vertical borders between columns. */
    object BordersInnerV : TableFlag(1 shl 9)

    /** Draw vertical borders on the left and right sides. */
    object BordersOuterV : TableFlag(1 shl 10)

    /** [ALPHA] Disable vertical borders in columns Body (borders will always appear in Headers). -> May move to style */
    object NoBordersInBody : TableFlag(1 shl 11)

    /** [ALPHA] Disable vertical borders in columns Body until hovered for resize (borders will always appear in Headers). -> May move to style */
    object NoBordersInBodyUntilResize : TableFlag(1 shl 12)

    // Sizing Policy (read above for defaults)

    /** Columns default to _WidthFixed or _WidthAuto (if resizable or not resizable), matching contents width. */
    object SizingFixedFit : TableFlag(1 shl 13)

    /** Columns default to _WidthFixed or _WidthAuto (if resizable or not resizable), matching the maximum contents width of all columns. Implicitly enable ImGuiTableFlags_NoKeepColumnsVisible. */
    object SizingFixedSame : TableFlag(2 shl 13)

    /** Columns default to _WidthStretch with default weights proportional to each columns contents widths. */
    object SizingStretchProp : TableFlag(3 shl 13)

    /** Columns default to _WidthStretch with default weights all equal, unless overridden by TableSetupColumn(). */
    object SizingStretchSame : TableFlag(4 shl 13)

    /** Make outer width auto-fit to columns, overriding outer_size.x value. Only available when ScrollX/ScrollY are disabled and Stretch columns are not used. */
    object NoHostExtendX : TableFlag(1 shl 16)

    /** Make outer height stop exactly at outer_size.y (prevent auto-extending table past the limit). Only available when ScrollX/ScrollY are disabled. Data below the limit will be clipped and not visible. */
    object NoHostExtendY : TableFlag(1 shl 17)

    /** Disable keeping column always minimally visible when ScrollX is off and table gets too small. Not recommended if columns are resizable. */
    object NoKeepColumnsVisible : TableFlag(1 shl 18)

    /** Disable distributing remainder width to stretched columns (width allocation on a 100-wide table with 3 columns: Without this flag: 33,33,34. With this flag: 33,33,33). With larger number of columns, resizing will appear to be less smooth. */
    object PreciseWidths : TableFlag(1 shl 19)

    // Clipping

    /** Disable clipping rectangle for every individual columns (reduce draw command count, items will be able to overflow into other columns). Generally incompatible with TableSetupScrollFreeze(). */
    object NoClip : TableFlag(1 shl 20)

    // Padding

    /** Default if BordersOuterV is on. Enable outermost padding. Generally desirable if you have headers. */
    object PadOuterX : TableFlag(1 shl 21)

    /** Default if BordersOuterV is off. Disable outermost padding. */
    object NoPadOuterX : TableFlag(1 shl 22)

    /** Disable inner padding between columns (double inner padding if BordersOuterV is on, single inner padding if BordersOuterV is off). */
    object NoPadInnerX : TableFlag(1 shl 23)

    // Scrolling

    /** Enable horizontal scrolling. Require 'outer_size' parameter of BeginTable() to specify the container size. Changes default sizing policy. Because this creates a child window, ScrollY is currently generally recommended when using ScrollX. */
    object ScrollX : TableFlag(1 shl 24)

    /** Enable vertical scrolling. Require 'outer_size' parameter of BeginTable() to specify the container size. */
    object ScrollY : TableFlag(1 shl 25)

    // Sorting

    /** Hold shift when clicking headers to sort on multiple column. TableGetSortSpecs() may return specs where (SpecsCount > 1). */
    object SortMulti : TableFlag(1 shl 26)

    /** Allow no sorting, disable default sorting. TableGetSortSpecs() may return specs where (SpecsCount == 0). */
    object SortTristate : TableFlag(1 shl 27)

    @GenSealedEnum
    companion object {
        /** Draw horizontal borders. */
        val BordersH: TableFlags get() = BordersInnerH or BordersOuterH

        /** Draw vertical borders. */
        val BordersV: TableFlags get() = BordersInnerV or BordersOuterV

        /** Draw inner borders. */
        val BordersInner: TableFlags get() = BordersInnerV or BordersInnerH

        /** Draw outer borders. */
        val BordersOuter: TableFlags get() = BordersOuterV or BordersOuterH

        /** Draw all borders. */
        val Borders: TableFlags get() = BordersInner or BordersOuter

        /** [Internal] Combinations and masks */
        val _SizingMask: TableFlags get() = SizingFixedFit or SizingFixedSame or SizingStretchProp or SizingStretchSame
    }
}

typealias TableColumnFlags = Flag<TableColumnFlag<*>>
typealias TableColumnSetupFlags = Flag<TableColumnFlag.Setup>

// Flags for ImGui::TableSetupColumn()
sealed class TableColumnFlag<out TCF : TableColumnFlag<TCF>>(override val i: Int) : FlagBase<TCF>() {
    sealed class Status(i: Int) : TableColumnFlag<Status>(i)

    sealed class Setup(i: Int) : TableColumnFlag<Setup>(i)

    /** Overriding/master disable flag: hide column, won't show in context menu (unlike calling TableSetColumnEnabled() which manipulates the user accessible state) */
    object Disabled : Setup(1 shl 0)

    /** Default as a hidden/disabled column. */
    object DefaultHide : Setup(1 shl 1)

    /** Default as a sorting column. */
    object DefaultSort : Setup(1 shl 2)

    /** Column will stretch. Preferable with horizontal scrolling disabled (default if table sizing policy is _SizingStretchSame or _SizingStretchProp). */
    object WidthStretch : Setup(1 shl 3)

    /** Column will not stretch. Preferable with horizontal scrolling enabled (default if table sizing policy is _SizingFixedFit and table is resizable). */
    object WidthFixed : Setup(1 shl 4)

    /** Disable manual resizing. */
    object NoResize : Setup(1 shl 5)

    /** Disable manual reordering this column, this will also prevent other columns from crossing over this column. */
    object NoReorder : Setup(1 shl 6)

    /** Disable ability to hide/disable this column. */
    object NoHide : Setup(1 shl 7)

    /** Disable clipping for this column (all NoClip columns will render in a same draw command). */
    object NoClip : Setup(1 shl 8)

    /** Disable ability to sort on this field (even if ImGuiTableFlags_Sortable is set on the table). */
    object NoSort : Setup(1 shl 9)

    /** Disable ability to sort in the ascending direction. */
    object NoSortAscending : Setup(1 shl 10)

    /** Disable ability to sort in the descending direction. */
    object NoSortDescending : Setup(1 shl 11)

    /** TableHeadersRow() will not submit label for this column. Convenient for some small columns. Name will still appear in context menu. */
    object NoHeaderLabel : Setup(1 shl 12)

    /** Disable header text width contribution to automatic column width. */
    object NoHeaderWidth : Setup(1 shl 13)

    /** Make the initial sort direction Ascending when first sorting on this column (default). */
    object PreferSortAscending : Setup(1 shl 14)

    /** Make the initial sort direction Descending when first sorting on this column. */
    object PreferSortDescending : Setup(1 shl 15)

    /** Use current Indent value when entering cell (default for column 0). */
    object IndentEnable : Setup(1 shl 16)

    /** Ignore current Indent value when entering cell (default for columns > 0). Indentation changes _within_ the cell will still be honored. */
    object IndentDisable : Setup(1 shl 17)

    // Output status flags, read-only via TableGetColumnFlags()

    /** Status: is enabled == not hidden by user/api (referred to as "Hide" in _DefaultHide and _NoHide) flags. */
    object IsEnabled : Status(1 shl 24)

    /** Status: is visible == is enabled AND not clipped by scrolling. */
    object IsVisible : Status(1 shl 25)

    /** Status: is currently part of the sort specs */
    object IsSorted : Status(1 shl 26)

    /** Status: is hovered by mouse */
    object IsHovered : Status(1 shl 27)

    /** [Internal] Disable user resizing this column directly (it may however we resized indirectly from its left edge) */
    object NoDirectResize_ : Setup(1 shl 30)

    @GenSealedEnum
    companion object {
        /** [Internal] Combinations and masks */
        val WidthMask: TableColumnSetupFlags get() = WidthStretch or WidthFixed

        val IndentMask: TableColumnSetupFlags get() = IndentEnable or IndentDisable

        val StatusMask: TableColumnFlags get() = IsEnabled or IsVisible or IsSorted or IsHovered
    }
}

typealias TableRowFlags = Flag<TableRowFlag>

// Flags for ImGui::TableNextRow()
sealed class TableRowFlag(override val i: Int) : FlagBase<TableRowFlag>() {
    /** Identify header row (set default background color + width of its contents accounted differently for auto column width) */
    object Headers : TableRowFlag(1 shl 0)

    @GenSealedEnum
    companion object
}

// Enum for ImGui::TableSetBgColor()
// Background colors are rendering in 3 layers:
//  - Layer 0: draw with RowBg0 color if set, otherwise draw with ColumnBg0 if set.
//  - Layer 1: draw with RowBg1 color if set, otherwise draw with ColumnBg1 if set.
//  - Layer 2: draw with CellBg color if set.
// The purpose of the two row/columns layers is to let you decide if a background color change should override or blend with the existing color.
// When using ImGuiTableFlags_RowBg on the table, each row has the RowBg0 color automatically set for odd/even rows.
// If you set the color of RowBg0 target, your color will override the existing RowBg0 color.
// If you set the color of RowBg1 or ColumnBg1 target, your color will blend over the RowBg0 color.
enum class TableBgTarget(val i: Int) {
    /** Set row background color 0 (generally used for background, automatically set when ImGuiTableFlags_RowBg is used) */
    RowBg0(1),

    /** Set row background color 1 (generally used for selection marking) */
    RowBg1(2),

    /** Set cell background color (top-most color) */
    CellBg(3);

    companion object {
        infix fun of(i: Int) = values().first { it.i == i }
    }
}

typealias FocusedFlags = Flag<FocusedFlag>

/** Flags for ImGui::IsWindowFocused() */
sealed class FocusedFlag : FlagBase<FocusedFlag>() {
    /** Return true if any children of the window is focused */
    object ChildWindows : FocusedFlag()

    /** Test from root window (top most parent of the current hierarchy) */
    object RootWindow : FocusedFlag()

    /** Return true if any window is focused.
     *  Important: If you are trying to tell how to dispatch your low-level inputs, do NOT use this. Use 'io.WantCaptureMouse' instead! Please read the FAQ! */
    object AnyWindow : FocusedFlag()

    /** Do not consider popup hierarchy (do not treat popup emitter as parent of popup) (when used with _ChildWindows or _RootWindow) */
    object NoPopupHierarchy : FocusedFlag()

    //ImGuiFocusedFlags_DockHierarchy               = 1 << 4,   // Consider docking hierarchy (treat dockspace host as parent of docked window) (when used with _ChildWindows or _RootWindow)

    override val i: Int = 1 shl ordinal

    @GenSealedEnum
    companion object {
        val RootAndChildWindows: FocusedFlags get() = RootWindow or ChildWindows
    }
}

typealias HoveredFlags = Flag<HoveredFlag<*>>
typealias ItemHoveredFlags = Flag<HoveredFlag.Item>
typealias WindowHoveredFlags = Flag<HoveredFlag.Window>

/** Flags: for IsItemHovered(), IsWindowHovered() etc.
 *  Note: if you are trying to check whether your mouse should be dispatched to Dear ImGui or to your app, you should use 'io.WantCaptureMouse' instead! Please read the FAQ!
 *  Note: windows with the ImGuiWindowFlags_NoInputs flag are ignored by IsWindowHovered() calls.*/

@Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
sealed interface HoveredFlag<out HF : HoveredFlag<HF>> : Flag<HF> {
    sealed interface Window : HoveredFlag<Window>
    sealed class WindowBase(override val i: Int) : Window, FlagBase<Window>()

    sealed interface Item : HoveredFlag<Item>
    sealed class ItemBase(override val i: Int) : Item, FlagBase<Item>()

    sealed class General(override val i: Int) : Window, Item, FlagBase<General>()

    /** isWindowHovered() only: Return true if any children of the window is hovered */
    object ChildWindows : WindowBase(1 shl 0)

    /** isWindowHovered() only: Test from root window (top most parent of the current hierarchy) */
    object RootWindow : WindowBase(1 shl 1)

    /** IsWindowHovered() only: Return true if any window is hovered    */
    object AnyWindow : WindowBase(1 shl 2)

    /** IsWindowHovered() only: Do not consider popup hierarchy (do not treat popup emitter as parent of popup) (when used with _ChildWindows or _RootWindow) */
    object NoPopupHierarchy : WindowBase(1 shl 3)
    //ImGuiHoveredFlags_DockHierarchy               = 1 << 4,   // IsWindowHovered() only: Consider docking hierarchy (treat dockspace host as parent of docked window) (when used with _ChildWindows or _RootWindow)

    /** Return true even if a popup window is normally blocking access to this item/window  */
    object AllowWhenBlockedByPopup : General(1 shl 5)
    //AllowWhenBlockedByModal     (1 shl 6),   // Return true even if a modal popup window is normally blocking access to this item/window. FIXME-TODO: Unavailable yet.

    /** Return true even if an active item is blocking access to this item/window. Useful for Drag and Drop patterns.   */
    object AllowWhenBlockedByActiveItem : General(1 shl 7)

    /** IsItemHovered() only: Return true even if the position is obstructed or overlapped by another window,   */
    object AllowWhenOverlapped : ItemBase(1 shl 8)

    /** IsItemHovered() only: Return true even if the item is disabled */
    object AllowWhenDisabled : ItemBase(1 shl 9)

    /** Disable using gamepad/keyboard navigation state when active, always query mouse. */
    object NoNavOverride : General(1 shl 10)

    // Hovering delays (for tooltips)
    /** Return true after io.HoverDelayNormal elapsed (~0.30 sec) */
    object DelayNormal : General(1 shl 11)

    /** Return true after io.HoverDelayShort elapsed (~0.10 sec) */
    object DelayShort : General(1 shl 12)

    /** Disable shared delay system where moving from one item to the next keeps the previous timer for a short time (standard for tooltips with long delays) */
    object NoSharedDelay : General(1 shl 13)

    @GenSealedEnum
    companion object {
        val RootAndChildWindows: WindowHoveredFlags get() = RootWindow or ChildWindows
        val RectOnly get() = AllowWhenBlockedByPopup or AllowWhenBlockedByActiveItem or AllowWhenOverlapped
    }
}

typealias DragDropFlags = Flag<DragDropFlag>

/** Flags for beginDragDropSource(), acceptDragDropPayload() */
sealed class DragDropFlag(override val i: Int) : FlagBase<DragDropFlag>() {
    /** Disable preview tooltip. By default, a successful call to BeginDragDropSource opens a tooltip so you can display a preview or description of the source contents. This flag disables this behavior. */
    object SourceNoPreviewTooltip : DragDropFlag(1 shl 0)

    /** By default, when dragging we clear data so that IsItemHovered() will return false,
     *  to avoid subsequent user code submitting tooltips.
     *  This flag disables this behavior so you can still call IsItemHovered() on the source item. */
    object SourceNoDisableHover : DragDropFlag(1 shl 1)

    /** Disable the behavior that allows to open tree nodes and collapsing header by holding over them while dragging
     *  a source item. */
    object SourceNoHoldToOpenOthers : DragDropFlag(1 shl 2)

    /** Allow items such as text()), Image() that have no unique identifier to be used as drag source),
     *  by manufacturing a temporary identifier based on their window-relative position.
     *  This is extremely unusual within the dear imgui ecosystem and so we made it explicit. */
    object SourceAllowNullID : DragDropFlag(1 shl 3)

    /** External source (from outside of dear imgui), won't attempt to read current item/window info. Will always return true.
     *  Only one Extern source can be active simultaneously.    */
    object SourceExtern : DragDropFlag(1 shl 4)

    /** Automatically expire the payload if the source cease to be submitted (otherwise payloads are persisting while being dragged) */
    object SourceAutoExpirePayload : DragDropFlag(1 shl 5)
    // AcceptDragDropPayload() flags
    /** AcceptDragDropPayload() will returns true even before the mouse button is released.
     *  You can then call isDelivery() to test if the payload needs to be delivered. */
    object AcceptBeforeDelivery : DragDropFlag(1 shl 10)

    /** Do not draw the default highlight rectangle when hovering over target. */
    object AcceptNoDrawDefaultRect : DragDropFlag(1 shl 11)

    /** Request hiding the BeginDragDropSource tooltip from the BeginDragDropTarget site. */
    object AcceptNoPreviewTooltip : DragDropFlag(1 shl 12)

    @GenSealedEnum
    companion object {
        /** For peeking ahead and inspecting the payload before delivery. */
        val AcceptPeekOnly: DragDropFlags get() = AcceptBeforeDelivery or AcceptNoDrawDefaultRect
    }
}

// Standard Drag and Drop payload types. Types starting with '_' are defined by Dear ImGui.
/** float[3]: Standard type for colors, without alpha. User code may use this type. */
val PAYLOAD_TYPE_COLOR_3F = "_COL3F"

/** float[4]: Standard type for colors. User code may use this type. */
val PAYLOAD_TYPE_COLOR_4F = "_COL4F"

/** A primary data type */
enum class DataType(val imguiName: String) {
    Byte("S8"), Ubyte("U8"),
    Short("S16"), Ushort("U16"),
    Int("S32"), Uint("U32"),
    Long("S64"), Ulong("U64"),
    Float("float"), Double("double"),
    Count("Count"),

    _String("[internal] String"), _Pointer("[internal] Pointer"), _ID("[internal] ID");

    val isUnsigned: Boolean
        get() = when (this) {
            Ubyte, Ushort, Uint, Ulong -> true
            else -> false
        }

    @JvmField
    val i = ordinal

    companion object {
        val imguiValues = values().dropLast(4)
    }
}

// Float is the most common data type in imgui, followed by Int, and so we have them at the start
// so that the compiler's dead code elimination gets rid of the rest of the branches.
// In reality, the JVM will optimize the comparisons since they're constant checks,
// So there should be no performance difference in tight loops.
// Also, a minifier like proguard deals with this very easily.
inline fun <reified D> dataTypeOf(): DataType where D : Number, D : Comparable<D> = when (D::class) {
    Float::class -> DataType.Float
    Int::class -> DataType.Int
    Byte::class -> DataType.Byte
    Short::class -> DataType.Short
    Long::class -> DataType.Long
    Double::class -> DataType.Double
    Ubyte::class -> DataType.Ubyte
    Ushort::class -> DataType.Ushort
    Uint::class -> DataType.Uint
    Ulong::class -> DataType.Ulong
    else -> {
        println("")
        throw Error("Unsupported data type ${D::class}")
    }
}

/** A cardinal direction */
enum class Dir {
    None, Left, Right, Up, Down;

    @JvmField
    val i = ordinal - 1


    companion object {
        infix fun of(i: Int) = values().first { it.i == i }
        val COUNT = Down.i + 1
    }
}

infix fun Int.shl(b: Dir) = shl(b.i)


/** A sorting direction */
enum class SortDirection {
    None,

    /** Ascending = 0->9, A->Z etc. */
    Ascending,

    /** Descending = 9->0, Z->A etc. */
    Descending;

    @JvmField
    val i = ordinal

    companion object {
        infix fun of(i: Int) = values().first { it.i == i }
    }
}


typealias KeyChord = Flag<Key>

// A key identifier (ImGuiKey_XXX or ImGuiMod_XXX value): can represent Keyboard, Mouse and Gamepad values.
// All our named keys are >= 512. Keys value 0 to 511 are left unused as legacy native/opaque key values (< 1.87).
// Since >= 1.89 we increased typing (went from int to enum), some legacy code may need a cast to ImGuiKey.
// Read details about the 1.87 and 1.89 transition : https://github.com/ocornut/imgui/issues/4921
// Note that "Keys" related to physical keys and are not the same concept as input "Characters", the later are submitted via io.AddInputCharacter().

/** A key identifier (ImGui-side enum) */
sealed class Key(private val int: Int? = null) : FlagBase<Key>() {

    // Keyboard
    object None : Key()

    object Tab : Key()
    object LeftArrow : Key()
    object RightArrow : Key()
    object UpArrow : Key()
    object DownArrow : Key()
    object PageUp : Key()
    object PageDown : Key()
    object Home : Key()
    object End : Key()
    object Insert : Key()
    object Delete : Key()
    object Backspace : Key()
    object Space : Key()
    object Enter : Key()
    object Escape : Key()
    object LeftCtrl : Key()
    object LeftShift : Key()
    object LeftAlt : Key()
    object LeftSuper : Key()
    object RightCtrl : Key()
    object RightShift : Key()
    object RightAlt : Key()
    object RightSuper : Key()
    object Menu : Key()
    object `0` : Key()
    object `1` : Key()
    object `2` : Key()
    object `3` : Key()
    object `4` : Key()
    object `5` : Key()
    object `6` : Key()
    object `7` : Key()
    object `8` : Key()
    object `9` : Key()
    object A : Key()
    object B : Key()
    object C : Key()
    object D : Key()
    object E : Key()
    object F : Key()
    object G : Key()
    object H : Key()
    object I : Key()
    object J : Key()
    object K : Key()
    object L : Key()
    object M : Key()
    object N : Key()
    object O : Key()
    object P : Key()
    object Q : Key()
    object R : Key()
    object S : Key()
    object T : Key()
    object U : Key()
    object V : Key()
    object W : Key()
    object X : Key()
    object Y : Key()
    object Z : Key()
    object F1 : Key()
    object F2 : Key()
    object F3 : Key()
    object F4 : Key()
    object F5 : Key()
    object F6 : Key()
    object F7 : Key()
    object F8 : Key()
    object F9 : Key()
    object F10 : Key()
    object F11 : Key()
    object F12 : Key()

    /** ' */
    object Apostrophe : Key()

    /** , */
    object Comma : Key()

    /** - */
    object Minus : Key()

    /** . */
    object Period : Key()

    /** / */
    object Slash : Key()

    /** ; */
    object Semicolon : Key()

    /** = */
    object Equal : Key()

    /** [ */
    object LeftBracket : Key()

    /** \ (this text inhibit multiline comment caused by backslash) */
    object Backslash : Key()

    /** ] */
    object RightBracket : Key()

    /** ` */
    object GraveAccent : Key()

    object CapsLock : Key()
    object ScrollLock : Key()
    object NumLock : Key()
    object PrintScreen : Key()
    object Pause : Key()
    object Keypad0 : Key()
    object Keypad1 : Key()
    object Keypad2 : Key()
    object Keypad3 : Key()
    object Keypad4 : Key()
    object Keypad5 : Key()
    object Keypad6 : Key()
    object Keypad7 : Key()
    object Keypad8 : Key()
    object Keypad9 : Key()
    object KeypadDecimal : Key()
    object KeypadDivide : Key()
    object KeypadMultiply : Key()
    object KeypadSubtract : Key()
    object KeypadAdd : Key()
    object KeypadEnter : Key()
    object KeypadEqual : Key()

    // Gamepad (some of those are analog values, 0.0f to 1.0f)                         // NAVIGATION action
    object GamepadStart : Key()          // Menu (Xbox)          + (Switch)      Start/Options (PS)  // --

    object GamepadBack : Key()           // View (Xbox)          - (Switch)      Share (PS)          // --
    object GamepadFaceLeft : Key()       // X (Xbox)             Y (Switch)      Square (PS)         // -> ImGuiNavInput_Menu

    object GamepadFaceRight : Key()      // B (Xbox)             A (Switch)      Circle (PS)         // -> ImGuiNavInput_Cancel

    object GamepadFaceUp : Key()         // Y (Xbox)             X (Switch)      Triangle (PS)       // -> ImGuiNavInput_Input

    object GamepadFaceDown : Key()       // A (Xbox)             B (Switch)      Cross (PS)          // -> ImGuiNavInput_Activate

    object GamepadDpadLeft : Key()       // D-pad Left                                               // -> ImGuiNavInput_DpadLeft

    object GamepadDpadRight : Key()      // D-pad Right                                              // -> ImGuiNavInput_DpadRight

    object GamepadDpadUp : Key()         // D-pad Up                                                 // -> ImGuiNavInput_DpadUp

    object GamepadDpadDown : Key()       // D-pad Down                                               // -> ImGuiNavInput_DpadDown

    object GamepadL1 : Key()             // L Bumper (Xbox)      L (Switch)      L1 (PS)             // -> ImGuiNavInput_FocusPrev + ImGuiNavInput_TweakSlow

    object GamepadR1 : Key()             // R Bumper (Xbox)      R (Switch)      R1 (PS)             // -> ImGuiNavInput_FocusNext + ImGuiNavInput_TweakFast

    object GamepadL2 : Key()             // L Trigger (Xbox)     ZL (Switch)     L2 (PS) [Analog]
    object GamepadR2 : Key()             // R Trigger (Xbox)     ZR (Switch)     R2 (PS) [Analog]
    object GamepadL3 : Key()             // L Thumbstick (Xbox)  L3 (Switch)     L3 (PS)
    object GamepadR3 : Key()             // R Thumbstick (Xbox)  R3 (Switch)     R3 (PS)
    object GamepadLStickLeft : Key()     // [Analog]                                                 // -> ImGuiNavInput_LStickLeft

    object GamepadLStickRight : Key()    // [Analog]                                                 // -> ImGuiNavInput_LStickRight

    object GamepadLStickUp : Key()       // [Analog]                                                 // -> ImGuiNavInput_LStickUp

    object GamepadLStickDown : Key()     // [Analog]                                                 // -> ImGuiNavInput_LStickDown

    object GamepadRStickLeft : Key()     // [Analog]
    object GamepadRStickRight : Key()    // [Analog]
    object GamepadRStickUp : Key()       // [Analog]
    object GamepadRStickDown : Key()     // [Analog]

    // Aliases: Mouse Buttons (auto-submitted from AddMouseButtonEvent() calls)
    // - This is mirroring the data also written to io.MouseDown[], io.MouseWheel, in a format allowing them to be accessed via standard key API.
    object MouseLeft : Key()

    object MouseRight : Key()
    object MouseMiddle : Key()
    object MouseX1 : Key()
    object MouseX2 : Key()
    object MouseWheelX : Key()
    object MouseWheelY : Key()

    // [Internal] Reserved for mod storage
    object ReservedForModCtrl : Key()

    object ReservedForModShift : Key()
    object ReservedForModAlt : Key()
    object ReservedForModSuper : Key()
    object Count : Key()

    // Keyboard Modifiers (explicitly submitted by backend via AddKeyEvent() calls)
    // - This is mirroring the data also written to io.KeyCtrl, io.KeyShift, io.KeyAlt, io.KeySuper, in a format allowing
    //   them to be accessed via standard key API, allowing calls such as IsKeyPressed(), IsKeyReleased(), querying duration etc.
    // - Code polling every key (e.g. an interface to detect a key press for input mapping) might want to ignore those
    //   and prefer using the real keys (e.g. ImGuiKey_LeftCtrl, ImGuiKey_RightCtrl instead of ImGuiMod_Ctrl).
    // - In theory the value of keyboard modifiers should be roughly equivalent to a logical or of the equivalent left/right keys.
    //   In practice: it's complicated; mods are often provided from different sources. Keyboard layout, IME, sticky keys and
    //   backends tend to interfere and break that equivalence. The safer decision is to relay that ambiguity down to the end-user...
    object Mod_None : Key(0)

    /** Alias for Ctrl (non-macOS) _or_ Super (macOS). */
    object Mod_Shortcut : Key(1 shl 11)

    /** Ctrl */
    object Mod_Ctrl : Key(1 shl 12)

    /** Shift */
    object Mod_Shift : Key(1 shl 13)

    /** Option/Menu */
    object Mod_Alt : Key(1 shl 14)

    /** Cmd/Super/Windows */
    object Mod_Super : Key(1 shl 15)

    override val i: Int = int ?: if (ordinal == 0) 0 else 511 + ordinal

    val index: Int
        get() = ordinal - 1

    @GenSealedEnum
    companion object {
        val COUNT get() = values.size
        val BEGIN: Key get() = None
        val END: Key get() = Count
        val Named get() = values.drop(1).take(Count.ordinal - 1)
        val Data get() = Named
        val Keyboard get() = values.drop(1).take(GamepadStart.ordinal - 1)
        val Gamepad get() = values.drop(GamepadStart.ordinal).take(GamepadRStickDown.ordinal - GamepadStart.ordinal + 1)
        val Mouse get() = values.drop(MouseLeft.ordinal).take(MouseWheelY.ordinal - MouseLeft.ordinal + 1)
        val Aliases get() = Mouse
        val Mod_Mask get() = Mod_Shortcut or Mod_Ctrl or Mod_Shift or Mod_Alt or Mod_Super
        infix fun of(chord: KeyChord) = values.first { chord == it }

        // [Internal] Named shortcuts for Navigation
        internal val _NavKeyboardTweakSlow get() = Mod_Ctrl
        internal val _NavKeyboardTweakFast get() = Mod_Shift
        internal val _NavGamepadTweakSlow get() = GamepadL1
        internal val _NavGamepadTweakFast get() = GamepadR1
        internal val _NavGamepadActivate get() = GamepadFaceDown
        internal val _NavGamepadCancel get() = GamepadFaceRight
        internal val _NavGamepadMenu get() = GamepadFaceLeft
        internal val _NavGamepadInput get() = GamepadFaceUp
    }
}

// Enumeration for AddMouseSourceEvent() actual source of Mouse Input data.
// Historically we use "Mouse" terminology everywhere to indicate pointer data, e.g. MousePos, IsMousePressed(), io.AddMousePosEvent()
// But that "Mouse" data can come from different source which occasionally may be useful for application to know about.
// You can submit a change of pointer type using io.AddMouseSourceEvent().
// -> enum ImGuiMouseSource      // Enum; A mouse input source identifier (Mouse, TouchScreen, Pen)
sealed class MouseSource : FlagBase<MouseSource>() {

    /** Input is coming from an actual mouse. */
    object Mouse : MouseSource()

    /** Input is coming from a touch screen (no hovering prior to initial press, less precise initial press aiming, dual-axis wheeling possible). */
    object TouchScreen : MouseSource()

    /** Input is coming from a pressure/magnetic pen (often used in conjunction with high-sampling rates). */
    object Pen : MouseSource()

    override val i: Int = ordinal

    @GenSealedEnum
    companion object {
        val COUNT get() = values.size
    }
}

/** ~MouseButtonToKey */
val MouseButton.key: Key
    get() = when (this) {
        MouseButton.None -> Key.None
        MouseButton.Left -> Key.MouseLeft
        MouseButton.Right -> Key.MouseRight
        MouseButton.Middle -> Key.MouseMiddle
        MouseButton._unused0 -> Key.MouseX1
        MouseButton._unused1 -> Key.MouseX2
    }

// for IO.keyMap

operator fun IntArray.set(key: Key, value: Int) {
    this[key.index] = value
}

operator fun IntArray.get(key: Key): Int = get(key.index)


/** Gamepad/Keyboard navigation
 *  Since >= 1.87 backends you generally don't need to care about this enum since io.NavInputs[] is setup automatically. This might become private/internal some day.
 *  Keyboard: Set io.configFlags |= NavFlags.EnableKeyboard to enable. ::newFrame() will automatically fill io.navInputs[] based on your io.AddKeyEvent() calls.
 *  Gamepad:  Set io.configFlags |= NavFlags.EnableGamepad to enable. Fill the io.navInputs[] fields before calling NewFrame(). Note that io.navInputs[] is cleared by EndFrame().
 *  Read instructions in imgui.cpp for more details. Download PNG/PSD at http://dearimgui.org/controls_sheets.
 *
 *  An input identifier for navigation */
enum class NavInput {
    // Gamepad Mapping
    /** Activate / Open / Toggle / Tweak value       // e.g. Cross  (PS4), A (Xbox), A (Switch), Space (Keyboard) */
    Activate,

    /** Cancel / Close / Exit                        // e.g. Circle (PS4), B (Xbox), B (Switch), Escape (Keyboard) */
    Cancel,

    /** Text input / On-Screen keyboard              // e.g. Triang.(PS4), Y (Xbox), X (Switch), Return (Keyboard) */
    Input,

    /** Tap: Toggle menu / Hold: Focus, Move, Resize // e.g. Square (PS4), X (Xbox), Y (Switch), Alt (Keyboard) */
    Menu,

    /** Move / Tweak / Resize window (w/ PadMenu)    // e.g. D-pad Left/Right/Up/Down (Gamepads), Arrow keys (Keyboard) */
    DpadLeft,

    DpadRight,

    DpadUp,

    DpadDown,

    /** Scroll / Move window (w/ PadMenu)            // e.g. Left Analog Stick Left/Right/Up/Down   */
    LStickLeft,

    LStickRight,

    LStickUp,

    LStickDown,

    /** Focus Next window (w/ PadMenu)               // e.g. L1 or L2 (PS4), LB or LT (Xbox), L or ZL (Switch) */
    FocusPrev,

    /** Focus Prev window (w/ PadMenu)               // e.g. R1 or R2 (PS4), RB or RT (Xbox), R or ZL (Switch) */
    FocusNext,

    /** Slower tweaks                                // e.g. L1 or L2 (PS4), LB or LT (Xbox), L or ZL (Switch) */
    TweakSlow,

    /** Faster tweaks                                // e.g. R1 or R2 (PS4), RB or RT (Xbox), R or ZL (Switch) */
    TweakFast,

    // [Internal] Don't use directly! This is used internally to differentiate keyboard from gamepad inputs for behaviors that require to differentiate them.
    // Keyboard behavior that have no corresponding gamepad mapping (e.g. CTRL+TAB) will be directly reading from keyboard keys instead of io.NavInputs[].

    /** Move left = Arrow keys  */
    _KeyLeft,

    /** Move right = Arrow keys  */
    _KeyRight,

    /** Move up = Arrow keys  */
    _KeyUp,

    /** Move down = Arrow keys  */
    _KeyDown,
    Count;

    @JvmField
    val i = ordinal

    companion object {
        val COUNT = values().size
        val InternalStart = _KeyLeft.i
    }
}

infix fun Int.shl(f: NavInput) = shl(f.i)

// for IO.navInputs

operator fun FloatArray.set(index: NavInput, value: Float) {
    this[index.i] = value
}

operator fun FloatArray.get(index: NavInput): Float = get(index.i)


typealias ConfigFlags = Flag<ConfigFlag>

/** Configuration flags stored in io.configFlags
 *
 *  Flags: for io.ConfigFlags   */
sealed class ConfigFlag(override val i: Int) : FlagBase<ConfigFlag>() {
    /** Master keyboard navigation enable flag. Enable full Tabbing + directional arrows + space/enter to activate. */
    object NavEnableKeyboard : ConfigFlag(1 shl 0)

    /** Master gamepad navigation enable flag. This is mostly to instruct your imgui backend to fill io.NavInputs[].
     *  Backend also needs to set ImGuiBackendFlags_HasGamepad. */
    object NavEnableGamepad : ConfigFlag(1 shl 1)

    /** Instruct navigation to move the mouse cursor. May be useful on TV/console systems where moving a virtual mouse is awkward.
     *  Will update io.MousePos and set io.wantSetMousePos=true. If enabled you MUST honor io.wantSetMousePos requests in your backend,
     *  otherwise ImGui will react as if the mouse is jumping around back and forth. */
    object NavEnableSetMousePos : ConfigFlag(1 shl 2)

    /** Instruct navigation to not set the io.WantCaptureKeyboard flag when io.NavActive is set. */
    object NavNoCaptureKeyboard : ConfigFlag(1 shl 3)

    /** Instruct imgui to clear mouse position/buttons in NewFrame(). This allows ignoring the mouse information set by the backend. */
    object NoMouse : ConfigFlag(1 shl 4)

    /** Request backend to not alter mouse cursor configuration.
     *  Use if the backend cursor changes are interfering with yours and you don't want to use setMouseCursor() to change mouse cursor.
     *  You may want to honor requests from imgui by reading ::mouseCursor yourself instead. */
    object NoMouseCursorChange : ConfigFlag(1 shl 5)

    /** JVM custom, request backend to not read the mouse status allowing you to provide your own custom input */
    object NoMouseUpdate : ConfigFlag(1 shl 12)

    /** JVM custom */
    object NoKeyboardUpdate : ConfigFlag(1 shl 13)

    // User storage (to allow your backend/engine to communicate to code that may be shared between multiple projects. Those flags are NOT used by core Dear ImGui)

    /** Application is SRGB-aware. */
    object IsSRGB : ConfigFlag(1 shl 20)

    /** Application is using a touch screen instead of a mouse. */
    object IsTouchScreen : ConfigFlag(1 shl 21)

    @GenSealedEnum
    companion object
}


typealias BackendFlags = Flag<BackendFlag>

/** Backend capabilities flags stored in io.BackendFlag. Set by imgui_impl_xxx or custom backend.
 *
 *  Flags: for io.BackendFlags  */
sealed class BackendFlag : FlagBase<BackendFlag>() {
    override val i: Int = 1 shl ordinal

    /** Backend Platform supports gamepad and currently has one connected. */
    object HasGamepad : BackendFlag()

    /** Backend Platform supports honoring GetMouseCursor() value to change the OS cursor shape. */
    object HasMouseCursors : BackendFlag()

    /** Backend Platform supports io.WantSetMousePos requests to reposition the OS mouse position (only used if ImGuiConfigFlags_NavEnableSetMousePos is set). */
    object HasSetMousePos : BackendFlag()

    /** Backend Platform supports ImDrawCmd::VtxOffset. This enables output of large meshes (64K+ vertices) while still using 16-bit indices. */
    object RendererHasVtxOffset : BackendFlag()

    @GenSealedEnum
    companion object
}

/** Enumeration for PushStyleColor() / PopStyleColor()  */
/** A color identifier for styling */
enum class Col {

    Text,
    TextDisabled,

    /** Background of normal windows    */
    WindowBg,

    /** Background of child windows */
    ChildBg,

    /*-* Background of popups, menus, tooltips windows  */
    PopupBg,
    Border,
    BorderShadow,

    /** Background of checkbox, radio button, plot, slider, text input  */
    FrameBg,
    FrameBgHovered,
    FrameBgActive,
    TitleBg,
    TitleBgActive,
    TitleBgCollapsed,
    MenuBarBg,
    ScrollbarBg,
    ScrollbarGrab,
    ScrollbarGrabHovered,
    ScrollbarGrabActive,
    CheckMark,
    SliderGrab,
    SliderGrabActive,
    Button,
    ButtonHovered,
    ButtonActive,

    /** Header* colors are used for CollapsingHeader, TreeNode, Selectable, MenuItem */
    Header,
    HeaderHovered,
    HeaderActive,
    Separator,
    SeparatorHovered,
    SeparatorActive,

    /** Resize grip in lower-right and lower-left corners of windows. */
    ResizeGrip,
    ResizeGripHovered,
    ResizeGripActive,

    /** TabItem in a TabBar */
    Tab,
    TabHovered,
    TabActive,
    TabUnfocused,
    TabUnfocusedActive,
    PlotLines,
    PlotLinesHovered,
    PlotHistogram,
    PlotHistogramHovered,

    /** Table header background */
    TableHeaderBg,

    /** Table outer and header borders (prefer using Alpha=1.0 here) */
    TableBorderStrong,

    /** Table inner borders (prefer using Alpha=1.0 here) */
    TableBorderLight,

    /** Table row background (even rows) */
    TableRowBg,

    /** Table row background (odd rows) */
    TableRowBgAlt,
    TextSelectedBg,

    /** Rectangle highlighting a drop target */
    DragDropTarget,

    /** Gamepad/keyboard: current highlighted item  */
    NavHighlight,

    /** Highlight window when using CTRL+TAB */
    NavWindowingHighlight,

    /** Darken/colorize entire screen behind the CTRL+TAB window list, when active */
    NavWindowingDimBg,

    /** Darken/colorize entire screen behind a modal window, when one is active; */
    ModalWindowDimBg;

    @JvmField
    val i = ordinal

    val u32 get() = getColorU32(i, alphaMul = 1f)

    companion object {
        val COUNT = values().size
        fun of(i: Int) = values()[i]
    }
}

/** for style.colors    */
operator fun ArrayList<Vec4>.get(idx: Col): Vec4 = this[idx.i]

operator fun ArrayList<Vec4>.set(idx: Col, vec: Vec4) = this[idx.i] put vec


// Enumeration for PushStyleVar() / PopStyleVar() to temporarily modify the ImGuiStyle structure.
// - The enum only refers to fields of ImGuiStyle which makes sense to be pushed/popped inside UI code.
//   During initialization or between frames, feel free to just poke into ImGuiStyle directly.
// - Tip: Use your programming IDE navigation facilities on the names in the _second column_ below to find the actual members and their description.
//   In Visual Studio IDE: CTRL+comma ("Edit.GoToAll") can follow symbols in comments, whereas CTRL+F12 ("Edit.GoToImplementation") cannot.
//   With Visual Assist installed: ALT+G ("VAssistX.GoToImplementation") can also follow symbols in comments.
// - When changing this enum, you need to update the associated internal table GStyleVarInfo[] accordingly. This is where we link enum values to members offset/type.

/** A variable identifier for styling   */
enum class StyleVar {
    /** float   */
    Alpha,

    /** WindowPadding */
    DisabledAlpha,

    /** vec2    */
    WindowPadding,

    /** float   */
    WindowRounding,

    /** float */
    WindowBorderSize,

    /** vec2    */
    WindowMinSize,

    /** Vec2    */
    WindowTitleAlign,

    /** float   */
    ChildRounding,

    /** float */
    ChildBorderSize,

    /** float */
    PopupRounding,

    /** float */
    PopupBorderSize,

    /** vec2    */
    FramePadding,

    /** float   */
    FrameRounding,

    /** float   */
    FrameBorderSize,

    /** vec2    */
    ItemSpacing,

    /** vec2    */
    ItemInnerSpacing,

    /** float   */
    IndentSpacing,

    /** vec2    */
    CellPadding,

    /** Float   */
    ScrollbarSize,

    /** Float   */
    ScrollbarRounding,

    /** float   */
    GrabMinSize,

    /** float   */
    GrabRounding,

    /** float */
    TabRounding,

    /** vec2  */
    ButtonTextAlign,

    /** vec2  */
    SelectableTextAlign,

    /** float */
    SeparatorTextBorderSize,

    /** vec2 */
    SeparatorTextAlign,

    /** vec2 */
    SeparatorTextPadding;

    @JvmField
    val i = ordinal
}


typealias ColorEditFlags = Flag<ColorEditFlag>

/** Flags for ColorEdit3() / ColorEdit4() / ColorPicker3() / ColorPicker4() / ColorButton()
 *
 *  Flags: for ColorEdit4(), ColorPicker4() etc.    */
sealed class ColorEditFlag(override val i: Int) : FlagBase<ColorEditFlag>() {
    /** ColorEdit, ColorPicker, ColorButton: ignore Alpha component (will only read 3 components from the input pointer). */
    object NoAlpha : ColorEditFlag(1 shl 1)

    /** ColorEdit: disable picker when clicking on color square.  */
    object NoPicker : ColorEditFlag(1 shl 2)

    /** ColorEdit: disable toggling options menu when right-clicking on inputs/small preview.   */
    object NoOptions : ColorEditFlag(1 shl 3)

    /** ColorEdit, ColorPicker: disable color square preview next to the inputs. (e.g. to show only the inputs)   */
    object NoSmallPreview : ColorEditFlag(1 shl 4)

    /** ColorEdit, ColorPicker: disable inputs sliders/text widgets (e.g. to show only the small preview color square).   */
    object NoInputs : ColorEditFlag(1 shl 5)

    /** ColorEdit, ColorPicker, ColorButton: disable tooltip when hovering the preview. */
    object NoTooltip : ColorEditFlag(1 shl 6)

    /** ColorEdit, ColorPicker: disable display of inline text label (the label is still forwarded to the tooltip and picker).  */
    object NoLabel : ColorEditFlag(1 shl 7)

    /** ColorPicker: disable bigger color preview on right side of the picker, use small color square preview instead.    */
    object NoSidePreview : ColorEditFlag(1 shl 8)

    /** ColorEdit: disable drag and drop target. ColorButton: disable drag and drop source. */
    object NoDragDrop : ColorEditFlag(1 shl 9)

    /** ColorButton: disable border (which is enforced by default) */
    object NoBorder : ColorEditFlag(1 shl 10)

    // User Options (right-click on widget to change some of them).

    /** ColorEdit, ColorPicker: show vertical alpha bar/gradient in picker. */
    object AlphaBar : ColorEditFlag(1 shl 16)

    /** ColorEdit, ColorPicker, ColorButton: display preview as a transparent color over a checkerboard, instead of opaque. */
    object AlphaPreview : ColorEditFlag(1 shl 17)

    /** ColorEdit, ColorPicker, ColorButton: display half opaque / half checkerboard, instead of opaque.    */
    object AlphaPreviewHalf : ColorEditFlag(1 shl 18)

    /** (WIP) ColorEdit: Currently only disable 0.0f..1.0f limits in RGBA edition (note: you probably want to use
     *  ColorEditFlags.Float flag as well). */
    object HDR : ColorEditFlag(1 shl 19)

    /** [Display] ColorEdit: override _display_ type among RGB/HSV/Hex. ColorPicker: select any combination using one or more of RGB/HSV/Hex.    */
    object DisplayRGB : ColorEditFlag(1 shl 20)

    /** [Display] ColorEdit: override _display_ type among RGB/HSV/Hex. ColorPicker: select any combination using one or more of RGB/HSV/Hex.    */
    object DisplayHSV : ColorEditFlag(1 shl 21)

    /** [Display] ColorEdit: override _display_ type among RGB/HSV/Hex. ColorPicker: select any combination using one or more of RGB/HSV/Hex.    */
    object DisplayHEX : ColorEditFlag(1 shl 22)

    /** [DataType] ColorEdit, ColorPicker, ColorButton: _display_ values formatted as 0..255.   */
    object Uint8 : ColorEditFlag(1 shl 23)

    /** [DataType] ColorEdit, ColorPicker, ColorButton: _display_ values formatted as 0.0f..1.0f floats instead of 0..255 integers.
     *  No round-trip of value via integers.    */
    object Float : ColorEditFlag(1 shl 24)

    /** [Picker]     // ColorPicker: bar for Hue, rectangle for Sat/Value. */
    object PickerHueBar : ColorEditFlag(1 shl 25)

    /** [Picker]     // ColorPicker: wheel for Hue, triangle for Sat/Value. */
    object PickerHueWheel : ColorEditFlag(1 shl 26)

    /** [Input]      // ColorEdit, ColorPicker: input and output data in RGB format. */
    object InputRGB : ColorEditFlag(1 shl 27)

    /** [Input]      // ColorEdit, ColorPicker: input and output data in HSV format. */
    object InputHSV : ColorEditFlag(1 shl 28)

    @GenSealedEnum
    companion object {
        /** Defaults Options. You can set application defaults using SetColorEditOptions(). The intent is that you probably don't want to
         *  override them in most of your calls. Let the user choose via the option menu and/or call SetColorEditOptions() once during startup. */
        val DefaultOptions: ColorEditFlags get() = Uint8 or DisplayRGB or InputRGB or PickerHueBar

        // [Internal] Masks
        val _DisplayMask: ColorEditFlags get() = DisplayRGB or DisplayHSV or DisplayHEX
        val _DataTypeMask: ColorEditFlags get() = Uint8 or Float
        val _PickerMask: ColorEditFlags get() = PickerHueWheel or PickerHueBar
        val _InputMask: ColorEditFlags get() = InputRGB or InputHSV
    }
}


/** Flags for DragFloat(), DragInt(), SliderFloat(), SliderInt() etc.
 *  We use the same sets of flags for DragXXX() and SliderXXX() functions as the features are the same and it makes it easier to swap them.
 *  (Those are per-item flags. There are shared flags in ImGuiIO: io.ConfigDragClickToInputText) */
typealias SliderFlags = Flag<SliderFlag>

sealed class SliderFlag(override val i: Int) : FlagBase<SliderFlag>() {

    /** Clamp value to min/max bounds when input manually with CTRL+Click. By default CTRL+Click allows going out of bounds. */
    object AlwaysClamp : SliderFlag(1 shl 4)

    /** Make the widget logarithmic (linear otherwise). Consider using ImGuiDragFlags_NoRoundToFormat with this if using a format-string with small amount of digits. */
    object Logarithmic : SliderFlag(1 shl 5)

    /** Disable rounding underlying value to match precision of the display format string (e.g. %.3f values are rounded to those 3 digits) */
    object NoRoundToFormat : SliderFlag(1 shl 6)

    /** Disable CTRL+Click or Enter key allowing to input text directly into the widget */
    object NoInput : SliderFlag(1 shl 7)

    /** [Private] Should this widget be orientated vertically? */
    object _Vertical : SliderFlag(1 shl 20)
    object _ReadOnly : SliderFlag(1 shl 21)

    @GenSealedEnum
    companion object
}

/** Identify a mouse button. */
enum class MouseButton {
    None, Left, Right, Middle, _unused0, _unused1;

    val i = ordinal - 1 // starts at -1

    companion object {
        val imguiValues = values().drop(1)
        val COUNT = 5
        infix fun of(i: Int): MouseButton = values()[1 + i]
    }
}


/** Enumeration for GetMouseCursor()
 *  User code may request backend to display given cursor by calling SetMouseCursor(),
 *  which is why we have some cursors that are marked unused here
 *
 *  A mouse cursor shape */
enum class MouseCursor {

    None,
    Arrow,

    /** When hovering over InputText, etc.  */
    TextInput,

    /** (Unused by Dear ImGui functions) */
    ResizeAll,

    /** When hovering over a horizontal border  */
    ResizeNS,

    /** When hovering over a vertical border or a column */
    ResizeEW,

    /** When hovering over the bottom-left corner of a window  */
    ResizeNESW,

    /** When hovering over the bottom-right corner of a window  */
    ResizeNWSE,

    /** (Unused by Dear ImGui functions. Use for e.g. hyperlinks) */
    Hand,

    /** When hovering something with disallowed interaction. Usually a crossed circle. */
    NotAllowed;

    @JvmField
    val i = ordinal - 1

    companion object {
        infix fun of(i: Int) = values().first { it.i == i }
        val COUNT = values().size - 1
    }
}

typealias CondFlags = Flag<Cond>

/** Enumeration for ImGui::SetWindow***(), SetNextWindow***(), SetNextItem***() functions
 *  Represent a condition.
 *  Important: Treat as a regular enum! Do NOT combine multiple values using binary operators!
 *  All the functions above treat 0 as a shortcut to Cond.Always.
 *
 *  Enum: A condition for many Set*() functions */
sealed class Cond : FlagBase<Cond>() {

    override val i: Int = if (ordinal == 0) 0 else 1 shl (ordinal - 1)

    /** No condition (always set the variable), same as Always */
    object None : Cond()

    /** No condition (always set the variable), same as None */
    object Always : Cond()

    /** Set the variable once per runtime session (only the first call will succeed)    */
    object Once : Cond()

    /** Set the variable if the object/window has no persistently saved data (no entry in .ini file)    */
    object FirstUseEver : Cond()

    /** Set the variable if the object/window is appearing after being hidden/inactive (or the first time) */
    object Appearing : Cond()

    @GenSealedEnum
    companion object
}