/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

/**
 * Enumeration for ColorEditMode().
 *
 * @author GBarbieri
 */
public interface ColorEditMode {

    public final int UserSelect = -2;
    
    public final int UserSelectShowButton = -1;
    
    public final int RGB = 0;
    
    public final int HSV = 1;
    
    public final int HEX = 2;
}
