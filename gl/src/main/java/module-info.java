module com.github.kotlin_graphics.imgui.gl {

    requires java.desktop;

    requires kotlin.stdlib;

    requires com.github.kotlin_graphics.imgui.core;
    requires com.github.kotlin_graphics.imgui.glfw;
    requires com.github.kotlin_graphics.uno.core;
    requires com.github.kotlin_graphics.glm;
    requires com.github.kotlin_graphics.gln;
    requires com.github.kotlin_graphics.kool;
    requires com.github.kotlin_graphics.kotlin_unsigned;

    requires org.lwjgl.opengl;
    requires org.lwjgl.glfw;

    exports imgui.impl.gl;
}