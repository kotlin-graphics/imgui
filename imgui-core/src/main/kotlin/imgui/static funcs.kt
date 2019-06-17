@file:Suppress("UNCHECKED_CAST")
@file:JvmName("ImGuiStaticFunctions")
package imgui

import gli_.has
import gli_.hasnt
import glm_.*
import glm_.vec2.Vec2
import imgui.ImGui.begin
import imgui.ImGui.clearActiveId
import imgui.ImGui.closePopupToLevel
import imgui.ImGui.closePopupsOverWindow
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.end
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.findWindowByName
import imgui.ImGui.frontMostPopupModal
import imgui.ImGui.getColumnOffset
import imgui.ImGui.getNavInputAmount
import imgui.ImGui.getNavInputAmount2d
import imgui.ImGui.io
import imgui.ImGui.isKeyDown
import imgui.ImGui.isMousePosValid
import imgui.ImGui.navInitWindow
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushStyleVar
import imgui.ImGui.selectable
import imgui.ImGui.setActiveId
import imgui.ImGui.setNavIDWithRectRel
import imgui.ImGui.setNavId
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setNextWindowSizeConstraints
import imgui.ImGui.style
import imgui.imgui.g
import imgui.imgui.imgui_colums.Companion.COLUMNS_HIT_RECT_HALF_WIDTH
import imgui.imgui.imgui_miscellaneousUtilities.Companion.getForegroundDrawList
import imgui.imgui.navRestoreLayer
import imgui.imgui.navScoreItem
import imgui.windowsIme.COMPOSITIONFORM
import imgui.windowsIme.DWORD
import imgui.windowsIme.HIMC
import imgui.windowsIme.imm
import imgui.internal.*
import org.lwjgl.system.MemoryUtil.NULL
import uno.glfw.HWND
import uno.kotlin.getValue
import uno.kotlin.isPrintable
import uno.kotlin.setValue
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import kotlin.collections.set
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KMutableProperty0
import imgui.ConfigFlag as Cf
import imgui.InputTextFlag as Itf
import imgui.WindowFlag as Wf


fun getDraggedColumnOffset(columns: Columns, columnIndex: Int): Float {
    /*  Active (dragged) column always follow mouse. The reason we need this is that dragging a column to the right edge
        of an auto-resizing window creates a feedback loop because we store normalized positions. So while dragging we
        enforce absolute positioning.   */

    val window = g.currentWindow!!
    assert(columnIndex > 0) { "We are not supposed to drag column 0." }
    assert(g.activeId == columns.id + columnIndex/* as ID */)

    var x = io.mousePos.x - g.activeIdClickOffset.x + COLUMNS_HIT_RECT_HALF_WIDTH - window.pos.x
    x = glm.max(x, getColumnOffset(columnIndex - 1) + style.columnsMinSpacing)
    if (columns.flags has ColumnsFlag.NoPreserveWidths)
        x = glm.min(x, getColumnOffset(columnIndex + 1) - style.columnsMinSpacing)

    return x
}

//-----------------------------------------------------------------------------
// Internal API exposed in imgui_internal.h
//-----------------------------------------------------------------------------

/** Find window given position, search front-to-back
FIXME: Note that we have an inconsequential lag here: OuterRectClipped is updated in Begin(), so windows moved programmatically
with SetWindowPos() and not SetNextWindowPos() will have that rectangle lagging by a frame at the time FindHoveredWindow() is
called, aka before the next Begin(). Moving window isn't affected..    */
fun findHoveredWindow() {

    var hoveredWindow = g.movingWindow?.takeIf { it.flags hasnt Wf.NoMouseInputs }

    val paddingRegular = Vec2(style.touchExtraPadding)
    val paddingForResizeFromEdges = when {
        io.configWindowsResizeFromEdges -> glm.max(style.touchExtraPadding, Vec2(WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS))
        else -> paddingRegular
    }

    var i = g.windows.lastIndex
    while (i >= 0 && hoveredWindow == null) {
        val window = g.windows[i]
        if (window.active && !window.hidden && window.flags hasnt Wf.NoMouseInputs) {
            // Using the clipped AABB, a child window will typically be clipped by its parent (not always)
            val bb = Rect(window.outerRectClipped)
            if (window.flags has (Wf.ChildWindow or Wf.NoResize or Wf.AlwaysAutoResize))
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

fun createNewWindow(name: String, size: Vec2, flags: Int) = Window(g, name).apply {
    // Create window the first time

    this.flags = flags
    g.windowsById[id] = this

    // Default/arbitrary window position. Use SetNextWindowPos() with the appropriate condition flag to change the initial position of a window.
    pos put 60f

    // User can disable loading and saving of settings. Tooltip and child windows also don't store settings.
    if (flags hasnt Wf.NoSavedSettings) {
        findWindowSettings(id)?.let { s ->
            //  Retrieve settings from .ini file
            settingsIdx = g.settingsWindows.indexOf(s)
            setConditionAllowFlags(Cond.FirstUseEver.i, false)
            pos = floor(s.pos)
            collapsed = s.collapsed
            if (s.size.lengthSqr > 0.00001f)
                size put floor(s.size)
        }
    }
    sizeFullAtLastBegin put floor(size)
    sizeFull put sizeFullAtLastBegin
    this.size put sizeFull
    dc.cursorMaxPos put pos // So first call to calcSizeContents() doesn't return crazy values

    if (flags has Wf.AlwaysAutoResize) {
        autoFitFrames put 2
        autoFitOnlyGrows = false
    } else {
        if (size.x <= 0f) autoFitFrames.x = 2
        if (size.y <= 0f) autoFitFrames.y = 2
        autoFitOnlyGrows = autoFitFrames.x > 0 || autoFitFrames.y > 0
    }

    g.windowsFocusOrder += this
    if (flags has Wf.NoBringToFrontOnFocus) g.windows.add(0, this) // Quite slow but rare and only once
    else g.windows.add(this)
}

/** Save and compare stack sizes on Begin()/End() to detect usage errors    */
fun checkStacksSize(window: Window, write: Boolean) {
    /*  NOT checking: DC.ItemWidth, DC.AllowKeyboardFocus, DC.ButtonRepeat, DC.TextWrapPos (per window) to allow user to
        conveniently push once and not pop (they are cleared on Begin)  */
    val backup = window.dc.stackSizesBackup
    var ptr = 0

    run {
        // Too few or too many PopID()/TreePop()
        val current = window.idStack.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] == current) {
            "PushID/PopID or TreeNode/TreePop Mismatch!"
        }
        ptr++
    }
    run {
        // Too few or too many EndGroup()
        val current = window.dc.groupStack.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] == current) { "BeginGroup/EndGroup Mismatch!" }
        ptr++
    }
    run {
        // Too few or too many EndMenu()/EndPopup()
        val current = g.beginPopupStack.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] == current) { "BeginMenu/EndMenu or BeginPopup/EndPopup Mismatch" }
        ptr++
    }
    // For color, style and font stacks there is an incentive to use Push/Begin/Pop/.../End patterns, so we relax our checks a little to allow them.
    run {
        // Too few or too many PopStyleColor()
        val current = g.colorModifiers.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] >= current) { "PushStyleColor/PopStyleColor Mismatch!" }
        ptr++
    }
    run {
        // Too few or too many PopStyleVar()
        val current = g.styleModifiers.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] >= current) { "PushStyleVar/PopStyleVar Mismatch!" }
        ptr++
    }
    run {
        // Too few or too many PopFont()
        val current = g.fontStack.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] >= current) { "PushFont/PopFont Mismatch!" }
        ptr++
    }
    assert(ptr == window.dc.stackSizesBackup.size)
}

