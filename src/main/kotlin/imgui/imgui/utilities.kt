package imgui.imgui

import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.Context.style
import imgui.ImGui.beginChild
import imgui.ImGui.currentWindowRead
import imgui.ImGui.endChild
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.isMouseClicked
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.internal.Rect
import imgui.internal.saturate
import kotlin.reflect.KMutableProperty0
import imgui.Context as g
import imgui.HoveredFlags as Hf
import imgui.WindowFlags as Wf


interface imgui_utilities {

    /** This is roughly matching the behavior of internal-facing ItemHoverable()
     *      - we allow hovering to be true when activeId==window.moveID, so that clicking on non-interactive items
     *          such as a text() item still returns true with isItemHovered()
     *      - this should work even for non-interactive items that have no ID, so we cannot use LastItemId  */
    fun isItemHovered(flags: Int = Hf.Default.i): Boolean {
        val window = g.currentWindow!!
        return when {
            !window.dc.lastItemRectHoveredRect -> false
        /*  [2017/10/16] Reverted commit 344d48be3 and testing RootWindow instead. I believe it is correct to NOT
            test for rootWindow but this leaves us unable to use isItemHovered after endChild() itself.
            Until a solution is found I believe reverting to the test from 2017/09/27 is safe since this was the test
            that has been running for a long while. */
        // g.hoveredWindow !== window -> false
            g.hoveredRootWindow !== window.rootWindow && flags hasnt Hf.AllowWhenOverlapped -> false
            flags hasnt Hf.AllowWhenBlockedByActiveItem && g.activeId != 0 && g.activeId != window.dc.lastItemId &&
                    !g.activeIdAllowOverlap && g.activeId != window.moveId -> false
            !window.isContentHoverable(flags) -> false
            window.dc.itemFlags has ItemFlags.Disabled -> false
            else -> true
        }
    }

    /** Is the last item active? (e.g. button being held, text field being edited- items that don't interact will always
     *  return false)   */
    val isItemActive get() = if (g.activeId != 0) g.activeId == g.currentWindow!!.dc.lastItemId else false

    /** Is the last item clicked? (e.g. button/node just clicked on)   */
    fun isItemClicked(mouseButton: Int = 0) = isMouseClicked(mouseButton) && isItemHovered(Hf.Default.i)

    /** Is the last item visible? (aka not out of sight due to clipping/scrolling.)    */
    val isItemVisible get() = with(currentWindowRead!!) { clipRect overlaps dc.lastItemRect }

    val isAnyItemHovered get() = g.hoveredId != 0 || g.hoveredIdPreviousFrame != 0

    val isAnyItemActive get() = g.activeId != 0

    /** get bounding rect of last item in screen space  */
    val itemRectMin get() = currentWindowRead!!.dc.lastItemRect.min

    /** get bounding rect of last item in screen space  */
    val itemRectMax get() = currentWindowRead!!.dc.lastItemRect.max

    /** get bounding rect of last item in screen space  */
    val itemRectSize get() = currentWindowRead!!.dc.lastItemRect.size

    /** allow last item to be overlapped by a subsequent item. sometimes useful with invisible buttons, selectables, etc.
     *  to catch unused area.   */
    fun setItemAllowOverlap() {
        if (g.hoveredId == g.currentWindow!!.dc.lastItemId)
            g.hoveredIdAllowOverlap = true
        if (g.activeId == g.currentWindow!!.dc.lastItemId)
            g.activeIdAllowOverlap = true
    }

    /** is current window focused   */
    val isWindowFocused get() = g.navWindow === g.currentWindow

    /** is current window hovered and hoverable (not blocked by a popup) (differentiate child windows from each others) */
    fun isWindowHovered(flags: Int = Hf.Default.i): Boolean {
        assert(flags hasnt Hf.AllowWhenOverlapped)  // Flags not supported by this function
        return when {
            g.hoveredWindow !== g.currentWindow -> false
            !g.hoveredRootWindow!!.isContentHoverable(flags) -> false
            flags hasnt Hf.AllowWhenBlockedByActiveItem && g.activeId != 0 && g.activeIdWindow !== g.currentWindow -> false
            else -> true
        }
    }

    /** is current root window focused (root = top-most parent of a child, otherwise self)  */
    val isRootWindowFocused get() = g.navWindow === g.currentWindow!!.rootWindow

    /** is current root window or any of its child (including current window) focused   */
    val isRootWindowOrAnyChildFocused get() = g.navWindow != null && g.navWindow!!.rootWindow === g.currentWindow!!.rootWindow

    /** is current root window or any of its child (including current window) hovered and hoverable (not blocked by a popup)    */
    fun isRootWindowOrAnyChildHovered(flags: Int = Hf.Default.i): Boolean {
        assert(flags hasnt Hf.AllowWhenOverlapped)  // Flags not supported by this function
        return when {
            g.hoveredRootWindow != null || g.hoveredRootWindow != g.currentWindow!!.rootWindow -> false
            !g.hoveredRootWindow!!.isContentHoverable(flags) -> false
            flags hasnt Hf.AllowWhenBlockedByActiveItem && g.activeId != 0 && g.activeIdWindow != g.currentWindow -> false
            else -> true
        }
    }

