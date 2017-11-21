package imgui.imgui

import glm_.vec4.Vec4
import imgui.Style
import imgui.Context as g

/** Styles  */
interface imgui_styles {

    fun styleColorsClassic(dst: Style? = null) = with(dst ?: g.style) {
        colors.clear()
        colors.addAll(arrayOf(
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
                Vec4(0.27f, 0.27f, 0.54f, 0.83f),
                Vec4(0.32f, 0.32f, 0.63f, 0.87f),
                Vec4(0.40f, 0.40f, 0.80f, 0.20f),
                Vec4(0.40f, 0.40f, 0.55f, 0.80f),
                Vec4(0.20f, 0.25f, 0.30f, 0.60f),
                Vec4(0.40f, 0.40f, 0.80f, 0.30f),
                Vec4(0.40f, 0.40f, 0.80f, 0.40f),
                Vec4(0.80f, 0.50f, 0.50f, 0.40f),
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

    fun styleColorsDark(dst: Style? = null) = with(dst ?: g.style) {
        colors.clear()
        val borderSepator = Vec4(1.00f, 1.00f, 1.00f, 0.19f)
        colors.addAll(arrayOf(
                Vec4(1.00f, 1.00f, 1.00f, 1.00f),
                Vec4(0.50f, 0.50f, 0.50f, 1.00f),
                Vec4(0.06f, 0.06f, 0.06f, 0.94f),
                Vec4(1.00f, 1.00f, 1.00f, 0.00f),
                Vec4(0.00f, 0.00f, 0.00f, 0.94f),
                Vec4(borderSepator),
                Vec4(0.00f, 0.00f, 0.00f, 0.00f),
                Vec4(0.16f, 0.29f, 0.48f, 0.54f),
                Vec4(0.26f, 0.59f, 0.98f, 0.40f),
                Vec4(0.26f, 0.59f, 0.98f, 0.67f),
                Vec4(0.04f, 0.04f, 0.04f, 1.00f),
                Vec4(0.18f, 0.18f, 0.18f, 1.00f),
                Vec4(0.00f, 0.00f, 0.00f, 0.51f),
                Vec4(0.14f, 0.14f, 0.14f, 1.00f),
                Vec4(0.02f, 0.02f, 0.02f, 0.53f),
                Vec4(0.31f, 0.31f, 0.31f, 1.00f),
                Vec4(0.41f, 0.41f, 0.41f, 1.00f),
                Vec4(0.51f, 0.51f, 0.51f, 1.00f),
                Vec4(0.26f, 0.59f, 0.98f, 1.00f),
                Vec4(0.24f, 0.52f, 0.88f, 1.00f),
                Vec4(0.26f, 0.59f, 0.98f, 1.00f),
                Vec4(0.26f, 0.59f, 0.98f, 0.40f),
                Vec4(0.26f, 0.59f, 0.98f, 1.00f),
                Vec4(0.06f, 0.53f, 0.98f, 1.00f),
                Vec4(0.26f, 0.59f, 0.98f, 0.31f),
                Vec4(0.26f, 0.59f, 0.98f, 0.80f),
                Vec4(0.26f, 0.59f, 0.98f, 1.00f),
                Vec4(borderSepator),
                Vec4(0.26f, 0.59f, 0.98f, 0.78f),
                Vec4(0.26f, 0.59f, 0.98f, 1.00f),
                Vec4(0.26f, 0.59f, 0.98f, 0.25f),
                Vec4(0.26f, 0.59f, 0.98f, 0.67f),
                Vec4(0.26f, 0.59f, 0.98f, 0.95f),
                Vec4(0.41f, 0.41f, 0.41f, 0.50f),
                Vec4(0.98f, 0.39f, 0.36f, 1.00f),
                Vec4(0.98f, 0.39f, 0.36f, 1.00f),
                Vec4(0.61f, 0.61f, 0.61f, 1.00f),
                Vec4(1.00f, 0.43f, 0.35f, 1.00f),
                Vec4(0.90f, 0.70f, 0.00f, 1.00f),
                Vec4(1.00f, 0.60f, 0.00f, 1.00f),
                Vec4(0.26f, 0.59f, 0.98f, 0.35f),
                Vec4(0.80f, 0.80f, 0.80f, 0.35f)))
    }
}