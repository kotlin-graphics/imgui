package plot.api

import com.livefront.sealedenum.GenSealedEnum
import glm_.has
import glm_.hasnt
import glm_.vec2.Vec2
import glm_.vec2.Vec2d
import glm_.vec4.Vec4
import imgui.*

//-----------------------------------------------------------------------------
// [SECTION] Enums and Types
//-----------------------------------------------------------------------------

// Forward declarations
//struct ImPlotContext;             // ImPlot context (opaque struct, see implot_internal.h)

// Enums/Flags

// Axis indices. The values assigned may change; NEVER hardcode these.
enum class Axis {
    // horizontal axes
    X1, // enabled by default
    X2,     // disabled by default
    X3,     // disabled by default

    // vertical axes
    Y1,     // enabled by default
    Y2,     // disabled by default
    Y3;     // disabled by default

    val i = ordinal

    companion object {
        // bookeeping
        val COUNT = values().size
        infix fun of(i: Int) = values().first { it.i == i }
    }
}

operator fun <T> Array<T>.get(index: Axis) = get(index.i)

typealias PlotFlags = Flag<PlotFlag>              // -> enum ImPlotFlags_

// Options for plots (see BeginPlot).
sealed class PlotFlag : FlagBase<PlotFlag>() {
    //    ImPlotFlags_None          = 0,       // default
    object NoTitle : PlotFlag()  // the plot title will not be displayed (titles are also hidden if preceeded by double hashes, e.g. "##MyPlot")
    object NoLegend : PlotFlag()  // the legend will not be displayed
    object NoMouseText : PlotFlag()  // the mouse position, in plot coordinates, will not be displayed inside of the plot
    object NoInputs : PlotFlag()  // the user will not be able to interact with the plot
    object NoMenus : PlotFlag()  // the user will not be able to open context menus
    object NoBoxSelect : PlotFlag()  // the user will not be able to box-select
    object NoChild : PlotFlag()  // a child window region will not be used to capture mouse scroll (can boost performance for single ImGui window applications)
    object NoFrame : PlotFlag()  // the ImGui frame will not be rendered
    object Equal : PlotFlag()  // x and y axes pairs will be constrained to have the same units/pixel
    object Crosshairs : PlotFlag()  // the default mouse cursor will be replaced with a crosshair when hovered

    override val i: Int = 1 shl ordinal

    @GenSealedEnum companion object {
        val CanvasOnly = NoTitle / NoLegend / NoMenus / NoBoxSelect / NoMouseText
    }
}

typealias PlotAxisFlags = Flag<PlotAxisFlag>          // -> enum ImPlotAxisFlags_

// Options for plot axes (see SetupAxis).
sealed class PlotAxisFlag : FlagBase<PlotAxisFlag>() {
    //    ImPlotAxisFlags_None          = 0,       // default
    object NoLabel : PlotAxisFlag()  // the axis label will not be displayed (axis labels are also hidden if the supplied string name is nullptr)
    object NoGridLines : PlotAxisFlag()  // no grid lines will be displayed
    object NoTickMarks : PlotAxisFlag()  // no tick marks will be displayed
    object NoTickLabels : PlotAxisFlag()  // no text labels will be displayed
    object NoInitialFit : PlotAxisFlag()  // axis will not be initially fit to data extents on the first rendered frame
    object NoMenus : PlotAxisFlag()  // the user will not be able to open context menus with right-click
    object NoSideSwitch : PlotAxisFlag()  // the user will not be able to switch the axis side by dragging it
    object NoHighlight : PlotAxisFlag()  // the axis will not have its background highlighted when hovered or held
    object Opposite : PlotAxisFlag()  // axis ticks and labels will be rendered on the conventionally opposite side (i.e, right or top)
    object Foreground : PlotAxisFlag()  // grid lines will be displayed in the foreground (i.e. on top of data) instead of the background
    object Invert : PlotAxisFlag() // the axis will be inverted
    object AutoFit : PlotAxisFlag() // axis will be auto-fitting to data extents
    object RangeFit : PlotAxisFlag() // axis will only fit points if the point is in the visible range of the **orthogonal** axis
    object PanStretch : PlotAxisFlag() // panning in a locked or constrained state will cause the axis to stretch if possible
    object LockMin : PlotAxisFlag() // the axis minimum value will be locked when panning/zooming
    object LockMax : PlotAxisFlag() // the axis maximum value will be locked when panning/zooming

