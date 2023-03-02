package imgui.internal.sections

import imgui.Flag
import imgui.ID
import imgui.internal.DrawListSplitter
import imgui.internal.classes.Rect


//-----------------------------------------------------------------------------
// [SECTION] Columns support
//-----------------------------------------------------------------------------


typealias OldColumnsFlags = Int

/** Flags: for Columns(), BeginColumns() */
enum class OldColumnsFlag : Flag<OldColumnsFlag> {

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

    override val i: OldColumnsFlags = if (ordinal == 0) 0 else 1 shl (ordinal - 1)
}

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
}