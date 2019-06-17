package imgui.impl

import gli_.has
import gli_.memCopy
import glm_.BYTES
import glm_.L
import glm_.f
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4ub
import imgui.*
import imgui.ImGui.io
import kool.adr
import kool.isValid
import kool.rem
import kool.set
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.MemoryUtil.memCopy
import org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED
import org.lwjgl.vulkan.VkCommandBuffer
import uno.kotlin.getValue
import uno.kotlin.setValue
import vkk.*
import vkk.entities.VkBuffer
import vkk.entities.VkDeviceMemory
import vkk.entities.VkDeviceSize
import vkk.entities.VkRenderPass
import vkk.extensionFunctions.*
import kotlin.reflect.KMutableProperty0

fun ImplVk_Init(renderPass_: VkRenderPass): Boolean {

    io.backendRendererName = "imgui_impl_vulkan"
    io.backendFlags = io.backendFlags or BackendFlag.HasVtxOffset // We can honor the ImDrawCmd::VtxOffset field, allowing for large meshes.
    val info = VkInitInfo

    info.run { assert(queue.isValid && descriptorPool.isValid && minImageCount >= 2 && imageCount >= minImageCount) }
    assert(renderPass_.isValid)

    renderPass = renderPass_

    ImplVk_CreateDeviceObjects()

    return true
}
fun ImplVk_Shutdown() = ImplVk_DestroyDeviceObjects()
fun ImplVk_NewFrame() = Unit

/** Render function
 *  (this used to be set in io.RenderDrawListsFn and called by ImGui::Render(), but you can now call this directly from your main loop) */
