package imgui.static

import glm_.*
import glm_.vec2.Vec2
import imgui.*
import imgui.api.g
import imgui.classes.Context
import imgui.classes.InputTextCallbackData
import imgui.internal.classes.InputTextState
import imgui.internal.classes.PtrOrIndex
import imgui.internal.classes.TabBar
import imgui.internal.classes.TabItem
import imgui.internal.isBlankW
import imgui.internal.sections.InputSource
import imgui.internal.textCountCharsFromUtf8
import imgui.internal.textStrFromUtf8
import uno.kotlin.NUL
import uno.kotlin.getValue
import uno.kotlin.isPrintable
import uno.kotlin.setValue
import kotlin.math.pow
import kotlin.reflect.KMutableProperty0

/** Time for drag-hold to activate items accepting the ImGuiButtonFlags_PressedOnDragDropHold button behavior. */
val DRAGDROP_HOLD_TO_OPEN_TIMER = 0.7f

/** Multiplier for the default value of io.MouseDragThreshold to make DragFloat/DragInt react faster to mouse drags. */
val DRAG_MOUSE_THRESHOLD_FACTOR = 0.5f

val minSteps = floatArrayOf(1f, 0.1f, 0.01f, 0.001f, 0.0001f, 0.00001f, 0.000001f, 0.0000001f, 0.00000001f, 0.000000001f)

fun getMinimumStepAtDecimalPrecision(decimalPrecision: Int): Float {
    return when {
        decimalPrecision < 0 -> Float.MIN_VALUE
        else -> minSteps.getOrElse(decimalPrecision) {
            10f.pow(-decimalPrecision.f)
        }
    }
}

/** Return false to discard a character.    */
fun inputTextFilterCharacter(char: KMutableProperty0<Char>, flags: InputTextFlags, callback: InputTextCallback?,
                             userData: Any?, inputSource: InputSource): Boolean {

    assert(inputSource == InputSource.Keyboard || inputSource == InputSource.Clipboard)
    var c by char

    // Filter non-printable (NB: isprint is unreliable! see #2467) [JVM we can rely on custom ::isPrintable]
    var applyNamedFilters = true
    if (c < 0x20 && !c.isPrintable) {
        var pass = false
        pass = pass or (c == '\n' && flags has InputTextFlag._Multiline) // Note that an Enter KEY will emit \r and be ignored (we poll for KEY in InputText() code)
        pass = pass or (c == '\t' && flags has InputTextFlag.AllowTabInput)
        if (!pass)
            return false
        applyNamedFilters = false // Override named filters below so newline and tabs can still be inserted.
    }

    if (inputSource != InputSource.Clipboard) {
        // We ignore Ascii representation of delete (emitted from Backspace on OSX, see #2578, #2817)
        if (c.i == 127)
            return false

        // Filter private Unicode range. GLFW on OSX seems to send private characters for special keys like arrow keys (FIXME)
        if (c >= 0xE000 && c <= 0xF8FF)
            return false
    }

    // Filter Unicode ranges we are not handling in this build
    if (c > UNICODE_CODEPOINT_MAX)
        return false

    // Generic named filters
    if (applyNamedFilters && flags has (InputTextFlag.CharsDecimal or InputTextFlag.CharsHexadecimal or InputTextFlag.CharsUppercase or InputTextFlag.CharsNoBlank or InputTextFlag.CharsScientific)) {

        // The libc allows overriding locale, with e.g. 'setlocale(LC_NUMERIC, "de_DE.UTF-8");' which affect the output/input of printf/scanf to use e.g. ',' instead of '.'.
        // The standard mandate that programs starts in the "C" locale where the decimal point is '.'.
        // We don't really intend to provide widespread support for it, but out of empathy for people stuck with using odd API, we support the bare minimum aka overriding the decimal point.
        // Change the default decimal_point with:
        //   ImGui::GetCurrentContext()->PlatformLocaleDecimalPoint = *localeconv()->decimal_point;
        // Users of non-default decimal point (in particular ',') may be affected by word-selection logic (is_word_boundary_from_right/is_word_boundary_from_left) functions.
        val cDecimalPoint = g.platformLocaleDecimalPoint

        // Full-width -> half-width conversion for numeric fields (https://en.wikipedia.org/wiki/Halfwidth_and_Fullwidth_Forms_(Unicode_block)
        // While this is mostly convenient, this has the side-effect for uninformed users accidentally inputting full-width characters that they may
        // scratch their head as to why it works in numerical fields vs in generic text fields it would require support in the font.
        if (flags has (InputTextFlag.CharsDecimal or InputTextFlag.CharsScientific or InputTextFlag.CharsHexadecimal))
            if (c >= 0xFF01 && c <= 0xFF5E)
                c = c - 0xFF01 + 0x21

        // Allow 0-9 . - + * /
        if (flags has InputTextFlag.CharsDecimal)
            if (c !in '0'..'9' && c != cDecimalPoint && c != '-' && c != '+' && c != '*' && c != '/')
                return false

        // Allow 0-9 . - + * / e E
        if (flags has InputTextFlag.CharsScientific)
            if (c !in '0'..'9' && c != cDecimalPoint && c != '-' && c != '+' && c != '*' && c != '/' && c != 'e' && c != 'E')
                return false

        // Allow 0-9 a-F A-F
        if (flags has InputTextFlag.CharsHexadecimal)
            if (c !in '0'..'9' && c !in 'a'..'f' && c !in 'A'..'F')
                return false

        // Turn a-z into A-Z
        if (flags has InputTextFlag.CharsUppercase && c in 'a'..'z')
            c += ('A' - 'a')

        if (flags has InputTextFlag.CharsNoBlank && c.isBlankW)
            return false
    }

    // Custom callback filter
    if (flags has InputTextFlag.CallbackCharFilter) {
        callback!! //callback is non-null from all calling functions
        val itcd = InputTextCallbackData(g)
        itcd.eventFlag = InputTextFlag.CallbackCharFilter
        itcd.eventChar = c
        itcd.flags = flags
        itcd.userData = userData

        if (callback(itcd))
            return false
        if (itcd.eventChar == NUL)
            return false
    }
    return true
}

