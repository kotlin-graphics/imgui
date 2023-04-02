package imgui.internal.api

import imgui.api.g
import imgui.internal.sections.ItemFlag

// Disabling [BETA API]
// - Disable all user interactions and dim items visuals (applying style.DisabledAlpha over current colors)
// - Those can be nested but it cannot be used to enable an already disabled section (a single BeginDisabled(true) in the stack is enough to keep everything disabled)
// - BeginDisabled(false) essentially does nothing useful but is provided to facilitate use of boolean expressions. If you can avoid calling BeginDisabled(False)/EndDisabled() best to avoid it.
internal interface disabling {

    /** BeginDisabled()/EndDisabled()
     *  - Those can be nested but it cannot be used to enable an already disabled section (a single BeginDisabled(true) in the stack is enough to keep everything disabled)
     *  - Visually this is currently altering alpha, but it is expected that in a future styling system this would work differently.
     *  - Feedback welcome at https://github.com/ocornut/imgui/issues/211
     *  - BeginDisabled(false) essentially does nothing useful but is provided to facilitate use of boolean expressions. If you can avoid calling BeginDisabled(False)/EndDisabled() best to avoid it.
     *  - Optimized shortcuts instead of PushStyleVar() + PushItemFlag() */
    fun beginDisabled(disabled: Boolean = true) {
        val wasDisabled = g.currentItemFlags has ItemFlag.Disabled
        if (!wasDisabled && disabled) {
            g.disabledAlphaBackup = g.style.alpha
            g.style.alpha *= g.style.disabledAlpha // PushStyleVar(ImGuiStyleVar_Alpha, g.Style.Alpha * g.Style.DisabledAlpha);
        }
        if (wasDisabled || disabled)
        g.currentItemFlags /= ItemFlag.Disabled
        g.itemFlagsStack += g.currentItemFlags
        g.disabledStackSize++
    }

    fun endDisabled() {
        assert(g.disabledStackSize > 0)
        g.disabledStackSize--
        val wasDisabled = g.currentItemFlags has ItemFlag.Disabled
        //popItemFlag()
        g.itemFlagsStack.pop()
        g.currentItemFlags = g.itemFlagsStack.last()
        if (wasDisabled && g.currentItemFlags hasnt ItemFlag.Disabled)
            g.style.alpha = g.disabledAlphaBackup //PopStyleVar();
    }
}