package imgui.internal.api

import glm_.has
import glm_.func.common.max
import glm_.glm
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.calcTextSize
import imgui.ImGui.calcTypematicRepeatAmount
import imgui.ImGui.clearActiveID
import imgui.ImGui.currentWindow
import imgui.ImGui.data
import imgui.ImGui.dragBehaviorT
import imgui.ImGui.findRenderedTextEnd
import imgui.ImGui.focusWindow
import imgui.ImGui.hoveredId
import imgui.ImGui.indent
import imgui.ImGui.io
import imgui.ImGui.isClicked
import imgui.ImGui.isDown
import imgui.ImGui.isItemHovered
import imgui.ImGui.isReleased
import imgui.ImGui.itemAdd
import imgui.ImGui.itemHoverable
import imgui.ImGui.itemSize
import imgui.ImGui.logSetNextTextDecoration
import imgui.ImGui.markItemEdited
import imgui.ImGui.mouseCursor
import imgui.ImGui.navClearPreferredPosForAxis
import imgui.ImGui.navMoveRequestCancel
import imgui.ImGui.pushOverrideID
import imgui.ImGui.renderArrow
import imgui.ImGui.renderBullet
import imgui.ImGui.renderFrame
import imgui.ImGui.renderNavHighlight
import imgui.ImGui.renderText
import imgui.ImGui.renderTextClipped
import imgui.ImGui.setActiveID
import imgui.ImGui.setFocusID
import imgui.ImGui.setItemAllowOverlap
import imgui.ImGui.setOwner
import imgui.ImGui.sliderBehaviorT
import imgui.ImGui.style
import imgui.ImGui.testOwner
import imgui.TreeNodeFlag
import imgui.api.g
import imgui.internal.classes.InputFlag
import imgui.internal.classes.Rect
import imgui.internal.floor
import imgui.internal.sections.*
import imgui.static.DRAGDROP_HOLD_TO_OPEN_TIMER
import kool.getValue
import kool.setValue
import kotlin.math.max
import kotlin.reflect.KMutableProperty0
import imgui.TreeNodeFlag as Tnf
import imgui.internal.sections.ButtonFlag as Bf

//@Suppress("UNCHECKED_CAST")

// Widgets

