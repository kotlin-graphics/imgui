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
import kotlin.math.cos
import kotlin.math.sin

//-----------------------------------------------------------------------------
// [SECTION] ImDrawList support
//-----------------------------------------------------------------------------


// ImDrawList: Helper function to calculate a circle's segment count given its radius and a "maximum error" value.
// FIXME: the minimum number of auto-segment may be undesirably high for very small radiuses (e.g. 1.0f)
const val DRAWLIST_CIRCLE_AUTO_SEGMENT_MIN = 12
const val DRAWLIST_CIRCLE_AUTO_SEGMENT_MAX = 512
fun DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC(_RAD: Float, _MAXERROR: Float) = clamp(((glm.πf * 2f) / acos((_RAD - _MAXERROR) / _RAD)).i, DRAWLIST_CIRCLE_AUTO_SEGMENT_MIN, DRAWLIST_CIRCLE_AUTO_SEGMENT_MAX)

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

    fun setCircleSegmentMaxError_(maxError: Float) {
        if (circleSegmentMaxError == maxError)
            return
        circleSegmentMaxError = maxError
        for (i in circleSegmentCounts.indices) {
            val radius = i.f
            val segmentCount = if(i > 0) DRAWLIST_CIRCLE_AUTO_SEGMENT_CALC(radius, circleSegmentMaxError) else 0
            circleSegmentCounts[i] = segmentCount min 255
        }
    }
}

/** Helper to build a ImDrawData instance */
class DrawDataBuilder {
    /** Global layers for: regular, tooltip */
    val layers = Array(2) { ArrayList<DrawList>() }

    fun clear() = layers.forEach { it.clear() }

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