package imgui.internal.classes

import glm_.glm
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import imgui.internal.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** An axis-aligned rectangle (2 points)
 *  2D axis aligned bounding-box
 *  NB: we can't rely on ImVec2 math operators being available here */
class Rect {

    /** Upper-left  */
    var min = Vec2()

    /** Lower-right */
    var max = Vec2()

    constructor()

    constructor(min: Vec2i, max: Vec2i) {
        this.min put min
        this.max put max
    }

    constructor(min: Vec2i, max: Vec2) {
        this.min put min
        this.max = max
    }

    constructor(min: Vec2, max: Vec2) {
        this.min = Vec2(min)
        this.max = Vec2(max)
    }

    constructor(min: Vec2, max: Vec2i) {
        this.min = Vec2(min)
        this.max = Vec2(max)
    }
    constructor(min: Float, max: Float) {
        this.min put min
        this.max put max
    }

    constructor(v: Vec4) {
        min.put(v.x, v.y)
        max.put(v.z, v.w)
    }

    constructor(r: Rect) {
        min put r.min
        max put r.max
    }

    constructor(x1: Float, y1: Float, x2: Float, y2: Float) {
        min.put(x1, y1)
        max.put(x2, y2)
    }

    val center get() = (min + max) * 0.5f
    val size get() = max - min
    val width get() = max.x - min.x
    val height get() = max.y - min.y
    val area get() = (max.x - min.x) * (max.y - min.y)

    /** Top-left    */
    val tl get() = Vec2(min)

    /** Top-right   */
    val tr get() = Vec2(max.x, min.y)

    /** Bottom-left */
    val bl get() = Vec2(min.x, max.y)

    /** Bottom-right    */
    val br get() = Vec2(max)

    infix operator fun contains(p: Vec2) = p.x >= min.x && p.y >= min.y && p.x < max.x && p.y < max.y
    infix operator fun contains(r: Rect) = r.min.x >= min.x && r.min.y >= min.y && r.max.x <= max.x && r.max.y <= max.y
    infix fun overlaps(r: Rect) = r.min.y < max.y && r.max.y > min.y && r.min.x < max.x && r.max.x > min.x
    infix fun overlaps(v: Vec4) = v.y < max.y && v.w > min.y && v.x < max.x && v.z > min.x
    infix fun add(p: Vec2) { // TODO operator?
        if (min.x > p.x) min.x = p.x
        if (min.y > p.y) min.y = p.y
        if (max.x < p.x) max.x = p.x
        if (max.y < p.y) max.y = p.y
    }

    infix fun add(r: Vec4) {
        if (min.x > r.x) min.x = r.x
        if (min.y > r.y) min.y = r.y
        if (max.x < r.z) max.x = r.z
        if (max.y < r.w) max.y = r.w
    }

    infix fun add(r: Rect) {
        if (min.x > r.min.x) min.x = r.min.x
        if (min.y > r.min.y) min.y = r.min.y
        if (max.x < r.max.x) max.x = r.max.x
        if (max.y < r.max.y) max.y = r.max.y
    }

    infix fun expand(amount: Float) {
        min.x -= amount
        min.y -= amount
        max.x += amount
        max.y += amount
    }

    infix fun expand(amount: Vec2) {
        min.x -= amount.x
        min.y -= amount.y
        max.x += amount.x
        max.y += amount.y
    }

    infix fun translate(d: Vec2) {
        min.x += d.x
        min.y += d.y
        max.x -= d.x
        max.y -= d.y
    }

    infix fun translateX(dx: Float) {
        min.x += dx
        max.x += dx
    }

    infix fun translateY(dy: Float) {
        min.y += dy
        max.y += dy
    }

    /** Simple version, may lead to an inverted rectangle, which is fine for Contains/Overlaps test but not for display. */
    infix fun clipWith(r: Rect) {
        min = min max r.min
        max = max min r.max
    }

    /** Full version, ensure both points are fully clipped. */
    infix fun clipWithFull(r: Rect) {
        min = glm.clamp(min, r.min, r.max)
        max = glm.clamp(max, r.min, r.max)
    }

    fun floor() {
        min.x = floor(min.x)
        min.y = floor(min.y)
        max.x = floor(max.x)
        max.y = floor(max.y)
    }

    val isInverted: Boolean get() = min.x > max.x || min.y > max.y
    fun toVec4(): Vec4 = Vec4(min, max)

    fun put(min: Vec2, max: Vec2) {
        this.min put min
        this.max put max
    }

    fun put(min: Float, max: Float) {
        this.min put min
        this.max put max
    }

    fun put(minX: Float, minY: Float, maxX: Float, maxY: Float) {
        min.put(minX, minY)
        max.put(maxX, maxY)
    }

    infix fun put(vec4: Vec4) {
        min.put(vec4.x, vec4.y)
        max.put(vec4.z, vec4.w)
    }

    infix fun put(rect: Rect) {
        min put rect.min
        max put rect.max
    }

    /* [JVM] for node-editor */

    val isEmpty: Boolean
        get() = min.x >= max.x || min.y >= max.y

    fun closestPoint(p: Vec2, snapToEdge: Boolean): Vec2 = when {
        !snapToEdge && contains(p) -> p
        else -> Vec2 { if (p[it] > max[it]) max[it] else if (p[it] < min[it]) min[it] else p[it] }
    }

    fun closestPoint(p: Vec2, snapToEdge: Boolean, radius: Float): Vec2 {

        val point = closestPoint(p, snapToEdge)

        val offset = p - point
        val distanceSq = offset.x * offset.x + offset.y * offset.y
        if (distanceSq <= 0)
            return point

        val distance = sqrt(distanceSq)

        return point + offset * (min(distance, radius) * (1f / distance))
    }

    infix fun closestPoint(other: Rect): Vec2 {
        val result = Vec2()
        result.x = when {
            other.min.x >= max.x -> max.x
            other.max.x <= min.x -> min.x
            else -> (kotlin.math.max(min.x, other.min.x) + kotlin.math.min(max.x, other.max.x)) / 2
        }
        result.y = if (other.min.y >= max.y)
            max.y
        else if (other.max.y <= min.y)
            min.y
        else
            (kotlin.math.max(min.y, other.min.y) + kotlin.math.min(max.y, other.max.y)) / 2
        return result
    }

    override fun toString() = "min: $min, max: $max"
}