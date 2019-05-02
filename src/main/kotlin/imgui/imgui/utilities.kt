package imgui.imgui

import gli_.hasnt
import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.beginChild
import imgui.ImGui.currentWindowRead
import imgui.ImGui.endChild
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.isMouseClicked
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.style
import imgui.internal.*
import uno.kotlin.getValue
import uno.kotlin.setValue
import kotlin.reflect.KMutableProperty0
import imgui.FocusedFlag as Ff
import imgui.HoveredFlag as Hf
import imgui.WindowFlag as Wf

/** Item/Widgets Utilities
 *  - Most of the functions are referring to the last/previous item we submitted.
 *  - See Demo Window under "Widgets->Querying Status" for an interactive visualization of most of those functions. */
interface imgui_utilities {

    /** This is roughly matching the behavior of internal-facing ItemHoverable()
     *      - we allow hovering to be true when activeId==window.moveID, so that clicking on non-interactive items
     *          such as a text() item still returns true with isItemHovered()
     *      - this should work even for non-interactive items that have no ID, so we cannot use LastItemId  */
    fun isItemHovered(flags: Hf) = isItemHovered(flags.i)

    fun isItemHovered(flags: Int = Hf.None.i): Boolean {

        if (g.navDisableMouseHover && !g.navDisableHighlight)
            return isItemFocused

        val window = g.currentWindow!!
        return when {
            // Test for bounding box overlap, as updated as ItemAdd()
            window.dc.lastItemStatusFlags hasnt ItemStatusFlag.HoveredRect -> false
            else -> {
                assert(flags hasnt (Hf.RootWindow or Hf.ChildWindows)) { "Flags not supported by this function" }
                when {
                    /*  Test if we are hovering the right window (our window could be behind another window)
                        [2017/10/16] Reverted commit 344d48be3 and testing RootWindow instead. I believe it is correct to
                        NOT test for rootWindow but this leaves us unable to use isItemHovered after endChild() itself.
                        Until a solution is found I believe reverting to the test from 2017/09/27 is safe since this was
                        the test that has been running for a long while. */
                    // g.hoveredWindow !== window -> false
                    g.hoveredRootWindow !== window.rootWindow && flags hasnt Hf.AllowWhenOverlapped -> false
                    // Test if another item is active (e.g. being dragged)
                    flags hasnt Hf.AllowWhenBlockedByActiveItem && g.activeId != 0 && g.activeId != window.dc.lastItemId &&
                            !g.activeIdAllowOverlap && g.activeId != window.moveId -> false
                    // Test if interactions on this window are blocked by an active popup or modal
                    g.navDisableMouseHover || !window.isContentHoverable(flags) -> false
                    // Test if the item is disabled
                    window.dc.itemFlags has ItemFlag.Disabled && flags hasnt Hf.AllowWhenDisabled -> false
                    /*  Special handling for the dummy item after Begin() which represent the title bar or tab.
                        When the window is collapsed (SkipItems==true) that last item will never be overwritten
                        so we need to detect the case.  */
                    window.dc.lastItemId == window.moveId && window.writeAccessed -> false
                    else -> true
                }
            }
        }
    }

    /** Is the last item active? (e.g. button being held, text field being edited.
     *  This will continuously return true while holding mouse button on an item. Items that don't interact will always return false) */
    val isItemActive: Boolean
        get() = if (g.activeId != 0) g.activeId == g.currentWindow!!.dc.lastItemId else false

    /** Is the last item focused for keyboard/gamepad navigation?   */
    val isItemFocused: Boolean
        get() = g.navId != 0 && !g.navDisableHighlight && g.navId == g.currentWindow!!.dc.lastItemId

    /** Is the last item clicked? (e.g. button/node just clicked on) == IsMouseClicked(mouse_button) && IsItemHovered() */
    fun isItemClicked(mouseButton: Int = 0) = isMouseClicked(mouseButton) && isItemHovered(Hf.None)

    /** Is the last item visible? (items may be out of sight because of clipping/scrolling)    */
    val isItemVisible: Boolean
        get() = currentWindowRead!!.run { clipRect overlaps dc.lastItemRect }

    val isItemEdited: Boolean
        get () = currentWindowRead!!.run { dc.lastItemStatusFlags has ItemStatusFlag.Edited }

