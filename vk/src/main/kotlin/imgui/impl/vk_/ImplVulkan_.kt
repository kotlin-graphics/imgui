package imgui.impl.vk_

import glm_.L
import glm_.i
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import imgui.BackendFlag
import imgui.ImGui
import imgui.internal.DrawData
import imgui.internal.DrawIdx
import imgui.internal.DrawVert
import imgui.or
import kool.*
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

class ImplVulkan_(info: InitInfo, renderPass: Long) {

    // Initialization data, for ImGui_ImplVulkan_Init()
    // [Please zero-clear before use!]
    class InitInfo {
        lateinit var instance: VkInstance
        lateinit var physicalDevice: VkPhysicalDevice
        lateinit var device: VkDevice
        var queueFamily = 0
        lateinit var queue: VkQueue
        var pipelineCache = 0L
        var descriptorPool = 0L
        var subpass = 0
        var minImageCount = 0          // >= 2
        var imageCount = 0             // >= MinImageCount
        var msaaSamples = 0   // >= VK_SAMPLE_COUNT_1_BIT
        var allocator: VkAllocationCallbacks? = null
//        void                (*CheckVkResultFn)(VkResult err);
    }

    // Called by user code
    //IMGUI_IMPL_API bool     ImGui_ImplVulkan_Init(ImGui_ImplVulkan_InitInfo* info, VkRenderPass render_pass);
    //IMGUI_IMPL_API void     ImGui_ImplVulkan_Shutdown();
    //IMGUI_IMPL_API void     ImGui_ImplVulkan_NewFrame();
    //IMGUI_IMPL_API void     ImGui_ImplVulkan_RenderDrawData(ImDrawData* draw_data, VkCommandBuffer command_buffer, VkPipeline pipeline = VK_NULL_HANDLE);
    //IMGUI_IMPL_API bool     ImGui_ImplVulkan_CreateFontsTexture(VkCommandBuffer command_buffer);
    //IMGUI_IMPL_API void     ImGui_ImplVulkan_DestroyFontUploadObjects();
    //IMGUI_IMPL_API void     ImGui_ImplVulkan_SetMinImageCount(uint32_t min_image_count); // To override MinImageCount after initialization (e.g. if swap chain is recreated)

    init {
        // Setup backend capabilities flags
        val io = ImGui.io
        io.backendRendererName = "imgui_impl_vulkan"
        io.backendFlags = io.backendFlags or BackendFlag.RendererHasVtxOffset  // We can honor the ImDrawCmd::VtxOffset field, allowing for large meshes.

        assert(info.instance.adr != VK_NULL_HANDLE)
        assert(info.physicalDevice.adr != VK_NULL_HANDLE)
        assert(info.device.adr != VK_NULL_HANDLE)
        assert(info.queue.adr != VK_NULL_HANDLE)
        assert(info.descriptorPool != VK_NULL_HANDLE)
        assert(info.minImageCount >= 2)
        assert(info.imageCount >= info.minImageCount)
        assert(renderPass != VK_NULL_HANDLE)

        gVulkanInitInfo = info
        gRenderPass = renderPass
        gSubpass = info.subpass

        createDeviceObjects()
    }

    fun shutdown() = destroyDeviceObjects()

    fun newFrame() {}

