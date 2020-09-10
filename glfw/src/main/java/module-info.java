module com.github.kotlin_graphics.imgui_glfw {

    requires kotlin.stdlib;

    requires com.github.kotlin_graphics.imgui_core;
    requires com.github.kotlin_graphics.uno_core;
    requires com.github.kotlin_graphics.glm;
    requires com.github.kotlin_graphics.kool;

    requires org.lwjgl.glfw;

    exports imgui.impl.glfw;
}