package imgui.internal

import gli_.has
import gli_.hasnt
import glm_.*
import glm_.vec2.Vec2
import glm_.vec2.Vec2bool
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.arrowButtonEx
import imgui.ImGui.beginCombo
import imgui.ImGui.buttonBehavior
import imgui.ImGui.clearActiveId
import imgui.ImGui.endCombo
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.io
import imgui.ImGui.isItemHovered
import imgui.ImGui.isMouseClicked
import imgui.ImGui.isMouseDragging
import imgui.ImGui.isMouseReleased
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.keepAliveId
import imgui.ImGui.markIniSettingsDirty
import imgui.ImGui.popClipRect
import imgui.ImGui.popItemFlag
import imgui.ImGui.popStyleColor
import imgui.ImGui.pushClipRect
import imgui.ImGui.pushItemFlag
import imgui.ImGui.pushStyleColor
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.selectable
import imgui.ImGui.setActiveId
import imgui.ImGui.setItemAllowOverlap
import imgui.ImGui.setTooltip
import imgui.ImGui.style
import imgui.ImGui.tabItemBackground
import imgui.ImGui.tabItemCalcSize
import imgui.ImGui.tabItemLabelAndCloseButton
import imgui.imgui.Context
import imgui.imgui.g
import imgui.imgui.imgui_colums.Companion.tabBarRef
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.reflect.KMutableProperty0
import imgui.HoveredFlag as Hf
import imgui.WindowFlag as Wf


/** An axis-aligned rectangle (2 points)
 *  2D axis aligned bounding-box
 *  NB: we can't rely on ImVec2 math operators being available here */
class Rect {
    /** Upper-left  */
    var min = Vec2(Float.MAX_VALUE, Float.MAX_VALUE)
    /** Lower-right */
    var max = Vec2(-Float.MAX_VALUE, -Float.MAX_VALUE)

    constructor()

    constructor(min: Vec2i, max: Vec2i) {
        this.min put min
        this.max put max
    }

    constructor(min: Vec2i, max: Vec2) {
        this.min put min
        this.max = max
    }

    constructor(min: Vec2, max: Vec2) {
        this.min = Vec2(min)
        this.max = Vec2(max)
    }

    constructor(v: Vec4) {
        min.put(v.x, v.y)
        max.put(v.z, v.w)
    }

    constructor(r: Rect) {
        min put r.min
        max put r.max
    }

    constructor(x1: Float, y1: Float, x2: Float, y2: Float) {
        min.put(x1, y1)
        max.put(x2, y2)
    }

    val center get() = (min + max) * 0.5f
    val size get() = max - min
    val width get() = max.x - min.x
    val height get() = max.y - min.y
    /** Top-left    */
    val tl get() = min
    /** Top-right   */
    val tr get() = Vec2(max.x, min.y)
    /** Bottom-left */
    val bl get() = Vec2(min.x, max.y)
    /** Bottom-right    */
    val br get() = max

    infix operator fun contains(p: Vec2) = p.x >= min.x && p.y >= min.y && p.x < max.x && p.y < max.y
    infix operator fun contains(r: Rect) = r.min.x >= min.x && r.min.y >= min.y && r.max.x <= max.x && r.max.y <= max.y
    infix fun overlaps(r: Rect) = r.min.y < max.y && r.max.y > min.y && r.min.x < max.x && r.max.x > min.x
    infix fun overlaps(v: Vec4) = v.y < max.y && v.w > min.y && v.x < max.x && v.z > min.x
    infix fun add(p: Vec2) {
        if (min.x > p.x) min.x = p.x
        if (min.y > p.y) min.y = p.y
        if (max.x < p.x) max.x = p.x
        if (max.y < p.y) max.y = p.y
    }

    infix fun add(r: Rect) {
        if (min.x > r.min.x) min.x = r.min.x
        if (min.y > r.min.y) min.y = r.min.y
        if (max.x < r.max.x) max.x = r.max.x
        if (max.y < r.max.y) max.y = r.max.y
    }

    infix fun expand(amount: Float) {
        min.x -= amount
        min.y -= amount
        max.x += amount
        max.y += amount
    }

    infix fun expand(amount: Vec2) {
        min.x -= amount.x
        min.y -= amount.y
        max.x += amount.x
        max.y += amount.y
    }

    infix fun translate(d: Vec2) {
        min.x += d.x
        min.y += d.y
        max.x -= d.x
        max.y -= d.y
    }

    infix fun translateX(dx: Float) {
        min.x += dx
        max.x += dx
    }

    infix fun translateY(dy: Float) {
        min.y += dy
        max.y += dy
    }

    /** Simple version, may lead to an inverted rectangle, which is fine for Contains/Overlaps test but not for display. */
    infix fun clipWith(r: Rect) {
        min = min max r.min
        max = max min r.max
    }

    /** Full version, ensure both points are fully clipped. */
    infix fun clipWithFull(r: Rect) {
        min = glm.clamp(min, r.min, r.max)
        max = glm.clamp(max, r.min, r.max)
    }

    fun floor() {
        min.x = min.x.i.f
        min.y = min.y.i.f
        max.x = max.x.i.f
        max.y = max.y.i.f
    }

    val isInverted get() = min.x > max.x || min.y > max.y
    val isFinite get() = min.x != Float.MAX_VALUE

    fun put(min: Vec2, max: Vec2) {
        this.min put min
        this.max put max
    }

    fun put(x1: Float, y1: Float, x2: Float, y2: Float) {
        min.put(x1, y1)
        max.put(x2, y2)
    }

    infix fun put(vec4: Vec4) {
        min.put(vec4.x, vec4.y)
        max.put(vec4.z, vec4.w)
    }

    infix fun put(rect: Rect) {
        min put rect.min
        max put rect.max
    }

    override fun toString() = "min: $min, max: $max"
}

/** Type information associated to one ImGuiDataType. Retrieve with DataTypeGetInfo(). */
class DataTypeInfo{
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
    var backupCurrentLineSize = Vec2()
    var backupCurrentLineTextBaseOffset = 0f
    var backupActiveIdIsAlive = 0
    var backupActiveIdPreviousFrameIsAlive = false
    var advanceCursor = false
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
            pos[i] = width.i.f
            width += nextWidths[i]
            nextWidths[i] = 0f
        }
    }

    fun declColumns(w0: Float, w1: Float, w2: Float): Float {
        nextWidth = 0f
        nextWidths[0] = glm.max(nextWidths[0], w0)
        nextWidths[1] = glm.max(nextWidths[1], w1)
        nextWidths[2] = glm.max(nextWidths[2], w2)
        for (i in pos.indices)
            nextWidth += nextWidths[i] + (if (i > 0 && nextWidths[i] > 0f) spacing else 0f)
        return glm.max(width, nextWidth)
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
class PopupRef(
        /** Set on OpenPopup()  */
        var popupId: ID,
        /** Resolved on BeginPopup() - may stay unresolved if user never calls OpenPopup()  */
        var window: Window?,
        /** Set on OpenPopup()  */
        var parentWindow: Window,
        /** Set on OpenPopup()  */
        var openFrameCount: Int,
        /** Set on OpenPopup(), we need this to differentiate multiple menu sets from each others
         *  (e.g. inside menu bar vs loose menu items)    */
        var openParentId: ID,
        /** Set on OpenPopup(), preferred popup position (typically == OpenMousePos when using mouse)   */
        var openPopupPos: Vec2,
        /** Set on OpenPopup(), copy of mouse position at the time of opening popup */
        var openMousePos: Vec2)

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
    var minX = 0f
    var maxX = 0f
    /** Backup of CursorPos at the time of BeginColumns() */
    var backupCursorPosY = 0f
    /** Backup of CursorMaxPos at the time of BeginColumns() */
    var backupCursorMaxPosX = 0f
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
        maxX = 0f
        minX = 0f
        backupCursorPosY = 0f
        backupCursorMaxPosX = 0f
        lineMaxY = 0f
        lineMinY = 0f
        columns.clear()
    }
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

