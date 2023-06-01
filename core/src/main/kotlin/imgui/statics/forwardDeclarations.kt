package imgui.statics

import glm_.i
import glm_.max
import glm_.min
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.createNewWindowSettings
import imgui.ImGui.findWindowByID
import imgui.ImGui.findWindowSettingsByID
import imgui.ImGui.findWindowSettingsByWindow
import imgui.ImGui.io
import imgui.ImGui.style
import imgui.WindowFlag
import imgui.api.g
import imgui.classes.Context
import imgui.classes.DrawList
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.floor
import imgui.internal.hashStr
import imgui.internal.sections.DrawListFlag
import imgui.internal.sections.SettingsHandler
import imgui.internal.sections.WindowSettings
import kool.rem
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import imgui.WindowFlag as Wf


//-------------------------------------------------------------------------
// [SECTION] FORWARD DECLARATIONS
//-------------------------------------------------------------------------

fun setCurrentWindow(window: Window?) {
    g.currentWindow = window
    g.currentTable = when {
        window != null && window.dc.currentTableIdx != -1 -> g.tables.getByIndex(window.dc.currentTableIdx)
        else -> null
    }
    if (window != null) {
        g.fontSize = window.calcFontSize()
        g.drawListSharedData.fontSize = g.fontSize
    }
}

/** Find window given position, search front-to-back
FIXME: Note that we have an inconsequential lag here: OuterRectClipped is updated in Begin(), so windows moved programmatically
with SetWindowPos() and not SetNextWindowPos() will have that rectangle lagging by a frame at the time FindHoveredWindow() is
called, aka before the next Begin(). Moving window isn't affected..    */
fun findHoveredWindow() {

    var hoveredWindow = g.movingWindow?.takeIf { it.flags hasnt Wf.NoMouseInputs }
    var hoveredWindowIgnoringMovingWindow: Window? = null

    val paddingRegular = style.touchExtraPadding // [JVM] careful, no copy
    val paddingForResize = when { // [JVM] careful, no copy
        io.configWindowsResizeFromEdges -> g.windowsHoverPadding
        else -> paddingRegular
    }

    for (window in g.windows.asReversed()) {
        if (!window.active || window.hidden)
            continue
        if (window.flags has Wf.NoMouseInputs)
            continue

        // Using the clipped AABB, a child window will typically be clipped by its parent (not always)
        val bb = Rect(window.outerRectClipped) // [JVM] we need a copy
        bb expand when {
            window.flags has (Wf._ChildWindow or Wf.NoResize or Wf.AlwaysAutoResize) -> paddingRegular
            else -> paddingForResize
        }

        if (io.mousePos !in bb)
            continue

        // Support for one rectangular hole in any given window
        // FIXME: Consider generalizing hit-testing override (with more generic data, callback, etc.) (#1512)
        if (window.hitTestHoleSize.x != 0) {
            val holePos = window.pos + window.hitTestHoleOffset
            val holeSize = window.hitTestHoleSize
            if (Rect(holePos, holePos + holeSize).contains(io.mousePos))
                continue
        }

        if (hoveredWindow == null)
            hoveredWindow = window
        val moving = g.movingWindow
        if (hoveredWindowIgnoringMovingWindow == null && (moving == null || window.rootWindow != moving.rootWindow))
            hoveredWindowIgnoringMovingWindow = window
        if (hoveredWindow != null && hoveredWindowIgnoringMovingWindow != null)
            break
    }

    g.hoveredWindow = hoveredWindow
    g.hoveredWindowUnderMovingWindow = hoveredWindow
}

/** ~ApplyWindowSettings */
infix fun Window.applySettings(settings: WindowSettings) {
    pos put floor(Vec2(settings.pos))
    if (settings.size allGreaterThan 0f) sizeFull put floor(Vec2(settings.size))
    size put sizeFull
    collapsed = settings.collapsed
}

