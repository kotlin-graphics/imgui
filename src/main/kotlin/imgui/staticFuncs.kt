package imgui

import gli_.has
import glm_.*
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import imgui.Context.style
import imgui.ImGui.getColumnOffset
import imgui.internal.*
import uno.kotlin.isPrintable
import java.io.File
import java.nio.file.Paths
import imgui.Context as g
import imgui.InputTextFlags as Itf
import imgui.WindowFlags as Wf


fun logRenderedText(refPos: Vec2?, text: String, textEnd: Int = 0): Nothing = TODO()

fun getDraggedColumnOffset(columnIndex: Int): Float {
    /*  Active (dragged) column always follow mouse. The reason we need this is that dragging a column to the right edge
        of an auto-resizing window creates a feedback loop because we store normalized positions. So while dragging we
        enforce absolute positioning.   */

    val window = g.currentWindow!!
    /*  We cannot drag column 0. If you get this assert you may have a conflict between the ID of your columns and
        another widgets.    */
    assert(columnIndex > 0)
    assert(g.activeId == window.dc.columnsSetId + columnIndex)

    var x = IO.mousePos.x - g.activeIdClickOffset.x - window.pos.x
    x = glm.max(x, getColumnOffset(columnIndex - 1) + style.columnsMinSpacing)
    if (window.dc.columnsFlags has ColumnsFlags.NoPreserveWidths)
        x = glm.min(x, getColumnOffset(columnIndex + 1) - style.columnsMinSpacing)

    return x
}

val defaultFont get() = IO.fontDefault ?: IO.fonts.fonts[0]

//-----------------------------------------------------------------------------
// Internal API exposed in imgui_internal.h
//-----------------------------------------------------------------------------

/** Find window given position, search front-to-back
FIXME: Note that we have a lag here because WindowRectClipped is updated in Begin() so windows moved by user via
SetWindowPos() and not SetNextWindowPos() will have that rectangle lagging by a frame at the time
FindHoveredWindow() is called, aka before the next Begin(). Moving window thankfully isn't affected.    */
fun findHoveredWindow(pos: Vec2): Window? {
    for (i in g.windows.size - 1 downTo 0) {
        val window = g.windows[i]
        if (!window.active)
            continue
        if (window.flags has Wf.NoInputs)
            continue

        // Using the clipped AABB, a child window will typically be clipped by its parent (not always)
        val bb = Rect(window.windowRectClipped.min - style.touchExtraPadding, window.windowRectClipped.max + style.touchExtraPadding)
        if (bb contains pos)
            return window
    }
    return null
}

fun createNewWindow(name: String, size: Vec2, flags: Int) = Window(name).apply {
    // Create window the first time

    this.flags = flags

    if (flags has Wf.NoSavedSettings) {
        // User can disable loading and saving of settings. Tooltip and child windows also don't store settings.
        sizeFull put size
        this.size put size
    } else {
        /*  Retrieve settings from .ini file
            Use SetWindowPos() or SetNextWindowPos() with the appropriate condition flag to change the initial position
            of a window.    */
        posF put 60
        pos.put(posF.x.i.f, posF.y.i.f)

        var settings = findWindowSettings(name)
        if (settings == null)
            settings = addWindowSettings(name)
        else {
            setWindowPosAllowFlags = setWindowPosAllowFlags wo Cond.FirstUseEver
            setWindowSizeAllowFlags = setWindowSizeAllowFlags wo Cond.FirstUseEver
            setWindowCollapsedAllowFlags = setWindowCollapsedAllowFlags wo Cond.FirstUseEver
        }

        if (settings.pos.x != Int.MAX_VALUE) {
            posF put settings.pos
            pos put posF
            collapsed = settings.collapsed
        }

        if (settings.size.lengthSqr > 0.00001f)
            size put settings.size
        sizeFull put size
        this.size put size
    }

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


fun clearSetNextWindowData() {
    // FIXME-OPT
    g.setNextWindowPosCond = Cond.Null
    g.setNextWindowSizeCond = Cond.Null
    g.setNextWindowPosCond = Cond.Null
    g.setNextWindowPosCond = Cond.Null
    g.setNextWindowSizeConstraint = false
    g.setNextWindowFocus = false
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
    run {
        // Too few or too many PopStyleColor()
        val current = g.colorModifiers.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] == current, { "PushStyleColor/PopStyleColor Mismatch!" })
        ptr++
    }
    run {
        // Too few or too many PopStyleVar()
        val current = g.styleModifiers.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] == current, { "PushStyleVar/PopStyleVar Mismatch!" })
        ptr++
    }
    run {
        // Too few or too many PopFont()
        val current = g.fontStack.size
        if (write) backup[ptr] = current
        else assert(backup[ptr] == current, { "PushFont/PopFont Mismatch!" })
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
    scroll max_ 0f
    if (!window.collapsed && !window.skipItems) {
        // == GetScrollMaxX for that window
        scroll.x = glm.min(scroll.x, glm.max(0f, window.sizeContents.x - (window.sizeFull.x - window.scrollbarSizes.x)))
        // == GetScrollMaxY for that window
        scroll.y = glm.min(scroll.y, glm.max(0f, window.sizeContents.y - (window.sizeFull.y - window.scrollbarSizes.y)))
    }
    return scroll
}

