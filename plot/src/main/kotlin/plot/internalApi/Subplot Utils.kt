package plot.internalApi

import imgui.ImGui
import imgui.has
import imgui.hasnt
import plot.api.PlotSubplotFlag
import plot.api.beginDisabledControls
import plot.api.endDisabledControls
import plot.api.subplotSetCell

//-----------------------------------------------------------------------------
// [SECTION] Subplot Utils
//-----------------------------------------------------------------------------

// Advances to next subplot
fun subplotNextCell() {
    val gp = gImPlot
    val subplot = gp.currentSubplot!!
    subplotSetCell(++subplot.currentIdx)
}


// Shows a subplot's context menu.
fun showSubplotsContextMenu(subplot: PlotSubplot) {
    if ((ImGui.beginMenu("Linking"))) {
        if (ImGui.menuItem("Link Rows", "", subplot.flags has PlotSubplotFlag.LinkRows))
            subplot::flags flip PlotSubplotFlag.LinkRows
        if (ImGui.menuItem("Link Cols", "", subplot.flags has PlotSubplotFlag.LinkCols))
            subplot::flags flip PlotSubplotFlag.LinkCols
        if (ImGui.menuItem("Link All X", "", subplot.flags has PlotSubplotFlag.LinkAllX))
            subplot::flags flip PlotSubplotFlag.LinkAllX
        if (ImGui.menuItem("Link All Y", "", subplot.flags has PlotSubplotFlag.LinkAllY))
            subplot::flags flip PlotSubplotFlag.LinkAllY
        ImGui.endMenu()
    }
    if ((ImGui.beginMenu("Settings"))) {
        beginDisabledControls(!subplot.hasTitle)
        if (ImGui.menuItem("Title", "",subplot.hasTitle && subplot.flags hasnt PlotSubplotFlag.NoTitle))
            subplot::flags flip PlotSubplotFlag.NoTitle
        endDisabledControls(!subplot.hasTitle)
        if (ImGui.menuItem("Resizable", "", subplot.flags hasnt PlotSubplotFlag.NoResize))
            subplot::flags flip PlotSubplotFlag.NoResize
        if (ImGui.menuItem("Align", "", subplot.flags hasnt PlotSubplotFlag.NoAlign))
            subplot::flags flip PlotSubplotFlag.NoAlign
        if (ImGui.menuItem("Share Items", "", subplot.flags has PlotSubplotFlag.ShareItems))
            subplot::flags flip PlotSubplotFlag.ShareItems
        ImGui.endMenu()
    }
}