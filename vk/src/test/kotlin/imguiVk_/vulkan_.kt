package imguiVk_

import glm_.vec4.Vec4
import imgui.ImGui
import imgui.classes.Context
import imgui.impl.glfw.ImplGlfw
import imgui.impl.vk_.*
import kool.Stack
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import uno.glfw.GlfwWindow
import uno.glfw.glfw
import uno.vk.vulkanSupported
import kotlin.system.exitProcess


var IMGUI_UNLIMITED_FRAME_RATE = false

var gAllocator: VkAllocationCallbacks? = null
lateinit var gInstance: VkInstance
lateinit var gPhysicalDevice: VkPhysicalDevice
lateinit var gDevice: VkDevice
var gQueueFamily = -1
lateinit var gQueue: VkQueue
var gDebugReport = 0L
var gPipelineCache = 0L
var gDescriptorPool = 0L

val gMainWindowData = ImplVulkanH_.Window()
var gMinImageCount = 2
var gSwapChainRebuild = false


fun main() {
    ImGuiVulkan()
}

private class ImGuiVulkan {

    val window: GlfwWindow
    val ctx: Context

    var f = 0f
    val clearColor = Vec4(0.45f, 0.55f, 0.6f, 1f)

    // Our state
    var showAnotherWindow = false
    var showDemoWindow = true
    var counter = 0

    val implGlfw: ImplGlfw
    val implVk: ImplVulkan_
    val wd: ImplVulkanH_.Window

    init {

        // Setup GLFW window
        glfw {
            errorCallback = { error, description -> println("Glfw Error $error: $description") }
            init()
            windowHint { api = uno.glfw.windowHint.Api.None }
        }
        window = GlfwWindow(1280, 720, "Dear ImGui GLFW+Vulkan example")

        // Setup Vulkan
        if (!glfw.vulkanSupported) {
            System.err.println("GLFW: Vulkan Not Supported")
            exitProcess(1)
        }
        setupVulkan_(GLFWVulkan.glfwGetRequiredInstanceExtensions()!!)

        // Create Window Surface
        var err = GLFWVulkan.glfwCreateWindowSurface(gInstance, window.handle.value, gAllocator, pL)
        val surface = pL[0]

        // Create Framebuffers
        val size = window.framebufferSize
        wd = gMainWindowData
        setupVulkanWindow_(wd, surface, size)

        // Setup Dear ImGui context
        ctx = Context()
        //io.ConfigFlags |= ImGuiConfigFlags_NavEnableKeyboard;     // Enable Keyboard Controls
        //io.ConfigFlags |= ImGuiConfigFlags_NavEnableGamepad;      // Enable Gamepad Controls

        // Setup Dear ImGui style
        ImGui.styleColorsDark()
        //ImGui::StyleColorsClassic();

        // Setup Platform/Renderer bindings
        implGlfw = ImplGlfw.initForVulkan(window, true)
        val initInfo = ImplVulkan_.InitInfo().also {
            it.instance = gInstance
            it.physicalDevice = gPhysicalDevice
            it.device = gDevice
            it.queueFamily = gQueueFamily
            it.queue = gQueue
            it.pipelineCache = gPipelineCache
            it.descriptorPool = gDescriptorPool
            it.minImageCount = gMinImageCount
            it.imageCount = wd.imageCount
        }
        implVk = ImplVulkan_(initInfo, wd.renderPass)

        // Load Fonts
        // - If no fonts are loaded, dear imgui will use the default font. You can also load multiple fonts and use ImGui::PushFont()/PopFont() to select them.
        // - AddFontFromFileTTF() will return the ImFont* so you can store it if you need to select the font among multiple.
        // - If the file cannot be loaded, the function will return NULL. Please handle those errors in your application (e.g. use an assertion, or display an error and quit).
        // - The fonts will be rasterized at a given size (w/ oversampling) and stored into a texture when calling ImFontAtlas::Build()/GetTexDataAsXXXX(), which ImGui_ImplXXXX_NewFrame below will call.
        // - Read 'docs/FONTS.md' for more instructions and details.
        // - Remember that in C/C++ if you want to include a backslash \ in a string literal you need to write a double backslash \\ !
        //io.Fonts->AddFontDefault();
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/Roboto-Medium.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/Cousine-Regular.ttf", 15.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/DroidSans.ttf", 16.0f);
        //io.Fonts->AddFontFromFileTTF("../../misc/fonts/ProggyTiny.ttf", 10.0f);
        //ImFont* font = io.Fonts->AddFontFromFileTTF("c:\\Windows\\Fonts\\ArialUni.ttf", 18.0f, NULL, io.Fonts->GetGlyphRangesJapanese());
        //IM_ASSERT(font != NULL);

