package imgui.internal.classes

import glm_.*
import glm_.vec2.Vec2
import imgui.*
import imgui.internal.*
import imgui.internal.sections.NavLayer
import imgui.TableColumnFlag as Tcf
import imgui.TableFlag as Tf

/** Special sentinel code which cannot be used as a regular color. */
val COL32_DISABLE = COL32(0, 0, 0, 1)

/** sizeof(ImU64) * 8. This is solely because we frequently encode columns set in a ImU64. */
val TABLE_MAX_COLUMNS = 64

/** See TableSetupDrawChannels() */
val TABLE_MAX_DRAW_CHANNELS = 4 + 64 * 2

// Our current column maximum is 64 but we may raise that in the future.
typealias TableColumnIdx = Int
typealias TableDrawChannelIdx = Int

/** [Internal] sizeof() ~ 104
 *  We use the terminology "Enabled" to refer to a column that is not Hidden by user/api.
 *  We use the terminology "Clipped" to refer to a column that is out of sight because of scrolling/clipping.
 *  This is in contrast with some user-facing api such as IsItemVisible() / IsRectVisible() which use "Visible" to mean "not clipped". */
class TableColumn {

    /** Flags after some patching (not directly same as provided by user). See ImGuiTableColumnFlags_ */
    var flags = Tcf.None.i

    /** Final/actual width visible == (MaxX - MinX), locked in TableUpdateLayout(). May be > WidthRequest to honor minimum width, may be < WidthRequest to honor shrinking columns down in tight space. */
    var widthGiven = 0f

    /** Absolute positions */
    var minX = 0f

    /** Absolute positions */
    var maxX = 0f

    /** Master width absolute value when !(Flags & _WidthStretch). When Stretch this is derived every frame from StretchWeight in TableUpdateLayout() */
    var widthRequest = -1f

    /** Automatic width */
    var widthAuto = 0f

    /** Master width weight when (Flags & _WidthStretch). Often around ~1.0f initially. */
    var stretchWeight = -1f

    /** Value passed to TableSetupColumn(). For Width it is a content width (_without padding_). */
    var initStretchWeightOrWidth = 0f

    /** Clipping rectangle for the column */
    val clipRect = Rect()

    /** Optional, value passed to TableSetupColumn() */
    var userID: ID = 0

    /** Contents region min ~(MinX + CellPaddingX + CellSpacingX1) == cursor start position when entering column */
    var workMinX = 0f

    /** Contents region max ~(MaxX - CellPaddingX - CellSpacingX2) */
    var workMaxX = 0f

    /** Current item width for the column, preserved across rows */
    var itemWidth = 0f

    /** Contents maximum position for frozen rows (apart from headers), from which we can infer content width. */
    var contentMaxXFrozen = 0f

    var contentMaxXUnfrozen = 0f

    /** Contents maximum position for headers rows (regardless of freezing). TableHeader() automatically softclip itself + report ideal desired size, to avoid creating extraneous draw calls */
    var contentMaxXHeadersUsed = 0f

    var contentMaxXHeadersIdeal = 0f

    /** Offset into parent ColumnsNames[] */
    var nameOffset = -1

    /** Index within Table's IndexToDisplayOrder[] (column may be reordered by users) */
    var displayOrder: TableColumnIdx = -1

    /** Index within enabled/visible set (<= IndexToDisplayOrder) */
    var indexWithinEnabledSet: TableColumnIdx = -1

    /** Index of prev enabled/visible column within Columns[], -1 if first enabled/visible column */
    var prevEnabledColumn: TableColumnIdx = -1

    /** Index of next enabled/visible column within Columns[], -1 if last enabled/visible column */
    var nextEnabledColumn: TableColumnIdx = -1

    /** Index of this column within sort specs, -1 if not sorting on this column, 0 for single-sort, may be >0 on multi-sort */
    var sortOrder: TableColumnIdx = -1

    /** Index within DrawSplitter.Channels[] */
    var drawChannelCurrent: TableDrawChannelIdx = -1

