/*
 * To change this license header; choose License Headers in Project Properties.
 * To change this template file; choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

/**
 * Flags for ImGui::Begin().
 * 
 * @author GBarbieri
 */
public interface WindowFlags {

    // Default: 0
    //
    /**
     * Disable title-bar.
     */
    public static final int NoTitleBar = 1 << 0;

    /**
     * Disable user resizing with the lower-right grip.
     */
    public static final int NoResize = 1 << 1;

    /**
     * Disable user moving the window.
     */
    public static final int NoMove = 1 << 2;

    /**
     * Disable scrollbars (window can still scroll with mouse or programatically).
     */
    public static final int NoScrollbar = 1 << 3;

    /**
     * Disable user vertically scrolling with mouse wheel.
     */
    public static final int NoScrollWithMouse = 1 << 4;

    /**
     * Disable user collapsing window by double-clicking on it.
     */
    public static final int NoCollapse = 1 << 5;

    /**
     * Resize every window to its content every frame.
     */
    public static final int AlwaysAutoResize = 1 << 6;

    /**
     * Show borders around windows and items.
     */
    public static final int ShowBorders = 1 << 7;

    /**
     * Never load/save settings in .ini file.
     */
    public static final int NoSavedSettings = 1 << 8;

    /**
     * Disable catching mouse or keyboard inputs.
     */
    public static final int NoInputs = 1 << 9;

    /**
     * Has a menu-bar.
     */
    public static final int MenuBar = 1 << 10;

    /**
     * Allow horizontal scrollbar to appear (off by default). You may use SetNextWindowContentSize(ImVec2(width;0.0f));
     * prior to calling Begin() to specify width. Read code in imgui_demo in the "Horizontal Scrolling" section.
     */
    public static final int HorizontalScrollbar = 1 << 11;

    /**
     * Disable taking focus when transitioning from hidden to visible state.
     */
    public static final int NoFocusOnAppearing = 1 << 12;

    /**
     * Disable bringing window to front when taking focus (e.g. clicking on it or programatically giving it focus).
     */
    public static final int NoBringToFrontOnFocus = 1 << 13;

    /**
     * Always show vertical scrollbar (even if ContentSize.y < Size.y).
     */
    public static final int AlwaysVerticalScrollbar = 1 << 14;

    /**
     * Always show horizontal scrollbar (even if ContentSize.x < Size.x).
     */
    public static final int AlwaysHorizontalScrollbar = 1 << 15;

    /**
     * Ensure child windows without border uses style.WindowPadding (ignored by default for non-bordered child windows;
     * because more convenient).
     */
    public static final int AlwaysUseWindowPadding = 1 << 16;

    // [Internal] --------------------------------------------------------------------------------------------------------
    //
    /**
     * For internal use by BeginChild().
     */
    static final int ChildWindow = 1 << 20;

    /**
     * For internal use by BeginChild().
     */
    static final int ChildWindowAutoFitX = 1 << 21;

    /**
     * For internal use by BeginChild().
     */
    static final int ChildWindowAutoFitY = 1 << 22;

    /**
     * For internal use by ComboBox().
     */
    static final int ComboBox = 1 << 23;

    /**
     * For internal use by BeginTooltip().
     */
    static final int Tooltip = 1 << 24;

    /**
     * Don't use! For internal use by BeginPopup().
     */
    static final int Popup = 1 << 25;

    /**
     * For internal use by BeginPopupModal().
     */
    static final int Modal = 1 << 26;

    /**
     * For internal use by BeginMenu().
     */
    static final int ChildMenu = 1 << 27;
}
