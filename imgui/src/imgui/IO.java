/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

import glm.vec._2.Vec2;
import java.util.ArrayList;

/**
 *
 * @author GBarbieri
 */
public class IO {

    /**
     * Display size, in pixels. For clamping windows positions.
     */
    Vec2 displaySize;
    /**
     * Time elapsed since last frame, in seconds.
     */
    float deltaTime = 1.0f / 60.0f;
    /**
     * Maximum time between saving positions/sizes to .ini file, in seconds.
     */
    float iniSavingRate = 5.0f;
    /**
     * Path to .ini file. NULL to disable .ini saving.
     */
    String iniFilename = "imgui.ini";
    /**
     * Path to .log file (default parameter to ImGui::LogToFile when no file is specified).
     */
    String logFilename = "imgui_log.txt";
    /**
     * Time for a double-click, in seconds.
     */
    float mouseDoubleClickTime = 0.30f;
    /**
     * Distance threshold to stay in to validate a double-click, in pixels.
     */
    float mouseDoubleClickMaxDist = 6.0f;
    /**
     * Distance threshold before considering we are dragging.
     */
    float mouseDragThreshold = 6.0f;
    /**
     * Map of indices into the KeysDown[512] entries array.
     */
    int[] keyMap = new int[Key.COUNT];
    /**
     * When holding a key/button, time before it starts repeating, in seconds (for buttons in Repeat mode, etc.).
     */
    float keyRepeatDelay = 0.250f;
    /**
     * When holding a key/button, rate at which it repeats, in seconds.
     */
    float keyRepeatRate = 0.020f;

    // void*         UserData;
    /**
     * Load and assemble one or more fonts into a single tightly packed texture. Output to Fonts array.
     */
    ArrayList<FontAtlas> fonts = new ArrayList<>();
    /**
     * Global scale all fonts.
     */
    float fontGlobalScale = 1.0f;
    /**
     * Allow user scaling text of individual window with CTRL+Wheel.
     */
    boolean fontAllowUserScaling = false;
    /**
     * For retina display or other situations where window coordinates are different from framebuffer coordinates. 
     * User storage only, presently not used by ImGui.
     */
    Vec2 displayFramebufferScale = new Vec2(1.0f);
    /**
     * If you use DisplaySize as a virtual space larger than your screen, set DisplayVisibleMin/Max to the visible area.
     */
    Vec2 displayVisibleMin = new Vec2(0.0f);
    /**
     * If the values are the same, we defaults to Min=(0.0f) and Max=DisplaySize.
     */
    Vec2 displayVisibleMax = new Vec2(0.0f);
}
