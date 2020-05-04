package imgui.internal.classes

import gli_.has
import glm_.*
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.api.g
import imgui.classes.Context
import imgui.classes.DrawList
import imgui.classes.WindowClass
import imgui.font.Font
import imgui.internal.*
import unsigned.toUInt
import java.lang.StringBuilder
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

//-----------------------------------------------------------------------------
// Forward declarations
//-----------------------------------------------------------------------------

/** Helper: ImBoolVector. Store 1-bit per value.
 *  Note that Resize() currently clears the whole vector. */
class BitVector(sz: Int) { // ~create
    var storage = IntArray((sz + 31) ushr 5)

    // Helpers: Bit arrays
    infix fun IntArray.testBit(n: Int): Boolean {
        val mask = 1 shl (n and 31); return (this[n ushr 5] and mask).bool; }

    infix fun IntArray.clearBit(n: Int) {
        val mask = 1 shl (n and 31); this[n ushr 5] = this[n ushr 5] wo mask; }

    infix fun IntArray.setBit(n: Int) {
        val mask = 1 shl (n and 31); this[n ushr 5] = this[n ushr 5] or mask; }

    fun IntArray.setBitRange(n_: Int, n2: Int) {
        var n = n_
        while (n <= n2) {
            val aMod = n and 31
            val bMod = (if (n2 >= n + 31) 31 else n2 and 31) + 1
            val mask = ((1L shl bMod) - 1).toUInt() wo ((1L shl aMod) - 1).toUInt()
            this[n ushr 5] = this[n ushr 5] or mask
            n = (n + 32) wo 31
        }
    }

    fun clear() {
        storage = IntArray(0)
    }

    infix fun testBit(n: Int): Boolean {
        assert(n < storage.size shl 5); return storage testBit n; }

    infix fun setBit(n: Int) {
        assert(n < storage.size shl 5); storage setBit n; }

    infix fun clearBit(n: Int) {
        assert(n < storage.size shl 5); storage clearBit n; }

    fun unpack(): ArrayList<Int> {
        val res = arrayListOf<Int>()
        storage.forEachIndexed { index, entries32 ->
            if (entries32 != 0)
                for (bitN in 0..31)
                    if (entries32 has (1 shl bitN))
                        res += (index shl 5) + bitN
        }
        return res
    }
}

// Rect -> Rect.kt

/** Helper to build a ImDrawData instance */
class DrawDataBuilder {
    /** Global layers for: regular, tooltip */
    val layers = Array(2) { ArrayList<DrawList>() }

    fun clear() = layers.forEach { it.clear() }

    fun flattenIntoSingleLayer() {
        val size = layers.map { it.size }.count()
        layers[0].ensureCapacity(size)
        for (layerN in 1 until layers.size) {
            val layer = layers[layerN]
            if (layer.isEmpty()) continue
            layers[0].addAll(layer)
            layer.clear()
        }
    }
}

// ImDrawList: Helper function to calculate a circle's segment count given its radius and a "maximum error" value.
const val DRAWLIST_CIRCLE_AUTO_SEGMENT_MIN = 12
const val DRAWLIST_CIRCLE_AUTO_SEGMENT_MAX = 512
fun DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC(_RAD: Float, _MAXERROR: Float) = clamp(((glm.πf * 2f) / acos((_RAD - _MAXERROR) / _RAD)).i, DRAWLIST_CIRCLE_AUTO_SEGMENT_MIN, DRAWLIST_CIRCLE_AUTO_SEGMENT_MAX)

// ImDrawList: You may set this to higher values (e.g. 2 or 3) to increase tessellation of fast rounded corners path.
var DRAWLIST_ARCFAST_TESSELLATION_MULTIPLIER = 1

/** Data shared between all ImDrawList instances
 *  You may want to create your own instance of this if you want to use ImDrawList completely without ImGui. In that case, watch out for future changes to this structure.
 *  Data shared among multiple draw lists (typically owned by parent ImGui context, but you may create one yourself) */
class DrawListSharedData {
    /** UV of white pixel in the atlas  */
    var texUvWhitePixel = Vec2()

    /** Current/default font (optional, for simplified AddText overload) */
    var font: Font? = null

    /** Current/default font size (optional, for simplified AddText overload) */
    var fontSize = 0f

