package imgui.internal.sections

import imgui.ID
import imgui.internal.DrawListSplitter
import imgui.internal.classes.Rect


//-----------------------------------------------------------------------------
// [SECTION] Columns support
//-----------------------------------------------------------------------------


typealias OldColumnsFlags = Int

/** Flags: for Columns(), BeginColumns() */
enum class OldColumnsFlag {

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

    val i: OldColumnsFlags = if (ordinal == 0) 0 else 1 shl (ordinal - 1)

    infix fun and(b: OldColumnsFlag): OldColumnsFlags = i and b.i
    infix fun and(b: OldColumnsFlags): OldColumnsFlags = i and b
    infix fun or(b: OldColumnsFlag): OldColumnsFlags = i or b.i
    infix fun or(b: OldColumnsFlags): OldColumnsFlags = i or b
    infix fun xor(b: OldColumnsFlag): OldColumnsFlags = i xor b.i
    infix fun xor(b: OldColumnsFlags): OldColumnsFlags = i xor b
    infix fun wo(b: OldColumnsFlags): OldColumnsFlags = and(b.inv())
}

infix fun OldColumnsFlags.and(b: OldColumnsFlag): OldColumnsFlags = and(b.i)
infix fun OldColumnsFlags.or(b: OldColumnsFlag): OldColumnsFlags = or(b.i)
infix fun OldColumnsFlags.xor(b: OldColumnsFlag): OldColumnsFlags = xor(b.i)
infix fun OldColumnsFlags.has(b: OldColumnsFlag): Boolean = and(b.i) != 0
infix fun OldColumnsFlags.hasnt(b: OldColumnsFlag): Boolean = and(b.i) == 0
infix fun OldColumnsFlags.wo(b: OldColumnsFlag): OldColumnsFlags = and(b.i.inv())


/** Storage data for a single column for legacy Columns() api */
class OldColumnData {
    /** Column start offset, normalized 0f (far left) -> 1f (far right) */
    var offsetNorm = 0f
    var offsetNormBeforeResize = 0f

    /** Not exposed */
    var flags: OldColumnsFlags = 0
    var clipRect = Rect()
}

/** Storage data for a columns set for legacy Columns() api */
class OldColumns {
    var id: ID = 0
    var flags: OldColumnsFlags = OldColumnsFlag.None.i
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
    var hostInitialClipRect = Rect()

    /** Backup of ClipRect during PushColumnsBackground()/PopColumnsBackground() */
    var hostBackupClipRect = Rect()

    /** Backup of WorkRect at the time of BeginColumns() */
    var hostBackupParentWorkRect = Rect()
    var lineMinY = 0f
    var lineMaxY = 0f
    val columns = ArrayList<OldColumnData>()
    val splitter = DrawListSplitter()

    fun destroy() = splitter.clearFreeMemory(destroy = true)

    /** ~GetColumnOffsetFromNorm    */
    infix fun getOffsetFrom(offsetNorm: Float): Float = offsetNorm * (offMaxX - offMinX)

    /** ~GetColumnNormFromOffset    */
    fun getNormFrom(offset: Float): Float = offset / (offMaxX - offMinX)
}