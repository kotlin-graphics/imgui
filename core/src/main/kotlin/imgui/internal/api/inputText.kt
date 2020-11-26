package imgui.internal.api

import gli_.has
import glm_.b
import glm_.compareTo
import glm_.func.common.max
import glm_.func.common.min
import glm_.glm
import glm_.i
import glm_.vec1.Vec1i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.beginChildEx
import imgui.ImGui.beginGroup
import imgui.ImGui.calcItemSize
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.clearActiveID
import imgui.ImGui.clipboardText
import imgui.ImGui.currentWindow
import imgui.ImGui.dataTypeApplyOpFromText
import imgui.ImGui.dataTypeClamp
import imgui.ImGui.dummy
import imgui.ImGui.endChild
import imgui.ImGui.endGroup
import imgui.ImGui.focusWindow
import imgui.ImGui.focusableItemRegister
import imgui.ImGui.format
import imgui.ImGui.getColorU32
import imgui.ImGui.io
import imgui.ImGui.itemAdd
import imgui.ImGui.itemHoverable
import imgui.ImGui.itemSize
import imgui.ImGui.logRenderedText
import imgui.ImGui.markItemEdited
import imgui.ImGui.mergedKeyModFlags
import imgui.ImGui.parseFormatTrimDecorations
import imgui.ImGui.popFont
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushFont
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.renderFrame
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.renderText
import imgui.ImGui.scrollMaxY
import imgui.ImGui.setActiveID
import imgui.ImGui.setFocusID
import imgui.ImGui.style
import imgui.api.g
import imgui.classes.InputTextCallbackData
import imgui.internal.*
import imgui.internal.classes.InputTextState
import imgui.internal.classes.InputTextState.K
import imgui.internal.classes.Rect
import imgui.internal.sections.Axis
import imgui.internal.sections.InputSource
import imgui.internal.sections.shl
import imgui.stb.te.click
import imgui.stb.te.cut
import imgui.stb.te.drag
import imgui.stb.te.paste
import uno.kotlin.NUL
import uno.kotlin.getValue
import uno.kotlin.isPrintable
import uno.kotlin.setValue
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KMutableProperty0
import imgui.InputTextFlag as Itf
import imgui.WindowFlag as Wf

/** InputText */
internal interface inputText {

    /** InputTextEx
     *  - bufSize account for the zero-terminator, so a buf_size of 6 can hold "Hello" but not "Hello!".
     *    This is so we can easily call InputText() on static arrays using ARRAYSIZE() and to match
     *    Note that in std::string world, capacity() would omit 1 byte used by the zero-terminator.
     *  - When active, hold on a privately held copy of the text (and apply back to 'buf'). So changing 'buf' while the InputText is active has no effect.
     *  - If you want to use ImGui::InputText() with std::string, see misc/cpp/imgui_stl.h
     *  (FIXME: Rather confusing and messy function, among the worse part of our codebase, expecting to rewrite a V2 at some point.. Partly because we are
     *  doing UTF8 > U16 > UTF8 conversions on the go to easily internal interface with stb_textedit. Ideally should stay in UTF-8 all the time. See https://github.com/nothings/stb/issues/188)
     */
    fun inputTextEx(label: String, hint: String?, buf_: ByteArray, sizeArg: Vec2, flags: InputTextFlags,
                    callback: InputTextCallback? = null, callbackUserData: Any? = null): Boolean {

        var buf = buf_

        val window = currentWindow
        if (window.skipItems) return false

        assert(buf.isNotEmpty())
        assert(!(flags has Itf.CallbackHistory && flags has Itf._Multiline)) { "Can't use both together (they both use up/down keys)" }
        assert(!(flags has Itf.CallbackCompletion && flags has Itf.AllowTabInput)) { "Can't use both together (they both use tab key)" }

        val RENDER_SELECTION_WHEN_INACTIVE = false
        val isMultiline = flags has Itf._Multiline
        val isReadOnly = flags has Itf.ReadOnly
        val isPassword = flags has Itf.Password
        val isUndoable = flags hasnt Itf.NoUndoRedo
        val isResizable = flags has Itf.CallbackResize
        if (isResizable)
            assert(callback != null) { "Must provide a callback if you set the ImGuiInputTextFlags_CallbackResize flag!" }
        if (flags has Itf.CallbackCharFilter)
            assert(callback != null) { "Must provide a callback if you want a char filter!" }

        if (isMultiline) // Open group before calling GetID() because groups tracks id created within their scope
            beginGroup()
        val id = window.getID(label)
        val labelSize = calcTextSize(label, hideTextAfterDoubleHash = true)
        val h = if (isMultiline) g.fontSize * 8f else labelSize.y
        val frameSize = calcItemSize(
                sizeArg,
                calcItemWidth(),
                h + style.framePadding.y * 2f
        ) // Arbitrary default of 8 lines high for multi-line
        val totalSize =
                Vec2(frameSize.x + if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, frameSize.y)

        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + frameSize)
        val totalBb = Rect(frameBb.min, frameBb.min + totalSize)

        var drawWindow = window
        val innerSize = Vec2(frameSize)
        val bufEnd = Vec1i()
        if (isMultiline) {
            if (!itemAdd(totalBb, id, frameBb)) {
                itemSize(totalBb, style.framePadding.y)
                endGroup()
                return false
            }
            // We reproduce the contents of BeginChildFrame() in order to provide 'label' so our window internal data are easier to read/debug.
            pushStyleColor(Col.ChildBg, style.colors[Col.FrameBg])
            pushStyleVar(StyleVar.ChildRounding, style.frameRounding)
            pushStyleVar(StyleVar.ChildBorderSize, style.frameBorderSize)
            pushStyleVar(StyleVar.WindowPadding, style.framePadding)
            val childVisible = beginChildEx(label, id, frameBb.size, true, Wf.NoMove or Wf.AlwaysUseWindowPadding)
            popStyleVar(3)
            popStyleColor()
            if (!childVisible) {
                endChild()
                endGroup()
                return false
            }
            drawWindow = g.currentWindow!!  // Child window
            // This is to ensure that EndChild() will display a navigation highlight so we can "enter" into it.
            drawWindow.dc.navLayerActiveMaskNext = drawWindow.dc.navLayerActiveMaskNext or (1 shl drawWindow.dc.navLayerCurrent)
            innerSize.x -= drawWindow.scrollbarSizes.x
        } else {
            itemSize(totalBb, style.framePadding.y)
            if (!itemAdd(totalBb, id, frameBb)) return false
        }
        val hovered = itemHoverable(frameBb, id)
        if (hovered) g.mouseCursor = MouseCursor.TextInput

