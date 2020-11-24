package imgui.impl.vk

import glm_.L
import glm_.i
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import imgui.BackendFlag
import imgui.ImGui
import imgui.ImGui.io
import imgui.internal.DrawData
import imgui.internal.DrawIdx
import imgui.internal.DrawVert
import imgui.or
import kool.BYTES
import kool.adr
import kool.rem
import kool.remSize
import org.lwjgl.system.MemoryUtil.memCopy
import org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED
import vkk.*
import vkk.entities.*
import vkk.identifiers.*
import vkk.vk10.*
import vkk.vk10.structs.*


class ImplVulkan(info: InitInfo, renderPass: VkRenderPass) {

    /** Initialization data, for ImGui_ImplVulkan_Init()
     *  [Please zero-clear before use!]
     *  ~ImGui_ImplVulkan_InitInfo */
    class InitInfo {
        lateinit var instance: Instance
        lateinit var physicalDevice: PhysicalDevice
        lateinit var device: Device
        var queueFamily = 0
        lateinit var queue: Queue
        var pipelineCache = VkPipelineCache.NULL
        var descriptorPool = VkDescriptorPool.NULL
        var subpass = 0
        var minImageCount = 0          // >= 2
        var imageCount = 0             // >= MinImageCount
        var msaaSamples = VkSampleCount._1_BIT   // >= VK_SAMPLE_COUNT_1_BIT
        //    const VkAllocationCallbacks* Allocator;
        //    void                (*CheckVkResultFn)(VkResult err);
    }

    // Called by user code
    //IMGUI_IMPL_API bool     ImGui_ImplVulkan_Init(ImGui_ImplVulkan_InitInfo* info, VkRenderPass render_pass);
    //IMGUI_IMPL_API void     ImGui_ImplVulkan_Shutdown();
    //IMGUI_IMPL_API void     ImGui_ImplVulkan_NewFrame();
    //IMGUI_IMPL_API void     ImGui_ImplVulkan_RenderDrawData(ImDrawData* draw_data, VkCommandBuffer command_buffer, VkPipeline pipeline = VK_NULL_HANDLE);
    //IMGUI_IMPL_API bool     ImGui_ImplVulkan_CreateFontsTexture(VkCommandBuffer command_buffer);
    //IMGUI_IMPL_API void     ImGui_ImplVulkan_DestroyFontUploadObjects();
    //IMGUI_IMPL_API void     ImGui_ImplVulkan_SetMinImageCount(uint32_t min_image_count); // To override MinImageCount after initialization (e.g. if swap chain is recreated)

    /** ~ImGui_ImplVulkan_Init */
    init {
        // Setup backend capabilities flags
        io.backendRendererName = "imgui_impl_vulkan"
        io.backendFlags = io.backendFlags or BackendFlag.RendererHasVtxOffset  // We can honor the ImDrawCmd::VtxOffset field, allowing for large meshes.

        info.apply {
            assert(instance.isValid)
            assert(physicalDevice.isValid)
            assert(device.isValid)
            assert(queue.isValid)
            assert(descriptorPool.isValid)
            assert(info.minImageCount >= 2)
            assert(info.imageCount >= info.minImageCount)
            assert(renderPass.isValid)
        }
        gVulkanInitInfo = info
        gRenderPass = renderPass
        gSubpass = info.subpass

        createDeviceObjects()
    }

    /** ~ImGui_ImplVulkan_Shutdown */
    fun shutdown() = destroyDeviceObjects()

    /** ~ImGui_ImplVulkan_NewFrame */
    fun newFrame() {}

