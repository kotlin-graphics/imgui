package imgui.gl


import glm_.vec4.Vec4
import gln.checkError
import gln.glClearColor
import gln.glViewport
import imgui.DEBUG
import imgui.ImGui
import imgui.imgui.Context
import imgui.impl.LwjglGlfw
import imgui.impl.LwjglGlfw.GlfwClientApi
import kool.stak
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.Platform
import uno.glfw.GlfwWindow
import uno.glfw.VSync
import uno.glfw.glfw

fun main() {
	TestMultipleWindowsLwjgl()
}

private class TestMultipleWindowsLwjgl {

	val window1: GlfwWindow
	val window2: GlfwWindow
	val lwjglGlfw1: LwjglGlfw
	val lwjglGlfw2: LwjglGlfw
	val ctx1: Context
	val ctx2: Context

	var f = 0f
	val clearColor = Vec4(0.45f, 0.55f, 0.6f, 1f)
	var showAnotherWindow = false
	var showDemo = true
	var counter = 0

	init {
		glfw.init(if (Platform.get() == Platform.MACOSX) "3.2" else "3.0")

		window1 = GlfwWindow(1280, 720, "Window 1").apply {
			init()
		}

		window2 = GlfwWindow(1280, 720, "Window 2").apply {
			init()
		}

		// Don't use vsync with the same thread handling both windows as it halves the framerate
		glfw.swapInterval = VSync.OFF   

		// Setup ImGui binding
//         glslVersion = 330 // set here your desidered glsl version

		// Setup Dear ImGui context 1
		ctx1 = Context()

		// Setup Dear ImGui style for context 1
		ImGui.styleColorsDark()

		// Setup Dear ImGui context 2 with fonts shared from context 1
		ctx2 = Context(ctx1.io.fonts)

		// The first context remains current so to make changes to #2 we have to first make it current
		ctx2.setCurrent()

		// Setup Dear ImGui style for context 2
		ImGui.styleColorsDark()

		// Setup Platform/Renderer bindings for each window
		lwjglGlfw1 = LwjglGlfw(window1, true, GlfwClientApi.OpenGL)
		lwjglGlfw2 = LwjglGlfw(window2, true, GlfwClientApi.OpenGL)

		while (window1.isOpen && window2.isOpen) {

			GLFW.glfwPollEvents()

			stak {
				window1.makeContextCurrent()
				render(it, window1, lwjglGlfw1, ctx1)

				window2.makeContextCurrent()
				render(it, window2, lwjglGlfw2, ctx2)

				if (window1.autoSwap)
					GLFW.glfwSwapBuffers(window1.handle.L)

				if (window2.autoSwap)
					GLFW.glfwSwapBuffers(window2.handle.L)
			}
		}

		lwjglGlfw1.shutdown()
		lwjglGlfw2.shutdown()
		
		ctx1.destroy()
		ctx2.destroy()

		window1.destroy()
		window2.destroy()
		glfw.terminate()
	}

	fun render(stack: MemoryStack, window: GlfwWindow, lwjglGlfw: LwjglGlfw, ctx: Context) {

		ctx.setCurrent()

		// Start the Dear ImGui frame
		lwjglGlfw.newFrame()

		ImGui.run {

			newFrame()

			// 1. Show the big demo window (Most of the sample code is in ImGui::ShowDemoWindow()! You can browse its code to learn more about Dear ImGui!).
			if (showDemo)
				showDemoWindow(::showDemo)

			// 2. Show a simple window that we create ourselves. We use a Begin/End pair to created a named window.
			run {
				begin("Hello, world!")                          // Create a window called "Hello, world!" and append into it.

				text("This is some useful text.")                // Display some text (you can use a format strings too)
				checkbox("Demo Window", ::showDemo)             // Edit bools storing our window open/close state
				checkbox("Another Window", ::showAnotherWindow)

				sliderFloat("float", ::f, 0f, 1f)   // Edit 1 float using a slider from 0.0f to 1.0f
				colorEdit3("clear color", clearColor)           // Edit 3 floats representing a color

				if (button("Button"))                           // Buttons return true when clicked (most widgets return true when edited/activated)
					counter++

				/*  Or you can take advantage of functional programming and pass directly a lambda as last parameter:
            button("Button") { counter++ }                */

				sameLine()
				text("counter = $counter")

				text("Application average %.3f ms/frame (%.1f FPS)", 1_000f / io.framerate, io.framerate)

				end()

				// 3. Show another simple window.
				if (showAnotherWindow) {
					// Pass a pointer to our bool variable (the window will have a closing button that will clear the bool when clicked)
					begin("Another Window", ::showAnotherWindow)
					text("Hello from another window!")
					if (button("Close Me"))
						showAnotherWindow = false
					end()
				}
			}
		}

		// Rendering
		ImGui.render()
		glViewport(window.framebufferSize)
		glClearColor(clearColor)
		glClear(GL_COLOR_BUFFER_BIT)

		lwjglGlfw.renderDrawData(ImGui.drawData!!)

		if (DEBUG)
			checkError("mainLoop")
	}
}