fun calcNextScrollFromScrollTargetAndClamp(window: Window, snapOnEdges: Boolean): Vec2 {  // TODO -> window class?
    val scroll = Vec2(window.scroll)
    if (window.scrollTarget.x < Float.MAX_VALUE)
        scroll.x = window.scrollTarget.x - window.scrollTargetCenterRatio.x * (window.sizeFull.x - window.scrollbarSizes.x)
    if (window.scrollTarget.y < Float.MAX_VALUE) {
        /*  'snap_on_edges' allows for a discontinuity at the edge of scrolling limits to take account of WindowPadding
            so that scrolling to make the last item visible scroll far enough to see the padding.         */
        val crY = window.scrollTargetCenterRatio.y
        var targetY = window.scrollTarget.y
        if (snapOnEdges && crY <= 0f && targetY <= window.windowPadding.y)
            targetY = 0f
        if (snapOnEdges && crY >= 1f && targetY >= window.sizeContents.y - window.windowPadding.y + style.itemSpacing.y)
            targetY = window.sizeContents.y
        scroll.y = targetY - (1f - crY) * (window.titleBarHeight + window.menuBarHeight) - crY * (window.sizeFull.y - window.scrollbarSizes.y)
    }
    scroll maxAssign 0f
    if (!window.collapsed && !window.skipItems) {
        scroll.x = glm.min(scroll.x, window.scrollMaxX)
        scroll.y = glm.min(scroll.y, window.scrollMaxY)
    }
    return scroll
}

fun findWindowSettings(id: ID) = g.settingsWindows.firstOrNull { it.id == id }

fun createNewWindowSettings(name: String) = WindowSettings(name).also { g.settingsWindows += it }





val viewportRect get() = Rect(0f, 0f, io.displaySize.x.f, io.displaySize.y.f)

/** Return false to discard a character.    */
fun inputTextFilterCharacter(char: KMutableProperty0<Char>, flags: InputTextFlags, callback: InputTextCallback?, userData: Any?): Boolean {

    var c by char

    // Filter non-printable (NB: isprint is unreliable! see #2467) [JVM we can rely on custom ::isPrintable]
    if (c < 0x20 && !c.isPrintable) {
        var pass = false
        pass = pass or (c == '\n' && flags has Itf.Multiline)
        pass = pass or (c == '\t' && flags has Itf.AllowTabInput)
        if (!pass) return false
    }

    // Filter private Unicode range. GLFW on OSX seems to send private characters for special keys like arrow keys (FIXME)
    if (c >= 0xE000 && c <= 0xF8FF) return false

    // Generic named filters
    if (flags has (Itf.CharsDecimal or Itf.CharsHexadecimal or Itf.CharsUppercase or Itf.CharsNoBlank or Itf.CharsScientific)) {

        if (flags has Itf.CharsDecimal)
            if (c !in '0'..'9' && c != '.' && c != '-' && c != '+' && c != '*' && c != '/')
                return false

        if (flags has Itf.CharsScientific)
            if (c !in '0'..'9' && c != '.' && c != '-' && c != '+' && c != '*' && c != '/' && c != 'e' && c != 'E')
                return false

        if (flags has Itf.CharsHexadecimal)
            if (c !in '0'..'9' && c !in 'a'..'f' && c !in 'A'..'F')
                return false

        if (flags has Itf.CharsUppercase && c in 'a'..'z')
            c = c + ('A' - 'a') // cant += because of https://youtrack.jetbrains.com/issue/KT-14833

        if (flags has Itf.CharsNoBlank && c.isBlankW)
            return false
    }

    // Custom callback filter
    if (flags has Itf.CallbackCharFilter) {
        callback!! //callback is non-null from all calling functions
        val itcd = InputTextCallbackData()
        itcd.eventFlag = imgui.InputTextFlag.CallbackCharFilter.i
        itcd.eventChar = c
        itcd.flags = flags
        itcd.userData = userData

        if (callback(itcd) != 0)
            return false
        if (itcd.eventChar == NUL)
            return false
    }
    return true
}

fun inputTextCalcTextLenAndLineCount(text: String, outTextEnd: IntArray): Int {

    var lineCount = 0
    var s = 0
    while (s < text.length) // We are only matching for \n so we can ignore UTF-8 decoding
        if (text[s++] == '\n')
            lineCount++
    s--
    if (text[s] != '\n' && text[s] != '\r')
        lineCount++
    outTextEnd[0] = s
    return lineCount
}

fun inputTextCalcTextSizeW(text: CharArray, textBegin: Int, textEnd: Int, remaining: KMutableProperty0<Int>? = null,
                           outOffset: Vec2? = null, stopOnNewLine: Boolean = false): Vec2 {

    val font = g.font
    val lineHeight = g.fontSize
    val scale = lineHeight / font.fontSize

    val textSize = Vec2()
    var lineWidth = 0f

    var s = textBegin
    while (s < textEnd) {
        val c = text[s++]
        if (c == '\n') {
            textSize.x = glm.max(textSize.x, lineWidth)
            textSize.y += lineHeight
            lineWidth = 0f
            if (stopOnNewLine)
                break
            continue
        }
        if (c == '\r') continue
        // renaming ::getCharAdvance continuously every build because of bug, https://youtrack.jetbrains.com/issue/KT-19612
        val charWidth = font.getCharAdvance(c) * scale
        lineWidth += charWidth
    }

    if (textSize.x < lineWidth)
        textSize.x = lineWidth

    // offset allow for the possibility of sitting after a trailing \n
    outOffset?.let {
        it.x = lineWidth
        it.y = textSize.y + lineHeight
    }

    if (lineWidth > 0 || textSize.y == 0f)  // whereas size.y will ignore the trailing \n
        textSize.y += lineHeight

    remaining?.set(s)

    return textSize
}

// TODO check if needed
//fun IntArray.format(dataType: DataType, format: String, buf: CharArray): CharArray {
//    val value: Number = when (dataType) {
//        DataType.Int -> this[0]
//        DataType.Float -> glm.intBitsToFloat(this[0])
//        else -> throw Error()
//    }
//    return Format.format(style.locale, value).toCharArray(buf)
//}

