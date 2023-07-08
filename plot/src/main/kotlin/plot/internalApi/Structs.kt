@file:OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)

package plot.internalApi

import glm_.L
import glm_.d
import glm_.f
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.internal.classes.Pool
import imgui.internal.classes.Rect
import imgui.internal.hashStr
import plot.api.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.reflect.KMutableProperty0

//-----------------------------------------------------------------------------
// [SECTION] Structs
//-----------------------------------------------------------------------------

// Combined date/time format spec
data class PlotDateTimeSpec(var date: DateFmt = DateFmt.None,
                       var time: TimeFmt = TimeFmt.None,
                       var use24HourClock: Boolean = false,
                       var useISO8601: Boolean = false)

// Two part timestamp struct.
class PlotTime : Comparable<PlotTime> {
    var s: Long // second part
    var μs: Int // microsecond part

    constructor() {
        s = 0
        μs = 0
    }

    constructor(s: Long, μs: Int = 0) {
        this.s = s + μs / 1_000_000
        this.μs = μs % 1_000_000
    }

    infix fun put(t: PlotTime) {
        s = t.s
        μs = t.μs
    }
    fun copy() = PlotTime(s, μs)

    fun rollOver() {
        s += μs / 1_000_000
        μs %= 1_000_000
    }

    val toDouble get() = s.d + μs.d / 1_000_000.0

    operator fun plus(t: PlotTime) = PlotTime(s + t.s, μs + t.μs)
    operator fun minus(t: PlotTime) = PlotTime(s - t.s, μs - t.μs)

    val year: Int
        get() = getTime(this).year

    override fun equals(other: Any?): Boolean = other is PlotTime && s == other.s && μs == other.μs
    override fun compareTo(other: PlotTime): Int = when (val cmp = s.compareTo(other.s)) {
        0 -> μs.compareTo(other.μs)
        else -> cmp
    }

    override fun hashCode(): Int = 31 * s.hashCode() + μs

    companion object {
        infix fun fromDouble(t: Double) = PlotTime(t.L, (t * 1_000_000 - floor(t) * 1_000_000).i)
    }
}

// Colormap data storage
class PlotColormapData {
    val keys = ArrayList<UInt>()
    val keyCounts = ArrayList<Int>()
    val keyOffsets = ArrayList<Int>()
    val tables = ArrayList<UInt>()
    val tableSizes = ArrayList<Int>()
    val tableOffsets = ArrayList<Int>()
    val text = ArrayList<String>()
    val textOffsets = ArrayList<Int>()
    val quals = ArrayList<Boolean>()
    val map = mutableMapOf<Int, Int>()
    var count = 0

    fun append(name: String, keys: UIntArray, qual: Boolean): PlotColormap {
        val count = keys.size
        if (getIndex(name) != PlotColormap.None)
            return PlotColormap.None
        keyOffsets += this.keys.size
        keyCounts += count
        this.keys.ensureCapacity(this.keys.size + count)
        for (i in 0..<count)
            this.keys += keys[i]
        textOffsets += text.size
        text += name
        quals += qual
        val id = hashStr(name)
        val idx = this.count++
        map[id] = idx
        appendTable(PlotColormap of idx)
        return PlotColormap of idx
    }

    internal infix fun appendTable(cmap: PlotColormap) {
        val keyCount = getKeyCount(cmap)
        val keys = getKeys(cmap)
        val off = tables.size
        tableOffsets += off
        if (isQual(cmap)) {
            tables.ensureCapacity(keyCount)
            for (i in 0..<keyCount)
                tables += keys[i]
            tableSizes += keyCount
        } else {
            val maxSize = 255 * (keyCount - 1) + 1
            tables.ensureCapacity(off + maxSize)
            // ImU32 last = keys[0];
            // Tables.push_back(last);
            // int n = 1;
            for (i in 0..<keyCount - 1) {
                for (s in 0..<255) {
                    val a = keys[i]
                    val b = keys[i + 1]
                    val c = mixU32(a, b, s)
                    // if (c != last) {
                    tables += c
                    // last = c;
                    // n++;
                    // }
                }
            }
            val c = keys[keyCount - 1]
            // if (c != last) {
            tables += c
            // n++;
            // }
            // TableSizes.push_back(n);
            tableSizes += maxSize
        }
    }

