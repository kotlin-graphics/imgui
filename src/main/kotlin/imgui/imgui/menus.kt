package imgui.imgui

import gli_.has
import glm_.f
import glm_.glm
import glm_.i
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
import imgui.ImGui.io
import imgui.ImGui.isPopupOpen
import imgui.ImGui.itemHoverable
import imgui.ImGui.navMoveRequestButNoResultYet
import imgui.ImGui.navMoveRequestCancel
import imgui.ImGui.openPopup
import imgui.ImGui.popClipRect
import imgui.ImGui.popId
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushClipRect
import imgui.ImGui.pushId
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.renderArrow
import imgui.ImGui.renderCheckMark
import imgui.ImGui.renderText
import imgui.ImGui.selectable
import imgui.ImGui.setNavIDWithRectRel
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.style
import imgui.internal.*
import imgui.internal.LayoutType
import kotlin.math.max
import kotlin.reflect.KMutableProperty0
import imgui.SelectableFlag as Sf
import imgui.WindowFlag as Wf
import imgui.internal.LayoutType as Lt

/** Menu    */
interface imgui_menus {


    /** Create and append to a full screen menu-bar.
     *  For the main menu bar, which cannot be moved, we honor g.Style.DisplaySafeAreaPadding to ensure text can be visible on a TV set. */
    fun beginMainMenuBar(): Boolean {

        g.nextWindowData.menuBarOffsetMinVal.put(style.displaySafeAreaPadding.x, max(style.displaySafeAreaPadding.y - style.framePadding.y, 0f))
        setNextWindowPos(Vec2())
        setNextWindowSize(Vec2(io.displaySize.x, g.nextWindowData.menuBarOffsetMinVal.y + g.fontBaseSize + style.framePadding.y))
        pushStyleVar(StyleVar.WindowRounding, 0f)
        pushStyleVar(StyleVar.WindowMinSize, Vec2i())
        val flags = Wf.NoTitleBar or Wf.NoResize or Wf.NoMove or Wf.NoScrollbar or Wf.NoSavedSettings or Wf.MenuBar
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
        if (g.currentWindow == g.navWindow && g.navLayer == NavLayer.Main)
            focusPreviousWindowIgnoringOne(g.navWindow)

        end()
    }