    /** was the last item just made active (item was previously inactive). */
    val isItemActivated: Boolean
        get() = when(g.activeId) {
            0 -> false
            else -> g.currentWindow!!.run { g.activeId == dc.lastItemId && g.activeIdPreviousFrame != dc.lastItemId }
        }

    /** Was the last item just made inactive (item was previously active).
     *  Useful for Undo/Redo patterns with widgets that requires continuous editing. */
    val isItemDeactivated: Boolean
        get() = g.currentWindow!!.dc.let { g.activeIdPreviousFrame == it.lastItemId && g.activeIdPreviousFrame != 0 && g.activeId != it.lastItemId }

    /** Was the last item just made inactive and made a value change when it was active? (e.g. Slider/Drag moved).
     *  Useful for Undo/Redo patterns with widgets that requires continuous editing.
     *  Note that you may get false positives (some widgets such as Combo()/ListBox()/Selectable() will return true even when clicking an already selected item). */
    val isItemDeactivatedAfterEdit: Boolean
        get() = isItemDeactivated && (g.activeIdPreviousFrameHasBeenEdited || (g.activeId == 0 && g.activeIdHasBeenEdited))

    /** is any item hovered? */
    val isAnyItemHovered: Boolean
        get() = g.hoveredId != 0 || g.hoveredIdPreviousFrame != 0

    /** is any item active? */
    val isAnyItemActive: Boolean
        get() = g.activeId != 0

    /** is any item focused? */
    val isAnyItemFocused: Boolean
        get() = g.navId != 0 && !g.navDisableHighlight

    /** get upper-left bounding rectangle of the last item (screen space)  */
    val itemRectMin
        get() = currentWindowRead!!.dc.lastItemRect.min

    /** get lower-right bounding rectangle of the last item (screen space)  */
    val itemRectMax
        get() = currentWindowRead!!.dc.lastItemRect.max

    /** get size of last item  */
    val itemRectSize
        get() = currentWindowRead!!.dc.lastItemRect.size

    /** allow last item to be overlapped by a subsequent item. sometimes useful with invisible buttons, selectables, etc.
     *  to catch unused area.   */
    fun setItemAllowOverlap() {
        if (g.hoveredId == g.currentWindow!!.dc.lastItemId)
            g.hoveredIdAllowOverlap = true
        if (g.activeId == g.currentWindow!!.dc.lastItemId)
            g.activeIdAllowOverlap = true
    }


    // Miscellaneous Utilities

    /** test if rectangle (of given size, starting from cursor position) is visible / not clipped.  */
    fun isRectVisible(size: Vec2) = with(currentWindowRead!!) { clipRect overlaps Rect(dc.cursorPos, dc.cursorPos + size) }

    /** test if rectangle (in screen space) is visible / not clipped. to perform coarse clipping on user's side.    */
    fun isRectVisible(rectMin: Vec2, rectMax: Vec2) = currentWindowRead!!.clipRect overlaps Rect(rectMin, rectMax)

    val time: Double
        get() = g.time

    val frameCount: Int
        get() = g.frameCount

    /** This seemingly unnecessary wrapper simplifies compatibility between the 'master' and 'docking' branches. */
    fun getForegroundDrawList(window: Window?): DrawList = g.foregroundDrawList

    /** this draw list will be the first rendering one. Useful to quickly draw shapes/text behind dear imgui contents. */
    val backgroundDrawList: DrawList
        get() = g.backgroundDrawList

    /** this draw list will be the last rendered one. Useful to quickly draw shapes/text over dear imgui contents.   */
    val foregroundDrawList: DrawList
        get() = g.foregroundDrawList

    /** you may use this when creating your own ImDrawList instances. */
    val drawListSharedData: DrawListSharedData
        get() = g.drawListSharedData

    /** Useless on JVM with Enums */
    //IMGUI_API const char*   GetStyleColorName(ImGuiCol idx);

    //IMGUI_API void          SetStateStorage(ImGuiStorage* tree);                                // replace tree state storage with our own (if you want to manipulate it yourself, typically clear subsection of it)

    //IMGUI_API ImGuiStorage* GetStateStorage();

    /** Calculate text size. Text can be multi-line. Optionally ignore text after a ## marker.
     *  CalcTextSize("") should return ImVec2(0.0f, GImGui->FontSize)   */
    fun calcTextSize(text: String, hideTextAfterDoubleHash: Boolean) = calcTextSize(text, 0, hideTextAfterDoubleHash)