/** JVM Imgui, dataTypeFormatString replacement TODO check if needed */
//fun IntArray.format(dataType: DataType, decimalPrecision: Int, buf: CharArray) = when (dataType) {
//
//    DataType.Int -> "%${if (decimalPrecision < 0) "" else ".$decimalPrecision"}d".format(style.locale, this[0])
///*  Ideally we'd have a minimum decimal precision of 1 to visually denote that it is a float, while hiding
//    non-significant digits?         */
//    DataType.Float -> "%${if (decimalPrecision < 0) "" else ".$decimalPrecision"}f".format(style.locale, glm.intBitsToFloat(this[0]))
//    DataType.Double -> TODO()//"%${if (decimalPrecision < 0) "" else ".$decimalPrecision"}f".format(style.locale, glm.intBitsToFloat(this[0]))
//    else -> throw Error("unsupported format data type")
//}.toCharArray(buf)
//
//fun KMutableProperty0<Number>.format(buf: CharArray, dataType: DataType, decimalPrecision: Int): Int { TODO REMOVE
//    val string = when {
//        decimalPrecision < 0 -> when (dataType) {
//            DataType.Int -> "%d".format(style.locale, this() as Int)
///*  Ideally we'd have a minimum decimal precision of 1 to visually denote that it is a float, while hiding
//    non-significant digits?         */
//            DataType.Float -> "%f".format(style.locale, this() as Float)
//            DataType.Double -> "%f".format(style.locale, this() as Double)
//            else -> throw Error("unsupported format data type")
//        }
//        else -> when (dataType) {
//            DataType.Int -> "%${decimalPrecision}d".format(style.locale, this() as Int)
//            DataType.Float -> "%${decimalPrecision}f".format(style.locale, this() as Float)
//            DataType.Double -> "%${decimalPrecision}g".format(style.locale, this() as Double)
//            else -> throw Error("unsupported format data type")
//        }
//    }
//    return string.toCharArray(buf).size
//}


/** Scroll to keep newly navigated item fully into view
 *  NB: We modify rect_rel by the amount we scrolled for, so it is immediately updated. */
fun navScrollToBringItemIntoView(window: Window, itemRect: Rect) {
    val windowRectRel = Rect(window.innerRect.min - 1, window.innerRect.max + 1)
    //GetOverlayDrawList(window)->AddRect(window->Pos + window_rect_rel.Min, window->Pos + window_rect_rel.Max, IM_COL32_WHITE); // [DEBUG]
    if (itemRect in windowRectRel) return

    if (window.scrollbar.x && itemRect.min.x < windowRectRel.min.x) {
        window.scrollTarget.x = itemRect.min.x - window.pos.x + window.scroll.x - style.itemSpacing.x
        window.scrollTargetCenterRatio.x = 0f
    } else if (window.scrollbar.x && itemRect.max.x >= windowRectRel.max.x) {
        window.scrollTarget.x = itemRect.max.x - window.pos.x + window.scroll.x + style.itemSpacing.x
        window.scrollTargetCenterRatio.x = 1f
    }
    if (itemRect.min.y < windowRectRel.min.y) {
        window.scrollTarget.y = itemRect.min.y - window.pos.y + window.scroll.y - style.itemSpacing.y
        window.scrollTargetCenterRatio.y = 0f
    } else if (itemRect.max.y >= windowRectRel.max.y) {
        window.scrollTarget.y = itemRect.max.y - window.pos.y + window.scroll.y + style.itemSpacing.y
        window.scrollTargetCenterRatio.y = 1f
    }
}

fun beginChildEx(name: String, id: ID, sizeArg: Vec2, border: Boolean, flags_: WindowFlags): Boolean {

    val parentWindow = g.currentWindow!!
    var flags = Wf.NoTitleBar or Wf.NoResize or Wf.NoSavedSettings or Wf.ChildWindow
    flags = flags or (parentWindow.flags and Wf.NoMove.i)  // Inherit the NoMove flag

    // Size
    val contentAvail = contentRegionAvail
    val size = floor(sizeArg)
    val autoFitAxes = (if (size.x == 0f) 1 shl Axis.X else 0x00) or (if (size.y == 0f) 1 shl Axis.Y else 0x00)
    if (size.x <= 0f)   // Arbitrary minimum child size (0.0f causing too much issues)
        size.x = glm.max(contentAvail.x + size.x, 4f)
    if (size.y <= 0f)
        size.y = glm.max(contentAvail.y + size.y, 4f)
    setNextWindowSize(size)

    // Build up name. If you need to append to a same child from multiple location in the ID stack, use BeginChild(ImGuiID id) with a stable value.
    val title = when {
        name.isNotEmpty() -> "${parentWindow.name}/$name".format(style.locale)
        else -> "${parentWindow.name}/%08X".format(style.locale, id)
    }
    val backupBorderSize = style.childBorderSize
    if (!border) style.childBorderSize = 0f
    flags = flags or flags_
    val ret = begin(title, null, flags)
    style.childBorderSize = backupBorderSize

    val childWindow = g.currentWindow!!.apply {
        childId = id
        autoFitChildAxes = autoFitAxes
    }

    // Set the cursor to handle case where the user called SetNextWindowPos()+BeginChild() manually.
    // While this is not really documented/defined, it seems that the expected thing to do.
    if (childWindow.beginCount == 1)
        parentWindow.dc.cursorPos put childWindow.pos

    // Process navigation-in immediately so NavInit can run on first frame
    if (g.navActivateId == id && flags hasnt Wf.NavFlattened && (childWindow.dc.navLayerActiveMask != 0 || childWindow.dc.navHasScroll)) {
        childWindow.focus()
        navInitWindow(childWindow, false)
        setActiveId(id + 1, childWindow) // Steal ActiveId with a dummy id so that key-press won't activate child item
        g.activeIdSource = InputSource.Nav
    }

    return ret
}