    fun rebuildTables() {
        tables.clear()
        tableSizes.clear()
        tableOffsets.clear()
        for (i in 0..<count)
            appendTable(PlotColormap of i)
    }

    infix fun isQual(cmap: PlotColormap) = quals[cmap.i]
    infix fun getName(cmap: PlotColormap) = text[cmap.i]
    infix fun getIndex(name: String): PlotColormap {
        val key = hashStr(name)
        return PlotColormap of (map[key] ?: -1)
    }

    infix fun getKeys(cmap: PlotColormap) = keys.subList(keyOffsets[cmap.i], keyOffsets.lastIndex).toUIntArray()
    infix fun getKeyCount(cmap: PlotColormap) = keyCounts[cmap.i]
    fun getKeyColor(cmap: PlotColormap, idx: Int) = keys[keyOffsets[cmap.i] + idx]
    fun setKeyColor(cmap: PlotColormap, idx: Int, value: UInt) {
        keys[keyOffsets[cmap.i] + idx] = value
        rebuildTables()
    }

    //    inline const ImU32 * GetTable(ImPlotColormap cmap) const { return &Tables[TableOffsets[cmap]]; }
    infix fun getTableSize(cmap: PlotColormap) = tableSizes[cmap.i]
    fun getTableColor(cmap: PlotColormap, idx: Int) = tables[tableOffsets[cmap.i] + idx]

    fun lerpTable(cmap: PlotColormap, t: Float): UInt {
        val off = tableOffsets[cmap.i]
        val siz = tableSizes[cmap.i]
        val idx = if (quals[cmap.i]) clamp((siz * t).i, 0, siz - 1) else ((siz - 1) * t + 0.5f).i
        return tables[off + idx]
    }
}

// ImPlotPoint with positive/negative error values
class PlotPointError(val x: Double, val y: Double, val neg: Double, val pos: Double)

// Interior plot label/annotation
class PlotAnnotation {
    var pos = Vec2()
    var offset = Vec2()
    var colorBg = 0u
    var colorFg = 0u
    var textOffset = 0
    var clamp = false
}

// Collection of plot labels
class PlotAnnotationCollection {

    val annotations = ArrayList<PlotAnnotation>()
    val textBuffer = ArrayList<String>()
    val size: Int
        get() {
            assert(annotations.size == textBuffer.size)
            return annotations.size
        }

//    ImPlotAnnotationCollection() { Reset(); }

    fun append(pos: Vec2, off: Vec2, bg: UInt, fg: UInt, clamp: Boolean, fmt: String, vararg args: String) {
        val an = PlotAnnotation()
        an.pos = pos; an.offset = off
        an.colorBg = bg; an.colorFg = fg
//        an.textOffset = TextBuffer.size();
        an.clamp = clamp
        annotations += an
        textBuffer += fmt.format(args)
//        const char nul[] = ""
//        TextBuffer.append(nul, nul + 1)
//        size++
    }

//    void Append (const ImVec2 & pos, const ImVec2& off, ImU32 bg, ImU32 fg, bool clamp, const char* fmt, ...) IM_FMTARGS(7) {
//        va_list args
//        va_start(args, fmt)
//        AppendV(pos, off, bg, fg, clamp, fmt, args)
//        va_end(args)
//    }

    infix fun getText(idx: Int) = textBuffer[annotations[idx].textOffset]

    fun reset() {
        annotations.clear()
        textBuffer.clear()
//        Size = 0
    }
}

class PlotTag(val axis: Axis,
              val value: Double,
              val colorBg: UInt,
              val colorFg: UInt,
              val textOffset: Int)

class PlotTagCollection {

    val tags = ArrayList<PlotTag>()
    val textBuffer = ArrayList<String>()
    val size: Int
        get() {
            assert(tags.size == textBuffer.size)
            return tags.size
        }

//    ImPlotTagCollection() { Reset(); }

    fun append(axis: Axis, value: Double, bg: UInt, fg: UInt, fmt: String, vararg args: String) {
        val tag = PlotTag(axis, value, bg, fg, textBuffer.size)
        tags += tag
        textBuffer += fmt.format(args)
//        const char nul[] = ""
//        TextBuffer.append(nul, nul + 1)
//        Size++
    }

//    void Append (ImAxis axis, double value , ImU32 bg, ImU32 fg, const char* fmt, ...) IM_FMTARGS(6)
//    {
//        va_list args
//        va_start(args, fmt)
//        AppendV(axis, value, bg, fg, fmt, args)
//        va_end(args)
//    }

