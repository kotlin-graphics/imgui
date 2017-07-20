package imgui.stb

import imgui.inputTextCalcTextSizeW
import imgui.internal.TextEditState

object stb {

    val TEXTEDIT_UNDOSTATECOUNT = 99
    val TEXTEDIT_UNDOCHARCOUNT = 999

    class UndoRecord {
        var where = 0
        var insertLength = 0
        var deleteLength = 0
        var charStorage = 0
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
    }

    /** Result of layout query, used by stb_textedit to determine where the text in each row is.
     *  result of layout query  */
    class TexteditRow     {
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

        fun layout(obj:TextEditState, lineStartIdx:Int)        {
            TODO()
            val text = obj.text
//            val text_remaining = NULL;
//            val size = inputTextCalcTextSizeW(text + lineStartIdx, text + obj->CurLenW, &text_remaining, NULL, true);
//            r->x0 = 0.0f;
//            r->x1 = size.x;
//            r->baseline_y_delta = size.y;
//            r->ymin = 0.0f;
//            r->ymax = size.y;
//            r->num_chars = (int)(text_remaining - (text + line_start_idx));
        }
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