    override val i: Int = 1 shl ordinal

    @GenSealedEnum companion object {
        val Lock = LockMin / LockMax
        val NoDecorations = NoLabel / NoGridLines / NoTickMarks / NoTickLabels
        val AuxDefault = NoGridLines / Opposite
    }
}

typealias PlotSubplotFlags = Flag<PlotSubplotFlag>       // -> enum ImPlotSubplotFlags_

// Options for subplots (see BeginSubplot)
sealed class PlotSubplotFlag : FlagBase<PlotSubplotFlag>() {
    //    ImPlotSubplotFlags_None        = 0,       // default
    object NoTitle : PlotSubplotFlag()  // the subplot title will not be displayed (titles are also hidden if preceeded by double hashes, e.g. "##MySubplot")
    object NoLegend : PlotSubplotFlag()  // the legend will not be displayed (only applicable if ImPlotSubplotFlags_ShareItems is enabled)
    object NoMenus : PlotSubplotFlag()  // the user will not be able to open context menus with right-click
    object NoResize : PlotSubplotFlag()  // resize splitters between subplot cells will be not be provided
    object NoAlign : PlotSubplotFlag()  // subplot edges will not be aligned vertically or horizontally
    object ShareItems : PlotSubplotFlag()  // items across all subplots will be shared and rendered into a single legend entry
    object LinkRows : PlotSubplotFlag()  // link the y-axis limits of all plots in each row (does not apply to auxiliary axes)
    object LinkCols : PlotSubplotFlag()  // link the x-axis limits of all plots in each column (does not apply to auxiliary axes)
    object LinkAllX : PlotSubplotFlag()  // link the x-axis limits in every plot in the subplot (does not apply to auxiliary axes)
    object LinkAllY : PlotSubplotFlag()  // link the y-axis limits in every plot in the subplot (does not apply to auxiliary axes)
    object ColMajor : PlotSubplotFlag() // subplots are added in column major order instead of the default row major order

    override val i = 1 shl ordinal

    @GenSealedEnum companion object
}

typealias PlotLegendFlags = Flag<PlotLegendFlag>        // -> enum ImPlotLegendFlags_

// Options for legends (see SetupLegend)
sealed class PlotLegendFlag : FlagBase<PlotLegendFlag>() {
    //    ImPlotLegendFlags_None            = 0,      // default
    object NoButtons : PlotLegendFlag() // legend icons will not function as hide/show buttons
    object NoHighlightItem : PlotLegendFlag() // plot items will not be highlighted when their legend entry is hovered
    object NoHighlightAxis : PlotLegendFlag() // axes will not be highlighted when legend entries are hovered (only relevant if x/y-axis count > 1)
    object NoMenus : PlotLegendFlag() // the user will not be able to open context menus with right-click
    object Outside : PlotLegendFlag() // legend will be rendered outside of the plot area
    object Horizontal : PlotLegendFlag() // legend entries will be displayed horizontally
    object Sort : PlotLegendFlag() // legend entries will be displayed in alphabetical order

    override val i = 1 shl ordinal

    @GenSealedEnum companion object
}

typealias PlotMouseTextFlags = Flag<PlotMouseTextFlag>     // -> enum ImPlotMouseTextFlags_

// Options for mouse hover text (see SetupMouseText)
sealed class PlotMouseTextFlag : FlagBase<PlotMouseTextFlag>() {
    //    ImPlotMouseTextFlags_None        = 0,      // default
    object NoAuxAxes : PlotMouseTextFlag() // only show the mouse position for primary axes
    object NoFormat : PlotMouseTextFlag() // axes label formatters won't be used to render text
    object ShowAlways : PlotMouseTextFlag() // always display mouse position even if plot not hovered

    override val i = 1 shl ordinal

    @GenSealedEnum companion object
}

typealias PlotDragToolFlags = Flag<PlotDragToolFlag>      // -> ImPlotDragToolFlags_

// Options for DragPoint, DragLine, DragRect
sealed class PlotDragToolFlag : FlagBase<PlotDragToolFlag>() {
    //    ImPlotDragToolFlags_None      = 0,      // default
    object NoCursors : PlotDragToolFlag() // drag tools won't change cursor icons when hovered or held
    object NoFit : PlotDragToolFlag() // the drag tool won't be considered for plot fits
    object NoInputs : PlotDragToolFlag() // lock the tool from user inputs
    object Delayed : PlotDragToolFlag() // tool rendering will be delayed one frame; useful when applying position-constraints

