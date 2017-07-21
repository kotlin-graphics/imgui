package imgui

import gli.has
import glm_.glm
import imgui.internal.isSpace
import imgui.stb.stb

/** Internal state of the currently focused/edited text input box   */
class textEdit {

    /** widget id owning the text state */
    var id = 0
    /** edit buffer, we need to persist but can't guarantee the persistence of the user-provided buffer. so we copy
    into own buffer.    */
    var text = ""
    /** backup of end-user buffer at the time of focus (in UTF-8, unaltered)    */
    var initialText = ""

    var tempTextBuffer = charArrayOf()
    /** we need to maintain our buffer length in both UTF-8 and wchar format.   */
    var curLenA = 0

    var curLenW = 0
    /** end-user buffer size    */
    var bufSizeA = 0

    var scrollX = 0f

    val state = stb.TexteditState()

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

    val hasSelection get() = state.selectStart != state.selectEnd

    fun clearSelection() {
        state.selectStart = state.cursor
        state.selectEnd = state.cursor
    }

    fun selectAll() {
        state.selectStart = 0
        state.selectEnd = curLenW
        state.cursor = state.selectEnd
        state.hasPreferredX = false
    }

    fun onKeyPressed(key: Int) {
        key(key)
        CursorFollow = true
        CursorAnimReset()
    }

    fun click(x: Float, y: Float) = with(state) {
        cursor = locateCoord(x, y)
        selectStart = cursor
        selectEnd = cursor
        hasPreferredX = false
    }

