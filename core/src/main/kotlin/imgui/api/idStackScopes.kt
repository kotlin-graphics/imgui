package imgui.api

import imgui.ID

// ID stack/scopes
// Read the FAQ (docs/FAQ.md or http://dearimgui.org/faq) for more details about how ID are handled in dear imgui.
// - Those questions are answered and impacted by understanding of the ID stack system:
//   - "Q: Why is my widget not reacting when I click on it?"
//   - "Q: How can I have widgets with an empty label?"
//   - "Q: How can I have multiple widgets with the same label?"
// - Short version: ID are hashes of the entire ID stack. If you are creating widgets in a loop you most likely
//   want to push a unique identifier (e.g. object pointer, loop index) to uniquely differentiate them.
// - You can also use the "Label##foobar" syntax within widget label to distinguish them from each others.
// - In this header file we use the "label"/"name" terminology to denote a string that will be displayed + used as an ID,
//   whereas "str_id" denote a string that is only used as an ID and not normally displayed.
interface idStackScopes {

    /** [JVM] */
    fun pushID(ptrID: Any) = with(g.currentWindow!!) { idStack += getID(ptrID) }

    /** push string into the ID stack (will hash string).  */
    fun pushID(strID: String) = with(g.currentWindow!!) { idStack += getID(strID) }

    /** push string into the ID stack (will hash string).  */
    fun pushID(strID: String, strIdEnd: Int) = with(g.currentWindow!!) { idStack += getID(strID, strIdEnd) }

    /** push pointer into the ID stack (will hash pointer).  */
    fun pushID(intPtr: Long) = with(g.currentWindow!!) { idStack += getID(intPtr) }

    /** push integer into the ID stack (will hash integer). */
    fun pushID(intId: Int) = with(g.currentWindow!!) { idStack += getID(intId) }

    /** pop from the ID stack. */
    fun popID() {
        val window = g.currentWindow!!
        assert(window.idStack.size > 1) { "Too many PopID(), or could be popping in a wrong/different window?" }
        window.idStack.pop()
    }

    /** calculate unique ID (hash of whole ID stack + given parameter). e.g. if you want to query into ImGuiStorage
     *  yourself. otherwise rarely needed   */
    fun getID(strID: String) = g.currentWindow!!.getID(strID)

    fun getID(ptrID: Any): ID = g.currentWindow!!.getID(ptrID)

    /** ~ImGui::GetID((void*)(intptr_t)i) */
    fun getID(intPtr: Long): ID = g.currentWindow!!.getID(intPtr)
}