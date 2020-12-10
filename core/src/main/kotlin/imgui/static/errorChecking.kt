package imgui.static

import imgui.*
import imgui.ImGui.end
import imgui.ImGui.io
import imgui.ImGui.mergedKeyModFlags
import imgui.ImGui.style
import imgui.api.g
import imgui.internal.classes.Rect
import imgui.internal.classes.Window


//-----------------------------------------------------------------------------
// [SECTION] ERROR CHECKING
//-----------------------------------------------------------------------------

fun errorCheckNewFrameSanityChecks() {

    // Check user data
    // (We pass an error message in the assert expression to make it visible to programmers who are not using a debugger, as most assert handlers display their argument)
    assert(g.initialized)
    assert(io.deltaTime > 0f || g.frameCount == 0) { "Need a positive DeltaTime!" }
    assert(g.frameCount == 0 || g.frameCountEnded == g.frameCount) { "Forgot to call Render() or EndFrame() at the end of the previous frame?" }
    assert(io.displaySize.x >= 0f && io.displaySize.y >= 0f) { "Invalid DisplaySize value!" } // TODO glm
    assert(io.fonts.fonts.size > 0) { "Font Atlas not built. Did you call io.Fonts->GetTexDataAsRGBA32() / GetTexDataAsAlpha8()?" }
    assert(io.fonts.fonts[0].isLoaded) { "Font Atlas not built. Did you call io.Fonts->GetTexDataAsRGBA32() / GetTexDataAsAlpha8()?" }
    assert(style.curveTessellationTol > 0f) { "Invalid style setting!" }
    assert(style.circleSegmentMaxError > 0f) { "Invalid style setting!" }
    assert(style.alpha in 0f..1f) { "Invalid style setting!" } // Allows us to avoid a few clamps in color computations
    assert(style.windowMinSize.x >= 1f && style.windowMinSize.y >= 1f) { "Invalid style setting." } // TODO glm
    assert(style.windowMenuButtonPosition == Dir.None || style.windowMenuButtonPosition == Dir.Left || style.windowMenuButtonPosition == Dir.Right)
    for (n in 0 until Key.COUNT)
        assert(io.keyMap[n] >= -1 && io.keyMap[n] < io.keysDown.size) { "io.KeyMap[] contains an out of bound value (need to be 0..512, or -1 for unmapped key)" }

    // Check: required key mapping (we intentionally do NOT check all keys to not pressure user into setting up everything, but Space is required and was only added in 1.60 WIP)
    if (io.configFlags has ConfigFlag.NavEnableKeyboard)
        assert(io.keyMap[Key.Space.i] != -1) { "ImGuiKey_Space is not mapped, required for keyboard navigation." }

    // Check: the beta io.ConfigWindowsResizeFromEdges option requires backend to honor mouse cursor changes and set the ImGuiBackendFlags_HasMouseCursors flag accordingly.
    if (io.configWindowsResizeFromEdges && io.backendFlags hasnt BackendFlag.HasMouseCursors)
        io.configWindowsResizeFromEdges = false

    // Perform simple check: error if Docking or Viewport are enabled _exactly_ on frame 1 (instead of frame 0 or later), which is a common error leading to loss of .ini data.
    if (g.frameCount == 1 && io.configFlags has ConfigFlag.DockingEnable && g.configFlagsLastFrame hasnt ConfigFlag.DockingEnable)
        assert(false) { "Please set DockingEnable before the first call to NewFrame()! Otherwise you will lose your .ini settings!" }
    if (g.frameCount == 1 && io.configFlags has ConfigFlag.ViewportsEnable && g.configFlagsLastFrame hasnt ConfigFlag.ViewportsEnable)
        assert(false) { "Please set ViewportsEnable before the first call to NewFrame()! Otherwise you will lose your .ini settings!" }

    // Perform simple checks: multi-viewport and platform windows support
    if (io.configFlags has ConfigFlag.ViewportsEnable) {
        if (io.backendFlags has BackendFlag.PlatformHasViewports && io.backendFlags has BackendFlag.RendererHasViewports) {
            assert(g.frameCount == 0 || g.frameCount == g.frameCountPlatformEnded) { "Forgot to call UpdatePlatformWindows() in main loop after EndFrame()? Check examples/ applications for reference." }
            assert(g.platformIO.platform_CreateWindow != null) { "Platform init didn't install handlers?" }
            assert(g.platformIO.platform_DestroyWindow != null) { "Platform init didn't install handlers?" }
            assert(g.platformIO.platform_GetWindowPos != null) { "Platform init didn't install handlers?" }
            assert(g.platformIO.platform_SetWindowPos != null) { "Platform init didn't install handlers?" }
            assert(g.platformIO.platform_GetWindowSize != null) { "Platform init didn't install handlers?" }
            assert(g.platformIO.platform_SetWindowSize != null) { "Platform init didn't install handlers?" }
            assert(g.platformIO.monitors.isNotEmpty()) { "Platform init didn't setup Monitors list?" }
            assert(g.viewports[0].platformUserData != null || g.viewports[0].platformHandle != null) { "Platform init didn't setup main viewport." }
            if (io.configDockingTransparentPayload && io.configFlags has ConfigFlag.DockingEnable)
                assert(g.platformIO.platform_SetWindowAlpha != null) { "Platform_SetWindowAlpha handler is required to use io.ConfigDockingTransparent!" }
        } else
        // Disable feature, our back-ends do not support it
            io.configFlags = io.configFlags wo ConfigFlag.ViewportsEnable

        // Perform simple checks on platform monitor data + compute a total bounding box for quick early outs
        g.platformIO.monitors.forEach {
            assert(it.mainSize anyGreaterThan 0f) { "Monitor main bounds not setup properly." }
            assert(Rect(it.workPos, it.workPos + it.workSize) in Rect(it.mainPos, it.mainPos + it.mainSize)) {"Monitor work bounds not setup properly. If you don't have work area information, just copy MainPos/MainSize into them."}
            assert(it.dpiScale != 0f)
        }
    }
}

