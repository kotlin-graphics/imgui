package imgui.internal.api

import imgui.ID
import imgui.api.g

// Focus Scope (WIP)
// This is generally used to identify a selection set (multiple of which may be in the same window), as selection
// patterns generally need to react (e.g. clear selection) when landing on an item of the set.
interface focusScope {

    fun pushFocusScope(id: ID) { // TODO dsl
        val window = g.currentWindow!!
        g.focusScopeStack += window.dc.navFocusScopeIdCurrent
        window.dc.navFocusScopeIdCurrent = id
    }

    fun popFocusScope() {
        val window = g.currentWindow!!
        assert(g.focusScopeStack.isNotEmpty()) { "Too many PopFocusScope() ?" }
        window.dc.navFocusScopeIdCurrent = g.focusScopeStack.last()
        g.focusScopeStack.pop()
    }

    /** Focus scope which is actually active
     *  ~GetFocusedFocusScope */
    val focusedFocusScope: ID
        get() = g.navFocusScopeId

    /** Focus scope we are outputting into, set by PushFocusScope()
     *  ~GetFocusScopeID */
    val focusScope: ID
        get() = g.currentWindow!!.dc.navFocusScopeIdCurrent
}