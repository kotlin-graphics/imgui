package imgui.imgui

import gli.has
import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import imgui.*
import imgui.Context.style
import imgui.ImGui.alignFirstTextHeightToWidgets
import imgui.ImGui.begin
import imgui.ImGui.beginGroup
import imgui.ImGui.beginPopupEx
import imgui.ImGui.calcTextSize
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.currentWindow
import imgui.ImGui.end
import imgui.ImGui.endGroup
import imgui.ImGui.endPopup
import imgui.ImGui.isHovered
import imgui.ImGui.isPopupOpen
import imgui.ImGui.openPopup
import imgui.ImGui.popClipRect
import imgui.ImGui.popId
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushClipRect
import imgui.ImGui.pushId
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.renderCheckMark
import imgui.ImGui.renderCollapseTriangle
import imgui.ImGui.renderText
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.internal.LayoutType
import imgui.internal.Rect
import imgui.internal.triangleContainsPoint
import imgui.Context as g
import imgui.WindowFlags as Wf
import imgui.SelectableFlags as Sf

/** Menu    */
interface imgui_menus {


    /** create and append to a full screen menu-bar. only call EndMainMenuBar() if this returns true!   */
    fun beginMainMenuBar(): Boolean {

        setNextWindowPos(Vec2())
        setNextWindowSize(Vec2(IO.displaySize.x, g.fontBaseSize + style.framePadding.y * 2f))
        pushStyleVar(StyleVar.WindowRounding, 0f)
        pushStyleVar(StyleVar.WindowMinSize, Vec2i())
        val flags = Wf.NoTitleBar or Wf.NoResize or Wf.NoMove or Wf.NoScrollbar or Wf.NoSavedSettings or Wf.MenuBar
        if (!begin("##MainMenuBar", null, flags) || !beginMenuBar()) {
            end()
            popStyleVar(2)
            return false
        }
        g.currentWindow!!.dc.menuBarOffsetX += style.displaySafeAreaPadding.x
        return true
    }

    fun endMainMenuBar() {
        endMenuBar()
        end()
        popStyleVar(2)
    }

    /** append to menu-bar of current window (requires WindowFlags.MenuBar flag set on parent window).
     *  Only call endMenuBar() if this returns true!    */
    fun beginMenuBar(): Boolean {

        val window = currentWindow
        if (window.skipItems) return false
        if (window.flags hasnt Wf.MenuBar) return false

        assert(!window.dc.menuBarAppending)
        beginGroup() // Save position
        pushId("##menubar")
        val rect = Rect(window.menuBarRect())
        pushClipRect(Vec2(glm.floor(rect.min.x + 0.5f), glm.floor(rect.min.y + window.borderSize + 0.5f)),
                Vec2(glm.floor(rect.max.x + 0.5f), glm.floor(rect.max.y + 0.5f)), false)
        window.dc.cursorPos = Vec2(rect.min.x + window.dc.menuBarOffsetX, rect.min.y)// + g.style.FramePadding.y);
        window.dc.layoutType = LayoutType.Horizontal
        window.dc.menuBarAppending = true
        alignFirstTextHeightToWidgets()
        return true
    }

    fun endMenuBar() {

        val window = currentWindow
        if (window.skipItems) return

        assert(window.flags has Wf.MenuBar)
        assert(window.dc.menuBarAppending)
        popClipRect()
        popId()
        window.dc.menuBarOffsetX = window.dc.cursorPos.x - window.menuBarRect().min.x
        window.dc.groupStack.last().advanceCursor = false
        endGroup()
        window.dc.layoutType = LayoutType.Vertical
        window.dc.menuBarAppending = false
    }

    /** create a sub-menu entry. only call EndMenu() if this returns true!  */
    fun beginMenu(label: String, enabled: Boolean = true): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(label)

        val labelSize = calcTextSize(label, true)

        val pressed: Boolean
        var menuIsOpen = isPopupOpen(id)
        val menusetIsOpen = window.flags hasnt Wf.Popup && g.openPopupStack.size > g.currentPopupStack.size &&
                g.openPopupStack[g.currentPopupStack.size].parentMenuSet == window.getId("##menus")
        val backedNavWindow = g.navWindow
        if (menusetIsOpen)
        // Odd hack to allow hovering across menus of a same menu-set (otherwise we wouldn't be able to hover parent)
            g.navWindow = window

        /*  The reference position stored in popupPos will be used by Begin() to find a suitable position for the child
            menu (using FindBestPopupWindowPos).         */
        val popupPos = Vec2()
        val pos = Vec2(window.dc.cursorPos)
        if (window.dc.layoutType == LayoutType.Horizontal) {
            // Menu inside an horizontal menu bar
            // Selectable extend their highlight by half ItemSpacing in each direction.
            popupPos.put(pos.x - window.windowPadding.x, pos.y - style.framePadding.y + window.menuBarHeight())
            window.dc.cursorPos.x += (style.itemSpacing.x * 0.5f).i.f
            pushStyleVar(StyleVar.ItemSpacing, style.itemSpacing * 2f)
            val w = labelSize.x
            val flags = Sf.Menu or Sf.DontClosePopups or if (enabled) 0 else Sf.Disabled.i
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
            val flags = Sf.Menu or Sf.DontClosePopups or Sf.DrawFillAvailWidth
            pressed = selectable(label, menuIsOpen, flags or if (enabled) Sf.Null else Sf.Disabled, Vec2(w, 0f))
            if (!enabled) pushStyleColor(Col.Text, style.colors[Col.TextDisabled])
            renderCollapseTriangle(pos + Vec2(window.menuColumns.pos[2] + extraW + g.fontSize * 0.2f, 0f), false)
            if (!enabled) popStyleColor()
        }

