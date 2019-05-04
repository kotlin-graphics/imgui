package imgui.imgui.demo

import glm_.f
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin_
import imgui.ImGui.bulletText
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.checkboxFlags
import imgui.ImGui.end
import imgui.ImGui.fontSize
import imgui.ImGui.io
import imgui.ImGui.logButtons
import imgui.ImGui.logFinish
import imgui.ImGui.logText
import imgui.ImGui.logToClipboard
import imgui.ImGui.menuItem
import imgui.ImGui.pushItemWidth
import imgui.ImGui.sameLine
import imgui.ImGui.separator
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.showAboutWindow
import imgui.ImGui.showUserGuide
import imgui.ImGui.spacing
import imgui.ImGui.text
import imgui.ImGui.textWrapped
import imgui.ImGui.time
import imgui.ImGui.version
import imgui.functionalProgramming.collapsingHeader
import imgui.functionalProgramming.menu
import imgui.functionalProgramming.menuBar
import imgui.functionalProgramming.treeNode
import imgui.functionalProgramming.withWindow
import imgui.imgui.demo.showExampleApp.*
import imgui.imgui.imgui_demoDebugInformations.Companion.showExampleMenuFile
import imgui.imgui.imgui_demoDebugInformations.Companion.helpMarker
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf

object ExampleApp {

    object show {
        // Examples Apps (accessible from the "Examples" menu)
        var documents = false
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
    var noBackground = false
    var noBringToFront = false

    var filter = TextFilter()

    operator fun invoke(open_: KMutableProperty0<Boolean>?) {

        var open = open_

        if (show.documents) Documents(show::documents);     // Process the Document app next, as it may also use a DockSpace()
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
            showAboutWindow(show::about)

        var windowFlags = 0
        if (noTitlebar) windowFlags = windowFlags or Wf.NoTitleBar
        if (noScrollbar) windowFlags = windowFlags or Wf.NoScrollbar
        if (!noMenu) windowFlags = windowFlags or Wf.MenuBar
        if (noMove) windowFlags = windowFlags or Wf.NoMove
        if (noResize) windowFlags = windowFlags or Wf.NoResize
        if (noCollapse) windowFlags = windowFlags or Wf.NoCollapse
        if (noNav) windowFlags = windowFlags or Wf.NoNav
        if (noBackground) windowFlags = windowFlags or Wf.NoNav
        if (noBringToFront) windowFlags = windowFlags or Wf.NoBringToFrontOnFocus
        if (noClose) open = null // Don't pass our bool* to Begin
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
                menuItem("Main menu bar", "", show::mainMenuBar)
                menuItem("Console", "", show::console)
                menuItem("Log", "", show::log)
                menuItem("Simple layout", "", show::layout)
                menuItem("Property editor", "", show::propertyEditor)
                menuItem("Long text display", "", show::longText)
                menuItem("Auto-resizing window", "", show::autoResize)
                menuItem("Constrained-resizing window", "", show::constrainedResize)
                menuItem("Simple overlay", "", show::simpleOverlay)
                menuItem("Manipulating window titles", "", show::windowTitles)
                menuItem("Custom rendering", "", show::customRendering)
                menuItem("Documents", "", show::documents)
            }
            menu("Help") {
                menuItem("Metrics", "", show::metrics)
                menuItem("Style Editor", "", show::styleEditor)
                menuItem("About Dear ImGui", "", show::about)
            }
        }

        spacing()

        collapsingHeader("Help") {
            text("PROGRAMMER GUIDE:")
            bulletText("Please see the ShowDemoWindow() code in imgui_demo.cpp. <- you are here!")
            bulletText("Please see the comments in imgui.cpp.")
            bulletText("Please see the examples/ in application.")
            bulletText("Enable 'io.ConfigFlags |= NavEnableKeyboard' for keyboard controls.")
            bulletText("Enable 'io.ConfigFlags |= NavEnableGamepad' for gamepad controls.")
            separator()

            text("USER GUIDE:")
            showUserGuide()
        }

