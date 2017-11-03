package imgui.imgui

import glm_.f
import glm_.func.common.min
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.Context.style
import imgui.ImGui.beginPopupEx
import imgui.ImGui.buttonBehavior
import imgui.ImGui.buttonEx
import imgui.ImGui.calcItemSize
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.currentWindow
import imgui.ImGui.endPopup
import imgui.ImGui.getColorU32
import imgui.ImGui.isPopupOpen
import imgui.ImGui.isWindowAppearing
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.openPopupEx
import imgui.ImGui.popId
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushId
import imgui.ImGui.pushStyleVar
import imgui.ImGui.renderCheckMark
import imgui.ImGui.renderFrame
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.renderTriangle
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setScrollHere
import imgui.ImGui.spacing
import imgui.imgui.imgui_internal.Companion.smallSquareSize
import imgui.internal.Dir
import imgui.internal.Rect
import imgui.Context as g
import imgui.WindowFlags as Wf
import imgui.internal.ButtonFlags as Bf


/** Widgets: Main   */
interface imgui_widgetsMain {

    /** button  */
    fun button(label: String, sizeArg: Vec2 = Vec2()) = buttonEx(label, sizeArg, 0)

    /** button with FramePadding = (0,0) to easily embed within text
     *  Small buttons fits within text without additional vertical spacing.     */
    fun smallButton(label: String): Boolean {
        val backupPaddingY = style.framePadding.y
        style.framePadding.y = 0f
        val pressed = buttonEx(label, Vec2(), Bf.AlignTextBaseLine.i)
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
            renderCheckMark(checkBb.min + Vec2(pad), Col.CheckMark.u32, checkBb.width - pad * 2f)
        }

        if (g.logEnabled) logRenderedText(textBb.min, if (v[0]) "[x]" else "[ ]")
        if (labelSize.x > 0f) renderText(textBb.min, label)

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

        if (window.flags has Wf.ShowBorders) {
            window.drawList.addCircle(center + Vec2(1), radius, Col.BorderShadow.u32, 16)
            window.drawList.addCircle(center, radius, Col.Border.u32, 16)
        }

        if (g.logEnabled)
            logRenderedText(textBb.min, if (active) "(x)" else "( )")
        if (labelSize.x > 0.0f)
            renderText(textBb.min, label)

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
    fun combo(label: String, currentItem: IntArray, itemsSeparatedByZeros: String, heightInItems: Int = -1): Boolean {

        val items = itemsSeparatedByZeros.split('\u0000').filter { it.isNotEmpty() }
        // FIXME-OPT: Avoid computing this, or at least only when combo is open
        return combo(label, currentItem, items, heightInItems)
    }

    /** FIXME-WIP: New Combo API    */
    fun beginCombo(label: String, previewValue: String?, popupSize: Vec2 = Vec2()): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(label)
        val w = calcItemWidth()

