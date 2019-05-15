package imgui

import glm_.vec2.Vec2i
import imgui.impl.*
import kool.indices
import kool.rem
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.VK_FALSE
import vkk.*
import vkk.entities.*
import vkk.extensionFunctions.*


var UNLIMITED_FRAME_RATE = true


lateinit var instance: VkInstance
lateinit var physicalDevice: VkPhysicalDevice
lateinit var device: VkDevice
var queueFamily = -1
lateinit var queue: VkQueue
var debugReport = VkDebugReportCallback()
var pipelineCache = VkPipelineCache()
var descriptorPool = VkDescriptorPool()

//var ImplVulkanH_Window g_MainWindowData; useless, we can use an object for that
var minImageCount = 2
var swapChainRebuild = false
var swapChainResizeSize = Vec2i()


val debugCallback = VkDebugReportCallbackEXT.create { flags, objectType, `object`, location, messageCode, pLayerPrefix, pMessage, pUserData ->
    System.err.println("[vulkan] ObjectType: $objectType\nMessage: ${memUTF8(pMessage)}")
    VK_FALSE
}

fun setupVulkan(extensions: ArrayList<String>) {

    // Create Vulkan Instance
    run {

        val createInfo = vk.InstanceCreateInfo {
            if (DEBUG) {
                // Enabling multiple validation layers grouped as LunarG standard validation
                enabledLayerNames = listOf("VK_LAYER_LUNARG_standard_validation")
                // Enable debug report extension (we need additional storage, so we duplicate the user array to add our new extension to it)
                extensions += "VK_EXT_debug_report"
            }
            enabledExtensionNames = extensions
        }
        // Create Vulkan Instance
        instance = vk createInstance createInfo

        if (DEBUG) {
            // Setup the debug report callback
            val debugReportCi = vk.DebugReportCallbackCreateInfoEXT {
                flags = VkDebugReport.ERROR_BIT_EXT or VkDebugReport.WARNING_BIT_EXT or VkDebugReport.PERFORMANCE_WARNING_BIT_EXT
                callback = debugCallback
            }
            debugReport = instance createDebugReportCallbackEXT debugReportCi
        }
    }

    // Select GPU
    run {
        val gpus = instance.enumeratePhysicalDevices

        /*  If a number >1 of GPUs got reported, you should find the best fit GPU for your purpose
            e.g. VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU if available, or with the greatest memory available, etc.
            for sake of simplicity we'll just take the first one, assuming it has a graphics queue family.         */
        physicalDevice = gpus[0]
    }

    // Select graphics queue family
    run {
        val queues = physicalDevice.queueFamilyProperties
        for (i in queues.indices)
            if (queues[i].queueFlags has VkQueueFlag.GRAPHICS_BIT) {
                queueFamily = i
                break
            }
        assert(queueFamily != -1)
    }

    // Create Logical Device (with 1 queue)
    run {
        val deviceExtensions = listOf("VK_KHR_swapchain")
        val queueInfo = vk.DeviceQueueCreateInfo {
            queueFamilyIndex = queueFamily
            queuePriority = 1f
        }
        val createInfo = vk.DeviceCreateInfo {
            queueCreateInfo = queueInfo
            enabledExtensionNames = deviceExtensions
        }
        device = physicalDevice createDevice createInfo
        queue = device.getQueue(queueFamily)
    }

    // Create Descriptor Pool
    run {
        val poolSizes = vk.DescriptorPoolSize(
                VkDescriptorType.SAMPLER, 1000,
                VkDescriptorType.COMBINED_IMAGE_SAMPLER, 1000,
                VkDescriptorType.SAMPLED_IMAGE, 1000,
                VkDescriptorType.STORAGE_IMAGE, 1000,
                VkDescriptorType.UNIFORM_TEXEL_BUFFER, 1000,
                VkDescriptorType.STORAGE_TEXEL_BUFFER, 1000,
                VkDescriptorType.UNIFORM_BUFFER, 1000,
                VkDescriptorType.STORAGE_BUFFER, 1000,
                VkDescriptorType.UNIFORM_BUFFER_DYNAMIC, 1000,
                VkDescriptorType.STORAGE_BUFFER_DYNAMIC, 1000,
                VkDescriptorType.INPUT_ATTACHMENT, 1000)
        val poolInfo = vk.DescriptorPoolCreateInfo {
            flags = VkDescriptorPoolCreate.FREE_DESCRIPTOR_SET_BIT.i
            maxSets = 1000 * poolSizes.rem
            this.poolSizes = poolSizes
        }
        descriptorPool = device createDescriptorPool poolInfo
    }
}

