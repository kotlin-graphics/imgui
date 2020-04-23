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
import imgui.internal.DrawData
import imgui.internal.classes.DrawDataBuilder
import imgui.internal.classes.Rect
import imgui.internal.classes.Window


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

// PlatformIO, PlatformMonitor -> platform.kt

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

    val data = HashMap<ID, Any>()

    // - Get***() functions find pair, never add/allocate. Pairs are sorted so a query is O(log N)
    // - Set***() functions find pair, insertion on demand if missing.
    // - Sorted insertion is costly, paid once. A typical frame shouldn't need to insert any new pair.

    fun clear() = data.clear()

    fun int(key: ID, defaultVal: Int = 0): Int = (data[key] as? Int) ?: defaultVal
//    operator fun set(key: ID, value: Int) = data.set(key, value)

    fun bool(key: ID, defaultVal: Boolean = false): Boolean = (data[key] as? Int)?.bool ?: defaultVal
//    operator fun set(id: ID, value: Boolean) = data.set(id, value)

    fun float(key: ID, defaultVal: Float = 0f): Float = (data[key] as? Int)?.let { glm.intBitsToFloat(it) } ?: defaultVal
//    operator fun set(key: ID, value: Float) = data.set(key, glm.floatBitsToInt(value))

    operator fun set(key: ID, value: Int) = data.set(key, value)

//    IMGUI_API void*     GetVoidPtr(ImGuiID key) const; // default_val is NULL
//    IMGUI_API void      SetVoidPtr(ImGuiID key, void* val);

    // - Get***Ref() functions finds pair, insert on demand if missing, return pointer. Useful if you intend to do Get+Set.
    // - References are only valid until a new value is added to the storage. Calling a Set***() function or a Get***Ref() function invalidates the pointer.
    // - A typical use case where this is convenient for quick hacking (e.g. add storage during a live Edit&Continue session if you can't modify existing struct)
    //      float* pvar = ImGui::GetFloatRef(key); ImGui::SliderFloat("var", pvar, 0, 100.0f); some_var += *pvar;

