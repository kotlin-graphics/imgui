@file:OptIn(ExperimentalStdlibApi::class)

package plot.api

import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.api.gImGui
import imgui.classes.DrawList
import imgui.internal.classes.Rect
import imgui.internal.lengthSqr
import imgui.internal.round
import imgui.internal.sections.ButtonFlag
import plot.internalApi.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.reflect.KMutableProperty0

//-----------------------------------------------------------------------------
// [SECTION] Setup
//-----------------------------------------------------------------------------

// The following API allows you to setup and customize various aspects of the
// current plot. The functions should be called immediately after BeginPlot
// and before any other API calls. Typical usage is as follows:

// if (BeginPlot(...)) {                     1) begin a new plot
//     SetupAxis(ImAxis_X1, "My X-Axis");    2) make Setup calls
//     SetupAxis(ImAxis_Y1, "My Y-Axis");
//     SetupLegend(ImPlotLocation_North);
//     ...
//     SetupFinish();                        3) [optional] explicitly finish setup
//     PlotLine(...);                        4) plot items
//     ...
//     EndPlot();                            5) end the plot
// }
//
// Important notes:
//
// - Always call Setup code at the top of your BeginPlot conditional statement.
// - Setup is locked once you start plotting or explicitly call SetupFinish.
//   Do NOT call Setup code after you begin plotting or after you make
//   any non-Setup API calls (e.g. utils like PlotToPixels also lock Setup)
// - Calling SetupFinish is OPTIONAL, but probably good practice. If you do not
//   call it yourself, then the first subsequent plotting or utility function will
//   call it for you.

// Enables an axis or sets the label and/or flags for an existing axis. Leave #label = nullptr for no label.
fun setupAxis(idx: Axis, label: String? = null, flags: PlotAxisFlags = none) {
    val gp = gImPlot
    assert(gp.currentPlot != null && !gp.currentPlot!!.setupLocked) { "Setup needs to be called after BeginPlot and before any setup locking functions (e.g. PlotX)!" }
    // get plot and axis
    val plot = gp.currentPlot!!
    val axis = plot.axes[idx]
    // set ID
    axis.id = plot.id + idx.i + 1
    // check and set flags
    if (plot.justCreated || flags != axis.previousFlags)
        axis.flags = flags
    axis.previousFlags = flags
    // enable axis
    axis.enabled = true
    // set label
    plot.setAxisLabel(axis, label)
    // cache colors
    updateAxisColors(axis)
}

fun updateAxisColors(axis: PlotAxis) {
    val colGrid = PlotCol.AxisGrid.vec4
    axis.colorMaj = colGrid.u32.toUInt()
    axis.colorMin = ImGui.getColorU32(colGrid * Vec4(1, 1, 1, gImPlot.style.minorAlpha)).toUInt()
    axis.colorTick = PlotCol.AxisTick.u32
    axis.colorTxt = PlotCol.AxisText.u32
    axis.colorBg = PlotCol.AxisBg.u32
    axis.colorHov = PlotCol.AxisBgHovered.u32
    axis.colorAct = PlotCol.AxisBgActive.u32
    // axis.ColorHiLi     = IM_COL32_BLACK_TRANS;
}

// Sets an axis range limits. If ImPlotCond_Always is used, the axes limits will be locked.
fun setupAxisLimits(idx: Axis, minLim: Double, maxLim: Double, cond: PlotCond = Cond.Once) {
    val gp = gImPlot
    assert(gp.currentPlot != null && !gp.currentPlot!!.setupLocked) { "Setup needs to be called after BeginPlot and before any setup locking functions (e.g. PlotX)!" }    // get plot and axis
    val plot = gp.currentPlot!!
    val axis = plot.axes[idx]
    assert(axis.enabled) { "Axis is not enabled! Did you forget to call SetupAxis()?" }
    if (!plot.initialized || cond == Cond.Always)
        axis.setRange(minLim, maxLim)
    axis.hasRange = true
    axis.rangeCond = cond
}

// Links an axis range limits to external values. Set to nullptr for no linkage. The pointer data must remain valid until EndPlot.
fun setupAxisLinks(idx: Axis, minLnk: KMutableProperty0<Double>, maxLnk: KMutableProperty0<Double>) {
    val gp = gImPlot
    assert(gp.currentPlot != null && !gp.currentPlot!!.setupLocked) { "Setup needs to be called after BeginPlot and before any setup locking functions (e.g. PlotX)!" }
    val plot = gp.currentPlot!!
    val axis = plot.axes[idx]
    assert(axis.enabled) { "Axis is not enabled! Did you forget to call SetupAxis()?" }
    axis.linkedMin = minLnk
    axis.linkedMax = maxLnk
    axis.pullLinks()
}

// Sets the format of numeric axis labels via formater specifier (default="%g"). Formated values will be double (i.e. use %f).
fun setupAxisFormat(idx: Axis, fmt: String?) {
    val gp = gImPlot
    assert(gp.currentPlot != null && !gp.currentPlot!!.setupLocked) { "Setup needs to be called after BeginPlot and before any setup locking functions (e.g. PlotX)!" }
    val plot = gp.currentPlot!!
    val axis = plot.axes[idx]
    assert(axis.enabled) { "Axis is not enabled! Did you forget to call SetupAxis()?" }
    axis.hasFormatSpec = fmt != null
    if (fmt != null)
        axis.formatSpec = fmt
}

// Sets the format of numeric axis labels via formatter callback. Given #value, write a label into #buff. Optionally pass user data.
fun setupAxisFormat(idx: Axis, formatter: PlotFormatter, data: Any? = null) {
    val gp = gImPlot
    assert(gp.currentPlot != null && !gp.currentPlot!!.setupLocked) { "Setup needs to be called after BeginPlot and before any setup locking functions (e.g. PlotX)!" }
    val plot = gp.currentPlot!!
    val axis = plot.axes[idx]
    assert(axis.enabled) { "Axis is not enabled! Did you forget to call SetupAxis()?" }
    axis.formatter = formatter
    axis.formatterData = data
}