    infix fun getText(idx: Int) = textBuffer[tags[idx].textOffset]

    fun reset() {
        tags.clear()
        textBuffer.clear()
//        Size = 0
    }
}

// Tick mark info
class PlotTick(val plotPos: Double, val major: Boolean, val level: Int, var showLabel: Boolean) {
    var pixelPos = 0f
    val labelSize = Vec2()
    var textOffset = -1
    var idx = 0
}

// Collection of ticks
class PlotTicker {
    val ticks = ArrayList<PlotTick>()
    val textBuffer = ArrayList<String>()
    val maxSize = Vec2()
    val lateSize = Vec2()
    var levels = 0

    init {
        reset()
    }

    fun addTick(value: Double, major: Boolean, level: Int, showLabel: Boolean, label: String?): PlotTick {
        val tick = PlotTick(value, major, level, showLabel)
        if (showLabel && label != null) {
            tick.textOffset = textBuffer.size
            textBuffer += label
            tick.labelSize put ImGui.calcTextSize(textBuffer[tick.textOffset])
        }
        return addTick(tick)
    }

    fun addTick(value: Double, major: Boolean, level: Int, showLabel: Boolean, formatter: PlotFormatter?, data: Any?): PlotTick {
        val tick = PlotTick(value, major, level, showLabel)
        if (showLabel && formatter != null) {
            tick.textOffset = textBuffer.size
            val buff = formatter(tick.plotPos, data)
            textBuffer += buff
            tick.labelSize put ImGui.calcTextSize(textBuffer[tick.textOffset])
        }
        return addTick(tick)
    }

    infix fun addTick(tick: PlotTick): PlotTick {
        if (tick.showLabel) {
            maxSize.x = if (tick.labelSize.x > maxSize.x) tick.labelSize.x else maxSize.x
            maxSize.y = if (tick.labelSize.y > maxSize.y) tick.labelSize.y else maxSize.y
        }
        tick.idx = ticks.size
        ticks += tick
        return tick
    }

    infix fun getText(idx: Int) = textBuffer[ticks[idx].textOffset]

    infix fun getText(tick: PlotTick) = getText(tick.idx)

    infix fun overrideSizeLate(size: Vec2) {
        lateSize.x = if (size.x > lateSize.x) size.x else lateSize.x
        lateSize.y = if (size.y > lateSize.y) size.y else lateSize.y
    }

    fun reset() {
        ticks.clear()
        textBuffer.clear()
        maxSize put lateSize
        lateSize put 0f
        levels = 1
    }

    val tickCount get() = ticks.size
}

