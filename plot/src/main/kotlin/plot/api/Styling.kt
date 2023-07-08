package plot.api

import glm_.asIntBits
import glm_.bitsAsFloat
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.Col
import imgui.StyleVar
import imgui.internal.classes.ColorMod
import imgui.internal.classes.StyleMod
import imgui.pop
import imgui.vec4
import plot.internalApi.gImPlot
import plot.internalApi.gImPlotNullable

//-----------------------------------------------------------------------------
// [SECTION] Styling
//-----------------------------------------------------------------------------

// Styling colors in ImPlot works similarly to styling colors in ImGui, but
// with one important difference. Like ImGui, all style colors are stored in an
// indexable array in ImPlotStyle. You can permanently modify these values through
// GetStyle().Colors, or temporarily modify them with Push/Pop functions below.
// However, by default all style colors in ImPlot default to a special color
// IMPLOT_AUTO_COL. The behavior of this color depends upon the style color to
// which it as applied:
//
//     1) For style colors associated with plot items (e.g. ImPlotCol_Line),
//        IMPLOT_AUTO_COL tells ImPlot to color the item with the next unused
//        color in the current colormap. Thus, every item will have a different
//        color up to the number of colors in the colormap, at which point the
//        colormap will roll over. For most use cases, you should not need to
//        set these style colors to anything but IMPLOT_COL_AUTO; you are
//        probably better off changing the current colormap. However, if you
//        need to explicitly color a particular item you may either Push/Pop
//        the style color around the item in question, or use the SetNextXXXStyle
//        API below. If you permanently set one of these style colors to a specific
//        color, or forget to call Pop, then all subsequent items will be styled
//        with the color you set.
//
//     2) For style colors associated with plot styling (e.g. ImPlotCol_PlotBg),
//        IMPLOT_AUTO_COL tells ImPlot to set that color from color data in your
//        **ImGuiStyle**. The ImGuiCol_ that these style colors default to are
//        detailed above, and in general have been mapped to produce plots visually
//        consistent with your current ImGui style. Of course, you are free to
//        manually set these colors to whatever you like, and further can Push/Pop
//        them around individual plots for plot-specific styling (e.g. coloring axes).

// Provides access to plot style structure for permanant modifications to colors, sizes, etc.
val style: PlotStyle
    get() {
        assert(gImPlotNullable != null) { "No current context. Did you call ImPlot::CreateContext() or ImPlot::SetCurrentContext()?" }
        val gp = gImPlot
        return gp.style
    }

// Style plot colors for current ImGui style (default).
fun styleColorsAuto(dst: PlotStyle? = null) {
    val style = dst ?: gImPlot.style
    val colors = style.colors

    style.minorAlpha = 0.25f

    colors[PlotCol.Line] put PLOT_AUTO_COL
    colors[PlotCol.Fill] put PLOT_AUTO_COL
    colors[PlotCol.MarkerOutline] put PLOT_AUTO_COL
    colors[PlotCol.MarkerFill] put PLOT_AUTO_COL
    colors[PlotCol.ErrorBar] put PLOT_AUTO_COL
    colors[PlotCol.FrameBg] put PLOT_AUTO_COL
    colors[PlotCol.PlotBg] put PLOT_AUTO_COL
    colors[PlotCol.PlotBorder] put PLOT_AUTO_COL
    colors[PlotCol.LegendBg] put PLOT_AUTO_COL
    colors[PlotCol.LegendBorder] put PLOT_AUTO_COL
    colors[PlotCol.LegendText] put PLOT_AUTO_COL
    colors[PlotCol.TitleText] put PLOT_AUTO_COL
    colors[PlotCol.InlayText] put PLOT_AUTO_COL
    colors[PlotCol.PlotBorder] put PLOT_AUTO_COL
    colors[PlotCol.AxisText] put PLOT_AUTO_COL
    colors[PlotCol.AxisGrid] put PLOT_AUTO_COL
    colors[PlotCol.AxisTick] put PLOT_AUTO_COL
    colors[PlotCol.AxisBg] put PLOT_AUTO_COL
    colors[PlotCol.AxisBgHovered] put PLOT_AUTO_COL
    colors[PlotCol.AxisBgActive] put PLOT_AUTO_COL
    colors[PlotCol.Selection] put PLOT_AUTO_COL
    colors[PlotCol.Crosshairs] put PLOT_AUTO_COL
}

