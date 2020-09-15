package imgui.static

import gli_.has
import gli_.hasnt
import glm_.max
import glm_.min
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.destroyPlatformWindow
import imgui.ImGui.findViewportByID
import imgui.ImGui.io
import imgui.ImGui.isAnyMouseDown
import imgui.ImGui.isMousePosValid
import imgui.ImGui.scaleWindowsInViewport
import imgui.ImGui.translateWindowsInViewport
import imgui.api.g
import imgui.classes.*
import imgui.internal.NextWindowDataFlag
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.has
import imgui.internal.hasnt

// Viewports

/** Using an arbitrary constant instead of e.g. ImHashStr("ViewportDefault", 0); so it's easier to spot in the debugger. The exact value doesn't matter. */
const val IMGUI_VIEWPORT_DEFAULT_ID: ID = 0x11111111

/** FIXME: We should ideally refactor the system to call this every frame (we currently don't)
 *  [JVM] Safe Instance Vec2 */
fun addUpdateViewport(window: Window?, id: ID, pos: Vec2, size: Vec2, flags_: ViewportFlags): ViewportP {

    var flags = flags_
    assert(id != 0)

    if (window != null) {
        if (g.movingWindow?.rootWindow === window)
            flags = flags or (ViewportFlag.NoInputs or ViewportFlag.NoFocusOnAppearing)
        if (window.flags has WindowFlag.NoMouseInputs && window.flags has WindowFlag.NoNavInputs)
            flags = flags or ViewportFlag.NoInputs
        if (window.flags has WindowFlag.NoFocusOnAppearing)
            flags = flags or ViewportFlag.NoFocusOnAppearing
    }

    val viewport = (findViewportByID(id) as? ViewportP)?.also {
        if (!it.platformRequestMove)
            it.pos put pos
        if (!it.platformRequestResize)
            it.size put size
        it.flags = flags or (it.flags and ViewportFlag.Minimized) // Preserve existing flags
    } ?: ViewportP().also {
        // New viewport
        it.id = id
        it.idx = g.viewports.size
        it.pos put pos
        it.lastPos put pos
        it.size put size
        it.flags = flags
        updateViewportPlatformMonitor(it)
        g.viewports += it
        IMGUI_DEBUG_LOG_VIEWPORT("Add Viewport %08X (${window!!.name})".format(id))

        // We normally setup for all viewports in NewFrame() but here need to handle the mid-frame creation of a new viewport.
        // We need to extend the fullscreen clip rect so the OverlayDrawList clip is correct for that the first frame
        val r = g.drawListSharedData.clipRectFullscreen
        r.x = r.x min it.pos.x
        r.y = r.y min it.pos.y
        r.z = r.z max (it.pos.x + it.size.x)
        r.w = r.w max (it.pos.y + it.size.y)

        // Store initial DpiScale before the OS platform window creation, based on expected monitor data.
        // This is so we can select an appropriate font size on the first frame of our window lifetime
        if (it.platformMonitor != -1)
            it.dpiScale = g.platformIO.monitors[it.platformMonitor].dpiScale
    }

    viewport.window = window
    viewport.lastFrameActive = g.frameCount
    assert(window == null || viewport.id == window.id)

    window?.viewportOwned = true

    return viewport
}

/** Update viewports and monitor infos
 *  Note that this is running even if 'ImGuiConfigFlags_ViewportsEnable' is not set, in order to clear unused viewports (if any) and update monitor info. */
