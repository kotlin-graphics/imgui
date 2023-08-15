package imgui.internal.api

import glm_.glm
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.beginPopup
import imgui.ImGui.beginTooltipEx
import imgui.ImGui.button
import imgui.ImGui.checkboxFlags
import imgui.ImGui.clipboardText
import imgui.ImGui.cursorScreenPos
import imgui.ImGui.endPopup
import imgui.ImGui.endTooltip
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.frameHeight
import imgui.ImGui.openPopup
import imgui.ImGui.popID
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushID
import imgui.ImGui.pushItemWidth
import imgui.ImGui.radioButton
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textEx
import imgui.api.colorButton
import imgui.api.colorPicker4
import imgui.api.g
import imgui.api.gImGui
import imgui.flagArrayOf
import imgui.internal.F32_TO_INT8_SAT
import imgui.internal.sections.TooltipFlag
import imgui.ColorEditFlag as Cef

/** Color */
internal interface color {

    fun colorTooltip(text: String, col: Vec3, flags: ColorEditFlags) = colorTooltip(text, col.x, col.y, col.z, 1f, flags)

    fun colorTooltip(text: String, col: Vec4, flags: ColorEditFlags) = colorTooltip(text, col.x, col.y, col.z, col.w, flags)

    /** Note: only access 3 floats if ColorEditFlag.NoAlpha flag is set. */
    fun colorTooltip(text: String, r: Float, g: Float, b: Float, a: Float, flags: ColorEditFlags) {

        if (!beginTooltipEx(TooltipFlag.OverridePrevious))
            return
        val textEnd = if (text.isEmpty()) findRenderedTextEnd(text) else 0
        if (textEnd > 0) {
            textEx(text, textEnd)
            separator()
        }
        val sz = Vec2(gImGui.fontSize * 3 + style.framePadding.y * 2)
        val cr = F32_TO_INT8_SAT(r)
        val cg = F32_TO_INT8_SAT(g)
        val cb = F32_TO_INT8_SAT(b)
        val ca = if (flags has Cef.NoAlpha) 255 else F32_TO_INT8_SAT(a)
        colorButton("##preview", r, g, b, if (flags has Cef.NoAlpha) 1f else a, (flags and (Cef._InputMask or Cef.NoAlpha or Cef.AlphaPreview or Cef.AlphaPreviewHalf)) or Cef.NoTooltip, sz)
        sameLine()
        if (flags has Cef.InputRGB || flags hasnt Cef._InputMask) if (flags has Cef.NoAlpha) text("#%02X%02X%02X\nR: $cr, G: $cg, B: $cb\n(%.3f, %.3f, %.3f)", cr, cg, cb, r, g, b)
        else text("#%02X%02X%02X%02X\nR:$cr, G:$cg, B:$cb, A:$ca\n(%.3f, %.3f, %.3f, %.3f)", cr, cg, cb, ca, r, g, b, a)
        else if (flags has Cef.InputHSV) if (flags has Cef.NoAlpha) text("H: %.3f, S: %.3f, V: %.3f", r, g, b)
        else text("H: %.3f, S: %.3f, V: %.3f, A: %.3f", r, g, b, a)

        endTooltip()
    }

    fun colorEditOptionsPopup(col: Vec3, flags: ColorEditFlags) {
        colorEditOptionsPopup(col.x, col.y, col.z, 1f, flags)
    }

    fun colorEditOptionsPopup(col: Vec4, flags: ColorEditFlags) {
        colorEditOptionsPopup(col.x, col.y, col.z, col.w, flags)
    }

