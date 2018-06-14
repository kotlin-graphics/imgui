package imgui

import gli_.has
import gli_.hasnt
import glm_.*
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import imgui.ImGui.clearActiveId
import imgui.ImGui.closePopupsOverWindow
import imgui.ImGui.getColumnOffset
import imgui.ImGui.getNavInputAmount
import imgui.ImGui.getNavInputAmount2d
import imgui.ImGui.io
import imgui.ImGui.isKeyDown
import imgui.ImGui.isMousePosValid
import imgui.ImGui.navInitWindow
import imgui.ImGui.overlayDrawList
import imgui.ImGui.style
import imgui.imgui.*
import imgui.imgui.imgui_colums.Companion.columnsRectHalfWidth
import imgui.internal.*
import uno.kotlin.isPrintable
import java.io.File
import java.nio.file.Paths
import java.util.*
import kotlin.collections.set
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KMutableProperty0
import imgui.ConfigFlag as Cf
import imgui.InputTextFlag as Itf
import imgui.WindowFlag as Wf


fun logRenderedText(refPos: Vec2?, text: String, textEnd: Int = 0): Nothing = TODO()

fun getDraggedColumnOffset(columns: ColumnsSet, columnIndex: Int): Float {
    /*  Active (dragged) column always follow mouse. The reason we need this is that dragging a column to the right edge
        of an auto-resizing window creates a feedback loop because we store normalized positions. So while dragging we
        enforce absolute positioning.   */

    val window = g.currentWindow!!
    assert(columnIndex > 0) { "We are not supposed to drag column 0." }
    assert(g.activeId == columns.id + columnIndex/* as ID */)

    var x = io.mousePos.x - g.activeIdClickOffset.x + columnsRectHalfWidth - window.pos.x
    x = glm.max(x, getColumnOffset(columnIndex - 1) + style.columnsMinSpacing)
    if (columns.flags has ColumnsFlag.NoPreserveWidths)
        x = glm.min(x, getColumnOffset(columnIndex + 1) - style.columnsMinSpacing)

    return x
}

val defaultFont get() = io.fontDefault ?: io.fonts.fonts[0]

//-----------------------------------------------------------------------------
// Internal API exposed in imgui_internal.h
//-----------------------------------------------------------------------------

/** Find window given position, search front-to-back
FIXME: Note that we have a lag here because WindowRectClipped is updated in Begin() so windows moved by user via
SetWindowPos() and not SetNextWindowPos() will have that rectangle lagging by a frame at the time
FindHoveredWindow() is called, aka before the next Begin(). Moving window thankfully isn't affected.    */
fun findHoveredWindow(): Window? {
    for (i in g.windows.size - 1 downTo 0) {
        val window = g.windows[i]
        if (!window.active)
            continue
        if (window.flags has Wf.NoInputs)
            continue

        // Using the clipped AABB, a child window will typically be clipped by its parent (not always)
        val bb = Rect(window.windowRectClipped.min - style.touchExtraPadding, window.windowRectClipped.max + style.touchExtraPadding)
        if (bb contains io.mousePos)
            return window
    }
    return null
}

fun createNewWindow(name: String, size: Vec2, flags: Int) = Window(g, name).apply {
    // Create window the first time

    this.flags = flags
    g.windowsById[id] = this

    // Default/arbitrary window position. Use SetNextWindowPos() with the appropriate condition flag to change the initial position of a window.
    pos put 60f

    // User can disable loading and saving of settings. Tooltip and child windows also don't store settings.
    if (flags hasnt Wf.NoSavedSettings) {
        //  Retrieve settings from .ini file

        findWindowSettings(id)?.let { s ->
            setConditionAllowFlags(Cond.FirstUseEver.i, false)
            pos = glm.floor(s.pos)
            collapsed = s.collapsed
            if (s.size.lengthSqr > 0.00001f)
                size put glm.floor(s.size)
        }
    }
    sizeFullAtLastBegin put size
    sizeFull put size
    this.size put size

    if (flags has Wf.AlwaysAutoResize) {
        autoFitFrames put 2
        autoFitOnlyGrows = false
    } else {
        if (size.x <= 0f) autoFitFrames.x = 2
        if (size.y <= 0f) autoFitFrames.y = 2
        autoFitOnlyGrows = autoFitFrames.x > 0 || autoFitFrames.y > 0
    }

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
        else assert(backup[ptr] == current, { "PushID/PopID or TreeNode/TreePop Mismatch!" })
        ptr++
    }
    run {
        // Too few or too many EndGroup()
        val current = window.dc.groupStack.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] == current, { "BeginGroup/EndGroup Mismatch!" })
        ptr++
    }
    run {
        // Too few or too many EndMenu()/EndPopup()
        val current = g.currentPopupStack.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] == current, { "BeginMenu/EndMenu or BeginPopup/EndPopup Mismatch" })
        ptr++
    }
    // For color, style and font stacks there is an incentive to use Push/Begin/Pop/.../End patterns, so we relax our checks a little to allow them.
    run {
        // Too few or too many PopStyleColor()
        val current = g.colorModifiers.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] >= current, { "PushStyleColor/PopStyleColor Mismatch!" })
        ptr++
    }
    run {
        // Too few or too many PopStyleVar()
        val current = g.styleModifiers.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] >= current, { "PushStyleVar/PopStyleVar Mismatch!" })
        ptr++
    }
    run {
        // Too few or too many PopFont()
        val current = g.fontStack.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] >= current, { "PushFont/PopFont Mismatch!" })
        ptr++
    }
    assert(ptr == window.dc.stackSizesBackup.size)
}

