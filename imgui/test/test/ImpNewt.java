/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.GLBuffers;
import imgui.DrawData;
import imgui.IO;
import imgui.ImGui;
import imgui.Key;
import imgui.Render;
import java.nio.IntBuffer;

/**
 *
 * @author GBarbieri
 */
public class ImpNewt {

    // Data
    static GLWindow window = null;

    public static boolean init(GLWindow window) {

        ImpNewt.window = window;

        imgui.IO io = ImGui.getIO();

        // Keyboard mapping. ImGui will use those indices to peek into the io.KeyDown[] array.
        io.keyMap[Key.Tab] = KeyEvent.VK_TAB;
        io.keyMap[Key.LeftArrow] = KeyEvent.VK_LEFT;
        io.keyMap[Key.RightArrow] = KeyEvent.VK_RIGHT;
        io.keyMap[Key.UpArrow] = KeyEvent.VK_UP;
        io.keyMap[Key.DownArrow] = KeyEvent.VK_DOWN;
        io.keyMap[Key.PageUp] = KeyEvent.VK_PAGE_UP;
        io.keyMap[Key.PageDown] = KeyEvent.VK_PAGE_DOWN;
        io.keyMap[Key.Home] = KeyEvent.VK_HOME;
        io.keyMap[Key.End] = KeyEvent.VK_END;
        io.keyMap[Key.Delete] = KeyEvent.VK_DELETE;
        io.keyMap[Key.Backspace] = KeyEvent.VK_BACK_SPACE;
        io.keyMap[Key.Enter] = KeyEvent.VK_ENTER;
        io.keyMap[Key.Escape] = KeyEvent.VK_ESCAPE;
        io.keyMap[Key.A] = KeyEvent.VK_A;
        io.keyMap[Key.C] = KeyEvent.VK_C;
        io.keyMap[Key.V] = KeyEvent.VK_V;
        io.keyMap[Key.X] = KeyEvent.VK_X;
        io.keyMap[Key.Y] = KeyEvent.VK_Y;
        io.keyMap[Key.Z] = KeyEvent.VK_Z;
    }

    private static IntBuffer fontTexture = GLBuffers.newDirectIntBuffer(1);

    public static void newFrame() {

        if (fontTexture.get(0) == 0) {

        }
    }

    private static void createDeviceObjects() {

        // Build texture atlas
        IO io = 
    }

}
