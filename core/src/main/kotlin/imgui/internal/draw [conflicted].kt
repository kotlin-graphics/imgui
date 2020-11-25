package imgui.internal

import glm_.asHexString
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.ImGui.io
import imgui.TextureID
import imgui.classes.DrawList
import imgui.logger
import imgui.resize
import kool.*
import kool.lib.indices
import java.util.Stack
import java.util.logging.Level

/** ImDrawCallback: Draw callbacks for advanced uses [configurable type: override in imconfig.h]
 *  NB: You most likely do NOT need to use draw callbacks just to create your own widget or customized UI rendering,
 *  you can poke into the draw list for that! Draw callback may be useful for example to:
 *      A) Change your GPU render state,
 *      B) render a complex 3D scene inside a UI element without an intermediate texture/render target, etc.
 *  The expected behavior from your rendering function is 'if (cmd.UserCallback != NULL) { cmd.UserCallback(parent_list, cmd); } else { RenderTriangles() }'
 *  If you want to override the signature of ImDrawCallback, you can simply use e.g. '#define ImDrawCallback MyDrawCallback' (in imconfig.h) + update rendering back-end accordingly. */
typealias DrawCallback = (DrawList, DrawCmd) -> Unit

// Typically, 1 command = 1 GPU draw call (unless command is a callback)
// - VtxOffset/IdxOffset: When 'io.BackendFlags & ImGuiBackendFlags_RendererHasVtxOffset' is enabled,
//   those fields allow us to render meshes larger than 64K vertices while keeping 16-bit indices.
//   Pre-1.71 back-ends will typically ignore the VtxOffset/IdxOffset fields.
// - The ClipRect/TextureId/VtxOffset fields must be contiguous as we memcmp() them together (this is asserted for).
class DrawCmd {

    // Also ensure our padding fields are zeroed
    constructor()

    constructor(drawCmd: DrawCmd) {
        put(drawCmd)
    }


    /** Clipping rectangle (x1, y1, x2, y2). Subtract ImDrawData->DisplayPos to get clipping rectangle in "viewport" coordinates */
    var clipRect = Vec4()

    /** User-provided texture ID. Set by user in ImfontAtlas::SetTexID() for fonts or passed to Image*() functions.
    Ignore if never using images or multiple fonts atlas.   */
    var textureId: TextureID? = null

    /** Start offset in vertex buffer. ImGuiBackendFlags_RendererHasVtxOffset: always 0, otherwise may be >0 to support
     *  meshes larger than 64K vertices with 16-bit indices. */
    var vtxOffset = 0

    /** Start offset in index buffer. Always equal to sum of ElemCount drawn so far. */
    var idxOffset = 0

    /** Number of indices (multiple of 3) to be rendered as triangles. Vertices are stored in the callee ImDrawList's
     *  vtx_buffer[] array, indices in idx_buffer[].    */
    var elemCount = 0

    /** If != NULL, call the function instead of rendering the vertices. clip_rect and texture_id will be set normally. */
    var userCallback: DrawCallback? = null

    /** Special Draw callback value to request renderer back-end to reset the graphics/render state.
     *  The renderer back-end needs to handle this special value, otherwise it will crash trying to call a function at this address.
     *  This is useful for example if you submitted callbacks which you know have altered the render state and you want it to be restored.
     *  It is not done by default because they are many perfectly useful way of altering render state for imgui contents
     *  (e.g. changing shader/blending settings before an Image call). */
    var resetRenderState = false

    var userCallbackData: Any? = null
//    void*           UserCallbackData;       // The draw callback code can access this.

    infix fun put(drawCmd: DrawCmd) {
        elemCount = drawCmd.elemCount
        clipRect put drawCmd.clipRect
        textureId = drawCmd.textureId
        vtxOffset = drawCmd.vtxOffset
        idxOffset = drawCmd.idxOffset
        userCallback = drawCmd.userCallback
        resetRenderState = drawCmd.resetRenderState
        userCallbackData = drawCmd.userCallbackData
    }

    // Compare ClipRect, TextureId and VtxOffset with a single memcmp()
//    #define ImDrawCmd_HeaderSize                        (IM_OFFSETOF(ImDrawCmd, VtxOffset) + sizeof(unsigned int))

    /** Compare ClipRect, TextureId, VtxOffset */
    infix fun headerCompare(other: DrawCmd): Boolean = clipRect == other.clipRect && textureId == other.textureId && vtxOffset == other.vtxOffset

    /** Copy ClipRect, TextureId, VtxOffset */
    infix fun headerCopy(other: DrawCmd) {
        clipRect put other.clipRect
        textureId = other.textureId
        vtxOffset = other.vtxOffset
    }
}

