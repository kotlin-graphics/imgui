package imgui.imgui

import glm_.glm
import imgui.Dir
import imgui.ImGui.navInitWindow
import imgui.ImGui.setNavIDWithRectRel
import imgui.internal.NavLayer
import imgui.internal.Rect
import imgui.internal.Window
import imgui.navRestoreLastChildNavWindow


//-----------------------------------------------------------------------------
// [SECTION] VIEWPORTS, PLATFORM WINDOWS
//-----------------------------------------------------------------------------

// (this section is filled in the 'viewport' and 'docking' branches)

//-----------------------------------------------------------------------------
// [SECTION] KEYBOARD/GAMEPAD NAVIGATION
//-----------------------------------------------------------------------------

inline fun navScoreItemDistInterval(a0: Float, a1: Float, b0: Float, b1: Float) = when {
    a1 < b0 -> a1 - b0
    b1 < a0 -> a0 - b1
    else -> 0f
}

fun navClampRectToVisibleAreaForMoveDir(moveDir: Dir, r: Rect, clipRect: Rect) = when (moveDir) {
    Dir.Left, Dir.Right -> {
        r.min.y = glm.clamp(r.min.y, clipRect.min.y, clipRect.max.y)
        r.max.y = glm.clamp(r.max.y, clipRect.min.y, clipRect.max.y)
    }
    else -> {
        r.min.x = glm.clamp(r.min.x, clipRect.min.x, clipRect.max.x)
        r.max.x = glm.clamp(r.max.x, clipRect.min.x, clipRect.max.x)
    }
}

fun navRestoreLayer(layer: NavLayer) {

    g.navLayer = layer
    if (layer == NavLayer.Main)
        g.navWindow = navRestoreLastChildNavWindow(g.navWindow!!)
    if (layer == NavLayer.Main && g.navWindow!!.navLastIds[0] != 0)
        setNavIDWithRectRel(g.navWindow!!.navLastIds[0], layer, g.navWindow!!.navRectRel[0])
    else
        navInitWindow(g.navWindow!!, true)
}

fun setCurrentWindow(window: Window?) {
    g.currentWindow = window
    if (window != null)
        g.fontSize = window.calcFontSize()
    g.drawListSharedData.fontSize = g.fontSize
}