        // We are only allowed to access the state if we are already the active widget.
        var state = getInputTextState(id)

        val focusRequested = focusableItemRegister(window, id)
        val focusRequestedByCode =
                focusRequested && g.focusRequestCurrWindow === window && g.focusRequestCurrCounterRegular == window.dc.focusCounterRegular
        val focusRequestedByTab = focusRequested && !focusRequestedByCode

        val userClicked = hovered && io.mouseClicked[0]
        val userNavInputStart =
                g.activeId != id && (g.navInputId == id || (g.navActivateId == id && g.navInputSource == InputSource.NavKeyboard))
        val userScrollFinish =
                isMultiline && state != null && g.activeId == 0 && g.activeIdPreviousFrame == drawWindow getScrollbarID Axis.Y
        val userScrollActive = isMultiline && state != null && g.activeId == drawWindow getScrollbarID Axis.Y

        var clearActiveId = false
        var selectAll = g.activeId != id && (flags has Itf.AutoSelectAll || userNavInputStart) && !isMultiline

        var scrollY = if (isMultiline) drawWindow.scroll.y else Float.MAX_VALUE

        val initMakeActive = focusRequested || userClicked || userScrollFinish || userNavInputStart
        val initState = initMakeActive || userScrollActive
        if (initState && g.activeId != id) {
            // Access state even if we don't own it yet.
            state = g.inputTextState
            state.cursorAnimReset()

            // Take a copy of the initial buffer value (both in original UTF-8 format and converted to wchar)
            // From the moment we focused we are ignoring the content of 'buf' (unless we are in read-only mode)
            val bufLen = buf.strlen()
            if (state.initialTextA.size < bufLen)
                state.initialTextA =
                        ByteArray(bufLen)   // UTF-8. we use +1 to make sure that .Data is always pointing to at least an empty string.
            else if (state.initialTextA.size > bufLen)
                state.initialTextA[bufLen] = 0
            System.arraycopy(buf, 0, state.initialTextA, 0, bufLen)

            // Start edition
            if (state.textW.size < buf.size)
                state.textW = CharArray(buf.size)   // wchar count <= UTF-8 count. we use +1 to make sure that .Data is always pointing to at least an empty string.
            else if (state.textW.size > buf.size)
                state.textW[buf.size] = NUL
//            state.textA = ByteArray(0)
            state.textAIsValid = false // TextA is not valid yet (we will display buf until then)
            state.curLenW = textStrFromUtf8(state.textW, buf, textRemaining = bufEnd)
            state.curLenA =
                    bufEnd[0] // We can't get the result from ImStrncpy() above because it is not UTF-8 aware. Here we'll cut off malformed UTF-8.

            /*  Preserve cursor position and undo/redo stack if we come back to same widget
                For non-readonly widgets we might be able to require that TextAIsValid && TextA == buf ? (untested) and discard undo stack if user buffer has changed. */
            val recycleState = state.id == id
            if (recycleState)
            /*  Recycle existing cursor/selection/undo stack but clamp position
                Note a single mouse click will override the cursor/position immediately by calling
                stb_textedit_click handler.                     */
                state.cursorClamp()
            else {
                state.id = id
                state.scrollX = 0f
                state.stb initialize !isMultiline
                if (!isMultiline && focusRequestedByCode)
                    selectAll = true
            }
            if (flags has Itf.AlwaysInsertMode)
                state.stb.insertMode = true
            if (!isMultiline && (focusRequestedByTab || (userClicked && io.keyCtrl)))
                selectAll = true
        }

        if (g.activeId != id && initMakeActive) {
            assert(state!!.id == id)
            setActiveID(id, window)
            setFocusID(id, window)
            focusWindow(window)

            // Declare our inputs
            assert(NavInput.values().size < 32)
            g.activeIdUsingNavDirMask = g.activeIdUsingNavDirMask or ((1 shl Dir.Left) or (1 shl Dir.Right))
            if (isMultiline || flags has Itf.CallbackHistory)
                g.activeIdUsingNavDirMask = g.activeIdUsingNavDirMask or ((1 shl Dir.Up) or (1 shl Dir.Down))
            g.activeIdUsingNavInputMask = g.activeIdUsingNavInputMask or (1 shl NavInput.Cancel)
            g.activeIdUsingKeyInputMask = g.activeIdUsingKeyInputMask or ((1L shl Key.Home) or (1L shl Key.End))
            if (isMultiline)
                g.activeIdUsingKeyInputMask =
                        g.activeIdUsingKeyInputMask or ((1L shl Key.PageUp) or (1L shl Key.PageDown))
            if (flags has (Itf.CallbackCompletion or Itf.AllowTabInput))  // Disable keyboard tabbing out as we will use the \t character.
                g.activeIdUsingKeyInputMask = g.activeIdUsingKeyInputMask or (1L shl Key.Tab)
        }

        // We have an edge case if ActiveId was set through another widget (e.g. widget being swapped), clear id immediately (don't wait until the end of the function)
        if (g.activeId == id && state == null)
            clearActiveID()

        // Release focus when we click outside
        if (g.activeId == id && io.mouseClicked[0] && !initState && !initMakeActive)
            clearActiveId = true

        // Lock the decision of whether we are going to take the path displaying the cursor or selection
        val renderCursor = g.activeId == id || (state != null && userScrollActive)
        var renderSelection = state?.hasSelection == true && (RENDER_SELECTION_WHEN_INACTIVE || renderCursor)
        var valueChanged = false
        var enterPressed = false

        // When read-only we always use the live data passed to the function
        // FIXME-OPT: Because our selection/cursor code currently needs the wide text we need to convert it when active, which is not ideal :(
        if (isReadOnly && state != null && (renderCursor || renderSelection)) {
            if (state.textW.size < buf.size)
                state.textW = CharArray(buf.size)
            else if (state.textW.size > buf.size)
                state.textW[buf.size] = NUL
            state.curLenW = textStrFromUtf8(state.textW, buf, textRemaining = bufEnd(0))
            state.curLenA = bufEnd.x
            state.cursorClamp()
            renderSelection = renderSelection && state.hasSelection
        }

        // Select the buffer to render.
        val bufDisplayFromState =
                (renderCursor || renderSelection || g.activeId == id) && !isReadOnly && state?.textAIsValid == true
        val isDisplayingHint = hint != null && (if (bufDisplayFromState) state!!.textA else buf)[0] == 0.b

