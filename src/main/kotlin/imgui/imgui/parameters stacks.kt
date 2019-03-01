package imgui.imgui

import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.currentWindow
import imgui.ImGui.currentWindowRead
import imgui.ImGui.defaultFont
import imgui.ImGui.popItemFlag
import imgui.ImGui.pushItemFlag
import imgui.ImGui.style
import imgui.ImGui.u32
import imgui.ImGui.vec4
import imgui.internal.ColorMod
import imgui.internal.StyleMod
import imgui.internal.ItemFlag as If


interface imgui_parametersStacks {

// Parameters stacks (shared)

    /** use NULL as a shortcut to push default font */
    fun pushFont(font: Font = defaultFont) {
        font.setCurrent()
        g.fontStack.push(font)
        g.currentWindow!!.drawList.pushTextureId(font.containerAtlas.texId)
    }

    fun popFont() {
        g.currentWindow!!.drawList.popTextureId()
        g.fontStack.pop()
        (g.fontStack.lastOrNull() ?: defaultFont).setCurrent()
    }

    /** FIXME: This may incur a round-trip (if the end user got their data from a float4) but eventually we aim to store
     *  the in-flight colors as ImU32   */
    fun pushStyleColor(idx: Col, col: Int) {
        val backup = ColorMod(idx, style.colors[idx])
        g.colorModifiers.push(backup)
        g.style.colors[idx] = col.vec4
    }

    fun pushStyleColor(idx: Col, col: Vec4) {
        val backup = ColorMod(idx, style.colors[idx])
        g.colorModifiers.push(backup)
        style.colors[idx] = col
    }

    fun popStyleColor(count: Int = 1) = repeat(count) {
        val backup = g.colorModifiers.pop()
        style.colors[backup.col] put backup.backupValue
    }

    /** It'll throw error if wrong correspondence between idx and value type
     *  GStyleVarInfo */
    fun pushStyleVar(idx: StyleVar, value: Any) {
        g.styleModifiers.push(StyleMod(idx).also {
            when (idx) {
                StyleVar.Alpha -> {
                    it.floats[0] = style.alpha
                    style.alpha = value as Float
                }
                StyleVar.WindowPadding -> {
                    style.windowPadding to it.floats
                    style.windowPadding put (value as Vec2)
                }
                StyleVar.WindowRounding -> {
                    it.floats[0] = style.windowRounding
                    style.windowRounding = value as Float
                }
                StyleVar.WindowBorderSize -> {
                    it.floats[0] = style.windowBorderSize
                    style.windowBorderSize = value as Float
                }
                StyleVar.WindowMinSize -> {
                    style.windowMinSize to it.ints
                    style.windowMinSize put (value as Vec2i)
                }
                StyleVar.WindowTitleAlign -> {
                    style.windowTitleAlign to it.floats
                    style.windowTitleAlign put (value as Vec2)
                }
                StyleVar.ChildRounding -> {
                    it.floats[0] = style.childRounding
                    style.childRounding = value as Float
                }
                StyleVar.ChildBorderSize -> {
                    it.floats[0] = style.childBorderSize
                    style.childBorderSize = value as Float
                }
                StyleVar.PopupRounding -> {
                    it.floats[0] = style.popupRounding
                    style.popupRounding = value as Float
                }
                StyleVar.PopupBorderSize -> {
                    it.floats[0] = style.popupBorderSize
                    style.popupBorderSize = value as Float
                }
                StyleVar.FramePadding -> {
                    style.framePadding to it.floats
                    style.framePadding put (value as Vec2)
                }
                StyleVar.FrameRounding -> {
                    it.floats[0] = style.frameRounding
                    style.frameRounding = value as Float
                }
                StyleVar.FrameBorderSize -> {
                    it.floats[0] = style.frameBorderSize
                    style.frameBorderSize = value as Float
                }
                StyleVar.ItemSpacing -> {
                    style.itemSpacing to it.floats
                    style.itemSpacing put (value as Vec2)
                }
                StyleVar.ItemInnerSpacing -> {
                    style.itemInnerSpacing to it.floats
                    style.itemInnerSpacing put (value as Vec2)
                }
                StyleVar.IndentSpacing -> {
                    it.floats[0] = style.indentSpacing
                    style.indentSpacing = value as Float
                }
                StyleVar.ScrollbarSize -> {
                    it.floats[0] = style.scrollbarSize
                    style.scrollbarSize = value as Float
                }
                StyleVar.ScrollbarRounding -> {
                    it.floats[0] = style.scrollbarRounding
                    style.scrollbarRounding = value as Float
                }
                StyleVar.GrabMinSize -> {
                    it.floats[0] = style.grabMinSize
                    style.grabMinSize = value as Float
                }
                StyleVar.GrabRounding -> {
                    it.floats[0] = style.grabRounding
                    style.grabRounding = value as Float
                }
                StyleVar.TabRounding -> {
                    it.floats[0] = style.tabRounding
                    style.tabRounding = value as Float
                }
                StyleVar.ButtonTextAlign -> {
                    style.buttonTextAlign to it.floats
                    style.buttonTextAlign put (value as Vec2)
                }
                StyleVar.SelectableTextAlign -> {
                    style.selectableTextAlign to it.floats
                    style.selectableTextAlign put (value as Vec2)
                }
            }
        })
    }

