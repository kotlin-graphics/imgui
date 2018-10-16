package imgui

import glm_.bool
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.ImGui.calcListClipping
import imgui.ImGui.colorConvertHSVtoRGB
import imgui.ImGui.currentWindow
import imgui.ImGui.currentWindowRead
import imgui.ImGui.cursorPosY
import imgui.ImGui.inputText
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushItemWidth
import imgui.ImGui.style
import imgui.internal.strlen
import java.nio.ByteBuffer
import kotlin.math.max

/** Helper: Execute a block of code at maximum once a frame. Convenient if you want to quickly create an UI within
 *  deep-nested code that runs multiple times every frame.
 *  Usage: static ImGuiOnceUponAFrame oaf; if (oaf) ImGui::Text("This will be called only once per frame"); */
class OnceUponAFrame {
    init {
        TODO()
    }
}

class TextFilter(defaultFilter: String? = "") {

    val inputBuf = CharArray(256)
    val filters = ArrayList<String>()
    var countGrep = 0

    init {
        defaultFilter?.toCharArray(inputBuf)
    }

    class TextRange

    /** Helper calling InputText+Build   */
    fun draw(label: String = "Filter (inc,-exc)", width: Float): Boolean {
        if (width != 0f)
            pushItemWidth(width)
        val valueChanged = inputText(label, inputBuf)
        if (width != 0f)
            popItemWidth()
//        if (valueChanged)
//            Build()
        return valueChanged
    }

    fun passFilter(text: String, textEnd: Int = 0): Boolean {

        if (filters.isEmpty()) return true

        for (f in filters) {
            if (f.isEmpty()) continue
            if (f[0] == '-') {
                // Subtract
                if (text.contains(f))
                    return false
            } else if (text.contains(f))   // Grep
                return true
        }
        // Implicit * grep
        return countGrep == 0
    }

//    void ImGuiTextFilter::Build()
//    {
//        Filters.resize(0);
//        TextRange input_range(InputBuf, InputBuf+strlen(InputBuf));
//        input_range.split(',', &Filters);
//
//        CountGrep = 0;
//        for (int i = 0; i != Filters.Size; i++)
//        {
//            TextRange& f = Filters[i];
//            while (f.b < f.e && ImCharIsBlankA(f.b[0]))
//                f.b++;
//            while (f.e > f.b && ImCharIsBlankA(f.e[-1]))
//                f.e--;
//            if (f.empty())
//                continue;
//            if (Filters[i].b[0] != '-')
//                CountGrep += 1;
//        }
//    }
}

class TextBuffer {
    init {
        TODO()
    }
}

/** Helper: Key->value storage
Typically you don't have to worry about this since a storage is held within each Window.
We use it to e.g. store collapse state for a tree (Int 0/1)
This is optimized for efficient lookup (dichotomy into a contiguous buffer) and rare insertion (typically tied to user
interactions aka max once a frame)
Declare your own storage if:
- You want to manipulate the open/close state of a particular sub-tree in your interface (tree node uses Int 0/1
to store their state).
- You want to store custom debug data easily without adding or editing structures in your code.
Types are NOT stored, so it is up to you to make sure your Key don't collide with different types.  */
class Storage {

    val data = HashMap<ID, Int>()

    // - Get***() functions find pair, never add/allocate. Pairs are sorted so a query is O(log N)
    // - Set***() functions find pair, insertion on demand if missing.
    // - Sorted insertion is costly, paid once. A typical frame shouldn't need to insert any new pair.

    fun clear() = data.clear()

    fun int(key: ID, defaultVal: Int = 0): Int = data[key] ?: defaultVal
    operator fun set(key: ID, value: Int) = data.set(key, value)

    fun bool(key: ID, defaultVal: Boolean = false) = data[key]?.bool ?: defaultVal
    operator fun set(id: ID, value: Boolean) = data.set(id, value.i)

    fun float(key: ID, defaultVal: Float = 0f) = data[key]?.let { glm.intBitsToFloat(it) } ?: defaultVal
    operator fun set(key: ID, value: Float) = data.set(key, glm.floatBitsToInt(value))

//    IMGUI_API void*     GetVoidPtr(ImGuiID key) const; // default_val is NULL
//    IMGUI_API void      SetVoidPtr(ImGuiID key, void* val);