// Style plot colors for ImGui "Classic".
fun styleColorsClassic(dst: PlotStyle? = null) {
    val style = dst ?: gImPlot.style
    val colors = style.colors

    style.minorAlpha = 0.5f

    colors[PlotCol.Line] put PLOT_AUTO_COL
    colors[PlotCol.Fill] put PLOT_AUTO_COL
    colors[PlotCol.MarkerOutline] put PLOT_AUTO_COL
    colors[PlotCol.MarkerFill] put PLOT_AUTO_COL
    colors[PlotCol.ErrorBar].put(0.90f, 0.90f, 0.90f, 1.00f)
    colors[PlotCol.FrameBg].put(0.43f, 0.43f, 0.43f, 0.39f)
    colors[PlotCol.PlotBg].put(0.00f, 0.00f, 0.00f, 0.35f)
    colors[PlotCol.PlotBorder].put(0.50f, 0.50f, 0.50f, 0.50f)
    colors[PlotCol.LegendBg].put(0.11f, 0.11f, 0.14f, 0.92f)
    colors[PlotCol.LegendBorder].put(0.50f, 0.50f, 0.50f, 0.50f)
    colors[PlotCol.LegendText].put(0.90f, 0.90f, 0.90f, 1.00f)
    colors[PlotCol.TitleText].put(0.90f, 0.90f, 0.90f, 1.00f)
    colors[PlotCol.InlayText].put(0.90f, 0.90f, 0.90f, 1.00f)
    colors[PlotCol.AxisText].put(0.90f, 0.90f, 0.90f, 1.00f)
    colors[PlotCol.AxisGrid].put(0.90f, 0.90f, 0.90f, 0.25f)
    colors[PlotCol.AxisTick] put PLOT_AUTO_COL // TODO
    colors[PlotCol.AxisBg] put PLOT_AUTO_COL // TODO
    colors[PlotCol.AxisBgHovered] put PLOT_AUTO_COL // TODO
    colors[PlotCol.AxisBgActive] put PLOT_AUTO_COL // TODO
    colors[PlotCol.Selection].put(0.97f, 0.97f, 0.39f, 1.00f)
    colors[PlotCol.Crosshairs].put(0.50f, 0.50f, 0.50f, 0.75f)
}

// Style plot colors for ImGui "Dark".
fun styleColorsDark(dst: PlotStyle? = null) {
    val style = dst ?: gImPlot.style
    val colors = style.colors

    style.minorAlpha = 0.25f

    colors[PlotCol.Line] put PLOT_AUTO_COL
    colors[PlotCol.Fill] put PLOT_AUTO_COL
    colors[PlotCol.MarkerOutline] put PLOT_AUTO_COL
    colors[PlotCol.MarkerFill] put PLOT_AUTO_COL
    colors[PlotCol.ErrorBar] put PLOT_AUTO_COL
    colors[PlotCol.FrameBg].put(1.00f, 1.00f, 1.00f, 0.07f)
    colors[PlotCol.PlotBg].put(0.00f, 0.00f, 0.00f, 0.50f)
    colors[PlotCol.PlotBorder].put(0.43f, 0.43f, 0.50f, 0.50f)
    colors[PlotCol.LegendBg].put(0.08f, 0.08f, 0.08f, 0.94f)
    colors[PlotCol.LegendBorder].put(0.43f, 0.43f, 0.50f, 0.50f)
    colors[PlotCol.LegendText].put(1.00f, 1.00f, 1.00f, 1.00f)
    colors[PlotCol.TitleText].put(1.00f, 1.00f, 1.00f, 1.00f)
    colors[PlotCol.InlayText].put(1.00f, 1.00f, 1.00f, 1.00f)
    colors[PlotCol.AxisText].put(1.00f, 1.00f, 1.00f, 1.00f)
    colors[PlotCol.AxisGrid].put(1.00f, 1.00f, 1.00f, 0.25f)
    colors[PlotCol.AxisTick] put PLOT_AUTO_COL // TODO
    colors[PlotCol.AxisBg] put PLOT_AUTO_COL // TODO
    colors[PlotCol.AxisBgHovered] put PLOT_AUTO_COL // TODO
    colors[PlotCol.AxisBgActive] put PLOT_AUTO_COL // TODO
    colors[PlotCol.Selection].put(1.00f, 0.60f, 0.00f, 1.00f)
    colors[PlotCol.Crosshairs].put(1.00f, 1.00f, 1.00f, 0.50f)
}

