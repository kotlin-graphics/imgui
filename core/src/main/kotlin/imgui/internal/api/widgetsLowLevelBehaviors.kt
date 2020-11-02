package imgui.internal.api

import gli_.has
import gli_.hasnt
import glm_.*
import glm_.func.common.max
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.calcTextSize
import imgui.ImGui.calcTypematicRepeatAmount
import imgui.ImGui.clearActiveID
import imgui.ImGui.currentWindow
import imgui.ImGui.dragBehaviorT
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.focusWindow
import imgui.ImGui.hoveredId
import imgui.ImGui.indent
import imgui.ImGui.io
import imgui.ImGui.isItemHovered
import imgui.ImGui.isMouseClicked
import imgui.ImGui.itemAdd
import imgui.ImGui.itemHoverable
import imgui.ImGui.itemSize
import imgui.ImGui.logRenderedText
import imgui.ImGui.markItemEdited
import imgui.ImGui.mouseCursor
import imgui.ImGui.navMoveRequestCancel
import imgui.ImGui.renderFrame
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.setActiveID
import imgui.ImGui.setFocusID
import imgui.ImGui.setItemAllowOverlap
import imgui.ImGui.sliderBehaviorT
import imgui.ImGui.style
import imgui.api.g
import imgui.internal.classes.Rect
import imgui.internal.floor
import imgui.internal.sections.*
import kool.getValue
import kool.setValue
import unsigned.Ubyte
import unsigned.Uint
import unsigned.Ulong
import unsigned.Ushort
import kotlin.math.max
import kotlin.reflect.KMutableProperty0
import imgui.TreeNodeFlag as Tnf
import imgui.internal.sections.ButtonFlag as Bf

@Suppress("UNCHECKED_CAST")

const val DRAG_DROP_HOLD_TIMER = 0.7f

/** Widgets low-level behaviors */
internal interface widgetsLowLevelBehaviors {

    /** @return []pressed, hovered, held] */
    fun buttonBehavior(bb: Rect, id: ID, flag: Bf) = buttonBehavior(bb, id, flag.i)

