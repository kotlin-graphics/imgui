package imgui.api

import glm_.func.deg
import glm_.func.rad
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec3.Vec3
import glm_.vec3.Vec3i
import glm_.vec4.Vec4
import glm_.vec4.Vec4i
import imgui.*
import imgui.ImGui.isClicked
import imgui.ImGui.logSetNextTextDecoration
import imgui.ImGui.setOwner
import imgui.ImGui.slider
import imgui.ImGui.sliderBehavior
import imgui.ImGui.tempInputScalar
import imgui.ImGui.vSlider
import imgui.internal.api.widgetN
import imgui.internal.classes.Rect
import imgui.internal.sections.ActivateFlag
import imgui.internal.sections.IMGUI_TEST_ENGINE_ITEM_INFO
import imgui.internal.sections.ItemFlag
import imgui.internal.sections.ItemStatusFlag
import kool.getValue
import kool.setValue
import kotlin.reflect.KMutableProperty0

// Widgets: Regular Sliders
// - CTRL+Click on any slider to turn them into an input box. Manually input values aren't clamped by default and can go off-bounds. Use ImGuiSliderFlags_AlwaysClamp to always clamp.
// - Adjust format string to decorate the value with a prefix, a suffix, or adapt the editing and display precision e.g. "%.3f" -> 1.234; "%5.2f secs" -> 01.23 secs; "Biscuit: %.0f" -> Biscuit: 1; etc.
// - Format string may also be set to NULL or use the default format ("%f" or "%d").
interface widgetsSliders {
    /** Adjust format to decorate the value with a prefix or a suffix.
     *  "%.3f"         1.234
     *  "%5.2f secs"   01.23 secs
     *  "Gold: %.0f"   Gold: 1
     *  adjust format to decorate the value with a prefix or a suffix for in-slider labels or unit display. Use power!=1.0 for power curve sliders */

