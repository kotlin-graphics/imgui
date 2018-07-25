package imgui.imgui.demo

import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin_
import imgui.ImGui.checkbox
import imgui.ImGui.end
import imgui.ImGui.fontSize
import imgui.ImGui.logButtons
import imgui.ImGui.menuItem
import imgui.ImGui.pushItemWidth
import imgui.ImGui.sameLine
import imgui.ImGui.separator
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.showUserGuide
import imgui.ImGui.spacing
import imgui.ImGui.text
import imgui.ImGui.textWrapped
import imgui.ImGui.version
import imgui.functionalProgramming.collapsingHeader
import imgui.functionalProgramming.menu
import imgui.functionalProgramming.menuBar
import imgui.functionalProgramming.treeNode
import imgui.functionalProgramming.withWindow
import imgui.imgui.demo.showExampleApp.*
import imgui.imgui.imgui_demoDebugInformations.Companion.showExampleMenuFile
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf

object ExampleApp {

    object show {
        // Examples Apps (accessible from the "Examples" menu)
        var mainMenuBar = false
        var console = false
        var log = false
        var layout = false
        var propertyEditor = false
        var longText = false
        var autoResize = false
        var constrainedResize = false
        var simpleOverlay = false
        var windowTitles = false
        var customRendering = false

        // Dear ImGui Apps (accessible from the "Help" menu)
        var metrics = false
        var styleEditor = false
        var about = false
    }

    // Demonstrate the various window flags. Typically you would just use the default!
    var noTitlebar = false
    var noScrollbar = false
    var noMenu = false
    var noMove = false
    var noResize = false
    var noCollapse = false
    var noClose = false
    var noNav = false

    var filter = TextFilter()

