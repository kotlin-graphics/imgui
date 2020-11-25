package imguiVk

import glm_.vec4.Vec4
import imgui.ImGui
import imgui.classes.Context
import imgui.impl.glfw.ImplGlfw
import imgui.impl.vk.ImplVulkan
import imgui.impl.vk.ImplVulkanH
import org.lwjgl.system.MemoryStack
import uno.glfw.GlfwWindow
import uno.glfw.glfw
import uno.glfw.windowHint.Api
import uno.vk.createSurface
import uno.vk.requiredInstanceExtensions
import uno.vk.vulkanSupported
import vkk.*
import vkk.entities.VkDebugReportCallback
import vkk.entities.VkDescriptorPool
import vkk.entities.VkPipelineCache
import vkk.identifiers.Device
import vkk.identifiers.Instance
import vkk.identifiers.PhysicalDevice
import vkk.identifiers.Queue
import vkk.vk10.*
import vkk.vk10.structs.*
import kotlin.system.exitProcess

var IMGUI_UNLIMITED_FRAME_RATE = false

lateinit var gInstance: Instance
lateinit var gPhysicalDevice: PhysicalDevice
lateinit var gDevice: Device
var gQueueFamily = -1
lateinit var gQueue: Queue
var gDebugReport = VkDebugReportCallback.NULL
var gPipelineCache = VkPipelineCache.NULL
var gDescriptorPool = VkDescriptorPool.NULL

val gMainWindowData = ImplVulkanH.Window()
var gMinImageCount = 2
var gSwapChainRebuild = false

fun main() {
    ImGuiVulkan()
}

private class ImGuiVulkan {

    val window: GlfwWindow
    val ctx: Context

    var f = 0f
    val clearColor = Vec4(0.45f, 0.55f, 0.6f, 1f)

    // Our state
    var showAnotherWindow = false
    var showDemoWindow = true
    var counter = 0

    val implGlfw: ImplGlfw
    val implVk: ImplVulkan
    val wd: ImplVulkanH.Window

    init {

        // Setup GLFW window
        glfw {
            errorCallback = { error, description -> println("Glfw Error $error: $description") }
            init()
            windowHint { api = Api.None }
        }
        window = GlfwWindow(1280, 720, "Dear ImGui GLFW+Vulkan example")

        // Setup Vulkan
        if (!glfw.vulkanSupported) {
            System.err.println("GLFW: Vulkan Not Supported")
            exitProcess(1)
        }
        setupVulkan(glfw.requiredInstanceExtensions)

        // Create Window Surface
        val surface = gInstance createSurface window

        // Create Framebuffers
        val size = window.framebufferSize
        wd = gMainWindowData
        setupVulkanWindow(wd, surface, size)

        // Setup Dear ImGui context
        ctx = Context()
        //io.ConfigFlags |= ImGuiConfigFlags_NavEnableKeyboard;     // Enable Keyboard Controls
        //io.ConfigFlags |= ImGuiConfigFlags_NavEnableGamepad;      // Enable Gamepad Controls

        // Setup Dear ImGui style
        ImGui.styleColorsDark()
        //ImGui::StyleColorsClassic();

        // Setup Platform/Renderer backends
        implGlfw = ImplGlfw.initForVulkan(window, true)
        val initInfo = ImplVulkan.InitInfo().also {
            it.instance = gInstance
            it.physicalDevice = gPhysicalDevice
            it.device = gDevice
            it.queueFamily = gQueueFamily
            it.queue = gQueue
            it.pipelineCache = gPipelineCache
            it.descriptorPool = gDescriptorPool
            it.minImageCount = gMinImageCount
            it.imageCount = wd.imageCount
        }
        implVk = ImplVulkan(initInfo, wd.renderPass)

        // Load Fonts
        // - If no fonts are loaded, dear imgui will use the default font. You can also load multiple fonts and use ImGui::PushFont()/PopFont() to select them.
        // - AddFontFromFileTTF() will return the ImFont* so you can store it if you need to select the font among multiple.
        // - If the file cannot be loaded, the function will return NULL. Please handle those errors in your application (e.g. use an assertion, or display an error and quit).
        // - The fonts will be rasterized at a given size (w/ oversampling) and stored into a texture when calling ImFontAtlas::Build()/GetTexDataAsXXXX(), which ImGui_ImplXXXX_NewFrame below will call.
        // - Read 'docs/FONTS.md' for more instructions and details.
        // - Remember that in C/C++ if you want to include a backslash \ in a string literal you need to write a double backslash \\ !
        //io.Fonts->AddFontDefault();
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/Roboto-Medium.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/Cousine-Regular.ttf", 15.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/DroidSans.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/ProggyTiny.ttf", 10.0f);
        //ImFont* font = io.Fonts->AddFontFromFileTTF("c:\\Windows\\Fonts\\ArialUni.ttf", 18.0f, NULL, io.Fonts->GetGlyphRangesJapanese());
        //IM_ASSERT(font != NULL);

