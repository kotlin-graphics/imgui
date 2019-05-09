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
import imgui.ImGui.calcTextSize
import imgui.ImGui.clipboardText
import imgui.ImGui.colorButton
import imgui.ImGui.dummy
import imgui.ImGui.endChildFrame
import imgui.ImGui.endGroup
import imgui.ImGui.endPopup
import imgui.ImGui.endTooltip
import imgui.ImGui.frameHeight
import imgui.ImGui.getColorU32
import imgui.ImGui.getColumnOffset
import imgui.ImGui.getColumnWidth
import imgui.ImGui.indent
import imgui.ImGui.io
import imgui.ImGui.isItemActive
import imgui.ImGui.isItemHovered
import imgui.ImGui.isMouseClicked
import imgui.ImGui.isMouseDragging
import imgui.ImGui.isMouseHoveringRect
import imgui.ImGui.isMousePosValid
import imgui.ImGui.logText
import imgui.ImGui.nextItemWidth
import imgui.ImGui.openPopup
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
import imgui.ImGui.setColumnOffset
import imgui.ImGui.setItemAllowOverlap
import imgui.ImGui.setNextWindowBgAlpha
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setTooltip
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textLineHeight
import imgui.TextEditState.K
import imgui.imgui.imgui_colums.Companion.COLUMNS_HIT_RECT_HALF_WIDTH
import imgui.imgui.widgets.*
import imgui.internal.*
import kool.lib.fill
import uno.kotlin.getValue
import uno.kotlin.setValue
import unsigned.Ubyte
import unsigned.Uint
import unsigned.Ulong
import unsigned.Ushort
import java.nio.ByteBuffer
import java.util.*
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.reflect.KMutableProperty0
import glm_.pow as _
import imgui.ColorEditFlag as Cef
import imgui.DragDropFlag as Ddf
import imgui.HoveredFlag as Hf
import imgui.InputTextFlag as Itf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf
import imgui.internal.ButtonFlag as Bf
import imgui.internal.ColumnsFlag as Cf
import imgui.internal.DrawCornerFlag as Dcf
import imgui.internal.ItemFlag as If
import imgui.internal.LayoutType as Lt


@Suppress("UNCHECKED_CAST")


interface imgui_internal {

    val currentWindowRead: Window?
        get() = g.currentWindow

    val currentWindow: Window
        get() = g.currentWindow?.apply { writeAccessed = true } ?: throw Error(
                "We should always have a CurrentWindow in the stack (there is an implicit \"Debug\" window)\n" +
                        "If this ever crash because ::currentWindow is NULL it means that either\n" +
                        "   - ::newFrame() has never been called, which is illegal.\n" +
                        "   - You are calling ImGui functions after ::render() and before the next ::newFrame(), which is also illegal.\n" +
                        "   - You are calling ImGui functions after ::endFrame()/::render() and before the next ImGui::newFrame(), which is also illegal.")

    fun findWindowByID(id: ID): Window? = g.windowsById[id]

    fun findWindowByName(name: String): Window? = g.windowsById[hash(name)]

    fun setCurrentFont(font: Font) {
        assert(font.isLoaded) { "Font Atlas not created. Did you call io.Fonts->GetTexDataAsRGBA32 / GetTexDataAsAlpha8 ?" }
        assert(font.scale > 0f)
        g.font = font
        g.fontBaseSize = 1f max (io.fontGlobalScale * g.font.fontSize * g.font.scale)
        g.fontSize = g.currentWindow?.calcFontSize() ?: 0f

        val atlas = g.font.containerAtlas
        g.drawListSharedData.texUvWhitePixel = atlas.texUvWhitePixel
        g.drawListSharedData.font = g.font
        g.drawListSharedData.fontSize = g.fontSize
    }

    val defaultFont: Font
        get() = io.fontDefault ?: io.fonts.fonts[0]

    fun markIniSettingsDirty() {
        if (g.settingsDirtyTimer <= 0f) g.settingsDirtyTimer = io.iniSavingRate
    }


    // Basic Accessors


    val itemID: ID
        get() = g.currentWindow!!.dc.lastItemId

    val activeID: ID
        get() = g.activeId

    val focusID: ID
        get() = g.navId

    fun setActiveId(id: ID, window: Window?) {
        g.activeIdIsJustActivated = g.activeId != id
        if (g.activeIdIsJustActivated) {
            g.activeIdTimer = 0f
            g.activeIdHasBeenPressed = false
            g.activeIdHasBeenEdited = false
            if (id != 0) {
                g.lastActiveId = id
                g.lastActiveIdTimer = 0f
            }
        }
        g.activeId = id
        g.activeIdAllowNavDirFlags = 0
        g.activeIdBlockNavInputFlags = 0
        g.activeIdAllowOverlap = false
        g.activeIdWindow = window
        if (id != 0) {
            g.activeIdIsAlive = id
            g.activeIdSource = when (id) {
                g.navActivateId, g.navInputId, g.navJustTabbedId, g.navJustMovedToId -> InputSource.Nav
                else -> InputSource.Mouse
            }
        }
    }

    /** FIXME-NAV: The existence of SetNavID/SetNavIDWithRectRel/SetFocusID is incredibly messy and confusing and needs some explanation or refactoring. */
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
        window.navLastIds[navLayer.i] = id
        if (window.dc.lastItemId == id)
            window.navRectRel[navLayer.i].put(window.dc.lastItemRect.min - window.pos, window.dc.lastItemRect.max - window.pos)

