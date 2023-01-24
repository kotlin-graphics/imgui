package imgui.internal.api

import glm_.vec2.Vec2
import imgui.ImGui
import imgui.api.g
import imgui.internal.sections.LogType

/** Logging/Capture */
internal interface loggingCapture {

    /** -> BeginCapture() when we design v2 api, for now stay under the radar by using the old name. */
    /** Start logging/capturing text output */
    fun logBegin(type: LogType, autoOpenDepth: Int) {

        val window = g.currentWindow!!

        assert(!g.logEnabled && g.logFile == null && g.logBuffer.isEmpty())
        g.logEnabled = true
        g.logType = type
//        g.logNextPrefix = g.LogNextSuffix = NULL TODO
        g.logDepthRef = window.dc.treeDepth
        g.logDepthToExpand = autoOpenDepth.takeIf { it >= 0 } ?: g.logDepthToExpandDefault
        g.logLinePosY = Float.MAX_VALUE
        g.logLineFirstItem = true
    }

    /** Start logging/capturing to internal buffer */
    fun logToBuffer(autoOpenDepth: Int = -1) {
        if (g.logEnabled)
            return
        logBegin(LogType.Buffer, autoOpenDepth)
    }

    /** Internal version that takes a position to decide on newline placement and pad items according to their depth.
     *  We split text into individual lines to add current tree level padding
     *  FIXME: This code is a little complicated perhaps, considering simplifying the whole system. */
    fun logRenderedText(refPos: Vec2?, text: String, textEnd: Int = ImGui.findRenderedTextEnd(text)) { // TODO ByteArray?

        TODO()
        //        ImGuiContext& g = *GImGui;
        //        ImGuiWindow* window = g.CurrentWindow;
        //
        //        const char* prefix = g.LogNextPrefix;
        //        const char* suffix = g.LogNextSuffix;
        //        g.LogNextPrefix = g.LogNextSuffix = NULL;
        //
        //        if (!text_end)
        //            text_end = FindRenderedTextEnd(text, text_end);
        //
        //        const bool log_new_line = ref_pos && (ref_pos->y > g.LogLinePosY + g.Style.FramePadding.y + 1);
        //        if (ref_pos)
        //            g.LogLinePosY = ref_pos->y;
        //        if (log_new_line)
        //        {
        //            LogText(IM_NEWLINE);
        //            g.LogLineFirstItem = true;
        //        }
        //
        //        if (prefix)
        //            LogRenderedText(ref_pos, prefix, prefix + strlen(prefix)); // Calculate end ourself to ensure "##" are included here.
        //
        //        // Re-adjust padding if we have popped out of our starting depth
        //        if (g.LogDepthRef > window->DC.TreeDepth)
        //        g.LogDepthRef = window->DC.TreeDepth;
        //        const int tree_depth = (window->DC.TreeDepth - g.LogDepthRef);
        //
        //        const char* text_remaining = text;
        //        for (;;)
        //        {
        //            // Split the string. Each new line (after a '\n') is followed by indentation corresponding to the current depth of our log entry.
        //            // We don't add a trailing \n yet to allow a subsequent item on the same line to be captured.
        //            const char* line_start = text_remaining;
        //            const char* line_end = ImStreolRange(line_start, text_end);
        //            const bool is_last_line = (line_end == text_end);
        //            if (line_start != line_end || !is_last_line)
        //            {
        //                const int line_length = (int)(line_end - line_start);
        //                const int indentation = g.LogLineFirstItem ? tree_depth * 4 : 1;
        //                LogText("%*s%.*s", indentation, "", line_length, line_start);
        //                g.LogLineFirstItem = false;
        //                if (*line_end == '\n')
        //                {
        //                    LogText(IM_NEWLINE);
        //                    g.LogLineFirstItem = true;
        //                }
        //            }
        //            if (is_last_line)
        //                break;
        //            text_remaining = line_end + 1;
        //        }
        //
        //        if (suffix)
        //            LogRenderedText(ref_pos, suffix, suffix + strlen(suffix));
    }

    /** Important: doesn't copy underlying data, use carefully (prefix/suffix must be in scope at the time of the next LogRenderedText) */
    fun logSetNextTextDecoration(prefix: String, suffix: String) { // TODO check where this is called, missed some porting
        g.logNextPrefix = prefix
        g.logNextSuffix = suffix
    }
}