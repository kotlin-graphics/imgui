package imgui.imgui

import gli.has
import glm_.f
import glm_.func.cos
import glm_.func.sin
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.Context.style
import imgui.ImGui.F32_TO_INT8_UNBOUND
import imgui.ImGui.beginGroup
import imgui.ImGui.beginPopup
import imgui.ImGui.buttonBehavior
import imgui.ImGui.buttonEx
import imgui.ImGui.calcItemSize
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.calcWrapWidthForPos
import imgui.ImGui.checkboxFlags
import imgui.ImGui.clearActiveId
import imgui.ImGui.u32
import imgui.ImGui.colorConvertHSVtoRGB
import imgui.ImGui.colorConvertRGBtoHSV
import imgui.ImGui.colorPicker4
import imgui.ImGui.colorTooltip
import imgui.ImGui.currentWindow
import imgui.ImGui.cursorScreenPos
import imgui.ImGui.dragFloat
import imgui.ImGui.dragInt
import imgui.ImGui.endGroup
import imgui.ImGui.endPopup
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.focusWindow
import imgui.ImGui.getColorU32
import imgui.ImGui.hsvToRGB
import imgui.ImGui.inputText
import imgui.ImGui.isClippedEx
import imgui.ImGui.isHovered
import imgui.ImGui.isItemActive
import imgui.ImGui.isItemRectHovered
import imgui.ImGui.isMouseClicked
import imgui.ImGui.isPopupOpen
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.openPopup
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.popStyleColor
import imgui.ImGui.popStyleVar
import imgui.ImGui.popTextWrapPos
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushStyleVar
import imgui.ImGui.pushTextWrapPos
import imgui.ImGui.radioButton
import imgui.ImGui.renderBullet
import imgui.ImGui.renderCollapseTriangle
import imgui.ImGui.renderColorRectWithAlphaCheckerboard
import imgui.ImGui.renderFrame
import imgui.ImGui.renderFrameBorder
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.renderTextWrapped
import imgui.ImGui.rgbToHSV
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setHoveredId
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setScrollHere
import imgui.ImGui.spacing
import imgui.ImGui.textLineHeight
import imgui.imgui.imgui_internal.Companion.colorSquareSize
import imgui.internal.*
import imgui.Context as g

interface imgui_widgets {

    fun text(fmt: String, vararg args: Any) = textV(fmt, args)

    fun textV(fmt: String, args: Array<out Any>) {

        val window = currentWindow
        if (window.skipItems) return

        val fmt =
                if (args.isEmpty())
                    fmt
                else
                    fmt.format(style.locale, *args)

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

    /** doesn't require null terminated string if 'text_end' is specified. no copy done to any bounded stack buffer,
     *  recommended for long chunks of text */
    fun textUnformatted(text: String, textEnd: Int = text.length) {

        val window = currentWindow
        if (window.skipItems) return

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
            val textPos = window.dc.cursorPos + Vec2(0f, window.dc.currentLineTextBaseOffset)
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
                        if (isClippedEx(lineRect, null, false)) break

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
                        val line_end = text.strchr(line, '\n') ?: textEnd
                        line = line_end + 1
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
            val textPos = Vec2(window.dc.cursorPos.x, window.dc.cursorPos.y + window.dc.currentLineTextBaseOffset)
            val bb = Rect(textPos, textPos + textSize)
            itemSize(textSize)
            if (!itemAdd(bb)) return

            // Render (we don't hide text after ## in this end-user function)
            renderTextWrapped(bb.min, text, textEnd, wrapWidth)
        }
    }

//    IMGUI_API void          LabelText(const char* label, const char* fmt, ...) IM_PRINTFARGS(2);    // display text+label aligned the same way as value+label widgets
//    IMGUI_API void          LabelTextV(const char* label, const char* fmt, va_list args);

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

    /** button  */
    fun button(label: String, sizeArg: Vec2 = Vec2()) = buttonEx(label, sizeArg, 0)

    /** button with FramePadding = (0,0) to easily embed in text
     *  Small buttons fits within text without additional vertical spacing.     */
    fun smallButton(label: String): Boolean {
        val backupPaddingY = style.framePadding.y
        style.framePadding.y = 0f
        val pressed = buttonEx(label, Vec2(), ButtonFlags.AlignTextBaseLine.i)
        style.framePadding.y = backupPaddingY
        return pressed
    }

    /** Tip: use ImGui::PushID()/PopID() to push indices or pointers in the ID stack.
     *  Then you can keep 'str_id' empty or the same for all your buttons (instead of creating a string based on a
     *  non-string id)  */
    fun invisibleButton(strId: String, sizeArg: Vec2): Boolean {
        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(strId)
        val size = calcItemSize(sizeArg, 0f, 0f)
        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        itemSize(bb)
        if (!itemAdd(bb, id)) return false

        val (pressed, _, _) = buttonBehavior(bb, id)

        return pressed
    }


    fun image(userTextureId: Int, size: Vec2, uv0: Vec2 = Vec2(), uv1: Vec2 = Vec2(1), tintCol: Vec4 = Vec4(1),
              borderCol: Vec4 = Vec4()) {

        val window = currentWindow
        if (window.skipItems) return

        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        if (borderCol.w > 0f)
            bb.max plus_ 2
        itemSize(bb)
        if (!itemAdd(bb)) return

        if (borderCol.w > 0f) {
            window.drawList.addRect(bb.min, bb.max, getColorU32(borderCol), 0f)
            window.drawList.addImage(userTextureId, bb.min + 1, bb.max - 1, uv0, uv1, getColorU32(tintCol))
        } else
            window.drawList.addImage(userTextureId, bb.min, bb.max, uv0, uv1, getColorU32(tintCol))
    }

    /** frame_padding < 0: uses FramePadding from style (default)
     *  frame_padding = 0: no framing/padding
     *  frame_padding > 0: set framing size
     *  The color used are the button colors.   */
    fun imageButton(userTextureId: Int, size: Vec2, uv0: Vec2 = Vec2(), uv1: Vec2 = Vec2(), framePadding: Int = -1, bgCol: Vec4 = Vec4(),
                    tintCol: Vec4 = Vec4(1)): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        /*  Default to using texture ID as ID. User can still push string/integer prefixes.
            We could hash the size/uv to create a unique ID but that would prevent the user from animating UV.         */
        pushId(userTextureId)
        val id = window.getId("#image")
        popId()

