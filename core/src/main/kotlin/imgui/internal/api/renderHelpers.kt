package imgui.internal.api

import glm_.b
import glm_.bool
import glm_.f
import glm_.func.common.max
import glm_.vec1.Vec1i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.calcTextSize
import imgui.ImGui.currentWindow
import imgui.ImGui.getColorU32
import imgui.ImGui.logText
import imgui.ImGui.style
import imgui.api.g
import imgui.classes.DrawList
import imgui.internal.*
import imgui.internal.classes.Rect
import imgui.internal.sections.*
import unsigned.toUInt
import kotlin.math.max
import imgui.internal.sections.DrawCornerFlag as Dcf

/** Render helpers
 *  AVOID USING OUTSIDE OF IMGUI.CPP! NOT FOR PUBLIC CONSUMPTION. THOSE FUNCTIONS ARE A MESS. THEIR SIGNATURE AND BEHAVIOR WILL CHANGE, THEY NEED TO BE REFACTORED INTO SOMETHING DECENT.
 *  NB: All position are in absolute pixels coordinates (we are never using window coordinates internally) */
internal interface renderHelpers {

    fun renderText(pos: Vec2, text: String, hideTextAfterHash: Boolean = true) {
        val bytes = text.toByteArray()
        renderText(pos, bytes, 0, bytes.size, hideTextAfterHash)
    }

    fun renderText(pos: Vec2, text: ByteArray, textBegin: Int = 0, textEnd: Int = -1, hideTextAfterHash: Boolean = true) {

        val window = g.currentWindow!!

        // Hide anything after a '##' string
        val textDisplayEnd = when {
            hideTextAfterHash -> findRenderedTextEnd(text, textBegin, textEnd)
            textEnd == -1 -> text.strlen(textBegin)
            else -> textEnd
        }

        if (textBegin != textDisplayEnd) {
            window.drawList.addText(g.font, g.fontSize, pos, Col.Text.u32, text, textBegin, textDisplayEnd)
            if (g.logEnabled)
                logRenderedText(pos, String(text, textBegin, textEnd - textBegin), textDisplayEnd)
        }
    }

    fun renderTextWrapped(pos: Vec2, text: ByteArray, textEnd_: Int, wrapWidth: Float) {

        val window = g.currentWindow!!

        val textEnd = if (textEnd_ == -1) text.strlen() else textEnd_ // FIXME-OPT

        if (textEnd > 0) {
            window.drawList.addText(g.font, g.fontSize, pos, Col.Text.u32, text, 0, textEnd, wrapWidth)
            if (g.logEnabled) logRenderedText(pos, text.cStr, textEnd)
        }
    }

    fun renderTextClipped(posMin: Vec2, posMax: Vec2, text: String, textSizeIfKnown: Vec2? = null,
                          align: Vec2 = Vec2(), clipRect: Rect? = null) {
        val bytes = text.toByteArray()
        renderTextClipped(posMin, posMax, bytes, bytes.size, textSizeIfKnown, align, clipRect)
    }

    fun renderTextClipped(posMin: Vec2, posMax: Vec2, text: ByteArray, textEnd: Int = text.size, textSizeIfKnown: Vec2? = null,
                          align: Vec2 = Vec2(), clipRect: Rect? = null) {
        // Hide anything after a '##' string
        val textDisplayEnd = findRenderedTextEnd(text, 0, textEnd)
        if (textDisplayEnd == 0) return

        val window = g.currentWindow!!
        renderTextClippedEx(window.drawList, posMin, posMax, text, textDisplayEnd, textSizeIfKnown, align, clipRect)
        if (g.logEnabled)
            logRenderedText(posMax, text.cStr, textDisplayEnd)
    }

    /** Default clipRect uses (pos_min,pos_max)
     *  Handle clipping on CPU immediately (vs typically let the GPU clip the triangles that are overlapping the clipping
     *  rectangle edges)    */
    fun renderTextClippedEx(drawList: DrawList, posMin: Vec2, posMax: Vec2, text: ByteArray, textDisplayEnd: Int = -1,
                            textSizeIfKnown: Vec2? = null, align: Vec2 = Vec2(), clipRect: Rect? = null) {

        // Perform CPU side clipping for single clipped element to avoid using scissor state
        val pos = Vec2(posMin)
        val textSize = textSizeIfKnown ?: calcTextSize(text, 0, textDisplayEnd, false, 0f)

        val clipMin = clipRect?.min ?: posMin
        val clipMax = clipRect?.max ?: posMax
        var needClipping = (pos.x + textSize.x >= clipMax.x) || (pos.y + textSize.y >= clipMax.y)
        clipRect?.let {
            // If we had no explicit clipping rectangle then pos==clipMin
            needClipping = needClipping || (pos.x < clipMin.x || pos.y < clipMin.y)
        }

        // Align whole block. We should defer that to the better rendering function when we'll have support for individual line alignment.
        if (align.x > 0f) pos.x = pos.x max (pos.x + (posMax.x - pos.x - textSize.x) * align.x)
        if (align.y > 0f) pos.y = pos.y max (pos.y + (posMax.y - pos.y - textSize.y) * align.y)

        // Render
        val fineClipRect = when {
            needClipping -> Vec4(clipMin.x, clipMin.y, clipMax.x, clipMax.y)
            else -> null
        }
        drawList.addText(null, 0f, pos, Col.Text.u32, text, 0, textDisplayEnd, 0f, fineClipRect)
    }