    fun colorEditOptionsPopup(r: Float, g: Float, b: Float, a: Float, flags: ColorEditFlags) {
        val allowOptInputs = flags hasnt Cef._DisplayMask
        val allowOptDatatype = flags hasnt Cef._DataTypeMask
        if ((!allowOptInputs && !allowOptDatatype) || !beginPopup("context")) return
        var opts = gImGui.colorEditOptions
        if (allowOptInputs) {
            if (radioButton("RGB", opts has Cef.DisplayRGB)) opts = (opts wo Cef._DisplayMask) or Cef.DisplayRGB
            if (radioButton("HSV", opts has Cef.DisplayHSV)) opts = (opts wo Cef._DisplayMask) or Cef.DisplayHSV
            if (radioButton("Hex", opts has Cef.DisplayHEX)) opts = (opts wo Cef._DisplayMask) or Cef.DisplayHEX
        }
        if (allowOptDatatype) {
            if (allowOptInputs) separator()
            if (radioButton("0..255", opts has Cef.Uint8))
                opts = (opts wo Cef._DataTypeMask) or Cef.Uint8
            if (radioButton("0.00..1.00", opts has Cef.Float))
                opts = (opts wo Cef._DataTypeMask) or Cef.Float
        }

        if (allowOptInputs || allowOptDatatype) separator()
        if (button("Copy as..", Vec2(-1, 0))) openPopup("Copy")
        if (beginPopup("Copy")) {
            val cr = F32_TO_INT8_SAT(r)
            val cg = F32_TO_INT8_SAT(g)
            val cb = F32_TO_INT8_SAT(b)
            val ca = if (flags has Cef.NoAlpha) 255 else F32_TO_INT8_SAT(a)
            var buf = "(%.3ff, %.3ff, %.3ff, %.3ff)".format(r, g, b, if (flags has Cef.NoAlpha) 1f else a)
            if (selectable(buf)) clipboardText = buf
            buf = "(%d,%d,%d,%d)".format(cr, cg, cb, ca)
            if (selectable(buf)) clipboardText = buf
            buf = "#%02X%02X%02X".format(cr, cg, cb)
            if (selectable(buf)) clipboardText = buf
            if (flags hasnt Cef.NoAlpha) {
                buf = "#%02X%02X%02X%02X".format(cr, cg, cb, ca)
                if (selectable(buf)) clipboardText = buf
            }
            endPopup()
        }

        gImGui.colorEditOptions = opts
        endPopup()
    }

    fun colorPickerOptionsPopup(refCol: Vec3, flags: ColorEditFlags) = colorPickerOptionsPopup(refCol.x, refCol.y, refCol.z, 1f, flags)

    fun colorPickerOptionsPopup(refCol: Vec4, flags: ColorEditFlags) = colorPickerOptionsPopup(refCol.x, refCol.y, refCol.z, refCol.w, flags)

    fun colorPickerOptionsPopup(x: Float, y: Float, z: Float, w: Float, flags: ColorEditFlags) {
        val allowOptPicker = flags hasnt Cef._PickerMask
        val allowOptAlphaBar = flags hasnt Cef.NoAlpha && flags hasnt Cef.AlphaBar
        if ((!allowOptPicker && !allowOptAlphaBar) || !beginPopup("context")) return
        if (allowOptPicker) {
            // FIXME: Picker size copied from main picker function
            val pickerSize = Vec2(g.fontSize * 8, glm.max(g.fontSize * 8 - (frameHeight + style.itemInnerSpacing.x), 1f))
            pushItemWidth(pickerSize.x)
            for (pickerType in 0..1) {
                // Draw small/thumbnail version of each picker type (over an invisible button for selection)
                if (pickerType > 0) separator()
                pushID(pickerType)
                var pickerFlags: ColorEditFlags = Cef.NoInputs or Cef.NoOptions or Cef.NoLabel or Cef.NoSidePreview or (flags and Cef.NoAlpha)
                if (pickerType == 0) pickerFlags = pickerFlags or Cef.PickerHueBar
                if (pickerType == 1) pickerFlags = pickerFlags or Cef.PickerHueWheel
                val backupPos = Vec2(cursorScreenPos)
                // By default, Selectable() is closing popup
                if (selectable("##selectable", false, sizeArg = pickerSize)) g.colorEditOptions = (g.colorEditOptions wo Cef._PickerMask) or (pickerFlags and Cef._PickerMask)
                cursorScreenPos = backupPos
                colorPicker4("##previewing_picker", x, y, z, w, pickerFlags)
                popID()
            }
            popItemWidth()
        }
        if (allowOptAlphaBar) {
            if (allowOptPicker) separator()
            val pI = flagArrayOf(g.colorEditOptions)
            checkboxFlags("Alpha Bar", pI, Cef.AlphaBar)
            g.colorEditOptions = pI[0]
        }
        endPopup()
    }
}