        if (g.activeIdSource == InputSource.Nav)
            g.navDisableMouseHover = true
        else
            g.navDisableHighlight = true
    }

    fun clearActiveId() = setActiveId(0, null)

    var hoveredId: ID
        get() = if (g.hoveredId != 0) g.hoveredId else g.hoveredIdPreviousFrame
        set(value) {
            g.hoveredId = value
            g.hoveredIdAllowOverlap = false
            if (value != 0 && g.hoveredIdPreviousFrame != value) {
                g.hoveredIdTimer = 0f
                g.hoveredIdNotActiveTimer = 0f
            }
        }

    fun keepAliveId(id: ID) {
        if (g.activeId == id)
            g.activeIdIsAlive = id
        if (g.activeIdPreviousFrame == id)
            g.activeIdPreviousFrameIsAlive = true
    }

    fun markItemEdited(id: ID) {
        /*  This marking is solely to be able to provide info for ::isItemDeactivatedAfterEdit().
            ActiveId might have been released by the time we call this (as in the typical press/release button behavior)
            but still need need to fill the data.         */
        assert(g.activeId == id || g.activeId == 0 || g.dragDropActive)
        //IM_ASSERT(g.CurrentWindow->DC.LastItemId == id)
        g.activeIdHasBeenEdited = true
        g.currentWindow!!.dc.apply { lastItemStatusFlags = lastItemStatusFlags or ItemStatusFlag.Edited }
    }

    /** Push a given id value ignoring the ID stack as a seed. */
    fun pushOverrideID(id: ID) {
        g.currentWindow!!.idStack.push(id)
    }


    // Basic Helpers for widget code


    /** Advance cursor given item size for layout.  */
    fun itemSize(size: Vec2, textOffsetY: Float = 0f) {

        val window = currentWindow
        if (window.skipItems) return

        // Always align ourselves on pixel boundaries
        val lineHeight = glm.max(window.dc.currentLineSize.y, size.y)
        val textBaseOffset = glm.max(window.dc.currentLineTextBaseOffset, textOffsetY)
        window.dc.apply {
            //if (io.keyAlt) window.drawList.addRect(window.dc.cursorPos, window.dc.cursorPos + Vec2(size.x, lineHeight), COL32(255,0,0,200)); // [DEBUG]
            cursorPosPrevLine.put(cursorPos.x + size.x, cursorPos.y)
            cursorPos.x = (window.pos.x + indent + columnsOffset).i.f
            cursorPos.y = (cursorPos.y + lineHeight + style.itemSpacing.y).i.f
            cursorMaxPos.x = cursorMaxPos.x max cursorPosPrevLine.x
            cursorMaxPos.y = cursorMaxPos.y max (cursorPos.y - style.itemSpacing.y)

            //if (io.keyAlt) window.drawList.addCircle(window.dc.cursorMaxPos, 3f, COL32(255,0,0,255), 4); // [DEBUG]

            prevLineSize.y = lineHeight
            prevLineTextBaseOffset = textBaseOffset
            currentLineTextBaseOffset = 0f
            currentLineSize.y = 0f

            // Horizontal layout mode
            if (layoutType == Lt.Horizontal) sameLine()
        }
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
            lastItemStatusFlags = ItemStatusFlag.None.i
        }

        if (IMGUI_ENABLE_TEST_ENGINE && id != 0)
            ImGuiTestEngineHook_ItemAdd(navBbArg ?: bb, id)

        // Clipping test
        if (isClippedEx(bb, id, false)) return false
        //if (g.io.KeyAlt) window->DrawList->AddRect(bb.Min, bb.Max, IM_COL32(255,255,0,120)); // [DEBUG]

        // We need to calculate this now to take account of the current clipping rectangle (as items like Selectable may change them)
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
            g.navDisableMouseHover || !window.isContentHoverable(Hf.None) -> false
            window.dc.itemFlags has If.Disabled -> false
            else -> {
                hoveredId = id
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

    /** Return true if focus is requested
     *  Process TAB/Shift+TAB. Be mindful that this function may _clear_ the ActiveID when tabbing out. */
    // TODO move both methods into Window class?
    fun focusableItemRegister(window: Window, id: ID): Boolean {

        // Increment counters
        val isTabStop = window.dc.itemFlags hasnt (If.NoTabStop or If.Disabled)
        window.dc.focusCounterAll++
        if (isTabStop)
            window.dc.focusCounterTab++

        // Process TAB/Shift-TAB to tab *OUT* of the currently focused item.
        // (Note that we can always TAB out of a widget that doesn't allow tabbing in)
        if (g.activeId == id && g.focusTabPressed && g.activeIdBlockNavInputFlags hasnt (1 shl NavInput.KeyTab.i) && g.focusRequestNextWindow == null) {
            g.focusRequestNextWindow = window
            g.focusRequestNextCounterTab = window.dc.focusCounterTab + when {
                // Modulo on index will be applied at the end of frame once we've got the total counter of items.
                io.keyShift -> if (isTabStop) -1 else 0
                else -> +1
            }
        }
        // Handle focus requests
        if (g.focusRequestCurrWindow === window) {
            if (window.dc.focusCounterAll == g.focusRequestCurrCounterAll)
                return true
            if (isTabStop && window.dc.focusCounterTab == g.focusRequestCurrCounterTab) {
                g.navJustTabbedId = id
                return true
            }

            // If another item is about to be focused, we clear our own active id
            if (g.activeId == id)
                clearActiveId()
        }

        return false
    }

    fun focusableItemUnregister(window: Window) {
        window.dc.focusCounterAll--
        window.dc.focusCounterTab--
    }

    /** [Internal] Calculate full item size given user provided 'size' parameter and default width/height. Default width is often == CalcItemWidth().
     *  Those two functions CalcItemWidth vs CalcItemSize are awkwardly named because they are not fully symmetrical.
     *  Note that only CalcItemWidth() is publicly exposed.
     *  The 4.0f here may be changed to match GetNextItemWidth() and/or BeginChild() (right now we have a mismatch which is harmless but undesirable) */
    fun calcItemSize(size: Vec2, defaultW: Float, defaultH: Float): Vec2 {
        val window = g.currentWindow!!

        val regionMax = if (size anyLessThan 0f) contentRegionMaxScreen else Vec2()

        if (size.x == 0f)
            size.x = defaultW
        else if (size.x < 0f)
            size.x = 4f max (regionMax.x - window.dc.cursorPos.x + size.x)

        if (size.y == 0f)
            size.y = defaultH
        else if (size.y < 0f)
            size.y = 4f max (regionMax.y - window.dc.cursorPos.y + size.y)

        return size
    }

    fun calcWrapWidthForPos(pos: Vec2, wrapPosX_: Float): Float {

        if (wrapPosX_ < 0f) return 0f

        val window = g.currentWindow!!
        var wrapPosX = wrapPosX_
        if (wrapPosX == 0f)
            wrapPosX = contentRegionMaxScreen.x
        else if (wrapPosX > 0f)
            wrapPosX += window.pos.x - window.scroll.x // wrap_pos_x is provided is window local space

        return glm.max(wrapPosX - pos.x, 1f)
    }

    fun pushMultiItemsWidths(components: Int, wFull: Float) {

        val window = ImGui.currentWindow
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

    /** was the last item selection toggled? (after Selectable(), TreeNode() etc. We only returns toggle _event_ in order to handle clipping correctly) */
    fun isItemToggledSelection() = g.currentWindow!!.dc.lastItemStatusFlags has ItemStatusFlag.ToggledSelection

    /** [Internal] Absolute coordinate. Saner. This is not exposed until we finishing refactoring work rect features. */
    val contentRegionMaxScreen: Vec2
        get() {
            val window = g.currentWindow!!
            val mx = Vec2(window.contentsRegionRect.max)
            window.dc.currentColumns?.let {
                mx.x = window.pos.x + getColumnOffset(it.current + 1) - window.windowPadding.x
            }
            return mx
        }

    // Logging/Capture

    /** -> BeginCapture() when we design v2 api, for now stay under the radar by using the old name. */
    /** Start logging/capturing text output */
    fun logBegin(type: LogType, autoOpenDepth: Int) {

        val window = g.currentWindow!!

        assert(!g.logEnabled && g.logFile == null && g.logBuffer.isEmpty())
        g.logEnabled = true
        g.logType = type
        g.logDepthRef = window.dc.treeDepth
        g.logDepthToExpand = autoOpenDepth.takeIf { it >= 0 } ?: g.logDepthToExpandDefault
        g.logLinePosY = Float.MAX_VALUE
        g.logLineFirstItem = true
    }

    /** Start logging/capturing to internal buffer */
    fun logToBuffer(autoOpenDepth: Int = -1): Nothing = TODO()

    // Popups, Modals, Tooltips


    /** Mark popup as open (toggle toward open state).
     *  Popups are closed when user click outside, or activate a pressable item, or CloseCurrentPopup() is called within
     *  a BeginPopup()/EndPopup() block.
     *  Popup identifiers are relative to the current ID-stack (so OpenPopup and BeginPopup needs to be at the same
     *  level).
     *  One open popup per level of the popup hierarchy (NB: when assigning we reset the Window member of ImGuiPopupRef
     *  to NULL)    */
    fun openPopupEx(id: ID) {

        val parentWindow = g.currentWindow!!
        val currentStackSize = g.beginPopupStack.size
        // Tagged as new ref as Window will be set back to NULL if we write this into OpenPopupStack.
        val openPopupPos = navCalcPreferredRefPos()
        val popupRef = PopupData(popupId = id, window = null, parentWindow = parentWindow, openFrameCount = g.frameCount,
                openParentId = parentWindow.idStack.last(), openPopupPos = openPopupPos,
                openMousePos = if (isMousePosValid(io.mousePos)) Vec2(io.mousePos) else Vec2(openPopupPos))
//        println("" + g.openPopupStack.size +", "+currentStackSize)
        if (g.openPopupStack.size < currentStackSize + 1)
            g.openPopupStack += popupRef
        else {
            /*  Gently handle the user mistakenly calling OpenPopup() every frame. It is a programming mistake!
                However, if we were to run the regular code path, the ui would become completely unusable because
                the popup will always be in hidden-while-calculating-size state _while_ claiming focus.
                Which would be a very confusing situation for the programmer. Instead, we silently allow the popup
                to proceed, it will keep reappearing and the programming error will be more obvious to understand.  */
            if (g.openPopupStack[currentStackSize].popupId == id && g.openPopupStack[currentStackSize].openFrameCount == g.frameCount - 1)
                g.openPopupStack[currentStackSize].openFrameCount = popupRef.openFrameCount
            else {
                // Close child popups if any
                if (g.openPopupStack.size > currentStackSize + 1) // ~resize
                    for (i in currentStackSize + 1 until g.openPopupStack.size)
                        g.openPopupStack.pop()
                else if (g.openPopupStack.size < currentStackSize + 1)
                    TODO()
                g.openPopupStack[currentStackSize] = popupRef
            }
            /*  When reopening a popup we first refocus its parent, otherwise if its parent is itself a popup
                it would get closed by closePopupsOverWindow().  This is equivalent to what ClosePopupToLevel() does. */
            //if (g.openPopupStack[currentStackSize].popupId == id) parentWindow.focus()
        }
    }

    fun closePopupToLevel(remaining: Int, applyFocusToWindowUnder: Boolean) {
        assert(remaining >= 0)
        var focusWindow = when {
            remaining > 0 -> g.openPopupStack[remaining - 1].window
            else -> g.openPopupStack[0].parentWindow
        }!!
        for (i in remaining until g.openPopupStack.size) // resize(remaining)
            g.openPopupStack.pop()

        /*  FIXME: This code is faulty and we may want to eventually to replace or remove the 'apply_focus_to_window_under=true' path completely.
            Instead of using g.OpenPopupStack[remaining-1].Window etc. we should find the highest root window that is behind the popups we are closing.
            The current code will set focus to the parent of the popup window which is incorrect.
            It rarely manifested until now because UpdateMouseMovingWindowNewFrame() would call FocusWindow() again on the clicked window,
            leading to a chain of focusing A (clicked window) then B (parent window of the popup) then A again.
            However if the clicked window has the _NoMove flag set we would be left with B focused.
            For now, we have disabled this path when called from ClosePopupsOverWindow() because the users of ClosePopupsOverWindow() don't need to alter focus anyway,
            but we should inspect and fix this properly. */
        if (applyFocusToWindowUnder) {
            if (g.navLayer == NavLayer.Main)
                focusWindow = navRestoreLastChildNavWindow(focusWindow)
            focusWindow.focus()
        }
    }

    fun closePopupsOverWindow(refWindow: Window?) {

        if (g.openPopupStack.empty())
            return

        /*  When popups are stacked, clicking on a lower level popups puts focus back to it and close popups above it.
            Don't close our own child popup windows */
        var popupCountToKeep = 0
        if (refWindow != null)
        // Find the highest popup which is a descendant of the reference window (generally reference window = NavWindow)
            while (popupCountToKeep < g.openPopupStack.size) {
                val popup = g.openPopupStack[popupCountToKeep]
                if (popup.window == null) {
                    popupCountToKeep++
                    continue
                }
                assert(popup.window!!.flags has Wf.Popup)
                if (popup.window!!.flags has Wf.ChildWindow) {
                    popupCountToKeep++
                    continue
                }
                // Trim the stack if popups are not direct descendant of the reference window (which is often the NavWindow)
                var popupOrDescendentHasFocus = false
                var m = popupCountToKeep
                while (m < g.openPopupStack.size && !popupOrDescendentHasFocus) {
                    g.openPopupStack[m].window?.let {
                        if (it.rootWindow === refWindow.rootWindow)
                            popupOrDescendentHasFocus = true
                    }
                    m++
                }
                if (!popupOrDescendentHasFocus) break
                popupCountToKeep++
            }

        if (popupCountToKeep < g.openPopupStack.size) { // This test is not required but it allows to set a convenient breakpoint on the statement below
            //IMGUI_DEBUG_LOG("ClosePopupsOverWindow(%s) -> ClosePopupToLevel(%d)\n", ref_window->Name, popup_count_to_keep);
            closePopupToLevel(popupCountToKeep, false)
        }
    }

    /** return true if the popup is open at the current begin-ed level of the popup stack.
     *  Test for id within current popup stack level (currently begin-ed into); this doesn't scan the whole popup stack! */
    fun isPopupOpen(id: ID) = g.openPopupStack.size > g.beginPopupStack.size && g.openPopupStack[g.beginPopupStack.size].popupId == id

    fun beginPopupEx(id: ID, extraFlags: WindowFlags): Boolean {

        if (!isPopupOpen(id)) {
            g.nextWindowData.clear() // We behave like Begin() and need to consume those values
            return false
        }

        val name = when {
            extraFlags has Wf.ChildMenu -> "##Menu_%02d".format(style.locale, g.beginPopupStack.size)    // Recycle windows based on depth
            else -> "##Popup_%08x".format(style.locale, id)     // Not recycling, so we can close/open during the same frame
        }
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
                    it.hidden = true
                    it.hiddenFramesCanSkipItems = 1
                    windowName = "##Tooltip_%02d".format(++g.tooltipOverrideCount)
                }
            }
        val flags = Wf.Tooltip or Wf.NoMouseInputs or Wf.NoTitleBar or Wf.NoMove or Wf.NoResize or Wf.NoSavedSettings or Wf.AlwaysAutoResize
        begin(windowName, null, flags or extraFlags)
    }

    val frontMostPopupModal: Window?
        get() {
            for (n in g.openPopupStack.size - 1 downTo 0)
                g.openPopupStack[n].window?.let { if (it.flags has Wf.Modal) return it }
            return null
        }

    fun findBestWindowPosForPopup(window: Window): Vec2 {

        val rOuter = window.getAllowedExtentRect()
        if (window.flags has Wf.ChildMenu) {
            /*  Child menus typically request _any_ position within the parent menu item,
                and then we move the new menu outside the parent bounds.
                This is how we end up with child menus appearing (most-commonly) on the right of the parent menu. */
            assert(g.currentWindow === window)
            val parentWindow = g.currentWindowStack[g.currentWindowStack.size - 2]
            // We want some overlap to convey the relative depth of each menu (currently the amount of overlap is hard-coded to style.ItemSpacing.x).
            val horizontalOverlap = style.itemInnerSpacing.x
            val rAvoid = parentWindow.run {
                when {
                    dc.menuBarAppending -> Rect(-Float.MAX_VALUE, pos.y + titleBarHeight, Float.MAX_VALUE, pos.y + titleBarHeight + menuBarHeight)
                    else -> Rect(pos.x + horizontalOverlap, -Float.MAX_VALUE, pos.x + size.x - horizontalOverlap - scrollbarSizes.x, Float.MAX_VALUE)
                }
            }
            return findBestWindowPosForPopupEx(Vec2(window.pos), window.size, window::autoPosLastDirection, rOuter, rAvoid)
        }
        if (window.flags has Wf.Popup) {
            val rAvoid = Rect(window.pos.x - 1, window.pos.y - 1, window.pos.x + 1, window.pos.y + 1)
            return findBestWindowPosForPopupEx(Vec2(window.pos), window.size, window::autoPosLastDirection, rOuter, rAvoid)
        }
        if (window.flags has Wf.Tooltip) {
            // Position tooltip (always follows mouse)
            val sc = style.mouseCursorScale
            val refPos = navCalcPreferredRefPos()
            val rAvoid = when {
                !g.navDisableHighlight && g.navDisableMouseHover && !(io.configFlags has ConfigFlag.NavEnableSetMousePos) ->
                    Rect(refPos.x - 16, refPos.y - 8, refPos.x + 16, refPos.y + 8)
                else -> Rect(refPos.x - 16, refPos.y - 8, refPos.x + 24 * sc, refPos.y + 24 * sc) // FIXME: Hard-coded based on mouse cursor shape expectation. Exact dimension not very important.
            }
            val pos = findBestWindowPosForPopupEx(refPos, window.size, window::autoPosLastDirection, rOuter, rAvoid)
            if (window.autoPosLastDirection == Dir.None)
            // If there's not enough room, for tooltip we prefer avoiding the cursor at all cost even if it means that part of the tooltip won't be visible.
                pos(refPos + 2)
            return pos
        }
        assert(false)
        return Vec2(window.pos)
    }

    /** rAvoid = the rectangle to avoid (e.g. for tooltip it is a rectangle around the mouse cursor which we want to avoid. for popups it's a small point around the cursor.)
     *  rOuter = the visible area rectangle, minus safe area padding. If our popup size won't fit because of safe area padding we ignore it. */
    fun findBestWindowPosForPopupEx(refPos: Vec2, size: Vec2, lastDirPtr: KMutableProperty0<Dir>, rOuter: Rect, rAvoid: Rect,
                                    policy: PopupPositionPolicy = PopupPositionPolicy.Default): Vec2 {

        var lastDir by lastDirPtr
        val basePosClamped = glm.clamp(refPos, rOuter.min, rOuter.max - size)
        //GImGui->OverlayDrawList.AddRect(r_avoid.Min, r_avoid.Max, IM_COL32(255,0,0,255));
        //GImGui->OverlayDrawList.AddRect(rOuter.Min, rOuter.Max, IM_COL32(0,255,0,255));

        // Combo Box policy (we want a connecting edge)
        if (policy == PopupPositionPolicy.ComboBox) {
            val dirPreferedOrder = arrayOf(Dir.Down, Dir.Right, Dir.Left, Dir.Up)
            for (n in (if (lastDir != Dir.None) -1 else 0) until Dir.COUNT) {
                val dir = if (n == -1) lastDir else dirPreferedOrder[n]
                if (n != -1 && dir == lastDir) continue // Already tried this direction?
                val pos = Vec2()
                if (dir == Dir.Down) pos.put(rAvoid.min.x, rAvoid.max.y)          // Below, Toward Right (default)
                if (dir == Dir.Right) pos.put(rAvoid.min.x, rAvoid.min.y - size.y) // Above, Toward Right
                if (dir == Dir.Left) pos.put(rAvoid.max.x - size.x, rAvoid.max.y) // Below, Toward Left
                if (dir == Dir.Up) pos.put(rAvoid.max.x - size.x, rAvoid.min.y - size.y) // Above, Toward Left
                if (Rect(pos, pos + size) !in rOuter) continue
                lastDir = dir
                return pos
            }
        }

        // Default popup policy
        val dirPreferedOrder = arrayOf(Dir.Right, Dir.Down, Dir.Up, Dir.Left)
        for (n in (if (lastDir != Dir.None) -1 else 0) until Dir.COUNT) {
            val dir = if (n == -1) lastDir else dirPreferedOrder[n]
            if (n != -1 && dir == lastDir) continue  // Already tried this direction?
            val availW = (if (dir == Dir.Left) rAvoid.min.x else rOuter.max.x) - if (dir == Dir.Right) rAvoid.max.x else rOuter.min.x
            val availH = (if (dir == Dir.Up) rAvoid.min.y else rOuter.max.y) - if (dir == Dir.Down) rAvoid.max.y else rOuter.min.y
            if (availW < size.x || availH < size.y) continue
            val pos = Vec2(
                    if (dir == Dir.Left) rAvoid.min.x - size.x else if (dir == Dir.Right) rAvoid.max.x else basePosClamped.x,
                    if (dir == Dir.Up) rAvoid.min.y - size.y else if (dir == Dir.Down) rAvoid.max.y else basePosClamped.y)
            lastDir = dir
            return pos
        }
        // Fallback, try to keep within display
        lastDir = Dir.None
        return Vec2(refPos).apply {
            x = max(min(x + size.x, rOuter.max.x) - size.x, rOuter.min.x)
            y = max(min(y + size.y, rOuter.max.y) - size.y, rOuter.min.y)
        }
    }


    // Navigation


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

    fun navMoveRequestButNoResultYet(): Boolean = g.navMoveRequest && g.navMoveResultLocal.id == 0 && g.navMoveResultOther.id == 0

    fun navMoveRequestCancel() {
        g.navMoveRequest = false
        navUpdateAnyRequestFlag()
    }

    fun navMoveRequestForward(moveDir: Dir, clipDir: Dir, bbRel: Rect, moveFlags: NavMoveFlags) {

        assert(g.navMoveRequestForward == NavForward.None)
        navMoveRequestCancel()
        g.navMoveDir = moveDir
        g.navMoveDir = clipDir
        g.navMoveRequestForward = NavForward.ForwardQueued
        g.navMoveRequestFlags = moveFlags
        g.navWindow!!.navRectRel[g.navLayer.i] = bbRel
    }

    fun navMoveRequestTryWrapping(window: Window, moveFlags: NavMoveFlags) {

        if (g.navWindow !== window || !navMoveRequestButNoResultYet() || g.navMoveRequestForward != NavForward.None || g.navLayer != NavLayer.Main)
            return
        assert(moveFlags != 0) // No points calling this with no wrapping
        val bbRel = window.navRectRel[0]

        var clipDir = g.navMoveDir
        if (g.navMoveDir == Dir.Left && moveFlags has (NavMoveFlag.WrapX or NavMoveFlag.LoopX)) {
            bbRel.min.x = max(window.sizeFull.x, window.sizeContents.x) - window.scroll.x
            bbRel.max.x = bbRel.min.x
            if (moveFlags has NavMoveFlag.WrapX) {
                bbRel translateY -bbRel.height
                clipDir = Dir.Up
            }
            navMoveRequestForward(g.navMoveDir, clipDir, bbRel, moveFlags)
        }
        if (g.navMoveDir == Dir.Right && moveFlags has (NavMoveFlag.WrapX or NavMoveFlag.LoopX)) {
            bbRel.min.x = -window.scroll.x
            bbRel.max.x = bbRel.min.x
            if (moveFlags has NavMoveFlag.WrapX) {
                bbRel translateY +bbRel.height
                clipDir = Dir.Down
            }
            navMoveRequestForward(g.navMoveDir, clipDir, bbRel, moveFlags)
        }
        if (g.navMoveDir == Dir.Up && moveFlags has (NavMoveFlag.WrapY or NavMoveFlag.LoopY)) {
            bbRel.min.y = max(window.sizeFull.y, window.sizeContents.y) - window.scroll.y
            bbRel.max.y = max(window.sizeFull.y, window.sizeContents.y) - window.scroll.y
            if (moveFlags has NavMoveFlag.WrapY) {
                bbRel translateX -bbRel.width
                clipDir = Dir.Left
            }
            navMoveRequestForward(g.navMoveDir, clipDir, bbRel, moveFlags)
        }
        if (g.navMoveDir == Dir.Down && moveFlags has (NavMoveFlag.WrapY or NavMoveFlag.LoopY)) {
            bbRel.min.y = -window.scroll.y
            bbRel.max.y = bbRel.min.y
            if (moveFlags has NavMoveFlag.WrapY) {
                bbRel translateX +bbRel.width
                clipDir = Dir.Right
            }
            navMoveRequestForward(g.navMoveDir, clipDir, bbRel, moveFlags)
        }
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

    /** Remotely activate a button, checkbox, tree node etc. given its unique ID. activation is queued and processed
     *  on the next frame when the item is encountered again.  */
    fun activateItem(id: ID) {
        g.navNextActivateId = id
    }

    fun setNavId(id: ID, navLayer: NavLayer) {
        // assert(navLayer == 0 || navLayer == 1) useless on jvm
        g.navId = id
        g.navWindow!!.navLastIds[navLayer.i] = id
    }

    fun setNavIDWithRectRel(id: ID, navLayer: NavLayer, rectRel: Rect) {
        setNavId(id, navLayer)
        g.navWindow!!.navRectRel[navLayer.i] put rectRel
        g.navMousePosDirty = true
        g.navDisableHighlight = false
        g.navDisableMouseHover = true
    }

    fun beginDragDropTargetCustom(bb: Rect, id: ID): Boolean {
        if (!g.dragDropActive) return false

        val window = g.currentWindow!!
        g.hoveredWindow.let { if (it == null || window.rootWindow != it.rootWindow) return false }
        assert(id != 0)
        if (!isMouseHoveringRect(bb.min, bb.max) || id == g.dragDropPayload.sourceId)
            return false
        if (window.skipItems) return false

        g.dragDropTargetRect put bb
        g.dragDropTargetId = id
        return true
    }

    fun clearDragDrop() = with(g) {
        dragDropActive = false
        dragDropPayload.clear()
        dragDropAcceptFlags = Ddf.None.i
        dragDropAcceptIdPrev = 0
        dragDropAcceptIdCurr = 0
        dragDropAcceptIdCurrRectSurface = Float.MAX_VALUE
        dragDropAcceptFrameCount = -1

        g.dragDropPayloadBufHeap = ByteBuffer.allocate(0)
        g.dragDropPayloadBufLocal.fill(0)
    }

    val isDragDropPayloadBeingAccepted get() = g.dragDropActive && g.dragDropAcceptIdPrev != 0

    fun beginDragDropTooltip() {
        /*  The default tooltip position is a little offset to give space to see the context menu
            (it's also clamped within the current viewport/monitor)
            In the context of a dragging tooltip we try to reduce that offset and we enforce following the cursor.
            Whatever we do we want to call SetNextWindowPos() to enforce a tooltip position and
            disable clipping the tooltip without our display area, like regular tooltip do. */

        //ImVec2 tooltip_pos = g.IO.MousePos - g.ActiveIdClickOffset - g.Style.WindowPadding;
        val tooltipPos = io.mousePos + Vec2(16 * style.mouseCursorScale, 8 * style.mouseCursorScale)
        setNextWindowPos(tooltipPos)
        setNextWindowBgAlpha(style.colors[Col.PopupBg].w * 0.6f)
        //PushStyleVar(ImGuiStyleVar_Alpha, g.Style.Alpha * 0.60f); // This would be nice but e.g ColorButton with checkboard has issue with transparent colors :(
        beginTooltipEx(0, true)
    }

    fun endDragDropTooltip() = endTooltip()

    // New Columns API (FIXME-WIP)

    /** setup number of columns. use an identifier to distinguish multiple column sets. close with EndColumns().    */
    fun beginColumns(strId: String = "", columnsCount: Int, flags: ColumnsFlags) {

        with(currentWindow) {

            assert(columnsCount >= 1)
            assert(dc.currentColumns == null) { "Nested columns are currently not supported" }

            val id = getColumnsID(strId, columnsCount)

            // Acquire storage for the columns set
            val columns = findOrCreateColumns(id)
            assert(columns.id == id)
            with(columns) {
                current = 0
                count = columnsCount
                this.flags = flags
            }
            dc.currentColumns = columns

            // Set state for first column
            val contentRegionWidth = if (sizeContentsExplicit.x != 0f) sizeContentsExplicit.x else innerClipRect.max.x - pos.x
            with(columns) {
                minX = dc.indent - style.itemSpacing.x // Lock our horizontal range
                maxX = max(contentRegionWidth - scroll.x, minX + 1f)
                backupCursorPosY = dc.cursorPos.y
                backupCursorMaxPosX = dc.cursorMaxPos.x
                lineMaxY = dc.cursorPos.y
                lineMinY = dc.cursorPos.y
            }
            dc.columnsOffset = 0f
            dc.cursorPos.x = (pos.x + dc.indent + dc.columnsOffset).i.f

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
                val clipX1 = floor(0.5f + pos.x + getColumnOffset(n))
                val clipX2 = floor(0.5f + pos.x + getColumnOffset(n + 1) - 1f)
                column.clipRect = Rect(clipX1, -Float.MAX_VALUE, clipX2, +Float.MAX_VALUE)
                column.clipRect clipWith clipRect
            }
            if (columns.count > 1) {
                drawList.channelsSplit(columns.count)
                pushColumnClipRect()
            }
            pushItemWidth(getColumnWidth() * 0.65f)
        }
    }

    fun endColumns() = with(currentWindow) {

        val columns = dc.currentColumns!!

        popItemWidth()
        if (columns.count > 1) {
            popClipRect()
            drawList.channelsMerge()
        }

        columns.lineMaxY = glm.max(columns.lineMaxY, dc.cursorPos.y)
        dc.cursorPos.y = columns.lineMaxY
        if (columns.flags hasnt Cf.GrowParentContentsSize)
            dc.cursorMaxPos.x = columns.backupCursorMaxPosX // Restore cursor max pos, as columns don't grow parent

        // Draw columns borders and handle resize
        var isBeingResized = false
        if (columns.flags hasnt Cf.NoBorder && !skipItems) {

            // We clip Y boundaries CPU side because very long triangles are mishandled by some GPU drivers.
            val y1 = columns.backupCursorPosY max clipRect.min.y
            val y2 = dc.cursorPos.y min clipRect.max.y
            var draggingColumn = -1
            for (n in 1 until columns.count) {
                val column = columns.columns[n]
                val x = pos.x + getColumnOffset(n)
                val columnId: ID = columns.id + n
                val columnHitHw = COLUMNS_HIT_RECT_HALF_WIDTH
                val columnHitRect = Rect(Vec2(x - columnHitHw, y1), Vec2(x + columnHitHw, y2))
                keepAliveId(columnId)
                if (isClippedEx(columnHitRect, columnId, false)) continue

                var hovered = false
                var held = false
                if (columns.flags hasnt Cf.NoResize) {

                    val (_, b, c) = buttonBehavior(columnHitRect, columnId)
                    hovered = b
                    held = c
                    if (hovered || held)
                        g.mouseCursor = MouseCursor.ResizeEW
                    if (held && column.flags hasnt Cf.NoResize)
                        draggingColumn = n
                }

                // Draw column
                val col = getColorU32(if (held) Col.SeparatorActive else if (hovered) Col.SeparatorHovered else Col.Separator)
                val xi = x.i.f
                drawList.addLine(Vec2(xi, y1 + 1f), Vec2(xi, y2), col)
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

        dc.currentColumns = null
        dc.columnsOffset = 0f
        dc.cursorPos.x = (pos.x + dc.indent + dc.columnsOffset).i.f
    }

    fun pushColumnClipRect(columnIndex_: Int = -1) {

        val window = currentWindowRead!!
        val columns = window.dc.currentColumns!!
        val columnIndex = if (columnIndex_ < 0) columns.current else columnIndex_

        pushClipRect(columns.columns[columnIndex].clipRect.min, columns.columns[columnIndex].clipRect.max, false)
    }

    fun getColumnsID(strId: String, columnsCount: Int): ID {

        val window = g.currentWindow!!

        // Differentiate column ID with an arbitrary prefix for cases where users name their columns set the same as another widget.
        // In addition, when an identifier isn't explicitly provided we include the number of columns in the hash to make it uniquer.
        pushId(0x11223347 + if (strId.isNotEmpty()) 0 else columnsCount)
        return window.getId(if (strId.isNotEmpty()) strId else "columns")
                .also { popId() }
    }

    // Tab Bars

    fun tabItemCalcSize(label: String, hasCloseButton: Boolean): Vec2 {

        val labelSize = calcTextSize(label, 0, true)
        val size = Vec2(labelSize.x + style.framePadding.x, labelSize.y + style.framePadding.y * 2f)
        size.x += style.framePadding.x + when {
            hasCloseButton -> style.itemInnerSpacing.x + g.fontSize // We use Y intentionally to fit the close button circle.
            else -> 1f
        }
        return Vec2(size.x min TabBar.calcMaxTabWidth(), size.y)
    }

    fun tabItemBackground(drawList: DrawList, bb: Rect, flags: TabItemFlags, col: Int) {

        // While rendering tabs, we trim 1 pixel off the top of our bounding box so they can fit within a regular frame height while looking "detached" from it.
        val width = bb.width
        assert(width > 0f)
        val rounding = max(0f, min(style.tabRounding, width * 0.5f - 1f))
        val y1 = bb.min.y + 1f
        val y2 = bb.max.y - 1f
        drawList.apply {
            pathLineTo(Vec2(bb.min.x, y2))
            pathArcToFast(Vec2(bb.min.x + rounding, y1 + rounding), rounding, 6, 9)
            pathArcToFast(Vec2(bb.max.x - rounding, y1 + rounding), rounding, 9, 12)
            pathLineTo(Vec2(bb.max.x, y2))
            pathFillConvex(col)
            if (style.tabBorderSize > 0f) {
                pathLineTo(Vec2(bb.min.x + 0.5f, y2))
                pathArcToFast(Vec2(bb.min.x + rounding + 0.5f, y1 + rounding + 0.5f), rounding, 6, 9)
                pathArcToFast(Vec2(bb.max.x - rounding - 0.5f, y1 + rounding + 0.5f), rounding, 9, 12)
                pathLineTo(Vec2(bb.max.x - 0.5f, y2))
                pathStroke(Col.Border.u32, false, style.tabBorderSize)
            }
        }
    }

    /** Render text label (with custom clipping) + Unsaved Document marker + Close Button logic
     *  We tend to lock style.FramePadding for a given tab-bar, hence the 'frame_padding' parameter.    */
    fun tabItemLabelAndCloseButton(drawList: DrawList, bb: Rect, flags: TabItemFlags, framePadding: Vec2, label: String, tabId: ID, closeButtonId: ID): Boolean {

        val labelSize = calcTextSize(label, 0, true)
        if (bb.width <= 1f) return false

        // Render text label (with clipping + alpha gradient) + unsaved marker
        val TAB_UNSAVED_MARKER = "*"
        val textPixelClipBb = Rect(bb.min.x + framePadding.x, bb.min.y + framePadding.y, bb.max.x - framePadding.x, bb.max.y)
        if (flags has TabItemFlag.UnsavedDocument) {
            textPixelClipBb.max.x -= calcTextSize(TAB_UNSAVED_MARKER, 0, false).x
            val unsavedMarkerPos = Vec2(min(bb.min.x + framePadding.x + labelSize.x + 2, textPixelClipBb.max.x), bb.min.y + framePadding.y + (-g.fontSize * 0.25f).i.f)
            renderTextClippedEx(drawList, unsavedMarkerPos, bb.max - framePadding, TAB_UNSAVED_MARKER, 0, null)
        }
        val textEllipsisClipBb = Rect(textPixelClipBb)

        /*  Close Button
            We are relying on a subtle and confusing distinction between 'hovered' and 'g.HoveredId' which happens
            because we are using ImGuiButtonFlags_AllowOverlapMode + SetItemAllowOverlap()
            'hovered' will be true when hovering the Tab but NOT when hovering the close button
            'g.HoveredId==id' will be true when hovering the Tab including when hovering the close button
            'g.ActiveId==close_button_id' will be true when we are holding on the close button, in which case both hovered booleans are false */
        var closeButtonPressed = false
        var closeButtonVisible = false
        if (closeButtonId != 0)
            if (g.hoveredId == tabId || g.hoveredId == closeButtonId || g.activeId == closeButtonId)
                closeButtonVisible = true
        if (closeButtonVisible) {
            val closeButtonSz = g.fontSize * 0.5f
            itemHoveredDataBackup {
                if (closeButton(closeButtonId, Vec2(bb.max.x - framePadding.x - closeButtonSz, bb.min.y + framePadding.y + closeButtonSz), closeButtonSz))
                    closeButtonPressed = true
            }

            // Close with middle mouse button
            if (flags hasnt TabItemFlag.NoCloseWithMiddleMouseButton && isMouseClicked(2))
                closeButtonPressed = true

            textPixelClipBb.max.x -= closeButtonSz * 2f
        }

        // Label with ellipsis
        // FIXME: This should be extracted into a helper but the use of text_pixel_clip_bb and !close_button_visible makes it tricky to abstract at the moment
        val labelDisplayEnd = findRenderedTextEnd(label)
        if (labelSize.x > textEllipsisClipBb.width) {
            val ellipsisDotCount = 3
            val ellipsisWidth = (1f + 1f) * ellipsisDotCount - 1f
            val remaining = IntArray(1)
            var labelSizeClippedX = g.font.calcTextSizeA(g.fontSize, textEllipsisClipBb.width - ellipsisWidth + 1f, 0f, label, labelDisplayEnd, remaining).x
            var labelEnd = remaining[0]
            if (labelEnd == 0 && labelEnd < labelDisplayEnd) {    // Always display at least 1 character if there's no room for character + ellipsis
                labelEnd = labelDisplayEnd // TODO CHECK textCountUtf8BytesFromChar(label, labelDisplayEnd)
                labelSizeClippedX = g.font.calcTextSizeA(g.fontSize, Float.MAX_VALUE, 0f, label, labelEnd).x
            }
            while (labelEnd > 0 && label[labelEnd - 1].isBlankA) { // Trim trailing space
                labelEnd--
                labelSizeClippedX -= g.font.calcTextSizeA(g.fontSize, Float.MAX_VALUE, 0f, label, labelEnd + 1).x // Ascii blanks are always 1 byte
            }
            renderTextClippedEx(drawList, textPixelClipBb.min, textPixelClipBb.max, label, labelEnd, labelSize, Vec2())

            val ellipsisX = textPixelClipBb.min.x + labelSizeClippedX + 1f
            if (!closeButtonVisible && ellipsisX + ellipsisWidth <= bb.max.x)
                renderPixelEllipsis(drawList, Vec2(ellipsisX, textPixelClipBb.min.y), ellipsisDotCount, Col.Text.u32)
        } else
            renderTextClippedEx(drawList, textPixelClipBb.min, textPixelClipBb.max, label, labelDisplayEnd, labelSize, Vec2())

        return closeButtonPressed
    }

    /*  Render helpers
        AVOID USING OUTSIDE OF IMGUI.CPP! NOT FOR PUBLIC CONSUMPTION. THOSE FUNCTIONS ARE A MESS. THEIR SIGNATURE AND
        BEHAVIOR WILL CHANGE, THEY NEED TO BE REFACTORED INTO SOMETHING DECENT.
        NB: All position are in absolute pixels coordinates (we are never using window coordinates internally) */
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

    fun renderTextWrapped(pos: Vec2, text: String, textEnd_: Int?, wrapWidth: Float) {

        val window = g.currentWindow!!

        val textEnd = textEnd_ ?: text.length // FIXME-OPT

        if (textEnd > 0) {
            window.drawList.addText(g.font, g.fontSize, pos, Col.Text.u32, text.toCharArray(), textEnd, wrapWidth)
            if (g.logEnabled) logRenderedText(pos, text, textEnd)
        }
    }


    /** Default clipRect uses (pos_min,pos_max)
     *  Handle clipping on CPU immediately (vs typically let the GPU clip the triangles that are overlapping the clipping
     *  rectangle edges)    */
    fun renderTextClippedEx(drawList: DrawList, posMin: Vec2, posMax: Vec2, text: String, textDisplayEnd: Int = 0,
                            textSizeIfKnown: Vec2? = null, align: Vec2 = Vec2(), clipRect: Rect? = null) {

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
            drawList.addText(null, 0f, pos, Col.Text.u32, text.toCharArray(), textDisplayEnd, 0f, fineClipRect)
        } else
            drawList.addText(null, 0f, pos, Col.Text.u32, text.toCharArray(), textDisplayEnd, 0f, null)
    }

    fun renderTextClipped(posMin: Vec2, posMax: Vec2, text: String, textEnd: Int = 0, textSizeIfKnown: Vec2? = null,
                          align: Vec2 = Vec2(), clipRect: Rect? = null) {
        // Hide anything after a '##' string
        val textDisplayEnd = findRenderedTextEnd(text, textEnd)
        if (textDisplayEnd == 0) return

        val window = g.currentWindow!!
        renderTextClippedEx(window.drawList, posMin, posMax, text, textDisplayEnd, textSizeIfKnown, align, clipRect)
        if (g.logEnabled)
            logRenderedText(posMax, text, textDisplayEnd)
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

    /** Render an arrow aimed to be aligned with text (p_min is a position in the same space text would be positioned). To e.g. denote expanded/collapsed state  */
    fun renderArrow(pMin: Vec2, dir: Dir, scale: Float = 1f) {

        val h = g.fontSize * 1f
        var r = h * 0.4f * scale
        val center = pMin + Vec2(h * 0.5f, h * 0.5f * scale)

        val a: Vec2
        val b: Vec2
        val c: Vec2
        when (dir) {
            Dir.Up, Dir.Down -> {
                if (dir == Dir.Up) r = -r
                a = Vec2(+0.000f, +0.75f) * r
                b = Vec2(-0.866f, -0.75f) * r
                c = Vec2(+0.866f, -0.75f) * r
            }
            Dir.Left, Dir.Right -> {
                if (dir == Dir.Left) r = -r
                a = Vec2(+0.75f, +0.000f) * r
                b = Vec2(-0.75f, +0.866f) * r
                c = Vec2(-0.75f, -0.866f) * r
            }
            else -> throw Error()
        }

        g.currentWindow!!.drawList.addTriangleFilled(center + a, center + b, center + c, Col.Text.u32)
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
            val fullyVisible = displayRect in window.clipRect
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
        val halfPI = glm.HPIf // We will == compare to this because we know this is the exact value ImAcos01 can return.
        val x0 = glm.max(p0.x, rect.min.x + rounding)
        if (arc0B == arc0E) {
            drawList.pathLineTo(Vec2(x0, p1.y))
            drawList.pathLineTo(Vec2(x0, p0.y))
        } else if (arc0B == 0f && arc0E == halfPI) {
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
            } else if (arc1B == 0f && arc1E == halfPI) {
                drawList.pathArcToFast(Vec2(x1, p0.y + rounding), rounding, 9, 12) // TR
                drawList.pathArcToFast(Vec2(x1, p1.y - rounding), rounding, 0, 3)  // BR
            } else {
                drawList.pathArcTo(Vec2(x1, p0.y + rounding), rounding, -arc1E, -arc1B, 3) // TR
                drawList.pathArcTo(Vec2(x1, p1.y - rounding), rounding, +arc1B, +arc1E, 3) // BR
            }
        }
        drawList.pathFillConvex(col)
    }

    /** FIXME: Rendering an ellipsis "..." is a surprisingly tricky problem for us... we cannot rely on font glyph having it,
     *  and regular dot are typically too wide. If we render a dot/shape ourselves it comes with the risk that it wouldn't match
     *  the boldness or positioning of what the font uses... */
    fun renderPixelEllipsis(drawList: DrawList, pos: Vec2, count: Int, col: Int) {
        val font = drawList._data.font!!
        val fontScale = drawList._data.fontSize / font.fontSize
        pos.y += (font.displayOffset.y + font.ascent * fontScale + 0.5f - 1f).i.f
        for (dotN in 0 until count)
            drawList.addRectFilled(Vec2(pos.x + dotN * 2f, pos.y), Vec2(pos.x + dotN * 2f + 1f, pos.y + 1f), col)
    }

    /** Find the optional ## from which we stop displaying text.    */
    fun findRenderedTextEnd(text: String, textEnd_: Int = text.length): Int { // TODO function extension?
        val textEnd = if (textEnd_ == 0) text.length else textEnd_
        var textDisplayEnd = 0
        while (textDisplayEnd < textEnd && (text[textDisplayEnd + 0] != '#' || text[textDisplayEnd + 1] != '#'))
            textDisplayEnd++
        return textDisplayEnd
    }

    fun logRenderedText(refPos: Vec2?, text: String, textEnd_: Int = text.length) {
        val window = g.currentWindow!!

        val textEnd = if (textEnd_ == 0) findRenderedTextEnd(text) else textEnd_

        val logNewLine = refPos?.let { it.y > g.logLinePosY + 1 } ?: false

        refPos?.let { g.logLinePosY = it.y }
        if (logNewLine)
            g.logLineFirstItem = true

        var textRemaining = text
        if (g.logDepthRef > window.dc.treeDepth)
            g.logDepthRef = window.dc.treeDepth

        val treeDepth = window.dc.treeDepth - g.logDepthRef

        //TODO: make textEnd aware
        while (true) {
            /*  Split the string. Each new line (after a '\n') is followed by spacing corresponding to the current depth of our log entry.
                We don't add a trailing \n to allow a subsequent item on the same line to be captured.
            */
            val lineStart = textRemaining
            val lineEnd = if (lineStart.indexOf('\n') == -1) lineStart.length else lineStart.indexOf('\n')
            val isFirstLine = text.startsWith(lineStart)
            val isLastLine = text.endsWith(lineStart.substring(0, lineEnd))
            if (!isLastLine or lineStart.isNotEmpty()) {
                val charCount = lineStart.length
                when {
                    logNewLine or !isFirstLine -> logText("%s%s", "", lineStart)
                    g.logLineFirstItem -> logText("%s%s", "", lineStart)
                    else -> logText("%s", lineStart)
                }
            } else if (logNewLine) {
                // An empty "" string at a different Y position should output a carriage return.
                logText("\n")
                break
            }


            if (isLastLine)
                break
            textRemaining = textRemaining.substring(lineEnd + 1)
        }
    }


    //-----------------------------------------------------------------------------
    // Internals Render Helpers
    // (progressively moved from imgui.cpp to here when they are redesigned to stop accessing ImGui global state)
    //-----------------------------------------------------------------------------
    // RenderMouseCursor()
    // RenderArrowPointingAt()
    // RenderRectFilledRangeH()
    //-----------------------------------------------------------------------------
    // Render helpers (those functions don't access any ImGui state!)


    fun renderMouseCursor(drawList: DrawList, pos: Vec2, scale: Float, mouseCursor: MouseCursor) {
        if (mouseCursor == MouseCursor.None) return
        val colShadow = COL32(0, 0, 0, 48)
        val colBorder = COL32(0, 0, 0, 255)          // Black
        val colFill = COL32(255, 255, 255, 255)    // White
        val fontAtlas = drawList._data.font!!.containerAtlas
        val offset = Vec2()
        val size = Vec2()
        val uv = Array(2) { Vec2() }
        if (fontAtlas.getMouseCursorTexData(mouseCursor, offset, size, uv)) {
            pos -= offset
            val texId: TextureID = fontAtlas.texId
            drawList.apply {
                pushTextureId(texId)
                addImage(texId, pos + Vec2(1, 0) * scale, pos + Vec2(1, 0) * scale + size * scale, uv[2], uv[3], colShadow)
                addImage(texId, pos + Vec2(2, 0) * scale, pos + Vec2(2, 0) * scale + size * scale, uv[2], uv[3], colShadow)
                addImage(texId, pos, pos + size * scale, uv[2], uv[3], colBorder)
                addImage(texId, pos, pos + size * scale, uv[0], uv[1], colFill)
                popTextureId()
            }
        }
    }

    /** Render an arrow. 'pos' is position of the arrow tip. halfSz.x is length from base to tip. halfSz.y is length on each side. */
    fun renderArrowPointingAt(drawList: DrawList, pos: Vec2, halfSz: Vec2, direction: Dir, col: Int) = when (direction) {
        Dir.Left -> drawList.addTriangleFilled(Vec2(pos.x + halfSz.x, pos.y - halfSz.y), Vec2(pos.x + halfSz.x, pos.y + halfSz.y), pos, col)
        Dir.Right -> drawList.addTriangleFilled(Vec2(pos.x - halfSz.x, pos.y + halfSz.y), Vec2(pos.x - halfSz.x, pos.y - halfSz.y), pos, col)
        Dir.Up -> drawList.addTriangleFilled(Vec2(pos.x + halfSz.x, pos.y + halfSz.y), Vec2(pos.x - halfSz.x, pos.y + halfSz.y), pos, col)
        Dir.Down -> drawList.addTriangleFilled(Vec2(pos.x - halfSz.x, pos.y - halfSz.y), Vec2(pos.x + halfSz.x, pos.y - halfSz.y), pos, col)
        else -> Unit
    }


    // Widgets

    /** Raw text without formatting. Roughly equivalent to text("%s", text) but:
     *  A) doesn't require null terminated string if 'textEnd' is specified
     *  B) it's faster, no memory copy is done, no buffer size limits, recommended for long chunks of text. */
    fun textEx(text: String, textEnd: Int = text.length, flags: TextFlags = 0) {

        val window = currentWindow
        if (window.skipItems) return

        val textPos = Vec2(window.dc.cursorPos.x, window.dc.cursorPos.y + window.dc.currentLineTextBaseOffset)
        val wrapPosX = window.dc.textWrapPos
        val wrapEnabled = wrapPosX >= 0f
        if (textEnd > 2000 && !wrapEnabled) {
            /*  Long text!
                Perform manual coarse clipping to optimize for long multi-line text
                - From this point we will only compute the width of lines that are visible. Optimization only available
                    when word-wrapping is disabled.
                - We also don't vertically center the text within the line full height, which is unlikely to matter
                    because we are likely the biggest and only item on the line.
                - We use memchr(), pay attention that well optimized versions of those str/mem functions are much faster
                    than a casually written loop.   */

            var line = 0
            val lineHeight = textLineHeight
            val textSize = Vec2()

            // Lines to skip (can't skip when logging text)
            val pos = Vec2(textPos)
            if (!g.logEnabled) {

                val linesSkippable = ((window.clipRect.min.y - textPos.y) / lineHeight).i
                if (linesSkippable > 0) {
                    var linesSkipped = 0
                    while (line < textEnd && linesSkipped < linesSkippable) {
                        val lineEnd = text.memchr(line, '\n') ?: textEnd
                        if (flags hasnt TextFlag.NoWidthForLargeClippedText)
                            textSize.x = textSize.x max calcTextSize(text.substring(line), lineEnd).x
                        line = lineEnd + 1
                        linesSkipped++
                    }
                    pos.y += linesSkipped * lineHeight
                }
            }
            // Lines to render
            if (line < textEnd) {
                val lineRect = Rect(pos, pos + Vec2(Float.MAX_VALUE, lineHeight))
                while (line < textEnd) {
                    if (isClippedEx(lineRect, 0, false)) break

                    val lineEnd = text.memchr(line, '\n') ?: textEnd
                    val pLine = text.substring(line)
                    textSize.x = textSize.x max calcTextSize(pLine, lineEnd).x
                    renderText(pos, pLine, lineEnd - line, false)
                    line = lineEnd + 1
                    lineRect.min.y += lineHeight
                    lineRect.max.y += lineHeight
                    pos.y += lineHeight
                }

                // Count remaining lines
                var linesSkipped = 0
                while (line < textEnd) {
                    val lineEnd = text.memchr(line, '\n') ?: textEnd
                    if (flags hasnt TextFlag.NoWidthForLargeClippedText)
                        textSize.x = textSize.x max calcTextSize(text.substring(line), lineEnd).x
                    line = lineEnd + 1
                    linesSkipped++
                }
                pos.y += linesSkipped * lineHeight
            }
            textSize.y += (pos - textPos).y

            val bb = Rect(textPos, textPos + textSize)
            itemSize(textSize)
            itemAdd(bb, 0)
        } else {
            val wrapWidth = if (wrapEnabled) calcWrapWidthForPos(window.dc.cursorPos, wrapPosX) else 0f
            val textSize = calcTextSize(text, textEnd, false, wrapWidth)

            val bb = Rect(textPos, textPos + textSize)
            itemSize(textSize)
            if (!itemAdd(bb, 0)) return

            // Render (we don't hide text after ## in this end-user function)
            renderTextWrapped(bb.min, text, textEnd, wrapWidth)
        }
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
        itemSize(size, style.framePadding.y)
        if (!itemAdd(bb, id)) return false

        var flags = flags_
        if (window.dc.itemFlags has If.ButtonRepeat) flags = flags or Bf.Repeat
        val (pressed, hovered, held) = buttonBehavior(bb, id, flags)
        if (pressed)
            markItemEdited(id)

        // Render
        val col = if (hovered && held) Col.ButtonActive else if (hovered) Col.ButtonHovered else Col.Button
        renderNavHighlight(bb, id)
        renderFrame(bb.min, bb.max, col.u32, true, style.frameRounding)
        renderTextClipped(bb.min + style.framePadding, bb.max - style.framePadding, label, 0, labelSize,
                style.buttonTextAlign, bb)

        // Automatically close popups
        //if (pressed && !(flags & ImGuiButtonFlags_DontClosePopups) && (window->Flags & ImGuiWindowFlags_Popup))
        //    CloseCurrentPopup();

        ImGuiTestEngineHook_ItemInfo(id, label, window.dc.lastItemStatusFlags)
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
            val col = if (held) Col.ButtonActive else Col.ButtonHovered
            window.drawList.addCircleFilled(center, 2f max radius, col.u32, 9)
        }

        val crossExtent = (radius * 0.7071f) - 1f
        val crossCol = Col.Text.u32
        center -= 0.5f
        window.drawList.addLine(center + crossExtent, center - crossExtent, crossCol, 1f)
        window.drawList.addLine(center + Vec2(crossExtent, -crossExtent), center + Vec2(-crossExtent, crossExtent), crossCol, 1f)

        return pressed
    }

    fun collapseButton(id: ID, pos: Vec2): Boolean {
        val window = g.currentWindow!!
        val bb = Rect(pos, pos + g.fontSize)
        itemAdd(bb, id)
        return buttonBehavior(bb, id, Bf.None)[0].also {
            renderNavHighlight(bb, id)
            renderArrow(bb.min, if (window.collapsed) Dir.Right else Dir.Down, 1f)
            // Switch to moving the window after mouse is moved beyond the initial drag threshold
            if (isItemActive && isMouseDragging())
                window.startMouseMoving()
        }
    }


    /** square button with an arrow shape */
    fun arrowButtonEx(strId: String, dir: Dir, size: Vec2, flags_: ButtonFlags): Boolean {

        var flags = flags_

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(strId)
        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        val defaultSize = frameHeight
        itemSize(size, if (size.y >= defaultSize) style.framePadding.y else 0f)
        if (!itemAdd(bb, id)) return false

        if (window.dc.itemFlags has If.ButtonRepeat)
            flags = flags or Bf.Repeat

        val (pressed, hovered, held) = buttonBehavior(bb, id, flags)

        // Render
        val col = if (hovered && held) Col.ButtonActive else if (hovered) Col.ButtonHovered else Col.Button
        renderNavHighlight(bb, id)
        renderFrame(bb.min, bb.max, col.u32, true, g.style.frameRounding)
        renderArrow(bb.min + Vec2(max(0f, (size.x - g.fontSize) * 0.5f), max(0f, (size.y - g.fontSize) * 0.5f)), dir)

        return pressed
    }

    /** Vertical scrollbar
     *  The entire piece of code below is rather confusing because:
     *  - We handle absolute seeking (when first clicking outside the grab) and relative manipulation (afterward or when
     *          clicking inside the grab)
     *  - We store values as normalized ratio and in a form that allows the window content to change while we are holding on
     *          a scrollbar
     *  - We handle both horizontal and vertical scrollbars, which makes the terminology not ideal. */
    fun scrollbar(axis: Axis) {

        val window = g.currentWindow!!

        val horizontal = axis == Axis.X
        val id = getScrollbarID(window, axis)
        keepAliveId(id)

        // Render background
        val otherScrollbar = if (horizontal) window.scrollbar.y else window.scrollbar.x
        val otherScrollbarSizeW = if (otherScrollbar) style.scrollbarSize else 0f
        val hostRect = window.rect()
        val borderSize = window.windowBorderSize
        val bb = when (horizontal) {
            true -> Rect(hostRect.min.x + borderSize, hostRect.max.y - style.scrollbarSize,
                    hostRect.max.x - otherScrollbarSizeW - borderSize, hostRect.max.y - borderSize)
            else -> Rect(hostRect.max.x - style.scrollbarSize, hostRect.min.y + borderSize,
                    hostRect.max.x - borderSize, hostRect.max.y - otherScrollbarSizeW - borderSize)
        }
        bb.min maxAssign hostRect.min // Handle case where the host rectangle is smaller than the scrollbar
        if (!horizontal)
            bb.min.y += window.titleBarHeight + if (window.flags has Wf.MenuBar) window.menuBarHeight else 0f  // FIXME: InnerRect?

        val bbWidth = bb.width
        val bbHeight = bb.height
        if (bbWidth <= 0f || bbHeight <= 0f)
            return

        // When we are too small, start hiding and disabling the grab (this reduce visual noise on very small window and facilitate using the resize grab)
        var alpha = 1.f
        if (axis == Axis.Y && bbHeight < g.fontSize + style.framePadding.y * 2f) {
            alpha = saturate((bbHeight - g.fontSize) / (style.framePadding.y * 2f))
            if (alpha <= 0f)
                return
        }
        val allowInteraction = alpha >= 1f

        val windowRoundingCorners = when {
            horizontal -> Dcf.BotLeft.i or if (otherScrollbar) 0 else Dcf.BotRight.i
            else -> (if (window.flags has Wf.NoTitleBar && window.flags hasnt Wf.MenuBar) Dcf.TopRight.i else 0) or
                    if (otherScrollbar) 0 else Dcf.BotRight.i
        }
        window.drawList.addRectFilled(bb.min, bb.max, Col.ScrollbarBg.u32, window.windowRounding, windowRoundingCorners)
        bb.expand(Vec2(
                -glm.clamp(((bbWidth - 2f) * 0.5f).i.f, 0f, 3f),
                -glm.clamp(((bbHeight - 2f) * 0.5f).i.f, 0f, 3f)))

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
        if (held && allowInteraction && grabHNorm < 1f) {
            val scrollbarPosV = if (horizontal) bb.min.x else bb.min.y
            val mousePosV = if (horizontal) io.mousePos.x else io.mousePos.y
            var clickDeltaToGrabCenterV = if (horizontal) g.scrollbarClickDeltaToGrabCenter.x else g.scrollbarClickDeltaToGrabCenter.y

            // Click position in scrollbar normalized space (0.0f->1.0f)
            val clickedVNorm = saturate((mousePosV - scrollbarPosV) / scrollbarSizeV)
            hoveredId = id

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

        // Render grab
        val grabCol = getColorU32(when {
            held -> Col.ScrollbarGrabActive
            hovered -> Col.ScrollbarGrabHovered
            else -> Col.ScrollbarGrab
        }, alpha)
        val grabRect = when {
            horizontal -> Rect(lerp(bb.min.x, bb.max.x, grabVNorm), bb.min.y,
                    min(lerp(bb.min.x, bb.max.x, grabVNorm) + grabHPixels, hostRect.max.x), bb.max.y)
            else -> Rect(bb.min.x, lerp(bb.min.y, bb.max.y, grabVNorm),
                    bb.max.x, min(lerp(bb.min.y, bb.max.y, grabVNorm) + grabHPixels, hostRect.max.y))
        }
        window.drawList.addRectFilled(grabRect.min, grabRect.max, grabCol, style.scrollbarRounding)
    }

    fun getScrollbarID(window: Window, axis: Axis): ID =
            window.getIdNoKeepAlive(if (axis == Axis.X) "#SCROLLX" else "#SCROLLY")

    /** Vertical separator, for menu bars (use current line height). not exposed because it is misleading
     *  what it doesn't have an effect on regular layout.   */
    fun verticalSeparator() {
        val window = currentWindow
        if (window.skipItems) return

        val y1 = window.dc.cursorPos.y
        val y2 = window.dc.cursorPos.y + window.dc.currentLineSize.y
        val bb = Rect(Vec2(window.dc.cursorPos.x, y1), Vec2(window.dc.cursorPos.x + 1f, y2))
        itemSize(Vec2(1f, 0f))
        if (!itemAdd(bb, 0)) return

        window.drawList.addLine(Vec2(bb.min), Vec2(bb.min.x, bb.max.y), Col.Separator.u32)
        if (g.logEnabled) logText(" |")
    }


    // Widgets low-level behaviors


    /** @return []pressed, hovered, held] */
    fun buttonBehavior(bb: Rect, id: ID, flag: Bf) = buttonBehavior(bb, id, flag.i)

    /** @return []pressed, hovered, held]
     *
     *  The ButtonBehavior() function is key to many interactions and used by many/most widgets.
     *  Because we handle so many cases (keyboard/gamepad navigation, drag and drop) and many specific behavior (via ImGuiButtonFlags_),
     *  this code is a little complex.
     *  By far the most common path is interacting with the Mouse using the default ImGuiButtonFlags_PressedOnClickRelease button behavior.
     *  See the series of events below and the corresponding state reported by dear imgui:
     *  ------------------------------------------------------------------------------------------------------------------------------------------------
     *  with PressedOnClickRelease:             return-value  IsItemHovered()  IsItemActive()  IsItemActivated()  IsItemDeactivated()  IsItemClicked()
     *    Frame N+0 (mouse is outside bb)        -             -                -               -                  -                    -
     *    Frame N+1 (mouse moves inside bb)      -             true             -               -                  -                    -
     *    Frame N+2 (mouse button is down)       -             true             true            true               -                    true
     *    Frame N+3 (mouse button is down)       -             true             true            -                  -                    -
     *    Frame N+4 (mouse moves outside bb)     -             -                true            -                  -                    -
     *    Frame N+5 (mouse moves inside bb)      -             true             true            -                  -                    -
     *    Frame N+6 (mouse button is released)   true          true             -               -                  true                 -
     *    Frame N+7 (mouse button is released)   -             true             -               -                  -                    -
     *    Frame N+8 (mouse moves outside bb)     -             -                -               -                  -                    -
     *  ------------------------------------------------------------------------------------------------------------------------------------------------
     *  with PressedOnClick:                    return-value  IsItemHovered()  IsItemActive()  IsItemActivated()  IsItemDeactivated()  IsItemClicked()
     *    Frame N+2 (mouse button is down)       true          true             true            true               -                    true
     *    Frame N+3 (mouse button is down)       -             true             true            -                  -                    -
     *    Frame N+6 (mouse button is released)   -             true             -               -                  true                 -
     *    Frame N+7 (mouse button is released)   -             true             -               -                  -                    -
     *  ------------------------------------------------------------------------------------------------------------------------------------------------
     *  with PressedOnRelease:                  return-value  IsItemHovered()  IsItemActive()  IsItemActivated()  IsItemDeactivated()  IsItemClicked()
     *    Frame N+2 (mouse button is down)       -             true             -               -                  -                    true
     *    Frame N+3 (mouse button is down)       -             true             -               -                  -                    -
     *    Frame N+6 (mouse button is released)   true          true             -               -                  -                    -
     *    Frame N+7 (mouse button is released)   -             true             -               -                  -                    -
     *  ------------------------------------------------------------------------------------------------------------------------------------------------
     *  with PressedOnDoubleClick:              return-value  IsItemHovered()  IsItemActive()  IsItemActivated()  IsItemDeactivated()  IsItemClicked()
     *    Frame N+0 (mouse button is down)       -             true             -               -                  -                    true
     *    Frame N+1 (mouse button is down)       -             true             -               -                  -                    -
     *    Frame N+2 (mouse button is released)   -             true             -               -                  -                    -
     *    Frame N+3 (mouse button is released)   -             true             -               -                  -                    -
     *    Frame N+4 (mouse button is down)       true          true             true            true               -                    true
     *    Frame N+5 (mouse button is down)       -             true             true            -                  -                    -
     *    Frame N+6 (mouse button is released)   -             true             -               -                  true                 -
     *    Frame N+7 (mouse button is released)   -             true             -               -                  -                    -
     *  ------------------------------------------------------------------------------------------------------------------------------------------------
     *  The behavior of the return-value changes when ImGuiButtonFlags_Repeat is set:
     *                                          Repeat+                  Repeat+           Repeat+             Repeat+
     *                                          PressedOnClickRelease    PressedOnClick    PressedOnRelease    PressedOnDoubleClick
     *  -------------------------------------------------------------------------------------------------------------------------------------------------
     *    Frame N+0 (mouse button is down)       -                        true              -                   true
     *    ...                                    -                        -                 -                   -
     *    Frame N + RepeatDelay                  true                     true              -                   true
     *    ...                                    -                        -                 -                   -
     *    Frame N + RepeatDelay + RepeatRate*N   true                     true              -                   true
     *  -------------------------------------------------------------------------------------------------------------------------------------------------   */
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

        if (IMGUI_ENABLE_TEST_ENGINE && id != 0 && window.dc.lastItemId != id)
            ImGuiTestEngineHook_ItemAdd(bb, id)

        var pressed = false
        var hovered = itemHoverable(bb, id)

        // Drag source doesn't report as hovered
        if (hovered && g.dragDropActive && g.dragDropPayload.sourceId == id && g.dragDropSourceFlags hasnt Ddf.SourceNoDisableHover)
            hovered = false

        // Special mode for Drag and Drop where holding button pressed for a long time while dragging another item triggers the button
        if (g.dragDropActive && flags has Bf.PressedOnDragDropHold && g.dragDropSourceFlags hasnt Ddf.SourceNoHoldToOpenOthers)
            if (isItemHovered(Hf.AllowWhenBlockedByActiveItem)) {
                hovered = true
                hoveredId = id
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
            if (flags hasnt Bf.NoHoveredOnNav)
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
                if ((navActivatedByCode || navActivatedByInputs) && flags hasnt Bf.NoNavFocus)
                    setFocusId(id, window)
                g.activeIdAllowNavDirFlags = (1 shl Dir.Left) or (1 shl Dir.Right) or (1 shl Dir.Up) or (1 shl Dir.Down)
            }
        }
        var held = false
        if (g.activeId == id) {
            if (pressed)
                g.activeIdHasBeenPressed = true
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

    fun dragBehavior(id: ID, dataType: DataType, v: FloatArray, ptr: Int, vSpeed: Float, vMin: Float?, vMax: Float?, format: String,
                     power: Float, flags: DragFlags): Boolean =
            withFloat(v, ptr) { dragBehavior(id, DataType.Float, it, vSpeed, vMin, vMax, format, power, flags) }

    fun <N : Number> dragBehavior(id: ID, dataType: DataType, v_: KMutableProperty0<N>, vSpeed: Float, vMin: Number?, vMax: Number?,
                                  format: String, power: Float, flags: DragFlags): Boolean {

        if (g.activeId == id)
            if (g.activeIdSource == InputSource.Mouse && !io.mouseDown[0])
                clearActiveId()
            else if (g.activeIdSource == InputSource.Nav && g.navActivatePressedId == id && !g.activeIdIsJustActivated)
                clearActiveId()

        var v by v_

        return when {
            g.activeId == id -> when (dataType) {
                DataType.Byte -> {
                    _i = (v as Byte).i
                    val min = vMin?.let { it as Byte } ?: Byte.MIN_VALUE
                    val max = vMax?.let { it as Byte } ?: Byte.MAX_VALUE
                    dragBehaviorT(dataType, ::_i, vSpeed, min.i, max.i, format, power, flags)
                            .also { v = _i.b as N }
                }
//                DataType.Ubyte -> {
//                    _i = (v as Ubyte).v.i
//                    val min = vMin?.let { it as Ubyte } ?: Ub
//                    val max = vMax?.let { it as Ubyte } ?: Byte.MAX_VALUE
//                    dragBehaviorT(dataType, ::_i, vSpeed, min.i, max.i, format, power, flags)
//                            .also { v = _i.b as N }
//                }
                DataType.Short -> {
                    _i = (v as Short).i
                    val min = vMin?.let { it as Short } ?: Short.MIN_VALUE
                    val max = vMax?.let { it as Short } ?: Short.MAX_VALUE
                    dragBehaviorT(dataType, ::_i, vSpeed, min.i, max.i, format, power, flags)
                            .also { v = _i.s as N }
                }
//                is Ubyte -> {
//                    _i = v.i
//                    val min = vMin?.i ?: Ubyte.MIN_VALUE
//                    val max = vMax?.i ?: Ubyte.MAX_VALUE
//                    dragBehaviorT(dataType, ::_i, vSpeed, min.i, max.i, format, power, flags)
//                            .also { (v as Ubyte).v = _i.b }
//                }
                DataType.Int -> {
                    _i = v as Int
                    val min = vMin?.let { it as Int } ?: Int.MIN_VALUE
                    val max = vMax?.let { it as Int } ?: Int.MAX_VALUE
                    dragBehaviorT(dataType, ::_i, vSpeed, min.i, max.i, format, power, flags)
                            .also { v = _i as N }
                }
//                is Ubyte -> {
//                    _i = v.i
//                    val min = vMin?.i ?: Ubyte.MIN_VALUE
//                    val max = vMax?.i ?: Ubyte.MAX_VALUE
//                    dragBehaviorT(dataType, ::_i, vSpeed, min.i, max.i, format, power, flags)
//                            .also { (v as Ubyte).v = _i.b }
//                }
                DataType.Long -> {
                    _L = v as Long
                    val min = vMin?.let { it as Long } ?: Long.MIN_VALUE
                    val max = vMax?.let { it as Long } ?: Long.MAX_VALUE
                    dragBehaviorT(dataType, ::_L, vSpeed, min.L, max.L, format, power, flags)
                            .also { v = _L as N }
                }
//                is Ubyte -> {
//                    _i = v.i
//                    val min = vMin?.i ?: Ubyte.MIN_VALUE
//                    val max = vMax?.i ?: Ubyte.MAX_VALUE
//                    dragBehaviorT(dataType, ::_i, vSpeed, min.i, max.i, format, power, flags)
//                            .also { (v as Ubyte).v = _i.b }
//                }
                DataType.Float -> {
                    _f = v as Float
                    val min = vMin?.let { it as Float } ?: Float.MIN_VALUE
                    val max = vMax?.let { it as Float } ?: Float.MAX_VALUE
                    dragBehaviorT(dataType, ::_f, vSpeed, min.f, max.f, format, power, flags)
                            .also { v = _f as N }
                }
                DataType.Double -> {
                    _d = v as Double
                    val min = vMin?.let { it as Double } ?: Double.MIN_VALUE
                    val max = vMax?.let { it as Double } ?: Double.MAX_VALUE
                    dragBehaviorT(dataType, ::_d, vSpeed, min.d, max.d, format, power, flags)
                            .also { v = _d as N }
                }
                else -> error("Invalid")
            }
            else -> false
        }
    }

    /** For 32-bits and larger types, slider bounds are limited to half the natural type range.
     *  So e.g. an integer Slider between INT_MAX-10 and INT_MAX will fail, but an integer Slider between INT_MAX/2-10 and INT_MAX/2 will be ok.
     *  It would be possible to lift that limitation with some work but it doesn't seem to be worth it for sliders. */
    fun sliderBehavior(bb: Rect, id: ID, v: FloatArray, vMin: Float, vMax: Float, format: String, power: Float,
                       flags: SliderFlags, outGrabBb: Rect) = sliderBehavior(bb, id, v, 0, vMin, vMax, format, power, flags, outGrabBb)

    fun sliderBehavior(bb: Rect, id: ID, v: FloatArray, ptr: Int, vMin: Float, vMax: Float, format: String, power: Float,
                       flags: SliderFlags, outGrabBb: Rect): Boolean = withFloat(v, ptr) {
        sliderBehavior(bb, id, DataType.Float, it, vMin, vMax, format, power, flags, outGrabBb)
    }

//    fun <N> sliderBehavior(bb: Rect, id: ID,
//                           v: KMutableProperty0<N>,
//                           vMin: Float, vMax: Float,
//                           format: String, power: Float,
//                           flags: SliderFlags, outGrabBb: Rect): Boolean where N : Number, N : Comparable<N> =
//            sliderBehavior(bb, id, DataType.Float, v, vMin, vMax, format, power, flags, outGrabBb)

    fun <N> sliderBehavior(bb: Rect, id: ID,
                           dataType: DataType, v: KMutableProperty0<N>,
                           vMin: N, vMax: N,
                           format: String, power: Float,
                           flags: SliderFlags, outGrabBb: Rect): Boolean where N : Number, N : Comparable<N> = when (dataType) {

        DataType.Byte -> {
            _i = (v() as Byte).i
            sliderBehaviorT(bb, id, dataType, ::_i, vMin.i, vMax.i, format, power, flags, outGrabBb)
                    .also { v.set(_i.b as N) }
        }
        DataType.Short -> {
            _i = (v() as Short).i
            sliderBehaviorT(bb, id, dataType, ::_i, vMin.i, vMax.i, format, power, flags, outGrabBb)
                    .also { v.set(_i.s as N) }
        }
        DataType.Int -> {
            assert(vMin as Int >= Int.MIN_VALUE / 2 && vMax as Int <= Int.MAX_VALUE / 2)
            sliderBehaviorT(bb, id, dataType, v, vMin as N, vMax, format, power, flags, outGrabBb)
        }
        DataType.Long -> {
            assert(vMin as Long >= Long.MIN_VALUE / 2 && vMax as Long <= Long.MAX_VALUE / 2)
            sliderBehaviorT(bb, id, dataType, v, vMin as N, vMax, format, power, flags, outGrabBb)
        }
        DataType.Float -> {
            assert(vMin as Float >= -Float.MAX_VALUE / 2f && vMax as Float <= Float.MAX_VALUE / 2f)
            sliderBehaviorT(bb, id, dataType, v, vMin as N, vMax, format, power, flags, outGrabBb)
        }
        DataType.Double -> {
            assert(vMin as Double >= -Double.MAX_VALUE / 2f && vMax as Double <= Double.MAX_VALUE / 2f)
            sliderBehaviorT(bb, id, dataType, v, vMin as N, vMax, format, power, flags, outGrabBb)
        }
        else -> throw Error()
    }

    fun treeNodeBehavior(id: ID, flags: TreeNodeFlags, label: String, labelEnd: Int = findRenderedTextEnd(label)): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val displayFrame = flags has Tnf.Framed
        val padding = if (displayFrame || flags has Tnf.FramePadding) Vec2(style.framePadding) else Vec2(style.framePadding.x, 0f)

        val labelSize = calcTextSize(label, labelEnd, false)

        // We vertically grow up to current line height up the typical widget height.
        val textBaseOffsetY = glm.max(padding.y, window.dc.currentLineTextBaseOffset) // Latch before ItemSize changes it
        val frameHeight = glm.max(glm.min(window.dc.currentLineSize.y, g.fontSize + style.framePadding.y * 2), labelSize.y + padding.y * 2)
        val frameBb = Rect(window.dc.cursorPos, Vec2(contentRegionMaxScreen.x, window.dc.cursorPos.y + frameHeight))
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
        val isLeaf = flags has Tnf.Leaf

        /*  Store a flag for the current depth to tell if we will allow closing this node when navigating one of its child.
            For this purpose we essentially compare if g.NavIdIsAlive went from 0 to 1 between TreeNode() and TreePop().
            This is currently only support 32 level deep and we are fine with (1 << Depth) overflowing into a zero. */
        if (isOpen && !g.navIdIsAlive && flags has Tnf.NavLeftJumpsBackHere && flags hasnt Tnf.NoTreePushOnOpen)
            window.dc.treeStoreMayJumpToParentOnPop = window.dc.treeStoreMayJumpToParentOnPop or (1 shl window.dc.treeDepth)

        val itemAdd = itemAdd(interactBb, id)
        window.dc.lastItemStatusFlags = window.dc.lastItemStatusFlags or ItemStatusFlag.HasDisplayRect
        window.dc.lastItemDisplayRect put frameBb

        if (!itemAdd) {
            if (isOpen && flags hasnt Tnf.NoTreePushOnOpen)
                treePushOverrideID(id)
            ImGuiTestEngineHook_ItemInfo(window.dc.lastItemId, label, window.dc.itemFlags or (if (isLeaf) ItemStatusFlag.None else ItemStatusFlag.Openable) or if (isOpen) ItemStatusFlag.Opened else ItemStatusFlag.None)
            return isOpen
        }

        /*  Flags that affects opening behavior:
                - 0 (default) .................... single-click anywhere to open
                - OpenOnDoubleClick .............. double-click anywhere to open
                - OpenOnArrow .................... single-click on arrow to open
                - OpenOnDoubleClick|OpenOnArrow .. single-click on arrow or double-click anywhere to open   */
        var buttonFlags: ButtonFlags = Bf.NoKeyModifiers.i
        if (flags has Tnf.AllowItemOverlap)
            buttonFlags = buttonFlags or Bf.AllowItemOverlap
        if (flags has Tnf.OpenOnDoubleClick)
            buttonFlags = buttonFlags or Bf.PressedOnDoubleClick or (if (flags has Tnf.OpenOnArrow) Bf.PressedOnClickRelease else Bf.None)
        if (!isLeaf)
            buttonFlags = buttonFlags or Bf.PressedOnDragDropHold

        val selected = flags has Tnf.Selected
        val wasSelected = selected

        val (pressed, hovered, held) = buttonBehavior(interactBb, id, buttonFlags)
        var toggled = false
        if (!isLeaf) {
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

        // In this branch, TreeNodeBehavior() cannot toggle the selection so this will never trigger.
        if (selected != wasSelected)
            window.dc.lastItemStatusFlags = window.dc.lastItemStatusFlags or ItemStatusFlag.ToggledSelection

        // Render
        val col = if (held && hovered) Col.HeaderActive else if (hovered) Col.HeaderHovered else Col.Header
        val textPos = frameBb.min + Vec2(textOffsetX, textBaseOffsetY)
        val navHighlightFlags: NavHighlightFlags = NavHighlightFlag.TypeThin.i
        if (displayFrame) {
            // Framed type
            renderFrame(frameBb.min, frameBb.max, col.u32, true, style.frameRounding)
            renderNavHighlight(frameBb, id, navHighlightFlags)
            renderArrow(frameBb.min + Vec2(padding.x, textBaseOffsetY), if (isOpen) Dir.Down else Dir.Right, 1f)
            if (g.logEnabled) {
                /*  NB: '##' is normally used to hide text (as a library-wide feature), so we need to specify the text
                    range to make sure the ## aren't stripped out here.                 */
                logRenderedText(textPos, "\n##", 3)
                renderTextClipped(textPos, frameBb.max, label, labelEnd, labelSize)
                logRenderedText(textPos, "#", 2) // TODO check me
            } else
                renderTextClipped(textPos, frameBb.max, label, labelEnd, labelSize)
        } else {
            // Unframed typed for tree nodes
            if (hovered || selected) {
                renderFrame(frameBb.min, frameBb.max, col.u32, false)
                renderNavHighlight(frameBb, id, navHighlightFlags)
            }
            if (flags has Tnf.Bullet)
                renderBullet(frameBb.min + Vec2(textOffsetX * 0.5f, g.fontSize * 0.5f + textBaseOffsetY))
            else if (!isLeaf)
                renderArrow(frameBb.min + Vec2(padding.x, g.fontSize * 0.15f + textBaseOffsetY),
                        if (isOpen) Dir.Down else Dir.Right, 0.7f)
            if (g.logEnabled)
                logRenderedText(textPos, ">")
            renderText(textPos, label, labelEnd, false)
        }

        if (isOpen && flags hasnt Tnf.NoTreePushOnOpen)
            treePushOverrideID(id)
        ImGuiTestEngineHook_ItemInfo(id, label, window.dc.itemFlags or (if (isLeaf) ItemStatusFlag.None else ItemStatusFlag.Openable) or if (isOpen) ItemStatusFlag.Opened else ItemStatusFlag.None)
        return isOpen
    }

    /** Consume previous SetNextTreeNodeOpened() data, if any. May return true when logging */
    fun treeNodeBehaviorIsOpen(id: ID, flags: TreeNodeFlags = 0): Boolean {

        if (flags has Tnf.Leaf) return true

        // We only write to the tree storage if the user clicks (or explicitly use SetNextTreeNode*** functions)
        val window = g.currentWindow!!
        val storage = window.dc.stateStorage

        var isOpen: Boolean
        if (g.nextTreeNodeOpenCond != Cond.None) {
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
            g.nextTreeNodeOpenCond = Cond.None
        } else
            isOpen = storage.int(id, if (flags has Tnf.DefaultOpen) 1 else 0) != 0 // TODO rename back

        /*  When logging is enabled, we automatically expand tree nodes (but *NOT* collapsing headers.. seems like
            sensible behavior).
            NB- If we are above max depth we still allow manually opened nodes to be logged.    */
        if (g.logEnabled && flags hasnt Tnf.NoAutoOpenOnLog && (window.dc.treeDepth - g.logDepthRef) < g.logDepthToExpand)
            isOpen = true

        return isOpen
    }

    fun treePushOverrideID(id: ID) {
        val window = currentWindow
        indent()
        window.dc.treeDepth++
        window.idStack.push(id)
    }

    // Data type helpers
//    IMGUI_API const ImGuiDataTypeInfo*  DataTypeGetInfo(ImGuiDataType data_type);

    /** DataTypeFormatString */
    fun KMutableProperty0<*>.format(dataType: DataType, format: String, size: Int = 0): CharArray {
        // useless
//    val value: Number = when (dataType) {
//        DataType.Int, DataType.Uint -> this() as Int    // Signedness doesn't matter when pushing the argument
//        DataType.Long, DataType.Ulong -> this() as Long // Signedness doesn't matter when pushing the argument
//        DataType.Float -> this() as Float
//        DataType.Double -> this() as Double
//        else -> throw Error()
//    }
        val t = this()
        val string = format.format(style.locale, when (t) {
            // we need to intervene since java printf cant handle %u
            is Ubyte -> t.i
            is Ushort -> t.i
            is Uint -> t.L
            is Ulong -> t.toBigInt()
            else -> t // normal scalar
        })
        return when (size) {
            0 -> string.toCharArray()
            else -> string.toCharArray(CharArray(size))
        }
    }

    fun <N : Number> dataTypeApplyOp(dataType: DataType, op: Char, value1: N, value2: N): N {
        assert(op == '+' || op == '-')
        return when (dataType) {
            /*  Signedness doesn't matter when adding or subtracting
                Note: on jvm Byte and Short (and their unsigned counterparts use all unsigned under the hood),
                so we directly switch to Integers in these cases. We also use some custom clamp min max values because of this
             */
            DataType.Byte -> when (op) {
                '+' -> addClampOverflow((value1 as Byte).i, (value2 as Byte).i, S8_MIN, S8_MAX).b as N
                '-' -> subClampOverflow((value1 as Byte).i, (value2 as Byte).i, S8_MIN, S8_MAX).b as N
                else -> throw Error()
            }
            DataType.Ubyte -> when (op) {
                '+' -> Ubyte(addClampOverflow((value1 as Ubyte).i, (value2 as Ubyte).i, U8_MIN, U8_MAX)) as N
                '-' -> Ubyte(subClampOverflow((value1 as Ubyte).i, (value2 as Ubyte).i, U8_MIN, U8_MAX)) as N
                else -> throw Error()
            }
            DataType.Short -> when (op) {
                '+' -> addClampOverflow((value1 as Short).i, (value2 as Short).i, S16_MIN, S16_MAX).s as N
                '-' -> subClampOverflow((value1 as Short).i, (value2 as Short).i, S16_MIN, S16_MAX).s as N
                else -> throw Error()
            }
            DataType.Ushort -> when (op) {
                '+' -> Ushort(addClampOverflow((value1 as Ushort).i, (value2 as Ushort).i, U16_MIN, U16_MAX)) as N
                '-' -> Ushort(subClampOverflow((value1 as Ushort).i, (value2 as Ushort).i, U16_MIN, U16_MAX)) as N
                else -> throw Error()
            }
            DataType.Int -> when (op) {
                '+' -> addClampOverflow(value1 as Int, value2 as Int, Int.MIN_VALUE, Int.MAX_VALUE) as N
                '-' -> subClampOverflow(value1 as Int, value2 as Int, Int.MIN_VALUE, Int.MAX_VALUE) as N
                else -> throw Error()
            }
            DataType.Uint -> when (op) {
                '+' -> Uint(addClampOverflow((value1 as Uint).L, (value2 as Uint).L, 0L, Uint.MAX_VALUE)) as N
                '-' -> Uint(subClampOverflow((value1 as Uint).L, (value2 as Uint).L, 0L, Uint.MAX_VALUE)) as N
                else -> throw Error()
            }
            DataType.Long -> when (op) {
                '+' -> addClampOverflow(value1 as Long, value2 as Long, Long.MIN_VALUE, Long.MAX_VALUE) as N
                '-' -> subClampOverflow(value1 as Long, value2 as Long, Long.MIN_VALUE, Long.MAX_VALUE) as N
                else -> throw Error()
            }
            DataType.Ulong -> when (op) {
                '+' -> Ulong(addClampOverflow((value1 as Ulong).toBigInt(), (value2 as Ulong).toBigInt(), Ulong.MIN_VALUE, Ulong.MAX_VALUE)) as N
                '-' -> Ulong(subClampOverflow((value1 as Ulong).toBigInt(), (value2 as Ulong).toBigInt(), Ulong.MIN_VALUE, Ulong.MAX_VALUE)) as N
                else -> throw Error()
            }
            DataType.Float -> when (op) {
                '+' -> (value1 as Float + value2 as Float) as N
                '-' -> (value1 as Float - value2 as Float) as N
                else -> throw Error()
            }
            DataType.Double -> when (op) {
                '+' -> (value1 as Double + value2 as Double) as N
                '-' -> (value1 as Double - value2 as Double) as N
                else -> throw Error()
            }
        }
    }

    /** User can input math operators (e.g. +100) to edit a numerical values.
     *  NB: This is _not_ a full expression evaluator. We should probably add one and replace this dumb mess.. */
    fun dataTypeApplyOpFromText(buf: CharArray, initialValueBuf: CharArray, dataType: DataType, data: IntArray, format: String? = null): Boolean {
        _i = data[0]
        return dataTypeApplyOpFromText(buf, initialValueBuf, dataType, ::_i, format)
                .also { data[0] = _i }
    }

    fun dataTypeApplyOpFromText(buf_: CharArray, initialValueBuf_: CharArray, dataType: DataType, dataPtr: KMutableProperty0<*>,
                                format: String? = null): Boolean {

        val buf = String(buf_)
                .replace(Regex("\\s+"), "")
                .replace("$NUL", "")
                .split(Regex("-+\\*/"))

        val initialValueBuf = String(initialValueBuf_)
                .replace(Regex("\\s+"), "")
                .replace("$NUL", "")
                .split(Regex("-+\\*/"))

        /*  We don't support '-' op because it would conflict with inputing negative value.
            Instead you can use +-100 to subtract from an existing value     */
        val op = buf.getOrNull(1)?.get(0)

        return when (buf_[0]) {
            NUL -> false
            else -> when (dataType) {
                DataType.Int -> {
                    val fmt = format ?: "%d"
                    var v by dataPtr as KMutableProperty0<Int>
                    val dataBackup = v
                    val arg0i = try {
                        buf[0].format(style.locale, fmt).i
                    } catch (_: Exception) {
                        return false
                    }

                    v = when (op) {
                        '+' -> {    // Add (use "+-" to subtract)
                            val arg1i = buf[2].format(style.locale, "%d").i
                            (arg0i + arg1i).i
                        }
                        '*' -> {    // Multiply
                            val arg1f = buf[2].format(style.locale, "%f").f
                            (arg0i * arg1f).i
                        }
                        '/' -> {    // Divide
                            val arg1f = buf[2].format(style.locale, "%f").f
                            when (arg1f) {
                                0f -> arg0i
                                else -> (arg0i / arg1f).i
                            }
                        }
                        else -> try { // Assign constant
                            buf[1].format(style.locale, fmt).i
                        } catch (_: Exception) {
                            arg0i
                        }
                    }
                    dataBackup != v
                }
                DataType.Float -> {
                    // For floats we have to ignore format with precision (e.g. "%.2f") because sscanf doesn't take them in TODO not true in java
                    val fmt = format ?: "%f"
                    var v by dataPtr as KMutableProperty0<Float>
                    val dataBackup = v
                    val arg0f = try {
                        initialValueBuf[0].format(style.locale, fmt).f
                    } catch (_: Exception) {
                        return false
                    }
                    val arg1f = try {
                        buf.getOrElse(2) { buf[0] }.format(style.locale, fmt).f
                    } catch (_: Exception) {
                        return false
                    }
                    v = when (op) {
                        '+' -> arg0f + arg1f    // Add (use "+-" to subtract)
                        '*' -> arg0f * arg1f    // Multiply
                        '/' -> when (arg1f) {   // Divide
                            0f -> arg0f
                            else -> arg0f / arg1f
                        }
                        else -> arg1f           // Assign constant
                    }
                    dataBackup != v
                }
                DataType.Double -> {
                    val fmt = format ?: "%f"
                    var v by dataPtr as KMutableProperty0<Double>
                    val dataBackup = v
                    val arg0f = try {
                        buf[0].format(style.locale, fmt).d
                    } catch (_: Exception) {
                        return false
                    }
                    val arg1f = try {
                        buf[2].format(style.locale, fmt).d
                    } catch (_: Exception) {
                        return false
                    }
                    v = when (op) {
                        '+' -> arg0f + arg1f    // Add (use "+-" to subtract)
                        '*' -> arg0f * arg1f    // Multiply
                        '/' -> when (arg1f) {   // Divide
                            0.0 -> arg0f
                            else -> arg0f / arg1f
                        }
                        else -> arg1f           // Assign constant
                    }
                    dataBackup != v
                }
                DataType.Uint, DataType.Long, DataType.Ulong ->
                    /*  Assign constant
                        FIXME: We don't bother handling support for legacy operators since they are a little too crappy.
                        Instead we may implement a proper expression evaluator in the future.                 */
                    //sscanf(buf, format, data_ptr)
                    false
                else -> false
//            else TODO
//            {
//                // Small types need a 32-bit buffer to receive the result from scanf()
//                int v32;
//                sscanf(buf, format, &v32);
//                if (data_type == ImGuiDataType_S8)
//                *(ImS8*)data_ptr = (ImS8)ImClamp(v32, (int)IM_S8_MIN, (int)IM_S8_MAX);
//                else if (data_type == ImGuiDataType_U8)
//                *(ImU8*)data_ptr = (ImU8)ImClamp(v32, (int)IM_U8_MIN, (int)IM_U8_MAX);
//                else if (data_type == ImGuiDataType_S16)
//                *(ImS16*)data_ptr = (ImS16)ImClamp(v32, (int)IM_S16_MIN, (int)IM_S16_MAX);
//                else if (data_type == ImGuiDataType_U16)
//                *(ImU16*)data_ptr = (ImU16)ImClamp(v32, (int)IM_U16_MIN, (int)IM_U16_MAX);
//                else
//                IM_ASSERT(0);
//            }
            }
        }
    }

    // InputText

    /** InputTextEx
     *  - bufSize account for the zero-terminator, so a buf_size of 6 can hold "Hello" but not "Hello!".
     *    This is so we can easily call InputText() on static arrays using ARRAYSIZE() and to match
     *    Note that in std::string world, capacity() would omit 1 byte used by the zero-terminator.
     *  - When active, hold on a privately held copy of the text (and apply back to 'buf'). So changing 'buf' while the InputText is active has no effect.
     *  - If you want to use ImGui::InputText() with std::string, see misc/cpp/imgui_stl.h
     *  (FIXME: Rather confusing and messy function, among the worse part of our codebase, expecting to rewrite a V2 at some point.. Partly because we are
     *  doing UTF8 > U16 > UTF8 conversions on the go to easily interface with stb_textedit. Ideally should stay in UTF-8 all the time. See https://github.com/nothings/stb/issues/188)
     */
    fun inputTextEx(label: String, hint: String?, buf: CharArray, sizeArg: Vec2, flags: InputTextFlags,
                    callback: InputTextCallback? = null, callbackUserData: Any? = null): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        // Can't use both together (they both use up/down keys)
        assert(!((flags has Itf.CallbackHistory) && (flags has Itf.Multiline)))
        // Can't use both together (they both use tab key)
        assert(!((flags has Itf.CallbackCompletion) && (flags has Itf.AllowTabInput)))

        val RENDER_SELECTION_WHEN_INACTIVE = false
        val isMultiline = flags has Itf.Multiline
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
        val id = window.getId(label)
        val labelSize = calcTextSize(label, 0, true)
        val size = calcItemSize(sizeArg, nextItemWidth,
                // Arbitrary default of 8 lines high for multi-line
                (if (isMultiline) textLineHeight * 8f else labelSize.y) + style.framePadding.y * 2f)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        val totalBb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))

        var drawWindow = window
        if (isMultiline) {
            if (!itemAdd(totalBb, id, frameBb)) {
                itemSize(totalBb, style.framePadding.y)
                endGroup()
                return false
            }
            if (!beginChildFrame(id, frameBb.size)) {
                endChildFrame()
                endGroup()
                return false
            }
            drawWindow = currentWindow
            // This is to ensure that EndChild() will display a navigation highlight
            drawWindow.dc.navLayerActiveMaskNext = drawWindow.dc.navLayerActiveMaskNext or drawWindow.dc.navLayerCurrentMask
            size.x -= drawWindow.scrollbarSizes.x
        } else {
            itemSize(totalBb, style.framePadding.y)
            if (!itemAdd(totalBb, id, frameBb)) return false
        }
        val hovered = itemHoverable(frameBb, id)
        if (hovered) g.mouseCursor = MouseCursor.TextInput

        // NB: we are only allowed to access 'editState' if we are the active widget.
        var state_: TextEditState? = g.inputTextState.takeIf { it.id == id }

        val focusRequested = focusableItemRegister(window, id)
        val focusRequestedByCode = focusRequested && g.focusRequestCurrWindow === window && g.focusRequestCurrCounterAll == window.dc.focusCounterAll
        val focusRequestedByTab = focusRequested && !focusRequestedByCode

        val userClicked = hovered && io.mouseClicked[0]
        val userNavInputStart = g.activeId != id && (g.navInputId == id || (g.navActivateId == id && g.navInputSource == InputSource.NavKeyboard))
        val userScrollFinish = isMultiline && state_ != null && g.activeId == 0 && g.activeIdPreviousFrame == getScrollbarID(drawWindow, Axis.Y)
        val userScrollActive = isMultiline && state_ != null && g.activeId == getScrollbarID(drawWindow, Axis.Y)

        var clearActiveId = false
        var selectAll = g.activeId != id && (flags has Itf.AutoSelectAll || userNavInputStart) && !isMultiline

