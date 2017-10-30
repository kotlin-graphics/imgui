package imgui.internal

import gli.hasnt
import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec2.Vec2bool
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import imgui.*
import imgui.Context.style
import imgui.ImGui.keepAliveId
import java.util.*
import kotlin.collections.ArrayList
import imgui.Context as g
import imgui.WindowFlags as Wf


/** 2D axis aligned bounding-box
NB: we can't rely on ImVec2 math operators being available here */
class Rect {
    /** Upper-left  */
    var min = Vec2(Float.MAX_VALUE, Float.MAX_VALUE)
    /** Lower-right */
    var max = Vec2(-Float.MAX_VALUE, -Float.MAX_VALUE)

    constructor()

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
    infix fun contains(r: Rect) = r.min.x >= min.x && r.min.y >= min.y && r.max.x < max.x && r.max.y < max.y
    infix fun overlaps(r: Rect) = r.min.y < max.y && r.max.y > min.y && r.min.x < max.x && r.max.x > min.x
    infix fun add(rhs: Vec2) {
        if (min.x > rhs.x) min.x = rhs.x
        if (min.y > rhs.y) min.y = rhs.y
        if (max.x < rhs.x) max.x = rhs.x
        if (max.y < rhs.y) max.y = rhs.y
    }

    infix fun add(rhs: Rect) {
        if (min.x > rhs.min.x) min.x = rhs.min.x
        if (min.y > rhs.min.y) min.y = rhs.min.y
        if (max.x < rhs.max.x) max.x = rhs.max.x
        if (max.y < rhs.max.y) max.y = rhs.max.y
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

    infix fun translate(v: Vec2) {
        min.x += v.x
        min.y += v.y
        max.x -= v.x
        max.y -= v.y
    }

    infix fun clipWith(clip: Rect) {
        if (min.x < clip.min.x) min.x = clip.min.x
        if (min.y < clip.min.y) min.y = clip.min.y
        if (max.x > clip.max.x) max.x = clip.max.x
        if (max.y > clip.max.y) max.y = clip.max.y
    }

    fun floor() {
        min.x = min.x.i.f
        min.y = min.y.i.f
        max.x = max.x.i.f
        max.y = max.y.i.f
    }

    fun getClosestPoint(p: Vec2, onEdge: Boolean): Vec2 {
        if (!onEdge && contains(p))
            return p
        if (p.x > max.x) p.x = max.x
        else if (p.x < min.x) p.x = min.x
        if (p.y > max.y) p.y = max.y
        else if (p.y < min.y) p.y = min.y
        return p
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

/* Stacked data for BeginGroup()/EndGroup() */
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

// Per column data for Columns()
class ColumnData {
    /** Column start offset, normalized 0.0 (far left) -> 1.0 (far right)   */
    var offsetNorm = 0f
    val clipRect = Rect()
    //float     IndentX;
}

// Simple column measurement currently used for MenuItem() only. This is very short-sighted/throw-away code and NOT a generic helper.
class SimpleColumns {

    var count = 0
    var spacing = 0f
    var width = 0f
    var nextWidth = 0f
    val pos = FloatArray(8)
    var nextWidths = FloatArray(8)

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

// Data saved in imgui.ini file
class IniData {
    var name = ""
    var id = 0
    var pos = Vec2i()
    var size = Vec2()
    var collapsed = false
}

// Mouse cursor data (used when io.MouseDrawCursor is set)
class MouseCursorData {
    var type = MouseCursor.None
    var hotOffset = Vec2()
    var size = Vec2()
    val texUvMin = Array(2, { Vec2() })
    val texUvMax = Array(2, { Vec2() })
}

/* Storage for current popup stack  */
class PopupRef(
        /** Set on OpenPopup()  */
        var popupId: Int,
        /** Set on OpenPopup()  */
        var parentWindow: Window,
        /** Set on OpenPopup()  */
        var parentMenuSet: Int,
        /** Copy of mouse position at the time of opening popup */
        var mousePosOnOpen: Vec2
) {
    /** Resolved on BeginPopup() - may stay unresolved if user never calls OpenPopup()  */
    var window: Window? = null
}

/** Transient per-window data, reset at the beginning of the frame
FIXME: That's theory, in practice the delimitation between ImGuiWindow and ImGuiDrawContext is quite tenuous and
could be reconsidered.  */
class DrawContext {

    var cursorPos = Vec2()

    var cursorPosPrevLine = Vec2()

    var cursorStartPos = Vec2()
    /** Implicitly calculate the size of our contents, always extending. Saved into window->SizeContents at the end of
    the frame   */
    var cursorMaxPos = Vec2()

    var currentLineHeight = 0f

    var currentLineTextBaseOffset = 0f

    var prevLineHeight = 0f

    var prevLineTextBaseOffset = 0f

    var logLinePosY = -1f

    var treeDepth = 0

    var lastItemId = 0

    var lastItemRect = Rect()

    var lastItemRectHoveredRect = false

    var menuBarAppending = false

    var menuBarOffsetX = 0f

    val childWindows = ArrayList<Window>()

    var stateStorage = Storage()

    var layoutType = LayoutType.Vertical


    /*  We store the current settings outside of the vectors to increase memory locality (reduce cache misses).
        The vectors are rarely modified. Also it allows us to not heap allocate for short-lived windows which are not
        using those settings.   */
    /** == itemFlagsStack.last() [empty == ItemFlags.Default]   */
    var itemFlags = 0
    /** == ItemWidthStack.back(). 0.0: default, >0.0: width in pixels, <0.0: align xx pixels to the right of window */
    var itemWidth = 0f
    /** == TextWrapPosStack.back() [empty == -1.0f] */
    var textWrapPos = -1f

    val itemFlagsStack = Stack<Int>()

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

    var columnsCurrent = 0

    var columnsCount = 1

    var columnsMinX = 0f

    var columnsMaxX = 0f

    var columnsStartPosY = 0f
    /** Backup of CursorMaxPos  */
    var columnsStartMaxPosX = 0f

    var columnsCellMinY = 0f

    var columnsCellMaxY = 0f

    var columnsFlags = 0

    var columnsSetId = 0

    val columnsData = ArrayList<ColumnData>()
}

/** Windows data    */
class Window(
        var name: String
) {
    /** == ImHash(Name) */
    val id = hash(name, 0)
    /** See enum ImGuiWindowFlags_  */
    var flags = 0
    /** Order within immediate parent window, if we are a child window. Otherwise 0.    */
    var orderWithinParent = 0

    var posF = Vec2()
    /** Position rounded-up to nearest pixel    */
    var pos = Vec2i()
    /** Current size (==SizeFull or collapsed title bar size)   */
    var size = Vec2()
    /** Size when non collapsed */
    var sizeFull = Vec2()
    /** Size of contents (== extents reach of the drawing cursor) from previous frame    */
    var sizeContents = Vec2()
    /** Size of contents explicitly set by the user via SetNextWindowContentSize()  */
    var sizeContentsExplicit = Vec2()
    /** Maximum visible content position in window coordinates.
    ~~ (SizeContentsExplicit ? SizeContentsExplicit : Size - ScrollbarSizes) - CursorStartPos, per axis */
    var contentsRegionRect = Rect()
    /** Window padding at the time of begin. We need to lock it, in particular manipulation of the ShowBorder would have
    effect  */
    var windowPadding = Vec2()
    /** == window->GetID("#MOVE")   */
    var moveId: Int

    var scroll = Vec2()
    /** target scroll position. stored as cursor position with scrolling canceled out, so the highest point is always
    0.0f. (FLT_MAX for no change)   */
    var scrollTarget = Vec2(Float.MAX_VALUE)
    /** 0.0f = scroll so that target position is at top, 0.5f = scroll so that target position is centered  */
    var scrollTargetCenterRatio = Vec2(.5f)

    var scrollbar = Vec2bool()

    var scrollbarSizes = Vec2()

    var borderSize = 0f
    /** Set to true on Begin()  */
    var active = false

    var wasActive = false
    /** Set to true when any widget access the current window   */
    var accessed = false
    /** Set when collapsing window to become only title-bar */
    var collapsed = false
    /** Set when items can safely be all clipped (e.g. window not visible or collapsed) */
    var skipItems = false
    /** Set during the frame where the window is appearing (or re-appearing)    */
    var appearing = false
    /** Number of Begin() during the current frame (generally 0 or 1, 1+ if appending via multiple Begin/End pairs) */
    var beginCount = 0
    /** ID in the popup stack when this window is used as a popup/menu (because we use generic Name/ID for recycling)   */
    var popupId = 0

    var autoFitFrames = Vec2i(-1)

    var autoFitOnlyGrows = false

    var autoFitChildAxes = 0x00

    var autoPosLastDirection = -1

    var hiddenFrames = 0
    /** store condition flags for next SetWindowPos() call. */
    var setWindowPosAllowFlags = Cond.Always or Cond.Once or Cond.FirstUseEver or Cond.Appearing
    /** store condition flags for next SetWindowSize() call.    */
    var setWindowSizeAllowFlags = Cond.Always or Cond.Once or Cond.FirstUseEver or Cond.Appearing
    /** store condition flags for next SetWindowCollapsed() call.   */
    var setWindowCollapsedAllowFlags = Cond.Always or Cond.Once or Cond.FirstUseEver or Cond.Appearing
    /** store window position when using a non-zero Pivot (position set needs to be processed when we know the window size) */
    var setWindowPosVal = Vec2()
    /** store window pivot for positioning. Vec2(0) when positioning from top-left corner; Vec2(0.5f) for centering;
     *  Vec2(1) for bottom right.   */
    var setWindowPosPivot = Vec2()


    /** Temporary per-window data, reset at the beginning of the frame  */
    var dc = DrawContext()
    /** ID stack. ID are hashes seeded with the value at the top of the stack   */
    val idStack = Stack<Int>()

    init {
        idStack.add(id)
        moveId = getId("#MOVE")
    }

    /** = DrawList->clip_rect_stack.back(). Scissoring / clipping rectangle. x1, y1, x2, y2.    */
    var clipRect = Rect()
    /** = WindowRect just after setup in Begin(). == window->Rect() for root window.    */
    var windowRectClipped = Rect()

    var lastFrameActive = -1

    var itemWidthDefault = 0f

    /** Simplified columns storage for menu items   */
    val menuColumns = SimpleColumns()

    var stateStorage = Storage()
    /** Scale multiplier per-window */
    var fontWindowScale = 1f

    var drawList = DrawList().apply { _ownerName = name }
    /** Immediate parent in the window stack *regardless* of whether this window is a child window or not)  */
    var parentWindow: Window? = null
    /** Generally point to ourself. If we are a child window, this is pointing to the first non-child parent window.    */
    lateinit var rootWindow: Window
    /** Generally point to ourself. Used to display TitleBgActive color and for selecting which window to use for NavWindowing  */
    var rootNonPopupWindow: Window? = null

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
    fun getId(str: String, end: Int = 0): Int {
        val seed = idStack.last()
        val id = hash(str, end, seed)
        keepAliveId(id)
        return id
    }

    fun getId(ptr: Any): Int {
        val seed = idStack.last()
        val id = System.identityHashCode(++ptrIndices)
        keepAliveId(id)
        return id
    }

    fun getIdNoKeepAlive(str: String, strEnd: Int = str.length): Int {
        val seed = idStack.last()
        return hash(str, str.length - strEnd, seed)
    }

    fun rect() = Rect(pos.x.f, pos.y.f, pos.x + size.x, pos.y + size.y)
    fun calcFontSize() = g.fontBaseSize * fontWindowScale
    fun titleBarHeight() = if (flags has Wf.NoTitleBar) 0f else calcFontSize() + style.framePadding.y * 2f
    fun titleBarRect() = Rect(pos, Vec2(pos.x + sizeFull.x, pos.y + titleBarHeight()))
    fun menuBarHeight() = if (flags has Wf.MenuBar) calcFontSize() + style.framePadding.y * 2f else 0f
    fun menuBarRect(): Rect {
        val y1 = pos.y + titleBarHeight()
        return Rect(pos.x.f, y1, pos.x + sizeFull.x, y1 + menuBarHeight())
    }

    // _______________________________________ JVM _______________________________________

    /** JVM Specific, for the deconstructor    */
    fun clear() {
        drawList.clear()
        name = ""
    }

    fun setCurrent() {
        g.currentWindow = this
        g.fontSize = calcFontSize()
    }

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
        setWindowPosAllowFlags = setWindowPosAllowFlags and (Cond.Once or Cond.FirstUseEver or Cond.Appearing).inv()
        setWindowPosVal put Float.MAX_VALUE
        setWindowPosPivot put Float.MAX_VALUE

        // Set
        val oldPos = Vec2(pos)
        posF put pos // TODO glm .f on vec
        pos put pos
        // As we happen to move the window while it is being appended to (which is a bad idea - will smear) let's at least
        // offset the cursor
        dc.cursorPos plus_ (pos - oldPos)
        dc.cursorMaxPos plus (pos - oldPos) // And more importantly we need to adjust this so size calculation doesn't get affected.
    }

    fun setSize(size: Vec2, cond: Cond) {
        // Test condition (NB: bit 0 is always true) and clear flags for next time
        if (cond != Cond.Null && setWindowSizeAllowFlags hasnt cond)
            return
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

    val isContentHoverable: Boolean
        get() {
            // An active popup disable hovering on other windows (apart from its own children)
            // FIXME-OPT: This could be cached/stored within the window.
            val focusedRootWindow = g.navWindow?.rootWindow ?: return true
            return !(focusedRootWindow.flags has Wf.Popup && focusedRootWindow.wasActive && focusedRootWindow !== rootWindow)
        }

    fun calcSizeFullWithConstraint(newSize: Vec2): Vec2 {

        if (g.setNextWindowSizeConstraint) {
            // Using -1,-1 on either X/Y axis to preserve the current size.
            val cr = g.setNextWindowSizeConstraintRect
            newSize.x = if (cr.min.x >= 0 && cr.max.x >= 0) glm.clamp(newSize.x, cr.min.x, cr.max.x) else sizeFull.x
            newSize.y = if (cr.min.y >= 0 && cr.max.y >= 0) glm.clamp(newSize.y, cr.min.y, cr.max.y) else sizeFull.y
            g.setNextWindowSizeConstraintCallback?.let {
                it(g.setNextWindowSizeConstraintCallbackUserData, pos, sizeFull, newSize)
            }
        }
        if (flags hasnt (Wf.ChildWindow or Wf.AlwaysAutoResize))
            newSize max_ style.windowMinSize
        return newSize
    }

    fun calcSizeAutoFit() = if (flags has Wf.Tooltip)
    // Tooltip always resize. We keep the spacing symmetric on both axises for aesthetic purpose.
        sizeContents + windowPadding - Vec2(0f, style.itemSpacing.y)
    else {
        // Handling case of auto fit window not fitting on the screen (on either axis): we are growing the size on the other axis to compensate for expected scrollbar. FIXME: Might turn bigger than DisplaySize-WindowPadding.
        val sizeAutoFit = glm.clamp(sizeContents + windowPadding, Vec2(style.windowMinSize),
                Vec2(glm.max(style.windowMinSize, IO.displaySize - style.displaySafeAreaPadding)))
        val sizeAutoFitAfterConstraint = calcSizeFullWithConstraint(sizeAutoFit)
        if (sizeAutoFitAfterConstraint.x < sizeContents.x && flags hasnt Wf.NoScrollbar && flags has Wf.HorizontalScrollbar)
            sizeAutoFit.y += style.scrollbarSize
        if (sizeAutoFitAfterConstraint.y < sizeContents.y && flags hasnt Wf.NoScrollbar)
            sizeAutoFit.x += style.scrollbarSize * 2f
        sizeAutoFit.y = glm.max(sizeAutoFit.y - style.itemSpacing.y, 0f)
        sizeAutoFit
    }


    infix fun addTo(renderList: ArrayList<DrawList>) {
        drawList addTo renderList
        dc.childWindows.filter { it.active }  // clipped children may have been marked not active
                .filter { it.flags hasnt Wf.Popup || it.hiddenFrames <= 0 }
                .forEach { it addTo renderList }
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

    fun addToRenderListSelectLayer() {
        // FIXME: Generalize this with a proper layering system so e.g. user can draw in specific layers, below text, ..
        IO.metricsActiveWindows++
        this addTo when {
            flags has Wf.Popup -> g.renderDrawLists[1]
            flags has Wf.Tooltip -> g.renderDrawLists[2]
            else -> g.renderDrawLists[0]
        }
    }

    // FIXME: Add a more explicit sort order in the window structure.
    private val childWindowComparer = compareBy<Window>({ it.flags has Wf.Popup }, { it.flags has Wf.Tooltip },
            { it.flags has Wf.ComboBox }, { it.orderWithinParent })
}