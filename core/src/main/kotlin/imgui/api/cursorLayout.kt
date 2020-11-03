package imgui.api

import glm_.f
import glm_.glm
import glm_.max
import glm_.vec2.Vec2
import imgui.ImGui.currentWindow
import imgui.ImGui.currentWindowRead
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.separatorEx
import imgui.ImGui.style
import imgui.internal.classes.GroupData
import imgui.internal.classes.Rect
import imgui.internal.sections.ItemStatusFlag
import imgui.internal.sections.SeparatorFlag
import imgui.internal.sections.or
import imgui.internal.sections.LayoutType as Lt


/** Cursor / Layout
 *  - By "cursor" we mean the current output position.
 *  - The typical widget behavior is to output themselves at the current cursor position, then move the cursor one line down.
 *  - You can call SameLine() between widgets to undo the last carriage return and output at the right of the preceding widget.
 *  - Attention! We currently have inconsistencies between window-local and absolute positions we will aim to fix with future API:
 *    Window-local coordinates:   SameLine(), GetCursorPos(), SetCursorPos(), GetCursorStartPos(), GetContentRegionMax(), GetWindowContentRegion*(), PushTextWrapPos()
 *    Absolute coordinate:        GetCursorScreenPos(), SetCursorScreenPos(), all ImDrawList:: functions.   */
interface cursorLayout {

    /** Vertical separator, for menu bars (use current line height). not exposed because it is misleading
     *  what it doesn't have an effect on regular layout.   */
    fun separator() {
        val window = g.currentWindow!!
        if (window.skipItems)
            return

        // Those flags should eventually be overridable by the user
        val flags = if (window.dc.layoutType == Lt.Horizontal) SeparatorFlag.Vertical else SeparatorFlag.Horizontal
        separatorEx(flags or SeparatorFlag.SpanAllColumns)
    }

    fun sameLine(offsetFromStartX: Int, spacing: Int = -1) = sameLine(offsetFromStartX.f, spacing.f)

    /** Call between widgets or groups to layout them horizontally. X position given in window coordinates.
     *  Gets back to previous line and continue with horizontal layout
     *      offset_from_start_x == 0 : follow right after previous item
     *      offset_from_start_x != 0 : align to specified x position (relative to window/group left)
     *      spacing_w < 0            : use default spacing if pos_x == 0, no spacing if pos_x != 0
     *      spacing_w >= 0           : enforce spacing amount    */
    fun sameLine(offsetFromStartX: Float = 0f, spacing: Float = -1f) {

        val window = currentWindow
        if (window.skipItems) return

        with(window) {
            dc.cursorPos.put(
                    if (offsetFromStartX != 0f)
                        pos.x - scroll.x + offsetFromStartX + glm.max(0f, spacing) + dc.groupOffset + dc.columnsOffset
                    else
                        dc.cursorPosPrevLine.x + if (spacing < 0f) style.itemSpacing.x else spacing
                    , dc.cursorPosPrevLine.y)
            dc.currLineSize.y = dc.prevLineSize.y
            dc.currLineTextBaseOffset = dc.prevLineTextBaseOffset
        }
    }

    /** undo a sameLine() or force a new line when in an horizontal-layout context.   */
    fun newLine() {
        val window = currentWindow
        if (window.skipItems) return

        val backupLayoutType = window.dc.layoutType
        window.dc.layoutType = Lt.Vertical
        // In the event that we are on a line with items that is smaller that FontSize high, we will preserve its height.
        itemSize(Vec2(0f, if (window.dc.currLineSize.y > 0f) 0f else g.fontSize))
        window.dc.layoutType = backupLayoutType
    }

    /** add vertical spacing.    */
    fun spacing() {
        if (currentWindow.skipItems) return
        itemSize(Vec2())
    }

    /** add a dummy item of given size. unlike InvisibleButton(), Dummy() won't take the mouse click or be navigable into.  */
    fun dummy(size: Vec2) {

        val window = currentWindow
        if (window.skipItems) return

        val bb = Rect(window.dc.cursorPos, window.dc.cursorPos + size)
        itemSize(size)
        itemAdd(bb, 0)
    }

    /** move content position toward the right, by style.indentSpacing or indentW if != 0    */
    fun indent(indentW: Float = 0f) = with(currentWindow) {
        dc.indent += if (indentW != 0f) indentW else style.indentSpacing
        dc.cursorPos.x = pos.x + dc.indent + dc.columnsOffset
    }

