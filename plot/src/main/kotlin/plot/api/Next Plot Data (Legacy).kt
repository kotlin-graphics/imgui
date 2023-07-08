package plot.api

import imgui.Cond
import plot.internalApi.gImPlot

//-----------------------------------------------------------------------------
// Next Plot Data (Legacy)
//-----------------------------------------------------------------------------

fun applyNextPlotData(idx: Axis) {
    val gp = gImPlot
    val plot = gp.currentPlot!!
    val axis = plot.axes[idx]
    if (!axis.enabled)
        return
    val npdLmin = gp.nextPlotData.linkedMin[idx]
    val npdLmax = gp.nextPlotData.linkedMax[idx]
    val npdRngh = gp.nextPlotData.hasRange[idx.i]
    val npdRngc = gp.nextPlotData.rangeCond[idx]
    val npdRngv = gp.nextPlotData.range[idx]
    axis.linkedMin = npdLmin
    axis.linkedMax = npdLmax
    axis.pullLinks()
    if (npdRngh) {
        if (!plot.initialized || npdRngc == Cond.Always)
            axis.setRange_(npdRngv)
    }
    axis.hasRange = npdRngh
    axis.rangeCond = npdRngc
}