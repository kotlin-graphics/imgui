package imgui.imgui

import gli.has
import gli.hasnt
import glm_.f
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
import imgui.ImGui.clearActiveId
import imgui.ImGui.colorConvertFloat4ToU32
import imgui.ImGui.colorConvertHSVtoRGB
import imgui.ImGui.colorConvertRGBtoHSV
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
import imgui.ImGui.isItemHovered
import imgui.ImGui.isMouseClicked
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.menuItem
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

//    IMGUI_API bool          CheckboxFlags(const char* label, unsigned int* flags, unsigned int flags_value);

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
        var popupOpenedNow = false

        val valueBb = Rect(frameBb.min, frameBb.max - Vec2(arrowSize, 0f))
        renderFrame(frameBb.min, frameBb.max, Col.FrameBg.u32, true, style.frameRounding)
        val col = if (popupOpen || hovered) Col.ButtonHovered else Col.Button
        renderFrame(Vec2(frameBb.max.x - arrowSize, frameBb.min.y), frameBb.max, col.u32, true, style.frameRounding) // FIXME-ROUNDING
        renderCollapseTriangle(Vec2(frameBb.max.x - arrowSize, frameBb.min.y) + style.framePadding, true)

        if (currentItem[0] in 0 until itemsCount)
            itemsGetter(data, currentItem[0])?.let { renderTextClipped(frameBb.min + style.framePadding, valueBb.max, it) }

        if (labelSize.x > 0)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, frameBb.min.y + style.framePadding.y), label)

        if (hovered) {
            setHoveredId(id)
            if (IO.mouseClicked[0]) {
                clearActiveId()
                if (isPopupOpen(id)) closePopup(id)
                else {
                    focusWindow(window)
                    openPopup(label)
                    popupOpen = true
                    popupOpenedNow = true
                }
            }
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
            if (beginPopupEx(label, flags)) {
                // Display items
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
                    if (itemSelected && popupOpenedNow) setScrollHere()
                    popId()
                }
                endPopup()
            }
            popStyleVar()
        }
        return valueChanged
    }

    /**  display a colored square/button, hover for details, return true when pressed.
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

        val colWithoutAlpha = Vec4(col.x, col.y, col.z, 1f)
        val gridStep = glm.min(size.x, size.y) / 2f
        val rounding = glm.min(style.frameRounding, gridStep * 0.5f)
        if (flags has ColorEditFlags.HalfAlphaPreview && flags hasnt (ColorEditFlags.NoAlpha or ColorEditFlags.NoAlphaPreview)
                && col.w < 1f) {
            val midX = ((bb.min.x + bb.max.x) * 0.5f + 0.5f).i.f
            renderColorRectWithAlphaCheckerboard(Vec2(bb.min.x + gridStep, bb.min.y), bb.max, getColorU32(col), gridStep,
                    Vec2(-gridStep, 0f), rounding, Corner.TopRight or Corner.BottomRight)
            window.drawList.addRectFilled(bb.min, Vec2(midX, bb.max.y), getColorU32(colWithoutAlpha), rounding,
                    Corner.TopLeft or Corner.BottomLeft)
        } else {
            val c = if (flags has (ColorEditFlags.NoAlpha or ColorEditFlags.NoAlphaPreview)) colWithoutAlpha else col
            renderColorRectWithAlphaCheckerboard(bb.min, bb.max, getColorU32(c), gridStep, Vec2(), rounding)
        }
        renderFrameBorder(bb.min, bb.max, rounding)

        if (hovered && flags hasnt ColorEditFlags.NoTooltip) {
            val pF = floatArrayOf(col.x)
            colorTooltip(descId, pF, flags and (ColorEditFlags.NoAlpha or ColorEditFlags.NoAlphaPreview or ColorEditFlags.HalfAlphaPreview))
            col.x = pF[0]
        }

        return pressed
    }

    /** 3-4 components color edition. Click on colored squared to open a color picker, right-click for options.
     *  Hint: 'float col[3]' function argument is same as 'float* col'.
     *  You can pass address of first element out of a contiguous set, e.g. &myvector.x */
    fun colorEdit3(label: String, col: FloatArray, flags: Int = 0) = colorEdit4(label, col, flags or ColorEditFlags.NoAlpha)

    /** Edit colors components (each component in 0.0f..1.0f range)
     *  Click on colored square to open a color picker (unless ImGuiColorEditFlags_NoPicker is set).
     *  // Note: only access 3 floats if ImGuiColorEditFlags_NoAlpha flag is set.   */
    fun colorEdit4(label: String, col: FloatArray, flags: Int = 0): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(label)
        val wFull = calcItemWidth()
        val wExtra = if (flags has ColorEditFlags.NoColorSquare) 0f else colorSquareSize + style.itemInnerSpacing.x
        val wItemsAll = wFull - wExtra

        val alpha = flags hasnt ColorEditFlags.NoAlpha
        val components = if (alpha) 4 else 3

        val flags = when {
        // If no mode is specified, defaults to RGB
            flags hasnt ColorEditFlags.ModeMask_ -> flags or ColorEditFlags.RGB
        // If we're not showing any slider there's no point in querying color mode, nor showing the options menu, nor doing any HSV conversions
            flags has ColorEditFlags.NoInputs -> (flags and ColorEditFlags.ModeMask_.inv()) or ColorEditFlags.RGB or ColorEditFlags.NoOptions
        // Read back edit mode from persistent storage
            flags hasnt ColorEditFlags.NoOptions -> (flags and ColorEditFlags.StoredMask_.inv()) or
                    ((g.colorEditModeStorage[id] ?: (flags and ColorEditFlags.StoredMask_)) and ColorEditFlags.StoredMask_)
            else -> flags
        }

        // Check that exactly one of RGB/HSV/HEX is set
        assert((flags and ColorEditFlags.ModeMask_).isPowerOfTwo)

        val f = floatArrayOf(col[0], col[1], col[2], if (alpha) col[3] else 1f)
        if (flags has ColorEditFlags.HSV)
            f.rgbToHSV()

        val i = IntArray(4, { F32_TO_INT8_UNBOUND(f[it]) })

        var valueChanged = false
        var valueChangedAsFloat = false

        beginGroup()
        pushId(label)

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
                    valueChangedAsFloat = valueChangedAsFloat or dragFloat(ids[n], f, n, 1f / 255f, 0f, 1f, fmtTableFloat[fmtIdx][n])
                    valueChanged = valueChanged or valueChangedAsFloat
                } else
                    valueChanged = valueChanged or dragInt(ids[n], i, n, 1f, 0, 255, fmtTableInt[fmtIdx][n])
            }
            popItemWidth()
            popItemWidth()

        } else if (flags has ColorEditFlags.HEX && flags hasnt ColorEditFlags.NoInputs) {
            // RGB Hexadecimal Input
            val buf = CharArray(64)
            (if (alpha)
                "#%02X%02X%02X%02X".format(style.locale, i[0], i[1], i[2], i[3])
            else
                "#%02X%02X%02X".format(style.locale, i[0], i[1], i[2])).toCharArray(buf)
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

        val labelDisplayEnd = findRenderedTextEnd(label)

        var pickerActive = false
        if (flags hasnt ColorEditFlags.NoColorSquare) {
            if (flags hasnt ColorEditFlags.NoInputs)
                sameLine(0f, style.itemInnerSpacing.x)

            val colDisplay = Vec4(col[0], col[1], col[2], if (alpha) col[3] else 1f) // 1.0f
            if (colorButton("##ColorButton", colDisplay, flags)) {
                if (flags hasnt ColorEditFlags.NoPicker) {
//                    g.colorPickerRef.put(col[0], col[1], col[2], if (alpha) col[3] else 1f)
                    openPopup("picker")
                    setNextWindowPos(window.dc.lastItemRect.bl + Vec2(-1, style.itemSpacing.y))
                }
            } else if (flags hasnt ColorEditFlags.NoOptions && isItemHovered() && isMouseClicked(1))
                openPopup("context")

            if (beginPopup("picker")) {
                pickerActive = true
                if (0 != labelDisplayEnd) {
                    textUnformatted(label, labelDisplayEnd)
                    separator()
                }
                val squareSz = colorSquareSize
                val pickerFlags = (flags and (ColorEditFlags.NoAlpha or ColorEditFlags.HalfAlphaPreview or ColorEditFlags.Float)) or
                        (ColorEditFlags.RGB or ColorEditFlags.HSV or ColorEditFlags.HEX) or ColorEditFlags.NoLabel
                pushItemWidth(squareSz * 12f)
                valueChanged = valueChanged or colorPicker4("##picker", col, pickerFlags)
                popItemWidth()
                endPopup()
            }
            if (flags hasnt ColorEditFlags.NoOptions && beginPopup("context")) {
                // FIXME-LOCALIZATION
                if (menuItem("Edit as RGB", "", flags has ColorEditFlags.RGB)) //TODO set bug
                    g.colorEditModeStorage.set(id, ColorEditFlags.RGB.i)
                if (menuItem("Edit as HSV", "", flags has ColorEditFlags.HSV))
                    g.colorEditModeStorage.set(id, ColorEditFlags.HSV.i)
                if (menuItem("Edit as Hexadecimal", "", flags has ColorEditFlags.HEX))
                    g.colorEditModeStorage.set(id, ColorEditFlags.HEX.i)
                endPopup()
            }

            // Recreate our own tooltip over's ColorButton() one because we want to display correct alpha here
            if (flags hasnt ColorEditFlags.NoTooltip && isItemHovered())
                colorTooltip(label, col, flags)
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
    fun colorPicker4(label: String, col: FloatArray, flags: Int = 0): Boolean {

        val window = currentWindow
        val drawList = window.drawList

        pushId(label)
        beginGroup()

        // Setup
        val alphaBar = flags has ColorEditFlags.AlphaBar && flags hasnt ColorEditFlags.NoAlpha
        val pickerPos = window.dc.cursorPos // consume only, safe passing reference
        val barsWidth = colorSquareSize     // Arbitrary smallish width of Hue/Alpha picking bars
        val barsLineExtrude = glm.min(2f, style.itemInnerSpacing.x * 0.5f)
        // Saturation/Value picking box
        val svPickerSize = glm.max(barsWidth * 1, calcItemWidth() - (if (alphaBar) 2 else 1) * (barsWidth + style.itemInnerSpacing.x))
        val bar0PosX = pickerPos.x + svPickerSize + style.itemInnerSpacing.x
        val bar1PosX = bar0PosX + barsWidth + style.itemInnerSpacing.x

        var (h, s, v) = colorConvertRGBtoHSV(col)

        // Color matrix logic
        var valueChanged = false
        var hsvChanged = false
        invisibleButton("sv", Vec2(svPickerSize))
        if (isItemActive) {
            s = saturate((IO.mousePos.x - pickerPos.x) / (svPickerSize - 1))
            v = 1f - saturate((IO.mousePos.y - pickerPos.y) / (svPickerSize - 1))
            valueChanged = true
            hsvChanged = true
        }

        // Hue bar logic
        cursorScreenPos = Vec2(bar0PosX, pickerPos.y)
        invisibleButton("hue", Vec2(barsWidth, svPickerSize))
        if (isItemActive) {
            h = saturate((IO.mousePos.y - pickerPos.y) / (svPickerSize - 1))
            valueChanged = true
            hsvChanged = true
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

        if (flags hasnt ColorEditFlags.NoLabel) {
            val labelDisplayEnd = findRenderedTextEnd(label)
            if (0 != labelDisplayEnd) {
                sameLine(0f, style.itemInnerSpacing.x)
                textUnformatted(label, labelDisplayEnd)
            }
        }

        // Convert back color to RGB
        if (hsvChanged)
            colorConvertHSVtoRGB(if (h >= 1f) h - 10 * 1e-6f else h, if (s > 0f) s else 10 * 1e-6f, if (v > 0f) v else 1e-6f, col)

        // R,G,B and H,S,V slider color editor
        var flags = flags
        if (flags hasnt ColorEditFlags.NoInputs) {
            if (flags hasnt ColorEditFlags.ModeMask_)
                flags = flags or ColorEditFlags.RGB or ColorEditFlags.HSV or ColorEditFlags.HEX
            pushItemWidth((if (alphaBar) bar1PosX else bar0PosX) + barsWidth - pickerPos.x)
            val subFlags = (flags and (ColorEditFlags.Float or ColorEditFlags.NoAlpha or ColorEditFlags.NoColorSquare)) or
                    ColorEditFlags.NoPicker or ColorEditFlags.NoOptions or ColorEditFlags.NoTooltip
            if (flags has ColorEditFlags.RGB)
                valueChanged = valueChanged or colorEdit4("##rgb", col, subFlags or ColorEditFlags.RGB)
            if (flags has ColorEditFlags.HSV)
                valueChanged = valueChanged or colorEdit4("##hsv", col, subFlags or ColorEditFlags.HSV)
            if (flags has ColorEditFlags.HEX)
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

        // Render hue bar
        val hueColorF = Vec4(1)
        val rgb = colorConvertHSVtoRGB(h, 1f, 1f)
        hueColorF.x = rgb[0]
        hueColorF.y = rgb[1]
        hueColorF.z = rgb[2]
        val hueColors = intArrayOf(COL32(255, 0, 0, 255), COL32(255, 255, 0, 255), COL32(0, 255, 0, 255),
                COL32(0, 255, 255, 255), COL32(0, 0, 255, 255), COL32(255, 0, 255, 255), COL32(255, 0, 0, 255))
        for (i in 0..5) {
            drawList.addRectFilledMultiColor(
                    Vec2(bar0PosX, pickerPos.y + i * (svPickerSize / 6)),
                    Vec2(bar0PosX + barsWidth, pickerPos.y + (i + 1) * (svPickerSize / 6)),
                    hueColors[i], hueColors[i], hueColors[i + 1], hueColors[i + 1])
        }
        val bar0LineY = (pickerPos.y + h * svPickerSize + 0.5f).i.f
        renderFrameBorder(Vec2(bar0PosX, pickerPos.y), Vec2(bar0PosX + barsWidth, pickerPos.y + svPickerSize), 0f)
        drawList.addLine(Vec2(bar0PosX - barsLineExtrude, bar0LineY), Vec2(bar0PosX + barsWidth + barsLineExtrude, bar0LineY), COL32_WHITE)

        // Render alpha bar
        if (alphaBar) {
            val alpha = saturate(col[3])
            val bar1LineY = (pickerPos.y + (1f - alpha) * svPickerSize + 0.5f).i.f
            val bar1Bb = Rect(bar1PosX, pickerPos.y, bar1PosX + barsWidth, pickerPos.y + svPickerSize)
            drawList.addRectFilledMultiColor(bar1Bb.min, bar1Bb.max, COL32_WHITE, COL32_WHITE, COL32_BLACK, COL32_BLACK)
            renderFrameBorder(bar1Bb.min, bar1Bb.max, 0f)
            drawList.addLine(Vec2(bar1PosX - barsLineExtrude, bar1LineY), Vec2(bar1PosX + barsWidth + barsLineExtrude, bar1LineY), COL32_WHITE)
        }

        // Render color matrix
        val hueColor32 = colorConvertFloat4ToU32(hueColorF)
        drawList.addRectFilledMultiColor(pickerPos, pickerPos + Vec2(svPickerSize), COL32_WHITE, hueColor32, hueColor32, COL32_WHITE)
        drawList.addRectFilledMultiColor(pickerPos, pickerPos + Vec2(svPickerSize), COL32_BLACK_TRANS, COL32_BLACK_TRANS, COL32_BLACK, COL32_BLACK)
        renderFrameBorder(pickerPos, pickerPos + Vec2(svPickerSize), 0f)

        // Render cross-hair (clamp S/V within 0..1 range because floating points colors may lead HSV values to be out of range)
        val CROSSHAIR_SIZE = 7f
        val p = Vec2((pickerPos.x + saturate(s) * svPickerSize + 0.5f).i.f, (pickerPos.y + saturate(1 - v) * svPickerSize + 0.5f).i.f)
        drawList.addLine(Vec2(p.x - CROSSHAIR_SIZE, p.y), Vec2(p.x - 2, p.y), COL32_WHITE)
        drawList.addLine(Vec2(p.x + CROSSHAIR_SIZE, p.y), Vec2(p.x + 2, p.y), COL32_WHITE)
        drawList.addLine(Vec2(p.x, p.y + CROSSHAIR_SIZE), Vec2(p.x, p.y + 2), COL32_WHITE)
        drawList.addLine(Vec2(p.x, p.y - CROSSHAIR_SIZE), Vec2(p.x, p.y - 2), COL32_WHITE)

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
}