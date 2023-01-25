package imgui.api

import gli_.has
import glm_.glm
import glm_.max
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.alignTextToFramePadding
import imgui.ImGui.beginGroup
import imgui.ImGui.beginPopupEx
import imgui.ImGui.beginViewportSideBar
import imgui.ImGui.calcTextSize
import imgui.ImGui.closePopupToLevel
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.currentWindow
import imgui.ImGui.end
import imgui.ImGui.endGroup
import imgui.ImGui.endPopup
import imgui.ImGui.focusTopMostWindowUnderOne
import imgui.ImGui.focusWindow
import imgui.ImGui.frameHeight
import imgui.ImGui.io
import imgui.ImGui.isPopupOpen
import imgui.ImGui.itemHoverable
import imgui.ImGui.mainViewport
import imgui.ImGui.menuItemEx
import imgui.ImGui.navMoveRequestButNoResultYet
import imgui.ImGui.navMoveRequestCancel
import imgui.ImGui.openPopup
import imgui.ImGui.popClipRect
import imgui.ImGui.popID
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushClipRect
import imgui.ImGui.pushID
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.renderText
import imgui.ImGui.selectable
import imgui.ImGui.setNavID
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.style
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.floor
import imgui.internal.round
import imgui.internal.sections.*
import imgui.internal.triangleContainsPoint
import kotlin.math.abs
import kotlin.math.max
import kotlin.reflect.KMutableProperty0
import imgui.SelectableFlag as Sf
import imgui.WindowFlag as Wf
import imgui.internal.sections.LayoutType as Lt

// Widgets: Menus
// - Use BeginMenuBar() on a window ImGuiWindowFlags_MenuBar to append to its menu bar.
// - Use BeginMainMenuBar() to create a menu bar at the top of the screen and append to it.
// - Use BeginMenu() to create a menu. You can call BeginMenu() multiple time with the same identifier to append more items to it.
// - Not that MenuItem() keyboardshortcuts are displayed as a convenience but _not processed_ by Dear ImGui at the moment.
interface widgetsMenus {

    /** Append to menu-bar of current window (requires WindowFlag.MenuBar flag set on parent window).
     *  Only call endMenuBar() if this returns true!
     *
     *  FIXME: Provided a rectangle perhaps e.g. a BeginMenuBarEx() could be used anywhere..
     *  Currently the main responsibility of this function being to setup clip-rect + horizontal layout + menu navigation layer.
     *  Ideally we also want this to be responsible for claiming space out of the main window scrolling rectangle, in which case ImGuiWindowFlags_MenuBar will become unnecessary.
     *  Then later the same system could be used for multiple menu-bars, scrollbars, side-bars. */
    fun beginMenuBar(): Boolean {

        val window = currentWindow
        if (window.skipItems) return false
        if (window.flags hasnt Wf.MenuBar) return false

        assert(!window.dc.menuBarAppending)
        beginGroup() // Backup position on layer 0 // FIXME: Misleading to use a group for that backup/restore
        pushID("##menubar")

        // We don't clip with current window clipping rectangle as it is already set to the area below. However we clip with window full rect.
        // We remove 1 worth of rounding to Max.x to that text in long menus and small windows don't tend to display over the lower-right rounded area, which looks particularly glitchy.
        val barRect = window.menuBarRect()
        val clipRect = Rect(round(barRect.min.x + window.windowBorderSize), round(barRect.min.y + window.windowBorderSize),
                            round(barRect.min.x max (barRect.max.x - (window.windowRounding max window.windowBorderSize))), round(barRect.max.y))
        clipRect clipWith window.outerRectClipped
        pushClipRect(clipRect.min, clipRect.max, false)

        with(window.dc) {
            // We overwrite CursorMaxPos because BeginGroup sets it to CursorPos (essentially the .EmitItem hack in EndMenuBar() would need something analogous here, maybe a BeginGroupEx() with flags).
            cursorMaxPos.put(barRect.min.x + window.dc.menuBarOffset.x, barRect.min.y + window.dc.menuBarOffset.y)
            cursorPos put cursorMaxPos
            layoutType = Lt.Horizontal
            navLayerCurrent = NavLayer.Menu
            menuBarAppending = true
        }
        alignTextToFramePadding()
        return true
    }