    override val i = 1 shl ordinal

    @GenSealedEnum companion object
}

typealias PlotColormapScaleFlags = Flag<PlotColormapScaleFlag> // -> ImPlotColormapScaleFlags_

// Flags for ColormapScale
sealed class PlotColormapScaleFlag : FlagBase<PlotColormapScaleFlag>() {
    //    ImPlotColormapScaleFlags_None     = 0,      // default
    object NoLabel : PlotColormapScaleFlag() // the colormap axis label will not be displayed
    object Opposite : PlotColormapScaleFlag() // render the colormap label and tick labels on the opposite side
    object Invert : PlotColormapScaleFlag() // invert the colormap bar and axis scale (this only affects rendering; if you only want to reverse the scale mapping, make scale_min > scale_max)

    override val i = 1 shl ordinal

    @GenSealedEnum companion object
}

typealias PlotItemFlags = Flag<PlotItemFlag>          // -> ImPlotItemFlags_

// Flags for ANY PlotX function
sealed class PlotItemFlag : FlagBase<PlotItemFlag>() {
    //    ImPlotItemFlags_None     = 0,
    object NoLegend : PlotItemFlag() // the item won't have a legend entry displayed
    object NoFit : PlotItemFlag() // the item won't be considered for plot fits

    override val i = 1 shl ordinal

    @GenSealedEnum companion object
}

typealias PlotLineFlags = Flag<PlotLineFlag>          // -> ImPlotLineFlags_

// Flags for PlotLine
sealed class PlotLineFlag : FlagBase<PlotLineFlag>() {
    //    ImPlotLineFlags_None        = 0,       // default
    object Segments : PlotLineFlag() // a line segment will be rendered from every two consecutive points
    object Loop : PlotLineFlag() // the last and first point will be connected to form a closed loop
    object SkipNaN : PlotLineFlag() // NaNs values will be skipped instead of rendered as missing data
    object NoClip : PlotLineFlag() // markers (if displayed) on the edge of a plot will not be clipped
    object Shaded : PlotLineFlag() // a filled region between the line and horizontal origin will be rendered; use PlotShaded for more advanced cases

    override val i = 1 shl (ordinal + 10)

    @GenSealedEnum companion object
}

typealias PlotScatterFlags = Flag<PlotScatterFlag>       // -> ImPlotScatterFlags

// Flags for PlotScatter
sealed class PlotScatterFlag : FlagBase<PlotScatterFlag>() {
    //    ImPlotScatterFlags_None   = 0,       // default
    object NoClip : PlotSubplotFlag() // markers on the edge of a plot will not be clipped

    override val i = 1 shl (ordinal + 10)

    @GenSealedEnum companion object
}

typealias PlotStairsFlags = Flag<PlotStairsFlag>        // -> ImPlotStairsFlags_

// Flags for PlotStairs
sealed class PlotStairsFlag : FlagBase<PlotStairsFlag>() {
    //    ImPlotStairsFlags_None     = 0,       // default
    object PreStep : PlotStairsFlag() // the y value is continued constantly to the left from every x position, i.e. the interval (x[i-1], x[i]] has the value y[i]
    object Shaded : PlotStairsFlag()  // a filled region between the stairs and horizontal origin will be rendered; use PlotShaded for more advanced cases

    override val i = 1 shl (ordinal + 10)

    @GenSealedEnum companion object
}

typealias PlotShadedFlags = Flag<PlotShadedFlag>        // -> ImPlotShadedFlags_

// Flags for PlotShaded (placeholder)
sealed class PlotShadedFlag : FlagBase<PlotShadedFlag>() {
//    ImPlotShadedFlags_None  = 0 // default

    @GenSealedEnum companion object
}

typealias PlotBarsFlags = Flag<PlotBarsFlag>          // -> ImPlotBarsFlags_

// Flags for PlotBars
sealed class PlotBarsFlag : FlagBase<PlotBarsFlag>() {
    //    ImPlotBarsFlags_None         = 0,       // default
    object Horizontal : PlotBarsFlag() // bars will be rendered horizontally on the current y-axis

    override val i = 1 shl (ordinal + 10)

    @GenSealedEnum companion object
}

