package imgui.impl.vk

import glm_.i
import glm_.vec2.Vec2i
import org.lwjgl.vulkan.VK10.VK_SUBPASS_EXTERNAL
import vkk.*
import vkk.entities.*
import vkk.extensions.*
import vkk.identifiers.CommandBuffer
import vkk.identifiers.Device
import vkk.identifiers.Instance
import vkk.identifiers.PhysicalDevice
import vkk.vk10.*
import vkk.vk10.structs.*

object ImplVulkanH {

    /** Helper structure to hold the data needed by one rendering frame
     *  (Used by example's main.cpp. Used by multi-viewport features. Probably NOT used by your own engine/app.)
     *  [Please zero-clear before use!]
     *  ~ImGui_ImplVulkanH_Frame */
    class Frame {
        var commandPool = VkCommandPool.NULL
        var commandBuffer: CommandBuffer? = null
        var fence = VkFence.NULL
        var backbuffer = VkImage.NULL
        var backbufferView = VkImageView.NULL
        var framebuffer = VkFramebuffer.NULL
    }

    /** ~ImGui_ImplVulkanH_FrameSemaphores */
    class FrameSemaphores {
        var imageAcquiredSemaphore = VkSemaphore.NULL
        var renderCompleteSemaphore = VkSemaphore.NULL
    }

    /** Helper structure to hold the data needed by one rendering context into one OS window
     *  (Used by example's main.cpp. Used by multi-viewport features. Probably NOT used by your own engine/app.)
     *  ~ImGui_ImplVulkanH_Window */
    class Window {
        val size = Vec2i()
        var swapchain = VkSwapchainKHR.NULL
        var surface = VkSurfaceKHR.NULL
        var surfaceFormat = SurfaceFormatKHR()
        var presentMode = VkPresentModeKHR(-1)
        var renderPass = VkRenderPass.NULL

        /** The window pipeline may uses a different VkRenderPass than the one passed in ImGui_ImplVulkan_InitInfo */
        var pipeline = VkPipeline.NULL
        var clearEnable = true
        var clearValue = ClearValue()

        /** Current frame being rendered to (0 <= FrameIndex < FrameInFlightCount) */
        var frameIndex = 0

        /** Number of simultaneous in-flight frames (returned by vkGetSwapchainImagesKHR, usually derived from min_image_count) */
        var imageCount = 0

        /** Current set of swapchain wait semaphores we're using (needs to be distinct from per frame data) */
        var semaphoreIndex = 0
        var frames: Array<Frame>? = null
        var frameSemaphores: Array<FrameSemaphores>? = null

        fun reset() {
            size put 0
            swapchain = VkSwapchainKHR.NULL
            surface = VkSurfaceKHR.NULL
            surfaceFormat = SurfaceFormatKHR()
            presentMode = VkPresentModeKHR(-1)
            renderPass = VkRenderPass.NULL
            pipeline = VkPipeline.NULL
            clearEnable = true
            clearValue = ClearValue()
            frameIndex = 0
            imageCount = 0
            semaphoreIndex = 0
            frames = null
            frameSemaphores = null
        }
    }

    /** Reusable buffers used for rendering 1 current in-flight frame, for ImGui_ImplVulkan_RenderDrawData()
     *  [Please zero-clear before use!]
     *  ~ImGui_ImplVulkanH_FrameRenderBuffers */
    class FrameRenderBuffers {
        var vertexBufferMemory = VkDeviceMemory.NULL
        var indexBufferMemory = VkDeviceMemory.NULL
        var vertexBufferSize = VkDeviceSize.NULL
        var indexBufferSize = VkDeviceSize.NULL
        var vertexBuffer = VkBuffer.NULL
        var indexBuffer = VkBuffer.NULL
    }

    /** Each viewport will hold 1 ImGui_ImplVulkanH_WindowRenderBuffers
     *  [Please zero-clear before use!]
     *  ~ImGui_ImplVulkanH_WindowRenderBuffers */
    class WindowRenderBuffers {
        var index = 0
        var frameRenderBuffers: Array<FrameRenderBuffers>? = null
        val count: Int
            get() = frameRenderBuffers?.size ?: 0
    }

