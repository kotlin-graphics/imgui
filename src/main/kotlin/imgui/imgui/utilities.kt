package imgui.imgui

import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.currentWindowRead
import imgui.ImGui.findRenderedTextEnd
import imgui.internal.Rect
import imgui.internal.saturate
import imgui.Context as g

/** was the last item hovered by mouse? */
fun isItemHovered() = currentWindowRead!!.dc.lastItemHoveredAndUsable

/** was the last item hovered by mouse? even if another item is active or window is blocked by popup while we are
 *  hovering this   */
val isItemHoveredRect get() = currentWindowRead!!.dc.lastItemHoveredAndUsable

/** was the last item active? (e.g. button being held, text field being edited- items that don't interact will always
 *  return false)   */
val isItemActive get() = if (g.activeId != 0) g.activeId == currentWindowRead!!.dc.lastItemId else false

/** was the last item clicked? (e.g. button/node just clicked on)   */
fun isItemClicked(mouseButton: Int = 0) = isMouseClicked(mouseButton) && isItemHovered()

/** was the last item visible? (aka not out of sight due to clipping/scrolling.)    */
val isItemVisible get() = with(currentWindowRead!!) { Rect(clipRect).overlaps(dc.lastItemRect) }

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

/** is current window hovered and hoverable (not blocked by a popup) (differentiate child windows from each others) */
val isWindowHovered get() = g.hoveredWindow == g.currentWindow && g.hoveredRootWindow!!.isContentHoverable

/** is current window focused   */
val isWindowFocused get() = g.focusedWindow == g.currentWindow

/** is current root window focused (root = top-most parent of a child, otherwise self)  */
val isRootWindowFocused get() = g.focusedWindow == g.currentWindow!!.rootWindow

/** is current root window or any of its child (including current window) focused   */
val isRootWindowOrAnyChildFocused get() = g.focusedWindow != null && g.focusedWindow!!.rootWindow == g.currentWindow!!.rootWindow

/** is current root window or any of its child (including current window) hovered and hoverable (not blocked by a popup)    */
val isRootWindowOrAnyChildHovered
    get() = g.hoveredRootWindow != null && (g.hoveredRootWindow === g.currentWindow!!.rootWindow) && g.hoveredRootWindow!!.isContentHoverable

/** test if rectangle (of given size, starting from cursor position) is visible / not clipped.  */
fun isRectVisible(size: Vec2) = with(currentWindowRead!!) { clipRect.overlaps(Rect(dc.cursorPos, dc.cursorPos + size)) }

/** test if rectangle (in screen space) is visible / not clipped. to perform coarse clipping on user's side.    */
fun isRectVisible(rectMin: Vec2, rectMax: Vec2) = currentWindowRead!!.clipRect.overlaps(Rect(rectMin, rectMax))

/** is given position hovering any active imgui window  */
fun isPosHoveringAnyWindow(pos: Vec2) = findHoveredWindow(pos, false) != null

val time get() = g.time

val frameCount get() = g.frameCount

//IMGUI_API const char*   GetStyleColName(ImGuiCol idx);
//IMGUI_API ImVec2        CalcItemRectClosestPoint(const ImVec2& pos, bool on_edge = false, float outward = +0.0f);   // utility to find the closest point the last item bounding rectangle edge. useful to visually link items

/** Calculate text size. Text can be multi-line. Optionally ignore text after a ## marker.
 *  CalcTextSize("") should return ImVec2(0.0f, GImGui->FontSize)   */
fun calcTextSize(text: String, hideTextAfterDoubleHash: Boolean) = calcTextSize(text, 0, hideTextAfterDoubleHash)

fun calcTextSize(text: String, textEnd: Int = text.length, hideTextAfterDoubleHash: Boolean = false, wrapWidth: Float = -1f): Vec2 {

    val textDisplayEnd =
            if (hideTextAfterDoubleHash)
                findRenderedTextEnd(text, textEnd)  // Hide anything after a '##' string
            else
                if (textEnd == 0) text.length else textEnd

    val font = g.font
    val fontSize = g.fontSize
    if (0 == textDisplayEnd)
        return Vec2(0f, fontSize)
    val textSize = font.calcTextSizeA(fontSize, Float.MAX_VALUE, wrapWidth, text, textDisplayEnd/*, NULL*/)

    // Cancel out character spacing for the last character of a line (it is baked into glyph->XAdvance field)
    val fontScale = fontSize / font.fontSize
    val characterSpacingX = 1f * fontScale
    if (textSize.x > 0f)
        textSize.x -= characterSpacingX
    textSize.x = (textSize.x + 0.95f).i.f

    return textSize
}

