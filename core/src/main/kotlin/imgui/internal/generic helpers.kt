package imgui.internal

import glm_.*
import glm_.vec1.Vec1i
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import imgui.*
import imgui.api.g
import kool.BYTES
import kool.rem
import uno.kotlin.NUL
import unsigned.toBigInt
import unsigned.toUInt
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import kotlin.math.abs
import kotlin.reflect.KMutableProperty0


//-----------------------------------------------------------------------------
// [SECTION] Generic helpers
//-----------------------------------------------------------------------------

/** Unsaturated, for display purpose    */
fun F32_TO_INT8_UNBOUND(_val: Float) = (_val * 255f + if (_val >= 0) 0.5f else -0.5f).i

/** Saturated, always output 0..255 */
fun F32_TO_INT8_SAT(_val: Float) = (saturate(_val) * 255f + 0.5f).i

fun round(f: Float): Float = (f + 0.5f).i.f


// -----------------------------------------------------------------------------------------------------------------
// Error handling
// Down the line in some frameworks/languages we would like to have a way to redirect those to the programmer and recover from more faults.
// -----------------------------------------------------------------------------------------------------------------
//#ifndef IMGUI_USER_ERROR
//#define IMGUI_USER_ERROR(_EXPR, _MSG)   IM_ASSERT((_EXPR) && (_MSG))    // Recoverable User Error
//#endif


// -----------------------------------------------------------------------------------------------------------------
// Helpers: Hashing
// -----------------------------------------------------------------------------------------------------------------
fun fileLoadToMemory(filename: String): CharArray? =
    ClassLoader.getSystemResourceAsStream(filename)?.use { s ->
        val bytes = s.readBytes()
        CharArray(bytes.size) { bytes[it].c }
    }

/** [JVM] */
fun hashData(data: Int, seed: Int = 0): ID {
    val buffer = ByteBuffer.allocate(Int.BYTES).order(ByteOrder.LITTLE_ENDIAN) // as C
    buffer.putInt(0, data)
    return hashData(buffer, seed)
}

