package plot.items

import imgui.classes.DrawList
import imgui.internal.DrawIdx
import imgui.internal.classes.Rect
import plot.api.plotDrawList
import plot.internalApi.currentPlot
import kotlin.math.min

//-----------------------------------------------------------------------------
// [SECTION] RenderPrimitives
//-----------------------------------------------------------------------------

// Renders primitive shapes in bulk as efficiently as possible.
//template <class _Renderer>
fun renderPrimitivesEx(renderer: RendererBase, drawList: DrawList, cullRect: Rect) {
    var prims = renderer.prims.toUInt()
    var primsCulled = 0u
    var idx = 0u
    renderer init drawList
    while (prims != 0u) {
        // find how many can be reserved up to end of current draw command's limit
        var cnt = min(prims, ((DrawIdx.MAX_VALUE - drawList._vtxCurrentIdx) / renderer.vtxConsumed).toUInt())
        // make sure at least this many elements can be rendered to avoid situations where at the end of buffer this slow path is not taken all the time
        if (cnt >= min(64u, prims)) {
            if (primsCulled >= cnt)
                primsCulled -= cnt // reuse previous reservation
            else {
                // add more elements to previous reservation
                drawList.primReserve(((cnt - primsCulled) * renderer.idxConsumed.toUInt()).toInt(), ((cnt - primsCulled) * renderer.vtxConsumed.toUInt()).toInt())
                primsCulled = 0u
            }
        } else {
            if (primsCulled > 0u) {
                drawList.primUnreserve((primsCulled * renderer.idxConsumed.toUInt()).toInt(), (primsCulled * renderer.vtxConsumed.toUInt()).toInt())
                primsCulled = 0u
            }
            cnt = min(prims, ((DrawIdx.MAX_VALUE - 0/*draw_list._VtxCurrentIdx*/) / renderer.vtxConsumed).toUInt())
            // reserve new draw command
            drawList.primReserve((cnt * renderer.idxConsumed.toUInt()).toInt(), (cnt * renderer.vtxConsumed.toUInt()).toInt())
        }
        prims -= cnt
        val ie = idx + cnt
        while (idx != ie) {
            if (!renderer.render(drawList, cullRect, idx.toInt()))
                primsCulled++
            ++idx
        }
    }
    if (primsCulled > 0u)
        drawList.primUnreserve((primsCulled * renderer.idxConsumed.toUInt()).toInt(), (primsCulled * renderer.vtxConsumed.toUInt()).toInt())
}

//template <template <class> class _Renderer, class _Getter, typename ...Args>
fun renderPrimitives1(renderer: RendererBase/*, Args... args*/) {
    val drawList = plotDrawList
    val cullRect = currentPlot!!.plotRect
    renderPrimitivesEx(renderer, drawList, cullRect)
}

//template <template <class,class> class _Renderer, class _Getter1, class _Getter2, typename ...Args>
fun renderPrimitives2(render: RendererBase/*, Args... args*/) {
    val drawList = plotDrawList
    val cullRect = currentPlot!!.plotRect
    renderPrimitivesEx(render, drawList, cullRect)
}