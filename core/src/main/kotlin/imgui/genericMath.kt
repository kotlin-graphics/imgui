package imgui

import glm_.*
import imgui.internal.addClampOverflow
import imgui.internal.charIsBlankA
import imgui.internal.parseFormatSanitizeForScanning
import imgui.internal.subClampOverflow
import uno.kotlin.NUL
import unsigned.*
import unsigned.parseUnsignedLong
import kotlin.reflect.KMutableProperty0
import kotlin.math.ln as realLn
import kotlin.math.pow as realPow

infix fun <N : Comparable<N>> N.min(other: N): N = if (this < other) this else other
infix fun <N : Comparable<N>> N.max(other: N): N = if (this > other) this else other

@JvmName("clampReceiver")
fun <N : Comparable<N>> N.clamp(min: N?, max: N?): N = when {
    min != null && this < min -> min
    max != null && this > max -> max
    else -> this
}

fun <N : Comparable<N>> clamp(value: N, min: N?, max: N?): N = value.clamp(min, max)

sealed interface NumberOps<N> where N : Number, N : Comparable<N> {
    val min: N
    val max: N
    val zero: N
    val one: N
    val N.isNegative: Boolean
        get() = this < zero
    val N.sign: Int
        get() = when {
            isNegative -> -1
            this == zero -> 0
            else -> 1
        }
    val dataType: DataType
    val Number.coerced: N

    /** User can input math operators (e.g. +100) to edit a numerical values.
     *  NB: This is _not_ a full expression evaluator. We should probably add one and replace this dumb mess..
     *
     *  ~DataTypeApplyFromText
     *  @return true if modified */
    fun KMutableProperty0<N>.applyFromText(buf: ByteArray, format: String): Boolean {

        var ptr = 0
        while (charIsBlankA(buf[ptr].i))
            ptr++
        if (buf[ptr] == 0.b)
            return false

        // Copy the value in an opaque buffer so we can compare at the end of the function if it changed at all.
        val backupData = get()

        // Sanitize format
        // For float/double we have to ignore format with precision (e.g. "%.2f") because sscanf doesn't take them in, so force them into %f and %lf
        // - For float/double we have to ignore format with precision (e.g. "%.2f") because sscanf doesn't take them in, so force them into %f and %lf
        // - In theory could treat empty format as using default, but this would only cover rare/bizarre case of using InputScalar() + integer + format string without %.
        val v = parse(buf, format)
        return v?.let {
            this.set(v)
            get() != backupData
        } == true
    }

    fun parse(buf: ByteArray, format: String, radix: Int): N
    val String.parsed: N

    fun N.format(format: String): String {
        // [JVM] we have to filter `.`, since `%.03d` causes `IllegalFormatPrecisionException`, but `%03d` doesn't
        return format.replace(".", "").format(when (this) {
                                                  // we need to intervene since java printf cant handle %u
                                                  is Ubyte -> i
                                                  is Ushort -> i
                                                  is Uint -> L
                                                  is Ulong -> toBigInt()
                                                  else -> this
                                              })
    }

    operator fun N.plus(other: N): N
    operator fun N.minus(other: N): N
    operator fun N.times(other: N): N = with(fpOps) { nTimesN(this@times, other) }
    operator fun N.div(other: N): N = with(fpOps) { nDivN(this@div, other) }
}

private fun <N, FP> NumberFpOps<N, FP>.nTimesN(n: N, other: N): N where N : Number, N : Comparable<N>, FP : Number, FP : Comparable<FP> = (n.fp * other.fp).n
private fun <N, FP> NumberFpOps<N, FP>.nDivN(n: N, other: N): N where N : Number, N : Comparable<N>, FP : Number, FP : Comparable<FP> = (n.fp / other.fp).n

fun <N> NumberOps<N>.parse(buf: ByteArray, format: String): N? where N : Number, N : Comparable<N> {
    // ImCharIsBlankA
    buf.cStr
        .replace(Regex("\\s+"), "")
        .replace("\t", "")
        .removeSuffix("\u0000")
        .toByteArray(buf)

    if (buf.isEmpty()) return null

    val radix = if (format.last().lowercaseChar() == 'x') 16 else 10

    // Sanitize format
    return parse(buf, parseFormatSanitizeForScanning(format), radix)
}