/** [JVM] */
fun hashData(data: IntArray, seed: Int = 0): ID {
    val buffer = ByteBuffer.allocate(data.size * Int.BYTES).order(ByteOrder.LITTLE_ENDIAN) // as C
    for (i in data.indices) buffer.putInt(i * Int.BYTES, data[i])
    val bytes = ByteArray(buffer.rem) { buffer[it] }
    return hashStr(String(bytes, StandardCharsets.ISO_8859_1), bytes.size, seed)
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
fun hashData(data: ByteArray, seed: Int = 0): ID {
    var crc = seed.inv()
    val crc32Lut = GCrc32LookupTable
    var b = 0
    var dataSize = data.size
    while (dataSize-- != 0)
        crc = (crc ushr 8) xor crc32Lut[(crc and 0xFF) xor data[b++].toUInt()]
    return crc.inv()
}

/** Known size hash
 *  It is ok to call ImHashData on a string with known length but the ### operator won't be supported.
 *  FIXME-OPT: Replace with e.g. FNV1a hash? CRC32 pretty much randomly access 1KB. Need to do proper measurements. */
fun hashData(data: ByteBuffer, seed: Int = 0): ID {
    var crc = seed.inv()
    val crc32Lut = GCrc32LookupTable
    var dataSize = data.rem
    while (dataSize-- != 0)
        crc = (crc ushr 8) xor crc32Lut[(crc and 0xFF) xor data.get().toUInt()]
    return crc.inv()
}

fun hashStr(data: String, dataSize_: Int = 0, seed_: Int = 0): ID {

    /*
    convert to "Extended ASCII" Windows-1252 (CP1252) https://en.wikipedia.org/wiki/Windows-1252
    this caused crashes with `-` which in CP1252 is 150, while on UTF16 is 8211
     */
    //    val data = data_.toByteArray(Charset.forName("Cp1252"))

    val ast = '#' //.b
    val seed = seed_.inv()
    var crc = seed
    var src = 0
    val crc32Lut = GCrc32LookupTable

    var dataSize = dataSize_
    if (dataSize != 0)
        while (dataSize-- != 0) {
            val c = data[src++]
            if (c == ast && data[src] == ast && data[src + 1] == ast)
                crc = seed
            crc = (crc ushr 8) xor crc32Lut[(crc and 0xFF) xor c.i]
        }
    else
        while (src < data.length) {
            val c = data[src++]
            if (c == ast && data.getOrNull(src) == ast && data.getOrNull(src + 1) == ast)
                crc = seed
            //            val b = crc ushr 8
            //            val d = crc and 0xFF
            //            val e = d xor c.b.toUnsignedInt
            //            crc = b xor crc32Lut[e]
            crc = (crc ushr 8) xor crc32Lut[(crc and 0xFF) xor c.b.toUInt()] // unsigned -> avoid negative values being passed as indices
        }
    return crc.inv()
}

// Helpers: Color Blending
fun alphaBlendColors(colA: Int, colB: Int): Int {
    val t = ((colB ushr COL32_A_SHIFT) and 0xFF) / 255.f
    val r = lerp((colA ushr COL32_R_SHIFT) and 0xFF, (colB ushr COL32_R_SHIFT) and 0xFF, t)
    val g = lerp((colA ushr COL32_G_SHIFT) and 0xFF, (colB ushr COL32_G_SHIFT) and 0xFF, t)
    val b = lerp((colA ushr COL32_B_SHIFT) and 0xFF, (colB ushr COL32_B_SHIFT) and 0xFF, t)
    return COL32(r, g, b, 0xFF)
}

// -----------------------------------------------------------------------------------------------------------------
// Helpers: Bit manipulation
// -----------------------------------------------------------------------------------------------------------------
val Int.isPowerOfTwo: Boolean
    get() = this != 0 && (this and (this - 1)) == 0
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
// Helpers: String, Formatting
// [SECTION] MISC HELPERS/UTILITIES (String, Format, Hash functions)
// -----------------------------------------------------------------------------------------------------------------


//IMGUI_API int           ImStricmp(const char* str1, const char* str2);
//IMGUI_API int           ImStrnicmp(const char* str1, const char* str2, size_t count);
// [JVM] => System.arrayCopy
//IMGUI_API void          ImStrncpy(char* dst, const char* src, size_t count);

//IMGUI_API char*         ImStrdup(const char* str);
//IMGUI_API char*         ImStrdupcpy(char* dst, size_t* p_dst_size, const char* str);
fun strchrRange(str: ByteArray, strBegin: Int, strEnd: Int, c: Char): Int {
    for (i in strBegin until strEnd)
        if (str[i] == c.b)
            return i
    return -1
}

val CharArray.strlenW: Int
    get() {
        var n = 0
        while (this[n] != NUL) n++
        return n
    }
//IMGUI_API const char*   ImStreolRange(const char* str, const char* str_end);                // End end-of-line
//IMGUI_API const ImWchar*ImStrbolW(const ImWchar* buf_mid_line, const ImWchar* buf_begin);   // Find beginning-of-line
//IMGUI_API const char*   ImStristr(const char* haystack, const char* haystack_end, const char* needle, const char* needle_end);
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

//IMGUI_API const char*   ImStrSkipBlank(const char* str);
//IMGUI_API int           ImFormatString(char* buf, size_t buf_size, const char* fmt, ...) IM_FMTARGS(3);

fun formatString(buf: ByteArray, fmt: String, vararg args: Any): Int {
    val bytes = fmt.format(g.style.locale, *args).toByteArray()
    bytes.copyInto(buf) // TODO IndexOutOfBoundsException?
    return bytes.size.also { w -> buf[w] = 0 }
}

//IMGUI_API const char*   ImParseFormatFindStart(const char* format);
//IMGUI_API const char*   ImParseFormatFindEnd(const char* format);
//IMGUI_API const char*   ImParseFormatTrimDecorations(const char* format, char* buf, size_t buf_size);
//IMGUI_API int           ImParseFormatPrecision(const char* format, int default_value);
fun charIsBlankA(c: Int): Boolean = c == ' '.i || c == '\t'.i

val Char.isBlankA: Boolean
    get() = this == ' ' || this == '\t'

fun charIsBlankW(c: Int): Boolean = c == ' '.i || c == '\t'.i || c == 0x3000

val Char.isBlankW: Boolean
    get() = this == ' ' || this == '\t' || i == 0x3000


// -----------------------------------------------------------------------------------------------------------------
// Helpers: UTF-8 <> wchar
// [SECTION] MISC HELPERS/UTILITIES (ImText* functions)
// -----------------------------------------------------------------------------------------------------------------

/* return out_buf */
fun textCharToUtf8(outBuf: ByteArray, c: Int): ByteArray {
    val count = textCharToUtf8Inline(outBuf, 5, c)
    outBuf[count] = 0
    return outBuf
}

/** return output UTF-8 bytes count */
fun textStrToUtf8(outBuf: ByteArray, text: CharArray): Int {
    var b = 0
    var t = 0
    while (b < outBuf.size && t < text.size && text[t] != NUL) {
        val c = text[t++].i
        if (c < 0x80)
            outBuf[b++] = c.b
        else
            b += textCharToUtf8Inline(outBuf, b, c)
    }
    if (b < outBuf.size) outBuf[b] = 0
    return b
}

/** Based on stb_to_utf8() from github.com/nothings/stb/
 *  ~ImTextCharToUtf8   */
fun textCharToUtf8Inline(buf: ByteArray, b: Int, c: Int): Int {
    if (c < 0x80) {
        buf[b + 0] = c.b
        return 1
    }
    if (c < 0x800) {
        if (buf.size < b + 2) return 0
        buf[b + 0] = (0xc0 + (c ushr 6)).b
        buf[b + 1] = (0x80 + (c and 0x3f)).b
        return 2
    }
    if (c < 0x10000) {
        if (buf.size < 3) return 0
        buf[0] = (0xe0 + (c ushr 12)).b
        buf[1] = (0x80 + ((c ushr 6) and 0x3f)).b
        buf[2] = (0x80 + (c and 0x3f)).b
        return 3
    }
    if (c <= 0x10FFFF) {
        if (buf.size < b + 4) return 0
        buf[b + 0] = (0xf0 + (c ushr 18)).b
        buf[b + 1] = (0x80 + ((c ushr 12) and 0x3f)).b
        buf[b + 2] = (0x80 + ((c ushr 6) and 0x3f)).b
        buf[b + 3] = (0x80 + (c and 0x3f)).b
        return 4
    }
    // Invalid code point, the max unicode is 0x10FFFF
    return 0
}

///** read one character. return input UTF-8 bytes count
// *  @return [JVM] [char: Int, bytes: Int] */
//fun textCharFromUtf8(text: ByteArray, textBegin: Int = 0, textEnd: Int = text.strlen()): Pair<Int, Int> {
//    var str = textBegin
//    fun s(i: Int = 0) = text[i + str].toUInt()
//    fun spp() = text[str++].toUInt()
//    val invalid = UNICODE_CODEPOINT_INVALID // will be invalid but not end of string
//    if ((s() and 0x80) == 0) return spp() to 1
//    if ((s() and 0xe0) == 0xc0) {
//        if (textEnd != 0 && textEnd - str < 2) return invalid to 1
//        if (s() < 0xc2) return invalid to 2
//        var c = (spp() and 0x1f) shl 6
//        if ((s() and 0xc0) != 0x80) return invalid to 2
//        c += (spp() and 0x3f)
//        return c to 2
//    }
//    if ((s() and 0xf0) == 0xe0) {
//        if (textEnd != 0 && textEnd - str < 3) return invalid to 1
//        if (s() == 0xe0 && (s(1) < 0xa0 || s(1) > 0xbf)) return invalid to 3
//        if (s() == 0xed && s(1) > 0x9f) return invalid to 3 // str[1] < 0x80 is checked below
//        var c = (spp() and 0x0f) shl 12
//        if ((s() and 0xc0) != 0x80) return invalid to 3
//        c += (spp() and 0x3f) shl 6
//        if ((s() and 0xc0) != 0x80) return invalid to 3
//        c += spp() and 0x3f
//        return c to 3
//    }
//    if ((s() and 0xf8) == 0xf0) {
//        if (textEnd != 0 && textEnd - str < 4) return invalid to 1
//        if (s() > 0xf4) return invalid to 4
//        if (s() == 0xf0 && (s(1) < 0x90 || s(1) > 0xbf)) return invalid to 4
//        if (s() == 0xf4 && s(1) > 0x8f) return invalid to 4 // str[1] < 0x80 is checked below
//        var c = (spp() and 0x07) shl 18
//        if ((s() and 0xc0) != 0x80) return invalid to 4
//        c += (spp() and 0x3f) shl 12
//        if ((s() and 0xc0) != 0x80) return invalid to 4
//        c += (spp() and 0x3f) shl 6
//        if ((s() and 0xc0) != 0x80) return invalid to 4
//        c += spp() and 0x3f
//        // utf-8 encodings of values used in surrogate pairs are invalid
//        if ((c and 0xFFFFF800.i) == 0xD800) return invalid to 4
//        // If codepoint does not fit in ImWchar, use replacement character U+FFFD instead
//        if (c >= UNICODE_CODEPOINT_MAX) c = UNICODE_CODEPOINT_INVALID
//        return c to 4
//    }
//    return 0 to 0
//}


private val lengths = intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 3, 3, 4, 0)
private val masks = intArrayOf(0x00, 0x7f, 0x1f, 0x0f, 0x07)
private val mins = intArrayOf(0x400000, 0, 0x80, 0x800, 0x10000)
private val shiftc = intArrayOf(0, 18, 12, 6, 0)
private val shifte = intArrayOf(0, 6, 4, 2, 0)