        // Upload Fonts
        run {
            // Use any command queue
            val commandPool = wd.frames!![wd.frameIndex].commandPool
            val commandBuffer = wd.frames!![wd.frameIndex].commandBuffer!!

            gDevice.resetCommandPool(commandPool, 0)
            val beginInfo = CommandBufferBeginInfo(flags = VkCommandBufferUsage.ONE_TIME_SUBMIT_BIT.i)
            commandBuffer.begin(beginInfo)

            implVk.createFontsTexture(commandBuffer)

            val endInfo = SubmitInfo(commandBuffer = commandBuffer)
            commandBuffer.end()
            gQueue.submit(endInfo)

            gDevice.waitIdle()
            implVk.destroyFontUploadObjects()
        }

        /*  Main loop
            This automatically also polls events, swaps buffers and resets the appBuffer

            Poll and handle events (inputs, window resize, etc.)
            You can read the io.wantCaptureMouse, io.wantCaptureKeyboard flags to tell if dear imgui wants to use your inputs.
            - When io.wantCaptureMouse is true, do not dispatch mouse input data to your main application.
            - When io.wantCaptureKeyboard is true, do not dispatch keyboard input data to your main application.
            Generally you may always pass all inputs to dear imgui, and hide them from your application based on those two flags.          */
        window.autoSwap = false // disabled for Vulkan
        window.loop(::mainLoop)
    }

    fun mainLoop(stack: MemoryStack) {

        // Resize swap chain?
        if (gSwapChainRebuild) {
            val size = window.framebufferSize
            if (size anyGreaterThan 0) {
                implVk.setMinImageCount(gMinImageCount)
                ImplVulkanH.createOrResizeWindow(gInstance, gPhysicalDevice, gDevice, gMainWindowData, gQueueFamily, size, gMinImageCount)
                gMainWindowData.frameIndex = 0
                gSwapChainRebuild = false
            }
        }

        // Start the Dear ImGui frame
        implVk.newFrame()
        implGlfw.newFrame()
        ImGui.run {
            newFrame()

            // 1. Show the big demo window (Most of the sample code is in ImGui::ShowDemoWindow()! You can browse its code to learn more about Dear ImGui!).
            if (showDemoWindow)
                showDemoWindow(::showDemoWindow)

                // 2. Show a simple window that we create ourselves. We use a Begin/End pair to created a named window.
                run {

                    begin("Hello, world!")                          // Create a window called "Hello, world!" and append into it.

                    text("This is some useful text.")               // Display some text (you can use a format strings too)
                    checkbox("Demo Window", ::showDemoWindow)      // Edit bools storing our window open/close state
                    checkbox("Another Window", ::showAnotherWindow)

                    sliderFloat("float", ::f, 0f, 1f)            // Edit 1 float using a slider from 0.0f to 1.0f
                    colorEdit3("clear color", clearColor) // Edit 3 floats representing a color

                    if (button("Button"))                            // Buttons return true when clicked (most widgets return true when edited/activated)
                        counter++

                    /*  Or you can take advantage of functional programming and pass directly a lambda as last parameter:
                    button("Button") { counter++ }                */

                    sameLine()
                    text("counter = $counter")

                    text("Application average %.3f ms/frame (%.1f FPS)", 1000f / io.framerate, io.framerate)
                    end()
                }

            // 3. Show another simple window.
            if (showAnotherWindow) {
                begin("Another Window", ::showAnotherWindow)   // Pass a pointer to our bool variable (the window will have a closing button that will clear the bool when clicked)
                text("Hello from another window!")
                if (button("Close Me"))
                    showAnotherWindow = false
                end()
            }
        }

        // Rendering
        ImGui.render()
        val drawData = ImGui.drawData!!
        val isMinimized = drawData.displaySize anyLessThanEqual 0f
        if (!isMinimized) {
            wd.clearValue.colorVec4 = clearColor
            frameRender(wd, drawData)
            framePresent(wd)
        }
    }
}


