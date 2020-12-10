package imgui.classes

import glm_.bool
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.colorConvertHSVtoRGB
import imgui.ImGui.frameCount
import imgui.ImGui.inputText
import imgui.ImGui.setNextItemWidth


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

/** Helper: Execute a block of code at maximum once a frame. Convenient if you want to quickly create an UI within
 *  deep-nested code that runs multiple times every frame.
 *  Usage: val oaf = OnceUponAFrame()
 *  if(oaf()) {
 *      ImGui.text("This will be called only once per frame")
 *  }
 */
class OnceUponAFrame {
    private var refFrame = -1
    operator fun invoke(): Boolean {
        val currentFrame = frameCount
        if (refFrame == currentFrame)
            return false
        refFrame = currentFrame
        return true
    }
}

/** Data payload for Drag and Drop operations: AcceptDragDropPayload(), GetDragDropPayload() */
class Payload {
    // Members

    // /** Data (copied and owned by dear imgui) */
    /** Data provided by setDragDropSource */
    var data: Any? = null
    /** Data size */
    var dataSize = 0

    // [Internal]

    /** Source item id */
    var sourceId: ID = 0
    /** Source parent id (if available) */
    var sourceParentId: ID = 0
    /** Data timestamp */
    var dataFrameCount = -1
    /** Data type tag (short user-supplied string, 32 characters max) */ // JVM: No character limit
    var dataType: String? = null
    /** Set when AcceptDragDropPayload() was called and mouse has been hovering the target item (nb: handle overlapping drag targets) */
    var preview = false
    /** Set when AcceptDragDropPayload() was called and mouse button is released over the target item. */
    var delivery = false

    fun clear() {
        sourceParentId = 0
        sourceId = 0
        data = null
        dataSize = 0
        dataType = null
        dataFrameCount = -1
        delivery = false
        preview = false
    }

    fun isDataType(type: String): Boolean = dataFrameCount != -1 && type == dataType
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


// struct Storage [JVM] substituted by HashMap

/** Helper: Growable text buffer for logging/accumulating text
 *  (this could be called 'ImGuiTextBuilder' / 'ImGuiStringBuilder') */
//class TextBuffer [JVM] StringBuilder

class TextFilter(defaultFilter: String = "") {

    // filters
    val inc = ArrayList<String>()
    val exc = ArrayList<String>()
    var inputBuf = ByteArray(256)
    var countGrep = 0

    init {
        this += defaultFilter
    }

    operator fun plusAssign(filter: String) {
        val f = filter.trim()
        if(f.isNotEmpty()) {
            if (f[0] == '-')
                exc += f.drop(1)
            else {
                inc += f
                countGrep++
            }
        }
    }

    fun isActive() = inc.isNotEmpty() || exc.isNotEmpty()

    /** Helper calling InputText+Build   */
    fun draw(label: String = "Filter (inc,-exc)", width: Float = 0f): Boolean {
        if (width != 0f)
            setNextItemWidth(width)
        val valueChanged = inputText(label, inputBuf)
        if (valueChanged)
            build()
        return valueChanged
    }

    fun passFilter(text_: String?, textEnd: Int = -1): Boolean {

        if(exc.isEmpty() && inc.isEmpty())
            return true

        var text = text_ ?: ""
        text = if (textEnd != text.length) text.substring(0, textEnd) else text
        if (exc.any { it in text }) return false
        if (inc.any { it in text }) return true
        // Implicit * grep
        return countGrep == 0
//        if (filters.isEmpty()) return true
//        if (filters.stream().filter(String::isNotEmpty).count() == 0L) return true
//
//        var passSub = false
//
//        for (f in filters) {
//            if (f.isEmpty()) continue
//            if (f[0] == '-') {
//                // Subtract
//                if (check.contains(f.substring(1)))
//                    return false
//                else
//                    passSub = true
//            } else if (check.contains(f))  // Grep
//                return true
//        }
//        // Implicit * grep
//        return passSub //countGrep == 0
    }

    fun build() {
        inc.clear()
        exc.clear()
        countGrep = 0
        inputBuf.cStr.split(",").map { it.trim() }.filter(String::isNotEmpty).
                forEach { f ->
                    if (f[0] == '-') exc += f
                    else {
                        inc += f
                        countGrep++
                    }
                }
    }

    fun clear() {
        inc.clear()
        exc.clear()
        countGrep = 0
    }
}

