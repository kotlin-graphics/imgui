package imgui.static

import gli_.has
import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.createNewWindowSettings
import imgui.ImGui.findWindowByID
import imgui.ImGui.findWindowSettings
import imgui.ImGui.io
import imgui.ImGui.mainViewport
import imgui.ImGui.style
import imgui.api.g
import imgui.classes.Context
import imgui.internal.classes.Rect
import imgui.internal.classes.SettingsHandler
import imgui.internal.classes.Window
import imgui.internal.classes.WindowSettings
import imgui.internal.floor
import imgui.internal.hash
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection


//-------------------------------------------------------------------------
// [SECTION] FORWARD DECLARATIONS
//-------------------------------------------------------------------------

fun setCurrentWindow(window: Window?) {
    g.currentWindow = window
    if (window != null)
        g.fontSize = window.calcFontSize()
    g.drawListSharedData.fontSize = g.fontSize
}

fun setWindowHitTestHole(window: Window, pos: Vec2, size: Vec2) {
    assert(window.hitTestHoleSize.x == 0f) { "We don't support multiple holes/hit test filters" }
    window.hitTestHoleSize put size
    window.hitTestHoleOffset put (pos - window.pos)
}

/** Find window given position, search front-to-back
FIXME: Note that we have an inconsequential lag here: OuterRectClipped is updated in Begin(), so windows moved programmatically
with SetWindowPos() and not SetNextWindowPos() will have that rectangle lagging by a frame at the time FindHoveredWindow() is
called, aka before the next Begin(). Moving window isn't affected..    */
fun findHoveredWindow() {

    // Special handling for the window being moved: Ignore the mouse viewport check (because it may reset/lose its viewport during the undocking frame)
    val movingWindowViewport = g.movingWindow?.viewport
    g.movingWindow?.viewport = g.mouseViewport

    var hoveredWindow = g.movingWindow?.takeIf { it.flags hasnt WindowFlag.NoMouseInputs }
    var hoveredWindowIgnoringMovingWindow: Window? = null

    val paddingRegular = Vec2(style.touchExtraPadding)
    val paddingForResizeFromEdges = when {
        io.configWindowsResizeFromEdges -> glm.max(style.touchExtraPadding, Vec2(WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS))
        else -> paddingRegular
    }

    for (i in g.windows.lastIndex downTo 0) {
        val window = g.windows[i]
        if (!window.active || window.hidden)
            continue
        if (window.flags has WindowFlag.NoMouseInputs)
            continue
        assert(window.viewport != null)
        if (window.viewport !== g.mouseViewport)
            continue

        // Using the clipped AABB, a child window will typically be clipped by its parent (not always)
        val bb = Rect(window.outerRectClipped)
        if (window.flags has (WindowFlag._ChildWindow or WindowFlag.NoResize or WindowFlag.AlwaysAutoResize))
            bb expand paddingRegular
        else
            bb expand paddingForResizeFromEdges
        if (io.mousePos !in bb)
            continue

        if (window.hitTestHoleSize.x != 0f) {
            // FIXME: Consider generalizing hit-testing override (with more generic data, callback, etc.) (#1512)
            val holeBb = Rect(window.hitTestHoleOffset.x.f, window.hitTestHoleOffset.y.f,
                    (window.hitTestHoleOffset.x + window.hitTestHoleSize.x).f, (window.hitTestHoleOffset.y + window.hitTestHoleSize.y).f)
            if (io.mousePos - window.pos in holeBb)
                continue
        }

        if (hoveredWindow == null)
            hoveredWindow = window
        if (hoveredWindowIgnoringMovingWindow == null && (g.movingWindow == null || window.rootWindow != g.movingWindow!!.rootWindow))
            hoveredWindowIgnoringMovingWindow = window
        if (hoveredWindowIgnoringMovingWindow != null)
            break
    }

    g.hoveredWindow = hoveredWindow
    g.hoveredRootWindow = g.hoveredWindow?.rootWindow
    g.hoveredWindowUnderMovingWindow = hoveredWindowIgnoringMovingWindow

    g.movingWindow?.viewport = movingWindowViewport
}

// ApplyWindowSettings -> Window class

fun createNewWindow(name: String, flags: WindowFlags) = Window(g, name).apply {

    //IMGUI_DEBUG_LOG("CreateNewWindow '%s', flags = 0x%08X\n", name, flags);

    // Create window the first time
    this.flags = flags
    g.windowsById[id] = this

    // Default/arbitrary window position. Use SetNextWindowPos() with the appropriate condition flag to change the initial position of a window.
    pos = mainViewport.pos + 60

    // User can disable loading and saving of settings. Tooltip and child windows also don't store settings.
    if (flags hasnt WindowFlag.NoSavedSettings) {
        findWindowSettings(id)?.let { setting ->
            //  Retrieve settings from .ini file
            settingsOffset = g.settingsWindows.indexOf(setting)
            setConditionAllowFlags(Cond.FirstUseEver.i, false)
            viewportPos put mainViewport.pos
            applySettings(setting)
        }
    }
    dc.cursorMaxPos put pos // So first call to CalcContentSize() doesn't return crazy values
    dc.cursorStartPos put pos

    if (flags has WindowFlag.AlwaysAutoResize) {
        autoFitFrames put 2
        autoFitOnlyGrows = false
    } else {
        if (this.size.x <= 0f) autoFitFrames.x = 2
        if (this.size.y <= 0f) autoFitFrames.y = 2
        autoFitOnlyGrows = autoFitFrames.x > 0 || autoFitFrames.y > 0
    }

    g.windowsFocusOrder += this
    if (flags has WindowFlag.NoBringToFrontOnFocus)
        g.windows.add(0, this) // Quite slow but rare and only once
    else g.windows += this
}

