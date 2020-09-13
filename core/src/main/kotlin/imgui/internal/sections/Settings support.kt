package imgui.internal.sections

import glm_.vec2.Vec2
import imgui.ID
import imgui.internal.classes.*
import imgui.internal.hash

/** Storage for a window .ini settings (we keep one of those even if the actual window wasn't instanced during this session)
 *
 *  Because we never destroy or rename ImGuiWindowSettings, we can store the names in a separate buffer easily.
 *  [JVM] We prefer keeping the `name` variable
 *
 *  ~ CreateNewWindowSettings */
class WindowSettings(val name: String = "") {
    var id: ID = hash(name)
    var pos = Vec2()
    var size = Vec2()
    var collapsed = false
    /** Set when loaded from .ini data (to enable merging/loading .ini data into an already running context) */
    var wantApply = false
}

/** Storage for one type registered in the .ini file */
class SettingsHandler {
    /** Short description stored in .ini file. Disallowed characters: '[' ']' */
    var typeName = ""
    /** == ImHashStr(TypeName) */
    var typeHash: ID = 0

    var clearAllFn: ClearAllFn? = null
    var applyAllFn: ApplyAllFn? = null
    lateinit var readOpenFn: ReadOpenFn
    lateinit var readLineFn: ReadLineFn
    lateinit var writeAllFn: WriteAllFn
    var userData: Any? = null
}