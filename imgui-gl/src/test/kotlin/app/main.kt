package app

import app.tests.registerTests
import engine.core.*
import engine.osIsDebuggerPresent
import glm_.parseInt
import imgui.ConfigFlag
import imgui.IMGUI_ENABLE_TEST_ENGINE
import imgui.ImGui
import imgui.api.gImGui
import imgui.classes.Context
import imgui.or
import org.lwjgl.system.Configuration
import uno.kotlin.parseInt
import kotlin.system.exitProcess

/*
 dear imgui - Standalone GUI/command-line app for Test Engine
 If you are new to dear imgui, see examples/README.txt and documentation at the top of imgui.cpp.

 Interactive mode, e.g.
   main.exe [tests]
   main.exe -gui -fileopener ..\..\tools\win32_open_with_sublime.cmd -slow
   main.exe -gui -fileopener ..\..\tools\win32_open_with_sublime.cmd -nothrottle

 Command-line mode, e.g.
   main.exe -nogui -v -nopause
   main.exe -nogui -nopause perf_
*/


//-------------------------------------------------------------------------
// Test Application
//-------------------------------------------------------------------------

val app = TestApp

fun main(args: Array<String>) {

    Configuration.DEBUG.set(true)
    Configuration.DEBUG_MEMORY_ALLOCATOR.set(true)

    IMGUI_ENABLE_TEST_ENGINE = true

//    #ifdef CMDLINE_ARGS
//        if (argc == 1)
//        {
//            printf("# [exe] %s\n", CMDLINE_ARGS);
//            ImParseSplitCommandLine(&argc, (const char***)&argv, CMDLINE_ARGS);
//            if (!ParseCommandLineOptions(argc, argv))
//                return ImGuiTestAppErrorCode_CommandLineError;
//            free(argv);
//        }
//        else
//    #endif
//    {
    if (!parseCommandLineOptions(args))
        exitProcess(TestAppErrorCode.CommandLineError.ordinal)
//    }
//    argv = NULL;

    // Default verbose level differs whether we are in in GUI or Command-Line mode
    if (app.optVerboseLevel == TestVerboseLevel.COUNT)
        app.optVerboseLevel = if (app.optGUI) TestVerboseLevel.Debug else TestVerboseLevel.Silent
    if (app.optVerboseLevelOnError == TestVerboseLevel.COUNT)
        app.optVerboseLevelOnError = if (app.optGUI) TestVerboseLevel.Debug else TestVerboseLevel.Debug

    // Setup Dear ImGui binding
    val ctx = Context()
    ImGui.styleColorsDark()
    val io = ImGui.io.apply {
        iniFilename = "imgui.ini"
        configFlags = configFlags or ConfigFlag.NavEnableKeyboard  // Enable Keyboard Controls
    }
    //ImGuiStyle& style = ImGui::GetStyle();
    //style.Colors[ImGuiCol_Border] = style.Colors[ImGuiCol_BorderShadow] = ImVec4(1.0f, 0, 0, 1.0f);
    //style.FrameBorderSize = 1.0f;
    //style.FrameRounding = 5.0f;
//    #ifdef IMGUI_HAS_VIEWPORT
//    //io.ConfigFlags |= ImGuiConfigFlags_ViewportsEnable;
//    #endif
//    #ifdef IMGUI_HAS_DOCK
//            io.ConfigFlags | = ImGuiConfigFlags_DockingEnable
//    //io.ConfigDockingTabBarOnSingleWindows = true;
//    #endif

    // Load Fonts
    loadFonts()

    // Create TestEngine context
    assert(app.testEngine == null)
    val engine = testEngine_createContext(gImGui!!).also { app.testEngine = it }

    // Apply options
    val testIo = engine.io.apply {
        configRunWithGui = app.optGUI
        configRunFast = app.optFast
        configVerboseLevel = app.optVerboseLevel
        configVerboseLevelOnError = app.optVerboseLevelOnError
        configNoThrottle = app.optNoThrottle
        perfStressAmount = app.optStressAmount
        if (!app.optGUI && osIsDebuggerPresent())
            configBreakOnError = true
//        srcFileOpenFunc = srcFileOpenerFunc TODO
//        #if defined(IMGUI_TESTS_BACKEND_WIN32_DX11) || defined(IMGUI_TESTS_BACKEND_SDL_GL3) || defined(IMGUI_TESTS_BACKEND_GLFW_GL3)
        screenCaptureFunc = when {
            app.optGUI -> captureFramebufferScreenshot
            else -> /* #endif */ captureScreenshotNull
        }
    }
    // Set up TestEngine context
    engine.registerTests()
//    engine.calcSourceLineEnds()

    // Non-interactive mode queue all tests by default
    if (!app.optGUI && app.testsToRun.isEmpty())
        app.testsToRun += "tests"

    // Queue requested tests
    // FIXME: Maybe need some cleanup to not hard-coded groups.
    for (testSpec_ in app.testsToRun)
        when(testSpec_) {
        "tests" -> app.testEngine!!.queueTests(TestGroup.Tests, runFlags = TestRunFlag.CommandLine.i)
        "perf" -> app.testEngine!!.queueTests(TestGroup.Perf, runFlags = TestRunFlag.CommandLine.i)
        else -> {
            val testSpec = testSpec_.takeIf { testSpec_ != "all" }
            for (group in 0 until TestGroup.COUNT.i)
                app.testEngine!!.queueTests(TestGroup(group), testSpec, runFlags = TestRunFlag.CommandLine.i)
        }
    }
    app.testsToRun.clear()

    // Branch name stored in annotation field by default
    // FIXME-TESTS: Obtain from git? maybe pipe from a batch-file?
//    #if defined(IMGUI_HAS_DOCK)
//    strcpy(testIo.PerfAnnotation, "docking")
//    #elif defined (IMGUI_HAS_TABLE)
//    strcpy(testIo.PerfAnnotation, "tables")
//    #else
    testIo.perfAnnotation = "master"
//    #endif

    // Run
    if (app.optGUI)
        mainLoop()
    else
        mainLoopNull()

    // Print results
    val (countTested, countSuccess) = engine.result
    engine.printResultSummary()
    val errorCode = if (countTested != countSuccess) TestAppErrorCode.TestFailed else TestAppErrorCode.Success

    // Shutdown
    // We shutdown the Dear ImGui context _before_ the test engine context, so .ini data may be saved.
    ctx.destroy()
    engine.shutdownContext()

//    if (app.optFileOpener)
//        free(g_App.OptFileOpener)

    if (app.optPauseOnExit && !app.optGUI) {
        println("Press Enter to exit.")
        System.`in`.read()
    }

    exitProcess(errorCode.ordinal)
}