/** Storage for SetNexWindow** functions    */
class NextWindowData {
    var posCond = Cond.None
    var sizeCond = Cond.None
    var contentSizeCond = Cond.None
    var collapsedCond = Cond.None
    var sizeConstraintCond = Cond.None
    var focusCond = Cond.None
    var bgAlphaCond = Cond.None
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
    /** This is not exposed publicly, so we don't clear it. */
    var menuBarOffsetMinVal = Vec2()

    fun clear() {
        posCond = Cond.None
        sizeCond = Cond.None
        contentSizeCond = Cond.None
        collapsedCond = Cond.None
        sizeConstraintCond = Cond.None
        focusCond = Cond.None
        bgAlphaCond = Cond.None
    }
}

class TabBarSortItem(var index: Int, var width: Float)

class TabBarRef {
    // Either field can be set, not both. Dock node tab bars are loose while BeginTabBar() ones are in a pool.
    val ptr: TabBar?
    val indexInMainPool: PoolIdx

    constructor(ptr: TabBar) {
        this.ptr = ptr
        indexInMainPool = PoolIdx(-1)
    }

    constructor(indexInMainPool: PoolIdx) {
        ptr = null
        this.indexInMainPool = indexInMainPool
    }
}

/** Temporary storage for one window(, that's the data which in theory we could ditch at the end of the frame)
 *  Transient per-window data, reset at the beginning of the frame. This used to be called ImGuiDrawContext, hence the DC variable name in ImGuiWindow.
 *  FIXME: That's theory, in practice the delimitation between Window and WindowTempData is quite tenuous and could be reconsidered.  */
class WindowTempData {

    var cursorPos = Vec2()

    var cursorPosPrevLine = Vec2()

    var cursorStartPos = Vec2()
    /** Used to implicitly calculate the size of our contents, always growing during the frame.
     *  Turned into window.sizeContents at the beginning of next frame   */
    var cursorMaxPos = Vec2()

    var currentLineSize = Vec2()

    var currentLineTextBaseOffset = 0f

    var prevLineSize = Vec2()

    var prevLineTextBaseOffset = 0f

    var treeDepth = 0
    /** Store a copy of !g.NavIdIsAlive for TreeDepth 0..31.. Could be turned into a ImU64 if necessary. */
    var treeStoreMayJumpToParentOnPop = 0

    var lastItemId: ID = 0
    /** ItemStatusFlag */
    var lastItemStatusFlags: ItemStatusFlags = 0
    /** Interaction rect    */
    var lastItemRect = Rect()
    /** End-user display rect (only valid if LastItemStatusFlags & ImGuiItemStatusFlags_HasDisplayRect) */
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

    var nextItemWidth = Float.MAX_VALUE
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

/** Storage for one window */
class Window(var context: Context, var name: String) {
    /** == ImHash(Name) */
    val id: ID = hash(name)
    /** See enum WindowFlags */
    var flags = Wf.None.i

    /** Position (always rounded-up to nearest pixel)    */
    var pos = Vec2()
    /** Current size (==SizeFull or collapsed title bar size)   */
    var size = Vec2()
    /** Size when non collapsed */
    var sizeFull = Vec2()
    /** Copy of SizeFull at the end of Begin. This is the reference value we'll use on the next frame to decide if we need scrollbars.  */
    var sizeFullAtLastBegin = Vec2()
    /** Size of contents (== extents reach of the drawing cursor) from previous frame    */
    var sizeContents = Vec2()
    /** Size of contents explicitly set by the user via SetNextWindowContentSize()  */
    var sizeContentsExplicit = Vec2()
    /** Window padding at the time of begin. */
    var windowPadding = Vec2()
    /** Window rounding at the time of begin.   */
    var windowRounding = 0f
    /** Window border size at the time of begin.    */
    var windowBorderSize = 1f
    /** Size of buffer storing Name. May be larger than strlen(Name)! */
    var nameBufLen = name.length
    /** == window->GetID("#MOVE")   */
    var moveId: ID
    /** ID of corresponding item in parent window (for navigation to return from child window to parent window)   */
    var childId: ID = 0

    var scroll = Vec2()
    /** target scroll position. stored as cursor position with scrolling canceled out, so the highest point is always
    0.0f. (FLT_MAX for no change)   */
    var scrollTarget = Vec2(Float.MAX_VALUE)
    /** 0.0f = scroll so that target position is at top, 0.5f = scroll so that target position is centered  */
    var scrollTargetCenterRatio = Vec2(.5f)
    /** Size taken by scrollbars on each axis */
    var scrollbarSizes = Vec2()

    var scrollbar = Vec2bool()

    /** Set to true on Begin(), unless Collapsed  */
    var active = false

    var wasActive = false
    /** Set to true when any widget access the current window   */
    var writeAccessed = false
    /** Set when collapsing window to become only title-bar */
    var collapsed = false

    var wantCollapseToggle = false
    /** Set when items can safely be all clipped (e.g. window not visible or collapsed) */
    var skipItems = false
    /** Set during the frame where the window is appearing (or re-appearing)    */
    var appearing = false
    /** Do not display (== (HiddenFrames*** > 0)) */
    var hidden = false
    /** Set when the window has a close button (p_open != NULL) */
    var hasCloseButton = false
    /** Current border being held for resize (-1: none, otherwise 0-3) */
    var resizeBorderHeld = -1
    /** Number of Begin() during the current frame (generally 0 or 1, 1+ if appending via multiple Begin/End pairs) */
    var beginCount = 0
    /** Order within immediate parent window, if we are a child window. Otherwise 0. */
    var beginOrderWithinParent = -1
    /** Order within entire imgui context. This is mostly used for debugging submission order related issues. */
    var beginOrderWithinContext = -1
    /** ID in the popup stack when this window is used as a popup/menu (because we use generic Name/ID for recycling)   */
    var popupId: ID = 0

    var autoFitFrames = Vec2i(-1)

    var autoFitOnlyGrows = false

    var autoFitChildAxes = 0x00

    var autoPosLastDirection = Dir.None
    /** Hide the window for N frames */
    var hiddenFramesCanSkipItems = 0
    /** Hide the window for N frames while allowing items to be submitted so we can measure their size */
    var hiddenFramesCannotSkipItems = 0
    /** store acceptable condition flags for SetNextWindowPos() use. */
    var setWindowPosAllowFlags = Cond.Always or Cond.Once or Cond.FirstUseEver or Cond.Appearing
    /** store acceptable condition flags for SetNextWindowSize() use.    */
    var setWindowSizeAllowFlags = Cond.Always or Cond.Once or Cond.FirstUseEver or Cond.Appearing
    /** store acceptable condition flags for SetNextWindowCollapsed() use.   */
    var setWindowCollapsedAllowFlags = Cond.Always or Cond.Once or Cond.FirstUseEver or Cond.Appearing
    /** store window position when using a non-zero Pivot (position set needs to be processed when we know the window size) */
    var setWindowPosVal = Vec2(Float.MAX_VALUE)
    /** store window pivot for positioning. Vec2(0) when positioning from top-left corner; Vec2(0.5f) for centering;
     *  Vec2(1) for bottom right.   */
    var setWindowPosPivot = Vec2(Float.MAX_VALUE)


    /** Temporary per-window data, reset at the beginning of the frame. This used to be called DrawContext, hence the "DC" variable name.  */
    var dc = WindowTempData()
    /** ID stack. ID are hashes seeded with the value at the top of the stack   */
    val idStack = Stack<ID>()

    init {
        idStack += id
        moveId = getId("#MOVE")
        childId = 0
    }

    /** Current clipping rectangle. = DrawList->clip_rect_stack.back(). Scissoring / clipping rectangle. x1, y1, x2, y2. */
    var clipRect = Rect()
    /** = WindowRect just after setup in Begin(). == window->Rect() for root window. */
    var outerRectClipped = Rect()

    var innerMainRect = Rect()

    var innerClipRect = Rect()
    /** FIXME: This is currently confusing/misleading. Maximum visible content position ~~ Pos + (SizeContentsExplicit ? SizeContentsExplicit : Size - ScrollbarSizes) - CursorStartPos, per axis */
    var contentsRegionRect = Rect()
    /** Last frame number the window was Active. */
    var lastFrameActive = -1

    var itemWidthDefault = 0f

