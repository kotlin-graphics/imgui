/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import imgui.DrawData;
import imgui.ImGui;
import imgui.Render;

/**
 *
 * @author GBarbieri
 */
public class NewtGL3Render implements Render {

    /**
     * This is the main rendering function that you have to implement and provide to ImGui (via setting up
     * 'RenderDrawListsFn' in the ImGuiIO structure)
     * If text or lines are blurry when integrating ImGui in your engine:
     * - in your Render function, try translating your projection matrix by (0.5f,0.5f) or (0.375f,0.375f).
     *
     * @param drawData
     */
    @Override
    public void renderDrawLists(DrawData drawData) {

        // Avoid rendering when minimized, scale coordinates for retina displays (screen coordinates != framebuffer coordinates)
        imgui.IO io = ImGui.getIO();
        int fbWidth = (int) (io.displaySize.x * io.displayFramebufferScale.x);
        int fbHeight = (int) (io.displaySize.y * io.displayFramebufferScale.y);
        if (fbWidth == 0 || fbHeight == 0) {
            return;
        }
        
    }
}
