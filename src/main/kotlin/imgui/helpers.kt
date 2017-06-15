package imgui

import glm_.bool
import glm_.f
import glm_.i


class OnceUponAFrame {
    init {
        TODO()
    }
}

class TextFilter {
    init {
        TODO()
    }
}

class TextBuffer {
    init {
        TODO()
    }
}

/** Helper: Simple Key->value storage
    Typically you don't have to worry about this since a storage is held within each Window.
    We use it to e.g. store collapse state for a tree (Int 0/1), store color edit options.
    You can use it as custom user storage for temporary values.
    Declare your own storage if:
        - You want to manipulate the open/close state of a particular sub-tree in your interface (tree node uses Int 0/1
            to store their state).
        - You want to store custom debug data easily without adding or editing structures in your code.
    Types are NOT stored, so it is up to you to make sure your Key don't collide with different types.  */
class Storage{

    val data = HashMap<Int, Float>()

    // - Get***() functions find pair, never add/allocate. Pairs are sorted so a query is O(log N)
    // - Set***() functions find pair, insertion on demand if missing.
    // - Sorted insertion is costly, paid once. A typical frame shouldn't need to insert any new pair.
    fun clear() = data.clear()
    fun int(key:Int, defaultVal:Int = 0) = data[key]?.i ?: defaultVal
    operator fun set(key:Int, value:Int) {data[key] = value.f}
    fun bool(key:Int, defaultVal:Boolean = false) = data[key]?.bool ?: defaultVal
    operator fun set(key:Int, value:Boolean) {data[key] = value.f}
    fun float(key:Int, defaultVal:Float = 0f) = data[key] ?: defaultVal
    operator fun set(key:Int, value:Float) {data[key] = value}
//    IMGUI_API void*     GetVoidPtr(ImGuiID key) const; // default_val is NULL
//    IMGUI_API void      SetVoidPtr(ImGuiID key, void* val);

    // - Get***Ref() functions finds pair, insert on demand if missing, return pointer. Useful if you intend to do Get+Set.
    // - References are only valid until a new value is added to the storage. Calling a Set***() function or a Get***Ref() function invalidates the pointer.
    // - A typical use case where this is convenient for quick hacking (e.g. add storage during a live Edit&Continue session if you can't modify existing struct)
    //      float* pvar = ImGui::GetFloatRef(key); ImGui::SliderFloat("var", pvar, 0, 100.0f); some_var += *pvar;
//    IMGUI_API int*      GetIntRef(ImGuiID key, int default_val = 0);
//    IMGUI_API bool*     GetBoolRef(ImGuiID key, bool default_val = false);
//    IMGUI_API float*    GetFloatRef(ImGuiID key, float default_val = 0.0f);
//    IMGUI_API void**    GetVoidPtrRef(ImGuiID key, void* default_val = NULL);

    /** Use on your own storage if you know only integer are being stored (open/close all tree nodes)   */
    fun setAllInt(value: Int) = data.replaceAll { i, f -> value.f }
}

class TextEditCallbackData {
    init {
        TODO()
    }
}

class SizeConstraintCallbackData {
    init {
        TODO()
    }
}

class Color {
    init {
        TODO()
    }
}

class ListClipper {
    init {
        TODO()
    }
}