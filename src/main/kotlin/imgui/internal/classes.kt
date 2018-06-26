package imgui.internal

import gli_.hasnt
import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec2.Vec2bool
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.clearActiveId
import imgui.ImGui.io
import imgui.ImGui.keepAliveId
import imgui.ImGui.style
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
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

    infix fun contains(p: Vec2) = p.x >= min.x && p.y >= min.y && p.x < max.x && p.y < max.y
    infix fun contains(r: Rect) = r.min.x >= min.x && r.min.y >= min.y && r.max.x <= max.x && r.max.y <= max.y
    infix fun overlaps(r: Rect) = r.min.y < max.y && r.max.y > min.y && r.min.x < max.x && r.max.x > min.x
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

/** Stacked color modifier, backup of modified data so we can restore it    */
class ColMod(val col: Col, value: Vec4) {
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
    var backupIndentX = 0f
    var backupGroupOffsetX = 0f
    var backupCurrentLineHeight = 0f
    var backupCurrentLineTextBaseOffset = 0f
    var backupLogLinePosY = 0f
    var backupActiveIdIsAlive = false
    var advanceCursor = false
}

/** Simple column measurement, currently used for MenuItem() only.. This is very short-sighted/throw-away code and NOT a generic helper. */
class MenuColumns {

    var count = 0
    var spacing = 0f
    var width = 0f
    var nextWidth = 0f
    val pos = FloatArray(4)
    var nextWidths = FloatArray(4)

    fun update(count: Int, spacing: Float, clear: Boolean) {
        assert(count <= pos.size)
        this.count = count
        nextWidth = 0f
        width = 0f
        this.spacing = spacing
        if (clear)
            nextWidths.fill(0f)
        for (i in 0 until count) {
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
        for (i in 0 until 3)
            nextWidth += nextWidths[i] + (if (i > 0 && nextWidths[i] > 0f) spacing else 0f)
        return glm.max(width, nextWidth)
    }


    fun calcExtraSpace(availW: Float) = glm.max(0f, availW - width)
}

/** Storage for window settings stored in .ini file (we keep one of those even if the actual window wasn't instanced during this session)
 *  Windows data saved in imgui.ini file */
class WindowSettings(val name: String = "") {
    var id: ID = hash(name, 0)
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
        /** Set on OpenPopup(), we need this to differenciate multiple menu sets from each others
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
class ColumnsSet {
    var id: ID = 0
    var flags: ColumnsFlags = 0
    var isFirstFrame = false
    var isBeingResized = false
    var current = 0
    var count = 1
    var minX = 0f
    var maxX = 0f
    /** Copy of CursorPos */
    var startPosY = 0f
    /** Copy of CursorMaxPos */
    var startMaxPosX = 0f
    var lineMinY = 0f
    var lineMaxY = 0f
    val columns = ArrayList<ColumnData>()

    fun clear() {
        id = 0
        flags = 0
        isFirstFrame = false
        isBeingResized = false
        current = 0
        count = 1
        maxX = 0f
        minX = 0f
        startPosY = 0f
        startMaxPosX = 0f
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
    /** Best candidate window.idStack.last() - to compare context  */
    var parentId: ID = 0
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
        parentId = 0
        window = null
        distBox = Float.MAX_VALUE
        distCenter = Float.MAX_VALUE
        distAxial = Float.MAX_VALUE
        rectRel = Rect()
    }
}

/** Storage for SetNexWindow** functions    */
class NextWindowData {
    var posCond = Cond.Null
    var sizeCond = Cond.Null
    var contentSizeCond = Cond.Null
    var collapsedCond = Cond.Null
    var sizeConstraintCond = Cond.Null
    var focusCond = Cond.Null
    var bgAlphaCond = Cond.Null
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
        posCond = Cond.Null
        sizeCond = Cond.Null
        contentSizeCond = Cond.Null
        collapsedCond = Cond.Null
        sizeConstraintCond = Cond.Null
        focusCond = Cond.Null
        bgAlphaCond = Cond.Null
    }
}

/** Temporary storage for one, that's the data which in theory we could ditch at the end of the frame
 *  Transient per-window data, reset at the beginning of the frame. This used to be called ImGuiDrawContext, hence the DC variable name in ImGuiWindow.
 *  FIXME: That's theory, in practice the delimitation between Window and WindowTempData is quite tenuous and could be reconsidered.  */
class WindowTempData {

    var cursorPos = Vec2()