    fun calcTextSize(text: String, textEnd: Int = text.length, hideTextAfterDoubleHash: Boolean = false, wrapWidth: Float = -1f): Vec2 {

        val textDisplayEnd =
                when {
                    hideTextAfterDoubleHash -> findRenderedTextEnd(text, textEnd)  // Hide anything after a '##' string
                    textEnd == 0 -> text.length
                    else -> textEnd
                }

        val font = g.font
        val fontSize = g.fontSize
        if (0 == textDisplayEnd)
            return Vec2(0f, fontSize)
        val textSize = font.calcTextSizeA(fontSize, Float.MAX_VALUE, wrapWidth, text, textDisplayEnd)

        // Round
        textSize.x = (textSize.x + 0.95f).i.f

        return textSize
    }

    /** calculate coarse clipping for large list of evenly sized items. Prefer using the ImGuiListClipper higher-level
     *  helper if you can.
     *  Helper to calculate coarse clipping of large list of evenly sized items.
     *  NB: Prefer using the ImGuiListClipper higher-level helper if you can! Read comments and instructions there on
     *  how those use this sort of pattern.
     *  NB: 'items_count' is only used to clamp the result, if you don't know your count you can use INT_MAX    */
    fun calcListClipping(itemsCount: Int, itemsHeight: Float): IntRange {
        val window = g.currentWindow!!
        return when {
            g.logEnabled -> 0..itemsCount // If logging is active, do not perform any clipping
            window.skipItems -> 0..0
            else -> {
                // We create the union of the ClipRect and the NavScoringRect which at worst should be 1 page away from ClipRect
                val unclippedRect = window.clipRect
                if (g.navMoveRequest)
                    unclippedRect add g.navScoringRectScreen

                val pos = window.dc.cursorPos
                var start = ((unclippedRect.min.y - pos.y) / itemsHeight).i
                var end = ((unclippedRect.max.y - pos.y) / itemsHeight).i

                // When performing a navigation request, ensure we have one item extra in the direction we are moving to
                if (g.navMoveRequest && g.navMoveDir == Dir.Up)
                    start--
                if (g.navMoveRequest && g.navMoveDir == Dir.Down)
                    end++
                start = glm.clamp(start, 0, itemsCount)
                end = glm.clamp(end + 1, start, itemsCount)
                start..end
            }
        }
    }


    /** helper to create a child window / scrolling region that looks like a normal widget frame    */
    fun beginChildFrame(id: ID, size: Vec2, extraFlags: WindowFlags = 0): Boolean {
        pushStyleColor(Col.ChildBg, style.colors[Col.FrameBg])
        pushStyleVar(StyleVar.ChildRounding, style.frameRounding)
        pushStyleVar(StyleVar.ChildBorderSize, style.frameBorderSize)
        pushStyleVar(StyleVar.WindowPadding, style.framePadding)
        val flags = Wf.NoMove or Wf.AlwaysUseWindowPadding or extraFlags
        return beginChild(id, size, true, Wf.NoMove or Wf.AlwaysUseWindowPadding or extraFlags).also {
            popStyleVar(3)
            popStyleColor()
        }
    }

    /** Always call EndChildFrame() regardless of BeginChildFrame() return values (which indicates a collapsed/clipped window)  */
    fun endChildFrame() = endChild()

    val Int.vec4: Vec4
        get() {
            val s = 1f / 255f
            return Vec4(
                    ((this ushr COL32_R_SHIFT) and 0xFF) * s,
                    ((this ushr COL32_G_SHIFT) and 0xFF) * s,
                    ((this ushr COL32_B_SHIFT) and 0xFF) * s,
                    ((this ushr COL32_A_SHIFT) and 0xFF) * s)
        }

    val Vec4.u32: Int
        get () {
            var out = F32_TO_INT8_SAT(x) shl COL32_R_SHIFT
            out = out or (F32_TO_INT8_SAT(y) shl COL32_G_SHIFT)
            out = out or (F32_TO_INT8_SAT(z) shl COL32_B_SHIFT)
            return out or (F32_TO_INT8_SAT(w) shl COL32_A_SHIFT)
        }


    // Color Utilities


