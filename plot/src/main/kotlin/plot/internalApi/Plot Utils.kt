@file:OptIn(ExperimentalStdlibApi::class)

package plot.internalApi

import imgui.ImGui
import imgui.api.gImGui
import imgui.has
import imgui.hasnt
import plot.api.*

//-----------------------------------------------------------------------------
// [SECTION] Plot Utils
//-----------------------------------------------------------------------------

// Gets a plot from the current ImPlotContext
fun getPlot(title: String): PlotPlot? {
    val window = gImGui.currentWindow!!
    val id = window.getID(title)
    return gImPlot.plots.getByKey(id)
}

// Gets the current plot from the current ImPlotContext
val currentPlot: PlotPlot?
    get() = gImPlot.currentPlot

// Busts the cache for every plot in the current context
fun bustPlotCache() {
    val gp = gImPlot
    gp.plots.clear()
    gp.subplots.clear()
}

// Shows a plot's context menu.
fun showPlotContextMenu(plot: PlotPlot) {
    val gp = gImPlot
    val ownsLegend = gp.currentItems === plot.items
    val equal = plot.flags has PlotFlag.Equal

//    char buf[16] = {}

    for (i in 0..<PLOT_NUM_X_AXES) {
        val xAxis = plot xAxis i
        if (!xAxis.enabled || !xAxis.hasMenus)
            continue
        ImGui.pushID(i)
        val buf = if (i == 0) "X-Axis" else "X-Axis ${i + 1}"
        if (ImGui.beginMenu(if (xAxis.hasLabel) plot getAxisLabel xAxis else buf)) {
            showAxisContextMenu(xAxis, if (equal) xAxis.orthoAxis!!() else null, false)
            ImGui.endMenu()
        }
        ImGui.popID()
    }

    for (i in 0..<PLOT_NUM_Y_AXES) {
        val yAxis = plot yAxis i
        if (!yAxis.enabled || !yAxis.hasMenus)
            continue
        ImGui.pushID(i)
        val buf = if (i == 0) "Y-Axis" else "Y-Axis ${i + 1}"
        if (ImGui.beginMenu(if (yAxis.hasLabel) plot getAxisLabel yAxis else buf)) {
            showAxisContextMenu(yAxis, if (equal) yAxis.orthoAxis!!() else null, false)
            ImGui.endMenu()
        }
        ImGui.popID()
    }

    ImGui.separator()
    if (gp.currentItems!!.legend.flags hasnt PlotLegendFlag.NoMenus) {
        if ((ImGui.beginMenu("Legend"))) {
            if (ownsLegend) {
                if (showLegendContextMenu(plot.items.legend, plot.flags hasnt PlotFlag.NoLegend))
                    plot::flags flip PlotFlag.NoLegend
            } else gp.currentSubplot?.let { subplot ->
                if (showLegendContextMenu(subplot.items.legend, subplot.flags hasnt PlotSubplotFlag.NoLegend))
                    subplot::flags flip PlotSubplotFlag.NoLegend
            }
            ImGui.endMenu()
        }
    }
    if ((ImGui.beginMenu("Settings"))) {
        if (ImGui.menuItem("Equal", "", plot.flags has PlotFlag.Equal))
            plot::flags flip PlotFlag.Equal
        if (ImGui.menuItem("Box Select", "", plot.flags hasnt PlotFlag.NoBoxSelect))
            plot::flags flip PlotFlag.NoBoxSelect
        beginDisabledControls(plot.titleOffset == -1)
        if (ImGui.menuItem("Title", "", plot.hasTitle))
            plot::flags flip PlotFlag.NoTitle
        endDisabledControls(plot.titleOffset == -1)
        if (ImGui.menuItem("Mouse Position", "", plot.flags hasnt PlotFlag.NoMouseText))
            plot::flags flip PlotFlag.NoMouseText
        if (ImGui.menuItem("Crosshairs", "", plot.flags has PlotFlag.Crosshairs))
            plot::flags flip PlotFlag.Crosshairs
        ImGui.endMenu()
    }
    gp.currentSubplot?.let { subplot ->
        if (subplot.flags hasnt PlotSubplotFlag.NoMenus) {
            ImGui.separator()
            if ((ImGui.beginMenu("Subplots"))) {
                showSubplotsContextMenu(subplot)
                ImGui.endMenu()
            }
        }
    }
}