// Sets an axis' ticks and optionally the labels. To keep the default ticks, set #keep_default=true.
fun setupAxisTicks(idx: Axis, values: DoubleArray, labels: Array<String>? = null, showDefault: Boolean = false) {
    val gp = gImPlot
    assert(gp.currentPlot != null && !gp.currentPlot!!.setupLocked) { "Setup needs to be called after BeginPlot and before any setup locking functions (e.g. PlotX)!" }
    val plot = gp.currentPlot!!
    val axis = plot.axes[idx]
    assert(axis.enabled) { "Axis is not enabled! Did you forget to call SetupAxis()?" }
    axis.showDefaultTicks = showDefault
    addTicksCustom(values, labels, axis.ticker, axis.formatter ?: ::formatter_Default,
                   if (axis.formatter != null && axis.formatterData != null) axis.formatterData!! else if (axis.hasFormatSpec) axis.formatSpec else PLOT_LABEL_FORMAT)
}

fun addTicksCustom(values: DoubleArray, labels: Array<String>?, ticker: PlotTicker, formatter: PlotFormatter, data: Any?) {
    for (i in values.indices) {
        if (labels != null)
            ticker.addTick(values[i], false, 0, true, labels[i])
        else
            ticker.addTick(values[i], false, 0, true, formatter, data)
    }
}

// Sets an axis' ticks and optionally the labels for the next plot. To keep the default ticks, set #keep_default=true.
fun setupAxisTicks(idx: Axis, vMin: Double, vMax: Double, nTicks_: Int, labels: Array<String>? = null, showDefault: Boolean = false) {
    val gp = gImPlot
    assert(gp.currentPlot != null && !gp.currentPlot!!.setupLocked) { "Setup needs to be called after BeginPlot and before any setup locking functions (e.g. PlotX)!" }
    val nTicks = if (nTicks_ < 2) 2 else nTicks_
    gp.tempDouble1 = DoubleArray(nTicks_)
    gp.tempDouble1.fillRange(vMin, vMax)
    setupAxisTicks(idx, gp.tempDouble1, labels, showDefault)
}

// Sets an axis' scale using built-in options.
fun setupAxisScale(idx: Axis, scale: PlotScale) {
    val gp = gImPlot
    assert(gp.currentPlot != null && !gp.currentPlot!!.setupLocked) { "Setup needs to be called after BeginPlot and before any setup locking functions (e.g. PlotX)!" }
    val plot = gp.currentPlot!!
    val axis = plot.axes[idx]
    assert(axis.enabled) { "Axis is not enabled! Did you forget to call SetupAxis()?" }
    axis.scale = scale
    when (scale) {
        PlotScale.Time -> {
            axis.transformForward = null
            axis.transformInverse = null
            axis.transformData = null
            axis.locator = ::locator_Time
            axis.constraintRange = PlotRange(PLOT_MIN_TIME, PLOT_MAX_TIME)
            axis.ticker.levels = 2
        }
        PlotScale.Log10 -> {
            axis.transformForward = ::transformForward_Log10
            axis.transformInverse = ::transformInverse_Log10
            axis.transformData = null
            axis.locator = ::locator_Log10
            axis.constraintRange = PlotRange(Double.MIN_VALUE, Double.POSITIVE_INFINITY)
        }
        PlotScale.SymLog -> {
            axis.transformForward = ::transformForward_SymLog
            axis.transformInverse = ::transformInverse_SymLog
            axis.transformData = null
            axis.locator = ::locator_SymLog
            axis.constraintRange = PlotRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
        }
        else -> {
            axis.transformForward = null
            axis.transformInverse = null
            axis.transformData = null
            axis.locator = null
            axis.constraintRange = PlotRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
        }
    }
}

// Sets an axis' scale using user supplied forward and inverse transfroms.
fun setupAxisScale(idx: Axis, fwd: PlotTransform, inv: PlotTransform, data: Any? = null) {
    val gp = gImPlot
    assert(gp.currentPlot != null && !gp.currentPlot!!.setupLocked) { "Setup needs to be called after BeginPlot and before any setup locking functions (e.g. PlotX)!" }
    val plot = gp.currentPlot!!
    val axis = plot.axes[idx]
    assert(axis.enabled) { "Axis is not enabled! Did you forget to call SetupAxis()?" }
    axis.scale = PlotScale.None
    axis.transformForward = fwd
    axis.transformInverse = inv
    axis.transformData = data
}

// Sets an axis' limits constraints.
fun setupAxisLimitsConstraints(idx: Axis, vMin: Double, vMax: Double) {
    val gp = gImPlot
    assert(gp.currentPlot != null && !gp.currentPlot!!.setupLocked) { "Setup needs to be called after BeginPlot and before any setup locking functions (e.g. PlotX)!" }
    val plot = gp.currentPlot!!
    val axis = plot.axes[idx]
    assert(axis.enabled) { "Axis is not enabled! Did you forget to call SetupAxis()?" }
    axis.constraintRange.min = vMin
    axis.constraintRange.max = vMax
}

// Sets an axis' zoom constraints.
fun setupAxisZoomConstraints(idx: Axis, zMin: Double, zMax: Double) {
    val gp = gImPlot
    assert(gp.currentPlot != null && !gp.currentPlot!!.setupLocked) { "Setup needs to be called after BeginPlot and before any setup locking functions (e.g. PlotX)!" }
    val plot = gp.currentPlot!!
    val axis = plot.axes[idx]
    assert(axis.enabled) { "Axis is not enabled! Did you forget to call SetupAxis()?" }
    axis.constraintZoom.min = zMin
    axis.constraintZoom.max = zMax
}

// Sets the label and/or flags for primary X and Y axes (shorthand for two calls to SetupAxis).
fun setupAxes(xLabel: String, yLabel: String, xFlags: PlotAxisFlags = none, yFlags: PlotAxisFlags = none) {
    setupAxis(Axis.X1, xLabel, xFlags)
    setupAxis(Axis.Y1, yLabel, yFlags)
}

// Sets the primary X and Y axes range limits. If ImPlotCond_Always is used, the axes limits will be locked (shorthand for two calls to SetupAxisLimits).
fun setupAxesLimits(xMin: Double, xMax: Double, yMin: Double, yMax: Double, cond: PlotCond = Cond.Once) {
    setupAxisLimits(Axis.X1, xMin, xMax, cond)
    setupAxisLimits(Axis.Y1, yMin, yMax, cond)
}