fun updateViewportsNewFrame() {

    assert(g.platformIO.viewports.size <= g.viewports.size)

    // Update Minimized status (we need it first in order to decide if we'll apply Pos/Size of the main viewport)
    val viewportsEnabled = g.configFlagsCurrFrame has ConfigFlag.ViewportsEnable
    if (viewportsEnabled) {
        g.viewports.forEach { viewport ->
            val platformFuncsAvailable = viewport.platformWindowCreated
            if (platformFuncsAvailable)
                g.platformIO.platform_GetWindowMinimized?.let {
                    val minimized = it(viewport)
                    viewport.flags = when {
                        minimized -> viewport.flags or ViewportFlag.Minimized
                        else -> viewport.flags wo ViewportFlag.Minimized
                    }
                }
        }
    }

    // Create/update main viewport with current platform position.
    // FIXME-VIEWPORT: Size is driven by back-end/user code for backward-compatibility but we should aim to make this more consistent.
    val mainViewport = g.viewports[0]
    assert(mainViewport.id == IMGUI_VIEWPORT_DEFAULT_ID)
    assert(mainViewport.window == null)
    val mainViewportPos = if (viewportsEnabled) Vec2(g.platformIO.platform_GetWindowPos!!.invoke(mainViewport)) else Vec2()
    val mainViewportSize = Vec2(io.displaySize)
    if (viewportsEnabled && mainViewport.flags has ViewportFlag.Minimized) {
        mainViewportPos put mainViewport.pos    // Preserve last pos/size when minimized (FIXME: We don't do the same for Size outside of the viewport path)
        mainViewportSize put mainViewport.size
    }
    addUpdateViewport(null, IMGUI_VIEWPORT_DEFAULT_ID, mainViewportPos, mainViewportSize, ViewportFlag.CanHostOtherWindows.i)

    g.currentDpiScale = 0f
    g.currentViewport = null
    g.mouseViewport = null
    var n = 0
    while (n < g.viewports.size) {
        val viewport = g.viewports[n]
        viewport.idx = n

        // Erase unused viewports
        if (n > 0 && viewport.lastFrameActive < g.frameCount - 2) {
            // Clear references to this viewport in windows (window->ViewportId becomes the master data)
            g.windows.forEach {
                if (it.viewport === viewport) {
                    it.viewport = null
                    it.viewportOwned = false
                }
            }
            if (viewport === g.mouseLastHoveredViewport)
                g.mouseLastHoveredViewport = null
            g.viewports.removeAt(n)

            // Destroy
            IMGUI_DEBUG_LOG_VIEWPORT("Delete Viewport %08X (${viewport.window?.name ?: "n/a"})".format(viewport.id))
            destroyPlatformWindow(viewport) // In most circumstances the platform window will already be destroyed here.
            assert(viewport !in g.platformIO.viewports)
            viewport.destroy()
            n--
            continue
        }

        val platformFuncsAvailable = viewport.platformWindowCreated
        if (viewportsEnabled) {
            // Update Position and Size (from Platform Window to ImGui) if requested.
            // We do it early in the frame instead of waiting for UpdatePlatformWindows() to avoid a frame of lag when moving/resizing using OS facilities.
            if (viewport.flags hasnt ViewportFlag.Minimized && platformFuncsAvailable) {
                if (viewport.platformRequestMove) {
                    viewport.pos put g.platformIO.platform_GetWindowPos!!.invoke(viewport)
                    viewport.lastPlatformPos put viewport.pos
                }
                if (viewport.platformRequestResize) {
                    viewport.size put g.platformIO.platform_GetWindowSize!!.invoke(viewport)
                    viewport.lastPlatformSize put viewport.size
                }
            }
        }

        // Update/copy monitor info
        updateViewportPlatformMonitor(viewport)

        // Lock down space taken by menu bars and status bars, reset the offset for fucntions like BeginMainMenuBar() to alter them again.
        viewport.workOffsetMin put viewport.currWorkOffsetMin
        viewport.workOffsetMax put viewport.currWorkOffsetMax
        viewport.currWorkOffsetMin put 0f
        viewport.currWorkOffsetMax put 0f

        // Reset alpha every frame. Users of transparency (docking) needs to request a lower alpha back.
        viewport.alpha = 1f

        // Translate imgui windows when a Host Viewport has been moved
        // (This additionally keeps windows at the same place when ImGuiConfigFlags_ViewportsEnable is toggled!)
        val viewportDeltaPos = viewport.pos - viewport.lastPos
        if (viewport.flags has ViewportFlag.CanHostOtherWindows && (viewportDeltaPos.x != 0f || viewportDeltaPos.y != 0f)) // TODO glm
            translateWindowsInViewport(viewport, viewport.lastPos, viewport.pos)

        // Update DPI scale
        val getDpi = g.platformIO.platform_GetWindowDpiScale
        val newDpiScale = when {
            getDpi != null && platformFuncsAvailable -> getDpi(viewport)
            viewport.platformMonitor != -1 -> g.platformIO.monitors[viewport.platformMonitor].dpiScale
            else -> viewport.dpiScale.takeIf { it != 0f } ?: 1f
        }
        if (viewport.dpiScale != 0f && newDpiScale != viewport.dpiScale) {
            val scaleFactor = newDpiScale / viewport.dpiScale
            if (io.configFlags has ConfigFlag.DpiEnableScaleViewports)
                scaleWindowsInViewport(viewport, scaleFactor)
            //if (viewport == GetMainViewport())
            //    g.PlatformInterface.SetWindowSize(viewport, viewport->Size * scale_factor);

            // Scale our window moving pivot so that the window will rescale roughly around the mouse position.
            // FIXME-VIEWPORT: This currently creates a resizing feedback loop when a window is straddling a DPI transition border.
            // (Minor: since our sizes do not perfectly linearly scale, deferring the click offset scale until we know the actual window scale ratio may get us slightly more precise mouse positioning.)
            //if (g.MovingWindow != NULL && g.MovingWindow->Viewport == viewport)
            //    g.ActiveIdClickOffset = ImFloor(g.ActiveIdClickOffset * scale_factor);
        }
        viewport.dpiScale = newDpiScale
        n++
    }

    if (!viewportsEnabled) {
        g.mouseViewport = mainViewport
        return
    }

    // Mouse handling: decide on the actual mouse viewport for this frame between the active/focused viewport and the hovered viewport.
    // Note that 'viewport_hovered' should skip over any viewport that has the ImGuiViewportFlags_NoInputs flags set.
    var viewportHovered: ViewportP?
    if (io.backendFlags has BackendFlag.HasMouseHoveredViewport) {
        viewportHovered = if (io.mouseHoveredViewport != 0) findViewportByID(io.mouseHoveredViewport) as ViewportP else null
        if (viewportHovered?.flags?.has(ViewportFlag.NoInputs) == true) {
            // Back-end failed at honoring its contract if it returned a viewport with the _NoInputs flag.
            assert(false)
            viewportHovered = findHoveredViewportFromPlatformWindowStack(io.mousePos)
        }
    } else
    // If the back-end doesn't know how to honor ImGuiViewportFlags_NoInputs, we do a search ourselves. Note that this search:
    // A) won't take account of the possibility that non-imgui windows may be in-between our dragged window and our target window.
    // B) uses LastFrameAsRefViewport as a flawed replacement for the last time a window was focused (we could/should fix that by introducing Focus functions in PlatformIO)
        viewportHovered = findHoveredViewportFromPlatformWindowStack(io.mousePos)
    if (viewportHovered != null)
        g.mouseLastHoveredViewport = viewportHovered
    else if (g.mouseLastHoveredViewport == null)
        g.mouseLastHoveredViewport = g.viewports[0]

    // Update mouse reference viewport
    // (when moving a window we aim at its viewport, but this will be overwritten below if we go in drag and drop mode)
    g.mouseViewport = g.movingWindow?.viewport ?: g.mouseLastHoveredViewport

    // When dragging something, always refer to the last hovered viewport.
    // - when releasing a moving window we will revert to aiming behind (at viewport_hovered)
    // - when we are between viewports, our dragged preview will tend to show in the last viewport _even_ if we don't have tooltips in their viewports (when lacking monitor info)
    // - consider the case of holding on a menu item to browse child menus: even thou a mouse button is held, there's no active id because menu items only react on mouse release.
    val isMouseDraggingWithAnExpectedDestination = g.dragDropActive
    if (isMouseDraggingWithAnExpectedDestination && viewportHovered == null)
        viewportHovered = g.mouseLastHoveredViewport
    if (isMouseDraggingWithAnExpectedDestination || g.activeId == 0 || !isAnyMouseDown)
        if (viewportHovered != null && viewportHovered !== g.mouseViewport && viewportHovered.flags has ViewportFlag.NoInputs)
            g.mouseViewport = viewportHovered

    assert(g.mouseViewport != null)
}

