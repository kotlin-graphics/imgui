package imgui.test.engine.context

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
    perfRefDt = engine!!.getPerfDeltaTime500Average(Engine)
    SetGuiFuncEnabled(true)
}
void PerfCapture ()