fun calcNextScrollFromScrollTargetAndClamp(window: Window): Vec2 {  // TODO -> window class?
    val scroll = Vec2(window.scroll)
    val crX = window.scrollTargetCenterRatio.x
    val crY = window.scrollTargetCenterRatio.y
    if (window.scrollTarget.x < Float.MAX_VALUE)
        scroll.x = window.scrollTarget.x - crX * (window.sizeFull.x - window.scrollbarSizes.x)
    if (window.scrollTarget.y < Float.MAX_VALUE)
        scroll.y = window.scrollTarget.y - (1f - crY) * (window.titleBarHeight + window.menuBarHeight) -
                crY * (window.sizeFull.y - window.scrollbarSizes.y)
    scroll maxAssign 0f
    if (!window.collapsed && !window.skipItems) {
        scroll.x = glm.min(scroll.x, window.scrollMaxX)
        scroll.y = glm.min(scroll.y, window.scrollMaxY)
    }
    return scroll
}

fun findWindowSettings(id: ID) = g.settingsWindows.firstOrNull { it.id == id }

fun addWindowSettings(name: String) = WindowSettings(name).apply { g.settingsWindows.add(this) }

/*  Settings/.Ini Utilities
    The disk functions are automatically called if io.IniFilename != NULL (default is "imgui.ini").
    Set io.IniFilename to NULL to load/save manually. Read io.WantSaveIniSettings description about handling .ini saving manually. */

/** call after CreateContext() and before the first call to NewFrame(). NewFrame() automatically calls LoadIniSettingsFromDisk(io.IniFilename). */
fun loadIniSettingsFromDisk(iniFilename: String?) {
    if (iniFilename == null) return
    var settings: WindowSettings? = null
    fileLoadToLines(iniFilename)?.filter { it.isNotEmpty() }?.forEach { s ->
        if (s[0] == '[' && s.last() == ']') {
            /*  Parse "[Type][Name]". Note that 'Name' can itself contains [] characters, which is acceptable with
                the current format and parsing code.                 */
            val firstCloseBracket = s.indexOf(']')
            val name: String
            val type: String
            if (firstCloseBracket != s.length - 1) { // Import legacy entries that have no type
                type = s.substring(1, firstCloseBracket)
                name = s.substring(firstCloseBracket + 2, s.length - 1)
            } else {
                type = "Window"
                name = s.substring(1, firstCloseBracket)
            }
            val typeHash = hash(type, 0, 0)
            settings = findWindowSettings(typeHash) ?: addWindowSettings(name)
        } else settings?.apply {
            when {
                s.startsWith("Pos") -> pos.put(s.substring(4).split(","))
                s.startsWith("Size") -> size put glm.max(Vec2i(s.substring(5).split(",")), style.windowMinSize)
                s.startsWith("Collapsed") -> collapsed = s.substring(10).toBoolean()
            }
        }
    }
    g.settingsLoaded = true
}

fun saveIniSettingsToDisk(iniFilename: String?) {

    g.settingsDirtyTimer = 0f
    if (iniFilename == null) return

    // Gather data from windows that were active during this session
    for (window in g.windows) {

        if (window.flags has Wf.NoSavedSettings) continue
        /** This will only return NULL in the rare instance where the window was first created with
         *  WindowFlag.NoSavedSettings then had the flag disabled later on.
         *  We don't bind settings in this case (bug #1000).    */
        val settings = findWindowSettings(window.id) ?: addWindowSettings(window.name)
        settings.pos put window.pos
        settings.size put window.sizeFull
        settings.collapsed = window.collapsed
    }

    /*  Write .ini file
        If a window wasn't opened in this session we preserve its settings     */
    File(Paths.get(iniFilename).toUri()).printWriter().use {
        for (setting in g.settingsWindows) {
            if (setting.pos.x == Float.MAX_VALUE) continue
            // Skip to the "###" marker if any. We don't skip past to match the behavior of GetID()
            val name = setting.name.substringBefore("###")
            it.println("[Window][$name]")   // TODO [%s][%s]\n", handler->TypeName, name
            it.println("Pos=${setting.pos.x},${setting.pos.y}")
            it.println("Size=${setting.size.x.i},${setting.size.y.i}")
            it.println("Collapsed=${setting.collapsed.i}")
            it.println()
        }
    }
}

fun getViewportRect(): Rect {
    if (io.displayVisibleMin != io.displayVisibleMax)
        return Rect(io.displayVisibleMin, io.displayVisibleMax)
    return Rect(0f, 0f, io.displaySize.x.f, io.displaySize.y.f)
}

fun closePopupToLevel(remaining: Int) {
    assert(remaining >= 0)
    var focusWindow = if (remaining > 0) g.openPopupStack[remaining - 1].window!!
    else g.openPopupStack[0].parentWindow
    if (g.navLayer == 0)
        focusWindow = navRestoreLastChildNavWindow(focusWindow)
    focusWindow.focus()
    focusWindow.dc.navHideHighlightOneFrame = true
    for (i in remaining until g.openPopupStack.size) g.openPopupStack.pop()  // resize(remaining)
}

enum class PopupPositionPolicy { Default, ComboBox }

fun findAllowedExtentRectForWindow(window: Window): Rect {
    val padding = Vec2(style.displaySafeAreaPadding)
    return getViewportRect().apply {
        expand(Vec2(if (width > padding.x * 2) -padding.x else 0f, if (height > padding.y * 2) -padding.y else 0f))
    }
}

/** rAvoid = the rectangle to avoid (e.g. for tooltip it is a rectangle around the mouse cursor which we want to avoid. for popups it's a small point around the cursor.)
 *  rOuter = the visible area rectangle, minus safe area padding. If our popup size won't fit because of safe area padding we ignore it.
 */
