@file:OptIn(ExperimentalStdlibApi::class)

package plot.internalApi

import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.internal.sections.ItemFlag
import plot.api.*
import java.time.DayOfWeek
import java.time.Month
import kotlin.reflect.KMutableProperty0

//-----------------------------------------------------------------------------
// Time Utils
//-----------------------------------------------------------------------------

// Returns true if year is leap year (366 days long)
fun isLeapYear(year: Int) = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

// Returns the number of days in a month, accounting for Feb. leap years. #month is zero indexed.
private val days = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
fun getDaysInMonth(year: Int, month: Int): Int = days[month] + (month == 1 && isLeapYear(year)).i

// Make a UNIX timestamp from a tm struct expressed in UTC time (i.e. GMT timezone).
//IMPLOT_API ImPlotTime MkGmtTime(struct tm *ptm);
//// Make a tm struct expressed in UTC time (i.e. GMT timezone) from a UNIX timestamp.
//IMPLOT_API tm* GetGmtTime(const ImPlotTime& t, tm* ptm);
//
//// Make a UNIX timestamp from a tm struct expressed in local time.
//IMPLOT_API ImPlotTime MkLocTime(struct tm *ptm);
//// Make a tm struct expressed in local time from a UNIX timestamp.
//IMPLOT_API tm* GetLocTime(const ImPlotTime& t, tm* ptm);
//
//// NB: The following functions only work if there is a current ImPlotContext because the
//// internal tm struct is owned by the context! They are aware of ImPlotStyle.UseLocalTime.
//
//// Make a timestamp from time components.
//// year[1970-3000], month[0-11], day[1-31], hour[0-23], min[0-59], sec[0-59], us[0,999999]
//IMPLOT_API ImPlotTime MakeTime(int year, int month = 0, int day = 1, int hour = 0, int min = 0, int sec = 0, int us = 0);
// Get year component from timestamp [1970-3000]
//IMPLOT_API int GetYear(const ImPlotTime& t); -> PlotTime class

//// Adds or subtracts time from a timestamp. #count > 0 to add, < 0 to subtract.
//IMPLOT_API ImPlotTime AddTime(const ImPlotTime& t, ImPlotTimeUnit unit, int count);
//// Rounds a timestamp down to nearest unit.
//IMPLOT_API ImPlotTime FloorTime(const ImPlotTime& t, ImPlotTimeUnit unit);
//// Rounds a timestamp up to the nearest unit.
//IMPLOT_API ImPlotTime CeilTime(const ImPlotTime& t, ImPlotTimeUnit unit);
//// Rounds a timestamp up or down to the nearest unit.
//IMPLOT_API ImPlotTime RoundTime(const ImPlotTime& t, ImPlotTimeUnit unit);
//// Combines the date of one timestamp with the time-of-day of another timestamp.
//IMPLOT_API ImPlotTime CombineDateTime(const ImPlotTime& date_part, const ImPlotTime& time_part);
//
//// Formats the time part of timestamp t into a buffer according to #fmt
//IMPLOT_API int FormatTime(const ImPlotTime& t, char* buffer, int size, ImPlotTimeFmt fmt, bool use_24_hr_clk);
//// Formats the date part of timestamp t into a buffer according to #fmt
//IMPLOT_API int FormatDate(const ImPlotTime& t, char* buffer, int size, ImPlotDateFmt fmt, bool use_iso_8601);
//// Formats the time and/or date parts of a timestamp t into a buffer according to #fmt
//IMPLOT_API int FormatDateTime(const ImPlotTime& t, char* buffer, int size, ImPlotDateTimeSpec fmt);