    var curveTessellationTol = 0f

    /** Number of circle segments to use per pixel of radius for AddCircle() etc */
    var circleSegmentMaxError = 0f

    /** Value for pushClipRectFullscreen() */
    var clipRectFullscreen = Vec4(-8192f, -8192f, 8192f, 8192f)

    /** Initial flags at the beginning of the frame (it is possible to alter flags on a per-drawlist basis afterwards) */
    var initialFlags = DrawListFlag.None.i

    // [Internal] Lookup tables

    // Lookup tables
    val arcFastVtx = Array(12 * DRAWLIST_ARCFAST_TESSELLATION_MULTIPLIER) {
        // FIXME: Bake rounded corners fill/borders in atlas
        val a = it * 2 * glm.PIf / 12
        Vec2(cos(a), sin(a))
    }

    /** Precomputed segment count for given radius (array index + 1) before we calculate it dynamically (to avoid calculation overhead) */
    val circleSegmentCounts = IntArray(64) // This will be set by SetCircleSegmentMaxError()

    fun setCircleSegmentMaxError_(maxError: Float) {
        if (circleSegmentMaxError == maxError)
            return
        circleSegmentMaxError = maxError
        for (i in circleSegmentCounts.indices) {
            val radius = i + 1f
            val segmentCount = DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC(radius, circleSegmentMaxError)
            circleSegmentCounts[i] = segmentCount min 255
        }
    }
}

/** Stacked color modifier, backup of modified data so we can restore it    */
class ColorMod(val col: Col, value: Vec4) {
    val backupValue = Vec4(value)
}

/** Storage data for a single column */
class ColumnData {
    /** Column start offset, normalized 0f (far left) -> 1f (far right) */
    var offsetNorm = 0f
    var offsetNormBeforeResize = 0f

    /** Not exposed */
    var flags: ColumnsFlags = 0
    var clipRect = Rect()
}

/** Storage data for a columns set */
class Columns {
    var id: ID = 0
    var flags: ColumnsFlags = ColumnsFlag.None.i
    var isFirstFrame = false
    var isBeingResized = false
    var current = 0
    var count = 1

    /** Offsets from HostWorkRect.Min.x */
    var offMinX = 0f

    /** Offsets from HostWorkRect.Min.x */
    var offMaxX = 0f

    /** Backup of CursorPos at the time of BeginColumns() */
    var hostCursorPosY = 0f

    /** Backup of CursorMaxPos at the time of BeginColumns() */
    var hostCursorMaxPosX = 0f

    /** Backup of ClipRect at the time of BeginColumns() */
    var hostClipRect = Rect()

    /** Backup of WorkRect at the time of BeginColumns() */
    var hostWorkRect = Rect()
    var lineMinY = 0f
    var lineMaxY = 0f
    val columns = ArrayList<ColumnData>()
    val splitter = DrawListSplitter()

    fun destroy() = splitter.clearFreeMemory(destroy = true)

    fun clear() {
        id = 0
        flags = ColumnsFlag.None.i
        isFirstFrame = false
        isBeingResized = false
        current = 0
        count = 1
        offMaxX = 0f
        offMinX = 0f
        hostCursorPosY = 0f
        hostCursorMaxPosX = 0f
        lineMaxY = 0f
        lineMinY = 0f
        columns.clear()
    }

    /** ~GetColumnOffsetFromNorm    */
    infix fun getOffsetFrom(offsetNorm: Float): Float = offsetNorm * (offMaxX - offMinX)

    /** ~GetColumnNormFromOffset    */
    fun getNormFrom(offset: Float): Float = offset / (offMaxX - offMinX)
}

// Context -> Context.kt

/** Type information associated to one ImGuiDataType. Retrieve with DataTypeGetInfo(). */
//class DataTypeInfo {
//    /** Size in byte */
//    var size = 0
//    /** Default printf format for the type */
//    lateinit var printFmt: String
//    /** Default scanf format for the type */
//    lateinit var scanFmt: String
//}

// DockContext, DockNode, DockNodeSettings -> dock.kt

