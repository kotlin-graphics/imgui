package imgui.imgui

import gli.hasnt
import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.getColorU32
import imgui.ImGui.getContentRegionMax
import imgui.internal.*
import imgui.Context as g

// We should always have a CurrentWindow in the stack (there is an implicit "Debug" window)
// If this ever crash because g.CurrentWindow is NULL it means that either
// - ImGui::NewFrame() has never been called, which is illegal.
// - You are calling ImGui functions after ImGui::Render() and before the next ImGui::NewFrame(), which is also illegal.

interface imgui_internal {

    fun getCurrentWindowRead() = g.currentWindow

    fun getCurrentWindow(): Window {
        g.currentWindow!!.accessed = true
        return g.currentWindow!!
    }
//IMGUI_API ImGuiWindow*  GetParentWindow();

    fun findWindowByName(name: String): Window? {
        // FIXME-OPT: Store sorted hashes -> pointers so we can do a bissection in a contiguous block
        val id = hash(name, 0)
        return g.windows.firstOrNull { it.id == id }
    }

    /** Moving window to front of display (which happens to be back of our sorted list) */
    fun focusWindow(window: Window?) {

        // Always mark the window we passed as focused. This is used for keyboard interactions such as tabbing.
        g.focusedWindow = window

        // Passing NULL allow to disable keyboard focus
        if (window == null) return

        // And move its root window to the top of the pile
//    if (window.rootWindow) TODO check
        val window = window.rootWindow

        // Steal focus on active widgets
        if (window.flags has WindowFlags.Popup) // FIXME: This statement should be unnecessary. Need further testing before removing it..
            if (g.activeId != 0 && g.activeIdWindow != null && g.activeIdWindow!!.rootWindow != window)
                clearActiveId()

        // Bring to front
        if ((window.flags has WindowFlags.NoBringToFrontOnFocus) || g.windows.last() == window)
            return
        g.windows.remove(window)
        g.windows.add(window)
    }

    /** Ends the ImGui frame. Automatically called by Render()! you most likely don't need to ever call that yourself
     *  directly. If you don't need to render you can call EndFrame() but you'll have wasted CPU already. If you don't
     *  need to render, don't create any windows instead!
     *
     *  This is normally called by Render(). You may want to call it directly if you want to avoid calling Render() but
     *  the gain will be very minimal.  */
    fun endFrame() {

        assert(g.initialized)                       // Forgot to call ImGui::NewFrame()
        assert(g.frameCountEnded != g.frameCount)   // ImGui::EndFrame() called multiple times, or forgot to call ImGui::NewFrame() again

        // Render tooltip
        if (g.tooltip.isNotEmpty() && g.tooltip[0] != '\u0000') {
            ImGui.beginTooltip()
            ImGui.textUnformatted(g.tooltip)
            ImGui.endTooltip()
        }

        // Notify OS when our Input Method Editor cursor has moved (e.g. CJK inputs using Microsoft IME)
//        if (IO.imeSetInputScreenPosFn && ImLengthSqr(g.OsImePosRequest - g.OsImePosSet) > 0.0001f) { TODO
//            g.IO.ImeSetInputScreenPosFn((int) g . OsImePosRequest . x, (int) g . OsImePosRequest . y)
//            g.OsImePosSet = g.OsImePosRequest
//        }

        // Hide implicit "Debug" window if it hasn't been used
        assert(g.currentWindowStack.size == 1)    // Mismatched Begin()/End() calls
        g.currentWindow?.let {
            if (!it.accessed) it.active = false
        }

        ImGui.end()

        // Click to focus window and start moving (after we're done with all our widgets)
        if (g.activeId == 0 && g.hoveredId == 0 && IO.mouseClicked[0]) {
            // Unless we just made a popup appear
            if (!(g.focusedWindow != null && !g.focusedWindow!!.wasActive && g.focusedWindow!!.active)) {
                if (g.hoveredRootWindow != null) {
                    focusWindow(g.hoveredWindow)
                    if (g.hoveredWindow!!.flags hasnt WindowFlags.NoMove) {
                        g.movedWindow = g.hoveredWindow
                        g.movedWindowMoveId = g.hoveredRootWindow!!.moveId
                        setActiveId(g.movedWindowMoveId, g.hoveredRootWindow)
                    }
                } else if (g.focusedWindow != null && getFrontMostModalRootWindow() == null)
                    focusWindow(null)   // Clicking on void disable focus
            }
        }

        /*  Sort the window list so that all child windows are after their parent
            We cannot do that on FocusWindow() because childs may not exist yet         */
        g.windowsSortBuffer.clear()
        g.windows.forEach {
            if (!it.active || it.flags hasnt WindowFlags.ChildWindow)  // if a child is active its parent will add it
                addWindowToSortedBuffer(g.windowsSortBuffer, it)
        }
        assert(g.windows.size == g.windowsSortBuffer.size)  // we done something wrong
        g.windows.clear()
        g.windows.addAll(g.windowsSortBuffer)

        // Clear Input data for next frame
        IO.mouseWheel = 0f
        IO.inputCharacters.fill('\u0000')

        g.frameCountEnded = g.frameCount
    }