        val padding = if (framePadding >= 0) Vec2(framePadding) else Vec2(style.framePadding)
        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + size + padding * 2)
        val imageBb = Rect(window.dc.cursorPos + padding, window.dc.cursorPos + padding + size)
        itemSize(bb)
        if (!itemAdd(bb, id))
            return false

        val (pressed, hovered, held) = buttonBehavior(bb, id)

        // Render
        val col = if (hovered && held) Col.ButtonActive else if (hovered) Col.ButtonHovered else Col.Button
        renderFrame(bb.min, bb.max, col.u32, true, glm.clamp(glm.min(padding.x, padding.y), 0f, style.frameRounding))
        if (bgCol.w > 0f)
            window.drawList.addRectFilled(imageBb.min, imageBb.max, getColorU32(bgCol))
        window.drawList.addImage(userTextureId, imageBb.min, imageBb.max, uv0, uv1, getColorU32(tintCol))

        return pressed
    }

    fun checkbox(label: String, v: BooleanArray): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(label)
        val labelSize = calcTextSize(label, true)

        // We want a square shape to we use Y twice
        val checkBb = Rect(window.dc.cursorPos, window.dc.cursorPos +
                Vec2(labelSize.y + style.framePadding.y * 2, labelSize.y + style.framePadding.y * 2))
        itemSize(checkBb, style.framePadding.y)

        val totalBb = Rect(checkBb)
        if (labelSize.x > 0)
            sameLine(0f, style.itemInnerSpacing.x)
        val textBb = Rect(window.dc.cursorPos + Vec2(0, style.framePadding.y), window.dc.cursorPos + Vec2(0, style.framePadding.y) + labelSize)
        if (labelSize.x > 0) {
            itemSize(Vec2(textBb.width, checkBb.height), style.framePadding.y)
            glm.min(checkBb.min, textBb.min, totalBb.min)
            glm.max(checkBb.max, textBb.max, totalBb.max)
        }

        if (!itemAdd(totalBb, id)) return false

        val (pressed, hovered, held) = buttonBehavior(totalBb, id)
        if (pressed) v[0] = !v[0]

        val col = if (held && hovered) Col.FrameBgActive else if (hovered) Col.FrameBgHovered else Col.FrameBg
        renderFrame(checkBb.min, checkBb.max, col.u32, true, style.frameRounding)
        if (v[0]) {
            val checkSz = glm.min(checkBb.width, checkBb.height)
            val pad = glm.max(1f, (checkSz / 6f).i.f)
            window.drawList.addRectFilled(checkBb.min + Vec2(pad), checkBb.max - Vec2(pad), Col.CheckMark.u32, style.frameRounding)
        }

        if (g.logEnabled) logRenderedText(textBb.tl, if (v[0]) "[x]" else "[ ]")
        if (labelSize.x > 0f) renderText(textBb.tl, label)

        return pressed
    }

    fun checkboxFlags(label: String, flags: IntArray, flagsValue: Int): Boolean {
        val v = booleanArrayOf((flags[0] and flagsValue) == flagsValue)
        val pressed = checkbox(label, v)
        if (pressed) {
            if (v[0])
                flags[0] = flags[0] or flagsValue
            else
                flags[0] = flags[0] wo flagsValue
        }

        return pressed
    }

    fun radioButton(label: String, active: Boolean): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(label)
        val labelSize = calcTextSize(label, true)

        val checkBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(labelSize.y + style.framePadding.y * 2 - 1, labelSize.y + style.framePadding.y * 2 - 1))
        itemSize(checkBb, style.framePadding.y)

        val totalBb = Rect(checkBb)
        if (labelSize.x > 0)
            sameLine(0f, style.itemInnerSpacing.x)
        val textBb = Rect(window.dc.cursorPos + Vec2(0, style.framePadding.y), window.dc.cursorPos + Vec2(0, style.framePadding.y) + labelSize)
        if (labelSize.x > 0) {
            itemSize(Vec2(textBb.width, checkBb.height), style.framePadding.y)
            totalBb.add(textBb)
        }

        if (!itemAdd(totalBb, id)) return false

        val center = Vec2(checkBb.center)
        center.x = (center.x + 0.5f).i.f
        center.y = (center.y + 0.5f).i.f
        val radius = checkBb.height * 0.5f

        val (pressed, hovered, held) = buttonBehavior(totalBb, id)

        val col = if (held && hovered) Col.FrameBgActive else if (hovered) Col.FrameBgHovered else Col.FrameBg
        window.drawList.addCircleFilled(center, radius, col.u32, 16)
        if (active) {
            val checkSz = glm.min(checkBb.width, checkBb.height)
            val pad = glm.max(1f, (checkSz / 6f).i.f)
            window.drawList.addCircleFilled(center, radius - pad, Col.CheckMark.u32, 16)
        }

        if (window.flags has WindowFlags.ShowBorders) {
            window.drawList.addCircle(center + Vec2(1), radius, Col.BorderShadow.u32, 16)
            window.drawList.addCircle(center, radius, Col.Border.u32, 16)
        }

        if (g.logEnabled)
            logRenderedText(textBb.tl, if (active) "(x)" else "( )")
        if (labelSize.x > 0.0f)
            renderText(textBb.tl, label)

        return pressed
    }

    fun radioButton(label: String, v: IntArray, vButton: Int): Boolean {
        val pressed = radioButton(label, v[0] == vButton)
        if (pressed) v[0] = vButton
        return pressed
    }
