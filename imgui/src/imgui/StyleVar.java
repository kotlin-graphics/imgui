/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

/**
 * Enumeration for PushStyleVar() / PopStyleVar().
 *
 * NB: the enum only refers to fields of ImGuiStyle() which makes sense to be pushed/poped in UI code. Feel free to add
 * others.
 *
 * @author GBarbieri
 */
public interface StyleVar {

    public final static int Alpha = 0;               // float

    public static final int WindowPadding = 1;       // ImVec2

    public static final int WindowRounding = 2;      // float

    public static final int WindowMinSize = 3;       // ImVec2

    public static final int ChildWindowRounding = 4; // float

    public static final int FramePadding = 5;        // ImVec2

    public static final int FrameRounding = 6;       // float

    public static final int ItemSpacing = 7;         // ImVec2

    public static final int ItemInnerSpacing = 8;    // ImVec2

    public static final int IndentSpacing = 9;       // float

    public static final int GrabMinSize = 10;        // float
}