    /** Simplified columns storage for menu items   */
    val menuColumns = MenuColumns()

    var stateStorage = Storage()

    val columnsStorage = ArrayList<Columns>()
    /** Index into SettingsWindow[] (indices are always valid as we only grow the array from the back) */
    var settingsIdx = -1
    /** User scale multiplier per-window */
    var fontWindowScale = 1f

    var drawListInst: DrawList = DrawList(context.drawListSharedData).apply { _ownerName = name }
    /** == &DrawListInst (for backward compatibility reason with code using imgui_internal.h we keep this a pointer) */
    var drawList = drawListInst
    /** If we are a child _or_ popup window, this is pointing to our parent. Otherwise NULL.  */
    var parentWindow: Window? = null
    /** Point to ourself or first ancestor that is not a child window.  */
    var rootWindow: Window? = null
    /** Point to ourself or first ancestor which will display TitleBgActive color when this window is active.   */
    var rootWindowForTitleBarHighlight: Window? = null
    /** Point to ourself or first ancestor which doesn't have the NavFlattened flag.    */
    var rootWindowForNav: Window? = null


    /** When going to the menu bar, we remember the child window we came from. (This could probably be made implicit if
     *  we kept g.Windows sorted by last focused including child window.)   */
    var navLastChildNavWindow: Window? = null
    /** Last known NavId for this window, per layer (0/1). ID-Array   */
    val navLastIds = IntArray(NavLayer.COUNT)
    /** Reference rectangle, in window relative space   */
    val navRectRel = Array(NavLayer.COUNT) { Rect() }


    /** calculate unique ID (hash of whole ID stack + given parameter). useful if you want to query into ImGuiStorage yourself  */
    fun getId(str: String, end: Int = 0): ID {
        // FIXME: ImHash with str_end doesn't behave same as with identical zero-terminated string, because of ### handling.
        val seed: ID = idStack.last()
        val id: ID = hash(str, end, seed)
        keepAliveId(id)
        return id
    }

    fun getId(ptr: Any): ID {
        val ptrIndex = ++ptrIndices
        if (ptrIndex >= ptrId.size) {
            val newBufLength = ptrId.size + 512
            val newBuf = Array(newBufLength) { it }
            System.arraycopy(ptrId, 0, newBuf, 0, ptrId.size)
            ptrId = newBuf
        }
        val id: ID = System.identityHashCode(ptrId[ptrIndex])
        keepAliveId(id)
        return id
    }

    fun getIdNoKeepAlive(str: String, strEnd: Int = str.length): ID {
        val seed: ID = idStack.last()
        return hash(str, str.length - strEnd, seed)
    }

    fun getIdNoKeepAlive(ptr: Any): ID {
        val ptrIndex = ++ptrIndices
        if (ptrIndex >= ptrId.size) {
            val newBufLength = ptrId.size + 512
            val newBuf = Array(newBufLength) { it }
            System.arraycopy(ptrId, 0, newBuf, 0, ptrId.size)
            ptrId = newBuf
        }
        return System.identityHashCode(ptrId[ptrIndex])
    }

    /** This is only used in rare/specific situations to manufacture an ID out of nowhere. */
    fun getIdFromRectangle(rAbs: Rect): ID {
        val seed: ID = idStack.last()
        val rRel = intArrayOf((rAbs.min.x - pos.x).i, (rAbs.min.y - pos.y).i, (rAbs.max.x - pos.x).i, (rAbs.max.y - pos.y).i)
        return hash(rRel, seed).also { keepAliveId(it) } // id
    }

    /** We don't use g.FontSize because the window may be != g.CurrentWidow. */
    fun rect() = Rect(pos.x.f, pos.y.f, pos.x + size.x, pos.y + size.y)

    fun calcFontSize() = g.fontBaseSize * fontWindowScale
    val titleBarHeight
        get() = when {
            flags has Wf.NoTitleBar -> 0f
            else -> calcFontSize() + style.framePadding.y * 2f
        }

    fun titleBarRect() = Rect(pos, Vec2(pos.x + sizeFull.x, pos.y + titleBarHeight))
    val menuBarHeight
        get() = when {
            flags has Wf.MenuBar -> dc.menuBarOffset.y + calcFontSize() + style.framePadding.y * 2f
            else -> 0f
        }

    fun menuBarRect(): Rect {
        val y1 = pos.y + titleBarHeight
        return Rect(pos.x.f, y1, pos.x + sizeFull.x, y1 + menuBarHeight)
    }

    // _______________________________________ JVM _______________________________________

    /** JVM Specific, for the deconstructor    */
    fun clear() {
        drawList.clear()
        name = ""
    }

    fun setPos(pos: Vec2, cond: Cond) {
        // Test condition (NB: bit 0 is always true) and clear flags for next time
        if (cond != Cond.None && setWindowPosAllowFlags hasnt cond)
            return
//        JVM, useless
//        assert(cond == Cond.None || cond.isPowerOfTwo) { "Make sure the user doesn't attempt to combine multiple condition flags." }
        setWindowPosAllowFlags = setWindowPosAllowFlags wo (Cond.Once or Cond.FirstUseEver or Cond.Appearing)
        setWindowPosVal put Float.MAX_VALUE
        setWindowPosPivot put Float.MAX_VALUE

        // Set
        val oldPos = Vec2(this.pos)
        this.pos put floor(pos)
        // As we happen to move the window while it is being appended to (which is a bad idea - will smear) let's at least
        // offset the cursor
        val offset = pos - oldPos
        dc.cursorPos plusAssign offset
        dc.cursorMaxPos plusAssign offset // And more importantly we need to adjust this so size calculation doesn't get affected.
    }

    fun setSize(size: Vec2, cond: Cond) {
        // Test condition (NB: bit 0 is always true) and clear flags for next time
        if (cond != Cond.None && setWindowSizeAllowFlags hasnt cond)
            return
//        JVM, useless
//        assert(cond == Cond.None || cond.isPowerOfTwo) { "Make sure the user doesn't attempt to combine multiple condition flags." }
        setWindowSizeAllowFlags = setWindowSizeAllowFlags and (Cond.Once or Cond.FirstUseEver or Cond.Appearing).inv()

        // Set
        if (size.x > 0f) {
            autoFitFrames.x = 0
            sizeFull.x = floor(size.x)
        } else {
            autoFitFrames.x = 2
            autoFitOnlyGrows = false
        }
        if (size.y > 0f) {
            autoFitFrames.y = 0
            sizeFull.y = floor(size.y)
        } else {
            autoFitFrames.y = 2
            autoFitOnlyGrows = false
        }
    }

    fun setCollapsed(collapsed: Boolean, cond: Cond) {
        // Test condition (NB: bit 0 is always true) and clear flags for next time
        if (cond != Cond.None && setWindowCollapsedAllowFlags hasnt cond)
            return
        setWindowCollapsedAllowFlags = setWindowCollapsedAllowFlags and (Cond.Once or Cond.FirstUseEver or Cond.Appearing).inv()
        // Set
        this.collapsed = collapsed
    }

    infix fun isContentHoverable(flag: Hf) = isContentHoverable(flag.i)

    /** ~IsWindowContentHoverable */
    infix fun isContentHoverable(flags: HoveredFlags): Boolean {
        // An active popup disable hovering on other windows (apart from its own children)
        // FIXME-OPT: This could be cached/stored within the window.
        val focusedRootWindow = g.navWindow?.rootWindow ?: return true
        if (focusedRootWindow.wasActive && focusedRootWindow !== rootWindow) {
            /*  For the purpose of those flags we differentiate "standard popup" from "modal popup"
                NB: The order of those two tests is important because Modal windows are also Popups.             */
            if (focusedRootWindow.flags has Wf.Modal)
                return false
            if (focusedRootWindow.flags has Wf.Popup && flags hasnt Hf.AllowWhenBlockedByPopup)
                return false
        }
        return true
    }