//    IMGUI_API bool          Combo(const char* label, int* current_item, const char* const* items, int items_count, int height_in_items = -1);

    /** Combo box helper allowing to pass all items in a single string.
     *  separate items with \0, end item-list with \0\0     */
    fun combo(label: String, currentItem: IntArray, itemsSeparatedByZeros: String, heightInItems: Int = -1) =
            combo(label, currentItem, Items.singleStringGetter, itemsSeparatedByZeros,
                    itemsSeparatedByZeros.split('\u0000').filter { it.isNotEmpty() }.count(), // FIXME-OPT: Avoid computing this, or at least only when combo is open
                    heightInItems)

    fun combo(label: String, currentItem: IntArray, items: Array<String>, heightInItems: Int = -1): Boolean {
        val itemsSeparatedByZeros = items.map { "$it\u0000" }.joinToString(separator = "") + "\u0000"
        return combo(label, currentItem, Items.singleStringGetter, itemsSeparatedByZeros, items.size, heightInItems)
    }

    // Combo box function.
    fun combo(label: String, currentItem: IntArray, itemsGetter: (String, Int) -> String?, data: String, itemsCount: Int,
              heightInItems: Int = -1): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(label)
        val w = calcItemWidth()

        val labelSize = calcTextSize(label, true)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w, labelSize.y + style.framePadding.y * 2f))
        val totalBb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))
        itemSize(totalBb, style.framePadding.y)
        if (!itemAdd(totalBb, id)) return false

        val arrowSize = g.fontSize + style.framePadding.x * 2f
        val hovered = isHovered(frameBb, id)
        var popupOpen = isPopupOpen(id)

        val valueBb = Rect(frameBb.min, frameBb.max - Vec2(arrowSize, 0f))
        renderFrame(frameBb.min, frameBb.max, Col.FrameBg.u32, true, style.frameRounding)
        val col = if (popupOpen || hovered) Col.ButtonHovered else Col.Button
        renderFrame(Vec2(frameBb.max.x - arrowSize, frameBb.min.y), frameBb.max, col.u32, true, style.frameRounding) // FIXME-ROUNDING
        renderCollapseTriangle(Vec2(frameBb.max.x - arrowSize, frameBb.min.y) + style.framePadding, true)

        if (currentItem[0] in 0 until itemsCount)
            itemsGetter(data, currentItem[0])?.let { renderTextClipped(frameBb.min + style.framePadding, valueBb.max, it) }

        if (labelSize.x > 0)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, frameBb.min.y + style.framePadding.y), label)

        var popupToggled = false
        if (hovered) {
            setHoveredId(id)
            if (IO.mouseClicked[0]) {
                clearActiveId()
                popupToggled = true
            }
        }

        if (popupToggled)
            if (isPopupOpen(id))
                closePopup(id)
            else {
                focusWindow(window)
                openPopup(label)
                popupOpen = true
            }

        var valueChanged = false
        if (isPopupOpen(id)) {
            // Size default to hold ~7 items
            var heightInItems = heightInItems
            if (heightInItems < 0)
                heightInItems = 7

            val popupHeight = (labelSize.y + style.itemSpacing.y) * glm.min(itemsCount, heightInItems) + style.framePadding.y * 3
            var popupY1 = frameBb.max.y
            var popupY2 = glm.clamp(popupY1 + popupHeight, popupY1, IO.displaySize.y - style.displaySafeAreaPadding.y)
            if ((popupY2 - popupY1) < glm.min(popupHeight, frameBb.min.y - style.displaySafeAreaPadding.y)) {
                /*  Position our combo ABOVE because there's more space to fit!
                    (FIXME: Handle in Begin() or use a shared helper. We have similar code in Begin() for popup placement)                 */
                popupY1 = glm.clamp(frameBb.min.y - popupHeight, style.displaySafeAreaPadding.y, frameBb.min.y)
                popupY2 = frameBb.min.y
            }
            val popupRect = Rect(Vec2(frameBb.min.x, popupY1), Vec2(frameBb.max.x, popupY2))
            setNextWindowPos(popupRect.min)
            setNextWindowSize(popupRect.size)
            pushStyleVar(StyleVar.WindowPadding, style.framePadding)

            val flags = WindowFlags.ComboBox or if (window.flags has WindowFlags.ShowBorders) WindowFlags.ShowBorders.i else 0
            if (beginPopupEx(id, flags)) {
                // Display items
                // FIXME-OPT: Use clipper
                spacing()
                repeat(itemsCount) { i ->
                    pushId(i)
                    val itemSelected = i == currentItem[0]
                    val itemText = itemsGetter(data, i) ?: "Unknown item*"
                    if (selectable(itemText, itemSelected)) {
                        clearActiveId()
                        valueChanged = true
                        currentItem[0] = i
                    }
                    if (itemSelected && popupToggled) setScrollHere()
                    popId()
                }
                endPopup()
            }
            popStyleVar()
        }
        return valueChanged
    }

    /**  A little colored square. Return true when clicked.
     *  FIXME: May want to display/ignore the alpha component in the color display? Yet show it in the tooltip.
     *  'desc_id' is not called 'label' because we don't display it next to the button, but only in the tooltip.    */
    fun colorButton(descId: String, col: Vec4, flags: Int = 0, size: Vec2 = Vec2()): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(descId)
        val defaultSize = colorSquareSize
        if (size.x == 0f)
            size.x = defaultSize
        if (size.y == 0f)
            size.y = defaultSize
        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        itemSize(bb)
        if (!itemAdd(bb, id)) return false

        val (pressed, hovered, held) = buttonBehavior(bb, id)

        var flags = flags
        if (flags has ColorEditFlags.NoAlpha)
            flags = flags and (ColorEditFlags.AlphaPreview or ColorEditFlags.AlphaPreviewHalf).inv()

        val colWithoutAlpha = Vec4(col.x, col.y, col.z, 1f)
        val gridStep = glm.min(size.x, size.y) / 2.99f
        val rounding = glm.min(style.frameRounding, gridStep * 0.5f)
        if (flags has ColorEditFlags.AlphaPreviewHalf && col.w < 1f) {
            val midX = ((bb.min.x + bb.max.x) * 0.5f + 0.5f).i.f
            renderColorRectWithAlphaCheckerboard(Vec2(bb.min.x + gridStep, bb.min.y), bb.max, getColorU32(col), gridStep,
                    Vec2(-gridStep, 0f), rounding, Corner.TopRight or Corner.BottomRight)
            window.drawList.addRectFilled(bb.min, Vec2(midX, bb.max.y), getColorU32(colWithoutAlpha), rounding,
                    Corner.TopLeft or Corner.BottomLeft)
        } else {
            val c = getColorU32(if (flags has ColorEditFlags.AlphaPreview) col else colWithoutAlpha)
            renderColorRectWithAlphaCheckerboard(bb.min, bb.max, c, gridStep, Vec2(), rounding)
        }
        if (window.flags has WindowFlags.ShowBorders)
            renderFrameBorder(bb.min, bb.max, rounding)
        else
            window.drawList.addRect(bb.min, bb.max, Col.FrameBg.u32, rounding)  // Color button are often in need of some sort of border

        if (hovered && flags hasnt ColorEditFlags.NoTooltip) {
            val pF = floatArrayOf(col.x)
            colorTooltip(descId, pF, flags and (ColorEditFlags.NoAlpha or ColorEditFlags.AlphaPreview or ColorEditFlags.AlphaPreviewHalf))
            col.x = pF[0]
        }

        return pressed
    }

    /** initialize current options (generally on application startup) if you want to select a default format, picker
     *  type, etc. User will be able to change many settings, unless you pass the _NoOptions flag to your calls.    */
    fun setColorEditOptions(flags: Int) {
        var flags = flags
        if (flags hasnt ColorEditFlags._InputsMask)
            flags = flags or (ColorEditFlags._OptionsDefault and ColorEditFlags._InputsMask)
        if (flags hasnt ColorEditFlags._DataTypeMask)
            flags = flags or (ColorEditFlags._OptionsDefault and ColorEditFlags._DataTypeMask)
        if (flags hasnt ColorEditFlags._PickerMask)
            flags = flags or (ColorEditFlags._OptionsDefault and ColorEditFlags._PickerMask)
        assert((flags and ColorEditFlags._InputsMask).isPowerOfTwo)     // Check only 1 option is selected
        assert((flags and ColorEditFlags._DataTypeMask).isPowerOfTwo)   // Check only 1 option is selected
        assert((flags and ColorEditFlags._PickerMask).isPowerOfTwo)     // Check only 1 option is selected
        g.colorEditOptions = flags
    }

    /** 3-4 components color edition. Click on colored squared to open a color picker, right-click for options.
     *  Hint: 'float col[3]' function argument is same as 'float* col'.
     *  You can pass address of first element out of a contiguous set, e.g. &myvector.x */
    fun colorEdit3(label: String, col: FloatArray, flags: Int = 0) = colorEdit4(label, col, flags or ColorEditFlags.NoAlpha)

    /** Edit colors components (each component in 0.0f..1.0f range).
     *  See enum ImGuiColorEditFlags_ for available options. e.g. Only access 3 floats if ColorEditFlags.NoAlpha flag is set.
     *  With typical options: Left-click on colored square to open color picker. Right-click to open option menu.
     *  CTRL-Click over input fields to edit them and TAB to go to next item.   */
    fun colorEdit4(label: String, col: FloatArray, flags: Int = 0): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val storageId = window.id   // Store options on a per window basis
        val wExtra = if (flags has ColorEditFlags.NoSmallPreview) 0f else colorSquareSize + style.itemInnerSpacing.x
        val wItemsAll = calcItemWidth() - wExtra
        val labelDisplayEnd = findRenderedTextEnd(label)

        val alpha = flags hasnt ColorEditFlags.NoAlpha
        val hdr = flags has ColorEditFlags.HDR
        val components = if (alpha) 4 else 3
        val flagsUntouched = flags

        beginGroup()
        pushId(label)

        var flags = flags

        // If we're not showing any slider there's no point in doing any HSV conversions
        if (flags has ColorEditFlags.NoInputs)
            flags = (flags wo ColorEditFlags._InputsMask) or ColorEditFlags.RGB or ColorEditFlags.NoOptions

        // Context menu: display and modify options (before defaults are applied)
        if (flags hasnt ColorEditFlags.NoOptions)
            colorEditOptionsPopup(flags)

        // Read stored options
        if (flags hasnt ColorEditFlags._InputsMask)
            flags = flags or (g.colorEditOptions and ColorEditFlags._InputsMask)
        if (flags hasnt ColorEditFlags._DataTypeMask)
            flags = flags or (g.colorEditOptions and ColorEditFlags._DataTypeMask)
        if (flags hasnt ColorEditFlags._PickerMask)
            flags = flags or (g.colorEditOptions and ColorEditFlags._PickerMask)
        flags = flags or (g.colorEditOptions wo (ColorEditFlags._InputsMask or ColorEditFlags._DataTypeMask or ColorEditFlags._PickerMask))

        // Convert to the formats we need
        val f = floatArrayOf(col[0], col[1], col[2], if (alpha) col[3] else 1f)
        if (flags has ColorEditFlags.HSV)
            f.rgbToHSV()

        val i = IntArray(4, { F32_TO_INT8_UNBOUND(f[it]) })

        var valueChanged = false
        var valueChangedAsFloat = false

        if (flags has (ColorEditFlags.RGB or ColorEditFlags.HSV) && flags hasnt ColorEditFlags.NoInputs) {

            // RGB/HSV 0..255 Sliders
            val wItemOne = glm.max(1f, ((wItemsAll - style.itemInnerSpacing.x * (components - 1)) / components).i.f)
            val wItemLast = glm.max(1f, (wItemsAll - (wItemOne + style.itemInnerSpacing.x) * (components - 1)).i.f)

            val hidePrefix = wItemOne <= calcTextSize("M:999").x
            val ids = arrayOf("##X", "##Y", "##Z", "##W")
            val fmtTableInt = arrayOf(
                    arrayOf("%3.0f", "%3.0f", "%3.0f", "%3.0f"),             // Short display
                    arrayOf("R:%3.0f", "G:%3.0f", "B:%3.0f", "A:%3.0f"),     // Long display for RGBA
                    arrayOf("H:%3.0f", "S:%3.0f", "V:%3.0f", "A:%3.0f"))     // Long display for HSVA
            val fmtTableFloat = arrayOf(
                    arrayOf("%0.3f", "%0.3f", "%0.3f", "%0.3f"), // Short display
                    arrayOf("R:%0.3f", "G:%0.3f", "B:%0.3f", "A:%0.3f"), // Long display for RGBA
                    arrayOf("H:%0.3f", "S:%0.3f", "V:%0.3f", "A:%0.3f"))  // Long display for HSVA
            val fmtIdx = if (hidePrefix) 0 else if (flags has ColorEditFlags.HSV) 2 else 1

            pushItemWidth(wItemOne)
            for (n in 0 until components) {
                if (n > 0)
                    sameLine(0f, style.itemInnerSpacing.x)
                if (n + 1 == components)
                    pushItemWidth(wItemLast)
                val int = intArrayOf(i[n])
                if (flags has ColorEditFlags.Float) {
                    valueChangedAsFloat = valueChangedAsFloat or dragFloat(ids[n], f, n, 1f / 255f, 0f, if (hdr) 0f else 1f, fmtTableFloat[fmtIdx][n])
                    valueChanged = valueChanged or valueChangedAsFloat
                } else
                    valueChanged = valueChanged or dragInt(ids[n], i, n, 1f, 0, if (hdr) 0 else 255, fmtTableInt[fmtIdx][n])
            }
            popItemWidth()
            popItemWidth()

        } else if (flags has ColorEditFlags.HEX && flags hasnt ColorEditFlags.NoInputs) {
            // RGB Hexadecimal Input
            val buf = CharArray(64)
            (if (alpha)
                "#%02X%02X%02X%02X".format(style.locale, glm.clamp(i[0], 0, 255), glm.clamp(i[1], 0, 255), glm.clamp(i[2], 0, 255), glm.clamp(i[3], 0, 255))
            else
                "#%02X%02X%02X".format(style.locale, glm.clamp(i[0], 0, 255), glm.clamp(i[1], 0, 255), glm.clamp(i[2], 0, 255))).toCharArray(buf)
            pushItemWidth(wItemsAll)
            if (inputText("##Text", buf, InputTextFlags.CharsHexadecimal or InputTextFlags.CharsUppercase)) {
                valueChanged = valueChanged || true
                var p = 0
                while (buf[p] == '#' || buf[p].isSpace)
                    p++
                i.fill(0)
                String(buf, p, buf.strlen - p).scanHex(i, if (alpha) 4 else 3, 2)   // Treat at unsigned (%X is unsigned)
            }
            popItemWidth()
        }

        var pickerActive = false
        if (flags hasnt ColorEditFlags.NoSmallPreview) {
            if (flags hasnt ColorEditFlags.NoInputs)
                sameLine(0f, style.itemInnerSpacing.x)

            val colVec4 = Vec4(col[0], col[1], col[2], if (alpha) col[3] else 1f) // 1.0f
            if (colorButton("##ColorButton", colVec4, flags)) {
                if (flags hasnt ColorEditFlags.NoPicker) {
                    // Store current color and open a picker
                    g.colorPickerRef put colVec4
                    openPopup("picker")
                    setNextWindowPos(window.dc.lastItemRect.bl + Vec2(-1, style.itemSpacing.y))
                }
            } else if (flags hasnt ColorEditFlags.NoOptions && isItemRectHovered() && isMouseClicked(1))
                openPopup("context")

            if (beginPopup("picker")) {
                pickerActive = true
                if (0 != labelDisplayEnd) {
                    textUnformatted(label, labelDisplayEnd)
                    separator()
                }
                val squareSz = colorSquareSize
                val pickerFlagsToForward = ColorEditFlags.Float or ColorEditFlags.HDR or ColorEditFlags.NoAlpha or ColorEditFlags.AlphaBar    // | ImGuiColorEditFlags_AlphaPreview | ImGuiColorEditFlags_AlphaPreviewHalf;
                val pickerFlags = (flags and pickerFlagsToForward) or (ColorEditFlags.RGB or ColorEditFlags.HSV or ColorEditFlags.HEX) or
                        ColorEditFlags.NoLabel or ColorEditFlags.AlphaPreviewHalf
                pushItemWidth(squareSz * 12f)   // Use 256 + bar sizes?
                val pF = floatArrayOf(g.colorPickerRef.x)
                valueChanged = valueChanged or colorPicker4("##picker", col, pickerFlags, pF)
                g.colorPickerRef.x = pF[0]
                popItemWidth()
                endPopup()
            }
        }

        if (0 != labelDisplayEnd && flags hasnt ColorEditFlags.NoLabel) {
            sameLine(0f, style.itemInnerSpacing.x)
            textUnformatted(label, labelDisplayEnd)
        }

        // Convert back
        if (!pickerActive) {
            if (!valueChangedAsFloat)
                for (n in 0..3)
                    f[n] = i[n] / 255f
            if (flags has ColorEditFlags.HSV)
                f.hsvToRGB()
            if (valueChanged) {
                col[0] = f[0]
                col[1] = f[1]
                col[2] = f[2]
                if (alpha)
                    col[3] = f[3]
            }
        }

        popId()
        endGroup()

        return valueChanged
    }

    fun colorEditVec4(label: String, col: Vec4, flags: Int = 0): Boolean {
        val col4 = floatArrayOf(col.x, col.y, col.z, col.w)
        val valueChanged = colorEdit4(label, col4, flags)
        col.x = col4[0]
        col.y = col4[1]
        col.z = col4[2]
        col.w = col4[3]
        return valueChanged
    }

    fun colorPicker3(label: String, col: FloatArray, flags: Int = 0): Boolean {
        val col4 = floatArrayOf(*col, 1f)
        if (!colorPicker4(label, col4, flags or ColorEditFlags.NoAlpha)) return false
        col[0] = col4[0]; col[1] = col4[1]; col[2] = col4[2]
        return true
    }

    /** ColorPicker
     *  Note: only access 3 floats if ImGuiColorEditFlags_NoAlpha flag is set.
     *  FIXME: we adjust the big color square height based on item width, which may cause a flickering feedback loop
     *  (if automatic height makes a vertical scrollbar appears, affecting automatic width..)   */
    fun colorPicker4(label: String, col: FloatArray, flags: Int = 0, refCol: FloatArray? = null): Boolean {

        val window = currentWindow
        val drawList = window.drawList

        pushId(label)
        beginGroup()

        var flags = flags
        if (flags hasnt ColorEditFlags.NoSidePreview)
            flags = flags or ColorEditFlags.NoSmallPreview

        // Context menu: display and store options.
        if (flags hasnt ColorEditFlags.NoOptions)
            colorPickerOptionsPopup(flags, col)

        // Read stored options
        if (flags hasnt ColorEditFlags._PickerMask)
            flags = flags or (if (g.colorEditOptions has ColorEditFlags._PickerMask) g.colorEditOptions else ColorEditFlags._OptionsDefault.i) and ColorEditFlags._PickerMask
        assert((flags and ColorEditFlags._PickerMask).isPowerOfTwo) // Check that only 1 is selected
        if (flags hasnt ColorEditFlags.NoOptions)
            flags = flags or (g.colorEditOptions and ColorEditFlags.AlphaBar)

        // Setup
        val alphaBar = flags has ColorEditFlags.AlphaBar && flags hasnt ColorEditFlags.NoAlpha
        val pickerPos = window.dc.cursorPos // consume only, safe passing reference
        val barsWidth = colorSquareSize     // Arbitrary smallish width of Hue/Alpha picking bars TODO check
        // Saturation/Value picking box
        val svPickerSize = glm.max(barsWidth * 1, calcItemWidth() - (if (alphaBar) 2 else 1) * (barsWidth + style.itemInnerSpacing.x))
        val bar0PosX = pickerPos.x + svPickerSize + style.itemInnerSpacing.x
        val bar1PosX = bar0PosX + barsWidth + style.itemInnerSpacing.x
        val barsTrianglesHalfSz = (barsWidth * 0.2f).i.f

        val wheelThickness = svPickerSize * 0.08f
        val wheelROuter = svPickerSize * 0.50f
        val wheelRInner = wheelROuter - wheelThickness
        val wheelCenter = Vec2(pickerPos.x + (svPickerSize + barsWidth) * 0.5f, pickerPos.y + svPickerSize * 0.5f)

        // Note: the triangle is displayed rotated with trianglePa pointing to Hue, but most coordinates stays unrotated for logic.
        val triangleR = wheelRInner - (svPickerSize * 0.027f).i
        val trianglePa = Vec2(triangleR, 0f)   // Hue point.
        val trianglePb = Vec2(triangleR * -0.5f, triangleR * -0.866025f) // Black point.
        val trianglePc = Vec2(triangleR * -0.5f, triangleR * +0.866025f) // White point.

        var (h, s, v) = colorConvertRGBtoHSV(col)

        var valueChanged = false
        var valueChangedH = false
        var valueChangedSv = false

        if (flags has ColorEditFlags.PickerHueWheel) {
            // Hue wheel + SV triangle logic
            invisibleButton("hsv", Vec2(svPickerSize + style.itemInnerSpacing.x + barsWidth, svPickerSize))
            if (isItemActive) {
                val initialOff = IO.mouseClickedPos[0] - wheelCenter
                val currentOff = IO.mousePos - wheelCenter
                val initialDist2 = initialOff.lengthSqr
                if (initialDist2 >= (wheelRInner - 1) * (wheelRInner - 1) && initialDist2 <= (wheelROuter + 1) * (wheelROuter + 1)) {
                    // Interactive with Hue wheel
                    h = glm.atan(currentOff.y, currentOff.x) / glm.PIf * 0.5f
                    if (h < 0f)
                        h += 1f
                    valueChanged = true
                    valueChangedH = true
                }
                val cosHueAngle = glm.cos(-h * 2f * glm.PIf)
                val sinHueAngle = glm.sin(-h * 2f * glm.PIf)
                if (triangleContainsPoint(trianglePa, trianglePb, trianglePc, initialOff.rotate_(cosHueAngle, sinHueAngle))) { // TODO check
                    // Interacting with SV triangle
                    val currentOffUnrotated = currentOff.rotate_(cosHueAngle, sinHueAngle)
                    if (!triangleContainsPoint(trianglePa, trianglePb, trianglePc, currentOffUnrotated))
                        currentOffUnrotated put triangleClosestPoint(trianglePa, trianglePb, trianglePc, currentOffUnrotated)
                    val (uu, vv, ww) = triangleBarycentricCoords(trianglePa, trianglePb, trianglePc, currentOffUnrotated)
                    v = glm.clamp(1f - vv, 0.0001f, 1f)
                    s = glm.clamp(uu / v, 0.0001f, 1f)
                    valueChangedSv = true
                    valueChanged = true
                }
            }
            if (flags hasnt ColorEditFlags.NoOptions && isItemRectHovered() && isMouseClicked(1))
                openPopup("context")

        } else if (flags has ColorEditFlags.PickerHueBar) {
            // SV rectangle logic
            invisibleButton("sv", Vec2(svPickerSize))
            if (isItemActive) {
                s = saturate((IO.mousePos.x - pickerPos.x) / (svPickerSize - 1))
                v = 1f - saturate((IO.mousePos.y - pickerPos.y) / (svPickerSize - 1))
                valueChangedSv = true
                valueChanged = true
            }
            if (flags hasnt ColorEditFlags.NoOptions && isItemRectHovered() && isMouseClicked(1))
                openPopup("context")
            // Hue bar logic
            cursorScreenPos.put(bar0PosX, pickerPos.y)
            invisibleButton("hue", Vec2(barsWidth, svPickerSize))
            if (isItemActive) {
                h = saturate((IO.mousePos.y - pickerPos.y) / (svPickerSize - 1))
                valueChangedH = true
                valueChanged = true
            }
        }

        // Alpha bar logic
        if (alphaBar) {
            cursorScreenPos = Vec2(bar1PosX, pickerPos.y)
            invisibleButton("alpha", Vec2(barsWidth, svPickerSize))
            if (isItemActive) {
                col[3] = 1f - saturate((IO.mousePos.y - pickerPos.y) / (svPickerSize - 1))
                valueChanged = true
            }
        }

        if (flags hasnt ColorEditFlags.NoSidePreview) {
            sameLine(0f, style.itemInnerSpacing.x)
            beginGroup()
        }

        if (flags hasnt ColorEditFlags.NoLabel) {
            val labelDisplayEnd = findRenderedTextEnd(label)
            if (0 != labelDisplayEnd) {
                if (flags has ColorEditFlags.NoSidePreview)
                    sameLine(0f, style.itemInnerSpacing.x)
                textUnformatted(label, labelDisplayEnd)
            }
        }
        if (flags hasnt ColorEditFlags.NoSidePreview) {
            val colV4 = Vec4(col[0], col[1], col[2], if (flags has ColorEditFlags.NoAlpha) 1f else col[3])
            val squareSz = colorSquareSize
            if (flags has ColorEditFlags.NoLabel)
                text("Current")
            val f = flags and (ColorEditFlags.HDR or ColorEditFlags.AlphaPreview or ColorEditFlags.AlphaPreviewHalf or ColorEditFlags.NoTooltip)
            colorButton("##current", colV4, f, Vec2(squareSz * 3, squareSz * 2))
            refCol?.let {
                text("Original")
                val refColV4 = Vec4(it[0], it[1], it[2], if (flags has ColorEditFlags.NoAlpha) 1f else it[3])
                if (colorButton("##original", refColV4, f, Vec2(squareSz * 3, squareSz * 2))) {
                    for (i in 0..2) col[i] = it[i]
                    if (flags hasnt ColorEditFlags.NoAlpha) col[3] = it[3]
                    valueChanged = true
                }
            }
            endGroup()
        }

        // Convert back color to RGB
        if (valueChangedH || valueChangedSv)
            colorConvertHSVtoRGB(if (h >= 1f) h - 10 * 1e-6f else h, if (s > 0f) s else 10 * 1e-6f, if (v > 0f) v else 1e-6f, col)

        // R,G,B and H,S,V slider color editor
        if (flags hasnt ColorEditFlags.NoInputs) {
            pushItemWidth((if (alphaBar) bar1PosX else bar0PosX) + barsWidth - pickerPos.x)
            val subFlagsToForward = ColorEditFlags._DataTypeMask or ColorEditFlags.HDR or ColorEditFlags.NoAlpha or ColorEditFlags.NoOptions or
                    ColorEditFlags.NoSmallPreview or ColorEditFlags.AlphaPreview or ColorEditFlags.AlphaPreviewHalf
            var subFlags = (flags and subFlagsToForward) or ColorEditFlags.NoPicker
            if (flags has ColorEditFlags.RGB || flags hasnt ColorEditFlags._InputsMask)
                valueChanged = valueChanged or colorEdit4("##rgb", col, subFlags or ColorEditFlags.RGB)
            if (flags has ColorEditFlags.HSV || flags hasnt ColorEditFlags._InputsMask)
                valueChanged = valueChanged or colorEdit4("##hsv", col, subFlags or ColorEditFlags.HSV)
            if (flags has ColorEditFlags.HEX || flags hasnt ColorEditFlags._InputsMask)
                valueChanged = valueChanged or colorEdit4("##hex", col, subFlags or ColorEditFlags.HEX)
            popItemWidth()
        }

        // Try to cancel hue wrap (after ColorEdit), if any
        if (valueChanged) {
            val (newH, newS, newV) = colorConvertRGBtoHSV(col)
            if (newH <= 0 && h > 0) {
                if (newV <= 0 && v != newV)
                    colorConvertHSVtoRGB(h, s, if (newV <= 0) v * 0.5f else newV, col)
                else if (newS <= 0)
                    colorConvertHSVtoRGB(h, if (newS <= 0) s * 0.5f else newS, newV, col)
            }
        }

        val hueColorF = Vec4(1)
        colorConvertHSVtoRGB(h, 1f, 1f).apply { hueColorF.x = this[0]; hueColorF.y = this[1]; hueColorF.z = this[0] }
        val hueColor32 = hueColorF.u32
        val col32NoAlpha = Vec4(col[0], col[1], col[2], 1f).u32

        val hueColors = arrayOf(COL32(255, 0, 0, 255), COL32(255, 255, 0, 255), COL32(0, 255, 0, 255),
                COL32(0, 255, 255, 255), COL32(0, 0, 255, 255), COL32(255, 0, 255, 255), COL32(255, 0, 0, 255))
        val svCursorPos = Vec2()

        if (flags has ColorEditFlags.PickerHueWheel) {
            // Render Hue Wheel
            val aeps = 1.5f / wheelROuter   // Half a pixel arc length in radians (2pi cancels out).
            val segmentPerArc = glm.max(4, (wheelROuter / 12).i)
            for (n in 0..5) {
                val a0 = n / 6f * 2f * glm.PIf - aeps
                val a1 = (n + 1f) / 6f * 2f * glm.PIf + aeps
                val vertStartIdx = drawList._vtxCurrentIdx
                drawList.pathArcTo(wheelCenter, (wheelRInner + wheelROuter) * 0.5f, a0, a1, segmentPerArc)
                drawList.pathStroke(COL32_WHITE, false, wheelThickness)

                // Paint colors over existing vertices
                val gradientP0 = Vec2(wheelCenter.x + a0.cos * wheelRInner, wheelCenter.y + a0.sin * wheelRInner)
                val gradientP1 = Vec2(wheelCenter.x + a1.cos * wheelRInner, wheelCenter.y + a1.sin * wheelRInner)
                paintVertsLinearGradientKeepAlpha(drawList, drawList._vtxWritePtr - (drawList._vtxCurrentIdx - vertStartIdx),
                        drawList._vtxWritePtr, gradientP0, gradientP1, hueColors[n], hueColors[n + 1])
            }

            // Render Cursor + preview on Hue Wheel
            val cosHueAngle = glm.cos(h * 2f * glm.PIf)
            val sinHueAngle = glm.sin(h * 2f * glm.PIf)
            val hueCursorPos = Vec2(wheelCenter.x + cosHueAngle * (wheelRInner + wheelROuter) * 0.5f,
                    wheelCenter.y + sinHueAngle * (wheelRInner + wheelROuter) * 0.5f)
            val hueCursorRad = wheelThickness * if (valueChangedH) 0.65f else 0.55f
            val hueCursorSegments = glm.clamp((hueCursorRad / 1.4f).i, 9, 32)
            drawList.addCircleFilled(hueCursorPos, hueCursorRad, hueColor32, hueCursorSegments)
            drawList.addCircle(hueCursorPos, hueCursorRad + 1, COL32(128, 128, 128, 255), hueCursorSegments)
            drawList.addCircle(hueCursorPos, hueCursorRad, COL32_WHITE, hueCursorSegments)

            // Render SV triangle (rotated according to hue)
            val tra = wheelCenter + trianglePa.rotate(cosHueAngle, sinHueAngle)
            val trb = wheelCenter + trianglePb.rotate(cosHueAngle, sinHueAngle)
            val trc = wheelCenter + trianglePc.rotate(cosHueAngle, sinHueAngle)
            val uvWhite = g.fontTexUvWhitePixel
            drawList.primReserve(6, 6)
            drawList.primVtx(tra, uvWhite, hueColor32)
            drawList.primVtx(trb, uvWhite, hueColor32)
            drawList.primVtx(trc, uvWhite, COL32_WHITE)
            drawList.primVtx(tra, uvWhite, COL32_BLACK_TRANS)
            drawList.primVtx(trb, uvWhite, COL32_BLACK)
            drawList.primVtx(trc, uvWhite, COL32_BLACK_TRANS)
            drawList.addTriangle(tra, trb, trc, COL32(128, 128, 128, 255), 1.5f)
            svCursorPos put trc.lerp(tra, saturate(s)).lerp(trb, saturate(1 - v))
        } else if (flags has ColorEditFlags.PickerHueBar) {
            // Render SV Square
            drawList.addRectFilledMultiColor(pickerPos, pickerPos + svPickerSize, COL32_WHITE, hueColor32, hueColor32, COL32_WHITE)
            drawList.addRectFilledMultiColor(pickerPos, pickerPos + svPickerSize, COL32_BLACK_TRANS, COL32_BLACK_TRANS, COL32_BLACK, COL32_BLACK)
            renderFrameBorder(pickerPos, pickerPos + svPickerSize, 0f)
            // Sneakily prevent the circle to stick out too much
            svCursorPos.x = glm.clamp((pickerPos.x + saturate(s) * svPickerSize + 0.5f).i.f, pickerPos.x + 2, pickerPos.x + svPickerSize - 2)
            svCursorPos.y = glm.clamp((pickerPos.y + saturate(1 - v) * svPickerSize + 0.5f).i.f, pickerPos.y + 2, pickerPos.y + svPickerSize - 2)

            // Render Hue Bar
            for (i in 0..5) {
                val a = Vec2(bar0PosX, pickerPos.y + i * (svPickerSize / 6))
                val c = Vec2(bar0PosX + barsWidth, pickerPos.y + (i + 1) * (svPickerSize / 6))
                drawList.addRectFilledMultiColor(a, c, hueColors[i], hueColors[i], hueColors[i + 1], hueColors[i + 1])
            }
            val bar0LineY = (pickerPos.y + h * svPickerSize + 0.5f).i.f
            renderFrameBorder(Vec2(bar0PosX, pickerPos.y), Vec2(bar0PosX + barsWidth, pickerPos.y + svPickerSize), 0f)
            renderArrowsForVerticalBar(drawList, Vec2(bar0PosX - 1, bar0LineY), Vec2(barsTrianglesHalfSz + 1, barsTrianglesHalfSz), barsWidth + 2f)
        }

        // Render cursor/preview circle (clamp S/V within 0..1 range because floating points colors may lead HSV values to be out of range)
        val svCursorRad = if (valueChangedSv) 10f else 6f
        drawList.addCircleFilled(svCursorPos, svCursorRad, col32NoAlpha, 12)
        drawList.addCircle(svCursorPos, svCursorRad + 1, COL32(128, 128, 128, 255), 12)
        drawList.addCircle(svCursorPos, svCursorRad, COL32_WHITE, 12)

        // Render alpha bar
        if (alphaBar) {
            val alpha = saturate(col[3])
            val bar1Bb = Rect(bar1PosX, pickerPos.y, bar1PosX + barsWidth, pickerPos.y + svPickerSize)
            renderColorRectWithAlphaCheckerboard(bar1Bb.min, bar1Bb.max, COL32(0, 0, 0, 0), bar1Bb.width / 2f, Vec2())
            drawList.addRectFilledMultiColor(bar1Bb.min, bar1Bb.max, col32NoAlpha, col32NoAlpha, col32NoAlpha wo COL32_A_MASK, col32NoAlpha wo COL32_A_MASK)
            val bar1LineY = (pickerPos.y + (1f - alpha) * svPickerSize + 0.5f)
            renderFrameBorder(bar1Bb.min, bar1Bb.max, 0f)
            renderArrowsForVerticalBar(drawList, Vec2(bar1PosX - 1, bar1LineY), Vec2(barsTrianglesHalfSz + 1, barsTrianglesHalfSz), barsWidth + 2f)
        }

        endGroup()
        popId()

        return valueChanged
    }

