package imgui

import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import imgui.imgui.Context
import imgui.impl.*
import kool.adr
import kool.indices
import kool.rem
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.system.Pointer
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.VK_FALSE
import uno.glfw.GlfwWindow
import uno.glfw.glfw
import uno.glfw.windowHint.Api.None
import vkk.*
import vkk.entities.*
import vkk.extensionFunctions.*
import kotlin.system.exitProcess


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

    val wd = ImplVkH_Window

    wd.surface = surface

    // Check for WSI support
    val res = physicalDevice.getSurfaceSupportKHR(queueFamily, wd.surface)
    if (!res)
        error("Error no WSI support on physical device 0")

    // Select Surface Format
    val requestSurfaceImageFormat = listOf(VkFormat.B8G8R8A8_UNORM, VkFormat.R8G8B8A8_UNORM, VkFormat.B8G8R8_UNORM, VkFormat.R8G8B8_UNORM)
    val requestSurfaceColorSpace = VkColorSpaceKHR.SRGB_NONLINEAR_KHR
    wd.surfaceFormat = ImplVkH_selectSurfaceFormat(physicalDevice, wd.surface, requestSurfaceImageFormat, requestSurfaceColorSpace)

    // Select Present Mode
    val presentModes = when {
        UNLIMITED_FRAME_RATE -> listOf(VkPresentModeKHR.MAILBOX_KHR, VkPresentModeKHR.IMMEDIATE_KHR, VkPresentModeKHR.FIFO_KHR)
        else -> listOf(VkPresentModeKHR.FIFO_KHR)
    }
    wd.presentMode = ImplVk_selectPresentMode(physicalDevice, wd.surface, presentModes)
    //printf("[vulkan] Selected PresentMode = %d\n", wd->PresentMode);

    // Create SwapChain, RenderPass, Framebuffer, etc.
    assert(minImageCount >= 2)
    ImplVkH_CreateWindow(physicalDevice, device, queueFamily, size, minImageCount)
}

fun cleanupVulkan() {

    device destroy descriptorPool

    if (DEBUG)   // Remove the debug report callback
        instance destroy debugReport

    device.destroy()
    instance.destroy()
}

fun cleanupVulkanWindow() = ImplVkH_destroyWindow(instance, device)

fun frameRender() {

    val wd = ImplVkH_Window

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
    renderDrawData(ImGui.drawData!!, fd.commandBuffer!!)

    // Submit command buffer
    fd.commandBuffer!!.endRenderPass()
    stak {
        val waitStage = it.ints(VkPipelineStage.COLOR_ATTACHMENT_OUTPUT_BIT.i)
        val info = vk.SubmitInfo {
            waitSemaphoreCount = 1
            waitSemaphore = imageAcquiredSemaphore
            waitDstStageMask = waitStage
            commandBuffer = fd.commandBuffer
            signalSemaphore = renderCompleteSemaphore
        }
        fd.commandBuffer!!.end()
        queue.submit(info, fd.fence)
    }
}

fun framePresent() {
    val wd = ImplVkH_Window
    val renderCompleteSemaphore = wd.frameSemaphores[wd.semaphoreIndex].renderCompleteSemaphore
    val info = vk.PresentInfoKHR {
        waitSemaphore = renderCompleteSemaphore
        swapchainCount = 1
        swapchain = wd.swapchain
        imageIndex = wd.frameIndex
    }
    queue presentKHR info
    wd.semaphoreIndex = (wd.semaphoreIndex + 1) % wd.imageCount // Now we can use the next set of semaphores
}

fun main() {

    ImGuiVk().apply {
        mainLoop()
        cleanup()
    }
}

class ImGuiVk {

    lateinit var window: GlfwWindow
    val ctx: Context

    var f = 0f
    val clearColor = Vec4(0.45f, 0.55f, 0.6f, 1f)
    var showAnotherWindow = false
    var showDemo = true
    var counter = 0

    val implGlfw: ImplGlfw

    init {
        // Setup GLFW window
        GLFWErrorCallback.createPrint(System.err).set()
        glfw {

            init()
            windowHint { api = None }
            window = GlfwWindow(1280, 720, "Dear ImGui GLFW+Vulkan example")
        }
        // Setup Vulkan
        if (!glfw.vulkanSupported) {
            System.err.println("GLFW: Vulkan Not Supported")
            exitProcess(1)
        }
        val extensions = glfw.requiredInstanceExtensions
        setupVulkan(extensions)

        // Create Window Surface
        val surface = glfw.createWindowSurface(instance, window.handle)

        // Create Framebuffers
        val size = window.framebufferSize
        window.framebufferSizeCallback = { size_ ->
            swapChainRebuild = true
            swapChainResizeSize = size_
        }
        val wd = ImplVkH_Window
        setupVulkanWindow(surface, size)

        // Setup Dear ImGui context
        ctx = Context()
        //io.ConfigFlags |= ImGuiConfigFlags_NavEnableKeyboard;  // Enable Keyboard Controls
        //io.ConfigFlags |= ImGuiConfigFlags_NavEnableGamepad;   // Enable Gamepad Controls

        // Setup Dear ImGui style
        ImGui.styleColorsDark()
        //ImGui::StyleColorsClassic();

        // Setup Platform/Renderer bindings
        implGlfw = ImplGlfw(window, true)
        val initInfo = VkInitInfo.also {
            it.instance = instance
            it.physicalDevice = physicalDevice
            it.device = device
            it.queueFamily = queueFamily
            it.queue = queue
            it.pipelineCache = pipelineCache
            it.descriptorPool = descriptorPool
            it.minImageCount = minImageCount
            it.imageCount = wd.imageCount
        }
        ImplVk_Init(wd.renderPass)

        // Load Fonts
        // - If no fonts are loaded, dear imgui will use the default font. You can also load multiple fonts and use ImGui::PushFont()/PopFont() to select them.
        // - AddFontFromFileTTF() will return the ImFont* so you can store it if you need to select the font among multiple.
        // - If the file cannot be loaded, the function will return NULL. Please handle those errors in your application (e.g. use an assertion, or display an error and quit).
        // - The fonts will be rasterized at a given size (w/ oversampling) and stored into a texture when calling ImFontAtlas::Build()/GetTexDataAsXXXX(), which ImGui_ImplXXXX_NewFrame below will call.
        // - Read 'misc/fonts/README.txt' for more instructions and details.
        // - Remember that in C/C++ if you want to include a backslash \ in a string literal you need to write a double backslash \\ !
        //io.Fonts->AddFontDefault();
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/Roboto-Medium.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/Cousine-Regular.ttf", 15.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/DroidSans.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/ProggyTiny.ttf", 10.0f);
        //ImFont* font = io.Fonts->AddFontFromFileTTF("c:\\Windows\\Fonts\\ArialUni.ttf", 18.0f, NULL, io.Fonts->GetGlyphRangesJapanese());
        //IM_ASSERT(font != NULL);

