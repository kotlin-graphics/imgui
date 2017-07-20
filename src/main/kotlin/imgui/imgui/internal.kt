package imgui.imgui

import gli.hasnt
import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.beginGroup
import imgui.ImGui.calcItemWidth
import imgui.ImGui.contentRegionMax
import imgui.ImGui.endGroup
import imgui.ImGui.getColorU32
import imgui.ImGui.textLineHeight
import imgui.ImGui.pushFont
import imgui.internal.*
import imgui.stb.stb
import imgui.Context as g
import kotlin.apply

// We should always have a CurrentWindow in the stack (there is an implicit "Debug" window)
// If this ever crash because g.CurrentWindow is NULL it means that either
// - ImGui::NewFrame() has never been called, which is illegal.
// - You are calling ImGui functions after ImGui::Render() and before the next ImGui::NewFrame(), which is also illegal.

interface imgui_internal {

    val currentWindowRead get() = g.currentWindow

    val currentWindow get() = g.currentWindow!!.apply { accessed = true }
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
                it addTo_ g.windowsSortBuffer
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

        val window = currentWindow
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
    fun itemAdd(bb: Rect, id: Int = 0): Boolean {

        val window = currentWindow
        with(window.dc) {
            lastItemId = id
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
                    if (window.isContentHoverable)
                        window.dc.lastItemHoveredAndUsable = true
        }

        return true
    }

    fun isClippedEx(bb: Rect, id: Int?, clipEvenWhenLogged: Boolean): Boolean {

        val window = currentWindowRead!!

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
            val window = currentWindowRead!!
            if (g.hoveredWindow == window || (flattenChilds && g.hoveredRootWindow == window.rootWindow))
                if ((g.activeId == 0 || g.activeId == id || g.activeIdAllowOverlap) && isMouseHoveringRect(bb))
                    if (g.hoveredRootWindow!!.isContentHoverable)
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
                && Key.Tab.isPressed())
        // Modulo on index will be applied at the end of frame once we've got the total counter of items.
            window.focusIdxTabRequestNext = window.focusIdxTabCounter + (if (IO.keyShift) (if (allowKeyboardFocus) -1 else 0) else +1)

        if (window.focusIdxAllCounter == window.focusIdxAllRequestCurrent) return true

