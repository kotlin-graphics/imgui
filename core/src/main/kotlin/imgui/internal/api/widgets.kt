package imgui.internal.api

import glm_.func.common.max
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.buttonBehavior
import imgui.ImGui.calcItemSize
import imgui.ImGui.calcTextSize
import imgui.ImGui.calcWrapWidthForPos
import imgui.ImGui.currentWindow
import imgui.ImGui.frameHeight
import imgui.ImGui.getColorU32
import imgui.ImGui.hoveredId
import imgui.ImGui.io
import imgui.ImGui.isClippedEx
import imgui.ImGui.isItemActive
import imgui.ImGui.isMouseDragging
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.keepAliveID
import imgui.ImGui.logRenderedText
import imgui.ImGui.logText
import imgui.ImGui.popColumnsBackground
import imgui.ImGui.pushColumnsBackground
import imgui.ImGui.renderFrame
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.renderTextWrapped
import imgui.ImGui.startMouseMovingWindowOrNode
import imgui.ImGui.style
import imgui.api.g
import imgui.internal.*
import imgui.internal.classes.DockNode
import imgui.internal.classes.Rect
import imgui.internal.sections.*
import uno.kotlin.getValue
import uno.kotlin.setValue
import kotlin.math.max
import kotlin.reflect.KMutableProperty0

/** Widgets */
internal interface widgets {

    /** Raw text without formatting. Roughly equivalent to text("%s", text) but:
     *  A) doesn't require null terminated string if 'textEnd' is specified
     *  B) it's faster, no memory copy is done, no buffer size limits, recommended for long chunks of text. */
    fun textEx(text: String, textEnd: Int = -1, flag: TextFlag = TextFlag.None) {
        val bytes = text.toByteArray()
        textEx(bytes, if (textEnd != -1) textEnd else bytes.strlen())
    }