    /** @return []pressed, hovered, held]
     *
     *  The ButtonBehavior() function is key to many interactions and used by many/most widgets.
     *  Because we handle so many cases (keyboard/gamepad navigation, drag and drop) and many specific behavior (via ImGuiButtonFlags_),
     *  this code is a little complex.
     *  By far the most common path is interacting with the Mouse using the default ImGuiButtonFlags_PressedOnClickRelease button behavior.
     *  See the series of events below and the corresponding state reported by dear imgui:
     *  ------------------------------------------------------------------------------------------------------------------------------------------------
     *  with PressedOnClickRelease:             return-value  IsItemHovered()  IsItemActive()  IsItemActivated()  IsItemDeactivated()  IsItemClicked()
     *    Frame N+0 (mouse is outside bb)        -             -                -               -                  -                    -
     *    Frame N+1 (mouse moves inside bb)      -             true             -               -                  -                    -
     *    Frame N+2 (mouse button is down)       -             true             true            true               -                    true
     *    Frame N+3 (mouse button is down)       -             true             true            -                  -                    -
     *    Frame N+4 (mouse moves outside bb)     -             -                true            -                  -                    -
     *    Frame N+5 (mouse moves inside bb)      -             true             true            -                  -                    -
     *    Frame N+6 (mouse button is released)   true          true             -               -                  true                 -
     *    Frame N+7 (mouse button is released)   -             true             -               -                  -                    -
     *    Frame N+8 (mouse moves outside bb)     -             -                -               -                  -                    -
     *  ------------------------------------------------------------------------------------------------------------------------------------------------
     *  with PressedOnClick:                    return-value  IsItemHovered()  IsItemActive()  IsItemActivated()  IsItemDeactivated()  IsItemClicked()
     *    Frame N+2 (mouse button is down)       true          true             true            true               -                    true
     *    Frame N+3 (mouse button is down)       -             true             true            -                  -                    -
     *    Frame N+6 (mouse button is released)   -             true             -               -                  true                 -
     *    Frame N+7 (mouse button is released)   -             true             -               -                  -                    -
     *  ------------------------------------------------------------------------------------------------------------------------------------------------
     *  with PressedOnRelease:                  return-value  IsItemHovered()  IsItemActive()  IsItemActivated()  IsItemDeactivated()  IsItemClicked()
     *    Frame N+2 (mouse button is down)       -             true             -               -                  -                    true
     *    Frame N+3 (mouse button is down)       -             true             -               -                  -                    -
     *    Frame N+6 (mouse button is released)   true          true             -               -                  -                    -
     *    Frame N+7 (mouse button is released)   -             true             -               -                  -                    -
     *  ------------------------------------------------------------------------------------------------------------------------------------------------
     *  with PressedOnDoubleClick:              return-value  IsItemHovered()  IsItemActive()  IsItemActivated()  IsItemDeactivated()  IsItemClicked()
     *    Frame N+0 (mouse button is down)       -             true             -               -                  -                    true
     *    Frame N+1 (mouse button is down)       -             true             -               -                  -                    -
     *    Frame N+2 (mouse button is released)   -             true             -               -                  -                    -
     *    Frame N+3 (mouse button is released)   -             true             -               -                  -                    -
     *    Frame N+4 (mouse button is down)       true          true             true            true               -                    true
     *    Frame N+5 (mouse button is down)       -             true             true            -                  -                    -
     *    Frame N+6 (mouse button is released)   -             true             -               -                  true                 -
     *    Frame N+7 (mouse button is released)   -             true             -               -                  -                    -
     *  ------------------------------------------------------------------------------------------------------------------------------------------------
     *  Note that some combinations are supported,
     *  - PressedOnDragDropHold can generally be associated with any flag.
     *  - PressedOnDoubleClick can be associated by PressedOnClickRelease/PressedOnRelease, in which case the second release event won't be reported.
     *  ------------------------------------------------------------------------------------------------------------------------------------------------
     *  The behavior of the return-value changes when ImGuiButtonFlags_Repeat is set:
     *                                          Repeat+                  Repeat+           Repeat+             Repeat+
     *                                          PressedOnClickRelease    PressedOnClick    PressedOnRelease    PressedOnDoubleClick
     *  -------------------------------------------------------------------------------------------------------------------------------------------------
     *    Frame N+0 (mouse button is down)       -                        true              -                   true
     *    ...                                    -                        -                 -                   -
     *    Frame N + RepeatDelay                  true                     true              -                   true
     *    ...                                    -                        -                 -                   -
     *    Frame N + RepeatDelay + RepeatRate*N   true                     true              -                   true
     *  -------------------------------------------------------------------------------------------------------------------------------------------------   */
    fun buttonBehavior(bb: Rect, id: ID, flags_: ButtonFlags = 0): BooleanArray {

        val window = currentWindow
        var flags = flags_

        if (flags has Bf.Disabled) {
            if (g.activeId == id) clearActiveID()
            return BooleanArray(3)
        }

        // Default only reacts to left mouse button
        if (flags hasnt Bf.MouseButtonMask_)
            flags = flags or Bf.MouseButtonDefault_

        // Default behavior requires click + release inside bounding box
        if (flags hasnt Bf.PressedOnMask_)
            flags = flags or Bf.PressedOnDefault_

        val backupHoveredWindow = g.hoveredWindow
        val flattenHoveredChildren = flags has Bf.FlattenChildren && g.hoveredRootWindow === window
        if (flattenHoveredChildren)
            g.hoveredWindow = window

        if (IMGUI_ENABLE_TEST_ENGINE && id != 0 && window.dc.lastItemId != id)
            Hook.itemAdd!!(g, bb, id)

        var pressed = false
        var hovered = itemHoverable(bb, id)

        // Drag source doesn't report as hovered
        if (hovered && g.dragDropActive && g.dragDropPayload.sourceId == id && g.dragDropSourceFlags hasnt DragDropFlag.SourceNoDisableHover)
            hovered = false

        // Special mode for Drag and Drop where holding button pressed for a long time while dragging another item triggers the button
        if (g.dragDropActive && flags has Bf.PressedOnDragDropHold && g.dragDropSourceFlags hasnt DragDropFlag.SourceNoHoldToOpenOthers)
            if (isItemHovered(HoveredFlag.AllowWhenBlockedByActiveItem)) {
                hovered = true
                hoveredId = id
                if (calcTypematicRepeatAmount(g.hoveredIdTimer + 0.0001f - io.deltaTime, g.hoveredIdTimer + 0.0001f - io.deltaTime, DRAG_DROP_HOLD_TIMER, 0f) != 0) {
                    pressed = true
                    g.dragDropHoldJustPressedId = id
                    focusWindow(window)
                }
            }

        if (flattenHoveredChildren)
            g.hoveredWindow = backupHoveredWindow

        /*  AllowOverlap mode (rarely used) requires previous frame hoveredId to be null or to match. This allows using
            patterns where a later submitted widget overlaps a previous one.         */
        if (hovered && flags has Bf.AllowItemOverlap && g.hoveredIdPreviousFrame != id && g.hoveredIdPreviousFrame != 0)
            hovered = false

        // Mouse handling
        if (hovered) {

            if (flags hasnt Bf.NoKeyModifiers || (!io.keyCtrl && !io.keyShift && !io.keyAlt)) {

                // Poll buttons
                val mouseButtonClicked = when {
                    flags has Bf.MouseButtonLeft && io.mouseClicked[0] -> 0
                    flags has Bf.MouseButtonRight && io.mouseClicked[1] -> 1
                    flags has Bf.MouseButtonMiddle && io.mouseClicked[2] -> 2
                    else -> -1
                }
                val mouseButtonReleased = when {
                    flags has Bf.MouseButtonLeft && io.mouseReleased[0] -> 0
                    flags has Bf.MouseButtonRight && io.mouseReleased[1] -> 1
                    flags has Bf.MouseButtonMiddle && io.mouseReleased[2] -> 2
                    else -> -1
                }

                if (mouseButtonClicked != -1 && g.activeId != id) {
                    if (flags has (Bf.PressedOnClickRelease or Bf.PressedOnClickReleaseAnywhere)) {
                        setActiveID(id, window)
                        g.activeIdMouseButton = mouseButtonClicked
                        if (flags hasnt Bf.NoNavFocus)
                            setFocusID(id, window)
                        focusWindow(window)
                    }
                    if (flags has Bf.PressedOnClick || (flags has Bf.PressedOnDoubleClick && io.mouseDoubleClicked[mouseButtonClicked])) {
                        pressed = true
                        if (flags has Bf.NoHoldingActiveId)
                            clearActiveID()
                        else
                            setActiveID(id, window) // Hold on ID
                        g.activeIdMouseButton = mouseButtonClicked
                        focusWindow(window)
                    }
                }
                if (flags has Bf.PressedOnRelease && mouseButtonReleased != -1) {
                    // Repeat mode trumps on release behavior
                    val hasRepeatedAtLeastOnce = flags has Bf.Repeat && io.mouseDownDurationPrev[mouseButtonReleased] >= io.keyRepeatDelay
                    if (!hasRepeatedAtLeastOnce)
                        pressed = true
                    clearActiveID()
                }

                /*  'Repeat' mode acts when held regardless of _PressedOn flags (see table above).
                Relies on repeat logic of IsMouseClicked() but we may as well do it ourselves if we end up exposing
                finer RepeatDelay/RepeatRate settings.  */
                if (g.activeId == id && flags has Bf.Repeat)
                    if (io.mouseDownDuration[g.activeIdMouseButton] > 0f && isMouseClicked(MouseButton of g.activeIdMouseButton, true))
                        pressed = true
            }

            if (pressed)
                g.navDisableHighlight = true
        }

        /*  Gamepad/Keyboard navigation
            We report navigated item as hovered but we don't set g.HoveredId to not interfere with mouse.         */
        if (g.navId == id && !g.navDisableHighlight && g.navDisableMouseHover && (g.activeId == 0 || g.activeId == id || g.activeId == window.moveId))
            if (flags hasnt Bf.NoHoveredOnFocus)
                hovered = true
        if (g.navActivateDownId == id) {
            val navActivatedByCode = g.navActivateId == id
            val navActivatedByInputs = NavInput.Activate.isTest(if (flags has Bf.Repeat) InputReadMode.Repeat else InputReadMode.Pressed)
            if (navActivatedByCode || navActivatedByInputs)
                pressed = true
            if (navActivatedByCode || navActivatedByInputs || g.activeId == id) {
                // Set active id so it can be queried by user via IsItemActive(), equivalent of holding the mouse button.
                g.navActivateId = id // This is so SetActiveId assign a Nav source
                setActiveID(id, window)
                if ((navActivatedByCode || navActivatedByInputs) && flags hasnt Bf.NoNavFocus)
                    setFocusID(id, window)
            }
        }

        // Process while held
        var held = false
        if (g.activeId == id) {
            if (g.activeIdSource == InputSource.Mouse) {
                if (g.activeIdIsJustActivated)
                    g.activeIdClickOffset = io.mousePos - bb.min

                val mouseButton = g.activeIdMouseButton
                assert(mouseButton >= 0 && mouseButton < MouseButton.COUNT)
                if (io.mouseDown[mouseButton])
                    held = true
                else {
                    val releaseIn = hovered && flags has Bf.PressedOnClickRelease
                    val releaseAnywhere = flags has Bf.PressedOnClickReleaseAnywhere
                    if ((releaseIn || releaseAnywhere) && !g.dragDropActive) {
                        // Report as pressed when releasing the mouse (this is the most common path)
                        val isDoubleClickRelease = flags has Bf.PressedOnDoubleClick && io.mouseDownWasDoubleClick[mouseButton]
                        val isRepeatingAlready = flags has Bf.Repeat && io.mouseDownDurationPrev[mouseButton] >= io.keyRepeatDelay // Repeat mode trumps <on release>
                        if (!isDoubleClickRelease && !isRepeatingAlready)
                            pressed = true
                    }
                    clearActiveID()
                }
                if (flags hasnt Bf.NoNavFocus)
                    g.navDisableHighlight = true
            } else if (g.activeIdSource == InputSource.Nav)
            // When activated using Nav, we hold on the ActiveID until activation button is released
                if (g.navActivateDownId != id)
                    clearActiveID()
            if (pressed)
                g.activeIdHasBeenPressedBefore = true
        }
        return booleanArrayOf(pressed, hovered, held)
    }

