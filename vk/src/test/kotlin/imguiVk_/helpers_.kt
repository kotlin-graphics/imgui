package imguiVk_

import glm_.has
import glm_.vec2.Vec2i
import imgui.impl.vk_.*
import imgui.internal.DrawData
import kool.Stack
import kool.rem
import kool.set
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugReport.*
import org.lwjgl.vulkan.EXTDebugUtils.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import kotlin.system.exitProcess

val debugReport = VkDebugReportCallbackEXT.create { _, objectType, _, _, _, _, pMessage, _ ->
    System.err.println("[vulkan] Debug report from ObjectType: $objectType\nMessage: ${MemoryUtil.memASCII(pMessage)}")
    VK_FALSE
}

var debugCallbackUtils = callback@ { severity: Int, type: Int, callbackDataPointer: Long, _: Long ->
    val dbg = if (type and EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT == EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT) {
        " (performance)"
    } else if(type and EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT == EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT) {
        " (validation)"
    } else {
        ""
    }

    val callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(callbackDataPointer)
    val obj = callbackData.pMessageIdNameString()
    val message = callbackData.pMessageString()
    val objectType = 0

    when (severity) {
        EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT ->
            println("!! $obj($objectType) Validation$dbg: $message")
        EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT ->
            println("!! $obj($objectType) Validation$dbg: $message")
        EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT ->
            println("!! $obj($objectType) Validation$dbg: $message")
        else -> println("!! $obj($objectType) Validation (unknown message type)$dbg: $message")
    }

    // return false here, otherwise the application would quit upon encountering a validation error.
    VK_FALSE
}

