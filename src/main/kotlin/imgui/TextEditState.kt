package imgui

import gli_.has
import glm_.c
import glm_.glm
import imgui.internal.isBlankW

/** Internal state of the currently focused/edited text input box   */
class TextEditState {

    /** widget id owning the text state */
    var id: ID = 0
    /** edit buffer, we need to persist but can't guarantee the persistence of the user-provided buffer. so we copy
    into own buffer.    */
    var text = charArrayOf()
    /** backup of end-user buffer at the time of focus (in UTF-8, unaltered)    */
    var initialText = charArrayOf()

    var tempTextBuffer = charArrayOf()
    /** we need to maintain our buffer length in both UTF-8 and wchar format.   */
    var curLenA = 0

    var curLenW = 0
    /** end-user buffer size    */
    var bufSizeA = 0

    var scrollX = 0f

    val state = State()

    var cursorAnim = 0f

    var cursorFollow = false

    var selectedAllMouseLock = false

    /** After a user-input the cursor stays on for a while without blinking */
    fun cursorAnimReset() {
        cursorAnim = -0.3f
    }

    fun cursorClamp() = with(state) {
        cursor = glm.min(cursor, curLenW)
        selectStart = glm.min(selectStart, curLenW)
        selectEnd = glm.min(selectEnd, curLenW)
    }

    fun clearSelection() {
        state.selectStart = state.cursor
        state.selectEnd = state.cursor
    }

    fun selectAll() {
        state.selectStart = 0
        state.selectEnd = curLenW
        state.cursor = curLenW
        state.hasPreferredX = false
    }

    fun onKeyPressed(key: Int) {
        key(key)
        cursorFollow = true
        cursorAnimReset()
    }


    /*
    ====================================================================================================================
        [imgui.cpp] Wrapper for stb_textedit.h to edit text (our wrapper is for: statically sized buffer, single-line,
        wchar characters. InputText converts between UTF-8 and wchar)
    ====================================================================================================================
    */

    val stringLen get() = curLenW

    fun getChar(idx: Int) = text[idx]
    fun getWidth(lineStartIdx: Int, charIdx: Int): Float {
        val c = text[lineStartIdx + charIdx]
        return if (c == '\n') -1f else g.font.getCharAdvance_ssaaaaaaaaaaaaa(c) * (g.fontSize / g.font.fontSize)
    }

    fun keyToText(key: Int) = if (key >= 0x10000) 0 else key
    val newLine get() = '\n'

    var textRemaining = 0

    fun layout(r: Row, lineStartIdx: Int) {

        val size = inputTextCalcTextSizeW(text, lineStartIdx, curLenW, ::textRemaining, null, true)
        with(r) {
            r.x0 = 0f
            r.x1 = size.x
            r.baselineYDelta = size.y
            r.yMin = 0f
            r.yMax = size.y
            r.numChars = textRemaining - lineStartIdx
        }
    }

    val Char.isSeparator
        get() = isBlankW || this == ',' || this == ';' || this == '(' || this == ')' ||
                this == '{' || this == '}' || this == '[' || this == ']' || this == '|'

    fun isWordBoundaryFromRight(idx: Int) = if (idx > 0) text[idx - 1].isSeparator && !text[idx].isSeparator else true

    fun moveWordLeft(idx_: Int): Int {
        var idx = idx_ - 1
        while (idx >= 0 && !isWordBoundaryFromRight(idx)) idx--
        return if (idx < 0) 0 else idx
    }

    fun moveWordRight(idx_: Int): Int {
        var idx = idx_ + 1
        val len = curLenW
        while (idx < len && !isWordBoundaryFromRight(idx)) idx++
        return if (idx > len) len else idx
    }

    fun deleteChars(pos: Int, n: Int) {

        var dst = pos

        // We maintain our buffer length in both UTF-8 and wchar formats
        curLenA -= n //TODO check textCountUtf8BytesFromStr(dst, dst + n)
        curLenW -= n

        // Offset remaining text
        for (c in pos + n until text.size)
            text[dst++] = text[c]
        text[dst] = NUL
    }