typealias PlotBarGroupsFlags = Flag<PlotBarGroupsFlag>     // -> ImPlotBarGroupsFlags_

// Flags for PlotBarGroups
sealed class PlotBarGroupsFlag : FlagBase<PlotBarGroupsFlag>() {
    //    ImPlotBarGroupsFlags_None        = 0,       // default
    object Horizontal : PlotBarGroupsFlag() // bar groups will be rendered horizontally on the current y-axis
    object Stacked : PlotBarGroupsFlag() // items in a group will be stacked on top of each other

    override val i = 1 shl (ordinal + 10)

    @GenSealedEnum companion object
}

typealias PlotErrorBarsFlags = Flag<PlotErrorBarsFlag>     // -> ImPlotErrorBarsFlags_

// Flags for PlotErrorBars
sealed class PlotErrorBarsFlag : FlagBase<PlotErrorBarsFlag>() {
    //    ImPlotErrorBarsFlags_None       = 0,       // default
    object Horizontal : PlotErrorBarsFlag() // error bars will be rendered horizontally on the current y-axis

    override val i = 1 shl (ordinal + 10)

    @GenSealedEnum companion object
}

typealias PlotStemsFlags = Flag<PlotStemsFlag>         // -> ImPlotStemsFlags_

// Flags for PlotStems
sealed class PlotStemsFlag : FlagBase<PlotStemsFlag>() {
    //    ImPlotStemsFlags_None       = 0,       // default
    object Horizontal : PlotStemsFlag() // stems will be rendered horizontally on the current y-axis

    override val i = 1 shl (ordinal + 10)

    @GenSealedEnum companion object
}
typealias PlotInfLinesFlags = Flag<PlotInfLinesFlag>      // -> ImPlotInfLinesFlags_

// Flags for PlotInfLines
sealed class PlotInfLinesFlag : FlagBase<PlotInfLinesFlag>() {
    //    ImPlotInfLinesFlags_None       = 0,      // default
    object Horizontal : PlotInfLinesFlag() // lines will be rendered horizontally on the current y-axis

    override val i = 1 shl (ordinal + 10)

    @GenSealedEnum companion object
}

typealias PlotPieChartFlags = Flag<PlotPieChartFlag>      // -> ImPlotPieChartFlags_

// Flags for PlotPieChart
sealed class PlotPieChartFlag : FlagBase<PlotPieChartFlag>() {
    //    ImPlotPieChartFlags_None      = 0,      // default
    object Normalize : PlotPieChartFlag() // force normalization of pie chart values (i.e. always make a full circle if sum < 0)

    override val i = 1 shl (ordinal + 10)

    @GenSealedEnum companion object
}

typealias PlotHeatmapFlags = Flag<PlotHeatmapFlag>       // -> ImPlotHeatmapFlags_

// Flags for PlotHeatmap
sealed class PlotHeatmapFlag : FlagBase<PlotHeatmapFlag>() {
    //    ImPlotHeatmapFlags_None     = 0,       // default
    object ColMajor : PlotHeatmapFlag() // data will be read in column major order

    override val i = 1 shl (ordinal + 10)

    @GenSealedEnum companion object
}

typealias PlotHistogramFlags = Flag<PlotHistogramFlag>     // -> ImPlotHistogramFlags_

// Flags for PlotHistogram and PlotHistogram2D
sealed class PlotHistogramFlag : FlagBase<PlotHistogramFlag>() {
    //    ImPlotHistogramFlags_None       = 0,       // default
    object Horizontal : PlotHistogramFlag() // histogram bars will be rendered horizontally (not supported by PlotHistogram2D)
    object Cumulative : PlotHistogramFlag() // each bin will contain its count plus the counts of all previous bins (not supported by PlotHistogram2D)
    object Density : PlotHistogramFlag() // counts will be normalized, i.e. the PDF will be visualized, or the CDF will be visualized if Cumulative is also set
    object NoOutliers : PlotHistogramFlag() // exclude values outside the specifed histogram range from the count toward normalizing and cumulative counts
    object ColMajor : PlotHistogramFlag() // data will be read in column major order (not supported by PlotHistogram)

    override val i = 1 shl (ordinal + 10)

    @GenSealedEnum companion object
}

typealias PlotDigitalFlags = Flag<PlotDigitalFlag>       // -> ImPlotDigitalFlags_