    /** Raw text without formatting. Roughly equivalent to text("%s", text) but:
     *  A) doesn't require null terminated string if 'textEnd' is specified
     *  B) it's faster, no memory copy is done, no buffer size limits, recommended for long chunks of text. */
    fun textEx(text: ByteArray, textEnd: Int = text.strlen(), flags: TextFlags = TextFlag.None.i) {

        val window = currentWindow
        if (window.skipItems) return

        val textPos = Vec2(window.dc.cursorPos.x, window.dc.cursorPos.y + window.dc.currLineTextBaseOffset)
        val wrapPosX = window.dc.textWrapPos
        val wrapEnabled = wrapPosX >= 0f
        if (textEnd > 2000 && !wrapEnabled) {
            // Long text!
            // Perform manual coarse clipping to optimize for long multi-line text
            // - From this point we will only compute the width of lines that are visible. Optimization only available when word-wrapping is disabled.
            // - We also don't vertically center the text within the line full height, which is unlikely to matter because we are likely the biggest and only item on the line.
            // - We use memchr(), pay attention that well optimized versions of those str/mem functions are much faster than a casually written loop.
            var line = 0
            val lineHeight = ImGui.textLineHeight
            val textSize = Vec2()

            // Lines to skip (can't skip when logging text)
            val pos = Vec2(textPos)
            if (!g.logEnabled) {
                val linesSkippable = ((window.clipRect.min.y - textPos.y) / lineHeight).i
                if (linesSkippable > 0) {
                    var linesSkipped = 0
                    while (line < textEnd && linesSkipped < linesSkippable) {
                        var lineEnd = text.memchr(line, '\n', textEnd - line)
                        if (lineEnd == -1)
                            lineEnd = textEnd
                        if (flags hasnt TextFlag.NoWidthForLargeClippedText)
                            textSize.x = textSize.x max calcTextSize(text, line, lineEnd).x
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
                    if (isClippedEx(lineRect, 0, false))
                        break

                    var lineEnd = text.memchr(line, '\n', textEnd - line)
                    if (lineEnd == -1)
                        lineEnd = textEnd
                    textSize.x = textSize.x max calcTextSize(text, line, lineEnd).x
                    renderText(pos, text, line, lineEnd, false)
                    line = lineEnd + 1
                    lineRect.min.y += lineHeight
                    lineRect.max.y += lineHeight
                    pos.y += lineHeight
                }

                // Count remaining lines
                var linesSkipped = 0
                while (line < textEnd) {
                    var lineEnd = text.memchr(line, '\n', textEnd - line)
                    if (lineEnd == -1)
                        lineEnd = textEnd
                    if (flags hasnt TextFlag.NoWidthForLargeClippedText)
                        textSize.x = textSize.x max calcTextSize(text, line, lineEnd).x
                    line = lineEnd + 1
                    linesSkipped++
                }
                pos.y += linesSkipped * lineHeight
            }
            textSize.y = pos.y - textPos.y

            val bb = Rect(textPos, textPos + textSize)
            itemSize(textSize, 0f)
            itemAdd(bb, 0)
        } else {
            val wrapWidth = if (wrapEnabled) calcWrapWidthForPos(window.dc.cursorPos, wrapPosX) else 0f
            val textSize = calcTextSize(text, 0, textEnd, false, wrapWidth)

            val bb = Rect(textPos, textPos + textSize)
            itemSize(textSize, 0f)
            if (!itemAdd(bb, 0)) return

            // Render (we don't hide text after ## in this end-user function)
            renderTextWrapped(bb.min, text, textEnd, wrapWidth)
        }
    }

    fun String.memchr(startIdx: Int, c: Char): Int? {
        val res = indexOf(c, startIdx)
        return if (res >= 0) res else null
    }

    fun buttonEx(label: String, sizeArg: Vec2 = Vec2(), flags_: Int = 0): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getID(label)
        val labelSize = calcTextSize(label, hideTextAfterDoubleHash = true)

        val pos = Vec2(window.dc.cursorPos)
        /*  Try to vertically align buttons that are smaller/have no padding so that text baseline matches (bit hacky,
            since it shouldn't be a flag)         */
        if (flags_ has ButtonFlag.AlignTextBaseLine && style.framePadding.y < window.dc.currLineTextBaseOffset)
            pos.y += window.dc.currLineTextBaseOffset - style.framePadding.y
        val size = calcItemSize(sizeArg, labelSize.x + style.framePadding.x * 2f, labelSize.y + style.framePadding.y * 2f)

        val bb = Rect(pos, pos + size)
        itemSize(size, style.framePadding.y)
        if (!itemAdd(bb, id)) return false

        var flags = flags_
        if (window.dc.itemFlags has ItemFlag.ButtonRepeat) flags = flags or ButtonFlag.Repeat
        val (pressed, hovered, held) = buttonBehavior(bb, id, flags)

        // Render
        val col = if (hovered && held) Col.ButtonActive else if (hovered) Col.ButtonHovered else Col.Button
        renderNavHighlight(bb, id)
        renderFrame(bb.min, bb.max, col.u32, true, style.frameRounding)
        renderTextClipped(bb.min + style.framePadding, bb.max - style.framePadding, label, labelSize, style.buttonTextAlign, bb)

        // Automatically close popups
        //if (pressed && !(flags & ImGuiButtonFlags_DontClosePopups) && (window->Flags & ImGuiWindowFlags_Popup))
        //    CloseCurrentPopup();

        Hook.itemInfo?.invoke(g, id, label, window.dc.lastItemStatusFlags)
        return pressed
    }

    /* Button to close a window    */
    fun closeButton(id: ID, pos: Vec2): Boolean {

        val window = currentWindow

        /*  We intentionally allow interaction when clipped so that a mechanical Alt, Right, Validate sequence close
            a window. (this isn't the regular behavior of buttons, but it doesn't affect the user much because
            navigation tends to keep items visible).   */
        val bb = Rect(pos, pos + g.fontSize + style.framePadding * 2f)
        val isClipped = !itemAdd(bb, id)

        val (pressed, hovered, held) = buttonBehavior(bb, id)
        if (isClipped) return pressed

        // Render
        val center = Vec2(bb.center)
        if (hovered) {
            val col = if (held) Col.ButtonActive else Col.ButtonHovered
            window.drawList.addCircleFilled(center, 2f max (g.fontSize * 0.5f + 1f), col.u32, 12)
        }

        val crossExtent = g.fontSize * 0.5f * 0.7071f - 1f
        val crossCol = Col.Text.u32
        center -= 0.5f
        window.drawList.addLine(center + crossExtent, center - crossExtent, crossCol, 1f)
        window.drawList.addLine(center + Vec2(crossExtent, -crossExtent), center + Vec2(-crossExtent, crossExtent), crossCol, 1f)

        return pressed
    }

