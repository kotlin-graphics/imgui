@file:OptIn(ExperimentalStdlibApi::class)

package plot.api

import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.isDoubleClicked
import imgui.api.gImGui
import imgui.internal.classes.Rect
import imgui.internal.round
import imgui.internal.sections.ButtonFlag
import plot.internalApi.*

//-----------------------------------------------------------------------------
// BEGIN/END SUBPLOT
//-----------------------------------------------------------------------------

const val SUBPLOT_BORDER_SIZE = 1f
const val SUBPLOT_SPLITTER_HALF_THICKNESS = 4f
const val SUBPLOT_SPLITTER_FEEDBACK_TIMER = 0.06f

fun subplotSetCell(row: Int, col: Int) {
    val gp = gImPlot
    val subplot = gp.currentSubplot!!
    if (row >= subplot.rows || col >= subplot.cols)
        return
    var xOff = 0f
    var yOff = 0f
    for (c in 0..<col)
        xOff += subplot.colRatios[c]
    for (r in 0..<row)
        yOff += subplot.rowRatios[r]
    val gridSize = subplot.gridRect.size
    val cPos = subplot.gridRect.min + Vec2(xOff * gridSize.x, yOff * gridSize.y)
    cPos.x = round(cPos.x)
    cPos.y = round(cPos.y)
    ImGui.currentWindow.dc.cursorPos put cPos
    // set cell size
    subplot.cellSize.x = round(subplot.gridRect.width * subplot.colRatios[col])
    subplot.cellSize.y = round(subplot.gridRect.height * subplot.rowRatios[row])
    // setup links
    val lX = subplot.flags has PlotSubplotFlag.LinkAllX
    val lY = subplot.flags has PlotSubplotFlag.LinkAllY
    val lR = subplot.flags has PlotSubplotFlag.LinkRows
    val lC = subplot.flags has PlotSubplotFlag.LinkCols

    setNextAxisLinks(Axis.X1, if (lX) subplot.colLinkData[0]::min else if (lC) subplot.colLinkData[col]::min else null,
                     if (lX) subplot.colLinkData[0]::max else if (lC) subplot.colLinkData[col]::max else null)
    setNextAxisLinks(Axis.Y1, if (lY) subplot.rowLinkData[0]::min else if (lR) subplot.rowLinkData[row]::min else null,
                     if (lY) subplot.rowLinkData[0]::max else if (lR) subplot.rowLinkData[row]::max else null)
    // setup alignment
    if (subplot.flags hasnt PlotSubplotFlag.NoAlign) {
        gp.currentAlignmentH = subplot.rowAlignmentData[row]
        gp.currentAlignmentV = subplot.colAlignmentData[col]
    }
    // set idx
    subplot.currentIdx = when {
        subplot.flags has PlotSubplotFlag.ColMajor -> col * subplot.rows + row
        else -> row * subplot.cols + col
    }
}

fun subplotSetCell(idx: Int) {
    val gp = gImPlot
    val subplot = gp.currentSubplot!!
    if (idx >= subplot.rows * subplot.cols)
        return
    val row: Int
    val col: Int
    if (subplot.flags has PlotSubplotFlag.ColMajor) {
        row = idx % subplot.rows
        col = idx / subplot.rows
    } else {
        row = idx / subplot.cols
        col = idx % subplot.cols
    }
    return subplotSetCell(row, col)
}

fun subplotNextCell() {
    val gp = gImPlot
    val subplot = gp.currentSubplot!!
    subplotSetCell(++subplot.currentIdx)
}


//-----------------------------------------------------------------------------
// [SECTION] Begin/End Subplots
//-----------------------------------------------------------------------------

