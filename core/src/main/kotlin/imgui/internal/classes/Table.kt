package imgui.internal.classes

import imgui.ID
import imgui.TableFlags
import imgui.TableRowFlags
import imgui.classes.TableColumnSortSpecs
import imgui.classes.TableSortSpecs
import imgui.emptyFlags
import imgui.internal.DrawListSplitter


/** FIXME-TABLE: more transient data could be stored in a stacked ImGuiTableTempData: e.g. SortSpecs, incoming RowData */
class Table {

    var id: ID = 0

    var flags: TableFlags = emptyFlags()

    /** Single allocation to hold Columns[], DisplayOrderToIndex[] and RowCellData[] */
    //    var rawData: Any? = null

    /** Transient data while table is active. Point within g.CurrentTableStack[] */
    var tempData: TableTempData? = null

    /** Point within RawData[] */
    val columns = ArrayList<TableColumn>()

    /** Point within RawData[]. Store display order of columns (when not reordered, the values are 0...Count-1) */
    var displayOrderToIndex = intArrayOf()

    /** Point within RawData[]. Store cells background requests for current row. */
    var rowCellData = arrayOf<TableCellData>()

    /** Column DisplayOrder -> IsEnabled map */
    var enabledMaskByDisplayOrder = 0L

    /** Column Index -> IsEnabled map (== not hidden by user/api) in a format adequate for iterating column without touching cold data */
    var enabledMaskByIndex = 0L

    /** Column Index -> IsVisibleX|IsVisibleY map (== not hidden by user/api && not hidden by scrolling/cliprect) */
    var visibleMaskByIndex = 0L

    /** Column Index -> IsVisible || AutoFit (== expect user to submit items) */
    var requestOutputMaskByIndex = 0L

    /** Which data were loaded from the .ini file (e.g. when order is not altered we won't save order) */
    var settingsLoadedFlags: TableFlags = emptyFlags()

    /** Offset in g.SettingsTables */
    var settingsOffset = 0

    var lastFrameActive = -1

    /** Number of columns declared in BeginTable() */
    var columnsCount = 0

    var currentRow = 0

    var currentColumn = 0

    /** Count of BeginTable() calls with same ID in the same frame (generally 0). This is a little bit similar to BeginCount for a window, but multiple table with same ID look are multiple tables, they are just synched. */
    var instanceCurrent = 0

    /** Mark which instance (generally 0) of the same ID is being interacted with */
    var instanceInteracted = 0

    var rowPosY1 = 0f

    var rowPosY2 = 0f

    /** Height submitted to TableNextRow() */
    var rowMinHeight = 0f

    var rowTextBaseline = 0f

    var rowIndentOffsetX = 0f

    /** Current row flags, see ImGuiTableRowFlags_ */
    var rowFlags: TableRowFlags = emptyFlags()

    var lastRowFlags: TableRowFlags = emptyFlags()

    /** Counter for alternating background colors (can be fast-forwarded by e.g clipper), not same as CurrentRow because header rows typically don't increase this. */
    var rowBgColorCounter = 0

    val rowBgColor = IntArray(2)              // Background color override for current row.

    var borderColorStrong = 0

    var borderColorLight = 0

    var borderX1 = 0f

    var borderX2 = 0f

    var hostIndentX = 0f

    var minColumnWidth = 0f

    var outerPaddingX = 0f

    /** Padding from each borders */
    var cellPaddingX = 0f

    /** Padding from each borders */
    var cellPaddingY = 0f

    /** Spacing between non-bordered cells */
    var cellSpacingX1 = 0f

    /** Spacing between non-bordered cells */
    var cellSpacingX2 = 0f

    /** User value passed to BeginTable(), see comments at the top of BeginTable() for details. */
    var innerWidth = 0f

    /** Sum of current column width */
    var columnsGivenWidth = 0f

    /** Sum of ideal column width in order nothing to be clipped, used for auto-fitting and content width submission in outer window */
    var columnsAutoFitWidth = 0f

    /** Sum of weight of all enabled stretching columns */
    var columnsStretchSumWeights = 0f

    var resizedColumnNextWidth = 0f

    /** Lock minimum contents width while resizing down in order to not create feedback loops. But we allow growing the table. */
    var resizeLockMinContentsX2 = 0f

    /** Reference scale to be able to rescale columns on font/dpi changes. */
    var refScale = 0f

    /** Note: for non-scrolling table, OuterRect.Max.y is often FLT_MAX until EndTable(), unless a height has been specified in BeginTable(). */
    val outerRect = Rect()

    /** InnerRect but without decoration. As with OuterRect, for non-scrolling tables, InnerRect.Max.y is */
    val innerRect = Rect()

    val workRect = Rect()

    var innerClipRect = Rect()

    /** We use this to cpu-clip cell background color fill, evolve during the frame as we cross frozen rows boundaries */
    val bgClipRect = Rect()

    /** Actual ImDrawCmd clip rect for BG0/1 channel. This tends to be == OuterWindow->ClipRect at BeginTable() because output in BG0/BG1 is cpu-clipped */
    val bg0ClipRectForDrawCmd = Rect()

    /** Actual ImDrawCmd clip rect for BG2 channel. This tends to be a correct, tight-fit, because output to BG2 are done by widgets relying on regular ClipRect. */
    val bg2ClipRectForDrawCmd = Rect()

    /** This is used to check if we can eventually merge our columns draw calls into the current draw call of the current window. */
    val hostClipRect = Rect()

