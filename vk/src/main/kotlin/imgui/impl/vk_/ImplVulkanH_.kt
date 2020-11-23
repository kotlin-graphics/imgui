package imgui.impl.vk_

import glm_.vec2.Vec2i
import kool.*
import kool.lib.indices
import kool.lib.isNotEmpty
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import java.nio.IntBuffer

val pP = MemoryUtil.memCallocPointer(1)
val pL = MemoryUtil.memCallocLong(1)
val pI = MemoryUtil.memCallocInt(1)

object ImplVulkanH_ {

    /** Helper structure to hold the data needed by one rendering frame
     *  (Used by example's main.cpp. Used by multi-viewport features. Probably NOT used by your own engine/app.)
     *  [Please zero-clear before use!]
     *  ~ImGui_ImplVulkanH_Frame */
    class Frame {
        var commandPool = 0L
        var commandBuffer: VkCommandBuffer? = null
        var fence = 0L
        var backbuffer = 0L
        var backbufferView = 0L
        var framebuffer = 0L
    }

    /** ~ImGui_ImplVulkanH_FrameSemaphores */
    class FrameSemaphores {
        var imageAcquiredSemaphore = LongBuffer(1)
        var renderCompleteSemaphore = LongBuffer(1)
    }

    /** Helper structure to hold the data needed by one rendering context into one OS window
     *  (Used by example's main.cpp. Used by multi-viewport features. Probably NOT used by your own engine/app.)
     *  ~ImGui_ImplVulkanH_Window */
    class Window {
        val size = Vec2i()
        var swapchain = 0L
        var surface = 0L
        var surfaceFormat = VkSurfaceFormatKHR.create()
        var presentMode = -1
        var renderPass = 0L

        /** The window pipeline may uses a different VkRenderPass than the one passed in ImGui_ImplVulkan_InitInfo */
        var pipeline = 0L
        var clearEnable = true
        var clearValue = VkClearValue.create(1)

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
            swapchain = 0L
            surface = 0L
            surfaceFormat = VkSurfaceFormatKHR.create()
            presentMode = -1
            renderPass = 0L
            pipeline = 0L
            clearEnable = true
            clearValue = VkClearValue.create(1)
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
        var vertexBufferMemory = LongBuffer(1)
        var indexBufferMemory = LongBuffer(1)
        var vertexBufferSize = LongBuffer(1)
        var indexBufferSize = LongBuffer(1)
        var vertexBuffer = LongBuffer(1)
        var indexBuffer = LongBuffer(1)
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

    // Helpers
    //    IMGUI_IMPL_API void                 ImGui_ImplVulkanH_CreateOrResizeWindow(VkInstance instance, VkPhysicalDevice physical_device, VkDevice device, ImGui_ImplVulkanH_Window* wnd, uint32_t queue_family, const VkAllocationCallbacks* allocator, int w, int h, uint32_t min_image_count);
    //    IMGUI_IMPL_API void                 ImGui_ImplVulkanH_DestroyWindow(VkInstance instance, VkDevice device, ImGui_ImplVulkanH_Window* wnd, const VkAllocationCallbacks* allocator);
    //    IMGUI_IMPL_API VkSurfaceFormatKHR   ImGui_ImplVulkanH_SelectSurfaceFormat(VkPhysicalDevice physical_device, VkSurfaceKHR surface, const VkFormat* request_formats, int request_formats_count, VkColorSpaceKHR request_color_space);
    //    IMGUI_IMPL_API VkPresentModeKHR     ImGui_ImplVulkanH_SelectPresentMode(VkPhysicalDevice physical_device, VkSurfaceKHR surface, const VkPresentModeKHR* request_modes, int request_modes_count);
    //    IMGUI_IMPL_API int                  ImGui_ImplVulkanH_GetMinImageCountFromPresentMode(VkPresentModeKHR present_mode);

    // Create or resize window
    fun createOrResizeWindow(instance: VkInstance, physicalDevice: VkPhysicalDevice, device: VkDevice,
                             wd: Window, queueFamily: Int, allocator: VkAllocationCallbacks?,
                             size: Vec2i, minImageCount: Int) {

        createWindowSwapChain(physicalDevice, device, wd, allocator, size, minImageCount)
        createWindowCommandBuffers(physicalDevice, device, wd, queueFamily, allocator)
    }

