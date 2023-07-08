@file:OptIn(ExperimentalStdlibApi::class)

package plot.internalApi

import glm_.*
import glm_.vec2.Vec2
import imgui.internal.round
import imgui.max
import plot.api.*
import kotlin.math.*

//------------------------------------------------------------------------------
// [SECTION] Locator
//------------------------------------------------------------------------------

const val TICK_FILL_X = 0.8f
const val TICK_FILL_Y = 1f

fun locator_Default(ticker: PlotTicker, range: PlotRange, pixels: Float, vertical: Boolean, formatter: PlotFormatter, formatterData: Any?) {
    if (range.min == range.max)
        return
    val nMinor = 10
    val nMajor = 2 max round(pixels / (if (vertical) 300f else 400f)).i
    val nice_range = niceNum(range.size * 0.99, false)
    val interval = niceNum(nice_range / (nMajor - 1), true)
    val graphmin = floor(range.min / interval) * interval
    val graphmax = ceil(range.max / interval) * interval
    var firstMajorSet = false
    var firstMajorIdx = 0
    val idx0 = ticker.tickCount // ticker may have user custom ticks
    val totalSize = Vec2()
    var major = graphmin
    while (major < graphmax + 0.5 * interval) {
        // is this zero? combat zero formatting issues
        if (major - interval < 0 && major + interval > 0)
            major = 0.0
        if (major in range) {
            if (!firstMajorSet) {
                firstMajorIdx = ticker.tickCount
                firstMajorSet = true
            }
            totalSize += ticker.addTick(major, true, 0, true, formatter, formatterData).labelSize
        }
        for (i in 1..<nMinor) {
            val minor = major + i * interval / nMinor
            if (minor in range)
                totalSize += ticker.addTick(minor, false, 0, true, formatter, formatterData).labelSize
        }
        major += interval
    }
    // prune if necessary
    if ((!vertical && totalSize.x > pixels * TICK_FILL_X) || (vertical && totalSize.y > pixels * TICK_FILL_Y)) {
        var i = firstMajorIdx - 1
        while (i >= idx0) {
            ticker.ticks[i].showLabel = false
            i -= 2
        }
        i = firstMajorIdx + 1
        while (i < ticker.tickCount) {
            ticker.ticks[i].showLabel = false
            i += 2
        }
    }
}

