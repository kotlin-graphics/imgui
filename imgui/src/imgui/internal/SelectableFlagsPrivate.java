/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui.internal;

/**
 * NB: need to be in sync with last value of ImGuiSelectableFlags
 * 
 * @author GBarbieri
 */
public interface SelectableFlagsPrivate {
   
    public static final int Menu = 1 << 3;
    public static final int MenuItem = 1 << 4;
    public static final int Disabled = 1 << 5;
    public static final int DrawFillAvailWidth = 1 << 6;
}
