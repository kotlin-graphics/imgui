package imgui.internal.api

import glm_.L
import glm_.f
import glm_.func.common.max
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.isDragging
import imgui.ImGui.renderArrow
import imgui.ImGui.startMouseMoving
import imgui.api.g
import imgui.internal.*
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.sections.*
import uno.kotlin.getValue
import uno.kotlin.setValue
import kotlin.math.max
import kotlin.reflect.KMutableProperty0

internal interface widgetsWindowDecorations {

    /* Button to close a window    */
    fun closeButton(id: ID, pos: Vec2): Boolean {

        val window = g.currentWindow!!

        // Tweak 1: Shrink hit-testing area if button covers an abnormally large proportion of the visible region. That's in order to facilitate moving the window away. (#3825)
        // This may better be applied as a general hit-rect reduction mechanism for all widgets to ensure the area to move window is always accessible?
        val bb = Rect(pos, pos + g.fontSize + ImGui.style.framePadding * 2f)
        val bbInteract = Rect(bb)
        val areaToVisibleRatio = window.outerRectClipped.area / bb.area
        if (areaToVisibleRatio < 1.5f)
            bbInteract expand floor(bbInteract.size * -0.25f)

        // Tweak 2: We intentionally allow interaction when clipped so that a mechanical Alt,Right,Activate sequence can always close a window.
        // (this isn't the regular behavior of buttons, but it doesn't affect the user much because navigation tends to keep items visible).
        val isClipped = !ImGui.itemAdd(bbInteract, id)

        val (pressed, hovered, held) = ImGui.buttonBehavior(bbInteract, id) // JVM, check bbInteract instance
        if (isClipped) return pressed

        // Render
        // FIXME: Clarify this mess
        val center = Vec2(bb.center)
        if (hovered) {
            val col = if (held) Col.ButtonActive else Col.ButtonHovered
            window.drawList.addCircleFilled(center, 2f max (g.fontSize * 0.5f + 1f), col.u32)
        }

        val crossExtent = g.fontSize * 0.5f * 0.7071f - 1f
        val crossCol = Col.Text.u32
        center -= 0.5f
        window.drawList.addLine(center + crossExtent, center - crossExtent, crossCol, 1f)
        window.drawList.addLine(center + Vec2(crossExtent, -crossExtent), center + Vec2(-crossExtent, crossExtent), crossCol, 1f)

        return pressed
    }

    fun collapseButton(id: ID, pos: Vec2): Boolean {
        val window = g.currentWindow!!
        val bb = Rect(pos, pos + g.fontSize + ImGui.style.framePadding * 2f)
        ImGui.itemAdd(bb, id)
        val (pressed, hovered, held) = ImGui.buttonBehavior(bb, id)

        // Render
        val bgCol = if (held && hovered) Col.ButtonActive else if (hovered) Col.ButtonHovered else Col.Button
        val textCol = Col.Text
        if (hovered || held)
            window.drawList.addCircleFilled(bb.center/* + Vec2(0.0f, -0.5f)*/, g.fontSize * 0.5f + 1f, bgCol.u32)
        window.drawList.renderArrow(bb.min + ImGui.style.framePadding, textCol.u32, if (window.collapsed) Dir.Right else Dir.Down, 1f)

        // Switch to moving the window after mouse is moved beyond the initial drag threshold
        if (ImGui.isItemActive && MouseButton.Left.isDragging())
            window.startMouseMoving()

        return pressed
    }

    /** Vertical scrollbar
     *  The entire piece of code below is rather confusing because:
     *  - We handle absolute seeking (when first clicking outside the grab) and relative manipulation (afterward or when
     *          clicking inside the grab)
     *  - We store values as normalized ratio and in a form that allows the window content to change while we are holding on
     *          a scrollbar
     *  - We handle both horizontal and vertical scrollbars, which makes the terminology not ideal. */
    infix fun scrollbar(axis: Axis) {

        val window = g.currentWindow!!
        val id = window getScrollbarID axis

        // Calculate scrollbar bounding box
        val bb = window getScrollbarRect axis
        var roundingCorners: DrawFlags = DrawFlag.RoundCornersNone
        if (axis == Axis.X) {
            roundingCorners = roundingCorners or DrawFlag.RoundCornersBottomLeft
            if (!window.scrollbar.y)
                roundingCorners = roundingCorners or DrawFlag.RoundCornersBottomLeft
        } else {
            if (window.flags has WindowFlag.NoTitleBar && window.flags hasnt WindowFlag.MenuBar)
                roundingCorners = roundingCorners or DrawFlag.RoundCornersTopRight
            if (!window.scrollbar.x)
                roundingCorners = roundingCorners or DrawFlag.RoundCornersBottomRight
        }
        val sizeAvail = window.innerRect.max[axis] - window.innerRect.min[axis]
        val sizeContents = window.contentSize[axis] + window.windowPadding[axis] * 2f
        scrollbarEx(bb, id, axis, (window.scroll mutablePropertyAt axis).L, sizeAvail.L, sizeContents.L, roundingCorners)
    }

