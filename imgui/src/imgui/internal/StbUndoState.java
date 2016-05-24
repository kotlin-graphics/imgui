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
public class StbUndoState {

    public static final int STB_TEXTEDIT_UNDOSTATECOUNT = 99;
    public static final int STB_TEXTEDIT_UNDOCHARCOUNT = 999;

    StbUndoRecord[] undoRec = new StbUndoRecord[STB_TEXTEDIT_UNDOSTATECOUNT];

    short[] undoChar = new short[STB_TEXTEDIT_UNDOCHARCOUNT];

    short undoPoint, redoPoint;
    
    short undoCharPoint, redoCharPoint;
}
