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
public interface ButtonFlags {

    /**
     * Hold to repeat.
     */
    public static final int Repeat = 1 << 0;
    /**
     * (default) Return pressed on click+release on same item (default if no PressedOn** flag is set).
     */
    public static final int PressedOnClickRelease = 1 << 1;
    /**
     * Return pressed on click (default requires click+release).
     */
    public static final int PressedOnClick = 1 << 2;
    /**
     * Return pressed on release (default requires click+release).
     */
    public static final int PressedOnRelease = 1 << 3;
    /**
     * Return pressed on double-click (default requires click+release).
     */
    public static final int PressedOnDoubleClick = 1 << 4;
    /**
     * Allow interaction even if a child window is overlapping.
     */
    public static final int FlattenChild = 1 << 5;
    /**
     * Disable automatically closing parent popup on press.
     */
    public static final int DontClosePopups = 1 << 6;
    /**
     * Disable interaction.
     */
    public static final int Disabled = 1 << 7;
    /**
     * Vertically align button to match text baseline - ButtonEx() only.
     */
    public static final int AlignTextBaseLine = 1 << 8;
    /**
     * Disable interaction if a key modifier is held.
     */
    public static final int NoKeyModifiers = 1 << 9;
    /**
     * Require previous frame HoveredId to either match id or be null before being usable.
     */
    public static final int AllowOverlapMode = 1 << 10;
}