fun parseCommandLineOptions(args: Array<String>): Boolean {
    var n = 0
    while (n < args.size) {
        val arg = args[n++]
        if (arg[0] == '-')
            when (arg) {
                // Command-line option
                "-v" -> {
                    app.optVerboseLevel = TestVerboseLevel.Info
                    app.optVerboseLevelOnError = TestVerboseLevel.Debug
                }
                "-gui" -> app.optGUI = true
                "-nogui" -> app.optGUI = false
                "-fast" -> {
                    app.optFast = true
                    app.optNoThrottle = true
                }
                "-slow" -> {
                    app.optFast = false
                    app.optNoThrottle = false
                }
                "-nothrottle" -> app.optNoThrottle = true
                "-nopause" -> app.optPauseOnExit = false
                "-stressamount" -> if (n < args.size) app.optStressAmount = args[n++].parseInt()
//            "-fileopener") == 0 && n + 1 < argc) {
//                g_App.OptFileOpener = strdup(argv[n + 1])
//                ImPathFixSeparatorsForCurrentOS(g_App.OptFileOpener)
//                n++
//            }
                else -> when {
                    arg.startsWith("-v") && arg[2] >= '0' && arg[2] <= '5' -> app.optVerboseLevel = TestVerboseLevel(arg[2].parseInt())
                    arg.startsWith("-ve") && arg[3] >= '0' && arg[3] <= '5' -> app.optVerboseLevelOnError = TestVerboseLevel(arg[3].parseInt())
                    else -> {
                        println("""
                            Syntax: .. <options> [tests]
                            Options:
                                -h                       : show command-line help.
                                -v                       : verbose mode (same as -v2 -ve4)
                                -v0/-v1/-v2/-v3/-v4      : verbose level [v0: silent, v1: errors, v2: warnings: v3: info, v4: debug]
                                -ve0/-ve1/-ve2/-ve3/-ve4 : verbose level for failing tests [v0: silent, v1: errors, v2: warnings: v3: info, v4: debug]
                                -gui/-nogui              : enable interactive mode.
                                -slow                    : run automation at feeble human speed.
                                -nothrottle              : run GUI app without throlling/vsync by default.
                                -nopause                 : don't pause application on exit.
                                -stressamount <int>      : set performance test duration multiplier (default: 5)
                                -fileopener <file>       : provide a bat/cmd/shell script to open source file.
                            Tests:
                                all/tests/perf           : queue by groups: all, only tests, only performance benchmarks.
                                [pattern]                : queue all tests containing the word [pattern].
                                """)
                        return false
                    }
                }
            }
        else // Add tests
            app.testsToRun += arg
    }
    return true
}

fun loadFonts() {
    val io = ImGui.io
    io.fonts.addFontDefault()
    //ImFontConfig cfg;
    //cfg.RasterizerMultiply = 1.1f;

    // Find font directory
    io.fonts.addFontFromFileTTF("fonts/NotoSans-Regular.ttf", 16f)
    io.fonts.addFontFromFileTTF("fonts/Roboto-Medium.ttf", 16f)
    //io.Fonts->AddFontFromFileTTF(Str64f("%s/%s", base_font_dir.c_str(), "RobotoMono-Regular.ttf").c_str(), 16.0f, &cfg);
    //io.Fonts->AddFontFromFileTTF(Str64f("%s/%s", base_font_dir.c_str(), "Cousine-Regular.ttf").c_str(), 15.0f);
    //io.Fonts->AddFontFromFileTTF(Str64f("%s/%s", base_font_dir.c_str(), "DroidSans.ttf").c_str(), 16.0f);
    //io.Fonts->AddFontFromFileTTF(Str64f("%s/%s", base_font_dir.c_str(), "ProggyTiny.ttf").c_str(), 10.0f);
    //IM_ASSERT(font != NULL);
}


// Source file opener
//static void SrcFileOpenerFunc(const char* filename, int line, void*)
//{
//    if (!g_App.OptFileOpener) {
//        fprintf(stderr, "Executable needs to be called with a -fileopener argument!\n")
//        return
//    }
//
//    ImGuiTextBuffer cmd_line
//            cmd_line.appendf("%s %s %d", g_App.OptFileOpener, filename, line)
//    printf("Calling: '%s'\n", cmd_line.c_str())
//    bool ret = ImOsCreateProcess (cmd_line.c_str())
//    if (!ret)
//        fprintf(stderr, "Error creating process!\n")
//}

// Return value for main()
enum class TestAppErrorCode { Success, CommandLineError, TestFailed }


