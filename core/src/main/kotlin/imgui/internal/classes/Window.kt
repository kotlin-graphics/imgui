package imgui.internal.classes

import gli_.has
import gli_.hasnt
import glm_.*
import glm_.vec2.Vec2
import glm_.vec2.Vec2bool
import glm_.vec2.Vec2i
import imgui.*
import imgui.ImGui.calcTextSize
import imgui.ImGui.clearActiveID
import imgui.ImGui.closeButton
import imgui.ImGui.collapseButton
import imgui.ImGui.focusWindow
import imgui.ImGui.io
import imgui.ImGui.keepAliveID
import imgui.ImGui.mainViewport
import imgui.ImGui.popID
import imgui.ImGui.renderFrame
import imgui.ImGui.renderTextClipped
import imgui.ImGui.scrollbar
import imgui.ImGui.setActiveID
import imgui.ImGui.style
import imgui.WindowFlag
import imgui.api.g
import imgui.classes.Context
import imgui.classes.DrawList
import imgui.classes.SizeCallbackData
import imgui.internal.*
import imgui.internal.sections.*
import kool.cap
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.reflect.KMutableProperty0
import imgui.WindowFlag as Wf

/** Storage for one window */
class Window(var context: Context,
             /** Window name, owned by the window. */
             var name: String) {

    /** == ImHashStr(Name) */
    val id: ID = hashStr(name)

    /** See enum WindowFlags */
    var flags = Wf.None.i

    /** Position (always rounded-up to nearest pixel)    */
    var pos = Vec2()

    /** Current size (==SizeFull or collapsed title bar size)   */
    var size = Vec2()

    /** Size when non collapsed */
    var sizeFull = Vec2()

    /** Size of contents/scrollable client area (calculated from the extents reach of the cursor) from previous frame. Does not include window decoration or window padding. */
    var contentSize = Vec2()

    val contentSizeIdeal = Vec2()

    /** Size of contents/scrollable client area explicitly request by the user via SetNextWindowContentSize(). */
    var contentSizeExplicit = Vec2()

    /** Window padding at the time of Begin(). */
    var windowPadding = Vec2()

    /** Window rounding at the time of Begin(). May be clamped lower to avoid rendering artifacts with title bar, menu bar etc.   */
    var windowRounding = 0f

    /** Window border size at the time of Begin().    */
    var windowBorderSize = 1f

    /** Size of buffer storing Name. May be larger than strlen(Name)! */
    var nameBufLen = name.toByteArray().size

    /** == window->GetID("#MOVE")   */
    var moveId: ID

    /** ID of corresponding item in parent window (for navigation to return from child window to parent window)   */
    var childId: ID = 0

    var scroll = Vec2()

    var scrollMax = Vec2()

    /** target scroll position. stored as cursor position with scrolling canceled out, so the highest point is always
    0.0f. (FLT_MAX for no change)   */
    var scrollTarget = Vec2(Float.MAX_VALUE)

    /** 0.0f = scroll so that target position is at top, 0.5f = scroll so that target position is centered  */
    var scrollTargetCenterRatio = Vec2(.5f)

    /** 0.0f = no snapping, >0.0f snapping threshold */
    val scrollTargetEdgeSnapDist = Vec2()

    /** Size taken by each scrollbars on their smaller axis. Pay attention! ScrollbarSizes.x == width of the vertical scrollbar, ScrollbarSizes.y = height of the horizontal scrollbar. */
    var scrollbarSizes = Vec2()

    /** Are scrollbars visible? */
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

    /** Do not display (== HiddenFrames*** > 0) */
    var hidden = false

    /** Set on the "Debug##Default" window. */
    var isFallbackWindow = false

    /** Set when the window has a close button (p_open != NULL) */
    var hasCloseButton = false

    /** Current border being held for resize (-1: none, otherwise 0-3) */
    var resizeBorderHeld = 0

    /** Number of Begin() during the current frame (generally 0 or 1, 1+ if appending via multiple Begin/End pairs) */
    var beginCount = 0

    /** Order within immediate parent window, if we are a child window. Otherwise 0. */
    var beginOrderWithinParent = 0

    /** Order within entire imgui context. This is mostly used for debugging submission order related issues. */
    var beginOrderWithinContext = 0

    /** ID in the popup stack when this window is used as a popup/menu (because we use generic Name/ID for recycling)   */
    var popupId: ID = 0

    var autoFitFrames = Vec2i(-1)

    var autoFitChildAxes = 0x00

    var autoFitOnlyGrows = false

    var autoPosLastDirection = Dir.None

    /** Hide the window for N frames */
    var hiddenFramesCanSkipItems = 0

    /** Hide the window for N frames while allowing items to be submitted so we can measure their size */
    var hiddenFramesCannotSkipItems = 0

    /** Hide the window until frame N at Render() time only */
    var hiddenFramesForRenderOnly = 0

    /** Disable window interactions for N frames */
    var disableInputsFrames = 0

    /** store acceptable condition flags for SetNextWindowPos() use. */
    var setWindowPosAllowFlags = Cond.Always or Cond.Once or Cond.FirstUseEver or Cond.Appearing

    /** store acceptable condition flags for SetNextWindowSize() use.    */
    var setWindowSizeAllowFlags = Cond.Always or Cond.Once or Cond.FirstUseEver or Cond.Appearing

    /** store acceptable condition flags for SetNextWindowCollapsed() use.   */
    var setWindowCollapsedAllowFlags = Cond.Always or Cond.Once or Cond.FirstUseEver or Cond.Appearing

    /** store window position when using a non-zero Pivot (position set needs to be processed when we know the window size) */
    var setWindowPosVal = Vec2(Float.MAX_VALUE)

    /** store window pivot for positioning. Vec2(0, 0) when positioning from top-left corner; Vec2(0.5f) for centering;
     *  Vec2(1) for bottom right.   */
    var setWindowPosPivot = Vec2(Float.MAX_VALUE)


    /** ID stack. ID are hashes seeded with the value at the top of the stack. (In theory this should be in the TempData structure)   */
    val idStack = Stack<ID>()

    init {
        idStack += id
        moveId = getID("#MOVE")
    }

    /** Temporary per-window data, reset at the beginning of the frame. This used to be called DrawContext, hence the "DC" variable name.  */
    var dc = WindowTempData()

    fun destroy() {
        assert(drawList === drawListInst)
        columnsStorage.forEach { it.destroy() }
        drawListInst._clearFreeMemory(true)
    }

    // The best way to understand what those rectangles are is to use the 'Metrics->Tools->Show Windows Rectangles' viewer.
    // The main 'OuterRect', omitted as a field, is window->Rect().

    /** == Window->Rect() just after setup in Begin(). == window->Rect() for root window. */
    var outerRectClipped = Rect()

    /** Inner rectangle (omit title bar, menu bar, scroll bar) */
    var innerRect = Rect(0f, 0f, 0f, 0f) // Clear so the InnerRect.GetSize() code in Begin() doesn't lead to overflow even if the result isn't used.

    /**  == InnerRect shrunk by WindowPadding*0.5f on each side, clipped within viewport or parent clip rect. */
    var innerClipRect = Rect()

    /** Initially covers the whole scrolling region. Reduced by containers e.g columns/tables when active.
     * Shrunk by WindowPadding*1.0f on each side. This is meant to replace ContentRegionRect over time (from 1.71+ onward). */
    var workRect = Rect()

    /** Backup of WorkRect before entering a container such as columns/tables. Used by e.g. SpanAllColumns functions
     *  to easily access. Stacked containers are responsible for maintaining this. // FIXME-WORKRECT: Could be a stack? */
    val parentWorkRect = Rect()

    /** Current clipping/scissoring rectangle, evolve as we are using PushClipRect(), etc. == DrawList->clip_rect_stack.back(). */
    var clipRect = Rect()

    /** FIXME: This is currently confusing/misleading. It is essentially WorkRect but not handling of scrolling. We currently rely on it as right/bottom aligned sizing operation need some size to rely on. */
    var contentRegionRect = Rect()

    /** Define an optional rectangular hole where mouse will pass-through the window. */
    val hitTestHoleSize = Vec2i()

    val hitTestHoleOffset = Vec2i()


    /** Last frame number the window was Active. */
    var lastFrameActive = -1

    /** Last timestamp the window was Active (using float as we don't need high precision there) */
    var lastTimeActive = -1f

    var itemWidthDefault = 0f

    var stateStorage = HashMap<ID, Boolean>()

    val columnsStorage = ArrayList<OldColumns>()

    /** User scale multiplier per-window, via SetWindowFontScale() */
    var fontWindowScale = 1f

    /** Offset into SettingsWindows[] (offsets are always valid as we only grow the array from the back) */
    var settingsOffset = -1

    val drawListInst = DrawList(null)

    /** == &DrawListInst (for backward compatibility reason with code using imgui_internal.h we keep this a pointer) */
    var drawList = drawListInst.apply {
        _data = context.drawListSharedData
        _ownerName = name
    }

    /** If we are a child _or_ popup window, this is pointing to our parent. Otherwise NULL.  */
    var parentWindow: Window? = null

    /** Point to ourself or first ancestor that is not a child window == Top-level window. */
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


    /** Backup of last idx/vtx count, so when waking up the window we can preallocate and avoid iterative alloc/copy */
    var memoryDrawListIdxCapacity = 0
    var memoryDrawListVtxCapacity = 0

    /** Set when window extraneous data have been garbage collected */
    var memoryCompacted = false

    /** calculate unique ID (hash of whole ID stack + given parameter). useful if you want to query into ImGuiStorage yourself  */
    fun getID(strID: String, end: Int = 0): ID {
        val seed: ID = idStack.last()
        val id: ID = hashStr(strID, end, seed)
        keepAliveID(id)
        if (IMGUI_ENABLE_TEST_ENGINE)
            IMGUI_TEST_ENGINE_ID_INFO2(id, DataType._String, strID, end)
        return id
    }

    /** [JVM] */
    fun getID(ptrID: Any): ID {
        val seed: ID = idStack.last()
        val id: ID = hash(System.identityHashCode(ptrID), seed)
        keepAliveID(id)
        if (IMGUI_ENABLE_TEST_ENGINE)
            IMGUI_TEST_ENGINE_ID_INFO(id, DataType._Pointer, ptrID)
        return id
    }

    /** [JVM] we hack the pointer version in this way */
    fun getID(intPtr: Long): ID {
        if (intPtr >= ptrId.size) increase()
        val seed: ID = idStack.last()
        val id = hash(System.identityHashCode(ptrId[intPtr.i]), seed)
        keepAliveID(id)
        if (IMGUI_ENABLE_TEST_ENGINE)
            IMGUI_TEST_ENGINE_ID_INFO(id, DataType.Long, intPtr) // TODO check me
        return id
    }

    fun getID(n: Int): ID {
        val seed = idStack.last()
        val id = hash(n, seed)
        keepAliveID(id)
        if (IMGUI_ENABLE_TEST_ENGINE)
            IMGUI_TEST_ENGINE_ID_INFO(id, DataType.Int, n)
        return id
    }

    fun getIdNoKeepAlive(strID: String, strEnd: Int = strID.length): ID {
        val id = hashStr(strID, strID.length - strEnd, seed_ = idStack.last())
        if (IMGUI_ENABLE_TEST_ENGINE)
            IMGUI_TEST_ENGINE_ID_INFO2(id, DataType._String, strID, strEnd)
        return id
    }

    fun getIdNoKeepAlive(ptrID: Any): ID {
        val id = hash(System.identityHashCode(ptrID), seed = idStack.last())
        if (IMGUI_ENABLE_TEST_ENGINE)
            IMGUI_TEST_ENGINE_ID_INFO(id, DataType._Pointer, ptrID)
        return id
    }

    fun getIdNoKeepAlive(intPtr: Long): ID {
        if (intPtr >= ptrId.size) increase()
        val id = hash(System.identityHashCode(ptrId[intPtr.i]), seed = idStack.last())
        if (IMGUI_ENABLE_TEST_ENGINE)
            IMGUI_TEST_ENGINE_ID_INFO(id, DataType.Long, intPtr) // TODO checkMe
        return id
    }

    fun getIdNoKeepAlive(n: Int): ID {
        val id = hash(n, seed = idStack.last())
        if (IMGUI_ENABLE_TEST_ENGINE)
            IMGUI_TEST_ENGINE_ID_INFO(id, DataType.Int, n)
        return id
    }

    private fun increase() {
        ptrId = Array(ptrId.size + 512) { i -> ptrId.getOrElse(i) { i } }
    }

    /** This is only used in rare/specific situations to manufacture an ID out of nowhere. */
    fun getIdFromRectangle(rAbs: Rect): ID {
        val seed: ID = idStack.last()
        val rRel =
            intArrayOf((rAbs.min.x - pos.x).i, (rAbs.min.y - pos.y).i, (rAbs.max.x - pos.x).i, (rAbs.max.y - pos.y).i)
        return hash(rRel, seed).also { keepAliveID(it) } // id
    }

    /** We don't use g.FontSize because the window may be != g.CurrentWidow. */
    fun rect(): Rect = Rect(pos.x.f, pos.y.f, pos.x + size.x, pos.y + size.y)

    fun calcFontSize(): Float {
        var scale = g.fontBaseSize * fontWindowScale
        parentWindow?.let { scale *= it.fontWindowScale }
        return scale
    }

    val titleBarHeight: Float
        get() = when {
            flags has Wf.NoTitleBar -> 0f
            else -> calcFontSize() + style.framePadding.y * 2f
        }

    fun titleBarRect(): Rect = Rect(pos, Vec2(pos.x + sizeFull.x, pos.y + titleBarHeight))
    val menuBarHeight: Float
        get() = when {
            flags has Wf.MenuBar -> dc.menuBarOffset.y + calcFontSize() + style.framePadding.y * 2f
            else -> 0f
        }

    fun menuBarRect(): Rect {
        val y1 = pos.y + titleBarHeight
        return Rect(pos.x.f, y1, pos.x + sizeFull.x, y1 + menuBarHeight)
    }


    // end class original methods


    // --------------------------------------------- internal API ------------------------------------------------------


    // Windows: Display Order and Focus Order

    /** ~BringWindowToFocusFront */
    fun bringToFocusFront() {
        if (g.windowsFocusOrder.last() === this) return
        for (i in g.windowsFocusOrder.size - 2 downTo 0) // We can ignore the top-most window
            if (g.windowsFocusOrder[i] === this) {
                g.windowsFocusOrder.removeAt(i)
                g.windowsFocusOrder += this
                break
            }
    }

    /** ~BringWindowToDisplayFront */
    fun bringToDisplayFront() {
        val currentFrontWindow = g.windows.last()
        if (currentFrontWindow === this || currentFrontWindow.rootWindow === this) // Cheap early out (could be better)
            return
        for (i in g.windows.size - 2 downTo 0) // We can ignore the top-most window
            if (g.windows[i] === this) {
                g.windows.removeAt(i)
                g.windows += this
                break
            }
    }

    /** ~ BringWindowToDisplayBack */
    fun bringToDisplayBack() {
        if (g.windows[0] === this) return
        for (i in 0 until g.windows.size) if (g.windows[i] === this) {
            g.windows.removeAt(i)
            g.windows.add(0, this)
        }
    }

    /** ~UpdateWindowParentAndRootLinks */
    fun updateParentAndRootLinks(flags: WindowFlags, parentWindow: Window?) {
        this.parentWindow = parentWindow
        rootWindow = this
        rootWindowForTitleBarHighlight = this
        rootWindowForNav = this
        parentWindow?.let {
            if (flags has Wf._ChildWindow && flags hasnt Wf._Tooltip) rootWindow = it.rootWindow
            if (flags hasnt Wf._Modal && flags has (Wf._ChildWindow or Wf._Popup)) rootWindowForTitleBarHighlight =
                it.rootWindowForTitleBarHighlight
        }
        while (rootWindowForNav!!.flags has Wf._NavFlattened) rootWindowForNav =
            rootWindowForNav!!.parentWindow!! // ~assert
    }

    /** ~CalcWindowNextAutoFitSize */
    fun calcNextAutoFitSize(): Vec2 {
        val sizeContentsCurrent = Vec2()
        val sizeContentsIdeal = Vec2()
        calcContentSizes(sizeContentsCurrent, sizeContentsIdeal)
        val sizeAutoFit = calcAutoFitSize(sizeContentsIdeal)
        val sizeFinal = calcSizeAfterConstraint(sizeAutoFit)
        return sizeFinal
    }

    /** ~IsWindowChildOf */
    infix fun isChildOf(potentialParent: Window?): Boolean {
        if (rootWindow === potentialParent) return true
        var window: Window? = this
        while (window != null) {
            if (window === potentialParent) return true
            window = window.parentWindow
        }
        return false
    }

    /** ~IsWindowAbove */
    infix fun isAbove(potentialBelow: Window?): Boolean {
        for (candidateWindow in g.windows.asReversed()) {
            if (candidateWindow === this)
                return true
            if (candidateWindow === potentialBelow)
                return false
        }
        return false
    }

    /** Can we focus this window with CTRL+TAB (or PadMenu + PadFocusPrev/PadFocusNext)
     *  Note that NoNavFocus makes the window not reachable with CTRL+TAB but it can still be focused with mouse or programmaticaly.
     *  If you want a window to never be focused, you may use the e.g. NoInputs flag.
     *  ~ IsWindowNavFocusable */
    val isNavFocusable: Boolean
        get() = wasActive && this === rootWindow && flags hasnt Wf.NoNavFocus

    /** ~GetWindowAllowedExtentRect
     *  Note that this is used for popups, which can overlap the non work-area of individual viewports. */
    val allowedExtentRect: Rect
        get() {
            val rScreen = (mainViewport as ViewportP).mainRect
            val padding = g.style.displaySafeAreaPadding
            rScreen expand Vec2(if (rScreen.width > padding.x * 2) -padding.x else 0f, if (rScreen.height > padding.y * 2) -padding.y else 0f)
            return rScreen
        }

    /** ~ SetWindowPos */
    fun setPos(pos: Vec2, cond: Cond = Cond.None) { // Test condition (NB: bit 0 is always true) and clear flags for next time
        if (cond != Cond.None && setWindowPosAllowFlags hasnt cond)
            return //        JVM, useless
        //        assert(cond == Cond.None || cond.isPowerOfTwo) { "Make sure the user doesn't attempt to combine multiple condition flags." }
        setWindowPosAllowFlags = setWindowPosAllowFlags wo (Cond.Once or Cond.FirstUseEver or Cond.Appearing)
        setWindowPosVal put Float.MAX_VALUE

        // Set
        val oldPos = Vec2(this.pos)
        this.pos put floor(pos)
        val offset = this.pos - oldPos
        dc.cursorPos plusAssign offset         // As we happen to move the window while it is being appended to (which is a bad idea - will smear) let's at least offset the cursor
        dc.cursorMaxPos plusAssign offset      // And more importantly we need to offset CursorMaxPos/CursorStartPos this so ContentSize calculation doesn't get affected.
        dc.idealMaxPos += offset
        dc.cursorStartPos plusAssign offset
    }

    /** ~SetWindowSize */
    fun setSize(size: Vec2, cond: Cond = Cond.None) { // Test condition (NB: bit 0 is always true) and clear flags for next time
        if (cond != Cond.None && setWindowSizeAllowFlags hasnt cond)
            return //        JVM, useless
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

    /** ~SetWindowCollapsed */
    fun setCollapsed(collapsed: Boolean,
                     cond: Cond = Cond.None) { // Test condition (NB: bit 0 is always true) and clear flags for next time
        if (cond != Cond.None && setWindowCollapsedAllowFlags hasnt cond) return
        setWindowCollapsedAllowFlags =
            setWindowCollapsedAllowFlags and (Cond.Once or Cond.FirstUseEver or Cond.Appearing).inv() // Set
        this.collapsed = collapsed
    }

    /** ~SetWindowHitTestHole */
    fun setHitTestHole(pos: Vec2, size: Vec2) {
        assert(hitTestHoleSize.x == 0) { "We don't support multiple holes/hit test filters" }
        hitTestHoleSize put size
        hitTestHoleOffset put (pos - this.pos)
    }


    // Garbage collection

    /** Free up/compact internal window buffers, we can use this when a window becomes unused.
     *  Not freed:
     *  - ImGuiWindow, ImGuiWindowSettings, Name, StateStorage, ColumnsStorage (may hold useful data)
     *  This should have no noticeable visual effect. When the window reappear however, expect new allocation/buffer growth/copy cost.
     *
     *  ~gcCompactTransientWindowBuffers */
    fun gcCompactTransientBuffers() {
        memoryCompacted = true
        memoryDrawListIdxCapacity = drawList.idxBuffer.cap
        memoryDrawListVtxCapacity = drawList.vtxBuffer.cap
        idStack.clear()
        drawList._clearFreeMemory()
        dc.apply {
            childWindows.clear()
            itemWidthStack.clear()
            textWrapPosStack.clear()
        }
    }

    /** ~GcAwakeTransientWindowBuffers */
    fun gcAwakeTransientBuffers() { // We stored capacity of the ImDrawList buffer to reduce growth-caused allocation/copy when awakening.
        // The other buffers tends to amortize much faster.
        memoryCompacted = false
        drawList.apply {
            idxBuffer = idxBuffer reserve memoryDrawListIdxCapacity
            vtxBuffer = vtxBuffer reserve memoryDrawListVtxCapacity
        }
        memoryDrawListIdxCapacity = 0
        memoryDrawListVtxCapacity = 0
    }


    // Internal API, newFrame


    /** ~ StartMouseMovingWindow */
    fun startMouseMoving() {/*  Set ActiveId even if the _NoMove flag is set. Without it, dragging away from a window with _NoMove would
            activate hover on other windows.
            We _also_ call this when clicking in a window empty space when io.ConfigWindowsMoveFromTitleBarOnly is set,
            but clear g.MovingWindow afterward.
            This is because we want ActiveId to be set even when the window is not permitted to move.   */
        focusWindow(this)
        setActiveID(moveId, this)
        g.navDisableHighlight = true
        g.activeIdNoClearOnFocusLoss = true
        g.activeIdClickOffset = io.mousePos - rootWindow!!.pos

        val canMoveWindow = flags hasnt Wf.NoMove && rootWindow!!.flags hasnt Wf.NoMove
        if (canMoveWindow) g.movingWindow = this
    }


    // Internal API, Settings


    fun markIniSettingsDirty() {
        if (flags hasnt Wf.NoSavedSettings && g.settingsDirtyTimer <= 0f) g.settingsDirtyTimer = io.iniSavingRate
    }


    // Internal API, Scrolling


    /** ~SetScrollX(ImGuiWindow* window, float new_scroll_x) */
    infix fun setScrollX(scrollX: Float) {
        scrollTarget.x = scrollX
        scrollTargetCenterRatio.x = 0f
        scrollTargetEdgeSnapDist.x = 0.0f
    }

    /** ~SetScrollY(ImGuiWindow* window, float new_scroll_y) */
    infix fun setScrollY(scrollY: Float) {
        scrollTarget.y = scrollY
        scrollTargetCenterRatio.y = 0f
        scrollTargetEdgeSnapDist.y = 0f
    }

    /** adjust scrolling amount to make given position visible. Generally GetCursorStartPos() + offset to compute a valid position.
     *
     *  Note that a local position will vary depending on initial scroll value,
     *  This is a little bit confusing so bear with us:
     *   - local_pos = (absolution_pos - window->Pos)
     *   - So local_x/local_y are 0.0f for a position at the upper-left corner of a window,
     *     and generally local_x/local_y are >(padding+decoration) && <(size-padding-decoration) when in the visible area.
     *   - They mostly exists because of legacy API.
     *  Following the rules above, when trying to work with scrolling code, consider that:
     *   - SetScrollFromPosY(0.0f) == SetScrollY(0.0f + scroll.y) == has no effect!
     *   - SetScrollFromPosY(-scroll.y) == SetScrollY(-scroll.y + scroll.y) == SetScrollY(0.0f) == reset scroll. Of course writing SetScrollY(0.0f) directly then makes more sense
     *  We store a target position so centering and clamping can occur on the next frame when we are guaranteed to have a known window size
     *
     *  ~SetScrollFromPosX(ImGuiWindow* window, float local_x, float center_x_ratio) */
    fun setScrollFromPosX(localX: Float, centerXRatio: Float) {
        assert(centerXRatio in 0f..1f)
        scrollTarget.x = floor(localX + scroll.x) // Convert local position to scroll offset
        scrollTargetCenterRatio.x = centerXRatio
        scrollTargetEdgeSnapDist.x = 0f
    }

    /** adjust scrolling amount to make given position visible. Generally GetCursorStartPos() + offset to compute a valid position.   */
    fun setScrollFromPosY(localY_: Float, centerYRatio: Float) {
        assert(centerYRatio in 0f..1f)
        val decorationUpHeight = titleBarHeight + menuBarHeight // FIXME: Would be nice to have a more standardized access to our scrollable/client rect;
        val localY = localY_ - decorationUpHeight
        scrollTarget.y = floor(localY + scroll.y) // Convert local position to scroll offset
        scrollTargetCenterRatio.y = centerYRatio
        scrollTargetEdgeSnapDist.y = 0f
    }

    /** Scroll to keep newly navigated item fully into view */
    infix fun scrollToBringRectIntoView(itemRect: Rect): Vec2 {
        val windowRect = Rect(innerRect.min - 1,
                              innerRect.max + 1) //GetOverlayDrawList(window)->AddRect(window->Pos + window_rect_rel.Min, window->Pos + window_rect_rel.Max, IM_COL32_WHITE); // [DEBUG]

        val deltaScroll = Vec2()
        if (itemRect !in windowRect) {
            if (scrollbar.x && itemRect.min.x < windowRect.min.x) setScrollFromPosX(itemRect.min.x - pos.x - style.itemSpacing.x,
                                                                                    0f)
            else if (scrollbar.x && itemRect.max.x >= windowRect.max.x) setScrollFromPosX(itemRect.max.x - pos.x + style.itemSpacing.x,
                                                                                          1f)
            if (itemRect.min.y < windowRect.min.y) setScrollFromPosY(itemRect.min.y - pos.y - style.itemSpacing.y, 0f)
            else if (itemRect.max.y >= windowRect.max.y) setScrollFromPosY(itemRect.max.y - pos.y + style.itemSpacing.y,
                                                                           1f)

            val nextScroll = calcNextScrollFromScrollTargetAndClamp()
            deltaScroll put (nextScroll - scroll)
        }

        // Also scroll parent window to keep us into view if necessary
        if (flags has Wf._ChildWindow) deltaScroll += parentWindow!! scrollToBringRectIntoView Rect(itemRect.min - deltaScroll,
                                                                                                    itemRect.max - deltaScroll)

        return deltaScroll
    }


    // Internal API, new columns API


    fun findOrCreateColumns(id: ID): OldColumns {

        // We have few columns per window so for now we don't need bother much with turning this into a faster lookup.
        for (c in columnsStorage) if (c.id == id) return c

        return OldColumns().also {
            columnsStorage += it
            it.id = id
        }
    }


    // Internal API, Widgets

    /** Return scrollbar rectangle, must only be called for corresponding axis if window->ScrollbarX/Y is set.
     *  ~GetWindowScrollbarRect     */
    infix fun getScrollbarRect(axis: Axis): Rect {
        val outerRect = rect() //        val innerRect = innerRect
        val borderSize = windowBorderSize
        val scrollbarSize =
            scrollbarSizes[axis xor 1] // (ScrollbarSizes.x = width of Y scrollbar; ScrollbarSizes.y = height of X scrollbar)
        assert(scrollbarSize > 0f)
        return when (axis) {
            Axis.X -> Rect(innerRect.min.x,
                           max(outerRect.min.y, outerRect.max.y - borderSize - scrollbarSize),
                           innerRect.max.x,
                           outerRect.max.y)
            else -> Rect(max(outerRect.min.x, outerRect.max.x - borderSize - scrollbarSize),
                         innerRect.min.y,
                         outerRect.max.x,
                         innerRect.max.y)
        }
    }

    /** ~GetWindowScrollbarID */
    infix fun getScrollbarID(axis: Axis): ID = getIdNoKeepAlive(if (axis == Axis.X) "#SCROLLX" else "#SCROLLY")

    /** 0..3: corners (Lower-right, Lower-left, Unused, Unused)
     *  4..7: borders (Top, Right, Bottom, Left) */
    infix fun getResizeID(n: Int): ID {
        assert(n in 0..7)
        val id = hashStr("#RESIZE", 0, id)
        return hash(intArrayOf(n), id)
    }


    // static - Misc

    /** ~updateWindowManualResize
     *  Handle resize for: Resize Grips, Borders, Gamepad
     *  @return [JVM] borderHelf to Boolean   */
    fun updateManualResize(
        sizeAutoFit: Vec2, borderHeld_: Int, resizeGripCount: Int,
        resizeGripCol: IntArray, visibilityRect: Rect,
                          ): Pair<Int, Boolean> {

        var borderHeld = borderHeld_

        val flags = flags

        if (flags has WindowFlag.NoResize || flags has WindowFlag.AlwaysAutoResize || autoFitFrames anyGreaterThan 0)
            return borderHeld to false
        if (!wasActive) // Early out to avoid running this code for e.g. an hidden implicit/fallback Debug window.
            return borderHeld to false

        var retAutoFit = false
        val resizeBorderCount = if (io.configWindowsResizeFromEdges) 4 else 0
        val gripDrawSize = floor(kotlin.math.max(g.fontSize * 1.35f, windowRounding + 1f + g.fontSize * 0.2f))
        val gripHoverInnerSize = floor(gripDrawSize * 0.75f)
        val gripHoverOuterSize = if (io.configWindowsResizeFromEdges) WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS else 0f

        val posTarget = Vec2(Float.MAX_VALUE)
        val sizeTarget = Vec2(Float.MAX_VALUE)

        // Resize grips and borders are on layer 1
        dc.navLayerCurrent = NavLayer.Menu

        // Manual resize grips
        ImGui.pushID("#RESIZE")
        for (resizeGripN in 0 until resizeGripCount) {

            val grip = resizeGripDef[resizeGripN]
            val corner = pos.lerp(pos + size, grip.cornerPosN)

            // Using the FlattenChilds button flag we make the resize button accessible even if we are hovering over a child window
            val resizeRect = Rect(corner - grip.innerDir * gripHoverOuterSize, corner + grip.innerDir * gripHoverInnerSize)
            if (resizeRect.min.x > resizeRect.max.x) swap(resizeRect.min::x, resizeRect.max::x)
            if (resizeRect.min.y > resizeRect.max.y) swap(resizeRect.min::y, resizeRect.max::y)

            val f = ButtonFlag.FlattenChildren or ButtonFlag.NoNavFocus
            val (_, hovered, held) = ImGui.buttonBehavior(resizeRect, getID(resizeGripN), f)
            //GetOverlayDrawList(window)->AddRect(resize_rect.Min, resize_rect.Max, IM_COL32(255, 255, 0, 255));
            if (hovered || held)
                g.mouseCursor = if (resizeGripN has 1) MouseCursor.ResizeNESW else MouseCursor.ResizeNWSE

            if (held && g.io.mouseDoubleClicked[0] && resizeGripN == 0) {
                // Manual auto-fit when double-clicking
                sizeTarget put calcSizeAfterConstraint(sizeAutoFit)
                retAutoFit = true
                clearActiveID()
            } else if (held) {
                // Resize from any of the four corners
                // We don't use an incremental MouseDelta but rather compute an absolute target size based on mouse position
                // Corner of the window corresponding to our corner grip
                var cornerTarget = g.io.mousePos - g.activeIdClickOffset + (grip.innerDir * gripHoverOuterSize).lerp(grip.innerDir * -gripHoverInnerSize, grip.cornerPosN)
                val clampMin = Vec2 { if (grip.cornerPosN[it] == 1f) visibilityRect.min[it] else -Float.MAX_VALUE }
                val clampMax = Vec2 { if (grip.cornerPosN[it] == 0f) visibilityRect.max[it] else Float.MAX_VALUE }
                cornerTarget = glm.clamp(cornerTarget, clampMin, clampMax)
                calcResizePosSizeFromAnyCorner(cornerTarget, grip.cornerPosN, posTarget, sizeTarget)
            }
            if (resizeGripN == 0 || held || hovered)
                resizeGripCol[resizeGripN] = (if (held) Col.ResizeGripActive else if (hovered) Col.ResizeGripHovered else Col.ResizeGrip).u32
        }
        for (borderN in 0 until resizeBorderCount) {
            val borderRect = getResizeBorderRect(borderN, gripHoverInnerSize, WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS)
            val (_, hovered, held) = ImGui.buttonBehavior(borderRect, getID((borderN + 4)), ButtonFlag.FlattenChildren)
            //GetOverlayDrawList(window)->AddRect(border_rect.Min, border_rect.Max, IM_COL32(255, 255, 0, 255));
            if ((hovered && g.hoveredIdTimer > WINDOWS_RESIZE_FROM_EDGES_FEEDBACK_TIMER) || held) {
                g.mouseCursor = if (borderN has 1) MouseCursor.ResizeEW else MouseCursor.ResizeNS
                if (held)
                    borderHeld = borderN
            }
            if (held) {
                var borderTarget = Vec2(pos)
                val borderPosN = when (borderN) {
                    0 -> {
                        borderTarget.y = g.io.mousePos.y - g.activeIdClickOffset.y + WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS
                        Vec2(0, 0)
                    }
                    1 -> {
                        borderTarget.x = g.io.mousePos.x - g.activeIdClickOffset.x + WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS
                        Vec2(1, 0)
                    }
                    2 -> {
                        borderTarget.y = g.io.mousePos.y - g.activeIdClickOffset.y + WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS
                        Vec2(0, 1)
                    }
                    3 -> {
                        borderTarget.x = g.io.mousePos.x - g.activeIdClickOffset.x + WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS
                        Vec2(0, 0)
                    }
                    else -> Vec2(0, 0)
                }
                val clampMin = Vec2 { if (borderN == it + 1) visibilityRect.min[it] else -Float.MAX_VALUE }
                val clampMax = Vec2 { if (borderN == (if (it == 0) 3 else 0)) visibilityRect.max[it] else Float.MAX_VALUE }
                borderTarget = glm.clamp(borderTarget, clampMin, clampMax)
                calcResizePosSizeFromAnyCorner(borderTarget, borderPosN, posTarget, sizeTarget)
            }
        }
        popID()

        // Restore nav layer
        dc.navLayerCurrent = NavLayer.Main

        // Navigation resize (keyboard/gamepad)
        if (g.navWindowingTarget?.rootWindow === this) {
            val navResizeDelta = Vec2()
            if (g.navInputSource == InputSource.Keyboard && g.io.keyShift)
                navResizeDelta put ImGui.getNavInputAmount2d(NavDirSourceFlag.Keyboard.i, InputReadMode.Down)
            if (g.navInputSource == InputSource.Gamepad)
                navResizeDelta put ImGui.getNavInputAmount2d(NavDirSourceFlag.PadDPad.i, InputReadMode.Down)
            if (navResizeDelta.x != 0f || navResizeDelta.y != 0f) {
                val NAV_RESIZE_SPEED = 600f
                navResizeDelta *= floor(NAV_RESIZE_SPEED * g.io.deltaTime * kotlin.math.min(g.io.displayFramebufferScale.x, g.io.displayFramebufferScale.y))
                navResizeDelta put glm.max(navResizeDelta, visibilityRect.min - pos - size)
                g.navWindowingToggleLayer = false
                g.navDisableMouseHover = true
                resizeGripCol[0] = Col.ResizeGripActive.u32
                // FIXME-NAV: Should store and accumulate into a separate size buffer to handle sizing constraints properly, right now a constraint will make us stuck.
                sizeTarget put calcSizeAfterConstraint(sizeFull + navResizeDelta)
            }
        }

        // Apply back modified position/size to window
        if (sizeTarget.x != Float.MAX_VALUE) {
            sizeFull put sizeTarget
            markIniSettingsDirty()
        }
        if (posTarget.x != Float.MAX_VALUE) {
            pos = floor(posTarget)
            markIniSettingsDirty()
        }

        size put sizeFull

        return borderHeld to retAutoFit
    }

    // ~RenderWindowOuterBorders
    fun renderOuterBorders() {

        val rounding = windowRounding
        val borderSize = windowBorderSize
        if (borderSize > 0f && flags hasnt Wf.NoBackground) drawList.addRect(pos, pos + size, Col.Border.u32, rounding, 0, borderSize)

        val borderHeld = resizeBorderHeld
        if (borderHeld != -1) {
            val def = resizeBorderDef[borderHeld]
            val borderR = getResizeBorderRect(borderHeld, rounding, 0f)
            drawList.apply {
                pathArcTo(borderR.min.lerp(borderR.max, def.cornerPosN1) + Vec2(0.5f) + def.innerDir * rounding,
                          rounding,
                          def.outerAngle - glm.PIf * 0.25f,
                          def.outerAngle)
                pathArcTo(borderR.min.lerp(borderR.max, def.cornerPosN2) + Vec2(0.5f) + def.innerDir * rounding,
                          rounding,
                          def.outerAngle,
                          def.outerAngle + glm.PIf * 0.25f)
                pathStroke(Col.SeparatorActive.u32, 0, 2f max borderSize) // Thicker than usual
            }
        }
        if (style.frameBorderSize > 0f && flags hasnt Wf.NoTitleBar) {
            val y = pos.y + titleBarHeight - 1
            drawList.addLine(Vec2(pos.x + borderSize, y),
                             Vec2(pos.x + size.x - borderSize, y),
                             Col.Border.u32,
                             style.frameBorderSize)
        }
    }

    /** ~RenderWindowDecorations
     *  Draw background and borders
     *  Draw and handle scrollbars */
    fun renderDecorations(titleBarRect: Rect,
                          titleBarIsHighlight: Boolean,
                          resizeGripCount: Int,
                          resizeGripCol: IntArray,
                          resizeGripDrawSize: Float) {

        // Ensure that ScrollBar doesn't read last frame's SkipItems
        assert(beginCount == 0)
        skipItems = false

        // Draw window + handle manual resize
        // As we highlight the title bar when want_focus is set, multiple reappearing windows will have have their title bar highlighted on their reappearing frame.
        if (collapsed) { // Title bar only
            val backupBorderSize = style.frameBorderSize
            g.style.frameBorderSize = windowBorderSize
            val titleBarCol =
                if (titleBarIsHighlight && !g.navDisableHighlight) Col.TitleBgActive else Col.TitleBgCollapsed
            renderFrame(titleBarRect.min, titleBarRect.max, titleBarCol.u32, true, windowRounding)
            style.frameBorderSize = backupBorderSize
        } else { // Window background
            if (flags hasnt Wf.NoBackground) {
                var bgCol = getWindowBgColorIdxFromFlags(flags).u32
                var overrideAlpha = false
                val alpha = when {
                    g.nextWindowData.flags has NextWindowDataFlag.HasBgAlpha -> {
                        overrideAlpha = true
                        g.nextWindowData.bgAlphaVal
                    }
                    else -> 1f
                }
                if (overrideAlpha) bgCol = (bgCol and COL32_A_MASK.inv()) or (F32_TO_INT8_SAT(alpha) shl COL32_A_SHIFT)
                drawList.addRectFilled(pos + Vec2(0f, titleBarHeight), pos + size, bgCol, windowRounding,
                                       if (flags has Wf.NoTitleBar) 0 else DrawFlag.RoundCornersBottom.i)
            }

            // Title bar
            if (flags hasnt Wf.NoTitleBar) {
                val titleBarCol = if (titleBarIsHighlight) Col.TitleBgActive else Col.TitleBg
                drawList.addRectFilled(titleBarRect.min, titleBarRect.max, titleBarCol.u32, windowRounding, DrawFlag.RoundCornersTop.i)
            }

            // Menu bar
            if (flags has Wf.MenuBar) {
                val menuBarRect = menuBarRect()
                menuBarRect clipWith rect() // Soft clipping, in particular child window don't have minimum size covering the menu bar so this is useful for them.
                val rounding = if (flags has Wf.NoTitleBar) windowRounding else 0f
                drawList.addRectFilled(menuBarRect.min + Vec2(windowBorderSize, 0f),
                                       menuBarRect.max - Vec2(windowBorderSize, 0f),
                                       Col.MenuBarBg.u32, rounding, DrawFlag.RoundCornersTop.i)
                if (style.frameBorderSize > 0f && menuBarRect.max.y < pos.y + size.y)
                    drawList.addLine(menuBarRect.bl, menuBarRect.br, Col.Border.u32, style.frameBorderSize)
            }

            // Scrollbars
            if (scrollbar.x) scrollbar(Axis.X)
            if (scrollbar.y) scrollbar(Axis.Y)

            // Render resize grips (after their input handling so we don't have a frame of latency)
            if (flags hasnt Wf.NoResize) repeat(resizeGripCount) { resizeGripN ->
                val grip = resizeGripDef[resizeGripN]
                val corner = pos.lerp(pos + size, grip.cornerPosN)
                with(drawList) {
                    pathLineTo(corner + grip.innerDir * (if (resizeGripN has 1) Vec2(windowBorderSize, resizeGripDrawSize) else Vec2(resizeGripDrawSize, windowBorderSize)))
                    pathLineTo(corner + grip.innerDir * (if (resizeGripN has 1) Vec2(resizeGripDrawSize, windowBorderSize) else Vec2(windowBorderSize, resizeGripDrawSize)))
                    pathArcToFast(Vec2(corner.x + grip.innerDir.x * (windowRounding + windowBorderSize),
                                       corner.y + grip.innerDir.y * (windowRounding + windowBorderSize)),
                                  windowRounding, grip.angleMin12, grip.angleMax12)
                    pathFillConvex(resizeGripCol[resizeGripN])
                }
            }

            // Borders
            renderOuterBorders()
        }
    }

    /** ~RenderWindowTitleBarContents
     *  Render title text, collapse button, close button */
    fun renderTitleBarContents(titleBarRect: Rect, name: String, pOpen: KMutableProperty0<Boolean>?) {

        val hasCloseButton = pOpen != null
        val hasCollapseButton = flags hasnt Wf.NoCollapse && style.windowMenuButtonPosition != Dir.None

        // Close & Collapse button are on the Menu NavLayer and don't default focus (unless there's nothing else on that layer)
        val itemFlagsBackup = dc.itemFlags
        dc.itemFlags = dc.itemFlags or ItemFlag.NoNavDefaultFocus
        dc.navLayerCurrent = NavLayer.Menu

        // Layout buttons
        // FIXME: Would be nice to generalize the subtleties expressed here into reusable code.
        var padL = style.framePadding.x
        var padR = style.framePadding.x
        val buttonSz = g.fontSize
        val closeButtonPos = Vec2()
        val collapseButtonPos = Vec2()
        if (hasCloseButton) {
            padR += buttonSz
            closeButtonPos.put(titleBarRect.max.x - padR - style.framePadding.x, titleBarRect.min.y)
        }
        if (hasCollapseButton && style.windowMenuButtonPosition == Dir.Right) {
            padR += buttonSz
            collapseButtonPos.put(titleBarRect.max.x - padR - style.framePadding.x, titleBarRect.min.y)
        }
        if (hasCollapseButton && style.windowMenuButtonPosition == Dir.Left) {
            collapseButtonPos.put(titleBarRect.min.x + padL - style.framePadding.x, titleBarRect.min.y)
            padL += buttonSz
        }

        // Collapse button (submitting first so it gets priority when choosing a navigation init fallback)
        if (hasCollapseButton)
            if (collapseButton(getID("#COLLAPSE"), collapseButtonPos))
                wantCollapseToggle = true // Defer actual collapsing to next frame as we are too far in the Begin() function

        // Close button
        if (hasCloseButton)
            if (closeButton(getID("#CLOSE"), closeButtonPos))
                pOpen!!.set(false)

        dc.navLayerCurrent = NavLayer.Main
        dc.itemFlags = itemFlagsBackup

        // Title bar text (with: horizontal alignment, avoiding collapse/close button, optional "unsaved document" marker)
        // FIXME: Refactor text alignment facilities along with RenderText helpers, this is too much code..
        val UNSAVED_DOCUMENT_MARKER = "*"
        val markerSizeX = when {
            flags has Wf.UnsavedDocument -> calcTextSize(UNSAVED_DOCUMENT_MARKER, hideTextAfterDoubleHash = false).x
            else -> 0f
        }
        val textSize = calcTextSize(name, hideTextAfterDoubleHash = true) + Vec2(markerSizeX, 0f)

        // As a nice touch we try to ensure that centered title text doesn't get affected by visibility of Close/Collapse button,
        // while uncentered title text will still reach edges correctly.
        if (padL > style.framePadding.x) padL += style.itemInnerSpacing.x
        if (padR > style.framePadding.x) padR += style.itemInnerSpacing.x
        if (style.windowTitleAlign.x > 0f && style.windowTitleAlign.x < 1f) {
            val centerness = saturate(1f - abs(style.windowTitleAlign.x - 0.5f) * 2f) // 0.0f on either edges, 1.0f on center
            val padExtend = min(max(padL, padR), titleBarRect.width - padL - padR - textSize.x)
            padL = padL max (padExtend * centerness)
            padR = padR max (padExtend * centerness)
        }

        val layoutR = Rect(titleBarRect.min.x + padL, titleBarRect.min.y, titleBarRect.max.x - padR, titleBarRect.max.y)
        val clipR = Rect(
            x1 = layoutR.min.x, layoutR.min.y,
            x2 = min(layoutR.max.x + style.itemInnerSpacing.x, titleBarRect.max.x),
            y2 = layoutR.max.y)
        //if (g.IO.KeyShift) window->DrawList->AddRect(layout_r.Min, layout_r.Max, IM_COL32(255, 128, 0, 255)); // [DEBUG]
        //if (g.IO.KeyCtrl) window->DrawList->AddRect(clip_r.Min, clip_r.Max, IM_COL32(255, 128, 0, 255)); // [DEBUG]
        renderTextClipped(layoutR.min, layoutR.max, name, textSize, style.windowTitleAlign, clipR)

        if (flags has Wf.UnsavedDocument) {
            val markerPos = Vec2(max(layoutR.min.x, layoutR.min.x + (layoutR.width - textSize.x) * style.windowTitleAlign.x) + textSize.x, layoutR.min.y) + Vec2(2 - markerSizeX, 0f)
            val off = Vec2(0f, floor(-g.fontSize * 0.25f))
            renderTextClipped(markerPos + off, layoutR.max + off, UNSAVED_DOCUMENT_MARKER,
                              null, Vec2(0, style.windowTitleAlign.y), clipR)
        }
    }

    /** ~ClampWindowRect */
    infix fun clampRect(visibilityRect: Rect) {
        val sizeForClamping = Vec2(size)
        if (io.configWindowsMoveFromTitleBarOnly && flags hasnt Wf.NoTitleBar)
            sizeForClamping.y = titleBarHeight
        pos put glm.clamp(pos, visibilityRect.min - sizeForClamping, visibilityRect.max)
    }

    //-------------------------------------------------------------------------
    // [SECTION] FORWARD DECLARATIONS
    //-------------------------------------------------------------------------

    // Helper to snap on edges when aiming at an item very close to the edge,
    // So the difference between WindowPadding and ItemSpacing will be in the visible area after scrolling.
    // When we refactor the scrolling API this may be configurable with a flag?
    // Note that the effect for this won't be visible on X axis with default Style settings as WindowPadding.x == ItemSpacing.x by default.
    fun calcScrollEdgeSnap(target: Float,
                           snapMin: Float,
                           snapMax: Float,
                           snapThreshold: Float,
                           centerRatio: Float): Float = when {
        target <= snapMin + snapThreshold -> lerp(snapMin, target, centerRatio)
        target >= snapMax - snapThreshold -> lerp(target, snapMax, centerRatio)
        else -> target
    }

    fun calcNextScrollFromScrollTargetAndClamp(): Vec2 {
        val scroll = Vec2(scroll)
        if (scrollTarget.x < Float.MAX_VALUE) {
            val decorationTotalWidth = scrollbarSizes.x
            val centerXRatio = scrollTargetCenterRatio.x
            var scrollTargetX = scrollTarget.x
            if (scrollTargetEdgeSnapDist.x > 0f) {
                val snapXMin = 0f
                val snapXMax = scrollMax.x + sizeFull.x - decorationTotalWidth
                scrollTargetX = calcScrollEdgeSnap(scrollTargetX, snapXMin, snapXMax, scrollTargetEdgeSnapDist.x, centerXRatio)
            }
            scroll.x = scrollTargetX - centerXRatio * (sizeFull.x - decorationTotalWidth)
        }
        if (scrollTarget.y < Float.MAX_VALUE) {
            val decorationTotalHeight = titleBarHeight + menuBarHeight + scrollbarSizes.y
            val centerYRatio = scrollTargetCenterRatio.y
            var scrollTargetY = scrollTarget.y
            if (scrollTargetEdgeSnapDist.y > 0f) {
                val snapYMin = 0f
                val snapYMax = scrollMax.y + sizeFull.y - decorationTotalHeight
                scrollTargetY = calcScrollEdgeSnap(scrollTargetY, snapYMin, snapYMax, scrollTargetEdgeSnapDist.y, centerYRatio)
            }
            scroll.y = scrollTargetY - centerYRatio * (sizeFull.y - decorationTotalHeight)
        }
        scroll.x = floor(scroll.x max 0f)
        scroll.y = floor(scroll.y max 0f)
        if (!collapsed && !skipItems) {
            scroll.x = glm.min(scroll.x, scrollMax.x)
            scroll.y = glm.min(scroll.y, scrollMax.y)
        }
        return scroll
    }

    /** ~AddWindowToSortBuffer */
    infix fun addToSortBuffer(sortedWindows: ArrayList<Window>) {
        sortedWindows += this
        if (active) {
            val count = dc.childWindows.size
            if (count > 1) dc.childWindows.sortWith(childWindowComparer)
            dc.childWindows.filter { it.active }.forEach { it addToSortBuffer sortedWindows }
        }
    }


    // sparse static methods


    /** ~IsWindowContentHoverable */
    infix fun isContentHoverable(flag: HoveredFlag): Boolean = isContentHoverable(flag.i)

    /** ~IsWindowContentHoverable */
    infix fun isContentHoverable(flags: HoveredFlags): Boolean { // An active popup disable hovering on other windows (apart from its own children)
        // FIXME-OPT: This could be cached/stored within the window.
        val focusedRootWindow = g.navWindow?.rootWindow ?: return true
        if (focusedRootWindow.wasActive && focusedRootWindow !== rootWindow) {/*  For the purpose of those flags we differentiate "standard popup" from "modal popup"
                NB: The order of those two tests is important because Modal windows are also Popups.             */
            if (focusedRootWindow.flags has Wf._Modal) return false
            if (focusedRootWindow.flags has Wf._Popup && flags hasnt HoveredFlag.AllowWhenBlockedByPopup) return false
        }
        return true
    }

    /** ~CalcWindowAutoFitSize */
    fun calcAutoFitSize(sizeContents: Vec2): Vec2 {
        val decorationUpHeight = titleBarHeight + menuBarHeight
        val sizePad = windowPadding * 2f
        val sizeDesired = sizeContents + sizePad + Vec2(0f, decorationUpHeight)
        return when {
            flags has Wf._Tooltip -> sizeDesired // Tooltip always resize
            else -> { // Maximum window size is determined by the viewport size or monitor size
                val isPopup = flags has Wf._Popup
                val isMenu = flags has Wf._ChildMenu
                val sizeMin =
                    Vec2(style.windowMinSize) // Popups and menus bypass style.WindowMinSize by default, but we give then a non-zero minimum size to facilitate understanding problematic cases (e.g. empty popups)
                if (isPopup || isMenu) sizeMin minAssign 4f

                // FIXME-VIEWPORT-WORKAREA: May want to use GetWorkSize() instead of Size depending on the type of windows?
                val availSize = ImGui.mainViewport.size
                val sizeAutoFit = glm.clamp(sizeDesired,
                                            sizeMin,
                                            glm.max(sizeMin, availSize - style.displaySafeAreaPadding * 2f))

                // When the window cannot fit all contents (either because of constraints, either because screen is too small),
                // we are growing the size on the other axis to compensate for expected scrollbar.
                // FIXME: Might turn bigger than ViewportSize-WindowPadding.
                val sizeAutoFitAfterConstraint = calcSizeAfterConstraint(sizeAutoFit)
                val willHaveScrollbarX =
                    (sizeAutoFitAfterConstraint.x - sizePad.x - 0f < sizeContents.x && flags hasnt Wf.NoScrollbar && flags has Wf.HorizontalScrollbar) || flags has Wf.AlwaysHorizontalScrollbar
                val willHaveScrollbarY =
                    (sizeAutoFitAfterConstraint.y - sizePad.y - decorationUpHeight < sizeContents.y && flags hasnt Wf.NoScrollbar) || flags has Wf.AlwaysVerticalScrollbar
                if (willHaveScrollbarX) sizeAutoFit.y += style.scrollbarSize
                if (willHaveScrollbarY) sizeAutoFit.x += style.scrollbarSize
                sizeAutoFit
            }
        }
    }

    /** AddWindowToDrawData */
    infix fun addToDrawData(layer: Int) {
        val viewport = g.viewports[0]
        io.metricsRenderWindows++
        drawList addTo viewport.drawDataBuilder!!.layers[layer]
        dc.childWindows.filter { it.isActiveAndVisible }  // Clipped children may have been marked not active
                .forEach { it addToDrawData layer }
    }

    /** Layer is locked for the root window, however child windows may use a different viewport (e.g. extruding menu)
     *  ~AddRootWindowToDrawData    */
    fun addRootToDrawData() {
        val layer = (flags has Wf._Tooltip).i
        addToDrawData(layer)
    }

    /** ~SetWindowConditionAllowFlags */
    fun setConditionAllowFlags(flags: Int, enabled: Boolean) = if (enabled) {
        setWindowPosAllowFlags = setWindowPosAllowFlags or flags
        setWindowSizeAllowFlags = setWindowSizeAllowFlags or flags
        setWindowCollapsedAllowFlags = setWindowCollapsedAllowFlags or flags
    } else {
        setWindowPosAllowFlags = setWindowPosAllowFlags wo flags
        setWindowSizeAllowFlags = setWindowSizeAllowFlags wo flags
        setWindowCollapsedAllowFlags = setWindowCollapsedAllowFlags wo flags
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
            0 -> Rect(rect.min.x + perpPadding,
                      rect.min.y - thickness,
                      rect.max.x - perpPadding,
                      rect.min.y + thickness)   // Top
            1 -> Rect(rect.max.x - thickness,
                      rect.min.y + perpPadding,
                      rect.max.x + thickness,
                      rect.max.y - perpPadding)   // Right
            2 -> Rect(rect.min.x + perpPadding,
                      rect.max.y - thickness,
                      rect.max.x - perpPadding,
                      rect.max.y + thickness)   // Bottom
            3 -> Rect(rect.min.x - thickness,
                      rect.min.y + perpPadding,
                      rect.min.x + thickness,
                      rect.max.y - perpPadding)   // Left
            else -> throw Error()
        }
    }

    /** ~CalcWindowSizeAfterConstraint */
    fun calcSizeAfterConstraint(sizeDesired: Vec2): Vec2 {
        val newSize = Vec2(sizeDesired)
        if (g.nextWindowData.flags has NextWindowDataFlag.HasSizeConstraint) { // Using -1,-1 on either X/Y axis to preserve the current size.
            val cr = g.nextWindowData.sizeConstraintRect
            newSize.x = if (cr.min.x >= 0 && cr.max.x >= 0) glm.clamp(newSize.x, cr.min.x, cr.max.x) else sizeFull.x
            newSize.y = if (cr.min.y >= 0 && cr.max.y >= 0) glm.clamp(newSize.y, cr.min.y, cr.max.y) else sizeFull.y
            g.nextWindowData.sizeCallback?.invoke(SizeCallbackData(userData = g.nextWindowData.sizeCallbackUserData,
                                                                   pos = Vec2(this@Window.pos),
                                                                   currentSize = sizeFull,
                                                                   desiredSize = newSize))
            newSize.x = floor(newSize.x)
            newSize.y = floor(newSize.y)
        }

        // Minimum size
        if (flags hasnt (Wf._ChildWindow or Wf.AlwaysAutoResize)) {
            val decorationUpHeight = titleBarHeight + menuBarHeight
            newSize maxAssign style.windowMinSize // Reduce artifacts with very small windows
            newSize.y = newSize.y max (decorationUpHeight + (0f max (style.windowRounding - 1f)))
        }
        return newSize
    }

    /** ~CalcWindowContentSizes */
    fun calcContentSizes(contentSizeCurrent: Vec2, contentSizeIdeal: Vec2) {
        var preserveOldContentSizes = false
        if (collapsed && autoFitFrames.x <= 0 && autoFitFrames.y <= 0)
            preserveOldContentSizes = true
        else if (hidden && hiddenFramesCannotSkipItems == 0 && hiddenFramesCanSkipItems > 0)
            preserveOldContentSizes = true
        if (preserveOldContentSizes) {
            contentSizeCurrent put this.contentSize
            contentSizeIdeal put contentSizeIdeal
            return
        }

        contentSizeCurrent.x = if (contentSizeExplicit.x != 0f) contentSizeExplicit.x else floor(dc.cursorMaxPos.x - dc.cursorStartPos.x)
        contentSizeCurrent.y = if (contentSizeExplicit.y != 0f) contentSizeExplicit.y else floor(dc.cursorMaxPos.y - dc.cursorStartPos.y)
        contentSizeIdeal.x = if (contentSizeExplicit.x != 0f) contentSizeExplicit.x else floor(max(dc.cursorMaxPos.x, dc.idealMaxPos.x) - dc.cursorStartPos.x)
        contentSizeIdeal.y = if (contentSizeExplicit.y != 0f) contentSizeExplicit.y else floor(max(dc.cursorMaxPos.y, dc.idealMaxPos.y) - dc.cursorStartPos.y)
    }


    /** Window has already passed the IsWindowNavFocusable()
     *  ~ getFallbackWindowNameForWindowingList */
    val fallbackWindowName: String
        get() = when {
            flags has Wf._Popup -> "(Popup)"
            flags has Wf.MenuBar && name == "##MainMenuBar" -> "(Main menu bar)"
            else -> "(Untitled)"
        }

    /** ~ IsWindowActiveAndVisible */
    val isActiveAndVisible: Boolean get() = active && !hidden


    /** ~StartLockWheelingWindow */
    fun startLockWheeling() {
        if (g.wheelingWindow === this) return
        g.wheelingWindow = this
        g.wheelingWindowRefMousePos put io.mousePos
        g.wheelingWindowTimer = WINDOWS_MOUSE_WHEEL_SCROLL_LOCK_TIMER
    }

    /** ~CreateNewWindow */
    infix fun applySettings(settings: WindowSettings) {
        pos put floor(Vec2(settings.pos))
        if (settings.size allGreaterThan 0f) sizeFull put floor(Vec2(settings.size))
        size put sizeFull
        collapsed = settings.collapsed
    }

    companion object {
        // FIXME: Add a more explicit sort order in the window structure.
        private val childWindowComparer =
            compareBy<Window>({ it.flags has Wf._Popup }, { it.flags has Wf._Tooltip }, { it.beginOrderWithinParent })

        class ResizeGripDef(val cornerPosN: Vec2, val innerDir: Vec2, val angleMin12: Int, val angleMax12: Int)

        val resizeGripDef = arrayOf(ResizeGripDef(Vec2(1, 1), Vec2(-1, -1), 0, 3),  // Lower-right
                                    ResizeGripDef(Vec2(0, 1), Vec2(+1, -1), 3, 6),  // Lower-left
                                    ResizeGripDef(Vec2(0, 0), Vec2(+1, +1), 6, 9),  // Upper-left (Unused)
                                    ResizeGripDef(Vec2(1, 0), Vec2(-1, +1), 9, 12)) // Upper-right (Unused)

        class ResizeBorderDef(val innerDir: Vec2, val cornerPosN1: Vec2, val cornerPosN2: Vec2, val outerAngle: Float)

        val resizeBorderDef = arrayOf(ResizeBorderDef(Vec2(0, +1), Vec2(0, 0), Vec2(1, 0), glm.PIf * 1.5f), // Top
                                      ResizeBorderDef(Vec2(-1, 0), Vec2(1, 0), Vec2(1, 1), glm.PIf * 0.0f), // Right
                                      ResizeBorderDef(Vec2(0, -1), Vec2(1, 1), Vec2(0, 1), glm.PIf * 0.5f), // Bottom
                                      ResizeBorderDef(Vec2(+1, 0), Vec2(0, 1), Vec2(0, 0), glm.PIf * 1.0f))  // Left

        fun getWindowBgColorIdxFromFlags(flags: Int) = when {
            flags has (Wf._Tooltip or Wf._Popup) -> Col.PopupBg
            flags has Wf._ChildWindow -> Col.ChildBg
            else -> Col.WindowBg
        }
    }

    override fun toString() = name
}

