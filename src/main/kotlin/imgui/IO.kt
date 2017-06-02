package imgui

import glm.vec2.Vec2

/** This is where your app communicate with ImGui. Access via ImGui::GetIO().
 *  Read 'Programmer guide' section in .cpp file for general usage. */
object IO {

    //------------------------------------------------------------------
    // Settings (fill once)
    //------------------------------------------------------------------

    /** Display size, in pixels. For clamping windows positions.    */
    var displaySize = Vec2(-1f)
    /** Time elapsed since last frame, in seconds.  */
    var deltaTime = 1.0f / 60.0f
    /** Maximum time between saving positions/sizes to .ini file, in seconds.   */
    var iniSavingRate = 5.0f
    /** Path to .ini file. NULL to disable .ini saving. */
    var iniFilename = "imgui.ini"
    /** Path to .log file (default parameter to ImGui::LogToFile when no file is specified).    */
    var logFilename = "imgui_log.txt"
    /** Time for a double-click, in seconds.    */
    var mouseDoubleClickTime = 0.30f
    /** Distance threshold to stay in to validate a double-click, in pixels.    */
    var mouseDoubleClickMaxDist = 6.0f
    /** Distance threshold before considering we are dragging   */
    var mouseDragThreshold = 6.0f
    /** Map of indices into the KeysDown[512] entries array */
    var keyMap = IntArray(Key_.COUNT.i, { -1 })
    /** When holding a key/button, time before it starts repeating, in seconds (for buttons in Repeat mode, etc.).  */
    var keyRepeatDelay = 0.250f
    /** When holding a key/button, rate at which it repeats, in seconds.    */
    var keyRepeatRate = 0.050f

//    void*         UserData;                 // = NULL               // Store your own data for retrieval by callbacks.

    /** Load and assemble one or more fonts into a single tightly packed texture. Output to Fonts array.    */
    val fonts = FontAtlas()
    /** Global scale all fonts  */
    var fontGlobalScale = 1.0f
    /** Allow user scaling text of individual window with CTRL+Wheel.   */
    var fontAllowUserScaling = false
//    ImFont*       FontDefault;              // = NULL               // Font to use on NewFrame(). Use NULL to uses Fonts->Fonts[0].
    /** For retina display or other situations where window coordinates are different from framebuffer coordinates.
     *  User storage only, presently not used by ImGui. */
    var displayFramebufferScale = Vec2(1.0f)
    /** If you use DisplaySize as a virtual space larger than your screen, set DisplayVisibleMin/Max to the visible
     *  area.   */
    var displayVisibleMin = Vec2()
    /** If the values are the same, we defaults to Min=(0.0f) and Max=DisplaySize   */
    var displayVisibleMax = Vec2()

    // Advanced/subtle behaviors

    /** OS X style: Text editing cursor movement using Alt instead of Ctrl, Shortcuts using Cmd/Super instead of Ctrl,
     *  Line/Text Start and End using Cmd+Arrows instead of Home/End, Double click selects by word instead of selecting
     *  whole text, Multi-selection in lists uses Cmd/Super instead of Ctrl */
    var osxBehaviors = false

    //------------------------------------------------------------------
    // User Functions
    //------------------------------------------------------------------

    /** Rendering function, will be called in Render().
     *  Alternatively you can keep this to NULL and call GetDrawData() after Render() to get the same pointer.
     *  See example applications if you are unsure of how to implement this.    */
    var renderDrawListsFn = { data: DrawData -> Unit }

    // Optional: access OS clipboard
    // (default to use native Win32 clipboard on Windows, otherwise uses a private clipboard. Override to access OS clipboard on other architectures)
//    const char* (*GetClipboardTextFn)(void* user_data);
//    void        (*SetClipboardTextFn)(void* user_data, const char* text);
//    void*       ClipboardUserData;
}

operator fun IntArray.set(index: Key_, value: Int) {
    this[index.i] = value
}
