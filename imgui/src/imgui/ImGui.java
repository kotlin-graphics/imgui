/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

import imgui.internal.Context;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

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
        return context.io;
    }

    public static void newFrame() {

        // Check user data
        //
        // Need a positive DeltaTime (zero is tolerated but will cause some timing issues)
        assert (context.io.deltaTime >= 0.0f);
        assert (context.io.displaySize.x >= 0.0f && context.getIo().displaySize.y >= 0.0f);
        // Font Atlas not created. Did you call io.Fonts->GetTexDataAsRGBA32 / GetTexDataAsAlpha8 ?
        //IM_ASSERT(g.IO.Fonts->Fonts.Size > 0);
        // Font Atlas not created. Did you call io.Fonts->GetTexDataAsRGBA32 / GetTexDataAsAlpha8 ?
        //IM_ASSERT(g.IO.Fonts->Fonts[0]->IsLoaded());
        // Invalid style setting
        assert (context.style.curveTessellationTol > 0.0f);

        if (!context.initialized) {

            // Initialize on first frame
            //g.LogClipboard = (ImGuiTextBuffer*)ImGui::MemAlloc(sizeof(ImGuiTextBuffer));
            //IM_PLACEMENT_NEW(g.LogClipboard) ImGuiTextBuffer();
            assert (context.settings.isEmpty());
        }
    }

    /**
     * Zero-tolerance, poor-man .ini parsing
     * FIXME: Write something less rubbish
     */
    private static void loadSettings() {

        String fileName = "c://lines.txt";

        //read file into stream, try-with-resources
        try (Stream<String> stream = Files.lines(Paths.get(context.io.iniFilename))) {

            stream.forEach(System.out::println);

        } catch (IOException e) {
        }
        switch(string) {
            
            case "Pos":
                settings.pos = value;
                break;
                
            case "Size":
                settings.size = value;
                break;
                
            case "Collapsed":
                settings.collapsed = value;
                break;
        }
    }
}