    companion object {
        // Render function
        fun renderDrawData(drawData: DrawData, commandBuffer: VkCommandBuffer, pipeline_: Long = VK_NULL_HANDLE) {

            var pipeline = pipeline_
            // Avoid rendering when minimized, scale coordinates for retina displays (screen coordinates != framebuffer coordinates)
            val fbSize = Vec2i(drawData.displaySize * drawData.framebufferScale)
            if (fbSize anyLessThanEqual 0)
                return

            val v = gVulkanInitInfo
            if (pipeline == VK_NULL_HANDLE)
                pipeline = gPipeline

            // Allocate array to store enough vertex/index buffers
            val wrb = gMainWindowRenderBuffers
            if (wrb.frameRenderBuffers == null) {
                wrb.index = 0
                wrb.frameRenderBuffers = Array(v.imageCount) { ImplVulkanH_.FrameRenderBuffers() }
            }
            assert(wrb.count == v.imageCount)
            wrb.index = (wrb.index + 1) % wrb.count
            val rb = wrb.frameRenderBuffers!![wrb.index]

            Stack { s ->
                if (drawData.totalVtxCount > 0) {
                    // Create or resize the vertex/index buffers
                    val vertexSize = drawData.totalVtxCount * DrawVert.SIZE.L
                    val indexSize = drawData.totalIdxCount * DrawIdx.BYTES.L
                    if (rb.vertexBuffer[0] == VK_NULL_HANDLE || rb.vertexBufferSize[0] < vertexSize)
                        createOrResizeBuffer(rb.vertexBuffer, rb.vertexBufferMemory, rb.vertexBufferSize, vertexSize, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
                    if (rb.indexBuffer[0] == VK_NULL_HANDLE || rb.indexBufferSize[0] < indexSize)
                        createOrResizeBuffer(rb.indexBuffer, rb.indexBufferMemory, rb.indexBufferSize, indexSize, VK_BUFFER_USAGE_INDEX_BUFFER_BIT)

                    // Upload vertex/index data into a single contiguous GPU buffer
                    val vtxDst = s.callocPointer(1)
                    val idxDst = s.callocPointer(1)
                    var err = vkMapMemory(v.device, rb.vertexBufferMemory[0], 0, vertexSize, 0, vtxDst)
                    checkVkResult(err)
                    err = vkMapMemory(v.device, rb.indexBufferMemory[0], 0, indexSize, 0, idxDst)
                    checkVkResult(err)
                    for (cmdList in drawData.cmdLists) {
                        MemoryUtil.memCopy(cmdList.vtxBuffer.adr, vtxDst[0], cmdList.vtxBuffer.sizeByte.L)
                        MemoryUtil.memCopy(cmdList.idxBuffer.adr, idxDst[0], cmdList.idxBuffer.remSize.L)
                        vtxDst[0] += cmdList.vtxBuffer.sizeByte.L
                        idxDst[0] += cmdList.idxBuffer.remSize.L
                    }
                    val range = VkMappedMemoryRange.callocStack(2, s)
                    range[0].sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
                            .memory(rb.vertexBufferMemory[0])
                            .size(VK_WHOLE_SIZE)
                    range[1].sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
                            .memory(rb.indexBufferMemory[0])
                            .size(VK_WHOLE_SIZE)
                    err = vkFlushMappedMemoryRanges(v.device, range)
                    checkVkResult(err)
                    vkUnmapMemory(v.device, rb.vertexBufferMemory[0])
                    vkUnmapMemory(v.device, rb.indexBufferMemory[0])
                }

                // Setup desired Vulkan state
                setupRenderState(drawData, pipeline, commandBuffer, rb, fbSize)

                // Will project scissor/clipping rectangles into framebuffer space
                val clipOff = drawData.displayPos         // (0,0) unless using multi-viewports
                val clipScale = drawData.framebufferScale // (1,1) unless using retina display which are often (2,2)

                // Render command lists
                // (Because we merged all buffers into a single one, we maintain our own offset into them)
                var globalVtxOffset = 0
                var globalIdxOffset = 0
                for (cmdList in drawData.cmdLists) {
                    for (cmd in cmdList.cmdBuffer) {
                        val cb = cmd.userCallback
                        if (cb != null)
                        // User callback, registered via ImDrawList::AddCallback()
                        // (ImDrawCallback_ResetRenderState is a special callback value used by the user to request the renderer to reset render state.)
                            if (cmd.resetRenderState)
                                setupRenderState(drawData, pipeline, commandBuffer, rb, fbSize)
                            else
                                cb(cmdList, cmd)
                        else {
                            // Project scissor/clipping rectangles into framebuffer space
                            val clipRect = Vec4 { (cmd.clipRect[it] - clipOff[it % 2]) * clipScale[it % 2] }

                            if (clipRect.x < fbSize.x && clipRect.y < fbSize.y && clipRect.z >= 0f && clipRect.w >= 0f) {
                                // Negative offsets are illegal for vkCmdSetScissor
                                if (clipRect.x < 0f)
                                    clipRect.x = 0f
                                if (clipRect.y < 0f)
                                    clipRect.y = 0f

                                // Apply scissor/clipping rectangle
                                val scissor = VkRect2D.callocStack(1, s).apply {
                                    it.offset().x(clipRect.x.i)
                                    it.offset().y(clipRect.y.i)
                                    it.extent().width((clipRect.z - clipRect.x).i)
                                    it.extent().height((clipRect.w - clipRect.y).i)
                                }
                                vkCmdSetScissor(commandBuffer, 0, scissor)

                                // Draw
                                vkCmdDrawIndexed(commandBuffer, cmd.elemCount, 1, cmd.idxOffset + globalIdxOffset, cmd.vtxOffset + globalVtxOffset, 0)
                            }
                        }
                    }
                    globalIdxOffset += cmdList.idxBuffer.remSize
                    globalVtxOffset += cmdList.vtxBuffer.sizeByte
                }
            }
        }
    }

    fun createFontsTexture(commandBuffer: VkCommandBuffer): Boolean = Stack { s ->

        val v = gVulkanInitInfo
        val io = ImGui.io

        val (pixels, size, bpp) = io.fonts.getTexDataAsRGBA32()
        val uploadSize = size.x * size.y * bpp * Byte.BYTES.L

        // Create the Image:
        run {
            val info = VkImageCreateInfo.callocStack(s)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .format(VK_FORMAT_R8G8B8A8_UNORM)
                    .extent {
                        it.width(size.x)
                        it.height(size.y)
                        it.depth(1)
                    }
                    .mipLevels(1)
                    .arrayLayers(1)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .tiling(VK_IMAGE_TILING_OPTIMAL)
                    .usage(VK_IMAGE_USAGE_SAMPLED_BIT or VK_IMAGE_USAGE_TRANSFER_DST_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            var err = vkCreateImage(v.device, info, v.allocator, pL)
            gFontImage = pL[0]
            checkVkResult(err)
            val req = VkMemoryRequirements.callocStack(s)
            vkGetImageMemoryRequirements(v.device, gFontImage, req)
            val allocInfo = VkMemoryAllocateInfo.callocStack(s)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(req.size())
                    .memoryTypeIndex(memoryType(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, req.memoryTypeBits()))
            err = vkAllocateMemory(v.device, allocInfo, v.allocator, pL)
            gFontMemory = pL[0]
            checkVkResult(err)
            err = vkBindImageMemory(v.device, gFontImage, gFontMemory, 0)
            checkVkResult(err)
        }

        // Create the Image View:
        run {
            val info = VkImageViewCreateInfo.callocStack(s)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(gFontImage)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(VK_FORMAT_R8G8B8A8_UNORM)
                    .subresourceRange {
                        it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                                .levelCount(1)
                                .layerCount(1)
                    }
            val err = vkCreateImageView(v.device, info, v.allocator, pL)
            gFontView = pL[0]
            checkVkResult(err)
        }

        // Update the Descriptor Set:
        run {
            val descImage = VkDescriptorImageInfo.callocStack(1, s)
                    .sampler(gFontSampler)
                    .imageView(gFontView)
                    .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            val writeDesc = VkWriteDescriptorSet.callocStack(1, s)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(gDescriptorSet)
                    .descriptorCount(1)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .pImageInfo(descImage)
            vkUpdateDescriptorSets(v.device, writeDesc, null)
        }

        // Create the Upload Buffer:
        run {
            val bufferInfo = VkBufferCreateInfo.callocStack(s)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(uploadSize)
                    .usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            var err = vkCreateBuffer(v.device, bufferInfo, v.allocator, pL)
            gUploadBuffer = pL[0]
            checkVkResult(err)
            val req = VkMemoryRequirements.callocStack(s)
            vkGetBufferMemoryRequirements(v.device, gUploadBuffer, req)
            gBufferMemoryAlignment = if (gBufferMemoryAlignment > req.alignment()) gBufferMemoryAlignment else req.alignment()
            val allocInfo = VkMemoryAllocateInfo.callocStack(s)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(req.size())
                    .memoryTypeIndex(memoryType(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT, req.memoryTypeBits()))
            err = vkAllocateMemory(v.device, allocInfo, v.allocator, pL)
            gUploadBufferMemory = pL[0]
            checkVkResult(err)
            err = vkBindBufferMemory(v.device, gUploadBuffer, gUploadBufferMemory, 0)
            checkVkResult(err)
        }

        // Upload to Buffer:
        run {
            val map = s.callocPointer(1)
            var err = vkMapMemory(v.device, gUploadBufferMemory, 0, uploadSize, 0, map)
            checkVkResult(err)
            MemoryUtil.memCopy(pixels.adr, map[0], uploadSize)
            val range = VkMappedMemoryRange.callocStack(1, s)
                    .sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
                    .memory(gUploadBufferMemory)
                    .size(uploadSize)
            err = vkFlushMappedMemoryRanges(v.device, range)
            checkVkResult(err)
            vkUnmapMemory(v.device, gUploadBufferMemory)
        }

        // Copy to Image:
        run {
            val copyBarrier = VkImageMemoryBarrier.callocStack(1, s)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(gFontImage)
                    .subresourceRange {
                        it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        it.levelCount(1)
                        it.layerCount(1)
                    }
            vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_HOST_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, copyBarrier)

            val region = VkBufferImageCopy.callocStack(1, s)
                    .imageSubresource {
                        it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        it.layerCount(1)
                    }
                    .imageExtent {
                        it.width(size.x)
                        it.height(size.y)
                        it.depth(1)
                    }
            vkCmdCopyBufferToImage(commandBuffer, gUploadBuffer, gFontImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region)

            val useBarrier = VkImageMemoryBarrier.callocStack(1, s)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                    .oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    .newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(gFontImage)
                    .subresourceRange {
                        it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        it.levelCount(1)
                        it.layerCount(1)
                    }
            vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, null, useBarrier)
        }

        // Store our identifier
        io.fonts.texID = gFontImage.i

        true
    }