    // Forward Declarations
    //    bool ImGui_ImplVulkan_CreateDeviceObjects();
    //    void ImGui_ImplVulkan_DestroyDeviceObjects();
    //    void ImGui_ImplVulkanH_DestroyFrame(VkDevice device, ImGui_ImplVulkanH_Frame* fd, const VkAllocationCallbacks* allocator);
    //    void ImGui_ImplVulkanH_DestroyFrameSemaphores(VkDevice device, ImGui_ImplVulkanH_FrameSemaphores* fsd, const VkAllocationCallbacks* allocator);
    //    void ImGui_ImplVulkanH_DestroyFrameRenderBuffers(VkDevice device, ImGui_ImplVulkanH_FrameRenderBuffers* buffers, const VkAllocationCallbacks* allocator);
    //    void ImGui_ImplVulkanH_DestroyWindowRenderBuffers(VkDevice device, ImGui_ImplVulkanH_WindowRenderBuffers* buffers, const VkAllocationCallbacks* allocator);
    //    void ImGui_ImplVulkanH_CreateWindowSwapChain(VkPhysicalDevice physical_device, VkDevice device, ImGui_ImplVulkanH_Window* wd, const VkAllocationCallbacks* allocator, int w, int h, uint32_t min_image_count);
    //    void ImGui_ImplVulkanH_CreateWindowCommandBuffers(VkPhysicalDevice physical_device, VkDevice device, ImGui_ImplVulkanH_Window* wd, uint32_t queue_family, const VkAllocationCallbacks* allocator);

    /** ~ImGui_ImplVulkanH_DestroyFrame */
    fun destroyFrame(device: Device, fd: Frame) = with(device) {
        destroy(fd.fence)
        freeCommandBuffers(fd.commandPool, fd.commandBuffer!!)
        destroy(fd.commandPool)
        fd.fence = VkFence.NULL
        fd.commandBuffer = null
        fd.commandPool = VkCommandPool.NULL

        destroy(fd.backbufferView)
        destroy(fd.framebuffer)
    }

    /** ~ImGui_ImplVulkanH_DestroyFrameSemaphores */
    fun destroyFrameSemaphores(device: Device, fsd: FrameSemaphores) = with(device) {
        destroy(fsd.imageAcquiredSemaphore)
        destroy(fsd.renderCompleteSemaphore)
        fsd.imageAcquiredSemaphore = VkSemaphore.NULL
        fsd.renderCompleteSemaphore = VkSemaphore.NULL
    }

    /** ~ImGui_ImplVulkanH_DestroyFrameRenderBuffers */
    fun destroyFrameRenderBuffers(device: Device, buffers: FrameRenderBuffers) {
        device.apply {
            if (buffers.vertexBuffer.isValid) {
                destroy(buffers.vertexBuffer)
                buffers.vertexBuffer = VkBuffer.NULL
            }
            if (buffers.vertexBufferMemory.isValid) {
                freeMemory(buffers.vertexBufferMemory)
                buffers.vertexBufferMemory = VkDeviceMemory.NULL
            }
            if (buffers.indexBuffer.isValid) {
                destroy(buffers.indexBuffer)
                buffers.indexBuffer = VkBuffer.NULL
            }
            if (buffers.indexBufferMemory.isValid) {
                freeMemory(buffers.indexBufferMemory)
                buffers.indexBufferMemory = VkDeviceMemory.NULL
            }
        }
        buffers.vertexBufferSize = VkDeviceSize.NULL
        buffers.indexBufferSize = VkDeviceSize.NULL
    }

    /** ~ImGui_ImplVulkanH_DestroyWindowRenderBuffers */
    fun destroyWindowRenderBuffers(device: Device, buffers: WindowRenderBuffers) {
        for (frameRenderBuffer in buffers.frameRenderBuffers!!)
            destroyFrameRenderBuffers(device, frameRenderBuffer)
        buffers.frameRenderBuffers = null
        buffers.index = 0
    }

