package imgui.imgui

import glm_.vec4.Vec4
import imgui.Col
import imgui.Style
import imgui.g
import imgui.internal.lerp

/** Styles  */
interface imgui_styles {

    operator fun <T> ArrayList<T>.get(col: Col): T = get(col.i)

    /*
     * Dont ever touch the formatting unless you have the option enabled in Idea,
     *
     * https://stackoverflow.com/questions/3375307/how-to-disable-code-formatting-for-some-part-of-the-code-using-comments
     *
     * or I'll kill you slowly
     */

    /** New, recommended style  */
    fun styleColorsDark(dst: Style = g.style) = dst.apply {
        colors.clear()
        for (c in Col.values()) colors += Vec4()
        // @formatter:off
        colors[Col.Text]                    (1.00f, 1.00f, 1.00f, 1.00f)
        colors[Col.TextDisabled]            (0.50f, 0.50f, 0.50f, 1.00f)
        colors[Col.WindowBg]                (0.06f, 0.06f, 0.06f, 0.94f)
        colors[Col.ChildBg]                 (0.00f, 0.00f, 0.00f, 0.00f)
        colors[Col.PopupBg]                 (0.08f, 0.08f, 0.08f, 0.94f)
        colors[Col.Border]                  (0.43f, 0.43f, 0.50f, 0.50f)
        colors[Col.BorderShadow]            (0.00f, 0.00f, 0.00f, 0.00f)
        colors[Col.FrameBg]                 (0.16f, 0.29f, 0.48f, 0.54f)
        colors[Col.FrameBgHovered]          (0.26f, 0.59f, 0.98f, 0.40f)
        colors[Col.FrameBgActive]           (0.26f, 0.59f, 0.98f, 0.67f)
        colors[Col.TitleBg]                 (0.04f, 0.04f, 0.04f, 1.00f)
        colors[Col.TitleBgActive]           (0.16f, 0.29f, 0.48f, 1.00f)
        colors[Col.TitleBgCollapsed]        (0.00f, 0.00f, 0.00f, 0.51f)
        colors[Col.MenuBarBg]               (0.14f, 0.14f, 0.14f, 1.00f)
        colors[Col.ScrollbarBg]             (0.02f, 0.02f, 0.02f, 0.53f)
        colors[Col.ScrollbarGrab]           (0.31f, 0.31f, 0.31f, 1.00f)
        colors[Col.ScrollbarGrabHovered]    (0.41f, 0.41f, 0.41f, 1.00f)
        colors[Col.ScrollbarGrabActive]     (0.51f, 0.51f, 0.51f, 1.00f)
        colors[Col.CheckMark]               (0.26f, 0.59f, 0.98f, 1.00f)
        colors[Col.SliderGrab]              (0.24f, 0.52f, 0.88f, 1.00f)
        colors[Col.SliderGrabActive]        (0.26f, 0.59f, 0.98f, 1.00f)
        colors[Col.Button]                  (0.26f, 0.59f, 0.98f, 0.40f)
        colors[Col.ButtonHovered]           (0.26f, 0.59f, 0.98f, 1.00f)
        colors[Col.ButtonActive]            (0.06f, 0.53f, 0.98f, 1.00f)
        colors[Col.Header]                  (0.26f, 0.59f, 0.98f, 0.31f)
        colors[Col.HeaderHovered]           (0.26f, 0.59f, 0.98f, 0.80f)
        colors[Col.HeaderActive]            (0.26f, 0.59f, 0.98f, 1.00f)
        colors[Col.Separator]               (colors[Col.Border])
        colors[Col.SeparatorHovered]        (0.10f, 0.40f, 0.75f, 0.78f)
        colors[Col.SeparatorActive]         (0.10f, 0.40f, 0.75f, 1.00f)
        colors[Col.ResizeGrip]              (0.26f, 0.59f, 0.98f, 0.25f)
        colors[Col.ResizeGripHovered]       (0.26f, 0.59f, 0.98f, 0.67f)
        colors[Col.ResizeGripActive]        (0.26f, 0.59f, 0.98f, 0.95f)
        colors[Col.Tab]                     (colors[Col.Header].lerp(colors[Col.TitleBgActive], 0.80f))
        colors[Col.TabHovered]              (colors[Col.HeaderHovered])
        colors[Col.TabActive]               (colors[Col.HeaderActive].lerp(colors[Col.TitleBgActive], 0.60f))
        colors[Col.TabUnfocused]            (colors[Col.Tab].lerp(colors[Col.TitleBg], 0.80f))
        colors[Col.TabUnfocusedActive]      (colors[Col.TabActive].lerp(colors[Col.TitleBg], 0.40f))
        colors[Col.PlotLines]               (0.61f, 0.61f, 0.61f, 1.00f )
        colors[Col.PlotLinesHovered]        (1.00f, 0.43f, 0.35f, 1.00f)
        colors[Col.PlotHistogram]           (0.90f, 0.70f, 0.00f, 1.00f)
        colors[Col.PlotHistogramHovered]    (1.00f, 0.60f, 0.00f, 1.00f)
        colors[Col.TextSelectedBg]          (0.26f, 0.59f, 0.98f, 0.35f)
        colors[Col.DragDropTarget]          (1.00f, 1.00f, 0.00f, 0.90f)
        colors[Col.NavHighlight]            (0.26f, 0.59f, 0.98f, 1.00f)
        colors[Col.NavWindowingHighlight]   (1.00f, 1.00f, 1.00f, 0.70f)
        colors[Col.NavWindowingDimBg]       (0.80f, 0.80f, 0.80f, 0.20f)
        colors[Col.ModalWindowDimBg]        (0.80f, 0.80f, 0.80f, 0.35f)
        // @formatter:on
    }