    fun setActiveId(id: Int, window: Window?) {
        g.activeId = id
        g.activeIdAllowOverlap = false
        g.activeIdIsJustActivated = true
        if (id != 0)
            g.activeIdIsAlive = true
        g.activeIdWindow = window
    }

    fun clearActiveId() = setActiveId(0, null)

    fun setHoveredId(id: Int) {
        g.hoveredId = id
        g.hoveredIdAllowOverlap = false
    }

    fun keepAliveId(id: Int) {
        if (g.activeId == id)
            g.activeIdIsAlive = true
    }

    /** Advance cursor given item size for layout.  */
    fun itemSize(size: Vec2, textOffsetY: Float = 0f) {

        val window = getCurrentWindow()
        if (window.skipItems) return

        // Always align ourselves on pixel boundaries
        val lineHeight = glm.max(window.dc.currentLineHeight, size.y)
        val textBaseOffset = glm.max(window.dc.currentLineTextBaseOffset, textOffsetY)
        window.dc.cursorPosPrevLine = Vec2(window.dc.cursorPos.x + size.x, window.dc.cursorPos.y)
        window.dc.cursorPos.x = (window.pos.x + window.dc.indentX + window.dc.columnsOffsetX).i.f
        window.dc.cursorPos.y = (window.dc.cursorPos.y + lineHeight + Style.itemSpacing.y).i.f
        window.dc.cursorMaxPos.x = glm.max(window.dc.cursorMaxPos.x, window.dc.cursorPosPrevLine.x)
        window.dc.cursorMaxPos.y = glm.max(window.dc.cursorMaxPos.y, window.dc.cursorPos.y)

        //window->DrawList->AddCircle(window->DC.CursorMaxPos, 3.0f, IM_COL32(255,0,0,255), 4); // Debug

        window.dc.prevLineHeight = lineHeight
        window.dc.prevLineTextBaseOffset = textBaseOffset
        window.dc.currentLineTextBaseOffset = 0f
        window.dc.currentLineHeight = 0f
    }

    fun itemSize(bb: Rect, textOffsetY: Float = 0f) = itemSize(bb.size, textOffsetY)

    /** Declare item bounding box for clipping and interaction.
     *  Note that the size can be different than the one provided to ItemSize(). Typically, widgets that spread over
     *  available surface declares their minimum size requirement to ItemSize() and then use a larger region for
     *  drawing/interaction, which is passed to ItemAdd().  */
    fun itemAdd(bb: Rect, id: Int?): Boolean {

        val window = getCurrentWindow()
        with(window.dc) {
            lastItemId = id ?: 0
            lastItemRect = Rect(bb)
            lastItemHoveredRect = false
            lastItemHoveredAndUsable = false
        }
        if (isClippedEx(bb, id, false)) return false

        // This is a sensible default, but widgets are free to override it after calling ItemAdd()
        if (isMouseHoveringRect(bb)) {
            /*  Matching the behavior of IsHovered() but allow if ActiveId==window->MoveID (we clicked on the window
                background)
                So that clicking on items with no active id such as Text() still returns true with IsItemHovered()  */
            window.dc.lastItemHoveredRect = true
            if (g.hoveredRootWindow == window.rootWindow)
                if (g.activeId == 0 || (id != 0 && g.activeId == id) || g.activeIdAllowOverlap || (g.activeId == window.moveId))
                    if (isWindowContentHoverable(window))
                        window.dc.lastItemHoveredAndUsable = true
        }

        return true
    }

