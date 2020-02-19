package imgui.test.engine

import gli_.has
import org.lwjgl.system.Platform

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
//
//enum ImOsConsoleTextColor
//{
//    ImOsConsoleTextColor_Black,
//    ImOsConsoleTextColor_White,
//    ImOsConsoleTextColor_BrightWhite,
//    ImOsConsoleTextColor_BrightRed,
//    ImOsConsoleTextColor_BrightGreen,
//    ImOsConsoleTextColor_BrightBlue,
//    ImOsConsoleTextColor_BrightYellow
//};
//
//struct ImBuildInfo
//{
//    const char* Type = "";
//    const char* Cpu = "";
//    const char* OS = "";
//    const char* Compiler = "";
//    char        Date[32];           // "YYYY-MM-DD"
//    const char* Time = "";          //
//};
//
//// Helpers: miscellaneous functions
//ImGuiID     ImHashDecoratedPath(const char* str, ImGuiID seed = 0);
//void        ImSleepInMilliseconds(int ms);
//ImU64       ImGetTimeInMicroseconds();
//
//bool        ImOsCreateProcess(const char* cmd_line);
//void        ImOsOpenInShell(const char* path);
//void        ImOsConsoleSetTextColor(ImOsConsoleStream stream, ImOsConsoleTextColor color);
//bool        ImOsIsDebuggerPresent();
//
//const char* ImPathFindFilename(const char* path, const char* path_end = NULL);
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
//
//// Helper: maintain/calculate moving average
//template<typename TYPE>
//struct ImMovingAverage
//{
//    ImVector<TYPE>  Samples;
//    TYPE            Accum;
//    int             Idx;
//    int             FillAmount;
//
//    ImMovingAverage()               { Accum = (TYPE)0; Idx = FillAmount = 0; }
//    void    Init(int count)         { Samples.resize(count); memset(Samples.Data, 0, Samples.Size * sizeof(TYPE)); Accum = (TYPE)0; Idx = FillAmount = 0; }
//    void    AddSample(TYPE v)       { Accum += v - Samples[Idx]; Samples[Idx] = v; if (++Idx == Samples.Size) Idx = 0; if (FillAmount < Samples.Size) FillAmount++;  }
//    TYPE    GetAverage() const      { return Accum / (TYPE)FillAmount; }
//    int     GetSampleCount() const  { return Samples.Size; }
//    bool    IsFull() const          { return FillAmount == Samples.Size; }
//};
//
////-----------------------------------------------------------------------------
//// Misc ImGui extensions
////-----------------------------------------------------------------------------
//
//namespace ImGui
//{
//    void    PushDisabled();
//    void    PopDisabled();
//}
//
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