    /** Vertical/Horizontal scrollbar
     *  The entire piece of code below is rather confusing because:
     *  - We handle absolute seeking (when first clicking outside the grab) and relative manipulation (afterward or when clicking inside the grab)
     *  - We store values as normalized ratio and in a form that allows the window content to change while we are holding on a scrollbar
     *  - We handle both horizontal and vertical scrollbars, which makes the terminology not ideal.
     *  Still, the code should probably be made simpler..   */
    fun scrollbarEx(bbFrame: Rect, id: ID, axis: Axis, pScrollV: KMutableProperty0<Long>, sizeAvailV: Long, sizeContentsV: Long, flags: DrawFlags): Boolean {

        var scrollV by pScrollV

        val window = g.currentWindow!!
        if (window.skipItems)
            return false

        val bbFrameWidth = bbFrame.width
        val bbFrameHeight = bbFrame.height
        if (bbFrameWidth <= 0f || bbFrameHeight <= 0f)
            return false

        // When we are too small, start hiding and disabling the grab (this reduce visual noise on very small window and facilitate using the window resize grab)
        var alpha = 1f
        if (axis == Axis.Y && bbFrameHeight < g.fontSize + ImGui.style.framePadding.y * 2f)
            alpha = saturate((bbFrameHeight - g.fontSize) / (ImGui.style.framePadding.y * 2f))
        if (alpha <= 0f)
            return false

        val allowInteraction = alpha >= 1f

        val bb = Rect(bbFrame)
        bb.expand(Vec2(-clamp(floor((bbFrameWidth - 2f) * 0.5f), 0f, 3f), -clamp(floor((bbFrameHeight - 2f) * 0.5f), 0f, 3f)))

        // V denote the main, longer axis of the scrollbar (= height for a vertical scrollbar)
        val scrollbarSizeV = if (axis == Axis.X) bb.width else bb.height

        // Calculate the height of our grabbable box. It generally represent the amount visible (vs the total scrollable amount)
        // But we maintain a minimum size in pixel to allow for the user to still aim inside.
        assert(max(sizeContentsV, sizeAvailV) > 0f) { "Adding this assert to check if the ImMax(XXX,1.0f) is still needed. PLEASE CONTACT ME if this triggers." }
        val winSizeV = max(sizeContentsV max sizeAvailV, 1L)
        val grabHPixels = clamp(scrollbarSizeV * (sizeAvailV.f / winSizeV.f), ImGui.style.grabMinSize, scrollbarSizeV)
        val grabHNorm = grabHPixels / scrollbarSizeV

        // Handle input right away. None of the code of Begin() is relying on scrolling position before calling Scrollbar().
        ImGui.itemAdd(bbFrame, id, null, ItemFlag.NoNav)
        val (_, hovered, held) = ImGui.buttonBehavior(bb, id, ButtonFlag.NoNavFocus)

        val scrollMax = max(1L, sizeContentsV - sizeAvailV)
        var scrollRatio = saturate(scrollV.f / scrollMax.f)
        var grabVNorm = scrollRatio * (scrollbarSizeV - grabHPixels) / scrollbarSizeV  // Grab position in normalized space
        if (held && allowInteraction && grabHNorm < 1f) {
            val scrollbarPosV = bb.min[axis]
            val mousePosV = ImGui.io.mousePos[axis]

            // Click position in scrollbar normalized space (0.0f->1.0f)
            val clickedVNorm = saturate((mousePosV - scrollbarPosV) / scrollbarSizeV)
            ImGui.hoveredId = id

            var seekAbsolute = false
            if (g.activeIdIsJustActivated) {
                // On initial click calculate the distance between mouse and the center of the grab
                seekAbsolute = (clickedVNorm < grabVNorm || clickedVNorm > grabVNorm + grabHNorm)
                g.scrollbarClickDeltaToGrabCenter = when {
                    seekAbsolute -> 0f
                    else -> clickedVNorm - grabVNorm - grabHNorm * 0.5f
                }
            }

            // Apply scroll (p_scroll_v will generally point on one member of window->Scroll)
            // It is ok to modify Scroll here because we are being called in Begin() after the calculation of ContentSize and before setting up our starting position
            val scrollVNorm = saturate((clickedVNorm - g.scrollbarClickDeltaToGrabCenter - grabHNorm * 0.5f) / (1f - grabHNorm))
            scrollV = (scrollVNorm * scrollMax).L

            // Update values for rendering
            scrollRatio = saturate(scrollV.f / scrollMax.f)
            grabVNorm = scrollRatio * (scrollbarSizeV - grabHPixels) / scrollbarSizeV

            // Update distance to grab now that we have seeked and saturated
            if (seekAbsolute)
                g.scrollbarClickDeltaToGrabCenter = clickedVNorm - grabVNorm - grabHNorm * 0.5f
        }

        // Render
        val bgCol = Col.ScrollbarBg.u32
        val grabCol = ImGui.getColorU32(when {
                                            held -> Col.ScrollbarGrabActive
                                            hovered -> Col.ScrollbarGrabHovered
                                            else -> Col.ScrollbarGrab
                                        }, alpha)
        window.drawList.addRectFilled(bbFrame.min, bbFrame.max, bgCol, window.windowRounding, flags)
        val grabRect = when (axis) {
            Axis.X -> Rect(lerp(bb.min.x, bb.max.x, grabVNorm), bb.min.y, lerp(bb.min.x, bb.max.x, grabVNorm) + grabHPixels, bb.max.y)
            else -> Rect(bb.min.x, lerp(bb.min.y, bb.max.y, grabVNorm), bb.max.x, lerp(bb.min.y, bb.max.y, grabVNorm) + grabHPixels)
        }
        window.drawList.addRectFilled(grabRect.min, grabRect.max, grabCol, ImGui.style.scrollbarRounding)

        return held
    }