/** Update user-facing viewport list (g.Viewports -> g.PlatformIO.Viewports after filtering out some) */
fun updateViewportsEndFrame() {
    g.platformIO.mainViewport = g.viewports[0]
    g.platformIO.viewports.clear()
    for (i in g.viewports.indices) {
        val viewport = g.viewports[i]
        viewport.lastPos put viewport.pos
        if (viewport.lastFrameActive < g.frameCount || viewport.size.x <= 0f || viewport.size.y <= 0f) // TODO glm
            if (i > 0) // Always include main viewport in the list
                continue
        val wnd = viewport.window
        if (wnd != null && !wnd.isActiveAndVisible)
            continue
        if (i > 0)
            assert(wnd != null)
        g.platformIO.viewports += viewport
    }
    g.viewports[0].clearRequestFlags() // Clear main viewport flags because UpdatePlatformWindows() won't do it and may not even be called
}

/** FIXME-VIEWPORT: This is all super messy and ought to be clarified or rewritten. */
fun updateSelectWindowViewport(window: Window) {

    val flags = window.flags
    window.viewportAllowPlatformMonitorExtend = -1

    // Restore main viewport if multi-viewport is not supported by the back-end
    val mainViewport = g.viewports[0]
    if (g.configFlagsCurrFrame hasnt ConfigFlag.ViewportsEnable) {
        setWindowViewport(window, mainViewport)
        return
    }
    window.viewportOwned = false

    // Appearing popups reset their viewport so they can inherit again
    if (flags has (WindowFlag._Popup or WindowFlag._Tooltip) && window.appearing) {
        window.viewport = null
        window.viewportId = 0
    }

    if (g.nextWindowData.flags hasnt NextWindowDataFlag.HasViewport) {
        // By default inherit from parent window
        if (window.viewport == null)
            window.parentWindow?.let {
                if (!it.isFallbackWindow)
                    window.viewport = it.viewport
            }

        // Attempt to restore saved viewport id (= window that hasn't been activated yet), try to restore the viewport based on saved 'window->ViewportPos' restored from .ini file
        if (window.viewport == null && window.viewportId != 0) {
            window.viewport = findViewportByID(window.viewportId) as? ViewportP
            if (window.viewport == null && window.viewportPos.x != Float.MAX_VALUE && window.viewportPos.y != Float.MAX_VALUE) // TODO glm
                window.viewport = addUpdateViewport(window, window.id, window.viewportPos, window.size, ViewportFlag.None.i)
        }
    }

    var lockViewport = false
    if (g.nextWindowData.flags has NextWindowDataFlag.HasViewport) {
        // Code explicitly request a viewport
        window.viewport = findViewportByID(g.nextWindowData.viewportId) as? ViewportP
        window.viewportId = g.nextWindowData.viewportId // Store ID even if Viewport isn't resolved yet.
        lockViewport = true
    } else if (flags has WindowFlag._ChildWindow || flags has WindowFlag._ChildMenu)
    // Always inherit viewport from parent window
        window.viewport = window.parentWindow!!.viewport
    else if (flags has WindowFlag._Tooltip)
        window.viewport = g.mouseViewport
    else if (getWindowAlwaysWantOwnViewport(window))
        window.viewport = addUpdateViewport(window, window.id, window.pos, window.size, ViewportFlag.None.i)
    else if (g.movingWindow != null && g.movingWindow!!.rootWindow === window && isMousePosValid()) {
        if (window.viewport != null && window.viewport!!.window === window)
            window.viewport = addUpdateViewport(window, window.id, window.pos, window.size, ViewportFlag.None.i)
    } else {
        // Merge into host viewport?
        // We cannot test window->ViewportOwned as it set lower in the function.
        val tryToMergeIntoHostViewport = window.viewport?.window === window && g.activeId == 0
        if (tryToMergeIntoHostViewport)
            updateTryMergeWindowIntoHostViewports(window)
    }

    // Fallback to default viewport
    if (window.viewport == null)
        window.viewport = mainViewport

    val vp = window.viewport
    // Mark window as allowed to protrude outside of its viewport and into the current monitor
    if (!lockViewport) {
        if (flags has (WindowFlag._Tooltip or WindowFlag._Popup)) {
            // We need to take account of the possibility that mouse may become invalid.
            // Popups/Tooltip always set ViewportAllowPlatformMonitorExtend so GetWindowAllowedExtentRect() will return full monitor bounds.
            val mouseRef = if (flags has WindowFlag._Tooltip) io.mousePos else g.beginPopupStack.last().openMousePos
            val useMouseRef = g.navDisableHighlight || !g.navDisableMouseHover || g.navWindow == null
            val mouseValid = isMousePosValid(mouseRef)
            window.viewportAllowPlatformMonitorExtend = when {
                (window.appearing || flags has (WindowFlag._Tooltip or WindowFlag._ChildMenu)) && (!useMouseRef || mouseValid) ->
                    findPlatformMonitorForPos(if (useMouseRef && mouseValid) mouseRef else navCalcPreferredRefPos())
                else -> vp!!.platformMonitor
            }
        } else if (vp != null && window !== vp.window && vp.window != null && flags hasnt WindowFlag._ChildWindow) {
            // When called from Begin() we don't have access to a proper version of the Hidden flag yet, so we replicate this code.
            val willBeVisible = !window.dockIsActive || window.dockTabIsVisible
            if (window.flags has WindowFlag._DockNodeHost && vp.lastFrameActive < g.frameCount && willBeVisible) {
                // Steal/transfer ownership
                IMGUI_DEBUG_LOG_VIEWPORT("Window '${window.name}' steal Viewport %08X from Window '${vp.window!!.name}'".format(vp.id))
                vp.window = window
                vp.id = window.id
                vp.lastNameHash = 0
            } else if (!updateTryMergeWindowIntoHostViewports(window)) // Merge?
            // New viewport
                window.viewport = addUpdateViewport(window, window.id, window.pos, window.size, ViewportFlag.NoFocusOnAppearing.i)
        } else if (window.viewportAllowPlatformMonitorExtend < 0 && flags hasnt WindowFlag._ChildWindow)
        // Regular (non-child, non-popup) windows by default are also allowed to protrude
        // Child windows are kept contained within their parent.
            window.viewportAllowPlatformMonitorExtend = vp!!.platformMonitor
    }

    // Update flags
    window.viewportOwned = window === vp!!.window
    window.viewportId = vp!!.id

    // If the OS window has a title bar, hide our imgui title bar
    //if (window->ViewportOwned && !(window->Viewport->Flags & ImGuiViewportFlags_NoDecoration))
    //    window->Flags |= ImGuiWindowFlags_NoTitleBar;
}

