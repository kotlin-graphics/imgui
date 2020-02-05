package stb_

import glm_.bool
import glm_.i
import imgui.DEBUG

/*  [DEAR IMGUI]
    This is a slightly modified version of stb_rect_pack.h 1.00.
     Those changes would need to be pushed into nothings/stb:
     - Added STBRP__CDECL
     Grep for [DEAR IMGUI] to find the changes.

     stb_rect_pack.h - v1.00 - public domain - rectangle packing
     Sean Barrett 2014

     Useful for e.g. packing rectangular textures into an atlas.
     Does not do rotation.

     Not necessarily the awesomest packing method, but better than
     the totally naive one in stb_truetype (which is primarily what
     this is meant to replace).

     Has only had a few tests run, may have issues.

     More docs to come.

     No memory allocations; uses qsort() and assert() from stdlib.
     Can override those by defining STBRP_SORT and STBRP_ASSERT.

     This library currently uses the Skyline Bottom-Left algorithm.

     Please note: better rectangle packers are welcome! Please
     implement them to the same API, but with a different init
     function.

     Credits

      Library
        Sean Barrett
      Minor features
        Martins Mozeiko
        github:IntellectualKitty

      Bugfixes / warning fixes
        Jeremy Jaussaud
        Fabian Giesen

     Version history:

         1.00  (2019-02-25)  avoid small space waste; gracefully fail too-wide rectangles
         0.99  (2019-02-07)  warning fixes
         0.11  (2017-03-03)  return packing success/fail result
         0.10  (2016-10-25)  remove cast-away-const to avoid warnings
         0.09  (2016-08-27)  fix compiler warnings
         0.08  (2015-09-13)  really fix bug with empty rects (w=0 or h=0)
         0.07  (2015-09-13)  fix bug with empty rects (w=0 or h=0)
         0.06  (2015-04-15)  added STBRP_SORT to allow replacing qsort
         0.05:  added STBRP_ASSERT to allow replacing assert
         0.04:  fixed minor bug in STBRP_LARGE_RECTS support
         0.01:  initial release

     LICENSE

    See end of file for license information.
   */

// [JVM] STBRP_LARGE_RECTS = defined

object rp {

    /** Assign packed locations to rectangles. The rectangles are of type
    'stbrp_rect' defined below, stored in the array 'rects', and there
    are 'num_rects' many of them.

    Rectangles which are successfully packed have the 'was_packed' flag
    set to a non-zero value and 'x' and 'y' store the minimum location
    on each axis (i.e. bottom-left in cartesian coordinates, top-left
    if you imagine y increasing downwards). Rectangles which do not fit
    have the 'was_packed' flag set to 0.

    You should not try to access the 'rects' array from another thread
    while this function is running, as the function temporarily reorders
    the array while it executes.

    To pack into another rectangle, you need to call stbrp_init_target
    again. To continue packing into the same rectangle, you can call
    this function again. Calling this multiple times with multiple rect
    arrays will probably produce worse packing results than calling it
    a single time with the full rectangle array, but the option is
    available.

    The function returns 1 if all of the rectangles were successfully
    packed and 0 otherwise.
     */
    fun packRects(context: Context, rects: Array<Rect>): Boolean {

        var allRectsPacked = true

        // we use the 'was_packed' field internally to allow sorting/unsorting
        rects.forEachIndexed { i, r -> r.wasPacked = i }

        // sort according to heuristic
        rects.sortWith(compareBy({ it.h }, { it.w }))

        var ar = 0
        rects.forEach { r ->
            if (r.w == 0 || r.h == 0) {
                // empty rect needs no space
                r.x = 0
                r.y = 0
            } else {
                if(r.wasPacked == 86)
                    println()
                val fr = skylinePackRectangle(context, r.w, r.h, r.wasPacked)
                if (fr.prevLink != null) {
                    r.x = fr.x
                    r.y = fr.y
                } else {
                    r.x = MAXVAL
                    r.y = MAXVAL
                }
            }
        }

        // unsort
        rects.sortByDescending { it.wasPacked }

        // set was_packed flags and all_rects_packed status
        for (r in rects) {
            r.wasPacked = (r.x != MAXVAL || r.y != MAXVAL).i
            if (!r.wasPacked.bool)
                allRectsPacked = false
        }

        // return the all_rects_packed status
        return allRectsPacked
    }

    /** 16 bytes, nominally */
    class Rect {
        // reserved for your use:
        var id = 0

        // input:
        var w = 0
        var h = 0

        // output:
        var x = 0
        var y = 0

        /** non-zero if valid packing */
        var wasPacked = 0
    }

