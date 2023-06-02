package imgui.demo.showExampleApp

import glm_.hasnt
import glm_.c
import glm_.f
import glm_.i
import glm_.max
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.beginTabBar
import imgui.ImGui.beginTabItem
import imgui.ImGui.bulletText
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.colorEdit4
import imgui.ImGui.combo
import imgui.ImGui.cursorScreenPos
import imgui.ImGui.dummy
import imgui.ImGui.endTabBar
import imgui.ImGui.endTabItem
import imgui.ImGui.fontSize
import imgui.ImGui.io
import imgui.ImGui.isMouseHoveringRect
import imgui.ImGui.logFinish
import imgui.ImGui.logText
import imgui.ImGui.logToClipboard
import imgui.ImGui.logToTTY
import imgui.ImGui.popFont
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushFont
import imgui.ImGui.pushItemWidth
import imgui.ImGui.sameLine
import imgui.ImGui.separator
import imgui.ImGui.separatorText
import imgui.ImGui.setNextItemWidth
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setWindowFontScale
import imgui.ImGui.showFontAtlas
import imgui.ImGui.showFontSelector
import imgui.ImGui.slider2
import imgui.ImGui.spacing
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textEx
import imgui.ImGui.textUnformatted
import imgui.ImGui.treeNode
import imgui.ImGui.treePop
import imgui.ImGui.windowDrawList
import imgui.ImGui.windowWidth
import imgui.api.demoDebugInformations.Companion.helpMarker
import imgui.api.demoDebugInformations.ShowStyleSelector
import imgui.api.drag
import imgui.api.g
import imgui.api.slider
import imgui.classes.Style
import imgui.classes.TextFilter
import imgui.dsl.button
import imgui.dsl.child
import imgui.dsl.group
import imgui.dsl.radioButton
import imgui.dsl.smallButton
import imgui.dsl.tooltip
import imgui.dsl.treeNode
import imgui.dsl.withID
import imgui.dsl.withItemWidth
import imgui.font.Font
import imgui.internal.textCharToUtf8
import uno.kotlin.NUL
import kotlin.math.floor
import kotlin.math.sqrt
import imgui.ColorEditFlag as Cef
import imgui.WindowFlag as Wf

object StyleEditor {

    var init = true
    var refSavedStyle: Style? = null

    // Default Styles Selector
    var styleIdx = 0

    var border = false
    var border1 = false
    var popupBorder = false

    var outputDest = 0
    var outputOnlyModified = true
    var alphaFlags: ColorEditFlags = none
    val filter = TextFilter()
    var windowScale = 1f