    var cursorPosPrevLine = Vec2()

    var cursorStartPos = Vec2()
    /** Used to implicitly calculate the size of our contents, always growing during the frame.
     *  Turned into window.sizeContents at the beginning of next frame   */
    var cursorMaxPos = Vec2()

    var currentLineHeight = 0f

    var currentLineTextBaseOffset = 0f

    var prevLineHeight = 0f

    var prevLineTextBaseOffset = 0f

    var logLinePosY = -1f

    var treeDepth = 0
    /** Store a copy of !g.NavIdIsAlive for TreeDepth 0..31 */
    var treeDepthMayJumpToParentOnPop = 0

    var lastItemId: ID = 0
    /** ItemStatusFlag */
    var lastItemStatusFlags: ItemStatusFlags = 0
    /** Interaction rect    */
    var lastItemRect = Rect()
    /** End-user display rect (only valid if LastItemStatusFlags & ImGuiItemStatusFlags_HasDisplayRect) */
    var lastItemDisplayRect = Rect()

    var navHideHighlightOneFrame = false
    /** Set when scrolling can be used (ScrollMax > 0.0f)   */
    var navHasScroll = false
    /** Current layer, 0..31 (we currently only use 0..1)   */
    var navLayerCurrent = 0
    /** = (1 << navLayerCurrent) used by ::itemAdd prior to clipping. */
    var navLayerCurrentMask = 1 shl 0
    /** Which layer have been written to (result from previous frame)   */
    var navLayerActiveMask = 0
    /** Which layer have been written to (buffer for current frame) */
    var navLayerActiveMaskNext = 0

    var menuBarAppending = false
    /** MenuBarOffset.x is sort of equivalent of a per-layer CursorPos.x, saved/restored as we switch to the menu bar.
     *  The only situation when MenuBarOffset.y is > 0 if when (SafeAreaPadding.y > FramePadding.y), often used on TVs. */
    var menuBarOffset = Vec2()

    val childWindows = ArrayList<Window>()

    var stateStorage = Storage()

    var layoutType = LayoutType.Vertical

    var parentLayoutType = LayoutType.Vertical


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
    var indentX = 0f

    var groupOffsetX = 0f
    /** Offset to the current column (if ColumnsCurrent > 0). FIXME: This and the above should be a stack to allow use
    cases like Tree->Column->Tree. Need revamp columns API. */
    var columnsOffsetX = 0f
    /** Current columns set */
    var columnsSet: ColumnsSet? = null
}

/** Storage for one window */
class Window(var context: Context, var name: String) {
    /** == ImHash(Name) */
    val id: ID = hash(name, 0)
    /** See enum WindowFlags */
    var flags: WindowFlags = 0

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
    /** == window->GetID("#MOVE")   */
    var moveId: ID
    /** Id of corresponding item in parent window (for child windows)   */
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

    var borderSize = 0f
    /** Set to true on Begin(), unless Collapsed  */
    var active = false

    var wasActive = false
    /** Set to true when any widget access the current window   */
    var writeAccessed = false
    /** Set when collapsing window to become only title-bar */
    var collapsed = false

    var collapseToggleWanted = false
    /** Set when items can safely be all clipped (e.g. window not visible or collapsed) */
    var skipItems = false
    /** Set during the frame where the window is appearing (or re-appearing)    */
    var appearing = false
    /** Set when the window has a close button (p_open != NULL) */
    var closeButton = false
    /** Order within immediate parent window, if we are a child window. Otherwise 0. */
    var beginOrderWithinParent = -1
    /** Order within entire imgui context. This is mostly used for debugging submission order related issues. */
    var beginOrderWithinContext = -1
    /** Number of Begin() during the current frame (generally 0 or 1, 1+ if appending via multiple Begin/End pairs) */
    var beginCount = 0
    /** ID in the popup stack when this window is used as a popup/menu (because we use generic Name/ID for recycling)   */
    var popupId: ID = 0

    var autoFitFrames = Vec2i(-1)

    var autoFitOnlyGrows = false

    var autoFitChildAxes = 0x00

    var autoPosLastDirection = Dir.None

    var hiddenFrames = 0
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

    var lastFrameActive = -1

    var itemWidthDefault = 0f

    /** Simplified columns storage for menu items   */
    val menuColumns = MenuColumns()

    var stateStorage = Storage()

