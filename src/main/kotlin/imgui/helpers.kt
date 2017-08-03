package imgui

import glm_.bool
import glm_.glm
import glm_.i
import glm_.vec4.Vec4
import imgui.ImGui.calcListClipping
import imgui.ImGui.currentWindow
import imgui.ImGui.currentWindowRead
import imgui.ImGui.cursorPosY
import imgui.ImGui.end
import imgui.ImGui.inputText
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushItemWidth

class OnceUponAFrame {
    init {
        TODO()
    }
}

class TextFilter(defaultFilter: String? = "") {

    val inputBuf = CharArray(256)
    val countGrep = 0

    init {
        defaultFilter?.let {
            defaultFilter.toCharArray(inputBuf)
        }
    }

    class TextRange

    fun draw(label: String = "Filter (inc,-exc)", width: Float): Boolean {
        if (width != 0f)
            pushItemWidth(width)
        val valueChanged = inputText(label, inputBuf)
        if (width != 0.0f)
            popItemWidth()
//        if (valueChanged)
//            Build()
        return valueChanged
    }
}

class TextBuffer {
    init {
        TODO()
    }
}

/** Helper: Simple Key->value storage
Typically you don't have to worry about this since a storage is held within each Window.
We use it to e.g. store collapse state for a tree (Int 0/1), store color edit options.
You can use it as custom user storage for temporary values.
Declare your own storage if:
- You want to manipulate the open/close state of a particular sub-tree in your interface (tree node uses Int 0/1
to store their state).
- You want to store custom debug data easily without adding or editing structures in your code.
Types are NOT stored, so it is up to you to make sure your Key don't collide with different types.  */
class Storage {

    val data = HashMap<Int, Int>()

    // - Get***() functions find pair, never add/allocate. Pairs are sorted so a query is O(log N)
    // - Set***() functions find pair, insertion on demand if missing.
    // - Sorted insertion is costly, paid once. A typical frame shouldn't need to insert any new pair.

    fun clear() = data.clear()

    fun intABaaaaaaaaaaaaaaaaaaaaaasassaa(key: Int, defaultVal: Int = 0) = data[key] ?: defaultVal // TODO rename back

    operator fun set(key: Int, value: Int) {
        data[key] = value
    }

    fun bool(key: Int, defaultVal: Boolean = false) = data[key]?.bool ?: defaultVal
    operator fun set(id: Int, value: Boolean) {
        data[id] = value.i
    }

    fun float(key: Int, defaultVal: Float = 0f) = data[key]?.let { glm.intBitsToFloat(it) } ?: defaultVal
    operator fun set(key: Int, value: Float) {
        data[key] = glm.floatBitsToInt(value)
    }
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

class TextEditCallbackData {
    init {
        TODO()
    }
}

class SizeConstraintCallbackData {
    init {
        TODO()
    }
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
class ListClipper {

    /** @param[itemsCount]:  Use -1 to ignore (you can call Begin later). Use INT_MAX if you don't know how many items
     *  you have (in which case the cursor won't be advanced in the final step).
     *  @param[itemsHeight]: Use -1.0f to be calculated automatically on first step. Otherwise pass in the distance
     *  between your items, typically GetTextLineHeightWithSpacing() or GetItemsLineHeightWithSpacing().
     *  If you don't specify an items_height, you NEED to call Step(). If you specify items_height you may call the old
     *  Begin()/End() api directly, but prefer calling Step().   */
    constructor(itemsCount: Int = -1, itemsHeight: Float = -1f) {
        /* NB: Begin() initialize every fields (as we allow user to call Begin/End multiple times on a same instance if
            they want).         */
        begin(itemsCount, itemsHeight)
    }

    var startPosY = 0f
    var itemsHeight = 0f
    var itemsCount = 0
    var stepNo = 0
    var display = 0..0

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
                assert(itemsHeight > 0f)   // If this triggers, it means Item 0 hasn't moved the cursor vertically
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
            if(stepNo == 3)
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
    fun begin(itemsCount: Int, itemsHeight: Float = -1f) {

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

        if (itemsCount < 0)            return
        /*  In theory here we should assert that ImGui::GetCursorPosY() == StartPosY + DisplayEnd * ItemsHeight,
            but it feels saner to just seek at the end and not assert/crash the user.         */
        if (itemsCount < Int.MAX_VALUE)
            setCursorPosYAndSetupDummyPrevLine(startPosY + itemsCount * itemsHeight, itemsHeight) // advance cursor
        itemsCount = -1
        stepNo = 3
    }

    fun setCursorPosYAndSetupDummyPrevLine(posY: Float, lineHeight: Float) {
        /*  Set cursor position and a few other things so that SetScrollHere() and Columns() can work when seeking
            cursor.
            FIXME: It is problematic that we have to do that here, because custom/equivalent end-user code would stumble
            on the same issue. Consider moving within SetCursorXXX functions?   */
        cursorPosY = posY
        with(currentWindow.dc) {
            // Setting those fields so that SetScrollHere() can properly function after the end of our clipper usage.
            cursorPosPrevLine.y = cursorPos.y - lineHeight
            /*  If we end up needing more accurate data (to e.g. use SameLine) we may as well make the clipper have a
                fourth step to let user process and display the last item in their list.             */
            prevLineHeight = lineHeight - Style.itemSpacing.y
            if (columnsCount > 1)
                columnsCellMinY = cursorPos.y   // Setting this so that cell Y position are set properly
        }
    }
}