// Sets up the plot legend.
fun setupLegend(location: PlotLocation, flags: PlotLegendFlags = none) {
    val gp = gImPlot
    assert(gp.currentPlot != null && !gp.currentPlot!!.setupLocked) { "Setup needs to be called after BeginPlot and before any setup locking functions (e.g. PlotX)!" }
    assert(gp.currentItems != null) { "SetupLegend() needs to be called within an itemized context!" }
    val legend = gp.currentItems!!.legend
    // check and set location
    if (location != legend.previousLocation)
        legend.location = location
    legend.previousLocation = location
    // check and set flags
    if (flags != legend.previousFlags)
        legend.flags = flags
    legend.previousFlags = flags
}

// Set the location of the current plot's mouse position text (default = South|East).
fun setupMouseText(location: PlotLocation, flags: PlotMouseTextFlags = none) {
    val gp = gImPlot
    assert(gp.currentPlot != null && !gp.currentPlot!!.setupLocked) { "Setup needs to be called after BeginPlot and before any setup locking functions (e.g. PlotX)!" }
    gp.currentPlot!!.mouseTextLocation = location
    gp.currentPlot!!.mouseTextFlags = flags
}

// Explicitly finalize plot setup. Once you call this, you cannot make anymore Setup calls for the current plot!
// Note that calling this function is OPTIONAL; it will be called by the first subsequent setup-locking API call.
fun setupFinish() {
    assert(gImPlotNullable != null) { "No current context. Did you call ImPlot::CreateContext() or ImPlot::SetCurrentContext()?" }
    val gp = gImPlot
    assert(gp.currentPlot != null) { "SetupFinish needs to be called after BeginPlot!" }

    val g = gImGui
    val drawlist = g.currentWindow!!.drawList
    val style = g.style

    val plot = gp.currentPlot!!

    // lock setup
    plot.setupLocked = true

    // finalize axes and set default formatter/locator
    for (i in 0..<Axis.COUNT) {
        val axis = plot.axes[i]
        if (axis.enabled) {
            axis.constrain()
            if (!plot.initialized && axis.canInitFit) {
                plot.fitThisFrame = true; axis.fitThisFrame = true
            }
        }
        if (axis.formatter == null) {
            axis.formatter = ::formatter_Default
            axis.formatterData = when {
                axis.hasFormatSpec -> axis.formatSpec
                else -> PLOT_LABEL_FORMAT
            }
        }
        if (axis.locator == null)
            axis.locator = ::locator_Default
    }

    // setup nullptr orthogonal axes
    val axisEqual = plot.flags has PlotFlag.Equal
    var iX = Axis.X1
    var iY = Axis.Y1
    while (iX < Axis.Y1 || iY != Axis.Y3) {
        var xAxis = plot.axes[iX]
        var yAxis = plot.axes[iY]
        if (xAxis.enabled && yAxis.enabled) {
            if (xAxis.orthoAxis == null)
                xAxis.orthoAxis = plot.axes mutablePropertyAt iY.i
            if (yAxis.orthoAxis == null)
                yAxis.orthoAxis = plot.axes mutablePropertyAt iX.i
        } else if (xAxis.enabled) {
            if (xAxis.orthoAxis == null && !axisEqual)
                xAxis.orthoAxis = plot.axes mutablePropertyAt Axis.Y1.i
        } else if (yAxis.enabled) {
            if (yAxis.orthoAxis == null && !axisEqual)
                yAxis.orthoAxis = plot.axes mutablePropertyAt Axis.X1.i
        }
    }

    // canvas/axes bb
    plot.canvasRect.put(plot.frameRect.min + gp.style.plotPadding, plot.frameRect.max - gp.style.plotPadding)
    plot.axesRect put plot.frameRect

    // outside legend adjustments
    if (plot.flags hasnt PlotFlag.NoLegend && plot.items.legendCount > 0 && plot.items.legend.flags has PlotLegendFlag.Outside) {
        val legend = plot.items.legend
        val horz = legend.flags has PlotLegendFlag.Horizontal
        val legendSize = calcLegendSize(plot.items, gp.style.legendInnerPadding, gp.style.legendSpacing, !horz)
        val west = legend.location has PlotLocation.West && legend.location hasnt PlotLocation.East
        val east = legend.location has PlotLocation.East && legend.location hasnt PlotLocation.West
        val north = legend.location has PlotLocation.North && legend.location hasnt PlotLocation.South
        val south = legend.location has PlotLocation.South && legend.location hasnt PlotLocation.North
        if ((west && !horz) || (west && horz && !north && !south)) {
            plot.canvasRect.min.x += (legendSize.x + gp.style.legendPadding.x)
            plot.axesRect.min.x += (legendSize.x + gp.style.plotPadding.x)
        }
        if ((east && !horz) || (east && horz && !north && !south)) {
            plot.canvasRect.max.x -= (legendSize.x + gp.style.legendPadding.x)
            plot.axesRect.max.x -= (legendSize.x + gp.style.plotPadding.x)
        }
        if ((north && horz) || (north && !horz && !west && !east)) {
            plot.canvasRect.min.y += (legendSize.y + gp.style.legendPadding.y)
            plot.axesRect.min.y += (legendSize.y + gp.style.plotPadding.y)
        }
        if ((south && horz) || (south && !horz && !west && !east)) {
            plot.canvasRect.max.y -= (legendSize.y + gp.style.legendPadding.y)
            plot.axesRect.max.y -= (legendSize.y + gp.style.plotPadding.y)
        }
    }

    // plot bb
    var padTop = 0f
    var padBot = 0f
    var padLeft = 0f
    var padRight = 0f

    // (0) calc top padding form title
    val titleSize = Vec2()
    if (plot.hasTitle)
        titleSize put ImGui.calcTextSize(plot.title!!, hideTextAfterDoubleHash = true)
    if (titleSize.x > 0) {
        padTop += titleSize.y + gp.style.labelPadding.y
        plot.axesRect.min.y += gp.style.plotPadding.y + padTop
    }

    // (1) calc addition top padding and bot padding
    padAndDatumAxesX(plot, padTop.mutableReference, padBot.mutableReference, gp.currentAlignmentH)

    val plotHeight = plot.canvasRect.height - padTop - padBot

    // (2) get y tick labels (needed for left/right pad)
    for (i in 0..<PLOT_NUM_Y_AXES) {
        val axis = plot.yAxis(i)
        if (axis.willRender && axis.showDefaultTicks)
            axis.locator!!(axis.ticker, axis.range, plotHeight, true, axis.formatter!!, axis.formatterData)
    }

    // (3) calc left/right pad
    padAndDatumAxesY(plot, padLeft.mutableReference, padRight.mutableReference, gp.currentAlignmentV)

    val plotWidth = plot.canvasRect.width - padLeft - padRight

    // (4) get x ticks
    for (i in 0..<PLOT_NUM_X_AXES) {
        val axis = plot.xAxis(i)
        if (axis.willRender && axis.showDefaultTicks) {
            axis.locator!!(axis.ticker, axis.range, plotWidth, false, axis.formatter!!, axis.formatterData)
        }
    }

    // (5) calc plot bb
    plot.plotRect.put(plot.canvasRect.min + Vec2(padLeft, padTop), plot.canvasRect.max - Vec2(padRight, padBot))

    // HOVER------------------------------------------------------------

    // axes hover rect, pixel ranges
    for (i in 0..<PLOT_NUM_X_AXES) {
        val xAx = plot.xAxis(i)
        xAx.hoverRect.put(plot.plotRect.min.x, xAx.datum1 min xAx.datum2, plot.plotRect.max.x, xAx.datum1 max xAx.datum2)
        xAx.pixelMin = if (xAx.isInverted) plot.plotRect.max.x else plot.plotRect.min.x
        xAx.pixelMax = if (xAx.isInverted) plot.plotRect.min.x else plot.plotRect.max.x
        xAx.updateTransformCache()
    }

    for (i in 0..<PLOT_NUM_Y_AXES) {
        val yAx = plot.yAxis(i)
        yAx.hoverRect.put(yAx.datum1 min yAx.datum2, plot.plotRect.min.y, yAx.datum1 max yAx.datum2, plot.plotRect.max.y)
        yAx.pixelMin = if (yAx.isInverted) plot.plotRect.min.y else plot.plotRect.max.y
        yAx.pixelMax = if (yAx.isInverted) plot.plotRect.max.y else plot.plotRect.min.y
        yAx.updateTransformCache()
    }
    // Equal axis constraint. Must happen after we set Pixels
    // constrain equal axes for primary x and y if not approximately equal
    // constrains x to y since x pixel size depends on y labels width, and causes feedback loops in opposite case
    if (axisEqual) {
        for (i in 0..<PLOT_NUM_X_AXES) {
            val xAxis = plot.xAxis(i)
            val orthoAxis = xAxis.orthoAxis?.get() ?: continue
            val xAr = xAxis.aspect
            val yAr = orthoAxis.aspect
            // edge case: user has set x range this frame, so fit y to x so that we honor their request for x range
            // NB: because of feedback across several frames, the user's x request may not be perfectly honored
            if (xAxis.hasRange)
                orthoAxis.setAspect(xAr)
            else if (!almostEqual(xAr, yAr) && !orthoAxis.isInputLocked)
                xAxis.setAspect(yAr)
        }
    }

    // INPUT ------------------------------------------------------------------
    if (plot.flags hasnt PlotFlag.NoInputs)
        updateInput(plot)

    // fit from FitNextPlotAxes or auto fit
    for (i in 0..<Axis.COUNT) {
        if (gp.nextPlotData.fit[i] || plot.axes[i].isAutoFitting) {
            plot.fitThisFrame = true
            plot.axes[i].fitThisFrame = true
        }
    }

    // RENDER -----------------------------------------------------------------

    val txtHeight = ImGui.textLineHeight

    // render frame
    if (plot.flags hasnt PlotFlag.NoFrame)
        ImGui.renderFrame(plot.frameRect.min, plot.frameRect.max, PlotCol.FrameBg.u32.toInt(), true, style.frameRounding)

    // grid bg
    drawlist.addRectFilled(plot.plotRect.min, plot.plotRect.max, PlotCol.PlotBg.u32.toInt())

    // transform ticks
    for (i in 0..<Axis.COUNT) {
        val axis = plot.axes[i]
        if (axis.willRender) {
            for (t in 0..<axis.ticker.tickCount) {
                val tk = axis.ticker.ticks[t]
                tk.pixelPos = round(axis.plotToPixels(tk.plotPos))
            }
        }
    }

    // render grid (background)
    for (i in 0..<PLOT_NUM_X_AXES) {
        val xAxis = plot.xAxis(i)
        if (xAxis.enabled && xAxis.hasGridLines && !xAxis.isForeground)
            renderGridLinesX(drawlist, xAxis.ticker, plot.plotRect, xAxis.colorMaj, xAxis.colorMin, gp.style.majorGridSize.x, gp.style.minorGridSize.x)
    }
    for (i in 0..<PLOT_NUM_Y_AXES) {
        val yAxis = plot.yAxis(i)
        if (yAxis.enabled && yAxis.hasGridLines && !yAxis.isForeground)
            renderGridLinesY(drawlist, yAxis.ticker, plot.plotRect, yAxis.colorMaj, yAxis.colorMin, gp.style.majorGridSize.y, gp.style.minorGridSize.y)
    }

    // render x axis button, label, tick labels
    for (i in 0..<PLOT_NUM_X_AXES) {
        val ax = plot.xAxis(i)
        if (!ax.enabled)
            continue
        if ((ax.hovered || ax.held) && !plot.held && ax.flags hasnt PlotAxisFlag.NoHighlight)
            drawlist.addRectFilled(ax.hoverRect.min, ax.hoverRect.max, (if (ax.held) ax.colorAct else ax.colorHov).toInt())
        else if (ax.colorHiLi.toInt() != COL32_BLACK_TRANS) {
            drawlist.addRectFilled(ax.hoverRect.min, ax.hoverRect.max, ax.colorHiLi.toInt())
            ax.colorHiLi = COL32_BLACK_TRANS.toUInt()
        } else if (ax.colorBg.toInt() != COL32_BLACK_TRANS)
            drawlist.addRectFilled(ax.hoverRect.min, ax.hoverRect.max, ax.colorBg.toInt())
        val tkr = ax.ticker
        val opp = ax.isOpposite
        if (ax.hasLabel) {
            val label = plot getAxisLabel ax
            val labelSize = ImGui.calcTextSize(label)
            val labelOffset = (if (ax.hasTickLabels) tkr.maxSize.y + gp.style.labelPadding.y else 0f) +
                    (tkr.levels - 1) * (txtHeight + gp.style.labelPadding.y) +
                    gp.style.labelPadding.y
            val labelPos = Vec2(plot.plotRect.center.x - labelSize.x * 0.5f, if (opp) ax.datum1 - labelOffset - labelSize.y else ax.datum1 + labelOffset)
            drawlist.addText(labelPos, ax.colorTxt.toInt(), label)
        }
        if (ax.hasTickLabels) {
            for (j in 0..<tkr.tickCount) {
                val tk = tkr.ticks[j]
                val datum = ax.datum1 + when {
                    opp -> -gp.style.labelPadding.y - txtHeight - tk.level * (txtHeight + gp.style.labelPadding.y)
                    else -> gp.style.labelPadding.y + tk.level * (txtHeight + gp.style.labelPadding.y)
                }
                if (tk.showLabel && tk.pixelPos >= plot.plotRect.min.x - 1 && tk.pixelPos <= plot.plotRect.max.x + 1) {
                    val start = Vec2(tk.pixelPos - 0.5f * tk.labelSize.x, datum)
                    drawlist.addText(start, ax.colorTxt.toInt(), tkr getText j)
                }
            }
        }
    }

    // render y axis button, label, tick labels
    for (i in 0..<PLOT_NUM_Y_AXES) {
        val ax = plot.yAxis(i)
        if (!ax.enabled)
            continue
        if ((ax.hovered || ax.held) && !plot.held && ax.flags hasnt PlotAxisFlag.NoHighlight)
            drawlist.addRectFilled(ax.hoverRect.min, ax.hoverRect.max, (if (ax.held) ax.colorAct else ax.colorHov).toInt())
        else if (ax.colorHiLi.toInt() != COL32_BLACK_TRANS) {
            drawlist.addRectFilled(ax.hoverRect.min, ax.hoverRect.max, ax.colorHiLi.toInt())
            ax.colorHiLi = COL32_BLACK_TRANS.toUInt()
        } else if (ax.colorBg.toInt() != COL32_BLACK_TRANS)
            drawlist.addRectFilled(ax.hoverRect.min, ax.hoverRect.max, ax.colorBg.toInt())
        val tkr = ax.ticker
        val opp = ax.isOpposite
        if (ax.hasLabel) {
            val label = plot getAxisLabel ax
            val labelSize = calcTextSizeVertical(label)
            val labelOffset = (if (ax.hasTickLabels) tkr.maxSize.x + gp.style.labelPadding.x else 0f) + gp.style.labelPadding.x
            val labelPos = Vec2(if (opp) ax.datum1 + labelOffset else ax.datum1 - labelOffset - labelSize.x,
                                plot.plotRect.center.y + labelSize.y * 0.5f)
            addTextVertical(drawlist, labelPos, ax.colorTxt, label.toByteArray())
        }
        if (ax.hasTickLabels) {
            for (j in 0..<tkr.tickCount) {
                val tk = tkr.ticks[j]
                val datum = ax.datum1 + if (opp) gp.style.labelPadding.x else -gp.style.labelPadding.x - tk.labelSize.x
                if (tk.showLabel && tk.pixelPos >= plot.plotRect.min.y - 1 && tk.pixelPos <= plot.plotRect.max.y + 1) {
                    val start = Vec2(datum, tk.pixelPos-0.5f * tk.labelSize.y)
                    drawlist.addText(start, ax.colorTxt.toInt(), tkr getText j)
                }
            }
        }
    }


    // clear legend (TODO: put elsewhere)
    plot.items.legend.reset()
    // push ID to set item hashes (NB: !!!THIS PROBABLY NEEDS TO BE IN BEGIN PLOT!!!!)
    ImGui.pushOverrideID(gp.currentItems!!.id)
}

