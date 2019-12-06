package imgui.classes

import glm_.*
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.api.g
import imgui.internal.*
import imgui.internal.ColumnsFlags
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.cos
import kotlin.math.sin


/** Type information associated to one ImGuiDataType. Retrieve with DataTypeGetInfo(). */
class DataTypeInfo {
    /** Size in byte */
    var size = 0
    /** Default printf format for the type */
    lateinit var printFmt: String
    /** Default scanf format for the type */
    lateinit var scanFmt: String
}

/** Stacked color modifier, backup of modified data so we can restore it    */
class ColorMod(val col: Col, value: Vec4) {
    val backupValue = Vec4(value)
}

/** Stacked style modifier, backup of modified data so we can restore it. Data type inferred from the variable. */
class StyleMod(val idx: StyleVar) {
    var ints = IntArray(2)
    val floats = FloatArray(2)
}

/* Stacked storage data for BeginGroup()/EndGroup() */
class GroupData {
    var backupCursorPos = Vec2()
    var backupCursorMaxPos = Vec2()
    var backupIndent = 0f
    var backupGroupOffset = 0f
    var backupCurrLineSize = Vec2()
    var backupCurrLineTextBaseOffset = 0f
    var backupActiveIdIsAlive = 0
    var backupActiveIdPreviousFrameIsAlive = false
    var emitItem = false
}

/** Simple column measurement, currently used for MenuItem() only.. This is very short-sighted/throw-away code and NOT a generic helper. */
class MenuColumns {

    var spacing = 0f
    var width = 0f
    var nextWidth = 0f
    val pos = FloatArray(3)
    var nextWidths = FloatArray(3)

    fun update(count: Int, spacing: Float, clear: Boolean) {
        assert(count == pos.size)
        nextWidth = 0f
        width = 0f
        this.spacing = spacing
        if (clear)
            nextWidths.fill(0f)
        for (i in pos.indices) {
            if (i > 0 && nextWidths[i] > 0f)
                width += spacing
            pos[i] = floor(width)
            width += nextWidths[i]
            nextWidths[i] = 0f
        }
    }

    fun declColumns(w0: Float, w1: Float, w2: Float): Float {
        nextWidth = 0f
        nextWidths[0] = nextWidths[0] max w0
        nextWidths[1] = nextWidths[1] max w1
        nextWidths[2] = nextWidths[2] max w2
        for (i in pos.indices)
            nextWidth += nextWidths[i] + (if (i > 0 && nextWidths[i] > 0f) spacing else 0f)
        return width max nextWidth.i.f // JVM only TODO why?
    }


    fun calcExtraSpace(availW: Float) = glm.max(0f, availW - width)
}

/** Storage for window settings stored in .ini file (we keep one of those even if the actual window wasn't instanced during this session)
 *  Windows data saved in imgui.ini file
 *  ~ CreateNewWindowSettings */
class WindowSettings(val name: String = "") {
    var id: ID = hash(name)
    var pos = Vec2()
    var size = Vec2()
    var collapsed = false
}

/* Storage for current popup stack  */
class PopupData(
        /** Set on OpenPopup()  */
        var popupId: ID = 0,
        /** Resolved on BeginPopup() - may stay unresolved if user never calls OpenPopup()  */
        var window: Window? = null,
        /** Set on OpenPopup() copy of NavWindow at the time of opening the popup  */
        var sourceWindow: Window? = null,
        /** Set on OpenPopup()  */
        var openFrameCount: Int = -1,
        /** Set on OpenPopup(), we need this to differentiate multiple menu sets from each others
         *  (e.g. inside menu bar vs loose menu items)    */
        var openParentId: ID = 0,
        /** Set on OpenPopup(), preferred popup position (typically == OpenMousePos when using mouse)   */
        var openPopupPos: Vec2 = Vec2(),
        /** Set on OpenPopup(), copy of mouse position at the time of opening popup */
        var openMousePos: Vec2 = Vec2())

