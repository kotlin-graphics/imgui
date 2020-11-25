package imgui.impl.vk

import vkk.VkSampleCount
import vkk.VkSampleCountFlags
import vkk.entities.VkDescriptorPool
import vkk.entities.VkPipelineCache
import vkk.identifiers.Device
import vkk.identifiers.Instance
import vkk.identifiers.PhysicalDevice
import vkk.identifiers.Queue

// Initialization data, for ImGui_ImplVulkan_Init()
// [Please zero-clear before use!]
class VulkanInitInfo {
    lateinit var instance: Instance
    lateinit var physicalDevice: PhysicalDevice
    lateinit var device: Device
    var queueFamily = 0
    lateinit var queue: Queue
    var pipelineCache = VkPipelineCache.NULL
    var descriptorPool = VkDescriptorPool.NULL
    var minImageCount = 0          // >= 2
    var imageCount = 0             // >= MinImageCount
    var msaaSamples = VkSampleCount._1_BIT   // >= VK_SAMPLE_COUNT_1_BIT
//    const VkAllocationCallbacks* Allocator;
//    void                (*CheckVkResultFn)(VkResult err);
}

