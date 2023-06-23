package imgui.internal.sections

import glm_.i
import imgui.classes.ListClipper

//-----------------------------------------------------------------------------
// [SECTION] Clipper support
//-----------------------------------------------------------------------------

// Note that Max is exclusive, so perhaps should be using a Begin/End convention.
class ListClipperRange {
    var min = 0
    var max = 0
    var posToIndexConvert = false      // Begin/End are absolute position (will be converted to indices later)
    var posToIndexOffsetMin = 0    // Add to Min after converting to indices
    var posToIndexOffsetMax = 0    // Add to Min after converting to indices

    companion object {
        fun fromIndices(min: Int, max: Int) = ListClipperRange().also { it.min = min; it.max = max }
        fun fromPositions(y1: Float, y2: Float, offMin: Int, offMax: Int) =
            ListClipperRange().also { it.min = y1.i; it.max = y2.i; it.posToIndexConvert = true; it.posToIndexOffsetMin = offMin; it.posToIndexOffsetMax = offMax }
    }
}

// Temporary clipper data, buffers shared/reused between instances
class ListClipperData {
    lateinit var listClipper: ListClipper
    var lossynessOffset = 0f
    var stepNo = 0
    var itemsFrozen = 0
    val ranges = ArrayList<ListClipperRange>()
    fun reset(clipper: ListClipper) {
        listClipper = clipper
        stepNo = 0
        itemsFrozen = 0
        ranges.clear()
    }
}