    /** move content position back to the left, by style.IndentSpacing or indentW if != 0    */
    fun unindent(indentW: Float = 0f) = with(currentWindow) {
        dc.indent -= if (indentW != 0f) indentW else style.indentSpacing
        dc.cursorPos.x = pos.x + dc.indent + dc.columnsOffset
    }

    /** Lock horizontal starting position + capture group bounding box into one "item" (so you can use IsItemHovered()
     *  or layout primitives such as SameLine() on whole group, etc.)   */
    fun beginGroup() {

        with(g.currentWindow!!) {
            dc.groupStack.add(
                    GroupData().apply {
                        backupCursorPos put dc.cursorPos
                        backupCursorMaxPos put dc.cursorMaxPos
                        backupIndent = dc.indent
                        backupGroupOffset = dc.groupOffset
                        backupCurrLineSize put dc.currLineSize
                        backupCurrLineTextBaseOffset = dc.currLineTextBaseOffset
                        backupActiveIdIsAlive = g.activeIdIsAlive
                        backupActiveIdPreviousFrameIsAlive = g.activeIdPreviousFrameIsAlive
                        emitItem = true
                    })
            dc.groupOffset = dc.cursorPos.x - pos.x - dc.columnsOffset
            dc.indent = dc.groupOffset
            dc.cursorMaxPos put dc.cursorPos
            dc.currLineSize.y = 0f
            if (g.logEnabled)
                g.logLinePosY = -Float.MAX_VALUE// To enforce Log carriage return
        }
    }

    /** unlock horizontal starting position + capture the whole group bounding box into one "item"
     *  (so you can use IsItemHovered() or layout primitives such as SameLine() on whole group, etc.) */
    fun endGroup() {

        val window = g.currentWindow!!
        assert(window.dc.groupStack.isNotEmpty()) { "Mismatched BeginGroup()/EndGroup() calls" }

        val groupData = window.dc.groupStack.last()

        val groupBb = Rect(groupData.backupCursorPos, window.dc.cursorMaxPos max groupData.backupCursorPos)

        with(window.dc) {
            cursorPos put groupData.backupCursorPos
            cursorMaxPos put glm.max(groupData.backupCursorMaxPos, cursorMaxPos)
            indent = groupData.backupIndent
            groupOffset = groupData.backupGroupOffset
            currLineSize put groupData.backupCurrLineSize
            currLineTextBaseOffset = groupData.backupCurrLineTextBaseOffset
            if (g.logEnabled)
                g.logLinePosY = -Float.MAX_VALUE // To enforce Log carriage return
        }

        if (!groupData.emitItem) {
            window.dc.groupStack.pop()
            return
        }

        window.dc.currLineTextBaseOffset = window.dc.prevLineTextBaseOffset max groupData.backupCurrLineTextBaseOffset      // FIXME: Incorrect, we should grab the base offset from the *first line* of the group but it is hard to obtain now.
        itemSize(groupBb.size)
        itemAdd(groupBb, 0)

        // If the current ActiveId was declared within the boundary of our group, we copy it to LastItemId so IsItemActive(), IsItemDeactivated() etc. will be functional on the entire group.
        // It would be be neater if we replaced window.DC.LastItemId by e.g. 'bool LastItemIsActive', but would put a little more burden on individual widgets.
        // Also if you grep for LastItemId you'll notice it is only used in that context.
        // (The tests not symmetrical because ActiveIdIsAlive is an ID itself, in order to be able to handle ActiveId being overwritten during the frame.)
        // (The two tests not the same because ActiveIdIsAlive is an ID itself, in order to be able to handle ActiveId being overwritten during the frame.)
        val groupContainsCurrActiveId = groupData.backupActiveIdIsAlive != g.activeId && g.activeIdIsAlive == g.activeId && g.activeId != 0
        val groupContainsPrevActiveId = !groupData.backupActiveIdPreviousFrameIsAlive && g.activeIdPreviousFrameIsAlive
        if (groupContainsCurrActiveId)
            window.dc.lastItemId = g.activeId
        else if (groupContainsPrevActiveId)
            window.dc.lastItemId = g.activeIdPreviousFrame
        window.dc.lastItemRect put groupBb

        // Forward Edited flag
        if (groupContainsCurrActiveId && g.activeIdHasBeenEditedThisFrame)
            window.dc.lastItemStatusFlags = window.dc.lastItemStatusFlags or ItemStatusFlag.Edited

        // Forward Deactivated flag
        window.dc.lastItemStatusFlags = window.dc.lastItemStatusFlags or ItemStatusFlag.HasDeactivated
        if (groupContainsPrevActiveId && g.activeId != g.activeIdPreviousFrame)
            window.dc.lastItemStatusFlags = window.dc.lastItemStatusFlags or ItemStatusFlag.Deactivated

        window.dc.groupStack.pop()
        //window->DrawList->AddRect(groupBb.Min, groupBb.Max, IM_COL32(255,0,255,255));   // [Debug]
    }