// Style plot colors for ImGui "Light".
fun styleColorsLight(dst: PlotStyle? = null) {
    val style = dst ?: gImPlot.style
    val colors = style.colors

    style.minorAlpha = 1f

    colors[PlotCol.Line] put PLOT_AUTO_COL
    colors[PlotCol.Fill] put PLOT_AUTO_COL
    colors[PlotCol.MarkerOutline] put PLOT_AUTO_COL
    colors[PlotCol.MarkerFill] put PLOT_AUTO_COL
    colors[PlotCol.ErrorBar] put PLOT_AUTO_COL
    colors[PlotCol.FrameBg].put(1.00f, 1.00f, 1.00f, 1.00f)
    colors[PlotCol.PlotBg].put(0.42f, 0.57f, 1.00f, 0.13f)
    colors[PlotCol.PlotBorder].put(0.00f, 0.00f, 0.00f, 0.00f)
    colors[PlotCol.LegendBg].put(1.00f, 1.00f, 1.00f, 0.98f)
    colors[PlotCol.LegendBorder].put(0.82f, 0.82f, 0.82f, 0.80f)
    colors[PlotCol.LegendText].put(0.00f, 0.00f, 0.00f, 1.00f)
    colors[PlotCol.TitleText].put(0.00f, 0.00f, 0.00f, 1.00f)
    colors[PlotCol.InlayText].put(0.00f, 0.00f, 0.00f, 1.00f)
    colors[PlotCol.AxisText].put(0.00f, 0.00f, 0.00f, 1.00f)
    colors[PlotCol.AxisGrid].put(1.00f, 1.00f, 1.00f, 1.00f)
    colors[PlotCol.AxisTick].put(0.00f, 0.00f, 0.00f, 0.25f)
    colors[PlotCol.AxisBg] put PLOT_AUTO_COL // TODO
    colors[PlotCol.AxisBgHovered] put PLOT_AUTO_COL // TODO
    colors[PlotCol.AxisBgActive] put PLOT_AUTO_COL // TODO
    colors[PlotCol.Selection].put(0.82f, 0.64f, 0.03f, 1.00f)
    colors[PlotCol.Crosshairs].put(0.00f, 0.00f, 0.00f, 0.50f)
}

// Use PushStyleX to temporarily modify your ImPlotStyle. The modification
// will last until the matching call to PopStyleX. You MUST call a pop for
// every push, otherwise you will leak memory! This behaves just like ImGui.

// Temporarily modify a style color. Don't forget to call PopStyleColor!
fun pushStyleColor(idx: PlotCol, col: UInt) {
    val gp = gImPlot
    val backup = ColorMod(Col of idx.i, gp.style.colors[idx])
    gp.colorModifiers += backup
    gp.style.colors[idx] = col.toInt().vec4
}

fun pushStyleColor(idx: PlotCol, col: Vec4) {
    val gp = gImPlot
    val backup = ColorMod(Col of idx.i, gp.style.colors[idx])
    gp.colorModifiers += backup
    gp.style.colors[idx] put col
}

// Undo temporary style color modification(s). Undo multiple pushes at once by increasing count.
fun popStyleColor(count_: Int = 1) {
    var count = count_
    val gp = gImPlot
    assert(count <= gp.colorModifiers.size) { "You can't pop more modifiers than have been pushed!" }
    while (count > 0) {
        val backup = gp.colorModifiers.last()
        gp.style.colors[backup.col.i] = backup.backupValue
        gp.colorModifiers.pop()
        count--
    }
}

