@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalStdlibApi::class)

package plot.internalApi

import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.api.gImGui
import imgui.classes.DrawList
import imgui.internal.classes.Rect
import imgui.internal.floor
import imgui.internal.textCharFromUtf8
import plot.api.PlotCol
import plot.api.PlotColormap
import plot.api.get

//-----------------------------------------------------------------------------
// [SECTION] Styling Utils
//-----------------------------------------------------------------------------

// Get styling data for next item (call between Begin/EndItem)
val itemData: PlotNextItemData get() = gImPlot.nextItemData

// Returns true if a color is set to be automatically determined
val Vec4.isColorAuto: Boolean get() = w == -1f

// Returns true if a style color is set to be automatically determined
val PlotCol.isColorAuto: Boolean get() = gImPlot.style.colors[this].isColorAuto

// Returns the automatically deduced style color
val PlotCol.autoColor: Vec4
    get() {
        val col = Vec4(0, 0, 0, 1)
        return when (this) {
            PlotCol.Line, PlotCol.Fill, PlotCol.MarkerOutline, PlotCol.MarkerFill -> col // these are plot dependent!
            PlotCol.ErrorBar -> ImGui.getStyleColorVec4(Col.Text)
            PlotCol.FrameBg -> ImGui.getStyleColorVec4(Col.FrameBg)
            PlotCol.PlotBg -> ImGui.getStyleColorVec4(Col.WindowBg)
            PlotCol.PlotBorder -> ImGui.getStyleColorVec4(Col.Border)
            PlotCol.LegendBg -> ImGui.getStyleColorVec4(Col.PopupBg)
            PlotCol.LegendBorder -> PlotCol.PlotBorder.vec4
            PlotCol.LegendText -> PlotCol.InlayText.vec4
            PlotCol.TitleText -> ImGui.getStyleColorVec4(Col.Text)
            PlotCol.InlayText -> ImGui.getStyleColorVec4(Col.Text)
            PlotCol.AxisText -> ImGui.getStyleColorVec4(Col.Text)
            PlotCol.AxisGrid -> PlotCol.AxisText.vec4 * Vec4(1, 1, 1, 0.25f)
            PlotCol.AxisTick -> PlotCol.AxisGrid.vec4
            PlotCol.AxisBg -> Vec4(0, 0, 0, 0)
            PlotCol.AxisBgHovered -> ImGui.getStyleColorVec4(Col.ButtonHovered)
            PlotCol.AxisBgActive -> ImGui.getStyleColorVec4(Col.ButtonActive)
            PlotCol.Selection -> Vec4(1, 1, 0, 1)
            PlotCol.Crosshairs -> PlotCol.PlotBorder.vec4
            else -> col
        }
    }

// Returns the style color whether it is automatic or custom set
val PlotCol.vec4 get() = if (isColorAuto) autoColor else Vec4(gImPlot.style.colors[this])
val PlotCol.u32 get() = vec4.u32.toUInt()

// Draws vertical text. The position is the bottom left of the text rect.
fun addTextVertical(drawList: DrawList, pos: Vec2, col: UInt, text: ByteArray, textEnd_: Int = -1) {
    // the code below is based loosely on ImFont::RenderText
    val textEnd = if (textEnd_ == -1) text.strlen() else textEnd_
    val g = gImGui
    val font = g.font
    // Align to be pixel perfect
    pos.x = floor(pos.x)
    pos.y = floor(pos.y)
    val scale = g.fontSize / font.fontSize
    var s = 0
    val charsExp = textEnd
    var charsRnd = 0
    val vtxCountMax = charsExp * 4
    val idxCountMax = charsExp * 6
    drawList.primReserve(idxCountMax, vtxCountMax)
    while (s < textEnd) {
        var c = text[s].toUInt().toInt()
        if (c < 0x80) {
            s += 1
        } else {
            val (char, bytes) = textCharFromUtf8(text, s, textEnd)
            c = char
            s += bytes
            if (c == 0) // Malformed UTF-8?
                break
        }
        val glyph = font.findGlyph(c) ?: continue
        drawList.primQuadUV(pos + Vec2(glyph.y0, -glyph.x0) * scale, pos + Vec2(glyph.y0, -glyph.x1) * scale,
                            pos + Vec2(glyph.y1, -glyph.x1) * scale, pos + Vec2(glyph.y1, -glyph.x0) * scale,
                            Vec2(glyph.u0, glyph.v0), Vec2(glyph.u1, glyph.v0),
                            Vec2(glyph.u1, glyph.v1), Vec2(glyph.u0, glyph.v1),
                            col.toInt())
        pos.y -= glyph.advanceX * scale
        charsRnd++
    }
    // Give back unused vertices
    val charsSkp = charsExp - charsRnd
    drawList.primUnreserve(charsSkp * 6, charsSkp * 4)
}

