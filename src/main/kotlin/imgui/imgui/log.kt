package imgui.imgui

/** Logging: all text output from interface is redirected to tty/file/clipboard. By default, tree nodes are
 *  automatically opened during logging.    */
interface imgui_log {

//    IMGUI_API void          LogToTTY(int max_depth = -1);                                       // start logging to tty
//    IMGUI_API void          LogToFile(int max_depth = -1, const char* filename = NULL);         // start logging to file
//    IMGUI_API void          LogToClipboard(int max_depth = -1);                                 // start logging to OS clipboard

    /** stop logging (close file, etc.) */
    fun logFinish():Nothing = TODO()
//    IMGUI_API void          LogButtons();                                                       // helper to display buttons for logging to tty/file/clipboard
//    IMGUI_API void          LogText(const char* fmt, ...) IM_PRINTFARGS(1);                     // pass text data straight to log (without being displayed)
}