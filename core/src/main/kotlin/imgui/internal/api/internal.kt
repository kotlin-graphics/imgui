package imgui.internal.api

import glm_.func.common.max
import glm_.parseInt
import imgui.*
import imgui.ImGui.clearActiveID
import imgui.ImGui.closePopupsOverWindow
import imgui.ImGui.foregroundDrawList
import imgui.ImGui.io
import imgui.api.g
import imgui.classes.DrawList
import imgui.font.Font
import imgui.internal.classes.ShrinkWidthItem
import imgui.internal.classes.Window
import imgui.internal.hashStr
import imgui.internal.sections.IMGUI_DEBUG_LOG_FOCUS
import imgui.internal.sections.NavLayer
import imgui.static.findWindowFocusIndex
import imgui.static.navRestoreLastChildNavWindow
import imgui.static.navUpdateAnyRequestFlag
import uno.kotlin.NUL
import java.util.*
import java.util.regex.Pattern
import imgui.WindowFlag as Wf


@Suppress("UNCHECKED_CAST")

internal interface internal {

    // Windows

    /** We should always have a CurrentWindow in the stack (there is an implicit "Debug" window)
     *  If this ever crash because g.CurrentWindow is NULL it means that either
     *  - ImGui::NewFrame() has never been called, which is illegal.
     *  - You are calling ImGui functions after ImGui::EndFrame()/ImGui::Render() and before the next ImGui::NewFrame(), which is also illegal. */

    /** ~GetCurrentWindowRead */
    val currentWindowRead: Window?
        get() = g.currentWindow

    /** ~GetCurrentWindow */
    val currentWindow: Window
        get() = g.currentWindow?.apply { writeAccessed = true } ?: throw Error("""
                We should always have a CurrentWindow in the stack (there is an implicit \"Debug\" window)
                If this ever crash because ::currentWindow is NULL it means that either
                - ::newFrame() has never been called, which is illegal.
                - You are calling ImGui functions after ::render() and before the next ::newFrame(), which is also illegal.
                - You are calling ImGui functions after ::endFrame()/::render() and before the next ImGui::newFrame(), which is also illegal.
                """.trimIndent())

    fun findWindowByID(id: ID): Window? = g.windowsById[id]

    fun findWindowByName(name: String): Window? = g.windowsById[hashStr(name)]


    // Windows: Display Order and Focus Order

    /** Moving window to front of display (which happens to be back of our sorted list)  ~ FocusWindow  */
    fun focusWindow(window: Window? = null) {

        if (g.navWindow !== window) {
            IMGUI_DEBUG_LOG_FOCUS("[focus] FocusWindow(\"${window?.name ?: "<NULL>"}\")\n")
            g.navWindow = window
            if (window != null && g.navDisableMouseHover)
                g.navMousePosDirty = true
            g.navId = window?.navLastIds?.get(0) ?: 0 // Restore NavId
            g.navFocusScopeId = 0
            g.navIdIsAlive = false
            g.navLayer = NavLayer.Main
            g.navInitRequest = false; g.navMoveSubmitted = false; g.navMoveScoringItems = false
            navUpdateAnyRequestFlag()
        }

        // Close popups if any
        closePopupsOverWindow(window, false)

        // Move the root window to the top of the pile
        assert(window == null || window.rootWindow != null)
        val focusFrontWindow = window?.rootWindow // NB: In docking branch this is window->RootWindowDockStop
        val displayFrontWindow = window?.rootWindow

        // Steal active widgets. Some of the cases it triggers includes:
        // - Focus a window while an InputText in another window is active, if focus happens before the old InputText can run.
        // - When using Nav to activate menu items (due to timing of activating on press->new window appears->losing ActiveId)
        if (g.activeId != 0 && g.activeIdWindow?.rootWindow !== focusFrontWindow)
            if (!g.activeIdNoClearOnFocusLoss)
                clearActiveID()

        // Passing NULL allow to disable keyboard focus
        if (window == null)
            return

        // Bring to front
        focusFrontWindow!!.bringToFocusFront()
        if ((window.flags or displayFrontWindow!!.flags) hasnt Wf.NoBringToFrontOnFocus)
            displayFrontWindow.bringToDisplayFront()
    }

    fun focusTopMostWindowUnderOne(underThisWindow_: Window? = null, ignoreWindow: Window? = null) {
        var underThisWindow = underThisWindow_
        var startIdx = g.windowsFocusOrder.lastIndex
        if (underThisWindow != null) {
            // Aim at root window behind us, if we are in a child window that's our own root (see #4640)
            var offset = -1
            while (underThisWindow!!.flags has Wf._ChildWindow) {
                underThisWindow = underThisWindow.parentWindow
                offset = 0
            }
            startIdx = findWindowFocusIndex(underThisWindow) + offset
        }
        for (i in startIdx downTo 0) {
            // We may later decide to test for different NoXXXInputs based on the active navigation input (mouse vs nav) but that may feel more confusing to the user.
            val window = g.windowsFocusOrder[i]
            assert(window === window.rootWindow)
            if (window !== ignoreWindow && window.wasActive)
                if ((window.flags and (Wf.NoMouseInputs or Wf.NoNavInputs)) != (Wf.NoMouseInputs or Wf.NoNavInputs)) {
                    focusWindow(navRestoreLastChildNavWindow(window))
                    return
                }
        }
        focusWindow()
    }

    // the rest of the window related functions is inside the corresponding class


    // Fonts, drawing

    /** Important: this alone doesn't alter current ImDrawList state. This is called by PushFont/PopFont only. */
    fun setCurrentFont(font: Font) {
        assert(font.isLoaded) { "Font Atlas not created. Did you call io.Fonts->GetTexDataAsRGBA32 / GetTexDataAsAlpha8 ?" }
        assert(font.scale > 0f)
        g.font = font
        g.fontBaseSize = 1f max (io.fontGlobalScale * g.font.fontSize * g.font.scale)
        g.fontSize = g.currentWindow?.calcFontSize() ?: 0f

        val atlas = g.font.containerAtlas
        g.drawListSharedData.also {
            it.texUvWhitePixel = atlas.texUvWhitePixel
            it.texUvLines = atlas.texUvLines
            it.font = g.font
            it.fontSize = g.fontSize
        }
    }

    /** ~GetDefaultFont */
    val defaultFont: Font
        get() = io.fontDefault ?: io.fonts.fonts[0]

    fun getForegroundDrawList(window: Window?): DrawList = foregroundDrawList // This seemingly unnecessary wrapper simplifies compatibility between the 'master' and 'docking' branches.

    // GetBackgroundDrawList(ImGuiViewport* viewport)
    // GetForegroundDrawList(ImGuiViewport* viewport); -> Viewport class

    companion object {

        fun alphaBlendColor(colA: Int, colB: Int): Int {
            val t = ((colB ushr COL32_A_SHIFT) and 0xFF) / 255f
            val r = imgui.internal.lerp((colA ushr COL32_R_SHIFT) and 0xFF, (colB ushr COL32_R_SHIFT) and 0xFF, t)
            val g = imgui.internal.lerp((colA ushr COL32_G_SHIFT) and 0xFF, (colB ushr COL32_G_SHIFT) and 0xFF, t)
            val b = imgui.internal.lerp((colA ushr COL32_B_SHIFT) and 0xFF, (colB ushr COL32_B_SHIFT) and 0xFF, t)
            return COL32(r, g, b, 0xFF)
        }

        val shrinkWidthItemComparer: Comparator<ShrinkWidthItem> = compareBy(ShrinkWidthItem::width, ShrinkWidthItem::index)
    }
}