        // Password pushes a temporary font with only a fallback glyph
        if (isPassword && !isDisplayingHint)
            g.inputTextPasswordFont.apply {
                val glyph = g.font.findGlyph('*')!!
                fontSize = g.font.fontSize
                scale = g.font.scale
                ascent = g.font.ascent
                descent = g.font.descent
                containerAtlas = g.font.containerAtlas
                fallbackGlyph = glyph
                fallbackAdvanceX = glyph.advanceX
                assert(glyphs.isEmpty() && indexAdvanceX.isEmpty() && indexLookup.isEmpty())
                pushFont(this)
            }

        // Process mouse inputs and character inputs
        var backupCurrentTextLength = 0
        if (g.activeId == id) {

            backupCurrentTextLength = state!!.curLenA
            state.apply {
                edited = false
                bufCapacityA = buf.size
                userFlags = flags
                userCallback = callback
                userCallbackData = callbackUserData
            }
            /*  Although we are active we don't prevent mouse from hovering other elements unless we are interacting
                right now with the widget.
                Down the line we should have a cleaner library-wide concept of Selected vs Active.  */
            g.activeIdAllowOverlap = !io.mouseDown[0]
            g.wantTextInputNextFrame = 1

            // Edit in progress
            val mouseX = io.mousePos.x - frameBb.min.x - style.framePadding.x + state.scrollX
            val mouseY = when {
                isMultiline -> io.mousePos.y - drawWindow.dc.cursorPos.y - style.framePadding.y
                else -> g.fontSize * 0.5f
            }

            // OS X style: Double click selects by word instead of selecting whole text
            val isOsx = io.configMacOSXBehaviors
            if (selectAll /*|| (hovered && !isOsx && io.mouseDoubleClicked[0])*/) { // [JVM] https://github.com/ocornut/imgui/pull/2244
                state.selectAll()
                state.selectedAllMouseLock = true
            } else if (hovered /*&& isOsx*/ && io.mouseDoubleClicked[0]) { // [JVM] https://github.com/ocornut/imgui/pull/2244
                // Double-click select a word only, OS X style (by simulating keystrokes)
                state.onKeyPressed(K.WORDLEFT)
                state.onKeyPressed(K.WORDRIGHT or K.SHIFT)
            } else if (io.mouseClicked[0] && !state.selectedAllMouseLock) {
                if (hovered) {
                    state.click(mouseX, mouseY)
                    state.cursorAnimReset()
                }
            } else if (io.mouseDown[0] && !state.selectedAllMouseLock && (io.mouseDelta.x != 0f || io.mouseDelta.y != 0f)) { // TODO -> glm once anyNotEqual gets fixed
                state.drag(mouseX, mouseY)
                state.cursorAnimReset()
                state.cursorFollow = true
            }
            if (state.selectedAllMouseLock && !io.mouseDown[0])
                state.selectedAllMouseLock = false

            // It is ill-defined whether the backend needs to send a \t character when pressing the TAB keys.
            // Win32 and GLFW naturally do it but not SDL.
            val ignoreCharInputs = (io.keyCtrl && !io.keyAlt) || (isOsx && io.keySuper)
            if (flags has Itf.AllowTabInput && Key.Tab.isPressed && !ignoreCharInputs && !io.keyShift && !isReadOnly)
                if ('\t' !in io.inputQueueCharacters)
                    withChar {
                        it.set('\t') // Insert TAB
                        if (inputTextFilterCharacter(it, flags, callback, callbackUserData))
                            state.onKeyPressed(it().i)
                    }

            // Process regular text input (before we check for Return because using some IME will effectively send a Return?)
            // We ignore CTRL inputs, but need to allow ALT+CTRL as some keyboards (e.g. German) use AltGR (which _is_ Alt+Ctrl) to input certain characters.
            if (io.inputQueueCharacters.isNotEmpty()) {
                if (!ignoreCharInputs && !isReadOnly && !userNavInputStart)
                    io.inputQueueCharacters.filter { it != NUL || (it == '\t' && io.keyShift) }.map {
                        // TODO check
                        withChar { c -> // Insert character if they pass filtering
                            if (inputTextFilterCharacter(c(it), flags, callback, callbackUserData))
                                state.onKeyPressed(c().i)
                        }
                    }
                // Consume characters
                io.inputQueueCharacters.clear()
            }
        }