fun updateTryMergeWindowIntoHostViewport(window: Window, viewport: ViewportP): Boolean {

    if (window.viewport === viewport ||
            viewport.flags hasnt ViewportFlag.CanHostOtherWindows ||
            viewport.flags has ViewportFlag.Minimized ||
            window.rect() !in viewport.mainRect ||
            getWindowAlwaysWantOwnViewport(window))
        return false

    for (n in g.windows.indices) {
        val windowBehind = g.windows[n]
        if (windowBehind === window)
            break
        if (windowBehind.wasActive && windowBehind.viewportOwned && windowBehind.flags hasnt WindowFlag._ChildWindow)
            if (windowBehind.viewport!!.mainRect overlaps window.rect())
                return false
    }

    // Move to the existing viewport, Move child/hosted windows as well (FIXME-OPT: iterate child)
    val oldViewport = window.viewport
    if (window.viewportOwned)
        g.windows.forEach {
            if (it.viewport === oldViewport)
                setWindowViewport(it, viewport)
        }
    setWindowViewport(window, viewport)
    window.bringToDisplayFront()

    return true
}

fun updateTryMergeWindowIntoHostViewports(window: Window) = updateTryMergeWindowIntoHostViewport(window, g.viewports[0])

fun setCurrentViewport(currentWindow: Window?, viewport: ViewportP?) {

    viewport?.lastFrameActive = g.frameCount
    if (g.currentViewport === viewport)
        return
    g.currentDpiScale = viewport?.dpiScale ?: 1f
    g.currentViewport = viewport
    //IMGUI_DEBUG_LOG_VIEWPORT("SetCurrentViewport changed '%s' 0x%08X\n", current_window ? current_window->Name : NULL, viewport ? viewport->ID : 0);

    // Notify platform layer of viewport changes
    // FIXME-DPI: This is only currently used for experimenting with handling of multiple DPI
    g.currentViewport?.let {
        g.platformIO.platform_OnChangedViewport?.invoke(it)
    }
}