fun findWindowSettings(name: String) = hash(name, 0).let { id -> g.settings.firstOrNull { it.id == id } }

fun addWindowSettings(name: String) = IniData().apply {
    g.settings.add(this)
    this.name = name
    id = hash(name, 0)
    collapsed = false
    pos put Int.MAX_VALUE
    size put 0
}

fun loadIniSettingsFromDisk(iniFilename: String?) {
    if (iniFilename == null) return
    var settings: IniData? = null
    val file = Paths.get(iniFilename).toFile()
    if (file.exists()) file.readLines().filter { it.isNotEmpty() }.forEach { s ->
        if (s[0] == '[' && s.last() == ']') {
            val name = s.substring(1, s.lastIndex)
            settings = findWindowSettings(name) ?: addWindowSettings(name)
        } else settings?.apply {
            when {
                s.startsWith("Pos") -> pos.put(s.substring(4).split(","))
                s.startsWith("Size") -> size put glm.max(Vec2i(s.substring(5).split(",")), style.windowMinSize)
                s.startsWith("Collapsed") -> collapsed = s.substring(10).toBoolean()
            }
        }
    }
}

fun saveIniSettingsToDisk(iniFilename: String?) {

    g.settingsDirtyTimer = 0f
    if (iniFilename == null) return

    // Gather data from windows that were active during this session
    for (window in g.windows) {

        if (window.flags has Wf.NoSavedSettings) continue
        /** This will only return NULL in the rare instance where the window was first created with
         *  WindowFlags.NoSavedSettings then had the flag disabled later on.
         *  We don't bind settings in this case (bug #1000).    */
        val settings = findWindowSettings(window.name) ?: continue
        settings.pos put window.pos
        settings.size put window.sizeFull
        settings.collapsed = window.collapsed
    }

    /*  Write .ini file
        If a window wasn't opened in this session we preserve its settings     */
    File(Paths.get(iniFilename).toUri()).printWriter().use {
        for (setting in g.settings) {
            if (setting.pos.x == Int.MAX_VALUE) continue
            // Skip to the "###" marker if any. We don't skip past to match the behavior of GetID()
            val name = setting.name.substringBefore("###")
            it.println("[$name]")
            it.println("Pos=${setting.pos.x},${setting.pos.y}")
            it.println("Size=${setting.size.x.i},${setting.size.y.i}")
            it.println("Collapsed=${setting.collapsed.i}")
            it.println()
        }
    }
}

fun markIniSettingsDirty(window: Window) {
    if (window.flags hasnt Wf.NoSavedSettings)
        if (g.settingsDirtyTimer <= 0f)
            g.settingsDirtyTimer = IO.iniSavingRate
}

fun getVisibleRect(): Rect {
    if (IO.displayVisibleMin != IO.displayVisibleMax)
        return Rect(IO.displayVisibleMin, IO.displayVisibleMax)
    return Rect(0f, 0f, IO.displaySize.x.f, IO.displaySize.y.f)
}

fun closeInactivePopups(refWindow: Window?) {

    if (g.openPopupStack.empty())
        return

    /*  When popups are stacked, clicking on a lower level popups puts focus back to it and close popups above it.
        Don't close our own child popup windows */
    var n = 0
    if (refWindow != null)
        while (n < g.openPopupStack.size) {
            val popup = g.openPopupStack[n]
            if (popup.window == null) {
                n++
                continue
            }
            assert(popup.window!!.flags has Wf.Popup)
            if (popup.window!!.flags has Wf.ChildWindow) {
                n++
                continue
            }
            // Trim the stack if popups are not direct descendant of the reference window (which is often the NavWindow)
            var hasFocus = false
            var m = n
            while (m < g.openPopupStack.size && !hasFocus) {
                hasFocus = g.openPopupStack[m].window != null && g.openPopupStack[m].window!!.rootWindow === refWindow.rootWindow
                m++
            }
            if (!hasFocus) break
            n++
        }

    if (n < g.openPopupStack.size)   // This test is not required but it allows to set a convenient breakpoint on the block below
        closePopupToLevel(n)
}

