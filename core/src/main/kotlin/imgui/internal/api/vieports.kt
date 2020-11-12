package imgui.internal.api

import glm_.vec2.Vec2
import imgui.Col
import imgui.ConfigFlag
import imgui.ImGui.dummy
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.getColorU32
import imgui.WindowFlag
import imgui.api.g
import imgui.classes.DrawList
import imgui.classes.ViewportFlag
import imgui.classes.ViewportP
import imgui.classes.has
import imgui.has
import imgui.internal.classes.Rect
import imgui.internal.floor
import imgui.internal.sections.ASSERT_PARANOID
import imgui.static.IMGUI_VIEWPORT_DEFAULT_ID

// Viewports
interface vieports {

    /** Translate imgui windows when a Host Viewport has been moved
     *  (This additionally keeps windows at the same place when ImGuiConfigFlags_ViewportsEnable is toggled!)
     *
     *  [JVM] Vec2 Instance safe */
    fun translateWindowsInViewport(viewport: ViewportP, oldPos: Vec2, newPos: Vec2) {

        ASSERT_PARANOID(viewport.window == null && viewport.flags has ViewportFlag.CanHostOtherWindows)

        // 1) We test if ImGuiConfigFlags_ViewportsEnable was just toggled, which allows us to conveniently
        // translate imgui windows from OS-window-local to absolute coordinates or vice-versa.
        // 2) If it's not going to fit into the new size, keep it at same absolute position.
        // One problem with this is that most Win32 applications doesn't update their render while dragging,
        // and so the window will appear to teleport when releasing the mouse.
        val translateAllWindows = g.configFlagsCurrFrame has ConfigFlag.ViewportsEnable != (g.configFlagsLastFrame has ConfigFlag.ViewportsEnable)
        val testStillFitRect = Rect(oldPos, oldPos + viewport.size)
        val deltaPos = newPos - oldPos
        g.windows.filter { translateAllWindows || (it.viewport === viewport && it.rect() in testStillFitRect) }
                .forEach { it translate deltaPos }
    }

    /** Scale all windows (position, size). Use when e.g. changing DPI. (This is a lossy operation!) */
    fun scaleWindowsInViewport(viewport: ViewportP, scale: Float) {
        viewport.window?.let { it scale scale }
                ?: g.windows.filter { it.viewport === viewport }.forEach { it scale scale }
    }

    fun destroyPlatformWindow(viewport: ViewportP) {
        if (viewport.platformWindowCreated) {
            g.platformIO.renderer_DestroyWindow?.invoke(viewport)
            g.platformIO.platform_DestroyWindow?.invoke(viewport)
            assert(viewport.rendererUserData == null && viewport.platformUserData == null)

            // Don't clear PlatformWindowCreated for the main viewport, as we initially set that up to true in Initialize()
            // The right-er way may be to leave it to the back-end to set this flag all-together, and made the flag public.
            if (viewport.id != IMGUI_VIEWPORT_DEFAULT_ID)
                viewport.platformWindowCreated = false
        } else
            assert(viewport.rendererUserData == null && viewport.platformUserData == null && viewport.platformHandle == null)

        viewport.rendererUserData = null
        viewport.platformUserData = null
        viewport.platformHandle = null
        viewport.clearRequestFlags()
    }

    fun showViewportThumbnails() {

        val window = g.currentWindow!!

        // We don't display full monitor bounds (we could, but it often looks awkward), instead we display just enough to cover all of our viewports.
        val SCALE = 1f / 8f
        val bbFull = Rect()
        //for (int n = 0; n < g.PlatformIO.Monitors.Size; n++)
        //    bb_full.Add(GetPlatformMonitorMainRect(g.PlatformIO.Monitors[n]));
        g.viewports.forEach { bbFull.add(it.mainRect) }
        val p = window.dc.cursorPos
        val off = p - bbFull.min * SCALE
        //for (int n = 0; n < g.PlatformIO.Monitors.Size; n++)
        //    window->DrawList->AddRect(off + g.PlatformIO.Monitors[n].MainPos * SCALE, off + (g.PlatformIO.Monitors[n].MainPos + g.PlatformIO.Monitors[n].MainSize) * SCALE, ImGui::GetColorU32(ImGuiCol_Border));
        g.viewports.forEach {
            val viewportDrawBb = Rect(off + (it.pos) * SCALE, off + (it.pos + it.size) * SCALE)
            renderViewportThumbnail(window.drawList, it, viewportDrawBb)
        }
        dummy(bbFull.size * SCALE)
    }

    companion object {

        fun renderViewportThumbnail(drawList: DrawList, viewport: ViewportP, bb: Rect) {

            val window = g.currentWindow!!

            val scale = bb.size / viewport.size
            val off = bb.min - viewport.pos * scale
            val alphaMul = if (viewport.flags has ViewportFlag.Minimized) 0.3f else 1f
            window.drawList.addRectFilled(bb.min, bb.max, getColorU32(Col.Border, alphaMul * 0.4f))
            for (thumbWindow in g.windows) {
                if (!thumbWindow.wasActive || thumbWindow.flags has WindowFlag._ChildWindow)
                    continue
                if (thumbWindow.skipItems && thumbWindow.flags has WindowFlag._ChildWindow) // FIXME-DOCK: Skip hidden docked windows. Identify those betters.
                    continue
                if (thumbWindow.viewport !== viewport)
                    continue

                val thumbR = thumbWindow.rect()
                val titleR = thumbWindow.titleBarRect()
                val thumbRScaled = Rect(floor(off + thumbR.min * scale), floor(off + thumbR.max * scale))
                val titleRScaled = Rect(floor(off + titleR.min * scale), floor(off + Vec2(titleR.max.x, titleR.min.y) * scale) + Vec2(0, 5)) // Exaggerate title bar height
                thumbRScaled clipWithFull bb
                titleRScaled clipWithFull bb
                val windowIsFocused = g.navWindow?.let { thumbWindow.rootWindowForTitleBarHighlight === it.rootWindowForTitleBarHighlight } == true
                window.drawList.apply {
                    addRectFilled(thumbRScaled.min, thumbRScaled.max, getColorU32(Col.WindowBg, alphaMul))
                    addRectFilled(titleRScaled.min, titleRScaled.max, getColorU32(if (windowIsFocused) Col.TitleBgActive else Col.TitleBg, alphaMul))
                    addRect(thumbRScaled.min, thumbRScaled.max, getColorU32(Col.Border, alphaMul))
                    thumbWindow.windowForTitleDisplay?.let { windowForTitle ->
                        addText(g.font, g.fontSize * 1f, titleRScaled.min, getColorU32(Col.Text, alphaMul), windowForTitle.name.toByteArray(), findRenderedTextEnd(windowForTitle.name))
                    }
                }
            }
            drawList.addRect(bb.min, bb.max, getColorU32(Col.Border, alphaMul))
        }

    }
}