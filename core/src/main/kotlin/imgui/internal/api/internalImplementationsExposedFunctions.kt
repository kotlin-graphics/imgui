package imgui.internal.api

import glm_.func.common.max
import glm_.func.common.min
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.format
import imgui.ImGui.sliderBehavior
import imgui.api.g
import imgui.internal.classes.Rect
import imgui.static.patchFormatStringFloatToInt
import uno.kotlin.getValue
import unsigned.Ubyte
import unsigned.Uint
import unsigned.Ulong
import unsigned.Ushort
import kotlin.reflect.KMutableProperty0

// Internal implementations for some of the exposed functions (use the non-internal versions instead)
interface internalImplementationsExposedFunctions {

    /** For all the Float2/Float3/Float4/Int2/Int3/Int4 versions of every functions, note that a 'float v[X]' function
     *  argument is the same as 'float* v', the array syntax is just a way to document the number of elements that are
     *  expected to be accessible. You can pass address of your first element out of a contiguous set, e.g. &myvector.x
     *  Adjust format string to decorate the value with a prefix, a suffix, or adapt the editing and display precision
     *  e.g. "%.3f" -> 1.234; "%5.2f secs" -> 01.23 secs; "Biscuit: %.0f" -> Biscuit: 1; etc.
     *  Speed are per-pixel of mouse movement (vSpeed = 0.2f: mouse needs to move by 5 pixels to increase value by 1).
     *  For gamepad/keyboard navigation, minimum speed is Max(vSpeed, minimumStepAtGivenPrecision). */
    fun dragScalarInternal(label: String, pData: FloatArray, vSpeed: Float,
                           pMin: Float? = null, pMax: Float? = null, format: String? = null,
                           power: Float = 1f, flags: DragFlags = 0): Boolean =
            dragScalarInternal(label, pData, 0, vSpeed, pMin, pMax, format, power, flags)

    /** If vMin >= vMax we have no bound  */
    fun dragScalarInternal(label: String, pData: FloatArray, ptr: Int = 0, vSpeed: Float,
                           pMin: Float? = null, pMax: Float? = null, format: String? = null,
                           power: Float, flags: DragFlags = 0): Boolean =
            withFloat(pData, ptr) {
                dragScalarInternal(label, DataType.Float, it, vSpeed, pMin, pMax, format, power, flags)
            }

