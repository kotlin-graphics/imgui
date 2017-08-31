package imgui.imgui

import gli.has
import gli.hasnt
import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.Context.style
import imgui.ImGui.F32_TO_INT8_SAT
import imgui.ImGui.beginChildFrame
import imgui.ImGui.beginGroup
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.colorButton
import imgui.ImGui.contentRegionMax
import imgui.ImGui.end
import imgui.ImGui.endChildFrame
import imgui.ImGui.endGroup
import imgui.ImGui.endTooltip
import imgui.ImGui.getColorU32
import imgui.ImGui.getColumnOffset
import imgui.ImGui.getColumnWidth
import imgui.ImGui.getMouseDragDelta
import imgui.ImGui.indent
import imgui.ImGui.inputText
import imgui.ImGui.isMouseClicked
import imgui.ImGui.isMouseHoveringRect
import imgui.ImGui.popClipRect
import imgui.ImGui.popFont
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushClipRect
import imgui.ImGui.pushFont
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.ImGui.sameLine
import imgui.ImGui.scrollMaxY
import imgui.ImGui.separator
import imgui.ImGui.setColumnOffset
import imgui.ImGui.setItemAllowOverlap
import imgui.ImGui.sliderFloat
import imgui.ImGui.text
import imgui.ImGui.textLineHeight
import imgui.ImGui.textUnformatted
import imgui.TextEditState.K
import imgui.imgui.imgui_colums.Companion.pixelsToOffsetNorm
import imgui.imgui.imgui_tooltips.Companion.beginTooltipEx
import imgui.internal.*
import java.util.*
import kotlin.apply
import imgui.Context as g

fun main(args: Array<String>) {

    fun c() = null

    fun b() = Unit

    fun a() {
        val a = c() ?: b().also { return }
        println("a")
    }

    a()
}

/** We should always have a CurrentWindow in the stack (there is an implicit "Debug" window)
 *  If this ever crash because g.CurrentWindow is NULL it means that either
 *      - ImGui::NewFrame() has never been called, which is illegal.
 *      - You are calling ImGui functions after ImGui::Render() and before the next ImGui::NewFrame(), which is also
 *          illegal.   */
interface imgui_internal {

    val currentWindowRead get() = g.currentWindow

    val currentWindow get() = g.currentWindow!!.apply { accessed = true }

    val parentWindow: Window
        get() {
            assert(g.currentWindowStack.size >= 2)
            return g.currentWindowStack[g.currentWindowStack.size - 2]
        }

    fun findWindowByName(name: String): Window? {
        // FIXME-OPT: Store sorted hashes -> pointers so we can do a bissection in a contiguous block
        val id = hash(name, 0)
        return g.windows.firstOrNull { it.id == id }
    }

    /** Moving window to front of display (which happens to be back of our sorted list) */
    fun focusWindow(window: Window?) {

        // Always mark the window we passed as focused. This is used for keyboard interactions such as tabbing.
        g.navWindow = window

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
        if ((window.flags has WindowFlags.NoBringToFrontOnFocus) || g.windows.last() === window)
            return
        g.windows.remove(window)
        g.windows.add(window)
    }

    fun initialize() {

        g.logClipboard = StringBuilder()

        assert(g.settings.isEmpty())
        loadIniSettingsFromDisk(IO.iniFilename)
        g.initialized = true
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

        end()

        // Click to focus window and start moving (after we're done with all our widgets)
        if (g.activeId == 0 && g.hoveredId == 0 && IO.mouseClicked[0]) {
            // Unless we just made a popup appear
            if (!(g.navWindow != null && !g.navWindow!!.wasActive && g.navWindow!!.active)) {
                if (g.hoveredRootWindow != null) {
                    focusWindow(g.hoveredWindow)
                    if (g.hoveredWindow!!.flags hasnt WindowFlags.NoMove) {
                        g.movedWindow = g.hoveredWindow
                        g.movedWindowMoveId = g.hoveredRootWindow!!.moveId
                        setActiveId(g.movedWindowMoveId, g.hoveredRootWindow)
                    }
                } else if (g.navWindow != null && getFrontMostModalRootWindow() == null)
                    focusWindow(null)   // Clicking on void disable focus
            }
        }

        /*  Sort the window list so that all child windows are after their parent
            We cannot do that on FocusWindow() because childs may not exist yet         */
        g.windowsSortBuffer.clear()
        g.windows.forEach {
            if (!it.active || it.flags hasnt WindowFlags.ChildWindow)  // if a child is active its parent will add it
                it.addToSortedBuffer()
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
        if (g.activeId == id) g.activeIdIsAlive = true
    }

    /** Advance cursor given item size for layout.  */
    fun itemSize(size: Vec2, textOffsetY: Float = 0f) {

        val window = currentWindow
        if (window.skipItems) return

        // Always align ourselves on pixel boundaries
        val lineHeight = glm.max(window.dc.currentLineHeight, size.y)
        val textBaseOffset = glm.max(window.dc.currentLineTextBaseOffset, textOffsetY)
        window.dc.cursorPosPrevLine.x = window.dc.cursorPos.x + size.x
        window.dc.cursorPosPrevLine.y = window.dc.cursorPos.y
        window.dc.cursorPos.x = (window.pos.x + window.dc.indentX + window.dc.columnsOffsetX).i.f
        window.dc.cursorPos.y = (window.dc.cursorPos.y + lineHeight + style.itemSpacing.y).i.f
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
            if (g.hoveredRootWindow === window.rootWindow)
                if (g.activeId == 0 || (id != 0 && g.activeId == id) || g.activeIdAllowOverlap || (g.activeId == window.moveId))
                    if (window.isContentHoverable)
                        window.dc.lastItemHoveredAndUsable = true
        }

        return true
    }

    fun isClippedEx(bb: Rect, id: Int?, clipEvenWhenLogged: Boolean): Boolean {

        val window = currentWindowRead!!
        if (!(bb overlaps window.clipRect))
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
            if (g.hoveredWindow === window || (flattenChilds && g.hoveredRootWindow === window.rootWindow))
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
                && Key.Tab.isPressed)
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
        if (size lessThan 0f)
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

    /** Mark popup as open (toggle toward open state).
     *  Popups are closed when user click outside, or activate a pressable item, or CloseCurrentPopup() is called within
     *  a BeginPopup()/EndPopup() block.
     *  Popup identifiers are relative to the current ID-stack (so OpenPopup and BeginPopup needs to be at the same
     *  level).
     *  One open popup per level of the popup hierarchy (NB: when assigning we reset the Window member of ImGuiPopupRef
     *  to NULL)    */
    fun openPopupEx(id: Int, reopenExisting: Boolean) {

        val window = g.currentWindow!!
        val currentStackSize = g.currentPopupStack.size
        // Tagged as new ref because constructor sets Window to NULL (we are passing the ParentWindow info here)
        val popupRef = PopupRef(id, window, window.getId("##menus"), IO.mousePos)
        if (g.openPopupStack.size < currentStackSize + 1)
            g.openPopupStack.push(popupRef)
        else if (reopenExisting || g.openPopupStack[currentStackSize].popupId != id)
            g.openPopupStack[currentStackSize] = popupRef
    }


    // New Columns API

