package imgui.classes

import glm_.vec2.Vec2
import glm_.vec2.Vec2i
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.styleColorsClassic
import imgui.internal.floor
import java.util.*
import kotlin.collections.ArrayList

//-----------------------------------------------------------------------------
// [SECTION] ImGuiStyle
//-----------------------------------------------------------------------------
/** You may modify the ImGui::GetStyle() main instance during initialization and before NewFrame().
 *  During the frame, use ImGui::PushStyleVar(ImGuiStyleVar_XXXX)/PopStyleVar() to alter the main style values,
 *  and ImGui::PushStyleColor(ImGuiCol_XXX)/PopStyleColor() for colors. */
class Style {

    /**  Global alpha applies to everything in Dear ImGui.    */
    var alpha = 1f

    /** Additional alpha multiplier applied by BeginDisabled(). Multiply over current value of Alpha. */
    var disabledAlpha = 0.6f

    /** Padding within a window. */
    var windowPadding = Vec2(8)

    /** Radius of window corners rounding. Set to 0.0f to have rectangular windows. Large values tend to lead to variety of artifacts and are not recommended.  */
    var windowRounding = 0f

    /** Thickness of border around windows. Generally set to 0f or 1f. (Other values are not well tested and more CPU/GPU costly).  */
    var windowBorderSize = 1f

    /** Minimum window size. This is a global setting. If you want to constrain individual windows, use SetNextWindowSizeConstraints(). */
    var windowMinSize = Vec2(32)

    /** Alignment for title bar text    */
    var windowTitleAlign = Vec2(0f, 0.5f)

    /** Side of the collapsing/docking button in the title bar (None/Left/Right). Defaults to ImGuiDir_Left. */
    var windowMenuButtonPosition = Dir.Left

    /** Radius of child window corners rounding. Set to 0.0f to have rectangular child windows.  */
    var childRounding = 0f

    /** Thickness of border around child windows. Generally set to 0f or 1f. (Other values are not well tested and more CPU/GPU costly). */
    var childBorderSize = 1f

    /** Radius of popup window corners rounding. (Note that tooltip windows use WindowRounding) */
    var popupRounding = 0f

    /** Thickness of border around popup/tooltip windows. Generally set to 0f or 1f.
     *  (Other values are not well tested and more CPU/GPU costly). */
    var popupBorderSize = 1f

    /** Padding within a framed rectangle (used by most widgets).    */
    var framePadding = Vec2(4, 3)

    /** Radius of frame corners rounding. Set to 0.0f to have rectangular frames (used by most widgets).    */
    var frameRounding = 0f

    /** Thickness of border around frames. Generally set to 0f or 1f. (Other values are not well tested and more CPU/GPU costly).    */
    var frameBorderSize = 0f

    /** Horizontal and vertical spacing between widgets/lines.   */
    var itemSpacing = Vec2(8, 4)

    /** Horizontal and vertical spacing between within elements of a composed widget (e.g. a slider and its label).  */
    var itemInnerSpacing = Vec2(4)

    /** Padding within a table cell */
    val cellPadding = Vec2(4, 2)

    /** Expand reactive bounding box for touch-based system where touch position is not accurate enough. Unfortunately
     *  we don't sort widgets so priority on overlap will always be given to the first widget. So don't grow this too much!   */
    var touchExtraPadding = Vec2()

    /** Horizontal spacing when e.g. entering a tree node. Generally == (FontSize + FramePadding.x*2).  */
    var indentSpacing = 21f

    /** Minimum horizontal spacing between two columns. Preferably > (FramePadding.x + 1).  */
    var columnsMinSpacing = 6f

    /** Width of the vertical scrollbar, Height of the horizontal scrollbar. */
    var scrollbarSize = 14f

    /** Radius of grab corners rounding for scrollbar.   */
    var scrollbarRounding = 9f

    /** Minimum width/height of a grab box for slider/scrollbar */
    var grabMinSize = 12f

    /** Radius of grabs corners rounding. Set to 0.0f to have rectangular slider grabs. */
    var grabRounding = 0f

    /** The size in pixels of the dead-zone around zero on logarithmic sliders that cross zero. */
    var logSliderDeadzone = 4f

    /** Radius of upper corners of a tab. Set to 0.0f to have rectangular tabs. */
    var tabRounding = 4f

    /** Thickness of border around tabs. */
    var tabBorderSize = 0f

    /** Minimum width for close button to appear on an unselected tab when hovered. Set to 0.0f to always show when hovering, set to FLT_MAX to never show close button unless selected. */
    var tabMinWidthForCloseButton = 0f

    /** Side of the color button in the ColorEdit4 widget (left/right). Defaults to ImGuiDir_Right. */
    var colorButtonPosition = Dir.Right

    /** Alignment of button text when button is larger than text. Defaults to (0.5f, 0.5f) (centered).   */
    var buttonTextAlign = Vec2(0.5f)

    /** Alignment of selectable text. Defaults to (0.0f, 0.0f) (top-left aligned). It's generally important to keep this left-aligned if you want to lay multiple items on a same line. */
    var selectableTextAlign = Vec2()

