/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

import glm.vec._4.Vec4;

/**
 * Typically, 1 command = 1 gpu draw call (unless command is a callback).
 *
 * @author GBarbieri
 */
public class DrawCmd {

    /**
     * Number of indices (multiple of 3) to be rendered as triangles. Vertices are stored in the callee ImDrawList's
     * vtx_buffer[] array, indices in idx_buffer[].
     */
    int elemCount = 0;

    /**
     * Clipping rectangle (x1, y1, x2, y2).
     */
    Vec4 clipRect = new Vec4(-8192.0f, -8192.0f, +8192.0f, +8192.0f);
    
    /**
     * User-provided texture ID. Set by user in ImfontAtlas::SetTexID() for fonts or passed to Image*() functions. Ignore if
     * never using images or multiple fonts atlas.
     * 
     * TODO check if intbuffer
     */
    int textureId = 0;            
    
    // TODO
//    ImDrawCallback UserCallback;           // If != NULL, call the function instead of rendering the vertices. clip_rect and texture_id will be set normally.
//    void*           UserCallbackData ;       // The draw callback code can access this.
}
