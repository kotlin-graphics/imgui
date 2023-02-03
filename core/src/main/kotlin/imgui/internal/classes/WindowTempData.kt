package imgui.internal.classes

import glm_.vec2.Vec2
import imgui.ID
import imgui.internal.sections.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/** Temporary storage for one window(, that's the data which in theory we could ditch at the end of the frame, in practice we currently keep it for each window)
 *
 *  Transient per-window data, reset at the beginning of the frame. This used to be called ImGuiDrawContext, hence the DC variable name in ImGuiWindow.
 *  (That's theory, in practice the delimitation between ImGuiWindow and ImGuiWindowTempData is quite tenuous and could be reconsidered..)
 *  (This doesn't need a constructor because we zero-clear it as part of ImGuiWindow and all frame-temporary data are setup on Begin) */
class WindowTempData {

    // Layout

    var cursorPos = Vec2()

    /** Current emitting position, in absolute coordinates. */
    var cursorPosPrevLine = Vec2()

    /** Initial position after Begin(), generally ~ window position + WindowPadding. */
    var cursorStartPos = Vec2()

    /** Used to implicitly calculate ContentSize at the beginning of next frame, for scrolling range and auto-resize. Always growing during the frame. */
    var cursorMaxPos = Vec2()

    /** Used to implicitly calculate ContentSizeIdeal at the beginning of next frame, for auto-resize only. Always growing during the frame. */
    val idealMaxPos = Vec2()

    var currLineSize = Vec2()

    var prevLineSize = Vec2()

    /** Baseline offset (0.0f by default on a new line, generally == style.FramePadding.y when a framed item has been added). */
    var currLineTextBaseOffset = 0f

    var prevLineTextBaseOffset = 0f

    var isSameLine = false

    var isSetPos = false

    /** Indentation / start position from left of window (increased by TreePush/TreePop, etc.)  */
    var indent = 0f

    /** Offset to the current column (if ColumnsCurrent > 0). FIXME: This and the above should be a stack to allow use
    cases like Tree->Column->Tree. Need revamp columns API. */
    var columnsOffset = 0f

    var groupOffset = 0f

    /** Record the loss of precision of CursorStartPos due to really large scrolling amount. This is used by clipper to compensentate and fix the most common use case of large scroll area. */
    val cursorStartPosLossyness = Vec2()

    // Keyboard/Gamepad navigation

    /** Current layer, 0..31 (we currently only use 0..1)   */
    var navLayerCurrent = NavLayer.Main

    /** Which layers have been written to (result from previous frame)   */
    var navLayersActiveMask = 0

    /** Which layers have been written to (accumulator for current frame) */
    var navLayersActiveMaskNext = 0x00

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
    var currentColumns: OldColumns? = null

    /** Current table index (into g.Tables) */
    var currentTableIdx = 0

    var layoutType = LayoutType.Horizontal

    var parentLayoutType = LayoutType.Horizontal


    // Local parameters stacks

    /** Current item width (>0.0: width in pixels, <0.0: align xx pixels to the right of window). */
    var itemWidth = 0f

    /** Current text wrap pos. */
    var textWrapPos = 0f

    /** Store item widths to restore (attention: .back() is not == ItemWidth) */
    val itemWidthStack = Stack<Float>()

    /** Store text wrap pos to restore (attention: .back() is not == TextWrapPos) */
    val textWrapPosStack = Stack<Float>()
}