// Flags for PlotDigital (placeholder)
sealed class PlotDigitalFlag : FlagBase<PlotDigitalFlag>() {
    //    ImPlotDigitalFlags_None = 0 // default
    @GenSealedEnum companion object
}

typealias PlotImageFlags = Flag<PlotImageFlag>         // -> ImPlotImageFlags_

// Flags for PlotImage (placeholder)
sealed class PlotImageFlag : FlagBase<PlotImageFlag>() {
//    ImPlotImageFlags_None = 0 // default

    @GenSealedEnum companion object
}

typealias PlotTextFlags = Flag<PlotTextFlag>          // -> ImPlotTextFlags_

// Flags for PlotText
sealed class PlotTextFlag : FlagBase<PlotTextFlag>() {
    //    ImPlotTextFlags_None     = 0,       // default
    object Vertical : PlotTextFlag()  // text will be rendered vertically

    override val i = 1 shl (ordinal + 10)

    @GenSealedEnum companion object
}

typealias PlotDummyFlags = Flag<PlotDummyFlag>         // -> ImPlotDummyFlags_

// Flags for PlotDummy (placeholder)
sealed class PlotDummyFlag : FlagBase<PlotDummyFlag>() {
//    ImPlotDummyFlags_None = 0 // default

    @GenSealedEnum companion object
}

//typedef int ImPlotCond;               // -> enum ImPlotCond_

// Represents a condition for SetupAxisLimits etc. (same as ImGuiCond, but we only support a subset of those enums)
typealias PlotCond = Cond
//enum ImPlotCond_
//{
//    ImPlotCond_None   = ImGuiCond_None,    // No condition (always set the variable), same as _Always
//    ImPlotCond_Always = ImGuiCond_Always,  // No condition (always set the variable)
//    ImPlotCond_Once   = ImGuiCond_Once,    // Set the variable once per runtime session (only the first call will succeed)
//};

//typedef int ImPlotCol;                // -> enum ImPlotCol_

// Plot styling colors.
enum class PlotCol {
    // item styling colors
    None,          // [JVM]
    Line,          // plot line/outline color (defaults to next unused color in current colormap)
    Fill,          // plot fill color for bars (defaults to the current line color)
    MarkerOutline, // marker outline color (defaults to the current line color)
    MarkerFill,    // marker fill color (defaults to the current line color)
    ErrorBar,      // error bar color (defaults to ImGuiCol_Text)

    // plot styling colors
    FrameBg,       // plot frame background color (defaults to ImGuiCol_FrameBg)
    PlotBg,        // plot area background color (defaults to ImGuiCol_WindowBg)
    PlotBorder,    // plot area border color (defaults to ImGuiCol_Border)
    LegendBg,      // legend background color (defaults to ImGuiCol_PopupBg)
    LegendBorder,  // legend border color (defaults to ImPlotCol_PlotBorder)
    LegendText,    // legend text color (defaults to ImPlotCol_InlayText)
    TitleText,     // plot title text color (defaults to ImGuiCol_Text)
    InlayText,     // color of text appearing inside of plots (defaults to ImGuiCol_Text)
    AxisText,      // axis label and tick lables color (defaults to ImGuiCol_Text)
    AxisGrid,      // axis grid color (defaults to 25% ImPlotCol_AxisText)
    AxisTick,      // axis tick color (defaults to AxisGrid)
    AxisBg,        // background color of axis hover region (defaults to transparent)
    AxisBgHovered, // axis hover color (defaults to ImGuiCol_ButtonHovered)
    AxisBgActive,  // axis active color (defaults to ImGuiCol_ButtonActive)
    Selection,     // box-selection color (defaults to yellow)
    Crosshairs;    // crosshairs color (defaults to ImPlotCol_PlotBorder)

    val i = ordinal - 1

    companion object {
        val COUNT = values().size
    }
}

operator fun <T> Array<T>.get(index: PlotCol) = get(index.i)
operator fun <T> Array<T>.set(index: PlotCol, value: T) = set(index.i, value)

//typedef int ImPlotStyleVar;           // -> enum ImPlotStyleVar_