    /** Backup of InnerWindow->ClipRect during PushTableBackground()/PopTableBackground() */
    val hostBackupInnerClipRect = Rect()

    /** Parent window for the table */
    var outerWindow: Window? = null

    /** Window holding the table data (== OuterWindow or a child window) */
    var innerWindow: Window? = null

    /** Contiguous buffer holding columns names */
    val columnsNames = ArrayList<String>()

    /** Shortcut to TempData->DrawSplitter while in table. Isolate draw commands per columns to avoid switching clip rect constantly */
    var drawSplitter = DrawListSplitter()

    var instanceDataFirst = TableInstanceData()
    val instanceDataExtra = ArrayList<TableInstanceData>()  // FIXME-OPT: Using a small-vector pattern would be good.

    lateinit var sortSpecsSingle: TableColumnSortSpecs

    val sortSpecsMulti = ArrayList<TableColumnSortSpecs>()     // FIXME-OPT: Using a small-vector pattern would be good.

    /** Public facing sorts specs, this is what we return in TableGetSortSpecs() */
    val sortSpecs = TableSortSpecs()

    var sortSpecsCount: TableColumnIdx = 0

    /** Number of enabled columns (<= ColumnsCount) */
    var columnsEnabledCount: TableColumnIdx = 0

    /** Number of enabled columns (<= ColumnsCount) */
    var columnsEnabledFixedCount: TableColumnIdx = 0

    /** Count calls to TableSetupColumn() */
    var declColumnsCount: TableColumnIdx = 0

    /** Index of column whose visible region is being hovered. Important: == ColumnsCount when hovering empty region after the right-most column! */
    var hoveredColumnBody: TableColumnIdx = 0

    /** Index of column whose right-border is being hovered (for resizing). */
    var hoveredColumnBorder: TableColumnIdx = 0

    /** Index of single column requesting auto-fit. */
    var autoFitSingleColumn: TableColumnIdx = 0

    /** Index of column being resized. Reset when InstanceCurrent==0. */
    var resizedColumn: TableColumnIdx = 0

    /** Index of column being resized from previous frame. */
    var lastResizedColumn: TableColumnIdx = 0

    /** Index of column header being held. */
    var heldHeaderColumn: TableColumnIdx = 0

    /** Index of column being reordered. (not cleared) */
    var reorderColumn: TableColumnIdx = 0

    /** -1 or +1 */
    var reorderColumnDir: TableColumnIdx = 0

    /** Index of left-most non-hidden column. */
    var leftMostEnabledColumn: TableColumnIdx = 0

    /** Index of right-most non-hidden column. */
    var rightMostEnabledColumn: TableColumnIdx = 0

    /** Index of left-most stretched column. */
    var leftMostStretchedColumn: TableColumnIdx = 0

    /** Index of right-most stretched column. */
    var rightMostStretchedColumn: TableColumnIdx = 0

    /** Column right-clicked on, of -1 if opening context menu from a neutral/empty spot */
    var contextPopupColumn: TableColumnIdx = 0

    /** Requested frozen rows count */
    var freezeRowsRequest: TableColumnIdx = 0

    /** Actual frozen row count (== FreezeRowsRequest, or == 0 when no scrolling offset) */
    var freezeRowsCount: TableColumnIdx = 0

    /** Requested frozen columns count */
    var freezeColumnsRequest: TableColumnIdx = 0

    /** Actual frozen columns count (== FreezeColumnsRequest, or == 0 when no scrolling offset) */
    var freezeColumnsCount: TableColumnIdx = 0

    /** Index of current RowCellData[] entry in current row */
    var rowCellDataCurrent: TableColumnIdx = 0

    /** Redirect non-visible columns here. */
    var dummyDrawChannel: TableDrawChannelIdx = 0

    /** For Selectable() and other widgets drawing across columns after the freezing line. Index within DrawSplitter.Channels[] */
    var bg2DrawChannelCurrent: TableDrawChannelIdx = 0

    var bg2DrawChannelUnfrozen: TableDrawChannelIdx = 0

    /** Set by TableUpdateLayout() which is called when beginning the first row. */
    var isLayoutLocked = false

    /** Set when inside TableBeginRow()/TableEndRow(). */
    var isInsideRow = false

    var isInitializing = false

    var isSortSpecsDirty = false

    /** Set when the first row had the ImGuiTableRowFlags_Headers flag. */
    var isUsingHeaders = false

    /** Set when default context menu is open (also see: ContextPopupColumn, InstanceInteracted). */
    var isContextPopupOpen = false

    var isSettingsRequestLoad = false

    /** Set when table settings have changed and needs to be reported into ImGuiTableSetttings data. */
    var isSettingsDirty = false

    /** Set when display order is unchanged from default (DisplayOrder contains 0...Count-1) */
    var isDefaultDisplayOrder = false

    var isResetAllRequest = false

    var isResetDisplayOrderRequest = false

    /** Set when we got past the frozen row. */
    var isUnfrozenRows = false

    /** Set if user didn't explicitly set a sizing policy in BeginTable() */
    var isDefaultSizingPolicy = false

    /** Whether ANY instance of this table had a vertical scrollbar during the current frame. */
    var hasScrollbarYCurr = false

    /** Whether ANY instance of this table had a vertical scrollbar during the previous. */
    var hasScrollbarYPrev = false

    var memoryCompacted = false

    /** Backup of InnerWindow->SkipItem at the end of BeginTable(), because we will overwrite InnerWindow->SkipItem on a per-column basis */
    var hostSkipItems = false
}