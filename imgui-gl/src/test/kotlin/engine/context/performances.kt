package engine.context

import engine.BuildInfo
import engine.core.TestRunFlag
import engine.core.perfDeltaTime500Average
import java.io.File

//-------------------------------------------------------------------------
// ImGuiTestContext - Performance Tools
//-------------------------------------------------------------------------

// Calculate the reference DeltaTime, averaged over 500 frames, with GuiFunc disabled.
fun TestContext.perfCalcRef() {

    logDebug("Measuring ref dt...")
    setGuiFuncEnabled(false)
    var n = 0
    while (n < 500 && !abort) {
        yield()
        n++
    }
    perfRefDt = engine!!.perfDeltaTime500Average
    setGuiFuncEnabled(true)
}

fun TestContext.perfCapture() {

    // Calculate reference average DeltaTime if it wasn't explicitly called by TestFunc
    if (perfRefDt < 0.0)
        perfCalcRef()
    assert(perfRefDt >= 0.0)

    // Yield for the average to stabilize
    logDebug("Measuring gui dt...")
    var n = 0
    while (n++ < 500 && !abort)
        yield()
    if (abort)
        return

    val dtCurr = engine!!.perfDeltaTime500Average
    val dtRefMs = perfRefDt * 1000
    val dtDeltaMs = (dtCurr - perfRefDt) * 1000

    val buildInfo = BuildInfo()

    // Display results
    // FIXME-TESTS: Would be nice if we could submit a custom marker (e.g. branch/feature name)
    logInfo("[PERF] Conditions: Stress x$perfStressAmount, $buildInfo")
    logInfo("[PERF] Result: %+6.3f ms (from ref %+6.3f)", dtDeltaMs, dtRefMs)

    // Log to .csv
    val file = File("imgui_perflog.csv")
    file.createNewFile()
    if (file.exists() && file.canWrite()) {
        val test = test!!
        val text = "${test.category},${test.name},%.3f,x%d,${engineIO!!.perfAnnotation},${buildInfo.type},${buildInfo.cpu},${buildInfo.os},${buildInfo.compiler},${buildInfo.date}\n".format(dtDeltaMs, perfStressAmount)
        file.appendText(text)
    } else
        logError("Failed to log to CSV file!")

    // Disable the "Success" message
    runFlags = runFlags or TestRunFlag.NoSuccessMsg
}