package app.tests

import engine.context.*
import engine.core.TestEngine
import engine.core.registerTest
import glm_.vec2.Vec2
import imgui.ImGui
import imgui.Key

//-------------------------------------------------------------------------
// Tests: Capture
//-------------------------------------------------------------------------

fun registerTests_Capture(e: TestEngine) {

    e.registerTest("capture", "capture_demo_documents").let { t ->
        t.testFunc = { ctx: TestContext ->

            ctx.windowRef("Dear ImGui Demo")
            ctx.menuAction(TestAction.Check, "Examples/Documents")

            ctx.windowRef("Example: Documents")
            // FIXME-TESTS: Locate within stack that uses windows/<pointer>/name
            //ctx->ItemCheck("Tomato"); // FIXME: WILL FAIL, NEED TO LOCATE BY NAME (STACK WILDCARD?)
            //ctx->ItemCheck("A Rather Long Title"); // FIXME: WILL FAIL
            //ctx->ItemClick("##tabs/Eggplant");
            //ctx->MouseMove("##tabs/Eggplant/Modify");
            ctx.sleep(1f)
        }
    }

//    #if 1
    // TODO: Better position of windows.
    // TODO: Draw in custom rendering canvas
    // TODO: Select color picker mode
    // TODO: Flags document as "Modified"
    // TODO: InputText selection
    e.registerTest("capture", "capture_readme_misc").let { t ->
        t.testFunc = { ctx: TestContext ->

            val io = ImGui.io
            //ImGuiStyle& style = ImGui::GetStyle();

            ctx.windowRef("Dear ImGui Demo")
            ctx.itemCloseAll("")
            ctx.menuCheck("Examples/Simple overlay")
            ctx.windowRef("Example: Simple overlay")
            val windowOverlay = ctx.getWindowByRef("")!!
//        IM_CHECK(windowOverlay != NULL)

            // FIXME-TESTS: Find last newly opened window?

            val fh = ImGui.fontSize
            var pad = fh

            ctx.windowRef("Dear ImGui Demo")
            ctx.menuCheck("Examples/Custom rendering")
            ctx.windowRef("Example: Custom rendering")
            ctx.windowResize("", Vec2(fh * 30))
            ctx.windowMove("", windowOverlay.rect().bl + Vec2(0f, pad))
            val windowCustomRendering = ctx.getWindowByRef("")!!
//        IM_CHECK(windowCustomRendering != NULL)

            ctx.windowRef("Dear ImGui Demo")
            ctx.menuCheck("Examples/Simple layout")
            ctx.windowRef("Example: Simple layout")
            ctx.windowResize("", Vec2(fh * 50, fh * 15))
            ctx.windowMove("", Vec2(pad, io.displaySize.y - pad), Vec2(0f, 1f))

            ctx.windowRef("Dear ImGui Demo")
            ctx.menuCheck("Examples/Documents")
            ctx.windowRef("Example: Documents")
            ctx.windowResize("", Vec2(fh * 20, fh * 27))
            ctx.windowMove("", Vec2(windowCustomRendering.pos.x + windowCustomRendering.size.x + pad, pad))

            ctx.logDebug("Setup Console window...")
            ctx.windowRef("Dear ImGui Demo")
            ctx.menuCheck("Examples/Console")
            ctx.windowRef("Example: Console")
            ctx.windowResize("", Vec2(fh * 40, fh * (34 - 7)))
            ctx.windowMove("", windowCustomRendering.pos + windowCustomRendering.size * Vec2(0.3f, 0.6f))
            ctx.itemClick("Clear")
            ctx.itemClick("Add Dummy Text")
            ctx.itemClick("Add Dummy Error")
            ctx.itemClick("Input")
            ctx.keyChars("H")
            ctx.keyPressMap(Key.Tab)
            ctx.keyCharsAppendEnter("ELP")
            ctx.keyCharsAppendEnter("hello, imgui world!")

            ctx.logDebug("Setup Demo window...")
            ctx.windowRef("Dear ImGui Demo")
            ctx.windowResize("", Vec2(fh * 35, io.displaySize.y - pad * 2f))
            ctx.windowMove("", Vec2(io.displaySize.x - pad, pad), Vec2(1f, 0f))
            ctx.itemOpen("Widgets")
            ctx.itemOpen("Color\\/Picker Widgets")
            ctx.itemOpen("Layout")
            ctx.itemOpen("Groups")
            ctx.scrollToY("Layout", 0.8f)

            ctx.logDebug("Capture screenshot...")
            ctx.windowRef("")

            ctx.captureAddWindow("Dear ImGui Demo")
            ctx.captureAddWindow("Example: Simple overlay")
            ctx.captureAddWindow("Example: Custom rendering")
            ctx.captureAddWindow("Example: Simple layout")
            ctx.captureAddWindow("Example: Documents")
            ctx.captureAddWindow("Example: Console")
            ctx.captureScreenshot()

            // Close everything
            ctx.windowRef("Dear ImGui Demo")
            ctx.itemCloseAll("")
            ctx.menuUncheckAll("Examples")
            ctx.menuUncheckAll("Tools")
        }
    }
//    #endif

//    // ## Capture a screenshot displaying different supported styles.
//    t = REGISTER_TEST("capture", "capture_readme_styles");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiIO& io = ImGui::GetIO();
//        ImGuiStyle& style = ImGui::GetStyle();
//        const ImGuiStyle backup_style = style;
//        const bool backup_cursor_blink = io.ConfigInputTextCursorBlink;
//
//        // Setup style
//        // FIXME-TESTS: Ideally we'd want to be able to manipulate fonts
//        ImFont* font = FindFontByName("Roboto-Medium.ttf, 16px");
//        IM_CHECK_SILENT(font != NULL);
//        ImGui::PushFont(font);
//        style.FrameRounding = style.ChildRounding = 0;
//        style.GrabRounding = 0;
//        style.FrameBorderSize = style.ChildBorderSize = 1;
//        io.ConfigInputTextCursorBlink = false;
//
//        // Show two windows
//        for (int n = 0; n < 2; n++)
//        {
//            bool open = true;
//            ImGui::SetNextWindowSize(ImVec2(300, 160), ImGuiCond_Appearing);
//            if (n == 0)
//            {
//                ImGui::StyleColorsDark(&style);
//                ImGui::Begin("Debug##Dark", &open);
//            }
//            else
//            {
//                ImGui::StyleColorsLight(&style);
//                ImGui::Begin("Debug##Light", &open);
//            }
//            char string_buffer[] = "";
//            float float_value = 0.6f;
//            ImGui::Text("Hello, world 123");
//            ImGui::Button("Save");
//            ImGui::SetNextItemWidth(194);
//            ImGui::InputText("string", string_buffer, IM_ARRAYSIZE(string_buffer));
//            ImGui::SetNextItemWidth(194);
//            ImGui::SliderFloat("float", &float_value, 0.0f, 1.0f);
//            ImGui::End();
//        }
//        ImGui::PopFont();
//
//        // Restore style
//        style = backup_style;
//        io.ConfigInputTextCursorBlink = backup_cursor_blink;
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        // Capture both windows in separate captures
//        float padding = 13.0f;
//        ImGuiContext& g = *ctx->UiContext;
//        for (int n = 0; n < 2; n++)
//        {
//            ImGuiWindow* window = (n == 0) ? ctx->GetWindowByRef("/Debug##Dark") : ctx->GetWindowByRef("/Debug##Light");
//            ctx->WindowRef(window->Name);
//            ctx->ItemClick("string");
//            ctx->KeyChars("quick brown fox");
//            //ctx->KeyPressMap(ImGuiKey_End);
//            ctx->MouseMove("float");
//            ctx->MouseMoveToPos(g.IO.MousePos + ImVec2(30, -10));
//            ctx->CaptureArgs.InPadding = padding;
//            ctx->CaptureArgs.InCaptureWindows.push_back(window);
//            ctx->CaptureScreenshot();
//        }
//    };
//
//    #ifdef IMGUI_HAS_TABLE
//        // ## Capture all tables demo
//        t = REGISTER_TEST("capture", "capture_tables_demo");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ctx->WindowRef("Dear ImGui Demo");
//        ctx->ItemOpen("Tables & Columns");
//        ctx->ItemClick("Tables/Open all");
//        ImGuiWindow* window = ctx->GetWindowByRef("");
//        ctx->CaptureArgs.InFlags |= ImGuiCaptureToolFlags_StitchFullContents;
//        ctx->CaptureArgs.InPadding = 13.0f;
//        ctx->CaptureArgs.InCaptureWindows.push_back(window);
//        ctx->CaptureScreenshot();
//    };
//    #endif
}
