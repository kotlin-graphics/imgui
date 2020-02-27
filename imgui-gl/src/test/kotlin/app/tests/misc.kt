package app.tests

import engine.context.*
import engine.core.TestEngine
import engine.core.registerTest
import engine.hashDecoratedPath
import glm_.b
import glm_.s
import glm_.vec2.Vec2
import imgui.*
import imgui.classes.TextFilter
import imgui.font.FontAtlas
import imgui.font.FontConfig
import imgui.font.FontGlyphRangesBuilder
import imgui.internal.*
import imgui.internal.classes.Pool
import imgui.internal.classes.PoolIdx
import imgui.internal.classes.Rect
import imgui.internal.classes.TabBar
import unsigned.Ubyte
import unsigned.Uint
import unsigned.Ulong
import unsigned.Ushort

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
            for (i in 0..2) {
                val window = ctx.getWindowByRef("GC Test $i")!!
                assert(window.memoryCompacted)
                assert(window.idStack.isEmpty())
                assert(window.drawList.cmdBuffer.isEmpty())
            }
            swap()
        }
    }

    // ## Test hash functions and ##/### operators
    e.registerTest("misc", "misc_hash_001").let { t ->
        t.testFunc = {
            // Test hash function for the property we need
            assert(hash("helloworld") == hash("world", 0, hash("hello", 0)))  // String concatenation
            assert(hash("hello###world") == hash("###world"))                      // ### operator reset back to the seed
            assert(hash("hello###world", 0, 1234) == hash("###world", 0, 1234))    // ### operator reset back to the seed
            assert(hash("helloxxx", 5) == hash("hello"))                           // String size is honored
            assert(hash("", 0, 0) == 0)                                          // Empty string doesn't alter hash
            assert(hash("", 0, 1234) == 1234)                                    // Empty string doesn't alter hash
            assert(hash("hello", 5) == hash("hello", 5))                          // FIXME: Do we need to guarantee this?

            val data = intArrayOf(42, 50)
            assert(hash(data) == hash(data[1], hash(data[0])))
            assert(hash("", 0, 1234) == 1234)                                   // Empty data doesn't alter hash

            // Verify that Test Engine high-level hash wrapper works
            assert(hashDecoratedPath("Hello/world") == hash("Helloworld"))            // Slashes are ignored
            assert(hashDecoratedPath("Hello\\/world") == hash("Hello/world"))         // Slashes can be inhibited
            assert(hashDecoratedPath("/Hello", 42) == hashDecoratedPath("Hello"))        // Leading / clears seed
        }
    }

    // ## Test ImVector functions
    e.registerTest("misc", "misc_vector_001").let { t ->
        t.testFunc = {
            val v = ArrayList<Int>()
            assert(v.isEmpty())
            v += 0
            v += 1
            assert(v.size == 2)
            v += 2
            var r = v.remove(1)
            assert(r)
            assert(v.size == 2)
            r = v.remove(1)
            assert(!r)
            assert(0 in v)
            assert(2 in v)
//            v.resize(0);
//            IM_CHECK(v.Data != NULL && v.Capacity >= 3);
            v.clear()
            assert(v.size == 0)
        }
    }

    // ## Test ImPool functions
    e.registerTest("misc", "misc_pool_001").let { t ->
        t.testFunc = {
            val pool = Pool { TabBar() }
            pool.getOrAddByKey(0x11)
            pool.getOrAddByKey(0x22) // May invalidate first point
            val t1 = pool[0x11]!!
            val t2 = pool[0x22]!!
//            assert(t1 != null && t2 != null)
            assert(pool.buf[pool.getIndex(t1).i + 1] === t2)
            assert(pool.getIndex(t1) == PoolIdx(0))
            assert(pool.getIndex(t2) == PoolIdx(1))
            assert(t1 in pool && t2 in pool)
            assert(TabBar() !in pool)
            assert(pool[pool.getIndex(t1)] === t1)
            assert(pool[pool.getIndex(t2)] === t2)
            val t3 = pool.getOrAddByKey(0x33)
            assert(pool.getIndex(t3) == PoolIdx(2))
            assert(pool.size == 3)
            pool.remove(0x22, pool[0x22]!!)
            assert(pool[0x22] == null)
            assert(pool.size == 2) // [JVM] different from native, 3
            val t4 = pool.getOrAddByKey(0x40)
            assert(pool.getIndex(t4) == PoolIdx(2)) // [JVM] different from native, 1
            assert(pool.size == 3)
            pool.clear()
            assert(pool.size == 0)
        }
    }

    // ## Test behavior of ImParseFormatTrimDecorations
    e.registerTest("misc", "misc_format_parse").let { t ->
        t.testFunc = {
            // fmt = "blah blah"  -> return fmt
            // fmt = "%.3f"       -> return fmt
            // fmt = "hello %.3f" -> return fmt + 6
            // fmt = "%.3f hello" -> return buf, "%.3f"
            //const char* ImGui::ParseFormatTrimDecorations(const char* fmt, char* buf, int buf_size)

            var fmt = "blah blah"
            var out = ImGui.parseFormatTrimDecorations(fmt)
            assert(out == fmt)

            fmt = "%.3f"
            out = ImGui.parseFormatTrimDecorations(fmt)
            assert(out == fmt)

            fmt = "hello %.3f"
            out = ImGui.parseFormatTrimDecorations(fmt)
            assert(out == fmt.substring(6))
            assert(out == "%.3f")

            fmt = "%%hi%.3f"
            out = ImGui.parseFormatTrimDecorations(fmt)
            assert(out == fmt.substring(4))
            assert(out == "%.3f")

            fmt = "hello %.3f ms"
            out = ImGui.parseFormatTrimDecorations(fmt)
//            assert(out == buf)
            assert(out == "%.3f")

            fmt = "hello %f blah"
            out = ImGui.parseFormatTrimDecorations(fmt)
//            IM_CHECK(out == buf)
            assert(out == "%f")
        }
    }

    // ## Test ImFontAtlas building with overlapping glyph ranges (#2353, #2233)
    e.registerTest("misc", "misc_atlas_build_glyph_overlap").let { t ->
        t.testFunc = {
            val atlas = FontAtlas()
            val fontConfig = FontConfig()
            fontConfig.glyphRanges = defaultRanges
            atlas.addFontDefault(fontConfig)
            atlas.build()
        }
    }

    e.registerTest("misc", "misc_atlas_ranges_builder").let { t ->
        t.testFunc = {
            val builder = FontGlyphRangesBuilder()
            builder.addChar(31)
            builder.addChar(0x10000 - 1)
            val outRanges = builder.buildRanges()
            builder.clear()
            assert(outRanges.size == (5 - 1) / 2)
        }
    }

    // ## Test whether splitting/merging draw lists properly retains a texture id.
    e.registerTest("misc", "misc_drawlist_splitter_texture_id").let { t ->
        t.guiFunc = {
            ImGui.begin("Test Window", null, WindowFlag.NoSavedSettings.i)
            val drawList = ImGui.windowDrawList
            val prevTextureId = drawList._textureIdStack.last()
            val drawCount = drawList.cmdBuffer.size
            assert(drawList.cmdBuffer.last().elemCount == 0)

            val p = ImGui.cursorScreenPos
            ImGui.dummy(Vec2(100 + 10 + 100, 100))

            drawList.apply {
                channelsSplit(2)
                channelsSetCurrent(0)
                // Image wont be clipped when added directly into the draw list.
                addImage(100, p, p + 100)
                channelsSetCurrent(1)
                addImage(200, p + Vec2(110, 0), p + Vec2(210, 100))
                channelsMerge()

                assert(cmdBuffer.size == drawCount + 2)
                assert(cmdBuffer.last().elemCount == 0)
                assert(prevTextureId == cmdBuffer.last().textureId)
            }
            // Replace fake texture IDs with a known good ID in order to prevent graphics API crashing application.
            for (cmd in drawList.cmdBuffer)
                if (cmd.textureId == 100 || cmd.textureId == 200)
                    cmd.textureId = prevTextureId

            ImGui.end()
        }
    }

    e.registerTest("misc", "misc_repeat_typematic").let { t ->
        t.testFunc = { ctx: TestContext ->
            ctx.logDebug("Regular repeat delay/rate")
            assert(ImGui.calcTypematicRepeatAmount(0.00f, 0.00f, 1.0f, 0.2f) == 1) // Trigger @ 0.0f, 1.0f, 1.2f, 1.4f, etc.
            assert(ImGui.calcTypematicRepeatAmount(0.00f, 0.99f, 1.0f, 0.2f) == 0) // "
            assert(ImGui.calcTypematicRepeatAmount(0.99f, 1.00f, 1.0f, 0.2f) == 1) // "
            assert(ImGui.calcTypematicRepeatAmount(0.99f, 1.01f, 1.0f, 0.2f) == 1) // "
            assert(ImGui.calcTypematicRepeatAmount(0.99f, 1.41f, 1.0f, 0.2f) == 3) // "
            assert(ImGui.calcTypematicRepeatAmount(1.01f, 1.41f, 1.0f, 0.2f) == 2) // "

            assert(ImGui.calcTypematicRepeatAmount(0.99f, 1.01f, 1.1f, 0.2f) == 0) // Trigger @ 0.0f, 1.1f, 1.3f, 1.5f, etc.

            assert(ImGui.calcTypematicRepeatAmount(0.99f, 1.01f, 0.1f, 1.0f) == 0) // Trigger @ 0.0f, 0.1f, 1.1f, 2.1f, etc.
            assert(ImGui.calcTypematicRepeatAmount(0.99f, 1.11f, 0.1f, 1.0f) == 1) // "

            ctx.logDebug("No repeat delay")
            assert(ImGui.calcTypematicRepeatAmount(0.00f, 0.00f, 0.0f, 0.2f) == 1) // Trigger @ 0.0f, 0.2f, 0.4f, 0.6f, etc.
            assert(ImGui.calcTypematicRepeatAmount(0.19f, 0.20f, 0.0f, 0.2f) == 1) // Trigger @ 0.0f, 0.2f, 0.4f, 0.6f, etc.
            assert(ImGui.calcTypematicRepeatAmount(0.20f, 0.20f, 0.0f, 0.2f) == 0) // Trigger @ 0.0f, 0.2f, 0.4f, 0.6f, etc.
            assert(ImGui.calcTypematicRepeatAmount(0.19f, 1.01f, 0.0f, 0.2f) == 5) // Trigger @ 0.0f, 0.2f, 0.4f, 0.6f, etc.

            ctx.logDebug("No repeat rate")
            assert(ImGui.calcTypematicRepeatAmount(0.00f, 0.00f, 1.0f, 0.0f) == 1) // Trigger @ 0.0f, 1.0f
            assert(ImGui.calcTypematicRepeatAmount(0.99f, 1.01f, 1.0f, 0.0f) == 1) // "
            assert(ImGui.calcTypematicRepeatAmount(1.01f, 2.00f, 1.0f, 0.0f) == 0) // "

            ctx.logDebug("No repeat delay/rate")
            assert(ImGui.calcTypematicRepeatAmount(0.00f, 0.00f, 0.0f, 0.0f) == 1) // Trigger @ 0.0f
            assert(ImGui.calcTypematicRepeatAmount(0.01f, 1.01f, 0.0f, 0.0f) == 0) // "
        }
    }

    // ## Test ImGui::InputScalar() handling overflow for different data types
    e.registerTest("misc", "misc_input_scalar_overflow").let { t ->
        t.testFunc = {
            run {
                val one: Byte = 1
                var value: Byte = 2
                value = ImGui.dataTypeApplyOp(DataType.Byte, '+', value, one)
                assert(value == 3.b)
                value = Byte.MAX_VALUE
                value = ImGui.dataTypeApplyOp(DataType.Byte, '+', value, one)
                assert(value == Byte.MAX_VALUE)
                value = Byte.MIN_VALUE
                value = ImGui.dataTypeApplyOp(DataType.Byte, '-', value, one)
                assert(value == Byte.MIN_VALUE)
            }
            run {
                val one = Ubyte(1)
                var value = Ubyte(2)
                value = ImGui.dataTypeApplyOp(DataType.Ubyte, '+', value, one)
                assert(value == Ubyte(3))
                value = Ubyte.MAX
                value = ImGui.dataTypeApplyOp(DataType.Ubyte, '+', value, one)
                assert(value == Ubyte.MAX)
                value = Ubyte(0)
                value = ImGui.dataTypeApplyOp(DataType.Ubyte, '-', value, one)
                assert(value == Ubyte(0))
            }
            run {
                val one: Short = 1
                var value: Short = 2
                value = ImGui.dataTypeApplyOp(DataType.Short, '+', value, one)
                assert(value == 3.s)
                value = Short.MAX_VALUE
                value = ImGui.dataTypeApplyOp(DataType.Short, '+', value, one)
                assert(value == Short.MAX_VALUE)
                value = Short.MIN_VALUE
                value = ImGui.dataTypeApplyOp(DataType.Short, '-', value, one)
                assert(value == Short.MIN_VALUE)
            }
            run {
                val one = Ushort(1)
                var value = Ushort(2)
                value = ImGui.dataTypeApplyOp(DataType.Ushort, '+', value, one)
                assert(value == Ushort(3))
                value = Ushort.MAX
                value = ImGui.dataTypeApplyOp(DataType.Ushort, '+', value, one)
                assert(value == Ushort.MAX)
                value = Ushort(0)
                value = ImGui.dataTypeApplyOp(DataType.Ushort, '-', value, one)
                assert(value == Ushort(0))
            }
            run {
                val one = 1
                var value = 2
                value = ImGui.dataTypeApplyOp(DataType.Int, '+', value, one)
                assert(value == 3)
                value = Int.MAX_VALUE
                value = ImGui.dataTypeApplyOp(DataType.Int, '+', value, one)
                assert(value == Int.MAX_VALUE)
                value = Int.MIN_VALUE
                value = ImGui.dataTypeApplyOp(DataType.Int, '-', value, one)
                assert(value == Int.MIN_VALUE)
            }
            run {
                val one = Uint(1)
                var value = Uint(2)
                value = ImGui.dataTypeApplyOp(DataType.Uint, '+', value, one)
                assert(value == Uint(3))
                value = Uint.MAX
                value = ImGui.dataTypeApplyOp(DataType.Uint, '+', value, one)
                assert(value == Uint.MAX)
                value = Uint(0)
                value = ImGui.dataTypeApplyOp(DataType.Uint, '-', value, one)
                assert(value == Uint(0))
            }
            run {
                val one = 1L
                var value = 2L
                value = ImGui.dataTypeApplyOp(DataType.Long, '+', value, one)
                assert(value == 3L)
                value = Long.MAX_VALUE
                value = ImGui.dataTypeApplyOp(DataType.Long, '+', value, one)
                assert(value == Long.MAX_VALUE)
                value = Long.MIN_VALUE
                value = ImGui.dataTypeApplyOp(DataType.Long, '-', value, one)
                assert(value == Long.MIN_VALUE)
            }
            run {
                val one = Ulong(1)
                var value = Ulong(2)
                value = ImGui.dataTypeApplyOp(DataType.Ulong, '+', value, one)
                assert(value == Ulong(3))
                value = Ulong.MAX
                value = ImGui.dataTypeApplyOp(DataType.Ulong, '+', value, one)
                assert(value == Ulong.MAX)
                value = Ulong(0)
                value = ImGui.dataTypeApplyOp(DataType.Ulong, '-', value, one)
                assert(value == Ulong(0))
            }
        }
    }

    // ## Test basic clipboard, test that clipboard is empty on start
    e.registerTest("misc", "misc_clipboard").let { t ->
        t.testFunc = {
            // By specs, the testing system should provide an empty clipboard (we don't want user clipboard leaking into tests!)
            var clipboardText = ImGui.clipboardText
            assert(clipboardText == "")

            // Regular clipboard test
            val message = "Clippy is alive."
            ImGui.clipboardText = message
            clipboardText = ImGui.clipboardText
            assert(message == clipboardText)
        }
    }

    // ## Test UTF-8 encoding and decoding.
    // Note that this is ONLY testing encoding/decoding, we are not attempting to display those characters not trying to be i18n compliant
    e.registerTest("misc", "misc_utf8").let { t ->

        fun memCmp(a: CharArray, b: CharArray, num: Int): Boolean {
            for (i in 0 until num)
                if (a[i] != b[i])
                    return false
            return true
        }

        fun memCmp(a: ByteArray, b: ByteArray, num: Int): Boolean {
            for (i in 0 until num)
                if (a[i] != b[i])
                    return false
            return true
        }

        // FIXME-UTF8: Once Dear ImGui supports codepoints above 0xFFFF we should only use 32-bit code-point testing variant.
        fun checkUtf8_cp16(utf8_: String, unicode_: String): Boolean {
            val utf8 = utf8_.toByteArray()
            val unicode = unicode_.toCharArray()
//        IM_STATIC_ASSERT(sizeof(ImWchar) == sizeof(char16_t));
            val utf8_len = utf8.strlen()
            val maxChars = utf8_len * 4

            val converted = CharArray(maxChars)
            val reconverted = ByteArray(maxChars)

            // Convert UTF-8 text to unicode codepoints and check against expected value.
            var resultBytes = textStrFromUtf8(converted, utf8)
            var success = unicode.strlen == resultBytes && memCmp(converted, unicode, resultBytes)

            // Convert resulting unicode codepoints back to UTF-8 and check them against initial UTF-8 input value.
            if (success) {
                resultBytes = textStrToUtf8(reconverted, converted)
                success = success && utf8_len == resultBytes && memCmp(utf8, reconverted, resultBytes)
            }

            return success
        }

        t.testFunc = {
            fun CHECK_UTF8_CP16(text: String) = checkUtf8_cp16(text, text)
            // #define IM_CHECK_UTF8_CP32(_TEXT) (CheckUtf8_cp32(u8##_TEXT, U##_TEXT))

            // Test data taken from https://bitbucket.org/knight666/utf8rewind/src/default/testdata/big-list-of-naughty-strings-master/blns.txt

            // Special Characters
            // Strings which contain common special ASCII characters (may need to be escaped)
            assert(CHECK_UTF8_CP16(",./;'[]\\-="))
            assert(CHECK_UTF8_CP16("<>?:\"{}|_+"))
            assert(CHECK_UTF8_CP16("!@#$%^&*()`~"))

            // Unicode Symbols
            // Strings which contain common unicode symbols (e.g. smart quotes)
            assert(CHECK_UTF8_CP16("\u03a9\u2248\u00e7\u221a\u222b\u02dc\u00b5\u2264\u2265\u00f7"))
            assert(CHECK_UTF8_CP16("\u00e5\u00df\u2202\u0192\u00a9\u02d9\u2206\u02da\u00ac\u2026\u00e6"))
            assert(CHECK_UTF8_CP16("\u0153\u2211\u00b4\u00ae\u2020\u00a5\u00a8\u02c6\u00f8\u03c0\u201c\u2018"))
            assert(CHECK_UTF8_CP16("\u00a1\u2122\u00a3\u00a2\u221e\u00a7\u00b6\u2022\u00aa\u00ba\u2013\u2260"))
            assert(CHECK_UTF8_CP16("\u00b8\u02db\u00c7\u25ca\u0131\u02dc\u00c2\u00af\u02d8\u00bf"))
            assert(CHECK_UTF8_CP16("\u00c5\u00cd\u00ce\u00cf\u02dd\u00d3\u00d4\uf8ff\u00d2\u00da\u00c6\u2603"))
            assert(CHECK_UTF8_CP16("\u0152\u201e\u00b4\u2030\u02c7\u00c1\u00a8\u02c6\u00d8\u220f\u201d\u2019"))
            assert(CHECK_UTF8_CP16("`\u2044\u20ac\u2039\u203a\ufb01\ufb02\u2021\u00b0\u00b7\u201a\u2014\u00b1"))
            assert(CHECK_UTF8_CP16("\u215b\u215c\u215d\u215e"))
            assert(CHECK_UTF8_CP16("\u0401\u0402\u0403\u0404\u0405\u0406\u0407\u0408\u0409\u040a\u040b\u040c\u040d\u040e\u040f\u0410\u0411\u0412\u0413\u0414\u0415\u0416\u0417\u0418\u0419\u041a\u041b\u041c\u041d\u041e\u041f\u0420\u0421\u0422\u0423\u0424\u0425\u0426\u0427\u0428\u0429\u042a\u042b\u042c\u042d\u042e\u042f\u0430\u0431\u0432\u0433\u0434\u0435\u0436\u0437\u0438\u0439\u043a\u043b\u043c\u043d\u043e\u043f\u0440\u0441\u0442\u0443\u0444\u0445\u0446\u0447\u0448\u0449\u044a\u044b\u044c\u044d\u044e\u044f"))
            assert(CHECK_UTF8_CP16("\u0660\u0661\u0662\u0663\u0664\u0665\u0666\u0667\u0668\u0669"))

            // Unicode Subscript/Superscript
            // Strings which contain unicode subscripts/superscripts; can cause rendering issues
            assert(CHECK_UTF8_CP16("\u2070\u2074\u2075"))
            assert(CHECK_UTF8_CP16("\u2080\u2081\u2082"))
            assert(CHECK_UTF8_CP16("\u2070\u2074\u2075\u2080\u2081\u2082"))

            // Quotation Marks
            // Strings which contain misplaced quotation marks; can cause encoding errors
            assert(CHECK_UTF8_CP16("'"))
            assert(CHECK_UTF8_CP16("\""))
            assert(CHECK_UTF8_CP16("''"))
            assert(CHECK_UTF8_CP16("\"\""))
            assert(CHECK_UTF8_CP16("'\"'"))
            assert(CHECK_UTF8_CP16("\"''''\"'\""))
            assert(CHECK_UTF8_CP16("\"'\"'\"''''\""))

            // Two-Byte Characters
            // Strings which contain two-byte characters: can cause rendering issues or character-length issues
            assert(CHECK_UTF8_CP16("\u7530\u4e2d\u3055\u3093\u306b\u3042\u3052\u3066\u4e0b\u3055\u3044"))
            assert(CHECK_UTF8_CP16("\u30d1\u30fc\u30c6\u30a3\u30fc\u3078\u884c\u304b\u306a\u3044\u304b"))
            assert(CHECK_UTF8_CP16("\u548c\u88fd\u6f22\u8a9e"))
            assert(CHECK_UTF8_CP16("\u90e8\u843d\u683c"))
            assert(CHECK_UTF8_CP16("\uc0ac\ud68c\uacfc\ud559\uc6d0 \uc5b4\ud559\uc5f0\uad6c\uc18c"))
            assert(CHECK_UTF8_CP16("\ucc26\ucc28\ub97c \ud0c0\uace0 \uc628 \ud3b2\uc2dc\ub9e8\uacfc \uc45b\ub2e4\ub9ac \ub620\ubc29\uac01\ud558"))
            assert(CHECK_UTF8_CP16("\u793e\u6703\u79d1\u5b78\u9662\u8a9e\u5b78\u7814\u7a76\u6240"))
            assert(CHECK_UTF8_CP16("\uc6b8\ub780\ubc14\ud1a0\ub974"))
            // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0002070e\U00020731\U00020779\U00020c53\U00020c78\U00020c96\U00020ccf"));

            // Japanese Emoticons
            // Strings which consists of Japanese-style emoticons which are popular on the web
            assert(CHECK_UTF8_CP16("\u30fd\u0f3c\u0e88\u0644\u035c\u0e88\u0f3d\uff89 \u30fd\u0f3c\u0e88\u0644\u035c\u0e88\u0f3d\uff89"))
            assert(CHECK_UTF8_CP16("(\uff61\u25d5 \u2200 \u25d5\uff61)"))
            assert(CHECK_UTF8_CP16("\uff40\uff68(\u00b4\u2200\uff40\u2229"))
            assert(CHECK_UTF8_CP16("__\uff9b(,_,*)"))
            assert(CHECK_UTF8_CP16("\u30fb(\uffe3\u2200\uffe3)\u30fb:*:"))
            assert(CHECK_UTF8_CP16("\uff9f\uff65\u273f\u30fe\u2572(\uff61\u25d5\u203f\u25d5\uff61)\u2571\u273f\uff65\uff9f"))
            assert(CHECK_UTF8_CP16(",\u3002\u30fb:*:\u30fb\u309c\u2019( \u263b \u03c9 \u263b )\u3002\u30fb:*:\u30fb\u309c\u2019"))
            assert(CHECK_UTF8_CP16("(\u256f\u00b0\u25a1\u00b0\uff09\u256f\ufe35 \u253b\u2501\u253b)"))
            assert(CHECK_UTF8_CP16("(\uff89\u0ca5\u76ca\u0ca5\uff09\uff89\ufeff \u253b\u2501\u253b"))
            assert(CHECK_UTF8_CP16("( \u0361\u00b0 \u035c\u0296 \u0361\u00b0)"))

            // Emoji
            // Strings which contain Emoji; should be the same behavior as two-byte characters, but not always
            // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001f60d"));
            // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001f469\U0001f3fd"));
            // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001f47e \U0001f647 \U0001f481 \U0001f645 \U0001f646 \U0001f64b \U0001f64e \U0001f64d"));
            // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001f435 \U0001f648 \U0001f649 \U0001f64a"));
            // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\u2764\ufe0f \U0001f494 \U0001f48c \U0001f495 \U0001f49e \U0001f493 \U0001f497 \U0001f496 \U0001f498 \U0001f49d \U0001f49f \U0001f49c \U0001f49b \U0001f49a \U0001f499"));
            // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\u270b\U0001f3ff \U0001f4aa\U0001f3ff \U0001f450\U0001f3ff \U0001f64c\U0001f3ff \U0001f44f\U0001f3ff \U0001f64f\U0001f3ff"));
            // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001f6be \U0001f192 \U0001f193 \U0001f195 \U0001f196 \U0001f197 \U0001f199 \U0001f3e7"));
            // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("0\ufe0f\u20e3 1\ufe0f\u20e3 2\ufe0f\u20e3 3\ufe0f\u20e3 4\ufe0f\u20e3 5\ufe0f\u20e3 6\ufe0f\u20e3 7\ufe0f\u20e3 8\ufe0f\u20e3 9\ufe0f\u20e3 \U0001f51f"));

            // Unicode Numbers
            // Strings which contain unicode numbers; if the code is localized, it should see the input as numeric
            assert(CHECK_UTF8_CP16("\uff11\uff12\uff13"))
            assert(CHECK_UTF8_CP16("\u0661\u0662\u0663"))

            // Right-To-Left Strings
            // Strings which contain text that should be rendered RTL if possible (e.g. Arabic, Hebrew)
            assert(CHECK_UTF8_CP16("\u062b\u0645 \u0646\u0641\u0633 \u0633\u0642\u0637\u062a \u0648\u0628\u0627\u0644\u062a\u062d\u062f\u064a\u062f\u060c, \u062c\u0632\u064a\u0631\u062a\u064a \u0628\u0627\u0633\u062a\u062e\u062f\u0627\u0645 \u0623\u0646 \u062f\u0646\u0648. \u0625\u0630 \u0647\u0646\u0627\u061f \u0627\u0644\u0633\u062a\u0627\u0631 \u0648\u062a\u0646\u0635\u064a\u0628 \u0643\u0627\u0646. \u0623\u0647\u0651\u0644 \u0627\u064a\u0637\u0627\u0644\u064a\u0627\u060c \u0628\u0631\u064a\u0637\u0627\u0646\u064a\u0627-\u0641\u0631\u0646\u0633\u0627 \u0642\u062f \u0623\u062e\u0630. \u0633\u0644\u064a\u0645\u0627\u0646\u060c \u0625\u062a\u0641\u0627\u0642\u064a\u0629 \u0628\u064a\u0646 \u0645\u0627, \u064a\u0630\u0643\u0631 \u0627\u0644\u062d\u062f\u0648\u062f \u0623\u064a \u0628\u0639\u062f, \u0645\u0639\u0627\u0645\u0644\u0629 \u0628\u0648\u0644\u0646\u062f\u0627\u060c \u0627\u0644\u0625\u0637\u0644\u0627\u0642 \u0639\u0644 \u0625\u064a\u0648."))
            assert(CHECK_UTF8_CP16("\u05d1\u05b0\u05bc\u05e8\u05b5\u05d0\u05e9\u05b4\u05c1\u05d9\u05ea, \u05d1\u05b8\u05bc\u05e8\u05b8\u05d0 \u05d0\u05b1\u05dc\u05b9\u05d4\u05b4\u05d9\u05dd, \u05d0\u05b5\u05ea \u05d4\u05b7\u05e9\u05b8\u05bc\u05c1\u05de\u05b7\u05d9\u05b4\u05dd, \u05d5\u05b0\u05d0\u05b5\u05ea \u05d4\u05b8\u05d0\u05b8\u05e8\u05b6\u05e5"))
            assert(CHECK_UTF8_CP16("\u05d4\u05b8\u05d9\u05b0\u05ea\u05b8\u05d4test\u0627\u0644\u0635\u0641\u062d\u0627\u062a \u0627\u0644\u062a\u0651\u062d\u0648\u0644"))
            assert(CHECK_UTF8_CP16("\ufdfd"))
            assert(CHECK_UTF8_CP16("\ufdfa"))

            // Unicode Spaces
            // Strings which contain unicode space characters with special properties (c.f. https://www.cs.tut.fi/~jkorpela/chars/spaces.html)
            assert(CHECK_UTF8_CP16("\u200b"))
            assert(CHECK_UTF8_CP16("\u180e"))
            assert(CHECK_UTF8_CP16("\ufeff"))
            assert(CHECK_UTF8_CP16("\u2423"))
            assert(CHECK_UTF8_CP16("\u2422"))
            assert(CHECK_UTF8_CP16("\u2421"))

            // Trick Unicode
            // Strings which contain unicode with unusual properties (e.g. Right-to-left override) (c.f. http://www.unicode.org/charts/PDF/U2000.pdf)
            assert(CHECK_UTF8_CP16("\u202a\u202atest\u202a"))
            assert(CHECK_UTF8_CP16("\u202btest\u202b"))
            assert(CHECK_UTF8_CP16("test"))
            assert(CHECK_UTF8_CP16("test\u2060test\u202b"))
            assert(CHECK_UTF8_CP16("\u2066test\u2067"))

            // Zalgo Text
            // Strings which contain "corrupted" text. The corruption will not appear in non-HTML text, however. (via http://www.eeemo.net)
            assert(CHECK_UTF8_CP16("\u1e70\u033a\u033a\u0315o\u035e \u0337i\u0332\u032c\u0347\u032a\u0359n\u031d\u0317\u0355v\u031f\u031c\u0318\u0326\u035fo\u0336\u0319\u0330\u0320k\u00e8\u035a\u032e\u033a\u032a\u0339\u0331\u0324 \u0316t\u031d\u0355\u0333\u0323\u033b\u032a\u035eh\u033c\u0353\u0332\u0326\u0333\u0318\u0332e\u0347\u0323\u0330\u0326\u032c\u034e \u0322\u033c\u033b\u0331\u0318h\u035a\u034e\u0359\u031c\u0323\u0332\u0345i\u0326\u0332\u0323\u0330\u0324v\u033b\u034de\u033a\u032d\u0333\u032a\u0330-m\u0322i\u0345n\u0316\u033a\u031e\u0332\u032f\u0330d\u0335\u033c\u031f\u0359\u0329\u033c\u0318\u0333 \u031e\u0325\u0331\u0333\u032dr\u031b\u0317\u0318e\u0359p\u0360r\u033c\u031e\u033b\u032d\u0317e\u033a\u0320\u0323\u035fs\u0318\u0347\u0333\u034d\u031d\u0349e\u0349\u0325\u032f\u031e\u0332\u035a\u032c\u035c\u01f9\u032c\u034e\u034e\u031f\u0316\u0347\u0324t\u034d\u032c\u0324\u0353\u033c\u032d\u0358\u0345i\u032a\u0331n\u0360g\u0334\u0349 \u034f\u0349\u0345c\u032c\u031fh\u0361a\u032b\u033b\u032f\u0358o\u032b\u031f\u0316\u034d\u0319\u031d\u0349s\u0317\u0326\u0332.\u0328\u0339\u0348\u0323"))
            assert(CHECK_UTF8_CP16("\u0321\u0353\u031e\u0345I\u0317\u0318\u0326\u035dn\u0347\u0347\u0359v\u032e\u032bok\u0332\u032b\u0319\u0348i\u0316\u0359\u032d\u0339\u0320\u031en\u0321\u033b\u032e\u0323\u033ag\u0332\u0348\u0359\u032d\u0359\u032c\u034e \u0330t\u0354\u0326h\u031e\u0332e\u0322\u0324 \u034d\u032c\u0332\u0356f\u0334\u0318\u0355\u0323\u00e8\u0356\u1eb9\u0325\u0329l\u0356\u0354\u035ai\u0353\u035a\u0326\u0360n\u0356\u034d\u0317\u0353\u0333\u032eg\u034d \u0328o\u035a\u032a\u0361f\u0318\u0323\u032c \u0316\u0318\u0356\u031f\u0359\u032ec\u0489\u0354\u032b\u0356\u0353\u0347\u0356\u0345h\u0335\u0324\u0323\u035a\u0354\u00e1\u0317\u033c\u0355\u0345o\u033c\u0323\u0325s\u0331\u0348\u033a\u0316\u0326\u033b\u0362.\u031b\u0316\u031e\u0320\u032b\u0330"))
            assert(CHECK_UTF8_CP16("\u0317\u033a\u0356\u0339\u032f\u0353\u1e6e\u0324\u034d\u0325\u0347\u0348h\u0332\u0301e\u034f\u0353\u033c\u0317\u0319\u033c\u0323\u0354 \u0347\u031c\u0331\u0320\u0353\u034d\u0345N\u0355\u0360e\u0317\u0331z\u0318\u031d\u031c\u033a\u0359p\u0324\u033a\u0339\u034d\u032f\u035ae\u0320\u033b\u0320\u035cr\u0328\u0324\u034d\u033a\u0316\u0354\u0316\u0316d\u0320\u031f\u032d\u032c\u031d\u035fi\u0326\u0356\u0329\u0353\u0354\u0324a\u0320\u0317\u032c\u0349\u0319n\u035a\u035c \u033b\u031e\u0330\u035a\u0345h\u0335\u0349i\u0333\u031ev\u0322\u0347\u1e19\u034e\u035f-\u0489\u032d\u0329\u033c\u0354m\u0324\u032d\u032bi\u0355\u0347\u031d\u0326n\u0317\u0359\u1e0d\u031f \u032f\u0332\u0355\u035e\u01eb\u031f\u032f\u0330\u0332\u0359\u033b\u031df \u032a\u0330\u0330\u0317\u0316\u032d\u0318\u0358c\u0326\u034d\u0332\u031e\u034d\u0329\u0319\u1e25\u035aa\u032e\u034e\u031f\u0319\u035c\u01a1\u0329\u0339\u034es\u0324.\u031d\u031d \u0489Z\u0321\u0316\u031c\u0356\u0330\u0323\u0349\u031ca\u0356\u0330\u0359\u032c\u0361l\u0332\u032b\u0333\u034d\u0329g\u0321\u031f\u033c\u0331\u035a\u031e\u032c\u0345o\u0317\u035c.\u031f"))
            assert(CHECK_UTF8_CP16("\u0326H\u032c\u0324\u0317\u0324\u035de\u035c \u031c\u0325\u031d\u033b\u034d\u031f\u0301w\u0315h\u0316\u032f\u0353o\u031d\u0359\u0316\u034e\u0331\u032e \u0489\u033a\u0319\u031e\u031f\u0348W\u0337\u033c\u032da\u033a\u032a\u034d\u012f\u0348\u0355\u032d\u0359\u032f\u031ct\u0336\u033c\u032es\u0318\u0359\u0356\u0315 \u0320\u032b\u0320B\u033b\u034d\u0359\u0349\u0333\u0345e\u0335h\u0335\u032c\u0347\u032b\u0359i\u0339\u0353\u0333\u0333\u032e\u034e\u032b\u0315n\u035fd\u0334\u032a\u031c\u0316 \u0330\u0349\u0329\u0347\u0359\u0332\u035e\u0345T\u0356\u033c\u0353\u032a\u0362h\u034f\u0353\u032e\u033be\u032c\u031d\u031f\u0345 \u0324\u0339\u031dW\u0359\u031e\u031d\u0354\u0347\u035d\u0345a\u034f\u0353\u0354\u0339\u033c\u0323l\u0334\u0354\u0330\u0324\u031f\u0354\u1e3d\u032b.\u0355"))
            assert(CHECK_UTF8_CP16("Z\u032e\u031e\u0320\u0359\u0354\u0345\u1e00\u0317\u031e\u0348\u033b\u0317\u1e36\u0359\u034e\u032f\u0339\u031e\u0353G\u033bO\u032d\u0317\u032e"))

            // Unicode Upsidedown
            // Strings which contain unicode with an "upsidedown" effect (via http://www.upsidedowntext.com)
            assert(CHECK_UTF8_CP16("\u02d9\u0250nb\u1d09l\u0250 \u0250u\u0183\u0250\u026f \u01dd\u0279olop \u0287\u01dd \u01dd\u0279oq\u0250l \u0287n \u0287unp\u1d09p\u1d09\u0254u\u1d09 \u0279od\u026f\u01dd\u0287 po\u026fsn\u1d09\u01dd op p\u01dds '\u0287\u1d09l\u01dd \u0183u\u1d09\u0254s\u1d09d\u1d09p\u0250 \u0279n\u0287\u01dd\u0287\u0254\u01ddsuo\u0254 '\u0287\u01dd\u026f\u0250 \u0287\u1d09s \u0279olop \u026fnsd\u1d09 \u026f\u01dd\u0279o\u02e5"))
            assert(CHECK_UTF8_CP16("00\u02d9\u0196$-"))

            // Unicode font
            // Strings which contain bold/italic/etc. versions of normal characters
            assert(CHECK_UTF8_CP16("\uff34\uff48\uff45 \uff51\uff55\uff49\uff43\uff4b \uff42\uff52\uff4f\uff57\uff4e \uff46\uff4f\uff58 \uff4a\uff55\uff4d\uff50\uff53 \uff4f\uff56\uff45\uff52 \uff54\uff48\uff45 \uff4c\uff41\uff5a\uff59 \uff44\uff4f\uff47"))
            // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001d413\U0001d421\U0001d41e \U0001d42a\U0001d42e\U0001d422\U0001d41c\U0001d424 \U0001d41b\U0001d42b\U0001d428\U0001d430\U0001d427 \U0001d41f\U0001d428\U0001d431 \U0001d423\U0001d42e\U0001d426\U0001d429\U0001d42c \U0001d428\U0001d42f\U0001d41e\U0001d42b \U0001d42d\U0001d421\U0001d41e \U0001d425\U0001d41a\U0001d433\U0001d432 \U0001d41d\U0001d428\U0001d420"));
            // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001d57f\U0001d58d\U0001d58a \U0001d596\U0001d59a\U0001d58e\U0001d588\U0001d590 \U0001d587\U0001d597\U0001d594\U0001d59c\U0001d593 \U0001d58b\U0001d594\U0001d59d \U0001d58f\U0001d59a\U0001d592\U0001d595\U0001d598 \U0001d594\U0001d59b\U0001d58a\U0001d597 \U0001d599\U0001d58d\U0001d58a \U0001d591\U0001d586\U0001d59f\U0001d59e \U0001d589\U0001d594\U0001d58c"));
            // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001d47b\U0001d489\U0001d486 \U0001d492\U0001d496\U0001d48a\U0001d484\U0001d48c \U0001d483\U0001d493\U0001d490\U0001d498\U0001d48f \U0001d487\U0001d490\U0001d499 \U0001d48b\U0001d496\U0001d48e\U0001d491\U0001d494 \U0001d490\U0001d497\U0001d486\U0001d493 \U0001d495\U0001d489\U0001d486 \U0001d48d\U0001d482\U0001d49b\U0001d49a \U0001d485\U0001d490\U0001d488"));
            // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001d4e3\U0001d4f1\U0001d4ee \U0001d4fa\U0001d4fe\U0001d4f2\U0001d4ec\U0001d4f4 \U0001d4eb\U0001d4fb\U0001d4f8\U0001d500\U0001d4f7 \U0001d4ef\U0001d4f8\U0001d501 \U0001d4f3\U0001d4fe\U0001d4f6\U0001d4f9\U0001d4fc \U0001d4f8\U0001d4ff\U0001d4ee\U0001d4fb \U0001d4fd\U0001d4f1\U0001d4ee \U0001d4f5\U0001d4ea\U0001d503\U0001d502 \U0001d4ed\U0001d4f8\U0001d4f0"));
            // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001d54b\U0001d559\U0001d556 \U0001d562\U0001d566\U0001d55a\U0001d554\U0001d55c \U0001d553\U0001d563\U0001d560\U0001d568\U0001d55f \U0001d557\U0001d560\U0001d569 \U0001d55b\U0001d566\U0001d55e\U0001d561\U0001d564 \U0001d560\U0001d567\U0001d556\U0001d563 \U0001d565\U0001d559\U0001d556 \U0001d55d\U0001d552\U0001d56b\U0001d56a \U0001d555\U0001d560\U0001d558"));
            // IM_CHECK_NO_RET(IM_CHECK_UTF8_CP32("\U0001d683\U0001d691\U0001d68e \U0001d69a\U0001d69e\U0001d692\U0001d68c\U0001d694 \U0001d68b\U0001d69b\U0001d698\U0001d6a0\U0001d697 \U0001d68f\U0001d698\U0001d6a1 \U0001d693\U0001d69e\U0001d696\U0001d699\U0001d69c \U0001d698\U0001d69f\U0001d68e\U0001d69b \U0001d69d\U0001d691\U0001d68e \U0001d695\U0001d68a\U0001d6a3\U0001d6a2 \U0001d68d\U0001d698\U0001d690"));
            assert(CHECK_UTF8_CP16("\u24af\u24a3\u24a0 \u24ac\u24b0\u24a4\u249e\u24a6 \u249d\u24ad\u24aa\u24b2\u24a9 \u24a1\u24aa\u24b3 \u24a5\u24b0\u24a8\u24ab\u24ae \u24aa\u24b1\u24a0\u24ad \u24af\u24a3\u24a0 \u24a7\u249c\u24b5\u24b4 \u249f\u24aa\u24a2"))

            // iOS Vulnerability
            // Strings which crashed iMessage in iOS versions 8.3 and earlier
            assert(CHECK_UTF8_CP16("Power\u0644\u064f\u0644\u064f\u0635\u0651\u0628\u064f\u0644\u064f\u0644\u0635\u0651\u0628\u064f\u0631\u0631\u064b \u0963 \u0963h \u0963 \u0963\u5197"))
        }
    }

    // ## Test ImGuiTextFilter
    e.registerTest("misc", "misc_text_filter").let { t ->
        t.guiFunc = {
            dsl.window("Text filter") {
                filter.draw("Filter", ImGui.fontSize * 16)   // Test input filter drawing
            }
        }
        t.testFunc = { ctx: TestContext ->
            // Test ImGuiTextFilter::Draw()
            ctx.windowRef("Text filter")
            ctx.itemInput("Filter")
            ctx.keyCharsAppend("Big,Cat,, ,  ,Bird") // Trigger filter rebuild

            // Test functionality
            val filter = TextFilter()
            filter += "-bar"
            assert(!filter.passFilter("bartender"))
            assert(filter.passFilter("cartender"))

            filter.clear()
            filter += "bar "
            assert(filter.passFilter("bartender"))
            assert(!filter.passFilter("cartender"))

            filter.clear()
            filter += "bar"
            assert(filter.passFilter("bartender"))
            assert(!filter.passFilter("cartender"))
        }
    }

    // ## Visual ImBezierClosestPoint test.
    e.registerTest("misc", "misc_bezier_closest_point").let { t ->
        t.guiFunc = { ctx: TestContext ->

            val style = ctx.uiContext!!.style

            ImGui.setNextWindowSize(Vec2(600, 400), Cond.Appearing)
            ImGui.begin("Bezier", null, WindowFlag.NoSavedSettings.i)
            ImGui.dragInt("Segments", ::numSegments, 0.05f, 0, 20)

            val drawList = ImGui.windowDrawList
            val mousePos = ImGui.mousePos
            val wp = ImGui.windowPos

            // Draw modifiable control points
            for (pt in points) {
                val halfCircle = 2f
                val fullCircle = halfCircle * 2f
                val r = Rect(wp + pt - halfCircle, wp + pt + halfCircle)
                val id = ImGui.getID(points.indexOf(pt))

                ImGui.itemAdd(r, id)
                val isHovered = ImGui.isItemHovered()
                val isActive = ImGui.isItemActive
                if (isHovered || isActive)
                    drawList.addCircleFilled(r.center, fullCircle, COL32(0, 255, 0, 255))
                else
                    drawList.addCircle(r.center, fullCircle, COL32(0, 255, 0, 255))

                if (isActive)
                    if (ImGui.isMouseDown(MouseButton.Left))
                        pt put (mousePos - wp)
                    else
                        ImGui.clearActiveID()
                else if (ImGui.isMouseDown(MouseButton.Left) && isHovered)
                    ImGui.setActiveID(id, ImGui.currentWindow)
            }
            drawList.addLine(wp + points[0], wp + points[1], COL32(0, 255, 0, 100))
            drawList.addLine(wp + points[2], wp + points[3], COL32(0, 255, 0, 100))

            // Draw curve itself
            drawList.addBezierCurve(wp + points[0], wp + points[1], wp + points[2], wp + points[3], COL32_WHITE, 2f, numSegments)

            // Draw point closest to the mouse cursor
            val point = when (numSegments) {
                0 -> bezierClosestPointCasteljau(wp + points[0], wp + points[1], wp + points[2], wp + points[3], mousePos, style.curveTessellationTol)
                else -> bezierClosestPoint(wp + points[0], wp + points[1], wp + points[2], wp + points[3], mousePos, numSegments)
            }
            drawList.addCircleFilled(point, 4f, COL32(255, 0, 0, 255))

            ImGui.end()
        }
    }

    // FIXME-TESTS
    e.registerTest("demo", "demo_misc_001").let { t ->
        t.testFunc = { ctx: TestContext ->
            ctx.windowRef("Dear ImGui Demo")
            ctx.itemOpen("Widgets")
            ctx.itemOpen("Basic")
            ctx.itemClick("Basic/Button")
            ctx.itemClick("Basic/radio a")
            ctx.itemClick("Basic/radio b")
            ctx.itemClick("Basic/radio c")
            ctx.itemClick("Basic/combo")
            ctx.itemClick("Basic/combo")
            ctx.itemClick("Basic/color 2/##ColorButton")
            //ctx->ItemClick("##Combo/BBBB");     // id chain
            ctx.sleepShort()
            ctx.popupClose()

            //ctx->ItemClick("Layout");  // FIXME: close popup
            ctx.itemOpen("Layout")
            ctx.itemOpen("Scrolling")
            ctx.itemHold("Scrolling/>>", 1f)
            ctx.sleepShort()
        }
    }

    // ## Coverage: open everything in demo window
    // ## Extra: test for inconsistent ScrollMax.y across whole demo window
    // ## Extra: run Log/Capture api on whole demo window
//    e.registerTest("demo", "demo_cov_auto_open").let { t ->
//        t.testFunc = { ctx: TestContext ->
//            ctx.windowRef("Dear ImGui Demo")
//            ctx.itemOpenAll("")
//
//            // Additional tests we bundled here because we are benefiting from the "opened all" state
//            val window = ctx.getWindowByRef("")!!
//            ctx.scrollVerifyScrollMax(window)
//
//            // Test the Log/Capture api
//            var clipboard = ImGui.clipboardText
//            assert(clipboard.isEmpty())
//            ctx.itemClick("Capture\\/Logging/LogButtons/Log To Clipboard")
//            clipboard = ImGui.clipboardText
//            val clipboardLen = clipboard.length
//            assert(clipboardLen >= 15000) // This is going to vary (as of 2019-11-18 on Master this 22766)
//        }
//    }

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

val defaultRanges = arrayOf(
        IntRange(0x0020, 0x00FF), // Basic Latin + Latin Supplement
        IntRange(0x0080, 0x00FF)) // Latin_Supplement

val filter = TextFilter()

// FIXME-TESTS: Store normalized?
val points = arrayOf(Vec2(30, 75), Vec2(185, 355), Vec2(400, 60), Vec2(590, 370))
var numSegments = 0