    var drawChannelFrozen: TableDrawChannelIdx = -1

    var drawChannelUnfrozen: TableDrawChannelIdx = -1

    /** Is the column not marked Hidden by the user? (even if off view, e.g. clipped by scrolling). */
    var isEnabled = false

    var isEnabledNextFrame = false

    /** Is actually in view (e.g. overlapping the host window clipping rectangle, not scrolled). */
    var isVisibleX = false

    var isVisibleY = false

    /** Return value for TableSetColumnIndex() / TableNextColumn(): whether we request user to output contents or not. */
    var isRequestOutput = false

    /** Do we want item submissions to this column to be completely ignored (no layout will happen). */
    var isSkipItems = false

    var isPreserveWidthAuto = false

    /** ImGuiNavLayer in 1 byte */
    var navLayerCurrent = NavLayer.Main

    /** Queue of 8 values for the next 8 frames to request auto-fit */
    var autoFitQueue = 0

    /** Queue of 8 values for the next 8 frames to disable Clipped/SkipItem */
    var cannotSkipItemsQueue = 0

    /** ImGuiSortDirection_Ascending or ImGuiSortDirection_Descending */
    var sortDirection = SortDirection.None

    /** Number of available sort directions (0 to 3) */
    var sortDirectionsAvailCount = 0

    /** Mask of available sort directions (1-bit each) */
    var sortDirectionsAvailMask = 0

    /** Ordered of available sort directions (2-bits each) */
    var sortDirectionsAvailList = 0
}

/** Temporary storage for one table (one per table in the stack), shared between tables.
 *
 *  Transient data that are only needed between BeginTable() and EndTable(), those buffers are shared.
 *  Accessing those requires chasing an extra pointer so for very frequently used data we leave them in the main table structure.
 *  FIXME-TABLE: more transient data could be stored here: DrawSplitter (!), SortSpecs? incoming RowData? */
class TableTempData {

    /** Index in g.Tables.Buf[] pool */
    var tableIndex = 0

    /** outer_size.x passed to BeginTable() */
    val userOuterSize = Vec2()

    /** Backup of InnerWindow->WorkRect at the end of BeginTable() */
    val hostBackupWorkRect = Rect()
    /** Backup of InnerWindow->ParentWorkRect at the end of BeginTable() */
    val hostBackupParentWorkRect = Rect()
    /** Backup of InnerWindow->DC.PrevLineSize at the end of BeginTable() */
    val hostBackupPrevLineSize = Vec2()
    /** Backup of InnerWindow->DC.CurrLineSize at the end of BeginTable() */
    val hostBackupCurrLineSize = Vec2()
    /** Backup of InnerWindow->DC.CursorMaxPos at the end of BeginTable() */
    val hostBackupCursorMaxPos = Vec2()
    /** Backup of OuterWindow->DC.ColumnsOffset at the end of BeginTable() */
    var hostBackupColumnsOffset = 0f
    /** Backup of OuterWindow->DC.ItemWidth at the end of BeginTable() */
    var hostBackupItemWidth = 0f
    /** Backup of OuterWindow->DC.ItemWidthStack.Size at the end of BeginTable() */
    var hostBackupItemWidthStackSize = 0
}

/** Transient cell data stored per row.
 *  sizeof() ~ 6 */
class TableCellData {
    /** Actual color */
    var bgColor = 0

    /** Column number */
    var column: TableColumnIdx = 0
}

/** sizeof() ~ 12 */
class TableColumnSettings {
    var widthOrWeight = 0f
    var userID: ID = 0
    var index: TableColumnIdx = -1
    var displayOrder: TableColumnIdx = -1
    var sortOrder: TableColumnIdx = -1
    var sortDirection = SortDirection.None

    /** "Visible" in ini file */
    var isEnabled = true
    var isStretch = false
}

/** This is designed to be stored in a single ImChunkStream (1 header followed by N ImGuiTableColumnSettings, etc.) */
class TableSettings