    var cursorPos: Vec2
        /** cursor position in window coordinates (relative to window position)
         *
         *  Cursor position is relative to window position
         *  User generally sees positions in window coordinates. Internally we store CursorPos in absolute screen coordinates
         *  because it is more convenient.
         *  Conversion happens as we pass the value to user, but it makes our naming convention confusing because
         *  cursorPos == dc.cursorPos - window.pos. May want to rename 'dc.cursorPos'.
         *  ~GetCursorPos   */
        get() = with(currentWindowRead!!) { dc.cursorPos - pos + scroll }
        /** are using the main, absolute coordinate system.
         *  ~SetCursorPos   */
        set(value) = with(currentWindowRead!!) {
            dc.cursorPos put (pos - scroll + value)
            dc.cursorMaxPos = glm.max(dc.cursorMaxPos, dc.cursorPos)
        }

    var cursorPosX: Float
        /** cursor position is relative to window position
         *  (some functions are using window-relative coordinates, such as: GetCursorPos, GetCursorStartPos, GetContentRegionMax, GetWindowContentRegion* etc.
         *  ~GetCursorPosX  */
        get() = with(currentWindowRead!!) { dc.cursorPos.x - pos.x + scroll.x }
        /** GetWindowPos() + GetCursorPos() == GetCursorScreenPos() etc.)
         *  ~SetCursorPosX  */
        set(value) = with(currentWindowRead!!) {
            dc.cursorPos.x = pos.x - scroll.x + value
            dc.cursorMaxPos.x = glm.max(dc.cursorMaxPos.x, dc.cursorPos.x)
        }

    var cursorPosY: Float
        /** cursor position is relative to window position
         *  other functions such as GetCursorScreenPos or everything in ImDrawList::
         *  ~GetCursorPosY  */
        get() = with(currentWindowRead!!) { dc.cursorPos.y - pos.y + scroll.y }
        /** ~SetCursorPosY */
        set(value) = with(currentWindowRead!!) {
            dc.cursorPos.y = pos.y - scroll.y + value
            dc.cursorMaxPos.y = glm.max(dc.cursorMaxPos.y, dc.cursorPos.y)
        }

    /** initial cursor position in window coordinates
     *  ~GetCursorStartPos */
    val cursorStartPos: Vec2
        get() = with(currentWindowRead!!) { dc.cursorStartPos - pos }

    /** cursor position in absolute screen coordinates [0..io.DisplaySize] (useful to work with ImDrawList API) */
    var cursorScreenPos: Vec2
        /** ~GetCursorScreenPos */
        get() = currentWindowRead!!.dc.cursorPos
        /** ~SetCursorScreenPos */
        set(value) = with(currentWindow.dc) {
            cursorPos put value
            cursorMaxPos maxAssign cursorPos
        }

    /** Vertically align/lower upcoming text to framePadding.y so that it will aligns to upcoming widgets
     *  (call if you have text on a line before regular widgets)    */
    fun alignTextToFramePadding() {
        val window = currentWindow
        if (window.skipItems) return
        window.dc.currLineSize.y = glm.max(window.dc.currLineSize.y, g.fontSize + style.framePadding.y * 2)
        window.dc.currLineTextBaseOffset = glm.max(window.dc.currLineTextBaseOffset, style.framePadding.y)
    }

    /** ~ FontSize
     *  ~GetTextLineHeight  */
    val textLineHeight: Float
        get() = g.fontSize

    /** ~ FontSize + style.ItemSpacing.y (distance in pixels between 2 consecutive lines of text)
     *  ~GetTextLineHeightWithSpacing   */
    val textLineHeightWithSpacing: Float
        get() = g.fontSize + style.itemSpacing.y

    /** ~ FontSize + style.FramePadding.y * 2
     *  ~GetFrameHeight */
    val frameHeight: Float
        get() = g.fontSize + style.framePadding.y * 2f

    /** distance (in pixels) between 2 consecutive lines of standard height widgets ==
     *  GetWindowFontSize() + GetStyle().FramePadding.y*2 + GetStyle().ItemSpacing.y
     *  ~GetFrameHeightWithSpacing  */
    val frameHeightWithSpacing: Float
        get() = g.fontSize + style.framePadding.y * 2f + style.itemSpacing.y

}