    /**
    Initialize a rectangle packer to:
    pack a rectangle that is 'width' by 'height' in dimensions
    using temporary storage provided by the array 'nodes', which is 'num_nodes' long

    You must call this function every time you start packing into a new target.

    There is no "shutdown" function. The 'nodes' memory must stay valid for
    the following stbrp_pack_rects() call (or calls), but can be freed after
    the call (or calls) finish.

    Note: to guarantee best results, either:
    1. make sure 'num_nodes' >= 'width'
    or  2. call stbrp_allow_out_of_mem() defined below with 'allow_out_of_mem = 1'

    If you don't do either of the above things, widths will be quantized to multiples
    of small integers to guarantee the algorithm doesn't run out of temporary storage.

    If you do #2, then the non-quantized algorithm will be used, but the algorithm
    may run out of temporary storage and be unable to pack some rectangles.
     */
    fun initTarget(context: Context, width: Int, height: Int, nodes: Array<Node>) {

        for (i in 0 until nodes.lastIndex)
            nodes[i].next = arrayOf(nodes[i + 1])
        nodes.last().next = null
        context.also {
            it.initMode = INIT_skyline
            it.heuristic = Skyline.default
            it.freeHead = arrayOf(nodes[0])
            it.activeHead = arrayOf(context.extra[0])
            it.width = width
            it.height = height
            it.numNodes = nodes.size

            setupAllowOutOfMem(context, false)

            // node 0 is the full width, node 1 is the sentinel (lets us not store width explicitly)
            it.extra[0].apply {
                x = 0
                y = 0
                next = arrayOf(it.extra[1])
            }
            it.extra[1].apply {
                x = width
                y = 1 shl 30
                next = null
            }
        }
    }

    /**
    Optionally call this function after init but before doing any packing to
    change the handling of the out-of-temp-memory scenario, described above.
    If you call init again, this will be reset to the default (false).
     */
    fun setupAllowOutOfMem(context: Context, allowOutOfMem: Boolean) {
        context.align = when {
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
            else -> (context.width + context.numNodes - 1) / context.numNodes
        }
    }


    /**
    Optionally select which packing heuristic the library should use. Different
    heuristics will produce better/worse results for different data sets.
    If you call init again, this will be reset to the default.
     */
    fun setupHeuristic(context: Context, heuristic: Skyline) {
        when (context.initMode) {
            INIT_skyline -> {
                assert(heuristic == Skyline.blSortHeight || heuristic == Skyline.bfSortHeight)
                context.heuristic = heuristic
            }
            else -> error("")
        }
    }


    enum class Skyline(val i: Int) { default(0), blSortHeight(default.i), bfSortHeight(1) }

    //////////////////////////////////////////////////////////////////////////////
    //
    // the details of the following structures don't matter to you, but they must
    // be visible so you can handle the memory allocations for them

    class Node {
        var x = 0
        var y = 0
        var next: Array<Node>? = null
    }

    class Context {
        var width = 0
        var height = 0
        var align = 0
        var initMode = 0
        var heuristic = Skyline.default
        var numNodes = 0

        var activeHead: Array<Node>? = null
        var freeHead: Array<Node>? = null

        /** we allocate two extra nodes so optimal user-node-count is 'width' not 'width+2' */
        val extra = Array(2) { Node() }
    }

    const val INIT_skyline = 1

    /** find minimum y position if it starts at x1
     *  [JVM]
     *  @return [y, waster] */
    fun skylineFindMinY(first: Node, x0: Int, width: Int): Pair<Int, Int> {
        var node = first
        val x1 = x0 + width

        assert(first.x <= x0)

//        #if 0
//        // skip in case we 're past the node
//        while (node->next->x <= x0)
//        ++node
//        #else
        assert(node.next!![0].x > x0) { "we ended up handling this in the caller for efficiency" }
//        #endif

        assert(node.x <= x0)

        var minY = 0
        var wasteArea = 0
        var visitedWidth = 0
        while (node.x < x1) {
            if (node.y > minY) {
                // raise min_y higher.
                // we've accounted for all waste up to min_y,
                // but we'll now add more waste for everything we've visted
                wasteArea += visitedWidth * (node.y - minY)
                minY = node.y
                // the first time through, visited_width might be reduced
                visitedWidth += node.next!![0].x - when {
                    node.x < x0 -> x0
                    else -> node.x
                }
            } else {
                // add waste area
                var underWidth = node.next!![0].x - node.x
                if (underWidth + visitedWidth > width)
                    underWidth = width - visitedWidth
                wasteArea += underWidth * (minY - node.y)
                visitedWidth += underWidth
            }
            node = node.next!![0]
        }
        return minY to wasteArea
    }

    class FindResult {
        var x = 0
        var y = 0
        var prevLink: Array<Node>? = null
    }

