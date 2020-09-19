package imgui.internal.sections

import glm_.vec2.Vec2
import imgui.ID
import imgui.classes.Context
import imgui.internal.classes.*
import imgui.internal.hash
import java.lang.StringBuilder

/** Storage for a window .ini settings (we keep one of those even if the actual window wasn't instanced during this session)
 *
 *  Because we never destroy or rename ImGuiWindowSettings, we can store the names in a separate buffer easily.
 *  [JVM] We prefer keeping the `name` variable
 *
 *  ~ CreateNewWindowSettings */
class WindowSettings(val name: String = "") {
    var id: ID = hash(name)

    /** NB: Settings position are stored RELATIVE to the viewport! Whereas runtime ones are absolute positions. */
    val pos = Vec2()
    val size = Vec2()

    val viewportPos = Vec2()
    var viewportId: ID = 0

    /** ID of last known DockNode (even if the DockNode is invisible because it has only 1 active window), or 0 if none. */
    var dockId: ID = 0

    /** ID of window class if specified */
    var classId: ID = 0

    /** Order of the last time the window was visible within its DockNode. This is used to reorder windows that are reappearing on the same frame. Same value between windows that were active and windows that were none are possible. */
    var dockOrder = -1
    var collapsed = false
    /** Set when loaded from .ini data (to enable merging/loading .ini data into an already running context) */
    var wantApply = false

    fun clear() {
        id = 0
        pos put 0f
        size put 0f
        viewportPos put 0f
        viewportId = 0
        dockId = 0
        classId = 0
        dockOrder = -1
        collapsed = false
        wantApply = false
    }
}

/** Clear all settings data */
typealias ClearAllFn = (ctx: Context, handler: SettingsHandler) -> Unit

/** Read: Called before reading (in registration order) */
typealias ReadInitFn = (ctx: Context, handler: SettingsHandler) -> Unit

/** Read: Called when entering into a new ini entry e.g. "[Window][Name]" */
typealias ReadOpenFn = (ctx: Context, handler: SettingsHandler, name: String) -> Any?

/** Read: Called for every line of text within an ini entry */
typealias ReadLineFn = (ctx: Context, handler: SettingsHandler, entry: Any, line: String) -> Unit

/** Read: Called after reading (in registration order) */
typealias ApplyAllFn = (ctx: Context, handler: SettingsHandler) -> Unit

/** Write: Output every entries into 'out_buf' */
typealias WriteAllFn  = (ctx: Context, handler: SettingsHandler, outBuf: StringBuilder) -> Unit

/** Storage for one type registered in the .ini file */
class SettingsHandler {
    /** Short description stored in .ini file. Disallowed characters: '[' ']' */
    var typeName = ""
    /** == ImHashStr(TypeName) */
    var typeHash: ID = 0

    var clearAllFn: ClearAllFn? = null
    var readInitFn: ReadInitFn? = null
    lateinit var readOpenFn: ReadOpenFn
    lateinit var readLineFn: ReadLineFn
    var applyAllFn: ApplyAllFn? = null
    lateinit var writeAllFn: WriteAllFn
    var userData: Any? = null
}