    /** setup number of columns. use an identifier to distinguish multiple column sets. close with EndColumns().    */
    fun beginColumns(id: String?, columnsCount: Int, flags: Int) {

        with(currentWindow) {

            assert(columnsCount > 1)
            assert(dc.columnsCount == 1) // Nested columns are currently not supported

            /*  Differentiate column ID with an arbitrary prefix for cases where users name their columns set the same
                as another widget.
                In addition, when an identifier isn't explicitly provided we include the number of columns in the hash
                to make it uniquer. */
            pushId(0x11223347 + if (id != null) 0 else columnsCount)
            dc.columnsSetId = getId(id ?: "columns")
            popId()

            // Set state for first column
            dc.columnsCurrent = 0
            dc.columnsCount = columnsCount
            dc.columnsFlags = flags

            val contentRegionWidth = if (sizeContentsExplicit.x != 0f) sizeContentsExplicit.x else size.x - scrollbarSizes.x
            dc.columnsMinX = dc.indentX - style.itemSpacing.x // Lock our horizontal range
            //window->DC.ColumnsMaxX = contentRegionWidth - window->Scroll.x -((window->Flags & ImGuiWindowFlags_NoScrollbar) ? 0 : g.Style.ScrollbarSize);// - window->WindowPadding().x;
            dc.columnsMaxX = contentRegionWidth - scroll.x
            dc.columnsStartPosY = dc.cursorPos.y
            dc.columnsStartMaxPosX = dc.cursorMaxPos.x
            dc.columnsCellMaxY = dc.cursorPos.y
            dc.columnsCellMinY = dc.cursorPos.y
            dc.columnsOffsetX = 0f
            dc.cursorPos.x = (pos.x + dc.indentX + dc.columnsOffsetX).i.f

            // Cache column offsets
            for (i in 0..columnsCount) dc.columnsData.add(ColumnData())
            for (columnIndex in 0..columnsCount) {

                val columnId = dc.columnsSetId + columnIndex
                keepAliveId(columnId)
                val defaultT = columnIndex / dc.columnsCount.f
                var t = dc.stateStorage.float(columnId, defaultT)
                if (dc.columnsFlags hasnt ColumnsFlags.NoForceWithinWindow)
                    t = glm.min(t, pixelsToOffsetNorm(this, dc.columnsMaxX - style.columnsMinSpacing * (dc.columnsCount - columnIndex)))
                dc.columnsData[columnIndex].offsetNorm = t
            }

            // Cache clipping rectangles
            for (columnIndex in 0 until columnsCount) {
                val clipX1 = glm.floor(0.5f + pos.x + getColumnOffset(columnIndex) - 1f)
                val clipX2 = glm.floor(0.5f + pos.x + getColumnOffset(columnIndex + 1) - 1f)
                dc.columnsData[columnIndex].clipRect.put(clipX1, -Float.MAX_VALUE, clipX2, Float.MAX_VALUE)
                dc.columnsData[columnIndex].clipRect.clipWith(clipRect)
            }
            drawList.channelsSplit(dc.columnsCount)
            pushColumnClipRect()
            pushItemWidth(getColumnWidth() * 0.65f)
        }
    }

    fun endColumns() = with(currentWindow) {

        assert(dc.columnsCount > 1)

        popItemWidth()
        popClipRect()
        drawList.channelsMerge()

        dc.columnsCellMaxY = glm.max(dc.columnsCellMaxY, dc.cursorPos.y)
        dc.cursorPos.y = dc.columnsCellMaxY
        dc.cursorMaxPos.x = glm.max(dc.columnsStartMaxPosX, dc.columnsMaxX)  // Columns don't grow parent

        // Draw columns borders and handle resize
        if (dc.columnsFlags hasnt ColumnsFlags.NoBorder && !skipItems) {

            val y1 = dc.columnsStartPosY
            val y2 = dc.cursorPos.y
            var draggingColumn = -1
            for (i in 1 until dc.columnsCount) {

                val x = pos.x + getColumnOffset(i)
                val columnId = dc.columnsSetId + i
                val columnW = 4f // Width for interaction
                val columnRect = Rect(x - columnW, y1, x + columnW, y2)
                if (isClippedEx(columnRect, columnId, false)) continue

                var hovered = false
                var held = false
                if (dc.columnsFlags hasnt ColumnsFlags.NoResize) {

                    val (_, b, c) = buttonBehavior(columnRect, columnId)
                    hovered = b
                    held = c
                    if (hovered || held)
                        g.mouseCursor = MouseCursor.ResizeEW
                    if (held && g.activeIdIsJustActivated)
                    /*  Store from center of column line (we used a 8 wide rect for columns clicking). This is used by
                        GetDraggedColumnOffset().                     */
                        g.activeIdClickOffset.x -= columnW
                    if (held)
                        draggingColumn = i
                }

                // Draw column
                val col = getColorU32(if (held) Col.SeparatorActive else if (hovered) Col.SeparatorHovered else Col.Separator)
                val xi = x.i.f
                drawList.addLine(Vec2(xi, y1 + 1f), Vec2(xi, y2), col)
            }

            // Apply dragging after drawing the column lines, so our rendered lines are in sync with how items were displayed during the frame.
            if (draggingColumn != -1)
                setColumnOffset(draggingColumn, getDraggedColumnOffset(draggingColumn))
        }

        dc.columnsSetId = 0
        dc.columnsCurrent = 0
        dc.columnsCount = 1
        dc.columnsFlags = 0
        dc.columnsData.clear()
        dc.columnsOffsetX = 0f
        dc.cursorPos.x = (pos.x + dc.indentX + dc.columnsOffsetX).i.f
    }

    fun pushColumnClipRect(columnIndex: Int = -1) {

        val window = currentWindowRead!!
        val columnIndex = if (columnIndex < 0) window.dc.columnsCurrent else columnIndex

        pushClipRect(window.dc.columnsData[columnIndex].clipRect.min, window.dc.columnsData[columnIndex].clipRect.max, false)
    }


    /** NB: All position are in absolute pixels coordinates (never using window coordinates internally)
     *  AVOID USING OUTSIDE OF IMGUI.CPP! NOT FOR PUBLIC CONSUMPTION. THOSE FUNCTIONS ARE A MESS. THEIR SIGNATURE AND
     *  BEHAVIOR WILL CHANGE, THEY NEED TO BE REFACTORED INTO SOMETHING DECENT. */
    fun renderText(pos: Vec2, text: String, textEnd: Int = text.length, hideTextAfterHash: Boolean = true) {

        val window = currentWindow

        // Hide anything after a '##' string
        val textDisplayEnd =
                if (hideTextAfterHash)
                    findRenderedTextEnd(text, textEnd)
                else
                    if (textEnd == 0) text.length else textEnd

        if (textDisplayEnd > 0) {
            window.drawList.addText(g.font, g.fontSize, pos, Col.Text.u32, text.toCharArray(), textDisplayEnd)
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
            window.drawList.addText(g.font, g.fontSize, pos, Col.Text.u32, text.toCharArray(), textEnd, wrapWidth)
            if (g.logEnabled)
                logRenderedText(pos, text, textEnd)
        }
    }