//    fun getIntRef(key: ID, defaultVal: Int = 0) {
//        int(key, defaultVal)
//    }
//    IMGUI_API bool*     GetBoolRef(ImGuiID key, bool default_val = false);
//    IMGUI_API float*    GetFloatRef(ImGuiID key, float default_val = 0.0f);
//    IMGUI_API void**    GetVoidPtrRef(ImGuiID key, void* default_val = NULL);

    /** Use on your own storage if you know only integer are being stored (open/close all tree nodes)   */
    fun setAllInt(value: Int) = data.replaceAll { _, _ -> value }
}

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
        if (f.isNotEmpty()) {
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

    fun passFilter(text_: String, textEnd: Int = text_.length): Boolean {
        val text = if (textEnd != text_.length) text_.substring(0, textEnd) else text_
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
        inputBuf.cStr.split(",").map { it.trim() }.filter(String::isNotEmpty).forEach { f ->
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

typealias ViewportFlags = Int

/** Flags stored in ImGuiViewport::Flags, giving indications to the platform back-ends. */
enum class ViewportFlag(val i: ViewportFlags) {
    None(0),

    /** Platform Window: Disable platform decorations: title bar, borders, etc. (generally set all windows, but if ImGuiConfigFlags_ViewportsDecoration is set we only set this on popups/tooltips) */
    NoDecoration(1 shl 0),

    /** Platform Window: Disable platform task bar icon (generally set on popups/tooltips, or all windows if ImGuiConfigFlags_ViewportsNoTaskBarIcon is set) */
    NoTaskBarIcon(1 shl 1),

    /** Platform Window: Don't take focus when created. */
    NoFocusOnAppearing(1 shl 2),

    /** Platform Window: Don't take focus when clicked on. */
    NoFocusOnClick(1 shl 3),

    /** Platform Window: Make mouse pass through so we can drag this window while peaking behind it. */
    NoInputs(1 shl 4),

    /** Platform Window: Renderer doesn't need to clear the framebuffer ahead (because we will fill it entirely). */
    NoRendererClear(1 shl 5),

    /** Platform Window: Display on top (for tooltips only). */
    TopMost(1 shl 6),

    /** Platform Window: Window is minimized, can skip render. When minimized we tend to avoid using the viewport pos/size for clipping window or testing if they are contained in the viewport. */
    Minimized(1 shl 7),

    /** Platform Window: Avoid merging this window into another host window. This can only be set via ImGuiWindowClass viewport flags override (because we need to now ahead if we are going to create a viewport in the first place!). */
    NoAutoMerge(1 shl 8),

    /** Main viewport: can host multiple imgui windows (secondary viewports are associated to a single window). */
    CanHostOtherWindows(1 shl 9);

    infix fun and(b: ViewportFlag): ViewportFlags = i and b.i
    infix fun and(b: ViewportFlags): ViewportFlags = i and b
    infix fun or(b: ViewportFlag): ViewportFlags = i or b.i
    infix fun or(b: ViewportFlags): ViewportFlags = i or b
    infix fun xor(b: ViewportFlag): ViewportFlags = i xor b.i
    infix fun xor(b: ViewportFlags): ViewportFlags = i xor b
    infix fun wo(b: ViewportFlags): ViewportFlags = and(b.inv())
}

infix fun ViewportFlags.and(b: ViewportFlag): ViewportFlags = and(b.i)
infix fun ViewportFlags.or(b: ViewportFlag): ViewportFlags = or(b.i)
infix fun ViewportFlags.xor(b: ViewportFlag): ViewportFlags = xor(b.i)
infix fun ViewportFlags.has(b: ViewportFlag): Boolean = and(b.i) != 0
infix fun ViewportFlags.hasnt(b: ViewportFlag): Boolean = and(b.i) == 0
infix fun ViewportFlags.wo(b: ViewportFlag): ViewportFlags = and(b.i.inv())


/** The viewports created and managed by Dear ImGui. The role of the platform back-end is to create the platform/OS windows corresponding to each viewport.
 *  - Main Area = entire viewport.
 *  - Work Area = entire viewport minus sections optionally used by menu bars, status bars. Some positioning code will prefer to use this. Window are also trying to stay within this area. */
open class Viewport {

    /** Unique identifier for the viewport */
    var id: ID = 0

    /** See ImGuiViewportFlags_ */
    var flags: ViewportFlags = ViewportFlag.None.i

    /** Main Area: Position of the viewport (the imgui coordinates are the same as OS desktop/native coordinates) */
    val pos = Vec2()

    /** Main Area: Size of the viewport. */
    val size = Vec2()

    /** Work Area: Offset from Pos to top-left corner of Work Area. Generally (0,0) or (0,+main_menu_bar_height). Work Area is Full Area but without menu-bars/status-bars (so WorkArea always fit inside Pos/Size!) */
    val workOffsetMin = Vec2()

    /** Work Area: Offset from Pos+Size to bottom-right corner of Work Area. Generally (0,0) or (0,-status_bar_height). */
    val workOffsetMax = Vec2()

    /** 1.0f = 96 DPI = No extra scale. */
    var dpiScale = 0f

    /** The ImDrawData corresponding to this viewport. Valid after Render() and until the next call to NewFrame(). */
    var drawData: DrawData? = null

    /** (Advanced) 0: no parent. Instruct the platform back-end to setup a parent/child relationship between platform windows. */
    var parentViewportId: ID = 0

    // Our design separate the Renderer and Platform back-ends to facilitate combining default back-ends with each others.
    // When our create your own back-end for a custom engine, it is possible that both Renderer and Platform will be handled
    // by the same system and you may not need to use all the UserData/Handle fields.
    // The library never uses those fields, they are merely storage to facilitate back-end implementation.

    /** void* to hold custom data structure for the renderer (e.g. swap chain, framebuffers etc.). generally set by your Renderer_CreateWindow function. */
    var rendererUserData: Any? = null

    /** void* to hold custom data structure for the OS / platform (e.g. windowing info, render context). generally set by your Platform_CreateWindow function.*/
    var platformUserData: Any? = null

    /** void* for FindViewportByPlatformHandle(). (e.g. suggested to use natural platform handle such as HWND, GLFWWindow*, SDL_Window*) */
    var platformHandle: Any? = null

    /** void* to hold lower-level, platform-native window handle (e.g. the HWND) when using an abstraction layer like GLFW or SDL (where PlatformHandle would be a SDL_Window*) */
    var platformHandleRaw: Any? = null

    /** Platform window requested move (e.g. window was moved by the OS / host window manager, authoritative position will be OS window position) */
    var platformRequestMove = false

    /** Platform window requested resize (e.g. window was resized by the OS / host window manager, authoritative size will be OS window size) */
    var platformRequestResize = false

    /** Platform window requested closure (e.g. window was moved by the OS / host window manager, e.g. pressing ALT-F4) */
    var platformRequestClose = false

    open fun destroy() = assert(platformUserData == null && rendererUserData == null)

    /** Access work-area rectangle */
    val workPos get() = Vec2(pos.x + workOffsetMin.x, pos.y + workOffsetMin.y)

    /** This not clamped */
    val workSize get() = Vec2(size.x - workOffsetMin.x + workOffsetMax.x, size.y - workOffsetMin.y + workOffsetMax.y)
}

/** ImGuiViewport Private/Internals fields (cardinal sin: we are using inheritance!)
 *  Note that every instance of ImGuiViewport is in fact a ImGuiViewportP. */
class ViewportP : Viewport() {

    var idx = -1

    /** Last frame number this viewport was activated by a window */
    var lastFrameActive = -1

    /** Last frame number the background (0) and foreground (1) draw lists were used */
    var lastFrameDrawLists = IntArray(2) { -1 }

    /** Last stamp number from when a window hosted by this viewport was made front-most (by comparing this value between two viewport we have an implicit viewport z-order */
    var lastFrontMostStampCount = -1

    var lastNameHash: ID = 0

    val lastPos = Vec2()

    /** Window opacity (when dragging dockable windows/viewports we make them transparent) */
    var alpha = 1f

    var lastAlpha = 1f
    var platformMonitor = -1
    var platformWindowCreated = false

    /** Set when the viewport is owned by a window (and ImGuiViewportFlags_CanHostOtherWindows is NOT set)*/
    var window: Window? = null

    /** Convenience background (0) and foreground (1) draw lists. We use them to draw software mouser cursor when io.MouseDrawCursor is set and to draw most debug overlays. */
    var drawLists = Array<DrawList?>(2) { null }
    val drawDataP = DrawData()
    val drawDataBuilder = DrawDataBuilder()
    val lastPlatformPos = Vec2(Float.MAX_VALUE)
    val lastPlatformSize = Vec2(Float.MAX_VALUE)
    val lastRendererSize = Vec2(Float.MAX_VALUE)

    /** Work area top-left offset being increased during the frame */
    val currWorkOffsetMin = Vec2()

    /** Work area bottom-right offset being decreased during the frame */
    val currWorkOffsetMax = Vec2()

    override fun destroy() {
        super.destroy()
        drawLists[0]?.clearFreeMemory(true)
        drawLists[1]?.clearFreeMemory(true)
    }

    val mainRect get() = Rect(pos.x, pos.y, pos.x + size.x, pos.y + size.y)
    val workRect get() = Rect(pos.x + workOffsetMin.x, pos.y + workOffsetMin.y, pos.x + size.x + workOffsetMax.x, pos.y + size.y + workOffsetMax.y)
    fun clearRequestFlags() {
        platformRequestClose = false
        platformRequestMove = false
        platformRequestResize = false
    }
}