    /** Convert rgb floats ([0-1],[0-1],[0-1]) to hsv floats ([0-1],[0-1],[0-1]), from Foley & van Dam p592
     *  Optimized http://lolengine.net/blog/2013/01/13/fast-rgb-to-hsv  */
    fun colorConvertRGBtoHSV(rgb: FloatArray, hsv: FloatArray = FloatArray(3)): FloatArray =
            colorConvertRGBtoHSV(rgb[0], rgb[1], rgb[2], hsv)

    fun colorConvertRGBtoHSV(r_: Float, g_: Float, b_: Float, hsv: FloatArray = FloatArray(3)): FloatArray {

        var k = 0f
        var r = r_
        var g = g_
        var b = b_
        if (g < b) {
            val tmp = g; g = b; b = tmp
            k = -1f
        }
        if (r < g) {
            val tmp = r; r = g; g = tmp
            k = -2f / 6f - k
        }

        val chroma = r - (if (g < b) g else b)
        hsv[0] = glm.abs(k + (g - b) / (6f * chroma + 1e-20f))
        hsv[1] = chroma / (r + 1e-20f)
        hsv[2] = r
        return hsv
    }

    fun FloatArray.rgbToHSV() = colorConvertRGBtoHSV(this, this)

    /** Convert hsv floats ([0-1],[0-1],[0-1]) to rgb floats ([0-1],[0-1],[0-1]), from Foley & van Dam p593
     *  also http://en.wikipedia.org/wiki/HSL_and_HSV   */
    fun colorConvertHSVtoRGB(hsv: FloatArray, rgb: FloatArray = FloatArray(3)) = colorConvertHSVtoRGB(hsv[0], hsv[1], hsv[2], rgb)

    fun colorConvertHSVtoRGB(h: Float, s: Float, v: Float, rgb: FloatArray = FloatArray(3)): FloatArray {
        colorConvertHSVtoRGB(h, s, v, ::f0, ::f1, ::f2)
        return rgb.apply { set(0, f0); set(1, f1); set(2, f2) }
    }

    fun colorConvertHSVtoRGB(h_: Float, s: Float, v: Float, rPtr: KMutableProperty0<Float>, gPtr: KMutableProperty0<Float>,
                             bPtr: KMutableProperty0<Float>) {

        var r by rPtr
        var g by gPtr
        var b by bPtr
        if (s == 0f) {
            // gray
            r = v
            g = v
            b = v
        }

        val h = glm.mod(h_, 1f) / (60f / 360f)
        val i = h.i
        val f = h - i.f
        val p = v * (1f - s)
        val q = v * (1f - s * f)
        val t = v * (1f - s * (1f - f))

        when (i) {
            0 -> {
                r = v; g = t; b = p; }
            1 -> {
                r = q; g = v; b = p; }
            2 -> {
                r = p; g = v; b = t; }
            3 -> {
                r = p; g = q; b = v; }
            4 -> {
                r = t; g = p; b = v; }
            else -> {
                r = v; g = p; b = q; }
        }
    }

    fun colorConvertHSVtoRGB(col: Vec4) {

        val h_ = col.x
        val s = col.y
        val v = col.z
        var r: Float
        var g: Float
        var b: Float
        if (s == 0f) {
            // gray
            r = v
            g = v
            b = v
        }

        val h = glm.mod(h_, 1f) / (60f / 360f)
        val i = h.i
        val f = h - i.f
        val p = v * (1f - s)
        val q = v * (1f - s * f)
        val t = v * (1f - s * (1f - f))

        when (i) {
            0 -> {
                r = v; g = t; b = p; }
            1 -> {
                r = q; g = v; b = p; }
            2 -> {
                r = p; g = v; b = t; }
            3 -> {
                r = p; g = q; b = v; }
            4 -> {
                r = t; g = p; b = v; }
            else -> {
                r = v; g = p; b = q; }
        }
        col.x = r
        col.y = g
        col.z = b
    }

    fun FloatArray.hsvToRGB() = colorConvertHSVtoRGB(this, this)

    /** Unsaturated, for display purpose    */
    fun F32_TO_INT8_UNBOUND(_val: Float) = (_val * 255f + if (_val >= 0) 0.5f else -0.5f).i

    /** Saturated, always output 0..255 */
    fun F32_TO_INT8_SAT(_val: Float) = (saturate(_val) * 255f + 0.5f).i

    companion object {
        var f0 = 0f
        var f1 = 0f
        var f2 = 0f
    }
}