/** Widgets low-level behaviors */
internal interface widgetsLowLevelBehaviors {

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
     *  -------------------------------------------------------------------------------------------------------------------------------------------------
     *
     *  @return [pressed, hovered, held] */
    fun buttonBehavior(bb: Rect, id: ID, flags_: ButtonFlags = none): BooleanArray {

        val window = currentWindow
        var flags = flags_

        // Default only reacts to left mouse button
        if (flags hasnt Bf.MouseButtonMask)
            flags /= Bf.MouseButtonDefault

        // Default behavior requires click + release inside bounding box
        if (flags hasnt Bf.PressedOnMask)
            flags /= Bf.PressedOnDefault

        // Default behavior inherited from item flags
        val itemFlags = if (g.lastItemData.id == id) g.lastItemData.inFlags else g.currentItemFlags
        if (itemFlags has ItemFlag.ButtonRepeat)
            flags /= Bf.Repeat

        val backupHoveredWindow = g.hoveredWindow
        val hoveredWindow = g.hoveredWindow
        val flattenHoveredChildren = flags has Bf.FlattenChildren && hoveredWindow != null && hoveredWindow.rootWindow === window
        if (flattenHoveredChildren)
            g.hoveredWindow = window

        if (IMGUI_ENABLE_TEST_ENGINE)
        // Alternate registration spot, for when caller didn't use ItemAdd()
            if (id != 0 && g.lastItemData.id != id)
                IMGUI_TEST_ENGINE_ITEM_ADD(id, bb, null)

        var pressed = false
        var hovered = itemHoverable(bb, id)

        // Special mode for Drag and Drop where holding button pressed for a long time while dragging another item triggers the button
        if (g.dragDropActive && flags has Bf.PressedOnDragDropHold && g.dragDropSourceFlags hasnt DragDropFlag.SourceNoHoldToOpenOthers)
            if (isItemHovered(HoveredFlag.AllowWhenBlockedByActiveItem)) {
                hovered = true
                hoveredId = id
                if (g.hoveredIdTimer - io.deltaTime <= DRAGDROP_HOLD_TO_OPEN_TIMER && g.hoveredIdTimer >= DRAGDROP_HOLD_TO_OPEN_TIMER) {
                    pressed = true
                    g.dragDropHoldJustPressedId = id
                    focusWindow(window)
                }
            }

        if (flattenHoveredChildren)
            g.hoveredWindow = backupHoveredWindow

        // AllowOverlap mode (rarely used) requires previous frame HoveredId to be null or to match. This allows using patterns where a later submitted widget overlaps a previous one.
        if (hovered && flags has Bf.AllowOverlap && g.hoveredIdPreviousFrame != id)
            hovered = false

        // Mouse handling
        val testOwnerId = if (flags has Bf.NoTestKeyOwner) KeyOwner_Any else id
        if (hovered) {

            // Poll mouse buttons
            // - 'mouse_button_clicked' is generally carried into ActiveIdMouseButton when setting ActiveId.
            // - Technically we only need some values in one code path, but since this is gated by hovered test this is fine.
            var mouseButtonClicked = MouseButton.None
            var mouseButtonReleased = MouseButton.None
            for (buttonIndex in 0..2) {
                val button = MouseButton of buttonIndex
                if (flags has button.buttonFlags) { // Handle ImGuiButtonFlags_MouseButtonRight and ImGuiButtonFlags_MouseButtonMiddle here.
                    if (button.isClicked(testOwnerId) && mouseButtonClicked == MouseButton.None)
                        mouseButtonClicked = button
                    if (button.isReleased(testOwnerId) && mouseButtonClicked == MouseButton.None)
                        mouseButtonReleased = button
                }
            }

            // Process initial action
            if (flags hasnt Bf.NoKeyModifiers || (!g.io.keyCtrl && !g.io.keyShift && !g.io.keyAlt)) {

                if (mouseButtonClicked != MouseButton.None && g.activeId != id) {

                    if (flags hasnt Bf.NoSetKeyOwner)
                        mouseButtonClicked.key.setOwner(id)
                    if (flags has (Bf.PressedOnClickRelease or Bf.PressedOnClickReleaseAnywhere)) {
                        setActiveID(id, window)
                        g.activeIdMouseButton = mouseButtonClicked
                        if (flags hasnt Bf.NoNavFocus)
                            setFocusID(id, window)
                        focusWindow(window)
                    }
                    if (flags has Bf.PressedOnClick || (flags has Bf.PressedOnDoubleClick && io.mouseClickedCount[mouseButtonClicked.i] == 2)) {
                        pressed = true
                        if (flags has Bf.NoHoldingActiveId)
                            clearActiveID()
                        else
                            setActiveID(id, window) // Hold on ID
                        if (flags hasnt Bf.NoNavFocus)
                            setFocusID(id, window)
                        g.activeIdMouseButton = mouseButtonClicked
                        focusWindow(window)
                    }
                }
                if (flags has Bf.PressedOnRelease)
                    if (mouseButtonReleased != MouseButton.None) {
                        val hasRepeatedAtLeastOnce =
                                flags has Bf.Repeat && io.mouseDownDurationPrev[mouseButtonReleased.i] >= io.keyRepeatDelay // Repeat mode trumps on release behavior
                        if (!hasRepeatedAtLeastOnce)
                            pressed = true
                        if (flags hasnt Bf.NoNavFocus)
                            setFocusID(id, window)
                        clearActiveID()
                    }

                /*  'Repeat' mode acts when held regardless of _PressedOn flags (see table above).
                Relies on repeat logic of IsMouseClicked() but we may as well do it ourselves if we end up exposing
                finer RepeatDelay/RepeatRate settings.  */
                if (g.activeId == id && flags has Bf.Repeat)
                    if (io.mouseDownDuration[g.activeIdMouseButton.i] > 0f && g.activeIdMouseButton.isClicked(testOwnerId, InputFlag.Repeat))
                        pressed = true
            }

            if (pressed)
                g.navDisableHighlight = true
        }

        // Gamepad/Keyboard navigation
        // We report navigated item as hovered but we don't set g.HoveredId to not interfere with mouse.
        if (g.navId == id && !g.navDisableHighlight && g.navDisableMouseHover && (g.activeId == 0 || g.activeId == id || g.activeId == window.moveId))
            if (flags hasnt Bf.NoHoveredOnFocus)
                hovered = true
        if (g.navActivateDownId == id) {
            val navActivatedByCode = g.navActivateId == id
            var navActivatedByInputs = g.navActivatePressedId == id
            if (!navActivatedByInputs && flags has Bf.Repeat) {
                // Avoid pressing multiple keys from triggering excessive amount of repeat events
                val key1 = Key.Space.data
                val key2 = Key.Enter.data
                val key3 = Key._NavGamepadActivate.data
                val t1 = (key1.downDuration max key2.downDuration) max key3.downDuration
                navActivatedByInputs = calcTypematicRepeatAmount(t1 - g.io.deltaTime, t1, g.io.keyRepeatDelay, g.io.keyRepeatRate) > 0
            }
            if (navActivatedByCode || navActivatedByInputs) {
                // Set active id so it can be queried by user via IsItemActive(), equivalent of holding the mouse button.
                pressed = true
                setActiveID(id, window)
                g.activeIdSource = g.navInputSource
                if (flags hasnt Bf.NoNavFocus)
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
                if (mouseButton == MouseButton.None)
                // Fallback for the rare situation were g.ActiveId was set programmatically or from another widget (e.g. #6304).
                    clearActiveID()
                else if (mouseButton isDown testOwnerId)
                    held = true
                else {
                    val releaseIn = hovered && flags has Bf.PressedOnClickRelease
                    val releaseAnywhere = flags has Bf.PressedOnClickReleaseAnywhere
                    if ((releaseIn || releaseAnywhere) && !g.dragDropActive) {
                        // Report as pressed when releasing the mouse (this is the most common path)
                        val isDoubleClickRelease = flags has Bf.PressedOnDoubleClick && g.io.mouseReleased[mouseButton.i] && io.mouseClickedLastCount[mouseButton.i] == 2
                        val isRepeatingAlready = flags has Bf.Repeat && io.mouseDownDurationPrev[mouseButton.i] >= io.keyRepeatDelay // Repeat mode trumps <on release>
                        val isButtonAvailOrOwned = mouseButton.key testOwner testOwnerId
                        if (!isDoubleClickRelease && !isRepeatingAlready && isButtonAvailOrOwned) pressed = true
                    }
                    clearActiveID()
                }
                if (flags hasnt Bf.NoNavFocus) g.navDisableHighlight = true
            } else if (g.activeIdSource == InputSource.Keyboard || g.activeIdSource == InputSource.Gamepad)
            // When activated using Nav, we hold on the ActiveID until activation button is released
                if (g.navActivateDownId != id) clearActiveID()
            if (pressed) g.activeIdHasBeenPressedBefore = true
        }
        return booleanArrayOf(pressed, hovered, held)
    }