    operator fun invoke(ref_: Style? = null) {

        // You can pass in a reference ImGuiStyle structure to compare to, revert to and save to
        // (without a reference style pointer, we will use one compared locally as a reference)

        // Default to using internal storage as reference
        if (init && ref_ == null) refSavedStyle = Style(style)
        init = false
        var ref = ref_ ?: refSavedStyle

        pushItemWidth(windowWidth * 0.55f)
        if (ShowStyleSelector("Colors##Selector")) refSavedStyle = Style(style)

        showFontSelector("Fonts##Selector")

        // Simplified Settings (expose floating-pointer border sizes as boolean representing 0.0f or 1.0f)
        if (slider("FrameRounding", style::frameRounding, 0f, 12f, "%.0f")) style.grabRounding = style.frameRounding    // Make GrabRounding always the same value as FrameRounding
        run {
            border = style.windowBorderSize > 0f
            if (checkbox("WindowBorder", ::border)) style.windowBorderSize = border.f
        }
        sameLine()
        run {
            border = style.frameBorderSize > 0f
            if (checkbox("FrameBorder", ::border)) style.frameBorderSize = border.f
        }
        sameLine()
        run {
            border = style.popupBorderSize > 0f
            if (checkbox("PopupBorder", ::border)) style.popupBorderSize = border.f
        }

        // Save/Revert button
        button("Save Ref") {
            refSavedStyle = Style(style)
            ref = style
        }
        sameLine()
        if (button("Revert Ref")) g.style = ref!!
        sameLine()
        helpMarker(
                "Save/Revert in local non-persistent storage. Default Colors definition are not affected. " + "Use \"Export\" below to save them somewhere.")

        separator()

        if (beginTabBar("##tabs")) {

            if (beginTabItem("Sizes")) {

                separatorText("Main")
                slider2("WindowPadding", style.windowPadding, 0f, 20f, "%.0f")
                slider2("FramePadding", style.framePadding, 0f, 20f, "%.0f")
                slider2("CellPadding", style.cellPadding, 0f, 20f, "%.0f")
                slider2("ItemSpacing", style.itemSpacing, 0f, 20f, "%.0f")
                slider2("ItemInnerSpacing", style.itemInnerSpacing, 0f, 20f, "%.0f")
                slider2("TouchExtraPadding", style.touchExtraPadding, 0f, 10f, "%.0f")
                slider("IndentSpacing", style::indentSpacing, 0f, 30f, "%.0f")
                slider("ScrollbarSize", style::scrollbarSize, 1f, 20f, "%.0f")
                slider("GrabMinSize", style::grabMinSize, 1f, 20f, "%.0f")

                separatorText("Borders")
                slider("WindowBorderSize", style::windowBorderSize, 0f, 1f, "%.0f")
                slider("ChildBorderSize", style::childBorderSize, 0f, 1f, "%.0f")
                slider("PopupBorderSize", style::popupBorderSize, 0f, 1f, "%.0f")
                slider("FrameBorderSize", style::frameBorderSize, 0f, 1f, "%.0f")
                slider("TabBorderSize", style::tabBorderSize, 0f, 1f, "%.0f")

                separatorText("Rounding")
                slider("WindowRounding", style::windowRounding, 0f, 12f, "%.0f")
                slider("ChildRounding", style::childRounding, 0f, 12f, "%.0f")
                slider("FrameRounding", style::frameRounding, 0f, 12f, "%.0f")
                slider("PopupRounding", style::popupRounding, 0f, 16f, "%.0f")
                slider("ScrollbarRounding", style::scrollbarRounding, 0f, 12f, "%.0f")
                slider("GrabRounding", style::grabRounding, 0f, 12f, "%.0f")
                slider("TabRounding", style::tabRounding, 0f, 12f, "%.0f")

                separatorText("Alignment")
                slider2("WindowTitleAlign", style.windowTitleAlign, 0f, 1f, "%.2f")
                run {
                    val sideRef = (style.windowMenuButtonPosition.i + 1).mutableReference
                    val side by sideRef
                    if (combo("WindowMenuButtonPosition", sideRef, "None${NUL}Left${NUL}Right${NUL}")) {
                        style.windowMenuButtonPosition = Dir.values().first { it.i == side - 1 }
                    }
                }
                run {
                    val sideRef = style.colorButtonPosition.i.mutableReference
                    val side by sideRef
                    combo("ColorButtonPosition", sideRef, "Left\u0000Right\u0000")
                    style.colorButtonPosition = Dir.values().first { it.i == side }
                }
                slider2("ButtonTextAlign", style.buttonTextAlign, 0f, 1f, "%.2f")
                sameLine(); helpMarker("Alignment applies when a button is larger than its text content.")
                slider2("SelectableTextAlign", style.selectableTextAlign, 0f, 1f, "%.2f")
                sameLine(); helpMarker("Alignment applies when a selectable is larger than its text content.")

                slider("SeparatorTextBorderSize", style::separatorTextBorderSize, 0f, 10f, "%.0f")
                slider2("SeparatorTextAlign", style.separatorTextAlign, 0f, 1f, "%.2f")
                slider2("SeparatorTextPadding", style.separatorTextPadding, 0f, 40f, "%0.f")
                slider("LogSliderDeadzone", style::logSliderDeadzone, 0f, 12f, "%.0f")

                separatorText("Misc")
                slider2("DisplaySafeAreaPadding", style.displaySafeAreaPadding, 0f, 30f, "%.0f"); sameLine(); helpMarker("Adjust if you cannot see the edges of your screen (e.g. on a TV where scaling has not been configured).")
                endTabItem()
            }

            if (beginTabItem("Colors")) {

                button("Export") {
                    if (outputDest == 0) logToClipboard()
                    else logToTTY()
                    logText("val colors = ImGui.style.colors\n")
                    for (i in Col.values()) {
                        val col = style.colors[i]
                        val name = i.name
                        if (!outputOnlyModified || col != ref!!.colors[i]) logText(
                                "colors[Col_$name]%s = Vec4(%.2f, %.2f, %.2f, %.2f)\n", " ".repeat(23 - name.length), col.x,
                                col.y, col.z, col.w)
                    }
                    logFinish()
                }
                sameLine()
                withItemWidth(120f) { combo("##output_type", ::outputDest, "To Clipboard\u0000To TTY\u0000") }
                sameLine()
                checkbox("Only Modified Colors", ::outputOnlyModified)

                text("Tip: Left-click on color square to open color picker,\nRight-click to open edit options menu.")

                filter.draw("Filter colors", fontSize * 16)

                radioButton("Opaque", alphaFlags.isEmpty) { alphaFlags = none }; sameLine()
                radioButton("Alpha", alphaFlags == Cef.AlphaPreview) { alphaFlags = Cef.AlphaPreview }; sameLine()
                radioButton("Both", alphaFlags == Cef.AlphaPreviewHalf) {
                    alphaFlags = Cef.AlphaPreviewHalf
                }; sameLine()
                helpMarker("""
                    In the color list:
                    Left-click on color square to open color picker,
                    Right-click to open edit options menu.""".trimIndent()
                )

                child(
                        "#colors", Vec2(), true,
                        Wf.AlwaysVerticalScrollbar or Wf.AlwaysHorizontalScrollbar or Wf._NavFlattened
                ) {
                    withItemWidth(-160) {
                        for (i in 0 until Col.COUNT) {
                            val name = Col.values()[i].name
                            if (!filter.passFilter(name)) continue
                            withID(i) {
                                colorEdit4("##color", style.colors[i], Cef.AlphaBar or alphaFlags)
                                if (style.colors[i] != ref!!.colors[i]) { // Tips: in a real user application, you may want to merge and use an icon font into the main font,
                                    // so instead of "Save"/"Revert" you'd use icons!
                                    // Read the FAQ and docs/FONTS.txt about using icon fonts. It's really easy and super convenient!
                                    sameLine(0f, style.itemInnerSpacing.x)
                                    if (button("Save")) ref!!.colors[i] = Vec4(style.colors[i])
                                    sameLine(0f, style.itemInnerSpacing.x)
                                    if (button("Revert")) style.colors[i] = Vec4(ref!!.colors[i])
                                }
                                sameLine(0f, style.itemInnerSpacing.x)
                                textEx(name)
                            }
                        }
                    }
                }

                endTabItem()
            }

            if (beginTabItem("Fonts")) {
                val atlas = io.fonts
                helpMarker("Read FAQ and docs/FONTS.md for details on font loading.")
                showFontAtlas(atlas)

                // Post-baking font scaling. Note that this is NOT the nice way of scaling fonts, read below.
                // (we enforce hard clamping manually as by default DragFloat/SliderFloat allows CTRL+Click text to get out of bounds).
                val MIN_SCALE = 0.3f
                val MAX_SCALE = 2f
                helpMarker("""
                    Those are old settings provided for convenience.
                    However, the _correct_ way of scaling your UI is currently to reload your font at the designed size, rebuild the font atlas, and call style.ScaleAllSizes() on a reference ImGuiStyle structure.
                    Using those settings here will give you poor quality results.""".trimIndent())
                pushItemWidth(fontSize * 8)
                if (drag("window scale", ::windowScale, 0.005f, MIN_SCALE, MAX_SCALE, "%.2f", SliderFlag.AlwaysClamp)) // Scale only this window
                    setWindowFontScale(windowScale)
                drag("global scale", io::fontGlobalScale, 0.005f, MIN_SCALE, MAX_SCALE, "%.2f", SliderFlag.AlwaysClamp) // Scale everything
                popItemWidth()

                endTabItem()
            }

            if (beginTabItem("Rendering")) {
                checkbox("Anti-aliased lines", style::antiAliasedLines)
                sameLine()
                helpMarker("When disabling anti-aliasing lines, you'll probably want to disable borders in your style as well.")

                checkbox("Anti-aliased lines use texture", style::antiAliasedLinesUseTex)
                sameLine()
                helpMarker("Faster lines using texture data. Require backend to render with bilinear filtering (not point/nearest filtering).")

                checkbox("Anti-aliased fill", style::antiAliasedFill)
                pushItemWidth(fontSize * 8)
                drag("Curve Tessellation Tolerance", style::curveTessellationTol, 0.02f, 0.1f, 10f, "%.2f")
                if (style.curveTessellationTol < 10f) style.curveTessellationTol = 0.1f

                // When editing the "Circle Segment Max Error" value, draw a preview of its effect on auto-tessellated circles.
                drag("Circle Tessellation Max Error", style::circleTessellationMaxError, 0.005f, 0.1f, 5f, "%.2f", SliderFlag.AlwaysClamp)
                val showSamples = ImGui.isItemActive
                if (showSamples)
                    setNextWindowPos(ImGui.cursorScreenPos)
                if (showSamples)
                    tooltip {
                        textUnformatted("(R = radius, N = number of segments)")
                        spacing()
                        val drawList = ImGui.windowDrawList
                        val minWidgetWidth = ImGui.calcTextSize("N: MMM\nR: MMM").x
                        for (n in 0..7) {

                            val RAD_MIN = 5f
                            val RAD_MAX = 70f
                            val rad = RAD_MIN + (RAD_MAX - RAD_MIN) * n / (8f - 1f)
                            val segmentCount = drawList._calcCircleAutoSegmentCount(rad)

                            group {
                                text("R: %.f\nN: ${drawList._calcCircleAutoSegmentCount(rad)}", rad)

                                val canvasWidth = minWidgetWidth max (rad * 2f)
                                val offsetX = floor(canvasWidth * 0.5f)
                                val offsetY = floor(RAD_MAX)

                                val p1 = cursorScreenPos
                                drawList.addCircle(Vec2(p1.x + offsetX, p1.y + offsetY), rad, Col.Text.u32)
                                dummy(Vec2(canvasWidth, RAD_MAX * 2))
                                /*
                                val p2 = cursorScreenPos
                                drawList.addCircleFilled(Vec2(p2.x+offsetX, p2.y+offsetY), rad, Col.Text.u32)
                                dummy(Vec2(canvasWidth, RAD_MAX * 2))
                                 */
                            }
                            sameLine()
                        }
                    }
                ImGui.sameLine()
                helpMarker("When drawing circle primitives with \"num_segments == 0\" tesselation will be calculated automatically.")

                /*  Not exposing zero here so user doesn't "lose" the UI (zero alpha clips all widgets).
                    But application code could have a toggle to switch between zero and non-zero.             */
                drag("Global Alpha", style::alpha, 0.005f, 0.2f, 1f, "%.2f")
                drag("Disabled Alpha", style::disabledAlpha, 0.005f, 0f, 1f, "%.2f"); sameLine(); helpMarker("Additional alpha multiplier for disabled items (multiply over current value of Alpha).")
                popItemWidth()

                endTabItem()
            }
            endTabBar()
        }
        popItemWidth()
    }

