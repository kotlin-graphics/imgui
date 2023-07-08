@file:OptIn(ExperimentalStdlibApi::class)

package plot.api

import glm_.d
import glm_.i
import glm_.min
import imgui.ImGui
import plot.internalApi.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import kotlin.math.abs

//-----------------------------------------------------------------------------
// Time Ticks and Utils
//-----------------------------------------------------------------------------

// this may not be thread safe?
val timeUnitSpans = doubleArrayOf(0.000001,
                                  0.001,
                                  1.0,
                                  60.0,
                                  3600.0,
                                  86400.0,
                                  2629800.0,
                                  31557600.0)

private val cutoffs = doubleArrayOf(0.001, 1.0, 60.0, 3600.0, 86400.0, 2629800.0, 31557600.0, PLOT_MAX_TIME.d)
fun getUnitForRange(range: Double): TimeUnit {
    for (i in 0..<TimeUnit.COUNT) {
        if (range <= cutoffs[i])
            return TimeUnit of i
    }
    return TimeUnit.Yr
}

fun lowerBoundStep(maxDivs: Int, divs: IntArray, step: IntArray): Int {
    if (maxDivs < divs[0])
        return 0
    for (i in 1..<divs.size) {
        if (maxDivs < divs[i])
            return step[i - 1]
    }
    return step.last()
}

fun getTimeStep(maxDivs: Int, unit: TimeUnit): Int = when (unit) {
    TimeUnit.Ms, TimeUnit.Us -> {
        val step = intArrayOf(500, 250, 200, 100, 50, 25, 20, 10, 5, 2, 1)
        val divs = intArrayOf(2, 4, 5, 10, 20, 40, 50, 100, 200, 500, 1000)
        lowerBoundStep(maxDivs, divs, step)
    }
    TimeUnit.S, TimeUnit.Min -> {
        val step = intArrayOf(30, 15, 10, 5, 1)
        val divs = intArrayOf(2, 4, 6, 12, 60)
        lowerBoundStep(maxDivs, divs, step)
    }
    TimeUnit.Hr -> {
        val step = intArrayOf(12, 6, 3, 2, 1)
        val divs = intArrayOf(2, 4, 8, 12, 24)
        lowerBoundStep(maxDivs, divs, step)
    }
    TimeUnit.Day -> {
        val step = intArrayOf(14, 7, 2, 1)
        val divs = intArrayOf(2, 4, 14, 28)
        lowerBoundStep(maxDivs, divs, step)
    }
    TimeUnit.Mo -> {
        val step = intArrayOf(6, 3, 2, 1)
        val divs = intArrayOf(2, 4, 6, 12)
        lowerBoundStep(maxDivs, divs, step)
    }
    else -> 0
}

fun mkGmtTime(ptm: LocalDateTime): PlotTime {
    val gmt = ptm.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("GMT"))
    val t = PlotTime()
    t.s = gmt.toEpochSecond()
    if (t.s < 0)
        t.s = 0
    return t
}

fun getGmtTime(t: PlotTime) = LocalDateTime.ofInstant(Instant.ofEpochSecond(t.s, t.μs * 1_000L), ZoneId.of("GMT"))

fun mkLocTime(ptm: LocalDateTime): PlotTime {
    val t = PlotTime()
    t.s = ptm.atZone(ZoneId.systemDefault()).toEpochSecond()
    if (t.s < 0)
        t.s = 0
    return t
}

fun getLocTime(t: PlotTime): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(t.s, t.μs * 1_000L), ZoneId.systemDefault())

fun mkTime(ptm: LocalDateTime): PlotTime = when {
    style.useLocalTime -> mkLocTime(ptm)
    else -> mkGmtTime(ptm)
}

fun getTime(t: PlotTime): LocalDateTime {
    val tm = when {
        style.useLocalTime -> getLocTime(t)
        else -> getGmtTime(t)
    }
    gImPlot.tm = tm
    return tm
}