    fun <N> NumberOps<N>.dragBehavior(id: ID, pV: KMutableProperty0<N>, vSpeed: Float, min: N?, max: N?, format: String, flags: SliderFlags): Boolean where N : Number, N : Comparable<N> {
        if (g.activeId == id)
        // Those are the things we can do easily outside the DragBehaviorT<> template, saves code generation.
            if (g.activeIdSource == InputSource.Mouse && !g.io.mouseDown[0])
                clearActiveID()
            else if ((g.activeIdSource == InputSource.Keyboard || g.activeIdSource == InputSource.Gamepad) && g.navActivatePressedId == id && !g.activeIdIsJustActivated)
                clearActiveID()

        if (g.activeId != id) return false
        if (g.lastItemData.inFlags has ItemFlag.ReadOnly || flags has SliderFlag._ReadOnly) return false

        return fpOps.dragBehaviorT(pV, vSpeed, min ?: this.min, max ?: this.max, format, flags)
    }

    /** For 32-bits and larger types, slider bounds are limited to half the natural type range.
     *  So e.g. an integer Slider between INT_MAX-10 and INT_MAX will fail, but an integer Slider between INT_MAX/2-10 and INT_MAX/2 will be ok.
     *  It would be possible to lift that limitation with some work but it doesn't seem to be worth it for sliders. */
    fun sliderBehavior(bb: Rect, id: ID, pV: FloatArray, pMin: Float, pMax: Float, format: String, flags: SliderFlags, outGrabBb: Rect): Boolean =
            sliderBehavior(bb, id, pV mutablePropertyAt 0, pMin, pMax, format, flags, outGrabBb)

