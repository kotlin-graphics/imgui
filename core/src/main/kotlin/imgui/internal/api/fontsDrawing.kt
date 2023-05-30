package imgui.internal.api

import glm_.func.common.max
import imgui.ImGui
import imgui.ImGui.foregroundDrawList
import imgui.api.g
import imgui.classes.DrawList
import imgui.font.Font
import imgui.internal.classes.Window

// Fonts, drawing
internal interface fontsDrawing {

    /** Important: this alone doesn't alter current ImDrawList state. This is called by PushFont/PopFont only. */
    fun setCurrentFont(font: Font) {
        assert(font.isLoaded) { "Font Atlas not created. Did you call io.Fonts->GetTexDataAsRGBA32 / GetTexDataAsAlpha8 ?" }
        assert(font.scale > 0f)
        g.font = font
        g.fontBaseSize = 1f max (ImGui.io.fontGlobalScale * g.font.fontSize * g.font.scale)
        g.fontSize = g.currentWindow?.calcFontSize() ?: 0f

        val atlas = g.font.containerAtlas
        g.drawListSharedData.also {
            it.texUvWhitePixel = atlas.texUvWhitePixel
            it.texUvLines = atlas.texUvLines
            it.font = g.font
            it.fontSize = g.fontSize
        }
    }

    /** ~GetDefaultFont */
    val defaultFont: Font
        get() = ImGui.io.fontDefault ?: ImGui.io.fonts.fonts[0]

    fun getForegroundDrawList(window: Window?): DrawList = ImGui.foregroundDrawList // This seemingly unnecessary wrapper simplifies compatibility between the 'master' and 'docking' branches.

    // GetBackgroundDrawList(ImGuiViewport* viewport)
    // GetForegroundDrawList(ImGuiViewport* viewport); -> Viewport class
}