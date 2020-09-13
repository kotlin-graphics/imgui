package imgui.static

import gli_.has
import glm_.L
import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.createNewWindowSettings
import imgui.ImGui.findWindowByID
import imgui.ImGui.findWindowSettings
import imgui.ImGui.io
import imgui.ImGui.style
import imgui.api.g
import imgui.classes.Context
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.hash
import imgui.internal.sections.SettingsHandler
import imgui.internal.sections.WindowSettings
import imgui.windowsIme.COMPOSITIONFORM
import imgui.windowsIme.DWORD
import imgui.windowsIme.HIMC
import imgui.windowsIme.imm
import org.lwjgl.system.MemoryUtil
import uno.glfw.HWND
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

/** Find window given position, search front-to-back
FIXME: Note that we have an inconsequential lag here: OuterRectClipped is updated in Begin(), so windows moved programmatically
with SetWindowPos() and not SetNextWindowPos() will have that rectangle lagging by a frame at the time FindHoveredWindow() is
called, aka before the next Begin(). Moving window isn't affected..    */
fun findHoveredWindow() {

    var hoveredWindow = g.movingWindow?.takeIf { it.flags hasnt WindowFlag.NoMouseInputs }

    val paddingRegular = Vec2(style.touchExtraPadding)
    val paddingForResizeFromEdges = when {
        io.configWindowsResizeFromEdges -> glm.max(style.touchExtraPadding, Vec2(WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS))
        else -> paddingRegular
    }

    var i = g.windows.lastIndex
    while (i >= 0 && hoveredWindow == null) {
        val window = g.windows[i]
        if (window.active && !window.hidden && window.flags hasnt WindowFlag.NoMouseInputs) {
            // Using the clipped AABB, a child window will typically be clipped by its parent (not always)
            val bb = Rect(window.outerRectClipped)
            if (window.flags has (WindowFlag._ChildWindow or WindowFlag.NoResize or WindowFlag.AlwaysAutoResize))
                bb expand paddingRegular
            else
                bb expand paddingForResizeFromEdges
            if (io.mousePos in bb) {
                // Those seemingly unnecessary extra tests are because the code here is a little different in viewport/docking branches.
                hoveredWindow = window
                break
            }
        }
        i--
    }

    g.hoveredWindow = hoveredWindow
    g.hoveredRootWindow = g.hoveredWindow?.rootWindow
}

// ApplyWindowSettings -> Window class

fun createNewWindow(name: String, flags: WindowFlags) = Window(g, name).apply {

    //IMGUI_DEBUG_LOG("CreateNewWindow '%s', flags = 0x%08X\n", name, flags);

    // Create window the first time
    this.flags = flags
    g.windowsById[id] = this

    // Default/arbitrary window position. Use SetNextWindowPos() with the appropriate condition flag to change the initial position of a window.
    pos put 60f

    // User can disable loading and saving of settings. Tooltip and child windows also don't store settings.
    if (flags hasnt WindowFlag.NoSavedSettings) {
        findWindowSettings(id)?.let { settings ->
            //  Retrieve settings from .ini file
            settingsOffset = g.settingsWindows.indexOf(settings)
            setConditionAllowFlags(Cond.FirstUseEver.i, false)
            applySettings(settings)
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
        line.startsWith("Pos") -> settings.pos put line.substring(4).split(",")
        line.startsWith("Size") -> settings.size put line.substring(5).split(",")
        line.startsWith("Collapsed") -> settings.collapsed = line.substring(10).toBoolean()
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
        settings.pos put window.pos
        settings.size put window.sizeFull
        settings.collapsed = window.collapsed
    }

    // Write to text buffer
    for (setting in g.settingsWindows)
        // all numeric fields to ints to have full c++ compatibility
        buf += """|[${handler.typeName}][${setting.name}]
                  |Pos=${setting.pos.x.i},${setting.pos.y.i}
                  |Size=${setting.size.x.i},${setting.size.y.i}
                  |Collapsed=${setting.collapsed.i} 
                  |""".trimMargin()
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
    val hwnd: HWND = io.imeWindowHandle
    if (hwnd.L != MemoryUtil.NULL) {
        val himc: HIMC = HIMC(imm.getContext(hwnd))
        if (himc.L != MemoryUtil.NULL) {
            val cf = COMPOSITIONFORM().apply {
                ptCurrentPos.x = x.L
                ptCurrentPos.y = y.L
                dwStyle = DWORD(imm.CFS_FORCE_POSITION.L)
            }
            if (imm.setCompositionWindow(himc, cf) == 0)
                System.err.println("imm.setCompositionWindow failed")
            if (imm.releaseContext(hwnd, himc) == 0)
                System.err.println("imm.releaseContext failed")
            cf.free()
        }
    }
}