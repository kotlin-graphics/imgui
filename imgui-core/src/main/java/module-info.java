module com.github.kotlin_graphics.imgui_core {

    requires java.desktop;
    requires jdk.jdi;

    requires kotlin.stdlib;

    requires com.github.kotlin_graphics.uno_core;
    requires com.github.kotlin_graphics.gli;
    requires com.github.kotlin_graphics.glm;
    requires com.github.kotlin_graphics.kool;
    requires com.github.kotlin_graphics.kotlin_unsigned;

    requires org.lwjgl;
    requires org.lwjgl.stb;
    requires annotations;

    exports imgui.api;
    exports imgui.classes;
    exports imgui.impl;
    exports imgui.internal;
    exports imgui.internalApi;
    exports imgui.demo.showExampleApp;
    exports imgui.demo;
    exports imgui.stb;
    exports imgui.windowsIme;
    exports imgui;
}