//        println(g.imeLastKey)
        val initMakeActive = focusRequested || userClicked || userScrollFinish || userNavInputStart
        val initState = initMakeActive || userScrollActive
        if (initState && g.activeId != id) {
            // Access state even if we don't own it yet.
            state_ = g.inputTextState
            state_.cursorAnimReset()

            // Take a copy of the initial buffer value (both in original UTF-8 format and converted to wchar)
            // From the moment we focused we are ignoring the content of 'buf' (unless we are in read-only mode)
            val bufLen = buf.strlen
            state_.initialTextA = CharArray(bufLen)   // UTF-8. we use +1 to make sure that .Data is always pointing to at least an empty string.
            System.arraycopy(buf, 0, state_.initialTextA, 0, bufLen)

            // Start edition
            state_.textW = CharArray(buf.size)   // wchar count <= UTF-8 count. we use +1 to make sure that .Data is always pointing to at least an empty string.
            state_.textA = CharArray(0)
            state_.textAIsValid = false // TextA is not valid yet (we will display buf until then)

            state_.curLenW = state_.textW.textStr(buf) // TODO check if ImTextStrFromUtf8 needed
            /*  We can't get the result from ImStrncpy() above because it is not UTF-8 aware.
                Here we'll cut off malformed UTF-8.                 */
            state_.curLenA = state_.curLenW //TODO check (int)(bufEnd - buf)
            state_.cursorAnimReset()

            /*  Preserve cursor position and undo/redo stack if we come back to same widget
                For non-readonly widgets we might be able to require that TextAIsValid && TextA == buf ? (untested) and discard undo stack if user buffer has changed. */
            val recycleState = state_.id == id
            if (recycleState)
            /*  Recycle existing cursor/selection/undo stack but clamp position
                Note a single mouse click will override the cursor/position immediately by calling
                stb_textedit_click handler.                     */
                state_.cursorClamp()
            else {
                state_.id = id
                state_.scrollX = 0f
                state_.stb.clear(!isMultiline)
                if (!isMultiline && focusRequestedByCode)
                    selectAll = true
            }
            if (flags has Itf.AlwaysInsertMode)
                state_.stb.insertMode = true
            if (!isMultiline && (focusRequestedByTab || (userClicked && io.keyCtrl)))
                selectAll = true
        }

        if (g.activeId != id && initMakeActive) {
            assert(state_!!.id == id)
            setActiveId(id, window)
            setFocusId(id, window)
            window.focus()
            assert(NavInput.values().size < 32)
            g.activeIdBlockNavInputFlags = 1 shl NavInput.Cancel
            if (flags has (Itf.CallbackCompletion or Itf.AllowTabInput))  // Disable keyboard tabbing out as we will use the \t character.
                g.activeIdBlockNavInputFlags = g.activeIdBlockNavInputFlags or (1 shl NavInput.KeyTab.i)
            if (!isMultiline && flags hasnt Itf.CallbackHistory)
                g.activeIdAllowNavDirFlags = (1 shl Dir.Up) or (1 shl Dir.Down)
        }

        // We have an edge case if ActiveId was set through another widget (e.g. widget being swapped), clear id immediately (don't wait until the end of the function)
        if (g.activeId == id && state_ == null)
            clearActiveId()

        // Release focus when we click outside
        if (g.activeId == id && io.mouseClicked[0] && !initState && !initMakeActive)
            clearActiveId = true

        // Lock the decision of whether we are going to take the path displaying the cursor or selection
        val renderCursor = g.activeId == id || (state_ != null && userScrollActive)
        var renderSelection = state_?.hasSelection == true && (RENDER_SELECTION_WHEN_INACTIVE || renderCursor)
        var valueChanged = false
        var enterPressed = false

        // When read-only we always use the live data passed to the function
        // FIXME-OPT: Because our selection/cursor code currently needs the wide text we need to convert it when active, which is not ideal :(
        if (isReadOnly && state_ != null && (renderCursor || renderSelection)) {
            val tmp = CharArray(buf.size) // TODO resize()?
            System.arraycopy(state_.textW, 0, tmp, 0, state_.textW.size)
            state_.curLenW = state_.textW.textStr(buf) // TODO check
            state_.curLenA = state_.curLenW // TODO check
            state_.cursorClamp()
            renderSelection = renderSelection && state_.hasSelection
        }

        // Select the buffer to render.
        val bufDisplayFromState = (renderCursor || renderSelection || g.activeId == id) && !isReadOnly && state_?.textAIsValid == true
        val isDisplayingHint = hint != null && (if (bufDisplayFromState) state_!!.textA else buf)[0] == NUL

        // Password pushes a temporary font with only a fallback glyph
        if (isPassword)
            g.inputTextPasswordFont.apply {
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

        // Process mouse inputs and character inputs
        var backupCurrentTextLength = 0
        if (g.activeId == id) {
            val state = state_!!

            backupCurrentTextLength = state.curLenA
            state.apply {
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
            val mouseY =
                    if (isMultiline)
                        io.mousePos.y - drawWindow.dc.cursorPos.y - style.framePadding.y
                    else g.fontSize * 0.5f

            // OS X style: Double click selects by word instead of selecting whole text
            val isOsx = io.configMacOSXBehaviors
            if (selectAll || (hovered && !isOsx && io.mouseDoubleClicked[0])) {
                state.selectAll()
                state.selectedAllMouseLock = true
            } else if (hovered && isOsx && io.mouseDoubleClicked[0]) {
                // Double-click select a word only, OS X style (by simulating keystrokes)
                state.onKeyPressed(K.WORDLEFT)
                state.onKeyPressed(K.WORDRIGHT or K.SHIFT)
            } else if (io.mouseClicked[0] && !state.selectedAllMouseLock) {
                if (hovered) {
                    state.click(mouseX, mouseY)
                    state.cursorAnimReset()
                }
            } else if (io.mouseDown[0] && !state.selectedAllMouseLock && io.mouseDelta anyNotEqual 0f) {
                state.stb.selectStart = state.stb.cursor
                state.stb.selectEnd = state.locateCoord(mouseX, mouseY)
                state.cursorFollow = true
                state.cursorAnimReset()
            }
            if (state.selectedAllMouseLock && !io.mouseDown[0])
                state.selectedAllMouseLock = false

            // It is ill-defined whether the back-end needs to send a \t character when pressing the TAB keys.
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
            if (io.inputQueueCharacters.size > 0)
            /*if (io.inputQueueCharacters[0] != NUL) I cant explaing why JVM had this TODO check */ {
                if (!ignoreCharInputs && !isReadOnly && !userNavInputStart)
                    io.inputQueueCharacters.filter { it != NUL || (it == '\t' && io.keyShift) }.map {
                        // TODO check
                        withChar { c ->
                            // Insert character if they pass filtering
                            if (inputTextFilterCharacter(c.apply { set(it) }, flags, callback, callbackUserData))
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
            val state = state_!!
            val kMask = if (io.keyShift) K.SHIFT else 0
            val isOsx = io.configMacOSXBehaviors
            // OS X style: Shortcuts using Cmd/Super instead of Ctrl
            val isShortcutKey = (if (isOsx) io.keySuper && !io.keyCtrl else io.keyCtrl && !io.keySuper) && !io.keyAlt && !io.keyShift
            val isOsxShiftShortcut = isOsx && io.keySuper && io.keyShift && !io.keyCtrl && !io.keyAlt
            val isWordmoveKeyDown = if (isOsx) io.keyAlt else io.keyCtrl // OS X style: Text editing cursor movement using Alt instead of Ctrl
            // OS X style: Line/Text Start and End using Cmd+Arrows instead of Home/End
            val isStartendKeyDown = isOsx && io.keySuper && !io.keyCtrl && !io.keyAlt
            val isCtrlKeyOnly = io.keyCtrl && !io.keyShift && !io.keyAlt && !io.keySuper
            val isShiftKeyOnly = io.keyShift && !io.keyCtrl && !io.keyAlt && !io.keySuper

            val isCut = ((isShortcutKey && Key.X.isPressed) || (isShiftKeyOnly && Key.Delete.isPressed)) && !isReadOnly && !isPassword && (!isMultiline || state.hasSelection)
            val isCopy = ((isShortcutKey && Key.C.isPressed) || (isCtrlKeyOnly && Key.Insert.isPressed)) && !isPassword && (!isMultiline || state.hasSelection)
            val isPaste = ((isShortcutKey && Key.V.isPressed) || (isShiftKeyOnly && Key.Insert.isPressed)) && !isReadOnly
            val isUndo = ((isShortcutKey && Key.Z.isPressed) && !isReadOnly && isUndoable)
            val isRedo = ((isShortcutKey && Key.Y.isPressed) || (isOsxShiftShortcut && Key.Z.isPressed)) && !isReadOnly && isUndoable

            when {
                Key.LeftArrow.isPressed -> state.onKeyPressed(when {
                    isStartendKeyDown -> K.LINESTART
                    isWordmoveKeyDown -> K.WORDLEFT
                    else -> K.LEFT
                } or kMask)
                Key.RightArrow.isPressed -> state.onKeyPressed(when {
                    isStartendKeyDown -> K.LINEEND
                    isWordmoveKeyDown -> K.WORDRIGHT
                    else -> K.RIGHT
                } or kMask)
                Key.UpArrow.isPressed && isMultiline ->
                    if (io.keyCtrl)
                        drawWindow.setScrollY(glm.max(drawWindow.scroll.y - g.fontSize, 0f))
                    else
                        state.onKeyPressed((if (isStartendKeyDown) K.TEXTSTART else K.UP) or kMask)
                Key.DownArrow.isPressed && isMultiline ->
                    if (io.keyCtrl)
                        drawWindow.setScrollY(glm.min(drawWindow.scroll.y + g.fontSize, scrollMaxY))
                    else
                        state.onKeyPressed((if (isStartendKeyDown) K.TEXTEND else K.DOWN) or kMask)
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
                Key.Enter.isPressed -> {
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
                        val ie = if (state.hasSelection) max(state.stb.selectStart, state.stb.selectEnd) else state.curLenW
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
                    for(c in clipboard) {
                        if (c == NUL)
                            break
                        _c = c
                        if (_c >= 0x10000 || !inputTextFilterCharacter(::_c, flags, callback, callbackUserData))
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
            val state = state_!!
            var applyNewText = CharArray(0)
//            var applyNewTextPtr = 0
            var applyNewTextLength = 0

            if (cancelEdit)
            // Restore initial value. Only return true if restoring to the initial value changes the current buffer contents.
                if (!isReadOnly && !Arrays.equals(buf, state.initialTextA)) {
                    applyNewText = state.initialTextA
                    applyNewTextLength = state.initialTextA.size
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
                if (!isReadOnly) {
                    state.textAIsValid = true
                    state.textA = CharArray(state.textW.size * 4) { state.textW.getOrElse(it) { NUL } }
                }

                // User callback
                if (flags has (Itf.CallbackCompletion or Itf.CallbackHistory or Itf.CallbackAlways)) {
                    callback!!
                    val (eventFlag, eventKey) = when {
                        (flags has imgui.InputTextFlag.CallbackCompletion) and Key.Tab.isPressed -> Pair(imgui.InputTextFlag.CallbackCompletion.i, Key.Tab)
                        (flags has imgui.InputTextFlag.CallbackHistory) and Key.UpArrow.isPressed -> Pair(imgui.InputTextFlag.CallbackHistory.i, Key.UpArrow)
                        (flags has imgui.InputTextFlag.CallbackHistory) and Key.DownArrow.isPressed -> Pair(imgui.InputTextFlag.CallbackHistory.i, Key.DownArrow)
                        flags has imgui.InputTextFlag.CallbackAlways -> Pair(imgui.InputTextFlag.CallbackAlways.i, Key.Count)
                        else -> Pair(0, Key.Count)
                    }

                    if (eventFlag != 0) {
                        val cbData = TextEditCallbackData()
                        cbData.eventFlag = eventFlag
                        cbData.flags = flags
                        cbData.userData = callbackUserData

                        cbData.eventKey = eventKey
                        cbData.buf = state.textA
                        cbData.bufTextLen = state.curLenA
                        cbData.bufSize = state.bufCapacityA
                        cbData.bufDirty = false

                        val cursorPos = state.stb.cursor
                        val selectionStart = state.stb.selectStart
                        val selectionEnd = state.stb.selectEnd

                        cbData.cursorPos = cursorPos
                        cbData.selectionStart = selectionStart
                        cbData.selectionEnd = selectionEnd

                        callback.invoke(cbData)

                        assert(cbData.bufSize == state.bufCapacityA)
                        assert(cbData.flags == flags)

                        if (cbData.cursorPos != cursorPos) {
                            state.stb.cursor = cbData.cursorPos
                        }
                        if (cbData.selectionStart != selectionStart) {
                            state.stb.selectStart = cbData.selectionStart
                        }
                        if (cbData.selectionEnd != selectionEnd) {
                            state.stb.selectEnd = cbData.selectionEnd
                        }
                        if (cbData.bufDirty) {
                            assert(cbData.bufTextLen == cbData.buf.strlen)
                            if ((cbData.bufTextLen > backupCurrentTextLength) and isResizable)
                                TODO("pass a reference to buf and bufSize")
                            //TODO: Hacky
                            state.deleteChars(0, cursorPos)
                            state.insertChars(0, cbData.buf, 0, cbData.bufTextLen)
                            state.cursorAnimReset()
                        }
                    }
                }
                // Will copy result string if modified
                if (!isReadOnly && !state.textA.cmp(buf)) {
                    applyNewText = state.textA
                    applyNewTextLength = state.curLenA
                }
            }

            // Copy result to user buffer
            if (applyNewText.isNotEmpty()) {
                assert(applyNewTextLength >= 0)
                if (backupCurrentTextLength != applyNewTextLength && isResizable) {
                    TODO("pass a reference to buf and bufSize")
//                    val callbackData = InputTextCallbackData().apply {
//                        eventFlag = Itf.CallbackResize.i
//                        this.flags = flags
//                        this.buf = buf
//                        bufTextLen = apply_new_text_length
////                        bufSize = max(bufSize, applyNewTextLength)
//                        userData = callbackUserData
//                    }
//                    callback!!(callbackData)
//                    buf = callback_data.Buf
//                    buf_size = callback_data.BufSize
//                    apply_new_text_length = ImMin(callback_data.BufTextLen, buf_size - 1);
//                    IM_ASSERT(apply_new_text_length <= buf_size);
                }
                /*  If the underlying buffer resize was denied or not carried to the next frame,
                    apply_new_text_length+1 may be >= buf_size.                 */
                buf.strncpy(applyNewText, applyNewTextLength min buf.size)
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
        if (clearActiveId && g.activeId == id) clearActiveId()

        // Render frame
        if (!isMultiline) {
            renderNavHighlight(frameBb, id)
            renderFrame(frameBb.min, frameBb.max, Col.FrameBg.u32, true, style.frameRounding)
        }

        val clipRect = Vec4(frameBb.min, frameBb.min + size) // Not using frameBb.Max because we have adjusted size
        val drawPos = if (isMultiline) Vec2(drawWindow.dc.cursorPos) else frameBb.min + style.framePadding
        val textSize = Vec2()

        /*  Set upper limit of single-line InputTextEx() at 2 million characters strings. The current pathological worst case is a long line
            without any carriage return, which would makes ImFont::RenderText() reserve too many vertices and probably crash. Avoid it altogether.
            Note that we only use this limit on single-line InputText(), so a pathologically large line on a InputTextMultiline() would still crash. */
        val bufDisplayMaxLength = 2 * 1024 * 1024
        var bufDisplay = if (bufDisplayFromState) state_!!.textA else buf
        val bufDisplayEnd = IntArray(1) // We have specialized paths below for setting the length
        if (isDisplayingHint) {
            bufDisplay = hint!!.toCharArray()
            bufDisplayEnd[0] = hint.length
        }

        // Render text. We currently only render selection when the widget is active or while scrolling.
        // FIXME: We could remove the '&& render_cursor' to keep rendering selection when inactive.
        if (renderCursor || renderSelection) {

            val state = state_!! // ~assert
            if (!isDisplayingHint)
                bufDisplayEnd[0] = state.curLenA

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
                val searchesInputPtr = IntArray(2)
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
                        if (searchesResultLineNo[0] == -1 && s >= searchesInputPtr[0]) {
                            searchesResultLineNo[0] = lineCount
                            if (--searchesRemaining <= 0) break
                        }
                        if (searchesResultLineNo[1] == -1 && s >= searchesInputPtr[1]) {
                            searchesResultLineNo[1] = lineCount
                            if (--searchesRemaining <= 0) break
                        }
                    }
                lineCount++
                if (searchesResultLineNo[0] == -1)
                    searchesResultLineNo[0] = lineCount
                if (searchesResultLineNo[1] == -1)
                    searchesResultLineNo[1] = lineCount

                // Calculate 2d position by finding the beginning of the line and measuring distance
                var start = text.beginOfLine(searchesInputPtr[0])
                cursorOffset.x = inputTextCalcTextSizeW(text, start, searchesInputPtr[0]).x
                cursorOffset.y = searchesResultLineNo[0] * g.fontSize
                if (searchesResultLineNo[1] >= 0) {
                    start = text.beginOfLine(searchesInputPtr[1])
                    selectStartOffset.x = inputTextCalcTextSizeW(text, start, searchesInputPtr[1]).x
                    selectStartOffset.y = searchesResultLineNo[1] * g.fontSize
                }

                // Store text height (note that we haven't calculated text width at all, see GitHub issues #383, #1224)
                if (isMultiline)
                    textSize.put(size.x, lineCount * g.fontSize)
            }

            // Scroll
            if (renderCursor && state.cursorFollow) {
                // Horizontal scroll in chunks of quarter width
                if (flags hasnt Itf.NoHorizontalScroll) {
                    val scrollIncrementX = size.x * 0.25f
                    if (cursorOffset.x < state.scrollX)
                        state.scrollX = (glm.max(0f, cursorOffset.x - scrollIncrementX)).i.f
                    else if (cursorOffset.x - size.x >= state.scrollX)
                        state.scrollX = (cursorOffset.x - size.x + scrollIncrementX).i.f
                } else
                    state.scrollX = 0f

                // Vertical scroll
                if (isMultiline) {
                    var scrollY = drawWindow.scroll.y
                    if (cursorOffset.y - g.fontSize < scrollY)
                        scrollY = glm.max(0f, cursorOffset.y - g.fontSize)
                    else if (cursorOffset.y - size.y >= scrollY)
                        scrollY = cursorOffset.y - size.y
                    drawWindow.dc.cursorPos.y += drawWindow.scroll.y - scrollY   // Manipulate cursor pos immediately avoid a frame of lag
                    drawWindow.scroll.y = scrollY
                    drawPos.y = drawWindow.dc.cursorPos.y
                }

                state.cursorFollow = false
            }

            // Draw selection
            val drawScroll = Vec2(state.scrollX, 0f)
            if (renderSelection) {

                val textSelectedBegin = glm.min(state.stb.selectStart, state.stb.selectEnd)
                val textSelectedEnd = glm.max(state.stb.selectStart, state.stb.selectEnd)

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
                        if (rectSize.x <= 0f) rectSize.x = (g.font.getCharAdvance(' ') * 0.5f).i.f
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
            if (isMultiline || bufDisplayEnd[0] < bufDisplayMaxLength) {
                val col = getColorU32(if (isDisplayingHint) Col.TextDisabled else Col.Text)
                drawWindow.drawList.addText(g.font, g.fontSize, drawPos - drawScroll, col, bufDisplay, bufDisplayEnd[0], 0f, clipRect.takeUnless { isMultiline })
            }

            // Draw blinking cursor
            if (renderCursor) {
                state.cursorAnim += io.deltaTime
                val cursorIsVisible = !io.configInputTextCursorBlink || state.cursorAnim <= 0f || glm.mod(state.cursorAnim, 1.2f) <= 0.8f
                val cursorScreenPos = drawPos + cursorOffset - drawScroll
                val cursorScreenRect = Rect(cursorScreenPos.x, cursorScreenPos.y - g.fontSize + 0.5f, cursorScreenPos.x + 1f, cursorScreenPos.y - 1.5f)
                if (cursorIsVisible && cursorScreenRect overlaps clipRect)
                    drawWindow.drawList.addLine(cursorScreenRect.min, cursorScreenRect.bl, Col.Text.u32)

                // Notify OS of text input position for advanced IME (-1 x offset so that Windows IME can cover our cursor. Bit of an extra nicety.)
                if (!isReadOnly)
                    g.platformImePos.put(cursorScreenPos.x - 1f, cursorScreenPos.y - g.fontSize)
            }
        } else {
            // Render text only (no selection, no cursor)
            if (isMultiline)
                textSize.put(size.x, inputTextCalcTextLenAndLineCount(bufDisplay.contentToString(), bufDisplayEnd) * g.fontSize) // We don't need width
            else if (!isDisplayingHint && g.activeId == id)
                bufDisplayEnd[0] = state_!!.curLenA
            else if (!isDisplayingHint)
                bufDisplayEnd[0] = bufDisplay.strlen

            if (isMultiline || bufDisplayEnd[0] < bufDisplayMaxLength) {
                val col = getColorU32(if (isDisplayingHint) Col.TextDisabled else Col.Text)
                drawWindow.drawList.addText(g.font, g.fontSize, drawPos, col, bufDisplay, bufDisplayEnd[0], 0f, clipRect.takeUnless { isMultiline })
            }
        }

        if (isMultiline) {
            dummy(textSize + Vec2(0f, g.fontSize)) // Always add room to scroll an extra line
            endChildFrame()
            endGroup()
        }

        if (isPassword && !isDisplayingHint)
            popFont()

        // Log as text
        if (g.logEnabled && !(isPassword && !isDisplayingHint))
            logRenderedText(drawPos, String(bufDisplay), bufDisplayEnd[0])

        if (labelSize.x > 0)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, frameBb.min.y + style.framePadding.y), label)

        if (valueChanged)
            markItemEdited(id)

        ImGuiTestEngineHook_ItemInfo(id, label, window.dc.itemFlags)
        return when {
            flags has Itf.EnterReturnsTrue -> enterPressed
            else -> valueChanged
        }
    }

    /** Create text input in place of another active widget (e.g. used when doing a CTRL+Click on drag/slider widgets)
     *  FIXME: Facilitate using this in variety of other situations. */
    fun tempInputTextScalar(bb: Rect, id: ID, label: String, dataType: DataType, data: KMutableProperty0<*>,
                            format_: String): Boolean {

        // On the first frame, g.TempInputTextId == 0, then on subsequent frames it becomes == id.
        // We clear ActiveID on the first frame to allow the InputText() taking it back.
        val init = g.tempInputTextId != 0
        if (init)
            clearActiveId()

        val fmtBuf = CharArray(32)
        val format = parseFormatTrimDecorations(format_, fmtBuf)
        var dataBuf = data.format(dataType, format, 32)
        dataBuf = trimBlanks(dataBuf)
        g.currentWindow!!.dc.cursorPos put bb.min
        val flags = Itf.AutoSelectAll or when (dataType) {
            DataType.Float, DataType.Double -> Itf.CharsScientific
            else -> Itf.CharsDecimal
        }
        val valueChanged = inputTextEx(label, null, dataBuf, bb.size, flags)
        if (init) {
            assert(g.activeId == id) { "First frame we started displaying the InputText widget, we expect it to take the active id." }
            g.tempInputTextId = g.activeId
        }
        return when {
            valueChanged -> dataTypeApplyOpFromText(dataBuf, g.inputTextState.initialTextA, dataType, data)
            else -> false
        }
    }

    fun tempInputTextIsActive(id: ID): Boolean = g.activeId == id && g.tempInputTextId == id

    // Color

    /** Note: only access 3 floats if ColorEditFlag.NoAlpha flag is set.   */
    fun colorTooltip(text: String, col: FloatArray, flags: ColorEditFlags) {

        beginTooltipEx(0, true)
        val textEnd = if (text.isEmpty()) findRenderedTextEnd(text) else 0
        if (textEnd > 0) {
            textEx(text, textEnd)
            separator()
        }
        val sz = Vec2(g.fontSize * 3 + style.framePadding.y * 2)
        val cf = Vec4(col[0], col[1], col[2], if (flags has Cef.NoAlpha) 1f else col[3])
        val cr = F32_TO_INT8_SAT(col[0])
        val cg = F32_TO_INT8_SAT(col[1])
        val cb = F32_TO_INT8_SAT(col[2])
        val ca = if (flags has Cef.NoAlpha) 255 else F32_TO_INT8_SAT(col[3])
        colorButton("##preview", cf, (flags and (Cef._InputMask or Cef.NoAlpha or Cef.AlphaPreview or Cef.AlphaPreviewHalf)) or Cef.NoTooltip, sz)
        sameLine()
        if (flags has Cef.InputRGB || flags hasnt Cef._InputMask)
            if (flags has Cef.NoAlpha)
                text("#%02X%02X%02X\nR: $cr, G: $cg, B: $cb\n(%.3f, %.3f, %.3f)", cr, cg, cb, col[0], col[1], col[2])
            else
                text("#%02X%02X%02X%02X\nR:$cr, G:$cg, B:$cb, A:$ca\n(%.3f, %.3f, %.3f, %.3f)", cr, cg, cb, ca, col[0], col[1], col[2], col[3])
        else if (flags has Cef.InputHSV)
            if (flags has Cef.NoAlpha)
                text("H: %.3f, S: %.3f, V: %.3f", col[0], col[1], col[2])
            else
                text("H: %.3f, S: %.3f, V: %.3f, A: %.3f", col[0], col[1], col[2], col[3])

        endTooltip()
    }

    /** @param flags ColorEditFlags */
    fun colorEditOptionsPopup(col: FloatArray, flags: ColorEditFlags) {
        val allowOptInputs = flags hasnt Cef._DisplayMask
        val allowOptDatatype = flags hasnt Cef._DataTypeMask
        if ((!allowOptInputs && !allowOptDatatype) || !beginPopup("context")) return
        var opts = g.colorEditOptions
        if (allowOptInputs) {
            if (radioButton("RGB", opts has Cef.DisplayRGB))
                opts = (opts wo Cef._DisplayMask) or Cef.DisplayRGB
            if (radioButton("HSV", opts has Cef.DisplayHSV))
                opts = (opts wo Cef._DisplayMask) or Cef.DisplayHSV
            if (radioButton("HEX", opts has Cef.DisplayHEX))
                opts = (opts wo Cef._DisplayMask) or Cef.DisplayHEX
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
                clipboardText = buf
            buf = "(%d,%d,%d,%d)".format(cr, cg, cb, ca)
            if (selectable(buf))
                clipboardText = buf
            buf = when {
                flags has Cef.NoAlpha -> "0x%02X%02X%02X".format(cr, cg, cb)
                else -> "0x%02X%02X%02X%02X".format(cr, cg, cb, ca)
            }
            if (selectable(buf))
                clipboardText = buf
            endPopup()
        }

        g.colorEditOptions = opts
        endPopup()
    }

    fun colorPickerOptionsPopup(refCol: FloatArray, flags: ColorEditFlags) {
        val allowOptPicker = flags hasnt Cef._PickerMask
        val allowOptAlphaBar = flags hasnt Cef.NoAlpha && flags hasnt Cef.AlphaBar
        if ((!allowOptPicker && !allowOptAlphaBar) || !beginPopup("context")) return
        if (allowOptPicker) {
            // FIXME: Picker size copied from main picker function
            val pickerSize = Vec2(g.fontSize * 8, glm.max(g.fontSize * 8 - (frameHeight + style.itemInnerSpacing.x), 1f))
            pushItemWidth(pickerSize.x)
            for (pickerType in 0..1) {
                // Draw small/thumbnail version of each picker type (over an invisible button for selection)
                if (pickerType > 0) separator()
                pushId(pickerType)
                var pickerFlags: ColorEditFlags = Cef.NoInputs or Cef.NoOptions or Cef.NoLabel or
                        Cef.NoSidePreview or (flags and Cef.NoAlpha)
                if (pickerType == 0) pickerFlags = pickerFlags or Cef.PickerHueBar
                if (pickerType == 1) pickerFlags = pickerFlags or Cef.PickerHueWheel
                val backupPos = Vec2(ImGui.cursorScreenPos)
                if (selectable("##selectable", false, 0, pickerSize)) // By default, Selectable() is closing popup
                    g.colorEditOptions = (g.colorEditOptions wo Cef._PickerMask) or (pickerFlags and Cef._PickerMask)
                ImGui.cursorScreenPos = backupPos
                val dummyRefCol = Vec4()
                for (i in 0..2) dummyRefCol[i] = refCol[i]
                if (pickerFlags hasnt Cef.NoAlpha) dummyRefCol[3] = refCol[3]
                ImGui.colorPicker4("##dummypicker", dummyRefCol, pickerFlags)
                popId()
            }
            popItemWidth()
        }
        if (allowOptAlphaBar) {
            if (allowOptPicker) separator()
            val pI = intArrayOf(g.colorEditOptions)
            ImGui.checkboxFlags("Alpha Bar", pI, Cef.AlphaBar.i)
            g.colorEditOptions = pI[0]
        }
        endPopup()
    }

    // Plot

    fun plotEx(plotType: PlotType, label: String, data: imgui_widgets_main.PlotArray, valuesOffset: Int, overlayText: String,
               scaleMin_: Float, scaleMax_: Float, frameSize: Vec2) {

        val window = currentWindow
        if (window.skipItems) return

        var scaleMin = scaleMin_
        var scaleMax = scaleMax_
        val valuesCount = data.count()

        val labelSize = calcTextSize(label, 0, true)
        if (frameSize.x == 0f) frameSize.x = nextItemWidth
        if (frameSize.y == 0f) frameSize.y = labelSize.y + style.framePadding.y * 2

        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + frameSize)
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
                if (v.isNaN) continue
                vMin = vMin min v
                vMax = vMax max v
            }
            if (scaleMin == Float.MAX_VALUE) scaleMin = vMin
            if (scaleMax == Float.MAX_VALUE) scaleMax = vMax
        }

        renderFrame(frameBb.min, frameBb.max, Col.FrameBg.u32, true, style.frameRounding)

        val valuesCountMin = if (plotType == PlotType.Lines) 2 else 1
        if (valuesCount >= valuesCountMin) {
            val resW = min(frameSize.x.i, valuesCount) + if (plotType == PlotType.Lines) -1 else 0
            val itemCount = valuesCount + if (plotType == PlotType.Lines) -1 else 0

            // Tooltip on hover
            var vHovered = -1
            if (hovered && io.mousePos in innerBb) {
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


    // NewFrame


    /** The reason this is exposed in imgui_internal.h is: on touch-based system that don't have hovering,
     *  we want to dispatch inputs to the right target (imgui vs imgui+app) */
    fun updateHoveredWindowAndCaptureFlags() {

        /*  Find the window hovered by mouse:
            - Child windows can extend beyond the limit of their parent so we need to derive HoveredRootWindow from HoveredWindow.
            - When moving a window we can skip the search, which also conveniently bypasses the fact that window.outerRectClipped
                is lagging as this point of the frame.
            - We also support the moved window toggling the NoInputs flag after moving has started in order
                to be able to detect windows below it, which is useful for e.g. docking mechanisms. */
        findHoveredWindow()

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

    /** Handle mouse moving window
     *  Note: moving window with the navigation keys (Square + d-pad / CTRL+TAB + Arrows) are processed in NavUpdateWindowing() */
    fun updateMouseMovingWindowNewFrame() {

        val mov = g.movingWindow
        if (mov != null) {
            /*  We actually want to move the root window. g.movingWindow === window we clicked on
                (could be a child window).
                We track it to preserve Focus and so that generally activeIdWindow === movingWindow and
                activeId == movingWindow.moveId for consistency.    */
            keepAliveId(g.activeId)
            assert(mov.rootWindow != null)
            val movingWindow = mov.rootWindow!!
            if (io.mouseDown[0] && isMousePosValid(io.mousePos)) {
                val pos = io.mousePos - g.activeIdClickOffset
                if (movingWindow.pos.x.f != pos.x || movingWindow.pos.y.f != pos.y) {
                    movingWindow.markIniSettingsDirty()
                    movingWindow.setPos(pos, Cond.Always)
                }
                mov.focus()
            } else {
                clearActiveId()
                g.movingWindow = null
            }
        } else
        /*  When clicking/dragging from a window that has the _NoMove flag, we still set the ActiveId in order
            to prevent hovering others.                 */
            if (g.activeIdWindow?.moveId == g.activeId) {
                keepAliveId(g.activeId)
                if (!io.mouseDown[0])
                    clearActiveId()
            }
    }

    /** Initiate moving window, handle left-click and right-click focus */
    fun updateMouseMovingWindowEndFrame() {
        // Initiate moving window
        if (g.activeId != 0 || g.hoveredId != 0) return

        // Unless we just made a window/popup appear
        if (g.navWindow?.appearing == true) return

        // Click to focus window and start moving (after we're done with all our widgets)
        if (io.mouseClicked[0]) {
            val hovered = g.hoveredRootWindow
            if (hovered != null) {
                hovered.startMouseMoving()
                if (io.configWindowsMoveFromTitleBarOnly && hovered.flags hasnt Wf.NoTitleBar)
                    if (io.mouseClickedPos[0] !in hovered.titleBarRect())
                        g.movingWindow = null
            } else if (g.navWindow != null && frontMostPopupModal == null)
                null.focus()  // Clicking on void disable focus
        }

        // With right mouse button we close popups without changing focus
        // (The left mouse button path calls FocusWindow which will lead NewFrame->ClosePopupsOverWindow to trigger)
        if (io.mouseClicked[1]) {
            // Find the top-most window between HoveredWindow and the front most Modal Window.
            // This is where we can trim the popup stack.
            val modal = frontMostPopupModal
            var hoveredWindowAboveModal = false
            if (modal == null)
                hoveredWindowAboveModal = true
            var i = g.windows.lastIndex
            while (i >= 0 && !hoveredWindowAboveModal) {
                val window = g.windows[i]
                if (window === modal)
                    break
                if (window === g.hoveredWindow)
                    hoveredWindowAboveModal = true
                i--
            }
            closePopupsOverWindow(if (hoveredWindowAboveModal) g.hoveredWindow else modal)
        }
    }

    val formatArgPattern: Pattern
        get() = Pattern.compile("%(\\d+\\\$)?([-#+ 0,(<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])")

    fun parseFormatFindStart(fmt: String): Int {
        val matcher = formatArgPattern.matcher(fmt)
        var i = 0
        while (matcher.find(i)) {
            if (fmt[matcher.end() - 1] != '%')
                return matcher.start()
            i = matcher.end()
        }
        return 0
    }

    fun parseFormatFindEnd(fmt: String, i_: Int = 0): Int {
        val matcher = formatArgPattern.matcher(fmt)
        var i = 0
        while (matcher.find(i)) {
            if (fmt[matcher.end() - 1] != '%')
                return matcher.end()
            i = matcher.end()
        }
        return 0
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
        return String(buf, fmtStart, min(fmtEnd - fmtStart + 1, buf.size))
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
    fun shadeVertsLinearColorGradientKeepAlpha(drawList: DrawList, vertStart: Int, vertEnd: Int, gradientP0: Vec2,
                                               gradientP1: Vec2, col0: Int, col1: Int) {
        val gradientExtent = gradientP1 - gradientP0
        val gradientInvLength2 = 1f / gradientExtent.lengthSqr
        for (i in vertStart until vertEnd) {
            val vert = drawList.vtxBuffer[i]
            val d = vert.pos - gradientP0 dot gradientExtent
            val t = glm.clamp(d * gradientInvLength2, 0f, 1f)
            val r = lerp((col0 ushr COL32_R_SHIFT) and 0xFF, (col1 ushr COL32_R_SHIFT) and 0xFF, t)
            val g = lerp((col0 ushr COL32_G_SHIFT) and 0xFF, (col1 ushr COL32_G_SHIFT) and 0xFF, t)
            val b = lerp((col0 ushr COL32_B_SHIFT) and 0xFF, (col1 ushr COL32_B_SHIFT) and 0xFF, t)
            vert.col = (r shl COL32_R_SHIFT) or (g shl COL32_G_SHIFT) or (b shl COL32_B_SHIFT) or (vert.col and COL32_A_MASK)
        }
    }

    /** Distribute UV over (a, b) rectangle */
    fun shadeVertsLinearUV(drawList: DrawList, vertStart: Int, vertEnd: Int, a: Vec2, b: Vec2, uvA: Vec2, uvB: Vec2, clamp: Boolean) {
        val size = b - a
        val uvSize = uvB - uvA
        val scale = Vec2(
                if (size.x != 0f) uvSize.x / size.x else 0f,
                if (size.y != 0f) uvSize.y / size.y else 0f)
        if (clamp) {
            val min = uvA min uvB
            val max = uvA max uvB
            for (i in vertStart until vertEnd) {
                val vertex = drawList.vtxBuffer[i]
                vertex.uv = glm.clamp(uvA + (vertex.pos - a) * scale, min, max)
            }
        } else
            for (i in vertStart until vertEnd) {
                val vertex = drawList.vtxBuffer[i]
                vertex.uv = uvA + (vertex.pos - a) * scale
            }
    }

    companion object {

        var _i = 0
        var _L = 0L
        var _f = 0f
        var _d = 0.0
        var _c = NUL

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
                else -> minSteps.getOrElse(decimalPrecision) {
                    10f.pow(-decimalPrecision.f)
                }
            }
        }

        fun acos01(x: Float) = when {
            x <= 0f -> glm.PIf * 0.5f
            x >= 1f -> 0f
            else -> glm.acos(x)
            //return (-0.69813170079773212f * x * x - 0.87266462599716477f) * x + 1.5707963267948966f; // Cheap approximation, may be enough for what we do.
        }
    }
}

// TODO move in a more appropriate place
fun <R> withBoolean(bools: BooleanArray, ptr: Int = 0, block: (KMutableProperty0<Boolean>) -> R): R {
    Ref.bPtr++
    val bool = Ref::bool
    bool.set(bools[ptr])
    val res = block(bool)
    bools[ptr] = bool()
    Ref.bPtr--
    return res
}

fun <R> withFloat(floats: FloatArray, ptr: Int, block: (KMutableProperty0<Float>) -> R): R { // TODO inline
    Ref.fPtr++
    val f = Ref::float
    f.set(floats[ptr])
    val res = block(f)
    floats[ptr] = f()
    Ref.fPtr--
    return res
}

fun <R> withInt(ints: IntArray, ptr: Int, block: (KMutableProperty0<Int>) -> R): R {
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

private inline fun <R> withChar(char: Char, block: (KMutableProperty0<Char>) -> R): R {
    Ref.cPtr++
    Ref.char = char
    return block(Ref::char).also { Ref.cPtr-- }
}