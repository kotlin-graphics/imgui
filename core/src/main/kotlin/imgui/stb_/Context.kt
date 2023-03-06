package imgui.stb_

import glm_.bool
import glm_.i
import imgui.DEBUG

class Context
// ~stbrp_init_target
// Initialize a rectangle packer to:
//    pack a rectangle that is 'width' by 'height' in dimensions
//    using temporary storage provided by the array 'nodes', which is 'num_nodes' long
//
// You must call this function every time you start packing into a new target.
//
// There is no "shutdown" function. The 'nodes' memory must stay valid for
// the following stbrp_pack_rects() call (or calls), but can be freed after
// the call (or calls) finish.
//
// Note: to guarantee best results, either:
//       1. make sure 'num_nodes' >= 'width'
//   or  2. call stbrp_allow_out_of_mem() defined below with 'allow_out_of_mem = 1'
//
// If you don't do either of the above things, widths will be quantized to multiples
// of small integers to guarantee the algorithm doesn't run out of temporary storage.
//
// If you do #2, then the non-quantized algorithm will be used, but the algorithm
// may run out of temporary storage and be unable to pack some rectangles.
constructor(val width: Int,
            val height: Int,
        // [JVM] property just for comfort
            val nodes: Array<Node>) {

    init {
        for (i in 0 until nodes.lastIndex)
            nodes[i].next = ptrOf(nodes[i + 1])
        nodes.last().next = null
    }

    var align = 0
    val numNodes
        get() = nodes.size
    val initMode = rectpack.INIT_skyline
    var heuristic = rectpack.Skyline.default

    /** we allocate two extra nodes so optimal user-node-count is 'width' not 'width+2' */
    val extra = Array(2) { Node() }

    var activeHead: Ptr<Node>? = ptrOf(extra[0])
    var freeHead: Ptr<Node>? = ptrOf(nodes[0])

    init {
        setupAllowOutOfMem(false)

        // node 0 is the full width, node 1 is the sentinel (lets us not store width explicitly)
        extra[0].apply {
            x = 0
            y = 0
            next = ptrOf(extra[1])
        }
        extra[1].apply {
            x = width
            y = 1 shl 30
            next = null
        }
    }

    /**
    Optionally call this function after init but before doing any packing to
    change the handling of the out-of-temp-memory scenario, described above.
    If you call init again, this will be reset to the default (false).
     */
    fun setupAllowOutOfMem(allowOutOfMem: Boolean) {
        align = when {
            // if it's ok to run out of memory, then don't bother aligning them;
            // this gives better packing, but may fail due to OOM (even though
            // the rectangles easily fit). @TODO a smarter approach would be to only
            // quantize once we've hit OOM, then we could get rid of this parameter.
            allowOutOfMem -> 1

            // if it's not ok to run out of memory, then quantize the widths
            // so that num_nodes is always enough nodes.
            //
            // I.e. num_nodes * align >= width
            //                  align >= width / num_nodes
            //                  align = ceil(width/num_nodes)
            else -> (width + numNodes - 1) / numNodes
        }
    }


    /**
    Optionally select which packing heuristic the library should use. Different
    heuristics will produce better/worse results for different data sets.
    If you call init again, this will be reset to the default.
     */
    fun setupHeuristic(heuristic: rectpack.Skyline) {
        when (initMode) {
            rectpack.INIT_skyline -> {
                assert(heuristic == rectpack.Skyline.blSortHeight || heuristic == rectpack.Skyline.bfSortHeight)
                this.heuristic = heuristic
            }

            else -> error("")
        }
    }

    // ~stbrp_pack_rects
    // Assign packed locations to rectangles. The rectangles are of type
    // 'stbrp_rect' defined below, stored in the array 'rects', and there
    // are 'num_rects' many of them.
    //
    // Rectangles which are successfully packed have the 'was_packed' flag
    // set to a non-zero value and 'x' and 'y' store the minimum location
    // on each axis (i.e. bottom-left in cartesian coordinates, top-left
    // if you imagine y increasing downwards). Rectangles which do not fit
    // have the 'was_packed' flag set to 0.
    //
    // You should not try to access the 'rects' array from another thread
    // while this function is running, as the function temporarily reorders
    // the array while it executes.
    //
    // To pack into another rectangle, you need to call stbrp_init_target
    // again. To continue packing into the same rectangle, you can call
    // this function again. Calling this multiple times with multiple rect
    // arrays will probably produce worse packing results than calling it
    // a single time with the full rectangle array, but the option is
    // available.
    //
    // The function returns 1 if all of the rectangles were successfully
    // packed and 0 otherwise.
    fun packRects(rects: Array<rectpack.Rect>): Boolean {

        var allRectsPacked = true

        // we use the 'was_packed' field internally to allow sorting/unsorting
        rects.forEachIndexed { i, r -> r.wasPacked = i }

        // sort according to heuristic
        rects.sortWith(rectpack.rectHeightCompare)

        rects.forEach { r ->
            if (r.w == 0 || r.h == 0) {
                // empty rect needs no space
                r.x = 0
                r.y = 0
            } else {
                val fr = skylinePackRectangle(r.w, r.h)
                if (fr.prevLink != null) {
                    r.x = fr.x
                    r.y = fr.y
                } else {
                    r.x = rectpack.MAXVAL
                    r.y = rectpack.MAXVAL
                }
            }
        }

        // unsort
        rects.sortBy { it.wasPacked }

        // set was_packed flags and all_rects_packed status
        for (r in rects) {
            r.wasPacked = (r.x != rectpack.MAXVAL || r.y != rectpack.MAXVAL).i
            if (!r.wasPacked.bool)
                allRectsPacked = false
        }

        // return the all_rects_packed status
        return allRectsPacked
    }

    fun skylinePackRectangle(width: Int, height: Int): rectpack.FindResult {
        // find best position according to heuristic
        val res = skylineFindBestPos(width, height)

        // bail if:
        //    1. it failed
        //    2. the best node doesn't fit (we don't always check this)
        //    3. we're out of memory
        if (res.prevLink == null || res.y + height > height || freeHead == null)
            return res.apply { prevLink = null }

        // on success, create new node
        val node = freeHead!!
        node().x = res.x
        node().y = res.y + height

        freeHead = node().next

        // insert the new node into the right starting point, and
        // let 'cur' point to the remaining nodes needing to be
        // stiched back in

        var cur = res.prevLink!!()
        if (cur().x < res.x) {
            // preserve the existing one, so start testing with the next one
            val next = cur().next!!
            cur().next = node
            cur = next
        } else
            res.prevLink!!.value = node

        // from here, traverse cur and free the nodes, until we get to one
        // that shouldn't be freed
        while (cur().next != null && cur().next!!().x <= res.x + width) {
            val next = cur().next!!
            // move the current node to the free list
            cur().next = freeHead
            freeHead = cur
            cur = next
        }

        // stitch the list back in
        node().next = cur

        if (cur().x < res.x + width)
            cur().x = res.x + width

        if (DEBUG) {
            cur = activeHead!!
            while (cur().x < width) {
                assert(cur().x < cur().next!!().x)
                cur = cur().next!!
            }
            assert(cur().next == null)

            run {
                var count = 0
                var cur = activeHead
                while (cur != null) {
                    cur = cur().next
                    ++count
                }
                cur = freeHead
                while (cur != null) {
                    cur = cur().next
                    ++count
                }
                assert(count == numNodes + 2)
            }
        }

        return res
    }

    fun skylineFindBestPos(width_: Int, height: Int): rectpack.FindResult {
        var bestWaste = 1 shl 30
        var bestY = 1 shl 30
        val fr = rectpack.FindResult()
        var best: Ptr<Ptr<Node>>? = null

        // align to multiple of c->align
        var width = width_ + align - 1
        width -= width % align
        assert(width % align == 0)

        // if it can't possibly fit, bail immediately
        if (width > this.width || height > this.height) return fr

        var node = activeHead!!
        var prev = ptrOf(activeHead!!)
        while (node().x + width <= this.width) {
            val (y, waste) = skylineFindMinY(node, node().x, width)
            if (heuristic == rectpack.Skyline.blSortHeight) { // actually just want to test BL
                // bottom left
                if (y < bestY) {
                    bestY = y
                    best = prev
                }
            } else {
                // best-fit
                if (y + height <= this.height) {
                    // can only use it if it first vertically
                    if (y < bestY || (y == bestY && waste < bestWaste)) {
                        bestY = y
                        bestWaste = waste
                        best = prev
                    }
                }
            }
            prev = ptrOf(node().next!!)
            node = node().next!!
        }

        var bestX = if (best == null) 0 else best()().x

        // if doing best-fit (BF), we also have to try aligning right edge to each node position
        //
        // e.g, if fitting
        //
        //     ____________________
        //    |____________________|
        //
        //            into
        //
        //   |                         |
        //   |             ____________|
        //   |____________|
        //
        // then right-aligned reduces waste, but bottom-left BL is always chooses left-aligned
        //
        // This makes BF take about 2x the time

        if (heuristic == rectpack.Skyline.bfSortHeight) {
            var tail = activeHead
            node = activeHead!!
            prev = ptrOf(activeHead!!)
            // find first node that's admissible
            while (tail!!().x < width)
                tail = tail!!().next
            while (tail != null) {
                val xPos = tail().x - width
                assert(xPos >= 0)
                // find the left position that matches this
                while (node().next!!().x <= xPos) {
                    prev = ptrOf(node().next!!)
                    node = node().next!!
                }
                assert(node().next!!().x > xPos && node().x <= xPos)
                val (y, waste) = skylineFindMinY(node, xPos, width)
                if (y + height <= this.height) {
                    if (y <= bestY) {
                        if (y < bestY || waste < bestWaste || (waste == bestWaste && xPos < bestX)) {
                            bestX = xPos
                            //assert(y <= bestY) [DEAR IMGUI]
                            bestY = y
                            bestWaste = waste
                            best = prev
                        }
                    }
                }
                tail = tail().next
            }
        }

        return fr.apply {
            prevLink = best
            x = bestX
            y = bestY
        }
    }

    /** find minimum y position if it starts at x1
     *  [JVM]
     *  @return [y, waster] */
    fun skylineFindMinY(first: Ptr<Node>, x0: Int, width: Int): Pair<Int, Int> {
        var node = first
        val x1 = x0 + width

        assert(first().x <= x0)

//        #if 0
//        // skip in case we 're past the node
//        while (node->next->x <= x0)
//        ++node
//        #else
        assert(node().next!!().x > x0) { "we ended up handling this in the caller for efficiency" }
//        #endif

        assert(node().x <= x0)

        var minY = 0
        var wasteArea = 0
        var visitedWidth = 0
        while (node().x < x1) {
            if (node().y > minY) {
                // raise min_y higher.
                // we've accounted for all waste up to min_y,
                // but we'll now add more waste for everything we've visted
                wasteArea += visitedWidth * (node().y - minY)
                minY = node().y
                // the first time through, visited_width might be reduced
                visitedWidth += node().next!!().x - if (node().x < x0) x0 else node().x
            } else {
                // add waste area
                var underWidth = node().next!!().x - node().x
                if (underWidth + visitedWidth > width)
                    underWidth = width - visitedWidth
                wasteArea += underWidth * (minY - node().y)
                visitedWidth += underWidth
            }
            node = node().next!!
        }
        return minY to wasteArea
    }
}