fun NumberOps<*>.defaultInputCharsFilter(format: String): InputTextFlag.Single {
    if (dataType == DataType.Float || dataType == DataType.Double) return InputTextFlag.CharsScientific
    return when (if (format.isNotEmpty()) format.last() else NUL) {
        'x', 'X' -> InputTextFlag.CharsHexadecimal
        else -> InputTextFlag.CharsDecimal
    }
}

val NumberOps<*>.defaultFormat: String
    get() = when (dataType) {
        DataType.Float, DataType.Double -> "%.3f"
        else -> "%d"
    }
val NumberOps<*>.isSigned: Boolean
    get() = !dataType.isUnsigned

val NumberOps<*>.isFloatingPoint: Boolean
    get() = dataType == DataType.Float || dataType == DataType.Double

val <N> NumberOps<N>.isSmallerThanInt: Boolean where N : Number, N : Comparable<N> get() = dataType < DataType.Int

inline fun <reified N> numberOps(): NumberOps<N> where N : Number, N : Comparable<N> = numberFpOps<N, Nothing>()
inline fun <reified N, FP> numberFpOps(): NumberFpOps<N, FP> where N : Number, N : Comparable<N>, FP : Number, FP : Comparable<FP> = dataTypeOf<N>().fpOps()
fun <N> DataType.ops(): NumberOps<N> where N : Number, N : Comparable<N> = fpOps<N, Nothing>()
fun <N, FP> DataType.fpOps(): NumberFpOps<N, FP> where N : Number, N : Comparable<N>, FP : Number, FP : Comparable<FP> = when (this) {
    DataType.Byte -> ByteOps
    DataType.Ubyte -> UbyteOps
    DataType.Short -> ShortOps
    DataType.Ushort -> UshortOps
    DataType.Int -> IntOps
    DataType.Uint -> UintOps
    DataType.Long -> LongOps
    DataType.Ulong -> UlongOps
    DataType.Float -> FloatOps
    DataType.Double -> DoubleOps
    else -> error("invalid")
} as NumberFpOps<N, FP>

val <N> NumberOps<N>.fpOps: NumberFpOps<N, Nothing> where N : Number, N : Comparable<N>
    get() = if (this is NumberFpOps<*, *>) this as NumberFpOps<N, Nothing> else dataType.fpOps()

sealed interface FloatingPointOps<FP> where  FP : Number, FP : Comparable<FP> {
    val Float.fp: FP get() = coercedFp

    val Number.coercedFp: FP
    operator fun FP.unaryMinus(): FP

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("fpPlus")
    operator fun FP.plus(other: FP): FP

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("fpMinus")
    operator fun FP.minus(other: FP): FP

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("fpTimes")
    operator fun FP.times(other: FP): FP

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("fpDiv")
    operator fun FP.div(other: FP): FP
    infix fun FP.pow(other: FP): FP
    fun ln(fp: FP): FP
    fun abs(fp: FP): FP = when {
        fp < 0f.fp -> -fp
        else -> fp
    }
}

sealed interface NumberFpOps<N, FP> : NumberOps<N>, FloatingPointOps<FP> where N : Number, N : Comparable<N>, FP : Number, FP : Comparable<FP> {
    // These are defined to signal that no coercion should be happening between the types
    // In other words, coerced* properties should usually be avoided
    val N.fp: FP get() = coercedFp
    val FP.n: N get() = coerced
}

fun <N, FP> NumberFpOps<N, FP>.lerp(a: N, b: N, t: FP): FP where N : Number, N : Comparable<N>, FP : Number, FP : Comparable<FP> = a.fp + (b - a).fp * t
fun <FP> FloatingPointOps<FP>.lerp(a: FP, b: FP, t: FP): FP where FP : Number, FP : Comparable<FP> = a + (b - a) * t

