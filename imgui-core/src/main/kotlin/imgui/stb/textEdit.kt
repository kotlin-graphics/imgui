package imgui.stb

import gli_.has
import glm_.c
import imgui.internal.classes.InputTextState
import imgui.internal.classes.InputTextState.K

/*
 [DEAR IMGUI]
 This is a slightly modified version of stb_textedit.h 1.13.
 Those changes would need to be pushed into nothings/stb:
 - Fix in stb_textedit_discard_redo (see https://github.com/nothings/stb/issues/321)
 Grep for [DEAR IMGUI] to find the changes.

 stb_textedit.h - v1.13  - public domain - Sean Barrett
 Development of this library was sponsored by RAD Game Tools

 This C header file implements the guts of a multi-line text-editing
 widget; you implement display, word-wrapping, and low-level string
 insertion/deletion, and stb_textedit will map user inputs into
 insertions & deletions, plus updates to the cursor position,
 selection state, and undo state.

 It is intended for use in games and other systems that need to build
 their own custom widgets and which do not have heavy text-editing
 requirements (this library is not recommended for use for editing large
 texts, as its performance does not scale and it has limited undo).

 Non-trivial behaviors are modelled after Windows text controls.


 LICENSE

 See end of file for license information.


 DEPENDENCIES

 Uses the C runtime function 'memmove', which you can override
 by defining STB_TEXTEDIT_memmove before the implementation.
 Uses no other functions. Performs no runtime allocations.


 VERSION HISTORY

   1.13 (2019-02-07) fix bug in undo size management
   1.12 (2018-01-29) user can change STB_TEXTEDIT_KEYTYPE, fix redo to avoid crash
   1.11 (2017-03-03) fix HOME on last line, dragging off single-line textfield
   1.10 (2016-10-25) supress warnings about casting away const with -Wcast-qual
   1.9  (2016-08-27) customizable move-by-word
   1.8  (2016-04-02) better keyboard handling when mouse button is down
   1.7  (2015-09-13) change y range handling in case baseline is non-0
   1.6  (2015-04-15) allow STB_TEXTEDIT_memmove
   1.5  (2014-09-10) add support for secondary keys for OS X
   1.4  (2014-08-17) fix signed/unsigned warnings
   1.3  (2014-06-19) fix mouse clicking to round to nearest char boundary
   1.2  (2014-05-27) fix some RAD types that had crept into the new code
   1.1  (2013-12-15) move-by-word (requires STB_TEXTEDIT_IS_SPACE )
   1.0  (2012-07-26) improve documentation, initial public release
   0.3  (2012-02-24) bugfixes, single-line mode; insert mode
   0.2  (2011-11-28) fixes to undo/redo
   0.1  (2010-07-08) initial version

 ADDITIONAL CONTRIBUTORS

   Ulf Winklemann: move-by-word in 1.1
   Fabian Giesen: secondary key inputs in 1.5
   Martins Mozeiko: STB_TEXTEDIT_memmove in 1.6

   Bugfixes:
      Scott Graham
      Daniel Keller
      Omar Cornut
      Dan Thompson

 USAGE

 This file behaves differently depending on what symbols you define
 before including it.


 Header-file mode:

   If you do not define STB_TEXTEDIT_IMPLEMENTATION before including this,
   it will operate in "header file" mode. In this mode, it declares a
   single public symbol, STB_TexteditState, which encapsulates the current
   state of a text widget (except for the string, which you will store
   separately).

   To compile in this mode, you must define STB_TEXTEDIT_CHARTYPE to a
   primitive type that defines a single character (e.g. char, wchar_t, etc).

   To save space or increase undo-ability, you can optionally define the
   following things that are used by the undo system:

      STB_TEXTEDIT_POSITIONTYPE         small int type encoding a valid cursor position
      STB_TEXTEDIT_UNDOSTATECOUNT       the number of undo states to allow
      STB_TEXTEDIT_UNDOCHARCOUNT        the number of characters to store in the undo buffer

   If you don't define these, they are set to permissive types and
   moderate sizes. The undo system does no memory allocations, so
   it grows STB_TexteditState by the worst-case storage which is (in bytes):

        [4 + 3 * sizeof(STB_TEXTEDIT_POSITIONTYPE)] * STB_TEXTEDIT_UNDOSTATE_COUNT
      +          sizeof(STB_TEXTEDIT_CHARTYPE)      * STB_TEXTEDIT_UNDOCHAR_COUNT


 Implementation mode:

   If you define STB_TEXTEDIT_IMPLEMENTATION before including this, it
   will compile the implementation of the text edit widget, depending
   on a large number of symbols which must be defined before the include.

   The implementation is defined only as static functions. You will then
   need to provide your own APIs in the same file which will access the
   static functions.

   The basic concept is that you provide a "string" object which
   behaves like an array of characters. stb_textedit uses indices to
   refer to positions in the string, implicitly representing positions
   in the displayed textedit. This is true for both plain text and
   rich text; even with rich text stb_truetype interacts with your
   code as if there was an array of all the displayed characters.

 Symbols that must be the same in header-file and implementation mode:

     STB_TEXTEDIT_CHARTYPE             the character type
     STB_TEXTEDIT_POSITIONTYPE         small type that is a valid cursor position
     STB_TEXTEDIT_UNDOSTATECOUNT       the number of undo states to allow
     STB_TEXTEDIT_UNDOCHARCOUNT        the number of characters to store in the undo buffer

 Symbols you must define for implementation mode:

    STB_TEXTEDIT_STRING               the type of object representing a string being edited,
                                      typically this is a wrapper object with other data you need

    STB_TEXTEDIT_STRINGLEN(obj)       the length of the string (ideally O(1))
    STB_TEXTEDIT_LAYOUTROW(&r,obj,n)  returns the results of laying out a line of characters
                                        starting from character #n (see discussion below)
    STB_TEXTEDIT_GETWIDTH(obj,n,i)    returns the pixel delta from the xpos of the i'th character
                                        to the xpos of the i+1'th char for a line of characters
                                        starting at character #n (i.e. accounts for kerning
                                        with previous char)
    STB_TEXTEDIT_KEYTOTEXT(k)         maps a keyboard input to an insertable character
                                        (return type is int, -1 means not valid to insert)
    STB_TEXTEDIT_GETCHAR(obj,i)       returns the i'th character of obj, 0-based
    STB_TEXTEDIT_NEWLINE              the character returned by _GETCHAR() we recognize
                                        as manually wordwrapping for end-of-line positioning

    STB_TEXTEDIT_DELETECHARS(obj,i,n)      delete n characters starting at i
    STB_TEXTEDIT_INSERTCHARS(obj,i,c*,n)   insert n characters at i (pointed to by STB_TEXTEDIT_CHARTYPE*)

    STB_TEXTEDIT_K_SHIFT       a power of two that is or'd in to a keyboard input to represent the shift key

    STB_TEXTEDIT_K_LEFT        keyboard input to move cursor left
    STB_TEXTEDIT_K_RIGHT       keyboard input to move cursor right
    STB_TEXTEDIT_K_UP          keyboard input to move cursor up
    STB_TEXTEDIT_K_DOWN        keyboard input to move cursor down
    STB_TEXTEDIT_K_LINESTART   keyboard input to move cursor to start of line  // e.g. HOME
    STB_TEXTEDIT_K_LINEEND     keyboard input to move cursor to end of line    // e.g. END
    STB_TEXTEDIT_K_TEXTSTART   keyboard input to move cursor to start of text  // e.g. ctrl-HOME
    STB_TEXTEDIT_K_TEXTEND     keyboard input to move cursor to end of text    // e.g. ctrl-END
    STB_TEXTEDIT_K_DELETE      keyboard input to delete selection or character under cursor
    STB_TEXTEDIT_K_BACKSPACE   keyboard input to delete selection or character left of cursor
    STB_TEXTEDIT_K_UNDO        keyboard input to perform undo
    STB_TEXTEDIT_K_REDO        keyboard input to perform redo

 Optional:
    STB_TEXTEDIT_K_INSERT              keyboard input to toggle insert mode
    STB_TEXTEDIT_IS_SPACE(ch)          true if character is whitespace (e.g. 'isspace'),
                                          required for default WORDLEFT/WORDRIGHT handlers
    STB_TEXTEDIT_MOVEWORDLEFT(obj,i)   custom handler for WORDLEFT, returns index to move cursor to
    STB_TEXTEDIT_MOVEWORDRIGHT(obj,i)  custom handler for WORDRIGHT, returns index to move cursor to
    STB_TEXTEDIT_K_WORDLEFT            keyboard input to move cursor left one word // e.g. ctrl-LEFT
    STB_TEXTEDIT_K_WORDRIGHT           keyboard input to move cursor right one word // e.g. ctrl-RIGHT
    STB_TEXTEDIT_K_LINESTART2          secondary keyboard input to move cursor to start of line
    STB_TEXTEDIT_K_LINEEND2            secondary keyboard input to move cursor to end of line
    STB_TEXTEDIT_K_TEXTSTART2          secondary keyboard input to move cursor to start of text
    STB_TEXTEDIT_K_TEXTEND2            secondary keyboard input to move cursor to end of text

 Todo:
    STB_TEXTEDIT_K_PGUP        keyboard input to move cursor up a page
    STB_TEXTEDIT_K_PGDOWN      keyboard input to move cursor down a page

 Keyboard input must be encoded as a single integer value; e.g. a character code
 and some bitflags that represent shift states. to simplify the interface, SHIFT must
 be a bitflag, so we can test the shifted state of cursor movements to allow selection,
 i.e. (STB_TEXTED_K_RIGHT|STB_TEXTEDIT_K_SHIFT) should be shifted right-arrow.

 You can encode other things, such as CONTROL or ALT, in additional bits, and
 then test for their presence in e.g. STB_TEXTEDIT_K_WORDLEFT. For example,
 my Windows implementations add an additional CONTROL bit, and an additional KEYDOWN
 bit. Then all of the STB_TEXTEDIT_K_ values bitwise-or in the KEYDOWN bit,
 and I pass both WM_KEYDOWN and WM_CHAR events to the "key" function in the
 API below. The control keys will only match WM_KEYDOWN events because of the
 keydown bit I add, and STB_TEXTEDIT_KEYTOTEXT only tests for the KEYDOWN
 bit so it only decodes WM_CHAR events.

 STB_TEXTEDIT_LAYOUTROW returns information about the shape of one displayed
 row of characters assuming they start on the i'th character--the width and
 the height and the number of characters consumed. This allows this library
 to traverse the entire layout incrementally. You need to compute word-wrapping
 here.

 Each textfield keeps its own insert mode state, which is not how normal
 applications work. To keep an app-wide insert mode, update/copy the
 "insert_mode" field of STB_TexteditState before/after calling API functions.

 API

    void stb_textedit_initialize_state(STB_TexteditState *state, int is_single_line)

    void stb_textedit_click(STB_TEXTEDIT_STRING *str, STB_TexteditState *state, float x, float y)
    void stb_textedit_drag(STB_TEXTEDIT_STRING *str, STB_TexteditState *state, float x, float y)
    int  stb_textedit_cut(STB_TEXTEDIT_STRING *str, STB_TexteditState *state)
    int  stb_textedit_paste(STB_TEXTEDIT_STRING *str, STB_TexteditState *state, STB_TEXTEDIT_CHARTYPE *text, int len)
    void stb_textedit_key(STB_TEXTEDIT_STRING *str, STB_TexteditState *state, STB_TEXEDIT_KEYTYPE key)

    Each of these functions potentially updates the string and updates the
    state.

      initialize_state:
          set the textedit state to a known good default state when initially
          constructing the textedit.

      click:
          call this with the mouse x,y on a mouse down; it will update the cursor
          and reset the selection start/end to the cursor point. the x,y must
          be relative to the text widget, with (0,0) being the top left.

      drag:
          call this with the mouse x,y on a mouse drag/up; it will update the
          cursor and the selection end point

      cut:
          call this to delete the current selection; returns true if there was
          one. you should FIRST copy the current selection to the system paste buffer.
          (To copy, just copy the current selection out of the string yourself.)

      paste:
          call this to paste text at the current cursor point or over the current
          selection if there is one.

      key:
          call this for keyboard inputs sent to the textfield. you can use it
          for "key down" events or for "translated" key events. if you need to
          do both (as in Win32), or distinguish Unicode characters from control
          inputs, set a high bit to distinguish the two; then you can define the
          various definitions like STB_TEXTEDIT_K_LEFT have the is-key-event bit
          set, and make STB_TEXTEDIT_KEYTOCHAR check that the is-key-event bit is
          clear. STB_TEXTEDIT_KEYTYPE defaults to int, but you can #define it to
          anything other type you wante before including.


   When rendering, you can read the cursor position and selection state from
   the STB_TexteditState.


 Notes:

 This is designed to be usable in IMGUI, so it allows for the possibility of
 running in an IMGUI that has NOT cached the multi-line layout. For this
 reason, it provides an interface that is compatible with computing the
 layout incrementally--we try to make sure we make as few passes through
 as possible. (For example, to locate the mouse pointer in the text, we
 could define functions that return the X and Y positions of characters
 and binary search Y and then X, but if we're doing dynamic layout this
 will run the layout algorithm many times, so instead we manually search
 forward in one pass. Similar logic applies to e.g. up-arrow and
 down-arrow movement.)

 If it's run in a widget that *has* cached the layout, then this is less
 efficient, but it's not horrible on modern computers. But you wouldn't
 want to edit million-line files with it.
*/

