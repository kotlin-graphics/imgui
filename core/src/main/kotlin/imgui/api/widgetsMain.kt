package imgui.api

import glm_.*
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.arrowButtonEx
import imgui.ImGui.buttonBehavior
import imgui.ImGui.buttonEx
import imgui.ImGui.calcItemSize
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.currentWindow
import imgui.ImGui.frameHeight
import imgui.ImGui.getColorU32
import imgui.ImGui.imageButtonEx
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.logRenderedText
import imgui.ImGui.markItemEdited
import imgui.ImGui.popID
import imgui.ImGui.pushID
import imgui.ImGui.renderFrame
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.sameLine
import imgui.ImGui.style
import imgui.internal.classes.Rect
import imgui.internal.floor
import imgui.internal.lerp
import imgui.internal.round
import imgui.internal.saturate
import imgui.internal.sections.*
import kool.getValue
import kool.setValue
import kotlin.reflect.KMutableProperty0
import imgui.internal.sections.ButtonFlag as Bf


// @formatter:off
val S8_MIN: Int = -128
val S8_MAX: Int = 127
val U8_MIN: Int = 0
val U8_MAX: Int = 0xFF
val S16_MIN: Int = -32768
val S16_MAX: Int = 32767
val U16_MIN: Int = 0
val U16_MAX: Int = 0xFFFF
val S32_MIN: Int = Integer.MIN_VALUE
val S32_MAX: Int = Integer.MAX_VALUE
// @formatter:on


/** Widgets: Main
 *  - Most widgets return true when the value has been changed or when pressed/selected
 *  - You may also use one of the many IsItemXXX functions (e.g. IsItemActive, IsItemHovered, etc.) to query widget state.  */
interface widgetsMain {

    /** button  */
    fun button(label: String, sizeArg: Vec2 = Vec2()): Boolean = buttonEx(label, sizeArg, Bf.None.i)

    /** button with FramePadding = (0,0) to easily embed within text
     *  Small buttons fits within text without additional vertical spacing.     */
    fun smallButton(label: String): Boolean {
        val backupPaddingY = style.framePadding.y
        style.framePadding.y = 0f
        val pressed = buttonEx(label, Vec2(), Bf.AlignTextBaseLine.i)
        style.framePadding.y = backupPaddingY
        return pressed
    }

    /** flexible button behavior without the visuals, frequently useful to build custom behaviors using the public api
     *  (along with isItemActive, isItemHovered, etc.)
     *  Tip: use pushId()/popId() to push indices or pointers in the ID stack.
     *  Then you can keep 'strid' empty or the same for all your buttons (instead of creating a string based on a
     *  non-string id)  */
    fun invisibleButton(strId: String, sizeArg: Vec2, flags: ButtonFlags = Bf.None.i): Boolean {
        val window = currentWindow
        if (window.skipItems) return false

        assert(sizeArg.allNotEqual(0f)) { "Cannot use zero-size for InvisibleButton(). Unlike Button() there is not way to fallback using the label size." }

        val id = window.getID(strId)
        val size = calcItemSize(sizeArg, 0f, 0f)
        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        itemSize(size)
        if (!itemAdd(bb, id)) return false

        val (pressed, _, _) = buttonBehavior(bb, id, flags)

        return pressed
    }

    fun arrowButton(id: String, dir: Dir): Boolean = arrowButtonEx(id, dir, Vec2(frameHeight), Bf.None.i)

    fun image(userTextureId: TextureID, size: Vec2, uv0: Vec2 = Vec2(), uv1: Vec2 = Vec2(1), tintCol: Vec4 = Vec4(1),
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
    fun imageButton(userTextureId: TextureID, size: Vec2, uv0: Vec2 = Vec2(), uv1: Vec2 = Vec2(), framePadding: Int = -1,
                    bgCol: Vec4 = Vec4(), tintCol: Vec4 = Vec4(1)): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        // Default to using texture ID as ID. User can still push string/integer prefixes.
        pushID(userTextureId.L)
        val id = window.getID("#image")
        popID()

        val padding = if (framePadding >= 0) Vec2(framePadding) else Vec2(style.framePadding)
        return imageButtonEx(id, userTextureId, size, uv0, uv1, padding, bgCol, tintCol)
    }

    fun checkbox(label: String, v: BooleanArray) = checkbox(label, v, 0)
    fun checkbox(label: String, v: BooleanArray, i: Int): Boolean {
        _b = v[i]
        return checkbox(label, ::_b).also {
            v[i] = _b
        }
    }

