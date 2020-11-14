package imgui.internal.classes

import glm_.vec2.Vec2
import imgui.ID
import imgui.internal.sections.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/** Temporary storage for one window(, that's the data which in theory we could ditch at the end of the frame)
 *  Transient per-window data, reset at the beginning of the frame. This used to be called ImGuiDrawContext, hence the DC variable name in ImGuiWindow.
 *  FIXME: That's theory, in practice the delimitation between Window and WindowTempData is quite tenuous and could be reconsidered.  */
class WindowTempData {

    // Layout

    var cursorPos = Vec2()
    /** Current emitting position, in absolute coordinates. */
    var cursorPosPrevLine = Vec2()
    /** Initial position after Begin(), generally ~ window position + WindowPadding. */
    var cursorStartPos = Vec2()
    /** Used to implicitly calculate the size of our contents, always growing during the frame. Used to calculate window->ContentSize at the beginning of next frame */
    var cursorMaxPos = Vec2()

    var currLineSize = Vec2()

    var prevLineSize = Vec2()
    /** Baseline offset (0.0f by default on a new line, generally == style.FramePadding.y when a framed item has been added). */
    var currLineTextBaseOffset = 0f

    var prevLineTextBaseOffset = 0f
    /** Indentation / start position from left of window (increased by TreePush/TreePop, etc.)  */
    var indent = 0f
    /** Offset to the current column (if ColumnsCurrent > 0). FIXME: This and the above should be a stack to allow use
    cases like Tree->Column->Tree. Need revamp columns API. */
    var columnsOffset = 0f

    var groupOffset = 0f


    // Last item status

    /** ID for last item */
    var lastItemId: ID = 0
    /** Status flags for last item (see ImGuiItemStatusFlags_) */
    var lastItemStatusFlags = ItemStatusFlag.None.i
    /** Interaction rect for last item    */
    var lastItemRect = Rect()
    /** End-user display rect for last item (only valid if LastItemStatusFlags & ImGuiItemStatusFlags_HasDisplayRect) */
    var lastItemDisplayRect = Rect()


    // Keyboard/Gamepad navigation

    /** Current layer, 0..31 (we currently only use 0..1)   */
    var navLayerCurrent = NavLayer.Main
    /** Which layers have been written to (result from previous frame)   */
    var navLayerActiveMask = 0
    /** Which layers have been written to (accumulator for current frame) */
    var navLayerActiveMaskNext = 0x00
    /** Current focus scope ID while appending */
    var navFocusScopeIdCurrent: ID = 0

    var navHideHighlightOneFrame = false
    /** Set when scrolling can be used (ScrollMax > 0.0f)   */
    var navHasScroll = false


    // Miscellaneous

    var menuBarAppending = false
    /** MenuBarOffset.x is sort of equivalent of a per-layer CursorPos.x, saved/restored as we switch to the menu bar.
     *  The only situation when MenuBarOffset.y is > 0 if when (SafeAreaPadding.y > FramePadding.y), often used on TVs. */
    var menuBarOffset = Vec2()
    /** Simplified columns storage for menu items   */
    val menuColumns = MenuColumns()
    /** Current tree depth. */
    var treeDepth = 0
    /** Store a copy of !g.NavIdIsAlive for TreeDepth 0..31.. Could be turned into a ImU64 if necessary. */
    var treeJumpToParentOnPopMask = 0x00

    val childWindows = ArrayList<Window>()
    /** Current persistent per-window storage (store e.g. tree node open/close state) */
    var stateStorage = HashMap<ID, Boolean>()
    /** Current columns set */
    var currentColumns: Columns? = null

    var layoutType = LayoutType.Vertical

    var parentLayoutType = LayoutType.Vertical
    /** (Legacy Focus/Tabbing system) Sequential counter, start at -1 and increase as assigned via FocusableItemRegister() (FIXME-NAV: Needs redesign) */
    var focusCounterRegular = -1
    /** (Legacy Focus/Tabbing system) Same, but only count widgets which you can Tab through. */
    var focusCounterTabStop = -1


    // Local parameters stacks

    /*  We store the current settings outside of the vectors to increase memory locality (reduce cache misses).
        The vectors are rarely modified. Also it allows us to not heap allocate for short-lived windows which are not
        using those settings.   */
    /** == itemFlagsStack.last() [empty == ItemFlag.Default]   */
    var itemFlags: ItemFlags = 0
    /** == ItemWidthStack.back(). 0.0: default, >0.0: width in pixels, <0.0: align xx pixels to the right of window */
    var itemWidth = 0f
    /** == TextWrapPosStack.back() [empty == -1.0f] */
    var textWrapPos = -1f

    val itemFlagsStack = Stack<ItemFlags>()

    val itemWidthStack = Stack<Float>()

    val textWrapPosStack = Stack<Float>()

    val groupStack = Stack<GroupData>()
    /** Store size of various stacks for asserting  */
    val stackSizesBackup = IntArray(6)
}