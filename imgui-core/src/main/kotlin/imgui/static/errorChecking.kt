package imgui.static

import imgui.ImGui.end
import imgui.ImGui.io
import imgui.ImGui.mergedKeyModFlags
import imgui.KeyMod
import imgui.KeyModFlags
import imgui.api.g
import imgui.internal.classes.Window


//-----------------------------------------------------------------------------
// [SECTION] ERROR CHECKING
//-----------------------------------------------------------------------------

fun errorCheckEndFrame() {

    // Verify that io.KeyXXX fields haven't been tampered with. Key mods shoudl not be modified between NewFrame() and EndFrame()
    val expectedKeyModFlags = mergedKeyModFlags
    assert(io.keyMods == expectedKeyModFlags) { "Mismatching io.KeyCtrl/io.KeyShift/io.KeyAlt/io.KeySuper vs io.KeyMods" }

    // Report when there is a mismatch of Begin/BeginChild vs End/EndChild calls. Important: Remember that the Begin/BeginChild API requires you
    // to always call End/EndChild even if Begin/BeginChild returns false! (this is unfortunately inconsistent with most other Begin* API).
    if (g.currentWindowStack.size != 1)
        if (g.currentWindowStack.size > 1) {
            assert(g.currentWindowStack.size == 1) { "Mismatched Begin/BeginChild vs End/EndChild calls: did you forget to call End/EndChild?" }
            while (g.currentWindowStack.size > 1)
                end()
        } else
            assert(g.currentWindowStack.size == 1) { "Mismatched Begin/BeginChild vs End/EndChild calls: did you call End/EndChild too much?" }
}

/** Save and compare stack sizes on Begin()/End() to detect usage errors
 *  Begin() calls this with write=true
 *  End() calls this with write=false */
fun errorCheckBeginEndCompareStacksSize(window: Window, write: Boolean) {

    val p = window.dc.stackSizesBackup
    var i = 0

    // Window stacks
    // NOT checking: DC.ItemWidth, DC.AllowKeyboardFocus, DC.ButtonRepeat, DC.TextWrapPos (per window) to allow user to conveniently push once and not pop (they are cleared on Begin)
    run { val n = window.idStack.size; if (write) p[i] = n else assert(p[i] == n) { "PushID/PopID or TreeNode/TreePop Mismatch!" }; i++; }    // Too few or too many PopID()/TreePop()
    run { val n = window.dc.groupStack.size; if (write) p[i] = n else assert(p[i] == n) { "BeginGroup/EndGroup Mismatch!" }; i++; }    // Too few or too many EndGroup()

    // Global stacks
    // For color, style and font stacks there is an incentive to use Push/Begin/Pop/.../End patterns, so we relax our checks a little to allow them.
    run { val n = g.beginPopupStack.size; if (write) p[i] = n; else assert(p[i] == n) { "BeginMenu/EndMenu or BeginPopup/EndPopup Mismatch!" }; i++; }// Too few or too many EndMenu()/EndPopup()
    run { val n = g.colorModifiers.size; if (write) p[i] = n; else assert(p[i] >= n) { "PushStyleColor/PopStyleColor Mismatch!" }; i++; }    // Too few or too many PopStyleColor()
    run { val n = g.styleModifiers.size; if (write) p[i] = n; else assert(p[i] >= n) { "PushStyleVar/PopStyleVar Mismatch!" }; i++; }    // Too few or too many PopStyleVar()
    run { val n = g.fontStack.size; if (write) p[i] = n; else assert(p[i] >= n) { "PushFont/PopFont Mismatch!" }; i++; }    // Too few or too many PopFont()
    assert(i == window.dc.stackSizesBackup.size)
}