// Plot styling variables.
enum class PlotStyleVar {
    // item styling variables
    LineWeight,         // float,  plot item line weight in pixels
    Marker,             // int,    marker specification
    MarkerSize,         // float,  marker size in pixels (roughly the marker's "radius")
    MarkerWeight,       // float,  plot outline weight of markers in pixels
    FillAlpha,          // float,  alpha modifier applied to all plot item fills
    ErrorBarSize,       // float,  error bar whisker width in pixels
    ErrorBarWeight,     // float,  error bar whisker weight in pixels
    DigitalBitHeight,   // float,  digital channels bit height (at 1) in pixels
    DigitalBitGap,      // float,  digital channels bit padding gap in pixels

    // plot styling variables
    PlotBorderSize,     // float,  thickness of border around plot area
    MinorAlpha,         // float,  alpha multiplier applied to minor axis grid lines
    MajorTickLen,       // ImVec2, major tick lengths for X and Y axes
    MinorTickLen,       // ImVec2, minor tick lengths for X and Y axes
    MajorTickSize,      // ImVec2, line thickness of major ticks
    MinorTickSize,      // ImVec2, line thickness of minor ticks
    MajorGridSize,      // ImVec2, line thickness of major grid lines
    MinorGridSize,      // ImVec2, line thickness of minor grid lines
    PlotPadding,        // ImVec2, padding between widget frame and plot area, labels, or outside legends (i.e. main padding)
    LabelPadding,       // ImVec2, padding between axes labels, tick labels, and plot edge
    LegendPadding,      // ImVec2, legend padding from plot edges
    LegendInnerPadding, // ImVec2, legend inner padding from legend edges
    LegendSpacing,      // ImVec2, spacing between legend entries
    MousePosPadding,    // ImVec2, padding between plot edge and interior info text
    AnnotationPadding,  // ImVec2, text padding around annotation labels
    FitPadding,         // ImVec2, additional fit padding as a percentage of the fit extents (e.g. ImVec2(0.1f,0.1f) adds 10% to the fit extents of X and Y)
    PlotDefaultSize,    // ImVec2, default size used when ImVec2(0,0) is passed to BeginPlot
    PlotMinSize;        // ImVec2, minimum size plot frame can be when shrunk

    val i = ordinal

    companion object {
        val COUNT = values().size
        infix fun of(i: Int) = values().first { it.i == i }
    }
}

//typedef int ImPlotScale;              // -> enum ImPlotScale_

// Axis scale
enum class PlotScale {
    None,       // [JVM]
    Linear,     // default linear scale
    Time,       // date/time scale
    Log10,      // base 10 logartithmic scale
    SymLog;     // symmetric log scale

    val i = ordinal - 1
}

//typedef int ImPlotMarker;             // -> enum ImPlotMarker_

// Marker specifications.
enum class PlotMarker {
    None,      // no marker
    Circle,    // a circle marker (default)
    Square,    // a square maker
    Diamond,   // a diamond marker
    Up,        // an upward-pointing triangle marker
    Down,      // an downward-pointing triangle marker
    Left,      // an leftward-pointing triangle marker
    Right,     // an rightward-pointing triangle marker
    Cross,     // a cross marker (not fillable)
    Plus,      // a plus marker (not fillable)
    Asterisk;  // a asterisk marker (not fillable)

    val i = ordinal - 1

    companion object {
        val COUNT = values().size
        infix fun of(i: Int) = values().first { it.i == i }
    }
}

//typedef int ImPlotColormap;           // -> enum ImPlotColormap_

// Built-in colormaps
enum class PlotColormap {
    None, // [JVM] for -1 values
    Deep,   // a.k.a. seaborn deep             (qual=true,  n=10) (default)
    Dark,   // a.k.a. matplotlib "Set1"        (qual=true,  n=9 )
    Pastel,   // a.k.a. matplotlib "Pastel1"     (qual=true,  n=9 )
    Paired,   // a.k.a. matplotlib "Paired"      (qual=true,  n=12)
    Viridis,   // a.k.a. matplotlib "viridis"     (qual=false, n=11)
    Plasma,   // a.k.a. matplotlib "plasma"      (qual=false, n=11)
    Hot,   // a.k.a. matplotlib/MATLAB "hot"  (qual=false, n=11)
    Cool,   // a.k.a. matplotlib/MATLAB "cool" (qual=false, n=11)
    Pink,   // a.k.a. matplotlib/MATLAB "pink" (qual=false, n=11)
    Jet,   // a.k.a. MATLAB "jet"             (qual=false, n=11)
    Twilight,  // a.k.a. matplotlib "twilight"    (qual=false, n=11)
    RdBu,  // red/blue, Color Brewer          (qual=false, n=11)
    BrBG,  // brown/blue-green, Color Brewer  (qual=false, n=11)
    PiYG,  // pink/yellow-green, Color Brewer (qual=false, n=11)
    Spectral,  // color spectrum, Color Brewer    (qual=false, n=11)
    Greys;  // white/black                     (qual=false, n=2 )