fun setupVulkan_(extensions: PointerBuffer) = Stack { s ->

    var err: Int

    // Create Vulkan Instance
    run {
        val createInfo = VkInstanceCreateInfo.callocStack(s)
                .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .ppEnabledExtensionNames(extensions)
        if (imgui.DEBUG) {
            System.err.println("DEBUG ENABLED, LOL")

            // Enabling validation layers
            val layers = s.callocPointer(1)
            layers[0] = MemoryUtil.memASCII("VK_LAYER_KHRONOS_validation")
            createInfo.ppEnabledLayerNames(layers)

            // Enable debug report extension (we need additional storage, so we duplicate the user array to add our new extension to it)
            val extensionsExt = s.callocPointer(extensions.rem + 1)
            MemoryUtil.memCopy(extensions, extensionsExt)
            extensionsExt[extensions.rem] = MemoryUtil.memASCII("VK_EXT_debug_utils")
            createInfo.ppEnabledExtensionNames(extensionsExt)

            // Create Vulkan Instance
            err = vkCreateInstance(createInfo, gAllocator, pP)
            gInstance = VkInstance(pP[0], createInfo)
            checkVkResult(err)

            // Setup the debug report callback
            val debugReportCi = VkDebugUtilsMessengerCreateInfoEXT.callocStack(s)
                    .sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                    .flags(0)
                    .pfnUserCallback(debugCallbackUtils)
                    .messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
                    .messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT)
//                    .pUserData(MemoryUtil.NULL)
            err = EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(gInstance, debugReportCi, gAllocator, pL)
            gDebugReport = pL[0]
            checkVkResult(err)
        } else {
            // Create Vulkan Instance without any debug feature
            err = vkCreateInstance(createInfo, gAllocator, pP)
            gInstance = VkInstance(pP[0], createInfo)
            checkVkResult(err)
        }
    }

    // Select GPU
    run {
        val gpuCount = s.callocInt(1)
        err = vkEnumeratePhysicalDevices(gInstance, gpuCount, null)
        checkVkResult(err)
        assert(gpuCount[0] > 0)

        val gpus = s.callocPointer(gpuCount[0])
        err = vkEnumeratePhysicalDevices(gInstance, gpuCount, gpus)
        checkVkResult(err)

        // If a number >1 of GPUs got reported, you should find the best fit GPU for your purpose
        // e.g. VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU if available, or with the greatest memory available, etc.
        // for sake of simplicity we'll just take the first one, assuming it has a graphics queue family.
        gPhysicalDevice = VkPhysicalDevice(gpus[0], gInstance)
    }

    // Select graphics queue family
    run {
        val count = s.mallocInt(1)
        vkGetPhysicalDeviceQueueFamilyProperties(gPhysicalDevice, count, null)
        val queues = VkQueueFamilyProperties.mallocStack(count[0], s)
        vkGetPhysicalDeviceQueueFamilyProperties(gPhysicalDevice, count, queues)
        for (i in 0 until count[0])
            if (queues[i].queueFlags() has VK_QUEUE_GRAPHICS_BIT) {
                gQueueFamily = i
                break
            }
        assert(gQueueFamily != -1)
    }

    // Create Logical Device (with 1 queue)
    run {
        val deviceExtensions = s.callocPointer(1)
        deviceExtensions[0] = MemoryUtil.memASCII("VK_KHR_swapchain")
        val layers = s.pointers(MemoryUtil.memUTF8("VK_LAYER_KHRONOS_validation"))
        val queuePriority = s.floats(1f)
        val queueInfo = VkDeviceQueueCreateInfo.callocStack(1, s)
                .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                .queueFamilyIndex(gQueueFamily)
                .pQueuePriorities(queuePriority)
        val createInfo = VkDeviceCreateInfo.callocStack(s)
                .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pQueueCreateInfos(queueInfo)
                .ppEnabledExtensionNames(deviceExtensions)
                .ppEnabledLayerNames(layers) // TODO check me
        err = vkCreateDevice(gPhysicalDevice, createInfo, gAllocator, pP)
        gDevice = VkDevice(pP[0], gPhysicalDevice, createInfo)
        checkVkResult(err)
        vkGetDeviceQueue(gDevice, gQueueFamily, 0, pP)
        gQueue = VkQueue(pP[0], gDevice)
    }

    // Create Descriptor Pool
    run {
        val poolSizes = VkDescriptorPoolSize.callocStack(11, s).also {
            it[0].type(VK_DESCRIPTOR_TYPE_SAMPLER).descriptorCount(1000)
            it[1].type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(1000)
            it[2].type(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE).descriptorCount(1000)
            it[3].type(VK_DESCRIPTOR_TYPE_STORAGE_IMAGE).descriptorCount(1000)
            it[4].type(VK_DESCRIPTOR_TYPE_UNIFORM_TEXEL_BUFFER).descriptorCount(1000)
            it[5].type(VK_DESCRIPTOR_TYPE_STORAGE_TEXEL_BUFFER).descriptorCount(1000)
            it[6].type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER).descriptorCount(1000)
            it[7].type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).descriptorCount(1000)
            it[8].type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC).descriptorCount(1000)
            it[9].type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC).descriptorCount(1000)
            it[10].type(VK_DESCRIPTOR_TYPE_INPUT_ATTACHMENT).descriptorCount(1000)
        }
        val poolInfo = VkDescriptorPoolCreateInfo.callocStack(s)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                .flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
                .maxSets(1000 * poolSizes.rem)
                .pPoolSizes(poolSizes)
        err = vkCreateDescriptorPool(gDevice, poolInfo, gAllocator, pL)
        gDescriptorPool = pL[0]
        checkVkResult(err)
    }
}

// All the ImGui_ImplVulkanH_XXX structures/functions are optional helpers used by the demo.
// Your real engine/app may not use them.
fun setupVulkanWindow_(wd: ImplVulkanH_.Window, surface: Long, size: Vec2i) = Stack { s ->

    wd.surface = surface

    // Check for WSI support
    vkGetPhysicalDeviceSurfaceSupportKHR(gPhysicalDevice, gQueueFamily, wd.surface, pI)
    val res = pI[0]
    if (res != VK_TRUE) {
        System.err.println("Error no WSI support on physical device 0")
        exitProcess(-1)
    }

    // Select Surface Format
    val requestSurfaceImageFormat = s.callocInt(4).also {
        it[0] = VK_FORMAT_B8G8R8A8_UNORM
        it[1] = VK_FORMAT_R8G8B8A8_UNORM
        it[2] = VK_FORMAT_B8G8R8_UNORM
        it[3] = VK_FORMAT_R8G8B8_UNORM
    }
    val requestSurfaceColorSpace = VK_COLORSPACE_SRGB_NONLINEAR_KHR
    wd.surfaceFormat = ImplVulkanH_.selectSurfaceFormat(gPhysicalDevice, wd.surface, requestSurfaceImageFormat, requestSurfaceColorSpace)

    // Select Present Mode
    val presentModes = when {
        IMGUI_UNLIMITED_FRAME_RATE -> intArrayOf(VK_PRESENT_MODE_MAILBOX_KHR, VK_PRESENT_MODE_IMMEDIATE_KHR, VK_PRESENT_MODE_FIFO_KHR)
        else -> intArrayOf(VK_PRESENT_MODE_FIFO_KHR)
    }
    wd.presentMode = ImplVulkanH_.selectPresentMode(gPhysicalDevice, wd.surface, presentModes)
    //printf("[vulkan] Selected PresentMode = %d\n", wd->PresentMode);

    // Create SwapChain, RenderPass, Framebuffer, etc.
    assert(gMinImageCount >= 2)
    ImplVulkanH_.createOrResizeWindow(gInstance, gPhysicalDevice, gDevice, wd, gQueueFamily, gAllocator, size, gMinImageCount)
}