    fun isClippedEx(bb: Rect, id: Int?, clipEvenWhenLogged: Boolean): Boolean {

        val window = getCurrentWindowRead()!!

        if (!bb.overlaps(window.clipRect))
            if (id == null || id != g.activeId)
                if (clipEvenWhenLogged || !g.logEnabled)
                    return true
        return false
    }

    /** NB: This is an internal helper. The user-facing IsItemHovered() is using data emitted from ItemAdd(), with a
     *  slightly different logic.   */
    fun isHovered(bb: Rect, id: Int, flattenChilds: Boolean = false): Boolean {

        if (g.hoveredId == 0 || g.hoveredId == id || g.hoveredIdAllowOverlap) {
            val window = getCurrentWindowRead()!!
            if (g.hoveredWindow == window || (flattenChilds && g.hoveredRootWindow == window.rootWindow))
                if ((g.activeId == 0 || g.activeId == id || g.activeIdAllowOverlap) && isMouseHoveringRect(bb))
                    if (isWindowContentHoverable(g.hoveredRootWindow!!))
                        return true
        }
        return false
    }

    /** Return true if focus is requested   */
    fun focusableItemRegister(window: Window, isActive: Boolean, tabStop: Boolean = true): Boolean {

        val allowKeyboardFocus = window.dc.allowKeyboardFocus
        window.focusIdxAllCounter++
        if (allowKeyboardFocus)
            window.focusIdxTabCounter++

        // Process keyboard input at this point: TAB, Shift-TAB switch focus
        // We can always TAB out of a widget that doesn't allow tabbing in.
        if (tabStop && window.focusIdxAllRequestNext == Int.MAX_VALUE && window.focusIdxTabRequestNext == Int.MAX_VALUE && isActive
                && isKeyPressedMap(Key.Tab))
        // Modulo on index will be applied at the end of frame once we've got the total counter of items.
            window.focusIdxTabRequestNext = window.focusIdxTabCounter + (if (IO.keyShift) (if (allowKeyboardFocus) -1 else 0) else +1)

        if (window.focusIdxAllCounter == window.focusIdxAllRequestCurrent) return true

        return allowKeyboardFocus && window.focusIdxTabCounter == window.focusIdxTabRequestCurrent
    }
//IMGUI_API void          FocusableItemUnregister(ImGuiWindow* window);
//IMGUI_API ImVec2        CalcItemSize(ImVec2 size, float default_x, float default_y);

    fun calcWrapWidthForPos(pos: Vec2, wrapPosX: Float): Float {

        if (wrapPosX < 0f) return 0f

        val window = getCurrentWindowRead()!!
        var wrapPosX = wrapPosX
        if (wrapPosX == 0f)
            wrapPosX = getContentRegionMax().x + window.pos.x
        else if (wrapPosX > 0f)
            wrapPosX += window.pos.x - window.scroll.x // wrap_pos_x is provided is window local space

        return glm.max(wrapPosX - pos.x, 1f)
    }

//IMGUI_API void          OpenPopupEx(const char* str_id, bool reopen_existing);
//
//// NB: All position are in absolute pixels coordinates (not window coordinates)
//// FIXME: All those functions are a mess and needs to be refactored into something decent. AVOID USING OUTSIDE OF IMGUI.CPP! NOT FOR PUBLIC CONSUMPTION.
//// We need: a sort of symbol library, preferably baked into font atlas when possible + decent text rendering helpers.

