package imgui.internal

import glm_.*
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import imgui.NUL
import java.nio.ByteBuffer

// -----------------------------------------------------------------------------------------------------------------
// Helpers: UTF-8 <> wchar
// -----------------------------------------------------------------------------------------------------------------

//IMGUI_API int           ImTextStrToUtf8(char* buf, int buf_size, const ImWchar* in_text, const ImWchar* in_text_end);      // return output UTF-8 bytes count

/** Convert UTF-8 to 32-bits character, process single character input.
 *  Based on stb_from_utf8() from github.com/nothings/stb/
 *  We handle UTF-8 decoding error by skipping forward. */
//fun textCharFromUtf8(unsigned int* out_char, const char* in_text, const char* in_text_end):Int{
//    unsigned int c = (unsigned int)-1;
//    const unsigned char* str = (const unsigned char*)in_text;
//    if (!(*str & 0x80))
//    {
//        c = (unsigned int)(*str++);
//        *out_char = c;
//        return 1;
//    }
//    if ((*str & 0xe0) == 0xc0)
//    {
//        *out_char = 0xFFFD; // will be invalid but not end of string
//        if (in_text_end && in_text_end - (const char*)str < 2) return 1;
//        if (*str < 0xc2) return 2;
//        c = (unsigned int)((*str++ & 0x1f) << 6);
//        if ((*str & 0xc0) != 0x80) return 2;
//        c += (*str++ & 0x3f);
//        *out_char = c;
//        return 2;
//    }
//    if ((*str & 0xf0) == 0xe0)
//    {
//        *out_char = 0xFFFD; // will be invalid but not end of string
//        if (in_text_end && in_text_end - (const char*)str < 3) return 1;
//        if (*str == 0xe0 && (str[1] < 0xa0 || str[1] > 0xbf)) return 3;
//        if (*str == 0xed && str[1] > 0x9f) return 3; // str[1] < 0x80 is checked below
//        c = (unsigned int)((*str++ & 0x0f) << 12);
//        if ((*str & 0xc0) != 0x80) return 3;
//        c += (unsigned int)((*str++ & 0x3f) << 6);
//        if ((*str & 0xc0) != 0x80) return 3;
//        c += (*str++ & 0x3f);
//        *out_char = c;
//        return 3;
//    }
//    if ((*str & 0xf8) == 0xf0)
//    {
//        *out_char = 0xFFFD; // will be invalid but not end of string
//        if (in_text_end && in_text_end - (const char*)str < 4) return 1;
//        if (*str > 0xf4) return 4;
//        if (*str == 0xf0 && (str[1] < 0x90 || str[1] > 0xbf)) return 4;
//        if (*str == 0xf4 && str[1] > 0x8f) return 4; // str[1] < 0x80 is checked below
//        c = (unsigned int)((*str++ & 0x07) << 18);
//        if ((*str & 0xc0) != 0x80) return 4;
//        c += (unsigned int)((*str++ & 0x3f) << 12);
//        if ((*str & 0xc0) != 0x80) return 4;
//        c += (unsigned int)((*str++ & 0x3f) << 6);
//        if ((*str & 0xc0) != 0x80) return 4;
//        c += (*str++ & 0x3f);
//        // utf-8 encodings of values used in surrogate pairs are invalid
//        if ((c & 0xFFFFF800) == 0xD800) return 4;
//        *out_char = c;
//        return 4;
//    }
//    *out_char = 0;
//    return 0;
//}
//IMGUI_API int           ImTextStrFromUtf8(ImWchar* buf, int buf_size, const char* in_text, const char* in_text_end, const char** in_remaining = NULL);   // return input UTF-8 bytes count
//IMGUI_API int           ImTextCountCharsFromUtf8(const char* in_text, const char* in_text_end);                            // return number of UTF-8 code-points (NOT bytes count)
//IMGUI_API int           ImTextCountUtf8BytesFromStr(const ImWchar* in_text, const ImWchar* in_text_end);                   // return number of bytes to express string as UTF-8 code-points


// -----------------------------------------------------------------------------------------------------------------
// Helpers: Misc
// -----------------------------------------------------------------------------------------------------------------

fun hash(data: IntArray, seed: Int = 0): Int {
    val buffer = ByteBuffer.allocate(data.size * Int.BYTES)
    for (i in data.indices) buffer.putInt(i * Int.BYTES, data[i])
    val bytes = ByteArray(buffer.size, { buffer[it] })
    return hash(String(bytes), bytes.size, seed)
}

