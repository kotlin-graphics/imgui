package plot.internalApi

import plot.api.setupFinish

//-----------------------------------------------------------------------------
// [SECTION] Setup Utils
//-----------------------------------------------------------------------------

// Lock Setup and call SetupFinish if necessary.
fun setupLock() {
    val gp = gImPlot
    if (!gp.currentPlot!!.setupLocked)
        setupFinish()
    gp.currentPlot!!.setupLocked = true
}