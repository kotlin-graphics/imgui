/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

/**
 * Flags for ImGui::Selectable().
 * 
 * @author GBarbieri
 */
public interface SelectableFlags {

    // Default: 0
    //
    /**
     * Clicking this don't close parent popup window.
     */
    public static final int DontClosePopups = 1 << 0;

    /**
     * Selectable frame can span all columns (text will still fit in current column).
     */
    public static final int SpanAllColumns = 1 << 1;

    /**
     * Generate press events on double clicks too.
     */
    public static final int AllowDoubleClick = 1 << 2;
}