    fun checkbox(label: String, vPtr: KMutableProperty0<Boolean>): Boolean {

        var v by vPtr
        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getID(label)
        val labelSize = calcTextSize(label, hideTextAfterDoubleHash = true)

        val squareSz = frameHeight
        val pos = Vec2(window.dc.cursorPos) //cursorPos gets updated somewhere else, which means we need to make a copy else checkboxes act incorrectly
        val totalBb = Rect(pos, pos + Vec2(squareSz + if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, labelSize.y + style.framePadding.y * 2f))
        itemSize(totalBb, style.framePadding.y)
        if (!itemAdd(totalBb, id))
            return false

        val (pressed, hovered, held) = buttonBehavior(totalBb, id)
        if (pressed) {
            v = !v
            markItemEdited(id)
        }

        val checkBb = Rect(pos, pos + squareSz)
        renderNavHighlight(totalBb, id)
        val col = if (held && hovered) Col.FrameBgActive else if (hovered) Col.FrameBgHovered else Col.FrameBg
        renderFrame(checkBb.min, checkBb.max, col.u32, true, style.frameRounding)
        val checkCol = Col.CheckMark.u32
        val mixedValue = window.dc.itemFlags has ItemFlag.MixedValue
        if (mixedValue) {
            // Undocumented tristate/mixed/indeterminate checkbox (#2644)
            val pad = Vec2(1f max floor(squareSz / 3.6f))
            window.drawList.addRectFilled(checkBb.min + pad, checkBb.max - pad, checkCol, style.frameRounding)
        } else if (v) {
            val pad = 1f max floor(squareSz / 6f)
            window.drawList.renderCheckMark(checkBb.min + pad, checkCol, squareSz - pad * 2f)
        }

        if (g.logEnabled) logRenderedText(totalBb.min, if (mixedValue) "[~]" else if (v) "[x]" else "[ ]")
        if (labelSize.x > 0f)
            renderText(Vec2(checkBb.max.x + style.itemInnerSpacing.x, checkBb.min.y + style.framePadding.y), label)

        Hook.itemInfo?.invoke(g, id, label, window.dc.itemFlags or ItemStatusFlag.Checkable or if (v) ItemStatusFlag.Checked else ItemStatusFlag.None)

        return pressed
    }

    fun checkboxFlags(label: String, flags: IntArray, flagsValue: Int): Boolean {
        val v = booleanArrayOf((flags[0] and flagsValue) == flagsValue)
        val pressed = when {
            !v[0] && flags[0] has flagsValue -> { // Mixed value (FIXME: find a way to expose neatly to Checkbox?)
                val window = currentWindow
                val backupItemFlags = window.dc.itemFlags
                window.dc.itemFlags = window.dc.itemFlags or ItemFlag.MixedValue
                checkbox(label, v).also {
                    window.dc.itemFlags = backupItemFlags
                }
            }
            else -> checkbox(label, v) // Regular checkbox
        }
        if (pressed)
            flags[0] = when {
                v[0] -> flags[0] or flagsValue
                else -> flags[0] wo flagsValue
            }
        return pressed
    }

    fun checkboxFlags(label: String, flagsPtr: KMutableProperty0<Int>, flagsValue: Int): Boolean {
        var flags by flagsPtr
        val v = booleanArrayOf((flags and flagsValue) == flagsValue)
        val pressed = checkbox(label, v)
        if (pressed)
            flags = when {
                v[0] -> flags or flagsValue
                else -> flags wo flagsValue
            }
        return pressed
    }

    /** use with e.g. if (radioButton("one", myValue==1))  myValue = 1 */
    fun radioButton(label: String, active: Boolean): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getID(label)
        val labelSize = calcTextSize(label, hideTextAfterDoubleHash = true)

        val squareSz = frameHeight
        val pos = window.dc.cursorPos
        val checkBb = Rect(pos, pos + squareSz)
        val totalBb = Rect(pos, pos + Vec2(squareSz + if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, labelSize.y + style.framePadding.y * 2f))
        itemSize(totalBb, style.framePadding.y)
        if (!itemAdd(totalBb, id))
            return false

        val center = Vec2(checkBb.center)
        center.x = round(center.x)
        center.y = round(center.y)
        val radius = (squareSz - 1f) * 0.5f

        val (pressed, hovered, held) = buttonBehavior(totalBb, id)
        if (pressed)
            markItemEdited(id)

