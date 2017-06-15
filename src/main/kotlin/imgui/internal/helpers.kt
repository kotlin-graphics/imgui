package imgui.internal

import glm_.i
import glm_.vec2.Vec2
import glm_.xor
import imgui.Context as g

// -----------------------------------------------------------------------------------------------------------------
// Helpers: UTF-8 <> wchar
// -----------------------------------------------------------------------------------------------------------------

//IMGUI_API int           ImTextStrToUtf8(char* buf, int buf_size, const ImWchar* in_text, const ImWchar* in_text_end);      // return output UTF-8 bytes count
//IMGUI_API int           ImTextCharFromUtf8(unsigned int* out_char, const char* in_text, const char* in_text_end);          // return input UTF-8 bytes count
//IMGUI_API int           ImTextStrFromUtf8(ImWchar* buf, int buf_size, const char* in_text, const char* in_text_end, const char** in_remaining = NULL);   // return input UTF-8 bytes count
//IMGUI_API int           ImTextCountCharsFromUtf8(const char* in_text, const char* in_text_end);                            // return number of UTF-8 code-points (NOT bytes count)
//IMGUI_API int           ImTextCountUtf8BytesFromStr(const ImWchar* in_text, const ImWchar* in_text_end);                   // return number of bytes to express string as UTF-8 code-points


// -----------------------------------------------------------------------------------------------------------------
// Helpers: Misc
// -----------------------------------------------------------------------------------------------------------------

/** Pass data_size==0 for zero-terminated strings
FIXME-OPT: Replace with e.g. FNV1a hash? CRC32 pretty much randomly access 1KB. Need to do proper measurements. */
fun hash(data: String, dataSize: Int, seed: Int = 0): Int {

    val crc32_lut = IntArray(256)
    if (crc32_lut[1] == 0) {
        val polynomial = 0xEDB88320.i
        repeat(256) {
            var crc = it
            for (i in 0 until 8)
                crc = (crc ushr 1) xor (-(crc and 1) and polynomial)
            crc32_lut[it] = crc
        }
    }

    val seed = seed.inv()
    var crc = seed
    var current = 0

    var dataSize = dataSize
    if (dataSize > 0)
    // Known size
        while (dataSize-- != 0)
            crc = (crc ushr 8) xor crc32_lut[(crc and 0xFF) xor data[current++].i]
    else
    // Zero-terminated string
        while (current < data.length) {

            val c = data[current]
            /*  We support a syntax of "label###id" where only "###id" is included in the hash, and only "label" gets
                displayed.
                Because this syntax is rarely used we are optimizing for the common case.
                    - If we reach ### in the string we discard the hash so far and reset to the seed.
                    - We don't do 'current += 2; continue;' after handling ### to keep the code smaller.    */
            if (c == '#' && data[current] == '#' && data[current + 1] == '#')
                crc = seed
            crc = (crc ushr 8) xor crc32_lut[(crc and 0xFF) xor c]

            current++
        }
    return crc.inv()
}
//IMGUI_API void*         ImFileLoadToMemory(const char* filename, const char* file_open_mode, int* out_file_size = NULL, int padding_bytes = 0);
//IMGUI_API FILE*         ImFileOpen(const char* filename, const char* file_open_mode);
//IMGUI_API bool          ImIsPointInTriangle(const ImVec2& p, const ImVec2& a, const ImVec2& b, const ImVec2& c);
//static inline bool      ImCharIsSpace(int c)            { return c == ' ' || c == '\t' || c == 0x3000; }
//static inline int       ImUpperPowerOfTwo(int v)        { v--; v |= v >> 1; v |= v >> 2; v |= v >> 4; v |= v >> 8; v |= v >> 16; v++; return v; }


// -----------------------------------------------------------------------------------------------------------------
// Helpers: String
// -----------------------------------------------------------------------------------------------------------------

//IMGUI_API int           ImStricmp(const char* str1, const char* str2);
//IMGUI_API int           ImStrnicmp(const char* str1, const char* str2, int count);
//IMGUI_API char*         ImStrdup(const char* str);
//IMGUI_API int           ImStrlenW(const ImWchar* str);
//IMGUI_API const ImWchar*ImStrbolW(const ImWchar* buf_mid_line, const ImWchar* buf_begin); // Find beginning-of-line
//IMGUI_API const char*   ImStristr(const char* haystack, const char* haystack_end, const char* needle, const char* needle_end);
//IMGUI_API int           ImFormatString(char* buf, int buf_size, const char* fmt, ...) IM_PRINTFARGS(3);
//IMGUI_API int           ImFormatStringV(char* buf, int buf_size, const char* fmt, va_list args);

// -----------------------------------------------------------------------------------------------------------------
// Helpers: Math
// -----------------------------------------------------------------------------------------------------------------
fun lengthSqr(lhs: Vec2) = lhs.x * lhs.x + lhs.y * lhs.y