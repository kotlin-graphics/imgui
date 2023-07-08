package plot.items

import imgui.Flags
import imgui.ImGui
import imgui.has
import imgui.u32
import plot.api.*
import plot.internalApi.beginItemEx
import plot.internalApi.itemData

//-----------------------------------------------------------------------------
// [SECTION] PlotLine
//-----------------------------------------------------------------------------

//template <typename _Getter>
fun plotLineEx(label_id: String, getter: Getter, flags: PlotLineFlags) {
    if (beginItemEx(label_id, Fitter1(getter), Flags<PlotItemFlag>(flags.i), PlotCol.Line)) {
        val s = itemData
        if (getter.count > 1) {
            if (flags has PlotLineFlag.Shaded && s.renderFill) {
                val colFill = s.colors[PlotCol.Fill].u32.toUInt()
                val getter2 = GetterOverrideY(getter, 0.0)
                renderPrimitives2(RendererShaded(getter, getter2, colFill))
            }
            if (s.renderLine) {
                val colLine = s.colors[PlotCol.Line].u32.toUInt()
                if (flags has PlotLineFlag.Segments)
                    renderPrimitives1(RendererLineSegments1(getter, colLine, s.lineWeight))
                else if (flags has PlotLineFlag.Loop)
                    if (flags has PlotLineFlag.SkipNaN)
                        renderPrimitives1(RendererLineStripSkip(GetterLoop(getter), colLine, s.lineWeight))
                    else
                        renderPrimitives1(RendererLineStrip(GetterLoop(getter), colLine, s.lineWeight))
                else
                    if (flags has PlotLineFlag.SkipNaN)
                        renderPrimitives1(RendererLineStripSkip(getter, colLine, s.lineWeight))
                    else
                        renderPrimitives1(RendererLineStrip(getter, colLine, s.lineWeight))
            }
        }
        // render markers
        if (s.marker != PlotMarker.None) {
            if (flags has PlotLineFlag.NoClip) {
                popPlotClipRect()
                pushPlotClipRect(s.markerSize)
            }
            val colLine = s.colors[PlotCol.MarkerOutline].u32.toUInt()
            val colFill = s.colors[PlotCol.MarkerFill].u32.toUInt()
            renderMarkers<_Getter>(getter, s.Marker, s.MarkerSize, s.RenderMarkerFill, colFill, s.RenderMarkerLine, colLine, s.MarkerWeight)
        }
        EndItem()
    }
}

//template <typename T>
//void PlotLine(const char* label_id, const T* values, int count, double xscale, double x0, ImPlotLineFlags flags, int offset, int stride) {
//    GetterXY<IndexerLin,IndexerIdx<T>> getter(IndexerLin(xscale,x0),IndexerIdx<T>(values,count,offset,stride),count);
//    PlotLineEx(label_id, getter, flags);
//}
//
//template <typename T>
//void PlotLine(const char* label_id, const T* xs, const T* ys, int count, ImPlotLineFlags flags, int offset, int stride) {
//    GetterXY<IndexerIdx<T>,IndexerIdx<T>> getter(IndexerIdx<T>(xs,count,offset,stride),IndexerIdx<T>(ys,count,offset,stride),count);
//    PlotLineEx(label_id, getter, flags);
//}
//
//#define INSTANTIATE_MACRO(T) \
//template IMPLOT_API void PlotLine<T> (const char* label_id, const T* values, int count, double xscale, double x0, ImPlotLineFlags flags, int offset, int stride); \
//template IMPLOT_API void PlotLine<T>(const char* label_id, const T* xs, const T* ys, int count, ImPlotLineFlags flags, int offset, int stride);
//CALL_INSTANTIATE_FOR_NUMERIC_TYPES()
//#undef INSTANTIATE_MACRO
//
//// custom
//void PlotLineG(const char* label_id, ImPlotGetter getter_func, void* data, int count, ImPlotLineFlags flags) {
//    GetterFuncPtr getter(getter_func,data, count);
//    PlotLineEx(label_id, getter, flags);
//}