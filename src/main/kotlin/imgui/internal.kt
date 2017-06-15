package imgui

import glm_.i
import glm_.vec2.Vec2
import glm_.xor

// Mouse cursor data (used when io.MouseDrawCursor is set)
class MouseCursorData {
    var type = MouseCursor_.None
    var hotOffset = Vec2()
    var size = Vec2()
    var texUvMin = arrayOf(Vec2(), Vec2())
    var texUvMax = arrayOf(Vec2(), Vec2())
}