    /** Only call EndMenuBar() if BeginMenuBar() returns true!  */
    fun endMenuBar() {

        val window = currentWindow
        if (window.skipItems) return

        // Nav: When a move request within one of our child menu failed, capture the request to navigate among our siblings.
        if (navMoveRequestButNoResultYet() && (g.navMoveDir == Dir.Left || g.navMoveDir == Dir.Right) && g.navWindow!!.flags has Wf._ChildMenu) {
            var navEarliestChild = g.navWindow!!

            tailrec fun Window.getParent(): Window {
                val parent = parentWindow
                return when {
                    parent != null && parent.flags has Wf._ChildMenu -> parent.getParent()
                    else -> this
                }
            }

            navEarliestChild = navEarliestChild.getParent()
            if (navEarliestChild.parentWindow == window && navEarliestChild.dc.parentLayoutType == Lt.Horizontal && g.navMoveRequestForward == NavForward.None) {
                // To do so we claim focus back, restore NavId and then process the movement request for yet another frame.
                // This involve a one-frame delay which isn't very problematic in this situation. We could remove it by scoring in advance for multiple window (probably not worth the hassle/cost)
                val layer = NavLayer.Menu
                assert(window.dc.navLayersActiveMaskNext has (1 shl layer)) { "Sanity check" }
                focusWindow(window)
                setNavID(window.navLastIds[layer], layer, 0, window.navRectRel[layer])
                g.navDisableHighlight = true // Hide highlight for the current frame so we don't see the intermediary selection.
                g.navDisableMouseHover = true; g.navMousePosDirty = true
                g.navMoveRequestForward = NavForward.ForwardQueued
                navMoveRequestCancel()
            }
        }

        assert(window.flags has Wf.MenuBar && window.dc.menuBarAppending)
        popClipRect()
        popID()
        with(window.dc) {
            menuBarOffset.x = cursorPos.x - window.pos.x
            g.groupStack.last().emitItem = false
            endGroup() // Restore position on layer 0
            layoutType = Lt.Vertical
            navLayerCurrent = NavLayer.Main
            menuBarAppending = false
        }
    }

    /** Create and append to a full screen menu-bar. */
    fun beginMainMenuBar(): Boolean {

        val viewport = mainViewport as ViewportP

        // For the main menu bar, which cannot be moved, we honor g.Style.DisplaySafeAreaPadding to ensure text can be visible on a TV set.
        // FIXME: This could be generalized as an opt-in way to clamp window->DC.CursorStartPos to avoid SafeArea?
        // FIXME: Consider removing support for safe area down the line... it's messy. Nowadays consoles have support for TV calibration in OS settings.
        g.nextWindowData.menuBarOffsetMinVal.put(style.displaySafeAreaPadding.x, max(style.displaySafeAreaPadding.y - style.framePadding.y, 0f))
        val windowFlags = Wf.NoScrollbar or Wf.NoSavedSettings or Wf.MenuBar
        val height = frameHeight
        val isOpen = beginViewportSideBar("##MainMenuBar", viewport, Dir.Up, height, windowFlags)
        g.nextWindowData.menuBarOffsetMinVal put 0f

        if (isOpen)
            beginMenuBar()
        else
            end()

        return isOpen
    }

    /** Only call EndMainMenuBar() if BeginMainMenuBar() returns true! */
    fun endMainMenuBar() {
        endMenuBar()

        // When the user has left the menu layer (typically: closed menus through activation of an item), we restore focus to the previous window
        // FIXME: With this strategy we won't be able to restore a NULL focus.
        if (g.currentWindow == g.navWindow && g.navLayer == NavLayer.Main && !g.navAnyRequest)
            focusTopMostWindowUnderOne(g.navWindow)

        end()
    }

