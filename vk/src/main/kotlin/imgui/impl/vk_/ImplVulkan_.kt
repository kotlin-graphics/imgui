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
                                val scissor = VkRect2D.callocStack(1, s)
                                        .offset {
                                            it.x(clipRect.x.i)
                                            it.y(clipRect.y.i)
                                        }
                                        .extent {
                                            it.width((clipRect.z - clipRect.x).i)
                                            it.height((clipRect.w - clipRect.y).i)
                                        }
                                vkCmdSetScissor(commandBuffer, 0, scissor)

                                // Draw
                                vkCmdDrawIndexed(commandBuffer, cmd.elemCount, 1, cmd.idxOffset + globalIdxOffset, cmd.vtxOffset + globalVtxOffset, 0)
                            }
                        }
                    }
                    globalIdxOffset += cmdList.idxBuffer.rem
                    globalVtxOffset += cmdList.vtxBuffer.rem
                }
            }
        }
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