fun findBestWindowPosForPopupEx(refPos: Vec2, size: Vec2, lastDir: KMutableProperty0<Dir>, rOuter: Rect, rAvoid: Rect,
                                policy: PopupPositionPolicy = PopupPositionPolicy.Default): Vec2 {

    val basePosClamped = glm.clamp(refPos, rOuter.min, rOuter.max - size)
    //GImGui->OverlayDrawList.AddRect(r_avoid.Min, r_avoid.Max, IM_COL32(255,0,0,255));
    //GImGui->OverlayDrawList.AddRect(rOuter.Min, rOuter.Max, IM_COL32(0,255,0,255));

    // Combo Box policy (we want a connecting edge)
    if (policy == PopupPositionPolicy.ComboBox) {
        val dirPreferedOrder = arrayOf(Dir.Down, Dir.Right, Dir.Left, Dir.Up)
        for (n in (if (lastDir() != Dir.None) -1 else 0) until Dir.Count.i) {
            val dir = if (n == -1) lastDir() else dirPreferedOrder[n]
            if (n != -1 && dir == lastDir.get()) continue // Already tried this direction?
            val pos = Vec2()
            if (dir == Dir.Down) pos.put(rAvoid.min.x, rAvoid.max.y)          // Below, Toward Right (default)
            if (dir == Dir.Right) pos.put(rAvoid.min.x, rAvoid.min.y - size.y) // Above, Toward Right
            if (dir == Dir.Left) pos.put(rAvoid.max.x - size.x, rAvoid.max.y) // Below, Toward Left
            if (dir == Dir.Up) pos.put(rAvoid.max.x - size.x, rAvoid.min.y - size.y) // Above, Toward Left
            if (!rOuter.contains(Rect(pos, pos + size))) continue
            lastDir.set(dir)
            return pos
        }
    }

    // Default popup policy
    val dirPreferedOrder = arrayOf(Dir.Right, Dir.Down, Dir.Up, Dir.Left)
    for (n in (if (lastDir() != Dir.None) -1 else 0) until Dir.values().size) {
        val dir = if (n == -1) lastDir() else dirPreferedOrder[n]
        if (n != -1 && dir == lastDir()) continue  // Already tried this direction?
        val availW = (if (dir == Dir.Left) rAvoid.min.x else rOuter.max.x) - if (dir == Dir.Right) rAvoid.max.x else rOuter.min.x
        val availH = (if (dir == Dir.Up) rAvoid.min.y else rOuter.max.y) - if (dir == Dir.Down) rAvoid.max.y else rOuter.min.y
        if (availW < size.x || availH < size.y) continue
        val pos = Vec2(
                if (dir == Dir.Left) rAvoid.min.x - size.x else if (dir == Dir.Right) rAvoid.max.x else basePosClamped.x,
                if (dir == Dir.Up) rAvoid.min.y - size.y else if (dir == Dir.Down) rAvoid.max.y else basePosClamped.y)
        lastDir.set(dir)
        return pos
    }
    // Fallback, try to keep within display
    lastDir.set(Dir.None)
    return Vec2(refPos).apply {
        x = max(min(x + size.x, rOuter.max.x) - size.x, rOuter.min.x)
        y = max(min(y + size.y, rOuter.max.y) - size.y, rOuter.min.y)
    }
}

fun findBestWindowPosForPopup(window: Window): Vec2 {

    val rOuter = findAllowedExtentRectForWindow(window)
    if (window.flags has Wf.ChildMenu) {
        /*  Child menus typically request _any_ position within the parent menu item,
            and then our FindBestWindowPosForPopup() function will move the new menu outside the parent bounds.
            This is how we end up with child menus appearing (most-commonly) on the right of the parent menu. */
        assert(g.currentWindow === window)
        val parentWindow = g.currentWindowStack[g.currentWindowStack.size - 2]
        // We want some overlap to convey the relative depth of each menu (currently the amount of overlap is hard-coded to style.ItemSpacing.x).
        val horizontalOverlap = style.itemSpacing.x
        val rAvoid = parentWindow.run {
            when {
                dc.menuBarAppending -> Rect(-Float.MAX_VALUE, pos.y + titleBarHeight, Float.MAX_VALUE, pos.y + titleBarHeight + menuBarHeight)
                else -> Rect(pos.x + horizontalOverlap, -Float.MAX_VALUE, pos.x + size.x - horizontalOverlap - scrollbarSizes.x, Float.MAX_VALUE)
            }
        }
        return findBestWindowPosForPopupEx(Vec2(window.pos), window.size, window::autoPosLastDirection, rOuter, rAvoid)
    }
    if (window.flags has Wf.Popup) {
        val rAvoid = Rect(window.pos.x - 1, window.pos.y - 1, window.pos.x + 1, window.pos.y + 1)
        return findBestWindowPosForPopupEx(Vec2(window.pos), window.size, window::autoPosLastDirection, rOuter, rAvoid)
    }
    if (window.flags has Wf.Tooltip) {
        // Position tooltip (always follows mouse)
        val sc = style.mouseCursorScale
        val refPos = navCalcPreferredRefPos()
        val rAvoid = when {
            !g.navDisableHighlight && g.navDisableMouseHover && !(io.configFlags has Cf.NavEnableSetMousePos) ->
                Rect(refPos.x - 16, refPos.y - 8, refPos.x + 16, refPos.y + 8)
            else -> Rect(refPos.x - 16, refPos.y - 8, refPos.x + 24 * sc, refPos.y + 24 * sc) // FIXME: Hard-coded based on mouse cursor shape expectation. Exact dimension not very important.
        }
        val pos = findBestWindowPosForPopupEx(refPos, window.size, window::autoPosLastDirection, rOuter, rAvoid)
        if (window.autoPosLastDirection == Dir.None)
        // If there's not enough room, for tooltip we prefer avoiding the cursor at all cost even if it means that part of the tooltip won't be visible.
            pos(refPos + 2)
        return pos
    }
    assert(false)
    return Vec2(window.pos)
}

