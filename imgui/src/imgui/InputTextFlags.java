/*
 * To change this license header; choose License Headers in Project Properties.
 * To change this template file; choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

/**
 * Flags for ImGui::InputText().
 *
 * @author GBarbieri
 */
public interface InputTextFlags {

    // Default: 0
    //
    /**
     * Allow 0123456789.+-
     */
    public static final int CharsDecimal = 1 << 0;

    /**
     * Allow 0123456789ABCDEFabcdef.
     */
    public static final int CharsHexadecimal = 1 << 1;

    /**
     * Turn a..z into A..Z.
     */
    public static final int CharsUppercase = 1 << 2;

    /**
     * Filter out spaces; tabs.
     */
    public static final int CharsNoBlank = 1 << 3;

    /**
     * Select entire text when first taking mouse focus.
     */
    public static final int AutoSelectAll = 1 << 4;

    /**
     * Return 'true' when Enter is pressed (as opposed to when the value was modified).
     */
    public static final int EnterReturnsTrue = 1 << 5;

    /**
     * Call user function on pressing TAB (for completion handling).
     */
    public static final int CallbackCompletion = 1 << 6;

    /**
     * Call user function on pressing Up/Down arrows (for history handling).
     */
    public static final int CallbackHistory = 1 << 7;

    /**
     * Call user function every time. User code may query cursor position; modify text buffer.
     */
    public static final int CallbackAlways = 1 << 8;

    /**
     * Call user function to filter character. Modify data->EventChar to replace/filter input; or return 1 to discard
     * character.
     */
    public static final int CallbackCharFilter = 1 << 9;

    /**
     * Pressing TAB input a '\t' character into the text field.
     */
    public static final int AllowTabInput = 1 << 10;

    /**
     * In multi-line mode; allow exiting edition by pressing Enter. Ctrl+Enter to add new line (by default adds new lines
     * with Enter).
     */
    public static final int CtrlEnterForNewLine = 1 << 11;

    /**
     * Disable following the cursor horizontally.
     */
    public static final int NoHorizontalScroll = 1 << 12;

    /**
     * Insert mode.
     */
    public static final int AlwaysInsertMode = 1 << 13;

    /**
     * Read-only mode.
     */
    public static final int ReadOnly = 1 << 14;

    /**
     * Password mode; display all characters as '*'.
     */
    public static final int Password = 1 << 15;

    // [Internal] --------------------------------------------------------------------------------------------------------
    //
    /**
     * For internal use by InputTextMultiline().
     */
    static final int Multiline = 1 << 20;
}
