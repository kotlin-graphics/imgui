package imgui.internal.api

import imgui.ContextHook
import imgui.ContextHookType
import imgui.classes.Context

// Generic context hooks
interface genericContextHooks {

    /** No specific ordering/dependency support, will see as needed */
    infix fun Context.addHook(hook: ContextHook) {
        assert(hook.callback != null)
        hooks += hook
    }

    /** Call context hooks (used by e.g. test engine)
     *  We assume a small number of hooks so all stored in same array */
    infix fun Context.callHooks(hookType: ContextHookType) {
        for (hook in hooks)
            if (hook.type == hookType)
                hook.callback!!.invoke(this, hook)
    }
}