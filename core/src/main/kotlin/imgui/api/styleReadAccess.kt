package imgui.api

import glm_.f
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.font.Font

// Style read access
// - Use the style editor (ShowStyleEditor() function) to interactively see what the colors are)
interface styleReadAccess {

    /** get current font
     *  ~GetFont    */
    val font: Font
        get() = g.font

    /** get current font size (= height in pixels) of current font with current scale applied
     *  ~GetFontSize    */
    val fontSize: Float
        get() = g.fontSize

    /** get UV coordinate for a while pixel, useful to draw custom shapes via the ImDrawList API
     *  ~GetFontTexUvWhitePixel */
    val fontTexUvWhitePixel: Vec2
        get() = g.drawListSharedData.texUvWhitePixel

    /** retrieve given style color with style alpha applied and optional extra alpha multiplier */
    fun getColorU32(idx: Col, alphaMul: Float = 1f): Int =
            getColorU32(idx.i, alphaMul)

    fun getColorU32(idx: Int, alphaMul: Float = 1f): Int {
        val c = Vec4(ImGui.style.colors[idx])
        c.w *= ImGui.style.alpha * alphaMul
        return c.u32
    }

    /** retrieve given color with style alpha applied   */
    fun getColorU32(col: Vec4): Int {
        val c = Vec4(col)
        c.w *= ImGui.style.alpha
        return c.u32
    }

    /** retrieve given color with style alpha applied    */
    fun getColorU32(col: Int): Int {
        val styleAlpha = ImGui.style.alpha
        if (styleAlpha >= 1f) return col
        var a = (col and COL32_A_MASK) ushr COL32_A_SHIFT
        a = (a * styleAlpha).i // We don't need to clamp 0..255 because Style.Alpha is in 0..1 range.
        return (col and COL32_A_MASK.inv()) or (a shl COL32_A_SHIFT)
    }

    fun getColorU32(r: Int, g: Int, b: Int, a: Int): Int {
        val c = Vec4(r.f / 255.0, g.f / 255.0, b.f / 255.0, a.f / 255.0)
        c.w *= ImGui.style.alpha
        return c.u32
    }

    /** retrieve style color as stored in ImGuiStyle structure. use to feed back into PushStyleColor(), otherwise use
     *  GetColorU32() to get style color + style alpha. */
    fun getStyleColorVec4(idx: Col): Vec4 = Vec4(ImGui.style.colors[idx])
}