    /** create a sub-menu entry. only call EndMenu() if this returns true!  */
    fun beginMenu(label: String, enabled: Boolean = true): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getID(label)
        var menuIsOpen = isPopupOpen(id)

        // Sub-menus are ChildWindow so that mouse can be hovering across them (otherwise top-most popup menu would steal focus and not allow hovering on parent menu)
        var flags = Wf._ChildMenu or Wf.AlwaysAutoResize or Wf.NoMove or Wf.NoTitleBar or Wf.NoSavedSettings or Wf.NoNavFocus
        if (window.flags has (Wf._Popup or Wf._ChildMenu))
            flags = flags or Wf._ChildWindow

        // If a menu with same the ID was already submitted, we will append to it, matching the behavior of Begin().
        // We are relying on a O(N) search - so O(N log N) over the frame - which seems like the most efficient for the expected small amount of BeginMenu() calls per frame.
        // If somehow this is ever becoming a problem we can switch to use e.g. ImGuiStorage mapping key to last frame used.
        if (id in g.menusIdSubmittedThisFrame) {
            if (menuIsOpen)
                menuIsOpen = beginPopupEx(id, flags) // menu_is_open can be 'false' when the popup is completely clipped (e.g. zero size display)
            else
                g.nextWindowData.clearFlags()          // we behave like Begin() and need to consume those values
            return menuIsOpen
        }

        // Tag menu as used. Next time BeginMenu() with same ID is called it will append to existing menu
        g.menusIdSubmittedThisFrame += id

        val labelSize = calcTextSize(label, hideTextAfterDoubleHash = true)

        val pressed: Boolean
        val menusetIsOpen = window.flags hasnt Wf._Popup && g.openPopupStack.size > g.beginPopupStack.size &&
                g.openPopupStack[g.beginPopupStack.size].openParentId == window.idStack.last()
        val backedNavWindow = g.navWindow
        if (menusetIsOpen)
        // Odd hack to allow hovering across menus of a same menu-set (otherwise we wouldn't be able to hover parent)
            g.navWindow = window

        // The reference position stored in popup_pos will be used by Begin() to find a suitable position for the child menu,
        // However the final position is going to be different! It is chosen by FindBestWindowPosForPopup().
        // e.g. Menus tend to overlap each other horizontally to amplify relative Z-ordering.
        val popupPos = Vec2();
        val pos = Vec2(window.dc.cursorPos)
        pushID(label)
        if (!enabled)
            pushStyleColor(Col.Text, style.colors[Col.TextDisabled])
        val offsets = window.dc.menuColumns
        if (window.dc.layoutType == Lt.Horizontal) {
            /*  Menu inside an horizontal menu bar
                Selectable extend their highlight by half ItemSpacing in each direction.
                For ChildMenu, the popup position will be overwritten by the call to FindBestWindowPosForPopup() in begin() */
            popupPos.put(pos.x - 1f - floor(style.itemSpacing.x * 0.5f), pos.y - style.framePadding.y + window.menuBarHeight)
            window.dc.cursorPos.x += floor(style.itemSpacing.x * 0.5f)
            pushStyleVar(StyleVar.ItemSpacing, Vec2(style.itemSpacing.x * 2f, style.itemSpacing.y))
            val w = labelSize.x
            val textPos = Vec2(window.dc.cursorPos.x + offsets.offsetLabel, window.dc.cursorPos.y + window.dc.currLineTextBaseOffset)
            val f = Sf._NoHoldingActiveID or Sf._SelectOnClick or Sf.DontClosePopups or if (enabled) 0 else Sf.Disabled.i
            pressed = selectable("", menuIsOpen, f, Vec2(w, 0f))
            renderText(textPos, label)
            popStyleVar()
            /*  -1 spacing to compensate the spacing added when selectable() did a sameLine(). It would also work
                to call sameLine() ourselves after the popStyleVar().   */
            window.dc.cursorPos.x += floor(style.itemSpacing.x * (-1f + 0.5f))
        } else {
            // Menu inside a menu
            // (In a typical menu window where all items are BeginMenu() or MenuItem() calls, extra_w will always be 0.0f.
            //  Only when they are other items sticking out we're going to add spacing, yet only register minimum width into the layout system.
            popupPos.put(pos.x, pos.y - style.windowPadding.y)
            val iconW = 0f // FIXME: This not currently exposed for BeginMenu() however you can call window->DC.MenuColumns.DeclColumns(w, 0, 0, 0) yourself
            val checkmarkW = floor(g.fontSize * 1.2f)
            val minW = window.dc.menuColumns.declColumns(iconW, labelSize.x, 0f, checkmarkW) // Feedback to next frame
            val extraW = 0f max (contentRegionAvail.x - minW)
            val textPos = Vec2(window.dc.cursorPos.x + offsets.offsetLabel, window.dc.cursorPos.y + window.dc.currLineTextBaseOffset)
            pressed = selectable("", menuIsOpen, Sf._NoHoldingActiveID or Sf._SelectOnClick or Sf.DontClosePopups or Sf._SpanAvailWidth or
                    if (!enabled) Sf.Disabled else Sf.None, Vec2(minW, 0f))
            renderText(textPos, label)
            window.drawList.renderArrow(pos + Vec2(offsets.offsetMark + extraW + g.fontSize * 0.3f, 0f), Col.Text.u32, Dir.Right)
        }
        if (!enabled)
            popStyleColor()
        popID()