/* Stacked storage data for BeginGroup()/EndGroup() */
class GroupData {
    var backupCursorPos = Vec2()
    var backupCursorMaxPos = Vec2()
    var backupIndent = 0f
    var backupGroupOffset = 0f
    var backupCurrLineSize = Vec2()
    var backupCurrLineTextBaseOffset = 0f
    var backupActiveIdIsAlive = 0
    var backupActiveIdPreviousFrameIsAlive = false
    var emitItem = false
}

/** Backup and restore just enough data to be able to use isItemHovered() on item A after another B in the same window
 *  has overwritten the data.
 *  ¬ItemHoveredDataBackup, we optimize by using a function accepting a lambda */
fun itemHoveredDataBackup(block: () -> Unit) {
    // backup
    var window = g.currentWindow!!
    val lastItemId = window.dc.lastItemId
    val lastItemStatusFlags = window.dc.lastItemStatusFlags
    val lastItemRect = Rect(window.dc.lastItemRect)
    val lastItemDisplayRect = Rect(window.dc.lastItemDisplayRect)

    block()

    // restore
    window = g.currentWindow!!
    window.dc.lastItemId = lastItemId
    window.dc.lastItemRect put lastItemRect
    window.dc.lastItemStatusFlags = lastItemStatusFlags
    window.dc.lastItemDisplayRect = lastItemDisplayRect
}

/** Simple column measurement, currently used for MenuItem() only.. This is very short-sighted/throw-away code and NOT a generic helper. */
class MenuColumns {

    var spacing = 0f
    var width = 0f
    var nextWidth = 0f
    val pos = FloatArray(3)
    var nextWidths = FloatArray(3)

    fun update(count: Int, spacing: Float, clear: Boolean) {
        assert(count == pos.size)
        nextWidth = 0f
        width = 0f
        this.spacing = spacing
        if (clear)
            nextWidths.fill(0f)
        for (i in pos.indices) {
            if (i > 0 && nextWidths[i] > 0f)
                width += spacing
            pos[i] = floor(width)
            width += nextWidths[i]
            nextWidths[i] = 0f
        }
    }

    fun declColumns(w0: Float, w1: Float, w2: Float): Float {
        nextWidth = 0f
        nextWidths[0] = nextWidths[0] max w0
        nextWidths[1] = nextWidths[1] max w1
        nextWidths[2] = nextWidths[2] max w2
        for (i in pos.indices)
            nextWidth += nextWidths[i] + (if (i > 0 && nextWidths[i] > 0f) spacing else 0f)
        return width max nextWidth.i.f // JVM only TODO why?
    }


    fun calcExtraSpace(availW: Float) = glm.max(0f, availW - width)
}

/** Result of a gamepad/keyboard directional navigation move query result */
class NavMoveResult {
    /** Best candidate window   */
    var window: Window? = null

    /** Best candidate ID  */
    var id: ID = 0

    /** Best candidate focus scope ID */
    var focusScopeId: ID = 0

    /** Best candidate box distance to current NavId    */
    var distBox = Float.MAX_VALUE

    /** Best candidate center distance to current NavId */
    var distCenter = Float.MAX_VALUE

    var distAxial = Float.MAX_VALUE

    /** Best candidate bounding box in window relative space    */
    var rectRel = Rect()

    fun clear() {
        id = 0
        window = null
        distBox = Float.MAX_VALUE
        distCenter = Float.MAX_VALUE
        distAxial = Float.MAX_VALUE
        rectRel = Rect()
    }
}

/** Storage for SetNextWindow** functions    */
class NextWindowData {
    var flags = NextWindowDataFlag.None.i
    var posCond = Cond.None
    var sizeCond = Cond.None
    var collapsedCond = Cond.None
    var dockCond = Cond.None
    val posVal = Vec2()
    val posPivotVal = Vec2()
    val sizeVal = Vec2()
    val contentSizeVal = Vec2()
    val scrollVal = Vec2()
    var posUndock = false
    var collapsedVal = false

    /** Valid if 'SetNextWindowSizeConstraint' is true  */
    val sizeConstraintRect = Rect()
    var sizeCallback: SizeCallback? = null
    var sizeCallbackUserData: Any? = null

    /** Override background alpha */
    var bgAlphaVal = Float.MAX_VALUE

    var viewportId: ID = 0
    var dockId: ID = 0
    var windowClass = WindowClass()

