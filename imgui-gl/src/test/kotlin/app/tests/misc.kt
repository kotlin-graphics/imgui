package app.tests

import engine.context.TestContext
import engine.context.getWindowByRef
import engine.context.logDebug
import engine.context.yieldFrames
import engine.core.TestEngine
import engine.core.registerTest
import imgui.ImGui
import imgui.dsl

fun registerTests_Misc(e: TestEngine) {

    // ## Test watchdog
//    #if 0
//    t = REGISTER_TEST("misc", "misc_watchdog");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        while (true)
//            ctx->Yield();
//    };
//    #endif

    // ## Test window data garbage collection
    e.registerTest("misc", "misc_gc").let { t ->
        t.guiFunc = { ctx: TestContext ->
            // Pretend window is no longer active once we start testing.
            if (ctx.frameCount < 2)
                for (i in 0..4) {
                    val name = "GC Test $i"
                    dsl.window(name) {
                        ImGui.textUnformatted(name)
                    }
                }
        }
        t.testFunc = { ctx: TestContext ->
            ctx.logDebug("Check normal state")
            for (i in 0..2) {
                val window = ctx.getWindowByRef("GC Test $i")!!
                assert(!window.memoryCompacted)
                assert(window.drawList.cmdBuffer.isNotEmpty())
            }

            var backupTimer = 0f
            fun swap() {
                val tmp = backupTimer
                backupTimer = ctx.uiContext!!.io.configWindowsMemoryCompactTimer
                ctx.uiContext!!.io.configWindowsMemoryCompactTimer = tmp
            }

            swap()

            ctx.yieldFrames(3) // Give time to perform GC
            ctx.logDebug("Check GC-ed state")
            for (i in 0..2)            {
                val window = ctx.getWindowByRef("GC Test $i")!!
                assert(window.memoryCompacted)
                assert(window.idStack.isEmpty())
                assert(window.drawList.cmdBuffer.isEmpty())
            }
            swap()
        }
    }

//    // ## Test hash functions and ##/### operators
//    t = REGISTER_TEST("misc", "misc_hash_001");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        // Test hash function for the property we need
//        IM_CHECK_EQ(ImHashStr("helloworld"), ImHashStr("world", 0, ImHashStr("hello", 0)));  // String concatenation
//        IM_CHECK_EQ(ImHashStr("hello###world"), ImHashStr("###world"));                      // ### operator reset back to the seed
//        IM_CHECK_EQ(ImHashStr("hello###world", 0, 1234), ImHashStr("###world", 0, 1234));    // ### operator reset back to the seed
//        IM_CHECK_EQ(ImHashStr("helloxxx", 5), ImHashStr("hello"));                           // String size is honored
//        IM_CHECK_EQ(ImHashStr("", 0, 0), (ImU32)0);                                          // Empty string doesn't alter hash
//        IM_CHECK_EQ(ImHashStr("", 0, 1234), (ImU32)1234);                                    // Empty string doesn't alter hash
//        IM_CHECK_EQ(ImHashStr("hello", 5), ImHashData("hello", 5));                          // FIXME: Do we need to guarantee this?
//
//        const int data[2] = { 42, 50 };
//        IM_CHECK_EQ(ImHashData(&data[0], sizeof(int) * 2), ImHashData(&data[1], sizeof(int), ImHashData(&data[0], sizeof(int))));
//        IM_CHECK_EQ(ImHashData("", 0, 1234), (ImU32)1234);                                   // Empty data doesn't alter hash
//
//        // Verify that Test Engine high-level hash wrapper works
//        IM_CHECK_EQ(ImHashDecoratedPath("Hello/world"), ImHashStr("Helloworld"));            // Slashes are ignored
//        IM_CHECK_EQ(ImHashDecoratedPath("Hello\\/world"), ImHashStr("Hello/world"));         // Slashes can be inhibited
//        IM_CHECK_EQ(ImHashDecoratedPath("/Hello", 42), ImHashDecoratedPath("Hello"));        // Leading / clears seed
//    };
//
//    // ## Test ImVector functions
//    t = REGISTER_TEST("misc", "misc_vector_001");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ImVector<int> v;
//        IM_CHECK(v.Data == NULL);
//        v.push_back(0);
//        v.push_back(1);
//        IM_CHECK(v.Data != NULL && v.Size == 2);
//        v.push_back(2);
//        bool r = v.find_erase(1);
//        IM_CHECK(r == true);
//        IM_CHECK(v.Data != NULL && v.Size == 2);
//        r = v.find_erase(1);
//        IM_CHECK(r == false);
//        IM_CHECK(v.contains(0));
//        IM_CHECK(v.contains(2));
//        v.resize(0);
//        IM_CHECK(v.Data != NULL && v.Capacity >= 3);
//        v.clear();
//        IM_CHECK(v.Data == NULL && v.Capacity == 0);
//    };
//
//    // ## Test ImPool functions
//    t = REGISTER_TEST("misc", "misc_pool_001");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ImPool<ImGuiTabBar> pool;
//        pool.GetOrAddByKey(0x11);
//        pool.GetOrAddByKey(0x22); // May invalidate first point
//        ImGuiTabBar* t1 = pool.GetByKey(0x11);
//        ImGuiTabBar* t2 = pool.GetByKey(0x22);
//        IM_CHECK(t1 != NULL && t2 != NULL);
//        IM_CHECK(t1 + 1 == t2);
//        IM_CHECK(pool.GetIndex(t1) == 0);
//        IM_CHECK(pool.GetIndex(t2) == 1);
//        IM_CHECK(pool.Contains(t1) && pool.Contains(t2));
//        IM_CHECK(pool.Contains(t2 + 1) == false);
//        IM_CHECK(pool.GetByIndex(pool.GetIndex(t1)) == t1);
//        IM_CHECK(pool.GetByIndex(pool.GetIndex(t2)) == t2);
//        ImGuiTabBar* t3 = pool.GetOrAddByKey(0x33);
//        IM_CHECK(pool.GetIndex(t3) == 2);
//        IM_CHECK(pool.GetSize() == 3);
//        pool.Remove(0x22, pool.GetByKey(0x22));
//        IM_CHECK(pool.GetByKey(0x22) == NULL);
//        IM_CHECK(pool.GetSize() == 3);
//        ImGuiTabBar* t4 = pool.GetOrAddByKey(0x40);
//        IM_CHECK(pool.GetIndex(t4) == 1);
//        IM_CHECK(pool.GetSize() == 3);
//        pool.Clear();
//        IM_CHECK(pool.GetSize() == 0);
//    };
//
//    // ## Test behavior of ImParseFormatTrimDecorations
//    t = REGISTER_TEST("misc", "misc_format_parse");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        // fmt = "blah blah"  -> return fmt
//        // fmt = "%.3f"       -> return fmt
//        // fmt = "hello %.3f" -> return fmt + 6
//        // fmt = "%.3f hello" -> return buf, "%.3f"
//        //const char* ImGui::ParseFormatTrimDecorations(const char* fmt, char* buf, int buf_size)
//        const char* fmt = NULL;
//        const char* out = NULL;
//        char buf[32] = { 0 };
//        size_t buf_size = sizeof(buf);
//
//        fmt = "blah blah";
//        out = ImParseFormatTrimDecorations(fmt, buf, buf_size);
//        IM_CHECK(out == fmt);
//
//        fmt = "%.3f";
//        out = ImParseFormatTrimDecorations(fmt, buf, buf_size);
//        IM_CHECK(out == fmt);
//
//        fmt = "hello %.3f";
//        out = ImParseFormatTrimDecorations(fmt, buf, buf_size);
//        IM_CHECK(out == fmt + 6);
//        IM_CHECK(strcmp(out, "%.3f") == 0);
//
//        fmt = "%%hi%.3f";
//        out = ImParseFormatTrimDecorations(fmt, buf, buf_size);
//        IM_CHECK(out == fmt + 4);
//        IM_CHECK(strcmp(out, "%.3f") == 0);
//
//        fmt = "hello %.3f ms";
//        out = ImParseFormatTrimDecorations(fmt, buf, buf_size);
//        IM_CHECK(out == buf);
//        IM_CHECK(strcmp(out, "%.3f") == 0);
//
//        fmt = "hello %f blah";
//        out = ImParseFormatTrimDecorations(fmt, buf, buf_size);
//        IM_CHECK(out == buf);
//        IM_CHECK(strcmp(out, "%f") == 0);
//    };
//
//    // ## Test ImFontAtlas building with overlapping glyph ranges (#2353, #2233)
//    t = REGISTER_TEST("misc", "misc_atlas_build_glyph_overlap");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ImFontAtlas atlas;
//        ImFontConfig font_config;
//        static const ImWchar default_ranges[] =
//                {
//                    0x0020, 0x00FF, // Basic Latin + Latin Supplement
//                    0x0080, 0x00FF, // Latin_Supplement
//                    0,
//                };
//        font_config.GlyphRanges = default_ranges;
//        atlas.AddFontDefault(&font_config);
//        atlas.Build();
//    };
//
//    t = REGISTER_TEST("misc", "misc_atlas_ranges_builder");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ImFontGlyphRangesBuilder builder;
//        builder.AddChar(31);
//        builder.AddChar(0x10000-1);
//        ImVector<ImWchar> out_ranges;
//        builder.BuildRanges(&out_ranges);
//        builder.Clear();
//        IM_CHECK_EQ(out_ranges.Size, 5);
//    };
//
//    // ## Test whether splitting/merging draw lists properly retains a texture id.
//    t = REGISTER_TEST("misc", "misc_drawlist_splitter_texture_id");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        ImGui::Begin("Test Window", NULL, ImGuiWindowFlags_NoSavedSettings);
//        ImDrawList* draw_list = ImGui::GetWindowDrawList();
//        ImTextureID prev_texture_id = draw_list->_TextureIdStack.back();
//        const int draw_count = draw_list->CmdBuffer.Size;
//        IM_CHECK(draw_list->CmdBuffer.back().ElemCount == 0);
//
//        ImVec2 p = ImGui::GetCursorScreenPos();
//        ImGui::Dummy(ImVec2(100+10+100, 100));
//
//        draw_list->ChannelsSplit(2);
//        draw_list->ChannelsSetCurrent(0);
//        // Image wont be clipped when added directly into the draw list.
//        draw_list->AddImage((ImTextureID)100, p, p + ImVec2(100, 100));
//        draw_list->ChannelsSetCurrent(1);
//        draw_list->AddImage((ImTextureID)200, p + ImVec2(110, 0), p + ImVec2(210, 100));
//        draw_list->ChannelsMerge();
//
//        IM_CHECK_NO_RET(draw_list->CmdBuffer.Size == draw_count + 2);
//        IM_CHECK_NO_RET(draw_list->CmdBuffer.back().ElemCount == 0);
//        IM_CHECK_NO_RET(prev_texture_id == draw_list->CmdBuffer.back().TextureId);
//
//        // Replace fake texture IDs with a known good ID in order to prevent graphics API crashing application.
//        for (ImDrawCmd& cmd : draw_list->CmdBuffer)
//        if (cmd.TextureId == (ImTextureID)100 || cmd.TextureId == (ImTextureID)200)
//        cmd.TextureId = prev_texture_id;
//
//        ImGui::End();
//    };
//
//    t = REGISTER_TEST("misc", "misc_repeat_typematic");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ctx->LogDebug("Regular repeat delay/rate");
//        IM_CHECK_EQ_NO_RET(ImGui::CalcTypematicRepeatAmount(0.00f, 0.00f, 1.0f, 0.2f), 1); // Trigger @ 0.0f, 1.0f, 1.2f, 1.4f, etc.
//        IM_CHECK_EQ_NO_RET(ImGui::CalcTypematicRepeatAmount(0.00f, 0.99f, 1.0f, 0.2f), 0); // "
//        IM_CHECK_EQ_NO_RET(ImGui::CalcTypematicRepeatAmount(0.99f, 1.00f, 1.0f, 0.2f), 1); // "
//        IM_CHECK_EQ_NO_RET(ImGui::CalcTypematicRepeatAmount(0.99f, 1.01f, 1.0f, 0.2f), 1); // "
//        IM_CHECK_EQ_NO_RET(ImGui::CalcTypematicRepeatAmount(0.99f, 1.41f, 1.0f, 0.2f), 3); // "
//        IM_CHECK_EQ_NO_RET(ImGui::CalcTypematicRepeatAmount(1.01f, 1.41f, 1.0f, 0.2f), 2); // "
//
//        IM_CHECK_EQ_NO_RET(ImGui::CalcTypematicRepeatAmount(0.99f, 1.01f, 1.1f, 0.2f), 0); // Trigger @ 0.0f, 1.1f, 1.3f, 1.5f, etc.
//
//        IM_CHECK_EQ_NO_RET(ImGui::CalcTypematicRepeatAmount(0.99f, 1.01f, 0.1f, 1.0f), 0); // Trigger @ 0.0f, 0.1f, 1.1f, 2.1f, etc.
//        IM_CHECK_EQ_NO_RET(ImGui::CalcTypematicRepeatAmount(0.99f, 1.11f, 0.1f, 1.0f), 1); // "
//
//        ctx->LogDebug("No repeat delay");
//        IM_CHECK_EQ_NO_RET(ImGui::CalcTypematicRepeatAmount(0.00f, 0.00f, 0.0f, 0.2f), 1); // Trigger @ 0.0f, 0.2f, 0.4f, 0.6f, etc.
//        IM_CHECK_EQ_NO_RET(ImGui::CalcTypematicRepeatAmount(0.19f, 0.20f, 0.0f, 0.2f), 1); // Trigger @ 0.0f, 0.2f, 0.4f, 0.6f, etc.
//        IM_CHECK_EQ_NO_RET(ImGui::CalcTypematicRepeatAmount(0.20f, 0.20f, 0.0f, 0.2f), 0); // Trigger @ 0.0f, 0.2f, 0.4f, 0.6f, etc.
//        IM_CHECK_EQ_NO_RET(ImGui::CalcTypematicRepeatAmount(0.19f, 1.01f, 0.0f, 0.2f), 5); // Trigger @ 0.0f, 0.2f, 0.4f, 0.6f, etc.
//
//        ctx->LogDebug("No repeat rate");
//        IM_CHECK_EQ_NO_RET(ImGui::CalcTypematicRepeatAmount(0.00f, 0.00f, 1.0f, 0.0f), 1); // Trigger @ 0.0f, 1.0f
//        IM_CHECK_EQ_NO_RET(ImGui::CalcTypematicRepeatAmount(0.99f, 1.01f, 1.0f, 0.0f), 1); // "
//        IM_CHECK_EQ_NO_RET(ImGui::CalcTypematicRepeatAmount(1.01f, 2.00f, 1.0f, 0.0f), 0); // "
//
//        ctx->LogDebug("No repeat delay/rate");
//        IM_CHECK_EQ_NO_RET(ImGui::CalcTypematicRepeatAmount(0.00f, 0.00f, 0.0f, 0.0f), 1); // Trigger @ 0.0f
//        IM_CHECK_EQ_NO_RET(ImGui::CalcTypematicRepeatAmount(0.01f, 1.01f, 0.0f, 0.0f), 0); // "
//    };
//
//    // ## Test ImGui::InputScalar() handling overflow for different data types
//    t = REGISTER_TEST("misc", "misc_input_scalar_overflow");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        {
//            ImS8 one = 1;
//            ImS8 value = 2;
//            ImGui::DataTypeApplyOp(ImGuiDataType_S8, '+', &value, &value, &one);
//            IM_CHECK(value == 3);
//            value = SCHAR_MAX;
//            ImGui::DataTypeApplyOp(ImGuiDataType_S8, '+', &value, &value, &one);
//            IM_CHECK(value == SCHAR_MAX);
//            value = SCHAR_MIN;
//            ImGui::DataTypeApplyOp(ImGuiDataType_S8, '-', &value, &value, &one);
//            IM_CHECK(value == SCHAR_MIN);
//        }
//        {
//            ImU8 one = 1;
//            ImU8 value = 2;
//            ImGui::DataTypeApplyOp(ImGuiDataType_U8, '+', &value, &value, &one);
//            IM_CHECK(value == 3);
//            value = UCHAR_MAX;
//            ImGui::DataTypeApplyOp(ImGuiDataType_U8, '+', &value, &value, &one);
//            IM_CHECK(value == UCHAR_MAX);
//            value = 0;
//            ImGui::DataTypeApplyOp(ImGuiDataType_U8, '-', &value, &value, &one);
//            IM_CHECK(value == 0);
//        }
//        {
//            ImS16 one = 1;
//            ImS16 value = 2;
//            ImGui::DataTypeApplyOp(ImGuiDataType_S16, '+', &value, &value, &one);
//            IM_CHECK(value == 3);
//            value = SHRT_MAX;
//            ImGui::DataTypeApplyOp(ImGuiDataType_S16, '+', &value, &value, &one);
//            IM_CHECK(value == SHRT_MAX);
//            value = SHRT_MIN;
//            ImGui::DataTypeApplyOp(ImGuiDataType_S16, '-', &value, &value, &one);
//            IM_CHECK(value == SHRT_MIN);
//        }
//        {
//            ImU16 one = 1;
//            ImU16 value = 2;
//            ImGui::DataTypeApplyOp(ImGuiDataType_U16, '+', &value, &value, &one);
//            IM_CHECK(value == 3);
//            value = USHRT_MAX;
//            ImGui::DataTypeApplyOp(ImGuiDataType_U16, '+', &value, &value, &one);
//            IM_CHECK(value == USHRT_MAX);
//            value = 0;
//            ImGui::DataTypeApplyOp(ImGuiDataType_U16, '-', &value, &value, &one);
//            IM_CHECK(value == 0);
//        }
//        {
//            ImS32 one = 1;
//            ImS32 value = 2;
//            ImGui::DataTypeApplyOp(ImGuiDataType_S32, '+', &value, &value, &one);
//            IM_CHECK(value == 3);
//            value = INT_MAX;
//            ImGui::DataTypeApplyOp(ImGuiDataType_S32, '+', &value, &value, &one);
//            IM_CHECK(value == INT_MAX);
//            value = INT_MIN;
//            ImGui::DataTypeApplyOp(ImGuiDataType_S32, '-', &value, &value, &one);
//            IM_CHECK(value == INT_MIN);
//        }
//        {
//            ImU32 one = 1;
//            ImU32 value = 2;
//            ImGui::DataTypeApplyOp(ImGuiDataType_U32, '+', &value, &value, &one);
//            IM_CHECK(value == 3);
//            value = UINT_MAX;
//            ImGui::DataTypeApplyOp(ImGuiDataType_U32, '+', &value, &value, &one);
//            IM_CHECK(value == UINT_MAX);
//            value = 0;
//            ImGui::DataTypeApplyOp(ImGuiDataType_U32, '-', &value, &value, &one);
//            IM_CHECK(value == 0);
//        }
//        {
//            ImS64 one = 1;
//            ImS64 value = 2;
//            ImGui::DataTypeApplyOp(ImGuiDataType_S64, '+', &value, &value, &one);
//            IM_CHECK(value == 3);
//            value = LLONG_MAX;
//            ImGui::DataTypeApplyOp(ImGuiDataType_S64, '+', &value, &value, &one);
//            IM_CHECK(value == LLONG_MAX);
//            value = LLONG_MIN;
//            ImGui::DataTypeApplyOp(ImGuiDataType_S64, '-', &value, &value, &one);
//            IM_CHECK(value == LLONG_MIN);
//        }
//        {
//            ImU64 one = 1;
//            ImU64 value = 2;
//            ImGui::DataTypeApplyOp(ImGuiDataType_U64, '+', &value, &value, &one);
//            IM_CHECK(value == 3);
//            value = ULLONG_MAX;
//            ImGui::DataTypeApplyOp(ImGuiDataType_U64, '+', &value, &value, &one);
//            IM_CHECK(value == ULLONG_MAX);
//            value = 0;
//            ImGui::DataTypeApplyOp(ImGuiDataType_U64, '-', &value, &value, &one);
//            IM_CHECK(value == 0);
//        }
//    };
//
//    // ## Test basic clipboard, test that clipboard is empty on start
//    t = REGISTER_TEST("misc", "misc_clipboard");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        // By specs, the testing system should provide an empty clipboard (we don't want user clipboard leaking into tests!)
//        const char* clipboard_text = ImGui::GetClipboardText();
//        IM_CHECK_STR_EQ(clipboard_text, "");
//
//        // Regular clipboard test
//        const char* message = "Clippy is alive.";
//        ImGui::SetClipboardText(message);
//        clipboard_text = ImGui::GetClipboardText();
//        IM_CHECK_STR_EQ(message, clipboard_text);
//    };
//
//    // ## Test UTF-8 encoding and decoding.
//    // Note that this is ONLY testing encoding/decoding, we are not attempting to display those characters not trying to be i18n compliant
//    t = REGISTER_TEST("misc", "misc_utf8");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        #define IM_CHECK_UTF8_CP16(_TEXT)   (CheckUtf8_cp16(u8##_TEXT, u##_TEXT))
//        // #define IM_CHECK_UTF8_CP32(_TEXT) (CheckUtf8_cp32(u8##_TEXT, U##_TEXT))
//
//        // Test data taken from https://bitbucket.org/knight666/utf8rewind/src/default/testdata/big-list-of-naughty-strings-master/blns.txt
//
//        // Special Characters
//        // Strings which contain common special ASCII characters (may need to be escaped)
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16(",./;'[]\\-="));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("<>?:\"{}|_+"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("!@#$%^&*()`~"));
//
//        // Unicode Symbols
//        // Strings which contain common unicode symbols (e.g. smart quotes)
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u03a9\u2248\u00e7\u221a\u222b\u02dc\u00b5\u2264\u2265\u00f7"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u00e5\u00df\u2202\u0192\u00a9\u02d9\u2206\u02da\u00ac\u2026\u00e6"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u0153\u2211\u00b4\u00ae\u2020\u00a5\u00a8\u02c6\u00f8\u03c0\u201c\u2018"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u00a1\u2122\u00a3\u00a2\u221e\u00a7\u00b6\u2022\u00aa\u00ba\u2013\u2260"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u00b8\u02db\u00c7\u25ca\u0131\u02dc\u00c2\u00af\u02d8\u00bf"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u00c5\u00cd\u00ce\u00cf\u02dd\u00d3\u00d4\uf8ff\u00d2\u00da\u00c6\u2603"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u0152\u201e\u00b4\u2030\u02c7\u00c1\u00a8\u02c6\u00d8\u220f\u201d\u2019"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("`\u2044\u20ac\u2039\u203a\ufb01\ufb02\u2021\u00b0\u00b7\u201a\u2014\u00b1"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u215b\u215c\u215d\u215e"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u0401\u0402\u0403\u0404\u0405\u0406\u0407\u0408\u0409\u040a\u040b\u040c\u040d\u040e\u040f\u0410\u0411\u0412\u0413\u0414\u0415\u0416\u0417\u0418\u0419\u041a\u041b\u041c\u041d\u041e\u041f\u0420\u0421\u0422\u0423\u0424\u0425\u0426\u0427\u0428\u0429\u042a\u042b\u042c\u042d\u042e\u042f\u0430\u0431\u0432\u0433\u0434\u0435\u0436\u0437\u0438\u0439\u043a\u043b\u043c\u043d\u043e\u043f\u0440\u0441\u0442\u0443\u0444\u0445\u0446\u0447\u0448\u0449\u044a\u044b\u044c\u044d\u044e\u044f"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u0660\u0661\u0662\u0663\u0664\u0665\u0666\u0667\u0668\u0669"));
//
//        // Unicode Subscript/Superscript
//        // Strings which contain unicode subscripts/superscripts; can cause rendering issues
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u2070\u2074\u2075"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u2080\u2081\u2082"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u2070\u2074\u2075\u2080\u2081\u2082"));
//
//        // Quotation Marks
//        // Strings which contain misplaced quotation marks; can cause encoding errors
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("'"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\""));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("''"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\"\""));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("'\"'"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\"''''\"'\""));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\"'\"'\"''''\""));
//
//        // Two-Byte Characters
//        // Strings which contain two-byte characters: can cause rendering issues or character-length issues
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u7530\u4e2d\u3055\u3093\u306b\u3042\u3052\u3066\u4e0b\u3055\u3044"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u30d1\u30fc\u30c6\u30a3\u30fc\u3078\u884c\u304b\u306a\u3044\u304b"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u548c\u88fd\u6f22\u8a9e"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u90e8\u843d\u683c"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\uc0ac\ud68c\uacfc\ud559\uc6d0 \uc5b4\ud559\uc5f0\uad6c\uc18c"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\ucc26\ucc28\ub97c \ud0c0\uace0 \uc628 \ud3b2\uc2dc\ub9e8\uacfc \uc45b\ub2e4\ub9ac \ub620\ubc29\uac01\ud558"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u793e\u6703\u79d1\u5b78\u9662\u8a9e\u5b78\u7814\u7a76\u6240"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\uc6b8\ub780\ubc14\ud1a0\ub974"));
//        // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0002070e\U00020731\U00020779\U00020c53\U00020c78\U00020c96\U00020ccf"));
//
//        // Japanese Emoticons
//        // Strings which consists of Japanese-style emoticons which are popular on the web
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u30fd\u0f3c\u0e88\u0644\u035c\u0e88\u0f3d\uff89 \u30fd\u0f3c\u0e88\u0644\u035c\u0e88\u0f3d\uff89"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("(\uff61\u25d5 \u2200 \u25d5\uff61)"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\uff40\uff68(\u00b4\u2200\uff40\u2229"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("__\uff9b(,_,*)"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u30fb(\uffe3\u2200\uffe3)\u30fb:*:"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\uff9f\uff65\u273f\u30fe\u2572(\uff61\u25d5\u203f\u25d5\uff61)\u2571\u273f\uff65\uff9f"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16(",\u3002\u30fb:*:\u30fb\u309c\u2019( \u263b \u03c9 \u263b )\u3002\u30fb:*:\u30fb\u309c\u2019"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("(\u256f\u00b0\u25a1\u00b0\uff09\u256f\ufe35 \u253b\u2501\u253b)"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("(\uff89\u0ca5\u76ca\u0ca5\uff09\uff89\ufeff \u253b\u2501\u253b"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("( \u0361\u00b0 \u035c\u0296 \u0361\u00b0)"));
//
//        // Emoji
//        // Strings which contain Emoji; should be the same behavior as two-byte characters, but not always
//        // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001f60d"));
//        // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001f469\U0001f3fd"));
//        // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001f47e \U0001f647 \U0001f481 \U0001f645 \U0001f646 \U0001f64b \U0001f64e \U0001f64d"));
//        // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001f435 \U0001f648 \U0001f649 \U0001f64a"));
//        // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\u2764\ufe0f \U0001f494 \U0001f48c \U0001f495 \U0001f49e \U0001f493 \U0001f497 \U0001f496 \U0001f498 \U0001f49d \U0001f49f \U0001f49c \U0001f49b \U0001f49a \U0001f499"));
//        // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\u270b\U0001f3ff \U0001f4aa\U0001f3ff \U0001f450\U0001f3ff \U0001f64c\U0001f3ff \U0001f44f\U0001f3ff \U0001f64f\U0001f3ff"));
//        // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001f6be \U0001f192 \U0001f193 \U0001f195 \U0001f196 \U0001f197 \U0001f199 \U0001f3e7"));
//        // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("0\ufe0f\u20e3 1\ufe0f\u20e3 2\ufe0f\u20e3 3\ufe0f\u20e3 4\ufe0f\u20e3 5\ufe0f\u20e3 6\ufe0f\u20e3 7\ufe0f\u20e3 8\ufe0f\u20e3 9\ufe0f\u20e3 \U0001f51f"));
//
//        // Unicode Numbers
//        // Strings which contain unicode numbers; if the code is localized, it should see the input as numeric
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\uff11\uff12\uff13"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u0661\u0662\u0663"));
//
//        // Right-To-Left Strings
//        // Strings which contain text that should be rendered RTL if possible (e.g. Arabic, Hebrew)
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u062b\u0645 \u0646\u0641\u0633 \u0633\u0642\u0637\u062a \u0648\u0628\u0627\u0644\u062a\u062d\u062f\u064a\u062f\u060c, \u062c\u0632\u064a\u0631\u062a\u064a \u0628\u0627\u0633\u062a\u062e\u062f\u0627\u0645 \u0623\u0646 \u062f\u0646\u0648. \u0625\u0630 \u0647\u0646\u0627\u061f \u0627\u0644\u0633\u062a\u0627\u0631 \u0648\u062a\u0646\u0635\u064a\u0628 \u0643\u0627\u0646. \u0623\u0647\u0651\u0644 \u0627\u064a\u0637\u0627\u0644\u064a\u0627\u060c \u0628\u0631\u064a\u0637\u0627\u0646\u064a\u0627-\u0641\u0631\u0646\u0633\u0627 \u0642\u062f \u0623\u062e\u0630. \u0633\u0644\u064a\u0645\u0627\u0646\u060c \u0625\u062a\u0641\u0627\u0642\u064a\u0629 \u0628\u064a\u0646 \u0645\u0627, \u064a\u0630\u0643\u0631 \u0627\u0644\u062d\u062f\u0648\u062f \u0623\u064a \u0628\u0639\u062f, \u0645\u0639\u0627\u0645\u0644\u0629 \u0628\u0648\u0644\u0646\u062f\u0627\u060c \u0627\u0644\u0625\u0637\u0644\u0627\u0642 \u0639\u0644 \u0625\u064a\u0648."));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u05d1\u05b0\u05bc\u05e8\u05b5\u05d0\u05e9\u05b4\u05c1\u05d9\u05ea, \u05d1\u05b8\u05bc\u05e8\u05b8\u05d0 \u05d0\u05b1\u05dc\u05b9\u05d4\u05b4\u05d9\u05dd, \u05d0\u05b5\u05ea \u05d4\u05b7\u05e9\u05b8\u05bc\u05c1\u05de\u05b7\u05d9\u05b4\u05dd, \u05d5\u05b0\u05d0\u05b5\u05ea \u05d4\u05b8\u05d0\u05b8\u05e8\u05b6\u05e5"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u05d4\u05b8\u05d9\u05b0\u05ea\u05b8\u05d4test\u0627\u0644\u0635\u0641\u062d\u0627\u062a \u0627\u0644\u062a\u0651\u062d\u0648\u0644"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\ufdfd"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\ufdfa"));
//
//        // Unicode Spaces
//        // Strings which contain unicode space characters with special properties (c.f. https://www.cs.tut.fi/~jkorpela/chars/spaces.html)
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u200b"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u180e"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\ufeff"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u2423"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u2422"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u2421"));
//
//        // Trick Unicode
//        // Strings which contain unicode with unusual properties (e.g. Right-to-left override) (c.f. http://www.unicode.org/charts/PDF/U2000.pdf)
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u202a\u202atest\u202a"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u202btest\u202b"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("test"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("test\u2060test\u202b"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u2066test\u2067"));
//
//        // Zalgo Text
//        // Strings which contain "corrupted" text. The corruption will not appear in non-HTML text, however. (via http://www.eeemo.net)
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u1e70\u033a\u033a\u0315o\u035e \u0337i\u0332\u032c\u0347\u032a\u0359n\u031d\u0317\u0355v\u031f\u031c\u0318\u0326\u035fo\u0336\u0319\u0330\u0320k\u00e8\u035a\u032e\u033a\u032a\u0339\u0331\u0324 \u0316t\u031d\u0355\u0333\u0323\u033b\u032a\u035eh\u033c\u0353\u0332\u0326\u0333\u0318\u0332e\u0347\u0323\u0330\u0326\u032c\u034e \u0322\u033c\u033b\u0331\u0318h\u035a\u034e\u0359\u031c\u0323\u0332\u0345i\u0326\u0332\u0323\u0330\u0324v\u033b\u034de\u033a\u032d\u0333\u032a\u0330-m\u0322i\u0345n\u0316\u033a\u031e\u0332\u032f\u0330d\u0335\u033c\u031f\u0359\u0329\u033c\u0318\u0333 \u031e\u0325\u0331\u0333\u032dr\u031b\u0317\u0318e\u0359p\u0360r\u033c\u031e\u033b\u032d\u0317e\u033a\u0320\u0323\u035fs\u0318\u0347\u0333\u034d\u031d\u0349e\u0349\u0325\u032f\u031e\u0332\u035a\u032c\u035c\u01f9\u032c\u034e\u034e\u031f\u0316\u0347\u0324t\u034d\u032c\u0324\u0353\u033c\u032d\u0358\u0345i\u032a\u0331n\u0360g\u0334\u0349 \u034f\u0349\u0345c\u032c\u031fh\u0361a\u032b\u033b\u032f\u0358o\u032b\u031f\u0316\u034d\u0319\u031d\u0349s\u0317\u0326\u0332.\u0328\u0339\u0348\u0323"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u0321\u0353\u031e\u0345I\u0317\u0318\u0326\u035dn\u0347\u0347\u0359v\u032e\u032bok\u0332\u032b\u0319\u0348i\u0316\u0359\u032d\u0339\u0320\u031en\u0321\u033b\u032e\u0323\u033ag\u0332\u0348\u0359\u032d\u0359\u032c\u034e \u0330t\u0354\u0326h\u031e\u0332e\u0322\u0324 \u034d\u032c\u0332\u0356f\u0334\u0318\u0355\u0323\u00e8\u0356\u1eb9\u0325\u0329l\u0356\u0354\u035ai\u0353\u035a\u0326\u0360n\u0356\u034d\u0317\u0353\u0333\u032eg\u034d \u0328o\u035a\u032a\u0361f\u0318\u0323\u032c \u0316\u0318\u0356\u031f\u0359\u032ec\u0489\u0354\u032b\u0356\u0353\u0347\u0356\u0345h\u0335\u0324\u0323\u035a\u0354\u00e1\u0317\u033c\u0355\u0345o\u033c\u0323\u0325s\u0331\u0348\u033a\u0316\u0326\u033b\u0362.\u031b\u0316\u031e\u0320\u032b\u0330"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u0317\u033a\u0356\u0339\u032f\u0353\u1e6e\u0324\u034d\u0325\u0347\u0348h\u0332\u0301e\u034f\u0353\u033c\u0317\u0319\u033c\u0323\u0354 \u0347\u031c\u0331\u0320\u0353\u034d\u0345N\u0355\u0360e\u0317\u0331z\u0318\u031d\u031c\u033a\u0359p\u0324\u033a\u0339\u034d\u032f\u035ae\u0320\u033b\u0320\u035cr\u0328\u0324\u034d\u033a\u0316\u0354\u0316\u0316d\u0320\u031f\u032d\u032c\u031d\u035fi\u0326\u0356\u0329\u0353\u0354\u0324a\u0320\u0317\u032c\u0349\u0319n\u035a\u035c \u033b\u031e\u0330\u035a\u0345h\u0335\u0349i\u0333\u031ev\u0322\u0347\u1e19\u034e\u035f-\u0489\u032d\u0329\u033c\u0354m\u0324\u032d\u032bi\u0355\u0347\u031d\u0326n\u0317\u0359\u1e0d\u031f \u032f\u0332\u0355\u035e\u01eb\u031f\u032f\u0330\u0332\u0359\u033b\u031df \u032a\u0330\u0330\u0317\u0316\u032d\u0318\u0358c\u0326\u034d\u0332\u031e\u034d\u0329\u0319\u1e25\u035aa\u032e\u034e\u031f\u0319\u035c\u01a1\u0329\u0339\u034es\u0324.\u031d\u031d \u0489Z\u0321\u0316\u031c\u0356\u0330\u0323\u0349\u031ca\u0356\u0330\u0359\u032c\u0361l\u0332\u032b\u0333\u034d\u0329g\u0321\u031f\u033c\u0331\u035a\u031e\u032c\u0345o\u0317\u035c.\u031f"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u0326H\u032c\u0324\u0317\u0324\u035de\u035c \u031c\u0325\u031d\u033b\u034d\u031f\u0301w\u0315h\u0316\u032f\u0353o\u031d\u0359\u0316\u034e\u0331\u032e \u0489\u033a\u0319\u031e\u031f\u0348W\u0337\u033c\u032da\u033a\u032a\u034d\u012f\u0348\u0355\u032d\u0359\u032f\u031ct\u0336\u033c\u032es\u0318\u0359\u0356\u0315 \u0320\u032b\u0320B\u033b\u034d\u0359\u0349\u0333\u0345e\u0335h\u0335\u032c\u0347\u032b\u0359i\u0339\u0353\u0333\u0333\u032e\u034e\u032b\u0315n\u035fd\u0334\u032a\u031c\u0316 \u0330\u0349\u0329\u0347\u0359\u0332\u035e\u0345T\u0356\u033c\u0353\u032a\u0362h\u034f\u0353\u032e\u033be\u032c\u031d\u031f\u0345 \u0324\u0339\u031dW\u0359\u031e\u031d\u0354\u0347\u035d\u0345a\u034f\u0353\u0354\u0339\u033c\u0323l\u0334\u0354\u0330\u0324\u031f\u0354\u1e3d\u032b.\u0355"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("Z\u032e\u031e\u0320\u0359\u0354\u0345\u1e00\u0317\u031e\u0348\u033b\u0317\u1e36\u0359\u034e\u032f\u0339\u031e\u0353G\u033bO\u032d\u0317\u032e"));
//
//        // Unicode Upsidedown
//        // Strings which contain unicode with an "upsidedown" effect (via http://www.upsidedowntext.com)
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u02d9\u0250nb\u1d09l\u0250 \u0250u\u0183\u0250\u026f \u01dd\u0279olop \u0287\u01dd \u01dd\u0279oq\u0250l \u0287n \u0287unp\u1d09p\u1d09\u0254u\u1d09 \u0279od\u026f\u01dd\u0287 po\u026fsn\u1d09\u01dd op p\u01dds '\u0287\u1d09l\u01dd \u0183u\u1d09\u0254s\u1d09d\u1d09p\u0250 \u0279n\u0287\u01dd\u0287\u0254\u01ddsuo\u0254 '\u0287\u01dd\u026f\u0250 \u0287\u1d09s \u0279olop \u026fnsd\u1d09 \u026f\u01dd\u0279o\u02e5"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("00\u02d9\u0196$-"));
//
//        // Unicode font
//        // Strings which contain bold/italic/etc. versions of normal characters
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\uff34\uff48\uff45 \uff51\uff55\uff49\uff43\uff4b \uff42\uff52\uff4f\uff57\uff4e \uff46\uff4f\uff58 \uff4a\uff55\uff4d\uff50\uff53 \uff4f\uff56\uff45\uff52 \uff54\uff48\uff45 \uff4c\uff41\uff5a\uff59 \uff44\uff4f\uff47"));
//        // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001d413\U0001d421\U0001d41e \U0001d42a\U0001d42e\U0001d422\U0001d41c\U0001d424 \U0001d41b\U0001d42b\U0001d428\U0001d430\U0001d427 \U0001d41f\U0001d428\U0001d431 \U0001d423\U0001d42e\U0001d426\U0001d429\U0001d42c \U0001d428\U0001d42f\U0001d41e\U0001d42b \U0001d42d\U0001d421\U0001d41e \U0001d425\U0001d41a\U0001d433\U0001d432 \U0001d41d\U0001d428\U0001d420"));
//        // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001d57f\U0001d58d\U0001d58a \U0001d596\U0001d59a\U0001d58e\U0001d588\U0001d590 \U0001d587\U0001d597\U0001d594\U0001d59c\U0001d593 \U0001d58b\U0001d594\U0001d59d \U0001d58f\U0001d59a\U0001d592\U0001d595\U0001d598 \U0001d594\U0001d59b\U0001d58a\U0001d597 \U0001d599\U0001d58d\U0001d58a \U0001d591\U0001d586\U0001d59f\U0001d59e \U0001d589\U0001d594\U0001d58c"));
//        // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001d47b\U0001d489\U0001d486 \U0001d492\U0001d496\U0001d48a\U0001d484\U0001d48c \U0001d483\U0001d493\U0001d490\U0001d498\U0001d48f \U0001d487\U0001d490\U0001d499 \U0001d48b\U0001d496\U0001d48e\U0001d491\U0001d494 \U0001d490\U0001d497\U0001d486\U0001d493 \U0001d495\U0001d489\U0001d486 \U0001d48d\U0001d482\U0001d49b\U0001d49a \U0001d485\U0001d490\U0001d488"));
//        // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001d4e3\U0001d4f1\U0001d4ee \U0001d4fa\U0001d4fe\U0001d4f2\U0001d4ec\U0001d4f4 \U0001d4eb\U0001d4fb\U0001d4f8\U0001d500\U0001d4f7 \U0001d4ef\U0001d4f8\U0001d501 \U0001d4f3\U0001d4fe\U0001d4f6\U0001d4f9\U0001d4fc \U0001d4f8\U0001d4ff\U0001d4ee\U0001d4fb \U0001d4fd\U0001d4f1\U0001d4ee \U0001d4f5\U0001d4ea\U0001d503\U0001d502 \U0001d4ed\U0001d4f8\U0001d4f0"));
//        // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001d54b\U0001d559\U0001d556 \U0001d562\U0001d566\U0001d55a\U0001d554\U0001d55c \U0001d553\U0001d563\U0001d560\U0001d568\U0001d55f \U0001d557\U0001d560\U0001d569 \U0001d55b\U0001d566\U0001d55e\U0001d561\U0001d564 \U0001d560\U0001d567\U0001d556\U0001d563 \U0001d565\U0001d559\U0001d556 \U0001d55d\U0001d552\U0001d56b\U0001d56a \U0001d555\U0001d560\U0001d558"));
//        // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001d683\U0001d691\U0001d68e \U0001d69a\U0001d69e\U0001d692\U0001d68c\U0001d694 \U0001d68b\U0001d69b\U0001d698\U0001d6a0\U0001d697 \U0001d68f\U0001d698\U0001d6a1 \U0001d693\U0001d69e\U0001d696\U0001d699\U0001d69c \U0001d698\U0001d69f\U0001d68e\U0001d69b \U0001d69d\U0001d691\U0001d68e \U0001d695\U0001d68a\U0001d6a3\U0001d6a2 \U0001d68d\U0001d698\U0001d690"));
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("\u24af\u24a3\u24a0 \u24ac\u24b0\u24a4\u249e\u24a6 \u249d\u24ad\u24aa\u24b2\u24a9 \u24a1\u24aa\u24b3 \u24a5\u24b0\u24a8\u24ab\u24ae \u24aa\u24b1\u24a0\u24ad \u24af\u24a3\u24a0 \u24a7\u249c\u24b5\u24b4 \u249f\u24aa\u24a2"));
//
//        // iOS Vulnerability
//        // Strings which crashed iMessage in iOS versions 8.3 and earlier
//        IM_CHECK_NO_RET(IM_CHECK_UTF8_CP16("Power\u0644\u064f\u0644\u064f\u0635\u0651\u0628\u064f\u0644\u064f\u0644\u0635\u0651\u0628\u064f\u0631\u0631\u064b \u0963 \u0963h \u0963 \u0963\u5197"));
//    };
//
//    // ## Test ImGuiTextFilter
//    t = REGISTER_TEST("misc", "misc_text_filter");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        static ImGuiTextFilter filter;
//        ImGui::Begin("Text filter");
//        filter.Draw("Filter", ImGui::GetFontSize() * 16);   // Test input filter drawing
//        ImGui::End();
//    };
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        // Test ImGuiTextFilter::Draw()
//        ctx->WindowRef("Text filter");
//        ctx->ItemInput("Filter");
//        ctx->KeyCharsAppend("Big,Cat,, ,  ,Bird"); // Trigger filter rebuild
//
//        // Test functionality
//        ImGuiTextFilter filter;
//        ImStrncpy(filter.InputBuf, "-bar", IM_ARRAYSIZE(filter.InputBuf));
//        filter.Build();
//
//        IM_CHECK(filter.PassFilter("bartender") == false);
//        IM_CHECK(filter.PassFilter("cartender") == true);
//
//        ImStrncpy(filter.InputBuf, "bar ", IM_ARRAYSIZE(filter.InputBuf));
//        filter.Build();
//        IM_CHECK(filter.PassFilter("bartender") == true);
//        IM_CHECK(filter.PassFilter("cartender") == false);
//
//        ImStrncpy(filter.InputBuf, "bar", IM_ARRAYSIZE(filter.InputBuf));
//        filter.Build();
//        IM_CHECK(filter.PassFilter("bartender") == true);
//        IM_CHECK(filter.PassFilter("cartender") == false);
//    };
//
//    // ## Visual ImBezierClosestPoint test.
//    t = REGISTER_TEST("misc", "misc_bezier_closest_point");
//    t->GuiFunc = [](ImGuiTestContext* ctx)
//    {
//        // FIXME-TESTS: Store normalized?
//        static ImVec2 points[4] = { ImVec2(30, 75), ImVec2(185, 355), ImVec2(400, 60), ImVec2(590, 370) };
//        static int num_segments = 0;
//        const ImGuiStyle& style = ctx->UiContext->Style;
//
//        ImGui::SetNextWindowSize(ImVec2(600, 400), ImGuiCond_Appearing);
//        ImGui::Begin("Bezier", NULL, ImGuiWindowFlags_NoSavedSettings);
//        ImGui::DragInt("Segments", &num_segments, 0.05f, 0, 20);
//
//        ImDrawList* draw_list = ImGui::GetWindowDrawList();
//        const ImVec2 mouse_pos = ImGui::GetMousePos();
//        const ImVec2 wp = ImGui::GetWindowPos();
//
//        // Draw modifiable control points
//        for (ImVec2& pt : points)
//        {
//            const float half_circle = 2.0f;
//            const float full_circle = half_circle * 2.0f;
//            ImRect r(wp + pt - ImVec2(half_circle, half_circle), wp + pt + ImVec2(half_circle, half_circle));
//            ImGuiID id = ImGui::GetID((void*)(&pt - points));
//
//            ImGui::ItemAdd(r, id);
//            bool is_hovered = ImGui::IsItemHovered();
//            bool is_active = ImGui::IsItemActive();
//            if (is_hovered || is_active)
//                draw_list->AddCircleFilled(r.GetCenter(), full_circle, IM_COL32(0,255,0,255));
//            else
//            draw_list->AddCircle(r.GetCenter(), full_circle, IM_COL32(0,255,0,255));
//
//            if (is_active)
//            {
//                if (ImGui::IsMouseDown(0))
//                    pt = mouse_pos - wp;
//                else
//                    ImGui::ClearActiveID();
//            }
//            else if (ImGui::IsMouseDown(0) && is_hovered)
//                ImGui::SetActiveID(id, ImGui::GetCurrentWindow());
//        }
//        draw_list->AddLine(wp + points[0], wp + points[1], IM_COL32(0,255,0,100));
//        draw_list->AddLine(wp + points[2], wp + points[3], IM_COL32(0,255,0,100));
//
//        // Draw curve itself
//        draw_list->AddBezierCurve(wp + points[0], wp + points[1], wp + points[2], wp + points[3], IM_COL32_WHITE, 2.0f, num_segments);
//
//        // Draw point closest to the mouse cursor
//        ImVec2 point;
//        if (num_segments == 0)
//            point = ImBezierClosestPointCasteljau(wp + points[0], wp + points[1], wp + points[2], wp + points[3], mouse_pos, style.CurveTessellationTol);
//        else
//            point = ImBezierClosestPoint(wp + points[0], wp + points[1], wp + points[2], wp + points[3], mouse_pos, num_segments);
//        draw_list->AddCircleFilled(point, 4.0f, IM_COL32(255,0,0,255));
//
//        ImGui::End();
//    };
//
//    // FIXME-TESTS
//    t = REGISTER_TEST("demo", "demo_misc_001");
//    t->GuiFunc = NULL;
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ctx->WindowRef("Dear ImGui Demo");
//        ctx->ItemOpen("Widgets");
//        ctx->ItemOpen("Basic");
//        ctx->ItemClick("Basic/Button");
//        ctx->ItemClick("Basic/radio a");
//        ctx->ItemClick("Basic/radio b");
//        ctx->ItemClick("Basic/radio c");
//        ctx->ItemClick("Basic/combo");
//        ctx->ItemClick("Basic/combo");
//        ctx->ItemClick("Basic/color 2/##ColorButton");
//        //ctx->ItemClick("##Combo/BBBB");     // id chain
//        ctx->SleepShort();
//        ctx->PopupClose();
//
//        //ctx->ItemClick("Layout");  // FIXME: close popup
//        ctx->ItemOpen("Layout");
//        ctx->ItemOpen("Scrolling");
//        ctx->ItemHold("Scrolling/>>", 1.0f);
//        ctx->SleepShort();
//    };
//
//    // ## Coverage: open everything in demo window
//    // ## Extra: test for inconsistent ScrollMax.y across whole demo window
//    // ## Extra: run Log/Capture api on whole demo window
//    t = REGISTER_TEST("demo", "demo_cov_auto_open");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ctx->WindowRef("Dear ImGui Demo");
//        ctx->ItemOpenAll("");
//
//        // Additional tests we bundled here because we are benefiting from the "opened all" state
//        ImGuiWindow* window = ctx->GetWindowByRef("");
//        ctx->ScrollVerifyScrollMax(window);
//
//        // Test the Log/Capture api
//        const char* clipboard = ImGui::GetClipboardText();
//        IM_CHECK(strlen(clipboard) == 0);
//        ctx->ItemClick("Capture\\/Logging/LogButtons/Log To Clipboard");
//        clipboard = ImGui::GetClipboardText();
//        const int clipboard_len = (int)strlen(clipboard);
//        IM_CHECK_GT(clipboard_len, 15000); // This is going to vary (as of 2019-11-18 on Master this 22766)
//    };
//
//    // ## Coverage: closes everything in demo window
//    t = REGISTER_TEST("demo", "demo_cov_auto_close");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ctx->WindowRef("Dear ImGui Demo");
//        ctx->ItemCloseAll("");
//    };
//
//    t = REGISTER_TEST("demo", "demo_cov_001");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ctx->WindowRef("Dear ImGui Demo");
//        ctx->ItemOpen("Help");
//        ctx->ItemOpen("Configuration");
//        ctx->ItemOpen("Window options");
//        ctx->ItemOpen("Widgets");
//        ctx->ItemOpen("Layout");
//        ctx->ItemOpen("Popups & Modal windows");
//        #if IMGUI_HAS_TABLE
//        ctx->ItemOpen("Tables & Columns");
//        #else
//        ctx->ItemOpen("Columns");
//        #endif
//        ctx->ItemOpen("Filtering");
//        ctx->ItemOpen("Inputs, Navigation & Focus");
//    };
//
//    // ## Open misc elements which are beneficial to coverage and not covered with ItemOpenAll
//    t = REGISTER_TEST("demo", "demo_cov_002");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ctx->WindowRef("Dear ImGui Demo");
//        ctx->ItemOpen("Layout");
//        ctx->ItemOpen("Scrolling");
//        ctx->ItemCheck("Scrolling/Show Horizontal contents size demo window");   // FIXME-TESTS: ItemXXX functions could do the recursion (e.g. Open parent)
//        ctx->ItemUncheck("Scrolling/Show Horizontal contents size demo window");
//
//        ctx->WindowRef("Dear ImGui Demo");
//        ctx->MenuCheck("Tools/About Dear ImGui");
//        ctx->WindowRef("About Dear ImGui");
//        ctx->ItemCheck("Config\\/Build Information");
//        ctx->WindowRef("Dear ImGui Demo");
//
//        ctx->WindowRef("Dear ImGui Demo");
//        ctx->MenuCheck("Tools/Style Editor");
//        ctx->WindowRef("Style Editor");
//        ctx->ItemClick("##tabs/Sizes");
//        ctx->ItemClick("##tabs/Colors");
//        ctx->ItemClick("##tabs/Fonts");
//        ctx->ItemClick("##tabs/Rendering");
//
//        ctx->WindowRef("Dear ImGui Demo");
//        ctx->MenuCheck("Examples/Custom rendering");
//        ctx->WindowRef("Example: Custom rendering");
//        ctx->ItemClick("##TabBar/Primitives");
//        ctx->ItemClick("##TabBar/Canvas");
//        ctx->ItemClick("##TabBar/BG\\/FG draw lists");
//
//        ctx->WindowRef("Dear ImGui Demo");
//        ctx->MenuUncheckAll("Examples");
//        ctx->MenuUncheckAll("Tools");
//    };
//
//    t = REGISTER_TEST("demo", "demo_cov_apps");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ctx->WindowRef("Dear ImGui Demo");
//        ctx->MenuClick("Menu/Open Recent/More..");
//        ctx->MenuCheckAll("Examples");
//        ctx->MenuUncheckAll("Examples");
//        ctx->MenuCheckAll("Tools");
//        ctx->MenuUncheckAll("Tools");
//    };
//
//    // ## Coverage: select all styles via the Style Editor
//    t = REGISTER_TEST("demo", "demo_cov_styles");
//    t->TestFunc = [](ImGuiTestContext* ctx)
//    {
//        ctx->WindowRef("Dear ImGui Demo");
//        ctx->MenuAction(ImGuiTestAction_Check, "Tools/Style Editor");
//
//        ImGuiTestRef ref_window = "Style Editor";
//        ctx->WindowRef(ref_window);
//        ctx->ItemClick("Colors##Selector");
//        ctx->Yield();
//        ImGuiTestRef ref_popup = ctx->GetFocusWindowRef();
//
//        ImGuiStyle style_backup = ImGui::GetStyle();
//        ImGuiTestItemList items;
//        ctx->GatherItems(&items, ref_popup);
//        for (int n = 0; n < items.Size; n++)
//        {
//            ctx->WindowRef(ref_window);
//            ctx->ItemClick("Colors##Selector");
//            ctx->WindowRef(ref_popup);
//            ctx->ItemClick(items[n]->ID);
//        }
//        ImGui::GetStyle() = style_backup;
//    };
}