    /** Internal ImGui functions to render text
     *  RenderText***() functions calls ImDrawList::AddText() calls ImBitmapFont::RenderText()  */
    fun renderText(pos: Vec2, text: String, textEnd: Int = text.length, hideTextAfterHash: Boolean = true) {

        val window = getCurrentWindow()

        // Hide anything after a '##' string
        val textDisplayEnd =
                if (hideTextAfterHash)
                    findRenderedTextEnd(text, textEnd)
                else
                    if (textEnd == 0) text.length else textEnd

        if (textDisplayEnd > 0) {
            window.drawList.addText(g.font, g.fontSize, pos, getColorU32(Col.Text), text, textDisplayEnd)
            if (g.logEnabled)
                logRenderedText(pos, text, textDisplayEnd)
        }
    }

    fun renderTextWrapped(pos: Vec2, text: String, textEnd: Int, wrapWidth: Float) {

        val window = getCurrentWindow()

        var textEnd = textEnd
        if (textEnd == 0)
            textEnd = text.length // FIXME-OPT

        if (textEnd > 0) {
            window.drawList.addText(g.font, g.fontSize, pos, getColorU32(Col.Text), text, textEnd, wrapWidth)
            if (g.logEnabled)
                logRenderedText(pos, text, textEnd)
        }
    }


    /** Default clipRect uses (pos_min,pos_max)
     *  Handle clipping on CPU immediately (vs typically let the GPU clip the triangles that are overlapping the clipping
     *  rectangle edges)    */
    fun renderTextClipped(posMin: Vec2, posMax: Vec2, text: String, textEnd: Int, textSizeIfKnown: Vec2?, align: Vec2 = Vec2(),
                          clipRect: Rect? = null) {
        // Hide anything after a '##' string
        val textDisplayEnd = findRenderedTextEnd(text, textEnd)
        if (textDisplayEnd == 0) return

        val window = getCurrentWindow()

        // Perform CPU side clipping for single clipped element to avoid using scissor state
        val pos = Vec2(posMin)
        val textSize = textSizeIfKnown ?: calcTextSize(text, textDisplayEnd, false, 0f)

        val clipMin = clipRect?.min ?: posMin
        val clipMax = clipRect?.max ?: posMax
        var needClipping = (pos.x + textSize.x >= clipMax.x) || (pos.y + textSize.y >= clipMax.y)
        clipRect?.let {
            // If we had no explicit clipping rectangle then pos==clipMin
            needClipping = needClipping || (pos.x < clipMin.x || pos.y < clipMin.y)
        }

        // Align whole block. We should defer that to the better rendering function when we'll have support for individual line alignment.
        if (align.x > 0f) pos.x = glm.max(pos.x, pos.x + (posMax.x - pos.x - textSize.x) * align.x)
        if (align.y > 0f) pos.y = glm.max(pos.y, pos.y + (posMax.y - pos.y - textSize.y) * align.y)

        // Render
        if (needClipping) {
            val fineClipRect = Vec4(clipMin.x, clipMin.y, clipMax.x, clipMax.y)
            window.drawList.addText(g.font, g.fontSize, pos, ImGui.getColorU32(Col.Text), text, textDisplayEnd, 0f, fineClipRect)
        } else {
            window.drawList.addText(g.font, g.fontSize, pos, ImGui.getColorU32(Col.Text), text, textDisplayEnd, 0f, null)
        }
//    if (g.logEnabled) TODO
//        LogRenderedText(pos, text, textDisplayEnd)
    }

    /** Render a rectangle shaped with optional rounding and borders    */
    fun renderFrame(pMin: Vec2, pMax: Vec2, fillCol: Int, border: Boolean = true, rounding: Float = 0f) {

        val window = getCurrentWindow()

        window.drawList.addRectFilled(pMin, pMax, fillCol, rounding)
        if (border && window.flags has WindowFlags.showBorders) {
            window.drawList.addRect(pMin + 1, pMax + 1, ImGui.getColorU32(Col.BorderShadow), rounding)
            window.drawList.addRect(pMin, pMax, ImGui.getColorU32(Col.Border), rounding)
        }
    }

