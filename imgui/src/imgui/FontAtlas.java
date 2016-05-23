/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

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

    
}