// Axis state information that must persist after EndPlot
class PlotAxis {
    var id: ID = 0
    var flags: PlotAxisFlags = none
    var previousFlags: PlotAxisFlags = none
    var range = PlotRange(0.0, 1.0)
    var rangeCond: Cond = Cond.None
    var scale = PlotScale.Linear
    var fitExtents = PlotRange(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
    var orthoAxis: KMutableProperty0<PlotAxis>? = null
    var constraintRange = PlotRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
    var constraintZoom = PlotRange(Double.MIN_VALUE, Double.POSITIVE_INFINITY)

    var ticker = PlotTicker()
    var formatter: PlotFormatter? = null
    var formatterData: Any? = null
    var formatSpec = ""
    var locator: PlotLocator? = null

    var linkedMin: KMutableProperty0<Double>? = null
    var linkedMax: KMutableProperty0<Double>? = null

    var pickerLevel = 0
    var pickerTimeMin = PlotTime()
    var pickerTimeMax = PlotTime()

    var transformForward: PlotTransform? = null
    var transformInverse: PlotTransform? = null
    var transformData: Any? = null
    var pixelMin = 0f
    var pixelMax = 0f
    var scaleMin = 0.0
    var scaleMax = 0.0
    var scaleToPixel = 0.0
    var datum1 = 0f
    var datum2 = 0f

    val hoverRect = Rect()
    var labelOffset = -1
    var colorMaj = 0u
    var colorMin = 0u
    var colorTick = 0u
    var colorTxt = 0u
    var colorBg = 0u
    var colorHov = 0u
    var colorAct = 0u
    var colorHiLi = COL32_BLACK_TRANS.toUInt()

    var enabled = false
    var vertical = false
    var fitThisFrame = false
    var hasRange = false
    var hasFormatSpec = false
    var showDefaultTicks = true
    var hovered = false
    var held = false

    fun reset() {
        enabled = false
        scale = PlotScale.Linear
        transformForward = null; transformInverse = null
        transformData = null
        labelOffset = -1
        hasFormatSpec = false
        formatter = null
        formatterData = null
        locator = null
        showDefaultTicks = true
        fitThisFrame = false
        fitExtents.min = Double.POSITIVE_INFINITY
        fitExtents.max = Double.NEGATIVE_INFINITY
        orthoAxis = null
        constraintRange = PlotRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
        constraintZoom = PlotRange(Double.MIN_VALUE, Double.POSITIVE_INFINITY)
        ticker.reset()
    }

    fun setMin(_min: Double, force: Boolean = false): Boolean {
        if (!force && isLockedMin)
            return false
        var min = _min.constrainInf().constrainNan()
        if (min < constraintRange.min)
            min = constraintRange.min
        val z = range.max - min
        if (z < constraintZoom.min)
            min = range.max - constraintZoom.min
        if (z > constraintZoom.max)
            min = range.max - constraintZoom.max
        if (min >= range.max)
            return false
        range.min = min
        pickerTimeMin = PlotTime fromDouble range.min
        updateTransformCache()
        return true
    }

    fun setMax(_max: Double, force: Boolean = false): Boolean {
        if (!force && isLockedMax)
            return false
        var max = _max.constrainInf().constrainNan()
        if (max > constraintRange.max)
            max = constraintRange.max
        val z = _max - range.min
        if (z < constraintZoom.min)
            max = range.min + constraintZoom.min
        if (z > constraintZoom.max)
            max = range.min + constraintZoom.max
        if (max <= range.min)
            return false
        range.max = max
        pickerTimeMax = PlotTime fromDouble range.max
        updateTransformCache()
        return true
    }

    fun setRange(v1: Double, v2: Double) {
        range.min = v1 min v2
        range.max = v1 max v2
        constrain()
        pickerTimeMin = PlotTime fromDouble range.min
        pickerTimeMax = PlotTime fromDouble range.max
        updateTransformCache()
    }

    infix fun setRange_(range: PlotRange) = setRange(range.min, range.max)

    infix fun setAspect(unitPerPix: Double) {
        val newSize = unitPerPix * pixelSize
        val delta = (newSize - range.size) * 0.5
        if (isLocked)
            return
        else if (isLockedMin && !isLockedMax)
            setRange(range.min, range.max + 2 * delta)
        else if (!isLockedMin && isLockedMax)
            setRange(range.min - 2 * delta, range.max)
        else
            setRange(range.min - delta, range.max + delta)
    }

    val pixelSize get() = abs(pixelMax - pixelMin)

    val aspect get() = range.size / pixelSize

    fun constrain() {
        range.min = range.min.constrainInf().constrainNan()
        range.max = range.max.constrainInf().constrainNan()
        if (range.min < constraintRange.min)
            range.min = constraintRange.min
        if (range.max > constraintRange.max)
            range.max = constraintRange.max
        val z = range.size
        if (z < constraintZoom.min) {
            val delta = (constraintZoom.min - z) * 0.5
            range.min -= delta
            range.max += delta
        }
        if (z > constraintZoom.max) {
            val delta = (z - constraintZoom.max) * 0.5
            range.min += delta
            range.max -= delta
        }
        if (range.max <= range.min)
            range.max = range.min + Double.MIN_VALUE
    }

    fun updateTransformCache() {
        scaleToPixel = (pixelMax - pixelMin) / range.size
        if (transformForward != null) {
            scaleMin = transformForward!!(range.min, transformData)
            scaleMax = transformForward!!(range.max, transformData)
        } else {
            scaleMin = range.min
            scaleMax = range.max
        }
    }

    infix fun plotToPixels(plt_: Double): Float {
        var plt = plt_
        if (transformForward != null) {
            val s = transformForward!!(plt, transformData)
            val t = (s - scaleMin) / (scaleMax - scaleMin)
            plt = range.min + range.size * t
        }
        return (pixelMin + scaleToPixel * (plt - range.min)).f
    }


    infix fun pixelsToPlot(pix: Float): Double {
        var plt = (pix - pixelMin) / scaleToPixel + range.min
        if (transformInverse != null) {
            val t = (plt - range.min) / range.size
            val s = t * (scaleMax - scaleMin) + scaleMin
            plt = transformInverse!!(s, transformData)
        }
        return plt
    }

    infix fun extendFit(v: Double) {
        if (!v.nanOrInf && v >= constraintRange.min && v <= constraintRange.max) {
            fitExtents.min = if (v < fitExtents.min) v else fitExtents.min
            fitExtents.max = if (v > fitExtents.max) v else fitExtents.max
        }
    }

    fun extendFitWith(alt: PlotAxis, v: Double, vAlt: Double) {
        if (flags has PlotAxisFlag.RangeFit && vAlt !in alt.range)
            return
        if (!v.nanOrInf && v >= constraintRange.min && v <= constraintRange.max) {
            fitExtents.min = if (v < fitExtents.min) v else fitExtents.min
            fitExtents.max = if (v > fitExtents.max) v else fitExtents.max
        }
    }

    infix fun applyFit(padding: Float) {
        val extSize = fitExtents.size * 0.5
        fitExtents.min -= extSize * padding
        fitExtents.max += extSize * padding
        if (!isLockedMin && !fitExtents.min.nanOrInf)
            range.min = fitExtents.min
        if (!isLockedMax && !fitExtents.max.nanOrInf)
            range.max = fitExtents.max
        if (almostEqual(range.min, range.max)) {
            range.max += 0.5
            range.min -= 0.5
        }
        constrain()
        updateTransformCache()
    }

    val hasLabel get() = labelOffset != -1 && flags hasnt PlotAxisFlag.NoLabel
    val hasGridLines get() = flags hasnt PlotAxisFlag.NoGridLines
    val hasTickLabels get() = flags hasnt PlotAxisFlag.NoTickLabels
    val hasTickMarks get() = flags hasnt PlotAxisFlag.NoTickMarks
    val willRender get() = enabled && (hasGridLines || hasTickLabels || hasTickMarks)
    val isOpposite get() = flags has PlotAxisFlag.Opposite
    val isInverted get() = flags has PlotAxisFlag.Invert
    val isForeground get() = flags has PlotAxisFlag.Foreground
    val isAutoFitting get() = flags has PlotAxisFlag.AutoFit
    val canInitFit get() = flags hasnt PlotAxisFlag.NoInitialFit && !hasRange && linkedMin == null && linkedMax == null
    val isRangeLocked get() = hasRange && rangeCond == Cond.Always
    val isLockedMin get() = !enabled || isRangeLocked || flags has PlotAxisFlag.LockMin
    val isLockedMax get() = !enabled || isRangeLocked || flags has PlotAxisFlag.LockMax
    val isLocked get() = isLockedMin && isLockedMax
    val isInputLockedMin get() = isLockedMin || isAutoFitting
    val isInputLockedMax get() = isLockedMax || isAutoFitting
    val isInputLocked get() = isLocked || isAutoFitting
    val hasMenus get() = flags hasnt PlotAxisFlag.NoMenus

    infix fun isPanLocked(increasing: Boolean): Boolean = when {
        flags has PlotAxisFlag.PanStretch -> isInputLocked
        isLockedMin || isLockedMax || isAutoFitting -> false
        increasing -> range.max == constraintRange.max
        else -> range.min == constraintRange.min
    }

    fun pushLinks() {
        linkedMin?.set(range.min)
        linkedMax?.set(range.max)
    }

    fun pullLinks() {
        linkedMin?.let { setMin(it(), true) }
        linkedMax?.let { setMax(it(), true) }
    }
}

// Align plots group data
class PlotAlignmentData {
    var vertical = true
    var padA = 0f
    var padB = 0f
    var padAMax = 0f
    var padBMax = 0f

