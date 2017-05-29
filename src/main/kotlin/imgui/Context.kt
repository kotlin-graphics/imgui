package imgui

import glm.vec2.Vec2


object Context {

    var initialized = false

    val io = IO

    val style = Style

//    ImFont*                 Font;                               // (Shortcut) == FontStack.empty() ? IO.Font : FontStack.back()

    /** (Shortcut) == FontBaseSize * g.CurrentWindow->FontWindowScale == window->FontSize() */
    var fontSize = 0f
    /** (Shortcut) == IO.FontGlobalScale * Font->Scale * Font->FontSize. Size of characters.    */
    var fontBaseSize = 0f
    /** (Shortcut) == Font->TexUvWhitePixel */
    var fontTexUvWhitePixel = Vec2()

    var Time = 0.0f
    var FrameCount = 0
    var FrameCountEnded = -1
    var FrameCountRendered = -1
}