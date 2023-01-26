package imgui.internal.api

import glm_.wo
import imgui.ImGui
import imgui.StyleVar
import imgui.api.g
import imgui.internal.sections.ItemFlag
import imgui.internal.sections.ItemFlags
import imgui.internal.sections.has
import imgui.internal.sections.hasnt

internal interface parameterStacks {

    /** allow focusing using TAB/Shift-TAB, enabled by default but you can disable it for certain widgets
     *  @param option = ItemFlag   */
    fun pushItemFlag(option: ItemFlags, enabled: Boolean) {
        var itemFlags = g.currentItemFlags
        assert(itemFlags == g.itemFlagsStack.last())
        itemFlags = when {
            enabled -> itemFlags or option
            else -> itemFlags wo option
        }
        g.currentItemFlags = itemFlags
        g.itemFlagsStack += itemFlags
    }

    fun popItemFlag() {
        assert(g.itemFlagsStack.size > 1) { "Too many calls to PopItemFlag() - we always leave a 0 at the bottom of the stack." }
        g.itemFlagsStack.pop()
        g.currentItemFlags = g.itemFlagsStack.last()
    }
}