// Draws multiline horizontal text centered.
fun addTextCentered(drawList: DrawList, topCenter: Vec2, col: UInt, text: ByteArray, textEnd: Int = -1) {
    val txtHt = ImGui.textLineHeight
    var textBegin = 0
    val titleEnd = ImGui.findRenderedTextEnd(text, textEnd)
    val textSize = Vec2()
    var y = 0f
    var tmp = text.memchr(textBegin,'\n')
    while (tmp != -1) {
        textSize put ImGui.calcTextSize(text, textBegin, textEnd = tmp, hideTextAfterDoubleHash = true)
        drawList.addText(Vec2(topCenter.x - textSize.x * 0.5f, topCenter.y + y), col.toInt(), String(text, textBegin, tmp - textBegin))
        textBegin = tmp + 1
        y += txtHt
        tmp = text.memchr(tmp, '\n')
    }
    textSize put ImGui.calcTextSize(text, textBegin, titleEnd, true)
    drawList.addText(Vec2(topCenter.x - textSize.x * 0.5f, topCenter.y + y), col.toInt(), String(text, textBegin, titleEnd))
}

// Calculates the size of vertical text
fun calcTextSizeVertical(text: String): Vec2 {
    val sz = ImGui.calcTextSize(text)
    return Vec2(sz.y, sz.x)
}

// Returns white or black text given background color
fun calcTextColor(bg: Vec4): UInt = (if (bg.x * 0.299f + bg.y * 0.587f + bg.z * 0.114f > 0.5f) COL32_BLACK else COL32_WHITE).toUInt()
fun calcTextColor(bg: UInt): UInt = calcTextColor(bg.toInt().vec4)

// Lightens or darkens a color for hover
fun calcHoverColor(col: UInt) = mixU32(col, calcTextColor(col), 32)

// Clamps a label position so that it fits a rect defined by Min/Max
fun clampLabelPos(pos_: Vec2, size: Vec2, min: Vec2, max: Vec2): Vec2 {
    val pos = Vec2(pos_)
    if (pos.x < min.x) pos.x = min.x
    if (pos.y < min.y) pos.y = min.y
    if ((pos.x + size.x) > max.x) pos.x = max.x - size.x
    if ((pos.y + size.y) > max.y) pos.y = max.y - size.y
    return pos
}

// Returns a color from the Color map given an index >= 0 (modulo will be performed).
fun getColormapColorU32(idx: Int, cmap_: PlotColormap): UInt {
    val gp = gImPlot
    val cmap = if(cmap_ == PlotColormap.None) gp.style.colormap else cmap_
    assert(cmap != PlotColormap.None) { "Invalid colormap index!" }
    return gp.colormapData.getKeyColor(cmap, idx % gp.colormapData.getKeyCount(cmap))
}

// Returns the next unused colormap color and advances the colormap. Can be used to skip colors if desired.
fun nextColormapColorU32(): UInt {
    val gp = gImPlot
    assert(gp.currentItems != null) { "NextColormapColor() needs to be called between BeginPlot() and EndPlot()!" }
    val idx = gp.currentItems!!.colormapIdx % gp.colormapData.getKeyCount(gp.style.colormap)
    val col = gp.colormapData.getKeyColor(gp.style.colormap, idx)
    gp.currentItems!!.colormapIdx++
    return col
}

// Linearly interpolates a color from the current colormap given t between 0 and 1.
fun sampleColormapU32(t: Float, cmap_: PlotColormap): UInt {
    val gp = gImPlot
    val cmap = if(cmap_ == PlotColormap.None) gp.style.colormap else cmap_
    assert(cmap != PlotColormap.None) { "Invalid colormap index!" }
    return gp.colormapData.lerpTable(cmap, t)
}

// Render a colormap bar
fun renderColorBar(colors: UIntArray, drawList: DrawList, bounds: Rect, vert: Boolean, reversed: Boolean, continuous: Boolean) {
    val size = colors.size
    val n = if (continuous) size - 1 else size
    if (vert) {
        val step = bounds.height / n
        val rect = Rect(bounds.min.x, bounds.min.y, bounds.max.x, bounds.min.y + step)
        for (i in 0..<n) {
            val col1: UInt
            val col2: UInt
            if (reversed) {
                col1 = colors[size - i - 1]
                col2 = if (continuous) colors[size - i - 2] else col1
            } else {
                col1 = colors[i]
                col2 = if (continuous) colors[i + 1] else col1
            }
            drawList.addRectFilledMultiColor(rect.min, rect.max, col1.toInt(), col1.toInt(), col2.toInt(), col2.toInt())
            rect translateY step
        }
    } else {
        val step = bounds.width / n
        val rect = Rect(bounds.min.x, bounds.min.y, bounds.min.x + step, bounds.max.y)
        for (i in 0..<n) {
            val col1: UInt
            val col2: UInt
            if (reversed) {
                col1 = colors[size - i - 1]
                col2 = if (continuous) colors[size - i - 2] else col1
            } else {
                col1 = colors[i]
                col2 = if (continuous) colors[i + 1] else col1
            }
            drawList.addRectFilledMultiColor(rect.min, rect.max, col1.toInt(), col2.toInt(), col2.toInt(), col1.toInt())
            rect translateX step
        }
    }
}