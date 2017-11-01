package imgui.imgui

import glm_.vec4.Vec4
import imgui.Style
import imgui.Context as g

/** Styles  */
interface imgui_styles {

    fun styleColorsClassic(dst: Style? = null) {

        val style = dst ?: g.style
        style.colors.addAll(arrayOf(
                Vec4(0.90f, 0.90f, 0.90f, 1.00f),
                Vec4(0.60f, 0.60f, 0.60f, 1.00f),
                Vec4(0.00f, 0.00f, 0.00f, 0.70f),
                Vec4(0.00f, 0.00f, 0.00f, 0.00f),
                Vec4(0.05f, 0.05f, 0.10f, 0.90f),
                Vec4(0.70f, 0.70f, 0.70f, 0.40f),
                Vec4(0.00f, 0.00f, 0.00f, 0.00f),
                Vec4(0.80f, 0.80f, 0.80f, 0.30f),   // Background of checkbox, radio button, plot, slider, text input
                Vec4(0.90f, 0.80f, 0.80f, 0.40f),
                Vec4(0.90f, 0.65f, 0.65f, 0.45f),
                Vec4(0.27f, 0.27f, 0.54f, 0.83f),   // TitleBg
                Vec4(0.32f, 0.32f, 0.63f, 0.87f),   // TitleBgActive
                Vec4(0.40f, 0.40f, 0.80f, 0.20f),   // TitleBgCollapsed
                Vec4(0.40f, 0.40f, 0.55f, 0.80f),
                Vec4(0.20f, 0.25f, 0.30f, 0.60f),
                Vec4(0.40f, 0.40f, 0.80f, 0.30f),
                Vec4(0.40f, 0.40f, 0.80f, 0.40f),
                Vec4(0.80f, 0.50f, 0.50f, 0.40f),
                Vec4(0.20f, 0.20f, 0.20f, 0.99f),
                Vec4(0.90f, 0.90f, 0.90f, 0.50f),
                Vec4(1.00f, 1.00f, 1.00f, 0.30f),
                Vec4(0.80f, 0.50f, 0.50f, 1.00f),
                Vec4(0.67f, 0.40f, 0.40f, 0.60f),
                Vec4(0.67f, 0.40f, 0.40f, 1.00f),
                Vec4(0.80f, 0.50f, 0.50f, 1.00f),
                Vec4(0.40f, 0.40f, 0.90f, 0.45f),
                Vec4(0.45f, 0.45f, 0.90f, 0.80f),
                Vec4(0.53f, 0.53f, 0.87f, 0.80f),
                Vec4(0.50f, 0.50f, 0.50f, 1.00f),
                Vec4(0.60f, 0.60f, 0.70f, 1.00f),
                Vec4(0.70f, 0.70f, 0.90f, 1.00f),
                Vec4(1.00f, 1.00f, 1.00f, 0.30f),
                Vec4(1.00f, 1.00f, 1.00f, 0.60f),
                Vec4(1.00f, 1.00f, 1.00f, 0.90f),
                Vec4(0.50f, 0.50f, 0.90f, 0.50f),
                Vec4(0.70f, 0.70f, 0.90f, 0.60f),
                Vec4(0.70f, 0.70f, 0.70f, 1.00f),
                Vec4(1.00f, 1.00f, 1.00f, 1.00f),
                Vec4(0.90f, 0.70f, 0.00f, 1.00f),
                Vec4(0.90f, 0.70f, 0.00f, 1.00f),
                Vec4(1.00f, 0.60f, 0.00f, 1.00f),
                Vec4(0.00f, 0.00f, 1.00f, 0.35f),
                Vec4(0.20f, 0.20f, 0.20f, 0.35f)))
    }
}