/** ~TableSettingsCreate */
constructor(
        /** Set to 0 to invalidate/delete the setting */
        var id: ID = 0,

        var columnsCount: TableColumnIdx = 0) {

    /** Indicate data we want to save using the Resizable/Reorderable/Sortable/Hideable flags (could be using its own flags..) */
    var saveFlags = Tf.None.i

    /** Reference scale to be able to rescale columns on font/dpi changes. */
    var refScale = 0f

    /** Maximum number of columns this settings instance can store, we can recycle a settings instance with lower number of columns but not higher */
    var columnsCountMax: TableColumnIdx = 0

    /** Set when loaded from .ini data (to enable merging/loading .ini data into an already running context) */
    var wantApply = false

    var columnSettings = Array(columnsCountMax) { TableColumnSettings() }

    /** ~TableSettingsInit */
    fun init(id: ID, columnsCount: Int, columnsCountMax: Int) {
        this.id = id
        this.columnsCount = columnsCount
        this.columnsCountMax = columnsCountMax
        columnSettings = Array(columnsCountMax) { TableColumnSettings() }
        wantApply = true
    }
}


//-----------------------------------------------------------------------------
// [SECTION] Tables: Main code
//-----------------------------------------------------------------------------

// Configuration

val TABLE_DRAW_CHANNEL_BG0 = 0
val TABLE_DRAW_CHANNEL_BG2_FROZEN = 1

/** When using ImGuiTableFlags_NoClip (this becomes the last visible channel) */
val TABLE_DRAW_CHANNEL_NOCLIP = 2

/** FIXME-TABLE: Currently hard-coded because of clipping assumptions with outer borders rendering. */
val TABLE_BORDER_SIZE = 1f

/** Extend outside inner borders. */
val TABLE_RESIZE_SEPARATOR_HALF_THICKNESS = 4f

/** Delay/timer before making the hover feedback (color+cursor) visible because tables/columns tends to be more cramped. */
val TABLE_RESIZE_SEPARATOR_FEEDBACK_TIMER = 0.06f


// Helper
fun tableFixFlags(flags_: TableFlags, outerWindow: Window): TableFlags {

    var flags = flags_

    // Adjust flags: set default sizing policy
    if (flags hasnt Tf._SizingMask)
        flags = flags or when {
            flags has Tf.ScrollX || outerWindow.flags has WindowFlag.AlwaysAutoResize -> Tf.SizingFixedFit
            else -> Tf.SizingStretchSame
        }

    // Adjust flags: enable NoKeepColumnsVisible when using ImGuiTableFlags_SizingFixedSame
    if (flags and Tf._SizingMask == Tf.SizingFixedSame.i)
        flags = flags or Tf.NoKeepColumnsVisible

    // Adjust flags: enforce borders when resizable
    if (flags has Tf.Resizable)
        flags = flags or Tf.BordersInnerV

    // Adjust flags: disable NoHostExtendX/NoHostExtendY if we have any scrolling going on
    if (flags has Tf.NoHostExtendY && flags has (Tf.ScrollX or Tf.ScrollY))
        flags = flags wo (Tf.NoHostExtendX or Tf.NoHostExtendY)

    // Adjust flags: NoBordersInBodyUntilResize takes priority over NoBordersInBody
    if (flags has Tf.NoBordersInBodyUntilResize)
        flags = flags wo Tf.NoBordersInBody

    // Adjust flags: disable saved settings if there's nothing to save
    if (flags hasnt (Tf.Resizable or Tf.Hideable or Tf.Reorderable or Tf.Sortable))
        flags = flags or Tf.NoSavedSettings

    // Inherit _NoSavedSettings from top-level window (child windows always have _NoSavedSettings set)
//    #ifdef IMGUI_HAS_DOCK
//        ImGuiWindow* window_for_settings = outerWindow->RootWindowDockStop
//    #else
    val windowForSettings = outerWindow.rootWindow!!
//    #endif
    if (windowForSettings.flags has WindowFlag.NoSavedSettings)
        flags = flags or Tf.NoSavedSettings

    return flags
}