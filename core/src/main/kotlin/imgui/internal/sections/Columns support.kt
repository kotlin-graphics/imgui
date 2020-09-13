package imgui.internal.sections

import imgui.ID
import imgui.internal.DrawListSplitter
import imgui.internal.classes.Rect


//-----------------------------------------------------------------------------
// [SECTION] Columns support
//-----------------------------------------------------------------------------


typealias ColumnsFlags = Int

/** Flags: for Columns(), BeginColumns() */
enum class ColumnsFlag {

    None,

    /** Disable column dividers */
    NoBorder,

    /** Disable resizing columns when clicking on the dividers  */
    NoResize,

    /** Disable column width preservation when adjusting columns    */
    NoPreserveWidths,

    /** Disable forcing columns to fit within window    */
    NoForceWithinWindow,

    /** (WIP) Restore pre-1.51 behavior of extending the parent window contents size but _without affecting the columns
     *  width at all_. Will eventually remove.  */
    GrowParentContentsSize;

    val i: ColumnsFlags = if (ordinal == 0) 0 else 1 shl (ordinal - 1)

    infix fun and(b: ColumnsFlag): ColumnsFlags = i and b.i
    infix fun and(b: ColumnsFlags): ColumnsFlags = i and b
    infix fun or(b: ColumnsFlag): ColumnsFlags = i or b.i
    infix fun or(b: ColumnsFlags): ColumnsFlags = i or b
    infix fun xor(b: ColumnsFlag): ColumnsFlags = i xor b.i
    infix fun xor(b: ColumnsFlags): ColumnsFlags = i xor b
    infix fun wo(b: ColumnsFlags): ColumnsFlags = and(b.inv())
}

infix fun ColumnsFlags.and(b: ColumnsFlag): ColumnsFlags = and(b.i)
infix fun ColumnsFlags.or(b: ColumnsFlag): ColumnsFlags = or(b.i)
infix fun ColumnsFlags.xor(b: ColumnsFlag): ColumnsFlags = xor(b.i)
infix fun ColumnsFlags.has(b: ColumnsFlag): Boolean = and(b.i) != 0
infix fun ColumnsFlags.hasnt(b: ColumnsFlag): Boolean = and(b.i) == 0
infix fun ColumnsFlags.wo(b: ColumnsFlag): ColumnsFlags = and(b.i.inv())


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