fun closePopupToLevel(remaining: Int) {
    if (remaining > 0)
        g.openPopupStack[remaining - 1].window.focus()
    else
        g.openPopupStack[0].parentWindow.focus()
    for (i in remaining until g.openPopupStack.size) g.openPopupStack.pop()  // resize(remaining)
}

val frontMostModalRootWindow: Window?
    get() {
        for (n in g.openPopupStack.size - 1 downTo 0)
            g.openPopupStack[n].window?.let { if (it.flags has Wf.Modal) return it }
        return null
    }

fun findBestPopupWindowPos(basePos: Vec2, window: Window, rInner: Rect): Vec2 {

    val size = window.size  // safe

    /*  Clamp into visible area while not overlapping the cursor. Safety padding is optional if our popup size won't fit
        without it. */
    val safePadding = style.displaySafeAreaPadding
    val rOuter = Rect(getVisibleRect())
    rOuter.expand(Vec2(if (size.x - rOuter.width > safePadding.x * 2) -safePadding.x else 0f,
            if (size.y - rOuter.height > safePadding.y * 2) -safePadding.y else 0f))
    val basePosClamped = glm.clamp(basePos, rOuter.min, rOuter.max - size)

    var n = if (window.autoPosLastDirection != -1) -1 else 0
    while (n < 4)   // Last, Right, down, up, left. (Favor last used direction).
    {
        val dir = if (n == -1) window.autoPosLastDirection else n
        n++
        val rect = Rect(
                if (dir == 0) rInner.max.x else rOuter.min.x, if (dir == 1) rInner.max.y else rOuter.min.y,
                if (dir == 3) rInner.min.x else rOuter.max.x, if (dir == 2) rInner.min.y else rOuter.max.y)
        if (rect.width < size.x || rect.height < size.y) continue
        window.autoPosLastDirection = dir
        return Vec2(if (dir == 0) rInner.max.x else if (dir == 3) rInner.min.x - size.x else basePosClamped.x,
                if (dir == 1) rInner.max.y else if (dir == 2) rInner.min.y - size.y else basePosClamped.y)
    }

    // Fallback, try to keep within display
    window.autoPosLastDirection = -1
    return Vec2(basePos).also {
        it.x = glm.max(glm.min(it.x + size.x, rOuter.max.x) - size.x, rOuter.min.x)
        it.y = glm.max(glm.min(it.y + size.y, rOuter.max.y) - size.y, rOuter.min.y)
    }
}