// Starts a subdivided plotting context. If the function returns true,
// EndSubplots() MUST be called! Call BeginPlot/EndPlot AT MOST [rows*cols]
// times in  between the begining and end of the subplot context. Plots are
// added in row major order.
//
// Example:
//
// if (BeginSubplots("My Subplot",2,3,ImVec2(800,400)) {
//     for (int i = 0; i < 6; ++i) {
//         if (BeginPlot(...)) {
//             ImPlot::PlotLine(...);
//             ...
//             EndPlot();
//         }
//     }
//     EndSubplots();
// }
//
// Produces:
//
// [0] | [1] | [2]
// ----|-----|----
// [3] | [4] | [5]
//
// Important notes:
//
// - #title_id must be unique to the current ImGui ID scope. If you need to avoid ID
//   collisions or don't want to display a title in the plot, use double hashes
//   (e.g. "MySubplot##HiddenIdText" or "##NoTitle").
// - #rows and #cols must be greater than 0.
// - #size is the size of the entire grid of subplots, not the individual plots
// - #row_ratios and #col_ratios must have AT LEAST #rows and #cols elements,
//   respectively. These are the sizes of the rows and columns expressed in ratios.
//   If the user adjusts the dimensions, the arrays are updated with new ratios.
//
// Important notes regarding BeginPlot from inside of BeginSubplots:
//
// - The #title_id parameter of _BeginPlot_ (see above) does NOT have to be
//   unique when called inside of a subplot context. Subplot IDs are hashed
//   for your convenience so you don't have call PushID or generate unique title
//   strings. Simply pass an empty string to BeginPlot unless you want to title
//   each subplot.
// - The #size parameter of _BeginPlot_ (see above) is ignored when inside of a
//   subplot context. The actual size of the subplot will be based on the
//   #size value you pass to _BeginSubplots_ and #row/#col_ratios if provided.