    val columnsStorage = ArrayList<ColumnsSet>()
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
    /** Point to ourself or first ancestor which can be CTRL-Tabbed into.   */
    var rootWindowForTabbing: Window? = null
    /** Point to ourself or first ancestor which doesn't have the NavFlattened flag.    */
    var rootWindowForNav: Window? = null


    /** When going to the menu bar, we remember the child window we came from. (This could probably be made implicit if
     *  we kept g.Windows sorted by last focused including child window.)   */
    var navLastChildNavWindow: Window? = null
    /** Last known NavId for this window, per layer (0/1). ID-Array   */
    val navLastIds = IntArray(2)
    /** Reference rectangle, in window relative space   */
    val navRectRel = Array(2) { Rect() }

    // -----------------------------------------------------------------------------------------------------------------
    // Navigation / Focus
    // -----------------------------------------------------------------------------------------------------------------

    /** Start at -1 and increase as assigned via FocusItemRegister()    */
    var focusIdxAllCounter = -1
    /** (same, but only count widgets which you can Tab through)    */
    var focusIdxTabCounter = -1
    /** Item being requested for focus  */
    var focusIdxAllRequestCurrent = Int.MAX_VALUE
    /** Tab-able item being requested for focus */
    var focusIdxTabRequestCurrent = Int.MAX_VALUE
    /** Item being requested for focus, for next update (relies on layout to be stable between the frame pressing TAB
    and the next frame) */
    var focusIdxAllRequestNext = Int.MAX_VALUE
    /** "   */
    var focusIdxTabRequestNext = Int.MAX_VALUE

    /** calculate unique ID (hash of whole ID stack + given parameter). useful if you want to query into ImGuiStorage yourself  */
    fun getId(str: String, end: Int = 0): ID {
        val seed: ID = idStack.last()
        val id: ID = hash(str, end, seed)
        keepAliveId(id)
        return id
    }

    fun getId(ptr: Any): Int {
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

    fun setPos(pos: Vec2, cond: Cond) {
        // Test condition (NB: bit 0 is always true) and clear flags for next time
        if (cond != Cond.Null && setWindowPosAllowFlags hasnt cond)
            return
//        JVM, useless
//        assert(cond == Cond.Null || cond.isPowerOfTwo) { "Make sure the user doesn't attempt to combine multiple condition flags." }
        setWindowPosAllowFlags = setWindowPosAllowFlags wo (Cond.Once or Cond.FirstUseEver or Cond.Appearing)
        setWindowPosVal put Float.MAX_VALUE
        setWindowPosPivot put Float.MAX_VALUE

        // Set
        val oldPos = Vec2(this.pos)
        this.pos put glm.floor(pos)
        // As we happen to move the window while it is being appended to (which is a bad idea - will smear) let's at least
        // offset the cursor
        val offset = pos - oldPos
        dc.cursorPos plusAssign offset
        dc.cursorMaxPos plusAssign offset // And more importantly we need to adjust this so size calculation doesn't get affected.
    }

    fun setSize(size: Vec2, cond: Cond) {
        // Test condition (NB: bit 0 is always true) and clear flags for next time
        if (cond != Cond.Null && setWindowSizeAllowFlags hasnt cond)
            return
//        JVM, useless
//        assert(cond == Cond.Null || cond.isPowerOfTwo) { "Make sure the user doesn't attempt to combine multiple condition flags." }
        setWindowSizeAllowFlags = setWindowSizeAllowFlags and (Cond.Once or Cond.FirstUseEver or Cond.Appearing).inv()

        // Set
        if (size.x > 0f) {
            autoFitFrames.x = 0
            sizeFull.x = size.x
        } else {
            autoFitFrames.x = 2
            autoFitOnlyGrows = false
        }
        if (size.y > 0f) {
            autoFitFrames.y = 0
            sizeFull.y = size.y
        } else {
            autoFitFrames.y = 2
            autoFitOnlyGrows = false
        }
    }

    fun setCollapsed(collapsed: Boolean, cond: Cond) {
        // Test condition (NB: bit 0 is always true) and clear flags for next time
        if (cond != Cond.Null && setWindowCollapsedAllowFlags hasnt cond)
            return
        setWindowCollapsedAllowFlags = setWindowCollapsedAllowFlags and (Cond.Once or Cond.FirstUseEver or Cond.Appearing).inv()
        // Set
        this.collapsed = collapsed
    }

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

        if (g.nextWindowData.sizeConstraintCond != Cond.Null) {
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
            /*  When the window cannot fit all contents (either because of constraints, either because screen is too small):
                    we are growing the size on the other axis to compensate for expected scrollbar.
                    FIXME: Might turn bigger than DisplaySize-WindowPadding.                 */
            val sizeAutoFit = glm.clamp(sizeContents, Vec2(style.windowMinSize),
                    Vec2(glm.max(style.windowMinSize, io.displaySize - style.displaySafeAreaPadding * 2f)))
            val sizeAutoFitAfterConstraint = calcSizeAfterConstraint(sizeAutoFit)
            if (sizeAutoFitAfterConstraint.x < sizeContents.x && flags hasnt Wf.NoScrollbar && flags has Wf.HorizontalScrollbar)
                sizeAutoFit.y += style.scrollbarSize
            if (sizeAutoFitAfterConstraint.y < sizeContents.y && flags hasnt Wf.NoScrollbar)
                sizeAutoFit.x += style.scrollbarSize
            sizeAutoFit
        }
    }