fun locator_Time(ticker: PlotTicker, range: PlotRange, pixels: Float, vertical: Boolean, formatter: PlotFormatter, formatterData: Any?) {
    assert(!vertical) { "Cannot locate Time ticks on vertical axis!" }
//    (void)vertical
    // get units for level 0 and level 1 labels
    val unit0 = getUnitForRange(range.size / (pixels / 100)) // level = 0 (top)
    val unit1 = if (unit0 == TimeUnit.Yr) TimeUnit.Yr else TimeUnit of (unit0.i + 1)  // level = 1 (bottom)
    // get time format specs
    val fmt0 = getDateTimeFmt(timeFormatLevel0, unit0)
    val fmt1 = getDateTimeFmt(timeFormatLevel1, unit1)
    val fmtf = getDateTimeFmt(timeFormatLevel1First, unit1)
    // min max times
    val tMin = PlotTime fromDouble range.min
    val tMax = PlotTime fromDouble range.max
    // maximum allowable density of labels
    val maxDensity = 0.5f
    // book keeping
    var lastMajorOffset = -1
    // formatter data
    val ftd = Formatter_Time_Data()
    ftd.userFormatter = formatter
    ftd.userFormatterData = formatterData
    if (unit0 != TimeUnit.Yr) {
        // pixels per major (level 1) division
        val pixPerMajorDiv = pixels / (range.size / timeUnitSpans[unit1.i])
        // nominal pixels taken up by labels
        val fmt0Width = getDateTimeWidth(fmt0)
        val fmt1Width = getDateTimeWidth(fmt1)
        val fmtfWidth = getDateTimeWidth(fmtf)
        // the maximum number of minor (level 0) labels that can fit between major (level 1) divisions
        val minorPerMajor = (maxDensity * pixPerMajorDiv / fmt0Width).i
        // the minor step size (level 0)
        val step = getTimeStep(minorPerMajor, unit0)
        // generate ticks
        var t1 = floorTime(PlotTime fromDouble range.min, unit1)
        while (t1 < tMax) {
            // get next major
            val t2 = addTime(t1, unit1, 1)
            // add major tick
            if (t1 in tMin..tMax) {
                // minor level 0 tick
                ftd.time = t1.copy(); ftd.spec = fmt0.copy()
                ticker.addTick(t1.toDouble, true, 0, true, ::formatter_Time, ftd)
                // major level 1 tick
                ftd.time = t1; ftd.spec = (if (lastMajorOffset < 0) fmtf else fmt1).copy()
                val tickMaj = ticker.addTick(t1.toDouble, true, 1, true, ::formatter_Time, ftd)
                val thisMajor = ticker.getText(tickMaj)
                if (lastMajorOffset >= 0 && timeLabelSame(ticker.textBuffer[lastMajorOffset], thisMajor))
                    tickMaj.showLabel = false
                lastMajorOffset = tickMaj.textOffset
            }
            // add minor ticks up until next major
            if (minorPerMajor > 1 && (tMin <= t2 && t1 <= tMax)) {
                var t12 = addTime(t1, unit0, step)
                while (t12 < t2) {
                    val pxToT2 = ((t2 - t12).toDouble / range.size).d * pixels
                    if (t12 in tMin..tMax) {
                        ftd.time = t12.copy(); ftd.spec = fmt0.copy()
                        ticker.addTick(t12.toDouble, false, 0, pxToT2 >= fmt0Width, ::formatter_Time, ftd)
                        if (lastMajorOffset < 0 && pxToT2 >= fmt0Width && pxToT2 >= (fmt1Width + fmtfWidth) / 2) {
                            ftd.time = t12.copy(); ftd.spec = fmtf.copy()
                            val tickMaj = ticker.addTick(t12.toDouble, true, 1, true, ::formatter_Time, ftd)
                            lastMajorOffset = tickMaj.textOffset
                        }
                    }
                    t12 = addTime(t12, unit0, step)
                }
            }
            t1 = t2.copy()
        }
    } else {
        val fmty = getDateTimeFmt(timeFormatLevel0, TimeUnit.Yr)
        val labelWidth = getDateTimeWidth(fmty)
        val maxLabels = (maxDensity * pixels / labelWidth).i
        val yearMin = tMin.year
        val yearMax = ceilTime(tMax, TimeUnit.Yr).year
        val niceRange = niceNum((yearMax - yearMin) * 0.99, false)
        val interval = niceNum(niceRange / (maxLabels - 1), true)
        val graphMin = (floor(yearMin / interval) * interval).i
        val graphMax = (ceil(yearMax / interval) * interval).i
        val step = if (interval.i <= 0) 1 else interval.i

        for (y in graphMin..<graphMax step step) {
            val t = makeTime(y)
            if (t in tMin..tMax) {
                ftd.time = t.copy(); ftd.spec = fmty.copy()
                ticker.addTick(t.toDouble, true, 0, true, ::formatter_Time, ftd)
            }
        }
    }
}

internal var IntArray.min: Int
    get() = get(0)
    set(value) = set(0, value)
internal var IntArray.max: Int
    get() = get(1)
    set(value) = set(1, value)
internal var IntArray.step: Int
    get() = get(2)
    set(value) = set(2, value)

