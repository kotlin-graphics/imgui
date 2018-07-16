package imgui.impl

import gli_.has
import glm_.BYTES
import glm_.L
import glm_.buffer.adr
import glm_.i
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec4.Vec4b
import imgui.DrawData
import imgui.DrawIdx
import imgui.DrawVert
import imgui.ImGui
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.MemoryUtil.memCopy
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED
import org.lwjgl.vulkan.VK10.VK_SUBPASS_EXTERNAL
import uno.buffer.toBuffer
import vkk.*
import kotlin.reflect.KMutableProperty0

object ImplVk {


    // ===== impl_vk.h =====

    var VK_QUEUED_FRAMES = 2
    val BINDING = 0 // TODO -> Omar

    fun init(): Boolean {

        assert(::instance.isInitialized)
        assert(::physicalDevice.isInitialized)
        assert(::device.isInitialized)
        assert(::queue.isInitialized)
        assert(descriptorPool != NULL)
        assert(renderPass != NULL)

        createDeviceObjects()

        return true
    }

    fun shutdown() = invalidateDeviceObjects()

    fun newFrame() {}

    /** Render function
     *  (this used to be set in io.renderDrawListsFn and called by ImGui::render(),
     *  but you can now call this directly from your main loop) */
    fun renderDrawData(drawData: DrawData, commandBuffer: VkCommandBuffer) {

        if (drawData.totalVtxCount == 0) return

        val fd = framesDataBuffers[frameIndex]
        frameIndex = (frameIndex + 1) % VK_QUEUED_FRAMES

        // Create the Vertex and Index buffers:
        val vertexSize = drawData.totalVtxCount * DrawVert.size
        val indexSize = drawData.totalIdxCount * DrawIdx.BYTES
        if (fd.vertexBuffer == NULL || fd.vertexBufferSize < vertexSize)
            createOrResizeBuffer(fd::vertexBuffer, fd->VertexBufferMemory, fd->VertexBufferSize, vertex_size, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
        if (!fd->IndexBuffer || fd->IndexBufferSize < index_size)
        CreateOrResizeBuffer(fd->IndexBuffer, fd->IndexBufferMemory, fd->IndexBufferSize, index_size, VK_BUFFER_USAGE_INDEX_BUFFER_BIT);

        // Upload Vertex and index Data:
        {
            ImDrawVert * vtx_dst;
            ImDrawIdx * idx_dst;
            err = vkMapMemory(g_Device, fd->VertexBufferMemory, 0, vertex_size, 0, (void**)(&vtx_dst));
            check_vk_result(err);
            err = vkMapMemory(g_Device, fd->IndexBufferMemory, 0, index_size, 0, (void**)(&idx_dst));
            check_vk_result(err);
            for (int n = 0; n < drawData->CmdListsCount; n++)
            {
                const ImDrawList * cmd_list = draw_data->CmdLists[n];
                memcpy(vtx_dst, cmd_list->VtxBuffer.Data, cmd_list->VtxBuffer.Size * sizeof(ImDrawVert));
                memcpy(idx_dst, cmd_list->IdxBuffer.Data, cmd_list->IdxBuffer.Size * sizeof(ImDrawIdx));
                vtx_dst += cmd_list->VtxBuffer.Size;
                idx_dst += cmd_list->IdxBuffer.Size;
            }
            VkMappedMemoryRange range [2] = {};
            range[0].sType = VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE;
            range[0].memory = fd->VertexBufferMemory;
            range[0].size = VK_WHOLE_SIZE;
            range[1].sType = VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE;
            range[1].memory = fd->IndexBufferMemory;
            range[1].size = VK_WHOLE_SIZE;
            err = vkFlushMappedMemoryRanges(g_Device, 2, range);
            check_vk_result(err);
            vkUnmapMemory(g_Device, fd->VertexBufferMemory);
            vkUnmapMemory(g_Device, fd->IndexBufferMemory);
        }

        // Bind pipeline and descriptor sets:
        {
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, g_Pipeline);
            VkDescriptorSet desc_set [1] = { g_DescriptorSet };
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, g_PipelineLayout, 0, 1, desc_set, 0, NULL);
        }

        // Bind Vertex And Index Buffer:
        {
            VkBuffer vertex_buffers [1] = { fd -> VertexBuffer };
            VkDeviceSize vertex_offset [1] = { 0 };
            vkCmdBindVertexBuffers(commandBuffer, 0, 1, vertex_buffers, vertex_offset);
            vkCmdBindIndexBuffer(commandBuffer, fd->IndexBuffer, 0, VK_INDEX_TYPE_UINT16);
        }

        // Setup viewport:
        {
            VkViewport viewport;
            viewport.x = 0;
            viewport.y = 0;
            viewport.width = drawData->DisplaySize.x;
            viewport.height = drawData->DisplaySize.y;
            viewport.minDepth = 0.0f;
            viewport.maxDepth = 1.0f;
            vkCmdSetViewport(commandBuffer, 0, 1, & viewport);
        }

        // Setup scale and translation:
        // Our visible imgui space lies from draw_data->DisplayPps (top left) to draw_data->DisplayPos+data_data->DisplaySize (bottom right). DisplayMin is typically (0,0) for single viewport apps.
        {
            float scale [2];
            scale[0] = 2.0f / drawData->DisplaySize.x;
            scale[1] = 2.0f / drawData->DisplaySize.y;
            float translate [2];
            translate[0] = -1.0f - drawData->DisplayPos.x * scale[0];
            translate[1] = -1.0f - drawData->DisplayPos.y * scale[1];
            vkCmdPushConstants(commandBuffer, g_PipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, sizeof(float) * 0, sizeof(float) * 2, scale);
            vkCmdPushConstants(commandBuffer, g_PipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, sizeof(float) * 2, sizeof(float) * 2, translate);
        }

