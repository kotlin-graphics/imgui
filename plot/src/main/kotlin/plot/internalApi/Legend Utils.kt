@file:OptIn(ExperimentalStdlibApi::class)

package plot.internalApi

import glm_.vec2.Vec2
import imgui.*
import imgui.api.gImGui
import imgui.classes.DrawList
import imgui.internal.classes.Rect
import imgui.internal.round
import plot.api.PlotCol
import plot.api.PlotLegendFlag
import plot.api.PlotLocation

//-----------------------------------------------------------------------------
// [SECTION] Legend Utils
//-----------------------------------------------------------------------------

// Gets the position of an inner rect that is located inside of an outer rect according to an ImPlotLocation and padding amount.
fun getLocationPos(outerRect: Rect, innerSize: Vec2, loc: PlotLocation, pad: Vec2 = Vec2(0, 0)): Vec2 {
    val pos = Vec2(when {
                       loc has PlotLocation.West && loc hasnt PlotLocation.East -> outerRect.min.x + pad.x
                       loc hasnt PlotLocation.West && loc has PlotLocation.East -> outerRect.max.x - pad.x - innerSize.x
                       else -> outerRect.center.x - innerSize.x * 0.5f
                   },
                   when {
                       // legend reference point y
                       loc has PlotLocation.North && loc hasnt PlotLocation.South -> outerRect.min.y + pad.y
                       loc hasnt PlotLocation.North && loc has PlotLocation.South -> outerRect.max.y - pad.y - innerSize.y
                       else -> outerRect.center.y - innerSize.y * 0.5f
                   })
    pos.x = round(pos.x)
    pos.y = round(pos.y)
    return pos
}

// Calculates the bounding box size of a legend
fun calcLegendSize(items: PlotItemGroup, pad: Vec2, spacing: Vec2, vertical: Boolean): Vec2 {
    // vars
    val nItems = items.legendCount
    val txtHt = ImGui.textLineHeight
    val iconSize = txtHt
    // get label max width
    var maxLabelWidth = 0f
    var sumLabelWidth = 0f
    for (i in 0..<nItems) {
        val label = items getLegendLabel i
        val labelWidth = ImGui.calcTextSize(label, true).x
        maxLabelWidth = if (labelWidth > maxLabelWidth) labelWidth else maxLabelWidth
        sumLabelWidth += labelWidth
    }
    // calc legend size
    val legendSize = when {
        vertical -> Vec2(pad.x * 2 + iconSize + maxLabelWidth, pad.y * 2 + nItems * txtHt + (nItems - 1) * spacing.y)
        else -> Vec2(pad.x * 2 + iconSize * nItems + sumLabelWidth + (nItems - 1) * spacing.x, pad.y * 2 + txtHt)
    }
    return legendSize
}

val legendSortingComp = Comparator<Int> { a, b ->
    val items = gImPlot.sortItems!!
    val labelA = items getLegendLabel a
    val labelB = items getLegendLabel b
    labelA.compareTo(labelB)
}

// Renders legend entries into a bounding box
fun showLegendEntries(items: PlotItemGroup, legendBb: Rect, hovered: Boolean, pad: Vec2, spacing: Vec2, vertical: Boolean, drawList: DrawList): Boolean {
    // vars
    val txtHt = ImGui.textLineHeight
    val iconSize = txtHt
    var iconShrink = 2
    val colTxt = PlotCol.LegendText.u32
    val colTxtDis = alphaU32(colTxt, 0.25f)
    // render each legend item
    var sumLabelWidth = 0f
    var anyItemHovered = false

    val numItems = items.legendCount
    if (numItems < 1)
        return hovered
    // build render order
    val gp = gImPlot
    var indices = IntArray(numItems) { it }
    if (items.legend.flags has PlotLegendFlag.Sort && numItems > 1) {
        gp.sortItems = items
        indices = indices.sortedWith(legendSortingComp).toIntArray()
    }
    // render
    for (i in 0..<numItems) {
        val idx = indices[i]
        val item = items getLegendItem idx
        val label = items getLegendLabel idx
        val labelWidth = ImGui.calcTextSize(label, true).x
        val topLeft = legendBb.min + pad + when {
            vertical -> Vec2(0, i * (txtHt + spacing.y))
            else -> Vec2(i * (iconSize + spacing.x) + sumLabelWidth, 0)
        }
        sumLabelWidth += labelWidth
        val iconBb = Rect(topLeft + iconShrink, topLeft + iconSize - iconShrink)
        val labelBb = Rect(topLeft, topLeft + Vec2(labelWidth + iconSize, iconSize))
        val colItem = alphaU32(item.color, 1f)

        val buttonBb = Rect(iconBb.min, labelBb.max)

        ImGui.keepAliveID(item.id)

        val (itemClk, itemHov, itemHld) = if (items.legend.flags has PlotLegendFlag.NoButtons) BooleanArray(3) else ImGui.buttonBehavior(buttonBb, item.id)

        if (itemClk)
            item.show = !item.show

        val canHover = itemHov && (items.legend.flags hasnt PlotLegendFlag.NoHighlightItem || items.legend.flags hasnt PlotLegendFlag.NoHighlightAxis)

        val colTxtHl = if (canHover) {
            item.legendHoverRect.min put iconBb.min
            item.legendHoverRect.max put labelBb.max
            item.legendHovered = true
            anyItemHovered = true
            mixU32(colTxt, colItem, 64)
        } else
            ImGui.getColorU32(colTxt.toInt()).toUInt()
        val colIcon = when {
            itemHld -> if (item.show) alphaU32(colItem, 0.5f) else ImGui.getColorU32(Col.TextDisabled, 0.5f).toUInt()
            itemHov -> if (item.show) alphaU32(colItem, 0.75f) else ImGui.getColorU32(Col.TextDisabled, 0.75f).toUInt()
            else -> if (item.show) colItem else colTxtDis
        }
        drawList.addRectFilled(iconBb.min, iconBb.max, colIcon.toInt())
        val textDisplayEnd = ImGui.findRenderedTextEnd(label)
        if (0 != textDisplayEnd)
            drawList.addText(topLeft + Vec2(iconSize, 0), if (item.show) colTxtHl.toInt() else colTxtDis.toInt(), label.take(textDisplayEnd))
    }
    return hovered && !anyItemHovered
}