    /** Default clipRect uses (pos_min,pos_max)
     *  Handle clipping on CPU immediately (vs typically let the GPU clip the triangles that are overlapping the clipping
     *  rectangle edges)    */
    fun renderTextClipped(posMin: Vec2, posMax: Vec2, text: String, textEnd: Int = 0, textSizeIfKnown: Vec2? = null,
                          align: Vec2 = Vec2(), clipRect: Rect? = null) {
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
            window.drawList.addText(g.font, g.fontSize, pos, Col.Text.u32, text.toCharArray(), textDisplayEnd, 0f, fineClipRect)
        } else
            window.drawList.addText(g.font, g.fontSize, pos, Col.Text.u32, text.toCharArray(), textDisplayEnd, 0f, null)
//    if (g.logEnabled) TODO
//        LogRenderedText(pos, text, textDisplayEnd)
    }

    /** Render a rectangle shaped with optional rounding and borders    */
    fun renderFrame(pMin: Vec2, pMax: Vec2, fillCol: Int, border: Boolean = true, rounding: Float = 0f) {

        val window = currentWindow

        window.drawList.addRectFilled(pMin, pMax, fillCol, rounding)
        if (border && window.flags has WindowFlags.ShowBorders) {
            window.drawList.addRect(pMin + 1, pMax + 1, Col.BorderShadow.u32, rounding)
            window.drawList.addRect(pMin, pMax, Col.Border.u32, rounding)
        }
    }

    fun renderFrameBorder(pMin: Vec2, pMax: Vec2, rounding: Float = 0f) = with(currentWindow) {
        if (flags has WindowFlags.ShowBorders) {
            drawList.addRect(pMin + 1, pMax + 1, Col.BorderShadow.u32, rounding)
            drawList.addRect(pMin, pMax, Col.Border.u32, rounding)
        }
    }

    /** NB: This is rather brittle and will show artifact when rounding this enabled if rounded corners overlap multiple cells.
     *  Caller currently responsible for avoiding that.
     *  I spent a non reasonable amount of time trying to getting this right for ColorButton with rounding + anti-aliasing +
     *  ColorEditFlags.HalfAlphaPreview flag + various grid sizes and offsets, and eventually gave up...
     *  probably more reasonable to disable rounding alltogether.   */
    fun renderColorRectWithAlphaCheckerboard(pMin: Vec2, pMax: Vec2, col: Int, gridStep: Float, gridOff: Vec2, rounding: Float = 0f,
                                             roundingCornerFlags: Int = 0.inv()) {
        val window = currentWindow
        if (((col and COL32_A_MASK) ushr COL32_A_SHIFT) < 0xFF) {
            val colBg1 = getColorU32(alphaBlendColor(COL32(204, 204, 204, 255), col))
            val colBg2 = getColorU32(alphaBlendColor(COL32(128, 128, 128, 255), col))
            window.drawList.addRectFilled(pMin, pMax, colBg1, rounding, roundingCornerFlags)

            var yi = 0
            var y = pMin.y + gridOff.y
            while (y < pMax.y) {
                val y1 = glm.clamp(y, pMin.y, pMax.y)
                val y2 = glm.min(y + gridStep, pMax.y)
                if (y2 > y1) {
                    var x = pMin.x + gridOff.x + (yi and 1) * gridStep
                    while (x < pMax.x) {
                        val x1 = glm.clamp(x, pMin.x, pMax.x)
                        val x2 = glm.min(x + gridStep, pMax.x)
                        x += gridStep * 2f
                        if (x2 <= x1) continue
                        var roundingCornersFlagsCell = 0
                        if (y1 <= pMin.y) {
                            if (x1 <= pMin.x) roundingCornersFlagsCell = roundingCornersFlagsCell or Corner.TopLeft
                            if (x2 >= pMax.x) roundingCornersFlagsCell = roundingCornersFlagsCell or Corner.TopRight
                        }
                        if (y2 >= pMax.y) {
                            if (x1 <= pMin.x) roundingCornersFlagsCell = roundingCornersFlagsCell or Corner.BotLeft
                            if (x2 >= pMax.x) roundingCornersFlagsCell = roundingCornersFlagsCell or Corner.BotRight
                        }
                        roundingCornersFlagsCell = roundingCornersFlagsCell and roundingCornerFlags
                        val r = if (roundingCornersFlagsCell != 0) rounding else 0f
                        window.drawList.addRectFilled(Vec2(x1, y1), Vec2(x2, y2), colBg2, r, roundingCornersFlagsCell)
                    }
                }
                y += gridStep
                yi++
            }
        } else
            window.drawList.addRectFilled(pMin, pMax, col, rounding, roundingCornerFlags)
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

        window.drawList.addTriangleFilled(a, b, c, Col.Text.u32)
    }

    fun renderBullet(pos: Vec2) = currentWindow.drawList.addCircleFilled(pos, g.fontSize * 0.2f, Col.Text.u32, 8)

    fun renderCheckMark(pos: Vec2, col: Int) {

        val window = currentWindow

        val startX = (g.fontSize * 0.307f + 0.5f).i.f
        val remThird = ((g.fontSize - startX) / 3f).i.f
        val b = Vec2(
                pos.x + 0.5f + startX + remThird,
                pos.y - 1f + (g.font.ascent * (g.fontSize / g.font.fontSize) + 0.5f).i.f + g.font.displayOffset.y.i.f)
        window.drawList.pathLineTo(b - remThird)
        window.drawList.pathLineTo(b)
        window.drawList.pathLineTo(Vec2(b.x + remThird * 2, b.y - remThird * 2))
        window.drawList.pathStroke(col, false)
    }

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

        // Default behavior requires click+release on same spot
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
                if ((flags has ButtonFlags.PressedOnClick && IO.mouseClicked[0]) ||
                        (flags has ButtonFlags.PressedOnDoubleClick && IO.mouseDoubleClicked[0])) {
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
        if (flags has ButtonFlags.AlignTextBaseLine && style.framePadding.y < window.dc.currentLineTextBaseOffset)
            pos.y += window.dc.currentLineTextBaseOffset - style.framePadding.y
        val size = calcItemSize(sizeArg, labelSize.x + style.framePadding.x * 2f, labelSize.y + style.framePadding.y * 2f)

        val bb = Rect(pos, pos + size)
        itemSize(bb, style.framePadding.y)
        if (!itemAdd(bb, id)) return false

        var flags = flags
        if (window.dc.buttonRepeat) flags = flags or ButtonFlags.Repeat
        val (pressed, hovered, held) = buttonBehavior(bb, id, flags)

        // Render
        val col = if (hovered && held) Col.ButtonActive else if (hovered) Col.ButtonHovered else Col.Button
        renderFrame(bb.min, bb.max, col.u32, true, style.frameRounding)
        renderTextClipped(bb.min + style.framePadding, bb.max - style.framePadding, label, 0, labelSize,
                style.buttonTextAlign, bb)

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
        val col = if (held && hovered) Col.CloseButtonActive else if (hovered) Col.CloseButtonHovered else Col.CloseButton
        val center = bb.center
        window.drawList.addCircleFilled(center, glm.max(2f, radius), col.u32, 12)

        val crossExtent = (radius * 0.7071f) - 1f
        if (hovered) {
            window.drawList.addLine(center + crossExtent, center - crossExtent, Col.Text.u32)
            window.drawList.addLine(center + Vec2(crossExtent, -crossExtent), center + Vec2(-crossExtent, crossExtent), Col.Text.u32)
        }

        return pressed
    }


    fun sliderBehavior(frameBb: Rect, id: Int, v: FloatArray, vMin: Float, vMax: Float, power: Float, decimalPrecision: Int,
                       flags: Int = 0) = sliderBehavior(frameBb, id, v, 0, vMin, vMax, power, decimalPrecision, flags)

    fun sliderBehavior(frameBb: Rect, id: Int, v: FloatArray, ptr: Int, vMin: Float, vMax: Float, power: Float, decimalPrecision: Int,
                       flags: Int = 0): Boolean {

        val window = currentWindow

//        println("Draw frame, ${v[ptr]}")
        // Draw frame
        renderFrame(frameBb.min, frameBb.max, Col.FrameBg.u32, true, style.frameRounding)

        val isNonLinear = (power < 1.0f - 0.00001f) || (power > 1.0f + 0.00001f)
        val isHorizontal = flags hasnt SliderFlags.Vertical

        val grabPadding = 2f
        val sliderSz = (if (isHorizontal) frameBb.width else frameBb.height) - grabPadding * 2f
        val grabSz =
                if (decimalPrecision != 0)
                    glm.min(style.grabMinSize, sliderSz)
                else
                    glm.min(
                            glm.max(1f * (sliderSz / ((if (vMin < vMax) vMax - vMin else vMin - vMax) + 1f)), style.grabMinSize),
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
        if (g.activeId == id) {

            var setNewValue = false
            var clickedT = 0f

            if (IO.mouseDown[0]) {

                val mouseAbsPos = if (isHorizontal) IO.mousePos.x else IO.mousePos.y
                clickedT =
                        if (sliderUsableSz > 0f)
                            glm.clamp((mouseAbsPos - sliderUsablePosMin) / sliderUsableSz, 0f, 1f)
                        else 0f
                if (!isHorizontal)
                    clickedT = 1f - clickedT

                setNewValue = true
            } else
                clearActiveId()

            if (setNewValue) {
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
                if (v[ptr] != newValue) {
                    v[ptr] = newValue
                    valueChanged = true
                }
            }
        }

        // Draw
        var grabT = sliderBehaviorCalcRatioFromValue(v[ptr], vMin, vMax, power, linearZeroPos)
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
        val col = if (g.activeId == id) Col.SliderGrabActive else Col.SliderGrab
        window.drawList.addRectFilled(grabBb.min, grabBb.max, col.u32, style.grabRounding)

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

    fun sliderFloatN(label: String, v: FloatArray, vMin: Float, vMax: Float, displayFormat: String, power: Float): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        var valueChanged = false
        beginGroup()
        pushId(label)
        pushMultiItemsWidths(v.size)
        for (i in v.indices) {
            pushId(i)
//            println(i) TODO clean
            valueChanged = valueChanged || sliderFloat("##v", v, i, vMin, vMax, displayFormat, power)
//            println("wtf")
            sameLine(0f, style.itemInnerSpacing.x)
            popId()
            popItemWidth()
        }
        popId()

        textUnformatted(label, findRenderedTextEnd(label))
        endGroup()
        return valueChanged
    }

//IMGUI_API bool          SliderIntN(const char* label, int* v, int components, int v_min, int v_max, const char* display_format);

    fun dragBehavior(frameBb: Rect, id: Int, v: FloatArray, ptr: Int, vSpeed: Float, vMin: Float, vMax: Float, decimalPrecision: Int,
                     power: Float): Boolean {

        // Draw frame
        val frameCol = when (id) {
            g.activeId -> Col.FrameBgActive
            g.hoveredId -> Col.FrameBgHovered
            else -> Col.FrameBg
        }
        renderFrame(frameBb.min, frameBb.max, frameCol.u32, true, style.frameRounding)

        var valueChanged = false

        // Process clicking on the drag
        if (g.activeId == id)

            if (IO.mouseDown[0]) {

                if (g.activeIdIsJustActivated) {
                    // Lock current value on click
                    g.dragCurrentValue = v[ptr]
                    g.dragLastMouseDelta put 0f
                }

                var vSpeed = vSpeed
                if (vSpeed == 0f && (vMax - vMin) != 0f && (vMax - vMin) < Float.MAX_VALUE)
                    vSpeed = (vMax - vMin) * g.dragSpeedDefaultRatio

                var vCur = g.dragCurrentValue
                val mouseDragDelta = getMouseDragDelta(0, 1f)
                if (glm.abs(mouseDragDelta.x - g.dragLastMouseDelta.x) > 0f) {
                    var speed = vSpeed
                    if (IO.keyShift && g.dragSpeedScaleFast >= 0f)
                        speed *= g.dragSpeedScaleFast
                    if (IO.keyAlt && g.dragSpeedScaleSlow >= 0f)
                        speed *= g.dragSpeedScaleSlow

                    val adjustDelta = (mouseDragDelta.x - g.dragLastMouseDelta.x) * speed
                    if (glm.abs(power - 1f) > 0.001f) {
                        // Logarithmic curve on both side of 0.0
                        val v0_abs = if (vCur >= 0f) vCur else -vCur
                        val v0_sign = if (vCur >= 0f) 1f else -1f
                        val v1 = glm.pow(v0_abs, 1f / power) + adjustDelta * v0_sign
                        val v1_abs = if (v1 >= 0f) v1 else -v1
                        val v1_sign = if (v1 >= 0f) 1f else -1f              // Crossed sign line
                        vCur = glm.pow(v1_abs, power) * v0_sign * v1_sign   // Reapply sign
                    } else
                        vCur += adjustDelta

                    g.dragLastMouseDelta.x = mouseDragDelta.x

                    // Clamp
                    if (vMin < vMax)
                        vCur = glm.clamp(vCur, vMin, vMax)
                    g.dragCurrentValue = vCur
                }

                // Round to user desired precision, then apply
                vCur = roundScalar(vCur, decimalPrecision)
                if (v[ptr] != vCur) {
                    v[ptr] = vCur
                    valueChanged = true
                }
            } else
                clearActiveId()

        return valueChanged
    }
//IMGUI_API bool          DragFloatN(const char* label, float* v, int components, float v_speed, float v_min, float v_max, const char* display_format, float power);
//IMGUI_API bool          DragIntN(const char* label, int* v, int components, float v_speed, int v_min, int v_max, const char* display_format);


    fun inputTextEx(label: String, buf: CharArray, sizeArg: Vec2, flags: Int
            /*, ImGuiTextEditCallback callback = NULL, void* user_data = NULL*/): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        // Can't use both together (they both use up/down keys)
        assert((flags has InputTextFlags.CallbackHistory) xor (flags has InputTextFlags.Multiline))
        // Can't use both together (they both use tab key)
        assert((flags has InputTextFlags.CallbackCompletion) xor (flags has InputTextFlags.AllowTabInput))

        val isMultiline = flags has InputTextFlags.Multiline
        val isEditable = flags hasnt InputTextFlags.ReadOnly
        val isPassword = flags has InputTextFlags.Password

        if (isMultiline) // Open group before calling GetID() because groups tracks id created during their spawn
            beginGroup()
        val id = window.getId(label)
        val labelSize = calcTextSize(label, 0, true)
        val size = calcItemSize(sizeArg, calcItemWidth(),
                // Arbitrary default of 8 lines high for multi-line
                (if (isMultiline) textLineHeight * 8f else labelSize.y) + style.framePadding.y * 2f)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        val totalBb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))

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
            itemSize(totalBb, style.framePadding.y)
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
                // wchar count <= UTF-8 count. we use +1 to make sure that .Data isn't NULL so it doesn't crash. TODO check if needed
                editState.text = CharArray(buf.size)
                editState.initialText = CharArray(buf.size)
                // UTF-8. we use +1 to make sure that .Data isn't NULL so it doesn't crash. TODO check if needed