    fun debugNodeFont(font: Font) {
        val name = font.configData.getOrNull(0)?.name ?: ""
        val fontDetailsOpened = treeNode(font,
                "Font \\\"$name\\\"\\n%.2f px, %.2f px, ${font.glyphs.size} glyphs, ${font.configDataCount} file(s)",
                font.fontSize)
        sameLine(); smallButton("Set as default") { io.fontDefault = font }
        if (!fontDetailsOpened)
            return

        // Display preview text
        pushFont(font)
        text("The quick brown fox jumps over the lazy dog")
        popFont()

        // Display details
        setNextItemWidth(fontSize * 8)
        drag("Font scale", font::scale, 0.005f, 0.3f, 2f, "%.1f")
        sameLine()
        helpMarker("""
                        |Note than the default embedded font is NOT meant to be scaled.
                        |
                        |Font are currently rendered into bitmaps at a given size at the time of building the atlas. You may oversample them to get some flexibility with scaling. You can also render at multiple sizes and select which one to use at runtime.
                        |
                        |(Glimmer of hope: the atlas system should hopefully be rewritten in the future to make scaling more natural and automatic.)""".trimMargin())
        text("Ascent: ${font.ascent}, Descent: ${font.descent}, Height: ${font.ascent - font.descent}")
        val cStr = ByteArray(5)
        text("Fallback character: '${textCharToUtf8(cStr, font.fallbackChar.code)}' (U+%04X)", font.fallbackChar)
        text("Ellipsis character: '${textCharToUtf8(cStr, font.ellipsisChar.code)}' (U+%04X)", font.ellipsisChar)
        val side = sqrt(font.metricsTotalSurface.f).i
        text("Texture Area: about ${font.metricsTotalSurface} px ~${side}x$side px")
        for (c in 0 until font.configDataCount)
            font.configData.getOrNull(c)?.let {
                bulletText(
                        "Input $c: '${it.name}', Oversample: ${it.oversample}, PixelSnapH: ${it.pixelSnapH}, Offset: (%.1f,%.1f)",
                        it.glyphOffset.x, it.glyphOffset.y)
            }

        // Display all glyphs of the fonts in separate pages of 256 characters
        treeNode("Glyphs", "Glyphs (${font.glyphs.size})") {
            var base = 0
            while (base <= UNICODE_CODEPOINT_MAX) {

                // Skip ahead if a large bunch of glyphs are not present in the font (test in chunks of 4k)
                // This is only a small optimization to reduce the number of iterations when IM_UNICODE_MAX_CODEPOINT
                // is large // (if ImWchar==ImWchar32 we will do at least about 272 queries here)
                if (base hasnt 4095 && font.isGlyphRangeUnused(base, base + 4095)) {
                    base += 4096 - 256
                    base += 256
                    continue
                }

                val count = (0 until 256).count { font.findGlyphNoFallback(base + it) != null }
                val s = if (count > 1) "glyphs" else "glyph"
                if (count > 0 && treeNode(Integer.valueOf(base), "U+%04X..U+%04X ($count $s)", base, base + 255)) {

                    val cellSize = font.fontSize * 1
                    val cellSpacing = style.itemSpacing.y
                    val basePos = Vec2(cursorScreenPos)
                    val drawList = windowDrawList
                    for (n in 0 until 256) {
                        val cellP1 = Vec2(basePos.x + (n % 16) * (cellSize + cellSpacing),
                                basePos.y + (n / 16) * (cellSize + cellSpacing))
                        val cellP2 = Vec2(cellP1.x + cellSize, cellP1.y + cellSize)
                        val glyph = font.findGlyphNoFallback((base + n).c)
                        drawList.addRect(cellP1, cellP2, COL32(255, 255, 255, if (glyph != null) 100 else 50)) // We use ImFont::RenderChar as a shortcut because we don't have UTF-8 conversion functions
                        // available here and thus cannot easily generate a zero-terminated UTF-8 encoded string.
                        if (glyph != null) {
                            font.renderChar(drawList, cellSize, cellP1, Col.Text.u32, (base + n).c)
                            if (isMouseHoveringRect(cellP1, cellP2)) tooltip {
                                text("Codepoint: U+%04X", base + n)
                                separator()
                                if (DEBUG) text("Visible: ${glyph.visible}")
                                else text("Visible: ${glyph.visible.i}")
                                text("AdvanceX+1: %.1f", glyph.advanceX)
                                text("Pos: (%.2f,%.2f)->(%.2f,%.2f)", glyph.x0, glyph.y0, glyph.x1, glyph.y1)
                                text("UV: (%.3f,%.3f)->(%.3f,%.3f)", glyph.u0, glyph.v0, glyph.u1, glyph.v1)
                            }
                        }
                    }
                    dummy(Vec2((cellSize + cellSpacing) * 16, (cellSize + cellSpacing) * 16))
                    treePop()
                }
                base += 256
            }
            treePop()
        }
    }
}