/** Storage data for a single column */
class ColumnData {
    /** Column start offset, normalized 0f (far left) -> 1f (far right) */
    var offsetNorm = 0f
    var offsetNormBeforeResize = 0f
    /** Not exposed */
    var flags: ColumnsFlags = 0
    var clipRect = Rect()
}

/** Storage data for a columns set */
class Columns {
    var id: ID = 0
    var flags: ColumnsFlags = ColumnsFlag.None.i
    var isFirstFrame = false
    var isBeingResized = false
    var current = 0
    var count = 1
    /** Offsets from HostWorkRect.Min.x */
    var offMinX = 0f
    /** Offsets from HostWorkRect.Min.x */
    var offMaxX = 0f
    /** Backup of CursorPos at the time of BeginColumns() */
    var hostCursorPosY = 0f
    /** Backup of CursorMaxPos at the time of BeginColumns() */
    var hostCursorMaxPosX = 0f
    /** Backup of ClipRect at the time of BeginColumns() */
    var hostClipRect = Rect()
    /** Backup of WorkRect at the time of BeginColumns() */
    var hostWorkRect = Rect()
    var lineMinY = 0f
    var lineMaxY = 0f
    val columns = ArrayList<ColumnData>()

    fun clear() {
        id = 0
        flags = ColumnsFlag.None.i
        isFirstFrame = false
        isBeingResized = false
        current = 0
        count = 1
        offMaxX = 0f
        offMinX = 0f
        hostCursorPosY = 0f
        hostCursorMaxPosX = 0f
        lineMaxY = 0f
        lineMinY = 0f
        columns.clear()
    }

    /** ~GetColumnOffsetFromNorm    */
    infix fun getOffsetFrom(offsetNorm: Float): Float = offsetNorm * (offMaxX - offMinX)

    /** ~GetColumnNormFromOffset    */
    fun getNormFrom(offset: Float): Float = offset / (offMaxX - offMinX)
}

/** Data shared between all ImDrawList instances
 * Data shared among multiple draw lists (typically owned by parent ImGui context, but you may create one yourself) */
class DrawListSharedData {
    /** UV of white pixel in the atlas  */
    var texUvWhitePixel = Vec2()
    /** Current/default font (optional, for simplified AddText overload) */
    var font: Font? = null
    /** Current/default font size (optional, for simplified AddText overload) */
    var fontSize = 0f

    var curveTessellationTol = 0f
    /** Value for pushClipRectFullscreen() */
    var clipRectFullscreen = Vec4(-8192f, -8192f, 8192f, 8192f)
    /** Initial flags at the beginning of the frame (it is possible to alter flags on a per-drawlist basis afterwards) */
    var initialFlags = DrawListFlag.None.i

    // Const data
    // FIXME: Bake rounded corners fill/borders in atlas
    var circleVtx12 = Array(12) {
        val a = it * 2 * glm.PIf / 12
        Vec2(cos(a), sin(a))
    }
}

/** Helper to build a ImDrawData instance */
class DrawDataBuilder {
    /** Global layers for: regular, tooltip */
    val layers = Array(2) { ArrayList<DrawList>() }

    fun clear() = layers.forEach { it.clear() }

    fun flattenIntoSingleLayer() {
        val size = layers.map { it.size }.count()
        layers[0].ensureCapacity(size)
        for (layerN in 1 until layers.size) {
            val layer = layers[layerN]
            if (layer.isEmpty()) continue
            layers[0].addAll(layer)
            layer.clear()
        }
    }
}

/** Result of a directional navigation move query result */
class NavMoveResult {
    /** Best candidate  */
    var id: ID = 0
    /** Best candidate window current selectable group ID */
    var selectScopeId: ID = 0
    /** Best candidate window   */
    var window: Window? = null
    /** Best candidate box distance to current NavId    */
    var distBox = Float.MAX_VALUE
    /** Best candidate center distance to current NavId */
    var distCenter = Float.MAX_VALUE

