package imgui.impl.vk_

import glm_.L
import glm_.f
import glm_.has
import glm_.i
import glm_.vec2.Vec2i
import imgui.ImGui
import imgui.internal.DrawData
import imgui.internal.DrawIdx
import imgui.internal.DrawVert
import kool.*
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.MemoryUtil.memASCII
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import java.nio.LongBuffer
import kotlin.reflect.KMutableProperty0

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
                    it.levelCount(1)
                    it.layerCount(1)
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

fun createShaderModules(device: VkDevice, allocator: VkAllocationCallbacks?) = Stack { s ->
    // Create the shader modules
    if (gShaderModuleVert == NULL) {
        val vertInfo = VkShaderModuleCreateInfo.callocStack(s)
                .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                .pCode(__glsl_shader_vert_spv.toBuffer(s))
        val err = vkCreateShaderModule(device, vertInfo, allocator, pL)
        gShaderModuleVert = pL[0]
        checkVkResult(err)
    }
    if (gShaderModuleFrag == NULL) {
        val fragInfo = VkShaderModuleCreateInfo.callocStack(s)
                .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                .pCode(__glsl_shader_frag_spv.toBuffer(s))
        val err = vkCreateShaderModule(device, fragInfo, allocator, pL)
        gShaderModuleFrag = pL[0]
        checkVkResult(err)
    }
}

fun createFontSampler(device: VkDevice, allocator: VkAllocationCallbacks?) = Stack { s ->

    if (gFontSampler != VK_NULL_HANDLE)
        return

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
    val err = vkCreateSampler(device, info, allocator, pL)
    gFontSampler = pL[0]
    checkVkResult(err)
}

fun createDescriptorSetLayout(device: VkDevice, allocator: VkAllocationCallbacks?) = Stack { s ->

    if (gDescriptorSetLayout != VK_NULL_HANDLE)
        return

    createFontSampler(device, allocator)
    val sampler = s.longs(gFontSampler)
    val binding = VkDescriptorSetLayoutBinding.callocStack(1, s)
            .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
            .descriptorCount(1)
            .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
            .pImmutableSamplers(sampler)
    val info = VkDescriptorSetLayoutCreateInfo.callocStack(s)
            .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
            .pBindings(binding)
    val err = vkCreateDescriptorSetLayout(device, info, allocator, pL)
    gDescriptorSetLayout = pL[0]
    checkVkResult(err)
}

fun createPipelineLayout(device: VkDevice, allocator: VkAllocationCallbacks?) = Stack { s ->

    if (gPipelineLayout != VK_NULL_HANDLE)
        return

    // Constants: we are using 'vec2 offset' and 'vec2 scale' instead of a full 3d projection matrix
    createDescriptorSetLayout(device, allocator)
    val pushConstants = VkPushConstantRange.callocStack(1, s)
            .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
            .offset(Float.BYTES * 0)
            .size(Float.BYTES * 4)
    val setLayout = s.longs(gDescriptorSetLayout)
    val layoutInfo = VkPipelineLayoutCreateInfo.callocStack(s)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
            .pSetLayouts(setLayout)
            .pPushConstantRanges(pushConstants)
    val err = vkCreatePipelineLayout(device, layoutInfo, allocator, pL)
    gPipelineLayout = pL[0]
    checkVkResult(err)
}