object ByteOps : NumberFpOps<Byte, Float>, FloatingPointOps<Float> by FloatOps {
    override val min = Byte.MIN_VALUE
    override val max = Byte.MAX_VALUE
    override val zero: Byte = 0
    override val one: Byte = 1
    override val Number.coerced get() = b
    override val dataType: DataType = DataType.Byte
    override fun parse(buf: ByteArray, format: String, radix: Int): Byte = format.format(buf.cStr.parseInt(radix)).toByte(radix)
    override val String.parsed: Byte get() = b
    override fun Byte.plus(other: Byte): Byte = addClampOverflow(i, other.i, min.i, max.i).b
    override fun Byte.minus(other: Byte): Byte = subClampOverflow(i, other.i, min.i, max.i).b
}

object UbyteOps : NumberFpOps<Ubyte, Float>, FloatingPointOps<Float> by FloatOps {
    override val min = Ubyte.MIN
    override val max = Ubyte.MAX
    override val zero = 0.ub
    override val one = 1.ub
    override val Ubyte.isNegative: Boolean get() = false
    override val Number.coerced get() = ub
    override val dataType: DataType = DataType.Ubyte
    override fun parse(buf: ByteArray, format: String, radix: Int): Ubyte = format.format(buf.cStr.parseInt(radix)).toInt(radix).ub
    override val String.parsed: Ubyte get() = ub
    override fun Ubyte.plus(other: Ubyte): Ubyte = addClampOverflow(i, other.i, min.i, max.i).ub
    override fun Ubyte.minus(other: Ubyte): Ubyte = subClampOverflow(i, other.i, min.i, max.i).ub
}

object ShortOps : NumberFpOps<Short, Float>, FloatingPointOps<Float> by FloatOps {
    override val min = Short.MIN_VALUE
    override val max = Short.MAX_VALUE
    override val zero: Short = 0
    override val one: Short = 1
    override val Number.coerced get() = s
    override val dataType: DataType = DataType.Short
    override fun parse(buf: ByteArray, format: String, radix: Int): Short = format.format(buf.cStr.parseInt(radix)).toShort(radix)
    override val String.parsed: Short get() = s
    override fun Short.plus(other: Short): Short = addClampOverflow(i, other.i, min.i, max.i).s
    override fun Short.minus(other: Short): Short = subClampOverflow(i, other.i, min.i, max.i).s
}

object UshortOps : NumberFpOps<Ushort, Float>, FloatingPointOps<Float> by FloatOps {
    override val min = Ushort.MIN
    override val max = Ushort.MAX
    override val zero = 0.us
    override val one = 1.us
    override val Ushort.isNegative: Boolean get() = false
    override val Number.coerced get() = us
    override val dataType: DataType = DataType.Ushort
    override fun parse(buf: ByteArray, format: String, radix: Int): Ushort = format.format(buf.cStr.parseInt(radix)).toInt(radix).us
    override val String.parsed: Ushort get() = us
    override fun Ushort.plus(other: Ushort): Ushort = addClampOverflow(i, other.i, min.i, max.i).us
    override fun Ushort.minus(other: Ushort): Ushort = subClampOverflow(i, other.i, min.i, max.i).us
}

object IntOps : NumberFpOps<Int, Float>, FloatingPointOps<Float> by FloatOps {
    override val min = Int.MIN_VALUE
    override val max = Int.MAX_VALUE
    override val zero = 0
    override val one = 1
    override val Number.coerced get() = i
    override val dataType: DataType = DataType.Int
    override fun parse(buf: ByteArray, format: String, radix: Int): Int = format.format(buf.cStr.parseInt(radix)).toInt(radix)
    override val String.parsed: Int get() = i
    override fun Int.plus(other: Int): Int = addClampOverflow(this, other, min, max)
    override fun Int.minus(other: Int): Int = subClampOverflow(this, other, min, max)
}

object UintOps : NumberFpOps<Uint, Float>, FloatingPointOps<Float> by FloatOps {
    override val min = Uint.MIN
    override val max = Uint.MAX
    override val zero = 0.ui
    override val one = 1.ui
    override val Uint.isNegative: Boolean get() = false
    override val Number.coerced get() = ui
    override val dataType: DataType = DataType.Uint
    override fun parse(buf: ByteArray, format: String, radix: Int): Uint = format.format(buf.cStr.parseLong(radix)).toLong(radix).ui
    override val String.parsed: Uint get() = ui
    override fun Uint.plus(other: Uint): Uint = addClampOverflow(L, other.L, min.L, max.L).ui
    override fun Uint.minus(other: Uint): Uint = subClampOverflow(L, other.L, min.L, max.L).ui
}