/** Return false to discard a character.    */
fun inputTextFilterCharacter(char: KMutableProperty0<Char>, flags: InputTextFlags/*, ImGuiTextEditCallback callback, void* user_data*/)
        : Boolean {

    var c = char()

    if (c < 128 && c != ' ' && !c.isPrintable) {
        var pass = false
        pass = pass or (c == '\n' && flags has Itf.Multiline)
        pass = pass or (c == '\t' && flags has Itf.AllowTabInput)
        if (!pass) return false
    }

    /*  Filter private Unicode range. I don't imagine anybody would want to input them. GLFW on OSX seems to send
        private characters for special keys like arrow keys.     */
    if (c >= 0xE000 && c <= 0xF8FF) return false
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

        if (flags has Itf.CharsUppercase && c in 'a'..'z') {
            c += 'A' - 'a'
            char.set(c)
        }

        if (flags has Itf.CharsNoBlank && c.isBlankW)
            return false
    }

    if (flags has Itf.CallbackCharFilter) {
        TODO()
//        ImGuiTextEditCallbackData callback_data
//                memset(& callback_data, 0, sizeof(ImGuiTextEditCallbackData))
//        callback_data.EventFlag = ImGuiInputTextFlags_CallbackCharFilter
//        callback_data.EventChar = (ImWchar) c
//                callback_data.Flags = flags
//        callback_data.UserData = user_data
//        if (callback(& callback_data) != 0)
//        return false
//        *p_char = callback_data.EventChar
//        if (!callback_data.EventChar)
//            return false
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
        val charWidth: Float = font.getCharAdvance_ssaaaaaaaa(c) * scale
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

/** DataTypeFormatString */
fun KMutableProperty0<*>.format(dataType: DataType, format: String): CharArray {
    val value: Number = when (dataType) {
        DataType.Int, DataType.Uint -> this() as Int    // Signedness doesn't matter when pushing the argument
        DataType.Long, DataType.Ulong -> this() as Long // Signedness doesn't matter when pushing the argument
        DataType.Float -> this() as Float
        DataType.Double -> this() as Double
        else -> throw Error()
    }
    return format.format(style.locale, value).toCharArray()
}

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

fun dataTypeApplyOp(dataType: DataType, op: Char, value1: Number, value2: Number): Number {
    assert(op == '+' || op == '-')
    return when (dataType) {
        DataType.Int, DataType.Uint -> when (op) {  // Signedness doesn't matter when adding or subtracting
            '+' -> value1 as Int + (value2 as Int)
            '-' -> value1 as Int - (value2 as Int)
            else -> throw Error()
        }
        DataType.Long, DataType.Ulong -> when (op) {  // Signedness doesn't matter when adding or subtracting
            '+' -> value1 as Long + (value2 as Long)
            '-' -> value1 as Long - (value2 as Long)
            else -> throw Error()
        }
        DataType.Float -> when (op) {
            '+' -> value1 as Float + (value2 as Float)
            '-' -> value1 as Float - (value2 as Float)
            else -> throw Error()
        }
        DataType.Double -> when (op) {
            '+' -> value1 as Double + (value2 as Double)
            '-' -> value1 as Double - (value2 as Double)
            else -> throw Error()
        }
        else -> throw Error()
    }
}

/** User can input math operators (e.g. +100) to edit a numerical values.
 *  NB: This is _not_ a full expression evaluator. We should probably add one and replace this dumb mess.. */
fun dataTypeApplyOpFromText(buf: CharArray, initialValueBuf: CharArray, dataType: DataType, data: IntArray, format: String? = null): Boolean {

    i0 = data[0]
    val res = dataTypeApplyOpFromText(buf, initialValueBuf, dataType, ::i0, format)
    data[0] = i0
    return res
}

fun dataTypeApplyOpFromText(buf: CharArray, initialValueBuf: CharArray, dataType: DataType, dataPtr: KMutableProperty0<*>,
                            format: String? = null): Boolean {

//    var s = 0
//    while (buf[s].isSpace)
//        s++
//
//    /*  We don't support '-' op because it would conflict with inputing negative value.
//        Instead you can use +-100 to subtract from an existing value     */
//    var op = buf[s]
//    if (op == '+' || op == '*' || op == '/') {
//        s++
//        while (buf[s].isSpace)
//            s++
//    } else
//        op = NUL
//
//    if (buf[s] == NUL)
//        return false

    dataPtr as KMutableProperty0<Number>

    val seq = String(buf)
            .replace(Regex("\\s+"), "")
            .replace("$NUL", "")
            .split(Regex("-+\\*/"))
    return when (buf[0]) {
        NUL -> false
        else -> when (dataType) {
            DataType.Int -> {
                val format = format ?: "%d"
                var v = dataPtr() as Int
                val oldV = v
                val a = try {
                    seq[0].format(style.locale, format).i
                } catch (_: Exception) {
                    return false
                }

                v = when (seq.size) {
                    2 -> {   // TODO support more complex operations? i.e: a + b * c
                        val op = seq[1][0]
                        /*  Store operand b in a float so we can use fractional value for multipliers (*1.1), but constant
                                always parsed as integer so we can fit big integers (e.g. 2000000003) past float precision  */
                        when (op) {
                            '+' -> a + seq[2].i         // Add (use "+-" to subtract)
                            '*' -> (a * seq[2].f).i     // Multiply
                            '/' -> {                    // Divide
                                val b = seq[2].f
                                when (b) {
                                    0f -> v
                                    else -> (a / b).i
                                }
                            }
                            else -> throw Error()
                        }
                    }
                    else -> try { // Assign constant
                        seq[1].format(style.locale, format).i
                    } catch (_: Exception) {
                        v
                    }
                }
                dataPtr.set(v)
                oldV != v
            }

            DataType.Uint, DataType.Long, DataType.Ulong ->
                /*  Assign constant
                    FIXME: We don't bother handling support for legacy operators since they are a little too crappy.
                    Instead we may implement a proper expression evaluator in the future.                 */
                //sscanf(buf, format, data_ptr)
                false

            DataType.Float -> {
                // For floats we have to ignore format with precision (e.g. "%.2f") because sscanf doesn't take them in TODO not true in java
                val format = format ?: "%f"
                var v = dataPtr() as Float
                val oldV = v
                val a = try {
                    seq[0].format(style.locale, format).f
                } catch (_: Exception) {
                    return false
                }
                val b = try {
                    seq[2].f
                } catch (_: Exception) {
                    return false
                }

                v = when (seq.size) {
                    2 -> {   // TODO support more complex operations? i.e: a + b * c
                        val op = seq[1][0]
                        when (op) {
                            '+' -> a + b                        // Add (use "+-" to subtract)
                            '*' -> a * b                        // Multiply
                            '/' -> if (b != 0f) a / b else v    // Divide
                            else -> throw Error()
                        }
                    }
                    else -> b   // Assign constant
                }
                dataPtr.set(v)
                oldV != v
            }
            DataType.Double -> {
                // For floats we have to ignore format with precision (e.g. "%.2f") because sscanf doesn't take them in TODO not true in java
//                val scalarFormat = scalarFormat ?: "%f"
                var v = dataPtr() as Double
                val oldV = v
                val a = try {
                    seq[0].format(style.locale, format).d
                } catch (_: Exception) {
                    return false
                }
                val b = try {
                    seq[2].d
                } catch (_: Exception) {
                    return false
                }

                v = when (seq.size) {
                    2 -> {   // TODO support more complex operations? i.e: a + b * c
                        val op = seq[1][0]
                        when (op) {
                            '+' -> a + b                        // Add (use "+-" to subtract)
                            '*' -> a * b                        // Multiply
                            '/' -> if (b != 0.0) a / b else v   // Divide
                            else -> throw Error()
                        }
                    }
                    else -> b   // Assign constant
                }
                dataPtr.set(v)
                oldV != v
            }
            else -> false
        }
    }
}


/** NB: We modify rect_rel by the amount we scrolled for, so it is immediately updated. */
fun navScrollToBringItemIntoView(window: Window, itemRectRel: Rect) {
    // Scroll to keep newly navigated item fully into view
    val windowRectRel = Rect(window.innerRect.min - window.pos - Vec2(1), window.innerRect.max - window.pos + Vec2(1))
    //g.OverlayDrawList.AddRect(window->Pos + window_rect_rel.Min, window->Pos + window_rect_rel.Max, IM_COL32_WHITE); // [DEBUG]
    if (windowRectRel contains itemRectRel) return

    if (window.scrollbar.x && itemRectRel.min.x < windowRectRel.min.x) {
        window.scrollTarget.x = itemRectRel.min.x + window.scroll.x - style.itemSpacing.x
        window.scrollTargetCenterRatio.x = 0f
    } else if (window.scrollbar.x && itemRectRel.max.x >= windowRectRel.max.x) {
        window.scrollTarget.x = itemRectRel.max.x + window.scroll.x + style.itemSpacing.x
        window.scrollTargetCenterRatio.x = 1f
    }
    if (itemRectRel.min.y < windowRectRel.min.y) {
        window.scrollTarget.y = itemRectRel.min.y + window.scroll.y - style.itemSpacing.y
        window.scrollTargetCenterRatio.y = 0f
    } else if (itemRectRel.max.y >= windowRectRel.max.y) {
        window.scrollTarget.y = itemRectRel.max.y + window.scroll.y + style.itemSpacing.y
        window.scrollTargetCenterRatio.y = 1f
    }

    // Estimate upcoming scroll so we can offset our relative mouse position so mouse position can be applied immediately (under this block)
    val nextScroll = calcNextScrollFromScrollTargetAndClamp(window)
    itemRectRel translate (window.scroll - nextScroll)
}


fun navUpdate() {

    io.wantSetMousePos = false

//    if (g.NavScoringCount > 0) printf("[%05d] NavScoringCount %d for '%s' layer %d (Init:%d, Move:%d)\n", g.FrameCount, g.NavScoringCount, g.NavWindow ? g . NavWindow->Name : "NULL", g.NavLayer, g.NavInitRequest || g.NavInitResultId != 0, g.NavMoveRequest)

    if (g.io.configFlags has Cf.NavEnableGamepad && io.backendFlags has BackendFlag.HasGamepad)
        if (g.io.navInputs[NavInput.Activate] > 0f || g.io.navInputs[NavInput.Input] > 0f ||
                g.io.navInputs[NavInput.Cancel] > 0f || g.io.navInputs[NavInput.Menu] > 0f)
            g.navInputSource = InputSource.NavGamepad

    // Update Keyboard->Nav inputs mapping
    if (io.configFlags has Cf.NavEnableKeyboard) {
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
        if (io.keyCtrl) io.navInputs[NavInput.TweakSlow] = 1f
        if (io.keyShift) io.navInputs[NavInput.TweakFast] = 1f
        if (io.keyAlt) io.navInputs[NavInput.KeyMenu] = 1f
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
        g.navWindow!!.navRectRel[g.navLayer] = g.navInitResultRectRel
    }
    g.navInitRequest = false
    g.navInitRequestFromMove = false
    g.navInitResultId = 0
    g.navJustMovedToId = 0

    // Process navigation move request
    if (g.navMoveRequest && (g.navMoveResultLocal.id != 0 || g.navMoveResultOther.id != 0)) {
        // Select which result to use
        var result = if (g.navMoveResultLocal.id != 0) g.navMoveResultLocal else g.navMoveResultOther
        // Maybe entering a flattened child? In this case solve the tie using the regular scoring rules
        if (g.navMoveResultOther.id != 0 && g.navMoveResultOther.window!!.parentWindow === g.navWindow)
            if (g.navMoveResultOther.distBox < g.navMoveResultLocal.distBox || (g.navMoveResultOther.distBox == g.navMoveResultLocal.distBox && g.navMoveResultOther.distCenter < g.navMoveResultLocal.distCenter))
                result = g.navMoveResultOther

        assert(g.navWindow != null)

        // Scroll to keep newly navigated item fully into view
        if (g.navLayer == 0)
            navScrollToBringItemIntoView(result.window!!, result.rectRel)

        // Apply result from previous frame navigation directional move request
        clearActiveId()
        g.navWindow = result.window!!
        setNavIDWithRectRel(result.id, g.navLayer, result.rectRel)
        g.navJustMovedToId = result.id
        g.navMoveFromClampedRefRect = false
    }

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
        if (io.configFlags has Cf.NavEnableSetMousePos && io.backendFlags has BackendFlag.HasSetMousePos) {
            assert(!g.navDisableHighlight && g.navDisableMouseHover)
            io.mousePosPrev = navCalcPreferredRefPos()
            io.mousePos put io.mousePosPrev
            io.wantSetMousePos = true
        }
        g.navMousePosDirty = false
    }
    g.navIdIsAlive = false
    g.navJustTabbedId = 0
    assert(g.navLayer == 0 || g.navLayer == 1)

    // Store our return window (for returning from Layer 1 to Layer 0) and clear it as soon as we step back in our own Layer 0
    g.navWindow?.let {
        navSaveLastChildNavWindow(it)
        if (it.navLastChildNavWindow != null && g.navLayer == 0)
            it.navLastChildNavWindow = null
    }

    navUpdateWindowing()

    // Set output flags for user application
    io.navActive = io.configFlags has (Cf.NavEnableGamepad or Cf.NavEnableKeyboard) && g.navWindow != null && g.navWindow!!.flags hasnt Wf.NoNavInputs
    val navKeyboardActive = io.configFlags has Cf.NavEnableKeyboard
    val navGamepadActive = io.configFlags has Cf.NavEnableGamepad && io.backendFlags has BackendFlag.HasGamepad
    io.navActive = (navKeyboardActive || navGamepadActive) && g.navWindow?.flags?.hasnt(Wf.NoNavInputs) ?: false
    io.navVisible = (io.navActive && g.navId != 0 && !g.navDisableHighlight) || g.navWindowingTarget != null || g.navInitRequest

    // Process NavCancel input (to close a popup, get back to parent, clear focus)
    if (NavInput.Cancel.isPressed(InputReadMode.Pressed)) {
        if (g.activeId != 0) {
            clearActiveId()
        } else if (g.navWindow != null && g.navWindow!!.flags has Wf.ChildWindow && g.navWindow!!.flags hasnt Wf.Popup && g.navWindow!!.parentWindow != null) {
            // Exit child window
            val childWindow = g.navWindow!!
            val parentWindow = childWindow.parentWindow!!
            assert(childWindow.childId != 0)
            parentWindow.focus()
            setNavId(childWindow.childId, 0)
            g.navIdIsAlive = false
            if (g.navDisableMouseHover)
                g.navMousePosDirty = true
        } else if (g.openPopupStack.isNotEmpty()) {
            // Close open popup/menu
            if (g.openPopupStack.last().window!!.flags hasnt Wf.Modal)
                closePopupToLevel(g.openPopupStack.lastIndex)
        } else if (g.navLayer != 0)
            navRestoreLayer(0)  // Leave the "menu" layer
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
    } else {
        /*  Forwarding previous request (which has been modified, e.g. wrap around menus rewrite the requests with
            a starting rectangle at the other side of the window)   */
        assert(g.navMoveDir != Dir.None)
        assert(g.navMoveRequestForward == NavForward.ForwardQueued)
        g.navMoveRequestForward = NavForward.ForwardActive
    }

    if (g.navMoveDir != Dir.None) {
        g.navMoveRequest = true
        g.navMoveDirLast = g.navMoveDir
    }

    /*  If we initiate a movement request and have no current navId, we initiate a InitDefautRequest that will be used
        as a fallback if the direction fails to find a match     */
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
            // *Fallback* manual-scroll with NavUp/NavDown when window has no navigable item
            val scrollSpeed = glm.floor(it.calcFontSize() * 100 * io.deltaTime + 0.5f) // We need round the scrolling speed because sub-pixel scroll isn't reliably supported.
            if (it.dc.navLayerActiveMask == 0 && it.dc.navHasScroll && g.navMoveRequest) {
                if (g.navMoveDir == Dir.Left || g.navMoveDir == Dir.Right)
                    it.setScrollX(glm.floor(it.scroll.x + (if (g.navMoveDir == Dir.Left) -1f else 1f) * scrollSpeed))
                if (g.navMoveDir == Dir.Up || g.navMoveDir == Dir.Down)
                    it.setScrollY(glm.floor(it.scroll.y + (if (g.navMoveDir == Dir.Up) -1f else 1f) * scrollSpeed))
            }

            // *Normal* Manual scroll with NavScrollXXX keys
            // Next movement request will clamp the NavId reference rectangle to the visible area, so navigation will resume within those bounds.
            val scrollDir = getNavInputAmount2d(NavDirSourceFlag.PadLStick.i, InputReadMode.Down, 1f / 10f, 10f)
            if (scrollDir.x != 0f && it.scrollbar.x) {
                it.setScrollX(glm.floor(it.scroll.x + scrollDir.x * scrollSpeed))
                g.navMoveFromClampedRefRect = true
            }
            if (scrollDir.y != 0f) {
                it.setScrollY(glm.floor(it.scroll.y + scrollDir.y * scrollSpeed))
                g.navMoveFromClampedRefRect = true
            }
        }
    }
    // Reset search results
    g.navMoveResultLocal.clear()
    g.navMoveResultOther.clear()

    // When we have manually scrolled (without using navigation) and NavId becomes out of bounds, we project its bounding box to the visible area to restart navigation within visible items
    if (g.navMoveRequest && g.navMoveFromClampedRefRect && g.navLayer == 0) {
        val window = g.navWindow!!
        val windowRectRel = Rect(window.innerRect.min - window.pos - 1, window.innerRect.max - window.pos + 1)
        if (!windowRectRel.contains(window.navRectRel[g.navLayer])) {
            val pad = window.calcFontSize() * 0.5f
            windowRectRel expand Vec2(-min(windowRectRel.width, pad), -min(windowRectRel.height, pad)) // Terrible approximation for the intent of starting navigation from first fully visible item
            window.navRectRel[g.navLayer] clipWith windowRectRel
            g.navId = 0
        }
        g.navMoveFromClampedRefRect = false
    }

    // For scoring we use a single segment on the left side our current item bounding box (not touching the edge to avoid box overlap with zero-spaced items)
    g.navWindow.let {
        if (it != null) {
            val navRectRel = if (!it.navRectRel[g.navLayer].isInverted) Rect(it.navRectRel[g.navLayer]) else Rect(0f, 0f, 0f, 0f)
            g.navScoringRectScreen.put(navRectRel.min + it.pos, navRectRel.max + it.pos)
        } else g.navScoringRectScreen put getViewportRect()

    }
    g.navScoringRectScreen.min.x = min(g.navScoringRectScreen.min.x + 1f, g.navScoringRectScreen.max.x)
    g.navScoringRectScreen.max.x = g.navScoringRectScreen.min.x
    // Ensure if we have a finite, non-inverted bounding box here will allows us to remove extraneous abs() calls in navScoreItem().
    assert(!g.navScoringRectScreen.isInverted)
    //g.OverlayDrawList.AddRect(g.NavScoringRectScreen.Min, g.NavScoringRectScreen.Max, IM_COL32(255,200,0,255)); // [DEBUG]
    g.navScoringCount = 0
    if (IMGUI_DEBUG_NAV_RECTS)
        g.navWindow?.let {
            for (layer in 0..1)
                overlayDrawList.addRect(it.navRectRel[layer].min + it.pos, it.navRectRel[layer].max + it.pos, COL32(255, 200, 0, 255))
            val col = if (it.hiddenFrames == 0) COL32(255, 0, 255, 255) else COL32(255, 0, 0, 255)
            val p = navCalcPreferredRefPos()
            g.overlayDrawList.addCircleFilled(p, 3f, col)
            g.overlayDrawList.addText(null, 13f, p + Vec2(8, -4), col, "${g.navLayer}".toCharArray())
        }
}

