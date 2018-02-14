package imgui.imgui

import glm_.vec4.Vec4
import imgui.Col
import imgui.Style
import imgui.g

/** Styles  */
interface imgui_styles {

    operator fun <T>ArrayList<T>.get(col: Col): T = get(col.i)

    /**
     * Dont ever touch the formatting unless you have the option enabled in Idea,
     *
     * https://stackoverflow.com/questions/3375307/how-to-disable-code-formatting-for-some-part-of-the-code-using-comments
     *
     * of I'll kill you slowly
     */

    fun styleColorsClassic(dst: Style? = null) {
        with(dst ?: g.style) {
            colors.clear()
            for(c in Col.values())
                colors += Vec4()
            // @formatter:off
        colors[Col.Text]                   .put(0.90f, 0.90f, 0.90f, 1.00f)
        colors[Col.TextDisabled]           .put(0.60f, 0.60f, 0.60f, 1.00f)
        colors[Col.WindowBg]               .put(0.00f, 0.00f, 0.00f, 0.70f)
        colors[Col.ChildBg]                .put(0.00f, 0.00f, 0.00f, 0.00f)
        colors[Col.PopupBg]                .put(0.11f, 0.11f, 0.14f, 0.92f)
        colors[Col.Border]                 .put(0.50f, 0.50f, 0.50f, 0.50f)
        colors[Col.BorderShadow]           .put(0.00f, 0.00f, 0.00f, 0.00f)
        colors[Col.FrameBg]                .put(0.43f, 0.43f, 0.43f, 0.39f)
        colors[Col.FrameBgHovered]         .put(0.47f, 0.47f, 0.69f, 0.40f)
        colors[Col.FrameBgActive]          .put(0.42f, 0.41f, 0.64f, 0.69f)
        colors[Col.TitleBg]                .put(0.27f, 0.27f, 0.54f, 0.83f)
        colors[Col.TitleBgActive]          .put(0.32f, 0.32f, 0.63f, 0.87f)
        colors[Col.TitleBgCollapsed]       .put(0.40f, 0.40f, 0.80f, 0.20f)
        colors[Col.MenuBarBg]              .put(0.40f, 0.40f, 0.55f, 0.80f)
        colors[Col.ScrollbarBg]            .put(0.20f, 0.25f, 0.30f, 0.60f)
        colors[Col.ScrollbarGrab]          .put(0.40f, 0.40f, 0.80f, 0.30f)
        colors[Col.ScrollbarGrabHovered]   .put(0.40f, 0.40f, 0.80f, 0.40f)
        colors[Col.ScrollbarGrabActive]    .put(0.41f, 0.39f, 0.80f, 0.60f)
        colors[Col.CheckMark]              .put(0.90f, 0.90f, 0.90f, 0.50f)
        colors[Col.SliderGrab]             .put(1.00f, 1.00f, 1.00f, 0.30f)
        colors[Col.SliderGrabActive]       .put(0.41f, 0.39f, 0.80f, 0.60f)
        colors[Col.Button]                 .put(0.35f, 0.40f, 0.61f, 0.62f)
        colors[Col.ButtonHovered]          .put(0.40f, 0.48f, 0.71f, 0.79f)
        colors[Col.ButtonActive]           .put(0.46f, 0.54f, 0.80f, 1.00f)
        colors[Col.Header]                 .put(0.40f, 0.40f, 0.90f, 0.45f)
        colors[Col.HeaderHovered]          .put(0.45f, 0.45f, 0.90f, 0.80f)
        colors[Col.HeaderActive]           .put(0.53f, 0.53f, 0.87f, 0.80f)
        colors[Col.Separator]              .put(0.50f, 0.50f, 0.50f, 1.00f)
        colors[Col.SeparatorHovered]       .put(0.60f, 0.60f, 0.70f, 1.00f)
        colors[Col.SeparatorActive]        .put(0.70f, 0.70f, 0.90f, 1.00f)
        colors[Col.ResizeGrip]             .put(1.00f, 1.00f, 1.00f, 0.16f)
        colors[Col.ResizeGripHovered]      .put(0.78f, 0.82f, 1.00f, 0.60f)
        colors[Col.ResizeGripActive]       .put(0.78f, 0.82f, 1.00f, 0.90f)
        colors[Col.CloseButton]            .put(0.50f, 0.50f, 0.90f, 0.50f)
        colors[Col.CloseButtonHovered]     .put(0.70f, 0.70f, 0.90f, 0.60f)
        colors[Col.CloseButtonActive]      .put(0.70f, 0.70f, 0.70f, 1.00f)
        colors[Col.PlotLines]              .put(1.00f, 1.00f, 1.00f, 1.00f)
        colors[Col.PlotLinesHovered]       .put(0.90f, 0.70f, 0.00f, 1.00f)
        colors[Col.PlotHistogram]          .put(0.90f, 0.70f, 0.00f, 1.00f)
        colors[Col.PlotHistogramHovered]   .put(1.00f, 0.60f, 0.00f, 1.00f)
        colors[Col.TextSelectedBg]         .put(0.00f, 0.00f, 1.00f, 0.35f)
        colors[Col.ModalWindowDarkening]   .put(0.20f, 0.20f, 0.20f, 0.35f)
        colors[Col.DragDropTarget]         .put(1.00f, 1.00f, 0.00f, 0.90f)
        colors[Col.NavHighlight]            put colors[Col.HeaderHovered]
        colors[Col.NavWindowingHighlight]  .put(1.00f, 1.00f, 1.00f, 0.70f)
        // @formatter:on
        }
    }

