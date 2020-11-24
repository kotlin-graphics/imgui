package app.tests

import engine.KeyModFlag
import engine.context.*
import engine.core.TestEngine
import engine.core.registerTest
import glm_.vec2.Vec2
import imgui.*
import imgui.internal.sections.InputSource
import imgui.internal.sections.NavLayer
import io.kotest.matchers.shouldBe
import kool.getValue
import kool.setValue
import imgui.WindowFlag as Wf

//-------------------------------------------------------------------------
// Tests: Nav
//-------------------------------------------------------------------------

fun registerTests_Nav(e: TestEngine) {

    // ## Test opening a new window from a checkbox setting the focus to the new window.
    // In 9ba2028 (2019/01/04) we fixed a bug where holding ImGuiNavInputs_Activate too long on a button would hold the focus on the wrong window.
    e.registerTest("nav", "nav_basic").let { t ->
        t.testFunc = { ctx: TestContext ->
            ctx setInputMode InputSource.Nav
            ctx.windowRef("Hello, world!")
            ctx.itemUncheck("Demo Window")
            ctx.itemCheck("Demo Window")

            val g = ctx.uiContext!!
            assert(g.navWindow!!.id == ctx.getID("/Dear ImGui Demo"))
        }
    }

    // ## Test that CTRL+Tab steal active id (#2380)
    e.registerTest("nav", "nav_ctrl_tab_takes_activeid_away").let { t ->
        t.guiFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            dsl.window("Test window 1", null, Wf.NoSavedSettings.i) {
                ImGui.text("This is test window 1")
                ImGui.inputText("InputText", vars.str1)
            }
            dsl.window("Test window 2", null, Wf.NoSavedSettings.i) {
                ImGui.text("This is test window 2")
                ImGui.inputText("InputText", vars.str2)
            }
        }
        t.testFunc = { ctx: TestContext ->
            // FIXME-TESTS: Fails if window is resized too small
            ctx setInputMode InputSource.Nav
            ctx.windowRef("Test window 1")
            ctx.itemInput("InputText")
            ctx.keyCharsAppend("123")
            ctx.uiContext!!.activeId shouldBe ctx.getID("InputText")
            ctx.keyPressMap(Key.Tab, KeyModFlag.Ctrl.i)
            ctx.uiContext!!.activeId shouldBe 0
            ctx.sleep(1f)
        }
    }

    // ## Test that ESC deactivate InputText without closing current Popup (#2321, #787)
    e.registerTest("nav", "nav_esc_popup").let { t ->
        t.guiFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            var popupOpen by vars::bool1
            var fieldActive by vars::bool2
            var popupId by vars::id

            dsl.window("Test Window", null, Wf.NoSavedSettings.i) {
                if (ImGui.button("Open Popup"))
                    ImGui.openPopup("Popup")

                popupOpen = ImGui.beginPopup("Popup")
                if (popupOpen) {
                    popupId = ImGui.currentWindow.id
                    ImGui.inputText("Field", vars.str1)
                    fieldActive = ImGui.isItemActive
                    ImGui.endPopup()
                }
            }
        }
        t.testFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            val popupOpen by vars::bool1
            val fieldActive by vars::bool2

            // FIXME-TESTS: Come up with a better mechanism to get popup ID
            var popupId by vars::id
            popupId = 0

            ctx.windowRef("Test Window")
            ctx.itemClick("Open Popup")

            while (popupId == 0 && !ctx.isError)
                ctx.yield()

            ctx.windowRef(popupId)
            ctx.itemClick("Field")
            assert(popupOpen)
            assert(fieldActive)

            ctx.keyPressMap(Key.Escape)
            assert(popupOpen)
            assert(!fieldActive)
        }
    }

    // ## Test AltGr doesn't trigger menu layer
    e.registerTest("nav", "nav_altgr_no_menu").let { t ->
        t.guiFunc = {
            dsl.window("Test window", null, Wf.NoSavedSettings or Wf.MenuBar) {
                dsl.menuBar {
                    dsl.menu("Menu") {
                        ImGui.text("Blah")
                    }
                }
            }
        }
        t.testFunc = { ctx: TestContext ->
            ctx.uiContext!!.apply {
                // FIXME-TESTS: Fails if window is resized too small
                assert(io.configFlags has ConfigFlag.NavEnableKeyboard)
                //ctx->SetInputMode(ImGuiInputSource_Nav);
                ctx.windowRef("Test window")
                assert(navLayer == NavLayer.Main)
                ctx.keyPressMap(Key.Count, KeyModFlag.Alt.i)
                assert(navLayer == NavLayer.Menu)
                ctx.keyPressMap(Key.Count, KeyModFlag.Alt.i)
                assert(navLayer == NavLayer.Main)
                ctx.keyPressMap(Key.Count, KeyModFlag.Alt or KeyModFlag.Ctrl)
                assert(navLayer == NavLayer.Main)
            }
        }
    }

    // ## Test navigation home and end keys
    e.registerTest("nav", "nav_home_end_keys").let { t ->
        t.guiFunc = {
            ImGui.setNextWindowSize(Vec2(100, 150))
            dsl.window("Test Window", null, Wf.NoSavedSettings.i) {
                for (i in 0..9)
                    ImGui.button("Button $i")
            }
        }
        t.testFunc = { ctx: TestContext ->
            val ui = ctx.uiContext!!
            assert(ui.io.configFlags has ConfigFlag.NavEnableKeyboard)
            val window = ImGui.findWindowByName("Test Window")!!
            ctx.windowRef("Test window")
            ctx.setInputMode(InputSource.Nav)

            // FIXME-TESTS: This should not be required but nav init request is not applied until we start navigating, this is a workaround
            ctx.keyPressMap(Key.Count, KeyModFlag.Alt.i)
            ctx.keyPressMap(Key.Count, KeyModFlag.Alt.i)

            assert(ui.navId == window.getID("Button 0"))
            assert(window.scroll.y == 0f)
            // Navigate to the middle of window
            for (i in 0..4)
                ctx.keyPressMap(Key.DownArrow)
            assert(ui.navId == window.getID("Button 5"))
            assert(window.scroll.y > 0 && window.scroll.y < window.scrollMax.y)
            // From the middle to the end
            ctx.keyPressMap(Key.End)
            assert(ui.navId == window.getID("Button 9"))
            assert(window.scroll.y == window.scrollMax.y)
            // From the end to the start
            ctx.keyPressMap(Key.Home)
            assert(ui.navId == window.getID("Button 0"))
            assert(window.scroll.y == 0f)
        }
    }

    // ## Test vertical wrap-around in menus/popups
    e.registerTest("nav", "nav_popup_wraparound").let { t ->
        t.testFunc = { ctx: TestContext ->
            ctx.windowRef("Dear ImGui Demo")
            ctx.menuClick("Menu")
            ctx.keyPressMap(Key.Count, KeyModFlag.Alt.i)
            assert(ctx.uiContext!!.navId == ctx.getID("/##Menu_00/New"))
            ctx.navKeyPress(NavInput._KeyUp)
            assert(ctx.uiContext!!.navId == ctx.getID("/##Menu_00/Quit"))
            ctx.navKeyPress(NavInput._KeyDown)
            assert(ctx.uiContext!!.navId == ctx.getID("/##Menu_00/New"))
        }
    }

    // ## Test CTRL+TAB window focusing
    e.registerTest("nav", "nav_ctrl_tab_focusing").let { t ->
        t.guiFunc = {
            dsl.window("Window 1", null, Wf.AlwaysAutoResize.i) {
                ImGui.textUnformatted("Not empty space")
            }

            dsl.window("Window 2", null, Wf.AlwaysAutoResize.i) {
                ImGui.button("Button Out")
                dsl.child("Child", Vec2(50), true) {
                    ImGui.button("Button In")
                }
            }
        }
        t.testFunc = { ctx: TestContext ->
            val g = ctx.uiContext!!
            val window2 = ctx.getWindowByRef("Window 2")!!

            // FIXME-TESTS: Facilitate usage of variants
            val testCount = if (ctx.hasDock) 2 else 1
            for (testN in 0 until testCount) {
                ctx.logDebug("TEST CASE $testN")
//                #ifdef IMGUI_HAS_DOCK
//                    ctx->DockMultiClear("Window 1", "Window 2", NULL)
//                if (test_n == 1)
//                    ctx->DockWindowInto("Window 1", "Window 2")
//                #endif

                // Set up window focus order.
                ctx.windowFocus("Window 1")
                ctx.windowFocus("Window 2")

                ctx.keyPressMap(Key.Tab, KeyModFlag.Ctrl.i)
                assert(g.navWindow === ctx.getWindowByRef("Window 1"))
                ctx.keyPressMap(Key.Tab, KeyModFlag.Ctrl.i)
                assert(g.navWindow === ctx.getWindowByRef("Window 2"))

                // Set up window focus order, focus child window.
                ctx.windowFocus("Window 1")
                ctx.windowFocus("Window 2") // FIXME: Needed for case when docked
                ctx.itemClick("Window 2\\/Child_%08X/Button In".format(window2.getID("Child")))

                ctx.keyPressMap(Key.Tab, KeyModFlag.Ctrl.i)
                assert(g.navWindow === ctx.getWindowByRef("Window 1"))
            }
        }
    }

    // ## Test NavID restoration during CTRL+TAB focusing
    e.registerTest("nav", "nav_ctrl_tab_nav_id_restore").let { t ->
        t.guiFunc = {
            dsl.window("Window 1", null, Wf.AlwaysAutoResize.i) {
                ImGui.button("Button 1")
            }

            dsl.window("Window 2", null, Wf.AlwaysAutoResize.i) {
                dsl.child("Child", Vec2(50), true) {
                    ImGui.button("Button 2")
                }
            }
        }
        t.testFunc = { ctx: TestContext ->
            val g = ctx.uiContext!!

            // FIXME-TESTS: Facilitate usage of variants
            val testCount = if (ctx.hasDock) 2 else 1
            for (testN in 0 until testCount) {
                ctx.logDebug("TEST CASE $testN")
//                #ifdef IMGUI_HAS_DOCK
//                    ctx->DockMultiClear("Window 1", "Window 2", NULL)
//                if (test_n == 1)
//                    ctx->DockWindowInto("Window 2", "Window 1")
//                #endif

                val window2 = ctx.getWindowByRef("Window 2")!!

                val win1ButtonRef = "Window 1/Button 1"
                val win2ButtonRef = "Window 2\\/Child_%08X/Button 2".format(window2.getID("Child"))

                // Focus Window 1, navigate to the button
                ctx.windowFocus("Window 1")
                ctx.navMoveTo(win1ButtonRef)

                // Focus Window 2, ensure nav id was changed, navigate to the button
                ctx.windowFocus("Window 2")
                assert(ctx.getID(win1ButtonRef) != g.navId)
                ctx.navMoveTo(win2ButtonRef)

                // Ctrl+Tab back to previous window, check if nav id was restored
                ctx.keyPressMap(Key.Tab, KeyModFlag.Ctrl.i)
                assert(ctx.getID(win1ButtonRef) == g.navId)

                // Ctrl+Tab back to previous window, check if nav id was restored
                ctx.keyPressMap(Key.Tab, KeyModFlag.Ctrl.i)
                assert(ctx.getID(win2ButtonRef) == g.navId)
            }
        }
    }

    // ## Test NavID restoration when focusing another window or STOPPING to submit another world
    e.registerTest("nav", "nav_focus_restore").let { t ->
        t.guiFunc = { ctx: TestContext ->
            dsl.window("Window 1", null, Wf.AlwaysAutoResize.i) {
                ImGui.button("Button 1")
            }

            if (ctx.genericVars.bool1)
                dsl.window("Window 2", null, Wf.AlwaysAutoResize.i) {
                    ImGui.button("Button 2")
                }
        }
        t.testFunc = { ctx: TestContext ->
            val g = ctx.uiContext!!

            // FIXME-TESTS: Facilitate usage of variants
            val testCount = if (ctx.hasDock) 2 else 1
            for (testN in 0 until testCount) {
                ctx.logDebug("TEST CASE $testN")
                ctx.genericVars.bool1 = true
                ctx.yieldFrames(2)
//                #ifdef IMGUI_HAS_DOCK
//                    ctx->DockMultiClear("Window 1", "Window 2", NULL)
//                if (test_n == 1)
//                    ctx->DockWindowInto("Window 2", "Window 1")
//                #endif
                ctx.windowFocus("Window 1")
                ctx.navMoveTo("Window 1/Button 1")

                ctx.windowFocus("Window 2")
                ctx.navMoveTo("Window 2/Button 2")

                ctx.windowFocus("Window 1")
                assert(g.navId == ctx.getID("Window 1/Button 1"))

                ctx.windowFocus("Window 2")
                assert(g.navId == ctx.getID("Window 2/Button 2"))

                ctx.genericVars.bool1 = false
                ctx.yieldFrames(2)
                assert(g.navId == ctx.getID("Window 1/Button 1"))
            }
        }
    }

    // ## Test NavID restoration after activating menu item.
    e.registerTest("nav", "nav_focus_restore_menus").let { t ->
        t.testFunc = { ctx: TestContext ->
            val g = ctx.uiContext!!

            // FIXME-TESTS: Facilitate usage of variants
            val testCount = if(ctx.hasDock) 2 else 1
            for (testN in 0 until testCount) {
                ctx.logDebug("TEST CASE $testN")
//                #ifdef IMGUI_HAS_DOCK
//                    ctx->WindowRef(ImGuiTestRef())
//                ctx->DockMultiClear("Dear ImGui Demo", "Hello, world!", NULL)
//                if (test_n == 0)
//                    ctx->DockWindowInto("Dear ImGui Demo", "Hello, world!")
//                #endif
                ctx.windowRef("Dear ImGui Demo")
                // Focus item.
                ctx.navMoveTo("Configuration")
                // Focus menu.
                ctx.navKeyPress(NavInput.Menu)
                // Open menu, focus first item in the menu.
                ctx.navActivate()
                // Activate first item in the menu.
                ctx.navActivate()
                // Verify NavId was restored to initial value.
                assert(g.navId == ctx.getID("Configuration"))
            }
        }
    }
}