        renderNavHighlight(totalBb, id)
        val col = if (held && hovered) Col.FrameBgActive else if (hovered) Col.FrameBgHovered else Col.FrameBg
        window.drawList.addCircleFilled(center, radius, col.u32, 16)
        if (active) {
            val pad = 1f max floor(squareSz / 6f)
            window.drawList.addCircleFilled(center, radius - pad, Col.CheckMark.u32, 16)
        }

        if (style.frameBorderSize > 0f) {
            window.drawList.addCircle(center + Vec2(1), radius, Col.BorderShadow.u32, 16, style.frameBorderSize)
            window.drawList.addCircle(center, radius, Col.Border.u32, 16, style.frameBorderSize)
        }

        if (g.logEnabled)
            logRenderedText(totalBb.min, if (active) "(x)" else "( )")
        if (labelSize.x > 0f)
            renderText(Vec2(checkBb.max.x + style.itemInnerSpacing.x, checkBb.min.y + style.framePadding.y), label)

        Hook.itemInfo?.invoke(g, id, label, window.dc.itemFlags)
        return pressed
    }

    /** shortcut to handle the above pattern when value is an integer
     *
     *  FIXME: This would work nicely if it was a public template, e.g. 'template<T> RadioButton(const char* label, T* v, T v_button)', but I'm not sure how we would expose it.. */
    fun radioButton(label: String, v: IntArray, vButton: Int): Boolean =
            radioButton(label, v[0] == vButton).also { if (it) v[0] = vButton }

    /** shortcut to handle the above pattern when value is an integer
     *
     *  FIXME: This would work nicely if it was a public template, e.g. 'template<T> RadioButton(const char* label, T* v, T v_button)', but I'm not sure how we would expose it.. */
    fun radioButton(label: String, v: KMutableProperty0<Int>, vButton: Int): Boolean =
            radioButton(label, v() == vButton).also { if (it) v.set(vButton) }

    fun progressBar(fraction_: Float, sizeArg: Vec2 = Vec2(-1f, 0f), overlay_: String = "") {
        val window = currentWindow
        if (window.skipItems) return

        val pos = Vec2(window.dc.cursorPos)
        val size = calcItemSize(sizeArg, calcItemWidth(), g.fontSize + style.framePadding.y * 2f)
        val bb = Rect(pos, pos + size)
        itemSize(size, style.framePadding.y)
        if (!itemAdd(bb, 0)) return
        // Render
        val fraction = saturate(fraction_)
        renderFrame(bb.min, bb.max, Col.FrameBg.u32, true, style.frameRounding)
        bb expand Vec2(-style.frameBorderSize)
        val fillBr = Vec2(lerp(bb.min.x, bb.max.x, fraction), bb.max.y)
        window.drawList.renderRectFilledRangeH(bb, Col.PlotHistogram.u32, 0f, fraction, style.frameRounding)
        // Default displaying the fraction as percentage string, but user can override it
        val overlay = if (overlay_.isEmpty()) "%.0f%%".format(style.locale, fraction * 100 + 0.01f) else overlay_

        val overlaySize = calcTextSize(overlay)
        if (overlaySize.x > 0f) {
            val x = glm.clamp(fillBr.x + style.itemSpacing.x, bb.min.x, bb.max.x - overlaySize.x - style.itemInnerSpacing.x)
            renderTextClipped(Vec2(x, bb.min.y), bb.max, overlay, overlaySize, Vec2(0f, 0.5f), bb)
        }
    }

    /** draw a small circle + keep the cursor on the same line. advance cursor x position
     *  by GetTreeNodeToLabelSpacing(), same distance that TreeNode() uses  */
    fun bullet() {

        val window = currentWindow
        if (window.skipItems) return

        val lineHeight = glm.max(glm.min(window.dc.currLineSize.y, g.fontSize + style.framePadding.y * 2), g.fontSize)
        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(g.fontSize, lineHeight))
        itemSize(bb)
        if (!itemAdd(bb, 0)) {
            sameLine(0f, style.framePadding.x * 2)
            return
        }

        // Render and stay on same line
        val textCol = Col.Text.u32
        window.drawList.renderBullet(bb.min + Vec2(style.framePadding.x + g.fontSize * 0.5f, lineHeight * 0.5f), textCol)
        sameLine(0f, style.framePadding.x * 2f)
        sameLine(0f, style.framePadding.x * 2)
    }
}