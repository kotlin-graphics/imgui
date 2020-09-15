//package java.lang
//
//import glm_.d
//import java.lang.ref.SoftReference
//import java.nio.charset.Charset
//import kotlin.Float
//
///** The cached coders for each thread */
//val decoder = ThreadLocal<SoftReference<StringDecoder>>()
////val encoder = ThreadLocal<SoftReference<StringEncoder>>()
//
//val <T> ThreadLocal<SoftReference<T>>.deref: T?
//    get() = get()?.get()
//
//infix fun <T> ThreadLocal<SoftReference<T>>.set(ob: T) = set(SoftReference(ob))
//
//// Trim the given byte array to the given length
//fun ByteArray.safeTrim(len: Int, isTrusted: Boolean): ByteArray = when {
//    len == size && (isTrusted || System.getSecurityManager() == null) -> this
//    else -> copyOf(len)
//}
//
//// We need to perform double, not float, arithmetic; otherwise
//// we lose low order bits when len is larger than 2**24.
//fun scale(len: Int, expansionFactor: Float) = (len * expansionFactor.d).i
//
//val kotlin.String.charset: Charset?
//    get() = when {
//        Charset.isSupported(this) -> Charset.forName(this)
//        else -> null
//    }
//
//
////@HotSpotIntrinsicCandidate
//fun ByteArray.hasNegatives(off: Int, len: Int): kotlin.Boolean {
//    for (i in off until off + len)
//        if (this[i] < 0)
//            return true
//    return false
//}
//
////@ExperimentalStdlibApi
////fun decode(charsetName: String, ba: ByteArray, off: Int, len: Int): Result{
////    var sd = decoder.deref
////    val csn = charsetName ?: "ISO-8859-1"
////    if (sd == null || !(csn == sd.requestedCharsetName || csn == sd.charsetName)) {
////        sd = null
////        when(val cs = csn.charset) {
////            Charsets.UTF_8 -> return decodeUTF8(ba, off, len, true)
////            Charsets.ISO_8859_1 -> return decodeLatin1(ba, off, len)
////            Charsets.US_ASCII -> return decodeASCII(ba, off, len)
////            sd = StringDecoder(cs, csn)
////        }
////        if (sd == null)
////            throw UnsupportedEncodingException(csn)
////        decoder.set(sd)
////    }
////    return sd.decode(ba, off, len)
////}