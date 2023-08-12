package imgui.internal.classes

import glm_.*
import glm_.vec2.Vec2
import glm_.vec2.Vec2bool
import glm_.vec2.Vec2i
import imgui.*
import imgui.ImGui.debugHookIdInfo
import imgui.ImGui.io
import imgui.ImGui.isWithinBeginStackOf
import imgui.ImGui.msg
import imgui.ImGui.rectAbsToRel
import imgui.ImGui.style
import imgui.api.g
import imgui.classes.Context
import imgui.classes.DrawList
import imgui.classes.SizeCallbackData
import imgui.internal.*
import imgui.internal.sections.*
import imgui.statics.addTo
import java.util.*
import kotlin.math.max
import imgui.WindowFlag as Wf

/** Storage for one window
 *
 *  ImGuiWindow is mostly a dumb struct. It merely has a constructor and a few helper methods */
class Window(
        /** Parent UI context (needs to be set explicitly by parent). */
        val ctx: Context,
        /** Window name, owned by the window. */
        var name: String) {

    /** == ImHashStr(Name) */
    val id: ID = hashStr(name)

    /** See enum ImGuiWindowFlags_ */
    var flags: WindowFlags = none

    /** Always set in Begin(). Inactive windows may have a NULL value here if their viewport was discarded. */
    var viewport: ViewportP? = null

    /** Position (always rounded-up to nearest pixel)    */
    var pos = Vec2()

    /** Current size (==SizeFull or collapsed title bar size)   */
    val size = Vec2()

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

    /** Left/Up offsets. Sum of non-scrolling outer decorations (X1 generally == 0.0f. Y1 generally = TitleBarHeight + MenuBarHeight). Locked during Begin(). */
    var decoOuterSizeX1 = 0f
    var decoOuterSizeY1 = 0f

    /** Right/Down offsets (X2 generally == ScrollbarSize.x, Y2 == ScrollbarSizes.y). */
    var decoOuterSizeX2 = 0f
    var decoOuterSizeY2 = 0f

    /** Applied AFTER/OVER InnerRect. Specialized for Tables as they use specialized form of clipping and frozen rows/columns are inside InnerRect (and not part of regular decoration sizes). */
    var decoInnerSizeX1 = 0f
    var decoInnerSizeY1 = 0f

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

    /** Set when passed _ChildWindow, left to false by BeginDocked() */
    var isExplicitChild = false

    /** Set when the window has a close button (p_open != NULL) */
    var hasCloseButton = false

    /** Current border being held for resize (-1: none, otherwise 0-3) */
    var resizeBorderHeld = 0

    /** Number of Begin() during the current frame (generally 0 or 1, 1+ if appending via multiple Begin/End pairs) */
    var beginCount = 0

    /** Number of Begin() during the previous frame */
    var beginCountPreviousFrame = 0

    /** Begin() order within immediate parent window, if we are a child window. Otherwise 0. */
    var beginOrderWithinParent = 0

    /** Begin() order within entire imgui context. This is mostly used for debugging submission order related issues. */
    var beginOrderWithinContext = 0

    /** Order within WindowsFocusOrder[], altered when windows are focused. */
    var focusOrder = 0

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
    var setWindowPosAllowFlags: CondFlags = Cond.Always or Cond.Once or Cond.FirstUseEver or Cond.Appearing

    /** store acceptable condition flags for SetNextWindowSize() use.    */
    var setWindowSizeAllowFlags: CondFlags = Cond.Always or Cond.Once or Cond.FirstUseEver or Cond.Appearing

    /** store acceptable condition flags for SetNextWindowCollapsed() use.   */
    var setWindowCollapsedAllowFlags: CondFlags = Cond.Always or Cond.Once or Cond.FirstUseEver or Cond.Appearing

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
    var innerRect = Rect() // Clear so the InnerRect.GetSize() code in Begin() doesn't lead to overflow even if the result isn't used.

    /**  == InnerRect shrunk by WindowPadding*0.5f on each side, clipped within viewport or parent clip rect. */
    val innerClipRect = Rect()

    /** Initially covers the whole scrolling region. Reduced by containers e.g columns/tables when active.
     * Shrunk by WindowPadding*1.0f on each side. This is meant to replace ContentRegionRect over time (from 1.71+ onward). */
    val workRect = Rect()

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
        _data = ctx.drawListSharedData
        _ownerName = name
    }

    /** If we are a child _or_ popup _or_ docked window, this is pointing to our parent. Otherwise NULL.  */
    var parentWindow: Window? = null

    var parentWindowInBeginStack: Window? = null

    /** Point to ourself or first ancestor that is not a child window. Doesn't cross through popups/dock nodes. */
    var rootWindow: Window? = null

    /** Point to ourself or first ancestor that is not a child window. Cross through popups parent<>child. */
    var rootWindowPopupTree: Window? = null

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

    /** Preferred X/Y position updated when moving on a given axis, reset to FLT_MAX. */
    val navPreferredScoringPosRel = Array(NavLayer.COUNT) { Vec2(Float.MAX_VALUE) }

    /** Focus Scope ID at the time of Begin() */
    var navRootFocusScopeId: ID = 0


    /** Backup of last idx/vtx count, so when waking up the window we can preallocate and avoid iterative alloc/copy */
    var memoryDrawListIdxCapacity = 0
    var memoryDrawListVtxCapacity = 0

    /** Set when window extraneous data have been garbage collected */
    var memoryCompacted = false

    /** calculate unique ID (hash of whole ID stack + given parameter). useful if you want to query into ImGuiStorage yourself  */
    fun getID(strID: String, end: Int = 0): ID {
        val seed: ID = idStack.last()
        val id: ID = hashStr(strID, end, seed)
        val g = ctx
        if (g.debugHookIdInfo == id)
            debugHookIdInfo(id, DataType._String, strID, end)
        return id
    }

    /** [JVM] */
    fun getID(ptrID: Any): ID {
        val seed: ID = idStack.last()
        val id: ID = hashData(System.identityHashCode(ptrID), seed)
        val g = ctx
        if (g.debugHookIdInfo == id)
            debugHookIdInfo(id, DataType._Pointer, ptrID)
        return id
    }

    /** [JVM] we hack the pointer version in this way */
    fun getID(intPtr: Long): ID {
//        if (intPtr >= ptrId.size) increase()
        val seed: ID = idStack.last()
        val id = hashData(System.identityHashCode(intPtr), seed)
//        val id = hashData(System.identityHashCode(ptrId[intPtr.i]), seed)
        val g = ctx
        if (g.debugHookIdInfo == id)
            debugHookIdInfo(id, DataType.Long, intPtr) // TODO check me
        return id
    }

    fun getID(n: Int): ID {
        val seed = idStack.last()
        val id = hashData(n, seed)
        val g = ctx
        if (g.debugHookIdInfo == id)
            debugHookIdInfo(id, DataType.Int, n)
        return id
    }

    private fun increase() {
        ptrId = Array(ptrId.size + 512) { i -> ptrId.getOrElse(i) { i } }
    }

    /** This is only used in rare/specific situations to manufacture an ID out of nowhere. */
    fun getIDFromRectangle(rAbs: Rect): ID {
        val seed: ID = idStack.last()
        val rRel = rectAbsToRel(rAbs)
        val id = hashData(intArrayOf(rRel.min.x.i, rRel.min.y.i, rRel.max.x.i, rRel.max.y.i), seed)
        return id
    }

    /** We don't use g.FontSize because the window may be != g.CurrentWindow. */
    fun rect(): Rect = Rect(pos.x.f, pos.y.f, pos.x + size.x, pos.y + size.y)

    fun calcFontSize(): Float {
        val g = ctx
        var scale = g.fontBaseSize * fontWindowScale
        parentWindow?.let { scale *= it.fontWindowScale }
        return scale
    }

    val titleBarHeight: Float
        get() = when {
            flags has Wf.NoTitleBar -> 0f
            else -> calcFontSize() + ctx.style.framePadding.y * 2f
        }

    fun titleBarRect(): Rect = Rect(pos, Vec2(pos.x + sizeFull.x, pos.y + titleBarHeight))
    val menuBarHeight: Float
        get() = when {
            flags has Wf.MenuBar -> dc.menuBarOffset.y + calcFontSize() + ctx.style.framePadding.y * 2f
            else -> 0f
        }

    fun menuBarRect(): Rect {
        val y1 = pos.y + titleBarHeight
        return Rect(pos.x.f, y1, pos.x + sizeFull.x, y1 + menuBarHeight)
    }


    // end class original methods


    /** ~ClampWindowPos, spare static */
    infix fun clampPos(visibilityRect: Rect) {
        val sizeForClamping = Vec2(size)
        if (io.configWindowsMoveFromTitleBarOnly && flags hasnt Wf.NoTitleBar)
            sizeForClamping.y = titleBarHeight
        pos put glm.clamp(pos, visibilityRect.min - sizeForClamping, visibilityRect.max)
    }

    //-------------------------------------------------------------------------
    // [SECTION] FORWARD DECLARATIONS
    //-------------------------------------------------------------------------


    // sparse static methods

    /** ~IsWindowContentHoverable */
    fun isContentHoverable(flags: HoveredFlags = none): Boolean { // An active popup disable hovering on other windows (apart from its own children)
        // FIXME-OPT: This could be cached/stored within the window.
        val focusedRootWindow = g.navWindow?.rootWindow ?: return true
        if (focusedRootWindow.wasActive && focusedRootWindow !== rootWindow) {
            // For the purpose of those flags we differentiate "standard popup" from "modal popup"
            // NB: The 'else' is important because Modal windows are also Popups.
            var wantInhibit = false
            if (focusedRootWindow.flags has Wf._Modal)
                wantInhibit = true
            else if (focusedRootWindow.flags has Wf._Popup && flags hasnt HoveredFlag.AllowWhenBlockedByPopup)
                wantInhibit = true

            // Inhibit hover unless the window is within the stack of our modal/popup
            if (wantInhibit)
                if (!rootWindow!!.isWithinBeginStackOf(focusedRootWindow))
                    return false
        }
        return true
    }

    val isContentHoverable: Boolean
        get() = isContentHoverable(none)

    /** ~CalcWindowAutoFitSize */
    fun calcAutoFitSize(sizeContents: Vec2): Vec2 {
        val decorationWWithoutScrollbars = decoOuterSizeX1 + decoOuterSizeX2 - scrollbarSizes.x
        val decorationHWithoutScrollbars = decoOuterSizeY1 + decoOuterSizeY2 - scrollbarSizes.y
        val sizePad = windowPadding * 2f
        val sizeDesired = sizeContents + sizePad + Vec2(decorationWWithoutScrollbars, decorationHWithoutScrollbars)
        return when {
            flags has Wf._Tooltip -> sizeDesired // Tooltip always resize
            else -> { // Maximum window size is determined by the viewport size or monitor size
                val isPopup = flags has Wf._Popup
                val isMenu = flags has Wf._ChildMenu
                val sizeMin = Vec2(style.windowMinSize) // Popups and menus bypass style.WindowMinSize by default, but we give then a non-zero minimum size to facilitate understanding problematic cases (e.g. empty popups)
                if (isPopup || isMenu)
                    sizeMin minAssign 4f

                val availSize = ImGui.mainViewport.workSize
                val sizeAutoFit = glm.clamp(sizeDesired, sizeMin, glm.max(sizeMin, availSize - style.displaySafeAreaPadding * 2f))

                // When the window cannot fit all contents (either because of constraints, either because screen is too small),
                // we are growing the size on the other axis to compensate for expected scrollbar.
                // FIXME: Might turn bigger than ViewportSize-WindowPadding.
                val sizeAutoFitAfterConstraint = calcSizeAfterConstraint(sizeAutoFit)
                val willHaveScrollbarX = (sizeAutoFitAfterConstraint.x - sizePad.x - decorationWWithoutScrollbars < sizeContents.x && flags hasnt Wf.NoScrollbar && flags has Wf.HorizontalScrollbar) || flags has Wf.AlwaysHorizontalScrollbar
                val willHaveScrollbarY = (sizeAutoFitAfterConstraint.y - sizePad.y - decorationHWithoutScrollbars < sizeContents.y && flags hasnt Wf.NoScrollbar) || flags has Wf.AlwaysVerticalScrollbar
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
        drawList addTo viewport.drawDataBuilder.layers[layer]
        dc.childWindows.filter { it.isActiveAndVisible }  // Clipped children may have been marked not active
                .forEach { it addToDrawData layer }
    }

    /** ~GetWindowDisplayLayer */
    val displayLayer: Int
        get() = if (flags has Wf._Tooltip) 1 else 0

    /** Layer is locked for the root window, however child windows may use a different viewport (e.g. extruding menu)
     *  ~AddRootWindowToDrawData    */
    fun addRootToDrawData() {
        addToDrawData(displayLayer)
    }

    /** ~SetWindowConditionAllowFlags */
    fun setConditionAllowFlags(flags: CondFlags, enabled: Boolean) = if (enabled) {
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
        if (thickness == 0f)
            rect.max minusAssign 1
        return when (Dir.of(borderN)) {
            Dir.Left -> Rect(rect.min.x - thickness, rect.min.y + perpPadding, rect.min.x + thickness, rect.max.y - perpPadding)
            Dir.Right -> Rect(rect.max.x - thickness, rect.min.y + perpPadding, rect.max.x + thickness, rect.max.y - perpPadding)
            Dir.Up -> Rect(rect.min.x + perpPadding, rect.min.y - thickness, rect.max.x - perpPadding, rect.min.y + thickness)
            Dir.Down -> Rect(rect.min.x + perpPadding, rect.max.y - thickness, rect.max.x - perpPadding, rect.max.y + thickness)
            else -> error("invalid dir")
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
            newSize maxAssign style.windowMinSize // Reduce artifacts with very small windows
            val minimumHeight = titleBarHeight + menuBarHeight + 0f max (g.style.windowRounding - 1f)
            newSize.y = newSize.y max minimumHeight // Reduce artifacts with very small windows
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
            contentSizeIdeal put this.contentSizeIdeal
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
            flags has Wf._Popup -> LocKey.WindowingPopup.msg
            flags has Wf.MenuBar && name == "##MainMenuBar" -> LocKey.WindowingMainMenuBar.msg
            else -> LocKey.WindowingUntitled.msg
        }

    /** ~ IsWindowActiveAndVisible, this is static natively */
    val isActiveAndVisible: Boolean get() = active && !hidden

    companion object {

        /** Data for resizing from corner */
        class ResizeGripDef(val cornerPosN: Vec2, val innerDir: Vec2, val angleMin12: Int, val angleMax12: Int)

        val resizeGripDef = arrayOf(
                ResizeGripDef(Vec2(1, 1), Vec2(-1, -1), 0, 3),  // Lower-right
                ResizeGripDef(Vec2(0, 1), Vec2(+1, -1), 3, 6),  // Lower-left
                ResizeGripDef(Vec2(0, 0), Vec2(+1, +1), 6, 9),  // Upper-left (Unused)
                ResizeGripDef(Vec2(1, 0), Vec2(-1, +1), 9, 12)) // Upper-right (Unused)

        /** Data for resizing from borders */
        class ResizeBorderDef(val innerDir: Vec2, val segmentN1: Vec2, val segmentN2: Vec2, val outerAngle: Float)

        val resizeBorderDef = arrayOf(
                ResizeBorderDef(Vec2(+1, 0), Vec2(0, 1), Vec2(0, 0), glm.πf * 1.0f),  // Left
                ResizeBorderDef(Vec2(-1, 0), Vec2(1, 0), Vec2(1, 1), glm.πf * 0.0f), // Right
                ResizeBorderDef(Vec2(0, +1), Vec2(0, 0), Vec2(1, 0), glm.πf * 1.5f), // Up
                ResizeBorderDef(Vec2(0, -1), Vec2(1, 1), Vec2(0, 1), glm.πf * 0.5f)) // Down

        fun getCombinedRootWindow(window_: Window, popupHierarchy: Boolean): Window {
            var window = window_
            var lastWindow: Window? = null
            while (lastWindow !== window) {
                lastWindow = window
                window = window.rootWindow!!
                if (popupHierarchy)
                    window = window.rootWindowPopupTree!!
            }
            return window
        }
    }

    override fun toString() = name
}

