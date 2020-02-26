package app.tests

import engine.context.TestContext
import engine.core.TestEngine
import engine.core.registerTest
import imgui.ImGui
import imgui.SelectableFlag
import imgui.WindowFlag
import imgui.dsl

//-------------------------------------------------------------------------
// Tests: Columns
//-------------------------------------------------------------------------

fun registerTests_Columns(e: TestEngine) {

    // ## Test number of draw calls used by columns
    e.registerTest("columns", "columns_draw_calls").let { t ->
        t.guiFunc = { ctx: TestContext ->
            ImGui.begin("Test window 1", null, WindowFlag.NoSavedSettings.i)
            ImGui.text("Hello")
            val drawList = ImGui.windowDrawList

            // Test: Single column don't consume draw call.
            var cmdCount = drawList.cmdBuffer.size
            dsl.columns("columns1", 1) {
                ImGui.text("AAAA"); ImGui.nextColumn()
                ImGui.text("BBBB"); ImGui.nextColumn()
            }
            ImGui.text("Hello")
            assert(drawList.cmdBuffer.size == cmdCount + 0)

            // Test: Multi-column consume 1 draw call per column + 1 due to conservative overlap expectation (FIXME)
            dsl.columns("columns3", 3) {
                ImGui.text("AAAA"); ImGui.nextColumn()
                ImGui.text("BBBB"); ImGui.nextColumn()
                ImGui.text("CCCC"); ImGui.nextColumn()
            }
            ImGui.text("Hello")
            assert(drawList.cmdBuffer.size == cmdCount || drawList.cmdBuffer.size == cmdCount + 3 + 1)

            // Test: Unused column don't consume a draw call
            cmdCount = drawList.cmdBuffer.size
            dsl.columns("columns3", 3) {
                ImGui.text("AAAA"); ImGui.nextColumn()
                ImGui.text("BBBB"); ImGui.nextColumn() // Leave one column empty
            }
            ImGui.text("Hello")
            assert(drawList.cmdBuffer.size == cmdCount || drawList.cmdBuffer.size == cmdCount + 2 + 1)

            // Test: Separators in columns don't consume a draw call
            cmdCount = drawList.cmdBuffer.size
            dsl.columns("columns3", 3) {
                ImGui.text("AAAA"); ImGui.nextColumn()
                ImGui.text("BBBB"); ImGui.nextColumn()
                ImGui.text("CCCC"); ImGui.nextColumn()
                ImGui.separator()
                ImGui.text("1111"); ImGui.nextColumn()
                ImGui.text("2222"); ImGui.nextColumn()
                ImGui.text("3333"); ImGui.nextColumn()
            }
            ImGui.text("Hello")
            assert(drawList.cmdBuffer.size == cmdCount || drawList.cmdBuffer.size == cmdCount + 3 + 1)

            // Test: Selectables in columns don't consume a draw call
            cmdCount = drawList.cmdBuffer.size
            dsl.columns("columns3", 3) {
                ImGui.selectable("AAAA", true, SelectableFlag.SpanAllColumns.i); ImGui.nextColumn()
                ImGui.text("BBBB"); ImGui.nextColumn()
                ImGui.text("CCCC"); ImGui.nextColumn()
                ImGui.selectable("1111", true, SelectableFlag.SpanAllColumns.i); ImGui.nextColumn()
                ImGui.text("2222"); ImGui.nextColumn()
                ImGui.text("3333"); ImGui.nextColumn()
            }
            ImGui.text("Hello")
            assert(drawList.cmdBuffer.size == cmdCount || drawList.cmdBuffer.size == cmdCount + 3 + 1)

            ImGui.end()
        }
    }

    // ## Test behavior of some Column functions without Columns/BeginColumns.
//    t = REGISTER_TEST("columns", "columns_functions_without_columns")
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::Begin("Test window 1", NULL, ImGuiWindowFlags_NoSavedSettings)
//        IM_CHECK_EQ(ImGui::GetColumnsCount(), 1)
//        IM_CHECK_EQ(ImGui::GetColumnOffset(), 0.0f)
//        IM_CHECK_EQ(ImGui::GetColumnWidth(), ImGui::GetContentRegionAvail().x)
//        IM_CHECK_EQ(ImGui::GetColumnIndex(), 0)
//        ImGui::End()
//    }
}