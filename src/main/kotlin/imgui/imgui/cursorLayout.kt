package imgui.imgui

import glm_.f
import glm_.glm
import glm_.vec2.Vec2
import imgui.ImGui.currentWindow
import imgui.ImGui.currentWindowRead
import imgui.ImGui.itemAdd
import imgui.ImGui.itemSize
import imgui.ImGui.style
import imgui.g
import imgui.internal.GroupData
import imgui.internal.Rect
import imgui.internal.LayoutType as Lt
import imgui.internal.SeparatorFlag as Sf

interface imgui_cursorLayout {


    /** Call between widgets or groups to layout them horizontally. X position given in window coordinates.
     *  Gets back to previous line and continue with horizontal layout
     *      localPosX == 0      : follow right after previous item
     *      localPosX != 0      : align to specified x position (relative to window/group left)
     *      spacingW < 0   : use default spacing if localPosX == 0, no spacing if localPosX != 0
     *      spacingW >= 0  : enforce spacing amount    */
    fun sameLine(localPosX: Int) = sameLine(localPosX, 1)

    fun sameLine(localPosX: Int, spacingW: Int) = sameLine(localPosX.f, spacingW.f)

    fun sameLine() = sameLine(0f, -1f)

    fun sameLine(localPosX: Float = 0f, spacingW: Float = -1f) {

        val window = currentWindow
        if (window.skipItems) return

        with(window) {
            dc.cursorPos.put(
                    if (localPosX != 0f)
                        pos.x - scroll.x + localPosX + glm.max(0f, spacingW) + dc.groupOffset + dc.columnsOffset
                    else
                        dc.cursorPosPrevLine.x + if (spacingW < 0f) style.itemSpacing.x else spacingW
                    , dc.cursorPosPrevLine.y)
            dc.currentLineSize.y = dc.prevLineSize.y
            dc.currentLineTextBaseOffset = dc.prevLineTextBaseOffset
        }
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

        with(currentWindow) {
            dc.groupStack.add(
                    GroupData().apply {
                        backupCursorPos put dc.cursorPos
                        backupCursorMaxPos put dc.cursorMaxPos
                        backupIndent = dc.indent
                        backupGroupOffset = dc.groupOffset
                        backupCurrentLineSize put dc.currentLineSize
                        backupCurrentLineTextBaseOffset = dc.currentLineTextBaseOffset
                        backupActiveIdIsAlive = g.activeIdIsAlive
                        backupActiveIdPreviousFrameIsAlive = g.activeIdPreviousFrameIsAlive
                        advanceCursor = true
                    })
            dc.groupOffset = dc.cursorPos.x - pos.x - dc.columnsOffset
            dc.indent = dc.groupOffset
            dc.cursorMaxPos put dc.cursorPos
            dc.currentLineSize.y = 0f
            if (g.logEnabled)
                g.logLinePosY = -Float.MAX_VALUE;// To enforce Log carriage return
        }
    }

    /** unlock horizontal starting position + capture the whole group bounding box into one "item"
     *  (so you can use IsItemHovered() or layout primitives such as SameLine() on whole group, etc.) */
    fun endGroup() {

        val window = currentWindow
        assert(window.dc.groupStack.isNotEmpty()) { "Mismatched BeginGroup()/EndGroup() calls" }

        val groupData = window.dc.groupStack.last()

        val groupBb = Rect(groupData.backupCursorPos, window.dc.cursorMaxPos)
        groupBb.max = glm.max(groupBb.min, groupBb.max)

        with(window.dc) {
            cursorPos put groupData.backupCursorPos
            cursorMaxPos put glm.max(groupData.backupCursorMaxPos, cursorMaxPos)
            indent = groupData.backupIndent
            groupOffset = groupData.backupGroupOffset
            currentLineSize put groupData.backupCurrentLineSize
            currentLineTextBaseOffset = groupData.backupCurrentLineTextBaseOffset
            if (g.logEnabled)
                g.logLinePosY = -Float.MAX_VALUE // To enforce Log carriage return
        }

        if (groupData.advanceCursor) {
            window.dc.currentLineTextBaseOffset = glm.max(window.dc.prevLineTextBaseOffset, groupData.backupCurrentLineTextBaseOffset)      // FIXME: Incorrect, we should grab the base offset from the *first line* of the group but it is hard to obtain now.
            itemSize(groupBb.size, 0f)
            itemAdd(groupBb, 0)
        }

        /*  If the current ActiveId was declared within the boundary of our group, we copy it to ::lastItemId so ::isItemActive,
            ::isItemDeactivated etc. will be functional on the entire group.
            It would be be neater if we replaced window.dc.lastItemId by e.g. 'lastItemIsActive: Boolean',
            but would put a little more burden on individual widgets.
            (and if you grep for LastItemId you'll notice it is only used in that context.    */
        if (groupData.backupActiveIdIsAlive != g.activeId && g.activeIdIsAlive == g.activeId && g.activeId != 0) // && g.ActiveIdWindow->RootWindow == window->RootWindow)
            window.dc.lastItemId = g.activeId
        else if (!groupData.backupActiveIdPreviousFrameIsAlive && g.activeIdPreviousFrameIsAlive) // && g.ActiveIdPreviousFrameWindow->RootWindow == window->RootWindow)
            window.dc.lastItemId = g.activeIdPreviousFrame
        window.dc.lastItemRect put groupBb
        window.dc.groupStack.pop() // TODO last() on top -> pop?

        //window->DrawList->AddRect(groupBb.Min, groupBb.Max, IM_COL32(255,0,255,255));   // [Debug]
    }