/** Pass data_size==0 for zero-terminated strings
FIXME-OPT: Replace with e.g. FNV1a hash? CRC32 pretty much randomly access 1KB. Need to do proper measurements. */
fun hash(data: String, dataSize: Int, seed: Int = 0): Int {

    val seed = seed.inv()
    var crc = seed
    var current = 0

    var dataSize = dataSize
    if (dataSize > 0)
    // Known size
        while (dataSize-- != 0)
            crc = (crc ushr 8) xor crc32Lut[(crc and 0xFF) xor data[current++].i]
    else
    // Zero-terminated string
        while (current < data.length) {
            val c = data[current++]
            /*  We support a syntax of "label###id" where only "###id" is included in the hash, and only "label" gets
                displayed.
                Because this syntax is rarely used we are optimizing for the common case.
                    - If we reach ### in the string we discard the hash so far and reset to the seed.
                    - We don't do 'current += 2; continue;' after handling ### to keep the code smaller.    */
            if (c == '#' && data[current] == '#' && data[current + 1] == '#')
                crc = seed
            crc = (crc ushr 8) xor crc32Lut[(crc and 0xFF) xor c]
        }
    return crc.inv()
}

val crc32Lut = IntArray(256, {
    val polynomial = 0xEDB88320.i
    var crc = it
    for (i in 0..7)
        crc = (crc ushr 1) xor ((-(crc and 1)) and polynomial)
    crc
})

fun fileLoadToCharArray(filename: String, paddingBytes: Int = 0) = ClassLoader.getSystemResourceAsStream(filename)?.use {
    val bytes = it.readBytes()
    CharArray(bytes.size, { bytes[it].c })
}

fun fileLoadToLines(filename: String) = ClassLoader.getSystemResourceAsStream(filename)?.use { it.bufferedReader().readLines() }


//IMGUI_API FILE*         ImFileOpen(const char* filename, const char* file_open_mode);


// Helpers: Geometry

fun lineClosestPoint(a: Vec2, b: Vec2, p: Vec2): Vec2 {
    val ap = p - a
    var abDir = b - a
    val abLen = glm.sqrt(abDir.x * abDir.x + abDir.y * abDir.y)
    abDir timesAssign 1f / abLen
    val dot = ap.x * abDir.x + ap.y * abDir.y
    return when {
        dot < 0f -> a
        dot > abLen -> b
        else -> a + abDir * dot
    }
}

fun triangleContainsPoint(a: Vec2, b: Vec2, c: Vec2, p: Vec2): Boolean {
    val b1 = ((p.x - b.x) * (a.y - b.y) - (p.y - b.y) * (a.x - b.x)) < 0f
    val b2 = ((p.x - c.x) * (b.y - c.y) - (p.y - c.y) * (b.x - c.x)) < 0f
    val b3 = ((p.x - a.x) * (c.y - a.y) - (p.y - a.y) * (c.x - a.x)) < 0f
    return ((b1 == b2) && (b2 == b3))
}

fun triangleBarycentricCoords(a: Vec2, b: Vec2, c: Vec2, p: Vec2, outU: Float = 0f, outV: Float = 0f, outW: Float = 0f): FloatArray {
    val v0 = b - a
    val v1 = c - a
    val v2 = p - a
    val denom = v0.x * v1.y - v1.x * v0.y
    return floatArrayOf(
            (v2.x * v1.y - v1.x * v2.y) / denom,
            (v0.x * v2.y - v2.x * v0.y) / denom,
            1f - outV - outW)
}

fun triangleClosestPoint(a: Vec2, b: Vec2, c: Vec2, p: Vec2): Vec2 {
    val projAB = lineClosestPoint(a, b, p)
    val projBC = lineClosestPoint(b, c, p)
    val projCA = lineClosestPoint(c, a, p)
    val dist2AB = (p - projAB).lengthSqr
    val dist2BC = (p - projBC).lengthSqr
    val dist2CA = (p - projCA).lengthSqr
    val m = glm.min(dist2AB, glm.min(dist2BC, dist2CA))
    return when (m) {
        dist2AB -> projAB
        dist2BC -> projBC
        else -> projCA
    }
}