//                editState.initialText.add('\u0000')
                editState.initialText strncpy buf
                var bufEnd = 0
                editState.curLenW = editState.text.textStr(buf) // TODO check if ImTextStrFromUtf8 needed
                /*  We can't get the result from ImFormatString() above because it is not UTF-8 aware.
                    Here we'll cut off malformed UTF-8.                 */
                editState.curLenA = editState.curLenW //TODO check (int)(bufEnd - buf)
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
                    editState.state.clear(!isMultiline)
                    if (!isMultiline && focusRequestedByCode)
                        selectAll = true
                }
                if (flags has InputTextFlags.AlwaysInsertMode)
                    editState.state.insertMode = true
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

                TODO()
                // When read-only we always use the live data passed to the function
//                editState.text.add('\u0000')
//                const char* buf_end = NULL
//                        editState.CurLenW = ImTextStrFromUtf8(editState.Text.Data, editState.Text.Size, buf, NULL, &buf_end)
//                editState.CurLenA = (int)(buf_end - buf)
//                editState.CursorClamp()
            }

            editState.bufSizeA = buf.size

            /*  Although we are active we don't prevent mouse from hovering other elements unless we are interacting
                right now with the widget.
                Down the line we should have a cleaner library-wide concept of Selected vs Active.  */
            g.activeIdAllowOverlap = !IO.mouseDown[0]

            // Edit in progress
            val mouseX = IO.mousePos.x - frameBb.min.x - style.framePadding.x + editState.scrollX
            val mouseY =
                    if (isMultiline)
                        IO.mousePos.y - drawWindow.dc.cursorPos.y - style.framePadding.y
                    else g.fontSize * 0.5f

            // OS X style: Double click selects by word instead of selecting whole text
            val osxDoubleClickSelectsWords = IO.osxBehaviors
            if (selectAll || (hovered && !osxDoubleClickSelectsWords && IO.mouseDoubleClicked[0])) {
                editState.selectAll()
                editState.selectedAllMouseLock = true
            } else if (hovered && osxDoubleClickSelectsWords && IO.mouseDoubleClicked[0]) {
                // Select a word only, OS X style (by simulating keystrokes)
                editState.onKeyPressed(K.WORDLEFT)
                editState.onKeyPressed(K.WORDRIGHT or K.SHIFT)
            } else if (IO.mouseClicked[0] && !editState.selectedAllMouseLock) {
                editState.click(mouseX, mouseY)
                editState.cursorAnimReset()
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
                        var c = IO.inputCharacters[n].i
                        if (c == 0) continue
                        // Insert character if they pass filtering
                        val pChar = intArrayOf(c)
                        if (!inputTextFilterCharacter(pChar, flags/*, callback, user_data*/))
                            continue
                        c = pChar[0]
                        editState.onKeyPressed(c)
                    }

                // Consume characters
                IO.inputCharacters.fill('\u0000')
            }

            // Handle various key-presses
            var cancelEdit = false
            val kMask = if (IO.keyShift) K.SHIFT else 0
            // OS X style: Shortcuts using Cmd/Super instead of Ctrl
            val isShortcutKeyOnly = (if (IO.osxBehaviors) IO.keySuper && !IO.keyCtrl else IO.keyCtrl && !IO.keySuper) && !IO.keyAlt && !IO.keyShift
            // OS X style: Text editing cursor movement using Alt instead of Ctrl
            val isWordmoveKeyDown = if (IO.osxBehaviors) IO.keyAlt else IO.keyCtrl
            // OS X style: Line/Text Start and End using Cmd+Arrows instead of Home/End
            val isStartendKeyDown = IO.osxBehaviors && IO.keySuper && !IO.keyCtrl && !IO.keyAlt

            when {
                Key.LeftArrow.isPressed -> editState.onKeyPressed(
                        when {
                            isStartendKeyDown -> K.LINESTART
                            isWordmoveKeyDown -> K.WORDLEFT
                            else -> K.LEFT
                        } or kMask)
                Key.RightArrow.isPressed -> editState.onKeyPressed(
                        when {
                            isStartendKeyDown -> K.LINEEND
                            isWordmoveKeyDown -> K.WORDRIGHT
                            else -> K.RIGHT
                        } or kMask)
                Key.UpArrow.isPressed && isMultiline ->
                    if (IO.keyCtrl)
                        drawWindow.setScrollY(glm.max(drawWindow.scroll.y - g.fontSize, 0f))
                    else
                        editState.onKeyPressed((if (isStartendKeyDown) K.TEXTSTART else K.UP) or kMask)
                Key.DownArrow.isPressed && isMultiline ->
                    if (IO.keyCtrl)
                        drawWindow.setScrollY(glm.min(drawWindow.scroll.y + g.fontSize, scrollMaxY))
                    else
                        editState.onKeyPressed((if (isStartendKeyDown) K.TEXTEND else K.DOWN) or kMask)
                Key.Home.isPressed -> editState.onKeyPressed((if (IO.keyCtrl) K.TEXTSTART else K.LINESTART) or kMask)
                Key.End.isPressed -> editState.onKeyPressed((if (IO.keyCtrl) K.TEXTEND else K.LINEEND) or kMask)
                Key.Delete.isPressed && isEditable -> editState.onKeyPressed(K.DELETE or kMask)
                Key.Backspace.isPressed && isEditable -> {
                    if (!editState.hasSelection) {
                        if (isWordmoveKeyDown)
                            editState.onKeyPressed(K.WORDLEFT or K.SHIFT)
                        else if (IO.osxBehaviors && IO.keySuper && !IO.keyAlt && !IO.keyCtrl)
                            editState.onKeyPressed(K.LINESTART or K.SHIFT)
                    }
                    editState.onKeyPressed(K.BACKSPACE or kMask)
                }
                Key.Enter.isPressed -> {
                    val ctrlEnterForNewLine = flags has InputTextFlags.CtrlEnterForNewLine
                    if (!isMultiline || (ctrlEnterForNewLine && !IO.keyCtrl) || (!ctrlEnterForNewLine && IO.keyCtrl)) {
                        clearActiveId()
                        enterPressed = true
                    } else if (isEditable) {
                        val c = '\n' // Insert new line
                        TODO()
//                        if (inputTextFilterCharacter(& c, flags, callback, user_data))
//                        editState.OnKeyPressed((int) c)
                    }
                }
                flags has InputTextFlags.AllowTabInput && Key.Tab.isPressed && !IO.keyCtrl && !IO.keyShift && !IO.keyAlt && isEditable -> {
                    val c = '\t' // Insert TAB
                    TODO()
//                    if (InputTextFilterCharacter(& c, flags, callback, user_data))
//                    editState.OnKeyPressed((int) c)
                }
                Key.Escape.isPressed -> {
                    clearActiveId()
                    cancelEdit = true
                }
                isShortcutKeyOnly -> when {

                    Key.Z.isPressed && isEditable -> {
                        editState.onKeyPressed(K.UNDO)
                        editState.clearSelection()
                    }
                    Key.Y.isPressed && isEditable -> {
                        editState.onKeyPressed(K.REDO)
                        editState.clearSelection()
                    }
                    Key.A.isPressed -> {
                        editState.selectAll()
                        editState.cursorFollow = true
                    }
                    !isPassword && ((Key.X.isPressed && isEditable) || Key.C.isPressed) && (!isMultiline || editState.hasSelection) -> {
                        // Cut, Copy
                        val cut = Key.X.isPressed
                        if (cut && !editState.hasSelection)
                            editState.selectAll()

                        TODO()
//                        if (IO.setClipboardTextFn) {
//                            val ib =
//                                    if (editState.hasSelection) glm.min(editState.state.selectStart, editState.state.selectEnd)
//                                    else 0
//                            val ie =
//                                    if(editState.hasSelection) glm.max(editState.state.selectStart, editState.state.selectEnd)
//                                    else editState.curLenW
//                            editState.TempTextBuffer.resize((ie - ib) * 4 + 1)
//                            ImTextStrToUtf8(editState.TempTextBuffer.Data, editState.TempTextBuffer.Size, editState.Text.Data + ib, editState.Text.Data + ie)
//                            SetClipboardText(editState.TempTextBuffer.Data)
//                        }
//
//                        if (cut) {
//                            editState.CursorFollow = true
//                            stb_textedit_cut(& editState, &editState.StbState)
//                        }
                    }
                    Key.V.isPressed && isEditable -> {
                        TODO()

//                        val clipboard = getClipboardText ()
//                        // Paste
//                        if (clipboard) {
//                            // Filter pasted buffer
//                            const int clipboard_len = (int) strlen (clipboard)
//                            ImWchar * clipboard_filtered = (ImWchar *) ImGui ::MemAlloc((clipboard_len + 1) * sizeof(ImWchar))
//                            int clipboard_filtered_len = 0
//                            for (const char* s = clipboard; *s; )
//                            {
//                                unsigned int c
//                                s += ImTextCharFromUtf8(& c, s, NULL)
//                                if (c == 0)
//                                    break
//                                if (c >= 0x10000 || !InputTextFilterCharacter(& c, flags, callback, user_data))
//                                continue
//                                clipboard_filtered[clipboard_filtered_len++] = (ImWchar) c
//                            }
//                            clipboard_filtered[clipboard_filtered_len] = 0
//                            if (clipboard_filtered_len > 0) // If everything was filtered, ignore the pasting operation
//                            {
//                                stb_textedit_paste(& editState, &editState.StbState, clipboard_filtered, clipboard_filtered_len)
//                                editState.CursorFollow = true
//                            }
//                            ImGui::MemFree(clipboard_filtered)
//                        }
                    }
                }
            }

            if (cancelEdit) {
                // Restore initial value
                if (isEditable) {
                    TODO()
//                        ImStrncpy(buf, editState.InitialText.Data, buf_size)
//                        valueChanged = true
                }
            } else {
                /*  Apply new value immediately - copy modified buffer back
                    Note that as soon as the input box is active, the in-widget value gets priority over any
                    underlying modification of the input buffer
                    FIXME: We actually always render 'buf' when calling DrawList->AddText, making the comment above
                    incorrect.
                    FIXME-OPT: CPU waste to do this every time the widget is active, should mark dirty state from
                    the stb_textedit callbacks. */
                if (isEditable)
                    editState.tempTextBuffer = editState.text.clone()

                // User callback
                if (flags has (InputTextFlags.CallbackCompletion or InputTextFlags.CallbackHistory or InputTextFlags.CallbackAlways)) {

                    TODO()
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
                }

                // Copy back to user buffer
                if (isEditable && !Arrays.equals(editState.tempTextBuffer, buf)) {
                    repeat(buf.size) { buf[it] = editState.tempTextBuffer[it] }
                    valueChanged = true
                }
            }
        }

        // ------------------------- Render -------------------------
        /*  Select which buffer we are going to display. When ImGuiInputTextFlags_NoLiveEdit is set 'buf' might still
            be the old value. We set buf to NULL to prevent accidental usage from now on.         */
        val bufDisplay = if (g.activeId == id && isEditable) editState.tempTextBuffer else buf
