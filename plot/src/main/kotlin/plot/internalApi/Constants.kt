package plot.internalApi

//-----------------------------------------------------------------------------
// [SECTION] Constants
//-----------------------------------------------------------------------------

// Constants can be changed unless stated otherwise. We may move some of these
// to ImPlotStyleVar_ over time.

// Mimimum allowable timestamp value 01/01/1970 @ 12:00am (UTC) (DO NOT DECREASE THIS)
const val PLOT_MIN_TIME = 0.0
// Maximum allowable timestamp value 01/01/3000 @ 12:00am (UTC) (DO NOT INCREASE THIS)
const val PLOT_MAX_TIME = 32503680000.0
// Default label format for axis labels
const val PLOT_LABEL_FORMAT = "%g"
// Max character size for tick labels
const val PLOT_LABEL_MAX_SIZE = 32