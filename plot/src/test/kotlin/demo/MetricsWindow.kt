@file:OptIn(ExperimentalStdlibApi::class)

package demo

import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import plot.api.*
import plot.internalApi.*
import kotlin.reflect.KMutableProperty0

object MetricsWindow {

    object show {
        var plotRects = false
        var axesRects = false
        var axisRects = false
        var canvasRects = false
        var frameRects = false
        var subplotFrameRects = false
        var subplotGridRects = false
    }

    var t = 0.5f

    operator fun invoke(pPopen: KMutableProperty0<Boolean>?) {

        val fg = ImGui.foregroundDrawList

        var gp = gImPlot
        // ImGuiContext& g = *GImGui;
        var io = ImGui.io
        ImGui.begin("ImPlot Metrics", pPopen)
        ImGui.text("ImPlot " + PLOT_VERSION)
        ImGui.text("Application average %.3f ms/frame (%.1f FPS)", 1_000f / io.framerate, io.framerate)
        ImGui.text("Mouse Position: [%.0f,%.0f]", io.mousePos.x, io.mousePos.y)
        ImGui.separator()
        if (ImGui.treeNode("Tools")) {
            if (ImGui.button("Bust Plot Cache"))
                bustPlotCache()
            ImGui.sameLine()
            if (ImGui.button("Bust Item Cache"))
                bustItemCache()
            ImGui.checkbox("Show Frame Rects", show::frameRects)
            ImGui.checkbox("Show Canvas Rects", show::canvasRects)
            ImGui.checkbox("Show Plot Rects", show::plotRects)
            ImGui.checkbox("Show Axes Rects", show::axesRects)
            ImGui.checkbox("Show Axis Rects", show::axisRects)
            ImGui.checkbox("Show Subplot Frame Rects", show::subplotFrameRects)
            ImGui.checkbox("Show Subplot Grid Rects", show::subplotGridRects)
            ImGui.treePop()
        }
        val nPlots = gp.plots.bufSize
        val nSubplots = gp.subplots.bufSize
        // render rects
        for (p in 0..<nPlots) {
            var plot = gp.plots getByIndex p
            if (show.frameRects)
                fg.addRect(plot.frameRect.min, plot.frameRect.max, COL32(255, 0, 255, 255))
            if (show.canvasRects)
                fg.addRect(plot.canvasRect.min, plot.canvasRect.max, COL32(0, 255, 255, 255))
            if (show.plotRects)
                fg.addRect(plot.plotRect.min, plot.plotRect.max, COL32(255, 255, 0, 255))
            if (show.axesRects)
                fg.addRect(plot.axesRect.min, plot.axesRect.max, COL32(0, 255, 128, 255))
            if (show.axisRects)
                for (i in 0..<Axis.COUNT) {
                    if (plot.axes[i].enabled)
                        fg.addRect(plot.axes[i].hoverRect.min, plot.axes[i].hoverRect.max, COL32(0, 255, 0, 255))
                }
        }
        for (p in 0..<nSubplots) {
            val subplot = gp.subplots getByIndex p
            if (show.subplotFrameRects)
                fg.addRect(subplot.frameRect.min, subplot.frameRect.max, COL32(255, 0, 0, 255))
            if (show.subplotGridRects)
                fg.addRect(subplot.gridRect.min, subplot.gridRect.max, COL32(0, 0, 255, 255))
        }
        if (ImGui.treeNode("Plots", "Plots ($nPlots)")) {
            for (p in 0..<nPlots) {
                // plot
                val plot = gp.plots getByIndex p
                ImGui.pushID(p)
                if (ImGui.treeNode("Plot", "Plot [0x%08X]", plot.id)) {
                    val nItems = plot.items.itemCount
                    if (ImGui.treeNode("Items", "Items ($nItems)")) {
                        for (i in 0..<nItems) {
                            val item = plot.items getItemByIndex i
                            ImGui.pushID(i)
                            if (ImGui.treeNode("Item", "Item [0x%08X]", item.id)) {
                                ImGui.bullet(); ImGui.checkbox("Show", item::show)
                                ImGui.bullet()
                                val temp = item.color.toInt().vec4
                                if (ImGui.colorEdit4("Color", temp, ColorEditFlag.NoInputs))
                                    item.color = temp.u32.toUInt()

                                ImGui.bulletText("NameOffset: " + item.nameOffset)
                                ImGui.bulletText("Name: " + if (item.nameOffset != -1) plot.items.legend.labels[item.nameOffset] else "N/A")
                                ImGui.bulletText("Hovered: " + item.legendHovered)
                                ImGui.treePop()
                            }
                            ImGui.popID()
                        }
                        ImGui.treePop()
                    }
                    for (i in 0..<PLOT_NUM_X_AXES) {
                        val buff = "X-Axis ${i + 1}"
                        if (plot.xAxis(i).enabled && ImGui.treeNode(buff, "X-Axis ${i + 1} [0x%08X]", plot.xAxis(i).id)) {
                            showAxisMetrics(plot, plot xAxis i)
                            ImGui.treePop()
                        }
                    }
                    for (i in 0..<PLOT_NUM_Y_AXES) {
                        val buff = "Y-Axis ${i + 1}"
                        if (plot.yAxis(i).enabled && ImGui.treeNode(buff, "Y-Axis ${i + 1} [0x%08X]", plot.yAxis(i).id)) {
                            showAxisMetrics(plot, plot.yAxis(i))
                            ImGui.treePop()
                        }
                    }
                    ImGui.bulletText("Title: ${plot.title ?: "none"}")
                    ImGui.bulletText("Flags: 0x%08X", plot.flags)
                    ImGui.bulletText("Initialized: " + plot.initialized)
                    ImGui.bulletText("Selecting: " + plot.selecting)
                    ImGui.bulletText("Selected: " + plot.selected)
                    ImGui.bulletText("Hovered: " + plot.hovered)
                    ImGui.bulletText("Held: " + plot.held)
                    ImGui.bulletText("LegendHovered: " + plot.items.legend.hovered)
                    ImGui.bulletText("ContextLocked: " + plot.contextLocked)
                    ImGui.treePop()
                }
                ImGui.popID()
            }
            ImGui.treePop()
        }

        if (ImGui.treeNode("Subplots", "Subplots ($nSubplots)")) {
            for (p in 0..<nSubplots) {
                // plot
                val plot = gp.subplots getByIndex p
                ImGui.pushID(p)
                if (ImGui.treeNode("Subplot", "Subplot [0x%08X]", plot.id)) {
                    val nItems = plot.items.itemCount
                    if (ImGui.treeNode("Items", "Items ($nItems)")) {
                        for (i in 0..<nItems) {
                            val item = plot.items getItemByIndex i
                            ImGui.pushID(i)
                            if (ImGui.treeNode("Item", "Item [0x%08X]", item.id)) {
                                ImGui.bullet(); ImGui.checkbox("Show", item::show)
                                ImGui.bullet()
                                val temp = item.color.toInt().vec4
                                if (ImGui.colorEdit4("Color", temp, ColorEditFlag.NoInputs))
                                    item.color = temp.u32.toUInt()

                                ImGui.bulletText("NameOffset: " + item.nameOffset)
                                ImGui.bulletText("Name: " + if (item.nameOffset != -1) plot.items.legend.labels[item.nameOffset] else "N/A")
                                ImGui.bulletText("Hovered: " + item.legendHovered)
                                ImGui.treePop()
                            }
                            ImGui.popID()
                        }
                        ImGui.treePop()
                    }
                    ImGui.bulletText("Flags: 0x%08X", plot.flags)
                    ImGui.bulletText("FrameHovered: " + plot.frameHovered)
                    ImGui.bulletText("LegendHovered: " + plot.items.legend.hovered)
                    ImGui.treePop()
                }
                ImGui.popID()
            }
            ImGui.treePop()
        }
        if (ImGui.treeNode("Colormaps")) {
            ImGui.bulletText("Colormaps:  " + gp.colormapData.count)
            ImGui.bulletText("Memory: ${gp.colormapData.tables.size * 4} bytes")
            if (ImGui.treeNode("Data")) {
                for (m in 0..<gp.colormapData.count) {
                    val p = PlotColormap of m
                    if (ImGui.treeNode(gp.colormapData getName p)) {
                        val count = gp.colormapData getKeyCount p
                        val size = gp.colormapData getTableSize p
                        val qual = gp.colormapData isQual p
                        ImGui.bulletText("Qualitative: $qual")
                        ImGui.bulletText("Key Count: $count")
                        ImGui.bulletText("Table Size: $size")
                        ImGui.indent()

                        val samp = Vec4()
                        val wid = 32 * 10 - ImGui.frameHeight - ImGui.style.itemSpacing.x
                        ImGui.setNextItemWidth(wid)
                        colormapSlider("##Sample", ::t, samp, "%.3f", p)
                        ImGui.sameLine()
                        ImGui.colorButton("Sampler", samp)
                        ImGui.pushStyleColor(Col.FrameBg, Vec4())
                        ImGui.pushStyleVar(StyleVar.ItemSpacing, Vec2())
                        for (c in 0..<size) {
                            val col = gp.colormapData.getTableColor(p, c).toInt().vec4
                            ImGui.pushID(m * 1000 + c)
                            ImGui.colorButton("", col, none, Vec2(10))
                            ImGui.popID()
                            if ((c + 1) % 32 != 0 && c != size - 1)
                                ImGui.sameLine()
                        }
                        ImGui.popStyleVar()
                        ImGui.popStyleColor()
                        ImGui.unindent()
                        ImGui.treePop()
                    }
                }
                ImGui.treePop()
            }
            ImGui.treePop()
        }
        ImGui.end()
    }