// Window management mode (hold to: change focus/move/resize, tap to: toggle menu layer)
fun navUpdateWindowing() {

    var applyFocusWindow: Window? = null
    var applyToggleLayer = false

    val startWindowingWithGamepad = g.navWindowingTarget == null && NavInput.Menu.isPressed(InputReadMode.Pressed)
    val startWindowingWithKeyboard = g.navWindowingTarget == null && io.keyCtrl && Key.Tab.isPressed && io.configFlags has Cf.NavEnableKeyboard
    if (startWindowingWithGamepad || startWindowingWithKeyboard)
        (g.navWindow ?: findWindowNavigable(g.windows.lastIndex, -Int.MAX_VALUE, -1))?.let {
            g.navWindowingTarget = it.rootWindowForTabbing
            g.navWindowingHighlightAlpha = 0f
            g.navWindowingHighlightTimer = 0f
            g.navWindowingToggleLayer = !startWindowingWithKeyboard
            g.navInputSource = if (startWindowingWithKeyboard) InputSource.NavKeyboard else InputSource.NavGamepad
        }

    // Gamepad update
    g.navWindowingHighlightTimer += io.deltaTime
    g.navWindowingTarget?.let {
        if (g.navInputSource == InputSource.NavGamepad) {
            /*  Highlight only appears after a brief time holding the button, so that a fast tap on PadMenu
                (to toggle NavLayer) doesn't add visual noise             */
            g.navWindowingHighlightAlpha = max(g.navWindowingHighlightAlpha, saturate((g.navWindowingHighlightTimer - 0.2f) / 0.05f))

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
            g.navWindowingHighlightAlpha = max(g.navWindowingHighlightAlpha, saturate((g.navWindowingHighlightTimer - 0.15f) / 0.04f)) // 1.0f
            if (Key.Tab.isPressed(true))
                navUpdateWindowingHighlightWindow(if (io.keyShift) 1 else -1)
            if (!io.keyCtrl)
                applyFocusWindow = g.navWindowingTarget
        }
    }

    // Keyboard: Press and Release ALT to toggle menu layer
    // FIXME: We lack an explicit IO variable for "is the imgui window focused", so compare mouse validity to detect the common case of back-end clearing releases all keys on ALT-TAB
    if ((g.activeId == 0 || g.activeIdAllowOverlap) && NavInput.KeyMenu.isPressed(InputReadMode.Released))
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
                val moveSpeed = glm.floor(NAV_MOVE_SPEED * io.deltaTime * min(io.displayFramebufferScale.x, io.displayFramebufferScale.y))
                it.pos plusAssign moveDelta * moveSpeed
                g.navDisableMouseHover = true
                it.markIniSettingsDirty()
            }
        }
    }

    // Apply final focus
    if (applyFocusWindow != null && (g.navWindow == null || applyFocusWindow !== g.navWindow!!.rootWindowForTabbing)) {
        g.navDisableHighlight = false
        g.navDisableMouseHover = true
        applyFocusWindow = navRestoreLastChildNavWindow(applyFocusWindow!!)
        closePopupsOverWindow(applyFocusWindow)
        applyFocusWindow.focus()
        if (applyFocusWindow!!.navLastIds[0] == 0)
            navInitWindow(applyFocusWindow!!, false)

        // If the window only has a menu layer, select it directly
        if (applyFocusWindow!!.dc.navLayerActiveMask == 1 shl 1)
            g.navLayer = 1
    }
    applyFocusWindow?.let { g.navWindowingTarget = null }

    // Apply menu/layer toggle
    if (applyToggleLayer)
        g.navWindow?.let {
            var newNavWindow = it
            while (newNavWindow.dc.navLayerActiveMask hasnt (1 shl 1) && newNavWindow.flags has Wf.ChildWindow && newNavWindow.flags hasnt (Wf.Popup or Wf.ChildMenu))
                newNavWindow = newNavWindow.parentWindow!!
            if (newNavWindow !== it) {
                val oldNavWindow = it
                newNavWindow.focus()
                newNavWindow.navLastChildNavWindow = oldNavWindow
            }
            g.navDisableHighlight = false
            g.navDisableMouseHover = true
            navRestoreLayer(if (it.dc.navLayerActiveMask has (1 shl 1)) g.navLayer xor 1 else 0)
        }
}

