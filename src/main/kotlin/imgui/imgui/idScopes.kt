package imgui.imgui

import imgui.ImGui.currentWindow
import imgui.Context as g

/** ID scopes
 *  If you are creating widgets in a loop you most likely want to push a unique identifier so ImGui can differentiate
 *  them.
 *  You can also use the "##foobar" syntax within widget label to distinguish them from each others. Read "A primer on
 *  the use of labels/IDs" in the FAQ for more details. */
interface imgui_idScopes {


    /** push identifier into the ID stack. IDs are hash of the *entire* stack!  */
    fun pushId(strId: String) = with(currentWindow) { idStack.push(getId(strId)) }

    /** Includes Int IDs as well    */
    fun pushId(ptrId: Any) = with(currentWindow) { idStack.push(getId(ptrId)) }

    fun popId() = currentWindow.idStack.pop()

    /** calculate unique ID (hash of whole ID stack + given parameter). useful if you want to query into ImGuiStorage
     *  yourself. otherwise rarely needed   */
    fun getId(strId: String) = g.currentWindow!!.getId(strId)

    fun getId(ptrId: Any) = currentWindow.getId(ptrId)
}