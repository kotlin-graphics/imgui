package engine.context

import IMGUI_HAS_TABLE
import imgui.*
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endGroup
import imgui.ImGui.endTabBar
import imgui.ImGui.popID
import imgui.ImGui.treePop
import engine.core.TestRunFlag
import engine.core.TestStatus
import imgui.WindowFlag as Wf

// Main control
fun TestContext.finish() {
    if (runFlags has TestRunFlag.NoTestFunc.i)
        return
    val test = this.test!!
    if (test.status == TestStatus.Running)
        test.status = TestStatus.Success
}

val TestContext.isError: Boolean
    get() = test!!.status == TestStatus.Error || abort
val TestContext.isFirstFrame: Boolean
    get() = frameCount == firstFrameCount

fun TestContext.setGuiFuncEnabled(v: Boolean) {
    runFlags = when {
        v -> runFlags wo TestRunFlag.NoGuiFunc
        else -> runFlags or TestRunFlag.NoGuiFunc
    }
}

// FIXME-ERRORHANDLING: Can't recover from inside BeginTabItem/EndTabItem yet.
// FIXME-ERRORHANDLING: Can't recover from interleaved BeginTabBar/Begin
// FIXME-ERRORHANDLING: Once this function is amazingly sturdy, we should make it a ImGui:: function.. See #1651
// FIXME-ERRORHANDLING: This is flawed as we are not necessarily End/Popping things in the right order.
fun TestContext.recoverFromUiContextErrors() {

    val g = uiContext!!
    val test = test!!

    // If we are _already_ in a test error state, recovering is normal so we'll hide the log.
    val verbose = test.status != TestStatus.Error

    while (g.currentWindowStack.size > 1) {
        if (IMGUI_HAS_TABLE) {
//                val table = g.currentTable
//                if (table && (table->OuterWindow == g.CurrentWindow || table->InnerWindow == g.CurrentWindow))
//                {
//                    if (verbose) LogWarning("Recovered from missing EndTable() call.")
//                    ImGui::EndTable()
//                }
        }

        while (g.currentTabBar != null) {
            if (verbose) logWarning("Recovered from missing EndTabBar() call.")
            endTabBar()
        }

        val win = g.currentWindow!!

        while (win.dc.treeDepth > 0) {
            if (verbose) logWarning("Recovered from missing TreePop() call.")
            treePop()
        }

        while (win.dc.groupStack.size > win.dc.stackSizesBackup[1]) {
            if (verbose) logWarning("Recovered from missing EndGroup() call.")
            endGroup()
        }

        while (win.idStack.size > win.dc.stackSizesBackup[0]) {
            if (verbose) logWarning("Recovered from missing PopID() call.")
            popID()
        }

        if (win.flags has Wf._ChildWindow) {
            if (verbose) logWarning("Recovered from missing EndChild() call.")
            endChild()
        } else {
            if (verbose) logWarning("Recovered from missing End() call.")
            end()
        }
    }
}

fun <T> TestContext.getUserData(): T {
    assert(userData != null)
    return userData as T
} // FIXME: Assert to compare sizes