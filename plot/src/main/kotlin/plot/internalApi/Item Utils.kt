@file:OptIn(ExperimentalStdlibApi::class)

package plot.internalApi

import imgui.*
import plot.api.*
import plot.items.Fitter
import kotlin.reflect.KMutableProperty0

//-----------------------------------------------------------------------------
// [SECTION] Item Utils
//-----------------------------------------------------------------------------

const val ITEM_HIGHLIGHT_LINE_SCALE = 2f
const val ITEM_HIGHLIGHT_MARK_SCALE = 1.25f

// Begins a new item. Returns false if the item should not be plotted. Pushes PlotClipRect.
fun beginItem(labelId: String, flags: PlotItemFlags = none, recolorFrom: PlotCol = PlotCol.None): Boolean {
    val gp = gImPlot
    assert(gp.currentPlot != null) { "PlotX() needs to be called between BeginPlot() and EndPlot()!" }
    setupLock()
    val justCreated = false
    val item = registerOrGetItem(labelId, flags, justCreated.mutableReference)
    // set current item
    gp.currentItem = item
    val s = gp.nextItemData
    // set/override item color
    if (recolorFrom != PlotCol.None) {
        if (!s.colors[recolorFrom].isColorAuto)
            item.color = s.colors[recolorFrom].u32.toUInt()
        else if (!gp.style.colors[recolorFrom].isColorAuto)
            item.color = gp.style.colors[recolorFrom].u32.toUInt()
        else if (justCreated)
            item.color = nextColormapColorU32()
    } else if (justCreated)
        item.color = nextColormapColorU32()

    // hide/show item
    if (gp.nextItemData.hasHidden) {
        if (justCreated || gp.nextItemData.hiddenCond == Cond.Always)
            item.show = !gp.nextItemData.hidden
    }
    if (!item.show) {
        // reset next item data
        gp.nextItemData.reset()
        gp.previousItem = item
        gp.currentItem = null
        return false
    } else {
        val itemColor = item.color.toInt().vec4
        // stage next item colors
        s.colors[PlotCol.Line] = if (s.colors[PlotCol.Line].isColorAuto) if (PlotCol.Line.isColorAuto) itemColor else gp.style.colors[PlotCol.Line] else s.colors[PlotCol.Line]
        s.colors[PlotCol.Fill] = if (s.colors[PlotCol.Fill].isColorAuto) if (PlotCol.Fill.isColorAuto) itemColor else gp.style.colors[PlotCol.Fill] else s.colors[PlotCol.Fill]
        s.colors[PlotCol.MarkerOutline] = if (s.colors[PlotCol.MarkerOutline].isColorAuto) if (PlotCol.MarkerOutline.isColorAuto) s.colors[PlotCol.Line] else gp.style.colors[PlotCol.MarkerOutline] else s.colors[PlotCol.MarkerOutline]
        s.colors[PlotCol.MarkerFill] = if (s.colors[PlotCol.MarkerFill].isColorAuto) if (PlotCol.MarkerFill.isColorAuto) s.colors[PlotCol.Line] else gp.style.colors[PlotCol.MarkerFill] else s.colors[PlotCol.MarkerFill]
        s.colors[PlotCol.ErrorBar] = if (s.colors[PlotCol.ErrorBar].isColorAuto) PlotCol.ErrorBar.vec4 else s.colors[PlotCol.ErrorBar]
        // stage next item style vars
        s.lineWeight = if (s.lineWeight < 0) gp.style.lineWeight else s.lineWeight
        s.marker = if (s.marker.i < 0) gp.style.marker else s.marker
        s.markerSize = if (s.markerSize < 0) gp.style.markerSize else s.markerSize
        s.markerWeight = if (s.markerWeight < 0) gp.style.markerWeight else s.markerWeight
        s.fillAlpha = if (s.fillAlpha < 0) gp.style.fillAlpha else s.fillAlpha
        s.errorBarSize = if (s.errorBarSize < 0) gp.style.errorBarSize else s.errorBarSize
        s.errorBarWeight = if (s.errorBarWeight < 0) gp.style.errorBarWeight else s.errorBarWeight
        s.digitalBitHeight = if (s.digitalBitHeight < 0) gp.style.digitalBitHeight else s.digitalBitHeight
        s.digitalBitGap = if (s.digitalBitGap < 0) gp.style.digitalBitGap else s.digitalBitGap
        // apply alpha modifier(s)
        s.colors[PlotCol.Fill].w *= s.fillAlpha
        s.colors[PlotCol.MarkerFill].w *= s.fillAlpha // TODO: this should be separate, if it at all
        // apply highlight mods
        if (item.legendHovered) {
            if (gp.currentItems!!.legend.flags hasnt PlotLegendFlag.NoHighlightItem) {
                s.lineWeight *= ITEM_HIGHLIGHT_LINE_SCALE
                s.markerSize *= ITEM_HIGHLIGHT_MARK_SCALE
                s.markerWeight *= ITEM_HIGHLIGHT_LINE_SCALE
                // TODO: how to highlight fills?
            }
            if (gp.currentItems!!.legend.flags hasnt PlotLegendFlag.NoHighlightAxis) {
                if (gp.currentPlot!!.enabledAxesX() > 1)
                    gp.currentPlot!!.axes[gp.currentPlot!!.currentX].colorHiLi = item.color
                if (gp.currentPlot!!.enabledAxesY() > 1)
                    gp.currentPlot!!.axes[gp.currentPlot!!.currentY].colorHiLi = item.color
            }
        }
        // set render flags
        s.renderLine = s.colors[PlotCol.Line].w > 0 && s.lineWeight > 0
        s.renderFill = s.colors[PlotCol.Fill].w > 0
        s.renderMarkerFill = s.colors[PlotCol.MarkerFill].w > 0
        s.renderMarkerLine = s.colors[PlotCol.MarkerOutline].w > 0 && s.markerWeight > 0
        // push rendering clip rect
        pushPlotClipRect()
        return true
    }
}