    /** Another overly complex function until we reorganize everything into a nice all-in-one helper.
     *  This is made more complex because we have dissociated the layout rectangle (pos_min..pos_max) which define _where_ the ellipsis is, from actual clipping of text and limit of the ellipsis display.
     *  This is because in the context of tabs we selectively hide part of the text when the Close Button appears, but we don't want the ellipsis to move. */
    fun renderTextEllipsis(drawList: DrawList, posMin: Vec2, posMax: Vec2, clipMaxX: Float, ellipsisMaxX: Float,
                           text: ByteArray, textEndFull: Int = findRenderedTextEnd(text), textSizeIfKnown: Vec2?) {

        val textSize = textSizeIfKnown ?: calcTextSize(text, 0, textEndFull, false, 0f)

        //draw_list->AddLine(ImVec2(pos_max.x, pos_min.y - 4), ImVec2(pos_max.x, pos_max.y + 4), IM_COL32(0, 0, 255, 255));
        //draw_list->AddLine(ImVec2(ellipsis_max_x, pos_min.y-2), ImVec2(ellipsis_max_x, pos_max.y+2), IM_COL32(0, 255, 0, 255));
        //draw_list->AddLine(ImVec2(clip_max_x, pos_min.y), ImVec2(clip_max_x, pos_max.y), IM_COL32(255, 0, 0, 255));
        // FIXME: We could technically remove (last_glyph->AdvanceX - last_glyph->X1) from text_size.x here and save a few pixels.
        if (textSize.x > posMax.x - posMin.x) {
            /*
                Hello wo...
                |       |   |
                min   max   ellipsis_max
                         <-> this is generally some padding value
             */
            val font = drawList._data.font!!
            val fontSize = drawList._data.fontSize
            val textEndEllipsis = Vec1i(-1)

            var ellipsisChar = font.ellipsisChar
            var ellipsisCharCount = 1
            if (ellipsisChar == '\uffff') {
                ellipsisChar = '.'
                ellipsisCharCount = 3
            }
            val glyph = font.findGlyph(ellipsisChar)!!

            var ellipsisGlyphWidth = glyph.x1       // Width of the glyph with no padding on either side
            var ellipsisTotalWidth = ellipsisGlyphWidth  // Full width of entire ellipsis

            if (ellipsisCharCount > 1) {
                // Full ellipsis size without free spacing after it.
                val spacingBetweenDots = 1f * (drawList._data.fontSize / font.fontSize)
                ellipsisGlyphWidth = glyph.x1 - glyph.x0 + spacingBetweenDots
                ellipsisTotalWidth = ellipsisGlyphWidth * ellipsisCharCount.f - spacingBetweenDots
            }

            // We can now claim the space between pos_max.x and ellipsis_max.x
            val textAvailWidth = ((max(posMax.x, ellipsisMaxX) - ellipsisTotalWidth) - posMin.x) max 1f
            var textSizeClippedX = font.calcTextSizeA(fontSize, textAvailWidth, 0f, text,
                    textEnd = textEndFull, remaining = textEndEllipsis).x
            if (0 == textEndEllipsis[0] && textEndEllipsis[0] < textEndFull) {
                // Always display at least 1 character if there's no room for character + ellipsis
                textEndEllipsis[0] = textCountUtf8BytesFromChar(text, textEndFull)
                textSizeClippedX = font.calcTextSizeA(fontSize, Float.MAX_VALUE, 0f, text, textEnd = textEndEllipsis[0]).x
            }
            while (textEndEllipsis[0] > 0 && charIsBlankA(text[textEndEllipsis[0] - 1].toUInt())) {
                // Trim trailing space before ellipsis (FIXME: Supporting non-ascii blanks would be nice, for this we need a function to backtrack in UTF-8 text)
                textEndEllipsis[0]--
                textSizeClippedX -= font.calcTextSizeA(fontSize, Float.MAX_VALUE, 0f, text,
                        textEndEllipsis[0], textEndEllipsis[0] + 1).x // Ascii blanks are always 1 byte
            }

            // Render text, render ellipsis
            renderTextClippedEx(drawList, posMin, Vec2(clipMaxX, posMax.y), text, textEndEllipsis[0], textSize, Vec2())
            var ellipsisX = posMin.x + textSizeClippedX
            if (ellipsisX + ellipsisTotalWidth <= ellipsisMaxX)
                for (i in 0 until ellipsisCharCount) {
                    font.renderChar(drawList, fontSize, Vec2(ellipsisX, posMin.y), Col.Text.u32, ellipsisChar)
                    ellipsisX += ellipsisGlyphWidth
                }
        } else
            renderTextClippedEx(drawList, posMin, Vec2(clipMaxX, posMax.y), text, textEndFull, textSize, Vec2())

        if (g.logEnabled)
            logRenderedText(posMin, text.cStr, textEndFull)
    }