    /** Also destroy old swap chain and in-flight frames data, if any.
     *  ~ImGui_ImplVulkanH_CreateWindowSwapChain */
    fun createWindowSwapChain(physicalDevice: PhysicalDevice, device: Device, wd: Window, size: Vec2i,
                              minImageCount_: Int) {

        var minImageCount = minImageCount_

        val oldSwapchain = wd.swapchain
        wd.swapchain = VkSwapchainKHR.NULL
        device.waitIdle()

        // We don't use ImGui_ImplVulkanH_DestroyWindow() because we want to preserve the old swapchain to create the new one.
        // Destroy old Framebuffer
        repeat(wd.imageCount) {
            destroyFrame(device, wd.frames!![it])
            destroyFrameSemaphores(device, wd.frameSemaphores!![it])
        }
        wd.frames = null
        wd.frameSemaphores = null
        wd.imageCount = 0
        if (wd.renderPass.isValid)
            device destroy wd.renderPass
        if (wd.pipeline.isValid)
            device destroy wd.pipeline

        // If min image count was not specified, request different count of images dependent on selected present mode
        if (minImageCount == 0)
            minImageCount = wd.presentMode.minImageCount

        // Create Swapchain
        run {
            val info = SwapchainCreateInfoKHR(
                    surface = wd.surface,
                    minImageCount = minImageCount,
                    imageFormat = wd.surfaceFormat.format,
                    imageColorSpace = wd.surfaceFormat.colorSpace,
                    imageArrayLayers = 1,
                    imageUsage = VkImageUsage.COLOR_ATTACHMENT_BIT.i,
                    imageSharingMode = VkSharingMode.EXCLUSIVE,           // Assume that graphics family == present family
                    preTransform = VkSurfaceTransformKHR.IDENTITY_BIT,
                    compositeAlpha = VkCompositeAlphaKHR.OPAQUE_BIT,
                    presentMode = wd.presentMode,
                    clipped = true,
                    oldSwapchain = oldSwapchain)
            val cap = physicalDevice getSurfaceCapabilitiesKHR wd.surface
            if (info.minImageCount < cap.minImageCount)
                info.minImageCount = cap.minImageCount
            else if (cap.maxImageCount != 0 && info.minImageCount > cap.maxImageCount)
                info.minImageCount = cap.maxImageCount

            if (cap.currentExtent.width == 0xffffffff.i) {
                info.imageExtent.size = Vec2i(size)
                wd.size put size
            } else {
                info.imageExtent.size = cap.currentExtent.size
                wd.size put cap.currentExtent.size
            }
            wd.swapchain = device createSwapchainKHR info
            val backbuffers = device getSwapchainImagesKHR wd.swapchain
            wd.imageCount = backbuffers.size
            assert(wd.imageCount >= minImageCount)

            assert(wd.frames == null)
            wd.frames = Array(wd.imageCount) { Frame() }
            wd.frameSemaphores = Array(wd.imageCount) { FrameSemaphores() }
            wd.frames!!.forEachIndexed { i, frame ->
                frame.backbuffer = backbuffers[i]
            }
        }
        if (oldSwapchain.isValid)
            device destroy oldSwapchain

        // Create the Render Pass
        run {
            val attachment = AttachmentDescription(
                    format = wd.surfaceFormat.format,
                    samples = VkSampleCount._1_BIT,
                    loadOp = if (wd.clearEnable) VkAttachmentLoadOp.CLEAR else VkAttachmentLoadOp.DONT_CARE,
                    storeOp = VkAttachmentStoreOp.STORE,
                    stencilLoadOp = VkAttachmentLoadOp.DONT_CARE,
                    stencilStoreOp = VkAttachmentStoreOp.DONT_CARE,
                    initialLayout = VkImageLayout.UNDEFINED,
                    finalLayout = VkImageLayout.PRESENT_SRC_KHR)
            val colorAttachment = AttachmentReference(attachment = 0, VkImageLayout.COLOR_ATTACHMENT_OPTIMAL)
            val subpass = SubpassDescription(pipelineBindPoint = VkPipelineBindPoint.GRAPHICS, colorAttachment = colorAttachment)
            val dependency = SubpassDependency(
                    srcSubpass = VK_SUBPASS_EXTERNAL,
                    dstSubpass = 0,
                    srcStageMask = VkPipelineStage.COLOR_ATTACHMENT_OUTPUT_BIT.i,
                    dstStageMask = VkPipelineStage.COLOR_ATTACHMENT_OUTPUT_BIT.i,
                    srcAccessMask = 0,
                    dstAccessMask = VkAccess.COLOR_ATTACHMENT_WRITE_BIT.i)
            val info = RenderPassCreateInfo(
                    attachment = attachment,
                    subpass = subpass,
                    dependency = dependency)
            wd.renderPass = device createRenderPass info

            // We do not create a pipeline by default as this is also used by examples' main.cpp,
            // but secondary viewport in multi-viewport mode may want to create one with:
            //ImGui_ImplVulkan_CreatePipeline(device, allocator, VK_NULL_HANDLE, wd->RenderPass, VK_SAMPLE_COUNT_1_BIT, &wd->Pipeline, g_Subpass);
        }

        // Create The Image Views
        run {
            val info = ImageViewCreateInfo(
                    viewType = VkImageViewType._2D,
                    format = wd.surfaceFormat.format,
                    components = ComponentMapping(),
                    subresourceRange = ImageSubresourceRange(VkImageAspect.COLOR_BIT.i, 0, 1, 0, 1))
            for (fd in wd.frames!!) {
                info.image = fd.backbuffer
                fd.backbufferView = device createImageView info
            }
        }

        // Create Framebuffer
        run {
            val attachment = VkImageView_Array(1)
            val info = FramebufferCreateInfo(
                    renderPass = wd.renderPass,
                    attachments = attachment,
                    width = wd.size.x,
                    height = wd.size.y,
                    layers = 1)
            for (fd in wd.frames!!) {
                attachment[0] = fd.backbufferView
                fd.framebuffer = device createFramebuffer info
            }
        }
    }