    val i = ordinal - 1

    companion object {
        val COUNT = values().size
        infix fun of(i: Int) = values().first { it.i == i }
    }
}

//typedef int ImPlotLocation;           // -> enum ImPlotLocation_

// Used to position items on a plot (e.g. legends, labels, etc.)
enum class PlotLocation(val i: Int) {
    Center(0),                                          // center-center
    North(1 shl 0),                                     // top-center
    South(1 shl 1),                                     // bottom-center
    West(1 shl 2),                                     // center-left
    East(1 shl 3),                                     // center-right
    NorthWest(North.i or West.i), // top-left
    NorthEast(North.i or East.i), // top-right
    SouthWest(South.i or West.i), // bottom-left
    SouthEast(South.i or East.i);  // bottom-right

    infix fun has(other: PlotLocation) = i has other.i
    infix fun hasnt(other: PlotLocation) = i hasnt other.i
}

//typedef int ImPlotBin;                // -> enum ImPlotBin_

// Enums for different automatic histogram binning methods (k = bin count or w = bin width)
enum class PlotBin {
    Sqrt, // k = sqrt(n)
    Sturges, // k = 1 + log2(n)
    Rice, // k = 2 * cbrt(n)
    Scott; // w = 3.49 * sigma / cbrt(n)

    val i = -ordinal - 1
}

// Double precision version of ImVec2 used by ImPlot. Extensible by end users.
typealias PlotPoint = Vec2d
//struct ImPlotPoint {
//    double x, y;
//    ImPlotPoint()                         { x = y = 0.0;      }
//    ImPlotPoint(double _x, double _y)     { x = _x; y = _y;   }
//    ImPlotPoint(const ImVec2& p)          { x = p.x; y = p.y; }
//    double  operator[] (size_t idx) const { return (&x)[idx]; }
//    double& operator[] (size_t idx)       { return (&x)[idx]; }
//    #ifdef IMPLOT_POINT_CLASS_EXTRA
//            IMPLOT_POINT_CLASS_EXTRA     // Define additional constructors and implicit cast operators in imconfig.h
//    // to convert back and forth between your math types and ImPlotPoint.
//    #endif
//};

// Range defined by a min/max value.
class PlotRange(var min: Double = 0.0, var max: Double = 0.0) {
    infix operator fun contains(value: Double) = value in min..max
    val size get() = max - min
    infix fun clamp(value: Double) = if (value < min) min else if (value > max) max else value
}

// Combination of two range limits for X and Y axes. Also an AABB defined by Min()/Max().
class PlotRect(val x: PlotRange = PlotRange(), val y: PlotRange = PlotRange()) {
    constructor(xMin: Double, xMax: Double, yMin: Double, yMax: Double) : this(PlotRange(xMin, xMax), PlotRange(yMin, yMax))

    infix operator fun contains(p: PlotPoint) = contains(p.x, p.y)
    fun contains(x: Double, y: Double) = this.x.contains(x) && this.y.contains(y)
    val size get() = PlotPoint(x.size, y.size)
    infix fun clamp(p: PlotPoint) = clamp(p.x, p.y)
    fun clamp(x: Double, y: Double) = PlotPoint(this.x.clamp(x), this.y.clamp(y))
    val min get() = PlotPoint(x.min, y.min)
    val max get() = PlotPoint(x.max, y.max)
}

// Plot style structure
class PlotStyle {
    // item styling variables
    var lineWeight = 1f              // = 1,      item line weight in pixels
    var marker = PlotMarker.None    // = ImPlotMarker_None, marker specification
    var markerSize = 4f              // = 4,      marker size in pixels (roughly the marker's "radius")
    var markerWeight = 1f            // = 1,      outline weight of markers in pixels
    var fillAlpha = 1f               // = 1,      alpha modifier applied to plot fills
    var errorBarSize = 5f            // = 5,      error bar whisker width in pixels
    var errorBarWeight = 1.5f          // = 1.5,    error bar whisker weight in pixels
    var digitalBitHeight = 8f        // = 8,      digital channels bit height (at y = 1.0f) in pixels
    var digitalBitGap = 4f           // = 4,      digital channels bit padding gap in pixels