fun makeTime(year: Int, month: Int = 0, day: Int = 1, hour: Int = 0, min: Int = 0, sec_: Int = 0, us_: Int = 0): PlotTime {
    var sec = sec_
    var us = us_
//    val tm = gImPlot.tm

//    var yr = year - 1900
//    if (yr < 0)
//        yr = 0

    sec += us / 1_000_000
    us %= 1_000_000

    val tm = LocalDateTime.of(year, Month.of(month), day, hour, min, sec)

    val t = mkTime(tm)

    t.μs = us
    return t
}

//int GetYear(const ImPlotTime& t) {
//    tm& Tm = GImPlot->Tm;
//    GetTime(t, &Tm);
//    return Tm.tm_year + 1900;
//}

fun addTime(t: PlotTime, unit: TimeUnit, count: Int): PlotTime {
    val tOut = t.copy()
    when (unit) {
        TimeUnit.Us -> tOut.μs += count
        TimeUnit.Ms -> tOut.μs += count * 1000
        TimeUnit.S -> tOut.s += count
        TimeUnit.Min -> tOut.s += count * 60
        TimeUnit.Hr -> tOut.s += count * 3600
        TimeUnit.Day -> tOut.s += count * 86400
        TimeUnit.Mo ->
            for (i in 0..<abs(count)) {
                val tm = getTime(tOut)
                if (count > 0)
                    tOut.s += 86400 * getDaysInMonth(tm.year, tm.monthValue)
                else if (count < 0)
                    tOut.s -= 86400 * getDaysInMonth(tm.year - if (tm.monthValue == 0) 1 else 0, if (tm.monthValue == 0) 11 else tm.monthValue - 1) // NOT WORKING
            }
        TimeUnit.Yr ->
            for (i in 0..<abs(count)) {
                if (count > 0)
                    tOut.s += 86400 * (365 + isLeapYear(tOut.year).i)
                else if (count < 0)
                    tOut.s -= 86400 * (365 + isLeapYear(tOut.year - 1).i)
                // this is incorrect if leap year and we are past Feb 28
            }
    }
    tOut.rollOver()
    return tOut
}

fun floorTime(t: PlotTime, unit: TimeUnit): PlotTime {
    val gp = gImPlot
    var tm = getTime(t)
    when (unit) {
        TimeUnit.S -> return PlotTime(t.s, 0)
        TimeUnit.Ms -> return PlotTime(t.s, (t.μs / 1_000) * 1_000)
        TimeUnit.Us -> return t
        else -> {
            tm = tm.withSecond(0)
            if (unit != TimeUnit.Min) {
                tm = tm.withMinute(0)
                if (unit != TimeUnit.Hr) {
                    tm = tm.withHour(0)
                    if (unit != TimeUnit.Day) {
                        tm = tm.withDayOfMonth(1)
                        if (unit != TimeUnit.Mo)
                            tm = tm.withMonth(0)
                    }
                }
            }
        }
    }
    return mkTime(tm)
}

fun ceilTime(t: PlotTime, unit: TimeUnit) = addTime(floorTime(t, unit), unit, 1)

//ImPlotTime RoundTime(const ImPlotTime& t, ImPlotTimeUnit unit) {
//    ImPlotTime t1 = FloorTime(t, unit);
//    ImPlotTime t2 = AddTime(t1,unit,1);
//    if (t1.S == t2.S)
//        return t.Us - t1.Us < t2.Us - t.Us ? t1 : t2;
//    return t.S - t1.S < t2.S - t.S ? t1 : t2;
//}

fun combineDateTime(datePart: PlotTime, todPart: PlotTime): PlotTime {
    val gp = gImPlot
    var tm = getTime(datePart)
    val y = tm.year
    val m = tm.monthValue
    val d = tm.dayOfMonth
    tm = getTime(todPart)
    tm = tm.withYear(y)
    tm = tm.withMonth(m)
    tm = tm.withDayOfMonth(d)
    val t = mkTime(tm)
    t.μs = todPart.μs
    return t
}

