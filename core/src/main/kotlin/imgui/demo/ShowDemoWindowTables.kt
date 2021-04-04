package imgui.demo

import glm_.f
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import glm_.wo
import imgui.*
import imgui.ImGui.alignTextToFramePadding
import imgui.ImGui.beginTable
import imgui.ImGui.button
import imgui.ImGui.calcTextSize
import imgui.ImGui.checkbox
import imgui.ImGui.checkboxFlags
import imgui.ImGui.closeCurrentPopup
import imgui.ImGui.collapsingHeader
import imgui.ImGui.combo
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.dragFloat
import imgui.ImGui.dragInt
import imgui.ImGui.dragVec2
import imgui.ImGui.endTable
import imgui.ImGui.indent
import imgui.ImGui.inputText
import imgui.ImGui.io
import imgui.ImGui.openPopup
import imgui.ImGui.popButtonRepeat
import imgui.ImGui.popID
import imgui.ImGui.popItemWidth
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushButtonRepeat
import imgui.ImGui.pushID
import imgui.ImGui.pushItemWidth
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.radioButton
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setNextItemOpen
import imgui.ImGui.setNextItemWidth
import imgui.ImGui.sliderFloat
import imgui.ImGui.sliderVec2
import imgui.ImGui.smallButton
import imgui.ImGui.spacing
import imgui.ImGui.style
import imgui.ImGui.tableGetColumnFlags
import imgui.ImGui.tableGetColumnIndex
import imgui.ImGui.tableGetColumnName
import imgui.ImGui.tableGetRowIndex
import imgui.ImGui.tableGetSortSpecs
import imgui.ImGui.tableHeader
import imgui.ImGui.tableHeadersRow
import imgui.ImGui.tableNextColumn
import imgui.ImGui.tableNextRow
import imgui.ImGui.tableSetBgColor
import imgui.ImGui.tableSetColumnIndex
import imgui.ImGui.tableSetupColumn
import imgui.ImGui.tableSetupScrollFreeze
import imgui.ImGui.text
import imgui.ImGui.textDisabled
import imgui.ImGui.textLineHeightWithSpacing
import imgui.ImGui.textUnformatted
import imgui.ImGui.textWrapped
import imgui.ImGui.treeNodeEx
import imgui.ImGui.treePop
import imgui.ImGui.unindent
import imgui.api.demoDebugInformations.Companion.helpMarker
import imgui.classes.DrawList
import imgui.classes.ListClipper
import imgui.classes.TableSortSpecs
import imgui.dsl.popup
import imgui.dsl.popupContextItem
import imgui.dsl.table
import imgui.dsl.tooltip
import imgui.dsl.treeNode
import imgui.dsl.treeNodeEx
import imgui.dsl.useCombo
import imgui.dsl.withID
import imgui.dsl.withItemWidth
import imgui.dsl.withTextWrapPos
import imgui.TableColumnFlag as Tcf
import imgui.TableFlag as Tf
import imgui.TableRowFlag as Trf
import imgui.TreeNodeFlag as Tnf

object ShowDemoWindowTables {

    val TEXT_BASE_HEIGHT = textLineHeightWithSpacing

    /** Make the UI compact because there are so many fields
     *  ~PushStyleCompact */
    inline fun pushingStyleCompact(block: () -> Unit) {
        pushStyleVar(StyleVar.FramePadding, Vec2(style.framePadding.x, (style.framePadding.y * 0.6f).i.f))
        pushStyleVar(StyleVar.ItemSpacing, Vec2(style.itemSpacing.x, (style.itemSpacing.y * 0.6f).i.f))
        block()
        popStyleVar(2)
    }

    class EnumDesc(val value: TableFlags, val name: String, val tooltip: String)

    val policies = arrayOf(
        EnumDesc(Tf.None.i, "Default", "Use default sizing policy:\n- ImGuiTableFlags_SizingFixedFit if ScrollX is on or if host window has ImGuiWindowFlags_AlwaysAutoResize.\n- ImGuiTableFlags_SizingStretchSame otherwise."),
        EnumDesc(Tf.SizingFixedFit.i, "ImGuiTableFlags_SizingFixedFit", "Columns default to _WidthFixed (if resizable) or _WidthAuto (if not resizable), matching contents width."),
        EnumDesc(Tf.SizingFixedSame.i, "ImGuiTableFlags_SizingFixedSame", "Columns are all the same width, matching the maximum contents width.\nImplicitly disable ImGuiTableFlags_Resizable and enable ImGuiTableFlags_NoKeepColumnsVisible."),
        EnumDesc(Tf.SizingStretchProp.i, "ImGuiTableFlags_SizingStretchProp", "Columns default to _WidthStretch with weights proportional to their widths."),
        EnumDesc(Tf.SizingStretchSame.i, "ImGuiTableFlags_SizingStretchSame", "Columns default to _WidthStretch with same weights."))

    /** Show a combo box with a choice of sizing policies */
    fun editTableSizingFlags(flags: IntArray, ptr: Int) {
        var flag = flags[ptr]
        val idx = policies.indexOfFirst { it.value == flag and Tf._SizingMask }
        val previewText = policies.getOrNull(idx)?.name?.substringAfter("ImGuiTableFlags") ?: ""
        useCombo("Sizing Policy", previewText) {
            for (n in policies.indices)
                if (selectable(policies[n].name, idx == n))
                    flag = (flag wo Tf._SizingMask.i) or policies[n].value
        }
        sameLine()
        textDisabled("(?)")
        if (ImGui.isItemHovered())
            tooltip {
                withTextWrapPos(ImGui.fontSize * 50f) {
                    for (m in policies.indices) {
                        separator()
                        text("${policies[m].name}:")
                        separator()
                        ImGui.cursorPosX = ImGui.cursorPosX + style.indentSpacing * 0.5f
                        textUnformatted(policies[m].tooltip)
                    }
                }
            }
    }

    fun editTableColumnsFlags(flags: TableColumnFlags): TableColumnFlags {
        _i = flags
        checkboxFlags("_DefaultHide", ::_i, Tcf.DefaultHide.i)
        checkboxFlags("_DefaultSort", ::_i, Tcf.DefaultSort.i)
        if (checkboxFlags("_WidthStretch", ::_i, Tcf.WidthStretch.i))
            _i = _i wo (Tcf.WidthMask_ xor Tcf.WidthStretch)
        if (checkboxFlags("_WidthFixed", ::_i, Tcf.WidthFixed.i))
            _i = _i wo (Tcf.WidthMask_ xor Tcf.WidthFixed)
        checkboxFlags("_NoResize", ::_i, Tcf.NoResize.i)
        checkboxFlags("_NoReorder", ::_i, Tcf.NoReorder.i)
        checkboxFlags("_NoHide", ::_i, Tcf.NoHide.i)
        checkboxFlags("_NoClip", ::_i, Tcf.NoClip.i)
        checkboxFlags("_NoSort", ::_i, Tcf.NoSort.i)
        checkboxFlags("_NoSortAscending", ::_i, Tcf.NoSortAscending.i)
        checkboxFlags("_NoSortDescending", ::_i, Tcf.NoSortDescending.i)
        checkboxFlags("_NoHeaderWidth", ::_i, Tcf.NoHeaderWidth.i)
        checkboxFlags("_PreferSortAscending", ::_i, Tcf.PreferSortAscending.i)
        checkboxFlags("_PreferSortDescending", ::_i, Tcf.PreferSortDescending.i)
        checkboxFlags("_IndentEnable", ::_i, Tcf.IndentEnable.i); sameLine(); helpMarker("Default for column 0")
        checkboxFlags("_IndentDisable", ::_i, Tcf.IndentDisable.i); sameLine(); helpMarker("Default for column >0")
        return _i
    }

    fun showTableColumnsStatusFlags(flags: TableColumnFlags) {
        _i = flags
        checkboxFlags("_IsEnabled", ::_i, Tcf.IsEnabled.i)
        checkboxFlags("_IsVisible", ::_i, Tcf.IsVisible.i)
        checkboxFlags("_IsSorted", ::_i, Tcf.IsSorted.i)
        checkboxFlags("_IsHovered", ::_i, Tcf.IsHovered.i)
    }

    /* Borders, background */
    enum class ContentsType0 { Text, FillButton }

    var flags0 = Tf.Borders or Tf.RowBg
    var displayHeaders = false
    var contentsType0 = ContentsType0.Text.ordinal

    /* Resizable, stretch */
    var flags1 = Tf.SizingStretchSame or Tf.Resizable or Tf.BordersOuter or Tf.BordersV or Tf.ContextMenuInBody

    /* Resizable, fixed */
    var flags2 = Tf.SizingFixedFit or Tf.Resizable or Tf.BordersOuter or Tf.BordersV or Tf.ContextMenuInBody
    var fixedFill0 = true

    /* Resizable, mixed */
    var flags3 = Tf.SizingFixedFit or Tf.RowBg or Tf.Borders or Tf.Resizable or Tf.Reorderable or Tf.Hideable

    /* Reorderable, hideable, with headers */
    var flags4 = Tf.Resizable or Tf.Reorderable or Tf.Hideable or Tf.BordersOuter or Tf.BordersV

    /* Padding */
    var flags5 = Tf.BordersV.i
    var showHeaders0 = false
    var flags6 = Tf.Borders or Tf.RowBg
    val cellPadding = Vec2()
    var showWidgetFrameBg = true
    val textBufs = Array(3 * 5) { "" } // Mini text storage for 3x5 cells
    var init = true

    /* Sizing policies */
    var fixedFill1 = true
    var flags7 = Tf.BordersV or Tf.BordersOuterH or Tf.RowBg or Tf.ContextMenuInBody
    val sizingPolicyFlags = intArrayOf(Tf.SizingFixedFit.i, Tf.SizingFixedSame.i, Tf.SizingStretchProp.i, Tf.SizingStretchSame.i)

    enum class ContentsType1 { ShowWidth, ShortText, LongText, Button, FillButton, InputText }

    var flags8 = Tf.ScrollY or Tf.Borders or Tf.RowBg or Tf.Resizable
    var contentsType1 = ContentsType1.ShowWidth
    var columnCount0 = 3
    val textBuf = ByteArray(32)

