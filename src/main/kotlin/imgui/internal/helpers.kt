package imgui.internal

import glm_.*
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import imgui.Dir
import imgui.NUL
import kool.rem
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import kotlin.math.abs
import kotlin.reflect.KMutableProperty0

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


// Helpers: String


/** Trim str by offsetting contents when there's leading data + writing a \0 at the trailing position.
 *  We use this in situation where the cost is negligible. */
fun trimBlanks(buf: CharArray): CharArray {
    var p = 0
    while (buf[p] == ' ' || buf[p] == '\t')     // Leading blanks
        p++
    val start = p
    p = buf.size    // end of string
    while (p > start && (buf[p - 1] == ' ' || buf[p - 1] == '\t'))  // Trailing blanks
        p--
    return when (start) {
        0 -> buf
        else -> CharArray(p - start) { buf[start + it] }
    }
}

// -----------------------------------------------------------------------------------------------------------------
// Helpers: Misc
// -----------------------------------------------------------------------------------------------------------------

fun hash(data: IntArray, seed: Int = 0): Int {
    val buffer = ByteBuffer.allocate(data.size * Int.BYTES).order(ByteOrder.LITTLE_ENDIAN) // as C
    for (i in data.indices) buffer.putInt(i * Int.BYTES, data[i])
    val bytes = ByteArray(buffer.rem) { buffer[it] }
    return hash(String(bytes, StandardCharsets.ISO_8859_1), bytes.size, seed)
}

/** CRC32 needs a 1KB lookup table (not cache friendly)
 *  Although the code to generate the table is simple and shorter than the table itself, using a const table allows us to easily:
 *  - avoid an unnecessary branch/memory tap, - keep the ImHashXXX functions usable by static constructors, - make it thread-safe. */
