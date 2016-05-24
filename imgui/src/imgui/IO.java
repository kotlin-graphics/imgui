/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package imgui;

import glm.vec._2.Vec2;

/**
 * This is where your app communicate with ImGui. Access via ImGui::GetIO().
 * Read 'Programmer guide' section in .cpp file for general usage.
 *
 * @author GBarbieri
 */
public class IO {

    /**
     * Display size, in pixels. For clamping windows positions.
     */
    public Vec2 displaySize = new Vec2(-1.0f);

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
    public int[] keyMap = new int[Key.COUNT];

    /**
     * When holding a key/button, time before it starts repeating, in seconds (for buttons in Repeat mode, etc.).
     */
    float keyRepeatDelay = 0.250f;

    /**
     * When holding a key/button, rate at which it repeats, in seconds.
     */
    float keyRepeatRate = 0.050f;

    // void*         UserData=null;
    /**
     * Load and assemble one or more fonts into a single tightly packed texture. Output to Fonts array.
     */
    FontAtlas[] fonts = {ImGui.defaultFontAtlas};

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
    public Vec2 displayFramebufferScale = new Vec2(1.0f);

    /**
     * If you use DisplaySize as a virtual space larger than your screen, set DisplayVisibleMin/Max to the visible area.
     */
    Vec2 displayVisibleMin = new Vec2(0.0f);

    /**
     * If the values are the same, we defaults to Min=(0.0f) and Max=DisplaySize.
     */
    Vec2 displayVisibleMax = new Vec2(0.0f);

    // Advanced/subtle behaviors -----------------------------------------------------------------------------------------
    //
    /**
     * OS X style: Text editing cursor movement using Alt instead of Ctrl.
     */
    boolean wordMovementUsesAltKey = true;

    /**
     * Shortcuts using Cmd/Super instead of Ctrl.
     */
    boolean shortcutsUseSuperKey = true;

    /**
     * OS X style: Double click selects by word instead of selecting whole text.
     */
    boolean doubleClickSelectsWord = true;

    /**
     * OS X style: Multi-selection in lists uses Cmd/Super instead of Ctrl [unused yet].
     */
    boolean multiSelectUsesSuperKey = true;

    //--------------------------------------------------------------------------------------------------------------------
    // Input - Fill before calling NewFrame()
    //--------------------------------------------------------------------------------------------------------------------
    /**
     * Mouse position, in pixels (set to -1,-1 if no mouse / on another screen, etc.)
     */
    Vec2 mousePos = new Vec2(-1);

    /**
     * Mouse buttons: left, right, middle + extras. ImGui itself mostly only uses left button (BeginPopupContext** are
     * using right button). Others buttons allows us to track if the mouse is being used by your application + available
     * to user as a convenience via IsMouse** API.
     */
    boolean[] mouseDown = new boolean[5];

    /**
     * Mouse wheel: 1 unit scrolls about 5 lines text.
     */
    float mouseWheel;

    /**
     * Request ImGui to draw a mouse cursor for you (if you are on a platform without a mouse cursor).
     */
    boolean mouseDrawCursor;

    /**
     * Keyboard modifier pressed: Control.
     */
    boolean keyCtrl;

    /**
     * Keyboard modifier pressed: Shift.
     */
    boolean keyShift;

    /**
     * Keyboard modifier pressed: Alt.
     */
    boolean keyAlt;

    /**
     * Keyboard modifier pressed: Cmd/Super/Windows.
     */
    boolean keySuper;

    /**
     * Keyboard keys that are pressed (in whatever storage order you naturally have access to keyboard data).
     */
    boolean[] keysDown = new boolean[512];

    /**
     * List of characters input (translated by user from keypress+keyboard state). Fill using AddInputCharacter() helper.
     */
    short[] InputCharacters = new short[16 + 1];

    //--------------------------------------------------------------------------------------------------------------------
    // Output - Retrieve after calling NewFrame(), you can use them to discard inputs or hide them from the rest of your 
    // application
    //--------------------------------------------------------------------------------------------------------------------
    /**
     * Mouse is hovering a window or widget is active (= ImGui will use your mouse input).
     */
    boolean wantCaptureMouse;

    /**
     * Widget is active (= ImGui will use your keyboard input).
     */
    boolean wantCaptureKeyboard;

    /**
     * Some text input widget is active, which will read input characters from the InputCharacters array.
     */
    boolean wantTextInput;

    /**
     * Framerate estimation, in frame per second. Rolling average estimation based on IO.DeltaTime over 120 frames.
     */
    float framerate;

    /**
     * Number of active memory allocations.
     */
    int metricsAllocs;

    /**
     * Vertices output during last call to Render().
     */
    int metricsRenderVertices;

    /**
     * Indices output during last call to Render() = number of triangles * 3.
     */
    int metricsRenderIndices;

    /**
     * Number of visible windows (exclude child windows).
     */
    int metricsActiveWindows;

    //--------------------------------------------------------------------------------------------------------------------
    // [Internal] ImGui will maintain those fields for you
    //--------------------------------------------------------------------------------------------------------------------
    /**
     * Previous mouse position.
     */
    Vec2 mousePosPrev = new Vec2(-1);

    /**
     * Mouse delta. Note that this is zero if either current or previous position are negative to allow mouse
     * enabling/disabling.
     */
    Vec2 mouseDelta;

    /**
     * Mouse button went from !Down to Down.
     */
    boolean[] mouseClicked = new boolean[5];

    /**
     * Position at time of clicking.
     */
    Vec2[] mouseClickedPos = new Vec2[5];

    /**
     * Time of last click (used to figure out double-click).
     */
    float[] mouseClickedTime = new float[5];

    /**
     * Has mouse button been double-clicked?.
     */
    boolean[] mouseDoubleClicked = new boolean[5];

    /**
     * Mouse button went from Down to !Down.
     */
    boolean[] mouseReleased = new boolean[5];

    /**
     * Track if button was clicked inside a window. We don't request mouse capture from the application if click started
     * outside ImGui bounds.
     */
    boolean[] mouseDownOwned = new boolean[5];

    /**
     * Duration the mouse button has been down (0.0f == just clicked).
     */
    float[] mouseDownDuration = new float[5];

    /**
     * Previous time the mouse button has been down.
     */
    float[] mouseDownDurationPrev = new float[5];

    /**
     * Squared maximum distance of how much mouse has traveled from the click point.
     */
    float[] mouseDragMaxDistanceSqr = new float[5];

    /**
     * Duration the keyboard key has been down (0.0f == just pressed).
     */
    float[] keysDownDuration = new float[512];

    /**
     * Previous duration the key has been down.
     */
    float[] keysDownDurationPrev = new float[512];

    public IO() {
        for (int i = 0; i < mouseDownDuration.length; i++) {
            mouseDownDuration[i] = -1.0f;
            mouseDownDurationPrev[i] = -1.0f;
        }
        for (int i = 0; i < keysDownDuration.length; i++) {
            keysDownDuration[i] = -1.0f;
            keysDownDurationPrev[i] = -1.0f;
        }
        for (int i = 0; i < Key.COUNT; i++) {
            keyMap[i] = -1;
        }
    }
}
