package imgui.api

import imgui.classes.DrawList

// Background/Foreground Draw Lists
interface backgroundForegroundDrawLists {

    /** this draw list will be the first rendering one. Useful to quickly draw shapes/text behind dear imgui contents. */
    val backgroundDrawList: DrawList
        get() = g.viewports[0].backgroundDrawList

    /** this draw list will be the last rendered one. Useful to quickly draw shapes/text over dear imgui contents.
     *  ~GetForegroundDrawList  */
    val foregroundDrawList: DrawList
        get() = g.viewports[0].foregroundDrawList
}