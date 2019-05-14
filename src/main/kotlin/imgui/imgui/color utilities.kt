package imgui.imgui

import glm_.f
import glm_.glm
import glm_.i
import glm_.vec4.Vec4
import imgui.imgui.imgui_internal.Companion._f0
import imgui.imgui.imgui_internal.Companion._f1
import imgui.imgui.imgui_internal.Companion._f2
import uno.kotlin.getValue
import uno.kotlin.setValue
import kotlin.reflect.KMutableProperty0
import imgui.FocusedFlag as Ff
import imgui.HoveredFlag as Hf
import imgui.WindowFlag as Wf


/** Color Utilities */
interface imgui_colorUtilities {

    /** Convert rgb floats ([0-1],[0-1],[0-1]) to hsv floats ([0-1],[0-1],[0-1]), from Foley & van Dam p592
     *  Optimized http://lolengine.net/blog/2013/01/13/fast-rgb-to-hsv  */
    fun colorConvertRGBtoHSV(rgb: FloatArray, hsv: FloatArray = FloatArray(3)): FloatArray =
            colorConvertRGBtoHSV(rgb[0], rgb[1], rgb[2], hsv)

    fun colorConvertRGBtoHSV(r_: Float, g_: Float, b_: Float, hsv: FloatArray = FloatArray(3)): FloatArray {

        var k = 0f
        var r = r_
        var g = g_
        var b = b_
        if (g < b) {
            val tmp = g; g = b; b = tmp
            k = -1f
        }
        if (r < g) {
            val tmp = r; r = g; g = tmp
            k = -2f / 6f - k
        }

        val chroma = r - (if (g < b) g else b)
        hsv[0] = glm.abs(k + (g - b) / (6f * chroma + 1e-20f))
        hsv[1] = chroma / (r + 1e-20f)
        hsv[2] = r
        return hsv
    }

    fun FloatArray.rgbToHSV() = colorConvertRGBtoHSV(this, this)

    /** Convert hsv floats ([0-1],[0-1],[0-1]) to rgb floats ([0-1],[0-1],[0-1]), from Foley & van Dam p593
     *  also http://en.wikipedia.org/wiki/HSL_and_HSV   */
    fun colorConvertHSVtoRGB(hsv: FloatArray, rgb: FloatArray = FloatArray(3)) = colorConvertHSVtoRGB(hsv[0], hsv[1], hsv[2], rgb)

    fun colorConvertHSVtoRGB(h: Float, s: Float, v: Float, rgb: FloatArray = FloatArray(3)): FloatArray {
        colorConvertHSVtoRGB(h, s, v, ::_f0, ::_f1, ::_f2)
        return rgb.apply { set(0, _f0); set(1, _f1); set(2, _f2) }
    }

    fun colorConvertHSVtoRGB(h_: Float, s: Float, v: Float, rPtr: KMutableProperty0<Float>, gPtr: KMutableProperty0<Float>,
                             bPtr: KMutableProperty0<Float>) {

        var r by rPtr
        var g by gPtr
        var b by bPtr
        if (s == 0f) {
            // gray
            r = v
            g = v
            b = v
        }

        val h = glm.mod(h_, 1f) / (60f / 360f)
        val i = h.i
        val f = h - i.f
        val p = v * (1f - s)
        val q = v * (1f - s * f)
        val t = v * (1f - s * (1f - f))

        when (i) {
            0 -> {
                r = v; g = t; b = p; }
            1 -> {
                r = q; g = v; b = p; }
            2 -> {
                r = p; g = v; b = t; }
            3 -> {
                r = p; g = q; b = v; }
            4 -> {
                r = t; g = p; b = v; }
            else -> {
                r = v; g = p; b = q; }
        }
    }

    fun colorConvertHSVtoRGB(col: Vec4) {

        val h_ = col.x
        val s = col.y
        val v = col.z
        var r: Float
        var g: Float
        var b: Float
        if (s == 0f) {
            // gray
            r = v
            g = v
            b = v
        }

        val h = glm.mod(h_, 1f) / (60f / 360f)
        val i = h.i
        val f = h - i.f
        val p = v * (1f - s)
        val q = v * (1f - s * f)
        val t = v * (1f - s * (1f - f))

        when (i) {
            0 -> {
                r = v; g = t; b = p; }
            1 -> {
                r = q; g = v; b = p; }
            2 -> {
                r = p; g = v; b = t; }
            3 -> {
                r = p; g = q; b = v; }
            4 -> {
                r = t; g = p; b = v; }
            else -> {
                r = v; g = p; b = q; }
        }
        col.x = r
        col.y = g
        col.z = b
    }

    fun FloatArray.hsvToRGB() = colorConvertHSVtoRGB(this, this)
}