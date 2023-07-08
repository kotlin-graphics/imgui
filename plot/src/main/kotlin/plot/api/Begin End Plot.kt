@file:OptIn(ExperimentalStdlibApi::class)

package plot.api

import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.api.gImGui
import imgui.internal.classes.Rect
import imgui.internal.lengthSqr
import imgui.internal.round
import imgui.internal.sections.DrawFlag
import plot.internalApi.*

//-----------------------------------------------------------------------------
// [SECTION] Begin/End Plot
//-----------------------------------------------------------------------------

// Starts a 2D plotting context. If this function returns true, EndPlot() MUST
// be called! You are encouraged to use the following convention:
//
// if (BeginPlot(...)) {
//     PlotLine(...);
//     ...
//     EndPlot();
// }
//
// Important notes:
//
// - #title_id must be unique to the current ImGui ID scope. If you need to avoid ID
//   collisions or don't want to display a title in the plot, use double hashes
//   (e.g. "MyPlot##HiddenIdText" or "##NoTitle").
// - #size is the **frame** size of the plot widget, not the plot area. The default
//   size of plots (i.e. when ImVec2(0,0)) can be modified in your ImPlotStyle.
fun beginPlot(titleId: String, size: Vec2 = Vec2(-1, 0), flags: PlotFlags = none): Boolean {
    assert(gImPlotNullable != null) { "No current context. Did you call ImPlot::CreateContext() or ImPlot::SetCurrentContext()?" }
    val gp = gImPlot
    assert(gp.currentPlot == null) { "Mismatched BeginPlot()/EndPlot()!" }

    // FRONT MATTER -----------------------------------------------------------

    if (gp.currentSubplot != null)
        ImGui.pushID(gp.currentSubplot!!.currentIdx)

    // get globals
    val g = gImGui
    var window = g.currentWindow!!

    // skip if needed
    if (window.skipItems && gp.currentSubplot == null) {
        gImPlot.resetCtxForNextPlot()
        return false
    }

    // ID and age (TODO: keep track of plot age in frames)
    val id = window.getID(titleId)
    val justCreated = gp.plots getByKey id == null
    gp.currentPlot = gp.plots getOrAddByKey id

    val plot = gp.currentPlot!!
    plot.id = id
    plot.items.id = id - 1
    plot.justCreated = justCreated
    plot.setupLocked = false

    // check flags
    if (plot.justCreated)
        plot.flags = flags
    else if (flags != plot.previousFlags)
        plot.flags = flags
    plot.previousFlags = flags

    // setup default axes
    if (plot.justCreated) {
        setupAxis(Axis.X1)
        setupAxis(Axis.Y1)
    }

    // reset axes
    for (i in 0..<Axis.COUNT) {
        plot.axes[i].reset()
        updateAxisColors(plot.axes[i])
    }
    // ensure first axes enabled
    plot.axes[Axis.X1].enabled = true
    plot.axes[Axis.Y1].enabled = true
    // set initial axes
    plot.currentX = Axis.X1
    plot.currentY = Axis.Y1

    // process next plot data (legacy)
    for (i in Axis.values())
        applyNextPlotData(i)

    // capture scroll with a child region
    if (plot.flags hasnt PlotFlag.NoChild) {
        val childSize = gp.currentSubplot?.cellSize?.let { Vec2(it) } ?: Vec2(if (size.x == 0f) gp.style.plotDefaultSize.x else size.x,
                                                                              if (size.y == 0f) gp.style.plotDefaultSize.y else size.y)
        ImGui.beginChild(titleId, childSize, false, WindowFlag.NoScrollbar)
        window = ImGui.currentWindow
        window.scrollMax.y = 1f
        gp.childWindowMade = true
    } else
        gp.childWindowMade = false

    // clear text buffers
    plot.clearTextBuffer()
    plot.title = titleId

    // set frame size
    val frameSize = gp.currentSubplot?.cellSize?.let { Vec2(it) } ?: ImGui.calcItemSize(size, gp.style.plotDefaultSize.x, gp.style.plotDefaultSize.y)

    if (frameSize.x < gp.style.plotMinSize.x && (size.x < 0f || gp.currentSubplot != null))
        frameSize.x = gp.style.plotMinSize.x
    if (frameSize.y < gp.style.plotMinSize.y && (size.y < 0f || gp.currentSubplot != null))
        frameSize.y = gp.style.plotMinSize.y

    plot.frameRect.put(window.dc.cursorPos, window.dc.cursorPos + frameSize)
    ImGui.itemSize(plot.frameRect)
    if (!ImGui.itemAdd(plot.frameRect, plot.id, plot.frameRect) && gp.currentSubplot == null) {
        gImPlot.resetCtxForNextPlot()
        return false
    }

    // setup items (or dont)
    if (gp.currentItems == null)
        gp.currentItems = plot.items

    return true
}