/** read one character. return input UTF-8 bytes count
 *
 *  Convert UTF-8 to 32-bit character, process single character input.
 *  Based on work of Christopher Wellons (https://github.com/skeeto/branchless-utf8)
 *  We handle UTF-8 decoding error by skipping forward.
 *
 *  @return [JVM] [char: Int, bytes: Int]
 *
 *  ~ImTextCharFromUtf8 */
fun textCharFromUtf8(text: ByteArray, begin: Int = 0, textEnd: Int = text.strlen()): Pair<Int, Int> {

    var end = textEnd

    val len = lengths[text[begin].i ushr 3]
    var wanted = len + (len == 0).i

    if (textEnd == -1)
        end = len + if (len == 0) 1 else 0 // Max length, nulls will be taken into account.

    // Copy at most 'len' bytes, stop copying at 0 or past in_text_end.
    val s = ByteArray(4)
    s[0] = if (begin + 0 < end) text[begin + 0] else 0
    s[1] = if (s[0] != 0.b && begin + 1 < end) text[begin + 1] else 0
    s[2] = if (s[1] != 0.b && begin + 2 < end) text[begin + 2] else 0
    s[3] = if (s[2] != 0.b && begin + 3 < end) text[begin + 3] else 0

    // Assume a four-byte character and load four bytes. Unused bits are shifted out.
    var outChar = (s[0].i and masks[len]) shl 18
    outChar = outChar or ((s[1].i and 0x3f) shl 12)
    outChar = outChar or ((s[2].i and 0x3f) shl 6)
    outChar = outChar or ((s[3].i and 0x3f) shl 0)
    outChar = outChar ushr shiftc[len]

    // Accumulate the various error conditions.
    var e = (outChar < mins[len]).i shl 6 // non-canonical encoding
    e = e or (((outChar ushr 11) == 0x1b).i shl 7)  // surrogate half?
    e = e or ((outChar > UNICODE_CODEPOINT_MAX).i shl 8)  // out of range?
    e = e or ((s[1].i and 0xc0) ushr 2)
    e = e or ((s[2].i and 0xc0) ushr 4)
    e = e or (s[3].i ushr 6)
    e = e xor 0x2a // top two bits of each tail byte correct?
    e = e ushr shifte[len]

    if (e != 0) {
        // No bytes are consumed when *in_text == 0 || in_text == in_text_end.
        // One byte is consumed in case of invalid first byte of in_text.
        // All available bytes (at most `len` bytes) are consumed on incomplete/invalid second to last bytes.
        // Invalid or incomplete input may consume less bytes than wanted, therefore every byte has to be inspected in s.
        wanted = min(wanted, s[0].bool.i + s[1].bool.i + s[2].bool.i + s[3].bool.i)
        outChar = UNICODE_CODEPOINT_INVALID
    }
    return outChar to wanted
}

