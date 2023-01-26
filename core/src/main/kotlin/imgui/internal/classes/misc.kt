package imgui.internal.classes

import glm_.*
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.api.g
import imgui.classes.Context
import imgui.internal.*
import imgui.internal.sections.*


//-----------------------------------------------------------------------------
// [SECTION] ImDrawList support -> section.`drawList support`.kt
//-----------------------------------------------------------------------------


/** Stacked color modifier, backup of modified data so we can restore it    */
class ColorMod(val col: Col, value: Vec4) {
    val backupValue = Vec4(value)
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
    var windowID: ID = 0
    var backupCursorPos = Vec2()
    var backupCursorMaxPos = Vec2()
    var backupIndent = 0f
    var backupGroupOffset = 0f
    var backupCurrLineSize = Vec2()
    var backupCurrLineTextBaseOffset = 0f
    var backupActiveIdIsAlive = 0
    var backupActiveIdPreviousFrameIsAlive = false
    var backupHoveredIdIsAlive = false
    var emitItem = false
}

/** Simple column measurement, currently used for MenuItem() only.. This is very short-sighted/throw-away code and NOT a generic helper. */
class MenuColumns {

    var totalWidth = 0
    var nextTotalWidth = 0
    var spacing = 0
    var offsetIcon = 0         // Always zero for now
    var offsetLabel = 0        // Offsets are locked in Update()
    var offsetShortcut = 0 // Offsets are locked in Update()
    var offsetMark = 0
    var widths = IntArray(4) // Width of:   Icon, Label, Shortcut, Mark  (accumulators for current frame)

    fun update(spacing: Float, windowReappearing: Boolean) {
        if (windowReappearing)
            widths.fill(0)
        this.spacing = spacing.i
        calcNextTotalWidth(true)
        widths.fill(0)
        totalWidth = nextTotalWidth
        nextTotalWidth = 0
    }

    fun declColumns(wIcon: Float, wLabel: Float, wShortcut: Float, wMark: Float): Float {
        widths[0] = widths[0] max wIcon.i
        widths[1] = widths[1] max wLabel.i
        widths[2] = widths[2] max wShortcut.i
        widths[3] = widths[3] max wMark.i
        calcNextTotalWidth(false)
        return (totalWidth max nextTotalWidth).f
    }

    fun calcNextTotalWidth(updateOffsets: Boolean) {
        var offset = 0
        var wantSpacing = false
        for (i in widths.indices) {
            val width = widths[i]
            if (wantSpacing && width > 0)
                offset += spacing
            wantSpacing = wantSpacing or (width > 0)
            if (updateOffsets)
                when (i) {
                    1 -> offsetLabel = offset
                    2 -> offsetShortcut = offset
                    3 -> offsetMark = offset
                }
            offset += width
        }
        nextTotalWidth = offset
    }
}

//-----------------------------------------------------------------------------
// [SECTION] Metrics, Debug
//-----------------------------------------------------------------------------

/** Storage for ShowMetricsWindow() and DebugNodeXXX() functions */
class MetricsConfig {
    var showWindowsRects = false
    var showWindowsBeginOrder = false
    var showTablesRects = false
    var showDrawCmdMesh = true
    var showDrawCmdBoundingBoxes = true
    var showWindowsRectsType = -1
    var showTablesRectsType = -1
}

class StackSizes {
    var sizeOfIDStack = 0
    var sizeOfColorStack = 0
    var sizeOfStyleVarStack = 0
    var sizeOfFontStack = 0
    var sizeOfFocusScopeStack = 0
    var sizeOfGroupStack = 0
    var sizeOfBeginPopupStack = 0

    /** Save current stack sizes for later compare */
    fun setToCurrentState() {
        val window = g.currentWindow!!
        sizeOfIDStack = window.idStack.size
        sizeOfColorStack = g.colorStack.size
        sizeOfStyleVarStack = g.styleVarStack.size
        sizeOfFontStack = g.fontStack.size
        sizeOfFocusScopeStack = g.focusScopeStack.size
        sizeOfGroupStack = g.groupStack.size
        sizeOfBeginPopupStack = g.beginPopupStack.size
    }

