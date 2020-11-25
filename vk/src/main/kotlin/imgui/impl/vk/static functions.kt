package imgui.impl.vk

import glm_.f
import glm_.has
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import imgui.internal.DrawData
import imgui.internal.DrawIdx
import imgui.internal.DrawVert
import kool.BYTES
import kool.Stack
import kool.mFloat
import kool.toBuffer
import vkk.*
import vkk.entities.*
import vkk.identifiers.CommandBuffer
import vkk.identifiers.Device
import vkk.vk10.*
import vkk.vk10.structs.*
import kotlin.reflect.KMutableProperty0

/** [JVM] @return VkPipeline
 *  ~ImGui_ImplVulkan_CreatePipeline */
fun createPipeline(device: Device, pipelineCache: VkPipelineCache, renderPass: VkRenderPass,
                   msaaSamples: VkSampleCount, subpass: Int): VkPipeline {

    createShaderModules(device)

    val stage = arrayOf(
            PipelineShaderStageCreateInfo(
                    stage = VkShaderStage.VERTEX_BIT,
                    module = gShaderModuleVert,
                    name = "main"),
            PipelineShaderStageCreateInfo(
                    stage = VkShaderStage.FRAGMENT_BIT,
                    module = gShaderModuleFrag,
                    name = "main"))

    val bindingDesc = arrayOf(VertexInputBindingDescription(stride = DrawVert.SIZE, inputRate = VkVertexInputRate.VERTEX))

    val attributeDesc = arrayOf(
            VertexInputAttributeDescription(
                    location = 0,
                    binding = bindingDesc[0].binding,
                    format = VkFormat.R32G32_SFLOAT,
                    offset = DrawVert.OFS_POS),
            VertexInputAttributeDescription(
                    location = 1,
                    binding = bindingDesc[0].binding,
                    format = VkFormat.R32G32_SFLOAT,
                    offset = DrawVert.OFS_UV),
            VertexInputAttributeDescription(
                    location = 2,
                    binding = bindingDesc[0].binding,
                    format = VkFormat.R8G8B8A8_UNORM,
                    offset = DrawVert.OFS_COL))

    val vertexInfo = PipelineVertexInputStateCreateInfo(bindingDesc, attributeDesc)

    val iaInfo = PipelineInputAssemblyStateCreateInfo(VkPrimitiveTopology.TRIANGLE_LIST)

    // TODO add option to create
    val viewportInfo = PipelineViewportStateCreateInfo(Viewport(width = 0f, height = 0f), Rect2D(extent = Extent2D(0, 0)))

    val rasterInfo = PipelineRasterizationStateCreateInfo(
        polygonMode = VkPolygonMode.FILL,
        cullMode = VkCullMode.NONE.i,
        frontFace = VkFrontFace.COUNTER_CLOCKWISE,
        lineWidth = 1f)

    val msInfo = PipelineMultisampleStateCreateInfo(rasterizationSamples = if(msaaSamples.i != 0) msaaSamples else VkSampleCount._1_BIT)

    val colorAttachment = PipelineColorBlendAttachmentState(
    blendEnable = true,
    srcColorBlendFactor = VkBlendFactor.SRC_ALPHA,
    dstColorBlendFactor = VkBlendFactor.ONE_MINUS_SRC_ALPHA,
    colorBlendOp = VkBlendOp.ADD,
    srcAlphaBlendFactor = VkBlendFactor.ONE_MINUS_SRC_ALPHA,
    dstAlphaBlendFactor = VkBlendFactor.ZERO,
    alphaBlendOp = VkBlendOp.ADD,
    colorWriteMask = VkColorComponent.RGBA_BIT.i)

    val depthInfo = PipelineDepthStencilStateCreateInfo()

    val blendInfo = PipelineColorBlendStateCreateInfo(attachment = colorAttachment)

    val dynamicStates = arrayOf(VkDynamicState.VIEWPORT, VkDynamicState.SCISSOR)
    val dynamicState = PipelineDynamicStateCreateInfo(dynamicStates)

    createPipelineLayout(device)

    val info = GraphicsPipelineCreateInfo(
    flags = gPipelineCreateFlags,
    stages = stage,
    vertexInputState = vertexInfo,
    inputAssemblyState = iaInfo,
    viewportState = viewportInfo,
    rasterizationState = rasterInfo,
    multisampleState = msInfo,
    depthStencilState = depthInfo,
    colorBlendState = blendInfo,
    dynamicState = dynamicState,
    layout = gPipelineLayout,
    renderPass = renderPass,
    subpass = subpass)
    return device.createGraphicsPipeline (pipelineCache, info)
}

/** ~ImGui_ImplVulkan_CreateShaderModules */
fun createShaderModules(device: Device) = Stack { s ->
    // Create the shader modules
    if (gShaderModuleVert.isInvalid) {
        val vertInfo = ShaderModuleCreateInfo(code = __glsl_shader_vert_spv.toBuffer(s))
        gShaderModuleVert = device createShaderModule vertInfo
    }
    if (gShaderModuleFrag.isInvalid) {
        val fragInfo = ShaderModuleCreateInfo(code = __glsl_shader_frag_spv.toBuffer(s))
        gShaderModuleFrag = device createShaderModule fragInfo
    }
}

/** ~ImGui_ImplVulkan_CreatePipelineLayout */
fun createPipelineLayout(device: Device) {

    if (gPipelineLayout.isInvalid)
        return

    // Constants: we are using 'vec2 offset' and 'vec2 scale' instead of a full 3d projection matrix
    createDescriptorSetLayout(device)
    val pushConstants = PushConstantRange(
    stageFlags = VkShaderStage.VERTEX_BIT.i,
    offset = Float.BYTES * 0,
    size = Float.BYTES * 4)
    val setLayout = gDescriptorSetLayout
    val layoutInfo = PipelineLayoutCreateInfo(setLayout, pushConstants)
    gPipelineLayout = device createPipelineLayout layoutInfo
}

