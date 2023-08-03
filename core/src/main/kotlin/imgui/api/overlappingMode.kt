package imgui.api

import imgui.div
import imgui.internal.sections.ItemFlag

// Overlapping mode
interface overlappingMode {

    // Allow next item to be overlapped by subsequent items.
    // This works by requiring HoveredId to match for two subsequent frames,
    // so if a following items overwrite it our interactions will naturally be disabled.
    fun setNextItemAllowOverlap() {
        val g = gImGui
        g.nextItemData.itemFlags /= ItemFlag.AllowOverlap
    }
}