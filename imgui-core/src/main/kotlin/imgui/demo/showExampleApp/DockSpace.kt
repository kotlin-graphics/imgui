package imgui.demo.showExampleApp

import imgui.*
import imgui.ImGui.pushStyleVar
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setNextWindowViewport
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

    operator fun invoke(pOpen: KMutableProperty0<Boolean>) {

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
        ImGui::PushStyleVar(ImGuiStyleVar_WindowPadding, ImVec2(0.0f, 0.0f))
        ImGui::Begin("DockSpace Demo", p_open, window_flags)
        ImGui::PopStyleVar()

        if (optFullscreen)
            ImGui::PopStyleVar(2)

        // DockSpace
        ImGuiIO& io = ImGui::GetIO()
        if (io.ConfigFlags & ImGuiConfigFlags_DockingEnable)
        {
            ImGuiID dockspace_id = ImGui::GetID("MyDockSpace")
            ImGui::DockSpace(dockspace_id, ImVec2(0.0f, 0.0f), dockspace_flags)
        }
        else
        {
            ShowDockingDisabledMessage()
        }

        if (ImGui::BeginMenuBar())
        {
            if (ImGui::BeginMenu("Docking"))
            {
                // Disabling fullscreen would allow the window to be moved to the front of other windows,
                // which we can't undo at the moment without finer window depth/z control.
                //ImGui::MenuItem("Fullscreen", NULL, &opt_fullscreen_persistant);

                if (ImGui::MenuItem("Flag: NoSplit",                "", (dockspace_flags & ImGuiDockNodeFlags_NoSplit) != 0))                 dockspace_flags ^= ImGuiDockNodeFlags_NoSplit
                if (ImGui::MenuItem("Flag: NoResize",               "", (dockspace_flags & ImGuiDockNodeFlags_NoResize) != 0))                dockspace_flags ^= ImGuiDockNodeFlags_NoResize
                if (ImGui::MenuItem("Flag: NoDockingInCentralNode", "", (dockspace_flags & ImGuiDockNodeFlags_NoDockingInCentralNode) != 0))  dockspace_flags ^= ImGuiDockNodeFlags_NoDockingInCentralNode
                if (ImGui::MenuItem("Flag: PassthruCentralNode",    "", (dockspace_flags & ImGuiDockNodeFlags_PassthruCentralNode) != 0))     dockspace_flags ^= ImGuiDockNodeFlags_PassthruCentralNode
                if (ImGui::MenuItem("Flag: AutoHideTabBar",         "", (dockspace_flags & ImGuiDockNodeFlags_AutoHideTabBar) != 0))          dockspace_flags ^= ImGuiDockNodeFlags_AutoHideTabBar
                ImGui::Separator()
                if (ImGui::MenuItem("Close DockSpace", NULL, false, p_open != NULL))
                *p_open = false
                ImGui::EndMenu()
            }
            HelpMarker(
                    "When docking is enabled, you can ALWAYS dock MOST window into another! Try it now!" "\n\n"
                    " > if io.ConfigDockingWithShift==false (default):" "\n"
                    "   drag windows from title bar to dock" "\n"
                    " > if io.ConfigDockingWithShift==true:" "\n"
                    "   drag windows from anywhere and hold Shift to dock" "\n\n"
                    "This demo app has nothing to do with it!" "\n\n"
                    "This demo app only demonstrate the use of ImGui::DockSpace() which allows you to manually create a docking node _within_ another window. This is useful so you can decorate your main application window (e.g. with a menu bar)." "\n\n"
                    "ImGui::DockSpace() comes with one hard constraint: it needs to be submitted _before_ any window which may be docked into it. Therefore, if you use a dock spot as the central point of your application, you'll probably want it to be part of the very first window you are submitting to imgui every frame." "\n\n"
                    "(NB: because of this constraint, the implicit \"Debug\" window can not be docked into an explicit DockSpace() node, because that window is submitted as part of the NewFrame() call. An easy workaround is that you can create your own implicit \"Debug##2\" window after calling DockSpace() and leave it in the window stack for anyone to use.)"
            )

            ImGui::EndMenuBar()
        }

        ImGui::End()
    }
}