// Same as above but with fitting functionality.
//template <typename _Fitter>
fun beginItemEx(labelId: String, fitter: Fitter, flags: PlotItemFlags = none, recolorFrom: PlotCol = PlotCol.None): Boolean {
    if (beginItem(labelId, flags, recolorFrom)) {
        val plot = currentPlot!!
        if (plot.fitThisFrame && flags hasnt PlotItemFlag.NoFit)
            fitter.fit(plot.axes[plot.currentX], plot.axes[plot.currentY])
        return true
    }
    return false
}

// Ends an item (call only if BeginItem returns true). Pops PlotClipRect.
fun endItem() {
    val gp = gImPlot
    // pop rendering clip rect
    popPlotClipRect()
    // reset next item data
    gp.nextItemData.reset()
    // set current item
    gp.previousItem = gp.currentItem
    gp.currentItem = null
}

// Register or get an existing item from the current plot.
fun registerOrGetItem(labelId: String, flags: PlotItemFlags, justCreated: KMutableProperty0<Boolean>?): PlotItem {
    val gp = gImPlot
    val items = gp.currentItems!!
    val id = items getItemID labelId
    justCreated?.set(items getItem id == null)
    val item = items getOrAddItem id
    if (item.seenThisFrame)
        return item
    item.seenThisFrame = true
    val idx = items getItemIndex item
    item.id = id
    if (flags hasnt PlotItemFlag.NoLegend && ImGui.findRenderedTextEnd(labelId) != 0) {
        items.legend.indices += idx.i
        item.nameOffset = items.legend.labels.size
        items.legend.labels += labelId
    } else
        item.show = true
    return item
}

// Get a plot item from the current plot.
fun getItem(labelId: String): PlotItem? {
    val gp = gImPlot
    return gp.currentItems!! getItem labelId
}

// Gets the current item.
val currentItem: PlotItem?
    get() {
        val gp = gImPlot
        return gp.currentItem
    }
// Busts the cache for every item for every plot in the current context.
fun bustItemCache() {
    val gp = gImPlot
    for (p in 0 ..< gp.plots.bufSize) {
        val plot = gp.plots getByIndex p
        plot.items.reset()
    }
    for (p in 0 ..< gp.subplots.bufSize) {
        val subplot = gp.subplots getByIndex p
        subplot.items.reset()
    }
}