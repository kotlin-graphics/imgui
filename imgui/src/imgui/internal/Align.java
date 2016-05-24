/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui.internal;

/**
 *
 * @author GBarbieri
 */
public interface Align {

    public final int Left = 1 << 0;
    
    public final int Center = 1 << 1;
    
    public final int Right = 1 << 2;
    
    public final int Top = 1 << 3;
    
    public final int VCenter = 1 << 4;
    
    public final int Default = Left | Top;
}