//        buf[0] = ""

        if (!isMultiline)
            renderFrame(frameBb.min, frameBb.max, Col.FrameBg.u32, true, style.frameRounding)

        val clipRect = Vec4(frameBb.min, frameBb.min + size) // Not using frameBb.Max because we have adjusted size
        val renderPos = if (isMultiline) Vec2(drawWindow.dc.cursorPos) else frameBb.min + style.framePadding
        val textSize = Vec2()
        val isCurrentlyScrolling = editState.id == id && isMultiline && g.activeId == drawWindow.getIdNoKeepAlive("#SCROLLY")
        if (g.activeId == id || isCurrentlyScrolling) {

            editState.cursorAnim += IO.deltaTime

            /*  This is going to be messy. We need to:
                    - Display the text (this alone can be more easily clipped)
                    - Handle scrolling, highlight selection, display cursor (those all requires some form of 1d->2d
                        cursor position calculation)
                    - Measure text height (for scrollbar)
                We are attempting to do most of that in **one main pass** to minimize the computation cost
                (non-negligible for large amount of text) + 2nd pass for selection rendering (we could merge them by an
                extra refactoring effort)   */
            // FIXME: This should occur on bufDisplay but we'd need to maintain cursor/select_start/select_end for UTF-8.
            val text = editState.text
            val cursorOffset = Vec2()
            val selectStartOffset = Vec2()

            run {
                // Count lines + find lines numbers straddling 'cursor' and 'select_start' position.
                val searchesInputPtr = intArrayOf(0 + editState.state.cursor, -1)
                var searchesRemaining = 1
                val searchesResultLineNumber = intArrayOf(-1, -999)
                if (editState.state.selectStart != editState.state.selectEnd) {
                    searchesInputPtr[1] = glm.min(editState.state.selectStart, editState.state.selectEnd)
                    searchesResultLineNumber[1] = -1
                    searchesRemaining++
                }

                // Iterate all lines to find our line numbers
                // In multi-line mode, we never exit the loop until all lines are counted, so add one extra to the searchesRemaining counter.
                if (isMultiline) searchesRemaining++
                var lineCount = 0
                for (s in text.indices)
                    if (text[s] == '\n') {
                        lineCount++
                        if (searchesResultLineNumber[0] == -1 && s >= searchesInputPtr[0]) {
                            searchesResultLineNumber[0] = lineCount
                            if (--searchesRemaining <= 0) break
                        }
                        if (searchesResultLineNumber[1] == -1 && s >= searchesInputPtr[1]) {
                            searchesResultLineNumber[1] = lineCount
                            if (--searchesRemaining <= 0) break
                        }
                    }
                lineCount++
                if (searchesResultLineNumber[0] == -1) searchesResultLineNumber[0] = lineCount
                if (searchesResultLineNumber[1] == -1) searchesResultLineNumber[1] = lineCount

                // Calculate 2d position by finding the beginning of the line and measuring distance
                var start = text.beginOfLine(searchesInputPtr[0])
                var length = text.size - start
                cursorOffset.x = inputTextCalcTextSizeW(String(text, start, length), searchesInputPtr[0]).x
                cursorOffset.y = searchesResultLineNumber[0] * g.fontSize
                if (searchesResultLineNumber[1] >= 0) {
                    start = text.beginOfLine(searchesInputPtr[1])
                    length = text.size - start
                    selectStartOffset.x = inputTextCalcTextSizeW(String(text, start, length), searchesInputPtr[1]).x
                    selectStartOffset.y = searchesResultLineNumber[1] * g.fontSize
                }

                // Store text height (note that we haven't calculated text width at all, see GitHub issues #383, #1224)
                if (isMultiline)
                    textSize.put(size.x, lineCount * g.fontSize)
            }

            // Scroll
            if (editState.cursorFollow) {
                // Horizontal scroll in chunks of quarter width
                if (flags hasnt InputTextFlags.NoHorizontalScroll) {
                    val scrollIncrementX = size.x * 0.25f
                    if (cursorOffset.x < editState.scrollX)
                        editState.scrollX = (glm.max(0f, cursorOffset.x - scrollIncrementX)).i.f
                    else if (cursorOffset.x - size.x >= editState.scrollX)
                        editState.scrollX = (cursorOffset.x - size.x + scrollIncrementX).i.f
                } else
                    editState.scrollX = 0f

                // Vertical scroll
                if (isMultiline) {
                    var scrollY = drawWindow.scroll.y
                    if (cursorOffset.y - g.fontSize < scrollY)
                        scrollY = glm.max(0f, cursorOffset.y - g.fontSize)
                    else if (cursorOffset.y - size.y >= scrollY)
                        scrollY = cursorOffset.y - size.y
                    drawWindow.dc.cursorPos.y += drawWindow.scroll.y - scrollY   // To avoid a frame of lag
                    drawWindow.scroll.y = scrollY
                    renderPos.y = drawWindow.dc.cursorPos.y
                }
            }
            editState.cursorFollow = false
            val renderScroll = Vec2(editState.scrollX, 0f)

            // Draw selection
            if (editState.state.selectStart != editState.state.selectEnd) {

                val textSelectedBegin = glm.min(editState.state.selectStart, editState.state.selectEnd)
                val textSelectedEnd = glm.max(editState.state.selectStart, editState.state.selectEnd)

                // FIXME: those offsets should be part of the style? they don't play so well with multi-line selection.
                val bgOffYUp = if (isMultiline) 0f else -1f
                val bgOffYDn = if (isMultiline) 0f else 2f
                val bgColor = Col.TextSelectedBg.u32
                val rectPos = renderPos + selectStartOffset - renderScroll
                var p = textSelectedBegin
                while (p < textSelectedEnd) {
                    if (rectPos.y > clipRect.w + g.fontSize) break
                    if (rectPos.y < clipRect.y) {
                        while (p < textSelectedEnd)
                            if (text[p++] == '\n')
                                break
                    } else {
                        val start = text.beginOfLine(p)
                        val end = text.size - start
                        val rectSize = inputTextCalcTextSizeW(String(text, start, end), textSelectedEnd, stopOnNewLine = true)
                        // So we can see selected empty lines
                        if (rectSize.x <= 0f) rectSize.x = (g.font.getCharAdvance_A(' ') * 0.5f).i.f
                        val rect = Rect(rectPos + Vec2(0f, bgOffYUp - g.fontSize), rectPos + Vec2(rectSize.x, bgOffYDn))
                        val clipRect_ = Rect(clipRect)
                        rect.clipWith(clipRect_)
                        if (rect overlaps clipRect_)
                            drawWindow.drawList.addRectFilled(rect.min, rect.max, bgColor)
                    }
                    rectPos.x = renderPos.x - renderScroll.x
                    rectPos.y += g.fontSize
                }
            }

            drawWindow.drawList.addText(g.font, g.fontSize, renderPos - renderScroll, Col.Text.u32, bufDisplay,
                    editState.curLenA, 0f, if (isMultiline) null else clipRect)

            // Draw blinking cursor
            val cursorIsVisible = g.inputTextState.cursorAnim <= 0f || glm.mod(g.inputTextState.cursorAnim, 1.2f) <= 0.8f
            val cursorScreenPos = renderPos + cursorOffset - renderScroll
            val cursorScreenRect = Rect(cursorScreenPos.x, cursorScreenPos.y - g.fontSize + 0.5f, cursorScreenPos.x + 1f, cursorScreenPos.y - 1.5f)
            if (cursorIsVisible && cursorScreenRect overlaps Rect(clipRect))
                drawWindow.drawList.addLine(cursorScreenRect.min, cursorScreenRect.bl, Col.Text.u32)

            /*  Notify OS of text input position for advanced IME (-1 x offset so that Windows IME can cover our cursor.
                Bit of an extra nicety.)             */
            if (isEditable)
                g.osImePosRequest = Vec2(cursorScreenPos.x - 1, cursorScreenPos.y - g.fontSize)
        } else {
            // Render text only
            val bufEnd = IntArray(1)
            if (isMultiline)
            // We don't need width
                textSize.put(size.x, inputTextCalcTextLenAndLineCount(bufDisplay.contentToString(), bufEnd) * g.fontSize)
            drawWindow.drawList.addText(g.font, g.fontSize, renderPos, Col.Text.u32, bufDisplay, bufEnd[0], 0f,
                    if (isMultiline) null else clipRect)
        }

        if (isMultiline) {
            TODO()
//            dummy(textSize + ImVec2(0.0f, g.FontSize)) // Always add room to scroll an extra line
//            EndChildFrame()
//            EndGroup()
        }

        if (isPassword)
            popFont()

        // Log as text
        if (g.logEnabled && !isPassword)
            logRenderedText(renderPos, String(bufDisplay))

        if (labelSize.x > 0)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, frameBb.min.y + style.framePadding.y), label)

        return if (flags has InputTextFlags.EnterReturnsTrue) enterPressed else valueChanged
    }
