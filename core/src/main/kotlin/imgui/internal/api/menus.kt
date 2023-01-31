package imgui.internal.api

import glm_.glm
import glm_.max
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.calcTextSize
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.popID
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.beginDisabled
import imgui.ImGui.pushID
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.renderText
import imgui.ImGui.selectable
import imgui.ImGui.style
import imgui.api.g
import imgui.internal.floor
import imgui.internal.sections.*
import imgui.internal.triangleContainsPoint
import kotlin.math.abs

// Menus
internal interface menus {


    /** Important: calling order matters!
     *  FIXME: Somehow overlapping with docking tech.
     *  FIXME: The "rect-cut" aspect of this could be formalized into a lower-level helper (rect-cut: https://halt.software/dead-simple-layouts) */
    fun beginViewportSideBar(name: String, viewportP: Viewport?, dir: Dir, axisSize: Float, windowFlags_: WindowFlags): Boolean {
        assert(dir != Dir.None)

        val barWindow = ImGui.findWindowByName(name)
        if (barWindow == null || barWindow.beginCount == 0) {
            // Calculate and set window size/position
            val viewport = (viewportP ?: ImGui.mainViewport) as ViewportP
            val availRect = viewport.buildWorkRect
            val axis = when (dir) {
                Dir.Up, Dir.Down -> Axis.Y
                else -> Axis.X
            }
            val pos = Vec2(availRect.min)
            if (dir == Dir.Right || dir == Dir.Down)
                pos[axis] = availRect.max[axis] - axisSize
            val size = availRect.size
            size[axis] = axisSize
            ImGui.setNextWindowPos(pos)
            ImGui.setNextWindowSize(size)

            // Report our size into work area (for next frame) using actual window size
            if (dir == Dir.Up || dir == Dir.Left)
                viewport.buildWorkOffsetMin[axis] += axisSize
            else if (dir == Dir.Down || dir == Dir.Right)
                viewport.buildWorkOffsetMax[axis] -= axisSize
        }

        val windowFlags = windowFlags_ or WindowFlag.NoTitleBar or WindowFlag.NoResize or WindowFlag.NoMove
        pushStyleVar(StyleVar.WindowRounding, 0f)
        pushStyleVar(StyleVar.WindowMinSize, Vec2(0, 0)) // Lift normal size constraint
        val isOpen = ImGui.begin(name, null, windowFlags)
        popStyleVar(2)

        return isOpen
    }