// Navigation
fun navUpdate() {

    io.wantSetMousePos = false

//    if (g.NavScoringCount > 0) printf("[%05d] NavScoringCount %d for '%s' layer %d (Init:%d, Move:%d)\n", g.FrameCount, g.NavScoringCount, g.NavWindow ? g . NavWindow->Name : "NULL", g.NavLayer, g.NavInitRequest || g.NavInitResultId != 0, g.NavMoveRequest)

    // Set input source as Gamepad when buttons are pressed before we map Keyboard (some features differs when used with Gamepad vs Keyboard)
    val navKeyboardActive = io.configFlags has Cf.NavEnableKeyboard
    val navGamepadActive = io.configFlags has Cf.NavEnableGamepad && io.backendFlags has BackendFlag.HasGamepad

    if (navGamepadActive)
        if (g.io.navInputs[NavInput.Activate] > 0f || g.io.navInputs[NavInput.Input] > 0f ||
                g.io.navInputs[NavInput.Cancel] > 0f || g.io.navInputs[NavInput.Menu] > 0f)
            g.navInputSource = InputSource.NavGamepad

    // Update Keyboard->Nav inputs mapping
    if (navKeyboardActive) {
        fun navMapKey(key: Key, navInput: NavInput) {
            if (isKeyDown(g.io.keyMap[key])) {
                g.io.navInputs[navInput] = 1f
                g.navInputSource = InputSource.NavKeyboard
            }
        }
        navMapKey(Key.Space, NavInput.Activate)
        navMapKey(Key.Enter, NavInput.Input)
        navMapKey(Key.Escape, NavInput.Cancel)
        navMapKey(Key.LeftArrow, NavInput.KeyLeft)
        navMapKey(Key.RightArrow, NavInput.KeyRight)
        navMapKey(Key.UpArrow, NavInput.KeyUp)
        navMapKey(Key.DownArrow, NavInput.KeyDown)
        navMapKey(Key.Tab, NavInput.KeyTab)
        if (io.keyCtrl)
            io.navInputs[NavInput.TweakSlow] = 1f
        if (io.keyShift)
            io.navInputs[NavInput.TweakFast] = 1f
        if (io.keyAlt && !io.keyCtrl) // AltGR is Alt+Ctrl, also even on keyboards without AltGR we don't want Alt+Ctrl to open menu.
            io.navInputs[NavInput.KeyMenu] = 1f
    }
    for (i in io.navInputsDownDuration.indices)
        io.navInputsDownDurationPrev[i] = io.navInputsDownDuration[i]
    for (i in io.navInputs.indices)
        io.navInputsDownDuration[i] = when (io.navInputs[i] > 0f) {
            true -> if (io.navInputsDownDuration[i] < 0f) 0f else io.navInputsDownDuration[i] + io.deltaTime
            else -> -1f
        }

    // Process navigation init request (select first/default focus)
    if (g.navInitResultId != 0 && (!g.navDisableHighlight || g.navInitRequestFromMove)) {
        /*  Apply result from previous navigation init request (will typically select the first item,
            unless setItemDefaultFocus() has been called)         */
//        assert(g.navWindow != null) !! later
        if (g.navInitRequestFromMove)
            setNavIDWithRectRel(g.navInitResultId, g.navLayer, g.navInitResultRectRel)
        else
            setNavId(g.navInitResultId, g.navLayer)
        g.navWindow!!.navRectRel[g.navLayer.i] = g.navInitResultRectRel
    }
    g.navInitRequest = false
    g.navInitRequestFromMove = false
    g.navInitResultId = 0
    g.navJustMovedToId = 0

    // Process navigation move request
    if (g.navMoveRequest)
        navUpdateMoveResult()

    // When a forwarded move request failed, we restore the highlight that we disabled during the forward frame
    if (g.navMoveRequestForward == NavForward.ForwardActive) {
        assert(g.navMoveRequest)
        if (g.navMoveResultLocal.id == 0 && g.navMoveResultOther.id == 0)
            g.navDisableHighlight = false
        g.navMoveRequestForward = NavForward.None
    }

    // Apply application mouse position movement, after we had a chance to process move request result.
    if (g.navMousePosDirty && g.navIdIsAlive) {
        // Set mouse position given our knowledge of the navigated item position from last frame
        if (io.configFlags has Cf.NavEnableSetMousePos && io.backendFlags has BackendFlag.HasSetMousePos)
            if (!g.navDisableHighlight && g.navDisableMouseHover && g.navWindow != null) {
                io.mousePos = navCalcPreferredRefPos()
                io.mousePosPrev = Vec2(io.mousePos)
                io.wantSetMousePos = true
            }
        g.navMousePosDirty = false
    }
    g.navIdIsAlive = false
    g.navJustTabbedId = 0
//    assert(g.navLayer == 0 || g.navLayer == 1) useless on jvm

    // Store our return window (for returning from Layer 1 to Layer 0) and clear it as soon as we step back in our own Layer 0
    g.navWindow?.let {
        navSaveLastChildNavWindowIntoParent(it)
        if (it.navLastChildNavWindow != null && g.navLayer == NavLayer.Main)
            it.navLastChildNavWindow = null
    }

    // Update CTRL+TAB and Windowing features (hold Square to move/resize/etc.)
    navUpdateWindowing()

    // Set output flags for user application
    io.navActive = (navKeyboardActive || navGamepadActive) && g.navWindow?.flags?.hasnt(Wf.NoNavInputs) ?: false
    io.navVisible = (io.navActive && g.navId != 0 && !g.navDisableHighlight) || g.navWindowingTarget != null

    // Process NavCancel input (to close a popup, get back to parent, clear focus)
    if (NavInput.Cancel.isPressed(InputReadMode.Pressed)) {
        if (g.activeId != 0) {
            if (g.activeIdBlockNavInputFlags hasnt (1 shl NavInput.Cancel))
                clearActiveId()
        } else if (g.navWindow != null && g.navWindow!!.flags has Wf.ChildWindow && g.navWindow!!.flags hasnt Wf.Popup && g.navWindow!!.parentWindow != null) {
            // Exit child window
            val childWindow = g.navWindow!!
            val parentWindow = childWindow.parentWindow!!
            assert(childWindow.childId != 0)
            parentWindow.focus()
            setNavId(childWindow.childId, NavLayer.Main)
            g.navIdIsAlive = false
            if (g.navDisableMouseHover)
                g.navMousePosDirty = true
        } else if (g.openPopupStack.isNotEmpty()) {
            // Close open popup/menu
            if (g.openPopupStack.last().window!!.flags hasnt Wf.Modal)
                closePopupToLevel(g.openPopupStack.lastIndex, true)
        } else if (g.navLayer != NavLayer.Main)
            navRestoreLayer(NavLayer.Main)  // Leave the "menu" layer
        else {
            // Clear NavLastId for popups but keep it for regular child window so we can leave one and come back where we were
            if (g.navWindow != null && (g.navWindow!!.flags has Wf.Popup || g.navWindow!!.flags hasnt Wf.ChildWindow))
                g.navWindow!!.navLastIds[0] = 0
            g.navId = 0
        }
    }

    // Process manual activation request
    g.navActivateId = 0
    g.navActivateDownId = 0
    g.navActivatePressedId = 0
    g.navInputId = 0
    if (g.navId != 0 && !g.navDisableHighlight && g.navWindowingTarget == null && g.navWindow != null && g.navWindow!!.flags hasnt Wf.NoNavInputs) {
        val activateDown = NavInput.Activate.isDown()
        val activatePressed = activateDown && NavInput.Activate.isPressed(InputReadMode.Pressed)
        if (g.activeId == 0 && activatePressed)
            g.navActivateId = g.navId
        if ((g.activeId == 0 || g.activeId == g.navId) && activateDown)
            g.navActivateDownId = g.navId
        if ((g.activeId == 0 || g.activeId == g.navId) && activatePressed)
            g.navActivatePressedId = g.navId
        if ((g.activeId == 0 || g.activeId == g.navId) && NavInput.Input.isPressed(InputReadMode.Pressed))
            g.navInputId = g.navId
    }
    g.navWindow?.let { if (it.flags has Wf.NoNavInputs) g.navDisableHighlight = true }
    if (g.navActivateId != 0)
        assert(g.navActivateDownId == g.navActivateId)
    g.navMoveRequest = false

    // Process programmatic activation request
    if (g.navNextActivateId != 0) {
        g.navInputId = g.navNextActivateId
        g.navActivatePressedId = g.navNextActivateId
        g.navActivateDownId = g.navNextActivateId
        g.navActivateId = g.navNextActivateId
    }
    g.navNextActivateId = 0

    // Initiate directional inputs request
    val allowedDirFlags = if (g.activeId == 0) 0.inv() else g.activeIdAllowNavDirFlags
    if (g.navMoveRequestForward == NavForward.None) {
        g.navMoveDir = Dir.None
        g.navMoveRequestFlags = NavMoveFlag.None.i
        g.navWindow?.let {
            if (g.navWindowingTarget == null && allowedDirFlags != 0 && it.flags hasnt Wf.NoNavInputs) {
                if (allowedDirFlags has (1 shl Dir.Left) && isNavInputPressedAnyOfTwo(NavInput.DpadLeft, NavInput.KeyLeft, InputReadMode.Repeat))
                    g.navMoveDir = Dir.Left
                if (allowedDirFlags has (1 shl Dir.Right) && isNavInputPressedAnyOfTwo(NavInput.DpadRight, NavInput.KeyRight, InputReadMode.Repeat))
                    g.navMoveDir = Dir.Right
                if (allowedDirFlags has (1 shl Dir.Up) && isNavInputPressedAnyOfTwo(NavInput.DpadUp, NavInput.KeyUp, InputReadMode.Repeat))
                    g.navMoveDir = Dir.Up
                if (allowedDirFlags has (1 shl Dir.Down) && isNavInputPressedAnyOfTwo(NavInput.DpadDown, NavInput.KeyDown, InputReadMode.Repeat))
                    g.navMoveDir = Dir.Down
            }
        }
        g.navMoveDir = g.navMoveDir
    } else {
        /*  Forwarding previous request (which has been modified, e.g. wrap around menus rewrite the requests with
            a starting rectangle at the other side of the window)
            (Preserve g.NavMoveRequestFlags, g.NavMoveClipDir which were set by the NavMoveRequestForward() function) */
        assert(g.navMoveDir != Dir.None && g.navMoveDir != Dir.None)
        assert(g.navMoveRequestForward == NavForward.ForwardQueued)
        g.navMoveRequestForward = NavForward.ForwardActive
    }

    // Update PageUp/PageDown scroll
    val navScoringRectOffsetY = when {
        navKeyboardActive -> navUpdatePageUpPageDown(allowedDirFlags)
        else -> 0f
    }

    /*  If we initiate a movement request and have no current navId, we initiate a InitDefautRequest that will be used
        as a fallback if the direction fails to find a match     */
    if (g.navMoveDir != Dir.None) {
        g.navMoveRequest = true
        g.navMoveDirLast = g.navMoveDir
    }
    if (g.navMoveRequest && g.navId == 0) {
        g.navInitRequest = true
        g.navInitRequestFromMove = true
        g.navInitResultId = 0
        g.navDisableHighlight = false
    }
    navUpdateAnyRequestFlag()

    // Scrolling
    g.navWindow?.let {

        if (it.flags hasnt Wf.NoNavInputs && g.navWindowingTarget == null) {
            // *Fallback* manual-scroll with Nav directional keys when window has no navigable item
            val scrollSpeed = floor(it.calcFontSize() * 100 * io.deltaTime + 0.5f) // We need round the scrolling speed because sub-pixel scroll isn't reliably supported.
            if (it.dc.navLayerActiveMask == 0 && it.dc.navHasScroll && g.navMoveRequest) {
                if (g.navMoveDir == Dir.Left || g.navMoveDir == Dir.Right)
                    it.setScrollX(floor(it.scroll.x + (if (g.navMoveDir == Dir.Left) -1f else 1f) * scrollSpeed))
                if (g.navMoveDir == Dir.Up || g.navMoveDir == Dir.Down)
                    it.setScrollY(floor(it.scroll.y + (if (g.navMoveDir == Dir.Up) -1f else 1f) * scrollSpeed))
            }

            // *Normal* Manual scroll with NavScrollXXX keys
            // Next movement request will clamp the NavId reference rectangle to the visible area, so navigation will resume within those bounds.
            val scrollDir = getNavInputAmount2d(NavDirSourceFlag.PadLStick.i, InputReadMode.Down, 1f / 10f, 10f)
            if (scrollDir.x != 0f && it.scrollbar.x) {
                it.setScrollX(floor(it.scroll.x + scrollDir.x * scrollSpeed))
                g.navMoveFromClampedRefRect = true
            }
            if (scrollDir.y != 0f) {
                it.setScrollY(floor(it.scroll.y + scrollDir.y * scrollSpeed))
                g.navMoveFromClampedRefRect = true
            }
        }
    }

    // Reset search results
    g.navMoveResultLocal.clear()
    g.navMoveResultLocalVisibleSet.clear()
    g.navMoveResultOther.clear()

    // When we have manually scrolled (without using navigation) and NavId becomes out of bounds, we project its bounding box to the visible area to restart navigation within visible items
    if (g.navMoveRequest && g.navMoveFromClampedRefRect && g.navLayer == NavLayer.Main) {
        val window = g.navWindow!!
        val windowRectRel = Rect(window.innerRect.min - window.pos - 1, window.innerRect.max - window.pos + 1)
        if (window.navRectRel[g.navLayer.i] !in windowRectRel) {
            val pad = window.calcFontSize() * 0.5f
            windowRectRel expand Vec2(-min(windowRectRel.width, pad), -min(windowRectRel.height, pad)) // Terrible approximation for the intent of starting navigation from first fully visible item
            window.navRectRel[g.navLayer.i] clipWith windowRectRel
            g.navId = 0
        }
        g.navMoveFromClampedRefRect = false
    }

    // For scoring we use a single segment on the left side our current item bounding box (not touching the edge to avoid box overlap with zero-spaced items)
    g.navWindow.let {
        if (it != null) {
            val navRectRel = if (!it.navRectRel[g.navLayer.i].isInverted) Rect(it.navRectRel[g.navLayer.i]) else Rect(0f, 0f, 0f, 0f)
            g.navScoringRectScreen.put(navRectRel.min + it.pos, navRectRel.max + it.pos)
        } else g.navScoringRectScreen put viewportRect
    }
    g.navScoringRectScreen translateY navScoringRectOffsetY
    g.navScoringRectScreen.min.x = min(g.navScoringRectScreen.min.x + 1f, g.navScoringRectScreen.max.x)
    g.navScoringRectScreen.max.x = g.navScoringRectScreen.min.x
    // Ensure if we have a finite, non-inverted bounding box here will allows us to remove extraneous abs() calls in navScoreItem().
    assert(!g.navScoringRectScreen.isInverted)
    //g.OverlayDrawList.AddRect(g.NavScoringRectScreen.Min, g.NavScoringRectScreen.Max, IM_COL32(255,200,0,255)); // [DEBUG]
    g.navScoringCount = 0
    if (IMGUI_DEBUG_NAV_RECTS)
        g.navWindow?.let { nav ->
            for (layer in 0..1)
                getForegroundDrawList(nav).addRect(nav.pos + nav.navRectRel[layer].min, nav.pos + nav.navRectRel[layer].max, COL32(255, 200, 0, 255))  // [DEBUG]
            val col = if (!nav.hidden) COL32(255, 0, 255, 255) else COL32(255, 0, 0, 255)
            val p = navCalcPreferredRefPos()
            val buf = "${g.navLayer}".toCharArray(CharArray(32))
            getForegroundDrawList(nav).addCircleFilled(p, 3f, col)
            getForegroundDrawList(nav).addText(null, 13f, p + Vec2(8, -4), col, buf)
        }
}

