package imgui.classes

import glm_.b
import glm_.glm
import imgui.*
import imgui.api.g
import uno.kotlin.NUL
import kotlin.math.max

/** Shared state of InputText(), passed as an argument to your callback when a ImGuiInputTextFlags_Callback* flag is used.
 *  The callback function should return 0 by default.
 *  Callbacks (follow a flag name and see comments in ImGuiInputTextFlags_ declarations for more details)
 *  - ImGuiInputTextFlags_CallbackCompletion:  Callback on pressing TAB
 *  - ImGuiInputTextFlags_CallbackHistory:     Callback on pressing Up/Down arrows
 *  - ImGuiInputTextFlags_CallbackAlways:      Callback on each iteration
 *  - ImGuiInputTextFlags_CallbackCharFilter:  Callback on character inputs to replace or discard them.
 *                                              Modify 'EventChar' to replace or discard, or return 1 in callback to discard.
 *  - ImGuiInputTextFlags_CallbackResize:      Callback on buffer capacity changes request (beyond 'buf_size' parameter value),
 *                                              allowing the string to grow.
 *
 *  Helper functions for text manipulation.
 *  Use those function to benefit from the CallbackResize behaviors. Calling those function reset the selection. */
class InputTextCallbackData {

    /** One ImGuiInputTextFlags_Callback*    // Read-only */
    var eventFlag: InputTextFlags = 0

    /** What user passed to InputText()      // Read-only */
    var flags: InputTextFlags = 0

    /** What user passed to InputText()      // Read-only */
    var userData: Any? = null

    /*  Arguments for the different callback events
     *  - To modify the text buffer in a callback, prefer using the InsertChars() / DeleteChars() function. InsertChars() will take care of calling the resize callback if necessary.
     *  - If you know your edits are not going to resize the underlying buffer allocation, you may modify the contents of 'Buf[]' directly. You need to update 'BufTextLen' accordingly (0 <= BufTextLen < BufSize) and set 'BufDirty'' to true so InputText can update its internal state. */

    /** Character input                     Read-write   [CharFilter] Replace character with another one, or set to zero to drop.
     *                                      return 1 is equivalent to setting EventChar=0; */
    var eventChar = NUL

    /** Key pressed (Up/Down/TAB)           Read-only    [Completion,History] */
    var eventKey = Key.Tab

    /** Text buffer                 Read-write   [Resize] Can replace pointer / [Completion,History,Always] Only write to pointed data, don't replace the actual pointer! */
    var buf = ByteArray(0)

    /** [JVM], current buf pointer */
    var bufPtr = 0

    /** Text length (in bytes)        Read-write   [Resize,Completion,History,Always] */
    var bufTextLen = 0

    /** Buffer size (in bytes) = capacity + 1    Read-only    [Resize,Completion,History,Always] */
    var bufSize = 0

    /** Set if you modify Buf/BufTextLen!  Write        [Completion,History,Always] */
    var bufDirty = false

    /** Read-write   [Completion,History,Always] */
    var cursorPos = 0

    /** Read-write   [Completion,History,Always] == to SelectionEnd when no selection) */
    var selectionStart = 0

    /** Read-write   [Completion,History,Always] */
    var selectionEnd = 0


    /** Public API to manipulate UTF-8 text
     *  We expose UTF-8 to the user (unlike the STB_TEXTEDIT_* functions which are manipulating wchar)
     *  FIXME: The existence of this rarely exercised code path is a bit of a nuisance. */
    fun deleteChars(pos: Int, bytesCount: Int) {
        assert(pos + bytesCount <= bufTextLen)
        var dst = pos
        var src = pos + bytesCount
        var c = buf[src++]
        while (c != 0.b) {
            buf[dst++] = c
            c = buf.getOrElse(src++) { 0.b }
        }
        if (cursorPos >= pos + bytesCount)
            cursorPos -= bytesCount
        else if (cursorPos >= pos)
            cursorPos = pos
        selectionEnd = cursorPos
        selectionStart = cursorPos
        bufDirty = true
        bufTextLen -= bytesCount
    }

    fun insertChars(pos: Int, newText: String) = insertChars(pos, newText.toByteArray())

    fun insertChars(pos: Int, newText: ByteArray, newTextEnd: Int = -1) {

        val isResizable = flags has InputTextFlag.CallbackResize
        val newTextLen = if (newTextEnd != -1) newTextEnd else newText.strlen()
        if (newTextLen + bufTextLen >= bufSize) {

            if (!isResizable) return

            // Contrary to STB_TEXTEDIT_INSERTCHARS() this is working in the UTF8 buffer, hence the mildly similar code (until we remove the U16 buffer altogether!)
            val editState = g.inputTextState
            assert(editState.id != 0 && g.activeId == editState.id)
            assert(buf === editState.textA)
            val newBufSize = bufTextLen + glm.clamp(newTextLen * 4, 32, max(256, newTextLen))
            val new = ByteArray(newBufSize)
            System.arraycopy(editState.textA, 0, new, 0, editState.textA.size)
            editState.textA = new
            buf = editState.textA
            editState.bufCapacityA = newBufSize
            bufSize = newBufSize
        }

        if (bufTextLen != pos)
            for (i in 0 until bufTextLen - pos)
                buf[pos + newTextLen + i] = buf[pos + i]
        for (i in 0 until newTextLen)
            buf[pos + i] = newText[i]

        if (cursorPos >= pos)
            cursorPos += newTextLen
        selectionEnd = cursorPos
        selectionStart = cursorPos
        bufDirty = true
        bufTextLen += newTextLen
    }

    fun selectAll() {
        selectionStart = 0
        selectionEnd = bufTextLen
    }

    fun clearSelection() {
        selectionEnd = bufTextLen
        selectionStart = selectionEnd
    }

    val hasSelection: Boolean
        get() = selectionStart != selectionEnd
}