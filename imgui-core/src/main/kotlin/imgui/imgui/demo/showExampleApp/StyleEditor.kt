package imgui.imgui.demo.showExampleApp

import glm_.c
import glm_.f
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.beginTabBar
import imgui.ImGui.beginTabItem
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
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushFont
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.ImGui.radioButton
import imgui.ImGui.sameLine
import imgui.ImGui.separator
import imgui.ImGui.setWindowFontScale
import imgui.ImGui.showFontSelector
import imgui.ImGui.showStyleSelector
import imgui.ImGui.sliderFloat
import imgui.ImGui.sliderVec2
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textEx
import imgui.ImGui.treeNode
import imgui.ImGui.treePop
import imgui.ImGui.windowDrawList
import imgui.ImGui.windowWidth
import imgui.dsl.button
import imgui.dsl.child
import imgui.dsl.smallButton
import imgui.dsl.tooltip
import imgui.dsl.treeNode
import imgui.dsl.withId
import imgui.dsl.withItemWidth
import imgui.imgui.g
import imgui.imgui.imgui_demoDebugInformations.Companion.helpMarker
import kotlin.math.sqrt
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf

object StyleEditor {

    var init = true
    var refSavedStyle: Style? = null
    // Default Styles Selector
    var styleIdx = 0

    var windowBorder = false
    var frameBorder = false
    var popupBorder = false

    var outputDest = 0
    var outputOnlyModified = true
    var alphaFlags: ColorEditFlags = 0
    val filter = TextFilter()
    var windowScale = 1f

    operator fun invoke(ref_: Style? = null) {

        /*  You can pass in a reference ImGuiStyle structure to compare to, revert to and save to
            (else it compares to the default style)         */

        // Default to using internal storage as reference
        if (init && ref_ == null) refSavedStyle = Style(style)
        init = false
        var ref = ref_ ?: refSavedStyle

        pushItemWidth(windowWidth * 0.55f)
        if (showStyleSelector("Colors##Selector"))
            refSavedStyle = Style(style)

        showFontSelector("Fonts##Selector")

        // Simplified Settings
        if (sliderFloat("FrameRounding", style::frameRounding, 0f, 12f, "%.0f"))
            style.grabRounding = style.frameRounding    // Make GrabRounding always the same value as FrameRounding
        run {
            windowBorder = style.windowBorderSize > 0f
            if (checkbox("WindowBorder", ::windowBorder)) style.windowBorderSize = if (windowBorder) 1f else 0f
        }
        sameLine()
        run {
            frameBorder = style.frameBorderSize > 0f
            if (checkbox("FrameBorder", ::frameBorder)) style.frameBorderSize = if (frameBorder) 1f else 0f
        }
        sameLine()
        run {
            popupBorder = style.popupBorderSize > 0f
            if (checkbox("PopupBorder", ::popupBorder)) style.popupBorderSize = if (popupBorder) 1f else 0f
        }

        // Save/Revert button
        button("Save Ref") {
            refSavedStyle = Style(style)
            ref = style
        }
        sameLine()
        if (button("Revert Ref")) g.style = ref!!
        sameLine()
        helpMarker("Save/Revert in local non-persistent storage. Default Colors definition are not affected. Use \"Export Colors\" below to save them somewhere.")

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
                sliderVec2("ButtonTextAlign", style.buttonTextAlign, 0f, 1f, "%.2f"); sameLine(); helpMarker("Alignment applies when a button is larger than its text content.")
                sliderVec2("SelectableTextAlign", style.selectableTextAlign, 0f, 1f, "%.2f"); sameLine(); helpMarker("Alignment applies when a selectable is larger than its text content.")
                text("Safe Area Padding"); sameLine(); helpMarker("Adjust if you cannot see the edges of your screen (e.g. on a TV where scaling has not been configured).")
                sliderVec2("DisplaySafeAreaPadding", style.displaySafeAreaPadding, 0f, 30f, "%.0f")
                endTabItem()
            }

