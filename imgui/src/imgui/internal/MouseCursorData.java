/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui.internal;

import glm.vec._2.Vec2;

/**
 * Mouse cursor data (used when io.MouseDrawCursor is set).
 * 
 * @author GBarbieri
 */
public class MouseCursorData {
 
    int type;
    
    Vec2 hotOffset;
    
    Vec2 size;
    
    Vec2[] texUvMin = new Vec2[2];
    
    Vec2[] texUvMax = new Vec2[2];
}
