package imgui.api

import glm_.func.common.max
import glm_.func.common.min
import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec3.Vec3
import glm_.vec3.Vec3i
import glm_.vec4.Vec4
import glm_.vec4.Vec4i
import imgui.*
import imgui.ImGui.beginGroup
import imgui.ImGui.calcItemWidth
import imgui.ImGui.currentWindow
import imgui.ImGui.drag
import imgui.ImGui.dragBehavior
import imgui.ImGui.endGroup
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.focusWindow
import imgui.ImGui.io
import imgui.ImGui.isClicked
import imgui.ImGui.isMouseDragPastThreshold
import imgui.ImGui.logSetNextTextDecoration
import imgui.ImGui.popID
import imgui.ImGui.popItemWidth
import imgui.ImGui.pushID
import imgui.ImGui.pushMultiItemsWidths
import imgui.ImGui.sameLine
import imgui.ImGui.setActiveID
import imgui.ImGui.setFocusID
import imgui.ImGui.setOwner
import imgui.ImGui.style
import imgui.ImGui.tempInputScalar
import imgui.ImGui.testOwner
import imgui.ImGui.textEx
import imgui.internal.api.widgetN
import imgui.internal.classes.Rect
import imgui.internal.sections.*
import imgui.static.DRAG_MOUSE_THRESHOLD_FACTOR
import uno.kotlin.getValue
import kotlin.reflect.KMutableProperty0

// Widgets: Drag Sliders
// - CTRL+Click on any drag box to turn them into an input box. Manually input values aren't clamped by default and can go off-bounds. Use ImGuiSliderFlags_AlwaysClamp to always clamp.
// - For all the Float2/Float3/Float4/Int2/Int3/Int4 versions of every function, note that a 'float v[X]' function argument is the same as 'float* v',
//   the array syntax is just a way to document the number of elements that are expected to be accessible. You can pass address of your first element out of a contiguous set, e.g. &myvector.x
// - Adjust format string to decorate the value with a prefix, a suffix, or adapt the editing and display precision e.g. "%.3f" -> 1.234; "%5.2f secs" -> 01.23 secs; "Biscuit: %.0f" -> Biscuit: 1; etc.
// - Format string may also be set to NULL or use the default format ("%f" or "%d").
// - Speed are per-pixel of mouse movement (v_speed=0.2f: mouse needs to move by 5 pixels to increase value by 1). For gamepad/keyboard navigation, minimum speed is Max(v_speed, minimum_step_at_given_precision).
// - Use v_min < v_max to clamp edits to given limits. Note that CTRL+Click manual input can override those limits if ImGuiSliderFlags_AlwaysClamp is not used.
// - Use v_max = FLT_MAX / INT_MAX etc to avoid clamping to a maximum, same with v_min = -FLT_MAX / INT_MIN to avoid clamping to a minimum.
// - We use the same sets of flags for DragXXX() and SliderXXX() functions as the features are the same and it makes it easier to swap them.
// - Legacy: Pre-1.78 there are DragXXX() function signatures that take a final `float power=1.0f' argument instead of the `ImGuiSliderFlags flags=0' argument.
//   If you get a warning converting a float to ImGuiSliderFlags, read https://github.com/ocornut/imgui/issues/3361
interface widgetsDrags {