    /** Render a triangle to denote expanded/collapsed state    */
    fun renderCollapseTriangle(pMin: Vec2, isOpen: Boolean, scale: Float = 1.0f) {

        val window = getCurrentWindow()

        val h = g.fontSize * 1f
        val r = h * 0.4f * scale
        val center = pMin + Vec2(h * 0.5f, h * 0.5f * scale)

        val a: Vec2
        val b: Vec2
        val c: Vec2
        if (isOpen) {
            center.y -= r * 0.25f
            a = center + Vec2(0, 1) * r
            b = center + Vec2(-0.866f, -0.5f) * r
            c = center + Vec2(+0.866f, -0.5f) * r
        } else {
            a = center + Vec2(1, 0) * r
            b = center + Vec2(-0.500f, +0.866f) * r
            c = center + Vec2(-0.500f, -0.866f) * r
        }

        window.drawList.addTriangleFilled(a, b, c, ImGui.getColorU32(Col.Text))
    }

//IMGUI_API void          RenderBullet(ImVec2 pos);
//IMGUI_API void          RenderCheckMark(ImVec2 pos, ImU32 col);

    /** Find the optional ## from which we stop displaying text.    */
    fun findRenderedTextEnd(text: String, textEnd: Int = text.length): Int {
        val textEnd = if (textEnd == 0) text.length else textEnd
        var textDisplayEnd = 0
        while (textDisplayEnd < textEnd && (text[textDisplayEnd + 0] != '#' || text[textDisplayEnd + 1] != '#'))
            textDisplayEnd++
        return textDisplayEnd
    }


    fun buttonBehavior(bb: Rect, id: Int, flags: ButtonFlags) = buttonBehavior(bb, id, flags.i)

    fun buttonBehavior(bb: Rect, id: Int, flags: Int = 0): BooleanArray {

        val window = getCurrentWindow()
        var flags = flags

        if (flags has ButtonFlags.Disabled) {
            if (g.activeId == id) clearActiveId()
            return BooleanArray(3)
        }

        if (flags hasnt (ButtonFlags.PressedOnClickRelease or ButtonFlags.PressedOnClick or ButtonFlags.PressedOnRelease or
                ButtonFlags.PressedOnDoubleClick))
            flags = flags or ButtonFlags.PressedOnClickRelease

        var pressed = false
        var hovered = isHovered(bb, id, flags has ButtonFlags.FlattenChilds)
        if (hovered) {
            setHoveredId(id)
            if (flags hasnt ButtonFlags.NoKeyModifiers || (!IO.keyCtrl && !IO.keyShift && !IO.keyAlt)) {

                /*                         | CLICKING        | HOLDING with ImGuiButtonFlags_Repeat
                PressedOnClickRelease  |  <on release>*  |  <on repeat> <on repeat> .. (NOT on release)  <-- MOST COMMON!
                                                                        (*) only if both click/release were over bounds
                PressedOnClick         |  <on click>     |  <on click> <on repeat> <on repeat> ..
                PressedOnRelease       |  <on release>   |  <on repeat> <on repeat> .. (NOT on release)
                PressedOnDoubleClick   |  <on dclick>    |  <on dclick> <on repeat> <on repeat> ..   */
                if (flags has ButtonFlags.PressedOnClickRelease && IO.mouseClicked[0]) {
                    setActiveId(id, window) // Hold on ID
                    focusWindow(window)
                    g.activeIdClickOffset = IO.mousePos - bb.min
                }
                if ((flags has ButtonFlags.PressedOnClick && IO.mouseClicked[0]) || (flags has ButtonFlags.PressedOnDoubleClick &&
                        IO.mouseDoubleClicked[0])) {
                    pressed = true
                    clearActiveId()
                    focusWindow(window)
                }
                if (flags has ButtonFlags.PressedOnRelease && IO.mouseReleased[0]) {
                    // Repeat mode trumps <on release>
                    if (!(flags has ButtonFlags.Repeat && IO.mouseDownDurationPrev[0] >= IO.keyRepeatDelay))
                        pressed = true
                    clearActiveId()
                }

                /*  'Repeat' mode acts when held regardless of _PressedOn flags (see table above).
                Relies on repeat logic of IsMouseClicked() but we may as well do it ourselves if we end up exposing
                finer RepeatDelay/RepeatRate settings.  */
                if (flags has ButtonFlags.Repeat && g.activeId == id && IO.mouseDownDuration[0] > 0f && isMouseClicked(0, true))
                    pressed = true
            }
        }
        var held = false
        if (g.activeId == id)
            if (IO.mouseDown[0])
                held = true
            else {
                if (hovered && flags has ButtonFlags.PressedOnClickRelease)
                // Repeat mode trumps <on release>
                    if (!(flags has ButtonFlags.Repeat && IO.mouseDownDurationPrev[0] >= IO.keyRepeatDelay))
                        pressed = true
                clearActiveId()
            }
        /*  AllowOverlap mode (rarely used) requires previous frame HoveredId to be null or to match. This allows using
        patterns where a later submitted widget overlaps a previous one.    */
        if (hovered && flags has ButtonFlags.AllowOverlapMode && (g.hoveredIdPreviousFrame != id && g.hoveredIdPreviousFrame != 0)) {
            held = false
            pressed = false
            hovered = false
        }
        return booleanArrayOf(pressed, hovered, held)
    }

//IMGUI_API bool          ButtonEx(const char* label, const ImVec2& size_arg = ImVec2(0,0), ImGuiButtonFlags flags = 0);