        // Process other shortcuts/key-presses
        var cancelEdit = false
        if (g.activeId == id && !g.activeIdIsJustActivated && !clearActiveId) {
            state!! // ~IM_ASSERT(state != NULL);
            assert(io.keyMods == mergedKeyModFlags) { "Mismatching io.KeyCtrl/io.KeyShift/io.KeyAlt/io.KeySuper vs io.KeyMods" } // We rarely do this check, but if anything let's do it here.

            val rowCountPerPage = ((innerSize.y - style.framePadding.y) / g.fontSize).i max 1
            state.stb.rowCountPerPage = rowCountPerPage

            val kMask = if (io.keyShift) K.SHIFT else 0
            val isOsx = io.configMacOSXBehaviors
            // OS X style: Shortcuts using Cmd/Super instead of Ctrl
            val isOsxShiftShortcut = isOsx && io.keyMods == (KeyMod.Super or KeyMod.Shift)
            val isWordmoveKeyDown = if (isOsx) io.keyAlt else io.keyCtrl // OS X style: Text editing cursor movement using Alt instead of Ctrl
            // OS X style: Line/Text Start and End using Cmd+Arrows instead of Home/End
            val isStartendKeyDown = isOsx && io.keySuper && !io.keyCtrl && !io.keyAlt
            val isCtrlKeyOnly = io.keyMods == KeyMod.Ctrl.i
            val isShiftKeyOnly = io.keyMods == KeyMod.Shift.i
            val isShortcutKey = io.keyMods == if (io.configMacOSXBehaviors) KeyMod.Super.i else KeyMod.Ctrl.i

            val isCut = ((isShortcutKey && Key.X.isPressed) || (isShiftKeyOnly && Key.Delete.isPressed)) && !isReadOnly && !isPassword && (!isMultiline || state.hasSelection)
            val isCopy = ((isShortcutKey && Key.C.isPressed) || (isCtrlKeyOnly && Key.Insert.isPressed)) && !isPassword && (!isMultiline || state.hasSelection)
            val isPaste = ((isShortcutKey && Key.V.isPressed) || (isShiftKeyOnly && Key.Insert.isPressed)) && !isReadOnly
            val isUndo = ((isShortcutKey && Key.Z.isPressed) && !isReadOnly && isUndoable)
            val isRedo = ((isShortcutKey && Key.Y.isPressed) || (isOsxShiftShortcut && Key.Z.isPressed)) && !isReadOnly && isUndoable

            when {
                Key.LeftArrow.isPressed -> state.onKeyPressed(
                        when {
                            isStartendKeyDown -> K.LINESTART
                            isWordmoveKeyDown -> K.WORDLEFT
                            else -> K.LEFT
                        } or kMask)
                Key.RightArrow.isPressed -> state.onKeyPressed(
                        when {
                            isStartendKeyDown -> K.LINEEND
                            isWordmoveKeyDown -> K.WORDRIGHT
                            else -> K.RIGHT
                        } or kMask)
                Key.UpArrow.isPressed && isMultiline ->
                    if (io.keyCtrl)
                        drawWindow setScrollY glm.max(drawWindow.scroll.y - g.fontSize, 0f)
                    else
                        state.onKeyPressed((if (isStartendKeyDown) K.TEXTSTART else K.UP) or kMask)
                Key.DownArrow.isPressed && isMultiline ->
                    if (io.keyCtrl)
                        drawWindow setScrollY glm.min(drawWindow.scroll.y + g.fontSize, scrollMaxY)
                    else
                        state.onKeyPressed((if (isStartendKeyDown) K.TEXTEND else K.DOWN) or kMask)
                Key.PageUp.isPressed && isMultiline -> {
                    state.onKeyPressed(K.PGUP or kMask)
                    scrollY -= rowCountPerPage * g.fontSize
                }
                Key.PageDown.isPressed && isMultiline -> {
                    state.onKeyPressed(K.PGDOWN or kMask)
                    scrollY += rowCountPerPage * g.fontSize
                }
                Key.Home.isPressed -> state.onKeyPressed((if (io.keyCtrl) K.TEXTSTART else K.LINESTART) or kMask)
                Key.End.isPressed -> state.onKeyPressed((if (io.keyCtrl) K.TEXTEND else K.LINEEND) or kMask)
                Key.Delete.isPressed && !isReadOnly -> state.onKeyPressed(K.DELETE or kMask)
                Key.Backspace.isPressed && !isReadOnly -> {
                    if (!state.hasSelection)
                        if (isWordmoveKeyDown)
                            state.onKeyPressed(K.WORDLEFT or K.SHIFT)
                        else if (isOsx && io.keySuper && !io.keyAlt && !io.keyCtrl)
                            state.onKeyPressed(K.LINESTART or K.SHIFT)
                    state.onKeyPressed(K.BACKSPACE or kMask)
                }
                Key.Enter.isPressed || Key.KeyPadEnter.isPressed -> {
                    val ctrlEnterForNewLine = flags has Itf.CtrlEnterForNewLine
                    if (!isMultiline || (ctrlEnterForNewLine && !io.keyCtrl) || (!ctrlEnterForNewLine && io.keyCtrl)) {
                        clearActiveId = true
                        enterPressed = true
                    } else if (!isReadOnly)
                        withChar('\n') { c ->
                            // Insert new line
                            if (inputTextFilterCharacter(c, flags, callback, callbackUserData))
                                state.onKeyPressed(c().i)
                        }
                }
                Key.Escape.isPressed -> {
                    cancelEdit = true
                    clearActiveId = true
                }
                isUndo || isRedo -> {
                    state.onKeyPressed(if (isUndo) K.UNDO else K.REDO)
                    state.clearSelection()
                }
                isShortcutKey && Key.A.isPressed -> {
                    state.selectAll()
                    state.cursorFollow = true
                }
                isCut || isCopy -> {
                    // Cut, Copy
                    io.setClipboardTextFn?.let {
                        val ib = if (state.hasSelection) min(state.stb.selectStart, state.stb.selectEnd) else 0
                        val ie =
                                if (state.hasSelection) max(state.stb.selectStart, state.stb.selectEnd) else state.curLenW
                        clipboardText = String(state.textW, ib, ie - ib)
                    }
                    if (isCut) {
                        if (!state.hasSelection)
                            state.selectAll()
                        state.cursorFollow = true
                        state.cut()
                    }
                }
                isPaste -> {

                    val clipboard = clipboardText

                    // Filter pasted buffer
                    val clipboardLen = clipboard.length
                    val clipboardFiltered = CharArray(clipboardLen)
                    var clipboardFilteredLen = 0
                    for (c in clipboard) {
                        if (c == NUL)
                            break
                        _c = c
                        if (!inputTextFilterCharacter(::_c, flags, callback, callbackUserData))
                            continue
                        clipboardFiltered[clipboardFilteredLen++] = _c
                    }
                    if (clipboardFilteredLen > 0) { // If everything was filtered, ignore the pasting operation
                        state.paste(clipboardFiltered, clipboardFilteredLen)
                        state.cursorFollow = true
                    }
                }
            }

            // Update render selection flag after events have been handled, so selection highlight can be displayed during the same frame.
            renderSelection = renderSelection || (state.hasSelection && (RENDER_SELECTION_WHEN_INACTIVE || renderCursor))
        }