object LongOps : NumberFpOps<Long, Double>, FloatingPointOps<Double> by DoubleOps {
    override val min = Long.MIN_VALUE
    override val max = Long.MAX_VALUE
    override val zero = 0L
    override val one = 1L
    override val Number.coerced get() = L
    override val dataType: DataType = DataType.Long
    override fun parse(buf: ByteArray, format: String, radix: Int): Long = format.format(buf.cStr.parseUnsignedLong(radix)).toLong(radix)
    override val String.parsed: Long get() = L
    override fun Long.plus(other: Long): Long = addClampOverflow(this, other, min, max)
    override fun Long.minus(other: Long): Long = subClampOverflow(this, other, min, max)
}

object UlongOps : NumberFpOps<Ulong, Double>, FloatingPointOps<Double> by DoubleOps {
    override val min = Ulong.MIN
    override val max = Ulong.MAX
    override val zero = 0.ul
    override val one = 1.ul
    override val Ulong.isNegative: Boolean get() = false
    override val Number.coerced get() = ul
    override val dataType: DataType = DataType.Ulong
    override fun parse(buf: ByteArray, format: String, radix: Int): Ulong = format.format(buf.cStr.parseUnsignedLong(radix)).toBigInteger(radix).ul
    override val String.parsed: Ulong get() = ul
    override fun Ulong.plus(other: Ulong): Ulong = addClampOverflow(toBigInt(), other.toBigInt(), min.toBigInt(), max.toBigInt()).ul
    override fun Ulong.minus(other: Ulong): Ulong = subClampOverflow(toBigInt(), other.toBigInt(), min.toBigInt(), max.toBigInt()).ul
}

object FloatOps : NumberFpOps<Float, Float> {
    override val min = -Float.MAX_VALUE
    override val max = Float.MAX_VALUE
    override val zero = 0f
    override val one = 1f
    override val Float.fp: Float get() = this
    override val Number.coerced get() = f
    override val Number.coercedFp: Float get() = f
    override val dataType: DataType = DataType.Float
    override fun Float.format(format: String): String = format.format(this)
    override fun parse(buf: ByteArray, format: String, radix: Int): Float = "%f".format(buf.cStr.parseFloat).f
    override val String.parsed: Float get() = f
    override fun Float.unaryMinus(): Float = -this

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("fpPlus")
    override fun Float.plus(other: Float): Float = this + other

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("fpMinus")
    override fun Float.minus(other: Float): Float = this - other

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("fpTimes")
    override fun Float.times(other: Float): Float = this * other

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("fpDiv")
    override fun Float.div(other: Float): Float = this / other
    override fun Float.pow(other: Float): Float = this.realPow(other)
    override fun ln(fp: Float): Float = realLn(fp)
}

object DoubleOps : NumberFpOps<Double, Double> {
    override val min = -Double.MAX_VALUE
    override val max = Double.MAX_VALUE
    override val zero = 0.0
    override val one = 1.0
    override val Number.coerced get() = d
    override val Number.coercedFp: Double get() = d
    override val dataType: DataType = DataType.Double
    override fun Double.format(format: String): String = format.format(this)
    override fun parse(buf: ByteArray, format: String, radix: Int): Double = "%f".format(buf.cStr.parseDouble).d
    override val String.parsed: Double get() = d
    override fun Double.unaryMinus(): Double = -this

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("fpPlus")
    override fun Double.plus(other: Double): Double = this + other

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("fpMinus")
    override fun Double.minus(other: Double): Double = this - other

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("fpTimes")
    override fun Double.times(other: Double): Double = this * other

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("fpDiv")
    override fun Double.div(other: Double): Double = this / other
    override fun Double.pow(other: Double): Double = this.realPow(other)
    override fun ln(fp: Double): Double = realLn(fp)
}