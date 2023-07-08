@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalStdlibApi::class)

package plot.api

import glm_.f
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.api.gImGui
import imgui.api.slider
import imgui.internal.classes.Rect
import plot.internalApi.*
import kotlin.reflect.KMutableProperty0

//-----------------------------------------------------------------------------
// [SECTION] Colormaps
//-----------------------------------------------------------------------------

// Item styling is based on colormaps when the relevant ImPlotCol_XXX is set to
// IMPLOT_AUTO_COL (default). Several built-in colormaps are available. You can
// add and then push/pop your own colormaps as well. To permanently set a colormap,
// modify the Colormap index member of your ImPlotStyle.

// Colormap data will be ignored and a custom color will be used if you have done one of the following:
//     1) Modified an item style color in your ImPlotStyle to anything other than IMPLOT_AUTO_COL.
//     2) Pushed an item style color using PushStyleColor().
//     3) Set the next item style with a SetNextXXXStyle function.

// Add a new colormap. The color data will be copied. The colormap can be used by pushing either the returned index or the
// string name with PushColormap. The colormap name must be unique and the size must be greater than 1. You will receive
// an assert otherwise! By default colormaps are considered to be qualitative (i.e. discrete). If you want to create a
// continuous colormap, set #qual=false. This will treat the colors you provide as keys, and ImPlot will build a linearly
// interpolated lookup table. The memory footprint of this table will be exactly ((size-1)*255+1)*4 bytes.
fun addColormap(name: String, colormap: List<Vec4>, qual: Boolean = true): PlotColormap {
    val gp = gImPlot
    assert(colormap.size > 1) { "The colormap size must be greater than 1!" }
    assert(gp.colormapData getIndex name == PlotColormap.None) { "The colormap name has already been used!" }
    val buffer = UIntArray(colormap.size) { colormap[it].u32.toUInt() }
    return gp.colormapData.append(name, buffer, qual)
}

fun addColormap(name: String, colormap: UIntArray, qual: Boolean = true): PlotColormap {
    val gp = gImPlot
    assert(colormap.size > 1) { "The colormap size must be greater than 1!" }
    assert(gp.colormapData getIndex name == PlotColormap.None) { "The colormap name has already be used!" }
    return gp.colormapData.append(name, colormap, qual)
}

// Returns the number of available colormaps (i.e. the built-in + user-added count).
val colormapCount: Int
    get() {
        val gp = gImPlot
        return gp.colormapData.count
    }

// Returns a null terminated string name for a colormap given an index. Returns nullptr if index is invalid.
//const char* GetColormapName(ImPlotColormap colormap) {
//    ImPlotContext& gp = *GImPlot;
//    return gp.ColormapData.GetName(colormap);
//}

// Returns an index number for a colormap given a valid string name. Returns -1 if name is invalid.
fun getColormapIndex(name: String): PlotColormap {
    val gp = gImPlot
    return gp.colormapData getIndex name
}

// Temporarily switch to one of the built-in (i.e. ImPlotColormap_XXX) or user-added colormaps (i.e. a return value of AddColormap). Don't forget to call PopColormap!
fun pushColormap(colormap: PlotColormap) {
    val gp = gImPlot
    assert(colormap != PlotColormap.None) { "The colormap index is invalid!" }
    gp.colormapModifiers += gp.style.colormap
    gp.style.colormap = colormap
}

// Push a colormap by string name. Use built-in names such as "Default", "Deep", "Jet", etc. or a string you provided to AddColormap. Don't forget to call PopColormap!
fun pushColormap(name: String) {
    val gp = gImPlot
    val idx = gp.colormapData getIndex name
    assert(idx != PlotColormap.None) { "The colormap name is invalid!" }
    pushColormap(idx)
}

// Undo temporary colormap modification(s). Undo multiple pushes at once by increasing count.
fun popColormap(count_: Int = 1) {
    var count = count_
    val gp = gImPlot
    assert(count <= gp.colormapModifiers.size) { "You can't pop more modifiers than have been pushed!" }
    while (count > 0) {
        val backup = gp.colormapModifiers.last()
        gp.style.colormap = backup
        gp.colormapModifiers.pop()
        count--
    }
}

