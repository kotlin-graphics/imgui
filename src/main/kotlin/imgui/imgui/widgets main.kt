package imgui.imgui

import glm_.f
import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.style
import imgui.ImGui.buttonBehavior
import imgui.ImGui.buttonEx
import imgui.ImGui.calcItemSize
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.currentWindow
import imgui.ImGui.frameHeight
import imgui.ImGui.getColorU32
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.plotEx
import imgui.ImGui.popId
import imgui.ImGui.pushId
import imgui.ImGui.renderArrow
import imgui.ImGui.renderCheckMark
import imgui.ImGui.renderFrame
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.renderRectFilledRangeH
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.sameLine
import imgui.internal.*
import kotlin.reflect.KMutableProperty0
import imgui.ComboFlags as Cf
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

    fun arrowButton(strId: String, dir: Dir): Boolean {
        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getId(strId)
        val sz = frameHeight
        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + sz)
        itemSize(bb)
        if (!itemAdd(bb, id)) return false

        val (pressed, hovered, held) = buttonBehavior(bb, id)

        // Render
        val col = if(hovered && held) Col.ButtonActive else if(hovered ) Col.ButtonHovered else Col.Button
        renderNavHighlight(bb, id)
        renderFrame(bb.min, bb.max, col.u32, true, g.style.frameRounding)
        renderArrow(bb.min + g.style.framePadding, dir)

        return pressed
    }

    /** button behavior without the visuals, useful to build custom behaviors using the public api (along with
     *  isItemActive, isItemHovered, etc.)
     *  Tip: use pushId()/popId() to push indices or pointers in the ID stack.
     *  Then you can keep 'strid' empty or the same for all your buttons (instead of creating a string based on a
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
        if (borderCol.w > 0f) bb.max plusAssign 2
        itemSize(bb)
        if (!itemAdd(bb, 0)) return

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
        renderNavHighlight(bb, id)
        renderFrame(bb.min, bb.max, col.u32, true, glm.clamp(glm.min(padding.x, padding.y), 0f, style.frameRounding))
        if (bgCol.w > 0f)
            window.drawList.addRectFilled(imageBb.min, imageBb.max, getColorU32(bgCol))
        window.drawList.addImage(userTextureId, imageBb.min, imageBb.max, uv0, uv1, getColorU32(tintCol))

        return pressed
    }


    fun checkbox(label: String, v: BooleanArray): Boolean {
        b = v[0]
        val res = checkbox(label, ::b)
        v[0] = b
        return res
    }

    fun checkbox(label: String, v: KMutableProperty0<Boolean>): Boolean {

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
        if (pressed) v.set(!v())

        renderNavHighlight(totalBb, id)
        val col = if (held && hovered) Col.FrameBgActive else if (hovered) Col.FrameBgHovered else Col.FrameBg
        renderFrame(checkBb.min, checkBb.max, col.u32, true, style.frameRounding)
        if (v()) {
            val checkSz = glm.min(checkBb.width, checkBb.height)
            val pad = glm.max(1f, (checkSz / 6f).i.f)
            renderCheckMark(checkBb.min + Vec2(pad), Col.CheckMark.u32, checkBb.width - pad * 2f)
        }

        if (g.logEnabled) logRenderedText(textBb.min, if (v()) "[x]" else "[ ]")
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

    fun checkboxFlags(label: String, flags: KMutableProperty0<Int>, flagsValue: Int): Boolean {
        val v = booleanArrayOf((flags() and flagsValue) == flagsValue)
        val pressed = checkbox(label, v)
        if (pressed)
            flags.set(
                    if (v[0]) flags() or flagsValue
                    else flags() wo flagsValue)
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

        renderNavHighlight(totalBb, id)
        val col = if (held && hovered) Col.FrameBgActive else if (hovered) Col.FrameBgHovered else Col.FrameBg
        window.drawList.addCircleFilled(center, radius, col.u32, 16)
        if (active) {
            val checkSz = glm.min(checkBb.width, checkBb.height)
            val pad = glm.max(1f, (checkSz / 6f).i.f)
            window.drawList.addCircleFilled(center, radius - pad, Col.CheckMark.u32, 16)
        }

        if (style.frameBorderSize > 0f) {
            window.drawList.addCircle(center + Vec2(1), radius, Col.BorderShadow.u32, 16, style.frameBorderSize)
            window.drawList.addCircle(center, radius, Col.Border.u32, 16, style.frameBorderSize)
        }

        if (g.logEnabled)
            logRenderedText(textBb.min, if (active) "(x)" else "( )")
        if (labelSize.x > 0.0f)
            renderText(textBb.min, label)

        return pressed
    }

    fun radioButton(label: String, v: IntArray, vButton: Int) = radioButton(label, v[0] == vButton).also { if (it) v[0] = vButton }
    fun radioButton(label: String, v: KMutableProperty0<Int>, vButton: Int) = radioButton(label, v() == vButton).also { if (it) v.set(vButton) }

    interface PlotArray {
        operator fun get(idx: Int): Float
        fun count(): Int
    }

    class PlotArrayData(val values: FloatArray, val stride: Int) : PlotArray {
        override operator fun get(idx: Int) = values[idx * stride]
        override fun count() = values.size
    }

    class PlotArrayFunc(val func: (Int) -> Float, val count: Int) : PlotArray {
        override operator fun get(idx: Int) = func(idx)
        override fun count() = count
    }

    fun plotLines(label: String, values: FloatArray, valuesOffset: Int = 0, overlayText: String = "", scaleMin: Float = Float.MAX_VALUE,
                  scaleMax: Float = Float.MAX_VALUE, graphSize: Vec2 = Vec2(), stride: Int = 1) {

        val data = PlotArrayData(values, stride)
        plotEx(PlotType.Lines, label, data, valuesOffset, overlayText, scaleMin, scaleMax, graphSize)
    }

    fun plotLines(label: String, valuesGetter: (idx: Int) -> Float, valuesCount: Int, valuesOffset: Int = 0,
                  overlayText: String = "", scaleMin: Float = Float.MAX_VALUE, scaleMax: Float = Float.MAX_VALUE,
                  graphSize: Vec2 = Vec2()) {

        val data = PlotArrayFunc(valuesGetter, valuesCount)
        plotEx(PlotType.Lines, label, data, valuesOffset, overlayText, scaleMin, scaleMax, graphSize)
    }

    fun plotHistogram(label: String, values: FloatArray, valuesOffset: Int = 0, overlayText: String = "",
                      scaleMin: Float = Float.MAX_VALUE, scaleMax: Float = Float.MAX_VALUE, graphSize: Vec2 = Vec2(), stride: Int = 1) {

        val data = PlotArrayData(values, stride)
        plotEx(PlotType.Histogram, label, data, valuesOffset, overlayText, scaleMin, scaleMax, graphSize)
    }

    fun plotHistogram(label: String, valuesGetter: (idx: Int) -> Float, valuesCount: Int, valuesOffset: Int = 0,
                      overlayText: String = "", scaleMin: Float = Float.MAX_VALUE, scaleMax: Float = Float.MAX_VALUE,
                      graphSize: Vec2 = Vec2()) {

        val data = PlotArrayFunc(valuesGetter, valuesCount)
        plotEx(PlotType.Histogram, label, data, valuesOffset, overlayText, scaleMin, scaleMax, graphSize)
    }

    fun progressBar(fraction: Float, sizeArg: Vec2 = Vec2(-1f, 0f), overlay: String = "") {
        val window = currentWindow
        if (window.skipItems) return

        val pos = Vec2(window.dc.cursorPos)
        val bb = Rect(pos, pos + calcItemSize(sizeArg, calcItemWidth(), g.fontSize + style.framePadding.y * 2f))
        itemSize(bb, style.framePadding.y)
        if (!itemAdd(bb, 0)) return
        // Render
        val fraction = saturate(fraction)
        renderFrame(bb.min, bb.max, Col.FrameBg.u32, true, style.frameRounding)
        bb expand Vec2(-style.frameBorderSize)
        val fillBr = Vec2(lerp(bb.min.x, bb.max.x, fraction), bb.max.y)
        renderRectFilledRangeH(window.drawList, bb, Col.PlotHistogram.u32, 0f, fraction, style.frameRounding)
        // Default displaying the fraction as percentage string, but user can override it
        val overlay = if (overlay.isEmpty()) "%.0f%%".format(style.locale, fraction * 100 + 0.01f) else overlay

        val overlaySize = calcTextSize(overlay, 0)
        if (overlaySize.x > 0f) {
            val x = glm.clamp(fillBr.x + style.itemSpacing.x, bb.min.x, bb.max.x - overlaySize.x - style.itemInnerSpacing.x)
            renderTextClipped(Vec2(x, bb.min.y), bb.max, overlay, 0, overlaySize, Vec2(0f, 0.5f), bb)
        }
    }

    companion object {
        private var b = false
    }
}