        return allowKeyboardFocus && window.focusIdxTabCounter == window.focusIdxTabRequestCurrent
    }

    fun focusableItemUnregister(window: Window) {
        window.focusIdxAllCounter--
        window.focusIdxTabCounter--
    }

    fun calcItemSize(size: Vec2, defaultX: Float, defaultY: Float): Vec2 {

        val contentMax = Vec2()
        if (size.x < 0f || size.y < 0f)
            contentMax put g.currentWindow!!.pos + contentRegionMax
        if (size.x <= 0f)
            size.x = if (size.x == 0f) defaultX else glm.max(contentMax.x - g.currentWindow!!.dc.cursorPos.x, 4f) + size.x
        if (size.y <= 0f)
            size.y = if (size.y == 0f) defaultY else glm.max(contentMax.y - g.currentWindow!!.dc.cursorPos.y, 4f) + size.y
        return size
    }

    fun calcWrapWidthForPos(pos: Vec2, wrapPosX: Float): Float {

        if (wrapPosX < 0f) return 0f

        val window = currentWindowRead!!
        var wrapPosX = wrapPosX
        if (wrapPosX == 0f)
            wrapPosX = contentRegionMax.x + window.pos.x
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

        val window = currentWindow

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

        val window = currentWindow

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

        val window = currentWindow

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

        val window = currentWindow

        window.drawList.addRectFilled(pMin, pMax, fillCol, rounding)
        if (border && window.flags has WindowFlags.ShowBorders) {
            window.drawList.addRect(pMin + 1, pMax + 1, ImGui.getColorU32(Col.BorderShadow), rounding)
            window.drawList.addRect(pMin, pMax, ImGui.getColorU32(Col.Border), rounding)
        }
    }

    /** Render a triangle to denote expanded/collapsed state    */
    fun renderCollapseTriangle(pMin: Vec2, isOpen: Boolean, scale: Float = 1.0f) {

        val window = currentWindow

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

        val window = currentWindow
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


    fun buttonEx(label: String, sizeArg: Vec2 = Vec2(), flags: Int = 0): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(label)
        val labelSize = calcTextSize(label, true)

        val pos = Vec2(window.dc.cursorPos)
        /*  Try to vertically align buttons that are smaller/have no padding so that text baseline matches (bit hacky,
            since it shouldn't be a flag)         */
        if (flags has ButtonFlags.AlignTextBaseLine && Style.framePadding.y < window.dc.currentLineTextBaseOffset)
            pos.y += window.dc.currentLineTextBaseOffset - Style.framePadding.y
        val size = calcItemSize(sizeArg, labelSize.x + Style.framePadding.x * 2f, labelSize.y + Style.framePadding.y * 2f)

        val bb = Rect(pos, pos + size)
        itemSize(bb, Style.framePadding.y)
        if (!itemAdd(bb, id)) return false

        var flags = flags
        if (window.dc.buttonRepeat) flags = flags or ButtonFlags.Repeat
        val (pressed, hovered, held) = buttonBehavior(bb, id, flags)

        // Render
        val col = getColorU32(if (hovered && held) Col.ButtonActive else if (hovered) Col.ButtonHovered else Col.Button)
        renderFrame(bb.min, bb.max, col, true, Style.frameRounding)
        renderTextClipped(bb.min + Style.framePadding, bb.max - Style.framePadding, label, 0, labelSize,
                Style.buttonTextAlign, bb)

        // Automatically close popups
        //if (pressed && !(flags & ImGuiButtonFlags_DontClosePopups) && (window->Flags & ImGuiWindowFlags_Popup))
        //    CloseCurrentPopup();

        return pressed
    }


    /* Upper-right button to close a window.    */
    fun closeButton(id: Int, pos: Vec2, radius: Float): Boolean {

        val window = currentWindow

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

        val window = currentWindow

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

    fun dragBehavior(frameBb: Rect, id: Int, v: FloatArray, vSpeed: Float, vMin: Float, vMax: Float, decimalPrecision: Int,
                     power: Float): Boolean {

        // Draw frame
        val frameCol = getColorU32(
                if (g.activeId == id) Col.FrameBgActive
                else if (g.hoveredId == id) Col.FrameBgHovered
                else Col.FrameBg)
        renderFrame(frameBb.min, frameBb.max, frameCol, true, Style.frameRounding)

        var valueChanged = false

        // Process clicking on the drag
        if (g.activeId == id)

            if (IO.mouseDown[0]) {

                if (g.activeIdIsJustActivated) {
                    // Lock current value on click
                    g.dragCurrentValue = v[0]
                    g.dragLastMouseDelta put 0f
                }

                var vCur = g.dragCurrentValue
                val mouseDragDelta = getMouseDragDelta(0, 1f)
                if (glm.abs(mouseDragDelta.x - g.dragLastMouseDelta.x) > 0f) {
                    var speed = vSpeed
                    if (speed == 0f && (vMax - vMin) != 0f && (vMax - vMin) < Float.MAX_VALUE)
                        speed = (vMax - vMin) * g.dragSpeedDefaultRatio
                    if (IO.keyShift && g.dragSpeedScaleFast >= 0f)
                        speed *= g.dragSpeedScaleFast
                    if (IO.keyAlt && g.dragSpeedScaleSlow >= 0f)
                        speed *= g.dragSpeedScaleSlow

                    val delta = (mouseDragDelta.x - g.dragLastMouseDelta.x) * speed
                    if (glm.abs(power - 1f) > 0.001f) {
                        // Logarithmic curve on both side of 0.0
                        val v0_abs = if (vCur >= 0f) vCur else -vCur
                        val v0_sign = if (vCur >= 0f) 1f else -1f
                        val v1 = glm.pow(v0_abs, 1f / power) + delta * v0_sign
                        val v1_abs = if (v1 >= 0f) v1 else -v1
                        val v1_sign = if (v1 >= 0f) 1f else -1f              // Crossed sign line
                        vCur = glm.pow(v1_abs, power) * v0_sign * v1_sign   // Reapply sign
                    } else
                        vCur += delta

                    g.dragLastMouseDelta.x = mouseDragDelta.x

                    // Clamp
                    if (vMin < vMax)
                        vCur = glm.clamp(vCur, vMin, vMax)
                    g.dragCurrentValue = vCur
                }

                // Round to user desired precision, then apply
                vCur = roundScalar(vCur, decimalPrecision)
                if (v[0] != vCur) {
                    v[0] = vCur
                    valueChanged = true
                }
            } else
                clearActiveId()

        return valueChanged
    }
//IMGUI_API bool          DragFloatN(const char* label, float* v, int components, float v_speed, float v_min, float v_max, const char* display_format, float power);
//IMGUI_API bool          DragIntN(const char* label, int* v, int components, float v_speed, int v_min, int v_max, const char* display_format);


    fun inputTextEx(label: String, buf: CharArray, bufSize: Int, sizeArg: Vec2, flags: Int
            /*, ImGuiTextEditCallback callback = NULL, void* user_data = NULL*/): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        // Can't use both together (they both use up/down keys)
        assert((flags has InputTextFlags.CallbackHistory) xor (flags has InputTextFlags.Multiline))
        // Can't use both together (they both use tab key)
        assert((flags has InputTextFlags.CallbackCompletion) xor (flags has InputTextFlags.AllowTabInput))

        val isMultiline = flags has InputTextFlags.Multiline
        val isEditable = flags has InputTextFlags.ReadOnly
        val isPassword = flags has InputTextFlags.Password

        if (isMultiline) // Open group before calling GetID() because groups tracks id created during their spawn
            beginGroup()
        val id = window.getId(label)
        val labelSize = calcTextSize(label, 0, true)
        val size = calcItemSize(sizeArg, calcItemWidth(),
                // Arbitrary default of 8 lines high for multi-line
                (if (isMultiline) textLineHeight * 8f else labelSize.y) + Style.framePadding.y * 2f)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        val totalBb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) Style.itemInnerSpacing.x + labelSize.x else 0f, 0f))

        var drawWindow = window
        if (isMultiline) {
            if (!beginChildFrame(id, frameBb.size)) {
                endChildFrame()
                endGroup()
                return false
            }
            drawWindow = currentWindow
            size.x -= drawWindow.scrollbarSizes.x
        } else {
            itemSize(totalBb, Style.framePadding.y)
            if (!itemAdd(totalBb, id)) return false
        }

        // Password pushes a temporary font with only a fallback glyph
        if (isPassword)
            with(g.inputTextPasswordFont) {
                val glyph = g.font.findGlyph('*')!!
                fontSize = g.font.fontSize
                scale = g.font.scale
                displayOffset = g.font.displayOffset
                ascent = g.font.ascent
                descent = g.font.descent
                containerAtlas = g.font.containerAtlas
                fallbackGlyph = glyph
                fallbackXAdvance = glyph.xAdvance
                assert(glyphs.isEmpty() && indexXAdvance.isEmpty() && indexLookup.isEmpty())
                pushFont(this)
            }

        // NB: we are only allowed to access 'editState' if we are the active widget.
        val editState = g.inputTextState

        // Using completion callback disable keyboard tabbing
        val tabStop = flags hasnt (InputTextFlags.CallbackCompletion or InputTextFlags.AllowTabInput)
        val focusRequested = focusableItemRegister(window, g.activeId == id, tabStop)
        val focusRequestedByCode = focusRequested && window.focusIdxAllCounter == window.focusIdxAllRequestCurrent
        val focusRequestedByTab = focusRequested && !focusRequestedByCode

        val hovered = isHovered(frameBb, id)
        if (hovered) {
            setHoveredId(id)
            g.mouseCursor = MouseCursor.TextInput
        }
        val userClicked = hovered && IO.mouseClicked[0]
        val userScrolled = isMultiline && g.activeId == 0 && editState.id == id &&
                g.activeIdPreviousFrame == drawWindow.getIdNoKeepAlive("#SCROLLY")

        var selectAll = g.activeId != id && flags has InputTextFlags.AutoSelectAll
        if (focusRequested || userClicked || userScrolled) {
            if (g.activeId != id) {
                /*  Start edition
                    Take a copy of the initial buffer value (both in original UTF-8 format and converted to wchar)
                    From the moment we focused we are ignoring the content of 'buf' (unless we are in read-only mode)   */
                val prevLenW = editState.curLenW
                // wchar count <= UTF-8 count. we use +1 to make sure that .Data isn't NULL so it doesn't crash.
                editState.text.add('\u0000')
                // UTF-8. we use +1 to make sure that .Data isn't NULL so it doesn't crash.
                editState.initialText.add('\u0000')
                for (i in 0 until bufSize) editState.initialText.add(buf[i])
                var bufEnd = 0
                editState.curLenW = buf.size //TODO check ImTextStrFromUtf8(editState.Text.Data, editState.Text.Size, buf, NULL, & buf_end)
                editState.curLenA = buf.size //TODO check (int)(bufEnd - buf) // We can't get the result from ImFormatString() above because it is not UTF-8 aware. Here we'll cut off malformed UTF-8.
                editState.cursorAnimReset()

                /*  Preserve cursor position and undo/redo stack if we come back to same widget
                    FIXME: We should probably compare the whole buffer to be on the safety side. Comparing buf (utf8)
                    and editState.Text (wchar). */
                if (editState.id == id && prevLenW == editState.curLenW)
                /*  Recycle existing cursor/selection/undo stack but clamp position
                    Note a single mouse click will override the cursor/position immediately by calling
                    stb_textedit_click handler.                     */
                    editState.cursorClamp()
                else {
                    editState.id = id
                    editState.scrollX = 0f
                    editState.stbState.clear(!isMultiline)
                    if (!isMultiline && focusRequestedByCode)
                        selectAll = true
                }
                if (flags has InputTextFlags.AlwaysInsertMode)
                    editState.stbState.insertMode = true
                if (!isMultiline && (focusRequestedByTab || (userClicked && IO.keyCtrl)))
                    selectAll = true
            }
            setActiveId(id, window)
            focusWindow(window)
        } else if (IO.mouseClicked[0])
        // Release focus when we click outside
            if (g.activeId == id)
                clearActiveId()

        var valueChanged = false
        var enterPressed = false

        if (g.activeId == id) {

            if (!isEditable && !g.activeIdIsJustActivated) {

                // When read-only we always use the live data passed to the function
                editState.text.add('\u0000')
                TODO()
//                const char* buf_end = NULL
//                        editState.CurLenW = ImTextStrFromUtf8(editState.Text.Data, editState.Text.Size, buf, NULL, &buf_end)
//                editState.CurLenA = (int)(buf_end - buf)
//                editState.CursorClamp()
            }

            editState.bufSizeA = bufSize

            /*  Although we are active we don't prevent mouse from hovering other elements unless we are interacting
                right now with the widget.
                Down the line we should have a cleaner library-wide concept of Selected vs Active.  */
            g.activeIdAllowOverlap = !IO.mouseDown[0]

            // Edit in progress
            val mouseX = IO.mousePos.x - frameBb.min.x - Style.framePadding.x + editState.scrollX
            val mouseY =
                    if (isMultiline)
                        IO.mousePos.y - drawWindow.dc.cursorPos.y - Style.framePadding.y
                    else g.fontSize * 0.5f

            // OS X style: Double click selects by word instead of selecting whole text
            val osxDoubleClickSelectsWords = IO.osxBehaviors
            if (selectAll || (hovered && !osxDoubleClickSelectsWords && IO.mouseDoubleClicked[0])) {
                editState.selectAll()
                editState.selectedAllMouseLock = true
            } else if (hovered && osxDoubleClickSelectsWords && IO.mouseDoubleClicked[0]) {
                // Select a word only, OS X style (by simulating keystrokes)
                editState.onKeyPressed(stb.TEXTEDIT_K_WORDLEFT)
                editState.onKeyPressed(stb.TEXTEDIT_K_WORDRIGHT or stb.TEXTEDIT_K_SHIFT)
            } else if (IO.mouseClicked[0] && !editState.selectedAllMouseLock) {
                TODO()
//                stb.texteditClick(editState, & editState . StbState, mouse_x, mouse_y)
//                editState.CursorAnimReset()
            } else if (IO.mouseDown[0] && !editState.selectedAllMouseLock && (IO.mouseDelta.x != 0f || IO.mouseDelta.y != 0f)) {
                TODO()
//                stb_textedit_drag(& editState, &editState.StbState, mouse_x, mouse_y)
//                editState.CursorAnimReset()
//                editState.CursorFollow = true
            }
            if (editState.selectedAllMouseLock && !IO.mouseDown[0])
                editState.selectedAllMouseLock = false

            if (IO.inputCharacters[0].i != 0) {
                /*  Process text input (before we check for Return because using some IME will effectively send a
                    Return?)
                    We ignore CTRL inputs, but need to allow CTRL+ALT as some keyboards (e.g. German) use AltGR - which
                    is Alt+Ctrl - to input certain characters.  */
                if (!(IO.keyCtrl && !IO.keyAlt) && isEditable)
                    for (n in IO.inputCharacters.indices) {
                        val c = IO.inputCharacters[n].i
                        if (c == 0) break
                        // Insert character if they pass filtering
                        TODO()
//                        if (!inputTextFilterCharacter(& c, flags, callback, user_data))
//                        continue
//                        editState.OnKeyPressed((int) c)
                    }

                // Consume characters
                IO.inputCharacters.fill('\u0000')
            }

            // Handle various key-presses
            var cancelEdit = false
            val kMask = if (IO.keyShift) stb.TEXTEDIT_K_SHIFT else 0
            // OS X style: Shortcuts using Cmd/Super instead of Ctrl
            val isShortcutKeyOnly = (if (IO.osxBehaviors) IO.keySuper && !IO.keyCtrl else IO.keyCtrl && !IO.keySuper) && !IO.keyAlt && !IO.keyShift
            // OS X style: Text editing cursor movement using Alt instead of Ctrl
            val isWordmoveKeyDown = if (IO.osxBehaviors) IO.keyAlt else IO.keyCtrl
            // OS X style: Line/Text Start and End using Cmd+Arrows instead of Home/End
            val isStartendKeyDown = IO.osxBehaviors && IO.keySuper && !IO.keyCtrl && !IO.keyAlt

//            when {
//                Key.LeftArrow.isPressed() -> editState.onKeyPressed(
//                        when {
//                            isStartendKeyDown -> stb.TEXTEDIT_K_LINESTART
//                            isWordmoveKeyDown -> stb.TEXTEDIT_K_WORDLEFT
//                            else -> stb.TEXTEDIT_K_LEFT
//                        } or kMask)
//                Key.RightArrow.isPressed() -> editState.onKeyPressed(
//                        when {
//                            isStartendKeyDown -> stb.TEXTEDIT_K_LINEEND
//                            isWordmoveKeyDown -> stb.TEXTEDIT_K_WORDRIGHT
//                            else -> stb.TEXTEDIT_K_RIGHT
//                        } or kMask)
//                Key.UpArrow.isPressed() && isMultiline ->
//                    if (IO.keyCtrl)
//                        drawWindow.setScrollY(glm.max(drawWindow.scroll.y - g.fontSize, 0f))
//                    else
//                        editState.onKeyPressed((if (isStartendKeyDown) stb.TEXTEDIT_K_TEXTSTART else stb.TEXTEDIT_K_UP) or kMask)
//                Key.DownArrow.isPressed() && isMultiline ->
//                    if (IO.keyCtrl)
//                        drawWindow.setScrollY(), glm.min(drawWindow.scroll.y + g.fontSize, getScrollMaxY())); else editState.OnKeyPressed((isStartendKeyDown ? STB_TEXTEDIT_K_TEXTEND : STB_TEXTEDIT_K_DOWN
//                    )
//                    | kMask
//                    )
//            }
//            else if (IsKeyPressedMap(ImGuiKey_Home)) {
//                editState.OnKeyPressed(io.KeyCtrl ? STB_TEXTEDIT_K_TEXTSTART | kMask : STB_TEXTEDIT_K_LINESTART | kMask)
//            } else if (IsKeyPressedMap(ImGuiKey_End)) {
//                editState.OnKeyPressed(io.KeyCtrl ? STB_TEXTEDIT_K_TEXTEND | kMask : STB_TEXTEDIT_K_LINEEND | kMask)
//            } else if (IsKeyPressedMap(ImGuiKey_Delete) && isEditable) {
//                editState.OnKeyPressed(STB_TEXTEDIT_K_DELETE | kMask)
//            } else if (IsKeyPressedMap(ImGuiKey_Backspace) && isEditable) {
//                if (!editState.HasSelection()) {
//                    if (isWordmoveKeyDown) editState.OnKeyPressed(STB_TEXTEDIT_K_WORDLEFT| STB_TEXTEDIT_K_SHIFT)
//                    else if (io.OSXBehaviors && io.KeySuper && !io.KeyAlt && !io.KeyCtrl) editState.OnKeyPressed(STB_TEXTEDIT_K_LINESTART| STB_TEXTEDIT_K_SHIFT)
//                }
//                editState.OnKeyPressed(STB_TEXTEDIT_K_BACKSPACE | kMask)
//            } else if (IsKeyPressedMap(ImGuiKey_Enter)) {
//                bool ctrl_enter_for_new_line =(flags & ImGuiInputTextFlags_CtrlEnterForNewLine) != 0
//                if (!isMultiline || (ctrl_enter_for_new_line && !io.KeyCtrl) || (!ctrl_enter_for_new_line && io.KeyCtrl)) {
//                    ClearActiveID()
//                    enterPressed = true
//                } else if (isEditable) {
//                    unsigned int c = '\n' // Insert new line
//                    if (InputTextFilterCharacter(& c, flags, callback, user_data))
//                    editState.OnKeyPressed((int) c)
//                }
//            } else if ((flags & ImGuiInputTextFlags_AllowTabInput) && IsKeyPressedMap(ImGuiKey_Tab) && !io.KeyCtrl && !io.KeyShift && !io.KeyAlt && is_editable) {
//                unsigned int c = '\t' // Insert TAB
//                if (InputTextFilterCharacter(& c, flags, callback, user_data))
//                editState.OnKeyPressed((int) c)
//            }
//            else if (IsKeyPressedMap(ImGuiKey_Escape)) {
//                ClearActiveID(); cancelEdit = true; } else if (isShortcutKeyOnly && IsKeyPressedMap(ImGuiKey_Z) && isEditable) {
//                editState.OnKeyPressed(STB_TEXTEDIT_K_UNDO); editState.ClearSelection(); } else if (isShortcutKeyOnly && IsKeyPressedMap(ImGuiKey_Y) && isEditable) {
//                editState.OnKeyPressed(STB_TEXTEDIT_K_REDO); editState.ClearSelection(); } else if (isShortcutKeyOnly && IsKeyPressedMap(ImGuiKey_A)) {
//                editState.SelectAll(); editState.CursorFollow = true; } else if (isShortcutKeyOnly && !isPassword && ((IsKeyPressedMap(ImGuiKey_X) && isEditable) || IsKeyPressedMap(ImGuiKey_C)) && (!isMultiline || editState.HasSelection())) {
//                // Cut, Copy
//                const bool cut = IsKeyPressedMap(ImGuiKey_X)
//                if (cut && !editState.HasSelection())
//                    editState.SelectAll()
//
//                if (io.SetClipboardTextFn) {
//                    const int ib = editState.HasSelection() ? ImMin(editState.StbState.select_start, editState.StbState.select_end) : 0
//                    const int ie = editState.HasSelection() ? ImMax(editState.StbState.select_start, editState.StbState.select_end) : editState.CurLenW
//                    editState.TempTextBuffer.resize((ie - ib) * 4 + 1)
//                    ImTextStrToUtf8(editState.TempTextBuffer.Data, editState.TempTextBuffer.Size, editState.Text.Data + ib, editState.Text.Data + ie)
//                    SetClipboardText(editState.TempTextBuffer.Data)
//                }
//
//                if (cut) {
//                    editState.CursorFollow = true
//                    stb_textedit_cut(& editState, &editState.StbState)
//                }
//            } else if (isShortcutKeyOnly && IsKeyPressedMap(ImGuiKey_V) && isEditable) {
//                // Paste
//                if (const char * clipboard = GetClipboardText ()) {
//                    // Filter pasted buffer
//                    const int clipboard_len = (int) strlen (clipboard)
//                    ImWchar * clipboard_filtered = (ImWchar *) ImGui ::MemAlloc((clipboard_len + 1) * sizeof(ImWchar))
//                    int clipboard_filtered_len = 0
//                    for (const char* s = clipboard; *s; )
//                    {
//                        unsigned int c
//                        s += ImTextCharFromUtf8(& c, s, NULL)
//                        if (c == 0)
//                            break
//                        if (c >= 0x10000 || !InputTextFilterCharacter(& c, flags, callback, user_data))
//                        continue
//                        clipboard_filtered[clipboard_filtered_len++] = (ImWchar) c
//                    }
//                    clipboard_filtered[clipboard_filtered_len] = 0
//                    if (clipboard_filtered_len > 0) // If everything was filtered, ignore the pasting operation
//                    {
//                        stb_textedit_paste(& editState, &editState.StbState, clipboard_filtered, clipboard_filtered_len)
//                        editState.CursorFollow = true
//                    }
//                    ImGui::MemFree(clipboard_filtered)
//                }
//            }
//        }
//
//            if (cancelEdit) {
//                // Restore initial value
//                if (isEditable) {
//                    ImStrncpy(buf, editState.InitialText.Data, buf_size)
//                    valueChanged = true
//                }
//            } else {
//                // Apply new value immediately - copy modified buffer back
//                // Note that as soon as the input box is active, the in-widget value gets priority over any underlying modification of the input buffer
//                // FIXME: We actually always render 'buf' when calling DrawList->AddText, making the comment above incorrect.
//                // FIXME-OPT: CPU waste to do this every time the widget is active, should mark dirty state from the stb_textedit callbacks.
//                if (isEditable) {
//                    editState.TempTextBuffer.resize(editState.Text.Size * 4)
//                    ImTextStrToUtf8(editState.TempTextBuffer.Data, editState.TempTextBuffer.Size, editState.Text.Data, NULL)
//                }
//
//                // User callback
//                if ((flags & (ImGuiInputTextFlags_CallbackCompletion | ImGuiInputTextFlags_CallbackHistory | ImGuiInputTextFlags_CallbackAlways)) != 0)
//                {
//                    IM_ASSERT(callback != NULL)
//
//                    // The reason we specify the usage semantic (Completion/History) is that Completion needs to disable keyboard TABBING at the moment.
//                    ImGuiInputTextFlags event_flag = 0
//                    ImGuiKey event_key = ImGuiKey_COUNT
//                            if ((flags & ImGuiInputTextFlags_CallbackCompletion) != 0 && IsKeyPressedMap(ImGuiKey_Tab))
//                    {
//                        event_flag = ImGuiInputTextFlags_CallbackCompletion
//                        event_key = ImGuiKey_Tab
//                    }
//                    else if ((flags & ImGuiInputTextFlags_CallbackHistory) != 0 && IsKeyPressedMap(ImGuiKey_UpArrow))
//                    {
//                        event_flag = ImGuiInputTextFlags_CallbackHistory
//                        event_key = ImGuiKey_UpArrow
//                    }
//                    else if ((flags & ImGuiInputTextFlags_CallbackHistory) != 0 && IsKeyPressedMap(ImGuiKey_DownArrow))
//                    {
//                        event_flag = ImGuiInputTextFlags_CallbackHistory
//                        event_key = ImGuiKey_DownArrow
//                    }
//                    else if (flags & ImGuiInputTextFlags_CallbackAlways)
//                    event_flag = ImGuiInputTextFlags_CallbackAlways
//
//                    if (event_flag) {
//                        ImGuiTextEditCallbackData callback_data
//                                memset(& callback_data, 0, sizeof(ImGuiTextEditCallbackData))
//                        callback_data.EventFlag = event_flag
//                        callback_data.Flags = flags
//                        callback_data.UserData = user_data
//                        callback_data.ReadOnly = !isEditable
//
//                        callback_data.EventKey = event_key
//                        callback_data.Buf = editState.TempTextBuffer.Data
//                        callback_data.BufTextLen = editState.CurLenA
//                        callback_data.BufSize = editState.BufSizeA
//                        callback_data.BufDirty = false
//
//                        // We have to convert from wchar-positions to UTF-8-positions, which can be pretty slow (an incentive to ditch the ImWchar buffer, see https://github.com/nothings/stb/issues/188)
//                        ImWchar * text = editState.Text.Data
//                        const int utf8_cursor_pos = callback_data.CursorPos = ImTextCountUtf8BytesFromStr(text, text + editState.StbState.cursor)
//                        const int utf8_selection_start = callback_data.SelectionStart = ImTextCountUtf8BytesFromStr(text, text + editState.StbState.select_start)
//                        const int utf8_selection_end = callback_data.SelectionEnd = ImTextCountUtf8BytesFromStr(text, text + editState.StbState.select_end)
//
//                        // Call user code
//                        callback(& callback_data)
//
//                        // Read back what user may have modified
//                        IM_ASSERT(callback_data.Buf == editState.TempTextBuffer.Data)  // Invalid to modify those fields
//                        IM_ASSERT(callback_data.BufSize == editState.BufSizeA)
//                        IM_ASSERT(callback_data.Flags == flags)
//                        if (callback_data.CursorPos != utf8_cursor_pos) editState.StbState.cursor = ImTextCountCharsFromUtf8(callback_data.Buf, callback_data.Buf + callback_data.CursorPos)
//                        if (callback_data.SelectionStart != utf8_selection_start) editState.StbState.select_start = ImTextCountCharsFromUtf8(callback_data.Buf, callback_data.Buf + callback_data.SelectionStart)
//                        if (callback_data.SelectionEnd != utf8_selection_end) editState.StbState.select_end = ImTextCountCharsFromUtf8(callback_data.Buf, callback_data.Buf + callback_data.SelectionEnd)
//                        if (callback_data.BufDirty) {
//                            IM_ASSERT(callback_data.BufTextLen == (int) strlen (callback_data.Buf)) // You need to maintain BufTextLen if you change the text!
//                            editState.CurLenW = ImTextStrFromUtf8(editState.Text.Data, editState.Text.Size, callback_data.Buf, NULL)
//                            editState.CurLenA = callback_data.BufTextLen  // Assume correct length and valid UTF-8 from user, saves us an extra strlen()
//                            editState.CursorAnimReset()
//                        }
//                    }
//                }
//
//                // Copy back to user buffer
//                if (isEditable && strcmp(editState.TempTextBuffer.Data, buf) != 0) {
//                    ImStrncpy(buf, editState.TempTextBuffer.Data, buf_size)
//                    valueChanged = true
//                }
//            }
    }