//// TODO: allow users to define these
//static const char* MONTH_NAMES[]  = {"January","February","March","April","May","June","July","August","September","October","November","December"};
//static const char* WD_ABRVS[]     = {"Su","Mo","Tu","We","Th","Fr","Sa"};
//static const char* MONTH_ABRVS[]  = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

fun formatTime(t: PlotTime, fmt: TimeFmt, use24HrClk: Boolean): String {
    val tm = getTime(t)
    val us = t.μs % 1_000
    val ms = t.μs / 1_000
    val sec = tm.second
    val min = tm.minute
    return when {
        use24HrClk -> {
            val hr = tm.hour
            when (fmt) {
                TimeFmt.Us -> ".%03d %03d".format(ms, us)
                TimeFmt.SUs -> ":%02d.%03d %03d".format(sec, ms, us)
                TimeFmt.SMs -> ":%02d.%03d".format(sec, ms)
                TimeFmt.S -> ":%02d".format(sec)
                TimeFmt.MinSMs -> ":%02d:%02d.%03d".format(min, sec, ms)
                TimeFmt.HrMinSMs -> "%02d:%02d:%02d.%03d".format(hr, min, sec, ms)
                TimeFmt.HrMinS -> "%02d:%02d:%02d".format(hr, min, sec)
                TimeFmt.HrMin -> "%02d:%02d".format(hr, min)
                TimeFmt.Hr -> "%02d:00".format(hr)
                else -> error("invalid fmt: $fmt")
            }
        }
        else -> {
            val ap = if (tm.hour < 12) "am" else "pm"
            val hr = if (tm.hour == 0 || tm.hour == 12) 12 else tm.hour % 12
            when (fmt) {
                TimeFmt.Us -> ".%03d %03d".format(ms, us)
                TimeFmt.SUs -> ":%02d.%03d %03d".format(sec, ms, us)
                TimeFmt.SMs -> ":%02d.%03d".format(sec, ms)
                TimeFmt.S -> ":%02d".format(sec)
                TimeFmt.MinSMs -> ":%02d:%02d.%03d".format(min, sec, ms)
                TimeFmt.HrMinSMs -> "$hr:%02d:%02d.%03d$ap".format(min, sec, ms)
                TimeFmt.HrMinS -> "$hr:%02d:%02d$ap".format(min, sec)
                TimeFmt.HrMin -> "$hr:%02d$ap".format(min)
                TimeFmt.Hr -> "$hr$ap"
                else -> error("invalid fmt: $fmt")
            }
        }
    }
}

fun formatDate(t: PlotTime, fmt: DateFmt, useIso8601: Boolean): String {
    val tm = getTime(t)
    val day = tm.dayOfMonth
    val month = tm.monthValue
    val mon = tm.month.name.take(3)
    val year = tm.year
    val yr = year % 100
    return when {
        useIso8601 -> when (fmt) {
            DateFmt.DayMo -> "--%02d-%02d".format(month, day)
            DateFmt.DayMoYr -> "$year-%02d-%02d".format(month, day)
            DateFmt.MoYr -> "$year-%02d".format(month)
            DateFmt.Mo -> "--%02d".format(month)
            DateFmt.Yr -> "$year"
            else -> error("invalid fmt: $fmt")
        }
        else -> when (fmt) {
            DateFmt.DayMo -> "$month/$day"
            DateFmt.DayMoYr -> "$month/$day/%02d".format(yr)
            DateFmt.MoYr -> "$mon $year"
            DateFmt.Mo -> mon
            DateFmt.Yr -> "$year"
            else -> error("invalid fmt: $fmt")
        }
    }
}

fun formatDateTime(t: PlotTime, fmt: PlotDateTimeSpec): String {
    var written = ""
    if (fmt.date != DateFmt.None)
        written += formatDate(t, fmt.date, fmt.useISO8601)
    if (fmt.time != TimeFmt.None) {
        if (fmt.date != DateFmt.None)
            written += ' '
        written += formatTime(t, fmt.time, fmt.use24HourClock)
    }
    return written
}