    /** Classic imgui style (default)   */
    fun styleColorsClassic(dst: Style = g.style) = dst.apply {
        colors.clear()
        for (c in Col.values())
            colors += Vec4()
        // @formatter:off
        colors[Col.Text]                    (0.90f, 0.90f, 0.90f, 1.00f)
        colors[Col.TextDisabled]            (0.60f, 0.60f, 0.60f, 1.00f)
        colors[Col.WindowBg]                (0.00f, 0.00f, 0.00f, 0.70f)
        colors[Col.ChildBg]                 (0.00f, 0.00f, 0.00f, 0.00f)
        colors[Col.PopupBg]                 (0.11f, 0.11f, 0.14f, 0.92f)
        colors[Col.Border]                  (0.50f, 0.50f, 0.50f, 0.50f)
        colors[Col.BorderShadow]            (0.00f, 0.00f, 0.00f, 0.00f)
        colors[Col.FrameBg]                 (0.43f, 0.43f, 0.43f, 0.39f)
        colors[Col.FrameBgHovered]          (0.47f, 0.47f, 0.69f, 0.40f)
        colors[Col.FrameBgActive]           (0.42f, 0.41f, 0.64f, 0.69f)
        colors[Col.TitleBg]                 (0.27f, 0.27f, 0.54f, 0.83f)
        colors[Col.TitleBgActive]           (0.32f, 0.32f, 0.63f, 0.87f)
        colors[Col.TitleBgCollapsed]        (0.40f, 0.40f, 0.80f, 0.20f)
        colors[Col.MenuBarBg]               (0.40f, 0.40f, 0.55f, 0.80f)
        colors[Col.ScrollbarBg]             (0.20f, 0.25f, 0.30f, 0.60f)
        colors[Col.ScrollbarGrab]           (0.40f, 0.40f, 0.80f, 0.30f)
        colors[Col.ScrollbarGrabHovered]    (0.40f, 0.40f, 0.80f, 0.40f)
        colors[Col.ScrollbarGrabActive]     (0.41f, 0.39f, 0.80f, 0.60f)
        colors[Col.CheckMark]               (0.90f, 0.90f, 0.90f, 0.50f)
        colors[Col.SliderGrab]              (1.00f, 1.00f, 1.00f, 0.30f)
        colors[Col.SliderGrabActive]        (0.41f, 0.39f, 0.80f, 0.60f)
        colors[Col.Button]                  (0.35f, 0.40f, 0.61f, 0.62f)
        colors[Col.ButtonHovered]           (0.40f, 0.48f, 0.71f, 0.79f)
        colors[Col.ButtonActive]            (0.46f, 0.54f, 0.80f, 1.00f)
        colors[Col.Header]                  (0.40f, 0.40f, 0.90f, 0.45f)
        colors[Col.HeaderHovered]           (0.45f, 0.45f, 0.90f, 0.80f)
        colors[Col.HeaderActive]            (0.53f, 0.53f, 0.87f, 0.80f)
        colors[Col.Separator]               (0.50f, 0.50f, 0.50f, 1.00f)
        colors[Col.SeparatorHovered]        (0.60f, 0.60f, 0.70f, 1.00f)
        colors[Col.SeparatorActive]         (0.70f, 0.70f, 0.90f, 1.00f)
        colors[Col.ResizeGrip]              (1.00f, 1.00f, 1.00f, 0.16f)
        colors[Col.ResizeGripHovered]       (0.78f, 0.82f, 1.00f, 0.60f)
        colors[Col.ResizeGripActive]        (0.78f, 0.82f, 1.00f, 0.90f)
        colors[Col.Tab]                     (colors[Col.Header].lerp(colors[Col.TitleBgActive], 0.80f))
        colors[Col.TabHovered]              (colors[Col.HeaderHovered])
        colors[Col.TabActive]               (colors[Col.HeaderActive].lerp(colors[Col.TitleBgActive], 0.60f))
        colors[Col.TabUnfocused]            (colors[Col.Tab].lerp(colors[Col.TitleBg], 0.80f))
        colors[Col.TabUnfocusedActive]      (colors[Col.TabActive].lerp(colors[Col.TitleBg], 0.40f))
        colors[Col.PlotLines]               (1.00f, 1.00f, 1.00f, 1.00f)
        colors[Col.PlotLinesHovered]        (0.90f, 0.70f, 0.00f, 1.00f)
        colors[Col.PlotHistogram]           (0.90f, 0.70f, 0.00f, 1.00f)
        colors[Col.PlotHistogramHovered]    (1.00f, 0.60f, 0.00f, 1.00f)
        colors[Col.TextSelectedBg]          (0.00f, 0.00f, 1.00f, 0.35f)
        colors[Col.DragDropTarget]          (1.00f, 1.00f, 0.00f, 0.90f)
        colors[Col.NavHighlight]            (colors[Col.HeaderHovered])
        colors[Col.NavWindowingHighlight]   (1.00f, 1.00f, 1.00f, 0.70f)
        colors[Col.NavWindowingDimBg]       (0.80f, 0.80f, 0.80f, 0.20f)
        colors[Col.ModalWindowDimBg]        (0.20f, 0.20f, 0.20f, 0.35f)
        // @formatter:on
    }

