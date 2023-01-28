package imgui.classes

import glm_.i
import glm_.max
import glm_.min
import imgui.ImGui.calcListClipping
import imgui.api.g

/** Helper: Manually clip large list of items.
 *  If you are submitting lots of evenly spaced items and you have a random access to the list, you can perform coarse
 *  clipping based on visibility to save yourself from processing those items at all.
 *  The clipper calculates the range of visible items and advance the cursor to compensate for the non-visible items we
 *  have skipped.
 *  (Dear ImGui already clip items based on their bounds but it needs to measure text size to do so, whereas manual
 *  coarse clipping before submission makes this cost and your own data fetching/submission cost almost null)
 *    ImGuiListClipper clipper;
 *    clipper.Begin(1000);         // We have 1000 elements, evenly spaced.
 *    clipper.ForceDisplay(42);    // Optional, force element with given index to be displayed (use f.e. if you need to update a tooltip for a drag&drop source)
 *    while (clipper.Step())
 *        for (int i = clipper.DisplayStart; i < clipper.DisplayEnd; i++)
 *            ImGui::Text("line number %d", i);
 *  Generally what happens is:
 *  - Clipper lets you process the first element (DisplayStart = 0, DisplayEnd = 1) regardless of it being visible or not.
 *  - User code submit one element.
 *  - Clipper can measure the height of the first element
 *  - Clipper calculate the actual range of elements to display based on the current clipping rectangle, position the cursor before the first visible element.
 *  - User code submit visible elements. */
class ListClipper {

    var displayStart = 0
    var displayEnd = 0

    val display
        get() = displayStart until displayEnd

    // [Internal]
    var itemsCount = -1
    val rangeStart = IntArray(4)  // 1 for the user, rest for internal use
    val rangeEnd = IntArray(4)
    var rangeCount = 0
    val yRangeMin = IntArray(1)
    val yRangeMax = IntArray(1)
    var yRangeCount = 0
    var stepNo = 0
    var itemsFrozen = 0
    var itemsHeight = 0f
    var startPosY = 0f

    fun dispose() = assert(itemsCount == -1) { "Forgot to call End(), or to Step() until false?" }

    /** Automatically called by constructor if you passed 'items_count' or by Step() in Step 1.
     *  Use case A: Begin() called from constructor with items_height<0, then called again from Sync() in StepNo 1
     *  Use case B: Begin() called from constructor with items_height>0
     *  FIXME-LEGACY: Ideally we should remove the Begin/End functions but they are part of the legacy API we still
     *  support. This is why some of the code in Step() calling Begin() and reassign some fields, spaghetti style.
     */
    fun begin(itemsCount: Int = -1, itemsHeight: Float = -1f) {

        val window = g.currentWindow!!

        g.currentTable?.let { table ->
            if (table.isInsideRow)
                table.endRow()
        }

        startPosY = window.dc.cursorPos.y
        this.itemsHeight = itemsHeight
        this.itemsCount = itemsCount
        itemsFrozen = 0
        stepNo = 0
        displayStart = -1
        displayEnd = 0
    }

    /** Automatically called on the last call of Step() that returns false. */
    fun end() {

        if (itemsCount < 0) // Already ended
            return

        // In theory here we should assert that ImGui::GetCursorPosY() == StartPosY + DisplayEnd * ItemsHeight, but it feels saner to just seek at the end and not assert/crash the user.
        if (itemsCount < Int.MAX_VALUE && displayStart >= 0)
            setCursorPosYAndSetupForPrevLine(startPosY + (itemsCount - itemsFrozen) * itemsHeight, itemsHeight)
        itemsCount = -1
        stepNo = rangeCount
    }

    /** Optionally call before the first call to Step() if you need a range of items to be displayed regardless of visibility. */
    fun forceDisplayRange(itemStart: Int, itemEnd: Int) {
        if (displayStart < 0 && rangeCount + yRangeCount < 1) { // Only allowed after Begin() and if there has not been a specified range yet.
            rangeStart[rangeCount] = itemStart
            rangeEnd[rangeCount] = itemEnd
            rangeCount++
        }
    }

    /** Like ForceDisplayRange, but with a number instead of an end index. */
    fun forceDisplay(item_start: Int, item_count: Int = 1) = forceDisplayRange(item_start, item_start + item_count)