    /** The Collapse button also functions as a Dock Menu button. */
    fun collapseButton(id: ID, pos: Vec2, dockNode: DockNode?): Boolean {
        val window = g.currentWindow!!
        val bb = Rect(pos, pos + g.fontSize + style.framePadding * 2f)
        itemAdd(bb, id)
        val (pressed, hovered, held) = buttonBehavior(bb, id, ButtonFlag.None)

        // Render
        //bool is_dock_menu = (window->DockNodeAsHost && !window->Collapsed);
        val off = if(dockNode != null) Vec2(floor(-style.itemInnerSpacing.x * 0.5f) + 0.5f, 0f) else Vec2(0f)
        val bgCol = if (held && hovered) Col.ButtonActive else if (hovered) Col.ButtonHovered else Col.Button
        val textCol = Col.Text.u32
        val center = bb.center
        if (hovered || held)
            window.drawList.addCircleFilled(center + off + Vec2(0f, -0.5f), g.fontSize * 0.5f + 1f, bgCol.u32, 12)

        if (dockNode != null)
            window.drawList.renderArrowDockMenu(bb.min + style.framePadding, g.fontSize, textCol)
        else
            window.drawList.renderArrow(bb.min + style.framePadding, textCol, if(window.collapsed) Dir.Right else Dir.Down, 1f)

        // Switch to moving the window after mouse is moved beyond the initial drag threshold
        if (isItemActive && isMouseDragging(MouseButton.Left))
            startMouseMovingWindowOrNode(window, dockNode, true)

        return pressed
    }

    /** square button with an arrow shape */
    fun arrowButtonEx(strId: String, dir: Dir, size: Vec2, flags_: ButtonFlags = ButtonFlag.None.i): Boolean {

        var flags = flags_

        val window = currentWindow
        if (window.skipItems) return false

        val id = window.getID(strId)
        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        val defaultSize = frameHeight
        itemSize(size, if (size.y >= defaultSize) style.framePadding.y else -1f)
        if (!itemAdd(bb, id)) return false

        if (window.dc.itemFlags has ItemFlag.ButtonRepeat)
            flags = flags or ButtonFlag.Repeat

        val (pressed, hovered, held) = buttonBehavior(bb, id, flags)

        // Render
        val bgCol = if (held && hovered) Col.ButtonActive else if (hovered) Col.ButtonHovered else Col.Button
        val textCol = Col.Text
        renderNavHighlight(bb, id)
        renderFrame(bb.min, bb.max, bgCol.u32, true, g.style.frameRounding)
        window.drawList.renderArrow(bb.min + Vec2(max(0f, (size.x - g.fontSize) * 0.5f), max(0f, (size.y - g.fontSize) * 0.5f)), textCol.u32, dir)

        return pressed
    }

    /** Vertical scrollbar
     *  The entire piece of code below is rather confusing because:
     *  - We handle absolute seeking (when first clicking outside the grab) and relative manipulation (afterward or when
     *          clicking inside the grab)
     *  - We store values as normalized ratio and in a form that allows the window content to change while we are holding on
     *          a scrollbar
     *  - We handle both horizontal and vertical scrollbars, which makes the terminology not ideal. */
    infix fun scrollbar(axis: Axis) {

        val window = g.currentWindow!!

        val id = window getScrollbarID axis
        keepAliveID(id)

        // Calculate scrollbar bounding box
        val bb = window getScrollbarRect axis
        var roundingCorners = DrawCornerFlag.None.i
        if (axis == Axis.X) {
            roundingCorners = roundingCorners or DrawCornerFlag.BotLeft
            if (!window.scrollbar.y)
                roundingCorners = roundingCorners or DrawCornerFlag.BotRight
        } else {
            if (window.flags has WindowFlag.NoTitleBar && window.flags hasnt WindowFlag.MenuBar)
                roundingCorners = roundingCorners or DrawCornerFlag.TopRight
            if (!window.scrollbar.x)
                roundingCorners = roundingCorners or DrawCornerFlag.BotRight
        }
        val sizeAvail = window.innerRect.max[axis] - window.innerRect.min[axis]
        val sizeContents = window.contentSize[axis] + window.windowPadding[axis] * 2f
        scrollbarEx(bb, id, axis, if (axis == Axis.X) window.scroll::x else window.scroll::y, sizeAvail, sizeContents, roundingCorners)
    }

