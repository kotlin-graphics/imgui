package imgui.api

import gli_.has
import glm_.max
import imgui.*
import imgui.ImGui.alignTextToFramePadding
import imgui.ImGui.beginGroup
import imgui.ImGui.beginMenuEx
import imgui.ImGui.beginViewportSideBar
import imgui.ImGui.closePopupToLevel
import imgui.ImGui.currentWindow
import imgui.ImGui.end
import imgui.ImGui.endGroup
import imgui.ImGui.endPopup
import imgui.ImGui.focusTopMostWindowUnderOne
import imgui.ImGui.focusWindow
import imgui.ImGui.frameHeight
import imgui.ImGui.mainViewport
import imgui.ImGui.menuItemEx
import imgui.ImGui.navMoveRequestButNoResultYet
import imgui.ImGui.navMoveRequestCancel
import imgui.ImGui.navMoveRequestForward
import imgui.ImGui.popClipRect
import imgui.ImGui.popID
import imgui.ImGui.pushClipRect
import imgui.ImGui.pushID
import imgui.ImGui.setNavID
import imgui.ImGui.style
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.round
import imgui.internal.sections.*
import kotlin.math.max
import kotlin.reflect.KMutableProperty0
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
            window.dc.isSameLine = false
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
            if (navEarliestChild.parentWindow == window && navEarliestChild.dc.parentLayoutType == Lt.Horizontal && g.navMoveFlags hasnt NavMoveFlag.Forwarded) {
                // To do so we claim focus back, restore NavId and then process the movement request for yet another frame.
                // This involve a one-frame delay which isn't very problematic in this situation. We could remove it by scoring in advance for multiple window (probably not worth bothering)
                val layer = NavLayer.Menu
                assert(window.dc.navLayersActiveMaskNext has (1 shl layer)) { "Sanity check" }
                focusWindow(window)
                setNavID(window.navLastIds[layer], layer, 0, window.navRectRel[layer])
                g.navDisableHighlight = true // Hide highlight for the current frame so we don't see the intermediary selection.
                g.navDisableMouseHover = true; g.navMousePosDirty = true
                navMoveRequestForward(g.navMoveDir, g.navMoveClipDir, g.navMoveFlags, g.navMoveScrollFlags) // Repeat
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
            window.dc.isSameLine = false
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

    fun beginMenu(label: String, enabled: Boolean = true) = beginMenuEx(label, "", enabled)

    /** Only call EndMenu() if BeginMenu() returns true! */
    fun endMenu() {
        // Nav: When a left move request _within our child menu_ failed, close ourselves (the _parent_ menu).
        // A menu doesn't close itself because EndMenuBar() wants to catch the last Left<>Right inputs.
        // However, it means that with the current code, a BeginMenu() from outside another menu or a menu-bar won't be closable with the Left direction.
        // FIXME: This doesn't work if the parent BeginMenu() is not on a menu.
        val window = g.currentWindow!!
        if (g.navMoveDir == Dir.Left && navMoveRequestButNoResultYet() && window.dc.layoutType == Lt.Vertical)
            g.navWindow?.rootWindowForNav?.let {
                if (it.flags has Wf._Popup && it.parentWindow === window) {
                    closePopupToLevel(g.beginPopupStack.size, true)
                    navMoveRequestCancel()
                }
            }
        endPopup()
    }

    fun menuItem(label: String, shortcut: String = "", selected: Boolean = false, enabled: Boolean = true): Boolean =
        menuItemEx(label, "", shortcut, selected, enabled)

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