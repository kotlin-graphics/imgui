package imgui.imgui

/** Logging: all text output from interface is redirected to tty/file/clipboard. By default, tree nodes are
 *  automatically opened during logging.    */
interface imgui_logging {

//    IMGUI_API void          LogToTTY(int max_depth = -1);                                       // start logging to tty
//    IMGUI_API void          LogToFile(int max_depth = -1, const char* filename = NULL);         // start logging to file
//    IMGUI_API void          LogToClipboard(int max_depth = -1);                                 // start logging to OS clipboard

    /** stop logging (close file, etc.) */
    fun logFinish(): Nothing = TODO()
//    IMGUI_API void          LogButtons();                                                       // helper to display buttons for logging to tty/file/clipboard

    /** pass text data straight to log (without being displayed)    */
    fun logText(fmt: String, vararg args: Any) {
        TODO()
//        ImGuiContext& g = *GImGui;
//        if (!g.LogEnabled)
//            return;
//
//        va_list args;
//        va_start(args, fmt);
//        if (g.LogFile)
//        {
//            vfprintf(g.LogFile, fmt, args);
//        }
//        else
//        {
//            g.LogClipboard->appendv(fmt, args);
//        }
//        va_end(args);
    }
}