    /** create a sub-menu entry. only call EndMenu() if this returns true!  */
    fun beginMenuEx(label: String, icon: String, enabled: Boolean = true): Boolean {

        val window = ImGui.currentWindow
        if (window.skipItems) return false

        val id = window.getID(label)
        var menuIsOpen = ImGui.isPopupOpen(id)

        // Sub-menus are ChildWindow so that mouse can be hovering across them (otherwise top-most popup menu would steal focus and not allow hovering on parent menu)
        // The first menu in a hierarchy isn't so hovering doesn't get accross (otherwise e.g. resizing borders with ImGuiButtonFlags_FlattenChildren would react), but top-most BeginMenu() will bypass that limitation.
        var flags = WindowFlag._ChildMenu or WindowFlag.AlwaysAutoResize or WindowFlag.NoMove or WindowFlag.NoTitleBar or WindowFlag.NoSavedSettings or WindowFlag.NoNavFocus
        if (window.flags has WindowFlag._ChildMenu)
            flags = flags or WindowFlag._ChildWindow

        // If a menu with same the ID was already submitted, we will append to it, matching the behavior of Begin().
        // We are relying on a O(N) search - so O(N log N) over the frame - which seems like the most efficient for the expected small amount of BeginMenu() calls per frame.
        // If somehow this is ever becoming a problem we can switch to use e.g. ImGuiStorage mapping key to last frame used.
        if (id in g.menusIdSubmittedThisFrame) {
            if (menuIsOpen)
                menuIsOpen = ImGui.beginPopupEx(id, flags) // menu_is_open can be 'false' when the popup is completely clipped (e.g. zero size display)
            else
                g.nextWindowData.clearFlags()          // we behave like Begin() and need to consume those values
            return menuIsOpen
        }

        // Tag menu as used. Next time BeginMenu() with same ID is called it will append to existing menu
        g.menusIdSubmittedThisFrame += id

        val labelSize = calcTextSize(label, hideTextAfterDoubleHash = true)

        // Odd hack to allow hovering across menus of a same menu-set (otherwise we wouldn't be able to hover parent without always being a Child window)
        val menusetIsOpen = widgets.isRootOfOpenMenuSet
        val backedNavWindow = g.navWindow
        if (menusetIsOpen)
            g.navWindow = window

        // The reference position stored in popup_pos will be used by Begin() to find a suitable position for the child menu,
        // However the final position is going to be different! It is chosen by FindBestWindowPosForPopup().
        // e.g. Menus tend to overlap each other horizontally to amplify relative Z-ordering.
        val popupPos = Vec2()
        val pos = Vec2(window.dc.cursorPos)
        pushID(label)
        if (!enabled)
            beginDisabled()
        val offsets = window.dc.menuColumns
        val pressed: Boolean
        val selectableFlags = SelectableFlag._NoHoldingActiveID or SelectableFlag._SelectOnClick or SelectableFlag.DontClosePopups
        if (window.dc.layoutType == LayoutType.Horizontal) {
            // Menu inside an horizontal menu bar
            // Selectable extend their highlight by half ItemSpacing in each direction.
            // For ChildMenu, the popup position will be overwritten by the call to FindBestWindowPosForPopup() in Begin()
            popupPos.put(pos.x - 1f - floor(style.itemSpacing.x * 0.5f), pos.y - style.framePadding.y + window.menuBarHeight)
            window.dc.cursorPos.x += floor(style.itemSpacing.x * 0.5f)
            pushStyleVar(StyleVar.ItemSpacing, Vec2(style.itemSpacing.x * 2f, style.itemSpacing.y))
            val w = labelSize.x
            val textPos = Vec2(window.dc.cursorPos.x + offsets.offsetLabel, window.dc.cursorPos.y + window.dc.currLineTextBaseOffset)
            pressed = selectable("", menuIsOpen, selectableFlags, Vec2(w, 0f))
            renderText(textPos, label)
            popStyleVar()
            /*  -1 spacing to compensate the spacing added when selectable() did a sameLine(). It would also work
                to call sameLine() ourselves after the popStyleVar().   */
            window.dc.cursorPos.x += floor(style.itemSpacing.x * (-1f + 0.5f))
        } else {
            // Menu inside a regular/vertical menu
            // (In a typical menu window where all items are BeginMenu() or MenuItem() calls, extra_w will always be 0.0f.
            //  Only when they are other items sticking out we're going to add spacing, yet only register minimum width into the layout system.
            popupPos.put(pos.x, pos.y - style.windowPadding.y)
            val iconW = if(icon.isNotEmpty()) calcTextSize(icon).x else 0f
            val checkmarkW = floor(g.fontSize * 1.2f)
            val minW = window.dc.menuColumns.declColumns(iconW, labelSize.x, 0f, checkmarkW) // Feedback to next frame
            val extraW = 0f max (contentRegionAvail.x - minW)
            val textPos = Vec2(window.dc.cursorPos.x + offsets.offsetLabel, window.dc.cursorPos.y + window.dc.currLineTextBaseOffset)
            pressed = selectable("", menuIsOpen, selectableFlags or SelectableFlag._SpanAvailWidth, Vec2(minW, 0f))
            renderText(textPos, label)
            if (iconW > 0f)
                renderText(pos + Vec2(offsets.offsetIcon, 0f), icon)
            window.drawList.renderArrow(pos + Vec2(offsets.offsetMark + extraW + g.fontSize * 0.3f, 0f), Col.Text.u32, Dir.Right)
        }
        if (!enabled)
            ImGui.endDisabled()

        val hovered = g.hoveredId == id && enabled && !g.navDisableMouseHover

        if (menusetIsOpen)
            g.navWindow = backedNavWindow

        var wantOpen = false
        var wantClose = false
        if (window.dc.layoutType == LayoutType.Vertical) {    // (window->Flags & (ImGuiWindowFlags_Popup|ImGuiWindowFlags_ChildMenu))
            // Close menu when not hovering it anymore unless we are moving roughly in the direction of the menu
            // Implement http://bjk5.com/post/44698559168/breaking-down-amazons-mega-dropdown to avoid using timers, so menus feels more reactive.
            var movingTowardChildMenu = false
            val childMenuWindow = when {
                g.beginPopupStack.size < g.openPopupStack.size && g.openPopupStack[g.beginPopupStack.size].sourceWindow == window -> g.openPopupStack[g.beginPopupStack.size].window
                else -> null
            }
            if (g.hoveredWindow === window && childMenuWindow != null && window.flags hasnt WindowFlag.MenuBar) {
                val refUnit = g.fontSize // FIXME-DPI
                val nextWindowRect = childMenuWindow.rect()
                val ta = ImGui.io.mousePos - ImGui.io.mouseDelta
                val tb = if (window.pos.x < childMenuWindow.pos.x) nextWindowRect.tl else nextWindowRect.tr
                val tc = if (window.pos.x < childMenuWindow.pos.x) nextWindowRect.bl else nextWindowRect.br
                val extra = glm.clamp(abs(ta.x - tb.x) * 0.3f, refUnit * 0.5f, refUnit * 2.5f)    // add a bit of extra slack.
                ta.x += if (window.pos.x < childMenuWindow.pos.x) -0.5f else +0.5f // to avoid numerical issues (FIXME: ??)
                tb.y = ta.y + kotlin.math.max((tb.y - extra) - ta.y, -refUnit * 8f)                // triangle has maximum height to limit the slope and the bias toward large sub-menus
                tc.y = ta.y + min((tc.y + extra) - ta.y, +refUnit * 8f)
                movingTowardChildMenu = triangleContainsPoint(ta, tb, tc, ImGui.io.mousePos)
                //GetForegroundDrawList()->AddTriangleFilled(ta, tb, tc, moving_toward_other_child_menu ? IM_COL32(0,128,0,128) : IM_COL32(128,0,0,128)); // [DEBUG]
            }

            // The 'HovereWindow == window' check creates an inconsistency (e.g. moving away from menu slowly tends to hit same window, whereas moving away fast does not)
            // But we also need to not close the top-menu menu when moving over void. Perhaps we should extend the triangle check to a larger polygon.
            // (Remember to test this on BeginPopup("A")->BeginMenu("B") sequence which behaves slightly differently as B isn't a Child of A and hovering isn't shared.)
            if (menuIsOpen && !hovered && g.hoveredWindow === window && !movingTowardChildMenu)
                wantClose = true

            // Open
            if (!menuIsOpen && pressed) // Click/activate to open
                wantOpen = true
            else if (!menuIsOpen && hovered && !movingTowardChildMenu) // Hover to open
                wantOpen = true

            if (g.navId == id && g.navMoveDir == Dir.Right) { // Nav-Right to open
                wantOpen = true
                ImGui.navMoveRequestCancel()
            }
        } else  // Menu bar
            if (menuIsOpen && pressed && menusetIsOpen) { // Click an open menu again to close it
                wantClose = true
                wantOpen = false
                menuIsOpen = false
            } else if (pressed || (hovered && menusetIsOpen && !menuIsOpen)) // First click to open, then hover to open others
                wantOpen = true
            else if (g.navId == id && g.navMoveDir == Dir.Down) { // Nav-Down to open
                wantOpen = true
                ImGui.navMoveRequestCancel()
            }

        if (!enabled) // explicitly close if an open menu becomes disabled, facilitate users code a lot in pattern such as 'if (BeginMenu("options", has_object)) { ..use object.. }'
            wantClose = true
        if (wantClose && ImGui.isPopupOpen(id))
            ImGui.closePopupToLevel(g.beginPopupStack.size, true)

        IMGUI_TEST_ENGINE_ITEM_INFO(id, label, g.lastItemData.statusFlags or ItemStatusFlag.Openable or if (menuIsOpen) ItemStatusFlag.Opened else ItemStatusFlag.None)
        popID()

        if (!menuIsOpen && wantOpen && g.openPopupStack.size > g.beginPopupStack.size) {
            // Don't recycle same menu level in the same frame, first close the other menu and yield for a frame.
            ImGui.openPopup(label)
            return false
        }

        menuIsOpen = menuIsOpen || wantOpen
        if (wantOpen) ImGui.openPopup(label)

        if (menuIsOpen) {
            ImGui.setNextWindowPos(popupPos, Cond.Always) // Note: this is super misleading! The value will serve as reference for FindBestWindowPosForPopup(), not actual pos.
            pushStyleVar(StyleVar.ChildRounding, style.popupRounding) // First level will use _PopupRounding, subsequent will use _ChildRounding
            menuIsOpen = ImGui.beginPopupEx(id, flags) // menu_is_open can be 'false' when the popup is completely clipped (e.g. zero size display)
            popStyleVar()
        } else
            g.nextWindowData.clearFlags() // We behave like Begin() and need to consume those values


        return menuIsOpen
    }

