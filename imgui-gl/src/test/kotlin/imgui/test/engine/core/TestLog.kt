package imgui.test.engine.core

//-------------------------------------------------------------------------
// ImGuiTestLog
//-------------------------------------------------------------------------

class TestLogLineInfo{
    var level = TestVerboseLevel.Silent
    var lineOffset = 0
}

class TestLog{
    var buffer = StringBuilder()
    val lineInfo = ArrayList<TestLogLineInfo>()
    val lineInfoError = ArrayList<TestLogLineInfo>()
    var cachedLinesPrintedToTTY = false

    fun clear()    {
        buffer.clear()
        lineInfo.clear()
        lineInfoError.clear()
        cachedLinesPrintedToTTY = false
    }

    fun updateLineOffsets(engineIo: TestEngineIO, level: TestVerboseLevel, start: String)    {
        assert(buffer.begin() <= start && start < Buffer.end())
        const char* p_begin = start
        const char* p_end = Buffer.end()
        const char* p = p_begin
        while (p < p_end)
        {
            const char* p_bol = p
            const char* p_eol = strchr(p, '\n')

            bool last_empty_line = (p_bol + 1 == p_end)

            if (!last_empty_line)
            {
                int offset = (int)(p_bol - Buffer.c_str())
                if (engine_io->ConfigVerboseLevel >= level)
                LineInfo.push_back({level, offset})
                if (engine_io->ConfigVerboseLevelOnError >= level)
                LineInfoError.push_back({level, offset})
            }
            p = p_eol ? p_eol + 1 : NULL
        }
    }
}