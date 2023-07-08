@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalStdlibApi::class)

package plot.api

import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.api.slider
import imgui.classes.DrawList
import imgui.classes.TextFilter
import imgui.internal.classes.Rect
import plot.internalApi.*

//-----------------------------------------------------------------------------
// [SECTION] Miscellaneous
//-----------------------------------------------------------------------------

// Render icons similar to those that appear in legends (nifty for data lists).
fun itemIcon(col: Vec4) = itemIcon(col.u32)

fun itemIcon(col: Int) {
    val txtSize = ImGui.textLineHeight
    val size = Vec2(txtSize - 4, txtSize)
    val window = ImGui.currentWindow
    val pos = window.dc.cursorPos // [JVM] safe using same instance
    ImGui.windowDrawList.addRectFilled(pos + Vec2(0, 2), pos + size - Vec2(0, 2), col)
    ImGui.dummy(size)
}

fun colormapIcon(cmap: PlotColormap) {
    val gp = gImPlot
    val txtSize = ImGui.textLineHeight
    val size = Vec2(txtSize - 4, txtSize)
    val window = ImGui.currentWindow
    val pos = window.dc.cursorPos // [JVM] safe using same instance
    val rect = Rect(pos + Vec2(0, 2), pos + size - Vec2(0, 2))
    val drawList = ImGui.windowDrawList
    renderColorBar(gp.colormapData.getKeys(cmap), drawList, rect, false, false, !gp.colormapData.isQual(cmap))
    ImGui.dummy(size)
}

// Get the plot draw list for custom rendering to the current plot area. Call between Begin/EndPlot.
val plotDrawList: DrawList get() = ImGui.windowDrawList

// Push clip rect for rendering to current plot area. The rect can be expanded or contracted by #expand pixels. Call between Begin/EndPlot.
fun pushPlotClipRect(expand: Float = 0f) {
    val gp = gImPlot
    assert(gp.currentPlot != null) { "PushPlotClipRect() needs to be called between BeginPlot() and EndPlot()!" }
    setupLock()
    val rect = Rect(gp.currentPlot!!.plotRect)
    rect expand expand
    ImGui.pushClipRect(rect.min, rect.max, true)
}

// Pop plot clip rect. Call between Begin/EndPlot.
fun popPlotClipRect() {
    setupLock()
    ImGui.popClipRect()
}

fun helpMarker(desc: String) {
    ImGui.textDisabled("(?)")
    if (ImGui.isItemHovered()) {
        ImGui.beginTooltip()
        ImGui.pushTextWrapPos(ImGui.fontSize * 35f)
        ImGui.textUnformatted(desc)
        ImGui.popTextWrapPos()
        ImGui.endTooltip()
    }
}

private var styleIdx = -1

// Shows ImPlot style selector dropdown menu.
fun showStyleSelector(label: String): Boolean {
    if (ImGui.combo(label, ::styleIdx, "Auto\u0000Classic\u0000Dark\u0000Light\u0000")) {
        when (styleIdx) {
            0 -> styleColorsAuto()
            1 -> styleColorsClassic()
            2 -> styleColorsDark()
            3 -> styleColorsLight()
        }
        return true
    }
    return false
}
// Shows ImPlot colormap selector dropdown menu.
//IMPLOT_API bool ShowColormapSelector(const char* label)
// Shows ImPlot input map selector dropdown menu.
//IMPLOT_API bool ShowInputMapSelector(const char* label)


private var refSavedStyle: PlotStyle? = null
private var init = true
private var outputDest0 = 0
private var outputOnlyModified = false
private val filter = TextFilter()
private var alphaFlags: ColorEditFlags = ColorEditFlag.AlphaPreviewHalf
private var outputDest1 = 0
private var edit = false
private val custom = ArrayList<Vec4>()
private val name = "MyColormap".toByteArray(16)
private var qual = true

