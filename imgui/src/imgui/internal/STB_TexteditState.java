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
public class STB_TexteditState {

    /**
     * Position of the text cursor within the string.
     */
    int cursor;
    
    /**
     * Selection start and end point in characters; if equal, no selection. Note that start may be less than or greater
     * than end (e.g. when dragging the mouse, start is where the initial click was, and you can drag in either direction).
     */
    int selectStart, selectEnd;
    
    /**
     * each textfield keeps its own insert mode state. to keep an app-wide insert mode, copy this value in/out of the app
     * state.
     */
    byte insertMode;

    /**
     * Not implemented yet.
     */
    byte cursorAtEndOfLine;
    
    byte initialized;
    
    byte hasPreferredX;
    
    byte singleLine;
    
    byte padding1, padding2, padding3;
    
    /**
     * this determines where the cursor up/down tries to seek to along x
     */
    float preferredX;
}
