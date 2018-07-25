package imgui.imgui.demo.showExampleApp

import gli_.has
import glm_.c
import glm_.f
import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.begin_
import imgui.ImGui.alignTextToFramePadding
import imgui.ImGui.beginChild
import imgui.ImGui.beginGroup
import imgui.ImGui.bulletText
import imgui.ImGui.button
import imgui.ImGui.checkbox
import imgui.ImGui.colorEditVec4
import imgui.ImGui.columns
import imgui.ImGui.combo
import imgui.ImGui.cursorScreenPos
import imgui.ImGui.dragFloat
import imgui.ImGui.dragScalar
import imgui.ImGui.dragInt
import imgui.ImGui.dummy
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.endGroup
import imgui.ImGui.fontSize
import imgui.ImGui.frameCount
import imgui.ImGui.frameHeightWithSpacing
import imgui.ImGui.image
import imgui.ImGui.inputFloat
import imgui.ImGui.io
import imgui.ImGui.isMouseHoveringRect
import imgui.ImGui.isMousePosValid
import imgui.ImGui.logButtons
import imgui.ImGui.logFinish
import imgui.ImGui.logToClipboard
import imgui.ImGui.menuItem
import imgui.ImGui.nextColumn
import imgui.ImGui.popFont
import imgui.ImGui.popId
import imgui.ImGui.popItemWidth
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushFont
import imgui.ImGui.pushId
import imgui.ImGui.pushItemWidth
import imgui.ImGui.pushStyleVar
import imgui.ImGui.radioButton
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.separator
import imgui.ImGui.setNextWindowBgAlpha
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.setNextWindowSizeConstraints
import imgui.ImGui.setScrollHere
import imgui.ImGui.setWindowFontScale
import imgui.ImGui.setWindowSize
import imgui.ImGui.showFontSelector
import imgui.ImGui.showStyleSelector
import imgui.ImGui.showUserGuide
import imgui.ImGui.sliderFloat
import imgui.ImGui.sliderInt
import imgui.ImGui.sliderVec2
import imgui.ImGui.spacing
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textUnformatted
import imgui.ImGui.textWrapped
import imgui.ImGui.time
import imgui.ImGui.treeNode
import imgui.ImGui.treeNodeEx
import imgui.ImGui.treePop
import imgui.ImGui.version
import imgui.ImGui.windowDrawList
import imgui.ImGui.windowWidth
import imgui.functionalProgramming.button
import imgui.functionalProgramming.collapsingHeader
import imgui.functionalProgramming.menu
import imgui.functionalProgramming.menuBar
import imgui.functionalProgramming.menuItem
import imgui.functionalProgramming.popupContextItem
import imgui.functionalProgramming.popupContextWindow
import imgui.functionalProgramming.smallButton
import imgui.functionalProgramming.treeNode
import imgui.functionalProgramming.withChild
import imgui.functionalProgramming.withId
import imgui.functionalProgramming.withItemWidth
import imgui.functionalProgramming.withTooltip
import imgui.functionalProgramming.withWindow
import imgui.imgui.demo.showExampleApp.Console
import imgui.imgui.demo.showExampleApp.Log
import imgui.imgui.demo.showExampleApp.MainMenuBar
import imgui.imgui.imgui_demoDebugInformations.Companion.showExampleMenuFile
import imgui.imgui.imgui_demoDebugInformations.Companion.showHelpMarker
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf

object Log {

    val log = ExampleAppLog()
    var lastTime = -1.0
    val randomWords = arrayOf("system", "info", "warning", "error", "fatal", "notice", "log")
    val random = Random()
    val rand get() = abs(random.nextInt() / 100_000)

    /** Demonstrate creating a simple log window with basic filtering.  */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        // Demo: add random items (unless Ctrl is held)
        val time = g.time
        if (time - lastTime >= 0.2f && !io.keyCtrl) {
            val s = randomWords[rand % randomWords.size]
            val t = "%.1f".format(style.locale, time)
            log.addLog("[$s] Hello, time is $t, frame count is $frameCount\n")
            lastTime = time
        }
        log.draw("Example: Log (Filter not yet implemented)", open)
    }

    /** Usage:
     *      static ExampleAppLog my_log;
     *      my_log.AddLog("Hello %d world\n", 123);
     *      my_log.Draw("title");   */
    class ExampleAppLog {

        val buf = StringBuilder()
        val filter = TextFilter()// TODO
        //        ImVector<int>       LineOffsets;        // Index to lines offset
        var scrollToBottom = false

        fun addLog(fmt: String) {
            buf.append(fmt)
            scrollToBottom = true
        }

        fun draw(title: String, open: KMutableProperty0<Boolean>? = null) {

            setNextWindowSize(Vec2(500, 400), Cond.FirstUseEver)
            if(!begin_(title, open)) {
                end()
                return
            }
            if (button("Clear")) clear()
            sameLine()
            val copy = button("Copy")
            sameLine()
            filter.draw("Filter", -100f)
            separator()
            beginChild("scrolling", Vec2(0, 0), false, Wf.HorizontalScrollbar.i)
            if (copy) logToClipboard()

//      TODO      if (Filter.IsActive())
//            {
//                const char* buf_begin = Buf.begin()
//                const char* line = buf_begin
//                for (int line_no = 0; line != NULL; line_no++)
//                {
//                    const char* line_end = (line_no < LineOffsets.Size) ? buf_begin + LineOffsets[line_no] : NULL
//                    if (Filter.PassFilter(line, line_end))
//                        ImGui::TextUnformatted(line, line_end)
//                    line = line_end && line_end[1] ? line_end + 1 : NULL
//                }
//            }
//            else
            textUnformatted(buf.toString())

            if (scrollToBottom) setScrollHere(1f)
            scrollToBottom = false
            endChild()
            end()
        }

        fun clear() = buf.setLength(0)
    }
}