//
//        // Render
//        // Select which buffer we are going to display. When ImGuiInputTextFlags_NoLiveEdit is set 'buf' might still be the old value. We set buf to NULL to prevent accidental usage from now on.
//        const char* buf_display = (g.ActiveId == id && isEditable) ? editState.TempTextBuffer.Data : buf; buf = NULL
//
//        if (!isMultiline)
//            RenderFrame(frameBb.Min, frameBb.Max, GetColorU32(ImGuiCol_FrameBg), true, style.FrameRounding)
//
//        const ImVec4 clip_rect(frameBb.Min.x, frameBb.Min.y, frameBb.Min.x + size.x, frameBb.Min.y + size.y) // Not using frameBb.Max because we have adjusted size
//        ImVec2 render_pos = is_multiline ? drawWindow->DC.CursorPos : frameBb.Min + style.FramePadding
//        ImVec2 text_size(0.f, 0.f)
//        const bool is_currently_scrolling = (editState.Id == id && isMultiline && g.ActiveId == drawWindow->GetIDNoKeepAlive("#SCROLLY"))
//        if (g.ActiveId == id || is_currently_scrolling)
//        {
//            editState.CursorAnim += io.DeltaTime
//
//            // This is going to be messy. We need to:
//            // - Display the text (this alone can be more easily clipped)
//            // - Handle scrolling, highlight selection, display cursor (those all requires some form of 1d->2d cursor position calculation)
//            // - Measure text height (for scrollbar)
//            // We are attempting to do most of that in **one main pass** to minimize the computation cost (non-negligible for large amount of text) + 2nd pass for selection rendering (we could merge them by an extra refactoring effort)
//            // FIXME: This should occur on buf_display but we'd need to maintain cursor/select_start/select_end for UTF-8.
//            const ImWchar* text_begin = editState.Text.Data
//                    ImVec2 cursor_offset, select_start_offset;
//
//            {
//                // Count lines + find lines numbers straddling 'cursor' and 'select_start' position.
//                const ImWchar* searches_input_ptr[2]
//                searches_input_ptr[0] = text_begin + editState.StbState.cursor
//                searches_input_ptr[1] = NULL
//                int searches_remaining = 1
//                int searches_result_line_number[2] = { -1, -999 }
//                if (editState.StbState.select_start != editState.StbState.select_end)
//                {
//                    searches_input_ptr[1] = text_begin + ImMin(editState.StbState.select_start, editState.StbState.select_end)
//                    searches_result_line_number[1] = -1
//                    searches_remaining++
//                }
//
//                // Iterate all lines to find our line numbers
//                // In multi-line mode, we never exit the loop until all lines are counted, so add one extra to the searches_remaining counter.
//                searches_remaining += isMultiline ? 1 : 0
//                int line_count = 0
//                for (const ImWchar* s = text_begin; *s != 0; s++)
//                if (*s == '\n')
//                {
//                    line_count++
//                    if (searches_result_line_number[0] == -1 && s >= searches_input_ptr[0]) { searches_result_line_number[0] = line_count; if (--searches_remaining <= 0) break; }
//                    if (searches_result_line_number[1] == -1 && s >= searches_input_ptr[1]) { searches_result_line_number[1] = line_count; if (--searches_remaining <= 0) break; }
//                }
//                line_count++
//                if (searches_result_line_number[0] == -1) searches_result_line_number[0] = line_count
//                if (searches_result_line_number[1] == -1) searches_result_line_number[1] = line_count
//
//                // Calculate 2d position by finding the beginning of the line and measuring distance
//                cursor_offset.x = InputTextCalcTextSizeW(ImStrbolW(searches_input_ptr[0], text_begin), searches_input_ptr[0]).x
//                cursor_offset.y = searches_result_line_number[0] * g.FontSize
//                if (searches_result_line_number[1] >= 0)
//                {
//                    select_start_offset.x = InputTextCalcTextSizeW(ImStrbolW(searches_input_ptr[1], text_begin), searches_input_ptr[1]).x
//                    select_start_offset.y = searches_result_line_number[1] * g.FontSize
//                }
//
//                // Store text height (note that we haven't calculated text width at all, see GitHub issues #383, #1224)
//                if (isMultiline)
//                    text_size = ImVec2(size.x, line_count * g.FontSize)
//            }
//
//            // Scroll
//            if (editState.CursorFollow)
//            {
//                // Horizontal scroll in chunks of quarter width
//                if (!(flags & ImGuiInputTextFlags_NoHorizontalScroll))
//                {
//                    const float scroll_increment_x = size.x * 0.25f
//                    if (cursor_offset.x < editState.ScrollX)
//                        editState.ScrollX = (float)(int)ImMax(0.0f, cursor_offset.x - scroll_increment_x)
//                    else if (cursor_offset.x - size.x >= editState.ScrollX)
//                    editState.ScrollX = (float)(int)(cursor_offset.x - size.x + scroll_increment_x)
//                }
//                else
//                {
//                    editState.ScrollX = 0.0f
//                }
//
//                // Vertical scroll
//                if (isMultiline)
//                {
//                    float scroll_y = drawWindow->Scroll.y
//                    if (cursor_offset.y - g.FontSize < scroll_y)
//                        scroll_y = ImMax(0.0f, cursor_offset.y - g.FontSize)
//                    else if (cursor_offset.y - size.y >= scroll_y)
//                        scroll_y = cursor_offset.y - size.y
//                    drawWindow->DC.CursorPos.y += (drawWindow->Scroll.y - scroll_y)   // To avoid a frame of lag
//                    drawWindow->Scroll.y = scroll_y
//                    render_pos.y = drawWindow->DC.CursorPos.y
//                }
//            }
//            editState.CursorFollow = false
//            const ImVec2 render_scroll = ImVec2(editState.ScrollX, 0.0f)
//
//            // Draw selection
//            if (editState.StbState.select_start != editState.StbState.select_end)
//            {
//                const ImWchar* text_selected_begin = text_begin + ImMin(editState.StbState.select_start, editState.StbState.select_end)
//                const ImWchar* text_selected_end = text_begin + ImMax(editState.StbState.select_start, editState.StbState.select_end)
//
//                float bg_offy_up = is_multiline ? 0.0f : -1.0f    // FIXME: those offsets should be part of the style? they don't play so well with multi-line selection.
//                float bg_offy_dn = is_multiline ? 0.0f : 2.0f
//                ImU32 bg_color = GetColorU32(ImGuiCol_TextSelectedBg)
//                ImVec2 rect_pos = render_pos + select_start_offset - render_scroll
//                for (const ImWchar* p = text_selected_begin; p < text_selected_end; )
//                {
//                    if (rect_pos.y > clip_rect.w + g.FontSize)
//                        break
//                    if (rect_pos.y < clip_rect.y)
//                    {
//                        while (p < text_selected_end)
//                            if (*p++ == '\n')
//                        break
//                    }
//                    else
//                    {
//                        ImVec2 rect_size = InputTextCalcTextSizeW(p, text_selected_end, &p, NULL, true)
//                        if (rect_size.x <= 0.0f) rect_size.x = (float)(int)(g.Font->GetCharAdvance((unsigned short)' ') * 0.50f) // So we can see selected empty lines
//                        ImRect rect(rect_pos + ImVec2(0.0f, bg_offy_up - g.FontSize), rect_pos +ImVec2(rect_size.x, bg_offy_dn))
//                        rect.Clip(clip_rect)
//                        if (rect.Overlaps(clip_rect))
//                            drawWindow->DrawList->AddRectFilled(rect.Min, rect.Max, bg_color)
//                    }
//                    rect_pos.x = render_pos.x - render_scroll.x
//                    rect_pos.y += g.FontSize
//                }
//            }
//
//            drawWindow->DrawList->AddText(g.Font, g.FontSize, render_pos - render_scroll, GetColorU32(ImGuiCol_Text), buf_display, buf_display + editState.CurLenA, 0.0f, is_multiline ? NULL : &clip_rect)
//
//            // Draw blinking cursor
//            bool cursor_is_visible = (g.InputTextState.CursorAnim <= 0.0f) || fmodf(g.InputTextState.CursorAnim, 1.20f) <= 0.80f
//            ImVec2 cursor_screen_pos = render_pos + cursor_offset - render_scroll
//            ImRect cursor_screen_rect(cursor_screen_pos.x, cursor_screen_pos.y-g.FontSize+0.5f, cursor_screen_pos.x+1.0f, cursor_screen_pos.y-1.5f)
//            if (cursor_is_visible && cursor_screen_rect.Overlaps(clip_rect))
//                drawWindow->DrawList->AddLine(cursor_screen_rect.Min, cursor_screen_rect.GetBL(), GetColorU32(ImGuiCol_Text))
//
//            // Notify OS of text input position for advanced IME (-1 x offset so that Windows IME can cover our cursor. Bit of an extra nicety.)
//            if (isEditable)
//                g.OsImePosRequest = ImVec2(cursor_screen_pos.x - 1, cursor_screen_pos.y - g.FontSize)
//        }
//        else
//        {
//            // Render text only
//            const char* buf_end = NULL
//                    if (isMultiline)
//                        text_size = ImVec2(size.x, InputTextCalcTextLenAndLineCount(buf_display, &buf_end) * g.FontSize) // We don't need width
//            drawWindow->DrawList->AddText(g.Font, g.FontSize, render_pos, GetColorU32(ImGuiCol_Text), buf_display, buf_end, 0.0f, is_multiline ? NULL : &clip_rect)
//        }
//
//        if (isMultiline)
//        {
//            Dummy(text_size + ImVec2(0.0f, g.FontSize)) // Always add room to scroll an extra line
//            EndChildFrame()
//            EndGroup()
//        }
//
//        if (isPassword)
//            PopFont()
//
//        // Log as text
//        if (g.LogEnabled && !isPassword)
//            LogRenderedText(render_pos, buf_display, NULL)
//
//        if (labelSize.x > 0)
//            RenderText(ImVec2(frameBb.Max.x + style.ItemInnerSpacing.x, frameBb.Min.y + style.FramePadding.y), label)
//
//        if ((flags & ImGuiInputTextFlags_EnterReturnsTrue) != 0)
//        return enterPressed
//        else
//        return valueChanged
    return false
}
//IMGUI_API bool          InputFloatN(const char* label, float* v, int components, int decimal_precision, ImGuiInputTextFlags extra_flags);
//IMGUI_API bool          InputIntN(const char* label, int* v, int components, ImGuiInputTextFlags extra_flags);
//IMGUI_API bool          InputScalarEx(const char* label, ImGuiDataType data_type, void* data_ptr, void* step_ptr, void* step_fast_ptr, const char* scalar_format, ImGuiInputTextFlags extra_flags);