//IMGUI_API void          CalcListClipping(int items_count, float items_height, int* out_items_display_start, int* out_items_display_end);    // calculate coarse clipping for large list of evenly sized items. Prefer using the ImGuiListClipper higher-level helper if you can.


/** helper to create a child window / scrolling region that looks like a normal widget frame    */
fun beginChildFrame(id: Int, size: Vec2, extraFlags: Int = 0): Boolean {

    ImGui.pushStyleColor(Col.ChildWindowBg, Style.colors[Col.FrameBg])
    ImGui.pushStyleVar(StyleVar.ChildWindowRounding, Style.frameRounding)
    ImGui.pushStyleVar(StyleVar.WindowPadding, Style.framePadding)
    return ImGui.beginChild(id, size, g.currentWindow!!.flags has WindowFlags.ShowBorders,
            WindowFlags.NoMove or WindowFlags.AlwaysUseWindowPadding or extraFlags)
}

fun endChildFrame() {
    ImGui.endChild()
    ImGui.popStyleVar(2)
    ImGui.popStyleColor()
}

//IMGUI_API ImVec4        ColorConvertU32ToFloat4(ImU32 in);
fun colorConvertFloat4ToU32(color: Vec4): Int {
    var out = F32_TO_INT8_SAT(color.x) shl COL32_R_SHIFT
    out = out or (F32_TO_INT8_SAT(color.y) shl COL32_G_SHIFT)
    out = out or (F32_TO_INT8_SAT(color.z) shl COL32_B_SHIFT)
    return out or (F32_TO_INT8_SAT(color.w) shl COL32_A_SHIFT)
}

/** Convert rgb floats ([0-1],[0-1],[0-1]) to hsv floats ([0-1],[0-1],[0-1]), from Foley & van Dam p592
 *  Optimized http://lolengine.net/blog/2013/01/13/fast-rgb-to-hsv  */
fun colorConvertRGBtoHSV(rgb: FloatArray, hsv: FloatArray) {

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
}

/** Convert hsv floats ([0-1],[0-1],[0-1]) to rgb floats ([0-1],[0-1],[0-1]), from Foley & van Dam p593
 *  also http://en.wikipedia.org/wiki/HSL_and_HSV   */
fun colorConvertHSVtoRGB(hsv: FloatArray, rgb: FloatArray) {

    var (h, s, v) = hsv
    if (s == 0f) {
        // gray
        rgb[0] = v
        rgb[1] = v
        rgb[2] = v
        return
    }

    h = glm.mod(h, 1f) / (60f / 360f)
    val i = h.i
    val f = h - i.f
    val p = v * (1f - s)
    val q = v * (1f - s * f)
    val t = v * (1f - s * (1f - f))

    when (i) {
        0 -> {
            rgb[0] = v; rgb[1] = t; rgb[2] = p; }
        1 -> {
            rgb[0] = q; rgb[1] = v; rgb[2] = p; }
        2 -> {
            rgb[0] = p; rgb[1] = v; rgb[2] = t; }
        3 -> {
            rgb[0] = p; rgb[1] = q; rgb[2] = v; }
        4 -> {
            rgb[0] = t; rgb[1] = p; rgb[2] = v; }
        else -> {
            rgb[0] = v; rgb[1] = p; rgb[2] = q; }
    }
}

/** Unsaturated, for display purpose    */
fun F32_TO_INT8_UNBOUND(_val: Float) = (_val * 255f + if (_val >= 0) 0.5f else -0.5f).i

/** Saturated, always output 0..255 */
fun F32_TO_INT8_SAT(_val: Float) = (saturate(_val) * 255f + 0.5f).i