/** @return [JVM] [lineCount, textEnd] */
fun inputTextCalcTextLenAndLineCount(text: ByteArray): Pair<Int, Int> {
    if (text.isEmpty()) return 1 to 0
    var lineCount = 0
    var s = 0
    var c = text[s++]
    while (c != 0.b) { // We are only matching for \n so we can ignore UTF-8 decoding
        if (c == '\n'.b)
            lineCount++
        c = text[s++]
    }
    s--
    if (text[s] != '\n'.b && text[s] != '\r'.b)
        lineCount++
    return lineCount to s
}

fun inputTextCalcTextSizeW(ctx: Context, text: CharArray, textBegin: Int, textEnd: Int, remaining: KMutableProperty0<Int>? = null,
                           outOffset: Vec2? = null, stopOnNewLine: Boolean = false): Vec2 {

    val g = ctx
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

// Find the shortest single replacement we can make to get the new text from the old text.
// Important: needs to be run before TextW is rewritten with the new characters because calling STB_TEXTEDIT_GETCHAR() at the end.
// FIXME: Ideally we should transition toward (1) making InsertChars()/DeleteChars() update undo-stack (2) discourage (and keep reconcile) or obsolete (and remove reconcile) accessing buffer directly.
fun inputTextReconcileUndoStateAfterUserCallback(state: InputTextState, newBufA: ByteArray, newLengthA: Int) {
    // Find the shortest single replacement we can make to get the new text
    // from the old text.
    val oldBuf = state.textW
    val oldLength = state.curLenW
    val newLength = textCountCharsFromUtf8(newBufA, newLengthA)
    val newBuf = CharArray(newLength)
    textStrFromUtf8(newBuf, newBufA, newLengthA)

    val shorterLength = oldLength min newLength
    var firstDiff = 0
    while (firstDiff < shorterLength)
        if (oldBuf[firstDiff] != newBuf[firstDiff++])
            break
    if (firstDiff == oldLength && firstDiff == newLength)
        return

    var oldLastDiff = oldLength - 1
    var newLastDiff = newLength - 1
    while (oldLastDiff >= firstDiff && newLastDiff >= firstDiff) {
        if (oldBuf[oldLastDiff] != newBuf[newLastDiff])
            break
        oldLastDiff--; newLastDiff--
    }

    val insertLen = newLastDiff - firstDiff + 1
    val deleteLen = oldLastDiff - firstDiff + 1
    if (insertLen > 0 || deleteLen > 0)
        state.stb.undoState.createUndo(firstDiff, deleteLen, insertLen)?.let { p ->
            for (i in 0 until deleteLen)
                state.stb.undoState.undoChar[p + i] = state getChar (firstDiff + i)
        }
}


fun tabItemGetSectionIdx(tab: TabItem) = when {
    tab.flags has TabItemFlag.Leading -> 0
    tab.flags has TabItemFlag.Trailing -> 2
    else -> 1
}

val tabItemComparerBySection = Comparator<TabItem> { a, b ->
    val aSection = tabItemGetSectionIdx(a)
    val bSection = tabItemGetSectionIdx(b)
    if (aSection != bSection)
        aSection - bSection
    else
        a.indexDuringLayout - b.indexDuringLayout
}

// [JVM] too much verbose, we can be much more concise without it
//static int IMGUI_CDECL TabItemComparerByBeginOrder(const void* lhs, const void* rhs)
//{
//    const ImGuiTabItem* a = (const ImGuiTabItem*)lhs;
//    const ImGuiTabItem* b = (const ImGuiTabItem*)rhs;
//    return (int)(a->BeginOrder - b->BeginOrder);
//}

// ~GetTabBarRefFromTabBar
val TabBar.tabBarRef: PtrOrIndex
    get() = when (this) {
        in g.tabBars -> PtrOrIndex(g.tabBars.getIndex(this))
        else -> PtrOrIndex(this)
    }