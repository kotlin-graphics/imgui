package imgui.api

import imgui.ConfigFlag
import imgui.ID
import imgui.IMGUI_DEBUG_LOG_VIEWPORT
import imgui.ImGui.destroyPlatformWindow
import imgui.ImGui.findRenderedTextEnd
import imgui.classes.*
import imgui.hasnt
import imgui.internal.hash

// (Optional) Platform/OS interface for multi-viewport support
// Read comments around the ImGuiPlatformIO structure for more details.
// Note: You may use GetWindowViewport() to get the current viewport of the current window.
interface platformOS {

    /** platform/renderer functions, for back-end to setup + viewports list. */
    val platformIO: PlatformIO
        get() {
            assert(gImGui != null) { "No current context. Did you call ImGui::CreateContext() or ImGui::SetCurrentContext()?" }
            return gImGui!!.platformIO
        }

    /** main viewport. same as GetPlatformIO().MainViewport == GetPlatformIO().Viewports[0]. */
    val mainViewport: Viewport
        get() = g.viewports[0]

    /** call in main loop. will call CreateWindow/ResizeWindow/etc. platform functions for each secondary viewport, and DestroyWindow for each inactive viewport.
     *
     *  Called by user at the end of the main loop, after EndFrame()
     *  This will handle the creation/update of all OS windows via function defined in the ImGuiPlatformIO api. */
    fun updatePlatformWindows() {
        assert(g.frameCountEnded == g.frameCount) { "Forgot to call Render() or EndFrame() before UpdatePlatformWindows()?" }
        assert(g.frameCountPlatformEnded < g.frameCount)
        g.frameCountPlatformEnded = g.frameCount
        if (g.configFlagsCurrFrame hasnt ConfigFlag.ViewportsEnable)
            return

        // Create/resize/destroy platform windows to match each active viewport.
        // Skip the main viewport (index 0), which is always fully handled by the application!
        for (i in 1 until g.viewports.size) {

            val viewport = g.viewports[i]

            // Destroy platform window if the viewport hasn't been submitted or if it is hosting a hidden window
            // (the implicit/fallback Debug##Default window will be registering its viewport then be disabled, causing a dummy DestroyPlatformWindow to be made each frame)
            var destroyPlatformWindow = false
            destroyPlatformWindow = destroyPlatformWindow or (viewport.lastFrameActive < g.frameCount - 1)
            destroyPlatformWindow = destroyPlatformWindow or (viewport.window?.isActiveAndVisible == false)
            if (destroyPlatformWindow) {
                destroyPlatformWindow(viewport)
                continue
            }

            // New windows that appears directly in a new viewport won't always have a size on their first frame
            if (viewport.lastFrameActive < g.frameCount || viewport.size.x <= 0 || viewport.size.y <= 0)
                continue

            // Create window
            val isNewPlatformWindow = !viewport.platformWindowCreated
            if (isNewPlatformWindow) {
                IMGUI_DEBUG_LOG_VIEWPORT("Create Platform Window %08X (${viewport.window?.name ?: "n/a"})".format(viewport.id))
                g.platformIO.platform_CreateWindow!!.invoke(viewport)
                g.platformIO.renderer_CreateWindow?.invoke(viewport)
                viewport.lastNameHash = 0
                // By clearing those we'll enforce a call to Platform_SetWindowPos/Size below, before Platform_ShowWindow (FIXME: Is that necessary?)
                viewport.lastPlatformPos put Float.MAX_VALUE
                viewport.lastPlatformSize put Float.MAX_VALUE
                viewport.lastRendererSize put viewport.size     // We don't need to call Renderer_SetWindowSize() as it is expected Renderer_CreateWindow() already did it.
                viewport.platformWindowCreated = true
            }

            // Apply Position and Size (from ImGui to Platform/Renderer back-ends)
            if ((viewport.lastPlatformPos.x != viewport.pos.x || viewport.lastPlatformPos.y != viewport.pos.y) && !viewport.platformRequestMove)
                g.platformIO.platform_SetWindowPos!!.invoke(viewport, viewport.pos)
            if ((viewport.lastPlatformSize.x != viewport.size.x || viewport.lastPlatformSize.y != viewport.size.y) && !viewport.platformRequestResize)
                g.platformIO.platform_SetWindowSize!!.invoke(viewport, viewport.size)
            if (viewport.lastRendererSize.x != viewport.size.x || viewport.lastRendererSize.y != viewport.size.y)
                g.platformIO.renderer_SetWindowSize?.invoke(viewport, viewport.size)
            viewport.lastPlatformPos put viewport.pos
            viewport.lastPlatformSize put viewport.size
            viewport.lastRendererSize put viewport.size

            // Update title bar (if it changed)
            viewport.window!!.windowForTitleDisplay?.let { windowForTitle ->
                val title = windowForTitle.name
                val titleEnd = findRenderedTextEnd(title)
                val titleHash = hash(title, titleEnd)
                if (viewport.lastNameHash != titleHash) {
                    g.platformIO.platform_SetWindowTitle!!.invoke(viewport, title)
                    viewport.lastNameHash = titleHash
                }
            }

            // Update alpha (if it changed)
            if (viewport.lastAlpha != viewport.alpha)
                g.platformIO.platform_SetWindowAlpha?.invoke(viewport, viewport.alpha)
            viewport.lastAlpha = viewport.alpha

            // Optional, general purpose call to allow the back-end to perform general book-keeping even if things haven't changed.
            g.platformIO.platform_UpdateWindow?.invoke(viewport)

            if (isNewPlatformWindow) {
                // On startup ensure new platform window don't steal focus (give it a few frames, as nested contents may lead to viewport being created a few frames late)
                if (g.frameCount < 3)
                    viewport.flags = viewport.flags or ViewportFlag.NoFocusOnAppearing

                // Show window
                g.platformIO.platform_ShowWindow!!.invoke(viewport)

                // Even without focus, we assume the window becomes front-most.
                // This is useful for our platform z-order heuristic when io.MouseHoveredViewport is not available.
                if (viewport.lastFrontMostStampCount != g.viewportFrontMostStampCount)
                    viewport.lastFrontMostStampCount = ++g.viewportFrontMostStampCount
            }

            // Clear request flags
            viewport.clearRequestFlags()
        }

        // Update our implicit z-order knowledge of platform windows, which is used when the back-end cannot provide io.MouseHoveredViewport.
        // When setting Platform_GetWindowFocus, it is expected that the platform back-end can handle calls without crashing if it doesn't have data stored.
        // FIXME-VIEWPORT: We should use this information to also set dear imgui-side focus, allowing us to handle os-level alt+tab.
        g.platformIO.platform_GetWindowFocus?.let { getWindowFocus ->
            g.viewports.find { it.platformWindowCreated && getWindowFocus(it) }?.let { focusedViewport ->
                // Store a tag so we can infer z-order easily from all our windows
                if (focusedViewport.lastFrontMostStampCount != g.viewportFrontMostStampCount)
                    focusedViewport.lastFrontMostStampCount = ++g.viewportFrontMostStampCount
            }
        }
    }