    fun styleColorsDark(dst: Style? = null) = with(dst ?: g.style) {
        colors.clear()
        for (c in Col.values()) colors += Vec4()
        // @formatter:off
        colors[Col.Text]                   .put(1.00f, 1.00f, 1.00f, 1.00f)
        colors[Col.TextDisabled]           .put(0.50f, 0.50f, 0.50f, 1.00f)
        colors[Col.WindowBg]               .put(0.06f, 0.06f, 0.06f, 0.94f)
        colors[Col.ChildBg]                .put(1.00f, 1.00f, 1.00f, 0.00f)
        colors[Col.PopupBg]                .put(0.08f, 0.08f, 0.08f, 0.94f)
        colors[Col.Border]                 .put(0.43f, 0.43f, 0.50f, 0.50f)
        colors[Col.BorderShadow]           .put(0.00f, 0.00f, 0.00f, 0.00f)
        colors[Col.FrameBg]                .put(0.16f, 0.29f, 0.48f, 0.54f)
        colors[Col.FrameBgHovered]         .put(0.26f, 0.59f, 0.98f, 0.40f)
        colors[Col.FrameBgActive]          .put(0.26f, 0.59f, 0.98f, 0.67f)
        colors[Col.TitleBg]                .put(0.04f, 0.04f, 0.04f, 1.00f)
        colors[Col.TitleBgActive]          .put(0.16f, 0.29f, 0.48f, 1.00f)
        colors[Col.TitleBgCollapsed]       .put(0.00f, 0.00f, 0.00f, 0.51f)
        colors[Col.MenuBarBg]              .put(0.14f, 0.14f, 0.14f, 1.00f)
        colors[Col.ScrollbarBg]            .put(0.02f, 0.02f, 0.02f, 0.53f)
        colors[Col.ScrollbarGrab]          .put(0.31f, 0.31f, 0.31f, 1.00f)
        colors[Col.ScrollbarGrabHovered]   .put(0.41f, 0.41f, 0.41f, 1.00f)
        colors[Col.ScrollbarGrabActive]    .put(0.51f, 0.51f, 0.51f, 1.00f)
        colors[Col.CheckMark]              .put(0.26f, 0.59f, 0.98f, 1.00f)
        colors[Col.SliderGrab]             .put(0.24f, 0.52f, 0.88f, 1.00f)
        colors[Col.SliderGrabActive]       .put(0.26f, 0.59f, 0.98f, 1.00f)
        colors[Col.Button]                 .put(0.26f, 0.59f, 0.98f, 0.40f)
        colors[Col.ButtonHovered]          .put(0.26f, 0.59f, 0.98f, 1.00f)
        colors[Col.ButtonActive]           .put(0.06f, 0.53f, 0.98f, 1.00f)
        colors[Col.Header]                 .put(0.26f, 0.59f, 0.98f, 0.31f)
        colors[Col.HeaderHovered]          .put(0.26f, 0.59f, 0.98f, 0.80f)
        colors[Col.HeaderActive]           .put(0.26f, 0.59f, 0.98f, 1.00f)
        colors[Col.Separator]               put colors[Col.Border]//ImVec4(0.61f, 0.61f, 0.61f, 1.00f);
        colors[Col.SeparatorHovered]       .put(0.10f, 0.40f, 0.75f, 0.78f)
        colors[Col.SeparatorActive]        .put(0.10f, 0.40f, 0.75f, 1.00f)
        colors[Col.ResizeGrip]             .put(0.26f, 0.59f, 0.98f, 0.25f)
        colors[Col.ResizeGripHovered]      .put(0.26f, 0.59f, 0.98f, 0.67f)
        colors[Col.ResizeGripActive]       .put(0.26f, 0.59f, 0.98f, 0.95f)
        colors[Col.CloseButton]            .put(0.41f, 0.41f, 0.41f, 0.50f)
        colors[Col.CloseButtonHovered]     .put(0.98f, 0.39f, 0.36f, 1.00f)
        colors[Col.CloseButtonActive]      .put(0.98f, 0.39f, 0.36f, 1.00f)
        colors[Col.PlotLines]              .put(0.61f, 0.61f, 0.61f, 1.00f)
        colors[Col.PlotLinesHovered]       .put(1.00f, 0.43f, 0.35f, 1.00f)
        colors[Col.PlotHistogram]          .put(0.90f, 0.70f, 0.00f, 1.00f)
        colors[Col.PlotHistogramHovered]   .put(1.00f, 0.60f, 0.00f, 1.00f)
        colors[Col.TextSelectedBg]         .put(0.26f, 0.59f, 0.98f, 0.35f)
        colors[Col.ModalWindowDarkening]   .put(0.80f, 0.80f, 0.80f, 0.35f)
        colors[Col.DragDropTarget]         .put(1.00f, 1.00f, 0.00f, 0.90f)
        colors[Col.NavHighlight]           .put(0.26f, 0.59f, 0.98f, 1.00f)
        colors[Col.NavWindowingHighlight]  .put(1.00f, 1.00f, 1.00f, 0.70f)
        // @formatter:on
    }