    /** Those light colors are better suited with a thicker font than the default one + FrameBorder
     *  Best used with borders and a custom, thicker font    */
    fun styleColorsLight(dst: Style = g.style) = dst.apply {
        colors.clear()
        for (c in Col.values()) colors += Vec4()
        // @formatter:off
        colors[Col.Text]                    (0.00f, 0.00f, 0.00f, 1.00f)
        colors[Col.TextDisabled]            (0.60f, 0.60f, 0.60f, 1.00f)
        colors[Col.WindowBg]                (0.94f, 0.94f, 0.94f, 1.00f)
        colors[Col.ChildBg]                 (0.00f, 0.00f, 0.00f, 0.00f)
        colors[Col.PopupBg]                 (1.00f, 1.00f, 1.00f, 0.98f)
        colors[Col.Border]                  (0.00f, 0.00f, 0.00f, 0.30f)
        colors[Col.BorderShadow]            (0.00f, 0.00f, 0.00f, 0.00f)
        colors[Col.FrameBg]                 (1.00f, 1.00f, 1.00f, 1.00f)
        colors[Col.FrameBgHovered]          (0.26f, 0.59f, 0.98f, 0.40f)
        colors[Col.FrameBgActive]           (0.26f, 0.59f, 0.98f, 0.67f)
        colors[Col.TitleBg]                 (0.96f, 0.96f, 0.96f, 1.00f)
        colors[Col.TitleBgActive]           (0.82f, 0.82f, 0.82f, 1.00f)
        colors[Col.TitleBgCollapsed]        (1.00f, 1.00f, 1.00f, 0.51f)
        colors[Col.MenuBarBg]               (0.86f, 0.86f, 0.86f, 1.00f)
        colors[Col.ScrollbarBg]             (0.98f, 0.98f, 0.98f, 0.53f)
        colors[Col.ScrollbarGrab]           (0.69f, 0.69f, 0.69f, 0.80f)
        colors[Col.ScrollbarGrabHovered]    (0.49f, 0.49f, 0.49f, 0.80f)
        colors[Col.ScrollbarGrabActive]     (0.49f, 0.49f, 0.49f, 1.00f)
        colors[Col.CheckMark]               (0.26f, 0.59f, 0.98f, 1.00f)
        colors[Col.SliderGrab]              (0.26f, 0.59f, 0.98f, 0.78f)
        colors[Col.SliderGrabActive]        (0.46f, 0.54f, 0.80f, 0.60f)
        colors[Col.Button]                  (0.26f, 0.59f, 0.98f, 0.40f)
        colors[Col.ButtonHovered]           (0.26f, 0.59f, 0.98f, 1.00f)
        colors[Col.ButtonActive]            (0.06f, 0.53f, 0.98f, 1.00f)
        colors[Col.Header]                  (0.26f, 0.59f, 0.98f, 0.31f)
        colors[Col.HeaderHovered]           (0.26f, 0.59f, 0.98f, 0.80f)
        colors[Col.HeaderActive]            (0.26f, 0.59f, 0.98f, 1.00f)
        colors[Col.Separator]               (0.39f, 0.39f, 0.39f, 1.00f)
        colors[Col.SeparatorHovered]        (0.14f, 0.44f, 0.80f, 0.78f)
        colors[Col.SeparatorActive]         (0.14f, 0.44f, 0.80f, 1.00f)
        colors[Col.ResizeGrip]              (0.80f, 0.80f, 0.80f, 0.56f)
        colors[Col.ResizeGripHovered]       (0.26f, 0.59f, 0.98f, 0.67f)
        colors[Col.ResizeGripActive]        (0.26f, 0.59f, 0.98f, 0.95f)
        colors[Col.Tab]                     (colors[Col.Header].lerp(colors[Col.TitleBgActive], 0.90f))
        colors[Col.TabHovered]              (colors[Col.HeaderHovered])
        colors[Col.TabActive]               (colors[Col.HeaderActive].lerp(colors[Col.TitleBgActive], 0.60f))
        colors[Col.TabUnfocused]            (colors[Col.Tab].lerp(colors[Col.TitleBg], 0.80f))
        colors[Col.TabUnfocusedActive]      (colors[Col.TabActive].lerp(colors[Col.TitleBg], 0.40f))
        colors[Col.PlotLines]               (0.39f, 0.39f, 0.39f, 1.00f)
        colors[Col.PlotLinesHovered]        (1.00f, 0.43f, 0.35f, 1.00f)
        colors[Col.PlotHistogram]           (0.90f, 0.70f, 0.00f, 1.00f)
        colors[Col.PlotHistogramHovered]    (1.00f, 0.45f, 0.00f, 1.00f)
        colors[Col.TextSelectedBg]          (0.26f, 0.59f, 0.98f, 0.35f)
        colors[Col.DragDropTarget]          (0.26f, 0.59f, 0.98f, 0.95f)
        colors[Col.NavHighlight]            (colors[Col.HeaderHovered])
        colors[Col.NavWindowingHighlight]   (0.70f, 0.70f, 0.70f, 0.70f)
        colors[Col.NavWindowingDimBg]       (0.20f, 0.20f, 0.20f, 0.20f)
        colors[Col.ModalWindowDimBg]        (0.20f, 0.20f, 0.20f, 0.35f)
        // @formatter:on
    }
}