fun updateWindowInFocusOrderList(window: Window, justCreated: Boolean, newFlags: WindowFlags) {

    val newIsExplicitChild = newFlags has Wf._ChildWindow && (newFlags hasnt Wf._Popup || newFlags has Wf._ChildMenu)
    val childFlagChanged = newIsExplicitChild != window.isExplicitChild
    if ((justCreated || childFlagChanged) && !newIsExplicitChild) {
        assert(window !in g.windowsFocusOrder)
        g.windowsFocusOrder += window
        window.focusOrder = g.windowsFocusOrder.lastIndex
    } else if (!justCreated && childFlagChanged && newIsExplicitChild) {
        assert(g.windowsFocusOrder[window.focusOrder] === window)
        for (n in window.focusOrder + 1 until g.windowsFocusOrder.size)
            g.windowsFocusOrder[n].focusOrder--
        g.windowsFocusOrder.removeAt(window.focusOrder)
        window.focusOrder = -1
    }
    window.isExplicitChild = newIsExplicitChild
}

fun initOrLoadWindowSettings(window: Window, settings: WindowSettings?) {

    // Initial window state with e.g. default/arbitrary window position
    // Use SetNextWindowPos() with the appropriate condition flag to change the initial position of a window.
    val mainViewport = ImGui.mainViewport
    window.pos = mainViewport.pos + 60
    window.setWindowCollapsedAllowFlags = Cond.Always / Cond.Once / Cond.FirstUseEver / Cond.Appearing
    window.setWindowSizeAllowFlags = window.setWindowCollapsedAllowFlags
    window.setWindowPosAllowFlags = window.setWindowSizeAllowFlags

    if (settings != null) {
        window.setConditionAllowFlags(Cond.FirstUseEver, false)
        window applySettings settings
    }
    window.dc.cursorStartPos put window.pos; window.dc.cursorMaxPos put window.pos; window.dc.idealMaxPos put window.pos // So first call to CalcWindowContentSizes() doesn't return crazy values

    if (window.flags hasnt WindowFlag.AlwaysAutoResize) {
        window.autoFitFrames put 2
        window.autoFitOnlyGrows = false
    } else {
        if (window.size.x <= 0f)
            window.autoFitFrames.x = 2
        if (window.size.y <= 0f)
            window.autoFitFrames.y = 2
        window.autoFitOnlyGrows = window.autoFitFrames.x > 0 || window.autoFitFrames.y > 0
    }
}

fun createNewWindow(name: String, flags: WindowFlags): Window {
    // Create window the first time
    //IMGUI_DEBUG_LOG("CreateNewWindow '%s', flags = 0x%08X\n", name, flags);
    val window = Window(g, name)
    window.flags = flags
    g.windowsById[window.id] = window

    var settings: WindowSettings? = null
    if (flags hasnt WindowFlag.NoSavedSettings) {
        settings = findWindowSettingsByWindow(window)
        if (settings != null)
            window.settingsOffset = g.settingsWindows.indexOf(settings)
    }

    initOrLoadWindowSettings(window, settings)

    if (flags has WindowFlag.NoBringToFrontOnFocus)
        g.windows.add(0, window) // Quite slow but rare and only once
    else
        g.windows += window

    return window
}

// Helper to snap on edges when aiming at an item very close to the edge,
// So the difference between WindowPadding and ItemSpacing will be in the visible area after scrolling.
// When we refactor the scrolling API this may be configurable with a flag?
// Note that the effect for this won't be visible on X axis with default Style settings as WindowPadding.x == ItemSpacing.x by default.
fun calcScrollEdgeSnap(target: Float, snapMin: Float, snapMax: Float, snapThreshold: Float, centerRatio: Float): Float = when {
    target <= snapMin + snapThreshold -> imgui.internal.lerp(snapMin, target, centerRatio)
    target >= snapMax - snapThreshold -> imgui.internal.lerp(target, snapMax, centerRatio)
    else -> target
}

fun Window.calcNextScrollFromScrollTargetAndClamp(): Vec2 {
    val scroll = Vec2(scroll)
    val decorationSize = Vec2(decoOuterSizeX1 + decoInnerSizeX1 + decoOuterSizeX2, decoOuterSizeY1 + decoInnerSizeY1 + decoOuterSizeY2)
    for (axis in 0..1) {
        if (scrollTarget[axis] < Float.MAX_VALUE) {
            val centerRatio = scrollTargetCenterRatio[axis]
            var scrollTarget = scrollTarget[axis]
            if (scrollTargetEdgeSnapDist[axis] > 0f) {
                val snapMin = 0f
                val snapMax = scrollMax[axis] + sizeFull[axis] - decorationSize[axis]
                scrollTarget = calcScrollEdgeSnap(scrollTarget, snapMin, snapMax, scrollTargetEdgeSnapDist[axis], centerRatio)
            }
            scroll[axis] = scrollTarget - centerRatio * (sizeFull[axis] - decorationSize[axis])
        }
        scroll[axis] = floor(scroll[axis] max 0f)
        if (!collapsed && !skipItems)
            scroll[axis] = scroll[axis] min scrollMax[axis]
    }
    return scroll
}