    fun begin() {; padAMax = 0f; padBMax = 0f; }
    fun update(refPadA: KMutableProperty0<Float>, refPadB: KMutableProperty0<Float>, refDeltaA: KMutableProperty0<Float>, refDeltaB: KMutableProperty0<Float>) {
        var padA by refPadA
        var padB by refPadB
        var deltaA by refDeltaA
        var deltaB by refDeltaB
        val bakA = padA
        val bakB = padB
        if (padAMax < padA) padAMax = padA
        if (padBMax < padB) padBMax = padB
        if (padA < this.padA) {; padA = this.padA; deltaA = padA - bakA; } else deltaA = 0f
        if (padB < this.padB) {; padB = this.padB; deltaB = padB - bakB; } else deltaB = 0f
    }

    fun end() {; padA = padAMax; padB = padBMax; }
    fun reset() {; padA = 0f; padB = 0f; padAMax = 0f; padBMax = 0f; }
}

// State information for Plot items
class PlotItem {
    var id: ID = 0
    var color = COL32_WHITE.toUInt()
    val legendHoverRect = Rect()
    var nameOffset = -1
    var show = true
    var legendHovered = false
    var seenThisFrame = false

//    ~ImPlotItem() { ID = 0; }
}

// Holds Legend state
class PlotLegend {
    var flags: PlotLegendFlags = none
    var previousFlags: PlotLegendFlags = none
    var location = PlotLocation.NorthWest
    var previousLocation = PlotLocation.NorthWest
    val indices = ArrayList<Int>()
    val labels = ArrayList<String>()
    val rect = Rect()
    var hovered = false
    var held = false
    var canGoInside = true