fun padAndDatumAxesX(plot: PlotPlot, refPadT: KMutableProperty0<Float>, refPadB: KMutableProperty0<Float>, align: PlotAlignmentData?) {

    var padT by refPadT
    var padB by refPadB
    val gp = gImPlot

    val t = ImGui.textLineHeight
    val p = gp.style.labelPadding.y
    val k = gp.style.minorTickLen.x

    var countT = 0
    var countB = 0
    var lastT = plot.axesRect.min.y
    var lastB = plot.axesRect.max.y

    var i = PLOT_NUM_X_AXES
    while (i-- > 0) { // FYI: can iterate forward
        val axis = plot.xAxis(i)
        if (!axis.enabled)
            continue
        val label = axis.hasLabel
        val ticks = axis.hasTickLabels
        val opp = axis.isOpposite
        val time = axis.scale == PlotScale.Time
        if (opp) {
            if (countT++ > 0)
                padT += k + p
            if (label)
                padT += t + p
            if (ticks)
                padT += max(t, axis.ticker.maxSize.y) + p + if (time) t + p else 0f
            axis.datum1 = plot.canvasRect.min.y + padT
            axis.datum2 = lastT
            lastT = axis.datum1
        } else {
            if (countB++ > 0)
                padB += k + p
            if (label)
                padB += t + p
            if (ticks)
                padB += max(t, axis.ticker.maxSize.y) + p + if (time) t + p else 0f
            axis.datum1 = plot.canvasRect.max.y - padB
            axis.datum2 = lastB
            lastB = axis.datum1
        }
    }

    if (align != null) {
        countT = 0; countB = 0
        var deltaT = 0f
        var deltaB = 0f
        align.update(refPadT, refPadB, deltaT.mutableReference, deltaB.mutableReference)
        i = PLOT_NUM_X_AXES
        while (i-- > 0) {
            val axis = plot.xAxis(i)
            if (!axis.enabled)
                continue
            if (axis.isOpposite) {
                axis.datum1 += deltaT
                axis.datum2 += if (countT++ > 1) deltaT else 0f
            } else {
                axis.datum1 -= deltaB
                axis.datum2 -= if (countB++ > 1) deltaB else 0f
            }
        }
    }
}

