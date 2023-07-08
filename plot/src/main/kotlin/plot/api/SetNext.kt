package plot.api

import imgui.Cond
import plot.internalApi.gImPlot
import kotlin.reflect.KMutableProperty0

//-----------------------------------------------------------------------------
// [SECTION] SetNext
//-----------------------------------------------------------------------------

// Though you should default to the `Setup` API above, there are some scenarios
// where (re)configuring a plot or axis before `BeginPlot` is needed (e.g. if
// using a preceding button or slider widget to change the plot limits). In
// this case, you can use the `SetNext` API below. While this is not as feature
// rich as the Setup API, most common needs are provided. These functions can be
// called anwhere except for inside of `Begin/EndPlot`. For example:

// if (ImGui::Button("Center Plot"))
//     ImPlot::SetNextPlotLimits(-1,1,-1,1);
// if (ImPlot::BeginPlot(...)) {
//     ...
//     ImPlot::EndPlot();
// }
//
// Important notes:
//
// - You must still enable non-default axes with SetupAxis for these functions
//   to work properly.

// Sets an upcoming axis range limits. If ImPlotCond_Always is used, the axes limits will be locked.
fun setNextAxisLimits(axis: Axis, vMin: Double, vMax: Double, cond: PlotCond = Cond.Once) {
    val gp = gImPlot
    assert(gp.currentPlot == null) { "SetNextAxisLimits() needs to be called before BeginPlot()!" }
//    IM_ASSERT(cond == 0 || ImIsPowerOfTwo(cond)) // Make sure the user doesn't attempt to combine multiple condition flags.
    gp.nextPlotData.hasRange[axis.i] = true
    gp.nextPlotData.rangeCond[axis.i] = cond
    gp.nextPlotData.range[axis].min = vMin
    gp.nextPlotData.range[axis].max = vMax
}

// Links an upcoming axis range limits to external values. Set to nullptr for no linkage. The pointer data must remain valid until EndPlot!
fun setNextAxisLinks(axis: Axis, linkMin: KMutableProperty0<Double>?, linkMax: KMutableProperty0<Double>?) {
    val gp = gImPlot
    assert(gp.currentPlot == null) { "SetNextAxisLinks() needs to be called before BeginPlot()!" }
    gp.nextPlotData.linkedMin[axis.i] = linkMin
    gp.nextPlotData.linkedMax[axis.i] = linkMax
}

// Set an upcoming axis to auto fit to its data.
fun setNextAxisToFit(axis: Axis) {
    val gp = gImPlot
    assert(gp.currentPlot == null) { "SetNextAxisToFit() needs to be called before BeginPlot()!" }
    gp.nextPlotData.fit[axis.i] = true
}

// Sets the upcoming primary X and Y axes range limits. If ImPlotCond_Always is used, the axes limits will be locked (shorthand for two calls to SetupAxisLimits).
fun setNextAxesLimits(xMin: Double, xMax: Double, yMin: Double, yMax: Double, cond: PlotCond = Cond.Once) {
    setNextAxisLimits(Axis.X1, xMin, xMax, cond)
    setNextAxisLimits(Axis.Y1, yMin, yMax, cond)
}
// Sets all upcoming axes to auto fit to their data.
fun setNextAxesToFit() {
    for (i in Axis.values())
        setNextAxisToFit(i)
}