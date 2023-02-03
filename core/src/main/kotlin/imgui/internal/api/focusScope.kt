package imgui.internal.api

import imgui.ID
import imgui.api.g

// [EXPERIMENTAL] Focus Scope
// This is generally used to identify a unique input location (for e.g. a selection set)
// There is one per window (automatically set in Begin), but:
// - Selection patterns generally need to react (e.g. clear a selection) when landing on one item of the set.
//   So in order to identify a set multiple lists in same window may each need a focus scope.
//   If you imagine an hypothetical BeginSelectionGroup()/EndSelectionGroup() api, it would likely call PushFocusScope()/EndFocusScope()
// - Shortcut routing also use focus scope as a default location identifier if an owner is not provided.
// We don't use the ID Stack for this as it is common to want them separate.
interface focusScope {

    fun pushFocusScope(id: ID) {
        g.focusScopeStack += id
        g.currentFocusScopeId = id
    }

    fun popFocusScope() {
        assert(g.focusScopeStack.isNotEmpty()) { "Too many PopFocusScope() ?" }
        g.focusScopeStack.pop()
        g.currentFocusScopeId = g.focusScopeStack.lastOrNull() ?: 0
    }

    /** Focus scope we are outputting into, set by PushFocusScope() */
    val currentFocusScope: ID
        get() = g.currentFocusScopeId
}