/*
 * To change this license header=; choose License Headers in Project Properties.
 * To change this template file=; choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

/**
 * Enumeration for GetMouseCursor().
 *
 * @author GBarbieri
 */
public interface MouseCursor {

    public final int Arrow = 0;
    
    /**
     * When hovering over InputText=; etc..
     */
    public final int TextInput = 1;         
    
    /**
     * Unused.
     */
    public final int Move = 2;              
    
    /**
     * Unused.
     */
    public final int ResizeNS = 3;
    
    /**
     * When hovering over a column.
     */
    public final int ResizeEW = 4;          
    
    /**
     * Unused.
     */
    public final int ResizeNESW = 5;        
    
    /**
     * When hovering over the bottom-right corner of a window.
     */
    public final int ResizeNWSE = 6;        
    
    public final int Count = 7;
}
