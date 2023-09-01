package imgui.api

import glm_.b
import imgui.ImGui
import imgui.ImGui.sameLine
import imgui.ImGui.tableHeadersRow
import imgui.ImGui.tableNextColumn
import imgui.ImGui.tableSetupColumn
import imgui.ImGui.text
import imgui.ImGui.textUnformatted
import imgui.TableFlag
import imgui.div
import imgui.dsl.table
import imgui.internal.textCharFromUtf8
import imgui.or
import uno.kotlin.NUL

// Debug Utilities
interface debugUtilities {

    /** Helper tool to diagnose between text encoding issues and font loading issues. Pass your UTF-8 string and verify that there are correct. */
    fun debugTextEncoding(str: ByteArray) {
        text("Text: \"$str\"")
        table("##DebugTextEncoding", 4, TableFlag.Borders / TableFlag.RowBg / TableFlag.SizingFixedFit / TableFlag.Resizable) {
            tableSetupColumn("Offset")
            tableSetupColumn("UTF-8")
            tableSetupColumn("Glyph")
            tableSetupColumn("Codepoint")
            tableHeadersRow()
            var p = 0
            while (p < str.size && str[p] != 0.b) {
                val (c, cUtf8Len) = textCharFromUtf8(str, p, -1)
                tableNextColumn()
                text("$p")
                tableNextColumn()
                for (byteIndex in 0 until cUtf8Len) {
                    if (byteIndex > 0)
                        sameLine()
                    text("0x%02X", str[p + byteIndex])
                }
                tableNextColumn()
                if (ImGui.font.findGlyphNoFallback(Char(c)) != null)
                    textUnformatted(str.copyOfRange(p, p + cUtf8Len), cUtf8Len)
                else
                    textUnformatted("[missing]")
                tableNextColumn()
                text("U+%04X", c)
                p += cUtf8Len
            }
        }
    }
    //IMGUI_API bool          DebugCheckVersionAndDataLayout(const char* version_str, size_t sz_io, size_t sz_style, size_t sz_vec2, size_t sz_vec4, size_t sz_drawvert, size_t sz_drawidx); // This is called by IMGUI_CHECKVERSION() macro.
}