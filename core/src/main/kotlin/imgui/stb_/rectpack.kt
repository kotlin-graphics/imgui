package imgui.stb_

import glm_.bool
import glm_.i
import imgui.DEBUG
import java.util.*
import kotlin.Comparator

// [DEAR IMGUI]
// This is a slightly modified version of stb_rect_pack.h 1.01.
// Grep for [DEAR IMGUI] to find the changes.
//
// stb_rect_pack.h - v1.01 - public domain - rectangle packing
// Sean Barrett 2014
//
// Useful for e.g. packing rectangular textures into an atlas.
// Does not do rotation.
//
// Before #including,
//
//    #define STB_RECT_PACK_IMPLEMENTATION
//
// in the file that you want to have the implementation.
//
// Not necessarily the awesomest packing method, but better than
// the totally naive one in stb_truetype (which is primarily what
// this is meant to replace).
//
// Has only had a few tests run, may have issues.
//
// More docs to come.
//
// No memory allocations; uses qsort() and assert() from stdlib.
// Can override those by defining STBRP_SORT and STBRP_ASSERT.
//
// This library currently uses the Skyline Bottom-Left algorithm.
//
// Please note: better rectangle packers are welcome! Please
// implement them to the same API, but with a different init
// function.
//
// Credits
//
//  Library
//    Sean Barrett
//  Minor features
//    Martins Mozeiko
//    github:IntellectualKitty
//
//  Bugfixes / warning fixes
//    Jeremy Jaussaud
//    Fabian Giesen
//
// Version history:
//
//     1.01  (2021-07-11)  always use large rect mode, expose STBRP__MAXVAL in public section
//     1.00  (2019-02-25)  avoid small space waste; gracefully fail too-wide rectangles
//     0.99  (2019-02-07)  warning fixes
//     0.11  (2017-03-03)  return packing success/fail result
//     0.10  (2016-10-25)  remove cast-away-const to avoid warnings
//     0.09  (2016-08-27)  fix compiler warnings
//     0.08  (2015-09-13)  really fix bug with empty rects (w=0 or h=0)
//     0.07  (2015-09-13)  fix bug with empty rects (w=0 or h=0)
//     0.06  (2015-04-15)  added STBRP_SORT to allow replacing qsort
//     0.05:  added STBRP_ASSERT to allow replacing assert
//     0.04:  fixed minor bug in STBRP_LARGE_RECTS support
//     0.01:  initial release
//
// LICENSE
//
//   See end of file for license information.

object rectpack {

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

        override fun toString() = "id=$id w=$w h=$h x=$x y=$y wasPacked=$wasPacked"
    }


    enum class Skyline {
        blSortHeight, bfSortHeight;

        companion object {
            val default = blSortHeight
        }
    }

    //////////////////////////////////////////////////////////////////////////////
    //
    // the details of the following structures don't matter to you, but they must
    // be visible so you can handle the memory allocations for them





    const val INIT_skyline = 1

    class FindResult {
        var x = 0
        var y = 0
        var prevLink: Ptr<Ptr<Node>>? = null
        override fun toString() = "x=$x y=$y prevLink=$prevLink"
    }



    val rectHeightCompare = Comparator<Rect> { a, b ->
        when {
            a.h > b.h -> -1
            a.h < b.h -> 1
            a.w > b.w -> -1
            else -> (a.w < b.w).i
        }
    }

    const val MAXVAL = 0x7fffffff
    // Mostly for internal use, but this is the maximum supported coordinate value.

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

//typealias PPNode = () -> rectpack.Node?
//typealias PPNodeB = KMutableProperty0<rp.Node?>