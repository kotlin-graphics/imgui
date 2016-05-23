/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

/**
 * User fill ImGuiIO.KeyMap[] array with indices into the ImGuiIO.KeysDown[512] array.
 *
 * @author GBarbieri
 */
public interface Key {

    /**
     * For tabbing through fields.
     */
    public static final int Tab = 0;

    /**
     * For text edit.
     */
    public static final int LeftArrow = 1;

    /**
     * For text edit.
     */
    public static final int RightArrow = 2;

    /**
     * For text edit.
     */
    public static final int UpArrow = 3;

    /**
     * For text edit.
     */
    public static final int DownArrow = 4;

    public static final int PageUp = 5;

    public static final int PageDown = 6;

    /**
     * For text edit.
     */
    public static final int Home = 7;

    /**
     * For text edit.
     */
    public static final int End = 8;

    /**
     * For text edit.
     */
    public static final int Delete = 9;

    /**
     * For text edit.
     */
    public static final int Backspace = 10;

    /**
     * For text edit.
     */
    public static final int Enter = 11;

    /**
     * For text edit.
     */
    public static final int Escape = 12;

    /**
     * For text edit CTRL+A: select all.
     */
    public static final int A = 13;

    /**
     * For text edit CTRL+C: copy.
     */
    public static final int C = 14;

    /**
     * For text edit CTRL+V: paste.
     */
    public static final int V = 15;

    /**
     * For text edit CTRL+X: cut.
     */
    public static final int X = 16;

    /**
     * For text edit CTRL+Y: redo.
     */
    public static final int Y = 17;

    /**
     * For text edit CTRL+Z: undo.
     */
    public static final int Z = 18;

    public static final int COUNT = 19;
}
