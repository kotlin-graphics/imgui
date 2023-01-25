package imgui.api

import gli_.has
import glm_.max
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.closeButton
import imgui.ImGui.currentWindow
import imgui.ImGui.getIDWithSeed
import imgui.ImGui.indent
import imgui.ImGui.navMoveRequestButNoResultYet
import imgui.ImGui.navMoveRequestCancel
import imgui.ImGui.popID
import imgui.ImGui.pushID
import imgui.ImGui.setNavID
import imgui.ImGui.style
import imgui.ImGui.treeNodeBehavior
import imgui.ImGui.unindent
import imgui.internal.classes.Rect
import imgui.internal.sections.NextItemDataFlag
import imgui.internal.formatString
import imgui.internal.sections.or
import kotlin.reflect.KMutableProperty0
import imgui.TreeNodeFlag as Tnf

/** Widgets: Trees
 *  - TreeNode functions return true when the node is open, in which case you need to also call TreePop() when you are finished displaying the tree node contents. */
interface widgetsTrees {

    /** if returning 'true' the node is open and the tree id is pushed into the id stack. user is responsible for
     *  calling TreePop().  */
    fun treeNode(label: String): Boolean {
        val window = currentWindow
        if (window.skipItems) return false
        return treeNodeBehavior(window.getID(label), 0, label)
    }

    /** read the FAQ about why and how to use ID. to align arbitrary text at the same level as a TreeNode() you can use
     *  Bullet().   */
    fun treeNode(strID: String, fmt: String, vararg args: Any): Boolean = treeNodeEx(strID, 0, fmt, *args)

    /** read the FAQ about why and how to use ID. to align arbitrary text at the same level as a TreeNode() you can use
     *  Bullet().   */
    fun treeNode(ptrID: Any, fmt: String, vararg args: Any): Boolean = treeNodeEx(ptrID, 0, fmt, *args)

    /** read the FAQ about why and how to use ID. to align arbitrary text at the same level as a TreeNode() you can use
     *  Bullet().   */
    fun treeNode(intPtr: Long, fmt: String, vararg args: Any): Boolean = treeNodeEx(intPtr, 0, fmt, *args)

    fun treeNodeEx(label: String, flags: TreeNodeFlags = 0): Boolean {
        val window = currentWindow
        if (window.skipItems) return false

        return treeNodeBehavior(window.getID(label), flags, label)
    }