//IMGUI_API bool          InputFloatN(const char* label, float* v, int components, int decimal_precision, ImGuiInputTextFlags extra_flags);
//IMGUI_API bool          InputIntN(const char* label, int* v, int components, ImGuiInputTextFlags extra_flags);

    /** NB: scalar_format here must be a simple "%xx" format string with no prefix/suffix (unlike the Drag/Slider
     *  functions "display_format" argument)    */
    fun inputScalarEx(label: String, dataType: DataType, data: IntArray, step: Number?, stepFast: Number?, scalarFormat: String,
                      extraFlags: Int): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val labelSize = calcTextSize(label, true)

        beginGroup()
        pushId(label)
        val buttonSz = Vec2(g.fontSize) + style.framePadding * 2f
        step?.let { pushItemWidth(glm.max(1f, calcItemWidth() - (buttonSz.x + style.itemInnerSpacing.x) * 2)) }

        val buf = data.format(dataType, scalarFormat, CharArray(64))

        var valueChanged = false
        var extraFlags = extraFlags
        if (extraFlags hasnt InputTextFlags.CharsHexadecimal)
            extraFlags = extraFlags or InputTextFlags.CharsDecimal
        extraFlags = extraFlags or InputTextFlags.AutoSelectAll
        if (inputText("", buf, extraFlags)) // PushId(label) + "" gives us the expected ID from outside point of view
            valueChanged = dataTypeApplyOpFromText(buf, g.inputTextState.initialText, dataType, data, scalarFormat)

        // Step buttons
        step?.let {
            popItemWidth()
            sameLine(0f, style.itemInnerSpacing.x)
            if (buttonEx("-", buttonSz, ButtonFlags.Repeat or ButtonFlags.DontClosePopups)) {
                dataTypeApplyOp(dataType, '-', data, if (IO.keyCtrl && stepFast != null) stepFast else step)
                valueChanged = true
            }
            sameLine(0f, style.itemInnerSpacing.x)
            if (buttonEx("+", buttonSz, ButtonFlags.Repeat or ButtonFlags.DontClosePopups)) {
                dataTypeApplyOp(dataType, '+', data, if (IO.keyCtrl && stepFast != null) stepFast else step)
                valueChanged = true
            }
        }
        popId()

        if (labelSize.x > 0) {
            sameLine(0f, style.itemInnerSpacing.x)
            renderText(Vec2(window.dc.cursorPos.x, window.dc.cursorPos.y + style.framePadding.y), label)
            itemSize(labelSize, style.framePadding.y)
        }
        endGroup()

        return valueChanged
    }

    /** Create text input in place of a slider (when CTRL+Clicking on slider)
     *  FIXME: Logic is messy and confusing. */
    fun inputScalarAsWidgetReplacement(aabb: Rect, label: String, dataType: DataType, data: IntArray, id: Int, decimalPrecision: Int)
            : Boolean {

        val window = currentWindow

        // Our replacement widget will override the focus ID (registered previously to allow for a TAB focus to happen)
        setActiveId(g.scalarAsInputTextId, window)
        setHoveredId(0)
        focusableItemUnregister(window)

        val buf = CharArray(32)
        data.format(dataType, decimalPrecision, buf)
        val textValueChanged = inputTextEx(label, buf, aabb.size, InputTextFlags.CharsDecimal or InputTextFlags.AutoSelectAll)
        if (g.scalarAsInputTextId == 0) {   // First frame we started displaying the InputText widget
            // InputText ID expected to match the Slider ID (else we'd need to store them both, which is also possible)
            assert(g.activeId == id)
            g.scalarAsInputTextId = g.activeId
            setHoveredId(id)
        } else if (g.activeId != g.scalarAsInputTextId)
            g.scalarAsInputTextId = 0   // Release
        if (textValueChanged)
            return dataTypeApplyOpFromText(buf, g.inputTextState.initialText, dataType, data)
        return false
    }

    /** Note: only access 3 floats if ImGuiColorEditFlags_NoAlpha flag is set.   */
    fun colorTooltip(text: String, col: FloatArray, flags: Int) {

        val cr = F32_TO_INT8_SAT(col[0])
        val cg = F32_TO_INT8_SAT(col[1])
        val cb = F32_TO_INT8_SAT(col[2])
        val ca = if (flags has ColorEditFlags.NoAlpha) 255 else F32_TO_INT8_SAT(col[3])
        beginTooltipEx(true)

        val textEnd = if (text.isEmpty()) findRenderedTextEnd(text) else 0
        if (textEnd > 0) {
            textUnformatted(text, textEnd)
            separator()
        }
        val sz = Vec2(g.fontSize * 3)
        val f = (flags and (ColorEditFlags.NoAlpha or ColorEditFlags.AlphaPreview or ColorEditFlags.AlphaPreviewHalf)) or ColorEditFlags.NoTooltip
        colorButton("##preview", Vec4(col), f, sz)
        sameLine()
        if (flags has ColorEditFlags.NoAlpha)
            text("#%02X%02X%02X\nR: $cr, G: $cg, B: $cb\n(%.3f, %.3f, %.3f)", cr, cg, cb, col[0], col[1], col[2])
        else
            text("#%02X%02X%02X%02X\nR:$cr, G:$cg, B:$cb, A:$ca\n(%.3f, %.3f, %.3f, %.3f)", cr, cg, cb, ca, col[0], col[1], col[2], col[3])
        endTooltip()
    }

    fun treeNodeBehavior(id: Int, flags: Int, label: String): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val displayFrame = flags has TreeNodeFlags.Framed
        val padding = if (displayFrame) Vec2(style.framePadding) else Vec2(style.framePadding.x, 0f)

        val labelSize = calcTextSize(label, false)

        // We vertically grow up to current line height up the typical widget height.
        val textBaseOffsetY = glm.max(0f, window.dc.currentLineTextBaseOffset - padding.y) // Latch before ItemSize changes it
        val frameHeight = glm.max(glm.min(window.dc.currentLineHeight, g.fontSize + style.framePadding.y * 2), labelSize.y + padding.y * 2)
        val bb = Rect(window.dc.cursorPos, Vec2(window.pos.x + contentRegionMax.x, window.dc.cursorPos.y + frameHeight))
        if (displayFrame) {
            // Framed header expand a little outside the default padding
            bb.min.x -= (window.windowPadding.x * 0.5f).i.f - 1
            bb.max.x += (window.windowPadding.x * 0.5f).i.f - 1
        }

        val textOffsetX = g.fontSize + padding.x * if (displayFrame) 3 else 2   // Collapser arrow width + Spacing
        val textWidth = g.fontSize + if (labelSize.x > 0f) labelSize.x + padding.x * 2 else 0f   // Include collapser
        itemSize(Vec2(textWidth, frameHeight), textBaseOffsetY)

        /*  For regular tree nodes, we arbitrary allow to click past 2 worth of ItemSpacing
            (Ideally we'd want to add a flag for the user to specify we want want the hit test to be done up to the
            right side of the content or not)         */
        val interactBb = if (displayFrame) Rect(bb) else Rect(bb.min.x, bb.min.y, bb.min.x + textWidth + style.itemSpacing.x * 2, bb.max.y)
        var isOpen = treeNodeBehaviorIsOpen(id, flags)

        if (!itemAdd(interactBb, id)) {
            if (isOpen && flags hasnt TreeNodeFlags.NoTreePushOnOpen)
                treePushRawId(id)
            return isOpen
        }

        /*  Flags that affects opening behavior:
                - 0(default) ..................... single-click anywhere to open
                - OpenOnDoubleClick .............. double-click anywhere to open
                - OpenOnArrow .................... single-click on arrow to open
                - OpenOnDoubleClick|OpenOnArrow .. single-click on arrow or double-click anywhere to open   */
        var buttonFlags = ButtonFlags.NoKeyModifiers or
                if (flags has TreeNodeFlags.AllowOverlapMode) ButtonFlags.AllowOverlapMode else ButtonFlags.Null
        if (flags has TreeNodeFlags.OpenOnDoubleClick)
            buttonFlags = buttonFlags or ButtonFlags.PressedOnDoubleClick or (
                    if (flags has TreeNodeFlags.OpenOnArrow) ButtonFlags.PressedOnClickRelease else ButtonFlags.Null)
        val (pressed, hovered, held) = buttonBehavior(interactBb, id, buttonFlags)
        if (pressed && flags hasnt TreeNodeFlags.Leaf) {
            var toggled = flags hasnt (TreeNodeFlags.OpenOnArrow or TreeNodeFlags.OpenOnDoubleClick)
            if (flags has TreeNodeFlags.OpenOnArrow)
                toggled = toggled or isMouseHoveringRect(interactBb.min, Vec2(interactBb.min.x + textOffsetX, interactBb.max.y))
            if (flags has TreeNodeFlags.OpenOnDoubleClick)
                toggled = toggled or IO.mouseDoubleClicked[0]
            if (toggled) {
                isOpen = !isOpen
                window.dc.stateStorage[id] = isOpen
            }
        }
        if (flags has TreeNodeFlags.AllowOverlapMode)
            setItemAllowOverlap()

        // Render
        val col = if (held && hovered) Col.HeaderActive else if (hovered) Col.HeaderHovered else Col.Header
        val textPos = bb.min + Vec2(textOffsetX, padding.y + textBaseOffsetY)
        if (displayFrame) {
            // Framed type
            renderFrame(bb.min, bb.max, col.u32, true, style.frameRounding)
            renderCollapseTriangle(bb.min + padding + Vec2(0f, textBaseOffsetY), isOpen, 1f)
            if (g.logEnabled) {
                /*  NB: '##' is normally used to hide text (as a library-wide feature), so we need to specify the text
                    range to make sure the ## aren't stripped out here.                 */
                logRenderedText(textPos, "\n##", 3)
                renderTextClipped(textPos, bb.max, label, label.length, labelSize)
                logRenderedText(textPos, "#", 3)
            } else
                renderTextClipped(textPos, bb.max, label, label.length, labelSize)
        } else {
            // Unframed typed for tree nodes
            if (hovered || flags has TreeNodeFlags.Selected)
                renderFrame(bb.min, bb.max, col.u32, false)

            if (flags has TreeNodeFlags.Bullet)
                TODO()//renderBullet(bb.Min + ImVec2(textOffsetX * 0.5f, g.FontSize * 0.50f + textBaseOffsetY))
            else if (flags hasnt TreeNodeFlags.Leaf)
                renderCollapseTriangle(bb.min + Vec2(padding.x, g.fontSize * 0.15f + textBaseOffsetY), isOpen, 0.7f)
            if (g.logEnabled)
                logRenderedText(textPos, ">")
            renderText(textPos, label, label.length, false)
        }

        if (isOpen && flags hasnt TreeNodeFlags.NoTreePushOnOpen)
            treePushRawId(id)
        return isOpen
    }

    /** Consume previous SetNextTreeNodeOpened() data, if any. May return true when logging */
    fun treeNodeBehaviorIsOpen(id: Int, flags: Int = 0): Boolean {

        if (flags has TreeNodeFlags.Leaf) return true

        // We only write to the tree storage if the user clicks (or explicitely use SetNextTreeNode*** functions)
        val window = g.currentWindow!!
        val storage = window.dc.stateStorage

        var isOpen = false
        if (g.setNextTreeNodeOpenCond != 0) {
            if (g.setNextTreeNodeOpenCond has Cond.Always) {
                isOpen = g.setNextTreeNodeOpenVal
                storage[id] = isOpen
            } else {
                /*  We treat ImGuiSetCondition_Once and ImGuiSetCondition_FirstUseEver the same because tree node state
                    are not saved persistently.                 */
                val storedValue = storage.int(id, -1)
                if (storedValue == -1) {
                    isOpen = g.setNextTreeNodeOpenVal
                    storage[id] = isOpen
                } else
                    isOpen = storedValue != 0
            }
            g.setNextTreeNodeOpenCond = 0
        } else
            isOpen = storage.int(id, if (flags has TreeNodeFlags.DefaultOpen) 1 else 0) != 0 // TODO rename back

        /*  When logging is enabled, we automatically expand tree nodes (but *NOT* collapsing headers.. seems like
            sensible behavior).
            NB- If we are above max depth we still allow manually opened nodes to be logged.    */
        if (g.logEnabled && flags hasnt TreeNodeFlags.NoAutoOpenOnLog && window.dc.treeDepth < g.logAutoExpandMaxDepth)
            isOpen = true

        return isOpen
    }

    fun treePushRawId(id: Int) {
        val window = currentWindow
        indent()
        window.dc.treeDepth++
        window.idStack.push(id)
    }