        // Upload Fonts
        Stack { s ->
            // Use any command queue
            val commandPool = wd.frames!![wd.frameIndex].commandPool
            val commandBuffer = wd.frames!![wd.frameIndex].commandBuffer!!

            err = vkResetCommandPool(gDevice, commandPool, 0)
            checkVkResult(err)
            val beginInfo = VkCommandBufferBeginInfo.callocStack(s)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
            err = vkBeginCommandBuffer(commandBuffer, beginInfo)
            checkVkResult(err)

            createFontsTexture(commandBuffer)

            val commandBuffers = s.pointers(commandBuffer)
            val endInfo = VkSubmitInfo.callocStack(s)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .pCommandBuffers(commandBuffers)
            err = vkEndCommandBuffer(commandBuffer)
            checkVkResult(err)
            err = vkQueueSubmit(gQueue, endInfo, VK_NULL_HANDLE)
            checkVkResult(err)

            err = vkDeviceWaitIdle(gDevice)
            checkVkResult(err)
            implVk.destroyFontUploadObjects()
        }

        /*  Main loop
            This automatically also polls events, swaps buffers and resets the appBuffer

            Poll and handle events (inputs, window resize, etc.)
            You can read the io.wantCaptureMouse, io.wantCaptureKeyboard flags to tell if dear imgui wants to use your inputs.
            - When io.wantCaptureMouse is true, do not dispatch mouse input data to your main application.
            - When io.wantCaptureKeyboard is true, do not dispatch keyboard input data to your main application.
            Generally you may always pass all inputs to dear imgui, and hide them from your application based on those two flags.          */
        window.autoSwap = false // disabled for Vulkan
        window.loop(::mainLoop)
    }

    fun mainLoop(stack: MemoryStack) {

        // Resize swap chain?
        if (gSwapChainRebuild) {
            val size = window.framebufferSize
            if (size anyGreaterThan 0) {
                implVk.setMinImageCount(gMinImageCount)
                ImplVulkanH_.createOrResizeWindow(gInstance, gPhysicalDevice, gDevice, gMainWindowData, gQueueFamily, gAllocator, size, gMinImageCount)
                gMainWindowData.frameIndex = 0
                gSwapChainRebuild = false
            }
        }

        // Start the Dear ImGui frame
        implVk.newFrame()
        implGlfw.newFrame()
        ImGui.run {
            newFrame()

            // 1. Show the big demo window (Most of the sample code is in ImGui::ShowDemoWindow()! You can browse its code to learn more about Dear ImGui!).
            if (showDemoWindow)
                showDemoWindow(::showDemoWindow)

            // 2. Show a simple window that we create ourselves. We use a Begin/End pair to created a named window.
            run {

                begin("Hello, world!")                          // Create a window called "Hello, world!" and append into it.

                text("This is some useful text.")               // Display some text (you can use a format strings too)
                checkbox("Demo Window", ::showDemoWindow)      // Edit bools storing our window open/close state
                checkbox("Another Window", ::showAnotherWindow)

                sliderFloat("float", ::f, 0f, 1f)            // Edit 1 float using a slider from 0.0f to 1.0f
                colorEdit3("clear color", clearColor) // Edit 3 floats representing a color

                if (button("Button"))                            // Buttons return true when clicked (most widgets return true when edited/activated)
                    counter++

                /*  Or you can take advantage of functional programming and pass directly a lambda as last parameter:
                button("Button") { counter++ }                */

                sameLine()
                text("counter = $counter")

                text("Application average %.3f ms/frame (%.1f FPS)", 1000f / io.framerate, io.framerate)
                end()
            }

            // 3. Show another simple window.
            if (showAnotherWindow) {
                begin("Another Window", ::showAnotherWindow)   // Pass a pointer to our bool variable (the window will have a closing button that will clear the bool when clicked)
                text("Hello from another window!")
                if (button("Close Me"))
                    showAnotherWindow = false
                end()
            }
        }

        // Rendering
        ImGui.render()
        val drawData = ImGui.drawData!!
        val isMinimized = drawData.displaySize anyLessThanEqual 0f
        if (!isMinimized) {
            clearColor to wd.clearValue.color().float32()
            frameRender(wd, drawData)
            framePresent(wd)
        }
    }
}