/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

import glm.vec._2.Vec2;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Load and rasterize multiple TTF fonts into a same texture.
 * Sharing a texture for multiple fonts allows us to reduce the number of draw calls during rendering.
 * We also add custom graphic data into the texture that serves for ImGui.
 * 1. (Optional) Call AddFont*** functions. If you don't call any, the default font will be loaded for you.
 * 2. Call GetTexDataAsAlpha8() or GetTexDataAsRGBA32() to build and retrieve pixels data.
 * 3. Upload the pixels data into a texture within your graphics system.
 * 4. Call SetTexID(my_tex_id); and pass the pointer/identifier to your texture. This value will be passed back to you
 * during rendering to identify the texture.
 * 5. Call ClearTexData() to free textures memory on the heap.
 * NB: If you use a 'glyph_ranges' array you need to make sure that your array persist up until the ImFont is cleared.
 * We only copy the pointer, not the data.
 *
 * @author GBarbieri
 */
public class FontAtlas {

    // Members
    // (Access texture data via GetTexData*() calls which will setup a default font for you.)
    /**
     * User data to refer to the texture once it has been uploaded to user's graphic systems. It ia passed back to you
     * during rendering.
     */
    int texId = 0;

    /**
     * 1 component per pixel, each component is unsigned 8-bit. Total size = TexWidth * TexHeight.
     */
    byte texPixelsAlpha8 = 0;

    /**
     * 4 component per pixel, each component is unsigned 8-bit. Total size = TexWidth * TexHeight * 4.
     */
    int texPixelsRGBA32 = 0;

    /**
     * Texture width calculated during Build().
     */
    int texWidth = 0;

    /**
     * Texture height calculated during Build().
     */
    int texHeight = 0;

    /**
     * Texture width desired by user before Build(). Must be a power-of-two. If have many glyphs your graphics API have
     * texture size restrictions you may want to increase texture width to decrease height.
     */
    int texDesiredWidth = 0;

    /**
     * Texture coordinates to a white pixel.
     */
    Vec2 texUvWhitePixel = new Vec2(0);

    /**
     * Hold all the fonts returned by AddFont*. Fonts[0] is the default font upon calling ImGui::NewFrame(), use
     * ImGui::PushFont()/PopFont() to change the current font.
     */
    ArrayList<Font> fonts = new ArrayList<>();

    //
    // Private ------------------------------------------------------------------------------------------------------------
    //
    /**
     * Internal data.
     */
    private ArrayList<FontConfig> configData = new ArrayList<>();

    public void getTexDataAsRGBA32(ByteBuffer pixels, int width, int height, int bytesPerPixel) {

        // Convert to RGBA32 format on demand
        // Although it is likely to be the most commonly used format, our font rendering is 1 channel / 8 bpp
        if (texPixelsRGBA32 == 0) {

        }
    }

    public void getTexDataAsAlpha8(ByteBuffer pixels, int width, int height, int bytesPerPixel) {

        // Build atlas on demand
        if (texPixelsAlpha8 == 0) {

            if (configData.isEmpty()) {

            }
        }
    }

    private void addFontDefault() {
        addFontDefault(null);
    }

    private void addFontDefault(FontConfig fontCfgTemplate) {

        FontConfig fontCfg = fontCfgTemplate != null ? fontCfgTemplate : new FontConfig();

        if (fontCfgTemplate == null) {

            fontCfg.oversampleH = 1;
            fontCfg.oversampleV = 1;
            fontCfg.pixelSnapH = true;
        }
        if (fontCfg.name.isEmpty()) {
            fontCfg.name = "<default>";
        }
    }
}
