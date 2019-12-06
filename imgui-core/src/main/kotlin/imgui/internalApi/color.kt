package imgui.internalApi

import glm_.glm
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.beginPopup
import imgui.ImGui.beginTooltipEx
import imgui.ImGui.button
import imgui.ImGui.checkboxFlags
import imgui.ImGui.clipboardText
import imgui.ImGui.colorButton
import imgui.ImGui.colorPicker4
import imgui.ImGui.cursorScreenPos
import imgui.ImGui.endPopup
import imgui.ImGui.endTooltip
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.frameHeight
import imgui.ImGui.openPopup
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.ImGui.radioButton
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textEx
import imgui.api.g

/** Color */
interface color {

    /** Note: only access 3 floats if ColorEditFlag.NoAlpha flag is set.   */
    fun colorTooltip(text: String, col: FloatArray, flags: ColorEditFlags) {

        beginTooltipEx(0, true)
        val textEnd = if (text.isEmpty()) findRenderedTextEnd(text) else 0
        if (textEnd > 0) {
            textEx(text, textEnd)
            separator()
        }
        val sz = Vec2(g.fontSize * 3 + style.framePadding.y * 2)
        val cf = Vec4(col[0], col[1], col[2], if (flags has ColorEditFlag.NoAlpha) 1f else col[3])
        val cr = F32_TO_INT8_SAT(col[0])
        val cg = F32_TO_INT8_SAT(col[1])
        val cb = F32_TO_INT8_SAT(col[2])
        val ca = if (flags has ColorEditFlag.NoAlpha) 255 else F32_TO_INT8_SAT(col[3])
        colorButton("##preview", cf, (flags and (ColorEditFlag._InputMask or ColorEditFlag.NoAlpha or ColorEditFlag.AlphaPreview or ColorEditFlag.AlphaPreviewHalf)) or ColorEditFlag.NoTooltip, sz)
        sameLine()
        if (flags has ColorEditFlag.InputRGB || flags hasnt ColorEditFlag._InputMask)
            if (flags has ColorEditFlag.NoAlpha)
                text("#%02X%02X%02X\nR: $cr, G: $cg, B: $cb\n(%.3f, %.3f, %.3f)", cr, cg, cb, col[0], col[1], col[2])
            else
                text("#%02X%02X%02X%02X\nR:$cr, G:$cg, B:$cb, A:$ca\n(%.3f, %.3f, %.3f, %.3f)", cr, cg, cb, ca, col[0], col[1], col[2], col[3])
        else if (flags has ColorEditFlag.InputHSV)
            if (flags has ColorEditFlag.NoAlpha)
                text("H: %.3f, S: %.3f, V: %.3f", col[0], col[1], col[2])
            else
                text("H: %.3f, S: %.3f, V: %.3f, A: %.3f", col[0], col[1], col[2], col[3])

        endTooltip()
    }

