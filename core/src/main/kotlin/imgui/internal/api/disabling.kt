package imgui.internal.api

import imgui.ImGui
import imgui.api.g
import imgui.internal.sections.ItemFlag
import imgui.internal.sections.div
import imgui.internal.sections.minus
import imgui.internal.sections.has
import imgui.internal.sections.hasnt

// Disabling [BETA API]
// - Disable all user interactions and dim items visuals (applying style.DisabledAlpha over current colors)
internal interface disabling {

    /** BeginDisabled()/EndDisabled()
     *  - Those can be nested but this cannot be used to enable an already disabled section (a single BeginDisabled(true) in the stack is enough to keep things disabled)
     *  - Visually this is currently altering alpha, but it is expected that in a future styling system this would work differently.
     *  - Feedback welcome at https://github.com/ocornut/imgui/issues/211
     *  - BeginDisabled(false) essentially does nothing but is provided to facilitate use of boolean expressions
     *  - Optimized shortcuts instead of PushStyleVar() + PushItemFlag() */
    fun beginDisabled(disabled: Boolean = true) {
        val wasDisabled = g.currentItemFlags has ItemFlag.Disabled
        g.disabledAlphaBackup = g.style.alpha
        if (!wasDisabled && disabled)
            g.style.alpha *= g.style.disabledAlpha // PushStyleVar(ImGuiStyleVar_Alpha, g.Style.Alpha * g.Style.DisabledAlpha);
        //PushItemFlag(ImGuiItemFlags_Disabled, was_disabled || disabled);
        g.currentItemFlags /= ItemFlag.Disabled
        g.itemFlagsStack += g.currentItemFlags
    }

    fun endDisabled() {
        val wasDisabled = g.currentItemFlags has ItemFlag.Disabled
        //popItemFlag()
        g.itemFlagsStack.pop()
        g.currentItemFlags -= ItemFlag.Disabled
        if (wasDisabled && g.currentItemFlags hasnt ItemFlag.Disabled)
            g.style.alpha = g.disabledAlphaBackup //PopStyleVar();
    }
}