    /** Render a rectangle shaped with optional rounding and borders    */
    fun renderFrame(pMin: Vec2, pMax: Vec2, fillCol: Int, border: Boolean = true, rounding: Float = 0f) {

        val window = g.currentWindow!!

        window.drawList.addRectFilled(pMin, pMax, fillCol, rounding)
        val borderSize = style.frameBorderSize
        if (border && borderSize > 0f) {
            window.drawList.addRect(pMin + 1, pMax + 1, Col.BorderShadow.u32, rounding, Dcf.All.i, borderSize)
            window.drawList.addRect(pMin, pMax, Col.Border.u32, rounding, 0.inv(), borderSize)
        }
    }

    fun renderFrameBorder(pMin: Vec2, pMax: Vec2, rounding: Float = 0f) = with(g.currentWindow!!) {
        val borderSize = style.frameBorderSize
        if (borderSize > 0f) {
            drawList.addRect(pMin + 1, pMax + 1, Col.BorderShadow.u32, rounding, Dcf.All.i, borderSize)
            drawList.addRect(pMin, pMax, Col.Border.u32, rounding, 0.inv(), borderSize)
        }
    }

    /** Helper for ColorPicker4()
     *  NB: This is rather brittle and will show artifact when rounding this enabled if rounded corners overlap multiple cells. Caller currently responsible for avoiding that.
     * Spent a non reasonable amount of time trying to getting this right for ColorButton with rounding+anti-aliasing+ImGuiColorEditFlags_HalfAlphaPreview flag + various grid sizes and offsets, and eventually gave up... probably more reasonable to disable rounding altogether.
     * FIXME: uses ImGui::GetColorU32
     * [JVM] safe passing Vec2 instances */
    fun renderColorRectWithAlphaCheckerboard(drawList: DrawList, pMin: Vec2, pMax: Vec2, col: Int, gridStep: Float,
                                             gridOff: Vec2, rounding: Float = 0f, roundingCornersFlags: Int = -1) {
        if (((col and COL32_A_MASK) ushr COL32_A_SHIFT) < 0xFF) {
            val colBg1 = getColorU32(alphaBlendColors(COL32(204, 204, 204, 255), col))
            val colBg2 = getColorU32(alphaBlendColors(COL32(128, 128, 128, 255), col))
            drawList.addRectFilled(pMin, pMax, colBg1, rounding, roundingCornersFlags)

            var yi = 0
            var y = pMin.y + gridOff.y
            while (y < pMax.y) {
                val y1 = clamp(y, pMin.y, pMax.y)
                val y2 = min(y + gridStep, pMax.y)
                if (y2 <= y1) {
                    y += gridStep
                    yi++
                    continue
                }
                var x = pMin.x + gridOff.x + (yi and 1) * gridStep
                while (x < pMax.x) {
                    val x1 = clamp(x, pMin.x, pMax.x)
                    val x2 = min(x + gridStep, pMax.x)
                    if (x2 <= x1) {
                        x += gridStep * 2f
                        continue
                    }
                    var roundingCornersFlagsCell = 0
                    if (y1 <= pMin.y) {
                        if (x1 <= pMin.x) roundingCornersFlagsCell = roundingCornersFlagsCell or Dcf.TopLeft
                        if (x2 >= pMax.x) roundingCornersFlagsCell = roundingCornersFlagsCell or Dcf.TopRight
                    }
                    if (y2 >= pMax.y) {
                        if (x1 <= pMin.x) roundingCornersFlagsCell = roundingCornersFlagsCell or Dcf.BotLeft
                        if (x2 >= pMax.x) roundingCornersFlagsCell = roundingCornersFlagsCell or Dcf.BotRight
                    }
                    roundingCornersFlagsCell = roundingCornersFlagsCell and roundingCornersFlags
                    drawList.addRectFilled(Vec2(x1, y1), Vec2(x2, y2), colBg2, if (roundingCornersFlagsCell.bool) rounding else 0f, roundingCornersFlagsCell)
                    x += gridStep * 2f
                }
                y += gridStep
                yi++
            }
        } else
            drawList.addRectFilled(pMin, pMax, col, rounding, roundingCornersFlags)
    }