    /** Thickkness of border in SeparatorText() */
    var separatorTextBorderSize = 3f

    /** Alignment of text within the separator. Defaults to (0.0f, 0.5f) (left aligned, center). */
    val separatorTextAlign = Vec2(0f, 0.5f)

    /** Horizontal offset of text from each edge of the separator + spacing on other axis. Generally small values. .y is recommended to be == FramePadding.y. */
    val separatorTextPadding = Vec2(20f, 3f)

    /** Window position are clamped to be visible within the display area or monitors by at least this amount.
     *  Only applies to regular windows.    */
    var displayWindowPadding = Vec2(19)

    /** If you cannot see the edges of your screen (e.g. on a TV) increase the safe area padding. Apply to popups/tooltips
     *  as well regular windows.  NB: Prefer configuring your TV sets correctly!   */
    var displaySafeAreaPadding = Vec2(3)

    /** Scale software rendered mouse cursor (when io.MouseDrawCursor is enabled). May be removed later.    */
    var mouseCursorScale = 1f

    /** Enable anti-aliased on lines/borders. Disable if you are really tight on CPU/GPU. Latched at the beginning of the frame (copied to ImDrawList).
     *
     *  Draw anti-aliased lines using textures where possible. */
    var antiAliasedLines = true

    /** Enable anti-aliased lines/borders using textures where possible. Require backend to render with
     *  bilinear filtering (NOT point/nearest filtering). Latched at the beginning of the frame (copied to ImDrawList). */
    var antiAliasedLinesUseTex = true

    /**  Enable anti-aliased on filled shapes (rounded rectangles, circles, etc.).. Disable if you are really tight
     *  on CPU/GPU. Latched at the beginning of the frame (copied to ImDrawList). */
    var antiAliasedFill = true

    /** Tessellation tolerance when using pathBezierCurveTo() without a specific number of segments.
     *  Decrease for highly tessellated curves (higher quality, more polygons), increase to reduce quality. */
    var curveTessellationTol = 1.25f

    /** Maximum error (in pixels) allowed when using AddCircle()/AddCircleFilled() or drawing rounded corner rectangles
     *  with no explicit segment count specified. Decrease for higher quality but more geometry. */
    var circleTessellationMaxError = 0.30f

    val colors = ArrayList<Vec4>()

    // Behaviors
    // (It is possible to modify those fields mid-frame if specific behavior need it, unlike e.g. configuration fields in ImGuiIO)

    /** Delay for IsItemHovered(ImGuiHoveredFlags_Stationary). Time required to consider mouse stationary. */
    var hoverStationaryDelay = 0.15f

    /** Delay for IsItemHovered(ImGuiHoveredFlags_DelayShort). Usually used along with HoverStationaryDelay. */
    var hoverDelayShort = 0.15f

    /** Delay for IsItemHovered(ImGuiHoveredFlags_DelayNormal). " */
    var hoverDelayNormal = 0.4f

    /** Default flags when using IsItemHovered(ImGuiHoveredFlags_ForTooltip) or BeginItemTooltip()/SetItemTooltip() while using mouse. */
    var hoverFlagsForTooltipMouse: HoveredFlags = HoveredFlag.Stationary / HoveredFlag.DelayShort

    /** Default flags when using IsItemHovered(ImGuiHoveredFlags_ForTooltip) or BeginItemTooltip()/SetItemTooltip() while using keyboard/gamepad. */
    var hoverFlagsForTooltipNav: HoveredFlags = HoveredFlag.NoSharedDelay / HoveredFlag.DelayNormal

    /** JVM IMGUI   */
    var locale: Locale = Locale.US
//    var locale: Locale = Locale("no", "NO")
//    val locale = Locale.getDefault()

    // [JVM]
    fun copy() = Style().also {
        it.alpha = alpha; it.disabledAlpha = disabledAlpha; it.windowPadding put windowPadding; it.windowRounding = windowRounding; it.windowBorderSize = windowBorderSize
        it.windowMinSize put windowMinSize; it.windowTitleAlign put windowTitleAlign; it.windowMenuButtonPosition = windowMenuButtonPosition; it.childRounding = childRounding
        it.childBorderSize = childBorderSize; it.popupRounding = popupRounding; it.popupBorderSize = popupBorderSize; it.framePadding put framePadding; it.frameRounding = frameRounding
        it.frameBorderSize = frameBorderSize; it.itemSpacing put itemSpacing; it.itemInnerSpacing put itemInnerSpacing; it.cellPadding put cellPadding
        it.touchExtraPadding put touchExtraPadding; it.indentSpacing = indentSpacing; it.columnsMinSpacing = columnsMinSpacing; it.scrollbarSize = scrollbarSize
        it.scrollbarRounding = scrollbarRounding; it.grabMinSize = grabMinSize; it.grabRounding = grabRounding; it.logSliderDeadzone = logSliderDeadzone; it.tabRounding = tabRounding
        it.tabBorderSize = tabBorderSize; it.tabMinWidthForCloseButton = tabMinWidthForCloseButton; it.colorButtonPosition = colorButtonPosition; it.buttonTextAlign put buttonTextAlign
        it.selectableTextAlign put selectableTextAlign; it.displayWindowPadding put displayWindowPadding; it.displaySafeAreaPadding put displaySafeAreaPadding
        it.mouseCursorScale = mouseCursorScale; it.antiAliasedLines = antiAliasedLines; it.antiAliasedLinesUseTex = antiAliasedLinesUseTex; it.antiAliasedFill = antiAliasedFill
        it.curveTessellationTol = curveTessellationTol; it.circleTessellationMaxError = circleTessellationMaxError; it.colors += colors; it.locale = locale
        it.hoverStationaryDelay = hoverStationaryDelay; it.hoverDelayShort = hoverDelayShort; it.hoverDelayNormal = hoverDelayNormal
        it.hoverFlagsForTooltipMouse = hoverFlagsForTooltipMouse; it.hoverFlagsForTooltipNav = hoverFlagsForTooltipNav
    }