    /** @param flags ColorEditFlags */
    fun colorEditOptionsPopup(col: FloatArray, flags: ColorEditFlags) {
        val allowOptInputs = flags hasnt ColorEditFlag._DisplayMask
        val allowOptDatatype = flags hasnt ColorEditFlag._DataTypeMask
        if ((!allowOptInputs && !allowOptDatatype) || !beginPopup("context")) return
        var opts = g.colorEditOptions
        if (allowOptInputs) {
            if (radioButton("RGB", opts has ColorEditFlag.DisplayRGB))
                opts = (opts wo ColorEditFlag._DisplayMask) or ColorEditFlag.DisplayRGB
            if (radioButton("HSV", opts has ColorEditFlag.DisplayHSV))
                opts = (opts wo ColorEditFlag._DisplayMask) or ColorEditFlag.DisplayHSV
            if (radioButton("HEX", opts has ColorEditFlag.DisplayHEX))
                opts = (opts wo ColorEditFlag._DisplayMask) or ColorEditFlag.DisplayHEX
        }
        if (allowOptDatatype) {
            if (allowOptInputs) separator()
            if (radioButton("0..255", opts has ColorEditFlag.Uint8))
                opts = (opts wo ColorEditFlag._DataTypeMask) or ColorEditFlag.Uint8
            if (radioButton("0.00..1.00", opts has ColorEditFlag.Float))
                opts = (opts wo ColorEditFlag._DataTypeMask) or ColorEditFlag.Float
        }

        if (allowOptInputs || allowOptDatatype) separator()
        if (button("Copy as..", Vec2(-1, 0)))
            openPopup("Copy")
        if (beginPopup("Copy")) {
            val cr = F32_TO_INT8_SAT(col[0])
            val cg = F32_TO_INT8_SAT(col[1])
            val cb = F32_TO_INT8_SAT(col[2])
            val ca = if (flags has ColorEditFlag.NoAlpha) 255 else F32_TO_INT8_SAT(col[3])
            var buf = "(%.3ff, %.3ff, %.3ff, %.3ff)".format(col[0], col[1], col[2], if (flags has ColorEditFlag.NoAlpha) 1f else col[3])
            if (selectable(buf))
                clipboardText = buf
            buf = "(%d,%d,%d,%d)".format(cr, cg, cb, ca)
            if (selectable(buf))
                clipboardText = buf
            buf = when {
                flags has ColorEditFlag.NoAlpha -> "0x%02X%02X%02X".format(cr, cg, cb)
                else -> "0x%02X%02X%02X%02X".format(cr, cg, cb, ca)
            }
            if (selectable(buf))
                clipboardText = buf
            endPopup()
        }

        g.colorEditOptions = opts
        endPopup()
    }

    fun colorPickerOptionsPopup(refCol: FloatArray, flags: ColorEditFlags) {
        val allowOptPicker = flags hasnt ColorEditFlag._PickerMask
        val allowOptAlphaBar = flags hasnt ColorEditFlag.NoAlpha && flags hasnt ColorEditFlag.AlphaBar
        if ((!allowOptPicker && !allowOptAlphaBar) || !beginPopup("context")) return
        if (allowOptPicker) {
            // FIXME: Picker size copied from main picker function
            val pickerSize = Vec2(g.fontSize * 8, glm.max(g.fontSize * 8 - (frameHeight + style.itemInnerSpacing.x), 1f))
            pushItemWidth(pickerSize.x)
            for (pickerType in 0..1) {
                // Draw small/thumbnail version of each picker type (over an invisible button for selection)
                if (pickerType > 0) separator()
                pushId(pickerType)
                var pickerFlags: ColorEditFlags = ColorEditFlag.NoInputs or ColorEditFlag.NoOptions or ColorEditFlag.NoLabel or
                        ColorEditFlag.NoSidePreview or (flags and ColorEditFlag.NoAlpha)
                if (pickerType == 0) pickerFlags = pickerFlags or ColorEditFlag.PickerHueBar
                if (pickerType == 1) pickerFlags = pickerFlags or ColorEditFlag.PickerHueWheel
                val backupPos = Vec2(cursorScreenPos)
                if (selectable("##selectable", false, 0, pickerSize)) // By default, Selectable() is closing popup
                    g.colorEditOptions = (g.colorEditOptions wo ColorEditFlag._PickerMask) or (pickerFlags and ColorEditFlag._PickerMask)
                cursorScreenPos = backupPos
                val dummyRefCol = Vec4()
                for (i in 0..2) dummyRefCol[i] = refCol[i]
                if (pickerFlags hasnt ColorEditFlag.NoAlpha) dummyRefCol[3] = refCol[3]
                colorPicker4("##dummypicker", dummyRefCol, pickerFlags)
                popId()
            }
            popItemWidth()
        }
        if (allowOptAlphaBar) {
            if (allowOptPicker) separator()
            val pI = intArrayOf(g.colorEditOptions)
            checkboxFlags("Alpha Bar", pI, ColorEditFlag.AlphaBar.i)
            g.colorEditOptions = pI[0]
        }
        endPopup()
    }

}