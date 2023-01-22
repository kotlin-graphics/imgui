package imgui.api

import glm_.f
import glm_.max
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.contentRegionMaxAbs
import imgui.ImGui.currentWindow
import imgui.ImGui.defaultFont
import imgui.ImGui.popItemFlag
import imgui.ImGui.pushItemFlag
import imgui.ImGui.style
import imgui.font.Font
import imgui.internal.*
import imgui.internal.classes.ColorMod
import imgui.internal.classes.StyleMod
import imgui.internal.sections.NextItemDataFlag
import imgui.internal.sections.has
import imgui.internal.sections.or
import imgui.internal.sections.wo
import imgui.internal.sections.ItemFlag as If

// Parameters stacks (shared)
interface parametersStacks {

    /** use NULL as a shortcut to push default font */
    fun pushFont(font: Font = defaultFont) {
        font.setCurrent()
        g.fontStack.push(font)
        g.currentWindow!!.drawList.pushTextureID(font.containerAtlas.texID)
    }

    fun popFont() {
        g.currentWindow!!.drawList.popTextureId()
        g.fontStack.pop()
        (g.fontStack.lastOrNull() ?: defaultFont).setCurrent()
    }

    /** FIXME: This may incur a round-trip (if the end user got their data from a float4) but eventually we aim to store
     *  the in-flight colors as ImU32
     *
     *  modify a style color. always use this if you modify the style after NewFrame(). */
    fun pushStyleColor(idx: Col, col: Int) {
        val backup = ColorMod(idx, style.colors[idx])
        g.colorStack.push(backup)
        g.style.colors[idx] = col.vec4
    }

    fun pushStyleColor(idx: Col, col: Vec4) {
        val backup = ColorMod(idx, style.colors[idx])
        g.colorStack.push(backup)
        style.colors[idx] = col
    }

    fun popStyleColor(count: Int = 1) = repeat(count) {
        val backup = g.colorStack.pop()
        style.colors[backup.col] put backup.backupValue
    }

    /** It'll throw error if wrong correspondence between idx and value type
     *  GStyleVarInfo
     *
     *  modify a style float variable. always use this if you modify the style after NewFrame().
     *  modify a style ImVec2 variable. always use this if you modify the style after NewFrame(). */
    fun pushStyleVar(idx: StyleVar, value: Any) {
        g.styleVarStack.push(StyleMod(idx).also {
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
                StyleVar.CellPadding -> {
                    style.cellPadding to it.floats
                    style.cellPadding put (value as Vec2)
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
        val backup = g.styleVarStack.pop()
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

    // FIXME: Look into renaming this once we have settled the new Focus/Activation/TabStop system.
    fun pushAllowKeyboardFocus(allowKeyboardFocus: Boolean) = pushItemFlag(If.NoTabStop.i, !allowKeyboardFocus)

    fun popAllowKeyboardFocus() = popItemFlag()

    /** in 'repeat' mode, Button*() functions return repeated true in a typematic manner
     *  (using io.KeyRepeatDelay/io.KeyRepeatRate setting). Note that you can call IsItemActive() after any Button() to
     *  tell if the button is held in the current frame.    */
    fun pushButtonRepeat(repeat: Boolean) = pushItemFlag(If.ButtonRepeat.i, repeat)

    fun popButtonRepeat() = popItemFlag()


    // Parameters stacks (current window)

    /** push width of items for common large "item+label" widgets. >0.0f: width in pixels, <0.0f align xx pixels to the
     *  right of window (so -FLT_MIN always align width to the right side). */
    fun pushItemWidth(itemWidth: Int) = pushItemWidth(itemWidth.f)

    // FIXME: Remove the == 0.0f behavior?
    fun pushItemWidth(itemWidth: Float) {
        currentWindow.apply {
            dc.itemWidthStack.push(dc.itemWidth) // Backup current width
            dc.itemWidth = if (itemWidth == 0f) itemWidthDefault else itemWidth
        }
        g.nextItemData.flags = g.nextItemData.flags wo NextItemDataFlag.HasWidth
    }

    fun popItemWidth() {
        with(currentWindow.dc) {
            itemWidth = itemWidthStack.last()
            itemWidthStack.pop()
        }
    }

    /** set width of the _next_ common large "item+label" widget. >0.0f: width in pixels, <0.0f align xx pixels to the
     *  right of window (so -FLT_MIN always align width to the right side) */
    fun setNextItemWidth(itemWidth: Float) {
        g.nextItemData.flags = g.nextItemData.flags or NextItemDataFlag.HasWidth
        g.nextItemData.width = itemWidth
    }

    /** Calculate default item width given value passed to PushItemWidth() or SetNextItemWidth().
     *  The SetNextItemWidth() data is generally cleared/consumed by ItemAdd() or NextItemData.ClearFlags()
     *
     *  width of item given pushed settings and current cursor position. NOT necessarily the width of last item unlike most 'Item' functions.
     *
     *  ~ GetNextItemWidth   */
    fun calcItemWidth(): Float {
        val window = g.currentWindow!!
        var w = when {
            g.nextItemData.flags has NextItemDataFlag.HasWidth -> g.nextItemData.width
            else -> window.dc.itemWidth
        }
        if (w < 0f) {
            val regionMaxX = contentRegionMaxAbs.x
            w = 1f max (regionMaxX - window.dc.cursorPos.x + w)
        }
        return floor(w)
    }

    /** push word-wrapping position for Text*() commands. < 0.0f: no wrapping; 0.0f: wrap to end of window (or column);
     *  > 0.0f: wrap at 'wrapLocalPosX' position in window local space */
    fun pushTextWrapPos(wrapPosX: Float = 0f) = with(currentWindow.dc) {
        textWrapPosStack.push(textWrapPos)
        textWrapPos = wrapPosX
    }

    fun popTextWrapPos() = with(currentWindow.dc) {
        textWrapPos = textWrapPosStack.last()
        textWrapPosStack.pop()
    }
}