package imgui.demo.showExampleApp

import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.beginMenu
import imgui.ImGui.beginMenuBar
import imgui.ImGui.dockSpace
import imgui.ImGui.endMenu
import imgui.ImGui.endMenuBar
import imgui.ImGui.getID
import imgui.ImGui.io
import imgui.ImGui.menuItem
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushStyleVar
import imgui.ImGui.sameLine
import imgui.ImGui.separator
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setNextWindowViewport
import imgui.ImGui.smallButton
import imgui.ImGui.text
import imgui.api.demoDebugInformations.Companion.helpMarker
import kotlin.reflect.KMutableProperty0
import imgui.WindowFlag as Wf

//-----------------------------------------------------------------------------
// [SECTION] Example App: Docking, DockSpace / ShowExampleAppDockSpace()
//-----------------------------------------------------------------------------

// Demonstrate using DockSpace() to create an explicit docking node within an existing window.
// Note that you already dock windows into each others _without_ a DockSpace() by just moving windows
// from their title bar (or by holding SHIFT if io.ConfigDockingWithShift is set).
// DockSpace() is only useful to construct to a central location for your application.
object DockSpace {

    var optFullscreenPersistant = true
    var dockspaceFlags = DockNodeFlag.None.i

    operator fun invoke(pOpen: KMutableProperty0<Boolean>?) {

        var optFullscreen = optFullscreenPersistant

        // We are using the ImGuiWindowFlags_NoDocking flag to make the parent window not dockable into,
        // because it would be confusing to have two docking targets within each others.
        var windowFlags = Wf.MenuBar or Wf.NoDocking
        if (optFullscreen) {
            val viewport = ImGui.mainViewport
            setNextWindowPos(viewport.workPos)
            setNextWindowSize(viewport.workSize)
            setNextWindowViewport(viewport.id)
            pushStyleVar(StyleVar.WindowRounding, 0f)
            pushStyleVar(StyleVar.WindowBorderSize, 0f)
            windowFlags = windowFlags or Wf.NoTitleBar or Wf.NoCollapse or Wf.NoResize or Wf.NoMove
            windowFlags = windowFlags or Wf.NoBringToFrontOnFocus or Wf.NoNavFocus
        }

        // When using ImGuiDockNodeFlags_PassthruCentralNode, DockSpace() will render our background
        // and handle the pass-thru hole, so we ask Begin() to not render a background.
        if (dockspaceFlags has DockNodeFlag.PassthruCentralNode)
            windowFlags = windowFlags or Wf.NoBackground

        // Important: note that we proceed even if Begin() returns false (aka window is collapsed).
        // This is because we want to keep our DockSpace() active. If a DockSpace() is inactive,
        // all active windows docked into it will lose their parent and become undocked.
        // We cannot preserve the docking relationship between an active window and an inactive docking, otherwise
        // any change of dockspace/settings would lead to windows being stuck in limbo and never being visible.
        pushStyleVar(StyleVar.WindowPadding, Vec2(0f))
        begin("DockSpace Demo", pOpen, windowFlags)
        popStyleVar()

        if (optFullscreen)
            popStyleVar(2)

        // DockSpace
        if (io.configFlags has ConfigFlag.DockingEnable) {
            val dockspaceID = getID("MyDockSpace")
            dockSpace(dockspaceID, Vec2(0f), dockspaceFlags)
        } else
            showDockingDisabledMessage()

        if (beginMenuBar()) {
            if (beginMenu("Docking")) {
                // Disabling fullscreen would allow the window to be moved to the front of other windows,
                // which we can't undo at the moment without finer window depth/z control.
                //ImGui::MenuItem("Fullscreen", NULL, &opt_fullscreen_persistant);

                if (menuItem("Flag: NoSplit", "", dockspaceFlags has DockNodeFlag.NoSplit)) dockspaceFlags = dockspaceFlags xor DockNodeFlag.NoSplit
                if (menuItem("Flag: NoResize", "", dockspaceFlags has DockNodeFlag.NoResize)) dockspaceFlags = dockspaceFlags xor DockNodeFlag.NoResize
                if (menuItem("Flag: NoDockingInCentralNode", "", dockspaceFlags has DockNodeFlag.NoDockingInCentralNode)) dockspaceFlags = dockspaceFlags xor DockNodeFlag.NoDockingInCentralNode
                if (menuItem("Flag: PassthruCentralNode", "", dockspaceFlags has DockNodeFlag.PassthruCentralNode)) dockspaceFlags = dockspaceFlags xor DockNodeFlag.PassthruCentralNode
                if (menuItem("Flag: AutoHideTabBar", "", dockspaceFlags has DockNodeFlag.AutoHideTabBar)) dockspaceFlags = dockspaceFlags xor DockNodeFlag.AutoHideTabBar
                separator()
                if (menuItem("Close DockSpace", "", false, pOpen != null))
                    pOpen!!.set(false)
                endMenu()
            }
            helpMarker(
                    "When docking is enabled, you can ALWAYS dock MOST window into another! Try it now!\n\n" +
                    " > if io.ConfigDockingWithShift==false (default):\n" +
                    "   drag windows from title bar to dock\n" +
                    " > if io.ConfigDockingWithShift==true:\n" +
                    "   drag windows from anywhere and hold Shift to dock\n\n" +
                    "This demo app has nothing to do with it!\n\n" +
                    "This demo app only demonstrate the use of ImGui::DockSpace() which allows you to manually create a docking node _within_ another window. This is useful so you can decorate your main application window (e.g. with a menu bar).\n\n" +
                    "ImGui::DockSpace() comes with one hard constraint: it needs to be submitted _before_ any window which may be docked into it. Therefore, if you use a dock spot as the central point of your application, you'll probably want it to be part of the very first window you are submitting to imgui every frame.\n\n" +
                    "(NB: because of this constraint, the implicit \"Debug\" window can not be docked into an explicit DockSpace() node, because that window is submitted as part of the NewFrame() call. An easy workaround is that you can create your own implicit \"Debug##2\" window after calling DockSpace() and leave it in the window stack for anyone to use.)")

            endMenuBar()
        }

        endMenuBar()
    }

    fun showDockingDisabledMessage() {
        text("ERROR: Docking is not enabled! See Demo > Configuration.")
        text("Set io.ConfigFlags |= ImGuiConfigFlags_DockingEnable in your code, or ")
        sameLine(0f, 0f)
        if (smallButton("click here"))
            io.configFlags = io.configFlags or ConfigFlag.DockingEnable
    }
}