        // Render the command lists:
        int vtx_offset = 0;
        int idx_offset = 0;
        ImVec2 display_pos = draw_data->DisplayPos;
        for (int n = 0; n < drawData->CmdListsCount; n++)
        {
            const ImDrawList * cmd_list = draw_data->CmdLists[n];
            for (int cmd_i = 0; cmd_i < cmd_list->CmdBuffer.Size; cmd_i++)
            {
                const ImDrawCmd * pcmd = &cmd_list->CmdBuffer[cmd_i];
                if (pcmd->UserCallback)
                { pcmd ->
                    UserCallback(cmd_list, pcmd);
                }
                else
                {
                    // Apply scissor/clipping rectangle
                    // FIXME: We could clamp width/height based on clamped min/max values.
                    VkRect2D scissor;
                    scissor.offset.x = (int32_t)(pcmd->ClipRect.x-display_pos.x) > 0 ? (int32_t)(pcmd->ClipRect.x-display_pos.x) : 0;
                    scissor.offset.y = (int32_t)(pcmd->ClipRect.y-display_pos.y) > 0 ? (int32_t)(pcmd->ClipRect.y-display_pos.y) : 0;
                    scissor.extent.width = (uint32_t)(pcmd->ClipRect.z-pcmd->ClipRect.x);
                    scissor.extent.height = (uint32_t)(pcmd->ClipRect.w-pcmd->ClipRect.y+1); // FIXME: Why +1 here?
                    vkCmdSetScissor(commandBuffer, 0, 1, & scissor);

                    // Draw
                    vkCmdDrawIndexed(commandBuffer, pcmd->ElemCount, 1, idx_offset, vtx_offset, 0);
                }
                idx_offset += pcmd->ElemCount;
            }
            vtx_offset += cmd_list->VtxBuffer.Size;
        }
    }

    fun createOrResizeBuffer(bufferPtr: KMutableProperty0<VkBuffer>, bufferMemory: VkDeviceMemory, bufferSize: VkDeviceSize, newSize: Long, usage: VkBufferUsage) {

        var buffer by bufferPtr
        if (bufferPtr != NULL)
            device destroyBuffer bufferPtr
        if (bufferMemory != NULL)
            device freeMemory bufferMemory

        val vertexBufferSizeAligned: VkDeviceSize = ((newSize - 1) / bufferMemoryAlignment + 1) * bufferMemoryAlignment
        val bufferInfo = vk.BufferCreateInfo {
            size = vertexBufferSizeAligned
            this.usage = usage.i
            sharingMode = VkSharingMode.EXCLUSIVE
        }
        bufferPtr = device createBuffer bufferInfo

        VkMemoryRequirements req;
        vkGetBufferMemoryRequirements(g_Device, bufferPtr, & req);
        g_BufferMemoryAlignment = (g_BufferMemoryAlignment > req.alignment) ? g_BufferMemoryAlignment : req.alignment;
        VkMemoryAllocateInfo alloc_info = {};
        alloc_info.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
        alloc_info.allocationSize = req.size;
        alloc_info.memoryTypeIndex = ImGui_ImplVulkan_MemoryType(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT, req.memoryTypeBits);
        err = vkAllocateMemory(g_Device, & alloc_info, g_Allocator, &buffer_memory);
        check_vk_result(err);

        err = vkBindBufferMemory(g_Device, bufferPtr, buffer_memory, 0);
        check_vk_result(err);
        bufferSize = newSize;
    }

