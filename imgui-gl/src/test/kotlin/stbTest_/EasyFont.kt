package stbTest_

import kool.Buffer
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11
import stbTest.FontDemo
import stb_.easyFont

fun main() {
    EasyFont("doc/README.md").run("STB Easy Font Demo.kt")
}

/** STB Easy Font demo.  */
class EasyFont(filePath: String) : FontDemo(BASE_HEIGHT, filePath) {
    override fun loop() {
        val charBuffer = Buffer(text.length * 270)
        val quads = easyFont.print(text = getText(), vertexBuffer = charBuffer)
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY)
        GL11.glVertexPointer(2, GL11.GL_FLOAT, 16, charBuffer)
        GL11.glClearColor(43f / 255f, 43f / 255f, 43f / 255f, 0f) // BG color
        GL11.glColor3f(169f / 255f, 183f / 255f, 198f / 255f) // Text color
        while (!GLFW.glfwWindowShouldClose(getWindow())) {
            GLFW.glfwPollEvents()
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)
            val scaleFactor = 1.0f + getScale() * 0.25f
            GL11.glPushMatrix()
            // Zoom
            GL11.glScalef(scaleFactor, scaleFactor, 1f)
            // Scroll
            GL11.glTranslatef(4.0f, 4.0f - getLineOffset() * getFontHeight(), 0f)
            GL11.glDrawArrays(GL11.GL_QUADS, 0, quads * 4)
            GL11.glPopMatrix()
            GLFW.glfwSwapBuffers(getWindow())
        }
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY)
    }

    companion object {
        private const val BASE_HEIGHT = 12
    }
}