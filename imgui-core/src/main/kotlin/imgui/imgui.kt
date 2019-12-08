package imgui

import glm_.i
import imgui.api.*
import imgui.api.dragAndDrop
import imgui.api.loggingCapture
import imgui.classes.Rect
import imgui.internal.ItemStatusFlags
import imgui.internal.api.*
import kool.Stack


// Version
const val IMGUI_BUILD = 0
/** get the compiled version string e.g. "1.23" (essentially the compiled value for IMGUI_VERSION) */
const val IMGUI_VERSION = "1.74 WIP"// build: $IMGUI_BUILD"
/** Integer encoded as XYYZZ for use in #if preprocessor conditionals.
Work in progress versions typically starts at XYY99 then bounce up to XYY00, XYY01 etc. when release tagging happens) */
const val IMGUI_VERSION_NUM = 17301


// Helpers macros to generate 32-bits encoded colors
@JvmField
var USE_BGRA_PACKED_COLOR = false

@JvmField
val COL32_R_SHIFT = lazy { if (USE_BGRA_PACKED_COLOR) 16 else 0 }.value
val COL32_G_SHIFT = 8
@JvmField
val COL32_B_SHIFT = lazy { if (USE_BGRA_PACKED_COLOR) 0 else 16 }.value
val COL32_A_SHIFT = 24
@JvmField
val COL32_A_MASK = 0xFF000000.i

fun COL32(r: Int, g: Int, b: Int, a: Int) = (a shl COL32_A_SHIFT) or (b shl COL32_B_SHIFT) or (g shl COL32_G_SHIFT) or (r shl COL32_R_SHIFT)
@JvmField
val COL32_WHITE = COL32(255, 255, 255, 255) // Opaque white = 0xFFFFFFFF
@JvmField
val COL32_BLACK = COL32(0, 0, 0, 255)       // Opaque black
@JvmField
val COL32_BLACK_TRANS = COL32(0, 0, 0, 0)   // Transparent black = 0x00000000

const val MOUSE_INVALID = -256000f

// Debug options

/** Display navigation scoring preview when hovering items. Display last moving direction matches when holding CTRL */
@JvmField
val IMGUI_DEBUG_NAV_SCORING = false
/** Display the reference navigation rectangle for each window */
@JvmField
val IMGUI_DEBUG_NAV_RECTS = false
/** Save additional comments in .ini file */
@JvmField
val IMGUI_DEBUG_INI_SETTINGS = false

// When using CTRL+TAB (or Gamepad Square+L/R) we delay the visual a little in order to reduce visual noise doing a fast switch.

/** Time before the highlight and screen dimming starts fading in */
const val NAV_WINDOWING_HIGHLIGHT_DELAY = 0.2f
/** Time before the window list starts to appear */
const val NAV_WINDOWING_LIST_APPEAR_DELAY = 0.15f

// Window resizing from edges (when io.configWindowsResizeFromEdges = true and BackendFlag.HasMouseCursors is set in io.backendFlags by back-end)

/** Extend outside and inside windows. Affect FindHoveredWindow(). */
const val WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS = 4f
/** Reduce visual noise by only highlighting the border after a certain time. */
const val WINDOWS_RESIZE_FROM_EDGES_FEEDBACK_TIMER = 0.04f
/** Lock scrolled window (so it doesn't pick child windows that are scrolling through) for a certaint time, unless mouse moved. */
const val WINDOWS_MOUSE_WHEEL_SCROLL_LOCK_TIMER = 2f

// Test engine hooks (imgui-test)
@JvmField
var IMGUI_ENABLE_TEST_ENGINE = false
@JvmField
var IMGUI_DEBUG_TOOL_ITEM_PICKER_EX = false
@JvmField
var ImGuiTestEngineHook_PreNewFrame: () -> Unit = {}
@JvmField
var ImGuiTestEngineHook_PostNewFrame: () -> Unit = {}
/** Register item bounding box */
@JvmField
var ImGuiTestEngineHook_ItemAdd: (bb: Rect, id: ID) -> Unit = { _, _ -> }
/** Register item label and status flags (optional) */
@JvmField
var ImGuiTestEngineHook_ItemInfo: (id: ID, label: String, flags: ItemStatusFlags) -> Unit = { _, _, _ -> }
@JvmField
        /** Custom log entry from user land into test log */
var ImGuiTestEngineHook_Log: (g: Context, /*vararg*/ fmt: String) -> Unit = { g, fmt -> }

@JvmField
var MINECRAFT_BEHAVIORS = false

