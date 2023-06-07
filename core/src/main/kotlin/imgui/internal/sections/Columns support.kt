package imgui.internal.sections

import com.livefront.sealedenum.GenSealedEnum
import imgui.Flag
import imgui.FlagBase
import imgui.ID
import imgui.none
import imgui.internal.DrawListSplitter
import imgui.internal.classes.Rect


//-----------------------------------------------------------------------------
// [SECTION] Columns support
//-----------------------------------------------------------------------------


typealias OldColumnFlags = Flag<OldColumnsFlag>

/** Flags: for Columns(), BeginColumns() */
sealed class OldColumnsFlag : FlagBase<OldColumnsFlag>() {

    /** Disable column dividers */
    object NoBorder : OldColumnsFlag()

    /** Disable resizing columns when clicking on the dividers  */
    object NoResize : OldColumnsFlag()

    /** Disable column width preservation when adjusting columns    */
    object NoPreserveWidths : OldColumnsFlag()

    /** Disable forcing columns to fit within window    */
    object NoForceWithinWindow : OldColumnsFlag()

    /** (WIP) Restore pre-1.51 behavior of extending the parent window contents size but _without affecting the columns
     *  width at all_. Will eventually remove.  */
    object GrowParentContentsSize : OldColumnsFlag()

    override val i: Int = 1 shl ordinal

    @GenSealedEnum
    companion object
}

/** Storage data for a single column for legacy Columns() api */
class OldColumnData {
    /** Column start offset, normalized 0f (far left) -> 1f (far right) */
    var offsetNorm = 0f
    var offsetNormBeforeResize = 0f

    /** Not exposed */
    var flags: OldColumnFlags = none
    var clipRect = Rect()
}

/** Storage data for a columns set for legacy Columns() api */
class OldColumns {
    var id: ID = 0
    var flags: OldColumnFlags = none
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