    //    fun <N> sliderBehavior(bb: Rect, id: ID,
    //                           v: KMutableProperty0<N>,
    //                           vMin: Float, vMax: Float,
    //                           format: String, power: Float,
    //                           flags: SliderFlags, outGrabBb: Rect): Boolean where N : Number, N : Comparable<N> =
    //            sliderBehavior(bb, id, DataType.Float, v, vMin, vMax, format, power, flags, outGrabBb)
    fun <N> NumberOps<N>.sliderBehavior(bb: Rect, id: ID, pV: KMutableProperty0<N>, min: N, max: N, format: String, flags: SliderFlags, outGrabBb: Rect): Boolean where N : Number, N : Comparable<N> = fpOps.sliderBehavior(bb, id, pV, min, max, format, flags, outGrabBb)

    fun <N, FP> NumberFpOps<N, FP>.sliderBehavior(bb: Rect, id: ID, pV: KMutableProperty0<N>, min: N, max: N, format: String, flags: SliderFlags, outGrabBb: Rect): Boolean where N : Number, N : Comparable<N>, FP : Number, FP : Comparable<FP> {
        // Those are the things we can do easily outside sliderBehaviorT
        if (g.lastItemData.inFlags has ItemFlag.ReadOnly || flags has SliderFlag._ReadOnly) return false

        // We allow the full range for bytes and shorts
        assert(isSmallerThanInt || min >= this.min / 2.coerced && max <= this.max / 2.coerced)
        return sliderBehaviorT(bb, id, pV, min, max, format, flags, outGrabBb)
    }

    /** Using 'hover_visibility_delay' allows us to hide the highlight and mouse cursor for a short time, which can be convenient to reduce visual noise. */
    fun splitterBehavior(bb: Rect, id: ID, axis: Axis, size1ptr: KMutableProperty0<Float>, size2ptr: KMutableProperty0<Float>, minSize1: Float, minSize2: Float, hoverExtend: Float = 0f, hoverVisibilityDelay: Float = 0f, bgCol: Int = 0): Boolean {

        var size1 by size1ptr
        var size2 by size2ptr
        val window = g.currentWindow!!

        if (!itemAdd(bb, id, null, ItemFlag.NoNav))
            return false

        val bbInteract = Rect(bb)
        bbInteract expand if (axis == Axis.Y) Vec2(0f, hoverExtend) else Vec2(hoverExtend, 0f)
        val (_, hovered, held) = buttonBehavior(bbInteract, id, Bf.FlattenChildren or Bf.AllowOverlap)
        if (hovered)
            g.lastItemData.statusFlags /= ItemStatusFlag.HoveredRect // for IsItemHovered(), because bb_interact is larger than bb
        if (g.activeId != id) // Because: we don't want to hover other while Active
            setItemAllowOverlap()

        if (held || (hovered && g.hoveredIdPreviousFrame == id && g.hoveredIdTimer >= hoverVisibilityDelay))
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
                size1 += mouseDelta
                size2 -= mouseDelta
                bbRender translate if (axis == Axis.X) Vec2(mouseDelta, 0f) else Vec2(0f, mouseDelta)
                markItemEdited(id)
            }
            bbRender translate if (axis == Axis.X) Vec2(mouseDelta, 0f) else Vec2(0f, mouseDelta)