    fun popStyleVar(count: Int = 1) = repeat(count) {
        val backup = g.styleModifiers.pop()
        when (backup.idx) {
            StyleVar.Alpha -> style.alpha = backup.floats[0]
            StyleVar.WindowPadding -> style.windowPadding put backup.floats
            StyleVar.WindowRounding -> style.windowRounding = backup.floats[0]
            StyleVar.WindowBorderSize -> style.windowBorderSize = backup.floats[0]
            StyleVar.WindowMinSize -> style.windowMinSize put backup.ints
            StyleVar.WindowTitleAlign -> style.windowTitleAlign put backup.floats
            StyleVar.ChildRounding -> style.childRounding = backup.floats[0]
            StyleVar.ChildBorderSize -> style.childBorderSize = backup.floats[0]
            StyleVar.PopupRounding -> style.popupRounding = backup.floats[0]
            StyleVar.PopupBorderSize -> style.popupBorderSize = backup.floats[0]
            StyleVar.FrameBorderSize -> style.frameBorderSize = backup.floats[0]
            StyleVar.FramePadding -> style.framePadding put backup.floats
            StyleVar.FrameRounding -> style.frameRounding = backup.floats[0]
            StyleVar.ItemSpacing -> style.itemSpacing put backup.floats
            StyleVar.ItemInnerSpacing -> style.itemInnerSpacing put backup.floats
            StyleVar.IndentSpacing -> style.indentSpacing = backup.floats[0]
            StyleVar.ScrollbarSize -> style.scrollbarSize = backup.floats[0]
            StyleVar.ScrollbarRounding -> style.scrollbarRounding = backup.floats[0]
            StyleVar.GrabMinSize -> style.grabMinSize = backup.floats[0]
            StyleVar.GrabRounding -> style.grabRounding = backup.floats[0]
            StyleVar.TabRounding -> style.tabRounding = backup.floats[0]
            StyleVar.ButtonTextAlign -> style.buttonTextAlign put backup.floats
            StyleVar.SelectableTextAlign -> style.selectableTextAlign put backup.floats
        }
    }

    /** retrieve style color as stored in ImGuiStyle structure. use to feed back into PushStyleColor(), otherwise use
     *  GetColorU32() to get style color + style alpha. */
    fun getStyleColorVec4(idx: Col): Vec4 = Vec4(style.colors[idx])

    /** get current font    */
    val font get() = g.font

    /** get current font size (= height in pixels) of current font with current scale applied   */
    val fontSize get() = g.fontSize

    /** get UV coordinate for a while pixel, useful to draw custom shapes via the ImDrawList API    */
    val fontTexUvWhitePixel get() = g.drawListSharedData.texUvWhitePixel