fun createPipeline(device: VkDevice, allocator: VkAllocationCallbacks?, pipelineCache: Long, renderPass: Long,
                   msaaSamples: Int, pPipeline: KMutableProperty0<Long>, subpass: Int) = Stack { s ->

    var pipeline by pPipeline

    createShaderModules(device, allocator)

    val stage = VkPipelineShaderStageCreateInfo.callocStack(2, s)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
            .stage(VK_SHADER_STAGE_VERTEX_BIT)
            .module(gShaderModuleVert)
            .pName(memASCII("main"))
            .apply(1) {
                it.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                it.stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                it.module(gShaderModuleFrag)
                it.pName(memASCII("main"))
            }

    val bindingDesc = VkVertexInputBindingDescription.callocStack(1, s)
            .stride(DrawVert.SIZE)
            .inputRate(VK_VERTEX_INPUT_RATE_VERTEX)

    val attributeDesc = VkVertexInputAttributeDescription.callocStack(3, s)
            .location(0)
            .binding(bindingDesc[0].binding())
            .format(VK_FORMAT_R32G32_SFLOAT)
            .offset(DrawVert.OFS_POS)
            .apply(1) {
                it.location(1)
                it.binding(bindingDesc[0].binding())
                it.format(VK_FORMAT_R32G32_SFLOAT)
                it.offset(DrawVert.OFS_UV)
            }
            .apply(2) {
                it.location(2)
                it.binding(bindingDesc[0].binding())
                it.format(VK_FORMAT_R8G8B8A8_UNORM)
                it.offset(DrawVert.OFS_COL)
            }

    val vertexInfo = VkPipelineVertexInputStateCreateInfo.callocStack(s)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
            .pVertexBindingDescriptions(bindingDesc)
            .pVertexAttributeDescriptions(attributeDesc)

    val iaInfo = VkPipelineInputAssemblyStateCreateInfo.callocStack(s)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
            .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)

    val viewportInfo = VkPipelineViewportStateCreateInfo.callocStack(s)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
            .viewportCount(1)
            .scissorCount(1)

    val rasterInfo = VkPipelineRasterizationStateCreateInfo.callocStack(s)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
            .polygonMode(VK_POLYGON_MODE_FILL)
            .cullMode(VK_CULL_MODE_NONE)
            .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
            .lineWidth(1f)

    val msInfo = VkPipelineMultisampleStateCreateInfo.callocStack(s)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
            .rasterizationSamples(if (msaaSamples != 0) msaaSamples else VK_SAMPLE_COUNT_1_BIT)

    val colorAttachment = VkPipelineColorBlendAttachmentState.callocStack(1, s)
            .blendEnable(true)
            .srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
            .dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
            .colorBlendOp(VK_BLEND_OP_ADD)
            .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
            .dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
            .alphaBlendOp(VK_BLEND_OP_ADD)
            .colorWriteMask(VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT)

    val depthInfo = VkPipelineDepthStencilStateCreateInfo.callocStack(s)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)

    val blendInfo = VkPipelineColorBlendStateCreateInfo.callocStack(s)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
            .pAttachments(colorAttachment)

    val dynamicStates = s.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR)
    val dynamicState = VkPipelineDynamicStateCreateInfo.callocStack(s)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
            .pDynamicStates(dynamicStates)

    createPipelineLayout(device, allocator)

    val info = VkGraphicsPipelineCreateInfo.callocStack(1, s)
            .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
            .flags(gPipelineCreateFlags)
            .pStages(stage)
            .pVertexInputState(vertexInfo)
            .pInputAssemblyState(iaInfo)
            .pViewportState(viewportInfo)
            .pRasterizationState(rasterInfo)
            .pMultisampleState(msInfo)
            .pDepthStencilState(depthInfo)
            .pColorBlendState(blendInfo)
            .pDynamicState(dynamicState)
            .layout(gPipelineLayout)
            .renderPass(renderPass)
            .subpass(subpass)
    val err = vkCreateGraphicsPipelines(device, pipelineCache, info, allocator, pL)
    pipeline = pL[0]
    checkVkResult(err)
}

//-----------------------------------------------------------------------------
// FUNCTIONS
//-----------------------------------------------------------------------------

fun memoryType(properties: Int, typeBits: Int): Int = Stack { s ->
    val v = gVulkanInitInfo
    val prop = VkPhysicalDeviceMemoryProperties.callocStack(s)
    vkGetPhysicalDeviceMemoryProperties(v.physicalDevice, prop)
    for (i in prop.memoryTypes().indices)
        if ((prop.memoryTypes()[i].propertyFlags() and properties) == properties && typeBits has (1 shl i))
            return i
    return -1 // Unable to find memoryType
}

