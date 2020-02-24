package app.tests

import engine.context.*
import engine.core.TestEngine
import engine.core.TestOpFlag
import engine.core.registerTest
import glm_.i
import glm_.vec2.Vec2
import imgui.ImGui
import imgui.dsl
import imgui.min
import io.kotlintest.shouldBe
import imgui.WindowFlag as Wf
import imgui.internal.ButtonFlag as Bf

//-------------------------------------------------------------------------
// Tests: Widgets
//-------------------------------------------------------------------------

fun registerTests_Widgets(e: TestEngine) {

    // ## Test basic button presses
    e.registerTest("widgets", "widgets_button_press").let { t ->
        t.userData = IntArray(6)
        t.guiFunc = { ctx: TestContext ->
            val vars = ctx.userData as IntArray

            dsl.window("Test Window", null, Wf.NoSavedSettings.i) {
                if (ImGui.button("Button0"))
                    vars[0]++
                if (ImGui.buttonEx("Button1", Vec2(), Bf.PressedOnDoubleClick.i))
                    vars[1]++
                if (ImGui.buttonEx("Button2", Vec2(), Bf.PressedOnClickRelease or Bf.PressedOnDoubleClick))
                    vars[2]++
                if (ImGui.buttonEx("Button3", Vec2(), Bf.PressedOnClickReleaseAnywhere.i))
                    vars[3]++
                if (ImGui.buttonEx("Button4", Vec2(), Bf.Repeat.i))
                    vars[4]++
            }
        }
        t.testFunc = { ctx: TestContext ->

            val vars = ctx.userData as IntArray

            ctx.windowRef("Test Window")
            ctx.itemClick("Button0")
            vars[0] shouldBe 1
            ctx.itemDoubleClick("Button1")
            vars[1] shouldBe 1
            ctx.itemDoubleClick("Button2")
            vars[2] shouldBe 2

            // Test ImGuiButtonFlags_PressedOnClickRelease vs ImGuiButtonFlags_PressedOnClickReleaseAnywhere
            vars[2] = 0
            ctx.mouseMove("Button2")
            ctx.mouseDown(0)
            ctx.mouseMove("Button0", TestOpFlag.NoCheckHoveredId.i)
            ctx.mouseUp(0)
            vars[2] shouldBe 0
            ctx.mouseMove("Button3")
            ctx.mouseDown(0)
            ctx.mouseMove("Button0", TestOpFlag.NoCheckHoveredId.i)
            ctx.mouseUp(0)
            vars[3] shouldBe 1

            // Test ImGuiButtonFlags_Repeat
            ctx.itemClick("Button4")
            vars[4] shouldBe 1
            ctx.mouseDown(0)
            vars[4] shouldBe 1
            ctx.uiContext!!.io.apply {
                val step = min(keyRepeatDelay, keyRepeatRate) * 0.5f
                ctx.sleepNoSkip(keyRepeatDelay, step)
                ctx.sleepNoSkip(keyRepeatRate, step)
                ctx.sleepNoSkip(keyRepeatRate, step)
                ctx.sleepNoSkip(keyRepeatRate, step)
            }
            vars[4] shouldBe (1 + 1 + 3 * 2) // FIXME: MouseRepeatRate is double KeyRepeatRate, that's not documented / or that's a bug
            ctx.mouseUp(0)
        }
    }

    // ## Test basic button presses
    e.registerTest("widgets", "widgets_button_mouse_buttons").let { t ->
        t.userData = IntArray(6)
        t.guiFunc = { ctx: TestContext ->

            val vars = ctx.userData as IntArray

            dsl.window("Test Window", null, Wf.NoSavedSettings or Wf.AlwaysAutoResize) {
                if (ImGui.buttonEx("ButtonL", Vec2(), Bf.MouseButtonLeft.i))
                    vars[0]++
                if (ImGui.buttonEx("ButtonR", Vec2(), Bf.MouseButtonRight.i))
                    vars[1]++
                if (ImGui.buttonEx("ButtonM", Vec2(), Bf.MouseButtonMiddle.i))
                    vars[2]++
                if (ImGui.buttonEx("ButtonLR", Vec2(), Bf.MouseButtonLeft or Bf.MouseButtonRight))
                    vars[3]++

                if (ImGui.buttonEx("ButtonL-release", Vec2(), Bf.MouseButtonLeft or Bf.PressedOnRelease))
                    vars[4]++
                if (ImGui.buttonEx("ButtonR-release", Vec2(), Bf.MouseButtonRight or Bf.PressedOnRelease)) {
                    ctx.logDebug("Pressed!")
                    vars[5]++
                }
                for (n in vars.indices)
                    ImGui.text("$n: ${vars[n]}")
            }
        }
        t.testFunc = { ctx: TestContext ->

            val vars = ctx.userData as IntArray

            ctx.windowRef("Test Window")
            ctx.itemClick("ButtonL", 0)
            vars[0] shouldBe 1
            ctx.itemClick("ButtonR", 1)
            vars[1] shouldBe 1
            ctx.itemClick("ButtonM", 2)
            vars[2] shouldBe 1
            ctx.itemClick("ButtonLR", 0)
            ctx.itemClick("ButtonLR", 1)
            vars[3] shouldBe 2

            vars[3] = 0
            ctx.mouseMove("ButtonLR")
            ctx.mouseDown(0)
            ctx.mouseDown(1)
            ctx.mouseUp(0)
            ctx.mouseUp(1)
            vars[3] shouldBe 1

            vars[3] = 0
            ctx.mouseMove("ButtonLR")
            ctx.mouseDown(0)
            ctx.mouseMove("ButtonR", TestOpFlag.NoCheckHoveredId.i)
            ctx.mouseDown(1)
            ctx.mouseUp(0)
            ctx.mouseMove("ButtonLR")
            ctx.mouseUp(1)
            vars[3] shouldBe 0
        }
    }

    e.registerTest("widgets", "widgets_button_status").let { t ->
        t.userData = ButtonStateTestVars()
        t.guiFunc = { ctx: TestContext ->

            val vars = ctx.userData as ButtonStateTestVars
            val status = vars.status

            ImGui.begin("Test Window", null, Wf.NoSavedSettings.i)

            val pressed = ImGui.button("Test")
            status.querySet()
            when (vars.nextStep) {
                ButtonStateMachineTestStep.Init -> {
                    pressed shouldBe false
                    status.hovered shouldBe 0
                    status.active shouldBe 0
                    status.activated shouldBe 0
                    status.deactivated shouldBe 0
                }
                ButtonStateMachineTestStep.MovedOver -> {
                    pressed shouldBe false
                    status.hovered shouldBe 1
                    status.active shouldBe 0
                    status.activated shouldBe 0
                    status.deactivated shouldBe 0
                }
                ButtonStateMachineTestStep.MouseDown -> {
                    pressed shouldBe false
                    status.hovered shouldBe 1
                    status.active shouldBe 1
                    status.activated shouldBe 1
                    status.deactivated shouldBe 0
                }
                ButtonStateMachineTestStep.MovedAway -> {
                    pressed shouldBe false
                    status.hovered shouldBe 0
                    status.active shouldBe 1
                    status.activated shouldBe 0
                    status.deactivated shouldBe 0
                }
                ButtonStateMachineTestStep.MovedOverAgain -> {
                    pressed shouldBe false
                    status.hovered shouldBe 1
                    status.active shouldBe 1
                    status.activated shouldBe 0
                    status.deactivated shouldBe 0
                }
                ButtonStateMachineTestStep.MouseUp -> {
                    pressed shouldBe true
                    status.hovered shouldBe 1
                    status.active shouldBe 0
                    status.activated shouldBe 0
                    status.deactivated shouldBe 1
                }
                ButtonStateMachineTestStep.Done -> {
                    pressed shouldBe false
                    status.hovered shouldBe 0
                    status.active shouldBe 0
                    status.activated shouldBe 0
                    status.deactivated shouldBe 0
                }
                else -> Unit
            }
            vars.nextStep = ButtonStateMachineTestStep.None

            // The "Dummy" button allows to move the mouse away from the "Test" button
            ImGui.button("Dummy")

            ImGui.end()
        }
        t.testFunc = { ctx: TestContext ->

            val vars = ctx.userData as ButtonStateTestVars
            vars.nextStep = ButtonStateMachineTestStep.None

            ctx.windowRef("Test Window")

            // Move mouse away from "Test" button
            ctx.mouseMove("Dummy")
            vars.nextStep = ButtonStateMachineTestStep.Init
            ctx.yield()

            ctx.mouseMove("Test")
            vars.nextStep = ButtonStateMachineTestStep.MovedOver
            ctx.yield()

            vars.nextStep = ButtonStateMachineTestStep.MouseDown
            ctx.mouseDown()

            ctx.mouseMove("Dummy", TestOpFlag.NoCheckHoveredId.i)
            vars.nextStep = ButtonStateMachineTestStep.MovedAway
            ctx.yield()

            ctx.mouseMove("Test")
            vars.nextStep = ButtonStateMachineTestStep.MovedOverAgain
            ctx.yield()

            vars.nextStep = ButtonStateMachineTestStep.MouseUp
            ctx.mouseUp()

            ctx.mouseMove("Dummy")
            vars.nextStep = ButtonStateMachineTestStep.Done
            ctx.yield()
        }
    }

//    // ## Test checkbox click
//    t = REGISTER_TEST("widgets", "widgets_checkbox_001");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::Begin("Window1");
//        ImGui::Checkbox("Checkbox", &ctx->GenericVars.Bool1);
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        // We use WindowRef() to ensure the window is uncollapsed.
//        IM_CHECK(ctx->GenericVars.Bool1 == false);
//        ctx->WindowRef("Window1");
//        ctx->ItemClick("Checkbox");
//        IM_CHECK(ctx->GenericVars.Bool1 == true);
//    };
//
//    // FIXME-TESTS: WIP
//    t = REGISTER_TEST("widgets", "widgets_datatype_1");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::SetNextWindowSize(ImVec2(200, 200));
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings);
//        char buf[3] = { 42, 100, 42 };
//        ImGui::DragScalar("Drag", ImGuiDataType_S8, &buf[1], 0.5f, NULL, NULL);
//        IM_ASSERT(buf[0] == 42 && buf[2] == 42);
//        ImGui::End();
//    };
//
//    // ## Test DragInt() as InputText
//    // ## Test ColorEdit4() as InputText (#2557)
//    t = REGISTER_TEST("widgets", "widgets_as_input");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings);
//        ImGui::DragInt("Drag", &vars.Int1);
//        ImGui::ColorEdit4("Color", &vars.Vec4.x);
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        ctx->WindowRef("Test Window");
//
//        IM_CHECK_EQ(vars.Int1, 0);
//        ctx->ItemInput("Drag");
//        IM_CHECK_EQ(ctx->UiContext->ActiveId, ctx->GetID("Drag"));
//        ctx->KeyCharsAppendEnter("123");
//        IM_CHECK_EQ(vars.Int1, 123);
//
//        ctx->ItemInput("Color##Y");
//        IM_CHECK_EQ(ctx->UiContext->ActiveId, ctx->GetID("Color##Y"));
//        ctx->KeyCharsAppend("123");
//        IM_CHECK(FloatEqual(vars.Vec4.y, 123.0f / 255.0f));
//        ctx->KeyPressMap(ImGuiKey_Tab);
//        ctx->KeyCharsAppendEnter("200");
//        IM_CHECK(FloatEqual(vars.Vec4.x,   0.0f / 255.0f));
//        IM_CHECK(FloatEqual(vars.Vec4.y, 123.0f / 255.0f));
//        IM_CHECK(FloatEqual(vars.Vec4.z, 200.0f / 255.0f));
//    };
//
//    // ## Test InputText widget
//    t = REGISTER_TEST("widgets", "widgets_inputtext_1");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        ImGui::SetNextWindowSize(ImVec2(200, 200));
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings);
//        ImGui::InputText("InputText", vars.Str1, IM_ARRAYSIZE(vars.Str1));
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        char* buf = ctx->GenericVars.Str1;
//
//        ctx->WindowRef("Test Window");
//
//        // Insert
//        strcpy(buf, "Hello");
//        ctx->ItemClick("InputText");
//        ctx->KeyCharsAppendEnter("World123");
//        IM_CHECK_STR_EQ(buf, "HelloWorld123");
//
//        // Delete
//        ctx->ItemClick("InputText");
//        ctx->KeyPressMap(ImGuiKey_End);
//        ctx->KeyPressMap(ImGuiKey_Backspace, ImGuiKeyModFlags_None, 3);
//        ctx->KeyPressMap(ImGuiKey_Enter);
//        IM_CHECK_STR_EQ(buf, "HelloWorld");
//
//        // Insert, Cancel
//        ctx->ItemClick("InputText");
//        ctx->KeyPressMap(ImGuiKey_End);
//        ctx->KeyChars("XXXXX");
//        ctx->KeyPressMap(ImGuiKey_Escape);
//        IM_CHECK_STR_EQ(buf, "HelloWorld");
//
//        // Delete, Cancel
//        ctx->ItemClick("InputText");
//        ctx->KeyPressMap(ImGuiKey_End);
//        ctx->KeyPressMap(ImGuiKey_Backspace, ImGuiKeyModFlags_None, 5);
//        ctx->KeyPressMap(ImGuiKey_Escape);
//        IM_CHECK_STR_EQ(buf, "HelloWorld");
//    };
//
//    // ## Test InputText undo/redo ops, in particular related to issue we had with stb_textedit undo/redo buffers
//    t = REGISTER_TEST("widgets", "widgets_inputtext_2");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        if (vars.StrLarge.empty())
//            vars.StrLarge.resize(10000, 0);
//        ImGui::SetNextWindowSize(ImVec2(ImGui::GetFontSize() * 50, 0.0f));
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_AlwaysAutoResize);
//        ImGui::Text("strlen() = %d", (int)strlen(vars.StrLarge.Data));
//        ImGui::InputText("Dummy", vars.Str1, IM_ARRAYSIZE(vars.Str1), ImGuiInputTextFlags_None);
//        ImGui::InputTextMultiline("InputText", vars.StrLarge.Data, vars.StrLarge.Size, ImVec2(-1, ImGui::GetFontSize() * 20), ImGuiInputTextFlags_None);
//        ImGui::End();
//        //ImDebugShowInputTextState();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        // https://github.com/nothings/stb/issues/321
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//
//        // Start with a 350 characters buffer.
//        // For this test we don't inject the characters via pasting or key-by-key in order to precisely control the undo/redo state.
//        char* buf = vars.StrLarge.Data;
//        IM_CHECK_EQ((int)strlen(buf), 0);
//        for (int n = 0; n < 10; n++)
//        strcat(buf, "xxxxxxx abcdefghijklmnopqrstuvwxyz\n");
//        IM_CHECK_EQ((int)strlen(buf), 350);
//
//        ctx->WindowRef("Test Window");
//        ctx->ItemClick("Dummy"); // This is to ensure stb_textedit_clear_state() gets called (clear the undo buffer, etc.)
//        ctx->ItemClick("InputText");
//
//        ImGuiInputTextState& input_text_state = GImGui->InputTextState;
//        ImStb::StbUndoState& undo_state = input_text_state.Stb.undostate;
//        IM_CHECK_EQ(input_text_state.ID, GImGui->ActiveId);
//        IM_CHECK_EQ(undo_state.undo_point, 0);
//        IM_CHECK_EQ(undo_state.undo_char_point, 0);
//        IM_CHECK_EQ(undo_state.redo_point, STB_TEXTEDIT_UNDOSTATECOUNT);
//        IM_CHECK_EQ(undo_state.redo_char_point, STB_TEXTEDIT_UNDOCHARCOUNT);
//        IM_CHECK_EQ(STB_TEXTEDIT_UNDOCHARCOUNT, 999); // Test designed for this value
//
//        // Insert 350 characters via 10 paste operations
//        // We use paste operations instead of key-by-key insertion so we know our undo buffer will contains 10 undo points.
//        //const char line_buf[26+8+1+1] = "xxxxxxx abcdefghijklmnopqrstuvwxyz\n"; // 8+26+1 = 35
//        //ImGui::SetClipboardText(line_buf);
//        //IM_CHECK(strlen(line_buf) == 35);
//        //ctx->KeyPressMap(ImGuiKey_V, ImGuiKeyModFlags_Shortcut, 10);
//
//        // Select all, copy, paste 3 times
//        ctx->KeyPressMap(ImGuiKey_A, ImGuiKeyModFlags_Shortcut);    // Select all
//        ctx->KeyPressMap(ImGuiKey_C, ImGuiKeyModFlags_Shortcut);    // Copy
//        ctx->KeyPressMap(ImGuiKey_End, ImGuiKeyModFlags_Shortcut);  // Go to end, clear selection
//        ctx->SleepShort();
//        for (int n = 0; n < 3; n++)
//        {
//            ctx->KeyPressMap(ImGuiKey_V, ImGuiKeyModFlags_Shortcut);// Paste append three times
//            ctx->SleepShort();
//        }
//        int len = (int)strlen(vars.StrLarge.Data);
//        IM_CHECK_EQ(len, 350 * 4);
//        IM_CHECK_EQ(undo_state.undo_point, 3);
//        IM_CHECK_EQ(undo_state.undo_char_point, 0);
//
//        // Undo x2
//        IM_CHECK(undo_state.redo_point == STB_TEXTEDIT_UNDOSTATECOUNT);
//        ctx->KeyPressMap(ImGuiKey_Z, ImGuiKeyModFlags_Shortcut);
//        ctx->KeyPressMap(ImGuiKey_Z, ImGuiKeyModFlags_Shortcut);
//        len = (int)strlen(vars.StrLarge.Data);
//        IM_CHECK_EQ(len, 350 * 2);
//        IM_CHECK_EQ(undo_state.undo_point, 1);
//        IM_CHECK_EQ(undo_state.redo_point, STB_TEXTEDIT_UNDOSTATECOUNT - 2);
//        IM_CHECK_EQ(undo_state.redo_char_point, STB_TEXTEDIT_UNDOCHARCOUNT - 350 * 2);
//
//        // Undo x1 should call stb_textedit_discard_redo()
//        ctx->KeyPressMap(ImGuiKey_Z, ImGuiKeyModFlags_Shortcut);
//        len = (int)strlen(vars.StrLarge.Data);
//        IM_CHECK_EQ(len, 350 * 1);
//    };
//
//    // ## Test InputText vs user ownership of data
//    t = REGISTER_TEST("widgets", "widgets_inputtext_3_text_ownership");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_AlwaysAutoResize);
//        ImGui::LogToBuffer();
//        ImGui::InputText("##InputText", vars.Str1, IM_ARRAYSIZE(vars.Str1)); // Remove label to simplify the capture/comparison
//        ImStrncpy(vars.Str2, ctx->UiContext->LogBuffer.c_str(), IM_ARRAYSIZE(vars.Str2));
//        ImGui::LogFinish();
//        ImGui::Text("Captured: \"%s\"", vars.Str2);
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        char* buf_user = vars.Str1;
//        char* buf_visible = vars.Str2;
//        ctx->WindowRef("Test Window");
//
//        IM_CHECK_STR_EQ(buf_visible, "");
//        strcpy(buf_user, "Hello");
//        ctx->Yield();
//        IM_CHECK_STR_EQ(buf_visible, "Hello");
//        ctx->ItemClick("##InputText");
//        ctx->KeyCharsAppend("1");
//        ctx->Yield();
//        IM_CHECK_STR_EQ(buf_user, "Hello1");
//        IM_CHECK_STR_EQ(buf_visible, "Hello1");
//
//        // Because the item is active, it owns the source data, so:
//        strcpy(buf_user, "Overwritten");
//        ctx->Yield();
//        IM_CHECK_STR_EQ(buf_user, "Hello1");
//        IM_CHECK_STR_EQ(buf_visible, "Hello1");
//
//        // Lose focus, at this point the InputTextState->ID should be holding on the last active state,
//        // so we verify that InputText() is picking up external changes.
//        ctx->KeyPressMap(ImGuiKey_Escape);
//        IM_CHECK_EQ(ctx->UiContext->ActiveId, (unsigned)0);
//        strcpy(buf_user, "Hello2");
//        ctx->Yield();
//        IM_CHECK_STR_EQ(buf_user, "Hello2");
//        IM_CHECK_STR_EQ(buf_visible, "Hello2");
//    };
//
//    // ## Test that InputText doesn't go havoc when activated via another item
//    t = REGISTER_TEST("widgets", "widgets_inputtext_4_id_conflict");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        ImGui::SetNextWindowSize(ImVec2(ImGui::GetFontSize() * 50, 0.0f));
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_AlwaysAutoResize);
//        if (ctx->FrameCount < 50)
//        ImGui::Button("Hello");
//        else
//        ImGui::InputText("Hello", vars.Str1, IM_ARRAYSIZE(vars.Str1));
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ctx->WindowRef("Test Window");
//        ctx->ItemHoldForFrames("Hello", 100);
//    };
//
//    // ## Test that InputText doesn't append two tab characters if the backend supplies both tab key and character
//    t = REGISTER_TEST("widgets", "widgets_inputtext_5_tab_double_insertion");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings);
//        ImGui::InputText("Field", vars.Str1, IM_ARRAYSIZE(vars.Str1), ImGuiInputTextFlags_AllowTabInput);
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        ctx->WindowRef("Test Window");
//        ctx->ItemClick("Field");
//        ctx->UiContext->IO.AddInputCharacter((ImWchar)'\t');
//        ctx->KeyPressMap(ImGuiKey_Tab);
//        IM_CHECK_STR_EQ(vars.Str1, "\t");
//    };
//
//    // ## Test input clearing action (ESC key) being undoable (#3008).
//    t = REGISTER_TEST("widgets", "widgets_inputtext_6_esc_undo");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings);
//        ImGui::InputText("Field", vars.Str1, IM_ARRAYSIZE(vars.Str1));
//        ImGui::End();
//
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        // FIXME-TESTS: Facilitate usage of variants
//        const int test_count = ctx->HasDock ? 2 : 1;
//        for (int test_n = 0; test_n < test_count; test_n++)
//        {
//            ctx->LogDebug("TEST CASE %d", test_n);
//            const char* initial_value = (test_n == 0) ? "" : "initial";
//            strcpy(vars.Str1, initial_value);
//            ctx->WindowRef("Test Window");
//            ctx->ItemInput("Field");
//            ctx->KeyCharsReplace("text");
//            IM_CHECK_STR_EQ(vars.Str1, "text");
//            ctx->KeyPressMap(ImGuiKey_Escape);                      // Reset input to initial value.
//            IM_CHECK_STR_EQ(vars.Str1, initial_value);
//            ctx->ItemInput("Field");
//            ctx->KeyPressMap(ImGuiKey_Z, ImGuiKeyModFlags_Shortcut);    // Undo
//            IM_CHECK_STR_EQ(vars.Str1, "text");
//            ctx->KeyPressMap(ImGuiKey_Enter);                       // Unfocus otherwise test_n==1 strcpy will fail
//        }
//    };
//
//    // ## Test resize callback (#3009, #2006, #1443, #1008)
//    t = REGISTER_TEST("widgets", "widgets_inputtext_7_resizecallback");
//    struct StrVars { Str str; };
//    t->SetUserDataType<StrVars>();
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        StrVars& vars = ctx->GetUserData<StrVars>();
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings);
//        if (ImGui::InputText("Field1", &vars.str, ImGuiInputTextFlags_EnterReturnsTrue))
//        {
//            IM_CHECK_EQ(vars.str.capacity(), 4 + 5 + 1);
//            IM_CHECK_STR_EQ(vars.str.c_str(), "abcdhello");
//        }
//        Str str_local_unsaved = "abcd";
//        if (ImGui::InputText("Field2", &str_local_unsaved, ImGuiInputTextFlags_EnterReturnsTrue))
//        {
//            IM_CHECK_EQ(str_local_unsaved.capacity(), 4 + 5 + 1);
//            IM_CHECK_STR_EQ(str_local_unsaved.c_str(), "abcdhello");
//        }
//        ImGui::End();
//
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        StrVars& vars = ctx->GetUserData<StrVars>();
//        vars.str.set("abcd");
//        IM_CHECK_EQ(vars.str.capacity(), 4+1);
//        ctx->WindowRef("Test Window");
//        ctx->ItemInput("Field1");
//        ctx->KeyCharsAppendEnter("hello");
//        ctx->ItemInput("Field2");
//        ctx->KeyCharsAppendEnter("hello");
//    };
//
//    // ## Test for Nav interference
//    t = REGISTER_TEST("widgets", "widgets_inputtext_nav");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings);
//        ImVec2 sz(50, 0);
//        ImGui::Button("UL", sz); ImGui::SameLine();
//        ImGui::Button("U",  sz); ImGui::SameLine();
//        ImGui::Button("UR", sz);
//        ImGui::Button("L",  sz); ImGui::SameLine();
//        ImGui::SetNextItemWidth(sz.x);
//        ImGui::InputText("##Field", vars.Str1, IM_ARRAYSIZE(vars.Str1), ImGuiInputTextFlags_AllowTabInput);
//        ImGui::SameLine();
//        ImGui::Button("R", sz);
//        ImGui::Button("DL", sz); ImGui::SameLine();
//        ImGui::Button("D", sz); ImGui::SameLine();
//        ImGui::Button("DR", sz);
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ctx->WindowRef("Test Window");
//        ctx->ItemClick("##Field");
//        ctx->KeyPressMap(ImGuiKey_LeftArrow);
//        IM_CHECK_EQ(ctx->UiContext->NavId, ctx->GetID("##Field"));
//        ctx->KeyPressMap(ImGuiKey_RightArrow);
//        IM_CHECK_EQ(ctx->UiContext->NavId, ctx->GetID("##Field"));
//        ctx->KeyPressMap(ImGuiKey_UpArrow);
//        IM_CHECK_EQ(ctx->UiContext->NavId, ctx->GetID("U"));
//        ctx->KeyPressMap(ImGuiKey_DownArrow);
//        ctx->KeyPressMap(ImGuiKey_DownArrow);
//        IM_CHECK_EQ(ctx->UiContext->NavId, ctx->GetID("D"));
//    };
//
//    // ## Test ColorEdit4() and IsItemDeactivatedXXX() functions
//    // ## Test that IsItemActivated() doesn't trigger when clicking the color button to open picker
//    t = REGISTER_TEST("widgets", "widgets_status_coloredit");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_AlwaysAutoResize);
//        bool ret = ImGui::ColorEdit4("Field", &vars.Vec4.x, ImGuiColorEditFlags_None);
//        vars.Status.QueryInc(ret);
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        // Accumulate return values over several frames/action into each bool
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        ImGuiTestGenericStatus& status = vars.Status;
//
//        // Testing activation flag being set
//        ctx->WindowRef("Test Window");
//        ctx->ItemClick("Field/##ColorButton");
//        IM_CHECK(status.Ret == 0 && status.Activated == 1 && status.Deactivated == 1 && status.DeactivatedAfterEdit == 0 && status.Edited == 0);
//        status.Clear();
//
//        ctx->KeyPressMap(ImGuiKey_Escape);
//        IM_CHECK(status.Ret == 0 && status.Activated == 0 && status.Deactivated == 0 && status.DeactivatedAfterEdit == 0 && status.Edited == 0);
//        status.Clear();
//    };
//
//    // ## Test InputText() and IsItemDeactivatedXXX() functions (mentioned in #2215)
//    t = REGISTER_TEST("widgets", "widgets_status_inputtext");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_AlwaysAutoResize);
//        bool ret = ImGui::InputText("Field", vars.Str1, IM_ARRAYSIZE(vars.Str1));
//        vars.Status.QueryInc(ret);
//        ImGui::InputText("Dummy Sibling", vars.Str2, IM_ARRAYSIZE(vars.Str2));
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        // Accumulate return values over several frames/action into each bool
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        ImGuiTestGenericStatus& status = vars.Status;
//
//        // Testing activation flag being set
//        ctx->WindowRef("Test Window");
//        ctx->ItemClick("Field");
//        IM_CHECK(status.Ret == 0 && status.Activated == 1 && status.Deactivated == 0 && status.DeactivatedAfterEdit == 0 && status.Edited == 0);
//        status.Clear();
//
//        // Testing deactivated flag being set when canceling with Escape
//        ctx->KeyPressMap(ImGuiKey_Escape);
//        IM_CHECK(status.Ret == 0 && status.Activated == 0 && status.Deactivated == 1 && status.DeactivatedAfterEdit == 0 && status.Edited == 0);
//        status.Clear();
//
//        // Testing validation with Return after editing
//        ctx->ItemClick("Field");
//        IM_CHECK(!status.Ret && status.Activated && !status.Deactivated && !status.DeactivatedAfterEdit && status.Edited == 0);
//        status.Clear();
//        ctx->KeyCharsAppend("Hello");
//        IM_CHECK(status.Ret && !status.Activated && !status.Deactivated && !status.DeactivatedAfterEdit && status.Edited >= 1);
//        status.Clear();
//        ctx->KeyPressMap(ImGuiKey_Enter);
//        IM_CHECK(!status.Ret && !status.Activated && status.Deactivated && status.DeactivatedAfterEdit && status.Edited == 0);
//        status.Clear();
//
//        // Testing validation with Tab after editing
//        ctx->ItemClick("Field");
//        ctx->KeyCharsAppend(" World");
//        IM_CHECK(status.Ret && status.Activated && !status.Deactivated && !status.DeactivatedAfterEdit && status.Edited >= 1);
//        status.Clear();
//        ctx->KeyPressMap(ImGuiKey_Tab);
//        IM_CHECK(!status.Ret && !status.Activated && status.Deactivated && status.DeactivatedAfterEdit && status.Edited == 0);
//        status.Clear();
//    };
//
//    // ## Test the IsItemDeactivatedXXX() functions (e.g. #2550, #1875)
//    t = REGISTER_TEST("widgets", "widgets_status_multicomponent");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_AlwaysAutoResize);
//        bool ret = ImGui::InputFloat4("Field", &vars.FloatArray[0]);
//        vars.Status.QueryInc(ret);
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        // Accumulate return values over several frames/action into each bool
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        ImGuiTestGenericStatus& status = vars.Status;
//
//        // FIXME-TESTS: Better helper to build ids out of various type of data
//        ctx->WindowRef("Test Window");
//        int n;
//        n = 0; ImGuiID field_0 = ImHashData(&n, sizeof(n), ctx->GetID("Field"));
//        n = 1; ImGuiID field_1 = ImHashData(&n, sizeof(n), ctx->GetID("Field"));
//        //n = 2; ImGuiID field_2 = ImHashData(&n, sizeof(n), ctx->GetID("Field"));
//
//        // Testing activation/deactivation flags
//        ctx->ItemClick(field_0);
//        IM_CHECK(status.Ret == 0 && status.Activated == 1 && status.Deactivated == 0 && status.DeactivatedAfterEdit == 0);
//        status.Clear();
//        ctx->KeyPressMap(ImGuiKey_Enter);
//        IM_CHECK(status.Ret == 0 && status.Activated == 0 && status.Deactivated == 1 && status.DeactivatedAfterEdit == 0);
//        status.Clear();
//
//        // Testing validation with Return after editing
//        ctx->ItemClick(field_0);
//        status.Clear();
//        ctx->KeyCharsAppend("123");
//        IM_CHECK(status.Ret >= 1 && status.Activated == 0 && status.Deactivated == 0);
//        status.Clear();
//        ctx->KeyPressMap(ImGuiKey_Enter);
//        IM_CHECK(status.Ret == 0 && status.Activated == 0 && status.Deactivated == 1);
//        status.Clear();
//
//        // Testing validation with Tab after editing
//        ctx->ItemClick(field_0);
//        ctx->KeyCharsAppend("456");
//        status.Clear();
//        ctx->KeyPressMap(ImGuiKey_Tab);
//        IM_CHECK(status.Ret == 0 && status.Activated == 1 && status.Deactivated == 1 && status.DeactivatedAfterEdit == 1);
//
//        // Testing Edited flag on all components
//        ctx->ItemClick(field_1); // FIXME-TESTS: Should not be necessary!
//        ctx->ItemClick(field_0);
//        ctx->KeyCharsAppend("111");
//        IM_CHECK(status.Edited >= 1);
//        ctx->KeyPressMap(ImGuiKey_Tab);
//        status.Clear();
//        ctx->KeyCharsAppend("222");
//        IM_CHECK(status.Edited >= 1);
//        ctx->KeyPressMap(ImGuiKey_Tab);
//        status.Clear();
//        ctx->KeyCharsAppend("333");
//        IM_CHECK(status.Edited >= 1);
//    };
//
//    // ## Test the IsItemEdited() function when input vs output format are not matching
//    t = REGISTER_TEST("widgets", "widgets_status_inputfloat_format_mismatch");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_AlwaysAutoResize);
//        bool ret = ImGui::InputFloat("Field", &vars.Float1);
//        vars.Status.QueryInc(ret);
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        ImGuiTestGenericStatus& status = vars.Status;
//
//        // Input "1" which will be formatted as "1.000", make sure we don't report IsItemEdited() multiple times!
//        ctx->WindowRef("Test Window");
//        ctx->ItemClick("Field");
//        ctx->KeyCharsAppend("1");
//        IM_CHECK(status.Ret == 1 && status.Edited == 1 && status.Activated == 1 && status.Deactivated == 0 && status.DeactivatedAfterEdit == 0);
//        ctx->Yield();
//        ctx->Yield();
//        IM_CHECK(status.Edited == 1);
//    };
//
//    // ## Test ColorEdit basic Drag and Drop
//    t = REGISTER_TEST("widgets", "widgets_coloredit_drag");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        ImGui::SetNextWindowSize(ImVec2(300, 200));
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings);
//        ImGui::ColorEdit4("ColorEdit1", &vars.Vec4Array[0].x, ImGuiColorEditFlags_None);
//        ImGui::ColorEdit4("ColorEdit2", &vars.Vec4Array[1].x, ImGuiColorEditFlags_None);
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiTestGenericVars& vars = ctx->GenericVars;
//        vars.Vec4Array[0] = ImVec4(1, 0, 0, 1);
//        vars.Vec4Array[1] = ImVec4(0, 1, 0, 1);
//
//        ctx->WindowRef("Test Window");
//
//        IM_CHECK_NE(memcmp(&vars.Vec4Array[0], &vars.Vec4Array[1], sizeof(ImVec4)), 0);
//        ctx->ItemDragAndDrop("ColorEdit1/##ColorButton", "ColorEdit2/##X"); // FIXME-TESTS: Inner items
//        IM_CHECK_EQ(memcmp(&vars.Vec4Array[0], &vars.Vec4Array[1], sizeof(ImVec4)), 0);
//    };
//
//    // ## Test that disabled Selectable has an ID but doesn't interfere with navigation
//    t = REGISTER_TEST("widgets", "widgets_selectable_disabled");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_AlwaysAutoResize);
//        ImGui::Selectable("Selectable A");
//        if (ctx->FrameCount == 0)
//        IM_CHECK_EQ(ImGui::GetItemID(), ImGui::GetID("Selectable A"));
//        ImGui::Selectable("Selectable B", false, ImGuiSelectableFlags_Disabled);
//        if (ctx->FrameCount == 0)
//        IM_CHECK_EQ(ImGui::GetItemID(), ImGui::GetID("Selectable B")); // Make sure B has an ID
//        ImGui::Selectable("Selectable C");
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ctx->WindowRef("Test Window");
//        ctx->ItemClick("Selectable A");
//        IM_CHECK_EQ(ctx->UiContext->NavId, ctx->GetID("Selectable A"));
//        ctx->KeyPressMap(ImGuiKey_DownArrow);
//        IM_CHECK_EQ(ctx->UiContext->NavId, ctx->GetID("Selectable C")); // Make sure we have skipped B
//    };
//
//    // ## Test that tight tab bar does not create extra drawcalls
//    t = REGISTER_TEST("widgets", "widgets_tabbar_drawcalls");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings);
//        if (ImGui::BeginTabBar("Tab Drawcalls"))
//        {
//            for (int i = 0; i < 20; i++)
//            if (ImGui::BeginTabItem(Str30f("Tab %d", i).c_str()))
//                ImGui::EndTabItem();
//            ImGui::EndTabBar();
//        }
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGuiWindow* window = ImGui::FindWindowByName("Test Window");
//        ctx->WindowResize("Test Window", ImVec2(300, 300));
//        int draw_calls = window->DrawList->CmdBuffer.Size;
//        ctx->WindowResize("Test Window", ImVec2(1, 1));
//        IM_CHECK(draw_calls == window->DrawList->CmdBuffer.Size);
//    };
//
//    // ## Test recursing Tab Bars (Bug #2371)
//    t = REGISTER_TEST("widgets", "widgets_tabbar_recurse");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_AlwaysAutoResize);
//        if (ImGui::BeginTabBar("TabBar 0"))
//        {
//            if (ImGui::BeginTabItem("TabItem"))
//            {
//                // If we have many tab bars here, it will invalidate pointers from pooled tab bars
//                for (int i = 0; i < 128; i++)
//                if (ImGui::BeginTabBar(Str30f("Inner TabBar %d", i).c_str()))
//                {
//                    if (ImGui::BeginTabItem("Inner TabItem"))
//                        ImGui::EndTabItem();
//                    ImGui::EndTabBar();
//                }
//                ImGui::EndTabItem();
//            }
//            ImGui::EndTabBar();
//        }
//        ImGui::End();
//    };
//
//    #ifdef IMGUI_HAS_DOCK
//        // ## Test Dockspace within a TabItem
//        t = REGISTER_TEST("widgets", "widgets_tabbar_dockspace");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings);
//        if (ImGui::BeginTabBar("TabBar"))
//        {
//            if (ImGui::BeginTabItem("TabItem"))
//            {
//                ImGui::DockSpace(ImGui::GetID("Hello"), ImVec2(0, 0));
//                ImGui::EndTabItem();
//            }
//            ImGui::EndTabBar();
//        }
//        ImGui::End();
//    };
//    #endif
//
//    // ## Test SetSelected on first frame of a TabItem
//    t = REGISTER_TEST("widgets", "widgets_tabbar_tabitem_setselected");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings);
//        if (ImGui::BeginTabBar("tab_bar"))
//        {
//            if (ImGui::BeginTabItem("TabItem 0"))
//            {
//                ImGui::TextUnformatted("First tab content");
//                ImGui::EndTabItem();
//            }
//
//            if (ctx->FrameCount >= 0)
//            {
//                bool tab_item_visible = ImGui::BeginTabItem("TabItem 1", NULL, ctx->FrameCount == 0 ? ImGuiTabItemFlags_SetSelected : ImGuiTabItemFlags_None);
//                if (tab_item_visible)
//                {
//                    ImGui::TextUnformatted("Second tab content");
//                    ImGui::EndTabItem();
//                }
//                if (ctx->FrameCount > 0)
//                IM_CHECK(tab_item_visible);
//            }
//            ImGui::EndTabBar();
//        }
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx) { ctx->Yield(); };
//
//    // ## Test ImGuiTreeNodeFlags_SpanAvailWidth and ImGuiTreeNodeFlags_SpanFullWidth flags
//    t = REGISTER_TEST("widgets", "widgets_tree_node_span_width");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::SetNextWindowSize(ImVec2(300, 100), ImGuiCond_Always);
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings);
//        ImGuiWindow* window = ImGui::GetCurrentWindow();
//
//        ImGui::SetNextItemOpen(true);
//        if (ImGui::TreeNodeEx("Parent"))
//        {
//            // Interaction rect does not span entire width of work area.
//            IM_CHECK(window->DC.LastItemRect.Max.x < window->WorkRect.Max.x);
//            // But it starts at very beginning of WorkRect for first tree level.
//            IM_CHECK(window->DC.LastItemRect.Min.x == window->WorkRect.Min.x);
//            ImGui::SetNextItemOpen(true);
//            if (ImGui::TreeNodeEx("Regular"))
//            {
//                // Interaction rect does not span entire width of work area.
//                IM_CHECK(window->DC.LastItemRect.Max.x < window->WorkRect.Max.x);
//                IM_CHECK(window->DC.LastItemRect.Min.x > window->WorkRect.Min.x);
//                ImGui::TreePop();
//            }
//            ImGui::SetNextItemOpen(true);
//            if (ImGui::TreeNodeEx("SpanAvailWidth", ImGuiTreeNodeFlags_SpanAvailWidth))
//            {
//                // Interaction rect matches visible frame rect
//                IM_CHECK((window->DC.LastItemStatusFlags & ImGuiItemStatusFlags_HasDisplayRect) != 0);
//                IM_CHECK(window->DC.LastItemDisplayRect.Min == window->DC.LastItemRect.Min);
//                IM_CHECK(window->DC.LastItemDisplayRect.Max == window->DC.LastItemRect.Max);
//                // Interaction rect extends to the end of the available area.
//                IM_CHECK(window->DC.LastItemRect.Max.x == window->WorkRect.Max.x);
//                ImGui::TreePop();
//            }
//            ImGui::SetNextItemOpen(true);
//            if (ImGui::TreeNodeEx("SpanFullWidth", ImGuiTreeNodeFlags_SpanFullWidth))
//            {
//                // Interaction rect matches visible frame rect
//                IM_CHECK((window->DC.LastItemStatusFlags & ImGuiItemStatusFlags_HasDisplayRect) != 0);
//                IM_CHECK(window->DC.LastItemDisplayRect.Min == window->DC.LastItemRect.Min);
//                IM_CHECK(window->DC.LastItemDisplayRect.Max == window->DC.LastItemRect.Max);
//                // Interaction rect extends to the end of the available area.
//                IM_CHECK(window->DC.LastItemRect.Max.x == window->WorkRect.Max.x);
//                // ImGuiTreeNodeFlags_SpanFullWidth also extends interaction rect to the left.
//                IM_CHECK(window->DC.LastItemRect.Min.x == window->WorkRect.Min.x);
//                ImGui::TreePop();
//            }
//            ImGui::TreePop();
//        }
//
//        ImGui::End();
//    };
//
//    // ## Test PlotLines() with a single value (#2387).
//    t = REGISTER_TEST("widgets", "widgets_plot_lines_unexpected_input");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        float values[1] = {0.f};
//        ImGui::PlotLines("PlotLines 1", NULL, 0);
//        ImGui::PlotLines("PlotLines 2", values, 0);
//        ImGui::PlotLines("PlotLines 3", values, 1);
//        // FIXME-TESTS: If test did not crash - it passed. A better way to check this would be useful.
//    };
//
//    // ## Test BeginDragDropSource() with NULL id.
//    t = REGISTER_TEST("widgets", "widgets_drag_source_null_id");
//    struct WidgetDragSourceNullIDData
//            {
//                ImVec2 Source;
//                ImVec2 Destination;
//                bool Dropped = false;
//            };
//    t->SetUserDataType<WidgetDragSourceNullIDData>();
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        WidgetDragSourceNullIDData& user_data = *(WidgetDragSourceNullIDData*)ctx->UserData;
//
//        ImGui::Begin("Null ID Test");
//        ImGui::TextUnformatted("Null ID");
//        user_data.Source = ImRect(ImGui::GetItemRectMin(), ImGui::GetItemRectMax()).GetCenter();
//
//        if (ImGui::BeginDragDropSource(ImGuiDragDropFlags_SourceAllowNullID))
//        {
//            int magic = 0xF00;
//            ImGui::SetDragDropPayload("MAGIC", &magic, sizeof(int));
//            ImGui::EndDragDropSource();
//        }
//        ImGui::TextUnformatted("Drop Here");
//        user_data.Destination = ImRect(ImGui::GetItemRectMin(), ImGui::GetItemRectMax()).GetCenter();
//
//        if (ImGui::BeginDragDropTarget())
//        {
//            if (const ImGuiPayload* payload = ImGui::AcceptDragDropPayload("MAGIC"))
//            {
//                user_data.Dropped = true;
//                IM_CHECK_EQ(payload->DataSize, (int)sizeof(int));
//                IM_CHECK_EQ(*(int*)payload->Data, 0xF00);
//            }
//            ImGui::EndDragDropTarget();
//        }
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        WidgetDragSourceNullIDData& user_data = *(WidgetDragSourceNullIDData*)ctx->UserData;
//
//        // ImGui::TextUnformatted() does not have an ID therefore we can not use ctx->ItemDragAndDrop() as that refers
//        // to items by their ID.
//        ctx->MouseMoveToPos(user_data.Source);
//        ctx->SleepShort();
//        ctx->MouseDown(0);
//
//        ctx->MouseMoveToPos(user_data.Destination);
//        ctx->SleepShort();
//        ctx->MouseUp(0);
//
//        IM_CHECK(user_data.Dropped);
//    };
//
//    // ## Test long text rendering by TextUnformatted().
//    t = REGISTER_TEST("widgets", "widgets_text_unformatted_long");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ctx->WindowRef("Dear ImGui Demo");
//        ctx->MenuClick("Examples/Long text display");
//        ctx->WindowRef("Example: Long text display");
//        ctx->ItemClick("Add 1000 lines");
//        ctx->SleepShort();
//
//        Str64f title("/Example: Long text display\\/Log_%08X", ctx->GetID("Log"));
//        ImGuiWindow* log_panel = ctx->GetWindowByRef(title.c_str());
//        IM_CHECK(log_panel != NULL);
//        ImGui::SetScrollY(log_panel, log_panel->ScrollMax.y);
//        ctx->SleepShort();
//        ctx->ItemClick("Clear");
//        // FIXME-TESTS: A bit of extra testing that will be possible once tomato problem is solved.
//        // ctx->ComboClick("Test type/Single call to TextUnformatted()");
//        // ctx->ComboClick("Test type/Multiple calls to Text(), clipped");
//        // ctx->ComboClick("Test type/Multiple calls to Text(), not clipped (slow)");
//        ctx->WindowClose("");
//    };
}

class ButtonStateTestVars {
    var nextStep = ButtonStateMachineTestStep.None
    var status = TestGenericStatus()
}

// ## Test ButtonBehavior frame by frame behaviors (see comments at the top of the ButtonBehavior() function)
enum class ButtonStateMachineTestStep { None, Init, MovedOver, MouseDown, MovedAway, MovedOverAgain, MouseUp, Done }