    /** test if rectangle (of given size, starting from cursor position) is visible / not clipped.  */
    fun isRectVisible(size: Vec2) = with(currentWindowRead!!) { clipRect overlaps Rect(dc.cursorPos, dc.cursorPos + size) }

    /** test if rectangle (in screen space) is visible / not clipped. to perform coarse clipping on user's side.    */
    fun isRectVisible(rectMin: Vec2, rectMax: Vec2) = currentWindowRead!!.clipRect overlaps Rect(rectMin, rectMax)

    val time get() = g.time

    val frameCount get() = g.frameCount

//IMGUI_API const char*   GetStyleColorName(ImGuiCol idx);

    /** Utility to find the closest point the last item bounding rectangle edge. useful to visually link items  */
    fun calcItemRectClosestPoint(pos: Vec2, onEdge: Boolean = false, outward: Float = 0f): Vec2 {
        val rect = Rect(currentWindowRead!!.dc.lastItemRect)
        rect.expand(outward)
        return rect.getClosestPoint(pos, onEdge)
    }

    /** Calculate text size. Text can be multi-line. Optionally ignore text after a ## marker.
     *  CalcTextSize("") should return ImVec2(0.0f, GImGui->FontSize)   */
    fun calcTextSize(text: String, hideTextAfterDoubleHash: Boolean) = calcTextSize(text, 0, hideTextAfterDoubleHash)

    fun calcTextSize(text: String, textEnd: Int = text.length, hideTextAfterDoubleHash: Boolean = false, wrapWidth: Float = -1f): Vec2 {

        val textDisplayEnd =
                if (hideTextAfterDoubleHash)
                    findRenderedTextEnd(text, textEnd)  // Hide anything after a '##' string
                else
                    if (textEnd == 0) text.length else textEnd // TODO check if right

        val font = g.font
        val fontSize = g.fontSize
        if (0 == textDisplayEnd)
            return Vec2(0f, fontSize)
        val textSize = font.calcTextSizeA(fontSize, Float.MAX_VALUE, wrapWidth, text, textDisplayEnd)

        // Cancel out character spacing for the last character of a line (it is baked into glyph->AdvanceX field)
        val fontScale = fontSize / font.fontSize
        val characterSpacingX = 1f * fontScale
        if (textSize.x > 0f) textSize.x -= characterSpacingX
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
                val pos = window.dc.cursorPos
                var start = ((window.clipRect.min.y - pos.y) / itemsHeight).i
                var end = ((window.clipRect.max.y - pos.y) / itemsHeight).i
                start = glm.clamp(start, 0, itemsCount)
                end = glm.clamp(end + 1, start, itemsCount)
                start..end
            }
        }
    }


    /** helper to create a child window / scrolling region that looks like a normal widget frame    */
    fun beginChildFrame(id: Int, size: Vec2, extraFlags: Int = 0): Boolean {
        pushStyleColor(Col.ChildWindowBg, style.colors[Col.FrameBg])
        pushStyleVar(StyleVar.ChildWindowRounding, style.frameRounding)
        pushStyleVar(StyleVar.WindowPadding, style.framePadding)
        val flags = Wf.NoMove or Wf.AlwaysUseWindowPadding or extraFlags
        return beginChild(id, size, g.currentWindow!!.flags has Wf.ShowBorders, flags)
    }

    fun endChildFrame() {
        endChild()
        popStyleVar(2)
        popStyleColor()
    }

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

    /** Convert rgb floats ([0-1],[0-1],[0-1]) to hsv floats ([0-1],[0-1],[0-1]), from Foley & van Dam p592
     *  Optimized http://lolengine.net/blog/2013/01/13/fast-rgb-to-hsv  */
    fun colorConvertRGBtoHSV(rgb: FloatArray, hsv: FloatArray = FloatArray(3)): FloatArray {

        var k = 0f
        var (r, g, b) = rgb
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

    fun colorConvertHSVtoRGB(h: Float, s: Float, v: Float, r: KMutableProperty0<Float>, g: KMutableProperty0<Float>,
                             b: KMutableProperty0<Float>) {

        if (s == 0f) {
            // gray
            r.set(v)
            g.set(v)
            b.set(v)
        }

        val h = glm.mod(h, 1f) / (60f / 360f)
        val i = h.i
        val f = h - i.f
        val p = v * (1f - s)
        val q = v * (1f - s * f)
        val t = v * (1f - s * (1f - f))

        when (i) {
            0 -> {
                r.set(v); g.set(t); b.set(p); }
            1 -> {
                r.set(q); g.set(v); b.set(p); }
            2 -> {
                r.set(p); g.set(v); b.set(t); }
            3 -> {
                r.set(p); g.set(q); b.set(v); }
            4 -> {
                r.set(t); g.set(p); b.set(v); }
            else -> {
                r.set(v); g.set(p); b.set(q); }
        }
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