    fun insertChars(pos: Int, newText: CharArray, ptr: Int, newTextLen: Int): Boolean {

        val textLen = curLenW
        assert(pos <= textLen)
        if (newTextLen + textLen + 1 > text.size) return false

        val newTextLenUtf8 = newTextLen //TODO check textCountUtf8BytesFromStr(new_text, new_text + newTextLen)
        if (newTextLenUtf8 + curLenA > bufSizeA) return false

        if (pos != textLen)
            for (i in 0 until textLen - pos) text[textLen - 1 + newTextLen - i] = text[textLen - 1 - i]
        for (i in 0 until newTextLen) text[pos + i] = newText[ptr + i]

        curLenW += newTextLen
        curLenA += newTextLenUtf8
        text[curLenW] = NUL

        return true
    }

    /*  We don't use an enum so we can build even with conflicting symbols (if another user of stb_textedit.h leak their 
        STB_TEXTEDIT_K_* symbols)     */
    object K {
        /** keyboard input to move cursor left  */
        val LEFT = 0x10000
        /** keyboard input to move cursor right */
        val RIGHT = 0x10001
        /** keyboard input to move cursor up    */
        val UP = 0x10002
        /** keyboard input to move cursor down  */
        val DOWN = 0x10003
        /** keyboard input to move cursor to start of line  */
        val LINESTART = 0x10004
        /** keyboard input to move cursor to end of line    */
        val LINEEND = 0x10005
        /** keyboard input to move cursor to start of text  */
        val TEXTSTART = 0x10006
        /** keyboard input to move cursor to end of text    */
        val TEXTEND = 0x10007
        /** keyboard input to delete selection or character under cursor    */
        val DELETE = 0x10008
        /** keyboard input to delete selection or character left of cursor  */
        val BACKSPACE = 0x10009
        /** keyboard input to perform undo  */
        val UNDO = 0x1000A
        /** keyboard input to perform redo  */
        val REDO = 0x1000B
        /** keyboard input to move cursor left one word */
        val WORDLEFT = 0x1000C
        /** keyboard input to move cursor right one word    */
        val WORDRIGHT = 0x1000D

        val SHIFT = 0x20000
    }

    /*
    ====================================================================================================================
    stb_textedit.h - v1.9  - public domain - Sean Barrett
    Development of this library was sponsored by RAD Game Tools
    ====================================================================================================================
    */

    companion object {
        val UNDOSTATECOUNT = 99
        val UNDOCHARCOUNT = 999
        fun memmove(dst: CharArray, pDst: Int, src: CharArray, pSrc: Int, len: Int) {
            val tmp = CharArray(len, { src[pSrc + it] })
            for (i in 0 until len) dst[pDst + i] = tmp[i]
        }

        fun memmove(dst: Array<UndoRecord>, pDst: Int, src: Array<UndoRecord>, pSrc: Int, len: Int) {
            val tmp = Array(len, { UndoRecord(src[pSrc + it]) })
            for (i in 0 until len) dst[pDst + i] = tmp[i]
        }

        val GETWIDTH_NEWLINE = -1f
    }

    class UndoRecord {
        var where = 0
        var insertLength = 0
        var deleteLength = 0
        var charStorage = 0

        constructor()
        constructor(undoRecord: UndoRecord) {
            where = undoRecord.where
            insertLength = undoRecord.insertLength
            deleteLength = undoRecord.deleteLength
            charStorage = undoRecord.charStorage
        }

        infix fun put(other: UndoRecord) {
            where = other.where
            insertLength = other.insertLength
            deleteLength = other.deleteLength
            charStorage = other.charStorage
        }
    }

    class UndoState {
        val undoRec = Array(UNDOSTATECOUNT, { UndoRecord() })
        val undoChar = CharArray(UNDOCHARCOUNT)
        var undoPoint = 0
        var redoPoint = 0
        var undoCharPoint = 0
        var redoCharPoint = 0

        fun clear() {
            undoPoint = 0
            undoCharPoint = 0
            redoPoint = UNDOSTATECOUNT
            redoCharPoint = UNDOCHARCOUNT
        }

        /////////////////////////////////////////////////////////////////////////////
        //
        //      Undo processing
        //
        // @OPTIMIZE: the undo/redo buffer should be circular
        //
        /////////////////////////////////////////////////////////////////////////////

        fun flushRedo() {
            redoPoint = UNDOSTATECOUNT
            redoCharPoint = UNDOCHARCOUNT
        }

