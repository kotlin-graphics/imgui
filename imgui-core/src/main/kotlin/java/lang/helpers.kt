//package java.lang
//
//import glm_.b
//import sun.nio.cs.ArrayDecoder
//import sun.nio.cs.HistoricallyNamedCharset
//import java.lang.String.*
//import java.nio.ByteBuffer
//import java.nio.CharBuffer
//import java.nio.charset.Charset
//import java.nio.charset.CodingErrorAction
//import java.util.*
//import kotlin.Byte
//import kotlin.String
//
//class Result {
//    var value: ByteArray? = null
//    var coder = 0.b
//
//    fun with(): Result {
//        coder = if(COMPACT_STRINGS) LATIN1 else UTF16
//        value = ByteArray(0)
//        return this
//    }
//
//    fun with(value: CharArray, off: Int, len: Int): Result {
//        if (COMPACT_STRINGS) {
//            val bs = StringUTF16.compress(value, off, len)
//            if (bs != null) {
//                this.value = bs
//                coder = LATIN1
//                return this
//            }
//        }
//        coder = UTF16
//        this.value = StringUTF16.toBytes(value, off, len)
//        return this
//    }
//
//    fun with(value: ByteArray, coder: Byte): Result {
//        this.coder = coder
//        this.value = value
//        return this
//    }
//}
//
//class StringDecoder(val cs: Charset, val requestedCharsetName: String) {
//    val cd = cs.newDecoder()
//            .onMalformedInput(CodingErrorAction.REPLACE)
//            .onUnmappableCharacter(CodingErrorAction.REPLACE)
//    val isASCIICompatible = cd is ArrayDecoder && cd.isASCIICompatible()
//
//    val result = Result()
//
//    val charsetName
//        get() = (cs as? HistoricallyNamedCharset).historicalName() ?: cs.name()
//
//    @ExperimentalStdlibApi
//    fun decode(ba: ByteArray, off: Int, len: Int): Result {
//        if (len == 0) return result.with()
//        // fastpath for ascii compatible
//        if (isASCIICompatible && !ba.hasNegatives(off, len)) {
//            return when {
//                COMPACT_STRINGS -> result.with(ba.copyOfRange(off, off + len), LATIN1)
//                else -> result.with(StringLatin1.inflate(ba, off, len), UTF16)
//            }
//        }
//        val en = scale(len, cd.maxCharsPerByte());
//        val ca = CharArray(en)
//        if (cd is ArrayDecoder) {
//            val clen = cd.decode(ba, off, len, ca)
//            return result.with(ca, 0, clen)
//        }
//        cd.reset()
//        val bb = ByteBuffer.wrap(ba, off, len)
//        val cb = CharBuffer.wrap(ca);
//        try {
//            var cr = cd.decode(bb, cb, true)
//            if (!cr.isUnderflow)
//                cr.throwException()
//            cr = cd.flush(cb);
//            if (!cr.isUnderflow)
//                cr.throwException()
//        } catch (x: CharacterCodingException) {
//            // Substitution is always enabled,
//            // so this shouldn't happen
//            throw Error(x)
//        }
//        return result.with(ca, 0, cb.position())
//    }
//}