    fun showAxisMetrics(plot: PlotPlot, axis: PlotAxis) {
        ImGui.bulletText("Label: " + if (axis.labelOffset == -1) "[none]" else plot getAxisLabel axis)
        ImGui.bulletText("Flags: 0x%08X", axis.flags)
        ImGui.bulletText("Range: [${axis.range.min},${axis.range.max}]")
        ImGui.bulletText("Pixels: " + axis.pixelSize)
        ImGui.bulletText("Aspect: " + axis.aspect)
        ImGui.bulletText(if (axis.orthoAxis == null) "OrtherAxis: NULL" else "OrthoAxis: 0x%08X".format(axis.orthoAxis!!().id))
        ImGui.bulletText("LinkedMin: " + axis.linkedMin)
        ImGui.bulletText("LinkedMax: " + axis.linkedMax)
        ImGui.bulletText("HasRange: " + axis.hasRange)
        ImGui.bulletText("Hovered: " + axis.hovered)
        ImGui.bulletText("Held: " + axis.held)

        if (ImGui.treeNode("Transform")) {
            ImGui.bulletText("PixelMin: " + axis.pixelMin)
            ImGui.bulletText("PixelMax: " + axis.pixelMax)
            ImGui.bulletText("ScaleToPixel: " + axis.scaleToPixel)
            ImGui.bulletText("ScaleMax: " + axis.scaleMax)
            ImGui.treePop()
        }

        if (ImGui.treeNode("Ticks")) {
            showTicksMetrics(axis.ticker)
            ImGui.treePop()
        }
    }

    fun showTicksMetrics(ticker: PlotTicker) {
        ImGui.bulletText("Size: " + ticker.tickCount)
        ImGui.bulletText("MaxSize: [${ticker.maxSize.x},${ticker.maxSize.y}]")
    }
}