    fun reset() {; indices.clear(); labels.clear(); }
}

// Holds Items and Legend data
class PlotItemGroup {
    var id: ID = 0
    var legend = PlotLegend()
    val itemPool = Pool { PlotItem() }
    var colormapIdx = 0

    val itemCount get() = itemPool.bufSize
    infix fun getItemID(labelId: String) = ImGui.getID(labelId) /* GetIDWithSeed */
    infix fun getItem(id: ID) = itemPool.getByKey(id)
    infix fun getItem(labelId: String) = getItem(getItemID(labelId))
    infix fun getOrAddItem(id: ID) = itemPool.getOrAddByKey(id)
    infix fun getItemByIndex(i: Int) = itemPool.getByIndex(i)
    infix fun getItemIndex(item: PlotItem) = itemPool.getIndex(item)
    val legendCount get() = legend.indices.size
    infix fun getLegendItem(i: Int) = itemPool.getByIndex(legend.indices[i])
    infix fun getLegendLabel(i: Int) = legend.labels[getLegendItem(i).nameOffset]
    fun reset() {; itemPool.clear(); legend.reset(); colormapIdx = 0; }
}

// Holds Plot state information that must persist after EndPlot
class PlotPlot {
    var id: ID = 0
    var flags: PlotFlags = none
    var previousFlags: PlotFlags = none
    var mouseTextLocation = PlotLocation.SouthEast
    var mouseTextFlags: PlotMouseTextFlags = none
    val axes = Array(Axis.COUNT) { PlotAxis() }
    val textBuffer = ArrayList<String>()
    val items = PlotItemGroup()
    var currentX = Axis.X1
    var currentY = Axis.Y1
    val frameRect = Rect()
    val canvasRect = Rect()
    val plotRect = Rect()
    val axesRect = Rect()
    val selectRect = Rect()
    val selectStart = Vec2()
    var titleOffset = -1
    var justCreated = true
    var initialized = false
    var setupLocked = false
    var fitThisFrame = false
    var hovered = false
    var held = false
    var selecting = false
    var selected = false
    var contextLocked = false

    init {
        for (i in 0..<PLOT_NUM_X_AXES)
            xAxis(i).vertical = false
        for (i in 0..<PLOT_NUM_Y_AXES)
            yAxis(i).vertical = true
    }

    val isInputLocked: Boolean
        get() {
            for (i in 0..<PLOT_NUM_X_AXES) {
                if (!xAxis(i).isInputLocked)
                    return false
            }
            for (i in 0..<PLOT_NUM_Y_AXES) {
                if (!yAxis(i).isInputLocked)
                    return false
            }
            return true
        }

    fun clearTextBuffer() = textBuffer.clear()

    var title: String?
        get() = textBuffer[titleOffset]
        set(value) {
            if (value != null && ImGui.findRenderedTextEnd(value) != 0) {
                titleOffset = textBuffer.size
                textBuffer += value
            } else
                titleOffset = -1
        }
    val hasTitle get() = titleOffset != -1 && flags hasnt PlotFlag.NoTitle

