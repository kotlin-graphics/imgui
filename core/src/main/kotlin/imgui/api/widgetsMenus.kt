package imgui.api

import gli_.has
import glm_.glm
import glm_.max
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import imgui.*
import imgui.ImGui.alignTextToFramePadding
import imgui.ImGui.begin
import imgui.ImGui.beginGroup
import imgui.ImGui.beginPopupEx
import imgui.ImGui.calcTextSize
import imgui.ImGui.closePopupToLevel
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.currentWindow
import imgui.ImGui.end
import imgui.ImGui.endGroup
import imgui.ImGui.endPopup
import imgui.ImGui.focusTopMostWindowUnderOne
import imgui.ImGui.focusWindow
import imgui.ImGui.io
import imgui.ImGui.isPopupOpen
import imgui.ImGui.itemHoverable
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
import imgui.ImGui.setNavIDWithRectRel
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.style
import imgui.internal.*
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.sections.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.reflect.KMutableProperty0
import imgui.SelectableFlag as Sf
import imgui.WindowFlag as Wf
import imgui.internal.sections.LayoutType as Lt

/** Menu
 *  - Use BeginMenuBar() on a window ImGuiWindowFlags_MenuBar to append to its menu bar.
 *  - Use BeginMainMenuBar() to create a menu bar at the top of the screen and append to it.
 *  - Use BeginMenu() to create a menu. You can call BeginMenu() multiple time with the same identifier to append more items to it. */
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
            // We overwrite CursorMaxPos because BeginGroup sets it to CursorPos (essentially the .EmitItem hack in EndMenuBar() would need something analoguous here, maybe a BeginGroupEx() with flags).
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
                return if (parent != null && parent.flags has Wf._ChildMenu) getParent() else this
            }

            navEarliestChild = navEarliestChild.getParent()
            if (navEarliestChild.parentWindow == window && navEarliestChild.dc.parentLayoutType == Lt.Horizontal && g.navMoveRequestForward == NavForward.None) {
                /*  To do so we claim focus back, restore NavId and then process the movement request for yet another
                    frame. This involve a one-frame delay which isn't very problematic in this situation.
                    We could remove it by scoring in advance for multiple window (probably not worth the hassle/cost)   */
                val layer = NavLayer.Menu
                assert(window.dc.navLayerActiveMaskNext has (1 shl layer)) { "Sanity check" }
                focusWindow(window)
                setNavIDWithRectRel(window.navLastIds[layer], layer, 0, window.navRectRel[layer])
                g.navLayer = layer
                g.navDisableHighlight = true // Hide highlight for the current frame so we don't see the intermediary selection.
                g.navMoveRequestForward = NavForward.ForwardQueued
                navMoveRequestCancel()
            }
        }

        assert(window.flags has Wf.MenuBar && window.dc.menuBarAppending)
        popClipRect()
        popID()
        with(window.dc) {
            // Save horizontal position so next append can reuse it. This is kinda equivalent to a per-layer CursorPos.
            menuBarOffset.x = cursorPos.x - window.menuBarRect().min.x
            groupStack.last().emitItem = false
            endGroup() // Restore position on layer 0
            layoutType = Lt.Vertical
            navLayerCurrent = NavLayer.Main
            menuBarAppending = false
        }
    }

    /** Create and append to a full screen menu-bar.
     *  For the main menu bar, which cannot be moved, we honor g.Style.DisplaySafeAreaPadding to ensure text can be visible on a TV set. */
    fun beginMainMenuBar(): Boolean {

        g.nextWindowData.menuBarOffsetMinVal.put(style.displaySafeAreaPadding.x, max(style.displaySafeAreaPadding.y - style.framePadding.y, 0f))
        setNextWindowPos(Vec2())
        setNextWindowSize(Vec2(io.displaySize.x, g.nextWindowData.menuBarOffsetMinVal.y + g.fontBaseSize + style.framePadding.y))
        pushStyleVar(StyleVar.WindowRounding, 0f)
        pushStyleVar(StyleVar.WindowMinSize, Vec2i())
        val windowFlags = Wf.NoTitleBar or Wf.NoResize or Wf.NoMove or Wf.NoScrollbar or Wf.NoSavedSettings or Wf.MenuBar
        val isOpen = begin("##MainMenuBar", null, windowFlags) && beginMenuBar()
        popStyleVar(2)
        g.nextWindowData.menuBarOffsetMinVal put 0f
        return when {
            !isOpen -> {
                end()
                false
            }
            else -> true
        }
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
        // If somehow this is ever becoming a problem we can switch to use e.g. a ImGuiStorager mapping key to last frame used.
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

        /*  The reference position stored in popup_pos will be used by Begin() to find a suitable position for the child menu,
            However the final position is going to be different! It is chosen by FindBestWindowPosForPopup().
            e.g. Menus tend to overlap each other horizontally to amplify relative Z-ordering.         */
        val popupPos = Vec2()
        val pos = Vec2(window.dc.cursorPos)
        if (window.dc.layoutType == Lt.Horizontal) {
            /*  Menu inside an horizontal menu bar
                Selectable extend their highlight by half ItemSpacing in each direction.
                For ChildMenu, the popup position will be overwritten by the call to FindBestWindowPosForPopup() in begin() */
            popupPos.put(pos.x - 1f - floor(style.itemSpacing.x * 0.5f), pos.y - style.framePadding.y + window.menuBarHeight)
            window.dc.cursorPos.x += floor(style.itemSpacing.x * 0.5f)
            pushStyleVar(StyleVar.ItemSpacing, Vec2(style.itemSpacing.x * 2f, style.itemSpacing.y))
            val w = labelSize.x
            val f = Sf._NoHoldingActiveId or Sf._SelectOnClick or Sf.DontClosePopups or if (enabled) 0 else Sf.Disabled.i
            pressed = selectable(label, menuIsOpen, f, Vec2(w, 0f))
            popStyleVar()
            /*  -1 spacing to compensate the spacing added when selectable() did a sameLine(). It would also work
                to call sameLine() ourselves after the popStyleVar().   */
            window.dc.cursorPos.x += floor(style.itemSpacing.x * (-1f + 0.5f))
        } else {
            // Menu inside a menu
            // (In a typical menu window where all items are BeginMenu() or MenuItem() calls, extra_w will always be 0.0f.
            //  Only when they are other items sticking out we're going to add spacing, yet only register minimum width into the layout system.
            popupPos.put(pos.x, pos.y - style.windowPadding.y)
            val minW = window.dc.menuColumns.declColumns(labelSize.x, 0f, floor(g.fontSize * 1.2f)) // Feedback to next frame
            val extraW = glm.max(0f, contentRegionAvail.x - minW)
            val f = Sf._NoHoldingActiveId or Sf._SelectOnClick or Sf.DontClosePopups or Sf._SpanAvailWidth
            pressed = selectable(label, menuIsOpen, f or if (enabled) Sf.None else Sf.Disabled, Vec2(minW, 0f))
            val textCol = if (enabled) Col.Text else Col.TextDisabled
            window.drawList.renderArrow(pos + Vec2(window.dc.menuColumns.pos[2] + extraW + g.fontSize * 0.3f, 0f), textCol.u32, Dir.Right)
        }
        val hovered = enabled && itemHoverable(window.dc.lastItemRect, id)

        if (menusetIsOpen)
            g.navWindow = backedNavWindow

        var wantOpen = false
        var wantClose = false
        if (window.dc.layoutType == Lt.Vertical) {    // (window->Flags & (ImGuiWindowFlags_Popup|ImGuiWindowFlags_ChildMenu))
            /*  Close menu when not hovering it anymore unless we are moving roughly in the direction of the menu
                Implement http://bjk5.com/post/44698559168/breaking-down-amazons-mega-dropdown to avoid using timers,
                so menus feels more reactive.             */
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

        Hook.itemInfo?.invoke(g, id, label, window.dc.itemFlags or ItemStatusFlag.Openable or if (menuIsOpen) ItemStatusFlag.Opened else ItemStatusFlag.None)

        if (!menuIsOpen && wantOpen && g.openPopupStack.size > g.beginPopupStack.size) {
            // Don't recycle same menu level in the same frame, first close the other menu and yield for a frame.
            openPopup(label)
            return false
        }

        menuIsOpen = menuIsOpen || wantOpen
        if (wantOpen) openPopup(label)

        if (menuIsOpen) {
            setNextWindowPos(popupPos, Cond.Always)
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

    /** return true when activated. shortcuts are displayed for convenience but not processed by ImGui at the moment    */
    fun menuItem(label: String, shortcut: String = "", selected: Boolean = false, enabled: Boolean = true): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val pos = Vec2(window.dc.cursorPos)
        val labelSize = calcTextSize(label, hideTextAfterDoubleHash = true)

        // We've been using the equivalent of ImGuiSelectableFlags_SetNavIdOnHover on all Selectable() since early Nav system days (commit 43ee5d73),
        // but I am unsure whether this should be kept at all. For now moved it to be an opt-in feature used by menus only.
        val flags = Sf._SelectOnRelease or Sf._SetNavIdOnHover or if (enabled) Sf.None else Sf.Disabled
        val pressed: Boolean
        if (window.dc.layoutType == Lt.Horizontal) {
            /*  Mimic the exact layout spacing of beginMenu() to allow menuItem() inside a menu bar, which is a little 
                misleading but may be useful 
                Note that in this situation we render neither the shortcut neither the selected tick mark   */
            val w = labelSize.x
            window.dc.cursorPos.x += floor(style.itemSpacing.x * 0.5f)
            pushStyleVar(StyleVar.ItemSpacing, Vec2(style.itemSpacing.x * 2f, style.itemSpacing.y))
            pressed = selectable(label, false, flags, Vec2(w, 0f))
            popStyleVar()
            /*  -1 spacing to compensate the spacing added when selectable() did a sameLine(). It would also work
                to call sameLine() ourselves after the popStyleVar().             */
            window.dc.cursorPos.x += floor(style.itemSpacing.x * (-1f + 0.5f))
        } else {
            // Menu item inside a vertical menu
            // (In a typical menu window where all items are BeginMenu() or MenuItem() calls, extra_w will always be 0.0f.
            //  Only when they are other items sticking out we're going to add spacing, yet only register minimum width into the layout system.
            val shortcutW = if(shortcut.isNotEmpty()) calcTextSize(shortcut).x else 0f
            val minW = window.dc.menuColumns.declColumns(labelSize.x, shortcutW, floor(g.fontSize * 1.2f)) // Feedback for next frame
            val extraW = max(0f, contentRegionAvail.x - minW)
            pressed = selectable(label, false, flags or Sf._SpanAvailWidth, Vec2(minW, 0f))
            if (shortcutW > 0f) {
                pushStyleColor(Col.Text, style.colors[Col.TextDisabled])
                renderText(pos + Vec2(window.dc.menuColumns.pos[1] + extraW, 0f), shortcut, false)
                popStyleColor()
            }
            if (selected)
                window.drawList.renderCheckMark(pos + Vec2(window.dc.menuColumns.pos[2] + extraW + g.fontSize * 0.4f, g.fontSize * 0.134f * 0.5f),
                        (if (enabled) Col.Text else Col.TextDisabled).u32, g.fontSize * 0.866f)
        }

        Hook.itemInfo?.invoke(g, window.dc.lastItemId, label, window.dc.itemFlags or ItemStatusFlag.Checkable or if (selected) ItemStatusFlag.Checked else ItemStatusFlag.None)
        return pressed
    }

    /** return true when activated + toggle (*p_selected) if p_selected != NULL */
    fun menuItem(label: String, shortcut: String = "", pSelected: BooleanArray?, enabled: Boolean = true): Boolean =
            if (menuItem(label, shortcut, pSelected?.get(0) == true, enabled)) {
                pSelected?.let { it[0] = !it[0] }
                true
            } else false

    fun menuItem(label: String, shortcut: String = "", selected: KMutableProperty0<Boolean>?, enabled: Boolean = true): Boolean =
            menuItem(label, shortcut, selected?.get() == true, enabled)
                    .also { if (it) selected?.apply { set(!get()) } }
}