// Temporarily modify a style variable of [JVM] *any* type. Don't forget to call PopStyleVar!
fun pushStyleVar(idx: PlotStyleVar, value: Any) {
    infix fun FloatArray.put(v: Vec2) {
        v to this
    }

    val gp = gImPlot
    gp.styleModifiers += StyleMod(StyleVar of idx.i).apply {
        when (idx) {
            PlotStyleVar.LineWeight -> {
                floats[0] = gp.style.lineWeight
                gp.style.lineWeight = value as Float
            }
            PlotStyleVar.Marker -> {
                floats[0] = gp.style.marker.i.bitsAsFloat
                gp.style.marker = value as PlotMarker
            }
            PlotStyleVar.MarkerSize -> {
                floats[0] = gp.style.markerSize
                gp.style.markerSize = value as Float
            }
            PlotStyleVar.MarkerWeight -> {
                floats[0] = gp.style.markerWeight
                gp.style.markerWeight = value as Float
            }
            PlotStyleVar.FillAlpha -> {
                floats[0] = gp.style.fillAlpha
                gp.style.fillAlpha = value as Float
            }
            PlotStyleVar.ErrorBarSize -> {
                floats[0] = gp.style.errorBarSize
                gp.style.errorBarSize = value as Float
            }
            PlotStyleVar.ErrorBarWeight -> {
                floats[0] = gp.style.errorBarWeight
                gp.style.errorBarWeight = value as Float
            }
            PlotStyleVar.DigitalBitHeight -> {
                floats[0] = gp.style.digitalBitHeight
                gp.style.digitalBitHeight = value as Float
            }
            PlotStyleVar.DigitalBitGap -> {
                floats[0] = gp.style.digitalBitGap
                gp.style.digitalBitGap = value as Float
            }
            PlotStyleVar.PlotBorderSize -> {
                floats[0] = gp.style.plotBorderSize
                gp.style.plotBorderSize = value as Float
            }
            PlotStyleVar.MinorAlpha -> {
                floats[0] = gp.style.minorAlpha
                gp.style.minorAlpha = value as Float
            }
            PlotStyleVar.MajorTickLen -> {
                floats put gp.style.majorTickLen
                gp.style.majorTickLen put value as Vec2
            }
            PlotStyleVar.MinorTickLen -> {
                floats put gp.style.minorTickLen
                gp.style.minorTickLen put value as Vec2
            }
            PlotStyleVar.MajorTickSize -> {
                floats put gp.style.majorTickSize
                gp.style.majorTickSize put value as Vec2
            }
            PlotStyleVar.MinorTickSize -> {
                floats put gp.style.minorTickSize
                gp.style.minorTickSize put value as Vec2
            }
            PlotStyleVar.MajorGridSize -> {
                floats put gp.style.majorGridSize
                gp.style.majorGridSize put value as Vec2
            }
            PlotStyleVar.MinorGridSize -> {
                floats put gp.style.minorGridSize
                gp.style.minorGridSize put value as Vec2
            }
            PlotStyleVar.PlotPadding -> {
                floats put gp.style.plotPadding
                gp.style.plotPadding put value as Vec2
            }
            PlotStyleVar.LabelPadding -> {
                floats put gp.style.labelPadding
                gp.style.labelPadding put value as Vec2
            }
            PlotStyleVar.LegendPadding -> {
                floats put gp.style.legendPadding
                gp.style.legendPadding put value as Vec2
            }
            PlotStyleVar.LegendInnerPadding -> {
                floats put gp.style.legendInnerPadding
                gp.style.legendInnerPadding put value as Vec2
            }
            PlotStyleVar.LegendSpacing -> {
                floats put gp.style.legendSpacing
                gp.style.legendSpacing put value as Vec2
            }
            PlotStyleVar.MousePosPadding -> {
                floats put gp.style.mousePosPadding
                gp.style.mousePosPadding put value as Vec2
            }
            PlotStyleVar.AnnotationPadding -> {
                floats put gp.style.annotationPadding
                gp.style.annotationPadding put value as Vec2
            }
            PlotStyleVar.FitPadding -> {
                floats put gp.style.fitPadding
                gp.style.fitPadding put value as Vec2
            }
            PlotStyleVar.PlotDefaultSize -> {
                floats put gp.style.plotDefaultSize
                gp.style.plotDefaultSize put value as Vec2
            }
            PlotStyleVar.PlotMinSize -> {
                floats put gp.style.plotMinSize
                gp.style.plotMinSize put value as Vec2
            }
        }
    }
}

