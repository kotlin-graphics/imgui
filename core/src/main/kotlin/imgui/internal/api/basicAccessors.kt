package imgui.internal.api

import imgui.DataType
import imgui.ID
import imgui.ImGui.debugHookIdInfo
import imgui.ImGui.setNavWindow
import imgui.api.g
import imgui.internal.classes.Window
import imgui.internal.hashStr
import imgui.internal.sections.*
import imgui.static.navUpdateAnyRequestFlag

// Basic Accessors
internal interface basicAccessors {

    /** ~GetItemID
     *  Get ID of last item (~~ often same ImGui::GetID(label) beforehand) */
    val itemID: ID
        get() = g.lastItemData.id

    /** ~GetItemStatusFlags */
    val itemStatusFlags: ItemStatusFlags
        get() = g.lastItemData.statusFlags

    val itemFlags: ItemFlags
        get() = g.lastItemData.inFlags

    /** ~GetActiveID */
    val activeID: ID
        get() = g.activeId

    /** ~GetFocusID */
    val focusID: ID
        get() = g.navId

    fun setActiveID(id: ID, window: Window?) {

        // While most behaved code would make an effort to not steal active id during window move/drag operations,
        // we at least need to be resilient to it. Cancelling the move is rather aggressive and users of 'master' branch
        // may prefer the weird ill-defined half working situation ('docking' did assert), so may need to rework that.
        if (g.movingWindow != null && g.activeId == g.movingWindow!!.moveId) {
            IMGUI_DEBUG_LOG_ACTIVEID("SetActiveID() cancel MovingWindow")
            g.movingWindow = null
        }

        // Set active id
        g.activeIdIsJustActivated = g.activeId != id
        if (g.activeIdIsJustActivated) {
            IMGUI_DEBUG_LOG_ACTIVEID("SetActiveID() old:0x%08X (window \"${g.activeIdWindow?.name ?: ""}\") -> new:0x%08X (window \"${window?.name ?: ""}\")",
                                     g.activeId, id)
            g.activeIdTimer = 0f
            g.activeIdHasBeenPressedBefore = false
            g.activeIdHasBeenEditedBefore = false
            g.activeIdMouseButton = -1
            if (id != 0) {
                g.lastActiveId = id
                g.lastActiveIdTimer = 0f
            }
        }
        g.activeId = id
        g.activeIdAllowOverlap = false
        g.activeIdNoClearOnFocusLoss = false
        g.activeIdWindow = window
        g.activeIdHasBeenEditedThisFrame = false
        if (id != 0) {
            g.activeIdIsAlive = id
            g.activeIdSource = when (id) {
                g.navActivateId, g.navActivateInputId, g.navJustMovedToId -> InputSource.Nav
                else -> InputSource.Mouse
            }
        }

        // Clear declaration of inputs claimed by the widget
        // (Please note that this is WIP and not all keys/inputs are thoroughly declared by all widgets yet)
        g.activeIdUsingNavDirMask = 0x00
        g.activeIdUsingKeyInputMask.clearAllBits()
    }

    /** FIXME-NAV: The existence of SetNavID/SetNavIDWithRectRel/SetFocusID is incredibly messy and confusing and needs some explanation or refactoring. */
    fun setFocusID(id: ID, window: Window) {

        assert(id != 0)

        if (g.navWindow !== window)
            setNavWindow(window)

        // Assume that SetFocusID() is called in the context where its window->DC.NavLayerCurrent and window->DC.NavFocusScopeIdCurrent are valid.
        // Note that window may be != g.CurrentWindow (e.g. SetFocusID call in InputTextEx for multi-line text)
        val navLayer = window.dc.navLayerCurrent
        g.navId = id
        g.navLayer = navLayer
        g.navFocusScopeId = window.dc.navFocusScopeIdCurrent
        window.navLastIds[navLayer] = id
        if (g.lastItemData.id == id)
            window.navRectRel[navLayer].put(window rectAbsToRel g.lastItemData.navRect)

        if (g.activeIdSource == InputSource.Nav)
            g.navDisableMouseHover = true
        else
            g.navDisableHighlight = true
    }

    fun clearActiveID() = setActiveID(0, null) // g.ActiveId = 0;

    var hoveredId: ID
        /** ~GetHoveredID */
        get() = if (g.hoveredId != 0) g.hoveredId else g.hoveredIdPreviousFrame
        /** ~SetHoveredID */
        set(value) {
            g.hoveredId = value
            g.hoveredIdAllowOverlap = false
            g.hoveredIdUsingMouseWheel = false
            if (value != 0 && g.hoveredIdPreviousFrame != value) {
                g.hoveredIdTimer = 0f
                g.hoveredIdNotActiveTimer = 0f
            }
        }


    /** This is called by ItemAdd().
     *  Code not using ItemAdd() may need to call this manually otherwise ActiveId will be cleared. In IMGUI_VERSION_NUM < 18717 this was called by GetID(). */
    fun keepAliveID(id: ID) {
        if (g.activeId == id)
            g.activeIdIsAlive = id
        if (g.activeIdPreviousFrame == id)
            g.activeIdPreviousFrameIsAlive = true
    }

    /** Mark data associated to given item as "edited", used by IsItemDeactivatedAfterEdit() function. */
    fun markItemEdited(id: ID) {
        // This marking is solely to be able to provide info for IsItemDeactivatedAfterEdit().
        // ActiveId might have been released by the time we call this (as in the typical press/release button behavior) but still need to fill the data.
        assert(g.activeId == id || g.activeId == 0 || g.dragDropActive)
        //IM_ASSERT(g.CurrentWindow->DC.LastItemId == id)
        g.activeIdHasBeenEditedThisFrame = true
        g.activeIdHasBeenEditedBefore = true
        g.lastItemData.statusFlags /= ItemStatusFlag.Edited
    }

    /** Push a given id value ignoring the ID stack as a seed.
     *  Push given value as-is at the top of the ID stack (whereas PushID combines old and new hashes) */
    fun pushOverrideID(id: ID) {
        val window = g.currentWindow!!
        if (g.debugHookIdInfo == id)
            debugHookIdInfo(id, DataType._ID, null)
        window.idStack += id
    }

    /** Helper to avoid a common series of PushOverrideID -> GetID() -> PopID() call
     *  (note that when using this pattern, TestEngine's "Stack Tool" will tend to not display the intermediate stack level.
     *  for that to work we would need to do PushOverrideID() -> ItemAdd() -> PopID() which would alter widget code a little more) */
    fun getIDWithSeed(str: String, strEnd: Int = -1, seed: ID): ID {
        val id = hashStr(str, if (strEnd != -1) strEnd else 0, seed)
        if (g.debugHookIdInfo == id)
            debugHookIdInfo(id, DataType._String, str, strEnd)
        return id
    }
}