        val hovered = enabled && itemHoverable(window.dc.lastItemRect, id)

        if (menusetIsOpen)
            g.navWindow = backedNavWindow

        var wantOpen = false
        var wantClose = false
        if (window.dc.layoutType == Lt.Vertical) {    // (window->Flags & (ImGuiWindowFlags_Popup|ImGuiWindowFlags_ChildMenu))
            // Close menu when not hovering it anymore unless we are moving roughly in the direction of the menu
            // Implement http://bjk5.com/post/44698559168/breaking-down-amazons-mega-dropdown to avoid using timers, so menus feels more reactive.
            var movingTowardOtherChildMenu = false

            val childMenuWindow = when {
                g.beginPopupStack.size < g.openPopupStack.size && g.openPopupStack[g.beginPopupStack.size].sourceWindow == window -> g.openPopupStack[g.beginPopupStack.size].window
                else -> null
            }
            if (g.hoveredWindow === window && childMenuWindow != null && window.flags hasnt Wf.MenuBar) {
                // FIXME-DPI: Values should be derived from a master "scale" factor.
                val nextWindowRect = childMenuWindow.rect()
                val ta = io.mousePos - io.mouseDelta
                val tb = if (window.pos.x < childMenuWindow.pos.x) nextWindowRect.tl else nextWindowRect.tr
                val tc = if (window.pos.x < childMenuWindow.pos.x) nextWindowRect.bl else nextWindowRect.br
                val extra = glm.clamp(abs(ta.x - tb.x) * 0.3f, 5f, 30f)    // add a bit of extra slack.
                ta.x += if (window.pos.x < childMenuWindow.pos.x) -0.5f else +0.5f // to avoid numerical issues
                tb.y = ta.y + max((tb.y - extra) - ta.y, -100f)                // triangle is maximum 200 high to limit the slope and the bias toward large sub-menus // FIXME: Multiply by fb_scale?
                tc.y = ta.y + min((tc.y + extra) - ta.y, +100f)
                movingTowardOtherChildMenu = triangleContainsPoint(ta, tb, tc, io.mousePos)
                //GetForegroundDrawList()->AddTriangleFilled(ta, tb, tc, moving_within_opened_triangle ? IM_COL32(0,128,0,128) : IM_COL32(128,0,0,128)); // [DEBUG]
            }

            // FIXME: Hovering a disabled BeginMenu or MenuItem won't close us
            if (menuIsOpen && !hovered && g.hoveredWindow === window && g.hoveredIdPreviousFrame != 0 && g.hoveredIdPreviousFrame != id && !movingTowardOtherChildMenu)
                wantClose = true

            if (!menuIsOpen && hovered && pressed) // Click to open
                wantOpen = true
            else if (!menuIsOpen && hovered && !movingTowardOtherChildMenu) // Hover to open
                wantOpen = true

            if (g.navActivateId == id) {
                wantClose = menuIsOpen
                wantOpen = !menuIsOpen
            }
            if (g.navId == id && g.navMoveRequest && g.navMoveDir == Dir.Right) { // Nav-Right to open
                wantOpen = true
                navMoveRequestCancel()
            }
        } else  // Menu bar
            if (menuIsOpen && pressed && menusetIsOpen) { // Click an open menu again to close it
                wantClose = true
                wantOpen = false
                menuIsOpen = false
            } else if (pressed || (hovered && menusetIsOpen && !menuIsOpen)) // First click to open, then hover to open others
                wantOpen = true
            else if (g.navId == id && g.navMoveRequest && g.navMoveDir == Dir.Down) { // Nav-Down to open
                wantOpen = true
                navMoveRequestCancel()
            }
        /*  explicitly close if an open menu becomes disabled, facilitate users code a lot in pattern such as
            'if (BeginMenu("options", has_object)) { ..use object.. }'         */
        if (!enabled)
            wantClose = true
        if (wantClose && isPopupOpen(id))
            closePopupToLevel(g.beginPopupStack.size, true)

