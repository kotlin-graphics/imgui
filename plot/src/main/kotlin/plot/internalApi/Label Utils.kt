package plot.internalApi

import plot.api.formatDateTime
import plot.api.getDateTimeFmt
import plot.api.getUnitForRange
import plot.api.timeFormatMouseCursor

//-----------------------------------------------------------------------------
// [SECTION] Label Utils
//-----------------------------------------------------------------------------

fun axisPrecision(axis: PlotAxis): Int {
    val range = if(axis.ticker.tickCount > 1) (axis.ticker.ticks[1].plotPos - axis.ticker.ticks[0].plotPos) else axis.range.size
    return precision(range)
}

fun roundAxisValue(axis: PlotAxis, value: Double): Double = roundTo(value, axisPrecision(axis))

// Create a a string label for a an axis value
fun labelAxisValue(axis: PlotAxis, value_: Double, round: Boolean = false): String {
    var value = value_
    val gp = gImPlot
    // TODO: We shouldn't explicitly check that the axis is Time here. Ideally,
    // Formatter_Time would handle the formatting for us, but the code below
    // needs additional arguments which are not currently available in ImPlotFormatter
    return when (axis.locator) {
        ::locator_Time -> {
            val unit = when {
                axis.vertical -> getUnitForRange(axis.range.size / (gp.currentPlot!!.plotRect.height / 100)) // TODO: magic value!
                else -> getUnitForRange(axis.range.size / (gp.currentPlot!!.plotRect.width / 100)) // TODO: magic value!
            }
            formatDateTime(PlotTime fromDouble value, getDateTimeFmt(timeFormatMouseCursor, unit))
        }
        else -> {
            if (round)
                value = roundAxisValue(axis, value)
            axis.formatter!!(value, axis.formatterData)
        }
    }
}