// Shows an alternate legend for the plot identified by #title_id, outside of the plot frame (can be called before or after of Begin/EndPlot but must occur in the same ImGui window!).
fun showAltLegend(titleId: String, vertical: Boolean = true, size: Vec2 = Vec2(), interactable_: Boolean = true) {
    var interactable = interactable_
    val gp = gImPlot
    val g = gImGui
    val window = g.currentWindow!!
    if (window.skipItems)
        return
    val drawList = window.drawList
    val plot = getPlot(titleId)
    val legendSize = Vec2()
    val defaultSize = gp.style.legendPadding * 2
    if (plot != null) {
        legendSize put calcLegendSize(plot.items, gp.style.legendInnerPadding, gp.style.legendSpacing, vertical)
        defaultSize put (legendSize + gp.style.legendPadding * 2)
    }
    val frameSize = ImGui.calcItemSize(size, defaultSize.x, defaultSize.y)
    val bbFrame = Rect(window.dc.cursorPos, window.dc.cursorPos + frameSize)
    ImGui.itemSize(bbFrame)
    if (!ImGui.itemAdd(bbFrame, 0, bbFrame))
        return
    ImGui.renderFrame(bbFrame.min, bbFrame.max, PlotCol.FrameBg.u32.toInt(), true, g.style.frameRounding)
    drawList.pushClipRect(bbFrame.min, bbFrame.max, true)
    if (plot != null) {
        val legendPos = getLocationPos(bbFrame, legendSize, PlotLocation.Center, gp.style.legendPadding)
        val legendBb = Rect(legendPos, legendPos + legendSize)
        interactable = interactable && ImGui.io.mousePos in bbFrame
        // render legend box
        val colBg = PlotCol.LegendBg.u32
        val colBd = PlotCol.LegendBorder.u32
        drawList.addRectFilled(legendBb.min, legendBb.max, colBg.toInt())
        drawList.addRect(legendBb.min, legendBb.max, colBd.toInt())
        // render entries
        showLegendEntries(plot.items, legendBb, interactable, gp.style.legendInnerPadding, gp.style.legendSpacing, vertical, drawList)
    }
    drawList.popClipRect()
}

// Shows an legends's context menu.
fun showLegendContextMenu(legend: PlotLegend, visible_: Boolean): Boolean {
    var visible = visible_
    val s = ImGui.frameHeight
    var ret = false
    if (ImGui.checkbox("Show", visible.mutableReference))
        ret = true
    if (legend.canGoInside)
        ImGui.checkboxFlags("Outside", legend::flags, PlotLegendFlag.Outside)
    if (ImGui.radioButton("H", legend.flags has PlotLegendFlag.Horizontal))
        legend.flags /= PlotLegendFlag.Horizontal
    ImGui.sameLine()
    if (ImGui.radioButton("V", legend.flags hasnt PlotLegendFlag.Horizontal))
        legend.flags -= PlotLegendFlag.Horizontal
    ImGui.pushStyleVar(StyleVar.ItemSpacing, Vec2(2))
    if (ImGui.button("NW", Vec2(1.5f * s, s))) {; legend.location = PlotLocation.NorthWest; }; ImGui.sameLine()
    if (ImGui.button("N", Vec2(1.5f * s, s))) {; legend.location = PlotLocation.North; }; ImGui.sameLine()
    if (ImGui.button("NE", Vec2(1.5f * s, s))) {; legend.location = PlotLocation.NorthEast; }
    if (ImGui.button("W", Vec2(1.5f * s, s))) {; legend.location = PlotLocation.West; }; ImGui.sameLine()
    if (ImGui.invisibleButton("C", Vec2(1.5f * s, s))) {; }; ImGui.sameLine()
    if (ImGui.button("E", Vec2(1.5f * s, s))) {; legend.location = PlotLocation.East; }
    if (ImGui.button("SW", Vec2(1.5f * s, s))) {; legend.location = PlotLocation.SouthWest; }; ImGui.sameLine()
    if (ImGui.button("S", Vec2(1.5f * s, s))) {; legend.location = PlotLocation.South; }; ImGui.sameLine()
    if (ImGui.button("SE", Vec2(1.5f * s, s))) {; legend.location = PlotLocation.SouthEast; }
    ImGui.popStyleVar()
    return ret
}