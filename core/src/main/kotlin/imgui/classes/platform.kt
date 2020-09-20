package imgui.classes

import glm_.vec2.Vec2

//-----------------------------------------------------------------------------
// [BETA] Platform interface for multi-viewport support
//-----------------------------------------------------------------------------
// (Optional) This is completely optional, for advanced users!
// If you are new to Dear ImGui and trying to integrate it into your engine, you can probably ignore this for now.
//
// This feature allows you to seamlessly drag Dear ImGui windows outside of your application viewport.
// This is achieved by creating new Platform/OS windows on the fly, and rendering into them.
// Dear ImGui manages the viewport structures, and the back-end create and maintain one Platform/OS window for each of those viewports.
//
// See Glossary https://github.com/ocornut/imgui/wiki/Glossary for details about some of the terminology.
// See Thread https://github.com/ocornut/imgui/issues/1542 for gifs, news and questions about this evolving feature.
//
// About the coordinates system:
// - When multi-viewports are enabled, all Dear ImGui coordinates become absolute coordinates (same as OS coordinates!)
// - So e.g. ImGui::SetNextWindowPos(ImVec2(0,0)) will position a window relative to your primary monitor!
// - If you want to position windows relative to your main application viewport, use ImGui::GetMainViewport()->Pos as a base position.
//
// Steps to use multi-viewports in your application, when using a default back-end from the examples/ folder:
// - Application:  Enable feature with 'io.ConfigFlags |= ImGuiConfigFlags_ViewportsEnable'.
// - Back-end:     The back-end initialization will setup all necessary ImGuiPlatformIO's functions and update monitors info every frame.
// - Application:  In your main loop, call ImGui::UpdatePlatformWindows(), ImGui::RenderPlatformWindowsDefault() after EndFrame() or Render().
// - Application:  Fix absolute coordinates used in ImGui::SetWindowPos() or ImGui::SetNextWindowPos() calls.
//
// Steps to use multi-viewports in your application, when using a custom back-end:
// - Important:    THIS IS NOT EASY TO DO and comes with many subtleties not described here!
//                 It's also an experimental feature, so some of the requirements may evolve.
//                 Consider using default back-ends if you can. Either way, carefully follow and refer to examples/ back-ends for details.
// - Application:  Enable feature with 'io.ConfigFlags |= ImGuiConfigFlags_ViewportsEnable'.
// - Back-end:     Hook ImGuiPlatformIO's Platform_* and Renderer_* callbacks (see below).
//                 Set 'io.BackendFlags |= ImGuiBackendFlags_PlatformHasViewports' and 'io.BackendFlags |= ImGuiBackendFlags_PlatformHasViewports'.
//                 Update ImGuiPlatformIO's Monitors list every frame.
//                 Update MousePos every frame, in absolute coordinates.
// - Application:  In your main loop, call ImGui::UpdatePlatformWindows(), ImGui::RenderPlatformWindowsDefault() after EndFrame() or Render().
//                 You may skip calling RenderPlatformWindowsDefault() if its API is not convenient for your needs. Read comments below.
// - Application:  Fix absolute coordinates used in ImGui::SetWindowPos() or ImGui::SetNextWindowPos() calls.
//
// About ImGui::RenderPlatformWindowsDefault():
// - This function is a mostly a _helper_ for the common-most cases, and to facilitate using default back-ends.
// - You can check its simple source code to understand what it does.
//   It basically iterates secondary viewports and call 4 functions that are setup in ImGuiPlatformIO, if available:
//     Platform_RenderWindow(), Renderer_RenderWindow(), Platform_SwapBuffers(), Renderer_SwapBuffers()
//   Those functions pointers exists only for the benefit of RenderPlatformWindowsDefault().
// - If you have very specific rendering needs (e.g. flipping multiple swap-chain simultaneously, unusual sync/threading issues, etc.),
//   you may be tempted to ignore RenderPlatformWindowsDefault() and write customized code to perform your renderingg.
//   You may decide to setup the platform_io's *RenderWindow and *SwapBuffers pointers and call your functions through those pointers,
//   or you may decide to never setup those pointers and call your code directly. They are a convenience, not an obligatory interface.
//-----------------------------------------------------------------------------