    /** ~ImGui_ImplVulkanH_CreateWindowCommandBuffers */
    fun createWindowCommandBuffers(physicalDevice: PhysicalDevice, device: Device, wd: Window, queueFamily: Int) {

        assert(physicalDevice.isValid && device.isValid)
        //        (void)physical_device;
        //        (void)allocator;

        // Create Command Buffers
        repeat(wd.imageCount) {
            val fd = wd.frames!![it]
            val fsd = wd.frameSemaphores!![it]
            run {
                val info = CommandPoolCreateInfo(VkCommandPoolCreate.RESET_COMMAND_BUFFER_BIT.i, queueFamily)
                fd.commandPool = device createCommandPool info
            }
            run {
                val info = CommandBufferAllocateInfo(fd.commandPool, VkCommandBufferLevel.PRIMARY, 1)
                fd.commandBuffer = device allocateCommandBuffer info
            }
            run {
                val info = FenceCreateInfo(VkFenceCreate.SIGNALED_BIT.i)
                fd.fence = device.createFence(info)
            }
            run {
                val info = SemaphoreCreateInfo()
                fsd.imageAcquiredSemaphore = device.createSemaphore(info)
                fsd.renderCompleteSemaphore = device.createSemaphore(info)
            }
        }
    }

    // Helpers
    //    IMGUI_IMPL_API void                 ImGui_ImplVulkanH_CreateOrResizeWindow(VkInstance instance, VkPhysicalDevice physical_device, VkDevice device, ImGui_ImplVulkanH_Window* wnd, uint32_t queue_family, const VkAllocationCallbacks* allocator, int w, int h, uint32_t min_image_count);
    //    IMGUI_IMPL_API void                 ImGui_ImplVulkanH_DestroyWindow(VkInstance instance, VkDevice device, ImGui_ImplVulkanH_Window* wnd, const VkAllocationCallbacks* allocator);
    //    IMGUI_IMPL_API VkSurfaceFormatKHR   ImGui_ImplVulkanH_SelectSurfaceFormat(VkPhysicalDevice physical_device, VkSurfaceKHR surface, const VkFormat* request_formats, int request_formats_count, VkColorSpaceKHR request_color_space);
    //    IMGUI_IMPL_API VkPresentModeKHR     ImGui_ImplVulkanH_SelectPresentMode(VkPhysicalDevice physical_device, VkSurfaceKHR surface, const VkPresentModeKHR* request_modes, int request_modes_count);
    //    IMGUI_IMPL_API int                  ImGui_ImplVulkanH_GetMinImageCountFromPresentMode(VkPresentModeKHR present_mode);

