/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

import glm.vec._2.Vec2;
import glm.vec._4.Vec4;
import java.util.ArrayList;

/**
 * Draw command list
 *
 * This is the low-level list of polygons that ImGui functions are filling. At the end of the frame, all command lists are
 * passed to your ImGuiIO::RenderDrawListFn function for rendering.
 * At the moment, each ImGui window contains its own ImDrawList but they could potentially be merged in the future.
 * If you want to add custom rendering within a window, you can use ImGui::GetWindowDrawList() to access the current draw
 * list and add your own primitives.
 * You can interleave normal ImGui:: calls and adding primitives to the current draw list.
 * All positions are in screen coordinates (0,0=top-left, 1 pixel per unit). Primitives are always added to the list and not
 * culled (culling is done at render time and at a higher-level by ImGui:: functions).
 *
 * @author GBarbieri
 */
public class DrawList {

    /**
     * This is what you have to render.
     */
    /**
     * Commands. Typically 1 command = 1 gpu draw call.
     */
    ArrayList<DrawCmd> cmdBuffer = new ArrayList<>();

    /**
     * Index buffer. Each command consume ImDrawCmd::ElemCount of those.
     */
    ArrayList<Short> idxBuffer = new ArrayList<>();

    /**
     * Vertex buffer.
     */
    ArrayList<DrawVert> vtxBuffer = new ArrayList<>();

    /**
     * --------------------------------------------------------------------------------------------------------------------
     * [Internal, used while building lists].
     */
    /**
     * Pointer to owner window's name (if any) for debugging.
     */
    private String ownerName;

    /**
     * == VtxBuffer.Size.
     */
    private int vtxCurrentIdx;

    /**
     * Point within VtxBuffer.Data after each add command (to avoid using the ImVector<> operators too much).
     */
    DrawVert vtxWritePtr;

    /**
     * Point within IdxBuffer.Data after each add command (to avoid using the ImVector<> operators too much).
     */
    short idxWritePtr;

    ArrayList<Vec4> clipRectStack = new ArrayList<>();

    ArrayList<Integer> textureIdStack = new ArrayList<>();

    /**
     * Current path building.
     */
    ArrayList<Vec2> path = new ArrayList<>();

    /**
     * Current channel number (0).
     */
    int channelsCurrent;

    /**
     * Number of active channels (1+).
     */
    int channelsCount;

    /**
     * Draw channels for columns API (not resized down so _ChannelsCount may be smaller than _Channels.Size).
     */
    ArrayList<DrawChannel> channels;

    private final Vec4 nullClipRect = new Vec4(-8192.0f, -8192.0f, +8192.0f, +8192.0f);

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public void clear() {

        cmdBuffer.clear();
        idxBuffer.clear();
        vtxBuffer.clear();
        vtxCurrentIdx = 0;
        vtxWritePtr = null;
        idxWritePtr = 0;
        clipRectStack.clear();
        textureIdStack.clear();
        path.clear();
        channelsCurrent = 0;
        channelsCount = 1;
        // NB: Do not clear channels so our allocations are re-used after the first frame.
    }

    public void clearFreeMemory() {

        cmdBuffer.clear();
        idxBuffer.clear();
        vtxBuffer.clear();
        vtxCurrentIdx = 0;
        vtxWritePtr = null;
        idxWritePtr = 0;
        clipRectStack.clear();
        textureIdStack.clear();
        path.clear();
        channelsCurrent = 0;
        channelsCount = 1;
        for (int i = 0; i < channels.size(); i++) {
            channels.get(i).cmdBuffer.clear();
            channels.get(i).idxBuffer.clear();
        }
    }

    private Vec4 getCurrentClipRect() {
        return !clipRectStack.isEmpty() ? clipRectStack.get(clipRectStack.size() - 1) : nullClipRect;
    }

    private int getCurrentTextureId() {
        return !textureIdStack.isEmpty() ? textureIdStack.get(textureIdStack.size() - 1) : 0;
    }

    private void addDrawCmd() {

        DrawCmd drawCmd = new DrawCmd();
        drawCmd.clipRect = getCurrentClipRect();
        drawCmd.textureId = getCurrentTextureId();
        
        assert (drawCmd.clipRect.x <= drawCmd.clipRect.z && drawCmd.clipRect.y <= drawCmd.clipRect.w);
        cmdBuffer.add(drawCmd);
    }
    
}
