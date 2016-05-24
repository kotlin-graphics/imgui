/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

import glm.vec._2.Vec2;
import glm.vec._4.Vec4;
import imgui.internal.Align;

/**
 * User facing structures.
 * 
 * @author GBarbieri
 */
public class Style {

    /**
     * Global alpha applies to everything in ImGui.
     */
    float alpha = 1.0f;

    /**
     * Padding within a window.
     */
    Vec2 windowPadding = new Vec2(8);

    /**
     * Minimum window size.
     */
    Vec2 windowMinSize = new Vec2(32);

    /**
     * Radius of window corners rounding. Set to 0.0f to have rectangular windows.
     */
    float windowRounding = 9.0f;

    /**
     * Alignment for title bar text.
     */
    int windowTitleAlign = Align.Left;

    /**
     * Radius of child window corners rounding. Set to 0.0f to have rectangular windows.
     */
    float childWindowRounding = 0.0f;

    /**
     * Padding within a framed rectangle (used by most widgets).
     */
    Vec2 framePadding = new Vec2(4, 3);

    /**
     * Radius of frame corners rounding. Set to 0.0f to have rectangular frame (used by most widgets).
     */
    float frameRounding = 0.0f;

    /**
     * Horizontal and vertical spacing between widgets/lines.
     */
    Vec2 itemSpacing = new Vec2(8, 4);

    /**
     * Horizontal and vertical spacing between within elements of a composed widget (e.g. a slider and its label).
     */
    Vec2 itemInnerSpacing = new Vec2(4);

    /**
     * Expand reactive bounding box for touch-based system where touch position is not accurate enough. Unfortunately we
     * don't sort widgets so priority on overlap will always be given to the first widget. So don't grow this too much!
     */
    Vec2 touchExtraPadding = new Vec2(0);

    /**
     * Horizontal indentation when e.g. entering a tree node. Generally == (FontSize + FramePadding.x*2).
     */
    float indentSpacing = 21.0f;

    /**
     * Minimum horizontal spacing between two columns.
     */
    float columnsMinSpacing = 6.0f;

    /**
     * Width of the vertical scrollbar, Height of the horizontal scrollbar.
     */
    float scrollbarSize = 16.0f;

    /**
     * Radius of grab corners for scrollbar.
     */
    float scrollbarRounding = 9.0f;

    /**
     * Minimum width/height of a grab box for slider/scrollbar.
     */
    float grabMinSize = 10.0f;

    /**
     * Radius of grabs corners rounding. Set to 0.0f to have rectangular slider grabs.
     */
    float grabRounding = 0.0f;

    /**
     * Window positions are clamped to be visible within the display area by at least this amount. Only covers regular
     * windows.
     */
    Vec2 displayWindowPadding = new Vec2(22);

    /**
     * If you cannot see the edge of your screen (e.g. on a TV) increase the safe area padding. Covers popups/tooltips as
     * well regular windows.
     */
    Vec2 displaySafeAreaPadding = new Vec2(4);

    /**
     * Enable anti-aliasing on lines/borders. Disable if you are really tight on CPU/GPU.
     */
    boolean antiAliasedLines = true;

    /**
     * Enable anti-aliasing on filled shapes (rounded rectangles, circles, etc.)
     */
    boolean antiAliasedShapes = true;

    /**
     * Tessellation tolerance. Decrease for highly tessellated curves (higher quality, more polygons), increase to reduce
     * quality.
     */
    float curveTessellationTol = 1.25f;

    Vec4[] colors = {
        new Vec4(0.90f, 0.90f, 0.90f, 1.00f),
        new Vec4(0.60f, 0.60f, 0.60f, 1.00f),
        new Vec4(0.00f, 0.00f, 0.00f, 0.70f),
        new Vec4(0.00f, 0.00f, 0.00f, 0.00f),
        new Vec4(0.05f, 0.05f, 0.10f, 0.90f),
        new Vec4(0.70f, 0.70f, 0.70f, 0.65f),
        new Vec4(0.00f, 0.00f, 0.00f, 0.00f),
        // Background of checkbox, radio button, plot, slider, text input
        new Vec4(0.80f, 0.80f, 0.80f, 0.30f),
        new Vec4(0.90f, 0.80f, 0.80f, 0.40f),
        new Vec4(0.90f, 0.65f, 0.65f, 0.45f),
        new Vec4(0.27f, 0.27f, 0.54f, 0.83f),
        new Vec4(0.40f, 0.40f, 0.80f, 0.20f),
        new Vec4(0.32f, 0.32f, 0.63f, 0.87f),
        new Vec4(0.40f, 0.40f, 0.55f, 0.80f),
        new Vec4(0.20f, 0.25f, 0.30f, 0.60f),
        new Vec4(0.40f, 0.40f, 0.80f, 0.30f),
        new Vec4(0.40f, 0.40f, 0.80f, 0.40f),
        new Vec4(0.80f, 0.50f, 0.50f, 0.40f),
        new Vec4(0.20f, 0.20f, 0.20f, 0.99f),
        new Vec4(0.90f, 0.90f, 0.90f, 0.50f),
        new Vec4(1.00f, 1.00f, 1.00f, 0.30f),
        new Vec4(0.80f, 0.50f, 0.50f, 1.00f),
        new Vec4(0.67f, 0.40f, 0.40f, 0.60f),
        new Vec4(0.67f, 0.40f, 0.40f, 1.00f),
        new Vec4(0.80f, 0.50f, 0.50f, 1.00f),
        new Vec4(0.40f, 0.40f, 0.90f, 0.45f),
        new Vec4(0.45f, 0.45f, 0.90f, 0.80f),
        new Vec4(0.53f, 0.53f, 0.87f, 0.80f),
        new Vec4(0.50f, 0.50f, 0.50f, 1.00f),
        new Vec4(0.70f, 0.60f, 0.60f, 1.00f),
        new Vec4(0.90f, 0.70f, 0.70f, 1.00f),
        new Vec4(1.00f, 1.00f, 1.00f, 0.30f),
        new Vec4(1.00f, 1.00f, 1.00f, 0.60f),
        new Vec4(1.00f, 1.00f, 1.00f, 0.90f),
        new Vec4(0.50f, 0.50f, 0.90f, 0.50f),
        new Vec4(0.70f, 0.70f, 0.90f, 0.60f),
        new Vec4(0.70f, 0.70f, 0.70f, 1.00f),
        new Vec4(1.00f, 1.00f, 1.00f, 1.00f),
        new Vec4(0.90f, 0.70f, 0.00f, 1.00f),
        new Vec4(0.90f, 0.70f, 0.00f, 1.00f),
        new Vec4(1.00f, 0.60f, 0.00f, 1.00f),
        new Vec4(0.00f, 0.00f, 1.00f, 0.35f),
        new Vec4(0.20f, 0.20f, 0.20f, 0.35f)};
}