// CheckStacksSize, CalcNextScrollFromScrollTargetAndClamp and AddWindowToSortBuffer are Window class methods

// AddDrawListToDrawData is a DrawList class method

/** ~GetViewportRect */
val viewportRect: Rect
    get() = Rect(0f, 0f, io.displaySize.x.f, io.displaySize.y.f)

//-----------------------------------------------------------------------------
// Settings
//-----------------------------------------------------------------------------

fun windowSettingsHandler_ClearAll(ctx: Context, handler: SettingsHandler) {
    val g = ctx
    g.windows.forEach { it.settingsOffset = -1 }
    g.settingsWindows.clear()
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

fun windowSettingsHandler_ReadOpen(ctx: Context, settingsHandler: SettingsHandler, name: String): WindowSettings {
    val settings = findWindowSettings(hash(name)) ?: createNewWindowSettings(name)
    settings.wantApply = true
    return settings
}

fun windowSettingsHandler_ReadLine(ctx: Context, settingsHandler: SettingsHandler, entry: Any, line: String) {
    val settings = entry as WindowSettings
    when {
        line.startsWith("Pos") -> settings.pos put line.substring(3 + 1).split(',')
        line.startsWith("Size") -> settings.size put line.substring(4 + 1).split(',')
        line.startsWith("ViewportId") -> settings.viewportId = line.substring(10 + 3 + 1).toInt(16) // ViewportId=0x
        line.startsWith("ViewportPos") -> settings.viewportPos put line.substring(11 + 1).split(',')
        line.startsWith("Collapsed") -> settings.collapsed = line.substring(10).toBoolean()
        line.startsWith("DockId") -> {   // "DockId=0x%X,%d" or "DockId=0x%X"
            val values = line.substring(6 + 3).split(',')
            settings.dockId = values[0].toInt(16)
            settings.dockOrder = values.getOrNull(1)?.toInt(16) ?: -1
        }
        line.startsWith("ClassId") -> settings.classId = line.substring(7 + 3).toInt(16)
    }
}

fun windowSettingsHandler_WriteAll(ctx: Context, handler: SettingsHandler, buf: StringBuilder) {
    // Gather data from windows that were active during this session
    // (if a window wasn't opened in this session we preserve its settings)
    val g = ctx
    for (window in g.windows) {

        if (window.flags has WindowFlag.NoSavedSettings)
            continue

        val settings = when {
            window.settingsOffset != -1 -> g.settingsWindows[window.settingsOffset]
            else -> findWindowSettings(window.id) ?: createNewWindowSettings(window.name).also {
                window.settingsOffset = g.settingsWindows.indexOf(it)
            }
        }
        assert(settings.id == window.id)
        settings.pos put (window.pos - window.viewportPos)
        settings.size put window.sizeFull
        settings.viewportId = window.viewportId
        settings.viewportPos put window.viewportPos
        assert(window.dockNode == null || window.dockNode!!.id == window.dockId)
        settings.dockId = window.dockId
        settings.classId = window.windowClass.classId
        settings.dockOrder = window.dockOrder
        settings.collapsed = window.collapsed
    }

    // Write to text buffer
    for (settings in g.settingsWindows) {
        // all numeric fields to ints to have full c++ compatibility
        buf += "[${handler.typeName}][${settings.name}]\n"
        if (settings.viewportId != 0 && settings.viewportId != IMGUI_VIEWPORT_DEFAULT_ID) {
            buf += "ViewportPos=${settings.viewportPos.x},${settings.viewportPos.y}\n"
            buf += "ViewportId=0x%08X\n".format(settings.viewportId)
        }
        if (settings.pos.x != 0f || settings.pos.y != 0f || settings.viewportId == IMGUI_VIEWPORT_DEFAULT_ID)
            buf += "Pos=${settings.pos.x.i},${settings.pos.y.i}\n"
        if (settings.size.x != 0f || settings.size.y != 0f)
            buf += "Size=${settings.size.x.i},${settings.size.y.i}\n"
        buf += "Collapsed=${settings.collapsed.i}\n"
        if (settings.dockId != 0) {
            // Write DockId as 4 digits if possible. Automatic DockId are small numbers, but full explicit DockSpace() are full ImGuiID range.
            buf += "DockId=0x%08X".format(settings.dockId)
            buf += if (settings.dockOrder == -1) "\n" else ",${settings.dockOrder}\n"
            if (settings.classId != 0)
                buf += "ClassId=0x%08X\n".format(settings.classId)
        }
        buf += '\n'
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

var imeSetInputScreenPosFn_Win32 = { x: Int, y: Int ->
    // Notify OS Input Method Editor of text input position
    TODO()
//    val hwnd: HWND = io.imeWindowHandle
//    if (hwnd.L != MemoryUtil.NULL) {
//        val himc: HIMC = HIMC(imm.getContext(hwnd))
//        if (himc.L != MemoryUtil.NULL) {
//            val cf = COMPOSITIONFORM().apply {
//                ptCurrentPos.x = x.L
//                ptCurrentPos.y = y.L
//                dwStyle = DWORD(imm.CFS_FORCE_POSITION.L)
//            }
//            if (imm.setCompositionWindow(himc, cf) == 0)
//                System.err.println("imm.setCompositionWindow failed")
//            if (imm.releaseContext(hwnd, himc) == 0)
//                System.err.println("imm.releaseContext failed")
//            cf.free()
//        }
//    }
}