// Called by Init/NewFrame/Shutdown

    /** Window Data */
    object wd {
        var size = Vec2i()
        var swapchain: VkSwapchainKHR = NULL
        var surface: VkSurfaceKHR = NULL
        lateinit var surfaceFormat: VkSurfaceFormatKHR
        var presentMode = VkPresentMode.IMMEDIATE_KHR
        var renderPass: VkRenderPass = NULL
        var clearEnable = false
        var clearValue: VkClearValue = VkClearValue.calloc()
        var backBufferCount = 0
        var backBuffer = VkImageArray(16)
        var backBufferView = VkImageViewArray(16)
        var framebuffer = VkFramebufferArray(16)
        var frameIndex = 0
        var frames = Array(VK_QUEUED_FRAMES) { VkFrameData() }
        val frame: VkFrameData
            get() = frames[frameIndex]
    }

    class VkFrameData {
        /** keep track of recently rendered swapchain frame indices */
        var backbufferIndex = 0
        var commandPool: VkCommandPool = NULL
        lateinit var commandBuffer: VkCommandBuffer
        var fence: VkFence = NULL
        var imageAcquiredSemaphore: VkSemaphore = NULL
        var renderCompleteSemaphore: VkSemaphore = NULL
    }

    fun selectSurfaceFormat(physicalDevice: VkPhysicalDevice, surface: VkSurfaceKHR, requestFormats: Array<VkFormat>, requestColorSpace: VkColorSpace): VkSurfaceFormatKHR {
        assert(requestFormats.isNotEmpty())

        /*  Per Spec Format and View Format are expected to be the same unless VK_IMAGE_CREATE_MUTABLE_BIT was set at image creation
            Assuming that the default behavior is without setting this bit, there is no need for separate Swapchain image and image view format
            Additionally several new color spaces were introduced with Vulkan Spec v1.0.40,
            hence we must make sure that a format with the mostly available color space, VK_COLOR_SPACE_SRGB_NONLINEAR_KHR, is found and used. */
        val availFormat = physicalDevice getSurfaceFormatsKHR surface

        // First check if only one format, VK_FORMAT_UNDEFINED, is available, which would imply that any format is available
        return when (availFormat.size) {
            1 -> when (availFormat[0].format) {
                VkFormat.UNDEFINED -> vk.SurfaceFormatKHR(requestFormats[0], requestColorSpace)
                else -> availFormat[0] // No point in searching another format
            }
            else -> {
                // Request several formats, the first found will be used
                for (request in requestFormats.indices)
                    for (avail in availFormat.indices)
                        if (availFormat[avail].format == requestFormats[request] && availFormat[avail].colorSpace == requestColorSpace)
                            return availFormat[avail]

                // If none of the requested image formats could be found, use the first available
                return availFormat[0]
            }
        }
    }

    fun selectPresentMode(requestModes: Array<VkPresentMode>): VkPresentMode {

        // Request a certain mode and confirm that it is available. If not use VK_PRESENT_MODE_FIFO_KHR which is mandatory
        val availModes = physicalDevice getSurfacePresentModesKHR wd.surface
        for (request in requestModes.indices)
            for (avail in availModes.indices)
                if (requestModes[request] == availModes[avail])
                    return requestModes[request]

        return VkPresentMode.FIFO_KHR // Always available
    }

    fun createWindowDataCommandBuffers() {

        // Create Command Buffers
        for (i in 0 until VK_QUEUED_FRAMES)

            wd.frames[i].apply {

                commandPool = device createCommandPool vk.CommandPoolCreateInfo {
                    flag = VkCommandPoolCreate.RESET_COMMAND_BUFFER_BIT
                    queueFamilyIndex = queueFamily
                }

                commandBuffer = device allocateCommandBuffer vk.CommandBufferAllocateInfo {
                    this.commandPool = this@apply.commandPool
                    level = VkCommandBufferLevel.PRIMARY
                    commandBufferCount = 1
                }
                fence = device createFence VkFenceCreate.SIGNALED_BIT

                val info = vk.SemaphoreCreateInfo()
                imageAcquiredSemaphore = device createSemaphore info
                renderCompleteSemaphore = device createSemaphore info
            }
    }

    fun createWindowDataSwapChainAndFramebuffer(size: Vec2i) {

        var minImageCount = 2    // FIXME: this should become a function parameter

        val oldSwapchain = wd.swapchain
        device.waitIdle()

        // Destroy old Framebuffer
        for (i in 0 until wd.backBufferCount) {
            if (wd.backBufferView[i] != NULL)
                device destroyImageView wd.backBufferView[i]
            if (wd.framebuffer[i] != NULL)
                device destroyFramebuffer wd.framebuffer[i]
        }
        wd.backBufferCount = 0
        if (wd.renderPass != NULL)
            device destroyRenderPass wd.renderPass

        // If min image count was not specified, request different count of images dependent on selected present mode
        if (minImageCount == 0)
            minImageCount = getMinImageCountFromPresentMode(wd.presentMode)

        // Create Swapchain
        run {
            val info = vk.SwapchainCreateInfoKHR {
                surface = wd.surface
                this.minImageCount = minImageCount
                imageFormat = wd.surfaceFormat.format
                imageColorSpace = wd.surfaceFormat.colorSpace
                imageArrayLayers = 1
                imageUsage = VkImageUsage.COLOR_ATTACHMENT_BIT.i
                imageSharingMode = VkSharingMode.EXCLUSIVE           // Assume that graphics family == present family
                preTransform = VkSurfaceTransform.IDENTITY_BIT_KHR
                compositeAlpha = VkCompositeAlpha.OPAQUE_BIT_KHR
                presentMode = wd.presentMode
                clipped = true
                this.oldSwapchain = oldSwapchain
            }
            val cap = physicalDevice getSurfaceCapabilitiesKHR wd.surface

            if (info.minImageCount < cap.minImageCount)
                info.minImageCount = cap.minImageCount
            else if (info.minImageCount > cap.maxImageCount)
                info.minImageCount = cap.maxImageCount

            if (cap.currentExtent.width == 0xffffffff.i) {
                wd.size(size)           // TODO bug cant inception
                info.imageExtent(size)
            } else {
                wd.size(cap.currentExtent.width, cap.currentExtent.height)  // TOOD bug
                info.imageExtent(wd.size)
            }
            wd.swapchain = device createSwapchainKHR info
            wd.backBuffer = device getSwapchainImagesKHR wd.swapchain
            wd.backBufferCount = wd.backBuffer.size
        }
        if (oldSwapchain != NULL)
            device destroySwapchainKHR oldSwapchain

        // Create the Render Pass
        run {
            val attachment = vk.AttachmentDescription {
                format = wd.surfaceFormat.format
                samples = VkSampleCount.`1_BIT`
                loadOp = if (wd.clearEnable) VkAttachmentLoadOp.CLEAR else VkAttachmentLoadOp.DONT_CARE
                storeOp = VkAttachmentStoreOp.STORE
                stencilLoadOp = VkAttachmentLoadOp.DONT_CARE
                stencilStoreOp = VkAttachmentStoreOp.DONT_CARE
                initialLayout = VkImageLayout.UNDEFINED
                finalLayout = VkImageLayout.PRESENT_SRC_KHR
            }
            val colorAttachment = vk.AttachmentReference(0, VkImageLayout.COLOR_ATTACHMENT_OPTIMAL)
            val subpass = vk.SubpassDescription {
                pipelineBindPoint = VkPipelineBindPoint.GRAPHICS
                colorAttachmentCount = 1
                this.colorAttachment = colorAttachment
            }
            val dependency = vk.SubpassDependency {
                srcSubpass = VK_SUBPASS_EXTERNAL
                dstSubpass = 0
                srcStageMask = VkPipelineStage.COLOR_ATTACHMENT_OUTPUT_BIT.i
                dstStageMask = VkPipelineStage.COLOR_ATTACHMENT_OUTPUT_BIT.i
                srcAccessMask = 0
                dstAccessMask = VkAccess.COLOR_ATTACHMENT_WRITE_BIT.i
            }
            val info = vk.RenderPassCreateInfo().also {
                it.attachment = attachment
                it.subpass = subpass
                it.dependency = dependency
            }
            wd.renderPass = device createRenderPass info
        }

        // Create The Image Views
        run {
            val info = vk.ImageViewCreateInfo {
                viewType = VkImageViewType.`2D`
                format = wd.surfaceFormat.format
                components(VkComponentSwizzle.R, VkComponentSwizzle.G, VkComponentSwizzle.B, VkComponentSwizzle.A)
            }
            val imageRange = vk.ImageSubresourceRange(VkImageAspect.COLOR_BIT, 0, 1, 0, 1)
            info.subresourceRange = imageRange
            for (i in 0 until wd.backBufferCount) {
                info.image = wd.backBuffer[i]
                wd.backBufferView[i] = device createImageView info
            }
        }

        // Create Framebuffer
        run {
            var attachment: VkImageView = NULL
            val info = vk.FramebufferCreateInfo {
                renderPass = wd.renderPass
                this.attachment = attachment
                extent(wd.size, 1)
            }
            for (i in 0 until wd.backBufferCount) {
                attachment = wd.backBufferView[i]
                wd.framebuffer[i] = device createFramebuffer info
            }
        }
    }

    fun getMinImageCountFromPresentMode(presentMode: VkPresentMode) = when (presentMode) {
        VkPresentMode.MAILBOX_KHR -> 3
        VkPresentMode.FIFO_KHR, VkPresentMode.FIFO_RELAXED_KHR -> 2
        VkPresentMode.IMMEDIATE_KHR -> 1
        else -> throw Error()
    }