    /** Navigation highlight
     * @param flags: NavHighlightFlag  */
    fun renderNavHighlight(bb: Rect, id: ID, flags: NavHighlightFlags = NavHighlightFlag.TypeDefault.i) {

        if (id != g.navId) return
        if (g.navDisableHighlight && flags hasnt NavHighlightFlag.AlwaysDraw) return
        val window = currentWindow
        if (window.dc.navHideHighlightOneFrame) return

        val rounding = if (flags hasnt NavHighlightFlag.NoRounding) 0f else g.style.frameRounding
        val displayRect = Rect(bb)
        displayRect clipWith window.clipRect
        if (flags has NavHighlightFlag.TypeDefault) {
            val THICKNESS = 2f
            val DISTANCE = 3f + THICKNESS * 0.5f
            displayRect expand Vec2(DISTANCE)
            val fullyVisible = displayRect in window.clipRect
            if (!fullyVisible)
                window.drawList.pushClipRect(displayRect) // check order here down
            window.drawList.addRect(displayRect.min + (THICKNESS * 0.5f), displayRect.max - (THICKNESS * 0.5f),
                    Col.NavHighlight.u32, rounding, Dcf.All.i, THICKNESS)
            if (!fullyVisible)
                window.drawList.popClipRect()
        }
        if (flags has NavHighlightFlag.TypeThin)
            window.drawList.addRect(displayRect.min, displayRect.max, Col.NavHighlight.u32, rounding, 0.inv(), 1f)
    }

    /** Find the optional ## from which we stop displaying text.    */
    fun findRenderedTextEnd(text: String, textEnd: Int = -1): Int {
        val bytes = text.toByteArray()
        return findRenderedTextEnd(bytes, 0, if (textEnd != -1) textEnd else bytes.size)
    }

    /** Find the optional ## from which we stop displaying text.    */
    fun findRenderedTextEnd(text: ByteArray, textBegin: Int = 0, textEnd: Int = text.size): Int {
        var textDisplayEnd = textBegin
        while (textDisplayEnd < textEnd && text[textDisplayEnd] != 0.b &&
                (text[textDisplayEnd + 0] != '#'.b || text[textDisplayEnd + 1] != '#'.b))
            textDisplayEnd++
        return textDisplayEnd
    }

    fun logRenderedText(refPos: Vec2?, text: String, textEnd: Int = findRenderedTextEnd(text)) { // TODO ByteArray?

        val window = g.currentWindow!!

        val logNewLine = refPos?.let { it.y > g.logLinePosY + 1 } ?: false

        refPos?.let { g.logLinePosY = it.y }
        if (logNewLine)
            g.logLineFirstItem = true

        var textRemaining = text
        if (g.logDepthRef > window.dc.treeDepth) // Re-adjust padding if we have popped out of our starting depth
            g.logDepthRef = window.dc.treeDepth
        val treeDepth = window.dc.treeDepth - g.logDepthRef
        while (true) {
            // TODO re-sync
            // Split the string. Each new line (after a '\n') is followed by spacing corresponding to the current depth of our log entry.
            // We don't add a trailing \n to allow a subsequent item on the same line to be captured.
            val lineStart = textRemaining
            val lineEnd = if (lineStart.indexOf('\n') == -1) lineStart.length else lineStart.indexOf('\n')
            val isFirstLine = text.startsWith(lineStart)
            val isLastLine = text.endsWith(lineStart.substring(0, lineEnd))
            if (!isLastLine or lineStart.isNotEmpty()) {
                val charCount = lineStart.length
                when {
                    logNewLine or !isFirstLine -> logText("%s%s", "", lineStart)
                    g.logLineFirstItem -> logText("%s%s", "", lineStart)
                    else -> logText("%s", lineStart)
                }
            } else if (logNewLine) {
                // An empty "" string at a different Y position should output a carriage return.
                logText("\n")
                break
            }


            if (isLastLine)
                break
            textRemaining = textRemaining.substring(lineEnd + 1)
        }
    }

    // Render helpers (those functions don't access any ImGui state!)
    // these are all in the DrawList class
}