fun getWindowAlwaysWantOwnViewport(window: Window): Boolean =
        // Tooltips and menus are not automatically forced into their own viewport when the NoMerge flag is set, however the multiplication of viewports makes them more likely to protrude and create their own.
        (io.configViewportsNoAutoMerge || window.windowClass.viewportFlagsOverrideSet has ViewportFlag.NoAutoMerge) &&
                g.configFlagsCurrFrame has ConfigFlag.ViewportsEnable &&
                !window.dockIsActive &&
                window.flags hasnt (WindowFlag._ChildWindow or WindowFlag._ChildMenu or WindowFlag._Tooltip) &&
                (window.flags hasnt WindowFlag._Popup || window.flags has WindowFlag._Modal)

/** [JVM] Safe Instance Vec2 */
fun findPlatformMonitorForPos(pos: Vec2): Int = g.platformIO.monitors.indexOfFirst { pos in Rect(it.mainPos, it.mainPos + it.mainSize) }

/** Search for the monitor with the largest intersection area with the given rectangle
 *  We generally try to avoid searching loops but the monitor count should be very small here
 *  FIXME-OPT: We could test the last monitor used for that viewport first, and early */
fun findPlatformMonitorForRect(rect: Rect): Int {

    val monitorCount = g.platformIO.monitors.size
    if (monitorCount <= 1)
        return monitorCount - 1

    // Use a minimum threshold of 1.0f so a zero-sized rect won't false positive, and will still find the correct monitor given its position.
    // This is necessary for tooltips which always resize down to zero at first.
    val surfaceThreshold = (rect.width * rect.height * 0.5f) max 1f
    var bestMonitorN = -1
    var bestMonitorSurface = 0.001f

    var monitorN = 0
    while (monitorN < g.platformIO.monitors.size && bestMonitorSurface < surfaceThreshold) {
        val monitor = g.platformIO.monitors[monitorN]
        val monitorRect = Rect(monitor.mainPos, monitor.mainPos + monitor.mainSize)
        if (rect in monitorRect)
            return monitorN
        val overlappingRect = Rect(rect)
        overlappingRect clipWithFull monitorRect
        val overlappingSurface = overlappingRect.width * overlappingRect.height
        if (overlappingSurface >= bestMonitorSurface) {
            bestMonitorSurface = overlappingSurface
            bestMonitorN = monitorN
        }
        monitorN++
    }
    return bestMonitorN
}