/** All the ImGui_ImplVulkanH_XXX structures/functions are optional helpers used by the demo.
 *  Your real engine/app may not use them. */
fun setupVulkanWindow(surface: VkSurfaceKHR, size: Vec2i) {

    wd.surface = surface

    // Check for WSI support
    val res = physicalDevice.getSurfaceSupportKHR(queueFamily, wd.surface)
    if (!res)
        error("Error no WSI support on physical device 0")

    // Select Surface Format
    val requestSurfaceImageFormat = listOf(VkFormat.B8G8R8A8_UNORM, VkFormat.R8G8B8A8_UNORM, VkFormat.B8G8R8_UNORM, VkFormat.R8G8B8_UNORM)
    val requestSurfaceColorSpace = VkColorSpaceKHR.SRGB_NONLINEAR_KHR
    wd.surfaceFormat = selectSurfaceFormat(physicalDevice, wd.surface, requestSurfaceImageFormat, requestSurfaceColorSpace)

    // Select Present Mode
    val presentModes = when {
        UNLIMITED_FRAME_RATE -> listOf(VkPresentModeKHR.MAILBOX_KHR, VkPresentModeKHR.IMMEDIATE_KHR, VkPresentModeKHR.FIFO_KHR)
        else -> listOf(VkPresentModeKHR.FIFO_KHR)
    }
    wd.presentMode = selectPresentMode(physicalDevice, wd.surface, presentModes)
    //printf("[vulkan] Selected PresentMode = %d\n", wd->PresentMode);

    // Create SwapChain, RenderPass, Framebuffer, etc.
    assert(minImageCount >= 2)
    createWindow(physicalDevice, device, queueFamily, size, minImageCount)
}

fun cleanupVulkan() {

    device destroy descriptorPool

    if (DEBUG)   // Remove the debug report callback
        instance destroy debugReport

    device.destroy()
    instance.destroy()
}

fun cleanupVulkanWindow() = destroyWindow(instance, device)

fun frameRender() {

    val imageAcquiredSemaphore = wd.frameSemaphores[wd.semaphoreIndex].imageAcquiredSemaphore
    val renderCompleteSemaphore = wd.frameSemaphores[wd.semaphoreIndex].renderCompleteSemaphore
    wd.frameIndex = device.acquireNextImageKHR(wd.swapchain, Long.MAX_VALUE, imageAcquiredSemaphore, VkFence())

    val fd = wd.frames[wd.frameIndex]
    run {
        device.waitForFences(fd.fence, true, Long.MAX_VALUE)    // wait indefinitely instead of periodically checking

        device.resetFences(fd.fence)
    }
    run {
        device.resetCommandPool(fd.commandPool)
        val info = vk.CommandBufferBeginInfo { flags = VkCommandBufferUsage.ONE_TIME_SUBMIT_BIT.i }
        fd.commandBuffer!! begin info
    }
    run {
        val info = vk.RenderPassBeginInfo {
            renderPass = wd.renderPass
            framebuffer = fd.framebuffer
            renderArea.extent(wd.size)
            clearValue = wd.clearValue
        }
        fd.commandBuffer!!.beginRenderPass(info, VkSubpassContents.INLINE)
    }

    // Record Imgui Draw Data and draw funcs into command buffer
    ImGui_ImplVulkan_RenderDrawData(ImGui::GetDrawData(), fd->CommandBuffer)

    // Submit command buffer
    vkCmdEndRenderPass(fd->CommandBuffer);
    {
        VkPipelineStageFlags wait_stage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                VkSubmitInfo info = {}
        info.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO
        info.waitSemaphoreCount = 1
        info.pWaitSemaphores = & image_acquired_semaphore
                info.pWaitDstStageMask = & wait_stage
                info.commandBufferCount = 1
        info.pCommandBuffers = & fd->CommandBuffer
        info.signalSemaphoreCount = 1
        info.pSignalSemaphores = & render_complete_semaphore

                err = vkEndCommandBuffer(fd->CommandBuffer)
        check_vk_result(err)
        err = vkQueueSubmit(g_Queue, 1, & info, fd->Fence)
        check_vk_result(err)
    }
}