object ImGui :
//-----------------------------------------------------------------------------
// ImGui: Dear ImGui end-user API
// (Inside a namespace so you can add extra functions in your own separate file. Please don't modify imgui source files!)
//-----------------------------------------------------------------------------
// context doesnt exist, only Context class
        main,
        demoDebugInformations,
        styles,
        windows,
        childWindows,
        windowsUtilities,
        contentRegion,
        windowScrolling,
        parametersStacks,
        cursorLayout,
        idStackScopes,
        widgetsText,
        widgetsMain,
        widgetsComboBox,
        widgetsDrags,
        widgetsSliders,
        widgetsInputWithKeyboard,
        widgetsColorEditorPicker,
        widgetsTrees,
        widgetsSelectables,
        widgetsListBoxes,
        widgetsDataPlotting,
        // value
        widgetsMenus,
        tooltips,
        popupsModals,
        columns,
        tabBarsTabs,
        loggingCapture,
        dragAndDrop,
        clipping,
        focusActivation,
        itemWidgetsUtilities,
        miscellaneousUtilities,
        colorUtilities,
        inputsUtilities,
        clipboardUtilities,
        settingsIniUtilities,

//-----------------------------------------------------------------------------
// Internal API
// No guarantee of forward compatibility here.
//-----------------------------------------------------------------------------
        internal,
        // init in Context class
        newFrame,
        settings,
        basicAccessors,
        basicHelpersForWidgetCode,
        imgui.internal.api.loggingCapture,
        PopupsModalsTooltips,
        navigation,
        inputs,
        imgui.internal.api.dragAndDrop,
        newColumnsAPI,
        tabBars,
        renderHelpers,
        widgets,
        widgetsLowLevelBehaviors,
        templateFunctions,
        dataTypeHelpers,
        inputText,
        color,
        plot,
        // shade functions in DrawList class
        debugTools

internal var ptrIndices = 0
internal var ptrId = Array(512) { it } // it was: java.lang.Byte.valueOf(it.b)

// TODO get rid of local top value KMutableProperty in favor of the better with*{} solution


const val NUL = '\u0000'

@JvmField
var DEBUG = true

operator fun StringBuilder.plusAssign(string: String) {
    append(string)
}

// Typedefs and Enumerations (declared as int for compatibility and to not pollute the top of this file)

/** Unique ID used by widgets (typically hashed from a stack of string) */
typealias ID = Int

/** User data to identify a texture */
typealias TextureID = Int

/** flags: for DrawList::addRect*() etc.   // enum DrawCornerFlag */
typealias DrawCornerFlags = Int

/** flags: for DrawList                    // enum DrawListFlag */
typealias DrawListFlags = Int

/** flags: for FontAtlas                   // enum FontAtlasFlags */
typealias FontAtlasFlags = Int
//typedef int ImGuiBackendFlags;      // flags: for io.BackendFlag               // enum ImGuiBackendFlags_
/** flags: for ColorEdit*(), ColorPicker*()  // enum ColorEditFlag */
typealias ColorEditFlags = Int

/** flags: for *Columns*()                   // enum ColumnsFlag */
typealias ColumnsFlags = Int

/** flags: for io.ConfigFlags                // enum ConfigFlag */
typealias ConfigFlags = Int

/** flags: for *DragDrop*()                  // enum DragDropFlag */
typealias DragDropFlags = Int

/** flags: for BeginCombo()                  // enum ComboFlag */
typealias ComboFlags = Int

/** flags: for ::isWindowFocused()             // enum FocusedFlag */
typealias FocusedFlags = Int

/** flags: for ::isItemHovered() etc.          // enum HoveredFlag */
typealias HoveredFlags = Int

/** flags: for ::inputText*()                  // enum InputTextFlag */
typealias InputTextFlags = Int

/** flags: for Selectable()                  // enum SelectableFlag */
typealias SelectableFlags = Int

/** Flags: for BeginTabBar() */
typealias TabBarFlags = Int                 // enum ImGuiTabBarFlags_

/** Flags: for BeginTabItem() */
typealias TabItemFlags = Int                // enum ImGuiTabItemFlags_

/** flags: for TreeNode*(),CollapsingHeader()// enum TreeNodeFlag */
typealias TreeNodeFlags = Int

/** flags: for Begin*()                      // enum WindowFlag */
typealias WindowFlags = Int

typealias InputTextCallback = (InputTextCallbackData) -> Boolean
typealias SizeCallback = (SizeCallbackData) -> Unit

typealias TextEditCallbackData = InputTextCallbackData

infix fun String.cmp(charArray: CharArray): Boolean {
    for (i in indices)
        if (get(i) != charArray[i])
            return false
    return true
}

infix fun CharArray.cmp(other: CharArray): Boolean {
    for (i in indices) {
        val a = get(i)
        val b = other.getOrElse(i) { return false }
        if (a == NUL)
            return b == NUL
        if (a != b)
            return false
    }
    return true
}

internal typealias stak = Stack

fun IM_DEBUG_BREAK() {

}