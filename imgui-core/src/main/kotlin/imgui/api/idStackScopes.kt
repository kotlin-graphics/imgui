package imgui.api

import imgui.DataType
import imgui.Hook
import imgui.ID

/** ID stack/scopes
 *  - Read the FAQ for more details about how ID are handled in dear imgui. If you are creating widgets in a loop you most
 *      likely want to push a unique identifier (e.g. object pointer, loop index) to uniquely differentiate them.
 *  - The resulting ID are hashes of the entire stack.
 *  - You can also use the "Label##foobar" syntax within widget label to distinguish them from each others.
 *  - In this header file we use the "label"/"name" terminology to denote a string that will be displayed and used as an ID,
 *      whereas "str_id" denote a string that is only used as an ID and not normally displayed.  */
interface idStackScopes {

    /** [JVM] */
    fun pushID(ptrID: Any) = with(g.currentWindow!!) { idStack += getIdNoKeepAlive(ptrID) }

    /** push string into the ID stack (will hash string).  */
    fun pushID(strID: String) {
        val window = g.currentWindow!!
        val id = window.getIdNoKeepAlive(strID)
        window.idStack += id
        Hook.pushID?.invoke(g, DataType._String, id, strID)
    }

    /** push string into the ID stack (will hash string).  */
    fun pushID(strID: String, strIdEnd: Int) {
        val window = g.currentWindow!!
        val id = window.getIdNoKeepAlive(strID, strIdEnd)
        window.idStack += id
        Hook.pushID2?.invoke(g, DataType._String, id, strID, strIdEnd)
    }

    /** push pointer into the ID stack (will hash pointer).  */
    fun pushID(intPtr: Long) {
        val window = g.currentWindow!!
        val id = window.getIdNoKeepAlive(intPtr)
        window.idStack += id
        Hook.pushID?.invoke(g, DataType._Pointer, id, intPtr)
    }

    /** push integer into the ID stack (will hash integer). */
    fun pushID(intId: Int) {
        val window = g.currentWindow!!
        val id = window.getIdNoKeepAlive(intId)
        window.idStack += id
        Hook.pushID?.invoke(g, DataType.Int, id, intId)
    }

    /** pop from the ID stack. */
    fun popID() = g.currentWindow!!.idStack.pop()

    /** calculate unique ID (hash of whole ID stack + given parameter). e.g. if you want to query into ImGuiStorage
     *  yourself. otherwise rarely needed   */
    fun getID(strID: String) = g.currentWindow!!.getID(strID)

    fun getID(ptrID: Any): ID = g.currentWindow!!.getID(ptrID)

    fun getID(intPtr: Long): ID = g.currentWindow!!.getID(intPtr)
}