// Temporarily modify a style variable of int type. Don't forget to call PopStyleVar!
//fun pushStyleVar(idx: PlotStyleVar, value: Int) {
//    val gp = gImPlot
//    val varInfo = getPlotStyleVarInfo(idx)
//    if (varInfo.type == DataType.Int && varInfo.count == 1) {
//        gp.styleModifiers += StyleMod(StyleVar of idx.i).apply { floats[0] = value.bitsAsFloat }
//        return
//    }
////    else if (var_info->Type == ImGuiDataType_Float && var_info->Count == 1) {
////        float* pvar = (float*)var_info->GetVarPtr(&gp.Style);
////        gp.StyleModifiers.push_back(ImGuiStyleMod((ImGuiStyleVar)idx, *pvar));
////        *pvar = (float)val;
////        return;
////    }
//    error("Called PushStyleVar() int variant but variable is not a int!")
//}
//
//// Temporarily modify a style variable of ImVec2 type. Don't forget to call PopStyleVar!
//fun pushStyleVar(idx: PlotStyleVar, value: Vec2) {
//    val gp = gImPlot
//    val varInfo = getPlotStyleVarInfo(idx)
//    if (varInfo.type == DataType.Float && varInfo.count == 2) {
//        gp.styleModifiers += StyleMod(StyleVar of idx.i).apply { value to floats }
//        return
//    }
//    error("Called PushStyleVar() ImVec2 variant but variable is not a ImVec2!")
//}

// Undo temporary style variable modification(s). Undo multiple pushes at once by increasing count.
fun popStyleVar(count_: Int = 1) {
    var count = count_
    val gp = gImPlot
    check(count <= gp.styleModifiers.size) { "You can't pop more modifiers than have been pushed!" }
    while (count > 0) {
        val backup = gp.styleModifiers.last()
//        val info = getPlotStyleVarInfo(PlotStyleVar of backup.idx.i)
//        void * data = info->GetVarPtr(&gp.Style)
//        if (info->Type == ImGuiDataType_Float && info->Count == 1) {
//            ((float *) data)[0] = backup.BackupFloat[0]
//        }
//        else if (info->Type == ImGuiDataType_Float && info->Count == 2) {
//            ((float *) data)[0] = backup.BackupFloat[0]
//            ((float *) data)[1] = backup.BackupFloat[1]
//        }
//        else if (info->Type == ImGuiDataType_S32 && info->Count == 1) {
//            ((int *) data)[0] = backup.BackupInt[0]
//        }
        when (backup.idx.i) {
            PlotStyleVar.LineWeight.i -> gp.style.lineWeight = backup.floats[0]
            PlotStyleVar.Marker.i -> gp.style.marker = PlotMarker of backup.floats[0].asIntBits
            PlotStyleVar.MarkerSize.i -> gp.style.markerSize = backup.floats[0]
            PlotStyleVar.MarkerWeight.i -> gp.style.markerWeight = backup.floats[0]
            PlotStyleVar.FillAlpha.i -> gp.style.fillAlpha = backup.floats[0]
            PlotStyleVar.ErrorBarSize.i -> gp.style.errorBarSize = backup.floats[0]
            PlotStyleVar.ErrorBarWeight.i -> gp.style.errorBarWeight = backup.floats[0]
            PlotStyleVar.DigitalBitHeight.i -> gp.style.digitalBitHeight = backup.floats[0]
            PlotStyleVar.DigitalBitGap.i -> gp.style.digitalBitGap = backup.floats[0]
            PlotStyleVar.PlotBorderSize.i -> gp.style.plotBorderSize = backup.floats[0]
            PlotStyleVar.MinorAlpha.i -> gp.style.minorAlpha = backup.floats[0]
            PlotStyleVar.MajorTickLen.i -> gp.style.majorTickLen put backup.floats
            PlotStyleVar.MinorTickLen.i -> gp.style.minorTickLen put backup.floats
            PlotStyleVar.MajorTickSize.i -> gp.style.majorTickSize put backup.floats
            PlotStyleVar.MinorTickSize.i -> gp.style.minorTickSize put backup.floats
            PlotStyleVar.MajorGridSize.i -> gp.style.majorGridSize put backup.floats
            PlotStyleVar.MinorGridSize.i -> gp.style.minorGridSize put backup.floats
            PlotStyleVar.PlotPadding.i -> gp.style.plotPadding put backup.floats
            PlotStyleVar.LabelPadding.i -> gp.style.labelPadding put backup.floats
            PlotStyleVar.LegendPadding.i -> gp.style.legendPadding put backup.floats
            PlotStyleVar.LegendInnerPadding.i -> gp.style.legendInnerPadding put backup.floats
            PlotStyleVar.LegendSpacing.i -> gp.style.legendSpacing put backup.floats
            PlotStyleVar.MousePosPadding.i -> gp.style.mousePosPadding put backup.floats
            PlotStyleVar.AnnotationPadding.i -> gp.style.annotationPadding put backup.floats
            PlotStyleVar.FitPadding.i -> gp.style.fitPadding put backup.floats
            PlotStyleVar.PlotDefaultSize.i -> gp.style.plotDefaultSize put backup.floats
            PlotStyleVar.PlotMinSize.i -> gp.style.plotMinSize put backup.floats
        }
        gp.styleModifiers.pop()
        count--
    }
}