//IMGUI_API void          PlotEx(ImGuiPlotType plot_type, const char* label, float (*values_getter)(void* data, int idx), void* data, int values_count, int values_offset, const char* overlay_text, float scale_min, float scale_max, ImVec2 graph_size);


    /** Parse display precision back from the display format string */
    fun parseFormatPrecision(fmt: String, defaultPrecision: Int): Int {

        var precision = defaultPrecision
        if (fmt.contains('.')) {
            val s = fmt.substringAfter('.')
            if (s.isNotEmpty()) {
                precision = Character.getNumericValue(s[0])
                if (precision < 0 || precision > 10)
                    precision = defaultPrecision
            }
        }
        if(fmt.contains('e', ignoreCase = true))    // Maximum precision with scientific notation
            precision = -1
        return precision
    }

    fun roundScalar(value: Float, decimalPrecision: Int): Float {

        /*  Round past decimal precision
            So when our value is 1.99999 with a precision of 0.001 we'll end up rounding to 2.0
            FIXME: Investigate better rounding methods  */
        if (decimalPrecision < 0) return value
        val minStep = getMinimumStepAtDecimalPrecision(decimalPrecision)
        val negative = value < 0f
        var value = glm.abs(value)
        val remainder = value % minStep
        if (remainder <= minStep * 0.5f)
            value -= remainder
        else
            value += minStep - remainder
        return if (negative) -value else value
    }

    companion object {

        val colorSquareSize get() = g.fontSize + style.framePadding.y * 2f

        fun alphaBlendColor(colA: Int, colB: Int): Int {
            val t = ((colB ushr COL32_A_SHIFT) and 0xFF) / 255f
            val r = lerp((colA ushr COL32_R_SHIFT) and 0xFF, (colB ushr COL32_R_SHIFT) and 0xFF, t)
            val g = lerp((colA ushr COL32_G_SHIFT) and 0xFF, (colB ushr COL32_G_SHIFT) and 0xFF, t)
            val b = lerp((colA ushr COL32_B_SHIFT) and 0xFF, (colB ushr COL32_B_SHIFT) and 0xFF, t)
            return COL32(r, g, b, 0xFF)
        }

        fun getMinimumStepAtDecimalPrecision(decimalPrecision: Int): Float {
            val minSteps = floatArrayOf(1f, 0.1f, 0.01f, 0.001f, 0.0001f, 0.00001f, 0.000001f, 0.0000001f, 0.00000001f, 0.000000001f)
            return if (decimalPrecision in 0..9) minSteps[decimalPrecision] else glm.pow(10f, -decimalPrecision.f)
        }
    }
}