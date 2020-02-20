package engine

import com.github.ajalt.mordant.AnsiColorCode
import com.github.ajalt.mordant.TermColors
import gli_.has
import glm_.b
import glm_.i
import glm_.vec4.Vec4
import imgui.Col
import imgui.ID
import imgui.ImGui
import imgui.internal.ItemFlag
import org.lwjgl.system.Platform
import unsigned.toUInt
import java.io.PrintStream

inline class KeyModFlags(val i: Int)       // See ImGuiKeyModFlags_
{
    infix fun has(f: KeyModFlag): Boolean = i has f.i.i
}

inline class KeyModFlag(val i: KeyModFlags) {
    companion object {
        val None = KeyModFlag(KeyModFlags(0))
        val Ctrl = KeyModFlag(KeyModFlags(1 shl 0))
        val Alt = KeyModFlag(KeyModFlags(1 shl 1))
        val Shift = KeyModFlag(KeyModFlags(1 shl 2))
        val Super = KeyModFlag(KeyModFlags(1 shl 3))
        val Shortcut = when (Platform.get()) {
            Platform.MACOSX -> Super
            else -> Ctrl
        }
    }
}

enum class KeyState {
    Unknown,
    Up,       // Released
    Down      // Pressed/held
}

//enum ImOsConsoleStream
//{
//    ImOsConsoleStream_StandardOutput,
//    ImOsConsoleStream_StandardError
//};

val termColor = TermColors()
typealias OsConsoleTextColor = AnsiColorCode
//enum class OsConsoleTextColor { Black, White, BrightWhite, BrightRed, BrightGreen, BrightBlue, BrightYellow }

//struct ImBuildInfo
//{
//    const char* Type = "";
//    const char* Cpu = "";
//    const char* OS = "";
//    const char* Compiler = "";
//    char        Date[32];           // "YYYY-MM-DD"
//    const char* Time = "";          //
//};


// Helpers: miscellaneous functions


// Hash "hello/world" as if it was "helloworld"
// To hash a forward slash we need to use "hello\\/world"
//   IM_ASSERT(ImHashDecoratedPath("Hello/world")   == ImHash("Helloworld", 0));
//   IM_ASSERT(ImHashDecoratedPath("Hello\\/world") == ImHash("Hello/world", 0));
// Adapted from ImHash(). Not particularly fast!
fun hashDecoratedPath(str_: String, seed_: ID = 0): ID {

    val str = str_.toByteArray()
    var seed = seed_

    // Prefixing the string with / ignore the seed
    if (str[0] == '/'.b)
        seed = 0

    seed = seed.inv()
    var crc = seed

    // Zero-terminated string
    var inhibitOne = false
    var current = 0
    var c = str[current++]
    while (c != 0.b) {
        if (c == '\\'.b && !inhibitOne) {
            inhibitOne = true
            c = str[current++]
            continue
        }

        // Forward slashes are ignored unless prefixed with a backward slash
        if (c == '/'.b && !inhibitOne) {
            inhibitOne = false
            c = str[current++]
            continue
        }

        // Reset the hash when encountering ###
        if (c == '#'.b && str[current] == '#'.b && str[current + 1] == '#'.b)
            crc = seed

        crc = (crc ushr 8) xor crc32Lut[(crc and 0xFF) xor c.toUInt()]
        inhibitOne = false
    }
    return crc.inv()
}

val crc32Lut by lazy {
    val polynomial = 0xEDB88320.i
    IntArray(256) {
        var crc = it
        for (j in 0..7)
            crc = (crc ushr 1) xor (-(crc and 1) and polynomial)
        crc
    }
}

//void        ImSleepInMilliseconds(int ms);
//ImU64       ImGetTimeInMicroseconds();
//
//bool        ImOsCreateProcess(const char* cmd_line);
//void        ImOsOpenInShell(const char* path);
fun osConsoleSetTextColor(stream: PrintStream, color: OsConsoleTextColor) = Unit
fun osIsDebuggerPresent() = true

fun pathFindFilename(path: String): String = path.substringAfterLast('/').substringAfterLast('\\')

//void        ImPathFixSeparatorsForCurrentOS(char* buf);
//
//void        ImParseSplitCommandLine(int* out_argc, char const*** out_argv, const char* cmd_line);
//void        ImParseDateFromCompilerIntoYMD(const char* in_data, char* out_buf, size_t out_buf_size);
//
//bool        ImFileCreateDirectoryChain(const char* path, const char* path_end = NULL);
//bool        ImFileLoadSourceBlurb(const char* file_name, int line_no_start, int line_no_end, ImGuiTextBuffer* out_buf);
//void        ImDebugShowInputTextState();
//
//const char* GetImGuiKeyName(ImGuiKey key);

fun getKeyModsPrefixStr(modFlags: KeyModFlags): String {
    var res = ""
    if (modFlags != KeyModFlag.None.i) {
        if (modFlags has KeyModFlag.Ctrl) res += "Ctrl+"
        if (modFlags has KeyModFlag.Alt) res += "Alt+"
        if (modFlags has KeyModFlag.Shift) res += "Shift+"
        if (modFlags has KeyModFlag.Super) res += "Super+"
    }
    return res
}
//const ImBuildInfo&  ImGetBuildInfo();
//ImFont*     FindFontByName(const char* name);

// Helper: maintain/calculate moving average
class MovingAverageDouble(val sampleCount: Int) {
    val samples = DoubleArray(sampleCount)
    var accum = 0.0
    var idx = 0
    var fillAmount = 0

    operator fun plusAssign(v: Double) {
        accum += v - samples[idx]
        samples[idx] = v
        if (++idx == samples.size)
            idx = 0
        if (fillAmount < samples.size)
            fillAmount++
    }

    val average get() = accum / fillAmount
    val isFull get() = fillAmount == samples.size
}

//-----------------------------------------------------------------------------
// Misc ImGui extensions
//-----------------------------------------------------------------------------

fun ImGui.pushDisabled() {
    val col = style.colors[Col.Text]
    pushItemFlag(ItemFlag.Disabled.i, true)
    pushStyleColor(Col.Text, Vec4(col.x, col.y, col.z, col.w * 0.5f))
}

fun ImGui.popDisabled() {
    popStyleColor()
    popItemFlag()
}

////-----------------------------------------------------------------------------
//// STR + InputText bindings (FIXME: move to Str.cpp?)
////-----------------------------------------------------------------------------
//
//class Str;
//namespace ImGui
//{
//    bool    InputText(const char* label, Str* str, ImGuiInputTextFlags flags = 0, ImGuiInputTextCallback callback = NULL, void* user_data = NULL);
//    bool    InputTextMultiline(const char* label, Str* str, const ImVec2& size = ImVec2(0, 0), ImGuiInputTextFlags flags = 0, ImGuiInputTextCallback callback = NULL, void* user_data = NULL);
//}