/** Vertex index, default to 16-bit
 *  To allow large meshes with 16-bit indices: set 'io.BackendFlags |= ImGuiBackendFlags_RendererHasVtxOffset' and handle ImDrawCmd::VtxOffset in the renderer back-end (recommended).
 *  To use 32-bits indices: override with '#define ImDrawIdx unsigned int' in imconfig.h. */
typealias DrawIdx = Int

/** Vertex layout
 *
 *  A single vertex (pos + uv + col = 20 bytes by default. Override layout with IMGUI_OVERRIDE_DRAWVERT_STRUCT_LAYOUT) */
class DrawVert(
        var pos: Vec2 = Vec2(),
        var uv: Vec2 = Vec2(),
        var col: Int = 0,
) {

    companion object {
        val SIZE = 2 * Vec2.size + Int.BYTES
        val OFS_POS = 0
        val OFS_UV = OFS_POS + Vec2.size
        val OFS_COL = OFS_UV + Vec2.size
    }

    override fun toString() = "pos: $pos, uv: $uv, col: $col"
}

/** Temporary storage to output draw commands out of order, used by ImDrawListSplitter and ImDrawList::ChannelsSplit()
 *
 *  Draw channels are used by the Columns API to "split" the render list into different channels while building, so
 *  items of each column can be batched together.
 *  You can also use them to simulate drawing layers and submit primitives in a different order than how they will be
 *  rendered.   */
class DrawChannel {

    val _cmdBuffer = Stack<DrawCmd>()
    var _idxBuffer = IntBuffer(0)

    fun memset0() {
        _cmdBuffer.clear()
        _idxBuffer.free()
        _idxBuffer = IntBuffer(0)
    }

    fun resize0() {
        _cmdBuffer.clear()
        _idxBuffer.lim = 0
    }
}

//-----------------------------------------------------------------------------
// ImDrawListSplitter
//-----------------------------------------------------------------------------
// FIXME: This may be a little confusing, trying to be a little too low-level/optimal instead of just doing vector swap..
//-----------------------------------------------------------------------------
/** Split/Merge functions are used to split the draw list into different layers which can be drawn into out of order.
 *  This is used by the Columns api, so items of each column can be batched together in a same draw call. */
class DrawListSplitter {
    /** Current channel number (0) */
    var _current = 0

    /** Number of active channels (1+) */
    var _count = 0

    /** Draw channels (not resized down so _Count might be < Channels.Size) */
    val _channels = Stack<DrawChannel>()

    init {
        clear()
    }

    fun clear() {
        _current = 0
        _count = 1
    } // Do not clear Channels[] so our allocations are reused next frame

    fun clearFreeMemory(destroy: Boolean = false) {
        _channels.forEach {
            //            if (i == _current)
//                memset(&_Channels[i], 0, sizeof(_Channels[i]));  // Current channel is a copy of CmdBuffer/IdxBuffer, don't destruct again
            it._cmdBuffer.clear()
            it._idxBuffer.free()
            if (!destroy)
                it._idxBuffer = IntBuffer(0).also { i -> logger.log(Level.INFO, "idxBuffer adr = ${i.adr.asHexString}") }
        }
        _current = 0
        _count = 1
        _channels.clear()
    }

    fun split(drawList: DrawList, channelsCount: Int) {
        assert(_current == 0 && _count <= 1) { "Nested channel splitting is not supported. Please use separate instances of ImDrawListSplitter." }
        val oldChannelsCount = _channels.size
        if (oldChannelsCount < channelsCount)
            for (i in oldChannelsCount until channelsCount)
                _channels += DrawChannel()
        _count = channelsCount

        // Channels[] (24/32 bytes each) hold storage that we'll swap with draw_list->_CmdBuffer/_IdxBuffer
        // The content of Channels[0] at this point doesn't matter. We clear it to make state tidy in a debugger but we don't strictly need to.
        // When we switch to the next channel, we'll copy draw_list->_CmdBuffer/_IdxBuffer into Channels[0] and then Channels[1] into draw_list->CmdBuffer/_IdxBuffer
        _channels[0].memset0()
        for (i in 1 until channelsCount) {
            if (i < oldChannelsCount)
                _channels[i].resize0()
            if (_channels[i]._cmdBuffer.isEmpty())
                _channels[i]._cmdBuffer += DrawCmd().apply { headerCopy(drawList._cmdHeader) } // Copy ClipRect, TextureId, VtxOffset
        }
    }

