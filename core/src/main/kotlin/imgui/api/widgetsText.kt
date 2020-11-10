package imgui.api

import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.currentWindow
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.popStyleColor
import imgui.ImGui.popTextWrapPos
import imgui.ImGui.pushStyleColor
import imgui.ImGui.pushTextWrapPos
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.style
import imgui.ImGui.textEx
import imgui.internal.classes.Rect
import imgui.internal.sections.TextFlag
import imgui.internal.formatString


/** Widgets: Text */
interface widgetsText {

    /** raw text without formatting. Roughly equivalent to Text("%s", text) but:
     *  A) doesn't require null terminated string if 'text_end' is specified,
     *  B) it's faster, no memory copy is done, no buffer size limits, recommended for long chunks of text. */
    fun textUnformatted(text: String, textEnd: Int = -1) =
            textEx(text, textEnd, TextFlag.NoWidthForLargeClippedText)

    /** formatted text */
    fun text(fmt: String, vararg args: Any) {

        val window = currentWindow
        if (window.skipItems) return

        val textEnd = formatString(g.tempBuffer, fmt, *args)
        textEx(g.tempBuffer, textEnd, TextFlag.NoWidthForLargeClippedText.i)
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

    fun textColoredV(col: Vec4, fmt: String, vararg args: Any) = dsl.withStyleColor(Col.Text, col) {
        if (fmt == "%s")
            TODO()
//            textEx(va_arg(args, const char*), NULL, ImGuiTextFlags_NoWidthForLargeClippedText); // Skip formatting
        else
            text(fmt, *args)
    }

    /** shortcut for:
     *      pushStyleColor(Col.Text, style.colors[Col.TextDisabled])
     *      text(fmt, ...)
     *      popStyleColor() */
    fun textDisabled(fmt: String, vararg args: Any) = dsl.withStyleColor(Col.Text, style.colors[Col.TextDisabled]) {
        if (fmt == "%s")
            TODO()
//            TextEx(va_arg(args, const char*), NULL, ImGuiTextFlags_NoWidthForLargeClippedText); // Skip formatting
        else
            text(fmt, *args)
    }

    /** shortcut for PushTextWrapPos(0.0f); Text(fmt, ...); PopTextWrapPos();. Note that this won't work on an
     *  auto-resizing window if there's no other widgets to extend the window width, yoy may need to set a size using
     *  SetNextWindowSize().    */
    fun textWrapped(fmt: String, vararg args: Any) {
        val needBackup = g.currentWindow!!.dc.textWrapPos < 0f  // Keep existing wrap position is one ia already set
        if (needBackup)
            pushTextWrapPos(0f)
        if (fmt == "%s")
            TODO()
//            TextEx(va_arg(args, const char*), NULL, ImGuiTextFlags_NoWidthForLargeClippedText); // Skip formatting
        else
            text(fmt, *args)
        if (needBackup)
            popTextWrapPos()
    }

//    IMGUI_API void          TextWrappedV(const char* fmt, va_list args)                     IM_FMTLIST(1);

    /** Display text+label aligned the same way as value+label widgets  */
    fun labelText(label: String, fmt: String, vararg args: Any) = labelTextV(label, fmt, args)

    /** Add a label+text combo aligned to other label+value widgets */
    fun labelTextV(label: String, fmt: String, args: Array<out Any>) {

        val window = currentWindow
        if (window.skipItems) return
        val w = calcItemWidth()

        val labelSize = calcTextSize(label, hideTextAfterDoubleHash =  true)
        val valueBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w, labelSize.y + style.framePadding.y * 2))
        val totalBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w + if (labelSize.x > 0f) style.itemInnerSpacing.x else 0f, style.framePadding.y * 2) + labelSize)
        itemSize(totalBb, style.framePadding.y)
        if (!itemAdd(totalBb, 0)) return
        // Render
        val text = fmt.format(style.locale, *args)
        renderTextClipped(valueBb.min, valueBb.max, text, null, Vec2(0f, 0.5f))
        if (labelSize.x > 0f)
            renderText(Vec2(valueBb.max.x + style.itemInnerSpacing.x, valueBb.min.y + style.framePadding.y), label)
    }

    /** shortcut for Bullet()+Text()    */
    fun bulletText(fmt: String, vararg args: Any) = bulletTextV(fmt, args)

    /** Text with a little bullet aligned to the typical tree node. */
    fun bulletTextV(fmt: String, args: Array<out Any>) {

        val window = currentWindow
        if (window.skipItems) return

        val text = fmt.format(style.locale, *args)
        val labelSize = calcTextSize(text, hideTextAfterDoubleHash =  false)
        val totalSize = Vec2(g.fontSize + if(labelSize.x > 0f) (labelSize.x + style.framePadding.x * 2) else 0f, labelSize.y)  // Empty text doesn't add padding
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
}