        val labelSize = calcTextSize(label, true)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w, labelSize.y + style.framePadding.y * 2f))
        val totalBb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f))
        itemSize(totalBb, style.framePadding.y)
        if (!itemAdd(totalBb, id)) return false

        val arrowSize = smallSquareSize
        val (pressed, hovered, held) = buttonBehavior(frameBb, id)
        var popupOpen = isPopupOpen(id)

        val valueBb = Rect(frameBb.min, frameBb.max - Vec2(arrowSize, 0f))
        renderFrame(frameBb.min, frameBb.max, Col.FrameBg.u32, true, style.frameRounding)
        val col = if (popupOpen || hovered) Col.ButtonHovered else Col.Button
        renderFrame(Vec2(frameBb.max.x - arrowSize, frameBb.min.y), frameBb.max, col.u32, true, style.frameRounding) // FIXME-ROUNDING
        renderTriangle(Vec2(frameBb.max.x - arrowSize, frameBb.min.y) plus_ style.framePadding.y, Dir.Down)

        if (previewValue != null)
            renderTextClipped(frameBb.min + style.framePadding, valueBb.max, previewValue)

        if (labelSize.x > 0)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, frameBb.min.y + style.framePadding.y), label)

        if (pressed && !popupOpen) {
            openPopupEx(id, false)
            popupOpen = true
        }

        if (!popupOpen) return false

        var popupY1 = frameBb.max.y
        var popupY2 = glm.clamp(popupY1 + popupSize.y, popupY1, IO.displaySize.y - style.displaySafeAreaPadding.y)
        if ((popupY2 - popupY1) < glm.min(popupSize.y, frameBb.min.y - style.displaySafeAreaPadding.y)) {
            /*  Position our combo ABOVE because there's more space to fit! (FIXME: Handle in Begin() or use a shared helper.
            We have similar code in Begin() for popup placement)         */
            popupY1 = glm.clamp(frameBb.min.y - popupSize.y, style.displaySafeAreaPadding.y, frameBb.min.y)
            popupY2 = frameBb.min.y
            setNextWindowPos(frameBb.min, Cond.Always, Vec2(0f, 1f))
        } else   // Position our combo below
            setNextWindowPos(Vec2(frameBb.min.x, frameBb.max.y), Cond.Always, Vec2())
        setNextWindowSize(Vec2(popupSize.x, popupY2 - popupY1), Cond.Appearing)
        pushStyleVar(StyleVar.WindowPadding, style.framePadding)

        val flags = Wf.ComboBox or if (window.flags has Wf.ShowBorders) Wf.ShowBorders else Wf.Null
        if (!beginPopupEx(id, flags)) {
            assert(false)   // This should never happen as we tested for IsPopupOpen() above
            return false
        }
        spacing()

        return true
    }

    fun endCombo() {
        endPopup()
        popStyleVar()
    }

    /** Combo box function. */
    fun combo(label: String, currentItem: IntArray, items: List<String>, heightInItems: Int = -1): Boolean {

        val previewText = items.getOrElse(currentItem[0], { "" })

        // Size default to hold ~7 items
        val heightInItems = if (heightInItems < 0) 7 else heightInItems
        val popupHeight = (g.fontSize + style.itemSpacing.y) * items.size.min(heightInItems) + style.framePadding.y * 3

        if (!beginCombo(label, previewText, Vec2(0f, popupHeight))) return false

        // Display items, FIXME-OPT: Use clipper
        var valueChanged = false
        for (i in 0 until items.size) {
            pushId(i)
            val itemSelected = i == currentItem[0]
            val itemText = items.getOrElse(i, { "*Unknown item*" })
            if (selectable(itemText, itemSelected)) {
                valueChanged = true
                currentItem[0] = i
            }
            if (itemSelected && isWindowAppearing)
                setScrollHere()
            popId()
        }
        endCombo()
        return valueChanged
    }

//    IMGUI_API void          PlotLines(const char* label, const float* values, int values_count, int values_offset = 0, const char* overlay_text = NULL, float scale_min = FLT_MAX, float scale_max = FLT_MAX, ImVec2 graph_size = ImVec2(0,0), int stride = sizeof(float));
//    IMGUI_API void          PlotLines(const char* label, float (*values_getter)(void* data, int idx), void* data, int values_count, int values_offset = 0, const char* overlay_text = NULL, float scale_min = FLT_MAX, float scale_max = FLT_MAX, ImVec2 graph_size = ImVec2(0,0));
//    IMGUI_API void          PlotHistogram(const char* label, const float* values, int values_count, int values_offset = 0, const char* overlay_text = NULL, float scale_min = FLT_MAX, float scale_max = FLT_MAX, ImVec2 graph_size = ImVec2(0,0), int stride = sizeof(float));
//    IMGUI_API void          PlotHistogram(const char* label, float (*values_getter)(void* data, int idx), void* data, int values_count, int values_offset = 0, const char* overlay_text = NULL, float scale_min = FLT_MAX, float scale_max = FLT_MAX, ImVec2 graph_size = ImVec2(0,0));
//    IMGUI_API void          ProgressBar(float fraction, const ImVec2& size_arg = ImVec2(-1,0), const char* overlay = NULL);
}