    /** (Always on) This is not exposed publicly, so we don't clear it and it doesn't have a corresponding flag (could we? for consistency?) */
    val menuBarOffsetMinVal = Vec2()

    fun clearFlags() {
        flags = NextWindowDataFlag.None.i
    }
}

class NextItemData {
    var flags: NextItemDataFlags = 0

    /** Set by SetNextItemWidth() */
    var width = 0f

    /** Set by SetNextItemMultiSelectData() (!= 0 signify value has been set, so it's an alternate version of HasSelectionData, we don't use Flags for this because they are cleared too early. This is mostly used for debugging) */
    var focusScopeId: ID = 0

    var openCond = Cond.None

    /** Set by SetNextItemOpen() function. */
    var openVal = false

    /** Also cleared manually by ItemAdd()! */
    fun clearFlags() {
        flags = NextItemDataFlag.None.i
    }
}

/* Storage for current popup stack  */
class PopupData(
        /** Set on OpenPopup()  */
        var popupId: ID = 0,
        /** Resolved on BeginPopup() - may stay unresolved if user never calls OpenPopup()  */
        var window: Window? = null,
        /** Set on OpenPopup() copy of NavWindow at the time of opening the popup  */
        var sourceWindow: Window? = null,
        /** Set on OpenPopup()  */
        var openFrameCount: Int = -1,
        /** Set on OpenPopup(), we need this to differentiate multiple menu sets from each others
         *  (e.g. inside menu bar vs loose menu items)    */
        var openParentId: ID = 0,
        /** Set on OpenPopup(), preferred popup position (typically == OpenMousePos when using mouse)   */
        var openPopupPos: Vec2 = Vec2(),
        /** Set on OpenPopup(), copy of mouse position at the time of opening popup */
        var openMousePos: Vec2 = Vec2())

/** Read: Called when entering into a new ini entry e.g. "[Window][Name]" */
typealias ReadOpenFn = (ctx: Context, handler: SettingsHandler, name: String) -> Any?

/** Read: Called for every line of text within an ini entry */
typealias ReadLineFn = (ctx: Context, handler: SettingsHandler, entry: Any, line: String) -> Unit

/** Write: Output every entries into 'out_buf' */
typealias WriteAllFn  = (ctx: Context, handler: SettingsHandler, outBuf: StringBuilder) -> Unit

/** Storage for one type registered in the .ini file */
class SettingsHandler {
    /** Short description stored in .ini file. Disallowed characters: '[' ']' */
    var typeName = ""
    /** == ImHashStr(TypeName) */
    var typeHash: ID = 0

    lateinit var readOpenFn: ReadOpenFn
    lateinit var readLineFn: ReadLineFn
    lateinit var writeAllFn: WriteAllFn
    var userData: Any? = null
}

/** Stacked style modifier, backup of modified data so we can restore it. Data type inferred from the variable. */
class StyleMod(val idx: StyleVar) {
    var ints = IntArray(2)
    val floats = FloatArray(2)
}

/** Storage for one active tab item (sizeof() 32~40 bytes) */
class TabItem {
    var id: ID = 0
    var flags = TabItemFlag.None.i

    /** When TabItem is part of a DockNode's TabBar, we hold on to a window. */
    var window: Window? = null
    var lastFrameVisible = -1

    /** This allows us to infer an ordered list of the last activated tabs with little maintenance */
    var lastFrameSelected = -1

    /** When Window==NULL, offset to name within parent ImGuiTabBar::TabsNames */
    var nameOffset = -1

    /** Position relative to beginning of tab */
    var offset = 0f

    /** Width currently displayed */
    var width = 0f

    /** Width of actual contents, stored during BeginTabItem() call */
    var contentWidth = 0f
}

/** Storage for a window .ini settings (we keep one of those even if the actual window wasn't instanced during this session)
 *
 *  Because we never destroy or rename ImGuiWindowSettings, we can store the names in a separate buffer easily.
 *  [JVM] We prefer keeping the `name` variable
 *
 *  ~ CreateNewWindowSettings */
class WindowSettings(val name: String = "") {
    var id: ID = hash(name)

    /** NB: Settings position are stored RELATIVE to the viewport! Whereas runtime ones are absolute positions. */
    val pos = Vec2()
    val size = Vec2()

