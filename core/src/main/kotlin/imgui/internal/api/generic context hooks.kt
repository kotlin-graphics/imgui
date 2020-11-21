package imgui.internal.api

import imgui.classes.Context
import imgui.classes.ContextHook
import imgui.classes.ContextHookType

// Generic context hooks
interface `generic context hooks` {

    /** No specific ordering/dependency support, will see as needed */
    fun addContextHook(ctx: Context, hook: ContextHook) {
        val g = ctx
//        assert(hook.callback != null)
        g.hooks += hook
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