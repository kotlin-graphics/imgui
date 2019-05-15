package imgui.impl

import glm_.BYTES
import glm_.i
import imgui.DrawData
import imgui.DrawIdx
import imgui.DrawVert
import org.lwjgl.vulkan.VkCommandBuffer
import uno.kotlin.getValue
import uno.kotlin.setValue
import vkk.*
import vkk.entities.VkBuffer
import vkk.entities.VkDeviceMemory
import vkk.entities.VkDeviceSize
import vkk.extensionFunctions.*
import kotlin.reflect.KMutableProperty0

//IMGUI_IMPL_API bool     ImGui_ImplVulkan_Init(ImGui_ImplVulkan_InitInfo* info, VkRenderPass render_pass);
//IMGUI_IMPL_API void     ImGui_ImplVulkan_Shutdown();
//IMGUI_IMPL_API void     ImGui_ImplVulkan_NewFrame();

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
    val wrb = MainWindowRenderBuffers
    if (wrb.frameRenderBuffers.isEmpty()) {
        wrb.index = 0
        wrb.count = v.imageCount
        wrb.frameRenderBuffers = Array(wrb.count) { VkFrameRenderBuffers() }
    }
    assert(wrb.count == v.imageCount)
    wrb.index = (wrb.index + 1).rem(wrb.count)
    val rb = wrb.frameRenderBuffers[wrb.index]

    // Create or resize the vertex/index buffers
    val vertexSize = VkDeviceSize(drawData.totalVtxCount * DrawVert.size)
    val indexSize = VkDeviceSize(drawData.totalIdxCount * DrawIdx.BYTES)
    if (rb.vertexBuffer.isInvalid || rb.vertexBufferSize < vertexSize)
        createOrResizeBuffer(rb::vertexBuffer, rb::vertexBufferMemory, rb::vertexBufferSize, vertexSize, VkBufferUsage.VERTEX_BUFFER_BIT)
    if (rb.indexBuffer.isInvalid || rb.indexBufferSize < indexSize)
        createOrResizeBuffer(rb::indexBuffer, rb::indexBufferMemory, rb::indexBufferSize, indexSize, VkBufferUsage.INDEX_BUFFER_BIT)

    // Upload vertex/index data into a single contiguous GPU buffer
    run {
        ImDrawVert * vtx_dst = NULL
        ImDrawIdx * idx_dst = NULL
        err = vkMapMemory(v->Device, rb->VertexBufferMemory, 0, vertex_size, 0, (void**)(&vtx_dst))
        check_vk_result(err)
        err = vkMapMemory(v->Device, rb->IndexBufferMemory, 0, index_size, 0, (void**)(&idx_dst))
        check_vk_result(err)
        for (int n = 0; n < draw_data->CmdListsCount; n++)
        {
            const ImDrawList * cmd_list = draw_data->CmdLists[n]
            memcpy(vtx_dst, cmd_list->VtxBuffer.Data, cmd_list->VtxBuffer.Size * sizeof(ImDrawVert))
            memcpy(idx_dst, cmd_list->IdxBuffer.Data, cmd_list->IdxBuffer.Size * sizeof(ImDrawIdx))
            vtx_dst += cmd_list->VtxBuffer.Size
            idx_dst += cmd_list->IdxBuffer.Size
        }
        VkMappedMemoryRange range [2] = {}
        range[0].sType = VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE
        range[0].memory = rb->VertexBufferMemory
        range[0].size = VK_WHOLE_SIZE
        range[1].sType = VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE
        range[1].memory = rb->IndexBufferMemory
        range[1].size = VK_WHOLE_SIZE
        err = vkFlushMappedMemoryRanges(v->Device, 2, range)
        check_vk_result(err)
        vkUnmapMemory(v->Device, rb->VertexBufferMemory)
        vkUnmapMemory(v->Device, rb->IndexBufferMemory)
    }

    // Setup desired Vulkan state
    ImGui_ImplVulkan_SetupRenderState(draw_data, command_buffer, rb, fbWidth, fbHeight)

    // Will project scissor/clipping rectangles into framebuffer space
    ImVec2 clip_off = draw_data->DisplayPos         // (0,0) unless using multi-viewports
    ImVec2 clip_scale = draw_data->FramebufferScale // (1,1) unless using retina display which are often (2,2)

    // Render command lists
    int vtx_offset = 0
    int idx_offset = 0
    for (int n = 0; n < draw_data->CmdListsCount; n++)
    {
        const ImDrawList * cmd_list = draw_data->CmdLists[n]
        for (int cmd_i = 0; cmd_i < cmd_list->CmdBuffer.Size; cmd_i++)
        {
            const ImDrawCmd * pcmd = &cmd_list->CmdBuffer[cmd_i]
            if (pcmd->UserCallback != NULL)
            {
                // User callback, registered via ImDrawList::AddCallback()
                // (ImDrawCallback_ResetRenderState is a special callback value used by the user to request the renderer to reset render state.)
                if (pcmd->UserCallback == ImDrawCallback_ResetRenderState)
                ImGui_ImplVulkan_SetupRenderState(draw_data, command_buffer, rb, fbWidth, fbHeight)
                else
                pcmd->UserCallback(cmd_list, pcmd)
            }
            else
            {
                // Project scissor/clipping rectangles into framebuffer space
                ImVec4 clip_rect
                        clip_rect.x = (pcmd->ClipRect.x-clip_off.x) * clip_scale.x
                clip_rect.y = (pcmd->ClipRect.y-clip_off.y) * clip_scale.y
                clip_rect.z = (pcmd->ClipRect.z-clip_off.x) * clip_scale.x
                clip_rect.w = (pcmd->ClipRect.w-clip_off.y) * clip_scale.y

                if (clip_rect.x < fbWidth && clip_rect.y < fbHeight && clip_rect.z >= 0.0f && clip_rect.w >= 0.0f) {
                    // Negative offsets are illegal for vkCmdSetScissor
                    if (clip_rect.x < 0.0f)
                        clip_rect.x = 0.0f
                    if (clip_rect.y < 0.0f)
                        clip_rect.y = 0.0f

                    // Apply scissor/clipping rectangle
                    VkRect2D scissor
                            scissor.offset.x = (int32_t)(clip_rect.x)
                    scissor.offset.y = (int32_t)(clip_rect.y)
                    scissor.extent.width = (uint32_t)(clip_rect.z - clip_rect.x)
                    scissor.extent.height = (uint32_t)(clip_rect.w - clip_rect.y)
                    vkCmdSetScissor(command_buffer, 0, 1, & scissor)

                    // Draw
                    vkCmdDrawIndexed(command_buffer, pcmd->ElemCount, 1, idx_offset, vtx_offset, 0)
                }
            }
            idx_offset += pcmd->ElemCount
        }
        vtx_offset += cmd_list->VtxBuffer.Size
    }
}

