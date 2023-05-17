package imgui.api

import imgui.Viewport

// Viewports
// - Currently represents the Platform Window created by the application which is hosting our Dear ImGui windows.
// - In 'docking' branch with multi-viewport enabled, we extend this concept to have multiple active viewports.
// - In the future we will extend this concept further to also represent Platform Monitor and support a "no main platform window" operation mode.
interface viewports {

    /** return primary/default viewport. This can never be NULL. */
    val mainViewport: Viewport
        get() = g.viewports[0]
}