/** Windowing management mode
 *  Keyboard: CTRL+Tab (change focus/move/resize), Alt (toggle menu layer)
 *  Gamepad:  Hold Menu/Square (change focus/move/resize), Tap Menu/Square (toggle menu layer) */
fun navUpdateWindowing() {

    var applyFocusWindow: Window? = null
    var applyToggleLayer = false

    val modalWindow = frontMostPopupModal
    if (modalWindow != null) {
        g.navWindowingTarget = null
        return
    }

    // Fade out
    if (g.navWindowingTargetAnim != null && g.navWindowingTarget == null) {
        g.navWindowingHighlightAlpha = (g.navWindowingHighlightAlpha - io.deltaTime * 10f) max 0f
        if (g.dimBgRatio <= 0f && g.navWindowingHighlightAlpha <= 0f)
            g.navWindowingTargetAnim = null
    }
    // Start CTRL-TAB or Square+L/R window selection
    val startWindowingWithGamepad = g.navWindowingTarget == null && NavInput.Menu.isPressed(InputReadMode.Pressed)
    val startWindowingWithKeyboard = g.navWindowingTarget == null && io.keyCtrl && Key.Tab.isPressed && io.configFlags has Cf.NavEnableKeyboard
    if (startWindowingWithGamepad || startWindowingWithKeyboard)
        (g.navWindow ?: findWindowNavFocusable(g.windowsFocusOrder.lastIndex, -Int.MAX_VALUE, -1))?.let {
            g.navWindowingTarget = it
            g.navWindowingTargetAnim = it
            g.navWindowingHighlightAlpha = 0f
            g.navWindowingTimer = 0f
            g.navWindowingToggleLayer = !startWindowingWithKeyboard
            g.navInputSource = if (startWindowingWithKeyboard) InputSource.NavKeyboard else InputSource.NavGamepad
        }

    // Gamepad update
    g.navWindowingTimer += io.deltaTime
    g.navWindowingTarget?.let {
        if (g.navInputSource == InputSource.NavGamepad) {
            /*  Highlight only appears after a brief time holding the button, so that a fast tap on PadMenu
                (to toggle NavLayer) doesn't add visual noise             */
            g.navWindowingHighlightAlpha = max(g.navWindowingHighlightAlpha, saturate((g.navWindowingTimer - NAV_WINDOWING_HIGHLIGHT_DELAY) / 0.05f))

            // Select window to focus
            val focusChangeDir = NavInput.FocusPrev.isPressed(InputReadMode.RepeatSlow).i - NavInput.FocusNext.isPressed(InputReadMode.RepeatSlow).i
            if (focusChangeDir != 0) {
                navUpdateWindowingHighlightWindow(focusChangeDir)
                g.navWindowingHighlightAlpha = 1f
            }

            // Single press toggles NavLayer, long press with L/R apply actual focus on release (until then the window was merely rendered front-most)
            if (!NavInput.Menu.isDown()) {
                // Once button was held long enough we don't consider it a tap-to-toggle-layer press anymore.
                g.navWindowingToggleLayer = g.navWindowingToggleLayer and (g.navWindowingHighlightAlpha < 1f)
                if (g.navWindowingToggleLayer && g.navWindow != null)
                    applyToggleLayer = true
                else if (!g.navWindowingToggleLayer)
                    applyFocusWindow = it
                g.navWindowingTarget = null
            }
        }
    }
    // Keyboard: Focus
    g.navWindowingTarget?.let {
        if (g.navInputSource == InputSource.NavKeyboard) {
            // Visuals only appears after a brief time after pressing TAB the first time, so that a fast CTRL+TAB doesn't add visual noise
            g.navWindowingHighlightAlpha = max(g.navWindowingHighlightAlpha, saturate((g.navWindowingTimer - NAV_WINDOWING_HIGHLIGHT_DELAY) / 0.05f)) // 1.0f
            if (Key.Tab.isPressed(true))
                navUpdateWindowingHighlightWindow(if (io.keyShift) 1 else -1)
            if (!io.keyCtrl)
                applyFocusWindow = g.navWindowingTarget
        }
    }

    // Keyboard: Press and Release ALT to toggle menu layer
    // FIXME: We lack an explicit IO variable for "is the imgui window focused", so compare mouse validity to detect the common case of back-end clearing releases all keys on ALT-TAB
    if (NavInput.KeyMenu.isPressed(InputReadMode.Pressed))
        g.navWindowingToggleLayer = true
    if ((g.activeId == 0 || g.activeIdAllowOverlap) && g.navWindowingToggleLayer && NavInput.KeyMenu.isPressed(InputReadMode.Released))
        if (isMousePosValid(io.mousePos) == isMousePosValid(io.mousePosPrev))
            applyToggleLayer = true

    // Move window
    g.navWindowingTarget?.let {
        if (it.flags hasnt Wf.NoMove) {
            var moveDelta = Vec2()
            if (g.navInputSource == InputSource.NavKeyboard && !io.keyShift)
                moveDelta = getNavInputAmount2d(NavDirSourceFlag.Keyboard.i, InputReadMode.Down)
            if (g.navInputSource == InputSource.NavGamepad)
                moveDelta = getNavInputAmount2d(NavDirSourceFlag.PadLStick.i, InputReadMode.Down)
            if (moveDelta.x != 0f || moveDelta.y != 0f) {
                val NAV_MOVE_SPEED = 800f
                // FIXME: Doesn't code variable framerate very well
                val moveSpeed = floor(NAV_MOVE_SPEED * io.deltaTime * min(io.displayFramebufferScale.x, io.displayFramebufferScale.y))
                it.rootWindow!!.pos plusAssign moveDelta * moveSpeed
                g.navDisableMouseHover = true
                it.markIniSettingsDirty()
            }
        }
    }

    // Apply final focus
    if (applyFocusWindow != null && (g.navWindow == null || applyFocusWindow !== g.navWindow!!.rootWindow)) {
        clearActiveId()
        g.navDisableHighlight = false
        g.navDisableMouseHover = true
        applyFocusWindow = navRestoreLastChildNavWindow(applyFocusWindow!!)
        closePopupsOverWindow(applyFocusWindow, false)
        applyFocusWindow.focus()
        if (applyFocusWindow!!.navLastIds[0] == 0)
            navInitWindow(applyFocusWindow!!, false)

        // If the window only has a menu layer, select it directly
        if (applyFocusWindow!!.dc.navLayerActiveMask == 1 shl NavLayer.Menu)
            g.navLayer = NavLayer.Menu
    }
    applyFocusWindow?.let { g.navWindowingTarget = null }

    // Apply menu/layer toggle
    if (applyToggleLayer)
        g.navWindow?.let {
            // Move to parent menu if necessary
            var newNavWindow = it

            tailrec fun Window.getParent(): Window {
                val parent = parentWindow
                return if (parent != null && dc.navLayerActiveMask hasnt (1 shl NavLayer.Menu) && flags has Wf.ChildWindow && flags hasnt (Wf.Popup or Wf.ChildMenu)) getParent() else this
            }

            newNavWindow = newNavWindow.getParent()

            if (newNavWindow !== it) {
                val oldNavWindow = it
                newNavWindow.focus()
                newNavWindow.navLastChildNavWindow = oldNavWindow
            }
            g.navDisableHighlight = false
            g.navDisableMouseHover = true
            // When entering a regular menu bar with the Alt key, we always reinitialize the navigation ID.
            val newNavLayer = when {
                it.dc.navLayerActiveMask has (1 shl NavLayer.Menu) -> NavLayer of (g.navLayer.i xor 1)
                else -> NavLayer.Main
            }
            navRestoreLayer(newNavLayer)
        }
}