    fun dragBehavior(id: ID, dataType: DataType, pV: FloatArray, ptr: Int, vSpeed: Float, pMin: Float?, pMax: Float?,
                     format: String, flags: SliderFlags): Boolean =
            withFloat(pV, ptr) { dragBehavior(id, DataType.Float, it, vSpeed, pMin, pMax, format, flags) }

    fun <N> dragBehavior(id: ID, dataType: DataType, pV: KMutableProperty0<N>, vSpeed: Float, pMin: Number?,
                         pMax: Number?, format: String, flags: SliderFlags): Boolean
            where N : Number, N : Comparable<N> {

        assert(flags == 1 || flags hasnt SliderFlag.InvalidMask_.i) { """
            Read imgui.cpp "API BREAKING CHANGES" section for 1.78 if you hit this assert.
            Invalid ImGuiSliderFlags flags! Has the 'float power' argument been mistakenly cast to flags? Call function with ImGuiSliderFlags_Logarithmic flags instead.""".trimIndent()
        }

        if (g.activeId == id)
            if (g.activeIdSource == InputSource.Mouse && !io.mouseDown[0])
                clearActiveID()
            else if (g.activeIdSource == InputSource.Nav && g.navActivatePressedId == id && !g.activeIdIsJustActivated)
                clearActiveID()

        if (g.activeId != id)
            return false
        if (g.currentWindow!!.dc.itemFlags has ItemFlag.ReadOnly || flags has SliderFlag._ReadOnly)
            return false

        var v by pV

        return when (v) {
            is Byte -> {
                _i = v.i
                val min = pMin ?: Byte.MIN_VALUE
                val max = pMax ?: Byte.MAX_VALUE
                dragBehaviorT(DataType.Int, ::_i, vSpeed, min.i, max.i, format, flags).also {
                    if (it)
                        v = _i.b as N
                }
            }
            is Ubyte -> {
                _ui.v = v.i
                val min = pMin ?: Ubyte.MIN_VALUE
                val max = pMax ?: Ubyte.MAX_VALUE
                dragBehaviorT(DataType.Uint, ::_ui, vSpeed, min.ui, max.ui, format, flags).also {
                    if (it)
                        (v as Ubyte).v = _ui.b
                }
            }
            is Short -> {
                _i = v.i
                val min = pMin ?: Short.MIN_VALUE
                val max = pMax ?: Short.MAX_VALUE
                dragBehaviorT(DataType.Int, ::_i, vSpeed, min.i, max.i, format, flags).also {
                    if (it)
                        v = _i.s as N
                }
            }
            is Ushort -> {
                _ui.v = v.i
                val min = pMin ?: Ushort.MIN_VALUE
                val max = pMax ?: Ushort.MAX_VALUE
                dragBehaviorT(DataType.Uint, ::_ui, vSpeed, min.ui, max.ui, format, flags).also {
                    if (it)
                        (v as Ushort).v = _ui.s
                }
            }
            is Int -> {
                val min = pMin ?: Int.MIN_VALUE
                val max = pMax ?: Int.MAX_VALUE
                dragBehaviorT(DataType.Int, pV as KMutableProperty0<Int>, vSpeed, min.i, max.i, format, flags)
            }
            is Uint -> {
                val min = pMin ?: Uint.MIN_VALUE
                val max = pMax ?: Uint.MAX_VALUE
                dragBehaviorT(DataType.Uint, pV as KMutableProperty0<Uint>, vSpeed, min.ui, max.ui, format, flags)
            }
            is Long -> {
                val min = pMin ?: Long.MIN_VALUE
                val max = pMax ?: Long.MAX_VALUE
                dragBehaviorT(DataType.Long, pV as KMutableProperty0<Long>, vSpeed, min.L, max.L, format, flags)
            }
            is Ulong -> {
                val min = pMin ?: Ulong.MIN_VALUE
                val max = pMax ?: Ulong.MAX_VALUE
                dragBehaviorT(DataType.Ulong, pV as KMutableProperty0<Ulong>, vSpeed, min.ul, max.ul, format, flags)
            }
            is Float -> {
                val min = pMin ?: Float.MIN_VALUE
                val max = pMax ?: Float.MAX_VALUE
                dragBehaviorT(DataType.Float, pV as KMutableProperty0<Float>, vSpeed, min.f, max.f, format, flags)
            }
            is Double -> {
                val min = pMin ?: Double.MIN_VALUE
                val max = pMax ?: Double.MAX_VALUE
                dragBehaviorT(DataType.Double, pV as KMutableProperty0<Double>, vSpeed, min.d, max.d, format, flags)
            }
            else -> error("Invalid") // ~IM_ASSERT(0); return false;
        }
    }

