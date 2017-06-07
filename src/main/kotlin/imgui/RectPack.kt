package imgui

import glm.i
import glm.vec2.Vec2i
import java.util.ArrayList

object RectPack {

    /** // 16 bytes, nominally  */
    class Rect {
        /** reserved for your use:  */
        var id = 0

        /** input:  */
        var w = 0
        /** input:  */
        var h = 0

        /** output:  */
        var x = 0
        /** output:  */
        var y = 0

        /** non-zero if valid packing   */
        var wasPacked = false
    }

    class Context {
        lateinit var size: Vec2i
        var align = 0
        var initMode = 0
        var heuristic = HeuristicSkyline.default
        var numNodes = 0
        var activeHead: Node? = null
        var freeHead: Node? = null
        /** we allocate two extra nodes so optimal user-node-count is 'width' not 'width+2' */
        val extra = arrayOf(Node(), Node())
    }

    /** the details of the following structures don't matter to you, but they must be visible so you can handle the
     *  memory allocations for them */
    class Node {
        var coord = Vec2i()
        var x = 0
            get() = coord.x
            set(value) {
                coord.x = value
                field = value
            }
        var y = 0
            get() = coord.y
            set(value) {
                coord.y = value
                field = value
            }
        var next: Node? = null
    }

    fun initTarget(context: Context, size: Vec2i, nodes: Array<Node>, numNodes: Int) {
        if (!LARGE_RECTS)
            assert(size.x <= 0xffff && size.y <= 0xffff)

        for (i in 0 until numNodes - 1)
            nodes[i].next = nodes[i + 1]
        nodes.last().next = null
        context.initMode = Init.skyline.i
        context.heuristic = HeuristicSkyline.default
        context.freeHead = nodes[0]
        context.activeHead = context.extra[0]
        context.size put size
        context.numNodes = numNodes
        setupAllowOutOfMem(context, false)

        // node 0 is the full width, node 1 is the sentinel (lets us not store width explicitly)
        context.extra[0].coord.put(0, 0)
        context.extra[0].next = context.extra[1]
        context.extra[1].coord.put(size.x, if (LARGE_RECTS) 1 shl 30 else 65535)
        context.extra[1].next = null
    }

    var LARGE_RECTS = false
    val MAXVAL get() = if (LARGE_RECTS) 0xffffffff.i else 0xffff

    enum class Init(val i: Int) { skyline(1) }

    enum class HeuristicSkyline(val i: Int) { default(0), bl_sortHeight(default.i), bf_sortHeight(2) }

    fun setupAllowOutOfMem(context: Context, allowOutOfMem: Boolean) {
        if (allowOutOfMem)
        /*  if it's ok to run out of memory, then don't bother aligning them; this gives better packing, but may fail
            due to OOM (even though the rectangles easily fit). TODO a smarter approach would be to only quantize once
            we've hit OOM, then we could get rid of this parameter. */
            context.align = 1
        else {
            /*  if it's not ok to run out of memory, then quantize the widths so that num_nodes is always enough nodes.
                I.e. num_nodes * align >= width
                                 align >= width / num_nodes
                                 align = ceil(width/num_nodes)  **/
            context.align = (context.size.x + context.numNodes - 1) / context.numNodes
        }
    }

    fun packRects(context: Context, rects: ArrayList<Rect>, numRects: Int) {

        // we use the 'was_packed' field internally to allow sorting/unsorting
        for (i in 0 until numRects) {
            rects[i].wasPacked = i != 0
            if (!LARGE_RECTS)
                assert(rects[i].w <= 0xffff && rects[i].h <= 0xffff)
        }

        // sort according to heuristic
        rects.sortBy { it.h }

        for (i in 0 until numRects) {
            if (rects[i].w == 0 || rects[i].h == 0) {
                // empty rect needs no space
                rects[i].x = 0
                rects[i].y = 0
            } else {
                val fr = skylinePackRectangle(context, Vec2i(rects[i].w, rects[i].h))
                if (fr.prevLink != null) {
                    rects[i].x = fr.x
                    rects[i].y = fr.y
                } else {
                    rects[i].x = MAXVAL
                    rects[i].y = MAXVAL
                }
            }
        }

        // unsort
        rects.sortBy { it.wasPacked }

        // set was_packed flags
        for (i in 0 until numRects)
            rects[i].wasPacked = !(rects[i].x == MAXVAL && rects[i].y == MAXVAL)
    }

