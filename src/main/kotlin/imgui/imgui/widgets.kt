package imgui.imgui

import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.F32_TO_INT8_SAT
import imgui.ImGui.F32_TO_INT8_UNBOUND
import imgui.ImGui.beginGroup
import imgui.ImGui.buttonBehavior
import imgui.ImGui.buttonEx
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.calcWrapWidthForPos
import imgui.ImGui.clearActiveId
import imgui.ImGui.colorConvertHSVtoRGB
import imgui.ImGui.colorConvertRGBtoHSV
import imgui.ImGui.currentWindow
import imgui.ImGui.dragInt
import imgui.ImGui.endGroup
import imgui.ImGui.endPopup
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.focusWindow
import imgui.ImGui.getColorU32
import imgui.ImGui.inputText
import imgui.ImGui.isClippedEx
import imgui.ImGui.isHovered
import imgui.ImGui.isItemHovered
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
import imgui.ImGui.renderBullet
import imgui.ImGui.renderCollapseTriangle
import imgui.ImGui.renderFrame
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.renderTextWrapped
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.setHoveredId
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setScrollHere
import imgui.ImGui.setTooltip
import imgui.ImGui.spacing
import imgui.ImGui.textLineHeight
import imgui.internal.*
import imgui.Context as g
import imgui.Context.style

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
        text(fmt, args)
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