    /** Internal implementation - see below for entry points */
    fun <N> dragScalarInternal(label: String, dataType: DataType, pData: KMutableProperty0<N>, vSpeed: Float,
                               pMin: N? = null, pMax: N? = null, format_: String? = null,
                               power: Float, flags: DragFlags = 0): Boolean
            where N : Number, N : Comparable<N> {

        val window = ImGui.currentWindow
        if (window.skipItems) return false

        if (power != 1f)
            assert(pMin != null && pMax != null) { "When using a power curve the drag needs to have known bounds" }

        val id = window.getID(label)
        val w = ImGui.calcItemWidth()
        val labelSize = ImGui.calcTextSize(label, hideTextAfterDoubleHash = true)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w, labelSize.y + ImGui.style.framePadding.y * 2f))
        val totalBb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) ImGui.style.itemInnerSpacing.x + labelSize.x else 0f, 0f))

        ImGui.itemSize(totalBb, ImGui.style.framePadding.y)
        if (!ImGui.itemAdd(totalBb, id, frameBb))
            return false

        // Default format string when passing NULL
        val format = when {
            format_ == null -> when (dataType) {
                DataType.Float, DataType.Double -> "%f"
                else -> "%d" // (FIXME-LEGACY: Patch old "%.0f" format string to use "%d", read function more details.)
            }
            dataType == DataType.Int && format_ != "%d" -> patchFormatStringFloatToInt(format_)
            else -> format_
        }

        // Tabbing or CTRL-clicking on Drag turns it into an input box
        val hovered = ImGui.itemHoverable(frameBb, id)
        var tempInputIsActive = ImGui.tempInputIsActive(id)
        if (!tempInputIsActive) {
            val focusRequested = ImGui.focusableItemRegister(window, id)
            val clicked = hovered && ImGui.io.mouseClicked[0]
            val doubleClicked = hovered && ImGui.io.mouseDoubleClicked[0]
            if (focusRequested || clicked || doubleClicked || g.navActivateId == id || g.navInputId == id) {
                ImGui.setActiveID(id, window)
                ImGui.setFocusID(id, window)
                ImGui.focusWindow(window)
                g.activeIdUsingNavDirMask = (1 shl Dir.Left) or (1 shl Dir.Right)
                if (focusRequested || (clicked && ImGui.io.keyCtrl) || doubleClicked || g.navInputId == id) {
                    tempInputIsActive = true
                    ImGui.focusableItemUnregister(window)
                }
            }
        }

        // Our current specs do NOT clamp when using CTRL+Click manual input, but we should eventually add a flag for that..
        if (tempInputIsActive)
            return ImGui.tempInputScalar(frameBb, id, label, dataType, pData, format) // , p_min, p_max)

        // Draw frame
        val frameCol = if (g.activeId == id) Col.FrameBgActive else if (g.hoveredId == id) Col.FrameBgHovered else Col.FrameBg
        ImGui.renderNavHighlight(frameBb, id)
        ImGui.renderFrame(frameBb.min, frameBb.max, frameCol.u32, true, ImGui.style.frameRounding)

        // Drag behavior
        val valueChanged = ImGui.dragBehavior(id, dataType, pData, vSpeed, pMin, pMax, format, power, flags)
        if (valueChanged)
            ImGui.markItemEdited(id)

        // Display value using user-provided display format so user can add prefix/suffix/decorations to the value.
        val value = pData.format(dataType, format)
        ImGui.renderTextClipped(frameBb.min, frameBb.max, value, null, Vec2(0.5f))

        if (labelSize.x > 0f)
            ImGui.renderText(Vec2(frameBb.max.x + ImGui.style.itemInnerSpacing.x, frameBb.min.y + ImGui.style.framePadding.y), label)

        Hook.itemInfo?.invoke(g, id, label, window.dc.itemFlags)
        return valueChanged
    }

    /** Internal implementation - see below for entry points */
    fun <N> dragScalarNInternal(label: String, dataType: DataType, v: Any, components: Int,
                                vSpeed: Float, vMin: N? = null, vMax: N? = null, format: String? = null,
                                power: Float = 1f, flags: DragFlags = 0): Boolean
            where N : Number, N : Comparable<N> {

        val window = ImGui.currentWindow
        if (window.skipItems) return false

        var valueChanged = false
        ImGui.beginGroup()
        ImGui.pushID(label)
        ImGui.pushMultiItemsWidths(components, ImGui.calcItemWidth())
        for (i in 0 until components) {
            ImGui.pushID(i)
            if (i > 0)
                ImGui.sameLine(0f, ImGui.style.itemInnerSpacing.x)
            when (dataType) {
                DataType.Int -> withInt(v as IntArray, i) {
                    valueChanged = dragScalarInternal("", dataType, it as KMutableProperty0<N>, vSpeed, vMin, vMax, format, power, flags) or valueChanged
                }
                DataType.Float -> withFloat(v as FloatArray, i) {
                    valueChanged = dragScalarInternal("", dataType, it as KMutableProperty0<N>, vSpeed, vMin, vMax, format, power, flags) or valueChanged
                }
                else -> error("invalid")
            }
            ImGui.popID()
            ImGui.popItemWidth()
        }
        ImGui.popID()

        val labelEnd = ImGui.findRenderedTextEnd(label)
        if (0 != labelEnd) {
            ImGui.sameLine(0f, ImGui.style.itemInnerSpacing.x)
            ImGui.textEx(label, labelEnd)
        }

        ImGui.endGroup()
        return valueChanged
    }

    /** Internal implementation */
    fun dragFloatRange2Internal(label: String, vCurrentMinPtr: KMutableProperty0<Float>, vCurrentMaxPtr: KMutableProperty0<Float>,
                                vSpeed: Float = 1f, vMin: Float = 0f, vMax: Float = 0f, format: String = "%.3f",
                                formatMax: String = format, power: Float = 1f, flags: DragFlags = 0): Boolean {

        val vCurrentMin by vCurrentMinPtr
        val vCurrentMax by vCurrentMaxPtr
        val window = ImGui.currentWindow
        if (window.skipItems) return false

        ImGui.pushID(label)
        ImGui.beginGroup()
        ImGui.pushMultiItemsWidths(2, ImGui.calcItemWidth())

        var min = if (vMin >= vMax) -Float.MAX_VALUE else vMin
        var max = if (vMin >= vMax) vCurrentMax else vMax min vCurrentMax
        if (min == max) {
            min = Float.MAX_VALUE; max = -Float.MAX_VALUE; } // Lock edit
        var valueChanged = dragScalarInternal("##min", DataType.Float, vCurrentMinPtr, vSpeed, min, max, format, power, flags)
        ImGui.popItemWidth()
        ImGui.sameLine(0f, ImGui.style.itemInnerSpacing.x)

        min = if (vMin >= vMax) vCurrentMin else vMin max vCurrentMin
        max = if (vMin >= vMax) Float.MAX_VALUE else vMax
        if (min == max) {
            min = Float.MAX_VALUE; max = -Float.MAX_VALUE; } // Lock edit
        val f = if (formatMax.isNotEmpty()) formatMax else format
        valueChanged = dragScalarInternal("##max", DataType.Float, vCurrentMaxPtr, vSpeed, min, max, f, power, flags) || valueChanged
        ImGui.popItemWidth()
        ImGui.sameLine(0f, ImGui.style.itemInnerSpacing.x)

        ImGui.textEx(label, ImGui.findRenderedTextEnd(label))
        ImGui.endGroup()
        ImGui.popID()
        return valueChanged
    }

    /** Adjust format to decorate the value with a prefix or a suffix.
     *  "%.3f"         1.234
     *  "%5.2f secs"   01.23 secs
     *  "Gold: %.0f"   Gold: 1
     *  Use power != 1.0f for non-linear sliders.
     *  adjust format to decorate the value with a prefix or a suffix for in-slider labels or unit display. Use power!=1.0 for power curve sliders
     *
     *  Internal implementation */
    fun <N> sliderScalarInternal(label: String, dataType: DataType, pData: KMutableProperty0<N>, pMin: N? = null, pMax: N? = null,
                                 format_: String? = null, power: Float = 1f, flags: SliderFlags = 0): Boolean
            where N : Number, N : Comparable<N> {

        val window = ImGui.currentWindow
        if (window.skipItems) return false

        val id = window.getID(label)
        val w = ImGui.calcItemWidth()

        val labelSize = ImGui.calcTextSize(label, hideTextAfterDoubleHash = true)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + Vec2(w, labelSize.y + ImGui.style.framePadding.y * 2f))
        val totalBb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) ImGui.style.itemInnerSpacing.x + labelSize.x else 0f, 0f))

        ImGui.itemSize(totalBb, ImGui.style.framePadding.y)
        if (!ImGui.itemAdd(totalBb, id, frameBb))
            return false

        // Default format string when passing NULL
        val format = when {
            format_ == null -> when (dataType) {
                DataType.Float, DataType.Double -> "%f"
                else -> "%d" // (FIXME-LEGACY: Patch old "%.0f" format string to use "%d", read function more details.)
            }
            dataType == DataType.Int && format_ != "%d" -> patchFormatStringFloatToInt(format_)
            else -> format_
        }

        // Tabbing or CTRL-clicking on Slider turns it into an input box
        val hovered = ImGui.itemHoverable(frameBb, id)
        var tempInputIsActive = ImGui.tempInputIsActive(id)
        if (!tempInputIsActive) {
            val focusRequested = ImGui.focusableItemRegister(window, id)
            val clicked = hovered && ImGui.io.mouseClicked[0]
            if (focusRequested || clicked || g.navActivateId == id || g.navInputId == id) {
                ImGui.setActiveID(id, window)
                ImGui.setFocusID(id, window)
                ImGui.focusWindow(window)
                g.activeIdUsingNavDirMask = g.activeIdUsingNavDirMask or ((1 shl Dir.Left) or (1 shl Dir.Right))
                if (focusRequested || (clicked && ImGui.io.keyCtrl) || g.navInputId == id) {
                    tempInputIsActive = true
                    ImGui.focusableItemUnregister(window)
                }
            }
        }

        // Our current specs do NOT clamp when using CTRL+Click manual input, but we should eventually add a flag for that..
        if (tempInputIsActive)
            return ImGui.tempInputScalar(frameBb, id, label, DataType.Float, pData, format)// , p_min, p_max)

        // Draw frame
        val frameCol = if (g.activeId == id) Col.FrameBgActive else if (g.hoveredId == id) Col.FrameBgHovered else Col.FrameBg
        ImGui.renderNavHighlight(frameBb, id)
        ImGui.renderFrame(frameBb.min, frameBb.max, frameCol.u32, true, ImGui.style.frameRounding)

        // Slider behavior
        val grabBb = Rect()
        val valueChanged = sliderBehavior(frameBb, id, dataType, pData, pMin!!, pMax!!, format, power, flags, grabBb)
        if (valueChanged)
            ImGui.markItemEdited(id)

        // Render grab
        if (grabBb.max.x > grabBb.min.x)
            window.drawList.addRectFilled(grabBb.min, grabBb.max, ImGui.getColorU32(if (g.activeId == id) Col.SliderGrabActive else Col.SliderGrab), ImGui.style.grabRounding)

        // Display value using user-provided display format so user can add prefix/suffix/decorations to the value.
        val value = format.format(ImGui.style.locale, when (val data = pData()) {
            is Ubyte -> data.v
            is Ushort -> data.v
            is Uint -> data.v
            is Ulong -> data.v
            else -> data
        })
        ImGui.renderTextClipped(frameBb.min, frameBb.max, value, null, Vec2(0.5f))

        if (labelSize.x > 0f)
            ImGui.renderText(Vec2(frameBb.max.x + ImGui.style.itemInnerSpacing.x, frameBb.min.y + ImGui.style.framePadding.y), label)

        Hook.itemInfo?.invoke(g, id, label, window.dc.itemFlags)
        return valueChanged
    }

    /** Internal implementation */
    fun <N> sliderScalarNInternal(label: String, dataType: DataType, pData: Any, components: Int, pMin: N? = null, pMax: N? = null,
                                  format: String? = null, power: Float = 1f, flags: SliderFlags = 0): Boolean
            where N : Number, N : Comparable<N> {

        val window = ImGui.currentWindow
        if (window.skipItems) return false

        var valueChanged = false
        ImGui.beginGroup()
        ImGui.pushID(label)
        ImGui.pushMultiItemsWidths(components, ImGui.calcItemWidth())
        for (i in 0 until components) {
            ImGui.pushID(i)
            if (i > 0)
                ImGui.sameLine(0f, ImGui.style.itemInnerSpacing.x)
            valueChanged = when (dataType) {
                DataType.Int -> withInt(pData as IntArray, i) {
                    sliderScalarInternal("", dataType, it as KMutableProperty0<N>, pMin, pMax, format, power, flags)
                }
                DataType.Float -> withFloat(pData as FloatArray, i) {
                    sliderScalarInternal("", dataType, it as KMutableProperty0<N>, pMin, pMax, format, power, flags)
                }
                else -> error("invalid")
            } || valueChanged
            ImGui.popID()
            ImGui.popItemWidth()
        }
        ImGui.popID()

        val labelEnd = ImGui.findRenderedTextEnd(label)
        if (0 != labelEnd) {
            ImGui.sameLine(0f, ImGui.style.itemInnerSpacing.x)
            ImGui.textEx(label, labelEnd)
        }
        ImGui.endGroup()
        return valueChanged
    }

    /** Internal implementation */
    fun <N> vSliderScalarInternal(label: String, size: Vec2, dataType: DataType, pData: KMutableProperty0<N>, pMin: N? = null, pMax: N? = null,
                          format_: String? = null, power: Float = 1f, flags: SliderFlags = 0): Boolean
            where N : Number, N : Comparable<N> {

        val window = ImGui.currentWindow
        if (window.skipItems) return false

        val id = window.getID(label)

        val labelSize = ImGui.calcTextSize(label, hideTextAfterDoubleHash = true)
        val frameBb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        val bb = Rect(frameBb.min, frameBb.max + Vec2(if (labelSize.x > 0f) ImGui.style.itemInnerSpacing.x + labelSize.x else 0f, 0f))

        ImGui.itemSize(bb, ImGui.style.framePadding.y)
        if (!ImGui.itemAdd(frameBb, id)) return false

        // Default format string when passing NULL
        val format = when {
            format_ == null -> when (dataType) {
                DataType.Float, DataType.Double -> "%f"
                else -> "%d" // (FIXME-LEGACY: Patch old "%.0f" format string to use "%d", read function more details.)
            }
            dataType == DataType.Int && format_ != "%d" -> patchFormatStringFloatToInt(format_)
            else -> format_
        }
        val hovered = ImGui.itemHoverable(frameBb, id)
        if ((hovered && ImGui.io.mouseClicked[0]) || g.navActivateId == id || g.navInputId == id) {
            ImGui.setActiveID(id, window)
            ImGui.setFocusID(id, window)
            ImGui.focusWindow(window)
            g.activeIdUsingNavDirMask = g.activeIdUsingNavDirMask or ((1 shl Dir.Up) or (1 shl Dir.Down))
        }

        // Draw frame
        val frameCol = if (g.activeId == id) Col.FrameBgActive else if (g.hoveredId == id) Col.FrameBgHovered else Col.FrameBg
        ImGui.renderNavHighlight(frameBb, id)
        ImGui.renderFrame(frameBb.min, frameBb.max, frameCol.u32, true, ImGui.style.frameRounding)
        // Slider behavior
        val grabBb = Rect()
        val valueChanged = sliderBehavior(frameBb, id, dataType, pData, pMin!!, pMax!!, format, power,
                flags or SliderFlag.Vertical.i, grabBb)

        if (valueChanged)
            ImGui.markItemEdited(id)

        // Render grab
        if (grabBb.max.y > grabBb.min.y)
            window.drawList.addRectFilled(grabBb.min, grabBb.max, ImGui.getColorU32(if (g.activeId == id) Col.SliderGrabActive else Col.SliderGrab), ImGui.style.grabRounding)

        /*  Display value using user-provided display format so user can add prefix/suffix/decorations to the value.
            For the vertical slider we allow centered text to overlap the frame padding         */
        val value = pData.format(dataType, format)
        val posMin = Vec2(frameBb.min.x, frameBb.min.y + ImGui.style.framePadding.y)
        ImGui.renderTextClipped(posMin, frameBb.max, value, null, Vec2(0.5f, 0f))
        if (labelSize.x > 0f)
            ImGui.renderText(Vec2(frameBb.max.x + ImGui.style.itemInnerSpacing.x, frameBb.min.y + ImGui.style.framePadding.y), label)

        return valueChanged
    }
}