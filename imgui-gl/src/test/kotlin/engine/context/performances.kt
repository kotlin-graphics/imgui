package engine.context

import engine.core.perfDeltaTime500Average

//-------------------------------------------------------------------------
// ImGuiTestContext - Performance Tools
//-------------------------------------------------------------------------

// Calculate the reference DeltaTime, averaged over 500 frames, with GuiFunc disabled.
fun TestContext.perfCalcRef () {

    logDebug("Measuring ref dt...")
    setGuiFuncEnabled(false)
    var n = 0
    while (n < 500 && !abort) {
        yield()
        n++
    }
    perfRefDt = engine!!.perfDeltaTime500Average
    SetGuiFuncEnabled(true)
}

fun TestContext.perfCapture () {

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
    val dt_delta_ms = (dtCurr - PerfRefDt) * 1000

    const ImBuildInfo& build_info = ImGetBuildInfo()

    // Display results
    // FIXME-TESTS: Would be nice if we could submit a custom marker (e.g. branch/feature name)
    LogInfo("[PERF] Conditions: Stress x%d, %s, %s, %s, %s, %s",
            PerfStressAmount, build_info.Type, build_info.Cpu, build_info.OS, build_info.Compiler, build_info.Date)
    LogInfo("[PERF] Result: %+6.3f ms (from ref %+6.3f)", dt_delta_ms, dtRefMs)

    // Log to .csv
    FILE* f = fopen("imgui_perflog.csv", "a+t")
    if (f == NULL)
    {
        LogError("Failed to log to CSV file!")
    }
    else
    {
        fprintf(f,
                "%s,%s,%.3f,x%d,%s,%s,%s,%s,%s,%s\n",
                Test->Category, Test->Name, dt_delta_ms,
        PerfStressAmount, EngineIO->PerfAnnotation, build_info.Type, build_info.Cpu, build_info.OS, build_info.Compiler, build_info.Date)
        fflush(f)
        fclose(f)
    }

    // Disable the "Success" message
    RunFlags |= ImGuiTestRunFlags_NoSuccessMsg
}