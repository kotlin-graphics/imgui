@file:OptIn(ExperimentalStdlibApi::class)

package plot.internalApi

import glm_.f
import imgui.ImGui
import imgui.mutableReference
import plot.api.*

//-----------------------------------------------------------------------------
// [SECTION] Axis Utils
//-----------------------------------------------------------------------------

// Returns true if any enabled axis is locked from user input.
//static inline bool AnyAxesInputLocked(ImPlotAxis* axes, int count) {
//    for (int i = 0; i < count; ++i) {
//        if (axes[i].Enabled && axes[i].IsInputLocked())
//            return true
//    }
//    return false
//}

// Returns true if all enabled axes are locked from user input.
fun allAxesInputLocked(axes: Array<PlotAxis>, idx: Axis, count: Int): Boolean {
    for (i in 0 ..< count) {
        if (axes[idx.i + i].enabled && !axes[idx.i + i].isInputLocked)
            return false
    }
    return true
}

fun anyAxesHeld(axes: Array<PlotAxis>, idx: Axis, count: Int): Boolean {
    for (i in 0 ..< count) {
        if (axes[idx.i + i].enabled && axes[idx.i + i].held)
            return true
    }
    return false
}

fun anyAxesHovered(axes: Array<PlotAxis>, idx: Axis, count: Int): Boolean {
    for (i in 0 ..< count) {
        if (axes[idx.i + i].enabled && axes[idx.i + i].hovered)
            return true
    }
    return false
}

// Returns true if the user has requested data to be fit.
fun fitThisFrame(): Boolean = gImPlot.currentPlot!!.fitThisFrame

// Extends the current plot's axes so that it encompasses a vertical line at x
fun fitPointX(x: Double) {
    val plot = currentPlot!!
    val xAxis = plot.axes[plot.currentX]
    xAxis extendFit x
}

// Extends the current plot's axes so that it encompasses a horizontal line at y
fun fitPointY(y: Double) {
    val plot = currentPlot!!
    val yAxis = plot.axes[plot.currentY]
    yAxis extendFit y
}

// Extends the current plot's axes so that it encompasses point p
fun fitPoint(p: PlotPoint) {
    val plot = currentPlot!!
    val xAxis = plot.axes[plot.currentX]
    val yAxis = plot.axes[plot.currentY]
    xAxis.extendFitWith(yAxis, p.x, p.y)
    yAxis.extendFitWith(xAxis, p.y, p.x)
}

// Returns true if two ranges overlap
infix fun PlotRange.rangesOverlap(p: PlotRange): Boolean = min <= p.max && p.min <= max

