package imguiVk

import glm_.vec2.Vec2i
import imgui.impl.vk.ImplVulkan
import imgui.impl.vk.ImplVulkanH
import imgui.internal.DrawData
import kool.Ptr
import kool.Stack
import kool.adr
import org.lwjgl.system.JNI
import org.lwjgl.system.MemoryUtil
import vkk.*
import vkk.entities.VkSurfaceKHR
import vkk.extensions.*
import vkk.identifiers.Instance
import vkk.identifiers.VK
import vkk.vk10.*
import vkk.vk10.structs.*

fun debugReport(flags: VkDebugReportFlagsEXT, objectType: VkDebugReportObjectTypeEXT, `object`: Long, location: Long, messageCode: Int,
                layerPrefix: String, message: String, userData: Ptr) =
        println("[vulkan] Debug report from ObjectType: $objectType\nMessage: $message")

fun setupVulkan(extensions: ArrayList<String>) {

    // Create Vulkan Instance
    run {
        val createInfo = InstanceCreateInfo(enabledExtensionNames = extensions)

        if (imgui.DEBUG) {
            // Enabling validation layers
            createInfo.enabledLayerNames = listOf("VK_LAYER_KHRONOS_validation")

            // Enable debug report extension (we need additional storage, so we duplicate the user array to add our new extension to it)
            extensions += "VK_EXT_debug_report"
            createInfo.enabledExtensionNames = extensions

            // Create Vulkan Instance
            gInstance = Instance(createInfo)

            // Setup the debug report callback
            val debugReportCi = DebugReportCallbackCreateInfo(flags = VkDebugReport.ERROR_BIT_EXT.i or VkDebugReport.WARNING_BIT_EXT.i or VkDebugReport.PERFORMANCE_WARNING_BIT_EXT.i)
            gDebugReport = gInstance createDebugReportCallbackEXT debugReportCi
            DebugReportCallback.callback = ::debugReport
        } else // Create Vulkan Instance without any debug feature
            gInstance = Instance(createInfo)
    }

    // Select GPU
    run {
        val gpus = gInstance.physicalDevices
        assert(gpus.isNotEmpty())

        // If a number >1 of GPUs got reported, you should find the best fit GPU for your purpose
        // e.g. VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU if available, or with the greatest memory available, etc.
        // for sake of simplicity we'll just take the first one, assuming it has a graphics queue family.
        gPhysicalDevice = gpus[0]
    }

    // Select graphics queue family
    run {
        val queues = gPhysicalDevice.queueFamilyProperties
        gQueueFamily = queues.indexOfFirst { it.queueFlags has VkQueueFlag.GRAPHICS_BIT }
        assert(gQueueFamily != -1)
    }

    // Create Logical Device (with 1 queue)
    run {
        val queueInfo = DeviceQueueCreateInfo(gQueueFamily, queuePriority = 1f)
        val createInfo = DeviceCreateInfo(
                queueCreateInfo = queueInfo,
                enabledExtensionNames = listOf("VK_KHR_swapchain"))
        gDevice = gPhysicalDevice createDevice createInfo
        gQueue = gDevice.getQueue(gQueueFamily)
    }

    // Create Descriptor Pool
    run {
        val poolSizes = arrayOf(
                DescriptorPoolSize(VkDescriptorType.SAMPLER, 1000),
                DescriptorPoolSize(VkDescriptorType.COMBINED_IMAGE_SAMPLER, 1000),
                DescriptorPoolSize(VkDescriptorType.SAMPLED_IMAGE, 1000),
                DescriptorPoolSize(VkDescriptorType.STORAGE_IMAGE, 1000),
                DescriptorPoolSize(VkDescriptorType.UNIFORM_TEXEL_BUFFER, 1000),
                DescriptorPoolSize(VkDescriptorType.STORAGE_TEXEL_BUFFER, 1000),
                DescriptorPoolSize(VkDescriptorType.UNIFORM_BUFFER, 1000),
                DescriptorPoolSize(VkDescriptorType.STORAGE_BUFFER, 1000),
                DescriptorPoolSize(VkDescriptorType.UNIFORM_BUFFER_DYNAMIC, 1000),
                DescriptorPoolSize(VkDescriptorType.STORAGE_BUFFER_DYNAMIC, 1000),
                DescriptorPoolSize(VkDescriptorType.INPUT_ATTACHMENT, 1000))
        val poolInfo = DescriptorPoolCreateInfo(
                flags = VkDescriptorPoolCreate.FREE_DESCRIPTOR_SET_BIT.i,
                maxSets = 1000 * poolSizes.size,
                poolSizes = poolSizes)
        gDescriptorPool = gDevice createDescriptorPool poolInfo
    }
}