    /* Vertical scrolling, with clipping */
    var flags9 = Tf.ScrollY or Tf.RowBg or Tf.BordersOuter or Tf.BordersV or Tf.Resizable or Tf.Reorderable or Tf.Hideable

    /* Horizontal scrolling */
    var flags10 = Tf.ScrollX or Tf.ScrollY or Tf.RowBg or Tf.BordersOuter or Tf.BordersV or Tf.Resizable or Tf.Reorderable or Tf.Hideable
    var freezeCols0 = 1
    var freezeRows0 = 1

    /* Stretch + ScrollX */
    var flags11 = Tf.SizingStretchSame or Tf.ScrollX or Tf.ScrollY or Tf.BordersOuter or Tf.RowBg or Tf.ContextMenuInBody
    var innerWidth0 = 1000f

    /* Columns flags */
    var columnCount1 = 3
    val columnNames = arrayOf("One", "Two", "Three")
    val columnFlags = intArrayOf(Tcf.DefaultSort.i, Tcf.None.i, Tcf.DefaultHide.i)
    val columnFlagsOut = IntArray(columnCount1) // Output from TableGetColumnFlags()
    var flags12 = Tf.SizingFixedFit or Tf.ScrollX or Tf.ScrollY or Tf.RowBg or Tf.BordersOuter or Tf.BordersV or
            Tf.Resizable or Tf.Reorderable or Tf.Hideable or Tf.Sortable

    /* Columns widths */
    var flags13 = Tf.Borders or Tf.NoBordersInBodyUntilResize
    var flags14 = Tf.None.i

    /* Outer size */
    var flags15 = Tf.Borders or Tf.Resizable or Tf.ContextMenuInBody or Tf.RowBg or Tf.SizingFixedFit
    var fixedFill2 = false

    /* Background color */
    var flags16 = Tf.RowBg.i
    var rowBgType = 1
    var rowBgTarget = 1
    var cellBgType = 1

    /* Tree view */
    var flags17 = Tf.BordersV or Tf.BordersOuterH or Tf.Resizable or Tf.RowBg or Tf.NoBordersInBody

    /* Item width */
    var dummyF = 0f

    // Simple storage to output a dummy file-system.
    class MyTreeNode(val name: String, val type: String, val size: Int, val childIdx: Int, val childCount: Int) {
        companion object {
            fun displayNode(nodes: Array<MyTreeNode>, index: Int) {
                tableNextRow()
                tableNextColumn()
                val node = nodes[index]
                val isFolder = node.childCount > 0
                if (isFolder) {
                    val open = treeNodeEx(node.name, Tnf.SpanFullWidth.i)
                    tableNextColumn()
                    textDisabled("--")
                    tableNextColumn()
                    textUnformatted(node.type)
                    if (open) {
                        for (childN in 0 until node.childCount)
                            displayNode(nodes, node.childIdx + childN)
                        treePop()
                    }
                } else {
                    treeNodeEx(node.name, Tnf.Leaf or Tnf.Bullet or Tnf.NoTreePushOnOpen or Tnf.SpanFullWidth)
                    tableNextColumn()
                    text("${node.size}")
                    tableNextColumn()
                    textUnformatted(node.type)
                }
            }
        }
    }

    val nodes = arrayOf(
        MyTreeNode("Root", "Folder", -1, 1, 3), // 0
        MyTreeNode("Music", "Folder", -1, 4, 2), // 1
        MyTreeNode("Textures", "Folder", -1, 6, 3), // 2
        MyTreeNode("desktop.ini", "System file", 1024, -1, -1), // 3
        MyTreeNode("File1_a.wav", "Audio file", 123000, -1, -1), // 4
        MyTreeNode("File1_b.wav", "Audio file", 456000, -1, -1), // 5
        MyTreeNode("Image001.png", "Image file", 203128, -1, -1), // 6
        MyTreeNode("Copy of Image001.png", "Image file", 203256, -1, -1), // 7
        MyTreeNode("Copy of Image001 (Final2).png", "Image file", 203512, -1, -1)) // 8

    /* Custom headers */
    val columnSelected = BooleanArray(3)

    /* Context menus */
    var flags18 = Tf.Resizable or Tf.Reorderable or Tf.Hideable or Tf.Borders or Tf.ContextMenuInBody

    /* Sorting */
    var flags19 = Tf.Resizable or Tf.Reorderable or Tf.Hideable or Tf.Sortable or Tf.SortMulti or
            Tf.RowBg or Tf.BordersOuter or Tf.BordersV or Tf.NoBordersInBody or Tf.ScrollY
    val templateItemsNames = arrayOf(
        "Banana", "Apple", "Cherry", "Watermelon", "Grapefruit", "Strawberry", "Mango",
        "Kiwi", "Orange", "Pineapple", "Blueberry", "Plum", "Coconut", "Pear", "Apricot")

    // We are passing our own identifier to TableSetupColumn() to facilitate identifying columns in the sorting code.
    // This identifier will be passed down into ImGuiTableSortSpec::ColumnUserID.
    // But it is possible to omit the user id parameter of TableSetupColumn() and just use the column index instead! (ImGuiTableSortSpec::ColumnIndex)
    // If you don't use sorting, you will generally never care about giving column an ID!
    enum class MyItemColumnID { ID, Name, Action, Quantity, Description }
    class MyItem(var id: ID = 0, var name: String = "", var quantity: Int = 0) {
        companion object {
            // We have a problem which is affecting _only this demo_ and should not affect your code:
            // As we don't rely on std:: or other third-party library to compile dear imgui, we only have reliable access to qsort(),
            // however qsort doesn't allow passing user data to comparing function.
            // As a workaround, we are storing the sort specs in a static/global for the comparing function to access.
            // In your own use case you would probably pass the sort specs to your sorting/comparing functions directly and not use a global.
            // We could technically call ImGui::TableGetSortSpecs() in CompareWithSortSpecs(), but considering that this function is called
            // very often by the sorting algorithm it would be a little wasteful.
            var currentSortSpecs: TableSortSpecs? = null

            // Compare function to be used by qsort()
            var res = 0
            val compareWithSortSpecs = Comparator<MyItem> { a, b ->
                for (n in 0 until currentSortSpecs!!.specsCount) {
                    // Here we identify columns using the ColumnUserID value that we ourselves passed to TableSetupColumn()
                    // We could also choose to identify columns based on their index (sort_spec->ColumnIndex), which is simpler!
                    val sortSpec = currentSortSpecs!!.specs(n)
                    val delta = when (sortSpec.columnUserID) {
                        MyItemColumnID.ID.ordinal -> a.id - b.id
                        MyItemColumnID.Name.ordinal -> a.name.compareTo(b.name)
                        MyItemColumnID.Quantity.ordinal -> a.quantity - b.quantity
                        MyItemColumnID.Description.ordinal -> a.name.compareTo(b.name)
                        else -> error("invalid")
                    }
                    if (delta > 0)
                        res = if (sortSpec.sortDirection == SortDirection.Ascending) +1 else -1
                    else if (delta < 0)
                        res = if (sortSpec.sortDirection == SortDirection.Ascending) -1 else +1
                }

                // qsort() is instable so always return a way to differenciate items.
                // Your own compare function may want to avoid fallback on implicit sort specs e.g. a Name compare if it wasn't already part of the sort specs.
                if (res == 0)
                    a.id - b.id
                else res
            }
        }
    }

    //    const ImGuiTableSortSpecs* MyItem::s_current_sort_specs = NULL
    // Create item list
    var items0 = Array(50) { n ->
        val templateN = n % templateItemsNames.size
        MyItem(n, templateItemsNames[templateN], (n * n - n) % 20) // Assign default quantities
    }

    /* Advanced */
    var flags20 = Tf.Resizable or Tf.Reorderable or Tf.Hideable or Tf.Sortable or Tf.SortMulti or Tf.RowBg or
            Tf.Borders or Tf.NoBordersInBody or Tf.ScrollX or Tf.ScrollY or Tf.SizingFixedFit

    enum class ContentsType2 { Text, Button, SmallButton, FillButton, Selectable, SelectableSpanRow }

    var contentsType2 = ContentsType2.SelectableSpanRow.ordinal
    val contentsTypeNames2 = arrayOf("Text", "Button", "SmallButton", "FillButton", "Selectable", "Selectable (span row)")
    var freezeCols1 = 1
    var freezeRows1 = 1
    var itemsCount = templateItemsNames.size * 2
    val outerSizeValue = Vec2(-Float.MIN_VALUE, TEXT_BASE_HEIGHT * 12)
    var rowMinHeight = 0f // Auto
    var innerWidthWithScroll = 0f // Auto-extend
    var outerSizeEnabled = true
    var showHeaders1 = true
    var showWrappedText = false

    //static ImGuiTextFilter filter;
    val items1 = ArrayList<MyItem>()
    val selection = ArrayList<Int>()
    var itemsNeedSort = false
    var showDebugDetails = false


    /* Columns */
    var selected = -1
    var disableIndent = false


