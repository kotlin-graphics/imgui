package imgui.api

import glm_.max
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.Col
import imgui.Flag
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.currentWindow
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.popStyleColor
import imgui.ImGui.popTextWrapPos
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushTextWrapPos
import imgui.ImGui.renderBullet
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.separatorTextEx
import imgui.ImGui.style
import imgui.ImGui.textEx
import imgui.dsl
import imgui.get
import imgui.internal.classes.Rect
import imgui.internal.formatStringToTempBuffer
import imgui.internal.sections.TextFlag


/** Widgets: Text */
interface widgetsText {

    /** raw text without formatting. Roughly equivalent to Text("%s", text) but:
     *  A) doesn't require null terminated string if 'text_end' is specified,
     *  B) it's faster, no memory copy is done, no buffer size limits, recommended for long chunks of text. */
    fun textUnformatted(text: String, textEnd: Int = -1) = textEx(text, textEnd, TextFlag.NoWidthForLargeClippedText)

    /** formatted text */
    fun text(fmt: String, vararg args: Any) {

        val window = currentWindow
        if (window.skipItems) return

        val textEnd = formatStringToTempBuffer(fmt, *args)
        textEx(g.tempBuffer, textEnd, TextFlag.NoWidthForLargeClippedText)
    }

    /** shortcut for
     *      PushStyleColor(ImGuiCol_Text, col);
     *      Text(fmt, ...);
     *      PopStyleColor();   */
    fun textColored(col: Vec4, fmt: String, vararg args: Any) {
        pushStyleColor(Col.Text, col)
        text(fmt, *args)
        popStyleColor()
    }

    /** shortcut for:
     *      pushStyleColor(Col.Text, style.colors[Col.TextDisabled])
     *      text(fmt, ...)
     *      popStyleColor() */
    fun textDisabled(fmt: String, vararg args: Any) =
            dsl.withStyleColor(Col.Text, style.colors[Col.TextDisabled]) {
                text(fmt, *args)
            }

    /** shortcut for PushTextWrapPos(0.0f); Text(fmt, ...); PopTextWrapPos();. Note that this won't work on an
     *  auto-resizing window if there's no other widgets to extend the window width, yoy may need to set a size using
     *  SetNextWindowSize().    */
    fun textWrapped(fmt: String, vararg args: Any) {
        val needBackup = g.currentWindow!!.dc.textWrapPos < 0f  // Keep existing wrap position is one ia already set
        if (needBackup)
            pushTextWrapPos(0f)
        text(fmt, *args)
        if (needBackup)
            popTextWrapPos()
    }

    //    IMGUI_API void          TextWrappedV(const char* fmt, va_list args)                     IM_FMTLIST(1);

    /** Display text+label aligned the same way as value+label widgets  */
    fun labelText(label: String, fmt: String, vararg args: Any) {

        val window = currentWindow
        if (window.skipItems) return
        val w = calcItemWidth()

        val valueTextBegin = g.tempBuffer
        val valueTextEnd = formatStringToTempBuffer(fmt, args)
        val valueSize = calcTextSize(valueTextBegin, 0, valueTextEnd, false)
        val labelSize = calcTextSize(label, hideTextAfterDoubleHash = true)

        val pos = window.dc.cursorPos // [JVM] careful, same instance
        val valueBb = Rect(pos, pos + Vec2(w, valueSize.y + style.framePadding.y * 2))
        val totalBb = Rect(pos, pos + Vec2(w + if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, (valueSize.y max labelSize.y) + style.framePadding.y * 2))
        itemSize(totalBb, style.framePadding.y)
        if (!itemAdd(totalBb, 0))
            return
        // Render
        renderTextClipped(valueBb.min + style.framePadding, valueBb.max, valueTextBegin, valueTextEnd, valueSize, Vec2(0f, 0.5f))
        if (labelSize.x > 0f)
            renderText(Vec2(valueBb.max.x + style.itemInnerSpacing.x, valueBb.min.y + style.framePadding.y), label)
    }

    /** shortcut for Bullet()+Text()
     *
     *  Text with a little bullet aligned to the typical tree node. */
    fun bulletText(fmt: String, vararg args: Any) {

        val window = currentWindow
        if (window.skipItems)
            return

        val text = fmt.format(style.locale, *args.map { if (it is Flag<*>) it.i else it }.toTypedArray())
        val labelSize = calcTextSize(text, hideTextAfterDoubleHash = false)
        val totalSize = Vec2(g.fontSize + if (labelSize.x > 0f) (labelSize.x + style.framePadding.x * 2) else 0f, labelSize.y)  // Empty text doesn't add padding
        val pos = Vec2(window.dc.cursorPos)
        pos.y += window.dc.currLineTextBaseOffset
        itemSize(totalSize, 0f)
        val bb = Rect(pos, pos + totalSize)
        if (!itemAdd(bb, 0)) return

        // Render
        val textCol = Col.Text.u32
        window.drawList.renderBullet(bb.min + Vec2(style.framePadding.x + g.fontSize * 0.5f, g.fontSize * 0.5f), textCol)
        renderText(bb.min + Vec2(g.fontSize + style.framePadding.x * 2, 0f), text, false)
    }

    // currently: formatted text with an horizontal line
    fun separatorText(label: String) {

        val window = currentWindow
        if (window.skipItems)
            return

        // The SeparatorText() vs SeparatorTextEx() distinction is designed to be considerate that we may want:
        // - allow separator-text to be draggable items (would require a stable ID + a noticeable highlight)
        // - this high-level entry point to allow formatting? (which in turns may require ID separate from formatted string)
        // - because of this we probably can't turn 'const char* label' into 'const char* fmt, ...'
        // Otherwise, we can decide that users wanting to drag this would layout a dedicated drag-item,
        // and then we can turn this into a format function.
        separatorTextEx(0, label, findRenderedTextEnd(label), 0f)
    }
}
