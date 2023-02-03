package imgui.internal.sections

import glm_.*
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.clamp
import imgui.classes.DrawList
import imgui.font.Font
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

//-----------------------------------------------------------------------------
// [SECTION] ImDrawList support
//-----------------------------------------------------------------------------


// ImDrawList: Helper function to calculate a circle's segment count given its radius and a "maximum error" value.
//
// Estimation of number of circle segment based on error is derived using method described in https://stackoverflow.com/a/2244088/15194693
// Number of segments (N) is calculated using equation:
//   N = ceil ( pi / acos(1 - error / r) )     where r > 0, error <= r
// Our equation is significantly simpler that one in the post thanks for choosing segment that is
// perpendicular to X axis. Follow steps in the article from this starting condition and you will
// will get this result.
//
// Rendering circles with an odd number of segments, while mathematically correct will produce
// asymmetrical results on the raster grid. Therefore we're rounding N to next even number.
// asymmetrical results on the raster grid. Therefore we're rounding N to next even number (7->8, 8->8, 9->10 etc.)
//
fun ROUNDUP_TO_EVEN(_V: Int) = (((_V + 1) / 2) * 2)
const val DRAWLIST_CIRCLE_AUTO_SEGMENT_MIN = 4
const val DRAWLIST_CIRCLE_AUTO_SEGMENT_MAX = 512
fun DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC(_RAD: Float, _MAXERROR: Float) = clamp(ROUNDUP_TO_EVEN(ceil(glm.πf / acos(1 - (_MAXERROR min _RAD) / _RAD)).i), DRAWLIST_CIRCLE_AUTO_SEGMENT_MIN, DRAWLIST_CIRCLE_AUTO_SEGMENT_MAX)

// Raw equation from IM_DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC rewritten for 'r' and 'error'.
fun DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC_R(_N: Int, _MAXERROR: Float) = _MAXERROR / (1 - cos(glm.πf / (_N.f max glm.πf)))
fun DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC_ERROR(_N: Int, _RAD: Float) = (1 - cos(glm.πf / (_N.f max glm.πf))) / _RAD

// ImDrawList: Lookup table size for adaptive arc drawing, cover full circle.
//#ifndef IM_DRAWLIST_ARCFAST_TABLE_SIZE
const val DRAWLIST_ARCFAST_TABLE_SIZE = 48 // Number of samples in lookup table.

//#endif
val DRAWLIST_ARCFAST_SAMPLE_MAX = DRAWLIST_ARCFAST_TABLE_SIZE // Sample index _PathArcToFastEx() for 360 angle.

/** Data shared between all ImDrawList instances
 *  You may want to create your own instance of this if you want to use ImDrawList completely without ImGui. In that case, watch out for future changes to this structure.
 *  Data shared among multiple draw lists (typically owned by parent ImGui context, but you may create one yourself) */
class DrawListSharedData {
    /** UV of white pixel in the atlas  */
    var texUvWhitePixel = Vec2()

    /** Current/default font (optional, for simplified AddText overload) */
    var font: Font? = null

    /** Current/default font size (optional, for simplified AddText overload) */
    var fontSize = 0f

    var curveTessellationTol = 0f

    /** Number of circle segments to use per pixel of radius for AddCircle() etc */
    var circleSegmentMaxError = 0f

    /** Value for pushClipRectFullscreen() */
    var clipRectFullscreen = Vec4()

    /** Initial flags at the beginning of the frame (it is possible to alter flags on a per-drawlist basis afterwards) */
    var initialFlags = DrawListFlag.None.i

    /** [Internal] Temp write buffer */
    val tempBuffer = ArrayList<Vec2>()

    // [Internal] Lookup tables

    /** Sample points on the quarter of the circle. */
    val arcFastVtx = Array(DRAWLIST_ARCFAST_TABLE_SIZE) {
        // FIXME: Bake rounded corners fill/borders in atlas
        val a = it * 2 * glm.PIf / 12
        Vec2(cos(a), sin(a))
    }

    /** Cutoff radius after which arc drawing will fallback to slower PathArcTo() */
    var arcFastRadiusCutoff = DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC_R(DRAWLIST_ARCFAST_SAMPLE_MAX, circleSegmentMaxError)

    /** Precomputed segment count for given radius before we calculate it dynamically (to avoid calculation overhead) */
    val circleSegmentCounts = IntArray(64)

    /** UV of anti-aliased lines in the atlas */
    lateinit var texUvLines: Array<Vec4>

    fun setCircleTessellationMaxError_(maxError: Float) {
        if (circleSegmentMaxError == maxError)
            return

        assert(maxError > 0f)
        circleSegmentMaxError = maxError
        for (i in circleSegmentCounts.indices) {
            val radius = i.f
            circleSegmentCounts[i] = when {
                i > 0 -> DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC(radius, circleSegmentMaxError)
                else -> DRAWLIST_ARCFAST_SAMPLE_MAX
            }
        }
        arcFastRadiusCutoff = DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC_R(DRAWLIST_ARCFAST_SAMPLE_MAX, circleSegmentMaxError)
    }
}

fun ArrayList<Vec2>.reserveDiscard(size: Int) {
    for (i in this.size until size)
        this += Vec2()
}

/** Helper to build a ImDrawData instance */
class DrawDataBuilder {
    /** Global layers for: regular, tooltip */
    val layers = Array(2) { ArrayList<DrawList>() }

    fun clear() = layers.forEach { it.clear() }

    val drawListCount: Int
        get() = layers.sumOf { it.size }

    fun flattenIntoSingleLayer() {
        val size = layers.map { it.size }.count()
        layers[0].ensureCapacity(size)
        for (layerN in 1 until layers.size) {
            val layer = layers[layerN]
            if (layer.isEmpty()) continue
            layers[0].addAll(layer)
            layer.clear()
        }
    }
}