    /** traverse the layout to locate the nearest character to a display position   */
    private fun locateCoord(x: Float, y: Float): Int {

        val r = stb.TexteditRow()
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

    private fun getWidth(lineStartIdx: Int, charIdx: Int): Float {
        val c = text[lineStartIdx + charIdx]
        return if (c == '\n') -1f else Context.font.getCharAdvance_(c) * (Context.fontSize / Context.font.fontSize)
    }

    /** API click: on mouse down, move the cursor to the clicked location, and reset the selection  */
    fun texteditClick(x: Float, y: Float) {
        state.cursor = locateCoord(x, y)
        state.selectStart = state.cursor
        state.selectEnd = state.cursor
        state.hasPreferredX = false
    }


    fun layout(r: stb.TexteditRow, lineStartIdx: Int) {

        val textRemaining = IntArray(1)
        val size = inputTextCalcTextSizeW(text + lineStartIdx, curLenW, textRemaining, null, true)
        with(r) {
            r.x0 = 0f
            r.x1 = size.x
            r.baselineYDelta = size.y
            r.yMin = 0f
            r.yMax = size.y
            r.numChars = textRemaining[0] - lineStartIdx
        }
    }

    // API key: process a keyboard input
    fun key(key: Int): Unit = when (key) {

//            stb.TEXTEDIT_K_INSERT:            state->insert_mode = !state->insert_mode

        stb.TEXTEDIT_K_UNDO -> {
            textUndo()
            state.hasPreferredX = false
        }
        stb.TEXTEDIT_K_REDO -> {
            textRedo()
            state.hasPreferredX = false
        }
        stb.TEXTEDIT_K_LEFT -> {
            // if currently there's a selection, move cursor to start of selection
            if (state.hasSelection)
                state.moveToFirst()
            else if (state.cursor > 0)
                --state.cursor
            state.hasPreferredX = false
        }
        stb.TEXTEDIT_K_RIGHT -> {
            // if currently there's a selection, move cursor to end of selection
            if (state.hasSelection)
                state.moveToLast(this)
            else
                ++state.cursor
            state.clamp(this)
            state.hasPreferredX = false
        }
        stb.TEXTEDIT_K_LEFT or stb.TEXTEDIT_K_SHIFT -> {
            state.clamp(this)
            state.prepSelectionAtCursor()
            // move selection left
            if (state.selectEnd > 0)
                --state.selectEnd
            state.cursor = state.selectEnd
            state.hasPreferredX = false
        }
        stb.TEXTEDIT_K_WORDLEFT ->
            if (state.hasSelection)
                state.moveToFirst()
            else {
                state.cursor = moveWordLeft(state.cursor)
                state.clamp(this)
            }
        stb.TEXTEDIT_K_WORDLEFT or stb.TEXTEDIT_K_SHIFT -> {
            if (!state.hasSelection)
                state.prepSelectionAtCursor()
            state.cursor = moveWordLeft(state.cursor)
            state.selectEnd = state.cursor
            state.clamp(this)
        }
        stb.TEXTEDIT_K_WORDRIGHT ->
            if (state.hasSelection)
                state.moveToLast(this)
            else {
                state.cursor = moveWordRight(state.cursor)
                state.clamp(this)
            }
        stb.TEXTEDIT_K_WORDRIGHT or stb.TEXTEDIT_K_SHIFT -> {
            if (!state.hasSelection)
                state.prepSelectionAtCursor()
            state.cursor = moveWordRight(state.cursor)
            state.selectEnd = state.cursor
            state.clamp(this)
        }
        stb.TEXTEDIT_K_RIGHT or stb.TEXTEDIT_K_SHIFT -> {
            state.prepSelectionAtCursor()
            // move selection right
            ++state.selectEnd
            state.clamp(this)
            state.cursor = state.selectEnd
            state.hasPreferredX = false
        }
        stb.TEXTEDIT_K_DOWN, stb.TEXTEDIT_K_DOWN or stb.TEXTEDIT_K_SHIFT -> {
            val find = stb.FindState()
            val row = stb.TexteditRow()
            var i = 0
            var sel = key has stb.TEXTEDIT_K_SHIFT

            if (state.singleLine)
            // on windows, up&down in single-line behave like left&right
                key(stb.TEXTEDIT_K_RIGHT or (key and stb.TEXTEDIT_K_SHIFT))

            if (sel)
                state.prepSelectionAtCursor()
            else if (state.hasSelection)
                state.moveToLast(this)

            // compute current position of cursor point
            state.clamp(this)
            findCharpos(find, state.cursor, state.singleLine)

            // now find character position down a row
            if (find.length != 0) {
                val goalX = if (state.hasPreferredX) state.preferredX else find.x
                var x = 0f
                val start = find.firstChar + find.length
                state.cursor = start
                layout(row, state.cursor)
                x = row.x0
                for (i in 0 until row.numChars) {
                    val dx = getWidth(start, i)
                    if (dx == STB_TEXTEDIT_GETWIDTH_NEWLINE)
                        break
                    x += dx
                    if (x > goalX)
                        break
                    ++state.cursor
                }
                state.clamp(this)

                state.hasPreferredX = true
                state.preferredX = goalX

                if (sel)
                    state.selectEnd = state.cursor
            }
            Unit
        }
        stb.TEXTEDIT_K_UP, stb.TEXTEDIT_K_UP or stb.TEXTEDIT_K_SHIFT -> {
            val find = stb.FindState()
            val row = stb.TexteditRow()
            var i = 0
            var sel = key has stb.TEXTEDIT_K_SHIFT

            if (state.singleLine)
            // on windows, up&down become left&right
                key(stb.TEXTEDIT_K_LEFT or (key and stb.TEXTEDIT_K_SHIFT))

            if (sel)
                state.prepSelectionAtCursor()
            else if (state.hasSelection)
                state.moveToFirst()

            // compute current position of cursor point
            state.clamp(this)
            findCharpos(find, state.cursor, state.singleLine)

            // can only go up if there's a previous row
            if (find.prevFirst != find.firstChar) {
                // now find character position up a row
                val goalX = if (state.hasPreferredX) state.preferredX else find.x
                var x = 0f
                state.cursor = find.prevFirst
                layout(row, state.cursor)
                x = row.x0
                for (i in 0 until row.numChars) {
                    val dx = getWidth(find.prevFirst, i)
                    if (dx == STB_TEXTEDIT_GETWIDTH_NEWLINE)
                        break
                    x += dx
                    if (x > goalX)
                        break
                    ++state.cursor
                }
                state.clamp(this)

                state.hasPreferredX = true
                state.preferredX = goalX

                if (sel)
                    state.selectEnd = state.cursor
            }
            Unit
        }
        stb.TEXTEDIT_K_DELETE, stb.TEXTEDIT_K_DELETE or stb.TEXTEDIT_K_SHIFT ->
            if (state.hasSelection)
                state.delete_selection(str, state)
            else {
                int n = STB_TEXTEDIT_STRINGLEN (str)
                if (state->cursor < n)
                stb_textedit_delete(str, state, state->cursor, 1)
            }
        state -> has_preferred_x = 0
        break

                case STB_TEXTEDIT_K_BACKSPACE:
            case STB_TEXTEDIT_K_BACKSPACE | STB_TEXTEDIT_K_SHIFT :
        if (STB_TEXT_HAS_SELECTION(state))
            stb_textedit_delete_selection(str, state)
        else {
            stb_textedit_clamp(str, state)
            if (state->cursor > 0) {
                stb_textedit_delete(str, state, state->cursor-1, 1)
                --state->cursor
            }
        }
                state -> has_preferred_x = 0
        break

            #ifdef STB_TEXTEDIT_K_TEXTSTART2
                case STB_TEXTEDIT_K_TEXTSTART2 :
            #endif
        case STB_TEXTEDIT_K_TEXTSTART :
                state -> cursor = state
        -> select_start = state
        -> select_end = 0
        state -> has_preferred_x = 0
        break

            #ifdef STB_TEXTEDIT_K_TEXTEND2
                case STB_TEXTEDIT_K_TEXTEND2 :
            #endif
        case STB_TEXTEDIT_K_TEXTEND :
                state -> cursor = STB_TEXTEDIT_STRINGLEN(str)
        state -> select_start = state
        -> select_end = 0
        state -> has_preferred_x = 0
        break

            #ifdef STB_TEXTEDIT_K_TEXTSTART2
                case STB_TEXTEDIT_K_TEXTSTART2 | STB_TEXTEDIT_K_SHIFT :
            #endif
        case STB_TEXTEDIT_K_TEXTSTART | STB_TEXTEDIT_K_SHIFT :
                stb_textedit_prep_selection_at_cursor (state)
                state -> cursor = state
        -> select_end = 0
        state -> has_preferred_x = 0
        break

            #ifdef STB_TEXTEDIT_K_TEXTEND2
                case STB_TEXTEDIT_K_TEXTEND2 | STB_TEXTEDIT_K_SHIFT :
            #endif
        case STB_TEXTEDIT_K_TEXTEND | STB_TEXTEDIT_K_SHIFT :
                stb_textedit_prep_selection_at_cursor (state)
                state -> cursor = state
        -> select_end = STB_TEXTEDIT_STRINGLEN(str)
        state -> has_preferred_x = 0
        break


            #ifdef STB_TEXTEDIT_K_LINESTART2
                case STB_TEXTEDIT_K_LINESTART2 :
            #endif
        case STB_TEXTEDIT_K_LINESTART :
                stb_textedit_clamp (str, state)
            stb_textedit_move_to_first(state)
        if (state -> single_line
            )
            state
        -> cursor = 0
        else while (state -> cursor > 0 && STB_TEXTEDIT_GETCHAR(str, state
        -> cursor - 1
            )
            != STB_TEXTEDIT_NEWLINE
            )
            --state
        -> cursor
        state -> has_preferred_x = 0
        break

            #ifdef STB_TEXTEDIT_K_LINEEND2
                case STB_TEXTEDIT_K_LINEEND2 :
            #endif
        case STB_TEXTEDIT_K_LINEEND : {
            int n = STB_TEXTEDIT_STRINGLEN (str)
            stb_textedit_clamp(str, state)
            stb_textedit_move_to_first(state)
            if (state->single_line)
            state->cursor = n
            else while (state->cursor < n && STB_TEXTEDIT_GETCHAR(str, state->cursor) != STB_TEXTEDIT_NEWLINE)
            ++state->cursor
            state->has_preferred_x = 0
            break
        }

            #ifdef STB_TEXTEDIT_K_LINESTART2
                case STB_TEXTEDIT_K_LINESTART2 | STB_TEXTEDIT_K_SHIFT :
            #endif
        case STB_TEXTEDIT_K_LINESTART | STB_TEXTEDIT_K_SHIFT :
                stb_textedit_clamp (str, state)
            stb_textedit_prep_selection_at_cursor(state)
        if (state -> single_line
            )
            state
        -> cursor = 0
        else while (state -> cursor > 0 && STB_TEXTEDIT_GETCHAR(str, state
        -> cursor - 1
            )
            != STB_TEXTEDIT_NEWLINE
            )
            --state
        -> cursor
        state -> select_end = state
        -> cursor
        state -> has_preferred_x = 0
        break

            #ifdef STB_TEXTEDIT_K_LINEEND2
                case STB_TEXTEDIT_K_LINEEND2 | STB_TEXTEDIT_K_SHIFT :
            #endif
        case STB_TEXTEDIT_K_LINEEND | STB_TEXTEDIT_K_SHIFT : {
            int n = STB_TEXTEDIT_STRINGLEN (str)
            stb_textedit_clamp(str, state)
            stb_textedit_prep_selection_at_cursor(state)
            if (state->single_line)
            state->cursor = n
            else while (state->cursor < n && STB_TEXTEDIT_GETCHAR(str, state->cursor) != STB_TEXTEDIT_NEWLINE)
            ++state->cursor
            state->select_end = state->cursor
            state->has_preferred_x = 0
            break
        }

                default : {
            int c = STB_TEXTEDIT_KEYTOTEXT (key)
            if (c > 0) {
                STB_TEXTEDIT_CHARTYPE ch =(STB_TEXTEDIT_CHARTYPE) c

                        // can't add newline in single-line mode
                        if (c == '\n' && state->single_line)
                break

                if (state->insert_mode && !STB_TEXT_HAS_SELECTION(state) && state->cursor < STB_TEXTEDIT_STRINGLEN(str)) {
                    stb_text_makeundo_replace(str, state, state->cursor, 1, 1)
                    STB_TEXTEDIT_DELETECHARS(str, state->cursor, 1)
                    if (STB_TEXTEDIT_INSERTCHARS(str, state->cursor, &ch, 1)) {
                    ++state->cursor
                    state->has_preferred_x = 0
                }
                } else {
                    stb_textedit_delete_selection(str, state) // implicity clamps
                    if (STB_TEXTEDIT_INSERTCHARS(str, state->cursor, &ch, 1)) {
                    stb_text_makeundo_insert(state, state->cursor, 1)
                    ++state->cursor
                    state->has_preferred_x = 0
                }
                }
            }
            break
        }

// @TODO:
//    STB_TEXTEDIT_K_PGUP      - move cursor up a page
//    STB_TEXTEDIT_K_PGDOWN    - move cursor down a page
    }

    private fun textUndo() {

        val s = state.undostate
        if (s.undoPoint == 0) return

        // we need to do two things: apply the undo record, and create a redo record
        val u = stb.UndoRecord(s.undoRec[s.undoPoint - 1])
        val r = s.undoRec[s.redoPoint - 1]
        r.charStorage = -1

        r.insertLength = u.deleteLength
        r.deleteLength = u.insertLength
        r.where = u.where
        TODO()/*
        if (u.deleteLength != 0) {
            // if the undo record says to delete characters, then the redo record will
            // need to re-insert the characters that get deleted, so we need to store
            // them.

            // there are three cases:
            //    there's enough room to store the characters
            //    characters stored for *redoing* don't leave room for redo
            //    characters stored for *undoing* don't leave room for redo
            // if the last is true, we have to bail

            if (s->undo_char_point+u.delete_length >= STB_TEXTEDIT_UNDOCHARCOUNT) {
                // the undo records take up too much character space; there's no space to store the redo characters
                r ->
                insert_length = 0
            } else {
                int i

                        // there's definitely room to store the characters eventually
                        while (s->undo_char_point+u.delete_length > s->redo_char_point) {
                // there's currently not enough room, so discard a redo record
                stb_textedit_discard_redo(s)
                // should never happen:
                if (s->redo_point == STB_TEXTEDIT_UNDOSTATECOUNT)
                return
            }
                r = & s->undo_rec[s->redo_point-1]

                r->char_storage = s->redo_char_point-u.delete_length
                s->redo_char_point = s->redo_char_point-(short) u.delete_length

                // now save the characters
                for (i= 0; i < u.delete_length; ++i)
                s->undo_char[r->char_storage+i] = STB_TEXTEDIT_GETCHAR(str, u.where+i)
            }

            // now we can carry out the deletion
            STB_TEXTEDIT_DELETECHARS(str, u.where, u.delete_length)
        }

        // check type of recorded action:
        if (u.insert_length) {
            // easy case: was a deletion, so we need to insert n characters
            STB_TEXTEDIT_INSERTCHARS(str, u.where, & s->undo_char[u.char_storage], u.insert_length)
            s->undo_char_point -= u.insert_length
        }

        state->cursor = u.where+u.insert_length

        s->undo_point--
        s->redo_point--*/
    }

    private fun textRedo() {

        TODO()/*
        StbUndoState *s = &state->undostate;
        StbUndoRecord *u, r;
        if (s->redo_point == STB_TEXTEDIT_UNDOSTATECOUNT)
        return;

        // we need to do two things: apply the redo record, and create an undo record
        u = &s->undo_rec[s->undo_point];
        r = s->undo_rec[s->redo_point];

        // we KNOW there must be room for the undo record, because the redo record
        // was derived from an undo record

        u->delete_length = r.insert_length;
        u->insert_length = r.delete_length;
        u->where = r.where;
        u->char_storage = -1;

        if (r.delete_length) {
            // the redo record requires us to delete characters, so the undo record
            // needs to store the characters

            if (s->undo_char_point + u->insert_length > s->redo_char_point) {
                u->insert_length = 0;
                u->delete_length = 0;
            } else {
                int i;
                u->char_storage = s->undo_char_point;
                s->undo_char_point = s->undo_char_point + u->insert_length;

                // now save the characters
                for (i=0; i < u->insert_length; ++i)
                s->undo_char[u->char_storage + i] = STB_TEXTEDIT_GETCHAR(str, u->where + i);
            }

            STB_TEXTEDIT_DELETECHARS(str, r.where, r.delete_length);
        }

        if (r.insert_length) {
            // easy case: need to insert n characters
            STB_TEXTEDIT_INSERTCHARS(str, r.where, &s->undo_char[r.char_storage], r.insert_length);
            s->redo_char_point += r.insert_length;
        }

        state->cursor = r.where + r.insert_length;

        s->undo_point++;
        s->redo_point++;*/
    }

    /** find the x/y location of a character, and remember info about the previous row in case we get a move-up event
     *  (for page up, we'll have to rescan) */
    fun findCharpos(find: stb.FindState, n: Int, singleLine: Boolean) {
        val r = stb.TexteditRow()
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

    private fun moveWordLeft(idx: Int): Int {
        var idx = idx - 1
        while (idx >= 0 && !isWordBoundaryFromRight(idx)) idx--
        return if (idx < 0) 0 else idx
    }

    fun isWordBoundaryFromRight(idx: Int) = if (idx > 0) text[idx - 1].isSeparator && !text[idx].isSeparator else true

    fun moveWordRight(idx: Int): Int {
        var idx = idx + 1
        val len = curLenW
        while (idx < len && !isWordBoundaryFromRight(idx)) idx++
        return if (idx > len) len else idx
    }

    val Char.isSeparator get() = isSpace(this) || this == ',' || this == ';' || this == '(' || this == ')' ||
            this == '{' || this == '}' || this == '[' || this == ']' || this == '|'

    val STB_TEXTEDIT_GETWIDTH_NEWLINE = -1f
}