    infix fun merge(drawList: DrawList) {

        // Note that we never use or rely on _Channels.Size because it is merely a buffer that we never shrink back to 0 to keep all sub-buffers ready for use.
        if (_count <= 1) return

        setCurrentChannel(drawList, 0)
        drawList._popUnusedDrawCmd()

        // Calculate our final buffer sizes. Also fix the incorrect IdxOffset values in each command.
        var newCmdBufferCount = 0
        var newIdxBufferCount = 0
        var lastCmd = if (_count > 0 && drawList.cmdBuffer.isNotEmpty()) drawList.cmdBuffer.last() else null
        var idxOffset = lastCmd?.run { idxOffset + elemCount } ?: 0
        for (i in 1 until _count) {

            val ch = _channels[i]

            // Equivalent of PopUnusedDrawCmd() for this channel's cmdbuffer and except we don't need to test for UserCallback.
            if (ch._cmdBuffer.lastOrNull()?.elemCount == 0)
                ch._cmdBuffer.pop()

            if (ch._cmdBuffer.isNotEmpty() && lastCmd != null) {
                val nextCmd = ch._cmdBuffer[0]
                if (lastCmd headerCompare nextCmd && lastCmd.userCallback == null && nextCmd.userCallback == null) {
                    // Merge previous channel last draw command with current channel first draw command if matching.
                    lastCmd.elemCount += nextCmd.elemCount
                    idxOffset += nextCmd.elemCount
                    ch._cmdBuffer.clear() // FIXME-OPT: Improve for multiple merges.
                }
            }
            if (ch._cmdBuffer.isNotEmpty())
                lastCmd = ch._cmdBuffer.last()
            newCmdBufferCount += ch._cmdBuffer.size
            newIdxBufferCount += ch._idxBuffer.lim
            ch._cmdBuffer.forEach {
                it.idxOffset = idxOffset
                idxOffset += it.elemCount
            }
        }
        for (i in 0 until newCmdBufferCount)
            drawList.cmdBuffer.push(DrawCmd())
        drawList.idxBuffer = drawList.idxBuffer.resize(drawList.idxBuffer.rem + newIdxBufferCount)

        // Write commands and indices in order (they are fairly small structures, we don't copy vertices only indices)
        var cmdWrite = drawList.cmdBuffer.size - newCmdBufferCount
        var idxWrite = drawList.idxBuffer.rem - newIdxBufferCount
        for (i in 1 until _count) {
            val ch = _channels[i]
            for (j in ch._cmdBuffer.indices)
                drawList.cmdBuffer[cmdWrite++] put ch._cmdBuffer[j]
            for (j in ch._idxBuffer.indices)
                drawList.idxBuffer[idxWrite++] = ch._idxBuffer[j]
        }
        drawList._idxWritePtr = idxWrite

        // Ensure there's always a non-callback draw command trailing the command-buffer
        if (drawList.cmdBuffer.isEmpty() || drawList.cmdBuffer.last().userCallback != null)
            drawList.addDrawCmd()

        // If current command is used with different settings we need to add a new command
        val currCmd = drawList.cmdBuffer.last()
        if (currCmd.elemCount == 0)
            currCmd headerCopy drawList._cmdHeader // Copy ClipRect, TextureId, VtxOffset
        else if (!currCmd.headerCompare(drawList._cmdHeader))
            drawList.addDrawCmd()

        _count = 1
    }

    fun setCurrentChannel(drawList: DrawList, idx: Int) {
        assert(idx in 0 until _count)
        if (_current == idx) return

        // Overwrite ImVector (12/16 bytes), four times. This is merely a silly optimization instead of doing .swap()
        _channels[_current]._cmdBuffer.clear()
        _channels[_current]._cmdBuffer.addAll(drawList.cmdBuffer)
        _channels[_current]._idxBuffer.free()
        _channels[_current]._idxBuffer = imgui.IntBuffer(drawList.idxBuffer)
        _current = idx
        drawList.cmdBuffer.clear()
        drawList.cmdBuffer.addAll(_channels[idx]._cmdBuffer)
        drawList.idxBuffer.free()
        drawList.idxBuffer = imgui.IntBuffer(_channels[idx]._idxBuffer)
        drawList._idxWritePtr = drawList.idxBuffer.lim

        // If current command is used with different settings we need to add a new command
        val currCmd = drawList.cmdBuffer.last()
        if (currCmd.elemCount == 0)
            currCmd headerCopy drawList._cmdHeader // Copy ClipRect, TextureId, VtxOffset
        else if (!currCmd.headerCompare(drawList._cmdHeader))
            drawList.addDrawCmd()
    }
}


