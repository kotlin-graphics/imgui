package app.tests

import engine.context.*
import engine.core.TestEngine
import engine.core.registerTest
import gli_.hasnt
import glm_.*
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.internal.*
import imgui.internal.classes.DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC
import kool.rem
import kotlin.math.cos
import kotlin.math.sin

//-------------------------------------------------------------------------
// Tests: Performance Tests
//-------------------------------------------------------------------------
// FIXME-TESTS: Maybe group and expose in a different spot of the UI?
// We currently don't call RegisterTests_Perf() by default because those are more costly.
//-------------------------------------------------------------------------

fun registerTests_Perf(e: TestEngine) {

    val perfCaptureFunc = { ctx: TestContext ->
        //ctx->KeyDownMap(ImGuiKey_COUNT, ImGuiKeyModFlags_Shift);
        ctx.perfCapture()
        //ctx->KeyUpMap(ImGuiKey_COUNT, ImGuiKeyModFlags_Shift);
    }

    // ## Measure the cost all demo contents
    e.registerTest("perf", "perf_demo_all").let { t ->
        t.testFunc = { ctx: TestContext ->

            ctx.perfCalcRef()

            ctx.windowRef("Dear ImGui Demo")
            ctx.itemOpenAll("")
            ctx.menuCheckAll("Examples")
            ctx.menuCheckAll("Tools")

            assert(ctx.uiContext!!.io.displaySize.x > 820)
            assert(ctx.uiContext!!.io.displaySize.y > 820)

            // FIXME-TESTS: Backup full layout
            val pos = ctx.mainViewportPos + 20
            for (window in ctx.uiContext!!.windows) {
                window.pos put pos
                window.sizeFull put 800
            }
            ctx.perfCapture()

            ctx.windowRef("Dear ImGui Demo")
            ctx.itemCloseAll("")
            ctx.menuUncheckAll("Examples")
            ctx.menuUncheckAll("Tools")
        }
    }

    val drawPrimFunc = { ctx: TestContext ->

        ImGui.begin("Test Func", null, WindowFlag.NoSavedSettings or WindowFlag.AlwaysAutoResize)
        val loopCount = 200 * ctx.perfStressAmount
        val drawList = ImGui.windowDrawList
        val segments = 0
        ImGui.button("##CircleFilled", Vec2(120))
        val boundsMin = ImGui.itemRectMin
        val boundsSize = ImGui.itemRectSize
        val center = boundsMin + boundsSize * 0.5f
        val r = ((boundsSize.x min boundsSize.y) * 0.8f * 0.5f).i.f
        val rounding = 8f
        val col = COL32(255, 255, 0, 255)
        val oldFlags = drawList.flags // Save old flags as some of these tests manipulate them
        when (DrawPrimFunc of ctx.test!!.argVariant) {
            DrawPrimFunc.RectStroke ->
                for (n in 0 until loopCount)
                    drawList.addRect(center - r, center + r, col, 0f, 0.inv(), 1f)
            DrawPrimFunc.RectStrokeThick ->
                for (n in 0 until loopCount)
                    drawList.addRect(center - r, center + r, col, 0f, 0.inv(), 4f)
            DrawPrimFunc.RectFilled ->
                for (n in 0 until loopCount)
                    drawList.addRectFilled(center - r, center + r, col, 0f)
            DrawPrimFunc.RectRoundedStroke ->
                for (n in 0 until loopCount)
                    drawList.addRect(center - r, center + r, col, rounding, 0.inv(), 1f)
            DrawPrimFunc.RectRoundedStrokeThick ->
                for (n in 0 until loopCount)
                    drawList.addRect(center - r, center + r, col, rounding, 0.inv(), 4f)
            DrawPrimFunc.RectRoundedFilled ->
                for (n in 0 until loopCount)
                    drawList.addRectFilled(center - r, center + r, col, rounding)
            DrawPrimFunc.CircleStroke ->
                for (n in 0 until loopCount)
                    drawList.addCircle(center, r, col, segments, 1f)
            DrawPrimFunc.CircleStrokeThick ->
                for (n in 0 until loopCount)
                    drawList.addCircle(center, r, col, segments, 4f)
            DrawPrimFunc.CircleFilled ->
                for (n in 0 until loopCount)
                    drawList.addCircleFilled(center, r, col, segments)
            DrawPrimFunc.TriangleStroke ->
                for (n in 0 until loopCount)
                    drawList.addNgon(center, r, col, 3, 1f)
            DrawPrimFunc.TriangleStrokeThick ->
                for (n in 0 until loopCount)
                    drawList.addNgon(center, r, col, 3, 4f)
            DrawPrimFunc.LongStroke ->
                drawList.addNgon(center, r, col, 10 * loopCount, 1f)
            DrawPrimFunc.LongStrokeThick ->
                drawList.addNgon(center, r, col, 10 * loopCount, 4f)
            DrawPrimFunc.LongJaggedStroke -> {
                var n = 0f
                while (n < 10f * loopCount) {
                    drawList.pathLineTo(center + Vec2(r * sin(n), r * cos(n)))
                    n += 2.51327412287f
                }
                drawList.pathStroke(col, false, 1f)
            }
            DrawPrimFunc.LongJaggedStrokeThick -> {
                var n = 0f
                while (n < 10f * loopCount) {
                    drawList.pathLineTo(center + Vec2(r * sin(n), r * cos(n)))
                    n += 2.51327412287f
                }
                drawList.pathStroke(col, false, 4f)
            }
            DrawPrimFunc.Line -> {
                drawList.flags = drawList.flags wo DrawListFlag.AntiAliasedLines
                for (n in 0 until loopCount)
                    drawList.addLine(center - r, center + r, col, 1f)
            }
            DrawPrimFunc.LineAA -> {
                drawList.flags = drawList.flags or DrawListFlag.AntiAliasedLines
                for (n in 0 until loopCount)
                    drawList.addLine(center - r, center + r, col, 1f)
            }
//                #ifdef IMGUI_HAS_TEXLINES
//                    case DrawPrimFunc_LineAANoTex:
//            draw_list->Flags |= ImDrawListFlags_AntiAliasedLines;
//            draw_list->Flags &= ~ImDrawListFlags_AntiAliasedLinesUseTexData;
//            for (int n = 0; n < loop_count; n++)
//            draw_list->AddLine(center - ImVec2(r, r), center + ImVec2(r, r), col, 1.0f);
//            break;
//                #endif
            DrawPrimFunc.LineThick -> {
                drawList.flags = drawList.flags wo DrawListFlag.AntiAliasedLines
                for (n in 0 until loopCount)
                    drawList.addLine(center - r, center + r, col, 4f)
            }
            DrawPrimFunc.LineThickAA -> {
                drawList.flags = drawList.flags or DrawListFlag.AntiAliasedLines
                for (n in 0 until loopCount)
                    drawList.addLine(center - r, center + r, col, 4f)
            }
//                #ifdef IMGUI_HAS_TEXLINES
//                    case DrawPrimFunc_LineThickAANoTex:
//            draw_list->Flags |= ImDrawListFlags_AntiAliasedLines;
//            draw_list->Flags &= ~ImDrawListFlags_AntiAliasedLinesUseTexData;
//            for (int n = 0; n < loop_count; n++)
//            draw_list->AddLine(center - ImVec2(r, r), center + ImVec2(r, r), col, 4.0f);
//            break;
//                #endif
            else -> assert(false)
        }
        drawList.flags = oldFlags // Restre flags
        ImGui.end()
    }

    e.registerTest("perf", "perf_draw_prim_rect_stroke").let { t ->
        t.argVariant = DrawPrimFunc.RectStroke.ordinal
        t.guiFunc = drawPrimFunc
        t.testFunc = perfCaptureFunc
    }

    e.registerTest("perf", "perf_draw_prim_rect_stroke_thick").let { t ->
        t.argVariant = DrawPrimFunc.RectStrokeThick.ordinal
        t.guiFunc = drawPrimFunc
        t.testFunc = perfCaptureFunc
    }

    e.registerTest("perf", "perf_draw_prim_rect_filled").let { t ->
        t.argVariant = DrawPrimFunc.RectFilled.ordinal
        t.guiFunc = drawPrimFunc
        t.testFunc = perfCaptureFunc
    }

    e.registerTest("perf", "perf_draw_prim_rect_rounded_stroke").let { t ->
        t.argVariant = DrawPrimFunc.RectRoundedStroke.ordinal
        t.guiFunc = drawPrimFunc
        t.testFunc = perfCaptureFunc
    }

    e.registerTest("perf", "perf_draw_prim_rect_rounded_stroke_thick").let { t ->
        t.argVariant = DrawPrimFunc.RectRoundedStrokeThick.ordinal
        t.guiFunc = drawPrimFunc
        t.testFunc = perfCaptureFunc
    }

    e.registerTest("perf", "perf_draw_prim_rect_rounded_filled").let { t ->
        t.argVariant = DrawPrimFunc.RectRoundedFilled.ordinal
        t.guiFunc = drawPrimFunc
        t.testFunc = perfCaptureFunc
    }

    e.registerTest("perf", "perf_draw_prim_circle_stroke").let { t ->
        t.argVariant = DrawPrimFunc.CircleStroke.ordinal
        t.guiFunc = drawPrimFunc
        t.testFunc = perfCaptureFunc
    }

    e.registerTest("perf", "perf_draw_prim_circle_stroke_thick").let { t ->
        t.argVariant = DrawPrimFunc.CircleStrokeThick.ordinal
        t.guiFunc = drawPrimFunc
        t.testFunc = perfCaptureFunc
    }

    e.registerTest("perf", "perf_draw_prim_circle_filled").let { t ->
        t.argVariant = DrawPrimFunc.CircleFilled.ordinal
        t.guiFunc = drawPrimFunc
        t.testFunc = perfCaptureFunc
    }

    e.registerTest("perf", "perf_draw_prim_triangle_stroke").let { t ->
        t.argVariant = DrawPrimFunc.TriangleStroke.ordinal
        t.guiFunc = drawPrimFunc
        t.testFunc = perfCaptureFunc
    }

    e.registerTest("perf", "perf_draw_prim_triangle_stroke_thick").let { t ->
        t.argVariant = DrawPrimFunc.TriangleStrokeThick.ordinal
        t.guiFunc = drawPrimFunc
        t.testFunc = perfCaptureFunc
    }

    e.registerTest("perf", "perf_draw_prim_long_stroke").let { t ->
        t.argVariant = DrawPrimFunc.LongStroke.ordinal
        t.guiFunc = drawPrimFunc
        t.testFunc = perfCaptureFunc
    }

    e.registerTest("perf", "perf_draw_prim_long_stroke_thick").let { t ->
        t.argVariant = DrawPrimFunc.LongStrokeThick.ordinal
        t.guiFunc = drawPrimFunc
        t.testFunc = perfCaptureFunc
    }

    e.registerTest("perf", "perf_draw_prim_long_jagged_stroke").let { t ->
        t.argVariant = DrawPrimFunc.LongJaggedStroke.ordinal
        t.guiFunc = drawPrimFunc
        t.testFunc = perfCaptureFunc
    }

    e.registerTest("perf", "perf_draw_prim_long_jagged_stroke_thick").let { t ->
        t.argVariant = DrawPrimFunc.LongJaggedStrokeThick.ordinal
        t.guiFunc = drawPrimFunc
        t.testFunc = perfCaptureFunc
    }

    e.registerTest("perf", "perf_draw_prim_line").let { t ->
        t.argVariant = DrawPrimFunc.Line.ordinal
        t.guiFunc = drawPrimFunc
        t.testFunc = perfCaptureFunc
    }

    e.registerTest("perf", "perf_draw_prim_line_antialiased").let { t ->
        t.argVariant = DrawPrimFunc.LineAA.ordinal
        t.guiFunc = drawPrimFunc
        t.testFunc = perfCaptureFunc
    }

//    #ifdef IMGUI_HAS_TEXLINES
//        t = REGISTER_TEST("perf", "perf_draw_prim_line_antialiased_no_tex");
//    t->ArgVariant = DrawPrimFunc_LineAANoTex;
//    t->GuiFunc = DrawPrimFunc;
//    t->TestFunc = PerfCaptureFunc;
//    #endif

    e.registerTest("perf", "perf_draw_prim_line_thick").let { t ->
        t.argVariant = DrawPrimFunc.LineThick.ordinal
        t.guiFunc = drawPrimFunc
        t.testFunc = perfCaptureFunc
    }

    e.registerTest("perf", "perf_draw_prim_line_thick_antialiased").let { t ->
        t.argVariant = DrawPrimFunc.LineThickAA.ordinal
        t.guiFunc = drawPrimFunc
        t.testFunc = perfCaptureFunc
    }

//    #ifdef IMGUI_HAS_TEXLINES
//        t = REGISTER_TEST("perf", "perf_draw_prim_line_thick_antialiased_no_tex");
//    t->ArgVariant = DrawPrimFunc_LineThickAANoTex;
//    t->GuiFunc = DrawPrimFunc;
//    t->TestFunc = PerfCaptureFunc;
//    #endif

    // ## Measure the cost of ImDrawListSplitter split/merge functions
    val drawSplittedFunc = { ctx: TestContext ->

        ImGui.begin("Test Func", null, WindowFlag.NoSavedSettings or WindowFlag.AlwaysAutoResize)
        val drawList = ImGui.windowDrawList

        val splitCount = ctx.test!!.argVariant
        var loopCount = 200 * ctx.perfStressAmount

        ImGui.button("##CircleFilled", Vec2(200))
        val boundsMin = ImGui.itemRectMin
        val boundsSize = ImGui.itemRectSize
        val center = boundsMin + boundsSize * 0.5f
        val r = ((boundsSize.x min boundsSize.y) * 0.8f * 0.5f).i.f

        if (ctx.frameCount == 0)
            ctx.logDebug("$loopCount primitives over $splitCount channels")
        if (splitCount != 1) {
            assert(loopCount % splitCount == 0)
            loopCount /= splitCount
            drawList.channelsSplit(splitCount)
            for (n in 0 until loopCount)
                for (ch in 0 until splitCount) {
                    drawList.channelsSetCurrent(ch)
                    drawList.addCircleFilled(center, r, COL32(255, 255, 0, 255), 12)
                }
            drawList.channelsMerge()
        } else
            for (n in 0 until loopCount)
                drawList.addCircleFilled(center, r, COL32(255, 255, 0, 255), 12)
        if (ctx.frameCount == 0)
            ctx.logDebug("Vertices: ${drawList.vtxBuffer.size}")

        ImGui.end()
    }

    e.registerTest("perf", "perf_draw_split_1").let { t ->
        t.argVariant = 1
        t.guiFunc = drawSplittedFunc
        t.testFunc = perfCaptureFunc
    }

    e.registerTest("perf", "perf_draw_split_0").let { t ->
        t.argVariant = 10
        t.guiFunc = drawSplittedFunc
        t.testFunc = perfCaptureFunc
    }

    // ## Measure the cost of simple Button() calls
    e.registerTest("perf", "perf_stress_button").let { t ->
        t.guiFunc = { ctx: TestContext ->
            dsl.window("Test Func", null, WindowFlag.NoSavedSettings or WindowFlag.AlwaysAutoResize) {
                val loopCount = 1000 * ctx.perfStressAmount
                for (n in 0 until loopCount)
                    ImGui.button("Hello, world")
            }
        }
        t.testFunc = perfCaptureFunc
    }

    // ## Measure the cost of simple Checkbox() calls
    e.registerTest("perf", "perf_stress_checkbox").let { t ->
        t.guiFunc = { ctx: TestContext ->
            dsl.window("Test Func", null, WindowFlag.NoSavedSettings or WindowFlag.AlwaysAutoResize) {
                val loopCount = 1000 * ctx.perfStressAmount
//                bool v1 = false, v2 = true
                ctx.genericVars.bool1 = false
                ctx.genericVars.bool2 = true
                for (n in 0 until loopCount / 2)
                    dsl.withId(n) {
                        ImGui.checkbox("Hello, world", ctx.genericVars::bool1)
                        ImGui.checkbox("Hello, world", ctx.genericVars::bool2)
                    }
            }
        }
        t.testFunc = perfCaptureFunc
    }

    // ## Measure the cost of dumb column-like setup using SameLine()
    e.registerTest("perf", "perf_stress_rows_1a").let { t ->
        t.guiFunc = { ctx: TestContext ->
            ImGui.setNextWindowSize(Vec2(400, 0))
            dsl.window("Test Func", null, WindowFlag.NoSavedSettings or WindowFlag.AlwaysAutoResize) {
                val loopCount = 50 * 2 * ctx.perfStressAmount
                for (n in 0 until loopCount) {
                    ImGui.textUnformatted("Cell 1")
                    ImGui.sameLine(100)
                    ImGui.textUnformatted("Cell 2")
                    ImGui.sameLine(200)
                    ImGui.textUnformatted("Cell 3")
                }
                ImGui.columns(1)
            }
        }
        t.testFunc = perfCaptureFunc
    }

    e.registerTest("perf", "perf_stress_rows_1b").let { t ->
        t.guiFunc = { ctx: TestContext ->
            ImGui.setNextWindowSize(Vec2(400, 0))
            dsl.window("Test Func", null, WindowFlag.NoSavedSettings or WindowFlag.AlwaysAutoResize) {
                val loopCount = 50 * 2 * ctx.perfStressAmount
                for (n in 0 until loopCount) {
                    ImGui.text("Cell 1")
                    ImGui.sameLine(100)
                    ImGui.text("Cell 2")
                    ImGui.sameLine(200)
                    ImGui.text("Cell 3")
                }
                ImGui.columns(1)
            }
        }
        t.testFunc = perfCaptureFunc
    }

    // ## Measure the cost of NextColumn(): one column set, many rows
    e.registerTest("perf", "perf_stress_columns_1").let { t ->
        t.guiFunc = { ctx: TestContext ->
            ImGui.setNextWindowSize(Vec2(400, 0))
            dsl.window("Test Func", null, WindowFlag.NoSavedSettings or WindowFlag.AlwaysAutoResize) {
                ImGui.columns(3, "Columns", true)
                val loopCount = 50 * 2 * ctx.perfStressAmount
                for (n in 0 until loopCount) {
                    ImGui.text("Cell 1,%d", n)
                    ImGui.nextColumn()
                    ImGui.textUnformatted("Cell 2")
                    ImGui.nextColumn()
                    ImGui.textUnformatted("Cell 3")
                    ImGui.nextColumn()
                }
                ImGui.columns(1)
            }
        }
        t.testFunc = perfCaptureFunc
    }

    // ## Measure the cost of Columns(): many columns sets, few rows
    e.registerTest("perf", "perf_stress_columns_2").let { t ->
        t.guiFunc = { ctx: TestContext ->
            ImGui.setNextWindowSize(Vec2(400, 0))
            dsl.window("Test Func", null, WindowFlag.NoSavedSettings or WindowFlag.AlwaysAutoResize) {
                val loopCount = 50 * ctx.perfStressAmount
                for (n in 0 until loopCount) {
                    ImGui.pushID(n)
                    ImGui.columns(3, "Columns", true)
                    for (row in 0..1) {
                        ImGui.text("Cell 1,$n")
                        ImGui.nextColumn()
                        ImGui.textUnformatted("Cell 2")
                        ImGui.nextColumn()
                        ImGui.textUnformatted("Cell 3")
                        ImGui.nextColumn()
                    }
                    ImGui.columns(1)
                    ImGui.separator()
                    ImGui.popID()
                }
            }
        }
        t.testFunc = perfCaptureFunc
    }

//    #ifdef helpers.getIMGUI_HAS_TABLE
//        // ## Measure the cost of TableNextCell(), TableNextRow(): one table, many rows
//        t = REGISTER_TEST("perf", "perf_stress_table_1");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::SetNextWindowSize(ImVec2(400, 0));
//        ImGui::Begin("Test Func", NULL, ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_AlwaysAutoResize);
//        int loop_count = 50 * 2 * ctx->PerfStressAmount;
//        if (ImGui::BeginTable("Table", 3, ImGuiTableFlags_BordersV))
//        {
//            for (int n = 0; n < loop_count; n++)
//            {
//                ImGui::TableNextCell();
//                ImGui::Text("Cell 1,%d", n);
//                ImGui::TableNextCell();
//                ImGui::TextUnformatted("Cell 2");
//                ImGui::TableNextCell();
//                ImGui::TextUnformatted("Cell 3");
//            }
//            ImGui::EndTable();
//        }
//        ImGui::End();
//    };
//    t->TestFunc = PerfCaptureFunc;
//
//    // ## Measure the cost of BeginTable(): many tables with few rows
//    t = REGISTER_TEST("perf", "perf_stress_table_2");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::SetNextWindowSize(ImVec2(400, 0));
//        ImGui::Begin("Test Func", NULL, ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_AlwaysAutoResize);
//        int loop_count = 50 * ctx->PerfStressAmount;
//        for (int n = 0; n < loop_count; n++)
//        {
//            ImGui::PushID(n);
//            if (ImGui::BeginTable("Table", 3, ImGuiTableFlags_BordersV))
//            {
//                for (int row = 0; row < 2; row++)
//                {
//                    ImGui::TableNextCell();
//                    ImGui::Text("Cell 1,%d", n);
//                    ImGui::TableNextCell();
//                    ImGui::TextUnformatted("Cell 2");
//                    ImGui::TableNextCell();
//                    ImGui::TextUnformatted("Cell 3");
//                }
//                ImGui::EndTable();
//            }
//            ImGui::Separator();
//            ImGui::PopID();
//        }
//        ImGui::End();
//    };
//    t->TestFunc = PerfCaptureFunc;
//    #endif // helpers.getIMGUI_HAS_TABLE

    // ## Measure the cost of simple ColorEdit4() calls (multi-component, group based widgets are quite heavy)
    e.registerTest("perf", "perf_stress_coloredit4").let { t ->
        t.guiFunc = { ctx: TestContext ->
            dsl.window("Test Func", null, WindowFlag.NoSavedSettings or WindowFlag.AlwaysAutoResize) {
                val loopCount = 500 * ctx.perfStressAmount
                val col = Vec4(1f, 0f, 0f, 1f)
                for (n in 0 until loopCount / 2)
                    dsl.withId(n) {
                        ImGui.colorEdit4("Color", col)
                    }
            }
        }
        t.testFunc = perfCaptureFunc
    }

    // ## Measure the cost of simple InputText() calls
    e.registerTest("perf", "perf_stress_input_text").let { t ->
        t.guiFunc = { ctx: TestContext ->
            dsl.window("Test Func", null, WindowFlag.NoSavedSettings or WindowFlag.AlwaysAutoResize) {
                val loopCount = 1000 * ctx.perfStressAmount
                val buf = "123".toByteArray(32)
                for (n in 0 until loopCount)
                    dsl.withId(n) {
                        ImGui.inputText("InputText", buf, InputTextFlag.None.i)
                    }
            }
        }
        t.testFunc = perfCaptureFunc
    }

    // ## Measure the cost of simple InputTextMultiline() calls
    // (this is creating a child window for every non-clipped widget, so doesn't scale very well)
    e.registerTest("perf", "perf_stress_input_text_multiline").let { t ->
        t.guiFunc = { ctx: TestContext ->
            dsl.window("Test Func", null, WindowFlag.NoSavedSettings or WindowFlag.AlwaysAutoResize) {
                val loopCount = 1000 * ctx.perfStressAmount
                val buf = "123".toByteArray(32)
                for (n in 0 until loopCount)
                    dsl.withId(n) {
                        ImGui.inputTextMultiline("InputText", buf, Vec2(0f, ImGui.frameHeightWithSpacing * 2), InputTextFlag.None.i)
                    }
            }
        }
        t.testFunc = perfCaptureFunc
    }

    // ## Measure the cost of our ImHashXXX functions
    e.registerTest("perf", "perf_stress_hash").let { t ->
        t.guiFunc = { ctx: TestContext ->
            dsl.window("Test Func", null, WindowFlag.NoSavedSettings or WindowFlag.AlwaysAutoResize) {
                ImGui.text("Hashing..")
                val loopCount = 5000 * ctx.perfStressAmount
                val buf = ByteArray(32)
                var seed = 0
                for (n in 0 until loopCount) {
                    seed = hash(buf, seed)
                    seed = hash("Hash me tender", 0, seed)
                    seed = hash("Hash me true", 12, seed)
                }
            }
        }
        t.testFunc = perfCaptureFunc
    }

    // ## Measure the cost of simple Listbox() calls
    // (this is creating a child window for every non-clipped widget, so doesn't scale very well)
    e.registerTest("perf", "perf_stress_list_box").let { t ->
        t.guiFunc = { ctx: TestContext ->
            dsl.window("Test Func", null, WindowFlag.NoSavedSettings or WindowFlag.AlwaysAutoResize) {
                val loopCount = 1000 * ctx.perfStressAmount
                for (n in 0 until loopCount)
                    dsl.withId(n) {
                        dsl.listBox("ListBox", Vec2(0f, ImGui.frameHeightWithSpacing * 2)) {
                            ImGui.menuItem("Hello")
                            ImGui.menuItem("World")
                        }
                    }
            }
        }
        t.testFunc = perfCaptureFunc
    }

    // ## Measure the cost of simple SliderFloat() calls
    e.registerTest("perf", "perf_stress_slider").let { t ->
        t.guiFunc = { ctx: TestContext ->
            dsl.window("Test Func", null, WindowFlag.NoSavedSettings or WindowFlag.AlwaysAutoResize) {
                val loopCount = 1000 * ctx.perfStressAmount
//            float f = 1.234f
                ctx.genericVars.float1 = 1.234f
                for (n in 0 until loopCount)
                    dsl.withId(n) {
                        ImGui.sliderFloat("SliderFloat", ctx.genericVars::float1, 0f, 10f)
                    }
            }
        }
        t.testFunc = perfCaptureFunc
    }

    // ## Measure the cost of simple SliderFloat2() calls
    // This at a glance by compared to SliderFloat() test shows us the overhead of group-based multi-component widgets
    e.registerTest("perf", "perf_stress_slider2").let { t ->
        t.guiFunc = { ctx: TestContext ->
            dsl.window("Test Func", null, WindowFlag.NoSavedSettings or WindowFlag.AlwaysAutoResize) {
                val loopCount = 1000 * ctx.perfStressAmount
                val v = Vec2()
                for (n in 0 until loopCount)
                    dsl.withId(n) {
                        ImGui.sliderVec2("SliderFloat2", v, 0f, 10f)
                    }
            }
        }
        t.testFunc = perfCaptureFunc
    }

    // ## Measure the cost of TextUnformatted() calls with relatively short text
    e.registerTest("perf", "perf_stress_text_unformatted_1").let { t ->
        t.guiFunc = { ctx: TestContext ->
            dsl.window("Test Func", null, WindowFlag.NoSavedSettings or WindowFlag.AlwaysAutoResize) {
                val buf = """
                0123456789 The quick brown fox jumps over the lazy dog.
                0123456789   The quick brown fox jumps over the lazy dog.
                0123456789     The quick brown fox jumps over the lazy dog.""".trimIndent()
                val loopCount = 1000 * ctx.perfStressAmount
                for (n in 0 until loopCount)
                    ImGui.textUnformatted(buf)
            }
        }
        t.testFunc = perfCaptureFunc
    }

    // ## Measure the cost of TextUnformatted() calls with long text
    e.registerTest("perf", "perf_stress_text_unformatted_2").let { t ->
        t.guiFunc = { ctx: TestContext ->
            dsl.window("Test Func", null, WindowFlag.NoSavedSettings or WindowFlag.AlwaysAutoResize) {
                if (textBuffer.isEmpty())
                    for (i in 0..999)
                        textBuffer += "$i The quick brown fox jumps over the lazy dog\n"
                val loopCount = 100 * ctx.perfStressAmount
                for (n in 0 until loopCount)
                    ImGui.textUnformatted(textBuffer.toString())
            }
        }
        t.testFunc = perfCaptureFunc
    }

    // ## Measure the cost/overhead of creating too many windows
    e.registerTest("perf", "perf_stress_window").let { t ->
        t.guiFunc = { ctx: TestContext ->
            val pos = ctx.mainViewportPos + 20
            val loopCount = 200 * ctx.perfStressAmount
            for (n in 0 until loopCount) {
                ImGui.setNextWindowPos(pos)
                dsl.window("Window_%05d".format(n + 1), null, WindowFlag.NoSavedSettings or WindowFlag.AlwaysAutoResize) {
                    ImGui.textUnformatted("Opening many windows!")
                }
            }
        }
        t.testFunc = perfCaptureFunc
    }

    // ## Circle segment count comparisons
    e.registerTest("perf", "perf_circle_segment_counts").let { t ->
        t.guiFunc = { ctx: TestContext ->
            val numCols = 3 // Number of columns to draw
            val numRows = 3 // Number of rows to draw
            val maxRadius = 400f // Maximum allowed radius

            // ImGui::SetNextWindowSize(ImVec2((num_cols + 0.5f) * item_spacing.x, (num_rows * item_spacing.y) + 128.0f));
            if (ImGui.begin("perf_circle_segment_counts", null, WindowFlag.HorizontalScrollbar.i)) {
                // Control area
                ImGui.sliderFloat("Line width", ::lineWidth0, 1f, 10f)
                ImGui.sliderFloat("Radius", ::radius, 1f, maxRadius)
                ImGui.sliderInt("Segments", ::segmentCountManual, 1, 512)
                ImGui.dragFloat("Circle segment Max Error", ImGui.style::circleSegmentMaxError, 0.01f, 0.1f, 10f, "%.2f", 1f)
                ImGui.checkbox("No anti-aliasing", ::noAA)
                ImGui.sameLine()
                ImGui.checkbox("Overdraw", ::overdraw)
                if (ImGui.isItemHovered())
                    ImGui.setTooltip("Draws each primitive repeatedly so that stray low-alpha pixels are easier to spot")
                ImGui.sameLine()
                val fillModes = arrayOf("Stroke", "Fill", "Stroke+Fill", "Fill+Stroke")
                ImGui.setNextItemWidth(128f)
                ImGui.combo("Fill mode", ::fillMode, fillModes)

                ImGui.colorEdit4("Color BG", colorBg)
                ImGui.colorEdit4("Color FG", colorFg)

                // Display area
                ImGui.setNextWindowContentSize(contentSize)
                ImGui.beginChild("Display", ImGui.contentRegionAvail, false, WindowFlag.HorizontalScrollbar.i)
                val drawList = ImGui.windowDrawList

                // Set up the grid layout
                val spacing = 96f max (radius * 2f + lineWidth0 + 8f) // Spacing between rows/columns
                val cursorPos = ImGui.cursorScreenPos // [JVM] no copy

                // Draw the first <n> radius/segment size pairs in a quasi-logarithmic down the side
                var pairRad = 1
                var step = 1
                while (pairRad <= 512) {
                    val segmentCount = DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC(pairRad.f, drawList._data.circleSegmentMaxError)
                    ImGui.textColored(if (pairRad == radius.i) colorBg else colorFg, "Rad $pairRad = $segmentCount segs")
                    if (pairRad >= 16 && (pairRad hasnt (pairRad - 1)))
                        step *= 2
                    pairRad += step
                }

                // Calculate the worst-case width for the size pairs
                val maxPairWidth = ImGui.calcTextSize("Rad 0000 = 0000 segs").x

                val textStandoff = Vec2(maxPairWidth + 64f, 16f) // How much space to leave for the size list and labels
                val basePos = Vec2(cursorPos.x + spacing * 0.5f + textStandoff.x, cursorPos.y + textStandoff.y)

                // Update content size for next frame
                contentSize.put(cursorPos.x + spacing * numCols + textStandoff.x, (cursorPos.y + spacing * numRows + textStandoff.y) max ImGui.cursorPosY)

                // Save old flags
                val backupDrawListFlags = drawList.flags
                if (noAA)
                    drawList.flags = drawList.flags wo DrawListFlag.AntiAliasedLines

                // Get the suggested segment count for this radius
                val segmentCountSuggested = DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC(radius, drawList._data.circleSegmentMaxError)
                val segmentCountByColumn = intArrayOf(segmentCountSuggested, segmentCountManual, 512)
                val descByColumn = arrayOf("auto", "manual", "max")

                // Draw row/column labels
                for (i in 0 until numCols) {
                    val name = "${descByColumn[i]}\n${segmentCountByColumn[i]} segs"
                    val pos = Vec2(cursorPos.x + maxPairWidth + 8f, cursorPos.y + textStandoff.y + (i + 0.5f) * spacing)
                    drawList.addText(pos, ImGui.getColorU32(colorFg), name)
                    pos.put(cursorPos.x + textStandoff.x + (i + 0.2f) * spacing, cursorPos.y)
                    drawList.addText(pos, ImGui.getColorU32(colorBg), name)
                }

                // Draw circles
                for (y in 0 until numRows)
                    for (x in 0 until numCols) {
                        val center = Vec2(basePos.x + spacing * x, basePos.y + spacing * (y + 0.5f))
                        for (pass in 0..1) {
                            val typeIndex = if (pass == 0) x else y
                            val color = if (pass == 0) colorBg else colorFg
                            val numSegmentCount = segmentCountByColumn[typeIndex]
                            val numShapesToDraw = if (overdraw) 20 else 1

                            // We fill either if fill mode was selected, or in stroke+fill mode for the first pass (so we can see what varying segment count fill+stroke looks like)
                            val fill = fillMode == 1 || (fillMode == 2 && pass == 1) || (fillMode == 3 && pass == 0)
                            for (i in 0 until numShapesToDraw)
                                if (fill)
                                    drawList.addCircleFilled(center, radius, color.u32, numSegmentCount)
                                else
                                    drawList.addCircle(center, radius, color.u32, numSegmentCount, lineWidth0)
                        }
                    }

                // Restore draw list flags
                drawList.flags = backupDrawListFlags
                ImGui.endChild()
            }
            ImGui.end()
        }
        t.testFunc = perfCaptureFunc
    }

    // ## Draw various AA/non-AA lines (not really a perf test, more a correctness one)
    e.registerTest("perf", "perf_misc_lines").let { t ->
        t.guiFunc = { ctx: TestContext ->
            val numCols = 16 // Number of columns (line rotations) to draw
            val numRows = 3 // Number of rows (line variants) to draw
            val lineLen = 64f // Length of line to draw
            val lineSpacing = Vec2(80f, 96f) // Spacing between lines

            ImGui.setNextWindowSize(Vec2((numCols + 0.5f) * lineSpacing.x, numRows * 2 * lineSpacing.y + 128f), Cond.Once)
            if (ImGui.begin("perf_misc_lines")) {

                val drawList = ImGui.windowDrawList

                ImGui.sliderFloat("Base rotation", ::baseRot, 0f, 360f)
                ImGui.sliderFloat("Line width", ::lineWidth1, 1f, 10f)

                ImGui.text("Press SHIFT to toggle textured/non-textured rows")
                val texToggle = ImGui.io.keyShift

                // Rotating lines
                val cursorPos = ImGui.cursorPos
                val cursorScreenPos = ImGui.cursorScreenPos
                val basePos = Vec2(cursorScreenPos.x + lineSpacing.x * 0.5f, cursorScreenPos.y)

//            #ifndef IMGUI_HAS_TEXLINES
                val antiAliasedLinesUseTexData = DrawListFlag.None
//            #endif

                for (i in 0 until numRows) {

                    val rowIdx = when {
                        texToggle -> when (i) {
                            1 -> 2
                            2 -> 1
                            else -> i
                        }
                        else -> i
                    }

                    val name = when (rowIdx) {
                        0 -> {
                            drawList.flags = drawList.flags wo DrawListFlag.AntiAliasedLines
                            "No AA"
                        }
                        1 -> {
                            drawList.flags = drawList.flags or DrawListFlag.AntiAliasedLines
                            drawList.flags = drawList.flags wo antiAliasedLinesUseTexData
                            "AA no texturing"
                        }
                        2 -> {
                            drawList.flags = drawList.flags or DrawListFlag.AntiAliasedLines
                            drawList.flags = drawList.flags or antiAliasedLinesUseTexData
                            "AA w/ texturing"
                        }
                        else -> ""
                    }

                    val initialVtxCount = drawList.vtxBuffer.size
                    val initialIdxCount = drawList.idxBuffer.rem

                    for (j in 0 until numCols) {

                        val r = baseRot * glm.πf / 180f + (j * glm.πf * 0.5f) / (numCols - 1)

                        val center = Vec2(basePos.x + lineSpacing.x * j, basePos.y + lineSpacing.y * (i + 0.5f))
                        val start = Vec2(center.x + sin(r) * lineLen * 0.5f, center.y + cos(r) * lineLen * 0.5f)
                        val end = Vec2(center.x - sin(r) * lineLen * 0.5f, center.y - cos(r) * lineLen * 0.5f)

                        drawList.addLine(start, end, COL32(255, 255, 255, 255), lineWidth1)
                    }

                    ImGui.cursorPosY = cursorPos.y + i * lineSpacing.y
                    ImGui.text("$name - ${drawList.vtxBuffer.size - initialVtxCount} vertices, ${drawList.idxBuffer.rem - initialIdxCount} indices")
                }

                ImGui.cursorPosY = cursorPos.y + numRows * lineSpacing.y

                // Squares
//                cursorPos = ImGui.cursorPos()
//                cursorScreenPos = ImGui::GetCursorScreenPos()
//                basePos = ImVec2(cursorScreenPos.x + (lineSpacing.x * 0.5f), cursorScreenPos.y)

                for (i in 0 until numRows) {

                    val rowIdx = when {
                        texToggle -> when (i) {
                            1 -> 2
                            2 -> 1
                            else -> i
                        }
                        else -> i
                    }

                    val name = when (rowIdx) {
                        0 -> {
                            drawList.flags = drawList.flags wo DrawListFlag.AntiAliasedLines
                            "No AA"
                        }
                        1 -> {
                            drawList.flags = drawList.flags or DrawListFlag.AntiAliasedLines
                            drawList.flags = drawList.flags wo antiAliasedLinesUseTexData
                            "AA no texturing"
                        }
                        2 -> {
                            drawList.flags = drawList.flags or DrawListFlag.AntiAliasedLines
                            drawList.flags = drawList.flags or antiAliasedLinesUseTexData
                            "AA w/ texturing"
                        }
                        else -> ""
                    }

                    val initialVtxCount = drawList.vtxBuffer.size
                    val initialIdxCount = drawList.idxBuffer.rem

                    for (j in 0 until numCols) {

                        val cellLineWidth = lineWidth1 + (j * 4f) / (numCols - 1)

                        val center = Vec2(basePos.x + lineSpacing.x * j, basePos.y + lineSpacing.y * (i + 0.5f))
                        val topLeft = Vec2(center.x - lineLen * 0.5f, center.y - lineLen * 0.5f)
                        val bottomRight = Vec2(center.x + lineLen * 0.5f, center.y + lineLen * 0.5f)

                        drawList.addRect(topLeft, bottomRight, COL32(255, 255, 255, 255), 0f, DrawCornerFlag.All.i, cellLineWidth)

                        ImGui.cursorPos = Vec2(cursorPos.x + (j + 0.5f) * lineSpacing.x - 16f, cursorPos.y + (i + 0.5f) * lineSpacing.y - ImGui.textLineHeight * 0.5f)
                        ImGui.text("%.2f", cellLineWidth)
                    }

                    ImGui.cursorPosY = cursorPos.y + i * lineSpacing.y
                    ImGui.text("$name - ${drawList.vtxBuffer.size - initialVtxCount} vertices, ${drawList.idxBuffer.rem - initialIdxCount} indices")
                }
            }
            ImGui.end()
        }
        t.testFunc = perfCaptureFunc
    }

    // ## Measure performance of drawlist text rendering
//    val measureTextRenderingPerf = { ctx: TestContext -> TODO kotlin bug
//
//        ImGui.setNextWindowSize(Vec2(300, 120), Cond.Always)
//        ImGui.begin("Test Func", null, WindowFlag.NoSavedSettings.i)
//
//        val window = ImGui.currentWindow
//        val drawList = ImGui.windowDrawList
//        val testVariant = ctx.test!!.argVariant
//        var str by ctx.genericVars::strLarge
//        var wrapWidth = 0f
//        var lineNum by ctx.genericVars::int1
//        var cpuFineClipRect: Vec4? = null
//        val textSize = ctx.genericVars.vec2
//        val windowPadding = ImGui.cursorScreenPos - window.pos
//
//        if (testVariant has PerfTestTextFlag.WithWrapWidth.i)
//            wrapWidth = 250f
//
//        if (testVariant has PerfTestTextFlag.WithCpuFineClipRect.i)
//            cpuFineClipRect = ctx.genericVars.vec4
//
//        // Set up test string.
//        if (ctx.genericVars.strLarge.isEmpty()) {
//            when {
//                testVariant has PerfTestTextFlag.TextLong.i -> {
//                    lineNum = 6
//                    str = ByteArray(2000)
//                }
//                testVariant has PerfTestTextFlag.TextWayTooLong.i -> {
//                    lineNum = 1
//                    str = ByteArray(10000)
//                }
//                else -> {
//                    lineNum = 400
//                    str = ByteArray(30)
//                }
//            }
//            // Support variable stress.
//            lineNum *= ctx.perfStressAmount
//            // Create test string.
//            str.fill('f'.b)
//            for (i in 14 until str.size step 15)
//                str[i] = ' '.b      // Spaces for word wrap.
////            str.back() = 0             // Null-terminate
//            // Measure text size and cache result.
//            textSize put ImGui.calcTextSize(str, 0, str.size, false, wrapWidth)
//            // Set up a cpu fine clip rect which should be about half of rect rendered text would occupy.
//            if (testVariant has PerfTestTextFlag.WithCpuFineClipRect.i)
//                cpuFineClipRect!!.put(
//                        window.pos.x + windowPadding.x,
//                        window.pos.y + windowPadding.y,
//                        window.pos.x + windowPadding.x + textSize.x * 0.5f,
//                        window.pos.y + windowPadding.y + textSize.y * lineNum * 0.5f)
//        }
//
//        ImGui.pushClipRect(Vec2(-Float.MAX_VALUE), Vec2(Float.MAX_VALUE), false)
//        var i = 0
//        var end = lineNum
//        while (i < end) {
//            val pos = window.pos + Vec2(windowPadding.x, windowPadding.y + textSize.y * i)
//            drawList.addText(null, 0f, pos, COL32_WHITE, str, 0, str.size, wrapWidth, cpuFineClipRect)
//            i++
//        }
//        ImGui.popClipRect()
//
//        ImGui.end()
//    }
//    val baseName = "perf_draw_text"
//    arrayOf("_short", "_long", "_too_long").forEachIndexed { i, textSuffix ->
//        arrayOf("", "_wrapped").forEachIndexed { j, wrapSuffix ->
//            arrayOf("", "_clipped").forEachIndexed { k, clipSuffix ->
//                val testName = "$baseName$textSuffix$wrapSuffix$clipSuffix"
//                e.registerTest("perf", "").let { t ->
//                    t.setOwnedName(testName)
//                    t.argVariant = (PerfTestTextFlag.TextShort.i shl i) or (PerfTestTextFlag.NoWrapWidth.i shl j) or (PerfTestTextFlag.NoCpuFineClipRect.i shl k)
//                    t.guiFunc = measureTextRenderingPerf
//                    t.testFunc = perfCaptureFunc
//                }
//            }
//        }
//    }
}