    /** Vertical/Horizontal scrollbar
     *  The entire piece of code below is rather confusing because:
     *  - We handle absolute seeking (when first clicking outside the grab) and relative manipulation (afterward or when clicking inside the grab)
     *  - We store values as normalized ratio and in a form that allows the window content to change while we are holding on a scrollbar
     *  - We handle both horizontal and vertical scrollbars, which makes the terminology not ideal.
     *  Still, the code should probably be made simpler..   */
    fun scrollbarEx(bbFrame: Rect, id: ID, axis: Axis, pScrollV: KMutableProperty0<Float>, sizeAvailV: Float, sizeContentsV: Float, roundingCorners: DrawCornerFlags): Boolean {

        var scrollV by pScrollV

        val window = g.currentWindow!!
        if (window.skipItems)
            return false

        val bbFrameWidth = bbFrame.width
        val bbFrameHeight = bbFrame.height
        if (bbFrameWidth <= 0f || bbFrameHeight <= 0f)
            return false

        // When we are too small, start hiding and disabling the grab (this reduce visual noise on very small window and facilitate using the window resize grab)
        var alpha = 1f
        if (axis == Axis.Y && bbFrameHeight < g.fontSize + style.framePadding.y * 2f)
            alpha = saturate((bbFrameHeight - g.fontSize) / (style.framePadding.y * 2f))
        if (alpha <= 0f)
            return false

        val allowInteraction = alpha >= 1f

        val bb = Rect(bbFrame)
        bb.expand(Vec2(-clamp(floor((bbFrameWidth - 2f) * 0.5f), 0f, 3f), -clamp(floor((bbFrameHeight - 2f) * 0.5f), 0f, 3f)))

        // V denote the main, longer axis of the scrollbar (= height for a vertical scrollbar)
        val scrollbarSizeV = if (axis == Axis.X) bb.width else bb.height

        // Calculate the height of our grabbable box. It generally represent the amount visible (vs the total scrollable amount)
        // But we maintain a minimum size in pixel to allow for the user to still aim inside.
        assert(max(sizeContentsV, sizeAvailV) > 0f) { "Adding this assert to check if the ImMax(XXX,1.0f) is still needed. PLEASE CONTACT ME if this triggers." }
        val winSizeV = max(sizeContentsV max sizeAvailV, 1f)
        val grabHPixels = clamp(scrollbarSizeV * (sizeAvailV / winSizeV), style.grabMinSize, scrollbarSizeV)
        val grabHNorm = grabHPixels / scrollbarSizeV

        // Handle input right away. None of the code of Begin() is relying on scrolling position before calling Scrollbar().
        val (_, hovered, held) = buttonBehavior(bb, id, ButtonFlag.NoNavFocus)

        val scrollMax = max(1f, sizeContentsV - sizeAvailV)
        var scrollRatio = saturate(scrollV / scrollMax)
        var grabVNorm = scrollRatio * (scrollbarSizeV - grabHPixels) / scrollbarSizeV  // Grab position in normalized space
        if (held && allowInteraction && grabHNorm < 1f) {
            val scrollbarPosV = bb.min[axis]
            val mousePosV = io.mousePos[axis]

            // Click position in scrollbar normalized space (0.0f->1.0f)
            val clickedVNorm = saturate((mousePosV - scrollbarPosV) / scrollbarSizeV)
            hoveredId = id

            var seekAbsolute = false
            if (g.activeIdIsJustActivated) {
                // On initial click calculate the distance between mouse and the center of the grab
                seekAbsolute = (clickedVNorm < grabVNorm || clickedVNorm > grabVNorm + grabHNorm)
                g.scrollbarClickDeltaToGrabCenter = when {
                    seekAbsolute -> 0f
                    else -> clickedVNorm - grabVNorm - grabHNorm * 0.5f
                }
            }

            // Apply scroll (p_scroll_v will generally point on one member of window->Scroll)
            // It is ok to modify Scroll here because we are being called in Begin() after the calculation of ContentSize and before setting up our starting position
            val scrollVNorm = saturate((clickedVNorm - g.scrollbarClickDeltaToGrabCenter - grabHNorm * 0.5f) / (1f - grabHNorm))
            scrollV = round(scrollVNorm * scrollMax) //(win_size_contents_v - win_size_v));

            // Update values for rendering
            scrollRatio = saturate(scrollV / scrollMax)
            grabVNorm = scrollRatio * (scrollbarSizeV - grabHPixels) / scrollbarSizeV

            // Update distance to grab now that we have seeked and saturated
            if (seekAbsolute)
                g.scrollbarClickDeltaToGrabCenter = clickedVNorm - grabVNorm - grabHNorm * 0.5f
        }

        // Render
        val bgCol = Col.ScrollbarBg.u32
        val grabCol = getColorU32(when {
            held -> Col.ScrollbarGrabActive
            hovered -> Col.ScrollbarGrabHovered
            else -> Col.ScrollbarGrab
        }, alpha)
        window.drawList.addRectFilled(bbFrame.min, bbFrame.max, bgCol, window.windowRounding, roundingCorners)
        val grabRect = when (axis) {
            Axis.X -> Rect(lerp(bb.min.x, bb.max.x, grabVNorm), bb.min.y, lerp(bb.min.x, bb.max.x, grabVNorm) + grabHPixels, bb.max.y)
            else -> Rect(bb.min.x, lerp(bb.min.y, bb.max.y, grabVNorm), bb.max.x, lerp(bb.min.y, bb.max.y, grabVNorm) + grabHPixels)
        }
        window.drawList.addRectFilled(grabRect.min, grabRect.max, grabCol, style.scrollbarRounding)

        return held
    }

