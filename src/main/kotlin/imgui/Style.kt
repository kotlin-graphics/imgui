package imgui

import glm.vec2.Vec2
import glm.vec4.Vec4


object Style {

    /**  Global alpha applies to everything in ImGui    */
    var alpha = 1.0f
    /** Padding within a window */
    var windowPadding = Vec2(8)
    /** Minimum window size */
    var windowMinSize = Vec2(32)
    /** Radius of window corners rounding. Set to 0.0f to have rectangular windows  */
    var windowRounding = 9.0f
    /** Alignment for title bar text    */
    var windowTitleAlign = Vec2(0.0f, 0.5f)
    /** Radius of child window corners rounding. Set to 0.0f to have rectangular child windows  */
    var childWindowRounding = 0.0f
    /** Padding within a framed rectangle (used by most widgets)    */
    var framePadding = Vec2(4, 3)
    /** Radius of frame corners rounding. Set to 0.0f to have rectangular frames (used by most widgets).    */
    var frameRounding = 0.0f
    /** Horizontal and vertical spacing between widgets/lines   */
    var itemSpacing = Vec2(8, 4)
    /** Horizontal and vertical spacing between within elements of a composed widget (e.g. a slider and its label)  */
    var itemInnerSpacing = Vec2(4)
    /** Expand reactive bounding box for touch-based system where touch position is not accurate enough. Unfortunately
     *  we don't sort widgets so priority on overlap will always be given to the first widget. So don't grow this too
     *  much!   */
    var touchExtraPadding = Vec2()
    /** Horizontal spacing when e.g. entering a tree node. Generally == (FontSize + FramePadding.x*2).  */
    var indentSpacing = 21.0f
    /** Minimum horizontal spacing between two columns  */
    var columnsMinSpacing = 6.0f
    /** Width of the vertical scrollbar, Height of the horizontal scrollbar */
    var scrollbarSize = 16.0f
    /** Radius of grab corners rounding for scrollbar   */
    var scrollbarRounding = 9.0f
    /** Minimum width/height of a grab box for slider/scrollbar */
    var grabMinSize = 10.0f
    /** Radius of grabs corners rounding. Set to 0.0f to have rectangular slider grabs. */
    var grabRounding = 0.0f
    /** Alignment of button text when button is larger than text.   */
    var buttonTextAlign = Vec2(0.5f)
    /** Window positions are clamped to be visible within the display area by at least this amount. Only covers regular
     *  windows.    */
    var displayWindowPadding = Vec2(22)
    /** If you cannot see the edge of your screen (e.g. on a TV) increase the safe area padding. Covers popups/tooltips
     *  as well regular windows.    */
    var displaySafeAreaPadding = Vec2(4)
    /** Enable anti-aliasing on lines/borders. Disable if you are really short on CPU/GPU.  */
    var antiAliasedLines = true
    /**  Enable anti-aliasing on filled shapes (rounded rectangles, circles, etc.)  */
    var antiAliasedShapes = true
    /** Tessellation tolerance. Decrease for highly tessellated curves (higher quality, more polygons), increase to
     *  reduce quality. */
    var curveTessellationTol = 1.25f

    val colors = arrayOf(
            Vec4(0.90f, 0.90f, 0.90f, 1.00f), // Text
            Vec4(0.60f, 0.60f, 0.60f, 1.00f), // TextDisabled
            Vec4(0.00f, 0.00f, 0.00f, 0.70f), // WindowBg
            Vec4(0.00f, 0.00f, 0.00f, 0.00f), // ChildWindowBg
            Vec4(0.05f, 0.05f, 0.10f, 0.90f), // PopupBg
            Vec4(0.70f, 0.70f, 0.70f, 0.65f), // Border
            Vec4(0.00f, 0.00f, 0.00f, 0.00f), // BorderShadow
            Vec4(0.80f, 0.80f, 0.80f, 0.30f), // FrameBg: background of checkbox, radio button, plot, slider, text input
            Vec4(0.90f, 0.80f, 0.80f, 0.40f), // FrameBgHovered
            Vec4(0.90f, 0.65f, 0.65f, 0.45f), // FrameBgActive
            Vec4(0.27f, 0.27f, 0.54f, 0.83f), // TitleBg
            Vec4(0.40f, 0.40f, 0.80f, 0.20f), // TitleBgCollapsed
            Vec4(0.32f, 0.32f, 0.63f, 0.87f), // TitleBgActive
            Vec4(0.40f, 0.40f, 0.55f, 0.80f), // MenuBarBg
            Vec4(0.20f, 0.25f, 0.30f, 0.60f), // ScrollbarBg
            Vec4(0.40f, 0.40f, 0.80f, 0.30f), // ScrollbarGrab
            Vec4(0.40f, 0.40f, 0.80f, 0.40f), // ScrollbarGrabHovered
            Vec4(0.80f, 0.50f, 0.50f, 0.40f), // ScrollbarGrabActive
            Vec4(0.20f, 0.20f, 0.20f, 0.99f), // ComboBg
            Vec4(0.90f, 0.90f, 0.90f, 0.50f), // CheckMark
            Vec4(1.00f, 1.00f, 1.00f, 0.30f), // SliderGrab
            Vec4(0.80f, 0.50f, 0.50f, 1.00f), // SliderGrabActive
            Vec4(0.67f, 0.40f, 0.40f, 0.60f), // Button
            Vec4(0.67f, 0.40f, 0.40f, 1.00f), // ButtonHovered
            Vec4(0.80f, 0.50f, 0.50f, 1.00f), // ButtonActive
            Vec4(0.40f, 0.40f, 0.90f, 0.45f), // Header
            Vec4(0.45f, 0.45f, 0.90f, 0.80f), // HeaderHovered
            Vec4(0.53f, 0.53f, 0.87f, 0.80f), // HeaderActive
            Vec4(0.50f, 0.50f, 0.50f, 1.00f), // Column
            Vec4(0.70f, 0.60f, 0.60f, 1.00f), // ColumnHovered
            Vec4(0.90f, 0.70f, 0.70f, 1.00f), // ColumnActive
            Vec4(1.00f, 1.00f, 1.00f, 0.30f), // ResizeGrip
            Vec4(1.00f, 1.00f, 1.00f, 0.60f), // ResizeGripHovered
            Vec4(1.00f, 1.00f, 1.00f, 0.90f), // ResizeGripActive
            Vec4(0.50f, 0.50f, 0.90f, 0.50f), // CloseButton
            Vec4(0.70f, 0.70f, 0.90f, 0.60f), // CloseButtonHovered
            Vec4(0.70f, 0.70f, 0.70f, 1.00f), // CloseButtonActive
            Vec4(1.00f, 1.00f, 1.00f, 1.00f), // PlotLines
            Vec4(0.90f, 0.70f, 0.00f, 1.00f), // PlotLinesHovered
            Vec4(0.90f, 0.70f, 0.00f, 1.00f), // PlotHistogram
            Vec4(1.00f, 0.60f, 0.00f, 1.00f), // PlotHistogramHovered
            Vec4(0.00f, 0.00f, 1.00f, 0.35f), // TextSelectedBg
            Vec4(0.20f, 0.20f, 0.20f, 0.35f))   // ModalWindowDarkening
}