// Returns the next color from the current colormap and advances the colormap for the current plot.
// Can also be used with no return value to skip colors if desired. You need to call this between Begin/EndPlot!
fun nextColormapColor(): Vec4 = nextColormapColorU32().toInt().vec4

// Colormap utils. If cmap = IMPLOT_AUTO (default), the current colormap is assumed.
// Pass an explicit colormap index (built-in or user-added) to specify otherwise.

// Returns the size of a colormap.
fun getColormapSize(cmap_: PlotColormap = PlotColormap.None): Int {
    val gp = gImPlot
    val cmap = if (cmap_ == PlotColormap.None) gp.style.colormap else cmap_
    assert(cmap != PlotColormap.None) { "Invalid colormap index!" }
    return gp.colormapData getKeyCount cmap
}

// Returns a color from a colormap given an index >= 0 (modulo will be performed).
fun getColormapColor(idx: Int, cmap: PlotColormap = PlotColormap.None): Vec4 = getColormapColorU32(idx, cmap).toInt().vec4

// Sample a color from the current colormap given t between 0 and 1.
fun sampleColormap(t: Float, cmap: PlotColormap = PlotColormap.None): Vec4 = sampleColormapU32(t, cmap).toInt().vec4

// Shows a vertical color scale with linear spaced ticks using the specified color map. Use double hashes to hide label (e.g. "##NoLabel"). If scale_min > scale_max, the scale to color mapping will be reversed.
fun colormapScale(label: String, scaleMin: Double, scaleMax: Double, size: Vec2 = Vec2(), format: String = "%g", flags: PlotColormapScaleFlags = none, cmap_: PlotColormap = PlotColormap.None) {
    val g = gImGui
    val window = g.currentWindow!!
    if (window.skipItems)
        return

    val id = window.getID(label)
    val labelSize = Vec2()
    if (flags hasnt PlotColormapScaleFlag.NoLabel)
        labelSize put ImGui.calcTextSize(label, hideTextAfterDoubleHash = true)

    val gp = gImPlot
    val cmap = if (cmap_ == PlotColormap.None) gp.style.colormap else cmap_
    assert(cmap != PlotColormap.None) { "Invalid colormap index!" }

    val frameSize = ImGui.calcItemSize(size, 0f, gp.style.plotDefaultSize.y)
    if (frameSize.y < gp.style.plotMinSize.y && size.y < 0f)
        frameSize.y = gp.style.plotMinSize.y

    val range = PlotRange(scaleMin min scaleMax, scaleMin max scaleMax)
    gp.cTicker.reset()
    locator_Default(gp.cTicker, range, frameSize.y, true, ::formatter_Default, format)

    val rendLabel = labelSize.x > 0
    val txtOff = gp.style.labelPadding.x
    val pad = txtOff + gp.cTicker.maxSize.x + if (rendLabel) txtOff + labelSize.y else 0f
    var barW = 20f
    if (frameSize.x == 0f)
        frameSize.x = barW + pad + 2 * gp.style.plotPadding.x
    else {
        barW = frameSize.x - (pad + 2 * gp.style.plotPadding.x)
        if (barW < gp.style.majorTickLen.y)
            barW = gp.style.majorTickLen.y
    }

    val drawlist = window.drawList
    val bbFrame = Rect(window.dc.cursorPos, window.dc.cursorPos + frameSize)
    ImGui.itemSize(bbFrame)
    if (!ImGui.itemAdd(bbFrame, id, bbFrame))
        return

    ImGui.renderFrame(bbFrame.min, bbFrame.max, Col.FrameBg.u32, true, g.style.frameRounding)

    val opposite = flags has PlotColormapScaleFlag.Opposite
    val inverted = flags has PlotColormapScaleFlag.Invert
    val reversed = scaleMin > scaleMax

    val bbGradShift = if (opposite) pad else 0f
    val bbGrad = Rect(bbFrame.min + gp.style.plotPadding + Vec2(bbGradShift, 0),
                      bbFrame.min + Vec2(barW + gp.style.plotPadding.x + bbGradShift, frameSize.y - gp.style.plotPadding.y))

    ImGui.pushClipRect(bbFrame.min, bbFrame.max, true)
    val colText = Col.Text.u32.toUInt()

    val invertScale = if (inverted) !reversed else reversed
    val yMin = if (invertScale) bbGrad.max.y else bbGrad.min.y
    val yMax = if (invertScale) bbGrad.min.y else bbGrad.max.y

    renderColorBar(gp.colormapData getKeys cmap, drawlist, bbGrad, true, !inverted, !gp.colormapData.isQual(cmap))
    for (i in 0..<gp.cTicker.tickCount) {
        val yPosPlt = gp.cTicker.ticks[i].plotPos
        val yPos = remap(yPosPlt.f, range.max.f, range.min.f, yMin, yMax)
        val tickWidth = if (gp.cTicker.ticks[i].major) gp.style.majorTickLen.y else gp.style.minorTickLen.y
        val tickThick = if (gp.cTicker.ticks[i].major) gp.style.majorTickSize.y else gp.style.minorTickSize.y
        val tickT = ((yPosPlt - scaleMin) / (scaleMax - scaleMin)).f
        val tickCol = calcTextColor(gp.colormapData.lerpTable(cmap, tickT))
        if (yPos < bbGrad.max.y - 2 && yPos > bbGrad.min.y + 2) {
            drawlist.addLine(if (opposite) Vec2(bbGrad.min.x + 1, yPos) else Vec2(bbGrad.max.x - 1, yPos),
                             if (opposite) Vec2(bbGrad.min.x + tickWidth, yPos) else Vec2(bbGrad.max.x - tickWidth, yPos),
                             tickCol.toInt(), tickThick)
        }
        val txtX = if (opposite) bbGrad.min.x - txtOff - gp.cTicker.ticks[i].labelSize.x else bbGrad.max.x + txtOff
        val txtY = yPos - gp.cTicker.ticks[i].labelSize.y * 0.5f
        drawlist.addText(Vec2(txtX, txtY), colText.toInt(), gp.cTicker getText i)
    }

    if (rendLabel) {
        val posX = if (opposite) bbFrame.min.x + gp.style.plotPadding.x else bbGrad.max.x + 2 * txtOff + gp.cTicker.maxSize.x
        val posY = bbGrad.center.y + labelSize.x * 0.5f
        val labelEnd = ImGui.findRenderedTextEnd(label)
        addTextVertical(drawlist, Vec2(posX, posY), colText, label.toByteArray(), labelEnd)
    }
    drawlist.addRect(bbGrad.min, bbGrad.max, PlotCol.PlotBorder.u32.toInt())
    ImGui.popClipRect()
}