/** return input UTF-8 bytes count */
fun textStrFromUtf8(buf: CharArray, text: ByteArray, textEnd: Int = text.size, textRemaining: Vec1i? = null): Int {
    var b = 0
    var t = 0
    while (b < buf.lastIndex && t < textEnd && text[t] != 0.b) {
        val (c, bytes) = textCharFromUtf8(text, t, textEnd)
        t += bytes
        if (c == 0)
            break
        buf[b++] = c.c
    }
    if (b < buf.size) buf[b] = NUL
    textRemaining?.put(t)
    return b
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

/** return number of UTF-8 code-points (NOT bytes count)
 *  ~ImTextCountCharsFromUtf8 */
fun textCountCharsFromUtf8(text: ByteArray, textEnd: Int = text.size): Int {
    var charCount = 0
    var t = 0
    while (t < textEnd && text[t] != 0.b) {
        val (c, bytes) = textCharFromUtf8(text, t, textEnd)
        t += bytes
        if (c == 0)
            break
        charCount++
    }
    return charCount
}

/** return number of bytes to express one char in UTF-8 */
fun textCountUtf8BytesFromChar(text: ByteArray, textEnd: Int): Int {
    val (_, bytes) = textCharFromUtf8(text, textEnd = textEnd)
    return bytes
}

/** return number of bytes to express one char in UTF-8 */
fun String.countUtf8BytesFromChar(textEnd: Int) = kotlin.math.min(length, textEnd)

/** return number of bytes to express string in UTF-8
 *  overload with textBegin = 0 */
fun textCountUtf8BytesFromStr(text: CharArray, textEnd: Int): Int = textCountUtf8BytesFromStr(text, 0, textEnd)

/** return number of bytes to express string in UTF-8 */
fun textCountUtf8BytesFromStr(text: CharArray, textBegin: Int, textEnd: Int): Int {
    var bytesCount = 0
    var t = textBegin
    while (t < textEnd && text[t] != NUL) {
        val c = text[t++].i
        if (c < 0x80)
            bytesCount++
        else
            bytesCount += textCountUtf8BytesFromChar(c)
    }
    return bytesCount
}

fun textCountUtf8BytesFromChar(c: Int) = when {
    c < 0x80 -> 1
    c < 0x800 -> 2
    c < 0x10000 -> 3
    c <= 0x10FFFF -> 4
    else -> 3
}

/** Find beginning-of-line
 *  ~ImStrbolW */
infix fun CharArray.beginOfLine(midLine: Int): Int {
    var res = midLine
    while (res > 0 && this[res - 1] != '\n') res--
    return res
}

val Vec2.lengthSqr: Float
    get() = x * x + y * y

// -----------------------------------------------------------------------------------------------------------------
// - ImMin/ImMax/ImClamp/ImLerp/ImSwap are used by widgets which support variety of types: signed/unsigned int/long long float/double
// (Exceptionally using templates here but we could also redefine them for those types)
// -----------------------------------------------------------------------------------------------------------------

fun swap(a: KMutableProperty0<Float>, b: KMutableProperty0<Float>) {
    val tmp = a()
    a.set(b())
    b.set(tmp)
}

/** Byte, Short versions */
fun addClampOverflow(a: Int, b: Int, mn: Int, mx: Int): Int = when {
    b < 0 && (a < mn - b) -> mn
    b > 0 && (a > mx - b) -> mx
    else -> a + b
}

fun subClampOverflow(a: Int, b: Int, mn: Int, mx: Int): Int = when {
    b > 0 && (a < mn + b) -> mn
    b < 0 && (a > mx + b) -> mx
    else -> a - b
}

/** Int versions */
fun addClampOverflow(a: Long, b: Long, mn: Long, mx: Long): Long = when {
    b < 0 && (a < mn - b) -> mn
    b > 0 && (a > mx - b) -> mx
    else -> a + b
}

fun subClampOverflow(a: Long, b: Long, mn: Long, mx: Long): Long = when {
    b > 0 && (a < mn + b) -> mn
    b < 0 && (a > mx + b) -> mx
    else -> a - b
}

/** Ulong versions */
fun addClampOverflow(a: BigInteger, b: BigInteger, mn: BigInteger, mx: BigInteger): BigInteger = when {
    b < 0.toBigInt() && (a < mn - b) -> mn
    b > 0.toBigInt() && (a > mx - b) -> mx
    else -> a + b
}

fun subClampOverflow(a: BigInteger, b: BigInteger, mn: BigInteger, mx: BigInteger): BigInteger = when {
    b > 0.toBigInt() && (a < mn + b) -> mn
    b < 0.toBigInt() && (a > mx + b) -> mx
    else -> a - b
}

// -----------------------------------------------------------------------------------------------------------------
// Misc maths helpers
// -----------------------------------------------------------------------------------------------------------------

fun floor(a: Float): Float = a.i.f
fun floor(a: Vec2): Vec2 = Vec2(floor(a.x), floor(a.y))
fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
fun lerp(a: Double, b: Double, t: Float): Double = a + (b - a) * t
fun lerp(a: Int, b: Int, t: Float): Int = (a + (b - a) * t).i
fun lerp(a: Long, b: Long, t: Float): Long = (a + (b - a) * t).L
fun modPositive(a: Int, b: Int): Int = (a + b) % b

// TODO -> glm
fun Vec2.lerp(b: Vec2, t: Float): Vec2 = Vec2(x + (b.x - x) * t, y + (b.y - y) * t)
fun Vec2.lerp(b: Vec2, t: Vec2): Vec2 = Vec2(x + (b.x - x) * t.x, y + (b.y - y) * t.y)
fun Vec2.lerp(b: Vec2i, t: Vec2): Vec2 = Vec2(x + (b.x - x) * t.x, y + (b.y - y) * t.y)
fun Vec2i.lerp(b: Vec2i, t: Vec2): Vec2 = Vec2(x + (b.x - x) * t.x, y + (b.y - y) * t.y)
fun Vec4.lerp(b: Vec4, t: Float): Vec4 = Vec4 { lerp(this[it], b[it], t) }
fun saturate(f: Float): Float = if (f < 0f) 0f else if (f > 1f) 1f else f
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

val Float.isAboveGuaranteedIntegerPrecision get() = f <= -16777216 || f >= 16777216

//-----------------------------------------------------------------------------
// [SECTION] MISC HELPERS/UTILITIES (Geometry functions)
//-----------------------------------------------------------------------------

fun bezierCubicCalc(p1: Vec2, p2: Vec2, p3: Vec2, p4: Vec2, t: Float): Vec2 {
    val u = 1f - t
    val w1 = u * u * u
    val w2 = 3 * u * u * t
    val w3 = 3 * u * t * t
    val w4 = t * t * t
    return Vec2(w1 * p1.x + w2 * p2.x + w3 * p3.x + w4 * p4.x, w1 * p1.y + w2 * p2.y + w3 * p3.y + w4 * p4.y)
}

/** For curves with explicit number of segments */
fun bezierCubicClosestPoint(p1: Vec2, p2: Vec2, p3: Vec2, p4: Vec2, p: Vec2, numSegments: Int): Vec2 {
    assert(numSegments > 0) { "Use ImBezierCubicClosestPointCasteljau()" }
    val pLast = Vec2(p1)
    val pClosest = Vec2()
    var pClosestDist2 = Float.MAX_VALUE
    val tStep = 1f / numSegments
    for (iStep in 1..numSegments) {
        val pCurrent = bezierCubicCalc(p1, p2, p3, p4, tStep * iStep)
        val pLine = lineClosestPoint(pLast, pCurrent, p)
        val dist2 = (p - pLine).lengthSqr
        if (dist2 < pClosestDist2) {
            pClosest put pLine
            pClosestDist2 = dist2
        }
        pLast put pCurrent
    }
    return pClosest
}

/** For auto-tessellated curves you can use tess_tol = style.CurveTessellationTol
 *
 *  tess_tol is generally the same value you would find in ImGui::GetStyle().CurveTessellationTol
 *  Because those ImXXX functions are lower-level than ImGui:: we cannot access this value automatically. */
fun bezierCubicClosestPointCasteljau(p1: Vec2, p2: Vec2, p3: Vec2, p4: Vec2, p: Vec2, tessTol: Float): Vec2 {
    assert(tessTol > 0f)
    val pLast = p1 // [JVM] careful, same instance!
    val pClosest = Vec2()
    val pClosestDist2 = Float.MAX_VALUE
    // [JVM] we dont need the return value
    bezierCubicClosestPointCasteljauStep(p, pClosest, pLast, pClosestDist2, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, p4.x, p4.y, tessTol, 0)
    return pClosest
}

fun bezierQuadraticCalc(p1: Vec2, p2: Vec2, p3: Vec2, t: Float): Vec2 {
    val u = 1f - t
    val w1 = u * u
    val w2 = 2 * u * t
    val w3 = t * t
    return Vec2(w1 * p1.x + w2 * p2.x + w3 * p3.x, w1 * p1.y + w2 * p2.y + w3 * p3.y)
}

/** Closely mimics PathBezierToCasteljau() in imgui_draw.cpp
 *
 *  [JVM] p, pClosest, pLast, pClosestDist2 are supposed to modify the given instance
 *  [JVM] @return pClosestDist2
 */
fun bezierCubicClosestPointCasteljauStep(p: Vec2, pClosest: Vec2, pLast: Vec2, pClosestDist2: Float,
                                         x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float,
                                         x4: Float, y4: Float, tessTol: Float, level: Int): Float {
    var res = pClosestDist2
    val dx = x4 - x1
    val dy = y4 - y1
    var d2 = ((x2 - x4) * dy - (y2 - y4) * dx)
    var d3 = ((x3 - x4) * dy - (y3 - y4) * dx)
    d2 = if (d2 >= 0) d2 else -d2
    d3 = if (d3 >= 0) d3 else -d3
    if ((d2 + d3) * (d2 + d3) < tessTol * (dx * dx + dy * dy)) {
        val pCurrent = Vec2(x4, y4)
        val pLine = lineClosestPoint(pLast, pCurrent, p)
        val dist2 = (p - pLine).lengthSqr
        if (dist2 < pClosestDist2) {
            pClosest put pLine
            res = dist2 // pClosestDist2 = dist2
        }
        pLast put pCurrent
    } else if (level < 10) {
        val x12 = (x1 + x2) * 0.5f
        val y12 = (y1 + y2) * 0.5f
        val x23 = (x2 + x3) * 0.5f
        val y23 = (y2 + y3) * 0.5f
        val x34 = (x3 + x4) * 0.5f
        val y34 = (y3 + y4) * 0.5f
        val x123 = (x12 + x23) * 0.5f
        val y123 = (y12 + y23) * 0.5f
        val x234 = (x23 + x34) * 0.5f
        val y234 = (y23 + y34) * 0.5f
        val x1234 = (x123 + x234) * 0.5f
        val y1234 = (y123 + y234) * 0.5f
        res = bezierCubicClosestPointCasteljauStep(p, pClosest, pLast, res, x1, y1, x12, y12, x123, y123, x1234, y1234, tessTol, level + 1)
        res = bezierCubicClosestPointCasteljauStep(p, pClosest, pLast, res, x1234, y1234, x234, y234, x34, y34, x4, y4, tessTol, level + 1)
    }
    return res
}

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
    return b1 == b2 && b2 == b3
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

fun triangleArea(a: Vec2, b: Vec2, c: Vec2): Float =
    abs((a.x * (b.y - c.y)) + (b.x * (c.y - a.y)) + (c.x * (a.y - b.y))) * 0.5f

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


// Helper: ImBitArray class (wrapper over ImBitArray functions)
// Store 1-bit per value.
//template<int BITCOUNT>
class BitArray(val bitCount: Int) {

    init {
        clearAllBits()
    }

    val storage = IntArray((bitCount + 31) ushr 5)

    fun clearAllBits() = storage.fill(0)
    fun setAllBits() = storage.fill(255)

    fun mask(n: Int): Int = 1 shl (n and 31)

    infix fun testBit(n: Int): Boolean {
        assert(n < bitCount)
        return storage[n ushr 5] has mask(n)
    }

    infix fun setBit(n: Int) {
        assert(n < bitCount)
        storage[n ushr 5] = storage[n ushr 5] or mask(n)
    }

    infix fun clearBit(n: Int) {
        assert(n < bitCount)
        storage[n ushr 5] = storage[n ushr 5] wo mask(n)
    }

    fun setBitRange(n_: Int, n2_: Int) { // Works on range [n..n2)
        var n = n_
        val n2 = n2_ - 1
        while (n <= n2) {
            val aMod = n and 31
            val bMod = (if (n2 > (n or 31)) 31 else n2 and 31) + 1
            val mask = ((1L shl bMod) - 1).i wo ((1L shl aMod) - 1).i
            storage[n ushr 5] = storage[n ushr 5] or mask
            n = (n + 32) wo 31
        }
    }
}