fun createOrResizeBuffer(pBuffer: KMutableProperty0<VkBuffer>, pBufferMemory: KMutableProperty0<VkDeviceMemory>, pBufferSize: KMutableProperty0<VkDeviceSize>,
                         newSize: VkDeviceSize, usage: VkBufferUsage) {

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
        memoryTypeIndex = vkMemoryType(VkMemoryProperty.HOST_VISIBLE_BIT.i, req.memoryTypeBits)
    }
    bufferMemory = v.device allocateMemory allocInfo

    v.device.bindBufferMemory(buffer, bufferMemory)
    bufferSize = newSize
}

fun vkMemoryType(properties: VkMemoryPropertyFlags, typeBits: Int): Int {
    val v = VkInitInfo
    val prop = v.physicalDevice.memoryProperties
    for (i in 0 until prop.memoryTypeCount)
        if ((prop.memoryTypes[i].propertyFlags and properties) == properties && typeBits has (1 shl i))
            return i
    return -1 // Unable to find memoryType
}

//IMGUI_IMPL_API bool     ImGui_ImplVulkan_CreateFontsTexture(VkCommandBuffer command_buffer);
//IMGUI_IMPL_API void     ImGui_ImplVulkan_DestroyFontUploadObjects();
//IMGUI_IMPL_API void     ImGui_ImplVulkan_SetMinImageCount(uint32_t min_image_count); // To override MinImageCount after initialization (e.g. if swap chain is recreated)