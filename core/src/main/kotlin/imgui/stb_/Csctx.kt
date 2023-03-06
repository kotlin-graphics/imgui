package imgui.stb_

import glm_.i

class Csctx(val bounds: Boolean) {
    var started = false
    var firstX = 0f
    var firstY = 0f
    var x = 0f
    var y = 0f
    var minX = 0
    var maxX = 0
    var minY = 0
    var maxY = 0

    lateinit var vertices: Array<Vertex>
    var numVertices = 0

    fun trackVertex(x: Int, y: Int) {
        if (x > maxX || !started) maxX = x
        if (y > maxY || !started) maxY = y
        if (x < minX || !started) minX = x
        if (y < minY || !started) minY = y
        started = true
    }

    fun v(type: Vertex.Type, x: Int, y: Int, cx: Int, cy: Int, cx1: Int, cy1: Int) {
        if (bounds) {
            trackVertex(x, y)
            if (type == Vertex.Type.cubic) {
                trackVertex(cx, cy)
                trackVertex(cx1, cy1)
            }
        } else vertices.last().apply {
            set(type, x, y, cx, cy)
            cX1 = cx1
            cY1 = cy1
        }
        numVertices++
    }

    fun closeShape() {
        if (firstX != x || firstY != y)
            v(Vertex.Type.line, firstX.i, firstY.i, 0, 0, 0, 0)
    }

    fun rMoveTo(dx: Float, dy: Float) {
        closeShape()
        x += dx
        firstX = x
        y += dy
        firstY = y
        v(Vertex.Type.move, x.i, y.i, 0, 0, 0, 0)
    }

    fun rLineTo(dx: Float, dy: Float) {
        x += dx
        y += dy
        v(Vertex.Type.line, x.i, y.i, 0, 0, 0, 0)
    }

    fun rcCurveTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float, dx3: Float, dy3: Float) {
        val cx1 = x + dx1
        val cy1 = y + dy1
        val cx2 = cx1 + dx2
        val cy2 = cy1 + dy2
        x = cx2 + dx3
        y = cy2 + dy3
        v(Vertex.Type.cubic, x.i, y.i, cx1.i, cy1.i, cx2.i, cy2.i)
    }
}