/** AddDrawListToDrawData */
infix fun DrawList.addTo(outList: ArrayList<DrawList>) {

    if (cmdBuffer.empty())
        return
    if (cmdBuffer.size == 1 && cmdBuffer[0].elemCount == 0 && cmdBuffer[0].userCallback == null)
        return

    /*  Draw list sanity check. Detect mismatch between PrimReserve() calls and incrementing _VtxCurrentIdx, _VtxWritePtr etc.
        May trigger for you if you are using PrimXXX functions incorrectly.   */
    assert(vtxBuffer.rem == 0 || _vtxWritePtr == vtxBuffer.rem)
    assert(idxBuffer.rem == 0 || _idxWritePtr == idxBuffer.rem)
    if (flags hasnt DrawListFlag.AllowVtxOffset)
        assert(_vtxCurrentIdx == vtxBuffer.rem)

    // JVM ImGui, this doesnt apply, we use Ints by default
    /*  Check that drawList doesn't use more vertices than indexable
        (default DrawIdx = unsigned short = 2 bytes = 64K vertices per DrawList = per window)
        If this assert triggers because you are drawing lots of stuff manually:
        - First, make sure you are coarse clipping yourself and not trying to draw many things outside visible bounds.
          Be mindful that the ImDrawList API doesn't filter vertices. Use the Metrics/Debugger window to inspect draw list contents.
        - If you want large meshes with more than 64K vertices, you can either:
          (A) Handle the ImDrawCmd::VtxOffset value in your renderer backend, and set 'io.BackendFlags |= ImGuiBackendFlags_RendererHasVtxOffset'.
              Most example backends already support this from 1.71. Pre-1.71 backends won't.
              Some graphics API such as GL ES 1/2 don't have a way to offset the starting vertex so it is not supported for them.
          (B) Or handle 32-bits indices in your renderer backend, and uncomment '#define ImDrawIdx unsigned int' line in imconfig.h.
              Most example backends already support this. For example, the OpenGL example code detect index size at compile-time:
                glDrawElements(GL_TRIANGLES, (GLsizei)pcmd->ElemCount, sizeof(ImDrawIdx) == 2 ? GL_UNSIGNED_SHORT : GL_UNSIGNED_INT, idx_buffer_offset);
              Your own engine or render API may use different parameters or function calls to specify index sizes.
              2 and 4 bytes indices are generally supported by most graphics API.
        - If for some reason neither of those solutions works for you, a workaround is to call BeginChild()/EndChild() before reaching
          the 64K limit to split your draw commands in multiple draw lists.         */
    outList += this
    io.metricsRenderVertices += vtxBuffer.rem
    io.metricsRenderIndices += idxBuffer.rem
}

// FIXME: Add a more explicit sort order in the window structure.
private val childWindowComparer = compareBy<Window>({ it.flags has Wf._Popup }, { it.flags has Wf._Tooltip }, { it.beginOrderWithinParent })

/** ~AddWindowToSortBuffer */
infix fun Window.addToSortBuffer(sortedWindows: ArrayList<Window>) {
    sortedWindows += this
    if (active) {
        val count = dc.childWindows.size
        if (count > 1) dc.childWindows.sortWith(childWindowComparer)
        dc.childWindows.filter { it.active }.forEach { it addToSortBuffer sortedWindows }
    }
}

//-----------------------------------------------------------------------------
// Settings
//-----------------------------------------------------------------------------

fun windowSettingsHandler_ClearAll(ctx: Context, handler: SettingsHandler) {
    val g = ctx
    g.windows.forEach { it.settingsOffset = -1 }
    g.settingsWindows.clear()
}