        // Process callbacks and apply result back to user's buffer.
        if (g.activeId == id) {
            state!!
            var applyNewText: ByteArray? = null
            var applyNewTextLength = 0

            if (cancelEdit)
            // Restore initial value. Only return true if restoring to the initial value changes the current buffer contents.
                if (!isReadOnly && buf strcmp state.initialTextA != 0) {
                    // Push records into the undo stack so we can CTRL+Z the revert operation itself
                    applyNewText = state.initialTextA
                    applyNewTextLength = state.initialTextA.size

                    var wText = CharArray(0)
                    if (applyNewTextLength > 0) {
                        wText = CharArray(textCountCharsFromUtf8(applyNewText, applyNewTextLength))
                        textStrFromUtf8(wText, applyNewText, applyNewTextLength)
                    }
                    state.replace(wText, if (applyNewTextLength > 0) wText.size else 0)
                }

            // When using 'ImGuiInputTextFlags_EnterReturnsTrue' as a special case we reapply the live buffer back to the input buffer before clearing ActiveId, even though strictly speaking it wasn't modified on this frame.
            // If we didn't do that, code like InputInt() with ImGuiInputTextFlags_EnterReturnsTrue would fail.
            // This also allows the user to use InputText() with ImGuiInputTextFlags_EnterReturnsTrue without maintaining any user-side storage (please note that if you use this property along ImGuiInputTextFlags_CallbackResize you can end up with your temporary string object unnecessarily allocating once a frame, either store your string data, either if you don't then don't use ImGuiInputTextFlags_CallbackResize).
            val applyEditBackToUserBuffer = !cancelEdit || (enterPressed && flags hasnt Itf.EnterReturnsTrue)
            if (applyEditBackToUserBuffer) {
                // Apply new value immediately - copy modified buffer back
                // Note that as soon as the input box is active, the in-widget value gets priority over any underlying modification of the input buffer
                // FIXME: We actually always render 'buf' when calling DrawList->AddText, making the comment above incorrect.
                // FIXME-OPT: CPU waste to do this every time the widget is active, should mark dirty state from the stb_textedit callbacks.
                if (!isReadOnly) {
                    state.textAIsValid = true
                    if (state.textA.size < state.textW.size * 4)
                        state.textA = ByteArray(state.textW.size * 4)
                    else if (state.textA.size > state.textW.size * 4)
                        state.textA[state.textW.size * 4] = 0
                    textStrToUtf8(state.textA, state.textW)
                }

                // User callback
                if (flags has (Itf.CallbackCompletion or Itf.CallbackHistory or Itf.CallbackAlways)) {
                    callback!!
                    // The reason we specify the usage semantic (Completion/History) is that Completion needs to disable keyboard TABBING at the moment.
                    var eventFlag = Itf.None
                    var eventKey = Key.Count
                    when {
                        flags has Itf.CallbackCompletion && Key.Tab.isPressed -> {
                            eventFlag = Itf.CallbackCompletion
                            eventKey = Key.Tab
                        }
                        flags has Itf.CallbackHistory && Key.UpArrow.isPressed -> {
                            eventFlag = Itf.CallbackHistory
                            eventKey = Key.UpArrow
                        }
                        flags has Itf.CallbackHistory && Key.DownArrow.isPressed -> {
                            eventFlag = Itf.CallbackHistory
                            eventKey = Key.DownArrow
                        }
                        flags has Itf.CallbackEdit && state.edited -> eventFlag = Itf.CallbackEdit
                        flags has Itf.CallbackAlways -> eventFlag = Itf.CallbackAlways
                    }

                    if (eventFlag != Itf.None) {
                        val cbData = TextEditCallbackData()
                        cbData.eventFlag = eventFlag.i
                        cbData.flags = flags
                        cbData.userData = callbackUserData

                        cbData.eventKey = eventKey
                        cbData.buf = state.textA
                        cbData.bufTextLen = state.curLenA
                        cbData.bufSize = state.bufCapacityA
                        cbData.bufDirty = false

                        // We have to convert from wchar-positions to UTF-8-positions, which can be pretty slow (an incentive to ditch the ImWchar buffer, see https://github.com/nothings/stb/issues/188)
                        val text = state.textW
                        cbData.cursorPos = textCountUtf8BytesFromStr(text, state.stb.cursor)
                        val utf8CursorPos = cbData.cursorPos
                        cbData.selectionStart = textCountUtf8BytesFromStr(text, state.stb.selectStart)
                        val utf8SelectionStart = cbData.selectionStart
                        cbData.selectionEnd = textCountUtf8BytesFromStr(text, state.stb.selectEnd)
                        val utf8SelectionEnd = cbData.selectionEnd

                        // Call user code
                        callback(cbData)

                        // Read back what user may have modified
                        assert(cbData.buf === state.textA) { "Invalid to modify those fields" }
                        assert(cbData.bufSize == state.bufCapacityA)
                        assert(cbData.flags == flags)
                        if (cbData.cursorPos != utf8CursorPos) {
                            state.stb.cursor =
                                    textCountCharsFromUtf8(cbData.buf, cbData.cursorPos); state.cursorFollow = true; }
                        if (cbData.selectionStart != utf8SelectionStart) {
                            state.stb.selectStart = textCountCharsFromUtf8(cbData.buf, cbData.selectionStart); }
                        if (cbData.selectionEnd != utf8SelectionEnd) {
                            state.stb.selectEnd = textCountCharsFromUtf8(cbData.buf, cbData.selectionEnd); }
                        if (cbData.bufDirty) {
                            assert(cbData.bufTextLen == cbData.buf.strlen()) { "You need to maintain BufTextLen if you change the text!" }
                            if ((cbData.bufTextLen > backupCurrentTextLength) and isResizable) {
                                val newSize = state.textW.size + (cbData.bufTextLen - backupCurrentTextLength)
                                if (state.textW.size < newSize)
                                    state.textW = CharArray(newSize)
                                else if (state.textW.size > newSize)
                                    state.textW[newSize] = NUL
                            }
                            state.curLenW = textStrFromUtf8(state.textW, cbData.buf)
                            state.curLenA =
                                    cbData.bufTextLen  // Assume correct length and valid UTF-8 from user, saves us an extra strlen()
                            state.cursorAnimReset()
                        }
                    }
                }
                // Will copy result string if modified
                if (!isReadOnly && state.textA strcmp buf != 0) {
                    applyNewText = state.textA
                    applyNewTextLength = state.curLenA
                }
            }

            // Copy result to user buffer
            if (applyNewText != null) {
                // We cannot test for 'backup_current_text_length != apply_new_text_length' here because we have no guarantee that the size
                // of our owned buffer matches the size of the string object held by the user, and by design we allow InputText() to be used
                // without any storage on user's side.
                assert(applyNewTextLength >= 0)
                if (isResizable) {
                    val callbackData = InputTextCallbackData().also {
                        it.eventFlag = Itf.CallbackResize.i
                        it.flags = flags
                        it.buf = buf
                        it.bufTextLen = applyNewTextLength
                        it.bufSize = buf.size max applyNewTextLength
                        it.userData = callbackUserData
                    }
                    callback!!(callbackData)
                    buf = callbackData.buf
//                    buf_size = callback_data.BufSize; TODO?
                    applyNewTextLength = callbackData.bufTextLen min buf.size
                    assert(applyNewTextLength <= buf.size)
                }
                //IMGUI_DEBUG_LOG("InputText(\"%s\"): apply_new_text length %d\n", label, apply_new_text_length);

                // If the underlying buffer resize was denied or not carried to the next frame, apply_new_text_length+1 may be >= buf_size.
                System.arraycopy(applyNewText, 0, buf, 0, applyNewTextLength min buf.size)
                if (applyNewTextLength < buf.size) buf[applyNewTextLength] = 0
                valueChanged = true
            }

            // Clear temporary user storage
            state.apply {
                userFlags = 0
                userCallback = null
                userCallbackData = null
            }
        }
        // Release active ID at the end of the function (so e.g. pressing Return still does a final application of the value)
        if (clearActiveId && g.activeId == id) clearActiveID()