fun renderDrawData(drawData: DrawData, commandBuffer: VkCommandBuffer) {

    // Avoid rendering when minimized, scale coordinates for retina displays (screen coordinates != framebuffer coordinates)
    val fbWidth = (drawData.displaySize.x * drawData.framebufferScale.x).i
    val fbHeight = (drawData.displaySize.y * drawData.framebufferScale.y).i
    if (fbWidth <= 0 || fbHeight <= 0 || drawData.totalVtxCount == 0)
        return

    val v = VkInitInfo

    // Allocate array to store enough vertex/index buffers
    val wrb = ImplVkH_WindowRenderBuffers
    if (wrb.frameRenderBuffers.isEmpty()) {
        wrb.index = 0
        wrb.frameRenderBuffers = Array(v.imageCount) { ImplVk_FrameRenderBuffers() }
    }
    assert(wrb.frameRenderBuffers.size == v.imageCount)
    wrb.index = (wrb.index + 1) % wrb.frameRenderBuffers.size
    val rb = wrb.frameRenderBuffers[wrb.index]

    // Create or resize the vertex/index buffers
    val vertexSize = VkDeviceSize(drawData.totalVtxCount * DrawVert.size)
    val indexSize = VkDeviceSize(drawData.totalIdxCount * DrawIdx.BYTES)
    if (rb.vertexBuffer.isInvalid || rb.vertexBufferSize < vertexSize)
        createOrResizeBuffer(rb::vertexBuffer, rb::vertexBufferMemory, rb::vertexBufferSize, vertexSize, VkBufferUsage.VERTEX_BUFFER_BIT)
    if (rb.indexBuffer.isInvalid || rb.indexBufferSize < indexSize)
        createOrResizeBuffer(rb::indexBuffer, rb::indexBufferMemory, rb::indexBufferSize, indexSize, VkBufferUsage.INDEX_BUFFER_BIT)

    // Upload vertex/index data into a single contiguous GPU buffer
    stak { s ->
        val vtxDst = s.pointers(NULL)
        val idxDst = s.pointers(NULL)
        v.device.mapMemory(rb.vertexBufferMemory, VkDeviceSize(0), vertexSize, 0, vtxDst)
        v.device.mapMemory(rb.indexBufferMemory, VkDeviceSize(0), indexSize, 0, idxDst)
        for (cmdList in drawData.cmdLists) {
            memCopy(cmdList.vtxBuffer.data.adr, vtxDst[0], cmdList.vtxBuffer.data.rem.L)
            memCopy(cmdList.idxBuffer.adr, idxDst[0], cmdList.idxBuffer.rem * DrawIdx.BYTES)
            vtxDst[0] += cmdList.vtxBuffer.data.rem.L
            idxDst[0] += cmdList.idxBuffer.rem * DrawIdx.BYTES.L
        }
        val range = vk.MappedMemoryRange(2).also {
            it[0].apply {
                memory = rb.vertexBufferMemory
                size = VK_WHOLE_SIZE
            }
            it[1].apply {
                memory = rb.indexBufferMemory
                size = VK_WHOLE_SIZE
            }
        }
        v.device.apply {
            flushMappedMemoryRanges(range)
            unmapMemory(rb.vertexBufferMemory)
            unmapMemory(rb.indexBufferMemory)
        }
    }

    // Setup desired Vulkan state
    setupRenderState(drawData, commandBuffer, rb, fbWidth, fbHeight)

    // Will project scissor/clipping rectangles into framebuffer space
    val clipOff = drawData.displayPos         // (0,0) unless using multi-viewports
    val clipScale = drawData.framebufferScale // (1,1) unless using retina display which are often (2,2)

    // Render command lists
    // (Because we merged all buffers into a single one, we maintain our own offset into them)
    var globalVtxOffset = 0
    var globalIdxOffset = 0
    for (cmdList in drawData.cmdLists) {
        for (cmd in cmdList.cmdBuffer) {
            val userCB = cmd.userCallback
            if (userCB != null) {
                // User callback, registered via ImDrawList::AddCallback()
                // (ImDrawCallback_ResetRenderState is a special callback value used by the user to request the renderer to reset render state.)
                if (cmd.resetRenderState)
                    setupRenderState(drawData, commandBuffer, rb, fbWidth, fbHeight)
                else
                    userCB(cmdList, cmd)
            } else {
                // Project scissor/clipping rectangles into framebuffer space
                var clipRectX = (cmd.clipRect.x - clipOff.x) * clipScale.x
                var clipRectY = (cmd.clipRect.y - clipOff.y) * clipScale.y
                val clipRectZ = (cmd.clipRect.z - clipOff.x) * clipScale.x
                val clipRectW = (cmd.clipRect.w - clipOff.y) * clipScale.y

                if (clipRectX < fbWidth && clipRectY < fbHeight && clipRectZ >= 0f && clipRectW >= 0f) {
                    // Negative offsets are illegal for vkCmdSetScissor
                    if (clipRectX < 0f)
                        clipRectX = 0f
                    if (clipRectY < 0f)
                        clipRectY = 0f

                    // Apply scissor/clipping rectangle
                    val scissor = vk.Rect2D(clipRectX.i, clipRectY.i, (clipRectZ - clipRectX).i, (clipRectW - clipRectY).i)
                    commandBuffer.setScissor(scissor)

                    // Draw
                    commandBuffer.drawIndexed(cmd.elemCount, 1, cmd.idxOffset + globalIdxOffset, cmd.vtxOffset + globalVtxOffset, 0)
                }
            }
            globalIdxOffset += cmdList.idxBuffer.rem
            globalVtxOffset += cmdList.vtxBuffer.rem
        }
    }
}

fun createOrResizeBuffer(pBuffer: KMutableProperty0<VkBuffer>, pBufferMemory: KMutableProperty0<VkDeviceMemory>,
                         pBufferSize: KMutableProperty0<VkDeviceSize>, newSize: VkDeviceSize, usage: VkBufferUsage) {

    var buffer by pBuffer
    var bufferMemory by pBufferMemory
    var bufferSize by pBufferSize

    val v = VkInitInfo
    if (buffer.isValid)
        v.device destroy buffer
    if (bufferMemory.isValid)
        v.device free bufferMemory

    val vertexBufferSizeAligned = ((newSize - 1) / bufferMemoryAlignment + 1) * bufferMemoryAlignment
    val bufferInfo = vk.BufferCreateInfo {
        size = vertexBufferSizeAligned
        this.usage = usage.i
        sharingMode = VkSharingMode.EXCLUSIVE
    }
    buffer = v.device createBuffer bufferInfo

    val req = v.device.getBufferMemoryRequirements(buffer)
    bufferMemoryAlignment = if (bufferMemoryAlignment > req.alignment) bufferMemoryAlignment else req.alignment
    val allocInfo = vk.MemoryAllocateInfo {
        allocationSize = req.size
        memoryTypeIndex = ImplVk_MemoryType(VkMemoryProperty.HOST_VISIBLE_BIT.i, req.memoryTypeBits)
    }
    bufferMemory = v.device allocateMemory allocInfo

    v.device.bindBufferMemory(buffer, bufferMemory)
    bufferSize = newSize
}