        collapsingHeader("Configuration") {

            treeNode("Configuration##2") {

                checkboxFlags("io.ConfigFlags: NavEnableKeyboard", io::configFlags, ConfigFlag.NavEnableKeyboard.i)
                checkboxFlags("io.ConfigFlags: NavEnableGamepad", io::configFlags, ConfigFlag.NavEnableGamepad.i)
                sameLine(); helpMarker("Required back-end to feed in gamepad inputs in io.NavInputs[] and set io.BackendFlags |= ImGuiBackendFlags_HasGamepad.\n\nRead instructions in imgui.cpp for details.")
                checkboxFlags("io.ConfigFlags: NavEnableSetMousePos", io::configFlags, ConfigFlag.NavEnableSetMousePos.i)
                sameLine(); helpMarker("Instruct navigation to move the mouse cursor. See comment for ImGuiConfigFlags_NavEnableSetMousePos.")
                checkboxFlags("io.ConfigFlags: NoMouse", io::configFlags, ConfigFlag.NoMouse.i)
                if (io.configFlags has ConfigFlag.NoMouse) { // Create a way to restore this flag otherwise we could be stuck completely!

                    if ((time.f % 0.4f) < 0.2f) {
                        sameLine()
                        text("<<PRESS SPACE TO DISABLE>>")
                    }
                    if (Key.Space.isPressed)
                        io.configFlags = io.configFlags wo ConfigFlag.NoMouse
                }
                checkboxFlags("io.ConfigFlags: NoMouseCursorChange", io::configFlags, ConfigFlag.NoMouseCursorChange.i)
                sameLine(); helpMarker("Instruct back-end to not alter mouse cursor shape and visibility.")
                checkbox("io.ConfigCursorBlink", io::configInputTextCursorBlink)
                sameLine(); helpMarker("Set to false to disable blinking cursor, for users who consider it distracting")
                checkbox("io.ConfigWindowsResizeFromEdges", io::configWindowsResizeFromEdges)
                sameLine(); helpMarker("Enable resizing of windows from their edges and from the lower-left corner.\nThis requires (io.BackendFlags & ImGuiBackendFlags_HasMouseCursors) because it needs mouse cursor feedback.")
                checkbox("io.configWindowsMoveFromTitleBarOnly", io::configWindowsMoveFromTitleBarOnly)
                checkbox("io.MouseDrawCursor", io::mouseDrawCursor)
                sameLine(); helpMarker("Instruct Dear ImGui to render a mouse cursor for you. Note that a mouse cursor rendered via your application GPU rendering path will feel more laggy than hardware cursor, but will be more in sync with your other visuals.\n\nSome desktop applications may use both kinds of cursors (e.g. enable software cursor only when resizing/dragging something).")
                separator()
            }
            treeNode("Backend Flags") {
                helpMarker("Those flags are set by the back-ends (imgui_impl_xxx files) to specify their capabilities.")
                val backendFlags = intArrayOf(io.backendFlags) // Make a local copy to avoid modifying the back-end flags.
                checkboxFlags("io.BackendFlags: HasGamepad", backendFlags, BackendFlag.HasGamepad.i)
                checkboxFlags("io.BackendFlags: HasMouseCursors", backendFlags, BackendFlag.HasMouseCursors.i)
                checkboxFlags("io.BackendFlags: HasSetMousePos", backendFlags, BackendFlag.HasSetMousePos.i)
                separator()
            }

            treeNode("Style") {
                StyleEditor()
                separator()
            }

            treeNode("Capture/Logging") {
                textWrapped("The logging API redirects all text output so you can easily capture the content of a window or a block. Tree nodes can be automatically expanded.")
                helpMarker("Try opening any of the contents below in this window and then click one of the \"Log To\" button.")
                logButtons()
                textWrapped("You can also call ImGui::LogText() to output directly to the log without a visual output.")
                if (button("Copy \"Hello, world!\" to clipboard")) {
                    logToClipboard()
                    logText("%s", "Hello, world!")
                    logFinish()
                }
            }
        }

        collapsingHeader("Window options") {
            checkbox("No titlebar", ::noTitlebar); sameLine(150)
            checkbox("No scrollbar", ::noScrollbar); sameLine(300)
            checkbox("No menu", ::noMenu)
            checkbox("No move", ::noMove); sameLine(150)
            checkbox("No resize", ::noResize); sameLine(300)
            checkbox("No collapse", ::noCollapse)
            checkbox("No close", ::noClose); sameLine(150)
            checkbox("No nav", ::noNav); sameLine(300)
            checkbox("No background", ::noBackground)
            checkbox("No bring to front", ::noBringToFront)
        }

        showDemoWindowWidgets()
        showDemoWindowLayout()
        showDemoWindowPopups()
        showDemoWindowColumns()


        showDemoWindowMisc()

        // End of ShowDemoWindow()
        end()
    }
}