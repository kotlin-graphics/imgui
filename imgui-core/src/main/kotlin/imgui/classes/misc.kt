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
import java.nio.ByteBuffer
import java.util.stream.Collectors


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

/** Helper: Key->Value storage
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

/** Helper: Growable text buffer for logging/accumulating text
 *  (this could be called 'ImGuiTextBuilder' / 'ImGuiStringBuilder') */
//class TextBuffer {
//    init {
//        TODO()
//    }
//}
class TextFilter(defaultFilter: String? = "") {

    var inputBuf = ByteArray(256)
    val filters = ArrayList<String>()
    var countGrep = 0

    init {
        defaultFilter?.toByteArray(inputBuf)
    }

    class TextRange

    fun isActive() = filters.isNotEmpty()

    /** Helper calling InputText+Build   */
    fun draw(label: String = "Filter (inc,-exc)", width: Float): Boolean {
        if (width != 0f)
            setNextItemWidth(width)
        val valueChanged = inputText(label, inputBuf)
        if (valueChanged)
            build()
        return valueChanged
    }

    fun passFilter(text: String, textEnd: Int = 0): Boolean {
        val check = if (textEnd > 0) text.substring(0, textEnd) else text

        if (filters.isEmpty()) return true
        if (filters.stream().filter(String::isNotEmpty).count() == 0L) return true

        var passSub = false

        for (f in filters) {
            if (f.isEmpty()) continue
            if (f[0] == '-') {
                // Subtract
                if (check.contains(f.substring(1)))
                    return false
                else
                    passSub = true
            } else if (check.contains(f))  // Grep
                return true
        }
        // Implicit * grep
        return passSub //countGrep == 0
    }

    fun build() {
        filters.clear()
        // TODO check if resync
        filters.addAll(String(inputBuf)
                .split(",")
                .stream()
                .filter(String::isNotEmpty)
                .collect(Collectors.toList()))
    }
}

