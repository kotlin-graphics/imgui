package imgui.imgui

import glm_.vec2.Vec2
import imgui.ImGui

interface imgui_functionalProgramming {

    fun button(label: String, sizeArg: Vec2 = Vec2(), block: () -> Unit) {
        if (ImGui.buttonEx(label, sizeArg, 0))
            block()
    }
}