        /** discard the oldest entry in the undo list   */
        fun discardUndo() {
            if (undoPoint > 0) {
                // if the 0th undo state has characters, clean those up
                if (undoRec[0].charStorage >= 0) {
                    val n = undoRec[0].insertLength
                    // delete n characters from all other records
                    undoCharPoint -= n  // vsnet05
                    memmove(undoChar, 0, undoChar, n, undoCharPoint)
                    for (i in 0 until undoPoint)
                        if (undoRec[i].charStorage >= 0)
                        // vsnet05 // @OPTIMIZE: get rid of char_storage and infer it
                            undoRec[i].charStorage = undoRec[i].charStorage - n
                }
                --undoPoint
                memmove(undoRec, 0, undoRec, 1, undoPoint)
            }
        }

        /** discard the oldest entry in the redo list--it's bad if this ever happens, but because undo & redo have to
         *  store the actual characters in different cases, the redo character buffer can fill up even though the undo
         *  buffer didn't   */
        fun discardRedo() {

            val k = UNDOSTATECOUNT - 1

            if (redoPoint <= k) {
                // if the k'th undo state has characters, clean those up
                if (undoRec[k].charStorage >= 0) {
                    val n = undoRec[k].insertLength
                    // delete n characters from all other records
                    redoCharPoint += n // vsnet05
                    memmove(undoChar, redoCharPoint, undoChar, redoCharPoint - n, UNDOCHARCOUNT - redoCharPoint)
                    for (i in redoPoint until k)
                        if (undoRec[i].charStorage >= 0)
                            undoRec[i].charStorage = undoRec[i].charStorage + n // vsnet05
                }
                memmove(undoRec, redoPoint, undoRec, redoPoint - 1, UNDOSTATECOUNT - redoPoint)
                ++redoPoint
            }
        }

        fun createUndoRecord(numChars: Int): UndoRecord? {

            // any time we create a new undo record, we discard redo
            flushRedo()

            // if we have no free records, we have to make room, by sliding the existing records down
            if (undoPoint == UNDOSTATECOUNT)
                discardUndo()

            // if the characters to store won't possibly fit in the buffer, we can't undo
            if (numChars > UNDOCHARCOUNT) {
                undoPoint = 0
                undoCharPoint = 0
                return null
            }

            // if we don't have enough free characters in the buffer, we have to make room
            while (undoCharPoint + numChars > UNDOCHARCOUNT)
                discardUndo()

            return undoRec[undoPoint++]
        }

        fun createundo(pos: Int, insertLen: Int, deleteLen: Int): Int? {

            val r = createUndoRecord(insertLen) ?: return null

            r.where = pos
            r.insertLength = insertLen
            r.deleteLength = deleteLen

            if (insertLen == 0) {
                r.charStorage = -1
                return null
            } else {
                r.charStorage = undoCharPoint
                undoCharPoint += insertLen
                return r.charStorage
            }
        }
    }

    class State {

        /** position of the text cursor within the string   */
        var cursor = 0

        /*  selection start and end point in characters; if equal, no selection.
            note that start may be less than or greater than end (e.g. when dragging the mouse, start is where the
            initial click was, and you can drag in either direction)    */

        /** selection start point   */
        var selectStart = 0

        /** selection end point   */
        var selectEnd = 0

        /** each textfield keeps its own insert mode state. to keep an app-wide insert mode, copy this value in/out of
         *  the app state   */
        var insertMode = false

        /** not implemented yet */
        var cursorAtEndOfLine = false
        var initialized = false
        var hasPreferredX = false
        var singleLine = false
        var padding1 = NUL
        var padding2 = NUL
        var padding3 = NUL
        /** this determines where the cursor up/down tries to seek to along x   */
        var preferredX = 0f
        val undostate = UndoState()


        /** reset the state to default  */
        fun clear(isSingleLine: Boolean) {

            undostate.clear()
            selectStart = 0
            selectEnd = 0
            cursor = 0
            hasPreferredX = false
            preferredX = 0f
            cursorAtEndOfLine = false
            initialized = true
            singleLine = isSingleLine
            insertMode = false
        }
    }

    /** Result of layout query, used by stb_textedit to determine where the text in each row is.
     *  result of layout query  */
    class Row {
        /** starting x location */
        var x0 = 0f
        /** end x location (allows for align=right, etc)    */
        var x1 = 0f
        /** position of baseline relative to previous row's baseline    */
        var baselineYDelta = 0f
        /** height of row above baseline    */
        var yMin = 0f
        /** height of row below baseline   */
        var yMax = 0f

        var numChars = 0
    }