    /** retrieve given style color with style alpha applied and optional extra alpha multiplier */
    fun getColorU32(idx: Col, alphaMul: Float = 1f) = getColorU32(idx.i, alphaMul)

    fun getColorU32(idx: Int, alphaMul: Float = 1f): Int {
        val c = Vec4(style.colors[idx])
        c.w *= style.alpha * alphaMul
        return c.u32
    }

    /** retrieve given color with style alpha applied   */
    fun getColorU32(col: Vec4): Int {
        val c = Vec4(col)
        c.w *= style.alpha
        return c.u32
    }

    /** retrieve given color with style alpha applied    */
    fun getColorU32(col: Int): Int {
        val styleAlpha = style.alpha
        if (styleAlpha >= 1f) return col
        var a = (col and COL32_A_MASK) ushr COL32_A_SHIFT
        a = (a * styleAlpha).i // We don't need to clamp 0..255 because Style.Alpha is in 0..1 range.
        return (col and COL32_A_MASK.inv()) or (a shl COL32_A_SHIFT)
    }

    fun getColorU32(r: Int, g: Int, b: Int, a: Int): Int {
        val c = Vec4(r.f / 255.0, g.f / 255.0, b.f/255.0, a.f / 255.0)
        c.w *= style.alpha
        return c.u32
    }

// Parameters stacks (current window)

    /** width of items for the common item+label case, pixels. 0.0f = default to ~2/3 of windows width, >0.0f: width in
     *  pixels, <0.0f align xx pixels to the right of window (so -1.0f always align width to the right side)    */
    fun pushItemWidth(itemWidth: Int) = pushItemWidth(itemWidth.f)

    fun pushItemWidth(itemWidth: Float) = with(currentWindow) {
        dc.itemWidth = if (itemWidth == 0f) itemWidthDefault else itemWidth
        dc.itemWidthStack.push(dc.itemWidth)
    }

    fun popItemWidth() {
        with(currentWindow) {
            dc.itemWidthStack.pop()
            dc.itemWidth = if (dc.itemWidthStack.empty()) itemWidthDefault else dc.itemWidthStack.last()
        }
    }

    /** width of item given pushed settings and current cursor position */
    fun calcItemWidth(): Float {

        val window = currentWindowRead!!
        var w = window.dc.itemWidth
        if (w < 0f) {
            // Align to a right-side limit. We include 1 frame padding in the calculation because this is how the width is always used (we add 2 frame padding to it), but we could move that responsibility to the widget as well.
            val widthToRightEdge = contentRegionAvail.x
            w = glm.max(1f, widthToRightEdge + w)
        }
        return w.i.f
    }

    /** word-wrapping for Text*() commands. < 0.0f: no wrapping; 0.0f: wrap to end of window (or column);
     *  > 0.0f: wrap at 'wrapLocalPosX' position in window local space */
    fun pushTextWrapPos(wrapLocalPosX: Float = 0f) = with(currentWindow.dc) {
        textWrapPos = wrapLocalPosX
        textWrapPosStack.push(wrapLocalPosX)
    }

    fun popTextWrapPos() = with(currentWindow.dc) {
        textWrapPosStack.pop()
        textWrapPos = textWrapPosStack.lastOrNull() ?: -1f
    }

    // FIXME: Look into renaming this once we have settled the new Focus/Activation/TabStop system.
    fun pushAllowKeyboardFocus(allowKeyboardFocus: Boolean) = pushItemFlag(If.NoTabStop.i, !allowKeyboardFocus)

    fun popAllowKeyboardFocus() = popItemFlag()

    /** in 'repeat' mode, Button*() functions return repeated true in a typematic manner
     *  (using io.KeyRepeatDelay/io.KeyRepeatRate setting). Note that you can call IsItemActive() after any Button() to
     *  tell if the button is held in the current frame.    */
    fun pushButtonRepeat(repeat: Boolean) = pushItemFlag(If.ButtonRepeat.i, repeat)

    fun popButtonRepeat() = popItemFlag()
}