    /* Upper-right button to close a window.    */
    fun closeButton(id: Int, pos: Vec2, radius: Float): Boolean {

        val window = getCurrentWindow()

        val bb = Rect(pos - radius, pos + radius)

        val (pressed, hovered, held) = buttonBehavior(bb, id)

        // Render
        val col = ImGui.getColorU32(if (held && hovered) Col.CloseButtonActive else if (hovered) Col.CloseButtonHovered else Col.CloseButton)
        val center = bb.center
        window.drawList.addCircleFilled(center, glm.max(2f, radius), col, 12)

        val crossExtent = (radius * 0.7071f) - 1f
        if (hovered) {
            window.drawList.addLine(center + crossExtent, center - crossExtent, ImGui.getColorU32(Col.Text))
            window.drawList.addLine(center + Vec2(crossExtent, -crossExtent), center + Vec2(-crossExtent, crossExtent),
                    ImGui.getColorU32(Col.Text))
        }

        return pressed
    }


    fun sliderBehavior(frameBb: Rect, id: Int, v: FloatArray, vMin: Float, vMax: Float, power: Float, decimalPrecision: Int,
                       flags: Int = 0): Boolean {

        val window = getCurrentWindow()

        // Draw frame
        renderFrame(frameBb.min, frameBb.max, getColorU32(Col.FrameBg), true, Style.frameRounding)

        val isNonLinear = (power < 1.0f - 0.00001f) || (power > 1.0f + 0.00001f)
        val isHorizontal = flags hasnt SliderFlags.Vertical

        val grabPadding = 2f
        val sliderSz = (if (isHorizontal) frameBb.width else frameBb.height) - grabPadding * 2f
        val grabSz =
                if (decimalPrecision > 0)
                    glm.min(Style.grabMinSize, sliderSz)
                else
                    glm.min(
                            glm.max(1f * (sliderSz / ((if (vMin < vMax) vMax - vMin else vMin - vMax) + 1f)), Style.grabMinSize),
                            sliderSz)  // Integer sliders, if possible have the grab size represent 1 unit
        val sliderUsableSz = sliderSz - grabSz
        val sliderUsablePosMin = (if (isHorizontal) frameBb.min.x else frameBb.min.y) + grabPadding + grabSz * 0.5f
        val sliderUsablePosMax = (if (isHorizontal) frameBb.max.x else frameBb.max.y) - grabPadding - grabSz * 0.5f

        // For logarithmic sliders that cross over sign boundary we want the exponential increase to be symmetric around 0.0f
        var linearZeroPos = 0f   // 0.0->1.0f
        if (vMin * vMax < 0f) {
            // Different sign
            val linearDistMinTo0 = glm.pow(glm.abs(0f - vMin), 1f / power)
            val linearDistMaxTo0 = glm.pow(glm.abs(vMax - 0f), 1f / power)
            linearZeroPos = linearDistMinTo0 / (linearDistMinTo0 + linearDistMaxTo0)
        } else  // Same sign
            linearZeroPos = if (vMin < 0f) 1f else 0f

        // Process clicking on the slider
        var valueChanged = false
        if (g.activeId == id)

            if (IO.mouseDown[0]) {

                val mouseAbsPos = if (isHorizontal) IO.mousePos.x else IO.mousePos.y
                var clickedT =
                        if (sliderUsableSz > 0f)
                            glm.clamp((mouseAbsPos - sliderUsablePosMin) / sliderUsableSz, 0f, 1f)
                        else 0f
                if (!isHorizontal)
                    clickedT = 1f - clickedT

                var newValue =
                        if (isNonLinear) {
                            // Account for logarithmic scale on both sides of the zero
                            if (clickedT < linearZeroPos) {
                                // Negative: rescale to the negative range before powering
                                var a = 1f - (clickedT / linearZeroPos)
                                a = glm.pow(a, power)
                                lerp(glm.min(vMax, 0f), vMin, a)
                            } else {
                                // Positive: rescale to the positive range before powering
                                var a =
                                        if (glm.abs(linearZeroPos - 1f) > 1e-6f)
                                            (clickedT - linearZeroPos) / (1f - linearZeroPos)
                                        else clickedT
                                a = glm.pow(a, power)
                                lerp(glm.max(vMin, 0.0f), vMax, a)
                            }
                        } else lerp(vMin, vMax, clickedT) // Linear slider
                // Round past decimal precision
                newValue = roundScalar(newValue, decimalPrecision)
                if (v[0] != newValue) {
                    v[0] = newValue
                    valueChanged = true
                }
            } else clearActiveId()
        // Calculate slider grab positioning
        var grabT = sliderBehaviorCalcRatioFromValue(v[0], vMin, vMax, power, linearZeroPos)
        // Draw
        if (!isHorizontal)
            grabT = 1f - grabT
        val grabPos = lerp(sliderUsablePosMin, sliderUsablePosMax, grabT)
        val grabBb =
                if (isHorizontal)
                    Rect(Vec2(grabPos - grabSz * 0.5f, frameBb.min.y + grabPadding),
                            Vec2(grabPos + grabSz * 0.5f, frameBb.max.y - grabPadding))
                else
                    Rect(Vec2(frameBb.min.x + grabPadding, grabPos - grabSz * 0.5f),
                            Vec2(frameBb.max.x - grabPadding, grabPos + grabSz * 0.5f))
        val col = getColorU32(if (g.activeId == id) Col.SliderGrabActive else Col.SliderGrab)
        window.drawList.addRectFilled(grabBb.min, grabBb.max, col, Style.grabRounding)

        return valueChanged
    }

