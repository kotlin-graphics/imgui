package imgui.internal.api

import imgui.ID
import imgui.api.g
import imgui.internal.sections.FocusScope

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
        if (g.focusScopeStackLocked > 0)
            return
        val scope = FocusScope(id, g.currentWindow)
        g.focusScopeStack += scope
        g.currentFocusScopeId = scope.focusScopeId
    }

    fun popFocusScope() {
        if (g.focusScopeStackLocked > 0)
            return
        assert(g.focusScopeStack.isNotEmpty()) { "Too many PopFocusScope() ?" }
        assert(g.focusScopeStack.last().window === g.currentWindow) { "Mismatched pop location?" }
        g.focusScopeStack.pop()
        g.currentFocusScopeId = g.focusScopeStack.lastOrNull()?.focusScopeId ?: 0
    }

    /** Focus scope we are outputting into, set by PushFocusScope() */
    val currentFocusScope: ID
        get() = g.currentFocusScopeId
}