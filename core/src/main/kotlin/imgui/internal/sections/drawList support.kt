package imgui.internal.sections

import glm_.f
import glm_.glm
import glm_.i
import glm_.min
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
// Estimation of number of circle segment based on error is derived using method described in
// this post (https://stackoverflow.com/a/2244088/15194693).
// Number of segments (N) is calculated using equation:
//
//            +-                     -+
//            |           pi          |
//   N = ceil | --------------------- |     where r > 0, error <= r
//            |  acos(1 - error / r)  |
//            +-                     -+
//
// Note:
//     Equation is significantly simpler that one in the post thanks for choosing segment
//     that is perpendicular to X axis. Follow steps in the article from this starting condition
//     and you will get this result.
//
// Rendering circles with an odd number of segments, while mathematically correct will produce
// asymmetrical results on the raster grid. Therefore we're rounding N to next even number.
// (7 became 8, 11 became 12, but 8 will still be 8).
const val DRAWLIST_CIRCLE_AUTO_SEGMENT_MIN = 4
const val DRAWLIST_CIRCLE_AUTO_SEGMENT_MAX = 512
fun DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC(_RAD: Float, _MAXERROR: Float) = clamp(((ceil(glm.πf / acos(1 - (_MAXERROR min _RAD) / _RAD)).i + 1) / 2) * 2, DRAWLIST_CIRCLE_AUTO_SEGMENT_MIN, DRAWLIST_CIRCLE_AUTO_SEGMENT_MAX)

/** ImDrawList: You may set this to higher values (e.g. 2 or 3) to increase tessellation of fast rounded corners path. */
var DRAWLIST_ARCFAST_TESSELLATION_MULTIPLIER = 1

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

    // [Internal] Lookup tables

    // Lookup tables
    val arcFastVtx = Array(12 * DRAWLIST_ARCFAST_TESSELLATION_MULTIPLIER) {
        // FIXME: Bake rounded corners fill/borders in atlas
        val a = it * 2 * glm.PIf / 12
        Vec2(cos(a), sin(a))
    }

    /** Precomputed segment count for given radius before we calculate it dynamically (to avoid calculation overhead) */
    val circleSegmentCounts = IntArray(64)

    /** UV of anti-aliased lines in the atlas */
    lateinit var texUvLines: Array<Vec4>

    fun setCircleTessellationMaxError_(maxError: Float) {
        if (circleSegmentMaxError == maxError)
            return
        circleSegmentMaxError = maxError
        for (i in circleSegmentCounts.indices) {
            val radius = i.f
            circleSegmentCounts[i] = when {
                i > 0 -> DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC(radius, circleSegmentMaxError)
                else -> 0
            }
        }
    }
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