package imgui.internal.api

import imgui.DataType
import imgui.Hook
import imgui.ID
import imgui.IMGUI_ENABLE_TEST_ENGINE
import imgui.api.g
import imgui.api.gImGui
import imgui.internal.classes.Window
import imgui.internal.hash
import imgui.internal.sections.*

/** Basic Accessors */
internal interface basicAccessors {

    /** ~GetItemID
     *  Get ID of last item (~~ often same ImGui::GetID(label) beforehand) */
    val itemID: ID
        get() = g.currentWindow!!.dc.lastItemId

    /** ~GetItemStatusFlags */
    val itemStatusFlags: ItemStatusFlags
        get() = g.currentWindow!!.dc.lastItemStatusFlags

    /** ~GetActiveID */
    val activeID: ID
        get() = g.activeId

    /** ~GetFocusID */
    val focusID: ID
        get() = g.navId

    fun setActiveID(id: ID, window: Window?) {
        g.activeIdIsJustActivated = g.activeId != id
        if (g.activeIdIsJustActivated) {
            g.activeIdTimer = 0f
            g.activeIdHasBeenPressedBefore = false
            g.activeIdHasBeenEditedBefore = false
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
                g.navActivateId, g.navInputId, g.navJustTabbedId, g.navJustMovedToId -> InputSource.Nav
                else -> InputSource.Mouse
            }
        }

        // Clear declaration of inputs claimed by the widget
        // (Please note that this is WIP and not all keys/inputs are thoroughly declared by all widgets yet)
        g.activeIdUsingNavDirMask = 0x00
        g.activeIdUsingNavInputMask = 0x00
        g.activeIdUsingKeyInputMask = 0x00
    }

    /** FIXME-NAV: The existence of SetNavID/SetNavIDWithRectRel/SetFocusID is incredibly messy and confusing and needs some explanation or refactoring. */
    fun setFocusID(id: ID, window: Window) {

        assert(id != 0)

        /*  Assume that SetFocusID() is called in the context where its window->DC.NavLayerCurrent and window->DC.NavFocusScopeIdCurrent are valid.
            Note that window may be != g.CurrentWindow (e.g. SetFocusID call in InputTextEx for multi-line text)         */
        val navLayer = window.dc.navLayerCurrent
        if (g.navWindow !== window)
            g.navInitRequest = false
        g.navWindow = window
        g.navId = id
        g.navLayer = navLayer
        g.navFocusScopeId = window.dc.navFocusScopeIdCurrent
        window.navLastIds[navLayer] = id
        if (window.dc.lastItemId == id)
            window.navRectRel[navLayer].put(window.dc.lastItemRect.min - window.pos, window.dc.lastItemRect.max - window.pos)

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
            if (value != 0 && g.hoveredIdPreviousFrame != value) {
                g.hoveredIdTimer = 0f
                g.hoveredIdNotActiveTimer = 0f
            }
        }

    fun keepAliveID(id: ID) {
        if (g.activeId == id)
            g.activeIdIsAlive = id
        if (g.activeIdPreviousFrame == id)
            g.activeIdPreviousFrameIsAlive = true
    }

    /** Mark data associated to given item as "edited", used by IsItemDeactivatedAfterEdit() function. */
    fun markItemEdited(id: ID) {
        /*  This marking is solely to be able to provide info for ::isItemDeactivatedAfterEdit().
            ActiveId might have been released by the time we call this (as in the typical press/release button behavior)
            but still need need to fill the data.         */
        assert(g.activeId == id || g.activeId == 0 || g.dragDropActive)
        //IM_ASSERT(g.CurrentWindow->DC.LastItemId == id)
        g.activeIdHasBeenEditedThisFrame = true
        g.activeIdHasBeenEditedBefore = true
        g.currentWindow!!.dc.apply { lastItemStatusFlags = lastItemStatusFlags or ItemStatusFlag.Edited }
    }

    /** Push a given id value ignoring the ID stack as a seed.
     *  Push given value as-is at the top of the ID stack (whereas PushID combines old and new hashes) */
    fun pushOverrideID(id: ID) {
        g.currentWindow!!.idStack += id
    }

    /** Helper to avoid a common series of PushOverrideID -> GetID() -> PopID() call
     *  (note that when using this pattern, TestEngine's "Stack Tool" will tend to not display the intermediate stack level.
     *  for that to work we would need to do PushOverrideID() -> ItemAdd() -> PopID() which would alter widget code a little more) */
    fun getIDWithSeed(str: String, strEnd: Int = -1, seed: ID): ID {
        val id = hash(str, if(strEnd != -1) strEnd else 0, seed)
        keepAliveID(id)
        if(IMGUI_ENABLE_TEST_ENGINE) {
            val g = gImGui!!
            Hook.idInfo2!!.invoke(g, DataType._String, id, str, strEnd)
        }
        return id
    }
}