fun padAndDatumAxesY(plot: PlotPlot, refPadL: KMutableProperty0<Float>, refPadR: KMutableProperty0<Float>, align: PlotAlignmentData?) {

    var padL by refPadL
    var padR by refPadR

    //   [   pad_L   ]                 [   pad_R   ]
    //   .................CanvasRect................
    //   :TPWPK.PTPWP _____PlotRect____ PWPTP.KPWPT:
    //   :A # |- A # |-               -| # A -| # A:
    //   :X   |  X   |                 |   X  |   x:
    //   :I # |- I # |-               -| # I -| # I:
    //   :S   |  S   |                 |   S  |   S:
    //   :3 # |- 0 # |-_______________-| # 1 -| # 2:
    //   :.........................................:
    //
    //   T = text height
    //   P = label padding
    //   K = minor tick length
    //   W = label width

    val gp = gImPlot

    val t = ImGui.textLineHeight
    val p = gp.style.labelPadding.x
    val k = gp.style.minorTickLen.y

    var countL = 0
    var countR = 0
    var lastL = plot.axesRect.min.x
    var lastR = plot.axesRect.max.x

    var i = PLOT_NUM_Y_AXES
    while (i-- > 0) { // FYI: can iterate forward
        val axis = plot.yAxis(i)
        if (!axis.enabled)
            continue
        val label = axis.hasLabel
        val ticks = axis.hasTickLabels
        val opp = axis.isOpposite
        if (opp) {
            if (countR++ > 0)
                padR += k + p
            if (label)
                padR += t + p
            if (ticks)
                padR += axis.ticker.maxSize.x + p
            axis.datum1 = plot.canvasRect.max.x - padR
            axis.datum2 = lastR
            lastR = axis.datum1
        } else {
            if (countL++ > 0)
                padL += k + p
            if (label)
                padL += t + p
            if (ticks)
                padL += axis.ticker.maxSize.x + p
            axis.datum1 = plot.canvasRect.min.x + padL
            axis.datum2 = lastL
            lastL = axis.datum1
        }
    }

    plot.plotRect.min.x = plot.canvasRect.min.x + padL
    plot.plotRect.max.x = plot.canvasRect.max.x - padR

    if (align != null) {
        countL = 0; countR = 0
        var deltaL = 0f
        var deltaR = 0f
        align.update(refPadL, refPadR, deltaL.mutableReference, deltaR.mutableReference)
        i = PLOT_NUM_Y_AXES
        while (i-- > 0) {
            val axis = plot.yAxis(i)
            if (!axis.enabled)
                continue
            if (axis.isOpposite) {
                axis.datum1 -= deltaR
                axis.datum2 -= if (countR++ > 1) deltaR else 0f
            } else {
                axis.datum1 += deltaL
                axis.datum2 += if (countL++ > 1) deltaL else 0f
            }
        }
    }
}

