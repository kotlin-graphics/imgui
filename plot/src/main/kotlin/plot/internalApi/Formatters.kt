package plot.internalApi

import plot.api.PlotFormatter
import plot.api.formatDateTime

//-----------------------------------------------------------------------------
// [SECTION] Formatters
//-----------------------------------------------------------------------------

fun formatter_Default(value: Double, data: Any?): String {
    val fmt = data as String
    return fmt.format(value)
}

fun formatter_Logit(value: Double, data: Any?): String = when {
    value == 0.5 -> "1/2"
    value < 0.5 -> "%g".format(value)
    else -> "1 - %g".format(1 - value)
}

class Formatter_Time_Data {
    var time = PlotTime()
    var spec = PlotDateTimeSpec()
    var userFormatter: PlotFormatter? = null
    var userFormatterData: Any? = null
}

fun formatter_Time(value: Double, data: Any?): String {
    val ftd = data as Formatter_Time_Data
    return formatDateTime(ftd.time, ftd.spec)
}