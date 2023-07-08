package plot.api

//-----------------------------------------------------------------------------
// [SECTION] Callbacks
//-----------------------------------------------------------------------------

// Callback signature for axis tick label formatter.
typealias PlotFormatter = (value: Double, userData: Any?) -> String

// Callback signature for data getter.
typealias PlotGetter = (idx: Int, userData: Any?) -> PlotPoint

// Callback signature for axis transform.
typealias PlotTransform = (value: Double, userData: Any?) -> Double