/** We get there when either navId == id, or when g.navAnyRequest is set (which is updated by navUpdateAnyRequestFlag above)    */
fun navProcessItem(window: Window, navBb: Rect, id: ID) {

    //if (!g.io.NavActive)  // [2017/10/06] Removed this possibly redundant test but I am not sure of all the side-effects yet. Some of the feature here will need to work regardless of using a _NoNavInputs flag.
    //    return;

    val itemFlags = window.dc.itemFlags
    val navBbRel = Rect(navBb.min - window.pos, navBb.max - window.pos)
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

    /*  Scoring for navigation
        FIXME-NAV: Consider policy for double scoring
        (scoring from NavScoringRectScreen + scoring from a rect wrapped according to current wrapping policy)     */
    if (g.navId != id && itemFlags hasnt ItemFlag.NoNav) {
        val result = if (window === g.navWindow) g.navMoveResultLocal else g.navMoveResultOther
        val newBest = when {
            IMGUI_DEBUG_NAV_SCORING -> {  // [DEBUG] Score all items in NavWindow at all times
                    if (!g.navMoveRequest) g.navMoveDir = g.navMoveDirLast
                    navScoreItem(result, navBb) && g.navMoveRequest
            }
            else -> g.navMoveRequest && navScoreItem(result, navBb)
        }
        if (newBest) {
            result.id = id
            result.parentId = window.idStack.last()
            result.window = window
            result.rectRel put navBbRel
        }
    }

    // Update window-relative bounding box of navigated item
    if (g.navId == id) {
        g.navWindow = window    // Always refresh g.NavWindow, because some operations such as FocusItem() don't have a window.
        g.navLayer = window.dc.navLayerCurrent
        g.navIdIsAlive = true
        g.navIdTabCounter = window.focusIdxTabCounter
        window.navRectRel[window.dc.navLayerCurrent] = navBbRel    // Store item bounding box (relative to window position)
    }
}


