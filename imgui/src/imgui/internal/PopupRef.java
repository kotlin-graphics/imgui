/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui.internal;

import glm.vec._2.Vec2;

/**
 * Storage for current popup stack.
 * 
 * @author GBarbieri
 */
public class PopupRef {
    
    /**
     * Set on OpenPopup().
     */
    int id;
    
    /**
     * Resolved on BeginPopup() - may stay unresolved if user never calls OpenPopup().
     */
    Window window;
    
    /**
     * Set on OpenPopup().
     */
    Window parentWindow;
    
    /**
     * Set on OpenPopup().
     */
    Window parentMenuSet;
    
    /**
     * Copy of mouse position at the time of opening popup.
     */
    Vec2 mousePosOnOpen;

    public PopupRef(int id, Window parentWindow, Window parentMenuSet, Vec2 mousePosOnOpen) {
        this.id = id;
        window = null;
        this.parentWindow = parentWindow;
        this.parentMenuSet = parentMenuSet;
        this.mousePosOnOpen = mousePosOnOpen;
    }
}