//    IMGUI_API bool          SmallButton(const char* label);                                         // button with FramePadding=(0,0)
//    IMGUI_API bool          InvisibleButton(const char* str_id, const ImVec2& size);
//    IMGUI_API void          Image(ImTextureID user_texture_id, const ImVec2& size, const ImVec2& uv0 = ImVec2(0,0), const ImVec2& uv1 = ImVec2(1,1), const ImVec4& tint_col = ImVec4(1,1,1,1), const ImVec4& border_col = ImVec4(0,0,0,0));
//    IMGUI_API bool          ImageButton(ImTextureID user_texture_id, const ImVec2& size, const ImVec2& uv0 = ImVec2(0,0),  const ImVec2& uv1 = ImVec2(1,1), int frame_padding = -1, const ImVec4& bg_col = ImVec4(0,0,0,0), const ImVec4& tint_col = ImVec4(1,1,1,1));    // <0 frame_padding uses default frame padding settings. 0 for no padding

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
        renderFrame(checkBb.min, checkBb.max, getColorU32(col), true, style.frameRounding)
        if (v[0]) {
            val checkSz = glm.min(checkBb.width, checkBb.height)
            val pad = glm.max(1f, (checkSz / 6f).i.f)
            window.drawList.addRectFilled(checkBb.min + Vec2(pad), checkBb.max - Vec2(pad), getColorU32(Col.CheckMark), style.frameRounding)
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

        val col = getColorU32(if (held && hovered) Col.FrameBgActive else if (hovered) Col.FrameBgHovered else Col.FrameBg)
        window.drawList.addCircleFilled(center, radius, col, 16)
        if (active) {
            val checkSz = glm.min(checkBb.width, checkBb.height)
            val pad = glm.max(1f, (checkSz / 6f).i.f)
            window.drawList.addCircleFilled(center, radius - pad, getColorU32(Col.CheckMark), 16)
        }

        if (window.flags has WindowFlags.ShowBorders) {
            window.drawList.addCircle(center + Vec2(1), radius, getColorU32(Col.BorderShadow), 16)
            window.drawList.addCircle(center, radius, getColorU32(Col.Border), 16)
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
        renderFrame(frameBb.min, frameBb.max, getColorU32(Col.FrameBg), true, style.frameRounding)
        val col = getColorU32(if (popupOpen || hovered) Col.ButtonHovered else Col.Button)
        renderFrame(Vec2(frameBb.max.x - arrowSize, frameBb.min.y), frameBb.max, col, true, style.frameRounding) // FIXME-ROUNDING
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

    /** A little colored square. Return true when clicked.
     *  FIXME: May want to display/ignore the alpha component in the color display? Yet show it in the tooltip. */
    fun colorButton(col: Vec4, smallHeight: Boolean = false, outlineBorder: Boolean = true): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId("#colorbutton")
        val squareSize = g.fontSize
        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(squareSize + style.framePadding.y * 2,
                squareSize + (if (smallHeight) 0f else style.framePadding.y * 2)))
        itemSize(bb, if (smallHeight) 0f else style.framePadding.y)
        if (!itemAdd(bb, id)) return false


        val (pressed, hovered, held) = buttonBehavior(bb, id)
        renderFrame(bb.min, bb.max, getColorU32(col), outlineBorder, style.frameRounding)

        if (hovered)
            setTooltip("Color:\n(%.2f,%.2f,%.2f,%.2f)\n#%02X%02X%02X%02X", col.x, col.y, col.z, col.w,
                    F32_TO_INT8_SAT(col.x), F32_TO_INT8_SAT(col.y), F32_TO_INT8_SAT(col.z), F32_TO_INT8_SAT(col.w))

        return pressed
    }

    /** Hint: 'float col[3]' function argument is same as 'float* col'. You can pass address of first element out of a
     *  contiguous set, e.g. &myvector.x
     *  IMPORTANT: col must be a float array[3]     */
    fun colorEdit3(label: String, col: FloatArray): Boolean {

        val col4 = floatArrayOf(*col, 1f)
        val valueChanged = colorEdit4(label, col4, false)
        col[0] = col4[0]
        col[1] = col4[1]
        col[2] = col4[2]
        return valueChanged
    }

    /** Hint: 'float col[4]' function argument is same as 'float* col'. You can pass address of first element out of a
     *  contiguous set, e.g. &myvector.x
     *  IMPORTANT: col must be a float array[4]     */
    fun colorEdit4(label: String, col: FloatArray, showAlpha: Boolean = true): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(label)
        val wFull = calcItemWidth()
        val squareSz = (g.fontSize + style.framePadding.y * 2f)

        val editMode =
                if (window.dc.colorEditMode == ColorEditMode.UserSelect || window.dc.colorEditMode == ColorEditMode.UserSelectShowButton)
                    ColorEditMode.of((g.colorEditModeStorage[id]?.i ?: 0) % 3)
                else window.dc.colorEditMode

        val f = col.copyOf()
        if (editMode == ColorEditMode.HSV)
            colorConvertRGBtoHSV(f, f)

        val i = IntArray(4, { F32_TO_INT8_UNBOUND(f[it]) })

        val components = if (showAlpha) 4 else 3
        var valueChanged = false

        beginGroup()
        pushId(label)

        val hsv = editMode == ColorEditMode.HSV
        when (editMode) {

            ColorEditMode.RGB, ColorEditMode.HSV -> {
                // RGB/HSV 0..255 Sliders
                val wItemsAll = wFull - (squareSz + style.itemInnerSpacing.x)
                val wItemOne = glm.max(1f, ((wItemsAll - style.itemInnerSpacing.x * (components - 1)) / components.f).i.f)
                val wItemLast = glm.max(1f, (wItemsAll - (wItemOne + style.itemInnerSpacing.x) * (components - 1)).i.f)

                val hidePrefix = wItemOne <= calcTextSize("M:999").x
                val ids = listOf("##X", "##Y", "##Z", "##W")
                val fmtTable = listOf(
                        listOf("%3.0f", "%3.0f", "%3.0f", "%3.0f"),
                        listOf("R:%3.0f", "G:%3.0f", "B:%3.0f", "A:%3.0f"),
                        listOf("H:%3.0f", "S:%3.0f", "V:%3.0f", "A:%3.0f"))
                val fmt = if (hidePrefix) fmtTable[0] else if (hsv) fmtTable[2] else fmtTable[1]

                pushItemWidth(wItemOne)
                for (n in 0 until components) {
                    if (n > 0)
                        sameLine(0f, style.itemInnerSpacing.x)
                    if (n + 1 == components)
                        pushItemWidth(wItemLast)
                    val int = intArrayOf(i[n])
                    valueChanged = valueChanged or dragInt(ids[n], int, 1f, 0, 255, fmt[n])
                    i[n] = int[0]
                }
                popItemWidth()
                popItemWidth()
            }

            ColorEditMode.HEX -> {
                // RGB Hexadecimal Input
                val wSliderAll = wFull - squareSz
                val buf = CharArray(64)
                (if (showAlpha) "#%02X%02X%02X%02X".format(style.locale, i[0], i[1], i[2], i[3])
                else "#%02X%02X%02X".format(style.locale, i[0], i[1], i[2])).toCharArray(buf)
                pushItemWidth(wSliderAll - style.itemInnerSpacing.x)
                if (inputText("##Text", buf, InputTextFlags.CharsHexadecimal or InputTextFlags.CharsUppercase)) {
                    valueChanged = valueChanged || true
                    var p = 0
                    while (buf[p] == '#' || buf[p].isSpace)
                        p++
                    i.fill(0)
                    String(buf, p, buf.strlen - p).scanHex(i, if (showAlpha) 4 else 3, 2)
                }
                popItemWidth()
            }
            else -> Unit
        }

        sameLine(0f, style.itemInnerSpacing.x)

        val colDisplay = Vec4(col[0], col[1], col[2], 1.0f)
        if (colorButton(colDisplay))
            g.colorEditModeStorage[id] = ((editMode.i + 1) % 3).f // Don't set local copy of 'editMode' right away!

        // Recreate our own tooltip over's ColorButton() one because we want to display correct alpha here
        if (isItemHovered())
            setTooltip("Color:\n(%.2f,%.2f,%.2f,%.2f)\n#%02X%02X%02X%02X", col[0], col[1], col[2], col[3],
                    F32_TO_INT8_SAT(col[0]), F32_TO_INT8_SAT(col[1]), F32_TO_INT8_SAT(col[2]), F32_TO_INT8_SAT(col[3]))

        if (window.dc.colorEditMode == ColorEditMode.UserSelectShowButton) {
            sameLine(0f, style.itemInnerSpacing.x)
            val buttonTitles = arrayOf("RGB", "HSV", "HEX")
            if (buttonEx(buttonTitles[editMode.i], Vec2(), ButtonFlags.DontClosePopups.i))
                g.colorEditModeStorage[id] = ((editMode.i + 1) % 3).f // Don't set local copy of 'editMode' right away!
        }

        val labelDisplayEnd = findRenderedTextEnd(label)
        if (labelDisplayEnd != 0) {
            sameLine(0f, if (window.dc.colorEditMode == ColorEditMode.UserSelectShowButton) -1f else style.itemInnerSpacing.x)
            textUnformatted(label, labelDisplayEnd)
        }

        // Convert back
        for (n in 0 until 4)
            f[n] = i[n] / 255f
        if (editMode == ColorEditMode.HSV)
            colorConvertHSVtoRGB(f, f)

        if (valueChanged) {
            col[0] = f[0]
            col[1] = f[1]
            col[2] = f[2]
            if (showAlpha)
                col[3] = f[3]
        }

        popId()
        endGroup()

        return valueChanged
    }

    fun colorEditVec4(label: String, col: Vec4, showAlpha: Boolean = true): Boolean {
        val col4 = floatArrayOf(col.x, col.y, col.z, col.w)
        val valueChanged = colorEdit4(label, col4, showAlpha)
        col.x = col4[0]
        col.y = col4[1]
        col.z = col4[2]
        col.w = col4[3]
        return valueChanged
    }

    /** FIXME-OBSOLETE: This is inconsistent with most of the API and will be obsoleted/replaced.   */
    fun colorEditMode(mode:ColorEditMode) {
        currentWindow.dc.colorEditMode = mode
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