fun ImplVk_MemoryType(properties: VkMemoryPropertyFlags, typeBits: Int): Int {
    val v = VkInitInfo
    val prop = v.physicalDevice.memoryProperties
    for (i in 0 until prop.memoryTypeCount)
        if ((prop.memoryTypes[i].propertyFlags and properties) == properties && typeBits has (1 shl i))
            return i
    return -1 // Unable to find memoryType
}

fun setupRenderState(drawData: DrawData, commandBuffer: VkCommandBuffer, rb: ImplVk_FrameRenderBuffers, fbWidth: Int, fbHeight: Int) {
    // Bind pipeline and descriptor sets:
    commandBuffer.apply {
        bindPipeline(VkPipelineBindPoint.GRAPHICS, pipeline)
        bindDescriptorSets(VkPipelineBindPoint.GRAPHICS, pipelineLayout, descriptorSet)
    }

    // Bind Vertex And Index Buffer:
    commandBuffer.apply {
        val offset = VkDeviceSize(0)
        bindVertexBuffers(0, rb.vertexBuffer, offset)
        bindIndexBuffer(rb.indexBuffer, offset, VkIndexType.UINT32)
    }

    // Setup viewport:
    commandBuffer.setViewport(fbWidth.f, fbHeight.f)

    // Setup scale and translation:
    // Our visible imgui space lies from draw_data->DisplayPos (top left) to draw_data->DisplayPos+data_data->DisplaySize (bottom right). DisplayMin is typically (0,0) for single viewport apps.
    stak {
        val scale = it.floats(2f / drawData.displaySize.x, 2f / drawData.displaySize.y)
        val translate = it.floats(-1f - drawData.displayPos.x * scale[0], -1f - drawData.displayPos.y * scale[1])
        commandBuffer.pushConstants(pipelineLayout, VkShaderStage.VERTEX_BIT.i, 0, scale)
        commandBuffer.pushConstants(pipelineLayout, VkShaderStage.VERTEX_BIT.i, Vec2.size, translate)
    }
}

