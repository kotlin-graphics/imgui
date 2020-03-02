package app.tests

import engine.context.TestContext
import engine.context.logDebug
import engine.core.TestEngine
import engine.core.registerTest
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import io.kotlintest.shouldBe

//-------------------------------------------------------------------------
// Tests: Layout
//-------------------------------------------------------------------------

private enum class ItemType { SmallButton, Button, Text, BulletText, TreeNode, Selectable, ImageButton }

fun registerTests_Layout(e: TestEngine) {

    // ## Test matching LastItemRect.Max/CursorMaxPos
    // ## Test text baseline
    e.registerTest("layout", "layout_baseline_and_cursormax").let { t ->
        t.guiFunc = { ctx: TestContext ->

            var y: Float

            ImGui.begin("Test Window", null, WindowFlag.NoSavedSettings.i)
            val g = ctx.uiContext!!
            val style = ImGui.style
            val window = ImGui.currentWindow
            val drawList = window.drawList

            for (itemType in ItemType.values()) {
                val itemTypeName = itemType.name
                for (n in 0..4) {
                    ctx.logDebug("Test '$itemTypeName' variant $n")

                    // Emit button with varying baseline
                    y = window.dc.cursorPos.y
                    if (n > 0) {
                        if (n in 1..2)
                            ImGui.smallButton("Button")
                        else if (n in 3..4)
                            ImGui.button("Button")
                        ImGui.sameLine()
                    }

                    val labelLineCount = if (n == 0 || n == 1 || n == 3) 1 else 2

                    val label = "$itemTypeName$n" + if (labelLineCount == 1) "" else "\nHello"

                    var expectedPadding = 0f
                    when (itemType) {
                        ItemType.SmallButton -> {
                            expectedPadding = window.dc.currLineTextBaseOffset
                            ImGui.smallButton(label)
                        }
                        ItemType.Button -> {
                            expectedPadding = style.framePadding.y * 2f
                            ImGui.button(label)
                        }
                        ItemType.Text -> {
                            expectedPadding = window.dc.currLineTextBaseOffset
                            ImGui.text(label)
                        }
                        ItemType.BulletText -> {
                            expectedPadding = if (n <= 2) 0f else style.framePadding.y
                            ImGui.bulletText(label)
                        }
                        ItemType.TreeNode -> {
                            expectedPadding = if (n <= 2) 0f else style.framePadding.y * 2f
                            ImGui.treeNodeEx(label, TreeNodeFlag.NoTreePushOnOpen.i)
                        }
                        ItemType.Selectable -> {
                            // FIXME-TESTS: We may want to aim the specificies of Selectable() and not clear ItemSpacing
                            //expected_padding = style.ItemSpacing.y * 0.5f;
                            expectedPadding = window.dc.currLineTextBaseOffset
                            ImGui.pushStyleVar(StyleVar.ItemSpacing, Vec2())
                            ImGui.selectable(label)
                            ImGui.popStyleVar()
                            ImGui.spacing()
                        }
                        ItemType.ImageButton -> {
                            expectedPadding = style.framePadding.y * 2f
                            ImGui.imageButton(ImGui.io.fonts.texID, Vec2(100, ImGui.textLineHeight * labelLineCount), Vec2(), Vec2(1), -1, Vec4(1f, 0.6f, 0f, 1f))
                        }
                    }

                    ImGui.debugDrawItemRect(COL32(255, 0, 0, 200))
                    drawList.addLine(Vec2(window.pos.x, window.dc.cursorMaxPos.y), Vec2(window.pos.x+window.size.x, window.dc.cursorMaxPos.y), COL32(255, 255, 0, 100))
                    if (labelLineCount > 1)
                        window.dc.cursorMaxPos.y shouldBe window.dc.lastItemRect.max.y

                    val currentHeight = window.dc.lastItemRect.max.y-y
                    val expectedHeight = g.fontSize * labelLineCount + expectedPadding
                    currentHeight shouldBe expectedHeight
                }

                ImGui.spacing()
            }

            // FIXME-TESTS: Selectable()

            ImGui.end()
        }
    }

//    // ## Test size of items with explicit thin size and larger label
//    #if 0
//    t = REGISTER_TEST("layout", "layout_height_label");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings);
//        ImGuiWindow* window = ImGui::GetCurrentWindow();
//        ImGuiContext& g = *ctx->UiContext;
//
//        ImGui::Button("Button", ImVec2(0, 8));
//        IM_CHECK_EQ_NO_RET(window->DC.LastItemRect.GetHeight(), 8.0f);
//
//        ImGui::Separator();
//
//        float values[3] = { 1.0f, 2.0f, 3.0f };
//        ImGui::PlotHistogram("##Histogram", values, 3, 0, NULL, 0.0f, 4.0f, ImVec2(0, 8));
//        IM_CHECK_EQ_NO_RET(window->DC.LastItemRect.GetHeight(), 8.0f);
//        ImGui::Separator();
//
//        ImGui::PlotHistogram("Histogram\nTwoLines", values, 3, 0, NULL, 0.0f, 4.0f, ImVec2(0, 8));
//        IM_CHECK_EQ_NO_RET(window->DC.LastItemRect.GetHeight(), g.FontSize * 2.0f);
//        ImGui::Separator();
//
//        static char buf[128] = "";
//        ImGui::InputTextMultiline("##InputText", buf, IM_ARRAYSIZE(buf), ImVec2(0, 8));
//        IM_CHECK_EQ_NO_RET(window->DC.LastItemRect.GetHeight(), 8.0f);
//        ImGui::Separator();
//
//        ImGui::InputTextMultiline("InputText\nTwoLines", buf, IM_ARRAYSIZE(buf), ImVec2(0, 8));
//        IM_CHECK_EQ_NO_RET(window->DC.LastItemRect.GetHeight(), g.FontSize * 2.0f);
//        ImGui::Separator();
//
//        ImGui::Text("Text");
//        IM_CHECK_EQ_NO_RET(window->DC.LastItemRect.GetHeight(), g.FontSize);
//
//        ImGui::End();
//    };
//    #endif

}