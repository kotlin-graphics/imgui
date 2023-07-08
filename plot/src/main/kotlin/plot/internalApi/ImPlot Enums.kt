package plot.internalApi

//-----------------------------------------------------------------------------
// [SECTION] ImPlot Enums
//-----------------------------------------------------------------------------

//typedef int ImPlotTimeUnit;    // -> enum ImPlotTimeUnit_
//typedef int ImPlotDateFmt;     // -> enum ImPlotDateFmt_
//typedef int ImPlotTimeFmt;     // -> enum ImPlotTimeFmt_

enum class TimeUnit {
    Us,  // microsecond
    Ms,  // millisecond
    S,   // second
    Min, // minute
    Hr,  // hour
    Day, // day
    Mo,  // month
    Yr;  // year
    val i = ordinal
    companion object {
        val COUNT = values().size
        infix fun of(i: Int) = values().first { it.ordinal == i }
    }
}

enum class DateFmt {              // default        [ ISO 8601     ]
    None,
    DayMo,           // 10/3           [ --10-03      ]
    DayMoYr,         // 10/3/91        [ 1991-10-03   ]
    MoYr,            // Oct 1991       [ 1991-10      ]
    Mo,              // Oct            [ --10         ]
    Yr               // 1991           [ 1991         ]
}

enum class TimeFmt {              // default        [ 24 Hour Clock ]
    None,
    Us,              // .428 552       [ .428 552     ]
    SUs,             // :29.428 552    [ :29.428 552  ]
    SMs,             // :29.428        [ :29.428      ]
    S,               // :29            [ :29          ]
    MinSMs,          // 21:29.428      [ 21:29.428    ]
    HrMinSMs,        // 7:21:29.428pm  [ 19:21:29.428 ]
    HrMinS,          // 7:21:29pm      [ 19:21:29     ]
    HrMin,           // 7:21pm         [ 19:21        ]
    Hr               // 7pm            [ 19:00        ]
}