// Shows a horizontal slider with a colormap gradient background. Optionally returns the color sampled at t in [0 1].
fun colormapSlider(label: String, pT: KMutableProperty0<Float>, out: Vec4? = null, format: String, cmap_: PlotColormap = PlotColormap.None): Boolean {
    var t by pT
    t = clamp(t, 0f, 1f)
    val g = gImGui
    val window = g.currentWindow!!
    if (window.skipItems)
        return false
    val gp = gImPlot
    val cmap = if (cmap_ == PlotColormap.None) gp.style.colormap else cmap_
    assert(cmap != PlotColormap.None) { "Invalid colormap index!" }
    val keys = gp.colormapData getKeys cmap
    val count = gp.colormapData getKeyCount cmap
    val qual = gp.colormapData isQual cmap
    val pos = ImGui.currentWindow.dc.cursorPos
    val w = ImGui.calcItemWidth()
    val h = ImGui.frameHeight
    val rect = Rect(pos.x, pos.y, pos.x + w, pos.y + h)
    renderColorBar(keys, ImGui.windowDrawList, rect, false, false, !qual)
    val grab = calcTextColor(gp.colormapData.lerpTable(cmap, t))
    // const ImU32 text = CalcTextColor(gp.ColormapData.LerpTable(cmap,0.5f));
    ImGui.pushStyleColor(Col.FrameBg, COL32_BLACK_TRANS)
    ImGui.pushStyleColor(Col.FrameBgActive, COL32_BLACK_TRANS)
    ImGui.pushStyleColor(Col.FrameBgHovered, Vec4(1, 1, 1, 0.1f))
    ImGui.pushStyleColor(Col.SliderGrab, grab.toInt())
    ImGui.pushStyleColor(Col.SliderGrabActive, grab.toInt())
    ImGui.pushStyleVar(StyleVar.GrabMinSize, 2)
    ImGui.pushStyleVar(StyleVar.FrameRounding, 0)
    val changed = ImGui.slider(label, pT, 0f, 1f, format)
    ImGui.popStyleColor(5)
    ImGui.popStyleVar(2)
    out?.put(gp.colormapData.lerpTable(cmap, t).toInt().vec4)
    return changed
}