/** Overlay displayed when using CTRL+TAB. Called by EndFrame(). */
fun navUpdateWindowingList() {

    val target = g.navWindowingTarget!! // ~ assert

    if (g.navWindowingTimer < NAV_WINDOWING_LIST_APPEAR_DELAY) return

    if (g.navWindowingList.isEmpty())
        findWindowByName("###NavWindowingList")?.let { g.navWindowingList += it }
    setNextWindowSizeConstraints(Vec2(io.displaySize.x * 0.2f, io.displaySize.y * 0.2f), Vec2(Float.MAX_VALUE))
    setNextWindowPos(Vec2(io.displaySize.x * 0.5f, io.displaySize.y * 0.5f), Cond.Always, Vec2(0.5f))
    pushStyleVar(StyleVar.WindowPadding, style.windowPadding * 2f)
    val flags = Wf.NoTitleBar or Wf.NoFocusOnAppearing or Wf.NoResize or Wf.NoMove or Wf.NoMouseInputs or Wf.AlwaysAutoResize or Wf.NoSavedSettings
    begin("###NavWindowingList", null, flags)
    for (n in g.windowsFocusOrder.lastIndex downTo 0) {
        val window = g.windowsFocusOrder[n]
        if (!window.isNavFocusable)
            continue
        var label = window.name
        val labelEnd = findRenderedTextEnd(label)
        if (labelEnd != 0)
            label = window.fallbackWindowName
        selectable(label, target == window)
    }
    end()
    popStyleVar()
}

