package app.tests

import engine.context.TestContext
import engine.context.finish
import engine.core.*
import glm_.f
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import io.kotlintest.matchers.floats.shouldBeLessThan
import io.kotlintest.shouldBe
import kotlin.math.abs
import imgui.WindowFlag as Wf

//-------------------------------------------------------------------------
// Tests: Window
//-------------------------------------------------------------------------

fun registerTests_Window(e: TestEngine) {

    var t: Test

    // ## Test size of an empty window
    t = e.registerTest("window", "empty")
    t.guiFunc = { _: TestContext ->
        //ImGui::GetStyle().WindowMinSize = ImVec2(10, 10);
        ImGui.begin("Test Window", null, Wf.NoSavedSettings or Wf.AlwaysAutoResize)
        ImGui.end()
    }

    t.testFunc = { ctx: TestContext ->
        val style = ImGui.style
        ImGui.findWindowByName("Test Window")!!.apply {
            CHECK_EQ(size.x, max(style.windowMinSize.x.f, style.windowPadding.x * 2f))
            CHECK_EQ(size.y, ImGui.fontSize + style.framePadding.y * 2f + style.windowPadding.y * 2f)
            contentSize shouldBe Vec2()
            scroll shouldBe Vec2()
        }
    }

    // ## Test that a window starting collapsed performs width/contents size measurement on its first few frames.
    t = e.registerTest("window", "window_size_collapsed_1")
    t.guiFunc = { ctx: TestContext ->
        ImGui.setNextWindowCollapsed(true, Cond.Appearing)
        ImGui.begin("Issue 2336", null, Wf.NoSavedSettings.i)
        ImGui.text("This is some text")
        ImGui.text("This is some more text")
        ImGui.text("This is some more text again")

        val w = ImGui.windowWidth
        if (ctx.frameCount == 0) { // We are past the warm-up frames already
            val expectedW = ImGui.calcTextSize("This is some more text again").x + ImGui.style.windowPadding.x * 2f
            abs(w - expectedW) shouldBeLessThan 1f
        }
        ImGui.end()
    }

    t = e.registerTest("window", "window_size_contents")
    t.flags = t.flags or TestFlag.NoAutoFinish
    t.guiFunc = { ctx: TestContext ->

        val style = ImGui.style

        dsl.window("Test Contents Size 1", null, Wf.AlwaysAutoResize.i) {
            ImGui.colorButton("test", Vec4(1f, 0.4f, 0f, 1f), ColorEditFlag.NoTooltip.i, Vec2(150))
            val window = ctx.uiContext!!.currentWindow!!
            if (ctx.frameCount > 0)
                window.contentSize shouldBe Vec2(150f)
        }
        ImGui.setNextWindowContentSize(Vec2(150, 150))
        dsl.window("Test Contents Size 2", null, Wf.AlwaysAutoResize.i) {
            val window = ctx.uiContext!!.currentWindow!!
            if (ctx.frameCount >= 0)
                window.contentSize shouldBe Vec2( 150.0f)
        }
        ImGui.setNextWindowContentSize(Vec2(150))
        ImGui.setNextWindowSize(Vec2(150) + style.windowPadding * 2f + Vec2(0f, ImGui.frameHeight))
        dsl.window("Test Contents Size 3", null, Wf.None.i) {
            val window = ctx.uiContext!!.currentWindow!!
            if (ctx.frameCount >= 0)            {
                window.scrollbar.y shouldBe false
                window.scrollMax.y shouldBe 0f
            }
        }
        ImGui.setNextWindowContentSize(Vec2(150, 150 + 1))
        ImGui.setNextWindowSize(Vec2(150) + style.windowPadding * 2f + Vec2(0f, ImGui.frameHeight))
        dsl.window("Test Contents Size 4", null, Wf.None.i) {
            val window = ctx.uiContext!!.currentWindow!!
            if (ctx.frameCount >= 0)            {
                window.scrollbar.y shouldBe true
                window.scrollMax.y shouldBe 1f
            }
        }

        if (ctx.frameCount == 2)
            ctx.finish()
    }

    // ## Test that non-integer size/position passed to window gets rounded down and not cause any drift.
    t = e.registerTest("window", "window_size_unrounded")
    t.flags = t.flags or TestFlag.NoAutoFinish
    t.guiFunc = { ctx: TestContext ->
        // #2067
        ImGui.setNextWindowPos(Vec2(901f, 103f), Cond.Once)
        ImGui.setNextWindowSize(Vec2(348.48f, 400f), Cond.Once)
        dsl.window("Issue 2067", null, Wf.NoSavedSettings.i) {
            val pos = ImGui.windowPos
            val size = ImGui.windowSize
            //ctx->LogDebug("%f %f, %f %f", pos.x, pos.y, size.x, size.y);
            pos.x shouldBe 901f; pos.y shouldBe 103f
            size.x shouldBe 348f; size.y shouldBe 400f
        }
        // Test that non-rounded size constraint are not altering pos/size (#2530)
        ImGui.setNextWindowPos(Vec2(901f, 103f), Cond.Once)
        ImGui.setNextWindowSize(Vec2(348.48f, 400f), Cond.Once)
        ImGui.setNextWindowSizeConstraints(Vec2(475.200012f, 0f), Vec2(475.200012f, 100.4f))
        dsl.window("Issue 2530", null, Wf.NoSavedSettings.i) {
            val pos = ImGui.windowPos
            val size = ImGui.windowSize
            //ctx->LogDebug("%f %f, %f %f", pos.x, pos.y, size.x, size.y);
            pos shouldBe Vec2(901f, 103f)
            size shouldBe Vec2(475f, 100f)
        }
        if (ctx.frameCount == 2)
            ctx.finish()
    }

//    // ## Test basic window auto resize
//    t = REGISTER_TEST("window", "window_auto_resize_basic");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        // FIXME-TESTS: Ideally we'd like a variant with/without the if (Begin) here
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_AlwaysAutoResize);
//        ImGui::Text("Hello World");
//        ImGui::BeginChild("Child", ImVec2(0, 200));
//        ImGui::EndChild();
//        ImVec2 sz = ImGui::GetWindowSize();
//        ImGui::End();
//        if (ctx->FrameCount >= 0 && ctx->FrameCount <= 2)
//        {
//            ImGuiStyle& style = ImGui::GetStyle();
//            IM_CHECK_EQ((int)sz.x, (int)(ImGui::CalcTextSize("Hello World").x + style.WindowPadding.x * 2.0f));
//            IM_CHECK_EQ((int)sz.y, (int)(ImGui::GetFrameHeight() + ImGui::CalcTextSize("Hello World").y + style.ItemSpacing.y + 200.0f + style.WindowPadding.y * 2.0f));
//        }
//    };
//
//    // ## Test that uncollapsing an auto-resizing window does not go through a frame where the window is smaller than expected
//    t = REGISTER_TEST("window", "window_auto_resize_uncollapse");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        // FIXME-TESTS: Ideally we'd like a variant with/without the if (Begin) here
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_AlwaysAutoResize);
//        ImGui::Text("Some text\nOver two lines\nOver three lines");
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiWindow* window = ImGui::FindWindowByName("Test Window");
//        ctx->OpFlags |= ImGuiTestOpFlags_NoAutoUncollapse;
//        ctx->WindowRef("Test Window");
//        ctx->WindowCollapse(window, false);
//        ctx->LogDebug("Size %f %f, SizeFull %f %f", window->Size.x, window->Size.y, window->SizeFull.x, window->SizeFull.y);
//        IM_CHECK_EQ(window->Size, window->SizeFull);
//        ImVec2 size_full_when_uncollapsed = window->SizeFull;
//        ctx->WindowCollapse(window, true);
//        ctx->LogDebug("Size %f %f, SizeFull %f %f", window->Size.x, window->Size.y, window->SizeFull.x, window->SizeFull.y);
//        ImVec2 size_collapsed = window->Size;
//        IM_CHECK_GT(size_full_when_uncollapsed.y, size_collapsed.y);
//        IM_CHECK_EQ(size_full_when_uncollapsed, window->SizeFull);
//        ctx->WindowCollapse(window, false);
//        ctx->LogDebug("Size %f %f, SizeFull %f %f", window->Size.x, window->Size.y, window->SizeFull.x, window->SizeFull.y);
//        IM_CHECK(window->Size.y == size_full_when_uncollapsed.y && "Window should have restored to full size.");
//        ctx->Yield();
//        IM_CHECK_EQ(window->Size.y, size_full_when_uncollapsed.y);
//    };
//
//    // ## Test appending multiple times to a child window (bug #2282)
//    t = REGISTER_TEST("window", "window_append");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::SetNextWindowSize(ImVec2(200, 200));
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings);
//        ImGui::Text("Line 1");
//        ImGui::End();
//        ImGui::Begin("Test Window");
//        ImGui::Text("Line 2");
//        ImGui::BeginChild("Blah", ImVec2(0, 50), true);
//        ImGui::Text("Line 3");
//        ImGui::EndChild();
//        ImVec2 pos1 = ImGui::GetCursorScreenPos();
//        ImGui::BeginChild("Blah");
//        ImGui::Text("Line 4");
//        ImGui::EndChild();
//        ImVec2 pos2 = ImGui::GetCursorScreenPos();
//        IM_CHECK_EQ(pos1, pos2); // Append calls to BeginChild() shouldn't affect CursorPos in parent window
//        ImGui::Text("Line 5");
//        ImVec2 pos3 = ImGui::GetCursorScreenPos();
//        ImGui::BeginChild("Blah");
//        ImGui::Text("Line 6");
//        ImGui::EndChild();
//        ImVec2 pos4 = ImGui::GetCursorScreenPos();
//        IM_CHECK_EQ(pos3, pos4); // Append calls to BeginChild() shouldn't affect CursorPos in parent window
//        ImGui::End();
//    };
//
//    // ## Test basic focus behavior
//    // FIXME-TESTS: This in particular when combined with Docking should be tested with and without ConfigDockingTabBarOnSingleWindows
//    t = REGISTER_TEST("window", "window_focus_1");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::Begin("AAAA", NULL, ImGuiWindowFlags_NoSavedSettings);
//        ImGui::End();
//        ImGui::Begin("BBBB", NULL, ImGuiWindowFlags_NoSavedSettings);
//        ImGui::End();
//        if ((ctx->FrameCount >= 20 && ctx->FrameCount < 40) || (ctx->FrameCount >= 50))
//        {
//            ImGui::Begin("CCCC", NULL, ImGuiWindowFlags_NoSavedSettings);
//            ImGui::End();
//            ImGui::Begin("DDDD", NULL, ImGuiWindowFlags_NoSavedSettings);
//            ImGui::End();
//        }
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiContext& g = *ctx->UiContext;
//        IM_CHECK_EQ(g.NavWindow->ID, ctx->GetID("/BBBB"));
//        ctx->YieldUntil(19);
//        IM_CHECK_EQ(g.NavWindow->ID, ctx->GetID("/BBBB"));
//        ctx->YieldUntil(20);
//        IM_CHECK_EQ(g.NavWindow->ID, ctx->GetID("/DDDD"));
//        ctx->YieldUntil(30);
//        ctx->WindowFocus("/CCCC");
//        IM_CHECK_EQ(g.NavWindow->ID, ctx->GetID("/CCCC"));
//        ctx->YieldUntil(39);
//        ctx->YieldUntil(40);
//        IM_CHECK_EQ(g.NavWindow->ID, ctx->GetID("/CCCC"));
//
//        // When docked, it should NOT takes 1 extra frame to lose focus (fixed 2019/03/28)
//        ctx->YieldUntil(41);
//        IM_CHECK_EQ(g.NavWindow->ID, ctx->GetID("/BBBB"));
//
//        ctx->YieldUntil(49);
//        IM_CHECK_EQ(g.NavWindow->ID, ctx->GetID("/BBBB"));
//        ctx->YieldUntil(50);
//        IM_CHECK_EQ(g.NavWindow->ID, ctx->GetID("/DDDD"));
//    };
//
//    // ## Test popup focus and right-click to close popups up to a given level
//    t = REGISTER_TEST("window", "window_focus_popup");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiContext& g = *ctx->UiContext;
//        ctx->WindowRef("Dear ImGui Demo");
//        ctx->ItemOpen("Popups & Modal windows");
//        ctx->ItemOpen("Popups");
//        ctx->ItemClick("Popups/Toggle..");
//
//        ImGuiWindow* popup_1 = g.NavWindow;
//        ctx->WindowRef(popup_1->Name);
//        ctx->ItemClick("Stacked Popup");
//        IM_CHECK(popup_1->WasActive);
//
//        ImGuiWindow* popup_2 = g.NavWindow;
//        ctx->MouseMove("Bream", ImGuiTestOpFlags_NoFocusWindow | ImGuiTestOpFlags_NoCheckHoveredId);
//        ctx->MouseClick(1); // Close with right-click
//        IM_CHECK(popup_1->WasActive);
//        IM_CHECK(!popup_2->WasActive);
//        IM_CHECK(g.NavWindow == popup_1);
//    };
//
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