package imgui

import glm.vec2.Vec2

// Mouse cursor data (used when io.MouseDrawCursor is set)
class MouseCursorData {
    var type = MouseCursor_.None
    var hotOffset = Vec2()
    var size = Vec2()
    var texUvMin = arrayOf(Vec2(), Vec2())
    var texUvMax = arrayOf(Vec2(), Vec2())
}