        // Render frame
        if (!isMultiline) {
            renderNavHighlight(frameBb, id)
            renderFrame(frameBb.min, frameBb.max, Col.FrameBg.u32, true, style.frameRounding)
        }

        val clipRect = Vec4(frameBb.min, frameBb.min + innerSize) // Not using frameBb.Max because we have adjusted size
        val drawPos = if (isMultiline) Vec2(drawWindow.dc.cursorPos) else frameBb.min + style.framePadding
        val textSize = Vec2()

        /*  Set upper limit of single-line InputTextEx() at 2 million characters strings. The current pathological worst case is a long line
            without any carriage return, which would makes ImFont::RenderText() reserve too many vertices and probably crash. Avoid it altogether.
            Note that we only use this limit on single-line InputText(), so a pathologically large line on a InputTextMultiline() would still crash. */
        val bufDisplayMaxLength = 2 * 1024 * 1024
        var bufDisplay = if (bufDisplayFromState) state!!.textA else buf
        var bufDisplayEnd = -1 // We have specialized paths below for setting the length
        if (isDisplayingHint) {
            bufDisplay = hint!!.toByteArray()
            bufDisplayEnd = hint.length
        }

        // Render text. We currently only render selection when the widget is active or while scrolling.
        // FIXME: We could remove the '&& render_cursor' to keep rendering selection when inactive.
        if (renderCursor || renderSelection) {

            state!!
            if (!isDisplayingHint)
                bufDisplayEnd = state.curLenA

            /*  Render text (with cursor and selection)
                This is going to be messy. We need to:
                    - Display the text (this alone can be more easily clipped)
                    - Handle scrolling, highlight selection, display cursor (those all requires some form of 1d->2d
                        cursor position calculation)
                    - Measure text height (for scrollbar)
                We are attempting to do most of that in **one main pass** to minimize the computation cost
                (non-negligible for large amount of text) + 2nd pass for selection rendering (we could merge them by an
                extra refactoring effort)   */
            // FIXME: This should occur on bufDisplay but we'd need to maintain cursor/select_start/select_end for UTF-8.
            val text = state.textW
            val cursorOffset = Vec2()
            val selectStartOffset = Vec2()

            run {
                // Find lines numbers straddling 'cursor' (slot 0) and 'select_start' (slot 1) positions.
                val searchesInputPtr = IntArray(2) { -1 }
                val searchesResultLineNo = intArrayOf(-1000, -1000)
                var searchesRemaining = 0
                if (renderCursor) {
                    searchesInputPtr[0] = state.stb.cursor
                    searchesResultLineNo[0] = -1
                    searchesRemaining++
                }
                if (renderSelection) {
                    searchesInputPtr[1] = state.stb.selectStart min state.stb.selectEnd
                    searchesResultLineNo[1] = -1
                    searchesRemaining++
                }

                // Iterate all lines to find our line numbers
                // In multi-line mode, we never exit the loop until all lines are counted, so add one extra to the searchesRemaining counter.
                if (isMultiline) searchesRemaining++
                var lineCount = 0
                var s = 0
                while (s < text.size && text[s] != NUL)
                    if (text[s++] == '\n') {
                        lineCount++
                        if (searchesResultLineNo[0] == -1 && s > searchesInputPtr[0]) {
                            searchesResultLineNo[0] = lineCount; if (--searchesRemaining <= 0) break
                        }
                        if (searchesResultLineNo[1] == -1 && s > searchesInputPtr[1]) {
                            searchesResultLineNo[1] = lineCount; if (--searchesRemaining <= 0) break
                        }
                    }
                lineCount++
                if (searchesResultLineNo[0] == -1)
                    searchesResultLineNo[0] = lineCount
                if (searchesResultLineNo[1] == -1)
                    searchesResultLineNo[1] = lineCount

                // Calculate 2d position by finding the beginning of the line and measuring distance
                var start = text beginOfLine searchesInputPtr[0]
                cursorOffset.x = inputTextCalcTextSizeW(text, start, searchesInputPtr[0]).x
                cursorOffset.y = searchesResultLineNo[0] * g.fontSize
                if (searchesResultLineNo[1] >= 0) {
                    start = text beginOfLine searchesInputPtr[1]
                    selectStartOffset.x = inputTextCalcTextSizeW(text, start, searchesInputPtr[1]).x
                    selectStartOffset.y = searchesResultLineNo[1] * g.fontSize
                }

                // Store text height (note that we haven't calculated text width at all, see GitHub issues #383, #1224)
                if (isMultiline)
                    textSize.put(innerSize.x, lineCount * g.fontSize)
            }

            // Scroll
            if (renderCursor && state.cursorFollow) {
                // Horizontal scroll in chunks of quarter width
                if (flags hasnt Itf.NoHorizontalScroll) {
                    val scrollIncrementX = innerSize.x * 0.25f
                    if (cursorOffset.x < state.scrollX)
                        state.scrollX = floor(glm.max(0f, cursorOffset.x - scrollIncrementX))
                    else if (cursorOffset.x - innerSize.x >= state.scrollX)
                        state.scrollX = floor(cursorOffset.x - innerSize.x + scrollIncrementX)
                } else
                    state.scrollX = 0f

                // Vertical scroll
                if (isMultiline) {
                    // Test if cursor is vertically visible
                    if (cursorOffset.y - g.fontSize < scrollY)
                        scrollY = glm.max(0f, cursorOffset.y - g.fontSize)
                    else if (cursorOffset.y - innerSize.y >= scrollY)
                        scrollY = cursorOffset.y - innerSize.y + style.framePadding.y * 2f
                    val scrollMaxY = ((textSize.y + style.framePadding.y * 2f) - innerSize.y) max 0f
                    scrollY = clamp(scrollY, 0f, scrollMaxY)
                    drawPos.y += drawWindow.scroll.y - scrollY   // Manipulate cursor pos immediately avoid a frame of lag
                    drawWindow.scroll.y = scrollY
                }

                state.cursorFollow = false
            }

            // Draw selection
            val drawScroll = Vec2(state.scrollX, 0f)
            if (renderSelection) {

                val textSelectedBegin = state.stb.selectStart min state.stb.selectEnd
                val textSelectedEnd = state.stb.selectStart max state.stb.selectEnd

                val bgColor = getColorU32(Col.TextSelectedBg, if (renderCursor) 1f else 0.6f) // FIXME: current code flow mandate that render_cursor is always true here, we are leaving the transparent one for tests.
                val bgOffYUp = if (isMultiline) 0f else -1f // FIXME: those offsets should be part of the style? they don't play so well with multi-line selection.
                val bgOffYDn = if (isMultiline) 0f else 2f
                val rectPos = drawPos + selectStartOffset - drawScroll
                var p = textSelectedBegin
                while (p < textSelectedEnd) {
                    if (rectPos.y > clipRect.w + g.fontSize) break
                    if (rectPos.y < clipRect.y) {
                        while (p < textSelectedEnd)
                            if (text[p++] == '\n')
                                break
                    } else {
                        val rectSize = withInt {
                            inputTextCalcTextSizeW(text, p, textSelectedEnd, it, stopOnNewLine = true).also { p = it() }
                        }
                        // So we can see selected empty lines
                        if (rectSize.x <= 0f) rectSize.x = floor(g.font.getCharAdvance(' ') * 0.5f)
                        val rect = Rect(rectPos + Vec2(0f, bgOffYUp - g.fontSize), rectPos + Vec2(rectSize.x, bgOffYDn))
                        val clipRect_ = Rect(clipRect)
                        rect clipWith clipRect_
                        if (rect overlaps clipRect_)
                            drawWindow.drawList.addRectFilled(rect.min, rect.max, bgColor)
                    }
                    rectPos.x = drawPos.x - drawScroll.x
                    rectPos.y += g.fontSize
                }
            }

            // We test for 'buf_display_max_length' as a way to avoid some pathological cases (e.g. single-line 1 MB string) which would make ImDrawList crash.
            if (isMultiline || bufDisplayEnd < bufDisplayMaxLength) {
                val col = getColorU32(if (isDisplayingHint) Col.TextDisabled else Col.Text)
                drawWindow.drawList.addText(g.font, g.fontSize, drawPos - drawScroll, col, bufDisplay, 0,
                        bufDisplayEnd, 0f, clipRect.takeUnless { isMultiline })
            }

            // Draw blinking cursor
            if (renderCursor) {
                state.cursorAnim += io.deltaTime
                val cursorIsVisible = !io.configInputTextCursorBlink || state.cursorAnim <= 0f || glm.mod(state.cursorAnim, 1.2f) <= 0.8f
                val cursorScreenPos = drawPos + cursorOffset - drawScroll
                val cursorScreenRect = Rect(cursorScreenPos.x, cursorScreenPos.y - g.fontSize + 0.5f,
                        cursorScreenPos.x + 1f, cursorScreenPos.y - 1.5f)
                if (cursorIsVisible && cursorScreenRect overlaps clipRect)
                    drawWindow.drawList.addLine(cursorScreenRect.min, cursorScreenRect.bl, Col.Text.u32)

                // Notify OS of text input position for advanced IME (-1 x offset so that Windows IME can cover our cursor. Bit of an extra nicety.)
                if (!isReadOnly) {
                    g.platformImePos.put(cursorScreenPos.x - 1f, cursorScreenPos.y - g.fontSize)
                    g.platformImePosViewport = window.viewport
                }
            }
        } else {
            // Render text only (no selection, no cursor)
            if (isMultiline) {
                val (lineCount, textEnd) = inputTextCalcTextLenAndLineCount(bufDisplay)
                bufDisplayEnd = textEnd
                textSize.put(innerSize.x, lineCount * g.fontSize) // We don't need width
            } else if (!isDisplayingHint && g.activeId == id)
                bufDisplayEnd = state!!.curLenA
            else if (!isDisplayingHint)
                bufDisplayEnd = bufDisplay.strlen()

            if (isMultiline || bufDisplayEnd < bufDisplayMaxLength) {
                val col = getColorU32(if (isDisplayingHint) Col.TextDisabled else Col.Text)
                drawWindow.drawList.addText(g.font, g.fontSize, drawPos, col, bufDisplay, 0, bufDisplayEnd,
                        0f, clipRect.takeUnless { isMultiline })
            }
        }

