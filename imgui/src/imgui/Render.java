/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

import com.jogamp.opengl.GL3;

/**
 *
 * @author GBarbieri
 */
public abstract interface Render {
    
    public abstract void renderDrawLists(GL3 gl3, DrawData drawData);
}