// Only call EndPlot() if BeginPlot() returns true! Typically called at the end
// of an if statement conditioned on BeginPlot(). See example above.
fun endPlot() {
    assert(gImPlotNullable != null) { "No current context. Did you call ImPlot::CreateContext() or ImPlot::SetCurrentContext()?" }
    val gp = gImPlot
    assert(gp.currentPlot != null) { "Mismatched BeginPlot()/EndPlot()!" }

    setupLock()

    val g = gImGui
    var plot = gp.currentPlot!!
    val window = g.currentWindow!!
    val drawlist = window.drawList
    val io = ImGui.io

    // FINAL RENDER -----------------------------------------------------------

    val renderBorder = gp.style.plotBorderSize > 0f && gp.style.colors[PlotCol.PlotBorder].w > 0f
    val anyXHeld = plot.held || anyAxesHeld(plot.axes, Axis.X1, PLOT_NUM_X_AXES)
    val anyYHeld = plot.held || anyAxesHeld(plot.axes, Axis.Y1, PLOT_NUM_Y_AXES)

    ImGui.pushClipRect(plot.frameRect.min, plot.frameRect.max, true)

    // render grid (foreground)
    for (i in 0..<PLOT_NUM_X_AXES) {
        val xAxis = plot xAxis i
        if (xAxis.enabled && xAxis.hasGridLines && xAxis.isForeground)
            renderGridLinesX(drawlist, xAxis.ticker, plot.plotRect, xAxis.colorMaj, xAxis.colorMin, gp.style.majorGridSize.x, gp.style.minorGridSize.x)
    }
    for (i in 0..<PLOT_NUM_Y_AXES) {
        val yAxis = plot yAxis i
        if (yAxis.enabled && yAxis.hasGridLines && yAxis.isForeground)
            renderGridLinesY(drawlist, yAxis.ticker, plot.plotRect, yAxis.colorMaj, yAxis.colorMin, gp.style.majorGridSize.y, gp.style.minorGridSize.y)
    }


    // render title
    if (plot.hasTitle) {
        val col = PlotCol.TitleText.u32
        addTextCentered(drawlist, Vec2(plot.plotRect.center.x, plot.canvasRect.min.y), col, plot.title!!.toByteArray())
    }

    // render x ticks
    var countB = 0
    var countT = 0
    for (i in 0..<PLOT_NUM_X_AXES) {
        val ax = plot xAxis i
        if (!ax.enabled)
            continue
        val tkr = ax.ticker
        val opp = ax.isOpposite
        val aux = (opp && countT > 0) || (!opp && countB > 0)
        if (ax.hasTickMarks) {
            val direction = if (opp) 1f else -1f
            for (j in 0..<tkr.tickCount) {
                val tk = tkr.ticks[j]
                if (tk.level != 0 || tk.pixelPos < plot.plotRect.min.x || tk.pixelPos > plot.plotRect.max.x)
                    continue
                val start = Vec2(tk.pixelPos, ax.datum1)
                val len = if (!aux && tk.major) gp.style.majorTickLen.x else gp.style.minorTickLen.x
                val thk = if (!aux && tk.major) gp.style.majorTickSize.x else gp.style.minorTickSize.x
                drawlist.addLine(start, start + Vec2(0, direction * len), ax.colorTick.toInt(), thk)
            }
            if (aux || !renderBorder)
                drawlist.addLine(Vec2(plot.plotRect.min.x, ax.datum1), Vec2(plot.plotRect.max.x, ax.datum1), ax.colorTick.toInt(), gp.style.minorTickSize.x)
        }
        countB += (!opp).i
        countT += opp.i
    }

    // render y ticks
    var countL = 0
    var countR = 0
    for (i in 0..<PLOT_NUM_Y_AXES) {
        val ax = plot yAxis i
        if (!ax.enabled)
            continue
        val tkr = ax.ticker
        val opp = ax.isOpposite
        val aux = (opp && countR > 0) || (!opp && countL > 0)
        if (ax.hasTickMarks) {
            val direction = if (opp) -1f else 1f
            for (j in 0..<tkr.tickCount) {
                val tk = tkr.ticks[j]
                if (tk.level != 0 || tk.pixelPos < plot.plotRect.min.y || tk.pixelPos > plot.plotRect.max.y)
                    continue
                val start = Vec2(ax.datum1, tk.pixelPos)
                val len = if (!aux && tk.major) gp.style.majorTickLen.y else gp.style.minorTickLen.y
                val thk = if (!aux && tk.major) gp.style.majorTickSize.y else gp.style.minorTickSize.y
                drawlist.addLine(start, start + Vec2(direction * len, 0), ax.colorTick.toInt(), thk)
            }
            if (aux || !renderBorder)
                drawlist.addLine(Vec2(ax.datum1, plot.plotRect.min.y), Vec2(ax.datum1, plot.plotRect.max.y), ax.colorTick.toInt(), gp.style.minorTickSize.y)
        }
        countL += (!opp).i
        countR += opp.i
    }
    ImGui.popClipRect()

    // render annotations
    pushPlotClipRect()
    for (i in 0..<gp.annotations.size) {
        val txt = gp.annotations getText i
        val an = gp.annotations.annotations[i]
        val txtSize = ImGui.calcTextSize(txt)
        val size = txtSize + gp.style.annotationPadding * 2
        val pos = Vec2(an.pos)
        if (an.offset.x == 0f)
            pos.x -= size.x / 2
        else if (an.offset.x > 0f)
            pos.x += an.offset.x
        else
            pos.x -= size.x - an.offset.x
        if (an.offset.y == 0f)
            pos.y -= size.y / 2
        else if (an.offset.y > 0f)
            pos.y += an.offset.y
        else
            pos.y -= size.y - an.offset.y
        if (an.clamp)
            pos put clampLabelPos(pos, size, plot.plotRect.min, plot.plotRect.max)
        val rect = Rect(pos, pos + size)
        if (an.offset.x != 0f || an.offset.y != 0f) {
            val corners = arrayOf(rect.tl, rect.tr, rect.br, rect.bl)
            var minCorner = 0
            var minLen = Float.MAX_VALUE
            for (c in 0..<4) {
                val len = (an.pos - corners[c]).lengthSqr
                if (len < minLen) {
                    minCorner = c
                    minLen = len
                }
            }
            drawlist.addLine(an.pos, corners[minCorner], an.colorBg.toInt())
        }
        drawlist.addRectFilled(rect.min, rect.max, an.colorBg.toInt())
        drawlist.addText(pos + gp.style.annotationPadding, an.colorFg.toInt(), txt)
    }

    // render selection
    if (plot.selected)
        renderSelectionRect(drawlist, plot.selectRect.min + plot.plotRect.min, plot.selectRect.max + plot.plotRect.min, PlotCol.Selection.vec4)

    // render crosshairs
    if (plot.flags has PlotFlag.Crosshairs && plot.hovered && !(anyXHeld || anyYHeld) && !plot.selecting && !plot.items.legend.hovered) {
        ImGui.mouseCursor = MouseCursor.None
        val xy = io.mousePos // [JVM] we can use the same instance
        val h1 = Vec2(plot.plotRect.min.x, xy.y)
        val h2 = Vec2(xy.x - 5, xy.y)
        val h3 = Vec2(xy.x + 5, xy.y)
        val h4 = Vec2(plot.plotRect.max.x, xy.y)
        val v1 = Vec2(xy.x, plot.plotRect.min.y)
        val v2 = Vec2(xy.x, xy.y - 5)
        val v3 = Vec2(xy.x, xy.y + 5)
        val v4 = Vec2(xy.x, plot.plotRect.max.y)
        val col = PlotCol.Crosshairs.u32.toInt()
        drawlist.addLine(h1, h2, col)
        drawlist.addLine(h3, h4, col)
        drawlist.addLine(v1, v2, col)
        drawlist.addLine(v3, v4, col)
    }

    // render mouse pos
    if (plot.flags hasnt PlotFlag.NoMouseText && (plot.hovered || plot.mouseTextFlags has PlotMouseTextFlag.ShowAlways)) {

        val noAux = plot.mouseTextFlags has PlotMouseTextFlag.NoAuxAxes
        val noFmt = plot.mouseTextFlags has PlotMouseTextFlag.NoFormat

        val builder = gp.mousePosStringBuilder
        builder.clear()

        val numX = if (noAux) 1 else PLOT_NUM_X_AXES
        for (i in 0..<numX) {
            val xAxis = plot xAxis i
            if (!xAxis.enabled)
                continue
            if (i > 0)
                builder += ", ("
            val v = xAxis pixelsToPlot io.mousePos.x
            builder += when {
                noFmt -> formatter_Default(v, PLOT_LABEL_FORMAT)
                else -> labelAxisValue(xAxis, v, true)
            }
            if (i > 0)
                builder += ")"
        }
        builder += ", "
        val numY = if (noAux) 1 else PLOT_NUM_Y_AXES
        for (i in 0..<numY) {
            val yAxis = plot yAxis i
            if (!yAxis.enabled)
                continue
            if (i > 0)
                builder += ", ("
            val v = yAxis pixelsToPlot io.mousePos.y
            builder += when {
                noFmt -> formatter_Default(v, PLOT_LABEL_FORMAT)
                else -> labelAxisValue(yAxis, v, true)
            }
            if (i > 0)
                builder += ")"
        }

        if (builder.isNotEmpty()) {
            val cStr = builder.joinToString(separator = "")
            val size = ImGui.calcTextSize(cStr)
            val pos = getLocationPos(plot.plotRect, size, plot.mouseTextLocation, gp.style.mousePosPadding)
            drawlist.addText(pos, PlotCol.InlayText.u32.toInt(), cStr)
        }
    }
    popPlotClipRect()

    // axis side switch
    if (!plot.held) {
        val mousePos = ImGui.io.mousePos // [JVM] we can use the same instance
        val triggerRect = plot.plotRect
        triggerRect.expand(-10f)
        for (i in 0..<PLOT_NUM_X_AXES) {
            val xAxis = plot xAxis i
            if (xAxis.flags has PlotAxisFlag.NoSideSwitch)
                continue
            if (xAxis.held && mousePos in plot.plotRect) {
                val opp = xAxis.flags has PlotAxisFlag.Opposite
                if (!opp) {
                    val rect = Rect(plot.plotRect.min.x - 5, plot.plotRect.min.y - 5, plot.plotRect.max.x + 5, plot.plotRect.min.y + 5)
                    if (mousePos.y < plot.plotRect.max.y - 10)
                        drawlist.addRectFilled(rect.min, rect.max, xAxis.colorHov.toInt())
                    if (mousePos in rect)
                        xAxis.flags /= PlotAxisFlag.Opposite
                } else {
                    val rect = Rect(plot.plotRect.min.x - 5, plot.plotRect.max.y - 5, plot.plotRect.max.x + 5, plot.plotRect.max.y + 5)
                    if (mousePos.y > plot.plotRect.min.y + 10)
                        drawlist.addRectFilled(rect.min, rect.max, xAxis.colorHov.toInt())
                    if (mousePos in rect)
                        xAxis.flags -= PlotAxisFlag.Opposite
                }
            }
        }
        for (i in 0..<PLOT_NUM_Y_AXES) {
            val yAxis = plot yAxis i
            if (yAxis.flags has PlotAxisFlag.NoSideSwitch)
                continue
            if (yAxis.held && mousePos in plot.plotRect) {
                val opp = yAxis.flags has PlotAxisFlag.Opposite
                if (!opp) {
                    val rect = Rect(plot.plotRect.max.x - 5, plot.plotRect.min.y - 5, plot.plotRect.max.x + 5, plot.plotRect.max.y + 5)
                    if (mousePos.x > plot.plotRect.min.x + 10)
                        drawlist.addRectFilled(rect.min, rect.max, yAxis.colorHov.toInt())
                    if (rect.contains(mousePos))
                        yAxis.flags /= PlotAxisFlag.Opposite
                } else {
                    val rect = Rect(plot.plotRect.min.x - 5, plot.plotRect.min.y - 5, plot.plotRect.min.x + 5, plot.plotRect.max.y + 5)
                    if (mousePos.x < plot.plotRect.max.x - 10)
                        drawlist.addRectFilled(rect.min, rect.max, yAxis.colorHov.toInt())
                    if (mousePos in rect)
                        yAxis.flags -= PlotAxisFlag.Opposite
                }
            }
        }
    }

    // reset legend hovers
    plot.items.legend.hovered = false
    for (i in 0..<plot.items.itemCount)
        plot.items.getItemByIndex(i).legendHovered = false
    // render legend
    if (plot.flags hasnt PlotFlag.NoLegend && plot.items.legendCount > 0) {
        val legend = plot.items.legend
        val legendOut = legend.flags has PlotLegendFlag.Outside
        val legendHorz = legend.flags has PlotLegendFlag.Horizontal
        val legendSize = calcLegendSize(plot.items, gp.style.legendInnerPadding, gp.style.legendSpacing, !legendHorz)
        val legendPos = getLocationPos(if (legendOut) plot.frameRect else plot.plotRect,
                                       legendSize,
                                       legend.location,
                                       if (legendOut) gp.style.plotPadding else gp.style.legendPadding)
        legend.rect.put(legendPos, legendPos + legendSize)
        // test hover
        legend.hovered = ImGui.isWindowHovered() && io.mousePos in legend.rect

        if (legendOut)
            ImGui.pushClipRect(plot.frameRect.min, plot.frameRect.max, true)
        else
            pushPlotClipRect()
        val colBg = PlotCol.LegendBg.u32.toInt()
        val colBd = PlotCol.LegendBorder.u32.toInt()
        drawlist.addRectFilled(legend.rect.min, legend.rect.max, colBg)
        drawlist.addRect(legend.rect.min, legend.rect.max, colBd)
        val legendContextable = showLegendEntries(plot.items, legend.rect, legend.hovered, gp.style.legendInnerPadding, gp.style.legendSpacing, !legendHorz, drawlist) && legend.flags hasnt PlotLegendFlag.NoMenus

        // main ctx menu
        if (gp.openContextThisFrame && legendContextable && plot.flags hasnt PlotFlag.NoMenus)
            ImGui.openPopup("##LegendContext")
        ImGui.popClipRect()
        if (ImGui.beginPopup("##LegendContext")) {
            ImGui.text("Legend"); ImGui.separator()
            if (showLegendContextMenu(legend, plot.flags hasnt PlotFlag.NoLegend))
                plot::flags flip PlotFlag.NoLegend
            ImGui.endPopup()
        }
    } else
        plot.items.legend.rect put Rect()

    // render border
    if (renderBorder)
        drawlist.addRect(plot.plotRect.min, plot.plotRect.max, PlotCol.PlotBorder.u32.toInt(), 0f, DrawFlag.RoundCornersAll, gp.style.plotBorderSize)

    // render tags
    for (i in 0..<gp.tags.size) {
        val tag = gp.tags.tags[i]
        val axis = plot.axes[tag.axis]
        if (!axis.enabled || tag.value !in axis.range)
            continue
        val txt = gp.tags getText i
        val textSize = ImGui.calcTextSize(txt)
        val size = textSize + gp.style.annotationPadding * 2
        val pos: Vec2
        axis.ticker overrideSizeLate size
        val pix = round(axis plotToPixels tag.value)
        if (axis.vertical)
            if (axis.isOpposite) {
                pos = Vec2(axis.datum1 + gp.style.labelPadding.x, pix - size.y * 0.5f)
                drawlist.addTriangleFilled(Vec2(axis.datum1, pix), pos, pos + Vec2(0, size.y), tag.colorBg.toInt())
            } else {
                pos = Vec2(axis.datum1 - size.x - gp.style.labelPadding.x, pix - size.y * 0.5f)
                drawlist.addTriangleFilled(pos + Vec2(size.x, 0), Vec2(axis.datum1, pix), pos + size, tag.colorBg.toInt())
            }
        else
            if (axis.isOpposite) {
                pos = Vec2(pix - size.x * 0.5f, axis.datum1 - size.y - gp.style.labelPadding.y)
                drawlist.addTriangleFilled(pos + Vec2(0, size.y), pos + size, Vec2(pix, axis.datum1), tag.colorBg.toInt())
            } else {
                pos = Vec2(pix - size.x * 0.5f, axis.datum1 + gp.style.labelPadding.y)
                drawlist.addTriangleFilled(pos, Vec2(pix, axis.datum1), pos + Vec2(size.x, 0), tag.colorBg.toInt())
            }
        drawlist.addRectFilled(pos, pos + size, tag.colorBg.toInt())
        drawlist.addText(pos + gp.style.annotationPadding, tag.colorFg.toInt(), txt)
    }

    // FIT DATA --------------------------------------------------------------
    val axisEqual = plot.flags has PlotFlag.Equal
    if (plot.fitThisFrame) {
        for (i in 0..<PLOT_NUM_X_AXES) {
            val xAxis = plot xAxis i
            if (xAxis.fitThisFrame) {
                xAxis applyFit gp.style.fitPadding.x
                if (axisEqual && xAxis.orthoAxis != null) {
                    var aspect = xAxis.aspect
                    val yAxis = xAxis.orthoAxis!!()
                    if (yAxis.fitThisFrame) {
                        yAxis applyFit gp.style.fitPadding.y
                        yAxis.fitThisFrame = false
                        aspect = aspect max yAxis.aspect
                    }
                    xAxis.setAspect(aspect)
                    yAxis.setAspect(aspect)
                }
            }
        }
        for (i in 0..<PLOT_NUM_Y_AXES) {
            val yAxis = plot yAxis i
            if (yAxis.fitThisFrame) {
                yAxis applyFit gp.style.fitPadding.y
                if (axisEqual && yAxis.orthoAxis != null) {
                    var aspect = yAxis.aspect
                    val xAxis = yAxis.orthoAxis!!()
                    if (xAxis.fitThisFrame) {
                        xAxis applyFit gp.style.fitPadding.x
                        xAxis.fitThisFrame = false
                        aspect = xAxis.aspect max aspect
                    }
                    xAxis.setAspect(aspect)
                    yAxis.setAspect(aspect)
                }
            }
        }
        plot.fitThisFrame = false
    }

    // CONTEXT MENUS -----------------------------------------------------------

    ImGui.pushOverrideID(plot.id)

    val canCtx = gp.openContextThisFrame && plot.flags hasnt PlotFlag.NoMenus && !plot.items.legend.hovered

    // main ctx menu
    if (canCtx && plot.hovered)
        ImGui.openPopup("##PlotContext")
    if (ImGui.beginPopup("##PlotContext")) {
        showPlotContextMenu(plot)
        ImGui.endPopup()
    }

    // axes ctx menus
    for (i in 0..<PLOT_NUM_X_AXES) {
        ImGui.pushID(i)
        val xAxis = plot.xAxis(i)
        if (canCtx && xAxis.hovered && xAxis.hasMenus)
            ImGui.openPopup("##XContext")
        if (ImGui.beginPopup("##XContext")) {
            ImGui.text(if (xAxis.hasLabel) plot getAxisLabel xAxis else if (i == 0) "X-Axis" else "X-Axis ${i + 1}")
            ImGui.separator()
            showAxisContextMenu(xAxis, if (axisEqual) xAxis.orthoAxis!!() else null, true)
            ImGui.endPopup()
        }
        ImGui.popID()
    }
    for (i in 0..<PLOT_NUM_Y_AXES) {
        ImGui.pushID(i)
        val yAxis = plot yAxis i
        if (canCtx && yAxis.hovered && yAxis.hasMenus)
            ImGui.openPopup("##YContext")
        if (ImGui.beginPopup("##YContext")) {
            ImGui.text(if (yAxis.hasLabel) plot getAxisLabel yAxis else if (i == 0) "Y-Axis" else "Y-Axis ${i + 1}")
            ImGui.separator()
            showAxisContextMenu(yAxis, if (axisEqual) yAxis.orthoAxis!!() else null, false)
            ImGui.endPopup()
        }
        ImGui.popID()
    }
    ImGui.popID()

    // LINKED AXES ------------------------------------------------------------

    for (i in 0..<Axis.COUNT)
        plot.axes[i].pushLinks()


    // CLEANUP ----------------------------------------------------------------

    // remove items
    if (gp.currentItems == plot.items)
        gp.currentItems = null
    // reset the plot items for the next frame
    for (i in 0 ..< plot.items.itemCount)
        plot.items.getItemByIndex(i).seenThisFrame = false

    // mark the plot as initialized, i.e. having made it through one frame completely
    plot.initialized = true
    // Pop ImGui::PushID at the end of BeginPlot
    ImGui.popID()
    // Reset context for next plot
    gImPlot.resetCtxForNextPlot()

    // setup next subplot
    if (gp.currentSubplot != null) {
        ImGui.popID()
        subplotNextCell()
    }
}