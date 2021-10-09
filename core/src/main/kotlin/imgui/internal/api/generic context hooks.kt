package imgui.internal.api

import imgui.ID
import imgui.classes.Context
import imgui.classes.ContextHook
import imgui.classes.ContextHookType

// Generic context hooks
interface `generic context hooks` {

    /** No specific ordering/dependency support, will see as needed */
    fun addContextHook(ctx: Context, hook: ContextHook): ID {
        val g = ctx
        check(hook.callback != null  && hook.hookId == 0 && hook.type != ContextHookType.PendingRemoval_)
        g.hooks += hook.apply {
            hookId = ++g.hookIdNext
        }
        return g.hookIdNext
    }

    // Deferred removal, avoiding issue with changing vector while iterating it
    fun removeContextHook(ctx: Context, hookId: ID) {
        val g = ctx
        check(hookId != 0)
        for (hook in g.hooks)
            if (hook.hookId == hookId)
                hook.type = ContextHookType.PendingRemoval_
    }

        /** Call context hooks (used by e.g. test engine)
     *  We assume a small number of hooks so all stored in same array */
    fun callContextHooks(ctx: Context, hookType: ContextHookType) {
        val g = ctx
        for (hook in g.hooks)
            if (hook.type == hookType)
                hook.callback!!(g, hook)
    }
}