            markItemEdited(id)
        }

        // Render at new position
        if (bgCol has COL32_A_MASK)
            window.drawList.addRectFilled(bbRender.min, bbRender.max, bgCol, 0f)
        val col = if (held) Col.SeparatorActive else if (hovered && g.hoveredIdTimer >= hoverVisibilityDelay) Col.SeparatorHovered else Col.Separator
        window.drawList.addRectFilled(bbRender.min, bbRender.max, col.u32, 0f)

        return held
    }

    fun treeNodeBehavior(id: ID, flags: TreeNodeFlags = none, label: String): Boolean =
            treeNodeBehavior(id, flags, label.toByteArray())

    fun treeNodeBehavior(id: ID, flags: TreeNodeFlags = none, label: ByteArray, labelEnd_: Int = -1): Boolean {

        val window = currentWindow
        if (window.skipItems)
            return false

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

        // Store a flag for the current depth to tell if we will allow closing this node when navigating one of its child.
        // For this purpose we essentially compare if g.NavIdIsAlive went from 0 to 1 between TreeNode() and TreePop().
        // This is currently only support 32 level deep and we are fine with (1 << Depth) overflowing into a zero.
        val isLeaf = flags has Tnf.Leaf
        var isOpen = treeNodeUpdateNextOpen(id, flags)
        if (isOpen && !g.navIdIsAlive && flags has Tnf.NavLeftJumpsBackHere && flags hasnt Tnf.NoTreePushOnOpen)
            window.dc.treeJumpToParentOnPopMask = window.dc.treeJumpToParentOnPopMask or (1 shl window.dc.treeDepth)

        val itemAdd = itemAdd(interactBb, id)
        g.lastItemData.statusFlags /= ItemStatusFlag.HasDisplayRect
        g.lastItemData.displayRect put frameBb

        if (!itemAdd) {
            if (isOpen && flags hasnt Tnf.NoTreePushOnOpen)
                treePushOverrideID(id)
            val f = if (isLeaf) none else ItemStatusFlag.Openable
            val f2 = if (isOpen) ItemStatusFlag.Opened else none
            IMGUI_TEST_ENGINE_ITEM_INFO(g.lastItemData.id, label.cStr, g.lastItemData.statusFlags or f or f2)
            return isOpen
        }

        var buttonFlags: ButtonFlags = none
        if (flags has Tnf.AllowOverlap)
            buttonFlags = buttonFlags or Bf.AllowOverlap
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
            isMouseXOverArrow -> Bf.PressedOnClick
            flags has Tnf.OpenOnDoubleClick -> Bf.PressedOnClickRelease or Bf.PressedOnDoubleClick
            else -> Bf.PressedOnClickRelease
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
                if (flags has Tnf.OpenOnDoubleClick && io.mouseClickedCount[0] == 2)
                    toggled = true
            } else if (pressed && g.dragDropHoldJustPressedId == id) {
                assert(buttonFlags has Bf.PressedOnDragDropHold)
                if (!isOpen) // When using Drag and Drop "hold to open" we keep the node highlighted after opening, but never close it again.
                    toggled = true
            }

            if (g.navId == id && g.navMoveDir == Dir.Left && isOpen) {
                toggled = true
                navClearPreferredPosForAxis(Axis.X)
                navMoveRequestCancel()
            }
            if (g.navId == id && g.navMoveDir == Dir.Right && !isOpen) { // If there's something upcoming on the line we may want to give it the priority?
                toggled = true
                navClearPreferredPosForAxis(Axis.X)
                navMoveRequestCancel()
            }
            if (toggled) {
                isOpen = !isOpen
                window.dc.stateStorage[id] = isOpen
                g.lastItemData.statusFlags /= ItemStatusFlag.ToggledOpen
            }
        }
        if (flags has Tnf.AllowOverlap && g.activeId != id) // Because: we don't want to hover other while Active
            setItemAllowOverlap()

        // In this branch, TreeNodeBehavior() cannot toggle the selection so this will never trigger.
        if (selected != wasSelected)
            g.lastItemData.statusFlags /= ItemStatusFlag.ToggledSelection

        // Render
        val textCol = Col.Text.u32
        val navHighlightFlags: NavHighlightFlags = NavHighlightFlag.TypeThin
        if (displayFrame) {
            // Framed type
            val bgCol = if (held && hovered) Col.HeaderActive else if (hovered) Col.HeaderHovered else Col.Header
            renderFrame(frameBb.min, frameBb.max, bgCol.u32, true, style.frameRounding)
            renderNavHighlight(frameBb, id, navHighlightFlags)
            if (flags has Tnf.Bullet)
                window.drawList.renderBullet(Vec2(textPos.x - textOffsetX * 0.6f, textPos.y + g.fontSize * 0.5f), textCol)
            else if (!isLeaf) {
                val dir = if (isOpen) if (flags has TreeNodeFlag.UpsideDownArrow) Dir.Up else Dir.Down else Dir.Right
                window.drawList.renderArrow(Vec2(textPos.x - textOffsetX + padding.x, textPos.y), textCol, dir, 1f)
            } else // Leaf without bullet, left-adjusted text
                textPos.x -= textOffsetX - padding.x
            if (flags has Tnf.ClipLabelForTrailingButton)
                frameBb.max.x -= g.fontSize + style.framePadding.x
            if (g.logEnabled)
                logSetNextTextDecoration("###", "###")
            renderTextClipped(textPos, frameBb.max, label, labelEnd, labelSize)
        } else {
            // Unframed typed for tree nodes
            if (hovered || selected) {
                val bgCol = if (held && hovered) Col.HeaderActive else if (hovered) Col.HeaderHovered else Col.Header
                renderFrame(frameBb.min, frameBb.max, bgCol.u32, false)
            }
            renderNavHighlight(frameBb, id, navHighlightFlags)
            if (flags has Tnf.Bullet)
                window.drawList.renderBullet(Vec2(textPos.x - textOffsetX * 0.5f, textPos.y + g.fontSize * 0.5f), textCol)
            else if (!isLeaf) {
                val dir = if (isOpen) if (flags has TreeNodeFlag.UpsideDownArrow) Dir.Up else Dir.Down else Dir.Right
                window.drawList.renderArrow(Vec2(textPos.x - textOffsetX + padding.x, textPos.y + g.fontSize * 0.15f), textCol, dir, 0.7f)
            }
            if (g.logEnabled)
                logSetNextTextDecoration(">", "")
            renderText(textPos, label, 0, labelEnd, false)
        }

        if (isOpen && flags hasnt Tnf.NoTreePushOnOpen)
            treePushOverrideID(id)
        val f = if (isLeaf) none else ItemStatusFlag.Openable
        val f2 = if (isOpen) ItemStatusFlag.Opened else none
        IMGUI_TEST_ENGINE_ITEM_INFO(id, label.cStr, g.lastItemData.statusFlags or f or f2)
        return isOpen
    }

    fun treePushOverrideID(id: ID) {
        val window = g.currentWindow!!
        indent()
        window.dc.treeDepth++
        pushOverrideID(id)
    }

    fun treeNodeSetOpen(id: ID, open: Boolean) {
        val storage = g.currentWindow!!.dc.stateStorage
        storage[id] = open
    }

    /** Return open state. Consume previous SetNextItemOpen() data, if any. May return true when logging. */
    fun treeNodeUpdateNextOpen(id: ID, flags: TreeNodeFlags): Boolean {

        if (flags has Tnf.Leaf) return true

        // We only write to the tree storage if the user clicks (or explicitly use the SetNextItemOpen function)
        val window = g.currentWindow!!
        val storage = window.dc.stateStorage

        var isOpen: Boolean
        if (g.nextItemData.flags has NextItemDataFlag.HasOpen) {
            if (g.nextItemData.openCond == Cond.Always) {
                isOpen = g.nextItemData.openVal
                treeNodeSetOpen(id, isOpen)
            } else {
                // We treat ImGuiCond_Once and ImGuiCond_FirstUseEver the same because tree node state are not saved persistently.
                val storedValue = storage.getOrElse(id) { -1 }
                if (storedValue == -1) {
                    isOpen = g.nextItemData.openVal
                    storage[id] = isOpen
                    treeNodeSetOpen(id, isOpen)
                } else isOpen = storedValue != 0
            }
        } else isOpen = storage[id] ?: (flags has Tnf.DefaultOpen)

        /*  When logging is enabled, we automatically expand tree nodes (but *NOT* collapsing headers.. seems like
            sensible behavior).
            NB- If we are above max depth we still allow manually opened nodes to be logged.    */
        if (g.logEnabled && flags hasnt Tnf.NoAutoOpenOnLog && (window.dc.treeDepth - g.logDepthRef) < g.logDepthToExpand) isOpen = true

        return isOpen
    }
}

