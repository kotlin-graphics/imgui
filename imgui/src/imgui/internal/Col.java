/*
 * To change this license header=; choose License Headers in Project Properties.
 * To change this template file=; choose Tools | Templates
 * and open the template in the editor.
 */
package imgui.internal;

/**
 * Enumeration for PushStyleColor() / PopStyleColor().
 * 
 * @author GBarbieri
 */
public interface Col {

    public static final int Text = 0;

    public static final int TextDisabled = 1;

    /**
     * Background of normal windows.
     */
    public static final int WindowBg = 2;

    public static final int ChildWindowBg = 3;

    /**
     * Background of popups=; menus=; tooltips windows.
     */
    public static final int PopupBg = 4;

    public static final int Border = 5;

    public static final int BorderShadow = 6;

    /**
     * Background of checkbox=; radio button=; plot=; slider=; text input.
     */
    public static final int FrameBg = 7;

    public static final int FrameBgHovered = 8;

    public static final int FrameBgActive = 9;

    public static final int TitleBg = 10;

    public static final int TitleBgCollapsed = 11;

    public static final int TitleBgActive = 12;

    public static final int MenuBarBg = 13;

    public static final int ScrollbarBg = 14;

    public static final int ScrollbarGrab = 15;

    public static final int ScrollbarGrabHovered = 16;

    public static final int ScrollbarGrabActive = 17;

    public static final int ComboBg = 18;

    public static final int CheckMark = 19;

    public static final int SliderGrab = 20;

    public static final int SliderGrabActive = 21;

    public static final int Button = 22;

    public static final int ButtonHovered = 23;

    public static final int ButtonActive = 24;

    public static final int Header = 25;

    public static final int HeaderHovered = 26;

    public static final int HeaderActive = 27;

    public static final int Column = 28;

    public static final int ColumnHovered = 29;

    public static final int ColumnActive = 30;

    public static final int ResizeGrip = 31;

    public static final int ResizeGripHovered = 32;

    public static final int ResizeGripActive = 33;

    public static final int CloseButton = 34;

    public static final int CloseButtonHovered = 35;

    public static final int CloseButtonActive = 36;

    public static final int PlotLines = 37;

    public static final int PlotLinesHovered = 38;

    public static final int PlotHistogram = 39;
    
    public static final int PlotHistogramHovered = 40;
    
    public static final int TextSelectedBg = 41;
    
    /**
     * Darken entire screen when a modal window is active.
     */
    public static final int ModalWindowDarkening = 42;  
    
    public static final int COUNT = 43;
}