    infix fun xAxis(i: Int): PlotAxis = axes[Axis.X1.i + i]
    infix fun yAxis(i: Int): PlotAxis = axes[Axis.Y1.i + i]

    fun enabledAxesX(): Int {
        var cnt = 0
        for (i in 0..<PLOT_NUM_X_AXES)
            cnt += xAxis(i).enabled.i
        return cnt
    }

    fun enabledAxesY(): Int {
        var cnt = 0
        for (i in 0..<PLOT_NUM_Y_AXES)
            cnt += yAxis(i).enabled.i
        return cnt
    }


    fun setAxisLabel(axis: PlotAxis, label: String?) {
        if (label != null && ImGui.findRenderedTextEnd(label) != 0) {
            axis.labelOffset = textBuffer.size
            textBuffer += label
        } else
            axis.labelOffset = -1
    }

    infix fun getAxisLabel(axis: PlotAxis) = textBuffer[axis.labelOffset]
}

// Holds subplot data that must persist after EndSubplot
class PlotSubplot {
    var id: ID = 0
    var flags: PlotSubplotFlags = none
    var previousFlags: PlotSubplotFlags = none
    val items = PlotItemGroup()

    init {
        items.legend.location = PlotLocation.North
        items.legend.flags = PlotLegendFlag.Horizontal / PlotLegendFlag.Outside
        items.legend.canGoInside = false
    }

    var rows = 0
    var cols = 0
    var currentIdx = 0
    val frameRect = Rect()
    val gridRect = Rect()
    val cellSize = Vec2()
    var rowAlignmentData = arrayOf<PlotAlignmentData>()
    var colAlignmentData = arrayOf<PlotAlignmentData>()
    var rowRatios = FloatArray(0)
    var colRatios = FloatArray(0)
    var rowLinkData = arrayOf<PlotRange>()
    var colLinkData = arrayOf<PlotRange>()
    val tempSizes = FloatArray(2)
    var frameHovered = false
    var hasTitle = false
}

// Temporary data storage for upcoming plot
class PlotNextPlotData {
    val rangeCond: Array<Cond> = Array(Axis.COUNT) { Cond.None }
    val range = Array(Axis.COUNT) { PlotRange() }
    val hasRange = BooleanArray(Axis.COUNT)
    val fit = BooleanArray(Axis.COUNT)
    val linkedMin: Array<KMutableProperty0<Double>?> = Array(Axis.COUNT) { null }
    val linkedMax: Array<KMutableProperty0<Double>?> = Array(Axis.COUNT) { null }

    fun reset() {
        for (i in 0..<Axis.COUNT) {
            hasRange[i] = false
            fit[i] = false
            linkedMin[i] = null; linkedMax[i] = null
        }
    }
}

// Temporary data storage for upcoming item
class PlotNextItemData {
    val colors = Array(5) { Vec4(PLOT_AUTO_COL) } // ImPlotCol_Line, ImPlotCol_Fill, ImPlotCol_MarkerOutline, ImPlotCol_MarkerFill, ImPlotCol_ErrorBar
    var lineWeight = PLOT_AUTO
    var marker = PlotMarker.None
    var markerSize = PLOT_AUTO
    var markerWeight = PLOT_AUTO
    var fillAlpha = PLOT_AUTO
    var errorBarSize = PLOT_AUTO
    var errorBarWeight = PLOT_AUTO
    var digitalBitHeight = PLOT_AUTO
    var digitalBitGap = PLOT_AUTO
    var renderLine = false
    var renderFill = false
    var renderMarkerLine = false
    var renderMarkerFill = false
    var hasHidden = false
    var hidden = false
    var hiddenCond: Cond = Cond.None

    //    ImPlotNextItemData() { Reset(); }
    fun reset() {
        for (i in 0..<5)
            colors[i] put PLOT_AUTO_COL
        lineWeight = PLOT_AUTO; markerSize = PLOT_AUTO; markerWeight = PLOT_AUTO; fillAlpha = PLOT_AUTO; errorBarSize = PLOT_AUTO; errorBarWeight = PLOT_AUTO; digitalBitHeight = PLOT_AUTO; digitalBitGap = PLOT_AUTO
        marker = PlotMarker.None
        hasHidden = false; hidden = false
    }
}

