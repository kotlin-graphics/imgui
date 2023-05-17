package imgui.api

import glm_.L
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.ImGui
import imgui.ImGui.imageButtonEx
import imgui.TextureID
import imgui.internal.classes.Rect

// Widgets: Images
// - Read about ImTextureID here: https://github.com/ocornut/imgui/wiki/Image-Loading-and-Displaying-Examples
interface widgetsImages {

    fun image(userTextureId: TextureID, size: Vec2, uv0: Vec2 = Vec2(), uv1: Vec2 = Vec2(1), tintCol: Vec4 = Vec4(1),
              borderCol: Vec4 = Vec4()) {

        val window = ImGui.currentWindow
        if (window.skipItems) return

        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        if (borderCol.w > 0f) bb.max plusAssign 2
        ImGui.itemSize(bb)
        if (!ImGui.itemAdd(bb, 0)) return

        if (borderCol.w > 0f) {
            window.drawList.addRect(bb.min, bb.max, ImGui.getColorU32(borderCol), 0f)
            window.drawList.addImage(userTextureId, bb.min + 1, bb.max - 1, uv0, uv1, ImGui.getColorU32(tintCol))
        } else
            window.drawList.addImage(userTextureId, bb.min, bb.max, uv0, uv1, ImGui.getColorU32(tintCol))
    }

    /** frame_padding < 0: uses FramePadding from style (default)
     *  frame_padding = 0: no framing/padding
     *  frame_padding > 0: set framing size
     *  The color used are the button colors.   */
    fun imageButton(strId: String, userTextureId: TextureID, size: Vec2, uv0: Vec2 = Vec2(), uv1: Vec2 = Vec2(),
                    bgCol: Vec4 = Vec4(), tintCol: Vec4 = Vec4(1)): Boolean {

        val window = ImGui.currentWindow
        if (window.skipItems)
            return false

        return imageButtonEx(window.getID(strId), userTextureId, size, uv0, uv1, bgCol, tintCol)
    }

}