//static void CleanupVulkan()
//{
//    vkDestroyDescriptorPool(g_Device, g_DescriptorPool, g_Allocator);
//
//    #ifdef IMGUI_VULKAN_DEBUG_REPORT
//        // Remove the debug report callback
//        auto vkDestroyDebugReportCallbackEXT = (PFN_vkDestroyDebugReportCallbackEXT)vkGetInstanceProcAddr(g_Instance, "vkDestroyDebugReportCallbackEXT");
//    vkDestroyDebugReportCallbackEXT(g_Instance, g_DebugReport, g_Allocator);
//    #endif // IMGUI_VULKAN_DEBUG_REPORT
//
//    vkDestroyDevice(g_Device, g_Allocator);
//    vkDestroyInstance(g_Instance, g_Allocator);
//}
//
//static void CleanupVulkanWindow()
//{
//    ImGui_ImplVulkanH_DestroyWindow(g_Instance, g_Device, &g_MainWindowData, g_Allocator);
//}
//
//static void FrameRender(ImGui_ImplVulkanH_Window* wd)
//{
//    VkResult err;
//
//    VkSemaphore image_acquired_semaphore  = wd->FrameSemaphores[wd->SemaphoreIndex].ImageAcquiredSemaphore;
//    VkSemaphore render_complete_semaphore = wd->FrameSemaphores[wd->SemaphoreIndex].RenderCompleteSemaphore;
//    err = vkAcquireNextImageKHR(g_Device, wd->Swapchain, UINT64_MAX, image_acquired_semaphore, VK_NULL_HANDLE, &wd->FrameIndex);
//    check_vk_result(err);
//
//    ImGui_ImplVulkanH_Frame* fd = &wd->Frames[wd->FrameIndex];
//    {
//        err = vkWaitForFences(g_Device, 1, &fd->Fence, VK_TRUE, UINT64_MAX);    // wait indefinitely instead of periodically checking
//        check_vk_result(err);
//
//        err = vkResetFences(g_Device, 1, &fd->Fence);
//        check_vk_result(err);
//    }
//    {
//        err = vkResetCommandPool(g_Device, fd->CommandPool, 0);
//        check_vk_result(err);
//        VkCommandBufferBeginInfo info = {};
//        info.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
//        info.flags |= VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
//        err = vkBeginCommandBuffer(fd->CommandBuffer, &info);
//        check_vk_result(err);
//    }
//    {
//        VkRenderPassBeginInfo info = {};
//        info.sType = VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
//        info.renderPass = wd->RenderPass;
//        info.framebuffer = fd->Framebuffer;
//        info.renderArea.extent.width = wd->Width;
//        info.renderArea.extent.height = wd->Height;
//        info.clearValueCount = 1;
//        info.pClearValues = &wd->ClearValue;
//        vkCmdBeginRenderPass(fd->CommandBuffer, &info, VK_SUBPASS_CONTENTS_INLINE);
//    }
//
//    // Record Imgui Draw Data and draw funcs into command buffer
//    ImGui_ImplVulkan_RenderDrawData(ImGui::GetDrawData(), fd->CommandBuffer);
//
//    // Submit command buffer
//    vkCmdEndRenderPass(fd->CommandBuffer);
//    {
//        VkPipelineStageFlags wait_stage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
//        VkSubmitInfo info = {};
//        info.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
//        info.waitSemaphoreCount = 1;
//        info.pWaitSemaphores = &image_acquired_semaphore;
//        info.pWaitDstStageMask = &wait_stage;
//        info.commandBufferCount = 1;
//        info.pCommandBuffers = &fd->CommandBuffer;
//        info.signalSemaphoreCount = 1;
//        info.pSignalSemaphores = &render_complete_semaphore;
//
//        err = vkEndCommandBuffer(fd->CommandBuffer);
//        check_vk_result(err);
//        err = vkQueueSubmit(g_Queue, 1, &info, fd->Fence);
//        check_vk_result(err);
//    }
//}
//
//static void FramePresent(ImGui_ImplVulkanH_Window* wd)
//{
//    VkSemaphore render_complete_semaphore = wd->FrameSemaphores[wd->SemaphoreIndex].RenderCompleteSemaphore;
//    VkPresentInfoKHR info = {};
//    info.sType = VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
//    info.waitSemaphoreCount = 1;
//    info.pWaitSemaphores = &render_complete_semaphore;
//    info.swapchainCount = 1;
//    info.pSwapchains = &wd->Swapchain;
//    info.pImageIndices = &wd->FrameIndex;
//    VkResult err = vkQueuePresentKHR(g_Queue, &info);
//    check_vk_result(err);
//    wd->SemaphoreIndex = (wd->SemaphoreIndex + 1) % wd->ImageCount; // Now we can use the next set of semaphores
//}
//
//
//static void glfw_resize_callback(GLFWwindow*, int w, int h)
//{
//    g_SwapChainRebuild = true;
//    g_SwapChainResizeWidth = w;
//    g_SwapChainResizeHeight = h;
//}
//