//    IMGUI_API void          PlotLines(const char* label, const float* values, int values_count, int values_offset = 0, const char* overlay_text = NULL, float scale_min = FLT_MAX, float scale_max = FLT_MAX, ImVec2 graph_size = ImVec2(0,0), int stride = sizeof(float));
//    IMGUI_API void          PlotLines(const char* label, float (*values_getter)(void* data, int idx), void* data, int values_count, int values_offset = 0, const char* overlay_text = NULL, float scale_min = FLT_MAX, float scale_max = FLT_MAX, ImVec2 graph_size = ImVec2(0,0));
//    IMGUI_API void          PlotHistogram(const char* label, const float* values, int values_count, int values_offset = 0, const char* overlay_text = NULL, float scale_min = FLT_MAX, float scale_max = FLT_MAX, ImVec2 graph_size = ImVec2(0,0), int stride = sizeof(float));
//    IMGUI_API void          PlotHistogram(const char* label, float (*values_getter)(void* data, int idx), void* data, int values_count, int values_offset = 0, const char* overlay_text = NULL, float scale_min = FLT_MAX, float scale_max = FLT_MAX, ImVec2 graph_size = ImVec2(0,0));
//    IMGUI_API void          ProgressBar(float fraction, const ImVec2& size_arg = ImVec2(-1,0), const char* overlay = NULL);

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

        fun paintVertsLinearGradientKeepAlpha(drawList: DrawList, vertStart: Int, vertEnd: Int, gradientP0: Vec2, gradientP1: Vec2, col0: Int, col1: Int) {
            val gradientExtent = gradientP1 - gradientP0
            val gradientInvLength = gradientExtent.invLength(0f)
            for (v in vertStart until vertEnd) {
                val vert = drawList.vtxBuffer[v]
                val d = (vert.pos - gradientP0) dot gradientExtent
                val t = glm.min(glm.sqrt(glm.max(d, 0f)) * gradientInvLength, 1f)
                val r = lerp((col0 ushr COL32_R_SHIFT) and 0xFF, (col1 ushr COL32_R_SHIFT) and 0xFF, t)
                val g = lerp((col0 ushr COL32_G_SHIFT) and 0xFF, (col1 ushr COL32_G_SHIFT) and 0xFF, t)
                val b = lerp((col0 ushr COL32_B_SHIFT) and 0xFF, (col1 ushr COL32_B_SHIFT) and 0xFF, t)
                vert.col = (r shl COL32_R_SHIFT) or (g shl COL32_G_SHIFT) or (b shl COL32_B_SHIFT) or (vert.col and COL32_A_MASK)
            }
        }

        fun colorEditOptionsPopup(flags: Int) {
            val allowOptInputs = flags hasnt ColorEditFlags._InputsMask
            val allowOptDatatype = flags hasnt ColorEditFlags._DataTypeMask
            if ((!allowOptInputs && !allowOptDatatype) || !beginPopup("context")) return
            var opts = g.colorEditOptions
            if (allowOptInputs) {
                if (radioButton("RGB", opts has ColorEditFlags.RGB))
                    opts = (opts wo ColorEditFlags._InputsMask) or ColorEditFlags.RGB
                if (radioButton("HSV", opts has ColorEditFlags.HSV))
                    opts = (opts wo ColorEditFlags._InputsMask) or ColorEditFlags.HSV
                if (radioButton("HEX", opts has ColorEditFlags.HEX))
                    opts = (opts wo ColorEditFlags._InputsMask) or ColorEditFlags.HEX
            }
            if (allowOptDatatype) {
                if (allowOptInputs) separator()
                if (radioButton("0..255", opts has ColorEditFlags.Uint8))
                    opts = (opts wo ColorEditFlags._DataTypeMask) or ColorEditFlags.Uint8
                if (radioButton("0.00..1.00", opts has ColorEditFlags.Float))
                    opts = (opts wo ColorEditFlags._DataTypeMask) or ColorEditFlags.Float
            }
            g.colorEditOptions = opts
            endPopup()
        }

        fun colorPickerOptionsPopup(flags: Int, refCol: FloatArray) {
            val allowOptPicker = flags hasnt ColorEditFlags._PickerMask
            val allowOptAlphaBar = flags hasnt ColorEditFlags.NoAlpha && flags hasnt ColorEditFlags.AlphaBar
            if ((!allowOptPicker && !allowOptAlphaBar) || !beginPopup("context")) return
            if (allowOptPicker) {
                // FIXME: Picker size copied from main picker function
                val pickerSize = Vec2(g.fontSize * 8, glm.max(g.fontSize * 8 - (colorSquareSize + style.itemInnerSpacing.x), 1f))
                pushItemWidth(pickerSize.x)
                for (pickerType in 0..1) {
                    // Draw small/thumbnail version of each picker type (over an invisible button for selection)
                    if (pickerType > 0) separator()
                    pushId(pickerType)
                    var pickerFlags = ColorEditFlags.NoInputs or ColorEditFlags.NoOptions or ColorEditFlags.NoLabel or
                            ColorEditFlags.NoSidePreview or (flags and ColorEditFlags.NoAlpha)
                    if (pickerType == 0) pickerFlags = pickerFlags or ColorEditFlags.PickerHueBar
                    if (pickerType == 1) pickerFlags = pickerFlags or ColorEditFlags.PickerHueWheel
                    val backupPos = Vec2(cursorScreenPos)
                    if (selectable("##selectable", false, 0, pickerSize)) // By default, Selectable() is closing popup
                        g.colorEditOptions = (g.colorEditOptions wo ColorEditFlags._PickerMask) or (pickerFlags and ColorEditFlags._PickerMask)
                    cursorScreenPos = backupPos
                    val dummyRefCol = Vec4()
                    for (i in 0..2) dummyRefCol[i] = refCol[i]
                    if (pickerFlags hasnt ColorEditFlags.NoAlpha) dummyRefCol[3] = refCol[3]
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
                checkboxFlags("Alpha Bar", pI, ColorEditFlags.AlphaBar.i)
                g.colorEditOptions = pI[0]
            }
            endPopup()
        }
    }
}