package imgui.imgui

import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.Context.style
import imgui.ImGui.beginPopup
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.calcWrapWidthForPos
import imgui.ImGui.checkboxFlags
import imgui.ImGui.colorPicker4
import imgui.ImGui.currentWindow
import imgui.ImGui.cursorScreenPos
import imgui.ImGui.endPopup
import imgui.ImGui.isClippedEx
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.popStyleColor
import imgui.ImGui.popTextWrapPos
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushTextWrapPos
import imgui.ImGui.renderBullet
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.renderTextWrapped
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.textLineHeight
import imgui.imgui.imgui_internal.Companion.smallSquareSize
import imgui.internal.Dir
import imgui.internal.Rect
import imgui.internal.strchr
import imgui.ColorEditFlags as Cef
import imgui.Context as g

interface imgui_widgetsText {

    /** doesn't require null terminated string if 'text_end' is specified. no copy done to any bounded stack buffer,
     *  recommended for long chunks of text */
    fun textUnformatted(text: String, textEnd: Int = text.length) {

        val window = currentWindow
        if (window.skipItems) return

        val textPos = Vec2(window.dc.cursorPos.x, window.dc.cursorPos.y + window.dc.currentLineTextBaseOffset)
        val wrapPosX = window.dc.textWrapPos
        val wrapEnabled = wrapPosX >= 0f
        if (textEnd > 2000 && !wrapEnabled) {
            /*  Long text!
                Perform manual coarse clipping to optimize for long multi-line text
                From this point we will only compute the width of lines that are visible. Optimization only available
                when word-wrapping is disabled.
                We also don't vertically center the text within the line full height, which is unlikely to matter
                because we are likely the biggest and only item on the line.    */

            var line = 0
            val lineHeight = textLineHeight
            val clipRect = Rect(window.clipRect)
            val textSize = Vec2()

            if (textPos.y <= clipRect.max.y) {

                val pos = Vec2(textPos)
                // Lines to skip (can't skip when logging text)
                if (!g.logEnabled) {
                    val linesSkippable = ((clipRect.min.y - textPos.y) / lineHeight).i
                    if (linesSkippable > 0) {
                        var linesSkipped = 0
                        while (line < textEnd && linesSkipped < linesSkippable) {
                            val lineEnd = text.strchr(line, '\n') ?: textEnd
                            line = lineEnd + 1
                            linesSkipped++
                        }
                        pos.y += linesSkipped * lineHeight
                    }
                }
                // Lines to render
                if (line < textEnd) {
                    val lineRect = Rect(pos, pos + Vec2(Float.MAX_VALUE, lineHeight))
                    while (line < textEnd) {
                        var lineEnd = text.strchr(line, '\n') ?: 0
                        if (isClippedEx(lineRect, 0, false)) break

                        val pLine = text.substring(line)
                        val lineSize = calcTextSize(pLine, lineEnd - line, false)
                        textSize.x = glm.max(textSize.x, lineSize.x)
                        renderText(pos, pLine, lineEnd - line, false)
                        if (lineEnd == 0) lineEnd = textEnd
                        line = lineEnd + 1
                        lineRect.min.y += lineHeight
                        lineRect.max.y += lineHeight
                        pos.y += lineHeight
                    }
                    // Count remaining lines
                    var linesSkipped = 0
                    while (line < textEnd) {
                        val lineEnd = text.strchr(line, '\n') ?: textEnd
                        line = lineEnd + 1
                        linesSkipped++
                    }
                    pos.y += linesSkipped * lineHeight
                }
                textSize.y += (pos - textPos).y
            }
            val bb = Rect(textPos, textPos + textSize)
            itemSize(bb)
            itemAdd(bb)
        } else {
            val wrapWidth = if (wrapEnabled) calcWrapWidthForPos(window.dc.cursorPos, wrapPosX) else 0f
            val textSize = calcTextSize(text, textEnd, false, wrapWidth)

            // Account of baseline offset
            val bb = Rect(textPos, textPos + textSize)
            itemSize(textSize)
            if (!itemAdd(bb)) return

            // Render (we don't hide text after ## in this end-user function)
            renderTextWrapped(bb.min, text, textEnd, wrapWidth)
        }
    }

    fun text(fmt: String, vararg args: Any) = textV(fmt, args)

    fun textV(fmt: String, args: Array<out Any>) {

        val window = currentWindow
        if (window.skipItems) return

        val fmt = if (args.isEmpty()) fmt else fmt.format(style.locale, *args)

        val textEnd = fmt.length
        textUnformatted(fmt, textEnd)
    }