    fun treeNodeEx(strID: String, flags: TreeNodeFlags, fmt: String, vararg args: Any): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val labelEnd = formatString(g.tempBuffer, fmt, args)
        return treeNodeBehavior(window.getID(strID), flags, g.tempBuffer, labelEnd)
    }

    fun treeNodeEx(ptrID: Any, flags: TreeNodeFlags, fmt: String, vararg args: Any): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val labelEnd = formatString(g.tempBuffer, fmt, *args)
        return treeNodeBehavior(window.getID(ptrID), flags, g.tempBuffer, labelEnd)
    }

    fun treeNodeEx(intPtr: Long, flags: TreeNodeFlags, fmt: String, vararg args: Any): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val labelEnd = formatString(g.tempBuffer, fmt, *args)
        return treeNodeBehavior(window.getID(intPtr), flags, g.tempBuffer, labelEnd)
    }

    //    IMGUI_API void          TreePush(const char* str_id = NULL);                                    // ~ Indent()+PushId(). Already called by TreeNode() when returning true, but you can call Push/Pop yourself for layout purpose

    /** ~ Indent()+PushId(). Already called by TreeNode() when returning true, but you can call TreePush/TreePop yourself if desired.  */
    fun treePush(strId: String = "#TreePush") {
        val window = currentWindow
        indent()
        window.dc.treeDepth++
        pushID(strId)
    }

    /** ~ Unindent()+PopId()    */
    fun treePop() {
        val window = g.currentWindow!!
        unindent()

        currentWindow.dc.treeDepth--
        val treeDepthMask = 1 shl window.dc.treeDepth

        // Handle Left arrow to move to parent tree node (when ImGuiTreeNodeFlags_NavLeftJumpsBackHere is enabled)
        if (g.navMoveDir == Dir.Left && g.navWindow === window && navMoveRequestButNoResultYet())
            if (g.navIdIsAlive && window.dc.treeJumpToParentOnPopMask has treeDepthMask) {
                setNavID(window.idStack.last(), g.navLayer, 0, Rect())
                navMoveRequestCancel()
            }
        window.dc.treeJumpToParentOnPopMask = window.dc.treeJumpToParentOnPopMask and treeDepthMask - 1

        assert(window.idStack.size > 1) { "There should always be 1 element in the idStack (pushed during window creation). If this triggers you called ::treePop/popId() too much." }
        popID()
    }

    /** horizontal distance preceding label when using TreeNode*() or Bullet() == (g.FontSize + style.FramePadding.x*2)
     *  for a regular unframed TreeNode
     *  ~GetTreeNodeToLabelSpacing  */
    val treeNodeToLabelSpacing: Float
        get() = g.fontSize + style.framePadding.x * 2f

    /** CollapsingHeader returns true when opened but do not indent nor push into the ID stack (because of the
     *  ImGuiTreeNodeFlags_NoTreePushOnOpen flag).
     *  This is basically the same as calling
     *      treeNodeEx(label, TreeNodeFlag.CollapsingHeader)
     *  You can remove the _NoTreePushOnOpen flag if you want behavior closer to normal TreeNode().
     *  If returning 'true' the header is open. doesn't indent nor push on ID stack. user doesn't have to call TreePop().   */
    fun collapsingHeader(label: String, flags: TreeNodeFlags = 0): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        return treeNodeBehavior(window.getID(label), flags or Tnf.CollapsingHeader, label)
    }

    /** when 'p_visible != NULL': if '*p_visible==true' display an additional small close button on upper right of the header which will set the bool to false when clicked, if '*p_visible==false' don't display the header.
     *
     *  p_visible == NULL                        : regular collapsing header
     *  p_visible != NULL && *p_visible == true  : show a small close button on the corner of the header, clicking the button will set *p_visible = false
     *  p_visible != NULL && *p_visible == false : do not show the header at all
     *  Do not mistake this with the Open state of the header itself, which you can adjust with SetNextItemOpen() or ImGuiTreeNodeFlags_DefaultOpen. */
    fun collapsingHeader(label: String, visible: KMutableProperty0<Boolean>?, flags_: TreeNodeFlags = 0): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        if (visible?.get() == false) return false

        val id = window.getID(label)
        var flags = flags_ or Tnf.CollapsingHeader
        if (visible != null)
            flags = flags or Tnf.AllowItemOverlap or Tnf._ClipLabelForTrailingButton
        val isOpen = treeNodeBehavior(id, flags, label)
        if (visible != null) {
            // Create a small overlapping close button
            // FIXME: We can evolve this into user accessible helpers to add extra buttons on title bars, headers, etc.
            // FIXME: CloseButton can overlap into text, need find a way to clip the text somehow.
            val lastItemBackup = g.lastItemData
            val buttonSize = g.fontSize
            val buttonX = g.lastItemData.rect.min.x max (g.lastItemData.rect.max.x - g.style.framePadding.x * 2f - buttonSize)
            val buttonY = g.lastItemData.rect.min.y
            val closeButtonId = getIDWithSeed("#CLOSE", -1, id)
            if (closeButton(closeButtonId, Vec2(buttonX, buttonY)))
                visible.set(false)
            g.lastItemData = lastItemBackup
        }
        return isOpen
    }

    /** Set next TreeNode/CollapsingHeader open state.  */
    fun setNextItemOpen(isOpen: Boolean, cond: Cond = Cond.Always) {
        if (g.currentWindow!!.skipItems) return
        g.nextItemData.apply {
            flags = flags or NextItemDataFlag.HasOpen
            openVal = isOpen
            openCond = cond.takeUnless { it == Cond.None } ?: Cond.Always
        }
    }
}