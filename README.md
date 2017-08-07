# imgui

This is the Kotlin port of [dear imgui](https://github.com/ocornut/imgui) (AKA ImGui), a bloat-free graphical user interface library in C++. It outputs optimized vertex buffers that you can render anytime in your 3D-pipeline enabled application. It is fast, portable, renderer agnostic and self-contained (few dependencies).

ImGui is designed to enable fast iteration and empower programmers to create content creation tools and visualization/ debug tools (as opposed to UI for the average end-user). It favors simplicity and productivity toward this goal, and thus lacks certain features normally found in more high-level libraries.

ImGui is particularly suited to integration in realtime 3D applications, fullscreen applications, embedded applications, games, or any applications on consoles platforms where operating system features are non-standard. 

ImGui is self-contained within a few files that you can easily copy and compile into your application/engine.

Your code passes mouse/keyboard inputs and settings to ImGui (see example applications for more details). After ImGui is setup, you can use it like in this example:

### Kotlin
```kotlin
with(ImGui) {
    text("Hello, world %d", 123)
    button("OK"){
        // react
    }
    inputText("string", buf)
    sliderFloat("float", f, 0f, 1f)
}
```

### Java
```Java
ImGui imgui = ImGui.INSTANCE;
imgui.text("Hello, world %d", 123);
if(imgui.button("OK")) {
    // react
}
imgui.inputText("string", buf);
imgui.sliderFloat("float", f, 0f, 1f);
```



![screenshot of sample code alongside its output with ImGui](http://i.imgur.com/KOhZQTu.png)

ImGui outputs vertex buffers and simple command-lists that you can render in your application. The number of draw calls and state changes is typically very small. Because it doesn't know or touch graphics state directly, you can call ImGui commands anywhere in your code (e.g. in the middle of a running algorithm, or in the middle of your own rendering process). Refer to the sample applications in the examples/ folder for instructions on how to integrate ImGui with your existing codebase. 

_A common misunderstanding is to think that immediate mode gui == immediate mode rendering, which usually implies hammering your driver/GPU with a bunch of inefficient draw calls and state changes, as the gui functions are called by the user. This is NOT what Dear ImGui does. Dear ImGui outputs vertex buffers and a small list of draw calls batches. It never touches your GPU directly. The draw call batches are decently optimal and you can render them later, in your app or even remotely._

ImGui allows you create elaborate tools as well as very short-lived ones. On the extreme side of short-liveness: using the Edit&Continue feature of modern compilers you can add a few widgets to tweaks variables while your application is running, and remove the code a minute later! ImGui is not just for tweaking values. You can use it to trace a running algorithm by just emitting text commands. You can use it along with your own reflection data to browse your dataset live. You can use it to expose the internals of a subsystem in your engine, to create a logger, an inspection tool, a profiler, a debugger, an entire game making editor/framework, etc.  


### Status:

To check what has been already ported and working, simply run one of the tests: [lwjgl](https://github.com/kotlin-graphics/imgui/blob/master/src/test/kotlin/imgui/test_lwjgl.kt) or [jogl](https://github.com/kotlin-graphics/imgui/blob/master/src/test/kotlin/imgui/test_jogl.kt).

![Imgur](http://i.imgur.com/0tsoWEP.png)

You should refer to those also to learn how to use the imgui library.


### Bugs:

- copy/cut/undo/redo text
- window resize constrains