fun checkVkResult(err: Int) {
    if (err == 0)
        return
    error("[vulkan] Error: VkResult = $err")
}

fun createOrResizeBuffer(buffer: LongBuffer, bufferMemory: LongBuffer, pBufferSize: LongBuffer, newSize: Long, usage: Int) = Stack { s ->
    val v = gVulkanInitInfo
    if (buffer[0] != VK_NULL_HANDLE)
        vkDestroyBuffer(v.device, buffer[0], v.allocator)
    if (bufferMemory[0] != VK_NULL_HANDLE)
        vkFreeMemory(v.device, bufferMemory[0], v.allocator)

    val vertexBufferSizeAligned = ((newSize - 1) / gBufferMemoryAlignment + 1) * gBufferMemoryAlignment
    val bufferInfo = VkBufferCreateInfo.callocStack(s)
            .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
            .size(vertexBufferSizeAligned)
            .usage(usage)
            .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
    var err = vkCreateBuffer(v.device, bufferInfo, v.allocator, buffer)
    checkVkResult(err)

    val req = VkMemoryRequirements.callocStack(s)
    vkGetBufferMemoryRequirements(v.device, buffer[0], req)
    gBufferMemoryAlignment = if (gBufferMemoryAlignment > req.alignment()) gBufferMemoryAlignment else req.alignment()
    val allocInfo = VkMemoryAllocateInfo.callocStack(s)
            .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
            .allocationSize(req.size())
            .memoryTypeIndex(memoryType(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT, req.memoryTypeBits()))
    err = vkAllocateMemory(v.device, allocInfo, v.allocator, bufferMemory)
    checkVkResult(err)

    err = vkBindBufferMemory(v.device, buffer[0], bufferMemory[0], 0)
    checkVkResult(err)
    pBufferSize[0] = newSize
}

fun setupRenderState(drawData: DrawData, pipeline: Long, commandBuffer: VkCommandBuffer, rb: ImplVulkanH_.FrameRenderBuffers, fbSize: Vec2i) = Stack { s ->
    // Bind pipeline and descriptor sets:
    run {
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline)
        val descSet = s.longs(gDescriptorSet)
        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, gPipelineLayout, 0, descSet, null)
    }

    // Bind Vertex And Index Buffer:
    if (drawData.totalVtxCount > 0) {
        val vertexBuffers = s.longs(rb.vertexBuffer[0])
        val vertexOffset = s.longs(0)
        vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, vertexOffset)
        vkCmdBindIndexBuffer(commandBuffer, rb.indexBuffer[0], 0, if (DrawIdx.BYTES == 2) VK_INDEX_TYPE_UINT16 else VK_INDEX_TYPE_UINT32)
    }

    // Setup viewport:
    run {
        val viewport = VkViewport.callocStack(1, s)
                .x(0f)
                .y(0f)
                .width(fbSize.x.f)
                .height(fbSize.y.f)
                .minDepth(0f)
                .maxDepth(1f)
        vkCmdSetViewport(commandBuffer, 0, viewport)
    }

    // Setup scale and translation:
    // Our visible imgui space lies from draw_data->DisplayPps (top left) to draw_data->DisplayPos+data_data->DisplaySize (bottom right). DisplayPos is (0,0) for single viewport apps.
    run {
        val scale = s.floats(2f / drawData.displaySize.x, 2f / drawData.displaySize.y)
        val translate = s.floats(-1f - drawData.displayPos.x * scale[0], -1f - drawData.displayPos.y * scale[1])
        vkCmdPushConstants(commandBuffer, gPipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, Float.BYTES * 0, scale)
        vkCmdPushConstants(commandBuffer, gPipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, Float.BYTES * 2, translate)
    }
}