// Shows ImPlot style editor block (not a window).
fun showStyleEditor(ref_: PlotStyle? = null) {
    val gp = gImPlot
    var style = plot.api.style
    // Default to using internal storage as reference
    if (init && ref_ == null)
        refSavedStyle = style
    init = false
    var ref = ref_ ?: refSavedStyle!!

    if (showStyleSelector("Colors##Selector"))
        refSavedStyle = style

    // Save/Revert button
    if (ImGui.button("Save Ref")) {
        ref = style; refSavedStyle = style
    }
    ImGui.sameLine()
    if (ImGui.button("Revert Ref")) {
        style = ref!!
    }
    ImGui.sameLine()
    helpMarker("Save/Revert in local non-persistent storage. Default Colors definition are not affected. Use \"Export\" below to save them somewhere.")
    if (ImGui.beginTabBar("##StyleEditor")) {
        if (ImGui.beginTabItem("Variables")) {
            ImGui.text("Item Styling")
            ImGui.slider("LineWeight", style::lineWeight, 0f, 5f, "%.1f")
            ImGui.slider("MarkerSize", style::markerSize, 2f, 10f, "%.1f")
            ImGui.slider("MarkerWeight", style::markerWeight, 0f, 5f, "%.1f")
            ImGui.slider("FillAlpha", style::fillAlpha, 0f, 1f, "%.2f")
            ImGui.slider("ErrorBarSize", style::errorBarSize, 0f, 10f, "%.1f")
            ImGui.slider("ErrorBarWeight", style::errorBarWeight, 0f, 5f, "%.1f")
            ImGui.slider("DigitalBitHeight", style::digitalBitHeight, 0f, 20f, "%.1f")
            ImGui.slider("DigitalBitGap", style::digitalBitGap, 0f, 20f, "%.1f")
            ImGui.text("Plot Styling")
            ImGui.slider("PlotBorderSize", style::plotBorderSize, 0f, 2f, "%.0f")
            ImGui.slider("MinorAlpha", style::minorAlpha, 0f, 1f, "%.2f")
            ImGui.slider2("MajorTickLen", style.majorTickLen, 0f, 20f, "%.0f")
            ImGui.slider2("MinorTickLen", style.minorTickLen, 0f, 20f, "%.0f")
            ImGui.slider2("MajorTickSize", style.majorTickSize, 0f, 2f, "%.1f")
            ImGui.slider2("MinorTickSize", style.minorTickSize, 0f, 2f, "%.1f")
            ImGui.slider2("MajorGridSize", style.majorGridSize, 0f, 2f, "%.1f")
            ImGui.slider2("MinorGridSize", style.minorGridSize, 0f, 2f, "%.1f")
            ImGui.slider2("PlotDefaultSize", style.plotDefaultSize, 0f, 1_000f, "%.0f")
            ImGui.slider2("PlotMinSize", style.plotMinSize, 0f, 300f, "%.0f")
            ImGui.text("Plot Padding")
            ImGui.slider2("PlotPadding", style.plotPadding, 0f, 20f, "%.0f")
            ImGui.slider2("LabelPadding", style.labelPadding, 0f, 20f, "%.0f")
            ImGui.slider2("LegendPadding", style.legendPadding, 0f, 20f, "%.0f")
            ImGui.slider2("LegendInnerPadding", style.legendInnerPadding, 0f, 10f, "%.0f")
            ImGui.slider2("LegendSpacing", style.legendSpacing, 0f, 5f, "%.0f")
            ImGui.slider2("MousePosPadding", style.mousePosPadding, 0f, 20f, "%.0f")
            ImGui.slider2("AnnotationPadding", style.annotationPadding, 0f, 5f, "%.0f")
            ImGui.slider2("FitPadding", style.fitPadding, 0f, 0.2f, "%.2f")

            ImGui.endTabItem()
        }
        if (ImGui.beginTabItem("Colors")) {

            if (ImGui.button("Export", Vec2(75, 0))) {
                if (outputDest0 == 0)
                    ImGui.logToClipboard()
                else
                    ImGui.logToTTY()
                ImGui.logText("ImVec4* colors = ImPlot::GetStyle().Colors;\n")
                for (c in PlotCol.values()) {
                    val col = style.colors[c.i]
                    val name = c.name
                    if (!outputOnlyModified || col != ref.colors[c.i])
                        if (c.isColorAuto)
                            ImGui.logText("colors[ImPlotCol_$name]${" ".repeat(14 - name.length)}= IMPLOT_AUTO_COL;\n")
                        else
                            ImGui.logText("colors[ImPlotCol_$name]${" ".repeat(14 - name.length)}= ImVec4(%.2ff, %.2ff, %.2ff, %.2ff);\n", col.x, col.y, col.z, col.w)
                }
                ImGui.logFinish()
            }
            ImGui.sameLine(); ImGui.setNextItemWidth(120f); ImGui.combo("##output_type", ::outputDest0, "To Clipboard\u0000To TTY\u0000")
            ImGui.sameLine(); ImGui.checkbox("Only Modified Colors", ::outputOnlyModified)

            filter.draw("Filter colors", ImGui.fontSize * 16)

            if (ImGui.radioButton("Opaque", alphaFlags == none)) {; alphaFlags = none; }; ImGui.sameLine()
            if (ImGui.radioButton("Alpha", alphaFlags == ColorEditFlag.AlphaPreview)) {; alphaFlags = ColorEditFlag.AlphaPreview; }; ImGui.sameLine()
            if (ImGui.radioButton("Both", alphaFlags == ColorEditFlag.AlphaPreviewHalf)) {; alphaFlags = ColorEditFlag.AlphaPreviewHalf; }; ImGui.sameLine()
            helpMarker("In the color list:\n" +
                               "Left-click on colored square to open color picker,\n" +
                               "Right-click to open edit options menu.")
            ImGui.separator()
            ImGui.pushItemWidth(-160f)
            for (p in PlotCol.values()) {
                val name = p.name
                if (!filter.passFilter(name))
                    continue
                ImGui.pushID(p.i)
                val temp = p.vec4
                val isAuto = p.isColorAuto
                if (!isAuto)
                    ImGui.pushStyleVar(StyleVar.Alpha, 0.25f)
                if (ImGui.button("Auto")) {
                    style.colors[p.i] put when {
                        isAuto -> temp
                        else -> PLOT_AUTO_COL
                    }
                    bustItemCache()
                }
                if (!isAuto)
                    ImGui.popStyleVar()
                ImGui.sameLine()
                if (ImGui.colorEdit4(name, temp, ColorEditFlag.NoInputs / alphaFlags)) {
                    style.colors[p.i] = temp
                    bustItemCache()
                }
                if (style.colors[p.i] != ref.colors[p.i]) {
                    ImGui.sameLine(175); if (ImGui.button("Save")) ref.colors[p.i] put style.colors[p.i]
                    ImGui.sameLine(); if (ImGui.button("Revert")) {
                        style.colors[p.i] put ref.colors[p.i]
                        bustItemCache()
                    }
                }
                ImGui.popID()
            }
            ImGui.popItemWidth()
            ImGui.separator()
            ImGui.text("Colors that are set to Auto (i.e. IMPLOT_AUTO_COL) will\n" +
                               "be automatically deduced from your ImGui style or the\n" +
                               "current ImPlot Colormap. If you want to style individual\n" +
                               "plot items, use Push/PopStyleColor around its function.")
            ImGui.endTabItem()
        }
        if (ImGui.beginTabItem("Colormaps")) {
            if (ImGui.button("Export", Vec2(75, 0))) {
                if (outputDest1 == 0)
                    ImGui.logToClipboard()
                else
                    ImGui.logToTTY()
                val size = getColormapSize()
                val name = gp.style.colormap.name
                ImGui.logText("static const ImU32 ${name}_Data[$size] = {\n")
                for (i in 0..<size) {
                    val col = getColormapColorU32(i, gp.style.colormap)
                    ImGui.logText("    $col${if (i == size - 1) "" else ","}\n")
                }
                ImGui.logText("};\nImPlotColormap $name = ImPlot::AddColormap(\"$name\", ${name}_Data, $size);")
                ImGui.logFinish()
            }
            ImGui.sameLine(); ImGui.setNextItemWidth(120f); ImGui.combo("##output_type", ::outputDest1, "To Clipboard\u0000To TTY\u0000")
            ImGui.sameLine()
            ImGui.checkbox("Edit Mode", ::edit)

            // built-in/added
            ImGui.separator()
            for (i in 0..<gp.colormapData.count) {
                val pc = PlotColormap of i
                ImGui.pushID(i)
                val size = gp.colormapData getKeyCount pc
                val selected = i == gp.style.colormap.i

                val name = pc.name
                if (!selected)
                    ImGui.pushStyleVar(StyleVar.Alpha, 0.25f)
                if (ImGui.button(name, Vec2(100, 0))) {
                    gp.style.colormap = pc
                    bustItemCache()
                }
                if (!selected)
                    ImGui.popStyleVar()
                ImGui.sameLine()
                ImGui.beginGroup()
                if (edit) {
                    for (c in 0..<size) {
                        ImGui.pushID(c)
                        val col4 = gp.colormapData.getKeyColor(pc, c).toInt().vec4
                        if (ImGui.colorEdit4("", col4, ColorEditFlag.NoInputs)) {
                            val col32 = col4.u32.toUInt()
                            gp.colormapData.setKeyColor(pc, c, col32)
                            bustItemCache()
                        }
                        if ((c + 1) % 12 != 0 && c != size - 1)
                            ImGui.sameLine()
                        ImGui.popID()
                    }
                } else {
                    if (colormapButton("##", Vec2(-1, 0), pc))
                        edit = true
                }
                ImGui.endGroup()
                ImGui.popID()
            }

            if (custom.size == 0) {
                custom += Vec4(1, 0, 0, 1)
                custom += Vec4(0, 1, 0, 1)
                custom += Vec4(0, 0, 1, 1)
            }
            ImGui.separator()
            ImGui.beginGroup()


            if (ImGui.button("+", Vec2((100 - ImGui.style.itemSpacing.x) / 2, 0)))
                custom += Vec4(0, 0, 0, 1)
            ImGui.sameLine()
            if (ImGui.button("-", Vec2((100 - ImGui.style.itemSpacing.x) / 2, 0)) && custom.size > 2)
                custom.pop()
            ImGui.setNextItemWidth(100f)
            ImGui.inputText("##Name", name, InputTextFlag.CharsNoBlank)
            ImGui.checkbox("Qualitative", ::qual)
            if (ImGui.button("Add", Vec2(100, 0)) && gp.colormapData getIndex name.cStr == PlotColormap.None)
                addColormap(name.cStr, custom, qual)

            ImGui.endGroup()
            ImGui.sameLine()
            ImGui.beginGroup()
            for (c in custom.indices) {
                ImGui.pushID(c)
                if (ImGui.colorEdit4("##Col1", custom[c], ColorEditFlag.NoInputs)) {

                }
                if ((c + 1) % 12 != 0)
                    ImGui.sameLine()
                ImGui.popID()
            }
            ImGui.endGroup()

            ImGui.endTabItem()
        }
        ImGui.endTabBar()
    }
}
// Add basic help/info block for end users (not a window).
//IMPLOT_API void ShowUserGuide()
//// Shows ImPlot metrics/debug information window.
//IMPLOT_API void ShowMetricsWindow(bool* p_popen = nullptr)