/** Update monitor from viewport rectangle (we'll use this info to clamp windows and save windows lost in a removed monitor) */
fun updateViewportPlatformMonitor(viewport: ViewportP) {
    viewport.platformMonitor = findPlatformMonitorForRect(viewport.mainRect)
}

// Spaiata

// If the back-end doesn't set MouseLastHoveredViewport or doesn't honor ImGuiViewportFlags_NoInputs, we do a search ourselves.
// A) It won't take account of the possibility that non-imgui windows may be in-between our dragged window and our target window.
// B) It requires Platform_GetWindowFocus to be implemented by back-end.
fun findHoveredViewportFromPlatformWindowStack(mousePlatformPos: Vec2): ViewportP? {
    var bestCandidate: ViewportP? = null
    g.viewports.forEach {
        if (it.flags hasnt (ViewportFlag.NoInputs or ViewportFlag.Minimized) && mousePlatformPos in it.mainRect)
            if (bestCandidate == null || bestCandidate!!.lastFrontMostStampCount < it.lastFrontMostStampCount)
                bestCandidate = it
    }
    return bestCandidate
}

// Spaiata
fun setWindowViewport(window: Window, viewport: ViewportP) {
    window.viewport = viewport
    window.viewportId = viewport.id
    window.viewportOwned = viewport.window === window
}