    val viewportPos = Vec2()
    var viewportId: ID = 0

    /** ID of last known DockNode (even if the DockNode is invisible because it has only 1 active window), or 0 if none. */
    var dockId: ID = 0

    /** ID of window class if specified */
    var classId: ID = 0

    /** Order of the last time the window was visible within its DockNode. This is used to reorder windows that are reappearing on the same frame. Same value between windows that were active and windows that were none are possible. */
    var dockOrder = -1
    var collapsed = false
}

//-----------------------------------------------------------------------------
// Tabs
//-----------------------------------------------------------------------------

class ShrinkWidthItem(var index: Int, var width: Float)
class PtrOrIndex(
        /** Either field can be set, not both. e.g. Dock node tab bars are loose while BeginTabBar() ones are in a pool. */
        val ptr: TabBar?,
        /** Usually index in a main pool. */
        val index: PoolIdx) {

    constructor(ptr: TabBar) : this(ptr, PoolIdx(-1))

    constructor(index: PoolIdx) : this(null, index)
}

/** Helper: ImPool<>
 *  Basic keyed storage for contiguous instances, slow/amortized insertion, O(1) indexable, O(Log N) queries by ID over a dense/hot buffer,
 *  Honor constructor/destructor. Add/remove invalidate all pointers. Indexes have the same lifetime as the associated object. */
inline class PoolIdx(val i: Int) {
    operator fun inc() = PoolIdx(i + 1)
    operator fun dec() = PoolIdx(i - 1)
    operator fun compareTo(other: PoolIdx): Int = i.compareTo(other.i)
    operator fun minus(int: Int) = PoolIdx(i - int)
}

class TabBarPool {
    /** Contiguous data */
    val list = ArrayList<TabBar?>()

    /** ID->Index */
    val map = mutableMapOf<ID, PoolIdx>()

    operator fun get(key: ID): TabBar? = map[key]?.let { list[it.i] }
    operator fun get(n: PoolIdx): TabBar? = list.getOrNull(n.i)
    fun getIndex(p: TabBar): PoolIdx = PoolIdx(list.indexOf(p))
    fun getOrAddByKey(key: ID): TabBar = map[key]?.let { list[it.i] }
            ?: add().also { map[key] = PoolIdx(list.lastIndex) }

    operator fun contains(p: TabBar): Boolean = p in list
    fun clear() {
        list.clear()
        map.clear()
    }

    fun add(): TabBar = TabBar().also { list += it }
//    fun remove(key: ID, p: TabBar) = remove(key, getIndex(p))
//    fun remove(key: ID, idx: PoolIdx) {
//        list[idx.i] = null
//        map -= key
//    }

    val size: Int
        get() = list.size
}

class Pool<T>(val placementNew: () -> T) {
    val buf = ArrayList<T>()        // Contiguous data
    val map = mutableMapOf<ID, PoolIdx>()        // ID->Index

    fun destroy() = clear()

    fun getByKey(key: ID): T? = map[key]?.let { buf[it.i] }
    operator fun get(key: ID): T? = getByKey(key)

    fun getByIndex(n: PoolIdx): T = buf[n.i]
    operator fun get(n: PoolIdx): T = getByIndex(n)

    fun getIndex(p: T) = PoolIdx(buf.indexOf(p))
    fun getOrAddByKey(key: ID): T {
        map[key]?.let { return buf[it.i] }
        val new = add()
        map[key] = PoolIdx(buf.lastIndex) // not size because ::add already increased it
        return new
    }

    operator fun contains(p: T): Boolean = p in buf
    fun clear() {
        map.clear()
        buf.clear()
    }

    fun add(): T {
        val new = placementNew()
        buf += new
        return new
    }

    @Deprecated("just a placeholder to remind the different behaviour with the indices")
    fun remove(key: ID, p: T) = remove(key, getIndex(p))

    @Deprecated("just a placeholder to remind the different behaviour with the indices")
    fun remove(key: ID, idx: PoolIdx) {
        buf.removeAt(idx.i)
        map.remove(key)
        // update indices in map
        map.replaceAll { _, i -> i - (i > idx).i }
    }
//    void        Reserve(int capacity)
//    { Buf.reserve(capacity); Map.Data.reserve(capacity); }

    val size get() = buf.size
}
