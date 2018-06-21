package imgui.imgui

import gli_.has
import gli_.hasnt
import glm_.*
import glm_.func.common.max
import glm_.func.common.min
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.F32_TO_INT8_SAT
import imgui.ImGui.begin
import imgui.ImGui.beginChildFrame
import imgui.ImGui.beginGroup
import imgui.ImGui.beginPopup
import imgui.ImGui.button
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.colorButton
import imgui.ImGui.contentRegionMax
import imgui.ImGui.endChildFrame
import imgui.ImGui.endGroup
import imgui.ImGui.endPopup
import imgui.ImGui.endTooltip
import imgui.ImGui.getColorU32
import imgui.ImGui.getColumnOffset
import imgui.ImGui.getColumnWidth
import imgui.ImGui.indent
import imgui.ImGui.io
import imgui.ImGui.isItemHovered
import imgui.ImGui.isMouseClicked
import imgui.ImGui.isMouseHoveringRect
import imgui.ImGui.logText
import imgui.ImGui.mouseCursor
import imgui.ImGui.openPopup
import imgui.ImGui.parseFormatFindStart
import imgui.ImGui.popClipRect
import imgui.ImGui.popFont
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushClipRect
import imgui.ImGui.pushFont
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.ImGui.radioButton
import imgui.ImGui.sameLine
import imgui.ImGui.scrollMaxY
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setClipboardText
import imgui.ImGui.setColumnOffset
import imgui.ImGui.setItemAllowOverlap
import imgui.ImGui.setTooltip
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textLineHeight
import imgui.ImGui.textUnformatted
import imgui.TextEditState.K
import imgui.imgui.imgui_colums.Companion.columnsRectHalfWidth
import imgui.imgui.imgui_main.Companion.dragBehaviorT
import imgui.internal.*
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.util.*
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KMutableProperty0
import glm_.pow as _
import imgui.ColorEditFlag as Cef
import imgui.DragDropFlag as Ddf
import imgui.HoveredFlag as Hf
import imgui.InputTextFlag as Itf
import imgui.ItemFlag as If
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf
import imgui.internal.ButtonFlag as Bf
import imgui.internal.ColumnsFlag as Cf
import imgui.internal.DrawCornerFlag as Dcf
import imgui.internal.LayoutType as Lt

@Suppress("UNCHECKED_CAST")


/** We should always have a CurrentWindow in the stack (there is an implicit "Debug" window)
 *  If this ever crash because g.CurrentWindow is NULL it means that either
 *      - ImGui::NewFrame() has never been called, which is illegal.
 *      - You are calling ImGui functions after ImGui::Render() and before the next ImGui::NewFrame(), which is also
 *          illegal.   */
interface imgui_internal {

    val currentWindowRead get() = g.currentWindow

    val currentWindow get() = g.currentWindow!!.apply { writeAccessed = true }

    fun findWindowByName(name: String) = g.windowsById[hash(name, 0)]

    fun markIniSettingsDirty() {
        if (g.settingsDirtyTimer <= 0f) g.settingsDirtyTimer = io.iniSavingRate
    }

    fun setActiveId(id: ID, window: Window?) {
        g.activeIdIsJustActivated = g.activeId != id
        if (g.activeIdIsJustActivated) g.activeIdTimer = 0f
        g.activeId = id
        g.activeIdAllowNavDirFlags = 0
        g.activeIdAllowOverlap = false
        g.activeIdWindow = window
        if (id != 0) {
            g.activeIdIsAlive = true
            g.activeIdSource = when (id) {
                g.navActivateId, g.navInputId, g.navJustTabbedId, g.navJustMovedToId -> InputSource.Nav
                else -> InputSource.Mouse
            }
        }
    }

    fun getActiveId() = g.activeId

    fun setFocusId(id: ID, window: Window) {

        assert(id != 0)

        /*  Assume that setFocusId() is called in the context where its ::navLayer is the current layer,
            which is the case everywhere we call it.         */
        val navLayer = window.dc.navLayerCurrent
        if (g.navWindow !== window)
            g.navInitRequest = false
        g.navId = id
        g.navWindow = window
        g.navLayer = navLayer
        window.navLastIds[navLayer] = id
        if (window.dc.lastItemId == id)
            window.navRectRel[navLayer].put(window.dc.lastItemRect.min - window.pos, window.dc.lastItemRect.max - window.pos)

        if (g.activeIdSource == InputSource.Nav)
            g.navDisableMouseHover = true
        else
            g.navDisableHighlight = true
    }

    fun clearActiveId() = setActiveId(0, null)

    // TODO custom setter/getters?
    fun setHoveredId(id: ID) {
        g.hoveredId = id
        g.hoveredIdAllowOverlap = false
        g.hoveredIdTimer = if (id != 0 && g.hoveredIdPreviousFrame == id) g.hoveredIdTimer + io.deltaTime else 0f
    }

    fun getHoveredId() = if (g.hoveredId != 0) g.hoveredId else g.hoveredIdPreviousFrame

    fun keepAliveId(id: ID) {
        if (g.activeId == id) g.activeIdIsAlive = true
    }

    /** Advance cursor given item size for layout.  */
    fun itemSize(size: Vec2, textOffsetY: Float = 0f) {

        val window = currentWindow
        if (window.skipItems) return

        // Always align ourselves on pixel boundaries
        val lineHeight = glm.max(window.dc.currentLineHeight, size.y)
        val textBaseOffset = glm.max(window.dc.currentLineTextBaseOffset, textOffsetY)
        //if (g.io.KeyAlt) window->DrawList->AddRect(window->DC.CursorPos, window->DC.CursorPos + ImVec2(size.x, line_height), IM_COL32(255,0,0,200)); // [DEBUG]
        window.dc.cursorPosPrevLine.put(window.dc.cursorPos.x + size.x, window.dc.cursorPos.y)
        window.dc.cursorPos.x = (window.pos.x + window.dc.indentX + window.dc.columnsOffsetX).i.f
        window.dc.cursorPos.y = (window.dc.cursorPos.y + lineHeight + style.itemSpacing.y).i.f
        window.dc.cursorMaxPos.x = glm.max(window.dc.cursorMaxPos.x, window.dc.cursorPosPrevLine.x)
        window.dc.cursorMaxPos.y = glm.max(window.dc.cursorMaxPos.y, window.dc.cursorPos.y - style.itemSpacing.y)

        //if (g.io.KeyAlt) window->DrawList->AddCircle(window->DC.CursorMaxPos, 3.0f, IM_COL32(255,0,0,255), 4); // [DEBUG]

        window.dc.prevLineHeight = lineHeight
        window.dc.prevLineTextBaseOffset = textBaseOffset
        window.dc.currentLineTextBaseOffset = 0f
        window.dc.currentLineHeight = 0f

        // Horizontal layout mode
        if (window.dc.layoutType == Lt.Horizontal) sameLine()
    }

    fun itemSize(bb: Rect, textOffsetY: Float = 0f) = itemSize(bb.size, textOffsetY)

    /** Declare item bounding box for clipping and interaction.
     *  Note that the size can be different than the one provided to ItemSize(). Typically, widgets that spread over
     *  available surface declare their minimum size requirement to ItemSize() and then use a larger region for
     *  drawing/interaction, which is passed to ItemAdd().  */
    fun itemAdd(bb: Rect, id: ID, navBbArg: Rect? = null): Boolean {

        val window = g.currentWindow!!
        if (id != 0) {
            /*  Navigation processing runs prior to clipping early-out
                (a) So that NavInitRequest can be honored, for newly opened windows to select a default widget
                (b) So that we can scroll up/down past clipped items. This adds a small O(N) cost to regular navigation
                    requests unfortunately, but it is still limited to one window.
                    it may not scale very well for windows with ten of thousands of item, but at least navMoveRequest
                    is only set on user interaction, aka maximum once a frame.
                    We could early out with "if (isClipped && !g.navInitRequest) return false" but when we wouldn't be
                    able to reach unclipped widgets. This would work if user had explicit scrolling control (e.g. mapped on a stick)    */
            window.dc.navLayerActiveMaskNext = window.dc.navLayerActiveMaskNext or window.dc.navLayerCurrentMask
            if (g.navId == id || g.navAnyRequest)
                if (g.navWindow!!.rootWindowForNav === window.rootWindowForNav)
                    if (window == g.navWindow || (window.flags or g.navWindow!!.flags) has Wf.NavFlattened)
                        navProcessItem(window, navBbArg ?: bb, id)
        }
        val dc = g.currentWindow!!.dc.apply {
            lastItemId = id
            lastItemRect = bb
            lastItemStatusFlags = 0
        }

        // Clipping test
        if (isClippedEx(bb, id, false)) return false
        //if (g.io.KeyAlt) window->DrawList->AddRect(bb.Min, bb.Max, IM_COL32(255,255,0,120)); // [DEBUG]

        /*  We need to calculate this now to take account of the current clipping rectangle (as items like Selectable
            may change them)         */
        if (isMouseHoveringRect(bb.min, bb.max))
            dc.lastItemStatusFlags = dc.lastItemStatusFlags or ItemStatusFlag.HoveredRect
        return true
    }

    /** Internal facing ItemHoverable() used when submitting widgets. Differs slightly from IsItemHovered().    */
    fun itemHoverable(bb: Rect, id: ID): Boolean {
        val window = g.currentWindow!!
        return when {
            g.hoveredId != 0 && g.hoveredId != id && !g.hoveredIdAllowOverlap -> false
            g.hoveredWindow !== window -> false
            g.activeId != 0 && g.activeId != id && !g.activeIdAllowOverlap -> false
            !isMouseHoveringRect(bb) -> false
            g.navDisableMouseHover || !window.isContentHoverable(Hf.Default.i) -> false
            window.dc.itemFlags has If.Disabled -> false
            else -> {
                setHoveredId(id)
                true
            }
        }
    }

    fun isClippedEx(bb: Rect, id: ID, clipEvenWhenLogged: Boolean): Boolean {

        val window = g.currentWindow!!
        if (!(bb overlaps window.clipRect))
            if (id == 0 || id != g.activeId)
                if (clipEvenWhenLogged || !g.logEnabled)
                    return true
        return false
    }