    fun destroyWindow(instance: VkInstance, device: VkDevice, wd: Window, allocator: VkAllocationCallbacks) {

        vkDeviceWaitIdle(device) // FIXME: We could wait on the Queue if we had the queue in wd-> (otherwise VulkanH functions can't use globals)
        //vkQueueWaitIdle(g_Queue);

        for (i in 0 until wd.imageCount) {
            destroyFrame(device, wd.frames!![i], allocator)
            destroyFrameSemaphores(device, wd.frameSemaphores!![i], allocator)
        }
        wd.frames = null
        wd.frameSemaphores = null
        vkDestroyPipeline(device, wd.pipeline, allocator)
        vkDestroyRenderPass(device, wd.renderPass, allocator)
        vkDestroySwapchainKHR(device, wd.swapchain, allocator)
        vkDestroySurfaceKHR(instance, wd.surface, allocator)

        wd.reset()
    }

    fun selectSurfaceFormat(physicalDevice: VkPhysicalDevice, surface: Long, requestFormats: IntBuffer, requestColorSpace: Int): VkSurfaceFormatKHR {

        assert(requestFormats.isNotEmpty())

        // Per Spec Format and View Format are expected to be the same unless VK_IMAGE_CREATE_MUTABLE_BIT was set at image creation
        // Assuming that the default behavior is without setting this bit, there is no need for separate Swapchain image and image view format
        // Additionally several new color spaces were introduced with Vulkan Spec v1.0.40,
        // hence we must make sure that a format with the mostly available color space, VK_COLOR_SPACE_SRGB_NONLINEAR_KHR, is found and used.
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, pI, null)
        val availCount = pI[0]
        val availFormat = VkSurfaceFormatKHR.create(availCount)
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, pI, availFormat)

        // First check if only one format, VK_FORMAT_UNDEFINED, is available, which would imply that any format is available
        when (availCount) {
            1 -> {
                return when (VK_FORMAT_UNDEFINED) {
                    availFormat[0].format() -> {
                        val ret = VkSurfaceFormatKHR.create()
                        MemoryUtil.memPutInt(ret.adr + VkSurfaceFormatKHR.FORMAT, requestFormats[0])
                        MemoryUtil.memPutInt(ret.adr + VkSurfaceFormatKHR.COLORSPACE, requestColorSpace)
                        ret
                    }
                    else // No point in searching another format
                    -> availFormat[0]
                }
            }
            else -> { // Request several formats, the first found will be used
                for (request_i in requestFormats.indices)
                    for (avail_i in 0 until availCount)
                        if (availFormat[avail_i].format() == requestFormats[request_i] && availFormat[avail_i].colorSpace() == requestColorSpace)
                            return availFormat[avail_i]

                // If none of the requested image formats could be found, use the first available
                return availFormat[0]
            }
        }
    }

    fun selectPresentMode(physicalDevice: VkPhysicalDevice, surface: Long, requestModes: IntArray): Int = Stack { s ->

        assert(requestModes.isNotEmpty())

        // Request a certain mode and confirm that it is available. If not use VK_PRESENT_MODE_FIFO_KHR which is mandatory
        vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, pI, null)
        val availCount = pI[0]
        val availModes = s.callocInt(availCount)
        vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, pI, availModes)
        //for (uint32_t avail_i = 0; avail_i < avail_count; avail_i++)
        //    printf("[vulkan] avail_modes[%d] = %d\n", avail_i, avail_modes[avail_i]);

        for (request_i in requestModes.indices)
            for (avail_i in 0 until availCount)
                if (requestModes[request_i] == availModes[avail_i])
                    return requestModes[request_i]

        VK_PRESENT_MODE_FIFO_KHR // Always available
    }

    fun getMinImageCountFromPresentMode(presentMode: Int): Int = when (presentMode) {
        VK_PRESENT_MODE_MAILBOX_KHR -> 3
        VK_PRESENT_MODE_FIFO_KHR, VK_PRESENT_MODE_FIFO_RELAXED_KHR -> 2
        VK_PRESENT_MODE_IMMEDIATE_KHR -> 1
        else -> error("invalid")
    }

    // Forward Declarations
