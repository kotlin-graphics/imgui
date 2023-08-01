package demo

import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.Col
import imgui.Cond
import imgui.ImGui
import imgui.WindowFlag
import plot.api.PLOT_VERSION
import plot.api.showStyleEditor
import kotlin.reflect.KMutableProperty0

//-----------------------------------------------------------------------------
// [SECTION] Demo
//-----------------------------------------------------------------------------

object show {
    var implotMetrics = false
    var implotStyleEditor = false
    var imguiMetrics = false
    var imguiStyleEditor = false
    var imguiDemo = false
}

private var showWarning = false // by lazy { sizeof(ImDrawIdx) * 8 == 16 && (ImGui::GetIO().BackendFlags & ImGuiBackendFlags_RendererHasVtxOffset) == false }

inline fun demoHeader(label: String, demo: () -> Unit) {
    if (ImGui.treeNodeEx(label)) {
        demo()
        ImGui.treePop()
    }
}

// Shows the ImPlot demo window (add implot_demo.cpp to your sources!)
fun showDemoWindow(pOpen: KMutableProperty0<Boolean>? = null) {

    if (show.implotMetrics)
        MetricsWindow(show::implotMetrics)
    if (show.implotStyleEditor) {
        ImGui.setNextWindowSize(Vec2(415, 762), Cond.Appearing)
        ImGui.begin("Style Editor (ImPlot)", show::implotStyleEditor)
        showStyleEditor()
        ImGui.end()
    }
    if (show.imguiStyleEditor) {
        ImGui.begin("Style Editor (ImGui)", show::imguiStyleEditor)
        ImGui.showStyleEditor()
        ImGui.end()
    }
    if (show.imguiMetrics)
        ImGui.showMetricsWindow(show::imguiMetrics)
    if (show.imguiDemo)
        ImGui.showDemoWindow(show::imguiDemo)
    ImGui.setNextWindowPos(Vec2(50), Cond.FirstUseEver)
    ImGui.setNextWindowSize(Vec2(600, 750), Cond.FirstUseEver)
    ImGui.begin("ImPlot Demo", pOpen, WindowFlag.MenuBar)
    if (ImGui.beginMenuBar()) {
        if (ImGui.beginMenu("Tools")) {
            ImGui.menuItem("Metrics", "", show::implotMetrics)
            ImGui.menuItem("Style Editor", "", show::implotStyleEditor)
            ImGui.separator()
            ImGui.menuItem("ImGui Metrics", "", show::imguiMetrics)
            ImGui.menuItem("ImGui Style Editor", "", show::imguiStyleEditor)
            ImGui.menuItem("ImGui Demo", "", show::imguiDemo)
            ImGui.endMenu()
        }
        ImGui.endMenuBar()
    }
    //-------------------------------------------------------------------------
    ImGui.text("ImPlot says hello. ($PLOT_VERSION)")
    // display warning about 16-bit indices
    if (showWarning) {
        ImGui.pushStyleColor(Col.Text, Vec4(1, 1, 0, 1))
        ImGui.textWrapped("WARNING: ImDrawIdx is 16-bit and ImGuiBackendFlags_RendererHasVtxOffset is false. Expect visual glitches and artifacts! See README for more information.")
        ImGui.popStyleColor()
    }

    ImGui.spacing()

    if (ImGui.beginTabBar("ImPlotDemoTabs")) {
        if (ImGui.beginTabItem("Plots")) {
            demoHeader("Line Plots", LinePlots::invoke)
//            DemoHeader("Filled Line Plots", Demo_FilledLinePlots)
//            DemoHeader("Shaded Plots##", Demo_ShadedPlots)
//            DemoHeader("Scatter Plots", Demo_ScatterPlots)
//            DemoHeader("Realtime Plots", Demo_RealtimePlots)
//            DemoHeader("Stairstep Plots", Demo_StairstepPlots)
//            DemoHeader("Bar Plots", Demo_BarPlots)
//            DemoHeader("Bar Groups", Demo_BarGroups)
//            DemoHeader("Bar Stacks", Demo_BarStacks)
//            DemoHeader("Error Bars", Demo_ErrorBars)
//            DemoHeader("Stem Plots##", Demo_StemPlots)
//            DemoHeader("Infinite Lines", Demo_InfiniteLines)
//            DemoHeader("Pie Charts", Demo_PieCharts)
//            DemoHeader("Heatmaps", Demo_Heatmaps)
//            DemoHeader("Histogram", Demo_Histogram)
//            DemoHeader("Histogram 2D", Demo_Histogram2D)
//            DemoHeader("Digital Plots", Demo_DigitalPlots)
//            DemoHeader("Images", Demo_Images)
//            DemoHeader("Markers and Text", Demo_MarkersAndText)
//            DemoHeader("NaN Values", Demo_NaNValues)
            ImGui.endTabItem()
        }
//        if (ImGui::BeginTabItem("Subplots")) {
//            DemoHeader("Sizing", Demo_SubplotsSizing)
//            DemoHeader("Item Sharing", Demo_SubplotItemSharing)
//            DemoHeader("Axis Linking", Demo_SubplotAxisLinking)
//            DemoHeader("Tables", Demo_Tables)
//            ImGui::EndTabItem()
//        }
//        if (ImGui::BeginTabItem("Axes")) {
//            DemoHeader("Log Scale", Demo_LogScale)
//            DemoHeader("Symmetric Log Scale", Demo_SymmetricLogScale)
//            DemoHeader("Time Scale", Demo_TimeScale)
//            DemoHeader("Custom Scale", Demo_CustomScale)
//            DemoHeader("Multiple Axes", Demo_MultipleAxes)
//            DemoHeader("Tick Labels", Demo_TickLabels)
//            DemoHeader("Linked Axes", Demo_LinkedAxes)
//            DemoHeader("Axis Constraints", Demo_AxisConstraints)
//            DemoHeader("Equal Axes", Demo_EqualAxes)
//            DemoHeader("Auto-Fitting Data", Demo_AutoFittingData)
//            ImGui::EndTabItem()
//        }
//        if (ImGui::BeginTabItem("Tools")) {
//            DemoHeader("Offset and Stride", Demo_OffsetAndStride)
//            DemoHeader("Drag Points", Demo_DragPoints)
//            DemoHeader("Drag Lines", Demo_DragLines)
//            DemoHeader("Drag Rects", Demo_DragRects)
//            DemoHeader("Querying", Demo_Querying)
//            DemoHeader("Annotations", Demo_Annotations)
//            DemoHeader("Tags", Demo_Tags)
//            DemoHeader("Drag and Drop", Demo_DragAndDrop)
//            DemoHeader("Legend Options", Demo_LegendOptions)
//            DemoHeader("Legend Popups", Demo_LegendPopups)
//            DemoHeader("Colormap Widgets", Demo_ColormapWidgets)
//            ImGui::EndTabItem()
//        }
//        if (ImGui::BeginTabItem("Custom")) {
//            DemoHeader("Custom Styles", Demo_CustomStyles)
//            DemoHeader("Custom Data and Getters", Demo_CustomDataAndGetters)
//            DemoHeader("Custom Rendering", Demo_CustomRendering)
//            DemoHeader("Custom Plotters and Tooltips", Demo_CustomPlottersAndTooltips)
//            ImGui::EndTabItem()
//        }
//        if (ImGui::BeginTabItem("Config")) {
//            Demo_Config()
//            ImGui::EndTabItem()
//        }
//        if (ImGui::BeginTabItem("Help")) {
//            Demo_Help()
//            ImGui::EndTabItem()
//        }
        ImGui.endTabBar()
    }
    ImGui.end()
}