val GCrc32LookupTable = longArrayOf(
        0x00000000, 0x77073096, 0xEE0E612C, 0x990951BA, 0x076DC419, 0x706AF48F, 0xE963A535, 0x9E6495A3, 0x0EDB8832, 0x79DCB8A4, 0xE0D5E91E, 0x97D2D988, 0x09B64C2B, 0x7EB17CBD, 0xE7B82D07, 0x90BF1D91,
        0x1DB71064, 0x6AB020F2, 0xF3B97148, 0x84BE41DE, 0x1ADAD47D, 0x6DDDE4EB, 0xF4D4B551, 0x83D385C7, 0x136C9856, 0x646BA8C0, 0xFD62F97A, 0x8A65C9EC, 0x14015C4F, 0x63066CD9, 0xFA0F3D63, 0x8D080DF5,
        0x3B6E20C8, 0x4C69105E, 0xD56041E4, 0xA2677172, 0x3C03E4D1, 0x4B04D447, 0xD20D85FD, 0xA50AB56B, 0x35B5A8FA, 0x42B2986C, 0xDBBBC9D6, 0xACBCF940, 0x32D86CE3, 0x45DF5C75, 0xDCD60DCF, 0xABD13D59,
        0x26D930AC, 0x51DE003A, 0xC8D75180, 0xBFD06116, 0x21B4F4B5, 0x56B3C423, 0xCFBA9599, 0xB8BDA50F, 0x2802B89E, 0x5F058808, 0xC60CD9B2, 0xB10BE924, 0x2F6F7C87, 0x58684C11, 0xC1611DAB, 0xB6662D3D,
        0x76DC4190, 0x01DB7106, 0x98D220BC, 0xEFD5102A, 0x71B18589, 0x06B6B51F, 0x9FBFE4A5, 0xE8B8D433, 0x7807C9A2, 0x0F00F934, 0x9609A88E, 0xE10E9818, 0x7F6A0DBB, 0x086D3D2D, 0x91646C97, 0xE6635C01,
        0x6B6B51F4, 0x1C6C6162, 0x856530D8, 0xF262004E, 0x6C0695ED, 0x1B01A57B, 0x8208F4C1, 0xF50FC457, 0x65B0D9C6, 0x12B7E950, 0x8BBEB8EA, 0xFCB9887C, 0x62DD1DDF, 0x15DA2D49, 0x8CD37CF3, 0xFBD44C65,
        0x4DB26158, 0x3AB551CE, 0xA3BC0074, 0xD4BB30E2, 0x4ADFA541, 0x3DD895D7, 0xA4D1C46D, 0xD3D6F4FB, 0x4369E96A, 0x346ED9FC, 0xAD678846, 0xDA60B8D0, 0x44042D73, 0x33031DE5, 0xAA0A4C5F, 0xDD0D7CC9,
        0x5005713C, 0x270241AA, 0xBE0B1010, 0xC90C2086, 0x5768B525, 0x206F85B3, 0xB966D409, 0xCE61E49F, 0x5EDEF90E, 0x29D9C998, 0xB0D09822, 0xC7D7A8B4, 0x59B33D17, 0x2EB40D81, 0xB7BD5C3B, 0xC0BA6CAD,
        0xEDB88320, 0x9ABFB3B6, 0x03B6E20C, 0x74B1D29A, 0xEAD54739, 0x9DD277AF, 0x04DB2615, 0x73DC1683, 0xE3630B12, 0x94643B84, 0x0D6D6A3E, 0x7A6A5AA8, 0xE40ECF0B, 0x9309FF9D, 0x0A00AE27, 0x7D079EB1,
        0xF00F9344, 0x8708A3D2, 0x1E01F268, 0x6906C2FE, 0xF762575D, 0x806567CB, 0x196C3671, 0x6E6B06E7, 0xFED41B76, 0x89D32BE0, 0x10DA7A5A, 0x67DD4ACC, 0xF9B9DF6F, 0x8EBEEFF9, 0x17B7BE43, 0x60B08ED5,
        0xD6D6A3E8, 0xA1D1937E, 0x38D8C2C4, 0x4FDFF252, 0xD1BB67F1, 0xA6BC5767, 0x3FB506DD, 0x48B2364B, 0xD80D2BDA, 0xAF0A1B4C, 0x36034AF6, 0x41047A60, 0xDF60EFC3, 0xA867DF55, 0x316E8EEF, 0x4669BE79,
        0xCB61B38C, 0xBC66831A, 0x256FD2A0, 0x5268E236, 0xCC0C7795, 0xBB0B4703, 0x220216B9, 0x5505262F, 0xC5BA3BBE, 0xB2BD0B28, 0x2BB45A92, 0x5CB36A04, 0xC2D7FFA7, 0xB5D0CF31, 0x2CD99E8B, 0x5BDEAE1D,
        0x9B64C2B0, 0xEC63F226, 0x756AA39C, 0x026D930A, 0x9C0906A9, 0xEB0E363F, 0x72076785, 0x05005713, 0x95BF4A82, 0xE2B87A14, 0x7BB12BAE, 0x0CB61B38, 0x92D28E9B, 0xE5D5BE0D, 0x7CDCEFB7, 0x0BDBDF21,
        0x86D3D2D4, 0xF1D4E242, 0x68DDB3F8, 0x1FDA836E, 0x81BE16CD, 0xF6B9265B, 0x6FB077E1, 0x18B74777, 0x88085AE6, 0xFF0F6A70, 0x66063BCA, 0x11010B5C, 0x8F659EFF, 0xF862AE69, 0x616BFFD3, 0x166CCF45,
        0xA00AE278, 0xD70DD2EE, 0x4E048354, 0x3903B3C2, 0xA7672661, 0xD06016F7, 0x4969474D, 0x3E6E77DB, 0xAED16A4A, 0xD9D65ADC, 0x40DF0B66, 0x37D83BF0, 0xA9BCAE53, 0xDEBB9EC5, 0x47B2CF7F, 0x30B5FFE9,
        0xBDBDF21C, 0xCABAC28A, 0x53B39330, 0x24B4A3A6, 0xBAD03605, 0xCDD70693, 0x54DE5729, 0x23D967BF, 0xB3667A2E, 0xC4614AB8, 0x5D681B02, 0x2A6F2B94, 0xB40BBE37, 0xC30C8EA1, 0x5A05DF1B, 0x2D02EF8D)
        .map { it.i }.toIntArray()


/** Known size hash
 *  It is ok to call ImHashData on a string with known length but the ### operator won't be supported.
 *  FIXME-OPT: Replace with e.g. FNV1a hash? CRC32 pretty much randomly access 1KB. Need to do proper measurements. */
fun hash(data: ByteBuffer, dataSize_: Int = data.rem, seed: Int = 0): Int {
    var crc = seed.inv()
    val crc32Lut = GCrc32LookupTable
    var dataSize = dataSize_
    while (dataSize-- != 0)
        crc = (crc ushr 8) xor crc32Lut[(crc and 0xFF) xor data.get().i]
    return crc.inv()
}

fun hash(data: String, dataSize_: Int, seed_: Int = 0): Int {

    val seed = seed_.inv()
    var crc = seed
    var src = 0
    val crc32Lut = GCrc32LookupTable

    var dataSize = dataSize_
    if (dataSize != 0)
        while (dataSize-- != 0) {
            val c = data[src++]
            if (c == '#' && data[src] == '#' && data[src + 1] == '#')
                crc = seed
            crc = (crc ushr 8) xor crc32Lut[(crc and 0xFF) xor c.i]
        }
    else
        while (src < data.length) {
            val c = data[src++]
            if (c == '#' && data[src] == '#' && data[src + 1] == '#')
                crc = seed
            crc = (crc ushr 8) xor crc32Lut[(crc and 0xFF) xor c]
        }
    return crc.inv()
}

fun fileLoadToCharArray(filename: String, paddingBytes: Int = 0) = ClassLoader.getSystemResourceAsStream(filename)?.use {
    val bytes = it.readBytes()
    CharArray(bytes.size) { bytes[it].c }
}