/** Apply result from previous frame navigation directional move request */
fun navUpdateMoveResult() {

    if (g.navMoveResultLocal.id == 0 && g.navMoveResultOther.id == 0) {
        // In a situation when there is no results but NavId != 0, re-enable the Navigation highlight (because g.NavId is not considered as a possible result)
        if (g.navId != 0) {
            g.navDisableHighlight = false
            g.navDisableMouseHover = true
        }
        return
    }
    // Select which result to use
    var result = if (g.navMoveResultLocal.id != 0) g.navMoveResultLocal else g.navMoveResultOther

    // PageUp/PageDown behavior first jumps to the bottom/top mostly visible item, _otherwise_ use the result from the previous/next page.
    if (g.navMoveRequestFlags has NavMoveFlag.AlsoScoreVisibleSet)
        if (g.navMoveResultLocalVisibleSet.id != 0 && g.navMoveResultLocalVisibleSet.id != g.navId)
            result = g.navMoveResultLocalVisibleSet

    // Maybe entering a flattened child from the outside? In this case solve the tie using the regular scoring rules.
    if (result != g.navMoveResultOther && g.navMoveResultOther.id != 0 && g.navMoveResultOther.window!!.parentWindow === g.navWindow)
        if (g.navMoveResultOther.distBox < result.distBox || (g.navMoveResultOther.distBox == result.distBox && g.navMoveResultOther.distCenter < result.distCenter))
            result = g.navMoveResultOther
    val window = result.window!!
    assert(g.navWindow != null)
    // Scroll to keep newly navigated item fully into view.
    if (g.navLayer == NavLayer.Main) {
        val rectAbs = Rect(result.rectRel.min + window.pos, result.rectRel.max + window.pos)
        navScrollToBringItemIntoView(window, rectAbs)
        // Estimate upcoming scroll so we can offset our result position so mouse position can be applied immediately after in NavUpdate()
        val nextScroll = calcNextScrollFromScrollTargetAndClamp(window, false)
        val deltaScroll = window.scroll - nextScroll
        result.rectRel.translate(deltaScroll)
        // Also scroll parent window to keep us into view if necessary (we could/should technically recurse back the whole the parent hierarchy).
        if (window.flags has Wf.ChildWindow)
            navScrollToBringItemIntoView(window.parentWindow!!, Rect(rectAbs.min + deltaScroll, rectAbs.max + deltaScroll))
    }

    clearActiveId()
    g.navWindow = window
    setNavIDWithRectRel(result.id, g.navLayer, result.rectRel)
    g.navJustMovedToId = result.id
    g.navMoveFromClampedRefRect = false
}

fun navUpdatePageUpPageDown(allowedDirFlags: Int): Float {

    if (g.navMoveDir == Dir.None)

        g.navWindow?.let { window ->

            if (window.flags hasnt Wf.NoNavInputs && g.navWindowingTarget == null && g.navLayer == NavLayer.Main) {

                val pageUpHeld = Key.PageUp.isDown && allowedDirFlags has (1 shl Dir.Up.i)
                val pageDownHeld = Key.PageDown.isDown && allowedDirFlags has (1 shl Dir.Down)
                if (pageUpHeld != pageDownHeld) // If either (not both) are pressed
                    if (window.dc.navLayerActiveMask == 0x00 && window.dc.navHasScroll) {
                        // Fallback manual-scroll when window has no navigable item
                        if (Key.PageUp.isPressed)
                            window.setScrollY(window.scroll.y - window.innerRect.height)
                        else if (Key.PageDown.isPressed)
                            window.setScrollY(window.scroll.y + window.innerRect.height)
                    } else {
                        val navRectRel = window.navRectRel[g.navLayer.i]
                        val pageOffsetY = 0f max (window.innerRect.height - window.calcFontSize() + navRectRel.height)
                        return when { // nav_scoring_rect_offset_y
                            Key.PageUp.isPressed -> {
                                g.navMoveDir = Dir.Down // Because our scoring rect is offset, we intentionally request the opposite direction (so we can always land on the last item)
                                g.navMoveClipDir = Dir.Up
                                g.navMoveRequestFlags = NavMoveFlag.AllowCurrentNavId or NavMoveFlag.AlsoScoreVisibleSet
                                -pageOffsetY
                            }
                            Key.PageDown.isPressed -> {
                                g.navMoveDir = Dir.Up // Because our scoring rect is offset, we intentionally request the opposite direction (so we can always land on the last item)
                                g.navMoveClipDir = Dir.Down
                                g.navMoveRequestFlags = NavMoveFlag.AllowCurrentNavId or NavMoveFlag.AlsoScoreVisibleSet
                                +pageOffsetY
                            }
                            else -> 0f
                        }
                    }
            }
        }
    return 0f
}

fun navUpdateAnyRequestFlag() {
    g.navAnyRequest = g.navMoveRequest || g.navInitRequest || (IMGUI_DEBUG_NAV_SCORING && g.navWindow != null)
    if (g.navAnyRequest)
        assert(g.navWindow != null)
}

