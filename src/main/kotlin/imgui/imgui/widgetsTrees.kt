package imgui.imgui

import imgui.ImGui.currentWindow
import imgui.ImGui.popId
import imgui.ImGui.treeNodeBehavior
import imgui.ImGui.unindent
import imgui.Style
import java.util.*
import imgui.Context as g

/** Widgets: Trees  */
interface imgui_widgetsTrees {

    /** if returning 'true' the node is open and the tree id is pushed into the id stack. user is responsible for
     *  calling TreePop().  */
    fun treeNode(label:String):Boolean {
        val window = currentWindow
        if (window.skipItems)        return false
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

        val label = fmt.format(Style.locale, *args)

        return treeNodeBehavior(window.getId(strId), flags, label)
    }

    fun treeNodeExV(ptrId: Any, flags: Int, fmt: String, vararg args: Any): Boolean {

        val window = currentWindow
        if (window.skipItems) return false

        val label = fmt.format(Style.locale, *args)

        return treeNodeBehavior(window.getId(ptrId), flags, label)
    }
//    IMGUI_API void          TreePush(const char* str_id = NULL);                                    // ~ Indent()+PushId(). Already called by TreeNode() when returning true, but you can call Push/Pop yourself for layout purpose
//    IMGUI_API void          TreePush(const void* ptr_id = NULL);                                    // "

    /** ~ Unindent()+PopId()    */
    fun treePop() {
        unindent()
        currentWindow.dc.treeDepth--
        popId()
    }
//    IMGUI_API void          TreeAdvanceToLabelPos();                                                // advance cursor x position by GetTreeNodeToLabelSpacing()
//    IMGUI_API float         GetTreeNodeToLabelSpacing();                                            // horizontal distance preceding label when using TreeNode*() or Bullet() == (g.FontSize + style.FramePadding.x*2) for a regular unframed TreeNode
//    IMGUI_API void          SetNextTreeNodeOpen(bool is_open, ImGuiSetCond cond = 0);               // set next TreeNode/CollapsingHeader open state.
//    IMGUI_API bool          CollapsingHeader(const char* label, ImGuiTreeNodeFlags flags = 0);      // if returning 'true' the header is open. doesn't indent nor push on ID stack. user doesn't have to call TreePop().
//    IMGUI_API bool          CollapsingHeader(const char* label, bool* p_open, ImGuiTreeNodeFlags flags = 0); // when 'p_open' isn't NULL, display an additional small close button on upper right of the header
}