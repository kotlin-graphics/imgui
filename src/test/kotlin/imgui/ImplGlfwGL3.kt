package imgui

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.*


object ImplGlfwGL3 {

    var window = 0L
    var time = 0.0
    val mousePressed = Array(3, { false })
    var mouseWheel = 0f
//    static GLuint       g_FontTexture = 0;

//    fun init(window: Long, installCallbacks: Boolean): Boolean {
//
//        this.window = window
//
//        with(IO) {
//            keyMap[Key_.Tab] = GLFW_KEY_TAB
//            keyMap[Key_.LeftArrow] = GLFW_KEY_LEFT
//            keyMap[Key_.RightArrow] = GLFW_KEY_RIGHT
//            keyMap[Key_.UpArrow] = GLFW_KEY_UP
//            keyMap[Key_.DownArrow] = GLFW_KEY_DOWN
//            keyMap[Key_.PageUp] = GLFW_KEY_PAGE_UP
//            keyMap[Key_.PageDown] = GLFW_KEY_PAGE_DOWN
//            keyMap[Key_.Home] = GLFW_KEY_HOME
//            keyMap[Key_.End] = GLFW_KEY_END
//            keyMap[Key_.Delete] = GLFW_KEY_DELETE
//            keyMap[Key_.Backspace] = GLFW_KEY_BACKSPACE
//            keyMap[Key_.Enter] = GLFW_KEY_ENTER
//            keyMap[Key_.Escape] = GLFW_KEY_ESCAPE
//            keyMap[Key_.A] = GLFW_KEY_A
//            keyMap[Key_.C] = GLFW_KEY_C
//            keyMap[Key_.V] = GLFW_KEY_V
//            keyMap[Key_.X] = GLFW_KEY_X
//            keyMap[Key_.Y] = GLFW_KEY_Y
//            keyMap[Key_.Z] = GLFW_KEY_Z
//        }
//    }
}