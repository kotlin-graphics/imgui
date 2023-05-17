package imgui.api

import glm_.f
import glm_.glm
import glm_.i
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import imgui.Vec3Setter
import imgui.put


/** Color Utilities */
interface colorUtilities {

    /** Convert rgb floats ([0-1],[0-1],[0-1]) to hsv floats ([0-1],[0-1],[0-1]), from Foley & van Dam p592
     *  Optimized http://lolengine.net/blog/2013/01/13/fast-rgb-to-hsv  */
    fun colorConvertRGBtoHSV(rgb: Vec3, hsv: Vec3 = Vec3()): Vec3 = colorConvertRGBtoHSV(rgb.r, rgb.g, rgb.b, hsv)
    fun colorConvertRGBtoHSV(rgb: Vec4, hsv: Vec4 = Vec4()): Vec4 = colorConvertRGBtoHSV(rgb.r, rgb.g, rgb.b, hsv)
    fun colorConvertRGBtoHSV(r: Float, g: Float, b: Float, hsv: Vec3) = hsv.apply {
        colorConvertRGBtoHSV(r, g, b, ::put)
    }

    fun colorConvertRGBtoHSV(r: Float, g: Float, b: Float, hsv: Vec4) = hsv.apply {
        colorConvertRGBtoHSV(r, g, b, ::put)
    }

    fun Vec3.rgbToHSV() = colorConvertRGBtoHSV(this, this)
    fun Vec4.rgbToHSV() = colorConvertRGBtoHSV(this, this)

    /** Convert hsv floats ([0-1],[0-1],[0-1]) to rgb floats ([0-1],[0-1],[0-1]), from Foley & van Dam p593
     *  also http://en.wikipedia.org/wiki/HSL_and_HSV   */
    fun colorConvertHSVtoRGB(hsv: Vec3, rgb: Vec3 = Vec3()): Vec3 = colorConvertHSVtoRGB(hsv.x, hsv.y, hsv.z, rgb)

    fun colorConvertHSVtoRGB(hsv: Vec4, rgb: Vec4 = Vec4()): Vec4 = colorConvertHSVtoRGB(hsv.x, hsv.y, hsv.z, rgb)

    fun colorConvertHSVtoRGB(h: Float, s: Float, v: Float, rgb: Vec3 = Vec3()): Vec3 = rgb.apply {
        colorConvertHSVtoRGB(h, s, v, ::put)
    }

    fun colorConvertHSVtoRGB(h: Float, s: Float, v: Float, rgb: Vec4 = Vec4(), unit: Unit = Unit): Vec4 = rgb.apply {
        colorConvertHSVtoRGB(h, s, v, ::put)
    }

    fun Vec3.hsvToRGB() = colorConvertHSVtoRGB(this, this)
    fun Vec4.hsvToRGB() = colorConvertHSVtoRGB(this, this)
}

inline fun colorConvertRGBtoHSV(rgb: Vec3, hsvSetter: Vec3Setter) {
    colorConvertRGBtoHSV(rgb.x, rgb.y, rgb.z, hsvSetter)
}

inline fun colorConvertRGBtoHSV(rgb: Vec4, hsvSetter: Vec3Setter) {
    colorConvertRGBtoHSV(rgb.x, rgb.y, rgb.z, hsvSetter)
}

inline fun colorConvertRGBtoHSV(r_: Float, g_: Float, b_: Float, hsvSetter: Vec3Setter) {
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
    hsvSetter(glm.abs(k + (g - b) / (6f * chroma + 1e-20f)), chroma / (r + 1e-20f), r)
}

inline fun colorConvertHSVtoRGB(hsv: Vec3, rgbSetter: Vec3Setter) {
    colorConvertHSVtoRGB(hsv.x, hsv.y, hsv.z, rgbSetter)
}

inline fun colorConvertHSVtoRGB(hsv: Vec4, rgbSetter: Vec3Setter) {
    colorConvertHSVtoRGB(hsv.x, hsv.y, hsv.z, rgbSetter)
}

inline fun colorConvertHSVtoRGB(h_: Float, s: Float, v: Float, rgbSetter: Vec3Setter) {
    if (s == 0f) {
        // gray
        return rgbSetter(v, v, v)
    }

    val h = glm.mod(h_, 1f) / (60f / 360f)
    val i = h.i
    val f = h - i.f
    val p = v * (1f - s)
    val q = v * (1f - s * f)
    val t = v * (1f - s * (1f - f))

    when (i) {
        0 -> rgbSetter(v, t, p)
        1 -> rgbSetter(q, v, p)
        2 -> rgbSetter(p, v, t)
        3 -> rgbSetter(p, q, v)
        4 -> rgbSetter(t, p, v)
        else -> rgbSetter(v, p, q)
    }
}