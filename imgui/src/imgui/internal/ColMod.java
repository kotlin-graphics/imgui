/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui.internal;

import glm.vec._4.Vec4;

/**
 * Stacked color modifier, backup of modified data so we can restore it.
 * 
 * @author GBarbieri
 */
public class ColMod {
    
    int col;
    Vec4 previousValue;
}