    /** return true when activated. */
    fun menuItemEx(label: String, icon: String, shortcut: String = "", selected: Boolean = false, enabled: Boolean = true): Boolean {

        val window = ImGui.currentWindow
        if (window.skipItems) return false

        val pos = Vec2(window.dc.cursorPos)
        val labelSize = ImGui.calcTextSize(label, hideTextAfterDoubleHash = true)

        val menusetIsOpen = widgets.isRootOfOpenMenuSet
        val backedNavWindow = g.navWindow
        if (menusetIsOpen)
            g.navWindow = window

        // We've been using the equivalent of ImGuiSelectableFlags_SetNavIdOnHover on all Selectable() since early Nav system days (commit 43ee5d73),
        // but I am unsure whether this should be kept at all. For now moved it to be an opt-in feature used by menus only.
        val pressed: Boolean
        pushID(label)
        if (!enabled)
            beginDisabled()

        val selectableFlags = SelectableFlag._SelectOnRelease or SelectableFlag._SetNavIdOnHover
        val offsets = window.dc.menuColumns
        if (window.dc.layoutType == LayoutType.Horizontal) {
            // Mimic the exact layout spacing of BeginMenu() to allow MenuItem() inside a menu bar, which is a little misleading but may be useful
            // Note that in this situation: we don't render the shortcut, we render a highlight instead of the selected tick mark.
            val w = labelSize.x
            window.dc.cursorPos.x += floor(style.itemSpacing.x * 0.5f)
            val textPos = Vec2(window.dc.cursorPos.x + offsets.offsetLabel, window.dc.cursorPos.y + window.dc.currLineTextBaseOffset)
            pushStyleVar(StyleVar.ItemSpacing, Vec2(style.itemSpacing.x * 2f, style.itemSpacing.y))
            pressed = selectable("", selected, selectableFlags, Vec2(w, 0f))
            popStyleVar()
            renderText(textPos, label)
            window.dc.cursorPos.x += floor(style.itemSpacing.x * (-1f + 0.5f)) // -1 spacing to compensate the spacing added when Selectable() did a SameLine(). It would also work to call SameLine() ourselves after the PopStyleVar().
        } else {
            // Menu item inside a vertical menu
            // (In a typical menu window where all items are BeginMenu() or MenuItem() calls, extra_w will always be 0.0f.
            //  Only when they are other items sticking out we're going to add spacing, yet only register minimum width into the layout system.
            val iconW = if(icon.isNotEmpty()) calcTextSize(icon).x else 0f
            val shortcutW = if(shortcut.isNotEmpty()) calcTextSize(shortcut).x else 0f
            val checkmarkW = floor(g.fontSize * 1.2f)
            val minW = window.dc.menuColumns.declColumns(iconW, labelSize.x, shortcutW, checkmarkW) // Feedback for next frame
            val stretchW = 0f max (contentRegionAvail.x - minW)
            pressed = selectable("", false, selectableFlags or SelectableFlag._SpanAvailWidth, Vec2(minW, 0f))
            renderText(pos + Vec2(offsets.offsetLabel, 0f), label)
            if (iconW > 0f)
                renderText(pos + Vec2(offsets.offsetIcon, 0f), icon)
            if (shortcutW > 0f) {
                pushStyleColor(Col.Text, style.colors[Col.TextDisabled])
                renderText(pos + Vec2(offsets.offsetShortcut + stretchW, 0f), shortcut, false)
                popStyleColor()
            }
            if (selected)
                window.drawList.renderCheckMark(pos + Vec2(offsets.offsetMark + stretchW + g.fontSize * 0.4f, g.fontSize * 0.134f * 0.5f),
                                                Col.Text.u32, g.fontSize * 0.866f)
        }
        IMGUI_TEST_ENGINE_ITEM_INFO(g.lastItemData.id, label, g.lastItemData.statusFlags or ItemStatusFlag.Checkable or if (selected) ItemStatusFlag.Checked else ItemStatusFlag.None)
        if (!enabled)
            popStyleColor()
        popID()
        if (menusetIsOpen)
            g.navWindow = backedNavWindow

        return pressed
    }
}