////////////////////////////////////////////////////////////////////////
//
//     STB_TexteditState
//
// Definition of STB_TexteditState which you should store
// per-textfield; it includes cursor position, selection state,
// and undo state.
//

object te {

    const val UNDOSTATECOUNT = 99
    const val UNDOCHARCOUNT = 999

    class UndoRecord {
        // private data
        var where = 0
        var insertLength = 0
        var deleteLength = 0
        var charStorage = 0
    }

    class UndoState {
        // private data
        val undoRec = Array(UNDOSTATECOUNT) { UndoRecord() }
        val undoChar = CharArray(UNDOCHARCOUNT)
        var undoPoint = 0
        var redoPoint = 0
        var undoCharPoint = 0
        var redoCharPoint = 0

        fun flushRedo() {
            redoPoint = UNDOSTATECOUNT
            redoCharPoint = UNDOCHARCOUNT
        }

        /** discard the oldest entry in the undo list */
        fun discardUndo() {
            if (undoPoint > 0) {
                // if the 0th undo state has characters, clean those up
                if (undoRec[0].charStorage >= 0) {
                    val n = undoRec[0].insertLength
                    // delete n characters from all other records
                    undoCharPoint -= n
                    STB_TEXTEDIT_memmove(state->undo_char, state->undo_char+n, (size_t) (state->undo_char_point*sizeof(STB_TEXTEDIT_CHARTYPE)));
                    for (i= 0; i < state->undo_point; ++i)
                    if (state->undo_rec[i].char_storage >= 0)
                    state->undo_rec[i].char_storage -= n; // @OPTIMIZE: get rid of char_storage and infer it
                }
                --state->undo_point;
                STB_TEXTEDIT_memmove(state->undo_rec, state->undo_rec+1, (size_t) (state->undo_point*sizeof(state->undo_rec[0])));
            }
        }
    }