    /** If v_min >= v_max we have no bound */
    fun drag2(label: String,
              v: FloatArray,
              vSpeed: Float = 1f,
              vMin: Float = 0f,
              vMax: Float = 0f,
              format: String = "%.3f",
              flags: SliderFlags = emptyFlags): Boolean = dragN(label, 2, vSpeed, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun drag2(label: String,
              v: Vec2,
              vSpeed: Float = 1f,
              vMin: Float = 0f,
              vMax: Float = 0f,
              format: String = "%.3f",
              flags: SliderFlags = emptyFlags): Boolean = dragN(label, 2, vSpeed, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun drag3(label: String,
              v: FloatArray,
              vSpeed: Float = 1f,
              vMin: Float = 0f,
              vMax: Float = 0f,
              format: String = "%.3f",
              flags: SliderFlags = emptyFlags): Boolean = dragN(label, 3, vSpeed, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun drag3(label: String,
              v: Vec3,
              vSpeed: Float = 1f,
              vMin: Float = 0f,
              vMax: Float = 0f,
              format: String = "%.3f",
              flags: SliderFlags = emptyFlags): Boolean = dragN(label, 3, vSpeed, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun drag4(label: String,
              v: FloatArray,
              vSpeed: Float = 1f,
              vMin: Float = 0f,
              vMax: Float = 0f,
              format: String = "%.3f",
              flags: SliderFlags = emptyFlags): Boolean = dragN(label, 4, vSpeed, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun drag4(label: String,
              v: Vec4,
              vSpeed: Float = 1f,
              vMin: Float = 0f,
              vMax: Float = 0f,
              format: String = "%.3f",
              flags: SliderFlags = emptyFlags): Boolean = dragN(label, 4, vSpeed, vMin, vMax, format, flags, v::mutablePropertyAt)

    /** NB: You likely want to specify the ImGuiSliderFlags_AlwaysClamp when using this. */
    fun dragRange(label: String,
                  vCurrentMinPtr: KMutableProperty0<Float>,
                  vCurrentMaxPtr: KMutableProperty0<Float>,
                  vSpeed: Float = 1f,
                  vMin: Float = 0f,
                  vMax: Float = 0f,
                  format: String = "%.3f",
                  formatMax: String = format,
                  flags: SliderFlags = emptyFlags): Boolean {

        val vCurrentMin by vCurrentMinPtr
        val vCurrentMax by vCurrentMaxPtr
        val window = ImGui.currentWindow
        if (window.skipItems) return false

        ImGui.pushID(label)
        ImGui.beginGroup()
        ImGui.pushMultiItemsWidths(2, ImGui.calcItemWidth())

        val minMin = if (vMin >= vMax) -Float.MAX_VALUE else vMin
        val minMax = if (vMin >= vMax) vCurrentMax else vMax min vCurrentMax
        val minFlags = flags or if (minMin == minMax) SliderFlag._ReadOnly else emptyFlags
        var valueChanged = drag("##min", vCurrentMinPtr, vSpeed, minMin, minMax, format, minFlags)
        ImGui.popItemWidth()
        ImGui.sameLine(0f, ImGui.style.itemInnerSpacing.x)

        val maxMin = if (vMin >= vMax) vCurrentMin else vMin max vCurrentMin
        val maxMax = if (vMin >= vMax) Float.MAX_VALUE else vMax
        val maxFlags = flags or if (maxMin == maxMax) SliderFlag._ReadOnly else emptyFlags
        val fmt = formatMax.ifEmpty { format }
        valueChanged /= drag("##max", vCurrentMaxPtr, vSpeed, maxMin, maxMax, fmt, maxFlags)
        ImGui.popItemWidth()
        ImGui.sameLine(0f, ImGui.style.itemInnerSpacing.x)

        ImGui.textEx(label, ImGui.findRenderedTextEnd(label))
        ImGui.endGroup()
        ImGui.popID()

        return valueChanged
    }

    /** If v_min >= v_max we have no bound
     *
     *  NB: vSpeed is float to allow adjusting the drag speed with more precision     */
    fun drag2(label: String,
              v: IntArray,
              vSpeed: Float = 1f,
              vMin: Int = 0,
              vMax: Int = 0,
              format: String = "%d",
              flags: SliderFlags = emptyFlags): Boolean = dragN(label, 2, vSpeed, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun drag2(label: String,
              v: Vec2i,
              vSpeed: Float = 1f,
              vMin: Int = 0,
              vMax: Int = 0,
              format: String = "%d",
              flags: SliderFlags = emptyFlags): Boolean = dragN(label, 2, vSpeed, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun drag3(label: String,
              v: IntArray,
              vSpeed: Float = 1f,
              vMin: Int = 0,
              vMax: Int = 0,
              format: String = "%d",
              flags: SliderFlags = emptyFlags): Boolean = dragN(label, 3, vSpeed, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun drag3(label: String,
              v: Vec3i,
              vSpeed: Float = 1f,
              vMin: Int = 0,
              vMax: Int = 0,
              format: String = "%d",
              flags: SliderFlags = emptyFlags): Boolean = dragN(label, 3, vSpeed, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun drag4(label: String,
              v: IntArray,
              vSpeed: Float = 1f,
              vMin: Int = 0,
              vMax: Int = 0,
              format: String = "%d",
              flags: SliderFlags = emptyFlags): Boolean = dragN(label, 4, vSpeed, vMin, vMax, format, flags, v::mutablePropertyAt)

    fun drag4(label: String,
              v: Vec4i,
              vSpeed: Float = 1f,
              vMin: Int = 0,
              vMax: Int = 0,
              format: String = "%d",
              flags: SliderFlags = emptyFlags): Boolean = dragN(label, 4, vSpeed, vMin, vMax, format, flags, v::mutablePropertyAt)

    /** NB: You likely want to specify the ImGuiSliderFlags_AlwaysClamp when using this. */
    fun dragRange(label: String,
                  vCurrentMinPtr: KMutableProperty0<Int>,
                  vCurrentMaxPtr: KMutableProperty0<Int>,
                  vSpeed: Float = 1f,
                  vMin: Int = 0,
                  vMax: Int = 0,
                  format: String = "%d",
                  formatMax: String = format,
                  flags: SliderFlags = emptyFlags): Boolean {

        val vCurrentMin by vCurrentMinPtr
        val vCurrentMax by vCurrentMaxPtr
        val window = currentWindow
        if (window.skipItems) return false

        pushID(label)
        beginGroup()
        pushMultiItemsWidths(2, calcItemWidth())

        val minMin = if (vMin >= vMax) Int.MIN_VALUE else vMin
        val minMax = if (vMin >= vMax) vCurrentMax else vMax min vCurrentMax
        val minFlags = flags or if (minMin == minMax) SliderFlag._ReadOnly else emptyFlags
        var valueChanged = drag("##min", vCurrentMinPtr, vSpeed, minMin, minMax, format, minFlags)
        popItemWidth()
        sameLine(0f, style.itemInnerSpacing.x)

        val maxMin = if (vMin >= vMax) vCurrentMin else vMin max vCurrentMin
        val maxMax = if (vMin >= vMax) Int.MAX_VALUE else vMax
        val maxFlags = flags or if (maxMin == maxMax) SliderFlag._ReadOnly else emptyFlags
        val fmt = formatMax.ifEmpty { format }
        valueChanged /= drag("##max", vCurrentMaxPtr, vSpeed, maxMin, maxMax, fmt, maxFlags)
        popItemWidth()
        sameLine(0f, style.itemInnerSpacing.x)

        textEx(label, findRenderedTextEnd(label))
        endGroup()
        popID()
        return valueChanged
    }

    /** For all the Float2/Float3/Float4/Int2/Int3/Int4 versions of every function, note that a 'float v[X]' function
     *  argument is the same as 'float* v', the array syntax is just a way to document the number of elements that are
     *  expected to be accessible. You can pass address of your first element out of a contiguous set, e.g. &myvector.x
     *  Adjust format string to decorate the value with a prefix, a suffix, or adapt the editing and display precision
     *  e.g. "%.3f" -> 1.234; "%5.2f secs" -> 01.23 secs; "Biscuit: %.0f" -> Biscuit: 1; etc.
     *  Speed are per-pixel of mouse movement (vSpeed = 0.2f: mouse needs to move by 5 pixels to increase value by 1).
     *  For gamepad/keyboard navigation, minimum speed is Max(vSpeed, minimumStepAtGivenPrecision). */
    fun drag(label: String,
             pData: FloatArray,
             vSpeed: Float = 1f,
             min: Float = 0f,
             max: Float = 0f,
             format: String = "%.3f",
             flags: SliderFlags = emptyFlags): Boolean = drag(label, pData mutablePropertyAt 0, vSpeed, min, max, format, flags)

    /** ote: p_data, p_min and p_max are _pointers_ to a memory address holding the data. For a Drag widget, p_min and p_max are optional.
     *  Read code of e.g. DragFloat(), DragInt() etc. or examples in 'Demo->Widgets->Data Types' to understand how to use this function directly. */
    fun <N> NumberOps<N>.drag(label: String,
                              pData: KMutableProperty0<N>,
                              vSpeed: Float = 1f,
                              min: N? = null,
                              max: N? = null,
                              format_: String? = null,
                              flags: SliderFlags = emptyFlags): Boolean where N : Number, N : Comparable<N> {

        val window = ImGui.currentWindow
        if (window.skipItems) return false

        val id = window.getID(label)
        val w = ImGui.calcItemWidth()

        val labelSize = ImGui.calcTextSize(label, hideTextAfterDoubleHash = true)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w, labelSize.y + style.framePadding.y * 2f))
        val totalBb = Rect(
                frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) style.itemInnerSpacing.x + labelSize.x else 0f, 0f)
        )

        val tempInputAllowed = flags hasnt SliderFlag.NoInput
        ImGui.itemSize(totalBb, ImGui.style.framePadding.y)
        if (!ImGui.itemAdd(totalBb, id, frameBb, if (tempInputAllowed) ItemFlag.Inputable else emptyFlags))
            return false

        // Default format string when passing NULL
        val format = format_ ?: defaultFormat

        val hovered = ImGui.itemHoverable(frameBb, id)
        var tempInputIsActive = tempInputAllowed && ImGui.tempInputIsActive(id)
        if (!tempInputIsActive) {

            // Tabbing or CTRL-clicking on Drag turns it into an InputText
            val inputRequestedByTabbing = tempInputAllowed && g.lastItemData.statusFlags has ItemStatusFlag.FocusedByTabbing
            val clicked = hovered && MouseButton.Left.isClicked(id)
            val doubleClicked = hovered && g.io.mouseClickedCount[0] == 2 && Key.MouseLeft testOwner id
            val makeActive = inputRequestedByTabbing || clicked || doubleClicked || g.navActivateId == id || g.navActivateInputId == id
            if (makeActive && (clicked || doubleClicked)) Key.MouseLeft.setOwner(id)
            if (makeActive && tempInputAllowed)
                if (inputRequestedByTabbing || (clicked && ImGui.io.keyCtrl) || doubleClicked || g.navActivateInputId == id)
                    tempInputIsActive = true

            // (Optional) simple click (without moving) turns Drag into an InputText
            if (io.configDragClickToInputText && tempInputAllowed && !tempInputIsActive)
                if (g.activeId == id && hovered && io.mouseReleased[0] && !isMouseDragPastThreshold(MouseButton.Left, io.mouseDragThreshold * DRAG_MOUSE_THRESHOLD_FACTOR)) {
                    g.navActivateId = id; g.navActivateInputId = id
                    tempInputIsActive = true
                }

            if (makeActive && !tempInputIsActive) {
                setActiveID(id, window)
                setFocusID(id, window)
                focusWindow(window)
                g.activeIdUsingNavDirMask = (1 shl Dir.Left) or (1 shl Dir.Right)
            }
        }

        if (tempInputIsActive) {
            // Only clamp CTRL+Click input when ImGuiSliderFlags_AlwaysClamp is set
            val isClampInput = flags has SliderFlag.AlwaysClamp && (min == null || max == null || min < max)
            return tempInputScalar(frameBb,
                    id,
                    label,
                    pData,
                    format,
                    min.takeIf { isClampInput },
                    max.takeIf { isClampInput })
        }

        // Draw frame
        val frameCol = when {
            g.activeId == id -> Col.FrameBgActive
            hovered -> Col.FrameBgHovered
            else -> Col.FrameBg
        }
        ImGui.renderNavHighlight(frameBb, id)
        ImGui.renderFrame(frameBb.min, frameBb.max, frameCol.u32, true, ImGui.style.frameRounding)

        // Drag behavior
        val valueChanged = dragBehavior(id, pData, vSpeed, min, max, format, flags)
        if (valueChanged) ImGui.markItemEdited(id)

        // Display value using user-provided display format so user can add prefix/suffix/decorations to the value.
        val value = pData().format(format)
        if (g.logEnabled) logSetNextTextDecoration("{", "}")
        ImGui.renderTextClipped(frameBb.min, frameBb.max, value, null, Vec2(0.5f))

        if (labelSize.x > 0f)
            ImGui.renderText(Vec2(frameBb.max.x + ImGui.style.itemInnerSpacing.x, frameBb.min.y + ImGui.style.framePadding.y), label)

        IMGUI_TEST_ENGINE_ITEM_INFO(id, label, g.lastItemData.statusFlags)
        return valueChanged
    }
}

inline fun <reified N> drag(label: String,
                            pData: KMutableProperty0<N>,
                            vSpeed: Float = 1f,
                            min: N? = null,
                            max: N? = null,
                            format_: String? = null,
                            flags: SliderFlags = emptyFlags)
        : Boolean where N : Number, N : Comparable<N> = numberOps<N>().drag(label, pData, vSpeed, min, max, format_, flags)

/** Note: p_data, p_min and p_max are _pointers_ to a memory address holding the data. For a Drag widget,
 *  p_min and p_max are optional.
 *  Read code of e.g. SliderFloat(), SliderInt() etc. or examples in 'Demo->Widgets->Data Types' to understand
 *  how to use this function directly. */
inline fun <reified N> dragN(label: String,
                             components: Int,
                             vSpeed: Float = 1f,
                             min: N? = null,
                             max: N? = null,
                             format: String? = null,
                             flags: SliderFlags = emptyFlags,
                             properties: (Int) -> KMutableProperty0<N>)
        : Boolean where N : Number, N : Comparable<N> =
        numberOps<N>().dragN(label, components, vSpeed, min, max, format, flags, properties)

inline fun <N> NumberOps<N>.dragN(label: String,
                                  components: Int,
                                  vSpeed: Float = 1f,
                                  min: N? = null,
                                  max: N? = null,
                                  format: String? = null,
                                  flags: SliderFlags = emptyFlags,
                                  properties: (Int) -> KMutableProperty0<N>)
        : Boolean where N : Number, N : Comparable<N> =
        widgetN(label, components) { i -> drag("", properties(i), vSpeed, min, max, format, flags) }