    fun calcSizeAfterConstraint(newSize: Vec2): Vec2 {

        if (g.nextWindowData.sizeConstraintCond != Cond.None) {
            // Using -1,-1 on either X/Y axis to preserve the current size.
            val cr = g.nextWindowData.sizeConstraintRect
            newSize.x = if (cr.min.x >= 0 && cr.max.x >= 0) glm.clamp(newSize.x, cr.min.x, cr.max.x) else sizeFull.x
            newSize.y = if (cr.min.y >= 0 && cr.max.y >= 0) glm.clamp(newSize.y, cr.min.y, cr.max.y) else sizeFull.y
            g.nextWindowData.sizeCallback?.invoke(SizeCallbackData(
                    userData = g.nextWindowData.sizeCallbackUserData,
                    pos = Vec2(this@Window.pos),
                    currentSize = sizeFull,
                    desiredSize = newSize))
        }

        // Minimum size
        if (flags hasnt (Wf.ChildWindow or Wf.AlwaysAutoResize)) {
            newSize maxAssign style.windowMinSize
            // Reduce artifacts with very small windows
            newSize.y = max(newSize.y, titleBarHeight + menuBarHeight + max(0f, style.windowRounding - 1f))
        }
        return newSize
    }

    fun calcSizeAutoFit(sizeContents: Vec2) = when {
        flags has Wf.Tooltip -> Vec2(sizeContents) // Tooltip always resize
        else -> {
            // Maximum window size is determined by the display size
            val isPopup = flags has Wf.Popup
            val isMenu = flags has Wf.ChildMenu
            val sizeMin = Vec2(style.windowMinSize)
            // Popups and menus bypass style.WindowMinSize by default, but we give then a non-zero minimum size to facilitate understanding problematic cases (e.g. empty popups)
            if (isPopup || isMenu)
                sizeMin minAssign 4f
            val sizeAutoFit = glm.clamp(sizeContents, sizeMin, glm.max(sizeMin, Vec2(io.displaySize) - style.displaySafeAreaPadding * 2f))

            // When the window cannot fit all contents (either because of constraints, either because screen is too small),
            // we are growing the size on the other axis to compensate for expected scrollbar.
            // FIXME: Might turn bigger than ViewportSize-WindowPadding.
            val sizeAutoFitAfterConstraint = calcSizeAfterConstraint(sizeAutoFit)
            if (sizeAutoFitAfterConstraint.x < sizeContents.x && flags hasnt Wf.NoScrollbar && flags has Wf.HorizontalScrollbar)
                sizeAutoFit.y += style.scrollbarSize
            if (sizeAutoFitAfterConstraint.y < sizeContents.y && flags hasnt Wf.NoScrollbar)
                sizeAutoFit.x += style.scrollbarSize
            sizeAutoFit
        }
    }

    /** ~GetWindowScrollMaxX */
    val scrollMaxX: Float
        get() = max(0f, sizeContents.x - (sizeFull.x - scrollbarSizes.x))
    /** ~GetWindowScrollMaxY */
    val scrollMaxY: Float
        get() = max(0f, sizeContents.y - (sizeFull.y - scrollbarSizes.y))

    /** AddWindowToDrawData */
    infix fun addToDrawData(outList: ArrayList<DrawList>) {
        io.metricsRenderWindows++
        drawList addTo outList
        dc.childWindows.filter { it.isActiveAndVisible }  // clipped children may have been marked not active
                .forEach { it addToDrawData outList }
    }

    /** AddWindowToSortedBuffer */
    infix fun addToSortBuffer(sortedWindows: ArrayList<Window>) {
        sortedWindows += this
        if (active) {
            val count = dc.childWindows.size
            if (count > 1)
                dc.childWindows.sortWith(childWindowComparer)
            dc.childWindows.filter { it.active }.forEach { it addToSortBuffer sortedWindows }
        }
    }

    /** Layer is locked for the root window, however child windows may use a different viewport (e.g. extruding menu) */
    fun addRootWindowToDrawData() = addToDrawData(if (flags has Wf.Tooltip) g.drawDataBuilder.layers[1] else g.drawDataBuilder.layers[0])

    // FIXME: Add a more explicit sort order in the window structure.
    private val childWindowComparer = compareBy<Window>({ it.flags has Wf.Popup }, { it.flags has Wf.Tooltip }, { it.beginOrderWithinParent })

    fun setConditionAllowFlags(flags: Int, enabled: Boolean) = if (enabled) {
        setWindowPosAllowFlags = setWindowPosAllowFlags or flags
        setWindowSizeAllowFlags = setWindowSizeAllowFlags or flags
        setWindowCollapsedAllowFlags = setWindowCollapsedAllowFlags or flags
    } else {
        setWindowPosAllowFlags = setWindowPosAllowFlags wo flags
        setWindowSizeAllowFlags = setWindowSizeAllowFlags wo flags
        setWindowCollapsedAllowFlags = setWindowCollapsedAllowFlags wo flags
    }

    fun bringToFocusFront() {
        if (g.windowsFocusOrder.last() === this)
            return
        for (i in g.windowsFocusOrder.size - 2 downTo 0) // We can ignore the front most window
            if (g.windowsFocusOrder[i] === this) {
                g.windowsFocusOrder.removeAt(i)
                g.windowsFocusOrder += this
                break
            }
    }

    /** ~BringWindowToDisplayFront */
    fun bringToDisplayFront() {
        val currentFrontWindow = g.windows.last()
        if (currentFrontWindow === this || currentFrontWindow.rootWindow === this)
            return
        for (i in g.windows.size - 2 downTo 0) // We can ignore the front most window
            if (g.windows[i] === this) {
                g.windows.removeAt(i)
                g.windows += this
                break
            }
    }

    /** ~ BringWindowToDisplayBack */
    fun bringToDisplayBack() {
        if (g.windows[0] === this) return
        for (i in 0 until g.windows.size)
            if (g.windows[i] === this) {
                g.windows.removeAt(i)
                g.windows.add(0, this)
            }
    }

    infix fun isChildOf(potentialParent: Window?): Boolean {
        if (rootWindow === potentialParent) return true
        var window: Window? = this
        while (window != null) {
            if (window === potentialParent) return true
            window = window.parentWindow
        }
        return false
    }

    /** Can we focus this window with CTRL+TAB (or PadMenu + PadFocusPrev/PadFocusNext)
     *  Note that NoNavFocus makes the window not reachable with CTRL+TAB but it can still be focused with mouse or programmaticaly.
     *  If you want a window to never be focused, you may use the e.g. NoInputs flag.
     *  ~ IsWindowNavFocusable */
    val isNavFocusable: Boolean
        get() = active && this === rootWindow && flags hasnt Wf.NoNavFocus

    /** SetWindowScrollX */
    fun setScrollX(newScrollX: Float) {
        dc.cursorMaxPos.x += scroll.x // SizeContents is generally computed based on CursorMaxPos which is affected by scroll position, so we need to apply our change to it.
        scroll.x = newScrollX
        dc.cursorMaxPos.x -= scroll.x
    }

    /** SetWindowScrollY */
    fun setScrollY(newScrollY: Float) {
        /*  SizeContents is generally computed based on CursorMaxPos which is affected by scroll position, so we need
            to apply our change to it.         */
        dc.cursorMaxPos.y += scroll.y
        scroll.y = newScrollY
        dc.cursorMaxPos.y -= scroll.y
    }

    fun getAllowedExtentRect(): Rect {
        val padding = style.displaySafeAreaPadding
        return viewportRect.apply {
            expand(Vec2(if (width > padding.x * 2) -padding.x else 0f, if (height > padding.y * 2) -padding.y else 0f))
        }
    }

    fun calcResizePosSizeFromAnyCorner(cornerTarget: Vec2, cornerNorm: Vec2, outPos: Vec2, outSize: Vec2) {
        val posMin = cornerTarget.lerp(pos, cornerNorm)             // Expected window upper-left
        val posMax = (size + pos).lerp(cornerTarget, cornerNorm)    // Expected window lower-right
        val sizeExpected = posMax - posMin
        val sizeConstrained = calcSizeAfterConstraint(sizeExpected)
        outPos put posMin
        if (cornerNorm.x == 0f) outPos.x -= (sizeConstrained.x - sizeExpected.x)
        if (cornerNorm.y == 0f) outPos.y -= (sizeConstrained.y - sizeExpected.y)
        outSize put sizeConstrained
    }

