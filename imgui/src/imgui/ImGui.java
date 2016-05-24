/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

import imgui.internal.Context;

/**
 *
 * @author GBarbieri
 */
public class ImGui {

    // Default context, default font atlas.
// New contexts always point by default to this font atlas. It can be changed by reassigning the GetIO().Fonts variable.
    static Context defaultContext = new Context();
    static FontAtlas defaultFontAtlas = new FontAtlas();

    private static Context context = defaultContext;

    // Main
    public static IO getIO() {
        return context.getIo();
    }

}
