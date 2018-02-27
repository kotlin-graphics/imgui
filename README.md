# imgui

[![Build Status](https://travis-ci.org/kotlin-graphics/imgui.svg?branch=master)](https://travis-ci.org/kotlin-graphics/imgui) 
[![license](https://img.shields.io/badge/License-MIT-orange.svg)](https://github.com/kotlin-graphics/imgui/blob/master/LICENSE) 
[![Release](https://jitpack.io/v/kotlin-graphics/imgui.svg)](https://jitpack.io/#kotlin-graphics/imgui) 
![](https://reposs.herokuapp.com/?path=kotlin-graphics/imgui&color=yellow) 
[![Github All Releases](https://img.shields.io/github/downloads/kotlin-graphics/imgui/total.svg)]()
[![Slack Status](http://slack.kotlinlang.org/badge.svg)](http://slack.kotlinlang.org/)

This porting is free but needs your support to sustain its development. There are lots of desirable new features and maintenance to do. If you are an individual using dear imgui, please consider donating via Patreon or PayPal. If your company is using dear imgui, please consider financial support (e.g. sponsoring a few weeks/months of development)

Monthly donations via Patreon:
<br>[![Patreon](https://cloud.githubusercontent.com/assets/8225057/5990484/70413560-a9ab-11e4-8942-1a63607c0b00.png)](http://www.patreon.com/jvmImGui)

One-off donations via PayPal:
<br>[![PayPal](https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=DJ88XMNUFG4FE)

----------

This is the Kotlin port of [dear imgui](https://github.com/ocornut/imgui) (AKA ImGui), a bloat-free graphical user interface library in C++. It outputs optimized vertex buffers that you can render anytime in your 3D-pipeline enabled application. It is fast, portable, renderer agnostic and self-contained (few dependencies).

Dear ImGui is designed to enable fast iteration and empower programmers to create content creation tools and visualization/ debug tools (as opposed to UI for the average end-user). It favors simplicity and productivity toward this goal, and thus lacks certain features normally found in more high-level libraries.

Dear ImGui is particularly suited to integration in realtime 3D applications, fullscreen applications, embedded applications, games, or any applications on consoles platforms where operating system features are non-standard. 

Dear ImGui is self-contained within a few files that you can easily copy and compile into your application/engine.

Your code passes mouse/keyboard inputs and settings to Dear ImGui (see example applications for more details). After ImGui is setup, you can use it like in this example:

### Kotlin
```kotlin
var f = 0f
with(ImGui) {
    text("Hello, world %d", 123)
    button("OK"){
        // react
    }
    inputText("string", buf)
    sliderFloat("float", ::f, 0f, 1f)
}
```

### Java
```java
ImGui imgui = ImGui.INSTANCE;
float[] f = {0f};
imgui.text("Hello, world %d", 123);
if(imgui.button("OK")) {
    // react
}
imgui.inputText("string", buf);
imgui.sliderFloat("float", f, 0f, 1f);
```



![screenshot of sample code alongside its output with ImGui](http://i.imgur.com/KOhZQTu.png)

Dear ImGui outputs vertex buffers and simple command-lists that you can render in your application. The number of draw calls and state changes is typically very small. Because it doesn't know or touch graphics state directly, you can call ImGui commands anywhere in your code (e.g. in the middle of a running algorithm, or in the middle of your own rendering process). Refer to the sample applications in the examples/ folder for instructions on how to integrate ImGui with your existing codebase. 

_A common misunderstanding is to think that immediate mode gui == immediate mode rendering, which usually implies hammering your driver/GPU with a bunch of inefficient draw calls and state changes, as the gui functions are called by the user. This is NOT what Dear ImGui does. Dear ImGui outputs vertex buffers and a small list of draw calls batches. It never touches your GPU directly. The draw call batches are decently optimal and you can render them later, in your app or even remotely._

Dear ImGui allows you create elaborate tools as well as very short-lived ones. On the extreme side of short-liveness: using the Edit&Continue feature of modern compilers you can add a few widgets to tweaks variables while your application is running, and remove the code a minute later! ImGui is not just for tweaking values. You can use it to trace a running algorithm by just emitting text commands. You can use it along with your own reflection data to browse your dataset live. You can use it to expose the internals of a subsystem in your engine, to create a logger, an inspection tool, a profiler, a debugger, an entire game making editor/framework, etc.  

### Gallery
-------

User screenshots:
<br>[Gallery Part 1](https://github.com/ocornut/imgui/issues/123) (Feb 2015 to Feb 2016)
<br>[Gallery Part 2](https://github.com/ocornut/imgui/issues/539) (Feb 2016 to Aug 2016)
<br>[Gallery Part 3](https://github.com/ocornut/imgui/issues/772) (Aug 2016 to Jan 2017)
<br>[Gallery Part 4](https://github.com/ocornut/imgui/issues/973) (Jan 2017 to Aug 2017)
<br>[Gallery Part 5](https://github.com/ocornut/imgui/issues/1269) (Aug 2017 onward)
<br>Also see the [Mega screenshots](https://github.com/ocornut/imgui/issues/1273) for an idea of the available features.

![screenshot 1](https://raw.githubusercontent.com/wiki/ocornut/imgui/web/v148/examples_01.png)

Various tools
<br>[![screenshot game](https://raw.githubusercontent.com/wiki/ocornut/imgui/web/v149/gallery_TheDragonsTrap-01-thumb.jpg)](https://cloud.githubusercontent.com/assets/8225057/20628927/33e14cac-b329-11e6-80f6-9524e93b048a.png)

Custom profiler
<br>[![screenshot profiler](https://raw.githubusercontent.com/wiki/ocornut/imgui/web/v148/profiler-880.jpg)](https://raw.githubusercontent.com/wiki/ocornut/imgui/web/v148/profiler.png)

Color Pickers
<br>![screenshot picker](https://user-images.githubusercontent.com/8225057/29062188-471e95ba-7c53-11e7-9618-c4484c0b75fe.PNG)

Menus
<br>![screenshot 5](https://raw.githubusercontent.com/wiki/ocornut/imgui/web/v151/menus.png)

Colors
<br>![screenshot 6](https://raw.githubusercontent.com/wiki/ocornut/imgui/web/v143/skinning_sample_02.png)

Custom rendering
<br>![screenshot 7](https://raw.githubusercontent.com/wiki/ocornut/imgui/web/v160/v160-drawdatabase.jpg)

ImGui supports also other languages, such as japanese, initiliazed [here](https://github.com/kotlin-graphics/imgui/blob/master/src/test/kotlin/imgui/test_lwjgl.kt#L67) as:

```kotlin
IO.fonts.addFontFromFileTTF("extraFonts/ArialUni.ttf", 18f, glyphRanges = IO.fonts.glyphRangesJapanese)!!
```
or [here](https://github.com/kotlin-graphics/imgui/blob/master/src/test/java/imgui/Test_lwjgl.java#L50-L51)
```java
Font font = io.getFonts().addFontFromFileTTF("extraFonts/ArialUni.ttf", 18f, new FontConfig(), io.getFonts().getGlyphRangesJapanese());
assert (font != null);
```

![Imgur](https://i.imgur.com/vul0VbT.png?1)


### References
----------

The Immediate Mode GUI paradigm may at first appear unusual to some users. This is mainly because "Retained Mode" GUIs have been so widespread and predominant. The following links can give you a better understanding about how Immediate Mode GUIs works. 
- [Johannes 'johno' Norneby's article](http://www.johno.se/book/imgui.html).
- [A presentation by Rickard Gustafsson and Johannes Algelind](http://www.cse.chalmers.se/edu/year/2011/course/TDA361/Advanced%20Computer%20Graphics/IMGUI.pdf).
- [Jari Komppa's tutorial on building an ImGui library](http://iki.fi/sol/imgui/).
- [Casey Muratori's original video that popularized the concept](https://mollyrocket.com/861).
- [Nicolas Guillemot's CppCon'16 flashtalk about Dear ImGui](https://www.youtube.com/watch?v=LSRJ1jZq90k).
- [Thierry Excoffier's Zero Memory Widget](http://perso.univ-lyon1.fr/thierry.excoffier/ZMW/).

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
    compile 'com.github.kotlin-graphics:imgui:-SNAPSHOT'
	
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

### LibGdx

On the initiative to Catvert, ImGui plays now nice also with LibGdx.

Simply follow this [short wiki](https://github.com/kotlin-graphics/imgui/wiki/Using-libGDX) on how to set it up.

### Status:

90% ported

Things to notice:

- text inputs handling, weak/bugged

- foreign languages support: at the moment tested shortly only japanese with IME support on Windows, not yet fully robust and tested

- other fonts Shortly tested

- and few other small things which escaped the demo checking

I'm looking for feedbacks to report and get fixed as much bugs as possible, don't hesitate to contribute (wiki, code, screenshots, suggestions, whatever)

To check what has been already ported and working, simply run one of the tests in Kotlin:

- [lwjgl](https://github.com/kotlin-graphics/imgui/blob/master/src/test/kotlin/imgui/test_lwjgl.kt) 
- [jogl](https://github.com/kotlin-graphics/imgui/blob/master/src/test/kotlin/imgui/test_jogl.kt)

or in Java:

- [lwjgl](https://github.com/kotlin-graphics/imgui/blob/master/src/test/java/imgui/Test_lwjgl.java) 

You should refer to those also to learn how to use the imgui library.

Ps: `DEBUG = false` to turn off debugs `println()`


### Bugs:

- copy/cut/undo/redo text
- text filters

### Credits
-------

Developed by [Omar Cornut](http://www.miracleworld.net) and every direct or indirect contributors to the GitHub. The early version of this library was developed with the support of [Media Molecule](http://www.mediamolecule.com) and first used internally on the game [Tearaway](http://tearaway.mediamolecule.com). 

Omar first discovered imgui principles at [Q-Games](http://www.q-games.com) where Atman had dropped his own simple imgui implementation in the codebase, which Omar spent quite some time improving and thinking about. It turned out that Atman was exposed to the concept directly by working with Casey. When Omar moved to Media Molecule he rewrote a new library trying to overcome the flaws and limitations of the first one he's worked with. It became this library and since then Omar has spent an unreasonable amount of time iterating on it. 

Embeds [ProggyClean.ttf](http://upperbounds.net) font by Tristan Grimmer (MIT license).

Embeds [stb_textedit.h, stb_truetype.h, stb_rectpack.h](https://github.com/nothings/stb/) by Sean Barrett (public domain).

Inspiration, feedback, and testing for early versions: Casey Muratori, Atman Binstock, Mikko Mononen, Emmanuel Briney, Stefan Kamoda, Anton Mikhailov, Matt Willis. And everybody posting feedback, questions and patches on the GitHub.