    /** Create or resize window
     *  ~ImGui_ImplVulkanH_CreateOrResizeWindow */
    fun createOrResizeWindow(instance: Instance, physicalDevice: PhysicalDevice, device: Device, wd: Window,
                             queueFamily: Int, size: Vec2i, minImageCount: Int) {
        //        (void)instance;
        createWindowSwapChain(physicalDevice, device, wd, size, minImageCount)
        createWindowCommandBuffers(physicalDevice, device, wd, queueFamily)
    }

    /** ~ImGui_ImplVulkanH_DestroyWindow */
    fun destroyWindow(instance: Instance, device: Device, wd: Window) {

        device.waitIdle() // FIXME: We could wait on the Queue if we had the queue in wd-> (otherwise VulkanH functions can't use globals)
        //vkQueueWaitIdle(g_Queue);

        repeat(wd.imageCount) {
            destroyFrame(device, wd.frames!![it])
            destroyFrameSemaphores(device, wd.frameSemaphores!![it])
        }
        wd.frames = null
        wd.frameSemaphores = null
        device.apply {
            destroy(wd.pipeline)
            destroy(wd.renderPass)
            destroy(wd.swapchain)
        }
        instance destroy wd.surface
        wd.reset()
    }

    /** ~ImGui_ImplVulkanH_SelectSurfaceFormat */
    fun selectSurfaceFormat(physicalDevice: PhysicalDevice, surface: VkSurfaceKHR, requestFormats: Array<VkFormat>,
                            requestColorSpace: VkColorSpaceKHR): SurfaceFormatKHR {

        assert(requestFormats.isNotEmpty())

        // Per Spec Format and View Format are expected to be the same unless VK_IMAGE_CREATE_MUTABLE_BIT was set at image creation
        // Assuming that the default behavior is without setting this bit, there is no need for separate Swapchain image and image view format
        // Additionally several new color spaces were introduced with Vulkan Spec v1.0.40,
        // hence we must make sure that a format with the mostly available color space, VK_COLOR_SPACE_SRGB_NONLINEAR_KHR, is found and used.
        val availFormats = physicalDevice getSurfaceFormatsKHR surface

        // First check if only one format, VK_FORMAT_UNDEFINED, is available, which would imply that any format is available
        when (availFormats.size) {
            1 -> return when (availFormats[0].format) {
                VkFormat.UNDEFINED -> SurfaceFormatKHR(requestFormats[0], requestColorSpace)
                else -> availFormats[0] // No point in searching another format
            }
            else -> {
                // Request several formats, the first found will be used
                for (requestFormat in requestFormats)
                    for (availFormat in availFormats)
                        if (availFormat.format == requestFormat && availFormat.colorSpace == requestColorSpace)
                            return availFormat

                // If none of the requested image formats could be found, use the first available
                return availFormats[0]
            }
        }
    }

    /** ~ImGui_ImplVulkanH_SelectPresentMode */
    fun selectPresentMode(physicalDevice: PhysicalDevice, surface: VkSurfaceKHR, requestModes: Array<VkPresentModeKHR>)
            : VkPresentModeKHR {

        assert(requestModes.isNotEmpty())

        // Request a certain mode and confirm that it is available. If not use VK_PRESENT_MODE_FIFO_KHR which is mandatory
        val availModes = physicalDevice getSurfacePresentModesKHR surface
        //for (uint32_t avail_i = 0; avail_i < avail_count; avail_i++)
        //    printf("[vulkan] avail_modes[%d] = %d\n", avail_i, avail_modes[avail_i]);

        for (requestMode in requestModes)
            for (availMode in availModes.array)
                if (requestMode.i == availMode)
                    return requestMode

        return VkPresentModeKHR.FIFO // Always available
    }

    /** ~ImGui_ImplVulkanH_GetMinImageCountFromPresentMode */
    val VkPresentModeKHR.minImageCount: Int
        get() = when (this) {
            VkPresentModeKHR.MAILBOX -> 3
            VkPresentModeKHR.FIFO, VkPresentModeKHR.FIFO_RELAXED -> 2
            VkPresentModeKHR.IMMEDIATE -> 1
            else -> error("invalid value")
        }
}