    // - Get***Ref() functions finds pair, insert on demand if missing, return pointer. Useful if you intend to do Get+Set.
    // - References are only valid until a new value is added to the storage. Calling a Set***() function or a Get***Ref() function invalidates the pointer.
    // - A typical use case where this is convenient for quick hacking (e.g. add storage during a live Edit&Continue session if you can't modify existing struct)
    //      float* pvar = ImGui::GetFloatRef(key); ImGui::SliderFloat("var", pvar, 0, 100.0f); some_var += *pvar;
//    IMGUI_API int*      GetIntRef(ImGuiID key, int default_val = 0);
//    IMGUI_API bool*     GetBoolRef(ImGuiID key, bool default_val = false);
//    IMGUI_API float*    GetFloatRef(ImGuiID key, float default_val = 0.0f);
//    IMGUI_API void**    GetVoidPtrRef(ImGuiID key, void* default_val = NULL);

    /** Use on your own storage if you know only integer are being stored (open/close all tree nodes)   */
    fun setAllInt(value: Int) = data.replaceAll { _, _ -> value }
}

/** Shared state of InputText(), passed as an argument to your callback when a ImGuiInputTextFlags_Callback* flag is used.
 *  The callback function should return 0 by default.
 *  Special processing:
 *  - InputTextFlag.CallbackCharFilter:  return 1 if the character is not allowed. You may also set 'EventChar=0'
 *                                       as any character replacement are allowed.
 *  - InputTextFlag.CallbackResize:      notified by InputText() when the string is resized.
 *                                       BufTextLen is set to the new desired string length so you can allocate or update known size.
 *                                       No need to initialize new characters or zero-terminator as InputText will do it.
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

    /** Character input                     Read-write   [CharFilter] Replace character or set to zero. return 1 is equivalent to setting EventChar=0; */
    var eventChar = NUL
    /** Key pressed (Up/Down/TAB)           Read-only    [Completion,History] */
    var eventKey = Key.Tab
    /** Text buffer                 Read-write   [Resize] Can replace pointer / [Completion,History,Always] Only write to pointed data, don't replace the actual pointer! */
    var buf = CharArray(0)
    /** JVM custom, current buf pointer */
    var bufPtr = 0
    /** Text length in bytes        Read-write   [Resize,Completion,History,Always] */
    var bufTextLen = 0
    /** Buffer capacity in bytes    Read-only    [Resize,Completion,History,Always] */
    var bufSize = 0
    /** Set if you modify Buf/BufTextLen!!  Write        [Completion,History,Always] */
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
        while (c != NUL) {
            buf[dst++] = c
            c = buf.getOrElse(src++) { NUL }
        }
        if (cursorPos + bytesCount >= pos)
            cursorPos -= bytesCount
        else if (cursorPos >= pos)
            cursorPos = pos
        selectionEnd = cursorPos
        selectionStart = cursorPos
        bufDirty = true
        bufTextLen -= bytesCount
    }

    fun insertChars(pos: Int, newText: CharArray, ptr: Int, newTextEnd: Int? = null) {

        val isResizable = flags has InputTextFlag.CallbackResize
        val newTextLen = newTextEnd ?: newText.strlen
        if (newTextLen + bufTextLen >= bufSize) {

            if (!isResizable) return

            // Contrary to STB_TEXTEDIT_INSERTCHARS() this is working in the UTF8 buffer, hence the midly similar code (until we remove the U16 buffer alltogether!)
            val editState = g.inputTextState
            assert(editState.id != 0 && g.activeId == editState.id)
            assert(buf === editState.tempBuffer)
            val newBufSize = bufTextLen + glm.clamp(newTextLen * 4, 32, max(256, newTextLen))
            val t = CharArray(newBufSize)
            System.arraycopy(editState.tempBuffer, 0, t, 0, editState.tempBuffer.size)
            editState.tempBuffer = t
            buf = editState.tempBuffer
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

    val hasSelection: Boolean
        get() = selectionStart != selectionEnd
}

class SizeCallbackData(
        /** Read-only.   What user passed to SetNextWindowSizeConstraints() */
        var userData: Any? = null,
        /** Read-only.   Window position, for reference.    */
        val pos: Vec2 = Vec2(),
        /** Read-only.   Current window size.   */
        val currentSize: Vec2 = Vec2(),
        /** Read-write.  Desired size, based on user's mouse position. Write to this field to restrain resizing.    */
        val desiredSize: Vec2 = Vec2())

/** Data payload for Drag and Drop operations */
class Payload {
    // Members

    /** Data (copied and owned by dear imgui) */
    var data: ByteBuffer? = null
    /** Data size */
    var dataSize = 0

    // [Internal]

    /** Source item id */
    var sourceId: ID = 0
    /** Source parent id (if available) */
    var sourceParentId: ID = 0
    /** Data timestamp */
    var dataFrameCount = -1
    /** Data type tag (short user-supplied string, 32 characters max) */
    val dataType = CharArray(32)
    val dataTypeS get() = String(dataType)
    /** Set when AcceptDragDropPayload() was called and mouse has been hovering the target item (nb: handle overlapping drag targets) */
    var preview = false
    /** Set when AcceptDragDropPayload() was called and mouse button is released over the target item. */
    var delivery = false

    fun clear() {
        sourceParentId = 0
        sourceId = 0
        data = null
        dataSize = 0
        dataType.fill(NUL)
        dataFrameCount = -1
        delivery = false
        preview = false
    }

    fun isDataType(type: String) = dataFrameCount != -1 && type cmp dataType
}

class Color {

    val value = Vec4()

    constructor(r: Int, g: Int, b: Int, a: Int = 255) {
        val sc = 1f / 255f
        value.x = r * sc
        value.y = g * sc
        value.z = b * sc
        value.w = a * sc
    }

    constructor(rgba: Int) {
        val sc = 1f / 255f
        value.x = ((rgba ushr COL32_R_SHIFT) and 0xFF) * sc
        value.y = ((rgba ushr COL32_G_SHIFT) and 0xFF) * sc
        value.z = ((rgba ushr COL32_B_SHIFT) and 0xFF) * sc
        value.w = ((rgba ushr COL32_A_SHIFT) and 0xFF) * sc
    }

    constructor(r: Float, g: Float, b: Float, a: Float = 1f) {
        value.x = r
        value.y = g
        value.z = b
        value.w = a
    }

    constructor(col: Vec4) {
        value put col
    }

    companion object {
        fun hsv(h: Float, s: Float, v: Float, a: Float = 1f): Vec4 {
            val (r, g, b) = colorConvertHSVtoRGB(h, s, v)
            return Color(r, g, b, a).value
        }
    }
}

/** Helper: Manually clip large list of items.
 *  If you are submitting lots of evenly spaced items and you have a random access to the list, you can perform coarse
 *  clipping based on visibility to save yourself from processing those items at all.
 *  The clipper calculates the range of visible items and advance the cursor to compensate for the non-visible items we
 *  have skipped.
 *  ImGui already clip items based on their bounds but it needs to measure text size to do so. Coarse clipping before
 *  submission makes this cost and your own data fetching/submission cost null.
 *  Usage:
 *      ImGuiListClipper clipper(1000);  // we have 1000 elements, evenly spaced.
 *      while (clipper.Step())
 *          for (int i = clipper.DisplayStart; i < clipper.DisplayEnd; i++)
 *              ImGui::Text("line number %d", i);
 *  - Step 0: the clipper let you process the first element, regardless of it being visible or not, so we can measure
 *      the element height (step skipped if we passed a known height as second arg to constructor).
 *  - Step 1: the clipper infer height from first element, calculate the actual range of elements to display, and
 *      position the cursor before the first element.
 *  - (Step 2: dummy step only required if an explicit items_height was passed to constructor or Begin() and user call
 *      Step(). Does nothing and switch to Step 3.)
 *  - Step 3: the clipper validate that we have reached the expected Y position (corresponding to element DisplayEnd),
 *      advance the cursor to the end of the list and then returns 'false' to end the loop. */
class ListClipper
/** @param itemsCount:  Use -1 to ignore (you can call begin() later). Use Int.MAX_VALUE if you don't know how many
 *  items you have (in which case the cursor won't be advanced in the final step).
 *  @param itemsHeight: Use -1f to be calculated automatically on first step. Otherwise pass in the distance
 *  between your items, typically textLineHeightWithSpacing or frameHeightWithSpacing.
 *  If you don't specify an items_height, you NEED to call step(). If you specify itemsHeight you may call the old
 *  begin()/end() api directly, but prefer calling step().   */
constructor(itemsCount: Int = -1, itemsHeight: Float = -1f) {

    var startPosY = 0f
    var itemsHeight = 0f
    var itemsCount = 0
    var stepNo = 0
    var display = 0..0

    init {
        /* NB: Begin() initialize every fields (as we allow user to call Begin/End multiple times on a same instance if they want). */
        begin(itemsCount, itemsHeight)
    }

    /** Call until it returns false. The DisplayStart/DisplayEnd fields will be set and you can process/draw those
     *  items.  */
    fun step() = when {

        itemsCount == 0 || currentWindowRead!!.skipItems -> {
            itemsCount = -1
            false
        }
        /*  Step 0: the clipper let you process the first element, regardless of it being visible or not, so we can measure
            the element height.     */
        stepNo == 0 -> {
            display = 0..1
            startPosY = cursorPosY
            stepNo = 1
            true
        }
        /*  Step 1: the clipper infer height from first element, calculate the actual range of elements to display, and
            position the cursor before the first element.     */
        stepNo == 1 -> {
            if (itemsCount == 1) {
                itemsCount = -1
                false
            } else {
                val itemsHeight = cursorPosY - startPosY
                assert(itemsHeight > 0f) { "If this triggers, it means Item 0 hasn't moved the cursor vertically" }
                begin(itemsCount - 1, itemsHeight)
                display = display.start + 1..display.last + 1
                stepNo = 3
                true
            }
        }
        /*  Step 2: dummy step only required if an explicit items_height was passed to constructor or Begin() and user still
            call Step(). Does nothing and switch to Step 3.     */
        stepNo == 2 -> {
            assert(display.start >= 0 && display.last >= 0)
            stepNo = 3
            true
        }
        else -> {
            /*  Step 3: the clipper validate that we have reached the expected Y position (corresponding to element
                DisplayEnd), advance the cursor to the end of the list and then returns 'false' to end the loop.             */
            if (stepNo == 3)
                end()
            false
        }
    }

    /** Automatically called by constructor if you passed 'items_count' or by Step() in Step 1.
     *  Use case A: Begin() called from constructor with items_height<0, then called again from Sync() in StepNo 1
     *  Use case B: Begin() called from constructor with items_height>0
     *  FIXME-LEGACY: Ideally we should remove the Begin/End functions but they are part of the legacy API we still
     *  support. This is why some of the code in Step() calling Begin() and reassign some fields, spaghetti style.
     */
    fun begin(itemsCount: Int = -1, itemsHeight: Float = -1f) {

        startPosY = cursorPosY
        this.itemsHeight = itemsHeight
        this.itemsCount = itemsCount
        stepNo = 0
        display = -1..-1
        if (itemsHeight > 0f) {
            display = calcListClipping(itemsCount, itemsHeight) // calculate how many to clip/display
            if (display.start > 0)
                setCursorPosYAndSetupDummyPrevLine(startPosY + display.start * itemsHeight, itemsHeight) // advance cursor
            stepNo = 2
        }
    }

    /** Automatically called on the last call of Step() that returns false. */
    fun end() {

        if (itemsCount < 0) return
        /*  In theory here we should assert that ImGui::GetCursorPosY() == StartPosY + DisplayEnd * ItemsHeight,
            but it feels saner to just seek at the end and not assert/crash the user.         */
        if (itemsCount < Int.MAX_VALUE)
            setCursorPosYAndSetupDummyPrevLine(startPosY + itemsCount * itemsHeight, itemsHeight) // advance cursor
        itemsCount = -1
        stepNo = 3
    }

    companion object {

        fun setCursorPosYAndSetupDummyPrevLine(posY: Float, lineHeight: Float) {
            /*  Set cursor position and a few other things so that SetScrollHere() and Columns() can work when seeking
                cursor.
                FIXME: It is problematic that we have to do that here, because custom/equivalent end-user code would
                stumble on the same issue.
                The clipper should probably have a 4th step to display the last item in a regular manner.   */
            cursorPosY = posY
            val window = currentWindow
            with(window.dc) {
                // Setting those fields so that SetScrollHere() can properly function after the end of our clipper usage.
                cursorPosPrevLine.y = cursorPos.y - lineHeight
                /*  If we end up needing more accurate data (to e.g. use SameLine) we may as well make the clipper have a
                    fourth step to let user process and display the last item in their list.             */
                prevLineSize.y = lineHeight - style.itemSpacing.y
                columnsSet?.lineMinY = window.dc.cursorPos.y // Setting this so that cell Y position are set properly
            }
        }
    }
}