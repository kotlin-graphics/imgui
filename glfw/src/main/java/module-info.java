module com.github.kotlin_graphics.imgui.glfw {

    requires kotlin.stdlib;

    requires com.github.kotlin_graphics.imgui.core;
    requires com.github.kotlin_graphics.uno.core;
    requires com.github.kotlin_graphics.glm;
    requires com.github.kotlin_graphics.kool;

    requires org.lwjgl.glfw;

    exports imgui.impl.glfw;
}