    /** shortcut for PushStyleColor(ImGuiCol_Text, col); Text(fmt, ...); PopStyleColor();   */
    fun textColored(col: Vec4, fmt: String, vararg args: Any) {
        pushStyleColor(Col.Text, col)
        text(fmt, *args)
        popStyleColor()
    }

    /** shortcut for:
     *      pushStyleColor(Col.Text, style.colors[Col.TextDisabled])
     *      text(fmt, ...)
     *      popStyleColor() */
    fun textDisabled(fmt: String, vararg args: Any) = textDisabledV(fmt, args)

    fun textDisabledV(fmt: String, args: Array<out Any>) {
        pushStyleColor(Col.Text, style.colors[Col.TextDisabled])
        textV(fmt, args)
        popStyleColor()
    }

    /** shortcut for PushTextWrapPos(0.0f); Text(fmt, ...); PopTextWrapPos();. Note that this won't work on an
     *  auto-resizing window if there's no other widgets to extend the window width, yoy may need to set a size using
     *  SetNextWindowSize().    */
    fun textWrapped(fmt: String, vararg args: Any) {

        val needWrap = g.currentWindow!!.dc.textWrapPos < 0f  // Keep existing wrap position is one ia already set
        if (needWrap) pushTextWrapPos(0f)
        text(fmt, *args)
        if (needWrap) popTextWrapPos()
    }

    /** Display text+label aligned the same way as value+label widgets  */
    fun labelText(label: String, fmt: String, vararg args: Any) = labelTextV(label, fmt, args)