// ===== impl_vk.cpp =====

    // Vulkan data
    lateinit var physicalDevice: VkPhysicalDevice
    lateinit var instance: VkInstance
    lateinit var device: VkDevice
    var queueFamily = -1
    lateinit var queue: VkQueue
    var pipelineCache: VkPipelineCache = NULL
    var descriptorPool: VkDescriptorPool = NULL
    var renderPass: VkRenderPass = NULL

    var bufferMemoryAlignment: VkDeviceSize = 256
    var pipelineCreateFlags: VkPipelineCreateFlags = 0

    var descriptorSetLayout: VkDescriptorSetLayout = NULL
    var pipelineLayout: VkPipelineLayout = NULL
    var descriptorSet: VkDescriptorSet = NULL
    var pipeline: VkPipeline = NULL

    // Frame data
    class FrameDataForRender {
        var vertexBufferMemory: VkDeviceMemory = NULL
        var indexBufferMemory: VkDeviceMemory = NULL
        var vertexBufferSize: VkDeviceSize = NULL
        var indexBufferSize: VkDeviceSize = NULL
        var vertexBuffer: VkBuffer = NULL
        var indexBuffer: VkBuffer = NULL
    }

    var frameIndex = 0
    val framesDataBuffers = Array(VK_QUEUED_FRAMES) { FrameDataForRender() }

    // Font data
    var fontSampler: VkSampler = NULL
    var fontMemory: VkDeviceMemory = NULL
    var fontImage: VkImage = NULL
    var fontView: VkImageView = NULL
    var uploadBufferMemory: VkDeviceMemory = NULL
    var uploadBuffer: VkBuffer = NULL

    // Called by Init/NewFrame/Shutdown

    fun invalidateFontUploadObjects() {
        if (uploadBuffer != NULL) {
            device destroyBuffer uploadBuffer
            uploadBuffer = NULL
        }
        if (uploadBufferMemory != NULL) {
            device freeMemory uploadBufferMemory
            uploadBufferMemory = NULL
        }
    }

    fun invalidateDeviceObjects() {
        invalidateFontUploadObjects()
        device.apply {
            framesDataBuffers.forEach { fd ->
                if (fd.vertexBuffer != NULL) destroyBuffer(fd.vertexBuffer).also { fd.vertexBuffer = NULL }
                if (fd.vertexBufferMemory != NULL) freeMemory(fd.vertexBufferMemory).also { fd.vertexBufferMemory = NULL }
                if (fd.indexBuffer != NULL) destroyBuffer(fd.indexBuffer).also { fd.indexBuffer = NULL }
                if (fd.indexBufferMemory != NULL) freeMemory(fd.indexBufferMemory).also { fd.indexBufferMemory = NULL }
            }

            if (fontView != NULL) destroyImageView(fontView).also { fontView = NULL }
            if (fontImage != NULL) destroyImage(fontImage).also { fontImage = NULL }
            if (fontMemory != NULL) freeMemory(fontMemory).also { fontMemory = NULL }
            if (fontSampler != NULL) destroySampler(fontSampler).also { fontSampler = NULL }
            if (descriptorSetLayout != NULL) destroyDescriptorSetLayout(descriptorSetLayout).also { descriptorSetLayout = NULL }
            if (pipelineLayout != NULL) destroyPipelineLayout(pipelineLayout).also { pipelineLayout = NULL }
            if (pipeline != NULL) destroyPipeline(pipeline).also { pipeline = NULL }
        }
    }

    fun createFontsTexture(commandBuffer: VkCommandBuffer): Boolean {

        val (pixels, size) = ImGui.io.fonts.getTexDataAsRGBA32()
        val uploadSize = size.x * size.y * Vec4b.size.L

        // Create the Image:
        run {
            val info = vk.ImageCreateInfo {
                imageType = VkImageType.`2D`
                format = VkFormat.R8G8B8A8_UNORM
                extent(size, 1)
                mipLevels = 1
                arrayLayers = 1
                samples = VkSampleCount.`1_BIT`
                tiling = VkImageTiling.OPTIMAL
                usage = VkImageUsage.SAMPLED_BIT or VkImageUsage.TRANSFER_DST_BIT
                sharingMode = VkSharingMode.EXCLUSIVE
                initialLayout = VkImageLayout.UNDEFINED
            }
            fontImage = device createImage info
            val req = device getImageMemoryRequirements fontImage
            val allocInfo = vk.MemoryAllocateInfo {
                allocationSize = req.size
                memoryTypeIndex = memoryType(VkMemoryProperty.DEVICE_LOCAL_BIT, req.memoryTypeBits)
            }
            fontMemory = device allocateMemory allocInfo
            device.bindImageMemory(fontImage, fontMemory)
        }

        // Create the Image View:
        fontView = device createImageView vk.ImageViewCreateInfo {
            image = fontImage
            viewType = VkImageViewType.`2D`
            format = VkFormat.R8G8B8A8_UNORM
            subresourceRange.apply {
                aspectMask = VkImageAspect.COLOR_BIT.i
                levelCount = 1
                layerCount = 1
            }
        }

        // Update the Descriptor Set:
        run {
            val descImage = vk.DescriptorImageInfo(fontSampler, fontView, VkImageLayout.SHADER_READ_ONLY_OPTIMAL)
            val writeDesc = vk.WriteDescriptorSet(descriptorSet, VkDescriptorType.COMBINED_IMAGE_SAMPLER, 0, descImage)
            device updateDescriptorSets writeDesc
        }

        // Create the Upload Buffer:
        run {
            val bufferInfo = vk.BufferCreateInfo {
                this.size = uploadSize
                usage = VkBufferUsage.TRANSFER_SRC_BIT.i
                sharingMode = VkSharingMode.EXCLUSIVE
            }
            uploadBuffer = device createBuffer bufferInfo
            val req = device getBufferMemoryRequirements uploadBuffer
            bufferMemoryAlignment = if (bufferMemoryAlignment > req.alignment) bufferMemoryAlignment else req.alignment
            val allocInfo = vk.MemoryAllocateInfo {
                allocationSize = req.size
                memoryTypeIndex = memoryType(VkMemoryProperty.HOST_VISIBLE_BIT, req.memoryTypeBits)
            }
            uploadBufferMemory = device allocateMemory allocInfo
            device.bindBufferMemory(uploadBuffer, uploadBufferMemory)
        }

        // Upload to Buffer:
        device.mappingMemory(uploadBufferMemory, 0, uploadSize) { map ->
            memCopy(pixels.adr, map, uploadSize)
            val range = vk.MappedMemoryRange {
                memory = uploadBufferMemory
                this.size = uploadSize
            }
            device flushMappedMemoryRanges range
        }

        // Copy to Image:
        run {
            val copyBarrier = vk.ImageMemoryBarrier {
                dstAccessMask = VkAccess.TRANSFER_WRITE_BIT.i
                oldLayout = VkImageLayout.UNDEFINED
                newLayout = VkImageLayout.TRANSFER_DST_OPTIMAL
                srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED
                dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED
                image = fontImage
                subresourceRange.apply {
                    aspectMask = VkImageAspect.COLOR_BIT.i
                    levelCount = 1
                    layerCount = 1
                }
            }
            commandBuffer.pipelineBarrier(VkPipelineStage.HOST_BIT, VkPipelineStage.TRANSFER_BIT, imageMemoryBarrier = copyBarrier)

            val region = vk.BufferImageCopy {
                imageSubresource.apply {
                    aspectMask = VkImageAspect.COLOR_BIT.i
                    layerCount = 1
                }
                imageExtent(size, 1)
            }
            commandBuffer.copyBufferToImage(uploadBuffer, fontImage, VkImageLayout.TRANSFER_DST_OPTIMAL, region)

            val useBarrier = vk.ImageMemoryBarrier {
                srcAccessMask = VkAccess.TRANSFER_WRITE_BIT.i
                dstAccessMask = VkAccess.SHADER_READ_BIT.i
                oldLayout = VkImageLayout.TRANSFER_DST_OPTIMAL
                newLayout = VkImageLayout.SHADER_READ_ONLY_OPTIMAL
                srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED
                dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED
                image = fontImage
                subresourceRange.apply {
                    aspectMask = VkImageAspect.COLOR_BIT.i
                    levelCount = 1
                    layerCount = 1
                }
            }
            commandBuffer.pipelineBarrier(VkPipelineStage.TRANSFER_BIT, VkPipelineStage.FRAGMENT_SHADER_BIT, imageMemoryBarrier = useBarrier)
        }

        // Store our identifier
        ImGui.io.fonts.texId = fontImage.i

        return true
    }

    fun createDeviceObjects(): Boolean {

        // Create The Shader Modules:
        val vertModule: VkShaderModule = device createShaderModule vk.ShaderModuleCreateInfo { code = glslShaderVertSpv }
        val fragModule: VkShaderModule = device createShaderModule vk.ShaderModuleCreateInfo { code = glslShaderFragSpv }

        if (fontSampler == NULL)
            fontSampler = device createSampler vk.SamplerCreateInfo {
                magFilter = VkFilter.LINEAR
                minFilter = VkFilter.LINEAR
                mipmapMode = VkSamplerMipmapMode.LINEAR
                addressMode = VkSamplerAddressMode.REPEAT
                minLod = -1000f
                maxLod = 1000f
                maxAnisotropy = 1f
            }

        if (descriptorSetLayout == NULL) {
            val binding = vk.DescriptorSetLayoutBinding {
                descriptorType = VkDescriptorType.COMBINED_IMAGE_SAMPLER
                descriptorCount = 1
                stageFlag = VkShaderStage.FRAGMENT_BIT
                immutableSampler = fontSampler
            }
            val info = vk.DescriptorSetLayoutCreateInfo(binding)
            descriptorSetLayout = device createDescriptorSetLayout info
        }

        // Create Descriptor Set:
        descriptorSet = device allocateDescriptorSets vk.DescriptorSetAllocateInfo(descriptorPool, descriptorSetLayout)

        if (pipelineLayout == NULL) {
            // Constants: we are using 'vec2 offset' and 'vec2 scale' instead of a full 3d projection matrix
            val pushConstant = vk.PushConstantRange(VkShaderStage.VERTEX_BIT, Vec2.size * 2)
            val layoutInfo = vk.PipelineLayoutCreateInfo {
                setLayout = descriptorSetLayout
                pushConstantRange = pushConstant
            }
            pipelineLayout = device createPipelineLayout layoutInfo
        }

        val stage = vk.PipelineShaderStageCreateInfo(2).also {
            it[0].apply {
                stage = VkShaderStage.VERTEX_BIT
                module = vertModule
                name = "main"
            }
            it[1].apply {
                stage = VkShaderStage.FRAGMENT_BIT
                module = fragModule
                name = "main"
            }
        }

        val bindingDesc = vk.VertexInputBindingDescription(BINDING, DrawVert.size, VkVertexInputRate.VERTEX)

        val attributeDesc = vk.VertexInputAttributeDescription(
                bindingDesc.binding, 0, VkFormat.R32G32_SFLOAT, DrawVert.ofsPos,
                bindingDesc.binding, 1, VkFormat.R32G32_SFLOAT, DrawVert.ofsUv,
                bindingDesc.binding, 2, VkFormat.R8G8B8A8_UNORM, DrawVert.ofsCol)

        val vertexInfo = vk.PipelineVertexInputStateCreateInfo {
            vertexBindingDescription = bindingDesc
            vertexAttributeDescriptions = attributeDesc
        }
        val iaInfo = vk.PipelineInputAssemblyStateCreateInfo(VkPrimitiveTopology.TRIANGLE_LIST)

        val viewportInfo = vk.PipelineViewportStateCreateInfo(1, 1)

        val rasterInfo = vk.PipelineRasterizationStateCreateInfo(VkPolygonMode.FILL, VkCullMode.NONE.i, VkFrontFace.COUNTER_CLOCKWISE)

        val msInfo = vk.PipelineMultisampleStateCreateInfo(VkSampleCount.`1_BIT`)

        val colorAttachment = vk.PipelineColorBlendAttachmentState {
            blendEnable = true
            srcColorBlendFactor = VkBlendFactor.SRC_ALPHA
            dstColorBlendFactor = VkBlendFactor.ONE_MINUS_SRC_ALPHA
            colorBlendOp = VkBlendOp.ADD
            srcAlphaBlendFactor = VkBlendFactor.ONE_MINUS_SRC_ALPHA
            dstAlphaBlendFactor = VkBlendFactor.ZERO
            alphaBlendOp = VkBlendOp.ADD
            colorWriteMask = VkColorComponent.R_BIT or VkColorComponent.G_BIT or VkColorComponent.B_BIT or VkColorComponent.A_BIT
        }

        val depthInfo = vk.PipelineDepthStencilStateCreateInfo()

        val blendInfo = vk.PipelineColorBlendStateCreateInfo(colorAttachment)

        val dynamicStates = listOf(VkDynamicState.VIEWPORT, VkDynamicState.SCISSOR)
        val dynamicState = vk.PipelineDynamicStateCreateInfo(dynamicStates)

        val info = vk.GraphicsPipelineCreateInfo {
            flags = pipelineCreateFlags
            stages = stage
            vertexInputState = vertexInfo
            inputAssemblyState = iaInfo
            viewportState = viewportInfo
            rasterizationState = rasterInfo
            multisampleState = msInfo
            depthStencilState = depthInfo
            colorBlendState = blendInfo
            this.dynamicState = dynamicState
            layout = pipelineLayout
            renderPass = this@ImplVk.renderPass
        }
        pipeline = device.createGraphicsPipelines(pipelineCache, info)

        device destroyShaderModule vertModule
        device destroyShaderModule fragModule

        return true
    }

    fun memoryType(properties: VkMemoryProperty, typeBits: Int) = memoryType(properties.i, typeBits)
    fun memoryType(properties: VkMemoryPropertyFlags, typeBits: Int): Int {
        val prop = physicalDevice.memoryProperties
        for (i in 0 until prop.memoryTypeCount)
            if ((prop.memoryTypes[i].propertyFlags and properties) == properties && typeBits has (1 shl i))
                return i
        return -1 // Unable to find memoryType
    }

    /** glsl_shader.vert, compiled with:
     *  # glslangValidator -V -x -o glsl_shader.vert.u32 glsl_shader.vert
     */
    val glslShaderVertSpv = intArrayOf(
            0x07230203, 0x00010000, 0x00080001, 0x0000002e, 0x00000000, 0x00020011, 0x00000001, 0x0006000b,
            0x00000001, 0x4c534c47, 0x6474732e, 0x3035342e, 0x00000000, 0x0003000e, 0x00000000, 0x00000001,
            0x000a000f, 0x00000000, 0x00000004, 0x6e69616d, 0x00000000, 0x0000000b, 0x0000000f, 0x00000015,
            0x0000001b, 0x0000001c, 0x00030003, 0x00000002, 0x000001c2, 0x00040005, 0x00000004, 0x6e69616d,
            0x00000000, 0x00030005, 0x00000009, 0x00000000, 0x00050006, 0x00000009, 0x00000000, 0x6f6c6f43,
            0x00000072, 0x00040006, 0x00000009, 0x00000001, 0x00005655, 0x00030005, 0x0000000b, 0x0074754f,
            0x00040005, 0x0000000f, 0x6c6f4361, 0x0000726f, 0x00030005, 0x00000015, 0x00565561, 0x00060005,
            0x00000019, 0x505f6c67, 0x65567265, 0x78657472, 0x00000000, 0x00060006, 0x00000019, 0x00000000,
            0x505f6c67, 0x7469736f, 0x006e6f69, 0x00030005, 0x0000001b, 0x00000000, 0x00040005, 0x0000001c,
            0x736f5061, 0x00000000, 0x00060005, 0x0000001e, 0x73755075, 0x6e6f4368, 0x6e617473, 0x00000074,
            0x00050006, 0x0000001e, 0x00000000, 0x61635375, 0x0000656c, 0x00060006, 0x0000001e, 0x00000001,
            0x61725475, 0x616c736e, 0x00006574, 0x00030005, 0x00000020, 0x00006370, 0x00040047, 0x0000000b,
            0x0000001e, 0x00000000, 0x00040047, 0x0000000f, 0x0000001e, 0x00000002, 0x00040047, 0x00000015,
            0x0000001e, 0x00000001, 0x00050048, 0x00000019, 0x00000000, 0x0000000b, 0x00000000, 0x00030047,
            0x00000019, 0x00000002, 0x00040047, 0x0000001c, 0x0000001e, 0x00000000, 0x00050048, 0x0000001e,
            0x00000000, 0x00000023, 0x00000000, 0x00050048, 0x0000001e, 0x00000001, 0x00000023, 0x00000008,
            0x00030047, 0x0000001e, 0x00000002, 0x00020013, 0x00000002, 0x00030021, 0x00000003, 0x00000002,
            0x00030016, 0x00000006, 0x00000020, 0x00040017, 0x00000007, 0x00000006, 0x00000004, 0x00040017,
            0x00000008, 0x00000006, 0x00000002, 0x0004001e, 0x00000009, 0x00000007, 0x00000008, 0x00040020,
            0x0000000a, 0x00000003, 0x00000009, 0x0004003b, 0x0000000a, 0x0000000b, 0x00000003, 0x00040015,
            0x0000000c, 0x00000020, 0x00000001, 0x0004002b, 0x0000000c, 0x0000000d, 0x00000000, 0x00040020,
            0x0000000e, 0x00000001, 0x00000007, 0x0004003b, 0x0000000e, 0x0000000f, 0x00000001, 0x00040020,
            0x00000011, 0x00000003, 0x00000007, 0x0004002b, 0x0000000c, 0x00000013, 0x00000001, 0x00040020,
            0x00000014, 0x00000001, 0x00000008, 0x0004003b, 0x00000014, 0x00000015, 0x00000001, 0x00040020,
            0x00000017, 0x00000003, 0x00000008, 0x0003001e, 0x00000019, 0x00000007, 0x00040020, 0x0000001a,
            0x00000003, 0x00000019, 0x0004003b, 0x0000001a, 0x0000001b, 0x00000003, 0x0004003b, 0x00000014,
            0x0000001c, 0x00000001, 0x0004001e, 0x0000001e, 0x00000008, 0x00000008, 0x00040020, 0x0000001f,
            0x00000009, 0x0000001e, 0x0004003b, 0x0000001f, 0x00000020, 0x00000009, 0x00040020, 0x00000021,
            0x00000009, 0x00000008, 0x0004002b, 0x00000006, 0x00000028, 0x00000000, 0x0004002b, 0x00000006,
            0x00000029, 0x3f800000, 0x00050036, 0x00000002, 0x00000004, 0x00000000, 0x00000003, 0x000200f8,
            0x00000005, 0x0004003d, 0x00000007, 0x00000010, 0x0000000f, 0x00050041, 0x00000011, 0x00000012,
            0x0000000b, 0x0000000d, 0x0003003e, 0x00000012, 0x00000010, 0x0004003d, 0x00000008, 0x00000016,
            0x00000015, 0x00050041, 0x00000017, 0x00000018, 0x0000000b, 0x00000013, 0x0003003e, 0x00000018,
            0x00000016, 0x0004003d, 0x00000008, 0x0000001d, 0x0000001c, 0x00050041, 0x00000021, 0x00000022,
            0x00000020, 0x0000000d, 0x0004003d, 0x00000008, 0x00000023, 0x00000022, 0x00050085, 0x00000008,
            0x00000024, 0x0000001d, 0x00000023, 0x00050041, 0x00000021, 0x00000025, 0x00000020, 0x00000013,
            0x0004003d, 0x00000008, 0x00000026, 0x00000025, 0x00050081, 0x00000008, 0x00000027, 0x00000024,
            0x00000026, 0x00050051, 0x00000006, 0x0000002a, 0x00000027, 0x00000000, 0x00050051, 0x00000006,
            0x0000002b, 0x00000027, 0x00000001, 0x00070050, 0x00000007, 0x0000002c, 0x0000002a, 0x0000002b,
            0x00000028, 0x00000029, 0x00050041, 0x00000011, 0x0000002d, 0x0000001b, 0x0000000d, 0x0003003e,
            0x0000002d, 0x0000002c, 0x000100fd, 0x00010038).toBuffer()

    /** glsl_shader.frag, compiled with:
     *  # glslangValidator -V -x -o glsl_shader.frag.u32 glsl_shader.frag
     */
    val glslShaderFragSpv = intArrayOf(
            0x07230203, 0x00010000, 0x00080001, 0x0000001e, 0x00000000, 0x00020011, 0x00000001, 0x0006000b,
            0x00000001, 0x4c534c47, 0x6474732e, 0x3035342e, 0x00000000, 0x0003000e, 0x00000000, 0x00000001,
            0x0007000f, 0x00000004, 0x00000004, 0x6e69616d, 0x00000000, 0x00000009, 0x0000000d, 0x00030010,
            0x00000004, 0x00000007, 0x00030003, 0x00000002, 0x000001c2, 0x00040005, 0x00000004, 0x6e69616d,
            0x00000000, 0x00040005, 0x00000009, 0x6c6f4366, 0x0000726f, 0x00030005, 0x0000000b, 0x00000000,
            0x00050006, 0x0000000b, 0x00000000, 0x6f6c6f43, 0x00000072, 0x00040006, 0x0000000b, 0x00000001,
            0x00005655, 0x00030005, 0x0000000d, 0x00006e49, 0x00050005, 0x00000016, 0x78655473, 0x65727574,
            0x00000000, 0x00040047, 0x00000009, 0x0000001e, 0x00000000, 0x00040047, 0x0000000d, 0x0000001e,
            0x00000000, 0x00040047, 0x00000016, 0x00000022, 0x00000000, 0x00040047, 0x00000016, 0x00000021,
            0x00000000, 0x00020013, 0x00000002, 0x00030021, 0x00000003, 0x00000002, 0x00030016, 0x00000006,
            0x00000020, 0x00040017, 0x00000007, 0x00000006, 0x00000004, 0x00040020, 0x00000008, 0x00000003,
            0x00000007, 0x0004003b, 0x00000008, 0x00000009, 0x00000003, 0x00040017, 0x0000000a, 0x00000006,
            0x00000002, 0x0004001e, 0x0000000b, 0x00000007, 0x0000000a, 0x00040020, 0x0000000c, 0x00000001,
            0x0000000b, 0x0004003b, 0x0000000c, 0x0000000d, 0x00000001, 0x00040015, 0x0000000e, 0x00000020,
            0x00000001, 0x0004002b, 0x0000000e, 0x0000000f, 0x00000000, 0x00040020, 0x00000010, 0x00000001,
            0x00000007, 0x00090019, 0x00000013, 0x00000006, 0x00000001, 0x00000000, 0x00000000, 0x00000000,
            0x00000001, 0x00000000, 0x0003001b, 0x00000014, 0x00000013, 0x00040020, 0x00000015, 0x00000000,
            0x00000014, 0x0004003b, 0x00000015, 0x00000016, 0x00000000, 0x0004002b, 0x0000000e, 0x00000018,
            0x00000001, 0x00040020, 0x00000019, 0x00000001, 0x0000000a, 0x00050036, 0x00000002, 0x00000004,
            0x00000000, 0x00000003, 0x000200f8, 0x00000005, 0x00050041, 0x00000010, 0x00000011, 0x0000000d,
            0x0000000f, 0x0004003d, 0x00000007, 0x00000012, 0x00000011, 0x0004003d, 0x00000014, 0x00000017,
            0x00000016, 0x00050041, 0x00000019, 0x0000001a, 0x0000000d, 0x00000018, 0x0004003d, 0x0000000a,
            0x0000001b, 0x0000001a, 0x00050057, 0x00000007, 0x0000001c, 0x00000017, 0x0000001b, 0x00050085,
            0x00000007, 0x0000001d, 0x00000012, 0x0000001c, 0x0003003e, 0x00000009, 0x0000001d, 0x000100fd,
            0x00010038).toBuffer()
}