fun windowSettingsHandler_ReadOpen(ctx: Context, settingsHandler: SettingsHandler, name: String): WindowSettings {
    val id = hashStr(name)
    val settings = findWindowSettingsByID(id)
            ?.apply { clear() } // Clear existing if recycling previous entry
            ?: createNewWindowSettings(name)
    settings.id = id
    settings.wantApply = true
    return settings
}

fun windowSettingsHandler_ReadLine(ctx: Context, settingsHandler: SettingsHandler, entry: Any, line: String) {
    val settings = entry as WindowSettings
    when {
        line.startsWith("Pos") -> settings.pos put line.substring(4).split(",")
        line.startsWith("Size") -> settings.size put line.substring(5).split(",")
        line.startsWith("Collapsed") -> settings.collapsed = line.substring(10).toBoolean()
    }
}

/** Apply to existing windows (if any) */
fun windowSettingsHandler_ApplyAll(ctx: Context, handler: SettingsHandler) {
    val g = ctx
    for (settings in g.settingsWindows)
        if (settings.wantApply) {
            findWindowByID(settings.id)?.applySettings(settings)
            settings.wantApply = false
        }
}

fun windowSettingsHandler_WriteAll(ctx: Context, handler: SettingsHandler, buf: StringBuilder) {
    // Gather data from windows that were active during this session
    // (if a window wasn't opened in this session we preserve its settings)
    val g = ctx
    for (window in g.windows) {

        if (window.flags has Wf.NoSavedSettings)
            continue

        val settings = findWindowSettingsByWindow(window) ?: createNewWindowSettings(window.name).also {
            window.settingsOffset = g.settingsWindows.indexOf(it)
        }
        assert(settings.id == window.id)
        settings.pos put window.pos
        settings.size put window.sizeFull

        settings.collapsed = window.collapsed
        settings.wantDelete = true
    }

    // Write to text buffer
    for (setting in g.settingsWindows) {
        // all numeric fields to ints to have full c++ compatibility
        if (setting.wantDelete)
            continue
        buf += """
                [${handler.typeName}][${setting.name}]
                Pos=${setting.pos.x.i},${setting.pos.y.i}
                Size=${setting.size.x.i},${setting.size.y.i}
                Collapsed=${setting.collapsed.i}
                
                
                """.trimIndent() // [JVM] prefer trimIndent over trimMargin to preserve the last line
    }
}

//-----------------------------------------------------------------------------
// Platform dependent default implementations
//-----------------------------------------------------------------------------

val getClipboardTextFn_DefaultImpl: (userData: Any?) -> String? = {
    // Create a Clipboard object using getSystemClipboard() method
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    // Get data stored in the clipboard that is in the form of a string (text)
    clipboard.getData(DataFlavor.stringFlavor) as? String
}

val setClipboardTextFn_DefaultImpl: (userData: Any?, text: String) -> Unit = { _, text ->
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(StringSelection(text), null)
}

val setPlatformImeDataFn_DefaultImpl = { viewport: Viewport, data: PlatformImeData ->
    // Notify OS Input Method Editor of text input position

    //TODO()
//    val hwnd: HWND = viewport.platformHandleRaw as HWND
//
//    //    ::ImmAssociateContextEx(hwnd, NULL, data->WantVisible ? IACE_DEFAULT : 0);
//    if (hwnd.L == MemoryUtil.NULL) {
//
//        val himc: HIMC = HIMC(imm.getContext(hwnd))
//        if (himc.L != MemoryUtil.NULL) {
//            val compositionForm = COMPOSITIONFORM().apply {
//                ptCurrentPos.x = data.inputPos.x.L
//                ptCurrentPos.y = data.inputPos.y.L
//                dwStyle = DWORD(imm.CFS_FORCE_POSITION.L)
//            }
//            if (imm.setCompositionWindow(himc, compositionForm) == 0) System.err.println("imm::setCompositionWindow failed")
//            val candidateForm = CANDIDATEFORM().apply {
//                dwStyle = DWORD(imm.CFS_FORCE_POSITION.L)
//                ptCurrentPos.x = data.inputPos.x.L
//                ptCurrentPos.y = data.inputPos.y.L
//            }
//            if (imm.setCandidateWindow(himc, candidateForm) == 0) System.err.println("imm::setCandidateWindow failed")
//            if (imm.releaseContext(hwnd, himc) == 0) System.err.println("imm::releaseContext failed")
//            compositionForm.free()
//        }
//    }
}