    /** Append to menu-bar of current window (requires WindowFlag.MenuBar flag set on parent window).
     *  Only call endMenuBar() if this returns true!    */
    fun beginMenuBar(): Boolean {

        val window = currentWindow
        if (window.skipItems) return false
        if (window.flags hasnt Wf.MenuBar) return false

        assert(!window.dc.menuBarAppending)
        beginGroup() // Backup position on layer 0
        pushId("##menubar")

        /*  We don't clip with current window clipping rectangle as it is already set to the area below. However we clip
            with window full rect.
            We remove 1 worth of rounding to max.x to that text in long menus and small windows don't tend to display
            over the lower-right rounded area, which looks particularly glitchy. */
        val barRect = window.menuBarRect()
        val clipRect = Rect(floor(barRect.min.x + 0.5f), floor(barRect.min.y + window.windowBorderSize + 0.5f),
                floor(max(barRect.min.x, barRect.max.x - window.windowRounding) + 0.5f), floor(barRect.max.y + 0.5f))
        clipRect clipWith window.outerRectClipped
        pushClipRect(clipRect.min, clipRect.max, false)

        with(window.dc) {
            cursorPos.put(barRect.min.x + window.dc.menuBarOffset.x, barRect.min.y + window.dc.menuBarOffset.y)
            layoutType = LayoutType.Horizontal
            navLayerCurrent = NavLayer.Menu
            navLayerCurrentMask = 1 shl NavLayer.Menu
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
        if (navMoveRequestButNoResultYet() && (g.navMoveDir == Dir.Left || g.navMoveDir == Dir.Right) && g.navWindow!!.flags has Wf.ChildMenu) {
            var navEarliestChild = g.navWindow!!

            tailrec fun Window.getParent(): Window {
                val parent = parentWindow
                return if (parent != null && parent.flags has Wf.ChildMenu) getParent() else this
            }

            navEarliestChild = navEarliestChild.getParent()
            if (navEarliestChild.parentWindow == window && navEarliestChild.dc.parentLayoutType == Lt.Horizontal && g.navMoveRequestForward == NavForward.None) {
                /*  To do so we claim focus back, restore NavId and then process the movement request for yet another
                    frame. This involve a one-frame delay which isn't very problematic in this situation.
                    We could remove it by scoring in advance for multiple window (probably not worth the hassle/cost)   */
                val layer = NavLayer.Menu
                assert(window.dc.navLayerActiveMaskNext has (1 shl layer)) { "Sanity check" }
                window.focus()
                setNavIDWithRectRel(window.navLastIds[layer.i], layer, window.navRectRel[layer.i])
                g.navLayer = layer
                g.navDisableHighlight = true // Hide highlight for the current frame so we don't see the intermediary selection.
                g.navMoveRequestForward = NavForward.ForwardQueued
                navMoveRequestCancel()
            }
        }

        assert(window.flags has Wf.MenuBar && window.dc.menuBarAppending)
        popClipRect()
        popId()
        with(window.dc) {
            // Save horizontal position so next append can reuse it. This is kinda equivalent to a per-layer CursorPos.
            menuBarOffset.x = cursorPos.x - window.menuBarRect().min.x
            groupStack.last().advanceCursor = false
            endGroup() // Restore position on layer 0
            layoutType = LayoutType.Vertical
            navLayerCurrent = NavLayer.Main
            navLayerCurrentMask = 1 shl NavLayer.Main
            menuBarAppending = false
        }
    }

    /** create a sub-menu entry. only call EndMenu() if this returns true!  */
    fun beginMenu(label: String, enabled: Boolean = true): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(label)

        val labelSize = calcTextSize(label, true)

        val pressed: Boolean
        var menuIsOpen = isPopupOpen(id)
        val menusetIsOpen = window.flags hasnt Wf.Popup && g.openPopupStack.size > g.beginPopupStack.size &&
                g.openPopupStack[g.beginPopupStack.size].openParentId == window.idStack.last()
        val backedNavWindow = g.navWindow
        if (menusetIsOpen)
        // Odd hack to allow hovering across menus of a same menu-set (otherwise we wouldn't be able to hover parent)
            g.navWindow = window

        /*  The reference position stored in popup_pos will be used by Begin() to find a suitable position for the child menu,
            However the final position is going to be different! It is choosen by FindBestWindowPosForPopup().
            e.g. Menus tend to overlap each other horizontally to amplify relative Z-ordering.         */
        val popupPos = Vec2()
        val pos = Vec2(window.dc.cursorPos)
        if (window.dc.layoutType == LayoutType.Horizontal) {
            /*  Menu inside an horizontal menu bar
                Selectable extend their highlight by half ItemSpacing in each direction.
                For ChildMenu, the popup position will be overwritten by the call to FindBestWindowPosForPopup() in begin() */
            popupPos.put(pos.x - 1.0f - (style.itemSpacing.x * 0.5f).i.f, pos.y - style.framePadding.y + window.menuBarHeight)
            window.dc.cursorPos.x += (style.itemSpacing.x * 0.5f).i.f
            pushStyleVar(StyleVar.ItemSpacing, Vec2(style.itemSpacing.x * 2f, style.itemSpacing.y))
            val w = labelSize.x
            val flags = Sf.NoHoldingActiveID or Sf.PressedOnClick or Sf.DontClosePopups or if (enabled) 0 else Sf.Disabled.i
            pressed = selectable(label, menuIsOpen, flags, Vec2(w, 0f))
            popStyleVar()
            /*  -1 spacing to compensate the spacing added when selectable() did a sameLine(). It would also work
                to call sameLine() ourselves after the popStyleVar().   */
            window.dc.cursorPos.x += (style.itemSpacing.x * (-1f + 0.5f)).i.f
        } else {
            // Menu inside a menu
            popupPos.put(pos.x, pos.y - style.windowPadding.y)
            val w = window.menuColumns.declColumns(labelSize.x, 0f, (g.fontSize * 1.2f).i.f) // Feedback to next frame
            val extraW = glm.max(0f, contentRegionAvail.x - w)
            val flags = Sf.NoHoldingActiveID or Sf.PressedOnClick or Sf.DontClosePopups or Sf.DrawFillAvailWidth
            pressed = selectable(label, menuIsOpen, flags or if (enabled) Sf.None else Sf.Disabled, Vec2(w, 0f))
            if (!enabled) pushStyleColor(Col.Text, style.colors[Col.TextDisabled])
            renderArrow(pos + Vec2(window.menuColumns.pos[2] + extraW + g.fontSize * 0.3f, 0f), Dir.Right)
            if (!enabled) popStyleColor()
        }
        val hovered = enabled && itemHoverable(window.dc.lastItemRect, id)

        if (menusetIsOpen)
            g.navWindow = backedNavWindow

        var wantOpen = false
        var wantClose = false
        if (window.dc.layoutType == Lt.Vertical) {    // (window->Flags & (ImGuiWindowFlags_Popup|ImGuiWindowFlags_ChildMenu))
            /*  Implement http://bjk5.com/post/44698559168/breaking-down-amazons-mega-dropdown to avoid using timers,
                so menus feels more reactive.             */
            var movingWithinOpenedTriangle = false
            if (g.hoveredWindow === window && g.openPopupStack.size > g.beginPopupStack.size &&
                    g.openPopupStack[g.beginPopupStack.size].parentWindow === window && window.flags hasnt Wf.MenuBar)

                g.openPopupStack[g.beginPopupStack.size].window?.let {
                    // FIXME-DPI: Values should be derived from a master "scale" factor.
                    val nextWindowRect = it.rect()
                    val ta = io.mousePos - io.mouseDelta
                    val tb = if (window.pos.x < it.pos.x) nextWindowRect.tl else nextWindowRect.tr
                    val tc = if (window.pos.x < it.pos.x) nextWindowRect.bl else nextWindowRect.br
                    val extra = glm.clamp(glm.abs(ta.x - tb.x) * 0.3f, 5f, 30f) // add a bit of extra slack.
                    ta.x += if (window.pos.x < it.pos.x) -0.5f else +0.5f   // to avoid numerical issues
                    /*  triangle is maximum 200 high to limit the slope and the bias toward large sub-menus
                        FIXME: Multiply by fb_scale?                     */
                    tb.y = ta.y + glm.max((tb.y - extra) - ta.y, -100f)
                    tc.y = ta.y + glm.min((tc.y + extra) - ta.y, +100f)
                    movingWithinOpenedTriangle = triangleContainsPoint(ta, tb, tc, io.mousePos)
                    //window->DrawList->PushClipRectFullScreen(); window->DrawList->AddTriangleFilled(ta, tb, tc, movingWithinOpenedTriangle ? IM_COL32(0,128,0,128) : IM_COL32(128,0,0,128)); window->DrawList->PopClipRect(); // Debug
                }

            wantClose = (menuIsOpen && !hovered && g.hoveredWindow === window && g.hoveredIdPreviousFrame != 0 &&
                    g.hoveredIdPreviousFrame != id && !movingWithinOpenedTriangle)
            wantOpen = (!menuIsOpen && hovered && !movingWithinOpenedTriangle) || (!menuIsOpen && hovered && pressed)

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

        ImGuiTestEngineHook_ItemInfo(id, label, window.dc.itemFlags or ItemStatusFlag.Openable or if (menuIsOpen) ItemStatusFlag.Opened else ItemStatusFlag.None)

        if (!menuIsOpen && wantOpen && g.openPopupStack.size > g.beginPopupStack.size) {
            // Don't recycle same menu level in the same frame, first close the other menu and yield for a frame.
            openPopup(label)
            return false
        }

        menuIsOpen = menuIsOpen || wantOpen
        if (wantOpen) openPopup(label)

        if (menuIsOpen) {
            /*  Sub-menus are ChildWindow so that mouse can be hovering across them (otherwise top-most popup menu
                would steal focus and not allow hovering on parent menu)             */
            setNextWindowPos(popupPos, Cond.Always)
            var flags = Wf.ChildMenu or Wf.AlwaysAutoResize or Wf.NoMove or Wf.NoTitleBar or Wf.NoSavedSettings or Wf.NoNavFocus
            if (window.flags has (Wf.Popup or Wf.ChildMenu))
                flags = flags or Wf.ChildWindow
            // menuIsOpen can be 'false' when the popup is completely clipped (e.g. zero size display)
            menuIsOpen = beginPopupEx(id, flags)
        }

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
        val labelSize = calcTextSize(label, true)
        val flags = Sf.PressedOnRelease or if (enabled) Sf.None else Sf.Disabled
        val pressed: Boolean
        if (window.dc.layoutType == Lt.Horizontal) {
            /*  Mimic the exact layout spacing of beginMenu() to allow menuItem() inside a menu bar, which is a little 
                misleading but may be useful 
                Note that in this situation we render neither the shortcut neither the selected tick mark   */
            val w = labelSize.x
            window.dc.cursorPos.x += (style.itemSpacing.x * 0.5f).i.f
            pushStyleVar(StyleVar.ItemSpacing, Vec2(style.itemSpacing.x * 2f, style.itemSpacing.y))
            pressed = selectable(label, false, flags, Vec2(w, 0f))
            popStyleVar()
            /*  -1 spacing to compensate the spacing added when selectable() did a sameLine(). It would also work
                to call sameLine() ourselves after the popStyleVar().             */
            window.dc.cursorPos.x += (style.itemSpacing.x * (-1f + 0.5f)).i.f
        } else {
            val shortcutSize = if (shortcut.isNotEmpty()) calcTextSize(shortcut) else Vec2()
            val w = window.menuColumns.declColumns(labelSize.x, shortcutSize.x, (g.fontSize * 1.2f)).i.f // Feedback for next frame
            val extraW = glm.max(0f, contentRegionAvail.x - w)
            pressed = selectable(label, false, flags or Sf.DrawFillAvailWidth, Vec2(w, 0f))
            if (shortcutSize.x > 0f) {
                pushStyleColor(Col.Text, style.colors[Col.TextDisabled])
                renderText(pos + Vec2(window.menuColumns.pos[1] + extraW, 0f), shortcut, 0, false)
                popStyleColor()
            }
            if (selected)
                renderCheckMark(pos + Vec2(window.menuColumns.pos[2] + extraW + g.fontSize * 0.4f, g.fontSize * 0.134f * 0.5f),
                        (if (enabled) Col.Text else Col.TextDisabled).u32, g.fontSize * 0.866f)
        }

        ImGuiTestEngineHook_ItemInfo(window.dc.lastItemId, label, window.dc.itemFlags or ItemStatusFlag.Checkable or if (selected) ItemStatusFlag.Checked else ItemStatusFlag.None)
        return pressed
    }

    /** return true when activated + toggle (*p_selected) if p_selected != NULL */
    fun menuItem(label: String, shortcut: String = "", pSelected: BooleanArray?, enabled: Boolean = true) =
            if (menuItem(label, shortcut, pSelected?.get(0) == true, enabled)) {
                pSelected?.let { it[0] = !it[0] }
                true
            } else false

    fun menuItem(label: String, shortcut: String = "", selected: KMutableProperty0<Boolean>?, enabled: Boolean = true) =
            menuItem(label, shortcut, selected?.get() == true, enabled).also {
                if (it) selected?.apply { set(!get()) }
            }
}