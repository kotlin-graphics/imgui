package imgui.internal.api

import glm_.func.common.max
import glm_.vec2.Vec2
import imgui.*
import imgui.api.g
import imgui.api.widgetsComboBox
import imgui.internal.classes.Rect
import imgui.internal.isPowerOfTwo
import imgui.internal.sections.NextWindowDataFlag
import imgui.internal.sections.PopupPositionPolicy
import imgui.internal.sections.has


// Combos
internal interface combos {

    fun beginComboPopup(popupId: ID, bb: Rect, flags_: ComboFlags): Boolean {

        var flags = flags_

        if (!ImGui.isPopupOpen(popupId, PopupFlag.None.i)) {
            g.nextWindowData.clearFlags()
            return false
        }

        // Set popup size
        val w = bb.width
        if (g.nextWindowData.flags has NextWindowDataFlag.HasSizeConstraint)
            g.nextWindowData.sizeConstraintRect.min.x = g.nextWindowData.sizeConstraintRect.min.x max w
        else {
            if (flags hasnt ComboFlag.HeightMask_)
                flags = flags or ComboFlag.HeightRegular
            assert((flags and ComboFlag.HeightMask_).isPowerOfTwo) { "Only one" }
            val popupMaxHeightInItems = when {
                flags has ComboFlag.HeightRegular -> 8
                flags has ComboFlag.HeightSmall -> 4
                flags has ComboFlag.HeightLarge -> 20
                else -> -1
            }
            ImGui.setNextWindowSizeConstraints(Vec2(w, 0f), Vec2(Float.MAX_VALUE, widgetsComboBox.calcMaxPopupHeightFromItemCount(popupMaxHeightInItems)))
        }

        // This is essentially a specialized version of BeginPopupEx()
        val name = "##Combo_%02d".format(g.beginPopupStack.size) // Recycle windows based on depth

        // Set position given a custom constraint (peak into expected window size so we can position it)
        // FIXME: This might be easier to express with an hypothetical SetNextWindowPosConstraints() function?
        // FIXME: This might be moved to Begin() or at least around the same spot where Tooltips and other Popups are calling FindBestWindowPosForPopupEx()?
        ImGui.findWindowByName(name)?.let {
            if (it.wasActive) {
                // Always override 'AutoPosLastDirection' to not leave a chance for a past value to affect us.
                val sizeExpected = it.calcNextAutoFitSize()
                it.autoPosLastDirection = if(flags has ComboFlag.PopupAlignLeft) Dir.Left else Dir.Down // Left = "Below, Toward Left", Down = "Below, Toward Right (default)"
                val rOuter = it.popupAllowedExtentRect
                val pos = ImGui.findBestWindowPosForPopupEx(bb.bl, sizeExpected, it::autoPosLastDirection, rOuter, bb, PopupPositionPolicy.ComboBox)
                ImGui.setNextWindowPos(pos)
            }
        }

        // We don't use BeginPopupEx() solely because we have a custom name string, which we could make an argument to BeginPopupEx()
        val windowFlags: WindowFlags = WindowFlag.AlwaysAutoResize or WindowFlag._Popup or WindowFlag.NoTitleBar or WindowFlag.NoResize or WindowFlag.NoSavedSettings or WindowFlag.NoMove

        ImGui.pushStyleVar(StyleVar.WindowPadding, Vec2(ImGui.style.framePadding.x, ImGui.style.windowPadding.y)) // Horizontally align ourselves with the framed text
        val ret = ImGui.begin(name, null, windowFlags)
        ImGui.popStyleVar()
        if (!ret) {
            ImGui.endPopup()
            assert(false) { "This should never happen as we tested for IsPopupOpen() above" }
            return false
        }
        return true
    }
}