//-----------------------------------------------------------------------------
// Platform dependent default implementations
//-----------------------------------------------------------------------------

//static const char*      GetClipboardTextFn_DefaultImpl(void* user_data);
//static void             SetClipboardTextFn_DefaultImpl(void* user_data, const char* text);
//static void             ImeSetInputScreenPosFn_DefaultImpl(int x, int y);

private var i0 = 0


fun navCalcPreferredRefPos(): Vec2 {
    if (g.navDisableHighlight || !g.navDisableMouseHover || g.navWindow == null)
        return glm.floor(io.mousePos)

    // When navigation is active and mouse is disabled, decide on an arbitrary position around the bottom left of the currently navigated item
    val rectRel = g.navWindow!!.navRectRel[g.navLayer]
    val pos = g.navWindow!!.pos + Vec2(rectRel.min.x + min(g.style.framePadding.x * 4, rectRel.width),
            rectRel.max.y - min(g.style.framePadding.y, rectRel.height))
    val visibleRect = getViewportRect()
    return glm.floor(glm.clamp(Vec2(pos), visibleRect.min, visibleRect.max))   // ImFloor() is important because non-integer mouse position application in back-end might be lossy and result in undesirable non-zero delta.
}

fun isNavInputPressedAnyOfTwo(n1: NavInput, n2: NavInput, mode: InputReadMode) = getNavInputAmount(n1, mode) + getNavInputAmount(n2, mode) > 0f

// FIXME-OPT O(N)
fun findWindowNavigable(iStart: Int, iStop: Int, dir: Int): Window? {
    var i = iStart
    while (i in g.windows.indices && i != iStop) {
        if (g.windows[i].isNavFocusable)
            return g.windows[i]
        i += dir
    }
    return null
}

fun navUpdateWindowingHighlightWindow(focusChangeDir: Int) {

    val target = g.navWindowingTarget!!
    if (target.flags has Wf.Modal) return

    val iCurrent = findWindowIndex(target)
    val windowTarget = findWindowNavigable(iCurrent + focusChangeDir, -Int.MAX_VALUE, focusChangeDir)
            ?: findWindowNavigable(if (focusChangeDir < 0) g.windows.lastIndex else 0, iCurrent, focusChangeDir)
    g.navWindowingTarget = windowTarget
    g.navWindowingToggleLayer = false
}

// FIXME-OPT O(N)
fun findWindowIndex(window: Window): Int {
    var i = g.windows.lastIndex
    while (i >= 0) {
        if (g.windows[i] == window)
            return i
        i--
    }
    return -1
}