    var cursorPos
        /** cursor position in window coordinates (relative to window position)
         *
         *  Cursor position is relative to window position
         *  User generally sees positions in window coordinates. Internally we store CursorPos in absolute screen coordinates
         *  because it is more convenient.
         *  Conversion happens as we pass the value to user, but it makes our naming convention confusing because
         *  cursorPos == dc.cursorPos - window.pos. May want to rename 'dc.cursorPos'.*/
        get() = with(currentWindowRead!!) { dc.cursorPos - pos + scroll }
        /** are using the main, absolute coordinate system.         */
        set(value) = with(currentWindowRead!!) {
            dc.cursorPos put (pos - scroll + value)
            dc.cursorMaxPos = glm.max(dc.cursorMaxPos, dc.cursorPos)
        }

    var cursorPosX
        /** cursor position is relative to window position
         *  (some functions are using window-relative coordinates, such as: GetCursorPos, GetCursorStartPos, GetContentRegionMax, GetWindowContentRegion* etc. */
        get() = with(currentWindowRead!!) { dc.cursorPos.x - pos.x + scroll.x }
        /** GetWindowPos() + GetCursorPos() == GetCursorScreenPos() etc.) */
        set(value) = with(currentWindowRead!!) {
            dc.cursorPos.x = pos.x - scroll.x + value
            dc.cursorMaxPos.x = glm.max(dc.cursorMaxPos.x, dc.cursorPos.x)
        }

    var cursorPosY
        /** cursor position is relative to window position
         *  other functions such as GetCursorScreenPos or everything in ImDrawList:: */
        get() = with(currentWindowRead!!) { dc.cursorPos.y - pos.y + scroll.y }
        set(value) = with(currentWindowRead!!) {
            dc.cursorPos.y = pos.y - scroll.y + value
            dc.cursorMaxPos.y = glm.max(dc.cursorMaxPos.y, dc.cursorPos.y)
        }

    /** initial cursor position in window coordinates */
    val cursorStartPos: Vec2
        get() = with(currentWindowRead!!) { dc.cursorStartPos - pos }

    /** cursor position in absolute screen coordinates [0..io.DisplaySize] (useful to work with ImDrawList API) */
    var cursorScreenPos: Vec2
        get() = currentWindowRead!!.dc.cursorPos
        set(value) = with(currentWindow.dc) {
            cursorPos put value
            cursorMaxPos maxAssign cursorPos
        }

    /** ~ FontSize   */
    val textLineHeight get() = g.fontSize

    /** ~ FontSize + style.ItemSpacing.y (distance in pixels between 2 consecutive lines of text)  */
    val textLineHeightWithSpacing get() = g.fontSize + style.itemSpacing.y

    /** ~ FontSize + style.FramePadding.y * 2 */
    val frameHeight get() = g.fontSize + style.framePadding.y * 2f

    /** distance (in pixels) between 2 consecutive lines of standard height widgets ==
     *  GetWindowFontSize() + GetStyle().FramePadding.y*2 + GetStyle().ItemSpacing.y    */
    val frameHeightWithSpacing get() = g.fontSize + style.framePadding.y * 2f + style.itemSpacing.y

}