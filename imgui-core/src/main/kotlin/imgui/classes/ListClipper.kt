package imgui.classes

import glm_.max
import imgui.ImGui
import imgui.ImGui.calcListClipping
import imgui.api.g

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

    var display: IntRange = 0..0
    var itemsCount = 0

    // [Internal]
    var stepNo = 0
    var itemsHeight = 0f
    var startPosY = 0f

    init {
        /* NB: Begin() initialize every fields (as we allow user to call Begin/End multiple times on a same instance if they want). */
        begin(itemsCount, itemsHeight)
    }

    /** Call until it returns false. The DisplayStart/DisplayEnd fields will be set and you can process/draw those
     *  items.  */
    fun step(): Boolean {
        val window = g.currentWindow!!

        return when {

            itemsCount == 0 || window.skipItems -> {
                itemsCount = -1
                false
            }
            /*  Step 0: the clipper let you process the first element, regardless of it being visible or not, so we can measure
                the element height.     */
            stepNo == 0 -> {
                display = 0..1
                startPosY = window.dc.cursorPos.y
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
                    val itemsHeight = window.dc.cursorPos.y - startPosY
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
    }

    /** Automatically called by constructor if you passed 'items_count' or by Step() in Step 1.
     *  Use case A: Begin() called from constructor with items_height<0, then called again from Sync() in StepNo 1
     *  Use case B: Begin() called from constructor with items_height>0
     *  FIXME-LEGACY: Ideally we should remove the Begin/End functions but they are part of the legacy API we still
     *  support. This is why some of the code in Step() calling Begin() and reassign some fields, spaghetti style.
     */
    fun begin(itemsCount: Int = -1, itemsHeight: Float = -1f) {

        val window = g.currentWindow!!

        startPosY = window.dc.cursorPos.y
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
            /*  Set cursor position and a few other things so that SetScrollHereY() and Columns() can work when seeking
                cursor.
                FIXME: It is problematic that we have to do that here, because custom/equivalent end-user code would
                stumble on the same issue.
                The clipper should probably have a 4th step to display the last item in a regular manner.   */
            g.currentWindow!!.dc.apply {
                cursorPos.y = posY
                cursorMaxPos.y = cursorMaxPos.y max posY
                cursorPosPrevLine.y = cursorPos.y - lineHeight  // Setting those fields so that SetScrollHereY() can properly function after the end of our clipper usage.
                prevLineSize.y = (lineHeight - g.style.itemSpacing.y)      // If we end up needing more accurate data (to e.g. use SameLine) we may as well make the clipper have a fourth step to let user process and display the last item in their list.
                currentColumns?.lineMinY = cursorPos.y                         // Setting this so that cell Y position are set properly
            }
        }
    }
}