    operator fun invoke() {

        //ImGui::SetNextItemOpen(true, ImGuiCond_Once);
        if (!collapsingHeader("Tables & Columns"))
            return

        // Using those as a base value to create width/height that are factor of the size of our font
        val TEXT_BASE_WIDTH = calcTextSize("A").x

        pushID("Tables")

        var openAction = -1
        if (button("Open all"))
            openAction = 1
        sameLine()
        if (button("Close all"))
            openAction = 0
        sameLine()

        // Options
        checkbox("Disable tree indentation", ::disableIndent)
        sameLine()
        helpMarker("Disable the indenting of tree nodes so demo tables can use the full window width.")
        separator()
        if (disableIndent)
            pushStyleVar(StyleVar.IndentSpacing, 0f)

        // About Styling of tables
        // Most settings are configured on a per-table basis via the flags passed to BeginTable() and TableSetupColumns APIs.
        // There are however a few settings that a shared and part of the ImGuiStyle structure:
        //   style.CellPadding                          // Padding within each cell
        //   style.Colors[ImGuiCol_TableHeaderBg]       // Table header background
        //   style.Colors[ImGuiCol_TableBorderStrong]   // Table outer and header borders
        //   style.Colors[ImGuiCol_TableBorderLight]    // Table inner borders
        //   style.Colors[ImGuiCol_TableRowBg]          // Table row background when ImGuiTableFlags_RowBg is enabled (even rows)
        //   style.Colors[ImGuiCol_TableRowBgAlt]       // Table row background when ImGuiTableFlags_RowBg is enabled (odds rows)

        // Demos
        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Basic") {
            // Here we will showcase three different ways to output a table.
            // They are very simple variations of a same thing!

            // [Method 1] Using TableNextRow() to create a new row, and TableSetColumnIndex() to select the column.
            // In many situations, this is the most flexible and easy to use pattern.
            helpMarker("Using TableNextRow() + calling TableSetColumnIndex() _before_ each cell, in a loop.")
            table("##table1", 3) {
                for (row in 0..3) {
                    tableNextRow()
                    for (column in 0..2) {
                        tableSetColumnIndex(column)
                        text("Row $row Column $column")
                    }
                }
            }

            // [Method 2] Using TableNextColumn() called multiple times, instead of using a for loop + TableSetColumnIndex().
            // This is generally more convenient when you have code manually submitting the contents of each columns.
            helpMarker("Using TableNextRow() + calling TableNextColumn() _before_ each cell, manually.")
            table("##table2", 3) {
                for (row in 0..3) {
                    tableNextRow()
                    tableNextColumn()
                    text("Row $row")
                    tableNextColumn()
                    text("Some contents")
                    tableNextColumn()
                    text("123.456")
                }
            }

            // [Method 3] We call TableNextColumn() _before_ each cell. We never call TableNextRow(),
            // as TableNextColumn() will automatically wrap around and create new roes as needed.
            // This is generally more convenient when your cells all contains the same type of data.
            helpMarker("""
                Only using TableNextColumn(), which tends to be convenient for tables where every cells contains the same type of contents.
                This is also more similar to the old NextColumn() function of the Columns API, and provided to facilitate the Columns->Tables API transition.""".trimIndent())
            table("##table3", 3) {
                for (item in 0..13) {
                    tableNextColumn()
                    text("Item $item")
                }
            }
        }

        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Borders, background") {
            // Expose a few Borders related flags interactively

            pushingStyleCompact {
                checkboxFlags("ImGuiTableFlags_RowBg", ::flags0, Tf.RowBg.i)
                checkboxFlags("ImGuiTableFlags_Borders", ::flags0, Tf.Borders.i)
                sameLine(); helpMarker("ImGuiTableFlags_Borders\n = ImGuiTableFlags_BordersInnerV\n | ImGuiTableFlags_BordersOuterV\n | ImGuiTableFlags_BordersInnerV\n | ImGuiTableFlags_BordersOuterH")
                indent()

                checkboxFlags("ImGuiTableFlags_BordersH", ::flags0, Tf.BordersH.i)
                indent()
                checkboxFlags("ImGuiTableFlags_BordersOuterH", ::flags0, Tf.BordersOuterH.i)
                checkboxFlags("ImGuiTableFlags_BordersInnerH", ::flags0, Tf.BordersInnerH.i)
                unindent()

                checkboxFlags("ImGuiTableFlags_BordersV", ::flags0, Tf.BordersV.i)
                indent()
                checkboxFlags("ImGuiTableFlags_BordersOuterV", ::flags0, Tf.BordersOuterV.i)
                checkboxFlags("ImGuiTableFlags_BordersInnerV", ::flags0, Tf.BordersInnerV.i)
                unindent()

                checkboxFlags("ImGuiTableFlags_BordersOuter", ::flags0, Tf.BordersOuter.i)
                checkboxFlags("ImGuiTableFlags_BordersInner", ::flags0, Tf.BordersInner.i)
                unindent()

                alignTextToFramePadding(); text("Cell contents:")
                sameLine(); radioButton("Text", ::contentsType0, ContentsType0.Text.ordinal)
                sameLine(); radioButton("FillButton", ::contentsType0, ContentsType0.FillButton.ordinal)
                checkbox("Display headers", ::displayHeaders)
                checkboxFlags("ImGuiTableFlags_NoBordersInBody", ::flags0, Tf.NoBordersInBody.i); sameLine(); helpMarker("Disable vertical borders in columns Body (borders will always appears in Headers")
            }