    /////////////////////////////////////////////////////////////////////////////
    //
    //      Mouse input handling
    //
    /////////////////////////////////////////////////////////////////////////////

    /** traverse the layout to locate the nearest character to a display position   */
    fun locateCoord(x: Float, y: Float): Int {

        val r = Row()
        val n = curLenW
        var baseY = 0f
        var prevX = 0f
        var i = 0

        // search rows to find one that straddles 'y'
        while (i < n) {
            layout(r, i)
            if (r.numChars <= 0)
                return n

            if (i == 0 && y < baseY + r.yMin)
                return 0

            if (y < baseY + r.yMax)
                break

            i += r.numChars
            baseY += r.baselineYDelta
        }

        // below all text, return 'after' last character
        if (i >= n)
            return n

        // check if it's before the beginning of the line
        if (x < r.x0)
            return i

        // check if it's before the end of the line
        if (x < r.x1) {
            // search characters in row for one that straddles 'x'
            prevX = r.x0
            for (k in 0 until r.numChars) {
                val w = getWidth(i, k)
                if (x < prevX + w) {
                    return if (x < prevX + w / 2) k + i else k + i + 1
                }
                prevX += w
            }
            // shouldn't happen, but if it does, fall through to end-of-line case
        }

        // if the last character is a newline, return that. otherwise return 'after' the last character
        return if (text[i + r.numChars - 1] == '\n') i + r.numChars - 1 else i + r.numChars
    }

    /** API click: on mouse down, move the cursor to the clicked location, and reset the selection  */
    fun click(x: Float, y: Float) = with(state) {
        cursor = locateCoord(x, y)
        selectStart = cursor
        selectEnd = cursor
        hasPreferredX = false
    }

    /** API drag: on mouse drag, move the cursor and selection endpoint to the clicked location */
    fun drag(x: Float, y: Float) {
        val p = locateCoord(x, y)
        if (state.selectStart == state.selectEnd)
            state.selectStart = state.cursor
        state.cursor = p
        state.selectEnd = p
    }

    /////////////////////////////////////////////////////////////////////////////
    //
    //      Keyboard input handling
    //
    /////////////////////////////////////////////////////////////////////////////

    fun undo() {

        val s = state.undostate
        if (s.undoPoint == 0) return

        // we need to do two things: apply the undo record, and create a redo record
        val u = UndoRecord(s.undoRec[s.undoPoint - 1])
        var r = s.undoRec[s.redoPoint - 1]
        r put u
        r.charStorage = -1

        if (u.deleteLength != 0) {
            /*  if the undo record says to delete characters, then the redo record will need to re-insert the characters
                that get deleted, so we need to store them.

                there are three cases:
                    - there's enough room to store the characters
                    - characters stored for *redoing* don't leave room for redo
                    - characters stored for *undoing* don't leave room for redo
                if the last is true, we have to bail    */

            if (s.undoCharPoint + u.deleteLength >= UNDOCHARCOUNT)
            //  the undo records take up too much character space; there's no space to store the redo characters
                r.insertLength = 0
            else {
                // there's definitely room to store the characters eventually
                while (s.undoCharPoint + u.deleteLength > s.redoCharPoint) {
                    // there's currently not enough room, so discard a redo record
                    s.discardRedo()
                    // should never happen:
                    if (s.redoPoint == UNDOSTATECOUNT)
                        return
                }
                r = s.undoRec[s.redoPoint - 1]

                r.charStorage = s.redoCharPoint - u.deleteLength
                s.redoCharPoint = s.redoCharPoint - u.deleteLength

                // now save the characters
                repeat(u.deleteLength) { s.undoChar[r.charStorage + it] = getChar(u.where + it) }
            }

            // now we can carry out the deletion
            deleteChars(u.where, u.deleteLength)
        }

        // check type of recorded action:
        if (u.insertLength != 0) {
            // easy case: was a deletion, so we need to insert n characters
            insertChars(u.where, s.undoChar, u.charStorage, u.insertLength)
            s.undoCharPoint -= u.insertLength
        }

        state.cursor = u.where + u.insertLength

        s.undoPoint--
        s.redoPoint--
    }