    /** Those light colors are better suited with a thicker font than the default one + FrameBorder */
    fun styleColorsLight(dst: Style? = null) = with(dst ?: g.style) {
        colors.clear()
        for (c in Col.values()) colors += Vec4()
        // @formatter:off
        colors[Col.Text]                   .put(0.00f, 0.00f, 0.00f, 1.00f)
        colors[Col.TextDisabled]           .put(0.60f, 0.60f, 0.60f, 1.00f)
        //colors[Col.TextHovered]          .put(1.00f, 1.00f, 1.00f, 1.00f);
        //colors[Col.TextActive]           .put(1.00f, 1.00f, 0.00f, 1.00f);
        colors[Col.WindowBg]               .put(0.94f, 0.94f, 0.94f, 1.00f)
        colors[Col.ChildBg]                .put(0.00f, 0.00f, 0.00f, 0.00f)
        colors[Col.PopupBg]                .put(1.00f, 1.00f, 1.00f, 0.98f)
        colors[Col.Border]                 .put(0.00f, 0.00f, 0.00f, 0.30f)
        colors[Col.BorderShadow]           .put(0.00f, 0.00f, 0.00f, 0.00f)
        colors[Col.FrameBg]                .put(1.00f, 1.00f, 1.00f, 1.00f)
        colors[Col.FrameBgHovered]         .put(0.26f, 0.59f, 0.98f, 0.40f)
        colors[Col.FrameBgActive]          .put(0.26f, 0.59f, 0.98f, 0.67f)
        colors[Col.TitleBg]                .put(0.96f, 0.96f, 0.96f, 1.00f)
        colors[Col.TitleBgActive]          .put(0.82f, 0.82f, 0.82f, 1.00f)
        colors[Col.TitleBgCollapsed]       .put(1.00f, 1.00f, 1.00f, 0.51f)
        colors[Col.MenuBarBg]              .put(0.86f, 0.86f, 0.86f, 1.00f)
        colors[Col.ScrollbarBg]            .put(0.98f, 0.98f, 0.98f, 0.53f)
        colors[Col.ScrollbarGrab]          .put(0.69f, 0.69f, 0.69f, 0.80f)
        colors[Col.ScrollbarGrabHovered]   .put(0.49f, 0.49f, 0.49f, 0.80f)
        colors[Col.ScrollbarGrabActive]    .put(0.49f, 0.49f, 0.49f, 1.00f)
        colors[Col.CheckMark]              .put(0.26f, 0.59f, 0.98f, 1.00f)
        colors[Col.SliderGrab]             .put(0.26f, 0.59f, 0.98f, 0.78f)
        colors[Col.SliderGrabActive]       .put(0.46f, 0.54f, 0.80f, 0.60f)
        colors[Col.Button]                 .put(0.26f, 0.59f, 0.98f, 0.40f)
        colors[Col.ButtonHovered]          .put(0.26f, 0.59f, 0.98f, 1.00f)
        colors[Col.ButtonActive]           .put(0.06f, 0.53f, 0.98f, 1.00f)
        colors[Col.Header]                 .put(0.26f, 0.59f, 0.98f, 0.31f)
        colors[Col.HeaderHovered]          .put(0.26f, 0.59f, 0.98f, 0.80f)
        colors[Col.HeaderActive]           .put(0.26f, 0.59f, 0.98f, 1.00f)
        colors[Col.Separator]              .put(0.39f, 0.39f, 0.39f, 1.00f)
        colors[Col.SeparatorHovered]       .put(0.14f, 0.44f, 0.80f, 0.78f)
        colors[Col.SeparatorActive]        .put(0.14f, 0.44f, 0.80f, 1.00f)
        colors[Col.ResizeGrip]             .put(0.80f, 0.80f, 0.80f, 0.56f)
        colors[Col.ResizeGripHovered]      .put(0.26f, 0.59f, 0.98f, 0.67f)
        colors[Col.ResizeGripActive]       .put(0.26f, 0.59f, 0.98f, 0.95f)
        colors[Col.CloseButton]            .put(0.59f, 0.59f, 0.59f, 0.50f)
        colors[Col.CloseButtonHovered]     .put(0.98f, 0.39f, 0.36f, 1.00f)
        colors[Col.CloseButtonActive]      .put(0.98f, 0.39f, 0.36f, 1.00f)
        colors[Col.PlotLines]              .put(0.39f, 0.39f, 0.39f, 1.00f)
        colors[Col.PlotLinesHovered]       .put(1.00f, 0.43f, 0.35f, 1.00f)
        colors[Col.PlotHistogram]          .put(0.90f, 0.70f, 0.00f, 1.00f)
        colors[Col.PlotHistogramHovered]   .put(1.00f, 0.45f, 0.00f, 1.00f)
        colors[Col.TextSelectedBg]         .put(0.26f, 0.59f, 0.98f, 0.35f)
        colors[Col.ModalWindowDarkening]   .put(0.20f, 0.20f, 0.20f, 0.35f)
        colors[Col.DragDropTarget]         .put(0.26f, 0.59f, 0.98f, 0.95f)
        colors[Col.NavHighlight]           put colors[Col.HeaderHovered]
        colors[Col.NavWindowingHighlight]  .put(0.70f, 0.70f, 0.70f, 0.70f)
        // @formatter:on
    }
}