        if (isPassword && !isDisplayingHint)
            popFont()

        if (isMultiline) {
            dummy(textSize)
            endChild()
            endGroup()
        }

        // Log as text
        if (g.logEnabled && (!isPassword || isDisplayingHint))
            logRenderedText(drawPos, String(bufDisplay, 0, bufDisplayEnd), bufDisplayEnd)

        if (labelSize.x > 0)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, frameBb.min.y + style.framePadding.y), label)

        if (valueChanged && flags hasnt Itf._NoMarkEdited)
            markItemEdited(id)

        Hook.itemInfo?.invoke(g, id, label, window.dc.itemFlags)
        return when {
            flags has Itf.EnterReturnsTrue -> enterPressed
            else -> valueChanged
        }
    }

    /** Create text input in place of another active widget (e.g. used when doing a CTRL+Click on drag/slider widgets)
     *  FIXME: Facilitate using this in variety of other situations. */
    fun tempInputText(bb: Rect, id: ID, label: String, buf: ByteArray, flags: InputTextFlags): Boolean {
        // On the first frame, g.TempInputTextId == 0, then on subsequent frames it becomes == id.
        // We clear ActiveID on the first frame to allow the InputText() taking it back.
        val init = g.tempInputId != id

        if (init)
            clearActiveID()

        g.currentWindow!!.dc.cursorPos put bb.min
        val valueChanged = inputTextEx(label, null, buf, bb.size, flags)
        if (init) {
            // First frame we started displaying the InputText widget, we expect it to take the active id.
            assert(g.activeId == id)
            g.tempInputId = g.activeId
        }
        return valueChanged
    }

    /** Note that Drag/Slider functions are only forwarding the min/max values clamping values if the
     *  ImGuiSliderFlags_AlwaysClamp flag is set!
     *  This is intended: this way we allow CTRL+Click manual input to set a value out of bounds, for maximum flexibility.
     *  However this may not be ideal for all uses, as some user code may break on out of bound values. */
    fun <N> tempInputScalar(
            bb: Rect, id: ID, label: String, dataType: DataType, pData: KMutableProperty0<N>,
            format_: String, clampMin_: N? = null, clampMax_: N? = null
    ): Boolean
            where N : Number, N : Comparable<N> {

        var clampMin = clampMin_
        var clampMax = clampMax_

        // On the first frame, g.TempInputTextId == 0, then on subsequent frames it becomes == id.
        // We clear ActiveID on the first frame to allow the InputText() taking it back.
        val init = g.tempInputId != id
        if (init)
            clearActiveID()

        val format = parseFormatTrimDecorations(format_)
        val dataBuf = pData.format(dataType, format).trim()

        val flags = Itf.AutoSelectAll or Itf._NoMarkEdited or when (dataType) {
            DataType.Float, DataType.Double -> Itf.CharsScientific
            else -> Itf.CharsDecimal
        }
        val buf = dataBuf.toByteArray(32)
        var valueChanged = false
        if (tempInputText(bb, id, label, buf, flags)) {
            // Backup old value
            val dataBackup = pData()

            // Apply new value (or operations) then clamp
            dataTypeApplyOpFromText(buf.cStr, g.inputTextState.initialTextA, dataType, pData)
            if (clampMin != null && clampMax != null) {
                if (clampMin > clampMax) {
                    val t = clampMin
                    clampMin = clampMax
                    clampMax = t
                }
                dataTypeClamp(dataType, pData, clampMin, clampMax)
            }

            // Only mark as edited if new value is different
            valueChanged = dataBackup != pData()

            if (valueChanged)
                markItemEdited(id)
        }
        return valueChanged
    }

    fun tempInputIsActive(id: ID): Boolean = g.activeId == id && g.tempInputId == id

    fun getInputTextState(id: ID): InputTextState? =
            g.inputTextState.takeIf { it.id == id } // Get input text state if active

    companion object {
        /** Return false to discard a character.    */
        fun inputTextFilterCharacter(
                char: KMutableProperty0<Char>,
                flags: InputTextFlags,
                callback: InputTextCallback?,
                userData: Any?
        ): Boolean {

            var c by char

            // Filter non-printable (NB: isprint is unreliable! see #2467) [JVM we can rely on custom ::isPrintable]
            if (c < 0x20 && !c.isPrintable) {
                var pass = false
                pass = pass or (c == '\n' && flags has Itf._Multiline)
                pass = pass or (c == '\t' && flags has Itf.AllowTabInput)
                if (!pass) return false
            }

            // We ignore Ascii representation of delete (emitted from Backspace on OSX, see #2578, #2817)
            if (c.i == 127)
                return false

            // Filter private Unicode range. GLFW on OSX seems to send private characters for special keys like arrow keys (FIXME)
            if (c >= 0xE000 && c <= 0xF8FF) return false

            // Filter Unicode ranges we are not handling in this build.
            if (c > UNICODE_CODEPOINT_MAX)
                return false

            // Generic named filters
            if (flags has (Itf.CharsDecimal or Itf.CharsHexadecimal or Itf.CharsUppercase or Itf.CharsNoBlank or Itf.CharsScientific)) {

                // The libc allows overriding locale, with e.g. 'setlocale(LC_NUMERIC, "de_DE.UTF-8");' which affect the output/input of printf/scanf.
                // The standard mandate that programs starts in the "C" locale where the decimal point is '.'.
                // We don't really intend to provide widespread support for it, but out of empathy for people stuck with using odd API, we support the bare minimum aka overriding the decimal point.
                // Change the default decimal_point with:
                //   ImGui::GetCurrentContext()->PlatformLocaleDecimalPoint = *localeconv()->decimal_point;
                val cDecimalPoint = g.platformLocaleDecimalPoint

                // Allow 0-9 . - + * /
                if (flags has Itf.CharsDecimal)
                    if (c !in '0'..'9' && c != cDecimalPoint && c != '-' && c != '+' && c != '*' && c != '/')
                        return false

                // Allow 0-9 . - + * / e E
                if (flags has Itf.CharsScientific)
                    if (c !in '0'..'9' && c != cDecimalPoint && c != '-' && c != '+' && c != '*' && c != '/' && c != 'e' && c != 'E')
                        return false

                // Allow 0-9 a-F A-F
                if (flags has Itf.CharsHexadecimal)
                    if (c !in '0'..'9' && c !in 'a'..'f' && c !in 'A'..'F')
                        return false

                // Turn a-z into A-Z
                if (flags has Itf.CharsUppercase && c in 'a'..'z')
                    c = c + ('A' - 'a')

                if (flags has Itf.CharsNoBlank && c.isBlankW)
                    return false
            }

            // Custom callback filter
            if (flags has Itf.CallbackCharFilter) {
                callback!! //callback is non-null from all calling functions
                val itcd = InputTextCallbackData()
                itcd.eventFlag = Itf.CallbackCharFilter.i
                itcd.eventChar = c
                itcd.flags = flags
                itcd.userData = userData

                if (callback(itcd))
                    return false
                if (itcd.eventChar == NUL)
                    return false
            }
            return true
        }

        /** @return [JVM] [lineCount, textEnd] */
        fun inputTextCalcTextLenAndLineCount(text: ByteArray): Pair<Int, Int> {
            if (text.isEmpty()) return 1 to 0
            var lineCount = 0
            var s = 0
            var c = text[s++]
            while (c != 0.b) { // We are only matching for \n so we can ignore UTF-8 decoding
                if (c == '\n'.b)
                    lineCount++
                c = text[s++]
            }
            s--
            if (text[s] != '\n'.b && text[s] != '\r'.b)
                lineCount++
            return lineCount to s
        }

        fun inputTextCalcTextSizeW(
                text: CharArray, textBegin: Int, textEnd: Int,
                remaining: KMutableProperty0<Int>? = null, outOffset: Vec2? = null,
                stopOnNewLine: Boolean = false,
        ): Vec2 {

            val font = g.font
            val lineHeight = g.fontSize
            val scale = lineHeight / font.fontSize

            val textSize = Vec2()
            var lineWidth = 0f

            var s = textBegin
            while (s < textEnd) {
                val c = text[s++]
                if (c == '\n') {
                    textSize.x = glm.max(textSize.x, lineWidth)
                    textSize.y += lineHeight
                    lineWidth = 0f
                    if (stopOnNewLine)
                        break
                    continue
                }
                if (c == '\r') continue
                // renaming ::getCharAdvance continuously every build because of bug, https://youtrack.jetbrains.com/issue/KT-19612
                val charWidth = font.getCharAdvance(c) * scale
                lineWidth += charWidth
            }

            if (textSize.x < lineWidth)
                textSize.x = lineWidth

            // offset allow for the possibility of sitting after a trailing \n
            outOffset?.let {
                it.x = lineWidth
                it.y = textSize.y + lineHeight
            }

            if (lineWidth > 0 || textSize.y == 0f)  // whereas size.y will ignore the trailing \n
                textSize.y += lineHeight

            remaining?.set(s)

            return textSize
        }
    }
}