    var distAxial = Float.MAX_VALUE
    /** Best candidate bounding box in window relative space    */
    var rectRel = Rect()

    fun clear() {
        id = 0
        window = null
        distBox = Float.MAX_VALUE
        distCenter = Float.MAX_VALUE
        distAxial = Float.MAX_VALUE
        rectRel = Rect()
    }
}

typealias NextWindowDataFlags = Int

enum class NextWindowDataFlag {
    None, HasPos, HasSize, HasContentSize, HasCollapsed, HasSizeConstraint, HasFocus, HasBgAlpha;

    val i: NextWindowDataFlags = if (ordinal == 0) 0 else 1 shl (ordinal - 1)
}

infix fun Int.has(f: NextWindowDataFlag) = and(f.i) != 0
infix fun Int.hasnt(f: NextWindowDataFlag) = and(f.i) == 0
infix fun Int.or(f: NextWindowDataFlag) = or(f.i)
infix fun Int.wo(f: NextWindowDataFlag) = and(f.i.inv())

/** Storage for SetNextWindow** functions    */
class NextWindowData {
    var flags = NextWindowDataFlag.None.i
    var posCond = Cond.None
    var sizeCond = Cond.None
    var collapsedCond = Cond.None
    val posVal = Vec2()
    val posPivotVal = Vec2()
    val sizeVal = Vec2()
    val contentSizeVal = Vec2()
    var collapsedVal = false
    /** Valid if 'SetNextWindowSizeConstraint' is true  */
    val sizeConstraintRect = Rect()
    var sizeCallback: SizeCallback? = null
    var sizeCallbackUserData: Any? = null
    var bgAlphaVal = Float.MAX_VALUE
    /** *Always on* This is not exposed publicly, so we don't clear it. */
    var menuBarOffsetMinVal = Vec2()

    fun clearFlags() {
        flags = NextWindowDataFlag.None.i
    }
}

typealias NextItemDataFlags = Int

enum class NextItemDataFlag(val i: NextItemDataFlags) {
    None(0),
    HasWidth(1 shl 0),
    HasOpen(1 shl 1)
}

infix fun Int.has(f: NextItemDataFlag) = and(f.i) != 0
infix fun Int.or(f: NextItemDataFlag) = or(f.i)
infix fun Int.wo(f: NextItemDataFlag) = and(f.i.inv())

class NextItemData {
    var flags: NextItemDataFlags = 0
    /** Set by SetNextItemWidth(). */
    var width = 0f
    /** Set by SetNextItemOpen() function. */
    var openVal = false
    var openCond = Cond.None

    fun clearFlags() {
        flags = NextItemDataFlag.None.i
    }
}

class ShrinkWidthItem(var index: Int, var width: Float)

class PtrOrIndex(
        /** Either field can be set, not both. e.g. Dock node tab bars are loose while BeginTabBar() ones are in a pool. */
        val ptr: TabBar?,
        /** Usually index in a main pool. */
        val index: PoolIdx) {

    constructor(ptr: TabBar) : this(ptr, PoolIdx(-1))

    constructor(index: PoolIdx) : this(null, index)
}

/** Temporary storage for one window(, that's the data which in theory we could ditch at the end of the frame)
 *  Transient per-window data, reset at the beginning of the frame. This used to be called ImGuiDrawContext, hence the DC variable name in ImGuiWindow.
 *  FIXME: That's theory, in practice the delimitation between Window and WindowTempData is quite tenuous and could be reconsidered.  */
