package plot.api

import glm_.vec4.Vec4
import imgui.Col
import imgui.DataType
import imgui.ImGui
import imgui.ImGui.getStyleColorVec4
import plot.internalApi.vec4
import java.util.Stack

//-----------------------------------------------------------------------------
// Style
//-----------------------------------------------------------------------------

//const char* GetStyleColorName(ImPlotCol col) {
//    static const char * col_names[ImPlotCol_COUNT] = {
//        "Line",
//        "Fill",
//        "MarkerOutline",
//        "MarkerFill",
//        "ErrorBar",
//        "FrameBg",
//        "PlotBg",
//        "PlotBorder",
//        "LegendBg",
//        "LegendBorder",
//        "LegendText",
//        "TitleText",
//        "InlayText",
//        "AxisText",
//        "AxisGrid",
//        "AxisTick",
//        "AxisBg",
//        "AxisBgHovered",
//        "AxisBgActive",
//        "Selection",
//        "Crosshairs"
//    };
//    return col_names[col];
//}
//
//const char* GetMarkerName(ImPlotMarker marker) {
//    switch(marker) {
//        case ImPlotMarker_None :     return "None";
//        case ImPlotMarker_Circle :   return "Circle";
//        case ImPlotMarker_Square :   return "Square";
//        case ImPlotMarker_Diamond :  return "Diamond";
//        case ImPlotMarker_Up :       return "Up";
//        case ImPlotMarker_Down :     return "Down";
//        case ImPlotMarker_Left :     return "Left";
//        case ImPlotMarker_Right :    return "Right";
//        case ImPlotMarker_Cross :    return "Cross";
//        case ImPlotMarker_Plus :     return "Plus";
//        case ImPlotMarker_Asterisk : return "Asterisk";
//        default:                    return "";
//    }
//}

val PlotCol.autoColor: Vec4
    get() {
        val col = Vec4(0, 0, 0, 1)
        return when (this) {
            PlotCol.Line -> col // these are plot dependent!
            PlotCol.Fill -> col // these are plot dependent!
            PlotCol.MarkerOutline -> col // these are plot dependent!
            PlotCol.MarkerFill -> col // these are plot dependent!
            PlotCol.ErrorBar -> ImGui.getStyleColorVec4(Col.Text)
            PlotCol.FrameBg -> ImGui.getStyleColorVec4(Col.FrameBg)
            PlotCol.PlotBg -> ImGui.getStyleColorVec4(Col.WindowBg)
            PlotCol.PlotBorder -> ImGui.getStyleColorVec4(Col.Border)
            PlotCol.LegendBg -> ImGui.getStyleColorVec4(Col.PopupBg)
            PlotCol.LegendBorder -> PlotCol.PlotBorder.vec4
            PlotCol.LegendText -> PlotCol.InlayText.vec4
            PlotCol.TitleText -> ImGui.getStyleColorVec4(Col.Text)
            PlotCol.InlayText -> ImGui.getStyleColorVec4(Col.Text)
            PlotCol.AxisText -> ImGui.getStyleColorVec4(Col.Text)
            PlotCol.AxisGrid -> PlotCol.AxisText.vec4 * Vec4(1, 1, 1, 0.25f)
            PlotCol.AxisTick -> PlotCol.AxisGrid.vec4
            PlotCol.AxisBg -> Vec4(0, 0, 0, 0)
            PlotCol.AxisBgHovered -> ImGui.getStyleColorVec4(Col.ButtonHovered)
            PlotCol.AxisBgActive -> ImGui.getStyleColorVec4(Col.ButtonActive)
            PlotCol.Selection -> Vec4(1, 1, 0, 1)
            PlotCol.Crosshairs -> PlotCol.PlotBorder.vec4
            else -> col
        }
    }

//class PlotStyleVarInfo(val type: DataType,
//                       val count: Int)
//    ImU32 Offset
//    void * GetVarPtr(ImPlotStyle * style) const { return (void *)((unsigned char *) style +Offset); }


//val gPlotStyleVarInfo = arrayOf(PlotStyleVarInfo(DataType.Float, 1), // ImPlotStyleVar_LineWeight
//                                PlotStyleVarInfo(DataType.Int, 1), // ImPlotStyleVar_Marker
//                                PlotStyleVarInfo(DataType.Float, 1), // ImPlotStyleVar_MarkerSize
//                                PlotStyleVarInfo(DataType.Float, 1), // ImPlotStyleVar_MarkerWeight
//                                PlotStyleVarInfo(DataType.Float, 1), // ImPlotStyleVar_FillAlpha
//                                PlotStyleVarInfo(DataType.Float, 1), // ImPlotStyleVar_ErrorBarSize
//                                PlotStyleVarInfo(DataType.Float, 1), // ImPlotStyleVar_ErrorBarWeight
//                                PlotStyleVarInfo(DataType.Float, 1), // ImPlotStyleVar_DigitalBitHeight
//                                PlotStyleVarInfo(DataType.Float, 1), // ImPlotStyleVar_DigitalBitGap
//
//                                PlotStyleVarInfo(DataType.Float, 1), // ImPlotStyleVar_PlotBorderSize
//                                PlotStyleVarInfo(DataType.Float, 1), // ImPlotStyleVar_MinorAlpha
//                                PlotStyleVarInfo(DataType.Float, 2), // ImPlotStyleVar_MajorTickLen
//                                PlotStyleVarInfo(DataType.Float, 2), // ImPlotStyleVar_MinorTickLen
//                                PlotStyleVarInfo(DataType.Float, 2), // ImPlotStyleVar_MajorTickSize
//                                PlotStyleVarInfo(DataType.Float, 2), // ImPlotStyleVar_MinorTickSize
//                                PlotStyleVarInfo(DataType.Float, 2), // ImPlotStyleVar_MajorGridSize
//                                PlotStyleVarInfo(DataType.Float, 2), // ImPlotStyleVar_MinorGridSize
//                                PlotStyleVarInfo(DataType.Float, 2), // ImPlotStyleVar_PlotPadding
//                                PlotStyleVarInfo(DataType.Float, 2), // ImPlotStyleVar_LabelPaddine
//                                PlotStyleVarInfo(DataType.Float, 2), // ImPlotStyleVar_LegendPadding
//                                PlotStyleVarInfo(DataType.Float, 2), // ImPlotStyleVar_LegendInnerPadding
//                                PlotStyleVarInfo(DataType.Float, 2), // ImPlotStyleVar_LegendSpacing
//
//                                PlotStyleVarInfo(DataType.Float, 2), // ImPlotStyleVar_MousePosPadding
//                                PlotStyleVarInfo(DataType.Float, 2), // ImPlotStyleVar_AnnotationPadding
//                                PlotStyleVarInfo(DataType.Float, 2), // ImPlotStyleVar_FitPadding
//                                PlotStyleVarInfo(DataType.Float, 2), // ImPlotStyleVar_PlotDefaultSize
//                                PlotStyleVarInfo(DataType.Float, 2))  // ImPlotStyleVar_PlotMinSize

//fun getPlotStyleVarInfo(idx: PlotStyleVar): PlotStyleVarInfo {
//    assert(idx.i >= 0 && idx.i < PlotStyleVar.COUNT)
//    assert(gPlotStyleVarInfo.size == PlotStyleVar.COUNT)
//    return gPlotStyleVarInfo[idx.i]
//}