    fun skylineFindBestPos(c: Context, width_: Int, height: Int): FindResult {
        var bestWaste = 1 shl 30
        var bestY = 1 shl 30
        val fr = FindResult()
        var best: Array<Node>? = null

        // align to multiple of c->align
        var width = width_ + c.align - 1
        width -= width % c.align
        assert(width % c.align == 0)

        // if it can't possibly fit, bail immediately
        if (width > c.width || height > c.height) return fr

        var node = c.activeHead!![0]
        var prev = c.activeHead!!
        while (node.x + width <= c.width) {
            val (y, waste) = skylineFindMinY(node, node.x, width)
            if (c.heuristic == Skyline.blSortHeight) { // actually just want to test BL
                // bottom left
                if (y < bestY) {
                    bestY = y
                    best = prev
                }
            } else {
                // best-fit
                if (y + height <= c.height) {
                    // can only use it if it first vertically
                    if (y < bestY || (y == bestY && waste < bestWaste)) {
                        bestY = y
                        bestWaste = waste
                        best = prev
                    }
                }
            }
            prev = node.next!!
            node = node.next!![0]
        }

        var bestX = best?.get(0)?.x ?: 0

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

        if (c.heuristic == Skyline.bfSortHeight) {
            var tail: Node? = c.activeHead!![0]
            node = c.activeHead!![0]
            prev = c.activeHead!!
            // find first node that's admissible
            while (tail!!.x < width)
                tail = tail.next!![0]
            while (tail != null) {
                val xPos = tail.x - width
                assert(xPos >= 0)
                // find the left position that matches this
                while (node.next!![0].x <= xPos) {
                    prev = node.next!!
                    node = node.next!![0]
                }
                assert(node.next!![0].x > xPos && node.x <= xPos)
                val (y, waste) = skylineFindMinY(node, xPos, width)
                if (y + height <= c.height) {
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
                tail = tail.next!![0]
            }
        }

        return fr.apply {
            prevLink = best
            x = bestX
            y = bestY
        }
    }

    fun skylinePackRectangle(context: Context, width: Int, height: Int, wp: Int): FindResult {
        // find best position according to heuristic
        val res = skylineFindBestPos(context, width, height)

        // bail if:
        //    1. it failed
        //    2. the best node doesn't fit (we don't always check this)
        //    3. we're out of memory
        if (res.prevLink == null || res.y + height > context.height || context.freeHead == null)
            return res.apply { prevLink = null }

        // on success, create new node
        val node = context.freeHead!![0].apply {
            x = res.x
            y = res.y + height
        }

        context.freeHead = node.next

        // insert the new node into the right starting point, and
        // let 'cur' point to the remaining nodes needing to be
        // stiched back in

        var cur = res.prevLink!![0]
        if (cur.x < res.x) {
            // preserve the existing one, so start testing with the next one
            val next = cur.next!!
            cur.next = arrayOf(node)
            cur = next[0]
        } else res.prevLink!![0] = node

        // from here, traverse cur and free the nodes, until we get to one
        // that shouldn't be freed
        println("wp $wp")
        while (cur.next != null && cur.next!![0].x <= res.x + width) {
            val next = cur.next!!
            // move the current node to the free list
            cur.next!![0] = context.freeHead!![0]
            context.freeHead = arrayOf(cur)
            cur = next[0]
        }

        // stitch the list back in
        node.next = arrayOf(cur)

        if (cur.x < res.x + width)
            cur.x = res.x + width

        if (DEBUG) {
            cur = context.activeHead!![0]
            while (cur.x < context.width) {
                assert(cur.x < cur.next!![0].x)
                cur = cur.next!![0]
            }
            assert(cur.next == null)

            run {
                var count = 0
                var c = context.activeHead?.get(0)
                while (c != null) {
                    c = c.next?.get(0)
                    ++count
                }
                c = context.freeHead?.get(0)
                while (c != null) {
                    c = c.next?.get(0)
                    ++count
                }
                assert(count == context.numNodes + 2)
            }
        }

        return res
    }

    //    #ifdef STBRP_LARGE_RECTS
    const val MAXVAL = -1
//    #else
//    #define STBRP__MAXVAL  0xffff
//    #endif

//    STBRP_DEF int stbrp_pack_rects(stbrp_context *context, stbrp_rect *rects, int num_rects)
//    {
//        int i, all_rects_packed = 1;
//
//        // we use the 'was_packed' field internally to allow sorting/unsorting
//        for (i=0; i < num_rects; ++i) {
//        rects[i].was_packed = i;
//    }
//
//        // sort according to heuristic
//        STBRP_SORT(rects, num_rects, sizeof(rects[0]), rect_height_compare);
//
//        for (i=0; i < num_rects; ++i) {
//        if (rects[i].w == 0 || rects[i].h == 0) {
//            rects[i].x = rects[i].y = 0;  // empty rect needs no space
//        } else {
//            stbrp__findresult fr = stbrp__skyline_pack_rectangle(context, rects[i].w, rects[i].h);
//            if (fr.prev_link) {
//                rects[i].x = (stbrp_coord) fr.x;
//                rects[i].y = (stbrp_coord) fr.y;
//            } else {
//                rects[i].x = rects[i].y = STBRP__MAXVAL;
//            }
//        }
//    }
//
//        // unsort
//        STBRP_SORT(rects, num_rects, sizeof(rects[0]), rect_original_order);
//
//        // set was_packed flags and all_rects_packed status
//        for (i=0; i < num_rects; ++i) {
//        rects[i].was_packed = !(rects[i].x == STBRP__MAXVAL && rects[i].y == STBRP__MAXVAL);
//        if (!rects[i].was_packed)
//            all_rects_packed = 0;
//    }
//
//        // return the all_rects_packed status
//        return all_rects_packed;
//    }
}

typealias PPNode = () -> rp.Node?
//typealias PPNodeB = KMutableProperty0<rp.Node?>