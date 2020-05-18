package imgui.demo.showExampleApp

import gli_.hasnt
import glm_.c
import glm_.f
import glm_.i
import glm_.max
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.beginTabBar
import imgui.ImGui.beginTabItem
import imgui.ImGui.beginTooltip
import imgui.ImGui.bulletText
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.colorEditVec4
import imgui.ImGui.combo
import imgui.ImGui.cursorScreenPos
import imgui.ImGui.dragFloat
import imgui.ImGui.dummy
import imgui.ImGui.endTabBar
import imgui.ImGui.endTabItem
import imgui.ImGui.endTooltip
import imgui.ImGui.fontSize
import imgui.ImGui.image
import imgui.ImGui.inputFloat
import imgui.ImGui.io
import imgui.ImGui.isMouseHoveringRect
import imgui.ImGui.logFinish
import imgui.ImGui.logText
import imgui.ImGui.logToClipboard
import imgui.ImGui.logToTTY
import imgui.ImGui.popFont
import imgui.ImGui.popID
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushFont
import imgui.ImGui.pushID
import imgui.ImGui.pushItemWidth
import imgui.ImGui.sameLine
import imgui.ImGui.separator
import imgui.ImGui.setWindowFontScale
import imgui.ImGui.showFontSelector
import imgui.ImGui.showStyleSelector
import imgui.ImGui.sliderFloat
import imgui.ImGui.sliderVec2
import imgui.ImGui.smallButton
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textEx
import imgui.ImGui.treeNode
import imgui.ImGui.treePop
import imgui.ImGui.windowDrawList
import imgui.ImGui.windowWidth
import imgui.api.demoDebugInformations.Companion.helpMarker
import imgui.api.g
import imgui.classes.Style
import imgui.classes.TextFilter
import imgui.dsl.button
import imgui.dsl.child
import imgui.dsl.radioButton
import imgui.dsl.smallButton
import imgui.dsl.tooltip
import imgui.dsl.treeNode
import imgui.dsl.withId
import imgui.dsl.withItemWidth
import imgui.font.Font
import kotlin.math.sqrt
import imgui.ColorEditFlag as Cef
import imgui.WindowFlag as Wf

object StyleEditor {

    var init = true
    var refSavedStyle: Style? = null

    // Default Styles Selector
    var styleIdx = 0

    var outputDest = 0
    var outputOnlyModified = true
    var alphaFlags: ColorEditFlags = 0
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
        if (showStyleSelector("Colors##Selector"))
            refSavedStyle = Style(style)

        showFontSelector("Fonts##Selector")

        // Simplified Settings (expose floating-pointer border sizes as boolean representing 0.0f or 1.0f)
        if (sliderFloat("FrameRounding", style::frameRounding, 0f, 12f, "%.0f"))
            style.grabRounding = style.frameRounding    // Make GrabRounding always the same value as FrameRounding
        run {
            val border = booleanArrayOf(style.windowBorderSize > 0f)
            if (checkbox("WindowBorder", border)) style.windowBorderSize = border[0].f
        }
        sameLine()
        run {
            val border = booleanArrayOf(style.frameBorderSize > 0f)
            if (checkbox("FrameBorder", border)) style.frameBorderSize = border[0].f
        }
        sameLine()
        run {
            val border = booleanArrayOf(style.popupBorderSize > 0f)
            if (checkbox("PopupBorder", border)) style.popupBorderSize = border[0].f
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
                "Save/Revert in local non-persistent storage. Default Colors definition are not affected. " +
                "Use \"Export\" below to save them somewhere.")

        separator()

