/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

import glm.vec._2.Vec2;
import java.nio.ByteBuffer;

/**
 * ImFontAtlas.
 * 
 * @author GBarbieri
 */
public class FontConfig {

    /**
     * TTF data
     */
    ByteBuffer fontData = null;
    /**
     * TTF data size.
     */
    int fontDataSize = 0;

    /**
     * TTF data ownership taken by the container ImFontAtlas (will delete memory itself).
     */
    boolean fontDataOwnedByAtlas = true;

    /**
     * Index of font within TTF file.
     */
    int fontNo = 0;

    /**
     * Size in pixels for rasterizer.
     */
    float sizePixels = 0.0f;

    /**
     * Rasterize at higher quality for sub-pixel positioning. We don't use sub-pixel positions on the Y axis.
     */
    int oversampleH = 3, oversampleV = 1;

    /**
     * Align every character to pixel boundary (if enabled, set OversampleH/V to 1).
     */
    boolean pixelSnapH = false;

    /**
     * Extra spacing (in pixels) between glyphs.
     */
    Vec2 glyphExtraSpacing = new Vec2(0.0f);

    /**
     * Pointer to a user-provided list of Unicode range (2 value per range, values are inclusive, zero-terminated list).
     * THE ARRAY DATA NEEDS TO PERSIST AS LONG AS THE FONT IS ALIVE.
     */
    short glyphRanges = 0;

    /**
     * Merge into previous ImFont, so you can combine multiple inputs font into one ImFont (e.g. ASCII font + icons +
     * Japanese glyphs).
     */
    boolean mergeMode = false;

    /**
     * When merging (multiple ImFontInput for one ImFont), vertically center new glyphs instead of aligning their baseline.
     */
    boolean mergeGlyphCenterV = false;

    // [Internal] ---------------------------------------------------------------------------------------------------------
    //
    /**
     * Name (strictly for debugging).
     */
    String name = "";

    private Font dstFont = null;
}