    /** call in main loop. will call RenderWindow/SwapBuffers platform functions for each secondary viewport which doesn't have the ImGuiViewportFlags_Minimized flag set. May be reimplemented by user for custom rendering needs.
     *
     *  This is a default/basic function for performing the rendering/swap of multiple Platform Windows.
     *  Custom renderers may prefer to not call this function at all, and instead iterate the publicly exposed platform data and handle rendering/sync themselves.
     *  The Render/Swap functions stored in ImGuiPlatformIO are merely here to allow for this helper to exist, but you can do it yourself:
     *
     *     ImGuiPlatformIO& platform_io = ImGui::GetPlatformIO();
     *     for (int i = 1; i < platform_io.Viewports.Size; i++)
     *         if ((platform_io.Viewports[i]->Flags & ImGuiViewportFlags_Minimized) == 0)
     *             MyRenderFunction(platform_io.Viewports[i], my_args);
     *     for (int i = 1; i < platform_io.Viewports.Size; i++)
     *         if ((platform_io.Viewports[i]->Flags & ImGuiViewportFlags_Minimized) == 0)
     *             MySwapBufferFunction(platform_io.Viewports[i], my_args);
     */
    fun renderPlatformWindowsDefault(platformRenderArg: Any? = null, rendererRenderArg: Any? = null) {
        // Skip the main viewport (index 0), which is always fully handled by the application!
        for (i in 1 until platformIO.viewports.size) {
            val viewport = platformIO.viewports[i]
            if (viewport.flags has ViewportFlag.Minimized)
                continue
            platformIO.platform_RenderWindow?.invoke(viewport, platformRenderArg)
            platformIO.renderer_RenderWindow?.invoke(viewport, rendererRenderArg)
        }
        for (i in 1 until platformIO.viewports.size) {
            val viewport = platformIO.viewports[i]
            if (viewport.flags has ViewportFlag.Minimized)
                continue
            platformIO.platform_SwapBuffers?.invoke(viewport, platformRenderArg)
            platformIO.renderer_SwapBuffers?.invoke(viewport, rendererRenderArg)
        }
    }

    /** call DestroyWindow platform functions for all viewports. call from back-end Shutdown() if you need to close platform windows before imgui shutdown. otherwise will be called by DestroyContext(). */
    fun destroyPlatformWindows() {
        // We call the destroy window on every viewport (including the main viewport, index 0) to give a chance to the back-end
        // to clear any data they may have stored in e.g. PlatformUserData, RendererUserData.
        // It is convenient for the platform back-end code to store something in the main viewport, in order for e.g. the mouse handling
        // code to operator a consistent manner.
        // It is expected that the back-end can handle calls to Renderer_DestroyWindow/Platform_DestroyWindow without
        // crashing if it doesn't have data stored.
        g.viewports.forEach(::destroyPlatformWindow)
    }

    /** this is a helper for back-ends. */
    fun findViewportByID(id: ID): Viewport? = g.viewports.find { it.id == id }

    // this is a helper for back-ends. the type platform_handle is decided by the back-end (e.g. HWND, MyWindow*, GLFWwindow* etc.)
    fun findViewportByPlatformHandle(platformHandle: Any): Viewport? = g.viewports.find { it.platformHandle == platformHandle }
}