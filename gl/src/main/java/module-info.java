module kotlin.graphics.imgui.gl {

    requires java.desktop;

    requires kotlin.stdlib;

    requires kotlin.graphics.imgui.core;
    requires kotlin.graphics.imgui.glfw;
    requires kotlin.graphics.uno.core;
    requires kotlin.graphics.glm;
    requires kotlin.graphics.gln;
    requires kotlin.graphics.kool;
    requires kotlin.graphics.unsigned;

    requires org.lwjgl.opengl;
    requires org.lwjgl.glfw;

    exports imgui.impl.gl;
}