    operator fun invoke(open_: KMutableProperty0<Boolean>?) {

        var open = open_

        if (show.mainMenuBar) MainMenuBar()
        if (show.console) Console(show::console)
        if (show.log) Log(show::log)
        if (show.layout) SimpleLayout(show::layout)
        if (show.propertyEditor) PropertyEditor(show::propertyEditor)
        if (show.longText) LongText(show::longText)
        if (show.autoResize) AutoResize(show::autoResize)
        if (show.constrainedResize) ConstrainedResize(show::constrainedResize)
        if (show.simpleOverlay) SimpleOverlay(show::simpleOverlay)
        if (show.windowTitles) WindowTitles(show::windowTitles)
        if (show.customRendering) CustomRendering(show::customRendering)
        if (show.metrics) ImGui.showMetricsWindow(show::metrics)
        if (show.styleEditor)
            withWindow("Style Editor", show::styleEditor) {
                StyleEditor()
            }

        if (show.about)
            withWindow("About Dear ImGui", show::about, Wf.AlwaysAutoResize.i) {
                text("JVM Dear ImGui, $version")
                separator()
                text("Original by Omar Cornut, ported by Giuseppe Barbieri and all dear imgui contributors.")
                text("Dear ImGui is licensed under the MIT License, see LICENSE for more information.")
            }

        var windowFlags = 0
        if (noTitlebar) windowFlags = windowFlags or Wf.NoTitleBar
        if (noScrollbar) windowFlags = windowFlags or Wf.NoScrollbar
        if (!noMenu) windowFlags = windowFlags or Wf.MenuBar
        if (noMove) windowFlags = windowFlags or Wf.NoMove
        if (noResize) windowFlags = windowFlags or Wf.NoResize
        if (noCollapse) windowFlags = windowFlags or Wf.NoCollapse
        if (noClose) open = null // Don't pass our bool* to Begin
        if (noNav) windowFlags = windowFlags or Wf.NoNav
        /*  We specify a default position/size in case there's no data in the .ini file. Typically this isn't required!
            We only do it to make the Demo applications a little more welcoming.         */
        setNextWindowPos(Vec2(650, 20), Cond.FirstUseEver)
        setNextWindowSize(Vec2(550, 680), Cond.FirstUseEver)

        // Main body of the Demo window starts here.
        if (!begin_("ImGui Demo", open, windowFlags)) {
            end()   // Early out if the window is collapsed, as an optimization.
            return
        }

        text("dear imgui says hello. ($version)")

        // Most "big" widgets share a common width settings by default.
        //pushItemWidth(windowWidth * 0.65f)    // Use 2/3 of the space for widgets and 1/3 for labels (default)
        // Use fixed width for labels (by passing a negative value), the rest goes to widgets. We choose a width proportional to our font size.
        pushItemWidth(fontSize * -12)

        // Menu
        menuBar {
            menu("Menu") { showExampleMenuFile() }
//            stop = true
//            println("nav window name " + g.navWindow?.rootWindow?.name)
//            println("Examples")
            menu("Examples") {
                menuItem("Main menu bar", "", ExampleApp.show::mainMenuBar)
                menuItem("Console", "", ExampleApp.show::console)
                menuItem("Log", "", ExampleApp.show::log)
                menuItem("Simple layout", "", ExampleApp.show::layout)
                menuItem("Property editor", "", ExampleApp.show::propertyEditor)
                menuItem("Long text display", "", ExampleApp.show::longText)
                menuItem("Auto-resizing window", "", ExampleApp.show::autoResize)
                menuItem("Constrained-resizing window", "", ExampleApp.show::constrainedResize)
                menuItem("Simple overlay", "", ExampleApp.show::simpleOverlay)
                menuItem("Manipulating window titles", "", ExampleApp.show::windowTitles)
                menuItem("Custom rendering", "", ExampleApp.show::customRendering)
            }
            menu("Help") {
                menuItem("Metrics", "", ExampleApp.show::metrics)
                menuItem("Style Editor", "", ExampleApp.show::styleEditor)
                menuItem("About Dear ImGui", "", ExampleApp.show::about)
            }
        }

        spacing()

        collapsingHeader("Help") {
            textWrapped("This window is being created by the ShowDemoWindow() function. Please refer to the code " +
                    "for programming reference.\n\nUser Guide:")
            showUserGuide()
        }

        collapsingHeader("Window options") {

            checkbox("No titlebar", ::noTitlebar); sameLine(150)
            checkbox("No scrollbar", ::noScrollbar); sameLine(300)
            checkbox("No menu", ::noMenu)
            checkbox("No move", ::noMove); sameLine(150)
            checkbox("No resize", ::noResize)
            checkbox("No collapse", ::noCollapse)
            checkbox("No close", ::noClose); sameLine(150)
            checkbox("No nav", ::noNav)

            treeNode("Style") { StyleEditor() }

            treeNode("Capture/Logging") {
                textWrapped("The logging API redirects all text output so you can easily capture the content of a " +
                        "window or a block. Tree nodes can be automatically expanded. You can also call LogText() to " +
                        "output directly to the log without a visual output.")
                logButtons()
            }
        }

        widgets()

        simpleLayot()

        popupsAndModalWindows()

        columns_()

        collapsingHeader("Filtering TODO") {
            //            ImGui::Text("Filter usage:\n"
//                    "  \"\"         display all lines\n"
//            "  \"xxx\"      display lines containing \"xxx\"\n"
//            "  \"xxx,yyy\"  display lines containing \"xxx\" or \"yyy\"\n"
//            "  \"-xxx\"     hide lines containing \"xxx\"");
//            filter.Draw();
//            const char* lines[] = { "aaa1.c", "bbb1.c", "ccc1.c", "aaa2.cpp", "bbb2.cpp", "ccc2.cpp", "abc.h", "hello, world" };
//            for (int i = 0; i < IM_ARRAYSIZE(lines); i++)
//            if (filter.PassFilter(lines[i]))
//                ImGui::BulletText("%s", lines[i]);
        }

        inputNavigationAndFocus()

        end()
    }
}