    /** For 32-bits and larger types, slider bounds are limited to half the natural type range.
     *  So e.g. an integer Slider between INT_MAX-10 and INT_MAX will fail, but an integer Slider between INT_MAX/2-10 and INT_MAX/2 will be ok.
     *  It would be possible to lift that limitation with some work but it doesn't seem to be worth it for sliders. */
    fun sliderBehavior(bb: Rect, id: ID, pV: FloatArray, pMin: Float, pMax: Float, format: String,
                       flags: SliderFlags, outGrabBb: Rect): Boolean =
            sliderBehavior(bb, id, pV, 0, pMin, pMax, format, flags, outGrabBb)

    fun sliderBehavior(bb: Rect, id: ID, pV: FloatArray, ptr: Int, pMin: Float, pMax: Float,
                       format: String, flags: SliderFlags, outGrabBb: Rect): Boolean =
            withFloat(pV, ptr) {
                sliderBehavior(bb, id, DataType.Float, it, pMin, pMax, format, flags, outGrabBb)
            }

//    fun <N> sliderBehavior(bb: Rect, id: ID,
//                           v: KMutableProperty0<N>,
//                           vMin: Float, vMax: Float,
//                           format: String, power: Float,
//                           flags: SliderFlags, outGrabBb: Rect): Boolean where N : Number, N : Comparable<N> =
//            sliderBehavior(bb, id, DataType.Float, v, vMin, vMax, format, power, flags, outGrabBb)