    fun getResizeBorderRect(borderN: Int, perpPadding: Float, thickness: Float): Rect {
        val rect = rect()
        if (thickness == 0f) rect.max minusAssign 1
        return when (borderN) {
            0 -> Rect(rect.min.x + perpPadding, rect.min.y - thickness, rect.max.x - perpPadding, rect.min.y + thickness)   // Top
            1 -> Rect(rect.max.x - thickness, rect.min.y + perpPadding, rect.max.x + thickness, rect.max.y - perpPadding)   // Right
            2 -> Rect(rect.min.x + perpPadding, rect.max.y - thickness, rect.max.x - perpPadding, rect.max.y + thickness)   // Bottom
            3 -> Rect(rect.min.x - thickness, rect.min.y + perpPadding, rect.min.x + thickness, rect.max.y - perpPadding)   // Left
            else -> throw Error()
        }
    }

    fun calcSizeContents() = when {
        collapsed && autoFitFrames allLessThanEqual 0 -> Vec2(sizeContents)
        hidden && hiddenFramesCannotSkipItems == 0 && hiddenFramesCanSkipItems > 0 -> Vec2(sizeContents)
        else -> Vec2(
                (if (sizeContentsExplicit.x != 0f) sizeContentsExplicit.x else dc.cursorMaxPos.x - pos.x + scroll.x).i.f + windowPadding.x,
                (if (sizeContentsExplicit.y != 0f) sizeContentsExplicit.y else dc.cursorMaxPos.y - pos.y + scroll.y).i.f + windowPadding.y)
    }

    fun findOrCreateColumns(id: ID): Columns {

        // We have few columns per window so for now we don't need bother much with turning this into a faster lookup.
        for (c in columnsStorage)
            if (c.id == id)
                return c

        return Columns().also {
            columnsStorage += it
            it.id = id
        }
    }

    fun destroy() {
        assert(drawList === drawListInst)
    }


    // Settings


    fun markIniSettingsDirty() {
        if (flags hasnt Wf.NoSavedSettings)
            if (g.settingsDirtyTimer <= 0f)
                g.settingsDirtyTimer = io.iniSavingRate
    }

    /** Window has already passed the IsWindowNavFocusable()
     *  ~ getFallbackWindowNameForWindowingList */
    val fallbackWindowName: String
        get() = when {
            flags has Wf.Popup -> "(Popup)"
            flags has Wf.MenuBar && name == "##MainMenuBar" -> "(Main menu bar)"
            else -> "(Untitled)"
        }

    /** ~ IsWindowActiveAndVisible */
    val isActiveAndVisible: Boolean get() = active && !hidden

    /** ~ StartMouseMovingWindow */
    fun startMouseMoving() {
        /*  Set ActiveId even if the _NoMove flag is set. Without it, dragging away from a window with _NoMove would
            activate hover on other windows.
            We _also_ call this when clicking in a window empty space when io.ConfigWindowsMoveFromTitleBarOnly is set,
            but clear g.MovingWindow afterward.
            This is because we want ActiveId to be set even when the window is not permitted to move.   */
        focus()
        setActiveId(moveId, this)
        g.navDisableHighlight = true
        g.activeIdClickOffset = io.mousePos - rootWindow!!.pos

        val canMoveWindow = flags hasnt Wf.NoMove && rootWindow!!.flags hasnt Wf.NoMove
        if (canMoveWindow)
            g.movingWindow = this
    }

    /** ~UpdateWindowParentAndRootLinks */
    fun updateParentAndRootLinks(flags: WindowFlags, parentWindow: Window?) {
        this.parentWindow = parentWindow
        rootWindow = this
        rootWindowForTitleBarHighlight = this
        rootWindowForNav = this
        parentWindow?.let {
            if (flags has Wf.ChildWindow && flags hasnt Wf.Tooltip)
                rootWindow = it.rootWindow
            if (flags hasnt Wf.Modal && flags has (Wf.ChildWindow or Wf.Popup))
                rootWindowForTitleBarHighlight = it.rootWindowForTitleBarHighlight
        }
        while (rootWindowForNav!!.flags has Wf.NavFlattened)
            rootWindowForNav = rootWindowForNav!!.parentWindow!! // ~assert
    }

