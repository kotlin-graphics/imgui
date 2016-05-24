/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

/**
 * Condition flags for ImGui::SetWindow***(), SetNextWindow***(), SetNextTreeNode***() functions
 * All those functions treat 0 as a shortcut to ImGuiSetCond_Always.
 *
 * @author GBarbieri
 */
public interface SetCond {

    /**
     * Set the variable.
     */
    public final int Always = 1 << 0;

    /**
     * Only set the variable on the first call per runtime session.
     */
    public final int Once = 1 << 1;

    /**
     * Only set the variable if the window doesn't exist in the .ini file.
     */
    public final int FirstUseEver = 1 << 2;

    /**
     * Only set the variable if the window is appearing after being inactive (or the first time).
     */
    public final int Appearing = 1 << 3;
}