    fun sliderBehaviorCalcRatioFromValue(v: Float, vMin: Float, vMax: Float, power: Float, linearZeroPos: Float): Float {

        if (vMin == vMax) return 0f

        val isNonLinear = power < 1f - 0.00001f || power > 1f + 0.00001f
        val vClamped = if (vMin < vMax) glm.clamp(v, vMin, vMax) else glm.clamp(v, vMax, vMin)
        if (isNonLinear)
            if (vClamped < 0f) {
                val f = 1f - (vClamped - vMin) / (glm.min(0f, vMax) - vMin)
                return (1f - glm.pow(f, 1f / power)) * linearZeroPos
            } else {
                val f = (vClamped - glm.max(0f, vMin)) / (vMax - glm.max(0f, vMin))
                return linearZeroPos + glm.pow(f, 1f / power) * (1f - linearZeroPos)
            }
        // Linear slider
        return (vClamped - vMin) / (vMax - vMin)
    }

//IMGUI_API bool          SliderFloatN(const char* label, float* v, int components, float v_min, float v_max, const char* display_format, float power);
//IMGUI_API bool          SliderIntN(const char* label, int* v, int components, int v_min, int v_max, const char* display_format);
//
//IMGUI_API bool          DragBehavior(const ImRect& frame_bb, ImGuiID id, float* v, float v_speed, float v_min, float v_max, int decimal_precision, float power);
//IMGUI_API bool          DragFloatN(const char* label, float* v, int components, float v_speed, float v_min, float v_max, const char* display_format, float power);
//IMGUI_API bool          DragIntN(const char* label, int* v, int components, float v_speed, int v_min, int v_max, const char* display_format);
//
//IMGUI_API bool          InputTextEx(const char* label, char* buf, int buf_size, const ImVec2& size_arg, ImGuiInputTextFlags flags, ImGuiTextEditCallback callback = NULL, void* user_data = NULL);
//IMGUI_API bool          InputFloatN(const char* label, float* v, int components, int decimal_precision, ImGuiInputTextFlags extra_flags);
//IMGUI_API bool          InputIntN(const char* label, int* v, int components, ImGuiInputTextFlags extra_flags);
//IMGUI_API bool          InputScalarEx(const char* label, ImGuiDataType data_type, void* data_ptr, void* step_ptr, void* step_fast_ptr, const char* scalar_format, ImGuiInputTextFlags extra_flags);