    /** Like ForceDisplayRange, but with y coordinates instead of item indices. */
    fun forceDisplayYRange(yMin: Float, yMax: Float) {
        if (displayStart < 0 && rangeCount + yRangeCount < 1) { // Only allowed after Begin() and if there has not been a specified range yet.
            yRangeMin[yRangeCount] = yMin.i
            yRangeMax[yRangeCount] = yMax.i
            yRangeCount++
        }
    }

    /** Call until it returns false. The DisplayStart/DisplayEnd fields will be set and you can process/draw those
     *  items.  */
    fun step(): Boolean {

        val window = g.currentWindow!!

        val table = g.currentTable
        if (table != null && table.isInsideRow)
            table.endRow()

        // Reached end of list
        if (itemsCount == 0 || skipItemForListClipping) {
            end()
            return false
        }

        var calcClipping = false

        // Step 0: Let you process the first element (regardless of it being visible or not, so we can measure the element height)
        if (stepNo == 0) {

            // While we are in frozen row state, keep displaying items one by one, unclipped
            // FIXME: Could be stored as a table-agnostic state.
            if (table != null && !table.isUnfrozenRows) {
                displayStart = itemsFrozen
                displayEnd = itemsFrozen + 1
                itemsFrozen++
                return true
            }

            startPosY = window.dc.cursorPos.y
            if (itemsHeight <= 0f) {
                // Submit the first item (or range) so we can measure its height (generally it is 0..1)
                rangeStart[rangeCount] = itemsFrozen
                rangeEnd[rangeCount] = itemsFrozen + 1
                if (++rangeCount > 1)
                    rangeCount = sortAndFuseRanges(rangeStart, 0, rangeEnd, 0, rangeCount)
                displayStart = rangeStart[0] max itemsFrozen
                displayEnd = rangeEnd[0] min itemsCount
                stepNo = 1
                return true
            }

            calcClipping = true // If on the first step with known item height, calculate clipping.
        }

        // Step 1: Let the clipper infer height from first range
        if (itemsHeight <= 0f) {
            assert(stepNo == 1)
            if (table != null) {
                val posY1 = table.rowPosY1   // Using this instead of StartPosY to handle clipper straddling the frozen row
                val posY2 = table.rowPosY2   // Using this instead of CursorPos.y to take account of tallest cell.
                itemsHeight = posY2 - posY1
                window.dc.cursorPos.y = posY2
            } else
                itemsHeight = (window.dc.cursorPos.y - startPosY) / (displayEnd - displayStart)
            assert(itemsHeight > 0f) { "Unable to calculate item height! First item hasn't moved the cursor vertically!" }

            calcClipping = true // If item height had to be calculated, calculate clipping afterwards.
        }

        // Step 0 or 1: Calculate the actual range of visible elements.
        if (calcClipping) {
            assert(itemsHeight > 0f)

            val alreadySubmitted = displayEnd
            calcListClipping(itemsCount - alreadySubmitted, itemsHeight).let { (start, end) -> rangeStart[rangeCount] = start; rangeEnd[rangeCount] = end }

            // Only add another range if it hasn't been handled by the initial range.
            if (rangeStart[rangeCount] < rangeEnd[rangeCount]) {
                rangeStart[rangeCount] += alreadySubmitted
                rangeEnd[rangeCount] += alreadySubmitted
                rangeCount++
            }

            // Convert specified y ranges to item index ranges.
            for (i in 0 until yRangeCount) {
                var start = alreadySubmitted + ((yRangeMin[i] - window.dc.cursorPos.y) / itemsHeight).i
                var end = alreadySubmitted + ((yRangeMax[i] - window.dc.cursorPos.y) / itemsHeight).i + 1

                start = start max alreadySubmitted
                end = end min itemsCount

                if (start < end) {
                    rangeStart[rangeCount] = start
                    rangeEnd[rangeCount] = end
                    rangeCount++
                }
            }

            // Try to sort and fuse only if there is more than 1 range remaining.
            if (rangeCount > stepNo + 1)
                rangeCount = stepNo + sortAndFuseRanges(rangeStart, stepNo, rangeEnd, stepNo, rangeCount - stepNo)
        }

        // Step 0+ (if item height is given in advance) or 1+: Display the next range in line.
        if (stepNo < rangeCount) {
            val alreadySubmitted = displayEnd
            displayStart = rangeStart[stepNo] max alreadySubmitted
            displayEnd = rangeEnd[stepNo] min itemsCount

            // Seek cursor
            if (displayStart > alreadySubmitted)
                setCursorPosYAndSetupForPrevLine(startPosY + (displayStart - itemsFrozen) * itemsHeight, itemsHeight)

            stepNo++
            return true
        }

        // After the last step: Let the clipper validate that we have reached the expected Y position (corresponding to element DisplayEnd),
        // Advance the cursor to the end of the list and then returns 'false' to end the loop.
        if (itemsCount < Int.MAX_VALUE)
            setCursorPosYAndSetupForPrevLine(startPosY + (itemsCount - itemsFrozen) * itemsHeight, itemsHeight) // advance cursor
        itemsCount = -1

        return false
    }