val Char.isSpace get() = this == ' ' || this == '\t' || this.i == 0x3000
val Int.isPowerOfTwo get() = this != 0 && (this and (this - 1)) == 0
val Int.upperPowerOfTwo: Int
    get() {
        var v = this - 1
        v = v or (v ushr 1)
        v = v or (v ushr 2)
        v = v or (v ushr 4)
        v = v or (v ushr 8)
        v = v or (v ushr 16)
        v++
        return v
    }


// -----------------------------------------------------------------------------------------------------------------
// Helpers: String
// -----------------------------------------------------------------------------------------------------------------

//IMGUI_API int           ImStricmp(const char* str1, const char* str2);
//IMGUI_API int           ImStrnicmp(const char* str1, const char* str2, int count);
//IMGUI_API char*         ImStrdup(const char* str);

val CharArray.strlenW: Int
    get() {
        var n = 0
        while (this[n] != 0.c) n++
        return n
    }

/** Find beginning-of-line  */
fun CharArray.beginOfLine(midLine: Int): Int {
    var res = midLine
    while (res > 0 && this[res - 1] != '\n') res--
    return res
}
//IMGUI_API const char*   ImStristr(const char* haystack, const char* haystack_end, const char* needle, const char* needle_end);
//IMGUI_API int           ImFormatString(char* buf, int buf_size, const char* fmt, ...) IM_PRINTFARGS(3);
//IMGUI_API int           ImFormatStringV(char* buf, int buf_size, const char* fmt, va_list args);

// -----------------------------------------------------------------------------------------------------------------
// Helpers: Math
// -----------------------------------------------------------------------------------------------------------------
val Vec2.lengthSqr get() = x * x + y * y

fun saturate(f: Float) = if (f < 0f) 0f else if (f > 1f) 1f else f

fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
fun lerp(a: Int, b: Int, t: Float) = (a + (b - a) * t).i
fun Vec2.lerp(b: Vec2, t: Float) = Vec2(x + (b.x - x) * t, y + (b.y - y) * t)
fun Vec2.lerp(b: Vec2, t: Vec2) = Vec2(x + (b.x - x) * t.x, y + (b.y - y) * t.y)
fun Vec2.lerp(b: Vec2i, t: Vec2) = Vec2(x + (b.x - x) * t.x, y + (b.y - y) * t.y)
fun Vec2i.lerp(b: Vec2i, t: Vec2) = Vec2(x + (b.x - x) * t.x, y + (b.y - y) * t.y)
fun Vec4.lerp(b: Vec4, t: Float) = Vec4(x + (b.x - x) * t, y + (b.y - y) * t, z + (b.z - z) * t, w + (b.w - w) * t)

fun Vec2.invLength(failValue: Float): Float {
    val d = x * x + y * y
    if (d > 0f)
        return 1f / glm.sqrt(d)
    return failValue
}

fun Vec2.rotate_(cosA: Float, sinA: Float) = put(x * cosA - y * sinA, x * sinA + y * cosA)
fun Vec2.rotate(cosA: Float, sinA: Float) = Vec2(x * cosA - y * sinA, x * sinA + y * cosA)
fun linearSweep(current: Float, target: Float, speed: Float) = when {
    current < target -> glm.min(current + speed, target)
    current > target -> glm.max(current - speed, target)
    else -> current
}


// JVM IMGUI
infix fun CharArray.strncpy(src: CharArray) = strncpy(src, size)

fun CharArray.strncpy(src: CharArray, count: Int) {
    if (count < 1) return
    for (i in 0 until count) {
        if (src[i] == NUL) break
        this[i] = src[i]
    }
}

fun CharArray.textStr(src: CharArray): Int {
    var i = 0
    while (i < size) {
        if (src[i] == NUL) break
        this[i] = src[i++]
    }
    return i
}

val CharArray.strlen: Int
    get() {
        var i = 0
        while (i < size && this[i] != NUL) i++
        return i
    }

// TODO precision? i.e: 02X
fun String.scanHex(ints: IntArray, count: Int = ints.size, precision: Int) {
    var c = 0
    for (i in 0 until count) {
        val end = glm.min((i + 1) * precision, length)
        ints[i] = if (c > end) 0 else with(substring(c, end)) { if (isEmpty()) 0 else toInt(16) }
        c += precision
    }
}

fun String.strchr(startIdx: Int, c: Char): Int? {
    val res = indexOf(c, startIdx)
    return if (res >= 0) res else null
}