            if (beginTabItem("Colors")) {

                button("Export Unsaved") {
                    if (outputDest == 0)
                        logToClipboard()
                    else
                        logToTTY()
                    logText("val colors = ImGui.style.colors\n")
                    for (i in Col.values()) {
                        val col = style.colors[i]
                        val name = i.name
                        if (!outputOnlyModified || col != ref!!.colors[i])
                            logText("colors[Col.%s.i] = Vec4(%.2f, %.2f, %.2f, %.2f)\n", name, col.x, col.y, col.z, col.w)
                    }
                    logFinish()
                }
                sameLine()
                withItemWidth(120f) { combo("##output_type", ::outputDest, "To Clipboard\u0000To TTY\u0000") }
                sameLine()
                checkbox("Only Modified Colors", ::outputOnlyModified)

                text("Tip: Left-click on colored square to open color picker,\nRight-click to open edit options menu.")

                filter.draw("Filter colors", fontSize * 16)

                radioButton("Opaque", ::alphaFlags, 0); sameLine()
                radioButton("Alpha", ::alphaFlags, Cef.AlphaPreview.i); sameLine()
                radioButton("Both", ::alphaFlags, Cef.AlphaPreviewHalf.i); sameLine()
                helpMarker("In the color list:\nLeft-click on colored square to open color picker,\nRight-click to open edit options menu.");

                child("#colors", Vec2(), true, Wf.AlwaysVerticalScrollbar or Wf.AlwaysHorizontalScrollbar or Wf.NavFlattened) {
                    withItemWidth(-160) {
                        for (i in 0 until Col.COUNT) {
                            val name = Col.values()[i].name
                            if (!filter.passFilter(name))
                                continue
                            withId(i) {
                                colorEditVec4("##color", style.colors[i], Cef.AlphaBar or alphaFlags)
                                if (style.colors[i] != ref!!.colors[i]) {
                                    /*  Tips: in a real user application, you may want to merge and use an icon font into
                                        the main font, so instead of "Save"/"Revert" you'd use icons.
                                        Read the FAQ and misc/fonts/README.txt about using icon fonts. It's really easy
                                        and super convenient!  */
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
                helpMarker("Read FAQ and misc/fonts/README.txt for details on font loading.")
                pushItemWidth(120)
                atlas.fonts.forEachIndexed { i, font ->
                    pushId(font)
                    val name = font.configData.getOrNull(0)?.name ?: ""
                    val fontDetailsOpened = treeNode(font, "Font $i: '$name', %.2f px, ${font.glyphs.size} glyphs, ${font.configDataCount} file(s)", font.fontSize)
                    sameLine(); smallButton("Set as default") { io.fontDefault = font }
                    if (fontDetailsOpened) {
                        pushFont(font)
                        text("The quick brown fox jumps over the lazy dog")
                        popFont()
                        dragFloat("Font scale", font::scale, 0.005f, 0.3f, 2f, "%.1f")
                        sameLine()
                        helpMarker("""
                        |Note than the default embedded font is NOT meant to be scaled.
                        |
                        |Font are currently rendered into bitmaps at a given size at the time of building the atlas. You may oversample them to get some flexibility with scaling. You can also render at multiple sizes and select which one to use at runtime.
                        |
                        |(Glimmer of hope: the atlas system should hopefully be rewritten in the future to make scaling more natural and automatic.)""".trimMargin())
                        inputFloat("Font offset", font.displayOffset::y, 1f, 1f)
                        text("Ascent: ${font.ascent}, Descent: ${font.descent}, Height: ${font.ascent - font.descent}")
                        text("Fallback character: '${font.fallbackChar}' (${font.fallbackChar.i})")
                        val side = sqrt(font.metricsTotalSurface.f).i
                        text("Texture surface: ${font.metricsTotalSurface} pixels (approx) ~ ${side}x$side")
                        for (c in 0 until font.configDataCount)
                            font.configData.getOrNull(c)?.let {
                                bulletText("Input $c: '${it.name}', Oversample: ${it.oversample}, PixelSnapH: ${it.pixelSnapH}")
                            }
                        treeNode("Glyphs", "Glyphs (${font.glyphs.size})") {
                            // Display all glyphs of the fonts in separate pages of 256 characters
                            // Forcefully/dodgily make FindGlyph() return NULL on fallback, which isn't the default behavior.
                            for (base in 0 until 0x10000 step 256) {
                                val count = (0 until 256).count { font.findGlyphNoFallback(base + it) != null }
                                val s = if (count > 1) "glyphs" else "glyph"
                                if (count > 0 && treeNode(base, "U+%04X..U+%04X ($count $s)", base, base + 255)) {
                                    val cellSize = font.fontSize * 1
                                    val cellSpacing = style.itemSpacing.y
                                    val basePos = Vec2(cursorScreenPos)
                                    val drawList = windowDrawList
                                    for (n in 0 until 256) {
                                        val cellP1 = Vec2(basePos.x + (n % 16) * (cellSize + cellSpacing),
                                                basePos.y + (n / 16) * (cellSize + cellSpacing))
                                        val cellP2 = Vec2(cellP1.x + cellSize, cellP1.y + cellSize)
                                        val glyph = font.findGlyphNoFallback((base + n).c)
                                        drawList.addRect(cellP1, cellP2, COL32(255, 255, 255, if (glyph != null) 100 else 50))
                                        /*  We use ImFont::RenderChar as a shortcut because we don't have UTF-8 conversion
                                            functions available to generate a string.                                     */
                                        if (glyph != null) {
                                            font.renderChar(drawList, cellSize, cellP1, Col.Text.u32, (base + n).c)
                                            if (isMouseHoveringRect(cellP1, cellP2))
                                                tooltip {
                                                    text("Codepoint: U+%04X", base + n)
                                                    separator()
                                                    text("AdvanceX+1: %.1f", glyph.advanceX)
                                                    text("Pos: (%.2f,%.2f)->(%.2f,%.2f)", glyph.x0, glyph.y0, glyph.x1, glyph.y1)
                                                    text("UV: (%.3f,%.3f)->(%.3f,%.3f)", glyph.u0, glyph.v0, glyph.u1, glyph.v1)
                                                }
                                        }
                                    }
                                    dummy(Vec2((cellSize + cellSpacing) * 16, (cellSize + cellSpacing) * 16))
                                    treePop()
                                }
                            }
                        }
                        treePop()
                    }
                    popId()
                }
                treeNode("Atlas texture", "Atlas texture (${atlas.texSize.x}x${atlas.texSize.y} pixels)") {
                    val tintCol = Vec4(1f)
                    val borderCol = Vec4(1f, 1f, 1f, 0.5f)
                    image(atlas.texId, Vec2(atlas.texSize), Vec2(), Vec2(1), tintCol, borderCol)
                }

                if (dragFloat("this window scale", ::windowScale, 0.005f, 0.3f, 2f, "%.2f"))    // scale only this window
                    setWindowFontScale(windowScale)
                dragFloat("global scale", io::fontGlobalScale, 0.005f, 0.3f, 2f, "%.2f") // scale everything
                popItemWidth()

                endTabItem()
            }

            if (beginTabItem("Rendering")) {
                checkbox("Anti-aliased lines", style::antiAliasedLines)
                checkbox("Anti-aliased fill", style::antiAliasedFill)
                pushItemWidth(100)
                dragFloat("Curve Tessellation Tolerance", style::curveTessellationTol, 0.02f, 0.1f, Float.MAX_VALUE, "%.2f", 2f)
                if (style.curveTessellationTol < 10f) style.curveTessellationTol = 0.1f
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
}