    fun skylinePackRectangle(context: Context, size: Vec2i): FindResult {
        // find best position according to heuristic
        val res = skylineFindBestPos(context, size)

        /*  bail if:
                1. it failed
                2. the best node doesn't fit (we don't always check this)
                3. we're out of memory  */
        if (res.prevLink == null || res.y + size.y > context.size.y || context.freeHead == null) {
            res.prevLink = null
            return res
        }

        // on success, create new node
        val node = context.freeHead!!
        node.x = res.x
        node.y = res.y + size.y

        context.freeHead = node.next

        /*  insert the new node into the right starting point, and let 'cur' point to the remaining nodes needing to be
            stiched back in */
        var cur = res.prevLink!!
        if (cur.x < res.x) {
            // preserve the existing one, so start testing with the next one
            val next = cur.next
            cur.next = node
            cur = next!!
        } else
            res.prevLink = node

        // from here, traverse cur and free the nodes, until we get to one that shouldn't be freed
        while (cur.next != null && cur.next!!.x <= res.x + size.x) {
            val next = cur.next
            // move the current node to the free list
            cur.next = context.freeHead
            context.freeHead = cur
            cur = next!!
        }

        // stitch the list back in
        node.next = cur

        if (cur.x < res.x + size.x)
            cur.x = res.x + size.x

        if (_DEBUG) {
            cur = context.activeHead!!
            while (cur.x < context.size.x) {
                assert(cur.x < cur.next!!.x)
                cur = cur.next!!
            }
            assert(cur.next == null)

            run {
                var count = 0
                var c = context.activeHead
                while (c != null) {
                    c = c.next
                    ++count
                }
                c = context.freeHead
                while (c != null) {
                    c = c.next
                    ++count
                }
                assert(count == context.numNodes + 2)
            }
        }
        return res
    }

    class FindResult(val x: Int, val y: Int, var prevLink: Node?)

    fun skylineFindBestPos(c: Context, size: Vec2i): FindResult {

        var bestWaste = 1 shl 30
        var bestX = 0
        var bestY = 1 shl 30
        var best: Node? = null

        // align to multiple of c->align
        size.x += c.align - 1
        size.x -= size.x % c.align
        assert(size.x % c.align == 0)

        var node = c.activeHead
        var prev = c.activeHead
        while (node!!.x + size.x <= c.size.x) {
            val (y, waste) = skylineFindMinY(c, node, node.x, size.x)
            if (c.heuristic == HeuristicSkyline.bl_sortHeight) {
                //  actually just want to test BL, bottom left
                if (y < bestY) {
                    bestY = y
                    best = prev
                }
            } else {
                // best-fit
                if (y + size.y <= c.size.y) {
                    // can only use it if it first vertically
                    if (y < bestY || (y == bestY && waste < bestWaste)) {
                        bestY = y
                        bestWaste = waste
                        best = prev
                    }
                }
            }
            prev = node.next
            node = node.next
        }

        bestX = best?.x ?: 0

        /*  if doing best-fit (BF), we also have to try aligning right edge to each node position

            e.g, if fitting

            |____________________|

                    into

            |                         |
            |             ____________|
            |____________|

            then right-aligned reduces waste, but bottom-left BL is always chooses left-aligned

            This makes BF take about 2x the time    */

        if (c.heuristic == HeuristicSkyline.bf_sortHeight) {
            var tail = c.activeHead
            node = c.activeHead
            prev = c.activeHead
            // find first node that's admissible
            while (tail!!.x < size.x)
                tail = tail.next
            while (tail != null) {
                val xPos = tail.x - size.x
                assert(xPos >= 0)
                // find the left position that matches this
                while (node!!.next!!.x <= xPos) {
                    prev = node.next
                    node = node.next
                }
                assert(node.next!!.x > xPos && node.x <= xPos)
                val (y, waste) = skylineFindMinY(c, node, xPos, size.x)
                if (y + size.y < c.size.y) {
                    if (y <= bestY) {
                        if (y < bestY || waste < bestWaste || (waste == bestWaste && xPos < bestX)) {
                            bestX = xPos
                            assert(y <= bestY)
                            bestY = y
                            bestWaste = waste
                            best = prev
                        }
                    }
                }
                tail = tail.next
            }
        }
        return FindResult(bestX, bestY, best)
    }

    /** find minimum y position if it starts at x1  */
    fun skylineFindMinY(c: Context, first: Node, x0: Int, width: Int): Pair<Int, Int> {

        var node = first
        val x1 = x0 + width

        assert(first.x <= x0)

//        if(false)
//        // skip in case we're past the node
//        while (node.next!!.coord.x <= x0)
//        node = node
//        #else
        assert(node.next!!.x > x0) // we ended up handling this in the caller for efficiency

        assert(node.x <= x0)

        var minY = 0
        var wasteArea = 0
        var visitedWidth = 0
        while (node.x < x1) {
            if (node.y > minY) {
                /*  raise min_y higher. we've accounted for all waste up to min_y, but we'll now add more waste for
                    everything we've visted */
                wasteArea += visitedWidth * (node.y - minY)
                minY = node.y
                // the first time through, visited_width might be reduced
                visitedWidth += node.next!!.x -
                        if (node.x < x0) x0
                        else node.x
            } else {
                // add waste area
                var underWidth = node.next!!.x - node.x
                if (underWidth + visitedWidth > width)
                    underWidth = width - visitedWidth
                wasteArea += underWidth * (minY - node.y)
                visitedWidth += underWidth
            }
            node = node.next!!
        }
        return minY to wasteArea
    }
}