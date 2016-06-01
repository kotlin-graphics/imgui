/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

import static com.jogamp.opengl.GL.GL_ACTIVE_TEXTURE;
import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER_BINDING;
import static com.jogamp.opengl.GL.GL_BLEND;
import static com.jogamp.opengl.GL.GL_BLEND_DST;
import static com.jogamp.opengl.GL.GL_BLEND_EQUATION_ALPHA;
import static com.jogamp.opengl.GL.GL_BLEND_EQUATION_RGB;
import static com.jogamp.opengl.GL.GL_BLEND_SRC;
import static com.jogamp.opengl.GL.GL_CULL_FACE;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER_BINDING;
import static com.jogamp.opengl.GL.GL_FUNC_ADD;
import static com.jogamp.opengl.GL.GL_ONE_MINUS_SRC_ALPHA;
import static com.jogamp.opengl.GL.GL_SCISSOR_TEST;
import static com.jogamp.opengl.GL.GL_SRC_ALPHA;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE_BINDING_2D;
import static com.jogamp.opengl.GL.GL_VIEWPORT;
import static com.jogamp.opengl.GL2ES2.GL_CURRENT_PROGRAM;
import static com.jogamp.opengl.GL2ES3.GL_VERTEX_ARRAY_BINDING;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import imgui.DrawData;
import imgui.ImGui;
import imgui.Render;
import java.nio.IntBuffer;

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
     * @param gl3
     * @param drawData
     */
    @Override
    public void renderDrawLists(GL3 gl3, DrawData drawData) {

        // Avoid rendering when minimized, scale coordinates for retina displays (screen coordinates != framebuffer coordinates)
        imgui.IO io = ImGui.getIO();
        int fbWidth = (int) (io.displaySize.x * io.displayFramebufferScale.x);
        int fbHeight = (int) (io.displaySize.y * io.displayFramebufferScale.y);
        if (fbWidth == 0 || fbHeight == 0) {
            return;
        }

        drawData.scaleClipRects(io.displayFramebufferScale);

        // Backup GL state
        IntBuffer data = GLBuffers.newDirectIntBuffer(4);
        gl3.glGetIntegerv(GL_CURRENT_PROGRAM, data);
        int lastProgram = data.get(0);
        gl3.glGetIntegerv(GL_TEXTURE_BINDING_2D, data);
        int lastTexture = data.get(0);
        gl3.glGetIntegerv(GL_ACTIVE_TEXTURE, data);
        int lastActiveTexture = data.get(0);
        gl3.glGetIntegerv(GL_ARRAY_BUFFER_BINDING, data);
        int lastArrayBuffer = data.get(0);
        gl3.glGetIntegerv(GL_ELEMENT_ARRAY_BUFFER_BINDING, data);
        int lastElementArrayBuffer = data.get(0);
        gl3.glGetIntegerv(GL_VERTEX_ARRAY_BINDING, data);
        int lastVertexArray = data.get(0);
        gl3.glGetIntegerv(GL_BLEND_SRC, data);
        int lastBlendSrc = data.get(0);
        gl3.glGetIntegerv(GL_BLEND_DST, data);
        int lastBlendDst = data.get(0);
        gl3.glGetIntegerv(GL_BLEND_EQUATION_RGB, data);
        int lastBlendEquationRgb = data.get(0);
        gl3.glGetIntegerv(GL_BLEND_EQUATION_ALPHA, data);
        int lastBlendEquationAlpha;
        gl3.glGetIntegerv(GL_VIEWPORT, data);
        int[] lastViewport = {data.get(0), data.get(1), data.get(2), data.get(3)};
        boolean lastEnableBlend = gl3.glIsEnabled(GL_BLEND);
        boolean lastEnableCullFace = gl3.glIsEnabled(GL_CULL_FACE);
        boolean lastEnableDepthTest = gl3.glIsEnabled(GL_DEPTH_TEST);
        boolean lastEnableScissorTest = gl3.glIsEnabled(GL_SCISSOR_TEST);

        // Setup render state: alpha-blending enabled, no face culling, no depth testing, scissor enabled
        gl3.glEnable(GL_BLEND);
        gl3.glBlendEquation(GL_FUNC_ADD);
        gl3.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        gl3.glDisable(GL_CULL_FACE);
        gl3.glDisable(GL_DEPTH_TEST);
        gl3.glEnable(GL_SCISSOR_TEST);
        gl3.glActiveTexture(GL_TEXTURE0);
    }
}
