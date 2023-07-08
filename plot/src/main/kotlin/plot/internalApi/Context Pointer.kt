package plot.internalApi

//-----------------------------------------------------------------------------
// [SECTION] Context Pointer
//-----------------------------------------------------------------------------

//#ifndef GImPlot
var gImPlot: PlotContext // Current implicit context pointer
    get() = gImPlotNullable!!
    set(value) {
        gImPlotNullable = value
    }
//#endif

var gImPlotNullable: PlotContext? = null