    class State {
        /////////////////////
        //
        // public data
        //

        /** position of the text cursor within the string */
        var cursor = 0


        // selection start and end point in characters; if equal, no selection.
        // note that start may be less than or greater than end (e.g. when
        // dragging the mouse, start is where the initial click was, and you
        // can drag in either direction)

        /** selection start point */
        var selectStart = 0

        var selectEnd = 0

        /** each textfield keeps its own insert mode state. to keep an app-wide
         *  insert mode, copy this value in/out of the app state */
        var insertMode = false

        /////////////////////
        //
        // private data
        //
//        unsigned char cursor_at_end_of_line // not implemented yet
//        unsigned char initialized
        var hasPreferredX = false
        var singleLine = false
//        unsigned char padding1, padding2, padding3
        /** this determines where the cursor up/down tries to seek to along x */
        var preferredX = 0f
        val undoState = UndoState()

        val hasSelection get() = selectStart != selectEnd

        /** canoncialize the selection so start <= end */
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

        /** update selection and cursor to match each other */
        fun prepSelectionAtCursor() {
            if (!hasSelection) {
                selectEnd = cursor
                selectStart = cursor
            } else cursor = selectEnd
        }
    }


    ////////////////////////////////////////////////////////////////////////
    //
    //     StbTexteditRow
    //
    // Result of layout query, used by stb_textedit to determine where
    // the text in each row is.

    // result of layout query
    class Row {

        /** starting x location (allows for align=right, etc) */
        var x0 = 0f

        /** end x location (allows for align=right, etc) */
        var x1 = 0f

