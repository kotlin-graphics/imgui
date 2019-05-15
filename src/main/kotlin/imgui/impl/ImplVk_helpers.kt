package imgui.impl

import glm_.vec2.Vec2i
import kool.lib.toIntArray
import kool.rem
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkSurfaceFormatKHR
import vkk.*
import vkk.entities.VkSurfaceKHR
import vkk.extensionFunctions.*


fun createWindow(physicalDevice: VkPhysicalDevice, device: VkDevice, queueFamily: Int, size: Vec2i, minImageCount: Int) {
    createWindowSwapChain(physicalDevice, device, size, minImageCount)
    createWindowCommandBuffers(physicalDevice, device, queueFamily)
}

fun destroyWindow(instance: VkInstance , device: VkDevice) {

    device.waitIdle() // FIXME: We could wait on the Queue if we had the queue in wd-> (otherwise VulkanH functions can't use globals)
    //vkQueueWaitIdle(g_Queue);

    val wd = VkWindow

    repeat(wd.imageCount)    {
        destroyFrame(device, wd.frames[it])
        destroyFrameSemaphores(device, wd.frameSemaphores[it])
    }
    wd.frames = emptyArray()
    wd.frameSemaphores = emptyArray()
    device destroy wd.renderPass
    device destroy wd.swapchain
    instance destroySurfaceKHR wd.surface

    wd.reset()
}

fun selectSurfaceFormat(physicalDevice: VkPhysicalDevice, surface: VkSurfaceKHR, requestFormats: List<VkFormat>, requestColorSpace: VkColorSpaceKHR): VkSurfaceFormatKHR {

    assert(requestFormats.isNotEmpty())

    /*  Per Spec Format and View Format are expected to be the same unless VK_IMAGE_CREATE_MUTABLE_BIT was set at image creation
        Assuming that the default behavior is without setting this bit, there is no need for separate Swapchain image and image view format
        Additionally several new color spaces were introduced with Vulkan Spec v1.0.40,
        hence we must make sure that a format with the mostly available color space, VK_COLOR_SPACE_SRGB_NONLINEAR_KHR, is found and used.     */
    val availFormat: VkSurfaceFormatKHR.Buffer = physicalDevice getSurfaceFormatsKHR (surface)

    // First check if only one format, VK_FORMAT_UNDEFINED, is available, which would imply that any format is available
    return when {
        availFormat.rem == 1 -> when {
            availFormat[0].format == VkFormat.UNDEFINED -> VkSurfaceFormatKHR.calloc().apply {
                format = requestFormats[0]
                colorSpace = requestColorSpace
            }
            else -> availFormat[0] // No point in searching another format
        }
        // Request several formats, the first found will be used
        else -> availFormat.firstOrNull { it.format in requestFormats && it.colorSpace == requestColorSpace }
                ?: availFormat[0] // If none of the requested image formats could be found, use the first available
    }
}

fun selectPresentMode(physicalDevice: VkPhysicalDevice, surface: VkSurfaceKHR, requestModes: List<VkPresentModeKHR>): VkPresentModeKHR {

    assert(requestModes.isNotEmpty())

    // Request a certain mode and confirm that it is available. If not use VK_PRESENT_MODE_FIFO_KHR which is mandatory
    val availModes: VkPresentModeKHR_Buffer = physicalDevice.getSurfacePresentModesKHR(surface)
    //for (uint32_t avail_i = 0; avail_i < avail_count; avail_i++)
    //    printf("[vulkan] avail_modes[%d] = %d\n", avail_i, avail_modes[avail_i]);

    for (request in requestModes)
        for (avail in availModes.buffer.toIntArray()) // TODO IntBuffer iterator()
            if (request.i == avail)
                return request

    return VkPresentModeKHR.FIFO_KHR // Always available
}

/** ~getMinImageCountFromPresentMode */
val VkPresentModeKHR.minImageCount: Int
    get() = when (this){
    VkPresentModeKHR.MAILBOX_KHR -> 3
    VkPresentModeKHR.FIFO_KHR, VkPresentModeKHR.FIFO_RELAXED_KHR -> 2
    VkPresentModeKHR.IMMEDIATE_KHR -> 1
    else -> error("Invalid present mode")
}