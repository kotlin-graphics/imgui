package imgui

import glm_.i
import imgui.api.*
import imgui.api.docking
import imgui.api.dragAndDrop
import imgui.api.loggingCapture
import imgui.classes.Context
import imgui.internal.sections.ItemStatusFlags
import imgui.internal.api.*
import imgui.internal.classes.Rect
import kool.Stack


// Version
const val IMGUI_BUILD = 0

/** get the compiled version string e.g. "1.23" (essentially the compiled value for IMGUI_VERSION) */
const val IMGUI_VERSION = "1.80 WIP"
const val IMGUI_VERSION_BUILD = "$IMGUI_VERSION.$IMGUI_BUILD"
/** Integer encoded as XYYZZ for use in #if preprocessor conditionals.
Work in progress versions typically starts at XYY99 then bounce up to XYY00, XYY01 etc. when release tagging happens) */
const val IMGUI_VERSION_NUM = 17905

/** Viewport WIP branch */
var IMGUI_HAS_VIEWPORT = true

/** Docking WIP branch */
var IMGUI_HAS_DOCK = true


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

fun COL32(i: Int) = COL32(i, i, i, i)
fun COL32(r: Int, g: Int, b: Int, a: Int) = (a shl COL32_A_SHIFT) or (b shl COL32_B_SHIFT) or (g shl COL32_G_SHIFT) or (r shl COL32_R_SHIFT)

@JvmField
val COL32_WHITE = COL32(255) // Opaque white = 0xFFFFFFFF

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

/** Save additional comments in .ini file (particularly helps for Docking, but makes saving slower) */
@JvmField
val IMGUI_DEBUG_INI_SETTINGS = false

// When using CTRL+TAB (or Gamepad Square+L/R) we delay the visual a little in order to reduce visual noise doing a fast switch.

/** Time before the highlight and screen dimming starts fading in */
const val NAV_WINDOWING_HIGHLIGHT_DELAY = 0.2f

/** Time before the window list starts to appear */
const val NAV_WINDOWING_LIST_APPEAR_DELAY = 0.15f

// Window resizing from edges (when io.configWindowsResizeFromEdges = true and BackendFlag.HasMouseCursors is set in io.backendFlags by backend)

/** Extend outside and inside windows. Affect FindHoveredWindow(). */
const val WINDOWS_RESIZE_FROM_EDGES_HALF_THICKNESS = 4f

/** Reduce visual noise by only highlighting the border after a certain time. */
const val WINDOWS_RESIZE_FROM_EDGES_FEEDBACK_TIMER = 0.04f

/** Lock scrolled window (so it doesn't pick child windows that are scrolling through) for a certain time, unless mouse moved. */
const val WINDOWS_MOUSE_WHEEL_SCROLL_LOCK_TIMER = 2f


// Docking

/** For use with io.ConfigDockingTransparentPayload. Apply to Viewport _or_ WindowBg in host viewport. */
const val DOCKING_TRANSPARENT_PAYLOAD_ALPHA = 0.5f
const val DOCKING_SPLITTER_SIZE = 2f


// Test engine hooks (imgui-test)
val IMGUI_ENABLE_TEST_ENGINE: Boolean
    get() = g.testEngineHookItems

@JvmField
var IMGUI_DEBUG_TOOL_ITEM_PICKER_EX = false


//-----------------------------------------------------------------------------
// [SECTION] Test Engine specific hooks (imgui_test_engine)
//-----------------------------------------------------------------------------

// TODO rename closer to cpp?

typealias Hook_ItemAdd = (Context, Rect, ID) -> Unit
typealias Hook_ItemInfo = (Context, ID, String, ItemStatusFlags) -> Unit
typealias Hook_IdInfo = (Context, DataType, ID, Any?) -> Unit
typealias Hook_IdInfo2 = (Context, DataType, ID, Any?, Int) -> Unit
typealias Hook_Log = (Context, String) -> Unit

object Hook {

    /** Register item bounding box
     *  ~ImGuiTestEngineHook_ItemAdd */
    var itemAdd: Hook_ItemAdd? = null

    /** Register item label and status flags (optional)
     *  ~ImGuiTestEngineHook_ItemInfo */
    var itemInfo: Hook_ItemInfo? = null

    /** ~ImGuiTestEngineHook_IdInfo */
    var idInfo: Hook_IdInfo? = null

    /** ~ImGuiTestEngineHook_IdInfo */
    var idInfo2: Hook_IdInfo2? = null

    /** Custom log entry from user land into test log
     *  ~ImGuiTestEngineHook_Log */
    var log: Hook_Log? = null
}


// Helper: Unicode defines

/** Last Unicode code point supported by this build.
 *
 *  Maximum Unicode code point supported by this build. */
const val UNICODE_CODEPOINT_MAX = /*sizeof(ImWchar) == 2 ?*/ 0xFFFF /*: 0x10FFFF*/

/** Standard invalid Unicode code point.
 *
 *  Invalid Unicode code point (standard value). */
const val UNICODE_CODEPOINT_INVALID = 0xFFFD

@JvmField
var MINECRAFT_BEHAVIORS = false

object ImGui :
//-----------------------------------------------------------------------------
// ImGui: Dear ImGui end-user API
// (This is a namespace. You can add extra ImGui:: functions in your own separate file. Please don't modify imgui source files!)
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
        docking,
        loggingCapture,
        dragAndDrop,
        clipping,
        focusActivation,
        itemWidgetsUtilities,
        miscellaneousUtilities,
        textUtilities,
        colorUtilities,
        inputUtilitiesKeyboard,
        inputUtilitiesMouse,
        clipboardUtilities,
        settingsIniUtilities,
        platformOS,

//-----------------------------------------------------------------------------
// Internal API
// No guarantee of forward compatibility here.
//-----------------------------------------------------------------------------
        internal,
        // init in Context class
        newFrame,
        genericContextHooks,
        vieports,
        settings,
        basicAccessors,
        basicHelpersForWidgetCode,
        imgui.internal.api.loggingCapture,
        PopupsModalsTooltips,
        navigation,
        focusScope,
        inputs,
        imgui.internal.api.docking,
        dockingBuilder,
        imgui.internal.api.dragAndDrop,
        internalColumnsAPI,
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


@JvmField
var DEBUG = true

internal typealias stak = Stack

fun IM_DEBUG_BREAK() {}

// Internal Drag and Drop payload types. String starting with '_' are reserved for Dear ImGui.
val IMGUI_PAYLOAD_TYPE_WINDOW = "_IMWINDOW"     // Payload == ImGuiWindow*

// Debug Logging
fun IMGUI_DEBUG_LOG(fmt: String) = println("[%05d] $fmt".format(g.frameCount))

// Debug Logging for selected systems. Remove the '((void)0) //' to enable.
//fun IMGUI_DEBUG_LOG_POPUP(string: String) = Unit
//fun IMGUI_DEBUG_LOG_VIEWPORT(string: String) = Unit
//fun IMGUI_DEBUG_LOG_DOCKING(string: String) = Unit
fun IMGUI_DEBUG_LOG_POPUP(string: String) = IMGUI_DEBUG_LOG(string)
fun IMGUI_DEBUG_LOG_VIEWPORT(string: String) = IMGUI_DEBUG_LOG(string)
fun IMGUI_DEBUG_LOG_DOCKING(string: String) = IMGUI_DEBUG_LOG(string)