// ## Measure the drawing cost of various ImDrawList primitives
enum class DrawPrimFunc {
    RectStroke,
    RectStrokeThick,
    RectFilled,
    RectRoundedStroke,
    RectRoundedStrokeThick,
    RectRoundedFilled,
    CircleStroke,
    CircleStrokeThick,
    CircleFilled,
    TriangleStroke,
    TriangleStrokeThick,
    LongStroke,
    LongStrokeThick,
    LongJaggedStroke,
    LongJaggedStrokeThick,
    Line,
    LineAA,

    //    #ifdef IMGUI_HAS_TEXLINES
//    DrawPrimFunc_LineAANoTex,
//    #endif
    LineThick,
    LineThickAA,

    //    #ifdef IMGUI_HAS_TEXLINES
    DrawPrimFunc_LineThickAANoTex;

    //    #endif
    companion object {
        infix fun of(i: Int) = values()[i]
    }
}

private val textBuffer = StringBuilder()

private val contentSize = Vec2(32f) // Size of window content on last frame
private var lineWidth0 = 1f
private var radius = 32f
private var segmentCountManual = 32
private var noAA = false
private var overdraw = false
private val colorBg = Vec4(1f, 0.2f, 0.2f, 1f)
private val colorFg = Vec4(1f)
private var fillMode = 0

private var baseRot = 0f
private var lineWidth1 = 1f

enum class PerfTestTextFlag {
    TextShort,
    TextLong,
    TextWayTooLong,
    NoWrapWidth,
    WithWrapWidth,
    NoCpuFineClipRect,
    WithCpuFineClipRect;

    val i = 1 shl ordinal
}