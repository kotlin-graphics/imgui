package imgui

import glm_.i
import glm_.vec2.Vec2
import imgui.api.*
import imgui.api.dragAndDrop
import imgui.api.loggingCapture
import imgui.internal.api.*
import imgui.internal.api.focusActivation

// Version
const val IMGUI_BUILD = 0

/** get the compiled version string e.g. "1.80 WIP" (essentially the value for IMGUI_VERSION from the compiled version of imgui.cpp) */
const val IMGUI_VERSION = "1.89.8 WIP"
const val IMGUI_VERSION_BUILD = "$IMGUI_VERSION.$IMGUI_BUILD"

/** Integer encoded as XYYZZ for use in #if preprocessor conditionals.
Work in progress versions typically starts at XYY99 then bounce up to XYY00, XYY01 etc. when release tagging happens) */
const val IMGUI_VERSION_NUM = 18971


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

// When using CTRL+TAB (or Gamepad Square+L/R) we delay the visual a little in order to reduce visual noise doing a fast switch.

/** Time before the highlight and screen dimming starts fading in */
const val NAV_WINDOWING_HIGHLIGHT_DELAY = 0.2f

/** Time before the window list starts to appear */
const val NAV_WINDOWING_LIST_APPEAR_DELAY = 0.15f

// Window resizing from edges (when io.configWindowsResizeFromEdges = true and BackendFlag.HasMouseCursors is set in io.backendFlags by backend)

/** Extend outside window for hovering/resizing (maxxed with TouchPadding) and inside windows for borders. Affect FindHoveredWindow(). */
const val WINDOWS_HOVER_PADDING = 4f

/** Reduce visual noise by only highlighting the border after a certain time. */
const val WINDOWS_RESIZE_FROM_EDGES_FEEDBACK_TIMER = 0.04f

/** Lock scrolled window (so it doesn't pick child windows that are scrolling through) for a certain time, unless mouse moved. */
const val WINDOWS_MOUSE_WHEEL_SCROLL_LOCK_TIMER = 0.7f

// Tooltip offset
var TOOLTIP_DEFAULT_OFFSET = Vec2(16, 10)            // Multiplied by g.Style.MouseCursorScale

// Test engine hooks (imgui-test)
var IMGUI_ENABLE_TEST_ENGINE = true

@JvmField
var IMGUI_DEBUG_TOOL_ITEM_PICKER_EX = false

var IMGUI_DISABLE_DEBUG_TOOLS = false


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
// [SECTION] Dear ImGui end-user API functions
// (Note that ImGui:: being a namespace, you can add extra ImGui:: functions in your own separate file. Please don't modify imgui source files!)
//-----------------------------------------------------------------------------
// context doesnt exist, only Context class
        main,
        demoDebugInformations,
        styles,
        imgui.api.windows,
        childWindows,
        windowsUtilities,
        contentRegion,
        windowScrolling,
        parametersStacks,
        styleReadAccess,
        cursorLayout,
        idStackScopes,
        imgui.api.viewports,
        widgetsText,
        widgetsMain,
        widgetsImages,
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
        tables,
        columns,
        tabBarsTabs,
        loggingCapture,
        dragAndDrop,
        clipping,
        imgui.api.focusActivation,
        overlappingMode,
        itemWidgetsUtilities,
        backgroundForegroundDrawLists,
        miscellaneousUtilities,
        textUtilities,
        colorUtilities,
        inputsUtilitiesKeyboardMouseGamepad,
        inputsUtilitiesShortcutRouting,
        inputUtilitiesMouse,
        clipboardUtilities,
        settingsIniUtilities,
        debugUtilities,

        //-----------------------------------------------------------------------------
        // Internal API
        // No guarantee of forward compatibility here.
        //-----------------------------------------------------------------------------
        imgui.internal.api.windows,
        windowsDisplayAndFocusOrder,
        fontsDrawing,
        // init in Context class
        newFrame,
        genericContextHooks,
        imgui.internal.api.viewports,
        settings,
        settingsWindows,
        localization,
        scrolling,
        basicAccessors,
        basicHelpersForWidgetCode,
        parameterStacks,
        imgui.internal.api.loggingCapture,
        popupsModalsTooltips,
        menus,
        combos,
        gamepadKeyboardNavigation,
        focusActivation,
        inputs,
        focusScope,
        imgui.internal.api.dragAndDrop,
        disabling,
        internalColumnsAPI,
        tablesCandidatesForPublicAPI,
        tablesInternal,
        tableSettings,
        tabBars,
        renderHelpers,
        widgets,
        widgetsWindowDecorations,
        widgetsLowLevelBehaviors,
        templateFunctions,
        inputText,
        color,
        shadeFunctions,
        garbageCollection,
        debugLog,
        debugTools


@JvmField
var DEBUG = false

fun IM_DEBUG_BREAK() {}