class WindowTempData {

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
    /** Current tree depth. */
    var treeDepth = 0
    /** Store a copy of !g.NavIdIsAlive for TreeDepth 0..31.. Could be turned into a ImU64 if necessary. */
    var treeMayJumpToParentOnPopMask = 0
    /** ID for last item */
    var lastItemId: ID = 0
    /** Status flags for last item (see ImGuiItemStatusFlags_) */
    var lastItemStatusFlags: ItemStatusFlags = 0
    /** Interaction rect for last item    */
    var lastItemRect = Rect()
    /** End-user display rect for last item (only valid if LastItemStatusFlags & ImGuiItemStatusFlags_HasDisplayRect) */
    var lastItemDisplayRect = Rect()
    /** Current layer, 0..31 (we currently only use 0..1)   */
    var navLayerCurrent = NavLayer.Main
    /** = (1 << navLayerCurrent) used by ::itemAdd prior to clipping. */
    var navLayerCurrentMask = 1 shl NavLayer.Main
    /** Which layer have been written to (result from previous frame)   */
    var navLayerActiveMask = 0
    /** Which layer have been written to (buffer for current frame) */
    var navLayerActiveMaskNext = 0

    var navHideHighlightOneFrame = false
    /** Set when scrolling can be used (ScrollMax > 0.0f)   */
    var navHasScroll = false

    var menuBarAppending = false
    /** MenuBarOffset.x is sort of equivalent of a per-layer CursorPos.x, saved/restored as we switch to the menu bar.
     *  The only situation when MenuBarOffset.y is > 0 if when (SafeAreaPadding.y > FramePadding.y), often used on TVs. */
    var menuBarOffset = Vec2()

    val childWindows = ArrayList<Window>()
    /** Current persistent per-window storage (store e.g. tree node open/close state) */
    var stateStorage = Storage()

    var layoutType = LayoutType.Vertical

    var parentLayoutType = LayoutType.Vertical
    /** Counter for focus/tabbing system. Start at -1 and increase as assigned via FocusableItemRegister() (FIXME-NAV: Needs redesign) */
    var focusCounterAll = -1
    /** (same, but only count widgets which you can Tab through) */
    var focusCounterTab = -1


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

    val allowKeyboardFocusStack = Stack<Boolean>()

    val buttonRepeatStack = Stack<Boolean>()

    val groupStack = Stack<GroupData>()
    /** Store size of various stacks for asserting  */
    val stackSizesBackup = IntArray(6)


    /** Indentation / start position from left of window (increased by TreePush/TreePop, etc.)  */
    var indent = 0f

    var groupOffset = 0f
    /** Offset to the current column (if ColumnsCurrent > 0). FIXME: This and the above should be a stack to allow use
    cases like Tree->Column->Tree. Need revamp columns API. */
    var columnsOffset = 0f
    /** Current columns set */
    var currentColumns: Columns? = null
}


//-----------------------------------------------------------------------------
// Tab Bar, Tab Item
//-----------------------------------------------------------------------------

/** Storage for one active tab item (sizeof() 26~32 bytes) */
class TabItem {
    var id: ID = 0
    var flags: TabItemFlags = 0
    var lastFrameVisible = -1
    /** This allows us to infer an ordered list of the last activated tabs with little maintenance */
    var lastFrameSelected = -1
    /** When Window==NULL, offset to name within parent ImGuiTabBar::TabsNames */
    var nameOffset = -1
    /** Position relative to beginning of tab */
    var offset = 0f
    /** Width currently displayed */
    var width = 0f
    /** Width of actual contents, stored during BeginTabItem() call */
    var widthContents = 0f
}

/** Backup and restore just enough data to be able to use isItemHovered() on item A after another B in the same window
 *  has overwritten the data.   */
fun itemHoveredDataBackup(block: () -> Unit) {
    // backup
    var window = g.currentWindow!!
    val lastItemId = window.dc.lastItemId
    val lastItemStatusFlags = window.dc.lastItemStatusFlags
    val lastItemRect = Rect(window.dc.lastItemRect)
    val lastItemDisplayRect = Rect(window.dc.lastItemDisplayRect)

    block()

    // restore
    window = g.currentWindow!!
    window.dc.lastItemId = lastItemId
    window.dc.lastItemRect put lastItemRect
    window.dc.lastItemStatusFlags = lastItemStatusFlags
    window.dc.lastItemDisplayRect = lastItemDisplayRect
}