    /** Return true if focus is requested   */
    fun focusableItemRegister(window: Window, id: ID, tabStop: Boolean = true): Boolean {

        val allowKeyboardFocus = (window.dc.itemFlags and (If.AllowKeyboardFocus or If.Disabled)) == If.AllowKeyboardFocus.i
        window.focusIdxAllCounter++
        if (allowKeyboardFocus)
            window.focusIdxTabCounter++

        /*  Process keyboard input at this point: TAB/Shift-TAB to tab out of the currently focused item.
            Note that we can always TAB out of a widget that doesn't allow tabbing in.         */
        if (tabStop && g.activeId == id && window.focusIdxAllRequestNext == Int.MAX_VALUE &&
                window.focusIdxTabRequestNext == Int.MAX_VALUE && !io.keyCtrl && Key.Tab.isPressed)
        // Modulo on index will be applied at the end of frame once we've got the total counter of items.
            window.focusIdxTabRequestNext = window.focusIdxTabCounter + if (io.keyShift) if (allowKeyboardFocus) -1 else 0 else 1

        if (window.focusIdxAllCounter == window.focusIdxAllRequestCurrent) return true

        if (allowKeyboardFocus && window.focusIdxTabCounter == window.focusIdxTabRequestCurrent) {
            g.navJustTabbedId = id
            return true
        }
        return false
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

    fun calcWrapWidthForPos(pos: Vec2, wrapPosX_: Float): Float {

        if (wrapPosX_ < 0f) return 0f

        val window = currentWindowRead!!
        var wrapPosX = wrapPosX_
        if (wrapPosX == 0f)
            wrapPosX = contentRegionMax.x + window.pos.x
        else if (wrapPosX > 0f)
            wrapPosX += window.pos.x - window.scroll.x // wrap_pos_x is provided is window local space

        return glm.max(wrapPosX - pos.x, 1f)
    }

    fun pushMultiItemsWidths(components: Int, wFull_: Float = 0f) {

        val window = ImGui.currentWindow
        val wFull = if (wFull_ <= 0f) calcItemWidth() else wFull_
        val wItemOne = glm.max(1f, ((wFull - (style.itemInnerSpacing.x) * (components - 1)) / components.f).i.f)
        val wItemLast = glm.max(1f, (wFull - (wItemOne + style.itemInnerSpacing.x) * (components - 1)).i.f)
        window.dc.itemWidthStack.push(wItemLast)
        for (i in 0 until components - 1)
            window.dc.itemWidthStack.push(wItemOne)
        window.dc.itemWidth = window.dc.itemWidthStack.last()
    }

    /** allow focusing using TAB/Shift-TAB, enabled by default but you can disable it for certain widgets
     *  @param option = ItemFlag   */
    fun pushItemFlag(option: ItemFlags, enabled: Boolean) = with(ImGui.currentWindow.dc) {
        if (enabled)
            itemFlags = itemFlags or option
        else
            itemFlags = itemFlags wo option
        itemFlagsStack.add(itemFlags)
    }

    fun popItemFlag() = with(ImGui.currentWindow.dc) {
        itemFlagsStack.pop()
        itemFlags = itemFlagsStack.lastOrNull() ?: If.Default_.i
    }

    fun setCurrentFont(font: Font) {
        assert(font.isLoaded) { "Font Atlas not created. Did you call io.Fonts->GetTexDataAsRGBA32 / GetTexDataAsAlpha8 ?" }
        assert(font.scale > 0f)
        g.font = font
        g.fontBaseSize = io.fontGlobalScale * g.font.fontSize * g.font.scale
        g.fontSize = g.currentWindow?.calcFontSize() ?: 0f

        val atlas = g.font.containerAtlas
        g.drawListSharedData.texUvWhitePixel = atlas.texUvWhitePixel
        g.drawListSharedData.font = g.font
        g.drawListSharedData.fontSize = g.fontSize
    }

    /** Mark popup as open (toggle toward open state).
     *  Popups are closed when user click outside, or activate a pressable item, or CloseCurrentPopup() is called within
     *  a BeginPopup()/EndPopup() block.
     *  Popup identifiers are relative to the current ID-stack (so OpenPopup and BeginPopup needs to be at the same
     *  level).
     *  One open popup per level of the popup hierarchy (NB: when assigning we reset the Window member of ImGuiPopupRef
     *  to NULL)    */
    fun openPopupEx(id: ID) {

        val parentWindow = g.currentWindow!!
        val currentStackSize = g.currentPopupStack.size
        // Tagged as new ref as Window will be set back to NULL if we write this into OpenPopupStack.
        val popupRef = PopupRef(popupId = id, window = null, parentWindow = parentWindow, openFrameCount = g.frameCount,
                openParentId = parentWindow.idStack.last(), openMousePos = Vec2(io.mousePos), openPopupPos = navCalcPreferredRefPos())
//        println("" + g.openPopupStack.size +", "+currentStackSize)
        if (g.openPopupStack.size < currentStackSize + 1)
            g.openPopupStack += popupRef
        else {
            // Close child popups if any
            if (g.openPopupStack.size > currentStackSize + 1)
                for (i in currentStackSize + 1 until g.openPopupStack.size)
                    g.openPopupStack.pop()
            else if (g.openPopupStack.size < currentStackSize + 1)
                TODO()

            /*  Gently handle the user mistakenly calling OpenPopup() every frame. It is a programming mistake!
                However, if we were to run the regular code path, the ui would become completely unusable because
                the popup will always be in hidden-while-calculating-size state _while_ claiming focus.
                Which would be a very confusing situation for the programmer. Instead, we silently allow the popup
                to proceed, it will keep reappearing and the programming error will be more obvious to understand.  */
            if (g.openPopupStack[currentStackSize].popupId == id && g.openPopupStack[currentStackSize].openFrameCount == g.frameCount - 1)
                g.openPopupStack[currentStackSize].openFrameCount = popupRef.openFrameCount
            else
                g.openPopupStack[currentStackSize] = popupRef

            /*  When reopening a popup we first refocus its parent, otherwise if its parent is itself a popup
                it would get closed by closePopupsOverWindow().  This is equivalent to what ClosePopupToLevel() does. */
            //if (g.openPopupStack[currentStackSize].popupId == id) parentWindow.focus()
        }
    }

    fun closePopup(id: ID) {
        if (!isPopupOpen(id)) return
        closePopupToLevel(g.openPopupStack.lastIndex)
    }

    fun closePopupsOverWindow(refWindow: Window?) {

        if (g.openPopupStack.empty())
            return

        /*  When popups are stacked, clicking on a lower level popups puts focus back to it and close popups above it.
            Don't close our own child popup windows */
        var n = 0
        if (refWindow != null)
            while (n < g.openPopupStack.size) {
                val popup = g.openPopupStack[n]
                if (popup.window == null) {
                    n++
                    continue
                }
                assert(popup.window!!.flags has Wf.Popup)
                if (popup.window!!.flags has Wf.ChildWindow) {
                    n++
                    continue
                }
                // Trim the stack if popups are not direct descendant of the reference window (which is often the NavWindow)
                var hasFocus = false
                var m = n
                while (m < g.openPopupStack.size && !hasFocus) {
                    hasFocus = g.openPopupStack[m].window != null && g.openPopupStack[m].window!!.rootWindow === refWindow.rootWindow
                    m++
                }
                if (!hasFocus) break
                n++
            }

        if (n < g.openPopupStack.size)   // This test is not required but it allows to set a convenient breakpoint on the block below
            closePopupToLevel(n)
    }

    // FIXME
    /** return true if the popup is open    */
    fun isPopupOpen(id: ID) = g.openPopupStack.size > g.currentPopupStack.size && g.openPopupStack[g.currentPopupStack.size].popupId == id

    fun beginPopupEx(id: ID, extraFlags: WindowFlags): Boolean {

        if (!isPopupOpen(id)) {
            g.nextWindowData.clear() // We behave like Begin() and need to consume those values
            return false
        }

        val name =
                if (extraFlags has Wf.ChildMenu)
                    "##Menu_%02d".format(style.locale, g.currentPopupStack.size)    // Recycle windows based on depth
                else
                    "##Popup_%08x".format(style.locale, id)     // Not recycling, so we can close/open during the same frame

        val isOpen = begin(name, null, extraFlags or Wf.Popup)
        if (!isOpen) // NB: Begin can return false when the popup is completely clipped (e.g. zero size display)
            endPopup()

        return isOpen
    }

    /** Not exposed publicly as BeginTooltip() because bool parameters are evil. Let's see if other needs arise first.
     *  @param extraFlags WindowFlag   */
    fun beginTooltipEx(extraFlags: WindowFlags, overridePreviousTooltip: Boolean = true) {

        var windowName = "##Tooltip_%02d".format(style.locale, g.tooltipOverrideCount)
        if (overridePreviousTooltip)
            findWindowByName(windowName)?.let {
                if (it.active) {
                    // Hide previous tooltip from being displayed. We can't easily "reset" the content of a window so we create a new one.
                    it.hiddenFrames = 1
                    windowName = "##Tooltip_%02d".format(++g.tooltipOverrideCount)
                }
            }
        val flags = Wf.Tooltip or Wf.NoInputs or Wf.NoTitleBar or Wf.NoMove or Wf.NoResize or Wf.NoSavedSettings or
                Wf.AlwaysAutoResize or Wf.NoNav
        begin(windowName, null, flags or extraFlags)
    }

    val frontMostPopupModal: Window?
        get() {
            for (n in g.openPopupStack.size - 1 downTo 0)
                g.openPopupStack[n].window?.let { if (it.flags has Wf.Modal) return it }
            return null
        }

    fun navInitWindow(window: Window, forceReinit: Boolean) {

        assert(window == g.navWindow)
        var initForNav = false
        if (window.flags hasnt Wf.NoNavInputs)
            if (window.flags hasnt Wf.ChildWindow || window.flags has Wf.Popup || window.navLastIds[0] == 0 || forceReinit)
                initForNav = true
        if (initForNav) {
            setNavId(0, g.navLayer)
            g.navInitRequest = true
            g.navInitRequestFromMove = false
            g.navInitResultId = 0
            g.navInitResultRectRel = Rect()
            navUpdateAnyRequestFlag()
        } else
            g.navId = window.navLastIds[0]
    }

    fun navMoveRequestCancel() {
        g.navMoveRequest = false
        navUpdateAnyRequestFlag()
    }

    /** Remotely activate a button, checkbox, tree node etc. given its unique ID. activation is queued and processed
     *  on the next frame when the item is encountered again.  */
    fun activateItem(id: ID) {
        g.navNextActivateId = id
    }

    fun getNavInputAmount(n: NavInput, mode: InputReadMode): Float {    // TODO -> NavInput?

        val i = n.i
        if (mode == InputReadMode.Down) return io.navInputs[i] // Instant, read analog input (0.0f..1.0f, as provided by user)

        val t = io.navInputsDownDuration[i]
        return when {
        // Return 1.0f when just released, no repeat, ignore analog input.
            t < 0f && mode == InputReadMode.Released -> if (io.navInputsDownDurationPrev[i] >= 0f) 1f else 0f
            t < 0f -> 0f
            else -> when (mode) {
            // Return 1.0f when just pressed, no repeat, ignore analog input.
                InputReadMode.Pressed -> if (t == 0f) 1 else 0
                InputReadMode.Repeat -> calcTypematicPressedRepeatAmount(t, t - io.deltaTime, io.keyRepeatDelay * 0.8f,
                        io.keyRepeatRate * 0.8f)
                InputReadMode.RepeatSlow -> calcTypematicPressedRepeatAmount(t, t - io.deltaTime, io.keyRepeatDelay * 1f, io.keyRepeatRate * 2f)
                InputReadMode.RepeatFast -> calcTypematicPressedRepeatAmount(t, t - io.deltaTime, io.keyRepeatDelay * 0.8f, io.keyRepeatRate * 0.3f)
                else -> 0
            }.f
        }
    }

    /** @param dirSources: NavDirSourceFlag    */
    fun getNavInputAmount2d(dirSources: NavDirSourceFlags, mode: InputReadMode, slowFactor: Float = 0f, fastFactor: Float = 0f): Vec2 {
        val delta = Vec2()
        if (dirSources has NavDirSourceFlag.Keyboard)
            delta += Vec2(getNavInputAmount(NavInput.KeyRight, mode) - getNavInputAmount(NavInput.KeyLeft, mode),
                    getNavInputAmount(NavInput.KeyDown, mode) - getNavInputAmount(NavInput.KeyUp, mode))
        if (dirSources has NavDirSourceFlag.PadDPad)
            delta += Vec2(getNavInputAmount(NavInput.DpadRight, mode) - getNavInputAmount(NavInput.DpadLeft, mode),
                    getNavInputAmount(NavInput.DpadDown, mode) - getNavInputAmount(NavInput.DpadUp, mode))
        if (dirSources has NavDirSourceFlag.PadLStick)
            delta += Vec2(getNavInputAmount(NavInput.LStickRight, mode) - getNavInputAmount(NavInput.LStickLeft, mode),
                    getNavInputAmount(NavInput.LStickDown, mode) - getNavInputAmount(NavInput.LStickUp, mode))
        if (slowFactor != 0f && NavInput.TweakSlow.isDown())
            delta *= slowFactor
        if (fastFactor != 0f && NavInput.TweakFast.isDown())
            delta *= fastFactor
        return delta
    }


    fun calcTypematicPressedRepeatAmount(t: Float, tPrev: Float, repeatDelay: Float, repeatRate: Float) = when {
        t == 0f -> 1
        t <= repeatDelay || repeatRate <= 0f -> 0
        else -> {
            val count = ((t - repeatDelay) / repeatRate).i - ((tPrev - repeatDelay) / repeatRate).i
            if (count > 0) count else 0
        }
    }

    /** Vertical scrollbar
     *  The entire piece of code below is rather confusing because:
     *  - We handle absolute seeking (when first clicking outside the grab) and relative manipulation (afterward or when
     *          clicking inside the grab)
     *  - We store values as normalized ratio and in a form that allows the window content to change while we are holding on
     *          a scrollbar
     *  - We handle both horizontal and vertical scrollbars, which makes the terminology not ideal. */
    fun scrollbar(direction: Lt) {

        val window = g.currentWindow!!
        val horizontal = direction == Lt.Horizontal

        val id = window.getId(if (horizontal) "#SCROLLX" else "#SCROLLY")

        // Render background
        val otherScrollbar = if (horizontal) window.scrollbar.y else window.scrollbar.x
        val otherScrollbarSizeW = if (otherScrollbar) style.scrollbarSize else 0f
        val windowRect = window.rect()
        val borderSize = window.windowBorderSize
        val bb = when (horizontal) {
            true -> Rect(window.pos.x + borderSize, windowRect.max.y - style.scrollbarSize,
                    windowRect.max.x - otherScrollbarSizeW - borderSize, windowRect.max.y - borderSize)
            else -> Rect(windowRect.max.x - style.scrollbarSize, window.pos.y + borderSize,
                    windowRect.max.x - borderSize, windowRect.max.y - otherScrollbarSizeW - borderSize)
        }
        if (!horizontal)
            bb.min.y += window.titleBarHeight + if (window.flags has Wf.MenuBar) window.menuBarHeight else 0f
        if (bb.width <= 0f || bb.height <= 0f) return

        val windowRoundingCorners =
                if (horizontal)
                    Dcf.BotLeft.i or if (otherScrollbar) 0 else Dcf.BotRight.i
                else
                    (if (window.flags has Wf.NoTitleBar && window.flags hasnt Wf.MenuBar) Dcf.TopRight.i else 0) or
                            if (otherScrollbar) 0 else Dcf.BotRight.i
        window.drawList.addRectFilled(bb.min, bb.max, Col.ScrollbarBg.u32, window.windowRounding, windowRoundingCorners)
        bb.expand(Vec2(
                -glm.clamp(((bb.max.x - bb.min.x - 2f) * 0.5f).i.f, 0f, 3f),
                -glm.clamp(((bb.max.y - bb.min.y - 2f) * 0.5f).i.f, 0f, 3f)))

        // V denote the main, longer axis of the scrollbar (= height for a vertical scrollbar)
        val scrollbarSizeV = if (horizontal) bb.width else bb.height
        var scrollV = if (horizontal) window.scroll.x else window.scroll.y
        val winSizeAvailV = (if (horizontal) window.sizeFull.x else window.sizeFull.y) - otherScrollbarSizeW
        val winSizeContentsV = if (horizontal) window.sizeContents.x else window.sizeContents.y

        /*  Calculate the height of our grabbable box. It generally represent the amount visible (vs the total scrollable amount)
            But we maintain a minimum size in pixel to allow for the user to still aim inside.  */
        // Adding this assert to check if the ImMax(XXX,1.0f) is still needed. PLEASE CONTACT ME if this triggers.
        assert(glm.max(winSizeContentsV, winSizeAvailV) > 0f)
        val winSizeV = glm.max(glm.max(winSizeContentsV, winSizeAvailV), 1f)
        val grabHPixels = glm.clamp(scrollbarSizeV * (winSizeAvailV / winSizeV), style.grabMinSize, scrollbarSizeV)
        val grabHNorm = grabHPixels / scrollbarSizeV

        // Handle input right away. None of the code of Begin() is relying on scrolling position before calling Scrollbar().
        val previouslyHeld = g.activeId == id
        val (_, hovered, held) = buttonBehavior(bb, id, Bf.NoNavFocus)

        val scrollMax = glm.max(1f, winSizeContentsV - winSizeAvailV)
        var scrollRatio = saturate(scrollV / scrollMax)
        var grabVNorm = scrollRatio * (scrollbarSizeV - grabHPixels) / scrollbarSizeV
        if (held && grabHNorm < 1f) {
            val scrollbarPosV = if (horizontal) bb.min.x else bb.min.y
            val mousePosV = if (horizontal) io.mousePos.x else io.mousePos.y
            var clickDeltaToGrabCenterV = if (horizontal) g.scrollbarClickDeltaToGrabCenter.x else g.scrollbarClickDeltaToGrabCenter.y

            // Click position in scrollbar normalized space (0.0f->1.0f)
            val clickedVNorm = saturate((mousePosV - scrollbarPosV) / scrollbarSizeV)
            setHoveredId(id)

            var seekAbsolute = false
            if (!previouslyHeld)
            // On initial click calculate the distance between mouse and the center of the grab
                if (clickedVNorm >= grabVNorm && clickedVNorm <= grabVNorm + grabHNorm)
                    clickDeltaToGrabCenterV = clickedVNorm - grabVNorm - grabHNorm * 0.5f
                else {
                    seekAbsolute = true
                    clickDeltaToGrabCenterV = 0f
                }

            /*  Apply scroll
                It is ok to modify Scroll here because we are being called in Begin() after the calculation of SizeContents
                and before setting up our starting position */
            val scrollVNorm = saturate((clickedVNorm - clickDeltaToGrabCenterV - grabHNorm * 0.5f) / (1f - grabHNorm))
            scrollV = (0.5f + scrollVNorm * scrollMax).i.f  //(winSizeContentsV - winSizeV));
            if (horizontal)
                window.scroll.x = scrollV
            else
                window.scroll.y = scrollV

            // Update values for rendering
            scrollRatio = saturate(scrollV / scrollMax)
            grabVNorm = scrollRatio * (scrollbarSizeV - grabHPixels) / scrollbarSizeV

            // Update distance to grab now that we have seeked and saturated
            if (seekAbsolute)
                clickDeltaToGrabCenterV = clickedVNorm - grabVNorm - grabHNorm * 0.5f

            if (horizontal)
                g.scrollbarClickDeltaToGrabCenter.x = clickDeltaToGrabCenterV
            else
                g.scrollbarClickDeltaToGrabCenter.y = clickDeltaToGrabCenterV
        }

        // Render
        val grabCol = (if (held) Col.ScrollbarGrabActive else if (hovered) Col.ScrollbarGrabHovered else Col.ScrollbarGrab).u32
        val grabRect =
                if (horizontal)
                    Rect(lerp(bb.min.x, bb.max.x, grabVNorm), bb.min.y,
                            min(lerp(bb.min.x, bb.max.x, grabVNorm) + grabHPixels, windowRect.max.x), bb.max.y)
                else
                    Rect(bb.min.x, lerp(bb.min.y, bb.max.y, grabVNorm),
                            bb.max.x, min(lerp(bb.min.y, bb.max.y, grabVNorm) + grabHPixels, windowRect.max.y))
        window.drawList.addRectFilled(grabRect.min, grabRect.max, grabCol, style.scrollbarRounding)
    }

    /** Vertical separator, for menu bars (use current line height). not exposed because it is misleading
     *  what it doesn't have an effect on regular layout.   */
    fun verticalSeparator() {
        val window = currentWindow
        if (window.skipItems) return

        val y1 = window.dc.cursorPos.y
        val y2 = window.dc.cursorPos.y + window.dc.currentLineHeight
        val bb = Rect(Vec2(window.dc.cursorPos.x, y1), Vec2(window.dc.cursorPos.x + 1f, y2))
        itemSize(Vec2(bb.width, 0f))
        if (!itemAdd(bb, 0)) return

        window.drawList.addLine(Vec2(bb.min), Vec2(bb.min.x, bb.max.y), Col.Separator.u32)
        if (g.logEnabled) logText(" |")
    }

    fun splitterBehavior(id: ID, bb: Rect, axis: Axis, size1: KMutableProperty0<Float>, size2: KMutableProperty0<Float>,
                         minSize1: Float, minSize2: Float, hoverExtend: Float): Boolean {
        val window = g.currentWindow!!

        val itemFlagsBackup = window.dc.itemFlags

        window.dc.itemFlags = window.dc.itemFlags or (If.NoNav or If.NoNavDefaultFocus)

        val itemAdd = itemAdd(bb, id)
        window.dc.itemFlags = itemFlagsBackup
        if (!itemAdd) return false

        val bbInteract = Rect(bb)
        bbInteract expand if (axis == Axis.Y) Vec2(0f, hoverExtend) else Vec2(hoverExtend, 0f)
        val (_, hovered, held) = buttonBehavior(bbInteract, id, Bf.FlattenChildren or Bf.AllowItemOverlap)
        if (g.activeId != id) setItemAllowOverlap()

        if (held || (g.hoveredId == id && g.hoveredIdPreviousFrame == id))
            mouseCursor = if (axis == Axis.Y) MouseCursor.ResizeNS else MouseCursor.ResizeEW

        val bbRender = Rect(bb)
        if (held) {
            val mouseDelta2d = io.mousePos - g.activeIdClickOffset - bbInteract.min
            var mouseDelta = if (axis == Axis.Y) mouseDelta2d.y else mouseDelta2d.x

            // Minimum pane size
            if (mouseDelta < minSize1 - size1())
                mouseDelta = minSize1 - size1()
            if (mouseDelta > size2() - minSize2)
                mouseDelta = size2() - minSize2

            // Apply resize
            size1.set(size1() + mouseDelta)
            size2.set(size2() - mouseDelta)
            bbRender translate if (axis == Axis.X) Vec2(mouseDelta, 0f) else Vec2(0f, mouseDelta)
        }

        // Render
        val col = if (held) Col.SeparatorActive else if (hovered) Col.SeparatorHovered else Col.Separator
        window.drawList.addRectFilled(bbRender.min, bbRender.max, col.u32, style.frameRounding)

        return held
    }


    fun beginDragDropTargetCustom(bb: Rect, id: ID): Boolean {
        if (!g.dragDropActive) return false

        val window = g.currentWindow!!
        g.hoveredWindow.let { if (it == null || window.rootWindow != it.rootWindow) return false }
        assert(id != 0)
        if (!isMouseHoveringRect(bb.min, bb.max) || id == g.dragDropPayload.sourceId)
            return false

        g.dragDropTargetRect put bb
        g.dragDropTargetId = id
        return true
    }

    fun clearDragDrop() = with(g) {
        dragDropActive = false
        dragDropPayload.clear()
        dragDropAcceptIdPrev = 0
        dragDropAcceptIdCurr = 0
        dragDropAcceptIdCurrRectSurface = Float.MAX_VALUE
        dragDropAcceptFrameCount = -1
    }

    val isDragDropPayloadBeingAccepted get() = g.dragDropActive && g.dragDropAcceptIdPrev != 0


    // FIXME-WIP: New Columns API

    /** setup number of columns. use an identifier to distinguish multiple column sets. close with EndColumns().    */
    fun beginColumns(strId: String = "", columnsCount: Int, flags: ColumnsFlags) {

        with(currentWindow) {

            assert(columnsCount > 1)
            assert(dc.columnsSet == null) { "Nested columns are currently not supported" }

            /*  Differentiate column ID with an arbitrary prefix for cases where users name their columns set the same
                as another widget.
                In addition, when an identifier isn't explicitly provided we include the number of columns in the hash
                to make it uniquer. */
            pushId(0x11223347 + if (strId.isNotEmpty()) 0 else columnsCount)
            val id = getId(if (strId.isEmpty()) "columns" else strId)
            popId()

            // Acquire storage for the columns set
            val columns = findOrAddColumnsSet(id)
            assert(columns.id == id)
            with(columns) {
                current = 0
                count = columnsCount
                this.flags = flags
            }
            dc.columnsSet = columns

            // Set state for first column
            val contentRegionWidth = if (sizeContentsExplicit.x != 0f) sizeContentsExplicit.x else innerClipRect.max.x - pos.x
            with(columns) {
                minX = dc.indentX - style.itemSpacing.x // Lock our horizontal range
                maxX = max(contentRegionWidth - scroll.x, minX + 1f)
                startPosY = dc.cursorPos.y
                startMaxPosX = dc.cursorMaxPos.x
                lineMaxY = dc.cursorPos.y
                lineMinY = dc.cursorPos.y
            }
            dc.columnsOffsetX = 0f
            dc.cursorPos.x = (pos.x + dc.indentX + dc.columnsOffsetX).i.f

            // Clear data if columns count changed
            if (columns.columns.isNotEmpty() && columns.columns.size != columnsCount + 1)
                columns.columns.clear()

            // Initialize defaults
            columns.isFirstFrame = columns.columns.isEmpty()
            if (columns.columns.isEmpty())
                for (n in 0..columnsCount)
                    columns.columns += ColumnData().apply { offsetNorm = n / columnsCount.f }

            for (n in 0 until columnsCount) {

                // Compute clipping rectangle
                val column = columns.columns[n]
                val clipX1 = floor(0.5f + pos.x + getColumnOffset(n) - 1f)
                val clipX2 = floor(0.5f + pos.x + getColumnOffset(n + 1) - 1f)
                column.clipRect = Rect(clipX1, -Float.MAX_VALUE, clipX2, +Float.MAX_VALUE)
                column.clipRect clipWith clipRect
            }
            drawList.channelsSplit(columns.count)
            pushColumnClipRect()
            pushItemWidth(getColumnWidth() * 0.65f)
        }
    }

    fun endColumns() = with(currentWindow) {

        val columns = dc.columnsSet!!

        popItemWidth()
        popClipRect()
        drawList.channelsMerge()

        columns.lineMaxY = glm.max(columns.lineMaxY, dc.cursorPos.y)
        dc.cursorPos.y = columns.lineMaxY
        if (columns.flags hasnt Cf.GrowParentContentsSize)
            dc.cursorMaxPos.x = columns.startMaxPosX // Restore cursor max pos, as columns don't grow parent

        // Draw columns borders and handle resize
        var isBeingResized = false
        if (columns.flags hasnt Cf.NoBorder && !skipItems) {

            val y1 = columns.startPosY
            val y2 = dc.cursorPos.y
            var draggingColumn = -1
            for (n in 1 until columns.count) {

                val x = pos.x + getColumnOffset(n)
                val columnId: ID = columns.id + n
                val columnHw = columnsRectHalfWidth // Half-width for interaction
                val columnRect = Rect(x - columnHw, y1, x + columnHw, y2)
                keepAliveId(columnId)
                if (isClippedEx(columnRect, columnId, false)) continue

                var hovered = false
                var held = false
                if (columns.flags hasnt Cf.NoResize) {

                    val (_, b, c) = buttonBehavior(columnRect, columnId)
                    hovered = b
                    held = c
                    if (hovered || held)
                        g.mouseCursor = MouseCursor.ResizeEW
                    if (held && columns.columns[n].flags hasnt Cf.NoResize)
                        draggingColumn = n
                }

                // Draw column (we clip the Y boundaries CPU side because very long triangles are mishandled by some GPU drivers.)
                val col = getColorU32(if (held) Col.SeparatorActive else if (hovered) Col.SeparatorHovered else Col.Separator)
                val xi = x.i.f
                drawList.addLine(Vec2(xi, max(y1 + 1f, clipRect.min.y)), Vec2(xi, min(y2, clipRect.max.y)), col)
            }

            // Apply dragging after drawing the column lines, so our rendered lines are in sync with how items were displayed during the frame.
            if (draggingColumn != -1) {
                if (!columns.isBeingResized)
                    for (n in 0..columns.count)
                        columns.columns[n].offsetNormBeforeResize = columns.columns[n].offsetNorm
                isBeingResized = true
                columns.isBeingResized = true
                val x = getDraggedColumnOffset(columns, draggingColumn)
                setColumnOffset(draggingColumn, getDraggedColumnOffset(columns, draggingColumn))
            }
        }
        columns.isBeingResized = isBeingResized

        dc.columnsSet = null
        dc.columnsOffsetX = 0f
        dc.cursorPos.x = (pos.x + dc.indentX + dc.columnsOffsetX).i.f
    }

    fun pushColumnClipRect(columnIndex_: Int = -1) {

        val window = currentWindowRead!!
        val columns = window.dc.columnsSet!!
        val columnIndex = if (columnIndex_ < 0) columns.current else columnIndex_

        pushClipRect(columns.columns[columnIndex].clipRect.min, columns.columns[columnIndex].clipRect.max, false)
    }


    /** NB: All position are in absolute pixels coordinates (never using window coordinates internally)
     *  AVOID USING OUTSIDE OF IMGUI.CPP! NOT FOR PUBLIC CONSUMPTION. THOSE FUNCTIONS ARE A MESS. THEIR SIGNATURE AND
     *  BEHAVIOR WILL CHANGE, THEY NEED TO BE REFACTORED INTO SOMETHING DECENT. */
    fun renderText(pos: Vec2, text: String, textEnd: Int = text.length, hideTextAfterHash: Boolean = true) {

        val window = g.currentWindow!!

        // Hide anything after a '##' string
        val textDisplayEnd = when {
            hideTextAfterHash -> findRenderedTextEnd(text, textEnd)
            textEnd == 0 -> text.length
            else -> textEnd
        }

        if (textDisplayEnd > 0) {
            window.drawList.addText(g.font, g.fontSize, pos, Col.Text.u32, text.toCharArray(), textDisplayEnd)
            if (g.logEnabled)
                logRenderedText(pos, text, textDisplayEnd)
        }
    }

    fun renderTextWrapped(pos: Vec2, text: String, textEnd_: Int, wrapWidth: Float) {

        val window = g.currentWindow!!

        var textEnd = textEnd_
        if (textEnd == 0)
            textEnd = text.length // FIXME-OPT

        if (textEnd > 0) {
            window.drawList.addText(g.font, g.fontSize, pos, Col.Text.u32, text.toCharArray(), textEnd, wrapWidth)
            if (g.logEnabled) logRenderedText(pos, text, textEnd)
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

        val window = g.currentWindow!!

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

        val window = g.currentWindow!!

        window.drawList.addRectFilled(pMin, pMax, fillCol, rounding)
        val borderSize = style.frameBorderSize
        if (border && borderSize > 0f) {
            window.drawList.addRect(pMin + 1, pMax + 1, Col.BorderShadow.u32, rounding, Dcf.All.i, borderSize)
            window.drawList.addRect(pMin, pMax, Col.Border.u32, rounding, 0.inv(), borderSize)
        }
    }

    fun renderFrameBorder(pMin: Vec2, pMax: Vec2, rounding: Float = 0f) = with(g.currentWindow!!) {
        val borderSize = style.frameBorderSize
        if (borderSize > 0f) {
            drawList.addRect(pMin + 1, pMax + 1, Col.BorderShadow.u32, rounding, Dcf.All.i, borderSize)
            drawList.addRect(pMin, pMax, Col.Border.u32, rounding, 0.inv(), borderSize)
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
                            if (x1 <= pMin.x) roundingCornersFlagsCell = roundingCornersFlagsCell or Dcf.TopLeft
                            if (x2 >= pMax.x) roundingCornersFlagsCell = roundingCornersFlagsCell or Dcf.TopRight
                        }
                        if (y2 >= pMax.y) {
                            if (x1 <= pMin.x) roundingCornersFlagsCell = roundingCornersFlagsCell or Dcf.BotLeft
                            if (x2 >= pMax.x) roundingCornersFlagsCell = roundingCornersFlagsCell or Dcf.BotRight
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
    fun renderArrow(pMin: Vec2, dir: Dir, scale: Float = 1.0f) {

        val window = g.currentWindow!!

        val h = g.fontSize * 1f
        var r = h * 0.4f * scale
        val center = pMin + Vec2(h * 0.5f, h * 0.5f * scale)

        val a: Vec2
        val b: Vec2
        val c: Vec2
        when (dir) {
            Dir.Up, Dir.Down -> {
                if (dir == Dir.Up) r = -r
                center.y -= r * 0.25f
                a = Vec2(0, 1) * r
                b = Vec2(-0.866f, -0.5f) * r
                c = Vec2(+0.866f, -0.5f) * r
            }
            Dir.Left, Dir.Right -> {
                if (dir == Dir.Left) r = -r
                center.x -= r * 0.25f
                a = Vec2(1, 0) * r
                b = Vec2(-0.500f, +0.866f) * r
                c = Vec2(-0.500f, -0.866f) * r
            }
            else -> throw Error()
        }

        window.drawList.addTriangleFilled(center + a, center + b, center + c, Col.Text.u32)
    }

    fun renderBullet(pos: Vec2) = currentWindow.drawList.addCircleFilled(pos, g.fontSize * 0.2f, Col.Text.u32, 8)

    fun renderCheckMark(pos: Vec2, col: Int, sz_: Float) {

        val window = g.currentWindow!!

        val thickness = glm.max(sz_ / 5f, 1f)
        val sz = sz_ - thickness * 0.5f
        pos += thickness * 0.25f

        val third = sz / 3f
        val bx = pos.x + third
        val by = pos.y + sz - third * 0.5f
        window.drawList.pathLineTo(Vec2(bx - third, by - third))
        window.drawList.pathLineTo(Vec2(bx, by))

        window.drawList.pathLineTo(Vec2(bx + third * 2, by - third * 2))
        window.drawList.pathStroke(col, false, thickness)
    }

    /** Navigation highlight
     * @param flags: NavHighlightFlag  */
    fun renderNavHighlight(bb: Rect, id: ID, flags: NavHighlightFlags = NavHighlightFlag.TypeDefault.i) {

        if (id != g.navId) return
        if (g.navDisableHighlight && flags hasnt NavHighlightFlag.AlwaysDraw) return
        val window = currentWindow
        if (window.dc.navHideHighlightOneFrame) return

        val rounding = if (flags hasnt NavHighlightFlag.NoRounding) 0f else g.style.frameRounding
        val displayRect = Rect(bb)
        displayRect clipWith window.clipRect
        if (flags has NavHighlightFlag.TypeDefault) {
            val THICKNESS = 2f
            val DISTANCE = 3f + THICKNESS * 0.5f
            displayRect expand Vec2(DISTANCE)
            val fullyVisible = window.clipRect contains displayRect
            if (!fullyVisible)
                window.drawList.pushClipRect(displayRect) // check order here down
            window.drawList.addRect(displayRect.min + (THICKNESS * 0.5f), displayRect.max - (THICKNESS * 0.5f),
                    Col.NavHighlight.u32, rounding, Dcf.All.i, THICKNESS)
            if (!fullyVisible)
                window.drawList.popClipRect()
        }
        if (flags has NavHighlightFlag.TypeThin)
            window.drawList.addRect(displayRect.min, displayRect.max, Col.NavHighlight.u32, rounding, 0.inv(), 1f)
    }

    /** FIXME: Cleanup and move code to ImDrawList. */
    fun renderRectFilledRangeH(drawList: DrawList, rect: Rect, col: Int, xStartNorm_: Float, xEndNorm_: Float, rounding_: Float) {
        var xStartNorm = xStartNorm_
        var xEndNorm = xEndNorm_
        if (xEndNorm == xStartNorm) return
        if (xStartNorm > xEndNorm) {
            val tmp = xStartNorm
            xStartNorm = xEndNorm
            xEndNorm = tmp
        }
        val p0 = Vec2(lerp(rect.min.x, rect.max.x, xStartNorm), rect.min.y)
        val p1 = Vec2(lerp(rect.min.x, rect.max.x, xEndNorm), rect.max.y)
        if (rounding_ == 0f) {
            drawList.addRectFilled(p0, p1, col, 0f)
            return
        }
        val rounding = glm.clamp(glm.min((rect.max.x - rect.min.x) * 0.5f, (rect.max.y - rect.min.y) * 0.5f) - 1f, 0f, rounding_)
        val invRounding = 1f / rounding
        val arc0B = acos01(1f - (p0.x - rect.min.x) * invRounding)
        val arc0E = acos01(1f - (p1.x - rect.min.x) * invRounding)
        val x0 = glm.max(p0.x, rect.min.x + rounding)
        if (arc0B == arc0E) {
            drawList.pathLineTo(Vec2(x0, p1.y))
            drawList.pathLineTo(Vec2(x0, p0.y))
        } else if (arc0B == 0f && arc0E == glm.PIf * 0.5f) {
            drawList.pathArcToFast(Vec2(x0, p1.y - rounding), rounding, 3, 6) // BL
            drawList.pathArcToFast(Vec2(x0, p0.y + rounding), rounding, 6, 9) // TR
        } else {
            drawList.pathArcTo(Vec2(x0, p1.y - rounding), rounding, glm.PIf - arc0E, glm.PIf - arc0B, 3) // BL
            drawList.pathArcTo(Vec2(x0, p0.y + rounding), rounding, glm.PIf + arc0B, glm.PIf + arc0E, 3) // TR
        }
        if (p1.x > rect.min.x + rounding) {
            val arc1B = acos01(1f - (rect.max.x - p1.x) * invRounding)
            val arc1E = acos01(1f - (rect.max.x - p0.x) * invRounding)
            val x1 = glm.min(p1.x, rect.max.x - rounding)
            if (arc1B == arc1E) {
                drawList.pathLineTo(Vec2(x1, p0.y))
                drawList.pathLineTo(Vec2(x1, p1.y))
            } else if (arc1B == 0f && arc1E == glm.PIf * 0.5f) {
                drawList.pathArcToFast(Vec2(x1, p0.y + rounding), rounding, 9, 12) // TR
                drawList.pathArcToFast(Vec2(x1, p1.y - rounding), rounding, 0, 3)  // BR
            } else {
                drawList.pathArcTo(Vec2(x1, p0.y + rounding), rounding, -arc1E, -arc1B, 3) // TR
                drawList.pathArcTo(Vec2(x1, p1.y - rounding), rounding, +arc1B, +arc1E, 3) // BR
            }
        }
        drawList.pathFillConvex(col)
    }

    /** Find the optional ## from which we stop displaying text.    */
    fun findRenderedTextEnd(text: String, textEnd_: Int = text.length): Int {
        val textEnd = if (textEnd_ == 0) text.length else textEnd_
        var textDisplayEnd = 0
        while (textDisplayEnd < textEnd && (text[textDisplayEnd + 0] != '#' || text[textDisplayEnd + 1] != '#'))
            textDisplayEnd++
        return textDisplayEnd
    }

    /** @return []pressed, hovered, held] */
    fun buttonBehavior(bb: Rect, id: ID, flag: Bf) = buttonBehavior(bb, id, flag.i)

    /** @return []pressed, hovered, held] */
    fun buttonBehavior(bb: Rect, id: ID, flags_: ButtonFlags = 0): BooleanArray {

        val window = currentWindow
        var flags = flags_

        if (flags has Bf.Disabled) {
            if (g.activeId == id) clearActiveId()
            return BooleanArray(3)
        }

        // Default behavior requires click+release on same spot
        if (flags hasnt (Bf.PressedOnClickRelease or Bf.PressedOnClick or Bf.PressedOnRelease or Bf.PressedOnDoubleClick))
            flags = flags or Bf.PressedOnClickRelease

        val backupHoveredWindow = g.hoveredWindow
        if (flags has Bf.FlattenChildren && g.hoveredRootWindow === window)
            g.hoveredWindow = window

        var pressed = false
        var hovered = itemHoverable(bb, id)

        // Special mode for Drag and Drop where holding button pressed for a long time while dragging another item triggers the button
        if (flags has Bf.PressedOnDragDropHold && g.dragDropActive && g.dragDropSourceFlags hasnt Ddf.SourceNoHoldToOpenOthers)
            if (isItemHovered(Hf.AllowWhenBlockedByActiveItem)) {
                hovered = true
                setHoveredId(id)
                if (calcTypematicPressedRepeatAmount(g.hoveredIdTimer + 0.0001f, g.hoveredIdTimer + 0.0001f - io.deltaTime,
                                0.01f, 0.7f) != 0) { // FIXME: Our formula for CalcTypematicPressedRepeatAmount() is fishy
                    pressed = true
                    window.focus()
                }
            }

        if (flags has Bf.FlattenChildren && g.hoveredRootWindow === window)
            g.hoveredWindow = backupHoveredWindow

        /*  AllowOverlap mode (rarely used) requires previous frame hoveredId to be null or to match. This allows using
            patterns where a later submitted widget overlaps a previous one.         */
        if (hovered && flags has Bf.AllowItemOverlap && g.hoveredIdPreviousFrame != id && g.hoveredIdPreviousFrame != 0)
            hovered = false

        // Mouse
        if (hovered) {
            if (flags hasnt Bf.NoKeyModifiers || (!io.keyCtrl && !io.keyShift && !io.keyAlt)) {

                /*                         | CLICKING        | HOLDING with ImGuiButtonFlags_Repeat
                PressedOnClickRelease  |  <on release>*  |  <on repeat> <on repeat> .. (NOT on release)  <-- MOST COMMON!
                                                                        (*) only if both click/release were over bounds
                PressedOnClick         |  <on click>     |  <on click> <on repeat> <on repeat> ..
                PressedOnRelease       |  <on release>   |  <on repeat> <on repeat> .. (NOT on release)
                PressedOnDoubleClick   |  <on dclick>    |  <on dclick> <on repeat> <on repeat> ..   */
                // FIXME-NAV: We don't honor those different behaviors.
                if (flags has Bf.PressedOnClickRelease && io.mouseClicked[0]) {
                    setActiveId(id, window)
                    if (flags hasnt Bf.NoNavFocus)
                        setFocusId(id, window)
                    window.focus()
                }
                if ((flags has Bf.PressedOnClick && io.mouseClicked[0]) || (flags has Bf.PressedOnDoubleClick && io.mouseDoubleClicked[0])) {
                    pressed = true
                    if (flags has Bf.NoHoldingActiveID)
                        clearActiveId()
                    else
                        setActiveId(id, window) // Hold on ID
                    window.focus()
                }
                if (flags has Bf.PressedOnRelease && io.mouseReleased[0]) {
                    // Repeat mode trumps <on release>
                    if (!(flags has Bf.Repeat && io.mouseDownDurationPrev[0] >= io.keyRepeatDelay))
                        pressed = true
                    clearActiveId()
                }

                /*  'Repeat' mode acts when held regardless of _PressedOn flags (see table above).
                Relies on repeat logic of IsMouseClicked() but we may as well do it ourselves if we end up exposing
                finer RepeatDelay/RepeatRate settings.  */
                if (flags has Bf.Repeat && g.activeId == id && io.mouseDownDuration[0] > 0f && isMouseClicked(0, true))
                    pressed = true
            }

            if (pressed)
                g.navDisableHighlight = true
        }

        /*  Gamepad/Keyboard navigation
            We report navigated item as hovered but we don't set g.HoveredId to not interfere with mouse.         */
        if (g.navId == id && !g.navDisableHighlight && g.navDisableMouseHover && (g.activeId == 0 || g.activeId == id || g.activeId == window.moveId))
            hovered = true

        if (g.navActivateDownId == id) {
            val navActivatedByCode = g.navActivateId == id
            val navActivatedByInputs = NavInput.Activate.isPressed(if (flags has Bf.Repeat) InputReadMode.Repeat else InputReadMode.Pressed)
            if (navActivatedByCode || navActivatedByInputs)
                pressed = true
            if (navActivatedByCode || navActivatedByInputs || g.activeId == id) {
                // Set active id so it can be queried by user via IsItemActive(), equivalent of holding the mouse button.
                g.navActivateId = id // This is so SetActiveId assign a Nav source
                setActiveId(id, window)
                if (flags hasnt Bf.NoNavFocus)
                    setFocusId(id, window)
                g.activeIdAllowNavDirFlags = (1 shl Dir.Left) or (1 shl Dir.Right) or (1 shl Dir.Up) or (1 shl Dir.Down)
            }
        }
        var held = false
        if (g.activeId == id) {
            if (g.activeIdSource == InputSource.Mouse) {
                if (g.activeIdIsJustActivated)
                    g.activeIdClickOffset = io.mousePos - bb.min
                if (io.mouseDown[0])
                    held = true
                else {
                    if (hovered && flags has Bf.PressedOnClickRelease)
                        if (!(flags has Bf.Repeat && io.mouseDownDurationPrev[0] >= io.keyRepeatDelay)) // Repeat mode trumps <on release>
                            if (!g.dragDropActive)
                                pressed = true
                    clearActiveId()
                }
                if (flags hasnt Bf.NoNavFocus)
                    g.navDisableHighlight = true
            } else if (g.activeIdSource == InputSource.Nav)
                if (g.navActivateDownId != id)
                    clearActiveId()
        }
        return booleanArrayOf(pressed, hovered, held)
    }


    fun buttonEx(label: String, sizeArg: Vec2 = Vec2(), flags_: Int = 0): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(label)
        val labelSize = calcTextSize(label, true)

        val pos = Vec2(window.dc.cursorPos)
        /*  Try to vertically align buttons that are smaller/have no padding so that text baseline matches (bit hacky,
            since it shouldn't be a flag)         */
        if (flags_ has Bf.AlignTextBaseLine && style.framePadding.y < window.dc.currentLineTextBaseOffset)
            pos.y += window.dc.currentLineTextBaseOffset - style.framePadding.y
        val size = calcItemSize(sizeArg, labelSize.x + style.framePadding.x * 2f, labelSize.y + style.framePadding.y * 2f)

        val bb = Rect(pos, pos + size)
        itemSize(bb, style.framePadding.y)
        if (!itemAdd(bb, id)) return false

        var flags = flags_
        if (window.dc.itemFlags has If.ButtonRepeat) flags = flags or Bf.Repeat
        val (pressed, hovered, held) = buttonBehavior(bb, id, flags)

        // Render
        val col = if (hovered && held) Col.ButtonActive else if (hovered) Col.ButtonHovered else Col.Button
        renderNavHighlight(bb, id)
        renderFrame(bb.min, bb.max, col.u32, true, style.frameRounding)
        renderTextClipped(bb.min + style.framePadding, bb.max - style.framePadding, label, 0, labelSize,
                style.buttonTextAlign, bb)

        // Automatically close popups
        //if (pressed && !(flags & ImGuiButtonFlags_DontClosePopups) && (window->Flags & ImGuiWindowFlags_Popup))
        //    CloseCurrentPopup();

        return pressed
    }


    /* Button to close a window    */
    fun closeButton(id: ID, pos: Vec2, radius: Float): Boolean {

        val window = currentWindow

        /*  We intentionally allow interaction when clipped so that a mechanical Alt, Right, Validate sequence close
            a window. (this isn't the regular behavior of buttons, but it doesn't affect the user much because
            navigation tends to keep items visible).   */
        val bb = Rect(pos - radius, pos + radius)
        val isClipped = !itemAdd(bb, id)

        val (pressed, hovered, held) = buttonBehavior(bb, id)
        if (isClipped) return pressed

        // Render
        val center = Vec2(bb.center)
        if (hovered) {
            val col = if (held && hovered) Col.ButtonActive else Col.ButtonHovered
            window.drawList.addCircleFilled(center, 2f max radius, col.u32, 9)
        }

        val crossExtent = (radius * 0.7071f) - 1f
        val crossCol = Col.Text.u32
        center -= 0.5f
        window.drawList.addLine(center + crossExtent, center - crossExtent, crossCol, 1f)
        window.drawList.addLine(center + Vec2(crossExtent, -crossExtent), center + Vec2(-crossExtent, crossExtent), crossCol, 1f)

        return pressed
    }

    fun dragBehavior(id: ID, dataType: DataType, v: FloatArray, ptr: Int, vSpeed: Float, vMin: Float?, vMax: Float?, format: String,
                     power: Float): Boolean = withFloat(v, ptr) {
        dragBehavior(id, DataType.Float, it, vSpeed, vMin, vMax, format, power)
    }

    fun dragBehavior(id: ID, dataType: DataType, v: KMutableProperty0<*>, vSpeed: Float, vMin: Number?, vMax: Number?,
                     format: String, power: Float): Boolean {

        if (g.activeId == id)
            if (g.activeIdSource == InputSource.Mouse && !io.mouseDown[0])
                clearActiveId()
            else if (g.activeIdSource == InputSource.Nav && g.navActivatePressedId == id && !g.activeIdIsJustActivated)
                clearActiveId()

        return when (g.activeId) {
            id -> when (dataType) {
                DataType.Int, DataType.Uint -> dragBehaviorT(dataType, v, vSpeed, vMin as? Int
                        ?: Int.MIN_VALUE, vMax as? Int ?: Int.MAX_VALUE, format, power)
                DataType.Long, DataType.Ulong -> dragBehaviorT(dataType, v, vSpeed, vMin as? Long
                        ?: Long.MIN_VALUE, vMax as? Long ?: Long.MAX_VALUE, format, power)
                DataType.Float -> dragBehaviorT(dataType, v, vSpeed, vMin as? Float
                        ?: -Float.MAX_VALUE, vMax as? Float ?: Float.MAX_VALUE, format, power)
                DataType.Double -> dragBehaviorT(dataType, v, vSpeed, vMin as? Double
                        ?: -Double.MAX_VALUE, vMax as? Double ?: Double.MAX_VALUE, format, power)
                else -> throw Error()
            }
            else -> false
        }
    }

    /** For 32-bits and larger types, slider bounds are limited to half the natural type range.
     *  So e.g. an integer Slider between INT_MAX-10 and INT_MAX will fail, but an integer Slider between INT_MAX/2-10 and INT_MAX/2.
     *  It would be possible to life that limitation with some work but it doesn't seem to be work it for sliders. */
    fun sliderBehavior(bb: Rect, id: ID, v: FloatArray, vMin: Float, vMax: Float, format: String, power: Float,
                       flags: SliderFlags = 0) = sliderBehavior(bb, id, v, 0, vMin, vMax, format, power, flags)

    fun sliderBehavior(bb: Rect, id: ID, v: FloatArray, ptr: Int, vMin: Float, vMax: Float, format: String, power: Float,
                       flags: SliderFlags = 0): Boolean = withFloat(v, ptr) {
        sliderBehavior(bb, id, DataType.Float, it, vMin, vMax, format, power, flags)
    }

    fun sliderBehavior(bb: Rect, id: ID, v: KMutableProperty0<*>, vMin: Float, vMax: Float, format: String, power: Float,
                       flags: SliderFlags = 0): Boolean = sliderBehavior(bb, id, DataType.Float, v, vMin, vMax, format, power, flags)

    fun sliderBehavior(bb: Rect, id: ID, dataType: DataType, v: KMutableProperty0<*>, vMin: Number, vMax: Number,
                       format: String, power: Float, flags: SliderFlags = 0): Boolean = when (dataType) {

        DataType.Int, DataType.Uint -> {
            assert(vMin as Int >= Int.MIN_VALUE / 2)
            assert(vMax as Int <= Int.MAX_VALUE / 2)
            sliderBehaviorT(bb, id, dataType, v, vMin, vMax, format, power, flags)
        }
        DataType.Long, DataType.Ulong -> {
            assert(vMin as Long >= Long.MIN_VALUE / 2)
            assert(vMax as Long <= Long.MAX_VALUE / 2)
            sliderBehaviorT(bb, id, dataType, v, vMin, vMax, format, power, flags)
        }
        DataType.Float -> {
            assert(vMin as Float >= -Float.MAX_VALUE / 2f && vMax as Float <= Float.MAX_VALUE / 2f)
            sliderBehaviorT(bb, id, dataType, v, vMin, vMax as Float, format, power, flags)
        }
        DataType.Double -> {
            assert(vMin as Double >= -Double.MAX_VALUE / 2f && vMax as Double <= Double.MAX_VALUE / 2f)
            sliderBehaviorT(bb, id, dataType, v, vMin, vMax as Double, format, power, flags)
        }
        else -> throw Error()
    }

    // FIXME: Move some of the code into SliderBehavior(). Current responsability is larger than what the equivalent DragBehaviorT<> does, we also do some rendering, etc.

    fun sliderBehaviorT(bb: Rect, id: Int, dataType: DataType, v: KMutableProperty0<*>, vMin: Int, vMax: Int, format: String,
                        power: Float, flags: SliderFlags = 0): Boolean {

        v as KMutableProperty0<Int>
        val window = currentWindow

        // Draw frame
        val frameCol = if (g.activeId == id) Col.FrameBgActive else if (g.hoveredId == id) Col.FrameBgHovered else Col.FrameBg
        renderNavHighlight(bb, id)
        renderFrame(bb.min, bb.max, frameCol.u32, true, style.frameRounding)

        val isHorizontal = flags hasnt SliderFlag.Vertical
        val isDecimal = dataType == DataType.Float || dataType == DataType.Double
        val isPower = power != 0f && isDecimal

        val grabPadding = 2f
        val sliderSz = when {
            isHorizontal -> bb.width - grabPadding * 2f
            else -> bb.height - grabPadding * 2f
        }
        var grabSz = style.grabMinSize
        val vRange = if (vMin < vMax) vMax - vMin else vMin - vMax
        if (!isDecimal && vRange >= 0)  // vRange < 0 may happen on integer overflows
            grabSz = max(sliderSz / (vRange + 1), style.grabMinSize)  // For integer sliders: if possible have the grab size represent 1 unit
        grabSz = grabSz min sliderSz
        val sliderUsableSz = sliderSz - grabSz
        val sliderUsablePosMin = (if (isHorizontal) bb.min.x else bb.min.y) + grabPadding + grabSz * 0.5f
        val sliderUsablePosMax = (if (isHorizontal) bb.max.x else bb.max.y) - grabPadding - grabSz * 0.5f

        // For power curve sliders that cross over sign boundary we want the curve to be symmetric around 0f
        val linearZeroPos = when {   // 0.0->1.0f
            vMin * vMax < 0f -> {
                // Different sign
                val linearDistMinTo0 = glm.pow(if (vMin >= 0) vMin.f else -vMin.f, 1f / power)
                val linearDistMaxTo0 = glm.pow(if (vMax >= 0) vMax.f else -vMax.f, 1f / power)
                linearDistMinTo0 / (linearDistMinTo0 + linearDistMaxTo0)
            } // Same sign
            else -> if (vMin < 0f) 1f else 0f
        }

        // Process interacting with the slider
        var valueChanged = false
        if (g.activeId == id) {

            var setNewValue = false
            var clickedT = 0f

            if (g.activeIdSource == InputSource.Mouse) {
                if (!io.mouseDown[0]) clearActiveId()
                else {
                    val mouseAbsPos = if (isHorizontal) io.mousePos.x else io.mousePos.y
                    clickedT = if (sliderUsableSz > 0f) glm.clamp((mouseAbsPos - sliderUsablePosMin) / sliderUsableSz, 0f, 1f) else 0f
                    if (!isHorizontal)
                        clickedT = 1f - clickedT
                    setNewValue = true
                }
            } else if (g.activeIdSource == InputSource.Nav) {
                val delta2 = getNavInputAmount2d(NavDirSourceFlag.Keyboard or NavDirSourceFlag.PadDPad, InputReadMode.RepeatFast, 0f, 0f)
                var delta = if (isHorizontal) delta2.x else -delta2.y
                if (g.navActivatePressedId == id && !g.activeIdIsJustActivated)
                    clearActiveId()
                else if (delta != 0f) {
                    clickedT = sliderBehaviorCalcRatioFromValue(dataType, v(), vMin, vMax, power, linearZeroPos)
                    delta = when {
                        isDecimal || isPower -> when { // Gamepad/keyboard tweak speeds in % of slider bounds
                            NavInput.TweakSlow.isDown() -> delta / 1_000f
                            else -> delta / 100f
                        }
                        else -> when {
                            (vRange >= -100f && vRange <= 100f) || NavInput.TweakSlow.isDown() ->
                                (if (delta < 0f) -1f else +1f) / vRange.f // Gamepad/keyboard tweak speeds in integer steps
                            else -> delta / 100f
                        }
                    }
                    if (NavInput.TweakFast.isDown())
                        delta *= 10f
                    setNewValue = true
                    // This is to avoid applying the saturation when already past the limits
                    if ((clickedT >= 1f && delta > 0f) || (clickedT <= 0f && delta < 0f))
                        setNewValue = false
                    else
                        clickedT = saturate(clickedT + delta)
                }
            }

            if (setNewValue) {
                var vNew = when {
                    isPower -> {
                        // Account for power curve scale on both sides of the zero
                        if (clickedT < linearZeroPos) {
                            // Negative: rescale to the negative range before powering
                            var a = 1f - (clickedT / linearZeroPos)
                            a = glm.pow(a, power)
                            lerp(glm.min(vMax, 0), vMin, a)
                        } else {
                            // Positive: rescale to the positive range before powering
                            var a = when {
                                glm.abs(linearZeroPos - 1f) > 1e-6f -> (clickedT - linearZeroPos) / (1f - linearZeroPos)
                                else -> clickedT
                            }
                            a = glm.pow(a, power)
                            lerp(glm.max(vMin, 0), vMax, a)
                        }
                    }
                    else -> when {// Linear slider
                        isDecimal -> lerp(vMin, vMax, clickedT)
                        else -> {
                            /*  For integer values we want the clicking position to match the grab box so we round above
                                This code is carefully tuned to work with large values (e.g. high ranges of U64) while preserving this property..                             */
                            val vNewOff_f = (vMax - vMin) * clickedT
                            val vNewOffFloor = vNewOff_f.i
                            val vNewOffRound = (vNewOff_f + 0.5f).i
                            if (!isDecimal && vNewOffFloor < vNewOffRound)
                                vMin + vNewOffRound
                            else
                                vMin + vNewOffFloor
                        }
                    }
                }
                // Round past decimal precision
                vNew = roundScalarWithFormat(format, vNew)

                // Apply result
                if (v() != vNew) {
                    v.set(vNew)
                    valueChanged = true
                }
            }
        }

        // Draw
        var grabT = sliderBehaviorCalcRatioFromValue(dataType, v(), vMin, vMax, power, linearZeroPos)
        if (!isHorizontal)
            grabT = 1f - grabT
        val grabPos = lerp(sliderUsablePosMin, sliderUsablePosMax, grabT)
        val grabBb = when {
            isHorizontal -> Rect(grabPos - grabSz * 0.5f, bb.min.y + grabPadding, grabPos + grabSz * 0.5f, bb.max.y - grabPadding)
            else -> Rect(bb.min.x + grabPadding, grabPos - grabSz * 0.5f, bb.max.x - grabPadding, grabPos + grabSz * 0.5f)
        }
        val col = if (g.activeId == id) Col.SliderGrabActive else Col.SliderGrab
        window.drawList.addRectFilled(grabBb.min, grabBb.max, col.u32, style.grabRounding)

        return valueChanged
    }

    fun sliderBehaviorT(bb: Rect, id: Int, dataType: DataType, v: KMutableProperty0<*>, vMin: Long, vMax: Long, format: String,
                        power: Float, flags: SliderFlags = 0): Boolean {

        v as KMutableProperty0<Long>
        val window = currentWindow

        // Draw frame
        val frameCol = if (g.activeId == id) Col.FrameBgActive else if (g.hoveredId == id) Col.FrameBgHovered else Col.FrameBg
        renderNavHighlight(bb, id)
        renderFrame(bb.min, bb.max, frameCol.u32, true, style.frameRounding)

        val isHorizontal = flags hasnt SliderFlag.Vertical
        val isDecimal = dataType == DataType.Float || dataType == DataType.Double
        val isPower = power != 0f && isDecimal

        val grabPadding = 2f
        val sliderSz = when {
            isHorizontal -> bb.width - grabPadding * 2f
            else -> bb.height - grabPadding * 2f
        }
        var grabSz = style.grabMinSize
        val vRange = if (vMin < vMax) vMax - vMin else vMin - vMax
        if (!isDecimal && vRange >= 0)  // vRange < 0 may happen on integer overflows
            grabSz = max(sliderSz / (vRange + 1).f, style.grabMinSize)  // For integer sliders: if possible have the grab size represent 1 unit
        grabSz = grabSz min sliderSz
        val sliderUsableSz = sliderSz - grabSz
        val sliderUsablePosMin = (if (isHorizontal) bb.min.x else bb.min.y) + grabPadding + grabSz * 0.5f
        val sliderUsablePosMax = (if (isHorizontal) bb.max.x else bb.max.y) - grabPadding - grabSz * 0.5f

        // For power curve sliders that cross over sign boundary we want the curve to be symmetric around 0f
        val linearZeroPos = when {   // 0.0->1.0f
            vMin * vMax < 0f -> {
                // Different sign
                val linearDistMinTo0 = glm.pow(if (vMin >= 0) vMin.f else -vMin.f, 1f / power)
                val linearDistMaxTo0 = glm.pow(if (vMax >= 0) vMax.f else -vMax.f, 1f / power)
                linearDistMinTo0 / (linearDistMinTo0 + linearDistMaxTo0)
            } // Same sign
            else -> if (vMin < 0f) 1f else 0f
        }

        // Process interacting with the slider
        var valueChanged = false
        if (g.activeId == id) {

            var setNewValue = false
            var clickedT = 0f

            if (g.activeIdSource == InputSource.Mouse) {
                if (!io.mouseDown[0]) clearActiveId()
                else {
                    val mouseAbsPos = if (isHorizontal) io.mousePos.x else io.mousePos.y
                    clickedT = if (sliderUsableSz > 0f) glm.clamp((mouseAbsPos - sliderUsablePosMin) / sliderUsableSz, 0f, 1f) else 0f
                    if (!isHorizontal)
                        clickedT = 1f - clickedT
                    setNewValue = true
                }
            } else if (g.activeIdSource == InputSource.Nav) {
                val delta2 = getNavInputAmount2d(NavDirSourceFlag.Keyboard or NavDirSourceFlag.PadDPad, InputReadMode.RepeatFast, 0f, 0f)
                var delta = if (isHorizontal) delta2.x else -delta2.y
                if (g.navActivatePressedId == id && !g.activeIdIsJustActivated)
                    clearActiveId()
                else if (delta != 0f) {
                    clickedT = sliderBehaviorCalcRatioFromValue(dataType, v(), vMin, vMax, power, linearZeroPos)
                    delta = when {
                        isDecimal || isPower -> when { // Gamepad/keyboard tweak speeds in % of slider bounds
                            NavInput.TweakSlow.isDown() -> delta / 1_000f
                            else -> delta / 100f
                        }
                        else -> when {
                            (vRange >= -100f && vRange <= 100f) || NavInput.TweakSlow.isDown() ->
                                (if (delta < 0f) -1f else +1f) / vRange.f // Gamepad/keyboard tweak speeds in integer steps
                            else -> delta / 100f
                        }
                    }
                    if (NavInput.TweakFast.isDown())
                        delta *= 10f
                    setNewValue = true
                    // This is to avoid applying the saturation when already past the limits
                    if ((clickedT >= 1f && delta > 0f) || (clickedT <= 0f && delta < 0f))
                        setNewValue = false
                    else
                        clickedT = saturate(clickedT + delta)
                }
            }

            if (setNewValue) {
                var vNew = when {
                    isPower -> {
                        // Account for power curve scale on both sides of the zero
                        if (clickedT < linearZeroPos) {
                            // Negative: rescale to the negative range before powering
                            var a = 1f - (clickedT / linearZeroPos)
                            a = glm.pow(a, power)
                            lerp(glm.min(vMax, 0L), vMin, a)
                        } else {
                            // Positive: rescale to the positive range before powering
                            var a = when {
                                glm.abs(linearZeroPos - 1f) > 1e-6f -> (clickedT - linearZeroPos) / (1f - linearZeroPos)
                                else -> clickedT
                            }
                            a = glm.pow(a, power)
                            lerp(glm.max(vMin, 0L), vMax, a)
                        }
                    }
                    else -> when {// Linear slider
                        isDecimal -> lerp(vMin, vMax, clickedT)
                        else -> {
                            /*  For integer values we want the clicking position to match the grab box so we round above
                                This code is carefully tuned to work with large values (e.g. high ranges of U64) while preserving this property..                             */
                            val vNewOff_f = (vMax - vMin) * clickedT
                            val vNewOffFloor = vNewOff_f.L
                            val vNewOffRound = (vNewOff_f + 0.5f).L
                            if (!isDecimal && vNewOffFloor < vNewOffRound)
                                vMin + vNewOffRound
                            else
                                vMin + vNewOffFloor
                        }
                    }
                }
                // Round past decimal precision
                vNew = roundScalarWithFormat(format, vNew)

                // Apply result
                if (v() != vNew) {
                    v.set(vNew)
                    valueChanged = true
                }
            }
        }

        // Draw
        var grabT = sliderBehaviorCalcRatioFromValue(dataType, v(), vMin, vMax, power, linearZeroPos)
        if (!isHorizontal)
            grabT = 1f - grabT
        val grabPos = lerp(sliderUsablePosMin, sliderUsablePosMax, grabT)
        val grabBb = when {
            isHorizontal -> Rect(grabPos - grabSz * 0.5f, bb.min.y + grabPadding, grabPos + grabSz * 0.5f, bb.max.y - grabPadding)
            else -> Rect(bb.min.x + grabPadding, grabPos - grabSz * 0.5f, bb.max.x - grabPadding, grabPos + grabSz * 0.5f)
        }
        val col = if (g.activeId == id) Col.SliderGrabActive else Col.SliderGrab
        window.drawList.addRectFilled(grabBb.min, grabBb.max, col.u32, style.grabRounding)

        return valueChanged
    }

    fun sliderBehaviorT(bb: Rect, id: Int, dataType: DataType, v: KMutableProperty0<*>, vMin: Float, vMax: Float, format: String,
                        power: Float, flags: SliderFlags = 0): Boolean {

        v as KMutableProperty0<Float>
        val window = currentWindow

        // Draw frame
        val frameCol = if (g.activeId == id) Col.FrameBgActive else if (g.hoveredId == id) Col.FrameBgHovered else Col.FrameBg
        renderNavHighlight(bb, id)
        renderFrame(bb.min, bb.max, frameCol.u32, true, style.frameRounding)

        val isHorizontal = flags hasnt SliderFlag.Vertical
        val isDecimal = dataType == DataType.Float || dataType == DataType.Double
        val isPower = power != 0f && isDecimal

        val grabPadding = 2f
        val sliderSz = when {
            isHorizontal -> bb.width - grabPadding * 2f
            else -> bb.height - grabPadding * 2f
        }
        var grabSz = style.grabMinSize
        val vRange = if (vMin < vMax) vMax - vMin else vMin - vMax
        if (!isDecimal && vRange >= 0)  // vRange < 0 may happen on integer overflows
            grabSz = max(sliderSz / (vRange + 1).f, style.grabMinSize)  // For integer sliders: if possible have the grab size represent 1 unit
        grabSz = grabSz min sliderSz
        val sliderUsableSz = sliderSz - grabSz
        val sliderUsablePosMin = (if (isHorizontal) bb.min.x else bb.min.y) + grabPadding + grabSz * 0.5f
        val sliderUsablePosMax = (if (isHorizontal) bb.max.x else bb.max.y) - grabPadding - grabSz * 0.5f

        // For power curve sliders that cross over sign boundary we want the curve to be symmetric around 0f
        val linearZeroPos = when {   // 0.0->1.0f
            vMin * vMax < 0f -> {
                // Different sign
                val linearDistMinTo0 = glm.pow(if (vMin >= 0) vMin.f else -vMin.f, 1f / power)
                val linearDistMaxTo0 = glm.pow(if (vMax >= 0) vMax.f else -vMax.f, 1f / power)
                linearDistMinTo0 / (linearDistMinTo0 + linearDistMaxTo0)
            } // Same sign
            else -> if (vMin < 0f) 1f else 0f
        }

        // Process interacting with the slider
        var valueChanged = false
        if (g.activeId == id) {

            var setNewValue = false
            var clickedT = 0f

            if (g.activeIdSource == InputSource.Mouse) {
                if (!io.mouseDown[0]) clearActiveId()
                else {
                    val mouseAbsPos = if (isHorizontal) io.mousePos.x else io.mousePos.y
                    clickedT = if (sliderUsableSz > 0f) glm.clamp((mouseAbsPos - sliderUsablePosMin) / sliderUsableSz, 0f, 1f) else 0f
                    if (!isHorizontal)
                        clickedT = 1f - clickedT
                    setNewValue = true
                }
            } else if (g.activeIdSource == InputSource.Nav) {
                val delta2 = getNavInputAmount2d(NavDirSourceFlag.Keyboard or NavDirSourceFlag.PadDPad, InputReadMode.RepeatFast, 0f, 0f)
                var delta = if (isHorizontal) delta2.x else -delta2.y
                if (g.navActivatePressedId == id && !g.activeIdIsJustActivated)
                    clearActiveId()
                else if (delta != 0f) {
                    clickedT = sliderBehaviorCalcRatioFromValue(dataType, v(), vMin, vMax, power, linearZeroPos)
                    delta = when {
                        isDecimal || isPower -> when { // Gamepad/keyboard tweak speeds in % of slider bounds
                            NavInput.TweakSlow.isDown() -> delta / 1_000f
                            else -> delta / 100f
                        }
                        else -> when {
                            (vRange >= -100f && vRange <= 100f) || NavInput.TweakSlow.isDown() ->
                                (if (delta < 0f) -1f else +1f) / vRange.f // Gamepad/keyboard tweak speeds in integer steps
                            else -> delta / 100f
                        }
                    }
                    if (NavInput.TweakFast.isDown())
                        delta *= 10f
                    setNewValue = true
                    // This is to avoid applying the saturation when already past the limits
                    if ((clickedT >= 1f && delta > 0f) || (clickedT <= 0f && delta < 0f))
                        setNewValue = false
                    else
                        clickedT = saturate(clickedT + delta)
                }
            }

            if (setNewValue) {
                var vNew = when {
                    isPower -> {
                        // Account for power curve scale on both sides of the zero
                        if (clickedT < linearZeroPos) {
                            // Negative: rescale to the negative range before powering
                            var a = 1f - (clickedT / linearZeroPos)
                            a = glm.pow(a, power)
                            lerp(glm.min(vMax, 0f), vMin, a)
                        } else {
                            // Positive: rescale to the positive range before powering
                            var a = when {
                                glm.abs(linearZeroPos - 1f) > 1e-6f -> (clickedT - linearZeroPos) / (1f - linearZeroPos)
                                else -> clickedT
                            }
                            a = glm.pow(a, power)
                            lerp(glm.max(vMin, 0f), vMax, a)
                        }
                    }
                    else -> when {// Linear slider
                        isDecimal -> lerp(vMin, vMax, clickedT)
                        else -> {
                            /*  For integer values we want the clicking position to match the grab box so we round above
                                This code is carefully tuned to work with large values (e.g. high ranges of U64) while preserving this property..                             */
                            val vNewOff_f = (vMax - vMin) * clickedT
                            val vNewOffFloor = vNewOff_f.L
                            val vNewOffRound = (vNewOff_f + 0.5f).L
                            if (!isDecimal && vNewOffFloor < vNewOffRound)
                                vMin + vNewOffRound
                            else
                                vMin + vNewOffFloor
                        }
                    }
                }
                // Round past decimal precision
                vNew = roundScalarWithFormat(format, vNew)

                // Apply result
                if (v() != vNew) {
                    v.set(vNew)
                    valueChanged = true
                }
            }
        }

        // Draw
        var grabT = sliderBehaviorCalcRatioFromValue(dataType, v(), vMin, vMax, power, linearZeroPos)
        if (!isHorizontal)
            grabT = 1f - grabT
        val grabPos = lerp(sliderUsablePosMin, sliderUsablePosMax, grabT)
        val grabBb = when {
            isHorizontal -> Rect(grabPos - grabSz * 0.5f, bb.min.y + grabPadding, grabPos + grabSz * 0.5f, bb.max.y - grabPadding)
            else -> Rect(bb.min.x + grabPadding, grabPos - grabSz * 0.5f, bb.max.x - grabPadding, grabPos + grabSz * 0.5f)
        }
        val col = if (g.activeId == id) Col.SliderGrabActive else Col.SliderGrab
        window.drawList.addRectFilled(grabBb.min, grabBb.max, col.u32, style.grabRounding)

        return valueChanged
    }

    fun sliderBehaviorT(bb: Rect, id: Int, dataType: DataType, v: KMutableProperty0<*>, vMin: Double, vMax: Double, format: String,
                        power: Float, flags: SliderFlags = 0): Boolean {

        v as KMutableProperty0<Double>
        val window = currentWindow

        // Draw frame
        val frameCol = if (g.activeId == id) Col.FrameBgActive else if (g.hoveredId == id) Col.FrameBgHovered else Col.FrameBg
        renderNavHighlight(bb, id)
        renderFrame(bb.min, bb.max, frameCol.u32, true, style.frameRounding)

        val isHorizontal = flags hasnt SliderFlag.Vertical
        val isDecimal = dataType == DataType.Float || dataType == DataType.Double
        val isPower = power != 0f && isDecimal

        val grabPadding = 2f
        val sliderSz = when {
            isHorizontal -> bb.width - grabPadding * 2f
            else -> bb.height - grabPadding * 2f
        }
        var grabSz = style.grabMinSize
        val vRange = if (vMin < vMax) vMax - vMin else vMin - vMax
        if (!isDecimal && vRange >= 0)  // vRange < 0 may happen on integer overflows
            grabSz = max(sliderSz / (vRange + 1).f, style.grabMinSize)  // For integer sliders: if possible have the grab size represent 1 unit
        grabSz = grabSz min sliderSz
        val sliderUsableSz = sliderSz - grabSz
        val sliderUsablePosMin = (if (isHorizontal) bb.min.x else bb.min.y) + grabPadding + grabSz * 0.5f
        val sliderUsablePosMax = (if (isHorizontal) bb.max.x else bb.max.y) - grabPadding - grabSz * 0.5f

        // For power curve sliders that cross over sign boundary we want the curve to be symmetric around 0f
        val linearZeroPos = when {   // 0.0->1.0f
            vMin * vMax < 0f -> {
                // Different sign
                val linearDistMinTo0 = glm.pow(if (vMin >= 0) vMin.f else -vMin.f, 1f / power)
                val linearDistMaxTo0 = glm.pow(if (vMax >= 0) vMax.f else -vMax.f, 1f / power)
                linearDistMinTo0 / (linearDistMinTo0 + linearDistMaxTo0)
            } // Same sign
            else -> if (vMin < 0f) 1f else 0f
        }

        // Process interacting with the slider
        var valueChanged = false
        if (g.activeId == id) {

            var setNewValue = false
            var clickedT = 0f

            if (g.activeIdSource == InputSource.Mouse) {
                if (!io.mouseDown[0]) clearActiveId()
                else {
                    val mouseAbsPos = if (isHorizontal) io.mousePos.x else io.mousePos.y
                    clickedT = if (sliderUsableSz > 0f) glm.clamp((mouseAbsPos - sliderUsablePosMin) / sliderUsableSz, 0f, 1f) else 0f
                    if (!isHorizontal)
                        clickedT = 1f - clickedT
                    setNewValue = true
                }
            } else if (g.activeIdSource == InputSource.Nav) {
                val delta2 = getNavInputAmount2d(NavDirSourceFlag.Keyboard or NavDirSourceFlag.PadDPad, InputReadMode.RepeatFast, 0f, 0f)
                var delta = if (isHorizontal) delta2.x else -delta2.y
                if (g.navActivatePressedId == id && !g.activeIdIsJustActivated)
                    clearActiveId()
                else if (delta != 0f) {
                    clickedT = sliderBehaviorCalcRatioFromValue(dataType, v(), vMin, vMax, power, linearZeroPos)
                    delta = when {
                        isDecimal || isPower -> when { // Gamepad/keyboard tweak speeds in % of slider bounds
                            NavInput.TweakSlow.isDown() -> delta / 1_000f
                            else -> delta / 100f
                        }
                        else -> when {
                            (vRange >= -100f && vRange <= 100f) || NavInput.TweakSlow.isDown() ->
                                (if (delta < 0f) -1f else +1f) / vRange.f // Gamepad/keyboard tweak speeds in integer steps
                            else -> delta / 100f
                        }
                    }
                    if (NavInput.TweakFast.isDown())
                        delta *= 10f
                    setNewValue = true
                    // This is to avoid applying the saturation when already past the limits
                    if ((clickedT >= 1f && delta > 0f) || (clickedT <= 0f && delta < 0f))
                        setNewValue = false
                    else
                        clickedT = saturate(clickedT + delta)
                }
            }

            if (setNewValue) {
                var vNew = when {
                    isPower -> {
                        // Account for power curve scale on both sides of the zero
                        if (clickedT < linearZeroPos) {
                            // Negative: rescale to the negative range before powering
                            var a = 1f - (clickedT / linearZeroPos)
                            a = glm.pow(a, power)
                            lerp(glm.min(vMax, 0.0), vMin, a)
                        } else {
                            // Positive: rescale to the positive range before powering
                            var a = when {
                                glm.abs(linearZeroPos - 1f) > 1e-6f -> (clickedT - linearZeroPos) / (1f - linearZeroPos)
                                else -> clickedT
                            }
                            a = glm.pow(a, power)
                            lerp(glm.max(vMin, 0.0), vMax, a)
                        }
                    }
                    else -> when {// Linear slider
                        isDecimal -> lerp(vMin, vMax, clickedT)
                        else -> {
                            /*  For integer values we want the clicking position to match the grab box so we round above
                                This code is carefully tuned to work with large values (e.g. high ranges of U64) while preserving this property..                             */
                            val vNewOff_f = (vMax - vMin) * clickedT
                            val vNewOffFloor = vNewOff_f.L
                            val vNewOffRound = (vNewOff_f + 0.5f).L
                            if (!isDecimal && vNewOffFloor < vNewOffRound)
                                vMin + vNewOffRound
                            else
                                vMin + vNewOffFloor
                        }
                    }
                }
                // Round past decimal precision
                vNew = roundScalarWithFormat(format, vNew)

                // Apply result
                if (v() != vNew) {
                    v.set(vNew)
                    valueChanged = true
                }
            }
        }

        // Draw
        var grabT = sliderBehaviorCalcRatioFromValue(dataType, v(), vMin, vMax, power, linearZeroPos)
        if (!isHorizontal)
            grabT = 1f - grabT
        val grabPos = lerp(sliderUsablePosMin, sliderUsablePosMax, grabT)
        val grabBb = when {
            isHorizontal -> Rect(grabPos - grabSz * 0.5f, bb.min.y + grabPadding, grabPos + grabSz * 0.5f, bb.max.y - grabPadding)
            else -> Rect(bb.min.x + grabPadding, grabPos - grabSz * 0.5f, bb.max.x - grabPadding, grabPos + grabSz * 0.5f)
        }
        val col = if (g.activeId == id) Col.SliderGrabActive else Col.SliderGrab
        window.drawList.addRectFilled(grabBb.min, grabBb.max, col.u32, style.grabRounding)

        return valueChanged
    }

    fun sliderBehaviorCalcRatioFromValue(dataType: DataType, v: Int, vMin: Int, vMax: Int, power: Float, linearZeroPos: Float): Float {

        if (vMin == vMax) return 0f

        val isPower = power != 1f && (dataType == DataType.Float || dataType == DataType.Double)
        val vClamped = if (vMin < vMax) glm.clamp(v, vMin, vMax) else glm.clamp(v, vMax, vMin)
        return when {
            isPower -> when {
                vClamped < 0f -> {
                    val f = 1f - (vClamped - vMin) / (min(0, vMax) - vMin)
                    (1f - glm.pow(f, 1f / power)) * linearZeroPos
                }
                else -> {
                    val f = ((vClamped - max(0, vMin)) / (vMax - max(0, vMin))).f
                    linearZeroPos + glm.pow(f, 1f / power) * (1f - linearZeroPos)
                }
            }
        // Linear slider
            else -> (vClamped - vMin).f / (vMax - vMin).f
        }
    }

    fun sliderBehaviorCalcRatioFromValue(dataType: DataType, v: Long, vMin: Long, vMax: Long, power: Float, linearZeroPos: Float): Float {

        if (vMin == vMax) return 0f

        val isPower = power != 1f && (dataType == DataType.Float || dataType == DataType.Double)
        val vClamped = if (vMin < vMax) glm.clamp(v, vMin, vMax) else glm.clamp(v, vMax, vMin)
        return when {
            isPower -> when {
                vClamped < 0f -> {
                    val f = 1f - (vClamped - vMin) / (min(0, vMax) - vMin)
                    (1f - glm.pow(f, 1f / power)) * linearZeroPos
                }
                else -> {
                    val f = ((vClamped - max(0, vMin)) / (vMax - max(0, vMin))).f
                    linearZeroPos + glm.pow(f, 1f / power) * (1f - linearZeroPos)
                }
            }
        // Linear slider
            else -> (vClamped - vMin).f / (vMax - vMin).f
        }
    }

    fun sliderBehaviorCalcRatioFromValue(dataType: DataType, v: Float, vMin: Float, vMax: Float, power: Float, linearZeroPos: Float): Float {

        if (vMin == vMax) return 0f

        val isPower = power != 1f && (dataType == DataType.Float || dataType == DataType.Double)
        val vClamped = if (vMin < vMax) glm.clamp(v, vMin, vMax) else glm.clamp(v, vMax, vMin)
        return when {
            isPower -> when {
                vClamped < 0f -> {
                    val f = 1f - (vClamped - vMin) / (min(0f, vMax) - vMin)
                    (1f - glm.pow(f, 1f / power)) * linearZeroPos
                }
                else -> {
                    val f = (vClamped - max(0f, vMin)) / (vMax - max(0f, vMin))
                    linearZeroPos + glm.pow(f, 1f / power) * (1f - linearZeroPos)
                }
            }
        // Linear slider
            else -> (vClamped - vMin) / (vMax - vMin)
        }
    }

    fun sliderBehaviorCalcRatioFromValue(dataType: DataType, v: Double, vMin: Double, vMax: Double, power: Float, linearZeroPos: Float): Float {

        if (vMin == vMax) return 0f

        val isPower = power != 1f && (dataType == DataType.Float || dataType == DataType.Double)
        val vClamped = if (vMin < vMax) glm.clamp(v, vMin, vMax) else glm.clamp(v, vMax, vMin)
        return when {
            isPower -> when {
                vClamped < 0f -> {
                    val f = 1f - ((vClamped - vMin) / (min(0.0, vMax) - vMin)).f
                    (1f - glm.pow(f, 1f / power)) * linearZeroPos
                }
                else -> {
                    val f = ((vClamped - max(0.0, vMin)) / (vMax - max(0.0, vMin))).f
                    linearZeroPos + glm.pow(f, 1f / power) * (1f - linearZeroPos)
                }
            }
        // Linear slider
            else -> ((vClamped - vMin) / (vMax - vMin)).f
        }
    }

    fun inputTextEx(label: String, buf: CharArray, sizeArg: Vec2, flags: InputTextFlags
            /*, ImGuiTextEditCallback callback = NULL, void* user_data = NULL*/): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        // Can't use both together (they both use up/down keys)
        assert(!((flags has Itf.CallbackHistory) && (flags has Itf.Multiline)))
        // Can't use both together (they both use tab key)
        assert(!((flags has Itf.CallbackCompletion) && (flags has Itf.AllowTabInput)))

        val isMultiline = flags has Itf.Multiline
        val isEditable = flags hasnt Itf.ReadOnly
        val isPassword = flags has Itf.Password
        val isUndoable = flags has Itf.NoUndoRedo

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
            if (!itemAdd(totalBb, id, frameBb)) return false
        }
        val hovered = itemHoverable(frameBb, id)
        if (hovered) g.mouseCursor = MouseCursor.TextInput

        // Password pushes a temporary font with only a fallback glyph
        if (isPassword) with(g.inputTextPasswordFont) {
            val glyph = g.font.findGlyph('*')!!
            fontSize = g.font.fontSize
            scale = g.font.scale
            displayOffset = g.font.displayOffset
            ascent = g.font.ascent
            descent = g.font.descent
            containerAtlas = g.font.containerAtlas
            fallbackGlyph = glyph
            fallbackAdvanceX = glyph.advanceX
            assert(glyphs.isEmpty() && indexAdvanceX.isEmpty() && indexLookup.isEmpty())
            pushFont(this)
        }

        // NB: we are only allowed to access 'editState' if we are the active widget.
        val editState = g.inputTextState

        // Using completion callback disable keyboard tabbing
        val tabStop = flags hasnt (Itf.CallbackCompletion or Itf.AllowTabInput)
        val focusRequested = focusableItemRegister(window, id, tabStop)
        val focusRequestedByCode = focusRequested && window.focusIdxAllCounter == window.focusIdxAllRequestCurrent
        val focusRequestedByTab = focusRequested && !focusRequestedByCode

        val userClicked = hovered && io.mouseClicked[0]
        val userScrolled = isMultiline && g.activeId == 0 && editState.id == id && g.activeIdPreviousFrame == drawWindow.getIdNoKeepAlive("#SCROLLY")
        val userNavInputStart = g.activeId != id && (g.navInputId == id || (g.navActivateId == id && g.navInputSource == InputSource.NavKeyboard))

        var clearActiveId = false

        var selectAll = g.activeId != id && (flags has Itf.AutoSelectAll || userNavInputStart) && !isMultiline
//        println(g.imeLastKey)
        if (focusRequested || userClicked || userScrolled || userNavInputStart) {
            if (g.activeId != id || g.imeLastKey != 0) {
                // JVM, put char if no more in ime mode and last key is valid
//                println("${g.imeInProgress}, ${g.imeLastKey}")
                if (!g.imeInProgress && g.imeLastKey != 0) {
                    for (i in 0 until buf.size)
                        if (buf[i] == NUL) {
                            buf[i] = g.imeLastKey.c
                            break
                        }
                    g.imeLastKey = 0
                }
                /*  Start edition
                    Take a copy of the initial buffer value (both in original UTF-8 format and converted to wchar)
                    From the moment we focused we are ignoring the content of 'buf' (unless we are in read-only mode)   */
                val prevLenW = editState.curLenW
                // wchar count <= UTF-8 count. we use +1 to make sure that .Data isn't NULL so it doesn't crash. TODO check if needed
                editState.text = CharArray(buf.size)
                editState.initialText = CharArray(buf.size)
                // UTF-8. we use +1 to make sure that .Data isn't NULL so it doesn't crash. TODO check if needed
//                editState.initialText.add(NUL)
                editState.initialText strncpy buf
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
                    if (!isMultiline && focusRequestedByCode) selectAll = true
                }
                if (flags has Itf.AlwaysInsertMode)
                    editState.state.insertMode = true
                if (!isMultiline && (focusRequestedByTab || (userClicked && io.keyCtrl)))
                    selectAll = true
            }
            setActiveId(id, window)
            setFocusId(id, window)
            window.focus()
            if (!isMultiline && flags hasnt Itf.CallbackHistory)
                g.activeIdAllowNavDirFlags = g.activeIdAllowNavDirFlags or ((1 shl Dir.Up) or (1 shl Dir.Down))
        } else if (io.mouseClicked[0])
        // Release focus when we click outside
            clearActiveId = true

        var valueChanged = false
        var enterPressed = false

        if (g.activeId == id) {

            if (!isEditable && !g.activeIdIsJustActivated) {
                TODO()
                // When read-only we always use the live data passed to the function
//                editState.text.add(NUL)
//                const char* buf_end = NULL
//                        editState.CurLenW = ImTextStrFromUtf8(editState.Text.Data, editState.Text.Size, buf, NULL, &buf_end)
//                editState.CurLenA = (int)(buf_end - buf)
//                editState.CursorClamp()
            }

            editState.bufSizeA = buf.size

            /*  Although we are active we don't prevent mouse from hovering other elements unless we are interacting
                right now with the widget.
                Down the line we should have a cleaner library-wide concept of Selected vs Active.  */
            g.activeIdAllowOverlap = !io.mouseDown[0]
            g.wantTextInputNextFrame = 1

            // Edit in progress
            val mouseX = io.mousePos.x - frameBb.min.x - style.framePadding.x + editState.scrollX
            val mouseY =
                    if (isMultiline)
                        io.mousePos.y - drawWindow.dc.cursorPos.y - style.framePadding.y
                    else g.fontSize * 0.5f

            // OS X style: Double click selects by word instead of selecting whole text
            val isOsx = io.optMacOSXBehaviors
            if (selectAll || (hovered && !isOsx && io.mouseDoubleClicked[0])) {
                editState.selectAll()
                editState.selectedAllMouseLock = true
            } else if (hovered && isOsx && io.mouseDoubleClicked[0]) {
                // Double-click select a word only, OS X style (by simulating keystrokes)
                editState.onKeyPressed(K.WORDLEFT)
                editState.onKeyPressed(K.WORDRIGHT or K.SHIFT)
            } else if (io.mouseClicked[0] && !editState.selectedAllMouseLock) {
                if (hovered) {
                    editState.click(mouseX, mouseY)
                    editState.cursorAnimReset()
                }
            } else if (io.mouseDown[0] && !editState.selectedAllMouseLock && io.mouseDelta notEqual 0f) {
                editState.state.selectStart = editState.state.cursor
                editState.state.selectEnd = editState.locateCoord(mouseX, mouseY)
                editState.cursorFollow = true
                editState.cursorAnimReset()
            }
            if (editState.selectedAllMouseLock && !io.mouseDown[0])
                editState.selectedAllMouseLock = false

            if (io.inputCharacters[0].i != 0) {
                /*  Process text input (before we check for Return because using some IME will effectively send a
                    Return?)
                    We ignore CTRL inputs, but need to allow ALT+CTRL as some keyboards (e.g. German) use AltGR
                    (which _is_ Alt+Ctrl) to input certain characters. */
                val ignoreInputs = (io.keyCtrl && !io.keyAlt) || (isOsx && io.keySuper)
                if (!ignoreInputs && isEditable && !userNavInputStart)
                    io.inputCharacters.filter { it != NUL }.map {
                        withChar { c ->
                            // Insert character if they pass filtering
                            if (inputTextFilterCharacter(c.apply { set(it) }, flags/*, callback, user_data*/))
                                editState.onKeyPressed(c().i)
                        }
                    }
                // Consume characters
                io.inputCharacters.fill(NUL)
            }
        }

        var cancelEdit = false
        if (g.activeId == id && !g.activeIdIsJustActivated && !clearActiveId) {
            // Handle key-presses
            val kMask = if (io.keyShift) K.SHIFT else 0
            val isOsx = io.optMacOSXBehaviors
            // OS X style: Shortcuts using Cmd/Super instead of Ctrl
            val isShortcutKey = (if (isOsx) io.keySuper && !io.keyCtrl else io.keyCtrl && !io.keySuper) && !io.keyAlt && !io.keyShift
            val isOsxShiftShortcut = isOsx && io.keySuper && io.keyShift && !io.keyCtrl && !io.keyAlt
            val isWordmoveKeyDown = if (isOsx) io.keyAlt else io.keyCtrl // OS X style: Text editing cursor movement using Alt instead of Ctrl
            // OS X style: Line/Text Start and End using Cmd+Arrows instead of Home/End
            val isStartendKeyDown = isOsx && io.keySuper && !io.keyCtrl && !io.keyAlt
            val isCtrlKeyOnly = io.keyCtrl && !io.keyShift && !io.keyAlt && !io.keySuper
            val isShiftKeyOnly = io.keyShift && !io.keyCtrl && !io.keyAlt && !io.keySuper

            val isCut = ((isShortcutKey && Key.X.isPressed) || (isShiftKeyOnly && Key.Delete.isPressed)) && isEditable && !isPassword && (!isMultiline || editState.hasSelection)
            val isCopy = ((isShortcutKey && Key.C.isPressed) || (isCtrlKeyOnly && Key.Insert.isPressed)) && !isPassword && (!isMultiline || editState.hasSelection)
            val isPaste = ((isShortcutKey && Key.V.isPressed) || (isShiftKeyOnly && Key.Insert.isPressed)) && isEditable
            val isUndo = ((isShortcutKey && Key.Z.isPressed) && isEditable && isUndoable)
            val isRedo = ((isShortcutKey && Key.Y.isPressed) || (isOsxShiftShortcut && Key.Z.isPressed)) && isEditable && isUndoable

            when {
                Key.LeftArrow.isPressed -> editState.onKeyPressed(when {
                    isStartendKeyDown -> K.LINESTART
                    isWordmoveKeyDown -> K.WORDLEFT
                    else -> K.LEFT
                } or kMask)
                Key.RightArrow.isPressed -> editState.onKeyPressed(when {
                    isStartendKeyDown -> K.LINEEND
                    isWordmoveKeyDown -> K.WORDRIGHT
                    else -> K.RIGHT
                } or kMask)
                Key.UpArrow.isPressed && isMultiline ->
                    if (io.keyCtrl)
                        drawWindow.setScrollY(glm.max(drawWindow.scroll.y - g.fontSize, 0f))
                    else
                        editState.onKeyPressed((if (isStartendKeyDown) K.TEXTSTART else K.UP) or kMask)
                Key.DownArrow.isPressed && isMultiline ->
                    if (io.keyCtrl)
                        drawWindow.setScrollY(glm.min(drawWindow.scroll.y + g.fontSize, scrollMaxY))
                    else
                        editState.onKeyPressed((if (isStartendKeyDown) K.TEXTEND else K.DOWN) or kMask)
                Key.Home.isPressed -> editState.onKeyPressed((if (io.keyCtrl) K.TEXTSTART else K.LINESTART) or kMask)
                Key.End.isPressed -> editState.onKeyPressed((if (io.keyCtrl) K.TEXTEND else K.LINEEND) or kMask)
                Key.Delete.isPressed && isEditable -> editState.onKeyPressed(K.DELETE or kMask)
                Key.Backspace.isPressed && isEditable -> {
                    if (!editState.hasSelection)
                        if (isWordmoveKeyDown)
                            editState.onKeyPressed(K.WORDLEFT or K.SHIFT)
                        else if (isOsx && io.keySuper && !io.keyAlt && !io.keyCtrl)
                            editState.onKeyPressed(K.LINESTART or K.SHIFT)
                    editState.onKeyPressed(K.BACKSPACE or kMask)
                }
                Key.Enter.isPressed -> {
                    val ctrlEnterForNewLine = flags has Itf.CtrlEnterForNewLine
                    if (!isMultiline || (ctrlEnterForNewLine && !io.keyCtrl) || (!ctrlEnterForNewLine && io.keyCtrl)) {
                        clearActiveId = true
                        enterPressed = true
                    } else if (isEditable) {
                        if (flags has Itf.EnterReturnsTrue) {
                            enterPressed = true
                            TODO()
                        } else {
                            editState.insertChars(editState.state.cursor, charArrayOf('\n'), 0, 1)
                            editState.state.cursor += 1
                        }
                    }
                }
                flags has Itf.AllowTabInput && Key.Tab.isPressed && !io.keyCtrl && !io.keyShift && !io.keyAlt && isEditable -> {
                    if (flags hasnt Itf.AllowTabInput)
                        TODO()
                    editState.insertChars(editState.state.cursor, charArrayOf('\t'), 0, 1)
                    editState.state.cursor += 1
                }
                Key.Escape.isPressed -> {
                    cancelEdit = true
                    clearActiveId = true
                }
                isUndo || isRedo -> {
                    editState.onKeyPressed(if (isUndo) K.UNDO else K.REDO)
                    editState.clearSelection()
                }
                isShortcutKey && Key.A.isPressed -> {
                    editState.selectAll()
                    editState.cursorFollow = true
                }
                isCut || isCopy -> {
                    // Cut, Copy
                    val min = min(editState.state.selectStart, editState.state.selectEnd)
                    val max = max(editState.state.selectStart, editState.state.selectEnd)

                    val copy = String(editState.text, min, max - editState.state.cursor)//for some reason this is needed.

                    if (copy.isNotEmpty()) {
                        val stringSelection = StringSelection(copy)
                        val clpbrd = Toolkit.getDefaultToolkit().systemClipboard
                        clpbrd.setContents(stringSelection, null)
                    }
                    if (isCut) {
                        if (!editState.hasSelection)
                            editState.selectAll()
                        System.arraycopy(editState.text, max, editState.text, min, max - min)
                        editState.deleteChars(editState.state.cursor, max - min)
                        editState.state.cursor = min
                        editState.clearSelection()
                    }
                }
                isPaste -> {
                    if (editState.hasSelection)
                        editState.deleteSelection()
                    val data = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
                    data?.let {
                        editState.insertChars(editState.state.cursor, data.toCharArray(), 0, data.toCharArray().size)
                        editState.state.cursor += data.length
                    }
                }
            }
        }
        if (g.activeId == id) {

            if (cancelEdit)
            // Restore initial value. Only return true if restoring to the initial value changes the current buffer contents.
                if (isEditable && !Arrays.equals(buf, editState.initialText)) {
                    for (c in 0 until buf.size)
                        buf[c] = editState.initialText[c]
                    valueChanged = true
                }

            /*  When using `InputTextFlag.EnterReturnsTrue` as a special case we reapply the live buffer back to the
                input buffer before clearing ActiveId, even though strictly speaking it wasn't modified on this frame.
                If we didn't do that, code like `inputInt()` with `InputTextFlag.EnterReturnsTrue` would fail.
                Also this allows the user to use `inputText()` with `InputTextFlag.EnterReturnsTrue` without
                maintaining any user-side storage.  */
            val applyEditBackToUserBuffer = !cancelEdit || (enterPressed && flags hasnt Itf.EnterReturnsTrue)
            if (applyEditBackToUserBuffer) {
                // Apply new value immediately - copy modified buffer back
                // Note that as soon as the input box is active, the in-widget value gets priority over any underlying modification of the input buffer
                // FIXME: We actually always render 'buf' when calling DrawList->AddText, making the comment above incorrect.
                // FIXME-OPT: CPU waste to do this every time the widget is active, should mark dirty state from the stb_textedit callbacks.
                if (isEditable)
                    editState.tempTextBuffer = CharArray(editState.text.size * 4) { editState.text.getOrElse(it) { NUL } }

                // User callback
                if (flags has (Itf.CallbackCompletion or Itf.CallbackHistory or Itf.CallbackAlways)) {
                    TODO()
                    //                        IM_ASSERT(callback != NULL);
//
//                        // The reason we specify the usage semantic (Completion/History) is that Completion needs to disable keyboard TABBING at the moment.
//                        val eventFlag: InputTextFlags = 0
//                        ImGuiKey event_key = ImGuiKey_COUNT;
//                        if ((flags & ImGuiInputTextFlags_CallbackCompletion) != 0 && IsKeyPressedMap(ImGuiKey_Tab))
//                        {
//                            event_flag = ImGuiInputTextFlags_CallbackCompletion;
//                            event_key = ImGuiKey_Tab;
//                        }
//                        else if ((flags & ImGuiInputTextFlags_CallbackHistory) != 0 && IsKeyPressedMap(ImGuiKey_UpArrow))
//                        {
//                            event_flag = ImGuiInputTextFlags_CallbackHistory;
//                            event_key = ImGuiKey_UpArrow;
//                        }
//                        else if ((flags & ImGuiInputTextFlags_CallbackHistory) != 0 && IsKeyPressedMap(ImGuiKey_DownArrow))
//                        {
//                            event_flag = ImGuiInputTextFlags_CallbackHistory;
//                            event_key = ImGuiKey_DownArrow;
//                        }
//                        else if (flags & ImGuiInputTextFlags_CallbackAlways)
//                        event_flag = ImGuiInputTextFlags_CallbackAlways;
//
//                        if (event_flag)
//                        {
//                            ImGuiTextEditCallbackData callback_data;
//                            memset(&callback_data, 0, sizeof(ImGuiTextEditCallbackData));
//                            callback_data.EventFlag = event_flag;
//                            callback_data.Flags = flags;
//                            callback_data.UserData = user_data;
//                            callback_data.ReadOnly = !is_editable;
//
//                            callback_data.EventKey = event_key;
//                            callback_data.Buf = edit_state.TempTextBuffer.Data;
//                            callback_data.BufTextLen = edit_state.CurLenA;
//                            callback_data.BufSize = edit_state.BufSizeA;
//                            callback_data.BufDirty = false;
//
//                            // We have to convert from wchar-positions to UTF-8-positions, which can be pretty slow (an incentive to ditch the ImWchar buffer, see https://github.com/nothings/stb/issues/188)
//                            ImWchar* text = edit_state.Text.Data;
//                            const int utf8_cursor_pos = callback_data.CursorPos = ImTextCountUtf8BytesFromStr(text, text + edit_state.StbState.cursor);
//                            const int utf8_selection_start = callback_data.SelectionStart = ImTextCountUtf8BytesFromStr(text, text + edit_state.StbState.select_start);
//                            const int utf8_selection_end = callback_data.SelectionEnd = ImTextCountUtf8BytesFromStr(text, text + edit_state.StbState.select_end);
//
//                            // Call user code
//                            callback(&callback_data);
//
//                            // Read back what user may have modified
//                            IM_ASSERT(callback_data.Buf == edit_state.TempTextBuffer.Data);  // Invalid to modify those fields
//                            IM_ASSERT(callback_data.BufSize == edit_state.BufSizeA);
//                            IM_ASSERT(callback_data.Flags == flags);
//                            if (callback_data.CursorPos != utf8_cursor_pos)            edit_state.StbState.cursor = ImTextCountCharsFromUtf8(callback_data.Buf, callback_data.Buf + callback_data.CursorPos);
//                            if (callback_data.SelectionStart != utf8_selection_start)  edit_state.StbState.select_start = ImTextCountCharsFromUtf8(callback_data.Buf, callback_data.Buf + callback_data.SelectionStart);
//                            if (callback_data.SelectionEnd != utf8_selection_end)      edit_state.StbState.select_end = ImTextCountCharsFromUtf8(callback_data.Buf, callback_data.Buf + callback_data.SelectionEnd);
//                            if (callback_data.BufDirty)
//                            {
//                                IM_ASSERT(callback_data.BufTextLen == (int)strlen(callback_data.Buf)); // You need to maintain BufTextLen if you change the text!
//                                edit_state.CurLenW = ImTextStrFromUtf8(edit_state.Text.Data, edit_state.Text.Size, callback_data.Buf, NULL);
//                                edit_state.CurLenA = callback_data.BufTextLen;  // Assume correct length and valid UTF-8 from user, saves us an extra strlen()
//                                edit_state.CursorAnimReset();
//                            }
//                        }
                }
                // Copy back to user buffer
                if (isEditable && !Arrays.equals(editState.tempTextBuffer, buf)) {
                    for (i in buf.indices) buf[i] = editState.tempTextBuffer[i]
                    valueChanged = true
                }
            }
        }
        // Release active ID at the end of the function (so e.g. pressing Return still does a final application of the value)
        if (clearActiveId && g.activeId == id) clearActiveId()

        // ------------------------- Render -------------------------
        /*  Select which buffer we are going to display. When ImGuiInputTextFlags_NoLiveEdit is set 'buf' might still
            be the old value. We set buf to NULL to prevent accidental usage from now on.         */
        val bufDisplay = if (g.activeId == id && isEditable) editState.tempTextBuffer else buf
//        buf[0] = ""

        renderNavHighlight(frameBb, id)
        if (!isMultiline)
            renderFrame(frameBb.min, frameBb.max, Col.FrameBg.u32, true, style.frameRounding)

        val clipRect = Vec4(frameBb.min, frameBb.min + size) // Not using frameBb.Max because we have adjusted size
        val renderPos = if (isMultiline) Vec2(drawWindow.dc.cursorPos) else frameBb.min + style.framePadding
        val textSize = Vec2()
        val isCurrentlyScrolling = editState.id == id && isMultiline && g.activeId == drawWindow.getIdNoKeepAlive("#SCROLLY")
        if (g.activeId == id || isCurrentlyScrolling) {

            editState.cursorAnim += io.deltaTime

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
                var s = 0
                while (s < text.size && text[s] != NUL)
                    if (text[s++] == '\n') {
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
                cursorOffset.x = inputTextCalcTextSizeW(text, start, searchesInputPtr[0]).x
                cursorOffset.y = searchesResultLineNumber[0] * g.fontSize
                if (searchesResultLineNumber[1] >= 0) {
                    start = text.beginOfLine(searchesInputPtr[1])
                    selectStartOffset.x = inputTextCalcTextSizeW(text, start, searchesInputPtr[1]).x
                    selectStartOffset.y = searchesResultLineNumber[1] * g.fontSize
                }

                // Store text height (note that we haven't calculated text width at all, see GitHub issues #383, #1224)
                if (isMultiline)
                    textSize.put(size.x, lineCount * g.fontSize)
            }

            // Scroll
            if (editState.cursorFollow) {
                // Horizontal scroll in chunks of quarter width
                if (flags hasnt Itf.NoHorizontalScroll) {
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
                        val rectSize = withInt {
                            inputTextCalcTextSizeW(text, p, textSelectedEnd, it, stopOnNewLine = true).also { p = it() }
                        }
                        // So we can see selected empty lines
                        if (rectSize.x <= 0f) rectSize.x = (g.font.getCharAdvance_ssaaaaaaaaaaaa(' ') * 0.5f).i.f
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
                    editState.curLenA, 0f, clipRect.takeIf { isMultiline })

            // Draw blinking cursor
            val cursorIsVisible = !io.optCursorBlink || g.inputTextState.cursorAnim <= 0f || glm.mod(g.inputTextState.cursorAnim, 1.2f) <= 0.8f
            val cursorScreenPos = renderPos + cursorOffset - renderScroll
            val cursorScreenRect = Rect(cursorScreenPos.x, cursorScreenPos.y - g.fontSize + 0.5f, cursorScreenPos.x + 1f, cursorScreenPos.y - 1.5f)
            if (cursorIsVisible && cursorScreenRect overlaps Rect(clipRect))
                drawWindow.drawList.addLine(cursorScreenRect.min, cursorScreenRect.bl, Col.Text.u32)

            /*  Notify OS of text input position for advanced IME (-1 x offset so that Windows IME can cover our cursor.
                Bit of an extra nicety.)             */
            if (isEditable)
                g.platformImePos = Vec2(cursorScreenPos.x - 1, cursorScreenPos.y - g.fontSize)
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

        return if (flags has Itf.EnterReturnsTrue) enterPressed else valueChanged
    }

    /** Create text input in place of a slider (when CTRL+Clicking on slider)
     *  FIXME: Logic is messy and confusing. */
    fun inputScalarAsWidgetReplacement(bb: Rect, id: ID, label: String, dataType: DataType, data: KMutableProperty0<*>,
                                       format_: String): Boolean {

        val window = currentWindow

        /*  Our replacement widget will override the focus ID (registered previously to allow for a TAB focus to happen)
            On the first frame, g.ScalarAsInputTextId == 0, then on subsequent frames it becomes == id  */
        setActiveId(g.scalarAsInputTextId, window)
        g.activeIdAllowNavDirFlags = (1 shl Dir.Up) or (1 shl Dir.Down)
        setHoveredId(0)
        focusableItemUnregister(window)

        val fmtBuf = CharArray(32)
        val format = parseFormatTrimDecorations(format_, fmtBuf)
        var dataBuf = data.format(dataType, format)
        dataBuf = trimBlanks(dataBuf)
        val flags = Itf.AutoSelectAll or when (dataType) {
            DataType.Float, DataType.Double -> Itf.CharsScientific
            else -> Itf.CharsDecimal
        }
        val valueChanged = inputTextEx(label, dataBuf, bb.size, flags)
        if (g.scalarAsInputTextId == 0) {   // First frame we started displaying the InputText widget
            assert(g.activeId == id) // InputText ID expected to match the Slider ID
            g.scalarAsInputTextId = g.activeId
            setHoveredId(id)
        }
        return when {
            valueChanged -> dataTypeApplyOpFromText(dataBuf, g.inputTextState.initialText, dataType, data)
            else -> false
        }
    }

    /** Note: only access 3 floats if ColorEditFlag.NoAlpha flag is set.   */
    fun colorTooltip(text: String, col: FloatArray, flags: ColorEditFlags) {
        val cr = F32_TO_INT8_SAT(col[0])
        val cg = F32_TO_INT8_SAT(col[1])
        val cb = F32_TO_INT8_SAT(col[2])
        val ca = if (flags has Cef.NoAlpha) 255 else F32_TO_INT8_SAT(col[3])
        beginTooltipEx(0, true)

        val textEnd = if (text.isEmpty()) findRenderedTextEnd(text) else 0
        if (textEnd > 0) {
            textUnformatted(text, textEnd)
            separator()
        }
        val sz = Vec2(g.fontSize * 3 + style.framePadding.y * 2)
        val f = (flags and (Cef.NoAlpha or Cef.AlphaPreview or Cef.AlphaPreviewHalf)) or Cef.NoTooltip
        colorButton("##preview", Vec4(col), f, sz)
        sameLine()
        if (flags has Cef.NoAlpha)
            text("#%02X%02X%02X\nR: $cr, G: $cg, B: $cb\n(%.3f, %.3f, %.3f)", cr, cg, cb, col[0], col[1], col[2])
        else
            text("#%02X%02X%02X%02X\nR:$cr, G:$cg, B:$cb, A:$ca\n(%.3f, %.3f, %.3f, %.3f)", cr, cg, cb, ca, col[0], col[1], col[2], col[3])
        endTooltip()
    }

    /** @param flags ColorEditFlags */
    fun colorEditOptionsPopup(col: FloatArray, flags: ColorEditFlags) {
        val allowOptInputs = flags hasnt Cef._InputsMask
        val allowOptDatatype = flags hasnt Cef._DataTypeMask
        if ((!allowOptInputs && !allowOptDatatype) || !beginPopup("context")) return
        var opts = g.colorEditOptions
        if (allowOptInputs) {
            if (radioButton("RGB", opts has Cef.RGB))
                opts = (opts wo Cef._InputsMask) or Cef.RGB
            if (radioButton("HSV", opts has Cef.HSV))
                opts = (opts wo Cef._InputsMask) or Cef.HSV
            if (radioButton("HEX", opts has Cef.HEX))
                opts = (opts wo Cef._InputsMask) or Cef.HEX
        }
        if (allowOptDatatype) {
            if (allowOptInputs) separator()
            if (radioButton("0..255", opts has Cef.Uint8))
                opts = (opts wo Cef._DataTypeMask) or Cef.Uint8
            if (radioButton("0.00..1.00", opts has Cef.Float))
                opts = (opts wo Cef._DataTypeMask) or Cef.Float
        }

        if (allowOptInputs || allowOptDatatype) separator()
        if (button("Copy as..", Vec2(-1, 0)))
            openPopup("Copy")
        if (beginPopup("Copy")) {
            val cr = F32_TO_INT8_SAT(col[0])
            val cg = F32_TO_INT8_SAT(col[1])
            val cb = F32_TO_INT8_SAT(col[2])
            val ca = if (flags has Cef.NoAlpha) 255 else F32_TO_INT8_SAT(col[3])
            var buf = "(%.3ff, %.3ff, %.3ff, %.3ff)".format(col[0], col[1], col[2], if (flags has Cef.NoAlpha) 1f else col[3])
            if (selectable(buf))
                setClipboardText(buf)
            buf = "(%d,%d,%d,%d)".format(cr, cg, cb, ca)
            if (selectable(buf))
                setClipboardText(buf)
            buf = when {
                flags has Cef.NoAlpha -> "0x%02X%02X%02X".format(cr, cg, cb)
                else -> "0x%02X%02X%02X%02X".format(cr, cg, cb, ca)
            }
            if (selectable(buf))
                setClipboardText(buf)
            endPopup()
        }

        g.colorEditOptions = opts
        endPopup()
    }

    fun treeNodeBehavior(id: ID, flags: TreeNodeFlags, label: String, labelEnd: Int = findRenderedTextEnd(label)): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val displayFrame = flags has Tnf.Framed
        val padding = if (displayFrame || flags has Tnf.FramePadding) Vec2(style.framePadding) else Vec2(style.framePadding.x, 0f)

        val labelSize = calcTextSize(label, labelEnd, false)

        // We vertically grow up to current line height up the typical widget height.
        val textBaseOffsetY = glm.max(padding.y, window.dc.currentLineTextBaseOffset) // Latch before ItemSize changes it
        val frameHeight = glm.max(glm.min(window.dc.currentLineHeight, g.fontSize + style.framePadding.y * 2), labelSize.y + padding.y * 2)
        val frameBb = Rect(window.dc.cursorPos, Vec2(window.pos.x + contentRegionMax.x, window.dc.cursorPos.y + frameHeight))
        if (displayFrame) {
            // Framed header expand a little outside the default padding
            frameBb.min.x -= (window.windowPadding.x * 0.5f).i.f - 1
            frameBb.max.x += (window.windowPadding.x * 0.5f).i.f - 1
        }

        val textOffsetX = g.fontSize + padding.x * if (displayFrame) 3 else 2   // Collapser arrow width + Spacing
        val textWidth = g.fontSize + if (labelSize.x > 0f) labelSize.x + padding.x * 2 else 0f   // Include collapser
        itemSize(Vec2(textWidth, frameHeight), textBaseOffsetY)

        /*  For regular tree nodes, we arbitrary allow to click past 2 worth of ItemSpacing
            (Ideally we'd want to add a flag for the user to specify if we want the hit test to be done up to the
            right side of the content or not)         */
        val interactBb = if (displayFrame) Rect(frameBb) else Rect(frameBb.min.x, frameBb.min.y, frameBb.min.x + textWidth + style.itemSpacing.x * 2, frameBb.max.y)
        var isOpen = treeNodeBehaviorIsOpen(id, flags)

        /*  Store a flag for the current depth to tell if we will allow closing this node when navigating one of its child.
            For this purpose we essentially compare if g.NavIdIsAlive went from 0 to 1 between TreeNode() and TreePop().
            This is currently only support 32 level deep and we are fine with (1 << Depth) overflowing into a zero. */
        if (isOpen && !g.navIdIsAlive && flags has Tnf.NavLeftJumpsBackHere && flags hasnt Tnf.NoTreePushOnOpen)
            window.dc.treeDepthMayJumpToParentOnPop = window.dc.treeDepthMayJumpToParentOnPop or (1 shl window.dc.treeDepth)

        val itemAdd = itemAdd(interactBb, id)
        window.dc.lastItemStatusFlags = window.dc.lastItemStatusFlags or ItemStatusFlag.HasDisplayRect
        window.dc.lastItemDisplayRect put frameBb

        if (!itemAdd) {
            if (isOpen && flags hasnt Tnf.NoTreePushOnOpen)
                treePushRawId(id)
            return isOpen
        }

        /*  Flags that affects opening behavior:
                - 0(default) ..................... single-click anywhere to open
                - OpenOnDoubleClick .............. double-click anywhere to open
                - OpenOnArrow .................... single-click on arrow to open
                - OpenOnDoubleClick|OpenOnArrow .. single-click on arrow or double-click anywhere to open   */
        var buttonFlags = Bf.NoKeyModifiers or if (flags has Tnf.AllowItemOverlap) Bf.AllowItemOverlap else Bf.Null
        if (flags hasnt Tnf.Leaf)
            buttonFlags = buttonFlags or Bf.PressedOnDragDropHold
        if (flags has Tnf.OpenOnDoubleClick)
            buttonFlags = buttonFlags or Bf.PressedOnDoubleClick or (if (flags has Tnf.OpenOnArrow) Bf.PressedOnClickRelease else Bf.Null)

        val (pressed, hovered, held) = buttonBehavior(interactBb, id, buttonFlags)
        if (flags hasnt Tnf.Leaf) {
            var toggled = false
            if (pressed) {
                toggled = !(flags has (Tnf.OpenOnArrow or Tnf.OpenOnDoubleClick)) || g.navActivateId == id
                if (flags has Tnf.OpenOnArrow) {
                    val max = Vec2(interactBb.min.x + textOffsetX, interactBb.max.y)
                    toggled = isMouseHoveringRect(interactBb.min, max) && !g.navDisableMouseHover || toggled
                }
                if (flags has Tnf.OpenOnDoubleClick)
                    toggled = io.mouseDoubleClicked[0] || toggled
                // When using Drag and Drop "hold to open" we keep the node highlighted after opening, but never close it again.
                if (g.dragDropActive && isOpen)
                    toggled = false
            }

            if (g.navId == id && g.navMoveRequest && g.navMoveDir == Dir.Left && isOpen) {
                toggled = true
                navMoveRequestCancel()
            }
            // If there's something upcoming on the line we may want to give it the priority?
            if (g.navId == id && g.navMoveRequest && g.navMoveDir == Dir.Right && !isOpen) {
                toggled = true
                navMoveRequestCancel()
            }
            if (toggled) {
                isOpen = !isOpen
                window.dc.stateStorage[id] = isOpen
            }
        }
        if (flags has Tnf.AllowItemOverlap)
            setItemAllowOverlap()

        // Render
        val col = if (held && hovered) Col.HeaderActive else if (hovered) Col.HeaderHovered else Col.Header
        val textPos = frameBb.min + Vec2(textOffsetX, textBaseOffsetY)
        if (displayFrame) {
            // Framed type
            renderFrame(frameBb.min, frameBb.max, col.u32, true, style.frameRounding)
            renderNavHighlight(frameBb, id, NavHighlightFlag.TypeThin.i)
            renderArrow(frameBb.min + Vec2(padding.x, textBaseOffsetY), if (isOpen) Dir.Down else Dir.Right, 1f)
            if (g.logEnabled) {
                /*  NB: '##' is normally used to hide text (as a library-wide feature), so we need to specify the text
                    range to make sure the ## aren't stripped out here.                 */
                logRenderedText(textPos, "\n##", 3)
                renderTextClipped(textPos, frameBb.max, label, labelEnd, labelSize)
                logRenderedText(textPos, "#", 3)
            } else
                renderTextClipped(textPos, frameBb.max, label, labelEnd, labelSize)
        } else {
            // Unframed typed for tree nodes
            if (hovered || flags has Tnf.Selected) {
                renderFrame(frameBb.min, frameBb.max, col.u32, false)
                renderNavHighlight(frameBb, id, NavHighlightFlag.TypeThin.i)
            }
            if (flags has Tnf.Bullet)
                TODO()//renderBullet(bb.Min + ImVec2(textOffsetX * 0.5f, g.FontSize * 0.50f + textBaseOffsetY))
            else if (flags hasnt Tnf.Leaf)
                renderArrow(frameBb.min + Vec2(padding.x, g.fontSize * 0.15f + textBaseOffsetY),
                        if (isOpen) Dir.Down else Dir.Right, 0.7f)
            if (g.logEnabled)
                logRenderedText(textPos, ">")
            renderText(textPos, label, labelEnd, false)
        }

        if (isOpen && flags hasnt Tnf.NoTreePushOnOpen)
            treePushRawId(id)
        return isOpen
    }

    /** Consume previous SetNextTreeNodeOpened() data, if any. May return true when logging */
    fun treeNodeBehaviorIsOpen(id: ID, flags: TreeNodeFlags = 0): Boolean {

        if (flags has Tnf.Leaf) return true

        // We only write to the tree storage if the user clicks (or explicitly use SetNextTreeNode*** functions)
        val window = g.currentWindow!!
        val storage = window.dc.stateStorage

        var isOpen: Boolean
        if (g.nextTreeNodeOpenCond != Cond.Null) {
            if (g.nextTreeNodeOpenCond has Cond.Always) {
                isOpen = g.nextTreeNodeOpenVal
                storage[id] = isOpen
            } else {
                /*  We treat ImGuiSetCondition_Once and ImGuiSetCondition_FirstUseEver the same because tree node state
                    are not saved persistently.                 */
                val storedValue = storage.int(id, -1)
                if (storedValue == -1) {
                    isOpen = g.nextTreeNodeOpenVal
                    storage[id] = isOpen
                } else
                    isOpen = storedValue != 0
            }
            g.nextTreeNodeOpenCond = Cond.Null
        } else
            isOpen = storage.int(id, if (flags has Tnf.DefaultOpen) 1 else 0) != 0 // TODO rename back

        /*  When logging is enabled, we automatically expand tree nodes (but *NOT* collapsing headers.. seems like
            sensible behavior).
            NB- If we are above max depth we still allow manually opened nodes to be logged.    */
        if (g.logEnabled && flags hasnt Tnf.NoAutoOpenOnLog && window.dc.treeDepth < g.logAutoExpandMaxDepth)
            isOpen = true

        return isOpen
    }

    fun treePushRawId(id: ID) {
        val window = currentWindow
        indent()
        window.dc.treeDepth++
        window.idStack.push(id)
    }


    fun plotEx(plotType: PlotType, label: String, data: imgui_widgetsMain.PlotArray, valuesOffset: Int, overlayText: String,
               scaleMin_: Float, scaleMax_: Float, graphSize: Vec2) {

        val window = currentWindow
        if (window.skipItems) return

        var scaleMin = scaleMin_
        var scaleMax = scaleMax_
        val valuesCount = data.count()

        val labelSize = calcTextSize(label, 0, true)
        if (graphSize.x == 0f) graphSize.x = calcItemWidth()
        if (graphSize.y == 0f) graphSize.y = labelSize.y + style.framePadding.y * 2

        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(graphSize))
        val innerBb = Rect(frameBb.min + style.framePadding, frameBb.max - style.framePadding)
        val totalBb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0))
        itemSize(totalBb, style.framePadding.y)
        if (!itemAdd(totalBb, 0, frameBb)) return
        val hovered = itemHoverable(innerBb, 0)

        // Determine scale from values if not specified
        if (scaleMin == Float.MAX_VALUE || scaleMax == Float.MAX_VALUE) {
            var vMin = Float.MAX_VALUE
            var vMax = -Float.MAX_VALUE
            for (i in 0 until valuesCount) {
                val v = data[i]
                vMin = vMin min v
                vMax = vMax max v
            }
            if (scaleMin == Float.MAX_VALUE) scaleMin = vMin
            if (scaleMax == Float.MAX_VALUE) scaleMax = vMax
        }

        renderFrame(frameBb.min, frameBb.max, Col.FrameBg.u32, true, style.frameRounding)

        if (valuesCount > 0) {
            val resW = min(graphSize.x.i, valuesCount) + if (plotType == PlotType.Lines) -1 else 0
            val itemCount = valuesCount + if (plotType == PlotType.Lines) -1 else 0

            // Tooltip on hover
            var vHovered = -1
            if (hovered) {
                val t = glm.clamp((io.mousePos.x - innerBb.min.x) / (innerBb.max.x - innerBb.min.x), 0f, 0.9999f)
                val vIdx = (t * itemCount).i
                assert(vIdx in 0 until valuesCount)

                val v0 = data[(vIdx + valuesOffset) % valuesCount]
                val v1 = data[(vIdx + 1 + valuesOffset) % valuesCount]
                when (plotType) {
                    PlotType.Lines -> setTooltip("$vIdx: %8.4g\n${vIdx + 1}: %8.4g", v0, v1)
                    PlotType.Histogram -> setTooltip("$vIdx: %8.4g", v0)
                }
                vHovered = vIdx
            }

            val tStep = 1f / resW
            val invScale = if (scaleMin == scaleMax) 0f else 1f / (scaleMax - scaleMin)

            val v0 = data[(0 + valuesOffset) % valuesCount]
            var t0 = 0f
            // Point in the normalized space of our target rectangle
            val tp0 = Vec2(t0, 1f - saturate((v0 - scaleMin) * invScale))
            // Where does the zero line stands
            val histogramZeroLineT = if (scaleMin * scaleMax < 0f) -scaleMin * invScale else if (scaleMin < 0f) 0f else 1f

            val colBase = (if (plotType == PlotType.Lines) Col.PlotLines else Col.PlotHistogram).u32
            val colHovered = (if (plotType == PlotType.Lines) Col.PlotLinesHovered else Col.PlotHistogramHovered).u32

            for (n in 0 until resW) {
                val t1 = t0 + tStep
                val v1Idx = (t0 * itemCount + 0.5f).i
                assert(v1Idx in 0 until valuesCount)
                val v1 = data[(v1Idx + valuesOffset + 1) % valuesCount]
                val tp1 = Vec2(t1, 1f - saturate((v1 - scaleMin) * invScale))

                // NB: Draw calls are merged together by the DrawList system. Still, we should render our batch are lower level to save a bit of CPU.
                val pos0 = innerBb.min.lerp(innerBb.max, tp0)
                val pos1 = innerBb.min.lerp(innerBb.max, if (plotType == PlotType.Lines) tp1 else Vec2(tp1.x, histogramZeroLineT))
                when (plotType) {
                    PlotType.Lines -> window.drawList.addLine(pos0, pos1, if (vHovered == v1Idx) colHovered else colBase)
                    PlotType.Histogram -> {
                        if (pos1.x >= pos0.x + 2f) pos1.x -= 1f
                        window.drawList.addRectFilled(pos0, pos1, if (vHovered == v1Idx) colHovered else colBase)
                    }
                }
                t0 = t1
                tp0 put tp1
            }
        }
        // Text overlay
        if (overlayText.isNotEmpty())
            renderTextClipped(Vec2(frameBb.min.x, frameBb.min.y + style.framePadding.y), frameBb.max, overlayText, 0, null, Vec2(0.5f, 0f))
        if (labelSize.x > 0f)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, innerBb.min.y), label)
    }


    /** The reason this is exposed in imgui_internal.h is: on touch-based system that don't have hovering,
     *  we want to dispatch inputs to the right target (imgui vs imgui+app) */
    fun newFrameUpdateHoveredWindowAndCaptureFlags() {

        /*  Find the window hovered by mouse:
            - Child windows can extend beyond the limit of their parent so we need to derive HoveredRootWindow from HoveredWindow.
            - When moving a window we can skip the search, which also conveniently bypasses the fact that window.outerRectClipped
                is lagging as this point of the frame.
            - We also support the moved window toggling the NoInputs flag after moving has started in order
                to be able to detect windows below it, which is useful for e.g. docking mechanisms. */
        g.hoveredWindow = g.movingWindow?.flags?.hasnt(Wf.NoInputs)?.let { g.movingWindow } ?: findHoveredWindow()
        g.hoveredRootWindow = g.hoveredWindow?.rootWindow

        fun nullate() {
            g.hoveredWindow = null
            g.hoveredRootWindow = null
        }

        // Modal windows prevents cursor from hovering behind them.
        val modalWindow = frontMostPopupModal
        if (modalWindow != null)
            if (g.hoveredRootWindow?.isChildOf(modalWindow) == false)
                nullate()
        // Disabled mouse?
        if (io.configFlags has ConfigFlag.NoMouse)
            nullate()

        // We track click ownership. When clicked outside of a window the click is owned by the application and won't report hovering nor request capture even while dragging over our windows afterward.
        var mouseEarliestButtonDown = -1
        var mouseAnyDown = false
        for (i in io.mouseDown.indices) {
            if (io.mouseClicked[i])
                io.mouseDownOwned[i] = g.hoveredWindow != null || g.openPopupStack.isNotEmpty()
            mouseAnyDown = mouseAnyDown || io.mouseDown[i]
            if (io.mouseDown[i])
                if (mouseEarliestButtonDown == -1 || io.mouseClickedTime[i] < io.mouseClickedTime[mouseEarliestButtonDown])
                    mouseEarliestButtonDown = i
        }
        val mouseAvailToImgui = mouseEarliestButtonDown == -1 || io.mouseDownOwned[mouseEarliestButtonDown]

        // If mouse was first clicked outside of ImGui bounds we also cancel out hovering.
        // FIXME: For patterns of drag and drop across OS windows, we may need to rework/remove this test (first committed 311c0ca9 on 2015/02)
        val mouseDraggingExternPayload = g.dragDropActive && g.dragDropSourceFlags has Ddf.SourceExtern
        if (!mouseAvailToImgui && !mouseDraggingExternPayload)
            nullate()

        // Update io.WantCaptureMouse for the user application (true = dispatch mouse info to imgui, false = dispatch mouse info to imgui + app)
        if (g.wantCaptureMouseNextFrame != -1)
            io.wantCaptureMouse = g.wantCaptureMouseNextFrame != 0
        else
            io.wantCaptureMouse = (mouseAvailToImgui && (g.hoveredWindow != null || mouseAnyDown)) || g.openPopupStack.isNotEmpty()

        // Update io.WantCaptureKeyboard for the user application (true = dispatch keyboard info to imgui, false = dispatch keyboard info to imgui + app)
        if (g.wantCaptureKeyboardNextFrame != -1)
            io.wantCaptureKeyboard = g.wantCaptureKeyboardNextFrame != 0
        else
            io.wantCaptureKeyboard = g.activeId != 0 || modalWindow != null
        if (io.navActive && io.configFlags has ConfigFlag.NavEnableKeyboard && io.configFlags hasnt ConfigFlag.NavNoCaptureKeyboard)
            io.wantCaptureKeyboard = true

        // Update io.WantTextInput flag, this is to allow systems without a keyboard (e.g. mobile, hand-held) to show a software keyboard if possible
        io.wantTextInput = if (g.wantTextInputNextFrame != -1) g.wantTextInputNextFrame != 0 else false
    }

    fun parseFormatFindStart(fmt: String): Int {
        var i = 0
        var c = fmt[i]
        while (c != NUL) {
            if (c == '%' && fmt[i + 1] != '%')
                return i
            else if (c == '%')
                i++
            c = fmt[++i]
        }
        return i
    }

    fun parseFormatFindEnd(fmt: String, i_: Int = 0): Int {
        var i = i_
        // Printf/scanf types modifiers: I/L/h/j/l/t/w/z. Other uppercase letters qualify as types aka end of the format.
        if (fmt[i] != '%')
            return i
        val ignoredUppercaseMask = (1 shl ('I' - 'A')) or (1 shl ('L' - 'A'))
        val ignoredLowercaseMask = (1 shl ('h' - 'a')) or (1 shl ('j' - 'a')) or (1 shl ('l' - 'a')) or (1 shl ('t' - 'a')) or (1 shl ('w' - 'a')) or (1 shl ('z' - 'a'))
        var c = fmt[i]
        while (c != NUL && i < fmt.length) {
            if (c in 'A'..'Z' && (1 shl (c - 'A')) hasnt ignoredUppercaseMask)
                return i + 1
            if (c in 'a'..'z' && (1 shl (c - 'a')) hasnt ignoredLowercaseMask)
                return i + 1
            c = fmt[++i]
        }
        return i
    }

    /** Extract the format out of a format string with leading or trailing decorations
     *  fmt = "blah blah"  -> return fmt
     *  fmt = "%.3f"       -> return fmt
     *  fmt = "hello %.3f" -> return fmt + 6
     *  fmt = "%.3f hello" -> return buf written with "%.3f" */
    fun parseFormatTrimDecorations(fmt: String, buf: CharArray): String {
        val fmtStart = parseFormatFindStart(fmt)
        if (fmt[fmtStart] != '%')
            return fmt
        val fmtEnd = parseFormatFindEnd(fmt.substring(fmtStart))
        if (fmtStart + fmtEnd >= fmt.length) // If we only have leading decoration, we don't need to copy the data.
            return fmt.substring(fmtStart)
        return String(buf, fmtStart, min((fmtEnd + 1 - fmtStart), buf.size))
    }

    /** Parse display precision back from the display format string
     *  FIXME: This is still used by some navigation code path to infer a minimum tweak step, but we should aim to rework widgets so it isn't needed. */
    fun parseFormatPrecision(fmt: String, defaultPrecision: Int): Int {
        var i = parseFormatFindStart(fmt)
        if (fmt[i] != '%')
            return defaultPrecision
        i++
        while (fmt[i] in '0'..'9')
            i++
        var precision = Int.MAX_VALUE
        if (fmt[i] == '.') {
            val s = fmt.substring(i).filter { it.isDigit() }
            if (s.isNotEmpty()) {
                precision = s.parseInt
                if (precision < 0 || precision > 99)
                    precision = defaultPrecision
            }
        }
        if (fmt[i].toLowerCase() == 'e')    // Maximum precision with scientific notation
            precision = -1
        if (fmt[i].toLowerCase() == 'g' && precision == Int.MAX_VALUE)
            precision = -1
        return when (precision) {
            Int.MAX_VALUE -> defaultPrecision
            else -> precision
        }
    }

    //-----------------------------------------------------------------------------
    // Shade functions (write over already created vertices)
    //-----------------------------------------------------------------------------

    /** Generic linear color gradient, write to RGB fields, leave A untouched.  */
    fun shadeVertsLinearColorGradientKeepAlpha(list: ArrayList<DrawVert>, vertStart: Int, vertEnd: Int, gradientP0: Vec2,
                                               gradientP1: Vec2, col0: Int, col1: Int) {
        val gradientExtent = gradientP1 - gradientP0
        val gradientInvLength2 = 1f / gradientExtent.lengthSqr
        for (i in vertStart until vertEnd) {
            val vert = list[i]
            val d = vert.pos - gradientP0 dot gradientExtent
            val t = glm.clamp(d * gradientInvLength2, 0f, 1f)
            val r = lerp((col0 ushr COL32_R_SHIFT) and 0xFF, (col1 ushr COL32_R_SHIFT) and 0xFF, t)
            val g = lerp((col0 ushr COL32_G_SHIFT) and 0xFF, (col1 ushr COL32_G_SHIFT) and 0xFF, t)
            val b = lerp((col0 ushr COL32_B_SHIFT) and 0xFF, (col1 ushr COL32_B_SHIFT) and 0xFF, t)
            vert.col = (r shl COL32_R_SHIFT) or (g shl COL32_G_SHIFT) or (b shl COL32_B_SHIFT) or (vert.col and COL32_A_MASK)
        }
    }

    /** Scan and shade backward from the end of given vertices. Assume vertices are text only (= vert_start..vert_end
     *  going left to right) so we can break as soon as we are out the gradient bounds. */
    fun shadeVertsLinearAlphaGradientForLeftToRightText(drawList: DrawList, vertStart: Int, vertEnd: Int,
                                                        gradientP0x: Float, gradientP1x: Float) {
        val gradientExtentX = gradientP1x - gradientP0x
        val gradientInvLength2 = 1f / (gradientExtentX * gradientExtentX)
        var fullAlphaCount = 0
        for (i in vertEnd - 1 downTo vertStart) {
            val vert = drawList.vtxBuffer[i]
            val d = (vert.pos.x - gradientP0x) * gradientExtentX
            val alphaMul = 1f - glm.clamp(d * gradientInvLength2, 0f, 1f)
            ++fullAlphaCount
            if (alphaMul >= 1f && fullAlphaCount > 2) return // Early out
            val a = (((vert.col ushr COL32_A_SHIFT) and 0xFF) * alphaMul).i
            vert.col = (vert.col wo COL32_A_MASK) or (a shl COL32_A_SHIFT)
        }
    }

    /** Distribute UV over (a, b) rectangle */
    fun shadeVertsLinearUV(list: ArrayList<DrawVert>, vertStart: Int, vertEnd: Int, a: Vec2, b: Vec2, uvA: Vec2, uvB: Vec2, clamp: Boolean) {
        val size = b - a
        val uvSize = uvB - uvA
        val scale = Vec2(
                if (size.x != 0f) uvSize.x / size.x else 0f,
                if (size.y != 0f) uvSize.y / size.y else 0f)
        if (clamp) {
            val min = uvA min uvB
            val max = uvA max uvB

            for (i in vertStart until vertEnd) {
                val vertex = list[i]
                vertex.uv = glm.clamp(uvA + (vertex.pos - a) * scale, min, max)
            }
        } else {
            for (i in vertStart until vertEnd) {
                val vertex = list[i]
                vertex.uv = uvA + (vertex.pos - a) * scale
            }
        }
    }

    companion object {

        fun alphaBlendColor(colA: Int, colB: Int): Int {
            val t = ((colB ushr COL32_A_SHIFT) and 0xFF) / 255f
            val r = lerp((colA ushr COL32_R_SHIFT) and 0xFF, (colB ushr COL32_R_SHIFT) and 0xFF, t)
            val g = lerp((colA ushr COL32_G_SHIFT) and 0xFF, (colB ushr COL32_G_SHIFT) and 0xFF, t)
            val b = lerp((colA ushr COL32_B_SHIFT) and 0xFF, (colB ushr COL32_B_SHIFT) and 0xFF, t)
            return COL32(r, g, b, 0xFF)
        }

        fun getMinimumStepAtDecimalPrecision(decimalPrecision: Int): Float {
            val minSteps = floatArrayOf(1f, 0.1f, 0.01f, 0.001f, 0.0001f, 0.00001f, 0.000001f, 0.0000001f, 0.00000001f, 0.000000001f)
            return when {
                decimalPrecision < 0 -> Float.MIN_VALUE
                decimalPrecision in 0..9 -> minSteps[decimalPrecision]
                else -> glm.pow(10f, -decimalPrecision.f)
            }
        }

        fun acos01(x: Float) = when {
            x <= 0f -> glm.PIf * 0.5f
            x >= 1f -> 0f
            else -> glm.acos(x)
        //return (-0.69813170079773212f * x * x - 0.87266462599716477f) * x + 1.5707963267948966f; // Cheap approximation, may be enough for what we do.
        }

        fun roundScalarWithFormat(format: String, value: Int): Int {
            val fmtStart = parseFormatFindStart(format)
            if (format[fmtStart] != '%' || format[fmtStart + 1] == '%') // Don't apply if the value is not visible in the format string
                return value
            val vStr = format.substring(parseFormatFindStart(format)).format(style.locale, value)
            return vStr.trimStart().i
        }

        fun roundScalarWithFormat(format: String, value: Long): Long {
            val fmtStart = parseFormatFindStart(format)
            if (format[fmtStart] != '%' || format[fmtStart + 1] == '%') // Don't apply if the value is not visible in the format string
                return value
            val vStr = format.substring(parseFormatFindStart(format)).format(style.locale, value)
            return vStr.trimStart().L
        }

        fun roundScalarWithFormat(format: String, value: Float): Float {
            val fmtStart = parseFormatFindStart(format)
            if (format[fmtStart] != '%' || format[fmtStart + 1] == '%') // Don't apply if the value is not visible in the format string
                return value
            val vStr = format.substring(parseFormatFindStart(format)).format(style.locale, value)
            return vStr.trimStart().f
        }

        fun roundScalarWithFormat(format: String, value: Double): Double {
            val fmtStart = parseFormatFindStart(format)
            if (format[fmtStart] != '%' || format[fmtStart + 1] == '%') // Don't apply if the value is not visible in the format string
                return value
            val vStr = format.substring(parseFormatFindStart(format)).format(style.locale, value)
            return vStr.trimStart().d
        }
    }
}

inline fun <R> withFloat(floats: FloatArray, ptr: Int, block: (KMutableProperty0<Float>) -> R): R {
    Ref.fPtr++
    val f = Ref::float
    f.set(floats[ptr])
    val res = block(f)
    floats[ptr] = f()
    Ref.fPtr--
    return res
}

inline fun <R> withInt(ints: IntArray, ptr: Int, block: (KMutableProperty0<Int>) -> R): R {
    Ref.iPtr++
    val i = Ref::int
    i.set(ints[ptr])
    val res = block(i)
    ints[ptr] = i()
    Ref.iPtr--
    return res
}

private inline fun <R> withInt(block: (KMutableProperty0<Int>) -> R): R {
    Ref.iPtr++
    return block(Ref::int).also { Ref.iPtr-- }
}

private inline fun <R> withChar(block: (KMutableProperty0<Char>) -> R): R {
    Ref.cPtr++
    return block(Ref::char).also { Ref.cPtr-- }
}