fun beginSubplots(title: String, rows: Int, cols: Int, size: Vec2, flags: PlotSubplotFlags, rowSizes: FloatArray? = null, colSizes: FloatArray? = null): Boolean {
    assert(rows > 0 && cols > 0) { "Invalid sizing arguments!" }
    assert(gImPlotNullable != null) { "No current context. Did you call ImPlot::CreateContext() or ImPlot::SetCurrentContext()?" }
    val gp = gImPlot
    assert(gp.currentSubplot == null) { "Mismatched BeginSubplots()/EndSubplots()!" }
    val g = gImGui
    val window = g.currentWindow!!
    if (window.skipItems)
        return false
    val id = window.getID(title)
    val justCreated = gp.subplots.getByKey(id) == null
    gp.currentSubplot = gp.subplots.getOrAddByKey(id)
    val subplot = gp.currentSubplot!!
    subplot.id = id
    subplot.items.id = id - 1
    subplot.hasTitle = ImGui.findRenderedTextEnd(title) != 0
    // push ID
    ImGui.pushID(id)

    if (justCreated)
        subplot.flags = flags
    else if (flags != subplot.previousFlags)
        subplot.flags = flags
    subplot.previousFlags = flags

    // check for change in rows and cols
    if (subplot.rows != rows || subplot.cols != cols) {
        subplot.rowAlignmentData = Array(rows) { PlotAlignmentData() }
        subplot.rowLinkData = Array(rows) { PlotRange(0.0, 1.0) }
        subplot.rowRatios = FloatArray(rows) { 1f / it }
        subplot.colAlignmentData = Array(cols) { PlotAlignmentData() }
        subplot.colLinkData = Array(cols) { PlotRange(0.0, 1.0) }
        subplot.colRatios = FloatArray(cols) { 1f / cols }
    }
    // check incoming size requests
    var rowSum = 0f
    var colSum = 0f
    if (rowSizes != null) {
        rowSum = rowSizes.take(rows).sum()
        for (r in 0..<rows)
            subplot.rowRatios[r] = rowSizes[r] / rowSum
    }
    if (colSizes != null) {
        colSum = colSizes.take(cols).sum()
        for (c in 0..<cols)
            subplot.colRatios[c] = colSizes[c] / colSum
    }
    subplot.rows = rows
    subplot.cols = cols

    // calc plot frame sizes
    val titleSize = Vec2()
    if (subplot.flags hasnt PlotSubplotFlag.NoTitle)
        titleSize put ImGui.calcTextSize(title, true)
    val padTop = if (titleSize.x > 0f) titleSize.y + gp.style.labelPadding.y else 0f
    val halfPad = gp.style.plotPadding / 2
    val frameSize = ImGui.calcItemSize(size, gp.style.plotDefaultSize.x, gp.style.plotDefaultSize.y)
    subplot.frameRect.put(window.dc.cursorPos, window.dc.cursorPos + frameSize)
    subplot.gridRect.min = subplot.frameRect.min + halfPad + Vec2(0, padTop)
    subplot.gridRect.max = subplot.frameRect.max - halfPad
    subplot.frameHovered = subplot.frameRect.contains(ImGui.mousePos) && ImGui.isWindowHovered(HoveredFlag.ChildWindows)

    // outside legend adjustments (TODO: make function)
    val shareItems = subplot.flags has PlotSubplotFlag.ShareItems
    if (shareItems)
        gp.currentItems = subplot.items
    if (shareItems && subplot.flags hasnt PlotSubplotFlag.NoLegend && subplot.items.legendCount > 0) {
        val legend = subplot.items.legend
        val horz = legend.flags has PlotLegendFlag.Horizontal
        val legendSize = calcLegendSize(subplot.items, gp.style.legendInnerPadding, gp.style.legendSpacing, !horz)
        val west = legend.location has PlotLocation.West && legend.location hasnt PlotLocation.East
        val east = legend.location has PlotLocation.East && legend.location hasnt PlotLocation.West
        val north = legend.location has PlotLocation.North && legend.location hasnt PlotLocation.South
        val south = legend.location has PlotLocation.South && legend.location hasnt PlotLocation.North
        if ((west && !horz) || (west && horz && !north && !south))
            subplot.gridRect.min.x += legendSize.x + gp.style.legendPadding.x
        if ((east && !horz) || (east && horz && !north && !south))
            subplot.gridRect.max.x -= legendSize.x + gp.style.legendPadding.x
        if ((north && horz) || (north && !horz && !west && !east))
            subplot.gridRect.min.y += legendSize.y + gp.style.legendPadding.y
        if ((south && horz) || (south && !horz && !west && !east))
            subplot.gridRect.max.y -= legendSize.y + gp.style.legendPadding.y
    }

    // render single background frame
    ImGui.renderFrame(subplot.frameRect.min, subplot.frameRect.max, PlotCol.FrameBg.u32.toInt(), true, ImGui.style.frameRounding)
    // render title
    if (titleSize.x > 0f && subplot.flags hasnt PlotSubplotFlag.NoTitle) {
        val col = PlotCol.TitleText.u32
        addTextCentered(ImGui.windowDrawList, Vec2(subplot.gridRect.center.x, subplot.gridRect.min.y - padTop + halfPad.y), col, title.toByteArray())
    }

    // render splitters
    if (subplot.flags hasnt PlotSubplotFlag.NoResize) {
        val drawlist = ImGui.windowDrawList
        val hovCol = gImGui.style.colors[Col.SeparatorHovered].u32
        val actCol = gImGui.style.colors[Col.SeparatorActive].u32
        var xPos = subplot.gridRect.min.x
        var yPos = subplot.gridRect.min.y
        var separator = 1
        // bool pass = false;
        for (r in 0..<subplot.rows - 1) {
            yPos += subplot.rowRatios[r] * subplot.gridRect.height
            val sepId = subplot.id + separator
            ImGui.keepAliveID(sepId)
            val sepBb = Rect(subplot.gridRect.min.x, yPos - SUBPLOT_SPLITTER_HALF_THICKNESS, subplot.gridRect.max.x, yPos + SUBPLOT_SPLITTER_HALF_THICKNESS)
            val (sepClk, sepHov, sepHld) = ImGui.buttonBehavior(sepBb, sepId, ButtonFlag.FlattenChildren / ButtonFlag.AllowItemOverlap / ButtonFlag.PressedOnClick / ButtonFlag.PressedOnDoubleClick)
            if ((sepHov && g.hoveredIdTimer > SUBPLOT_SPLITTER_FEEDBACK_TIMER) || sepHld) {
                if (sepClk && MouseButton.Left.isDoubleClicked) {
                    val p = (subplot.rowRatios[r] + subplot.rowRatios[r + 1]) / 2
                    subplot.rowRatios[r] = p; subplot.rowRatios[r + 1] = p
                }
                if (sepClk) {
                    subplot.tempSizes[0] = subplot.rowRatios[r]
                    subplot.tempSizes[1] = subplot.rowRatios[r + 1]
                }
                if (sepHld) {
                    val dp = ImGui.getMouseDragDelta(MouseButton.Left).y / subplot.gridRect.height
                    if (subplot.tempSizes[0] + dp > 0.1f && subplot.tempSizes[1] - dp > 0.1f) {
                        subplot.rowRatios[r] = subplot.tempSizes[0] + dp
                        subplot.rowRatios[r + 1] = subplot.tempSizes[1] - dp
                    }
                }
                drawlist.addLine(Vec2(round(subplot.gridRect.min.x), round(yPos)), Vec2(round(subplot.gridRect.max.x), round(yPos)), if (sepHld) actCol else hovCol, SUBPLOT_BORDER_SIZE)
                ImGui.mouseCursor = MouseCursor.ResizeNS
            }
            separator++
        }
        for (c in 0..<subplot.cols - 1) {
            xPos += subplot.colRatios[c] * subplot.gridRect.width
            val sepId = subplot.id + separator
            ImGui.keepAliveID(sepId)
            val sepBb = Rect(xPos - SUBPLOT_SPLITTER_HALF_THICKNESS, subplot.gridRect.min.y, xPos + SUBPLOT_SPLITTER_HALF_THICKNESS, subplot.gridRect.max.y)
            val (sepClk, sepHov, sepHld) = ImGui.buttonBehavior(sepBb, sepId, ButtonFlag.FlattenChildren / ButtonFlag.AllowItemOverlap / ButtonFlag.PressedOnClick / ButtonFlag.PressedOnDoubleClick)
            if ((sepHov && g.hoveredIdTimer > SUBPLOT_SPLITTER_FEEDBACK_TIMER) || sepHld) {
                if (sepClk && MouseButton.Left.isDoubleClicked) {
                    val p = (subplot.colRatios[c] + subplot.colRatios[c + 1]) / 2
                    subplot.colRatios[c] = p; subplot.colRatios[c + 1] = p
                }
                if (sepClk) {
                    subplot.tempSizes[0] = subplot.colRatios[c]
                    subplot.tempSizes[1] = subplot.colRatios[c + 1]
                }
                if (sepHld) {
                    val dp = ImGui.getMouseDragDelta(MouseButton.Left).x / subplot.gridRect.width
                    if (subplot.tempSizes[0] + dp > 0.1f && subplot.tempSizes[1] - dp > 0.1f) {
                        subplot.colRatios[c] = subplot.tempSizes[0] + dp
                        subplot.colRatios[c + 1] = subplot.tempSizes[1] - dp
                    }
                }
                drawlist.addLine(Vec2(round(xPos), round(subplot.gridRect.min.y)), Vec2(round(xPos), round(subplot.gridRect.max.y)), if (sepHld) actCol else hovCol, SUBPLOT_BORDER_SIZE)
                ImGui.mouseCursor = MouseCursor.ResizeEW
            }
            separator++
        }
    }

    // set outgoing sizes
    if (rowSizes != null) {
        for (r in 0..<rows)
            rowSizes[r] = subplot.rowRatios[r] * rowSum
    }
    if (colSizes != null) {
        for (c in 0..<cols)
            colSizes[c] = subplot.colRatios[c] * colSum
    }

    // push styling
    pushStyleColor(PlotCol.FrameBg, COL32_BLACK_TRANS.toUInt())
    pushStyleVar(PlotStyleVar.PlotPadding, halfPad)
    pushStyleVar(PlotStyleVar.PlotMinSize, Vec2())
    ImGui.pushStyleVar(StyleVar.FrameBorderSize, 0)

    // set initial cursor pos
    window.dc.cursorPos put subplot.gridRect.min
    // begin alignments
    for (r in 0..<subplot.rows)
        subplot.rowAlignmentData[r].begin()
    for (c in 0..<subplot.cols)
        subplot.colAlignmentData[c].begin()
    // clear legend data
    subplot.items.legend.reset()
    // Setup first subplot
    subplotSetCell(0, 0)
    return true
}