private const val MOUSE_CURSOR_DRAG_THRESHOLD = 5f
private const val BOX_SELECT_DRAG_THRESHOLD = 4f

fun updateInput(plot: PlotPlot): Boolean {

    var changed = false

    val gp = gImPlot
    val io = ImGui.io

    // BUTTON STATE -----------------------------------------------------------

    val plotButtonFlags = ButtonFlag.AllowItemOverlap / ButtonFlag.PressedOnClick / ButtonFlag.PressedOnDoubleClick / ButtonFlag.MouseButtonLeft / ButtonFlag.MouseButtonRight / ButtonFlag.MouseButtonMiddle
    val axisButtonFlags = ButtonFlag.FlattenChildren / plotButtonFlags

    val (plotClicked, hovered, held) = ImGui.buttonBehavior(plot.plotRect, plot.id, plotButtonFlags)
    plot.hovered = hovered
    plot.held = held
    ImGui.setItemAllowOverlap()

    if (plotClicked) {
        if (plot.flags hasnt PlotFlag.NoBoxSelect && io.mouseClicked[gp.inputMap.select.i] && io.keyMods has gp.inputMap.selectMod) {
            plot.selecting = true
            plot.selectStart put io.mousePos
            plot.selectRect.put(0f, 0f, 0f, 0f)
        }
        if (io.mouseDoubleClicked[gp.inputMap.fit.i]) {
            plot.fitThisFrame = true
            for (i in 0..<Axis.COUNT)
                plot.axes[i].fitThisFrame = true
        }
    }

    val canPan = io.mouseDown[gp.inputMap.pan.i] && io.keyMods has gp.inputMap.panMod

    plot.held = plot.held && canPan

    val xClick = BooleanArray(PLOT_NUM_X_AXES)
    val xHeld = BooleanArray(PLOT_NUM_X_AXES)
    val xHov = BooleanArray(PLOT_NUM_X_AXES)

    val yClick = BooleanArray(PLOT_NUM_Y_AXES)
    val yHeld = BooleanArray(PLOT_NUM_Y_AXES)
    val yHov = BooleanArray(PLOT_NUM_Y_AXES)

    for (i in 0..<PLOT_NUM_X_AXES) {
        val xAx = plot.xAxis(i)
        if (xAx.enabled) {
            ImGui.keepAliveID(xAx.id)
            val (pressed, hovered, held) = ImGui.buttonBehavior(xAx.hoverRect, xAx.id, axisButtonFlags)
            xClick[i] = pressed; xAx.hovered = hovered; xAx.held = held
            if (xClick[i] && io.mouseDoubleClicked[gp.inputMap.fit.i]) {
                plot.fitThisFrame = true; xAx.fitThisFrame = true
            }
            xAx.held = xAx.held && canPan
            xHov[i] = xAx.hovered || plot.hovered
            xHeld[i] = xAx.held || plot.held
        }
    }

    for (i in 0..<PLOT_NUM_Y_AXES) {
        val yAx = plot.yAxis(i)
        if (yAx.enabled) {
            ImGui.keepAliveID(yAx.id)
            val (pressed, hovered, held) = ImGui.buttonBehavior(yAx.hoverRect, yAx.id, axisButtonFlags)
            yClick[i] = pressed; yAx.hovered = hovered; yAx.held = held
            if (yClick[i] && io.mouseDoubleClicked[gp.inputMap.fit.i]) {
                plot.fitThisFrame = true; yAx.fitThisFrame = true
            }
            yAx.held = yAx.held && canPan
            yHov[i] = yAx.hovered || plot.hovered
            yHeld[i] = yAx.held || plot.held
        }
    }

    // cancel due to DND activity
    if (gImGui.dragDropActive || (io.keyMods == gp.inputMap.overrideMod && gp.inputMap.overrideMod != none))
        return false

    // STATE -------------------------------------------------------------------

    val axisEqual = plot.flags has PlotFlag.Equal

    val anyXHov = plot.hovered || anyAxesHovered(plot.axes, Axis.X1, PLOT_NUM_X_AXES)
    val anyXHeld = plot.held || anyAxesHeld(plot.axes, Axis.X1, PLOT_NUM_X_AXES)
    val anyYHov = plot.hovered || anyAxesHovered(plot.axes, Axis.Y1, PLOT_NUM_Y_AXES)
    val anyYHeld = plot.held || anyAxesHeld(plot.axes, Axis.Y1, PLOT_NUM_Y_AXES)
    val anyHov = anyXHov || anyYHov
    val anyHeld = anyXHeld || anyYHeld

    val selectDrag = ImGui.getMouseDragDelta(gp.inputMap.select) // [JVM] safe same instance
    val panDrag = ImGui.getMouseDragDelta(gp.inputMap.pan)
    val selectDragSq = selectDrag.lengthSqr
    val panDragSq = panDrag.lengthSqr
    val selecting = plot.selecting && selectDragSq > MOUSE_CURSOR_DRAG_THRESHOLD
    val panning = anyHeld && panDragSq > MOUSE_CURSOR_DRAG_THRESHOLD

    // CONTEXT MENU -----------------------------------------------------------

    if (io.mouseReleased[gp.inputMap.menu.i] && !plot.contextLocked)
        gp.openContextThisFrame = true

    if (selecting || panning)
        plot.contextLocked = true
    else if (!(io.mouseDown[gp.inputMap.menu.i] || io.mouseReleased[gp.inputMap.menu.i]))
        plot.contextLocked = false

    // DRAG INPUT -------------------------------------------------------------

    if (anyHeld && !plot.selecting) {
        var dragDirection = 0
        for (i in 0..<PLOT_NUM_X_AXES) {
            val xAxis = plot.xAxis(i)
            if (xHeld[i] && !xAxis.isInputLocked) {
                dragDirection = dragDirection or (1 shl 1)
                val increasing = if (xAxis.isInverted) io.mouseDelta.x > 0f else io.mouseDelta.x < 0f
                if (io.mouseDelta.x != 0f && !xAxis.isPanLocked(increasing)) {
                    val plotL = xAxis pixelsToPlot (plot.plotRect.min.x - io.mouseDelta.x)
                    val plotR = xAxis pixelsToPlot (plot.plotRect.max.x - io.mouseDelta.x)
                    xAxis.setMin(if (xAxis.isInverted) plotR else plotL)
                    xAxis.setMax(if (xAxis.isInverted) plotL else plotR)
                    if (axisEqual && xAxis.orthoAxis != null)
                        xAxis.orthoAxis!!().setAspect(xAxis.aspect)
                    changed = true
                }
            }
        }
        for (i in 0..<PLOT_NUM_Y_AXES) {
            val yAxis = plot.yAxis(i)
            if (yHeld[i] && !yAxis.isInputLocked) {
                dragDirection = dragDirection or (1 shl 2)
                val increasing = if (yAxis.isInverted) io.mouseDelta.y < 0f else io.mouseDelta.y > 0f
                if (io.mouseDelta.y != 0f && !yAxis.isPanLocked(increasing)) {
                    val plotT = yAxis pixelsToPlot (plot.plotRect.min.y - io.mouseDelta.y)
                    val plotB = yAxis pixelsToPlot (plot.plotRect.max.y - io.mouseDelta.y)
                    yAxis.setMin(if (yAxis.isInverted) plotT else plotB)
                    yAxis.setMax(if (yAxis.isInverted) plotB else plotT)
                    if (axisEqual && yAxis.orthoAxis != null)
                        yAxis.orthoAxis!!().setAspect(yAxis.aspect)
                    changed = true
                }
            }
        }
        if (io.mouseDragMaxDistanceSqr[gp.inputMap.pan.i] > MOUSE_CURSOR_DRAG_THRESHOLD)
            when (dragDirection) {
                0 -> ImGui.mouseCursor = MouseCursor.NotAllowed
                1 shl 1 -> ImGui.mouseCursor = MouseCursor.ResizeEW
                1 shl 2 -> ImGui.mouseCursor = MouseCursor.ResizeNS
                else -> ImGui.mouseCursor = MouseCursor.ResizeAll
            }
    }

    // SCROLL INPUT -----------------------------------------------------------

    if (anyHov && io.mouseWheel != 0f && io.keyMods has gp.inputMap.zoomMod) {

        var zoomRate = gp.inputMap.zoomRate
        if (io.mouseWheel > 0f)
            zoomRate = (-zoomRate) / (1f + 2f * zoomRate)
        val rectSize = plot.plotRect.size
        val tX = remap(io.mousePos.x, plot.plotRect.min.x, plot.plotRect.max.x, 0f, 1f)
        val tY = remap(io.mousePos.y, plot.plotRect.min.y, plot.plotRect.max.y, 0f, 1f)

        for (i in 0..<PLOT_NUM_X_AXES) {
            val xAxis = plot.xAxis(i)
            val equalZoom = axisEqual && xAxis.orthoAxis != null
            val equalLocked = equalZoom && xAxis.orthoAxis!!().isInputLocked
            if (xHov[i] && !xAxis.isInputLocked && !equalLocked) {
                val correction = if (plot.hovered && equalZoom) 0.5f else 1f
                val plotL = xAxis pixelsToPlot (plot.plotRect.min.x - rectSize.x * tX * zoomRate * correction)
                val plotR = xAxis pixelsToPlot (plot.plotRect.max.x + rectSize.x * (1 - tX) * zoomRate * correction)
                xAxis.setMin(if (xAxis.isInverted) plotR else plotL)
                xAxis.setMax(if (xAxis.isInverted) plotL else plotR)
                if (axisEqual && xAxis.orthoAxis != null)
                    xAxis.orthoAxis!!().setAspect(xAxis.aspect)
                changed = true
            }
        }
        for (i in 0..<PLOT_NUM_Y_AXES) {
            val yAxis = plot.yAxis(i)
            val equalZoom = axisEqual && yAxis.orthoAxis != null
            val equalLocked = equalZoom && yAxis.orthoAxis!!().isInputLocked
            if (yHov[i] && !yAxis.isInputLocked && !equalLocked) {
                val correction = if (plot.hovered && equalZoom) 0.5f else 1f
                val plotT = yAxis pixelsToPlot (plot.plotRect.min.y - rectSize.y * tY * zoomRate * correction)
                val plotB = yAxis pixelsToPlot (plot.plotRect.max.y + rectSize.y * (1 - tY) * zoomRate * correction)
                yAxis.setMin(if (yAxis.isInverted) plotT else plotB)
                yAxis.setMax(if (yAxis.isInverted) plotB else plotT)
                if (axisEqual && yAxis.orthoAxis != null)
                    yAxis.orthoAxis!!().setAspect(yAxis.aspect)
                changed = true
            }
        }
    }

    // BOX-SELECTION ----------------------------------------------------------

    if (plot.selecting) {
        val d = plot.selectStart - io.mousePos
        val xCanChange = io.keyMods hasnt gp.inputMap.selectHorzMod && abs(d.x) > 2
        val yCanChange = io.keyMods hasnt gp.inputMap.selectVertMod && abs(d.y) > 2
        // confirm
        if (io.mouseReleased[gp.inputMap.select.i]) {
            for (i in 0..<PLOT_NUM_X_AXES) {
                val xAxis = plot.xAxis(i)
                if (!xAxis.isInputLocked && xCanChange) {
                    val p1 = xAxis pixelsToPlot plot.selectStart.x
                    val p2 = xAxis pixelsToPlot io.mousePos.x
                    xAxis.setMin(p1 min p2)
                    xAxis.setMax(p1 max p2)
                    changed = true
                }
            }
            for (i in 0..<PLOT_NUM_Y_AXES) {
                val yAxis = plot.yAxis(i)
                if (!yAxis.isInputLocked && yCanChange) {
                    val p1 = yAxis pixelsToPlot plot.selectStart.y
                    val p2 = yAxis pixelsToPlot io.mousePos.y
                    yAxis.setMin(p1 min p2)
                    yAxis.setMax(p1 max p2)
                    changed = true
                }
            }
            if (xCanChange || yCanChange || (io.keyMods has gp.inputMap.selectHorzMod && io.keyMods has gp.inputMap.selectVertMod))
                gp.openContextThisFrame = false
            plot.selected = false; plot.selecting = false
        }
        // cancel
        else if (io.mouseReleased[gp.inputMap.selectCancel.i]) {
            plot.selected = false; plot.selecting = false
            gp.openContextThisFrame = false
        } else if (d.lengthSqr > BOX_SELECT_DRAG_THRESHOLD) {
            // bad selection
            if (plot.isInputLocked) {
                ImGui.mouseCursor = MouseCursor.NotAllowed
                gp.openContextThisFrame = false
                plot.selected = false
            } else {
                // TODO: Handle only min or max locked cases
                val fullWidth = io.keyMods has gp.inputMap.selectHorzMod || allAxesInputLocked(plot.axes, Axis.X1, PLOT_NUM_X_AXES)
                val fullHeight = io.keyMods has gp.inputMap.selectVertMod || allAxesInputLocked(plot.axes, Axis.Y1, PLOT_NUM_Y_AXES)
                plot.selectRect.min.x = if (fullWidth) plot.plotRect.min.x else plot.selectStart.x min io.mousePos.x
                plot.selectRect.max.x = if (fullWidth) plot.plotRect.max.x else plot.selectStart.x max io.mousePos.x
                plot.selectRect.min.y = if (fullHeight) plot.plotRect.min.y else plot.selectStart.y min io.mousePos.y
                plot.selectRect.max.y = if (fullHeight) plot.plotRect.max.y else plot.selectStart.y max io.mousePos.y
                plot.selectRect.min minusAssign plot.plotRect.min
                plot.selectRect.max minusAssign plot.plotRect.min
                plot.selected = true
            }
        } else
            plot.selected = false
    }
    return changed
}

