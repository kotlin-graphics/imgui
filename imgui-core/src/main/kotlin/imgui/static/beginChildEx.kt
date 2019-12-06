package imgui.static

import glm_.glm
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.begin
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.focusWindow
import imgui.ImGui.navInitWindow
import imgui.ImGui.setActiveId
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.style
import imgui.api.g
import imgui.internal.*

fun beginChildEx(name: String, id: ID, sizeArg: Vec2, border: Boolean, flags_: WindowFlags): Boolean {

    val parentWindow = g.currentWindow!!
    var flags = WindowFlag.NoTitleBar or WindowFlag.NoResize or WindowFlag.NoSavedSettings or WindowFlag._ChildWindow
    flags = flags or (parentWindow.flags and WindowFlag.NoMove.i)  // Inherit the NoMove flag

    // Size
    val contentAvail = contentRegionAvail
    val size = floor(sizeArg)
    val autoFitAxes = (if (size.x == 0f) 1 shl Axis.X else 0x00) or (if (size.y == 0f) 1 shl Axis.Y else 0x00)
    if (size.x <= 0f)   // Arbitrary minimum child size (0.0f causing too much issues)
        size.x = glm.max(contentAvail.x + size.x, 4f)
    if (size.y <= 0f)
        size.y = glm.max(contentAvail.y + size.y, 4f)
    setNextWindowSize(size)

    // Build up name. If you need to append to a same child from multiple location in the ID stack, use BeginChild(ImGuiID id) with a stable value.
    val title = when {
        name.isNotEmpty() -> "${parentWindow.name}/$name".format(style.locale)
        else -> "${parentWindow.name}/%08X".format(style.locale, id)
    }
    val backupBorderSize = style.childBorderSize
    if (!border) style.childBorderSize = 0f
    flags = flags or flags_
    val ret = begin(title, null, flags)
    style.childBorderSize = backupBorderSize

    val childWindow = g.currentWindow!!.apply {
        childId = id
        autoFitChildAxes = autoFitAxes
    }

    // Set the cursor to handle case where the user called SetNextWindowPos()+BeginChild() manually.
    // While this is not really documented/defined, it seems that the expected thing to do.
    if (childWindow.beginCount == 1)
        parentWindow.dc.cursorPos put childWindow.pos

    // Process navigation-in immediately so NavInit can run on first frame
    if (g.navActivateId == id && flags hasnt WindowFlag._NavFlattened && (childWindow.dc.navLayerActiveMask != 0 || childWindow.dc.navHasScroll)) {
        focusWindow(childWindow)
        navInitWindow(childWindow, false)
        setActiveId(id + 1, childWindow) // Steal ActiveId with a dummy id so that key-press won't activate child item
        g.activeIdSource = InputSource.Nav
    }

    return ret
}