//    void ImGui_ImplVulkanH_DestroyFrame(VkDevice device, ImGui_ImplVulkanH_Frame* fd, const VkAllocationCallbacks* allocator);
//    void ImGui_ImplVulkanH_DestroyFrameSemaphores(VkDevice device, ImGui_ImplVulkanH_FrameSemaphores* fsd, const VkAllocationCallbacks* allocator);
//    void ImGui_ImplVulkanH_DestroyFrameRenderBuffers(VkDevice device, ImGui_ImplVulkanH_FrameRenderBuffers* buffers, const VkAllocationCallbacks* allocator);
//    void ImGui_ImplVulkanH_DestroyWindowRenderBuffers(VkDevice device, ImGui_ImplVulkanH_WindowRenderBuffers* buffers, const VkAllocationCallbacks* allocator);
//    void ImGui_ImplVulkanH_CreateWindowSwapChain(VkPhysicalDevice physical_device, VkDevice device, ImGui_ImplVulkanH_Window* wd, const VkAllocationCallbacks* allocator, int w, int h, uint32_t min_image_count);
//    void ImGui_ImplVulkanH_CreateWindowCommandBuffers(VkPhysicalDevice physical_device, VkDevice device, ImGui_ImplVulkanH_Window* wd, uint32_t queue_family, const VkAllocationCallbacks* allocator);

    fun destroyFrame(device: VkDevice, fd: Frame, allocator: VkAllocationCallbacks?) {
        vkDestroyFence(device, fd.fence, allocator)
        vkFreeCommandBuffers(device, fd.commandPool, fd.commandBuffer!!)
        vkDestroyCommandPool(device, fd.commandPool, allocator)
        fd.fence = VK_NULL_HANDLE
        fd.commandBuffer = null
        fd.commandPool = VK_NULL_HANDLE

        vkDestroyImageView(device, fd.backbufferView, allocator)
        vkDestroyFramebuffer(device, fd.framebuffer, allocator)
    }

    fun destroyFrameSemaphores(device: VkDevice, fsd: FrameSemaphores, allocator: VkAllocationCallbacks?) {
        vkDestroySemaphore(device, fsd.imageAcquiredSemaphore[0], allocator)
        vkDestroySemaphore(device, fsd.renderCompleteSemaphore[0], allocator)
        fsd.imageAcquiredSemaphore[0] = VK_NULL_HANDLE
        fsd.renderCompleteSemaphore[0] = VK_NULL_HANDLE
    }

    fun destroyFrameRenderBuffers(device: VkDevice, buffers: FrameRenderBuffers, allocator: VkAllocationCallbacks?) {
        if (buffers.vertexBuffer[0] != VK_NULL_HANDLE) {
            vkDestroyBuffer(device, buffers.vertexBuffer[0], allocator)
            buffers.vertexBuffer[0] = VK_NULL_HANDLE
        }
        if (buffers.vertexBufferMemory[0] != VK_NULL_HANDLE) {
            vkFreeMemory(device, buffers.vertexBufferMemory[0], allocator)
            buffers.vertexBufferMemory[0] = VK_NULL_HANDLE
        }
        if (buffers.indexBuffer[0] != VK_NULL_HANDLE) {
            vkDestroyBuffer(device, buffers.indexBuffer[0], allocator)
            buffers.indexBuffer[0] = VK_NULL_HANDLE
        }
        if (buffers.indexBufferMemory[0] != VK_NULL_HANDLE) {
            vkFreeMemory(device, buffers.indexBufferMemory[0], allocator)
            buffers.indexBufferMemory[0] = VK_NULL_HANDLE
        }
        buffers.vertexBufferSize[0] = 0
        buffers.indexBufferSize[0] = 0
    }

    fun destroyWindowRenderBuffers(device: VkDevice, buffers: WindowRenderBuffers, allocator: VkAllocationCallbacks?) {
        for (n in 0 until buffers.count)
            destroyFrameRenderBuffers(device, buffers.frameRenderBuffers!![n], allocator)
        buffers.frameRenderBuffers = null
        buffers.index = 0
    }

    // Also destroy old swap chain and in-flight frames data, if any.
    fun createWindowSwapChain(physicalDevice: VkPhysicalDevice, device: VkDevice, wd: Window,
                              allocator: VkAllocationCallbacks?, size: Vec2i, minImageCount_: Int) = Stack { s ->

        var minImageCount = minImageCount_

        val oldSwapchain = wd.swapchain
        wd.swapchain = VK_NULL_HANDLE
        var err = vkDeviceWaitIdle(device)
        checkVkResult(err)

        // We don't use ImGui_ImplVulkanH_DestroyWindow() because we want to preserve the old swapchain to create the new one.
        // Destroy old Framebuffer
        for (i in 0 until wd.imageCount) {
            destroyFrame(device, wd.frames!![i], allocator)
            destroyFrameSemaphores(device, wd.frameSemaphores!![i], allocator)
        }
        wd.frames = null
        wd.frameSemaphores = null
        wd.imageCount = 0
        if (wd.renderPass != VK_NULL_HANDLE)
            vkDestroyRenderPass(device, wd.renderPass, allocator)
        if (wd.pipeline != VK_NULL_HANDLE)
            vkDestroyPipeline(device, wd.pipeline, allocator)

        // If min image count was not specified, request different count of images dependent on selected present mode
        if (minImageCount == 0)
            minImageCount = getMinImageCountFromPresentMode(wd.presentMode)

        // Create Swapchain
        run {
            val info = VkSwapchainCreateInfoKHR.callocStack(s)
                    .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(wd.surface)
                    .minImageCount(minImageCount)
                    .imageFormat(wd.surfaceFormat.format())
                    .imageColorSpace(wd.surfaceFormat.colorSpace())
                    .imageArrayLayers(1)
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)           // Assume that graphics family == present family
                    .preTransform(VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR)
                    .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .presentMode(wd.presentMode)
                    .clipped(true)
                    .oldSwapchain(oldSwapchain)
            val cap = VkSurfaceCapabilitiesKHR.callocStack(s)
            err = vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, wd.surface, cap)
            checkVkResult(err)
            if (info.minImageCount() < cap.minImageCount())
                info.minImageCount(cap.minImageCount())
            else if (cap.maxImageCount() != 0 && info.minImageCount() > cap.maxImageCount())
                info.minImageCount(cap.maxImageCount())

            if (cap.currentExtent().width() == -1) {
                info.imageExtent().width(size.x)
                info.imageExtent().height(size.y)
                wd.size put size
            } else {
                wd.size.x = cap.currentExtent().width()
                info.imageExtent().width(wd.size.x)
                wd.size.y = cap.currentExtent().height()
                info.imageExtent().height(wd.size.y)
            }
            err = vkCreateSwapchainKHR(device, info, allocator, pL)
            wd.swapchain = pL[0]
            checkVkResult(err)
            err = vkGetSwapchainImagesKHR(device, wd.swapchain, pI, null)
            wd.imageCount = pI[0]
            checkVkResult(err)
            val backbuffers = s.callocLong(16)
            assert(wd.imageCount >= minImageCount)
            assert(wd.imageCount < backbuffers.rem)
            err = vkGetSwapchainImagesKHR(device, wd.swapchain, pI, backbuffers)
            wd.imageCount = pI[0]
            checkVkResult(err)

            assert(wd.frames == null)
            wd.frames = Array(wd.imageCount) { Frame() }
            wd.frameSemaphores = Array(wd.imageCount) { FrameSemaphores() }
            for (i in 0 until wd.imageCount)
                wd.frames!![i].backbuffer = backbuffers[i]
        }
        if (oldSwapchain != VK_NULL_HANDLE)
            vkDestroySwapchainKHR(device, oldSwapchain, allocator)

        // Create the Render Pass
        run {
            val attachment = VkAttachmentDescription.callocStack(1, s)
                    .format(wd.surfaceFormat.format())
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(if (wd.clearEnable) VK_ATTACHMENT_LOAD_OP_CLEAR else VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
            val colorAttachment = VkAttachmentReference.callocStack(1, s)
                    .attachment(0)
                    .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
            val subpass = VkSubpassDescription.callocStack(1, s)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(1)
                    .pColorAttachments(colorAttachment)
            val dependency = VkSubpassDependency.callocStack(1, s)
                    .srcSubpass(VK_SUBPASS_EXTERNAL)
                    .dstSubpass(0)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .srcAccessMask(0)
                    .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            val info = VkRenderPassCreateInfo.callocStack(s)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pAttachments(attachment)
                .pSubpasses(subpass)
                .pDependencies(dependency)
            err = vkCreateRenderPass(device, info, allocator, pL)
            wd.renderPass = pL[0]
            checkVkResult(err)

            // We do not create a pipeline by default as this is also used by examples' main.cpp,
            // but secondary viewport in multi-viewport mode may want to create one with:
            //ImGui_ImplVulkan_CreatePipeline(device, allocator, VK_NULL_HANDLE, wd->RenderPass, VK_SAMPLE_COUNT_1_BIT, &wd->Pipeline, g_Subpass);
        }

        // Create The Image Views
        run {
            val info = VkImageViewCreateInfo.callocStack(s)
                .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .viewType(VK_IMAGE_VIEW_TYPE_2D)
                .format(wd.surfaceFormat.format())
                .components {
                    it.r(VK_COMPONENT_SWIZZLE_R)
                    it.g(VK_COMPONENT_SWIZZLE_G)
                    it.b(VK_COMPONENT_SWIZZLE_B)
                    it.a(VK_COMPONENT_SWIZZLE_A)
                }
            val imageRange = VkImageSubresourceRange.callocStack(s)
                    .set( VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1)
            info.subresourceRange(imageRange)
            for (i in 0 until wd.imageCount) {
                val fd = wd.frames!![i]
                info.image(fd.backbuffer)
                err = vkCreateImageView(device, info, allocator, pL)
                fd.backbufferView = pL[0]
                checkVkResult(err)
            }
        }

        // Create Framebuffer
        run {
            val attachment = s.callocLong(1)
            val info = VkFramebufferCreateInfo.callocStack(s)
                .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                .renderPass(wd.renderPass)
                .pAttachments(attachment)
                .width(wd.size.x)
                .height(wd.size.y)
                .layers(1)
            for (i in 0 until wd.imageCount) {
                val fd = wd.frames!![i]
                attachment[0] = fd.backbufferView
                err = vkCreateFramebuffer(device, info, allocator, pL)
                fd.framebuffer = pL[0]
                checkVkResult(err)
            }
        }
    }

    fun createWindowCommandBuffers(physicalDevice: VkPhysicalDevice, device: VkDevice, wd: Window,
                                   queueFamily: Int, allocator: VkAllocationCallbacks?) = Stack { s ->
        assert(physicalDevice.adr != VK_NULL_HANDLE && device.adr != VK_NULL_HANDLE)

        // Create Command Buffers
        for (i in 0 until wd.imageCount) {
            val fd = wd.frames!![i]
            val fsd = wd.frameSemaphores!![i]
            run {
                val info = VkCommandPoolCreateInfo.callocStack(s)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                    .queueFamilyIndex(queueFamily)
                val err = vkCreateCommandPool(device, info, allocator, pL)
                fd.commandPool = pL[0]
                checkVkResult(err)
            }
            run {
                val info = VkCommandBufferAllocateInfo.callocStack(s)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(fd.commandPool)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1)
                val err = vkAllocateCommandBuffers(device, info, pP)
                fd.commandBuffer = VkCommandBuffer(pP[0], device)
                checkVkResult(err)
            }
            run {
                val info = VkFenceCreateInfo.callocStack(s)
                    .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(VK_FENCE_CREATE_SIGNALED_BIT)
                val err = vkCreateFence(device, info, allocator, pL)
                fd.fence = pL[0]
                checkVkResult(err)
            }
            run {
                val info = VkSemaphoreCreateInfo.callocStack(s)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
                var err = vkCreateSemaphore(device, info, allocator, pL)
                fsd.imageAcquiredSemaphore[0] = pL[0]
                checkVkResult(err)
                err = vkCreateSemaphore(device, info, allocator, pL)
                fsd.renderCompleteSemaphore[0] = pL[0]
                checkVkResult(err)
            }
        }
    }
}