// Shows a date picker widget block (year/month/day).
// #level = 0 for day, 1 for month, 2 for year. Modified by user interaction.
// #t will be set when a day is clicked and the function will return true.
// #t1 and #t2 are optional dates to highlight.
fun showDatePicker(id: String, pLevel: KMutableProperty0<Int>, t: PlotTime, t1: PlotTime? = null, t2: PlotTime? = null): Boolean {

    var level by pLevel

    ImGui.pushID(id)
    ImGui.beginGroup()

    val style = ImGui.style
    val colTxt = style.colors[Col.Text]
    val colDis = style.colors[Col.TextDisabled]
    val colBtn = style.colors[Col.Button]
    ImGui.pushStyleColor(Col.Button, Vec4())
    ImGui.pushStyleVar(StyleVar.ItemSpacing, Vec2())

    val ht = ImGui.frameHeight
    val cellSize = Vec2(ht * 1.25f, ht)
//    char buff [32]
    var clk = false
//    tm& Tm = GImPlot->Tm

    val minYr = 1970
    val maxYr = 2999

    // t1 parts
    var t1Mo = 0;
    var t1Md = 0;
    var t1Yr = 0
    if (t1 != null) {
        val tm = getTime(t1)
        t1Mo = tm.monthValue
        t1Md = tm.dayOfMonth
        t1Yr = tm.year
    }

    // t2 parts
    var t2Mo = 0;
    var t2Md = 0;
    var t2Yr = 0
    if (t2 != null) {
        val tm = getTime(t2)
        t2Mo = tm.monthValue
        t2Md = tm.dayOfMonth
        t2Yr = tm.year
    }

    // day widget
    if (level == 0) {
        t put floorTime(t, TimeUnit.Day)
        var tm = getTime(t)
        val thisYear = tm.year
        val lastYear = thisYear - 1
        val nextYear = thisYear + 1
        val thisMon = tm.monthValue
        val lastMon = if (thisMon == 0) 11 else thisMon - 1
        val nextMon = if (thisMon == 11) 0 else thisMon + 1
        val daysThisMo = getDaysInMonth(thisYear, thisMon)
        val daysLastMo = getDaysInMonth(if (thisMon == 0) lastYear else thisYear, lastMon)
        val tFirstMo = floorTime(t, TimeUnit.Mo)
        tm = getTime(tFirstMo)
        val firstWd = tm.dayOfWeek.value - 1 // [JVM] we need to decrease because on jvm is [1-7]
        // month year
        var buff = "${tm.month.name.lowercase().capitalize()} $thisYear"
        if (ImGui.button(buff))
            level = 1
        ImGui.sameLine(5 * cellSize.x)
        beginDisabledControls(thisYear <= minYr && thisMon == 0)
        if (ImGui.arrowButtonEx("##Up", Dir.Up, cellSize))
            t put addTime(t, TimeUnit.Mo, -1)
        endDisabledControls(thisYear <= minYr && thisMon == 0)
        ImGui.sameLine()
        beginDisabledControls(thisYear >= maxYr && thisMon == 11)
        if (ImGui.arrowButtonEx("##Down", Dir.Down, cellSize))
            t put addTime(t, TimeUnit.Mo, 1)
        endDisabledControls(thisYear >= maxYr && thisMon == 11)
        // render weekday abbreviations
        ImGui.pushItemFlag(ItemFlag.Disabled, true)
        for (i in 0..<7) {
            ImGui.button(DayOfWeek.values()[i].name.take(2).lowercase().capitalize(), cellSize)
            if (i != 6)
                ImGui.sameLine()
        }
        ImGui.popItemFlag()
        // 0 = last mo, 1 = this mo, 2 = next mo
        var mo = if (firstWd > 0) 0 else 1
        var day = if (mo == 1) 1 else daysLastMo - firstWd + 1
        for (i in 0..<6) {
            for (j in 0..<7) {
                if (mo == 0 && day > daysLastMo) {
                    mo = 1
                    day = 1
                } else if (mo == 1 && day > daysThisMo) {
                    mo = 2
                    day = 1
                }
                val nowYr = if (mo == 0 && thisMon == 0) lastYear else if (mo == 2 && thisMon == 11) nextYear else thisYear
                val nowMo = if (mo == 0) lastMon else if (mo == 1) thisMon else nextMon
                val nowMd = day

                val offMo = mo == 0 || mo == 2
                val t1OrT2 = (t1 != null && t1Mo == nowMo && t1Yr == nowYr && t1Md == nowMd) || (t2 != null && t2Mo == nowMo && t2Yr == nowYr && t2Md == nowMd)

                if (offMo)
                    ImGui.pushStyleColor(Col.Text, colDis)
                if (t1OrT2) {
                    ImGui.pushStyleColor(Col.Button, colBtn)
                    ImGui.pushStyleColor(Col.Text, colTxt)
                }
                ImGui.pushID(i * 7 + j)
                buff = "$day"
                if (nowYr == minYr - 1 || nowYr == maxYr + 1) {
                    ImGui.dummy(cellSize)
                } else if (ImGui.button(buff, cellSize) && !clk) {
                    t put makeTime(nowYr, nowMo, nowMd)
                    clk = true
                }
                ImGui.popID()
                if (t1OrT2)
                    ImGui.popStyleColor(2)
                if (offMo)
                    ImGui.popStyleColor()
                if (j != 6)
                    ImGui.sameLine()
                day++
            }
        }
    }
    // month widget
    else if (level == 1) {
        t put floorTime(t, TimeUnit.Mo)
        val tm = getTime(t)
        val thisYr = tm.year
        val buff = "$thisYr"
        if (ImGui.button(buff))
            level = 2
        beginDisabledControls(thisYr <= minYr)
        ImGui.sameLine(5 * cellSize.x)
        if (ImGui.arrowButtonEx("##Up", Dir.Up, cellSize))
            t put addTime(t, TimeUnit.Yr, -1)
        endDisabledControls(thisYr <= minYr)
        ImGui.sameLine()
        beginDisabledControls(thisYr >= maxYr)
        if (ImGui.arrowButtonEx("##Down", Dir.Down, cellSize))
            t put addTime(t, TimeUnit.Yr, 1)
        endDisabledControls(thisYr >= maxYr)
        // ImGui::Dummy(cell_size);
        cellSize.x *= 7f / 4f
        cellSize.y *= 7f / 3f
        var mo = 0
        for (i in 0..<3) {
            for (j in 0..<4) {
                val t1OrT2 = (t1 != null && t1Yr == thisYr && t1Mo == mo) || (t2 != null && t2Yr == thisYr && t2Mo == mo)
                if (t1OrT2)
                    ImGui.pushStyleColor(Col.Button, colBtn)
                if (ImGui.button(Month.values()[mo].name.take(3).lowercase().capitalize(), cellSize) && !clk) {
                    t put makeTime(thisYr, mo)
                    level = 0
                }
                if (t1OrT2)
                    ImGui.popStyleColor()
                if (j != 3)
                    ImGui.sameLine()
                mo++
            }
        }
    } else if (level == 2) {
        t put floorTime(t, TimeUnit.Yr)
        val thisYr = t.year
        var yr = thisYr - thisYr % 20
        ImGui.pushItemFlag(ItemFlag.Disabled, true)
        var buff = "$yr-${yr + 19}"
        ImGui.button(buff)
        ImGui.popItemFlag()
        ImGui.sameLine(5 * cellSize.x)
        beginDisabledControls(yr <= minYr)
        if (ImGui.arrowButtonEx("##Up", Dir.Up, cellSize))
            t put makeTime(yr - 20)
        endDisabledControls(yr <= minYr)
        ImGui.sameLine()
        beginDisabledControls(yr + 20 >= maxYr)
        if (ImGui.arrowButtonEx("##Down", Dir.Down, cellSize))
            t put makeTime(yr + 20)
        endDisabledControls(yr + 20 >= maxYr)
        // ImGui::Dummy(cell_size);
        cellSize.x *= 7f / 4f
        cellSize.y *= 7f / 5f
        for (i in 0..<5) {
            for (j in 0..<4) {
                val t1OrT2 = (t1 != null && t1Yr == yr) || (t2 != null && t2Yr == yr)
                if (t1OrT2)
                    ImGui.pushStyleColor(Col.Button, colBtn)
                buff = "$yr"
                if (yr < 1970 || yr > 3000) {
                    ImGui.dummy(cellSize)
                } else if (ImGui.button(buff, cellSize)) {
                    t put makeTime(yr)
                    level = 1
                }
                if (t1OrT2)
                    ImGui.popStyleColor()
                if (j != 3)
                    ImGui.sameLine()
                yr++
            }
        }
    }
    ImGui.popStyleVar()
    ImGui.popStyleColor()
    ImGui.endGroup()
    ImGui.popID()
    return clk
}