    fun redo() {

        val s = state.undostate
        if (s.redoPoint == UNDOSTATECOUNT) return

        // we need to do two things: apply the redo record, and create an undo record
        val u = s.undoRec[s.undoPoint]
        val r = UndoRecord(s.undoRec[s.redoPoint])

        // we KNOW there must be room for the undo record, because the redo record was derived from an undo record

        u put r
        u.charStorage = -1

        if (r.deleteLength != 0) {
            // the redo record requires us to delete characters, so the undo record needs to store the characters

            if (s.undoCharPoint + u.insertLength > s.redoCharPoint) {
                u.insertLength = 0
                u.deleteLength = 0
            } else {
                u.charStorage = s.undoCharPoint
                s.undoCharPoint = s.undoCharPoint + u.insertLength

                // now save the characters
                for (i in 0 until u.insertLength)
                    s.undoChar[u.charStorage + i] = getChar(u.where + i)
            }

            deleteChars(r.where, r.deleteLength)
        }

        if (r.insertLength != 0) {
            // easy case: need to insert n characters
            insertChars(r.where, s.undoChar, r.charStorage, r.insertLength)
            s.redoCharPoint += r.insertLength
        }

        state.cursor = r.where + r.insertLength

        s.undoPoint++
        s.redoPoint++
    }

    fun makeundoInsert(where: Int, length: Int) = state.undostate.createundo(where, 0, length)

    fun makeundoDelete(where: Int, length: Int) = state.undostate.createundo(where, length, 0)?.let {
        for (i in 0 until length)
            state.undostate.undoChar[it + i] = getChar(where + i)
    }

    fun makeundoReplace(where: Int, oldLength: Int, newLength: Int) = state.undostate.createundo(where, oldLength, newLength)?.let {
        for (i in 0 until oldLength)
            state.undostate.undoChar[i] = getChar(where + i)
    }


    class FindState {
        // position of n'th character
        var x = 0f
        var y = 0f
        /** height of line   */
        var height = 0f
        /** first char of row   */
        var firstChar = 0
        /** first char length   */
        var length = 0
        /** first char of previous row  */
        var prevFirst = 0
    }

    val hasSelection get() = state.selectStart != state.selectEnd

    /** find the x/y location of a character, and remember info about the previous row in case we get a move-up event
     *  (for page up, we'll have to rescan) */
    fun findCharpos(find: FindState, n: Int, singleLine: Boolean) {
        val r = Row()
        var prevStart = 0
        val z = curLenW
        var i = 0
        var first = 0

        if (n == z) {
            // if it's at the end, then find the last line -- simpler than trying to
            // explicitly handle this case in the regular code
            if (singleLine) {
                layout(r, 0)
                with(find) {
                    y = 0f
                    firstChar = 0
                    length = z
                    height = r.yMax - r.yMin
                    x = r.x1
                }
            } else with(find) {
                y = 0f
                x = 0f
                height = 1f
                while (i < z) {
                    layout(r, i)
                    prevStart = i
                    i += r.numChars
                }
                firstChar = i
                length = 0
                prevFirst = prevStart
            }
            return
        }

        // search rows to find the one that straddles character n
        find.y = 0f

        while (true) {
            layout(r, i)
            if (n < i + r.numChars)
                break
            prevStart = i
            i += r.numChars
            find.y += r.baselineYDelta
        }

        with(find) {
            first = i
            firstChar = i
            length = r.numChars
            height = r.yMax - r.yMin
            prevFirst = prevStart

            // now scan to find xpos
            x = r.x0
            i = 0
            while (first + i < n) {
                x += getWidth(first, i)
                ++i
            }
        }
    }

    /** make the selection/cursor state valid if client altered the string  */
    fun clamp() {
        val n = stringLen
        with(state) {
            if (hasSelection) {
                if (selectStart > n) selectStart = n
                if (selectEnd > n) selectEnd = n
                // if clamping forced them to be equal, move the cursor to match
                if (selectStart == selectEnd)
                    cursor = selectStart
            }
            if (cursor > n) cursor = n
        }
    }

    /** delete characters while updating undo   */
    fun delete(where: Int, len: Int) {
        makeundoDelete(where, len)
        deleteChars(where, len)
        state.hasPreferredX = false
    }

    /** delete the section  */
    fun deleteSelection() {
        clamp()
        with(state) {
            if (hasSelection) {
                if (state.selectStart < state.selectEnd) {
                    delete(selectStart, selectEnd - selectStart)
                    cursor = selectStart
                    selectEnd = selectStart
                } else {
                    delete(selectEnd, selectStart - selectEnd)
                    cursor = selectEnd
                    selectStart = selectEnd
                }
                hasPreferredX = false
            }
        }
    }

