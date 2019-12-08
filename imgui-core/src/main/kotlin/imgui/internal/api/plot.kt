package imgui.internal.api

import glm_.func.common.max
import glm_.func.common.min
import glm_.glm
import glm_.i
import glm_.isNaN
import glm_.vec2.Vec2
import imgui.Col
import imgui.ImGui.calcItemWidth
import imgui.ImGui.calcTextSize
import imgui.ImGui.currentWindow
import imgui.ImGui.io
import imgui.ImGui.itemAdd
import imgui.ImGui.itemHoverable
import imgui.ImGui.itemSize
import imgui.ImGui.renderFrame
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.setTooltip
import imgui.ImGui.style
import imgui.api.widgetsDataPlotting.Companion.PlotArray
import imgui.internal.PlotType
import imgui.internal.classes.Rect
import imgui.internal.lerp
import imgui.internal.saturate
import kotlin.math.min

/** Plot */
internal interface plot {

    fun plotEx(plotType: PlotType, label: String, data: PlotArray, valuesOffset: Int, overlayText: String,
               scaleMin_: Float, scaleMax_: Float, frameSize: Vec2) {

        val window = currentWindow
        if (window.skipItems) return

        var scaleMin = scaleMin_
        var scaleMax = scaleMax_
        val valuesCount = data.count()

        val labelSize = calcTextSize(label, -1, true)
        if (frameSize.x == 0f) frameSize.x = calcItemWidth()
        if (frameSize.y == 0f) frameSize.y = labelSize.y + style.framePadding.y * 2

        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + frameSize)
        val innerBb = Rect(frameBb.min + style.framePadding, frameBb.max - style.framePadding)
        val totalBb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0))
        itemSize(totalBb, style.framePadding.y)
        if (!itemAdd(totalBb, 0, frameBb)) return
        val hovered = itemHoverable(innerBb, 0)

        // Determine scale from values if not specified
        if (scaleMin == Float.MAX_VALUE || scaleMax == Float.MAX_VALUE) {
            var vMin = Float.MAX_VALUE
            var vMax = -Float.MAX_VALUE
            for (i in 0 until valuesCount) {
                val v = data[i]
                if (v.isNaN) continue
                vMin = vMin min v
                vMax = vMax max v
            }
            if (scaleMin == Float.MAX_VALUE) scaleMin = vMin
            if (scaleMax == Float.MAX_VALUE) scaleMax = vMax
        }

        renderFrame(frameBb.min, frameBb.max, Col.FrameBg.u32, true, style.frameRounding)

        val valuesCountMin = if (plotType == PlotType.Lines) 2 else 1
        if (valuesCount >= valuesCountMin) {
            val resW = min(frameSize.x.i, valuesCount) + if (plotType == PlotType.Lines) -1 else 0
            val itemCount = valuesCount + if (plotType == PlotType.Lines) -1 else 0

            // Tooltip on hover
            var vHovered = -1
            if (hovered && io.mousePos in innerBb) {
                val t = glm.clamp((io.mousePos.x - innerBb.min.x) / (innerBb.max.x - innerBb.min.x), 0f, 0.9999f)
                val vIdx = (t * itemCount).i
                assert(vIdx in 0 until valuesCount)

                val v0 = data[(vIdx + valuesOffset) % valuesCount]
                val v1 = data[(vIdx + 1 + valuesOffset) % valuesCount]
                when (plotType) {
                    PlotType.Lines -> setTooltip("$vIdx: %8.4g\n${vIdx + 1}: %8.4g", v0, v1)
                    PlotType.Histogram -> setTooltip("$vIdx: %8.4g", v0)
                }
                vHovered = vIdx
            }

            val tStep = 1f / resW
            val invScale = if (scaleMin == scaleMax) 0f else 1f / (scaleMax - scaleMin)

            val v0 = data[(0 + valuesOffset) % valuesCount]
            var t0 = 0f
            // Point in the normalized space of our target rectangle
            val tp0 = Vec2(t0, 1f - saturate((v0 - scaleMin) * invScale))
            // Where does the zero line stands
            val histogramZeroLineT = if (scaleMin * scaleMax < 0f) -scaleMin * invScale else if (scaleMin < 0f) 0f else 1f

            val colBase = (if (plotType == PlotType.Lines) Col.PlotLines else Col.PlotHistogram).u32
            val colHovered = (if (plotType == PlotType.Lines) Col.PlotLinesHovered else Col.PlotHistogramHovered).u32

            for (n in 0 until resW) {
                val t1 = t0 + tStep
                val v1Idx = (t0 * itemCount + 0.5f).i
                assert(v1Idx in 0 until valuesCount)
                val v1 = data[(v1Idx + valuesOffset + 1) % valuesCount]
                val tp1 = Vec2(t1, 1f - saturate((v1 - scaleMin) * invScale))

                // NB: Draw calls are merged together by the DrawList system. Still, we should render our batch are lower level to save a bit of CPU.
                val pos0 = innerBb.min.lerp(innerBb.max, tp0)
                val pos1 = innerBb.min.lerp(innerBb.max, if (plotType == PlotType.Lines) tp1 else Vec2(tp1.x, histogramZeroLineT))
                when (plotType) {
                    PlotType.Lines -> window.drawList.addLine(pos0, pos1, if (vHovered == v1Idx) colHovered else colBase)
                    PlotType.Histogram -> {
                        if (pos1.x >= pos0.x + 2f) pos1.x -= 1f
                        window.drawList.addRectFilled(pos0, pos1, if (vHovered == v1Idx) colHovered else colBase)
                    }
                }
                t0 = t1
                tp0 put tp1
            }
        }
        // Text overlay
        if (overlayText.isNotEmpty())
            renderTextClipped(Vec2(frameBb.min.x, frameBb.min.y + style.framePadding.y), frameBb.max, overlayText, -1, null, Vec2(0.5f, 0f))
        if (labelSize.x > 0f)
            renderText(Vec2(frameBb.max.x + style.itemInnerSpacing.x, innerBb.min.y), label)
    }
}