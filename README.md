# dear jvm imgui

[![Build Status](https://github.com/kotlin-graphics/imgui/workflows/build/badge.svg)](https://github.com/kotlin-graphics/imgui/actions?workflow=build)
[![license](https://img.shields.io/badge/License-MIT-orange.svg)](https://github.com/kotlin-graphics/imgui/blob/master/LICENSE) 
[![Release](https://jitpack.io/v/kotlin-graphics/imgui.svg)](https://jitpack.io/#kotlin-graphics/imgui) 
![Size](https://github-size-badge.herokuapp.com/kotlin-graphics/imgui.svg)
[![Github All Releases](https://img.shields.io/github/downloads/kotlin-graphics/imgui/total.svg)]()

(This rewrite is free but, on the same line of the original library, it needs your support to sustain its development. There are many desirable features and maintenance ahead. If you are an individual using dear imgui, please consider donating via Patreon or PayPal. If your company is using dear imgui, please consider financial support (e.g. sponsoring a few weeks/months of development. E-mail: elect86 at gmail).

Monthly donations via Patreon:
<br>[![Patreon](https://cloud.githubusercontent.com/assets/8225057/5990484/70413560-a9ab-11e4-8942-1a63607c0b00.png)](http://www.patreon.com/jvmImGui)

One-off donations via PayPal:
<br>[![PayPal](https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=DJ88XMNUFG4FE)

Btc: 3DKLj6rEZNovEh6xeVp4RU3fk3WxZvFtPM

----------

This is the Kotlin rewrite of [dear imgui](https://github.com/ocornut/imgui) (AKA ImGui), a bloat-free graphical user interface library for C++. It outputs optimized vertex buffers that you can render anytime in your 3D-pipeline enabled application. It is fast, portable, renderer agnostic and self-contained (few external dependencies).

Dear ImGui is designed to enable fast iterations and to empower programmers to create content creation tools and visualization / debug tools (as opposed to UI for the average end-user). It favors simplicity and productivity toward this goal, and lacks certain features normally found in more high-level libraries.

Dear ImGui is particularly suited to integration in games engine (for tooling), real-time 3D applications, fullscreen applications, embedded applications, or any applications on consoles platforms where operating system features are non-standard.

Dear ImGui is self-contained within a few files that you can easily copy and compile into your application/engine.

It doesn't provide the guarantee that dear imgui provides, but it is actually a much better fit for java/kotlin users.

### Usage

Your code passes mouse/keyboard inputs and settings to Dear ImGui (see example applications for more details). After ImGui is setup, you can use it like in this example:


##### Kotlin
```kotlin
with(ImGui) {
    text("Hello, world %d", 123)
    button("Save"){
        // do stuff
    }
    inputText("string", buf)
    sliderFloat("float", ::f, 0f, 1f)
}
```

##### Java
```java
imgui.text("Hello, world %d", 123);
if(imgui.button("Save")) {
    // do stuff
}
imgui.inputText("string", buf);
imgui.sliderFloat("float", f, 0f, 1f);
```

Result:

![screenshot of sample code alongside its output with ImGui](https://raw.githubusercontent.com/wiki/ocornut/imgui/web/v160/code_sample_02.png)

_(settings: Dark style (left), Light style (right) / Font: Roboto-Medium, 16px / Rounding: 5)_

Code:

```kotlin
// Create a window called "My First Tool", with a menu bar.
begin("My First Tool", ::myToolActive, WindowFlag.MenuBar)
menuBar {
    menu("File") {
        menuItem("Open..", "Ctrl+O")) { /* Do stuff */ }
        menuItem("Save", "Ctrl+S"))   { /* Do stuff */ }
        menuItem("Close", "Ctrl+W"))  { myToolActive = false }
    }
}

// Edit a color (stored as FloatArray[4] or Vec4)
colorEdit4("Color", myColor);

// Plot some values
val myValues = floatArrayOf( 0.2f, 0.1f, 1f, 0.5f, 0.9f, 2.2f )
plotLines("Frame Times", myValues)
 
// Display contents in a scrolling region
textColored(Vec4(1,1,0,1), "Important Stuff")
withChild("Scrolling") {
    for (int n = 0; n < 50; n++)
        text("%04d: Some text", n)
}
end()
```

Result:

![screenshot of sample code alongside its output with ImGui](https://raw.githubusercontent.com/wiki/ocornut/imgui/web/v160/code_sample_03_color.gif)


### How it works

Check out the References section if you want to understand the core principles behind the IMGUI paradigm. An IMGUI tries to minimize state duplication, state synchronization and state storage from the user's point of view. It is less error prone (less code and less bugs) than traditional retained-mode interfaces, and lends itself to create dynamic user interfaces.

Dear ImGui outputs vertex buffers and command lists that you can easily render in your application. The number of draw calls and state changes is typically very small. Because it doesn't know or touch graphics state directly, you can call ImGui commands anywhere in your code (e.g. in the middle of a running algorithm, or in the middle of your own rendering process). Refer to the sample applications in the examples/ folder for instructions on how to integrate dear imgui with your existing codebase.

_A common misunderstanding is to mistake immediate mode gui for immediate mode rendering, which usually implies hammering your driver/GPU with a bunch of inefficient draw calls and state changes as the gui functions are called. This is NOT what Dear ImGui does. Dear ImGui outputs vertex buffers and a small list of draw calls batches. It never touches your GPU directly. The draw call batches are decently optimal and you can render them later, in your app or even remotely._

Dear ImGui allows you create elaborate tools as well as very short-lived ones. On the extreme side of short-liveness: using the Edit&Continue (hot code reload) feature of modern compilers you can add a few widgets to tweaks variables while your application is running, and remove the code a minute later! Dear ImGui is not just for tweaking values. You can use it to trace a running algorithm by just emitting text commands. You can use it along with your own reflection data to browse your dataset live. You can use it to expose the internals of a subsystem in your engine, to create a logger, an inspection tool, a profiler, a debugger, an entire game making editor/framework, etc.

### Demo

You should be able to try the examples from `test` (tested on Windows/Mac/Linux) within minutes. If you can't, let me know!

OpenGL:
- [Kotlin](gl/src/test/kotlin/examples/opengl3.kt)
- [Java](gl/src/test/java/imgui/examples/OpenGL3.java)

Vulkan:
- Kotlin:

     - [VK²](vk/src/test/kotlin/imguiVk/vulkan.kt)

     - [LWJGL vanilla](vk/src/test/kotlin/imguiVk_/vulkan_.kt)
- Java:
      - PR me!

You should refer to those also to learn how to use the imgui library.

The demo applications are unfortunately not yet DPI aware so expect some blurriness on a 4K screen. For DPI awareness you can load/reload your font at different scale, and scale your Style with `style.ScaleAllSizes()`.

Ps: `DEBUG = false` to turn off debugs `println()`

### Functional Programming / Domain Specific Language

All the functions are ported exactly as the original. Moreover, in order to take advantage of Functional Programming 
this port offers some comfortable constructs, giving the luxury to forget about some annoying and very error prone
burden code such as the ending `*Pop()`, `*end()` and so on.

These constructs shine especially in Kotlin, where they are also inlined.
 
Let's take an original cpp sample and let's see how we can make it nicer:
```cpp
    if (ImGui::TreeNode("Querying Status (Active/Focused/Hovered etc.)")) {            
        ImGui::Checkbox("Hovered/Active tests after Begin() for title bar testing", &test_window);
        if (test_window) {            
            ImGui::Begin("Title bar Hovered/Active tests", &test_window);
            if (ImGui::BeginPopupContextItem()) // <-- This is using IsItemHovered() {                
                if (ImGui::MenuItem("Close")) { test_window = false; }
                ImGui::EndPopup();
            }
            ImGui::Text("whatever\n");
            ImGui::End();
        }
        ImGui::TreePop();
    }
```
This may become in Kotlin:
```kotlin
    treeNode("Querying Status (Active/Focused/Hovered etc.)") {            
        checkbox("Hovered/Active tests after Begin() for title bar testing", ::test_window)
        if (test_window)
            window ("Title bar Hovered/Active tests", ::test_window) {
                popupContextItem { // <-- This is using IsItemHovered() {                    
                    menuItem("Close") { test_window = false }
                }
                text("whatever\n")
            }
    }
```
Or in Java:
```java
    treeNode("Querying Status (Active/Focused/Hovered etc.)", () -> {            
        checkbox("Hovered/Active tests after Begin() for title bar testing", test_window)
        if (test_window[0])
            window ("Title bar Hovered/Active tests", test_window, () -> {
                popupContextItem(() -> { // <-- This is using IsItemHovered() {                    
                    menuItem("Close", () -> test_window = false);
                });
                text("whatever\n");
            });
    });
```

The demo mixes some traditional imgui-calls with these DSL calls.

Refer to the corresponding [`dsl`](src/main/kotlin/imgui/dsl.kt) object for Kotlin or [`dsl_`](src/main/java/imgui/dsl_.java) class for Java.

### Native Roadmap

Some of the goals of Omar for 2018 are:

- Finish work on gamepad/keyboard controls. (see #787)
- Finish work on viewports and multiple OS windows management. (see #1542)
- Finish work on docking, tabs. (see #351)
- Make Columns better. (they are currently pretty terrible!)
- Make the examples look better, improve styles, improve font support, make the examples hi-DPI aware.
    
### Rewrite Roadmap
   
- finish to rewrite the last few remaining methods
- make text input and handling robust (copy/cut/undo/redo and text filters)
- hunt down bugs

### How to get it

ImGui does not impose any platform specific dependency. Therefor users must specify runtime dependencies themselves. This should
be done with great care to ensure that the dependencies versions do not conflict.

Using Gradle with the following workaround is recommended to keep the manual maintenance cost low.

```groovy
import org.gradle.internal.os.OperatingSystem

repositories {
    ...
    maven { url 'https://jitpack.io' }
}

dependencies {
    /*
    Each renderer will need different dependencies.
    Each one needs core.
    OpenGL needs "gl", "glfw"
    Vulkan needs "vk", "glfw"
    JOGL needs "jogl"
    OpenJFX needs "openjfx"
    
    To get all the dependencies in one sweep, create an array of the strings needed and loop through them like below.
    Any number of renderers can be added to the project like this however, you could all all of them with the array ["gl", "glfw", "core", "vk", "jogl", "openjfx"] 
    This example gets the OpenGL needed modules.
     */
    ["gl", "glfw", "core"].each {
        implementation "com.github.kotlin-graphics.imgui:$it:-SNAPSHOT"
    }
	
    switch ( OperatingSystem.current() ) {
        case OperatingSystem.WINDOWS:
            ext.lwjglNatives = "natives-windows"
            break
        case OperatingSystem.LINUX:
            ext.lwjglNatives = "natives-linux"
            break
        case OperatingSystem.MAC_OS:
            ext.lwjglNatives = "natives-macos"
            break
    }

    // Look up which modules and versions of LWJGL are required and add setup the approriate natives.
    configurations.compile.resolvedConfiguration.getResolvedArtifacts().forEach {
        if (it.moduleVersion.id.group == "org.lwjgl") {
            runtime "org.lwjgl:${it.moduleVersion.id.name}:${it.moduleVersion.id.version}:${lwjglNatives}"
        }
    }
}
```

Please refer to the [wiki](https://github.com/kotlin-graphics/imgui/wiki/Install) for a more detailed guide and for other
systems (such as Maven, Sbt or Leiningen).

Note: total repo size is around 24.1 MB, but there are included 22.6 MB of assets (mainly fonts), this means that the actual size is around 1.5 MB. I always thought a pairs of tens of MB is negligible, but if this is not your case, then just clone and throw away the fonts you don't need or pick up the `imgui-light` jar from the [release page](https://github.com/kotlin-graphics/imgui/releases). Thanks to [chrjen](https://github.com/chrjen) for that.

### LibGdx

On the initiative to Catvert, ImGui plays now nice also with LibGdx.

Simply follow this [short wiki](https://github.com/kotlin-graphics/imgui/wiki/Using-libGDX) on how to set it up.

Gallery
-------

User screenshots:
<br>[Gallery Part 1](https://github.com/ocornut/imgui/issues/123) (Feb 2015 to Feb 2016)
<br>[Gallery Part 2](https://github.com/ocornut/imgui/issues/539) (Feb 2016 to Aug 2016)
<br>[Gallery Part 3](https://github.com/ocornut/imgui/issues/772) (Aug 2016 to Jan 2017)
<br>[Gallery Part 4](https://github.com/ocornut/imgui/issues/973) (Jan 2017 to Aug 2017)
<br>[Gallery Part 5](https://github.com/ocornut/imgui/issues/1269) (Aug 2017 to Feb 2018)
<br>[Gallery Part 6](https://github.com/ocornut/imgui/issues/1607) (Feb 2018 to June 2018)
<br>[Gallery Part 7](https://github.com/ocornut/imgui/issues/1902) (June 2018 to January 2019)
<br>[Gallery Part 8](https://github.com/ocornut/imgui/issues/2265) (January 2019 onward)
<br>Also see the [Mega screenshots](https://github.com/ocornut/imgui/issues/1273) for an idea of the available features.

Various tools
[![screenshot game](https://raw.githubusercontent.com/wiki/ocornut/imgui/web/v149/gallery_TheDragonsTrap-01-thumb.jpg)](https://cloud.githubusercontent.com/assets/8225057/20628927/33e14cac-b329-11e6-80f6-9524e93b048a.png)

[![screenshot tool](https://raw.githubusercontent.com/wiki/ocornut/imgui/web/v160/editor_white_preview.jpg)](https://raw.githubusercontent.com/wiki/ocornut/imgui/web/v160/editor_white.png)

![screenshot demo](https://raw.githubusercontent.com/wiki/ocornut/imgui/web/v160/v160-misc-classic.png)

[![screenshot profiler](https://raw.githubusercontent.com/wiki/ocornut/imgui/web/v148/profiler-880.jpg)](https://raw.githubusercontent.com/wiki/ocornut/imgui/web/v148/profiler.png)


ImGui supports also other languages, such as japanese or chinese, initiliazed [here](https://github.com/kotlin-graphics/imgui/blob/master/src/test/kotlin/imgui/test_lwjgl.kt#L67) as:

```kotlin
IO.fonts.addFontFromFileTTF("extraFonts/ArialUni.ttf", 18f, glyphRanges = IO.fonts.glyphRangesJapanese)!!
```

or [here](https://github.com/kotlin-graphics/imgui/blob/master/src/test/java/imgui/Test_lwjgl.java#L50-L51)

```java
Font font = io.getFonts().addFontFromFileTTF("extraFonts/ArialUni.ttf", 18f, new FontConfig(), io.getFonts().getGlyphRangesJapanese());
assert (font != null);
```

![Imgur](https://i.imgur.com/vul0VbT.png?1)

References
----------

The Immediate Mode GUI paradigm may at first appear unusual to some users. This is mainly because "Retained Mode" GUIs have been so widespread and predominant. The following links can give you a better understanding about how Immediate Mode GUIs works. 
- [Johannes 'johno' Norneby's article](http://www.johno.se/book/imgui.html).
- [A presentation by Rickard Gustafsson and Johannes Algelind](http://www.cse.chalmers.se/edu/year/2011/course/TDA361/Advanced%20Computer%20Graphics/IMGUI.pdf).
- [Jari Komppa's tutorial on building an ImGui library](http://iki.fi/sol/imgui/).
- [Casey Muratori's original video that popularized the concept](https://mollyrocket.com/861).
- [Nicolas Guillemot's CppCon'16 flash-talk about Dear ImGui](https://www.youtube.com/watch?v=LSRJ1jZq90k).
- [Thierry Excoffier's Zero Memory Widget](http://perso.univ-lyon1.fr/thierry.excoffier/ZMW/).

See the [Wiki](https://github.com/ocornut/imgui/wiki) and [Bindings](https://github.com/ocornut/imgui/wiki/Bindings) for third-party bindings to different languages and frameworks.

Credits
-------

Developed by [Omar Cornut](http://www.miracleworld.net) and every direct or indirect contributors to the GitHub. The early version of this library was developed with the support of [Media Molecule](http://www.mediamolecule.com) and first used internally on the game [Tearaway](http://tearaway.mediamolecule.com). 

Omar first discovered imgui principles at [Q-Games](http://www.q-games.com) where Atman had dropped his own simple imgui implementation in the codebase, which Omar spent quite some time improving and thinking about. It turned out that Atman was exposed to the concept directly by working with Casey. When Omar moved to Media Molecule he rewrote a new library trying to overcome the flaws and limitations of the first one he's worked with. It became this library and since then he (and me) has spent an unreasonable amount of time iterating on it. 

Embeds [ProggyClean.ttf](http://upperbounds.net) font by Tristan Grimmer (MIT license).

Embeds [stb_textedit.h, stb_truetype.h, stb_rectpack.h](https://github.com/nothings/stb/) by Sean Barrett (public domain).

Inspiration, feedback, and testing for early versions: Casey Muratori, Atman Binstock, Mikko Mononen, Emmanuel Briney, Stefan Kamoda, Anton Mikhailov, Matt Willis. And everybody posting feedback, questions and patches on the GitHub.

Ongoing native dear imgui development is financially supported on [**Patreon**](http://www.patreon.com/imgui) and by private sponsors (Kotlin imgui [here](https://www.patreon.com/jvmImGui))

Double-chocolate native sponsors:
- Blizzard Entertainment
- Media Molecule
- Mobigame
- Insomniac Games
- Aras Pranckevičius
- Lizardcube
- Greggman
- DotEmu
- Nadeo

and many other [private persons](https://github.com/ocornut/imgui#credits)

I'm very grateful for the support of the persons that have directly contributed to this rewrite via bugs reports, bug fixes, discussions and other forms of support (in alphabetical order):

- Balázs Bódi, [bbodi](https://github.com/bbodi)
- [Catvert](https://github.com/Catvert)
- Christer Jensen, [chrjen](https://github.com/chrjen)
- Florian Schäfers, [Sunny1337](https://github.com/Sunny1337)
- [klianc09](https://github.com/klianc09)
- Leon Linhart, [TheMrMilchmann](https://github.com/TheMrMilchmann)
- Nick Johnson, [Sylvyrfysh](https://github.com/Sylvyrfysh)

License
-------

Dear JVM ImGui is licensed under the MIT License, see LICENSE for more information.