    fun destroyFontUploadObjects() {
        val v = gVulkanInitInfo
        if (gUploadBuffer != VK_NULL_HANDLE) {
            vkDestroyBuffer(v.device, gUploadBuffer, v.allocator)
            gUploadBuffer = VK_NULL_HANDLE
        }
        if (gUploadBufferMemory != VK_NULL_HANDLE) {
            vkFreeMemory(v.device, gUploadBufferMemory, v.allocator)
            gUploadBufferMemory = VK_NULL_HANDLE
        }
    }

    fun setMinImageCount(minImageCount: Int) {
        assert(minImageCount >= 2)
        if (gVulkanInitInfo.minImageCount == minImageCount)
            return

        val v = gVulkanInitInfo
        val err = vkDeviceWaitIdle(v.device)
        checkVkResult(err)
        ImplVulkanH_.destroyWindowRenderBuffers(v.device, gMainWindowRenderBuffers, v.allocator)
        gVulkanInitInfo.minImageCount = minImageCount
    }

    // Forward Declarations
    //bool ImGui_ImplVulkan_CreateDeviceObjects();
    //void ImGui_ImplVulkan_DestroyDeviceObjects();

    fun createDeviceObjects(): Boolean = Stack { s ->

        val v = gVulkanInitInfo

        if (gFontSampler == VK_NULL_HANDLE) {
            val info = VkSamplerCreateInfo.callocStack(s)
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .magFilter(VK_FILTER_LINEAR)
                    .minFilter(VK_FILTER_LINEAR)
                    .mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
                    .addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .minLod(-1000f)
                    .maxLod(1000f)
                    .maxAnisotropy(1f)
            val err = vkCreateSampler(v.device, info, v.allocator, pL)
            gFontSampler = pL[0]
            checkVkResult(err)
        }

        if (gDescriptorSetLayout == VK_NULL_HANDLE) {
            val sampler = s.longs(gFontSampler)
            val binding = VkDescriptorSetLayoutBinding.callocStack(1, s)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .pImmutableSamplers(sampler)
            val info = VkDescriptorSetLayoutCreateInfo.callocStack(s)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pBindings(binding)
            val err = vkCreateDescriptorSetLayout(v.device, info, v.allocator, pL)
            gDescriptorSetLayout = pL[0]
            checkVkResult(err)
        }

        // Create Descriptor Set:
        run {
            val setLayouts = s.longs(gDescriptorSetLayout)
            val allocInfo = VkDescriptorSetAllocateInfo.callocStack(s)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(v.descriptorPool)
                    .pSetLayouts(setLayouts)
            val err = vkAllocateDescriptorSets(v.device, allocInfo, pL)
            gDescriptorSet = pL[0]
            checkVkResult(err)
        }

        if (gPipelineLayout == VK_NULL_HANDLE) {
            // Constants: we are using 'vec2 offset' and 'vec2 scale' instead of a full 3d projection matrix
            val pushConstants = VkPushConstantRange.callocStack(1, s)
                    .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                    .offset(Float.BYTES * 0)
                    .size(Float.BYTES * 4)
            val setLayout = s.longs(gDescriptorSetLayout)
            val layoutInfo = VkPipelineLayoutCreateInfo.callocStack(s)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .pSetLayouts(setLayout)
                    .pPushConstantRanges(pushConstants)
            val err = vkCreatePipelineLayout(v.device, layoutInfo, v.allocator, pL)
            gPipelineLayout = pL[0]
            checkVkResult(err)
        }

        createPipeline(v.device, v.allocator, v.pipelineCache, gRenderPass, v.msaaSamples, ::gPipeline, gSubpass)

        true
    }