fun fileLoadToLines(filename: String): List<String>? {
    val file = File(Paths.get("imgui.ini").toUri())
    return file.takeIf { it.exists() && it.canRead() }?.readLines()
}


//IMGUI_API FILE*         ImFileOpen(const char* filename, const char* file_open_mode);


// Helpers: Geometry

fun lineClosestPoint(a: Vec2, b: Vec2, p: Vec2): Vec2 {
    val ap = p - a
    val abDir = b - a
    val dot = ap.x * abDir.x + ap.y * abDir.y
    return when {
        dot < 0f -> a
        else -> {
            val abLenSqr = abDir.x * abDir.x + abDir.y * abDir.y
            return when {
                dot > abLenSqr -> b
                else -> a + abDir * dot / abLenSqr
            }
        }
    }
}

fun triangleContainsPoint(a: Vec2, b: Vec2, c: Vec2, p: Vec2): Boolean {
    val b1 = ((p.x - b.x) * (a.y - b.y) - (p.y - b.y) * (a.x - b.x)) < 0f
    val b2 = ((p.x - c.x) * (b.y - c.y) - (p.y - c.y) * (b.x - c.x)) < 0f
    val b3 = ((p.x - a.x) * (c.y - a.y) - (p.y - a.y) * (c.x - a.x)) < 0f
    return ((b1 == b2) && (b2 == b3))
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

fun triangleBarycentricCoords(a: Vec2, b: Vec2, c: Vec2, p: Vec2): FloatArray {
    val v0 = b - a
    val v1 = c - a
    val v2 = p - a
    val denom = v0.x * v1.y - v1.x * v0.y
    val outV = (v2.x * v1.y - v1.x * v2.y) / denom
    val outW = (v0.x * v2.y - v2.x * v0.y) / denom
    val outU = 1f - outV - outW
    return floatArrayOf(outU, outV, outW)
}

fun getDirQuadrantFromDelta(dx: Float, dy: Float) = when {
    abs(dx) > abs(dy) -> when {
        dx > 0f -> Dir.Right
        else -> Dir.Left
    }
    else -> when {
        dy > 0f -> Dir.Down
        else -> Dir.Up
    }
}

val Char.isBlankA get() = this == ' ' || this == '\t'
val Char.isBlankW get() = this == ' ' || this == '\t' || i == 0x3000
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
        while (this[n] != NUL) n++
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

// ---------------------------------------------------------------------------------------------------------------------
// Helpers: ImVec2/ImVec4 operators
// We are keeping those disabled by default so they don't leak in user space, to allow user enabling
// implicit cast operators between ImVec2 and their own types (using IM_VEC2_CLASS_EXTRA etc.)
// We unfortunately don't have a unary- operator for Vec2 because this would needs to be defined inside the class itself.
// ---------------------------------------------------------------------------------------------------------------------
val Vec2.lengthSqr get() = x * x + y * y

fun saturate(f: Float) = if (f < 0f) 0f else if (f > 1f) 1f else f

fun swap(a: KMutableProperty0<Float>, b: KMutableProperty0<Float>) {
    val tmp = a()
    a.set(b())
    b.set(tmp)
}

fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
fun lerp(a: Double, b: Double, t: Float) = a + (b - a) * t
fun lerp(a: Int, b: Int, t: Float) = (a + (b - a) * t).i
fun lerp(a: Long, b: Long, t: Float) = (a + (b - a) * t).L
fun Vec2.lerp(b: Vec2, t: Float) = Vec2(x + (b.x - x) * t, y + (b.y - y) * t)
fun Vec2.lerp(b: Vec2, t: Vec2) = Vec2(x + (b.x - x) * t.x, y + (b.y - y) * t.y)
fun Vec2.lerp(b: Vec2i, t: Vec2) = Vec2(x + (b.x - x) * t.x, y + (b.y - y) * t.y)
fun Vec2i.lerp(b: Vec2i, t: Vec2) = Vec2(x + (b.x - x) * t.x, y + (b.y - y) * t.y)
fun Vec4.lerp(b: Vec4, t: Float) = Vec4(x + (b.x - x) * t, y + (b.y - y) * t, z + (b.z - z) * t, w + (b.w - w) * t)

infix fun Vec2.invLength(failValue: Float): Float {
    val d = x * x + y * y
    if (d > 0f)
        return 1f / glm.sqrt(d)
    return failValue
}

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
//        if (src[i] == NUL) break
        this[i] = src[i]
    }
}

/** ~ImTextStrFromUtf8 */
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

fun String.memchr(startIdx: Int, c: Char): Int? {
    val res = indexOf(c, startIdx)
    return if (res >= 0) res else null
}

fun CharArray.memchr(startIdx: Int, c: Char): Int? {
    for (index in startIdx until size)
        if (c == this[index])
            return index
    return null
}