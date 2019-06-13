///*
// * Copyright LWJGL. All rights reserved.
// * License terms: https://www.lwjgl.org/license
// */
//import static org.lwjgl.system.MemoryUtil.*;
//import static org.lwjgl.vulkan.EXTDebugReport.*;
//import static org.lwjgl.vulkan.KHRSwapchain.*;
//import static org.lwjgl.vulkan.KHRSurface.*;
//import static org.lwjgl.vulkan.VK10.*;
//import static org.lwjgl.glfw.GLFW.*;
//import static org.lwjgl.glfw.GLFWVulkan.*;
//import static VKUtil.translateVulkanResult
//
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.nio.FloatBuffer;
//import java.nio.IntBuffer;
//import java.nio.LongBuffer;
//
//import org.lwjgl.PointerBuffer;
//import org.lwjgl.glfw.GLFWKeyCallback;
//import org.lwjgl.glfw.GLFWWindowSizeCallback;
//import org.lwjgl.system.MemoryUtil;
//import org.lwjgl.vulkan.VkApplicationInfo;
//import org.lwjgl.vulkan.VkAttachmentDescription;
//import org.lwjgl.vulkan.VkAttachmentReference;
//import org.lwjgl.vulkan.VkBufferCreateInfo;
//import org.lwjgl.vulkan.VkClearValue;
//import org.lwjgl.vulkan.VkCommandBuffer;
//import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
//import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
//import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
//import org.lwjgl.vulkan.VkDebugReportCallbackCreateInfoEXT;
//import org.lwjgl.vulkan.VkDebugReportCallbackEXT;
//import org.lwjgl.vulkan.VkDevice;
//import org.lwjgl.vulkan.VkDeviceCreateInfo;
//import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
//import org.lwjgl.vulkan.VkExtent2D;
//import org.lwjgl.vulkan.VkFramebufferCreateInfo;
//import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
//import org.lwjgl.vulkan.VkImageViewCreateInfo;
//import org.lwjgl.vulkan.VkInstance;
//import org.lwjgl.vulkan.VkInstanceCreateInfo;
//import org.lwjgl.vulkan.VkMemoryAllocateInfo;
//import org.lwjgl.vulkan.VkMemoryRequirements;
//import org.lwjgl.vulkan.VkPhysicalDevice;
//import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
//import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
//import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
//import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
//import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
//import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
//import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
//import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
//import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
//import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
//import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
//import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
//import org.lwjgl.vulkan.VkPresentInfoKHR;
//import org.lwjgl.vulkan.VkQueue;
//import org.lwjgl.vulkan.VkQueueFamilyProperties;
//import org.lwjgl.vulkan.VkRect2D;
//import org.lwjgl.vulkan.VkRenderPassBeginInfo;
//import org.lwjgl.vulkan.VkRenderPassCreateInfo;
//import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
//import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
//import org.lwjgl.vulkan.VkSubmitInfo;
//import org.lwjgl.vulkan.VkSubpassDescription;
//import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
//import org.lwjgl.vulkan.VkSurfaceFormatKHR;
//import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
//import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
//import org.lwjgl.vulkan.VkVertexInputBindingDescription;
//import org.lwjgl.vulkan.VkViewport;
//
///**
// * Renders a simple triangle on a cornflower blue background on a GLFW window with Vulkan.
// *
// * @author Kai Burjack
// */
//public class TriangleDemo {
//
//    private static final boolean validation = Boolean.parseBoolean(System.getProperty("vulkan.validation", "false"));
//
//    private static ByteBuffer[] layers = {
//            memUTF8("VK_LAYER_LUNARG_standard_validation"),
//    };
//
//    /**
//     * Remove if added to spec.
//     */
//    private static final int VK_FLAGS_NONE = 0;
//
//    /**
//     * This is just -1L, but it is nicer as a symbolic constant.
//     */
//    private static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;
//
//    /**
//     * Create a Vulkan instance using LWJGL 3.
//     *
//     * @return the VkInstance handle
//     */
//    private static VkInstance createInstance(PointerBuffer requiredExtensions) {
//        VkApplicationInfo appInfo = VkApplicationInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
//                .pApplicationName(memUTF8("GLFW Vulkan Demo"))
//                .pEngineName(memUTF8(""))
//                .apiVersion(VK_API_VERSION_1_0);
//        PointerBuffer ppEnabledExtensionNames = memAllocPointer(requiredExtensions.remaining() + 1);
//        ppEnabledExtensionNames.put(requiredExtensions);
//        ByteBuffer VK_EXT_DEBUG_REPORT_EXTENSION = memUTF8(VK_EXT_DEBUG_REPORT_EXTENSION_NAME);
//        ppEnabledExtensionNames.put(VK_EXT_DEBUG_REPORT_EXTENSION);
//        ppEnabledExtensionNames.flip();
//        PointerBuffer ppEnabledLayerNames = memAllocPointer(layers.length);
//        for (int i = 0; validation && i < layers.length; i++)
//            ppEnabledLayerNames.put(layers[i]);
//        ppEnabledLayerNames.flip();
//        VkInstanceCreateInfo pCreateInfo = VkInstanceCreateInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
//                .pNext(NULL)
//                .pApplicationInfo(appInfo)
//                .ppEnabledExtensionNames(ppEnabledExtensionNames)
//                .ppEnabledLayerNames(ppEnabledLayerNames);
//        PointerBuffer pInstance = memAllocPointer(1);
//        int err = vkCreateInstance(pCreateInfo, null, pInstance);
//        long instance = pInstance.get(0);
//        memFree(pInstance);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to create VkInstance: " + translateVulkanResult(err));
//        }
//        VkInstance ret = new VkInstance(instance, pCreateInfo);
//        pCreateInfo.free();
//        memFree(ppEnabledLayerNames);
//        memFree(VK_EXT_DEBUG_REPORT_EXTENSION);
//        memFree(ppEnabledExtensionNames);
//        memFree(appInfo.pApplicationName());
//        memFree(appInfo.pEngineName());
//        appInfo.free();
//        return ret;
//    }
//
//    private static long setupDebugging(VkInstance instance, int flags, VkDebugReportCallbackEXT callback) {
//        VkDebugReportCallbackCreateInfoEXT dbgCreateInfo = VkDebugReportCallbackCreateInfoEXT.calloc()
//                .sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT)
//                .pNext(NULL)
//                .pfnCallback(callback)
//                .pUserData(NULL)
//                .flags(flags);
//        LongBuffer pCallback = memAllocLong(1);
//        int err = vkCreateDebugReportCallbackEXT(instance, dbgCreateInfo, null, pCallback);
//        long callbackHandle = pCallback.get(0);
//        memFree(pCallback);
//        dbgCreateInfo.free();
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to create VkInstance: " + translateVulkanResult(err));
//        }
//        return callbackHandle;
//    }
//
//    private static VkPhysicalDevice getFirstPhysicalDevice(VkInstance instance) {
//        IntBuffer pPhysicalDeviceCount = memAllocInt(1);
//        int err = vkEnumeratePhysicalDevices(instance, pPhysicalDeviceCount, null);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to get number of physical devices: " + translateVulkanResult(err));
//        }
//        PointerBuffer pPhysicalDevices = memAllocPointer(pPhysicalDeviceCount.get(0));
//        err = vkEnumeratePhysicalDevices(instance, pPhysicalDeviceCount, pPhysicalDevices);
//        long physicalDevice = pPhysicalDevices.get(0);
//        memFree(pPhysicalDeviceCount);
//        memFree(pPhysicalDevices);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to get physical devices: " + translateVulkanResult(err));
//        }
//        return new VkPhysicalDevice(physicalDevice, instance);
//    }
//
//    private static class DeviceAndGraphicsQueueFamily {
//        VkDevice device;
//        int queueFamilyIndex;
//        VkPhysicalDeviceMemoryProperties memoryProperties;
//    }
//
//    private static DeviceAndGraphicsQueueFamily createDeviceAndGetGraphicsQueueFamily(VkPhysicalDevice physicalDevice) {
//        IntBuffer pQueueFamilyPropertyCount = memAllocInt(1);
//        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyPropertyCount, null);
//        int queueCount = pQueueFamilyPropertyCount.get(0);
//        VkQueueFamilyProperties.Buffer queueProps = VkQueueFamilyProperties.calloc(queueCount);
//        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyPropertyCount, queueProps);
//        memFree(pQueueFamilyPropertyCount);
//        int graphicsQueueFamilyIndex;
//        for (graphicsQueueFamilyIndex = 0; graphicsQueueFamilyIndex < queueCount; graphicsQueueFamilyIndex++) {
//            if ((queueProps.get(graphicsQueueFamilyIndex).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0)
//                break;
//        }
//        queueProps.free();
//        FloatBuffer pQueuePriorities = memAllocFloat(1).put(0.0f);
//        pQueuePriorities.flip();
//        VkDeviceQueueCreateInfo.Buffer queueCreateInfo = VkDeviceQueueCreateInfo.calloc(1)
//                .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
//                .queueFamilyIndex(graphicsQueueFamilyIndex)
//                .pQueuePriorities(pQueuePriorities);
//
//        PointerBuffer extensions = memAllocPointer(1);
//        ByteBuffer VK_KHR_SWAPCHAIN_EXTENSION = memUTF8(VK_KHR_SWAPCHAIN_EXTENSION_NAME);
//        extensions.put(VK_KHR_SWAPCHAIN_EXTENSION);
//        extensions.flip();
//        PointerBuffer ppEnabledLayerNames = memAllocPointer(layers.length);
//        for (int i = 0; validation && i < layers.length; i++)
//            ppEnabledLayerNames.put(layers[i]);
//        ppEnabledLayerNames.flip();
//
//        VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
//                .pNext(NULL)
//                .pQueueCreateInfos(queueCreateInfo)
//                .ppEnabledExtensionNames(extensions)
//                .ppEnabledLayerNames(ppEnabledLayerNames);
//
//        PointerBuffer pDevice = memAllocPointer(1);
//        int err = vkCreateDevice(physicalDevice, deviceCreateInfo, null, pDevice);
//        long device = pDevice.get(0);
//        memFree(pDevice);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to create device: " + translateVulkanResult(err));
//        }
//
//        VkPhysicalDeviceMemoryProperties memoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
//        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);
//
//        DeviceAndGraphicsQueueFamily ret = new DeviceAndGraphicsQueueFamily();
//        ret.device = new VkDevice(device, physicalDevice, deviceCreateInfo);
//        ret.queueFamilyIndex = graphicsQueueFamilyIndex;
//        ret.memoryProperties = memoryProperties;
//
//        deviceCreateInfo.free();
//        memFree(ppEnabledLayerNames);
//        memFree(VK_KHR_SWAPCHAIN_EXTENSION);
//        memFree(extensions);
//        memFree(pQueuePriorities);
//        return ret;
//    }
//
//    private static class ColorFormatAndSpace {
//        int colorFormat;
//        int colorSpace;
//    }
//
//    private static ColorFormatAndSpace getColorFormatAndSpace(VkPhysicalDevice physicalDevice, long surface) {
//        IntBuffer pQueueFamilyPropertyCount = memAllocInt(1);
//        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyPropertyCount, null);
//        int queueCount = pQueueFamilyPropertyCount.get(0);
//        VkQueueFamilyProperties.Buffer queueProps = VkQueueFamilyProperties.calloc(queueCount);
//        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyPropertyCount, queueProps);
//        memFree(pQueueFamilyPropertyCount);
//
//        // Iterate over each queue to learn whether it supports presenting:
//        IntBuffer supportsPresent = memAllocInt(queueCount);
//        for (int i = 0; i < queueCount; i++) {
//            supportsPresent.position(i);
//            int err = vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, i, surface, supportsPresent);
//            if (err != VK_SUCCESS) {
//                throw new AssertionError("Failed to physical device surface support: " + translateVulkanResult(err));
//            }
//        }
//
//        // Search for a graphics and a present queue in the array of queue families, try to find one that supports both
//        int graphicsQueueNodeIndex = Integer.MAX_VALUE;
//        int presentQueueNodeIndex = Integer.MAX_VALUE;
//        for (int i = 0; i < queueCount; i++) {
//            if ((queueProps.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
//                if (graphicsQueueNodeIndex == Integer.MAX_VALUE) {
//                    graphicsQueueNodeIndex = i;
//                }
//                if (supportsPresent.get(i) == VK_TRUE) {
//                    graphicsQueueNodeIndex = i;
//                    presentQueueNodeIndex = i;
//                    break;
//                }
//            }
//        }
//        queueProps.free();
//        if (presentQueueNodeIndex == Integer.MAX_VALUE) {
//            // If there's no queue that supports both present and graphics try to find a separate present queue
//            for (int i = 0; i < queueCount; ++i) {
//                if (supportsPresent.get(i) == VK_TRUE) {
//                    presentQueueNodeIndex = i;
//                    break;
//                }
//            }
//        }
//        memFree(supportsPresent);
//
//        // Generate error if could not find both a graphics and a present queue
//        if (graphicsQueueNodeIndex == Integer.MAX_VALUE) {
//            throw new AssertionError("No graphics queue found");
//        }
//        if (presentQueueNodeIndex == Integer.MAX_VALUE) {
//            throw new AssertionError("No presentation queue found");
//        }
//        if (graphicsQueueNodeIndex != presentQueueNodeIndex) {
//            throw new AssertionError("Presentation queue != graphics queue");
//        }
//
//        // Get list of supported formats
//        IntBuffer pFormatCount = memAllocInt(1);
//        int err = vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, pFormatCount, null);
//        int formatCount = pFormatCount.get(0);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to query number of physical device surface formats: " + translateVulkanResult(err));
//        }
//
//        VkSurfaceFormatKHR.Buffer surfFormats = VkSurfaceFormatKHR.calloc(formatCount);
//        err = vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, pFormatCount, surfFormats);
//        memFree(pFormatCount);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to query physical device surface formats: " + translateVulkanResult(err));
//        }
//
//        int colorFormat;
//        if (formatCount == 1 && surfFormats.get(0).format() == VK_FORMAT_UNDEFINED) {
//            colorFormat = VK_FORMAT_B8G8R8A8_UNORM;
//        } else {
//            colorFormat = surfFormats.get(0).format();
//        }
//        int colorSpace = surfFormats.get(0).colorSpace();
//        surfFormats.free();
//
//        ColorFormatAndSpace ret = new ColorFormatAndSpace();
//        ret.colorFormat = colorFormat;
//        ret.colorSpace = colorSpace;
//        return ret;
//    }
//
//    private static long createCommandPool(VkDevice device, int queueNodeIndex) {
//        VkCommandPoolCreateInfo cmdPoolInfo = VkCommandPoolCreateInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
//                .queueFamilyIndex(queueNodeIndex)
//                .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
//        LongBuffer pCmdPool = memAllocLong(1);
//        int err = vkCreateCommandPool(device, cmdPoolInfo, null, pCmdPool);
//        long commandPool = pCmdPool.get(0);
//        cmdPoolInfo.free();
//        memFree(pCmdPool);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to create command pool: " + translateVulkanResult(err));
//        }
//        return commandPool;
//    }
//
//    private static VkQueue createDeviceQueue(VkDevice device, int queueFamilyIndex) {
//        PointerBuffer pQueue = memAllocPointer(1);
//        vkGetDeviceQueue(device, queueFamilyIndex, 0, pQueue);
//        long queue = pQueue.get(0);
//        memFree(pQueue);
//        return new VkQueue(queue, device);
//    }
//
//    private static VkCommandBuffer createCommandBuffer(VkDevice device, long commandPool) {
//        VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
//                .commandPool(commandPool)
//                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
//                .commandBufferCount(1);
//        PointerBuffer pCommandBuffer = memAllocPointer(1);
//        int err = vkAllocateCommandBuffers(device, cmdBufAllocateInfo, pCommandBuffer);
//        cmdBufAllocateInfo.free();
//        long commandBuffer = pCommandBuffer.get(0);
//        memFree(pCommandBuffer);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to allocate command buffer: " + translateVulkanResult(err));
//        }
//        return new VkCommandBuffer(commandBuffer, device);
//    }
//
//    private static class Swapchain {
//        long swapchainHandle;
//        long[] images;
//        long[] imageViews;
//    }
//
//    private static Swapchain createSwapChain(VkDevice device, VkPhysicalDevice physicalDevice, long surface, long oldSwapChain, VkCommandBuffer commandBuffer, int newWidth,
//                                             int newHeight, int colorFormat, int colorSpace) {
//        int err;
//        // Get physical device surface properties and formats
//        VkSurfaceCapabilitiesKHR surfCaps = VkSurfaceCapabilitiesKHR.calloc();
//        err = vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, surfCaps);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to get physical device surface capabilities: " + translateVulkanResult(err));
//        }
//
//        IntBuffer pPresentModeCount = memAllocInt(1);
//        err = vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, pPresentModeCount, null);
//        int presentModeCount = pPresentModeCount.get(0);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to get number of physical device surface presentation modes: " + translateVulkanResult(err));
//        }
//
//        IntBuffer pPresentModes = memAllocInt(presentModeCount);
//        err = vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, pPresentModeCount, pPresentModes);
//        memFree(pPresentModeCount);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to get physical device surface presentation modes: " + translateVulkanResult(err));
//        }
//
//        // Try to use mailbox mode. Low latency and non-tearing
//        int swapchainPresentMode = VK_PRESENT_MODE_FIFO_KHR;
//        for (int i = 0; i < presentModeCount; i++) {
//            if (pPresentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
//                swapchainPresentMode = VK_PRESENT_MODE_MAILBOX_KHR;
//                break;
//            }
//            if ((swapchainPresentMode != VK_PRESENT_MODE_MAILBOX_KHR) && (pPresentModes.get(i) == VK_PRESENT_MODE_IMMEDIATE_KHR)) {
//                swapchainPresentMode = VK_PRESENT_MODE_IMMEDIATE_KHR;
//            }
//        }
//        memFree(pPresentModes);
//
//        // Determine the number of images
//        int desiredNumberOfSwapchainImages = surfCaps.minImageCount() + 1;
//        if ((surfCaps.maxImageCount() > 0) && (desiredNumberOfSwapchainImages > surfCaps.maxImageCount())) {
//            desiredNumberOfSwapchainImages = surfCaps.maxImageCount();
//        }
//
//        VkExtent2D currentExtent = surfCaps.currentExtent();
//        int currentWidth = currentExtent.width();
//        int currentHeight = currentExtent.height();
//        if (currentWidth != -1 && currentHeight != -1) {
//            width = currentWidth;
//            height = currentHeight;
//        } else {
//            width = newWidth;
//            height = newHeight;
//        }
//
//        int preTransform;
//        if ((surfCaps.supportedTransforms() & VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR) != 0) {
//            preTransform = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
//        } else {
//            preTransform = surfCaps.currentTransform();
//        }
//        surfCaps.free();
//
//        VkSwapchainCreateInfoKHR swapchainCI = VkSwapchainCreateInfoKHR.calloc()
//                .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
//                .pNext(NULL)
//                .surface(surface)
//                .minImageCount(desiredNumberOfSwapchainImages)
//                .imageFormat(colorFormat)
//                .imageColorSpace(colorSpace)
//                .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
//                .preTransform(preTransform)
//                .imageArrayLayers(1)
//                .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
//                .pQueueFamilyIndices(null)
//                .presentMode(swapchainPresentMode)
//                .oldSwapchain(oldSwapChain)
//                .clipped(true)
//                .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
//        swapchainCI.imageExtent()
//                .width(width)
//                .height(height);
//        LongBuffer pSwapChain = memAllocLong(1);
//        err = vkCreateSwapchainKHR(device, swapchainCI, null, pSwapChain);
//        swapchainCI.free();
//        long swapChain = pSwapChain.get(0);
//        memFree(pSwapChain);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to create swap chain: " + translateVulkanResult(err));
//        }
//
//        // If we just re-created an existing swapchain, we should destroy the old swapchain at this point.
//        // Note: destroying the swapchain also cleans up all its associated presentable images once the platform is done with them.
//        if (oldSwapChain != VK_NULL_HANDLE) {
//            vkDestroySwapchainKHR(device, oldSwapChain, null);
//        }
//
//        IntBuffer pImageCount = memAllocInt(1);
//        err = vkGetSwapchainImagesKHR(device, swapChain, pImageCount, null);
//        int imageCount = pImageCount.get(0);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to get number of swapchain images: " + translateVulkanResult(err));
//        }
//
//        LongBuffer pSwapchainImages = memAllocLong(imageCount);
//        err = vkGetSwapchainImagesKHR(device, swapChain, pImageCount, pSwapchainImages);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to get swapchain images: " + translateVulkanResult(err));
//        }
//        memFree(pImageCount);
//
//        long[] images = new long[imageCount];
//        long[] imageViews = new long[imageCount];
//        LongBuffer pBufferView = memAllocLong(1);
//        VkImageViewCreateInfo colorAttachmentView = VkImageViewCreateInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
//                .pNext(NULL)
//                .format(colorFormat)
//                .viewType(VK_IMAGE_VIEW_TYPE_2D)
//                .flags(VK_FLAGS_NONE);
//        colorAttachmentView.components()
//                .r(VK_COMPONENT_SWIZZLE_IDENTITY)
//                .g(VK_COMPONENT_SWIZZLE_IDENTITY)
//                .b(VK_COMPONENT_SWIZZLE_IDENTITY)
//                .a(VK_COMPONENT_SWIZZLE_IDENTITY);
//        colorAttachmentView.subresourceRange()
//                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
//                .baseMipLevel(0)
//                .levelCount(1)
//                .baseArrayLayer(0)
//                .layerCount(1);
//        for (int i = 0; i < imageCount; i++) {
//            images[i] = pSwapchainImages.get(i);
//            colorAttachmentView.image(images[i]);
//            err = vkCreateImageView(device, colorAttachmentView, null, pBufferView);
//            imageViews[i] = pBufferView.get(0);
//            if (err != VK_SUCCESS) {
//                throw new AssertionError("Failed to create image view: " + translateVulkanResult(err));
//            }
//        }
//        colorAttachmentView.free();
//        memFree(pBufferView);
//        memFree(pSwapchainImages);
//
//        Swapchain ret = new Swapchain();
//        ret.images = images;
//        ret.imageViews = imageViews;
//        ret.swapchainHandle = swapChain;
//        return ret;
//    }
//
//    private static long createRenderPass(VkDevice device, int colorFormat) {
//        VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(1)
//                .format(colorFormat)
//                .samples(VK_SAMPLE_COUNT_1_BIT)
//                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
//                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
//                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
//                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
//                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
//                .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
//
//        VkAttachmentReference.Buffer colorReference = VkAttachmentReference.calloc(1)
//                .attachment(0)
//                .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
//
//        VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1)
//                .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
//                .flags(VK_FLAGS_NONE)
//                .pInputAttachments(null)
//                .colorAttachmentCount(colorReference.remaining())
//                .pColorAttachments(colorReference) // <- only color attachment
//                .pResolveAttachments(null)
//                .pDepthStencilAttachment(null)
//                .pPreserveAttachments(null);
//
//        VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
//                .pNext(NULL)
//                .pAttachments(attachments)
//                .pSubpasses(subpass)
//                .pDependencies(null);
//
//        LongBuffer pRenderPass = memAllocLong(1);
//        int err = vkCreateRenderPass(device, renderPassInfo, null, pRenderPass);
//        long renderPass = pRenderPass.get(0);
//        memFree(pRenderPass);
//        renderPassInfo.free();
//        colorReference.free();
//        subpass.free();
//        attachments.free();
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to create clear render pass: " + translateVulkanResult(err));
//        }
//        return renderPass;
//    }
//
//    private static long[] createFramebuffers(VkDevice device, Swapchain swapchain, long renderPass, int width, int height) {
//        LongBuffer attachments = memAllocLong(1);
//        VkFramebufferCreateInfo fci = VkFramebufferCreateInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
//                .pAttachments(attachments)
//                .flags(VK_FLAGS_NONE)
//                .height(height)
//                .width(width)
//                .layers(1)
//                .pNext(NULL)
//                .renderPass(renderPass);
//        // Create a framebuffer for each swapchain image
//        long[] framebuffers = new long[swapchain.images.length];
//        LongBuffer pFramebuffer = memAllocLong(1);
//        for (int i = 0; i < swapchain.images.length; i++) {
//            attachments.put(0, swapchain.imageViews[i]);
//            int err = vkCreateFramebuffer(device, fci, null, pFramebuffer);
//            long framebuffer = pFramebuffer.get(0);
//            if (err != VK_SUCCESS) {
//                throw new AssertionError("Failed to create framebuffer: " + translateVulkanResult(err));
//            }
//            framebuffers[i] = framebuffer;
//        }
//        memFree(attachments);
//        memFree(pFramebuffer);
//        fci.free();
//        return framebuffers;
//    }
//
//    private static void submitCommandBuffer(VkQueue queue, VkCommandBuffer commandBuffer) {
//        if (commandBuffer == null || commandBuffer.address() == NULL)
//            return;
//        VkSubmitInfo submitInfo = VkSubmitInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
//        PointerBuffer pCommandBuffers = memAllocPointer(1)
//                .put(commandBuffer)
//                .flip();
//        submitInfo.pCommandBuffers(pCommandBuffers);
//        int err = vkQueueSubmit(queue, submitInfo, VK_NULL_HANDLE);
//        memFree(pCommandBuffers);
//        submitInfo.free();
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to submit command buffer: " + translateVulkanResult(err));
//        }
//    }
//
//    private static long loadShader(String classPath, VkDevice device, int stage) throws IOException {
//        ByteBuffer shaderCode = glslToSpirv(classPath, stage);
//        int err;
//        VkShaderModuleCreateInfo moduleCreateInfo = VkShaderModuleCreateInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
//                .pNext(NULL)
//                .pCode(shaderCode)
//                .flags(0);
//        LongBuffer pShaderModule = memAllocLong(1);
//        err = vkCreateShaderModule(device, moduleCreateInfo, null, pShaderModule);
//        long shaderModule = pShaderModule.get(0);
//        memFree(pShaderModule);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to create shader module: " + translateVulkanResult(err));
//        }
//        return shaderModule;
//    }
//
//    private static VkPipelineShaderStageCreateInfo loadShader(VkDevice device, String classPath, int stage) throws IOException {
//        VkPipelineShaderStageCreateInfo shaderStage = VkPipelineShaderStageCreateInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
//                .stage(stage)
//                .module(loadShader(classPath, device, stage))
//                .pName(memUTF8("main"));
//        return shaderStage;
//    }
//
//    private static boolean getMemoryType(VkPhysicalDeviceMemoryProperties deviceMemoryProperties, int typeBits, int properties, IntBuffer typeIndex) {
//        int bits = typeBits;
//        for (int i = 0; i < 32; i++) {
//            if ((bits & 1) == 1) {
//                if ((deviceMemoryProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
//                    typeIndex.put(0, i);
//                    return true;
//                }
//            }
//            bits >>= 1;
//        }
//        return false;
//    }
//
//    private static class Vertices {
//        long verticesBuf;
//        VkPipelineVertexInputStateCreateInfo createInfo;
//    }
//
//    private static Vertices createVertices(VkPhysicalDeviceMemoryProperties deviceMemoryProperties, VkDevice device) {
//        ByteBuffer vertexBuffer = memAlloc(3 * 2 * 4);
//        FloatBuffer fb = vertexBuffer.asFloatBuffer();
//        // The triangle will showup upside-down, because Vulkan does not do proper viewport transformation to
//        // account for inverted Y axis between the window coordinate system and clip space/NDC
//        fb.put(-0.5f).put(-0.5f);
//        fb.put( 0.5f).put(-0.5f);
//        fb.put( 0.0f).put( 0.5f);
//
//        VkMemoryAllocateInfo memAlloc = VkMemoryAllocateInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
//                .pNext(NULL)
//                .allocationSize(0)
//                .memoryTypeIndex(0);
//        VkMemoryRequirements memReqs = VkMemoryRequirements.calloc();
//
//        int err;
//
//        // Generate vertex buffer
//        //  Setup
//        VkBufferCreateInfo bufInfo = VkBufferCreateInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
//                .pNext(NULL)
//                .size(vertexBuffer.remaining())
//                .usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
//                .flags(0);
//        LongBuffer pBuffer = memAllocLong(1);
//        err = vkCreateBuffer(device, bufInfo, null, pBuffer);
//        long verticesBuf = pBuffer.get(0);
//        memFree(pBuffer);
//        bufInfo.free();
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to create vertex buffer: " + translateVulkanResult(err));
//        }
//
//        vkGetBufferMemoryRequirements(device, verticesBuf, memReqs);
//        memAlloc.allocationSize(memReqs.size());
//        IntBuffer memoryTypeIndex = memAllocInt(1);
//        getMemoryType(deviceMemoryProperties, memReqs.memoryTypeBits(), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT, memoryTypeIndex);
//        memAlloc.memoryTypeIndex(memoryTypeIndex.get(0));
//        memFree(memoryTypeIndex);
//        memReqs.free();
//
//        LongBuffer pMemory = memAllocLong(1);
//        err = vkAllocateMemory(device, memAlloc, null, pMemory);
//        long verticesMem = pMemory.get(0);
//        memFree(pMemory);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to allocate vertex memory: " + translateVulkanResult(err));
//        }
//
//        PointerBuffer pData = memAllocPointer(1);
//        err = vkMapMemory(device, verticesMem, 0, memAlloc.allocationSize(), 0, pData);
//        memAlloc.free();
//        long data = pData.get(0);
//        memFree(pData);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to map vertex memory: " + translateVulkanResult(err));
//        }
//
//        MemoryUtil.memCopy(memAddress(vertexBuffer), data, vertexBuffer.remaining());
//        memFree(vertexBuffer);
//        vkUnmapMemory(device, verticesMem);
//        err = vkBindBufferMemory(device, verticesBuf, verticesMem, 0);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to bind memory to vertex buffer: " + translateVulkanResult(err));
//        }
//
//        // Binding description
//        VkVertexInputBindingDescription.Buffer bindingDescriptor = VkVertexInputBindingDescription.calloc(1)
//                .binding(0) // <- we bind our vertex buffer to point 0
//                .stride(2 * 4)
//                .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
//
//        // Attribute descriptions
//        // Describes memory layout and shader attribute locations
//        VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(1);
//        // Location 0 : Position
//        attributeDescriptions.get(0)
//                .binding(0) // <- binding point used in the VkVertexInputBindingDescription
//                .location(0) // <- location in the shader's attribute layout (inside the shader source)
//                .format(VK_FORMAT_R32G32_SFLOAT)
//                .offset(0);
//
//        // Assign to vertex buffer
//        VkPipelineVertexInputStateCreateInfo vi = VkPipelineVertexInputStateCreateInfo.calloc();
//        vi.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
//        vi.pNext(NULL);
//        vi.pVertexBindingDescriptions(bindingDescriptor);
//        vi.pVertexAttributeDescriptions(attributeDescriptions);
//
//        Vertices ret = new Vertices();
//        ret.createInfo = vi;
//        ret.verticesBuf = verticesBuf;
//        return ret;
//    }
//
//    private static long createPipeline(VkDevice device, long renderPass, VkPipelineVertexInputStateCreateInfo vi) throws IOException {
//        int err;
//        // Vertex input state
//        // Describes the topoloy used with this pipeline
//        VkPipelineInputAssemblyStateCreateInfo inputAssemblyState = VkPipelineInputAssemblyStateCreateInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
//                .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
//
//        // Rasterization state
//        VkPipelineRasterizationStateCreateInfo rasterizationState = VkPipelineRasterizationStateCreateInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
//                .polygonMode(VK_POLYGON_MODE_FILL)
//                .cullMode(VK_CULL_MODE_NONE)
//                .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
//                .depthClampEnable(false)
//                .rasterizerDiscardEnable(false)
//                .depthBiasEnable(false)
//                .lineWidth(1.0f);
//
//        // Color blend state
//        // Describes blend modes and color masks
//        VkPipelineColorBlendAttachmentState.Buffer colorWriteMask = VkPipelineColorBlendAttachmentState.calloc(1)
//                .blendEnable(false)
//                .colorWriteMask(0xF); // <- RGBA
//        VkPipelineColorBlendStateCreateInfo colorBlendState = VkPipelineColorBlendStateCreateInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
//                .pAttachments(colorWriteMask);
//
//        // Viewport state
//        VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
//                .viewportCount(1) // <- one viewport
//                .scissorCount(1); // <- one scissor rectangle
//
//        // Enable dynamic states
//        // Describes the dynamic states to be used with this pipeline
//        // Dynamic states can be set even after the pipeline has been created
//        // So there is no need to create new pipelines just for changing
//        // a viewport's dimensions or a scissor box
//        IntBuffer pDynamicStates = memAllocInt(2);
//        pDynamicStates.put(VK_DYNAMIC_STATE_VIEWPORT).put(VK_DYNAMIC_STATE_SCISSOR).flip();
//        VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc()
//                // The dynamic state properties themselves are stored in the command buffer
//                .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
//                .pDynamicStates(pDynamicStates);
//
//        // Depth and stencil state
//        // Describes depth and stenctil test and compare ops
//        VkPipelineDepthStencilStateCreateInfo depthStencilState = VkPipelineDepthStencilStateCreateInfo.calloc()
//                // No depth test/write and no stencil used
//                .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
//                .depthTestEnable(false)
//                .depthWriteEnable(false)
//                .depthCompareOp(VK_COMPARE_OP_ALWAYS)
//                .depthBoundsTestEnable(false)
//                .stencilTestEnable(false);
//        depthStencilState.back()
//                .failOp(VK_STENCIL_OP_KEEP)
//                .passOp(VK_STENCIL_OP_KEEP)
//                .compareOp(VK_COMPARE_OP_ALWAYS);
//        depthStencilState.front(depthStencilState.back());
//
//        // Multi sampling state
//        // No multi sampling used in this example
//        VkPipelineMultisampleStateCreateInfo multisampleState = VkPipelineMultisampleStateCreateInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
//                .pSampleMask(null)
//                .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);
//
//        // Load shaders
//        VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2);
//        shaderStages.get(0).set(loadShader(device, "org/lwjgl/demo/vulkan/triangle.vert", VK_SHADER_STAGE_VERTEX_BIT));
//        shaderStages.get(1).set(loadShader(device, "org/lwjgl/demo/vulkan/triangle.frag", VK_SHADER_STAGE_FRAGMENT_BIT));
//
//        // Create the pipeline layout that is used to generate the rendering pipelines that
//        // are based on this descriptor set layout
//        VkPipelineLayoutCreateInfo pPipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
//                .pNext(NULL)
//                .pSetLayouts(null);
//
//        LongBuffer pPipelineLayout = memAllocLong(1);
//        err = vkCreatePipelineLayout(device, pPipelineLayoutCreateInfo, null, pPipelineLayout);
//        long layout = pPipelineLayout.get(0);
//        memFree(pPipelineLayout);
//        pPipelineLayoutCreateInfo.free();
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to create pipeline layout: " + translateVulkanResult(err));
//        }
//
//        // Assign states
//        VkGraphicsPipelineCreateInfo.Buffer pipelineCreateInfo = VkGraphicsPipelineCreateInfo.calloc(1)
//                .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
//                .layout(layout) // <- the layout used for this pipeline (NEEDS TO BE SET! even though it is basically empty)
//                .renderPass(renderPass) // <- renderpass this pipeline is attached to
//                .pVertexInputState(vi)
//                .pInputAssemblyState(inputAssemblyState)
//                .pRasterizationState(rasterizationState)
//                .pColorBlendState(colorBlendState)
//                .pMultisampleState(multisampleState)
//                .pViewportState(viewportState)
//                .pDepthStencilState(depthStencilState)
//                .pStages(shaderStages)
//                .pDynamicState(dynamicState);
//
//        // Create rendering pipeline
//        LongBuffer pPipelines = memAllocLong(1);
//        err = vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineCreateInfo, null, pPipelines);
//        long pipeline = pPipelines.get(0);
//        shaderStages.free();
//        multisampleState.free();
//        depthStencilState.free();
//        dynamicState.free();
//        memFree(pDynamicStates);
//        viewportState.free();
//        colorBlendState.free();
//        colorWriteMask.free();
//        rasterizationState.free();
//        inputAssemblyState.free();
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to create pipeline: " + translateVulkanResult(err));
//        }
//        return pipeline;
//    }
//
//    private static VkCommandBuffer[] createRenderCommandBuffers(VkDevice device, long commandPool, long[] framebuffers, long renderPass, int width, int height,
//                                                                long pipeline, long verticesBuf) {
//        // Create the render command buffers (one command buffer per framebuffer image)
//        VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
//                .commandPool(commandPool)
//                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
//                .commandBufferCount(framebuffers.length);
//        PointerBuffer pCommandBuffer = memAllocPointer(framebuffers.length);
//        int err = vkAllocateCommandBuffers(device, cmdBufAllocateInfo, pCommandBuffer);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to allocate render command buffer: " + translateVulkanResult(err));
//        }
//        VkCommandBuffer[] renderCommandBuffers = new VkCommandBuffer[framebuffers.length];
//        for (int i = 0; i < framebuffers.length; i++) {
//            renderCommandBuffers[i] = new VkCommandBuffer(pCommandBuffer.get(i), device);
//        }
//        memFree(pCommandBuffer);
//        cmdBufAllocateInfo.free();
//
//        // Create the command buffer begin structure
//        VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
//                .pNext(NULL);
//
//        // Specify clear color (cornflower blue)
//        VkClearValue.Buffer clearValues = VkClearValue.calloc(1);
//        clearValues.color()
//                .float32(0, 100/255.0f)
//                .float32(1, 149/255.0f)
//                .float32(2, 237/255.0f)
//                .float32(3, 1.0f);
//
//        // Specify everything to begin a render pass
//        VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
//                .pNext(NULL)
//                .renderPass(renderPass)
//                .pClearValues(clearValues);
//        VkRect2D renderArea = renderPassBeginInfo.renderArea();
//        renderArea.offset().set(0, 0);
//        renderArea.extent().set(width, height);
//
//        for (int i = 0; i < renderCommandBuffers.length; ++i) {
//            // Set target frame buffer
//            renderPassBeginInfo.framebuffer(framebuffers[i]);
//
//            err = vkBeginCommandBuffer(renderCommandBuffers[i], cmdBufInfo);
//            if (err != VK_SUCCESS) {
//                throw new AssertionError("Failed to begin render command buffer: " + translateVulkanResult(err));
//            }
//
//            vkCmdBeginRenderPass(renderCommandBuffers[i], renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);
//
//            // Update dynamic viewport state
//            VkViewport.Buffer viewport = VkViewport.calloc(1)
//                    .height(height)
//                    .width(width)
//                    .minDepth(0.0f)
//                    .maxDepth(1.0f);
//            vkCmdSetViewport(renderCommandBuffers[i], 0, viewport);
//            viewport.free();
//
//            // Update dynamic scissor state
//            VkRect2D.Buffer scissor = VkRect2D.calloc(1);
//            scissor.extent().set(width, height);
//            scissor.offset().set(0, 0);
//            vkCmdSetScissor(renderCommandBuffers[i], 0, scissor);
//            scissor.free();
//
//            // Bind the rendering pipeline (including the shaders)
//            vkCmdBindPipeline(renderCommandBuffers[i], VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
//
//            // Bind triangle vertices
//            LongBuffer offsets = memAllocLong(1);
//            offsets.put(0, 0L);
//            LongBuffer pBuffers = memAllocLong(1);
//            pBuffers.put(0, verticesBuf);
//            vkCmdBindVertexBuffers(renderCommandBuffers[i], 0, pBuffers, offsets);
//            memFree(pBuffers);
//            memFree(offsets);
//
//            // Draw triangle
//            vkCmdDraw(renderCommandBuffers[i], 3, 1, 0, 0);
//
//            vkCmdEndRenderPass(renderCommandBuffers[i]);
//
//            err = vkEndCommandBuffer(renderCommandBuffers[i]);
//            if (err != VK_SUCCESS) {
//                throw new AssertionError("Failed to begin render command buffer: " + translateVulkanResult(err));
//            }
//        }
//        renderPassBeginInfo.free();
//        clearValues.free();
//        cmdBufInfo.free();
//        return renderCommandBuffers;
//    }
//
//    /*
//     * All resources that must be reallocated on window resize.
//     */
//    private static Swapchain swapchain;
//    private static long[] framebuffers;
//    private static int width, height;
//    private static VkCommandBuffer[] renderCommandBuffers;
//
//    public static void main(String[] args) throws IOException {
//        if (!glfwInit()) {
//            throw new RuntimeException("Failed to initialize GLFW");
//        }
//        if (!glfwVulkanSupported()) {
//            throw new AssertionError("GLFW failed to find the Vulkan loader");
//        }
//
//        /* Look for instance extensions */
//        PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
//        if (requiredExtensions == null) {
//            throw new AssertionError("Failed to find list of required Vulkan extensions");
//        }
//
//        // Create the Vulkan instance
//        final VkInstance instance = createInstance(requiredExtensions);
//        final VkDebugReportCallbackEXT debugCallback = new VkDebugReportCallbackEXT() {
//            public int invoke(int flags, int objectType, long object, long location, int messageCode, long pLayerPrefix, long pMessage, long pUserData) {
//                System.err.println("ERROR OCCURED: " + VkDebugReportCallbackEXT.getString(pMessage));
//                return 0;
//            }
//        };
//        final long debugCallbackHandle = setupDebugging(instance, VK_DEBUG_REPORT_ERROR_BIT_EXT | VK_DEBUG_REPORT_WARNING_BIT_EXT, debugCallback);
//        final VkPhysicalDevice physicalDevice = getFirstPhysicalDevice(instance);
//        final DeviceAndGraphicsQueueFamily deviceAndGraphicsQueueFamily = createDeviceAndGetGraphicsQueueFamily(physicalDevice);
//        final VkDevice device = deviceAndGraphicsQueueFamily.device;
//        int queueFamilyIndex = deviceAndGraphicsQueueFamily.queueFamilyIndex;
//        final VkPhysicalDeviceMemoryProperties memoryProperties = deviceAndGraphicsQueueFamily.memoryProperties;
//
//        // Create GLFW window
//        glfwDefaultWindowHints();
//        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
//        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
//        long window = glfwCreateWindow(800, 600, "GLFW Vulkan Demo", NULL, NULL);
//        GLFWKeyCallback keyCallback;
//        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
//            public void invoke(long window, int key, int scancode, int action, int mods) {
//                if (action != GLFW_RELEASE)
//                    return;
//                if (key == GLFW_KEY_ESCAPE)
//                    glfwSetWindowShouldClose(window, true);
//            }
//        });
//        LongBuffer pSurface = memAllocLong(1);
//        int err = glfwCreateWindowSurface(instance, window, null, pSurface);
//        final long surface = pSurface.get(0);
//        if (err != VK_SUCCESS) {
//            throw new AssertionError("Failed to create surface: " + translateVulkanResult(err));
//        }
//
//        // Create static Vulkan resources
//        final ColorFormatAndSpace colorFormatAndSpace = getColorFormatAndSpace(physicalDevice, surface);
//        final long commandPool = createCommandPool(device, queueFamilyIndex);
//        final VkCommandBuffer setupCommandBuffer = createCommandBuffer(device, commandPool);
//        final VkQueue queue = createDeviceQueue(device, queueFamilyIndex);
//        final long renderPass = createRenderPass(device, colorFormatAndSpace.colorFormat);
//        final long renderCommandPool = createCommandPool(device, queueFamilyIndex);
//        final Vertices vertices = createVertices(memoryProperties, device);
//        final long pipeline = createPipeline(device, renderPass, vertices.createInfo);
//
//        final class SwapchainRecreator {
//            boolean mustRecreate = true;
//            void recreate() {
//                // Begin the setup command buffer (the one we will use for swapchain/framebuffer creation)
//                VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.calloc()
//                        .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
//                        .pNext(NULL);
//                int err = vkBeginCommandBuffer(setupCommandBuffer, cmdBufInfo);
//                cmdBufInfo.free();
//                if (err != VK_SUCCESS) {
//                    throw new AssertionError("Failed to begin setup command buffer: " + translateVulkanResult(err));
//                }
//                long oldChain = swapchain != null ? swapchain.swapchainHandle : VK_NULL_HANDLE;
//                // Create the swapchain (this will also add a memory barrier to initialize the framebuffer images)
//                swapchain = createSwapChain(device, physicalDevice, surface, oldChain, setupCommandBuffer,
//                        width, height, colorFormatAndSpace.colorFormat, colorFormatAndSpace.colorSpace);
//                err = vkEndCommandBuffer(setupCommandBuffer);
//                if (err != VK_SUCCESS) {
//                    throw new AssertionError("Failed to end setup command buffer: " + translateVulkanResult(err));
//                }
//                submitCommandBuffer(queue, setupCommandBuffer);
//                vkQueueWaitIdle(queue);
//
//                if (framebuffers != null) {
//                    for (int i = 0; i < framebuffers.length; i++)
//                        vkDestroyFramebuffer(device, framebuffers[i], null);
//                }
//                framebuffers = createFramebuffers(device, swapchain, renderPass, width, height);
//                // Create render command buffers
//                if (renderCommandBuffers != null) {
//                    vkResetCommandPool(device, renderCommandPool, VK_FLAGS_NONE);
//                }
//                renderCommandBuffers = createRenderCommandBuffers(device, renderCommandPool, framebuffers, renderPass, width, height, pipeline,
//                        vertices.verticesBuf);
//
//                mustRecreate = false;
//            }
//        }
//        final SwapchainRecreator swapchainRecreator = new SwapchainRecreator();
//
//        // Handle canvas resize
//        GLFWWindowSizeCallback windowSizeCallback = new GLFWWindowSizeCallback() {
//            public void invoke(long window, int width, int height) {
//                if (width <= 0 || height <= 0)
//                    return;
//                TriangleDemo.width = width;
//                TriangleDemo.height = height;
//                swapchainRecreator.mustRecreate = true;
//            }
//        };
//        glfwSetWindowSizeCallback(window, windowSizeCallback);
//        glfwShowWindow(window);
//
//        // Pre-allocate everything needed in the render loop
//
//        IntBuffer pImageIndex = memAllocInt(1);
//        int currentBuffer = 0;
//        PointerBuffer pCommandBuffers = memAllocPointer(1);
//        LongBuffer pSwapchains = memAllocLong(1);
//        LongBuffer pImageAcquiredSemaphore = memAllocLong(1);
//        LongBuffer pRenderCompleteSemaphore = memAllocLong(1);
//
//        // Info struct to create a semaphore
//        VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
//                .pNext(NULL)
//                .flags(VK_FLAGS_NONE);
//
//        // Info struct to submit a command buffer which will wait on the semaphore
//        IntBuffer pWaitDstStageMask = memAllocInt(1);
//        pWaitDstStageMask.put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
//        VkSubmitInfo submitInfo = VkSubmitInfo.calloc()
//                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
//                .pNext(NULL)
//                .waitSemaphoreCount(pImageAcquiredSemaphore.remaining())
//                .pWaitSemaphores(pImageAcquiredSemaphore)
//                .pWaitDstStageMask(pWaitDstStageMask)
//                .pCommandBuffers(pCommandBuffers)
//                .pSignalSemaphores(pRenderCompleteSemaphore);
//
//        // Info struct to present the current swapchain image to the display
//        VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc()
//                .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
//                .pNext(NULL)
//                .pWaitSemaphores(pRenderCompleteSemaphore)
//                .swapchainCount(pSwapchains.remaining())
//                .pSwapchains(pSwapchains)
//                .pImageIndices(pImageIndex)
//                .pResults(null);
//
//        // The render loop
//        while (!glfwWindowShouldClose(window)) {
//            // Handle window messages. Resize events happen exactly here.
//            // So it is safe to use the new swapchain images and framebuffers afterwards.
//            glfwPollEvents();
//            if (swapchainRecreator.mustRecreate)
//                swapchainRecreator.recreate();
//
//            // Create a semaphore to wait for the swapchain to acquire the next image
//            err = vkCreateSemaphore(device, semaphoreCreateInfo, null, pImageAcquiredSemaphore);
//            if (err != VK_SUCCESS) {
//                throw new AssertionError("Failed to create image acquired semaphore: " + translateVulkanResult(err));
//            }
//
//            // Create a semaphore to wait for the render to complete, before presenting
//            err = vkCreateSemaphore(device, semaphoreCreateInfo, null, pRenderCompleteSemaphore);
//            if (err != VK_SUCCESS) {
//                throw new AssertionError("Failed to create render complete semaphore: " + translateVulkanResult(err));
//            }
//
//            // Get next image from the swap chain (back/front buffer).
//            // This will setup the imageAquiredSemaphore to be signalled when the operation is complete
//            err = vkAcquireNextImageKHR(device, swapchain.swapchainHandle, UINT64_MAX, pImageAcquiredSemaphore.get(0), VK_NULL_HANDLE, pImageIndex);
//            currentBuffer = pImageIndex.get(0);
//            if (err != VK_SUCCESS) {
//                throw new AssertionError("Failed to acquire next swapchain image: " + translateVulkanResult(err));
//            }
//
//            // Select the command buffer for the current framebuffer image/attachment
//            pCommandBuffers.put(0, renderCommandBuffers[currentBuffer]);
//
//            // Submit to the graphics queue
//            err = vkQueueSubmit(queue, submitInfo, VK_NULL_HANDLE);
//            if (err != VK_SUCCESS) {
//                throw new AssertionError("Failed to submit render queue: " + translateVulkanResult(err));
//            }
//
//            // Present the current buffer to the swap chain
//            // This will display the image
//            pSwapchains.put(0, swapchain.swapchainHandle);
//            err = vkQueuePresentKHR(queue, presentInfo);
//            if (err != VK_SUCCESS) {
//                throw new AssertionError("Failed to present the swapchain image: " + translateVulkanResult(err));
//            }
//            // Create and submit post present barrier
//            vkQueueWaitIdle(queue);
//
//            // Destroy this semaphore (we will create a new one in the next frame)
//            vkDestroySemaphore(device, pImageAcquiredSemaphore.get(0), null);
//            vkDestroySemaphore(device, pRenderCompleteSemaphore.get(0), null);
//        }
//        presentInfo.free();
//        memFree(pWaitDstStageMask);
//        submitInfo.free();
//        memFree(pImageAcquiredSemaphore);
//        memFree(pRenderCompleteSemaphore);
//        semaphoreCreateInfo.free();
//        memFree(pSwapchains);
//        memFree(pCommandBuffers);
//
//        vkDestroyDebugReportCallbackEXT(instance, debugCallbackHandle, null);
//
//        windowSizeCallback.free();
//        keyCallback.free();
//        glfwDestroyWindow(window);
//        glfwTerminate();
//
//        // We don't bother disposing of all Vulkan resources.
//        // Let the OS process manager take care of it.
//    }
//
//}