            table("##table1", 3, flags0) {
                // Display headers so we can inspect their interaction with borders.
                // (Headers are not the main purpose of this section of the demo, so we are not elaborating on them too much. See other sections for details)
                if (displayHeaders) {
                    tableSetupColumn("One")
                    tableSetupColumn("Two")
                    tableSetupColumn("Three")
                    tableHeadersRow()
                }

                for (row in 0..4) {
                    tableNextRow()
                    for (column in 0..2) {
                        tableSetColumnIndex(column)
                        val buf = "Hello $column,$row"
                        if (contentsType0 == ContentsType0.Text.ordinal)
                            textUnformatted(buf)
                        else if (contentsType0 != 0)
                            button(buf, Vec2(-Float.MIN_VALUE, 0f))
                    }
                }
            }
        }

        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Resizable, stretch") {
            // By default, if we don't enable ScrollX the sizing policy for each columns is "Stretch"
            // Each columns maintain a sizing weight, and they will occupy all available width.
            pushingStyleCompact {
                checkboxFlags("ImGuiTableFlags_Resizable", ::flags1, Tf.Resizable.i)
                checkboxFlags("ImGuiTableFlags_BordersV", ::flags1, Tf.BordersV.i)
                sameLine(); helpMarker("Using the _Resizable flag automatically enables the _BordersInnerV flag as well, this is why the resize borders are still showing when unchecking this.")
            }

            table("##table1", 3, flags1) {
                for (row in 0..4) {
                    tableNextRow()
                    for (column in 0..2) {
                        tableSetColumnIndex(column)
                        text("Hello $column,$row")
                    }
                }
            }
        }

        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Resizable, fixed") {
            // Here we use ImGuiTableFlags_SizingFixedFit (even though _ScrollX is not set)
            // So columns will adopt the "Fixed" policy and will maintain a fixed width regardless of the whole available width (unless table is small)
            // If there is not enough available width to fit all columns, they will however be resized down.
            // FIXME-TABLE: Providing a stretch-on-init would make sense especially for tables which don't have saved settings
            helpMarker(
                "Using _Resizable + _SizingFixedFit flags.\n" +
                        "Fixed-width columns generally makes more sense if you want to use horizontal scrolling.\n\n" +
                        "Double-click a column border to auto-fit the column to its contents.")
            pushingStyleCompact {
                checkbox("fill", ::fixedFill0)
            }

            val outerSize = Vec2(if (fixedFill0) -Float.MIN_VALUE else 0f, 0f)
            table("##table1", 3, flags2, outerSize) {
                for (row in 0..4) {
                    tableNextRow()
                    for (column in 0..2) {
                        tableSetColumnIndex(column)
                        text("Hello $column,$row")
                    }
                }
            }
        }

        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Resizable, mixed") {
            helpMarker("Using TableSetupColumn() to alter resizing policy on a per-column basis.\n\n" +
                               "When combining Fixed and Stretch columns, generally you only want one, maybe two trailing columns to use _WidthStretch.")

            table("##table1", 3, flags3) {
                tableSetupColumn("AAA", Tcf.WidthFixed.i)
                tableSetupColumn("BBB", Tcf.WidthFixed.i)
                tableSetupColumn("CCC", Tcf.WidthStretch.i)
                tableHeadersRow()
                for (row in 0..4) {
                    tableNextRow()
                    for (column in 0..2) {
                        tableSetColumnIndex(column)
                        text("${if (column == 2) "Stretch" else "Fixed"} $column,$row", column, row)
                    }
                }
            }
            table("##table2", 6, flags3) {
                tableSetupColumn("AAA", Tcf.WidthFixed.i)
                tableSetupColumn("BBB", Tcf.WidthFixed.i)
                tableSetupColumn("CCC", Tcf.WidthFixed or Tcf.DefaultHide)
                tableSetupColumn("DDD", Tcf.WidthStretch.i)
                tableSetupColumn("EEE", Tcf.WidthStretch.i)
                tableSetupColumn("FFF", Tcf.WidthStretch or Tcf.DefaultHide)
                tableHeadersRow()
                for (row in 0..4) {
                    tableNextRow()
                    for (column in 0..5) {
                        tableSetColumnIndex(column)
                        text("${if (column >= 3) "Stretch" else "Fixed"} $column,$row", column, row)
                    }
                }
            }
        }

        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Reorderable, hideable, with headers") {
            helpMarker(
                "Click and drag column headers to reorder columns.\n\n" +
                        "Right-click on a header to open a context menu.")
            pushingStyleCompact {
                checkboxFlags("ImGuiTableFlags_Resizable", ::flags4, Tf.Resizable.i)
                checkboxFlags("ImGuiTableFlags_Reorderable", ::flags4, Tf.Reorderable.i)
                checkboxFlags("ImGuiTableFlags_Hideable", ::flags4, Tf.Hideable.i)
                checkboxFlags("ImGuiTableFlags_NoBordersInBody", ::flags4, Tf.NoBordersInBody.i)
                checkboxFlags("ImGuiTableFlags_NoBordersInBodyUntilResize", ::flags4, Tf.NoBordersInBodyUntilResize.i); sameLine(); helpMarker("Disable vertical borders in columns Body until hovered for resize (borders will always appears in Headers)")
            }

            table("##table1", 3, flags4) {
                // Submit columns name with TableSetupColumn() and call TableHeadersRow() to create a row with a header in each column.
                // (Later we will show how TableSetupColumn() has other uses, optional flags, sizing weight etc.)
                tableSetupColumn("One")
                tableSetupColumn("Two")
                tableSetupColumn("Three")
                tableHeadersRow()
                for (row in 0..5) {
                    tableNextRow()
                    for (column in 0..2) {
                        tableSetColumnIndex(column)
                        text("Hello $column,$row")
                    }
                }
            }

            // Use outer_size.x == 0.0f instead of default to make the table as tight as possible (only valid when no scrolling and no stretch column)
            table("##table2", 3, flags4 or Tf.SizingFixedFit, Vec2()) {
                tableSetupColumn("One")
                tableSetupColumn("Two")
                tableSetupColumn("Three")
                tableHeadersRow()
                for (row in 0..5) {
                    tableNextRow()
                    for (column in 0..2) {
                        tableSetColumnIndex(column)
                        text("Fixed $column,$row")
                    }
                }
            }
        }

        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Padding") {
            // First example: showcase use of padding flags and effect of BorderOuterV/BorderInnerV on X padding.
            // We don't expose BorderOuterH/BorderInnerH here because they have no effect on X padding.
            helpMarker("We often want outer padding activated when any using features which makes the edges of a column visible:\n" +
                               "e.g.:\n" +
                               "- BorderOuterV\n" +
                               "- any form of row selection\n" +
                               "Because of this, activating BorderOuterV sets the default to PadOuterX. Using PadOuterX or NoPadOuterX you can override the default.\n\n" +
                               "Actual padding values are using style.CellPadding.\n\n" +
                               "In this demo we don't show horizontal borders to emphasis how they don't affect default horizontal padding.")

            pushingStyleCompact {
                checkboxFlags("ImGuiTableFlags_PadOuterX", ::flags5, Tf.PadOuterX.i)
                sameLine(); helpMarker("Enable outer-most padding (default if ImGuiTableFlags_BordersOuterV is set)")
                checkboxFlags("ImGuiTableFlags_NoPadOuterX", ::flags5, Tf.NoPadOuterX.i)
                sameLine(); helpMarker("Disable outer-most padding (default if ImGuiTableFlags_BordersOuterV is not set)")
                checkboxFlags("ImGuiTableFlags_NoPadInnerX", ::flags5, Tf.NoPadInnerX.i)
                sameLine(); helpMarker("Disable inner padding between columns (double inner padding if BordersOuterV is on, single inner padding if BordersOuterV is off)")
                checkboxFlags("ImGuiTableFlags_BordersOuterV", ::flags5, Tf.BordersOuterV.i)
                checkboxFlags("ImGuiTableFlags_BordersInnerV", ::flags5, Tf.BordersInnerV.i)
                checkbox("show_headers", ::showHeaders0)
            }

            table("##table1", 3, flags5) {
                if (showHeaders0) {
                    tableSetupColumn("One")
                    tableSetupColumn("Two")
                    tableSetupColumn("Three")
                    tableHeadersRow()
                }

                for (row in 0..4) {
                    tableNextRow()
                    for (column in 0..2) {
                        tableSetColumnIndex(column)
                        if (row == 0)
                            text("Avail %.2f", contentRegionAvail.x)
                        else
                            button("Hello $column,$row", Vec2(-Float.MIN_VALUE, 0f))
                        //if (ImGui::TableGetColumnFlags() & ImGuiTableColumnFlags_IsHovered)
                        //    ImGui::TableSetBgColor(ImGuiTableBgTarget_CellBg, IM_COL32(0, 100, 0, 255));
                    }
                }
            }

            // Second example: set style.CellPadding to (0.0) or a custom value.
            // FIXME-TABLE: Vertical border effectively not displayed the same way as horizontal one...
            helpMarker("Setting style.CellPadding to (0,0) or a custom value.")

            pushingStyleCompact {
                checkboxFlags("ImGuiTableFlags_Borders", ::flags6, Tf.Borders.i)
                checkboxFlags("ImGuiTableFlags_BordersH", ::flags6, Tf.BordersH.i)
                checkboxFlags("ImGuiTableFlags_BordersV", ::flags6, Tf.BordersV.i)
                checkboxFlags("ImGuiTableFlags_BordersInner", ::flags6, Tf.BordersInner.i)
                checkboxFlags("ImGuiTableFlags_BordersOuter", ::flags6, Tf.BordersOuter.i)
                checkboxFlags("ImGuiTableFlags_RowBg", ::flags6, Tf.RowBg.i)
                checkboxFlags("ImGuiTableFlags_Resizable", ::flags6, Tf.Resizable.i)
                checkbox("show_widget_frame_bg", ::showWidgetFrameBg)
                sliderVec2("CellPadding", cellPadding, 0f, 10f, "%.0f")
            }

            pushStyleVar(StyleVar.CellPadding, cellPadding)
            table("##table2", 3, flags6) {
                if (!showWidgetFrameBg)
                    pushStyleColor(Col.FrameBg, 0)
                for (cell in 0 until 3 * 5) {
                    tableNextColumn()
                    if (init)
                        textBufs[cell] = "edit me"
                    setNextItemWidth(-Float.MIN_VALUE)
                    pushID(cell)
                    inputText("##cell", textBufs[cell])
                    popID()
                }
                if (!showWidgetFrameBg)
                    popStyleColor()
                init = false
            }
            popStyleVar()
        }

        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Sizing policies") {
            pushingStyleCompact {
                checkbox("fill", ::fixedFill1)
                sameLine(); helpMarker(
                "Value passed to outer_size only affects tables with _SizingFixedFit or _SizingFixedSame sizing policies.\n\n" +
                        " - outer_size.x == 0: table fit to columns total contents.\n" +
                        " - outer_size.x == -FLT_MIN: table fill until right-most edge.")
                checkboxFlags("ImGuiTableFlags_Resizable", ::flags7, Tf.Resizable.i)
            }

            val outerSize = Vec2(if (fixedFill1) -Float.MIN_VALUE else 0f, 0f)
            for (tableN in 0..3)
                withID(tableN) {
                    setNextItemWidth(TEXT_BASE_WIDTH * 30)
                    editTableSizingFlags(sizingPolicyFlags, tableN)

                    // To make it easier to understand the different sizing policy,
                    // For each policy: we display one table where the columns have equal contents width, and one where the columns have different contents width.
                    table("##table1", 3, sizingPolicyFlags[tableN] or flags7, outerSize) {
                        for (row in 0..2) {
                            tableNextRow()
                            tableNextColumn(); text("Oh dear")
                            tableNextColumn(); text("Oh dear")
                            tableNextColumn(); text("Oh dear")
                        }
                    }
                    table("##table2", 3, sizingPolicyFlags[tableN] or flags7, outerSize) {
                        for (row in 0..2) {
                            tableNextRow()
                            tableNextColumn(); text("AAAA")
                            tableNextColumn(); text("BBBBBBBB")
                            tableNextColumn(); text("CCCCCCCCCCCC")
                        }
                    }
                }

            spacing()
            textUnformatted("Advanced")
            sameLine()
            helpMarker("This section allows you to interact and see the effect of various sizing policies depending on whether Scroll is enabled and the contents of your columns.")

            pushingStyleCompact {
                withID("Advanced") {
                    withItemWidth(TEXT_BASE_WIDTH * 30) {
                        val flags = intArrayOf(flags8)
                        editTableSizingFlags(flags, 0)
                        flags8 = flags[0]
                        _i = contentsType1.ordinal
                        combo("Contents", ::_i, "Show width\u0000Short Text\u0000Long Text\u0000Button\u0000Fill Button\u0000InputText\u0000")
                        contentsType1 = ContentsType1.values()[_i]
                        if (contentsType1 == ContentsType1.FillButton) {
                            sameLine()
                            helpMarker("Be mindful that using right-alignment (e.g. size.x = -FLT_MIN) creates a feedback loop where contents width can feed into auto-column width can feed into contents width.")
                        }
                        dragInt("Columns", ::columnCount0, 0.1f, 1, 64, "%d", SliderFlag.AlwaysClamp.i)
                        checkboxFlags("ImGuiTableFlags_Resizable", ::flags8, Tf.Resizable.i)
                        checkboxFlags("ImGuiTableFlags_PreciseWidths", ::flags8, Tf.PreciseWidths.i)
                        sameLine(); helpMarker("Disable distributing remainder width to stretched columns (width allocation on a 100-wide table with 3 columns: Without this flag: 33,33,34. With this flag: 33,33,33). With larger number of columns, resizing will appear to be less smooth.")
                        checkboxFlags("ImGuiTableFlags_Resizable", ::flags8, Tf.Resizable.i)
                        checkboxFlags("ImGuiTableFlags_ScrollX", ::flags8, Tf.ScrollX.i)
                        checkboxFlags("ImGuiTableFlags_ScrollY", ::flags8, Tf.ScrollY.i)
                        checkboxFlags("ImGuiTableFlags_NoClip", ::flags8, Tf.NoClip.i)
                    }
                }
            }

            outerSize.put(-Float.MIN_VALUE, TEXT_BASE_HEIGHT * 7)
            table("##table2", columnCount0, flags8, outerSize) {
                for (cell in 0 until 10 * columnCount0) {
                    tableNextColumn()
                    val column = tableGetColumnIndex()
                    val row = tableGetRowIndex()

                    withID(cell) {
                        val label = "Hello $column,$row"
                        when (contentsType1) {
                            ContentsType1.ShortText -> textUnformatted(label)
                            ContentsType1.LongText -> text("Some ${if (column == 0) "long" else "longeeer"} text $column,$row\nOver two lines..")
                            ContentsType1.ShowWidth -> text("W: %.1f", contentRegionAvail.x)
                            ContentsType1.Button -> button(label)
                            ContentsType1.FillButton -> button(label, Vec2(-Float.MIN_VALUE, 0f))
                            ContentsType1.InputText -> {
                                setNextItemWidth(-Float.MIN_VALUE)
                                inputText("##", label.toByteArray(textBuf))
                            }
                        }
                    }
                }
            }
        }

        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Vertical scrolling, with clipping") {
            helpMarker("Here we activate ScrollY, which will create a child window container to allow hosting scrollable contents.\n\nWe also demonstrate using ImGuiListClipper to virtualize the submission of many items.")

            pushingStyleCompact {
                checkboxFlags("ImGuiTableFlags_ScrollY", ::flags9, Tf.ScrollY.i)
            }

            // When using ScrollX or ScrollY we need to specify a size for our table container!
            // Otherwise by default the table will fit all available space, like a BeginChild() call.
            val size = Vec2(-Float.MIN_VALUE, TEXT_BASE_HEIGHT * 8)
            table("##table1", 3, flags9, size) {
                tableSetupScrollFreeze(0, 1) // Make top row always visible
                tableSetupColumn("One", Tcf.None.i)
                tableSetupColumn("Two", Tcf.None.i)
                tableSetupColumn("Three", Tcf.None.i)
                tableHeadersRow()

                // Demonstrate using clipper for large vertical lists
                val clipper = ListClipper()
                clipper.begin(1000)
                while (clipper.step())
                    for (row in clipper.display) {
                        tableNextRow()
                        for (column in 0..2) {
                            tableSetColumnIndex(column)
                            text("Hello $column,$row")
                        }
                    }
            }
        }

        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Horizontal scrolling") {
            helpMarker(
                "When ScrollX is enabled, the default sizing policy becomes ImGuiTableFlags_SizingFixedFit, " +
                        "as automatically stretching columns doesn't make much sense with horizontal scrolling.\n\n" +
                        "Also note that as of the current version, you will almost always want to enable ScrollY along with ScrollX," +
                        "because the container window won't automatically extend vertically to fix contents (this may be improved in future versions).")

            pushingStyleCompact {
                checkboxFlags("ImGuiTableFlags_Resizable", ::flags10, Tf.Resizable.i)
                checkboxFlags("ImGuiTableFlags_ScrollX", ::flags10, Tf.ScrollX.i)
                checkboxFlags("ImGuiTableFlags_ScrollY", ::flags10, Tf.ScrollY.i)
                setNextItemWidth(ImGui.frameHeight)
                dragInt("freeze_cols", ::freezeCols0, 0.2f, 0, 9, null, SliderFlag.NoInput.i)
                setNextItemWidth(ImGui.frameHeight)
                dragInt("freeze_rows", ::freezeRows0, 0.2f, 0, 9, null, SliderFlag.NoInput.i)
            }

            // When using ScrollX or ScrollY we need to specify a size for our table container!
            // Otherwise by default the table will fit all available space, like a BeginChild() call.
            val outerSize = Vec2(-Float.MIN_VALUE, TEXT_BASE_HEIGHT * 8)
            table("##table1", 7, flags10, outerSize) {
                tableSetupScrollFreeze(freezeCols0, freezeRows0)
                tableSetupColumn("Line #", Tcf.NoHide.i) // Make the first column not hideable to match our use of TableSetupScrollFreeze()
                tableSetupColumn("One")
                tableSetupColumn("Two")
                tableSetupColumn("Three")
                tableSetupColumn("Four")
                tableSetupColumn("Five")
                tableSetupColumn("Six")
                tableHeadersRow()
                for (row in 0..19) {
                    tableNextRow()
                    for (column in 0..6) {
                        // Both TableNextColumn() and TableSetColumnIndex() return true when a column is visible or performing width measurement.
                        // Because here we know that:
                        // - A) all our columns are contributing the same to row height
                        // - B) column 0 is always visible,
                        // We only always submit this one column and can skip others.
                        // More advanced per-column clipping behaviors may benefit from polling the status flags via TableGetColumnFlags().
                        if (!tableSetColumnIndex(column) && column > 0)
                            continue
                        if (column == 0)
                            text("Line $row")
                        else
                            text("Hello world $column,$row")
                    }
                }
            }

            spacing()
            textUnformatted("Stretch + ScrollX")
            sameLine()
            helpMarker(
                "Showcase using Stretch columns + ScrollX together: " +
                        "this is rather unusual and only makes sense when specifying an 'inner_width' for the table!\n" +
                        "Without an explicit value, inner_width is == outer_size.x and therefore using Stretch columns + ScrollX together doesn't make sense.")
            pushingStyleCompact {
                withID("flags3") {
                    withItemWidth(TEXT_BASE_WIDTH * 30) {
                        checkboxFlags("ImGuiTableFlags_ScrollX", ::flags11, Tf.ScrollX.i)
                        dragFloat("inner_width", ::innerWidth0, 1f, 0f, Float.MAX_VALUE, "%.1f")
                    }
                }
            }
            table("##table2", 7, flags11, outerSize, innerWidth0) {
                for (cell in 0 until 20 * 7) {
                    tableNextColumn()
                    text("Hello world ${tableGetColumnIndex()},${tableGetRowIndex()}")
                }
            }
        }

        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Columns flags") {
            // Create a first table just to show all the options/flags we want to make visible in our example!
            // -> begin of the class, as static

            table("##flags", columnCount1, Tf.None.i) {
                pushingStyleCompact {
                    for (column in 0 until columnCount1) {
                        tableNextColumn()
                        pushID(column)
                        alignTextToFramePadding() // FIXME-TABLE: Workaround for wrong text baseline propagation
                        text("'${columnNames[column]}'")
                        spacing()
                        text("Input flags:")
                        columnFlags[column] = editTableColumnsFlags(columnFlags[column])
                        spacing()
                        text("Output flags:")
                        showTableColumnsStatusFlags(columnFlagsOut[column])
                        popID()
                    }
                }
            }

            // Create the real table we care about for the example!
            // We use a scrolling table to be able to showcase the difference between the _IsEnabled and _IsVisible flags above, otherwise in
            // a non-scrolling table columns are always visible (unless using ImGuiTableFlags_NoKeepColumnsVisible + resizing the parent window down)
            val size = Vec2(-Float.MIN_VALUE, TEXT_BASE_HEIGHT * 9)
            table("##table", columnCount1, flags12, size) {
                for (column in 0 until columnCount1)
                    tableSetupColumn(columnNames[column], columnFlags[column])
                tableHeadersRow()
                for (column in 0 until columnCount1)
                    columnFlagsOut[column] = tableGetColumnFlags(column)
                val indentStep = (TEXT_BASE_WIDTH.i / 2).f
                for (row in 0..7) {
                    indent(indentStep) // Add some indentation to demonstrate usage of per-column IndentEnable/IndentDisable flags.
                    tableNextRow()
                    for (column in 0 until columnCount1) {
                        tableSetColumnIndex(column)
                        text("${if (column == 0) "Indented" else "Hello"} ${tableGetColumnName(column)}")
                    }
                }
                unindent(indentStep * 8f)
            }
        }

        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Columns widths") {
            helpMarker("Using TableSetupColumn() to setup default width.")

            pushingStyleCompact {
                checkboxFlags("ImGuiTableFlags_Resizable", ::flags13, Tf.Resizable.i)
                checkboxFlags("ImGuiTableFlags_NoBordersInBodyUntilResize", ::flags13, Tf.NoBordersInBodyUntilResize.i)
            }
            table("##table1", 3, flags13) {
                // We could also set ImGuiTableFlags_SizingFixedFit on the table and all columns will default to ImGuiTableColumnFlags_WidthFixed.
                tableSetupColumn("one", Tcf.WidthFixed.i, 100f) // Default to 100.0f
                tableSetupColumn("two", Tcf.WidthFixed.i, 200f) // Default to 200.0f
                tableSetupColumn("three", Tcf.WidthFixed.i)       // Default to auto
                tableHeadersRow()
                for (row in 0..3) {
                    tableNextRow()
                    for (column in 0..2) {
                        tableSetColumnIndex(column)
                        if (row == 0)
                            text("(w: %5.1f)", ImGui.contentRegionAvail.x)
                        else
                            text("Hello $column,$row")
                    }
                }
            }

            helpMarker("Using TableSetupColumn() to setup explicit width.\n\nUnless _NoKeepColumnsVisible is set, fixed columns with set width may still be shrunk down if there's not enough space in the host.")

            pushingStyleCompact {
                checkboxFlags("ImGuiTableFlags_NoKeepColumnsVisible", ::flags14, Tf.NoKeepColumnsVisible.i)
                checkboxFlags("ImGuiTableFlags_BordersInnerV", ::flags14, Tf.BordersInnerV.i)
                checkboxFlags("ImGuiTableFlags_BordersOuterV", ::flags14, Tf.BordersOuterV.i)
            }

            table("##table2", 4, flags14) {
                // We could also set ImGuiTableFlags_SizingFixedFit on the table and all columns will default to ImGuiTableColumnFlags_WidthFixed.
                tableSetupColumn("", Tcf.WidthFixed.i, 100f)
                tableSetupColumn("", Tcf.WidthFixed.i, TEXT_BASE_WIDTH * 15f)
                tableSetupColumn("", Tcf.WidthFixed.i, TEXT_BASE_WIDTH * 30f)
                tableSetupColumn("", Tcf.WidthFixed.i, TEXT_BASE_WIDTH * 15f)
                for (row in 0..4) {
                    tableNextRow()
                    for (column in 0..3) {
                        tableSetColumnIndex(column)
                        if (row == 0)
                            text("(w: %5.1f)", contentRegionAvail.x)
                        else
                            text("Hello $column,$row")
                    }
                }
            }
        }

        treeNode("Nested tables") {
            helpMarker("This demonstrate embedding a table into another table cell.")

            table("nested1", 2, Tf.Borders or Tf.Resizable or Tf.Reorderable or Tf.Hideable) {
                tableSetupColumn("A0")
                tableSetupColumn("A1")
                tableHeadersRow()

                tableNextColumn()
                text("A0 Cell 0")
                run {
                    val rowsHeight = TEXT_BASE_HEIGHT * 2
                    table("nested2", 2, Tf.Borders or Tf.Resizable or Tf.Reorderable or Tf.Hideable) {
                        tableSetupColumn("B0")
                        tableSetupColumn("B1")
                        tableHeadersRow()

                        tableNextRow(Trf.None.i, rowsHeight)
                        tableNextColumn()
                        text("B0 Cell 0")
                        tableNextColumn()
                        text("B0 Cell 1")
                        tableNextRow(Trf.None.i, rowsHeight)
                        tableNextColumn()
                        text("B1 Cell 0")
                        tableNextColumn()
                        text("B1 Cell 1")
                    }
                }
                tableNextColumn(); text("A0 Cell 1")
                tableNextColumn(); text("A1 Cell 0")
                tableNextColumn(); text("A1 Cell 1")
            }
        }

        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Row height") {
            helpMarker("You can pass a 'min_row_height' to TableNextRow().\n\nRows are padded with 'style.CellPadding.y' on top and bottom, so effectively the minimum row height will always be >= 'style.CellPadding.y * 2.0f'.\n\nWe cannot honor a _maximum_ row height as that would requires a unique clipping rectangle per row.")
            table("##Table", 1, Tf.BordersOuter or Tf.BordersInnerV) {
                for (row in 0..9) {
                    val minRowHeight = (TEXT_BASE_HEIGHT * 0.3f * row).i.f
                    tableNextRow(Trf.None.i, minRowHeight)
                    tableNextColumn()
                    text("min_row_height = %.2f", minRowHeight)
                }
            }
        }

        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Outer size") {
            // Showcasing use of outer_size.x == 0.0f and ImGuiTableFlags_NoHostExtendY
            // The default value of outer_size.x is -FLT_MIN which right-align tables.
            // Using outer_size.x == 0.0f on a table with no scrolling and no stretch column we can make them tighter.
            text("Using auto/all width, using NoHostExtendY:")
            pushingStyleCompact {
                checkbox("fill", ::fixedFill2)
                checkboxFlags("ImGuiTableFlags_NoHostExtendY", ::flags15, Tf.NoHostExtendY.i)
            }

            val outerSize = Vec2(if (fixedFill2) -Float.MIN_VALUE else 0f, TEXT_BASE_HEIGHT * 5.5f)
            table("##table3", 3, flags15, outerSize) {
                for (row in 0..9) { // TODO dsl this pattern?
                    tableNextRow()
                    for (column in 0..2) {
                        tableNextColumn()
                        text("Cell $column,$row")
                    }
                }
            }
            sameLine()
            text("Hello!")

            spacing()

            text("Using explicit size:")
            table("##table1", 3, Tf.Borders or Tf.RowBg, Vec2(TEXT_BASE_WIDTH * 30, 0f)) {
                for (row in 0..4) {
                    tableNextRow()
                    for (column in 0..2) {
                        tableNextColumn()
                        text("Cell $column,$row")
                    }
                }
            }
            sameLine()
            table("##table2", 3, Tf.Borders or Tf.RowBg, Vec2(TEXT_BASE_WIDTH * 30, 0f)) {
                for (row in 0..2) {
                    tableNextRow(0, TEXT_BASE_HEIGHT * 1.5f)
                    for (column in 0..2) {
                        tableNextColumn()
                        text("Cell $column,$row")
                    }
                }
            }
        }

        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Background color") {

            pushingStyleCompact {
                checkboxFlags("ImGuiTableFlags_Borders", ::flags16, Tf.Borders.i)
                checkboxFlags("ImGuiTableFlags_RowBg", ::flags16, Tf.RowBg.i)
                sameLine(); helpMarker("ImGuiTableFlags_RowBg automatically sets RowBg0 to alternative colors pulled from the Style.")
                combo("row bg type", ::rowBgType, "None\u0000Red\u0000Gradient\u0000")
                combo("row bg target", ::rowBgTarget, "RowBg0\u0000RowBg1\u0000"); sameLine(); helpMarker("Target RowBg0 to override the alternating odd/even colors,\nTarget RowBg1 to blend with them.")
                combo("cell bg type", ::cellBgType, "None\u0000Blue\u0000"); sameLine(); helpMarker("We are colorizing cells to B1->C2 here.")
                assert(rowBgType in 0..2)
                assert(rowBgTarget in 0..1)
                assert(cellBgType in 0..1)
            }

            table("##Table", 5, flags16) {
                for (row in 0..5) {

                    tableNextRow()

                    // Demonstrate setting a row background color with 'ImGui::TableSetBgColor(ImGuiTableBgTarget_RowBgX, ...)'
                    // We use a transparent color so we can see the one behind in case our target is RowBg1 and RowBg0 was already targeted by the ImGuiTableFlags_RowBg flag.
                    if (rowBgType != 0) {
                        val rowBgColor = if (rowBgType == 1) Vec4(0.7f, 0.3f, 0.3f, 0.65f) else Vec4(0.2f + row * 0.1f, 0.2f, 0.2f, 0.65f) // Flat or Gradient?
                        tableSetBgColor(TableBgTarget of (TableBgTarget.RowBg0.i + rowBgTarget), rowBgColor.u32)
                    }

                    // Fill cells
                    for (column in 0..4) {
                        tableSetColumnIndex(column)
                        text("${'A' + row}${'0' + column}")

                        // Change background of Cells B1->C2
                        // Demonstrate setting a cell background color with 'ImGui::TableSetBgColor(ImGuiTableBgTarget_CellBg, ...)'
                        // (the CellBg color will be blended over the RowBg and ColumnBg colors)
                        // We can also pass a column number as a third parameter to TableSetBgColor() and do this outside the column loop.
                        if (row in 1..2 && column >= 1 && column <= 2 && cellBgType == 1) {
                            val cellBgColor = Vec4(0.3f, 0.3f, 0.7f, 0.65f).u32
                            tableSetBgColor(TableBgTarget.CellBg, cellBgColor)
                        }
                    }
                }
            }
        }

        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Tree view") {
            table("##3ways", 3, flags17) {
                // The first column will use the default _WidthStretch when ScrollX is Off and _WidthFixed when ScrollX is On
                tableSetupColumn("Name", Tcf.NoHide.i)
                tableSetupColumn("Size", Tcf.WidthFixed.i, TEXT_BASE_WIDTH * 12f)
                tableSetupColumn("Type", Tcf.WidthFixed.i, TEXT_BASE_WIDTH * 18f)
                tableHeadersRow()

                MyTreeNode.displayNode(nodes, 0)
            }
        }

        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Item width") {
            helpMarker(
                "Showcase using PushItemWidth() and how it is preserved on a per-column basis.\n\n" +
                        "Note that on auto-resizing non-resizable fixed columns, querying the content width for e.g. right-alignment doesn't make sense.")
            table("##table2", 3, Tf.Borders.i) {
                tableSetupColumn("small")
                tableSetupColumn("half")
                tableSetupColumn("right-align")
                tableHeadersRow()

                for (row in 0..2) {
                    tableNextRow()
                    if (row == 0) {
                        // Setup ItemWidth once (instead of setting up every time, which is also possible but less efficient)
                        tableSetColumnIndex(0)
                        pushItemWidth(TEXT_BASE_WIDTH * 3f) // Small
                        tableSetColumnIndex(1)
                        pushItemWidth(-contentRegionAvail.x * 0.5f)
                        tableSetColumnIndex(2)
                        pushItemWidth(-Float.MIN_VALUE) // Right-aligned
                    }

                    // Draw our contents
                    withID(row) {
                        tableSetColumnIndex(0)
                        sliderFloat("float0", ::dummyF, 0f, 1f)
                        tableSetColumnIndex(1)
                        sliderFloat("float1", ::dummyF, 0f, 1f)
                        tableSetColumnIndex(2)
                        sliderFloat("float2", ::dummyF, 0f, 1f)
                    }
                }
            }
        }

        // Demonstrate using TableHeader() calls instead of TableHeadersRow()
        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Custom headers") {
            val COLUMNS_COUNT = 3
            table("##table1", COLUMNS_COUNT, Tf.Borders or Tf.Reorderable or Tf.Hideable) {
                tableSetupColumn("Apricot")
                tableSetupColumn("Banana")
                tableSetupColumn("Cherry")

                // Dummy entire-column selection storage
                // FIXME: It would be nice to actually demonstrate full-featured selection using those checkbox.

                // Instead of calling TableHeadersRow() we'll submit custom headers ourselves
                tableNextRow(Trf.Headers.i)
                for (column in 0 until COLUMNS_COUNT) {
                    tableSetColumnIndex(column)
                    val columnName = tableGetColumnName(column) // Retrieve name passed to TableSetupColumn()
                    pushID(column)
                    pushStyleVar(StyleVar.FramePadding, Vec2(0))
                    checkbox("##checkall", columnSelected, column)
                    popStyleVar()
                    sameLine(0f, style.itemInnerSpacing.x)
                    tableHeader(columnName!!)
                    popID()
                }

                for (row in 0..4) {
                    tableNextRow()
                    for (column in 0..2) {
                        tableSetColumnIndex(column)
                        selectable("Cell $column,$row", columnSelected, column)
                    }
                }
            }
        }

        // Demonstrate creating custom context menus inside columns, while playing it nice with context menus provided by TableHeadersRow()/TableHeader()
        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Context menus") {
            helpMarker("By default, right-clicking over a TableHeadersRow()/TableHeader() line will open the default context-menu.\nUsing ImGuiTableFlags_ContextMenuInBody we also allow right-clicking over columns body.")

            pushingStyleCompact {
                checkboxFlags("ImGuiTableFlags_ContextMenuInBody", ::flags18, Tf.ContextMenuInBody.i)
            }

            // Context Menus: first example
            // [1.1] Right-click on the TableHeadersRow() line to open the default table context menu.
            // [1.2] Right-click in columns also open the default table context menu (if ImGuiTableFlags_ContextMenuInBody is set)
            val COLUMNS_COUNT = 3
            table("##table1", COLUMNS_COUNT, flags18) {
                tableSetupColumn("One")
                tableSetupColumn("Two")
                tableSetupColumn("Three")

                // [1.1]] Right-click on the TableHeadersRow() line to open the default table context menu.
                tableHeadersRow()

                // Submit dummy contents
                for (row in 0..3) {
                    tableNextRow()
                    for (column in 0 until COLUMNS_COUNT) {
                        tableSetColumnIndex(column)
                        text("Cell $column,$row")
                    }
                }
            }

            // Context Menus: second example
            // [2.1] Right-click on the TableHeadersRow() line to open the default table context menu.
            // [2.2] Right-click on the ".." to open a custom popup
            // [2.3] Right-click in columns to open another custom popup
            helpMarker("Demonstrate mixing table context menu (over header), item context button (over button) and custom per-colum context menu (over column body).")
            val flags15b = Tf.Resizable or Tf.SizingFixedFit or Tf.Reorderable or Tf.Hideable or Tf.Borders
            // [JVM] cant use DSL because of endTable isnt the last call
            if (beginTable("##table2", COLUMNS_COUNT, flags15b)) {
                tableSetupColumn("One")
                tableSetupColumn("Two")
                tableSetupColumn("Three")

                // [2.1] Right-click on the TableHeadersRow() line to open the default table context menu.
                tableHeadersRow()
                for (row in 0..3) {
                    tableNextRow()
                    for (column in 0 until COLUMNS_COUNT) {
                        // Submit dummy contents
                        tableSetColumnIndex(column)
                        text("Cell $column,$row")
                        sameLine()

                        // [2.2] Right-click on the ".." to open a custom popup
                        pushID(row * COLUMNS_COUNT + column)
                        smallButton("..")
                        popupContextItem {
                            text("This is the popup for Button(\"..\") in Cell $column,$row")
                            if (button("Close"))
                                closeCurrentPopup()
                        }
                        popID()
                    }
                }

                // [2.3] Right-click anywhere in columns to open another custom popup
                // (instead of testing for !IsAnyItemHovered() we could also call OpenPopup() with ImGuiPopupFlags_NoOpenOverExistingPopup
                // to manage popup priority as the popups triggers, here "are we hovering a column" are overlapping)
                var hoveredColumn = -1
                for (column in 0..COLUMNS_COUNT) {
                    pushID(column)
                    if (tableGetColumnFlags(column) has Tcf.IsHovered)
                        hoveredColumn = column
                    if (hoveredColumn == column && !ImGui.isAnyItemHovered && ImGui.isMouseReleased(MouseButton.Right))
                        openPopup("MyPopup")
                    popup("MyPopup") {
                        if (column == COLUMNS_COUNT)
                            text("This is a custom popup for unused space after the last column.")
                        else
                            text("This is a custom popup for Column $column")
                        if (button("Close"))
                            closeCurrentPopup()
                    }
                    popID()
                }

                endTable()
                text("Hovered column: $hoveredColumn")
            }
        }

        // Demonstrate creating multiple tables with the same ID
        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Synced instances") {
            helpMarker("Multiple tables with the same identifier will share their settings, width, visibility, order etc.")
            for (n in 0..2) {
                val open = collapsingHeader("Synced Table $n", Tnf.DefaultOpen.i)
                val flags = Tf.Resizable or Tf.Reorderable or Tf.Hideable or Tf.Borders or Tf.SizingFixedFit or Tf.NoSavedSettings
                if (open && beginTable("Table", 3, flags)) {
                    tableSetupColumn("One")
                    tableSetupColumn("Two")
                    tableSetupColumn("Three")
                    tableHeadersRow()
                    for (cell in 0..8) {
                        tableNextColumn()
                        text("this cell $cell")
                    }
                    endTable()
                }
            }
        }

        // Demonstrate using Sorting facilities
        // This is a simplified version of the "Advanced" example, where we mostly focus on the code necessary to handle sorting.
        // Note that the "Advanced" example also showcase manually triggering a sort (e.g. if item quantities have been modified)
        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Sorting") {

            // Options
            pushingStyleCompact {
                checkboxFlags("ImGuiTableFlags_SortMulti", ::flags19, Tf.SortMulti.i)
                sameLine(); helpMarker("When sorting is enabled: hold shift when clicking headers to sort on multiple column. TableGetSortSpecs() may return specs where (SpecsCount > 1).")
                checkboxFlags("ImGuiTableFlags_SortTristate", ::flags19, Tf.SortTristate.i)
                sameLine(); helpMarker("When sorting is enabled: allow no sorting, disable default sorting. TableGetSortSpecs() may return specs where (SpecsCount == 0).")
            }

            table("##table", 4, flags19, Vec2(-Float.MIN_VALUE, TEXT_BASE_HEIGHT * 15), 0f) {
                // Declare columns
                // We use the "user_id" parameter of TableSetupColumn() to specify a user id that will be stored in the sort specifications.
                // This is so our sort function can identify a column given our own identifier. We could also identify them based on their index!
                // Demonstrate using a mixture of flags among available sort-related flags:
                // - ImGuiTableColumnFlags_DefaultSort
                // - ImGuiTableColumnFlags_NoSort / ImGuiTableColumnFlags_NoSortAscending / ImGuiTableColumnFlags_NoSortDescending
                // - ImGuiTableColumnFlags_PreferSortAscending / ImGuiTableColumnFlags_PreferSortDescending
                tableSetupColumn("ID", Tcf.DefaultSort or Tcf.WidthFixed, 0f, MyItemColumnID.ID.ordinal)
                tableSetupColumn("Name", Tcf.WidthFixed.i, 0f, MyItemColumnID.Name.ordinal)
                tableSetupColumn("Action", Tcf.NoSort or Tcf.WidthFixed, 0f, MyItemColumnID.Action.ordinal)
                tableSetupColumn("Quantity", Tcf.PreferSortDescending or Tcf.WidthStretch, 0f, MyItemColumnID.Quantity.ordinal)
                tableSetupScrollFreeze(0, 1) // Make row always visible
                tableHeadersRow()

                // Sort our data if sort specs have been changed!
                tableGetSortSpecs()?.let { sortsSpecs ->
                    if (sortsSpecs.specsDirty) {
                        MyItem.currentSortSpecs = sortsSpecs // Store in variable accessible by the sort function.
                        if (items0.size > 1)
                            items0.sortWith(MyItem.compareWithSortSpecs)
                        MyItem.currentSortSpecs = null
                        sortsSpecs.specsDirty = false
                    }
                }

                // Demonstrate using clipper for large vertical lists
                val clipper = ListClipper()
                clipper.begin(items0.size)
                while (clipper.step())
                    for (rowN in clipper.display) {
                        // Display a data item
                        val item = items0[rowN]
                        pushID(item.id)
                        tableNextRow()
                        tableNextColumn()
                        text("%04d", item.id)
                        tableNextColumn()
                        textUnformatted(item.name)
                        tableNextColumn()
                        smallButton("None")
                        tableNextColumn()
                        text("${item.quantity}")
                        popID()
                    }
            }
        }

        //ImGui::SetNextItemOpen(true, ImGuiCond_Once); // [DEBUG]
        if (openAction != -1)
            setNextItemOpen(openAction != 0)
        treeNode("Advanced") {
            //ImGui::SetNextItemOpen(true, ImGuiCond_Once); // FIXME-TABLE: Enabling this results in initial clipped first pass on table which tend to affects column sizing
            treeNode("Options") {
                // Make the UI compact because there are so many fields
                pushingStyleCompact {
                    pushItemWidth(TEXT_BASE_WIDTH * 28f)

                    treeNodeEx("Features:", Tnf.DefaultOpen.i) {
                        checkboxFlags("ImGuiTableFlags_Resizable", ::flags20, Tf.Resizable.i)
                        checkboxFlags("ImGuiTableFlags_Reorderable", ::flags20, Tf.Reorderable.i)
                        checkboxFlags("ImGuiTableFlags_Hideable", ::flags20, Tf.Hideable.i)
                        checkboxFlags("ImGuiTableFlags_Sortable", ::flags20, Tf.Sortable.i)
                        checkboxFlags("ImGuiTableFlags_NoSavedSettings", ::flags20, Tf.NoSavedSettings.i)
                        checkboxFlags("ImGuiTableFlags_ContextMenuInBody", ::flags20, Tf.ContextMenuInBody.i)
                    }

                    treeNodeEx("Decorations:", Tnf.DefaultOpen.i) {
                        checkboxFlags("ImGuiTableFlags_RowBg", ::flags20, Tf.RowBg.i)
                        checkboxFlags("ImGuiTableFlags_BordersV", ::flags20, Tf.BordersV.i)
                        checkboxFlags("ImGuiTableFlags_BordersOuterV", ::flags20, Tf.BordersOuterV.i)
                        checkboxFlags("ImGuiTableFlags_BordersInnerV", ::flags20, Tf.BordersInnerV.i)
                        checkboxFlags("ImGuiTableFlags_BordersH", ::flags20, Tf.BordersH.i)
                        checkboxFlags("ImGuiTableFlags_BordersOuterH", ::flags20, Tf.BordersOuterH.i)
                        checkboxFlags("ImGuiTableFlags_BordersInnerH", ::flags20, Tf.BordersInnerH.i)
                        checkboxFlags("ImGuiTableFlags_NoBordersInBody", ::flags20, Tf.NoBordersInBody.i); sameLine(); helpMarker("Disable vertical borders in columns Body (borders will always appears in Headers")
                        checkboxFlags("ImGuiTableFlags_NoBordersInBodyUntilResize", ::flags20, Tf.NoBordersInBodyUntilResize.i); sameLine(); helpMarker("Disable vertical borders in columns Body until hovered for resize (borders will always appears in Headers)")
                    }

                    treeNodeEx("Sizing:", Tnf.DefaultOpen.i) {
                        val flags = intArrayOf(flags20)
                        editTableSizingFlags(flags, 0)
                        flags20 = flags[0]
                        checkboxFlags("ImGuiTableFlags_NoHostExtendY", ::flags20, Tf.NoHostExtendY.i)
                        checkboxFlags("ImGuiTableFlags_NoKeepColumnsVisible", ::flags20, Tf.NoKeepColumnsVisible.i)
                        sameLine(); helpMarker("Only available if ScrollX is disabled.")
                        checkboxFlags("ImGuiTableFlags_PreciseWidths", ::flags20, Tf.PreciseWidths.i)
                        sameLine(); helpMarker("Disable distributing remainder width to stretched columns (width allocation on a 100-wide table with 3 columns: Without this flag: 33,33,34. With this flag: 33,33,33). With larger number of columns, resizing will appear to be less smooth.")
                        checkboxFlags("ImGuiTableFlags_NoClip", ::flags20, Tf.NoClip.i)
                        sameLine(); helpMarker("Disable clipping rectangle for every individual columns (reduce draw command count, items will be able to overflow into other columns). Generally incompatible with ScrollFreeze options.")
                    }

                    treeNodeEx("Padding:", Tnf.DefaultOpen.i) {
                        checkboxFlags("ImGuiTableFlags_PadOuterX", ::flags20, Tf.PadOuterX.i)
                        checkboxFlags("ImGuiTableFlags_NoPadOuterX", ::flags20, Tf.NoPadOuterX.i)
                        checkboxFlags("ImGuiTableFlags_NoPadInnerX", ::flags20, Tf.NoPadInnerX.i)
                    }

                    treeNodeEx("Scrolling:", Tnf.DefaultOpen.i) {
                        checkboxFlags("ImGuiTableFlags_ScrollX", ::flags20, Tf.ScrollX.i)
                        sameLine()
                        setNextItemWidth(ImGui.frameHeight)
                        dragInt("freeze_cols", ::freezeCols1, 0.2f, 0, 9, null, SliderFlag.NoInput.i)
                        checkboxFlags("ImGuiTableFlags_ScrollY", ::flags20, Tf.ScrollY.i)
                        sameLine()
                        setNextItemWidth(ImGui.frameHeight)
                        dragInt("freeze_rows", ::freezeRows1, 0.2f, 0, 9, null, SliderFlag.NoInput.i)
                    }

                    treeNodeEx("Sorting:", Tnf.DefaultOpen.i) {
                        checkboxFlags("ImGuiTableFlags_SortMulti", ::flags20, Tf.SortMulti.i)
                        sameLine(); helpMarker("When sorting is enabled: hold shift when clicking headers to sort on multiple column. TableGetSortSpecs() may return specs where (SpecsCount > 1).")
                        checkboxFlags("ImGuiTableFlags_SortTristate", ::flags20, Tf.SortTristate.i)
                        sameLine(); helpMarker("When sorting is enabled: allow no sorting, disable default sorting. TableGetSortSpecs() may return specs where (SpecsCount == 0).")
                    }

                    treeNodeEx("Other:", Tnf.DefaultOpen.i) {
                        checkbox("show_headers", ::showHeaders1)
                        checkbox("show_wrapped_text", ::showWrappedText)

                        dragVec2("##OuterSize", outerSizeValue)
                        sameLine(0f, style.itemInnerSpacing.x)
                        checkbox("outer_size", ::outerSizeEnabled)
                        sameLine()
                        helpMarker("""
                            If scrolling is disabled (ScrollX and ScrollY not set):
                            - The table is output directly in the parent window.
                            - OuterSize.x < 0.0f will right-align the table.
                            - OuterSize.x = 0.0f will narrow fit the table unless there are any Stretch column.
                            - OuterSize.y then becomes the minimum size for the table, which will extend vertically if there are more rows (unless NoHostExtendY is set).""".trimIndent())

                        // From a user point of view we will tend to use 'inner_width' differently depending on whether our table is embedding scrolling.
                        // toying with this demo we will actually pass 0.0f to the BeginTable() when ScrollX is disabled.
                        dragFloat("inner_width (when ScrollX active)", ::innerWidthWithScroll, 1f, 0f, Float.MAX_VALUE)

                        dragFloat("row_min_height", ::rowMinHeight, 1f, 0f, Float.MAX_VALUE)
                        sameLine(); helpMarker("Specify height of the Selectable item.")

                        dragInt("items_count", ::itemsCount, 0.1f, 0, 9999)
                        combo("items_type (first column)", ::contentsType2, contentsTypeNames2)
                        //filter.Draw("filter");
                    }
                    popItemWidth()
                }
                spacing()
            }

            // Recreate/reset item list if we changed the number of items
            if (items0.size != itemsCount) {
                items0 = Array(itemsCount) { items0.getOrElse(it) { MyItem() } }
                for (n in 0 until itemsCount) {
                    val templateN = n % templateItemsNames.size
                    val item = items1[n]
                    item.id = n
                    item.name = templateItemsNames[templateN]
                    item.quantity = if (templateN == 3) 10 else if (templateN == 4) 20 else 0 // Assign default quantities
                }
            }

            val parentDrawList = ImGui.windowDrawList
            val parentDrawListDrawCmdCount = parentDrawList.cmdBuffer.size
            val tableScrollCur = Vec2()         // For debug display
            val tableScrollMax = Vec2()         // "
            var tableDrawList: DrawList? = null // "

            val innerWidthToUse = if (flags20 has Tf.ScrollX) innerWidthWithScroll else 0f
            table("##table", 6, flags20, if (outerSizeEnabled) outerSizeValue else Vec2(0), innerWidthToUse) {
                // Declare columns
                // We use the "user_id" parameter of TableSetupColumn() to specify a user id that will be stored in the sort specifications.
                // This is so our sort function can identify a column given our own identifier. We could also identify them based on their index!
                tableSetupColumn("ID", Tcf.DefaultSort or Tcf.WidthFixed or Tcf.NoHide, 0f, MyItemColumnID.ID.ordinal)
                tableSetupColumn("Name", Tcf.WidthFixed.i, 0f, MyItemColumnID.Name.ordinal)
                tableSetupColumn("Action", Tcf.NoSort or Tcf.WidthFixed, 0f, MyItemColumnID.Action.ordinal)
                tableSetupColumn("Quantity", Tcf.PreferSortDescending.i, 0f, MyItemColumnID.Quantity.ordinal)
                tableSetupColumn("Description", Tcf.WidthStretch.i, 0f, MyItemColumnID.Description.ordinal)
                tableSetupColumn("Hidden", Tcf.DefaultHide or Tcf.NoSort)
                tableSetupScrollFreeze(freezeCols1, freezeRows1)

                // Sort our data if sort specs have been changed!
                val sortsSpecs = tableGetSortSpecs()
                if (sortsSpecs != null && sortsSpecs.specsDirty)
                    itemsNeedSort = true
                if (sortsSpecs != null && itemsNeedSort && items0.size > 1) {
                    MyItem.currentSortSpecs = sortsSpecs // Store in variable accessible by the sort function.
                    items1.sortedWith(MyItem.compareWithSortSpecs)
                    MyItem.currentSortSpecs = null
                    sortsSpecs.specsDirty = false
                }
                itemsNeedSort = false

                // Take note of whether we are currently sorting based on the Quantity field,
                // we will use this to trigger sorting when we know the data of this column has been modified.
                val sortsSpecsUsingQuantity = tableGetColumnFlags(3) has Tcf.IsSorted

                // Show headers
                if (showHeaders1)
                    tableHeadersRow()

                // Show data
                // FIXME-TABLE FIXME-NAV: How we can get decent up/down even though we have the buttons here?
                pushButtonRepeat(true)
                //                #if 1
                // Demonstrate using clipper for large vertical lists
                val clipper = ListClipper()
                clipper.begin(items0.size)
                while (clipper.step()) {
                    for (rowN in clipper.display)
                    //                    #else
                    //                    // Without clipper
                    //                    {
                    //                        for (int row_n = 0; row_n < items.Size; row_n++)
                    //                        #endif
                    {
                        val item = items1[rowN]
                        //if (!filter.PassFilter(item->Name))
                        //    continue;

                        val itemIsSelected = item.id in selection
                        pushID(item.id)
                        tableNextRow(Trf.None.i, rowMinHeight)
                        tableNextColumn()

                        // For the demo purpose we can select among different type of items submitted in the first column
                        val label = "%04d".format(item.id)
                        val type = ContentsType2.values().first { it.ordinal == contentsType2 }
                        if (type == ContentsType2.Text)
                            textUnformatted(label)
                        else if (type == ContentsType2.Button)
                            button(label)
                        else if (type == ContentsType2.SmallButton)
                            smallButton(label)
                        else if (type == ContentsType2.FillButton)
                            button(label, Vec2(-Float.MIN_VALUE, 0f))
                        else if (type == ContentsType2.Selectable || type == ContentsType2.SelectableSpanRow) {
                            val selectableFlags = when (type) {
                                ContentsType2.SelectableSpanRow -> SelectableFlag.SpanAllColumns or SelectableFlag.AllowItemOverlap
                                else -> SelectableFlag.None.i
                            }
                            if (selectable(label, itemIsSelected, selectableFlags, Vec2(0f, rowMinHeight)))
                                if (io.keyCtrl)
                                    if (itemIsSelected)
                                        selection -= item.id
                                    else
                                        selection += item.id
                                else {
                                    selection.clear()
                                    selection += item.id
                                }
                        }

                        if (tableNextColumn())
                            textUnformatted(item.name)

                        // Here we demonstrate marking our data set as needing to be sorted again if we modified a quantity,
                        // and we are currently sorting on the column showing the Quantity.
                        // To avoid triggering a sort while holding the button, we only trigger it when the button has been released.
                        // You will probably need a more advanced system in your code if you want to automatically sort when a specific entry changes.
                        if (tableNextColumn()) {
                            if (smallButton("Chop")) item.quantity += 1
                            if (sortsSpecsUsingQuantity && ImGui.isItemDeactivated) itemsNeedSort = true
                            sameLine()
                            if (smallButton("Eat")) item.quantity -= 1
                            if (sortsSpecsUsingQuantity && ImGui.isItemDeactivated) itemsNeedSort = true
                        }

                        if (tableNextColumn())
                            text("${item.quantity}")

                        tableNextColumn()
                        if (showWrappedText)
                            textWrapped("Lorem ipsum dolor sit amet")
                        else
                            text("Lorem ipsum dolor sit amet")

                        if (tableNextColumn())
                            text("1234")

                        popID()
                    }
                }
                popButtonRepeat()

                // Store some info to display debug details below
                tableScrollCur.put(ImGui.scrollX, ImGui.scrollY)
                tableScrollMax.put(ImGui.scrollMaxX, ImGui.scrollMaxY)
                tableDrawList = ImGui.windowDrawList
            }
            checkbox("Debug details", ::showDebugDetails)
            if (showDebugDetails)
                tableDrawList?.let { drawList ->
                    sameLine(0f, 0f)
                    val tableDrawListDrawCmdCount = drawList.cmdBuffer.size
                    if (drawList === parentDrawList)
                        text(": DrawCmd: +${tableDrawListDrawCmdCount - parentDrawListDrawCmdCount} (in same window)")
                    else
                        text(": DrawCmd: +${tableDrawListDrawCmdCount - 1} (in child window), Scroll: (%.f/%.f) (%.f/%.f)",
                             tableScrollCur.x, tableScrollMax.x, tableScrollCur.y, tableScrollMax.y)
                }
            treePop()
        }

        popID()

        ShowDemoWindowColumns()

        if (disableIndent)
            popStyleVar()
    }
}