/** We get there when either navId == id, or when g.navAnyRequest is set (which is updated by navUpdateAnyRequestFlag above)    */
fun navProcessItem(window: Window, navBb: Rect, id: ID) {

    //if (!g.io.NavActive)  // [2017/10/06] Removed this possibly redundant test but I am not sure of all the side-effects yet. Some of the feature here will need to work regardless of using a _NoNavInputs flag.
    //    return;

    val itemFlags = window.dc.itemFlags
    val navBbRel = Rect(navBb.min - window.pos, navBb.max - window.pos)

    // Process Init Request
    if (g.navInitRequest && g.navLayer == window.dc.navLayerCurrent) {
        // Even if 'ImGuiItemFlags_NoNavDefaultFocus' is on (typically collapse/close button) we record the first ResultId so they can be used as a fallback
        if (itemFlags hasnt ItemFlag.NoNavDefaultFocus || g.navInitResultId == 0) {
            g.navInitResultId = id
            g.navInitResultRectRel = navBbRel
        }
        if (itemFlags hasnt ItemFlag.NoNavDefaultFocus) {
            g.navInitRequest = false // Found a match, clear request
            navUpdateAnyRequestFlag()
        }
    }

    /*  Process Move Request (scoring for navigation)
        FIXME-NAV: Consider policy for double scoring
        (scoring from NavScoringRectScreen + scoring from a rect wrapped according to current wrapping policy)     */
    if ((g.navId != id || g.navMoveRequestFlags has NavMoveFlag.AllowCurrentNavId) && itemFlags hasnt (ItemFlag.Disabled or ItemFlag.NoNav)) {
        var result by if (window === g.navWindow) g::navMoveResultLocal else g::navMoveResultOther
        val newBest = when {
            IMGUI_DEBUG_NAV_SCORING -> {  // [DEBUG] Score all items in NavWindow at all times
                if (!g.navMoveRequest) g.navMoveDir = g.navMoveDirLast
                navScoreItem(result, navBb) && g.navMoveRequest
            }
            else -> g.navMoveRequest && navScoreItem(result, navBb)
        }
        if (newBest) {
            result.id = id
            result.window = window
            result.rectRel put navBbRel
        }

        val VISIBLE_RATIO = 0.7f
        if (g.navMoveRequestFlags has NavMoveFlag.AlsoScoreVisibleSet && window.clipRect overlaps navBb)
            if (glm.clamp(navBb.max.y, window.clipRect.min.y, window.clipRect.max.y) -
                    glm.clamp(navBb.min.y, window.clipRect.min.y, window.clipRect.max.y) >= (navBb.max.y - navBb.min.y) * VISIBLE_RATIO)
                if (navScoreItem(g.navMoveResultLocalVisibleSet, navBb)) {
                    result = g.navMoveResultLocalVisibleSet.also {
                        it.id = id
                        it.window = window
                        it.rectRel = navBbRel
                    }
                }
    }

    // Update window-relative bounding box of navigated item
    if (g.navId == id) {
        g.navWindow = window    // Always refresh g.NavWindow, because some operations such as FocusItem() don't have a window.
        g.navLayer = window.dc.navLayerCurrent
        g.navIdIsAlive = true
        g.navIdTabCounter = window.dc.focusCounterTab
        window.navRectRel[window.dc.navLayerCurrent.i] = navBbRel    // Store item bounding box (relative to window position)
    }
}

fun navCalcPreferredRefPos(): Vec2 {
    if (g.navDisableHighlight || !g.navDisableMouseHover || g.navWindow == null) {
        // Mouse (we need a fallback in case the mouse becomes invalid after being used)
        if (isMousePosValid(io.mousePos))
            return Vec2(io.mousePos)
        return Vec2(g.lastValidMousePos)
    } else {
        // When navigation is active and mouse is disabled, decide on an arbitrary position around the bottom left of the currently navigated item.
        val rectRel = g.navWindow!!.navRectRel[g.navLayer.i]
        val pos = g.navWindow!!.pos + Vec2(rectRel.min.x + min(style.framePadding.x * 4, rectRel.width), rectRel.max.y - min(style.framePadding.y, rectRel.height))
        val visibleRect = viewportRect
        return glm.floor(glm.clamp(pos, visibleRect.min, visibleRect.max))   // ImFloor() is important because non-integer mouse position application in back-end might be lossy and result in undesirable non-zero delta.
    }
}

/** FIXME: This could be replaced by updating a frame number in each window when (window == NavWindow) and (NavLayer == 0).
 *  This way we could find the last focused window among our children. It would be much less confusing this way? */
fun navSaveLastChildNavWindowIntoParent(navWindow: Window?) {

    tailrec fun Window.getParent(): Window {
        val parent = parentWindow
        return when {
            parent != null && flags has Wf.ChildWindow && flags hasnt (Wf.Popup or Wf.ChildMenu) -> parent.getParent()
            else -> this
        }
    }

    navWindow?.getParent()?.let { if (it !== navWindow) it.navLastChildNavWindow = navWindow }
}


/** Restore the last focused child.
 *  Call when we are expected to land on the Main Layer (0) after FocusWindow()    */
fun navRestoreLastChildNavWindow(window: Window) = window.navLastChildNavWindow ?: window

// FIXME-OPT O(N)
fun findWindowFocusIndex(window: Window): Int {
    var i = g.windowsFocusOrder.lastIndex
    while (i >= 0) {
        if (g.windowsFocusOrder[i] == window)
            return i
        i--
    }
    return -1
}

//-----------------------------------------------------------------------------
// Platform dependent default implementations
//-----------------------------------------------------------------------------

val getClipboardTextFn_DefaultImpl: () -> String? = {
    // Create a Clipboard object using getSystemClipboard() method
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    // Get data stored in the clipboard that is in the form of a string (text)
    clipboard.getData(DataFlavor.stringFlavor) as? String
}

val setClipboardTextFn_DefaultImpl: (String) -> Unit = {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(StringSelection(it), null)
}

var imeSetInputScreenPosFn_Win32 = { x: Int, y: Int ->
    // Notify OS Input Method Editor of text input position
    val hwnd: HWND = io.imeWindowHandle
    if (hwnd.L != NULL) {
        val himc: HIMC = HIMC(imm.getContext(hwnd))
        if (himc.L != NULL) {
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

fun isNavInputPressedAnyOfTwo(n1: NavInput, n2: NavInput, mode: InputReadMode) = getNavInputAmount(n1, mode) + getNavInputAmount(n2, mode) > 0f

// FIXME-OPT O(N)
fun findWindowNavFocusable(iStart: Int, iStop: Int, dir: Int): Window? {
    var i = iStart
    while (i in g.windowsFocusOrder.indices && i != iStop) {
        if (g.windowsFocusOrder[i].isNavFocusable)
            return g.windowsFocusOrder[i]
        i += dir
    }
    return null
}

fun navUpdateWindowingHighlightWindow(focusChangeDir: Int) {

    val target = g.navWindowingTarget!!
    if (target.flags has Wf.Modal) return

    val iCurrent = findWindowFocusIndex(target)
    val windowTarget = findWindowNavFocusable(iCurrent + focusChangeDir, -Int.MAX_VALUE, focusChangeDir)
            ?: findWindowNavFocusable(if (focusChangeDir < 0) g.windowsFocusOrder.lastIndex else 0, iCurrent, focusChangeDir)
    // Don't reset windowing target if there's a single window in the list
    windowTarget?.let {
        g.navWindowingTarget = it
        g.navWindowingTargetAnim = it
    }
    g.navWindowingToggleLayer = false
}