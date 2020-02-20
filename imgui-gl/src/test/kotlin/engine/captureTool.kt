package engine

import gli_.has
import gli_.hasnt
import glm_.f
import glm_.i
import glm_.max
import glm_.min
import glm_.vec2.Vec2
import glm_.vec2.operators.minus
import imgui.api.gImGui
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.test.engine.context.TestContext
import imgui.test.engine.popDisabled
import imgui.test.engine.pushDisabled
import kool.set
import java.awt.image.BufferedImage
import java.io.File
import java.nio.ByteBuffer
import javax.imageio.ImageIO
import kotlin.reflect.KMutableProperty0
import imgui.WindowFlag as Wf

// Helper class for simple bitmap manipulation (not particularly efficient!)
class ImageBuf {
//    typedef unsigned int u32;

    var width = 0
    var height = 0
    var data: ByteBuffer? = null

    ~ImageBuf()
    { Clear(); }

    void Clear()                                           // Free allocated memory buffer if such exists.
    fun createEmpty(w: Int, h: Int) {                         // Reallocate buffer for pixel data, and zero it.
        width = w
        height = h
        data = ByteBuffer.allocate(width * height * 4)
    }

    fun saveFile(filename: String) {                    // Save pixel data to specified file.
        val image = BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
        image.raster.setDataElements(0, 0, width, height, data)
        ImageIO.write(image, filename, File(filename))
    }

    fun removeAlpha() {                                     // Clear alpha channel from all pixels.
        val data = data!!.asIntBuffer()
        var p = 0
        var n = width * height
        while (n-- > 0) {
            data[p] = data[p] or 0xFF000000.i
            p++
        }
    }

    fun blitSubImage(dstX: Int, dstY: Int, srcX: Int, srcY: Int, w: Int, h: Int, source: _root_ide_package_.engine.ImageBuf) {
//        assert(source && "Source image is null.");
        assert(dstX >= 0 && dstY >= 0) { "Destination coordinates can not be negative." }
        assert(srcX >= 0 && srcY >= 0) { "Source coordinates can not be negative." }
        assert(dstX + w <= width && dstY + h <= height) { "Destination image is too small." }
        assert(srcX + w <= source.width && srcY + h <= source.height) { "Source image is too small." }

        for (y in 0 until h)
            for (i in 0 until source.width * 4)
                data!![(dstY + y) * width + dstX + i] = source.data!![(srcY + y) * source.width + srcX]
    }
}

typealias ScreenCaptureFunc = (x: Int, y: Int, w: Int, h: Int, pixels: ByteBuffer, userData: Any?) -> Boolean

enum class CaptureFlag(val i: _root_ide_package_.engine.CaptureFlags) {
    None(0),      //
    StitchFullContents(1 shl 1), // Expand window to it's content size and capture its full height.
    IgnoreCaptureToolWindow(1 shl 2), // Current window will not appear in screenshots or helper UI.
    ExpandToIncludePopups(1 shl 3), // Expand capture area to automatically include visible popups and tooltips.
    Default(_root_ide_package_.engine.CaptureFlag.StitchFullContents.i or _root_ide_package_.engine.CaptureFlag.IgnoreCaptureToolWindow.i)
}

typealias CaptureFlags = Int

enum class CaptureToolState {
    None,                             // No capture in progress.
    PickingSingleWindow,              // CaptureWindowPicker() is selecting a window under mouse cursor.
    SelectRectStart,                  // Next mouse click will create selection rectangle.
    SelectRectUpdate,                 // Update selection rectangle until mouse is released.
    Capturing                         // Capture is in progress.
}

// Defines input and output arguments for capture process.
class CaptureArgs {
    // [Input]
    var inFlags: _root_ide_package_.engine.CaptureFlags = 0                    // Flags for customizing behavior of screenshot tool.
    val inCaptureWindows = ArrayList<Window>()               // Windows to capture. All other windows will be hidden. May be used with InCaptureRect to capture only some windows in specified rect.
    var inCaptureRect = Rect()                  // Screen rect to capture. Does not include padding.
    var inPadding = 10f              // Extra padding at the edges of the screenshot.