        if (beginTabBar("##tabs", TabBarFlag.None.i)) {

            if (beginTabItem("Sizes")) {
                text("Main")
                sliderVec2("WindowPadding", style.windowPadding, 0f, 20f, "%.0f")
                sliderVec2("FramePadding", style.framePadding, 0f, 20f, "%.0f")
                sliderVec2("ItemSpacing", style.itemSpacing, 0f, 20f, "%.0f")
                sliderVec2("ItemInnerSpacing", style.itemInnerSpacing, 0f, 20f, "%.0f")
                sliderVec2("TouchExtraPadding", style.touchExtraPadding, 0f, 10f, "%.0f")
                sliderFloat("IndentSpacing", style::indentSpacing, 0f, 30f, "%.0f")
                sliderFloat("ScrollbarSize", style::scrollbarSize, 1f, 20f, "%.0f")
                sliderFloat("GrabMinSize", style::grabMinSize, 1f, 20f, "%.0f")
                text("Borders")
                sliderFloat("WindowBorderSize", style::windowBorderSize, 0f, 1f, "%.0f")
                sliderFloat("ChildBorderSize", style::childBorderSize, 0f, 1f, "%.0f")
                sliderFloat("PopupBorderSize", style::popupBorderSize, 0f, 1f, "%.0f")
                sliderFloat("FrameBorderSize", style::frameBorderSize, 0f, 1f, "%.0f")
                sliderFloat("TabBorderSize", style::tabBorderSize, 0f, 1f, "%.0f")
                text("Rounding")
                sliderFloat("WindowRounding", style::windowRounding, 0f, 12f, "%.0f")
                sliderFloat("ChildRounding", style::childRounding, 0f, 12f, "%.0f")
                sliderFloat("FrameRounding", style::frameRounding, 0f, 12f, "%.0f")
                sliderFloat("PopupRounding", style::popupRounding, 0f, 16f, "%.0f")
                sliderFloat("ScrollbarRounding", style::scrollbarRounding, 0f, 12f, "%.0f")
                sliderFloat("GrabRounding", style::grabRounding, 0f, 12f, "%.0f")
                sliderFloat("TabRounding", style::tabRounding, 0f, 12f, "%.0f")
                text("Alignment")
                sliderVec2("WindowTitleAlign", style.windowTitleAlign, 0f, 1f, "%.2f")
                run {
                    _i = style.windowMenuButtonPosition.i + 1
                    if (combo("WindowMenuButtonPosition", ::_i, "None${NUL}Left${NUL}Right${NUL}"))
                        style.windowMenuButtonPosition = Dir.values().first { it.i == _i - 1 }
                }
                run {
                    _i = style.colorButtonPosition.i
                    combo("ColorButtonPosition", ::_i, "Left\u0000Right\u0000")
                    style.colorButtonPosition = Dir.values().first { it.i == _i }
                }
                sliderVec2("ButtonTextAlign", style.buttonTextAlign, 0f, 1f, "%.2f")
                sameLine(); helpMarker("Alignment applies when a button is larger than its text content.")
                sliderVec2("SelectableTextAlign", style.selectableTextAlign, 0f, 1f, "%.2f")
                sameLine(); helpMarker("Alignment applies when a selectable is larger than its text content.")
                text("Safe Area Padding")
                sameLine(); helpMarker("Adjust if you cannot see the edges of your screen (e.g. on a TV where scaling has not been configured).")
                sliderVec2("DisplaySafeAreaPadding", style.displaySafeAreaPadding, 0f, 30f, "%.0f")
                endTabItem()
            }

            if (beginTabItem("Colors")) {

                button("Export") {
                    if (outputDest == 0)
                        logToClipboard()
                    else
                        logToTTY()
                    logText("val colors = ImGui.style.colors\n")
                    for (i in Col.values()) {
                        val col = style.colors[i]
                        val name = i.name
                        if (!outputOnlyModified || col != ref!!.colors[i])
                            logText("colors[Col_$name]%s = Vec4(%.2f, %.2f, %.2f, %.2f)\n",
                                    " ".repeat(23 - name.length), col.x, col.y, col.z, col.w)
                    }
                    logFinish()
                }
                sameLine()
                withItemWidth(120f) { combo("##output_type", ::outputDest, "To Clipboard\u0000To TTY\u0000") }
                sameLine()
                checkbox("Only Modified Colors", ::outputOnlyModified)

                text("Tip: Left-click on colored square to open color picker,\nRight-click to open edit options menu.")

                filter.draw("Filter colors", fontSize * 16)

                radioButton("Opaque", alphaFlags == 0) { alphaFlags = 0 }; sameLine()
                radioButton("Alpha", alphaFlags == Cef.AlphaPreview.i) { alphaFlags = Cef.AlphaPreview.i }; sameLine()
                radioButton("Both", alphaFlags == Cef.AlphaPreviewHalf.i) { alphaFlags = Cef.AlphaPreviewHalf.i }; sameLine()
                helpMarker(
                        "In the color list:\n" +
                        "Left-click on colored square to open color picker,\n" +
                        "Right-click to open edit options menu.")

                child("#colors", Vec2(), true, Wf.AlwaysVerticalScrollbar or Wf.AlwaysHorizontalScrollbar or Wf._NavFlattened) {
                    withItemWidth(-160) {
                        for (i in 0 until Col.COUNT) {
                            val name = Col.values()[i].name
                            if (!filter.passFilter(name))
                                continue
                            withId(i) {
                                colorEditVec4("##color", style.colors[i], Cef.AlphaBar or alphaFlags)
                                if (style.colors[i] != ref!!.colors[i]) {
                                    // Tips: in a real user application, you may want to merge and use an icon font into the main font,
                                    // so instead of "Save"/"Revert" you'd use icons!
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
                helpMarker("Read FAQ and docs/FONTS.txt for details on font loading.")
                pushItemWidth(120)
                for(font in atlas.fonts) {
                    pushID(font)
                    val name = font.configData.getOrNull(0)?.name ?: ""
                    nodeFont(font)
                    popID()
                }
                treeNode("Atlas texture", "Atlas texture (${atlas.texSize.x}x${atlas.texSize.y} pixels)") {
                    val tintCol = Vec4(1f)
                    val borderCol = Vec4(1f, 1f, 1f, 0.5f)
                    image(atlas.texID, Vec2(atlas.texSize), Vec2(), Vec2(1), tintCol, borderCol)
                }

                // Post-baking font scaling. Note that this is NOT the nice way of scaling fonts, read below.
                // (we enforce hard clamping manually as by default DragFloat/SliderFloat allows CTRL+Click text to get out of bounds).
                val MIN_SCALE = 0.3f
                val MAX_SCALE = 2f
                helpMarker(
                        "Those are old settings provided for convenience.\n" +
                        "However, the _correct_ way of scaling your UI is currently to reload your font at the designed size, " +
                        "rebuild the font atlas, and call style.ScaleAllSizes() on a reference ImGuiStyle structure.\n" +
                        "Using those settings here will give you poor quality results.")
                if (dragFloat("window scale", ::windowScale, 0.005f, MIN_SCALE, MAX_SCALE, "%.2f"))  // Scale only this window
                    setWindowFontScale(windowScale max  MIN_SCALE)
                if(dragFloat("global scale", io::fontGlobalScale, 0.005f, MIN_SCALE, MAX_SCALE, "%.2f")) // Scale everything
                    io.fontGlobalScale = io.fontGlobalScale max MIN_SCALE
                popItemWidth()

                endTabItem()
            }

            if (beginTabItem("Rendering")) {
                checkbox("Anti-aliased lines", style::antiAliasedLines)
                sameLine(); helpMarker("When disabling anti-aliasing lines, you'll probably want to disable borders in your style as well.")
                checkbox("Anti-aliased fill", style::antiAliasedFill)
                pushItemWidth(100)
                dragFloat("Curve Tessellation Tolerance", style::curveTessellationTol, 0.02f, 0.1f, 10f, "%.2f")
                if (style.curveTessellationTol < 10f) style.curveTessellationTol = 0.1f
                dragFloat("Circle segment Max Error", style::circleSegmentMaxError, 0.01f, 0.1f, 10f, "%.2f")
                /*  Not exposing zero here so user doesn't "lose" the UI (zero alpha clips all widgets).
                    But application code could have a toggle to switch between zero and non-zero.             */
                dragFloat("Global Alpha", style::alpha, 0.005f, 0.2f, 1f, "%.2f")
                popItemWidth()

                endTabItem()
            }
            endTabBar()
        }
        popItemWidth()
    }

    /** [Internal] Display details for a single font, called by ShowStyleEditor(). */
    fun nodeFont(font: Font) {
        val fontDetailsOpened = treeNode(font, "Font: \"${font.configData.getOrElse(0) { "" }}\"\n%.2f px, " +
                "${font.glyphs.size} glyphs, ${font.configDataCount} file(s)", font.fontSize)
        sameLine(); if (smallButton("Set as default")) io.fontDefault = font
        if (!fontDetailsOpened)
            return

        pushFont(font)
        text("The quick brown fox jumps over the lazy dog")
        popFont()
        dragFloat("Font scale", font::scale, 0.005f, 0.3f, 2f, "%.1f")   // Scale only this font
        sameLine(); helpMarker(
                "Note than the default embedded font is NOT meant to be scaled.\n\n" +
                        "Font are currently rendered into bitmaps at a given size at the time of building the atlas. " +
                        "You may oversample them to get some flexibility with scaling. " +
                        "You can also render at multiple sizes and select which one to use at runtime.\n\n" +
                        "(Glimmer of hope: the atlas system will be rewritten in the future to make scaling more flexible.)")
        inputFloat("Font offset", font.displayOffset::y, 1f, 1f, "%.0f")
        text("Ascent: ${font.ascent}, Descent: ${font.descent}, Height: ${font.ascent - font.descent}")
        text("Fallback character: '${font.fallbackChar}' (U+%04X)", font.fallbackChar)
        text("Ellipsis character: '${font.ellipsisChar}' (U+%04X)", font.ellipsisChar)
        val surfaceSqrt = sqrt(font.metricsTotalSurface.f).i
        text("Texture Area: about ${font.metricsTotalSurface} px ~${surfaceSqrt}x$surfaceSqrt px")
        for (configI in 0 until font.configDataCount)
            if (font.configData.isNotEmpty())
                font.configData.getOrNull(configI)?.let { cfg ->
                    bulletText("Input $configI: \'${cfg.name}\', Oversample: (${cfg.oversample.x},${cfg.oversample.y}), PixelSnapH: ${cfg.pixelSnapH}")
                }
        if (treeNode("Glyphs", "Glyphs (${font.glyphs.size})")) {
            // Display all glyphs of the fonts in separate pages of 256 characters
            val glyphCol = Col.Text.u32
            var base = 0
            while (base <= UNICODE_CODEPOINT_MAX) { //  step 256
                // Skip ahead if a large bunch of glyphs are not present in the font (test in chunks of 4k)
                // This is only a small optimization to reduce the number of iterations when IM_UNICODE_MAX_CODEPOINT
                // is large // (if ImWchar==ImWchar32 we will do at least about 272 queries here)
                if (base hasnt 4095 && font.isGlyphRangeUnused(base, base + 4095)) {
                    base += 4096 /*- 256 + 256 */
                    continue
                }

                val count = (0..255).count { font.findGlyphNoFallback((base + it).c) != null }
                if (count <= 0) {
                    base += 256
                    continue
                }
                // TreenNode((void*)(intptr_t)base, ..)
                if (!treeNode(Integer.valueOf(base), "U+%04X..U+%04X ($count ${if (count > 1) "glyphs" else "glyph"})", base, base + 255)) {
                    base += 256
                    continue
                }
                val cellSize = font.fontSize * 1
                val cellSpacing = style.itemSpacing.y
                val basePos = ImGui.cursorScreenPos // safe instance
                val drawList = ImGui.windowDrawList
                for (n in 0..255) {
                    // We use ImFont::RenderChar as a shortcut because we don't have UTF-8 conversion functions
                    // available here and thus cannot easily generate a zero-terminated UTF-8 encoded string.
                    val cellP1 = Vec2 (basePos.x + (n % 16) * (cellSize + cellSpacing), basePos.y+(n / 16) * (cellSize+cellSpacing))
                    val cellP2 = Vec2 (cellP1.x + cellSize, cellP1.y+cellSize)
                    val glyph = font.findGlyphNoFallback((base+n).c)
                    drawList.addRect(cellP1, cellP2, COL32(255, 255, 255, if(glyph != null) 100 else 50))
                    if (glyph != null)
                        font.renderChar(drawList, cellSize, cellP1, glyphCol, (base+n).c)
                    if (glyph != null && ImGui.isMouseHoveringRect(cellP1, cellP2)) {
                        beginTooltip()
                        text("Codepoint: U+%04X", base + n)
                        separator()
                        text("Visible: ${glyph.visible.i}")
                        text("AdvanceX: %.1f", glyph.advanceX)
                        text("Pos: (%.2f,%.2f)->(%.2f,%.2f)", glyph.x0, glyph.y0, glyph.x1, glyph.y1)
                        text("UV: (%.3f,%.3f)->(%.3f,%.3f)", glyph.u0, glyph.v0, glyph.u1, glyph.v1)
                        endTooltip()
                    }
                }
                dummy(Vec2((cellSize + cellSpacing) * 16))
                treePop()
                base += 256
            }
            treePop()
        }
        treePop()
    }
}
