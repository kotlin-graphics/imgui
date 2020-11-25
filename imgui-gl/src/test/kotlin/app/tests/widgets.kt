package app.tests

import engine.KeyModFlag
import engine.context.*
import engine.core.TestEngine
import engine.core.TestOpFlag
import engine.core.registerTest
import engine.inputText_
import glm_.ext.equal
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.api.gImGui
import imgui.internal.ItemStatusFlag
import imgui.internal.classes.Rect
import imgui.internal.has
import imgui.internal.hash
import imgui.stb.te
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
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

    // ## Test checkbox click
    e.registerTest("widgets", "widgets_checkbox_001").let { t ->
        t.guiFunc = { ctx: TestContext ->
            dsl.window("Window1") {
                ImGui.checkbox("Checkbox", ctx.genericVars::bool1)
            }
        }
        t.testFunc = { ctx: TestContext ->
            // We use WindowRef() to ensure the window is uncollapsed.
            ctx.genericVars.bool1 shouldBe false
            ctx.windowRef("Window1")
            ctx.itemClick("Checkbox")
            ctx.genericVars.bool1 shouldBe true
        }
    }

    // FIXME-TESTS: WIP
    e.registerTest("widgets", "widgets_datatype_1").let { t ->
        t.guiFunc = {
            ImGui.setNextWindowSize(Vec2(200))
            dsl.window("Test Window", null, Wf.NoSavedSettings.i) {
                val buf = floatArrayOf(42f, 100f, 42f)
                ImGui.dragScalar("Drag", buf, 1, 0.5f)
                assert(buf[0] == 42f && buf[2] == 42f)
            }
        }
    }

    // ## Test DragInt() as InputText
    // ## Test ColorEdit4() as InputText (#2557)
    e.registerTest("widgets", "widgets_as_input").let { t ->
        t.guiFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            dsl.window("Test Window", null, Wf.NoSavedSettings.i) {
                ImGui.dragInt("Drag", vars::int1)
                ImGui.colorEdit4("Color", vars.vec4)
            }
        }
        t.testFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            ctx.windowRef("Test Window")

            vars.int1 shouldBe 0
            ctx.itemInput("Drag")
            ctx.uiContext!!.activeId shouldBe ctx.getID("Drag")
            ctx.keyCharsAppendEnter("123")
            vars.int1 shouldBe 123

            ctx.itemInput("Color##Y")
            ctx.uiContext!!.activeId shouldBe ctx.getID("Color##Y")
            ctx.keyCharsAppend("123")
            vars.vec4.y.equal(123f / 255f) shouldBe true
            ctx.keyPressMap(Key.Tab)
            ctx.keyCharsAppendEnter("200")
            (vars.vec4.x.equal(0f / 255f)) shouldBe true
            (vars.vec4.y.equal(123f / 255f)) shouldBe true
            (vars.vec4.z.equal(200f / 255f)) shouldBe true
        }
    }

    // ## Test InputText widget
    e.registerTest("widgets", "widgets_inputtext_1").let { t ->
        t.guiFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            ImGui.setNextWindowSize(Vec2(200))
            dsl.window("Test Window", null, Wf.NoSavedSettings.i) {
                ImGui.inputText("InputText", vars.str1)
            }
        }
        t.testFunc = { ctx: TestContext ->

            val buf = ctx.genericVars.str1

            ctx.windowRef("Test Window")

            // Insert
            "Hello".toByteArray(buf)
            ctx.itemClick("InputText")
            ctx.keyCharsAppendEnter("World123")
            buf.cStr shouldBe "HelloWorld123"

            // Delete
            ctx.itemClick("InputText")
            ctx.keyPressMap(Key.End)
            ctx.keyPressMap(Key.Backspace, KeyModFlag.None.i, 3)
            ctx.keyPressMap(Key.Enter)
            buf.cStr shouldBe "HelloWorld"

            // Insert, Cancel
            ctx.itemClick("InputText")
            ctx.keyPressMap(Key.End)
            ctx.keyChars("XXXXX")
            ctx.keyPressMap(Key.Escape)
            buf.cStr shouldBe "HelloWorld"

            // Delete, Cancel
            ctx.itemClick("InputText")
            ctx.keyPressMap(Key.End)
            ctx.keyPressMap(Key.Backspace, KeyModFlag.None.i, 5)
            ctx.keyPressMap(Key.Escape)
            buf.cStr shouldBe "HelloWorld"
        }
    }

    // ## Test InputText undo/redo ops, in particular related to issue we had with stb_textedit undo/redo buffers
    e.registerTest("widgets", "widgets_inputtext_2").let { t ->
        t.guiFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            if (vars.strLarge.isEmpty())
                vars.strLarge = ByteArray(10000)
            ImGui.setNextWindowSize(Vec2(ImGui.fontSize * 50, 0f))
            dsl.window("Test Window", null, Wf.NoSavedSettings or Wf.AlwaysAutoResize) {
                ImGui.text("strlen() = ${vars.strLarge.strlen()}")
                ImGui.inputText("Dummy", vars.str1, InputTextFlag.None.i)
                ImGui.inputTextMultiline("InputText", vars.strLarge, Vec2(-1f, ImGui.fontSize * 20), InputTextFlag.None.i)
            }
            //ImDebugShowInputTextState();
        }
        t.testFunc = { ctx: TestContext ->

            // https://github.com/nothings/stb/issues/321
            val vars = ctx.genericVars

            // Start with a 350 characters buffer.
            // For this test we don't inject the characters via pasting or key-by-key in order to precisely control the undo/redo state.
            val buf = vars.strLarge
            buf.strlen() shouldBe 0
            for (n in 0..9) {
                val bytes = "xxxxxxx abcdefghijklmnopqrstuvwxyz\n".toByteArray()
                val size = bytes.strlen()
                bytes.copyInto(buf, size * n)
            }
            buf.strlen() shouldBe 350

            ctx.windowRef("Test Window")
            ctx.itemClick("Dummy") // This is to ensure stb_textedit_clear_state() gets called (clear the undo buffer, etc.)
            ctx.itemClick("InputText")

            val inputTextState = gImGui!!.inputTextState
            val undoState = inputTextState.stb.undoState
            inputTextState.id shouldBe gImGui!!.activeId
            undoState.undoPoint shouldBe 0
            undoState.undoCharPoint shouldBe 0
            undoState.redoPoint = te.UNDOSTATECOUNT
            undoState.redoCharPoint shouldBe te.UNDOCHARCOUNT
            te.UNDOCHARCOUNT shouldBe 999 // Test designed for this value

            // Insert 350 characters via 10 paste operations
            // We use paste operations instead of key-by-key insertion so we know our undo buffer will contains 10 undo points.
            //const char line_buf[26+8+1+1] = "xxxxxxx abcdefghijklmnopqrstuvwxyz\n"; // 8+26+1 = 35
            //ImGui::SetClipboardText(line_buf);
            //IM_CHECK(strlen(line_buf) == 35);
            //ctx->KeyPressMap(ImGuiKey_V, ImGuiKeyModFlags_Shortcut, 10);

            // Select all, copy, paste 3 times
            ctx.keyPressMap(Key.A, KeyModFlag.Shortcut.i)    // Select all
            ctx.keyPressMap(Key.C, KeyModFlag.Shortcut.i)    // Copy
            ctx.keyPressMap(Key.End, KeyModFlag.Shortcut.i)  // Go to end, clear selection
            ctx.sleepShort()
            for (n in 0..2) {
                ctx.keyPressMap(Key.V, KeyModFlag.Shortcut.i)// Paste append three times
                ctx.sleepShort()
            }
            var len = vars.strLarge.strlen()
            len shouldBe (350 * 4)
            undoState.undoPoint shouldBe 3
            undoState.undoCharPoint shouldBe 0

            // Undo x2
            undoState.redoPoint shouldBe te.UNDOSTATECOUNT
            ctx.keyPressMap(Key.Z, KeyModFlag.Shortcut.i)
            ctx.keyPressMap(Key.Z, KeyModFlag.Shortcut.i)
            len = vars.strLarge.strlen()
            len shouldBe (350 * 2)
            undoState.undoPoint shouldBe 1
            undoState.redoPoint shouldBe (te.UNDOSTATECOUNT - 2)
            undoState.redoCharPoint shouldBe (te.UNDOCHARCOUNT - 350 * 2)

            // Undo x1 should call stb_textedit_discard_redo()
            ctx.keyPressMap(Key.Z, KeyModFlag.Shortcut.i)
            len = vars.strLarge.strlen()
            len shouldBe (350 * 1)
        }
    }

    // ## Test InputText vs user ownership of data
    e.registerTest("widgets", "widgets_inputtext_3_text_ownership").let { t ->
        t.guiFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            dsl.window("Test Window", null, Wf.NoSavedSettings or Wf.AlwaysAutoResize) {
                ImGui.logToBuffer()
                ImGui.inputText("##InputText", vars.str1) // Remove label to simplify the capture/comparison
                ctx.uiContext!!.logBuffer.toString().toByteArray(vars.str2)
                ImGui.logFinish()
                ImGui.text("Captured: \"${vars.str2.cStr}\"")
            }
        }
        t.testFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            val bufUser = vars.str1
            val bufVisible = vars.str2
            ctx.windowRef("Test Window")

            bufVisible.cStr shouldBe ""
            "Hello".toByteArray(bufUser)
            ctx.yield()
            bufVisible.cStr shouldBe "Hello"
            ctx.itemClick("##InputText")
            ctx.keyCharsAppend("1")
            ctx.yield()
            bufUser.cStr shouldBe "Hello1"
            bufVisible.cStr shouldBe "Hello1"

            // Because the item is active, it owns the source data, so:
            "Overwritten".toByteArray(bufUser)
            ctx.yield()
            bufUser.cStr shouldBe "Hello1"
            bufVisible.cStr shouldBe "Hello1"

            // Lose focus, at this point the InputTextState->ID should be holding on the last active state,
            // so we verify that InputText() is picking up external changes.
            ctx.keyPressMap(Key.Escape)
            ctx.uiContext!!.activeId shouldBe 0
            "Hello2".toByteArray(bufUser)
            ctx.yield()
            bufUser.cStr shouldBe "Hello2"
            bufVisible.cStr shouldBe "Hello2"
        }
    }

    // ## Test that InputText doesn't go havoc when activated via another item
    e.registerTest("widgets", "widgets_inputtext_4_id_conflict").let { t ->
        t.guiFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            ImGui.setNextWindowSize(Vec2(ImGui.fontSize * 50, 0f))
            dsl.window("Test Window", null, Wf.NoSavedSettings or Wf.AlwaysAutoResize) {
                if (ctx.frameCount < 50)
                    ImGui.button("Hello")
                else
                    ImGui.inputText("Hello", vars.str1)
            }
        }
        t.testFunc = { ctx: TestContext ->
            ctx.windowRef("Test Window")
            ctx.itemHoldForFrames("Hello", 100)
        }
    }

    // ## Test that InputText doesn't append two tab characters if the backend supplies both tab key and character
    e.registerTest("widgets", "widgets_inputtext_5_tab_double_insertion").let { t ->
        t.guiFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            dsl.window("Test Window", null, Wf.NoSavedSettings.i) {
                ImGui.inputText("Field", vars.str1, InputTextFlag.AllowTabInput.i)
            }
        }
        t.testFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            ctx.windowRef("Test Window")
            ctx.itemClick("Field")
            ctx.uiContext!!.io.addInputCharacter('\t')
            ctx.keyPressMap(Key.Tab)
            vars.str1.cStr shouldBe "\t"
        }
    }

    // ## Test input clearing action (ESC key) being undoable (#3008).
    e.registerTest("widgets", "widgets_inputtext_6_esc_undo").let { t ->
        t.guiFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            dsl.window("Test Window", null, Wf.NoSavedSettings.i) {
                ImGui.inputText("Field", vars.str1)
            }

        }
        t.testFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            // FIXME-TESTS: Facilitate usage of variants
            val testCount = if (ctx.hasDock) 2 else 1
            for (testN in 0 until testCount) {
                ctx.logDebug("TEST CASE $testN")
                val initialValue = if (testN == 0) "" else "initial"
                initialValue.toByteArray(vars.str1)
                ctx.windowRef("Test Window")
                ctx.itemInput("Field")
                ctx.keyCharsReplace("text")
                vars.str1.cStr shouldBe "text"
                ctx.keyPressMap(Key.Escape)                      // Reset input to initial value.
                vars.str1.cStr shouldBe initialValue
                ctx.itemInput("Field")
                ctx.keyPressMap(Key.Z, KeyModFlag.Shortcut.i)    // Undo
                vars.str1.cStr shouldBe "text"
                ctx.keyPressMap(Key.Enter)                       // Unfocus otherwise test_n==1 strcpy will fail
            }
        }
    }

    // ## Test resize callback (#3009, #2006, #1443, #1008)
    e.registerTest("widgets", "widgets_inputtext_7_resizecallback").let { t ->
        t.userData = ByteArray(0)
        t.guiFunc = { ctx: TestContext ->
            val vars = ctx.userData as ByteArray
            dsl.window("Test Window", null, Wf.NoSavedSettings.i) {
                if (ImGui.inputText_("Field1", vars, InputTextFlag.EnterReturnsTrue.i)) {
                    vars.strlen() shouldBe (4 + 5)
                    vars.cStr shouldBe "abcdhello"
                }
                // [JVM] increase buffer size in order to avoid reassignment inside ImGui::inputText when
                // calling the callback which would create a new (bigger) ByteArray, breaking our
                // ::strLocalUnsaved reference.
                // This is a jvm limit, cpp works because it passes pointers around
                val strLocalUnsaved = "abcd".toByteArray(32)
                if (ImGui.inputText_("Field2", strLocalUnsaved, InputTextFlag.EnterReturnsTrue.i)) {
                    strLocalUnsaved.strlen() shouldBe (4 + 5)
                    strLocalUnsaved.cStr shouldBe "abcdhello"
                }
            }
        }
        t.testFunc = { ctx: TestContext ->
            // size 32 for the same reason as right above
            ctx.userData = "abcd".toByteArray(32).also {
                it.strlen() shouldBe 4
            }
            ctx.windowRef("Test Window")
            ctx.itemInput("Field1")
            ctx.keyCharsAppendEnter("hello")
            ctx.itemInput("Field2")
            ctx.keyCharsAppendEnter("hello")
        }
    }

    // ## Test for Nav interference
    e.registerTest("widgets", "widgets_inputtext_nav").let { t ->
        t.guiFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            dsl.window("Test Window", null, Wf.NoSavedSettings.i) {
                val sz = Vec2(50, 0)
                ImGui.button("UL", sz); ImGui.sameLine()
                ImGui.button("U", sz); ImGui.sameLine()
                ImGui.button("UR", sz)
                ImGui.button("L", sz); ImGui.sameLine()
                ImGui.setNextItemWidth(sz.x)
                ImGui.inputText("##Field", vars.str1, InputTextFlag.AllowTabInput.i)
                ImGui.sameLine()
                ImGui.button("R", sz)
                ImGui.button("DL", sz); ImGui.sameLine()
                ImGui.button("D", sz); ImGui.sameLine()
                ImGui.button("DR", sz)
            }
        }
        t.testFunc = { ctx: TestContext ->
            ctx.windowRef("Test Window")
            ctx.itemClick("##Field")
            ctx.keyPressMap(Key.LeftArrow)
            ctx.uiContext!!.navId shouldBe ctx.getID("##Field")
            ctx.keyPressMap(Key.RightArrow)
            ctx.uiContext!!.navId shouldBe ctx.getID("##Field")
            ctx.keyPressMap(Key.UpArrow)
            ctx.uiContext!!.navId shouldBe ctx.getID("U")
            ctx.keyPressMap(Key.DownArrow)
            ctx.keyPressMap(Key.DownArrow)
            ctx.uiContext!!.navId shouldBe ctx.getID("D")
        }
    }

    // ## Test ColorEdit4() and IsItemDeactivatedXXX() functions
    // ## Test that IsItemActivated() doesn't trigger when clicking the color button to open picker
    e.registerTest("widgets", "widgets_status_coloredit").let { t ->
        t.guiFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            dsl.window("Test Window", null, Wf.NoSavedSettings or Wf.AlwaysAutoResize) {
                val ret = ImGui.colorEdit4("Field", vars.vec4, ColorEditFlag.None.i)
                vars.status.queryInc(ret)
            }
        }
        t.testFunc = { ctx: TestContext ->
            // Accumulate return values over several frames/action into each bool
            val vars = ctx.genericVars
            val status = vars.status

            // Testing activation flag being set
            ctx.windowRef("Test Window")
            ctx.itemClick("Field/##ColorButton")
            status.apply {
                assert(ret == 0 && activated == 1 && deactivated == 1 && deactivatedAfterEdit == 0 && edited == 0)
                clear()

                ctx.keyPressMap(Key.Escape)
                assert(ret == 0 && activated == 0 && deactivated == 0 && deactivatedAfterEdit == 0 && edited == 0)
                clear()
            }
        }
    }

    // ## Test InputText() and IsItemDeactivatedXXX() functions (mentioned in #2215)
    e.registerTest("widgets", "widgets_status_inputtext").let { t ->
        t.guiFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            dsl.window("Test Window", null, Wf.NoSavedSettings or Wf.AlwaysAutoResize) {
                val ret = ImGui.inputText("Field", vars.str1)
                vars.status.queryInc(ret)
                ImGui.inputText("Dummy Sibling", vars.str2)
            }
        }
        t.testFunc = { ctx: TestContext ->
            // Accumulate return values over several frames/action into each bool
            val vars = ctx.genericVars
            val status = vars.status

            // Testing activation flag being set
            ctx.windowRef("Test Window")
            ctx.itemClick("Field")
            status.apply {
                assert(ret == 0 && activated == 1 && deactivated == 0 && deactivatedAfterEdit == 0 && edited == 0)
                clear()

                // Testing deactivated flag being set when canceling with Escape
                ctx.keyPressMap(Key.Escape)
                assert(ret == 0 && activated == 0 && deactivated == 1 && deactivatedAfterEdit == 0 && edited == 0)
                clear()

                // Testing validation with Return after editing
                ctx.itemClick("Field")
                assert(ret == 0 && activated != 0 && deactivated == 0 && deactivatedAfterEdit == 0 && edited == 0)
                clear()
                ctx.keyCharsAppend("Hello")
                assert(ret != 0 && activated == 0 && deactivated == 0 && deactivatedAfterEdit == 0 && edited >= 1)
                clear()
                ctx.keyPressMap(Key.Enter)
                assert(ret == 0 && activated == 0 && deactivated != 0 && deactivatedAfterEdit != 0 && edited == 0)
                clear()

                // Testing validation with Tab after editing
                ctx.itemClick("Field")
                ctx.keyCharsAppend(" World")
                assert(ret != 0 && activated != 0 && deactivated == 0 && deactivatedAfterEdit == 0 && edited >= 1)
                clear()
                ctx.keyPressMap(Key.Tab)
                assert(ret == 0 && activated == 0 && deactivated != 0 && deactivatedAfterEdit != 0 && edited == 0)
                clear()
            }
        }
    }

    // ## Test the IsItemDeactivatedXXX() functions (e.g. #2550, #1875)
    e.registerTest("widgets", "widgets_status_multicomponent").let { t ->
        t.guiFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            dsl.window("Test Window", null, Wf.NoSavedSettings or Wf.AlwaysAutoResize) {
                val ret = ImGui.inputFloat4("Field", vars.floatArray)
                vars.status.queryInc(ret)
            }
        }
        t.testFunc = { ctx: TestContext ->
            // Accumulate return values over several frames/action into each bool
            val vars = ctx.genericVars
            val status = vars.status

            // FIXME-TESTS: Better helper to build ids out of various type of data
            ctx.windowRef("Test Window")
            var n: Int
            n = 0
            val field0: ID = hash(n, ctx.getID("Field"))
            n = 1
            val field1: ID = hash(n, ctx.getID("Field"))
            //n = 2; ImGuiID field_2 = ImHashData(&n, sizeof(n), ctx->GetID("Field"));

            status.apply {
                // Testing activation/deactivation flags
                ctx.itemClick(field0)
                assert(ret == 0 && activated == 1 && deactivated == 0 && deactivatedAfterEdit == 0)
                clear()
                ctx.keyPressMap(Key.Enter)
                assert(ret == 0 && activated == 0 && deactivated == 1 && deactivatedAfterEdit == 0)
                clear()

                // Testing validation with Return after editing
                ctx.itemClick(field0)
                clear()
                ctx.keyCharsAppend("123")
                assert(ret >= 1 && activated == 0 && deactivated == 0)
                clear()
                ctx.keyPressMap(Key.Enter)
                assert(ret == 0 && activated == 0 && deactivated == 1)
                clear()

                // Testing validation with Tab after editing
                ctx.itemClick(field0)
                ctx.keyCharsAppend("456")
                clear()
                ctx.keyPressMap(Key.Tab)
                assert(ret == 0 && activated == 1 && deactivated == 1 && deactivatedAfterEdit == 1)

                // Testing Edited flag on all components
                ctx.itemClick(field1) // FIXME-TESTS: Should not be necessary!
                ctx.itemClick(field0)
                ctx.keyCharsAppend("111")
                assert(edited >= 1)
                ctx.keyPressMap(Key.Tab)
                clear()
                ctx.keyCharsAppend("222")
                assert(edited >= 1)
                ctx.keyPressMap(Key.Tab)
                clear()
                ctx.keyCharsAppend("333")
                assert(edited >= 1)
            }
        }
    }

    // ## Test the IsItemEdited() function when input vs output format are not matching
    e.registerTest("widgets", "widgets_status_inputfloat_format_mismatch").let { t ->
        t.guiFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            dsl.window("Test Window", null, Wf.NoSavedSettings or Wf.AlwaysAutoResize) {
                val ret = ImGui.inputFloat("Field", vars::float1)
                vars.status.queryInc(ret)
            }
        }
        t.testFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            val status = vars.status

            // Input "1" which will be formatted as "1.000", make sure we don't report IsItemEdited() multiple times!
            ctx.windowRef("Test Window")
            ctx.itemClick("Field")
            ctx.keyCharsAppend("1")
            status.apply {
                assert(ret == 1 && edited == 1 && activated == 1 && deactivated == 0 && deactivatedAfterEdit == 0)
                ctx.yield()
                ctx.yield()
                assert(edited == 1)
            }
        }
    }

    // ## Test ColorEdit basic Drag and Drop
    e.registerTest("widgets", "widgets_coloredit_drag").let { t ->
        t.guiFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            ImGui.setNextWindowSize(Vec2(300, 200))
            dsl.window("Test Window", null, Wf.NoSavedSettings.i) {
                ImGui.colorEdit4("ColorEdit1", vars.vec4Array[0], ColorEditFlag.None.i)
                ImGui.colorEdit4("ColorEdit2", vars.vec4Array[1], ColorEditFlag.None.i)
            }
        }
        t.testFunc = { ctx: TestContext ->
            val vars = ctx.genericVars
            vars.vec4Array[0] = Vec4(1, 0, 0, 1)
            vars.vec4Array[1] = Vec4(0, 1, 0, 1)

            ctx.windowRef("Test Window")

            vars.vec4Array[0] shouldNotBe vars.vec4Array[1]
            ctx.itemDragAndDrop("ColorEdit1/##ColorButton", "ColorEdit2/##X") // FIXME-TESTS: Inner items
            vars.vec4Array[0] shouldBe vars.vec4Array[1]
        }
    }

    // ## Test that disabled Selectable has an ID but doesn't interfere with navigation
    e.registerTest("widgets", "widgets_selectable_disabled").let { t ->
        t.guiFunc = { ctx: TestContext ->
            dsl.window("Test Window", null, Wf.NoSavedSettings or Wf.AlwaysAutoResize) {
                ImGui.selectable("Selectable A")
                if (ctx.frameCount == 0)
                    ImGui.itemID shouldBe ImGui.getID("Selectable A")
                ImGui.selectable("Selectable B", false, SelectableFlag.Disabled.i)
                if (ctx.frameCount == 0)
                    ImGui.itemID shouldBe ImGui.getID("Selectable B") // Make sure B has an ID
                ImGui.selectable("Selectable C")
            }
        }
        t.testFunc = { ctx: TestContext ->
            ctx.windowRef("Test Window")
            ctx.itemClick("Selectable A")
            ctx.uiContext!!.navId shouldBe ctx.getID("Selectable A")
            ctx.keyPressMap(Key.DownArrow)
            ctx.uiContext!!.navId shouldBe ctx.getID("Selectable C") // Make sure we have skipped B
        }
    }

    // ## Test that tight tab bar does not create extra drawcalls
    e.registerTest("widgets", "widgets_tabbar_drawcalls").let { t ->
        t.guiFunc = {
            dsl.window("Test Window", null, Wf.NoSavedSettings.i) {
                dsl.tabBar("Tab Drawcalls") {
                    for (i in 0..19)
                        dsl.tabItem("Tab $i") {}
                }
            }
        }
        t.testFunc = { ctx: TestContext ->
            val window = ImGui.findWindowByName("Test Window")!!
            ctx.windowResize("Test Window", Vec2(300))
            val drawCalls = window.drawList.cmdBuffer.size
            ctx.windowResize("Test Window", Vec2(1))
            drawCalls shouldBe window.drawList.cmdBuffer.size
        }
    }

    // ## Test recursing Tab Bars (Bug #2371)
    e.registerTest("widgets", "widgets_tabbar_recurse").let { t ->
        t.guiFunc = {
            dsl.window("Test Window", null, Wf.NoSavedSettings or Wf.AlwaysAutoResize) {
                dsl.tabBar("TabBar 0") {
                    dsl.tabItem("TabItem") {
                        // If we have many tab bars here, it will invalidate pointers from pooled tab bars
                        for (i in 0..127)
                            dsl.tabBar("Inner TabBar $i") {
                                dsl.tabItem("Inner TabItem") {}
                            }
                    }
                }
            }
        }
    }

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

    // ## Test SetSelected on first frame of a TabItem
    e.registerTest("widgets", "widgets_tabbar_tabitem_setselected").let { t ->
        t.guiFunc = { ctx: TestContext ->
            dsl.window("Test Window", null, Wf.NoSavedSettings.i) {
                dsl.tabBar("tab_bar") {
                    dsl.tabItem("TabItem 0") {
                        ImGui.textUnformatted("First tab content")
                    }

                    if (ctx.frameCount >= 0) {
                        val flag = if (ctx.frameCount == 0) TabItemFlag.SetSelected else TabItemFlag.None
                        val tabItemVisible = ImGui.beginTabItem("TabItem 1", null, flag.i)
                        if (tabItemVisible) {
                            ImGui.textUnformatted("Second tab content")
                            ImGui.endTabItem()
                        }
                        if (ctx.frameCount > 0)
                            assert(tabItemVisible)
                    }
                }
            }
        }
        t.testFunc = { ctx: TestContext -> ctx.yield() }
    }

    // ## Test ImGuiTreeNodeFlags_SpanAvailWidth and ImGuiTreeNodeFlags_SpanFullWidth flags
    e.registerTest("widgets", "widgets_tree_node_span_width").let { t ->
        t.guiFunc = {
            ImGui.setNextWindowSize(Vec2(300, 100), Cond.Always)
            dsl.window("Test Window", null, Wf.NoSavedSettings.i) {
                val window = ImGui.currentWindow

                ImGui.setNextItemOpen(true)
                if (ImGui.treeNodeEx("Parent")) {
                    // Interaction rect does not span entire width of work area.
                    window.dc.lastItemRect.max.x shouldBeLessThan window.workRect.max.x
                    // But it starts at very beginning of WorkRect for first tree level.
                    window.dc.lastItemRect.min.x shouldBe window.workRect.min.x
                    ImGui.setNextItemOpen(true)
                    if (ImGui.treeNodeEx("Regular")) {
                        // Interaction rect does not span entire width of work area.
                        window.dc.lastItemRect.max.x shouldBeLessThan window.workRect.max.x
                        window.dc.lastItemRect.min.x shouldBeGreaterThan window.workRect.min.x
                        ImGui.treePop()
                    }
                    ImGui.setNextItemOpen(true)
                    if (ImGui.treeNodeEx("SpanAvailWidth", TreeNodeFlag.SpanAvailWidth.i)) {
                        // Interaction rect matches visible frame rect
                        assert(window.dc.lastItemStatusFlags has ItemStatusFlag.HasDisplayRect)
                        window.dc.lastItemDisplayRect.min shouldBe window.dc.lastItemRect.min
                        window.dc.lastItemDisplayRect.max shouldBe window.dc.lastItemRect.max
                        // Interaction rect extends to the end of the available area.
                        window.dc.lastItemRect.max.x shouldBe window.workRect.max.x
                        ImGui.treePop()
                    }
                    ImGui.setNextItemOpen(true)
                    if (ImGui.treeNodeEx("SpanFullWidth", TreeNodeFlag.SpanFullWidth.i)) {
                        // Interaction rect matches visible frame rect
                        assert(window.dc.lastItemStatusFlags has ItemStatusFlag.HasDisplayRect)
                        window.dc.lastItemDisplayRect.min shouldBe window.dc.lastItemRect.min
                        window.dc.lastItemDisplayRect.max shouldBe window.dc.lastItemRect.max
                        // Interaction rect extends to the end of the available area.
                        window.dc.lastItemRect.max.x shouldBe window.workRect.max.x
                        // ImGuiTreeNodeFlags_SpanFullWidth also extends interaction rect to the left.
                        window.dc.lastItemRect.min.x shouldBe window.workRect.min.x
                        ImGui.treePop()
                    }
                    ImGui.treePop()
                }
            }
        }
    }

    // ## Test PlotLines() with a single value (#2387).
    e.registerTest("widgets", "widgets_plot_lines_unexpected_input").let { t ->
        t.testFunc = {
            val values = floatArrayOf(0f)
            ImGui.plotLines("PlotLines 1", floatArrayOf())
            ImGui.plotLines("PlotLines 2", values)
            ImGui.plotLines("PlotLines 3", values)
            // FIXME-TESTS: If test did not crash - it passed. A better way to check this would be useful.
        }
    }

    // ## Test BeginDragDropSource() with NULL id.
    e.registerTest("widgets", "widgets_drag_source_null_id").let { t ->
        t.userData = WidgetDragSourceNullIdData()
        t.guiFunc = { ctx: TestContext ->

            val userData = ctx.userData as WidgetDragSourceNullIdData

            dsl.window("Null ID Test") {
                ImGui.textUnformatted("Null ID")
                userData.source = Rect(ImGui.itemRectMin, ImGui.itemRectMax).center

                dsl.dragDropSource(DragDropFlag.SourceAllowNullID.i) {
                    val magic = 0xF00
                    ImGui.setDragDropPayload("MAGIC", magic)
                }
                ImGui.textUnformatted("Drop Here")
                userData.destination = Rect(ImGui.itemRectMin, ImGui.itemRectMax).center

                dsl.dragDropTarget {
                    ImGui.acceptDragDropPayload("MAGIC")?.let { payload ->
                        userData.dropped = true
                        (payload.data as Int) shouldBe 0xF00
                    }
                }
            }
        }
        t.testFunc = { ctx: TestContext ->

            val userData = ctx.userData as WidgetDragSourceNullIdData

            // ImGui::TextUnformatted() does not have an ID therefore we can not use ctx->ItemDragAndDrop() as that refers
            // to items by their ID.
            ctx.mouseMoveToPos(userData.source)
            ctx.sleepShort()
            ctx.mouseDown(0)

            ctx.mouseMoveToPos(userData.destination)
            ctx.sleepShort()
            ctx.mouseUp(0)

            userData.dropped shouldBe true
        }
    }

    // ## Test long text rendering by TextUnformatted().
    e.registerTest("widgets", "widgets_text_unformatted_long").let { t ->
        t.testFunc = { ctx: TestContext ->
            ctx.windowRef("Dear ImGui Demo")
            ctx.menuClick("Examples/Long text display")
            ctx.windowRef("Example: Long text display")
            ctx.itemClick("Add 1000 lines")
            ctx.sleepShort()

            val title = "/Example: Long text display\\/Log_%08X".format(ctx.getID("Log"))
            val logPanel = ctx.getWindowByRef(title)!!
//            assert(logPanel != null)
            logPanel setScrollY logPanel.scrollMax.y
            ctx.sleepShort()
            ctx.itemClick("Clear")
            // FIXME-TESTS: A bit of extra testing that will be possible once tomato problem is solved.
            // ctx->ComboClick("Test type/Single call to TextUnformatted()");
            // ctx->ComboClick("Test type/Multiple calls to Text(), clipped");
            // ctx->ComboClick("Test type/Multiple calls to Text(), not clipped (slow)");
            ctx.windowClose("")
        }
    }
}

class ButtonStateTestVars {
    var nextStep = ButtonStateMachineTestStep.None
    var status = TestGenericStatus()
}

// ## Test ButtonBehavior frame by frame behaviors (see comments at the top of the ButtonBehavior() function)
enum class ButtonStateMachineTestStep { None, Init, MovedOver, MouseDown, MovedAway, MovedOverAgain, MouseUp, Done }

class WidgetDragSourceNullIdData(var dropped: Boolean = false) {
    lateinit var source: Vec2
    lateinit var destination: Vec2
}