        val hovered = enabled && isHovered(window.dc.lastItemRect, id)

        if (menusetIsOpen)
            g.navWindow = backedNavWindow

        var wantOpen = false
        var wantClose = false
        if (window.flags has (Wf.Popup or Wf.ChildMenu)) {
            /*  Implement http://bjk5.com/post/44698559168/breaking-down-amazons-mega-dropdown to avoid using timers,
                so menus feels more reactive.             */
            var movingWithinOpenedTriangle = false
            if (g.hoveredWindow === window && g.openPopupStack.size > g.currentPopupStack.size &&
                    g.openPopupStack[g.currentPopupStack.size].parentWindow === window)

                g.openPopupStack[g.currentPopupStack.size].window?.let {
                    val nextWindowRect = it.rect()
                    val ta = IO.mousePos - IO.mouseDelta
                    val tb = if (window.pos.x < it.pos.x) nextWindowRect.tl else nextWindowRect.tr
                    val tc = if (window.pos.x < it.pos.x) nextWindowRect.bl else nextWindowRect.br
                    val extra = glm.clamp(glm.abs(ta.x - tb.x) * 0.3f, 5f, 30f) // add a bit of extra slack.
                    ta.x += if (window.pos.x < it.pos.x) -0.5f else +0.5f   // to avoid numerical issues
                    /*  triangle is maximum 200 high to limit the slope and the bias toward large sub-menus
                        FIXME: Multiply by fb_scale?                     */
                    tb.y = ta.y + glm.max((tb.y - extra) - ta.y, -100f)
                    tc.y = ta.y + glm.min((tc.y + extra) - ta.y, +100f)
                    movingWithinOpenedTriangle = triangleContainsPoint(ta, tb, tc, IO.mousePos)
                    //window->DrawList->PushClipRectFullScreen(); window->DrawList->AddTriangleFilled(ta, tb, tc, movingWithinOpenedTriangle ? IM_COL32(0,128,0,128) : IM_COL32(128,0,0,128)); window->DrawList->PopClipRect(); // Debug
                }

            wantClose = (menuIsOpen && !hovered && g.hoveredWindow === window && g.hoveredIdPreviousFrame != 0 &&
                    g.hoveredIdPreviousFrame != id && !movingWithinOpenedTriangle)
            wantOpen = (!menuIsOpen && hovered && !movingWithinOpenedTriangle) || (!menuIsOpen && hovered && pressed)
        } else if (menuIsOpen && pressed && menusetIsOpen) {    // Menu bar: click an open menu again to close it
            wantClose = true
            wantOpen = false
            menuIsOpen = false
        } else if (pressed || (hovered && menusetIsOpen && !menuIsOpen)) // menu-bar: first click to open, then hover to open others
            wantOpen = true
        /*  explicitly close if an open menu becomes disabled, facilitate users code a lot in pattern such as
            'if (BeginMenu("options", has_object)) { ..use object.. }'         */
        if (!enabled)
            wantClose = true
        if (wantClose && isPopupOpen(id))
            closePopupToLevel(g.currentPopupStack.size)

        if (!menuIsOpen && wantOpen && g.openPopupStack.size > g.currentPopupStack.size) {
            // Don't recycle same menu level in the same frame, first close the other menu and yield for a frame.
            openPopup(label)
            return false
        }

        menuIsOpen = menuIsOpen || wantOpen
        if (wantOpen)
            openPopup(label)

        if (menuIsOpen) {
            setNextWindowPos(popupPos, Cond.Always)
            val flags = Wf.ShowBorders or Wf.AlwaysAutoResize or (
                    if (window.flags has (Wf.Popup or Wf.ChildMenu)) Wf.ChildMenu or Wf.ChildWindow
                    else Wf.ChildMenu.i)
            // menuIsOpen can be 'false' when the popup is completely clipped (e.g. zero size display)
            menuIsOpen = beginPopupEx(id, flags)
        }

        return menuIsOpen
    }

    fun endMenu() = endPopup()

    /** return true when activated. shortcuts are displayed for convenience but not processed by ImGui at the moment    */
    fun menuItem(label: String, shortcut: String = "", selected: Boolean = false, enabled: Boolean = true): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val pos = Vec2(window.dc.cursorPos)
        val labelSize = calcTextSize(label, true)
        val shortcutSize = if (shortcut.isNotEmpty()) calcTextSize(shortcut, 0) else Vec2()
        val w = window.menuColumns.declColumns(labelSize.x, shortcutSize.x, (g.fontSize * 1.2f).i.f) // Feedback for next frame
        val extraW = glm.max(0f, contentRegionAvail.x - w)

        val flags = Sf.MenuItem or Sf.DrawFillAvailWidth or if (enabled) Sf.Null else Sf.Disabled
        val pressed = selectable(label, false, flags, Vec2(w, 0f))
        if (shortcutSize.x > 0f) {
            pushStyleColor(Col.Text, style.colors[Col.TextDisabled])
            renderText(pos + Vec2(window.menuColumns.pos[1] + extraW, 0f), shortcut, 0, false)
            popStyleColor()
        }

        if (selected)
            renderCheckMark(pos + Vec2(window.menuColumns.pos[2] + extraW + g.fontSize * 0.2f, 0f), (if (enabled) Col.Text else Col.TextDisabled).u32)

        return pressed
    }

    /** return true when activated + toggle (*p_selected) if p_selected != NULL */
    fun menuItem(label: String, shortcut: String = "", pSelected: BooleanArray?, enabled: Boolean = true) =
            if (menuItem(label, shortcut, pSelected?.get(0) ?: false, enabled)) {
                pSelected?.let { it[0] = !it[0] }
                true
            } else false
}