    /** Return scrollbar rectangle, must only be called for corresponding axis if window->ScrollbarX/Y is set.
     *  ~GetWindowScrollbarRect     */
    infix fun Window.getScrollbarRect(axis: Axis): Rect {
        val outerRect = rect() //        val innerRect = innerRect
        val borderSize = windowBorderSize
        val scrollbarSize =
            scrollbarSizes[axis xor 1] // (ScrollbarSizes.x = width of Y scrollbar; ScrollbarSizes.y = height of X scrollbar)
        assert(scrollbarSize > 0f)
        return when (axis) {
            Axis.X -> Rect(innerRect.min.x,
                           max(outerRect.min.y, outerRect.max.y - borderSize - scrollbarSize),
                           innerRect.max.x,
                           outerRect.max.y)
            else -> Rect(max(outerRect.min.x, outerRect.max.x - borderSize - scrollbarSize),
                         innerRect.min.y,
                         outerRect.max.x,
                         innerRect.max.y)
        }
    }

    /** ~GetWindowScrollbarID */
    infix fun Window.getScrollbarID(axis: Axis): ID = getID(if (axis == Axis.X) "#SCROLLX" else "#SCROLLY")

    /** ~GetWindowResizeCornerID
     *
     *  0..3: corners (Lower-right, Lower-left, Unused, Unused) */
    infix fun Window.getResizeCornerID(n: Int): ID {
        assert(n in 0..3)
        var id = this.id
        id = hashStr("#RESIZE", 0, id)
        id = hashData(n, id)
        return id
    }

    /** ~GetWindowResizeBorderID
     *
     *  Borders (Left, Right, Up, Down) */
    infix fun Window.getResizeBorderID(dir: Dir): ID {
        assert(dir.i in 0..3)
        val n = dir.i + 4
        var id = this.id
        id = hashStr("#RESIZE", 0, id)
        id = hashData(n, id)
        return id
    }
}