    // plot styling variables
    var plotBorderSize = 1f          // = 1,      line thickness of border around plot area
    var minorAlpha = 0.25f              // = 0.25    alpha multiplier applied to minor axis grid lines
    val majorTickLen = Vec2(10)            // = 10,10   major tick lengths for X and Y axes
    val minorTickLen = Vec2(5)            // = 5,5     minor tick lengths for X and Y axes
    val majorTickSize = Vec2(1)           // = 1,1     line thickness of major ticks
    val minorTickSize = Vec2(1)           // = 1,1     line thickness of minor ticks
    val majorGridSize = Vec2(1)           // = 1,1     line thickness of major grid lines
    val minorGridSize = Vec2(1)           // = 1,1     line thickness of minor grid lines
    val plotPadding = Vec2(10)             // = 10,10   padding between widget frame and plot area, labels, or outside legends (i.e. main padding)
    val labelPadding = Vec2(5)            // = 5,5     padding between axes labels, tick labels, and plot edge
    val legendPadding = Vec2(10)           // = 10,10   legend padding from plot edges
    val legendInnerPadding = Vec2(5)      // = 5,5     legend inner padding from legend edges
    val legendSpacing = Vec2(5, 0)           // = 5,0     spacing between legend entries
    val mousePosPadding = Vec2(10)         // = 10,10   padding between plot edge and interior mouse location text
    val annotationPadding = Vec2(2)       // = 2,2     text padding around annotation labels
    val fitPadding = Vec2()              // = 0,0     additional fit padding as a percentage of the fit extents (e.g. ImVec2(0.1f,0.1f) adds 10% to the fit extents of X and Y)
    val plotDefaultSize = Vec2(400, 300)         // = 400,300 default size used when ImVec2(0,0) is passed to BeginPlot
    val plotMinSize = Vec2(200, 150)             // = 200,150 minimum size plot frame can be when shrunk

    // style colors
    val colors = Array(PlotCol.COUNT) { Vec4() } // Array of styling colors. Indexable with ImPlotCol_ enums.

    // colormap
    var colormap = PlotColormap.Deep         // The current colormap. Set this to either an ImPlotColormap_ enum or an index returned by AddColormap.

    // settings/flags
    var useLocalTime = false            // = false,  axis labels will be formatted for your timezone when ImPlotAxisFlag_Time is enabled
    var useISO8601 = false              // = false,  dates will be formatted according to ISO 8601 where applicable (e.g. YYYY-MM-DD, YYYY-MM, --MM-DD, etc.)
    var use24HourClock = false          // = false,  times will be formatted using a 24 hour clock
//    IMPLOT_API ImPlotStyle ();

    init {
        styleColorsAuto(this)
    }
}

// Input mapping structure. Default values listed. See also MapInputDefault, MapInputReverse.
class PlotInputMap {
    var pan = MouseButton.Left           // LMB    enables panning when held,
    var panMod: KeyChord = Key.Mod_None        // none   optional modifier that must be held for panning/fitting
    var fit = MouseButton.Left           // LMB    initiates fit when double clicked
    var select = MouseButton.Right        // RMB    begins box selection when pressed and confirms selection when released
    var selectCancel = MouseButton.Left  // LMB    cancels active box selection when pressed; cannot be same as Select
    var selectMod: KeyChord = Key.Mod_None     // none   optional modifier that must be held for box selection
    var selectHorzMod: KeyChord = Key.Mod_Alt // Alt    expands active box selection horizontally to plot edge when held
    var selectVertMod: KeyChord = Key.Mod_Shift // Shift  expands active box selection vertically to plot edge when held
    var menu = MouseButton.Right          // RMB    opens context menus (if enabled) when clicked
    var overrideMod: KeyChord = Key.Mod_Ctrl   // Ctrl   when held, all input is ignored; used to enable axis/plots as DND sources
    var zoomMod: KeyChord = Key.Mod_None       // none   optional modifier that must be held for scroll wheel zooming
    var zoomRate = 0.1f      // 0.1f   zoom rate for scroll (e.g. 0.1f = 10% plot range every scroll click); make negative to invert
//    IMPLOT_API ImPlotInputMap ();
}