    fun <N> sliderBehavior(bb: Rect, id: ID, dataType: DataType, pV: KMutableProperty0<N>, pMin: N, pMax: N,
                           format: String, flags: SliderFlags, outGrabBb: Rect): Boolean
            where N : Number, N : Comparable<N> {

        assert(flags == 1 || flags hasnt SliderFlag.InvalidMask_.i) {"""
            Read imgui.cpp "API BREAKING CHANGES" section for 1.78 if you hit this assert.
            Invalid ImGuiSliderFlags flag!  Has the 'float power' argument been mistakenly cast to flags? Call function with ImGuiSliderFlags_Logarithmic flags instead.""".trimIndent()
        }

        if (g.currentWindow!!.dc.itemFlags has ItemFlag.ReadOnly || flags has SliderFlag._ReadOnly)
            return false

        var v by pV

        return when (dataType) {
            DataType.Byte -> {
                _i = v.i
                sliderBehaviorT(bb, id, dataType, ::_i, pMin.i, pMax.i, format, flags, outGrabBb).also {
                    if (it)
                        v = _i.b as N
                }
            }
            DataType.Ubyte -> {
                _ui.v = v.i
                sliderBehaviorT(bb, id, dataType, ::_ui, pMin.ui, pMax.ui, format, flags, outGrabBb).also {
                    if (it)
                        (v as Ubyte).v = _ui.b
                }
            }
            DataType.Short -> {
                _i = v.i
                sliderBehaviorT(bb, id, dataType, ::_i, pMin.i, pMax.i, format, flags, outGrabBb).also {
                    if (it)
                        v = _i.s as N
                }
            }
            DataType.Ushort -> {
                _ui.v = v.i
                sliderBehaviorT(bb, id, dataType, ::_ui, pMin.ui, pMax.ui, format, flags, outGrabBb).also {
                    if (it)
                        (v as Ushort).v = _ui.s
                }
            }
            DataType.Int -> {
                assert(pMin as Int >= Int.MIN_VALUE / 2 && pMax as Int <= Int.MAX_VALUE / 2)
                sliderBehaviorT(bb, id, dataType, pV as KMutableProperty0<Int>, pMin, pMax as Int, format, flags, outGrabBb)
            }
            DataType.Uint -> {
                assert(pMax as Uint <= Uint.MAX / 2)
                sliderBehaviorT(bb, id, dataType, pV as KMutableProperty0<Uint>, pMin as Uint, pMax as Uint, format, flags, outGrabBb)
            }
            DataType.Long -> {
                assert(pMin as Long >= Long.MIN_VALUE / 2 && pMax as Long <= Long.MAX_VALUE / 2)
                sliderBehaviorT(bb, id, dataType, pV as KMutableProperty0<Long>, pMin as Long, pMax as Long, format, flags, outGrabBb)
            }
            DataType.Ulong -> {
                assert(pMax as Ulong <= Ulong.MAX / 2)
                sliderBehaviorT(bb, id, dataType, pV as KMutableProperty0<Ulong>, pMin as Ulong, pMax as Ulong, format, flags, outGrabBb)
            }
            DataType.Float -> {
                assert(pMin as Float >= -Float.MAX_VALUE / 2f && pMax as Float <= Float.MAX_VALUE / 2f)
                sliderBehaviorT(bb, id, dataType, pV as KMutableProperty0<Float>, pMin as Float, pMax as Float, format, flags, outGrabBb)
            }
            DataType.Double -> {
                assert(pMin as Double >= -Double.MAX_VALUE / 2f && pMax as Double <= Double.MAX_VALUE / 2f)
                sliderBehaviorT(bb, id, dataType, pV as KMutableProperty0<Double>, pMin as Double, pMax as Double, format, flags, outGrabBb)
            }
            else -> throw Error()
        }
    }