fun ImplVk_CreateFontsTexture(commandBuffer: VkCommandBuffer): Boolean {

    val v = VkInitInfo

    val (pixels, size, _) = io.fonts.getTexDataAsRGBA32()
    val uploadSize = VkDeviceSize(size.x * size.y * Vec4ub.size)

    // Create the Image:
    run {
        val info = vk.ImageCreateInfo {
            imageType = VkImageType._2D
            format = VkFormat.R8G8B8A8_UNORM
            extent(size, 1)
            mipLevels = 1
            arrayLayers = 1
            samples = VkSampleCount._1_BIT
            tiling = VkImageTiling.OPTIMAL
            usage = VkImageUsage.SAMPLED_BIT or VkImageUsage.TRANSFER_DST_BIT
            sharingMode = VkSharingMode.EXCLUSIVE
            initialLayout = VkImageLayout.UNDEFINED
        }
        fontImage = v.device createImage info
        val req = v.device.getImageMemoryRequirements(fontImage)
        val allocInfo = vk.MemoryAllocateInfo {
            allocationSize = req.size
            memoryTypeIndex = ImplVk_MemoryType(VkMemoryProperty.DEVICE_LOCAL_BIT.i, req.memoryTypeBits)
        }
        fontMemory = v.device allocateMemory allocInfo
        v.device.bindImageMemory(fontImage, fontMemory)
    }

    // Create the Image View:
    run {
        val info = vk.ImageViewCreateInfo {
            image = fontImage
            viewType = VkImageViewType._2D
            format = VkFormat.R8G8B8A8_UNORM
            subresourceRange.aspectMask = VkImageAspect.COLOR_BIT.i
            subresourceRange.levelCount = 1
            subresourceRange.layerCount = 1
        }
        fontView = v.device createImageView info
    }

    // Update the Descriptor Set:
    run {
        val descImage = vk.DescriptorImageInfo(fontSampler, fontView, VkImageLayout.SHADER_READ_ONLY_OPTIMAL)
        val writeDesc = vk.WriteDescriptorSet {
            dstSet = descriptorSet
            descriptorCount = 1
            descriptorType = VkDescriptorType.COMBINED_IMAGE_SAMPLER
            imageInfo = descImage
        }
        v.device.updateDescriptorSets(writeDesc)
    }

    // Create the Upload Buffer:
    run {
        val bufferInfo = vk.BufferCreateInfo {
            this.size = uploadSize
            usage = VkBufferUsage.TRANSFER_SRC_BIT.i
            sharingMode = VkSharingMode.EXCLUSIVE
        }
        uploadBuffer = v.device createBuffer bufferInfo
        val req = v.device.getBufferMemoryRequirements(uploadBuffer)
        bufferMemoryAlignment = if (bufferMemoryAlignment > req.alignment) bufferMemoryAlignment else req.alignment
        val allocInfo = vk.MemoryAllocateInfo {
            allocationSize = req.size
            memoryTypeIndex = ImplVk_MemoryType(VkMemoryProperty.HOST_VISIBLE_BIT.i, req.memoryTypeBits)
        }
        uploadBufferMemory = v.device allocateMemory allocInfo
        v.device.bindBufferMemory(uploadBuffer, uploadBufferMemory)
    }

    // Upload to Buffer:
    v.device.mappedMemory(uploadBufferMemory, VkDeviceSize(0), uploadSize) { map ->
        memCopy(pixels.adr, map, uploadSize.L)
        val range = vk.MappedMemoryRange {
            memory = uploadBufferMemory
            this.size = uploadSize
        }
        v.device.flushMappedMemoryRanges(range)
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
            subresourceRange.aspectMask = VkImageAspect.COLOR_BIT.i
            subresourceRange.levelCount = 1
            subresourceRange.layerCount = 1
        }
        commandBuffer.pipelineBarrier(VkPipelineStage.HOST_BIT, VkPipelineStage.TRANSFER_BIT, imageMemoryBarrier = copyBarrier)

        val region = vk.BufferImageCopy {
            imageSubresource.aspectMask = VkImageAspect.COLOR_BIT.i
            imageSubresource.layerCount = 1
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
            subresourceRange.aspectMask = VkImageAspect.COLOR_BIT.i
            subresourceRange.levelCount = 1
            subresourceRange.layerCount = 1
        }
        commandBuffer.pipelineBarrier(VkPipelineStage.TRANSFER_BIT, VkPipelineStage.FRAGMENT_SHADER_BIT, imageMemoryBarrier = useBarrier)
    }

    // Store our identifier
    io.fonts.texId = fontImage.L.i

    return true
}

fun ImplVk_DestroyFontUploadObjects() {

    val v = VkInitInfo
    if (uploadBuffer.isValid)    {
        v.device destroy uploadBuffer
        uploadBuffer = VkBuffer.NULL
    }
    if (uploadBufferMemory.isValid)    {
        v.device free uploadBufferMemory
        uploadBufferMemory = VkDeviceMemory.NULL
    }
}

/** To override MinImageCount after initialization (e.g. if swap chain is recreated) */
fun ImplVk_SetMinImageCount(minImageCount: Int) {

    assert(minImageCount >= 2)
    val v = VkInitInfo
    if (v.minImageCount == minImageCount)
        return

    v.device.waitIdle()
    ImplVkH_DestroyWindowRenderBuffers(v.device)
    VkInitInfo.minImageCount = minImageCount
}