    init {
        styleColorsClassic(this)
    }

    constructor()

    constructor(style: Style) {
        alpha = style.alpha
        windowPadding put style.windowPadding
        windowRounding = style.windowRounding
        windowBorderSize = style.windowBorderSize
        windowMinSize put style.windowMinSize
        windowTitleAlign put style.windowTitleAlign
        windowMenuButtonPosition = style.windowMenuButtonPosition
        childRounding = style.childRounding
        childBorderSize = style.childBorderSize
        popupRounding = style.popupRounding
        popupBorderSize = style.popupBorderSize
        framePadding put style.framePadding
        frameRounding = style.frameRounding
        frameBorderSize = style.frameBorderSize
        itemSpacing put style.itemSpacing
        itemInnerSpacing put style.itemInnerSpacing
        cellPadding put style.cellPadding
        touchExtraPadding put style.touchExtraPadding
        indentSpacing = style.indentSpacing
        columnsMinSpacing = style.columnsMinSpacing
        scrollbarSize = style.scrollbarSize
        scrollbarRounding = style.scrollbarRounding
        grabMinSize = style.grabMinSize
        grabRounding = style.grabRounding
        logSliderDeadzone = style.logSliderDeadzone
        tabRounding = style.tabRounding
        tabBorderSize = style.tabBorderSize
        tabMinWidthForCloseButton = style.tabMinWidthForCloseButton
        colorButtonPosition = style.colorButtonPosition
        buttonTextAlign put style.buttonTextAlign
        selectableTextAlign put style.selectableTextAlign
        displayWindowPadding put style.displayWindowPadding
        displaySafeAreaPadding put style.displaySafeAreaPadding
        mouseCursorScale = style.mouseCursorScale
        antiAliasedLines = style.antiAliasedLines
        antiAliasedLinesUseTex = style.antiAliasedLinesUseTex
        antiAliasedFill = style.antiAliasedFill
        curveTessellationTol = style.curveTessellationTol
        circleTessellationMaxError = style.circleTessellationMaxError
        style.colors.forEach { colors += Vec4(it) }
//        locale = style.locale
    }

    /** To scale your entire UI (e.g. if you want your app to use High DPI or generally be DPI aware) you may use this
     *  helper function. Scaling the fonts is done separately and is up to you.
     *  Tips: if you need to change your scale multiple times, prefer calling this on a freshly initialized Style
     *  structure rather than scaling multiple times (because floating point multiplications are lossy).    */
    fun scaleAllSizes(scaleFactor: Float) {
        windowPadding = floor(windowPadding * scaleFactor)
        windowRounding = floor(windowRounding * scaleFactor)
        windowMinSize.put(floor(windowMinSize * scaleFactor))
        childRounding = floor(childRounding * scaleFactor)
        popupRounding = floor(popupRounding * scaleFactor)
        framePadding = floor(framePadding * scaleFactor)
        frameRounding = floor(frameRounding * scaleFactor)
        itemSpacing = floor(itemSpacing * scaleFactor)
        itemInnerSpacing = floor(itemInnerSpacing * scaleFactor)
        cellPadding put floor(cellPadding * scaleFactor)
        touchExtraPadding = floor(touchExtraPadding * scaleFactor)
        indentSpacing = floor(indentSpacing * scaleFactor)
        columnsMinSpacing = floor(columnsMinSpacing * scaleFactor)
        scrollbarSize = floor(scrollbarSize * scaleFactor)
        scrollbarRounding = floor(scrollbarRounding * scaleFactor)
        grabMinSize = floor(grabMinSize * scaleFactor)
        grabRounding = floor(grabRounding * scaleFactor)
        logSliderDeadzone = floor(logSliderDeadzone * scaleFactor)
        tabRounding = floor(tabRounding * scaleFactor)
        tabMinWidthForCloseButton = if (tabMinWidthForCloseButton != Float.MAX_VALUE) floor(tabMinWidthForCloseButton * scaleFactor) else Float.MAX_VALUE
        separatorTextPadding put floor(separatorTextPadding * scaleFactor)
        displayWindowPadding = floor(displayWindowPadding * scaleFactor)
        displaySafeAreaPadding = floor(displaySafeAreaPadding * scaleFactor)
        mouseCursorScale = floor(mouseCursorScale * scaleFactor)
    }
}