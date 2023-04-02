package imgui.internal.api

import glm_.b
import glm_.f
import glm_.func.common.max
import glm_.glm
import glm_.vec1.Vec1i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.calcTextSize
import imgui.ImGui.currentWindow
import imgui.ImGui.getColorU32
import imgui.ImGui.logRenderedText
import imgui.ImGui.style
import imgui.api.g
import imgui.classes.DrawList
import imgui.internal.*
import imgui.internal.classes.Rect
import imgui.internal.sections.*
import unsigned.toUInt
import kotlin.math.max

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
            if (g.logEnabled)
                logRenderedText(pos, text.cStr, textEnd)
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
                ellipsisChar = font.dotChar
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
            window.drawList.addRect(pMin + 1, pMax + 1, Col.BorderShadow.u32, rounding, thickness = borderSize)
            window.drawList.addRect(pMin, pMax, Col.Border.u32, rounding, thickness = borderSize)
        }
    }

    fun renderFrameBorder(pMin: Vec2, pMax: Vec2, rounding: Float = 0f) = with(g.currentWindow!!) {
        val borderSize = style.frameBorderSize
        if (borderSize > 0f) {
            drawList.addRect(pMin + 1, pMax + 1, Col.BorderShadow.u32, rounding, thickness = borderSize)
            drawList.addRect(pMin, pMax, Col.Border.u32, rounding, thickness = borderSize)
        }
    }

    /** Helper for ColorPicker4()
     *  NB: This is rather brittle and will show artifact when rounding this enabled if rounded corners overlap multiple cells. Caller currently responsible for avoiding that.
     * Spent a non reasonable amount of time trying to getting this right for ColorButton with rounding+anti-aliasing+ImGuiColorEditFlags_HalfAlphaPreview flag + various grid sizes and offsets, and eventually gave up... probably more reasonable to disable rounding altogether.
     * FIXME: uses ImGui::GetColorU32
     * [JVM] safe passing Vec2 instances */
    fun renderColorRectWithAlphaCheckerboard(
        drawList: DrawList, pMin: Vec2, pMax: Vec2, col: Int, gridStep: Float,
        gridOff: Vec2, rounding: Float = 0f, flags_: DrawFlags = emptyFlags()
    ) {
        val flags = when {
            flags_ hasnt DrawFlag.RoundCornersMask -> DrawFlag.RoundCornersDefault
            else -> flags_
        }
        if (((col and COL32_A_MASK) ushr COL32_A_SHIFT) < 0xFF) {
            val colBg1 = getColorU32(alphaBlendColors(COL32(204, 204, 204, 255), col))
            val colBg2 = getColorU32(alphaBlendColors(COL32(128, 128, 128, 255), col))
            drawList.addRectFilled(pMin, pMax, colBg1, rounding, flags)

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
                    var cellFlags: DrawFlags = DrawFlag.RoundCornersNone
                    if (y1 <= pMin.y) {
                        if (x1 <= pMin.x) cellFlags = cellFlags or DrawFlag.RoundCornersTopLeft
                        if (x2 >= pMax.x) cellFlags = cellFlags or DrawFlag.RoundCornersTopRight
                    }
                    if (y2 >= pMax.y) {
                        if (x1 <= pMin.x) cellFlags = cellFlags or DrawFlag.RoundCornersBottomLeft
                        if (x2 >= pMax.x) cellFlags = cellFlags or DrawFlag.RoundCornersBottomRight
                    }
                    // Combine flags
                    cellFlags = when {
                        flags eq DrawFlag.RoundCornersNone || cellFlags eq DrawFlag.RoundCornersNone -> DrawFlag.RoundCornersNone
                        else -> cellFlags or flags
                    }
                    drawList.addRectFilled(Vec2(x1, y1), Vec2(x2, y2), colBg2, rounding, cellFlags)
                    x += gridStep * 2f
                }
                y += gridStep
                yi++
            }
        } else
            drawList.addRectFilled(pMin, pMax, col, rounding, flags)
    }

    /** Navigation highlight
     * @param flags: NavHighlightFlag  */
    fun renderNavHighlight(bb: Rect, id: ID, flags: NavHighlightFlags = NavHighlightFlag.TypeDefault) {

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
            window.drawList.addRect(
                displayRect.min + (THICKNESS * 0.5f), displayRect.max - (THICKNESS * 0.5f),
                Col.NavHighlight.u32, rounding, thickness = THICKNESS
            )
            if (!fullyVisible)
                window.drawList.popClipRect()
        }
        if (flags has NavHighlightFlag.TypeThin)
            window.drawList.addRect(displayRect.min, displayRect.max, Col.NavHighlight.u32, rounding, thickness = 1f)
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

    fun renderMouseCursor(basePos: Vec2, baseScale: Float, mouseCursor: MouseCursor, colFill: Int, colBorder: Int, colShadow: Int) {
        //        IM_ASSERT(mouseCursor > ImGuiMouseCursor_None && mouseCursor < ImGuiMouseCursor_COUNT);
        for (viewport in g.viewports) {
            val drawList = viewport.foregroundDrawList
            val fontAtlas = drawList._data.font!!.containerAtlas
            val offset = Vec2()
            val size = Vec2()
            val uv = Array(4) { Vec2() }
            if (fontAtlas.getMouseCursorTexData(mouseCursor, offset, size, uv)) {
                val pos = basePos - offset
                val scale = baseScale
                val texId = fontAtlas.texID
                drawList.pushTextureID(texId)
                drawList.addImage(texId, pos + Vec2(1, 0) * scale, pos + (Vec2(1, 0) + size) * scale, uv[2], uv[3], colShadow)
                drawList.addImage(texId, pos + Vec2(2, 0) * scale, pos + (Vec2(2, 0) + size) * scale, uv[2], uv[3], colShadow)
                drawList.addImage(texId, pos, pos + size * scale, uv[2], uv[3], colBorder)
                drawList.addImage(texId, pos, pos + size * scale, uv[0], uv[1], colFill)
                drawList.popTextureID()
            }
        }
    }

    /** Render an arrow aimed to be aligned with text (p_min is a position in the same space text would be positioned). To e.g. denote expanded/collapsed state  */
    fun DrawList.renderArrow(pos: Vec2, col: Int, dir: Dir, scale: Float = 1f) {

        val h = _data.fontSize * 1f
        var r = h * 0.4f * scale
        val center = pos + Vec2(h * 0.5f, h * 0.5f * scale)

        val a: Vec2
        val b: Vec2
        val c: Vec2
        when (dir) {
            Dir.Up, Dir.Down -> {
                if (dir == Dir.Up) r = -r
                a = Vec2(+0.000f, +0.75f) * r
                b = Vec2(-0.866f, -0.75f) * r
                c = Vec2(+0.866f, -0.75f) * r
            }

            Dir.Left, Dir.Right -> {
                if (dir == Dir.Left) r = -r
                a = Vec2(+0.75f, +0.000f) * r
                b = Vec2(-0.75f, +0.866f) * r
                c = Vec2(-0.75f, -0.866f) * r
            }

            else -> throw Error()
        }

        addTriangleFilled(center + a, center + b, center + c, col)
    }

    fun DrawList.renderBullet(pos: Vec2, col: Int) = addCircleFilled(pos, _data.fontSize * 0.2f, col, 8)

    @Deprecated("placeholder: pos gets modified!")
    fun DrawList.renderCheckMark(pos: Vec2, col: Int, sz_: Float) {

        val thickness = imgui.max(sz_ / 5f, 1f)
        val sz = sz_ - thickness * 0.5f
        pos += thickness * 0.25f

        val third = sz / 3f
        val bx = pos.x + third
        val by = pos.y + sz - third * 0.5f
        pathLineTo(Vec2(bx - third, by - third))
        pathLineTo(Vec2(bx, by))
        pathLineTo(Vec2(bx + third * 2f, by - third * 2f))
        pathStroke(col, thickness = thickness)
    }

    /** Render an arrow. 'pos' is position of the arrow tip. halfSz.x is length from base to tip. halfSz.y is length on each side. */
    fun DrawList.renderArrowPointingAt(pos: Vec2, halfSz: Vec2, direction: Dir, col: Int) =
            when (direction) {
                Dir.Left -> addTriangleFilled(Vec2(pos.x + halfSz.x, pos.y - halfSz.y), Vec2(pos.x + halfSz.x, pos.y + halfSz.y), pos, col)
                Dir.Right -> addTriangleFilled(Vec2(pos.x - halfSz.x, pos.y + halfSz.y), Vec2(pos.x - halfSz.x, pos.y - halfSz.y), pos, col)
                Dir.Up -> addTriangleFilled(Vec2(pos.x + halfSz.x, pos.y + halfSz.y), Vec2(pos.x - halfSz.x, pos.y + halfSz.y), pos, col)
                Dir.Down -> addTriangleFilled(Vec2(pos.x - halfSz.x, pos.y - halfSz.y), Vec2(pos.x + halfSz.x, pos.y - halfSz.y), pos, col)
                else -> Unit
            }

    /** FIXME: Cleanup and move code to ImDrawList. */
    fun DrawList.renderRectFilledRangeH(rect: Rect, col: Int, xStartNorm_: Float, xEndNorm_: Float, rounding_: Float) {
        var xStartNorm = xStartNorm_
        var xEndNorm = xEndNorm_
        if (xEndNorm == xStartNorm) return
        if (xStartNorm > xEndNorm) {
            val tmp = xStartNorm
            xStartNorm = xEndNorm
            xEndNorm = tmp
        }
        val p0 = Vec2(lerp(rect.min.x, rect.max.x, xStartNorm), rect.min.y)
        val p1 = Vec2(lerp(rect.min.x, rect.max.x, xEndNorm), rect.max.y)
        if (rounding_ == 0f) {
            addRectFilled(p0, p1, col, 0f)
            return
        }
        val rounding = glm.clamp(glm.min((rect.max.x - rect.min.x) * 0.5f, (rect.max.y - rect.min.y) * 0.5f) - 1f, 0f, rounding_)
        val invRounding = 1f / rounding
        val arc0B = acos01(1f - (p0.x - rect.min.x) * invRounding)
        val arc0E = acos01(1f - (p1.x - rect.min.x) * invRounding)
        val halfPI = glm.HPIf // We will == compare to this because we know this is the exact value ImAcos01 can return.
        val x0 = glm.max(p0.x, rect.min.x + rounding)
        if (arc0B == arc0E) {
            pathLineTo(Vec2(x0, p1.y))
            pathLineTo(Vec2(x0, p0.y))
        } else if (arc0B == 0f && arc0E == halfPI) {
            pathArcToFast(Vec2(x0, p1.y - rounding), rounding, 3, 6) // BL
            pathArcToFast(Vec2(x0, p0.y + rounding), rounding, 6, 9) // TR
        } else {
            pathArcTo(Vec2(x0, p1.y - rounding), rounding, glm.PIf - arc0E, glm.PIf - arc0B, 3) // BL
            pathArcTo(Vec2(x0, p0.y + rounding), rounding, glm.PIf + arc0B, glm.PIf + arc0E, 3) // TR
        }
        if (p1.x > rect.min.x + rounding) {
            val arc1B = acos01(1f - (rect.max.x - p1.x) * invRounding)
            val arc1E = acos01(1f - (rect.max.x - p0.x) * invRounding)
            val x1 = glm.min(p1.x, rect.max.x - rounding)
            if (arc1B == arc1E) {
                pathLineTo(Vec2(x1, p0.y))
                pathLineTo(Vec2(x1, p1.y))
            } else if (arc1B == 0f && arc1E == halfPI) {
                pathArcToFast(Vec2(x1, p0.y + rounding), rounding, 9, 12) // TR
                pathArcToFast(Vec2(x1, p1.y - rounding), rounding, 0, 3)  // BR
            } else {
                pathArcTo(Vec2(x1, p0.y + rounding), rounding, -arc1E, -arc1B, 3) // TR
                pathArcTo(Vec2(x1, p1.y - rounding), rounding, +arc1B, +arc1E, 3) // BR
            }
        }
        pathFillConvex(col)
    }

    fun DrawList.renderRectFilledWithHole(outer: Rect, inner: Rect, col: Int, rounding: Float) {
        val fillL = inner.min.x > outer.min.x
        val fillR = inner.max.x < outer.max.x
        val fillU = inner.min.y > outer.min.y
        val fillD = inner.max.y < outer.max.y
        if (fillL) addRectFilled(
            Vec2(outer.min.x, inner.min.y), Vec2(inner.min.x, inner.max.y), col, rounding,
            DrawFlag.RoundCornersNone or (if (fillU) emptyFlags() else DrawFlag.RoundCornersTopLeft) or if (fillD) emptyFlags() else DrawFlag.RoundCornersBottomLeft
        )
        if (fillR) addRectFilled(
            Vec2(inner.max.x, inner.min.y), Vec2(outer.max.x, inner.max.y), col, rounding,
            DrawFlag.RoundCornersNone or (if (fillU) emptyFlags() else DrawFlag.RoundCornersTopRight) or if (fillD) emptyFlags() else DrawFlag.RoundCornersBottomRight
        )
        if (fillU) addRectFilled(
            Vec2(inner.min.x, outer.min.y), Vec2(inner.max.x, inner.min.y), col, rounding,
            DrawFlag.RoundCornersNone or (if (fillL) emptyFlags() else DrawFlag.RoundCornersTopLeft) or if (fillR) emptyFlags() else DrawFlag.RoundCornersTopRight
        )
        if (fillD) addRectFilled(
            Vec2(inner.min.x, inner.max.y), Vec2(inner.max.x, outer.max.y), col, rounding,
            DrawFlag.RoundCornersNone or (if (fillL) emptyFlags() else DrawFlag.RoundCornersBottomLeft) or if (fillR) emptyFlags() else DrawFlag.RoundCornersBottomRight
        )
        if (fillL && fillU) addRectFilled(Vec2(outer.min.x, outer.min.y), Vec2(inner.min.x, inner.min.y), col, rounding, DrawFlag.RoundCornersTopLeft)
        if (fillR && fillU) addRectFilled(Vec2(inner.max.x, outer.min.y), Vec2(outer.max.x, inner.min.y), col, rounding, DrawFlag.RoundCornersTopRight)
        if (fillL && fillD) addRectFilled(Vec2(outer.min.x, inner.max.y), Vec2(inner.min.x, outer.max.y), col, rounding, DrawFlag.RoundCornersBottomLeft)
        if (fillR && fillD) addRectFilled(Vec2(inner.max.x, inner.max.y), Vec2(outer.max.x, outer.max.y), col, rounding, DrawFlag.RoundCornersBottomRight)
    }

    companion object {
        private fun acos01(x: Float) = when {
            x <= 0f -> glm.PIf * 0.5f
            x >= 1f -> 0f
            else -> glm.acos(x)
            //return (-0.69813170079773212f * x * x - 0.87266462599716477f) * x + 1.5707963267948966f; // Cheap approximation, may be enough for what we do.
        }
    }
}