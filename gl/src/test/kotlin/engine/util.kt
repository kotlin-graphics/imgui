package engine

import com.github.ajalt.mordant.AnsiColorCode
import com.github.ajalt.mordant.TermColors
import gli_.has
import glm_.*
import glm_.vec4.Vec4
import imgui.*
import imgui.classes.InputTextCallbackData
import imgui.internal.sections.ItemFlag
import io.kotest.matchers.shouldBe
import org.lwjgl.system.Platform
import unsigned.toUInt
import java.io.File
import java.io.PrintStream
import java.util.*

inline class KeyModFlags(val i: Int)       // See ImGuiKeyModFlags_
{
    infix fun has(f: KeyModFlag): Boolean = i has f.i.i
    infix fun or(f: KeyModFlags) = KeyModFlags(i or f.i)
    infix fun wo(f: KeyModFlags) = KeyModFlags(i wo f.i)
}

inline class KeyModFlag(val i: KeyModFlags) {
    infix fun or(f: KeyModFlag) = KeyModFlags(i.i or f.i.i)

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

class BuildInfo {
    val type = when {
        java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0 ->
            "Debug"
        else -> "Release"
    }
    val cpu = when(System.getProperty("sun.arch.data.model")) {
        "32" -> "X86"
        "64" -> "X64"
        else -> "Unknown"
    }
    val os = System.getProperty("os.name") //Platform.get()
    val compiler = "ojdk ${System.getProperty("java.version")}"
    val date = Date()           // "YYYY-MM-DD"
    var time = System.currentTimeMillis()          //
    override fun toString() = "$type, $cpu, $os, $compiler, $date"
}


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
    if (str.isNotEmpty() && str[0] == '/'.b)
        seed = 0

    seed = seed.inv()
    var crc = seed

    // Zero-terminated string
    var inhibitOne = false
    var current = 0
    var c = str.getOrElse(current++) { 0.b }
    while (c != 0.b) {
        if (c == '\\'.b && !inhibitOne) {
            inhibitOne = true
            c = str.getOrElse(current++) { 0 }
            continue
        }

        // Forward slashes are ignored unless prefixed with a backward slash
        if (c == '/'.b && !inhibitOne) {
            inhibitOne = false
            c = str.getOrElse(current++) { 0 }
            continue
        }

        // Reset the hash when encountering ###
        if (c == '#'.b && str[current] == '#'.b && str[current + 1] == '#'.b)
            crc = seed

        crc = (crc ushr 8) xor crc32Lut[(crc and 0xFF) xor c.toUInt()]
        inhibitOne = false
        c = str.getOrElse(current++) { 0 }
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

fun sleepInMilliseconds(ms: Int) = Thread.sleep(ms.L)

//ImU64       ImGetTimeInMicroseconds();
//
//bool        ImOsCreateProcess(const char* cmd_line);
fun osOpenInShell(path: String) = Unit // TODD
fun osConsoleSetTextColor(stream: PrintStream, color: OsConsoleTextColor) = Unit
fun osIsDebuggerPresent() = true

fun pathFindFilename(path: String): String = path.substringAfterLast('/').substringAfterLast('\\')
fun pathFindDirectory(path: String): String = path.substringBeforeLast('/').substringBeforeLast('\\')

//void        ImPathFixSeparatorsForCurrentOS(char* buf);
//
//void        ImParseSplitCommandLine(int* out_argc, char const*** out_argv, const char* cmd_line);
//void        ImParseDateFromCompilerIntoYMD(const char* in_data, char* out_buf, size_t out_buf_size);

fun fileCreateDirectoryChain(path: String) {
    val dir = path.substringBeforeLast('/')
    File(dir).mkdir()
}

//bool        ImFileLoadSourceBlurb(const char* filename, int line_no_start, int line_no_end, ImGuiTextBuffer* out_buf);
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

//const ImBuildInfo&  ImGetBuildInfo(); [JVM] -> simply instantiate BuildInfo
//ImFont*     FindFontByName(const char* name);

// Helper: maintain/calculate moving average
class MovingAverageDouble(val sampleCount: Int) {
    val samples = DoubleArray(sampleCount)
    var accum = 0.0
    var idx = 0
    var fillAmount = 0

    operator fun plusAssign(v_: Number) {
        val v = v_.d
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

//-----------------------------------------------------------------------------
// STR + InputText bindings (FIXME: move to Str.cpp?)
//-----------------------------------------------------------------------------

class InputTextCallbackStr_UserData(
        var strObj: ByteArray,
        var chainCallback: InputTextCallback?,
        var chainCallbackUserData: Any?)

val inputTextCallback: InputTextCallback = { data: InputTextCallbackData ->
    val userData = data.userData as InputTextCallbackStr_UserData
    when {
        data.eventFlag == InputTextFlag.CallbackResize.i -> {
            // Resize string callback
            // If for some reason we refuse the new length (BufTextLen) and/or capacity (BufSize) we need to set them back to what we want.
            val str = userData.strObj
            data.buf.cStr shouldBe str.cStr
            if (str.size < data.bufTextLen)
                userData.strObj = str.copyInto(ByteArray(data.bufTextLen))
            data.buf = userData.strObj
            false
        }
        userData.chainCallback != null -> {
            // Forward to user callback, if any
            data.userData = userData.chainCallbackUserData
            userData.chainCallback!!(data)
        }
        else -> false
    }
}

//namespace ImGui
//{
fun ImGui.inputText_(label: String, str: ByteArray, flags_: InputTextFlags = InputTextFlag.None.i,
                     callback: InputTextCallback? = null, userData: Any? = null): Boolean {

    var flags = flags_
    assert(flags hasnt InputTextFlag.CallbackResize)
    flags = flags or InputTextFlag.CallbackResize

    val cbUserData = InputTextCallbackStr_UserData(str, callback, userData)
    return inputText(label, str.cStr, flags, inputTextCallback, cbUserData)
}
//    bool    InputTextMultiline(const char* label, Str* str, const ImVec2& size = ImVec2(0, 0), ImGuiInputTextFlags flags = 0, ImGuiInputTextCallback callback = NULL, void* user_data = NULL);
//}