private val tMaxWidth = makeTime(2888, 12, 22, 12, 58, 58, 888888) // best guess at time that maximizes pixel width
fun getDateTimeWidth(fmt: PlotDateTimeSpec): Float {
    val buffer = formatDateTime(tMaxWidth, fmt)
    return ImGui.calcTextSize(buffer).x
}

fun timeLabelSame(l1: String, l2: String): Boolean {
    val len1 = l1.length
    val len2 = l2.length
    val n = len1 min len2
    return l1.substring(len1 - n) == l2.substring(len2 - n)
}

val timeFormatLevel0 = arrayOf(PlotDateTimeSpec(DateFmt.None, TimeFmt.Us),
                               PlotDateTimeSpec(DateFmt.None, TimeFmt.SMs),
                               PlotDateTimeSpec(DateFmt.None, TimeFmt.S),
                               PlotDateTimeSpec(DateFmt.None, TimeFmt.HrMin),
                               PlotDateTimeSpec(DateFmt.None, TimeFmt.Hr),
                               PlotDateTimeSpec(DateFmt.DayMo, TimeFmt.None),
                               PlotDateTimeSpec(DateFmt.Mo, TimeFmt.None),
                               PlotDateTimeSpec(DateFmt.Yr, TimeFmt.None))

val timeFormatLevel1 = arrayOf(PlotDateTimeSpec(DateFmt.None, TimeFmt.HrMin),
                               PlotDateTimeSpec(DateFmt.None, TimeFmt.HrMinS),
                               PlotDateTimeSpec(DateFmt.None, TimeFmt.HrMin),
                               PlotDateTimeSpec(DateFmt.None, TimeFmt.HrMin),
                               PlotDateTimeSpec(DateFmt.DayMoYr, TimeFmt.None),
                               PlotDateTimeSpec(DateFmt.DayMoYr, TimeFmt.None),
                               PlotDateTimeSpec(DateFmt.Yr, TimeFmt.None),
                               PlotDateTimeSpec(DateFmt.Yr, TimeFmt.None))

val timeFormatLevel1First = arrayOf(PlotDateTimeSpec(DateFmt.DayMoYr, TimeFmt.HrMinS),
                                    PlotDateTimeSpec(DateFmt.DayMoYr, TimeFmt.HrMinS),
                                    PlotDateTimeSpec(DateFmt.DayMoYr, TimeFmt.HrMin),
                                    PlotDateTimeSpec(DateFmt.DayMoYr, TimeFmt.HrMin),
                                    PlotDateTimeSpec(DateFmt.DayMoYr, TimeFmt.None),
                                    PlotDateTimeSpec(DateFmt.DayMoYr, TimeFmt.None),
                                    PlotDateTimeSpec(DateFmt.Yr, TimeFmt.None),
                                    PlotDateTimeSpec(DateFmt.Yr, TimeFmt.None))

val timeFormatMouseCursor = arrayOf(PlotDateTimeSpec(DateFmt.None, TimeFmt.Us),
                                    PlotDateTimeSpec(DateFmt.None, TimeFmt.SUs),
                                    PlotDateTimeSpec(DateFmt.None, TimeFmt.SMs),
                                    PlotDateTimeSpec(DateFmt.None, TimeFmt.HrMinS),
                                    PlotDateTimeSpec(DateFmt.None, TimeFmt.HrMin),
                                    PlotDateTimeSpec(DateFmt.DayMo, TimeFmt.Hr),
                                    PlotDateTimeSpec(DateFmt.DayMoYr, TimeFmt.None),
                                    PlotDateTimeSpec(DateFmt.MoYr, TimeFmt.None))

fun getDateTimeFmt(ctx: Array<PlotDateTimeSpec>, idx: TimeUnit): PlotDateTimeSpec {
    val style = style
    val fmt = ctx[idx.ordinal]
    fmt.useISO8601 = style.useISO8601
    fmt.use24HourClock = style.use24HourClock
    return fmt
}