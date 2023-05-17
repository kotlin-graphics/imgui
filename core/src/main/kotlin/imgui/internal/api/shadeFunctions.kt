package imgui.internal.api

import glm_.glm
import glm_.i
import glm_.vec2.Vec2
import imgui.COL32_A_MASK
import imgui.COL32_B_SHIFT
import imgui.COL32_G_SHIFT
import imgui.COL32_R_SHIFT
import imgui.classes.DrawList
import imgui.internal.DrawVert
import imgui.internal.lengthSqr

internal interface shadeFunctions {

    /** Generic linear color gradient, write to RGB fields, leave A untouched.  */
    fun DrawList.shadeVertsLinearColorGradientKeepAlpha(
        vertStart: Int, vertEnd: Int, gradientP0: Vec2,
        gradientP1: Vec2, col0: Int, col1: Int,
                                              ) {
        val gradientExtent = gradientP1 - gradientP0
        val gradientInvLength2 = 1f / gradientExtent.lengthSqr
        val col0R = (col0 ushr COL32_R_SHIFT) and 0xFF
        val col0G = (col0 ushr COL32_G_SHIFT) and 0xFF
        val col0B = (col0 ushr COL32_B_SHIFT) and 0xFF
        val colDeltaR = ((col1 ushr COL32_R_SHIFT) and 0xFF) - col0R
        val colDeltaG = ((col1 ushr COL32_G_SHIFT) and 0xFF) - col0G
        val colDeltaB = ((col1 ushr COL32_B_SHIFT) and 0xFF) - col0B
        for (i in vertStart until vertEnd) {
            var offset = i * DrawVert.SIZE
            val pos = Vec2(vtxBuffer.data, offset)
            val d = pos - gradientP0 dot gradientExtent
            val t = glm.clamp(d * gradientInvLength2, 0f, 1f)
            val r = (col0R + colDeltaR * t).i
            val g = (col0G + colDeltaG * t).i
            val b = (col0B + colDeltaB * t).i
            offset += Vec2.size * 2
            val col = vtxBuffer.data.getInt(offset)
            val newCol = (r shl COL32_R_SHIFT) or (g shl COL32_G_SHIFT) or (b shl COL32_B_SHIFT) or (col and COL32_A_MASK)
            vtxBuffer.data.putInt(offset, newCol)
        }
    }

    /** Distribute UV over (a, b) rectangle */
    fun DrawList.shadeVertsLinearUV(vertStart: Int, vertEnd: Int, a: Vec2, b: Vec2, uvA: Vec2, uvB: Vec2, clamp: Boolean) {
        val size = b - a
        val uvSize = uvB - uvA
        val scale = Vec2(
            if (size.x != 0f) uvSize.x / size.x else 0f,
            if (size.y != 0f) uvSize.y / size.y else 0f)
        if (clamp) {
            val min = uvA min uvB
            val max = uvA max uvB
            for (i in vertStart until vertEnd) {
                val vertexPos = Vec2(vtxBuffer.data, i * DrawVert.SIZE)
                val vertexUV = glm.clamp(uvA + (vertexPos - a) * scale, min, max)
                vertexUV.to(vtxBuffer.data, i * DrawVert.SIZE + Vec2.size)
            }
        } else
            for (i in vertStart until vertEnd) {
                val vertexPos = Vec2(vtxBuffer.data, i * DrawVert.SIZE)
                val vertexUV = uvA + (vertexPos - a) * scale
                vertexUV.to(vtxBuffer.data, i * DrawVert.SIZE + Vec2.size)
            }
    }
}