    // [Output]
    var outFileCounter = 0             // Counter which may be appended to file name when saving. By default counting starts from 1. When done this field holds number of saved files.
    var outImageBuf: _root_ide_package_.engine.ImageBuf? = null             // Output will be saved to image buffer if specified.
    var outImageFileTemplate = "" // Output will be saved to a file if OutImageBuf is NULL.

    // [Internal]
    internal var capturing = false             // FIXME-TESTS: ???
}

// Implements functionality for capturing images
class CaptureContext(
        var screenCaptureFunc: _root_ide_package_.engine.ScreenCaptureFunc? = null) {              // Graphics-backend-specific function that captures specified portion of framebuffer and writes RGBA data to `pixels` buffer.

    var userData: Any? = null                // Custom user pointer which is passed to ScreenCaptureFunc. (Optional)

    // [Internal]
    internal var captureRect = Rect()                   // Viewport rect that is being captured.
    internal var combinedWindowRectPos = Vec2()         // Top-left corner of region that covers all windows included in capture. This is not same as _CaptureRect.Min when capturing explicitly specified rect.
    internal var output: _root_ide_package_.engine.ImageBuf? = null                        // Output image buffer.
    internal var saveFileNameFinal = ""   // Final file name to which captured image will be saved.
    internal var chunkNo = 0                   // Number of chunk that is being captured when capture spans multiple frames.
    internal var frameNo = 0                   // Frame number during capture process that spans multiple frames.
    internal val windowBackupRects = ArrayList<Rect>()             // Backup window state that will be restored when screen capturing is done. Size and order matches windows of ImGuiCaptureArgs::InCaptureWindows.
    internal val windowBackupRectsWindows = ArrayList<Window>()      // Backup windows that will have their state restored. args->InCaptureWindows can not be used because popups may get closed during capture and no longer appear in that list.
    internal var displayWindowPaddingBackup = Vec2()   // Backup padding. We set it to {0, 0} during capture.
    internal var displaySafeAreaPaddingBackup = Vec2()  // Backup padding. We set it to {0, 0} during capture.

    // Capture a screenshot. If this function returns true then it should be called again with same arguments on the next frame.
    // Returns true when capture is in progress.
    fun captureScreenshot(args: _root_ide_package_.engine.CaptureArgs): Boolean {

        val g = _root_ide_package_.imgui.ImGui
        val io = g.io
//        IM_ASSERT(args != NULL);
        assert(screenCaptureFunc != null)
        assert(args.outImageBuf != null || args.outImageFileTemplate.isNotEmpty())

        val output = args.outImageBuf ?: output

        // Hide other windows so they can't be seen visible behind captured window
        for (window in g.windows) {
//            #ifdef IMGUI_HAS_VIEWPORT
//                if ((io.ConfigFlags & ImGuiConfigFlags_ViewportsEnable) && (args->InFlags & ImGuiCaptureToolFlags_StitchFullContents))
//            {
//                // FIXME-VIEWPORTS: Content stitching is not possible because window would get moved out of main viewport and detach from it. We need a way to force captured windows to remain in main viewport here.
//                return false
//            }
//            #endif
            if (window.flags has Wf._ChildWindow || window in args.inCaptureWindows)
                continue
            window.hidden = true
            window.hiddenFramesCannotSkipItems = 2
        }

        if (frameNo == 0) {
            // Initialize capture state.
            chunkNo = 0
            captureRect.put(Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
            windowBackupRects.clear()
            combinedWindowRectPos put Float.MAX_VALUE
            displayWindowPaddingBackup = g.style.displayWindowPadding
            displaySafeAreaPaddingBackup = g.style.displaySafeAreaPadding
            g.style.displayWindowPadding put 0                    // Allow window to be positioned fully outside
            g.style.displaySafeAreaPadding put 0                  // of visible viewport.
            args.capturing = true

            val isCapturingRect = args.inCaptureRect.width > 0 && args.inCaptureRect.height > 0
            if (isCapturingRect) {
                // Capture arbitrary rectangle. If any windows are specified in this mode only they will appear in captured region.
                captureRect put args.inCaptureRect
                if (args.inCaptureWindows.isEmpty()) {
                    // Gather all top level windows. We will need to move them in order to capture regions larger than viewport.
                    for (window in g.windows) {
                        // Child windows will be included by their parents.
                        if (window.parentWindow != null)
                            continue

                        if ((window.flags has Wf._Popup || window.flags has Wf._Tooltip) && args.inFlags hasnt _root_ide_package_.engine.CaptureFlag.ExpandToIncludePopups.i)
                            continue

                        args.inCaptureWindows += window
                    }
                }
            }

            // Save rectangle covering all windows and find top-left corner of combined rect which will be used to
            // translate this group of windows to top-left corner of the screen.
            for (window in args.inCaptureWindows) {
                windowBackupRects += window.rect()
                windowBackupRectsWindows += window
                combinedWindowRectPos.put(combinedWindowRectPos.x min window.pos.x, combinedWindowRectPos.y min window.pos.y)
            }

            if (args.inFlags has _root_ide_package_.engine.CaptureFlag.StitchFullContents.i) {
                if (isCapturingRect)
                    _root_ide_package_.imgui.ImGui.logText("Capture Tool: capture of full window contents is not possible when capturing specified rect.")
                else if (args.inCaptureWindows.size != 1)
                    _root_ide_package_.imgui.ImGui.logText("Capture Tool: capture of full window contents is not possible when capturing more than one window.")
                else {
                    // Resize window to it's contents and capture it's entire width/height. However if window is bigger than
                    // it's contents - keep original size.
                    val window = args.inCaptureWindows.first()
                    val fullSize = Vec2(
                            window.sizeFull.x max (window.contentSize.x + window.windowPadding.y * 2),
                            window.sizeFull.y max (window.contentSize.y + window.windowPadding.y * 2 + window.titleBarHeight + window.menuBarHeight))
                    window.setSize(fullSize)
                }
            }
        } else if (frameNo == 1) {
            // Move group of windows so combined rectangle position is at the top-left corner + padding and create combined
            // capture rect of entire area that will be saved to screenshot. Doing this on the second frame because when
            // ImGuiCaptureToolFlags_StitchFullContents flag is used we need to allow window to reposition.
            val moveOffset = args.inPadding - combinedWindowRectPos
//            #ifdef IMGUI_HAS_VIEWPORT
//                if (io.ConfigFlags & ImGuiConfigFlags_ViewportsEnable)
//            {
//                ImGuiViewport* main_viewport = ImGui::GetMainViewport()
//                moveOffset += main_viewport->Pos
//            }
//            #endif
            for (window in args.inCaptureWindows) {
                // Repositioning of a window may take multiple frames, depending on whether window was already rendered or not.
                window.setPos(window.pos + moveOffset)
                captureRect += window.rect()
            }

            // Include padding in capture.
            captureRect expand args.inPadding

            // Initialize capture buffer.
            output.createEmpty(captureRect.width.i, captureRect.height.i)
        } else if (frameNo % 4 == 0) {
            // FIXME: Implement capture of regions wider than viewport.
            // Capture a portion of image. Capturing of windows wider than viewport is not implemented yet.
            val capture_rect = Rect(captureRect)
            val clipRect = Rect(Vec2(), io.displaySize)
//            #ifdef IMGUI_HAS_VIEWPORT
//                if (io.ConfigFlags & ImGuiConfigFlags_ViewportsEnable)
//            {
//                ImGuiViewport* main_viewport = ImGui::GetMainViewport()
//                clipRect = ImRect(main_viewport->Pos, main_viewport->Pos + main_viewport->Size)
//            }
//            #endif
            capture_rect clipWith clipRect
            val captureHeight = io.displaySize.y min captureRect.height
            val x1 = (capture_rect.min.x - clipRect.min.x).i
            val y1 = (capture_rect.min.y - clipRect.min.y).i
            val w = capture_rect.width.i
            val h = _root_ide_package_.imgui.min(output.height - chunkNo * captureHeight, captureHeight).i
            if (h > 0) {
                if (!screenCaptureFunc(x1, y1, w, h, output.data.sliceAt(chunkNo * w * captureHeight), userData))
                    return false
                chunkNo++

                // Window moves up in order to expose it's lower part.
                for (window in args.inCaptureWindows)
                    window.setWindowPos(window.pos - Vec2(0, h.f))
                captureRect translateY -h.f
            } else {
                output.removeAlpha()

                if (args.outImageBuf == null) {
                    // Save file only if custom buffer was not specified.
//                    val file_name_size = IM_ARRAYSIZE (_SaveFileNameFinal)
//                    if (ImFormatString(_SaveFileNameFinal, file_name_size, args->OutImageFileTemplate, args->OutFileCounter+1) >= file_name_size)
//                    {
//                        ImGui::LogText("Capture Tool: file name is too long.")
//                    }
//                    else
//                    {
                    ImPathFixSeparatorsForCurrentOS(_SaveFileNameFinal)
                    if (!ImFileCreateDirectoryChain(_SaveFileNameFinal, ImPathFindFilename(_SaveFileNameFinal))) {
                        _root_ide_package_.imgui.ImGui.logText("Capture Tool: unable to create directory for file '%s'.", _SaveFileNameFinal)
                    } else {
                        args.outFileCounter++
                        output.saveFile(saveFileNameFinal)
                    }
//                    }
                    output.clear()
                }

                // Restore window position
                for (i in windowBackupRects.indices) {
                    val window = windowBackupRectsWindows[i]
                    if (window.hidden) continue

                    val rect = windowBackupRects[i]
                    window.setPos(rect.min, _root_ide_package_.imgui.Cond.Always)
                    window.setSize(rect.size, _root_ide_package_.imgui.Cond.Always)
                }

                frameNo = 0
                chunkNo = 0
                g.style.displayWindowPadding put displayWindowPaddingBackup
                g.style.displaySafeAreaPadding put displaySafeAreaPaddingBackup
                args.capturing = false
                args.inCaptureWindows.clear() // FIXME-TESTS: Why clearing this? aka why isn't args a read-only structure
                args.inCaptureRect = Rect() // FIXME-TESTS: "
                return false
            }
        }

        // Keep going
        frameNo++
        return true
    }
}

// Implements UI for capturing images
class CaptureTool(captureFunc: _root_ide_package_.engine.ScreenCaptureFunc? = null) {
    val context = _root_ide_package_.engine.CaptureContext(captureFunc)                        // Screenshot capture context.
    var flags: _root_ide_package_.engine.CaptureFlags = _root_ide_package_.engine.CaptureFlag.Default.i // Customize behavior of screenshot capture process. Flags are used by both ImGuiCaptureTool and ImGuiCaptureContext.
    var visible = false                // Tool visibility state.
    var padding = 10f                // Extra padding around captured area.
    var saveFileName = "captures/imgui_capture_%04d.png"              // File name where screenshots will be saved. May contain directories or variation of %d format.
    var snapGridSize = 32f           // Size of the grid cell for "snap to grid" functionality.

    var captureArgsPicker = _root_ide_package_.engine.CaptureArgs()             // Capture args for single window picker widget.
    var captureArgsSelector = _root_ide_package_.engine.CaptureArgs()           // Capture args for multiple window selector widget.
    var captureState: _root_ide_package_.engine.CaptureToolState = _root_ide_package_.engine.CaptureToolState.None // Which capture function is in progress.
    var windowNameMaxPosX = 170f    // X post after longest window name in CaptureWindowsSelector().

    // Render a window picker that captures picked window to file specified in file_name.
    fun captureWindowPicker(title: String, args: _root_ide_package_.engine.CaptureArgs) {

        val g = imgui.api.g
        val io = g.io

        if (captureState == _root_ide_package_.engine.CaptureToolState.Capturing && args.capturing)
            if (_root_ide_package_.imgui.Key.Escape.isPressed || !context.captureScreenshot(args))
                captureState = _root_ide_package_.engine.CaptureToolState.None

        val buttonSz = Vec2(_root_ide_package_.imgui.ImGui.calcTextSize("M").x * 30, 0f)
        val pickingId = _root_ide_package_.imgui.ImGui.getID("##picking")
        if (_root_ide_package_.imgui.ImGui.button(title, buttonSz))
            captureState = _root_ide_package_.engine.CaptureToolState.PickingSingleWindow

        if (captureState != _root_ide_package_.engine.CaptureToolState.PickingSingleWindow) {
            if (_root_ide_package_.imgui.ImGui.activeID == pickingId)
                _root_ide_package_.imgui.ImGui.clearActiveID()
            return
        }

        // Picking a window
        val fgDrawList = _root_ide_package_.imgui.ImGui.foregroundDrawList
        _root_ide_package_.imgui.ImGui.setActiveID(pickingId, g.currentWindow)    // Steal active ID so our click won't interact with something else.
        _root_ide_package_.imgui.ImGui.mouseCursor = _root_ide_package_.imgui.MouseCursor.Hand

        val captureWindow = g.hoveredRootWindow
        if (captureWindow != null) {
            if (flags has _root_ide_package_.engine.CaptureFlag.IgnoreCaptureToolWindow.i && captureWindow === _root_ide_package_.imgui.ImGui.currentWindow)
                return

            // Draw rect that is about to be captured
            val r = captureWindow.rect().apply {
                expand(args.inPadding)
                clipWith(Rect(Vec2(), io.displaySize))
                expand(1f)
            }
            fgDrawList.addRect(r.min, r.max, _root_ide_package_.imgui.COL32_WHITE, 0f, 0.inv(), 2f)
        }

        _root_ide_package_.imgui.ImGui.setTooltip("Capture window: ${captureWindow?.name ?: "<None>"}\nPress ESC to cancel.")
        if (_root_ide_package_.imgui.ImGui.isMouseClicked(_root_ide_package_.imgui.MouseButton.Left)) {
            args.inCaptureWindows.clear()
            args.inCaptureWindows += captureWindow!!
            captureState = _root_ide_package_.engine.CaptureToolState.Capturing
            // We cheat a little. args->_Capturing is set to true when Capture.CaptureScreenshot(args), but we use this
            // field to differentiate which capture is in progress (windows picker or selector), therefore we set it to true
            // in advance and execute Capture.CaptureScreenshot(args) only when args->_Capturing is true.
            args.capturing = true
        }
    }

    // Render a selector for selecting multiple windows for capture.
    fun captureWindowsSelector(title: String, args: _root_ide_package_.engine.CaptureArgs) {

        val g = imgui.api.g
        val io = g.io
        val buttonSz = Vec2(_root_ide_package_.imgui.ImGui.calcTextSize("M").x * 30, 0f)

        // Capture Button
        var doCapture = _root_ide_package_.imgui.ImGui.button(title, buttonSz)
        doCapture = doCapture || io.keyAlt && _root_ide_package_.imgui.Key.C.isPressed
        if (captureState == _root_ide_package_.engine.CaptureToolState.SelectRectUpdate && !_root_ide_package_.imgui.ImGui.isMouseDown(_root_ide_package_.imgui.MouseButton.Left)) {
            // Exit rect-capture even if selection is invalid and capture does not execute.
            captureState = _root_ide_package_.engine.CaptureToolState.None
            doCapture = true
        }

        if (_root_ide_package_.imgui.ImGui.button("Rect-Select Windows", buttonSz))
            captureState = _root_ide_package_.engine.CaptureToolState.SelectRectStart
        if (captureState == _root_ide_package_.engine.CaptureToolState.SelectRectStart || captureState == _root_ide_package_.engine.CaptureToolState.SelectRectUpdate) {
            _root_ide_package_.imgui.ImGui.mouseCursor = _root_ide_package_.imgui.MouseCursor.Hand
            if (_root_ide_package_.imgui.ImGui.isItemHovered())
                _root_ide_package_.imgui.ImGui.setTooltip("Select multiple windows by pressing left mouse button and dragging.")
        }
        _root_ide_package_.imgui.ImGui.separator()

        // Show window list and update rectangles
        val selectRect = Rect()
        if (captureState == _root_ide_package_.engine.CaptureToolState.SelectRectStart && _root_ide_package_.imgui.ImGui.isMouseDown(_root_ide_package_.imgui.MouseButton.Left)) {
            captureState = when {
                _root_ide_package_.imgui.ImGui.isWindowHovered(_root_ide_package_.imgui.HoveredFlag.AnyWindow) -> _root_ide_package_.engine.CaptureToolState.None
                else -> {
                    args.inCaptureRect.min put io.mousePos
                    _root_ide_package_.engine.CaptureToolState.SelectRectUpdate
                }
            }
        } else if (captureState == _root_ide_package_.engine.CaptureToolState.SelectRectUpdate) {
            // Avoid inverted-rect issue
            selectRect.min = args.inCaptureRect.min min io.mousePos
            selectRect.max = args.inCaptureRect.min max io.mousePos
        }

        args.inCaptureWindows.clear()

        var maxWindowNameX = 0f
        val captureRect = Rect(Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        _root_ide_package_.imgui.ImGui.text("Windows:")
        for (window in g.windows) {
            if (!window.wasActive)
                continue

            if (args.inFlags has _root_ide_package_.engine.CaptureFlag.ExpandToIncludePopups.i && (window.flags has Wf._Popup || window.flags has Wf._Tooltip)) {
                captureRect add window.rect()
                args.inCaptureWindows += window
                continue
            }

            if (window.flags has Wf._ChildWindow)
                continue

            if (args.inFlags has _root_ide_package_.engine.CaptureFlag.IgnoreCaptureToolWindow.i && window === _root_ide_package_.imgui.ImGui.currentWindow)
                continue

            _root_ide_package_.imgui.ImGui.pushID(window)
            val curr = g.currentWindow!!
            var selected = curr.stateStorage.bool(window.rootWindow!!.id, false)

            if (captureState == _root_ide_package_.engine.CaptureToolState.SelectRectUpdate)
                selected = window.rect() in selectRect

            // Ensure that text after the ## is actually displayed to the user (FIXME: won't be able to check/uncheck from it)
            selected = _root_ide_package_.imgui.withBool(selected) { _root_ide_package_.imgui.ImGui.checkbox(window.name, it) }
            curr.stateStorage[window.rootWindow!!.id] = selected
            val remainingText = _root_ide_package_.imgui.ImGui.findRenderedTextEnd(window.name)
            if (remainingText != 0) {
                _root_ide_package_.imgui.ImGui.sameLine(0, 1)
                _root_ide_package_.imgui.ImGui.textUnformatted(window.name.substring(remainingText))
            }

            maxWindowNameX = maxWindowNameX max (curr.dc.cursorPosPrevLine.x - curr.pos.x)
            if (selected) {
                captureRect add window.rect()
                args.inCaptureWindows += window
            }
            _root_ide_package_.imgui.ImGui.sameLine(windowNameMaxPosX + g.style.itemSpacing.x)
            _root_ide_package_.imgui.ImGui.setNextItemWidth(100f)
            _root_ide_package_.imgui.ImGui.dragVec2("Pos", window.pos, 0.05f, 0f, 0f, "%.0f")
            _root_ide_package_.imgui.ImGui.sameLine()
            _root_ide_package_.imgui.ImGui.setNextItemWidth(100f)
            _root_ide_package_.imgui.ImGui.dragVec2("Size", window.sizeFull, 0.05f, 0f, 0f, "%.0f")
            _root_ide_package_.imgui.ImGui.popID()
        }
        windowNameMaxPosX = maxWindowNameX

        // Draw capture rectangle
        val drawList = _root_ide_package_.imgui.ImGui.getForegroundDrawList()
        val canCapture = !captureRect.isInverted && !args.inCaptureWindows.isEmpty()
        if (canCapture && (captureState == _root_ide_package_.engine.CaptureToolState.None || captureState == _root_ide_package_.engine.CaptureToolState.selectRectUpdate)) {
            assert(captureRect.width > 0 && captureRect.height > 0)
            captureRect expand args.inPadding
            val displayPos = Vec2()
            val displaySize = Vec2(io.displaySize)
//            #ifdef IMGUI_HAS_VIEWPORT
//                    if (io.ConfigFlags & ImGuiConfigFlags_ViewportsEnable)
//            {
//                ImGuiViewport * main_viewport = ImGui::GetMainViewport()
//                displayPos = main_viewport->Pos
//                displaySize = main_viewport->Size
//            }
//            #endif
            captureRect clipWith Rect(displayPos, displayPos + displaySize)
            drawList.addRect(captureRect.min - 1f, captureRect.max + 1f, _root_ide_package_.imgui.COL32_WHITE)
        }

        if (captureState == _root_ide_package_.engine.CaptureToolState.SelectRectUpdate)
            drawList.addRect(selectRect.min - 1f, selectRect.max + 1f, _root_ide_package_.imgui.COL32_WHITE)

        // Process capture
        if (canCapture && doCapture) {
            // We cheat a little. args->_Capturing is set to true when Capture.CaptureScreenshot(args), but we use this
            // field to differentiate which capture is in progress (windows picker or selector), therefore we set it to true
            // in advance and execute Capture.CaptureScreenshot(args) only when args->_Capturing is true.
            args.capturing = true
            captureState = _root_ide_package_.engine.CaptureToolState.Capturing
        }

        if (_root_ide_package_.imgui.ImGui.isItemHovered())
            _root_ide_package_.imgui.ImGui.setTooltip("Alternatively press Alt+C to capture selection.")

        if (captureState == _root_ide_package_.engine.CaptureToolState.Capturing && args.capturing)
            if (!context.captureScreenshot(args))
                captureState = _root_ide_package_.engine.CaptureToolState.None
    }

    // Render a capture tool window with various options and utilities.
    fun TestContext.showCaptureToolWindow(pOpen: KMutableProperty0<Boolean>? = null) {

        if (!_root_ide_package_.imgui.ImGui.begin("Dear ImGui Capture Tool", pOpen)) {
            _root_ide_package_.imgui.ImGui.end()
            return
        }

        if (context.screenCaptureFunc == null) {
            _root_ide_package_.imgui.ImGui.textColored(Vec4(1, 0, 0, 1), "Back-end is missing ScreenCaptureFunc!")
            _root_ide_package_.imgui.ImGui.end()
            return
        }

        val io = _root_ide_package_.imgui.ImGui.io
        val style = _root_ide_package_.imgui.ImGui.style

        // Options
        _root_ide_package_.imgui.ImGui.setNextItemOpen(true, _root_ide_package_.imgui.Cond.Once)
        _root_ide_package_.imgui.dsl.treeNode("Options") {
            val hasLastFileName = context.saveFileNameFinal.isNotEmpty()
            if (!hasLastFileName)
                _root_ide_package_.imgui.ImGui.pushDisabled()
            if (_root_ide_package_.imgui.ImGui.button("Open Last"))             // FIXME-CAPTURE: Running tests changes last captured file name.
                osOpenInShell(context._SaveFileNameFinal)
            if (!hasLastFileName)
                _root_ide_package_.imgui.ImGui.popDisabled()
            if (hasLastFileName && _root_ide_package_.imgui.ImGui.sItemHovered())
                _root_ide_package_.imgui.ImGui.setTooltip("Open ${context.saveFileNameFinal}")
            _root_ide_package_.imgui.ImGui.sameLine()
            TODO()
//            Str128 save_file_dir(SaveFileName)
//            if (!save_file_dir[0])
//                ImGui::PushDisabled()
//            else if (char* slash_pos = ImMax(strrchr(save_file_dir.c_str(), '/'), strrchr(save_file_dir.c_str(), '\\')))
//            *slash_pos = 0                         // Remove file name.
//            else
//            strcpy(save_file_dir.c_str(), ".")     // Only filename is present, open current directory.
//            if (ImGui::Button("Open Directory"))
//                ImOsOpenInShell(save_file_dir.c_str())
//            if (save_file_dir[0] && ImGui::IsItemHovered())
//                ImGui::SetTooltip("Open %s/", save_file_dir.c_str())
//            if (!save_file_dir[0])
//                ImGui::PopDisabled()
//
//            ImGui::PushItemWidth(-200.0f)
//
//            ImGui::InputText("Out filename template", SaveFileName, IM_ARRAYSIZE(SaveFileName))
//            ImGui::DragFloat("Padding", &Padding, 0.1f, 0, 32, "%.0f")
//
//            if (ImGui::Button("Snap Windows To Grid", ImVec2(-200, 0)))
//                SnapWindowsToGrid(SnapGridSize)
//            ImGui::SameLine(0.0f, style.ItemInnerSpacing.x)
//            ImGui::SetNextItemWidth(50.0f)
//            ImGui::DragFloat("##SnapGridSize", &SnapGridSize, 1.0f, 1.0f, 128.0f, "%.0f")
//
//            ImGui::Checkbox("Software Mouse Cursor", &io.MouseDrawCursor)  // FIXME-TESTS: Test engine always resets this value.
//            ImGui::CheckboxFlags("Stitch and capture full contents height", &Flags, ImGuiCaptureToolFlags_StitchFullContents)
//            ImGui::CheckboxFlags("Always ignore capture tool window", &Flags, ImGuiCaptureToolFlags_IgnoreCaptureToolWindow)
//            if (ImGui::IsItemHovered())
//                ImGui::SetTooltip("Full height of picked window will be captured.")
//            ImGui::CheckboxFlags("Include tooltips", &Flags, ImGuiCaptureToolFlags_ExpandToIncludePopups)
//            if (ImGui::IsItemHovered())
//                ImGui::SetTooltip("Capture area will be expanded to include visible tooltips.")
//
//            ImGui::PopItemWidth()
        }

        _root_ide_package_.imgui.ImGui.separator()

        // Ensure that use of different contexts use same file counter and don't overwrite previously created files.
        captureArgsPicker.outFileCounter = captureArgsPicker.outFileCounter max captureArgsSelector.outFileCounter
        captureArgsSelector.outFileCounter = captureArgsPicker.outFileCounter
        // Propagate settings from UI to args.
        captureArgsPicker.inPadding = padding
        captureArgsSelector.inPadding = padding
        captureArgsPicker.inFlags = flags
        captureArgsSelector.inFlags = flags
        TODO()
//        ImStrncpy(_CaptureArgsPicker.OutImageFileTemplate, SaveFileName, (size_t)IM_ARRAYSIZE(_CaptureArgsPicker.OutImageFileTemplate))
//        ImStrncpy(_CaptureArgsSelector.OutImageFileTemplate, SaveFileName, (size_t)IM_ARRAYSIZE(_CaptureArgsSelector.OutImageFileTemplate))
//
//        // Hide tool window unconditionally.
//        if (Flags & ImGuiCaptureToolFlags_IgnoreCaptureToolWindow && _CaptureState == ImGuiCaptureToolState_Capturing)
//        {
//            ImGuiWindow* window = ImGui::GetCurrentWindowRead()
//            window->Hidden = true
//            window->HiddenFramesCannotSkipItems = 2
//        }
//
//        CaptureWindowPicker("Capture Window", &_CaptureArgsPicker)
//        CaptureWindowsSelector("Capture Selected", &_CaptureArgsSelector)
//        ImGui::Separator()
//
//        ImGui::End()
    }

    // Snaps edges of all visible windows to a virtual grid.
    //
    // Move/resize all windows so they are neatly aligned on a grid
    // This is an easy way of ensuring some form of alignment without specifying detailed constraints.
    infix fun TestContext.snapWindowsToGrid(cellSize: Float) {
        gImGui!!.windows
                .filter { it.wasActive && it.flags hasnt Wf._ChildWindow && it.flags hasnt Wf._Popup && it.flags hasnt Wf._Tooltip }
                .forEach { window ->
                    val rect = window.rect().apply {
                        min.x = imgui.internal.floor(min.x / cellSize) * cellSize
                        min.y = imgui.internal.floor(min.y / cellSize) * cellSize
                        max.x = imgui.internal.floor(max.x / cellSize) * cellSize
                        max.y = imgui.internal.floor(max.y / cellSize) * cellSize
                        min.plusAssign(padding)
                        max.plusAssign(padding)
                    }
                    window.setPos(rect.min)
                    window.setSize(rect.size)
                }
    }
}