    companion object {

        fun setCursorPosYAndSetupForPrevLine(posY: Float, lineHeight: Float) {
            // Set cursor position and a few other things so that SetScrollHereY() and Columns() can work when seeking cursor.
            // FIXME: It is problematic that we have to do that here, because custom/equivalent end-user code would stumble on the same issue.
            // The clipper should probably have a 4th step to display the last item in a regular manner.
            val window = g.currentWindow!!
            val offY = posY - window.dc.cursorPos.y
            window.dc.cursorPos.y = posY
            window.dc.cursorMaxPos.y = window.dc.cursorMaxPos.y max (posY - g.style.itemSpacing.y)
            window.dc.cursorPosPrevLine.y = window.dc.cursorPos.y - lineHeight  // Setting those fields so that SetScrollHereY() can properly function after the end of our clipper usage.
            window.dc.prevLineSize.y = lineHeight - g.style.itemSpacing.y      // If we end up needing more accurate data (to e.g. use SameLine) we may as well make the clipper have a fourth step to let user process and display the last item in their list.
            window.dc.currentColumns?.let { columns ->
                columns.lineMinY = window.dc.cursorPos.y                         // Setting this so that cell Y position are set properly
            }
            g.currentTable?.let { table ->
                if (table.isInsideRow)
                    table.endRow()
                table.rowPosY2 = window.dc.cursorPos.y
                val rowIncrease = ((offY / lineHeight) + 0.5f).i
                //table->CurrentRow += row_increase; // Can't do without fixing TableEndRow()
                table.rowBgColorCounter += rowIncrease
            }
        }

        fun sortAndFuseRanges(rangeStart: IntArray, startOfs: Int, rangeEnd: IntArray, endOfs: Int, rangeCount_: Int): Int {
            var rangeCount = rangeCount_
            // Helper to order ranges and fuse them together if possible.
            // First sort both rangeStart and rangeEnd by rangeStart. Since this helper will just sort 2 or 3 entries, a bubble sort will do fine.
            for (sortEnd in (rangeCount - 1) downTo 1)
                for (i in 0 until sortEnd)
                    if (rangeStart[i] > rangeStart[i + 1]) {
                        var swap = rangeStart[i]
                        rangeStart[i] = rangeStart[i + 1]
                        rangeStart[i + 1] = swap
                        swap = rangeEnd[i]
                        rangeEnd[i] = rangeEnd[i + 1]
                        rangeEnd[i + 1] = swap
                    }

            // Now fuse ranges together as much as possible.
            var i = 1
            while (i < rangeCount)
                if (rangeEnd[i - 1] >= rangeStart[i]) {
                    rangeEnd[i - 1] = rangeEnd[i - 1] max rangeEnd[i]
                    rangeCount--
                    for (j in i until rangeCount) {
                        rangeStart[j] = rangeStart[j + 1]
                        rangeEnd[j] = rangeEnd[j + 1]
                    }
                } else
                    i++

            return rangeCount
        }
    }
}

// FIXME-TABLE: This prevents us from using ImGuiListClipper _inside_ a table cell.
// The problem we have is that without a Begin/End scheme for rows using the clipper is ambiguous.
val skipItemForListClipping: Boolean
    get() = g.currentTable?.hostSkipItems ?: g.currentWindow!!.skipItems