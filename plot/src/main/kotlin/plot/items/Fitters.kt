@file:OptIn(ExperimentalStdlibApi::class)

package plot.items

import plot.api.Getter
import plot.internalApi.PlotAxis

//-----------------------------------------------------------------------------
// [SECTION] Fitters
//-----------------------------------------------------------------------------

interface Fitter {
    fun fit(xAxis: PlotAxis, yAxis: PlotAxis)
}

//template <typename _Getter1>
class Fitter1(val getter: Getter): Fitter {
    override fun fit(xAxis: PlotAxis, yAxis: PlotAxis) {
        for (i in 0..<getter.count) {
            val p = getter[i]
            xAxis.extendFitWith(yAxis, p.x, p.y)
            yAxis.extendFitWith(xAxis, p.y, p.x)
        }
    }
}

//template <typename _Getter1>
//struct FitterX {
//    FitterX(const _Getter1& getter) : Getter(getter) { }
//    void Fit(ImPlotAxis& x_axis, ImPlotAxis&) const {
//        for (int i = 0; i < Getter.Count; ++i) {
//        ImPlotPoint p = Getter(i);
//        x_axis.ExtendFit(p.x);
//    }
//    }
//    const _Getter1& Getter;
//};
//
//template <typename _Getter1>
//struct FitterY {
//    FitterY(const _Getter1& getter) : Getter(getter) { }
//    void Fit(ImPlotAxis&, ImPlotAxis& y_axis) const {
//        for (int i = 0; i < Getter.Count; ++i) {
//        ImPlotPoint p = Getter(i);
//        y_axis.ExtendFit(p.y);
//    }
//    }
//    const _Getter1& Getter;
//};
//
//template <typename _Getter1, typename _Getter2>
//struct Fitter2 {
//    Fitter2(const _Getter1& getter1, const _Getter2& getter2) : Getter1(getter1), Getter2(getter2) { }
//    void Fit(ImPlotAxis& x_axis, ImPlotAxis& y_axis) const {
//        for (int i = 0; i < Getter1.Count; ++i) {
//        ImPlotPoint p = Getter1(i);
//        x_axis.ExtendFitWith(y_axis, p.x, p.y);
//        y_axis.ExtendFitWith(x_axis, p.y, p.x);
//    }
//        for (int i = 0; i < Getter2.Count; ++i) {
//        ImPlotPoint p = Getter2(i);
//        x_axis.ExtendFitWith(y_axis, p.x, p.y);
//        y_axis.ExtendFitWith(x_axis, p.y, p.x);
//    }
//    }
//    const _Getter1& Getter1;
//    const _Getter2& Getter2;
//};
//
//template <typename _Getter1, typename _Getter2>
//struct FitterBarV {
//    FitterBarV(const _Getter1& getter1, const _Getter2& getter2, double width) :
//    Getter1(getter1),
//    Getter2(getter2),
//    HalfWidth(width*0.5)
//    { }
//    void Fit(ImPlotAxis& x_axis, ImPlotAxis& y_axis) const {
//        int count = ImMin(Getter1.Count, Getter2.Count);
//        for (int i = 0; i < count; ++i) {
//        ImPlotPoint p1 = Getter1(i); p1.x -= HalfWidth;
//        ImPlotPoint p2 = Getter2(i); p2.x += HalfWidth;
//        x_axis.ExtendFitWith(y_axis, p1.x, p1.y);
//        y_axis.ExtendFitWith(x_axis, p1.y, p1.x);
//        x_axis.ExtendFitWith(y_axis, p2.x, p2.y);
//        y_axis.ExtendFitWith(x_axis, p2.y, p2.x);
//    }
//    }
//    const _Getter1& Getter1;
//    const _Getter2& Getter2;
//    const double    HalfWidth;
//};
//
//template <typename _Getter1, typename _Getter2>
//struct FitterBarH {
//    FitterBarH(const _Getter1& getter1, const _Getter2& getter2, double height) :
//    Getter1(getter1),
//    Getter2(getter2),
//    HalfHeight(height*0.5)
//    { }
//    void Fit(ImPlotAxis& x_axis, ImPlotAxis& y_axis) const {
//        int count = ImMin(Getter1.Count, Getter2.Count);
//        for (int i = 0; i < count; ++i) {
//        ImPlotPoint p1 = Getter1(i); p1.y -= HalfHeight;
//        ImPlotPoint p2 = Getter2(i); p2.y += HalfHeight;
//        x_axis.ExtendFitWith(y_axis, p1.x, p1.y);
//        y_axis.ExtendFitWith(x_axis, p1.y, p1.x);
//        x_axis.ExtendFitWith(y_axis, p2.x, p2.y);
//        y_axis.ExtendFitWith(x_axis, p2.y, p2.x);
//    }
//    }
//    const _Getter1& Getter1;
//    const _Getter2& Getter2;
//    const double    HalfHeight;
//};
//
//struct FitterRect {
//    FitterRect(const ImPlotPoint& pmin, const ImPlotPoint& pmax) :
//    Pmin(pmin),
//    Pmax(pmax)
//    { }
//    FitterRect(const ImPlotRect& rect) :
//    FitterRect(rect.Min(), rect.Max())
//    { }
//    void Fit(ImPlotAxis& x_axis, ImPlotAxis& y_axis) const {
//        x_axis.ExtendFitWith(y_axis, Pmin.x, Pmin.y);
//        y_axis.ExtendFitWith(x_axis, Pmin.y, Pmin.x);
//        x_axis.ExtendFitWith(y_axis, Pmax.x, Pmax.y);
//        y_axis.ExtendFitWith(x_axis, Pmax.y, Pmax.x);
//    }
//    const ImPlotPoint Pmin;
//    const ImPlotPoint Pmax;
//};