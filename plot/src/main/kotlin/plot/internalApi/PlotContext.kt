@file:OptIn(ExperimentalUnsignedTypes::class)

package plot.internalApi

import imgui.COL32_BLACK
import imgui.COL32_WHITE
import imgui.ImGui
import imgui.internal.classes.ColorMod
import imgui.internal.classes.Pool
import imgui.internal.classes.StyleMod
import plot.api.PlotColormap
import plot.api.PlotInputMap
import plot.api.PlotStyle
import java.time.LocalDateTime

// Holds state information that must persist between calls to BeginPlot()/EndPlot()
class PlotContext {
    // Plot States
    val plots = Pool { PlotPlot() }
    val subplots = Pool { PlotSubplot() }
    var currentPlot: PlotPlot? = null
    var currentSubplot: PlotSubplot? = null
    var currentItems: PlotItemGroup? = null
    var currentItem: PlotItem? = null
    var previousItem: PlotItem? = null

    // Tick Marks and Labels
    val cTicker = PlotTicker()

    // Annotation and Tabs
    val annotations = PlotAnnotationCollection()
    val tags = PlotTagCollection()

    // Flags
    var childWindowMade = false

    // Style and Colormaps
    val style = PlotStyle()
    val colorModifiers = ArrayList<ColorMod>()
    val styleModifiers = ArrayList<StyleMod>()
    val colormapData = PlotColormapData()
    val colormapModifiers = ArrayList<PlotColormap>()

    // Time
    var tm = LocalDateTime.MIN

    // Temp data for general use
    var tempDouble1 = DoubleArray(0)
    val tempDouble2 = ArrayList<Double>()
    var tempInt1 = ArrayList<Int>()

    // Misc
    var digitalPlotItemCnt = 0f
    var digitalPlotOffset = 0f
    val nextPlotData = PlotNextPlotData()
    val nextItemData = PlotNextItemData()
    val inputMap = PlotInputMap()
    var openContextThisFrame = false
    val mousePosStringBuilder = ArrayList<String>()
    var sortItems: PlotItemGroup? = null

    // Align plots
    val alignmentData = Pool { PlotAlignmentData() }
    var currentAlignmentH: PlotAlignmentData? = null
    var currentAlignmentV: PlotAlignmentData? = null