        val f = ItemStatusFlag.Openable or if (menuIsOpen) ItemStatusFlag.Opened else ItemStatusFlag.None
        IMGUI_TEST_ENGINE_ITEM_INFO(id, label, g.currentItemFlags or f)

        if (!menuIsOpen && wantOpen && g.openPopupStack.size > g.beginPopupStack.size) {
            // Don't recycle same menu level in the same frame, first close the other menu and yield for a frame.
            openPopup(label)
            return false
        }

        menuIsOpen = menuIsOpen || wantOpen
        if (wantOpen) openPopup(label)

        if (menuIsOpen) {
            setNextWindowPos(popupPos, Cond.Always) // Note: this is super misleading! The value will serve as reference for FindBestWindowPosForPopup(), not actual pos.
            menuIsOpen = beginPopupEx(id, flags) // menu_is_open can be 'false' when the popup is completely clipped (e.g. zero size display)
        } else
            g.nextWindowData.clearFlags() // We behave like Begin() and need to consume those values


        return menuIsOpen
    }

    /** Only call EndMenu() if BeginMenu() returns true! */
    fun endMenu() {
        /*  Nav: When a left move request _within our child menu_ failed, close ourselves (the _parent_ menu).
            A menu doesn't close itself because EndMenuBar() wants the catch the last Left<>Right inputs.
            However, it means that with the current code, a beginMenu() from outside another menu or a menu-bar won't be
            closable with the Left direction.   */
        val window = g.currentWindow!!
        g.navWindow?.let {
            if (it.parentWindow === window && g.navMoveDir == Dir.Left && navMoveRequestButNoResultYet() && window.dc.layoutType == Lt.Vertical) {
                closePopupToLevel(g.beginPopupStack.size, true)
                navMoveRequestCancel()
            }
        }
        endPopup()
    }

    fun menuItem(label: String, shortcut: String, selected: Boolean, enabled: Boolean): Boolean = menuItemEx(label, "", shortcut, selected, enabled)

    /** return true when activated + toggle (*p_selected) if p_selected != NULL */
    fun menuItem(label: String, shortcut: String = "", pSelected: BooleanArray?, enabled: Boolean = true): Boolean =
        if (menuItemEx(label, "", shortcut, pSelected?.get(0) == true, enabled)) {
            pSelected?.let { it[0] = !it[0] }
            true
        } else false

    fun menuItem(label: String, shortcut: String = "", selected: KMutableProperty0<Boolean>?, enabled: Boolean = true): Boolean =
        menuItemEx(label, "", shortcut, selected?.get() == true, enabled)
                .also { if (it) selected?.apply { set(!get()) } }
}