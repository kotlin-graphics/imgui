package imgui.internal.api

import imgui.DataType
import imgui.Hook
import imgui.ID
import imgui.api.g

/** Focus scope (WIP) */
interface focusScope {

    /** Note: this is storing in same stack as IDStack, so Push/Pop mismatch will be reported there. */
    fun pushFocusScope(id: ID) { // TODO dsl
        g.currentWindow!!.apply {
            idStack += dc.navFocusScopeIdCurrent
            dc.navFocusScopeIdCurrent = id
        }
    }

    fun popFocusScope() {
        g.currentWindow!!.apply {
            dc.navFocusScopeIdCurrent = idStack.last()
            idStack.pop()
        }
    }

    /** ~GetFocusScopeID */
    val focusScopeID: ID
        get() = g.navFocusScopeId
}