    val scrollMaxX get() = max(0f, sizeContents.x - (sizeFull.x - scrollbarSizes.x))
    val scrollMaxY get() = max(0f, sizeContents.y - (sizeFull.y - scrollbarSizes.y))

    /** AddWindowToDrawData */
    infix fun addTo(outList: ArrayList<DrawList>) {
        drawList addTo outList
        dc.childWindows.filter { it.active && it.hiddenFrames == 0 }  // clipped children may have been marked not active
                .forEach { it addTo outList }
    }

    fun addToSortedBuffer() {
        g.windowsSortBuffer.add(this)
        if (active) {
            val count = dc.childWindows.size
            if (count > 1)
                dc.childWindows.sortWith(childWindowComparer)
            dc.childWindows.filter { active }.forEach { it.addToSortedBuffer() }
        }
    }

    fun addToDrawDataSelectLayer() {
        io.metricsActiveWindows++
        addTo(if (flags has Wf.Tooltip) g.drawDataBuilder.layers[1] else g.drawDataBuilder.layers[0])
    }

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

    fun bringToFront() {
        val currentFrontWindow = g.windows.last()
        if (currentFrontWindow === this || currentFrontWindow.rootWindow === this)
            return
        for (i in g.windows.size - 2 downTo 0)
            if (g.windows[i] === this) {
                g.windows.removeAt(i)
                g.windows.add(this)
                break
            }
    }

    fun bringToBack() {
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

    /** Can we focus this window with CTRL+TAB (or PadMenu + PadFocusPrev/PadFocusNext) */
    val isNavFocusable get() = active && this === rootWindowForTabbing && (flags hasnt Wf.NoNavFocus || this === g.navWindow)

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

    fun getBorderRect(borderN: Int, perpPadding: Float, thickness: Float): Rect {
        val rect = rect()
        if (thickness == 0f) rect.max minusAssign 1
        return when (borderN) {
            0 -> Rect(rect.min.x + perpPadding, rect.min.y, rect.max.x - perpPadding, rect.min.y + thickness)
            1 -> Rect(rect.max.x - thickness, rect.min.y + perpPadding, rect.max.x, rect.max.y - perpPadding)
            2 -> Rect(rect.min.x + perpPadding, rect.max.y - thickness, rect.max.x - perpPadding, rect.max.y)
            3 -> Rect(rect.min.x, rect.min.y + perpPadding, rect.min.x + thickness, rect.max.y - perpPadding)
            else -> throw Error()
        }
    }

    fun calcSizeContents() = Vec2(
            (if (sizeContentsExplicit.x != 0f) sizeContentsExplicit.x else dc.cursorMaxPos.x - pos.x + scroll.x).i.f + windowPadding.x,
            (if (sizeContentsExplicit.y != 0f) sizeContentsExplicit.y else dc.cursorMaxPos.y - pos.y + scroll.y).i.f + windowPadding.y)

    fun findOrAddColumnsSet(id: ID): ColumnsSet {
        for (c in columnsStorage)
            if (c.id == id)
                return c

        return ColumnsSet().also {
            columnsStorage += it
            it.id = id
        }
    }

    fun destroy() {
        assert(drawList === drawListInst)
    }

    fun markIniSettingsDirty() {
        if (flags hasnt Wf.NoSavedSettings)
            if (g.settingsDirtyTimer <= 0f)
                g.settingsDirtyTimer = io.iniSavingRate
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
        g.navLayer = 0
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
    if (window.flags hasnt Wf.NoBringToFrontOnFocus)
        window.bringToFront()
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