typealias Platform_CreateWindow = (vp: Viewport) -> Unit
typealias Platform_DestroyWindow = (vp: Viewport) -> Unit
typealias Platform_ShowWindow = (vp: Viewport) -> Unit
typealias Platform_SetWindowPos = (vp: Viewport, pos: Vec2) -> Unit
typealias Platform_GetWindowPos = (vp: Viewport) -> Vec2
typealias Platform_SetWindowSize = (vp: Viewport, size: Vec2) -> Unit
typealias Platform_GetWindowSize = (vp: Viewport) -> Vec2
typealias Platform_SetWindowFocus = (vp: Viewport) -> Unit
typealias Platform_GetWindowFocus = (vp: Viewport) -> Boolean
typealias Platform_GetWindowMinimized = (vp: Viewport) -> Boolean
typealias Platform_SetWindowTitle = (vp: Viewport, str: String) -> Unit
typealias Platform_SetWindowAlpha = (vp: Viewport, alpha: Float) -> Unit
typealias Platform_UpdateWindow = (vp: Viewport) -> Unit
typealias Platform_RenderWindow = (vp: Viewport, renderArg: Any?) -> Unit
typealias Platform_SwapBuffers = (vp: Viewport, renderArg: Any?) -> Unit
typealias Platform_GetWindowDpiScale = (vp: Viewport) -> Float
typealias Platform_OnChangedViewport = (vp: Viewport) -> Unit
typealias Platform_SetImeInputPos = (vp: Viewport, pos: Vec2) -> Unit
//typealias Platform_CreateVkSurface = (vp: Viewport, vkInst: Long, const void* vk_allocators, ImU64* out_vk_surface); // (Optional) For a Vulkan Renderer to call into Platform code (since the surface creation needs to tie them both).

typealias Renderer_CreateWindow = (vp: Viewport) -> Unit
typealias Renderer_DestroyWindow = (vp: Viewport) -> Unit
typealias Renderer_SetWindowSize = (vp: Viewport, size: Vec2) -> Unit
typealias Renderer_RenderWindow = (vp: Viewport, renderArg: Any?) -> Unit
typealias Renderer_SwapBuffers = (vp: Viewport, renderArg: Any?) -> Unit

/** (Optional) Access via ImGui::GetPlatformIO() */
class PlatformIO {

    //------------------------------------------------------------------
    // Input - Back-end interface/functions + Monitor List
    //------------------------------------------------------------------

    // (Optional) Platform functions (e.g. Win32, GLFW, SDL2)
    // For reference, the second column shows which function are generally calling the Platform Functions:
    //   N = ImGui::NewFrame()                        ~ beginning of the dear imgui frame: read info from platform/OS windows (latest size/position)
    //   F = ImGui::Begin(), ImGui::EndFrame()        ~ during the dear imgui frame
    //   U = ImGui::UpdatePlatformWindows()           ~ after the dear imgui frame: create and update all platform/OS windows
    //   R = ImGui::RenderPlatformWindowsDefault()    ~ render
    //   D = ImGui::DestroyPlatformWindows()          ~ shutdown
    // The general idea is that NewFrame() we will read the current Platform/OS state, and UpdatePlatformWindows() will write to it.
    //
    // The functions are designed so we can mix and match 2 imgui_impl_xxxx files, one for the Platform (~window/input handling), one for Renderer.
    // Custom engine back-ends will often provide both Platform and Renderer interfaces and so may not need to use all functions.
    // Platform functions are typically called before their Renderer counterpart, apart from Destroy which are called the other way.

    // @formatter:off

