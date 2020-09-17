package imgui.internal.api

import imgui.ID
import imgui.api.g

// Focus Scope (WIP)
// This is generally used to identify a selection set (multiple of which may be in the same window), as selection
// patterns generally need to react (e.g. clear selection) when landing on an item of the set.
interface focusScope {

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