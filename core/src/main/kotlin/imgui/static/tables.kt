package imgui.static

import imgui.*
import imgui.ImGui.fixColumnSortDirection
import imgui.internal.classes.Table
import imgui.internal.classes.TableColumn
import imgui.internal.classes.Window

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


/** Adjust flags: default width mode + stretch columns are not allowed when auto extending
 *
 *  ~static void TableSetupColumnFlags(ImGuiTable* table, ImGuiTableColumn* column, ImGuiTableColumnFlags flags_in) */
fun Table.setupColumnFlags(column: TableColumn, flagsIn: TableColumnFlags = emptyFlags()) {

    var flags = flagsIn

    // Sizing Policy
    if (flags hasnt TableColumnFlag.WidthMask) {
        val tableSizingPolicy = this.flags and TableFlag._SizingMask
        flags = flags or when {
            tableSizingPolicy eq TableFlag.SizingFixedFit || tableSizingPolicy eq TableFlag.SizingFixedSame -> TableColumnFlag.WidthFixed
            else -> TableColumnFlag.WidthStretch
        }
    } else
        assert((flags and TableColumnFlag.WidthMask).isPowerOfTwo) { "Check that only 1 of each set is used." }

    // Resize
    if (this.flags hasnt TableFlag.Resizable)
        flags = flags or TableColumnFlag.NoResize

    // Sorting
    if (flags has TableColumnFlag.NoSortAscending && flags has TableColumnFlag.NoSortDescending)
        flags = flags or TableColumnFlag.NoSort

    // Indentation
    if (flags hasnt TableColumnFlag.IndentMask)
        flags = flags or if (columns.indexOf(column) == 0) TableColumnFlag.IndentEnable else TableColumnFlag.IndentDisable

    // Alignment
    //if ((flags & ImGuiTableColumnFlags_AlignMask_) == 0)
    //    flags |= ImGuiTableColumnFlags_AlignCenter;
    //IM_ASSERT(ImIsPowerOfTwo(flags & ImGuiTableColumnFlags_AlignMask_)); // Check that only 1 of each set is used.

    // Preserve status flags
    column.flags = flags or (column.flags and TableColumnFlag.StatusMask)

    // Build an ordered list of available sort directions
    column.sortDirectionsAvailCount = 0
    column.sortDirectionsAvailMask = 0
    column.sortDirectionsAvailList = 0
    if (this.flags has TableFlag.Sortable) {
        var count = 0
        var mask = 0
        var list = 0
        if (flags has TableColumnFlag.PreferSortAscending && flags hasnt TableColumnFlag.NoSortAscending) {
            mask = mask or (1 shl SortDirection.Ascending.i)
            list = list or (SortDirection.Ascending.i shl (count shl 1))
            count++
        }
        if (flags has TableColumnFlag.PreferSortDescending && flags hasnt TableColumnFlag.NoSortDescending) {
            mask = mask or (1 shl SortDirection.Descending.i)
            list = list or (SortDirection.Descending.i shl (count shl 1))
            count++
        }
        if (flags hasnt TableColumnFlag.PreferSortAscending && flags hasnt TableColumnFlag.NoSortAscending) {
            mask = mask or (1 shl SortDirection.Ascending.i)
            list = list or (SortDirection.Ascending.i shl (count shl 1))
            count++
        }
        if (flags hasnt TableColumnFlag.PreferSortDescending && flags hasnt TableColumnFlag.NoSortDescending) {
            mask = mask or (1 shl SortDirection.Descending.i)
            list = list or (SortDirection.Descending.i shl (count shl 1))
            count++
        }
        if (this.flags has TableFlag.SortTristate || count == 0) {
            mask = mask or (1 shl SortDirection.None.i)
            count++
        }
        column.sortDirectionsAvailList = list
        column.sortDirectionsAvailMask = mask
        column.sortDirectionsAvailCount = count
        fixColumnSortDirection(column)
    }
}

fun tableGetColumnAvailSortDirection(column: TableColumn, n: Int): SortDirection {
    assert(n < column.sortDirectionsAvailCount)
    return SortDirection of ((column.sortDirectionsAvailList ushr (n shl 1)) and 0x03)
}


// Helper
fun tableFixFlags(flags_: TableFlags, outerWindow: Window): TableFlags {

    var flags = flags_

    // Adjust flags: set default sizing policy
    if (flags hasnt TableFlag._SizingMask)
        flags = flags or when {
            flags has TableFlag.ScrollX || outerWindow.flags has WindowFlag.AlwaysAutoResize -> TableFlag.SizingFixedFit
            else -> TableFlag.SizingStretchSame
        }

    // Adjust flags: enable NoKeepColumnsVisible when using ImGuiTableFlags_SizingFixedSame
    if (flags and TableFlag._SizingMask eq TableFlag.SizingFixedSame)
        flags = flags or TableFlag.NoKeepColumnsVisible

    // Adjust flags: enforce borders when resizable
    if (flags has TableFlag.Resizable)
        flags = flags or TableFlag.BordersInnerV

    // Adjust flags: disable NoHostExtendX/NoHostExtendY if we have any scrolling going on
    if (flags has TableFlag.NoHostExtendY && flags has (TableFlag.ScrollX or TableFlag.ScrollY))
        flags = flags wo (TableFlag.NoHostExtendX or TableFlag.NoHostExtendY)

    // Adjust flags: NoBordersInBodyUntilResize takes priority over NoBordersInBody
    if (flags has TableFlag.NoBordersInBodyUntilResize)
        flags = flags wo TableFlag.NoBordersInBody

    // Adjust flags: disable saved settings if there's nothing to save
    if (flags hasnt (TableFlag.Resizable or TableFlag.Hideable or TableFlag.Reorderable or TableFlag.Sortable))
        flags = flags or TableFlag.NoSavedSettings

    // Inherit _NoSavedSettings from top-level window (child windows always have _NoSavedSettings set)
    if (outerWindow.rootWindow!!.flags has WindowFlag.NoSavedSettings)
        flags = flags or TableFlag.NoSavedSettings

    return flags
}