/** All the ImGui_ImplVulkanH_XXX structures/functions are optional helpers used by the demo.
 *  Your real engine/app may not use them. */
fun setupVulkanWindow(wd: ImplVulkanH.Window, surface: VkSurfaceKHR, size: Vec2i) {

    wd.surface = surface

    // Check for WSI support
    val res = gPhysicalDevice.getSurfaceSupportKHR(gQueueFamily, wd.surface)
    if (!res) {
        System.err.println("Error no WSI support on physical device 0")
        System.exit(-1)
    }

    // Select Surface Format
    val requestSurfaceImageFormat = arrayOf(VkFormat.B8G8R8A8_UNORM, VkFormat.R8G8B8A8_UNORM, VkFormat.B8G8R8_UNORM, VkFormat.R8G8B8_UNORM)
    val requestSurfaceColorSpace = VkColorSpaceKHR.SRGB_NONLINEAR_KHR
    wd.surfaceFormat = ImplVulkanH.selectSurfaceFormat(gPhysicalDevice, wd.surface, requestSurfaceImageFormat, requestSurfaceColorSpace)

    // Select Present Mode
    val presentModes = when {
        IMGUI_UNLIMITED_FRAME_RATE -> arrayOf(VkPresentModeKHR.MAILBOX, VkPresentModeKHR.IMMEDIATE, VkPresentModeKHR.FIFO)
        else -> arrayOf(VkPresentModeKHR.FIFO)
    }
    wd.presentMode = ImplVulkanH.selectPresentMode(gPhysicalDevice, wd.surface, presentModes)
    //printf("[vulkan] Selected PresentMode = %d\n", wd->PresentMode);

    // Create SwapChain, RenderPass, Framebuffer, etc.
    assert(gMinImageCount >= 2)
    ImplVulkanH.createOrResizeWindow(gInstance, gPhysicalDevice, gDevice, wd, gQueueFamily, size, gMinImageCount)
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

fun frameRender(wd: ImplVulkanH.Window, drawData: DrawData) {

    val imageAcquiredSemaphore = wd.frameSemaphores!![wd.semaphoreIndex].imageAcquiredSemaphore
    val renderCompleteSemaphore = wd.frameSemaphores!![wd.semaphoreIndex].renderCompleteSemaphore
    wd.frameIndex = gDevice.acquireNextImageKHR(wd.swapchain, -1L, imageAcquiredSemaphore) {
        if (it == VkResult.ERROR_OUT_OF_DATE_KHR) {
            gSwapChainRebuild = true
            return@acquireNextImageKHR
        }
    }

    val fd = wd.frames!![wd.frameIndex]
    gDevice.apply {
        waitForFences(fd.fence, waitAll = true, -1L)    // wait indefinitely instead of periodically checking
        resetFences(fd.fence)
    }

    val cmd = fd.commandBuffer!!
    run {
        gDevice.resetCommandPool(fd.commandPool, 0)
        val info = CommandBufferBeginInfo(flags = VkCommandBufferUsage.ONE_TIME_SUBMIT_BIT.i)
        cmd begin info
    }
    run {
        val info = RenderPassBeginInfo(
                renderPass = wd.renderPass,
                framebuffer = fd.framebuffer,
                renderArea = Rect2D(extent = Extent2D(wd.size)),
                clearValue = wd.clearValue)
        cmd.beginRenderPass(info, VkSubpassContents.INLINE)
    }

    // Record dear imgui primitives into command buffer
    ImplVulkan.renderDrawData(drawData, cmd)

    // Submit command buffer
    cmd.endRenderPass()
    run {
        val info = SubmitInfo(
                waitSemaphoreCount = 1,
                waitSemaphore = imageAcquiredSemaphore,
                waitDstStageMask = VkPipelineStage.COLOR_ATTACHMENT_OUTPUT_BIT.i,
                commandBuffer = cmd,
                signalSemaphore = renderCompleteSemaphore)

        cmd.end()
        gQueue.submit(info, fd.fence)
    }
}

fun framePresent(wd: ImplVulkanH.Window) {
    if (gSwapChainRebuild)
        return
    val renderCompleteSemaphore = wd.frameSemaphores!![wd.semaphoreIndex].renderCompleteSemaphore
    val info = PresentInfoKHR(
            waitSemaphore = renderCompleteSemaphore,
            swapchain = wd.swapchain,
            imageIndex = wd.frameIndex)
    val err = gQueue.presentKHR(info)
    if (err == VkResult.ERROR_OUT_OF_DATE_KHR) {
        gSwapChainRebuild = true
        return
    }
    wd.semaphoreIndex = (wd.semaphoreIndex + 1) % wd.imageCount // Now we can use the next set of semaphores
}