// [JVM] exp: [min, max, step]
fun calcLogarithmicExponents(range: PlotRange, pix: Float, vertical: Boolean, exp: IntArray): Boolean {
    if (range.min * range.max > 0) {
        val nMajor = 2 max round(pix * if (vertical) 0.02f else 0.01f).i // TODO: magic numbers
        val logMin = log10(abs(range.min))
        val logMax = log10(abs(range.max))
        val logA = logMin min logMax
        val logB = logMin max logMax
        exp.step = 1 max ((logB - logA).i / nMajor)
        exp.min = logA.i
        exp.max = logB.i
        if (exp.step != 1) {
            while (exp.step % 3 != 0) exp.step++ // make step size multiple of three
            while (exp.min % exp.step != 0) exp.min--  // decrease exp_min until exp_min + N * exp_step will be 0
        }
        return true
    }
    return false
}

fun addTicksLogarithmic(range: PlotRange, expMin: Int, expMax: Int, expStep: Int, ticker: PlotTicker, formatter: PlotFormatter, data: Any?) {
    val sign = sign(range.max)
    var e = expMin - expStep
    while (e < (expMax + expStep)) {
        var major1 = sign * 10.pow(e)
        var major2 = sign * 10.pow(e + 1)
        var interval = (major2 - major1) / 9
        if (major1 >= (range.min - Double.MIN_VALUE) && major1 <= (range.max + Double.MIN_VALUE))
            ticker.addTick(major1, true, 0, true, formatter, data)
        for (j in 0..<expStep) {
            major1 = sign * 10.pow(e + j)
            major2 = sign * 10.pow(e + j + 1)
            interval = (major2 - major1) / 9
            for (i in 1..<(9 + (j < (expStep - 1)).i)) {
                val minor = major1 + i * interval
                if (minor >= (range.min - Double.MIN_VALUE) && minor <= (range.max + Double.MIN_VALUE))
                    ticker.addTick(minor, false, 0, false, formatter, data)
            }
        }
        e += expStep
    }
}

fun locator_Log10(ticker: PlotTicker, range: PlotRange, pixels: Float, vertical: Boolean, formatter: PlotFormatter, formatterData: Any?) {
    val exp = IntArray(3)
    if (calcLogarithmicExponents(range, pixels, vertical, exp))
        addTicksLogarithmic(range, exp.min, exp.max, exp.step, ticker, formatter, formatterData)
}

fun calcSymLogPixel(plt_: Double, range: PlotRange, pixels: Float): Float {
    var plt = plt_
    val scaleToPixels = pixels / range.size
    val scaleMin = transformForward_SymLog(range.min, null)
    val scaleMax = transformForward_SymLog(range.max, null)
    val s = transformForward_SymLog(plt, null)
    val t = (s - scaleMin) / (scaleMax - scaleMin)
    plt = range.min + range.size * t

    return (0 + scaleToPixels * (plt - range.min)).f
}

fun locator_SymLog(ticker: PlotTicker, range: PlotRange, pixels: Float, vertical: Boolean, formatter: PlotFormatter, formatterData: Any?) {
    if (range.min >= -1 && range.max <= 1)
        locator_Default(ticker, range, pixels, vertical, formatter, formatterData)
    else if (range.min * range.max < 0) { // cross zero
        val pixMin = 0
        val pixMax = pixels
        val pixP1 = calcSymLogPixel(1.0, range, pixels)
        val pixN1 = calcSymLogPixel(-1.0, range, pixels)
        val expP = IntArray(3)
        val expN = IntArray(3)
        calcLogarithmicExponents(PlotRange(1.0, range.max), abs(pixMax - pixP1), vertical, expP)
        calcLogarithmicExponents(PlotRange(range.min, -1.0), abs(pixN1 - pixMin), vertical, expN)
        val expStep = expN.step max expP.step
        ticker.addTick(0.0, true, 0, true, formatter, formatterData)
        addTicksLogarithmic(PlotRange(1.0, range.max), expP.min, expP.max, expStep, ticker, formatter, formatterData)
        addTicksLogarithmic(PlotRange(range.min, -1.0), expN.min, expN.max, expStep, ticker, formatter, formatterData)
    } else
        locator_Log10(ticker, range, pixels, vertical, formatter, formatterData)
}