package imgui.imgui

import glm_.glm
import glm_.vec2.Vec2
import imgui.ImGui.currentWindow
import imgui.ImGui.popId
import imgui.ImGui.treeNodeBehavior
import imgui.ImGui.unindent
import imgui.Context.style
import imgui.ImGui.closeButton
import imgui.ImGui.indent
import imgui.ImGui.pushId
import imgui.Cond
import imgui.TreeNodeFlags
import imgui.or
import imgui.Context as g

/** Widgets: Trees  */
interface imgui_widgetsTrees {

    /** if returning 'true' the node is open and the tree id is pushed into the id stack. user is responsible for
     *  calling TreePop().  */
    fun treeNode(label: String): Boolean {
        val window = currentWindow
        if (window.skipItems) return false
        return treeNodeBehavior(window.getId(label), 0, label)
    }

    /** read the FAQ about why and how to use ID. to align arbitrary text at the same level as a TreeNode() you can use
     *  Bullet().   */
    fun treeNode(strId: String, fmt: String, vararg args: Any) = treeNodeExV(strId, 0, fmt, *args)

    /** read the FAQ about why and how to use ID. to align arbitrary text at the same level as a TreeNode() you can use
     *  Bullet().   */
    fun treeNode(ptrId: Any, fmt: String, vararg args: Any) = treeNodeExV(ptrId, 0, fmt, *args)
//    IMGUI_API bool          TreeNodeV(const char* str_id, const char* fmt, va_list args);           // "
//    IMGUI_API bool          TreeNodeV(const void* ptr_id, const char* fmt, va_list args);           // "
//    IMGUI_API bool          TreeNodeEx(const char* label, ImGuiTreeNodeFlags flags = 0);
//    IMGUI_API bool          TreeNodeEx(const char* str_id, ImGuiTreeNodeFlags flags, const char* fmt, ...) IM_PRINTFARGS(3);
//    IMGUI_API bool          TreeNodeEx(const void* ptr_id, ImGuiTreeNodeFlags flags, const char* fmt, ...) IM_PRINTFARGS(3);

    fun treeNodeExV(strId: String, flags: Int, fmt: String, vararg args: Any): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val label = fmt.format(style.locale, *args)

        return treeNodeBehavior(window.getId(strId), flags, label)
    }

    fun treeNodeExV(ptrId: Any, flags: Int, fmt: String, vararg args: Any): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val label = fmt.format(style.locale, *args)

        return treeNodeBehavior(window.getId(ptrId), flags, label)
    }
//    IMGUI_API void          TreePush(const char* str_id = NULL);                                    // ~ Indent()+PushId(). Already called by TreeNode() when returning true, but you can call Push/Pop yourself for layout purpose

    /** ~ Indent()+PushId(). Already called by TreeNode() when returning true, but you can call Push/Pop yourself for
     *  layout purpose  */
    fun treePush(ptrId: Any? = null) {
        val window = currentWindow
        indent()
        window.dc.treeDepth++
        pushId(ptrId ?: "#TreePush")
    }

    /** ~ Unindent()+PopId()    */
    fun treePop() {
        unindent()
        currentWindow.dc.treeDepth--
        popId()
    }

    /** advance cursor x position by treeNodeToLabelSpacing    */
    fun treeAdvanceToLabelPos() {
        g.currentWindow!!.dc.cursorPos.x += treeNodeToLabelSpacing
    }

    /** horizontal distance preceding label when using TreeNode*() or Bullet() == (g.FontSize + style.FramePadding.x*2)
     *  for a regular unframed TreeNode */
    val treeNodeToLabelSpacing get() = g.fontSize + style.framePadding.x * 2f

    /** set next TreeNode/CollapsingHeader open state.  */
    fun setNextTreeNodeOpen(isOpen: Boolean, cond: Cond = Cond.Always) {
        g.setNextTreeNodeOpenVal = isOpen
        g.setNextTreeNodeOpenCond = cond.i
    }

    /** CollapsingHeader returns true when opened but do not indent nor push into the ID stack (because of the
     *  ImGuiTreeNodeFlags_NoTreePushOnOpen flag).
     *  This is basically the same as calling
     *      treeNodeEx(label, TreeNodeFlags.CollapsingHeader | TreeNodeFlags.NoTreePushOnOpen)
     *  You can remove the _NoTreePushOnOpen flag if you want behavior closer to normal TreeNode().
     *  If returning 'true' the header is open. doesn't indent nor push on ID stack. user doesn't have to call TreePop().   */
    fun collapsingHeader(label: String, flags: Int = 0): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        return treeNodeBehavior(window.getId(label), flags or TreeNodeFlags.CollapsingHeader or TreeNodeFlags.NoTreePushOnOpen, label)
    }

    /** when 'pOpen' isn't NULL, display an additional small close button on upper right of the header */
    fun collapsingHeader(label: String, pOpen: BooleanArray?, flags: Int = 0): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        if(pOpen!= null && !pOpen[0]) return false

        val id = window.getId(label)
        val isOpen = treeNodeBehavior(id, flags or TreeNodeFlags.CollapsingHeader or TreeNodeFlags.NoTreePushOnOpen or
                if (pOpen != null) TreeNodeFlags.AllowOverlapMode else TreeNodeFlags.Null, label)
        if (pOpen != null) {
            // Create a small overlapping close button // FIXME: We can evolve this into user accessible helpers to add extra buttons on title bars, headers, etc.
            val buttonSz = g.fontSize * 0.5f
            if (closeButton(window.getId(id + 1), Vec2(glm.min(window.dc.lastItemRect.max.x, window.clipRect.max.x) -
                    style.framePadding.x - buttonSz, window.dc.lastItemRect.min.y + style.framePadding.y + buttonSz), buttonSz))
            pOpen[0] = false
        }
        return isOpen
    }
}