package imgui.internal.api

import imgui.ImGui
import imgui.ImGui.popItemFlag
import imgui.ImGui.pushItemFlag
import imgui.StyleVar
import imgui.api.g
import imgui.internal.sections.ItemFlag
import imgui.internal.sections.has
import imgui.internal.sections.hasnt

// Disabling [BETA API]
// - Disable all user interactions and dim items visuals (applying style.DisabledAlpha over current colors)
internal interface disabling {

    /** BeginDisabled()/EndDisabled()
     *  - Those can be nested but this cannot be used to enable an already disabled section (a single BeginDisabled(true) in the stack is enough to keep things disabled)
     *  - Visually this is currently altering alpha, but it is expected that in a future styling system this would work differently.
     *  - Feedback welcome at https://github.com/ocornut/imgui/issues/211
     *  - BeginDisabled(false) essentially does nothing but is provided to facilitate use of boolean expressions */
    fun beginDisabled(disabled: Boolean = true) {
        val wasDisabled = g.currentItemFlags has ItemFlag.Disabled
        if (!wasDisabled && disabled)
            ImGui.pushStyleVar(StyleVar.Alpha, g.style.alpha * g.style.disabledAlpha)
        pushItemFlag(ItemFlag.Disabled.i, wasDisabled || disabled)
    }

    fun endDisabled() {
        popItemFlag()
        val wasDisabled = g.currentItemFlags has ItemFlag.Disabled
        if (wasDisabled && g.currentItemFlags hasnt ItemFlag.Disabled)
            ImGui.popStyleVar()
    }
}