/** -----------------------------------------------------------------------------
 *  All draw command lists required to render the frame + pos/size coordinates to use for the projection matrix.
 *
 *  Draw List API (ImDrawCmd, ImDrawIdx, ImDrawVert, ImDrawChannel, ImDrawListFlags, ImDrawList, ImDrawData)
 *  Hold a series of drawing commands. The user provides a renderer for ImDrawData which essentially contains an array of ImDrawList.
 *
 *  All draw data to render a Dear ImGui frame
 *  (NB: the style and the naming convention here is a little inconsistent, we currently preserve them for backward compatibility purpose,
 *  as this is one of the oldest structure exposed by the library! Basically, ImDrawList == CmdList)
 *  ----------------------------------------------------------------------------- */

/** The maximum line width to bake anti-aliased textures for. Build atlas with ImFontAtlasFlags_NoBakedLines to disable baking. */
var DRAWLIST_TEX_LINES_WIDTH_MAX = 63

class DrawData {

    /** Only valid after Render() is called and before the next NewFrame() is called.   */
    var valid = false

    /** Array of ImDrawList* to render. The ImDrawList are owned by ImGuiContext and only pointed to from here. */
    val cmdLists = ArrayList<DrawList>()

    /** For convenience, sum of all DrawList's IdxBuffer.Size   */
    var totalIdxCount = 0

    /** For convenience, sum of all DrawList's VtxBuffer.Size   */
    var totalVtxCount = 0

    /** Upper-left position of the viewport to render (== upper-left of the orthogonal projection matrix to use) */
    var displayPos = Vec2()

    /** Size of the viewport to render (== io.displaySize for the main viewport) (displayPos + displaySize == lower-right of the orthogonal projection matrix to use) */
    var displaySize = Vec2()

    /** Amount of pixels for each unit of DisplaySize. Based on io.DisplayFramebufferScale. Generally (1,1) on normal display, (2,2) on OSX with Retina display. */
    var framebufferScale = Vec2()

    // Functions

    /** The ImDrawList are owned by ImGuiContext! */
    fun clear() {
        valid = false
        cmdLists.clear()
        totalIdxCount = 0
        totalVtxCount = 0
        displayPos put 0f
        displaySize put 0f
        framebufferScale put 0f
    }

    /** Helper to convert all buffers from indexed to non-indexed, in case you cannot render indexed.
     *  Note: this is slow and most likely a waste of resources. Always prefer indexed rendering!   */
    fun deIndexAllBuffers() {
        TODO()
//        val newVtxBuffer = mutableListOf<DrawVert>()
//        totalVtxCount = 0
//        totalIdxCount = 0
//        cmdLists.filter { it.idxBuffer.isNotEmpty() }.forEach { cmdList ->
//            for (j in cmdList.idxBuffer.indices)
//                newVtxBuffer[j] = cmdList.vtxBuffer[cmdList.idxBuffer[j]]
//            cmdList.vtxBuffer.clear()
//            cmdList.vtxBuffer.addAll(newVtxBuffer)
//            cmdList.idxBuffer.clear()
//            totalVtxCount += cmdList.vtxBuffer.size
//        }
    }

    /** Helper to scale the ClipRect field of each ImDrawCmd.
     *  Use if your final output buffer is at a different scale than draw Dear ImGui expects,
     *  or if there is a difference between your window resolution and framebuffer resolution.  */
    infix fun scaleClipRects(fbScale: Vec2) {
        cmdLists.forEach {
            it.cmdBuffer.forEach { cmd ->
                cmd.clipRect.timesAssign(fbScale.x, fbScale.y, fbScale.x, fbScale.y)
            }
        }
    }

    fun clone(): DrawData {
        val ret = DrawData()

        ret.cmdLists.addAll(cmdLists) // TODO check if it's fine https://github.com/kotlin-graphics/imgui/pull/135
        ret.displayPos put displayPos
        ret.displaySize put displaySize
        ret.totalIdxCount = totalIdxCount
        ret.totalVtxCount = totalVtxCount
        ret.valid = valid
        ret.framebufferScale put framebufferScale

        return ret
    }

    /** ~SetupDrawData */
    infix fun setup(drawLists: ArrayList<DrawList>) {
        valid = true
        cmdLists.clear()
        if (drawLists.isNotEmpty())
            cmdLists += drawLists
        totalIdxCount = 0
        totalVtxCount = 0
        displayPos put 0f
        displaySize put io.displaySize
        framebufferScale put io.displayFramebufferScale
        for (n in 0 until drawLists.size) {
            totalVtxCount += drawLists[n].vtxBuffer.lim
            totalIdxCount += drawLists[n].idxBuffer.lim
        }
    }
}