fun errorCheckEndFrameSanityChecks() {

    // Verify that io.KeyXXX fields haven't been tampered with. Key mods should not be modified between NewFrame() and EndFrame()
    // One possible reason leading to this assert is that your backends update inputs _AFTER_ NewFrame().
    // It is known that when some modal native windows called mid-frame takes focus away, some backends such as GLFW will
    // send key release events mid-frame. This would normally trigger this assertion and lead to sheared inputs.
    // We silently accommodate for this case by ignoring/ the case where all io.KeyXXX modifiers were released (aka key_mod_flags == 0),
    // while still correctly asserting on mid-frame key press events.
    val keyModFlags = mergedKeyModFlags
    assert(keyModFlags == 0 || g.io.keyMods == keyModFlags) { "Mismatching io.KeyCtrl/io.KeyShift/io.KeyAlt/io.KeySuper vs io.KeyMods" }

    // Recover from errors
    //ErrorCheckEndFrameRecover();

    // Report when there is a mismatch of Begin/BeginChild vs End/EndChild calls. Important: Remember that the Begin/BeginChild API requires you
    // to always call End/EndChild even if Begin/BeginChild returns false! (this is unfortunately inconsistent with most other Begin* API).
    if (g.currentWindowStack.size != 1)
        if (g.currentWindowStack.size > 1) {
            assert(g.currentWindowStack.size == 1) { "Mismatched Begin/BeginChild vs End/EndChild calls: did you forget to call End/EndChild?" }
            while (g.currentWindowStack.size > 1)
                end()
        } else
            assert(g.currentWindowStack.size == 1) { "Mismatched Begin/BeginChild vs End/EndChild calls: did you call End/EndChild too much?" }

    assert(g.groupStack.isEmpty()) { "Missing EndGroup call!" }
}