        /** position of baseline relative to previous row's baseline */
        var baselineYDelta = 0f

        /** height of row above baseline */
        var yMin = 0f

        /** height of row below baseline */
        var yMax = 0f
        var numChars = 0
    }

    /////////////////////////////////////////////////////////////////////////////
    //
    //      Mouse input handling
    //

    /** traverse the layout to locate the nearest character to a display position */
    fun InputTextState.textLocateCoord(x: Float, y: Float): Int {
        val r = Row()
        val n = stringLen
        var baseY = 0f
        var prevX = 0f
        var i = 0

        // search rows to find one that straddles 'y'
        while (i < n) {
            layoutRow(r, i)
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
                if (x < prevX + w)
                    return k + i + (x >= prevX + w / 2).i
                prevX += w
            }
            // shouldn't happen, but if it does, fall through to end-of-line case
        }

        // if the last character is a newline, return that. otherwise return 'after' the last character
        return i + r.numChars - if (getChar(i + r.numChars - 1) == NEWLINE) 1 else 0
    }

    /** API click: on mouse down, move the cursor to the clicked location, and reset the selection */
    fun InputTextState.click(x: Float, y_: Float) {
        var y = y_
        // In single-line mode, just always make y = 0. This lets the drag keep working if the mouse
        // goes off the top or bottom of the text
        if (stb.singleLine) {
            val r = Row()
            layoutRow(r, 0)
            y = r.yMin
        }

        stb.apply {
            cursor = locateCoord(x, y)
            selectStart = cursor
            selectEnd = cursor
            hasPreferredX = false
        }
    }

    /** API drag: on mouse drag, move the cursor and selection endpoint to the clicked location */
    fun InputTextState.drag(x: Float, y_: Float) {
        var y = y_
        // In single-line mode, just always make y = 0. This lets the drag keep working if the mouse
        // goes off the top or bottom of the text
        if (stb.singleLine) {
            val r = Row()
            layoutRow(r, 0)
            y = r.yMin
        }

        if (stb.selectStart == stb.selectEnd)
            stb.selectStart = stb.cursor

        val p = locateCoord(x, y)
        stb.selectEnd = p
        stb.cursor = p
    }

    /////////////////////////////////////////////////////////////////////////////
    //
    //      Keyboard input handling
    //

    class FindState {
        // position of n'th character
        var x = 0f
        var y = 0f

        /** height of line */
        var height = 0f

        /** first char of row */
        var firstChar = 0

        /** first char length */
        var length = 0

        /** first char of previous row */
        var prevFirst = 0
    }

    /** find the x/y location of a character, and remember info about the previous row in
     *  case we get a move-up event (for page up, we'll have to rescan) */
    fun InputTextState.findCharpos(find: FindState, n: Int, singleLine: Boolean) {
        val r = Row()
        var prevStart = 0
        val z = stringLen
        var i = 0

        if (n == z) {
            // if it's at the end, then find the last line -- simpler than trying to
            // explicitly handle this case in the regular code
            find.apply {
                if (singleLine) {
                    layoutRow(r, 0)
                    y = 0f
                    firstChar = 0
                    length = z
                    height = r.yMax - r.yMin
                    x = r.x1
                } else {
                    y = 0f
                    x = 0f
                    height = 1f
                    while (i < z) {
                        layoutRow(r, i)
                        prevStart = i
                        i += r.numChars
                    }
                    firstChar = i
                    length = 0
                    prevFirst = prevStart
                }
            }
            return
        }

        // search rows to find the one that straddles character n
        find.y = 0f

        while (true) {
            layoutRow(r, i)
            if (n < i + r.numChars)
                break
            prevStart = i
            i += r.numChars
            find.y += r.baselineYDelta
        }

        find.apply {
            firstChar = i
            val first = i
            length = r.numChars
            height = r.yMax - r.yMin
            prevFirst = prevStart

            // now scan to find xpos
            x = r.x0
            i = 0
            while (first + i < n)
                x += getWidth(first, i++)
        }
    }

    //#define STB_TEXT_HAS_SELECTION(s)   ((s)->select_start != (s)->select_end) [JVM] State class

    /** make the selection/cursor state valid if client altered the string */
    fun InputTextState.clamp() {
        val n = stringLen
        if (stb.hasSelection) {
            if (stb.selectStart > n) stb.selectStart = n
            if (stb.selectEnd > n) stb.selectEnd = n
            // if clamping forced them to be equal, move the cursor to match
            if (stb.selectStart == stb.selectEnd)
                stb.cursor = stb.selectStart
        }
        if (stb.cursor > n) stb.cursor = n
    }

    /** delete characters while updating undo */
    fun InputTextState.delete(where: Int, len: Int) {
        makeundoDelete(where, len)
        deleteChars(where, len)
        stb.hasPreferredX = false
    }

    /** delete the section */
    fun InputTextState.deleteSelection() {
        clamp()
        if (stb.hasSelection) {
            if (stb.selectStart < stb.selectEnd) {
                delete(stb.selectStart, stb.selectEnd - stb.selectStart)
                stb.cursor = stb.selectStart
                stb.selectEnd = stb.selectStart
            } else {
                delete(stb.selectEnd, stb.selectStart - stb.selectEnd)
                stb.cursor = stb.selectEnd
                stb.selectStart = stb.selectEnd
            }
            stb.hasPreferredX = false
        }
    }

    // stb_textedit_sortselection [JVM] -> State class
    // stb_textedit_move_to_first ''

    /** move cursor to last character of selection */
    fun InputTextState.moveToLast() {
        stb.apply {
            if (hasSelection) {
                sortSelection()
                clamp()
                cursor = selectEnd
                selectStart = selectEnd
                hasPreferredX = false
            }
        }
    }

    /*#ifdef STB_TEXTEDIT_IS_SPACE
static int is_word_boundary( STB_TEXTEDIT_STRING *str, int idx )
{
    return idx > 0 ? (STB_TEXTEDIT_IS_SPACE( STB_TEXTEDIT_GETCHAR(str,idx-1) ) && !STB_TEXTEDIT_IS_SPACE( STB_TEXTEDIT_GETCHAR(str, idx) ) ) : 1
}

#ifndef STB_TEXTEDIT_MOVEWORDLEFT
static int stb_textedit_move_to_word_previous( STB_TEXTEDIT_STRING *str, int c )
{
    --c // always move at least one character
    while( c >= 0 && !is_word_boundary( str, c ) )
        --c

    if( c < 0 )
        c = 0

    return c
}
#define STB_TEXTEDIT_MOVEWORDLEFT stb_textedit_move_to_word_previous
#endif

#ifndef STB_TEXTEDIT_MOVEWORDRIGHT
static int stb_textedit_move_to_word_next( STB_TEXTEDIT_STRING *str, int c )
{
    const int len = STB_TEXTEDIT_STRINGLEN(str)
    ++c // always move at least one character
    while( c < len && !is_word_boundary( str, c ) )
        ++c

    if( c > len )
        c = len

    return c
}
#define STB_TEXTEDIT_MOVEWORDRIGHT stb_textedit_move_to_word_next
#endif

#endif*/

    // stb_textedit_prep_selection_at_cursor [JVM] -> State class

    /** API cut: delete selection */
    fun InputTextState.cut(): Boolean {
        if (stb.hasSelection) {
            deleteSelection() // implicitly clamps
            stb.hasPreferredX = false
            return true
        }
        return false
    }

    /** API paste: replace existing selection with passed-in text */
    fun InputTextState.pasteInternal(text: CharArray): Boolean {
        // if there's a selection, the paste should delete it
        clamp()
        deleteSelection()
        // try to insert the characters
        if (insertChars(stb.cursor, text, 0, text.len)) {
            makeUndoInsert(stb.cursor, text.len)
            stb.cursor += text.len
            stb.hasPreferredX = false
            return true
        }
        // remove the undo since we didn't actually insert the characters
        if (stb.undoState.undoPoint != 0)
            --stb.undoState.undoPoint
        return false
    }

    /** API key: process a keyboard input */
    infix fun InputTextState.key(key: Int) = when (key) {
//                #ifdef STB_TEXTEDIT_K_INSERT
//                    case STB_TEXTEDIT_K_INSERT:
//                    state->insert_mode = !state->insert_mode
//                    break
//                #endif

        K.UNDO -> {
            undo()
            stb.hasPreferredX = false
        }

        K.REDO -> {
            redo()
            stb.hasPreferredX = false
        }

        K.LEFT -> {
            // if currently there's a selection, move cursor to start of selection
            if (stb.hasSelection)
                moveToFirst()
            else if (stb.cursor > 0)
                --stb.cursor
            stb.hasPreferredX = false
        }

        K.RIGHT -> {
            // if currently there's a selection, move cursor to end of selection
            if (stb.hasSelection)
                moveToLast()
            else
                ++stb.cursor
            clamp()
            stb.hasPreferredX = false
        }

        K.LEFT or K.SHIFT -> {
            clamp()
            prepSelectionAtCursor()
            // move selection left
            if (stb.selectEnd > 0)
                --stb.selectEnd
            stb.cursor = stb.selectEnd
            stb.hasPreferredX = false
        }

//            #ifdef STB_TEXTEDIT_MOVEWORDLEFT
        K.WORDLEFT -> {
            if (stb.hasSelection)
                moveToFirst()
            else {
                stb.cursor = moveWordLeft(stb.cursor)
                clamp()
            }
        }

        K.WORDLEFT or K.SHIFT -> {
            if (!stb.hasSelection)
                prepSelectionAtCursor()

            stb.cursor = moveWordLeft(stb.cursor)
            stb.selectEnd = stb.cursor

            clamp()
        }
//            #endif

//                #ifdef STB_TEXTEDIT_MOVEWORDRIGHT
        K.WORDRIGHT -> {
            if (stb.hasSelection)
                moveToLast()
            else {
                stb.cursor = moveWordRight(stb.cursor)
                clamp()
            }
        }

        K.WORDRIGHT or K.SHIFT -> {
            if (!stb.hasSelection)
                prepSelectionAtCursor()

            stb.cursor = moveWordRight(stb.cursor)
            stb.selectEnd = stb.cursor

            clamp()
        }
//                #endif

        K.RIGHT or K.SHIFT -> {
            prepSelectionAtCursor()
            // move selection right
            ++stb.selectEnd
            clamp()
            stb.cursor = stb.selectEnd
            stb.hasPreferredX = false
        }

        K.DOWN,
        K.DOWN or K.SHIFT -> {
            val find = FindState()
            val row = Row()
//                int i
            val sel = key has K.SHIFT

            if (stb.singleLine)
            // on windows, up&down in single-line behave like left&right
                key(K.RIGHT or (key and K.SHIFT))

            if (sel)
                prepSelectionAtCursor()
            else if (stb.hasSelection)
                moveToLast()

            // compute current position of cursor point
            clamp()
            findCharpos(find, stb.cursor, stb.singleLine)

            // now find character position down a row
            if (find.length != 0) {
                val goalX = if (stb.hasPreferredX) stb.preferredX else find.x
                val start = find.firstChar + find.length
                stb.cursor = start
                layoutRow(row, stb.cursor)
                var x = row.x0
                for (i in 0 until row.numChars) {
                    val dx = getWidth(start, i)
//                        #ifdef STB_TEXTEDIT_GETWIDTH_NEWLINE
                    if (dx == GETWIDTH_NEWLINE)
                        break
//                        #endif
                    x += dx
                    if (x > goalX)
                        break
                    ++stb.cursor
                }
                clamp()

                stb.hasPreferredX = true
                stb.preferredX = goalX

                if (sel)
                    stb.selectEnd = stb.cursor
            }
        }

        K.UP,
        K.UP or K.SHIFT -> {
            val find = FindState()
            val row = Row()
            val sel = key has K.SHIFT

            if (stb.singleLine)
            // on windows, up&down become left&right
                key(K.LEFT or (key and K.SHIFT))

            if (sel)
                prepSelectionAtCursor()
            else if (stb.hasSelection)
                moveToFirst()

            // compute current position of cursor point
            clamp()
            findCharpos(find, stb.cursor, stb.singleLine)

            // can only go up if there's a previous row
            if (find.prevFirst != find.firstChar) {
                // now find character position up a row
                val goalX = if (stb.hasPreferredX) stb.preferredX else find.x
                stb.cursor = find.prevFirst
                layoutRow(row, stb.cursor)
                var x = row.x0
                for (i in 0 until row.numChars) {
                    val dx = getWidth(find.prevFirst, i)
//                        #ifdef STB_TEXTEDIT_GETWIDTH_NEWLINE
                    if (dx == GETWIDTH_NEWLINE)
                        break
//                        #endif
                    x += dx
                    if (x > goalX)
                        break
                    ++stb.cursor
                }
                clamp()

                stb.hasPreferredX = true
                stb.preferredX = goalX

                if (sel)
                    stb.selectEnd = stb.cursor
            }
        }

        K.DELETE,
        K.DELETE or K.SHIFT -> {
            if (stb.hasSelection)
                deleteSelection()
            else {
                val n = stringLen
                if (stb.cursor < n)
                    delete(stb.cursor, 1)
            }
            stb.hasPreferredX = false
        }

        K.BACKSPACE,
        K.BACKSPACE or K.SHIFT -> {
            if (stb.hasSelection)
                deleteSelection()
            else {
                clamp()
                if (stb.cursor > 0) {
                    delete(stb.cursor - 1, 1)
                    --stb.cursor
                }
            }
            stb.hasPreferredX = false
        }

        K.TEXTSTART -> {
            stb.cursor = 0
            stb.selectStart = 0
            stb.selectEnd = 0
            stb.hasPreferredX = false
        }

        K.TEXTEND -> {
            stb.cursor = stringLen
            stb.selectStart 0
            stb.selectEnd = 0
            stb.hasPreferredX = false
        }

        K.TEXTSTART or K.SHIFT -> {
            prepSelectionAtCursor()
            stb.cursor = 0
            stb.selectEnd = 0
            stb.hasPreferredX = false
        }

        K.TEXTEND or K.SHIFT -> {
            prepSelectionAtCursor()
            stb.cursor = stringLen
            stb.selectEnd = stringLen
            stb.hasPreferredX = false
        }

        K.LINESTART -> {
            clamp()
            moveToFirst()
            if (stb.singleLine)
                stb.cursor = 0
            else while (stb.cursor > 0 && getChar(stb.cursor - 1) != NEWLINE)
                --stb.cursor
            stb.hasPreferredX = false
        }

        K.LINEEND -> {
            val n = stringLen
            clamp()
            moveToFirst()
            if (stb.singleLine)
                stb.cursor = n
            else while (stb.cursor < n && getChar(stb.cursor) != NEWLINE)
                ++stb.cursor
            stb.hasPreferredX = false
        }

        K.LINESTART or K.SHIFT -> {
            clamp()
            prepSelectionAtCursor()
            if (stb.singleLine)
                stb.cursor = 0
            else while (stb.cursor > 0 && getChar(stb.cursor - 1) != NEWLINE)
                --stb.cursor
            stb.selectEnd = stb.cursor
            stb.hasPreferredX = false
        }

        K.LINEEND or K.SHIFT -> {
            val n = stringLen
            clamp()
            prepSelectionAtCursor()
            if (stb.singleLine)
                stb.cursor = n
            else while (stb.cursor < n && getChar(stb.cursor) != NEWLINE)
                ++stb.cursor
            stb.selectEnd = stb.cursor
            stb.hasPreferredX = false
        }

        // @TODO:
        //    STB_TEXTEDIT_K_PGUP      - move cursor up a page
        //    STB_TEXTEDIT_K_PGDOWN    - move cursor down a page

        else -> {
            val c = keyToText(key)
            if (c > 0) {
                val ch = c.c

                // can't add newline in single-line mode
                if (ch != '\n' || !stb.singleLine)
                    if (stb.insertMode && !stb.hasSelection && stb.cursor < stringLen) {
                        makeUndoReplace(stb.cursor, 1, 1)
                        deleteChars(stb.cursor, 1)
                        if (insertChars(stb.cursor, ch)) {
                            ++stb.cursor
                            stb.hasPreferredX = true
                        }
                    } else {
                        deleteSelection() // implicitly clamps
                        if (insertChars(stb.cursor, ch)) {
                            makeUndoInsert(stb.cursor, 1)
                            ++stb.cursor
                            stb.hasPreferredX = false
                        }
                    }
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    //
    //      Undo processing
    //
    // @OPTIMIZE: the undo/redo buffer should be circular

    // stb_textedit_flush_redo [JVM] -> UndoState class

    // stb_textedit_discard_undo [JVM] -> UndoState class

//// discard the oldest entry in the redo list--it's bad if this
//// ever happens, but because undo & redo have to store the actual
//// characters in different cases, the redo character buffer can
//// fill up even though the undo buffer didn't
//static void stb_textedit_discard_redo(StbUndoState *state)
//{
//    int k = STB_TEXTEDIT_UNDOSTATECOUNT-1
//
//    if (state->redo_point <= k) {
//    // if the k'th undo state has characters, clean those up
//    if (state->undo_rec[k].char_storage >= 0) {
//    int n = state->undo_rec[k].insert_length, i
//    // move the remaining redo character data to the end of the buffer
//    state->redo_char_point += n
//    STB_TEXTEDIT_memmove(state->undo_char + state->redo_char_point, state->undo_char + state->redo_char_point-n, (size_t) ((STB_TEXTEDIT_UNDOCHARCOUNT - state->redo_char_point)*sizeof(STB_TEXTEDIT_CHARTYPE)))
//    // adjust the position of all the other records to account for above memmove
//    for (i=state->redo_point; i < k; ++i)
//    if (state->undo_rec[i].char_storage >= 0)
//    state->undo_rec[i].char_storage += n
//}
//    // now move all the redo records towards the end of the buffer; the first one is at 'redo_point'
//    // {DEAR IMGUI]
//    size_t move_size = (size_t)((STB_TEXTEDIT_UNDOSTATECOUNT - state->redo_point - 1) * sizeof(state->undo_rec[0]))
//    const char* buf_begin = (char*)state->undo_rec; (void)buf_begin
//    const char* buf_end   = (char*)state->undo_rec + sizeof(state->undo_rec); (void)buf_end
//    IM_ASSERT(((char*)(state->undo_rec + state->redo_point)) >= buf_begin)
//    IM_ASSERT(((char*)(state->undo_rec + state->redo_point + 1) + move_size) <= buf_end)
//    STB_TEXTEDIT_memmove(state->undo_rec + state->redo_point+1, state->undo_rec + state->redo_point, move_size)
//
//    // now move redo_point to point to the new one
//    ++state->redo_point
//}
//}
//
//static StbUndoRecord *stb_text_create_undo_record(StbUndoState *state, int numchars)
//{
//    // any time we create a new undo record, we discard redo
//    stb_textedit_flush_redo(state)
//
//    // if we have no free records, we have to make room, by sliding the
//    // existing records down
//    if (state->undo_point == STB_TEXTEDIT_UNDOSTATECOUNT)
//    stb_textedit_discard_undo(state)
//
//    // if the characters to store won't possibly fit in the buffer, we can't undo
//    if (numchars > STB_TEXTEDIT_UNDOCHARCOUNT) {
//        state->undo_point = 0
//        state->undo_char_point = 0
//        return NULL
//    }
//
//    // if we don't have enough free characters in the buffer, we have to make room
//    while (state->undo_char_point + numchars > STB_TEXTEDIT_UNDOCHARCOUNT)
//    stb_textedit_discard_undo(state)
//
//    return &state->undo_rec[state->undo_point++]
//}
//
//static STB_TEXTEDIT_CHARTYPE *stb_text_createundo(StbUndoState *state, int pos, int insert_len, int delete_len)
//{
//    StbUndoRecord *r = stb_text_create_undo_record(state, insert_len)
//    if (r == NULL)
//        return NULL
//
//    r->where = pos
//    r->insert_length = (STB_TEXTEDIT_POSITIONTYPE) insert_len
//    r->delete_length = (STB_TEXTEDIT_POSITIONTYPE) delete_len
//
//    if (insert_len == 0) {
//        r->char_storage = -1
//        return NULL
//    } else {
//        r->char_storage = state->undo_char_point
//        state->undo_char_point += insert_len
//        return &state->undo_char[r->char_storage]
//    }
//}
//
//static void stb_text_undo(STB_TEXTEDIT_STRING *str, STB_TexteditState *state)
//{
//    StbUndoState *s = &state->undostate
//    StbUndoRecord u, *r
//    if (s->undo_point == 0)
//    return
//
//    // we need to do two things: apply the undo record, and create a redo record
//    u = s->undo_rec[s->undo_point-1]
//    r = &s->undo_rec[s->redo_point-1]
//    r->char_storage = -1
//
//    r->insert_length = u.delete_length
//    r->delete_length = u.insert_length
//    r->where = u.where
//
//    if (u.delete_length) {
//        // if the undo record says to delete characters, then the redo record will
//        // need to re-insert the characters that get deleted, so we need to store
//        // them.
//
//        // there are three cases:
//        //    there's enough room to store the characters
//        //    characters stored for *redoing* don't leave room for redo
//        //    characters stored for *undoing* don't leave room for redo
//        // if the last is true, we have to bail
//
//        if (s->undo_char_point + u.delete_length >= STB_TEXTEDIT_UNDOCHARCOUNT) {
//            // the undo records take up too much character space; there's no space to store the redo characters
//            r->insert_length = 0
//        } else {
//            int i
//
//            // there's definitely room to store the characters eventually
//            while (s->undo_char_point + u.delete_length > s->redo_char_point) {
//            // should never happen:
//            if (s->redo_point == STB_TEXTEDIT_UNDOSTATECOUNT)
//            return
//            // there's currently not enough room, so discard a redo record
//            stb_textedit_discard_redo(s)
//        }
//            r = &s->undo_rec[s->redo_point-1]
//
//            r->char_storage = s->redo_char_point - u.delete_length
//            s->redo_char_point = s->redo_char_point - u.delete_length
//
//            // now save the characters
//            for (i=0; i < u.delete_length; ++i)
//            s->undo_char[r->char_storage + i] = STB_TEXTEDIT_GETCHAR(str, u.where + i)
//        }
//
//        // now we can carry out the deletion
//        STB_TEXTEDIT_DELETECHARS(str, u.where, u.delete_length)
//    }
//
//    // check type of recorded action:
//    if (u.insert_length) {
//        // easy case: was a deletion, so we need to insert n characters
//        STB_TEXTEDIT_INSERTCHARS(str, u.where, &s->undo_char[u.char_storage], u.insert_length)
//        s->undo_char_point -= u.insert_length
//    }
//
//    state->cursor = u.where + u.insert_length
//
//    s->undo_point--
//    s->redo_point--
//}
//
//static void stb_text_redo(STB_TEXTEDIT_STRING *str, STB_TexteditState *state)
//{
//    StbUndoState *s = &state->undostate
//    StbUndoRecord *u, r
//    if (s->redo_point == STB_TEXTEDIT_UNDOSTATECOUNT)
//    return
//
//    // we need to do two things: apply the redo record, and create an undo record
//    u = &s->undo_rec[s->undo_point]
//    r = s->undo_rec[s->redo_point]
//
//    // we KNOW there must be room for the undo record, because the redo record
//    // was derived from an undo record
//
//    u->delete_length = r.insert_length
//    u->insert_length = r.delete_length
//    u->where = r.where
//    u->char_storage = -1
//
//    if (r.delete_length) {
//        // the redo record requires us to delete characters, so the undo record
//        // needs to store the characters
//
//        if (s->undo_char_point + u->insert_length > s->redo_char_point) {
//            u->insert_length = 0
//            u->delete_length = 0
//        } else {
//            int i
//            u->char_storage = s->undo_char_point
//            s->undo_char_point = s->undo_char_point + u->insert_length
//
//            // now save the characters
//            for (i=0; i < u->insert_length; ++i)
//            s->undo_char[u->char_storage + i] = STB_TEXTEDIT_GETCHAR(str, u->where + i)
//        }
//
//        STB_TEXTEDIT_DELETECHARS(str, r.where, r.delete_length)
//    }
//
//    if (r.insert_length) {
//        // easy case: need to insert n characters
//        STB_TEXTEDIT_INSERTCHARS(str, r.where, &s->undo_char[r.char_storage], r.insert_length)
//        s->redo_char_point += r.insert_length
//    }
//
//    state->cursor = r.where + r.insert_length
//
//    s->undo_point++
//    s->redo_point++
//}
//
//static void stb_text_makeundo_insert(STB_TexteditState *state, int where, int length)
//{
//    stb_text_createundo(&state->undostate, where, 0, length)
//}
//
//static void stb_text_makeundo_delete(STB_TEXTEDIT_STRING *str, STB_TexteditState *state, int where, int length)
//{
//    int i
//    STB_TEXTEDIT_CHARTYPE *p = stb_text_createundo(&state->undostate, where, length, 0)
//    if (p) {
//        for (i=0; i < length; ++i)
//        p[i] = STB_TEXTEDIT_GETCHAR(str, where+i)
//    }
//}
//
//static void stb_text_makeundo_replace(STB_TEXTEDIT_STRING *str, STB_TexteditState *state, int where, int old_length, int new_length)
//{
//    int i
//    STB_TEXTEDIT_CHARTYPE *p = stb_text_createundo(&state->undostate, where, old_length, new_length)
//    if (p) {
//        for (i=0; i < old_length; ++i)
//        p[i] = STB_TEXTEDIT_GETCHAR(str, where+i)
//    }
//}
//
//// reset the state to default
//static void stb_textedit_clear_state(STB_TexteditState *state, int is_single_line)
//{
//    state->undostate.undo_point = 0
//    state->undostate.undo_char_point = 0
//    state->undostate.redo_point = STB_TEXTEDIT_UNDOSTATECOUNT
//    state->undostate.redo_char_point = STB_TEXTEDIT_UNDOCHARCOUNT
//    state->select_end = state->select_start = 0
//    state->cursor = 0
//    state->has_preferred_x = 0
//    state->preferred_x = 0
//    state->cursor_at_end_of_line = 0
//    state->initialized = 1
//    state->single_line = (unsigned char) is_single_line
//    state->insert_mode = 0
//}
//
//// API initialize
//static void stb_textedit_initialize_state(STB_TexteditState *state, int is_single_line)
//{
//    stb_textedit_clear_state(state, is_single_line)
//}
//
//#if defined(__GNUC__) || defined(__clang__)
//#pragma GCC diagnostic push
//#pragma GCC diagnostic ignored "-Wcast-qual"
//#endif
//
//static int stb_textedit_paste(STB_TEXTEDIT_STRING *str, STB_TexteditState *state, STB_TEXTEDIT_CHARTYPE const *ctext, int len)
//{
//    return stb_textedit_paste_internal(str, state, (STB_TEXTEDIT_CHARTYPE *) ctext, len)
//}
//
//#if defined(__GNUC__) || defined(__clang__)
//#pragma GCC diagnostic pop
//#endif
//
//#endif//STB_TEXTEDIT_IMPLEMENTATION

}

/*
------------------------------------------------------------------------------
This software is available under 2 licenses -- choose whichever you prefer.
------------------------------------------------------------------------------
ALTERNATIVE A - MIT License
Copyright (c) 2017 Sean Barrett
Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
------------------------------------------------------------------------------
ALTERNATIVE B - Public Domain (www.unlicense.org)
This is free and unencumbered software released into the public domain.
Anyone is free to copy, modify, publish, use, compile, sell, or distribute this
software, either in source code form or as a compiled binary, for any purpose,
commercial or non-commercial, and by any means.
In jurisdictions that recognize copyright laws, the author or authors of this
software dedicate any and all copyright interest in the software to the public
domain. We make this dedication for the benefit of the public at large and to
the detriment of our heirs and successors. We intend this dedication to be an
overt act of relinquishment in perpetuity of all present and future rights to
this software under copyright law.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
------------------------------------------------------------------------------
*/
