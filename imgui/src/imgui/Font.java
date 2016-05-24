/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

import glm.vec._2.Vec2;
import java.util.ArrayList;

/**
 * Font runtime data and rendering
 * ImFontAtlas automatically loads a default embedded font for you when you call GetTexDataAsAlpha8() or
 * GetTexDataAsRGBA32().
 *
 * @author GBarbieri
 */
public class Font {

    private class Glyph {

        short codepoint;

        float xAdvance;

        float x0, y0, x1, y1;

        /**
         * Texture coordinates.
         */
        float u0, v0, u1, v1;
    }

    // Members: Hot ~62/78 bytes --------------------------------------------------------------------------------------------
    //
    /**
     * Height of characters, set during loading (don't change after loading).
     */
    float fontSize = 0.0f;

    /**
     * Base font scale, multiplied by the per-window font scale which you can adjust with SetFontScale().
     */
    float scale = 1.0f;

    /**
     * Offset font rendering by xx pixels.
     */
    Vec2 displayOffset = new Vec2(0.0f, 1.0f);

    /**
     * All glyphs.
     */
    ArrayList<Glyph> glyphs;

    /**
     * Sparse. Glyphs->XAdvance in a directly indexable way (more cache-friendly, for CalcTextSize functions which are
     * often bottleneck in large UI).
     */
    ArrayList<Float> indexXAdvance;

    /**
     * Sparse. Index glyphs by Unicode code-point..
     */
    ArrayList<Short> indexLookup;

    /**
     * == FindGlyph(FontFallbackChar).
     */
    Glyph fallbackGlyph = null;

    /**
     * == FallbackGlyph->XAdvance.
     */
    float fallbackXAdvance = 0.0f;

    /**
     * = '?', Replacement glyph if one isn't found. Only set via SetFallbackChar().
     */
    char fallbackChar = '?';

    // Members: Cold ~18/26 bytes -----------------------------------------------------------------------------------------
    //
    /**
     * ~ 1, Number of ImFontConfig involved in creating this font. Bigger than 1 when merging multiple font sources into
     * one ImFont.
     */
    short configDataCount = 0;

    /**
     * Pointer within ContainerAtlas->ConfigData.
     */
    FontConfig configData = null;

    /**
     * What we has been loaded into.
     */
    FontAtlas containerAtlas = null;

    /**
     * Ascent: distance from top to bottom of e.g. 'A' [0..FontSize].
     */
    float ascent = 0.0f, descent = 0.0f;
}
