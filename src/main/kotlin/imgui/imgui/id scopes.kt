package imgui.imgui

import imgui.ID

/** ID stack/scopes
 *  - Read the FAQ for more details about how ID are handled in dear imgui. If you are creating widgets in a loop you most
 *      likely want to push a unique identifier (e.g. object pointer, loop index) to uniquely differentiate them.
 *  - The resulting ID are hashes of the entire stack.
 *  - You can also use the "Label##foobar" syntax within widget label to distinguish them from each others.
 *  - In this header file we use the "label"/"name" terminology to denote a string that will be displayed and used as an ID,
 *      whereas "str_id" denote a string that is only used as an ID and not normally displayed.  */
interface imgui_idScopes {


    /** push string into the ID stack (will hash string).  */
    fun pushId(strId: String) = with(g.currentWindow!!) { idStack += getIdNoKeepAlive(strId) }

    /** push string into the ID stack (will hash string).  */
    fun pushId(strId: String, strIdEnd: Int) = with(g.currentWindow!!) { idStack += getIdNoKeepAlive(strId, strIdEnd) }

    /** push pointer into the ID stack (will hash pointer).  */
    fun pushId(ptrId: Any) = with(g.currentWindow!!) { idStack += getIdNoKeepAlive(ptrId) }

    /** push integer into the ID stack (will hash integer). */
    fun pushId(intId: Int) = with(g.currentWindow!!) { idStack += getIdNoKeepAlive(intId) }

    /** pop from the ID stack. */
    fun popId() = g.currentWindow!!.idStack.pop()

    /** calculate unique ID (hash of whole ID stack + given parameter). e.g. if you want to query into ImGuiStorage
     *  yourself. otherwise rarely needed   */
    fun getId(strId: String) = g.currentWindow!!.getId(strId)

    fun getId(ptrId: Any): ID = g.currentWindow!!.getId(ptrId)
}