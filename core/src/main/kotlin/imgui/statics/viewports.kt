package imgui.static

import imgui.ViewportFlag
import imgui.api.g
import imgui.or

// Viewports

/** Update viewports and monitor infos */
fun updateViewportsNewFrame() {

    assert(g.viewports.size == 1)

    // Update main viewport with current platform position.
    // FIXME-VIEWPORT: Size is driven by backend/user code for backward-compatibility but we should aim to make this more consistent.
    g.viewports[0].apply {
        flags = ViewportFlag.IsPlatformWindow or ViewportFlag.OwnedByApp
        pos put 0f
        size put g.io.displaySize
    }
    for (viewport in g.viewports)
        viewport.apply {
            // Lock down space taken by menu bars and status bars, reset the offset for fucntions like BeginMainMenuBar() to alter them again.
            workOffsetMin put buildWorkOffsetMin
            workOffsetMax put buildWorkOffsetMax
            buildWorkOffsetMin put 0f
            buildWorkOffsetMax put 0f
            updateWorkRect()
        }
}