// Shows an axis's context menu.
fun showAxisContextMenu(axis: PlotAxis, equalAxis: PlotAxis?, timeAllowed: Boolean = false) {

    ImGui.pushItemWidth(75)
    val alwaysLocked = axis.isRangeLocked || axis.isAutoFitting
    val label = axis.hasLabel
    val grid = axis.hasGridLines
    val ticks = axis.hasTickMarks
    val labels = axis.hasTickLabels
    val dragSpeed = if (axis.range.size <= Double.MIN_VALUE) Double.MIN_VALUE * 1.0e+13 else 0.01 * axis.range.size // recover from almost equal axis limits.

    if (axis.scale == PlotScale.Time) {
        var tMin = PlotTime fromDouble axis.range.min
        var tMax = PlotTime fromDouble axis.range.max

        beginDisabledControls(alwaysLocked)
        ImGui.checkboxFlags("##LockMin", axis::flags, PlotAxisFlag.LockMin)
        endDisabledControls(alwaysLocked)
        ImGui.sameLine()
        beginDisabledControls(axis.isLockedMin || alwaysLocked)
        if (ImGui.beginMenu("Min Time")) {
            if (showTimePicker("mintime", tMin)) {
                if (tMin >= tMax)
                    tMax = addTime(tMin, TimeUnit.S, 1)
                axis.setRange(tMin.toDouble, tMax.toDouble)
            }
            ImGui.separator()
            if (showDatePicker("mindate", axis::pickerLevel, axis.pickerTimeMin, tMin, tMax)) {
                tMin = combineDateTime(axis.pickerTimeMin, tMin)
                if (tMin >= tMax)
                    tMax = addTime(tMin, TimeUnit.S, 1)
                axis.setRange(tMin.toDouble, tMax.toDouble)
            }
            ImGui.endMenu()
        }
        endDisabledControls(axis.isLockedMin || alwaysLocked)

        beginDisabledControls(alwaysLocked)
        ImGui.checkboxFlags("##LockMax", axis::flags, PlotAxisFlag.LockMax)
        endDisabledControls(alwaysLocked)
        ImGui.sameLine()
        beginDisabledControls(axis.isLockedMax || alwaysLocked)
        if (ImGui.beginMenu("Max Time")) {
            if (showTimePicker("maxtime", tMax)) {
                if (tMax <= tMin)
                    tMin = addTime(tMax, TimeUnit.S, -1)
                axis.setRange(tMin.toDouble, tMax.toDouble)
            }
            ImGui.separator()
            if (showDatePicker("maxdate", axis::pickerLevel, axis.pickerTimeMax, tMin, tMax)) {
                tMax = combineDateTime(axis.pickerTimeMax, tMax)
                if (tMax <= tMin)
                    tMin = addTime(tMax, TimeUnit.S, -1)
                axis.setRange(tMin.toDouble, tMax.toDouble)
            }
            ImGui.endMenu()
        }
        endDisabledControls(axis.isLockedMax || alwaysLocked)
    } else {
        beginDisabledControls(alwaysLocked)
        ImGui.checkboxFlags("##LockMin", axis::flags, PlotAxisFlag.LockMin)
        endDisabledControls(alwaysLocked)
        ImGui.sameLine()
        beginDisabledControls(axis.isLockedMin || alwaysLocked)
        val tempMin = axis.range.min
        if (dragFloat("Min", tempMin.mutableReference, dragSpeed.f, -Double.MAX_VALUE, axis.range.max - Double.MIN_VALUE)) {
            axis.setMin(tempMin, true)
            equalAxis?.setAspect(axis.aspect)
        }
        endDisabledControls(axis.isLockedMin || alwaysLocked)

        beginDisabledControls(alwaysLocked)
        ImGui.checkboxFlags("##LockMax", axis::flags, PlotAxisFlag.LockMax)
        endDisabledControls(alwaysLocked)
        ImGui.sameLine()
        beginDisabledControls(axis.isLockedMax || alwaysLocked)
        val tempMax = axis.range.max
        if (dragFloat("Max", tempMax.mutableReference, dragSpeed.f, axis.range.min + Double.MIN_VALUE, Double.MAX_VALUE)) {
            axis.setMax(tempMax, true)
            equalAxis?.setAspect(axis.aspect)
        }
        endDisabledControls(axis.isLockedMax || alwaysLocked)
    }

    ImGui.separator()

    ImGui.checkboxFlags("Auto-Fit", axis::flags, PlotAxisFlag.AutoFit)
    // TODO
    // BeginDisabledControls(axis.IsTime() && time_allowed);
    // ImGui::CheckboxFlags("Log Scale",(unsigned int*)&axis.Flags, ImPlotAxisFlags_LogScale);
    // EndDisabledControls(axis.IsTime() && time_allowed);
    // if (time_allowed) {
    //     BeginDisabledControls(axis.IsLog() || axis.IsSymLog());
    //     ImGui::CheckboxFlags("Time",(unsigned int*)&axis.Flags, ImPlotAxisFlags_Time);
    //     EndDisabledControls(axis.IsLog() || axis.IsSymLog());
    // }
    ImGui.separator()
    ImGui.checkboxFlags("Invert", axis::flags, PlotAxisFlag.Invert)
    ImGui.checkboxFlags("Opposite", axis::flags, PlotAxisFlag.Opposite)
    ImGui.separator()
    beginDisabledControls(axis.labelOffset == -1)
    if (ImGui.checkbox("Label", label.mutableReference))
        axis::flags flip PlotAxisFlag.NoLabel
    endDisabledControls(axis.labelOffset == -1)
    if (ImGui.checkbox("Grid Lines", grid.mutableReference))
        axis::flags flip PlotAxisFlag.NoGridLines
    if (ImGui.checkbox("Tick Marks", ticks.mutableReference))
        axis::flags flip PlotAxisFlag.NoTickMarks
    if (ImGui.checkbox("Tick Labels", labels.mutableReference))
        axis::flags flip PlotAxisFlag.NoTickLabels

}