    /** Using 'hover_visibility_delay' allows us to hide the highlight and mouse cursor for a short time, which can be convenient to reduce visual noise. */
    fun splitterBehavior(bb: Rect, id: ID, axis: Axis,
                         size1ptr: KMutableProperty0<Float>, size2ptr: KMutableProperty0<Float>,
                         minSize1: Float, minSize2: Float,
                         hoverExtend: Float = 0f, hoverVisibilityDelay: Float = 0f): Boolean {

        var size1 by size1ptr
        var size2 by size2ptr
        val window = g.currentWindow!!

        val itemFlagsBackup = window.dc.itemFlags

        window.dc.itemFlags = window.dc.itemFlags or (ItemFlag.NoNav or ItemFlag.NoNavDefaultFocus)

        val itemAdd = itemAdd(bb, id)
        window.dc.itemFlags = itemFlagsBackup
        if (!itemAdd) return false

        val bbInteract = Rect(bb)
        bbInteract expand if (axis == Axis.Y) Vec2(0f, hoverExtend) else Vec2(hoverExtend, 0f)
        val (_, hovered, held) = buttonBehavior(bbInteract, id, Bf.FlattenChildren or Bf.AllowItemOverlap)
        if (g.activeId != id) setItemAllowOverlap()

        if (held || (g.hoveredId == id && g.hoveredIdPreviousFrame == id && g.hoveredIdTimer >= hoverVisibilityDelay))
            mouseCursor = if (axis == Axis.Y) MouseCursor.ResizeNS else MouseCursor.ResizeEW

        val bbRender = Rect(bb)
        if (held) {
            val mouseDelta2d = io.mousePos - g.activeIdClickOffset - bbInteract.min
            var mouseDelta = if (axis == Axis.Y) mouseDelta2d.y else mouseDelta2d.x

            // Minimum pane size
            val size1MaximumDelta = max(0f, size1 - minSize1)
            val size2MaximumDelta = max(0f, size2 - minSize2)
            if (mouseDelta < -size1MaximumDelta)
                mouseDelta = -size1MaximumDelta
            if (mouseDelta > size2MaximumDelta)
                mouseDelta = size2MaximumDelta


            // Apply resize
            if (mouseDelta != 0f) {
                if (mouseDelta < 0f)
                    assert(size1 + mouseDelta >= minSize1)
                else if (mouseDelta > 0f)
                    assert(size2 - mouseDelta >= minSize2)
                size1 = size1 + mouseDelta // cant += because of https://youtrack.jetbrains.com/issue/KT-14833
                size2 = size2 - mouseDelta
                bbRender translate if (axis == Axis.X) Vec2(mouseDelta, 0f) else Vec2(0f, mouseDelta)
                markItemEdited(id)
            }
            bbRender translate if (axis == Axis.X) Vec2(mouseDelta, 0f) else Vec2(0f, mouseDelta)

            markItemEdited(id)
        }

        // Render
        val col = when {
            held -> Col.SeparatorActive
            hovered && g.hoveredIdTimer >= hoverVisibilityDelay -> Col.SeparatorHovered
            else -> Col.Separator
        }
        window.drawList.addRectFilled(bbRender.min, bbRender.max, col.u32, 0f)

        return held
    }

    fun treeNodeBehavior(id: ID, flags: TreeNodeFlags, label: String): Boolean = treeNodeBehavior(id, flags, label.toByteArray())

    fun treeNodeBehavior(id: ID, flags: TreeNodeFlags, label: ByteArray, labelEnd_: Int = -1): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val displayFrame = flags has Tnf.Framed
        val padding = when {
            displayFrame || flags has Tnf.FramePadding -> Vec2(style.framePadding)
            else -> Vec2(style.framePadding.x, window.dc.currLineTextBaseOffset min style.framePadding.y)
        }

        val labelEnd = if (labelEnd_ == -1) findRenderedTextEnd(label, 0) else labelEnd_
        val labelSize = calcTextSize(label, 0, labelEnd, false)

        // We vertically grow up to current line height up the typical widget height.
        val frameHeight = glm.max(glm.min(window.dc.currLineSize.y, g.fontSize + style.framePadding.y * 2), labelSize.y + padding.y * 2)
        val frameBb = Rect(
                x1 = if (flags has Tnf.SpanFullWidth) window.workRect.min.x else window.dc.cursorPos.x,
                y1 = window.dc.cursorPos.y,
                x2 = window.workRect.max.x,
                y2 = window.dc.cursorPos.y + frameHeight)
        if (displayFrame) {
            // Framed header expand a little outside the default padding, to the edge of InnerClipRect
            // (FIXME: May remove this at some point and make InnerClipRect align with WindowPadding.x instead of WindowPadding.x*0.5f)
            frameBb.min.x -= floor(window.windowPadding.x * 0.5f - 1f)
            frameBb.max.x += floor(window.windowPadding.x * 0.5f)
        }

        val textOffsetX = g.fontSize + padding.x * if (displayFrame) 3 else 2                   // Collapser arrow width + Spacing
        val textOffsetY = padding.y max window.dc.currLineTextBaseOffset                        // Latch before ItemSize changes it
        val textWidth = g.fontSize + if (labelSize.x > 0f) labelSize.x + padding.x * 2 else 0f  // Include collapser
        val textPos = Vec2(window.dc.cursorPos.x + textOffsetX, window.dc.cursorPos.y + textOffsetY)
        itemSize(Vec2(textWidth, frameHeight), padding.y)

        // For regular tree nodes, we arbitrary allow to click past 2 worth of ItemSpacing
        val interactBb = Rect(frameBb)
        if (!displayFrame && flags hasnt (Tnf.SpanAvailWidth or Tnf.SpanFullWidth))
            interactBb.max.x = frameBb.min.x + textWidth + style.itemSpacing.x * 2f