/** ~ImGui_ImplVulkan_CreateDescriptorSetLayout */
fun createDescriptorSetLayout(device: Device) {

    if (gDescriptorSetLayout.isInvalid)
        return

    createFontSampler(device)
    val sampler = gFontSampler
    val binding = DescriptorSetLayoutBinding(
    descriptorType = VkDescriptorType.COMBINED_IMAGE_SAMPLER,
    descriptorCount = 1,
    stageFlags = VkShaderStage.FRAGMENT_BIT.i,
    immutableSampler = sampler)
    val info = DescriptorSetLayoutCreateInfo(binding = binding)
    gDescriptorSetLayout = device createDescriptorSetLayout info
}

/** ImGui_ImplVulkan_CreateFontSampler */
fun createFontSampler(device: Device) {

    if (gFontSampler.isInvalid)
        return

    val info = SamplerCreateInfo(
        magMinFilter = VkFilter.LINEAR,
        mipmapMode = VkSamplerMipmapMode.LINEAR,
        addressModeUVW = VkSamplerAddressMode.REPEAT,
        minLod = -1000f,
        maxLod = 1000f,
        maxAnisotropy = 1f)
    gFontSampler = device createSampler info
}

//-----------------------------------------------------------------------------
// FUNCTIONS
//-----------------------------------------------------------------------------

/** ~ImGui_ImplVulkan_MemoryType */
fun memoryType(properties: VkMemoryPropertyFlags, typeBits: Int): Int {
    val v = gVulkanInitInfo
    val prop = v.physicalDevice.memoryProperties
    // Unable to find memoryType [JVM, automatically handled with the -1]
    for (i in prop.memoryTypes.indices)
        if ((prop.memoryTypes[i].propertyFlags and properties) == properties && typeBits has (1 shl i))
            return i
    return -1 // Unable to find memoryType
}

fun createOrResizeBuffer(pBuffer: KMutableProperty0<VkBuffer>, pBufferMemory: KMutableProperty0<VkDeviceMemory>,
                         pBufferSize: KMutableProperty0<VkDeviceSize>, newSize: VkDeviceSize, usage: VkBufferUsage) {

    var buffer by pBuffer
    var bufferMemory by pBufferMemory
    var bufferSize by pBufferSize

    val v = gVulkanInitInfo
    if (buffer.isValid)
        v.device destroy buffer
    if (bufferMemory.isValid)
        v.device freeMemory bufferMemory

    val vertexBufferSizeAligned = ((newSize - 1) / gBufferMemoryAlignment + 1) * gBufferMemoryAlignment
    val bufferInfo = BufferCreateInfo(
        size = vertexBufferSizeAligned,
        usageFlags = usage.i,
        sharingMode = VkSharingMode.EXCLUSIVE)
    buffer = v.device createBuffer bufferInfo

    val req = v.device getBufferMemoryRequirements buffer
    gBufferMemoryAlignment = if(gBufferMemoryAlignment > req.alignment) gBufferMemoryAlignment else req.alignment
    val allocInfo = MemoryAllocateInfo(
        allocationSize = req.size,
        memoryTypeIndex = memoryType(VkMemoryProperty.HOST_VISIBLE_BIT.i, req.memoryTypeBits))
    bufferMemory = v.device allocateMemory allocInfo

    v.device.bindBufferMemory(buffer, bufferMemory)
    bufferSize = newSize
}

/** ~ImGui_ImplVulkan_SetupRenderState */
fun setupRenderState(drawData: DrawData, pipeline: VkPipeline, commandBuffer: CommandBuffer, rb: ImplVulkanH.FrameRenderBuffers, fbSize: Vec2i) {
    // Bind pipeline and descriptor sets:
    commandBuffer.apply {
        bindPipeline(VkPipelineBindPoint.GRAPHICS, pipeline)
        bindDescriptorSets(VkPipelineBindPoint.GRAPHICS, gPipelineLayout, 0, gDescriptorSet)
    }

    // Bind Vertex And Index Buffer:
    if (drawData.totalVtxCount > 0) {
        commandBuffer.bindVertexBuffers(0, 1, rb.vertexBuffer, offset = VkDeviceSize(0))
        commandBuffer.bindIndexBuffer(rb.indexBuffer, VkDeviceSize(0), if(DrawIdx.BYTES == 2) VkIndexType.UINT16 else VkIndexType.UINT32)
    }

    // Setup viewport:
    commandBuffer setViewport Viewport(0f, 0f, fbSize.x.f, fbSize.y.f)

    // Setup scale and translation:
    // Our visible imgui space lies from draw_data->DisplayPps (top left) to draw_data->DisplayPos+data_data->DisplaySize (bottom right). DisplayPos is (0,0) for single viewport apps.
    Stack {
        val scale = it.mFloat(Vec2.length)
        scale[0] = 2f / drawData.displaySize.x
        scale[1] = 2f / drawData.displaySize.y
        commandBuffer.pushConstants(gPipelineLayout, VkShaderStage.VERTEX_BIT.i, Float.BYTES * 0, Vec2.size, scale.adr)
        val translate = it.mFloat(Vec2.length)
        translate[0] = -1f - drawData.displayPos.x * scale[0]
        translate[1] = -1f - drawData.displayPos.y * scale[1]
        commandBuffer.pushConstants(gPipelineLayout, VkShaderStage.VERTEX_BIT.i, Float.BYTES * 2, Vec2.size, translate.adr)
    }
}