    // Platform function --------------------------------------------------- Called by -----
    var platform_CreateWindow: Platform_CreateWindow? = null                // . . U . .  // Create a new platform window for the given viewport
    var platform_DestroyWindow: Platform_DestroyWindow? = null              // N . U . D  //
    var platform_ShowWindow: Platform_ShowWindow? = null                    // . . U . .  // Newly created windows are initially hidden so SetWindowPos/Size/Title can be called on them before showing the window
    var platform_SetWindowPos: Platform_SetWindowPos? = null                // . . U . .  // Set platform window position (given the upper-left corner of client area)
    var platform_GetWindowPos: Platform_GetWindowPos? = null                // N . . . .  //
    var platform_SetWindowSize: Platform_SetWindowSize? = null              // . . U . .  // Set platform window client area size (ignoring OS decorations such as OS title bar etc.)
    var platform_GetWindowSize: Platform_GetWindowSize? = null              // N . . . .  // Get platform window client area size
    var platform_SetWindowFocus: Platform_SetWindowFocus? = null            // N . . . .  // Move window to front and set input focus
    var platform_GetWindowFocus: Platform_GetWindowFocus? = null            // . . U . .  //
    var platform_GetWindowMinimized: Platform_GetWindowMinimized? = null    // N . . . .  // Get platform window minimized state. When minimized, we generally won't attempt to get/set size and contents will be culled more easily
    var platform_SetWindowTitle: Platform_SetWindowTitle? = null            // . . U . .  // Set platform window title (given an UTF-8 string)
    var platform_SetWindowAlpha: Platform_SetWindowAlpha? = null            // . . U . .  // (Optional) Setup window transparency
    var platform_UpdateWindow: Platform_UpdateWindow? = null                // . . U . .  // (Optional) Called by UpdatePlatformWindows(). Optional hook to allow the platform back-end from doing general book-keeping every frame.
    var platform_RenderWindow: Platform_RenderWindow? = null                // . . . R .  // (Optional) Main rendering (platform side! This is often unused, or just setting a "current" context for OpenGL bindings). 'render_arg' is the value passed to RenderPlatformWindowsDefault().
    var platform_SwapBuffers: Platform_SwapBuffers? = null                  // . . . R .  // (Optional) Call Present/SwapBuffers (platform side! This is often unused!). 'render_arg' is the value passed to RenderPlatformWindowsDefault().
    var platform_GetWindowDpiScale: Platform_GetWindowDpiScale? = null      // N . . . .  // (Optional) [BETA] FIXME-DPI: DPI handling: Return DPI scale for this viewport. 1.0f = 96 DPI.
    var platform_OnChangedViewport: Platform_OnChangedViewport? = null      // . F . . .  // (Optional) [BETA] FIXME-DPI: DPI handling: Called during Begin() every time the viewport we are outputting into changes, so back-end has a chance to swap fonts to adjust style.
    var platform_SetImeInputPos: Platform_SetImeInputPos? = null            // . F . . .  // (Optional) Set IME (Input Method Editor, e.g. for Asian languages) input position, so text preview appears over the imgui input box. FIXME: The call timing of this is inconsistent because we want to support without multi-viewports.
//    var Platform_CreateVkSurface)(ImGuiViewport* vp, ImU64 vk_inst, const void* vk_allocators, ImU64* out_vk_surface); // (Optional) For a Vulkan Renderer to call into Platform code (since the surface creation needs to tie them both).

    // (Optional) Renderer functions (e.g. DirectX, OpenGL, Vulkan)
    var renderer_CreateWindow: Renderer_CreateWindow? = null                // . . U . .  // Create swap chain, frame buffers etc. (called after Platform_CreateWindow)
    var renderer_DestroyWindow: Renderer_DestroyWindow? = null              // N . U . D  // Destroy swap chain, frame buffers etc. (called before Platform_DestroyWindow)
    var renderer_SetWindowSize: Renderer_SetWindowSize? = null              // . . U . .  // Resize swap chain, frame buffers etc. (called after Platform_SetWindowSize)
    var renderer_RenderWindow: Renderer_RenderWindow? = null                // . . . R .  // (Optional) Clear framebuffer, setup render target, then render the viewport->DrawData. 'render_arg' is the value passed to RenderPlatformWindowsDefault().
    var renderer_SwapBuffers: Renderer_SwapBuffers? = null                  // . . . R .  // (Optional) Call Present/SwapBuffers. 'render_arg' is the value passed to RenderPlatformWindowsDefault().

    // @formatter:on

    // (Optional) Monitor list
    // - Updated by: app/back-end. Update every frame to dynamically support changing monitor or DPI configuration.
    // - Used by: dear imgui to query DPI info, clamp popups/tooltips within same monitor and not have them straddle monitors.
    val monitors = ArrayList<PlatformMonitor>()

    //------------------------------------------------------------------
    // Output - List of viewports to render into platform windows
    //------------------------------------------------------------------

    /** Viewports list (the list is updated by calling ImGui::EndFrame or ImGui::Render)
     *  (in the future we will attempt to organize this feature to remove the need for a "main viewport")
     *  Guaranteed to be == Viewports[0] */
    var mainViewport: Viewport? = null

    /** Main viewports, followed by all secondary viewports. */
    val viewports = ArrayList<Viewport>()
}

/** (Optional) This is required when enabling multi-viewport. Represent the bounds of each connected monitor/display and their DPI.
 *  We use this information for multiple DPI support + clamping the position of popups and tooltips so they don't straddle multiple monitors. */
class PlatformMonitor {
    // Coordinates of the area displayed on this monitor (Min = upper left, Max = bottom right)
    val mainPos = Vec2()
    val mainSize = Vec2()

    // Coordinates without task bars / side bars / menu bars. Used to avoid positioning popups/tooltips inside this region. If you don't have this info, please copy the value for MainPos/MainSize.
    val workPos = Vec2()
    val workSize = Vec2()

    /**  1.0f = 96 DPI */
    var dpiScale = 1f
}