// The following can be used to modify the style of the next plot item ONLY. They do
// NOT require calls to PopStyleX. Leave style attributes you don't want modified to
// IMPLOT_AUTO or IMPLOT_AUTO_COL. Automatic styles will be deduced from the current
// values in your ImPlotStyle or from Colormap data.

// Set the line color and weight for the next item only.
fun setNextLineStyle(col: Vec4 = PLOT_AUTO_COL, weight: Float = PLOT_AUTO) {
    val gp = gImPlot
    gp.nextItemData.colors[PlotCol.Line] put col
    gp.nextItemData.lineWeight = weight
}

// Set the fill color for the next item only.
fun setNextFillStyle(col: Vec4 = PLOT_AUTO_COL, alpha: Float = PLOT_AUTO) {
    val gp = gImPlot
    gp.nextItemData.colors[PlotCol.Fill] put col
    gp.nextItemData.fillAlpha = alpha
}

// Set the marker style for the next item only.
fun setNextMarkerStyle(marker: PlotMarker = PlotMarker.None, size: Float = PLOT_AUTO, fill: Vec4 = PLOT_AUTO_COL, weight: Float = PLOT_AUTO, outline: Vec4 = PLOT_AUTO_COL) {
    val gp = gImPlot
    gp.nextItemData.marker = marker
    gp.nextItemData.colors[PlotCol.MarkerFill] put fill
    gp.nextItemData.markerSize = size
    gp.nextItemData.colors[PlotCol.MarkerOutline] put outline
    gp.nextItemData.markerWeight = weight
}

// Set the error bar style for the next item only.
fun setNextErrorBarStyle(col: Vec4 = PLOT_AUTO_COL, size: Float = PLOT_AUTO, weight: Float = PLOT_AUTO) {
    val gp = gImPlot
    gp.nextItemData.colors[PlotCol.ErrorBar] put col
    gp.nextItemData.errorBarSize = size
    gp.nextItemData.errorBarWeight = weight
}

// Gets the last item primary color (i.e. its legend icon color)
val lastItemColor: Vec4
    get() {
        val gp = gImPlot
        return gp.previousItem?.color?.toInt()?.vec4 ?: Vec4()
    }

//// Returns the null terminated string name for an ImPlotCol.
//IMPLOT_API const char* GetStyleColorName(ImPlotCol idx)
//// Returns the null terminated string name for an ImPlotMarker.
//IMPLOT_API const char* GetMarkerName(ImPlotMarker idx)