        // Upload Fonts
        run {
            // Use any command queue
            val commandPool = wd.frames[wd.frameIndex].commandPool
            val commandBuffer = wd.frames[wd.frameIndex].commandBuffer!!

            device.resetCommandPool(commandPool)
            val beginInfo = vk.CommandBufferBeginInfo {
                flags = flags or VkCommandBufferUsage.ONE_TIME_SUBMIT_BIT
            }
            commandBuffer begin beginInfo

            ImplVk_CreateFontsTexture(commandBuffer)

            val endInfo = vk.SubmitInfo { this.commandBuffer = commandBuffer }
            commandBuffer.end()
            queue.submit(endInfo)

            device.waitIdle()
            ImplVk_DestroyFontUploadObjects()
        }
    }

    fun mainLoop() {

        while (!window.shouldClose) {
            // Poll and handle events (inputs, window resize, etc.)
            // You can read the io.WantCaptureMouse, io.WantCaptureKeyboard flags to tell if dear imgui wants to use your inputs.
            // - When io.WantCaptureMouse is true, do not dispatch mouse input data to your main application.
            // - When io.WantCaptureKeyboard is true, do not dispatch keyboard input data to your main application.
            // Generally you may always pass all inputs to dear imgui, and hide them from your application based on those two flags.
            glfw.pollEvents()

            if (swapChainRebuild) {
                swapChainRebuild = false
                ImplVk_SetMinImageCount(minImageCount)
                ImplVkH_CreateWindow(physicalDevice, device, queueFamily, swapChainResizeSize, minImageCount) // TODO remove top args
                ImplVkH_Window.frameIndex = 0
            }

            // Start the Dear ImGui frame
            ImplVk_NewFrame()
            ImplGlfw.newFrame()
            ImGui.newFrame()

            // 1. Show the big demo window (Most of the sample code is in ImGui::ShowDemoWindow()! You can browse its code to learn more about Dear ImGui!).
            if (showDemo)
                ImGui.showDemoWindow(::showDemo)

            // 2. Show a simple window that we create ourselves. We use a Begin/End pair to created a named window.
            run {
                //                ImGui::Begin("Hello, world!")                          // Create a window called "Hello, world!" and append into it.
//
//                ImGui::Text("This is some useful text.")               // Display some text (you can use a format strings too)
//                ImGui::Checkbox("Demo Window", & show_demo_window)      // Edit bools storing our window open/close state
//                ImGui::Checkbox("Another Window", & show_another_window)
//
//                ImGui::SliderFloat("float", & f, 0.0f, 1.0f)            // Edit 1 float using a slider from 0.0f to 1.0f
//                ImGui::ColorEdit3("clear color", (float *)& clear_color) // Edit 3 floats representing a color
//
//                if (ImGui::Button("Button"))                            // Buttons return true when clicked (most widgets return true when edited/activated)
//                    counter++
//                ImGui::SameLine()
//                ImGui::Text("counter = %d", counter)
//
//                ImGui::Text("Application average %.3f ms/frame (%.1f FPS)", 1000.0f / ImGui::GetIO().Framerate, ImGui::GetIO().Framerate)
//                ImGui::End()
            }

            // 3. Show another simple window.
//            if (show_another_window) {
//                ImGui::Begin("Another Window", & show_another_window)   // Pass a pointer to our bool variable (the window will have a closing button that will clear the bool when clicked)
//                ImGui::Text("Hello from another window!")
//                if (ImGui::Button("Close Me"))
//                    show_another_window = false
//                ImGui::End()
//            }

            // Rendering
            ImGui.render()
            ImplVkH_Window.clearValue.color(clearColor)
            frameRender()

            framePresent()
        }
    }

    fun cleanup() {

        device.waitIdle()
        ImplVk_Shutdown()
        ImplGlfw.shutdown()
        ctx.shutdown()

        cleanupVulkanWindow()
        cleanupVulkan()

        window.destroy()
        glfw.terminate()
    }
}