    fun calcExpectedSize(): Vec2 {
        val sizeContents = calcSizeContents()
        return calcSizeAfterConstraint(calcSizeAutoFit(sizeContents))
    }
}

fun Window?.setCurrent() {
    g.currentWindow = this
    this?.let {
        g.drawListSharedData.fontSize = calcFontSize()
        g.fontSize = g.drawListSharedData.fontSize
    }
}

/** Moving window to front of display (which happens to be back of our sorted list) */
fun Window?.focus() {

    if (g.navWindow !== this) {
        g.navWindow = this
        if (this != null && g.navDisableMouseHover)
            g.navMousePosDirty = true
        g.navInitRequest = false
        g.navId = this?.navLastIds?.get(0) ?: 0 // Restore NavId
        g.navIdIsAlive = false
        g.navLayer = NavLayer.Main
        //IMGUI_DEBUG_LOG("FocusWindow(\"%s\")\n", window ? window->Name : NULL);
    }

    // Passing NULL allow to disable keyboard focus
    if (this == null) return

    var window = this
    // Move the root window to the top of the pile
    if (rootWindow != null) window = rootWindow!!

    // Steal focus on active widgets
    if (window.flags has Wf.Popup) // FIXME: This statement should be unnecessary. Need further testing before removing it..
        if (g.activeId != 0 && g.activeIdWindow != null && g.activeIdWindow!!.rootWindow != window)
            clearActiveId()

    // Bring to front
    bringToFocusFront()
    if (window.flags hasnt Wf.NoBringToFrontOnFocus)
        window.bringToDisplayFront()
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

/** Storage for a tab bar (sizeof() 92~96 bytes) */
class TabBar {
    val tabs = ArrayList<TabItem>()
    /** Zero for tab-bars used by docking */
    var id: ID = 0
    /** Selected tab */
    var selectedTabId: ID = 0
    var nextSelectedTabId: ID = 0
    /** Can occasionally be != SelectedTabId (e.g. when previewing contents for CTRL+TAB preview) */
    var visibleTabId: ID = 0
    var currFrameVisible = -1
    var prevFrameVisible = -1
    var barRect = Rect()
    var contentsHeight = 0f
    /** Distance from BarRect.Min.x, locked during layout */
    var offsetMax = 0f
    /** Distance from BarRect.Min.x, incremented with each BeginTabItem() call, not used if ImGuiTabBarFlags_Reorderable if set. */
    var offsetNextTab = 0f
    var scrollingAnim = 0f
    var scrollingTarget = 0f
    var scrollingTargetDistToVisibility = 0f
    var scrollingSpeed = 0f
    var flags: TabBarFlags = TabBarFlag.None.i
    var reorderRequestTabId: ID = 0
    var reorderRequestDir = 0
    var wantLayout = false
    var visibleTabWasSubmitted = false
    /** For BeginTabItem()/EndTabItem() */
    var lastTabItemIdx = -1
    /** style.FramePadding locked at the time of BeginTabBar() */
    var framePadding = Vec2()
    /** For non-docking tab bar we re-append names in a contiguous buffer. */
    val tabsNames = ArrayList<String>()

    val TabItem.order get() = tabs.indexOf(this)

    val TabItem.name: String
        get() {
            assert(nameOffset in tabsNames.indices)
            return tabsNames[nameOffset]
        }

    // Tab Bars

    /** ~ beginTabBarEx */
    fun beginEx(bb: Rect, flags__: TabBarFlags): Boolean {

        val window = g.currentWindow!!
        if (window.skipItems) return false

        var flags_ = flags__
        if (flags_ hasnt TabBarFlag.DockNode)
            window.idStack += id

        // Add to stack
        g.currentTabBarStack += tabBarRef
        g.currentTabBar = this
        if (currFrameVisible == g.frameCount) {
            //printf("[%05d] BeginTabBarEx already called this frame\n", g.FrameCount);
            assert(false)
            return true
        }

        // When toggling back from ordered to manually-reorderable, shuffle tabs to enforce the last visible order.
        // Otherwise, the most recently inserted tabs would move at the end of visible list which can be a little too confusing or magic for the user.
        if (flags_ has TabBarFlag.Reorderable && flags_ hasnt TabBarFlag.Reorderable && tabs.isNotEmpty() && prevFrameVisible != -1)
            tabs.sortBy(TabItem::offset)

        // Flags
        if (flags_ hasnt TabBarFlag.FittingPolicyMask_)
            flags_ = flags_ or TabBarFlag.FittingPolicyDefault_

        flags = flags_
        barRect = bb
        wantLayout = true // Layout will be done on the first call to ItemTab()
        prevFrameVisible = currFrameVisible
        currFrameVisible = g.frameCount

        // Layout
        itemSize(Vec2(offsetMax, barRect.height))
        window.dc.cursorPos.x = barRect.min.x

        // Draw separator
        val col = if (flags has TabBarFlag.IsFocused) Col.TabActive else Col.Tab
        val y = barRect.max.y - 1f
        run {
            val separatorMinX = barRect.min.x - window.windowPadding.x
            val separatorMaxX = barRect.max.x + window.windowPadding.x
            window.drawList.addLine(Vec2(separatorMinX, y), Vec2(separatorMaxX, y), col.u32, 1f)
        }
        return true
    }

    fun tabItemEx(label: String, pOpen: KMutableProperty0<Boolean>?, flags_: TabItemFlags): Boolean {

        // Layout whole tab bar if not already done
        if (wantLayout)
            layout()

        val window = g.currentWindow!!
        if (window.skipItems) return false

        val id = calcTabID(label)

        // If the user called us with *p_open == false, we early out and don't render. We make a dummy call to ItemAdd() so that attempts to use a contextual popup menu with an implicit ID won't use an older ID.
        if (pOpen?.get() == false) {
            pushItemFlag(ItemFlag.NoNav or ItemFlag.NoNavDefaultFocus, true)
            itemAdd(Rect(), id)
            popItemFlag()
            return false
        }

        // Calculate tab contents size
        val size = tabItemCalcSize(label, pOpen != null)

        // Acquire tab data
        var tabIsNew = false
        val tab = findTabByID(id) ?: TabItem().also {
            it.id = id
            it.width = size.x
            tabs += it
            tabIsNew = true
        }
        lastTabItemIdx = tabs.indexOf(tab)
        tab.widthContents = size.x

        if (pOpen == null)
            flags = flags or TabItemFlag.NoCloseButton

        val tabBarAppearing = prevFrameVisible + 1 < g.frameCount
        val tabBarFocused = flags has TabBarFlag.IsFocused
        val tabAppearing = tab.lastFrameVisible + 1 < g.frameCount
        tab.lastFrameVisible = g.frameCount
        tab.flags = flags_

        // Append name with zero-terminator
        tab.nameOffset = tabsNames.size
        tabsNames += label

        // If we are not reorderable, always reset offset based on submission order.
        // (We already handled layout and sizing using the previous known order, but sizing is not affected by order!)
        if (!tabAppearing && flags hasnt TabBarFlag.Reorderable) {
            tab.offset = offsetNextTab
            offsetNextTab += tab.width + style.itemInnerSpacing.x
        }

        // Update selected tab
        if (tabAppearing && flags has TabBarFlag.AutoSelectNewTabs && nextSelectedTabId == 0)
            if (!tabBarAppearing || selectedTabId == 0)
                nextSelectedTabId = id  // New tabs gets activated
        if (flags has TabItemFlag.SetSelected && selectedTabId != id) // SetSelected can only be passed on explicit tab bar
            nextSelectedTabId = id

        // Lock visibility
        var tabContentsVisible = visibleTabId == id
        if (tabContentsVisible)
            visibleTabWasSubmitted = true

        // On the very first frame of a tab bar we let first tab contents be visible to minimize appearing glitches
        if (!tabContentsVisible && selectedTabId == 0 && tabBarAppearing)
            if (tabs.size == 1 && flags hasnt TabBarFlag.AutoSelectNewTabs)
                tabContentsVisible = true

        if (tabAppearing && !tabBarAppearing || tabIsNew) {
            pushItemFlag(ItemFlag.NoNav or ItemFlag.NoNavDefaultFocus, true)
            itemAdd(Rect(), id)
            popItemFlag()
            return tabContentsVisible
        }

        if (selectedTabId == id)
            tab.lastFrameSelected = g.frameCount

        // Backup current layout position
        val backupMainCursorPos = Vec2(window.dc.cursorPos)

        // Layout
        size.x = tab.width
        window.dc.cursorPos = barRect.min + Vec2(tab.offset.i.f - scrollingAnim, 0f)
        val pos = Vec2(window.dc.cursorPos)
        val bb = Rect(pos, pos + size)

        // We don't have CPU clipping primitives to clip the CloseButton (until it becomes a texture), so need to add an extra draw call (temporary in the case of vertical animation)
        val wantClipRect = bb.min.x < barRect.min.x || bb.max.x >= barRect.max.x
        if (wantClipRect)
            pushClipRect(Vec2(bb.min.x max barRect.min.x, bb.min.y - 1), Vec2(barRect.max.x, bb.max.y), true)

        itemSize(bb, style.framePadding.y)
        if (!itemAdd(bb, id)) {
            if (wantClipRect)
                popClipRect()
            window.dc.cursorPos = backupMainCursorPos
            return tabContentsVisible
        }

        // Click to Select a tab
        var buttonFlags = ButtonFlag.PressedOnClick or ButtonFlag.AllowItemOverlap
        if (g.dragDropActive)
            buttonFlags = buttonFlags or ButtonFlag.PressedOnDragDropHold
        val (pressed, hovered_, held) = buttonBehavior(bb, id, buttonFlags)
        if (pressed)
            nextSelectedTabId = id
        val hovered = hovered_ || g.hoveredId == id

        // Allow the close button to overlap unless we are dragging (in which case we don't want any overlapping tabs to be hovered)
        if (!held)
            setItemAllowOverlap()

        // Drag and drop: re-order tabs
        if (held && !tabAppearing && isMouseDragging(0))
            if (!g.dragDropActive && flags has TabBarFlag.Reorderable)
            // While moving a tab it will jump on the other side of the mouse, so we also test for MouseDelta.x
                if (io.mouseDelta.x < 0f && io.mousePos.x < bb.min.x) {
//                    if (flags_ has TabBarFlag.Reorderable)
                    queueChangeTabOrder(tab, -1)
                } else if (io.mouseDelta.x > 0f && io.mousePos.x > bb.max.x)
//                    if (flags_ has TabBarFlag.Reorderable)
                    queueChangeTabOrder(tab, +1)

//        if (false)
//            if (hovered && g.hoveredIdNotActiveTimer > 0.5f && bb.width < tab.widthContents)        {
//                // Enlarge tab display when hovering
//                bb.max.x = bb.min.x + lerp (bb.width, tab.widthContents, saturate((g.hoveredIdNotActiveTimer-0.4f) * 6f)).i.f
//                displayDrawList = GetOverlayDrawList(window)
//                TabItemBackground(display_draw_list, bb, flags_, GetColorU32(ImGuiCol_TitleBgActive))
//            }

        // Render tab shape
        val displayDrawList = window.drawList
        val tabCol = when {
            held || hovered -> Col.TabHovered
            else -> when {
                tabContentsVisible -> when {
                    tabBarFocused -> Col.TabActive
                    else -> Col.TabUnfocusedActive
                }
                else -> when {
                    tabBarFocused -> Col.Tab
                    else -> Col.TabUnfocused
                }
            }
        }
        tabItemBackground(displayDrawList, bb, flags_, tabCol.u32)
        renderNavHighlight(bb, id)

        // Select with right mouse button. This is so the common idiom for context menu automatically highlight the current widget.
        val hoveredUnblocked = isItemHovered(Hf.AllowWhenBlockedByPopup)
        if (hoveredUnblocked && (isMouseClicked(1) || isMouseReleased(1)))
            nextSelectedTabId = id

        val flags__ = when {
            flags_ has TabBarFlag.NoCloseWithMiddleMouseButton -> flags_ or TabItemFlag.NoCloseWithMiddleMouseButton
            else -> flags_
        }

        // Render tab label, process close button
        val closeButtonId = if (pOpen?.get() == true) window.getId(id + 1) else 0
        val justClosed = tabItemLabelAndCloseButton(displayDrawList, bb, flags__, framePadding, label, id, closeButtonId)
        if (justClosed && pOpen != null) {
            pOpen.set(false)
            closeTab(tab)
        }

        // Restore main window position so user can draw there
        if (wantClipRect)
            popClipRect()
        window.dc.cursorPos = backupMainCursorPos

        // Tooltip (FIXME: Won't work over the close button because ItemOverlap systems messes up with HoveredIdTimer)
        // We test IsItemHovered() to discard e.g. when another item is active or drag and drop over the tab bar (which g.HoveredId ignores)
        if (g.hoveredId == id && !held && g.hoveredIdNotActiveTimer > 0.50f && isItemHovered())
            if (flags hasnt TabBarFlag.NoTooltip)
                setTooltip(label.substring(0, findRenderedTextEnd(label)))

        return tabContentsVisible
    }

    /** This is called only once a frame before by the first call to ItemTab()
     *  The reason we're not calling it in BeginTabBar() is to leave a chance to the user to call the SetTabItemClosed() functions.
     *  ~ TabBarLayout */
    fun layout() {

        wantLayout = false

        // Garbage collect
        var tabDstN = 0
        for (tabSrcN in tabs.indices) {
            val tab = tabs[tabSrcN]
            if (tab.lastFrameVisible < prevFrameVisible) {
                if (tab.id == selectedTabId)
                    selectedTabId = 0
                continue
            }
            if (tabDstN != tabSrcN)
                tabs[tabDstN] = tabs[tabSrcN]
            tabDstN++
        }
        if (tabs.size != tabDstN)
            for (i in tabDstN until tabs.size)
                tabs.remove(tabs.last())

        // Setup next selected tab
        var scrollTrackSelectedTabID: ID = 0
        if (nextSelectedTabId != 0) {
            selectedTabId = nextSelectedTabId
            nextSelectedTabId = 0
            scrollTrackSelectedTabID = selectedTabId
        }

        // Process order change request (we could probably process it when requested but it's just saner to do it in a single spot).
        if (reorderRequestTabId != 0) {
            findTabByID(reorderRequestTabId)?.let { tab1 ->
                //IM_ASSERT(tab_bar->Flags & ImGuiTabBarFlags_Reorderable); // <- this may happen when using debug tools
                val tab2_order = tab1.order + reorderRequestDir
                if (tab2_order in tabs.indices) {
                    val tab2 = tabs[tab2_order]
                    val itemTmp = tab1
                    tabs[tab1.order] = tab2 // *tab1 = *tab2
                    tabs[tab2_order] = itemTmp // *tab2 = itemTmp
                    if (tab2.id == selectedTabId)
                        scrollTrackSelectedTabID = tab2.id
                }
                if (flags has TabBarFlag.SaveSettings)
                    markIniSettingsDirty()
            }
            reorderRequestTabId = 0
        }

        // Tab List Popup
        val tabListPopupButton = flags has TabBarFlag.TabListPopupButton
        if (tabListPopupButton)
            tabListPopupButton()?.let { tabToSelect ->
                // NB: Will alter BarRect.Max.x!
                selectedTabId = tabToSelect.id
                scrollTrackSelectedTabID = tabToSelect.id
            }

        val widthSortBuffer = g.tabSortByWidthBuffer

        // Compute ideal widths
        var widthTotalContents = 0f
        var mostRecentlySelectedTab: TabItem? = null
        var foundSelectedTabID = false
        for (tabN in tabs.indices) {
            val tab = tabs[tabN]
            assert(tab.lastFrameVisible >= prevFrameVisible)

            if (mostRecentlySelectedTab == null || mostRecentlySelectedTab.lastFrameSelected < tab.lastFrameSelected)
                mostRecentlySelectedTab = tab
            if (tab.id == selectedTabId)
                foundSelectedTabID = true

            // Refresh tab width immediately, otherwise changes of style e.g. style.FramePadding.x would noticeably lag in the tab bar.
            // Additionally, when using TabBarAddTab() to manipulate tab bar order we occasionally insert new tabs that don't have a width yet,
            // and we cannot wait for the next BeginTabItem() call. We cannot compute this width within TabBarAddTab() because font size depends on the active window.
            tab.widthContents = tabItemCalcSize(tab.name, tab.flags hasnt TabItemFlag.NoCloseButton).x

            widthTotalContents += (if (tabN > 0) style.itemInnerSpacing.x else 0f) + tab.widthContents

            // Store data so we can build an array sorted by width if we need to shrink tabs down
            widthSortBuffer += TabBarSortItem(tabN, tab.widthContents)
        }

        // Compute width
        val widthAvail = barRect.width
        var widthExcess = if (widthAvail < widthTotalContents) widthTotalContents - widthAvail else 0f
        if (widthExcess > 0f && flags has TabBarFlag.FittingPolicyResizeDown) {
            // If we don't have enough room, resize down the largest tabs first
            if (tabs.size > 1)
                widthSortBuffer.sortWith(compareBy(TabBarSortItem::width, TabBarSortItem::index))
            var tabCountSameWidth = 1
            while (widthExcess > 0f && tabCountSameWidth < tabs.size) {
                while (tabCountSameWidth < tabs.size && widthSortBuffer[0].width == widthSortBuffer[tabCountSameWidth].width)
                    tabCountSameWidth++
                val widthToRemovePerTabMax = when {
                    tabCountSameWidth < tabs.size -> widthSortBuffer[0].width - widthSortBuffer[tabCountSameWidth].width
                    else -> widthSortBuffer[0].width - 1f
                }
                val widthToRemovePerTab = (widthExcess / tabCountSameWidth) min widthToRemovePerTabMax
                for (tabN in 0 until tabCountSameWidth)
                    widthSortBuffer[tabN].width -= widthToRemovePerTab
                widthExcess -= widthToRemovePerTab * tabCountSameWidth
            }
            for (tabN in tabs.indices)
                tabs[widthSortBuffer[tabN].index].width = widthSortBuffer[tabN].width.i.f
        } else {
            val tabMaxWidth = calcMaxTabWidth()
            for (tab in tabs)
                tab.width = tab.widthContents min tabMaxWidth
        }

        // Layout all active tabs
        var offsetX = 0f
        for (tab in tabs) {
            tab.offset = offsetX
            if (scrollTrackSelectedTabID == 0 && g.navJustMovedToId == tab.id)
                scrollTrackSelectedTabID = tab.id
            offsetX += tab.width + style.itemInnerSpacing.x
        }
        offsetMax = (offsetX - style.itemInnerSpacing.x) max 0f
        offsetNextTab = 0f

        // Horizontal scrolling buttons
        val scrollingButtons = offsetMax > barRect.width && tabs.size > 1 && flags hasnt TabBarFlag.NoTabListScrollingButtons && flags has TabBarFlag.FittingPolicyScroll
        if (scrollingButtons)
            scrollingButtons()?.let { tabToSelect ->
                // NB: Will alter BarRect.Max.x!
                selectedTabId = tabToSelect.id
                scrollTrackSelectedTabID = selectedTabId
            }

        // If we have lost the selected tab, select the next most recently active one
        if (!foundSelectedTabID)
            selectedTabId = 0
        if (selectedTabId == 0 && nextSelectedTabId == 0)
            mostRecentlySelectedTab?.let {
                selectedTabId = it.id
                scrollTrackSelectedTabID = selectedTabId
            }

        // Lock in visible tab
        visibleTabId = selectedTabId
        visibleTabWasSubmitted = false

        // Update scrolling
        if (scrollTrackSelectedTabID != 0)
            findTabByID(scrollTrackSelectedTabID)?.let(::scrollToTab)
        scrollingAnim = scrollClamp(scrollingAnim)
        scrollingTarget = scrollClamp(scrollingTarget)
        if (scrollingAnim != scrollingTarget) {
            // Scrolling speed adjust itself so we can always reach our target in 1/3 seconds.
            // Teleport if we are aiming far off the visible line
            scrollingSpeed = scrollingSpeed max (70f * g.fontSize)
            scrollingSpeed = scrollingSpeed max (abs(scrollingTarget - scrollingAnim) / 0.3f)
            val teleport = prevFrameVisible + 1 < g.frameCount || scrollingTargetDistToVisibility > 10f * g.fontSize
            scrollingAnim = if (teleport) scrollingTarget else linearSweep(scrollingAnim, scrollingTarget, io.deltaTime * scrollingSpeed)
        } else scrollingSpeed = 0f

        // Clear name buffers
        if (flags hasnt TabBarFlag.DockNode)
            tabsNames.clear()
    }

    /** Dockables uses Name/ID in the global namespace. Non-dockable items use the ID stack.
     *  ~ TabBarCalcTabID   */
    infix fun calcTabID(label: String): Int {
        return when {
            flags has TabBarFlag.DockNode -> {
                val id = hash(label)
                keepAliveId(id)
                id
            }
            else -> g.currentWindow!!.getId(label)
        }
    }

    fun scrollClamp(scrolling_: Float): Float {
        val scrolling = scrolling_ min (offsetMax - barRect.width)
        return scrolling max 0f
    }

    fun scrollToTab(tab: TabItem) {

        val margin = g.fontSize * 1f // When to scroll to make Tab N+1 visible always make a bit of N visible to suggest more scrolling area (since we don't have a scrollbar)
        val order = tab.order
        val tabX1 = tab.offset + if (order > 0) -margin else 0f
        val tabX2 = tab.offset + tab.width + if (order + 1 < tabs.size) margin else 1f
        scrollingTargetDistToVisibility = 0f
        if (scrollingTarget > tabX1) {
            scrollingTargetDistToVisibility = (scrollingAnim - tabX2) max 0f
            scrollingTarget = tabX1
        } else if (scrollingTarget < tabX2 - barRect.width) {
            scrollingTargetDistToVisibility = ((tabX1 - barRect.width) - scrollingAnim) max 0f
            scrollingTarget = tabX2 - barRect.width
        }
    }

    /** ~ TabBarScrollingButtons */
    fun scrollingButtons(): TabItem? {

        val window = g.currentWindow!!

        val arrowButtonSize = Vec2(g.fontSize - 2f, g.fontSize + style.framePadding.y * 2f)
        val scrollingButtonsWidth = arrowButtonSize.x * 2f

        val backupCursorPos = Vec2(window.dc.cursorPos)
        //window->DrawList->AddRect(ImVec2(tab_bar->BarRect.Max.x - scrolling_buttons_width, tab_bar->BarRect.Min.y), ImVec2(tab_bar->BarRect.Max.x, tab_bar->BarRect.Max.y), IM_COL32(255,0,0,255));

        val availBarRect = Rect(barRect)
        val wantClipRect = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(scrollingButtonsWidth, 0f)) !in availBarRect
        if (wantClipRect)
            pushClipRect(barRect.min, barRect.max + Vec2(style.itemInnerSpacing.x, 0f), true)

        var tabToSelect: TabItem? = null

        var selectDir = 0
        val arrowCol = Vec4(style.colors[Col.Text])
        arrowCol.w *= 0.5f

        pushStyleColor(Col.Text, arrowCol)
        pushStyleColor(Col.Button, Vec4(0f))
        val backupRepeatDelay = io.keyRepeatDelay
        val backupRepeatRate = io.keyRepeatRate
        io.keyRepeatDelay = 0.25f
        io.keyRepeatRate = 0.20f
        window.dc.cursorPos.put(barRect.max.x - scrollingButtonsWidth, barRect.min.y)
        if (arrowButtonEx("##<", Dir.Left, arrowButtonSize, ButtonFlag.PressedOnClick or ButtonFlag.Repeat))
            selectDir = -1
        window.dc.cursorPos.put(barRect.max.x - scrollingButtonsWidth + arrowButtonSize.x, barRect.min.y)
        if (arrowButtonEx("##>", Dir.Right, arrowButtonSize, ButtonFlag.PressedOnClick or ButtonFlag.Repeat))
            selectDir = +1
        popStyleColor(2)
        io.keyRepeatRate = backupRepeatRate
        io.keyRepeatDelay = backupRepeatDelay

        if (wantClipRect)
            popClipRect()

        if (selectDir != 0)
            findTabByID(selectedTabId)?.let { tabItem ->
                val selectedOrder = tabItem.order
                val targetOrder = selectedOrder + selectDir
                tabToSelect = tabs[if (targetOrder in tabs.indices) targetOrder else selectedOrder] // If we are at the end of the list, still scroll to make our tab visible
            }
        window.dc.cursorPos put backupCursorPos
        barRect.max.x -= scrollingButtonsWidth + 1f

        return tabToSelect
    }

    /** ~TabBarTabListPopupButton */
    fun tabListPopupButton(): TabItem? {

        val window = g.currentWindow!!

        // We use g.Style.FramePadding.y to match the square ArrowButton size
        val tabListPopupButtonWidth = g.fontSize + style.framePadding.y * 2f
        val backupCursorPos = Vec2(window.dc.cursorPos)
        window.dc.cursorPos.put(barRect.min.x - style.framePadding.y, barRect.min.y)
        barRect.min.x += tabListPopupButtonWidth

        val arrowCol = Vec4(style.colors[Col.Text])
        arrowCol.w *= 0.5f
        pushStyleColor(Col.Text, arrowCol)
        pushStyleColor(Col.Button, Vec4())
        val open = beginCombo("##v", null, ComboFlag.NoPreview.i)
        popStyleColor(2)

        var tabToSelect: TabItem? = null
        if (open) {
            tabs.forEach { tab ->
                if (selectable(tab.name, selectedTabId == id))
                    tabToSelect = tab
            }
            endCombo()
        }

        window.dc.cursorPos = backupCursorPos
        return tabToSelect
    }


    fun findTabByID(tabId: ID): TabItem? = when (tabId) {
        0 -> null
        else -> tabs.find { it.id == tabId }
    }

    /** The *TabId fields be already set by the docking system _before_ the actual TabItem was created, so we clear them regardless.
     *  ~ tabBarRemoveTab     */
    infix fun removeTab(tabId: ID) {
        findTabByID(tabId)?.let(tabs::remove)
        if (visibleTabId == tabId) visibleTabId = 0
        if (selectedTabId == tabId) selectedTabId = 0
        if (nextSelectedTabId == tabId) nextSelectedTabId = 0
    }

    /** Called on manual closure attempt
     *  ~ tabBarCloseTab     */
    fun closeTab(tab: TabItem) {
        if (visibleTabId == tab.id && tab.flags hasnt TabItemFlag.UnsavedDocument) {
            // This will remove a frame of lag for selecting another tab on closure.
            // However we don't run it in the case where the 'Unsaved' flag is set, so user gets a chance to fully undo the closure
            tab.lastFrameVisible = -1
            nextSelectedTabId = 0
            selectedTabId = 0
        } else if (visibleTabId != tab.id && tab.flags has TabItemFlag.UnsavedDocument)
        // Actually select before expecting closure
            nextSelectedTabId = tab.id
    }

    /** ~ tabBarQueueChangeTabOrder */
    fun queueChangeTabOrder(tab: TabItem, dir: Int) {
        assert(dir == -1 || dir == +1)
        assert(reorderRequestTabId == 0)
        reorderRequestTabId = tab.id
        reorderRequestDir = dir
    }

    companion object {
        fun calcMaxTabWidth() = g.fontSize * 20f
    }
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

fun focusPreviousWindowIgnoringOne(ignoreWindow: Window?) {
    for (i in g.windowsFocusOrder.lastIndex downTo 0) {
        // We may later decide to test for different NoXXXInputs based on the active navigation input (mouse vs nav) but that may feel more confusing to the user.
        val window = g.windowsFocusOrder[i]
        if (window !== ignoreWindow && window.wasActive && window.flags hasnt Wf.ChildWindow)
            if ((window.flags and (Wf.NoMouseInputs or Wf.NoNavInputs)) != (Wf.NoMouseInputs or Wf.NoNavInputs)) {
                navRestoreLastChildNavWindow(window).focus()
                return
            }
    }
}