        /*  Store a flag for the current depth to tell if we will allow closing this node when navigating one of its child.
            For this purpose we essentially compare if g.NavIdIsAlive went from 0 to 1 between TreeNode() and TreePop().
            This is currently only support 32 level deep and we are fine with (1 << Depth) overflowing into a zero. */
        val isLeaf = flags has Tnf.Leaf
        var isOpen = treeNodeBehaviorIsOpen(id, flags)
        if (isOpen && !g.navIdIsAlive && flags has Tnf.NavLeftJumpsBackHere && flags hasnt Tnf.NoTreePushOnOpen)
            window.dc.treeJumpToParentOnPopMask = window.dc.treeJumpToParentOnPopMask or (1 shl window.dc.treeDepth)

        val itemAdd = itemAdd(interactBb, id)
        window.dc.lastItemStatusFlags = window.dc.lastItemStatusFlags or ItemStatusFlag.HasDisplayRect
        window.dc.lastItemDisplayRect put frameBb

        if (!itemAdd) {
            if (isOpen && flags hasnt Tnf.NoTreePushOnOpen)
                treePushOverrideID(id)
            Hook.itemInfo?.invoke(g, window.dc.lastItemId, label.cStr, window.dc.itemFlags or (if (isLeaf) ItemStatusFlag.None else ItemStatusFlag.Openable) or if (isOpen) ItemStatusFlag.Opened else ItemStatusFlag.None)
            return isOpen
        }

        var buttonFlags = Bf.None.i
        if (flags has Tnf.AllowItemOverlap)
            buttonFlags = buttonFlags or Bf.AllowItemOverlap
        if (!isLeaf)
            buttonFlags = buttonFlags or Bf.PressedOnDragDropHold

        // We allow clicking on the arrow section with keyboard modifiers held, in order to easily
        // allow browsing a tree while preserving selection with code implementing multi-selection patterns.
        // When clicking on the rest of the tree node we always disallow keyboard modifiers.
        val arrowHitX1 = (textPos.x - textOffsetX) - style.touchExtraPadding.x
        val arrowHitX2 = (textPos.x - textOffsetX) + (g.fontSize + padding.x * 2f) + style.touchExtraPadding.x
        val isMouseXOverArrow = io.mousePos.x >= arrowHitX1 && io.mousePos.x < arrowHitX2
        if (window !== g.hoveredWindow || !isMouseXOverArrow)
            buttonFlags = buttonFlags or Bf.NoKeyModifiers

        // Open behaviors can be altered with the _OpenOnArrow and _OnOnDoubleClick flags.
        // Some alteration have subtle effects (e.g. toggle on MouseUp vs MouseDown events) due to requirements for multi-selection and drag and drop support.
        // - Single-click on label = Toggle on MouseUp (default, when _OpenOnArrow=0)
        // - Single-click on arrow = Toggle on MouseDown (when _OpenOnArrow=0)
        // - Single-click on arrow = Toggle on MouseDown (when _OpenOnArrow=1)
        // - Double-click on label = Toggle on MouseDoubleClick (when _OpenOnDoubleClick=1)
        // - Double-click on arrow = Toggle on MouseDoubleClick (when _OpenOnDoubleClick=1 and _OpenOnArrow=0)
        // It is rather standard that arrow click react on Down rather than Up.
        // We set ImGuiButtonFlags_PressedOnClickRelease on OpenOnDoubleClick because we want the item to be active on the initial MouseDown in order for drag and drop to work.
        buttonFlags = buttonFlags or when {
            isMouseXOverArrow -> Bf.PressedOnClick.i
            flags has Tnf.OpenOnDoubleClick -> Bf.PressedOnClickRelease or Bf.PressedOnDoubleClick
            else -> Bf.PressedOnClickRelease.i
        }


        val selected = flags has Tnf.Selected
        val wasSelected = selected

        val (pressed, hovered, held) = buttonBehavior(interactBb, id, buttonFlags)
        var toggled = false
        if (!isLeaf) {
            if (pressed && g.dragDropHoldJustPressedId != id) {
                if (flags hasnt (Tnf.OpenOnArrow or Tnf.OpenOnDoubleClick) || g.navActivateId == id)
                    toggled = true
                if (flags has Tnf.OpenOnArrow)
                    toggled = (isMouseXOverArrow && !g.navDisableMouseHover) || toggled // Lightweight equivalent of IsMouseHoveringRect() since ButtonBehavior() already did the job
                if (flags has Tnf.OpenOnDoubleClick && io.mouseDoubleClicked[0])
                    toggled = true
            } else if (pressed && g.dragDropHoldJustPressedId == id) {
                assert(buttonFlags has Bf.PressedOnDragDropHold)
                if (!isOpen) // When using Drag and Drop "hold to open" we keep the node highlighted after opening, but never close it again.
                    toggled = true
            }

            if (g.navId == id && g.navMoveRequest && g.navMoveDir == Dir.Left && isOpen) {
                toggled = true
                navMoveRequestCancel()
            }
            // If there's something upcoming on the line we may want to give it the priority?
            if (g.navId == id && g.navMoveRequest && g.navMoveDir == Dir.Right && !isOpen) {
                toggled = true
                navMoveRequestCancel()
            }
            if (toggled) {
                isOpen = !isOpen
                window.dc.stateStorage[id] = isOpen
                window.dc.lastItemStatusFlags = window.dc.lastItemStatusFlags or ItemStatusFlag.ToggledOpen
            }
        }
        if (flags has Tnf.AllowItemOverlap)
            setItemAllowOverlap()

