package plot.internalApi

import glm_.func.common.abs
import glm_.func.common.isNan
import glm_.isInfinite
import glm_.isNaN
import imgui.*
import plot.IMPLOT_MIX64
import kotlin.math.ulp
import kotlin.reflect.KMutableProperty0

//-----------------------------------------------------------------------------
// [SECTION] Generic Helpers
//-----------------------------------------------------------------------------

// Computes the common (base-10) logarithm
//static inline float  ImLog10(float x)  { return log10f(x); }
//static inline double ImLog10(double x) { return log10(x);  }
//static inline float  ImSinh(float x)   { return sinhf(x);  }
//static inline double ImSinh(double x)  { return sinh(x);   }
//static inline float  ImAsinh(float x)  { return asinhf(x); }
//static inline double ImAsinh(double x) { return asinh(x);  }
//// Returns true if a flag is set
//template <typename TSet, typename TFlag>
//static inline bool ImHasFlag(TSet set, TFlag flag) { return (set & flag) == flag; }
// Flips a flag in a flagset
//template <typename TSet, typename TFlag>
infix fun <F : FlagBase<F>> KMutableProperty0<Flag<F>>.flip(flag: F) {
    var set by this
    if (set has flag)
        set -= flag
    else set /= flag
}

// Linearly remaps x from [x0 x1] to [y0 y1].
//template <typename T>
fun remap(x: Float, x0: Float, x1: Float, y0: Float, y1: Float) = y0 + (x - x0) * (y1 - y0) / (x1 - x0)
//// Linear rempas x from [x0 x1] to [0 1]
//template <typename T>
//static inline T ImRemap01(T x, T x0, T x1) { return (x - x0) / (x1 - x0); }
//// Returns always positive modulo (assumes r != 0)
//static inline int ImPosMod(int l, int r) { return (l % r + r) % r; }
//// Returns true if val is NAN
//static inline bool ImNan(double val) { return isnan(val); }
// Returns true if val is NAN or INFINITY
val Double.nanOrInf get() = isNan || isInfinite

// Turns NANs to 0s
fun Double.constrainNan() = if (isNaN) 0.0 else this

// Turns infinity to floating point maximums
fun Double.constrainInf() = if (this >= Double.MAX_VALUE) Double.MAX_VALUE else if (this <= -Double.MAX_VALUE) -Double.MAX_VALUE else this

//// Turns numbers less than or equal to 0 to 0.001 (sort of arbitrary, is there a better way?)
//static inline double ImConstrainLog(double val) { return val <= 0 ? 0.001f : val; }
//// Turns numbers less than 0 to zero
//static inline double ImConstrainTime(double val) { return val < IMPLOT_MIN_TIME ? IMPLOT_MIN_TIME : (val > IMPLOT_MAX_TIME ? IMPLOT_MAX_TIME : val); }
// True if two numbers are approximately equal using units in the last place.
fun almostEqual(v1: Double, v2: Double, ulp: Int = 2) = (v1.ulp - v2.ulp).abs <= ulp

//// Finds min value in an unsorted array
//template <typename T>
//static inline T ImMinArray(const T* values, int count) { T m = values[0]; for (int i = 1; i < count; ++i) { if (values[i] < m) { m = values[i]; } } return m; }
//// Finds the max value in an unsorted array
//template <typename T>
//static inline T ImMaxArray(const T* values, int count) { T m = values[0]; for (int i = 1; i < count; ++i) { if (values[i] > m) { m = values[i]; } } return m; }
//// Finds the min and max value in an unsorted array
//template <typename T>
//static inline void ImMinMaxArray(const T* values, int count, T* min_out, T* max_out) {
//    T Min = values[0]; T Max = values[0];
//    for (int i = 1; i < count; ++i) {
//        if (values[i] < Min) { Min = values[i]; }
//        if (values[i] > Max) { Max = values[i]; }
//    }
//    *min_out = Min; *max_out = Max;
//}
//// Finds the sim of an array
//template <typename T>
//static inline T ImSum(const T* values, int count) {
//    T sum  = 0;
//    for (int i = 0; i < count; ++i)
//    sum += values[i];
//    return sum;
//}
//// Finds the mean of an array
//template <typename T>
//static inline double ImMean(const T* values, int count) {
//    double den = 1.0 / count;
//    double mu  = 0;
//    for (int i = 0; i < count; ++i)
//    mu += (double)values[i] * den;
//    return mu;
//}
//// Finds the sample standard deviation of an array
//template <typename T>
//static inline double ImStdDev(const T* values, int count) {
//    double den = 1.0 / (count - 1.0);
//    double mu  = ImMean(values, count);
//    double x   = 0;
//    for (int i = 0; i < count; ++i)
//    x += ((double)values[i] - mu) * ((double)values[i] - mu) * den;
//    return sqrt(x);
//}
// Mix color a and b by factor s in [0 256]
fun mixU32(a: UInt, b: UInt, s_: Int): UInt = when {
    IMPLOT_MIX64 -> {
        TODO()
//        const ImU32 af = 256 - s;
//        const ImU32 bf = s;
//        const ImU64 al = (a & 0x00ff00ff) | (((ImU64)(a & 0xff00ff00)) << 24);
//        const ImU64 bl = (b & 0x00ff00ff) | (((ImU64)(b & 0xff00ff00)) << 24);
//        const ImU64 mix = (al * af + bl * bf);
//        return ((mix > > 32) & 0xff00ff00) | ((mix & 0xff00ff00) >> 8);
    }
    else -> {
        val s = s_.toUInt()
        val af = 256u - s
        val bf = s
        val al = a and 0x00FF00FFu
        val ah = (a and 0xFF00FF00u) shr 8
        val bl = b and 0x00FF00FFu
        val bh = (b and 0xFF00FF00u) shr 8
        val ml = al * af + bl * bf
        val mh = ah * af + bh * bf
        (mh and 0xFF00FF00u) or ((ml and 0xFF00FF00u) shr 8)
    }
}

//// Lerp across an array of 32-bit collors given t in [0.0 1.0]
//static inline ImU32 ImLerpU32(const ImU32* colors, int size, float t) {
//    int i1 = (int)((size - 1 ) * t);
//    int i2 = i1 + 1;
//    if (i2 == size || size == 1)
//        return colors[i1];
//    float den = 1.0f / (size - 1);
//    float t1 = i1 * den;
//    float t2 = i2 * den;
//    float tr = ImRemap01(t, t1, t2);
//    return ImMixU32(colors[i1], colors[i2], (ImU32)(tr*256));
//}
//
// Set alpha channel of 32-bit color from float in range [0.0 1.0]
fun alphaU32(col: UInt, alpha: Float): UInt = col and (((1f - alpha) * 255).toUInt() shl COL32_A_SHIFT).inv()

//// Returns true of two ranges overlap
//template <typename T>
//static inline bool ImOverlaps(T min_a, T max_a, T min_b, T max_b) {
//    return min_a <= max_b && min_b <= max_a;
//}