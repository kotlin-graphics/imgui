package imgui.imgui

import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.colorConvertFloat4ToU32
import imgui.ImGui.contentRegionAvail
import imgui.ImGui.currentWindow
import imgui.ImGui.currentWindowRead
import imgui.internal.ColMod
import imgui.internal.StyleMod
import imgui.Context as g


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

    fun pushStyleColor(idx: Col, col: Vec4) {

        val backup = ColMod(idx, Style.colors[idx])
        g.colorModifiers.push(backup)
        Style.colors[idx] = col
    }

    fun popStyleColor(count: Int = 1) = repeat(count) {
        val backup = g.colorModifiers.pop()
        Style.colors[backup.col] put backup.backupValue
    }

    /** It'll throw error if wrong correspondence between idx and value type    */
    fun pushStyleVar(idx: StyleVar, value: Any) {

        g.styleModifiers.push(StyleMod(idx).also {
            when (idx) {
                StyleVar.Alpha -> {
                    it.floats[0] = Style.alpha
                    Style.alpha = value as Float
                }
                StyleVar.WindowPadding -> {
                    Style.windowPadding to it.floats
                    Style.windowPadding put (value as Vec2)
                }
                StyleVar.WindowRounding -> {
                    it.floats[0] = Style.windowRounding
                    Style.windowRounding = value as Float
                }
                StyleVar.WindowMinSize -> {
                    Style.windowMinSize to it.ints
                    Style.windowMinSize put (value as Vec2i)
                }
                StyleVar.ChildWindowRounding -> {
                    it.floats[0] = Style.childWindowRounding
                    Style.childWindowRounding = value as Float
                }
                StyleVar.FramePadding -> {
                    Style.framePadding to it.floats
                    Style.framePadding put (value as Vec2)
                }
                StyleVar.FrameRounding -> {
                    it.floats[0] = Style.frameRounding
                    Style.frameRounding = value as Float
                }
                StyleVar.ItemSpacing -> {
                    Style.itemSpacing to it.floats
                    Style.itemSpacing put (value as Vec2)
                }
                StyleVar.ItemInnerSpacing -> {
                    Style.itemInnerSpacing to it.floats
                    Style.itemInnerSpacing put (value as Vec2)
                }
                StyleVar.IndentSpacing -> {
                    it.floats[0] = Style.indentSpacing
                    Style.indentSpacing = value as Float
                }
                StyleVar.GrabMinSize -> {
                    it.floats[0] = Style.grabMinSize
                    Style.grabMinSize = value as Float
                }
                StyleVar.ButtonTextAlign -> {
                    Style.buttonTextAlign to it.floats
                    Style.buttonTextAlign put (value as Vec2)
                }
                else -> Unit
            }
        })
    }

    fun popStyleVar(count: Int = 1) = repeat(count) {
        val backup = g.styleModifiers.pop()
        when (backup.idx) {
            StyleVar.Alpha -> Style.alpha = backup.floats[0]
            StyleVar.WindowPadding -> Style.windowPadding put backup.floats
            StyleVar.WindowRounding -> Style.windowRounding = backup.floats[0]
            StyleVar.WindowMinSize -> Style.windowMinSize put backup.ints
            StyleVar.ChildWindowRounding -> Style.childWindowRounding = backup.floats[0]
            StyleVar.FramePadding -> Style.framePadding put backup.floats
            StyleVar.FrameRounding -> Style.frameRounding = backup.floats[0]
            StyleVar.ItemSpacing -> Style.itemSpacing put backup.floats
            StyleVar.ItemInnerSpacing -> Style.itemInnerSpacing put backup.floats
            StyleVar.IndentSpacing -> Style.indentSpacing = backup.floats[0]
            StyleVar.GrabMinSize -> Style.grabMinSize = backup.floats[0]
            StyleVar.ButtonTextAlign -> Style.buttonTextAlign put backup.floats
            else -> Unit
        }
    }

    /** get current font    */
    val font get() = g.font

    /** get current font size (= height in pixels) of current font with current scale applied   */
    val fontSize get() = g.fontSize

    /** get UV coordinate for a while pixel, useful to draw custom shapes via the ImDrawList API    */
    val fontTexUvWhitePixel get() = g.fontTexUvWhitePixel

    /** retrieve given style color with style alpha applied and optional extra alpha multiplier */
    fun getColorU32(idx: Col, alphaMul: Float = 1f) = getColorU32(idx.i, alphaMul)

    fun getColorU32(idx: Int, alphaMul: Float = 1f): Int {
        val c = Vec4(Style.colors[idx])
        c.w *= Style.alpha * alphaMul
        return colorConvertFloat4ToU32(c)
    }

    /** retrieve given color with style alpha applied   */
    fun getColorU32(col: Vec4): Int {
        val c = Vec4(col)
        c.w *= Style.alpha
        return colorConvertFloat4ToU32(c)
    }

// Parameters stacks (current window)

    /** width of items for the common item+label case, pixels. 0.0f = default to ~2/3 of windows width, >0.0f: width in
     *  pixels, <0.0f align xx pixels to the right of window (so -1.0f always align width to the right side)    */
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
     *  > 0.0f: wrap at 'wrapPosX' position in window local space */
    fun pushTextWrapPos(wrapPosX: Float = 0f) = with(currentWindow.dc) {
        textWrapPos = wrapPosX
        textWrapPosStack.push(wrapPosX)
    }

    fun popTextWrapPos() = with(currentWindow.dc) {
        textWrapPosStack.pop()
        textWrapPos = textWrapPosStack.lastOrNull() ?: -1f
    }

    /** allow focusing using TAB/Shift-TAB, enabled by default but you can disable it for certain widgets   */
    fun pushAllowKeyboardFocus(allowKeyboardFocus:Boolean) = with(currentWindow){
        dc.allowKeyboardFocus = allowKeyboardFocus
        dc.allowKeyboardFocusStack.push(allowKeyboardFocus)
    }

    fun popAllowKeyboardFocus() = with(currentWindow.dc) {
        allowKeyboardFocusStack.pop()
        allowKeyboardFocus = allowKeyboardFocusStack.lastOrNull() ?: true
    }

    /** in 'repeat' mode, Button*() functions return repeated true in a typematic manner
     *  (uses io.KeyRepeatDelay/io.KeyRepeatRate for now). Note that you can call IsItemActive() after any Button() to
     *  tell if the button is held in the current frame.    */
    fun pushButtonRepeat(repeat:Boolean) = with(currentWindow.dc) {
        buttonRepeat = repeat
        buttonRepeatStack.push(repeat)
    }

    fun popButtonRepeat() = with(currentWindow.dc) {
        buttonRepeatStack.pop()
        buttonRepeat = buttonRepeatStack.lastOrNull() ?: false
    }
}