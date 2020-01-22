package stb_

import imgui.NUL

object stbrp {

    class Context {
        var width = 0
        var height = 0
        var x = 0
        var y = 0
        var bottomY = 0
    }

    class Node {
        var x = NUL
    }

    class Rect {
        var x = 0
        var y = 0
        var id = 0
        var w = 0
        var h = 0
        var wasPacked = false
    }

    fun initTarget(con: Context, pw: Int, ph: Int, nodes: Array<Node>) {
        con.width = pw
        con.height = ph
        con.x = 0
        con.y = 0
        con.bottomY = 0
    }

    fun packRects(con: Context, rects: Array<Rect>) {
        var i = 0
        while (i in rects.indices) {
            if (con.x + rects[i].w > con.width) {
                con.x = 0
                con.y = con.bottomY
            }
            if (con.y + rects[i].h > con.height)
                break
            rects[i].x = con.x
            rects[i].y = con.y
            rects[i].wasPacked = true
            con.x += rects[i].w
            if (con.y + rects[i].h > con.bottomY)
                con.bottomY = con.y + rects[i].h
            i++
        }
        while (i in rects.indices)
            rects[i].wasPacked = false
    }
}