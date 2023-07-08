package plot.internalApi

import plot.api.PlotFormatter
import plot.api.PlotRange

//-----------------------------------------------------------------------------
// [SECTION] Callbacks
//-----------------------------------------------------------------------------

typealias PlotLocator = (ticker: PlotTicker, range: PlotRange, pixels: Float, vertical: Boolean, formatter: PlotFormatter, formatterData: Any?) -> Unit