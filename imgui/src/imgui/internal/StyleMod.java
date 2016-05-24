/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui.internal;

import glm.vec._2.Vec2;

/**
 * Stacked style modifier, backup of modified data so we can restore it.
 *
 * @author GBarbieri
 */
public class StyleMod {

    int var;

    Vec2 previousValue;
}