/** Create text input in place of a slider (when CTRL+Clicking on slider)   */
fun inputScalarAsWidgetReplacement(aabb: Rect, label: String, dataType: DataType, data: FloatArray, id: Int, decimalPrecision: Int): Boolean {

    val window = currentWindow

    // Our replacement widget will override the focus ID (registered previously to allow for a TAB focus to happen)
    setActiveId(g.scalarAsInputTextId, window)
    setHoveredId(0)
    focusableItemUnregister(window)

    val value = data.format(dataType, decimalPrecision)
    print(data)
//    val textValueChanged = inputTextEx(label, value, aabb.size, InputTextFlags.CharsDecimal or InputTextFlags.AutoSelectAll)
//    if (g.ScalarAsInputTextId == 0) {
//        // First frame
//        IM_ASSERT(g.ActiveId == id)    // InputText ID expected to match the Slider ID (else we'd need to store them both, which is also possible)
//        g.ScalarAsInputTextId = g.ActiveId
//        SetHoveredID(id)
//    } else if (g.ActiveId != g.ScalarAsInputTextId) {
//        // Release
//        g.ScalarAsInputTextId = 0
//    }
//    if (textValueChanged)
//        return DataTypeApplyOpFromText(buf, GImGui->InputTextState.InitialText.begin(), data_type, data_ptr, NULL)
    return false
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
//            precision = s[0]. parse
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