/** Return false to discard a character.    */
fun inputTextFilterCharacter(pChar: IntArray, flags: Int/*, ImGuiTextEditCallback callback, void* user_data*/): Boolean {

    var c = pChar[0].c

    if (c < 128 && c != ' ' && !c.isPrintable) {
        var pass = false
        pass = pass or (c == '\n' && flags has Itf.Multiline)
        pass = pass or (c == '\t' && flags has Itf.AllowTabInput)
        if (!pass) return false
    }

    /*  Filter private Unicode range. I don't imagine anybody would want to input them. GLFW on OSX seems to send
        private characters for special keys like arrow keys.     */
    if (c >= 0xE000 && c <= 0xF8FF) return false

    if (flags has (Itf.CharsDecimal or Itf.CharsHexadecimal or Itf.CharsUppercase or Itf.CharsNoBlank)) {

        if (flags has Itf.CharsDecimal)
            if (!(c in '0'..'9') && (c != '.') && (c != '-') && (c != '+') && (c != '*') && (c != '/')) return false

        if (flags has Itf.CharsHexadecimal)
            if (!(c in '0'..'9') && !(c in 'a'..'f') && !(c in 'A'..'F'))
                return false

        if (flags has Itf.CharsUppercase && c in 'a'..'z') {
            c += 'A' - 'a'
            pChar[0] = c.i
        }

        if (flags has Itf.CharsNoBlank && c.isSpace) return false
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

fun inputTextCalcTextSizeW(text: String, textEnd: Int, remaining: IntArray? = null, outOffset: Vec2? = null,
                           stopOnNewLine: Boolean = false): Vec2 {

    val font = g.font
    val lineHeight = g.fontSize
    val scale = lineHeight / font.fontSize

    val textSize = Vec2()
    var lineWidth = 0f

    var s = 0
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

        val charWidth: Float = font.getCharAdvance(c) * scale  //TODO rename back
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

    remaining?.set(0, s)

    return textSize
}

fun IntArray.format(dataType: DataType, displayFormat: String, buf: CharArray): CharArray {
    val value: Number = when (dataType) {
        DataType.Int -> this[0]
        DataType.Float -> glm.intBitsToFloat(this[0])
        else -> throw Error()
    }
    return displayFormat.format(style.locale, value).toCharArray(buf)
}

/** JVM Imgui, dataTypeFormatString replacement */
fun IntArray.format(dataType: DataType, decimalPrecision: Int, buf: CharArray) = when (dataType) {

    DataType.Int -> "%${if (decimalPrecision < 0) "" else ".$decimalPrecision"}d".format(style.locale, this[0])
/*  Ideally we'd have a minimum decimal precision of 1 to visually denote that it is a float, while hiding
    non-significant digits?         */
    DataType.Float -> "%${if (decimalPrecision < 0) "" else ".$decimalPrecision"}f".format(style.locale, glm.intBitsToFloat(this[0]))
    else -> throw Error("unsupported format data type")
}.toCharArray(buf)

fun dataTypeApplyOp(dataType: DataType, op: Char, pA: IntArray, B: Number) = when (dataType) {
    DataType.Int -> pA[0] = when (op) {
        '+' -> pA[0] + B as Int
        '-' -> pA[0] - B as Int
        else -> throw Error()
    }
    DataType.Float -> pA[0] = glm.floatBitsToInt(when (op) {
        '+' -> glm.intBitsToFloat(pA[0]) + B as Float
        '-' -> glm.intBitsToFloat(pA[0]) - B as Float
        else -> throw Error()
    })
    else -> throw Error()
}

/** User can input math operators (e.g. +100) to edit a numerical values.   */
fun dataTypeApplyOpFromText(buf: CharArray, initialValueBuf: CharArray, dataType: DataType, data: IntArray, scalarFormat: String? = null)
        : Boolean {

//    var s = 0
//    while (buf[s].isSpace) s++
//
//    /*  We don't support '-' op because it would conflict with inputing negative value.
//        Instead you can use +-100 to subtract from an existing value     */
//    var op = buf[s]
//    if (op == '+' || op == '*' || op == '/') {
//        s++
//        while (buf[s].isSpace) s++
//    } else
//        op = 0.c
//
//    if (buf[s] == 0.c) return false

    val seq = String(buf).replace("\\s+", "").split(Regex("-+\\*/"))
    return when (dataType) {

        DataType.Int -> {
            val scalarFormat = scalarFormat ?: "%d"
            var v = data[0]
            val oldV = v
            val a = seq[0].i

            if (seq.size == 2) {   // TODO support more complex operations? i.e: a + b * c

                val op = seq[1][0]
                /*  Store operand b in a float so we can use fractional value for multipliers (*1.1), but constant
                    always parsed as integer so we can fit big integers (e.g. 2000000003) past float precision  */
                val b = seq[2].f
                when (op) {
                    '+' -> v = (a + b).i    // Add (use "+-" to subtract)
                    '*' -> v = (a * b).i    // Multiply
                    '/' -> v = (a / b).i    // Divide   TODO / 0 will throw
                    else -> throw Error()
                }
            } else
                v = a   // Assign constant

            data[0] = v
            oldV != v
        }
        DataType.Float -> {
            // For floats we have to ignore format with precision (e.g. "%.2f") because sscanf doesn't take them in TODO not true in java
            val scalarFormat = scalarFormat ?: "%f"
            var v = glm.intBitsToFloat(data[0])
            val oldV = v
            val a = seq[0].f

            if (seq.size == 2) {   // TODO support more complex operations? i.e: a + b * c

                val op = seq[1][0]
                val b = seq[2].f
                when (op) {
                    '+' -> v = a + b    // Add (use "+-" to subtract)
                    '*' -> v = a * b    // Multiply
                    '/' -> v = a / b    // Divide   TODO / 0 will throw
                    else -> throw Error()
                }
            } else
                v = a   // Assign constant

            data[0] = glm.floatBitsToInt(v)
            oldV != v
        }
        else -> false
    }
}

//-----------------------------------------------------------------------------
// Platform dependent default implementations
//-----------------------------------------------------------------------------

//static const char*      GetClipboardTextFn_DefaultImpl(void* user_data);
//static void             SetClipboardTextFn_DefaultImpl(void* user_data, const char* text);
//static void             ImeSetInputScreenPosFn_DefaultImpl(int x, int y);