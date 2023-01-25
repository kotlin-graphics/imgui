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

    // PushDisabled()/PopDisabled()
    // - Those are not yet exposed in imgui.h because we are unsure of how to alter the style in a way that works for everyone.
    //   We may rework this. Hypothetically, a future styling system may set a flag which make widgets use different colors.
    // - Feedback welcome at https://github.com/ocornut/imgui/issues/211
    // - You may trivially implement your own variation of this if needed.
    //   Here we test (CurrentItemFlags & ImGuiItemFlags_Disabled) to allow nested PushDisabled() calls.
    fun pushDisabled(disabled: Boolean = true) {
        val wasDisabled = g.currentItemFlags has ItemFlag.Disabled
        if (!wasDisabled && disabled)
            ImGui.pushStyleVar(StyleVar.Alpha, g.style.alpha * 0.6f)
        pushItemFlag(ItemFlag.Disabled.i, wasDisabled || disabled)
    }

    fun popDisabled() {
        popItemFlag()
        val wasDisabled = g.currentItemFlags has ItemFlag.Disabled
        if (wasDisabled && g.currentItemFlags hasnt ItemFlag.Disabled)
            ImGui.popStyleVar()
    }
}