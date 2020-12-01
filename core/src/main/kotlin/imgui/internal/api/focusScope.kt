package imgui.internal.api

import imgui.ID
import imgui.api.g

// Focus Scope (WIP)
// This is generally used to identify a selection set (multiple of which may be in the same window), as selection
// patterns generally need to react (e.g. clear selection) when landing on an item of the set.
interface focusScope {

    /** FIXME: this is storing in same stack as IDStack, so Push/Pop mismatch will be reported there. Maybe play nice and a separate in-context stack. */
    fun pushFocusScope(id: ID) { // TODO dsl
        g.currentWindow!!.apply {
            val topMostId = idStack.last()
            idStack += dc.navFocusScopeIdCurrent
            idStack += topMostId
            dc.navFocusScopeIdCurrent = id
        }
    }

    fun popFocusScope() {
        g.currentWindow!!.apply {
            dc.navFocusScopeIdCurrent = idStack.last()
            idStack.pop()
            assert(idStack.size > 1) { "Too many PopID or PopFocusScope (or could be popping in a wrong/different window?)" }
            idStack.pop()
        }
    }

    /** ~GetFocusScopeID */
    val focusScopeID: ID
        get() = g.navFocusScopeId
}