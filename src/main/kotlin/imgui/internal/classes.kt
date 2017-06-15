package imgui.internal

import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec2.Vec2bool
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import imgui.*
import java.util.*
import kotlin.collections.ArrayList


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
        this.min = min
        this.max = max
    }

    constructor(v: Vec4) {
        min.put(v.x, v.y)
        max.put(v.z, v.w)
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
    val br = max

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

    infix fun reduce(amount: Vec2) {
        min.x += amount.x
        min.y += amount.y
        max.x -= amount.x
        max.y -= amount.y
    }

    infix fun clip(clip: Rect) {
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
}

/** Stacked color modifier, backup of modified data so we can restore it    */
class ColMod {
    var col = 0
    var backupValue = 0
}

/** Stacked style modifier, backup of modified data so we can restore it. Data type inferred from the variable. */
class StyleMod {
    var varIdx = 0
    val backupInt = IntArray(2)
    val backupFloat = FloatArray(2)

    constructor(idx: Int, v: Int) {
        varIdx = idx
        backupInt[0] = v
    }

    constructor(idx: Int, v: Float) {
        varIdx = idx
        backupFloat[0] = v
    }

    constructor(idx: Int, v: Vec2) {
        varIdx = idx
        backupFloat[0] = v.x
        backupFloat[1] = v.y
    }
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

/** Internal state of the currently focused/edited text input box   */
class TextEditState {

    /** widget id owning the text state */
    var id = 0
    /** edit buffer, we need to persist but can't guarantee the persistence of the user-provided buffer. so we copy
    into own buffer.    */
    val text = ArrayList<Char>()
    /** backup of end-user buffer at the time of focus (in UTF-8, unaltered)    */
    val initialText = ArrayList<Char>()

    val tempTextBuffer = ArrayList<Char>()
    /** we need to maintain our buffer length in both UTF-8 and wchar format.   */
    var curLenA = 0

    var curLenW = 0
    /** end-user buffer size    */
    var bufSizeA = 0

    var scrollX = 0f

//    var stbState = STB_TexteditState

    var cursorAnim = 0f

    var cursorFollow = false

    var selectedAllMouseLock = false


    /** After a user-input the cursor stays on for a while without blinking */
    fun cursorAnimReset() {
        cursorAnim = -0.30f
    }

    //    fun cursorClamp() { StbState.cursor = ImMin(StbState.cursor, CurLenW); StbState.select_start = ImMin(StbState.select_start, CurLenW); StbState.select_end = ImMin(StbState.select_end, CurLenW); }
//    fun hasSelection() { return StbState.select_start != StbState.select_end; }
//    fun clearSelection(){ StbState.select_start = StbState.select_end = StbState.cursor; }
//    fun selectAll(){ StbState.select_start = 0; StbState.select_end = CurLenW; StbState.cursor = StbState.select_end; StbState.has_preferred_x = false; }
    fun onKeyPressed(key: Int) {
        TODO()
//        stb_textedit_key(this, &StbState, key);
//        CursorFollow = true;
//        CursorAnimReset();
    }
}

// Data saved in imgui.ini file
class IniData {
    var name = ""
    var id = 0
    var pos = Vec2()
    var size = Vec2()
    var collapsed = false
}

// Mouse cursor data (used when io.MouseDrawCursor is set)
class MouseCursorData {
    var type = MouseCursor_.None
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

    var lastItemRect = Rect(0f, 0f, 0f, 0f)
    /** Item rectangle is hovered, and its window is currently interactable with (not blocked by a popup preventing
    access to the window)   */
    var lastItemHoveredAndUsable = false
    /** Item rectangle is hovered, but its window may or not be currently interactable with (might be blocked by a popup
    preventing access to the window)    */
    var lastItemHoveredRect = false

    var menuBarAppending = false

    var menuBarOffsetX = 0f

    val childWindows = ArrayList<Window>()

    var stateStorage: Storage? = null

    var layoutType = LayoutType_.Vertical


    // We store the current settings outside of the vectors to increase memory locality (reduce cache misses).
    // The vectors are rarely modified. Also it allows us to not heap allocate for short-lived windows which are not
    // using those settings.

    /** == ItemWidthStack.back(). 0.0: default, >0.0: width in pixels, <0.0: align xx pixels to the right of window */
    var itemWidth = 0f
    /** == TextWrapPosStack.back() [empty == -1.0f] */
    var textWrapPos = -1f
    /** == AllowKeyboardFocusStack.back() [empty == true]   */
    var allowKeyboardFocus = true
    /** == ButtonRepeatStack.back() [empty == false]    */
    var buttonRepeat = false

    val itemWidthStack = Stack<Float>()

    val textWrapPosStack = Stack<Float>()

    val allowKeyboardFocusStack = Stack<Boolean>()

    val buttonRepeatStack = Stack<Boolean>()

    val groupStack = Stack<GroupData>()

    var colorEditMode = ColorEditMode_.RGB
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

    var columnsCellMinY = 0f

    var columnsCellMaxY = 0f

    var columnsShowBorders = true

    var columnsSetId = 0

    val columnsData = ArrayList<ColumnData>()
}

/** Windows data    */
class Window(
        val name: String
) {
    /** == ImHash(Name) */
    var id = hash(name, 0).also { idStack.add(it) }
    /** See enum ImGuiWindowFlags_  */
    var flags = 0
    /** Order within immediate parent window, if we are a child window. Otherwise 0.    */
    var indexWithinParent = 0

    var posF = Vec2()
    /** Position rounded-up to nearest pixel    */
    var pos = Vec2i()
    /** Current size (==SizeFull or collapsed title bar size)   */
    var size = Vec2()
    /** Size when non collapsed */
    var sizeFull = Vec2()
    /** // Size of contents (== extents reach of the drawing cursor) from previous frame    */
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
    var moveId = getId("#MOVE")

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
    /** == Visible && !Collapsed    */
    var skipItems = false
    /** Number of Begin() during the current frame (generally 0 or 1, 1+ if appending via multiple Begin/End pairs) */
    var beginCount = 0
    /** ID in the popup stack when this window is used as a popup/menu (because we use generic Name/ID for recycling)   */
    var popupId = 0

    var autoFitFrames = Vec2i(-1)

    var autoFitOnlyGrows = false

    var autoPosLastDirection = -1

    var hiddenFrames = 0
    /** bit ImGuiSetCond_*** specify if SetWindowPos() call will succeed with this particular flag. */
    var setWindowPosAllowFlags = SetCond_.Always or SetCond_.Once or SetCond_.FirstUseEver or SetCond_.Appearing
    /** bit ImGuiSetCond_*** specify if SetWindowSize() call will succeed with this particular flag.    */
    var setWindowSizeAllowFlags = SetCond_.Always or SetCond_.Once or SetCond_.FirstUseEver or SetCond_.Appearing
    /** bit ImGuiSetCond_*** specify if SetWindowCollapsed() call will succeed with this particular flag.   */
    var setWindowCollapsedAllowFlags = SetCond_.Always or SetCond_.Once or SetCond_.FirstUseEver or SetCond_.Appearing

    var setWindowPosCenterWanted = false


    /** Temporary per-window data, reset at the beginning of the frame  */
    var dc = DrawContext()
    /** ID stack. ID are hashes seeded with the value at the top of the stack   */
    val idStack = ArrayList<Int>()
    /** = DrawList->clip_rect_stack.back(). Scissoring / clipping rectangle. x1, y1, x2, y2.    */
    var clipRect = Rect()
    /** = WindowRect just after setup in Begin(). == window->Rect() for root window.    */
    var windowRectClipped = Rect()

    var lastFrameActive = -1

    var itemWidthDefault = 0f
    /** Simplified columns storage for menu items   */
    //    ImGuiSimpleColumns      MenuColumns
//    ImGuiStorage            StateStorage;
    /** Scale multiplier per-window */
    var fontWindowScale = 1f

    var drawList = DrawList()
    /** If we are a child window, this is pointing to the first non-child parent window. Else point to ourself. */
    lateinit var rootWindow: Window
    /** If we are a child window, this is pointing to the first non-child non-popup parent window. Else point to ourself.   */
    var rootNonPopupWindow: Window? = null
    /** If we are a child window, this is pointing to our parent window. Else point to NULL.    */
    var parentWindow: Window? = null

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

    fun getId(str: String, end: Int = str.length): Int {
        val seed = idStack.last()
        val id = hash(str, str.length - end, seed)
        keepAliveID(id)
        return id
    }

    //    ImGuiID     GetID(const void* ptr);
    fun getIDNoKeepAlive(str: String, strEnd: Int = str.length): Int {
        val seed = idStack.last()
        return hash(str, str.length - strEnd, seed)
    }

    fun rect() = Rect(pos.x.f, pos.y.f, pos.x + size.x, pos.y + size.y)
    fun calcFontSize() = Context.fontBaseSize * fontWindowScale
    fun titleBarHeight() = if (flags has WindowFlags_.NoTitleBar) 0f else calcFontSize() + Style.framePadding.y * 2f
    fun titleBarRect() = Rect(pos, Vec2(pos.x + sizeFull.x, pos.y + titleBarHeight()))
    fun menuBarHeight() = if (flags has WindowFlags_.MenuBar) calcFontSize() + Style.framePadding.y * 2f else 0f
    fun menuBarRect(): Rect {
        val y1 = pos.y + titleBarHeight()
        return Rect(pos.x.f, y1, pos.x + sizeFull.x, y1 + menuBarHeight())
    }
}