private val nums = arrayOf("00", "01", "02", "03", "04", "05", "06", "07", "08", "09",
                           "10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
                           "20", "21", "22", "23", "24", "25", "26", "27", "28", "29",
                           "30", "31", "32", "33", "34", "35", "36", "37", "38", "39",
                           "40", "41", "42", "43", "44", "45", "46", "47", "48", "49",
                           "50", "51", "52", "53", "54", "55", "56", "57", "58", "59")

private val amPm = arrayOf("am", "pm")

// Shows a time picker widget block (hour/min/sec).
// #t will be set when a new hour, minute, or sec is selected or am/pm is toggled, and the function will return true.
fun showTimePicker(id: String, t: PlotTime): Boolean {
    val gp = gImPlot
    ImGui.pushID(id)
    var tm = getTime(t)

    val hour24 = gp.style.use24HourClock

    var hr = if (hour24) tm.hour else (if (tm.hour == 0 || tm.hour == 12) 12 else tm.hour % 12)
    var min = tm.minute
    var sec = tm.second
    var ap = if (tm.hour < 12) 0 else 1

    var changed = false

    val spacing = Vec2(ImGui.style.itemSpacing)
    spacing.x = 0f
    val width = ImGui.calcTextSize("888").x
    val height = ImGui.frameHeight

    ImGui.pushStyleVar(StyleVar.ItemSpacing, spacing)
    ImGui.pushStyleVar(StyleVar.ScrollbarSize, 2f)
    ImGui.pushStyleColor(Col.FrameBg, Vec4())
    ImGui.pushStyleColor(Col.Button, Vec4())
    ImGui.pushStyleColor(Col.FrameBgHovered, ImGui.getStyleColorVec4(Col.ButtonHovered))

    ImGui.setNextItemWidth(width)
    if (ImGui.beginCombo("##hr", nums[hr], ComboFlag.NoArrowButton)) {
        val ia = if (hour24) 0 else 1
        val ib = if (hour24) 24 else 13
        for (i in ia..<ib)
            if (ImGui.selectable(nums[i], i == hr)) {
                hr = i
                changed = true
            }
        ImGui.endCombo()
    }
    ImGui.sameLine()
    ImGui.text(":")
    ImGui.sameLine()
    ImGui.setNextItemWidth(width)
    if (ImGui.beginCombo("##min", nums[min], ComboFlag.NoArrowButton)) {
        for (i in 0..<60)
            if (ImGui.selectable(nums[i], i == min)) {
                min = i
                changed = true
            }
        ImGui.endCombo()
    }
    ImGui.sameLine()
    ImGui.text(":")
    ImGui.sameLine()
    ImGui.setNextItemWidth(width)
    if (ImGui.beginCombo("##sec", nums[sec], ComboFlag.NoArrowButton)) {
        for (i in 0..<60)
            if (ImGui.selectable(nums[i], i == sec)) {
                sec = i
                changed = true
            }
        ImGui.endCombo()
    }
    if (!hour24) {
        ImGui.sameLine()
        if (ImGui.button(amPm[ap], Vec2(0, height))) {
            ap = 1 - ap
            changed = true
        }
    }

    ImGui.popStyleColor(3)
    ImGui.popStyleVar(2)
    ImGui.popID()

    if (changed) {
        if (!hour24)
            hr = hr % 12 + ap * 12
        tm = tm.withHour(hr)
        tm = tm.withMinute(min)
        tm = tm.withSecond(sec)
        t put mkTime(tm)
    }

    return changed
}