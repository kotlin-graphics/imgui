package imgui.api

import glm_.glm
import glm_.max
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.arrowButtonEx
import imgui.ImGui.buttonBehavior
import imgui.ImGui.buttonEx
import imgui.ImGui.calcItemSize
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.currentWindow
import imgui.ImGui.frameHeight
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.logRenderedText
import imgui.ImGui.markItemEdited
import imgui.ImGui.renderBullet
import imgui.ImGui.renderCheckMark
import imgui.ImGui.renderFrame
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.renderRectFilledRangeH
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.sameLine
import imgui.ImGui.style
import imgui.internal.classes.Rect
import imgui.internal.floor
import imgui.internal.lerp
import imgui.internal.round
import imgui.internal.saturate
import imgui.internal.sections.ButtonFlags
import imgui.internal.sections.IMGUI_TEST_ENGINE_ITEM_INFO
import imgui.internal.sections.ItemFlag
import imgui.internal.sections.ItemStatusFlag
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


// Widgets: Main
// - Most widgets return true when the value has been changed or when pressed/selected
// - You may also use one of the many IsItemXXX functions (e.g. IsItemActive, IsItemHovered, etc.) to query widget state.
interface widgetsMain {

    /** button  */
    fun button(label: String, sizeArg: Vec2 = Vec2()): Boolean = buttonEx(label, sizeArg, emptyFlags())

    /** button with FramePadding = (0,0) to easily embed within text
     *  Small buttons fits within text without additional vertical spacing.     */
    fun smallButton(label: String): Boolean {
        val backupPaddingY = style.framePadding.y
        style.framePadding.y = 0f
        val pressed = buttonEx(label, Vec2(), Bf.AlignTextBaseLine)
        style.framePadding.y = backupPaddingY
        return pressed
    }

    /** flexible button behavior without the visuals, frequently useful to build custom behaviors using the public api
     *  (along with isItemActive, isItemHovered, etc.)
     *  Tip: use pushId()/popId() to push indices or pointers in the ID stack.
     *  Then you can keep 'strid' empty or the same for all your buttons (instead of creating a string based on a
     *  non-string id)  */
    fun invisibleButton(strId: String, sizeArg: Vec2, flags: ButtonFlags = emptyFlags()): Boolean {
        val window = currentWindow
        if (window.skipItems) return false

        assert(sizeArg.allNotEqual(0f)) { "Cannot use zero-size for InvisibleButton(). Unlike Button() there is not way to fallback using the label size." }

        val id = window.getID(strId)
        val size = calcItemSize(sizeArg, 0f, 0f)
        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        itemSize(size)
        if (!itemAdd(bb, id)) return false

        val (pressed, _, _) = buttonBehavior(bb, id, flags)

        IMGUI_TEST_ENGINE_ITEM_INFO(id, strId, g.lastItemData.statusFlags)
        return pressed
    }

    fun arrowButton(id: String, dir: Dir): Boolean = arrowButtonEx(id, dir, Vec2(frameHeight), emptyFlags())

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
        if (!itemAdd(totalBb, id)) {
            IMGUI_TEST_ENGINE_ITEM_INFO(id, label, g.lastItemData.statusFlags or ItemStatusFlag.Checkable or if (v) ItemStatusFlag.Checked else emptyFlags())
            return false
        }

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
        val mixedValue = g.lastItemData.inFlags has ItemFlag.MixedValue
        if (mixedValue) {
            // Undocumented tristate/mixed/indeterminate checkbox (#2644)
            val pad = Vec2(1f max floor(squareSz / 3.6f))
            window.drawList.addRectFilled(checkBb.min + pad, checkBb.max - pad, checkCol, style.frameRounding)
        } else if (v) {
            val pad = 1f max floor(squareSz / 6f)
            window.drawList.renderCheckMark(checkBb.min + pad, checkCol, squareSz - pad * 2f)
        }

        val renderTextPos = Vec2(checkBb.max.x + style.itemInnerSpacing.x, checkBb.min.y + style.framePadding.y)

        if (g.logEnabled)
            logRenderedText(renderTextPos, if (mixedValue) "[~]" else if (v) "[x]" else "[ ]")
        if (labelSize.x > 0f)
            renderText(renderTextPos, label)

        val flags = ItemStatusFlag.Checkable or if (v) ItemStatusFlag.Checked else emptyFlags()
        IMGUI_TEST_ENGINE_ITEM_INFO(id, label, g.lastItemData.statusFlags or flags)

        return pressed
    }

    // We use JvmName to ensure that the function can be seen in Java as checkboxFlags
    // Suppressing the warning since we're in an interface.
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("checkboxFlags")
    fun <F : Flag<F>> checkboxFlags(label: String, flags: FlagArray<F>, flagsValue: Flag<F>): Boolean {
        _b = flagsValue in flags[0] // ~allOn
        val anyOn = flags[0] has flagsValue
        val pressed = when {
            !_b && anyOn -> {
                val window = currentWindow
                val backupItemFlags = g.currentItemFlags
                g.currentItemFlags = g.currentItemFlags or ItemFlag.MixedValue
                checkbox(label, ::_b).also {
                    g.currentItemFlags = backupItemFlags
                }
            }
            else -> checkbox(label, ::_b)
        }
        if (pressed)
            flags[0] = when {
                _b -> flags[0] or flagsValue
                else -> flags[0] wo flagsValue
            }
        return pressed
    }

    fun <F : Flag<F>> checkboxFlags(label: String, flagsPtr: KMutableProperty0<Flag<F>>, flagsValue: Flag<F>): Boolean {
        var flags by flagsPtr
        val v = booleanArrayOf(flagsValue in flags)
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

        val renderTextPos = Vec2(checkBb.max.x + style.itemInnerSpacing.x, checkBb.min.y + style.framePadding.y)
        if (g.logEnabled)
            logRenderedText(renderTextPos, if (active) "(x)" else "( )")
        if (labelSize.x > 0f)
            renderText(renderTextPos, label)

        IMGUI_TEST_ENGINE_ITEM_INFO(id, label, g.lastItemData.statusFlags)
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

    fun progressBar(fraction_: Float, sizeArg: Vec2 = Vec2(-Float.MIN_VALUE, 0f), overlay_: String = "") {
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