    fun slider2(label: String, v: FloatArray, vMin: Float, vMax: Float, format: String = "%.3f", flags: SliderFlags = none): Boolean = sliderN(label, 2, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun slider2(label: String, v: Vec2, vMin: Float, vMax: Float, format: String = "%.3f", flags: SliderFlags = none): Boolean = sliderN(label, 2, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun slider3(label: String, v: FloatArray, vMin: Float, vMax: Float, format: String = "%.3f", flags: SliderFlags = none): Boolean = sliderN(label, 3, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun slider3(label: String, v: Vec3, vMin: Float, vMax: Float, format: String = "%.3f", flags: SliderFlags = none): Boolean = sliderN(label, 3, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun slider4(label: String, v: FloatArray, vMin: Float, vMax: Float, format: String = "%.3f", flags: SliderFlags = none): Boolean = sliderN(label, 4, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun slider4(label: String, v: Vec4, vMin: Float, vMax: Float, format: String = "%.3f", flags: SliderFlags = none): Boolean = sliderN(label, 4, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun sliderAngle(label: String, vRadPtr: KMutableProperty0<Float>, vDegreesMin: Float = -360f, vDegreesMax: Float = 360f, format_: String = "%.0f deg", flags: SliderFlags = none): Boolean {
        val format = format_.ifEmpty { "%.0f deg" }
        var vRad by vRadPtr
        vRad = vRad.deg
        return slider(label, vRadPtr, vDegreesMin, vDegreesMax, format, flags).also { vRad = vRad.rad }
    }

    fun slider2(label: String, v: IntArray, vMin: Int, vMax: Int, format: String = "%d", flags: SliderFlags = none): Boolean = sliderN(label, 2, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun slider2(label: String, v: Vec2i, vMin: Int, vMax: Int, format: String = "%d", flags: SliderFlags = none): Boolean = sliderN(label, 2, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun slider3(label: String, v: IntArray, vMin: Int, vMax: Int, format: String = "%d", flags: SliderFlags = none): Boolean = sliderN(label, 3, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun slider3(label: String, v: Vec3i, vMin: Int, vMax: Int, format: String = "%d", flags: SliderFlags = none): Boolean = sliderN(label, 3, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun slider4(label: String, v: IntArray, vMin: Int, vMax: Int, format: String = "%d", flags: SliderFlags = none): Boolean = sliderN(label, 4, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun slider4(label: String, v: Vec4i, vMin: Int, vMax: Int, format: String = "%d", flags: SliderFlags = none): Boolean = sliderN(label, 4, vMin, vMax, format, flags, v::mutablePropertyAt)

    /** Adjust format to decorate the value with a prefix or a suffix.
     *  "%.3f"         1.234
     *  "%5.2f secs"   01.23 secs
     *  "Gold: %.0f"   Gold: 1
     *  adjust format to decorate the value with a prefix or a suffix for in-slider labels or unit display. Use power!=1.0 for power curve sliders
     *
     *  Note: p_data, p_min and p_max are _pointers_ to a memory address holding the data. For a slider, they are all required.
     *  Read code of e.g. SliderFloat(), SliderInt() etc. or examples in 'Demo->Widgets->Data Types' to understand how to use this function directly. */
    fun <N> NumberOps<N>.slider(label: String, pData: KMutableProperty0<N>, min: N, max: N, format_: String? = null, flags: SliderFlags = none): Boolean where N : Number, N : Comparable<N> {

        val window = ImGui.currentWindow
        if (window.skipItems) return false

        val id = window.getID(label)
        val w = ImGui.calcItemWidth()

        val labelSize = ImGui.calcTextSize(label, hideTextAfterDoubleHash = true)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w, labelSize.y + ImGui.style.framePadding.y * 2f))
        val totalBb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) ImGui.style.itemInnerSpacing.x + labelSize.x else 0f, 0f))

        val tempInputAllowed = flags hasnt SliderFlag.NoInput
        ImGui.itemSize(totalBb, ImGui.style.framePadding.y)
        if (!ImGui.itemAdd(totalBb, id, frameBb, if (tempInputAllowed) ItemFlag.Inputable else none)) return false

        // Default format string when passing NULL
        val format = format_ ?: defaultFormat

        val hovered = ImGui.itemHoverable(frameBb, id)
        var tempInputIsActive = tempInputAllowed && ImGui.tempInputIsActive(id)
        if (!tempInputIsActive) {

            // Tabbing or CTRL-clicking on Slider turns it into an input box
            val inputRequestedByTabbing = tempInputAllowed && g.lastItemData.statusFlags has ItemStatusFlag.FocusedByTabbing
            val clicked = hovered && MouseButton.Left.isClicked(id)
            val makeActive = inputRequestedByTabbing || clicked || g.navActivateId == id
            if (makeActive && clicked)
                Key.MouseLeft.setOwner(id)
            if (makeActive && tempInputAllowed)
                if (inputRequestedByTabbing || (clicked && g.io.keyCtrl) || (g.navActivateId == id && g.navActivateFlags has ActivateFlag.PreferInput))
                    tempInputIsActive = true

            if (makeActive && !tempInputIsActive) {
                ImGui.setActiveID(id, window)
                ImGui.setFocusID(id, window)
                ImGui.focusWindow(window)
                g.activeIdUsingNavDirMask = g.activeIdUsingNavDirMask or ((1 shl Dir.Left) or (1 shl Dir.Right))
            }
        }

        if (tempInputIsActive) {
            // Only clamp CTRL+Click input when ImGuiSliderFlags_AlwaysClamp is set
            val isClampInput = flags has SliderFlag.AlwaysClamp
            return tempInputScalar(frameBb, id, label, pData, format, min.takeIf { isClampInput }, max.takeIf { isClampInput })
        }

        // Draw frame
        val frameCol = when {
            g.activeId == id -> Col.FrameBgActive
            hovered -> Col.FrameBgHovered
            else -> Col.FrameBg
        }
        ImGui.renderNavHighlight(frameBb, id)
        ImGui.renderFrame(frameBb.min, frameBb.max, frameCol.u32, true, ImGui.style.frameRounding)

        // Slider behavior
        val grabBb = Rect()
        val valueChanged = sliderBehavior(frameBb, id, pData, min, max, format, flags, grabBb)
        if (valueChanged) ImGui.markItemEdited(id)

        // Render grab
        if (grabBb.max.x > grabBb.min.x) {
            val col = if (g.activeId == id) Col.SliderGrabActive else Col.SliderGrab
            window.drawList.addRectFilled(grabBb.min, grabBb.max, col.u32, ImGui.style.grabRounding)
        }

        // Display value using user-provided display format so user can add prefix/suffix/decorations to the value.
        val value = pData().format(format)
        if (g.logEnabled) logSetNextTextDecoration("{", "}")
        ImGui.renderTextClipped(frameBb.min, frameBb.max, value, null, Vec2(0.5f))

        if (labelSize.x > 0f) {
            val pos = Vec2(frameBb.max.x + ImGui.style.itemInnerSpacing.x, frameBb.min.y + ImGui.style.framePadding.y)
            ImGui.renderText(pos, label)
        }

        IMGUI_TEST_ENGINE_ITEM_INFO(id, label, g.lastItemData.statusFlags / if (tempInputAllowed) ItemStatusFlag.Inputable else none)
        return valueChanged
    }

    /** Internal implementation */
    fun <N> NumberOps<N>.vSlider(label: String, size: Vec2, pData: KMutableProperty0<N>, min: N, max: N, format_: String? = null, flags: SliderFlags = none): Boolean where N : Number, N : Comparable<N> {

        val window = ImGui.currentWindow
        if (window.skipItems) return false

        val id = window.getID(label)

        val labelSize = ImGui.calcTextSize(label, hideTextAfterDoubleHash = true)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        val bb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) ImGui.style.itemInnerSpacing.x + labelSize.x else 0f, 0f))

        ImGui.itemSize(bb, ImGui.style.framePadding.y)
        if (!ImGui.itemAdd(frameBb, id)) return false

        // Default format string when passing NULL
        val format = format_ ?: defaultFormat

        val hovered = ImGui.itemHoverable(frameBb, id)
        val clicked = hovered && MouseButton.Left.isClicked(id)
        if (clicked || g.navActivateId == id) {
            if (clicked)
                Key.MouseLeft.setOwner(id)
            ImGui.setActiveID(id, window)
            ImGui.setFocusID(id, window)
            ImGui.focusWindow(window)
            g.activeIdUsingNavDirMask = g.activeIdUsingNavDirMask or ((1 shl Dir.Up) or (1 shl Dir.Down))
        }

        // Draw frame
        val frameCol = when {
            g.activeId == id -> Col.FrameBgActive
            hovered -> Col.FrameBgHovered
            else -> Col.FrameBg
        }
        ImGui.renderNavHighlight(frameBb, id)
        ImGui.renderFrame(frameBb.min, frameBb.max, frameCol.u32, true, ImGui.style.frameRounding)
        // Slider behavior
        val grabBb = Rect()
        val valueChanged = sliderBehavior(frameBb, id, pData, min, max, format, flags or SliderFlag._Vertical, grabBb)

        if (valueChanged) ImGui.markItemEdited(id)

        // Render grab
        if (grabBb.max.y > grabBb.min.y) window.drawList.addRectFilled(grabBb.min, grabBb.max, ImGui.getColorU32(if (g.activeId == id) Col.SliderGrabActive else Col.SliderGrab), ImGui.style.grabRounding)

        /*  Display value using user-provided display format so user can add prefix/suffix/decorations to the value.
            For the vertical slider we allow centered text to overlap the frame padding         */
        val value = pData().format(format)
        val posMin = Vec2(frameBb.min.x, frameBb.min.y + ImGui.style.framePadding.y)
        ImGui.renderTextClipped(posMin, frameBb.max, value, null, Vec2(0.5f, 0f))
        if (labelSize.x > 0f) ImGui.renderText(Vec2(frameBb.max.x + ImGui.style.itemInnerSpacing.x, frameBb.min.y + ImGui.style.framePadding.y), label)

        return valueChanged
    }

}

inline fun <reified N> slider(label: String, pData: KMutableProperty0<N>, min: N, max: N, format_: String? = null, flags: SliderFlags = none): Boolean where N : Number, N : Comparable<N> =
        ImGui.slider(label, pData, min, max, format_, flags)

inline fun <reified N> ImGui.slider(label: String, pData: KMutableProperty0<N>, min: N, max: N, format_: String? = null, flags: SliderFlags = none): Boolean where N : Number, N : Comparable<N> =
        numberOps<N>().slider(label, pData, min, max, format_, flags)

/** Add multiple sliders on 1 line for compact edition of multiple components */
inline fun <reified N> sliderN(label: String, components: Int, min: N, max: N, format: String? = null, flags: SliderFlags = none, properties: (Int) -> KMutableProperty0<N>): Boolean where N : Number, N : Comparable<N> =
        ImGui.sliderN(label, components, min, max, format, flags, properties)

/** Add multiple sliders on 1 line for compact edition of multiple components */
inline fun <reified N> ImGui.sliderN(label: String, components: Int, min: N, max: N, format: String? = null, flags: SliderFlags = none, properties: (Int) -> KMutableProperty0<N>): Boolean where N : Number, N : Comparable<N> =
        numberOps<N>().sliderN(label, components, min, max, format, flags, properties)

inline fun <N> NumberOps<N>.sliderN(label: String, components: Int, min: N, max: N, format: String? = null, flags: SliderFlags = none, properties: (Int) -> KMutableProperty0<N>): Boolean where N : Number, N : Comparable<N> =
        widgetN(label, components) { i ->
            slider("", properties(i), min, max, format, flags)
        }

inline fun <reified N> vSlider(label: String, size: Vec2, pData: KMutableProperty0<N>, min: N, max: N, format_: String? = null, flags: SliderFlags = none): Boolean where N : Number, N : Comparable<N> =
        ImGui.vSlider(label, size, pData, min, max, format_, flags)

inline fun <reified N> ImGui.vSlider(label: String, size: Vec2, pData: KMutableProperty0<N>, min: N, max: N, format_: String? = null, flags: SliderFlags = none): Boolean where N : Number, N : Comparable<N> =
        numberOps<N>().vSlider(label, size, pData, min, max, format_, flags)