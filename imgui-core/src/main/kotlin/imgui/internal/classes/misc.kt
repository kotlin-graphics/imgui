package imgui.internal.classes

import glm_.f
import glm_.glm
import glm_.i
import glm_.max
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.api.g
import imgui.classes.*
import imgui.font.Font
import imgui.internal.*
import imgui.internal.ColumnsFlags
import kotlin.math.cos
import kotlin.math.sin

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

/** Data shared between all ImDrawList instances
 * Data shared among multiple draw lists (typically owned by parent ImGui context, but you may create one yourself) */
class DrawListSharedData {
    /** UV of white pixel in the atlas  */
    var texUvWhitePixel = Vec2()
    /** Current/default font (optional, for simplified AddText overload) */
    var font: Font? = null
    /** Current/default font size (optional, for simplified AddText overload) */
    var fontSize = 0f

    var curveTessellationTol = 0f
    /** Value for pushClipRectFullscreen() */
    var clipRectFullscreen = Vec4(-8192f, -8192f, 8192f, 8192f)
    /** Initial flags at the beginning of the frame (it is possible to alter flags on a per-drawlist basis afterwards) */
    var initialFlags = DrawListFlag.None.i

    // Const data
    // FIXME: Bake rounded corners fill/borders in atlas
    var circleVtx12 = Array(12) {
        val a = it * 2 * glm.PIf / 12
        Vec2(cos(a), sin(a))
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

/** Type information associated to one ImGuiDataType. Retrieve with DataTypeGetInfo(). */
//class DataTypeInfo {
//    /** Size in byte */
//    var size = 0
//    /** Default printf format for the type */
//    lateinit var printFmt: String
//    /** Default scanf format for the type */
//    lateinit var scanFmt: String
//}

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

/** Result of a directional navigation move query result */
class NavMoveResult {
    /** Best candidate  */
    var id: ID = 0
    /** Best candidate window current selectable group ID */
    var selectScopeId: ID = 0
    /** Best candidate window   */
    var window: Window? = null
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
    val posVal = Vec2()
    val posPivotVal = Vec2()
    val sizeVal = Vec2()
    val contentSizeVal = Vec2()
    var collapsedVal = false
    /** Valid if 'SetNextWindowSizeConstraint' is true  */
    val sizeConstraintRect = Rect()
    var sizeCallback: SizeCallback? = null
    var sizeCallbackUserData: Any? = null
    var bgAlphaVal = Float.MAX_VALUE
    /** *Always on* This is not exposed publicly, so we don't clear it. */
    var menuBarOffsetMinVal = Vec2()

    fun clearFlags() {
        flags = NextWindowDataFlag.None.i
    }
}

class NextItemData {
    var flags: NextItemDataFlags = 0
    /** Set by SetNextItemWidth(). */
    var width = 0f
    /** Set by SetNextItemOpen() function. */
    var openVal = false
    var openCond = Cond.None

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

//struct ImGuiSettingsHandler;        // Storage for one type registered in the .ini file
/** Stacked style modifier, backup of modified data so we can restore it. Data type inferred from the variable. */
class StyleMod(val idx: StyleVar) {
    var ints = IntArray(2)
    val floats = FloatArray(2)
}

/** Storage for one active tab item (sizeof() 26~32 bytes) */
class TabItem {
    var id: ID = 0
    var flags: TabItemFlags = 0
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

/** Storage for window settings stored in .ini file (we keep one of those even if the actual window wasn't instanced during this session)
 *  Windows data saved in imgui.ini file
 *  ~ CreateNewWindowSettings */
class WindowSettings(val name: String = "") {
    var id: ID = hash(name)
    var pos = Vec2()
    var size = Vec2()
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

/** Helper: ImPool<>. Basic keyed storage for contiguous instances, slow/amortized insertion, O(1) indexable, O(Log N) queries by ID over a dense/hot buffer,
 *  Honor constructor/destructor. Add/remove invalidate all pointers. Indexes have the same lifetime as the associated object. */
inline class PoolIdx(val i: Int)

class TabBarPool {
    /** Contiguous data */
    val data = ArrayList<TabBar?>()
    /** ID->Index */
    val map = mutableMapOf<ID, PoolIdx>()

    operator fun get(key: ID): TabBar? = map[key]?.let { data[it.i] }
    operator fun get(n: PoolIdx): TabBar? = data.getOrNull(n.i)
    fun getIndex(p: TabBar): PoolIdx = PoolIdx(data.indexOf(p))
    fun getOrAddByKey(key: ID): TabBar = map[key]?.let { data[it.i] }
            ?: add().also { map[key] = PoolIdx(data.lastIndex) }

    operator fun contains(p: TabBar): Boolean = p in data
    fun clear() {
        data.clear()
        map.clear()
    }

    fun add(): TabBar = TabBar().also { data += it }
    fun remove(key: ID, p: TabBar) = remove(key, getIndex(p))
    fun remove(key: ID, idx: PoolIdx) {
        data[idx.i] = null
        map -= key
    }

    val size get() = data.size
}