        // In this branch, TreeNodeBehavior() cannot toggle the selection so this will never trigger.
        if (selected != wasSelected)
            window.dc.lastItemStatusFlags = window.dc.lastItemStatusFlags or ItemStatusFlag.ToggledSelection

        // Render
        val textCol = Col.Text.u32
        val navHighlightFlags: NavHighlightFlags = NavHighlightFlag.TypeThin.i
        if (displayFrame) {
            // Framed type
            val bgCol = if (held && hovered) Col.HeaderActive else if (hovered) Col.HeaderHovered else Col.Header
            renderFrame(frameBb.min, frameBb.max, bgCol.u32, true, style.frameRounding)
            renderNavHighlight(frameBb, id, navHighlightFlags)
            if (flags has Tnf.Bullet)
                window.drawList.renderBullet(Vec2(textPos.x - textOffsetX * 0.6f, textPos.y + g.fontSize * 0.5f), textCol)
            else if (!isLeaf)
                window.drawList.renderArrow(Vec2(textPos.x - textOffsetX + padding.x, textPos.y), textCol, if (isOpen) Dir.Down else Dir.Right, 1f)
            else // Leaf without bullet, left-adjusted text
                textPos.x -= textOffsetX
            if (flags has Tnf._ClipLabelForTrailingButton)
                frameBb.max.x -= g.fontSize + style.framePadding.x
            if (g.logEnabled) {
                /*  NB: '##' is normally used to hide text (as a library-wide feature), so we need to specify the text
                    range to make sure the ## aren't stripped out here.                 */
                logRenderedText(textPos, "\n##", 3)
                renderTextClipped(textPos, frameBb.max, label, labelEnd, labelSize)
                logRenderedText(textPos, "#", 2) // TODO check me
            } else
                renderTextClipped(textPos, frameBb.max, label, labelEnd, labelSize)
        } else {
            // Unframed typed for tree nodes
            if (hovered || selected) {
                val bgCol = if (held && hovered) Col.HeaderActive else if (hovered) Col.HeaderHovered else Col.Header
                renderFrame(frameBb.min, frameBb.max, bgCol.u32, false)
                renderNavHighlight(frameBb, id, navHighlightFlags)
            }
            if (flags has Tnf.Bullet)
                window.drawList.renderBullet(Vec2(textPos.x - textOffsetX * 0.5f, textPos.y + g.fontSize * 0.5f), textCol)
            else if (!isLeaf)
                window.drawList.renderArrow(Vec2(textPos.x - textOffsetX + padding.x, textPos.y + g.fontSize * 0.15f), textCol, if (isOpen) Dir.Down else Dir.Right, 0.7f)
            if (g.logEnabled)
                logRenderedText(textPos, ">")
            renderText(textPos, label, 0, labelEnd, false)
        }

        if (isOpen && flags hasnt Tnf.NoTreePushOnOpen)
            treePushOverrideID(id)
        Hook.itemInfo?.invoke(g, id, label.cStr, window.dc.itemFlags or (if (isLeaf) ItemStatusFlag.None else ItemStatusFlag.Openable) or if (isOpen) ItemStatusFlag.Opened else ItemStatusFlag.None)
        return isOpen
    }

    /** Consume previous SetNextItemOpen() data, if any. May return true when logging */
    fun treeNodeBehaviorIsOpen(id: ID, flags: TreeNodeFlags = Tnf.None.i): Boolean {

        if (flags has Tnf.Leaf) return true

        // We only write to the tree storage if the user clicks (or explicitly use the SetNextItemOpen function)
        val window = g.currentWindow!!
        val storage = window.dc.stateStorage

        var isOpen: Boolean
        if (g.nextItemData.flags has NextItemDataFlag.HasOpen) {
            if (g.nextItemData.openCond == Cond.Always) {
                isOpen = g.nextItemData.openVal
                storage[id] = isOpen
            } else
            /*  We treat ImGuiSetCondition_Once and ImGuiSetCondition_FirstUseEver the same because tree node state
                are not saved persistently.                 */
                isOpen = storage.getOrPut(id) { g.nextItemData.openVal }
        } else
            isOpen = storage[id] ?: flags has Tnf.DefaultOpen

        /*  When logging is enabled, we automatically expand tree nodes (but *NOT* collapsing headers.. seems like
            sensible behavior).
            NB- If we are above max depth we still allow manually opened nodes to be logged.    */
        if (g.logEnabled && flags hasnt Tnf.NoAutoOpenOnLog && (window.dc.treeDepth - g.logDepthRef) < g.logDepthToExpand)
            isOpen = true

        return isOpen
    }

    fun treePushOverrideID(id: ID) {
        val window = g.currentWindow!!
        indent()
        window.dc.treeDepth++
        window.idStack.push(id)
    }
}