    companion object {
        /** Render function */
        fun renderDrawData(drawData: DrawData, commandBuffer: CommandBuffer, pipeline_: VkPipeline = VkPipeline.NULL) {

            var pipeline = pipeline_
            // Avoid rendering when minimized, scale coordinates for retina displays (screen coordinates != framebuffer coordinates)
            val fbSize = Vec2i(drawData.displaySize * drawData.framebufferScale)
            if (fbSize anyLessThanEqual 0)
                return

            val v = gVulkanInitInfo
            if (pipeline.isInvalid)
                pipeline = gPipeline

            // Allocate array to store enough vertex/index buffers
            val wrb = gMainWindowRenderBuffers
            if (wrb.frameRenderBuffers == null) {
                wrb.index = 0
                wrb.frameRenderBuffers = Array(v.imageCount) { ImplVulkanH.FrameRenderBuffers() }
            }
            wrb.index = (wrb.index + 1) % wrb.count
            val rb = wrb.frameRenderBuffers!![wrb.index]

            if (drawData.totalVtxCount > 0) {
                // Create or resize the vertex/index buffers
                val vertexSize = VkDeviceSize(drawData.totalVtxCount * DrawVert.SIZE)
                val indexSize = VkDeviceSize(drawData.totalIdxCount * DrawIdx.BYTES)
                if (rb.vertexBuffer.isInvalid || rb.vertexBufferSize < vertexSize)
                    createOrResizeBuffer(rb::vertexBuffer, rb::vertexBufferMemory, rb::vertexBufferSize, vertexSize, VkBufferUsage.VERTEX_BUFFER_BIT)
                if (rb.indexBuffer.isInvalid || rb.indexBufferSize < indexSize)
                    createOrResizeBuffer(rb::indexBuffer, rb::indexBufferMemory, rb::indexBufferSize, indexSize, VkBufferUsage.INDEX_BUFFER_BIT)

                // Upload vertex/index data into a single contiguous GPU buffer
                //            var vtxDst: DrawVert? = null
                var vtxDst = v.device.mapMemory(rb.vertexBufferMemory, VkDeviceSize(0), vertexSize)
                var idxDst = v.device.mapMemory(rb.indexBufferMemory, VkDeviceSize(0), indexSize)
                for (cmdList in drawData.cmdLists) {
                    memCopy(cmdList.vtxBuffer.adr, vtxDst, cmdList.vtxBuffer.sizeByte.L)
                    memCopy(cmdList.idxBuffer.adr, idxDst, cmdList.idxBuffer.remSize.L)
                    vtxDst += cmdList.vtxBuffer.sizeByte.L
                    idxDst += cmdList.idxBuffer.remSize.L
                }
                val range = arrayOf(
                        MappedMemoryRange(rb.vertexBufferMemory, size = VkDeviceSize.WHOLE_SIZE),
                        MappedMemoryRange(rb.indexBufferMemory, size = VkDeviceSize.WHOLE_SIZE))
                v.device flushMappedMemoryRanges range
                v.device unmapMemory rb.vertexBufferMemory
                v.device unmapMemory rb.indexBufferMemory
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
                    if (cb != null) {
                        // User callback, registered via ImDrawList::AddCallback()
                        // (ImDrawCallback_ResetRenderState is a special callback value used by the user to request the renderer to reset render state.)
                        if (cmd.resetRenderState)
                            setupRenderState(drawData, pipeline, commandBuffer, rb, fbSize)
                        else
                            cb(cmdList, cmd)
                    } else {
                        // Project scissor/clipping rectangles into framebuffer space
                        val clipRect = Vec4 { (cmd.clipRect[it] - clipOff[it % 2]) * clipScale[it % 2] }

                        if (clipRect.x < fbSize.x && clipRect.y < fbSize.y && clipRect.z >= 0f && clipRect.w >= 0f) {
                            // Negative offsets are illegal for vkCmdSetScissor
                            if (clipRect.x < 0f)
                                clipRect.x = 0f
                            if (clipRect.y < 0f)
                                clipRect.y = 0f

                            // Apply scissor/clipping rectangle
                            val offset = Offset2D(clipRect.x.i, clipRect.y.i)
                            val extent = Extent2D((clipRect.z - clipRect.x).i, (clipRect.w - clipRect.y).i)
                            commandBuffer setScissor Rect2D(offset, extent)

                            // Draw
                            commandBuffer.drawIndexed(cmd.elemCount, 1, cmd.idxOffset + globalIdxOffset, cmd.vtxOffset + globalVtxOffset, 0)
                        }
                    }
                }
                globalIdxOffset += cmdList.idxBuffer.rem
                globalVtxOffset += cmdList.vtxBuffer.rem
            }
        }
    }

    /** ~ImGui_ImplVulkan_CreateFontsTexture */
    fun createFontsTexture(commandBuffer: CommandBuffer): Boolean {

        val v = gVulkanInitInfo
        val io = ImGui.io

        val (pixels, size, bpp) = io.fonts.getTexDataAsRGBA32()
        val uploadSize = VkDeviceSize(size.x * size.y * bpp * Byte.BYTES)

        // Create the Image:
        run {
            val info = ImageCreateInfo(
                    imageType = VkImageType._2D,
                    format = VkFormat.R8G8B8A8_UNORM,
                    extent = Extent3D(size, 1),
                    mipLevels = 1,
                    arrayLayers = 1,
                    samples = VkSampleCount._1_BIT,
                    tiling = VkImageTiling.OPTIMAL,
                    usage = VkImageUsage.SAMPLED_BIT or VkImageUsage.TRANSFER_DST_BIT,
                    sharingMode = VkSharingMode.EXCLUSIVE,
                    initialLayout = VkImageLayout.UNDEFINED)
            gFontImage = v.device createImage info
            val req = v.device getImageMemoryRequirements gFontImage
            val allocInfo = MemoryAllocateInfo(
                    allocationSize = req.size,
                    memoryTypeIndex = memoryType(VkMemoryProperty.DEVICE_LOCAL_BIT.i, req.memoryTypeBits))
            gFontMemory = v.device allocateMemory allocInfo
            v.device.bindImageMemory(gFontImage, gFontMemory, VkDeviceSize(0))
        }

        // Create the Image View:
        run {
            val info = ImageViewCreateInfo(
                    image = gFontImage,
                    viewType = VkImageViewType._2D,
                    format = VkFormat.R8G8B8A8_UNORM,
                    subresourceRange = ImageSubresourceRange(
                            aspectMask = VkImageAspect.COLOR_BIT.i,
                            levelCount = 1,
                            layerCount = 1))
            gFontView = v.device createImageView info
        }

        // Update the Descriptor Set:
        run {
            val descImage = DescriptorImageInfo(
                    sampler = gFontSampler,
                    imageView = gFontView,
                    imageLayout = VkImageLayout.SHADER_READ_ONLY_OPTIMAL)
            val writeDesc = WriteDescriptorSet(
                    dstSet = gDescriptorSet,
                    descriptorCount = 1,
                    descriptorType = VkDescriptorType.COMBINED_IMAGE_SAMPLER,
                    imageInfo = arrayOf(descImage))
            v.device.updateDescriptorSet(writeDesc)
        }

        // Create the Upload Buffer:
        run {
            val bufferInfo = BufferCreateInfo(
                    size = uploadSize,
                    usageFlags = VkBufferUsage.TRANSFER_SRC_BIT.i,
                    sharingMode = VkSharingMode.EXCLUSIVE)
            gUploadBuffer = v.device createBuffer bufferInfo
            val req = v.device getBufferMemoryRequirements gUploadBuffer
            gBufferMemoryAlignment = if (gBufferMemoryAlignment > req.alignment) gBufferMemoryAlignment else req.alignment
            val allocInfo = MemoryAllocateInfo(
                    allocationSize = req.size,
                    memoryTypeIndex = memoryType(VkMemoryProperty.HOST_VISIBLE_BIT.i, req.memoryTypeBits))
            gUploadBufferMemory = v.device allocateMemory allocInfo
            v.device.bindBufferMemory(gUploadBuffer, gUploadBufferMemory)
        }

        // Upload to Buffer:
        v.device.mappedMemory(gUploadBufferMemory, VkDeviceSize(0), uploadSize) { map ->
            memCopy(pixels.adr, map, uploadSize)
            val range = MappedMemoryRange(memory = gUploadBufferMemory, size = uploadSize)
            v.device flushMappedMemoryRanges range
        }

        // Copy to Image:
        run {
            val copyBarrier = ImageMemoryBarrier(
                    dstAccessMask = VkAccess.TRANSFER_WRITE_BIT.i,
                    oldLayout = VkImageLayout.UNDEFINED,
                    newLayout = VkImageLayout.TRANSFER_DST_OPTIMAL,
                    srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED,
                    dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED,
                    image = gFontImage,
                    subresourceRange = ImageSubresourceRange(
                            aspectMask = VkImageAspect.COLOR_BIT.i,
                            levelCount = 1,
                            layerCount = 1))
            commandBuffer.pipelineBarrier(VkPipelineStage.HOST_BIT.i, VkPipelineStage.TRANSFER_BIT.i, 0, imageMemoryBarrier = copyBarrier)

            val region = BufferImageCopy(
                    imageSubresource = ImageSubresourceLayers(
                            aspectMask = VkImageAspect.COLOR_BIT.i,
                            layerCount = 1),
                    imageExtent = Extent3D(size, 1))
            commandBuffer.copyBufferToImage(gUploadBuffer, gFontImage, VkImageLayout.TRANSFER_DST_OPTIMAL, region)

            val useBarrier = ImageMemoryBarrier(
                    srcAccessMask = VkAccess.TRANSFER_WRITE_BIT.i,
                    dstAccessMask = VkAccess.SHADER_READ_BIT.i,
                    oldLayout = VkImageLayout.TRANSFER_DST_OPTIMAL,
                    newLayout = VkImageLayout.SHADER_READ_ONLY_OPTIMAL,
                    srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED,
                    dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED,
                    image = gFontImage,
                    subresourceRange = ImageSubresourceRange(
                            aspectMask = VkImageAspect.COLOR_BIT.i,
                            levelCount = 1,
                            layerCount = 1))
            commandBuffer.pipelineBarrier(VkPipelineStage.TRANSFER_BIT.i, VkPipelineStage.FRAGMENT_SHADER_BIT.i, 0, imageMemoryBarrier = useBarrier)
        }

        // Store our identifier
        io.fonts.texID = gFontImage.L.i

        return true
    }

    /** ~ImGui_ImplVulkan_DestroyFontUploadObjects */
    fun destroyFontUploadObjects() {
        val v = gVulkanInitInfo
        if (gUploadBuffer.isValid) {
            v.device destroy gUploadBuffer
            gUploadBuffer = VkBuffer.NULL
        }
        if (gUploadBufferMemory.isValid) {
            v.device.freeMemory(gUploadBufferMemory)
            gUploadBufferMemory = VkDeviceMemory.NULL
        }
    }

    /** ~ImGui_ImplVulkan_SetMinImageCount */
    fun setMinImageCount(minImageCount: Int) {
        assert(minImageCount >= 2)
        if (gVulkanInitInfo.minImageCount == minImageCount)
            return

        val v = gVulkanInitInfo
        v.device.waitIdle()
        ImplVulkanH.destroyWindowRenderBuffers(v.device, gMainWindowRenderBuffers)
        gVulkanInitInfo.minImageCount = minImageCount
    }

    // Forward Declarations
    //bool ImGui_ImplVulkan_CreateDeviceObjects();
    //void ImGui_ImplVulkan_DestroyDeviceObjects();

    /** ~ImGui_ImplVulkan_CreateDeviceObjects */
    fun createDeviceObjects(): Boolean {

        val v = gVulkanInitInfo

        if (gFontSampler.isInvalid) {
            val info = SamplerCreateInfo(
                    magMinFilter = VkFilter.LINEAR,
                    mipmapMode = VkSamplerMipmapMode.LINEAR,
                    addressModeUVW = VkSamplerAddressMode.REPEAT,
                    minLod = -1000f,
                    maxLod = 1000f,
                    maxAnisotropy = 1f)
            gFontSampler = v.device createSampler info
        }

        if (gDescriptorSetLayout.isInvalid) {
            val binding = DescriptorSetLayoutBinding(
                    descriptorType = VkDescriptorType.COMBINED_IMAGE_SAMPLER,
                    descriptorCount = 1,
                    stageFlags = VkShaderStage.FRAGMENT_BIT.i,
                    immutableSampler = gFontSampler)
            gDescriptorSetLayout = v.device createDescriptorSetLayout DescriptorSetLayoutCreateInfo(binding = binding)
        }

        // Create Descriptor Set:
        gDescriptorSet = v.device allocateDescriptorSet DescriptorSetAllocateInfo(v.descriptorPool, gDescriptorSetLayout)

        if (gPipelineLayout.isInvalid) {
            // Constants: we are using 'vec2 offset' and 'vec2 scale' instead of a full 3d projection matrix
            val pushConstants = PushConstantRange(
                    stageFlags = VkShaderStage.VERTEX_BIT.i,
                    offset = Float.BYTES * 0,
                    size = Float.BYTES * 4)
            val layoutInfo = PipelineLayoutCreateInfo(
                    setLayout = gDescriptorSetLayout,
                    pushConstantRange = pushConstants)
            gPipelineLayout = v.device createPipelineLayout layoutInfo
        }

        gPipeline = createPipeline(v.device, v.pipelineCache, gRenderPass, v.msaaSamples, gSubpass)

        return true
    }

    /** ~ImGui_ImplVulkan_DestroyDeviceObjects */
    fun destroyDeviceObjects() {
        val v = gVulkanInitInfo
        ImplVulkanH.destroyWindowRenderBuffers(v.device, gMainWindowRenderBuffers)
        destroyFontUploadObjects()

        v.device.apply {
            if (gShaderModuleVert.isValid) {
                destroy(gShaderModuleVert)
                gShaderModuleVert = VkShaderModule.NULL
            }
            if (gShaderModuleFrag.isValid) {
                destroy(gShaderModuleFrag)
                gShaderModuleFrag = VkShaderModule.NULL
            }
            if (gFontView.isValid) {
                destroy(gFontView)
                gFontView = VkImageView.NULL
            }
            if (gFontImage.isValid) {
                destroy(gFontImage)
                gFontImage = VkImage.NULL
            }
            if (gFontMemory.isValid) {
                freeMemory(gFontMemory)
                gFontMemory = VkDeviceMemory.NULL
            }
            if (gFontSampler.isValid) {
                destroy(gFontSampler)
                gFontSampler = VkSampler.NULL
            }
            if (gDescriptorSetLayout.isValid) {
                destroy(gDescriptorSetLayout)
                gDescriptorSetLayout = VkDescriptorSetLayout.NULL
            }
            if (gPipelineLayout.isValid) {
                destroy(gPipelineLayout)
                gPipelineLayout = VkPipelineLayout.NULL
            }
            if (gPipeline.isValid) {
                destroy(gPipeline)
                gPipeline = VkPipeline.NULL
            }
        }
    }
}