    /** Create text input in place of a slider (when CTRL+Clicking on slider)   */
    fun inputScalarAsWidgetReplacement(aabb: Rect, label: String, dataType: DataType, data: Any, id: Int, decimalPrecision: Int): Boolean {
        TODO()
    }
//
//IMGUI_API bool          TreeNodeBehavior(ImGuiID id, ImGuiTreeNodeFlags flags, const char* label, const char* label_end = NULL);
//IMGUI_API bool          TreeNodeBehaviorIsOpen(ImGuiID id, ImGuiTreeNodeFlags flags = 0);                     // Consume previous SetNextTreeNodeOpened() data, if any. May return true when logging
//IMGUI_API void          TreePushRawID(ImGuiID id);
//
//IMGUI_API void          PlotEx(ImGuiPlotType plot_type, const char* label, float (*values_getter)(void* data, int idx), void* data, int values_count, int values_offset, const char* overlay_text, float scale_min, float scale_max, ImVec2 graph_size);
//

    /** Parse display precision back from the display format string */
    fun parseFormatPrecision(fmt: String, defaultPrecision: Int): Int {

        var precision = defaultPrecision
        if (fmt.contains('.')) {
            val s = fmt.substringAfter('.')
            if (s.isNotEmpty()) {
                precision = s[0].i
                if (precision < 0 || precision > 10)
                    precision = defaultPrecision
            }
        }
        return precision
    }

    fun roundScalar(value: Float, decimalPrecision: Int): Float {

        /*  Round past decimal precision
            So when our value is 1.99999 with a precision of 0.001 we'll end up rounding to 2.0
            FIXME: Investigate better rounding methods  */
        val minSteps = floatArrayOf(1f, 0.1f, 0.01f, 0.001f, 0.0001f, 0.00001f, 0.000001f, 0.0000001f, 0.00000001f, 0.000000001f)
        val minStep = if (decimalPrecision in 0..9) minSteps[decimalPrecision] else glm.pow(10f, -decimalPrecision.f)
        val negative = value < 0f
        var value = glm.abs(value)
        val remainder = value % minStep
        if (remainder <= minStep * 0.5f)
            value -= remainder
        else
            value += minStep - remainder
        return if (negative) -value else value
    }

}