    fun initialize() {
        resetCtxForNextPlot()
        resetCtxForNextAlignedPlots()
        resetCtxForNextSubplot()

        val deep = uintArrayOf(4289753676u, 4283598045u, 4285048917u, 4283584196u, 4289950337u, 4284512403u, 4291005402u, 4287401100u, 4285839820u, 4291671396u)
        val dark = uintArrayOf(4280031972u, 4290281015u, 4283084621u, 4288892568u, 4278222847u, 4281597951u, 4280833702u, 4290740727u, 4288256409u)
        val pastel = uintArrayOf(4289639675u, 4293119411u, 4291161036u, 4293184478u, 4289124862u, 4291624959u, 4290631909u, 4293712637u, 4294111986u)
        val paired = uintArrayOf(4293119554u, 4290017311u, 4287291314u, 4281114675u, 4288256763u, 4280031971u, 4285513725u, 4278222847u, 4292260554u, 4288298346u, 4288282623u, 4280834481u)
        val viridis = uintArrayOf(4283695428u, 4285867080u, 4287054913u, 4287455029u, 4287526954u, 4287402273u, 4286883874u, 4285579076u, 4283552122u, 4280737725u, 4280674301u)
        val plasma = uintArrayOf(4287039501u, 4288480321u, 4289200234u, 4288941455u, 4287638193u, 4286072780u, 4284638433u, 4283139314u, 4281771772u, 4280667900u, 4280416752u)
        val hot = uintArrayOf(4278190144u, 4278190208u, 4278190271u, 4278190335u, 4278206719u, 4278223103u, 4278239231u, 4278255615u, 4283826175u, 4289396735u, 4294967295u)
        val cool = uintArrayOf( 4294967040u, 4294960666u, 4294954035u, 4294947661u, 4294941030u, 4294934656u, 4294928025u, 4294921651u, 4294915020u, 4294908646u, 4294902015u)
        val pink = uintArrayOf( 4278190154u, 4282532475u, 4284308894u, 4285690554u, 4286879686u, 4287870160u, 4288794330u, 4289651940u, 4291685869u, 4293392118u, 4294967295u)
        val jet = uintArrayOf(4289331200u, 4294901760u, 4294923520u, 4294945280u, 4294967040u, 4289396565u, 4283826090u, 4278255615u, 4278233855u, 4278212095u, 4278190335u)
        val twilight = uintArrayOf(RGB(226, 217, 226), RGB(166, 191, 202), RGB(109, 144, 192), RGB(95, 88, 176), RGB(83, 30, 124), RGB(47, 20, 54), RGB(100, 25, 75), RGB(159, 60, 80), RGB(192, 117, 94), RGB(208, 179, 158), RGB(226, 217, 226))
        val rdBu = uintArrayOf(RGB(103, 0, 31), RGB(178, 24, 43), RGB(214, 96, 77), RGB(244, 165, 130), RGB(253, 219, 199), RGB(247, 247, 247), RGB(209, 229, 240), RGB(146, 197, 222), RGB(67, 147, 195), RGB(33, 102, 172), RGB(5, 48, 97))
        val brBG = uintArrayOf(RGB(84, 48, 5), RGB(140, 81, 10), RGB(191, 129, 45), RGB(223, 194, 125), RGB(246, 232, 195), RGB(245, 245, 245), RGB(199, 234, 229), RGB(128, 205, 193), RGB(53, 151, 143), RGB(1, 102, 94), RGB(0, 60, 48))
        val piYG = uintArrayOf(RGB(142, 1, 82), RGB(197, 27, 125), RGB(222, 119, 174), RGB(241, 182, 218), RGB(253, 224, 239), RGB(247, 247, 247), RGB(230, 245, 208), RGB(184, 225, 134), RGB(127, 188, 65), RGB(77, 146, 33), RGB(39, 100, 25))
        val spectral = uintArrayOf(RGB(158, 1, 66), RGB(213, 62, 79), RGB(244, 109, 67), RGB(253, 174, 97), RGB(254, 224, 139), RGB(255, 255, 191), RGB(230, 245, 152), RGB(171, 221, 164), RGB(102, 194, 165), RGB(50, 136, 189), RGB(94, 79, 162))
        val greys = uintArrayOf(COL32_WHITE.toUInt(), COL32_BLACK.toUInt())

        APPEND_CMAP("deep", deep,  true)
        APPEND_CMAP("dark", dark,  true)
        APPEND_CMAP("pastel", pastel,  true)
        APPEND_CMAP("paired", paired,  true)
        APPEND_CMAP("viridis", viridis,  false)
        APPEND_CMAP("plasma", plasma,  false)
        APPEND_CMAP("hot", hot,  false)
        APPEND_CMAP("cool", cool,  false)
        APPEND_CMAP("pink", pink,  false)
        APPEND_CMAP("jet", jet,  false)
        APPEND_CMAP("twilight", twilight,  false)
        APPEND_CMAP("rdBu", rdBu, false)
        APPEND_CMAP("brBG", brBG, false)
        APPEND_CMAP("piYG", piYG, false)
        APPEND_CMAP("spectral", spectral,  false)
        APPEND_CMAP("greys", greys,  false)
    }

    fun resetCtxForNextPlot() {
        // end child window if it was made
        if (childWindowMade)
            ImGui.endChild()
        childWindowMade = false
        // reset the next plot/item data
        nextPlotData.reset()
        nextItemData.reset()
        // reset labels
        annotations.reset()
        tags.reset()
        // reset extents/fit
        openContextThisFrame = false
        // reset digital plot items count
        digitalPlotItemCnt = 0f
        digitalPlotOffset = 0f
        // nullify plot
        currentPlot = null
        currentItem = null
        previousItem = null
    }

    fun resetCtxForNextAlignedPlots() {
        currentAlignmentH = null
        currentAlignmentV = null
    }

    fun resetCtxForNextSubplot() {
        currentSubplot = null
        currentAlignmentH = null
        currentAlignmentV = null
    }
}