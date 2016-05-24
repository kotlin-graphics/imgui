/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

import glm.vec._2.Vec2;
import java.util.ArrayList;

/**
 * All draw data to render an ImGui frame.
 *
 * @author GBarbieri
 */
public class DrawData {

    /**
     * Only valid after Render() is called and before the next NewFrame() is called.
     */
    boolean valid = false;

    ArrayList<DrawList> cmdLists = null;

    int cmdListsCount = 0;

    /**
     * For convenience, sum of all cmd_lists vtx_buffer.Size.
     */
    int totalVtxCount = 0;

    /**
     * For convenience, sum of all cmd_lists idx_buffer.Size.
     */
    int totalIdxCount = 0;

    /**
     * Helper to scale the ClipRect field of each ImDrawCmd. Use if your final output buffer is at a different scale than 
     * ImGui expects, or if there is a difference between your window resolution and framebuffer resolution.
     *
     * @param scale
     */
    public void scaleClipRects(Vec2 scale) {

        for (DrawList cmdList : cmdLists) {
            
            for (DrawCmd cmd : cmdList.cmdBuffer) {
                
                cmd.clipRect.set(cmd.clipRect.x * scale.x, cmd.clipRect.y * scale.y, cmd.clipRect.z * scale.x, 
                        cmd.clipRect.w * scale.y);
            }
        }
    }
}
