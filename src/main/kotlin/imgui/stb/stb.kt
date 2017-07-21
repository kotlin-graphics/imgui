package imgui.stb

import imgui.textEdit

object stb {

    val TEXTEDIT_UNDOSTATECOUNT = 99
    val TEXTEDIT_UNDOCHARCOUNT = 999

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
    }

    class UndoState {
        val undoRec = Array(TEXTEDIT_UNDOSTATECOUNT, { UndoRecord() })
        val undoChar = CharArray(TEXTEDIT_UNDOCHARCOUNT)
        var undoPoint = 0
        var redoPoint = 0
        var undoCharPoint = 0
        var redoCharPoint = 0

        fun clear() {
            undoPoint = 0
            undoCharPoint = 0
            redoPoint = TEXTEDIT_UNDOSTATECOUNT
            redoCharPoint = TEXTEDIT_UNDOCHARCOUNT
        }
    }

    class TexteditState {

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
        var padding1 = '\u0000'
        var padding2 = '\u0000'
        var padding3 = '\u0000'
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

        val hasSelection get() = selectStart != selectEnd


        /** make the selection/cursor state valid if client altered the string  */
        fun clamp(str: textEdit) {
            val n = str.curLenW
            if (hasSelection) {
                if (selectStart > n) selectStart = n
                if (selectEnd > n) selectEnd = n
                // if clamping forced them to be equal, move the cursor to match
                if (selectStart == selectEnd)
                    cursor = selectStart
            }
            if (cursor > n) cursor = n
        }

        /** delete characters while updating undo   */
        fun delete(str: textEdit, where:Int, len:Int)        {
            stb_text_makeundo_delete(str, state, where, len);
            STB_TEXTEDIT_DELETECHARS(str, where, len);
            state->has_preferred_x = 0;
        }

        /** delete the section  */
        fun deleteSelection(str: textEdit)        {
            clamp(str)
            if (hasSelection) {
                if (selectStart < selectEnd) {
                    delete(str, state, state->select_start, state->select_end - state->select_start)
                    state->select_end = state->cursor = state->select_start
                } else {
                    stb_textedit_delete(str, state, state->select_end, state->select_start - state->select_end)
                    state->select_start = state->cursor = state->select_end
                }
                state->has_preferred_x = 0
            }
        }

        /** canoncialize the selection so start <= end  */
        fun sortSelection() {
            if (selectEnd < selectStart) {
                val temp = selectEnd
                selectEnd = selectStart
                selectStart = temp
            }
        }

        /** move cursor to first character of selection */
        fun moveToFirst() {
            if (hasSelection) {
                sortSelection()
                cursor = selectStart
                selectEnd = selectStart
                hasPreferredX = false
            }
        }

        // move cursor to last character of selection
        fun moveToLast(str: textEdit) {
            if (hasSelection) {
                sortSelection()
                clamp(str)
                cursor = selectEnd
                selectStart = selectEnd
                hasPreferredX = false
            }
        }

        // update selection and cursor to match each other
        fun prepSelectionAtCursor() {
            if (!hasSelection) {
                selectStart = cursor
                selectEnd = cursor
            } else
                cursor = selectEnd
        }
    }

    /** Result of layout query, used by stb_textedit to determine where the text in each row is.
     *  result of layout query  */
    class TexteditRow {
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

    class FindState    {
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

    /*  We don't use an enum so we can build even with conflicting symbols (if another user of stb_textedit.h leak their 
        STB_TEXTEDIT_K_* symbols)     */
    /** keyboard input to move cursor left  */
    val TEXTEDIT_K_LEFT = 0x10000
    /** keyboard input to move cursor right */
    val TEXTEDIT_K_RIGHT = 0x10001
    /** keyboard input to move cursor up    */
    val TEXTEDIT_K_UP = 0x10002
    /** keyboard input to move cursor down  */
    val TEXTEDIT_K_DOWN = 0x10003
    /** keyboard input to move cursor to start of line  */
    val TEXTEDIT_K_LINESTART = 0x10004
    /** keyboard input to move cursor to end of line    */
    val TEXTEDIT_K_LINEEND = 0x10005
    /** keyboard input to move cursor to start of text  */
    val TEXTEDIT_K_TEXTSTART = 0x10006
    /** keyboard input to move cursor to end of text    */
    val TEXTEDIT_K_TEXTEND = 0x10007
    /** keyboard input to delete selection or character under cursor    */
    val TEXTEDIT_K_DELETE = 0x10008
    /** keyboard input to delete selection or character left of cursor  */
    val TEXTEDIT_K_BACKSPACE = 0x10009
    /** keyboard input to perform undo  */
    val TEXTEDIT_K_UNDO = 0x1000A
    /** keyboard input to perform redo  */
    val TEXTEDIT_K_REDO = 0x1000B
    /** keyboard input to move cursor left one word */
    val TEXTEDIT_K_WORDLEFT = 0x1000C
    /** keyboard input to move cursor right one word    */
    val TEXTEDIT_K_WORDRIGHT = 0x1000D

    val TEXTEDIT_K_SHIFT = 0x20000
}