    /** ImageButton() is flawed as 'id' is always derived from 'texture_id' (see #2464 #1390)
     *  We provide this internal helper to write your own variant while we figure out how to redesign the public ImageButton() API. */
    fun imageButtonEx(
            id: ID, textureId: TextureID, size: Vec2, uv0: Vec2, uv1: Vec2, padding: Vec2, bgCol: Vec4, tintCol: Vec4,
    ): Boolean {

        val window = currentWindow
        if (window.skipItems)
            return false

        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + size + padding * 2)

        itemSize(bb)
        if (!itemAdd(bb, id))
            return false
        val (pressed, hovered, held) = buttonBehavior(bb, id)

        // Render
        val col = getColorU32(if(held && hovered) Col.ButtonActive else if(hovered) Col.ButtonHovered else Col.Button)
        renderNavHighlight(bb, id)
        renderFrame(bb.min, bb.max, col, true, clamp(min(padding.x, padding.y), 0f, style.frameRounding))
        if (bgCol.w > 0f)
            window.drawList.addRectFilled(bb.min + padding, bb.max - padding, bgCol.u32)
        window.drawList.addImage(textureId, bb.min + padding, bb.max - padding, uv0, uv1, tintCol.u32)

        return pressed
    }

    // GetWindowScrollbarRect -> Window class
    // GetWindowScrollbarID -> Window class

    /** Horizontal/vertical separating line
     *  Separator, generally horizontal. inside a menu bar or in horizontal layout mode, this becomes a vertical separator. */
    fun separatorEx(flags: SeparatorFlags) {

        val window = currentWindow
        if (window.skipItems) return

        assert((flags and (SeparatorFlag.Horizontal or SeparatorFlag.Vertical)).isPowerOfTwo) { "Check that only 1 option is selected" }

        val thicknessDraw = 1f
        val thicknessLayout = 0f
        if (flags has SeparatorFlag.Vertical) {
            // Vertical separator, for menu bars (use current line height). Not exposed because it is misleading and it doesn't have an effect on regular layout.
            val y1 = window.dc.cursorPos.y
            val y2 = window.dc.cursorPos.y + window.dc.currLineSize.y
            val bb = Rect(Vec2(window.dc.cursorPos.x, y1), Vec2(window.dc.cursorPos.x + thicknessDraw, y2))
            itemSize(Vec2(thicknessLayout, 0f))
            if (!itemAdd(bb, 0))
                return
            // Draw
            window.drawList.addLine(Vec2(bb.min.x, bb.min.y), Vec2(bb.min.x, bb.max.y), Col.Separator.u32)
            if (g.logEnabled)
                logText(" |")

        } else if (flags has SeparatorFlag.Horizontal) {
            // Horizontal Separator
            var x1 = window.pos.x
            val x2 = window.pos.x + window.size.x
            // FIXME-WORKRECT: old hack (#205) until we decide of consistent behavior with WorkRect/Indent and Separator
            if (g.groupStack.isNotEmpty() && g.groupStack.last().windowID == window.id)
                x1 += window.dc.indent

            val columns = window.dc.currentColumns.takeIf { flags has SeparatorFlag.SpanAllColumns }
            if (columns != null)
                pushColumnsBackground()

            // We don't provide our width to the layout so that it doesn't get feed back into AutoFit
            val bb = Rect(Vec2(x1, window.dc.cursorPos.y), Vec2(x2, window.dc.cursorPos.y + thicknessDraw))
            itemSize(Vec2(0f, thicknessLayout))
            val itemVisible = itemAdd(bb, 0)
            if (itemVisible) {
                // Draw
                window.drawList.addLine(bb.min, Vec2(bb.max.x, bb.min.y), Col.Separator.u32)
                if (g.logEnabled)
                    logRenderedText(bb.min, "--------------------------------")
            }
            columns?.let {
                popColumnsBackground()
                it.lineMinY = window.dc.cursorPos.y
            }
        }
    }
}