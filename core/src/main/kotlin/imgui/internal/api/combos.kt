package imgui.internal.api

import glm_.func.common.max
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.calcNextAutoFitSize
import imgui.ImGui.popClipRect
import imgui.ImGui.popupAllowedExtentRect
import imgui.ImGui.pushClipRect
import imgui.api.g
import imgui.api.widgetsComboBox
import imgui.internal.classes.Rect
import imgui.internal.sections.ItemStatusFlag
import imgui.internal.sections.LayoutType
import imgui.internal.sections.NextWindowDataFlag
import imgui.internal.sections.PopupPositionPolicy


// Combos
internal interface combos {

    fun beginComboPopup(popupId: ID, bb: Rect, flags_: ComboFlags): Boolean {

        var flags = flags_

        if (!ImGui.isPopupOpen(popupId)) {
            g.nextWindowData.clearFlags()
            return false
        }

        // Set popup size
        val w = bb.width
        if (g.nextWindowData.flags has NextWindowDataFlag.HasSizeConstraint)
            g.nextWindowData.sizeConstraintRect.min.x = g.nextWindowData.sizeConstraintRect.min.x max w
        else {
            if (flags hasnt ComboFlag.HeightMask)
                flags = flags or ComboFlag.HeightRegular
            assert((flags and ComboFlag.HeightMask).isPowerOfTwo) { "Only one" }
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
                it.autoPosLastDirection = if (flags has ComboFlag.PopupAlignLeft) Dir.Left else Dir.Down // Left = "Below, Toward Left", Down = "Below, Toward Right (default)"
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

    /** Call directly after the BeginCombo/EndCombo block. The preview is designed to only host non-interactive elements
     *  (Experimental, see GitHub issues: #1658, #4168) */
    fun beginComboPreview(): Boolean {
        val window = g.currentWindow!!
        val previewData = g.comboPreviewData

        if (window.skipItems || g.lastItemData.statusFlags hasnt ItemStatusFlag.Visible)
            return false
        assert(g.lastItemData.rect.min.x == previewData.previewRect.min.x && g.lastItemData.rect.min.y == previewData.previewRect.min.y) { "Didn't call after BeginCombo/EndCombo block or forgot to pass ImGuiComboFlags_CustomPreview flag?" }
        if (!window.clipRect.contains(previewData.previewRect)) // Narrower test (optional)
            return false

        // FIXME: This could be contained in a PushWorkRect() api
        previewData.backupCursorPos put window.dc.cursorPos
        previewData.backupCursorMaxPos put window.dc.cursorMaxPos
        previewData.backupCursorPosPrevLine put window.dc.cursorPosPrevLine
        previewData.backupPrevLineTextBaseOffset = window.dc.prevLineTextBaseOffset
        previewData.backupLayout = window.dc.layoutType
        window.dc.cursorPos = previewData.previewRect.min + g.style.framePadding
        window.dc.cursorMaxPos put window.dc.cursorPos
        window.dc.layoutType = LayoutType.Horizontal
        window.dc.isSameLine = false
        pushClipRect(previewData.previewRect.min, previewData.previewRect.max, true)

        return true
    }

    fun endComboPreview() {
        val window = g.currentWindow!!
        val previewData = g.comboPreviewData

        // FIXME: Using CursorMaxPos approximation instead of correct AABB which we will store in ImDrawCmd in the future
        val drawList = window.drawList
        if (window.dc.cursorMaxPos.x < previewData.previewRect.max.x && window.dc.cursorMaxPos.y < previewData.previewRect.max.y)
        if (drawList.cmdBuffer.size > 1) { // Unlikely case that the PushClipRect() didn't create a command
            val rect = drawList.cmdBuffer[drawList.cmdBuffer.size-2].clipRect
            drawList.cmdBuffer[drawList.cmdBuffer.lastIndex].clipRect put rect
            drawList._cmdHeader.clipRect put rect
            drawList._tryMergeDrawCmds()
        }
        popClipRect()
        window.dc.cursorPos put previewData.backupCursorPos
        window.dc.cursorMaxPos = window.dc.cursorMaxPos max previewData.backupCursorMaxPos
        window.dc.cursorPosPrevLine put previewData.backupCursorPosPrevLine
        window.dc.prevLineTextBaseOffset = previewData.backupPrevLineTextBaseOffset
        window.dc.layoutType = previewData.backupLayout
        window.dc.isSameLine = false
        previewData.previewRect put Rect()
    }

}