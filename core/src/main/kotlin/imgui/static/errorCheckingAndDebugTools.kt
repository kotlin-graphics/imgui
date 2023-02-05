package imgui.static

import imgui.*
import imgui.ImGui.end
import imgui.ImGui.getStyleColorVec4
import imgui.ImGui.io
import imgui.ImGui.isMouseClicked
import imgui.ImGui.isPressed
import imgui.ImGui.setNextWindowBgAlpha
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textColored
import imgui.api.g
import imgui.dsl.tooltip


//-----------------------------------------------------------------------------
// [SECTION] ERROR CHECKING
// Error Checking and Debug Tools
//-----------------------------------------------------------------------------

// Helper function to verify ABI compatibility between caller code and compiled version of Dear ImGui.
// Verify that the type sizes are matching between the calling file's compilation unit and imgui.cpp's compilation unit
// If this triggers you have an issue:
// - Most commonly: mismatched headers and compiled code version.
// - Or: mismatched configuration #define, compilation settings, packing pragma etc.
//   The configuration settings mentioned in imconfig.h must be set for all compilation units involved with Dear ImGui,
//   which is way it is required you put them in your imconfig file (and not just before including imgui.h).
//   Otherwise it is possible that different compilation units would see different structure layout
fun errorCheckNewFrameSanityChecks() {

    // Check user data
    // (We pass an error message in the assert expression to make it visible to programmers who are not using a debugger, as most assert handlers display their argument)
    assert(g.initialized)
    assert(io.deltaTime > 0f || g.frameCount == 0) { "Need a positive DeltaTime!" }
    assert(g.frameCount == 0 || g.frameCountEnded == g.frameCount) { "Forgot to call Render() or EndFrame() at the end of the previous frame?" }
    assert(io.displaySize.x >= 0f && io.displaySize.y >= 0f) { "Invalid DisplaySize value!" } // TODO glm
    assert(io.fonts.isBuilt) { "Font Atlas not built! Make sure you called ImGui_ImplXXXX_NewFrame() function for renderer backend, which should call io.Fonts->GetTexDataAsRGBA32() / GetTexDataAsAlpha8()" }
    assert(style.curveTessellationTol > 0f) { "Invalid style setting!" }
    assert(style.circleTessellationMaxError > 0f) { "Invalid style setting!" }
    assert(style.alpha in 0f..1f) { "Invalid style setting!" } // Allows us to avoid a few clamps in color computations
    assert(style.windowMinSize.x >= 1f && style.windowMinSize.y >= 1f) { "Invalid style setting." } // TODO glm
    assert(style.windowMenuButtonPosition == Dir.None || style.windowMenuButtonPosition == Dir.Left || style.windowMenuButtonPosition == Dir.Right)
    assert(g.style.colorButtonPosition == Dir.Left || g.style.colorButtonPosition == Dir.Right)

    // Check: the beta io.ConfigWindowsResizeFromEdges option requires backend to honor mouse cursor changes and set the ImGuiBackendFlags_HasMouseCursors flag accordingly.
    if (io.configWindowsResizeFromEdges && io.backendFlags hasnt BackendFlag.HasMouseCursors)
        io.configWindowsResizeFromEdges = false
}

fun errorCheckEndFrameSanityChecks() {

    // Verify that io.KeyXXX fields haven't been tampered with. Key mods should not be modified between NewFrame() and EndFrame()
    // One possible reason leading to this assert is that your backends update inputs _AFTER_ NewFrame().
    // It is known that when some modal native windows called mid-frame takes focus away, some backends such as GLFW will
    // send key release events mid-frame. This would normally trigger this assertion and lead to sheared inputs.
    // We silently accommodate for this case by ignoring the case where all io.KeyXXX modifiers were released (aka key_mod_flags == 0),
    // while still correctly asserting on mid-frame key press events.
    val keyMods = mergedModsFromBools
    assert(keyMods == 0 || g.io.keyMods == keyMods) { "Mismatching io.KeyCtrl/io.KeyShift/io.KeyAlt/io.KeySuper vs io.KeyMods" }

    // [EXPERIMENTAL] Recover from errors: You may call this yourself before EndFrame().
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

/** [DEBUG] Item picker tool - start with DebugStartItemPicker() - useful to visually select an item and break into its call-stack. */
fun updateDebugToolItemPicker() {

    g.debugItemPickerBreakId = 0
    if (g.debugItemPickerActive) {

        val hoveredId = g.hoveredIdPreviousFrame
        ImGui.mouseCursor = MouseCursor.Hand
        if (Key.Escape.isPressed)
            g.debugItemPickerActive = false
        val changeMapping = g.io.keyMods == Key.Mod_Ctrl or Key.Mod_Shift
        if (!changeMapping && isMouseClicked(g.debugItemPickerMouseButton) && hoveredId != 0) {
            g.debugItemPickerBreakId = hoveredId
            g.debugItemPickerActive = false
        }
        for (mouseButton in MouseButton.values().drop(1)) // drop None
            if (changeMapping && isMouseClicked(mouseButton))
                g.debugItemPickerMouseButton = mouseButton
        setNextWindowBgAlpha(0.7f)
        tooltip {
            text("HoveredId: 0x%08X", hoveredId)
            text("Press ESC to abort picking.")
            if (changeMapping)
                text("Remap w/ Ctrl+Shift: click anywhere to select new mouse button.")
            else
                textColored(getStyleColorVec4(if (hoveredId != 0) Col.Text else Col.TextDisabled), "Click ${g.debugItemPickerMouseButton} Button to break in debugger! (remap w/ Ctrl+Shift)")
        }
    }
}

fun updateDebugToolStackQueries() {

    val tool = g.debugStackTool

    // Clear hook when stack tool is not visible
    g.debugHookIdInfo = 0
    if (g.frameCount != tool.lastActiveFrame + 1)
        return

    // Update queries. The steps are: -1: query Stack, >= 0: query each stack item
    // We can only perform 1 ID Info query every frame. This is designed so the GetID() tests are cheap and constant-time
    val queryId = if (g.hoveredIdPreviousFrame != 0) g.hoveredIdPreviousFrame else g.activeId
    if (tool.queryId != queryId) {
        tool.queryId = queryId
        tool.stackLevel = -1
        tool.results.clear()
    }
    if (queryId == 0)
        return

    // Advance to next stack level when we got our result, or after 2 frames (in case we never get a result)
    var stackLevel = tool.stackLevel
    if (stackLevel in tool.results.indices)
        if (tool.results[stackLevel].querySuccess || tool.results[stackLevel].queryFrameCount > 2)
            tool.stackLevel++

    // Update hook
    stackLevel = tool.stackLevel
    if (stackLevel == -1)
        g.debugHookIdInfo = queryId
    if (stackLevel in tool.results.indices) {
        g.debugHookIdInfo = tool.results[stackLevel].id
        tool.results[stackLevel].queryFrameCount++
    }
}