fun renderGridLinesX(drawList: DrawList, ticker: PlotTicker, rect: Rect, colMaj: UInt, colMin_: UInt, sizeMaj: Float, sizeMin: Float) {
    var colMin = colMin_
    val density = ticker.tickCount / rect.width
    val colMin4 = colMin.toInt().vec4
    colMin4.w *= clamp(remap(density, 0.1f, 0.2f, 1f, 0f), 0f, 1f)
    colMin = colMin4.u32.toUInt()
    for (t in 0..<ticker.tickCount) {
        val xt = ticker.ticks[t]
        if (xt.pixelPos < rect.min.x || xt.pixelPos > rect.max.x)
            continue
        if (xt.level == 0) {
            if (xt.major)
                drawList.addLine(Vec2(xt.pixelPos, rect.min.y), Vec2(xt.pixelPos, rect.max.y), colMaj.toInt(), sizeMaj)
            else if (density < 0.2f)
                drawList.addLine(Vec2(xt.pixelPos, rect.min.y), Vec2(xt.pixelPos, rect.max.y), colMin.toInt(), sizeMin)
        }
    }
}

fun renderGridLinesY(drawList: DrawList, ticker: PlotTicker, rect: Rect, colMaj: UInt, colMin_: UInt, sizeMaj: Float, sizeMin: Float) {
    var colMin = colMin_
    val density = ticker.tickCount / rect.height
    val colMin4 = colMin.toInt().vec4
    colMin4.w *= clamp(remap(density, 0.1f, 0.2f, 1f, 0f), 0f, 1f)
    colMin = colMin4.u32.toUInt()
    for (t in 0..<ticker.tickCount) {
        val yt = ticker.ticks[t]
        if (yt.pixelPos < rect.min.y || yt.pixelPos > rect.max.y)
            continue
        if (yt.major)
            drawList.addLine(Vec2(rect.min.x, yt.pixelPos), Vec2(rect.max.x, yt.pixelPos), colMaj.toInt(), sizeMaj)
        else if (density < 0.2f)
            drawList.addLine(Vec2(rect.min.x, yt.pixelPos), Vec2(rect.max.x, yt.pixelPos), colMin.toInt(), sizeMin)
    }
}

fun renderSelectionRect(drawList: DrawList, pMin: Vec2, pMax: Vec2, col: Vec4) {
    val colBg = ImGui.getColorU32(col * Vec4(1, 1, 1, 0.25f))
    val colBd = col.u32
    drawList.addRectFilled(pMin, pMax, colBg)
    drawList.addRect(pMin, pMax, colBd)
}