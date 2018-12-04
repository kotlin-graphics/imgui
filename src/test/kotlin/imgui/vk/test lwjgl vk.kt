//package imgui.vk
//
//import ab.appBuffer
//import glm_.vec2.Vec2
//import glm_.vec2.Vec2i
//import glm_.vec4.Vec4
//import imgui.Cond
//import imgui.Context
//import imgui.ImGui
//import imgui.ImGui.button
//import imgui.ImGui.setNextWindowPos
//import imgui.ImGui.showDemoWindow
//import imgui.ImGui.text
//import imgui.destroy
//import imgui.functionalProgramming.withWindow
//import imgui.impl.ImplVk
//import imgui.impl.LwjglGlfw
//import imgui.impl.LwjglGlfw.GlfwClientApi
//import imgui.impl.LwjglGlfw.window
//import org.lwjgl.system.MemoryUtil
//import org.lwjgl.vulkan.VkPresentInfoKHR
//import uno.glfw.GlfwWindow
//import uno.glfw.glfw
//import uno.glfw.windowHint.Api
//import vkk.*
//
//fun main(args: Array<String>) {
//    HelloWorld_lwjgl().run()
//}
//
//var DEBUG_REPORT = true
//var UNLIMITED_FRAME_RATE = false
//
//private class HelloWorld_lwjgl {
//
//    var resizeWanted = false
//    var resizeSize = Vec2i()
//
//    val wd: ImplVk.WindowData
//        get() = ImplVk.wd
//
//    val ctx: Context
//
//    init {
//        // Setup window
//        glfw.errorCallback = glfw.defaultErrorCallback
//        glfw.init()
//
//        glfw.windowHint { api = Api.None }
//        val window = GlfwWindow(1280, 720, "ImGui GLFW+Vulkan example").apply {
//            autoSwap = false // no swapBuffer with vk
//        }
//
//        // Setup Vulkan
//        if (!glfw.vulkanSupported)
//            throw Error("GLFW: Vulkan Not Supported")
//
//        setupVulkan(glfw.requiredInstanceExtensions)
//
//        // Create Window Surface
//        wd.surface = window createSurface ImplVk.instance
//
//        // Create Framebuffers
//        val size = window.framebufferSize
//        window.framebufferSizeCallback = ::resizeCallback
//        setupVulkanWindowData(size)
//
//        // Setup Dear ImGui context
//        ctx = Context()
//        //io.ConfigFlags |= ImGuiConfigFlags_NavEnableKeyboard;  // Enable Keyboard Controls
//        //io.ConfigFlags |= ImGuiConfigFlags_NavEnableGamepad;   // Enable Gamepad Controls
//
//        // Setup GLFW and Vulkan binding
//        LwjglGlfw.init(window, true, GlfwClientApi.Vulkan)
//
//        // Setup Style
//        ImGui.styleColorsDark()
//        //ImGui.styleColorsClassic()
//
//        // Load Fonts
//        // - If no fonts are loaded, dear imgui will use the default font. You can also load multiple fonts and use ImGui::PushFont()/PopFont() to select them.
//        // - AddFontFromFileTTF() will return the ImFont* so you can store it if you need to select the font among multiple.
//        // - If the file cannot be loaded, the function will return NULL. Please handle those errors in your application (e.g. use an assertion, or display an error and quit).
//        // - The fonts will be rasterized at a given size (w/ oversampling) and stored into a texture when calling ImFontAtlas::Build()/GetTexDataAsXXXX(), which ImGui_ImplXXXX_NewFrame below will call.
//        // - Read 'misc/fonts/README.txt' for more instructions and details.
//        // - Remember that in C/C++ if you want to include a backslash \ in a string literal you need to write a double backslash \\ !
//        //io.Fonts->AddFontDefault();
//        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/Roboto-Medium.ttf", 16.0f);
//        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/Cousine-Regular.ttf", 15.0f);
//        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/DroidSans.ttf", 16.0f);
//        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/ProggyTiny.ttf", 10.0f);
//        //ImFont* font = io.Fonts->AddFontFromFileTTF("c:\\Windows\\Fonts\\ArialUni.ttf", 18.0f, NULL, io.Fonts->GetGlyphRangesJapanese());
//        //IM_ASSERT(font != NULL);
//
//        uploadFonts()
//    }
//
//    fun setupVulkan(extensions: ArrayList<String>) {
//
//        // Create Vulkan Instance
//        run {
//            val createInfo = vk.InstanceCreateInfo { enabledExtensionNames = extensions }
//            if (DEBUG_REPORT) {
//                // Enabling multiple validation layers grouped as LunarG standard validation
//                createInfo.enabledLayerNames = listOf("VK_LAYER_LUNARG_standard_validation")
//
//                // Enable debug report extension
//                extensions += "VK_EXT_debug_report"
//                createInfo.enabledExtensionNames = extensions
//
//                // Create Vulkan Instance
//                ImplVk.instance = vk.createInstance(createInfo)
//
//                // Setup the debug report callback
//                val debugReportCi = vk.DebugReportCallbackCreateInfoEXT {
//                    flags = VkDebugReport.ERROR_BIT_EXT or VkDebugReport.WARNING_BIT_EXT or VkDebugReport.PERFORMANCE_WARNING_BIT_EXT
//                    callback = { _, objType, _, _, _, _, msg, _ ->
//                        System.err.println("[vulkan] ObjectType: $objType\nMessage: $msg\n\n")
//                        false
//                    }
//                }
//                ImplVk.debugReport = ImplVk.instance createDebugReportCallbackEXT debugReportCi
//
//            } else  // Create Vulkan Instance without any debug feature
//                ImplVk.instance = vk.createInstance(createInfo)
//        }
//
//        // Select GPU
//        run {
//            val gpus = ImplVk.instance.enumeratePhysicalDevices()
//
//            /*  If a number >1 of GPUs got reported, you should find the best fit GPU for your purpose
//                e.g. VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU if available, or with the greatest memory available, etc.
//                for sake of simplicity we'll just take the first one, assuming it has a graphics queue family. */
//            ImplVk.physicalDevice = gpus[0]
//        }
//
//        // Select graphics queue family
//        run {
//            val queues = ImplVk.physicalDevice.queueFamilyProperties
//            for (i in queues.indices)
//                if (queues[i].queueFlags has VkQueueFlag.GRAPHICS_BIT) {
//                    ImplVk.queueFamily = i
//                    break
//                }
//            assert(ImplVk.queueFamily != -1)
//        }
//
//        // Create Logical Device (with 1 queue)
//        run {
//            val queueInfo = vk.DeviceQueueCreateInfo {
//                queueFamilyIndex = ImplVk.queueFamily
//                queuePriority = 1f
//            }
//            val createInfo = vk.DeviceCreateInfo {
//                queueCreateInfo = queueInfo
//                enabledExtensionNames = listOf("VK_KHR_swapchain")
//            }
//            ImplVk.device = ImplVk.physicalDevice createDevice createInfo
//            ImplVk.queue = ImplVk.device getQueue ImplVk.queueFamily
//        }
//
//        // Create Descriptor Pool
//        run {
//            val poolSizes = vk.DescriptorPoolSize(
//                    VkDescriptorType.SAMPLER, 1000,
//                    VkDescriptorType.COMBINED_IMAGE_SAMPLER, 1000,
//                    VkDescriptorType.SAMPLED_IMAGE, 1000,
//                    VkDescriptorType.STORAGE_IMAGE, 1000,
//                    VkDescriptorType.UNIFORM_TEXEL_BUFFER, 1000,
//                    VkDescriptorType.STORAGE_TEXEL_BUFFER, 1000,
//                    VkDescriptorType.UNIFORM_BUFFER, 1000,
//                    VkDescriptorType.STORAGE_BUFFER, 1000,
//                    VkDescriptorType.UNIFORM_BUFFER_DYNAMIC, 1000,
//                    VkDescriptorType.STORAGE_BUFFER_DYNAMIC, 1000,
//                    VkDescriptorType.INPUT_ATTACHMENT, 1000)
//            val poolInfo = vk.DescriptorPoolCreateInfo {
//                flags = VkDescriptorPoolCreate.FREE_DESCRIPTOR_SET_BIT.i
//                maxSets = 1000 * poolSizes.capacity()
//                this.poolSizes = poolSizes
//            }
//            ImplVk.descriptorPool = ImplVk.device createDescriptorPool poolInfo
//        }
//    }
//
//    fun resizeCallback(size: Vec2i) {
//        resizeWanted = true
//        resizeSize put size
//    }
//
//    fun setupVulkanWindowData(size: Vec2i) {
//
//        // Check for WSI support
//        if (!ImplVk.physicalDevice.getSurfaceSupportKHR(ImplVk.queueFamily, wd.surface))
//            throw Error("Error no WSI support on physical device 0")
//
//        // Get Surface Format
//        val requestSurfaceImageFormat = arrayOf(VkFormat.B8G8R8A8_UNORM, VkFormat.R8G8B8A8_UNORM, VkFormat.B8G8R8_UNORM, VkFormat.R8G8B8_UNORM)
//        val requestSurfaceColorSpace = VkColorSpace.SRGB_NONLINEAR_KHR
//        wd.surfaceFormat = ImplVk.selectSurfaceFormat(ImplVk.physicalDevice, wd.surface, requestSurfaceImageFormat, requestSurfaceColorSpace)
//
//        // Get Present Mode
//        val presentMode = when {
//            UNLIMITED_FRAME_RATE -> { VK_PRESENT_MODE_MAILBOX_KHR, VK_PRESENT_MODE_IMMEDIATE_KHR, VK_PRESENT_MODE_FIFO_KHR };
//            else -> VkPresentMode.FIFO_KHR
//        }
//        wd.presentMode = ImplVk.selectPresentMode(arrayOf(presentMode))
//      printf("[vulkan] Selected PresentMode = %d\n", wd->PresentMode);
//
//        // Create SwapChain, RenderPass, Framebuffer, etc.
//        ImplVk.createWindowDataCommandBuffers()
//        ImplVk.createWindowDataSwapChainAndFramebuffer(size)
//    }
//
//    fun uploadFonts() {
//        // Use any command queue
//        val commandPool = wd.frame.commandPool
//        val commandBuffer = wd.frame.commandBuffer
//
//        ImplVk.device resetCommandPool commandPool
//        val beginInfo = vk.CommandBufferBeginInfo {
//            flags = flags or VkCommandBufferUsage.ONE_TIME_SUBMIT_BIT
//        }
//        commandBuffer begin beginInfo
//
//        ImplVk.createFontsTexture(commandBuffer)
//
//        val endInfo = vk.SubmitInfo { this.commandBuffer = commandBuffer }
//        commandBuffer.end()
//        ImplVk.queue submit endInfo
//
//        ImplVk.device.waitIdle()
//        ImplVk.invalidateFontUploadObjects()
//    }
//
//    var showDemoWindow = true
//    var showAnotherWindow = false
//    val clearColor = Vec4(0.45f, 0.55f, 0.6f, 1f)
//    var f = 0f
//    var counter = 0
//
//    fun run() {
//        // Main loop
//        // Poll and handle events (inputs, window resize, etc.)
//        // You can read the io.WantCaptureMouse, io.WantCaptureKeyboard flags to tell if dear imgui wants to use your inputs.
//        // - When io.WantCaptureMouse is true, do not dispatch mouse input data to your main application.
//        // - When io.WantCaptureKeyboard is true, do not dispatch keyboard input data to your main application.
//        // Generally you may always pass all inputs to dear imgui, and hide them from your application based on those two flags.
//        LwjglGlfw.window.loop {
//
//            if (resizeWanted) {
//                ImplVk.createWindowDataSwapChainAndFramebuffer(resizeSize)
//                resizeWanted = false
//            }
//
//            // Start the ImGui frame
//            LwjglGlfw.newFrame()
//
//            // 1. Show a simple window.
//            // Tip: if we don't call ImGui::Begin()/ImGui::End() the widgets automatically appears in a window called "Debug".
//            ImGui.apply {
//                text("Hello, world!")                           // Display some text (you can use a format string too)
//                sliderFloat("float", ::f, 0f, 1f)            // Edit 1 float using a slider from 0.0f to 1.0f
//                colorEdit3("clear color", clearColor) // Edit 3 floats representing a color (nb: you could use (float*)&wd->ClearValue instead)
//
//                checkbox("Demo Window", this@HelloWorld_lwjgl::showDemoWindow)      // Edit bools storing our windows open/close state
//                checkbox("Another Window", ::showAnotherWindow)
//
//                if (button("Button"))                            // Buttons return true when clicked (NB: most widgets return true when edited/activated)
//                    counter++
//                sameLine()
//                text("counter = %d", counter)
//
//                text("Application average %.3f ms/frame (%.1f FPS)", 1000f / io.framerate, io.framerate)
//            }
//
//            // 2. Show another simple window. In most cases you will use an explicit Begin/End pair to name your windows.
//            if (showAnotherWindow)
//                withWindow("Another Window", ::showAnotherWindow) {
//                    text("Hello from another window!")
//                    if (button("Close Me"))
//                        showAnotherWindow = false
//                }
//
//            // 3. Show the ImGui demo window. Most of the sample code is in ImGui::ShowDemoWindow(). Read its code to learn more about Dear ImGui!
//            if (showDemoWindow) {
//                setNextWindowPos(Vec2(650, 20), Cond.FirstUseEver) // Normally user code doesn't need/want to call this because positions are saved in .ini file anyway. Here we just want to make the demo initial state a bit more friendly!
//                showDemoWindow(::showDemoWindow)
//            }
//
//            // Rendering
//            ImGui.render()
//            wd.clearValue.color(clearColor)
//            frameRender()
//
//            framePresent()
//        }
//
//        // Cleanup
//        ImplVk.device.waitIdle()
//        LwjglGlfw.shutdown()
//        ctx.destroy()
//        cleanupVulkan()
//
//        window.destroy()
//        glfw.terminate()
//    }
//
//    fun frameRender() {
//
//        val imageAcquiredSemaphore = wd.frame.imageAcquiredSemaphore
//        wd.frameIndex = ImplVk.device.acquireNextImageKHR(wd.swapchain, UINT64_MAX, imageAcquiredSemaphore)
//
//        val fd = wd.frame
//        ImplVk.device.apply {
//            waitForFence(fd.fence, true, UINT64_MAX)    // wait indefinitely instead of periodically checking
//            resetFence(fd.fence)
//        }
//        run {
//            ImplVk.device.resetCommandPool(fd.commandPool)
//            fd.commandBuffer begin vk.CommandBufferBeginInfo { flags = flags or VkCommandBufferUsage.ONE_TIME_SUBMIT_BIT }
//        }
//        run {
//            val info = vk.RenderPassBeginInfo {
//                renderPass = wd.renderPass
//                framebuffer = wd.framebuffer[wd.frameIndex]
//                renderArea.extent(wd.size)
//                clearValue = wd.clearValue
//            }
//            fd.commandBuffer.beginRenderPass(info, VkSubpassContents.INLINE)
//        }
//
//        // Record Imgui Draw Data and draw funcs into command buffer
//        ImplVk.renderDrawData(ImGui.drawData!!)
//
//        // Submit command buffer
//        fd.commandBuffer.endRenderPass()
//        run {
//            val waitStage = appBuffer intBufferOf VkPipelineStage.COLOR_ATTACHMENT_OUTPUT_BIT.i
//            val info = vk.SubmitInfo {
//                waitSemaphoreCount = 1
//                waitSemaphore = imageAcquiredSemaphore
//                waitDstStageMask = waitStage
//                commandBuffer = fd.commandBuffer
//                signalSemaphore = fd.renderCompleteSemaphore
//            }
//            fd.commandBuffer.end()
//            ImplVk.queue.submit(info, fd.fence)
//        }
//    }
//
//    fun framePresent() {
//        val fd = wd.frame
//        val info = vk.PresentInfoKHR {
////            waitSemaphore = fd.renderCompleteSemaphore
//            val pLong = appBuffer.long
//            MemoryUtil.memPutLong(pLong, fd.renderCompleteSemaphore)
//            MemoryUtil.memPutAddress(adr + VkPresentInfoKHR.PWAITSEMAPHORES, pLong)
//            VkPresentInfoKHR.nwaitSemaphoreCount(adr, 1)
//            swapchainCount = 1
//            swapchain = ImplVk.wd.swapchain
//            imageIndex = wd.frameIndex
//        }
//        ImplVk.queue presentKHR info
//    }
//
//    fun cleanupVulkan() {
//        ImplVk.destroyWindowData()
//        ImplVk.device destroyDescriptorPool ImplVk.descriptorPool
//
//        if (DEBUG_REPORT)
//        // Remove the debug report callback
//            ImplVk.instance destroyDebugReportCallbackEXT ImplVk.debugReport
//
//        ImplVk.device.destroy()
//        ImplVk.instance.destroy()
//    }
//}
//