fun endSubplots() {
    assert(gImPlotNullable != null) { "No current context. Did you call ImPlot::CreateContext() or ImPlot::SetCurrentContext()?" }
    val gp = gImPlot
    assert(gp.currentSubplot != null) { "Mismatched BeginSubplots()/EndSubplots()!" }
    val subplot = gp.currentSubplot!!
    // set alignments
    for (r in 0..<subplot.rows)
        subplot.rowAlignmentData[r].end()
    for (c in 0..<subplot.cols)
        subplot.colAlignmentData[c].end()
    // pop styling
    popStyleColor()
    popStyleVar()
    popStyleVar()
    ImGui.popStyleVar()
    // legend
    subplot.items.legend.hovered = false
    for (i in 0..<subplot.items.itemCount)
        subplot.items.getItemByIndex(i).legendHovered = false
    // render legend
    val shareItems = subplot.flags has PlotSubplotFlag.ShareItems
    val drawlist = ImGui.windowDrawList
    if (shareItems && subplot.flags hasnt PlotSubplotFlag.NoLegend && subplot.items.legendCount > 0) {
        val legendHorz = subplot.items.legend.flags has PlotLegendFlag.Horizontal
        val legendSize = calcLegendSize(subplot.items, gp.style.legendInnerPadding, gp.style.legendSpacing, !legendHorz)
        val legendPos = getLocationPos(subplot.frameRect, legendSize, subplot.items.legend.location, gp.style.plotPadding)
        subplot.items.legend.rect.put(legendPos, legendPos + legendSize)
        subplot.items.legend.hovered = subplot.frameHovered && ImGui.io.mousePos in subplot.items.legend.rect
        ImGui.pushClipRect(subplot.frameRect.min, subplot.frameRect.max, true)
        val colBg = PlotCol.LegendBg.u32
        val colBd = PlotCol.LegendBorder.u32
        drawlist.addRectFilled(subplot.items.legend.rect.min, subplot.items.legend.rect.max, colBg.toInt())
        drawlist.addRect(subplot.items.legend.rect.min, subplot.items.legend.rect.max, colBd.toInt())
        val legendContextable = showLegendEntries(subplot.items, subplot.items.legend.rect, subplot.items.legend.hovered, gp.style.legendInnerPadding, gp.style.legendSpacing, !legendHorz, drawlist)
                && subplot.items.legend.flags hasnt PlotLegendFlag.NoMenus
        if (legendContextable && subplot.flags hasnt PlotSubplotFlag.NoMenus && ImGui.io.mouseReleased[gp.inputMap.menu.i])
            ImGui.openPopup("##LegendContext")
        ImGui.popClipRect()
        if (ImGui.beginPopup("##LegendContext")) {
            ImGui.text("Legend"); ImGui.separator()
            if (showLegendContextMenu(subplot.items.legend, subplot.flags hasnt PlotSubplotFlag.NoLegend)) // PR?
                subplot::flags flip PlotSubplotFlag.NoLegend
            ImGui.endPopup()
        }
    } else
        subplot.items.legend.rect put Rect()
    // remove items
    if (gp.currentItems === subplot.items)
        gp.currentItems = null
    // reset the plot items for the next frame (TODO: put this elswhere)
    for (i in 0..<subplot.items.itemCount)
        subplot.items.getItemByIndex(i).seenThisFrame = false
    // pop id
    ImGui.popID()
    // set DC back correctly
    gImGui.currentWindow!!.dc.cursorPos put subplot.frameRect.min
    ImGui.dummy(subplot.frameRect.size)
    gImPlot.resetCtxForNextSubplot()
}