    /** canoncialize the selection so start <= end  */
    fun sortSelection() = with(state) {
        if (selectEnd < selectStart) {
            val temp = selectEnd
            selectEnd = selectStart
            selectStart = temp
        }
    }

    /** move cursor to first character of selection */
    fun moveToFirst() = with(state) {
        if (hasSelection) {
            sortSelection()
            cursor = selectStart
            selectEnd = selectStart
            hasPreferredX = false
        }
    }

    /* move cursor to last character of selection   */
    fun moveToLast() = with(state) {
        if (hasSelection) {
            sortSelection()
            clamp()
            cursor = selectEnd
            selectStart = selectEnd
            hasPreferredX = false
        }
    }

    /** update selection and cursor to match each other */
    fun prepSelectionAtCursor() = with(state) {
        if (!hasSelection) {
            selectStart = cursor
            selectEnd = cursor
        } else
            cursor = selectEnd
    }

    /** API cut: delete selection   */
    fun cut() =
            if (hasSelection) {
                deleteSelection() // implicity clamps
                state.hasPreferredX = false
                true
            } else false

    /** API paste: replace existing selection with passed-in text   */
    fun paste(len: Int): Boolean {

        // if there's a selection, the paste should delete it
        clamp()
        deleteSelection()
        // try to insert the characters
        if (insertChars(state.cursor, text, 0, len)) {
            makeundoInsert(state.cursor, len)
            state.cursor += len
            state.hasPreferredX = true
            return true
        }
        // remove the undo since we didn't actually insert the characters
        if (state.undostate.undoPoint != 0)
            --state.undostate.undoPoint
        return false
    }

