package app.tests

import engine.context.*
import engine.core.*
import glm_.f
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import io.kotlintest.matchers.floats.shouldBeGreaterThan
import io.kotlintest.matchers.floats.shouldBeLessThan
import io.kotlintest.shouldBe
import kotlin.math.abs
import imgui.WindowFlag as Wf

//-------------------------------------------------------------------------
// Tests: Window
//-------------------------------------------------------------------------

fun registerTests_Window(e: TestEngine) {

    // ## Test size of an empty window
    e.registerTest("window", "empty").let { t ->
        t.guiFunc = {
            //ImGui::GetStyle().WindowMinSize = ImVec2(10, 10);
            ImGui.begin("Test Window", null, Wf.NoSavedSettings or Wf.AlwaysAutoResize)
            ImGui.end()
        }
        t.testFunc = {
            val style = ImGui.style
            ImGui.findWindowByName("Test Window")!!.apply {
                CHECK_EQ(size.x, max(style.windowMinSize.x.f, style.windowPadding.x * 2f))
                CHECK_EQ(size.y, ImGui.fontSize + style.framePadding.y * 2f + style.windowPadding.y * 2f)
                contentSize shouldBe Vec2()
                scroll shouldBe Vec2()
            }
        }
    }

//    // ## Test that a window starting collapsed performs width/contents size measurement on its first few frames.
//    e.registerTest("window", "window_size_collapsed_1").let { t ->
//        t.guiFunc = { ctx: TestContext ->
//            ImGui.setNextWindowCollapsed(true, Cond.Appearing)
//            ImGui.begin("Issue 2336", null, Wf.NoSavedSettings.i)
//            ImGui.text("This is some text")
//            ImGui.text("This is some more text")
//            ImGui.text("This is some more text again")
//
//            val w = ImGui.windowWidth
//            if (ctx.frameCount == 0) { // We are past the warm-up frames already
//                val expectedW = ImGui.calcTextSize("This is some more text again").x + ImGui.style.windowPadding.x * 2f
//                abs(w - expectedW) shouldBeLessThan 1f
//            }
//            ImGui.end()
//        }
//    }
//
//    e.registerTest("window", "window_size_contents").let { t ->
//        t.flags = t.flags or TestFlag.NoAutoFinish
//        t.guiFunc = { ctx: TestContext ->
//
//            val style = ImGui.style
//
//            dsl.window("Test Contents Size 1", null, Wf.AlwaysAutoResize.i) {
//                ImGui.colorButton("test", Vec4(1f, 0.4f, 0f, 1f), ColorEditFlag.NoTooltip.i, Vec2(150))
//                val window = ctx.uiContext!!.currentWindow!!
//                if (ctx.frameCount > 0)
//                    window.contentSize shouldBe Vec2(150f)
//            }
//            ImGui.setNextWindowContentSize(Vec2(150, 150))
//            dsl.window("Test Contents Size 2", null, Wf.AlwaysAutoResize.i) {
//                val window = ctx.uiContext!!.currentWindow!!
//                if (ctx.frameCount >= 0)
//                    window.contentSize shouldBe Vec2(150.0f)
//            }
//            ImGui.setNextWindowContentSize(Vec2(150))
//            ImGui.setNextWindowSize(Vec2(150) + style.windowPadding * 2f + Vec2(0f, ImGui.frameHeight))
//            dsl.window("Test Contents Size 3", null, Wf.None.i) {
//                val window = ctx.uiContext!!.currentWindow!!
//                if (ctx.frameCount >= 0) {
//                    window.scrollbar.y shouldBe false
//                    window.scrollMax.y shouldBe 0f
//                }
//            }
//            ImGui.setNextWindowContentSize(Vec2(150, 150 + 1))
//            ImGui.setNextWindowSize(Vec2(150) + style.windowPadding * 2f + Vec2(0f, ImGui.frameHeight))
//            dsl.window("Test Contents Size 4", null, Wf.None.i) {
//                val window = ctx.uiContext!!.currentWindow!!
//                if (ctx.frameCount >= 0) {
//                    window.scrollbar.y shouldBe true
//                    window.scrollMax.y shouldBe 1f
//                }
//            }
//
//            if (ctx.frameCount == 2)
//                ctx.finish()
//        }
//    }
//
//    // ## Test that non-integer size/position passed to window gets rounded down and not cause any drift.
//    e.registerTest("window", "window_size_unrounded").let { t ->
//        t.flags = t.flags or TestFlag.NoAutoFinish
//        t.guiFunc = { ctx: TestContext ->
//            // #2067
//            ImGui.setNextWindowPos(Vec2(901f, 103f), Cond.Once)
//            ImGui.setNextWindowSize(Vec2(348.48f, 400f), Cond.Once)
//            dsl.window("Issue 2067", null, Wf.NoSavedSettings.i) {
//                val pos = ImGui.windowPos
//                val size = ImGui.windowSize
//                //ctx->LogDebug("%f %f, %f %f", pos.x, pos.y, size.x, size.y);
//                pos.x shouldBe 901f; pos.y shouldBe 103f
//                size.x shouldBe 348f; size.y shouldBe 400f
//            }
//            // Test that non-rounded size constraint are not altering pos/size (#2530)
//            ImGui.setNextWindowPos(Vec2(901f, 103f), Cond.Once)
//            ImGui.setNextWindowSize(Vec2(348.48f, 400f), Cond.Once)
//            ImGui.setNextWindowSizeConstraints(Vec2(475.200012f, 0f), Vec2(475.200012f, 100.4f))
//            dsl.window("Issue 2530", null, Wf.NoSavedSettings.i) {
//                val pos = ImGui.windowPos
//                val size = ImGui.windowSize
//                //ctx->LogDebug("%f %f, %f %f", pos.x, pos.y, size.x, size.y);
//                pos shouldBe Vec2(901f, 103f)
//                size shouldBe Vec2(475f, 100f)
//            }
//            if (ctx.frameCount == 2)
//                ctx.finish()
//        }
//    }
//
//    // ## Test basic window auto resize
//    e.registerTest("window", "window_auto_resize_basic").let { t ->
//        t.guiFunc = { ctx: TestContext ->
//            // FIXME-TESTS: Ideally we'd like a variant with/without the if (Begin) here
//            ImGui.begin("Test Window", null, Wf.NoSavedSettings or Wf.AlwaysAutoResize)
//            ImGui.text("Hello World")
//            ImGui.beginChild("Child", Vec2(0, 200))
//            ImGui.endChild()
//            val sz = ImGui.windowSize
//            ImGui.end()
//            if (ctx.frameCount in 0..2) {
//                val style = ImGui.style
//                sz.x.i shouldBe (ImGui.calcTextSize("Hello World").x + style.windowPadding.x * 2f).i
//                sz.y.i shouldBe (ImGui.frameHeight + ImGui.calcTextSize("Hello World").y + style.itemSpacing.y + 200f + style.windowPadding.y * 2f).i
//            }
//        }
//    }
//
//    // ## Test that uncollapsing an auto-resizing window does not go through a frame where the window is smaller than expected
//    e.registerTest("window", "window_auto_resize_uncollapse").let { t ->
//        t.guiFunc = { ctx: TestContext ->
//            // FIXME-TESTS: Ideally we'd like a variant with/without the if (Begin) here
//            dsl.window("Test Window", null, Wf.NoSavedSettings or Wf.AlwaysAutoResize) {
//                ImGui.text("Some text\nOver two lines\nOver three lines")
//            }
//        }
//        t.testFunc = { ctx: TestContext ->
//            val window = ImGui.findWindowByName("Test Window")!!
//            ctx.opFlags = ctx.opFlags or TestOpFlag.NoAutoUncollapse
//            ctx.windowRef("Test Window")
//            ctx.windowCollapse(window, false)
//            ctx.logDebug("Size %f %f, SizeFull %f %f", window.size.x, window.size.y, window.sizeFull.x, window.sizeFull.y)
//            window.size shouldBe window.sizeFull
//            val sizeFullWhenUncollapsed = window.sizeFull
//            ctx.windowCollapse(window, true)
//            ctx.logDebug("Size %f %f, SizeFull %f %f", window.size.x, window.size.y, window.sizeFull.x, window.sizeFull.y)
//            val sizeCollapsed = window.size
//            sizeFullWhenUncollapsed.y shouldBeGreaterThan sizeCollapsed.y
//            sizeFullWhenUncollapsed shouldBe window.sizeFull
//            ctx.windowCollapse(window, false)
//            ctx.logDebug("Size %f %f, SizeFull %f %f", window.size.x, window.size.y, window.sizeFull.x, window.sizeFull.y)
//            assert(window.size.y == sizeFullWhenUncollapsed.y) { "Window should have restored to full size." }
//            ctx.yield()
//            window.size.y shouldBe sizeFullWhenUncollapsed.y
//        }
//    }
//
//    // ## Test appending multiple times to a child window (bug #2282)
//    e.registerTest("window", "window_append").let { t ->
//        t.guiFunc = { ctx: TestContext ->
//            ImGui.setNextWindowSize(Vec2(200))
//            dsl.window("Test Window", null, Wf.NoSavedSettings.i) {
//                ImGui.text("Line 1")
//            }
//            dsl.window("Test Window") {
//                ImGui.text("Line 2")
//                dsl.child("Blah", Vec2(0, 50), true) {
//                    ImGui.text("Line 3")
//                }
//                val pos1 = Vec2(ImGui.cursorScreenPos)
//                dsl.child("Blah") {
//                    ImGui.text("Line 4")
//                }
//                val pos2 = ImGui.cursorScreenPos
//                pos1 shouldBe pos2 // Append calls to BeginChild() shouldn't affect CursorPos in parent window
//                ImGui.text("Line 5")
//                val pos3 = Vec2(ImGui.cursorScreenPos)
//                dsl.child("Blah") {
//                    ImGui.text("Line 6")
//                }
//                val pos4 = ImGui.cursorScreenPos
//                pos3 shouldBe pos4 // Append calls to BeginChild() shouldn't affect CursorPos in parent window
//            }
//        }
//    }
//
//    // ## Test basic focus behavior
//    // FIXME-TESTS: This in particular when combined with Docking should be tested with and without ConfigDockingTabBarOnSingleWindows
//    e.registerTest("window", "window_focus_1").let { t ->
//        t.guiFunc = { ctx: TestContext ->
//            dsl.window("AAAA", null, Wf.NoSavedSettings.i) {}
//            dsl.window("BBBB", null, Wf.NoSavedSettings.i) {}
//            if (ctx.frameCount in 20..39 || ctx.frameCount >= 50) {
//                dsl.window("CCCC", null, Wf.NoSavedSettings.i) {}
//                dsl.window("DDDD", null, Wf.NoSavedSettings.i) {}
//            }
//        }
//        t.testFunc = { ctx: TestContext ->
//            fun id() = ctx.uiContext!!.navWindow!!.id
//            id() shouldBe ctx.getID("/BBBB")
//            ctx.yieldUntil(19)
//            id() shouldBe ctx.getID("/BBBB")
//            ctx.yieldUntil(20)
//            id() shouldBe ctx.getID("/DDDD")
//            ctx.yieldUntil(30)
//            ctx.windowFocus("/CCCC")
//            id() shouldBe ctx.getID("/CCCC")
//            ctx.yieldUntil(39)
//            ctx.yieldUntil(40)
//            id() shouldBe ctx.getID("/CCCC")
//
//            // When docked, it should NOT takes 1 extra frame to lose focus (fixed 2019/03/28)
//            ctx.yieldUntil(41)
//            id() shouldBe ctx.getID("/BBBB")
//
//            ctx.yieldUntil(49)
//            id() shouldBe ctx.getID("/BBBB")
//            ctx.yieldUntil(50)
//            id() shouldBe ctx.getID("/DDDD")
//        }
//    }
//
//    // ## Test popup focus and right-click to close popups up to a given level
//    e.registerTest("window", "window_focus_popup").let { t ->
//        t.testFunc = { ctx: TestContext ->
//            val g = ctx.uiContext!!
//            ctx.windowRef("Dear ImGui Demo")
//            ctx.itemOpen("Popups & Modal windows")
//            ctx.itemOpen("Popups")
//            ctx.itemClick("Popups/Toggle..")
//
//            val popup1 = g.navWindow!!
//            ctx.windowRef(popup1.name)
//            ctx.itemClick("Stacked Popup")
//            assert(popup1.wasActive)
//
//            val popup2 = g.navWindow!!
//            ctx.mouseMove("Bream", TestOpFlag.NoFocusWindow or TestOpFlag.NoCheckHoveredId)
//            ctx.mouseClick(1) // Close with right-click
//            assert(popup1.wasActive)
//            assert(!popup2.wasActive)
//            assert(g.navWindow === popup1)
//        }
//    }

//    // ## Test that child window correctly affect contents size based on how their size was specified.
//    t = REGISTER_TEST("window", "window_child_layout_size");
//    t->Flags |= ImGuiTestFlags_NoAutoFinish;
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings);
//        ImGui::Text("Hi");
//        ImGui::BeginChild("Child 1", ImVec2(100, 100), true);
//        ImGui::EndChild();
//        if (ctx->FrameCount == 2)
//        IM_CHECK_EQ(ctx->UiContext->CurrentWindow->ContentSize, ImVec2(100, 100 + ImGui::GetTextLineHeightWithSpacing()));
//        if (ctx->FrameCount == 2)
//        ctx->Finish();
//        ImGui::End();
//    };
//
//    // ## Test that child window outside the visible section of their parent are clipped
//    t = REGISTER_TEST("window", "window_child_clip");
//    t->Flags |= ImGuiTestFlags_NoAutoFinish;
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::SetNextWindowSize(ImVec2(200, 200));
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings);
//        ImGui::SetCursorPos(ImVec2(0, 0));
//        ImGui::BeginChild("Child 1", ImVec2(100, 100));
//        if (ctx->FrameCount == 2)
//        IM_CHECK_EQ(ctx->UiContext->CurrentWindow->SkipItems, false);
//        ImGui::EndChild();
//        ImGui::SetCursorPos(ImVec2(300, 300));
//        ImGui::BeginChild("Child 2", ImVec2(100, 100));
//        if (ctx->FrameCount == 2)
//        IM_CHECK_EQ(ctx->UiContext->CurrentWindow->SkipItems, true);
//        ImGui::EndChild();
//        ImGui::End();
//        if (ctx->FrameCount == 2)
//        ctx->Finish();
//    };
//
//    // ## Test that basic SetScrollHereY call scrolls all the way (#1804)
//    // ## Test expected value of ScrollMaxY
//    t = REGISTER_TEST("window", "window_scroll_001");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::SetNextWindowSize(ImVec2(0.0f, 500));
//        ImGui::Begin("Test Scrolling", NULL, ImGuiWindowFlags_NoSavedSettings);
//        for (int n = 0; n < 100; n++)
//        ImGui::Text("Hello %d\n", n);
//        ImGui::SetScrollHereY(0.0f);
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiWindow* window = ImGui::FindWindowByName("Test Scrolling");
//        ImGuiStyle& style = ImGui::GetStyle();
//
//        IM_CHECK(window->ContentSize.y > 0.0f);
//        float scroll_y = window->Scroll.y;
//        float scroll_max_y = window->ScrollMax.y;
//        ctx->LogDebug("scroll_y = %f", scroll_y);
//        ctx->LogDebug("scroll_max_y = %f", scroll_max_y);
//        ctx->LogDebug("window->SizeContents.y = %f", window->ContentSize.y);
//
//        IM_CHECK_NO_RET(scroll_y > 0.0f);
//        IM_CHECK_NO_RET(scroll_y == scroll_max_y);
//
//        float expected_size_contents_y = 100 * ImGui::GetTextLineHeightWithSpacing() - style.ItemSpacing.y; // Newer definition of SizeContents as per 1.71
//        IM_CHECK(FloatEqual(window->ContentSize.y, expected_size_contents_y));
//
//        float expected_scroll_max_y = expected_size_contents_y + window->WindowPadding.y * 2.0f - window->InnerRect.GetHeight();
//        IM_CHECK(FloatEqual(scroll_max_y, expected_scroll_max_y));
//    };
//
//    // ## Test that ScrollMax values are correctly zero (we had/have bugs where they are seldomly == BorderSize)
//    t = REGISTER_TEST("window", "window_scroll_002");
//    t->Flags |= ImGuiTestFlags_NoAutoFinish;
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::Begin("Test Scrolling 1", NULL, ImGuiWindowFlags_AlwaysHorizontalScrollbar | ImGuiWindowFlags_AlwaysAutoResize);
//        ImGui::Dummy(ImVec2(200, 200));
//        ImGuiWindow* window1 = ctx->UiContext->CurrentWindow;
//        IM_CHECK_NO_RET(window1->ScrollMax.x == 0.0f); // FIXME-TESTS: If another window in another test used same name, ScrollMax won't be zero on first frame
//        IM_CHECK_NO_RET(window1->ScrollMax.y == 0.0f);
//        ImGui::End();
//
//        ImGui::Begin("Test Scrolling 2", NULL, ImGuiWindowFlags_AlwaysVerticalScrollbar | ImGuiWindowFlags_AlwaysAutoResize);
//        ImGui::Dummy(ImVec2(200, 200));
//        ImGuiWindow* window2 = ctx->UiContext->CurrentWindow;
//        IM_CHECK_NO_RET(window2->ScrollMax.x == 0.0f);
//        IM_CHECK_NO_RET(window2->ScrollMax.y == 0.0f);
//        ImGui::End();
//        if (ctx->FrameCount == 2)
//        ctx->Finish();
//    };
//
//    // ## Test that SetScrollY/GetScrollY values are matching. You'd think this would be obvious! Think again!
//    // FIXME-TESTS: With/without menu bars, could we easily allow for test variations that affects both GuiFunc and TestFunc
//    t = REGISTER_TEST("window", "window_scroll_003");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::Begin("Test Scrolling 3");
//        for (int n = 0; n < 100; n++)
//        ImGui::Text("Line %d", n);
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ctx->Yield();
//        ImGuiWindow* window = ImGui::FindWindowByName("Test Scrolling 3");
//        ImGui::SetScrollY(window, 100.0f);
//        ctx->Yield();
//        float sy = window->Scroll.y;
//        IM_CHECK_EQ(sy, 100.0f);
//    };
//
//    // ## Test that an auto-fit window doesn't have scrollbar while resizing (FIXME-TESTS: Also test non-zero ScrollMax when implemented)
//    t = REGISTER_TEST("window", "window_scroll_while_resizing");
//    t->Flags |= ImGuiTestFlags_NoAutoFinish;
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::Begin("Test Scrolling", NULL, ImGuiWindowFlags_NoSavedSettings);
//        ImGui::Text("Below is a child window");
//        ImGui::BeginChild("blah", ImVec2(0, 0), false, ImGuiWindowFlags_HorizontalScrollbar);
//        ImGuiWindow* child_window = ctx->UiContext->CurrentWindow;
//        ImGui::EndChild();
//        IM_CHECK(child_window->ScrollbarY == false);
//        if (ctx->FrameCount >= ctx->FirstFrameCount)
//        {
//            ImGuiWindow* window = ctx->UiContext->CurrentWindow;
//            IM_CHECK(window->ScrollbarY == false);
//            //IM_CHECK(window->ScrollMax.y == 0.0f);    // FIXME-TESTS: 1.71 I would like to make this change but unsure of side effects yet
//            if (ctx->FrameCount == 2)
//            ctx->Finish();
//        }
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ctx->WindowResize("Test Scrolling", ImVec2(400, 400));
//        ctx->WindowResize("Test Scrolling", ImVec2(100, 100));
//    };
//
//    // ## Test window moving
//    t = REGISTER_TEST("window", "window_move");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::SetNextWindowSize(ImVec2(0, 0));
//        ImGui::Begin("Movable Window");
//        ImGui::TextUnformatted("Lorem ipsum dolor sit amet");
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiWindow* window = ctx->GetWindowByRef("Movable Window");
//        ctx->WindowMove("Movable Window", ImVec2(0, 0));
//        IM_CHECK(window->Pos == ImVec2(0, 0));
//        ctx->WindowMove("Movable Window", ImVec2(100, 0));
//        IM_CHECK(window->Pos == ImVec2(100, 0));
//        ctx->WindowMove("Movable Window", ImVec2(50, 100));
//        IM_CHECK(window->Pos == ImVec2(50, 100));
//    };
//
//    // ## Test closing current popup
//    // FIXME-TESTS: Test left-click/right-click forms of closing popups
//    t = REGISTER_TEST("window", "window_close_current_popup");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::SetNextWindowSize(ImVec2(0, 0));
//        ImGui::Begin("Popups", NULL, ImGuiWindowFlags_MenuBar);
//        if (ImGui::BeginMenuBar())
//        {
//            if (ImGui::BeginMenu("Menu"))
//            {
//                if (ImGui::BeginMenu("Submenu"))
//                {
//                    if (ImGui::MenuItem("Close"))
//                        ImGui::CloseCurrentPopup();
//                    ImGui::EndMenu();
//                }
//                ImGui::EndMenu();
//            }
//            ImGui::EndMenuBar();
//        }
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ctx->WindowRef("Popups");
//        IM_CHECK(ctx->UiContext->OpenPopupStack.size() == 0);
//        ctx->MenuClick("Menu");
//        IM_CHECK(ctx->UiContext->OpenPopupStack.size() == 1);
//        ctx->MenuClick("Menu/Submenu");
//        IM_CHECK(ctx->UiContext->OpenPopupStack.size() == 2);
//        ctx->MenuClick("Menu/Submenu/Close");
//        IM_CHECK(ctx->UiContext->OpenPopupStack.size() == 0);
//    };
}