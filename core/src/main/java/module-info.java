module kotlin.graphics.imgui.core {

    requires java.desktop;
    requires jdk.jdi;

    requires kotlin.stdlib;

    requires kotlin.graphics.uno.core;
    requires kotlin.graphics.gli;
    requires kotlin.graphics.glm;
    requires kotlin.graphics.kool;
    requires kotlin.graphics.unsigned;

    requires org.lwjgl;
    requires org.lwjgl.stb;
//    requires annotations;
    requires java.logging;

    exports imgui.api;
    exports imgui.classes;
    exports imgui.internal.classes; // TODO remove me https://sormuras.github.io/blog/2018-09-11-testing-in-the-modular-world.html
    exports imgui.demo.showExampleApp;
    exports imgui.demo;
    exports imgui.font;
    exports imgui.impl;
    exports imgui.internal;
    exports imgui;
    exports imgui.stb;
    exports imgui.windowsIme;
}