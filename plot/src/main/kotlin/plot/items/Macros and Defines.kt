package plot.items

import kotlin.math.sqrt

//-----------------------------------------------------------------------------
// [SECTION] Macros and Defines
//-----------------------------------------------------------------------------

const val SQRT_1_2 = 0.70710678118f
const val SQRT_3_2 = 0.86602540378f
//
//#ifndef IMPLOT_NO_FORCE_INLINE
//#ifdef _MSC_VER
//#define IMPLOT_INLINE __forceinline
//#elif defined(__GNUC__)
//#define IMPLOT_INLINE inline __attribute__((__always_inline__))
//#elif defined(__CLANG__)
//#if __has_attribute(__always_inline__)
//#define IMPLOT_INLINE inline __attribute__((__always_inline__))
//#else
//#define IMPLOT_INLINE inline
//#endif
//#else
//#define IMPLOT_INLINE inline
//#endif
//#else
//#define IMPLOT_INLINE inline
//#endif
//
//#if defined __SSE__ || defined __x86_64__ || defined _M_X64
//#ifndef IMGUI_ENABLE_SSE
//#include <immintrin.h>
//#endif
//static IMPLOT_INLINE float  ImInvSqrt(float x) { return _mm_cvtss_f32(_mm_rsqrt_ss(_mm_set_ss(x))); }
//#else
val Float.invSqrt: Float
    get() = 1f / sqrt(this)
//#endif
//
//#define IMPLOT_NORMALIZE2F_OVER_ZERO(VX,VY) do { float d2 = VX*VX + VY*VY; if (d2 > 0.0f) { float inv_len = ImInvSqrt(d2); VX *= inv_len; VY *= inv_len; } } while (0)
//
//// Support for pre-1.82 versions. Users on 1.82+ can use 0 (default) flags to mean "all corners" but in order to support older versions we are more explicit.
//#if (IMGUI_VERSION_NUM < 18102) && !defined(ImDrawFlags_RoundCornersAll)
//#define ImDrawFlags_RoundCornersAll ImDrawCornerFlags_All
//#endif