    /** API key: process a keyboard input   */
    fun key(key: Int): Unit = with(state) {
        when (key) {
            K.UNDO -> {
                undo()
                hasPreferredX = false
            }
            K.REDO -> {
                redo()
                hasPreferredX = false
            }
            K.LEFT -> {
                if (hasSelection)   // if currently there's a selection, move cursor to start of selection
                    moveToFirst()
                else if (cursor > 0)
                    --cursor
                hasPreferredX = false
            }
            K.RIGHT -> {
                if (hasSelection)   // if currently there's a selection, move cursor to end of selection
                    moveToLast()
                else
                    ++cursor
                clamp()
                hasPreferredX = false
            }
            K.LEFT or K.SHIFT -> {
                clamp()
                prepSelectionAtCursor()
                // move selection left
                if (selectEnd > 0)
                    --selectEnd
                cursor = selectEnd
                hasPreferredX = false
            }
            K.WORDLEFT ->
                if (hasSelection) moveToFirst()
                else {
                    cursor = moveWordLeft(cursor)
                    clamp()
                }
            K.WORDLEFT or K.SHIFT -> {
                if (!hasSelection)
                    prepSelectionAtCursor()
                cursor = moveWordLeft(cursor)
                selectEnd = cursor
                clamp()
            }
            K.WORDRIGHT ->
                if (hasSelection) moveToLast()
                else {
                    cursor = moveWordRight(cursor)
                    clamp()
                }
            K.WORDRIGHT or K.SHIFT -> {
                if (!hasSelection) prepSelectionAtCursor()
                cursor = moveWordRight(cursor)
                selectEnd = cursor
                clamp()
            }
            K.RIGHT or K.SHIFT -> {
                prepSelectionAtCursor()
                ++selectEnd   // move selection right
                clamp()
                cursor = selectEnd
                hasPreferredX = false
            }
            K.DOWN, K.DOWN or K.SHIFT -> {
                val find = FindState()
                val row = Row()
                val sel = key has K.SHIFT

                if (singleLine)
                    key(K.RIGHT or (key and K.SHIFT))   // on windows, up&down in single-line behave like left&right

                if (sel) prepSelectionAtCursor()
                else if (hasSelection) moveToLast()

                // compute current position of cursor point
                clamp()
                findCharpos(find, cursor, singleLine)

                // now find character position down a row
                if (find.length != 0) {
                    val goalX = if (hasPreferredX) preferredX else find.x
                    var x = 0f
                    val start = find.firstChar + find.length
                    cursor = start
                    layout(row, cursor)
                    x = row.x0
                    for (i in 0 until row.numChars) {
                        val dx = getWidth(start, i)
                        if (dx == GETWIDTH_NEWLINE)
                            break
                        x += dx
                        if (x > goalX)
                            break
                        ++cursor
                    }
                    clamp()

                    hasPreferredX = true
                    preferredX = goalX

                    if (sel) selectEnd = cursor
                }
                Unit
            }
            K.UP, K.UP or K.SHIFT -> {
                val find = FindState()
                val row = Row()
                var i = 0
                val sel = key has K.SHIFT

                if (singleLine)
                    key(K.LEFT or (key and K.SHIFT))    // on windows, up&down become left&right

                if (sel) prepSelectionAtCursor()
                else if (hasSelection) moveToFirst()

                // compute current position of cursor point
                clamp()
                findCharpos(find, cursor, singleLine)

                // can only go up if there's a previous row
                if (find.prevFirst != find.firstChar) {
                    // now find character position up a row
                    val goalX = if (hasPreferredX) preferredX else find.x
                    cursor = find.prevFirst
                    layout(row, cursor)
                    var x = row.x0
                    while (i < row.numChars) {
                        val dx = getWidth(find.prevFirst, i++)
                        if (dx == GETWIDTH_NEWLINE)
                            break
                        x += dx
                        if (x > goalX)
                            break
                        ++cursor
                    }
                    clamp()

                    hasPreferredX = true
                    preferredX = goalX

                    if (sel) selectEnd = cursor
                }
                Unit
            }
            K.DELETE, K.DELETE or K.SHIFT -> {
                if (hasSelection) deleteSelection()
                else if (cursor < stringLen)
                    delete(cursor, 1)
                hasPreferredX = false
            }
            K.BACKSPACE, K.BACKSPACE or K.SHIFT -> {
                if (hasSelection) deleteSelection()
                else {
                    clamp()
                    if (cursor > 0) {
                        delete(cursor - 1, 1)
                        --cursor
                    }
                }
                hasPreferredX = false
            }
            K.TEXTSTART -> {
                cursor = 0
                selectStart = 0
                selectEnd = 0
                hasPreferredX = false
            }
            K.TEXTEND -> {
                cursor = stringLen
                selectStart = 0
                selectEnd = 0
                hasPreferredX = false
            }
            K.TEXTSTART or K.SHIFT -> {
                prepSelectionAtCursor()
                cursor = 0
                selectEnd = 0
                hasPreferredX = false
            }
            K.TEXTEND or K.SHIFT -> {
                prepSelectionAtCursor()
                cursor = stringLen
                selectEnd = cursor
                hasPreferredX = false
            }
            K.LINESTART -> {
                clamp()
                moveToFirst()
                if (singleLine)
                    cursor = 0
                else while (cursor > 0 && getChar(cursor - 1) != '\n')
                    --cursor
                hasPreferredX = false
            }
            K.LINEEND -> {
                val n = stringLen
                clamp()
                moveToFirst()
                if (singleLine)
                    cursor = n
                else while (cursor < n && getChar(cursor) != 'n')
                    ++cursor
                hasPreferredX = false
            }
            K.LINESTART or K.SHIFT -> {
                clamp()
                prepSelectionAtCursor()
                if (singleLine)
                    cursor = 0
                else while (cursor > 0 && getChar(cursor - 1) != '\n')
                    --cursor
                selectEnd = cursor
                state.hasPreferredX = false
            }
            K.LINEEND or K.SHIFT -> {
                val n = stringLen
                clamp()
                prepSelectionAtCursor()
                if (singleLine)
                    cursor = n
                else while (cursor < n && getChar(cursor) != '\n')
                    ++cursor
                selectEnd = cursor
                hasPreferredX = false
            }
            else -> {
                val c = keyToText(key)
                if (c > 0) {
                    val ch = c.c
                    // can't add newline in single-line mode
                    if (ch == '\n' && singleLine) return@with

                    if (insertMode && !hasSelection && cursor < stringLen) {
                        makeundoReplace(cursor, 1, 1)
                        deleteChars(cursor, 1)
                        if (insertChars(cursor, charArrayOf(ch), 0, 1)) {
                            ++cursor
                            hasPreferredX = false
                        }
                    } else {
                        deleteSelection() // implicity clamps
                        if (insertChars(cursor, charArrayOf(ch), 0, 1)) {
                            makeundoInsert(cursor, 1)
                            ++cursor
                            hasPreferredX = false
                        }
                    }
                }
            }
        }
    }
}