    /** Compare to detect usage errors */
    fun compareWithCurrentState() {

        val window = g.currentWindow!!

        // Window stacks
        // NOT checking: DC.ItemWidth, DC.TextWrapPos (per window) to allow user to conveniently push once and not pop (they are cleared on Begin)
        assert(sizeOfIDStack == window.idStack.size) {
            "PushID/PopID or TreeNode/TreePop Mismatch!"
        }

        // Global stacks
        // For color, style and font stacks there is an incentive to use Push/Begin/Pop/.../End patterns, so we relax our checks a little to allow them.
        assert(sizeOfGroupStack == g.groupStack.size) { "BeginGroup/EndGroup Mismatch!" }
        assert(sizeOfBeginPopupStack == g.beginPopupStack.size) { "BeginPopup/EndPopup or BeginMenu/EndMenu Mismatch!" }
        assert(sizeOfColorStack >= g.colorStack.size) { "PushStyleColor/PopStyleColor Mismatch!" }
        assert(sizeOfStyleVarStack >= g.styleVarStack.size) { "PushStyleVar/PopStyleVar Mismatch!" }
        assert(sizeOfFontStack >= g.fontStack.size) { "PushFont/PopFont Mismatch!" }
        assert(sizeOfFocusScopeStack == g.focusScopeStack.size) { "PushFocusScope/PopFocusScope Mismatch!" }
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
    val scrollVal = Vec2()
    var collapsedVal = false

    /** Valid if 'SetNextWindowSizeConstraint' is true  */
    val sizeConstraintRect = Rect()
    var sizeCallback: SizeCallback? = null
    var sizeCallbackUserData: Any? = null

    /** Override background alpha */
    var bgAlphaVal = Float.MAX_VALUE

    /** (Always on) This is not exposed publicly, so we don't clear it and it doesn't have a corresponding flag (could we? for consistency?) */
    var menuBarOffsetMinVal = Vec2()

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

/** Status storage for last submitted item */
class LastItemData {
    var id: ID = 0
    var inFlags: ItemFlags = 0 // See ImGuiItemFlags_
    var statusFlags: ItemStatusFlags = 0 // See ImGuiItemStatusFlags_
    val rect = Rect() // Full rectangle
    val navRect = Rect() // Navigation scoring rectangle (not displayed)
    val displayRect = Rect() // Display rectangle (only if ImGuiItemStatusFlags_HasDisplayRect is set)
}

/** Data saved for each window pushed into the stack */
class WindowStackData {
    lateinit var window: Window
    lateinit var parentLastItemDataBackup: LastItemData
};

class ShrinkWidthItem(var index: Int = 0, var width: Float = 0f) {
    infix fun put(other: ShrinkWidthItem) {
        index = other.index
        width = other.width
    }
}

class PtrOrIndex(
    /** Either field can be set, not both. e.g. Dock node tab bars are loose while BeginTabBar() ones are in a pool. */
    val ptr: TabBar?,
    /** Usually index in a main pool. */
    val index: PoolIdx) {

    constructor(ptr: TabBar) : this(ptr, PoolIdx(-1))

    constructor(index: PoolIdx) : this(null, index)
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


/** Clear all settings data */
typealias ClearAllFn = (ctx: Context, handler: SettingsHandler) -> Unit

/** Read: Called before reading (in registration order) */
typealias ReadInitFn = (ctx: Context, handler: SettingsHandler) -> Unit

/** Read: Called when entering into a new ini entry e.g. "[Window][Name]" */
typealias ReadOpenFn = (ctx: Context, handler: SettingsHandler, name: String) -> Any

/** Read: Called for every line of text within an ini entry */
typealias ReadLineFn = (ctx: Context, handler: SettingsHandler, entry: Any, line: String) -> Unit

/** Read: Called after reading (in registration order) */
typealias ApplyAllFn = (ctx: Context, handler: SettingsHandler) -> Unit

/** Write: Output every entries into 'out_buf' */
typealias WriteAllFn = (ctx: Context, handler: SettingsHandler, outBuf: StringBuilder) -> Unit


/** Stacked style modifier, backup of modified data so we can restore it. Data type inferred from the variable. */
class StyleMod(val idx: StyleVar) {
    var ints = IntArray(2)
    val floats = FloatArray(2)
}

/** Storage data for BeginComboPreview()/EndComboPreview() */
class ComboPreviewData {
    val previewRect = Rect()
    val backupCursorPos = Vec2()
    val backupCursorMaxPos = Vec2()
    val backupCursorPosPrevLine = Vec2()
    var backupPrevLineTextBaseOffset = 0f
    var backupLayout = LayoutType.Horizontal
}

/** Storage for one active tab item (sizeof() 40 bytes) */
class TabItem {
    var id: ID = 0
    var flags = TabItemFlag.None.i
    var lastFrameVisible = -1

    /** This allows us to infer an ordered list of the last activated tabs with little maintenance */
    var lastFrameSelected = -1

    /** Position relative to beginning of tab */
    var offset = 0f

    /** Width currently displayed */
    var width = 0f

    /** Width of label, stored during BeginTabItem() call */
    var contentWidth = 0f

    /** When Window==NULL, offset to name within parent ImGuiTabBar::TabsNames */
    var nameOffset = -1

    /** BeginTabItem() order, used to re-order tabs after toggling ImGuiTabBarFlags_Reorderable */
    var beginOrder = -1

    /** Index only used during TabBarLayout() */
    var indexDuringLayout = -1

    /** Marked as closed by SetTabItemClosed() */
    var wantClose = false
}


//-----------------------------------------------------------------------------
// Tabs
//-----------------------------------------------------------------------------

/** Helper: ImPool<>
 *  Basic keyed storage for contiguous instances, slow/amortized insertion, O(1) indexable, O(Log N) queries by ID over a dense/hot buffer,
 *  Honor constructor/destructor. Add/remove invalidate all pointers. Indexes have the same lifetime as the associated object. */
inline class PoolIdx(val i: Int) {
    operator fun inc() = PoolIdx(i + 1)
    operator fun dec() = PoolIdx(i - 1)
    operator fun compareTo(other: PoolIdx): Int = i.compareTo(other.i)
    operator fun compareTo(other: Int): Int = i.compareTo(other.i)
    operator fun minus(int: Int) = PoolIdx(i - int)
}

class TabBarPool {
    /** Contiguous data */
    val list = ArrayList<TabBar?>()

    /** ID->Index */
    val map = mutableMapOf<ID, PoolIdx>()

    /** ~GetByKey */
    operator fun get(key: ID): TabBar? = map[key]?.let { list[it.i] }

    /** ~GetByIndex */
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

class Pool<T>(val placementNew: () -> T) : Iterable<T> {
    val buf = ArrayList<T>()        // Contiguous data
    val map = mutableMapOf<ID, PoolIdx>()        // ID->Index

    /** Number of active/alive items (for display purpose) */
    var aliveCount = PoolIdx(0)

    fun destroy() = clear()

    fun getByKey(key: ID): T? = map[key]?.let { buf[it.i] }
    operator fun get(key: ID): T? = getByKey(key)

    fun getByIndex(n: PoolIdx): T = buf[n.i]
    fun getByIndex(n: Int): T = buf[n]
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
        aliveCount = PoolIdx(0)
    }

    fun add(): T {
        val new = placementNew()
        buf += new
        aliveCount++
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
        aliveCount--
    }
    //    void        Reserve(int capacity) { Buf.reserve(capacity); Map.Data.reserve(capacity); }

    // To iterate a ImPool: for (int n = 0; n < pool.GetMapSize(); n++) if (T* t = pool.TryGetMapData(n)) { ... }
    // Can be avoided if you know .Remove() has never been called on the pool, or AliveCount == GetMapSize()
    val bufSize: Int
        get() {
            assert(buf.size == map.size)
            return buf.size
        }

    /** It is the map we need iterate to find valid items, since we don't have "alive" storage anywhere */
    val mapSize get() = map.size
    //    T*          TryGetBufData(ImPoolIdx n) { int idx = Map . Data [n].val_i; if (idx == -1) return NULL; return GetByIndex(idx); }

    val size get() = buf.size
    override fun iterator(): Iterator<T> = buf.iterator()
}