fun frameRender(wd: ImplVulkanH_.Window, drawData: DrawData) = Stack { s ->

    val imageAcquiredSemaphore = wd.frameSemaphores!![wd.semaphoreIndex].imageAcquiredSemaphore
    val renderCompleteSemaphore = wd.frameSemaphores!![wd.semaphoreIndex].renderCompleteSemaphore
    var err = vkAcquireNextImageKHR(gDevice, wd.swapchain, -1L, imageAcquiredSemaphore[0], VK_NULL_HANDLE, pI)
    wd.frameIndex = pI[0]
    if (err == VK_ERROR_OUT_OF_DATE_KHR) {
        gSwapChainRebuild = true
        return@Stack
    }
    checkVkResult(err)

    val fd = wd.frames!![wd.frameIndex]
    run {
        err = vkWaitForFences(gDevice, fd.fence, true, -1L)    // wait indefinitely instead of periodically checking
        checkVkResult(err)

        err = vkResetFences(gDevice, fd.fence)
        checkVkResult(err)
    }
    run {
        err = vkResetCommandPool(gDevice, fd.commandPool, 0)
        checkVkResult(err)
        val info = VkCommandBufferBeginInfo.callocStack(s)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
        err = vkBeginCommandBuffer(fd.commandBuffer!!, info)
        checkVkResult(err)
    }
    run {
        val info = VkRenderPassBeginInfo.callocStack(s)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                .renderPass(wd.renderPass)
                .framebuffer(fd.framebuffer)
                .renderArea {
                    it.extent().width(wd.size.x)
                    it.extent().height(wd.size.y)
                }
                .pClearValues(wd.clearValue)
        vkCmdBeginRenderPass(fd.commandBuffer!!, info, VK_SUBPASS_CONTENTS_INLINE)
    }

    // Record dear imgui primitives into command buffer
    ImplVulkan_.renderDrawData(drawData, fd.commandBuffer!!)

    // Submit command buffer
    vkCmdEndRenderPass(fd.commandBuffer!!)
    run {
        val waitStage = s.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
        val commandBuffers = s.pointers(fd.commandBuffer!!)
        val info = VkSubmitInfo.callocStack(s)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .waitSemaphoreCount(1)
                .pWaitSemaphores(imageAcquiredSemaphore)
                .pWaitDstStageMask(waitStage)
                .pCommandBuffers(commandBuffers)
                .pSignalSemaphores(renderCompleteSemaphore)

        err = vkEndCommandBuffer(fd.commandBuffer!!)
        checkVkResult(err)
        err = vkQueueSubmit(gQueue, info, fd.fence)
        checkVkResult(err)
    }
}

fun framePresent(wd: ImplVulkanH_.Window) = Stack { s ->
    if (gSwapChainRebuild)
        return@Stack
    val renderCompleteSemaphore = wd.frameSemaphores!![wd.semaphoreIndex].renderCompleteSemaphore
    val swapchains = s.longs(wd.swapchain)
    val indices = s.ints(wd.frameIndex)
    val info = VkPresentInfoKHR.callocStack(s)
            .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
            .pWaitSemaphores(renderCompleteSemaphore)
            .swapchainCount(1)
            .pSwapchains(swapchains)
            .pImageIndices(indices)
    val err = vkQueuePresentKHR(gQueue, info)
    if (err == VK_ERROR_OUT_OF_DATE_KHR) {
        gSwapChainRebuild = true
        return
    }
    checkVkResult(err)
    wd.semaphoreIndex = (wd.semaphoreIndex + 1) % wd.imageCount // Now we can use the next set of semaphores
}