    /** Add a label+text combo aligned to other label+value widgets */
    fun labelTextV(label: String, fmt: String,  args: Array<out Any>) {

        val window = currentWindow
        if (window.skipItems) return
        val w = calcItemWidth()

        val labelSize = calcTextSize(label, 0, true)
        val valueBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w, labelSize.y + style.framePadding.y * 2))
        val totalBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w + if (labelSize.x > 0f) style.itemInnerSpacing.x else 0f, style.framePadding.y * 2) + labelSize)
        itemSize(totalBb, style.framePadding.y)
        if (!itemAdd(totalBb, 0)) return
        // Render
        val text = fmt.format(style.locale, *args)
        renderTextClipped(valueBb.min, valueBb.max, text, text.length, null, Vec2(0f, 0.5f))
        if (labelSize.x > 0f)
            renderText(Vec2(valueBb.max.x + style.itemInnerSpacing.x, valueBb.min.y + style.framePadding.y), label)
    }

    /** shortcut for Bullet()+Text()    */
    fun bulletText(fmt: String, vararg args: Any) = bulletTextV(fmt, args)

    /** Text with a little bullet aligned to the typical tree node. */
    fun bulletTextV(fmt: String, args: Array<out Any>) {

        val window = currentWindow
        if (window.skipItems) return

        val text = fmt.format(style.locale, *args)
        val labelSize = calcTextSize(text, false)
        val textBaseOffsetY = glm.max(0f, window.dc.currentLineTextBaseOffset) // Latch before ItemSize changes it
        val lineHeight = glm.max(glm.min(window.dc.currentLineHeight, g.fontSize + style.framePadding.y * 2), g.fontSize)
        val x = g.fontSize + if (labelSize.x > 0f) labelSize.x + style.framePadding.x * 2 else 0f
        // Empty text doesn't add padding
        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(x, glm.max(lineHeight, labelSize.y)))
        itemSize(bb)
        if (!itemAdd(bb)) return

        // Render
        renderBullet(bb.min + Vec2(style.framePadding.x + g.fontSize * 0.5f, lineHeight * 0.5f))
        renderText(bb.min + Vec2(g.fontSize + style.framePadding.x * 2, textBaseOffsetY), text, text.length, false)
    }

    /** draw a small circle and keep the cursor on the same line. advance cursor x position
     *  by GetTreeNodeToLabelSpacing(), same distance that TreeNode() uses  */
    fun bullet() {

        val window = currentWindow
        if (window.skipItems) return

        val lineHeight = glm.max(glm.min(window.dc.currentLineHeight, g.fontSize + style.framePadding.y * 2), g.fontSize)
        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(g.fontSize, lineHeight))
        itemSize(bb)
        if (!itemAdd(bb)) {
            sameLine(0f, style.framePadding.x * 2)
            return
        }

        // Render and stay on same line
        renderBullet(bb.min + Vec2(style.framePadding.x + g.fontSize * 0.5f, lineHeight * 0.5f))
        sameLine(0f, style.framePadding.x * 2)
    }

    // TODO, lambdas are so short, consider removing it
    object Items {
        // FIXME-OPT: we could pre-compute the indices to fasten this. But only 1 active combo means the waste is limited.
        val singleStringGetter = { data: String, idx: Int -> data.split('\u0000')[idx] }
        val arrayGetter = { data: Array<String>, idx: Int, outText: Array<String> -> outText[0] = data[idx]; true }
    }

    companion object {
        /** 'pos' is position of the arrow tip. halfSz.x is length from base to tip. halfSz.y is length on each side. */
        fun renderArrow(drawList: DrawList, pos: Vec2, halfSz: Vec2, direction: Dir, col: Int) = when (direction) {
            Dir.Right -> drawList.addTriangleFilled(Vec2(pos.x - halfSz.x, pos.y + halfSz.y), Vec2(pos.x - halfSz.x, pos.y - halfSz.y), pos, col)
            Dir.Left -> drawList.addTriangleFilled(Vec2(pos.x + halfSz.x, pos.y - halfSz.y), Vec2(pos.x + halfSz.x, pos.y + halfSz.y), pos, col)
            Dir.Down -> drawList.addTriangleFilled(Vec2(pos.x - halfSz.x, pos.y - halfSz.y), Vec2(pos.x + halfSz.x, pos.y - halfSz.y), pos, col)
            Dir.Up -> drawList.addTriangleFilled(Vec2(pos.x + halfSz.x, pos.y + halfSz.y), Vec2(pos.x - halfSz.x, pos.y + halfSz.y), pos, col)
            else -> Unit
        }

        fun renderArrowsForVerticalBar(drawList: DrawList, pos: Vec2, halfSz: Vec2, barW: Float) {
            renderArrow(drawList, Vec2(pos.x + halfSz.x + 1, pos.y), Vec2(halfSz.x + 2, halfSz.y + 1), Dir.Right, COL32_BLACK)
            renderArrow(drawList, Vec2(pos.x + halfSz.x, pos.y), halfSz, Dir.Right, COL32_WHITE)
            renderArrow(drawList, Vec2(pos.x + barW - halfSz.x - 1, pos.y), Vec2(halfSz.x + 2, halfSz.y + 1), Dir.Left, COL32_BLACK)
            renderArrow(drawList, Vec2(pos.x + barW - halfSz.x, pos.y), halfSz, Dir.Left, COL32_WHITE)
        }

        fun colorPickerOptionsPopup(flags: Int, refCol: FloatArray) {
            val allowOptPicker = flags hasnt Cef._PickerMask
            val allowOptAlphaBar = flags hasnt Cef.NoAlpha && flags hasnt Cef.AlphaBar
            if ((!allowOptPicker && !allowOptAlphaBar) || !beginPopup("context")) return
            if (allowOptPicker) {
                // FIXME: Picker size copied from main picker function
                val pickerSize = Vec2(g.fontSize * 8, glm.max(g.fontSize * 8 - (smallSquareSize + style.itemInnerSpacing.x), 1f))
                pushItemWidth(pickerSize.x)
                for (pickerType in 0..1) {
                    // Draw small/thumbnail version of each picker type (over an invisible button for selection)
                    if (pickerType > 0) separator()
                    pushId(pickerType)
                    var pickerFlags = Cef.NoInputs or Cef.NoOptions or Cef.NoLabel or
                            Cef.NoSidePreview or (flags and Cef.NoAlpha)
                    if (pickerType == 0) pickerFlags = pickerFlags or Cef.PickerHueBar
                    if (pickerType == 1) pickerFlags = pickerFlags or Cef.PickerHueWheel
                    val backupPos = Vec2(cursorScreenPos)
                    if (selectable("##selectable", false, 0, pickerSize)) // By default, Selectable() is closing popup
                        g.colorEditOptions = (g.colorEditOptions wo Cef._PickerMask) or (pickerFlags and Cef._PickerMask)
                    cursorScreenPos = backupPos
                    val dummyRefCol = Vec4()
                    for (i in 0..2) dummyRefCol[i] = refCol[i]
                    if (pickerFlags hasnt Cef.NoAlpha) dummyRefCol[3] = refCol[3]
                    val pF = floatArrayOf(dummyRefCol.x)
                    colorPicker4("##dummypicker", pF, pickerFlags)
                    dummyRefCol.x = pF[0]
                    popId()
                }
                popItemWidth()
            }
            if (allowOptAlphaBar) {
                if (allowOptPicker) separator()
                val pI = intArrayOf(g.colorEditOptions)
                checkboxFlags("Alpha Bar", pI, Cef.AlphaBar.i)
                g.colorEditOptions = pI[0]
            }
            endPopup()
        }
    }
}