inline fun <reified N> dragBehavior(id: ID, pV: KMutableProperty0<N>, vSpeed: Float, min: N?, max: N?, format: String, flags: SliderFlags): Boolean where N : Number, N : Comparable<N> =
        ImGui.dragBehavior(id, pV, vSpeed, min, max, format, flags)

inline fun <reified N> ImGui.dragBehavior(id: ID, pV: KMutableProperty0<N>, vSpeed: Float, min: N?, max: N?, format: String, flags: SliderFlags): Boolean where N : Number, N : Comparable<N> =
        numberFpOps<N, Nothing>().dragBehavior(id, pV, vSpeed, min, max, format, flags)

inline fun <reified N> sliderBehavior(bb: Rect, id: ID, pV: KMutableProperty0<N>, min: N, max: N, format: String, flags: SliderFlags, outGrabBb: Rect): Boolean where N : Number, N : Comparable<N> =
        ImGui.sliderBehavior(bb, id, pV, min, max, format, flags, outGrabBb)

inline fun <reified N> ImGui.sliderBehavior(bb: Rect, id: ID, pV: KMutableProperty0<N>, min: N, max: N, format: String, flags: SliderFlags, outGrabBb: Rect): Boolean where N : Number, N : Comparable<N> =
        numberFpOps<N, Nothing>().sliderBehavior(bb, id, pV, min, max, format, flags, outGrabBb)