    fun destroyDeviceObjects() {

        val v = gVulkanInitInfo
        ImplVulkanH_.destroyWindowRenderBuffers(v.device, gMainWindowRenderBuffers, v.allocator)
        destroyFontUploadObjects()

        if (gShaderModuleVert != VK_NULL_HANDLE) {
            vkDestroyShaderModule(v.device, gShaderModuleVert, v.allocator)
            gShaderModuleVert = VK_NULL_HANDLE
        }
        if (gShaderModuleFrag != VK_NULL_HANDLE) {
            vkDestroyShaderModule(v.device, gShaderModuleFrag, v.allocator)
            gShaderModuleFrag = VK_NULL_HANDLE
        }
        if (gFontView != VK_NULL_HANDLE) {
            vkDestroyImageView(v.device, gFontView, v.allocator)
            gFontView = VK_NULL_HANDLE
        }
        if (gFontImage != VK_NULL_HANDLE) {
            vkDestroyImage(v.device, gFontImage, v.allocator)
            gFontImage = VK_NULL_HANDLE
        }
        if (gFontMemory != VK_NULL_HANDLE) {
            vkFreeMemory(v.device, gFontMemory, v.allocator)
            gFontMemory = VK_NULL_HANDLE
        }
        if (gFontSampler != VK_NULL_HANDLE) {
            vkDestroySampler(v.device, gFontSampler, v.allocator)
            gFontSampler = VK_NULL_HANDLE
        }
        if (gDescriptorSetLayout != VK_NULL_HANDLE) {
            vkDestroyDescriptorSetLayout(v.device, gDescriptorSetLayout, v.allocator)
            gDescriptorSetLayout = VK_NULL_HANDLE
        }
        if (gPipelineLayout != VK_NULL_HANDLE) {
            vkDestroyPipelineLayout(v.device, gPipelineLayout, v.allocator)
            gPipelineLayout = VK_NULL_HANDLE
        }
        if (gPipeline != VK_NULL_HANDLE) {
            vkDestroyPipeline(v.device, gPipeline, v.allocator)
            gPipeline = VK_NULL_HANDLE
        }
    }
}