// Shows a button with a colormap gradient brackground.
fun colormapButton(label: String, sizeArg: Vec2 = Vec2(), cmap_: PlotColormap = PlotColormap.None): Boolean {
    val g = gImGui
    val style = g.style
    val window = g.currentWindow!!
    if (window.skipItems)
        return false
    val gp = gImPlot
    val cmap = if (cmap_ == PlotColormap.None) gp.style.colormap else cmap_
    assert(cmap != PlotColormap.None) { "Invalid colormap index!" }
    val keys = gp.colormapData getKeys cmap
    val count = gp.colormapData getKeyCount cmap
    val qual = gp.colormapData isQual cmap
    val pos = ImGui.currentWindow.dc.cursorPos
    val labelSize = ImGui.calcTextSize(label, hideTextAfterDoubleHash = true)
    val size = ImGui.calcItemSize(sizeArg, labelSize.x + style.framePadding.x * 2f, labelSize.y + style.framePadding.y * 2f)
    val rect = Rect(pos.x, pos.y, pos.x + size.x, pos.y + size.y)
    renderColorBar(keys, ImGui.windowDrawList, rect, false, false, !qual)
    val text = calcTextColor(gp.colormapData.lerpTable(cmap, g.style.buttonTextAlign.x))
    ImGui.pushStyleColor(Col.Button, COL32_BLACK_TRANS)
    ImGui.pushStyleColor(Col.ButtonHovered, Vec4(1, 1, 1, 0.1f))
    ImGui.pushStyleColor(Col.ButtonActive, Vec4(1, 1, 1, 0.2f))
    ImGui.pushStyleColor(Col.Text, text.toInt())
    ImGui.pushStyleVar(StyleVar.FrameRounding, 0)
    val pressed = ImGui.button(label, size)
    ImGui.popStyleColor(4)
    ImGui.popStyleVar(1)
    return pressed
}

// When items in a plot sample their color from a colormap, the color is cached and does not change
// unless explicitly overriden. Therefore, if you change the colormap after the item has already been plotted,
// item colors will NOT update. If you need item colors to resample the new colormap, then use this
// function to bust the cached colors. If #plot_title_id is nullptr, then every item in EVERY existing plot
// will be cache busted. Otherwise only the plot specified by #plot_title_id will be busted. For the
// latter, this function must be called in the same ImGui ID scope that the plot is in. You should rarely if ever
// need this function, but it is available for applications that require runtime colormap swaps (e.g. Heatmaps demo).
fun bustColorCache(plotTitleId: String? = null) {
    val gp = gImPlot
    if (plotTitleId == null)
        bustItemCache()
    else {
        val id = ImGui.currentWindow.getID(plotTitleId)
        val plot = gp.plots getByKey id
        if (plot != null)
            plot.items.reset()
        else {
            val subplot = gp.subplots getByKey id
            subplot?.items?.reset()
        }
    }
}