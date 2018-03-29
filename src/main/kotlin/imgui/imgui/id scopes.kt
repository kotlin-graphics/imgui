package imgui.imgui

import imgui.ID
import imgui.ImGui.currentWindow
import imgui.ImGui.currentWindowRead
import imgui.g

/** ID scopes
 *  Read the FAQ for more details about how ID are handled in dear imgui. If you are creating widgets in a loop you most
 *  likely want to push a unique identifier (e.g. object pointer, loop index) to uniquely differentiate them.
 *  You can also use the "##foobar" syntax within widget label to distinguish them from each others.
 *  In this header file we use the "label"/"name" terminology to denote a string that will be displayed and used as an ID,
 *  whereas "str_id" denote a string that is only used as an ID and not aimed to be displayed.  */
interface imgui_idScopes {


    /** push identifier into the ID stack. IDs are hash of the entire stack!  */
    fun pushId(strId: String) = with(currentWindowRead!!) { idStack.push(getId(strId)) }

    /** Includes Int IDs as well    */
    fun pushId(ptrId: Any) = with(currentWindowRead!!) { idStack.push(getId(ptrId)) }

    fun popId() = currentWindowRead!!.idStack.pop()

    /** calculate unique ID (hash of whole ID stack + given parameter). e.g. if you want to query into ImGuiStorage
     *  yourself. otherwise rarely needed   */
    fun getId(strId: String) = g.currentWindow!!.getId(strId)

    fun getId(ptrId: Any): ID = currentWindow.getId(ptrId)
}